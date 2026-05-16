package com.chiko.musicplayer.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.chiko.musicplayer.R
import com.chiko.musicplayer.data.Folder
import com.chiko.musicplayer.data.Song
import com.chiko.musicplayer.ui.LibraryTab
import com.chiko.musicplayer.ui.SortBy
import com.chiko.musicplayer.ui.ViewMode
import com.chiko.musicplayer.ui.components.FolderGridItem
import com.chiko.musicplayer.ui.components.FolderRow
import com.chiko.musicplayer.ui.components.SongGridItem
import com.chiko.musicplayer.ui.components.SongRow
import com.chiko.musicplayer.ui.components.resonanceScrollbar
import com.chiko.musicplayer.youtube.YoutubeFeed
import com.chiko.musicplayer.youtube.YoutubeFilter
import com.chiko.musicplayer.youtube.YoutubeResult
import com.chiko.musicplayer.youtube.YoutubeVideo

@Composable
fun HomeScreen(
    songs: List<Song>,
    folders: List<Folder>,
    selectedFolder: Folder?,
    tab: LibraryTab,
    sortBy: SortBy,
    viewMode: ViewMode,
    isLoading: Boolean,
    currentSongId: Long?,
    isPlaying: Boolean,
    searchActive: Boolean,
    searchQuery: String,
    contentPadding: PaddingValues,
    youtubeQuery: String,
    youtubeResults: List<YoutubeResult>,
    youtubeFeed: YoutubeFeed?,
    youtubeFilter: YoutubeFilter,
    youtubeGridView: Boolean,
    youtubeSearching: Boolean,
    youtubeResolving: Boolean,
    youtubeError: String?,
    youtubeHistory: List<String>,
    onTabChange: (LibraryTab) -> Unit,
    onSortChange: (SortBy) -> Unit,
    onToggleViewMode: () -> Unit,
    onOpenSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onFolderClick: (Folder) -> Unit,
    onFolderBack: () -> Unit,
    onSongClick: (Song) -> Unit,
    onYoutubeSearch: (String) -> Unit,
    onYoutubeRemoveHistory: (String) -> Unit,
    onYoutubeFilterChange: (YoutubeFilter) -> Unit,
    onYoutubeToggleGridView: () -> Unit,
    onYoutubeCloseFeed: () -> Unit,
    onYoutubeOpenResult: (YoutubeResult) -> Unit,
    onYoutubePlayAudio: (YoutubeVideo) -> Unit,
    onYoutubePlayVideo: (YoutubeVideo) -> Unit,
    onYoutubePlayAudioFromFeed: (List<YoutubeVideo>, Int) -> Unit,
    onYoutubePlayVideoFromFeed: (List<YoutubeVideo>, Int) -> Unit,
    onYoutubeDownloadAudio: (YoutubeVideo) -> Unit,
    onYoutubeDownloadVideo: (YoutubeVideo) -> Unit,
    onYoutubeLoadMore: () -> Unit,
    onOpenSettings: () -> Unit,
    editMode: Boolean = false,
    selectedIds: Set<Long> = emptySet(),
    onEnterEditMode: (Long) -> Unit = {},
    onExitEditMode: () -> Unit = {},
    onToggleSelection: (Long) -> Unit = {},
    onOpenMoveDialog: () -> Unit = {},
    onReorder: (Long, Int, Int) -> Unit = { _, _, _ -> },
    streamSource: com.chiko.musicplayer.youtube.StreamSource = com.chiko.musicplayer.youtube.StreamSource.YouTube,
    onStreamSourceChange: (com.chiko.musicplayer.youtube.StreamSource) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val isCompactHeight = LocalConfiguration.current.screenHeightDp < 500
    val showSort = selectedFolder != null || tab == LibraryTab.Songs

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
        ) {
            when {
                editMode -> EditToolbar(
                    selectedCount = selectedIds.size,
                    canMove = selectedIds.isNotEmpty(),
                    onCancel = onExitEditMode,
                    onMove = onOpenMoveDialog,
                )
                searchActive -> SearchBar(
                    query = searchQuery,
                    onQueryChange = onSearchQueryChange,
                    onClose = onCloseSearch,
                )
                isCompactHeight && selectedFolder != null -> CompactFolderHeader(
                    folder = selectedFolder,
                    onBack = onFolderBack,
                    viewMode = viewMode,
                    onToggleViewMode = onToggleViewMode,
                    sortBy = sortBy,
                    onSortChange = onSortChange,
                    onOpenSearch = onOpenSearch,
                )
                isCompactHeight -> CompactBrowseHeader(
                    tab = tab,
                    onTabChange = onTabChange,
                    viewMode = viewMode,
                    onToggleViewMode = onToggleViewMode,
                    sortBy = sortBy,
                    onSortChange = onSortChange,
                    onOpenSearch = onOpenSearch,
                    showSort = showSort,
                    streamSource = streamSource,
                    onCycleStreamSource = { onStreamSourceChange(nextStreamSource(streamSource)) },
                )
                selectedFolder != null -> {
                    FolderHeader(folder = selectedFolder, onBack = onFolderBack)
                    ToolbarRow(
                        viewMode = viewMode,
                        onToggleViewMode = onToggleViewMode,
                        sortBy = sortBy,
                        onSortChange = onSortChange,
                        showSort = true,
                        showSearch = true,
                        onOpenSearch = onOpenSearch,
                        showEdit = true,
                        onEnterEditMode = { onEnterEditMode(-1L) },
                    )
                }
                else -> {
                    LibraryTabs(
                        tab = tab,
                        onTabChange = onTabChange,
                        streamSource = streamSource,
                        onCycleStreamSource = { onStreamSourceChange(nextStreamSource(streamSource)) },
                    )
                    if (tab != LibraryTab.YouTube) {
                        ToolbarRow(
                            viewMode = viewMode,
                            onToggleViewMode = onToggleViewMode,
                            sortBy = sortBy,
                            onSortChange = onSortChange,
                            showSort = showSort,
                            showSearch = true,
                            onOpenSearch = onOpenSearch,
                            showEdit = tab == LibraryTab.Songs,
                            onEnterEditMode = { onEnterEditMode(-1L) },
                        )
                    }
                }
            }

            when {
                isLoading && tab != LibraryTab.YouTube -> LoadingState()
                selectedFolder == null && tab == LibraryTab.Folders -> FolderContent(
                    folders = folders,
                    viewMode = viewMode,
                    searchQuery = if (searchActive) searchQuery else "",
                    onFolderClick = onFolderClick,
                )
                selectedFolder == null && tab == LibraryTab.YouTube -> YoutubeTabContent(
                    initialQuery = youtubeQuery,
                    results = youtubeResults,
                    feed = youtubeFeed,
                    filter = youtubeFilter,
                    gridView = youtubeGridView,
                    isSearching = youtubeSearching,
                    isResolving = youtubeResolving,
                    error = youtubeError,
                    history = youtubeHistory,
                    onSubmit = onYoutubeSearch,
                    onRemoveHistory = onYoutubeRemoveHistory,
                    onFilterChange = onYoutubeFilterChange,
                    onToggleGridView = onYoutubeToggleGridView,
                    onCloseFeed = onYoutubeCloseFeed,
                    onOpenResult = onYoutubeOpenResult,
                    onPlayAudio = onYoutubePlayAudio,
                    onPlayVideo = onYoutubePlayVideo,
                    onPlayAudioFromFeed = onYoutubePlayAudioFromFeed,
                    onPlayVideoFromFeed = onYoutubePlayVideoFromFeed,
                    onDownloadAudio = onYoutubeDownloadAudio,
                    onDownloadVideo = onYoutubeDownloadVideo,
                    onLoadMore = onYoutubeLoadMore,
                    contentPadding = contentPadding,
                )
                else -> SongContent(
                    songs = songs,
                    viewMode = viewMode,
                    currentSongId = currentSongId,
                    isPlaying = isPlaying,
                    searchQuery = if (searchActive) searchQuery else "",
                    onSongClick = { song ->
                        if (editMode) onToggleSelection(song.id) else onSongClick(song)
                    },
                    editMode = editMode,
                    selectedIds = selectedIds,
                    onLongPress = { song -> if (!editMode) onEnterEditMode(song.id) },
                    folderId = selectedFolder?.id,
                    onReorder = onReorder,
                )
            }
        }

        // Settings floats flush in the top-left corner (only offset down past
        // the status bar so it stays tappable). Hidden in edit/search modes
        // where a back/close control already owns that corner.
        if (!editMode && !searchActive) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = contentPadding.calculateTopPadding())
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.35f))
                    .clickable(onClick = onOpenSettings),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
                    modifier = Modifier.size(19.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Close search",
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
        TextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = {
                Text(
                    text = "Search tracks, artists, albums",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onBackground,
            ),
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = {
                        onQueryChange("")
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
                keyboardController?.hide()
                focusManager.clearFocus()
            }),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                cursorColor = MaterialTheme.colorScheme.primary,
            ),
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
        )
    }
}

