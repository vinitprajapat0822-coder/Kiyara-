package com.example.operating

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.provider.Settings
import com.example.operating.accessibility.KiaraaAccessibilityService
import com.example.operating.accessibility.KiaraaNotificationListenerService
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
    private val capabilityCatalog = listOf(
        CapabilityInfo(
            id = "jee_solver",
            name = "JEE Problem Step-Solver",
            description = "Dedicated direct tool execution is not wired yet; use KIARAA chat for concept, formula, application, and common traps.",
            platform = "KIARAA AI Core",
            riskLevel = RiskLevel.LOW,
            status = CapabilityStatus.PLANNED,
            verificationStrategy = "Pedagogical dimensional & consistency check"
        ),
        CapabilityInfo(
            id = "study_timer",
            name = "JEE Study Sprint Timer",
            description = "A real countdown/foreground timer is not wired in this build; no timer will be falsely reported as started.",
            platform = "Android Foreground",
            riskLevel = RiskLevel.LOW,
            status = CapabilityStatus.PLANNED,
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
            id = "open_app",
            name = "Open Installed App",
            description = "Opens an installed app by its visible launcher name. KIARAA does not force-close other apps.",
            platform = "Android Package Manager",
            riskLevel = RiskLevel.LOW,
            status = CapabilityStatus.AVAILABLE,
            verificationStrategy = "Resolved launcher activity and startActivity request"
        ),
        CapabilityInfo(
            id = "open_system_settings",
            name = "Open Android Settings",
            description = "Opens a selected Android settings page. The user makes the actual setting change inside Android.",
            platform = "Android Settings Intents",
            riskLevel = RiskLevel.LOW,
            status = CapabilityStatus.AVAILABLE,
            verificationStrategy = "System settings activity start"
        ),
        CapabilityInfo(
            id = "system_navigation",
            name = "User-Requested System Navigation",
            description = "Performs Back, Home, Recents, or notification-shade navigation only after explicit user input and manual OS permission enablement.",
            platform = "Android Accessibility Service",
            riskLevel = RiskLevel.MEDIUM,
            status = CapabilityStatus.REQUIRES_PERMISSION,
            verificationStrategy = "Android global-action return value"
        ),
        CapabilityInfo(
            id = "read_notifications",
            name = "Read Active Notifications",
            description = "Reads active notification summaries only after notification-access approval. Content stays in memory and is not automatically sent to Gemini.",
            platform = "Android Notification Listener",
            riskLevel = RiskLevel.MEDIUM,
            status = CapabilityStatus.REQUIRES_PERMISSION,
            verificationStrategy = "Notification-listener connection and in-memory snapshot"
        ),
        CapabilityInfo(
            id = "volume_control",
            name = "Media Volume Control",
            description = "Raises, lowers, or mutes music-stream volume after explicit confirmation.",
            platform = "Android AudioManager",
            riskLevel = RiskLevel.MEDIUM,
            status = CapabilityStatus.AVAILABLE,
            verificationStrategy = "AudioManager adjustment request"
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

    val capabilities: List<CapabilityInfo>
        get() = capabilityCatalog.map { capability ->
            when (capability.id) {
                "system_navigation" -> capability.copy(
                    status = if (KiaraaAccessibilityService.isEnabled(context)) CapabilityStatus.AVAILABLE
                    else CapabilityStatus.REQUIRES_PERMISSION
                )
                "read_notifications" -> capability.copy(
                    status = if (KiaraaNotificationListenerService.isAccessGranted(context)) CapabilityStatus.AVAILABLE
                    else CapabilityStatus.REQUIRES_PERMISSION
                )
                else -> capability
            }
        }

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
            "open_app" -> {
                val openedApp = openInstalledApp(parameter)
                logAudit(capability, if (openedApp != null) "SUCCESS" else "FAILED", "Requested app launch: $parameter")
                if (openedApp != null) ExecutionResult.Success("Opened $openedApp.")
                else ExecutionResult.Error("No launchable app matched '$parameter'. No app was opened.")
            }
            "open_system_settings" -> {
                val settingName = openSystemSettings(parameter)
                logAudit(capability, if (settingName != null) "SUCCESS" else "FAILED", "Requested Android settings page: $parameter")
                if (settingName != null) ExecutionResult.Success("Opened $settingName. Change the setting inside Android; KIARAA did not change it silently.")
                else ExecutionResult.Error("Could not open that Android settings page.")
            }
            "system_navigation" -> {
                val performed = KiaraaAccessibilityService.performUserAction(context, parameter)
                logAudit(capability, if (performed) "SUCCESS" else "BLOCKED", "User-requested navigation action: $parameter")
                if (performed) ExecutionResult.Success("Android navigation action completed: $parameter.")
                else ExecutionResult.Blocked("Navigation was not performed. Enable KIARAA navigation in Android Settings > Accessibility, then try again.")
            }
            "read_notifications" -> {
                if (!KiaraaNotificationListenerService.isAccessGranted(context)) {
                    ExecutionResult.Blocked("Notification access is off. Open More > Personalization & Privacy > Notification Access to enable it manually.")
                } else {
                    val summaries = KiaraaNotificationListenerService.currentNotificationSummaries()
                    if (summaries == null) {
                        ExecutionResult.Blocked("Android has not connected the notification listener yet. On some Android 10 low-RAM devices, notification listeners may be restricted.")
                    } else if (summaries.isEmpty()) {
                        logAudit(capability, "SUCCESS", "Notification access checked; no active notification summaries available.")
                        ExecutionResult.Success("No active notifications are currently available to read.")
                    } else {
                        val localSummary = summaries.takeLast(12).joinToString("\n").take(1600)
                        logAudit(capability, "SUCCESS", "Read ${summaries.size} active notification summaries in memory.")
                        ExecutionResult.Success("Active notification summaries (kept local; not sent to Gemini):", localSummary)
                    }
                }
            }
            "volume_control" -> {
                val changed = adjustMediaVolume(parameter)
                logAudit(capability, if (changed) "SUCCESS" else "FAILED", "Media volume action requested: $parameter")
                if (changed) ExecutionResult.Success("Media volume action requested: $parameter.")
                else ExecutionResult.Error("Unknown volume action. Use up, down, or mute.")
            }
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
                ExecutionResult.Blocked("The study timer is planned but not implemented in this build. No countdown was started.")
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
                ExecutionResult.Blocked("The dedicated JEE tool handler is not implemented yet. Ask KIARAA in chat; no separate tool action was executed.")
            }
            else -> ExecutionResult.Error("Handler not implemented.")
        }
    }

    private fun openInstalledApp(requestedName: String): String? {
        val requested = requestedName.trim()
        if (requested.isBlank()) return null
        return try {
            val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val activities = context.packageManager.queryIntentActivities(launcherIntent, 0)
            val match = activities.firstOrNull { info ->
                val label = info.loadLabel(context.packageManager).toString()
                label.equals(requested, ignoreCase = true) ||
                    info.activityInfo.packageName.equals(requested, ignoreCase = true)
            } ?: activities.firstOrNull { info ->
                info.loadLabel(context.packageManager).toString().contains(requested, ignoreCase = true)
            } ?: return null

            val launchIntent = context.packageManager.getLaunchIntentForPackage(match.activityInfo.packageName)
                ?: return null
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            match.loadLabel(context.packageManager).toString()
        } catch (_: Exception) {
            null
        }
    }

    private fun openSystemSettings(target: String): String? {
        val key = target.trim().lowercase()
        val selection = when (key) {
            "wifi", "wi-fi", "wireless" -> Settings.ACTION_WIFI_SETTINGS to "Wi-Fi settings"
            "bluetooth" -> Settings.ACTION_BLUETOOTH_SETTINGS to "Bluetooth settings"
            "display", "screen" -> Settings.ACTION_DISPLAY_SETTINGS to "Display settings"
            "sound", "audio", "volume" -> Settings.ACTION_SOUND_SETTINGS to "Sound settings"
            "accessibility" -> Settings.ACTION_ACCESSIBILITY_SETTINGS to "Accessibility settings"
            "notifications", "notification access" -> Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS to "Notification access settings"
            "apps", "applications" -> Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS to "App settings"
            "battery" -> Settings.ACTION_BATTERY_SAVER_SETTINGS to "Battery settings"
            "location" -> Settings.ACTION_LOCATION_SOURCE_SETTINGS to "Location settings"
            else -> Settings.ACTION_SETTINGS to "Android Settings"
        }
        return try {
            context.startActivity(Intent(selection.first).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            selection.second
        } catch (_: Exception) {
            null
        }
    }

    private fun adjustMediaVolume(direction: String): Boolean {
        val adjustment = when (direction.trim().lowercase()) {
            "up", "raise", "increase" -> AudioManager.ADJUST_RAISE
            "down", "lower", "decrease" -> AudioManager.ADJUST_LOWER
            "mute" -> AudioManager.ADJUST_MUTE
            else -> return false
        }
        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return false
            audioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                adjustment,
                AudioManager.FLAG_SHOW_UI
            )
            true
        } catch (_: Exception) {
            false
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
