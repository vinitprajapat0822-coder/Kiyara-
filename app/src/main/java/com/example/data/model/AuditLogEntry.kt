package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audit_logs")
data class AuditLogEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val toolName: String,
    val actionSummary: String,
    val riskLevel: String, // "LOW", "MEDIUM", "HIGH", "CRITICAL"
    val status: String, // "SUCCESS", "CONFIRMED", "DENIED", "FAILED", "PLANNED"
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)
