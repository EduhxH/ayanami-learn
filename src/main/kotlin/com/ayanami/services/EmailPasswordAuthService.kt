package com.ayanami.services

import com.ayanami.models.AuthResponse
import io.github.cdimascio.dotenv.dotenv
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
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

class EmailPasswordAuthService {
    private val dotenv = dotenv { ignoreIfMissing = true }
    private val json = Json { ignoreUnknownKeys = true }
    private val firebaseApiKey = System.getenv("FIREBASE_WEB_API_KEY")
        ?: dotenv["FIREBASE_WEB_API_KEY"]
        ?: error("FIREBASE_WEB_API_KEY was not found in .env or environment variables.")

    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(json)
        }
    }

    suspend fun register(fullName: String, email: String, password: String): AuthResponse {
        val signUp = postFirebaseAuth(
            endpoint = "accounts:signUp",
            body = FirebaseEmailPasswordRequest(
                email = email,
                password = password,
                returnSecureToken = true
            )
        )

        updateFirebaseProfile(
            endpoint = "accounts:update",
            body = FirebaseUpdateProfileRequest(
                idToken = signUp.idToken,
                displayName = fullName,
                returnSecureToken = true
            )
        )

        return signUp.toAuthResponse(
            fallbackFullName = fullName,
            message = "Conta criada com sucesso."
        )
    }

    suspend fun login(email: String, password: String): AuthResponse {
        val signIn = postFirebaseAuth(
            endpoint = "accounts:signInWithPassword",
            body = FirebaseEmailPasswordRequest(
                email = email,
                password = password,
                returnSecureToken = true
            )
        )

        return signIn.toAuthResponse(
            fallbackFullName = "",
            message = "Login realizado com sucesso."
        )
    }

    private suspend inline fun <reified T : Any> postFirebaseAuth(endpoint: String, body: T): FirebaseAuthRestResponse {
        val response = httpClient.post("https://identitytoolkit.googleapis.com/v1/$endpoint?key=$firebaseApiKey") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }

        val responseText = response.bodyAsText()
        if (!response.status.isSuccess()) {
            val firebaseMessage = runCatching {
                json.decodeFromString(FirebaseErrorResponse.serializer(), responseText).error.message
            }.getOrElse { responseText }

            error(firebaseMessage.toReadableMessage())
        }

        return json.decodeFromString(FirebaseAuthRestResponse.serializer(), responseText)
    }

    private suspend fun updateFirebaseProfile(endpoint: String, body: FirebaseUpdateProfileRequest) {
        val response = httpClient.post("https://identitytoolkit.googleapis.com/v1/$endpoint?key=$firebaseApiKey") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }

        val responseText = response.bodyAsText()
        if (!response.status.isSuccess()) {
            val firebaseMessage = runCatching {
                json.decodeFromString(FirebaseErrorResponse.serializer(), responseText).error.message
            }.getOrElse { responseText }

            error(firebaseMessage.toReadableMessage())
        }
    }

    private fun FirebaseAuthRestResponse.toAuthResponse(
        fallbackFullName: String,
        message: String
    ): AuthResponse {
        return AuthResponse(
            firebaseUid = localId,
            fullName = displayName?.takeIf { it.isNotBlank() } ?: fallbackFullName,
            email = email.orEmpty(),
            idToken = idToken,
            refreshToken = refreshToken,
            message = message
        )
    }

    private fun String.toReadableMessage(): String {
        return when (this) {
            "EMAIL_EXISTS" -> "Este email ja esta cadastrado."
            "EMAIL_NOT_FOUND" -> "Email nao encontrado."
            "INVALID_PASSWORD", "INVALID_LOGIN_CREDENTIALS" -> "Email ou senha invalidos."
            "USER_DISABLED" -> "Esta conta foi desativada."
            "WEAK_PASSWORD : Password should be at least 6 characters" -> "A senha precisa ter pelo menos 6 caracteres."
            else -> this
        }
    }
}

@Serializable
private data class FirebaseEmailPasswordRequest(
    val email: String,
    val password: String,
    val returnSecureToken: Boolean
)

@Serializable
private data class FirebaseUpdateProfileRequest(
    val idToken: String,
    val displayName: String,
    val returnSecureToken: Boolean
)

@Serializable
private data class FirebaseAuthRestResponse(
    val localId: String,
    val email: String? = null,
    val displayName: String? = null,
    val idToken: String,
    val refreshToken: String
)

@Serializable
private data class FirebaseErrorResponse(
    val error: FirebaseError
)

@Serializable
private data class FirebaseError(
    val code: Int,
    val message: String,
    @SerialName("errors")
    val errors: List<FirebaseErrorItem> = emptyList()
)

@Serializable
private data class FirebaseErrorItem(
    val message: String? = null,
    val reason: String? = null
)
