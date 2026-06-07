package com.yourname.ayanami.learn.data.remote

import com.yourname.ayanami.learn.data.model.AuthResponseDto
import com.yourname.ayanami.learn.data.model.EmailLoginRequestDto
import com.yourname.ayanami.learn.data.model.EmailRegisterRequestDto
import com.yourname.ayanami.learn.data.model.UserDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import javax.inject.Inject

class AuthApiService @Inject constructor(
    private val httpClient: HttpClient
) {
    suspend fun authenticateWithFirebase(firebaseToken: String, userDto: UserDto): String {
        val response = httpClient.post("${ApiConfig.BASE_URL}/api/users/register") {
            contentType(ContentType.Application.Json)
            bearerAuth(firebaseToken)
            setBody(userDto)
        }
        response.ensureSuccess()
        return response.body()
    }

    suspend fun registerWithEmail(
        fullName: String,
        email: String,
        password: String,
        nativeLanguage: String
    ): AuthResponseDto {
        val response = httpClient.post("${ApiConfig.BASE_URL}/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(
                EmailRegisterRequestDto(
                    fullName = fullName,
                    email = email,
                    password = password,
                    nativeLanguage = nativeLanguage
                )
            )
        }
        response.ensureSuccess()
        return response.body()
    }

    suspend fun loginWithEmail(email: String, password: String): AuthResponseDto {
        val response = httpClient.post("${ApiConfig.BASE_URL}/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(
                EmailLoginRequestDto(
                    email = email,
                    password = password
                )
            )
        }
        response.ensureSuccess()
        return response.body()
    }

    private suspend fun HttpResponse.ensureSuccess() {
        if (!status.isSuccess()) {
            throw IllegalStateException(bodyAsText().toReadableBackendError())
        }
    }

    private fun String.toReadableBackendError(): String {
        return substringAfter("\"message\":\"", this)
            .substringBefore("\"")
            .takeIf { it.isNotBlank() && it != this }
            ?: this
    }
}
