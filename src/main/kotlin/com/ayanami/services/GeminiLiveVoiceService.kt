package com.ayanami.services

import io.github.cdimascio.dotenv.dotenv
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.DefaultWebSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readBytes
import io.ktor.websocket.readText
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream

class GeminiLiveVoiceService(
    private val chatMemoryService: ChatMemoryService
) {
    private val logger = LoggerFactory.getLogger(GeminiLiveVoiceService::class.java)
    private val dotenv = dotenv { ignoreIfMissing = true }
    private val json = Json { ignoreUnknownKeys = true }
    private val httpClient = HttpClient(CIO) {
        install(WebSockets)
        install(HttpTimeout) {
            requestTimeoutMillis = HttpTimeout.INFINITE_TIMEOUT_MS
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = HttpTimeout.INFINITE_TIMEOUT_MS
        }
    }

    private val apiKey = System.getenv("GEMINI_API_KEY")
        ?: dotenv["GEMINI_API_KEY"]
        ?: ""
    private val model = System.getenv("GEMINI_LIVE_MODEL")
        ?: dotenv["GEMINI_LIVE_MODEL"]
        ?: "gemini-3.1-flash-live-preview"
    private val voiceName = System.getenv("GEMINI_LIVE_VOICE")
        ?: dotenv["GEMINI_LIVE_VOICE"]
        ?: "Kore"

    val isConfigured: Boolean
        get() = apiKey.isNotBlank()

    suspend fun bridge(
        appSession: DefaultWebSocketSession,
        userId: String,
        nativeLanguage: String
    ) {
        if (!isConfigured) {
            appSession.sendAppMessage("error", "Gemini Live API key is not configured.")
            appSession.close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Gemini key missing"))
            return
        }

        val context = chatMemoryService.loadContext(userId)
        var lastUserTranscript = ""
        var assistantTranscript = StringBuilder()
        var appAudioChunks = 0
        logger.info(
            "Gemini Live bridge opening userId={} nativeLanguage={} model={} voice={}",
            userId,
            nativeLanguage,
            model,
            voiceName
        )

        runCatching {
            httpClient.webSocket(urlString = geminiWebSocketUrl()) {
                send(Frame.Text(buildSetupMessage(nativeLanguage, context.memoryFacts, context.history)))
                var setupCompleteReceived = false

                coroutineScope {
                    val setupTimeout = launch {
                        delay(12_000)
                        if (!setupCompleteReceived && appSession.isActive) {
                            appSession.sendAppMessage(
                                "error",
                                "Gemini Live did not confirm setup. Check the Live model name and API access."
                            )
                            appSession.close(
                                CloseReason(
                                    CloseReason.Codes.CANNOT_ACCEPT,
                                    "Gemini setup timed out"
                                )
                            )
                            close(
                                CloseReason(
                                    CloseReason.Codes.CANNOT_ACCEPT,
                                    "Gemini setup timed out"
                                )
                            )
                        }
                    }

                    val fromApp = launch {
                        for (frame in appSession.incoming) {
                            if (!isActive) break
                            if (frame !is Frame.Text) continue

                            val message = frame.readText().toJsonObjectOrNull() ?: continue
                            when (message["type"]?.jsonPrimitive?.contentOrNull) {
                                "audio" -> {
                                    val data = message["data"]?.jsonPrimitive?.contentOrNull.orEmpty()
                                    if (data.isNotBlank()) {
                                        appAudioChunks += 1
                                        if (appAudioChunks == 1 || appAudioChunks % 25 == 0) {
                                            logger.info(
                                                "Gemini Live received app audio chunk count={} base64Chars={}",
                                                appAudioChunks,
                                                data.length
                                            )
                                        }
                                        send(Frame.Text(buildGeminiAudioMessage(data)))
                                    }
                                }
                                "text" -> {
                                    val text = message["text"]?.jsonPrimitive?.contentOrNull.orEmpty()
                                    if (text.isNotBlank()) {
                                        send(Frame.Text(buildGeminiTextMessage(text)))
                                    }
                                }
                                "stop" -> close(CloseReason(CloseReason.Codes.NORMAL, "Client stopped"))
                                "audioStreamEnd" -> {
                                    send(Frame.Text(buildGeminiAudioStreamEndMessage()))
                                }
                            }
                        }
                    }

                    val fromGemini = launch {
                        val payloadBuffer = ByteArrayOutputStream()
                        for (frame in incoming) {
                            if (!isActive) break

                            val payload = frame.readJsonPayload(payloadBuffer) ?: continue
                            val response = payload.toJsonObjectOrNull() ?: continue
                            if (response["setupComplete"] != null) {
                                setupCompleteReceived = true
                                setupTimeout.cancel()
                                logger.info("Gemini Live setup complete.")
                                appSession.sendAppMessage("ready")
                            }

                            val serverContent = response["serverContent"]?.jsonObject
                            if (serverContent != null) {
                                val inputText = serverContent["inputTranscription"]
                                    ?.jsonObject
                                    ?.get("text")
                                    ?.jsonPrimitive
                                    ?.contentOrNull
                                    .orEmpty()
                                if (inputText.isNotBlank()) {
                                    lastUserTranscript = inputText.trim()
                                    logger.info("Gemini Live input transcript: {}", lastUserTranscript.take(120))
                                    chatMemoryService.saveMessage(userId, "user", lastUserTranscript)
                                    appSession.sendAppMessage("inputTranscript", inputText)
                                }

                                val outputText = serverContent["outputTranscription"]
                                    ?.jsonObject
                                    ?.get("text")
                                    ?.jsonPrimitive
                                    ?.contentOrNull
                                    .orEmpty()
                                if (outputText.isNotBlank()) {
                                    assistantTranscript.append(outputText)
                                    logger.info("Gemini Live output transcript chunk: {}", outputText.take(120))
                                    appSession.sendAppMessage("outputTranscript", outputText)
                                }

                                serverContent["modelTurn"]
                                    ?.jsonObject
                                    ?.get("parts")
                                    ?.let { parts -> sendAudioParts(appSession, parts) }

                                if (serverContent["interrupted"]?.jsonPrimitive?.contentOrNull == "true") {
                                    appSession.sendAppMessage("interrupted")
                                }

                                if (serverContent["turnComplete"]?.jsonPrimitive?.contentOrNull == "true") {
                                    val assistantText = assistantTranscript.toString().trim()
                                    if (assistantText.isNotBlank()) {
                                        chatMemoryService.saveMessage(userId, "assistant", assistantText)
                                        rememberExchange(userId, lastUserTranscript, assistantText)
                                    }
                                    assistantTranscript = StringBuilder()
                                    appSession.sendAppMessage("turnComplete")
                                }
                            }

                            response["error"]
                                ?.jsonObject
                                ?.get("message")
                                ?.jsonPrimitive
                                ?.contentOrNull
                                ?.let { message -> appSession.sendAppMessage("error", message) }
                        }

                        if (!setupCompleteReceived && appSession.isActive) {
                            appSession.sendAppMessage("error", "Gemini Live closed before setup completed.")
                        }
                    }

                    fromApp.invokeOnCompletion {
                        fromGemini.cancel()
                        setupTimeout.cancel()
                    }
                    fromGemini.invokeOnCompletion {
                        fromApp.cancel()
                        setupTimeout.cancel()
                    }
                    joinAll(fromApp, fromGemini, setupTimeout)
                }
            }
        }.onFailure { error ->
            if (appSession.isActive) {
                appSession.sendAppMessage("error", error.message ?: "Gemini Live connection failed.")
            }
            logger.warn("Gemini Live bridge failed.", error)
        }
    }

    private suspend fun rememberExchange(userId: String, userMessage: String, assistantReply: String) {
        if (userMessage.isBlank() || assistantReply.isBlank()) return
        runCatching {
            chatMemoryService.rememberFromExchange(
                userId = userId,
                userMessage = userMessage,
                assistantReply = assistantReply
            )
        }
    }

    private suspend fun sendAudioParts(appSession: DefaultWebSocketSession, parts: JsonElement) {
        val array = parts as? JsonArray ?: return
        array.forEach { part ->
            val inlineData = part.jsonObject["inlineData"]?.jsonObject ?: return@forEach
            val data = inlineData["data"]?.jsonPrimitive?.contentOrNull.orEmpty()
            val mimeType = inlineData["mimeType"]?.jsonPrimitive?.contentOrNull.orEmpty()
            val sampleRate = mimeType.extractPcmRate() ?: GEMINI_OUTPUT_SAMPLE_RATE
            if (data.isNotBlank()) {
                appSession.send(
                    Frame.Text(
                        json.encodeToString(
                            buildJsonObject {
                                put("type", "audio")
                                put("data", data)
                                put("sampleRate", sampleRate)
                                if (mimeType.isNotBlank()) {
                                    put("mimeType", mimeType)
                                }
                            }
                        )
                    )
                )
            }
        }
    }

    private fun buildSetupMessage(
        nativeLanguage: String,
        memories: List<String>,
        history: List<GroqChatMessage>
    ): String {
        return json.encodeToString(
            buildJsonObject {
                putJsonObject("setup") {
                    put("model", "models/$model")
                    putJsonObject("generationConfig") {
                        putJsonArray("responseModalities") {
                            add(JsonPrimitive("AUDIO"))
                        }
                        putJsonObject("speechConfig") {
                            putJsonObject("voiceConfig") {
                                putJsonObject("prebuiltVoiceConfig") {
                                    put("voiceName", voiceName)
                                }
                            }
                        }
                    }
                    putJsonObject("systemInstruction") {
                        putJsonArray("parts") {
                            add(
                                buildJsonObject {
                                    put("text", buildSystemInstruction(nativeLanguage, memories, history))
                                }
                            )
                        }
                    }
                    putJsonObject("realtimeInputConfig") {
                        putJsonObject("automaticActivityDetection") {
                            put("disabled", false)
                            put("startOfSpeechSensitivity", "START_SENSITIVITY_HIGH")
                            put("endOfSpeechSensitivity", "END_SENSITIVITY_HIGH")
                            put("prefixPaddingMs", 120)
                            put("silenceDurationMs", 700)
                        }
                        put("activityHandling", "START_OF_ACTIVITY_INTERRUPTS")
                        put("turnCoverage", "TURN_INCLUDES_ONLY_ACTIVITY")
                    }
                    putJsonObject("inputAudioTranscription") {}
                    putJsonObject("outputAudioTranscription") {}
                }
            }
        )
    }

    private fun buildSystemInstruction(
        nativeLanguage: String,
        memories: List<String>,
        history: List<GroqChatMessage>
    ): String {
        val memoryBlock = memories
            .take(12)
            .joinToString(separator = "\n") { "- $it" }
            .ifBlank { "- Sem memorias persistentes relevantes ainda." }

        val recentHistory = history
            .takeLast(8)
            .joinToString(separator = "\n") { "${it.role}: ${it.content.take(180)}" }
            .ifBlank { "Sem historico recente." }

        val nativeLanguageName = when (nativeLanguage) {
            "ru" -> "russo"
            "uk" -> "ucraniano"
            else -> "portugues do Brasil"
        }

        return """
            Voce e Rei Ayanami, tutora de ingles do Ayanami Learn.
            Esta e uma conversa por voz em tempo real. Fale de forma natural, curta, calma e humana.
            A lingua nativa do usuario e $nativeLanguageName.
            Use a lingua nativa para explicar, orientar e acolher. Use ingles para pratica, repeticao, exemplos e quando o usuario pedir ou tentar falar ingles.
            Se o usuario praticar ingles, corrija um ponto por vez e convide-o a repetir uma frase curta.
            Nao soe como leitura de roteiro. Evite listas longas. Prefira uma pergunta simples por turno.
            Memorias persistentes do usuario:
            $memoryBlock
            Historico recente:
            $recentHistory
        """.trimIndent()
    }

    private fun buildGeminiAudioMessage(base64Pcm: String): String {
        return json.encodeToString(
            buildJsonObject {
                putJsonObject("realtimeInput") {
                    putJsonObject("audio") {
                        put("data", base64Pcm)
                        put("mimeType", "audio/pcm;rate=$GEMINI_INPUT_SAMPLE_RATE")
                    }
                }
            }
        )
    }

    private fun buildGeminiTextMessage(text: String): String {
        return json.encodeToString(
            buildJsonObject {
                putJsonObject("realtimeInput") {
                    put("text", text)
                }
            }
        )
    }

    private fun buildGeminiAudioStreamEndMessage(): String {
        return json.encodeToString(
            buildJsonObject {
                putJsonObject("realtimeInput") {
                    put("audioStreamEnd", true)
                }
            }
        )
    }

    private suspend fun DefaultWebSocketSession.sendAppMessage(type: String, text: String? = null) {
        send(
            Frame.Text(
                json.encodeToString(
                    buildJsonObject {
                        put("type", type)
                        if (text != null) put("text", text)
                    }
                )
            )
        )
    }

    private fun String.toJsonObjectOrNull(): JsonObject? {
        return runCatching { json.parseToJsonElement(this).jsonObject }.getOrNull()
    }

    private fun Frame.readJsonPayload(payloadBuffer: ByteArrayOutputStream): String? {
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

    private fun geminiWebSocketUrl(): String {
        return "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent?key=$apiKey"
    }

    private fun String.extractPcmRate(): Int? {
        return Regex("""rate=(\d+)""")
            .find(this)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()
    }

    private companion object {
        const val GEMINI_INPUT_SAMPLE_RATE = 16_000
        const val GEMINI_OUTPUT_SAMPLE_RATE = 24_000
    }
}

private fun kotlinx.serialization.json.JsonObjectBuilder.putJsonObject(
    key: String,
    builder: kotlinx.serialization.json.JsonObjectBuilder.() -> Unit
) {
    put(key, buildJsonObject(builder))
}

private fun kotlinx.serialization.json.JsonObjectBuilder.putJsonArray(
    key: String,
    builder: kotlinx.serialization.json.JsonArrayBuilder.() -> Unit
) {
    put(key, buildJsonArray(builder))
}
