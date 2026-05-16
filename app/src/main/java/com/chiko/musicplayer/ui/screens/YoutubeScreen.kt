package com.chiko.musicplayer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.chiko.musicplayer.ui.components.YoutubeChannelRow
import com.chiko.musicplayer.ui.components.YoutubeGridItem
import com.chiko.musicplayer.ui.components.YoutubePlaylistRow
import com.chiko.musicplayer.ui.components.YoutubeVideoRow
import com.chiko.musicplayer.ui.components.resonanceScrollbar
import com.chiko.musicplayer.youtube.YoutubeFeed
import com.chiko.musicplayer.youtube.YoutubeFilter
import com.chiko.musicplayer.youtube.YoutubeResult
import com.chiko.musicplayer.youtube.YoutubeVideo

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun YoutubeTabContent(
    initialQuery: String,
    results: List<YoutubeResult>,
    feed: YoutubeFeed?,
    filter: YoutubeFilter,
    gridView: Boolean,
    isSearching: Boolean,
    isResolving: Boolean,
    error: String?,
    history: List<String>,
    onSubmit: (String) -> Unit,
    onRemoveHistory: (String) -> Unit,
    onFilterChange: (YoutubeFilter) -> Unit,
    onToggleGridView: () -> Unit,
    onCloseFeed: () -> Unit,
    onOpenResult: (YoutubeResult) -> Unit,
    onPlayAudio: (YoutubeVideo) -> Unit,
    onPlayVideo: (YoutubeVideo) -> Unit,
    onPlayAudioFromFeed: (List<YoutubeVideo>, Int) -> Unit,
    onPlayVideoFromFeed: (List<YoutubeVideo>, Int) -> Unit,
    onDownloadAudio: (YoutubeVideo) -> Unit,
    onDownloadVideo: (YoutubeVideo) -> Unit,
    onLoadMore: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    var query by remember(initialQuery) { mutableStateOf(initialQuery) }
    var searchFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    // Clear the floating settings gear pinned top-left.
                    .padding(start = 44.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    placeholder = {
                        Text(
                            text = "Search or paste URL",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = {
                                query = ""
                                focusRequester.requestFocus()
                                keyboardController?.show()
                            }) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Clear",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        onSubmit(query)
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    }),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        cursorColor = MaterialTheme.colorScheme.primary,
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester)
                        .onFocusChanged { searchFocused = it.isFocused },
                )
                Spacer(Modifier.width(4.dp))
                FilterIcon(
                    icon = Icons.Rounded.Person,
                    description = "Channels",
                    selected = filter == YoutubeFilter.Channels,
                    onClick = {
                        onFilterChange(
                            if (filter == YoutubeFilter.Channels) YoutubeFilter.Videos
                            else YoutubeFilter.Channels
                        )
                    },
                )
                FilterIcon(
                    icon = Icons.Rounded.PlaylistPlay,
                    description = "Playlists",
                    selected = filter == YoutubeFilter.Playlists,
                    onClick = {
                        onFilterChange(
                            if (filter == YoutubeFilter.Playlists) YoutubeFilter.Videos
                            else YoutubeFilter.Playlists
                        )
                    },
                )
                IconButton(onClick = onToggleGridView) {
                    Icon(
                        imageVector = if (gridView) Icons.AutoMirrored.Rounded.ViewList
                        else Icons.Rounded.GridView,
                        contentDescription = "View",
                        tint = if (gridView) MaterialTheme.colorScheme.onBackground
                        else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f),
                    )
                }
            }

            val showHistoryOverResults = searchFocused && history.isNotEmpty() && feed == null
            val historyClick: (String) -> Unit = { q ->
                query = q
                onSubmit(q)
                keyboardController?.hide()
                focusManager.clearFocus()
            }
            when {
                isSearching -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }
                error != null -> MessageBox(error)
                showHistoryOverResults -> HistoryList(
                    history = history,
                    onSubmit = historyClick,
                    onRemove = onRemoveHistory,
                )
                feed != null -> FeedView(
                    feed = feed,
                    gridView = gridView,
                    contentPadding = contentPadding,
                    onClose = onCloseFeed,
                    onPlayAudioFromFeed = onPlayAudioFromFeed,
                    onPlayVideoFromFeed = onPlayVideoFromFeed,
                    onDownloadAudio = onDownloadAudio,
                    onDownloadVideo = onDownloadVideo,
                    onLoadMore = onLoadMore,
                )
                results.isEmpty() && initialQuery.isNotBlank() ->
                    MessageBox("No results for \"$initialQuery\"")
                results.isEmpty() && history.isNotEmpty() -> HistoryList(
                    history = history,
                    onSubmit = historyClick,
                    onRemove = onRemoveHistory,
                )
                results.isEmpty() ->
                    MessageBox("Search or paste a playlist / channel URL.")
                else -> ResultList(
                    results = results,
                    gridView = gridView,
                    contentPadding = contentPadding,
                    onOpenResult = onOpenResult,
                    onPlayAudioFromResults = onPlayAudioFromFeed,
                    onPlayVideoFromResults = onPlayVideoFromFeed,
                    onDownloadAudio = onDownloadAudio,
                    onDownloadVideo = onDownloadVideo,
                    onLoadMore = onLoadMore,
                )
            }
        }

        if (isResolving) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Preparing stream…",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = if (selected) MaterialTheme.colorScheme.onBackground
            else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f),
        )
    }
}

