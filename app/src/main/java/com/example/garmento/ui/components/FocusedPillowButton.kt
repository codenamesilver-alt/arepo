package com.example.garmento.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FocusedPillowButton(
    label: String = "Label",
    figure: String = "0",
    onPress: () -> Unit = {},
    width: Int = 200,
    height: Int = 106,
    modifier: Modifier = Modifier
) {
    val corner = 16.dp
    // Outer illuminated focus ring
    Box(
        modifier = modifier
            .padding(5.dp)
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(corner),
                ambientColor = Color(0xFFA855F7),
                spotColor = Color(0xFFA855F7)
            )
            .background(
                color = Color(0xFFF0DCFF),
                shape = RoundedCornerShape(corner)
            )
            .border(
                width = 3.dp,
                color = Color(0xFFDAB7FF),
                shape = RoundedCornerShape(corner)
            ),
        contentAlignment = Alignment.Center
    ) {
        // Button body
        Box(
            modifier = Modifier
                .size(width.dp, height.dp)
                .shadow(
                    elevation = 6.dp,
                    shape = RoundedCornerShape(corner),
                    ambientColor = Color(0xFF4C1D95),
                    spotColor = Color(0xFF4C1D95)
                )
                .clickable(onClick = onPress)
        ) {
            // Purple gradient surface
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colorStops = arrayOf(
                                0f to Color(0xFFC18AFF),
                                0.48f to Color(0xFF9551F5),
                                1f to Color(0xFF7135DC)
                            )
                        ),
                        shape = RoundedCornerShape(corner)
                    )
                    .border(
                        width = 1.dp,
                        color = Color(0xFFD5A5FF),
                        shape = RoundedCornerShape(corner)
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Top glossy highlight
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 1.dp, start = 12.dp, end = 12.dp)
                        .size(width = (width - 24).dp, height = 2.dp)
                        .background(
                            color = Color(0xFFFFFFFF).copy(alpha = 0.65f),
                            shape = RoundedCornerShape(10.dp)
                        )
                )
                if (figure.isBlank()) {
                    // Single label, centered.
                    Text(
                        text = label,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                } else {
                    // Two-part layout: label | figure, divided by vertical line
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Text(
                            text = label,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                        // Vertical divider line in the center
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .fillMaxHeight(0.7f)
                                .background(Color.White.copy(alpha = 0.7f))
                        )
                        Text(
                            text = figure,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}