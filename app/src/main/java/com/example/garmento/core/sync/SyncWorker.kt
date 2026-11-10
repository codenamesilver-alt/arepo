package com.example.garmento.core.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.garmento.core.data.local.dao.PendingSyncDao
import com.example.garmento.core.util.UserSession
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val pendingSyncDao: PendingSyncDao,
    private val supabase: SupabaseClient
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        if (!isNetworkAvailable()) return Result.retry()

        val activeCompanyId = UserSession.currentCompanyId?.takeIf { it.isNotBlank() }
            ?: UserSession.companyId?.takeIf { it.isNotBlank() }
        // No active company: skip sync entirely.
        if (activeCompanyId.isNullOrBlank()) return Result.success()

        return try {
            val pendingItems = pendingSyncDao.getPendingItemsList(activeCompanyId)
            for (item in pendingItems) {
                try {
                    val rowCompanyId = item.companyId.takeIf { it.isNotBlank() } ?: activeCompanyId
                    val parsed = Json.parseToJsonElement(item.payload_json)
                    val payload = when (parsed) {
                        is JsonObject -> JsonObject(parsed.toMap() + ("company_id" to JsonPrimitive(rowCompanyId)))
                        else -> parsed
                    }

                    when (item.table_name) {
                        "orders" -> supabase.from("orders").insert(payload)
                        "customers" -> supabase.from("customers").insert(payload)
                        "subscriptions" -> supabase.from("subscriptions").insert(payload)
                        "subscription_plans" -> supabase.from("subscription_plans").upsert(payload)
                        "transactions" -> supabase.from("transactions").insert(payload)
                    }
                    pendingSyncDao.markSynced(item.id, rowCompanyId)
                } catch (_: Exception) {
                    // Continue with next item
                }
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val context = applicationContext
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}