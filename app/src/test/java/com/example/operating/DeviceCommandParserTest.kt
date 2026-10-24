package com.example.operating

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceCommandParserTest {
    @Test
    fun parsesBrightnessPercentage() {
        assertEquals(
            DeviceCommand.Brightness(BrightnessOperation.SET, 75),
            DeviceCommandParser.parse("brightness 75%")
        )
    }

    @Test
    fun parsesMediaVolumeIncrease() {
        assertEquals(
            DeviceCommand.Volume(VolumeOperation.INCREASE, VolumeStream.MEDIA),
            DeviceCommandParser.parse("volume badhao")
        )
    }

    @Test
    fun restrictedConnectivityToggleNeverClaimsDirectSuccess() {
        val command = DeviceCommandParser.parse("Wi-Fi on")
        assertTrue(command is DeviceCommand.RestrictedToggle)
        command as DeviceCommand.RestrictedToggle
        assertEquals("Wi-Fi", command.feature)
        assertEquals(SystemSettingsPage.WIFI, command.page)
        assertTrue(command.enable)
    }

    @Test
    fun parsesInstalledAppLaunchRequest() {
        assertEquals(DeviceCommand.LaunchApp("chrome"), DeviceCommandParser.parse("open chrome"))
    }

    @Test
    fun parsesMultiStepMediaSearchWithoutClaimingPlayback() {
        assertEquals(
            DeviceCommand.PlaySearch(MediaPlatform.YOUTUBE, "lofi mix"),
            DeviceCommandParser.parse("YouTube kholo aur lofi mix play karo")
        )
    }

    @Test
    fun asksWhenNextVideoIsAmbiguous() {
        assertTrue(DeviceCommandParser.parse("next video") is DeviceCommand.Clarify)
    }

    @Test
    fun routesScreenshotThroughAccessibility() {
        assertEquals(DeviceCommand.TakeScreenshot, DeviceCommandParser.parse("screenshot lo"))
    }

    @Test
    fun parsesScreenTimeoutInMinutes() {
        assertEquals(DeviceCommand.ScreenTimeout(120), DeviceCommandParser.parse("screen timeout 2 minutes"))
    }

    @Test
    fun parsesExplicitTapCoordinates() {
        assertEquals(DeviceCommand.Tap(120f, 240f), DeviceCommandParser.parse("tap 120,240"))
    }

    @Test
    fun normalConversationIsNotMisclassifiedAsDeviceAction() {
        assertNull(DeviceCommandParser.parse("Can you explain Kotlin coroutines in Hinglish?"))
    }
}
