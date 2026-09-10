package com.example.garmento.feature.customers

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.garmento.core.domain.model.Customer
import com.example.garmento.core.domain.model.Subscription
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CustomersUiState(
    val customers: List<Customer> = emptyList(),
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val error: String? = null
)

data class CustomerDetailUiState(
    val customer: Customer? = null,
    val activeSubscription: Subscription? = null,
    val subscriptions: List<Subscription> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CustomersViewModel @Inject constructor(
    private val customerRepository: CustomerRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val customerId: String? = savedStateHandle.get<String>("customerId")

    private val _uiState = MutableStateFlow(CustomersUiState())
    val uiState: StateFlow<CustomersUiState> = _uiState.asStateFlow()

    private val _detailState = MutableStateFlow(CustomerDetailUiState())
    val detailState: StateFlow<CustomerDetailUiState> = _detailState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")

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

    init {
        loadCustomers()
        customerId?.let { loadCustomerDetail(it) }
    }

    fun loadCustomers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                customerRepository.syncFromRemote()
            } catch (_: Exception) { }
            _uiState.update { it.copy(isLoading = false) }
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
                    subscriptions = subs,
                    isLoading = false
                )
            }
        }
    }

    suspend fun getCustomer(customerId: String): Customer? {
        return customerRepository.getCustomerById(customerId)
    }
}