@Composable
private fun HistoryList(
    history: List<String>,
    onSubmit: (String) -> Unit,
    onRemove: (String) -> Unit,
) {
    val state = rememberLazyListState()
    LazyColumn(
        state = state,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.fillMaxSize().resonanceScrollbar(state),
    ) {
        items(history, key = { it }) { query ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSubmit(query) }
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = query,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { onRemove(query) }) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Remove",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageBox(text: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ResultList(
    results: List<YoutubeResult>,
    gridView: Boolean,
    contentPadding: PaddingValues,
    onOpenResult: (YoutubeResult) -> Unit,
    onPlayAudioFromResults: (List<YoutubeVideo>, Int) -> Unit,
    onPlayVideoFromResults: (List<YoutubeVideo>, Int) -> Unit,
    onDownloadAudio: (YoutubeVideo) -> Unit,
    onDownloadVideo: (YoutubeVideo) -> Unit,
    onLoadMore: () -> Unit,
) {
    val videos = remember(results) { results.filterIsInstance<YoutubeResult.Video>().map { it.video } }
    val videoIndexByUrl = remember(videos) { videos.withIndex().associate { it.value.url to it.index } }

    if (gridView) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val cells: GridCells = if (maxWidth >= 600.dp)
                GridCells.Adaptive(200.dp) else GridCells.Fixed(2)
            val gridState = rememberLazyGridState()
            NearEndLoaderGrid(state = gridState, threshold = 6, onLoadMore = onLoadMore)
            LazyVerticalGrid(
                columns = cells,
                state = gridState,
                contentPadding = PaddingValues(
                    start = 12.dp, end = 12.dp, top = 4.dp,
                    bottom = contentPadding.calculateBottomPadding() + 120.dp,
                ),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize().resonanceScrollbar(gridState),
            ) {
                results.forEach { result ->
                    when (result) {
                        is YoutubeResult.Video -> item(key = result.url) {
                            YoutubeGridItem(
                                video = result.video,
                                onPlayAudio = {
                                    val idx = videoIndexByUrl[result.video.url] ?: -1
                                    if (idx >= 0) onPlayAudioFromResults(videos, idx)
                                },
                                onPlayVideo = {
                                    val idx = videoIndexByUrl[result.video.url] ?: -1
                                    if (idx >= 0) onPlayVideoFromResults(videos, idx)
                                },
                                onDownloadAudio = { onDownloadAudio(result.video) },
                                onDownloadVideo = { onDownloadVideo(result.video) },
                            )
                        }
                        is YoutubeResult.Channel -> item(
                            key = result.url,
                            span = { GridItemSpan(maxLineSpan) },
                        ) {
                            YoutubeChannelRow(channel = result, onClick = { onOpenResult(result) })
                        }
                        is YoutubeResult.Playlist -> item(
                            key = result.url,
                            span = { GridItemSpan(maxLineSpan) },
                        ) {
                            YoutubePlaylistRow(playlist = result, onClick = { onOpenResult(result) })
                        }
                    }
                }
            }
        }
    } else {
        val state = rememberLazyListState()
        NearEndLoader(state = state, threshold = 3, onLoadMore = onLoadMore)
        LazyColumn(
            state = state,
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp, top = 4.dp,
                bottom = contentPadding.calculateBottomPadding() + 120.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize().resonanceScrollbar(state),
        ) {
            items(results, key = { it.url }) { result ->
                when (result) {
                    is YoutubeResult.Video -> YoutubeVideoRow(
                        video = result.video,
                        onPlayAudio = {
                            val idx = videoIndexByUrl[result.video.url] ?: -1
                            if (idx >= 0) onPlayAudioFromResults(videos, idx)
                        },
                        onPlayVideo = {
                            val idx = videoIndexByUrl[result.video.url] ?: -1
                            if (idx >= 0) onPlayVideoFromResults(videos, idx)
                        },
                        onDownloadAudio = { onDownloadAudio(result.video) },
                        onDownloadVideo = { onDownloadVideo(result.video) },
                        expanded = false,
                    )
                    is YoutubeResult.Channel -> YoutubeChannelRow(
                        channel = result, onClick = { onOpenResult(result) },
                    )
                    is YoutubeResult.Playlist -> YoutubePlaylistRow(
                        playlist = result, onClick = { onOpenResult(result) },
                    )
                }
            }
        }
    }
}

