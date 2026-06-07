package com.yourname.ayanami.learn.data.remote

import com.yourname.ayanami.learn.data.model.ChatRequestDto
import com.yourname.ayanami.learn.data.model.ChatResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import java.io.File
import javax.inject.Inject

class ChatApiService @Inject constructor(
    private val httpClient: HttpClient
) {
    suspend fun sendTextMessage(chatRequest: ChatRequestDto): ChatResponseDto {
        return httpClient.post("${ApiConfig.BASE_URL}/api/chat") {
            contentType(ContentType.Application.Json)
            setBody(chatRequest)
        }.body()
    }

    suspend fun sendVoiceMessage(
        audioFile: File,
        userId: String,
        nativeLanguage: String
    ): ChatResponseDto {
        return httpClient.post("${ApiConfig.BASE_URL}/api/chat/voice") {
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append(
                            key = "audio",
                            value = audioFile.readBytes(),
                            headers = Headers.build {
                                append(HttpHeaders.ContentType, "audio/wav")
                                append(HttpHeaders.ContentDisposition, "filename=\"audio.wav\"")
                            }
                        )
                        append("userId", userId)
                        append("nativeLanguage", nativeLanguage)
                    }
                )
            )
        }.body()
    }
}
