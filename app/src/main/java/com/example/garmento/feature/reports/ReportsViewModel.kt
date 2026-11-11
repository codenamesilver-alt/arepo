package com.example.garmento.feature.reports

import com.example.garmento.core.util.DateParsing

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
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
    val dailySales10Days: List<Pair<String, Double>> = emptyList(),
    val chartTitle: String = "",
    val periodLabel: String = "",
    val isLoading: Boolean = false,
    val isExporting: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val reportsRepository: ReportsRepository,
    private val orderRepository: com.example.garmento.feature.orders.OrderRepository,
    private val orderDao: com.example.garmento.core.data.local.dao.OrderDao,
    private val subscriptionDao: com.example.garmento.core.data.local.dao.SubscriptionDao,
    private val customerDao: com.example.garmento.core.data.local.dao.CustomerDao,
    private val planDao: com.example.garmento.core.data.local.dao.PlanDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportsUiState())
    val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()

    init {
        refresh()
        loadDailySales()
    }

    private fun loadDailySales() {
        viewModelScope.launch {
            try {
                val data = reportsRepository.getDailySalesLast10Days()
                _uiState.update { it.copy(dailySales10Days = data) }
            } catch (_: Exception) { }
        }
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

    fun generateOrderReportPdf(context: Context, start: LocalDate, end: LocalDate) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, error = null) }
            try {
                withContext(Dispatchers.IO) {
                    val cid = com.example.garmento.core.util.UserSession.currentCompanyId ?: return@withContext
                    val orders = orderDao.getAllOrdersWithCustomers(cid).first().filter { o ->
                        try { com.example.garmento.core.util.DateParsing.parseLocalDateTime(o.order_date).toLocalDate() in start..end } catch (_: Exception) { false }
                    }
                    val lines = mutableListOf<String>()
                    lines.add("Date | Order# | Customer | Mobile | Items | Amount")
                    orders.forEach { o ->
                        val items = o.items_json ?: ""
                        lines.add("${o.order_date.take(10)} | ${o.order_number} | ${o.customer_name} | ${o.customer_phone} | ${o.clothes_count} items | Rs.${String.format("%.2f", o.total_amount)}")
                        if (items.isNotBlank()) lines.add("   items: $items")
                    }
                    if (orders.isEmpty()) lines.add("No orders in range.")
                    val file = PdfReportGenerator.createPdf(context, "order_report.pdf", "Order Report ($start to $end)", lines)
                    PdfReportGenerator.sharePdf(context, file)
                }
                _uiState.update { it.copy(isExporting = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isExporting = false, error = e.message ?: "Failed to generate PDF") }
            }
        }
    }

    fun generateSubscriptionReportPdf(context: Context, start: LocalDate, end: LocalDate) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, error = null) }
            try {
                withContext(Dispatchers.IO) {
                    val cid = com.example.garmento.core.util.UserSession.currentCompanyId ?: return@withContext
                    val subs = subscriptionDao.getAllSubscriptions(cid).filter { s ->
                        try { com.example.garmento.core.util.DateParsing.parseLocalDate(s.start_date) in start..end } catch (_: Exception) { false }
                    }
                    val customers = customerDao.getAllCustomersOnce(cid).associateBy { it.id }
                    val plans = planDao.getAllPlansOnce(cid).associateBy { it.id }
                    val lines = mutableListOf<String>()
                    lines.add("Customer | Mobile | Plan | Amount | Quota Left")
                    subs.forEach { s ->
                        val c = customers[s.customer_id]
                        val p = plans[s.plan_id]
                        val quotaLeft = s.clothes_quota - s.clothes_used
                        lines.add("${c?.name ?: "Unknown"} | ${c?.phone ?: ""} | ${p?.name ?: ""} | Rs.${String.format("%.2f", p?.amount ?: 0.0)} | $quotaLeft")
                    }
                    if (subs.isEmpty()) lines.add("No subscriptions in range.")
                    val file = PdfReportGenerator.createPdf(context, "subscription_report.pdf", "Subscription Report ($start to $end)", lines)
                    PdfReportGenerator.sharePdf(context, file)
                }
                _uiState.update { it.copy(isExporting = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isExporting = false, error = e.message ?: "Failed to generate PDF") }
            }
        }
    }

    fun generateExpenseReportPdf(context: Context, start: LocalDate, end: LocalDate) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, error = null) }
            try {
                withContext(Dispatchers.IO) {
                    val expenses = com.example.garmento.feature.expenses.ExpensesStore.expenses.value.filter { e ->
                        try { com.example.garmento.core.util.DateParsing.parseLocalDate(e.date) in start..end } catch (_: Exception) { false }
                    }
                    val lines = mutableListOf<String>()
                    lines.add("Head | Amount | Date | Notes")
                    expenses.forEach { e ->
                        lines.add("${e.head} | ${e.amount} | ${e.date} | ${e.notes}")
                    }
                    if (expenses.isEmpty()) lines.add("No expenses in range.")
                    val file = PdfReportGenerator.createPdf(context, "expense_report.pdf", "Expense Report ($start to $end)", lines)
                    PdfReportGenerator.sharePdf(context, file)
                }
                _uiState.update { it.copy(isExporting = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isExporting = false, error = e.message ?: "Failed to generate PDF") }
            }
        }
    }

    fun outstandingOrders(): kotlinx.coroutines.flow.Flow<List<com.example.garmento.core.data.local.dao.OrderDao.OrderWithCustomer>> {
        val cid = com.example.garmento.core.util.UserSession.currentCompanyId ?: return kotlinx.coroutines.flow.flowOf(emptyList())
        return orderDao.getAllOrdersWithCustomers(cid).map { list ->
            list.filter { it.delivery_status == "delivered" && it.payment_status != "paid" }
        }
    }

    fun markPaid(orderId: String) {
        viewModelScope.launch {
            try {
                orderRepository.markOrderPaid(orderId)
            } catch (_: Exception) { }
        }
    }

    fun generateOutstandingPdf(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, error = null) }
            try {
                withContext(Dispatchers.IO) {
                    val cid = com.example.garmento.core.util.UserSession.currentCompanyId ?: return@withContext
                    val orders = orderDao.getAllOrdersWithCustomers(cid).first().filter { o ->
                        o.delivery_status == "delivered" && o.payment_status != "paid"
                    }
                    val lines = mutableListOf<String>()
                    lines.add("Order# | Customer | Date | Pending")
                    orders.forEach { o ->
                        lines.add("${o.order_number} | ${o.customer_name ?: ""} | ${o.order_date} | Rs.${String.format("%.2f", o.total_amount - o.paid_amount)}")
                    }
                    if (orders.isEmpty()) lines.add("No outstanding payments.")
                    val file = PdfReportGenerator.createPdf(context, "outstanding_report.pdf", "Outstanding Payment Report", lines)
                    PdfReportGenerator.sharePdf(context, file)
                }
                _uiState.update { it.copy(isExporting = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isExporting = false, error = e.message ?: "Failed to generate PDF") }
            }
        }
    }

    fun generateDeliveryReportPdf(context: Context, date: LocalDate) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, error = null) }
            try {
                withContext(Dispatchers.IO) {
                    val cid = com.example.garmento.core.util.UserSession.currentCompanyId ?: return@withContext
                    val rows = orderDao.getDeliveriesForDate(date.toString(), cid).first()  // Flow
                    val lines = mutableListOf<String>()
                    lines.add("Order# | Customer | Mobile | Items | Status")
                    rows.forEach { d ->
                        lines.add("${d.order_number} | ${d.customer_name} | ${d.customer_phone} | ${d.clothes_count} | ${d.delivery_status}")
                    }
                    if (rows.isEmpty()) lines.add("No deliveries scheduled for this date.")
                    val file = PdfReportGenerator.createPdf(context, "delivery_report.pdf", "Delivery Report ($date)", lines)
                    PdfReportGenerator.sharePdf(context, file)
                }
                _uiState.update { it.copy(isExporting = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isExporting = false, error = e.message ?: "Failed to generate PDF") }
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
                    DateParsing.parseLocalDate(transaction.transaction_date.substring(0, 10)) to transaction.amount
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