package com.example.garmento.feature.customers

import com.example.garmento.core.data.local.dao.SubscriptionDao
import com.example.garmento.core.data.local.dao.TransactionDao
import com.example.garmento.core.data.local.entity.SubscriptionEntity
import com.example.garmento.core.data.local.entity.TransactionEntity
import com.example.garmento.core.domain.model.Subscription
import com.example.garmento.core.domain.model.SubscriptionStatus
import com.example.garmento.core.domain.model.SubscriptionPlan
import com.example.garmento.core.domain.model.Transaction
import com.example.garmento.core.domain.model.TransactionType
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalDateTime
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

@Singleton
class SubscriptionRepository @Inject constructor(
    private val subscriptionDao: SubscriptionDao,
    private val transactionDao: TransactionDao,
    private val supabase: SupabaseClient
) {
    suspend fun createSubscription(
        customerId: UUID,
        plan: SubscriptionPlan,
        paymentMethod: String
    ): Subscription {
        val now = LocalDate.now()
        val endDate = now.plusDays(plan.durationDays.toLong())

        val subscription = Subscription(
            customerId = customerId,
            planId = plan.id,
            startDate = now,
            endDate = endDate,
            clothesQuota = plan.totalClothes,
            clothesUsed = 0,
            status = SubscriptionStatus.ACTIVE
        )

        // Save to Room
        subscriptionDao.insertSubscription(subscription.toEntity())

        // Save to Supabase
        try {
            supabase.from("subscriptions").insert(
                SupabaseSubscription(
                    id = subscription.id.toString(),
                    customer_id = customerId.toString(),
                    plan_id = plan.id.toString(),
                    start_date = now.toString(),
                    end_date = endDate.toString(),
                    clothes_quota = plan.totalClothes,
                    clothes_used = 0,
                    status = "active",
                    renewed_from_id = null,
                    created_at = LocalDateTime.now().toString()
                )
            )
        } catch (_: Exception) { }

        // Create transaction
        val transaction = Transaction(
            customerId = customerId,
            subscriptionId = subscription.id,
            amount = plan.amount,
            type = TransactionType.NEW,
            paymentMethod = paymentMethod
        )
        transactionDao.insertTransaction(transaction.toEntity())

        try {
            supabase.from("transactions").insert(
                SupabaseTransaction(
                    id = transaction.id.toString(),
                    customer_id = customerId.toString(),
                    subscription_id = subscription.id.toString(),
                    amount = plan.amount,
                    type = "new",
                    payment_method = paymentMethod,
                    transaction_date = LocalDateTime.now().toString()
                )
            )
        } catch (_: Exception) { }

        return subscription
    }

    suspend fun renewSubscription(
        oldSubscriptionId: UUID,
        customerId: UUID,
        plan: SubscriptionPlan,
        paymentMethod: String
    ): Subscription {
        // Mark old as renewed
        val oldSub = subscriptionDao.getSubscriptionById(oldSubscriptionId.toString())
        if (oldSub != null) {
            subscriptionDao.updateSubscription(
                oldSub.copy(status = "renewed")
            )
        }

        return createSubscription(customerId, plan, paymentMethod)
    }

    suspend fun updateSubscriptionQuota(subscription: Subscription) {
        subscriptionDao.updateSubscription(subscription.toEntity())
        try {
            supabase.from("subscriptions").update(
                mapOf(
                    "clothes_used" to subscription.clothesUsed,
                    "status" to subscription.status.value
                )
            ) {
                filter { eq("id", subscription.id.toString()) }
            }
        } catch (_: Exception) { }
    }

    suspend fun getSubscriptionById(id: String): Subscription? {
        return subscriptionDao.getSubscriptionById(id)?.toDomain()
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

    private fun Subscription.toEntity() = SubscriptionEntity(
        id = id.toString(),
        customer_id = customerId.toString(),
        plan_id = planId.toString(),
        start_date = startDate.toString(),
        end_date = endDate.toString(),
        clothes_quota = clothesQuota,
        clothes_used = clothesUsed,
        status = status.value,
        renewed_from_id = renewedFromId?.toString(),
        created_at = createdAt.toString()
    )

    private fun Transaction.toEntity() = TransactionEntity(
        id = id.toString(),
        customer_id = customerId.toString(),
        subscription_id = subscriptionId.toString(),
        amount = amount,
        type = type.value,
        payment_method = paymentMethod,
        transaction_date = transactionDate.toString()
    )
}
