package com.example.garmento.core.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.garmento.core.data.local.dao.PendingSyncDao
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.json.Json
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

        return try {
            val pendingItems = pendingSyncDao.getPendingItemsList()
            for (item in pendingItems) {
                try {
                    val jsonElement = Json.parseToJsonElement(item.payload_json)
                    when (item.table_name) {
                        "orders" -> supabase.from("orders").insert(jsonElement)
                        "customers" -> supabase.from("customers").insert(jsonElement)
                        "subscriptions" -> supabase.from("subscriptions").insert(jsonElement)
                        "transactions" -> supabase.from("transactions").insert(jsonElement)
                    }
                    pendingSyncDao.markSynced(item.id)
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
