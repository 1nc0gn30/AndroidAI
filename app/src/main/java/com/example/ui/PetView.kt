package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PetMood
import com.example.model.PetSpecies
import com.example.model.PetState

@Composable
fun PetStage(
    petState: PetState,
    onPetTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pet_idle")

    // Breathing float animation
    val breathingY by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing"
    )

    // Antenna glow pulse
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_pulse"
    )

    // Ear twitch
    val earTwitchAngle by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ear_twitch"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Speech Bubble
        PetSpeechBubble(
            text = petState.currentSpeech,
            mood = petState.mood,
            petName = petState.customName
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Cute Animated Pet Avatar Stage
        Box(
            modifier = Modifier
                .size(170.dp)
                .testTag("pet_avatar_touch_area")
                .clickable { onPetTap() },
            contentAlignment = Alignment.Center
        ) {
            // Ambient Aura Ring
            Canvas(
                modifier = Modifier
                    .size(160.dp)
                    .scale(glowPulse)
            ) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(petState.species.accentColorHex).copy(alpha = 0.35f),
                            Color(petState.species.primaryColorHex).copy(alpha = 0.10f),
                            Color.Transparent
                        )
                    )
                )
            }

            // Interactive Pet Body Canvas
            Canvas(
                modifier = Modifier
                    .size(130.dp)
                    .scale(if (petState.mood == PetMood.HAPPY) 1.05f else 1.0f)
                    .rotate(if (petState.mood == PetMood.EXECUTING) earTwitchAngle else 0f)
            ) {
                val cx = size.width / 2f
                val cy = size.height / 2f + breathingY

                val primaryColor = Color(petState.species.primaryColorHex)
                val accentColor = Color(petState.species.accentColorHex)
                val isSleeping = petState.mood == PetMood.SLEEPING

                // Antenna stem & beacon
                drawLine(
                    color = accentColor,
                    start = Offset(cx, cy - 42f),
                    end = Offset(cx, cy - 58f),
                    strokeWidth = 6f,
                    cap = StrokeCap.Round
                )
                drawCircle(
                    color = accentColor,
                    radius = 8f,
                    center = Offset(cx, cy - 60f)
                )

                // Ears according to species
                when (petState.species) {
                    PetSpecies.ROBO_BUNNY -> {
                        // Long bunny ears
                        drawRoundRect(
                            color = primaryColor,
                            topLeft = Offset(cx - 36f, cy - 80f),
                            size = Size(20f, 48f),
                            cornerRadius = CornerRadius(10f, 10f)
                        )
                        drawRoundRect(
                            color = accentColor,
                            topLeft = Offset(cx - 32f, cy - 76f),
                            size = Size(12f, 36f),
                            cornerRadius = CornerRadius(6f, 6f)
                        )
                        drawRoundRect(
                            color = primaryColor,
                            topLeft = Offset(cx + 16f, cy - 80f),
                            size = Size(20f, 48f),
                            cornerRadius = CornerRadius(10f, 10f)
                        )
                        drawRoundRect(
                            color = accentColor,
                            topLeft = Offset(cx + 20f, cy - 76f),
                            size = Size(12f, 36f),
                            cornerRadius = CornerRadius(6f, 6f)
                        )
                    }
                    PetSpecies.QUANTUM_PUP -> {
                        // Floppy puppy ears
                        drawRoundRect(
                            color = primaryColor,
                            topLeft = Offset(cx - 58f, cy - 25f),
                            size = Size(22f, 38f),
                            cornerRadius = CornerRadius(12f, 12f)
                        )
                        drawRoundRect(
                            color = primaryColor,
                            topLeft = Offset(cx + 36f, cy - 25f),
                            size = Size(22f, 38f),
                            cornerRadius = CornerRadius(12f, 12f)
                        )
                    }
                    else -> {
                        // Cat / Drake / Fox pointed ears
                        val leftEar = Path().apply {
                            moveTo(cx - 38f, cy - 22f)
                            lineTo(cx - 48f, cy - 52f)
                            lineTo(cx - 16f, cy - 36f)
                            close()
                        }
                        val rightEar = Path().apply {
                            moveTo(cx + 38f, cy - 22f)
                            lineTo(cx + 48f, cy - 52f)
                            lineTo(cx + 16f, cy - 36f)
                            close()
                        }
                        drawPath(leftEar, primaryColor)
                        drawPath(rightEar, primaryColor)
                    }
                }

                // Main Head Body
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.White, Color(0xFFF1F5F9))
                    ),
                    topLeft = Offset(cx - 46f, cy - 40f),
                    size = Size(92f, 80f),
                    cornerRadius = CornerRadius(36f, 36f)
                )

                // Face Screen / Visor
                drawRoundRect(
                    color = Color(0xFF0F172A),
                    topLeft = Offset(cx - 38f, cy - 28f),
                    size = Size(76f, 54f),
                    cornerRadius = CornerRadius(22f, 22f)
                )

                if (isSleeping) {
                    // Closed sleepy eyes ^ ^
                    drawArc(
                        color = Color(0xFF38BDF8),
                        startAngle = 0f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = Offset(cx - 28f, cy - 14f),
                        size = Size(16f, 10f),
                        style = Stroke(width = 4f, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = Color(0xFF38BDF8),
                        startAngle = 0f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = Offset(cx + 12f, cy - 14f),
                        size = Size(16f, 10f),
                        style = Stroke(width = 4f, cap = StrokeCap.Round)
                    )
                } else {
                    // Bright glowing eyes
                    val eyeColor = if (petState.mood == PetMood.HAPPY) Color(0xFF34D399) else Color(0xFF38BDF8)
                    drawOval(
                        color = eyeColor,
                        topLeft = Offset(cx - 26f, cy - 18f),
                        size = Size(14f, 20f)
                    )
                    drawOval(
                        color = eyeColor,
                        topLeft = Offset(cx + 12f, cy - 18f),
                        size = Size(14f, 20f)
                    )

                    // Eye highlights (sparkles)
                    drawCircle(
                        color = Color.White,
                        radius = 3.5f,
                        center = Offset(cx - 22f, cy - 14f)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 3.5f,
                        center = Offset(cx + 16f, cy - 14f)
                    )

                    // Cute smiling mouth
                    val mouthPath = Path().apply {
                        moveTo(cx - 8f, cy + 10f)
                        quadraticBezierTo(cx, cy + 16f, cx + 8f, cy + 10f)
                    }
                    drawPath(
                        path = mouthPath,
                        color = Color(0xFFF472B6),
                        style = Stroke(width = 4f, cap = StrokeCap.Round)
                    )
                }

                // Blush cheeks
                drawCircle(
                    color = Color(0xFFFB7185).copy(alpha = 0.7f),
                    radius = 6f,
                    center = Offset(cx - 30f, cy + 6f)
                )
                drawCircle(
                    color = Color(0xFFFB7185).copy(alpha = 0.7f),
                    radius = 6f,
                    center = Offset(cx + 30f, cy + 6f)
                )

                // Cute paws below head
                drawRoundRect(
                    color = primaryColor,
                    topLeft = Offset(cx - 30f, cy + 34f),
                    size = Size(20f, 16f),
                    cornerRadius = CornerRadius(8f, 8f)
                )
                drawRoundRect(
                    color = primaryColor,
                    topLeft = Offset(cx + 10f, cy + 34f),
                    size = Size(20f, 16f),
                    cornerRadius = CornerRadius(8f, 8f)
                )
            }

            // Floating status badge in corner
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 4.dp, bottom = 4.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color(petState.mood.bubbleColorHex),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = petState.species.iconEmoji,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = petState.mood.label,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Pet Stats strip
        Row(
            modifier = Modifier
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Happiness",
                    tint = Color(0xFFF43F5E),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${petState.happinessLevel}% Happy",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Completed Tasks",
                    tint = Color(0xFF8B5CF6),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${petState.completedTasksCount} Automations Done",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun PetSpeechBubble(
    text: String,
    mood: PetMood,
    petName: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(20.dp))
            .testTag("pet_speech_bubble"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "💬 $petName",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(mood.bubbleColorHex)
                )

                Text(
                    text = "Tap pet to interact",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
