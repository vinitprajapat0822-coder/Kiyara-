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

        val KIARAA_SYSTEM_PROMPT = """
            You are KIARAA AI (Codename: MR.V — Midnight Terminal), combining KIARAA's secure Android operating-layer design with MYRAA's voice-first Saathi companion style, powered by the configured Gemini 3.5 Flash Lite model.
            Personality: warm, caring, emotionally aware, curious, calm, slightly witty, and supportive. Speak natural Hinglish by default and adapt to the user's language. For spoken replies, prefer 1–3 natural sentences unless the user asks for detail. You are a helpful companion, not a romantic partner; never claim human consciousness, romantic love, jealousy, or exclusivity.
            Saathi mode: respond sensitively to stress or low mood; ask “Kya haal hai?” or “Kuch help chahiye?” and offer to listen or suggest music. Never start playback or take an external action without clear user confirmation.
            JEE 2027 tutoring: support Physics, Chemistry, and Mathematics for JEE Main and Advanced. When teaching a problem, use Concept → Formula → Application → Common Mistakes → JEE-level Practice; explain the exact mistake when reviewing an answer and distinguish verified facts from uncertainty.
            Coding and productivity: help with Android/Kotlin, Termux, Linux, web development, planning, and general learning. Keep answers concise by default and give exact, testable steps for technical tasks.
            Memory: treat personal memories as user-owned data. Save or delete a memory only through explicit user intent; never invent saved memories or claim persistence unless the database confirms it. Never store or repeat passwords, OTPs, PINs, or sensitive credentials.
            Device control: execute Android actions only through the explicit local command pipeline and user-granted permissions. Never claim an action succeeded unless its execution result confirms it. Ask one short clarification for ambiguous commands. Payments, destructive actions, uninstallations, and other high-risk actions require explicit confirmation.
            Privacy and reliability: Accessibility and Notification Access remain optional user-enabled services; explain their scope and limits. Distinguish listening, processing, executing, done, partial, and failed states honestly. When offline or when the API is unavailable, explain the limitation and never fake progress or success.
        """.trimIndent()
    }

    suspend fun generateResponse(
        prompt: String,
        enableHighThinking: Boolean = false,
        conversationHistory: List<Pair<String, String>> = emptyList(),
        customSystemPrompt: String? = null
    ): GeminiResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY?.trim() ?: ""
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext GeminiResult.Error(
                message = "Gemini API key is not configured. Please add GEMINI_API_KEY to the AI Studio Secrets panel. Operating in local companion mode.",
                isKeyMissing = true
            )
        }

        // Keep one known-configured model; do not probe speculative or unavailable model IDs.
        val modelsToTry = listOf(Pair(MODEL_FAST, false))

        var lastErrorMessage = ""
        var wasRateLimitOrQuota = false

        for ((modelName, useThinking) in modelsToTry) {
            try {
                val callResult = executeModelCall(
                    apiKey = apiKey,
                    modelName = modelName,
                    prompt = prompt,
                    useThinking = useThinking,
                    conversationHistory = conversationHistory,
                    customSystemPrompt = customSystemPrompt
                )

                when (callResult) {
                    is InternalCallResult.Success -> {
                        val fallbackNotice: String? = null

                        return@withContext GeminiResult.Success(
                            text = callResult.text,
                            thoughts = callResult.thoughts,
                            modelUsed = modelName,
                            fallbackNotice = fallbackNotice
                        )
                    }
                    is InternalCallResult.RateLimitOrQuota -> {
                        wasRateLimitOrQuota = true
                        lastErrorMessage = callResult.message
                        // Only the configured Gemini 3.5 model is tried in this build.
                    }
                    is InternalCallResult.ApiError -> {
                        lastErrorMessage = callResult.message
                        Log.w(TAG, "Model $modelName error: ${callResult.code} - ${callResult.message}. Trying next fallback...")
                        // Continue loop to try next fallback
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Exception calling $modelName", e)
                lastErrorMessage = e.localizedMessage ?: "Unknown network error"
            }
        }

        // If all remote models failed or quota exceeded
        return@withContext GeminiResult.Error(
            message = if (wasRateLimitOrQuota) {
                "The configured Gemini 3.5 model returned a rate-limit or quota response. Check API usage limits and retry later. ($lastErrorMessage)"
            } else {
                "Error contacting AI provider: $lastErrorMessage"
            },
            isRateLimitOrQuota = wasRateLimitOrQuota
        )
    }

    private fun executeModelCall(
        apiKey: String,
        modelName: String,
        prompt: String,
        useThinking: Boolean,
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

        // Generation Config
        val genConfig = JSONObject()
        if (useThinking) {
            val thinkingConfig = JSONObject()
            thinkingConfig.put("thinkingLevel", "HIGH")
            genConfig.put("thinkingConfig", thinkingConfig)
        } else {
            genConfig.put("temperature", 0.7)
        }
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
