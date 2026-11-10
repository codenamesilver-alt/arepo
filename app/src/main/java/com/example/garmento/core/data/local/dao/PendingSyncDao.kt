package com.example.garmento.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.garmento.core.data.local.entity.PendingSyncEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingSyncDao {

    @Query("SELECT * FROM pending_sync WHERE companyId = :companyId AND synced = 0 ORDER BY created_at ASC")
    fun getPendingItems(companyId: String = ""): Flow<List<PendingSyncEntity>>

    @Query("SELECT * FROM pending_sync WHERE companyId = :companyId AND synced = 0")
    suspend fun getPendingItemsList(companyId: String = ""): List<PendingSyncEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPendingItem(item: PendingSyncEntity)

    @Query("UPDATE pending_sync SET synced = 1 WHERE id = :id AND companyId = :companyId")
    suspend fun markSynced(id: String, companyId: String = "")

    @Query("DELETE FROM pending_sync WHERE companyId = :companyId AND synced = 1")
    suspend fun deleteSyncedItems(companyId: String = "")

    @Query("SELECT COUNT(*) FROM pending_sync WHERE companyId = :companyId AND synced = 0")
    suspend fun getPendingCount(companyId: String = ""): Int
}