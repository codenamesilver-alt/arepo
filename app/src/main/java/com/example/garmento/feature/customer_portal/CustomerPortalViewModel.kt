package com.example.garmento.feature.customer_portal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.garmento.core.domain.model.Customer
import com.example.garmento.core.domain.model.Subscription
import com.example.garmento.core.domain.model.SubscriptionPlan
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class CustomerPortalUiState(
    val customer: Customer? = null,
    val subscription: Subscription? = null,
    val plan: SubscriptionPlan? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class CustomerPortalViewModel @Inject constructor(
    private val portalRepository: CustomerPortalRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CustomerPortalUiState())
    val uiState: StateFlow<CustomerPortalUiState> = _uiState.asStateFlow()

    fun loadCustomer(publicCode: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val customer = portalRepository.getByPublicCode(publicCode)
                if (customer != null) {
                    val subscription = portalRepository.getActiveSubscription(customer.id)
                    val plan = subscription?.let { portalRepository.getPlan(it.planId) }
                    _uiState.update {
                        it.copy(
                            customer = customer,
                            subscription = subscription,
                            plan = plan,
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(error = "Customer not found", isLoading = false)
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "Unknown error", isLoading = false)
                }
            }
        }
    }
}
