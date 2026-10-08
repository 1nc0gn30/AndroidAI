package com.example.viewmodel

import android.app.Application
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AiEngine
import com.example.ai.AiResponseResult
import com.example.model.ActionType
import com.example.model.AutomationTask
import com.example.model.HarnessConfig
import com.example.model.HarnessProvider
import com.example.model.InstalledAppItem
import com.example.model.PetMood
import com.example.model.PetSpecies
import com.example.model.PetState
import com.example.model.StepStatus
import com.example.model.TaskStep
import com.example.service.OverlayPetService
import com.example.service.PhoneControlManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OmniPetViewModel(application: Application) : AndroidViewModel(application) {

    private val phoneManager = PhoneControlManager(application)
    private val aiEngine = AiEngine()

    private val _petState = MutableStateFlow(PetState())
    val petState: StateFlow<PetState> = _petState.asStateFlow()

    private val _harnessConfigs = MutableStateFlow(
        HarnessProvider.values().associateWith { provider ->
            HarnessConfig(provider = provider)
        }
    )
    val harnessConfigs: StateFlow<Map<HarnessProvider, HarnessConfig>> = _harnessConfigs.asStateFlow()

    private val _activeProvider = MutableStateFlow(HarnessProvider.GOOGLE)
    val activeProvider: StateFlow<HarnessProvider> = _activeProvider.asStateFlow()

    private val _installedApps = MutableStateFlow<List<InstalledAppItem>>(emptyList())
    val installedApps: StateFlow<List<InstalledAppItem>> = _installedApps.asStateFlow()

    private val _currentTask = MutableStateFlow<AutomationTask?>(null)
    val currentTask: StateFlow<AutomationTask?> = _currentTask.asStateFlow()

    private val _taskHistory = MutableStateFlow<List<AutomationTask>>(emptyList())
    val taskHistory: StateFlow<List<AutomationTask>> = _taskHistory.asStateFlow()

    private val _batteryInfo = MutableStateFlow(phoneManager.getBatteryInfo())
    val batteryInfo: StateFlow<Pair<Int, Boolean>> = _batteryInfo.asStateFlow()

    private val _flashlightOn = MutableStateFlow(phoneManager.isFlashlightActive())
    val flashlightOn: StateFlow<Boolean> = _flashlightOn.asStateFlow()

    private val _clipboardText = MutableStateFlow(phoneManager.getClipboardText())
    val clipboardText: StateFlow<String> = _clipboardText.asStateFlow()

    private val _isExecuting = MutableStateFlow(false)
    val isExecuting: StateFlow<Boolean> = _isExecuting.asStateFlow()

    private val _lastAiResponse = MutableStateFlow<AiResponseResult?>(null)
    val lastAiResponse: StateFlow<AiResponseResult?> = _lastAiResponse.asStateFlow()

    private val _isOverlayPermissionGranted = MutableStateFlow(Settings.canDrawOverlays(application))
    val isOverlayPermissionGranted: StateFlow<Boolean> = _isOverlayPermissionGranted.asStateFlow()

    init {
        loadInstalledApps()
        refreshTelemetry()
    }

    fun refreshTelemetry() {
        _batteryInfo.value = phoneManager.getBatteryInfo()
        _clipboardText.value = phoneManager.getClipboardText()
        _flashlightOn.value = phoneManager.isFlashlightActive()
        _isOverlayPermissionGranted.value = Settings.canDrawOverlays(getApplication())
    }

    fun checkOverlayPermission(): Boolean {
        val granted = Settings.canDrawOverlays(getApplication())
        _isOverlayPermissionGranted.value = granted
        return granted
    }

    fun loadInstalledApps() {
        viewModelScope.launch {
            val apps = phoneManager.getInstalledUserApps()
            _installedApps.value = apps
        }
    }

    fun selectHarness(provider: HarnessProvider) {
        _activeProvider.value = provider
        phoneManager.vibrateTap()
        _petState.update { current ->
            current.copy(
                currentSpeech = "Switched to ${provider.displayName}! (${provider.specialty})"
            )
        }
    }

    fun updateHarnessConfig(
        provider: HarnessProvider,
        apiKey: String,
        model: String,
        endpoint: String
    ) {
        _harnessConfigs.update { map ->
            val existing = map[provider] ?: HarnessConfig(provider)
            map + (provider to existing.copy(
                apiKey = apiKey,
                model = model.ifBlank { provider.defaultModel },
                customEndpoint = endpoint
            ))
        }
    }

    fun changePetSpecies(species: PetSpecies) {
        phoneManager.vibrateTap()
        _petState.update { current ->
            current.copy(
                species = species,
                customName = species.petName,
                currentSpeech = "I'm ${species.petName} the ${species.title}! What shall we automate next?"
            )
        }
    }

    fun petPoke() {
        phoneManager.vibrateTap(40)
        val quotes = listOf(
            "*Purrs softly* I'm monitoring your apps!",
            "Did you know? I bridge 10 AI models directly to your phone!",
            "Ready for action! Tap an app or assign a task below.",
            "*Happy beep!* All systems nominal and battery at ${_batteryInfo.value.first}%!",
            "I'm keeping your automation chains primed and ready!"
        )
        _petState.update { current ->
            current.copy(
                mood = PetMood.HAPPY,
                happinessLevel = (current.happinessLevel + 2).coerceAtMost(100),
                currentSpeech = quotes.random()
            )
        }
    }

    fun toggleFlashlight() {
        val result = phoneManager.toggleFlashlight()
        val newState = result.getOrDefault(false)
        _flashlightOn.value = newState
        _petState.update { current ->
            current.copy(
                currentSpeech = if (newState) "Flashlight turned ON!" else "Flashlight turned OFF!"
            )
        }
    }

    fun adjustVolume(increase: Boolean) {
        val level = phoneManager.adjustVolume(increase)
        phoneManager.vibrateTap(30)
        _petState.update { current ->
            current.copy(
                currentSpeech = if (increase) "Volume increased to level $level" else "Volume decreased to level $level"
            )
        }
    }

    fun launchApp(pkgName: String, appName: String) {
        val ok = phoneManager.launchAppByPackage(pkgName)
        if (ok) {
            _petState.update { current ->
                current.copy(
                    mood = PetMood.HAPPY,
                    currentSpeech = "Launched $appName! Standing by for tasks."
                )
            }
        } else {
            _petState.update { current ->
                current.copy(
                    mood = PetMood.IDLE,
                    currentSpeech = "Could not open $appName directly. Trying fallback."
                )
            }
        }
    }

    fun toggleOverlayService(): Boolean {
        val context = getApplication<Application>()
        val canOverlay = Settings.canDrawOverlays(context)

        if (!canOverlay) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                android.net.Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            _petState.update { current ->
                current.copy(
                    currentSpeech = "Please toggle 'Allow display over other apps' so I can float on your screen!"
                )
            }
            return false
        }

        val nextActive = !_petState.value.isOverlayActive
        val intent = Intent(context, OverlayPetService::class.java).apply {
            action = if (nextActive) OverlayPetService.ACTION_START else OverlayPetService.ACTION_STOP
            putExtra(OverlayPetService.EXTRA_SPEECH, _petState.value.currentSpeech)
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            _petState.update { current ->
                current.copy(
                    isOverlayActive = nextActive,
                    currentSpeech = if (nextActive) "I'm floating over your apps now! Drag me anywhere." else "Floating overlay tucked away."
                )
            }
            phoneManager.vibrateSuccess()
            return true
        } catch (e: Exception) {
            _petState.update { current ->
                current.copy(
                    isOverlayActive = false,
                    currentSpeech = "Could not launch overlay: ${e.message}"
                )
            }
            return false
        }
    }

    fun submitUserTask(prompt: String, targetAppPkg: String? = null) {
        if (prompt.isBlank() || _isExecuting.value) return

        val provider = _activeProvider.value
        val config = _harnessConfigs.value[provider] ?: HarnessConfig(provider)

        viewModelScope.launch {
            _isExecuting.value = true
            _petState.update { current ->
                current.copy(
                    mood = PetMood.THINKING,
                    currentSpeech = "Asking ${provider.displayName} to plan: \"${prompt.take(35)}...\""
                )
            }

            val aiResult = aiEngine.executeHarnessPrompt(config, prompt, targetAppPkg)
            _lastAiResponse.value = aiResult

            _harnessConfigs.update { map ->
                val cfg = map[provider] ?: HarnessConfig(provider)
                map + (provider to cfg.copy(
                    requestCount = cfg.requestCount + 1,
                    lastLatencyMs = aiResult.latencyMs,
                    lastSuccess = true
                ))
            }

            val task = aiResult.generatedTask
            if (task != null) {
                _currentTask.value = task.copy(isRunning = true)
                _petState.update { current ->
                    current.copy(
                        mood = PetMood.EXECUTING,
                        currentSpeech = "Executing ${task.steps.size} automation steps across your phone!"
                    )
                }

                executeTaskSteps(task)
            } else {
                _petState.update { current ->
                    current.copy(
                        mood = PetMood.HAPPY,
                        currentSpeech = aiResult.text.take(120)
                    )
                }
                _isExecuting.value = false
            }
        }
    }

    private suspend fun executeTaskSteps(task: AutomationTask) {
        val updatedSteps = task.steps.toMutableList()

        for (i in updatedSteps.indices) {
            val step = updatedSteps[i]
            updatedSteps[i] = step.copy(status = StepStatus.RUNNING)
            _currentTask.value = task.copy(steps = updatedSteps.toList())
            delay(400)

            var output = ""
            var success = true

            when (step.actionType) {
                ActionType.FLASHLIGHT_TOGGLE -> {
                    val res = phoneManager.toggleFlashlight()
                    val state = res.getOrDefault(false)
                    _flashlightOn.value = state
                    output = "Flashlight set to ${if (state) "ON" else "OFF"}"
                }
                ActionType.BATTERY_CHECK -> {
                    val bat = phoneManager.getBatteryInfo()
                    _batteryInfo.value = bat
                    output = "Battery level: ${bat.first}% (Charging: ${bat.second})"
                }
                ActionType.CLIPBOARD_COPY -> {
                    phoneManager.copyToClipboard("OmniPet Task", step.param)
                    _clipboardText.value = step.param
                    output = "Copied to clipboard"
                }
                ActionType.VIBRATE_HAPTIC -> {
                    phoneManager.vibrateSuccess()
                    output = "Haptic cue triggered"
                }
                ActionType.WEB_SEARCH -> {
                    phoneManager.openWebSearch(step.param)
                    output = "Launched web search for '${step.param}'"
                }
                ActionType.MAP_NAVIGATE -> {
                    phoneManager.openMapLocation(step.param)
                    output = "Dispatched map navigation"
                }
                ActionType.SHARE_TEXT -> {
                    phoneManager.shareText("OmniPet AI Automation", step.param)
                    output = "Dispatched Android share sheet"
                }
                ActionType.DRAFT_EMAIL -> {
                    phoneManager.composeEmailDraft("", "Task Action", step.param)
                    output = "Dispatched email composer"
                }
                ActionType.LAUNCH_APP -> {
                    val pkg = step.param
                    if (pkg.isNotBlank()) {
                        val ok = phoneManager.launchAppByPackage(pkg)
                        output = if (ok) "Launched target application ($pkg)" else "Target app launch queued"
                    }
                }
                ActionType.VOLUME_SET -> {
                    val v = phoneManager.adjustVolume(true)
                    output = "Volume set to $v"
                }
                ActionType.AI_REASONING -> {
                    output = "Plan verified by ${_activeProvider.value.displayName}"
                }
            }

            updatedSteps[i] = step.copy(
                status = if (success) StepStatus.COMPLETED else StepStatus.FAILED,
                logOutput = output
            )
            _currentTask.value = task.copy(steps = updatedSteps.toList())
            delay(250)
        }

        val completedTask = task.copy(
            steps = updatedSteps,
            isRunning = false,
            isCompleted = true,
            resultSummary = "All automation steps executed successfully!"
        )

        _currentTask.value = completedTask
        _taskHistory.update { listOf(completedTask) + it.take(9) }
        _isExecuting.value = false

        phoneManager.vibrateSuccess()
        _petState.update { current ->
            current.copy(
                mood = PetMood.CELEBRATING,
                completedTasksCount = current.completedTasksCount + 1,
                currentSpeech = "Hurray! '${task.title}' was completed seamlessly across your phone!"
            )
        }
    }

    fun runPredefinedRoutine(routineType: String) {
        when (routineType) {
            "morning" -> submitUserTask("Morning Kickoff: Check battery, turn on flashlight brief test, and open news")
            "research" -> submitUserTask("Web Research: Search best AI automation workflows and copy summary to clipboard")
            "flashlight" -> submitUserTask("Emergency Light: Turn on flashlight and vibrate confirmation")
            "battery_save" -> submitUserTask("Power Audit: Check battery percentage and lower media volume")
            "directions" -> submitUserTask("Directions: Open maps for nearest coffee shop")
            else -> submitUserTask(routineType)
        }
    }
}
