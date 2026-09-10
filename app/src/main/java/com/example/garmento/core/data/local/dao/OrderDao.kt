package com.example.garmento.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.garmento.core.data.local.entity.OrderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {

    @Query("SELECT * FROM orders WHERE subscription_id = :subscriptionId ORDER BY order_date DESC")
    fun getOrdersBySubscription(subscriptionId: String): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE id = :id")
    suspend fun getOrderById(id: String): OrderEntity?

    @Query("SELECT * FROM orders ORDER BY order_date DESC LIMIT :limit")
    fun getRecentOrders(limit: Int): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE is_voided = 0 ORDER BY order_date DESC")
    fun getActiveOrders(): Flow<List<OrderEntity>>

    @Query("SELECT COUNT(*) FROM orders WHERE is_voided = 0 AND order_date LIKE '%' || :datePrefix || '%'")
    suspend fun getOrdersCountForDate(datePrefix: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrders(orders: List<OrderEntity>)

    @Update
    suspend fun updateOrder(order: OrderEntity)

    @Query("DELETE FROM orders")
    suspend fun deleteAllOrders()
}