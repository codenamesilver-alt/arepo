package com.example.garmento.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.garmento.core.data.local.entity.PendingSyncEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingSyncDao {

    @Query("SELECT * FROM pending_sync WHERE synced = 0 ORDER BY created_at ASC")
    fun getPendingItems(): Flow<List<PendingSyncEntity>>

    @Query("SELECT * FROM pending_sync WHERE synced = 0")
    suspend fun getPendingItemsList(): List<PendingSyncEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPendingItem(item: PendingSyncEntity)

    @Query("UPDATE pending_sync SET synced = 1 WHERE id = :id")
    suspend fun markSynced(id: String)

    @Query("DELETE FROM pending_sync WHERE synced = 1")
    suspend fun deleteSyncedItems()

    @Query("SELECT COUNT(*) FROM pending_sync WHERE synced = 0")
    suspend fun getPendingCount(): Int
}