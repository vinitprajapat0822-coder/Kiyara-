package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.GeminiApiClient
import com.example.data.api.GeminiResult
import com.example.data.local.AppDatabase
import com.example.data.local.DefaultDataSeeder
import com.example.data.model.AuditLogEntry
import com.example.data.model.ChatMessage
import com.example.data.model.MemoryItem
import com.example.operating.CapabilityInfo
import com.example.operating.CapabilityRegistry
import com.example.operating.ExecutionResult
import com.example.operating.DeviceCommand
import com.example.operating.DeviceCommandParser
import com.example.operating.DeviceCommandExecutor
import com.example.operating.DeviceActionStatus
import com.example.ui.components.OrbVisualTheme
import com.example.voice.VoiceManager
import com.example.voice.VoiceState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppScreen {
    TERMINAL,
    LIVE_BAAT,
    MEMORY,
    SETTINGS
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val chatDao = database.chatDao()
    private val memoryDao = database.memoryDao()
    private val auditDao = database.auditDao()

    private val geminiClient = GeminiApiClient()
    private val deviceCommandExecutor = DeviceCommandExecutor(application)
    val capabilityRegistry = CapabilityRegistry(application, auditDao)
    val voiceManager = VoiceManager(application, viewModelScope)

    // Current navigation tab
    private val _currentScreen = MutableStateFlow(AppScreen.TERMINAL)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    // Messages flow from Room
    val messages: StateFlow<List<ChatMessage>> = chatDao.getAllMessages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Memories flow from Room
    val memories: StateFlow<List<MemoryItem>> = memoryDao.getAllMemories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Audit logs flow from Room
    val auditLogs: StateFlow<List<AuditLogEntry>> = auditDao.getAllLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI & Thinking State
    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _activeModelName = MutableStateFlow(GeminiApiClient.MODEL_FAST)
    val activeModelName: StateFlow<String> = _activeModelName.asStateFlow()

    private val _isKeyMissing = MutableStateFlow(false)
    val isKeyMissing: StateFlow<Boolean> = _isKeyMissing.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    // Confirmation dialog for risk-classified operations
    private val _pendingConfirmation = MutableStateFlow<Pair<CapabilityInfo, String>?>(null)
    val pendingConfirmation: StateFlow<Pair<CapabilityInfo, String>?> = _pendingConfirmation.asStateFlow()

    // Orb customization
    private val _orbTheme = MutableStateFlow(OrbVisualTheme.MIDNIGHT_TERMINAL)
    val orbTheme: StateFlow<OrbVisualTheme> = _orbTheme.asStateFlow()

    private val _orbPulsationRate = MutableStateFlow(1.0f)
    val orbPulsationRate: StateFlow<Float> = _orbPulsationRate.asStateFlow()

    init {
        // Wire voice manager final speech output into conversation flow
        voiceManager.onFinalSpeechResult = { text ->
            if (text.isNotBlank()) {
                sendMessage(text, fromVoice = true)
            }
        }

        // Seed first-run data atomically without duplicating existing memories.
        viewModelScope.launch(Dispatchers.IO) {
            DefaultDataSeeder(database).seedIfNeeded()
        }
    }

    fun setScreen(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun updateInputText(text: String) {
        _inputText.value = text
    }

    fun setOrbTheme(theme: OrbVisualTheme) {
        _orbTheme.value = theme
    }

    fun setOrbPulsationRate(rate: Float) {
        _orbPulsationRate.value = rate
    }

    fun dismissBanner() {
        _statusBanner.value = null
    }

    fun sendMessage(userText: String = _inputText.value, fromVoice: Boolean = false) {
        val trimmed = userText.trim()
        if (trimmed.isEmpty() || _isGenerating.value) return

        val deviceCommand = DeviceCommandParser.parse(trimmed)
        if (deviceCommand != null) {
            executeDeviceCommand(trimmed, deviceCommand, fromVoice)
            return
        }

        _inputText.value = ""
        _isGenerating.value = true

        viewModelScope.launch(Dispatchers.IO) {
            // 1. Record User Message
            val userMsg = ChatMessage(
                role = "user",
                content = trimmed,
                mode = "general"
            )
            chatDao.insertMessage(userMsg)

            // 2. Build history for context
            val historyList = messages.value.takeLast(6).map { Pair(it.role, it.content) }

            // 3. Request Gemini API (or local companion fallback if key missing)
            val result = geminiClient.generateResponse(
                prompt = trimmed,
                enableHighThinking = false,
                conversationHistory = historyList
            )

            when (result) {
                is GeminiResult.Success -> {
                    _isKeyMissing.value = false
                    _activeModelName.value = result.modelUsed
                    if (result.fallbackNotice != null) {
                        _statusBanner.value = result.fallbackNotice
                    }
                    val assistantMsg = ChatMessage(
                        role = "assistant",
                        content = result.text,
                        isThinking = result.thoughts.isNotEmpty(),
                        thoughts = result.thoughts,
                        mode = "general"
                    )
                    chatDao.insertMessage(assistantMsg)

                    // If user initiated from voice or in Live Baat, speak the response
                    if (fromVoice || _currentScreen.value == AppScreen.LIVE_BAAT) {
                        voiceManager.speak(result.text)
                    }
                }
                is GeminiResult.Error -> {
                    if (result.isKeyMissing) {
                        _isKeyMissing.value = true
                        val localResponse = generateLocalCompanionResponse(trimmed)
                        val assistantMsg = ChatMessage(
                            role = "assistant",
                            content = localResponse,
                            mode = "local_companion"
                        )
                        chatDao.insertMessage(assistantMsg)

                        if (fromVoice || _currentScreen.value == AppScreen.LIVE_BAAT) {
                            voiceManager.speak(localResponse)
                        }
                        _statusBanner.value = "Local Companion Mode: Add GEMINI_API_KEY in AI Studio Secrets for live AI."
                    } else if (result.isRateLimitOrQuota) {
                        // Resilient recovery: Answer using local reasoning engine so user is never blocked
                        val localResponse = generateLocalCompanionResponse(trimmed) +
                            "\n\n*(ℹ️ Note: Gemini API rate limit or quota was reached on available models. KIARAA answered using the local fallback engine.)*"
                        val assistantMsg = ChatMessage(
                            role = "assistant",
                            content = localResponse,
                            mode = "local_companion"
                        )
                        chatDao.insertMessage(assistantMsg)

                        if (fromVoice || _currentScreen.value == AppScreen.LIVE_BAAT) {
                            voiceManager.speak(localResponse)
                        }
                        _statusBanner.value = "Gemini API rate limit or quota reached. Switched to the local companion fallback."
                    } else {
                        val localResponse = generateLocalCompanionResponse(trimmed)
                        val assistantMsg = ChatMessage(
                            role = "assistant",
                            content = "$localResponse\n\n*(Network note: ${result.message})*",
                            mode = "local_companion"
                        )
                        chatDao.insertMessage(assistantMsg)
                        _statusBanner.value = result.message
                    }
                }
            }

            _isGenerating.value = false
        }
    }

    private fun generateLocalCompanionResponse(query: String): String {
        val q = query.lowercase()
        return when {
            q.contains("termux") || q.contains("bash") || q.contains("android") || q.contains("kotlin") || q.contains("code") ->
                "Haanji, main abhi offline/local mode mein hoon. Main safe command steps aur debugging guidance de sakti hoon, lekin phone par koi command chalane ka claim tabhi karungi jab execution verify ho."
            listOf("mood kharab", "sad hoon", "tired hoon", "stress", "pareshan").any(q::contains) ->
                "Kya haal hai? Main sun rahi hoon. Baat karni hai, thoda break plan karna hai, ya calming music ke options dekhne hain?"
            else ->
                "Haanji, main KIARAA hoon — Hinglish personal assistant. Gemini API abhi unavailable hai, isliye main sirf basic local help de sakti hoon. Network/API restore hone par detailed response milega; maine koi external action execute nahi kiya."
        }
    }

    private fun executeDeviceCommand(input: String, command: DeviceCommand, fromVoice: Boolean) {
        _inputText.value = ""
        _isGenerating.value = true
        _statusBanner.value = "PROCESSING • Checking requested device action…"
        viewModelScope.launch {
            var spokenResult = "Device action failed."
            try {
                val safeUserText = if (command is DeviceCommand.TypeText) {
                    "Type-text request received (text payload not saved)."
                } else input
                chatDao.insertMessage(ChatMessage(role = "user", content = safeUserText, mode = "device_control"))
                _statusBanner.value = "EXECUTING • Running the local Android action…"
                val result = deviceCommandExecutor.execute(command)
                spokenResult = result.message
                if (result.status == DeviceActionStatus.NEEDS_ACCESSIBILITY) {
                    _currentScreen.value = AppScreen.SETTINGS
                }
                chatDao.insertMessage(ChatMessage(role = "assistant", content = result.message.take(1600), mode = "device_control"))
                if (fromVoice || _currentScreen.value == AppScreen.LIVE_BAAT) voiceManager.speak(spokenResult)
                val label = when (result.status) {
                    DeviceActionStatus.SUCCESS -> "DONE"
                    DeviceActionStatus.PARTIAL -> "PARTIAL"
                    DeviceActionStatus.CLARIFICATION -> "NEEDS CLARIFICATION"
                    DeviceActionStatus.NEEDS_ACCESSIBILITY, DeviceActionStatus.NEEDS_PERMISSION -> "PERMISSION NEEDED"
                    DeviceActionStatus.FAILED -> "FAILED"
                }
                _statusBanner.value = "${label} • ${result.message.take(180)}"
            } catch (error: Exception) {
                spokenResult = "Device action failed: ${error.localizedMessage ?: "unknown Android error"}"
                _statusBanner.value = "FAILED • $spokenResult"
                runCatching {
                    chatDao.insertMessage(ChatMessage(role = "assistant", content = spokenResult, mode = "device_control"))
                }
                if (fromVoice || _currentScreen.value == AppScreen.LIVE_BAAT) voiceManager.speak(spokenResult)
            } finally {
                _isGenerating.value = false
            }
        }
    }

    // Memory Actions
    fun addMemory(title: String, content: String, category: String = "General") {
        viewModelScope.launch(Dispatchers.IO) {
            memoryDao.insertMemory(
                MemoryItem(
                    category = category,
                    title = title,
                    content = content,
                    source = "User Confirmed"
                )
            )
            _statusBanner.value = "Memory saved to on-device vault."
        }
    }

    fun deleteMemory(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            memoryDao.deleteById(id)
            _statusBanner.value = "Memory deleted from local vault."
        }
    }

    fun clearAllMemories() {
        viewModelScope.launch(Dispatchers.IO) {
            memoryDao.clearAll()
            _statusBanner.value = "All stored memories cleared."
        }
    }

    fun clearChatHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            chatDao.clearHistory()
            _statusBanner.value = "Chat session history reset."
        }
    }

    // Capability Executions
    fun triggerCapability(capabilityId: String, parameter: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = capabilityRegistry.executeCapability(capabilityId, parameter, confirmedByUser = false)
            when (result) {
                is ExecutionResult.RequiresConfirmation -> {
                    _pendingConfirmation.value = Pair(result.capability, result.parameter)
                }
                is ExecutionResult.Success -> {
                    _statusBanner.value = result.message
                }
                is ExecutionResult.Blocked -> {
                    _statusBanner.value = result.reason
                }
                is ExecutionResult.Error -> {
                    _statusBanner.value = "Error: ${result.error}"
                }
            }
        }
    }

    fun confirmPendingAction() {
        val pending = _pendingConfirmation.value ?: return
        _pendingConfirmation.value = null
        viewModelScope.launch(Dispatchers.IO) {
            val result = capabilityRegistry.executeCapability(pending.first.id, pending.second, confirmedByUser = true)
            when (result) {
                is ExecutionResult.Success -> _statusBanner.value = result.message
                is ExecutionResult.Blocked -> _statusBanner.value = result.reason
                is ExecutionResult.Error -> _statusBanner.value = "Error: ${result.error}"
                else -> {}
            }
        }
    }

    fun dismissConfirmation() {
        _pendingConfirmation.value = null
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.destroy()
    }
}
