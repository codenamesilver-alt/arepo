package com.example.garmento.feature.customer_portal

import com.example.garmento.core.data.local.dao.CustomerDao
import com.example.garmento.core.data.local.dao.SubscriptionDao
import com.example.garmento.core.domain.model.Customer
import com.example.garmento.core.domain.model.Subscription
import com.example.garmento.core.domain.model.SubscriptionPlan
import com.example.garmento.core.domain.model.SubscriptionStatus
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class SupabaseCustomer(
    val id: String,
    val name: String,
    val phone: String,
    val address: String?,
    val notes: String?,
    val public_code: String,
    val created_at: String
)

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
private data class SupabasePlan(
    val id: String,
    val name: String,
    val duration_days: Int,
    val amount: Double,
    val total_clothes: Int,
    val is_active: Boolean,
    val created_at: String
)

@Singleton
class CustomerPortalRepository @Inject constructor(
    private val customerDao: CustomerDao,
    private val subscriptionDao: SubscriptionDao,
    private val supabase: SupabaseClient
) {
    suspend fun getByPublicCode(code: String): Customer? {
        return try {
            val customers = supabase.from("customers")
                .select()
                .decodeList<SupabaseCustomer>()
            val found = customers.firstOrNull { it.public_code == code }
            found?.let { sc ->
                Customer(
                    id = UUID.fromString(sc.id),
                    name = sc.name,
                    phone = sc.phone,
                    address = sc.address ?: "",
                    notes = sc.notes ?: "",
                    publicCode = sc.public_code,
                    createdAt = LocalDateTime.parse(sc.created_at)
                )
            }
        } catch (_: Exception) {
            customerDao.getCustomerByPublicCode(code)?.let { entity ->
                Customer(
                    id = UUID.fromString(entity.id),
                    name = entity.name,
                    phone = entity.phone,
                    address = entity.address,
                    notes = entity.notes,
                    publicCode = entity.public_code,
                    createdAt = LocalDateTime.parse(entity.created_at)
                )
            }
        }
    }

    suspend fun getActiveSubscription(customerId: UUID): Subscription? {
        return try {
            val subs = supabase.from("subscriptions")
                .select()
                .decodeList<SupabaseSubscription>()
            val found = subs
                .filter { it.customer_id == customerId.toString() }
                .firstOrNull { it.status == "active" }
            found?.let { ss ->
                Subscription(
                    id = UUID.fromString(ss.id),
                    customerId = UUID.fromString(ss.customer_id),
                    planId = UUID.fromString(ss.plan_id),
                    startDate = LocalDate.parse(ss.start_date),
                    endDate = LocalDate.parse(ss.end_date),
                    clothesQuota = ss.clothes_quota,
                    clothesUsed = ss.clothes_used,
                    status = SubscriptionStatus.fromValue(ss.status),
                    renewedFromId = ss.renewed_from_id?.let { UUID.fromString(it) },
                    createdAt = LocalDateTime.parse(ss.created_at)
                )
            }
        } catch (_: Exception) {
            subscriptionDao.getActiveSubscriptionForCustomer(customerId.toString())?.let { entity ->
                Subscription(
                    id = UUID.fromString(entity.id),
                    customerId = UUID.fromString(entity.customer_id),
                    planId = UUID.fromString(entity.plan_id),
                    startDate = LocalDate.parse(entity.start_date),
                    endDate = LocalDate.parse(entity.end_date),
                    clothesQuota = entity.clothes_quota,
                    clothesUsed = entity.clothes_used,
                    status = SubscriptionStatus.fromValue(entity.status),
                    renewedFromId = entity.renewed_from_id?.let { UUID.fromString(it) },
                    createdAt = LocalDateTime.parse(entity.created_at)
                )
            }
        }
    }

    suspend fun getPlan(planId: UUID): SubscriptionPlan? {
        return try {
            val plans = supabase.from("subscription_plans")
                .select()
                .decodeList<SupabasePlan>()
            val found = plans.firstOrNull { it.id == planId.toString() }
            found?.let { sp ->
                SubscriptionPlan(
                    id = UUID.fromString(sp.id),
                    name = sp.name,
                    durationDays = sp.duration_days,
                    amount = sp.amount,
                    totalClothes = sp.total_clothes,
                    isActive = sp.is_active,
                    createdAt = LocalDateTime.parse(sp.created_at)
                )
            }
        } catch (_: Exception) {
            null
        }
    }
}
