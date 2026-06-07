package com.ayanami.services

import io.github.cdimascio.dotenv.dotenv
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID

@Serializable
private data class CartesiaVoiceSpecifier(
    val id: String
)

@Serializable
private data class CartesiaOutputFormat(
    val container: String,
    val encoding: String? = null,
    @SerialName("sample_rate")
    val sampleRate: Int
)

@Serializable
private data class CartesiaGenerationConfig(
    val volume: Double = 0.9,
    val speed: Double = 0.78
)

@Serializable
private data class CartesiaTtsRequest(
    @SerialName("model_id")
    val modelId: String,
    val transcript: String,
    val voice: CartesiaVoiceSpecifier,
    @SerialName("output_format")
    val outputFormat: CartesiaOutputFormat,
    val language: String? = null,
    @SerialName("generation_config")
    val generationConfig: CartesiaGenerationConfig = CartesiaGenerationConfig()
)

private data class SpeechSegment(
    val text: String,
    val language: String
)

private data class PcmWavAudio(
    val sampleRate: Int,
    val channels: Int,
    val bitsPerSample: Int,
    val data: ByteArray
)

class CartesiaAudioTtsService {
    private val dotenv = dotenv()
    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 90_000
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = 90_000
        }
    }

    private val apiKey = System.getenv("CARTESIA_API_KEY")
        ?: dotenv["CARTESIA_API_KEY"]
        ?: ""
    private val apiVersion = System.getenv("CARTESIA_VERSION")
        ?: dotenv["CARTESIA_VERSION"]
        ?: "2026-03-01"
    private val modelId = System.getenv("CARTESIA_TTS_MODEL")
        ?: dotenv["CARTESIA_TTS_MODEL"]
        ?: "sonic-3.5"
    private val voiceId = System.getenv("CARTESIA_VOICE_ID")
        ?: dotenv["CARTESIA_VOICE_ID"]
        ?: PORTUGUESE_VOICE_ID
    private val portugueseVoiceId = System.getenv("CARTESIA_VOICE_ID_PT")
        ?: dotenv["CARTESIA_VOICE_ID_PT"]
        ?: PORTUGUESE_VOICE_ID
    private val russianVoiceId = System.getenv("CARTESIA_VOICE_ID_RU")
        ?: dotenv["CARTESIA_VOICE_ID_RU"]
        ?: RUSSIAN_VOICE_ID
    private val ukrainianVoiceId = System.getenv("CARTESIA_VOICE_ID_UK")
        ?: dotenv["CARTESIA_VOICE_ID_UK"]
        ?: UKRAINIAN_VOICE_ID
    private val englishVoiceId = System.getenv("CARTESIA_VOICE_ID_EN")
        ?: dotenv["CARTESIA_VOICE_ID_EN"]
        ?: ENGLISH_VOICE_ID
    private val audioFormat = System.getenv("CARTESIA_TTS_FORMAT")
        ?: dotenv["CARTESIA_TTS_FORMAT"]
        ?: "wav"
    private val sampleRate = (System.getenv("CARTESIA_TTS_SAMPLE_RATE")
        ?: dotenv["CARTESIA_TTS_SAMPLE_RATE"]
        ?: "44100").toInt()
    private val speed = (System.getenv("CARTESIA_TTS_SPEED")
        ?: dotenv["CARTESIA_TTS_SPEED"]
        ?: "0.78").toDouble()

    private val audioDir = File("build/tmp/tts-audio")

    val isConfigured: Boolean
        get() = apiKey.isNotBlank() && apiKey.startsWith("sk_car_")

    suspend fun synthesizeToFile(text: String, nativeLanguage: String = "pt"): File? {
        if (!isConfigured) return null

        val transcript = text.toSpeechText()
        if (transcript.isBlank()) return null

        audioDir.mkdirs()
        pruneOldAudioFiles()

        val segments = transcript.toSpeechSegments(nativeLanguage)
        val extension = if (audioFormat.equals("mp3", ignoreCase = true) && segments.size == 1) "mp3" else "wav"
        val audioFile = File(audioDir, "ayanami-${System.currentTimeMillis()}-${UUID.randomUUID()}.$extension")

        if (extension == "mp3") {
            audioFile.writeBytes(
                synthesizeChunk(
                    transcript = segments.first().text,
                    language = segments.first().language,
                    format = "mp3"
                )
            )
            return audioFile
        }

        val audioChunks = segments.map { segment ->
            synthesizeChunk(
                transcript = segment.text,
                language = segment.language,
                format = "wav"
            ).toPcmWavAudioOrNull() ?: error("Cartesia returned an unsupported WAV file.")
        }

        audioFile.writeBytes(audioChunks.toSingleWavBytes())
        return audioFile
    }

    private suspend fun synthesizeChunk(
        transcript: String,
        language: String,
        format: String
    ): ByteArray {
        val response = httpClient.post(CARTESIA_TTS_URL) {
            bearerAuth(apiKey)
            header("Cartesia-Version", apiVersion)
            contentType(ContentType.Application.Json)
            setBody(
                CartesiaTtsRequest(
                    modelId = modelId,
                    transcript = transcript.toCartesiaTranscript().take(MAX_SEGMENT_CHARS),
                    voice = CartesiaVoiceSpecifier(id = language.toVoiceId()),
                    outputFormat = CartesiaOutputFormat(
                        container = format,
                        encoding = if (format == "wav") "pcm_s16le" else null,
                        sampleRate = sampleRate
                    ),
                    language = language,
                    generationConfig = CartesiaGenerationConfig(volume = TTS_VOLUME, speed = speed)
                )
            )
        }

        if (!response.status.isSuccess()) {
            error("Cartesia TTS failed (${response.status.value}): ${response.bodyAsText()}")
        }

        return response.body<ByteArray>()
    }

    fun resolveAudioFile(fileName: String): File? {
        if (!fileName.matches(Regex("[A-Za-z0-9._-]+"))) return null

        val file = File(audioDir, fileName).canonicalFile
        val canonicalDir = audioDir.canonicalFile
        return file.takeIf {
            it.exists() && it.isFile && it.path.startsWith(canonicalDir.path)
        }
    }

    private fun String.toSpeechText(): String {
        return replace(Regex("\\*\\*(.*?)\\*\\*"), "$1")
            .replace(Regex("`([^`]*)`"), "$1")
            .replace(Regex("[*_#>`]"), "")
            .replace(Regex("\\[(.*?)\\]\\((.*?)\\)"), "$1")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun String.toSpeechSegments(nativeLanguage: String): List<SpeechSegment> {
        val nativeCartesiaLanguage = nativeLanguage.toCartesiaLanguage()
        val segments = mutableListOf<SpeechSegment>()
        val quotePattern = Regex("\"([^\"]+)\"")
        var currentIndex = 0

        quotePattern.findAll(this).forEach { match ->
            val before = substring(currentIndex, match.range.first).trimForSpeechSegment()
            if (before.isNotBlank()) {
                segments += SpeechSegment(before, nativeCartesiaLanguage)
            }

            val quoted = match.groups[1]?.value.orEmpty().trimForSpeechSegment()
            if (quoted.isNotBlank()) {
                segments += SpeechSegment(
                    text = quoted,
                    language = if (quoted.shouldUseEnglishVoice()) "en" else nativeCartesiaLanguage
                )
            }
            currentIndex = match.range.last + 1
        }

        val tail = substring(currentIndex).trimForSpeechSegment()
        if (tail.isNotBlank()) {
            segments += SpeechSegment(tail, nativeCartesiaLanguage)
        }

        return segments
            .fold(mutableListOf<SpeechSegment>()) { acc, segment ->
                val previous = acc.lastOrNull()
                if (previous != null && previous.language == segment.language) {
                    acc[acc.lastIndex] = previous.copy(text = "${previous.text} ${segment.text}".take(MAX_SEGMENT_CHARS))
                } else {
                    acc += segment.copy(text = segment.text.take(MAX_SEGMENT_CHARS))
                }
                acc
            }
            .take(MAX_SEGMENTS)
            .ifEmpty { listOf(SpeechSegment(this.take(MAX_SEGMENT_CHARS), nativeCartesiaLanguage)) }
    }

    private fun String.toCartesiaLanguage(): String {
        return when (lowercase()) {
            "uk", "ua" -> "uk"
            "ru" -> "ru"
            "en" -> "en"
            else -> "pt"
        }
    }

    private fun String.toVoiceId(): String {
        return when (lowercase()) {
            "en" -> englishVoiceId
            "ru" -> russianVoiceId
            "uk" -> ukrainianVoiceId
            "pt" -> portugueseVoiceId
            else -> voiceId
        }
    }

    private fun String.shouldUseEnglishVoice(): Boolean {
        if (any { it.code in CYRILLIC_CODE_RANGE }) {
            return false
        }
        if (any { it.code > ASCII_MAX_CODE }) {
            return false
        }

        val words = lowercase().split(Regex("[^a-z']+")).filter { it.isNotBlank() }
        if (words.isEmpty()) return false
        if (words.any { it in PORTUGUESE_HINT_WORDS }) return false

        val englishHints = setOf(
            "a", "an", "the", "i", "you", "your", "he", "she", "we", "they", "am", "are",
            "is", "was", "were", "do", "does", "did", "have", "has", "had", "can", "could",
            "want", "need", "like", "help", "go", "went", "walk", "walked", "buy", "bought",
            "drink", "drank", "read", "speak", "practice", "english", "yesterday", "today",
            "tomorrow", "school", "weather", "book", "park", "simple", "morning", "night"
        )
        return words.any { it in englishHints }
    }

    private fun String.trimForSpeechSegment(): String {
        return replace(Regex("\\s+"), " ")
            .trim()
            .trim(',', ';', ':', '-', ' ')
    }

    private fun String.toCartesiaTranscript(): String {
        return replace(Regex("\\s+"), " ")
            .replace("...", ".")
            .replace(":", ".")
            .trim()
            .let { text ->
                if (text.lastOrNull() in listOf('.', '!', '?')) text else "$text."
            }
    }

    private fun ByteArray.toPcmWavAudioOrNull(): PcmWavAudio? {
        if (size < WAV_HEADER_MIN_BYTES) return null
        if (!matchesAscii(0, "RIFF") || !matchesAscii(8, "WAVE")) return null

        var audioFormat = 0
        var channels = 0
        var sampleRate = 0
        var bitsPerSample = 0
        var dataOffset = -1
        var dataSize = 0
        var offset = 12

        while (offset + 8 <= size) {
            val chunkId = asciiChunkId(offset)
            val chunkSize = readIntLe(offset + 4)
            val chunkDataOffset = offset + 8
            if (chunkSize < 0 && chunkId != "data") return null

            val effectiveChunkSize = if (chunkSize < 0) {
                size - chunkDataOffset
            } else {
                chunkSize
            }
            if (chunkDataOffset + effectiveChunkSize > size) return null

            when (chunkId) {
                "fmt " -> {
                    if (effectiveChunkSize >= 16) {
                        audioFormat = readShortLe(chunkDataOffset).toInt()
                        channels = readShortLe(chunkDataOffset + 2).toInt()
                        sampleRate = readIntLe(chunkDataOffset + 4)
                        bitsPerSample = readShortLe(chunkDataOffset + 14).toInt()
                    }
                }
                "data" -> {
                    dataOffset = chunkDataOffset
                    dataSize = effectiveChunkSize
                }
            }

            offset = chunkDataOffset + effectiveChunkSize + (effectiveChunkSize and 1)
        }

        if (audioFormat != PCM_FORMAT || channels <= 0 || sampleRate <= 0 || bitsPerSample != 16) return null
        if (dataOffset < 0 || dataSize <= 0) return null

        return PcmWavAudio(
            sampleRate = sampleRate,
            channels = channels,
            bitsPerSample = bitsPerSample,
            data = copyOfRange(dataOffset, dataOffset + dataSize)
        )
    }

    private fun List<PcmWavAudio>.toSingleWavBytes(): ByteArray {
        val first = first()
        val mergedPcm = ByteArrayOutputStream()
        forEachIndexed { index, audio ->
            require(audio.sampleRate == first.sampleRate)
            require(audio.channels == first.channels)
            require(audio.bitsPerSample == first.bitsPerSample)

            mergedPcm.write(audio.withSoftEdges().data)
            if (index != lastIndex) {
                mergedPcm.write(first.silenceBytes(INTER_SEGMENT_SILENCE_MS))
            }
        }

        return writePcmWav(
            sampleRate = first.sampleRate,
            channels = first.channels,
            bitsPerSample = first.bitsPerSample,
            pcmData = mergedPcm.toByteArray()
        )
    }

    private fun PcmWavAudio.withSoftEdges(): PcmWavAudio {
        val bytesPerFrame = channels * bitsPerSample / 8
        val frameCount = data.size / bytesPerFrame
        val fadeFrames = minOf(frameCount / 2, sampleRate * FADE_MS / 1_000)
        if (fadeFrames <= 1) return this

        val softened = data.copyOf()
        for (frame in 0 until fadeFrames) {
            val fadeInGain = frame.toDouble() / fadeFrames
            val fadeOutGain = (fadeFrames - frame).toDouble() / fadeFrames
            for (channel in 0 until channels) {
                softened.scalePcm16(frame * bytesPerFrame + channel * 2, fadeInGain)
                softened.scalePcm16((frameCount - frame - 1) * bytesPerFrame + channel * 2, fadeOutGain)
            }
        }

        return copy(data = softened)
    }

    private fun PcmWavAudio.silenceBytes(durationMs: Int): ByteArray {
        val bytesPerFrame = channels * bitsPerSample / 8
        val frameCount = sampleRate * durationMs / 1_000
        return ByteArray(frameCount * bytesPerFrame)
    }

    private fun writePcmWav(
        sampleRate: Int,
        channels: Int,
        bitsPerSample: Int,
        pcmData: ByteArray
    ): ByteArray {
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8

        return ByteArrayOutputStream().apply {
            writeAscii("RIFF")
            writeIntLe(36 + pcmData.size)
            writeAscii("WAVE")
            writeAscii("fmt ")
            writeIntLe(16)
            writeShortLe(PCM_FORMAT)
            writeShortLe(channels)
            writeIntLe(sampleRate)
            writeIntLe(byteRate)
            writeShortLe(blockAlign)
            writeShortLe(bitsPerSample)
            writeAscii("data")
            writeIntLe(pcmData.size)
            write(pcmData)
        }.toByteArray()
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

    private fun ByteArray.scalePcm16(offset: Int, gain: Double) {
        val sample = readShortLe(offset).toInt()
        val scaledSample = (sample * gain)
            .toInt()
            .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
        writeShortLe(offset, scaledSample)
    }

    private fun ByteArray.writeShortLe(offset: Int, value: Int) {
        this[offset] = (value and 0xff).toByte()
        this[offset + 1] = (value shr 8 and 0xff).toByte()
    }

    private fun ByteArrayOutputStream.writeAscii(value: String) {
        write(value.toByteArray(Charsets.US_ASCII))
    }

    private fun ByteArrayOutputStream.writeIntLe(value: Int) {
        write(value and 0xff)
        write(value shr 8 and 0xff)
        write(value shr 16 and 0xff)
        write(value shr 24 and 0xff)
    }

    private fun ByteArrayOutputStream.writeShortLe(value: Int) {
        write(value and 0xff)
        write(value shr 8 and 0xff)
    }

    private fun pruneOldAudioFiles() {
        val cutoff = System.currentTimeMillis() - AUDIO_TTL_MILLIS
        audioDir.listFiles()
            ?.filter { file -> file.isFile && file.lastModified() < cutoff }
            ?.forEach { file -> file.delete() }
    }

    private companion object {
        const val CARTESIA_TTS_URL = "https://api.cartesia.ai/tts/bytes"
        const val PORTUGUESE_VOICE_ID = "8d826d43-20ad-4c56-8d37-1048eccca1bf"
        const val RUSSIAN_VOICE_ID = "779673f3-895f-4935-b6b5-b031dc78b319"
        const val UKRAINIAN_VOICE_ID = "05ffab9c-d380-4909-8375-cd12f59238c3"
        const val ENGLISH_VOICE_ID = "3c7dfd17-3fa8-47aa-aacc-6313fe025442"
        const val MAX_SEGMENT_CHARS = 420
        const val MAX_SEGMENTS = 8
        const val WAV_HEADER_MIN_BYTES = 44
        const val PCM_FORMAT = 1
        const val INTER_SEGMENT_SILENCE_MS = 320
        const val FADE_MS = 18
        const val TTS_VOLUME = 0.9
        const val ASCII_MAX_CODE = 127
        val CYRILLIC_CODE_RANGE = 0x0400..0x04FF
        val PORTUGUESE_HINT_WORDS = setOf(
            "eu", "voce", "voces", "ele", "ela", "nos", "meu", "minha", "seu", "sua",
            "um", "uma", "uns", "umas", "o", "os", "as", "de", "do", "da", "dos",
            "das", "em", "no", "na", "para", "por", "com", "livro", "ontem", "hoje",
            "amanha", "comprei", "estudei", "trabalhou", "fui", "foi"
        )
        const val AUDIO_TTL_MILLIS = 30 * 60 * 1_000L
    }
}
