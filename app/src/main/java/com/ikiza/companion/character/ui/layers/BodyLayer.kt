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
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.ikiza.companion.core.model.CharacterState
import com.ikiza.companion.core.model.CompanionExpression

/**
 * Base body layer: Neck, shoulders, clothing collar, face contour, ears, and cheek blush.
 */
@Composable
fun BodyLayer(
    state: CharacterState,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Breathing vertical offset
        val breathOffsetY = (state.breathProgress - 0.5f) * 6f

        drawShouldersAndClothing(w, h, breathOffsetY)
        drawNeck(w, h, breathOffsetY)
        drawEars(w, h, breathOffsetY)
        drawFaceContour(w, h, breathOffsetY)
        drawBlush(w, h, breathOffsetY, state.expression)
    }
}

private fun DrawScope.drawShouldersAndClothing(w: Float, h: Float, offsetY: Float) {
    val clothingTop = h * 0.76f + offsetY
    val shoulderPath = Path().apply {
        moveTo(w * 0.05f, h)
        lineTo(w * 0.18f, clothingTop + h * 0.04f)
        quadraticBezierTo(w * 0.35f, clothingTop, w * 0.42f, clothingTop)
        lineTo(w * 0.58f, clothingTop)
        quadraticBezierTo(w * 0.65f, clothingTop, w * 0.82f, clothingTop + h * 0.04f)
        lineTo(w * 0.95f, h)
        close()
    }

    // Outfit body (Soft lavender / navy stylized companion hoodie)
    drawPath(
        path = shoulderPath,
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF4A4E69), Color(0xFF22223B)),
            startY = clothingTop,
            endY = h
        )
    )

    // Inner collar (White trim)
    val collarPath = Path().apply {
        moveTo(w * 0.40f, clothingTop)
        lineTo(w * 0.50f, clothingTop + h * 0.08f)
        lineTo(w * 0.60f, clothingTop)
        close()
    }
    drawPath(collarPath, Color(0xFFF7F7F7))

    // Cute ribbon / bowtie accent
    val ribbonPath = Path().apply {
        moveTo(w * 0.44f, clothingTop + h * 0.04f)
        lineTo(w * 0.50f, clothingTop + h * 0.06f)
        lineTo(w * 0.56f, clothingTop + h * 0.04f)
        lineTo(w * 0.50f, clothingTop + h * 0.075f)
        close()
    }
    drawPath(ribbonPath, Color(0xFFFF6B6B))
}

private fun DrawScope.drawNeck(w: Float, h: Float, offsetY: Float) {
    val neckWidth = w * 0.16f
    val neckTop = h * 0.62f + offsetY
    val neckBottom = h * 0.78f + offsetY
    val neckPath = Path().apply {
        moveTo((w - neckWidth) / 2, neckTop)
        lineTo((w - neckWidth) / 2, neckBottom)
        lineTo((w + neckWidth) / 2, neckBottom)
        lineTo((w + neckWidth) / 2, neckTop)
        close()
    }
    drawPath(neckPath, Color(0xFFF6DACB)) // Soft shaded neck tone
}

private fun DrawScope.drawEars(w: Float, h: Float, offsetY: Float) {
    val earY = h * 0.48f + offsetY
    val earRadiusX = w * 0.045f
    val earRadiusY = h * 0.06f

    // Left ear
    drawOval(
        color = Color(0xFFFBE6DC),
        topLeft = Offset(w * 0.20f, earY),
        size = Size(earRadiusX * 2, earRadiusY * 2)
    )
    // Right ear
    drawOval(
        color = Color(0xFFFBE6DC),
        topLeft = Offset(w * 0.71f, earY),
        size = Size(earRadiusX * 2, earRadiusY * 2)
    )
}

private fun DrawScope.drawFaceContour(w: Float, h: Float, offsetY: Float) {
    val faceTop = h * 0.24f + offsetY
    val chinY = h * 0.68f + offsetY
    val centerX = w * 0.50f

    // Soft rounded anime jaw / face contour
    val facePath = Path().apply {
        moveTo(w * 0.24f, faceTop + h * 0.16f)
        // Forehead / top contour
        quadraticBezierTo(centerX, faceTop - h * 0.02f, w * 0.76f, faceTop + h * 0.16f)
        // Right cheek and jaw down to chin
        quadraticBezierTo(w * 0.76f, h * 0.54f + offsetY, centerX + w * 0.08f, chinY - h * 0.02f)
        quadraticBezierTo(centerX, chinY, centerX - w * 0.08f, chinY - h * 0.02f)
        // Left jaw and cheek back up
        quadraticBezierTo(w * 0.24f, h * 0.54f + offsetY, w * 0.24f, faceTop + h * 0.16f)
        close()
    }

    drawPath(facePath, Color(0xFFFFF0E8)) // Radiant anime skin tone
}

private fun DrawScope.drawBlush(
    w: Float,
    h: Float,
    offsetY: Float,
    expression: CompanionExpression
) {
    val blushAlpha = when (expression) {
        CompanionExpression.HAPPY -> 0.65f
        CompanionExpression.SURPRISED -> 0.40f
        CompanionExpression.CURIOUS -> 0.45f
        CompanionExpression.SLEEPY -> 0.30f
        CompanionExpression.NEUTRAL -> 0.35f
    }

    val blushY = h * 0.53f + offsetY
    val blushColor = Color(0xFFFF85A1).copy(alpha = blushAlpha)

    // Left cheek blush
    drawOval(
        color = blushColor,
        topLeft = Offset(w * 0.28f, blushY),
        size = Size(w * 0.09f, h * 0.035f)
    )
    // Right cheek blush
    drawOval(
        color = blushColor,
        topLeft = Offset(w * 0.63f, blushY),
        size = Size(w * 0.09f, h * 0.035f)
    )
}
