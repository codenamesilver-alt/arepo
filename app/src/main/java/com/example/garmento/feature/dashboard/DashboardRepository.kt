package com.example.garmento.feature.dashboard

import com.example.garmento.core.data.local.dao.CustomerDao
import com.example.garmento.core.data.local.dao.OrderDao
import com.example.garmento.core.data.local.dao.SubscriptionDao
import com.example.garmento.core.data.local.dao.TransactionDao
import com.example.garmento.core.data.local.entity.CustomerEntity
import com.example.garmento.core.data.local.entity.SubscriptionEntity
import com.example.garmento.core.data.local.entity.TransactionEntity
import com.example.garmento.core.data.local.entity.OrderEntity
import com.example.garmento.core.domain.model.DeliveryStatus
import com.example.garmento.core.domain.model.Subscription
import com.example.garmento.core.domain.model.SubscriptionStatus
import com.example.garmento.core.domain.model.Transaction
import com.example.garmento.core.domain.model.TransactionType
import com.example.garmento.core.util.Constants
import com.example.garmento.core.util.UserSession
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class ExpiringSoonRow(
    val customerName: String,
    val daysLeft: Long,
    val clothesLeft: Int
)

data class UpcomingDeliveryRow(
    val customerName: String,
    val orderNumber: String,
    val deliveryDate: String,
    val deliveryStatus: DeliveryStatus,
    val clothesCount: Int
)

@Serializable
private data class SupabaseSubscription(
    val id: String,
    val company_id: String? = null,
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
    val company_id: String? = null,
    val customer_id: String,
    val subscription_id: String,
    val amount: Double,
    val type: String,
    val payment_method: String,
    val transaction_date: String
)

@Serializable
private data class SupabaseCustomer(
    val id: String,
    val company_id: String? = null,
    val name: String,
    val phone: String,
    val address: String?,
    val notes: String?,
    val public_code: String,
    val created_at: String
)

@Serializable
private data class SupabaseOrder(
    val id: String,
    val company_id: String? = null,
    val subscription_id: String,
    val order_number: String,
    val clothes_count: Int,
    val clothes_from_quota: Int,
    val clothes_overage: Int,
    val order_date: String,
    val is_voided: Boolean,
    val void_reason: String?,
    val grace_period: Boolean,
    val delivery_date: String? = null,
    val delivery_status: String = "pending",
    val delivery_notes: String? = null
)

data class RecentTransactionRow(
    val customerName: String,
    val amount: Double,
    val type: TransactionType
)

