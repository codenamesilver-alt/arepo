package com.example.garmento.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.garmento.core.domain.model.Subscription
import com.example.garmento.feature.customers.CustomerRepository
import com.example.garmento.feature.customers.SubscriptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExpiringSub(
    val subscriptionId: String,
    val customerId: String,
    val customerName: String,
    val customerPhone: String,
    val planName: String,
    val endDate: String
)

@HiltViewModel
class ExpiringSubscriptionsViewModel @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository,
    private val customerRepository: CustomerRepository,
    private val planRepository: com.example.garmento.feature.plans.PlanRepository
) : ViewModel() {
    private val _items = MutableStateFlow<List<ExpiringSub>>(emptyList())
    val items: StateFlow<List<ExpiringSub>> = _items.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            val subs = subscriptionRepository.getExpiringWithinDays(3)
            val result = subs.map { s ->
                val customer = customerRepository.getCustomerById(s.customer_id)
                val plan = planRepository.getPlanById(s.plan_id)
                ExpiringSub(
                    subscriptionId = s.id,
                    customerId = s.customer_id,
                    customerName = customer?.name ?: "Unknown",
                    customerPhone = customer?.phone ?: "",
                    planName = plan?.name ?: "Plan",
                    endDate = s.end_date
                )
            }
            _items.value = result
        }
    }
}