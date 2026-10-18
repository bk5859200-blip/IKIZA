package com.ikiza.companion.character.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.ikiza.companion.character.ui.layers.BodyLayer
import com.ikiza.companion.character.ui.layers.EyesLayer
import com.ikiza.companion.character.ui.layers.HairLayer
import com.ikiza.companion.character.ui.layers.MouthLayer
import com.ikiza.companion.core.model.CharacterState

/**
 * Modular layered anime character drawn in Jetpack Compose.
 *
 * Layers:
 * 1. [bodyLayer]: Neck, shoulders, outfit, face contour, and blush
 * 2. [eyesLayer]: Sclera, iris gradient, pupil, highlights, blink lids, eyebrows
 * 3. [mouthLayer]: Expression resting smile / open mouth / speech flap
 * 4. [hairLayer]: Bangs, locks, hair shine halo, and hairpin
 *
 * Built with customizable composable slots so real artwork (PNG/WebP/Vector)
 * can be plugged in seamlessly without altering animation systems.
 */
@Composable
fun CharacterView(
    state: CharacterState,
    modifier: Modifier = Modifier,
    bodyLayer: @Composable (CharacterState) -> Unit = { BodyLayer(it) },
    eyesLayer: @Composable (CharacterState) -> Unit = { EyesLayer(it) },
    mouthLayer: @Composable (CharacterState) -> Unit = { MouthLayer(it) },
    hairLayer: @Composable (CharacterState) -> Unit = { HairLayer(it) }
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.85f)
            .graphicsLayer {
                // Subtle head tilt driven by expression
                rotationZ = state.headTiltDegrees
                // Subtle organic scale breathing expansion
                val scale = 1.0f + (state.breathProgress * 0.012f)
                scaleX = scale
                scaleY = scale
            },
        contentAlignment = Alignment.Center
    ) {
        // Layer 1: Body, head, and clothing
        bodyLayer(state)

        // Layer 2: Eyes and eyebrows
        eyesLayer(state)

        // Layer 3: Mouth and talking animations
        mouthLayer(state)

        // Layer 4: Front hair bangs, framing locks, and clips
        hairLayer(state)
    }
}
