package com.example.garmento.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.util.UUID

class DomainModelsTest {

    @Test
    fun `subscription remainingClothes returns quota minus used`() {
        val sub = Subscription(
            customerId = UUID.randomUUID(),
            planId = UUID.randomUUID(),
            startDate = LocalDate.now(),
            endDate = LocalDate.now().plusDays(30),
            clothesQuota = 20,
            clothesUsed = 8
        )
        assertEquals(12, sub.remainingClothes)
    }

    @Test
    fun `subscription remainingClothes never negative`() {
        val sub = Subscription(
            customerId = UUID.randomUUID(),
            planId = UUID.randomUUID(),
            startDate = LocalDate.now(),
            endDate = LocalDate.now().plusDays(30),
            clothesQuota = 10,
            clothesUsed = 15
        )
        assertEquals(0, sub.remainingClothes)
    }

    @Test
    fun `subscription clothesOverage returns positive when over quota`() {
        val sub = Subscription(
            customerId = UUID.randomUUID(),
            planId = UUID.randomUUID(),
            startDate = LocalDate.now(),
            endDate = LocalDate.now().plusDays(30),
            clothesQuota = 10,
            clothesUsed = 15
        )
        assertEquals(5, sub.clothesOverage)
    }

    @Test
    fun `subscription clothesOverage is zero when under quota`() {
        val sub = Subscription(
            customerId = UUID.randomUUID(),
            planId = UUID.randomUUID(),
            startDate = LocalDate.now(),
            endDate = LocalDate.now().plusDays(30),
            clothesQuota = 20,
            clothesUsed = 5
        )
        assertEquals(0, sub.clothesOverage)
    }

    @Test
    fun `subscription isExpired when endDate is past`() {
        val sub = Subscription(
            customerId = UUID.randomUUID(),
            planId = UUID.randomUUID(),
            startDate = LocalDate.now().minusDays(60),
            endDate = LocalDate.now().minusDays(1),
            clothesQuota = 20,
            clothesUsed = 10
        )
        assertTrue(sub.isExpired)
    }

    @Test
    fun `subscription is not expired when endDate is future`() {
        val sub = Subscription(
            customerId = UUID.randomUUID(),
            planId = UUID.randomUUID(),
            startDate = LocalDate.now(),
            endDate = LocalDate.now().plusDays(30),
            clothesQuota = 20,
            clothesUsed = 5
        )
        assertFalse(sub.isExpired)
    }

    @Test
    fun `subscription daysRemaining is zero when expired`() {
        val sub = Subscription(
            customerId = UUID.randomUUID(),
            planId = UUID.randomUUID(),
            startDate = LocalDate.now().minusDays(60),
            endDate = LocalDate.now().minusDays(5),
            clothesQuota = 20,
            clothesUsed = 10
        )
        assertEquals(0L, sub.daysRemaining)
    }

    @Test
    fun `subscriptionStatus fromValue returns correct enum`() {
        assertEquals(SubscriptionStatus.ACTIVE, SubscriptionStatus.fromValue("active"))
        assertEquals(SubscriptionStatus.EXPIRED, SubscriptionStatus.fromValue("expired"))
        assertEquals(SubscriptionStatus.RENEWED, SubscriptionStatus.fromValue("renewed"))
        assertEquals(SubscriptionStatus.ACTIVE, SubscriptionStatus.fromValue("unknown"))
    }

    @Test
    fun `transactionType fromValue returns correct enum`() {
        assertEquals(TransactionType.NEW, TransactionType.fromValue("new"))
        assertEquals(TransactionType.RENEWAL, TransactionType.fromValue("renewal"))
        assertEquals(TransactionType.NEW, TransactionType.fromValue("unknown"))
    }

    @Test
    fun `auditAction fromValue returns correct enum`() {
        assertEquals(AuditAction.EDIT, AuditAction.fromValue("edit"))
        assertEquals(AuditAction.VOID, AuditAction.fromValue("void"))
        assertEquals(AuditAction.EDIT, AuditAction.fromValue("unknown"))
    }
}
