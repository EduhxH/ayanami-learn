package com.yourname.ayanami.learn.utils

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

class AudioRecorder(private val context: Context) {
    private var audioRecord: AudioRecord? = null
    private var outputFile: File? = null
    private var recordingThread: Thread? = null
    private val isRecording = AtomicBoolean(false)

    private val sampleRate = 16_000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    @SuppressLint("MissingPermission")
    fun startRecording(): File? {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return null
        }

        cancelRecording()

        val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        if (minBufferSize <= 0) return null

        val bufferSize = minBufferSize.coerceAtLeast(sampleRate)
        val file = File(context.cacheDir, "audio_record_${System.currentTimeMillis()}.wav")
        outputFile = file

        val recorder = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize
        )

        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            outputFile = null
            return null
        }

        audioRecord = recorder
        isRecording.set(true)
        writeEmptyWavHeader(file)
        recorder.startRecording()

        recordingThread = thread(name = "AyanamiAudioRecorder") {
            val buffer = ByteArray(bufferSize)
            RandomAccessFile(file, "rw").use { wav ->
                wav.seek(WAV_HEADER_SIZE.toLong())
                while (isRecording.get()) {
                    val bytesRead = recorder.read(buffer, 0, buffer.size)
                    if (bytesRead > 0) {
                        wav.write(buffer, 0, bytesRead)
                    }
                }
                updateWavHeader(wav, wav.length() - WAV_HEADER_SIZE)
            }
        }

        return file
    }

    fun stopRecording(): File? {
        val file = outputFile
        isRecording.set(false)
        runCatching { audioRecord?.stop() }
        recordingThread?.join(1_000)
        audioRecord?.release()

        audioRecord = null
        recordingThread = null
        outputFile = null

        return file?.takeIf { it.exists() && it.length() > WAV_HEADER_SIZE }
    }

    fun cancelRecording() {
        isRecording.set(false)
        runCatching { audioRecord?.stop() }
        recordingThread?.join(500)
        audioRecord?.release()
        outputFile?.delete()

        audioRecord = null
        recordingThread = null
        outputFile = null
    }

    private fun writeEmptyWavHeader(file: File) {
        RandomAccessFile(file, "rw").use { wav ->
            wav.setLength(0)
            writeWavHeader(wav, 0)
        }
    }

    private fun updateWavHeader(wav: RandomAccessFile, dataSize: Long) {
        wav.seek(0)
        writeWavHeader(wav, dataSize)
    }

    private fun writeWavHeader(wav: RandomAccessFile, dataSize: Long) {
        val byteRate = sampleRate * CHANNELS * BITS_PER_SAMPLE / 8
        val blockAlign = CHANNELS * BITS_PER_SAMPLE / 8

        wav.writeBytes("RIFF")
        wav.writeIntLE((36 + dataSize).toInt())
        wav.writeBytes("WAVE")
        wav.writeBytes("fmt ")
        wav.writeIntLE(16)
        wav.writeShortLE(1)
        wav.writeShortLE(CHANNELS)
        wav.writeIntLE(sampleRate)
        wav.writeIntLE(byteRate)
        wav.writeShortLE(blockAlign)
        wav.writeShortLE(BITS_PER_SAMPLE)
        wav.writeBytes("data")
        wav.writeIntLE(dataSize.toInt())
    }

    private fun RandomAccessFile.writeIntLE(value: Int) {
        write(value and 0xff)
        write(value shr 8 and 0xff)
        write(value shr 16 and 0xff)
        write(value shr 24 and 0xff)
    }

    private fun RandomAccessFile.writeShortLE(value: Int) {
        write(value and 0xff)
        write(value shr 8 and 0xff)
    }

    companion object {
        private const val WAV_HEADER_SIZE = 44L
        private const val CHANNELS = 1
        private const val BITS_PER_SAMPLE = 16
    }
}
