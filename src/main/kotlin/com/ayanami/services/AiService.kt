package com.ayanami.services

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import io.github.cdimascio.dotenv.dotenv
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class GroqChatCompletionRequest(
    val model: String,
    val messages: List<GroqChatMessage>,
    val temperature: Double = 0.7,
    val max_tokens: Int = 1024,
    val stream: Boolean = false
)

@Serializable
data class GroqChatMessage(
    val role: String,
    val content: String
)

@Serializable
data class GroqChatCompletionResponse(
    val id: String,
    val choices: List<GroqChoice>
)

@Serializable
data class GroqChoice(
    val index: Int,
    val message: GroqChatMessage,
    val finish_reason: String
)

@Serializable
data class GroqTranscriptionResponse(
    val text: String
)

@Serializable
data class ExtractedMemory(
    val key: String,
    val value: String,
    val category: String = "general"
)

@Serializable
private data class MemoryExtractionResponse(
    val memories: List<ExtractedMemory> = emptyList()
)

class AiService {
    private val dotenv = dotenv()

    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    private val groqApiKey = System.getenv("GROQ_API_KEY")
        ?: dotenv["GROQ_API_KEY"]
        ?: "SUA_CHAVE_API_GROQ_AQUI"
    private val groqChatModel = System.getenv("GROQ_CHAT_MODEL")
        ?: dotenv["GROQ_CHAT_MODEL"]
        ?: "llama-3.1-8b-instant"
    private val groqTranscriptionModel = System.getenv("GROQ_TRANSCRIPTION_MODEL")
        ?: dotenv["GROQ_TRANSCRIPTION_MODEL"]
        ?: "whisper-large-v3-turbo"
    private val groqChatUrl = "https://api.groq.com/openai/v1/chat/completions"
    private val groqTranscriptionUrl = "https://api.groq.com/openai/v1/audio/transcriptions"

    suspend fun getTutorResponse(
        userMessage: String,
        history: List<GroqChatMessage>,
        memories: List<String> = emptyList(),
        nativeLanguage: String = "pt"
    ): String {
        val memoryBlock = if (memories.isEmpty()) {
            "Nenhuma memoria persistente registrada ainda."
        } else {
            memories.joinToString(separator = "\n") { memory -> "- $memory" }
        }
        val languagePolicy = nativeLanguage.toLanguagePolicy()

        val systemPrompt = GroqChatMessage(
            role = "system",
            content = """
                Voce e Rei Ayanami, uma tutora de ingles de um aplicativo chamado Ayanami Learn.
                Sua personalidade e calma, analitica, mas secretamente atenciosa.
                Seu objetivo e ajudar o usuario a praticar conversacao em ingles.

                Lingua nativa do usuario:
                ${languagePolicy.name}

                Politica de idioma:
                ${languagePolicy.instructions}

                Memorias persistentes sobre este usuario:
                $memoryBlock

                Use essas memorias de forma natural. Nao diga que esta lendo uma memoria; apenas demonstre continuidade, cuidado e contexto.

                Regras:
                1. Se o usuario cometer um erro gramatical, corrija-o educadamente antes de continuar a conversa.
                2. Mantenha as respostas curtas, com no maximo 1 ou 2 frases curtas e cerca de 180 caracteres quando possivel, para ficar natural em voz.
                3. Nao use Markdown, asteriscos, listas ou blocos de codigo; responda em texto limpo para ser lido em voz alta.
                4. Quando der exemplos, frases para repetir, exercicios ou correcoes de ingles, a frase-alvo deve estar em ingles e entre aspas duplas.
                5. Use aspas duplas somente para frases de pratica em ingles. Nunca coloque texto em portugues, russo ou ucraniano dentro de aspas duplas.
                6. Se o usuario pedir uma frase para repetir, use uma frase natural em ingles, por exemplo: Pratique: "I bought a book yesterday."
                7. Nao use aspas para nomes gramaticais como past simple; use aspas apenas para a frase que o usuario deve praticar.
                8. A lingua nativa serve para explicar, orientar e dar feedback; o conteudo de pratica deve ser ingles.
                9. Ocasionalmente, sugira um novo topico de conversa se o assunto morrer.
            """.trimIndent()
        )

        val messages = mutableListOf(systemPrompt)
        messages.addAll(history)
        messages.add(GroqChatMessage(role = "user", content = userMessage))

        val requestBody = GroqChatCompletionRequest(
            model = groqChatModel,
            messages = messages
        )

        val response: GroqChatCompletionResponse = httpClient.post(groqChatUrl) {
            contentType(ContentType.Application.Json)
            bearerAuth(groqApiKey)
            setBody(requestBody)
        }.body()

        return response.choices.first().message.content
    }

