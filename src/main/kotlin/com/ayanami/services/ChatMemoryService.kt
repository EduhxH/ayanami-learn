package com.ayanami.services

import com.ayanami.database.MongoManager
import com.ayanami.models.ChatMessageRecord
import com.ayanami.models.UserMemory
import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.client.model.Sorts
import kotlinx.coroutines.flow.toList
import org.bson.types.ObjectId

data class ChatMemoryContext(
    val memories: List<UserMemory>,
    val history: List<GroqChatMessage>
) {
    val memoryFacts: List<String>
        get() = memories.map { memory -> "${memory.key}: ${memory.value}" }
}

class ChatMemoryService(
    private val aiService: AiService
) {
    private val messagesCollection = MongoManager.database.getCollection<ChatMessageRecord>("chat_messages")
    private val memoriesCollection = MongoManager.database.getCollection<UserMemory>("user_memories")

    suspend fun loadContext(userId: String): ChatMemoryContext {
        val memories = memoriesCollection
            .find(Filters.eq(UserMemory::userId.name, userId))
            .sort(Sorts.ascending(UserMemory::category.name, UserMemory::key.name))
            .limit(MAX_MEMORIES_IN_PROMPT)
            .toList()

        val recentMessages = messagesCollection
            .find(Filters.eq(ChatMessageRecord::userId.name, userId))
            .sort(Sorts.descending(ChatMessageRecord::createdAt.name))
            .limit(MAX_RECENT_MESSAGES)
            .toList()
            .asReversed()

        return ChatMemoryContext(
            memories = memories,
            history = recentMessages.map { message ->
                GroqChatMessage(role = message.role, content = message.content)
            }
        )
    }

    suspend fun saveMessage(userId: String, role: String, content: String) {
        val cleanContent = content.trim()
        if (cleanContent.isBlank()) return

        messagesCollection.insertOne(
            ChatMessageRecord(
                userId = userId,
                role = role,
                content = cleanContent.take(MAX_STORED_MESSAGE_CHARS)
            )
        )
    }

    suspend fun rememberFromExchange(
        userId: String,
        userMessage: String,
        assistantReply: String
    ) {
        val existingMemories = memoriesCollection
            .find(Filters.eq(UserMemory::userId.name, userId))
            .sort(Sorts.ascending(UserMemory::category.name, UserMemory::key.name))
            .limit(MAX_MEMORIES_FOR_EXTRACTION)
            .toList()
            .map { memory -> "${memory.key}: ${memory.value}" }

        val extractedMemories = aiService.extractDurableMemories(
            userMessage = userMessage,
            assistantReply = assistantReply,
            existingMemories = existingMemories
        )

        extractedMemories.forEach { extracted ->
            val key = extracted.key.toMemoryKey()
            val value = extracted.value.trim().take(MAX_MEMORY_VALUE_CHARS)
            if (key.isBlank() || value.isBlank()) return@forEach

            val existing = memoriesCollection
                .find(
                    Filters.and(
                        Filters.eq(UserMemory::userId.name, userId),
                        Filters.eq(UserMemory::key.name, key)
                    )
                )
                .toList()
                .firstOrNull()

            memoriesCollection.replaceOne(
                filter = Filters.and(
                    Filters.eq(UserMemory::userId.name, userId),
                    Filters.eq(UserMemory::key.name, key)
                ),
                replacement = UserMemory(
                    id = existing?.id ?: ObjectId().toHexString(),
                    userId = userId,
                    key = key,
                    value = value,
                    category = extracted.category.ifBlank { existing?.category ?: "general" },
                    source = "chat",
                    createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                ),
                options = ReplaceOptions().upsert(true)
            )
        }
    }

    suspend fun listMemories(userId: String): List<UserMemory> {
        return memoriesCollection
            .find(Filters.eq(UserMemory::userId.name, userId))
            .sort(Sorts.ascending(UserMemory::category.name, UserMemory::key.name))
            .toList()
    }

    private fun String.toMemoryKey(): String {
        return lowercase()
            .replace(Regex("[^a-z0-9_ -]"), "")
            .trim()
            .replace(Regex("\\s+"), "_")
            .take(MAX_MEMORY_KEY_CHARS)
    }

    private companion object {
        const val MAX_RECENT_MESSAGES = 24
        const val MAX_MEMORIES_IN_PROMPT = 40
        const val MAX_MEMORIES_FOR_EXTRACTION = 60
        const val MAX_STORED_MESSAGE_CHARS = 2_000
        const val MAX_MEMORY_KEY_CHARS = 80
        const val MAX_MEMORY_VALUE_CHARS = 500
    }
}
