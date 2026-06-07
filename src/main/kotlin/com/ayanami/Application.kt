package com.ayanami

import com.ayanami.database.MongoManager
import com.ayanami.models.AuthResponse
import com.ayanami.models.EmailLoginRequest
import com.ayanami.models.EmailRegisterRequest
import com.ayanami.models.User
import com.ayanami.services.AiService
import com.ayanami.services.CartesiaAudioTtsService
import com.ayanami.services.ChatMemoryService
import com.ayanami.services.EmailPasswordAuthService
import com.ayanami.services.GeminiLiveVoiceService
import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOptions
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.core.readBytes
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.Serializable
import java.io.File
import kotlin.math.abs
import kotlin.math.sqrt

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

fun Application.module() {
    install(CallLogging)
    install(WebSockets)
    install(ContentNegotiation) {
        json()
    }

    val aiService = AiService()
    val emailPasswordAuthService = EmailPasswordAuthService()
    val cartesiaAudioTtsService = CartesiaAudioTtsService()
    val chatMemoryService = ChatMemoryService(aiService)
    val geminiLiveVoiceService = GeminiLiveVoiceService(chatMemoryService)

    // Initializes Firebase Admin when credentials are available.
    FirebaseAdmin.init()

    routing {
        get("/") {
            call.respondText("Ayanami Learn Backend Online!")
        }

        get("/health") {
            call.respondText("OK")
        }

        get("/api/audio/{fileName}") {
            val fileName = call.parameters["fileName"]
            val audioFile = fileName?.let(cartesiaAudioTtsService::resolveAudioFile)
            if (audioFile == null) {
                call.respond(HttpStatusCode.NotFound, ErrorResponse("Audio not found."))
                return@get
            }

            val contentType = if (audioFile.extension.equals("wav", ignoreCase = true)) {
                ContentType.parse("audio/wav")
            } else {
                ContentType.Audio.MPEG
            }
            call.respondBytes(audioFile.readBytes(), contentType)
        }

        get("/api/memory/{userId}") {
            val userId = call.parameters["userId"].toChatUserId()
            call.respond(chatMemoryService.listMemories(userId))
        }

        webSocket("/api/live/voice") {
            val userId = call.request.queryParameters["userId"].toChatUserId()
            val nativeLanguage = call.request.queryParameters["nativeLanguage"].toNativeLanguageCode()
            geminiLiveVoiceService.bridge(
                appSession = this,
                userId = userId,
                nativeLanguage = nativeLanguage
            )
        }

        post("/api/auth/register") {
            val request = call.receive<EmailRegisterRequest>()
            if (request.fullName.isBlank() || request.email.isBlank() || request.password.length < 6) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Nome, email e senha com 6+ caracteres sao obrigatorios."))
                return@post
            }

            runCatching {
                emailPasswordAuthService.register(
                    fullName = request.fullName.trim(),
                    email = request.email.trim(),
                    password = request.password
                )
            }.onSuccess { authResponse ->
                upsertUserFromAuth(authResponse, request.nativeLanguage.toNativeLanguageCode())
                call.respond(HttpStatusCode.Created, authResponse)
            }.onFailure { error ->
                call.respond(HttpStatusCode.BadRequest, ErrorResponse(error.message ?: "Falha ao criar conta."))
            }
        }

        post("/api/auth/login") {
            val request = call.receive<EmailLoginRequest>()
            if (request.email.isBlank() || request.password.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Email e senha sao obrigatorios."))
                return@post
            }

            runCatching {
                emailPasswordAuthService.login(
                    email = request.email.trim(),
                    password = request.password
                )
            }.onSuccess { authResponse ->
                upsertUserFromAuth(authResponse)
                call.respond(authResponse)
            }.onFailure { error ->
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse(error.message ?: "Falha ao entrar."))
            }
        }

        post("/api/chat") {
            val request = call.receive<ChatRequest>()
            val userMessage = request.message.trim()
            if (userMessage.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Message cannot be blank."))
                return@post
            }

            val userId = request.userId.toChatUserId()
            val nativeLanguage = request.nativeLanguage.toNativeLanguageCode()
            val context = chatMemoryService.loadContext(userId)
            chatMemoryService.saveMessage(userId = userId, role = "user", content = userMessage)

            val reply = aiService.getTutorResponse(
                userMessage = userMessage,
                history = context.history,
                memories = context.memoryFacts,
                nativeLanguage = nativeLanguage
            )
            chatMemoryService.saveMessage(userId = userId, role = "assistant", content = reply)
            runCatching {
                chatMemoryService.rememberFromExchange(
                    userId = userId,
                    userMessage = userMessage,
                    assistantReply = reply
                )
            }.onFailure { error ->
                application.log.warn("Memory extraction failed. The chat reply was still returned.", error)
            }

            val audioReplyUrl = call.tryCreateAudioReplyUrl(reply, cartesiaAudioTtsService, nativeLanguage)
            call.respond(ChatResponse(reply = reply, audioReplyUrl = audioReplyUrl))
        }

        post("/api/chat/voice") {
            val upload = call.receiveVoiceAudio()
            if (upload == null) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ChatResponse(reply = "Nao recebi nenhum arquivo de audio.")
                )
                return@post
            }

            try {
                application.log.info(
                    "Voice upload received: {} bytes, contentType={}, file={}",
                    upload.file.length(),
                    upload.contentType,
                    upload.file.name
                )
                maybeSaveDebugVoiceUpload(upload.file)
                val wavStats = upload.file.readPcm16WavStatsOrNull()
                if (wavStats != null) {
                    application.log.info(
                        "Voice audio level: durationSeconds={}, peak={}, rms={}",
                        wavStats.durationSeconds,
                        wavStats.peak,
                        wavStats.rms
                    )
                    if (wavStats.isAlmostSilent) {
                        call.respond(
                            ChatResponse(
                                reply = "Recebi o audio, mas ele chegou quase sem volume. No emulador, confira o microfone do Windows ou teste em um celular fisico."
                            )
                        )
                        return@post
                    }
                }

                val transcript = aiService.transcribeAudio(upload.file, upload.contentType)
                if (!transcript.hasSpeechText()) {
                    call.respond(ChatResponse(reply = "Nao consegui ouvir uma fala clara nesse audio. Tente novamente."))
                    return@post
                }

                application.log.info("Voice transcript: {}", transcript)
                val userId = upload.userId.toChatUserId()
                val nativeLanguage = upload.nativeLanguage.toNativeLanguageCode()
                val context = chatMemoryService.loadContext(userId)
                chatMemoryService.saveMessage(userId = userId, role = "user", content = transcript)

                val reply = runCatching {
                    aiService.getTutorResponse(
                        userMessage = transcript,
                        history = context.history,
                        memories = context.memoryFacts,
                        nativeLanguage = nativeLanguage
                    )
                }.getOrElse { error ->
                    application.log.warn("Voice reply failed after successful transcript.", error)
                    "Consegui transcrever sua fala, mas a IA nao respondeu agora. Tente enviar novamente."
                }
                chatMemoryService.saveMessage(userId = userId, role = "assistant", content = reply)
                runCatching {
                    chatMemoryService.rememberFromExchange(
                        userId = userId,
                        userMessage = transcript,
                        assistantReply = reply
                    )
                }.onFailure { error ->
                    application.log.warn("Memory extraction failed. The voice reply was still returned.", error)
                }

                val audioReplyUrl = call.tryCreateAudioReplyUrl(reply, cartesiaAudioTtsService, nativeLanguage)
                call.respond(ChatResponse(reply = reply, audioReplyUrl = audioReplyUrl, transcript = transcript))
            } catch (error: Exception) {
                call.respond(
                    HttpStatusCode.BadGateway,
                    ChatResponse(reply = "Erro ao processar audio: ${error.message ?: "falha desconhecida."}")
                )
            } finally {
                upload.file.delete()
            }
        }

        post("/api/users/register") {
            val user = call.receive<User>()
            val authorization = call.request.headers[HttpHeaders.Authorization]
            val firebaseUidFromToken = FirebaseAdmin.verifyBearerTokenOrNull(authorization)
            if (firebaseUidFromToken == null) {
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Valid Firebase token is required."))
                return@post
            }
            if (firebaseUidFromToken != user.firebaseUid) {
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Firebase token does not match user."))
                return@post
            }

            upsertUser(user)
            call.respondText("Usuario registrado com sucesso!", status = HttpStatusCode.Created)
        }
    }
}

