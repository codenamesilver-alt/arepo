package com.example.garmento.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.garmento.ui.theme.DisplayFont
import com.example.garmento.ui.theme.LabelTracking
import kotlin.random.Random

/**
 * Full-bleed woven textile ground: fine warp/weft threads with a fixed-seed
 * spacing drift (moiré) plus faint registration marks at the corners.
 */
@Composable
fun WovenBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val threadColor = MaterialTheme.colorScheme.primary
    val accent = MaterialTheme.colorScheme.tertiary
    val threads = remember {
        val generator = Random(17)
        List(42) { generator.nextFloat() }
    }
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val base = threadColor
            var k = 0
            var x = 0f
            // Warp (vertical) threads
            while (x <= size.width) {
                val line = base.copy(alpha = 0.022f + threads[k % threads.size] * 0.05f)
                drawLine(
                    color = line,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 1f,
                    cap = StrokeCap.Square
                )
                x += 5f + threads[k % threads.size] * 7f
                k++
            }
            var y = 0f
            // Weft (horizontal) threads
            while (y <= size.height) {
                val line = base.copy(alpha = 0.02f + threads[(k + 13) % threads.size] * 0.04f)
                drawLine(
                    color = line,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f,
                    cap = StrokeCap.Square
                )
                y += 7f + threads[(k + 23) % threads.size] * 9f
                k++
            }
            // Registration marks — top-left and bottom-right corners
            val mark = base.copy(alpha = 0.5f)
            val tick = 14.dp.toPx()
            drawLine(mark, Offset(10.dp.toPx(), 10.dp.toPx()), Offset(tick, 10.dp.toPx()), 1f)
            drawLine(mark, Offset(10.dp.toPx(), 10.dp.toPx()), Offset(10.dp.toPx(), tick), 1f)
            val brx = size.width - 10.dp.toPx()
            val bry = size.height - 10.dp.toPx()
            drawLine(accent.copy(alpha = 0.4f), Offset(brx - tick, bry), Offset(brx * 0.92f, bry), 1f)
            drawLine(accent.copy(alpha = 0.4f), Offset(brx, bry - tick), Offset(brx, bry * 0.92f), 1f)
        }
        content()
    }
}

/**
 * Editorial brand lockup: serif wordmark, clinical sub-caption with a hairline rule.
 */
@Composable
fun BrandLockup(
    modifier: Modifier = Modifier,
    wordmark: String = "GARMENTO"
) {
    val onBg = MaterialTheme.colorScheme.onBackground
    val accent = MaterialTheme.colorScheme.tertiary
    Box(modifier = modifier) {
        Canvas(Modifier.matchParentSize()) {
            val y = size.height * 0.18f
            val ruleWidth = size.width * 0.34f
            drawLine(
                color = accent,
                start = Offset(0f, y),
                end = Offset(ruleWidth, y),
                strokeWidth = 1.5f
            )
            drawCircle(
                color = accent,
                radius = 2.5f,
                center = Offset(ruleWidth, y)
            )
        }
        Text(
            text = wordmark,
            color = onBg,
            fontFamily = DisplayFont,
            fontWeight = FontWeight.SemiBold,
            fontSize = 40.sp,
            letterSpacing = 2.sp,
            lineHeight = 46.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(androidx.compose.ui.Alignment.Center)
        )
    }
}

@Composable
fun BrandSubcaption(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.tertiary,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = LabelTracking,
        textAlign = TextAlign.Center,
        modifier = modifier
    )
}