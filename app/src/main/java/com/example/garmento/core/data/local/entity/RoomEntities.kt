package com.example.garmento.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey val id: String,
    val name: String,
    val phone: String,
    val address: String,
    val notes: String,
    val public_code: String,
    val created_at: String
)

@Entity(tableName = "subscription_plans")
data class PlanEntity(
    @PrimaryKey val id: String,
    val name: String,
    val duration_days: Int,
    val amount: Double,
    val total_clothes: Int,
    val is_active: Boolean,
    val created_at: String
)

@Entity(tableName = "subscriptions")
data class SubscriptionEntity(
    @PrimaryKey val id: String,
    val customer_id: String,
    val plan_id: String,
    val start_date: String,
    val end_date: String,
    val clothes_quota: Int,
    val clothes_used: Int,
    val status: String,
    val renewed_from_id: String?,
    val created_at: String
)

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey val id: String,
    val subscription_id: String,
    val order_number: String,
    val clothes_count: Int,
    val clothes_from_quota: Int,
    val clothes_overage: Int,
    val order_date: String,
    val is_voided: Boolean,
    val void_reason: String?,
    val grace_period: Boolean
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val customer_id: String,
    val subscription_id: String,
    val amount: Double,
    val type: String,
    val payment_method: String,
    val transaction_date: String
)

@Entity(tableName = "pending_sync")
data class PendingSyncEntity(
    @PrimaryKey val id: String,
    val table_name: String,
    val payload_json: String,
    val created_at: String,
    val synced: Boolean
)