@Singleton
class DashboardRepository @Inject constructor(
    private val subscriptionDao: SubscriptionDao,
    private val orderDao: OrderDao,
    private val transactionDao: TransactionDao,
    private val customerDao: CustomerDao,
    private val supabase: SupabaseClient
) {
    private fun activeCompanyId(): String? = UserSession.currentCompanyId?.takeIf { it.isNotBlank() }

    suspend fun getActiveSubscriptionCount(): Int {
        val cid = activeCompanyId() ?: return 0
        return subscriptionDao.getActiveSubscriptionCount(LocalDate.now().toString(), cid)
    }

    suspend fun getExpiringSoon(daysAhead: Int = 7): List<ExpiringSoonRow> {
        val cid = activeCompanyId() ?: return emptyList()
        val targetDate = LocalDate.now().plusDays(daysAhead.toLong()).toString()
        return subscriptionDao.getExpiringSubscriptions(targetDate, cid)
            .map { subscription ->
                val customer = customerDao.getCustomerById(subscription.customer_id, cid)
                ExpiringSoonRow(
                    customerName = customer?.name ?: "Unknown",
                    daysLeft = subscription.toDomain().daysRemaining,
                    clothesLeft = subscription.toDomain().remainingClothes
                )
            }
    }

    suspend fun getRevenueLast30Days(): Double {
        val cid = activeCompanyId() ?: return 0.0
        val since = LocalDate.now().minusDays(30).toString()
        val subscriptionRevenue = transactionDao.getRevenueSince(since, cid) ?: 0.0
        val overageItems = orderDao.getOverageItemsSince(since, cid) ?: 0
        return subscriptionRevenue + (overageItems * Constants.OVERAGE_RATE_PER_ITEM)
    }

    suspend fun getRevenueToday(): Double {
        val cid = activeCompanyId() ?: return 0.0
        val today = LocalDate.now().toString()
        val subscriptionRevenue = transactionDao.getRevenueForDate(today, cid) ?: 0.0
        val overageItems = orderDao.getOverageItemsForDate(today, cid) ?: 0
        return subscriptionRevenue + (overageItems * Constants.OVERAGE_RATE_PER_ITEM)
    }

    suspend fun getOrdersTodayCount(): Int {
        val cid = activeCompanyId() ?: return 0
        val todayPrefix = LocalDate.now().toString()
        return orderDao.getOrdersCountForDate(todayPrefix, cid)
    }

    fun getRecentTransactions(limit: Int = 10): Flow<List<RecentTransactionRow>> {
        val cid = activeCompanyId() ?: return flowOf(emptyList())
        return transactionDao.getRecentTransactions(limit, cid)
            .map { entities ->
                entities.map { tx ->
                    val customer = customerDao.getCustomerById(tx.customer_id, cid)
                    RecentTransactionRow(
                        customerName = customer?.name ?: "Unknown",
                        amount = tx.amount,
                        type = TransactionType.fromValue(tx.type)
                    )
                }
            }
    }

    fun getUpcomingDeliveries(limit: Int = 15): Flow<List<UpcomingDeliveryRow>> {
        val cid = activeCompanyId() ?: return flowOf(emptyList())
        return orderDao.getUpcomingDeliveries(
            today = LocalDate.now().toString(),
            limit = limit,
            companyId = cid
        ).map { rows ->
            rows.map {
                UpcomingDeliveryRow(
                    customerName = it.customer_name,
                    orderNumber = it.order_number,
                    deliveryDate = it.delivery_date ?: "",
                    deliveryStatus = DeliveryStatus.fromValue(it.delivery_status),
                    clothesCount = it.clothes_count
                )
            }
        }
    }

    fun getRecentOrders(limit: Int = 5): Flow<List<OrderEntity>> {
        val cid = activeCompanyId() ?: return flowOf(emptyList())
        return orderDao.getRecentOrders(limit, cid)
    }

    suspend fun syncFromRemote() {
        val cid = activeCompanyId() ?: return
        try {
            val remoteCustomers = supabase.from("customers")
                .select()
                .decodeList<SupabaseCustomer>()
            customerDao.insertCustomers(remoteCustomers.map {
                CustomerEntity(
                    id = it.id,
                    companyId = it.company_id ?: cid,
                    name = it.name,
                    phone = it.phone,
                    address = it.address ?: "",
                    notes = it.notes ?: "",
                    public_code = it.public_code,
                    created_at = it.created_at
                )
            })

            val remoteSubs = supabase.from("subscriptions")
                .select()
                .decodeList<SupabaseSubscription>()
            subscriptionDao.insertSubscriptions(remoteSubs.map {
                com.example.garmento.core.data.local.entity.SubscriptionEntity(
                    id = it.id,
                    companyId = it.company_id ?: cid,
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
                    companyId = it.company_id ?: cid,
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
                    companyId = it.company_id ?: cid,
                    subscription_id = it.subscription_id,
                    order_number = it.order_number,
                    clothes_count = it.clothes_count,
                    clothes_from_quota = it.clothes_from_quota,
                    clothes_overage = it.clothes_overage,
                    order_date = it.order_date,
                    is_voided = it.is_voided,
                    void_reason = it.void_reason,
                    grace_period = it.grace_period,
                    delivery_date = it.delivery_date,
                    delivery_status = it.delivery_status,
                    delivery_notes = it.delivery_notes
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