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

    @Query("SELECT * FROM subscriptions WHERE companyId = :companyId AND customer_id = :customerId ORDER BY created_at DESC")
    fun getSubscriptionsByCustomer(customerId: String, companyId: String = ""): Flow<List<SubscriptionEntity>>

    @Query("SELECT * FROM subscriptions WHERE id = :id AND companyId = :companyId")
    suspend fun getSubscriptionById(id: String, companyId: String = ""): SubscriptionEntity?

    @Query("SELECT * FROM subscriptions WHERE companyId = :companyId AND customer_id = :customerId AND status = 'active'")
    suspend fun getActiveSubscriptionForCustomer(customerId: String, companyId: String = ""): SubscriptionEntity?

    @Query("SELECT * FROM subscriptions WHERE companyId = :companyId AND status = 'active' AND end_date <= :dateStr")
    suspend fun getExpiringSubscriptions(dateStr: String, companyId: String = ""): List<SubscriptionEntity>

    @Query("SELECT * FROM subscriptions WHERE companyId = :companyId AND status = 'active'")
    suspend fun getAllActiveSubscriptions(companyId: String = ""): List<SubscriptionEntity>

    @Query("SELECT * FROM subscriptions WHERE companyId = :companyId ORDER BY created_at DESC")
    suspend fun getAllSubscriptions(companyId: String = ""): List<SubscriptionEntity>

    @Query("SELECT COUNT(DISTINCT customer_id) FROM subscriptions WHERE companyId = :companyId AND status = 'active' AND end_date >= :dateStr")
    suspend fun getActiveSubscriptionCount(dateStr: String, companyId: String = ""): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscription(subscription: SubscriptionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscriptions(subscriptions: List<SubscriptionEntity>)

    @Update
    suspend fun updateSubscription(subscription: SubscriptionEntity)

    @Query("DELETE FROM subscriptions WHERE id = :id AND companyId = :companyId")
    suspend fun deleteSubscription(id: String, companyId: String = "")

    @Query("DELETE FROM subscriptions WHERE companyId = :companyId")
    suspend fun deleteAllSubscriptions(companyId: String = "")
}