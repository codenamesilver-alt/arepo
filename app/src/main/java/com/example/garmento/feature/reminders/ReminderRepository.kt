package com.example.garmento.feature.reminders

import com.example.garmento.core.util.DateParsing

import com.example.garmento.core.data.local.dao.SubscriptionDao
import com.example.garmento.core.domain.model.Subscription
import com.example.garmento.core.domain.model.SubscriptionStatus
import com.example.garmento.core.util.UserSession
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
    private fun activeCompanyId(): String? = UserSession.currentCompanyId?.takeIf { it.isNotBlank() }

    suspend fun getExpiringSoon(daysAhead: Int = 7): List<Subscription> {
        val cid = activeCompanyId() ?: return emptyList()
        val targetDate = LocalDate.now().plusDays(daysAhead.toLong()).toString()
        return subscriptionDao.getExpiringSubscriptions(targetDate, cid)
            .map { it.toDomain() }
    }

    suspend fun getAllActiveSubscriptions(): List<Subscription> {
        val cid = activeCompanyId() ?: return emptyList()
        return subscriptionDao.getAllActiveSubscriptions(cid)
            .map { it.toDomain() }
    }

    suspend fun getExpiringInDays(days: Int): List<Subscription> {
        val cid = activeCompanyId() ?: return emptyList()
        val allActive = subscriptionDao.getAllActiveSubscriptions(cid)
        return allActive.filter { entity ->
            val endDate = DateParsing.parseLocalDate(entity.end_date)
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
            startDate = DateParsing.parseLocalDate(start_date),
            endDate = DateParsing.parseLocalDate(end_date),
            clothesQuota = clothes_quota,
            clothesUsed = clothes_used,
            status = SubscriptionStatus.fromValue(status),
            renewedFromId = renewed_from_id?.let { UUID.fromString(it) },
            createdAt = DateParsing.parseLocalDateTime(created_at)
        )
}