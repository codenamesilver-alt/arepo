package com.example.garmento.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.garmento.ui.components.FocusedPillowButton
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private val Gold = Color(0xFFD4AF37)

@Composable
fun DashboardScreen(
    onNavigateToReminders: () -> Unit = {},
    onNavigateToCustomers: () -> Unit = {},
    onNavigateToAddCustomer: () -> Unit = {},
    onNavigateToSubscription: () -> Unit = {},
    onNavigateToServices: () -> Unit = {},
    onNavigateToItem: () -> Unit = {},
    onNavigateToOrderHistory: () -> Unit = {},
    onNavigateToExpenses: () -> Unit = {},
    onNavigateToReports: () -> Unit = {},
    onNavigateToOrders: () -> Unit = {},
    onNavigateToPlaceOrder: () -> Unit = {},
    onNavigateToPlans: () -> Unit = {},
    onNavigateToActiveSubscriptions: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToStaff: () -> Unit = {},
    onNavigateToTab: (MainTab) -> Unit = {},
    onNavigateToDeliveriesForDate: (String) -> Unit = {},
    viewModel: DashboardViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showDatePicker by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(LocalDate.now().toString()) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(top = 34.dp, bottom = 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // Pillow (pill) button with DASHBOARD written on top.
            Box(
                modifier = Modifier
                    .background(
                        color = Color.White,
                        shape = RoundedCornerShape(50)
                    )
                    .border(
                        width = 2.dp,
                        color = Gold,
                        shape = RoundedCornerShape(50)
                    )
                    .padding(horizontal = 40.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "DASHBOARD",
                    color = Gold,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    letterSpacing = 2.sp
                )
            }

            // 2x2 grid of pillow buttons just below.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FocusedPillowButton(label = "Order Today", figure = uiState.ordersToday.toString(), onPress = {}, width = 180, height = 93)
                    FocusedPillowButton(label = "Revenue Today", figure = "Rs.${String.format("%.0f", uiState.revenueToday)}", onPress = {}, width = 180, height = 93)
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FocusedPillowButton(label = "Revenue 30 days", figure = "Rs.${String.format("%.0f", uiState.revenueLast30Days)}", onPress = {}, width = 180, height = 93)
                    FocusedPillowButton(label = "Active Sub", figure = uiState.activeSubscriptionCount.toString(), onPress = {}, width = 180, height = 93)
                }
            }

            // Delivery Schedule wide button (1x2) with calendar.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                FocusedPillowButton(
                    label = "Delivery Schedule",
                    figure = selectedDate,
                    onPress = { showDatePicker = true },
                    width = 370,
                    height = 93
                )
            }

            // 2x3 grid of tabs below.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FocusedPillowButton(label = "Place Order", figure = "", onPress = onNavigateToPlaceOrder, width = 180, height = 93)
                    FocusedPillowButton(label = "Subscription", figure = "", onPress = onNavigateToSubscription, width = 180, height = 93)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FocusedPillowButton(label = "Add Customer", figure = "", onPress = onNavigateToAddCustomer, width = 180, height = 93)
                    FocusedPillowButton(label = "Expenses", figure = "", onPress = onNavigateToExpenses, width = 180, height = 93)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FocusedPillowButton(label = "Report", figure = "", onPress = onNavigateToReports, width = 180, height = 93)
                    FocusedPillowButton(label = "Settings", figure = "", onPress = onNavigateToSettings, width = 180, height = 93)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FocusedPillowButton(label = "Services", figure = "", onPress = onNavigateToServices, width = 180, height = 93)
                    FocusedPillowButton(label = "Item", figure = "", onPress = onNavigateToItem, width = 180, height = 93)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FocusedPillowButton(label = "Order History", figure = "", onPress = onNavigateToOrderHistory, width = 370, height = 93)
                }
            }
        }

        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            selectedDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().toString()
                            onNavigateToDeliveriesForDate(selectedDate)
                        }
                        showDatePicker = false
                    }) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }
}