@Composable
private fun NearEndLoaderGrid(
    state: androidx.compose.foundation.lazy.grid.LazyGridState,
    threshold: Int,
    onLoadMore: () -> Unit,
) {
    androidx.compose.runtime.LaunchedEffect(state) {
        androidx.compose.runtime.snapshotFlow {
            val info = state.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            val total = info.totalItemsCount
            if (total > 0 && last >= total - threshold) total else -1
        }.collect { if (it > 0) onLoadMore() }
    }
}

@Composable
private fun FeedHeader(feed: YoutubeFeed, onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!feed.thumbnailUrl.isNullOrBlank()) {
            AsyncImage(
                model = feed.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(12.dp)),
            )
            Spacer(Modifier.width(12.dp))
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = feed.title.ifBlank {
                    if (feed.kind == YoutubeFeed.Kind.Playlist) "Playlist" else "Channel"
                },
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val kindLabel = when (feed.kind) {
                YoutubeFeed.Kind.Playlist -> "Playlist · ${feed.videos.size} videos"
                YoutubeFeed.Kind.Channel -> "Channel · ${feed.videos.size} videos"
            }
            Text(
                text = kindLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (feed.subtitle.isNotBlank()) {
                Text(
                    text = feed.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Close",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun NearEndLoader(
    state: androidx.compose.foundation.lazy.LazyListState,
    threshold: Int,
    onLoadMore: () -> Unit,
) {
    androidx.compose.runtime.LaunchedEffect(state) {
        androidx.compose.runtime.snapshotFlow {
            val info = state.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            val total = info.totalItemsCount
            if (total > 0 && last >= total - threshold) total else -1
        }.collect { triggerCount ->
            if (triggerCount > 0) onLoadMore()
        }
    }
}

@Composable
private fun FeedView(
    feed: YoutubeFeed,
    gridView: Boolean,
    contentPadding: PaddingValues,
    onClose: () -> Unit,
    onPlayAudioFromFeed: (List<YoutubeVideo>, Int) -> Unit,
    onPlayVideoFromFeed: (List<YoutubeVideo>, Int) -> Unit,
    onDownloadAudio: (YoutubeVideo) -> Unit,
    onDownloadVideo: (YoutubeVideo) -> Unit,
    onLoadMore: () -> Unit,
) {
    if (gridView) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val cells: GridCells = if (maxWidth >= 600.dp)
                GridCells.Adaptive(200.dp) else GridCells.Fixed(2)
            val gridState = rememberLazyGridState()
            NearEndLoaderGrid(state = gridState, threshold = 6, onLoadMore = onLoadMore)
            LazyVerticalGrid(
                columns = cells,
                state = gridState,
                contentPadding = PaddingValues(
                    start = 12.dp, end = 12.dp, top = 4.dp,
                    bottom = contentPadding.calculateBottomPadding() + 120.dp,
                ),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize().resonanceScrollbar(gridState),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    FeedHeader(feed = feed, onClose = onClose)
                }
                gridItemsIndexed(feed.videos, key = { _, v -> v.url }) { index, video ->
                    YoutubeGridItem(
                        video = video,
                        onPlayAudio = { onPlayAudioFromFeed(feed.videos, index) },
                        onPlayVideo = { onPlayVideoFromFeed(feed.videos, index) },
                        onDownloadAudio = { onDownloadAudio(video) },
                        onDownloadVideo = { onDownloadVideo(video) },
                    )
                }
            }
        }
        return
    }
    val state = rememberLazyListState()
    NearEndLoader(state = state, threshold = 4, onLoadMore = onLoadMore)
    LazyColumn(
        state = state,
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 4.dp,
            bottom = contentPadding.calculateBottomPadding() + 120.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize().resonanceScrollbar(state),
    ) {
        item {
            FeedHeader(feed = feed, onClose = onClose)
        }
        itemsIndexed(feed.videos, key = { _, v -> v.url }) { index, video ->
            YoutubeVideoRow(
                video = video,
                onPlayAudio = { onPlayAudioFromFeed(feed.videos, index) },
                onPlayVideo = { onPlayVideoFromFeed(feed.videos, index) },
                onDownloadAudio = { onDownloadAudio(video) },
                onDownloadVideo = { onDownloadVideo(video) },
                expanded = false,
            )
        }
        if (feed.videos.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No videos found.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
