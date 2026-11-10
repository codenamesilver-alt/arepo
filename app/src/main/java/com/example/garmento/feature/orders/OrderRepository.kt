package com.example.garmento.feature.orders

import com.example.garmento.core.data.local.dao.OrderDao
import com.example.garmento.core.data.local.dao.TransactionDao
import com.example.garmento.core.data.local.entity.OrderEntity
import com.example.garmento.core.data.local.entity.TransactionEntity
import com.example.garmento.core.domain.model.DeliveryStatus
import com.example.garmento.core.domain.model.Order
import com.example.garmento.core.domain.model.Subscription
import com.example.garmento.core.domain.model.Transaction
import com.example.garmento.core.domain.model.TransactionType
import com.example.garmento.core.util.Constants
import com.example.garmento.core.util.UserSession
import com.example.garmento.feature.customers.SubscriptionRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

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

@Singleton
class OrderRepository @Inject constructor(
    private val orderDao: OrderDao,
    private val subscriptionRepository: SubscriptionRepository,
    private val transactionDao: TransactionDao,
    private val supabase: SupabaseClient
) {
    private fun activeCompanyId(): String? = UserSession.currentCompanyId?.takeIf { it.isNotBlank() }

    suspend fun placeOrder(
        subscription: Subscription,
        orderNumber: String,
        clothesCount: Int,
        paymentMethod: String = "cash",
        deliveryDate: LocalDate? = null
    ): Order {
        val cid = activeCompanyId()
        val remaining = subscription.clothesQuota - subscription.clothesUsed
        val clothesFromQuota = minOf(clothesCount, maxOf(remaining, 0))
        val clothesOverage = clothesCount - clothesFromQuota

        val order = Order(
            subscriptionId = subscription.id,
            orderNumber = orderNumber,
            clothesCount = clothesCount,
            clothesFromQuota = clothesFromQuota,
            clothesOverage = clothesOverage,
            orderDate = LocalDateTime.now(),
            isVoided = false,
            gracePeriod = false,
            deliveryDate = deliveryDate
        )

        orderDao.insertOrder(order.toEntity(cid))

        subscriptionRepository.updateSubscriptionQuota(
            subscription.copy(clothesUsed = subscription.clothesUsed + clothesCount)
        )

        try {
            supabase.from("orders").insert(
                SupabaseOrder(
                    id = order.id.toString(),
                    company_id = cid,
                    subscription_id = subscription.id.toString(),
                    order_number = orderNumber,
                    clothes_count = clothesCount,
                    clothes_from_quota = clothesFromQuota,
                    clothes_overage = clothesOverage,
                    order_date = LocalDateTime.now().toString(),
                    is_voided = false,
                    void_reason = null,
                    grace_period = false,
                    delivery_date = deliveryDate?.toString(),
                    delivery_status = "pending"
                )
            )
        } catch (_: Exception) { }

        if (clothesOverage > 0) {
            val perItemRate = Constants.OVERAGE_RATE_PER_ITEM
            val overageAmount = clothesOverage * perItemRate
            if (overageAmount > 0) {
                val transaction = Transaction(
                    customerId = subscription.customerId,
                    subscriptionId = subscription.id,
                    amount = overageAmount,
                    type = TransactionType.OVERAGE,
                    paymentMethod = paymentMethod
                )
                transactionDao.insertTransaction(transaction.toEntity(cid))
                try {
                    supabase.from("transactions").insert(
                        SupabaseTransaction(
                            id = transaction.id.toString(),
                            company_id = cid,
                            customer_id = subscription.customerId.toString(),
                            subscription_id = subscription.id.toString(),
                            amount = overageAmount,
                            type = TransactionType.OVERAGE.value,
                            payment_method = paymentMethod,
                            transaction_date = transaction.transactionDate.toString()
                        )
                    )
                } catch (_: Exception) { }
            }
        }

        return order
    }

    suspend fun perItemRateFor(subscription: Subscription): Double =
        Constants.OVERAGE_RATE_PER_ITEM

    suspend fun updateOrder(order: Order, newClothesCount: Int, reason: String) {
        val cid = activeCompanyId()
        val subscription = subscriptionRepository.getSubscriptionById(order.subscriptionId.toString())
            ?: return

        val reversedUsed = subscription.clothesUsed - order.clothesCount
        val newUsed = reversedUsed + newClothesCount

        val remaining = subscription.clothesQuota - reversedUsed
        val clothesFromQuota = minOf(newClothesCount, maxOf(remaining, 0))
        val clothesOverage = newClothesCount - clothesFromQuota

        val updatedOrder = order.copy(
            clothesCount = newClothesCount,
            clothesFromQuota = clothesFromQuota,
            clothesOverage = clothesOverage
        )
        orderDao.updateOrder(updatedOrder.toEntity(cid))

        subscriptionRepository.updateSubscriptionQuota(
            subscription.copy(clothesUsed = newUsed)
        )

        try {
            supabase.from("orders").update(
                mapOf(
                    "clothes_count" to newClothesCount,
                    "clothes_from_quota" to clothesFromQuota,
                    "clothes_overage" to clothesOverage
                )
            ) {
                filter { eq("id", order.id.toString()) }
            }
        } catch (_: Exception) { }
    }

    suspend fun voidOrder(order: Order, reason: String) {
        val cid = activeCompanyId()
        val subscription = subscriptionRepository.getSubscriptionById(order.subscriptionId.toString())
            ?: return

        val updatedOrder = order.copy(
            isVoided = true,
            voidReason = reason
        )
        orderDao.updateOrder(updatedOrder.toEntity(cid))

        subscriptionRepository.updateSubscriptionQuota(
            subscription.copy(clothesUsed = maxOf(subscription.clothesUsed - order.clothesCount, 0))
        )

        try {
            supabase.from("orders").update(
                mapOf(
                    "is_voided" to true,
                    "void_reason" to reason
                )
            ) {
                filter { eq("id", order.id.toString()) }
            }
        } catch (_: Exception) { }
    }

    suspend fun getOrderById(id: String): Order? {
        val cid = activeCompanyId() ?: return null
        return orderDao.getOrderById(id, cid)?.toDomain()
    }

    suspend fun markDelivered(order: Order, itemsLeft: Int?, note: String?) {
        val cid = activeCompanyId()
        val status = if (itemsLeft != null && itemsLeft > 0) DeliveryStatus.PARTIAL else DeliveryStatus.DELIVERED
        val updated = order.copy(
            deliveryStatus = status,
            deliveryNotes = note ?: if (status == DeliveryStatus.PARTIAL) "$itemsLeft items left for delivery" else null
        )
        orderDao.updateOrder(updated.toEntity(cid))
        try {
            supabase.from("orders").update(
                mapOf(
                    "delivery_status" to status.value,
                    "delivery_notes" to (updated.deliveryNotes ?: "")
                )
            ) {
                filter { eq("id", order.id.toString()) }
            }
        } catch (_: Exception) { }
    }

    suspend fun getOrdersForDate(datePrefix: String): Int {
        val cid = activeCompanyId() ?: return 0
        return orderDao.getOrdersCountForDate(datePrefix, cid)
    }

    private fun OrderEntity.toDomain() = Order(
        id = UUID.fromString(id),
        subscriptionId = UUID.fromString(subscription_id),
        orderNumber = order_number,
        clothesCount = clothes_count,
        clothesFromQuota = clothes_from_quota,
        clothesOverage = clothes_overage,
        orderDate = LocalDateTime.parse(order_date),
        isVoided = is_voided,
        voidReason = void_reason,
        gracePeriod = grace_period,
        deliveryDate = delivery_date?.let { LocalDate.parse(it) },
        deliveryStatus = DeliveryStatus.fromValue(delivery_status),
        deliveryNotes = delivery_notes
    )

    private fun Order.toEntity(cid: String?) = OrderEntity(
        id = id.toString(),
        companyId = cid ?: "",
        subscription_id = subscriptionId.toString(),
        order_number = orderNumber,
        clothes_count = clothesCount,
        clothes_from_quota = clothesFromQuota,
        clothes_overage = clothesOverage,
        order_date = orderDate.toString(),
        is_voided = isVoided,
        void_reason = voidReason,
        grace_period = gracePeriod,
        delivery_date = deliveryDate?.toString(),
        delivery_status = deliveryStatus.value,
        delivery_notes = deliveryNotes
    )

    private fun Transaction.toEntity(cid: String?) = TransactionEntity(
        id = id.toString(),
        companyId = cid ?: "",
        customer_id = customerId.toString(),
        subscription_id = subscriptionId.toString(),
        amount = amount,
        type = type.value,
        payment_method = paymentMethod,
        transaction_date = transactionDate.toString()
    )
}