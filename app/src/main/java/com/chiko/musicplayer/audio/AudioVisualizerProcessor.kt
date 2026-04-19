package com.chiko.musicplayer.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.pow
import kotlin.math.sqrt

@UnstableApi
class AudioVisualizerProcessor : BaseAudioProcessor() {

    private val fft = Fft(FFT_SIZE)
    private val fftAccum = FloatArray(FFT_SIZE)
    private var fftPos = 0
    private val rawBands = FloatArray(NUM_BANDS)

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        return when (inputAudioFormat.encoding) {
            C.ENCODING_PCM_16BIT, C.ENCODING_PCM_FLOAT -> inputAudioFormat
            else -> AudioProcessor.AudioFormat.NOT_SET
        }
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        when (inputAudioFormat.encoding) {
            C.ENCODING_PCM_16BIT -> process16Bit(inputBuffer)
            C.ENCODING_PCM_FLOAT -> processFloat(inputBuffer)
        }

        val outputBuffer = replaceOutputBuffer(remaining)
        outputBuffer.put(inputBuffer)
        outputBuffer.flip()
    }

    private fun process16Bit(buffer: ByteBuffer) {
        val view = buffer.duplicate().order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
        val channels = inputAudioFormat.channelCount.coerceAtLeast(1)
        val totalSamples = view.remaining()
        val frameCount = totalSamples / channels
        if (frameCount == 0) return

        val n = frameCount.coerceAtMost(MAX_FRAMES)
        var sumSquares = 0.0
        for (f in 0 until n) {
            var monoSum = 0f
            for (c in 0 until channels) {
                monoSum += view.get().toFloat() / 32768f
            }
            val mono = monoSum / channels
            sumSquares += mono * mono
            fftAccum[fftPos] = mono
            fftPos++
            if (fftPos >= FFT_SIZE) {
                computeAndPublishBands()
                fftPos = 0
            }
        }
        VisualizerManager.publishRms(sqrt(sumSquares / n).toFloat())
    }

    private fun processFloat(buffer: ByteBuffer) {
        val view = buffer.duplicate().order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer()
        val channels = inputAudioFormat.channelCount.coerceAtLeast(1)
        val totalSamples = view.remaining()
        val frameCount = totalSamples / channels
        if (frameCount == 0) return

        val n = frameCount.coerceAtMost(MAX_FRAMES)
        var sumSquares = 0.0
        for (f in 0 until n) {
            var monoSum = 0f
            for (c in 0 until channels) {
                monoSum += view.get()
            }
            val mono = monoSum / channels
            sumSquares += mono * mono
            fftAccum[fftPos] = mono
            fftPos++
            if (fftPos >= FFT_SIZE) {
                computeAndPublishBands()
                fftPos = 0
            }
        }
        VisualizerManager.publishRms(sqrt(sumSquares / n).toFloat())
    }

    private fun computeAndPublishBands() {
        val mags = fft.magnitudes(fftAccum)
        val bins = mags.size
        val minBin = 1f
        val maxBin = bins.toFloat()
        val ratio = maxBin / minBin
        for (b in 0 until NUM_BANDS) {
            val startBin = (minBin * ratio.pow(b.toFloat() / NUM_BANDS))
                .toInt().coerceIn(1, bins - 1)
            val endBinExclusive = (minBin * ratio.pow((b + 1).toFloat() / NUM_BANDS))
                .toInt().coerceIn(startBin + 1, bins)
            var sum = 0f
            var count = 0
            for (i in startBin until endBinExclusive) {
                sum += mags[i]
                count++
            }
            rawBands[b] = if (count > 0) sum / count else 0f
        }
        VisualizerManager.publishBands(rawBands)
    }

    override fun onFlush() {
        fftPos = 0
        VisualizerManager.reset()
    }

    override fun onReset() {
        fftPos = 0
        VisualizerManager.reset()
    }

    private companion object {
        const val FFT_SIZE = 1024
        const val NUM_BANDS = 24
        const val MAX_FRAMES = 8192
    }
}
