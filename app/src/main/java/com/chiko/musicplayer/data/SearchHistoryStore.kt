package com.chiko.musicplayer.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SearchHistoryStore(context: Context, private val key: String) {

    private val prefs = context.applicationContext.getSharedPreferences(
        "search_history",
        Context.MODE_PRIVATE,
    )

    private val _history = MutableStateFlow(load())
    val history: StateFlow<List<String>> = _history.asStateFlow()

    fun add(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        val updated = (listOf(trimmed) + _history.value.filter { it != trimmed }).take(MAX)
        _history.value = updated
        prefs.edit().putString(key, updated.joinToString(SEPARATOR)).apply()
    }

    fun remove(query: String) {
        val updated = _history.value.filter { it != query }
        _history.value = updated
        prefs.edit().putString(key, updated.joinToString(SEPARATOR)).apply()
    }

    fun clear() {
        _history.value = emptyList()
        prefs.edit().remove(key).apply()
    }

    private fun load(): List<String> {
        val raw = prefs.getString(key, null) ?: return emptyList()
        return raw.split(SEPARATOR).filter { it.isNotBlank() }
    }

    private companion object {
        const val MAX = 20
        const val SEPARATOR = ""
    }
}
