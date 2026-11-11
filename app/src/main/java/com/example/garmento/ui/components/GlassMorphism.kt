package com.example.garmento.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Vibrant gradient background used as the base for glassmorphism surfaces.
 */
@Composable
fun GlassBackground(
    modifier: Modifier = Modifier,
    content: @Composable (androidx.compose.foundation.layout.BoxScope.() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F2027),
                        Color(0xFF203A43),
                        Color(0xFF2C5364)
                    )
                )
            )
    ) {
        content?.invoke(this)
    }
}

/**
 * A frosted-glass card: translucent white surface with a subtle border,
 * inner highlight, and soft shadow — Apple Liquid Glass inspired.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(cornerRadius),
                ambientColor = Color(0x40A855F7),
                spotColor = Color(0x40A855F7)
            )
            .background(
                color = Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(cornerRadius)
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.25f),
                shape = RoundedCornerShape(cornerRadius)
            )
    ) {
        content()
    }
}