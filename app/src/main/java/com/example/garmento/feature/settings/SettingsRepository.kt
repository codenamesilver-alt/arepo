package com.example.garmento.feature.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.example.garmento.core.util.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    val gracePeriodDays: Flow<Int> = dataStore.data.map { prefs ->
        prefs[Constants.GRACE_PERIOD_KEY] ?: 0
    }

    val isDarkMode: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Constants.DARK_MODE_KEY] ?: false
    }

    val username: Flow<String> = dataStore.data.map { prefs ->
        prefs[Constants.USERNAME_KEY] ?: ""
    }

    suspend fun setGracePeriodDays(days: Int) {
        dataStore.edit { prefs ->
            prefs[Constants.GRACE_PERIOD_KEY] = days
        }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Constants.DARK_MODE_KEY] = enabled
        }
    }

    suspend fun logout() {
        dataStore.edit { prefs ->
            prefs[Constants.SESSION_KEY] = false
            prefs[Constants.USERNAME_KEY] = ""
        }
    }
}
