package com.example.garmento.feature.dashboard

import com.example.garmento.core.data.local.dao.OrderDao
import com.example.garmento.core.data.local.dao.SubscriptionDao
import com.example.garmento.core.data.local.dao.TransactionDao
import com.example.garmento.core.data.local.entity.SubscriptionEntity
import com.example.garmento.core.data.local.entity.TransactionEntity
import com.example.garmento.core.domain.model.Subscription
import com.example.garmento.core.domain.model.SubscriptionStatus
import com.example.garmento.core.domain.model.Transaction
import com.example.garmento.core.domain.model.TransactionType
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class SupabaseSubscription(
    val id: String,
    val customer_id: String,
    val plan_id: String,
    val start_date: String,
    val end_date: String,
    val clothes_quota: Int,
    val clothes_used: Int,
    val status: String,
    val renewed_from_id: String?,
    val created_at: String
)

@Serializable
private data class SupabaseTransaction(
    val id: String,
    val customer_id: String,
    val subscription_id: String,
    val amount: Double,
    val type: String,
    val payment_method: String,
    val transaction_date: String
)

@Serializable
private data class SupabaseOrder(
    val id: String,
    val subscription_id: String,
    val order_number: String,
    val clothes_count: Int,
    val clothes_from_quota: Int,
    val clothes_overage: Int,
    val order_date: String,
    val is_voided: Boolean,
    val void_reason: String?,
    val grace_period: Boolean
)

@Singleton
class DashboardRepository @Inject constructor(
    private val subscriptionDao: SubscriptionDao,
    private val orderDao: OrderDao,
    private val transactionDao: TransactionDao,
    private val supabase: SupabaseClient
) {
    suspend fun getActiveSubscriptionCount(): Int {
        return subscriptionDao.getActiveSubscriptionCount()
    }

    suspend fun getExpiringSoon(daysAhead: Int = 7): List<Subscription> {
        val targetDate = LocalDate.now().plusDays(daysAhead.toLong()).toString()
        return subscriptionDao.getExpiringSubscriptions(targetDate)
            .map { it.toDomain() }
    }

    suspend fun getRevenueThisMonth(): Double {
        val monthPrefix = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
        return transactionDao.getRevenueForMonth(monthPrefix) ?: 0.0
    }

    suspend fun getOrdersTodayCount(): Int {
        val todayPrefix = LocalDate.now().toString()
        return orderDao.getOrdersCountForDate(todayPrefix)
    }

    fun getRecentTransactions(limit: Int = 10) = transactionDao.getRecentTransactions(limit)
        .map { entities -> entities.map { it.toDomain() } }

    fun getRecentOrders(limit: Int = 5) = orderDao.getRecentOrders(limit)

    suspend fun syncFromRemote() {
        try {
            val remoteSubs = supabase.from("subscriptions")
                .select()
                .decodeList<SupabaseSubscription>()
            subscriptionDao.insertSubscriptions(remoteSubs.map {
                com.example.garmento.core.data.local.entity.SubscriptionEntity(
                    id = it.id,
                    customer_id = it.customer_id,
                    plan_id = it.plan_id,
                    start_date = it.start_date,
                    end_date = it.end_date,
                    clothes_quota = it.clothes_quota,
                    clothes_used = it.clothes_used,
                    status = it.status,
                    renewed_from_id = it.renewed_from_id,
                    created_at = it.created_at
                )
            })

            val remoteTrans = supabase.from("transactions")
                .select()
                .decodeList<SupabaseTransaction>()
            transactionDao.insertTransactions(remoteTrans.map {
                com.example.garmento.core.data.local.entity.TransactionEntity(
                    id = it.id,
                    customer_id = it.customer_id,
                    subscription_id = it.subscription_id,
                    amount = it.amount,
                    type = it.type,
                    payment_method = it.payment_method,
                    transaction_date = it.transaction_date
                )
            })

            val remoteOrders = supabase.from("orders")
                .select()
                .decodeList<SupabaseOrder>()
            orderDao.insertOrders(remoteOrders.map {
                com.example.garmento.core.data.local.entity.OrderEntity(
                    id = it.id,
                    subscription_id = it.subscription_id,
                    order_number = it.order_number,
                    clothes_count = it.clothes_count,
                    clothes_from_quota = it.clothes_from_quota,
                    clothes_overage = it.clothes_overage,
                    order_date = it.order_date,
                    is_voided = it.is_voided,
                    void_reason = it.void_reason,
                    grace_period = it.grace_period
                )
            })
        } catch (_: Exception) { }
    }

    private fun SubscriptionEntity.toDomain() = Subscription(
        id = UUID.fromString(id),
        customerId = UUID.fromString(customer_id),
        planId = UUID.fromString(plan_id),
        startDate = LocalDate.parse(start_date),
        endDate = LocalDate.parse(end_date),
        clothesQuota = clothes_quota,
        clothesUsed = clothes_used,
        status = SubscriptionStatus.fromValue(status),
        renewedFromId = renewed_from_id?.let { UUID.fromString(it) },
        createdAt = LocalDateTime.parse(created_at)
    )

    private fun TransactionEntity.toDomain() = Transaction(
        id = UUID.fromString(id),
        customerId = UUID.fromString(customer_id),
        subscriptionId = UUID.fromString(subscription_id),
        amount = amount,
        type = TransactionType.fromValue(type),
        paymentMethod = payment_method,
        transactionDate = LocalDateTime.parse(transaction_date)
    )
}
