package com.ikiza.companion.character.ui.layers

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.ikiza.companion.core.model.CharacterState
import com.ikiza.companion.core.model.CompanionExpression

/**
 * Eyes layer: renders anime eyes, iris gradients, specular light reflections,
 * blinking eyelids, and dynamic eyebrows corresponding to [CompanionExpression].
 */
@Composable
fun EyesLayer(
    state: CharacterState,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val breathOffsetY = (state.breathProgress - 0.5f) * 6f

        drawEyebrows(w, h, breathOffsetY, state.expression)

        val leftEyeCenter = Offset(w * 0.36f, h * 0.46f + breathOffsetY)
        val rightEyeCenter = Offset(w * 0.64f, h * 0.46f + breathOffsetY)
        val eyeWidth = w * 0.15f
        val baseEyeHeight = h * 0.11f

        when (state.expression) {
            CompanionExpression.HAPPY -> {
                // Happy curved eyes (^ ^)
                drawHappyEye(leftEyeCenter, eyeWidth, baseEyeHeight)
                drawHappyEye(rightEyeCenter, eyeWidth, baseEyeHeight)
            }
            CompanionExpression.SLEEPY -> {
                // Sleepy droopy half-closed eyes
                val effectiveOpen = (state.eyeOpenRatio * 0.35f).coerceAtLeast(0.0f)
                drawOpenEye(leftEyeCenter, eyeWidth, baseEyeHeight, effectiveOpen, isSurprised = false)
                drawOpenEye(rightEyeCenter, eyeWidth, baseEyeHeight, effectiveOpen, isSurprised = false)
            }
            CompanionExpression.SURPRISED -> {
                // Wide shocked circular eyes
                drawOpenEye(leftEyeCenter, eyeWidth, baseEyeHeight * 1.15f, state.eyeOpenRatio, isSurprised = true)
                drawOpenEye(rightEyeCenter, eyeWidth, baseEyeHeight * 1.15f, state.eyeOpenRatio, isSurprised = true)
            }
            CompanionExpression.CURIOUS -> {
                // Curious: left eye wide, right eye slightly narrowed
                drawOpenEye(leftEyeCenter, eyeWidth, baseEyeHeight, state.eyeOpenRatio, isSurprised = false)
                drawOpenEye(rightEyeCenter, eyeWidth, baseEyeHeight, state.eyeOpenRatio * 0.85f, isSurprised = false)
            }
            CompanionExpression.NEUTRAL -> {
                // Classic vibrant anime eyes
                drawOpenEye(leftEyeCenter, eyeWidth, baseEyeHeight, state.eyeOpenRatio, isSurprised = false)
                drawOpenEye(rightEyeCenter, eyeWidth, baseEyeHeight, state.eyeOpenRatio, isSurprised = false)
            }
        }
    }
}

private fun DrawScope.drawHappyEye(center: Offset, eyeWidth: Float, eyeHeight: Float) {
    val halfW = eyeWidth / 2
    val lashPath = Path().apply {
        moveTo(center.x - halfW, center.y + eyeHeight * 0.1f)
        quadraticBezierTo(center.x, center.y - eyeHeight * 0.35f, center.x + halfW, center.y + eyeHeight * 0.1f)
    }

    drawPath(
        path = lashPath,
        color = Color(0xFF2B2D42),
        style = Stroke(width = eyeWidth * 0.14f, cap = StrokeCap.Round)
    )

    // Secondary lash curl
    val curlPath = Path().apply {
        moveTo(center.x + halfW * 0.7f, center.y - eyeHeight * 0.1f)
        lineTo(center.x + halfW * 1.15f, center.y - eyeHeight * 0.25f)
    }
    drawPath(
        path = curlPath,
        color = Color(0xFF2B2D42),
        style = Stroke(width = eyeWidth * 0.08f, cap = StrokeCap.Round)
    )
}