    suspend fun extractDurableMemories(
        userMessage: String,
        assistantReply: String,
        existingMemories: List<String>
    ): List<ExtractedMemory> {
        val existingMemoryBlock = if (existingMemories.isEmpty()) {
            "Nenhuma memoria existente."
        } else {
            existingMemories.joinToString(separator = "\n") { memory -> "- $memory" }
        }

        val systemPrompt = GroqChatMessage(
            role = "system",
            content = """
                Voce extrai memorias persistentes para um app educacional.
                Retorne somente JSON valido no formato:
                {"memories":[{"key":"snake_case","value":"frase rica e util","category":"categoria"}]}

                Guarde informacoes duraveis e uteis para personalizacao e relatorios:
                identidade, nome, apelido, cidade, escola, rotina, objetivos, interesses, motivacoes,
                nivel de ingles, dificuldades recorrentes, pontos fortes, confianca para falar,
                preferencias de aprendizagem, temas que engajam, progresso observado e notas de desenvolvimento.

                Nao guarde segredos, senhas, chaves de API, dados de pagamento ou fatos muito sensiveis.
                Evite memorias temporarias como "hoje esta cansado", a menos que revelem um padrao relevante.
                Se uma memoria existente precisar ser atualizada, use a mesma key.
                Escolha keys especificas e precisas: use pais para pais, cidade para cidade, nome para nome,
                objetivo_ingles_falado para objetivo de fala, dificuldade_grammar para dificuldade gramatical, etc.
                Escreva value como uma frase descritiva, rica e util para futuros relatorios.

                Categorias recomendadas:
                identity, location, education, goals, interests, english_level, strengths, difficulties,
                learning_preferences, motivation, emotional_context, progress, report_note, general.
            """.trimIndent()
        )

        val userPrompt = GroqChatMessage(
            role = "user",
            content = """
                Memorias existentes:
                $existingMemoryBlock

                Nova mensagem do usuario:
                $userMessage

                Resposta da tutora:
                $assistantReply
            """.trimIndent()
        )

        val requestBody = GroqChatCompletionRequest(
            model = groqChatModel,
            messages = listOf(systemPrompt, userPrompt),
            temperature = 0.1,
            max_tokens = 700
        )

        val response: GroqChatCompletionResponse = httpClient.post(groqChatUrl) {
            contentType(ContentType.Application.Json)
            bearerAuth(groqApiKey)
            setBody(requestBody)
        }.body()

        val content = response.choices.first().message.content
        return runCatching {
            Json { ignoreUnknownKeys = true }
                .decodeFromString(MemoryExtractionResponse.serializer(), content.extractJsonObject())
                .memories
                .filter { memory -> memory.key.isNotBlank() && memory.value.isNotBlank() }
                .take(MAX_EXTRACTED_MEMORIES_PER_TURN)
        }.getOrElse { error ->
            if (error !is SerializationException && error !is IllegalArgumentException) throw error
            emptyList()
        }
    }

    suspend fun transcribeAudio(audioFile: File, audioContentType: String): String {
        val response = httpClient.post(groqTranscriptionUrl) {
            bearerAuth(groqApiKey)
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append("model", groqTranscriptionModel)
                        append(
                            key = "file",
                            value = audioFile.readBytes(),
                            headers = Headers.build {
                                append(HttpHeaders.ContentType, audioContentType)
                                append(HttpHeaders.ContentDisposition, "filename=\"${audioFile.name}\"")
                            }
                        )
                    }
                )
            )
        }

        val responseText = response.bodyAsText()
        if (!response.status.isSuccess()) {
            error("Groq transcription failed: $responseText")
        }

        return Json { ignoreUnknownKeys = true }
            .decodeFromString(GroqTranscriptionResponse.serializer(), responseText)
            .text
            .trim()
    }

    private fun String.extractJsonObject(): String {
        val start = indexOf('{')
        val end = lastIndexOf('}')
        require(start >= 0 && end > start) { "No JSON object found." }
        return substring(start, end + 1)
    }

    private data class LanguagePolicy(
        val name: String,
        val instructions: String
    )

    private fun String.toLanguagePolicy(): LanguagePolicy {
        return when (lowercase()) {
            "uk" -> LanguagePolicy(
                name = "Ucraniano",
                instructions = """
                    Explique e oriente principalmente em ucraniano natural.
                    Use ingles apenas quando o usuario pedir pratica, pronuncia, exemplos, frases, correcao de uma frase em ingles, ou quando ele proprio falar em ingles.
                    Quando usar ingles, mantenha a frase curta e depois explique em ucraniano, se necessario.
                    Nunca transforme toda conversa em ingles sem pedido claro do usuario.
                """.trimIndent()
            )
            "ru" -> LanguagePolicy(
                name = "Russo",
                instructions = """
                    Explique e oriente principalmente em russo natural.
                    Use ingles apenas quando o usuario pedir pratica, pronuncia, exemplos, frases, correcao de uma frase em ingles, ou quando ele proprio falar em ingles.
                    Quando usar ingles, mantenha a frase curta e depois explique em russo, se necessario.
                    Nunca transforme toda conversa em ingles sem pedido claro do usuario.
                """.trimIndent()
            )
            else -> LanguagePolicy(
                name = "Portugues do Brasil",
                instructions = """
                    Explique e oriente principalmente em portugues do Brasil natural.
                    Use ingles apenas quando o usuario pedir pratica, pronuncia, exemplos, frases, correcao de uma frase em ingles, ou quando ele proprio falar em ingles.
                    Quando usar ingles, mantenha a frase curta e depois explique em portugues, se necessario.
                    Nunca transforme toda conversa em ingles sem pedido claro do usuario.
                """.trimIndent()
            )
        }
    }

    private companion object {
        const val MAX_EXTRACTED_MEMORIES_PER_TURN = 6
    }
}
