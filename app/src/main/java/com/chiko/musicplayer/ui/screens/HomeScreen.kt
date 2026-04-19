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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Search
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.chiko.musicplayer.ui.theme.AppGradient
import com.chiko.musicplayer.ui.theme.NeonViolet

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
    onTabChange: (LibraryTab) -> Unit,
    onSortChange: (SortBy) -> Unit,
    onToggleViewMode: () -> Unit,
    onOpenSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onFolderClick: (Folder) -> Unit,
    onFolderBack: () -> Unit,
    onSongClick: (Song) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isCompactHeight = LocalConfiguration.current.screenHeightDp < 500
    val showSort = selectedFolder != null || tab == LibraryTab.Songs

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppGradient),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
        ) {
            when {
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
                    )
                }
                else -> {
                    AppHeader(songCount = songs.size)
                    LibraryTabs(tab = tab, onTabChange = onTabChange)
                    ToolbarRow(
                        viewMode = viewMode,
                        onToggleViewMode = onToggleViewMode,
                        sortBy = sortBy,
                        onSortChange = onSortChange,
                        showSort = showSort,
                        showSearch = true,
                        onOpenSearch = onOpenSearch,
                    )
                }
            }

            when {
                isLoading -> LoadingState()
                selectedFolder == null && tab == LibraryTab.Folders -> FolderContent(
                    folders = folders,
                    viewMode = viewMode,
                    searchQuery = if (searchActive) searchQuery else "",
                    onFolderClick = onFolderClick,
                )
                else -> SongContent(
                    songs = songs,
                    viewMode = viewMode,
                    currentSongId = currentSongId,
                    isPlaying = isPlaying,
                    searchQuery = if (searchActive) searchQuery else "",
                    onSongClick = onSongClick,
                )
            }
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
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
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                cursorColor = NeonViolet,
            ),
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
        )
        if (query.isNotEmpty()) {
            IconButton(onClick = { onQueryChange("") }) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Clear",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
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
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
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
        Spacer(Modifier.weight(1f))
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
            .padding(horizontal = 4.dp, vertical = 4.dp),
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
        color = if (selected) NeonViolet else MaterialTheme.colorScheme.onSurfaceVariant,
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
            .padding(horizontal = 8.dp, vertical = 12.dp),
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

@Composable
private fun LibraryTabs(tab: LibraryTab, onTabChange: (LibraryTab) -> Unit) {
    val tabs = remember { listOf(LibraryTab.Songs, LibraryTab.Folders) }
    val selectedIndex = tabs.indexOf(tab)
    PrimaryTabRow(
        selectedTabIndex = selectedIndex,
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(horizontal = 12.dp),
    ) {
        tabs.forEachIndexed { index, t ->
            Tab(
                selected = index == selectedIndex,
                onClick = { onTabChange(t) },
                selectedContentColor = NeonViolet,
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                text = {
                    Text(
                        text = if (t == LibraryTab.Songs) "Songs" else "Folders",
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
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
                            color = if (option == current) NeonViolet else MaterialTheme.colorScheme.onSurface,
                        )
                    },
                    leadingIcon = {
                        if (option == current) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = NeonViolet,
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
) {
    if (songs.isEmpty()) {
        val message = if (searchQuery.isNotEmpty()) "No tracks match \"$searchQuery\"."
        else "No tracks here yet."
        EmptyState(message = message)
        return
    }
    when (viewMode) {
        ViewMode.List -> LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(songs, key = { it.id }) { song ->
                SongRow(
                    song = song,
                    isCurrent = song.id == currentSongId,
                    isPlaying = isPlaying,
                    onClick = { onSongClick(song) },
                )
            }
            item { Spacer(Modifier.height(120.dp)) }
        }
        ViewMode.Grid -> BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val cells: GridCells = if (maxWidth >= 600.dp)
                GridCells.Adaptive(160.dp) else GridCells.Fixed(3)
            LazyVerticalGrid(
                columns = cells,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(songs, key = { it.id }) { song ->
                    SongGridItem(
                        song = song,
                        isCurrent = song.id == currentSongId,
                        isPlaying = isPlaying,
                        onClick = { onSongClick(song) },
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
        ViewMode.List -> LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(folders, key = { it.id }) { folder ->
                FolderRow(folder = folder, onClick = { onFolderClick(folder) })
            }
            item { Spacer(Modifier.height(120.dp)) }
        }
        ViewMode.Grid -> BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val cells: GridCells = if (maxWidth >= 600.dp)
                GridCells.Adaptive(160.dp) else GridCells.Fixed(3)
            LazyVerticalGrid(
                columns = cells,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize(),
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
        CircularProgressIndicator(color = NeonViolet)
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
