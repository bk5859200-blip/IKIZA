package com.ikiza.companion.character.animation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.ikiza.companion.core.model.CharacterState
import com.ikiza.companion.core.model.CompanionExpression
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * Controller providing continuous idle animations:
 * - Idle breathing (smooth sinusoidal vertical displacement & scaling)
 * - Periodic natural blinking (spontaneous eye closure every 3-5 seconds)
 * - Mouth flap animation during active speech
 * - Expression parameter interpolation
 */
@Composable
fun rememberAnimatedCharacterState(
    targetExpression: CompanionExpression = CompanionExpression.NEUTRAL,
    isSpeaking: Boolean = false
): CharacterState {
    // 1. Idle breathing animation
    val infiniteTransition = rememberInfiniteTransition(label = "CharacterIdleBreathing")
    val breathCycle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (targetExpression == CompanionExpression.SLEEPY) 4500 else 3200,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BreathProgress"
    )

    // 2. Natural blinking scheduler
    var isBlinking by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            // Blink every 3.0 to 5.5 seconds
            val delayMs = Random.nextLong(3000L, 5500L)
            delay(delayMs)
            isBlinking = true
            // Quick anime blink duration: ~140ms
            delay(140L)
            isBlinking = false
        }
    }

    val eyeBlinkRatio by animateFloatAsState(
        targetValue = if (isBlinking) 0.0f else 1.0f,
        animationSpec = tween(durationMillis = 70, easing = LinearEasing),
        label = "BlinkRatio"
    )

    // 3. Speaking mouth flap animation
    var mouthFlapProgress by remember { mutableFloatStateOf(0.0f) }
    LaunchedEffect(isSpeaking) {
        if (isSpeaking) {
            while (true) {
                // Modulate mouth opening between 0.15 and 0.85
                mouthFlapProgress = 0.8f
                delay(120L)
                mouthFlapProgress = 0.2f
                delay(100L)
            }
        } else {
            mouthFlapProgress = 0.0f
        }
    }

    val animatedMouthRatio by animateFloatAsState(
        targetValue = if (isSpeaking) mouthFlapProgress else 0.0f,
        animationSpec = tween(durationMillis = 80, easing = LinearEasing),
        label = "MouthRatio"
    )

    // 4. Head tilt / gesture based on expression
    val headTiltDegrees by animateFloatAsState(
        targetValue = when (targetExpression) {
            CompanionExpression.CURIOUS -> 4.5f
            CompanionExpression.HAPPY -> -2.0f
            CompanionExpression.SLEEPY -> 2.0f
            else -> 0.0f
        },
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "HeadTilt"
    )

    return CharacterState(
        expression = targetExpression,
        eyeOpenRatio = eyeBlinkRatio,
        mouthOpenRatio = animatedMouthRatio,
        breathProgress = breathCycle,
        isSpeaking = isSpeaking,
        headTiltDegrees = headTiltDegrees
    )
}
