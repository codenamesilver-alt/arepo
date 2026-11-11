package com.example.garmento.feature.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
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
import kotlinx.coroutines.flow.combine
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
    val destination: SessionDestination? = null,
    val error: String? = null
)

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val companyRepository: CompanyRepository,
    private val dataStore: DataStore<Preferences>?
) : ViewModel() {

    constructor(authRepository: AuthRepository) : this(
        authRepository,
        object : CompanyRepository {
            override suspend fun getMyCompanies(): List<Company> = emptyList()
            override suspend fun createCompany(name: String): Company =
                throw UnsupportedOperationException("No company repository")
        },
        null
    )

    constructor(companyRepository: CompanyRepository, authRepository: AuthRepository) : this(
        authRepository,
        companyRepository,
        null
    )

    private val _destination = MutableStateFlow<SessionDestination?>(null)
    val destination: StateFlow<SessionDestination?> = _destination.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

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
    val uiState: StateFlow<SessionUiState> = kotlinx.coroutines.flow.combine(destination, _error) { d, e ->
        SessionUiState(d, e)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SessionUiState(null))

    init {
        viewModelScope.launch { resolve() }
    }

    private suspend fun resolve() {
        _error.value = null
        // Hydrate session from persisted preferences when available.
        dataStore?.let { runCatching { UserSession.load(it) } }

        runCatching {
            authRepository.storedRole.first()?.let { roleName ->
                UserSession.setRole(
                    runCatching { UserRole.valueOf(roleName) }.getOrNull()
                )
            }
        }

        val session = authRepository.currentSession()
        val withinWindow = runCatching { authRepository.isLoggedIn.first() }.getOrDefault(false)
        val loggedIn = session != null || withinWindow
        if (!loggedIn) {
            UserSession.clear()
            _destination.value = SessionDestination.LOGGED_OUT
            return
        }

        session?.user?.let { u ->
            UserSession.setUserId(u.id)
            UserSession.setEmail(u.email)
        }

        // If we are within the 7-day window but Supabase session is gone,
        // restore from persisted state without requiring a network round-trip.
        if (session == null && withinWindow) {
            val storedCompany = UserSession.companyId
            val storedRole = UserSession.role
            if (storedCompany != null) {
                if (storedRole == null) UserSession.setRole(UserRole.STAFF)
                _destination.value = SessionDestination.LOGGED_IN
            } else {
                _destination.value = SessionDestination.NEEDS_COMPANY
            }
            return
        }

        val companiesResult = runCatching { companyRepository.getMyCompanies() }
        if (companiesResult.isFailure) {
            android.util.Log.e("SessionViewModel", "getMyCompanies failed", companiesResult.exceptionOrNull())
            _error.value = companiesResult.exceptionOrNull()?.message ?: "Failed to load companies"
            return
        }
        val companies = companiesResult.getOrNull() ?: emptyList()
        when {
            companies.isEmpty() -> {
                _destination.value = SessionDestination.NEEDS_COMPANY
            }
            companies.size == 1 -> {
                val c = companies.first()
                val role = if (c.role == "owner") UserRole.ADMIN else UserRole.STAFF
                UserSession.setCompany(c.id, role)
                dataStore?.let { runCatching { UserSession.save(it) } }
                _destination.value = SessionDestination.LOGGED_IN
            }
            else -> {
                _destination.value = SessionDestination.COMPANY_PICKER
            }
        }
    }

    /** Re-run resolution (e.g. after login). */
    fun refresh() {
        viewModelScope.launch { resolve() }
    }
}