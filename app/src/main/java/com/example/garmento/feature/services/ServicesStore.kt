package com.example.garmento.feature.services

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val Context.servicesDataStore by preferencesDataStore(name = "services_store")

object ServicesStore {
    private val _services = MutableStateFlow(
        listOf("Wash Only", "Wash & Fold", "Wash & Iron", "Dry Clean", "Premium Care")
    )
    val services: StateFlow<List<String>> = _services.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        initialized = true
        appContext = context.applicationContext
        scope.launch {
            val prefs = appContext!!.servicesDataStore.data.first()
            val stored = prefs[KEY_SERVICES]
            if (stored != null) {
                val list = stored.split("\n").map { it.trim() }.filter { it.isNotBlank() }
                if (list.isNotEmpty()) _services.value = list
            }
        }
    }

    fun add(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        if (trimmed in _services.value) return
        _services.value = _services.value + trimmed
        persist()
    }

    fun remove(name: String) {
        _services.value = _services.value - name
        persist()
    }

    private fun persist() {
        scope.launch {
            val appCtx = appContext ?: return@launch
            appCtx.servicesDataStore.edit { prefs ->
                prefs[KEY_SERVICES] = _services.value.joinToString("\n")
            }
        }
    }

    private var appContext: Context? = null

    fun setContext(context: Context) {
        appContext = context.applicationContext
    }

    private val KEY_SERVICES = stringPreferencesKey("services_list")
}