package com.example.garmento.feature.customers

import com.example.garmento.core.data.local.dao.CustomerDao
import com.example.garmento.core.data.local.dao.SubscriptionDao
import com.example.garmento.core.data.local.entity.CustomerEntity
import com.example.garmento.core.data.local.entity.SubscriptionEntity
import com.example.garmento.core.domain.model.Customer
import com.example.garmento.core.domain.model.Subscription
import com.example.garmento.core.domain.model.SubscriptionStatus
import com.example.garmento.core.util.UserSession
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
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
    val company_id: String? = null,
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
    private fun activeCompanyId(): String? = UserSession.currentCompanyId?.takeIf { it.isNotBlank() }

    fun getAllCustomers(): Flow<List<Customer>> {
        val cid = activeCompanyId() ?: return flowOf(emptyList())
        return customerDao.getAllCustomers(cid).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun searchCustomers(query: String): Flow<List<Customer>> {
        val cid = activeCompanyId() ?: return flowOf(emptyList())
        return customerDao.searchCustomers(query, cid).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun getCustomerById(id: String): Customer? {
        val cid = activeCompanyId() ?: return null
        return customerDao.getCustomerById(id, cid)?.toDomain()
    }

    suspend fun getCustomerByPublicCode(code: String): Customer? {
        val cid = activeCompanyId() ?: return null
        return customerDao.getCustomerByPublicCode(code, cid)?.toDomain()
    }

    suspend fun saveCustomer(customer: Customer) {
        val cid = activeCompanyId() ?: return
        val entity = customer.toEntity(cid)
        customerDao.insertCustomer(entity)
        try {
            supabase.from("customers").upsert(
                SupabaseCustomer(
                    id = customer.id.toString(),
                    company_id = cid,
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
        val cid = activeCompanyId() ?: return null
        return subscriptionDao.getActiveSubscriptionForCustomer(customerId, cid)?.toDomain()
    }

    suspend fun getSubscriptions(customerId: String): List<Subscription> {
        val cid = activeCompanyId() ?: return emptyList()
        return subscriptionDao.getSubscriptionsByCustomer(customerId, cid)
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
        val cid = activeCompanyId() ?: return
        try {
            val remote = supabase.from("customers")
                .select()
                .decodeList<SupabaseCustomer>()
            val remoteIds = remote.map { it.id }.toSet()
            // Keep customers that only exist locally (offline-create, failed upload)
            // so a remote pull never wipes data that never reached the server.
            val localOnly = customerDao.getAllCustomersOnce(cid).filter { it.id !in remoteIds }
            customerDao.deleteAllCustomers(cid)
            customerDao.insertCustomers(remote.map { it.toEntity(cid) } + localOnly)
        } catch (_: Exception) {
            // Use local cache
        }
    }

    suspend fun getCustomerCount(): Int {
        val cid = activeCompanyId() ?: return 0
        return customerDao.getCustomerCount(cid)
    }

    private fun CustomerEntity.toDomain() = Customer(
        id = UUID.fromString(id),
        name = name,
        phone = phone,
        address = address,
        notes = notes,
        publicCode = public_code,
        createdAt = LocalDateTime.parse(created_at)
    )

    private fun Customer.toEntity(cid: String) = CustomerEntity(
        id = id.toString(),
        companyId = cid,
        name = name,
        phone = phone,
        address = address,
        notes = notes,
        public_code = publicCode,
        created_at = createdAt.toString()
    )

    private fun SupabaseCustomer.toEntity(cid: String) = CustomerEntity(
        id = id,
        companyId = company_id ?: cid,
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