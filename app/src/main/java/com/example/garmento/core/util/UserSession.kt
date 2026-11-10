package com.example.garmento.core.util

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.example.garmento.core.domain.model.UserRole
import kotlinx.coroutines.flow.first

object UserSession {
    var userId: String? = null
        private set

    var email: String? = null
        private set

    var companyId: String? = null
        private set

    var role: UserRole? = null
        private set

    /** Alias used by repositories/DAOs that prefer an explicit "current" name. */
    val currentCompanyId: String? get() = companyId
    val currentUserId: String? get() = userId

    val isAdmin: Boolean get() = role == UserRole.ADMIN
    val isStaff: Boolean get() = role == UserRole.STAFF

    fun setUserId(newUserId: String?) {
        userId = newUserId
    }

    fun setEmail(newEmail: String?) {
        email = newEmail
    }

    fun setRole(newRole: UserRole?) {
        role = newRole
    }

    fun setCompany(newCompanyId: String?, newRole: UserRole?) {
        companyId = newCompanyId
        role = newRole
    }

    fun clear() {
        userId = null
        email = null
        companyId = null
        role = null
    }

    /** Hydrate in-memory state from DataStore preferences. */
    fun load(prefs: Preferences) {
        userId = prefs[Constants.USER_ID_KEY]
        email = prefs[Constants.EMAIL_KEY]
        companyId = prefs[Constants.COMPANY_ID_KEY]
        role = prefs[Constants.ROLE_KEY]?.let { runCatching { UserRole.valueOf(it) }.getOrNull() }
    }

    fun hydrate(prefs: Preferences) = load(prefs)

    /** Persist current in-memory state to DataStore preferences. */
    suspend fun save(dataStore: DataStore<Preferences>) {
        dataStore.edit { prefs ->
            if (userId != null) prefs[Constants.USER_ID_KEY] = userId!! else prefs.remove(Constants.USER_ID_KEY)
            if (email != null) prefs[Constants.EMAIL_KEY] = email!! else prefs.remove(Constants.EMAIL_KEY)
            if (companyId != null) prefs[Constants.COMPANY_ID_KEY] = companyId!! else prefs.remove(Constants.COMPANY_ID_KEY)
            if (role != null) prefs[Constants.ROLE_KEY] = role!!.name else prefs.remove(Constants.ROLE_KEY)
        }
    }

    /** Load in-memory state from a DataStore. */
    suspend fun load(dataStore: DataStore<Preferences>) {
        load(dataStore.data.first())
    }
}
