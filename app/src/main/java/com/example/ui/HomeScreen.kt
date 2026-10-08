package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.AiResponseResult
import com.example.model.AutomationTask
import com.example.model.HarnessProvider
import com.example.model.InstalledAppItem
import com.example.model.StepStatus
import com.example.model.TaskStep
import com.example.viewmodel.OmniPetViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: OmniPetViewModel,
    modifier: Modifier = Modifier
) {
    val petState by viewModel.petState.collectAsState()
    val harnessConfigs by viewModel.harnessConfigs.collectAsState()
    val activeProvider by viewModel.activeProvider.collectAsState()
    val installedApps by viewModel.installedApps.collectAsState()
    val currentTask by viewModel.currentTask.collectAsState()
    val batteryInfo by viewModel.batteryInfo.collectAsState()
    val flashlightOn by viewModel.flashlightOn.collectAsState()
    val clipboardText by viewModel.clipboardText.collectAsState()
    val isExecuting by viewModel.isExecuting.collectAsState()
    val lastAiResponse by viewModel.lastAiResponse.collectAsState()
    val isOverlayPermissionGranted by viewModel.isOverlayPermissionGranted.collectAsState()

    var showHarnessDialog by remember { mutableStateOf(false) }
    var showPetDialog by remember { mutableStateOf(false) }
    var showOverlayPermissionDialog by remember { mutableStateOf(false) }
    var userPromptInput by remember { mutableStateOf("") }

    val batteryLevel = batteryInfo.first
    val isCharging = batteryInfo.second

    if (showOverlayPermissionDialog) {
        OverlayPermissionFlowDialog(
            petState = petState,
            onPermissionGranted = {
                viewModel.checkOverlayPermission()
                if (!petState.isOverlayActive) {
                    viewModel.toggleOverlayService()
                }
                showOverlayPermissionDialog = false
            },
            onDismiss = {
                viewModel.checkOverlayPermission()
                showOverlayPermissionDialog = false
            }
        )
    }

    if (showHarnessDialog) {
        HarnessSettingsDialog(
            harnessConfigs = harnessConfigs,
            activeProvider = activeProvider,
            onSaveConfig = { provider, key, model, endpoint ->
                viewModel.updateHarnessConfig(provider, key, model, endpoint)
            },
            onSelectActiveProvider = { viewModel.selectHarness(it) },
            onDismiss = { showHarnessDialog = false }
        )
    }

    if (showPetDialog) {
        PetCustomizationDialog(
            petState = petState,
            onSelectSpecies = { viewModel.changePetSpecies(it) },
            onDismiss = { showPetDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "OmniPet AI",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(activeProvider.badgeColorHex).copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = activeProvider.displayName,
                                color = Color(activeProvider.badgeColorHex),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showPetDialog = true },
                        modifier = Modifier.testTag("pet_dialog_button")
                    ) {
                        Text(text = petState.species.iconEmoji, fontSize = 20.sp)
                    }

                    IconButton(
                        onClick = { showHarnessDialog = true },
                        modifier = Modifier.testTag("harness_settings_button")
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = "AI Harness Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .widthIn(max = 680.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                PetStage(
                    petState = petState,
                    onPetTap = { viewModel.petPoke() }
                )
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clickable {
                            if (!isOverlayPermissionGranted) {
                                showOverlayPermissionDialog = true
                            }
                        }
                        .testTag("overlay_control_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (petState.isOverlayActive)
                            Color(0xFF312E81)
                        else if (!isOverlayPermissionGranted)
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Layers,
                                    contentDescription = "Screen Overlay",
                                    tint = if (petState.isOverlayActive)
                                        Color(0xFFA5B4FC)
                                    else if (!isOverlayPermissionGranted)
                                        MaterialTheme.colorScheme.tertiary
                                    else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (petState.isOverlayActive)
                                                "Float Pet Over Other Apps: ON"
                                            else "Float Pet Over Other Apps",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (petState.isOverlayActive) Color.White else MaterialTheme.colorScheme.onSurface
                                        )
                                        if (!isOverlayPermissionGranted) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = MaterialTheme.colorScheme.tertiary
                                            ) {
                                                Text(
                                                    text = "PERMISSION NEEDED",
                                                    fontSize = 9.sp,
                                                    color = MaterialTheme.colorScheme.onTertiary,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = if (!isOverlayPermissionGranted)
                                            "Tap to grant 'Display over other apps' permission"
                                        else if (petState.isOverlayActive)
                                            "Pet is floating! Drag anywhere across apps on screen"
                                        else
                                            "Shows companion character & bubble anywhere on phone",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (petState.isOverlayActive) Color(0xFFC7D2FE) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (isOverlayPermissionGranted) {
                                Switch(
                                    checked = petState.isOverlayActive,
                                    onCheckedChange = { viewModel.toggleOverlayService() },
                                    modifier = Modifier.testTag("overlay_toggle_switch")
                                )
                            } else {
                                Button(
                                    onClick = { showOverlayPermissionDialog = true },
                                    modifier = Modifier.testTag("request_overlay_permission_button")
                                ) {
                                    Text("Setup", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            item {
                PhoneControlsBar(
                    flashlightOn = flashlightOn,
                    batteryLevel = batteryLevel,
                    isCharging = isCharging,
                    onToggleFlashlight = { viewModel.toggleFlashlight() },
                    onAdjustVolume = { viewModel.adjustVolume(it) },
                    clipboardText = clipboardText
                )
            }

            item {
                HarnessSelectorSection(
                    activeProvider = activeProvider,
                    onSelectProvider = { viewModel.selectHarness(it) },
                    onOpenSettings = { showHarnessDialog = true }
                )
            }

            item {
                TaskPromptSection(
                    inputText = userPromptInput,
                    onInputChange = { userPromptInput = it },
                    isExecuting = isExecuting,
                    onSubmit = {
                        viewModel.submitUserTask(userPromptInput)
                        userPromptInput = ""
                    },
                    onRoutineSelect = { viewModel.runPredefinedRoutine(it) }
                )
            }

            if (currentTask != null) {
                item {
                    TaskExecutionCard(
                        task = currentTask!!,
                        lastAiResponse = lastAiResponse
                    )
                }
            }

            item {
                InstalledAppsSection(
                    apps = installedApps,
                    onLaunchApp = { pkg, name ->
                        viewModel.launchApp(pkg, name)
                    },
                    onAutomateWithApp = { pkg, name ->
                        viewModel.submitUserTask("Inspect and launch $name for daily workflow", pkg)
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun PhoneControlsBar(
    flashlightOn: Boolean,
    batteryLevel: Int,
    isCharging: Boolean,
    onToggleFlashlight: () -> Unit,
    onAdjustVolume: (Boolean) -> Unit,
    clipboardText: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("phone_controls_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "⚡ Phone Hardware & System Controls",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = onToggleFlashlight,
                    modifier = Modifier.testTag("flashlight_control_button"),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (flashlightOn) Color(0xFFFEF08A) else MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Icon(
                        imageVector = if (flashlightOn) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                        contentDescription = "Flashlight",
                        tint = if (flashlightOn) Color(0xFF854D0E) else MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (flashlightOn) "Torch ON" else "Torch OFF",
                        color = if (flashlightOn) Color(0xFF854D0E) else MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryFull,
                            contentDescription = "Battery",
                            tint = if (batteryLevel > 20) Color(0xFF10B981) else Color(0xFFEF4444),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "$batteryLevel%${if (isCharging) " ⚡" else ""}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                Row {
                    FilledTonalIconButton(
                        onClick = { onAdjustVolume(false) },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(Icons.Default.VolumeDown, contentDescription = "Vol Down", modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    FilledTonalIconButton(
                        onClick = { onAdjustVolume(true) },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(Icons.Default.VolumeUp, contentDescription = "Vol Up", modifier = Modifier.size(18.dp))
                    }
                }
            }

            if (clipboardText.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Clipboard", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Clipboard: \"${clipboardText.take(45)}${if (clipboardText.length > 45) "..." else ""}\"",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun HarnessSelectorSection(
    activeProvider: HarnessProvider,
    onSelectProvider: (HarnessProvider) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🤖 10 Mainstream AI Harnesses",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = "Configure API Keys",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onOpenSettings() }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(HarnessProvider.values()) { provider ->
                val isSelected = provider == activeProvider

                Card(
                    modifier = Modifier
                        .width(135.dp)
                        .clickable { onSelectProvider(provider) }
                        .testTag("harness_chip_${provider.id}"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected)
                            Color(provider.badgeColorHex)
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = provider.displayName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = provider.specialty,
                            fontSize = 10.sp,
                            color = if (isSelected) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TaskPromptSection(
    inputText: String,
    onInputChange: (String) -> Unit,
    isExecuting: Boolean,
    onSubmit: () -> Unit,
    onRoutineSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("task_prompt_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "✨ Cross-App Task Automator",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Your AI pet coordinates hardware actions, search, and app navigation.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                QuickRoutineChip("🌅 Morning Kickoff") { onRoutineSelect("morning") }
                QuickRoutineChip("🔍 Deep Research") { onRoutineSelect("research") }
                QuickRoutineChip("🔦 Emergency Light") { onRoutineSelect("flashlight") }
                QuickRoutineChip("🔋 Power Audit") { onRoutineSelect("battery_save") }
                QuickRoutineChip("📍 Navigation") { onRoutineSelect("directions") }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = onInputChange,
                    placeholder = { Text("Ask your pet to automate anything...") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("task_input_field"),
                    maxLines = 3,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onSubmit,
                    enabled = inputText.isNotBlank() && !isExecuting,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("submit_task_button")
                ) {
                    if (isExecuting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Send, contentDescription = "Run Task")
                    }
                }
            }
        }
    }
}

@Composable
fun QuickRoutineChip(label: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun TaskExecutionCard(
    task: AutomationTask,
    lastAiResponse: AiResponseResult?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("task_execution_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "⚙️ Executing: ${task.title}",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "Orchestrated by ${task.harnessProvider.displayName}",
                        fontSize = 11.sp,
                        color = Color(task.harnessProvider.badgeColorHex)
                    )
                }

                if (task.isRunning) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else if (task.isCompleted) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Completed",
                        tint = Color(0xFF10B981)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for ((index, step) in task.steps.withIndex()) {
                    StepItemRow(index = index + 1, step = step)
                }
            }

            if (lastAiResponse?.reasoningTrace != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "🧠 Model Reasoning Trace:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = lastAiResponse.reasoningTrace,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StepItemRow(index: Int, step: TaskStep) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = when (step.status) {
                StepStatus.COMPLETED -> Color(0xFF10B981)
                StepStatus.RUNNING -> MaterialTheme.colorScheme.primary
                StepStatus.PENDING -> MaterialTheme.colorScheme.outline
                StepStatus.FAILED -> Color(0xFFEF4444)
            },
            modifier = Modifier.size(22.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "$index",
                    fontSize = 11.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = step.title,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp
            )
            if (step.logOutput.isNotBlank()) {
                Text(
                    text = step.logOutput,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (step.status == StepStatus.RUNNING) {
            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
        }
    }
}

@Composable
fun InstalledAppsSection(
    apps: List<InstalledAppItem>,
    onLaunchApp: (String, String) -> Unit,
    onAutomateWithApp: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("installed_apps_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "📱 Device App Ecosystem & Integration",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "OmniPet pairs with installed applications for cross-app automation.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (app in apps.take(8)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = app.appName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "${app.category} • ${app.packageName.take(28)}...",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }

                        Row {
                            OutlinedButton(
                                onClick = { onLaunchApp(app.packageName, app.appName) },
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("Open", fontSize = 11.sp)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Button(
                                onClick = { onAutomateWithApp(app.packageName, app.appName) },
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("Automate", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
