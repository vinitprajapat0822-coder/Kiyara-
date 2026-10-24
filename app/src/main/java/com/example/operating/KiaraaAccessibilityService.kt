package com.example.operating

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/** Performs explicit user-requested actions only; no background screen capture. */
class KiaraaAccessibilityService : AccessibilityService() {
    override fun onServiceConnected() {
        super.onServiceConnected()
        activeService = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit

    override fun onDestroy() {
        if (activeService === this) activeService = null
        super.onDestroy()
    }

    private fun execute(command: DeviceCommand): DeviceActionResult = when (command) {
        DeviceCommand.GoBack -> globalAction(GLOBAL_ACTION_BACK, "Back")
        DeviceCommand.GoHome -> globalAction(GLOBAL_ACTION_HOME, "Home")
        DeviceCommand.RecentApps -> globalAction(GLOBAL_ACTION_RECENTS, "Recent apps")
        DeviceCommand.TakeScreenshot -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) globalAction(GLOBAL_ACTION_TAKE_SCREENSHOT, "Screenshot")
            else DeviceActionResult.failed("System screenshot action requires Android 9 or later.")
        DeviceCommand.LockScreen -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) globalAction(GLOBAL_ACTION_LOCK_SCREEN, "Lock screen")
            else DeviceActionResult.failed("Lock-screen action requires Android 9 or later.")
        DeviceCommand.ScrollForward -> scroll(true)
        DeviceCommand.ScrollBackward -> scroll(false)
        is DeviceCommand.Tap -> dispatchTap(command)
        is DeviceCommand.TypeText -> typeIntoFocusedField(command.text)
        DeviceCommand.ReadScreen -> readCurrentScreen()
        DeviceCommand.OpenNotificationShade -> globalAction(GLOBAL_ACTION_NOTIFICATIONS, "Notification shade")
        else -> DeviceActionResult.failed("This action is not handled by Accessibility Service.")
    }

    private fun globalAction(action: Int, label: String): DeviceActionResult =
        if (performGlobalAction(action)) DeviceActionResult.success("$label action dispatched.")
        else DeviceActionResult.failed("Android declined the $label action. Unlock the phone and try again.")

    private fun scroll(forward: Boolean): DeviceActionResult {
        val root = rootInActiveWindow ?: return DeviceActionResult.failed("No active app screen is available to scroll.")
        val action = if (forward) AccessibilityNodeInfo.ACTION_SCROLL_FORWARD else AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
        val target = findScrollable(root, action)
        val ok = target?.performAction(action) == true
        target?.recycleSafely()
        root.recycleSafely()
        return if (ok) DeviceActionResult.success(if (forward) "Scrolled down." else "Scrolled up.")
        else DeviceActionResult.failed("No scrollable area was found on the current screen.")
    }

    private fun findScrollable(node: AccessibilityNodeInfo, action: Int): AccessibilityNodeInfo? {
        if (node.isScrollable && node.actionList.any { it.id == action }) return AccessibilityNodeInfo.obtain(node)
        for (i in 0 until node.childCount.coerceAtMost(80)) {
            val child = node.getChild(i) ?: continue
            val result = findScrollable(child, action)
            child.recycleSafely()
            if (result != null) return result
        }
        return null
    }

    private fun dispatchTap(command: DeviceCommand.Tap): DeviceActionResult {
        val path = Path().apply {
            moveTo(command.x, command.y)
            if (command.longPress) lineTo(command.x + 0.1f, command.y + 0.1f)
        }
        val duration = if (command.longPress) 750L else 80L
        val gesture = GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(path, 0L, duration)).build()
        return if (dispatchGesture(gesture, null, null)) {
            DeviceActionResult.success(if (command.longPress) "Long-press gesture dispatched." else "Tap gesture dispatched.")
        } else DeviceActionResult.failed("Android declined the gesture. Check Accessibility permission and screen state.")
    }

    private fun typeIntoFocusedField(text: String): DeviceActionResult {
        if (text.isBlank()) return DeviceActionResult.failed("Text is empty; nothing was typed.")
        val root = rootInActiveWindow ?: return DeviceActionResult.failed("No active screen is available.")
        val field = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        if (field == null || field.isPassword) {
            field?.recycleSafely()
            root.recycleSafely()
            return DeviceActionResult.failed("No safe, focused text field was found. Tap a text field first.")
        }
        val args = Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text.take(1000)) }
        val ok = field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        field.recycleSafely()
        root.recycleSafely()
        return if (ok) DeviceActionResult.success("Text inserted into the focused field.")
        else DeviceActionResult.failed("This app does not allow accessibility text entry in the current field.")
    }

    private fun readCurrentScreen(): DeviceActionResult {
        val root = rootInActiveWindow ?: return DeviceActionResult.failed("No active screen is available to read.")
        val collected = mutableListOf<String>()
        collectScreenText(root, collected, 0)
        root.recycleSafely()
        if (collected.isEmpty()) return DeviceActionResult.failed("No readable text was exposed by the current app.")
        return DeviceActionResult.success("Current screen text (credential fields omitted): " + collected.distinct().take(20).joinToString(". "))
    }

    private fun collectScreenText(node: AccessibilityNodeInfo, output: MutableList<String>, depth: Int) {
        if (depth > 20 || output.size >= 30) return
        val candidates = listOf(node.text?.toString()?.trim().orEmpty(), node.contentDescription?.toString()?.trim().orEmpty())
        val text = candidates.firstOrNull { it.isNotBlank() }.orEmpty()
        val safe = !node.isPassword &&
            !text.contains("password", true) && !text.contains("passcode", true) &&
            !text.contains("one-time code", true) && !text.contains("verification code", true) &&
            !Regex("""\botp\b""", RegexOption.IGNORE_CASE).containsMatchIn(text)
        val redactedText = Regex("""\b\d{4,8}\b""").replace(text, "••••")
        if (safe && redactedText.isNotBlank() && redactedText.length <= 300) output.add(redactedText)
        for (i in 0 until node.childCount.coerceAtMost(80)) {
            val child = node.getChild(i) ?: continue
            collectScreenText(child, output, depth + 1)
            child.recycleSafely()
            if (output.size >= 30) break
        }
    }

    private fun AccessibilityNodeInfo.recycleSafely() {
        @Suppress("DEPRECATION")
        runCatching { recycle() }
    }

    companion object {
        @Volatile private var activeService: KiaraaAccessibilityService? = null
        fun isConnected(): Boolean = activeService != null
        fun executeIfConnected(command: DeviceCommand): DeviceActionResult? {
            val service = activeService ?: return null
            return runCatching { service.execute(command) }
                .getOrElse { DeviceActionResult.failed("Accessibility action failed: ${it.message ?: "unknown system error"}") }
        }
    }
}

enum class DeviceActionStatus { SUCCESS, PARTIAL, FAILED, NEEDS_ACCESSIBILITY, NEEDS_PERMISSION, CLARIFICATION }

data class DeviceActionResult(val status: DeviceActionStatus, val message: String) {
    companion object {
        fun success(message: String) = DeviceActionResult(DeviceActionStatus.SUCCESS, message)
        fun partial(message: String) = DeviceActionResult(DeviceActionStatus.PARTIAL, message)
        fun failed(message: String) = DeviceActionResult(DeviceActionStatus.FAILED, message)
        fun needsAccessibility(message: String) = DeviceActionResult(DeviceActionStatus.NEEDS_ACCESSIBILITY, message)
        fun needsPermission(message: String) = DeviceActionResult(DeviceActionStatus.NEEDS_PERMISSION, message)
        fun clarification(message: String) = DeviceActionResult(DeviceActionStatus.CLARIFICATION, message)
    }
}
