package com.example.garmento.feature.customers

import com.example.garmento.core.util.DateParsing

import com.example.garmento.core.data.local.dao.SubscriptionDao
import com.example.garmento.core.data.local.dao.TransactionDao
import com.example.garmento.core.data.local.entity.SubscriptionEntity
import com.example.garmento.core.data.local.entity.TransactionEntity
import com.example.garmento.core.domain.model.Subscription
import com.example.garmento.core.domain.model.SubscriptionStatus
import com.example.garmento.core.domain.model.SubscriptionPlan
import com.example.garmento.core.domain.model.Transaction
import com.example.garmento.core.domain.model.TransactionType
import com.example.garmento.core.util.UserSession
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

@Singleton
class SubscriptionRepository @Inject constructor(
    private val subscriptionDao: SubscriptionDao,
    private val transactionDao: TransactionDao,
    private val supabase: SupabaseClient
) {
    private fun activeCompanyId(): String? = UserSession.currentCompanyId?.takeIf { it.isNotBlank() }

    suspend fun createSubscription(
        customerId: UUID,
        plan: SubscriptionPlan,
        paymentMethod: String
    ): Subscription {
        val cid = activeCompanyId()
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
        subscriptionDao.insertSubscription(subscription.toEntity(cid))

        // Save to Supabase
        try {
            supabase.from("subscriptions").insert(
                SupabaseSubscription(
                    id = subscription.id.toString(),
                    company_id = cid,
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
        transactionDao.insertTransaction(transaction.toEntity(cid))

        try {
            supabase.from("transactions").insert(
                SupabaseTransaction(
                    id = transaction.id.toString(),
                    company_id = cid,
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
        val cid = activeCompanyId()
        // Mark old as renewed
        val oldSub = subscriptionDao.getSubscriptionById(oldSubscriptionId.toString(), cid ?: "")
        if (oldSub != null) {
            val renewedSub = oldSub.copy(status = "renewed")
            subscriptionDao.updateSubscription(renewedSub)
            try {
                supabase.from("subscriptions").update(
                    mapOf("status" to "renewed")
                ) {
                    filter { eq("id", renewedSub.id) }
                }
            } catch (_: Exception) { }
        }

        return createSubscription(customerId, plan, paymentMethod)
    }

    suspend fun updateSubscriptionQuota(subscription: Subscription) {
        val cid = activeCompanyId()
        subscriptionDao.updateSubscription(subscription.toEntity(cid))
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

    suspend fun revokeSubscription(customerId: String) {
        val cid = activeCompanyId() ?: return
        val active = subscriptionDao.getActiveSubscriptionForCustomer(customerId, cid) ?: return
        subscriptionDao.deleteSubscription(active.id, cid)
        try {
            supabase.from("subscriptions").delete {
                filter { eq("id", active.id) }
            }
        } catch (_: Exception) {
            // Offline
        }
    }

    suspend fun syncFromRemote() {
        val cid = activeCompanyId() ?: return
        try {
            val remote = supabase.from("subscriptions")
                .select()
                .decodeList<SupabaseSubscription>()
            remote.forEach { s ->
                val entity = SubscriptionEntity(
                    id = s.id,
                    companyId = s.company_id ?: cid,
                    customer_id = s.customer_id,
                    plan_id = s.plan_id,
                    start_date = s.start_date,
                    end_date = s.end_date,
                    clothes_quota = s.clothes_quota,
                    clothes_used = s.clothes_used,
                    status = s.status,
                    renewed_from_id = s.renewed_from_id,
                    created_at = s.created_at
                )
                subscriptionDao.insertSubscription(entity)
            }
        } catch (_: Exception) {
            // Offline
        }
    }

    suspend fun updateSubscriptionFields(subscription: Subscription) {
        val cid = activeCompanyId()
        subscriptionDao.updateSubscription(subscription.toEntity(cid))
        try {
            supabase.from("subscriptions").update(
                mapOf(
                    "end_date" to subscription.endDate.toString(),
                    "clothes_used" to subscription.clothesUsed,
                    "status" to subscription.status.value
                )
            ) {
                filter { eq("id", subscription.id.toString()) }
            }
        } catch (_: Exception) { }
    }

    suspend fun getSubscriptionById(id: String): Subscription? {
        val cid = activeCompanyId() ?: return null
        return subscriptionDao.getSubscriptionById(id, cid)?.toDomain()
    }

    private fun SubscriptionEntity.toDomain() = Subscription(
        id = UUID.fromString(id),
        customerId = UUID.fromString(customer_id),
        planId = UUID.fromString(plan_id),
        startDate = DateParsing.parseLocalDate(start_date),
        endDate = DateParsing.parseLocalDate(end_date),
        clothesQuota = clothes_quota,
        clothesUsed = clothes_used,
        status = SubscriptionStatus.fromValue(status),
        renewedFromId = renewed_from_id?.let { UUID.fromString(it) },
        createdAt = DateParsing.parseLocalDateTime(created_at)
    )

    private fun Subscription.toEntity(cid: String?) = SubscriptionEntity(
        id = id.toString(),
        companyId = cid ?: "",
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