@Composable
private fun CompactBrowseHeader(
    tab: LibraryTab,
    onTabChange: (LibraryTab) -> Unit,
    viewMode: ViewMode,
    onToggleViewMode: () -> Unit,
    sortBy: SortBy,
    onSortChange: (SortBy) -> Unit,
    onOpenSearch: () -> Unit,
    showSort: Boolean,
    streamSource: com.chiko.musicplayer.youtube.StreamSource = com.chiko.musicplayer.youtube.StreamSource.YouTube,
    onCycleStreamSource: () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // Leave room for the floating settings gear pinned top-left.
            .padding(start = 44.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_brand_logo),
            contentDescription = null,
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp)),
        )
        Spacer(Modifier.width(10.dp))
        CompactTab(
            label = "Songs",
            selected = tab == LibraryTab.Songs,
            onClick = { onTabChange(LibraryTab.Songs) },
        )
        Spacer(Modifier.width(2.dp))
        CompactTab(
            label = "Folders",
            selected = tab == LibraryTab.Folders,
            onClick = { onTabChange(LibraryTab.Folders) },
        )
        Spacer(Modifier.width(2.dp))
        CompactTab(
            label = streamSource.label,
            selected = tab == LibraryTab.YouTube,
            onClick = {
                if (tab == LibraryTab.YouTube) onCycleStreamSource()
                else onTabChange(LibraryTab.YouTube)
            },
        )
        Spacer(Modifier.weight(1f))
        if (tab != LibraryTab.YouTube) {
            if (showSort) {
                SortMenu(current = sortBy, onChange = onSortChange)
                Spacer(Modifier.width(4.dp))
            }
            IconButton(onClick = onOpenSearch) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            IconButton(onClick = onToggleViewMode) {
                Icon(
                    imageVector = if (viewMode == ViewMode.List) Icons.Rounded.GridView else Icons.AutoMirrored.Rounded.ViewList,
                    contentDescription = if (viewMode == ViewMode.List) "Grid view" else "List view",
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
    }
}

@Composable
private fun CompactFolderHeader(
    folder: Folder,
    onBack: () -> Unit,
    viewMode: ViewMode,
    onToggleViewMode: () -> Unit,
    sortBy: SortBy,
    onSortChange: (SortBy) -> Unit,
    onOpenSearch: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // Clear the floating settings gear pinned top-left.
            .padding(start = 44.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
        Text(
            text = folder.name,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        SortMenu(current = sortBy, onChange = onSortChange)
        IconButton(onClick = onOpenSearch) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = "Search",
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
        IconButton(onClick = onToggleViewMode) {
            Icon(
                imageVector = if (viewMode == ViewMode.List) Icons.Rounded.GridView else Icons.AutoMirrored.Rounded.ViewList,
                contentDescription = if (viewMode == ViewMode.List) "Grid view" else "List view",
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

@Composable
private fun CompactTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Text(
        text = label,
        style = MaterialTheme.typography.titleMedium,
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}

@Composable
private fun AppHeader(songCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_brand_logo),
            contentDescription = null,
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp)),
        )
        Spacer(Modifier.size(10.dp))
        Text(
            text = "Resonance",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.weight(1f))
        if (songCount > 0) {
            Text(
                text = "$songCount tracks",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FolderHeader(folder: Folder, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // Clear the floating settings gear pinned top-left.
            .padding(start = 44.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
        Spacer(Modifier.size(4.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = folder.name,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${folder.songCount} ${if (folder.songCount == 1) "track" else "tracks"}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun nextStreamSource(
    current: com.chiko.musicplayer.youtube.StreamSource,
): com.chiko.musicplayer.youtube.StreamSource {
    val values = com.chiko.musicplayer.youtube.StreamSource.values()
    return values[(values.indexOf(current) + 1) % values.size]
}

@Composable
private fun LibraryTabs(
    tab: LibraryTab,
    onTabChange: (LibraryTab) -> Unit,
    streamSource: com.chiko.musicplayer.youtube.StreamSource = com.chiko.musicplayer.youtube.StreamSource.YouTube,
    onCycleStreamSource: () -> Unit = {},
) {
    val tabs = remember { listOf(LibraryTab.Songs, LibraryTab.Folders, LibraryTab.YouTube) }
    val selectedIndex = tabs.indexOf(tab).coerceAtLeast(0)
    PrimaryTabRow(
        selectedTabIndex = selectedIndex,
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(horizontal = 12.dp),
    ) {
        tabs.forEachIndexed { index, t ->
            Tab(
                selected = index == selectedIndex,
                onClick = {
                    if (t == LibraryTab.YouTube && tab == LibraryTab.YouTube) {
                        onCycleStreamSource()
                    } else {
                        onTabChange(t)
                    }
                },
                selectedContentColor = MaterialTheme.colorScheme.primary,
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                icon = {
                    Icon(
                        imageVector = when (t) {
                            LibraryTab.Songs -> Icons.Rounded.MusicNote
                            LibraryTab.Folders -> Icons.Rounded.Folder
                            LibraryTab.YouTube -> when (streamSource) {
                                com.chiko.musicplayer.youtube.StreamSource.YouTube -> Icons.Rounded.VideoLibrary
                                com.chiko.musicplayer.youtube.StreamSource.SoundCloud -> Icons.Rounded.Cloud
                            }
                        },
                        contentDescription = when (t) {
                            LibraryTab.Songs -> "Songs"
                            LibraryTab.Folders -> "Folders"
                            LibraryTab.YouTube -> streamSource.label
                        },
                        modifier = Modifier.size(22.dp),
                    )
                },
            )
        }
    }
}

@Composable
private fun EditToolbar(
    selectedCount: Int,
    canMove: Boolean,
    onCancel: () -> Unit,
    onMove: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onCancel) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Exit edit mode",
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
        Text(
            text = if (selectedCount == 0) "Edit — tap tracks to select"
            else "$selectedCount selected",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
        androidx.compose.material3.TextButton(
            onClick = onMove,
            enabled = canMove,
        ) {
            Icon(
                imageVector = Icons.Rounded.Folder,
                contentDescription = null,
                tint = if (canMove) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "Move",
                color = if (canMove) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            )
        }
    }
}

@Composable
private fun ToolbarRow(
    viewMode: ViewMode,
    onToggleViewMode: () -> Unit,
    sortBy: SortBy,
    onSortChange: (SortBy) -> Unit,
    showSort: Boolean,
    showSearch: Boolean,
    onOpenSearch: () -> Unit,
    showEdit: Boolean = false,
    onEnterEditMode: () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showSort) {
            SortMenu(current = sortBy, onChange = onSortChange)
        }
        Spacer(Modifier.weight(1f))
        if (showEdit) {
            IconButton(onClick = onEnterEditMode) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = "Edit",
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
        if (showSearch) {
            IconButton(onClick = onOpenSearch) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
        IconButton(onClick = onToggleViewMode) {
            Icon(
                imageVector = if (viewMode == ViewMode.List) Icons.Rounded.GridView else Icons.AutoMirrored.Rounded.ViewList,
                contentDescription = if (viewMode == ViewMode.List) "Grid view" else "List view",
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

@Composable
private fun SortMenu(current: SortBy, onChange: (SortBy) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                .clickable { expanded = true }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.Sort,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.size(8.dp))
            Text(
                text = current.label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            SortBy.entries.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option.label,
                            color = if (option == current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        )
                    },
                    leadingIcon = {
                        if (option == current) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        } else {
                            Spacer(Modifier.size(24.dp))
                        }
                    },
                    onClick = {
                        onChange(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun SongContent(
    songs: List<Song>,
    viewMode: ViewMode,
    currentSongId: Long?,
    isPlaying: Boolean,
    searchQuery: String,
    onSongClick: (Song) -> Unit,
    editMode: Boolean = false,
    selectedIds: Set<Long> = emptySet(),
    onLongPress: (Song) -> Unit = {},
    folderId: Long? = null,
    onReorder: (Long, Int, Int) -> Unit = { _, _, _ -> },
) {
    if (songs.isEmpty()) {
        val message = if (searchQuery.isNotEmpty()) "No tracks match \"$searchQuery\"."
        else "No tracks here yet."
        EmptyState(message = message)
        return
    }
    val canReorder = editMode && folderId != null
    when (viewMode) {
        ViewMode.List -> {
            val listState = rememberLazyListState()
            val density = androidx.compose.ui.platform.LocalDensity.current
            var draggedId by remember { mutableStateOf<Long?>(null) }
            var dragStartIndex by remember { mutableStateOf(0) }
            var dragOffsetY by remember { mutableStateOf(0f) }
            val rowHeightPx = with(density) { 82.dp.toPx() }
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize().resonanceScrollbar(listState),
            ) {
                itemsIndexed(songs, key = { _, it -> it.id }) { index, song ->
                    val isDragging = song.id == draggedId
                    val rowModifier = if (isDragging) {
                        Modifier
                            .zIndex(1f)
                            .graphicsLayer { translationY = dragOffsetY }
                    } else {
                        Modifier
                    }
                    val dragHandleMod = if (canReorder) {
                        Modifier.pointerInput(song.id, songs.size) {
                            detectDragGestures(
                                onDragStart = {
                                    draggedId = song.id
                                    dragStartIndex = index
                                    dragOffsetY = 0f
                                },
                                onDrag = { change, drag ->
                                    change.consume()
                                    dragOffsetY += drag.y
                                },
                                onDragEnd = {
                                    val currentId = draggedId
                                    if (currentId != null && folderId != null) {
                                        val delta = (dragOffsetY / rowHeightPx).toInt()
                                        val target = (dragStartIndex + delta)
                                            .coerceIn(0, songs.size - 1)
                                        if (target != dragStartIndex) {
                                            onReorder(folderId, dragStartIndex, target)
                                        }
                                    }
                                    draggedId = null
                                    dragOffsetY = 0f
                                },
                                onDragCancel = {
                                    draggedId = null
                                    dragOffsetY = 0f
                                },
                            )
                        }
                    } else {
                        Modifier
                    }
                    SongRow(
                        song = song,
                        isCurrent = song.id == currentSongId,
                        isPlaying = isPlaying,
                        onClick = { onSongClick(song) },
                        modifier = rowModifier,
                        editMode = editMode,
                        selected = song.id in selectedIds,
                        onLongPress = if (!editMode) ({ onLongPress(song) }) else null,
                        showDragHandle = canReorder,
                        dragHandleModifier = dragHandleMod,
                    )
                }
                item { Spacer(Modifier.height(120.dp)) }
            }
        }
        ViewMode.Grid -> BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val cells: GridCells = if (maxWidth >= 600.dp)
                GridCells.Adaptive(160.dp) else GridCells.Fixed(3)
            val gridState = rememberLazyGridState()
            LazyVerticalGrid(
                columns = cells,
                state = gridState,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize().resonanceScrollbar(gridState),
            ) {
                items(songs, key = { it.id }) { song ->
                    SongGridItem(
                        song = song,
                        isCurrent = song.id == currentSongId,
                        isPlaying = isPlaying,
                        onClick = { onSongClick(song) },
                        editMode = editMode,
                        selected = song.id in selectedIds,
                        onLongPress = if (!editMode) ({ onLongPress(song) }) else null,
                    )
                }
                item(span = { GridItemSpan(maxLineSpan) }) { Spacer(Modifier.height(120.dp)) }
            }
        }
    }
}

@Composable
private fun FolderContent(
    folders: List<Folder>,
    viewMode: ViewMode,
    searchQuery: String,
    onFolderClick: (Folder) -> Unit,
) {
    if (folders.isEmpty()) {
        val message = if (searchQuery.isNotEmpty()) "No folders match \"$searchQuery\"."
        else "No folders found."
        EmptyState(message = message)
        return
    }
    when (viewMode) {
        ViewMode.List -> {
            val listState = rememberLazyListState()
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize().resonanceScrollbar(listState),
            ) {
                items(folders, key = { it.id }) { folder ->
                    FolderRow(folder = folder, onClick = { onFolderClick(folder) })
                }
                item { Spacer(Modifier.height(120.dp)) }
            }
        }
        ViewMode.Grid -> BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val cells: GridCells = if (maxWidth >= 600.dp)
                GridCells.Adaptive(160.dp) else GridCells.Fixed(3)
            val gridState = rememberLazyGridState()
            LazyVerticalGrid(
                columns = cells,
                state = gridState,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize().resonanceScrollbar(gridState),
            ) {
                items(folders, key = { it.id }) { folder ->
                    FolderGridItem(folder = folder, onClick = { onFolderClick(folder) })
                }
                item(span = { GridItemSpan(maxLineSpan) }) { Spacer(Modifier.height(120.dp)) }
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun EmptyState(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Rounded.LibraryMusic,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(72.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
    }
}
