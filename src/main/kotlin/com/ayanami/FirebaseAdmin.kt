package com.ayanami

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import io.github.cdimascio.dotenv.dotenv
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.util.Base64

object FirebaseAdmin {
    private val dotenv = dotenv { ignoreIfMissing = true }
    private var initialized = false

    fun init() {
        if (initialized || FirebaseApp.getApps().isNotEmpty()) {
            initialized = true
            return
        }

        runCatching {
            val credentials = serviceAccountCredentials() ?: GoogleCredentials.getApplicationDefault()
            val options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .build()

            FirebaseApp.initializeApp(options)
            initialized = true
            println("Firebase Admin initialized.")
        }.onFailure { error ->
            println("Firebase Admin not initialized: ${error.message}")
            println("Set FIREBASE_SERVICE_ACCOUNT_JSON, FIREBASE_SERVICE_ACCOUNT_BASE64, FIREBASE_SERVICE_ACCOUNT_PATH or Google application default credentials to enable token checks.")
        }
    }

    fun verifyBearerTokenOrNull(authorizationHeader: String?): String? {
        if (!initialized || authorizationHeader.isNullOrBlank()) {
            return null
        }

        val token = authorizationHeader
            .removePrefix("Bearer")
            .trim()

        if (token.isBlank()) {
            return null
        }

        return runCatching {
            FirebaseAuth.getInstance().verifyIdToken(token).uid
        }.getOrNull()
    }

    private fun serviceAccountCredentials(): GoogleCredentials? {
        val rawJson = System.getenv("FIREBASE_SERVICE_ACCOUNT_JSON")
            ?: dotenv["FIREBASE_SERVICE_ACCOUNT_JSON"]
        if (!rawJson.isNullOrBlank()) {
            return ByteArrayInputStream(rawJson.toByteArray(Charsets.UTF_8)).use { stream ->
                GoogleCredentials.fromStream(stream)
            }
        }

        val base64Json = System.getenv("FIREBASE_SERVICE_ACCOUNT_BASE64")
            ?: dotenv["FIREBASE_SERVICE_ACCOUNT_BASE64"]
        if (!base64Json.isNullOrBlank()) {
            val decoded = Base64.getDecoder().decode(base64Json)
            return ByteArrayInputStream(decoded).use { stream ->
                GoogleCredentials.fromStream(stream)
            }
        }

        val path = System.getenv("FIREBASE_SERVICE_ACCOUNT_PATH")
            ?: dotenv["FIREBASE_SERVICE_ACCOUNT_PATH"]
            ?: return null

        val file = File(path)
        if (!file.exists()) {
            error("Firebase service account file not found at $path")
        }

        return FileInputStream(file).use { stream ->
            GoogleCredentials.fromStream(stream)
        }
    }
}
