package com.example.garmento.feature.expenses

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

private val Context.expensesDataStore by preferencesDataStore(name = "expenses_store")

data class ExpenseHead(val id: String = java.util.UUID.randomUUID().toString(), val name: String)
data class ExpenseEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val head: String,
    val amount: String,
    val date: String,
    val notes: String
)

object ExpensesStore {
    private val _heads = MutableStateFlow<List<ExpenseHead>>(emptyList())
    val heads: StateFlow<List<ExpenseHead>> = _heads.asStateFlow()

    private val _expenses = MutableStateFlow<List<ExpenseEntry>>(emptyList())
    val expenses: StateFlow<List<ExpenseEntry>> = _expenses.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var initialized = false
    private var appContext: Context? = null

    fun init(context: Context) {
        if (initialized) return
        initialized = true
        appContext = context.applicationContext
        scope.launch {
            val prefs = appContext!!.expensesDataStore.data.first()
            val headsStored = prefs[KEY_HEADS]
            if (headsStored != null) {
                val list = headsStored.lines().map { it.trim() }.filter { it.isNotBlank() }.map { ExpenseHead(name = it) }
                if (list.isNotEmpty()) _heads.value = list
            }
            val expStored = prefs[KEY_EXPENSES]
            if (expStored != null) {
                val list = expStored.lines().map { it.trim() }.filter { it.isNotBlank() }.mapNotNull { line ->
                    val parts = line.split("|")
                    if (parts.size >= 4) ExpenseEntry(id = parts[0], head = parts[1], amount = parts[2], date = parts[3], notes = parts.getOrElse(4) { "" }) else null
                }
                if (list.isNotEmpty()) _expenses.value = list
            }
        }
    }

    fun addHead(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        if (_heads.value.any { it.name.equals(trimmed, ignoreCase = true) }) return
        _heads.value = _heads.value + ExpenseHead(name = trimmed)
        persist()
    }

    fun removeHead(id: String) {
        _heads.value = _heads.value.filter { it.id != id }
        persist()
    }

    fun addExpense(head: String, amount: String, date: String, notes: String) {
        if (head.isBlank() || amount.isBlank()) return
        _expenses.value = _expenses.value + ExpenseEntry(head = head, amount = amount.trim(), date = date, notes = notes.trim())
        persist()
    }

    fun removeExpense(id: String) {
        _expenses.value = _expenses.value.filter { it.id != id }
        persist()
    }

    private fun persist() {
        scope.launch {
            val ctx = appContext ?: return@launch
            ctx.expensesDataStore.edit { prefs ->
                prefs[KEY_HEADS] = _heads.value.joinToString("\n") { it.name }
                prefs[KEY_EXPENSES] = _expenses.value.joinToString("\n") { "${it.id}|${it.head}|${it.amount}|${it.date}|${it.notes}" }
            }
        }
    }

    private val KEY_HEADS = stringPreferencesKey("expense_heads")
    private val KEY_EXPENSES = stringPreferencesKey("expenses_list")
}