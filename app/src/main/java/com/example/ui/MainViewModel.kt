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
import com.example.operating.KiaraaNotificationListenerService
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

data class PendingDeviceConfirmation(
    val command: DeviceCommand,
    val originalInput: String,
    val fromVoice: Boolean,
    val title: String,
    val details: String
)

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

    // Potentially consequential screen interactions require an explicit UI confirmation.
    private val _pendingDeviceConfirmation = MutableStateFlow<PendingDeviceConfirmation?>(null)
    val pendingDeviceConfirmation: StateFlow<PendingDeviceConfirmation?> = _pendingDeviceConfirmation.asStateFlow()

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
        if (trimmed.isEmpty() || _isGenerating.value || _pendingDeviceConfirmation.value != null) return

        val parsedCommand = DeviceCommandParser.parse(trimmed)
        val deviceCommand = if (parsedCommand is DeviceCommand.OpenNotification) {
            parsedCommand.copy(expectedKey = KiaraaNotificationListenerService.captureNotificationKey(parsedCommand.index))
        } else {
            parsedCommand
        }
        if (deviceCommand != null) {
            // These screen actions are blocked in DeviceCommandExecutor until target-app
            // focus can be guaranteed; do not show a misleading "Confirm action" dialog.
            if (deviceCommand is DeviceCommand.Tap || deviceCommand is DeviceCommand.TypeText) {
                executeDeviceCommand(trimmed, deviceCommand, fromVoice)
                return
            }
            if (deviceCommand.requiresExplicitConfirmation()) {
                val (title, details) = when (deviceCommand) {
                    is DeviceCommand.Tap -> "Confirm screen tap" to
                        "KIARAA will tap at (${deviceCommand.x.toInt()}, ${deviceCommand.y.toInt()}) on the current screen. This may activate a button, send content, or submit a form. Verify the intended target before continuing."
                    is DeviceCommand.TypeText -> "Confirm text entry" to
                        "KIARAA will enter this text into the currently focused field:\n“${deviceCommand.text.take(240)}${if (deviceCommand.text.length > 240) "…" else ""}”\n\nKIARAA will not tap Send or Submit. Verify the destination field before continuing."
                    is DeviceCommand.OpenNotification -> "Open notification ${deviceCommand.index}?" to
                        "KIARAA will open only the notification identified when this confirmation was prepared. The list is rechecked before opening; if it changed or the target cannot be verified, nothing will open. This may switch apps or reveal private content. No preview is shown here."
                    else -> error("Confirmation requested for an unsupported command type.")
                }
                _inputText.value = ""
                _pendingDeviceConfirmation.value = PendingDeviceConfirmation(
                    command = deviceCommand,
                    originalInput = trimmed,
                    fromVoice = fromVoice,
                    title = title,
                    details = details
                )
                _statusBanner.value = "WAITING FOR CONFIRMATION • No screen action has been executed yet."
            } else {
                executeDeviceCommand(trimmed, deviceCommand, fromVoice)
            }
            return
        }

        _inputText.value = ""
        _isGenerating.value = true

        viewModelScope.launch(Dispatchers.IO) {
            // 1. Record User Message
            val userMsg = ChatMessage(
                role = "user",
                content = redactSensitiveForStorage(trimmed),
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
                            "\n\n*(Note: Gemini API rate limit or quota was reached. This is a limited offline response; full AI reasoning is unavailable until the API works again.)*"
                        val assistantMsg = ChatMessage(
                            role = "assistant",
                            content = localResponse,
                            mode = "local_companion"
                        )
                        chatDao.insertMessage(assistantMsg)

                        if (fromVoice || _currentScreen.value == AppScreen.LIVE_BAAT) {
                            voiceManager.speak(localResponse)
                        }
                        _statusBanner.value = "Gemini API rate limit or quota reached. Offline responses are limited."
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

    fun confirmPendingDeviceAction() {
        val pending = _pendingDeviceConfirmation.value ?: return
        _pendingDeviceConfirmation.value = null
        executeDeviceCommand(pending.originalInput, pending.command, pending.fromVoice)
    }

    fun cancelPendingDeviceAction() {
        _pendingDeviceConfirmation.value = null
        _statusBanner.value = "CANCELLED • No screen action was executed."
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
                // Notification previews and screen text can contain private messages or account data.
                // Show/speak results for this interaction, but never persist their contents in chat history.
                val storedResult = when (command) {
                    is DeviceCommand.ReadNotifications ->
                        "Recent notification previews were read for this interaction and were not saved to chat history."
                    DeviceCommand.ReadScreen ->
                        "Screen text was read for this interaction and was not saved to chat history."
                    else -> result.message.take(1600)
                }
                chatDao.insertMessage(ChatMessage(role = "assistant", content = storedResult, mode = "device_control"))
                if (fromVoice || _currentScreen.value == AppScreen.LIVE_BAAT) voiceManager.speak(spokenResult)
                val label = when (result.status) {
                    DeviceActionStatus.SUCCESS -> "DONE"
                    DeviceActionStatus.PARTIAL -> "PARTIAL"
                    DeviceActionStatus.CLARIFICATION -> "NEEDS CLARIFICATION"
                    DeviceActionStatus.NEEDS_ACCESSIBILITY, DeviceActionStatus.NEEDS_PERMISSION -> "PERMISSION NEEDED"
                    DeviceActionStatus.FAILED -> "FAILED"
                }
                val bannerMessage = if (command is DeviceCommand.ReadNotifications) result.message.take(500) else result.message.take(180)
                _statusBanner.value = "${label} • ${bannerMessage}"
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
        if (containsSensitiveCredential(title) || containsSensitiveCredential(content)) {
            _statusBanner.value = "Memory not saved: passwords, OTPs, PINs, API keys and credentials are not allowed in the Memory Vault."
            return
        }
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

    fun updateMemory(memory: MemoryItem) {
        if (containsSensitiveCredential(memory.title) || containsSensitiveCredential(memory.content)) {
            _statusBanner.value = "Memory not updated: passwords, OTPs, PINs, API keys and credentials are not allowed in the Memory Vault."
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            memoryDao.updateMemory(memory)
            _statusBanner.value = "Memory updated in the on-device vault."
        }
    }

    private fun containsSensitiveCredential(value: String): Boolean {
        val lower = value.lowercase()
        val credentialTerms = listOf("password", "passcode", "one-time password", "verification code", "security code", "otp", "pin code", "api key", "access token", "secret key")
        return credentialTerms.any { lower.contains(it) } ||
            Regex("""\b(?:otp|pin|verification code|security code)\s*(?:is|:|-)?\s*\d{4,8}\b""", RegexOption.IGNORE_CASE).containsMatchIn(value) || Regex("""^\s*\d{4,8}\s*$""").containsMatchIn(value)
    }

    private fun redactSensitiveForStorage(value: String): String =
        if (containsSensitiveCredential(value)) "[Potential credential content redacted from chat history]" else value

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
