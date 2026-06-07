package com.ayanami.database

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import io.github.cdimascio.dotenv.dotenv
import org.bson.codecs.configuration.CodecRegistries
import org.bson.codecs.configuration.CodecRegistry
import org.bson.codecs.kotlinx.KotlinSerializerCodecProvider

object MongoManager {
    private val dotenv = dotenv { ignoreIfMissing = true }

    private val connectionString = System.getenv("MONGO_URI")
        ?: dotenv["MONGO_URI"]
        ?: throw IllegalStateException("MONGO_URI was not found in .env or environment variables.")

    private val client: MongoClient
    val database: MongoDatabase

    init {
        val codecRegistry: CodecRegistry = CodecRegistries.fromRegistries(
            CodecRegistries.fromProviders(KotlinSerializerCodecProvider()),
            MongoClientSettings.getDefaultCodecRegistry()
        )

        val settings = MongoClientSettings.builder()
            .applyConnectionString(ConnectionString(connectionString))
            .codecRegistry(codecRegistry)
            .build()

        client = MongoClient.create(settings)
        database = client.getDatabase("ayanami_learn_db")

        println("Connected to MongoDB Atlas.")
    }
}