private fun DrawScope.drawOpenEye(
    center: Offset,
    eyeWidth: Float,
    baseEyeHeight: Float,
    openRatio: Float,
    isSurprised: Boolean
) {
    val currentHeight = baseEyeHeight * openRatio.coerceIn(0.0f, 1.0f)
    val halfW = eyeWidth / 2

    if (openRatio < 0.15f) {
        // Closed blink line
        val blinkLine = Path().apply {
            moveTo(center.x - halfW, center.y)
            quadraticBezierTo(center.x, center.y + baseEyeHeight * 0.15f, center.x + halfW, center.y)
        }
        drawPath(
            path = blinkLine,
            color = Color(0xFF2B2D42),
            style = Stroke(width = eyeWidth * 0.12f, cap = StrokeCap.Round)
        )
        return
    }

    // 1. Sclera (Eye white)
    val scleraPath = Path().apply {
        moveTo(center.x - halfW, center.y)
        quadraticBezierTo(center.x, center.y - currentHeight * 0.65f, center.x + halfW, center.y)
        quadraticBezierTo(center.x, center.y + currentHeight * 0.65f, center.x - halfW, center.y)
        close()
    }
    drawPath(scleraPath, Color.White)

    // 2. Iris (Vibrant violet-indigo anime gradient)
    val irisRadiusX = halfW * 0.75f
    val irisRadiusY = currentHeight * 0.55f
    val irisCenter = Offset(center.x, center.y + (1.0f - openRatio) * baseEyeHeight * 0.1f)

    drawOval(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF3D348B), Color(0xFF7678ED), Color(0xFFF7B801)),
            startY = irisCenter.y - irisRadiusY,
            endY = irisCenter.y + irisRadiusY
        ),
        topLeft = Offset(irisCenter.x - irisRadiusX, irisCenter.y - irisRadiusY),
        size = Size(irisRadiusX * 2, irisRadiusY * 2)
    )

    // 3. Pupil (Dark center)
    val pupilRadius = if (isSurprised) irisRadiusX * 0.28f else irisRadiusX * 0.45f
    drawCircle(
        color = Color(0xFF1B1B2F),
        radius = pupilRadius,
        center = irisCenter
    )

    // 4. Specular Highlights (Sparkling anime light reflection)
    // Primary big light dot
    drawCircle(
        color = Color.White,
        radius = irisRadiusX * 0.32f,
        center = Offset(irisCenter.x - irisRadiusX * 0.30f, irisCenter.y - irisRadiusY * 0.32f)
    )
    // Secondary cute light dot
    drawCircle(
        color = Color.White.copy(alpha = 0.85f),
        radius = irisRadiusX * 0.18f,
        center = Offset(irisCenter.x + irisRadiusX * 0.30f, irisCenter.y + irisRadiusY * 0.25f)
    )

    // 5. Upper Eyelash Rim (Distinct anime thick lash)
    val upperLash = Path().apply {
        moveTo(center.x - halfW * 1.05f, center.y - currentHeight * 0.10f)
        quadraticBezierTo(
            center.x,
            center.y - currentHeight * 0.75f,
            center.x + halfW * 1.08f,
            center.y - currentHeight * 0.15f
        )
    }
    drawPath(
        path = upperLash,
        color = Color(0xFF2B2D42),
        style = Stroke(width = eyeWidth * 0.14f, cap = StrokeCap.Round)
    )
}

private fun DrawScope.drawEyebrows(
    w: Float,
    h: Float,
    offsetY: Float,
    expression: CompanionExpression
) {
    val browY = h * 0.38f + offsetY
    val browThickness = w * 0.022f
    val browColor = Color(0xFF5E503F)

    // Left eyebrow
    val leftBrow = Path()
    // Right eyebrow
    val rightBrow = Path()

    when (expression) {
        CompanionExpression.SURPRISED -> {
            // High arched surprised brows
            leftBrow.moveTo(w * 0.30f, browY - h * 0.025f)
            leftBrow.quadraticBezierTo(w * 0.36f, browY - h * 0.045f, w * 0.42f, browY - h * 0.02f)

            rightBrow.moveTo(w * 0.58f, browY - h * 0.02f)
            rightBrow.quadraticBezierTo(w * 0.64f, browY - h * 0.045f, w * 0.70f, browY - h * 0.025f)
        }
        CompanionExpression.CURIOUS -> {
            // Asymmetric: left brow raised higher
            leftBrow.moveTo(w * 0.30f, browY - h * 0.03f)
            leftBrow.quadraticBezierTo(w * 0.36f, browY - h * 0.05f, w * 0.42f, browY - h * 0.025f)

            rightBrow.moveTo(w * 0.58f, browY - h * 0.015f)
            rightBrow.quadraticBezierTo(w * 0.64f, browY - h * 0.025f, w * 0.70f, browY - h * 0.01f)
        }
        CompanionExpression.HAPPY -> {
            // Soft rounded gentle brows
            leftBrow.moveTo(w * 0.30f, browY - h * 0.015f)
            leftBrow.quadraticBezierTo(w * 0.36f, browY - h * 0.035f, w * 0.42f, browY - h * 0.01f)

            rightBrow.moveTo(w * 0.58f, browY - h * 0.01f)
            rightBrow.quadraticBezierTo(w * 0.64f, browY - h * 0.035f, w * 0.70f, browY - h * 0.015f)
        }
        CompanionExpression.SLEEPY -> {
            // Relaxed lowered flat brows
            leftBrow.moveTo(w * 0.30f, browY + h * 0.005f)
            leftBrow.lineTo(w * 0.42f, browY + h * 0.005f)

            rightBrow.moveTo(w * 0.58f, browY + h * 0.005f)
            rightBrow.lineTo(w * 0.70f, browY + h * 0.005f)
        }
        CompanionExpression.NEUTRAL -> {
            // Balanced natural anime brows
            leftBrow.moveTo(w * 0.30f, browY - h * 0.01f)
            leftBrow.quadraticBezierTo(w * 0.36f, browY - h * 0.025f, w * 0.42f, browY - h * 0.005f)

            rightBrow.moveTo(w * 0.58f, browY - h * 0.005f)
            rightBrow.quadraticBezierTo(w * 0.64f, browY - h * 0.025f, w * 0.70f, browY - h * 0.01f)
        }
    }

    drawPath(leftBrow, browColor, style = Stroke(width = browThickness, cap = StrokeCap.Round))
    drawPath(rightBrow, browColor, style = Stroke(width = browThickness, cap = StrokeCap.Round))
}
