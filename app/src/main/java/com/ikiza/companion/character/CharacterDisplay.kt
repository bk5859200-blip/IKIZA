package com.ikiza.companion.character

import android.graphics.Bitmap
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ikiza.companion.character.animation.CompanionAnimationState
import com.ikiza.companion.ui.theme.TextMutedDark

/**
 * Renders the character artwork on the AMOLED black screen with smooth, natural anime animations.
 *
 * Characteristics:
 * - Scaled cleanly with [ContentScale.Fit] so artwork is never distorted or cropped.
 * - Single-image compatible: delicate breathing, subtle vertical floating, speech bounce, and alert reactions.
 * - Lifecycle-aware: completely halts animation computations when character is hidden (pure AMOLED black).
 * - GPU-accelerated via graphicsLayer to avoid layout thrashing and preserve battery life.
 */
@Composable
fun CharacterDisplay(
    bitmap: Bitmap?,
    isVisible: Boolean,
    animationState: CompanionAnimationState = CompanionAnimationState.Idle,
    modifier: Modifier = Modifier
) {
    if (!isVisible) {
        return
    }

    // 1. Continuous breathing & floating loops (only active when visible)
    val infiniteTransition = rememberInfiniteTransition(label = "CompanionIdleFloating")

    // Idle breathing: 3.2s period, subtle ±3.5dp floating
    val idleOffsetY by infiniteTransition.animateFloat(
        initialValue = -3.5f,
        targetValue = 3.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "IdleOffsetY"
    )
    val idleScale by infiniteTransition.animateFloat(
        initialValue = 0.997f,
        targetValue = 1.006f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "IdleScale"
    )

    // Sleepy breathing: 5.4s period, very slow & calm
    val sleepyOffsetY by infiniteTransition.animateFloat(
        initialValue = -1.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "SleepyOffsetY"
    )
    val sleepyScale by infiniteTransition.animateFloat(
        initialValue = 0.998f,
        targetValue = 1.003f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "SleepyScale"
    )

    // Speaking rhythm: dynamic subtle pulse during TTS
    val speakingOffsetY by infiniteTransition.animateFloat(
        initialValue = -4.0f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 550, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "SpeakingOffsetY"
    )

    // Thinking subtle contemplative tilt:
    val thinkingTilt by infiniteTransition.animateFloat(
        initialValue = -1.0f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ThinkingTilt"
    )

    // Call rhythmic alert pulse:
    val callAlertOffsetY by infiniteTransition.animateFloat(
        initialValue = -6.0f,
        targetValue = 2.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "CallAlertOffsetY"
    )

    // 2. State-driven offsets, scale, and rotation
    val targetBaseYOffset = when (animationState) {
        CompanionAnimationState.Greeting -> -10f
        CompanionAnimationState.NotificationReaction -> -6f
        CompanionAnimationState.CallReaction -> callAlertOffsetY
        CompanionAnimationState.Speaking -> speakingOffsetY
        CompanionAnimationState.Thinking -> -2f
        CompanionAnimationState.Sleepy -> sleepyOffsetY
        CompanionAnimationState.Curious -> -3f
        CompanionAnimationState.Idle -> idleOffsetY
    }

    val animatedBaseYOffset by animateFloatAsState(
        targetValue = targetBaseYOffset,
        animationSpec = spring(stiffness = 200f),
        label = "AnimatedBaseYOffset"
    )

    val targetScale = when (animationState) {
        CompanionAnimationState.Greeting -> 1.025f
        CompanionAnimationState.NotificationReaction -> 1.012f
        CompanionAnimationState.CallReaction -> 1.010f
        CompanionAnimationState.Speaking -> 1.008f
        CompanionAnimationState.Thinking -> 1.002f
        CompanionAnimationState.Sleepy -> sleepyScale
        CompanionAnimationState.Curious -> 1.005f
        CompanionAnimationState.Idle -> idleScale
    }

    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(stiffness = 250f),
        label = "AnimatedScale"
    )

    val targetRotation = when (animationState) {
        CompanionAnimationState.Curious -> -1.8f
        CompanionAnimationState.NotificationReaction -> 1.0f
        CompanionAnimationState.Thinking -> thinkingTilt
        else -> 0.0f
    }

    val animatedRotation by animateFloatAsState(
        targetValue = targetRotation,
        animationSpec = spring(stiffness = 250f),
        label = "AnimatedRotation"
    )

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Companion Character",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationY = animatedBaseYOffset.dp.toPx()
                        scaleX = animatedScale
                        scaleY = animatedScale
                        rotationZ = animatedRotation
                    }
            )
        } else {
            Text(
                text = "Character revealed.\nLong-press to open settings.",
                color = TextMutedDark,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(32.dp)
            )
        }
    }
}
