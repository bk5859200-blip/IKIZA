package com.ikiza.companion.core.model

/**
 * Realtime state of the companion character animation and rendering.
 *
 * Designed to decouple animation logic from the rendering layers,
 * allowing procedural vector layers or real art assets (PNG/SVG) to be swapped cleanly.
 */
data class CharacterState(
    val expression: CompanionExpression = CompanionExpression.NEUTRAL,
    val eyeOpenRatio: Float = 1.0f,      // 1.0 = wide open, 0.0 = fully closed (blink)
    val mouthOpenRatio: Float = 0.0f,    // 0.0 = closed, 1.0 = fully open (speaking)
    val breathProgress: Float = 0.0f,    // 0.0 -> 1.0 oscillating breathing cycle
    val isSpeaking: Boolean = false,
    val headTiltDegrees: Float = 0.0f,
    val eyeGazeX: Float = 0.0f,          // -1.0 left to +1.0 right
    val eyeGazeY: Float = 0.0f           // -1.0 up to +1.0 down
)
