package com.yourname.ayanami.learn.utils

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class PcmAudioPlayer {
    private var audioTrack: AudioTrack? = null
    private val lock = Any()
    private val queue = LinkedBlockingDeque<PlaybackItem>()
    private val isRunning = AtomicBoolean(false)
    private var playbackThread: Thread? = null
    private var activeSampleRate = OUTPUT_SAMPLE_RATE
    private var isTrackPlaying = false
    private var bufferedBeforePlayBytes = 0
    private var writtenFrames = 0L

    fun playChunk(pcm: ByteArray, sampleRate: Int = OUTPUT_SAMPLE_RATE) {
        if (pcm.isEmpty()) return
        queue.offer(PlaybackItem.Chunk(AudioChunk(pcm, sampleRate.normalizedSampleRate())))
        ensurePlaybackThread()
    }

    fun finishTurn() {
        queue.offer(PlaybackItem.EndOfTurn)
        ensurePlaybackThread()
    }

    fun clearQueue() {
        queue.clear()
    }

    private fun ensurePlaybackThread() {
        if (isRunning.getAndSet(true)) return

        playbackThread = Thread {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO)
            while (isRunning.get()) {
                val item = try {
                    queue.poll(350, TimeUnit.MILLISECONDS)
                } catch (_: InterruptedException) {
                    break
                }
                if (item == null) {
                    if (queue.isEmpty()) {
                        startBufferedAudioIfNeeded()
                        isRunning.set(false)
                    }
                    continue
                }
                when (item) {
                    is PlaybackItem.Chunk -> writeChunk(item.chunk.withImmediatelyAvailableChunks())
                    PlaybackItem.EndOfTurn -> finishCurrentTurn()
                }
            }
            isRunning.set(false)
        }.apply {
            name = "AyanamiPcmAudioPlayer"
            priority = Thread.MAX_PRIORITY
            start()
        }
    }

    private fun AudioChunk.withImmediatelyAvailableChunks(): AudioChunk {
        var totalBytes = pcm.size
        val chunks = mutableListOf(this)

        while (totalBytes < MAX_BATCH_BYTES) {
            when (val next = queue.poll() ?: break) {
                is PlaybackItem.Chunk -> {
                    if (next.chunk.sampleRate != sampleRate) {
                        queue.offerFirst(next)
                        break
                    }
                    chunks += next.chunk
                    totalBytes += next.chunk.pcm.size
                }
                PlaybackItem.EndOfTurn -> {
                    queue.offerFirst(next)
                    break
                }
            }
        }

        if (chunks.size == 1) return this

        val merged = ByteArray(totalBytes)
        var offset = 0
        chunks.forEach { chunk ->
            chunk.pcm.copyInto(merged, offset)
            offset += chunk.pcm.size
        }
        return AudioChunk(merged, sampleRate)
    }

    private fun startBufferedAudioIfNeeded() {
        synchronized(lock) {
            val track = audioTrack ?: return
            if (!isTrackPlaying && bufferedBeforePlayBytes > 0) {
                track.play()
                isTrackPlaying = true
            }
        }
    }

    private fun finishCurrentTurn() {
        startBufferedAudioIfNeeded()

        val playbackTarget = synchronized(lock) {
            val track = audioTrack ?: return
            PlaybackTarget(
                track = track,
                sampleRate = activeSampleRate,
                framesWritten = writtenFrames,
                framesPlayed = track.playbackHeadPosition.toLong()
            )
        }

        val remainingFrames = (playbackTarget.framesWritten - playbackTarget.framesPlayed).coerceAtLeast(0)
        val timeoutMs = ((remainingFrames * 1_000L) / playbackTarget.sampleRate)
            .coerceAtLeast(MIN_DRAIN_WAIT_MS) + DRAIN_GRACE_MS
        val deadline = android.os.SystemClock.elapsedRealtime() + timeoutMs

        while (isRunning.get() && android.os.SystemClock.elapsedRealtime() < deadline) {
            val playedFrames = synchronized(lock) {
                if (audioTrack !== playbackTarget.track) return
                runCatching { playbackTarget.track.playbackHeadPosition.toLong() }.getOrDefault(playbackTarget.framesPlayed)
            }
            if (playedFrames >= playbackTarget.framesWritten) break
            try {
                Thread.sleep(DRAIN_POLL_MS)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                break
            }
        }

        synchronized(lock) {
            if (audioTrack === playbackTarget.track) {
                releaseTrackLocked()
            }
        }
    }

    fun stop() {
        isRunning.set(false)
        queue.clear()
        playbackThread?.interrupt()
        playbackThread = null
        releaseTrack()
    }

    private fun writeChunk(chunk: AudioChunk) {
        synchronized(lock) {
            val track = if (audioTrack == null || activeSampleRate != chunk.sampleRate) {
                releaseTrackLocked()
                activeSampleRate = chunk.sampleRate
                createTrack(chunk.sampleRate).also { created ->
                    audioTrack = created
                    isTrackPlaying = false
                    bufferedBeforePlayBytes = 0
                    writtenFrames = 0L
                }
            } else {
                audioTrack
            } ?: return

            var offset = 0
            while (offset < chunk.pcm.size && isRunning.get()) {
                val written = track.write(
                    chunk.pcm,
                    offset,
                    chunk.pcm.size - offset,
                    AudioTrack.WRITE_BLOCKING
                )
                if (written <= 0) {
                    Log.w(TAG, "AudioTrack write failed with code $written.")
                    break
                }
                offset += written
                writtenFrames += written / BYTES_PER_SAMPLE
                if (!isTrackPlaying) {
                    bufferedBeforePlayBytes += written
                    if (bufferedBeforePlayBytes >= prebufferBytes(chunk.sampleRate)) {
                        track.play()
                        isTrackPlaying = true
                    }
                }
            }
        }
    }

    private fun releaseTrack() {
        synchronized(lock) {
            releaseTrackLocked()
        }
    }

    private fun releaseTrackLocked() {
        audioTrack?.let { track ->
            runCatching { track.pause() }
            runCatching { track.flush() }
            runCatching { track.release() }
        }
        audioTrack = null
        isTrackPlaying = false
        bufferedBeforePlayBytes = 0
        writtenFrames = 0L
    }

    private fun createTrack(sampleRate: Int): AudioTrack {
        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val safeMinBufferSize = minBufferSize.coerceAtLeast(sampleRate * BYTES_PER_SAMPLE)
        val bufferSize = (safeMinBufferSize * 8)
            .coerceAtLeast(sampleRate * BYTES_PER_SAMPLE * 2)
            .coerceAtLeast(OUTPUT_SAMPLE_RATE * BYTES_PER_SAMPLE)
        return AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
            .also { track ->
                track.setVolume(PLAYBACK_VOLUME)
            }
    }

    private fun prebufferBytes(sampleRate: Int): Int {
        return ((sampleRate * BYTES_PER_SAMPLE * PREBUFFER_MS) / 1_000)
            .coerceAtLeast(MIN_PREBUFFER_BYTES)
    }

    private fun Int.normalizedSampleRate(): Int {
        return when (this) {
            16_000, 22_050, 24_000, 32_000, 44_100, 48_000 -> this
            else -> {
                Log.w(TAG, "Unexpected PCM sample rate $this. Falling back to $OUTPUT_SAMPLE_RATE.")
                OUTPUT_SAMPLE_RATE
            }
        }
    }

    private data class AudioChunk(
        val pcm: ByteArray,
        val sampleRate: Int
    )

    private sealed class PlaybackItem {
        data class Chunk(val chunk: AudioChunk) : PlaybackItem()
        data object EndOfTurn : PlaybackItem()
    }

    private data class PlaybackTarget(
        val track: AudioTrack,
        val sampleRate: Int,
        val framesWritten: Long,
        val framesPlayed: Long
    )

    companion object {
        private const val TAG = "AyanamiVoice"
        const val OUTPUT_SAMPLE_RATE = 24_000
        private const val BYTES_PER_SAMPLE = 2
        private const val PREBUFFER_MS = 260
        private const val MIN_PREBUFFER_BYTES = 12_480
        private const val MAX_BATCH_BYTES = 48_000
        private const val PLAYBACK_VOLUME = 0.9f
        private const val DRAIN_POLL_MS = 20L
        private const val MIN_DRAIN_WAIT_MS = 250L
        private const val DRAIN_GRACE_MS = 700L
    }
}
