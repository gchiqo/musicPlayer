package com.chiko.musicplayer.audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class Fft(private val n: Int) {

    init {
        require(n > 0 && (n and (n - 1)) == 0) { "n must be a power of two" }
    }

    private val real = FloatArray(n)
    private val imag = FloatArray(n)
    private val mags = FloatArray(n / 2)
    private val cosTable = FloatArray(n / 2)
    private val sinTable = FloatArray(n / 2)
    private val window = FloatArray(n)

    init {
        for (i in 0 until n / 2) {
            cosTable[i] = cos(-2.0 * PI * i / n).toFloat()
            sinTable[i] = sin(-2.0 * PI * i / n).toFloat()
        }
        for (i in 0 until n) {
            window[i] = (0.5 * (1.0 - cos(2.0 * PI * i / (n - 1)))).toFloat()
        }
    }

    fun magnitudes(input: FloatArray): FloatArray {
        for (i in 0 until n) {
            real[i] = input[i] * window[i]
            imag[i] = 0f
        }
        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) {
                j = j xor bit
                bit = bit shr 1
            }
            j = j xor bit
            if (i < j) {
                var tmp = real[i]; real[i] = real[j]; real[j] = tmp
                tmp = imag[i]; imag[i] = imag[j]; imag[j] = tmp
            }
        }
        var size = 2
        while (size <= n) {
            val half = size / 2
            val step = n / size
            var i = 0
            while (i < n) {
                var k = 0
                for (idx in i until i + half) {
                    val cv = cosTable[k]
                    val sv = sinTable[k]
                    val rIdx = real[idx + half]
                    val iIdx = imag[idx + half]
                    val tpre = rIdx * cv - iIdx * sv
                    val tpim = rIdx * sv + iIdx * cv
                    real[idx + half] = real[idx] - tpre
                    imag[idx + half] = imag[idx] - tpim
                    real[idx] += tpre
                    imag[idx] += tpim
                    k += step
                }
                i += size
            }
            size *= 2
        }
        for (i in 0 until n / 2) {
            mags[i] = sqrt(real[i] * real[i] + imag[i] * imag[i])
        }
        return mags
    }
}
