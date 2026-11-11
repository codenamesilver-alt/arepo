package com.example.garmento.feature.plans

import com.example.garmento.core.util.DateParsing

import com.example.garmento.core.data.local.dao.PlanDao
import com.example.garmento.core.data.local.dao.PendingSyncDao
import com.example.garmento.core.data.local.entity.PlanEntity
import com.example.garmento.core.data.local.entity.PendingSyncEntity
import com.example.garmento.core.domain.model.SubscriptionPlan
import com.example.garmento.core.util.UserSession
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class SupabasePlan(
    val id: String,
    val company_id: String? = null,
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
    private val pendingSyncDao: PendingSyncDao,
    private val supabase: SupabaseClient
) {
    private fun activeCompanyId(): String? = UserSession.currentCompanyId?.takeIf { it.isNotBlank() }

    fun getAllPlans(): Flow<List<SubscriptionPlan>> {
        val cid = activeCompanyId() ?: return flowOf(emptyList())
        return planDao.getAllPlans(cid).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun getActivePlans(): Flow<List<SubscriptionPlan>> {
        val cid = activeCompanyId() ?: return flowOf(emptyList())
        return planDao.getActivePlans(cid).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun getPlanById(id: String): SubscriptionPlan? {
        val cid = activeCompanyId() ?: return null
        return planDao.getPlanById(id, cid)?.toDomain()
    }

    suspend fun savePlan(plan: SubscriptionPlan) {
        val cid = activeCompanyId() ?: return
        val entity = plan.toEntity(cid)
        planDao.insertPlan(entity)
        val payload = SupabasePlan(
            id = plan.id.toString(),
            company_id = cid,
            name = plan.name,
            duration_days = plan.durationDays,
            amount = plan.amount,
            total_clothes = plan.totalClothes,
            is_active = plan.isActive,
            created_at = plan.createdAt.toString()
        )
        try {
            supabase.from("subscription_plans").upsert(payload)
        } catch (_: Exception) {
            // Offline or schema mismatch: keep local copy and queue for retry
            queueForSync(payload, cid)
        }
    }

    suspend fun deletePlan(planId: String) {
        val cid = activeCompanyId() ?: return
        val entity = planDao.getPlanById(planId, cid) ?: return
        planDao.deletePlan(entity)
        try {
            supabase.from("subscription_plans").delete {
                filter { eq("id", planId) }
            }
        } catch (_: Exception) {
            // Offline
        }
    }

    suspend fun toggleActive(planId: String) {
        val cid = activeCompanyId() ?: return
        val plan = planDao.getPlanById(planId, cid) ?: return
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
        val cid = activeCompanyId() ?: return
        try {
            val remote = supabase.from("subscription_plans")
                .select()
                .decodeList<SupabasePlan>()
            val remoteIds = remote.map { it.id }.toSet()
            // Keep plans that only exist locally (offline-create, failed upload, pending sync)
            // so a remote pull never wipes data that never reached the server.
            val localOnly = planDao.getAllPlansOnce(cid).filter { it.id !in remoteIds }
            planDao.deleteAllPlans(cid)
            planDao.insertPlans(remote.map { it.toEntity(cid) } + localOnly)
        } catch (_: Exception) {
            // Offline or schema mismatch: keep local cache
        }
    }

    private suspend fun queueForSync(payload: SupabasePlan, cid: String) {
        try {
            pendingSyncDao.insertPendingItem(
                PendingSyncEntity(
                    id = UUID.randomUUID().toString(),
                    companyId = cid,
                    table_name = "subscription_plans",
                    payload_json = Json.encodeToString(payload),
                    created_at = LocalDateTime.now().toString(),
                    synced = false
                )
            )
        } catch (_: Exception) {
            // Local only
        }
    }

    private fun PlanEntity.toDomain() = SubscriptionPlan(
        id = UUID.fromString(id),
        name = name,
        durationDays = duration_days,
        amount = amount,
        totalClothes = total_clothes,
        isActive = is_active,
        createdAt = DateParsing.parseLocalDateTime(created_at)
    )

    private fun SubscriptionPlan.toEntity(cid: String) = PlanEntity(
        id = id.toString(),
        companyId = cid,
        name = name,
        duration_days = durationDays,
        amount = amount,
        total_clothes = totalClothes,
        is_active = isActive,
        created_at = createdAt.toString()
    )

    private fun SupabasePlan.toEntity(cid: String) = PlanEntity(
        id = id,
        companyId = company_id ?: cid,
        name = name,
        duration_days = duration_days,
        amount = amount,
        total_clothes = total_clothes,
        is_active = is_active,
        created_at = created_at
    )
}