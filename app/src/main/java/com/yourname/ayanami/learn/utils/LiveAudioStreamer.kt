package com.yourname.ayanami.learn.utils

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

class LiveAudioStreamer(private val context: Context) {
    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null
    private val isRecording = AtomicBoolean(false)

    @SuppressLint("MissingPermission")
    fun start(
        onPcmChunk: (ByteArray) -> Unit,
        onError: (String) -> Unit = {}
    ): Boolean {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return false
        }

        stop()

        val minBufferSize = AudioRecord.getMinBufferSize(
            INPUT_SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBufferSize <= 0) return false

        val bufferSize = minBufferSize.coerceAtLeast(CHUNK_BYTES)
        val recorder = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            INPUT_SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )

        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            return false
        }

        audioRecord = recorder
        isRecording.set(true)
        runCatching {
            recorder.startRecording()
        }.onFailure {
            isRecording.set(false)
            recorder.release()
            audioRecord = null
            return false
        }

        recordingThread = thread(name = "AyanamiLiveAudioStreamer") {
            val buffer = ByteArray(bufferSize)
            while (isRecording.get()) {
                val bytesRead = recorder.read(buffer, 0, buffer.size)
                if (bytesRead > 0) {
                    onPcmChunk(buffer.copyOf(bytesRead))
                } else if (bytesRead < 0) {
                    onError("Microphone read failed with code $bytesRead.")
                    isRecording.set(false)
                }
            }
        }

        return true
    }

    fun stop() {
        isRecording.set(false)
        runCatching { audioRecord?.stop() }
        recordingThread?.join(700)
        audioRecord?.release()
        audioRecord = null
        recordingThread = null
    }

    companion object {
        const val INPUT_SAMPLE_RATE = 16_000
        private const val CHUNK_BYTES = 3_200
    }
}
