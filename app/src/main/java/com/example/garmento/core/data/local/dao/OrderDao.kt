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
    data class OrderWithCustomer(
        val id: String,
        val subscription_id: String,
        val order_number: String,
        val clothes_count: Int,
        val clothes_from_quota: Int,
        val clothes_overage: Int,
        val order_date: String,
        val is_voided: Boolean,
        val void_reason: String?,
        val grace_period: Boolean,
        val delivery_date: String?,
        val delivery_status: String,
        val delivery_notes: String?,
        val customer_name: String,
        val customer_phone: String
    )

    @Query("""
        SELECT o.*, c.name AS customer_name, c.phone AS customer_phone
        FROM orders o
        JOIN subscriptions s ON o.subscription_id = s.id
        JOIN customers c ON s.customer_id = c.id
        WHERE o.companyId = :companyId
    """)
    fun getAllOrdersWithCustomers(companyId: String = ""): Flow<List<OrderWithCustomer>>

    @Query("""
        SELECT o.*, c.name AS customer_name, c.phone AS customer_phone
        FROM orders o
        JOIN subscriptions s ON o.subscription_id = s.id
        JOIN customers c ON s.customer_id = c.id
        WHERE o.companyId = :companyId
          AND o.is_voided = 0
          AND o.delivery_status != 'delivered'
          AND o.delivery_date IS NOT NULL
          AND o.delivery_date >= :today
        ORDER BY o.delivery_date ASC
        LIMIT :limit
    """)
    fun getUpcomingDeliveries(today: String, limit: Int, companyId: String = ""): Flow<List<OrderWithCustomer>>

    @Query("SELECT * FROM orders WHERE companyId = :companyId AND subscription_id = :subscriptionId ORDER BY order_date DESC")
    fun getOrdersBySubscription(subscriptionId: String, companyId: String = ""): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE id = :id AND companyId = :companyId")
    suspend fun getOrderById(id: String, companyId: String = ""): OrderEntity?

    @Query("SELECT * FROM orders WHERE companyId = :companyId ORDER BY order_date DESC LIMIT :limit")
    fun getRecentOrders(limit: Int, companyId: String = ""): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE companyId = :companyId AND is_voided = 0 ORDER BY order_date DESC")
    fun getActiveOrders(companyId: String = ""): Flow<List<OrderEntity>>

    @Query("SELECT COUNT(*) FROM orders WHERE companyId = :companyId AND is_voided = 0 AND order_date LIKE '%' || :datePrefix || '%'")
    suspend fun getOrdersCountForDate(datePrefix: String, companyId: String = ""): Int

    @Query("SELECT SUM(clothes_overage) FROM orders WHERE companyId = :companyId AND is_voided = 0 AND order_date >= :sinceStr")
    suspend fun getOverageItemsSince(sinceStr: String, companyId: String = ""): Int?

    @Query("SELECT SUM(clothes_overage) FROM orders WHERE companyId = :companyId AND is_voided = 0 AND order_date LIKE '%' || :datePrefix || '%'")
    suspend fun getOverageItemsForDate(datePrefix: String, companyId: String = ""): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrders(orders: List<OrderEntity>)

    @Update
    suspend fun updateOrder(order: OrderEntity)

    @Query("DELETE FROM orders WHERE companyId = :companyId")
    suspend fun deleteAllOrders(companyId: String = "")
}