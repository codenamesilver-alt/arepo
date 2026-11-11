package com.example.garmento.feature.customers

import com.example.garmento.core.util.DateParsing

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.garmento.core.data.local.dao.PlanDao
import com.example.garmento.core.data.local.dao.SubscriptionDao
import com.example.garmento.core.data.local.entity.SubscriptionEntity
import com.example.garmento.core.domain.model.Customer
import com.example.garmento.core.domain.model.Subscription
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class CustomersUiState(
    val customers: List<Customer> = emptyList(),
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val error: String? = null
)

data class CustomerListRow(
    val customer: Customer,
    val planName: String? = null,
    val daysLeft: Long? = null,
    val clothesLeft: Int? = null,
    val status: CustomerListStatus = CustomerListStatus.NO_PLAN
)

enum class CustomerListStatus {
    ACTIVE, OVER_QUOTA, NO_PLAN
}

data class CustomerDetailUiState(
    val customer: Customer? = null,
    val activeSubscription: Subscription? = null,
    val selectedSubscription: Subscription? = null,
    val subscriptions: List<Subscription> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CustomersViewModel @Inject constructor(
    private val customerRepository: CustomerRepository,
    private val subscriptionRepository: com.example.garmento.feature.customers.SubscriptionRepository,
    private val subscriptionDao: SubscriptionDao,
    private val planDao: PlanDao,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val customerId: String? = savedStateHandle.get<String>("customerId")

    private val _uiState = MutableStateFlow(CustomersUiState())
    val uiState: StateFlow<CustomersUiState> = _uiState.asStateFlow()

    private val _detailState = MutableStateFlow(CustomerDetailUiState())
    val detailState: StateFlow<CustomerDetailUiState> = _detailState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")

    private val _subscribedIds = MutableStateFlow<Set<String>>(emptySet())
    val subscribedCustomerIds: StateFlow<Set<String>> = _subscribedIds.asStateFlow()

    private fun refreshSubscribed() {
        viewModelScope.launch {
            try {
                try { subscriptionRepository.syncFromRemote() } catch (_: Exception) { }
                val cid = com.example.garmento.core.util.UserSession.currentCompanyId?.takeIf { it.isNotBlank() } ?: ""
                val active = subscriptionDao.getAllActiveSubscriptions(cid)
                android.util.Log.d("CustomersVM", "active subs=${active.size} ids=${active.map { it.customer_id }} cid=$cid")
                _subscribedIds.value = active.map { it.customer_id }.toSet()
            } catch (e: Exception) {
                android.util.Log.e("CustomersVM", "refreshSubscribed failed", e)
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val customers: StateFlow<List<Customer>> = _searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) {
                customerRepository.getAllCustomers()
            } else {
                customerRepository.searchCustomers(query)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val customerRows: StateFlow<List<CustomerListRow>> = customers
        .flatMapLatest { list ->
            flow { emit(buildRows(list)) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private suspend fun buildRows(customers: List<Customer>): List<CustomerListRow> {
        if (customers.isEmpty()) return emptyList()
        val today = LocalDate.now()
        val activeSubs = subscriptionDao.getAllSubscriptions()
            .filter { it.status == "active" && it.end_date >= today.toString() }
            .groupBy { it.customer_id }
            .mapValues { (_, subs) -> subs.maxByOrNull { it.end_date } }
        val planNames = planDao.getAllPlansOnce()
            .associateBy { it.id }
            .mapValues { it.value.name }
        return customers.map { customer ->
            val sub = activeSubs[customer.id.toString()]
            if (sub == null) {
                CustomerListRow(customer = customer, status = CustomerListStatus.NO_PLAN)
            } else {
                val clothesLeft = sub.clothes_quota - sub.clothes_used
                CustomerListRow(
                    customer = customer,
                    planName = planNames[sub.plan_id],
                    daysLeft = sub.daysRemaining(),
                    clothesLeft = clothesLeft,
                    status = if (clothesLeft < 0) CustomerListStatus.OVER_QUOTA else CustomerListStatus.ACTIVE
                )
            }
        }
    }

    private fun SubscriptionEntity.daysRemaining(): Long =
        java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), DateParsing.parseLocalDate(end_date))

    init {
        loadCustomers()
        refreshSubscribed()
        customerId?.let { loadCustomerDetail(it) }
    }

    fun loadCustomers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                customerRepository.syncFromRemote()
            } catch (_: Exception) { }
            _uiState.update { it.copy(isLoading = false) }
            refreshSubscribed()
        }
    }

    fun deleteCustomer(id: String) {
        viewModelScope.launch {
            try {
                customerRepository.deleteCustomer(id)
            } catch (_: Exception) { }
            loadCustomers()
        }
    }

    fun revokeSubscription(customerId: String) {
        viewModelScope.launch {
            try {
                subscriptionRepository.revokeSubscription(customerId)
            } catch (_: Exception) { }
            refreshSubscribed()
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        _searchQuery.value = query
    }

    fun loadCustomerDetail(customerId: String) {
        viewModelScope.launch {
            _detailState.update { it.copy(isLoading = true) }
            val customer = customerRepository.getCustomerById(customerId)
            val activeSub = customerRepository.getActiveSubscription(customerId)
            val subs = customerRepository.getSubscriptions(customerId)
            _detailState.update {
                it.copy(
                    customer = customer,
                    activeSubscription = activeSub,
                    selectedSubscription = activeSub ?: subs.maxByOrNull { it.endDate },
                    subscriptions = subs,
                    isLoading = false
                )
            }
        }
    }

    fun selectDetailSubscription(subscriptionId: String) {
        val sub = _detailState.value.subscriptions.find { it.id.toString() == subscriptionId }
        _detailState.update { it.copy(selectedSubscription = sub) }
    }

    suspend fun getCustomer(customerId: String): Customer? {
        return customerRepository.getCustomerById(customerId)
    }
}
