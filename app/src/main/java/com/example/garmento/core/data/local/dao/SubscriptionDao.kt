package com.example.garmento.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.garmento.core.data.local.entity.SubscriptionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubscriptionDao {

    @Query("SELECT * FROM subscriptions WHERE customer_id = :customerId ORDER BY created_at DESC")
    fun getSubscriptionsByCustomer(customerId: String): Flow<List<SubscriptionEntity>>

    @Query("SELECT * FROM subscriptions WHERE id = :id")
    suspend fun getSubscriptionById(id: String): SubscriptionEntity?

    @Query("SELECT * FROM subscriptions WHERE customer_id = :customerId AND status = 'active'")
    suspend fun getActiveSubscriptionForCustomer(customerId: String): SubscriptionEntity?

    @Query("SELECT * FROM subscriptions WHERE status = 'active' AND end_date <= :dateStr")
    suspend fun getExpiringSubscriptions(dateStr: String): List<SubscriptionEntity>

    @Query("SELECT * FROM subscriptions WHERE status = 'active'")
    suspend fun getAllActiveSubscriptions(): List<SubscriptionEntity>

    @Query("SELECT COUNT(*) FROM subscriptions WHERE status = 'active'")
    suspend fun getActiveSubscriptionCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscription(subscription: SubscriptionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscriptions(subscriptions: List<SubscriptionEntity>)

    @Update
    suspend fun updateSubscription(subscription: SubscriptionEntity)

    @Query("DELETE FROM subscriptions")
    suspend fun deleteAllSubscriptions()
}