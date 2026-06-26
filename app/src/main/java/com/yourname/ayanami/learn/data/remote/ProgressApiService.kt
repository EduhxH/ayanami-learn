package com.yourname.ayanami.learn.data.remote

import com.yourname.ayanami.learn.data.model.LearnerProgressDto
import com.yourname.ayanami.learn.data.model.LessonCompletionRequestDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.encodeURLPath
import javax.inject.Inject

class ProgressApiService @Inject constructor(
    private val httpClient: HttpClient
) {
    suspend fun getProgress(userId: String): LearnerProgressDto {
        return httpClient.get(
            "${ApiConfig.BASE_URL}/api/progress/${userId.encodeURLPath()}"
        ).body()
    }

    suspend fun recordCompletion(
        userId: String,
        request: LessonCompletionRequestDto
    ): LearnerProgressDto {
        return httpClient.post(
            "${ApiConfig.BASE_URL}/api/progress/${userId.encodeURLPath()}/complete"
        ) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }
}
