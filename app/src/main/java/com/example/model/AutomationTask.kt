package com.example.model

enum class ActionType(val label: String, val iconName: String) {
    LAUNCH_APP("Launch App", "open_in_new"),
    WEB_SEARCH("Web Query", "travel_explore"),
    CLIPBOARD_COPY("Copy to Clipboard", "content_copy"),
    FLASHLIGHT_TOGGLE("Toggle Flashlight", "flashlight_on"),
    VIBRATE_HAPTIC("Haptic Feedback", "vibration"),
    BATTERY_CHECK("Inspect Battery", "battery_charging_full"),
    VOLUME_SET("Adjust Volume", "volume_up"),
    MAP_NAVIGATE("Navigate Location", "navigation"),
    SHARE_TEXT("Share Intent", "share"),
    DRAFT_EMAIL("Draft Message", "email"),
    AI_REASONING("AI Agent Reasoning", "psychology")
}

enum class StepStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED
}

data class TaskStep(
    val id: String,
    val title: String,
    val actionType: ActionType,
    val param: String = "",
    val status: StepStatus = StepStatus.PENDING,
    val logOutput: String = ""
)

data class AutomationTask(
    val id: String,
    val title: String,
    val description: String,
    val harnessProvider: HarnessProvider,
    val targetApp: String? = null,
    val steps: List<TaskStep>,
    val isRunning: Boolean = false,
    val isCompleted: Boolean = false,
    val resultSummary: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
