package com.chiko.musicplayer.data

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.chiko.musicplayer.ui.SortBy
import com.chiko.musicplayer.ui.ViewMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsStore private constructor(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _sortBy = MutableStateFlow(loadSort())
    val sortBy: StateFlow<SortBy> = _sortBy.asStateFlow()

    private val _viewMode = MutableStateFlow(loadViewMode())
    val viewMode: StateFlow<ViewMode> = _viewMode.asStateFlow()

    private val _youtubeGridView = MutableStateFlow(prefs.getBoolean(KEY_YT_GRID, true))
    val youtubeGridView: StateFlow<Boolean> = _youtubeGridView.asStateFlow()

    private val _accentColor = MutableStateFlow(prefs.getInt(KEY_ACCENT, Color(0xFFEDEDED).toArgb()))
    val accentColor: StateFlow<Int> = _accentColor.asStateFlow()

    private val _backgroundColor = MutableStateFlow(prefs.getInt(KEY_BG, Color.Black.toArgb()))
    val backgroundColor: StateFlow<Int> = _backgroundColor.asStateFlow()

    private val _dynamicFromArt = MutableStateFlow(prefs.getBoolean(KEY_DYNAMIC, false))
    val dynamicFromArt: StateFlow<Boolean> = _dynamicFromArt.asStateFlow()

    fun setSortBy(v: SortBy) {
        _sortBy.value = v
        prefs.edit().putString(KEY_SORT, v.name).apply()
    }

    fun setViewMode(v: ViewMode) {
        _viewMode.value = v
        prefs.edit().putString(KEY_VIEW, v.name).apply()
    }

    fun setYoutubeGridView(v: Boolean) {
        _youtubeGridView.value = v
        prefs.edit().putBoolean(KEY_YT_GRID, v).apply()
    }

    fun setAccentColor(argb: Int) {
        _accentColor.value = argb
        prefs.edit().putInt(KEY_ACCENT, argb).apply()
    }

    fun setBackgroundColor(argb: Int) {
        _backgroundColor.value = argb
        prefs.edit().putInt(KEY_BG, argb).apply()
    }

    fun setDynamicFromArt(v: Boolean) {
        _dynamicFromArt.value = v
        prefs.edit().putBoolean(KEY_DYNAMIC, v).apply()
    }

    private fun loadSort(): SortBy {
        val name = prefs.getString(KEY_SORT, null) ?: return SortBy.Title
        return runCatching { SortBy.valueOf(name) }.getOrDefault(SortBy.Title)
    }

    private fun loadViewMode(): ViewMode {
        val name = prefs.getString(KEY_VIEW, null) ?: return ViewMode.List
        return runCatching { ViewMode.valueOf(name) }.getOrDefault(ViewMode.List)
    }

    companion object {
        private const val PREFS = "resonance_settings"
        private const val KEY_SORT = "sort"
        private const val KEY_VIEW = "view"
        private const val KEY_YT_GRID = "yt_grid_view"
        private const val KEY_ACCENT = "accent"
        private const val KEY_BG = "bg"
        private const val KEY_DYNAMIC = "dynamic"

        @Volatile private var INSTANCE: SettingsStore? = null

        fun getInstance(context: Context): SettingsStore {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SettingsStore(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
