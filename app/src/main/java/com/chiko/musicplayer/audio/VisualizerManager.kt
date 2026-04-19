package com.chiko.musicplayer.audio

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.log10

object VisualizerManager {
    private const val NUM_BANDS = 24

    private val _level = MutableStateFlow(0f)
    val level: StateFlow<Float> = _level.asStateFlow()

    private val _bands = MutableStateFlow(FloatArray(NUM_BANDS))
    val bands: StateFlow<FloatArray> = _bands.asStateFlow()

    @Volatile private var currentLevel = 0f
    private val bandSmoothed = FloatArray(NUM_BANDS)

    fun publishRms(rms: Float) {
        val safe = rms.coerceAtLeast(1e-5f)
        val db = 20f * log10(safe)
        val normalized = ((db + 50f) / 50f).coerceIn(0f, 1f)
        currentLevel = if (normalized > currentLevel) normalized else currentLevel * 0.85f
        _level.value = currentLevel.coerceIn(0f, 1f)
    }

    fun publishBands(rawBands: FloatArray) {
        val n = bandSmoothed.size.coerceAtMost(rawBands.size)
        for (i in 0 until n) {
            val safe = rawBands[i].coerceAtLeast(1e-5f)
            val db = 20f * log10(safe)
            val norm = ((db + 40f) / 40f).coerceIn(0f, 1f)
            bandSmoothed[i] = if (norm > bandSmoothed[i]) norm else bandSmoothed[i] * 0.80f
        }
        _bands.value = bandSmoothed.copyOf()
    }

    fun reset() {
        currentLevel = 0f
        bandSmoothed.fill(0f)
        _level.value = 0f
        _bands.value = FloatArray(NUM_BANDS)
    }
}
