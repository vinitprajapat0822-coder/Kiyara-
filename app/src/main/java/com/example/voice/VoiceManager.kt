package com.example.voice

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import com.example.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

enum class VoiceState {
    IDLE,
    LISTENING,
    PROCESSING,
    SPEAKING,
    ERROR
}

private class ElevenLabsApiException(val statusCode: Int, val apiStatus: String) : IOException("HTTP $statusCode ($apiStatus)")

class VoiceManager(
    private val context: Context,
    private val coroutineScope: CoroutineScope
) : TextToSpeech.OnInitListener {

    companion object {
        const val TAG = "VoiceManager"
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized = false
    private var elevenLabsPlayer: MediaPlayer? = null
    private var speechRequestId = 0
    private val elevenLabsClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()
    @Volatile private var continuousListeningEnabled = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private val restartListening = Runnable {
        if (continuousListeningEnabled && _voiceState.value != VoiceState.SPEAKING && _voiceState.value != VoiceState.LISTENING) {
            startListening()
        }
    }

    private val _voiceState = MutableStateFlow(VoiceState.IDLE)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    private val _recognizedText = MutableStateFlow("")
    val recognizedText: StateFlow<String> = _recognizedText.asStateFlow()

    private val _partialText = MutableStateFlow("")
    val partialText: StateFlow<String> = _partialText.asStateFlow()

    private val _audioRms = MutableStateFlow(0f)
    val audioRms: StateFlow<Float> = _audioRms.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _voiceProviderStatus = MutableStateFlow("Checking voice configuration…")
    val voiceProviderStatus: StateFlow<String> = _voiceProviderStatus.asStateFlow()

    var onFinalSpeechResult: ((String) -> Unit)? = null

    init {
        _voiceProviderStatus.value = if (hasElevenLabsConfiguration()) {
            "ElevenLabs configured • selected Voice ID will be used"
        } else {
            "Android system voice • ElevenLabs key/Voice ID not configured in this APK"
        }
        textToSpeech = TextToSpeech(context.applicationContext, this)
    }

    private fun hasElevenLabsConfiguration(): Boolean {
        val apiKey = BuildConfig.ELEVENLABS_API_KEY?.trim().orEmpty()
        val voiceId = BuildConfig.ELEVENLABS_VOICE_ID?.trim().orEmpty()
        return apiKey.isNotEmpty() && apiKey != "MY_ELEVENLABS_API_KEY" &&
            voiceId.isNotEmpty() && voiceId != "MY_ELEVENLABS_VOICE_ID"
    }

    private fun describeElevenLabsFailure(error: Exception): String {
        return when (error) {
            is ElevenLabsApiException -> when (error.statusCode) {
                401 -> "ElevenLabs rejected the API key (401). Check the ELEVENLABS_API_KEY secret."
                402 -> "ElevenLabs credits or quota are exhausted (402)."
                403 -> "This ElevenLabs account/key may not have permission to use the selected voice (403)."
                404 -> "The Voice ID was not found or is not available to this API account (404)."
                422 -> "ElevenLabs rejected the request settings (422). Check voice/model access."
                429 -> "ElevenLabs rate limit reached (429). Try again later."
                else -> "ElevenLabs returned HTTP ${error.statusCode}. Check account access and voice settings."
            }
            is SocketTimeoutException -> "ElevenLabs request timed out. Check the network and retry."
            is UnknownHostException -> "Cannot reach ElevenLabs. Check the internet connection or DNS."
            is IOException -> "ElevenLabs network error (${error.javaClass.simpleName}). Check connection and retry."
            else -> "ElevenLabs voice request failed. Check API access, Voice ID, and model settings."
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech?.setLanguage(Locale("en", "IN"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                textToSpeech?.setLanguage(Locale.US)
            }
            textToSpeech?.setPitch(1.05f)
            textToSpeech?.setSpeechRate(1.0f)
            isTtsInitialized = true

            textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _voiceState.value = VoiceState.SPEAKING
                }

                override fun onDone(utteranceId: String?) {
                    _voiceState.value = VoiceState.IDLE
                    _audioRms.value = 0f
                    scheduleContinuousRestart()
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _voiceState.value = VoiceState.IDLE
                    _audioRms.value = 0f
                    scheduleContinuousRestart()
                }
            })
        } else {
            Log.e(TAG, "TextToSpeech init failed with status: $status")
        }
    }

    fun setContinuousListeningEnabled(enabled: Boolean) {
        continuousListeningEnabled = enabled
        if (!enabled) mainHandler.removeCallbacks(restartListening)
    }

    private fun scheduleContinuousRestart(delayMillis: Long = 450L) {
        if (!continuousListeningEnabled) return
        mainHandler.removeCallbacks(restartListening)
        mainHandler.postDelayed(restartListening, delayMillis)
    }

    fun startListening() {
        mainHandler.removeCallbacks(restartListening)
        stopSpeaking()
        _errorMessage.value = null
        _partialText.value = ""

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _errorMessage.value = "Speech recognition service is not available on this device."
            _voiceState.value = VoiceState.ERROR
            return
        }

        coroutineScope.launch(Dispatchers.Main) {
            try {
                if (speechRecognizer == null) {
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                        setRecognitionListener(createRecognitionListener())
                    }
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                }

                _voiceState.value = VoiceState.LISTENING
                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Error starting speech recognition", e)
                _errorMessage.value = "Failed to start listening: ${e.localizedMessage}"
                _voiceState.value = VoiceState.ERROR
            }
        }
    }

    fun stopListening() {
        coroutineScope.launch(Dispatchers.Main) {
            try {
                speechRecognizer?.stopListening()
                if (_voiceState.value == VoiceState.LISTENING) {
                    _voiceState.value = VoiceState.PROCESSING
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping listener", e)
            }
        }
    }

    fun cancelListening() {
        coroutineScope.launch(Dispatchers.Main) {
            try {
                speechRecognizer?.cancel()
                _voiceState.value = VoiceState.IDLE
                _audioRms.value = 0f
            } catch (e: Exception) {
                Log.e(TAG, "Error cancelling listener", e)
            }
        }
    }

    fun speak(text: String, utteranceId: String = "kiaraa_speech") {
        speechRequestId++
        stopElevenLabsPlayback()
        textToSpeech?.stop()
        cancelListening()
        // Strip markdown formatting before vocalizing and cap request cost/latency.
        val cleanSpeech = text
            .replace(Regex("[*#_`>]"), "")
            .replace(Regex("\\[.*?\\]\\(.*?\\)"), "")
            .take(600)
        if (cleanSpeech.isBlank()) return

        val apiKey = BuildConfig.ELEVENLABS_API_KEY?.trim().orEmpty()
        val voiceId = BuildConfig.ELEVENLABS_VOICE_ID?.trim().orEmpty()
        _errorMessage.value = null
        if (apiKey.isNotEmpty() && apiKey != "MY_ELEVENLABS_API_KEY" &&
            voiceId.isNotEmpty() && voiceId != "MY_ELEVENLABS_VOICE_ID") {
            _voiceProviderStatus.value = "ElevenLabs • connecting to selected voice"
            val requestId = ++speechRequestId
            _voiceState.value = VoiceState.PROCESSING
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val payload = JSONObject()
                        .put("text", cleanSpeech)
                        .put("model_id", "eleven_multilingual_v2")
                        .put("voice_settings", JSONObject()
                            .put("stability", 0.45)
                            .put("similarity_boost", 0.8))
                        .toString()
                    val request = Request.Builder()
                        .url("https://api.elevenlabs.io/v1/text-to-speech/$voiceId?output_format=mp3_44100_128")
                        .header("xi-api-key", apiKey)
                        .header("Accept", "audio/mpeg")
                        .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))
                        .build()
                    val audioBytes = elevenLabsClient.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            val errorBody = response.body?.string().orEmpty()
                            val apiStatus = runCatching {
                                JSONObject(errorBody).optJSONObject("detail")?.optString("status").orEmpty()
                            }.getOrDefault("")
                            throw ElevenLabsApiException(response.code, apiStatus)
                        }
                        response.body?.bytes() ?: throw IOException("ElevenLabs returned empty audio")
                    }
                    if (audioBytes.isEmpty()) throw IOException("ElevenLabs returned empty audio")
                    val audioFile = File(context.cacheDir, "kiaraa_voice_${requestId}.mp3")
                    audioFile.writeBytes(audioBytes)
                    mainHandler.post {
                        if (requestId != speechRequestId) {
                            audioFile.delete()
                            return@post
                        }
                        _voiceProviderStatus.value = "ElevenLabs • selected voice active"
                        _errorMessage.value = null
                        playElevenLabsFile(audioFile, utteranceId)
                    }
                } catch (e: Exception) {
                    val failure = describeElevenLabsFailure(e)
                    Log.w(TAG, "Custom voice request failed: $failure")
                    mainHandler.post {
                        if (requestId == speechRequestId) {
                            _voiceProviderStatus.value = "ElevenLabs unavailable • ${(e as? ElevenLabsApiException)?.statusCode?.let { "HTTP $it" } ?: e.javaClass.simpleName}"
                            _errorMessage.value = failure
                            _voiceState.value = VoiceState.ERROR
                            scheduleContinuousRestart()
                        }
                    }
                }
            }
            return
        }
        _voiceProviderStatus.value = "Android system voice • ElevenLabs key/Voice ID not configured"
        speakWithAndroidTts(cleanSpeech, utteranceId)
    }

    private fun speakWithAndroidTts(text: String, utteranceId: String) {
        if (!isTtsInitialized || textToSpeech == null) {
            _voiceState.value = VoiceState.ERROR
            _errorMessage.value = "Voice engine is not ready yet."
            return
        }
        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        }
        _voiceState.value = VoiceState.SPEAKING
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    private fun playElevenLabsFile(audioFile: File, utteranceId: String) {
        stopElevenLabsPlayback()
        try {
            val player = MediaPlayer()
            elevenLabsPlayer = player
            player.setDataSource(audioFile.absolutePath)
            player.setOnPreparedListener {
                _voiceState.value = VoiceState.SPEAKING
                it.start()
            }
            player.setOnCompletionListener {
                runCatching { it.release() }
                if (elevenLabsPlayer === it) elevenLabsPlayer = null
                audioFile.delete()
                _voiceState.value = VoiceState.IDLE
                _audioRms.value = 0f
                scheduleContinuousRestart()
            }
            player.setOnErrorListener { mp, _, _ ->
                runCatching { mp.release() }
                if (elevenLabsPlayer === mp) elevenLabsPlayer = null
                audioFile.delete()
                _voiceState.value = VoiceState.ERROR
                _errorMessage.value = "ElevenLabs audio was generated, but Android could not play it. Try again."
                _voiceProviderStatus.value = "ElevenLabs audio playback failed"
                _audioRms.value = 0f
                scheduleContinuousRestart()
                true
            }
            player.prepareAsync()
        } catch (e: Exception) {
            Log.w(TAG, "Unable to play generated ElevenLabs audio (${e.javaClass.simpleName}).")
            runCatching { elevenLabsPlayer?.release() }
            elevenLabsPlayer = null
            audioFile.delete()
            _voiceState.value = VoiceState.ERROR
            _errorMessage.value = "ElevenLabs audio was generated, but playback failed. Try again."
            _voiceProviderStatus.value = "ElevenLabs audio playback failed"
            scheduleContinuousRestart()
        }
    }

    private fun stopElevenLabsPlayback() {
        val player = elevenLabsPlayer
        elevenLabsPlayer = null
        if (player != null) {
            runCatching { if (player.isPlaying) player.stop() }
            runCatching { player.reset() }
            runCatching { player.release() }
        }
    }

    fun stopSpeaking() {
        speechRequestId++
        stopElevenLabsPlayback()
        if (textToSpeech?.isSpeaking == true) {
            textToSpeech?.stop()
        }
        if (_voiceState.value == VoiceState.SPEAKING) {
            _voiceState.value = VoiceState.IDLE
        }
        _audioRms.value = 0f
    }

    private fun createRecognitionListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _voiceState.value = VoiceState.LISTENING
            }

            override fun onBeginningOfSpeech() {
                _voiceState.value = VoiceState.LISTENING
            }

            override fun onRmsChanged(rmsdB: Float) {
                // Normalize dB (typically -2 to 10 dB) to 0.0 .. 1.0 range for UI waveform
                val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                _audioRms.value = normalized
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                _voiceState.value = VoiceState.PROCESSING
                _audioRms.value = 0f
            }

            override fun onError(error: Int) {
                val errorMsg = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error."
                    SpeechRecognizer.ERROR_CLIENT -> "Client error in speech service."
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Audio record permission needed."
                    SpeechRecognizer.ERROR_NETWORK -> "Network error during speech recognition."
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout."
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected. Try again!"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer is busy."
                    SpeechRecognizer.ERROR_SERVER -> "Speech server error."
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech heard."
                    else -> "Speech recognition error ($error)"
                }
                Log.w(TAG, "SpeechRecognizer error: $errorMsg")
                _errorMessage.value = errorMsg
                _voiceState.value = VoiceState.IDLE
                _audioRms.value = 0f
                if (error == SpeechRecognizer.ERROR_NO_MATCH ||
                    error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT ||
                    error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) {
                    scheduleContinuousRestart(1200L)
                }
            }

            override fun onResults(results: Bundle?) {
                _voiceState.value = VoiceState.IDLE
                _audioRms.value = 0f
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty() && matches[0].isNotBlank()) {
                    val text = matches[0]
                    _recognizedText.value = text
                    onFinalSpeechResult?.invoke(text)
                } else {
                    scheduleContinuousRestart(900L)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    _partialText.value = matches[0]
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    fun destroy() {
        continuousListeningEnabled = false
        speechRequestId++
        mainHandler.removeCallbacksAndMessages(null)
        stopElevenLabsPlayback()
        try {
            speechRecognizer?.destroy()
            textToSpeech?.stop()
            textToSpeech?.shutdown()
        } catch (e: Exception) {
            Log.e(TAG, "Error destroying VoiceManager", e)
        }
    }
}
