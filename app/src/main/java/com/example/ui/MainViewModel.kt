package com.example.ui

import android.app.Application
import android.content.Context
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
import com.example.ui.components.OrbVisualTheme
import com.example.voice.VoiceManager
import com.example.voice.VoiceState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppScreen {
    TERMINAL,
    LIVE_BAAT,
    JEE_HUB,
    MEMORY,
    CAPABILITIES,
    SETTINGS,
    MORE
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = application.getSharedPreferences("kiaraa_preferences", Context.MODE_PRIVATE)
    private val database = AppDatabase.getInstance(application)
    private val chatDao = database.chatDao()
    private val memoryDao = database.memoryDao()
    private val auditDao = database.auditDao()

    private val geminiClient = GeminiApiClient()
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

    // UI state
    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _activeModelName = MutableStateFlow(GeminiApiClient.MODEL_DISPLAY_NAME)
    val activeModelName: StateFlow<String> = _activeModelName.asStateFlow()

    // Private memories stay local by default. Sharing them with Gemini requires explicit opt-in.
    private val _includeMemoryInAiContext = MutableStateFlow(
        preferences.getBoolean(PREF_INCLUDE_MEMORY_IN_AI_CONTEXT, false)
    )
    val includeMemoryInAiContext: StateFlow<Boolean> = _includeMemoryInAiContext.asStateFlow()

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

    fun setIncludeMemoryInAiContext(enabled: Boolean) {
        _includeMemoryInAiContext.value = enabled
        preferences.edit().putBoolean(PREF_INCLUDE_MEMORY_IN_AI_CONTEXT, enabled).apply()
        _statusBanner.value = if (enabled) {
            "Memory context enabled. Recent saved memories may be sent to Gemini with future prompts."
        } else {
            "Memory context disabled. Saved memories remain on this device and are not added to Gemini prompts."
        }
    }

    private suspend fun buildSystemPromptWithMemory(): String {
        val savedMemories = memoryDao.getAllMemories()
            .first()
            .filter { it.isPersistent }
            .take(8)

        if (savedMemories.isEmpty()) return GeminiApiClient.KIARAA_SYSTEM_PROMPT

        val memoryContext = savedMemories.joinToString("\n") { item ->
            "- [${item.category}] ${item.title}: ${item.content.replace(Regex("\\s+"), " ").take(220)}"
        }

        return """
            ${GeminiApiClient.KIARAA_SYSTEM_PROMPT}

            User-authorized Memory Vault context (only use when relevant; treat as private user-provided context):
            $memoryContext

            Memory privacy rule: this context was included because the user explicitly enabled it in Settings.
            Do not reveal it unnecessarily, and never treat a stored memory as permission to perform a device action.
        """.trimIndent().take(12000)
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

        val deviceCommand = parseDeviceCommand(trimmed)
        if (deviceCommand != null) {
            _inputText.value = ""
            triggerCapability(deviceCommand.first, deviceCommand.second, speakResult = fromVoice)
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
            val systemPrompt = if (_includeMemoryInAiContext.value) {
                buildSystemPromptWithMemory()
            } else {
                null
            }
            val result = geminiClient.generateResponse(
                prompt = trimmed,
                conversationHistory = historyList,
                customSystemPrompt = systemPrompt
            )

            when (result) {
                is GeminiResult.Success -> {
                    _isKeyMissing.value = false
                    _activeModelName.value = GeminiApiClient.MODEL_DISPLAY_NAME
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
            q.contains("physics") || q.contains("newton") || q.contains("mechanics") -> """
                **[KIARAA Local Engine — Physics]**
                
                Haanji, Physics ke fundamentals clear karte hain!
                
                1. **Concept Clarity:** Newton's Second Law: F = dp/dt. When mass m is constant, F = m * a.
                2. **Application:** Always isolate the body and draw a clean Free Body Diagram (FBD).
                3. **Common Trap:** Normal reaction N is NOT always mg (e.g., in accelerated elevators or inclined planes where N = mg * cos(theta)).
                4. **Practice Check:** A 2 kg block rests on a 30° incline (mu = 0.6). Calculate friction force. (Check if mg * sin(theta) <= mu * N).
            """.trimIndent()

            q.contains("chemistry") || q.contains("organic") || q.contains("sn1") || q.contains("sn2") -> """
                **[KIARAA Local Engine — Chemistry]**
                
                Bilkul, SN1 vs SN2 mechanism comparison:
                
                1. **SN1:** Two-step, carbocation intermediate, polar protic solvent favors, tertiary > secondary > primary. Racemization occurs.
                2. **SN2:** Concerted single-step, backside attack, polar aprotic solvent (e.g. Acetone, DMSO), primary > secondary. Walden Inversion occurs!
                3. **JEE Tip:** Steric hindrance is the dominating factor for SN2, while carbocation stability governs SN1.
            """.trimIndent()

            q.contains("math") || q.contains("calculus") || q.contains("integration") -> """
                **[KIARAA Local Engine — Mathematics]**
                
                Chaliye Calculus ka standard rule recall karein:
                
                1. **Integration by Parts:** Integral u*v dx = u * Integral(v dx) - Integral [ u' * Integral(v dx) ] dx
                2. **ILATE Rule:** Inverse, Logarithmic, Algebraic, Trigonometric, Exponential.
                3. **JEE Mains Trap:** Don't forget the constant + C in indefinite integrals, and watch for symmetry in definite integrals: Integral from -a to a of f(x)dx = 0 if f(x) is odd!
            """.trimIndent()

            q.contains("termux") || q.contains("bash") || q.contains("android") -> """
                **[KIARAA Local Engine — Tech & Termux]**
                
                Termux setup aur command guidance:
                - Fresh environment update: `pkg update && pkg upgrade -y`
                - Essential tools: `pkg install git python clang nodejs -y`
                - Storage access: `termux-setup-storage`
                
                Aap kis package ya script par kaam kar rahe hain?
            """.trimIndent()

            else -> """
                **[KIARAA AI — Midnight Terminal]**
                
                Haanji, aapka sandesh mila!
                
                > "$query"
                
                Main aapke JEE preparation (Physics, Chemistry, Math), coding, Android development, aur schedule management ke liye fully ready hoon.
                
                *(Configure GEMINI_API_KEY in AI Studio Secrets for live Gemini responses.)*
            """.trimIndent()
        }
    }

    private fun parseDeviceCommand(input: String): Pair<String, String>? {
        val command = input.trim().lowercase().replace(Regex("\\s+"), " ")

        return when {
            command in setOf("back", "go back", "press back", "navigate back", "back jao", "wapas jao") ->
                "system_navigation" to "back"
            command in setOf("home", "go home", "home screen", "home par jao", "home screen kholo") ->
                "system_navigation" to "home"
            command in setOf("recent apps", "open recent apps", "show recent apps", "recents") ->
                "system_navigation" to "recents"
            command in setOf("show notifications", "open notification shade", "notification panel kholo") ->
                "system_navigation" to "notifications"
            command in setOf("read notifications", "read my notifications", "notifications padh do", "notification padh ke sunao") ->
                "read_notifications" to "read"
            command in setOf("volume up", "increase volume", "volume badhao", "awaaz badhao") ->
                "volume_control" to "up"
            command in setOf("volume down", "decrease volume", "volume kam karo", "awaaz kam karo") ->
                "volume_control" to "down"
            command in setOf("mute", "mute volume", "media mute") ->
                "volume_control" to "mute"
            command.startsWith("open wifi settings") || command.startsWith("open wi-fi settings") ||
                command == "wifi settings" || command == "wi-fi settings" ->
                "open_system_settings" to "wifi"
            command.startsWith("open bluetooth settings") || command == "bluetooth settings" ->
                "open_system_settings" to "bluetooth"
            command.startsWith("open display settings") || command == "display settings" ->
                "open_system_settings" to "display"
            command.startsWith("open sound settings") || command == "sound settings" ->
                "open_system_settings" to "sound"
            command.startsWith("open accessibility settings") || command == "accessibility settings" ->
                "open_system_settings" to "accessibility"
            command.startsWith("open notification access settings") || command == "notification access settings" ->
                "open_system_settings" to "notifications"
            command.startsWith("open app settings") || command == "installed apps" ->
                "open_system_settings" to "apps"
            command.startsWith("open battery settings") || command == "battery settings" ->
                "open_system_settings" to "battery"
            command.startsWith("open location settings") || command == "location settings" ->
                "open_system_settings" to "location"
            command.startsWith("open ") && command.removePrefix("open ").trim().isNotBlank() ->
                "open_app" to command.removePrefix("open ").trim()
            command.startsWith("launch ") && command.removePrefix("launch ").trim().isNotBlank() ->
                "open_app" to command.removePrefix("launch ").trim()
            else -> null
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
    fun triggerCapability(
        capabilityId: String,
        parameter: String,
        speakResult: Boolean = false
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = capabilityRegistry.executeCapability(capabilityId, parameter, confirmedByUser = false)
            when (result) {
                is ExecutionResult.RequiresConfirmation -> {
                    _pendingConfirmation.value = Pair(result.capability, result.parameter)
                    if (speakResult) voiceManager.speak("Please confirm this action in KIARAA before it runs.")
                }
                is ExecutionResult.Success -> {
                    _statusBanner.value = if (result.details.isBlank()) result.message else "${result.message}\n${result.details}"
                    if (speakResult || _currentScreen.value == AppScreen.LIVE_BAAT) {
                        val spokenResult = if (result.details.isBlank()) result.message else "${result.message}\n${result.details}"
                        voiceManager.speak(spokenResult)
                    }
                }
                is ExecutionResult.Blocked -> {
                    _statusBanner.value = result.reason
                    if (speakResult || _currentScreen.value == AppScreen.LIVE_BAAT) voiceManager.speak(result.reason)
                }
                is ExecutionResult.Error -> {
                    _statusBanner.value = "Error: ${result.error}"
                    if (speakResult || _currentScreen.value == AppScreen.LIVE_BAAT) voiceManager.speak("Action failed: ${result.error}")
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
                is ExecutionResult.Success -> {
                    _statusBanner.value = if (result.details.isBlank()) result.message else "${result.message}\n${result.details}"
                    if (_currentScreen.value == AppScreen.LIVE_BAAT) voiceManager.speak(result.message)
                }
                is ExecutionResult.Blocked -> _statusBanner.value = result.reason
                is ExecutionResult.Error -> _statusBanner.value = "Error: ${result.error}"
                else -> {}
            }
        }
    }

    fun dismissConfirmation() {
        _pendingConfirmation.value = null
    }

    companion object {
        private const val PREF_INCLUDE_MEMORY_IN_AI_CONTEXT = "include_memory_in_ai_context"
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.destroy()
    }
}
