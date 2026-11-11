package com.example.garmento.feature.reports

import com.example.garmento.core.util.DateParsing

import com.example.garmento.core.data.local.dao.CustomerDao
import com.example.garmento.core.data.local.dao.OrderDao
import com.example.garmento.core.data.local.dao.PlanDao
import com.example.garmento.core.data.local.dao.SubscriptionDao
import com.example.garmento.core.data.local.dao.TransactionDao
import com.example.garmento.core.data.local.entity.OrderEntity
import com.example.garmento.core.data.local.entity.TransactionEntity
import com.example.garmento.core.util.UserSession
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

data class TransactionSummary(
    val totalRevenue: Double = 0.0,
    val transactionCount: Int = 0,
    val orderCount: Int = 0,
    val averageClothesPerOrder: Double = 0.0
)

@Singleton
class ReportsRepository @Inject constructor(
    private val transactionDao: TransactionDao,
    private val orderDao: OrderDao,
    private val subscriptionDao: SubscriptionDao,
    private val customerDao: CustomerDao,
    private val planDao: PlanDao
) {
    private fun activeCompanyId(): String? = UserSession.currentCompanyId?.takeIf { it.isNotBlank() }

    suspend fun getTransactionsBetween(startDate: LocalDate, endDate: LocalDate): List<TransactionEntity> {
        val cid = activeCompanyId() ?: return emptyList()
        return transactionDao.getAllTransactions(cid).first()
            .filter { it.dateIn(startDate, endDate) }
    }

    suspend fun getRevenue(startDate: LocalDate, endDate: LocalDate): Double {
        return getTransactionsBetween(startDate, endDate).sumOf { it.amount }
    }

    suspend fun getTransactionSummary(startDate: LocalDate, endDate: LocalDate): TransactionSummary {
        val cid = activeCompanyId() ?: return TransactionSummary()
        val transactions = transactionDao.getAllTransactions(cid).first()
            .filter { it.dateIn(startDate, endDate) }
        val orders = orderDao.getActiveOrders(cid).first()
            .filter { it.dateIn(startDate, endDate) }
        return TransactionSummary(
            totalRevenue = transactions.sumOf { it.amount },
            transactionCount = transactions.size,
            orderCount = orders.size,
            averageClothesPerOrder = if (orders.isEmpty()) 0.0 else
                orders.sumOf { it.clothes_count.toDouble() } / orders.size
        )
    }

    suspend fun getRevenuePerPlan(startDate: LocalDate, endDate: LocalDate): List<Pair<String, Double>> {
        val cid = activeCompanyId() ?: return emptyList()
        val transactions = transactionDao.getAllTransactions(cid).first()
            .filter { it.dateIn(startDate, endDate) }
        if (transactions.isEmpty()) return emptyList()

        val subscriptions = subscriptionDao.getAllSubscriptions(cid)
            .associateBy { it.id }
        val planIds = subscriptions.values.mapNotNull { it.plan_id }.toSet()
        val plans = planDao.getAllPlans(cid).first()
            .filter { it.id in planIds }
            .associateBy { it.id }

        return transactions
            .groupBy { subscriptions[it.subscription_id]?.plan_id ?: "" }
            .map { (planId, list) ->
                (plans[planId]?.name ?: "Unknown") to list.sumOf { it.amount }
            }
            .sortedByDescending { it.second }
    }

    suspend fun getOrdersPerCustomer(startDate: LocalDate, endDate: LocalDate): List<Pair<String, Int>> {
        val cid = activeCompanyId() ?: return emptyList()
        val orders = orderDao.getActiveOrders(cid).first()
            .filter { it.dateIn(startDate, endDate) }
        if (orders.isEmpty()) return emptyList()

        val subscriptions = subscriptionDao.getAllSubscriptions(cid)
            .associateBy { it.id }
        val customerIds = subscriptions.values.map { it.customer_id }.toSet()
        val customers = customerDao.getAllCustomers(cid).first()
            .filter { it.id in customerIds }
            .associateBy { it.id }

        return orders
            .groupBy { subscriptions[it.subscription_id]?.customer_id ?: "" }
            .map { (customerId, list) ->
                (customers[customerId]?.name ?: "Unknown") to list.sumOf { it.clothes_count }
            }
            .sortedByDescending { it.second }
    }

    private fun TransactionEntity.dateIn(startDate: LocalDate, endDate: LocalDate): Boolean {
        return try {
            parseDate(transaction_date) in startDate..endDate
        } catch (_: Exception) {
            false
        }
    }

    private fun OrderEntity.dateIn(startDate: LocalDate, endDate: LocalDate): Boolean {
        return try {
            parseDate(order_date) in startDate..endDate
        } catch (_: Exception) {
            false
        }
    }

    suspend fun getDailySalesLast10Days(): List<Pair<String, Double>> {
        val cid = activeCompanyId() ?: return emptyList()
        val orders = orderDao.getAllOrdersWithCustomers(cid).first()  // Flow -> first
        // Actually getAllOrdersWithCustomers returns Flow; .first() ok
        val today = LocalDate.now()
        val result = mutableListOf<Pair<String, Double>>()
        for (i in 9 downTo 0) {
            val date = today.minusDays(i.toLong())
            val total = orders.filter { o ->
                try { DateParsing.parseLocalDateTime(o.order_date).toLocalDate() == date } catch (_: Exception) { false }
            }.sumOf { it.total_amount }
            result.add(date.dayOfMonth.toString() to total)
        }
        return result
    }

    private fun parseDate(value: String): LocalDate =
        DateParsing.parseLocalDateTime(value).toLocalDate()
}