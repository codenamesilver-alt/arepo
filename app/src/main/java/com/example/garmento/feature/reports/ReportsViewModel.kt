package com.example.garmento.feature.reports

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

enum class ReportPeriod(val label: String) {
    MONTHLY("Monthly"),
    WEEKLY("Weekly")
}

data class ReportsUiState(
    val period: ReportPeriod = ReportPeriod.MONTHLY,
    val selectedMonth: YearMonth = YearMonth.now(),
    val selectedWeekDate: LocalDate = LocalDate.now(),
    val summary: TransactionSummary = TransactionSummary(),
    val revenuePerPlan: List<Pair<String, Double>> = emptyList(),
    val ordersPerCustomer: List<Pair<String, Int>> = emptyList(),
    val chartEntries: List<Pair<String, Double>> = emptyList(),
    val chartTitle: String = "",
    val periodLabel: String = "",
    val isLoading: Boolean = false,
    val isExporting: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val reportsRepository: ReportsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportsUiState())
    val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun onPeriodChange(period: ReportPeriod) {
        _uiState.update { it.copy(period = period) }
        refresh()
    }

    fun onMonthSelected(month: YearMonth) {
        _uiState.update { it.copy(selectedMonth = month) }
        refresh()
    }

    fun onWeekSelected(date: LocalDate) {
        val monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        _uiState.update { it.copy(selectedWeekDate = monday) }
        refresh()
    }

    fun refresh() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val (start, end) = rangeFor(state)
                val summary = reportsRepository.getTransactionSummary(start, end)
                val revenuePerPlan = reportsRepository.getRevenuePerPlan(start, end)
                val ordersPerCustomer = reportsRepository.getOrdersPerCustomer(start, end)
                val chartEntries = buildChartEntries(state, start, end)
                _uiState.update {
                    it.copy(
                        summary = summary,
                        revenuePerPlan = revenuePerPlan,
                        ordersPerCustomer = ordersPerCustomer,
                        chartEntries = chartEntries,
                        chartTitle = chartTitleFor(state),
                        periodLabel = periodLabelFor(state),
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to load reports") }
            }
        }
    }

    fun exportExcel(context: Context) {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, error = null) }
            try {
                val (start, end) = rangeFor(state)
                val uri = withContext(Dispatchers.IO) {
                    ExcelExporter.export(context, state.summary, state.revenuePerPlan, state.ordersPerCustomer, start, end)
                }
                share(context, uri)
                _uiState.update { it.copy(isExporting = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isExporting = false, error = e.message ?: "Failed to export report") }
            }
        }
    }

    private fun share(context: Context, uri: Uri) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.ms-excel"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(shareIntent, "Share report")
        )
    }

    private fun rangeFor(state: ReportsUiState): Pair<LocalDate, LocalDate> {
        return when (state.period) {
            ReportPeriod.MONTHLY -> {
                val start = state.selectedMonth.atDay(1)
                val end = state.selectedMonth.atEndOfMonth()
                start to end
            }
            ReportPeriod.WEEKLY -> {
                val monday = state.selectedWeekDate
                val sunday = monday.plusDays(6)
                monday to sunday
            }
        }
    }

    private suspend fun buildChartEntries(
        state: ReportsUiState,
        start: LocalDate,
        end: LocalDate
    ): List<Pair<String, Double>> {
        val revenueByDay = reportsRevenueByDay(start, end)
        return when (state.period) {
            ReportPeriod.MONTHLY -> {
                val days = start.lengthOfMonth()
                (1..days).map { day ->
                    val date = state.selectedMonth.atDay(day)
                    date.dayOfMonth.toString() to revenueByDay.getValue(date)
                }
            }
            ReportPeriod.WEEKLY -> {
                val labels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                (0L..6L).map { offset ->
                    val date = start.plusDays(offset)
                    labels[offset.toInt()] to revenueByDay.getValue(date)
                }
            }
        }
    }

    private suspend fun reportsRevenueByDay(start: LocalDate, end: LocalDate): Map<LocalDate, Double> {
        val transactions = reportsRepository.getTransactionsBetween(start, end)
        return transactions
            .mapNotNull { transaction ->
                runCatching {
                    LocalDate.parse(transaction.transaction_date.substring(0, 10)) to transaction.amount
                }.getOrNull()
            }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, amounts) -> amounts.sum() }
            .toMutableMap()
            .apply {
                var date = start
                while (!date.isAfter(end)) {
                    putIfAbsent(date, 0.0)
                    date = date.plusDays(1)
                }
            }
    }

    private fun chartTitleFor(state: ReportsUiState): String {
        return when (state.period) {
            ReportPeriod.MONTHLY -> "Revenue per day — ${state.selectedMonth.month.name} ${state.selectedMonth.year}"
            ReportPeriod.WEEKLY -> "Revenue per day — ${state.selectedWeekDate} to ${state.selectedWeekDate.plusDays(6)}"
        }
    }

    private fun periodLabelFor(state: ReportsUiState): String {
        return when (state.period) {
            ReportPeriod.MONTHLY -> state.selectedMonth.toString()
            ReportPeriod.WEEKLY -> "${state.selectedWeekDate} to ${state.selectedWeekDate.plusDays(6)}"
        }
    }
}