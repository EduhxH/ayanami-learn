package com.yourname.ayanami.learn.data.remote

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.http.encodeURLParameter
import io.ktor.websocket.Frame
import io.ktor.websocket.readBytes
import io.ktor.websocket.readText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.ByteArrayOutputStream
import javax.inject.Inject

sealed class LiveVoiceOutbound {
    data class Audio(val base64Pcm: String) : LiveVoiceOutbound()
    data class Text(val text: String) : LiveVoiceOutbound()
    data object ActivityStart : LiveVoiceOutbound()
    data object ActivityEnd : LiveVoiceOutbound()
    data object AudioStreamEnd : LiveVoiceOutbound()
    data object Stop : LiveVoiceOutbound()
}

sealed class LiveVoiceEvent {
    data object Ready : LiveVoiceEvent()
    data object TurnComplete : LiveVoiceEvent()
    data object Interrupted : LiveVoiceEvent()
    data object Closed : LiveVoiceEvent()
    data class Audio(val base64Pcm: String, val sampleRate: Int) : LiveVoiceEvent()
    data class InputTranscript(val text: String) : LiveVoiceEvent()
    data class OutputTranscript(val text: String) : LiveVoiceEvent()
    data class Error(val message: String) : LiveVoiceEvent()
}

class LiveVoiceApiService @Inject constructor(
    private val httpClient: HttpClient
) {
    private val json = Json { ignoreUnknownKeys = true }
    private var receivedAudioEvents = 0

    suspend fun runSession(
        userId: String,
        nativeLanguage: String,
        outgoing: ReceiveChannel<LiveVoiceOutbound>,
        onEvent: suspend (LiveVoiceEvent) -> Unit
    ) {
        val url = "${ApiConfig.WS_BASE_URL}/api/live/voice" +
            "?userId=${userId.encodeURLParameter()}" +
            "&nativeLanguage=${nativeLanguage.encodeURLParameter()}"

        runCatching {
            Log.d(TAG, "Opening voice WebSocket: $url")
            httpClient.webSocket(urlString = url) {
                Log.d(TAG, "Voice WebSocket opened.")
                coroutineScope {
                    val sender = launch {
                        var audioCount = 0
                        for (message in outgoing) {
                            send(Frame.Text(message.toWireJson()))
                            if (message is LiveVoiceOutbound.Audio) {
                                audioCount += 1
                                if (audioCount == 1 || audioCount % 25 == 0) {
                                    Log.d(TAG, "Sent audio chunks: $audioCount")
                                }
                            } else {
                                Log.d(TAG, "Sent voice message: ${message.javaClass.simpleName}")
                            }
                            if (message is LiveVoiceOutbound.Stop) break
                        }
                    }

                    val receiver = launch {
                        val payloadBuffer = ByteArrayOutputStream()
                        for (frame in incoming) {
                            val payload = frame.readPayload(payloadBuffer) ?: continue
                            parseEvent(payload)?.let { event ->
                                logReceivedEvent(event)
                                onEvent(event)
                            }
                        }
                    }

                    receiver.join()
                    sender.cancel()
                }
            }
        }.onFailure { error ->
            if (error !is CancellationException) {
                Log.e(TAG, "Voice session failed.", error)
                onEvent(LiveVoiceEvent.Error(error.localizedMessage ?: "Voice session failed."))
            }
        }

        Log.d(TAG, "Voice WebSocket closed.")
        onEvent(LiveVoiceEvent.Closed)
    }

    private fun LiveVoiceOutbound.toWireJson(): String {
        return json.encodeToString(
            buildJsonObject {
                when (this@toWireJson) {
                    is LiveVoiceOutbound.Audio -> {
                        put("type", "audio")
                        put("data", base64Pcm)
                    }
                    is LiveVoiceOutbound.Text -> {
                        put("type", "text")
                        put("text", text)
                    }
                    LiveVoiceOutbound.ActivityStart -> {
                        put("type", "activityStart")
                    }
                    LiveVoiceOutbound.ActivityEnd -> {
                        put("type", "activityEnd")
                    }
                    LiveVoiceOutbound.AudioStreamEnd -> {
                        put("type", "audioStreamEnd")
                    }
                    LiveVoiceOutbound.Stop -> {
                        put("type", "stop")
                    }
                }
            }
        )
    }

    private fun parseEvent(raw: String): LiveVoiceEvent? {
        val obj = runCatching { json.parseToJsonElement(raw).jsonObject }.getOrNull() ?: return null
        return when (val type = obj["type"]?.jsonPrimitive?.contentOrNull) {
            "ready" -> LiveVoiceEvent.Ready
            "audio" -> LiveVoiceEvent.Audio(
                base64Pcm = obj["data"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                sampleRate = obj["sampleRate"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 24_000
            )
            "inputTranscript" -> LiveVoiceEvent.InputTranscript(obj["text"]?.jsonPrimitive?.contentOrNull.orEmpty())
            "outputTranscript" -> LiveVoiceEvent.OutputTranscript(obj["text"]?.jsonPrimitive?.contentOrNull.orEmpty())
            "turnComplete" -> LiveVoiceEvent.TurnComplete
            "interrupted" -> LiveVoiceEvent.Interrupted
            "error" -> LiveVoiceEvent.Error(obj["text"]?.jsonPrimitive?.contentOrNull ?: "Voice session error.")
            else -> LiveVoiceEvent.Error("Unknown voice event: $type")
        }
    }

    private fun Frame.readPayload(payloadBuffer: ByteArrayOutputStream): String? {
        val bytes = when (this) {
            is Frame.Text -> readText().toByteArray(Charsets.UTF_8)
            is Frame.Binary -> readBytes()
            else -> return null
        }
        payloadBuffer.write(bytes)
        if (!fin) return null

        val payload = payloadBuffer.toByteArray().toString(Charsets.UTF_8)
        payloadBuffer.reset()
        return payload
    }

    private fun logReceivedEvent(event: LiveVoiceEvent) {
        if (event is LiveVoiceEvent.Audio) {
            receivedAudioEvents += 1
            if (receivedAudioEvents == 1 || receivedAudioEvents % 25 == 0) {
                Log.d(TAG, "Received audio events: $receivedAudioEvents sampleRate=${event.sampleRate}")
            }
        } else {
            Log.d(TAG, "Received voice event: ${event.javaClass.simpleName}")
        }
    }

    private companion object {
        const val TAG = "AyanamiVoice"
    }
}
