package com.example.garmento.feature.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.example.garmento.core.util.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    val isLoggedIn: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Constants.SESSION_KEY] ?: false
    }

    val getUsername: Flow<String> = dataStore.data.map { prefs ->
        prefs[Constants.USERNAME_KEY] ?: ""
    }

    suspend fun login(username: String, password: String): Boolean {
        return if (username == Constants.ADMIN_USERNAME && password == Constants.ADMIN_PASSWORD) {
            dataStore.edit { prefs ->
                prefs[Constants.SESSION_KEY] = true
                prefs[Constants.USERNAME_KEY] = username
            }
            true
        } else {
            false
        }
    }

    suspend fun logout() {
        dataStore.edit { prefs ->
            prefs[Constants.SESSION_KEY] = false
            prefs[Constants.USERNAME_KEY] = ""
        }
    }
}
