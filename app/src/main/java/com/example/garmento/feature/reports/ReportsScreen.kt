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
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    onNavigateBack: () -> Unit,
    viewModel: ReportsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showPeriodPicker by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reports") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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

            Button(
                onClick = { viewModel.exportExcel(context) },
                enabled = !uiState.isExporting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(if (uiState.isExporting) "Preparing Excel..." else "Export to Excel")
            }
        }
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
                    labelsRef.value.getOrNull(axisValue.roundToInt()) ?: ""
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