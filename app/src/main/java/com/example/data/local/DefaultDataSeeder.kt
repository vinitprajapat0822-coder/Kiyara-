package com.example.data.local

import androidx.room.withTransaction
import com.example.data.model.ChatMessage
import com.example.data.model.MemoryItem

/**
 * Inserts the first-run welcome message and default study memories exactly once.
 *
 * The transaction makes the existence checks and inserts atomic if more than one
 * ViewModel is initialized at approximately the same time.
 */
class DefaultDataSeeder(
    private val database: AppDatabase
) {
    suspend fun seedIfNeeded() {
        database.withTransaction {
            val chatDao = database.chatDao()
            val memoryDao = database.memoryDao()

            if (chatDao.getMessageCount() == 0) {
                chatDao.insertMessage(createWelcomeMessage())
            }

            defaultMemories().forEach { memory ->
                if (memoryDao.getCountByCategoryAndTitle(memory.category, memory.title) == 0) {
                    memoryDao.insertMemory(memory)
                }
            }
        }
    }

    private fun createWelcomeMessage() = ChatMessage(
        role = "assistant",
        content = "Namaste! Main hoon **KIARAA** (Codename: MR.V — Midnight Terminal). Aapka voice-first personal AI assistant aur operating layer.\n\nJEE 2027 prep (Physics, Chemistry, Math), coding, Termux commands, ya daily planning — bataiye aaj hum kya solve karein?",
        mode = "general"
    )

    private fun defaultMemories() = listOf(
        MemoryItem(
            category = "JEE Prep",
            title = "Target Exam",
            content = "Targeting JEE 2027 Mains & Advanced. Focus on deep concept clarity and speed accuracy.",
            source = "System Initializer"
        ),
        MemoryItem(
            category = "Preferences",
            title = "Language Style",
            content = "Conversational Hinglish by default. Concise explanations with structured 5-stage breakdowns.",
            source = "System Initializer"
        )
    )
}
