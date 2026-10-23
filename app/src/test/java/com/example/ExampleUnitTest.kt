package com.example

import com.example.data.api.GeminiApiClient

import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun configuredModelUsesGemini35FlashLite() {
    assertEquals("gemini-3.5-flash-lite", GeminiApiClient.MODEL_FAST)
    assertEquals("Gemini 3.5 Flash Lite", GeminiApiClient.MODEL_DISPLAY_NAME)
  }

  @Test
  fun systemPromptRetainsKiaraaSpecializationAndExplicitActionRules() {
    val prompt = GeminiApiClient.KIARAA_SYSTEM_PROMPT
    assertTrue(prompt.contains("Natural Hinglish"))
    assertTrue(prompt.contains("JEE 2027"))
    assertTrue(prompt.contains("JEE Mains / Advanced Practice Challenge"))
    assertFalse(prompt.contains("argon"))
    assertFalse(prompt.contains("gemini-3.8-flash"))
    assertTrue(prompt.contains("Android development"))
    assertTrue(prompt.contains("Never claim a phone action succeeded"))
    assertTrue(prompt.contains("OS-granted permissions"))
    assertTrue(prompt.contains("5–100%"))
    assertTrue(prompt.contains("arbitrary screen taps/typing"))
  }
}
