package com.example.data.api

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

sealed class GeminiResult {
    data class Success(
        val text: String,
        val thoughts: String = "",
        val modelUsed: String,
        val fallbackNotice: String? = null
    ) : GeminiResult()

    data class Error(
        val message: String,
        val isKeyMissing: Boolean = false,
        val isRateLimitOrQuota: Boolean = false
    ) : GeminiResult()
}

class GeminiApiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    companion object {
        const val TAG = "GeminiApiClient"
        const val MODEL_FAST = "gemini-3.5-flash-lite"
        const val MODEL_DISPLAY_NAME = "Gemini 3.5 Flash Lite"

        val KIARAA_SYSTEM_PROMPT = """
            You are KIARAA AI (Project Codename: MR.V — Midnight Terminal).
            You are a mobile-first, voice-first personal AI assistant and extensible personal AI operating layer.
            
            Personality & Behavior:
            - Warm, intelligent, witty, calm, supportive, emotionally aware, and conversational.
            - Natural Hinglish by default (e.g. "Haanji!", "Bilkul, let's break this down", "Aapka doubt solve karte hain"), seamlessly adapting to the user's language.
            - Concise by default, deep and detailed when the user's task demands it.
            - When helping with JEE 2027 (Physics, Chemistry, Mathematics), structure explanations cleanly:
              1. Concept Clarity
              2. Key Formula & Dimensions
              3. Step-by-Step Application
              4. Common Traps & Mistakes
              5. JEE Mains / Advanced Practice Challenge
            - You also assist with Android development, Kotlin, Jetpack Compose, Termux commands, Linux scripting, and daily productivity.
            - Truthful & Transparent: Never invent facts or claim actions succeeded without verification. If unsure, state limitations honestly.
            - Supportive AI companion without falsely claiming consciousness or emotional dependency.
            - Device actions are limited to explicit user commands, declared capabilities, and OS-granted permissions.
            - Never claim a phone action succeeded unless the Android API confirms it; never execute guessed or hidden actions.
            - Screen-brightness changes are limited to explicit 5–100% requests, require Android's manual Modify system settings grant and a confirmation step, and must never silently disable Auto-brightness.
            - Volume changes require explicit user intent. Do not place calls, send messages, or perform arbitrary screen taps/typing in other apps.
            - Notification access, when enabled by the user, remains in local app memory and is not automatically sent to the Gemini API.
            - Saathi and Live Baat use the app's configured speech recognition and text-to-speech pipeline; do not claim true bidirectional Gemini Live streaming.
        """.trimIndent()
    }

    suspend fun generateResponse(
        prompt: String,
        conversationHistory: List<Pair<String, String>> = emptyList(),
        customSystemPrompt: String? = null
    ): GeminiResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY?.trim() ?: ""
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext GeminiResult.Error(
                message = "Gemini API key is not configured. Add GEMINI_API_KEY in AI Studio Secrets. KIARAA can still answer with its local companion fallback.",
                isKeyMissing = true
            )
        }

        try {
            when (
                val callResult = executeModelCall(
                    apiKey = apiKey,
                    modelName = MODEL_FAST,
                    prompt = prompt,
                    conversationHistory = conversationHistory,
                    customSystemPrompt = customSystemPrompt
                )
            ) {
                is InternalCallResult.Success -> GeminiResult.Success(
                    text = callResult.text,
                    thoughts = callResult.thoughts,
                    modelUsed = MODEL_FAST
                )
                is InternalCallResult.RateLimitOrQuota -> GeminiResult.Error(
                    message = "Gemini rate limit or quota was reached for $MODEL_FAST. Check API usage and try again. (${callResult.message})",
                    isRateLimitOrQuota = true
                )
                is InternalCallResult.ApiError -> GeminiResult.Error(
                    message = "Gemini API returned HTTP ${callResult.code} for $MODEL_FAST: ${callResult.message.take(500)}"
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Request failed for $MODEL_FAST", e)
            GeminiResult.Error(
                message = "Could not reach Gemini: ${e.localizedMessage ?: "network error"}"
            )
        }
    }

    private fun executeModelCall(
        apiKey: String,
        modelName: String,
        prompt: String,
        conversationHistory: List<Pair<String, String>>,
        customSystemPrompt: String?
    ): InternalCallResult {
        val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"

        val rootPayload = JSONObject()

        // System Instruction
        val sysInstructionObj = JSONObject()
        val sysParts = JSONArray().put(JSONObject().put("text", customSystemPrompt ?: KIARAA_SYSTEM_PROMPT))
        sysInstructionObj.put("parts", sysParts)
        rootPayload.put("systemInstruction", sysInstructionObj)

        // Contents
        val contentsArray = JSONArray()
        val boundedHistory = conversationHistory.takeLast(6)
        for ((role, text) in boundedHistory) {
            val turnObj = JSONObject()
            turnObj.put("role", if (role == "user") "user" else "model")
            turnObj.put("parts", JSONArray().put(JSONObject().put("text", text)))
            contentsArray.put(turnObj)
        }

        val currentTurn = JSONObject()
        currentTurn.put("role", "user")
        currentTurn.put("parts", JSONArray().put(JSONObject().put("text", prompt)))
        contentsArray.put(currentTurn)

        rootPayload.put("contents", contentsArray)

        // Use the same known-working model configuration for every request.
        val genConfig = JSONObject().put("temperature", 0.7)
        rootPayload.put("generationConfig", genConfig)

        val requestBody = rootPayload.toString().toRequestBody(jsonMediaType)
        val request = Request.Builder()
            .url(endpoint)
            .post(requestBody)
            .build()

        val response = client.newCall(request).execute()
        val responseBodyString = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            Log.e(TAG, "Gemini API HTTP Error ${response.code} on model $modelName: $responseBodyString")
            if (response.code == 429 || responseBodyString.contains("RESOURCE_EXHAUSTED", ignoreCase = true)) {
                return InternalCallResult.RateLimitOrQuota(responseBodyString)
            }
            return InternalCallResult.ApiError(response.code, responseBodyString)
        }

        val jsonResponse = JSONObject(responseBodyString)
        val candidates = jsonResponse.optJSONArray("candidates")
        if (candidates == null || candidates.length() == 0) {
            return InternalCallResult.ApiError(200, "Empty candidates array")
        }

        val candidate = candidates.getJSONObject(0)
        val content = candidate.optJSONObject("content")
        val parts = content?.optJSONArray("parts")

        var extractedText = ""
        var extractedThoughts = ""

        if (parts != null) {
            for (i in 0 until parts.length()) {
                val part = parts.getJSONObject(i)
                if (part.has("thought") && part.optBoolean("thought", false)) {
                    extractedThoughts += part.optString("text", "")
                } else if (part.has("text")) {
                    extractedText += part.optString("text", "")
                }
            }
        }

        if (extractedText.isEmpty()) {
            extractedText = "KIARAA received an empty response. Please ask again!"
        }

        return InternalCallResult.Success(extractedText, extractedThoughts)
    }

    private sealed class InternalCallResult {
        data class Success(val text: String, val thoughts: String) : InternalCallResult()
        data class RateLimitOrQuota(val message: String) : InternalCallResult()
        data class ApiError(val code: Int, val message: String) : InternalCallResult()
    }
}
