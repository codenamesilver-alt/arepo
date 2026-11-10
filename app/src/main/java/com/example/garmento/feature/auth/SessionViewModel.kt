package com.example.garmento.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.garmento.core.domain.model.UserRole
import com.example.garmento.core.util.UserSession
import com.example.garmento.feature.company.Company
import com.example.garmento.feature.company.CompanyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SessionDestination {
    LOGGED_OUT,
    NEEDS_COMPANY,
    COMPANY_PICKER,
    LOGGED_IN
}

data class SessionUiState(
    val destination: SessionDestination? = null
)

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val companyRepository: CompanyRepository
) : ViewModel() {

    constructor(authRepository: AuthRepository) : this(
        authRepository,
        object : CompanyRepository {
            override suspend fun getMyCompanies(): List<Company> = emptyList()
            override suspend fun createCompany(name: String): Company =
                throw UnsupportedOperationException("No company repository")
        }
    )

    constructor(companyRepository: CompanyRepository, authRepository: AuthRepository) : this(
        authRepository,
        companyRepository
    )

    private val _destination = MutableStateFlow<SessionDestination?>(null)
    val destination: StateFlow<SessionDestination?> = _destination.asStateFlow()

    /** Alias for callers/tests that prefer a different name. */
    val sessionDestination: StateFlow<SessionDestination?> get() = destination

    /** Alias kept for backward compatibility with the previous boolean API. */
    val sessionState: StateFlow<Boolean?> = destination.map { d ->
        when (d) {
            null -> null
            SessionDestination.LOGGED_OUT -> false
            else -> true
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Alias exposing a small UI state wrapper. */
    val uiState: StateFlow<SessionUiState> = destination.map { SessionUiState(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, SessionUiState(null))

    init {
        viewModelScope.launch { resolve() }
    }

    private suspend fun resolve() {
        // Hydrate role from persisted preferences when available.
        runCatching {
            authRepository.storedRole.first()?.let { roleName ->
                UserSession.setRole(
                    runCatching { UserRole.valueOf(roleName) }.getOrNull()
                )
            }
        }

        val session = authRepository.currentSession()
        val loggedIn = session != null || runCatching { authRepository.isLoggedIn.first() }.getOrDefault(false)
        if (!loggedIn) {
            UserSession.clear()
            _destination.value = SessionDestination.LOGGED_OUT
            return
        }

        session?.user?.let { u ->
            UserSession.setUserId(u.id)
            UserSession.setEmail(u.email)
        }

        val companies = runCatching { companyRepository.getMyCompanies() }.getOrDefault(emptyList())
        when {
            companies.isEmpty() -> _destination.value = SessionDestination.NEEDS_COMPANY
            companies.size == 1 -> {
                val c = companies.first()
                val role = if (c.role == "owner") UserRole.ADMIN else UserRole.STAFF
                UserSession.setCompany(c.id, role)
                _destination.value = SessionDestination.LOGGED_IN
            }
            else -> _destination.value = SessionDestination.COMPANY_PICKER
        }
    }

    /** Re-run resolution (e.g. after login). */
    fun refresh() {
        viewModelScope.launch { resolve() }
    }
}