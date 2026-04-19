package com.chiko.musicplayer.audio

import android.content.Context
import android.content.SharedPreferences
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class EqBand(val index: Short, val centerHz: Int)

object EqualizerManager {
    private const val PREFS = "eq_prefs"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_PRESET = "preset"
    private const val KEY_BAND = "band_"
    private const val KEY_BASS = "bass"
    const val CUSTOM_PRESET: Short = -1

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var sessionId: Int = 0
    private var prefs: SharedPreferences? = null

    private val _isAvailable = MutableStateFlow(false)
    val isAvailable: StateFlow<Boolean> = _isAvailable.asStateFlow()

    private val _enabled = MutableStateFlow(false)
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    private val _bands = MutableStateFlow<List<EqBand>>(emptyList())
    val bands: StateFlow<List<EqBand>> = _bands.asStateFlow()

    private val _bandLevels = MutableStateFlow<List<Short>>(emptyList())
    val bandLevels: StateFlow<List<Short>> = _bandLevels.asStateFlow()

    private val _minLevel = MutableStateFlow<Short>(-1500)
    val minLevel: StateFlow<Short> = _minLevel.asStateFlow()

    private val _maxLevel = MutableStateFlow<Short>(1500)
    val maxLevel: StateFlow<Short> = _maxLevel.asStateFlow()

    private val _presets = MutableStateFlow<List<String>>(emptyList())
    val presets: StateFlow<List<String>> = _presets.asStateFlow()

    private val _currentPreset = MutableStateFlow(CUSTOM_PRESET)
    val currentPreset: StateFlow<Short> = _currentPreset.asStateFlow()

    private val _bassStrength = MutableStateFlow<Short>(0)
    val bassStrength: StateFlow<Short> = _bassStrength.asStateFlow()

    fun init(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs = p
        _enabled.value = p.getBoolean(KEY_ENABLED, false)
        _bassStrength.value = p.getInt(KEY_BASS, 0).toShort()
        _currentPreset.value = p.getInt(KEY_PRESET, CUSTOM_PRESET.toInt()).toShort()
    }

    fun attach(audioSessionId: Int) {
        if (audioSessionId == 0) return
        if (audioSessionId == sessionId && equalizer != null) return
        detach()
        sessionId = audioSessionId
        try {
            val eq = Equalizer(0, audioSessionId)
            equalizer = eq
            bassBoost = try { BassBoost(0, audioSessionId) } catch (_: Throwable) { null }

            val bandCount = eq.numberOfBands.toInt()
            _bands.value = (0 until bandCount).map { i ->
                val idx = i.toShort()
                EqBand(idx, eq.getCenterFreq(idx) / 1000)
            }
            val range = eq.bandLevelRange
            _minLevel.value = range[0]
            _maxLevel.value = range[1]
            _presets.value = (0 until eq.numberOfPresets.toInt()).map { eq.getPresetName(it.toShort()) }

            val p = prefs
            val saved = (0 until bandCount).map { i ->
                p?.getInt(KEY_BAND + i, 0)?.toShort() ?: 0
            }
            _bandLevels.value = saved

            _isAvailable.value = true
            applyToHardware()
        } catch (_: Throwable) {
            _isAvailable.value = false
        }
    }

    fun detach() {
        try { equalizer?.release() } catch (_: Throwable) {}
        try { bassBoost?.release() } catch (_: Throwable) {}
        equalizer = null
        bassBoost = null
        sessionId = 0
    }

    fun setEnabled(on: Boolean) {
        _enabled.value = on
        prefs?.edit()?.putBoolean(KEY_ENABLED, on)?.apply()
        applyToHardware()
    }

    fun setBandLevel(bandIndex: Int, level: Short) {
        val list = _bandLevels.value.toMutableList()
        if (bandIndex !in list.indices) return
        list[bandIndex] = level
        _bandLevels.value = list
        _currentPreset.value = CUSTOM_PRESET
        prefs?.edit()
            ?.putInt(KEY_BAND + bandIndex, level.toInt())
            ?.putInt(KEY_PRESET, CUSTOM_PRESET.toInt())
            ?.apply()
        if (_enabled.value) {
            try { equalizer?.setBandLevel(bandIndex.toShort(), level) } catch (_: Throwable) {}
        }
    }

    fun applyPreset(preset: Short) {
        val eq = equalizer ?: return
        try {
            eq.usePreset(preset)
            val newLevels = (0 until eq.numberOfBands.toInt()).map { eq.getBandLevel(it.toShort()) }
            _bandLevels.value = newLevels
            _currentPreset.value = preset
            val editor = prefs?.edit()
            editor?.putInt(KEY_PRESET, preset.toInt())
            newLevels.forEachIndexed { i, l -> editor?.putInt(KEY_BAND + i, l.toInt()) }
            editor?.apply()
        } catch (_: Throwable) {}
    }

    fun setBassStrength(strength: Short) {
        _bassStrength.value = strength
        prefs?.edit()?.putInt(KEY_BASS, strength.toInt())?.apply()
        try {
            bassBoost?.setStrength(strength)
            bassBoost?.enabled = _enabled.value && strength > 0
        } catch (_: Throwable) {}
    }

    fun resetFlat() {
        val zero = List(_bands.value.size) { 0.toShort() }
        _bandLevels.value = zero
        _currentPreset.value = CUSTOM_PRESET
        val editor = prefs?.edit()
        zero.forEachIndexed { i, l -> editor?.putInt(KEY_BAND + i, l.toInt()) }
        editor?.putInt(KEY_PRESET, CUSTOM_PRESET.toInt())?.apply()
        if (_enabled.value) {
            zero.forEachIndexed { i, l ->
                try { equalizer?.setBandLevel(i.toShort(), l) } catch (_: Throwable) {}
            }
        }
    }

    private fun applyToHardware() {
        try {
            equalizer?.enabled = _enabled.value
            bassBoost?.enabled = _enabled.value && _bassStrength.value > 0
            if (_enabled.value) {
                _bandLevels.value.forEachIndexed { i, l ->
                    try { equalizer?.setBandLevel(i.toShort(), l) } catch (_: Throwable) {}
                }
                try { bassBoost?.setStrength(_bassStrength.value) } catch (_: Throwable) {}
            }
        } catch (_: Throwable) {}
    }
}
