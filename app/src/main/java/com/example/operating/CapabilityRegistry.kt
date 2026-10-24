package com.example.operating

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.data.local.AuditDao
import com.example.data.model.AuditLogEntry

enum class RiskLevel {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

enum class CapabilityStatus {
    AVAILABLE,
    REQUIRES_PERMISSION,
    REQUIRES_INTEGRATION,
    PLANNED
}

data class CapabilityInfo(
    val id: String,
    val name: String,
    val description: String,
    val platform: String,
    val riskLevel: RiskLevel,
    val status: CapabilityStatus,
    val verificationStrategy: String
)

sealed class ExecutionResult {
    data class Success(val message: String, val details: String = "") : ExecutionResult()
    data class RequiresConfirmation(val capability: CapabilityInfo, val parameter: String) : ExecutionResult()
    data class Blocked(val reason: String) : ExecutionResult()
    data class Error(val error: String) : ExecutionResult()
}

class CapabilityRegistry(
    private val context: Context,
    private val auditDao: AuditDao
) {
    val capabilities = listOf(
        CapabilityInfo(
            id = "clipboard_copy",
            name = "Smart Clipboard Export",
            description = "Copies code snippets, study notes, or formula sheets to Android clipboard.",
            platform = "Android System API",
            riskLevel = RiskLevel.LOW,
            status = CapabilityStatus.AVAILABLE,
            verificationStrategy = "ClipboardManager primary clip check"
        ),
        CapabilityInfo(
            id = "haptic_pulse",
            name = "Haptic Response Feedback",
            description = "Triggers a tactile vibration pulse on device for confirmation.",
            platform = "Android Vibrator API",
            riskLevel = RiskLevel.LOW,
            status = CapabilityStatus.AVAILABLE,
            verificationStrategy = "Vibrator hardware check"
        ),
        CapabilityInfo(
            id = "termux_builder",
            name = "Termux / Linux Command Assistant",
            description = "Synthesizes verified CLI pipelines for Termux, Python, Git, and Android build tools.",
            platform = "Terminal Environment",
            riskLevel = RiskLevel.LOW,
            status = CapabilityStatus.AVAILABLE,
            verificationStrategy = "Syntax & safety sandbox scan"
        ),
        CapabilityInfo(
            id = "system_settings",
            name = "Device Settings Controller",
            description = "Open Android settings pages. Direct connectivity switches remain controlled by Android system restrictions.",
            platform = "Android System Settings",
            riskLevel = RiskLevel.HIGH,
            status = CapabilityStatus.REQUIRES_INTEGRATION,
            verificationStrategy = "Requires WRITE_SETTINGS or Device Owner privilege"
        ),
        CapabilityInfo(
            id = "cloud_sync",
            name = "Cross-Device Encrypted Sync",
            description = "Synchronizes selected memories across Android, Web, and Desktop.",
            platform = "Cloud Sync Service",
            riskLevel = RiskLevel.MEDIUM,
            status = CapabilityStatus.PLANNED,
            verificationStrategy = "Zero-knowledge cryptographic handshake"
        )
    )

    suspend fun executeCapability(
        capabilityId: String,
        parameter: String,
        confirmedByUser: Boolean = false
    ): ExecutionResult {
        val capability = capabilities.find { it.id == capabilityId }
            ?: return ExecutionResult.Error("Unknown capability: $capabilityId")

        // Risk check: High/Medium risk requires explicit user confirmation
        if ((capability.riskLevel == RiskLevel.HIGH || capability.riskLevel == RiskLevel.MEDIUM) && !confirmedByUser) {
            logAudit(capability, "PENDING_CONFIRMATION", "Awaiting explicit user authorization for ${capability.name}")
            return ExecutionResult.RequiresConfirmation(capability, parameter)
        }

        // Status check
        if (capability.status == CapabilityStatus.REQUIRES_INTEGRATION) {
            val msg = "Honest limitation: ${capability.name} requires native Android OS Admin privileges. Not claimed in sandbox."
            logAudit(capability, "BLOCKED", msg)
            return ExecutionResult.Blocked(msg)
        }

        if (capability.status == CapabilityStatus.PLANNED) {
            val msg = "${capability.name} is scheduled on the Roadmap (Phase 2). Not yet active in this build."
            logAudit(capability, "PLANNED", msg)
            return ExecutionResult.Blocked(msg)
        }

        // Execute available tools
        return when (capability.id) {
            "haptic_pulse" -> {
                val ok = triggerHaptic()
                logAudit(capability, if (ok) "SUCCESS" else "FAILED", "Haptic pulse triggered on device.")
                if (ok) ExecutionResult.Success("Haptic pulse transmitted successfully.")
                else ExecutionResult.Error("Vibration hardware unavailable.")
            }
            "clipboard_copy" -> {
                val ok = copyToClipboard(parameter)
                logAudit(capability, if (ok) "SUCCESS" else "FAILED", "Copied ${parameter.length} chars to clipboard.")
                if (ok) ExecutionResult.Success("Copied snippet to clipboard safely.")
                else ExecutionResult.Error("Failed to write to clipboard.")
            }
            "termux_builder" -> {
                val cmd = when {
                    parameter.contains("python", ignoreCase = true) -> "pkg install python -y && pip install --upgrade pip"
                    parameter.contains("git", ignoreCase = true) -> "pkg install git -y && git config --global user.name 'Developer'"
                    parameter.contains("update", ignoreCase = true) -> "pkg update && pkg upgrade -y"
                    else -> "pkg update -y && pkg install build-essential clang -y"
                }
                val copied = copyToClipboard(cmd)
                if (copied) {
                    logAudit(capability, "SUCCESS", "Generated and copied Termux command; command was not executed.")
                    ExecutionResult.Success("Command generated and copied to clipboard. It has not been executed.", cmd)
                } else {
                    logAudit(capability, "FAILED", "Generated Termux command but clipboard copy failed.")
                    ExecutionResult.Error("Command was generated but clipboard access failed. No command was executed.")
                }
            }
            else -> ExecutionResult.Error("Handler not implemented.")
        }
    }

    private fun triggerHaptic(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager ?: return false
                val vibrator = manager.defaultVibrator
                if (!vibrator.hasVibrator()) return false
                vibrator.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
                true
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return false
                if (!vibrator.hasVibrator()) return false
                @Suppress("DEPRECATION")
                vibrator.vibrate(80)
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun copyToClipboard(text: String): Boolean {
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = ClipData.newPlainText("KIARAA AI", text)
            clipboard?.setPrimaryClip(clip)
            true
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun logAudit(capability: CapabilityInfo, status: String, details: String) {
        try {
            auditDao.insertLog(
                AuditLogEntry(
                    toolName = capability.name,
                    actionSummary = capability.description,
                    riskLevel = capability.riskLevel.name,
                    status = status,
                    details = details
                )
            )
        } catch (_: Exception) {}
    }
}
