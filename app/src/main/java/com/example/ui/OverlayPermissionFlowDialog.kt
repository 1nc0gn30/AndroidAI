package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PetState

@Composable
fun OverlayPermissionFlowDialog(
    petState: PetState,
    onPermissionGranted: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isGranted by remember { mutableStateOf(Settings.canDrawOverlays(context)) }

    // Result launcher for returning from the Android Settings screen
    val overlaySettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        val currentlyAllowed = Settings.canDrawOverlays(context)
        isGranted = currentlyAllowed
        if (currentlyAllowed) {
            onPermissionGranted()
        }
    }

    LaunchedEffect(Unit) {
        isGranted = Settings.canDrawOverlays(context)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("overlay_permission_dialog"),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(petState.species.primaryColorHex)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = petState.species.iconEmoji, fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Display Over Other Apps",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Status banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isGranted)
                            Color(0xFFD1FAE5)
                        else
                            MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Layers,
                            contentDescription = "Permission Status",
                            tint = if (isGranted) Color(0xFF065F46) else MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isGranted) "Permission Active" else "Permission Required",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (isGranted) Color(0xFF065F46) else MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = if (isGranted)
                                    "Your pet companion can float over apps anytime!"
                                else
                                    "System permission is needed to show the on-screen pet.",
                                fontSize = 11.sp,
                                color = if (isGranted) Color(0xFF047857) else MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                if (!isGranted) {
                    Text(
                        text = "Why is this permission needed?",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Text(
                        text = "To let ${petState.customName} float as an interactive pet and talking speech bubble on top of other apps, Android requires the 'Display over other apps' system toggle.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Step-by-step instructions
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Setup Steps:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "1. Tap \"Open Permission Settings\" below",
                                fontSize = 11.sp
                            )
                            Text(
                                text = "2. Toggle \"Allow display over other apps\" to ON",
                                fontSize = 11.sp
                            )
                            Text(
                                text = "3. Return to OmniPet AI to start your companion",
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Privacy Note
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Privacy",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Privacy-Safe: Does not record screen or touch data.",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                } else {
                    Text(
                        text = "Great! You are ready to have ${petState.customName} assist you on any app on your device.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        confirmButton = {
            if (!isGranted) {
                Button(
                    onClick = {
                        try {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            overlaySettingsLauncher.launch(intent)
                        } catch (_: Exception) {
                            val fallbackIntent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                            overlaySettingsLauncher.launch(fallbackIntent)
                        }
                    },
                    modifier = Modifier.testTag("open_overlay_settings_button")
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open Permission Settings")
                }
            } else {
                Button(
                    onClick = {
                        onPermissionGranted()
                        onDismiss()
                    },
                    modifier = Modifier.testTag("start_floating_pet_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981)
                    )
                ) {
                    Text("Launch Floating Pet")
                }
            }
        },
        dismissButton = {
            if (!isGranted) {
                OutlinedButton(
                    onClick = {
                        isGranted = Settings.canDrawOverlays(context)
                        if (isGranted) {
                            onPermissionGranted()
                        }
                    },
                    modifier = Modifier.testTag("check_permission_again_button")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Check Again")
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("Done")
                }
            }
        }
    )
}
