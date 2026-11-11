package com.example.garmento.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.random.Random

// Metallic gold palette for the tabs.
val Gold = Color(0xFFD4AF37)
val GoldDark = Color(0xFF8B6914)
val GoldLight = Color(0xFFF5E6A3)
val GoldSheen = Color(0xFFFFF2CC)

// Background color requested for screens.
val BackgroundGreen = Color(0xFF142C25)

data class GoldGrain(
    val pores: List<Pair<Offset, Float>>,
    val specks: List<Pair<Offset, Float>>,
    val brushLines: List<Pair<Float, Float>>
)

fun buildGoldGrain(seed: Int): GoldGrain {
    val rng = Random(seed)
    val pores = List(30) {
        Offset(rng.nextFloat() * 1000f, rng.nextFloat() * 1000f) to (0.5f + rng.nextFloat() * 1.4f)
    }
    val specks = List(90) {
        Offset(rng.nextFloat() * 1000f, rng.nextFloat() * 1000f) to (0.3f + rng.nextFloat() * 1.0f)
    }
    val brushLines = List(18) {
        rng.nextFloat() * 1000f to (0.4f + rng.nextFloat() * 0.6f)
    }
    return GoldGrain(pores, specks, brushLines)
}

/**
 * Draws a metallic gold texture into the given DrawScope.
 */
fun DrawScope.drawGoldTexture(grain: GoldGrain) {
    val w = size.width
    val h = size.height
    val scaleX = w / 1000f
    val scaleY = h / 1000f

    // Base gradient for metallic depth (bright center, darker edges).
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(GoldLight.copy(alpha = 0.55f), Gold, GoldDark.copy(alpha = 0.6f)),
            center = Offset(w * 0.4f, h * 0.3f),
            radius = w * 1.1f
        )
    )
    // Brushed-metal fine horizontal lines.
    grain.brushLines.forEach { (y, alpha) ->
        val yy = y * scaleY
        drawLine(
            color = GoldLight.copy(alpha = alpha * 0.25f),
            start = Offset(0f, yy),
            end = Offset(w, yy),
            strokeWidth = 0.6f
        )
    }
    // Pores (dark pits).
    grain.pores.forEach { (p, r) ->
        val x = p.x * scaleX
        val y = p.y * scaleY
        drawCircle(
            color = GoldDark.copy(alpha = 0.6f),
            radius = r * (w / 120f),
            center = Offset(x, y)
        )
    }
    // Light specks (metallic highlights).
    grain.specks.forEach { (p, r) ->
        drawCircle(
            color = GoldSheen.copy(alpha = 0.45f),
            radius = r * (w / 120f),
            center = Offset(p.x * scaleX, p.y * scaleY)
        )
    }
    // Specular sheen streak.
    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(Color.Transparent, GoldSheen.copy(alpha = 0.35f), Color.Transparent),
            start = Offset(w * 0.1f, 0f),
            end = Offset(w * 0.9f, h)
        )
    )
    // Vignette for worn edges.
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(Color.Transparent, GoldDark.copy(alpha = 0.5f)),
            center = Offset(w / 2f, h / 2f),
            radius = w * 0.85f
        )
    )
}

/**
 * A composable that fills its area with a metallic gold texture.
 */
@Composable
fun GoldBackground(modifier: Modifier = Modifier) {
    val grain = remember { buildGoldGrain(42) }
    Canvas(modifier = modifier) {
        drawRect(color = Gold)
        drawGoldTexture(grain)
    }
}