private data class VoiceAudioUpload(
    val file: File,
    val contentType: String,
    val userId: String?,
    val nativeLanguage: String?
)

private suspend fun ApplicationCall.receiveVoiceAudio(): VoiceAudioUpload? {
    val multipart = receiveMultipart()
    var uploadedFile: File? = null
    var uploadedContentType = "audio/mp4"
    var uploadedUserId: String? = null
    var uploadedNativeLanguage: String? = null

    multipart.forEachPart { part ->
        when (part) {
            is PartData.FileItem -> {
                if (uploadedFile == null) {
                    val extension = part.originalFileName
                        ?.substringAfterLast('.', "m4a")
                        ?.takeIf { it.isNotBlank() }
                        ?: "m4a"
                    val tempFile = File.createTempFile("ayanami-voice-", ".$extension")
                    part.provider().use { input ->
                        tempFile.writeBytes(input.readBytes())
                    }
                    uploadedFile = tempFile
                    uploadedContentType = part.headers[HttpHeaders.ContentType] ?: uploadedContentType
                }
            }
            is PartData.FormItem -> {
                when (part.name) {
                    "userId" -> uploadedUserId = part.value
                    "nativeLanguage" -> uploadedNativeLanguage = part.value
                }
            }
            else -> Unit
        }
        part.dispose()
    }

    return uploadedFile?.let { file ->
        if (file.length() == 0L) {
            file.delete()
            null
        } else {
            VoiceAudioUpload(
                file = file,
                contentType = uploadedContentType,
                userId = uploadedUserId,
                nativeLanguage = uploadedNativeLanguage
            )
        }
    }
}

