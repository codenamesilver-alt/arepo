package com.example.garmento.feature.customers

import com.example.garmento.core.data.local.dao.CustomerDao
import com.example.garmento.core.data.local.dao.SubscriptionDao
import com.example.garmento.core.data.local.entity.CustomerEntity
import com.example.garmento.core.data.local.entity.SubscriptionEntity
import com.example.garmento.core.domain.model.Customer
import com.example.garmento.core.domain.model.Subscription
import com.example.garmento.core.domain.model.SubscriptionStatus
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class SupabaseCustomer(
    val id: String,
    val name: String,
    val phone: String,
    val address: String?,
    val notes: String?,
    val public_code: String,
    val created_at: String
)

@Singleton
class CustomerRepository @Inject constructor(
    private val customerDao: CustomerDao,
    private val subscriptionDao: SubscriptionDao,
    private val supabase: SupabaseClient
) {
    fun getAllCustomers(): Flow<List<Customer>> {
        return customerDao.getAllCustomers().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun searchCustomers(query: String): Flow<List<Customer>> {
        return customerDao.searchCustomers(query).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun getCustomerById(id: String): Customer? {
        return customerDao.getCustomerById(id)?.toDomain()
    }

    suspend fun getCustomerByPublicCode(code: String): Customer? {
        return customerDao.getCustomerByPublicCode(code)?.toDomain()
    }

    suspend fun saveCustomer(customer: Customer) {
        val entity = customer.toEntity()
        customerDao.insertCustomer(entity)
        try {
            supabase.from("customers").upsert(
                SupabaseCustomer(
                    id = customer.id.toString(),
                    name = customer.name,
                    phone = customer.phone,
                    address = customer.address.ifBlank { null },
                    notes = customer.notes.ifBlank { null },
                    public_code = customer.publicCode,
                    created_at = customer.createdAt.toString()
                )
            )
        } catch (_: Exception) {
            // Offline
        }
    }

    suspend fun getActiveSubscription(customerId: String): Subscription? {
        return subscriptionDao.getActiveSubscriptionForCustomer(customerId)?.toDomain()
    }

    suspend fun getSubscriptions(customerId: String): List<Subscription> {
        return subscriptionDao.getSubscriptionsByCustomer(customerId)
            .first()
            .map { it.toDomain() }
    }

    suspend fun generatePublicCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return buildString {
            repeat(6) { append(chars.random()) }
        }
    }

    suspend fun syncFromRemote() {
        try {
            val remote = supabase.from("customers")
                .select()
                .decodeList<SupabaseCustomer>()
            customerDao.deleteAllCustomers()
            customerDao.insertCustomers(remote.map { it.toEntity() })
        } catch (_: Exception) {
            // Use local cache
        }
    }

    suspend fun getCustomerCount(): Int = customerDao.getCustomerCount()

    private fun CustomerEntity.toDomain() = Customer(
        id = UUID.fromString(id),
        name = name,
        phone = phone,
        address = address,
        notes = notes,
        publicCode = public_code,
        createdAt = LocalDateTime.parse(created_at)
    )

    private fun Customer.toEntity() = CustomerEntity(
        id = id.toString(),
        name = name,
        phone = phone,
        address = address,
        notes = notes,
        public_code = publicCode,
        created_at = createdAt.toString()
    )

    private fun SupabaseCustomer.toEntity() = CustomerEntity(
        id = id,
        name = name,
        phone = phone,
        address = address ?: "",
        notes = notes ?: "",
        public_code = public_code,
        created_at = created_at
    )

    private fun SubscriptionEntity.toDomain() = Subscription(
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
