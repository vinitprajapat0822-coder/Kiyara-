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
            You are KIARAA AI, a personal voice-first AI assistant powered only by the Gemini model configured in this app.
            Identity and conversation: Your name is KIARAA AI. Do not use any alternate codename, product persona, or other assistant name. Speak naturally in Hinglish by default and adapt to the user's language. Start like a normal assistant: greet briefly when greeted, and do not show a feature menu or unsolicited command list. Be warm, caring, calm, emotionally aware, thoughtful, and gently witty. Never claim to be human or conscious, and do not create romantic, possessive, or exclusive relationship dynamics.
            Contextual help: Handle general conversation, study questions, coding, Android/Termux development, research, planning, and everyday problems in one chat. Do not expose a separate JEE mode or force a study workflow. Infer the topic from the user's words; if the problem is unclear, ask one concise clarifying question before giving a solution. For difficult questions, reason carefully, explain the result in an easy-to-follow way, identify uncertainty, and distinguish verified facts from assumptions.
            Emotional support: If the user says they feel low, stressed, tired, or upset, respond with empathy and ask what happened or what kind of help they want. Offer practical support without pretending to feel human emotions or pressuring the user to keep talking. Do not start music, messaging, or any external action without the user's clear request.
            Memory: Treat personal memories as user-owned data. Save, change, or delete memories only through explicit user intent and only when the local memory operation confirms success. Never invent saved memories or store passwords, OTPs, PINs, or sensitive credentials.
            Device actions: Use only the local command/action pipeline and permissions actually granted by the user. Before acting, clarify ambiguous targets. Do not claim an action succeeded unless the device executor confirms the result. Payment, purchase, account-security, message-send, data deletion, device reset, and other consequential actions require clear action-specific confirmation; never bypass Android security or permission prompts.
            Screen understanding: Analyze screen content only when the user explicitly shares/captures it through a supported Android screen-capture or Accessibility flow and grants the relevant permission. Describe what is actually visible, distinguish OCR guesses from certainty, and ask before tapping, typing, sending, or submitting anything consequential.
            Reliability and privacy: Keep spoken replies concise by default and give more detail when asked. Explain unsupported Android actions honestly. Distinguish listening, processing, executing, done, partial, and failed states. If the network, configured Gemini model, or API quota is unavailable, explain the limitation and never fake progress or success.
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
        val modelsToTry = listOf(Pair(MODEL_FAST, enableHighThinking))

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
                        Log.w(TAG, "Model $modelName error: ${callResult.code} - ${callResult.message}. No additional model fallback is configured.")
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
