package com.yourname.ayanami.learn.utils

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import java.io.IOException

class AudioPlayer {
    private var mediaPlayer: MediaPlayer? = null

    fun playAudioFromUrl(url: String, onCompletion: () -> Unit = {}) {
        releasePlayer()
        mediaPlayer = MediaPlayer().apply {
            try {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                setVolume(1f, 1f)
                setDataSource(url)
                prepareAsync()
                setOnPreparedListener { player ->
                    Log.d(TAG, "Playing assistant audio: $url")
                    player.start()
                }
                setOnCompletionListener { player ->
                    player.release()
                    mediaPlayer = null
                    onCompletion()
                }
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "Audio playback failed. what=$what extra=$extra url=$url")
                    releasePlayer()
                    true
                }
            } catch (error: IOException) {
                Log.e(TAG, "Could not open assistant audio: $url", error)
                releasePlayer()
            }
        }
    }

    fun stopPlaying() {
        releasePlayer()
    }

    private fun releasePlayer() {
        mediaPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
    }

    private companion object {
        const val TAG = "AyanamiAudioPlayer"
    }
}
