package com.example.garmento.feature.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.garmento.ui.components.Gold

private val Stitch = Color(0xFFB48C4B)

enum class MainTab {
    DASHBOARD, CUSTOMERS, ORDERS, PLACED_ORDERS, PLANS, REPORTS
}

private data class MainTabItem(
    val label: String,
    val icon: ImageVector
)

@Composable
fun MainBottomBar(
    current: MainTab,
    onNavigate: (MainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        MainTab.DASHBOARD to MainTabItem("Dashboard", Icons.Filled.Home),
        MainTab.CUSTOMERS to MainTabItem("Customers", Icons.Filled.Person),
        MainTab.ORDERS to MainTabItem("Orders", Icons.Filled.ShoppingCart),
        MainTab.PLACED_ORDERS to MainTabItem("Placed Orders", Icons.AutoMirrored.Filled.List),
        MainTab.PLANS to MainTabItem("Plans", Icons.Filled.Star),
        MainTab.REPORTS to MainTabItem("Reports", Icons.Filled.DateRange)
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 6.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items.forEach { (tab, item) ->
                val selected = current == tab
                GoldTab(
                    label = item.label,
                    icon = item.icon,
                    selected = selected,
                    onClick = { if (current != tab) onNavigate(tab) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun GoldTab(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val corner = 12.dp
    Box(
        modifier = modifier
            .height(64.dp)
            .clip(RoundedCornerShape(corner))
            .background(Gold)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            // Stitched border on top.
            val stroke = 2.dp.toPx()
            val cornerPx = corner.toPx()
            val w = size.width
            val h = size.height
            drawRoundRect(
                color = Stitch,
                topLeft = Offset(stroke / 2f, stroke / 2f),
                size = Size(w - stroke, h - stroke),
                cornerRadius = CornerRadius(cornerPx, cornerPx),
                style = Stroke(
                    width = stroke,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f), 0f)
                )
            )
            drawRoundRect(
                color = Stitch.copy(alpha = 0.4f),
                topLeft = Offset(stroke * 2f, stroke * 2f),
                size = Size(w - stroke * 4f, h - stroke * 4f),
                cornerRadius = CornerRadius(cornerPx - stroke, cornerPx - stroke),
                style = Stroke(
                    width = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 6f), 0f)
                )
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) Color(0xFF1A1A1A) else Stitch,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = label,
                color = if (selected) Color(0xFF1A1A1A) else Stitch,
                fontSize = 9.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}