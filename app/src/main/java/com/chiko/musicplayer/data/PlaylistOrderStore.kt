package com.chiko.musicplayer.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Persists per-folder song order (folderId -> ordered list of song IDs).
 * Stored as a single string in SharedPreferences:
 *   "folderId:id1,id2,id3|folderId2:id4,id5"
 */
class PlaylistOrderStore private constructor(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _orders = MutableStateFlow(load())
    val orders: StateFlow<Map<Long, List<Long>>> = _orders.asStateFlow()

    fun orderFor(folderId: Long): List<Long> = _orders.value[folderId].orEmpty()

    fun setOrder(folderId: Long, songIds: List<Long>) {
        val updated = _orders.value.toMutableMap().apply { put(folderId, songIds) }
        _orders.value = updated
        save(updated)
    }

    fun moveItem(folderId: Long, fromIndex: Int, toIndex: Int, fallback: List<Long>) {
        val current = (_orders.value[folderId] ?: fallback).toMutableList()
        if (fromIndex !in current.indices) return
        val target = toIndex.coerceIn(0, current.size - 1)
        if (fromIndex == target) return
        val item = current.removeAt(fromIndex)
        current.add(target, item)
        setOrder(folderId, current)
    }

    /** Drop entries no longer present and append any new IDs at the end. */
    fun reconcile(folderId: Long, currentIds: List<Long>): List<Long> {
        val saved = _orders.value[folderId].orEmpty()
        if (saved.isEmpty()) return currentIds
        val present = currentIds.toSet()
        val kept = saved.filter { it in present }
        val missing = currentIds.filter { it !in kept }
        val merged = kept + missing
        if (merged != saved) setOrder(folderId, merged)
        return merged
    }

    private fun load(): Map<Long, List<Long>> {
        val raw = prefs.getString(KEY, "").orEmpty()
        if (raw.isBlank()) return emptyMap()
        return raw.split('|').mapNotNull { entry ->
            val (idStr, listStr) = entry.split(':', limit = 2).let {
                if (it.size != 2) return@mapNotNull null
                it[0] to it[1]
            }
            val folderId = idStr.toLongOrNull() ?: return@mapNotNull null
            val ids = listStr.split(',').mapNotNull { it.toLongOrNull() }
            folderId to ids
        }.toMap()
    }

    private fun save(map: Map<Long, List<Long>>) {
        val raw = map.entries.joinToString("|") { (id, ids) ->
            "$id:${ids.joinToString(",")}"
        }
        prefs.edit().putString(KEY, raw).apply()
    }

    companion object {
        private const val PREFS = "resonance_playlist_order"
        private const val KEY = "orders"

        @Volatile private var INSTANCE: PlaylistOrderStore? = null

        fun getInstance(context: Context): PlaylistOrderStore {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PlaylistOrderStore(context.applicationContext)
                    .also { INSTANCE = it }
            }
        }
    }
}
