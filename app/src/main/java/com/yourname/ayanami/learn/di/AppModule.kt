package com.yourname.ayanami.learn.di

import android.app.Application
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.yourname.ayanami.learn.R
import com.yourname.ayanami.learn.data.remote.AuthApiService
import com.yourname.ayanami.learn.data.remote.ChatApiService
import com.yourname.ayanami.learn.data.remote.LiveVoiceApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideGoogleSignInOptions(app: Application): GoogleSignInOptions {
        return GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(app.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
    }

    @Provides
    @Singleton
    fun provideGoogleSignInClient(
        app: Application,
        googleSignInOptions: GoogleSignInOptions
    ): GoogleSignInClient {
        return GoogleSignIn.getClient(app, googleSignInOptions)
    }

    @Provides
    @Singleton
    fun provideHttpClient(): HttpClient {
        return HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
            install(HttpTimeout) {
                connectTimeoutMillis = 10_000
                requestTimeoutMillis = 20_000
                socketTimeoutMillis = HttpTimeout.INFINITE_TIMEOUT_MS
            }
            install(Logging) {
                level = LogLevel.ALL
            }
            install(WebSockets)
        }
    }

    @Provides
    @Singleton
    fun provideAuthApiService(httpClient: HttpClient): AuthApiService {
        return AuthApiService(httpClient)
    }

    @Provides
    @Singleton
    fun provideChatApiService(httpClient: HttpClient): ChatApiService {
        return ChatApiService(httpClient)
    }

    @Provides
    @Singleton
    fun provideLiveVoiceApiService(httpClient: HttpClient): LiveVoiceApiService {
        return LiveVoiceApiService(httpClient)
    }
}
