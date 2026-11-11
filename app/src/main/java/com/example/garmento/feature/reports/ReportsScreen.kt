package com.example.garmento.feature.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.layer.ColumnCartesianLayer
import com.example.garmento.feature.dashboard.MainBottomBar
import com.example.garmento.feature.dashboard.MainTab
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToTab: (MainTab) -> Unit = {},
    viewModel: ReportsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val outstandingOrders by viewModel.outstandingOrders().collectAsStateWithLifecycle(initialValue = emptyList())
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showPeriodPicker by remember { mutableStateOf(false) }

    var orderStart by remember { mutableStateOf(LocalDate.now().minusDays(7)) }
    var orderEnd by remember { mutableStateOf(LocalDate.now()) }
    var subStart by remember { mutableStateOf(LocalDate.now().minusDays(7)) }
    var subEnd by remember { mutableStateOf(LocalDate.now()) }
    var expStart by remember { mutableStateOf(LocalDate.now().minusDays(7)) }
    var expEnd by remember { mutableStateOf(LocalDate.now()) }
    var deliveryDate by remember { mutableStateOf(LocalDate.now()) }

    var showOrderStartPicker by remember { mutableStateOf(false) }
    var showOrderEndPicker by remember { mutableStateOf(false) }
    var showSubStartPicker by remember { mutableStateOf(false) }
    var showSubEndPicker by remember { mutableStateOf(false) }
    var showExpStartPicker by remember { mutableStateOf(false) }
    var showExpEndPicker by remember { mutableStateOf(false) }
    var showDeliveryPicker2 by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Daily Sales — Last 10 Days", style = MaterialTheme.typography.titleSmall)
            RevenueBarChart(
                entries = uiState.dailySales10Days,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )

            PeriodSelector(
                period = uiState.period,
                onPeriodChange = viewModel::onPeriodChange
            )

            ReportPeriodButton(
                label = uiState.periodLabel,
                onPeriodClick = { showPeriodPicker = true }
            )

            if (showPeriodPicker) {
                if (uiState.period == ReportPeriod.MONTHLY) {
                    MonthPickerDialog(
                        initial = uiState.selectedMonth,
                        onConfirm = {
                            viewModel.onMonthSelected(it)
                            showPeriodPicker = false
                        },
                        onDismiss = { showPeriodPicker = false }
                    )
                } else {
                    WeekPickerDialog(
                        initial = uiState.selectedWeekDate,
                        onConfirm = {
                            viewModel.onWeekSelected(it)
                            showPeriodPicker = false
                        },
                        onDismiss = { showPeriodPicker = false }
                    )
                }
            }

            Text(
                uiState.chartTitle,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            RevenueBarChart(
                entries = uiState.chartEntries,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            )

            if (uiState.isLoading) {
                Spacer(modifier = Modifier.height(8.dp))
            }

            SummaryStats(summary = uiState.summary)

            BreakdownSection(
                title = "Revenue by Plan",
                rows = uiState.revenuePerPlan.map { (label, value) -> label to formatCurrency(value) }
            )

            BreakdownSection(
                title = "Orders by Customer",
                rows = uiState.ordersPerCustomer.map { (label, value) -> label to value.toString() }
            )

            // Order Report PDF
            Text("Order Report (PDF)", style = MaterialTheme.typography.titleSmall)
            DateRangeRow(
                start = orderStart, end = orderEnd,
                onStartPick = { showOrderStartPicker = true },
                onEndPick = { showOrderEndPicker = true }
            )
            Button(
                onClick = { viewModel.generateOrderReportPdf(context, orderStart, orderEnd) },
                enabled = !uiState.isExporting,
                modifier = Modifier.fillMaxWidth().height(44.dp)
            ) { Text("Generate Order Report PDF") }

            // Subscription Report PDF
            Text("Subscription Report (PDF)", style = MaterialTheme.typography.titleSmall)
            DateRangeRow(
                start = subStart, end = subEnd,
                onStartPick = { showSubStartPicker = true },
                onEndPick = { showSubEndPicker = true }
            )
            Button(
                onClick = { viewModel.generateSubscriptionReportPdf(context, subStart, subEnd) },
                enabled = !uiState.isExporting,
                modifier = Modifier.fillMaxWidth().height(44.dp)
            ) { Text("Generate Subscription Report PDF") }

            // Expense Report PDF
            Text("Expense Report (PDF)", style = MaterialTheme.typography.titleSmall)
            DateRangeRow(
                start = expStart, end = expEnd,
                onStartPick = { showExpStartPicker = true },
                onEndPick = { showExpEndPicker = true }
            )
            Button(
                onClick = { viewModel.generateExpenseReportPdf(context, expStart, expEnd) },
                enabled = !uiState.isExporting,
                modifier = Modifier.fillMaxWidth().height(44.dp)
            ) { Text("Generate Expense Report PDF") }

            // Delivery Report PDF
            Text("Delivery Report (PDF)", style = MaterialTheme.typography.titleSmall)
            OutlinedButton(
                onClick = { showDeliveryPicker2 = true },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Delivery Date: $deliveryDate") }
            Button(
                onClick = { viewModel.generateDeliveryReportPdf(context, deliveryDate) },
                enabled = !uiState.isExporting,
                modifier = Modifier.fillMaxWidth().height(44.dp)
            ) { Text("Generate Delivery Report PDF") }

            // Outstanding Payment section
            Text("Outstanding Payment", style = MaterialTheme.typography.titleSmall)
            if (outstandingOrders.isEmpty()) {
                Text("No outstanding payments.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                outstandingOrders.forEach { o ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Order #${o.order_number}", style = MaterialTheme.typography.titleSmall, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                            Text("Customer: ${o.customer_name ?: ""}", style = MaterialTheme.typography.bodySmall)
                            Text("Date: ${o.order_date}", style = MaterialTheme.typography.bodySmall)
                            Text("Pending: Rs.${String.format("%.2f", o.total_amount - o.paid_amount)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                            Button(
                                onClick = { viewModel.markPaid(o.id) },
                                modifier = Modifier.fillMaxWidth().height(40.dp)
                            ) { Text("Mark as Paid") }
                        }
                    }
                }
            }
            Button(
                onClick = { viewModel.generateOutstandingPdf(context) },
                modifier = Modifier.fillMaxWidth().height(44.dp)
            ) { Text("Generate Outstanding PDF") }

            Button(
                onClick = { viewModel.exportExcel(context) },
                enabled = !uiState.isExporting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(if (uiState.isExporting) "Preparing..." else "Export to Excel")
            }

            if (showOrderStartPicker) {
                SimpleDatePicker(initial = orderStart, onConfirm = { orderStart = it; showOrderStartPicker = false }, onDismiss = { showOrderStartPicker = false })
            }
            if (showOrderEndPicker) {
                SimpleDatePicker(initial = orderEnd, onConfirm = { orderEnd = it; showOrderEndPicker = false }, onDismiss = { showOrderEndPicker = false })
            }
            if (showSubStartPicker) {
                SimpleDatePicker(initial = subStart, onConfirm = { subStart = it; showSubStartPicker = false }, onDismiss = { showSubStartPicker = false })
            }
            if (showSubEndPicker) {
                SimpleDatePicker(initial = subEnd, onConfirm = { subEnd = it; showSubEndPicker = false }, onDismiss = { showSubEndPicker = false })
            }
            if (showExpStartPicker) {
                SimpleDatePicker(initial = expStart, onConfirm = { expStart = it; showExpStartPicker = false }, onDismiss = { showExpStartPicker = false })
            }
            if (showExpEndPicker) {
                SimpleDatePicker(initial = expEnd, onConfirm = { expEnd = it; showExpEndPicker = false }, onDismiss = { showExpEndPicker = false })
            }
            if (showDeliveryPicker2) {
                SimpleDatePicker(initial = deliveryDate, onConfirm = { deliveryDate = it; showDeliveryPicker2 = false }, onDismiss = { showDeliveryPicker2 = false })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateRangeRow(
    start: LocalDate,
    end: LocalDate,
    onStartPick: () -> Unit,
    onEndPick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(onClick = onStartPick, modifier = Modifier.weight(1f)) {
            Text("From: $start", style = MaterialTheme.typography.bodySmall)
        }
        OutlinedButton(onClick = onEndPick, modifier = Modifier.weight(1f)) {
            Text("To: $end", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SimpleDatePicker(
    initial: LocalDate,
    onConfirm: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { millis ->
                    onConfirm(java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneId.systemDefault()).toLocalDate())
                }
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    ) {
        DatePicker(state = state, showModeToggle = false)
    }
}

@Composable
private fun PeriodSelector(
    period: ReportPeriod,
    onPeriodChange: (ReportPeriod) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = period == ReportPeriod.MONTHLY,
            onClick = { onPeriodChange(ReportPeriod.MONTHLY) },
            label = { Text("Monthly") }
        )
        FilterChip(
            selected = period == ReportPeriod.WEEKLY,
            onClick = { onPeriodChange(ReportPeriod.WEEKLY) },
            label = { Text("Weekly") }
        )
    }
}

