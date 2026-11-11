package com.example.garmento.feature.items

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

private val Context.itemsDataStore by preferencesDataStore(name = "items_store")

data class ItemEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val service: String,
    val price: String
)

object ItemStore {
    private val _items = MutableStateFlow<List<ItemEntry>>(emptyList())
    val items: StateFlow<List<ItemEntry>> = _items.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var initialized = false
    private var appContext: Context? = null

    fun init(context: Context) {
        if (initialized) return
        initialized = true
        appContext = context.applicationContext
        scope.launch {
            val prefs = appContext!!.itemsDataStore.data.first()
            val stored = prefs[KEY_ITEMS]
            if (stored != null) {
                val list = stored.lines().map { it.trim() }.filter { it.isNotBlank() }.mapNotNull { line ->
                    val parts = line.split("|")
                    if (parts.size >= 3) ItemEntry(id = parts[0], name = parts[1], service = parts[2], price = parts.getOrElse(3) { "" }) else null
                }
                if (list.isNotEmpty()) _items.value = list
            }
        }
    }

    fun add(name: String, service: String, price: String) {
        if (name.isBlank()) return
        _items.value = _items.value + ItemEntry(name = name.trim(), service = service, price = price.trim())
        persist()
    }

    fun update(id: String, name: String, service: String, price: String) {
        _items.value = _items.value.map { e ->
            if (e.id == id) e.copy(name = name.trim(), service = service, price = price.trim()) else e
        }
        persist()
    }

    fun remove(id: String) {
        _items.value = _items.value.filter { it.id != id }
        persist()
    }

    private fun persist() {
        scope.launch {
            val ctx = appContext ?: return@launch
            ctx.itemsDataStore.edit { prefs ->
                prefs[KEY_ITEMS] = _items.value.joinToString("\n") { "${it.id}|${it.name}|${it.service}|${it.price}" }
            }
        }
    }

    private val KEY_ITEMS = stringPreferencesKey("items_list")
}