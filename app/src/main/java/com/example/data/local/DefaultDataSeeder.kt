package com.example.data.local

import androidx.room.withTransaction
import com.example.data.model.ChatMessage

/**
 * Adds only a welcome message. It does not create personal memories without consent.
 * Old automatically-created sample memories are removed, while user-created memories are preserved.
 */
class DefaultDataSeeder(
    private val database: AppDatabase
) {
    suspend fun seedIfNeeded() {
        database.withTransaction {
            val chatDao = database.chatDao()
            database.memoryDao().deleteSystemSeededMemories()
            val welcome = createWelcomeMessage()
            chatDao.replaceLegacyWelcomeMessage(welcome.content)
            if (chatDao.getMessageCount() == 0) {
                chatDao.insertMessage(welcome)
            }
        }
    }

    private fun createWelcomeMessage() = ChatMessage(
        role = "assistant",
        content = "Hi! Main KIARAA AI hoon. Kya haal hai? Batao, main kis cheez mein help karun?",
        mode = "general"
    )
}
