package com.example.garmento.feature.reminders

import com.example.garmento.core.data.local.dao.SubscriptionDao
import com.example.garmento.core.domain.model.Subscription
import com.example.garmento.core.domain.model.SubscriptionStatus
import io.github.jan.supabase.SupabaseClient
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderRepository @Inject constructor(
    private val subscriptionDao: SubscriptionDao,
    private val supabase: SupabaseClient
) {
    suspend fun getExpiringSoon(daysAhead: Int = 7): List<Subscription> {
        val targetDate = LocalDate.now().plusDays(daysAhead.toLong()).toString()
        return subscriptionDao.getExpiringSubscriptions(targetDate)
            .map { it.toDomain() }
    }

    suspend fun getAllActiveSubscriptions(): List<Subscription> {
        return subscriptionDao.getAllActiveSubscriptions()
            .map { it.toDomain() }
    }

    suspend fun getExpiringInDays(days: Int): List<Subscription> {
        val allActive = subscriptionDao.getAllActiveSubscriptions()
        return allActive.filter { entity ->
            val endDate = LocalDate.parse(entity.end_date)
            val daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), endDate)
            daysLeft in 0..days.toLong()
        }.map { it.toDomain() }
    }

    fun getWhatsAppUrl(phone: String, message: String): String {
        val encodedMessage = java.net.URLEncoder.encode(message, "UTF-8")
        return "https://wa.me/$phone?text=$encodedMessage"
    }

    fun getSmsUrl(phone: String, message: String): String {
        val encodedMessage = java.net.URLEncoder.encode(message, "UTF-8")
        return "smsto:$phone?body=$encodedMessage"
    }

    private fun com.example.garmento.core.data.local.entity.SubscriptionEntity.toDomain() =
        Subscription(
            id = UUID.fromString(id),
            customerId = UUID.fromString(customer_id),
            planId = UUID.fromString(plan_id),
            startDate = LocalDate.parse(start_date),
            endDate = LocalDate.parse(end_date),
            clothesQuota = clothes_quota,
            clothesUsed = clothes_used,
            status = SubscriptionStatus.fromValue(status),
            renewedFromId = renewed_from_id?.let { UUID.fromString(it) },
            createdAt = LocalDateTime.parse(created_at)
        )
}
