package com.chiko.musicplayer.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun Modifier.resonanceScrollbar(
    state: LazyListState,
    color: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
    thumbWidth: Dp = 4.dp,
    hitZoneWidth: Dp = 20.dp,
): Modifier = composed {
    val density = LocalDensity.current
    val thumbPx = with(density) { thumbWidth.toPx() }
    val padPx = with(density) { 2.dp.toPx() }
    val minThumbPx = with(density) { 32.dp.toPx() }
    val hitZonePx = with(density) { hitZoneWidth.toPx() }
    val scope = rememberCoroutineScope()
    var dragging by remember { mutableStateOf(false) }
    val alphaAnim by animateFloatAsState(
        targetValue = if (state.isScrollInProgress || dragging) 1f else 0f,
        animationSpec = tween(600),
        label = "sbar",
    )

    fun metrics(): Triple<Float, Float, Float>? {
        val info = state.layoutInfo
        val visible = info.visibleItemsInfo
        val total = info.totalItemsCount
        if (visible.isEmpty() || total == 0) return null
        val avgSize = visible.sumOf { it.size } / visible.size.toFloat()
        val totalHeight = total * avgSize
        val viewport = (info.viewportEndOffset - info.viewportStartOffset).toFloat()
        if (totalHeight <= viewport) return null
        val scrollOffset = state.firstVisibleItemIndex * avgSize + state.firstVisibleItemScrollOffset
        return Triple(avgSize, totalHeight, scrollOffset)
    }

    this
        .pointerInput(state) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                if (down.position.x < size.width - hitZonePx) return@awaitEachGesture
                val (_, totalHeight, _) = metrics() ?: return@awaitEachGesture
                down.consume()
                dragging = true
                val info = state.layoutInfo
                val viewport = (info.viewportEndOffset - info.viewportStartOffset).toFloat()
                val thumbH = (viewport * viewport / totalHeight).coerceAtLeast(minThumbPx)
                val track = (size.height - thumbH).coerceAtLeast(1f)
                val perPx = (totalHeight - viewport) / track
                // jump-to on tap
                val currentScroll = state.firstVisibleItemIndex *
                    (info.visibleItemsInfo.sumOf { it.size } / info.visibleItemsInfo.size.toFloat()) +
                    state.firstVisibleItemScrollOffset
                val tapProgress = (down.position.y / size.height).coerceIn(0f, 1f)
                val targetScroll = tapProgress * (totalHeight - viewport)
                scope.launch { state.scrollBy(targetScroll - currentScroll) }
                var lastY = down.position.y
                drag(down.id) { change ->
                    val dy = change.position.y - lastY
                    lastY = change.position.y
                    scope.launch { state.scrollBy(dy * perPx) }
                    change.consume()
                }
                dragging = false
            }
        }
        .drawWithContent {
            drawContent()
            if (alphaAnim <= 0f) return@drawWithContent
            val (_, totalHeight, scrollOffset) = metrics() ?: return@drawWithContent
            val info = state.layoutInfo
            val viewport = (info.viewportEndOffset - info.viewportStartOffset).toFloat()
            val maxScroll = (totalHeight - viewport).coerceAtLeast(1f)
            val progress = (scrollOffset / maxScroll).coerceIn(0f, 1f)
            val thumbH = (viewport * viewport / totalHeight).coerceAtLeast(minThumbPx)
            val track = (size.height - thumbH).coerceAtLeast(0f)
            val thumbTop = progress * track
            drawRoundRect(
                color = color.copy(alpha = color.alpha * alphaAnim),
                topLeft = Offset(size.width - thumbPx - padPx, thumbTop),
                size = Size(thumbPx, thumbH),
                cornerRadius = CornerRadius(thumbPx / 2f, thumbPx / 2f),
            )
        }
}

@Composable
fun Modifier.resonanceScrollbar(
    state: LazyGridState,
    color: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
    thumbWidth: Dp = 4.dp,
    hitZoneWidth: Dp = 20.dp,
): Modifier = composed {
    val density = LocalDensity.current
    val thumbPx = with(density) { thumbWidth.toPx() }
    val padPx = with(density) { 2.dp.toPx() }
    val minThumbPx = with(density) { 32.dp.toPx() }
    val hitZonePx = with(density) { hitZoneWidth.toPx() }
    val scope = rememberCoroutineScope()
    var dragging by remember { mutableStateOf(false) }
    val alphaAnim by animateFloatAsState(
        targetValue = if (state.isScrollInProgress || dragging) 1f else 0f,
        animationSpec = tween(600),
        label = "grid-sbar",
    )

    fun metrics(): Triple<Int, Float, Float>? {
        val info = state.layoutInfo
        val visible = info.visibleItemsInfo
        val total = info.totalItemsCount
        if (visible.isEmpty() || total == 0) return null
        val columns = (visible.maxOfOrNull { it.column + 1 } ?: 1).coerceAtLeast(1)
        val rowCount = ((total + columns - 1) / columns).coerceAtLeast(1)
        val avgRowHeight = visible.sumOf { it.size.height } / visible.size.toFloat()
        val totalHeight = rowCount * avgRowHeight
        val scrollOffset = (state.firstVisibleItemIndex / columns) * avgRowHeight +
            state.firstVisibleItemScrollOffset
        return Triple(columns, totalHeight, scrollOffset)
    }

    this
        .pointerInput(state) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                if (down.position.x < size.width - hitZonePx) return@awaitEachGesture
                val m = metrics() ?: return@awaitEachGesture
                val (_, totalHeight, scrollOffset) = m
                down.consume()
                dragging = true
                val info = state.layoutInfo
                val viewport = (info.viewportEndOffset - info.viewportStartOffset).toFloat()
                val thumbH = (viewport * viewport / totalHeight).coerceAtLeast(minThumbPx)
                val track = (size.height - thumbH).coerceAtLeast(1f)
                val perPx = (totalHeight - viewport) / track
                val tapProgress = (down.position.y / size.height).coerceIn(0f, 1f)
                val targetScroll = tapProgress * (totalHeight - viewport)
                scope.launch { state.scrollBy(targetScroll - scrollOffset) }
                var lastY = down.position.y
                drag(down.id) { change ->
                    val dy = change.position.y - lastY
                    lastY = change.position.y
                    scope.launch { state.scrollBy(dy * perPx) }
                    change.consume()
                }
                dragging = false
            }
        }
        .drawWithContent {
            drawContent()
            if (alphaAnim <= 0f) return@drawWithContent
            val m = metrics() ?: return@drawWithContent
            val (_, totalHeight, scrollOffset) = m
            val info = state.layoutInfo
            val viewport = (info.viewportEndOffset - info.viewportStartOffset).toFloat()
            if (totalHeight <= viewport) return@drawWithContent
            val maxScroll = (totalHeight - viewport).coerceAtLeast(1f)
            val progress = (scrollOffset / maxScroll).coerceIn(0f, 1f)
            val thumbH = (viewport * viewport / totalHeight).coerceAtLeast(minThumbPx)
            val track = (size.height - thumbH).coerceAtLeast(0f)
            val thumbTop = progress * track
            drawRoundRect(
                color = color.copy(alpha = color.alpha * alphaAnim),
                topLeft = Offset(size.width - thumbPx - padPx, thumbTop),
                size = Size(thumbPx, thumbH),
                cornerRadius = CornerRadius(thumbPx / 2f, thumbPx / 2f),
            )
        }
}
