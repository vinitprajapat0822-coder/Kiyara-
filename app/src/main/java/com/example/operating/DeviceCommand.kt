package com.example.operating

sealed class DeviceCommand {
    data class Brightness(val operation: BrightnessOperation, val percent: Int? = null) : DeviceCommand()
    data class Volume(val operation: VolumeOperation, val stream: VolumeStream = VolumeStream.MEDIA, val percent: Int? = null) : DeviceCommand()
    data class ScreenTimeout(val seconds: Int) : DeviceCommand()
    data class LaunchApp(val name: String) : DeviceCommand()
    data class OpenSettings(val page: SystemSettingsPage) : DeviceCommand()
    data class RestrictedToggle(val feature: String, val page: SystemSettingsPage, val enable: Boolean) : DeviceCommand()
    data class Flashlight(val enabled: Boolean) : DeviceCommand()
    data class PlaySearch(val platform: MediaPlatform, val query: String) : DeviceCommand()
    data class MediaKey(val key: MediaKeyType) : DeviceCommand()
    data object GoHome : DeviceCommand()
    data object GoBack : DeviceCommand()
    data object RecentApps : DeviceCommand()
    data object TakeScreenshot : DeviceCommand()
    data object LockScreen : DeviceCommand()
    data object ScrollForward : DeviceCommand()
    data object ScrollBackward : DeviceCommand()
    data class Tap(val x: Float, val y: Float, val longPress: Boolean = false) : DeviceCommand()
    data class TypeText(val text: String) : DeviceCommand()
    data object ReadScreen : DeviceCommand()
    data object OpenNotificationShade : DeviceCommand()
    data object ReadNotifications : DeviceCommand()
    data class OpenNotification(val index: Int) : DeviceCommand()
    data class Clarify(val prompt: String) : DeviceCommand()
}

enum class BrightnessOperation { INCREASE, DECREASE, SET, AUTO_ON, AUTO_OFF }
enum class VolumeOperation { INCREASE, DECREASE, MUTE, UNMUTE, SET }
enum class VolumeStream { MEDIA, RING, NOTIFICATION }
enum class MediaPlatform { YOUTUBE, SPOTIFY }
enum class MediaKeyType { PLAY_PAUSE, NEXT, PREVIOUS }
enum class SystemSettingsPage { GENERAL, WIFI, BLUETOOTH, MOBILE_NETWORK, AIRPLANE, DISPLAY, SOUND, ACCESSIBILITY, BATTERY_SAVER, DO_NOT_DISTURB, NOTIFICATION_ACCESS, APP_DETAILS }

object DeviceCommandParser {
    private val setBrightness = Regex("""^(?:set\s+)?(?:screen\s+)?brightness(?:\s+to)?\s+(\d{1,3})\s*%?$""")
    private val setVolume = Regex("""^(?:set\s+)?(media\s+|ring\s+|notification\s+)?volume(?:\s+to)?\s+(\d{1,3})\s*%?$""")
    private val setTimeout = Regex("""^(?:set\s+)?screen\s+timeout(?:\s+to)?\s+(\d{1,4})\s*(seconds?|secs?|minutes?|mins?)$""")
    private val tap = Regex("""^(?:tap|click)\s+(\d{1,4})\s*[, ]\s*(\d{1,4})$""")
    private val longPress = Regex("""^(?:long\s*press|press\s+and\s+hold)\s+(\d{1,4})\s*[, ]\s*(\d{1,4})$""")
    private val type = Regex("""^(?:type|enter|screen\s+par\s+likho)\s+(.+)$""")
    private val multiStepMedia = Regex("""^(youtube|spotify)\s+(?:kholo|open)\s+(?:aur|and)\s+(.+?)\s+(?:play|chalao|chala\s+do)(?:\s+karo)?$""")

