package com.example.garmento.feature.plans

import com.example.garmento.core.data.local.dao.PlanDao
import com.example.garmento.core.data.local.entity.PlanEntity
import com.example.garmento.core.domain.model.SubscriptionPlan
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

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
class PlanRepository @Inject constructor(
    private val planDao: PlanDao,
    private val supabase: SupabaseClient
) {
    fun getAllPlans(): Flow<List<SubscriptionPlan>> {
        return planDao.getAllPlans().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun getActivePlans(): Flow<List<SubscriptionPlan>> {
        return planDao.getActivePlans().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun getPlanById(id: String): SubscriptionPlan? {
        return planDao.getPlanById(id)?.toDomain()
    }

    suspend fun savePlan(plan: SubscriptionPlan) {
        val entity = plan.toEntity()
        planDao.insertPlan(entity)
        try {
            supabase.from("subscription_plans").upsert(
                SupabasePlan(
                    id = plan.id.toString(),
                    name = plan.name,
                    duration_days = plan.durationDays,
                    amount = plan.amount,
                    total_clothes = plan.totalClothes,
                    is_active = plan.isActive,
                    created_at = plan.createdAt.toString()
                )
            )
        } catch (_: Exception) {
            // Offline: already saved to Room
        }
    }

    suspend fun toggleActive(planId: String) {
        val plan = planDao.getPlanById(planId) ?: return
        val updated = plan.copy(is_active = !plan.is_active)
        planDao.updatePlan(updated)
        try {
            supabase.from("subscription_plans").update(
                mapOf("is_active" to updated.is_active)
            ) {
                filter { eq("id", planId) }
            }
        } catch (_: Exception) {
            // Offline: already saved to Room
        }
    }

    suspend fun syncFromRemote() {
        try {
            val remote = supabase.from("subscription_plans")
                .select()
                .decodeList<SupabasePlan>()
            planDao.deleteAllPlans()
            planDao.insertPlans(remote.map { it.toEntity() })
        } catch (_: Exception) {
            // Offline: use local cache
        }
    }

    private fun PlanEntity.toDomain() = SubscriptionPlan(
        id = UUID.fromString(id),
        name = name,
        durationDays = duration_days,
        amount = amount,
        totalClothes = total_clothes,
        isActive = is_active,
        createdAt = LocalDateTime.parse(created_at)
    )

    private fun SubscriptionPlan.toEntity() = PlanEntity(
        id = id.toString(),
        name = name,
        duration_days = durationDays,
        amount = amount,
        total_clothes = totalClothes,
        is_active = isActive,
        created_at = createdAt.toString()
    )

    private fun SupabasePlan.toEntity() = PlanEntity(
        id = id,
        name = name,
        duration_days = duration_days,
        amount = amount,
        total_clothes = total_clothes,
        is_active = is_active,
        created_at = created_at
    )
}
