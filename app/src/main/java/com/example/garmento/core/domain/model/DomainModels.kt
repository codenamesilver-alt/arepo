package com.example.garmento.core.domain.model

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlinx.serialization.Serializable

enum class UserRole {
    ADMIN,
    STAFF
}

data class Customer(
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val phone: String,
    val address: String = "",
    val notes: String = "",
    val publicCode: String,
    val createdAt: LocalDateTime = LocalDateTime.now()
)

data class SubscriptionPlan(
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val durationDays: Int,
    val amount: Double,
    val totalClothes: Int,
    val isActive: Boolean = true,
    val createdAt: LocalDateTime = LocalDateTime.now()
)

data class Subscription(
    val id: UUID = UUID.randomUUID(),
    val customerId: UUID,
    val planId: UUID,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val clothesQuota: Int,
    val clothesUsed: Int = 0,
    val status: SubscriptionStatus = SubscriptionStatus.ACTIVE,
    val renewedFromId: UUID? = null,
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    val remainingClothes: Int
        get() = maxOf(clothesQuota - clothesUsed, 0)

    val isExpired: Boolean
        get() = LocalDate.now().isAfter(endDate)

    val daysRemaining: Long
        get() = maxOf(ChronoUnit.DAYS.between(LocalDate.now(), endDate), 0)

    val clothesOverage: Int
        get() = maxOf(clothesUsed - clothesQuota, 0)

    fun isInGracePeriod(gracePeriodDays: Int): Boolean =
        isExpired && gracePeriodDays > 0 &&
            LocalDate.now() in endDate..endDate.plusDays(gracePeriodDays.toLong())

    val statusBadge: String
        get() = when {
            isExpired -> "Expired"
            clothesOverage > 0 -> "Over quota ($clothesOverage)"
            remainingClothes <= 5 -> "Low quota ($remainingClothes)"
            else -> "Active"
        }
}

enum class SubscriptionStatus(val value: String) {
    ACTIVE("active"),
    EXPIRED("expired"),
    RENEWED("renewed");

    companion object {
        fun fromValue(value: String): SubscriptionStatus =
            entries.firstOrNull { it.value == value } ?: ACTIVE
    }
}

enum class DeliveryStatus(val value: String) {
    PENDING("pending"),
    DELIVERED("delivered"),
    PARTIAL("partial");

    companion object {
        fun fromValue(value: String?): DeliveryStatus =
            entries.firstOrNull { it.value == value } ?: PENDING
    }
}

@Serializable
data class OrderItem(
    val clothesType: String,
    val quantity: Int,
    val unitPrice: Double
) {
    val lineTotal: Double get() = quantity * unitPrice
}

data class Order(
    val id: UUID = UUID.randomUUID(),
    val subscriptionId: UUID? = null,
    val customerId: UUID? = null,
    val isSubscription: Boolean = true,
    val orderNumber: String,
    val clothesCount: Int,
    val clothesFromQuota: Int = 0,
    val clothesOverage: Int = 0,
    val items: List<OrderItem> = emptyList(),
    val totalAmount: Double = 0.0,
    val orderDate: LocalDateTime = LocalDateTime.now(),
    val isVoided: Boolean = false,
    val voidReason: String? = null,
    val gracePeriod: Boolean = false,
    val deliveryDate: LocalDate? = null,
    val deliveryStatus: DeliveryStatus = DeliveryStatus.PENDING,
    val deliveryNotes: String? = null
)

data class Transaction(
    val id: UUID = UUID.randomUUID(),
    val customerId: UUID,
    val subscriptionId: UUID? = null,
    val amount: Double,
    val type: TransactionType,
    val paymentMethod: String = "cash",
    val transactionDate: LocalDateTime = LocalDateTime.now()
)

enum class TransactionType(val value: String) {
    NEW("new"),
    RENEWAL("renewal"),
    OVERAGE("overage");

    companion object {
        fun fromValue(value: String): TransactionType =
            entries.firstOrNull { it.value == value } ?: NEW
    }
}

data class OrderAudit(
    val id: UUID = UUID.randomUUID(),
    val orderId: UUID,
    val action: AuditAction,
    val oldClothesCount: Int? = null,
    val newClothesCount: Int? = null,
    val reason: String? = null,
    val performedBy: String = "",
    val timestamp: LocalDateTime = LocalDateTime.now()
)

enum class AuditAction(val value: String) {
    EDIT("edit"),
    VOID("void");

    companion object {
        fun fromValue(value: String): AuditAction =
            entries.firstOrNull { it.value == value } ?: EDIT
    }
}

data class PendingSync(
    val id: UUID = UUID.randomUUID(),
    val tableName: String,
    val payloadJson: String,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val synced: Boolean = false
)
