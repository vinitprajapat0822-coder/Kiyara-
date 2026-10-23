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
  fun fallbackModelsUseDocumentedModelIds() {
    assertEquals("gemini-4-argon", GeminiApiClient.MODEL_HIGH_THINKING)
    assertEquals("gemini-3.8-flash", GeminiApiClient.MODEL_RECOMMENDED)
    assertEquals("gemini-3.5-flash-lite", GeminiApiClient.MODEL_FAST)
  }
}
