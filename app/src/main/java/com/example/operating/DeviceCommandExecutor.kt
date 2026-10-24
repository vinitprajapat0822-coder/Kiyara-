package com.example.operating

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.KeyEvent
import androidx.core.content.ContextCompat
import java.net.URLEncoder
import java.util.Locale

class DeviceCommandExecutor(context: Context) {
    private val app = context.applicationContext

    fun execute(command: DeviceCommand): DeviceActionResult = when (command) {
        is DeviceCommand.Brightness -> changeBrightness(command)
        is DeviceCommand.Volume -> changeVolume(command)
        is DeviceCommand.ScreenTimeout -> changeTimeout(command.seconds)
        is DeviceCommand.LaunchApp -> launchApp(command.name)
        is DeviceCommand.OpenSettings -> openSettings(command.page)
        is DeviceCommand.RestrictedToggle -> restrictedToggle(command)
        is DeviceCommand.Flashlight -> flashlight(command.enabled)
        is DeviceCommand.PlaySearch -> openMediaSearch(command.platform, command.query)
        is DeviceCommand.MediaKey -> mediaKey(command.key)
        is DeviceCommand.Clarify -> DeviceActionResult.clarification(command.prompt)
        DeviceCommand.GoHome, DeviceCommand.GoBack, DeviceCommand.RecentApps,
        DeviceCommand.TakeScreenshot, DeviceCommand.LockScreen, DeviceCommand.ScrollForward,
        DeviceCommand.ScrollBackward, is DeviceCommand.Tap, is DeviceCommand.TypeText,
        DeviceCommand.ReadScreen, DeviceCommand.OpenNotificationShade -> {
            KiaraaAccessibilityService.executeIfConnected(command) ?: DeviceActionResult.needsAccessibility(
                "Is action ke liye KIARAA Accessibility enable karni hogi. Settings tab → Device Access → Enable Accessibility par jaakar manually enable karo. Launch par permission force nahi hoti."
            )
        }
        DeviceCommand.ReadNotifications -> {
            if (!KiaraaNotificationListenerService.hasAccess(app)) {
                openIntent(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                DeviceActionResult.needsPermission(
                    "Notification Access settings kholi hain. KIARAA ko manually enable karo, phir “read notifications” dobara bolo. Notifications Memory Vault mein save nahi hoti."
                )
            } else {
                KiaraaNotificationListenerService.readRecent()
            }
        }
        is DeviceCommand.OpenNotification -> {
            if (!KiaraaNotificationListenerService.hasAccess(app)) {
                openIntent(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                DeviceActionResult.needsPermission("Notification Access settings kholi hain. KIARAA ko manually enable karo, phir command repeat karo.")
            } else {
                KiaraaNotificationListenerService.openRecent(command.index)
            }
        }
    }

    private fun changeBrightness(command: DeviceCommand.Brightness): DeviceActionResult {
        if (!Settings.System.canWrite(app)) {
            openIntent(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${app.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return DeviceActionResult.needsPermission("“Modify system settings” permission page khola hai. Permission allow karke brightness command dobara bolo.")
        }
        return runCatching {
            if (command.operation == BrightnessOperation.AUTO_ON || command.operation == BrightnessOperation.AUTO_OFF) {
                val mode = if (command.operation == BrightnessOperation.AUTO_ON) Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC else Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
                val ok = Settings.System.putInt(app.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, mode)
                if (ok) DeviceActionResult.success(if (mode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) "Auto brightness enabled." else "Auto brightness disabled.")
                else DeviceActionResult.failed("Android did not accept the auto-brightness setting.")
            } else {
                val current = Settings.System.getInt(app.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128)
                val next = when (command.operation) {
                    BrightnessOperation.INCREASE -> (current + 32).coerceAtMost(255)
                    BrightnessOperation.DECREASE -> (current - 32).coerceAtLeast(1)
                    BrightnessOperation.SET -> ((command.percent ?: 50) * 255 / 100).coerceIn(1, 255)
                    else -> current
                }
                val ok = Settings.System.putInt(app.contentResolver, Settings.System.SCREEN_BRIGHTNESS, next)
                if (ok) DeviceActionResult.success("Brightness set to approximately ${next * 100 / 255}%.")
                else DeviceActionResult.failed("Android did not accept the brightness value.")
            }
        }.getOrElse { DeviceActionResult.failed("Brightness update failed: ${it.message ?: "system setting error"}") }
    }

    private fun changeTimeout(seconds: Int): DeviceActionResult {
        if (!Settings.System.canWrite(app)) {
            openIntent(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${app.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return DeviceActionResult.needsPermission("“Modify system settings” permission page khola hai. Permission allow karke screen-timeout command dobara bolo.")
        }
        val ok = runCatching { Settings.System.putInt(app.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, seconds * 1000) }.getOrDefault(false)
        return if (ok) DeviceActionResult.success("Screen timeout set to $seconds seconds.") else DeviceActionResult.failed("Android did not accept the timeout value.")
    }

    private fun changeVolume(command: DeviceCommand.Volume): DeviceActionResult = runCatching {
        val audio = app.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val stream = when (command.stream) {
            VolumeStream.MEDIA -> AudioManager.STREAM_MUSIC
            VolumeStream.RING -> AudioManager.STREAM_RING
            VolumeStream.NOTIFICATION -> AudioManager.STREAM_NOTIFICATION
        }
        when (command.operation) {
            VolumeOperation.INCREASE -> audio.adjustStreamVolume(stream, AudioManager.ADJUST_RAISE, 0)
            VolumeOperation.DECREASE -> audio.adjustStreamVolume(stream, AudioManager.ADJUST_LOWER, 0)
            VolumeOperation.MUTE -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) audio.adjustStreamVolume(stream, AudioManager.ADJUST_MUTE, 0) else audio.setStreamVolume(stream, 0, 0)
            VolumeOperation.UNMUTE -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) audio.adjustStreamVolume(stream, AudioManager.ADJUST_UNMUTE, 0) else audio.setStreamVolume(stream, (audio.getStreamMaxVolume(stream) / 2).coerceAtLeast(1), 0)
            VolumeOperation.SET -> {
                val max = audio.getStreamMaxVolume(stream)
                audio.setStreamVolume(stream, ((command.percent ?: 50) * max / 100).coerceIn(0, max), 0)
            }
        }
        val label = when (command.stream) { VolumeStream.MEDIA -> "Media"; VolumeStream.RING -> "Ringtone"; VolumeStream.NOTIFICATION -> "Notification" }
        DeviceActionResult.success("$label volume updated (${audio.getStreamVolume(stream) * 100 / audio.getStreamMaxVolume(stream).coerceAtLeast(1)}%).")
    }.getOrElse { DeviceActionResult.failed("Volume change failed: ${it.message ?: "audio service error"}") }

    private fun launchApp(requested: String): DeviceActionResult {
        val query = requested.trim().lowercase(Locale.ROOT)
        if (query.isBlank()) return DeviceActionResult.failed("App ka naam nahi mila. Exact app name bolo.")
        return runCatching {
            val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val matches = app.packageManager.queryIntentActivities(launcher, 0).mapNotNull { info ->
                val activity = info.activityInfo ?: return@mapNotNull null
                val label = info.loadLabel(app.packageManager)?.toString()?.trim().orEmpty()
                if (label.isBlank() || activity.packageName == app.packageName) null else AppMatch(label, activity.packageName, activity.name)
            }.distinctBy { it.packageName }
            val exact = matches.filter { it.label.equals(requested.trim(), true) }
            val candidates = if (exact.isNotEmpty()) exact else matches.filter { it.label.lowercase(Locale.ROOT).contains(query) }
            when {
                candidates.isEmpty() -> DeviceActionResult.failed("“$requested” naam ka launchable app nahi mila. Installed app ka exact naam bolo.")
                candidates.size > 1 -> DeviceActionResult.clarification("Multiple apps match hue: ${candidates.take(4).joinToString { it.label }}. Exact app name bolo.")
                else -> {
                    val resolved = candidates.first()
                    val intent = app.packageManager.getLaunchIntentForPackage(resolved.packageName)
                        ?: Intent().setClassName(resolved.packageName, resolved.activityName)
                    app.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    DeviceActionResult.success("${resolved.label} open kar diya.")
                }
            }
        }.getOrElse { DeviceActionResult.failed("App open nahi hua: ${it.message ?: "Android launch error"}") }
    }

    private data class AppMatch(val label: String, val packageName: String, val activityName: String)

    private fun openSettings(page: SystemSettingsPage): DeviceActionResult {
        val opened = openIntent(settingsIntent(page).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        return if (opened) DeviceActionResult.success("${page.name.replace('_', ' ')} settings opened.")
        else DeviceActionResult.failed("${page.name.replace('_', ' ')} settings page is unavailable on this Android build.")
    }

    private fun restrictedToggle(command: DeviceCommand.RestrictedToggle): DeviceActionResult {
        val opened = openIntent(settingsIntent(command.page).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        val desired = if (command.enable) "on" else "off"
        return if (opened) DeviceActionResult.partial(
            "${command.feature} $desired directly toggle nahi kiya: Android third-party apps ko yeh switch control karne se restrict karta hai. Settings panel khola hai; wahan manually $desired karo."
        ) else DeviceActionResult.failed("${command.feature} settings panel could not be opened.")
    }

    private fun settingsIntent(page: SystemSettingsPage): Intent = when (page) {
        SystemSettingsPage.WIFI -> Intent(Settings.ACTION_WIFI_SETTINGS)
        SystemSettingsPage.BLUETOOTH -> Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
        SystemSettingsPage.MOBILE_NETWORK -> Intent(Settings.ACTION_WIRELESS_SETTINGS)
        SystemSettingsPage.AIRPLANE -> Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS)
        SystemSettingsPage.DISPLAY -> Intent(Settings.ACTION_DISPLAY_SETTINGS)
        SystemSettingsPage.SOUND -> Intent(Settings.ACTION_SOUND_SETTINGS)
        SystemSettingsPage.ACCESSIBILITY -> Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        SystemSettingsPage.BATTERY_SAVER -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS) else Intent(Settings.ACTION_SETTINGS)
        SystemSettingsPage.DO_NOT_DISTURB -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS) else Intent(Settings.ACTION_SOUND_SETTINGS)
        SystemSettingsPage.NOTIFICATION_ACCESS -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS) else Intent(Settings.ACTION_SETTINGS)
        SystemSettingsPage.APP_DETAILS -> Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${app.packageName}"))
    }

    private fun flashlight(enabled: Boolean): DeviceActionResult {
        if (ContextCompat.checkSelfPermission(app, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            openIntent(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${app.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return DeviceActionResult.needsPermission("Flashlight ke liye Camera permission chahiye. App details kholi hain; Camera permission allow karke retry karo.")
        }
        return runCatching {
            val manager = app.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = manager.cameraIdList.firstOrNull {
                manager.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return DeviceActionResult.failed("Is device par flashlight-capable camera nahi mila.")
            manager.setTorchMode(cameraId, enabled)
            DeviceActionResult.success(if (enabled) "Flashlight on." else "Flashlight off.")
        }.getOrElse { DeviceActionResult.failed("Flashlight update failed: ${it.message ?: "camera service error"}") }
    }

    private fun mediaKey(key: MediaKeyType): DeviceActionResult = runCatching {
        val audio = app.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val code = when (key) { MediaKeyType.PLAY_PAUSE -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE; MediaKeyType.NEXT -> KeyEvent.KEYCODE_MEDIA_NEXT; MediaKeyType.PREVIOUS -> KeyEvent.KEYCODE_MEDIA_PREVIOUS }
        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, code))
        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, code))
        DeviceActionResult.partial("Media control event sent to Android. Playback depends on an active media session.")
    }.getOrElse { DeviceActionResult.failed("Media control failed: ${it.message ?: "no active media session"}") }

    private fun openMediaSearch(platform: MediaPlatform, query: String): DeviceActionResult {
        if (query.isBlank()) return DeviceActionResult.clarification("Kya search karun? Song ya playlist ka naam bolo.")
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = if (platform == MediaPlatform.SPOTIFY) "https://open.spotify.com/search/$encoded" else "https://www.youtube.com/results?search_query=$encoded"
        return if (openIntent(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))) {
            DeviceActionResult.partial("${platform.name.lowercase().replaceFirstChar { it.uppercase() }} search khol di. KIARAA ne song/video auto-select nahi kiya; correct result tap karke playback start karo.")
        } else DeviceActionResult.failed("Media search open nahi hui. Browser ya media app check karo.")
    }

    private fun openIntent(intent: Intent): Boolean = runCatching {
        if (intent.resolveActivity(app.packageManager) == null) return false
        app.startActivity(intent)
        true
    }.getOrDefault(false)
}
