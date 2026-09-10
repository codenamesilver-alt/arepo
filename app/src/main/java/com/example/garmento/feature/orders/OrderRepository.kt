package com.example.garmento.feature.orders

import com.example.garmento.core.data.local.dao.OrderDao
import com.example.garmento.core.data.local.entity.OrderEntity
import com.example.garmento.core.domain.model.Order
import com.example.garmento.core.domain.model.Subscription
import com.example.garmento.feature.customers.SubscriptionRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

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
class OrderRepository @Inject constructor(
    private val orderDao: OrderDao,
    private val subscriptionRepository: SubscriptionRepository,
    private val supabase: SupabaseClient
) {
    suspend fun placeOrder(
        subscription: Subscription,
        orderNumber: String,
        clothesCount: Int
    ): Order {
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
            gracePeriod = false
        )

        orderDao.insertOrder(order.toEntity())

        subscriptionRepository.updateSubscriptionQuota(
            subscription.copy(clothesUsed = subscription.clothesUsed + clothesCount)
        )

        try {
            supabase.from("orders").insert(
                SupabaseOrder(
                    id = order.id.toString(),
                    subscription_id = subscription.id.toString(),
                    order_number = orderNumber,
                    clothes_count = clothesCount,
                    clothes_from_quota = clothesFromQuota,
                    clothes_overage = clothesOverage,
                    order_date = LocalDateTime.now().toString(),
                    is_voided = false,
                    void_reason = null,
                    grace_period = false
                )
            )
        } catch (_: Exception) { }

        return order
    }

    suspend fun updateOrder(order: Order, newClothesCount: Int, reason: String) {
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
        orderDao.updateOrder(updatedOrder.toEntity())

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
        val subscription = subscriptionRepository.getSubscriptionById(order.subscriptionId.toString())
            ?: return

        val updatedOrder = order.copy(
            isVoided = true,
            voidReason = reason
        )
        orderDao.updateOrder(updatedOrder.toEntity())

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
        return orderDao.getOrderById(id)?.toDomain()
    }

    suspend fun getOrdersForDate(datePrefix: String): Int {
        return orderDao.getOrdersCountForDate(datePrefix)
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
        gracePeriod = grace_period
    )

    private fun Order.toEntity() = OrderEntity(
        id = id.toString(),
        subscription_id = subscriptionId.toString(),
        order_number = orderNumber,
        clothes_count = clothesCount,
        clothes_from_quota = clothesFromQuota,
        clothes_overage = clothesOverage,
        order_date = orderDate.toString(),
        is_voided = isVoided,
        void_reason = voidReason,
        grace_period = gracePeriod
    )
}
