package com.example

import com.example.data.api.GeminiApiClient
import org.junit.Assert.assertEquals
import org.junit.Test

class GeminiModelConfigurationTest {
    @Test
    fun configuredModelUsesSupportedGemini35FlashLiteId() {
        assertEquals("gemini-3.5-flash-lite", GeminiApiClient.MODEL_FAST)
    }
}
