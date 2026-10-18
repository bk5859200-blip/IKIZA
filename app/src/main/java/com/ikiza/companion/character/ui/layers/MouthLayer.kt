package com.ikiza.companion.character.ui.layers

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.ikiza.companion.core.model.CharacterState
import com.ikiza.companion.core.model.CompanionExpression

/**
 * Mouth layer: renders resting anime mouth expressions and dynamic speaking mouth flap.
 */
@Composable
fun MouthLayer(
    state: CharacterState,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val breathOffsetY = (state.breathProgress - 0.5f) * 6f

        val mouthCenter = Offset(w * 0.50f, h * 0.61f + breathOffsetY)
        val mouthWidth = w * 0.08f

        if (state.isSpeaking && state.mouthOpenRatio > 0.1f) {
            // Speaking mouth flap
            drawSpeakingMouth(mouthCenter, mouthWidth, state.mouthOpenRatio)
        } else {
            // Expression resting pose
            when (state.expression) {
                CompanionExpression.HAPPY -> drawHappyMouth(mouthCenter, mouthWidth)
                CompanionExpression.SURPRISED -> drawSurprisedMouth(mouthCenter, mouthWidth)
                CompanionExpression.SLEEPY -> drawSleepyMouth(mouthCenter, mouthWidth)
                CompanionExpression.CURIOUS -> drawCuriousMouth(mouthCenter, mouthWidth)
                CompanionExpression.NEUTRAL -> drawNeutralMouth(mouthCenter, mouthWidth)
            }
        }
    }
}

private fun DrawScope.drawSpeakingMouth(center: Offset, mouthWidth: Float, openRatio: Float) {
    val currentHeight = mouthWidth * 0.9f * openRatio.coerceIn(0.1f, 1.0f)
    val halfW = mouthWidth * 0.55f

    // Mouth cavity
    val cavity = Path().apply {
        moveTo(center.x - halfW, center.y)
        quadraticBezierTo(center.x, center.y - currentHeight * 0.3f, center.x + halfW, center.y)
        quadraticBezierTo(center.x, center.y + currentHeight, center.x - halfW, center.y)
        close()
    }
    drawPath(cavity, Color(0xFFC84B31))

    // Tongue accent
    val tongue = Path().apply {
        moveTo(center.x - halfW * 0.6f, center.y + currentHeight * 0.4f)
        quadraticBezierTo(center.x, center.y + currentHeight, center.x + halfW * 0.6f, center.y + currentHeight * 0.4f)
        close()
    }
    drawPath(tongue, Color(0xFFFF85A1))

    // Outline
    drawPath(
        path = cavity,
        color = Color(0xFF2B2D42),
        style = Stroke(width = mouthWidth * 0.10f, cap = StrokeCap.Round)
    )
}

private fun DrawScope.drawHappyMouth(center: Offset, mouthWidth: Float) {
    val halfW = mouthWidth * 0.65f
    val depth = mouthWidth * 0.55f

    val openMouth = Path().apply {
        moveTo(center.x - halfW, center.y)
        lineTo(center.x + halfW, center.y)
        quadraticBezierTo(center.x, center.y + depth, center.x - halfW, center.y)
        close()
    }
    // Mouth interior
    drawPath(openMouth, Color(0xFFE05A47))

    // Tongue
    val tongue = Path().apply {
        moveTo(center.x - halfW * 0.5f, center.y + depth * 0.4f)
        quadraticBezierTo(center.x, center.y + depth, center.x + halfW * 0.5f, center.y + depth * 0.4f)
        close()
    }
    drawPath(tongue, Color(0xFFFF9EAA))

    // Outline
    drawPath(
        path = openMouth,
        color = Color(0xFF2B2D42),
        style = Stroke(width = mouthWidth * 0.12f, cap = StrokeCap.Round)
    )
}

private fun DrawScope.drawSurprisedMouth(center: Offset, mouthWidth: Float) {
    val radiusX = mouthWidth * 0.28f
    val radiusY = mouthWidth * 0.42f

    // Cute small open "O"
    drawOval(
        color = Color(0xFFC84B31),
        topLeft = Offset(center.x - radiusX, center.y - radiusY),
        size = Size(radiusX * 2, radiusY * 2)
    )
    drawOval(
        color = Color(0xFF2B2D42),
        topLeft = Offset(center.x - radiusX, center.y - radiusY),
        size = Size(radiusX * 2, radiusY * 2),
        style = Stroke(width = mouthWidth * 0.10f)
    )
}

private fun DrawScope.drawSleepyMouth(center: Offset, mouthWidth: Float) {
    val halfW = mouthWidth * 0.4f
    val softCurve = Path().apply {
        moveTo(center.x - halfW, center.y)
        quadraticBezierTo(center.x, center.y + mouthWidth * 0.08f, center.x + halfW, center.y)
    }
    drawPath(
        path = softCurve,
        color = Color(0xFF2B2D42),
        style = Stroke(width = mouthWidth * 0.11f, cap = StrokeCap.Round)
    )
}

private fun DrawScope.drawCuriousMouth(center: Offset, mouthWidth: Float) {
    val halfW = mouthWidth * 0.48f
    val smirkPath = Path().apply {
        moveTo(center.x - halfW * 0.8f, center.y)
        quadraticBezierTo(center.x, center.y + mouthWidth * 0.18f, center.x + halfW, center.y - mouthWidth * 0.08f)
    }
    drawPath(
        path = smirkPath,
        color = Color(0xFF2B2D42),
        style = Stroke(width = mouthWidth * 0.12f, cap = StrokeCap.Round)
    )
}

private fun DrawScope.drawNeutralMouth(center: Offset, mouthWidth: Float) {
    val halfW = mouthWidth * 0.45f
    val smilePath = Path().apply {
        moveTo(center.x - halfW, center.y)
        quadraticBezierTo(center.x, center.y + mouthWidth * 0.18f, center.x + halfW, center.y)
    }
    drawPath(
        path = smilePath,
        color = Color(0xFF2B2D42),
        style = Stroke(width = mouthWidth * 0.12f, cap = StrokeCap.Round)
    )
}
