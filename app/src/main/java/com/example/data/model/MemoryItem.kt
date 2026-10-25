package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memories")
data class MemoryItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val category: String, // "Preferences", "Projects", "Decisions", "Routines", "General"
    val title: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val source: String = "User Confirmed", // "User Confirmed", "KIARAA Inferred", "Session"
    val isPersistent: Boolean = true
)
