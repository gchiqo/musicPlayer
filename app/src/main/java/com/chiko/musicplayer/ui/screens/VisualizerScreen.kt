package com.chiko.musicplayer.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlin.random.Random
import com.chiko.musicplayer.audio.VisualizerManager
import com.chiko.musicplayer.ui.theme.NeonCyan
import com.chiko.musicplayer.ui.theme.NeonViolet
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private val MODES = listOf("Pulse", "Wave", "Orbit", "Bars", "Rays", "Tunnel", "Particles", "Dancer")

private data class ParticleSeed(
    val baseX: Float,
    val baseY: Float,
    val speedX: Float,
    val speedY: Float,
    val baseRadius: Float,
    val tint: Float,
)

@Composable
fun VisualizerScreen(
    contentPadding: PaddingValues,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rawLevel by VisualizerManager.level.collectAsState()
    val level by animateFloatAsState(
        targetValue = rawLevel,
        animationSpec = tween(durationMillis = 60),
        label = "level",
    )
    val bands by VisualizerManager.bands.collectAsState()
    val particles = remember {
        List(90) {
            ParticleSeed(
                baseX = Random.nextFloat(),
                baseY = Random.nextFloat(),
                speedX = (Random.nextFloat() - 0.5f) * 0.06f,
                speedY = (Random.nextFloat() - 0.5f) * 0.06f,
                baseRadius = 1.5f + Random.nextFloat() * 3.5f,
                tint = Random.nextFloat(),
            )
        }
    }

    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (view.context as? android.app.Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        controller?.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    var modeIndex by remember { mutableIntStateOf(0) }
    var time by remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) {
        val start = System.currentTimeMillis()
        while (isActive) {
            time = (System.currentTimeMillis() - start) / 1000f
            delay(16)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { modeIndex = (modeIndex + 1) % MODES.size },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            when (modeIndex) {
                0 -> drawPulse(level, time)
                1 -> drawWave(level, time)
                2 -> drawOrbit(level, time)
                3 -> drawBars(bands)
                4 -> drawRays(bands, time)
                5 -> drawTunnel(level, time)
                6 -> drawParticles(particles, level, time)
                else -> drawDancer(level, bands, time)
            }
        }

        Box(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
            IconButton(
                onClick = onClose,
                modifier = Modifier.align(Alignment.TopStart),
            ) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "Close",
                    tint = Color.White.copy(alpha = 0.85f),
                )
            }
            Text(
                text = MODES[modeIndex].uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.6f),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp),
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPulse(level: Float, time: Float) {
    val center = Offset(size.width / 2f, size.height / 2f)
    val maxR = size.minDimension / 2f * 0.95f

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                NeonViolet.copy(alpha = 0.45f + level * 0.45f),
                NeonViolet.copy(alpha = 0.15f),
                Color.Transparent,
            ),
            center = center,
            radius = maxR,
        ),
        center = center,
        radius = maxR,
    )

    val coreR = 50f + level * (maxR * 0.55f)
    drawCircle(color = NeonViolet.copy(alpha = 0.85f), center = center, radius = coreR)
    drawCircle(color = Color.White.copy(alpha = 0.95f), center = center, radius = 18f + level * 28f)

    for (i in 0 until 3) {
        val phase = (time * 0.4f + i * 0.33f) % 1f
        val r = phase * maxR
        val alpha = (1f - phase) * (0.25f + level * 0.6f)
        drawCircle(
            color = NeonCyan.copy(alpha = alpha),
            center = center,
            radius = r,
            style = Stroke(width = 3f),
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawWave(level: Float, time: Float) {
    val centerY = size.height / 2f
    val amplitude = (20f + level * size.height / 4f).coerceAtMost(size.height / 2f - 20f)
    val path = Path().apply {
        moveTo(0f, centerY)
        var x = 0f
        val step = 4f
        while (x <= size.width) {
            val phase = (x / size.width.toFloat() * 6f + time * 2.5f).toDouble()
            val secondary = sin(phase * 2.0 + time.toDouble()) * 0.35
            val y = centerY + ((sin(phase) + secondary) * amplitude).toFloat()
            lineTo(x, y)
            x += step
        }
    }
    drawPath(
        path = path,
        brush = Brush.horizontalGradient(listOf(NeonCyan, NeonViolet, NeonCyan)),
        style = Stroke(width = 5f, cap = StrokeCap.Round),
    )

    val mirror = Path().apply {
        moveTo(0f, centerY)
        var x = 0f
        val step = 4f
        while (x <= size.width) {
            val phase = (x / size.width.toFloat() * 6f - time * 2.5f).toDouble()
            val y = centerY - (sin(phase) * amplitude * 0.7f).toFloat()
            lineTo(x, y)
            x += step
        }
    }
    drawPath(
        path = mirror,
        color = NeonViolet.copy(alpha = 0.5f),
        style = Stroke(width = 3f, cap = StrokeCap.Round),
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawOrbit(level: Float, time: Float) {
    val center = Offset(size.width / 2f, size.height / 2f)
    val maxR = size.minDimension / 2f * 0.85f
    val rings = 5
    for (i in 0 until rings) {
        val ringRadius = (i + 1f) / rings * maxR
        val dotCount = 8 + i * 4
        val rotation = time * (0.4f + i * 0.15f) * if (i % 2 == 0) 1f else -1f
        val baseAngle = rotation * 2 * PI
        val dotR = (3f + level * 12f) * (1f - i.toFloat() / rings * 0.5f)
        val color = lerpColor(NeonViolet, NeonCyan, i.toFloat() / (rings - 1))
        for (d in 0 until dotCount) {
            val angle = baseAngle + d * 2 * PI / dotCount
            val pulse = 1f + level * 0.4f * sin((time * 4 + d).toDouble()).toFloat()
            val x = (center.x + cos(angle).toFloat() * ringRadius * pulse)
            val y = (center.y + sin(angle).toFloat() * ringRadius * pulse)
            drawCircle(
                color = color.copy(alpha = 0.55f + level * 0.35f),
                center = Offset(x, y),
                radius = dotR,
            )
        }
    }
}

private fun DrawScope.drawBars(bands: FloatArray) {
    if (bands.isEmpty()) return
    val n = bands.size
    val gap = 8f
    val maxBarWidth = 6.dp.toPx()
    val sidePad = size.width * 0.06f
    val available = size.width - sidePad * 2f
    val computed = (available - (n - 1) * gap) / n
    val barWidth = computed.coerceAtMost(maxBarWidth).coerceAtLeast(2f)
    val contentWidth = n * barWidth + (n - 1) * gap
    val startX = (size.width - contentWidth) / 2f
    val maxHalf = size.height * 0.22f
    val centerY = size.height / 2f
    val minVisible = barWidth * 0.7f

    for (i in 0 until n) {
        val v = bands[i].coerceIn(0f, 1f)
        val h = (v * maxHalf * 2f).coerceAtLeast(minVisible)
        val x = startX + i * (barWidth + gap)
        val color = lerpColor(NeonViolet, NeonCyan, v)
        drawRoundRect(
            color = color,
            topLeft = Offset(x, centerY - h / 2f),
            size = Size(barWidth, h),
            cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
        )
    }
}

private fun DrawScope.drawRays(bands: FloatArray, time: Float) {
    if (bands.isEmpty()) return
    val n = bands.size
    val center = Offset(size.width / 2f, size.height / 2f)
    val baseR = size.minDimension * 0.13f
    val maxR = size.minDimension * 0.42f
    val arcLen = 2 * PI.toFloat() * baseR / n
    val strokeW = (arcLen * 0.55f).coerceAtLeast(2f)
    val rotation = time * 0.15f

    for (i in 0 until n) {
        val v = bands[i].coerceIn(0f, 1f)
        val angle = i * 2.0 * PI / n - PI / 2.0 + rotation.toDouble()
        val length = baseR + v * (maxR - baseR)
        val ca = cos(angle).toFloat()
        val sa = sin(angle).toFloat()
        val color = lerpColor(NeonViolet, NeonCyan, v)
        drawLine(
            color = color,
            start = Offset(center.x + ca * baseR, center.y + sa * baseR),
            end = Offset(center.x + ca * length, center.y + sa * length),
            strokeWidth = strokeW,
            cap = StrokeCap.Round,
        )
    }
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(NeonViolet.copy(alpha = 0.9f), Color.Transparent),
            center = center,
            radius = baseR * 1.2f,
        ),
        center = center,
        radius = baseR * 1.2f,
    )
    drawCircle(color = Color.White.copy(alpha = 0.9f), center = center, radius = baseR * 0.35f)
}

private fun DrawScope.drawTunnel(level: Float, time: Float) {
    val center = Offset(size.width / 2f, size.height / 2f)
    val maxR = size.minDimension * 0.55f
    val rings = 14
    rotate(degrees = time * 12f, pivot = center) {
        for (i in 0 until rings) {
            val phase = ((time * 0.45f + i.toFloat() / rings) % 1f)
            val r = maxR * phase
            val alpha = (1f - phase) * (0.25f + level * 0.7f)
            val color = lerpColor(NeonViolet, NeonCyan, phase)
            drawRect(
                color = color.copy(alpha = alpha),
                topLeft = Offset(center.x - r, center.y - r),
                size = Size(r * 2f, r * 2f),
                style = Stroke(width = 3f),
            )
        }
    }
}

private fun DrawScope.drawParticles(
    particles: List<ParticleSeed>,
    level: Float,
    time: Float,
) {
    val w = size.width
    val h = size.height
    val growth = 1f + level * 2.4f
    val alphaBase = 0.25f + level * 0.65f
    for (p in particles) {
        val rawX = (p.baseX + p.speedX * time) % 1f
        val rawY = (p.baseY + p.speedY * time) % 1f
        val nx = if (rawX < 0f) rawX + 1f else rawX
        val ny = if (rawY < 0f) rawY + 1f else rawY
        val x = nx * w
        val y = ny * h
        val r = p.baseRadius * growth
        val color = lerpColor(NeonViolet, NeonCyan, p.tint)
        drawCircle(
            color = color.copy(alpha = (alphaBase * 0.35f).coerceIn(0f, 1f)),
            center = Offset(x, y),
            radius = r * 2.8f,
        )
        drawCircle(
            color = Color.White.copy(alpha = alphaBase.coerceIn(0f, 1f)),
            center = Offset(x, y),
            radius = r,
        )
    }
}

private fun DrawScope.drawDancer(level: Float, bands: FloatArray, time: Float) {
    val bass = bandAverage(bands, 0, 4)
    val mid = bandAverage(bands, 8, 14)
    val high = bandAverage(bands, 18, bands.size)

    val bodyH = size.height * 0.50f
    val torso = bodyH * 0.30f
    val limb = bodyH * 0.22f
    val headR = bodyH * 0.07f
    val strokeW = (bodyH * 0.045f).coerceAtLeast(4f)
    val cx = size.width / 2f
    val cy = size.height / 2f

    val bobIdle = sin((time * 2.5).toDouble()).toFloat() * 4f * (0.3f + level)
    val bounce = -bass * bodyH * 0.10f + bobIdle
    val hipY = cy + bodyH * 0.18f + bounce
    val shoulderY = hipY - torso
    val headCenterY = shoulderY - headR * 1.4f

    val danceSpeed = 2.4f + bass * 3.6f
    val phase = (time * danceSpeed).toDouble()
    val leftArmAngle = (((sin(phase) + 1.0) * 0.5).toFloat()) * PI.toFloat() * 0.8f
    val rightArmAngle = (((sin(phase + PI) + 1.0) * 0.5).toFloat()) * PI.toFloat() * 0.8f
    val forearmBend = PI.toFloat() * 0.18f + mid * 0.7f

    val legSplay = 0.18f
    val leftKneeBend = (bass * 0.6f) + (((sin(phase + PI) + 1.0) * 0.5).toFloat()) * 0.25f
    val rightKneeBend = (bass * 0.6f) + (((sin(phase) + 1.0) * 0.5).toFloat()) * 0.25f

    val color = lerpColor(NeonViolet, NeonCyan, level)

    // Aura
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                NeonViolet.copy(alpha = (0.15f + bass * 0.55f).coerceIn(0f, 0.7f)),
                Color.Transparent,
            ),
            center = Offset(cx, headCenterY + torso * 0.3f),
            radius = bodyH * 0.75f,
        ),
        center = Offset(cx, headCenterY + torso * 0.3f),
        radius = bodyH * 0.75f,
    )

    // Head
    drawCircle(color = color, center = Offset(cx, headCenterY), radius = headR)
    if (high > 0.05f) {
        drawCircle(
            color = Color.White.copy(alpha = (high * 0.7f).coerceIn(0f, 1f)),
            center = Offset(cx, headCenterY),
            radius = headR * 0.45f,
        )
    }

    // Torso
    drawLine(
        color = color,
        start = Offset(cx, shoulderY),
        end = Offset(cx, hipY),
        strokeWidth = strokeW,
        cap = StrokeCap.Round,
    )

    // Arms
    val leftElbow = limbEnd(Offset(cx, shoulderY), leftArmAngle, limb * 0.55f, leftSide = true)
    val leftHand = limbEnd(leftElbow, leftArmAngle + forearmBend, limb * 0.55f, leftSide = true)
    drawLine(color, Offset(cx, shoulderY), leftElbow, strokeWidth = strokeW, cap = StrokeCap.Round)
    drawLine(color, leftElbow, leftHand, strokeWidth = strokeW * 0.85f, cap = StrokeCap.Round)

    val rightElbow = limbEnd(Offset(cx, shoulderY), rightArmAngle, limb * 0.55f, leftSide = false)
    val rightHand = limbEnd(rightElbow, rightArmAngle + forearmBend, limb * 0.55f, leftSide = false)
    drawLine(color, Offset(cx, shoulderY), rightElbow, strokeWidth = strokeW, cap = StrokeCap.Round)
    drawLine(color, rightElbow, rightHand, strokeWidth = strokeW * 0.85f, cap = StrokeCap.Round)

    // Legs
    val leftKnee = limbEnd(Offset(cx, hipY), legSplay, limb * 0.6f, leftSide = true)
    val leftFoot = limbEnd(leftKnee, legSplay - leftKneeBend, limb * 0.55f, leftSide = true)
    drawLine(color, Offset(cx, hipY), leftKnee, strokeWidth = strokeW, cap = StrokeCap.Round)
    drawLine(color, leftKnee, leftFoot, strokeWidth = strokeW * 0.85f, cap = StrokeCap.Round)

    val rightKnee = limbEnd(Offset(cx, hipY), legSplay, limb * 0.6f, leftSide = false)
    val rightFoot = limbEnd(rightKnee, legSplay - rightKneeBend, limb * 0.55f, leftSide = false)
    drawLine(color, Offset(cx, hipY), rightKnee, strokeWidth = strokeW, cap = StrokeCap.Round)
    drawLine(color, rightKnee, rightFoot, strokeWidth = strokeW * 0.85f, cap = StrokeCap.Round)
}

private fun limbEnd(start: Offset, angleFromDown: Float, length: Float, leftSide: Boolean): Offset {
    val sign = if (leftSide) -1f else 1f
    val dx = sign * sin(angleFromDown.toDouble()).toFloat() * length
    val dy = cos(angleFromDown.toDouble()).toFloat() * length
    return Offset(start.x + dx, start.y + dy)
}

private fun bandAverage(bands: FloatArray, from: Int, untilExclusive: Int): Float {
    if (bands.isEmpty()) return 0f
    val a = from.coerceIn(0, bands.size)
    val b = untilExclusive.coerceIn(a, bands.size)
    if (a >= b) return 0f
    var sum = 0f
    for (i in a until b) sum += bands[i]
    return sum / (b - a)
}

private fun lerpColor(a: Color, b: Color, t: Float): Color = Color(
    red = a.red + (b.red - a.red) * t,
    green = a.green + (b.green - a.green) * t,
    blue = a.blue + (b.blue - a.blue) * t,
    alpha = 1f,
)