@Composable
private fun ReportPeriodButton(label: String, onPeriodClick: () -> Unit) {
    OutlinedButton(
        onClick = onPeriodClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(Icons.Filled.DateRange, contentDescription = null)
        Spacer(modifier = Modifier.padding(horizontal = 4.dp))
        Text(label)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MonthPickerDialog(
    initial: YearMonth,
    onConfirm: (YearMonth) -> Unit,
    onDismiss: () -> Unit
) {
    val initialMillis = initial.atDay(1).toUtcMillis()
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let { millis ->
                    onConfirm(YearMonth.from(millis.fromUtcMillis()))
                }
            }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    ) {
        DatePicker(state = datePickerState, showModeToggle = false)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeekPickerDialog(
    initial: LocalDate,
    onConfirm: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initial.toUtcMillis())
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let { millis ->
                    onConfirm(millis.fromUtcMillis())
                }
            }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    ) {
        DatePicker(state = datePickerState, showModeToggle = false)
    }
}

@Composable
private fun RevenueBarChart(
    entries: List<Pair<String, Double>>,
    modifier: Modifier = Modifier
) {
    if (entries.isEmpty()) {
        Card(modifier = modifier) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "No revenue data for this period",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    val modelProducer = remember { CartesianChartModelProducer() }
    val labels = remember(entries) { entries.map { it.first } }
    val labelsRef = rememberUpdatedState(labels)

    LaunchedEffect(entries) {
        modelProducer.runTransaction {
            columnSeries {
                series(y = entries.map { it.second })
            }
        }
    }

    val columnProvider = ColumnCartesianLayer.ColumnProvider.series(
        rememberLineComponent(
            fill = fill(MaterialTheme.colorScheme.primary),
            thickness = 20.dp
        )
    )

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberColumnCartesianLayer(columnProvider = columnProvider),
            startAxis = VerticalAxis.rememberStart(),
            bottomAxis = HorizontalAxis.rememberBottom(
                valueFormatter = { _, axisValue, _ ->
                    labelsRef.value.getOrNull(axisValue.roundToInt()) ?: axisValue.toInt().toString()
                }
            )
        ),
        modelProducer = modelProducer,
        modifier = modifier
    )
}

@Composable
private fun SummaryStats(summary: TransactionSummary) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatCard("Revenue", formatCurrency(summary.totalRevenue), Modifier.weight(1f))
        StatCard("Orders", summary.orderCount.toString(), Modifier.weight(1f))
        StatCard("Transactions", summary.transactionCount.toString(), Modifier.weight(1f))
    }
}

@Composable
private fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun BreakdownSection(
    title: String,
    rows: List<Pair<String, String>>
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            if (rows.isEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("No data", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                rows.forEach { (label, value) ->
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                        Text(value, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

private fun formatCurrency(value: Double): String = String.format("%.2f", value)

private fun LocalDate.toUtcMillis(): Long =
    atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.fromUtcMillis(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()