private fun String?.toChatUserId(): String {
    val normalized = this
        ?.trim()
        ?.replace(Regex("[^A-Za-z0-9_.@-]"), "_")
        ?.take(120)
        ?: ""

    return normalized.ifBlank { "demo_user" }
}

private fun String?.toNativeLanguageCode(): String {
    return when (this?.trim()?.lowercase()) {
        "uk", "ua", "ukrainian", "ucraniano", "ucranian" -> "uk"
        "ru", "russian", "russo" -> "ru"
        else -> "pt"
    }
}

private fun String.hasSpeechText(): Boolean {
    return any { it.isLetterOrDigit() }
}

private suspend fun ApplicationCall.tryCreateAudioReplyUrl(
    reply: String,
    cartesiaAudioTtsService: CartesiaAudioTtsService,
    nativeLanguage: String
): String? {
    return runCatching {
        cartesiaAudioTtsService.synthesizeToFile(reply, nativeLanguage)?.let { audioFile ->
            "${publicBaseUrl().trimEnd('/')}/api/audio/${audioFile.name}"
        }
    }.onFailure { error ->
        application.log.warn("TTS generation failed. The text reply will still be returned.", error)
    }.getOrNull()
}

private fun publicBaseUrl(): String {
    return System.getenv("PUBLIC_BASE_URL")
        ?: io.github.cdimascio.dotenv.dotenv()["PUBLIC_BASE_URL"]
        ?: "http://10.0.2.2:8080"
}

private data class WavAudioStats(
    val durationSeconds: Double,
    val peak: Int,
    val rms: Double
) {
    val isAlmostSilent: Boolean
        get() = peak < 80 || rms < 8.0
}

private fun File.readPcm16WavStatsOrNull(): WavAudioStats? {
    val bytes = readBytes()
    if (bytes.size < 44) return null
    if (!bytes.matchesAscii(0, "RIFF") || !bytes.matchesAscii(8, "WAVE")) return null

    var audioFormat = 0
    var channels = 0
    var sampleRate = 0
    var bitsPerSample = 0
    var dataOffset = -1
    var dataSize = 0
    var offset = 12

    while (offset + 8 <= bytes.size) {
        val chunkId = bytes.asciiChunkId(offset)
        val chunkSize = bytes.readIntLe(offset + 4)
        if (chunkSize < 0) return null

        val chunkDataOffset = offset + 8
        if (chunkDataOffset + chunkSize > bytes.size) break

        when (chunkId) {
            "fmt " -> {
                if (chunkSize >= 16) {
                    audioFormat = bytes.readShortLe(chunkDataOffset).toInt()
                    channels = bytes.readShortLe(chunkDataOffset + 2).toInt()
                    sampleRate = bytes.readIntLe(chunkDataOffset + 4)
                    bitsPerSample = bytes.readShortLe(chunkDataOffset + 14).toInt()
                }
            }
            "data" -> {
                dataOffset = chunkDataOffset
                dataSize = chunkSize
            }
        }

        offset = chunkDataOffset + chunkSize + (chunkSize and 1)
    }

    if (audioFormat != 1 || channels <= 0 || sampleRate <= 0 || bitsPerSample != 16) return null
    if (dataOffset < 0 || dataSize < 2) return null

    val sampleCount = dataSize / 2
    var peak = 0
    var sumSquares = 0.0

    for (sampleIndex in 0 until sampleCount) {
        val sample = bytes.readShortLe(dataOffset + sampleIndex * 2).toInt()
        val absoluteSample = if (sample == Short.MIN_VALUE.toInt()) Short.MAX_VALUE.toInt() else abs(sample)
        if (absoluteSample > peak) peak = absoluteSample
        sumSquares += sample.toDouble() * sample.toDouble()
    }

    val durationSeconds = sampleCount.toDouble() / channels / sampleRate
    val rms = sqrt(sumSquares / sampleCount)
    return WavAudioStats(durationSeconds = durationSeconds, peak = peak, rms = rms)
}

