package com.ikiza.companion.character.ui.layers

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.ikiza.companion.core.model.CharacterState

/**
 * Hair layer: renders stylized anime bangs, side framing locks, hair shine halo,
 * and a decorative companion hairpin.
 */
@Composable
fun HairLayer(
    state: CharacterState,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val breathOffsetY = (state.breathProgress - 0.5f) * 6f

        drawBackHair(w, h, breathOffsetY)
        drawFrontBangs(w, h, breathOffsetY)
        drawHairShine(w, h, breathOffsetY)
        drawHairPin(w, h, breathOffsetY)
    }
}

private fun DrawScope.drawBackHair(w: Float, h: Float, offsetY: Float) {
    val hairColor = Color(0xFF382933) // Rich chocolate / espresso anime hair
    val hairTop = h * 0.18f + offsetY
    val hairBottom = h * 0.72f + offsetY

    // Back hair dome and side volume
    val backHair = Path().apply {
        moveTo(w * 0.16f, hairBottom)
        quadraticBezierTo(w * 0.12f, h * 0.40f + offsetY, w * 0.22f, hairTop + h * 0.05f)
        quadraticBezierTo(w * 0.50f, hairTop - h * 0.03f, w * 0.78f, hairTop + h * 0.05f)
        quadraticBezierTo(w * 0.88f, h * 0.40f + offsetY, w * 0.84f, hairBottom)
        lineTo(w * 0.74f, hairBottom - h * 0.06f)
        quadraticBezierTo(w * 0.50f, hairBottom - h * 0.10f, w * 0.26f, hairBottom - h * 0.06f)
        close()
    }
    drawPath(backHair, hairColor)
}

private fun DrawScope.drawFrontBangs(w: Float, h: Float, offsetY: Float) {
    val hairColor = Color(0xFF47333F)
    val hairTop = h * 0.18f + offsetY

    // Left side framing lock
    val leftLock = Path().apply {
        moveTo(w * 0.22f, hairTop + h * 0.12f)
        quadraticBezierTo(w * 0.17f, h * 0.45f + offsetY, w * 0.20f, h * 0.62f + offsetY)
        quadraticBezierTo(w * 0.24f, h * 0.45f + offsetY, w * 0.26f, hairTop + h * 0.20f)
        close()
    }
    drawPath(leftLock, hairColor)

    // Right side framing lock
    val rightLock = Path().apply {
        moveTo(w * 0.78f, hairTop + h * 0.12f)
        quadraticBezierTo(w * 0.83f, h * 0.45f + offsetY, w * 0.80f, h * 0.62f + offsetY)
        quadraticBezierTo(w * 0.76f, h * 0.45f + offsetY, w * 0.74f, hairTop + h * 0.20f)
        close()
    }
    drawPath(rightLock, hairColor)

    // Layered front bangs covering the forehead
    val bangs = Path().apply {
        moveTo(w * 0.24f, hairTop + h * 0.10f)
        // Strand 1 (Left)
        quadraticBezierTo(w * 0.32f, hairTop + h * 0.24f, w * 0.34f, hairTop + h * 0.23f)
        // Strand 2 (Center-Left)
        quadraticBezierTo(w * 0.41f, hairTop + h * 0.28f, w * 0.43f, hairTop + h * 0.25f)
        // Strand 3 (Center-Right focal lock)
        quadraticBezierTo(w * 0.49f, hairTop + h * 0.31f, w * 0.52f, hairTop + h * 0.27f)
        // Strand 4 (Right)
        quadraticBezierTo(w * 0.60f, hairTop + h * 0.28f, w * 0.64f, hairTop + h * 0.24f)
        // Strand 5 (Right temple)
        quadraticBezierTo(w * 0.72f, hairTop + h * 0.22f, w * 0.76f, hairTop + h * 0.12f)
        // Crown curve connecting top
        quadraticBezierTo(w * 0.50f, hairTop - h * 0.02f, w * 0.24f, hairTop + h * 0.10f)
        close()
    }
    drawPath(bangs, hairColor)
}

private fun DrawScope.drawHairShine(w: Float, h: Float, offsetY: Float) {
    // Angelic highlight arc across the bangs
    val shineY = h * 0.28f + offsetY
    val shineColor = Color(0xFFFFD1DC).copy(alpha = 0.55f)

    // Left highlight dot
    drawCircle(
        color = shineColor,
        radius = w * 0.016f,
        center = Offset(w * 0.35f, shineY)
    )
    // Center highlight streak
    drawOval(
        color = shineColor,
        topLeft = Offset(w * 0.42f, shineY - h * 0.008f),
        size = androidx.compose.ui.geometry.Size(w * 0.08f, h * 0.016f)
    )
    // Right highlight streak
    drawOval(
        color = shineColor,
        topLeft = Offset(w * 0.56f, shineY - h * 0.008f),
        size = androidx.compose.ui.geometry.Size(w * 0.06f, h * 0.015f)
    )
}

private fun DrawScope.drawHairPin(w: Float, h: Float, offsetY: Float) {
    val pinCenter = Offset(w * 0.73f, h * 0.27f + offsetY)
    val starColor = Color(0xFFFFD166)

    // Star hairpin clip
    val starPath = Path().apply {
        val rOuter = w * 0.038f
        val rInner = w * 0.018f
        val points = 5
        var angle = -Math.PI / 2
        val step = Math.PI / points

        for (i in 0 until (points * 2)) {
            val r = if (i % 2 == 0) rOuter else rInner
            val x = pinCenter.x + (r * Math.cos(angle)).toFloat()
            val y = pinCenter.y + (r * Math.sin(angle)).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
            angle += step
        }
        close()
    }
    drawPath(starPath, starColor)
}