    fun parse(input: String): DeviceCommand? {
        val q = input.trim().lowercase().removeSuffix("?").replace(Regex("""\s+"""), " ")
        if (q.isBlank()) return null

        // Only treat imperative-style requests as actions; keep educational questions in chat.
        val informationalPrefixes = listOf(
            "how do i ", "how to ", "how can i ", "what is ", "what are ",
            "why is ", "why does ", "explain ", "tell me about ", "meaning of ",
            "kaise kaam karta hai", "kaise kaam karti hai", "kya hota hai", "kyu hota hai",
            "mujhe samjhao", "samjha do ki "
        )
        if (informationalPrefixes.any(q::startsWith)) return null

        multiStepMedia.matchEntire(q)?.let {
            return DeviceCommand.PlaySearch(if (it.groupValues[1] == "spotify") MediaPlatform.SPOTIFY else MediaPlatform.YOUTUBE, it.groupValues[2].trim())
        }
        setBrightness.matchEntire(q)?.let { return DeviceCommand.Brightness(BrightnessOperation.SET, it.groupValues[1].toInt().coerceIn(1, 100)) }
        if (q.contains("auto brightness") || q.contains("automatic brightness")) {
            return when {
                listOf(" on", " enable", " chalu").any(q::contains) -> DeviceCommand.Brightness(BrightnessOperation.AUTO_ON)
                listOf(" off", " disable", " band").any(q::contains) -> DeviceCommand.Brightness(BrightnessOperation.AUTO_OFF)
                else -> DeviceCommand.OpenSettings(SystemSettingsPage.DISPLAY)
            }
        }
        if (q.contains("brightness") || q.contains("screen light")) {
            return when {
                listOf("increase", " up", "badhao", "badhado", "zyada", "tez").any(q::contains) -> DeviceCommand.Brightness(BrightnessOperation.INCREASE)
                listOf("decrease", " down", "kam", "ghatao", "ghata do", "dim").any(q::contains) -> DeviceCommand.Brightness(BrightnessOperation.DECREASE)
                else -> null
            }
        }
        setVolume.matchEntire(q)?.let {
            val stream = when (it.groupValues[1].trim()) { "ring" -> VolumeStream.RING; "notification" -> VolumeStream.NOTIFICATION; else -> VolumeStream.MEDIA }
            return DeviceCommand.Volume(VolumeOperation.SET, stream, it.groupValues[2].toInt().coerceIn(0, 100))
        }
        val stream = when {
            q.contains("ring volume") || q.contains("ringtone") || q.contains("ringer") -> VolumeStream.RING
            q.contains("notification volume") -> VolumeStream.NOTIFICATION
            else -> VolumeStream.MEDIA
        }
        if (listOf("mute", "silent kar", "awaz band", "awaaz band", "volume band").any(q::contains)) return DeviceCommand.Volume(VolumeOperation.MUTE, stream)
        if (listOf("unmute", "sound on", "awaz chalu", "awaaz chalu", "volume chalu").any(q::contains)) return DeviceCommand.Volume(VolumeOperation.UNMUTE, stream)
        if (q.contains("volume") || q.contains("awaz") || q.contains("awaaz")) {
            if (listOf("increase", " up", "louder", "badhao", "badhado", "zyada", "tez").any(q::contains)) return DeviceCommand.Volume(VolumeOperation.INCREASE, stream)
            if (listOf("decrease", " down", "lower", "kam", "ghatao", "ghata do").any(q::contains)) return DeviceCommand.Volume(VolumeOperation.DECREASE, stream)
        }
        setTimeout.matchEntire(q)?.let {
            val n = it.groupValues[1].toInt()
            val seconds = if (it.groupValues[2].startsWith("min")) n * 60 else n
            return DeviceCommand.ScreenTimeout(seconds.coerceIn(15, 1800))
        }

        if (q.contains("wifi") || q.contains("wi-fi")) {
            if (listOf(" on", " enable", " chalu", " off", " disable", " band").any(q::contains)) {
                return DeviceCommand.RestrictedToggle("Wi-Fi", SystemSettingsPage.WIFI, listOf(" on", " enable", " chalu").any(q::contains))
            }
            return DeviceCommand.OpenSettings(SystemSettingsPage.WIFI)
        }
        if (q.contains("bluetooth")) {
            if (listOf(" on", " enable", " chalu", " off", " disable", " band").any(q::contains)) {
                return DeviceCommand.RestrictedToggle("Bluetooth", SystemSettingsPage.BLUETOOTH, listOf(" on", " enable", " chalu").any(q::contains))
            }
            return DeviceCommand.OpenSettings(SystemSettingsPage.BLUETOOTH)
        }
        if (q.contains("mobile data") || q.contains("cellular data") || q.contains("mobile network")) {
            if (listOf(" on", " enable", " chalu", " off", " disable", " band").any(q::contains)) {
                return DeviceCommand.RestrictedToggle("Mobile data", SystemSettingsPage.MOBILE_NETWORK, listOf(" on", " enable", " chalu").any(q::contains))
            }
            return DeviceCommand.OpenSettings(SystemSettingsPage.MOBILE_NETWORK)
        }
        if (q.contains("airplane mode") || q.contains("flight mode")) {
            if (listOf(" on", " enable", " chalu", " off", " disable", " band").any(q::contains)) {
                return DeviceCommand.RestrictedToggle("Airplane mode", SystemSettingsPage.AIRPLANE, listOf(" on", " enable", " chalu").any(q::contains))
            }
            return DeviceCommand.OpenSettings(SystemSettingsPage.AIRPLANE)
        }
        if (q.contains("display settings") || q.contains("display setting")) return DeviceCommand.OpenSettings(SystemSettingsPage.DISPLAY)
        if (q.contains("sound settings") || q.contains("audio settings")) return DeviceCommand.OpenSettings(SystemSettingsPage.SOUND)
        if (q.contains("battery saver") || q.contains("power saving")) return DeviceCommand.OpenSettings(SystemSettingsPage.BATTERY_SAVER)
        if (q.contains("do not disturb") || q == "dnd") return DeviceCommand.OpenSettings(SystemSettingsPage.DO_NOT_DISTURB)
        if (q.contains("accessibility settings") || q == "enable accessibility") return DeviceCommand.OpenSettings(SystemSettingsPage.ACCESSIBILITY)
        if (q.contains("notification access")) return DeviceCommand.OpenSettings(SystemSettingsPage.NOTIFICATION_ACCESS)

        if (q.contains("flashlight") || q.contains("torch")) {
            if (listOf(" on", " enable", " chalu").any(q::contains)) return DeviceCommand.Flashlight(true)
            if (listOf(" off", " disable", " band").any(q::contains)) return DeviceCommand.Flashlight(false)
        }
        if (q in setOf("go home", "home", "home jao", "ghar jao", "home screen kholo", "home screen par jao")) return DeviceCommand.GoHome
        if (q in setOf("back", "go back", "peeche jao", "wapas jao", "ek step back")) return DeviceCommand.GoBack
        if (q in setOf("recent apps", "show recent apps", "recent kholo", "recent apps kholo", "app switcher kholo")) return DeviceCommand.RecentApps
        if (q in setOf("take screenshot", "screenshot lo", "screenshot le lo", "screen capture")) return DeviceCommand.TakeScreenshot
        if (q in setOf("lock screen", "screen lock karo", "phone lock karo", "lock phone")) return DeviceCommand.LockScreen
        if (q in setOf("scroll down", "neeche scroll karo", "page neeche karo")) return DeviceCommand.ScrollForward
        if (q in setOf("scroll up", "upar scroll karo", "page upar karo")) return DeviceCommand.ScrollBackward
        if (q in setOf("read screen", "screen padh ke batao", "screen par kya hai", "read this screen")) return DeviceCommand.ReadScreen
        if (q in setOf("open notification shade", "notification panel kholo", "notifications kholo")) return DeviceCommand.OpenNotificationShade
        if (q in setOf("read notifications", "notifications padh ke sunao", "recent notifications padho")) return DeviceCommand.ReadNotifications
        Regex("""^(?:open|kholo)\s+notification\s+(\d{1,2})$""").matchEntire(q)?.let { return DeviceCommand.OpenNotification(it.groupValues[1].toInt().coerceIn(1, 10)) }

        longPress.matchEntire(q)?.let { return DeviceCommand.Tap(it.groupValues[1].toFloat(), it.groupValues[2].toFloat(), true) }
        tap.matchEntire(q)?.let { return DeviceCommand.Tap(it.groupValues[1].toFloat(), it.groupValues[2].toFloat()) }
        type.matchEntire(q)?.let { return DeviceCommand.TypeText(it.groupValues[1]) }
        if (q in setOf("play pause", "pause music", "resume music", "play music", "music pause", "gaana pause", "gaana chalao")) return DeviceCommand.MediaKey(MediaKeyType.PLAY_PAUSE)
        if (q in setOf("next song", "next track", "gaana badlo", "agla gaana", "next music")) return DeviceCommand.MediaKey(MediaKeyType.NEXT)
        if (q in setOf("previous song", "previous track", "pichla gaana", "last song")) return DeviceCommand.MediaKey(MediaKeyType.PREVIOUS)
        if (q in setOf("next video", "video badlo", "agla video")) return DeviceCommand.Clarify("Kaunsa app ya video? YouTube search kholun ya current player ka next item badlun?")
        if (q.startsWith("play ") || q.startsWith("gaana chalao ")) {
            val query = q.removePrefix("play ").removePrefix("gaana chalao ").trim().removeSuffix(" please").removeSuffix(" karo")
            if (query.isNotBlank() && query !in setOf("pause", "resume")) return DeviceCommand.PlaySearch(MediaPlatform.YOUTUBE, query)
        }

        Regex("""^(?:open|launch|start|kholo|khol do)\s+(.+?)(?:\s+app)?$""").matchEntire(q)?.let {
            val target = it.groupValues[1].trim()
            if (target == "settings") return DeviceCommand.OpenSettings(SystemSettingsPage.GENERAL)
            if (target.isNotBlank()) return DeviceCommand.LaunchApp(target)
        }
        Regex("""^(.+?)\s+(?:kholo|khol do)$""").matchEntire(q)?.let {
            val target = it.groupValues[1].trim()
            if (target == "settings") return DeviceCommand.OpenSettings(SystemSettingsPage.GENERAL)
            if (target.isNotBlank()) return DeviceCommand.LaunchApp(target)
        }
        return null
    }
}