private fun ByteArray.matchesAscii(offset: Int, value: String): Boolean {
    if (offset + value.length > size) return false
    return value.indices.all { index -> this[offset + index].toInt().toChar() == value[index] }
}

private fun ByteArray.asciiChunkId(offset: Int): String {
    return String(this, offset, 4, Charsets.US_ASCII)
}

private fun ByteArray.readIntLe(offset: Int): Int {
    return (this[offset].toInt() and 0xff) or
        ((this[offset + 1].toInt() and 0xff) shl 8) or
        ((this[offset + 2].toInt() and 0xff) shl 16) or
        ((this[offset + 3].toInt() and 0xff) shl 24)
}

private fun ByteArray.readShortLe(offset: Int): Short {
    val value = (this[offset].toInt() and 0xff) or
        ((this[offset + 1].toInt() and 0xff) shl 8)
    return value.toShort()
}

private fun maybeSaveDebugVoiceUpload(file: File) {
    val enabled = System.getenv("DEBUG_SAVE_VOICE_UPLOADS")
        ?: io.github.cdimascio.dotenv.dotenv()["DEBUG_SAVE_VOICE_UPLOADS"]
        ?: "false"

    if (enabled.lowercase() != "true") return

    val debugDir = File("build/tmp/voice-debug")
    debugDir.mkdirs()
    file.copyTo(File(debugDir, "last-voice${file.extension.takeIf { it.isNotBlank() }?.let { ".$it" } ?: ""}"), overwrite = true)
    file.copyTo(File(debugDir, "voice-${System.currentTimeMillis()}${file.extension.takeIf { it.isNotBlank() }?.let { ".$it" } ?: ""}"), overwrite = true)
}

private suspend fun upsertUserFromAuth(authResponse: AuthResponse, nativeLanguage: String = "pt") {
    upsertUser(
        User(
            firebaseUid = authResponse.firebaseUid,
            fullName = authResponse.fullName.ifBlank { authResponse.email.substringBefore("@") },
            email = authResponse.email,
            nativeLanguage = nativeLanguage,
            streak = 0,
            rankPoints = 0,
            aiPersonality = "Friendly"
        )
    )
}

private suspend fun upsertUser(user: User) {
    val usersCollection = MongoManager.database.getCollection<User>("users")
    val existingUser = usersCollection
        .find(Filters.eq(User::firebaseUid.name, user.firebaseUid))
        .firstOrNull()

    val userToSave = user.copy(
        id = existingUser?.id ?: user.id,
        fullName = user.fullName.ifBlank { existingUser?.fullName ?: user.email.substringBefore("@") },
        nativeLanguage = user.nativeLanguage.ifBlank { existingUser?.nativeLanguage ?: "pt" },
        streak = existingUser?.streak ?: user.streak,
        rankPoints = existingUser?.rankPoints ?: user.rankPoints,
        aiPersonality = existingUser?.aiPersonality ?: user.aiPersonality
    )

    usersCollection.replaceOne(
        filter = Filters.eq(User::firebaseUid.name, user.firebaseUid),
        replacement = userToSave,
        options = ReplaceOptions().upsert(true)
    )
}

@Serializable
data class ChatRequest(
    val message: String,
    val userId: String,
    val nativeLanguage: String = "pt"
)

@Serializable
data class ChatResponse(
    val reply: String,
    val audioReplyUrl: String? = null,
    val transcript: String? = null
)

@Serializable
data class ErrorResponse(val message: String)
