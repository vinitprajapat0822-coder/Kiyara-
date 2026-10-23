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
        const val MODEL_HIGH_THINKING = "gemini-4-argon"
        const val MODEL_RECOMMENDED = "gemini-3.8-flash"
        const val MODEL_FAST = "gemini-3.5-flash-lite"

        // Skip the high-thinking model in this process only after an explicit quota-exhaustion response.
        @Volatile
        var isHighThinkingModelQuotaLimited = false

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

        // Determine priority models to attempt
        val modelsToTry = mutableListOf<Pair<String, Boolean>>() // (modelName, withThinking)

        if (enableHighThinking && !isHighThinkingModelQuotaLimited) {
            // First attempt specified gemini-4-argon
            modelsToTry.add(Pair(MODEL_HIGH_THINKING, true))
        }

        // Resilient fallbacks: Gemini 3.8 Flash with thinking, standard Gemini 3.8 Flash, then Gemini 3.5 Flash-Lite.
        if (enableHighThinking) {
            modelsToTry.add(Pair(MODEL_RECOMMENDED, true))
        }
        modelsToTry.add(Pair(MODEL_RECOMMENDED, false))
        modelsToTry.add(Pair(MODEL_FAST, false))

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
                        val fallbackNotice = if (enableHighThinking && modelName != MODEL_HIGH_THINKING) {
                            "High-thinking model unavailable or rate-limited; automatically routed this request to $modelName."
                        } else null

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
                        if (
                            modelName == MODEL_HIGH_THINKING &&
                            (callResult.message.contains("RESOURCE_EXHAUSTED", ignoreCase = true) ||
                                callResult.message.contains("quota", ignoreCase = true))
                        ) {
                            // Skip retries only after the API explicitly reports quota exhaustion.
                            isHighThinkingModelQuotaLimited = true
                            Log.w(TAG, "$MODEL_HIGH_THINKING reported quota exhaustion. Using fallback models for the rest of this process.")
                        }
                        // Continue loop to try next model in fallback list
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
                "Gemini API rate limit or quota reached for all attempted models. Check API usage limits and retry later. ($lastErrorMessage)"
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
