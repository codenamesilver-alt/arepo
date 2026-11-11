package com.example.garmento.feature.dashboard

import com.example.garmento.core.util.DateParsing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.garmento.core.data.local.dao.CustomerDao
import com.example.garmento.core.data.local.dao.SubscriptionDao
import com.example.garmento.core.domain.model.Subscription
import com.example.garmento.core.domain.model.SubscriptionStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

data class ActiveSubscriptionRow(
    val customerId: UUID,
    val customerName: String,
    val phone: String,
    val subscription: Subscription
) {
    val daysLeft: Long get() = subscription.daysRemaining
    val clothesLeft: Int get() = subscription.remainingClothes
    val isOverQuota: Boolean get() = subscription.remainingClothes < 0
}

data class ActiveSubscriptionsUiState(
    val rows: List<ActiveSubscriptionRow> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ActiveSubscriptionsViewModel @Inject constructor(
    private val subscriptionDao: SubscriptionDao,
    private val customerDao: CustomerDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(ActiveSubscriptionsUiState())
    val uiState: StateFlow<ActiveSubscriptionsUiState> = _uiState.asStateFlow()

    init {
        loadActiveSubscriptions()
    }

    fun loadActiveSubscriptions() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val customers = customerDao.getAllCustomersOnce().associateBy { it.id }
                val rows = subscriptionDao.getAllActiveSubscriptions()
                    .filter { it.end_date >= LocalDate.now().toString() }
                    .map { sub ->
                        val customer = customers[sub.customer_id]
                        ActiveSubscriptionRow(
                            customerId = UUID.fromString(sub.customer_id),
                            customerName = customer?.name ?: "Unknown customer",
                            phone = customer?.phone ?: "",
                            subscription = sub.toDomain()
                        )
                    }
                    .sortedBy { it.daysLeft }
                _uiState.update { it.copy(rows = rows, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private fun com.example.garmento.core.data.local.entity.SubscriptionEntity.toDomain() = Subscription(
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