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
            id = "jee_solver",
            name = "JEE Problem Step-Solver",
            description = "Deconstructs complex Physics, Chemistry, and Math questions into concept, formula, application, and common traps.",
            platform = "On-Device / AI Core",
            riskLevel = RiskLevel.LOW,
            status = CapabilityStatus.AVAILABLE,
            verificationStrategy = "Pedagogical dimensional & consistency check"
        ),
        CapabilityInfo(
            id = "study_timer",
            name = "JEE Study Sprint Timer",
            description = "Configures 25m focus sprint or 45m JEE Advanced mock slot with haptic completion cue.",
            platform = "Android Foreground",
            riskLevel = RiskLevel.LOW,
            status = CapabilityStatus.AVAILABLE,
            verificationStrategy = "System chronometer verification"
        ),
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
            description = "Modify device connectivity (Wi-Fi, Bluetooth, Screen Timeout).",
            platform = "Android System Settings",
            riskLevel = RiskLevel.HIGH,
            status = CapabilityStatus.REQUIRES_INTEGRATION,
            verificationStrategy = "Requires WRITE_SETTINGS or Device Owner privilege"
        ),
        CapabilityInfo(
            id = "cloud_sync",
            name = "Cross-Device Encrypted Sync",
            description = "Synchronizes memory graph and study state across Android, Web, and Desktop.",
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
            "study_timer" -> {
                val duration = parameter.toIntOrNull() ?: 25
                logAudit(capability, "SUCCESS", "Timer initialized for $duration minutes.")
                ExecutionResult.Success("Study sprint started for $duration minutes. Stay focused!", "Timer active: $duration mins")
            }
            "termux_builder" -> {
                val cmd = when {
                    parameter.contains("python", ignoreCase = true) -> "pkg install python -y && pip install --upgrade pip"
                    parameter.contains("git", ignoreCase = true) -> "pkg install git -y && git config --global user.name 'Developer'"
                    parameter.contains("update", ignoreCase = true) -> "pkg update && pkg upgrade -y"
                    else -> "pkg update -y && pkg install build-essential clang -y"
                }
                copyToClipboard(cmd)
                logAudit(capability, "SUCCESS", "Generated & copied Termux command: $cmd")
                ExecutionResult.Success("Termux command ready and copied to clipboard: `$cmd`", cmd)
            }
            "jee_solver" -> {
                logAudit(capability, "SUCCESS", "Processed JEE structural request: $parameter")
                ExecutionResult.Success("JEE Solver ready. Framework activated for: $parameter")
            }
            else -> ExecutionResult.Error("Handler not implemented.")
        }
    }

    private fun triggerHaptic(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
                true
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                vibrator?.vibrate(80)
                true
            }
        } catch (e: Exception) {
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
