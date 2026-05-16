package com.chiko.musicplayer

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import android.widget.Toast
import androidx.media3.common.util.UnstableApi
import com.chiko.musicplayer.ui.MusicViewModel
import com.chiko.musicplayer.ui.components.MiniPlayer
import com.chiko.musicplayer.ui.screens.EqualizerScreen
import com.chiko.musicplayer.ui.screens.HomeScreen
import com.chiko.musicplayer.ui.screens.PermissionScreen
import com.chiko.musicplayer.ui.screens.PlayerScreen
import com.chiko.musicplayer.ui.screens.SettingsScreen
import com.chiko.musicplayer.ui.screens.VideoPlayerScreen
import com.chiko.musicplayer.ui.screens.VisualizerScreen
import com.chiko.musicplayer.ui.theme.MusicPlayerTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MusicPlayerTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color.Transparent,
                ) { padding ->
                    App(padding)
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class, UnstableApi::class)
@Composable
private fun App(scaffoldPadding: PaddingValues) {
    val audioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        android.Manifest.permission.READ_MEDIA_AUDIO
    } else {
        android.Manifest.permission.READ_EXTERNAL_STORAGE
    }
    val permissionState = rememberPermissionState(audioPermission)
    val granted = permissionState.status is PermissionStatus.Granted
    val context = androidx.compose.ui.platform.LocalContext.current

    var hasRequested by rememberSaveable { mutableStateOf(false) }
    if (!granted) {
        val status = permissionState.status
        val permanentlyDenied = hasRequested &&
            status is PermissionStatus.Denied && !status.shouldShowRationale
        PermissionScreen(
            permanentlyDenied = permanentlyDenied,
            onRequest = {
                hasRequested = true
                permissionState.launchPermissionRequest()
            },
            onOpenSettings = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxSize(),
        )
        return
    }

    PlayerHost(scaffoldPadding)
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun PlayerHost(scaffoldPadding: PaddingValues) {
    val viewModel: MusicViewModel = viewModel()

    LaunchedEffect(Unit) {
        viewModel.connect()
        viewModel.loadSongs()
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val notifPermission = rememberPermissionState(android.Manifest.permission.POST_NOTIFICATIONS)
        var requestedNotif by rememberSaveable { mutableStateOf(false) }
        LaunchedEffect(notifPermission.status) {
            if (!requestedNotif && notifPermission.status !is PermissionStatus.Granted) {
                requestedNotif = true
                notifPermission.launchPermissionRequest()
            }
        }
    }

    val effectiveAccent by viewModel.effectiveAccent.collectAsState()
    val effectiveBackground by viewModel.effectiveBackground.collectAsState()
    val backgroundArgb by viewModel.backgroundColor.collectAsState()
    val dynamicFromArt by viewModel.dynamicFromArt.collectAsState()
    val displayedSongs by viewModel.displayedSongs.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val selectedFolder by viewModel.selectedFolder.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()
    val youtubeQuery by viewModel.youtubeQuery.collectAsState()
    val youtubeResults by viewModel.youtubeResults.collectAsState()
    val youtubeFeed by viewModel.youtubeFeed.collectAsState()
    val youtubeFilter by viewModel.youtubeFilter.collectAsState()
    val youtubeGridView by viewModel.youtubeGridView.collectAsState()
    val youtubeSearching by viewModel.youtubeSearching.collectAsState()
    val youtubeResolving by viewModel.youtubeResolving.collectAsState()
    val youtubeError by viewModel.youtubeError.collectAsState()
    val youtubeHistory by viewModel.youtubeHistory.collectAsState()
    val tab by viewModel.tab.collectAsState()
    val sortBy by viewModel.sortBy.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val currentId by viewModel.currentSongId.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val positionMs by viewModel.positionMs.collectAsState()
    val durationMs by viewModel.durationMs.collectAsState()
    val shuffle by viewModel.shuffle.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val playbackSpeed by viewModel.playbackSpeed.collectAsState()
    val showPlayer by viewModel.showPlayer.collectAsState()
    val showEqualizer by viewModel.showEqualizer.collectAsState()
    val showVisualizer by viewModel.showVisualizer.collectAsState()
    val showVideoPlayer by viewModel.showVideoPlayer.collectAsState()
    val showSettings by viewModel.showSettings.collectAsState()
    val accentArgb by viewModel.accentColor.collectAsState()
    val currentIsVideo by viewModel.currentIsVideo.collectAsState()
    val toast by viewModel.toast.collectAsState()
    val ctx = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(toast) {
        toast?.let {
            Toast.makeText(ctx, it, Toast.LENGTH_SHORT).show()
            viewModel.consumeToast()
        }
    }
    val searchActive by viewModel.searchActive.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val editMode by viewModel.editMode.collectAsState()
    val selectedIds by viewModel.selectedIds.collectAsState()
    val streamSource by viewModel.streamSource.collectAsState()
    val showMoveDialog by viewModel.showMoveDialog.collectAsState()
    val consentRequest by viewModel.consentRequest.collectAsState()
    val showPlaylistDownloadDialog by viewModel.showPlaylistDownloadDialog.collectAsState()
    val playlistDownload by viewModel.playlistDownload.collectAsState()
    val progress = if (durationMs > 0) positionMs.toFloat() / durationMs.toFloat() else 0f

    val consentLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        viewModel.onConsentResult(result.resultCode == android.app.Activity.RESULT_OK)
    }
    LaunchedEffect(consentRequest) {
        consentRequest?.let { sender ->
            consentLauncher.launch(
                androidx.activity.result.IntentSenderRequest.Builder(sender).build()
            )
        }
    }

    BackHandler(enabled = showSettings) { viewModel.closeSettings() }
    BackHandler(enabled = !showSettings && showVideoPlayer) { viewModel.closeVideoPlayer() }
    BackHandler(enabled = !showSettings && !showVideoPlayer && showVisualizer) { viewModel.closeVisualizer() }
    BackHandler(enabled = !showSettings && !showVideoPlayer && !showVisualizer && showEqualizer) { viewModel.closeEqualizer() }
    BackHandler(enabled = !showSettings && !showVideoPlayer && !showVisualizer && !showEqualizer && showPlayer) { viewModel.closePlayer() }
    BackHandler(enabled = !showSettings && !showVideoPlayer && !showVisualizer && !showEqualizer && !showPlayer && searchActive) { viewModel.closeSearch() }
    BackHandler(enabled = !showSettings && !showVideoPlayer && !showVisualizer && !showEqualizer && !showPlayer && !searchActive && editMode) { viewModel.exitEditMode() }
    BackHandler(enabled = !showSettings && !showVideoPlayer && !showVisualizer && !showEqualizer && !showPlayer && !searchActive && !editMode && selectedFolder != null) { viewModel.closeFolder() }

    MusicPlayerTheme(
        accent = Color(effectiveAccent),
        background = Color(effectiveBackground),
    ) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(effectiveBackground)),
    ) {
        HomeScreen(
            songs = displayedSongs,
            folders = folders,
            selectedFolder = selectedFolder,
            tab = tab,
            sortBy = sortBy,
            viewMode = viewMode,
            isLoading = isLoading,
            currentSongId = currentId,
            isPlaying = isPlaying,
            searchActive = searchActive,
            searchQuery = searchQuery,
            contentPadding = scaffoldPadding,
            youtubeQuery = youtubeQuery,
            youtubeResults = youtubeResults,
            youtubeFeed = youtubeFeed,
            youtubeFilter = youtubeFilter,
            youtubeGridView = youtubeGridView,
            youtubeSearching = youtubeSearching,
            youtubeResolving = youtubeResolving,
            youtubeError = youtubeError,
            youtubeHistory = youtubeHistory,
            onTabChange = { viewModel.setTab(it) },
            onSortChange = { viewModel.setSort(it) },
            onToggleViewMode = { viewModel.toggleViewMode() },
            onOpenSearch = { viewModel.openSearch() },
            onCloseSearch = { viewModel.closeSearch() },
            onSearchQueryChange = { viewModel.setSearchQuery(it) },
            onFolderClick = { viewModel.openFolder(it) },
            onFolderBack = { viewModel.closeFolder() },
            onSongClick = { viewModel.playSong(it) },
            onYoutubeSearch = { viewModel.searchYoutube(it) },
            onYoutubeRemoveHistory = { viewModel.removeYoutubeHistory(it) },
            onYoutubeFilterChange = { viewModel.setYoutubeFilter(it) },
            onYoutubeToggleGridView = { viewModel.toggleYoutubeGridView() },
            onYoutubeCloseFeed = { viewModel.clearYoutubeFeed() },
            onYoutubeOpenResult = { viewModel.openYoutubeResult(it) },
            onYoutubePlayAudio = { viewModel.playYoutubeAudio(it) },
            onYoutubePlayVideo = { viewModel.playYoutubeVideo(it) },
            onYoutubePlayAudioFromFeed = { videos, idx ->
                viewModel.playYoutubeAudioFromPlaylist(videos, idx)
            },
            onYoutubePlayVideoFromFeed = { videos, idx ->
                viewModel.playYoutubeVideoFromPlaylist(videos, idx)
            },
            onYoutubeDownloadAudio = { viewModel.downloadYoutubeAudio(it) },
            onYoutubeDownloadVideo = { viewModel.downloadYoutubeVideo(it) },
            onYoutubeDownloadPlaylist = { viewModel.openPlaylistDownloadDialog() },
            youtubePlaylistDownloadLabel = playlistDownload?.let {
                "${it.done + it.failed}/${it.total}"
            },
            onYoutubeLoadMore = { viewModel.loadMoreYoutube() },
            onOpenSettings = { viewModel.openSettings() },
            editMode = editMode,
            selectedIds = selectedIds,
            onEnterEditMode = { id -> viewModel.enterEditMode(id) },
            onExitEditMode = { viewModel.exitEditMode() },
            onToggleSelection = { id -> viewModel.toggleSelection(id) },
            onOpenMoveDialog = { viewModel.openMoveDialog() },
            onReorder = { folderId, from, to -> viewModel.reorderInFolder(folderId, from, to) },
            streamSource = streamSource,
            onStreamSourceChange = { viewModel.setStreamSource(it) },
        )

        if (showMoveDialog) {
            com.chiko.musicplayer.ui.components.MoveToFolderDialog(
                existingFolders = folders,
                currentFolderName = selectedFolder?.name,
                selectedCount = selectedIds.size,
                onDismiss = { viewModel.closeMoveDialog() },
                onConfirm = { name -> viewModel.moveSelectedTo(name) },
            )
        }

        if (showPlaylistDownloadDialog) {
            com.chiko.musicplayer.ui.components.PlaylistDownloadDialog(
                existingFolders = folders,
                trackCount = youtubeFeed?.videos?.size ?: 0,
                suggestedName = youtubeFeed?.title.orEmpty(),
                onDismiss = { viewModel.closePlaylistDownloadDialog() },
                onConfirm = { name -> viewModel.downloadPlaylistTo(name) },
            )
        }

        MiniPlayer(
            song = if (!showPlayer) currentSong else null,
            isPlaying = isPlaying,
            progress = progress,
            onClick = {
                if (currentIsVideo) viewModel.reopenVideoPlayer()
                else viewModel.openPlayer()
            },
            onPlayPause = { viewModel.togglePlayPause() },
            onNext = { viewModel.next() },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .widthIn(max = 720.dp)
                .padding(bottom = scaffoldPadding.calculateBottomPadding())
                .navigationBarsPadding(),
        )

        AnimatedVisibility(
            visible = showPlayer && currentSong != null,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.fillMaxSize(),
        ) {
            currentSong?.let { song ->
                PlayerScreen(
                    song = song,
                    isPlaying = isPlaying,
                    positionMs = positionMs,
                    durationMs = durationMs,
                    shuffle = shuffle,
                    repeatMode = repeatMode,
                    playbackSpeed = playbackSpeed,
                    contentPadding = scaffoldPadding,
                    onClose = { viewModel.closePlayer() },
                    onPlayPause = { viewModel.togglePlayPause() },
                    onNext = { viewModel.next() },
                    onPrevious = { viewModel.previous() },
                    onSeek = { viewModel.seekTo(it) },
                    onToggleShuffle = { viewModel.toggleShuffle() },
                    onCycleRepeat = { viewModel.cycleRepeat() },
                    onOpenEqualizer = { viewModel.openEqualizer() },
                    onOpenVisualizer = { viewModel.openVisualizer() },
                    onSpeedChange = { viewModel.setPlaybackSpeed(it) },
                )
            }
        }

        AnimatedVisibility(
            visible = showEqualizer,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.fillMaxSize(),
        ) {
            EqualizerScreen(
                contentPadding = scaffoldPadding,
                onClose = { viewModel.closeEqualizer() },
            )
        }

        AnimatedVisibility(
            visible = showVisualizer,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize(),
        ) {
            VisualizerScreen(
                contentPadding = scaffoldPadding,
                onClose = { viewModel.closeVisualizer() },
            )
        }

        AnimatedVisibility(
            visible = showVideoPlayer,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize(),
        ) {
            val sharedPlayer = viewModel.getController()
            if (sharedPlayer != null) {
                VideoPlayerScreen(
                    player = sharedPlayer,
                    title = currentSong?.title.orEmpty(),
                    subtitle = currentSong?.artist.orEmpty(),
                    onClose = { viewModel.closeVideoPlayer() },
                )
            }
        }

        AnimatedVisibility(
            visible = showSettings,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.fillMaxSize(),
        ) {
            SettingsScreen(
                accentArgb = accentArgb,
                backgroundArgb = backgroundArgb,
                dynamicFromArt = dynamicFromArt,
                onAccentChange = { viewModel.setAccentColor(it) },
                onBackgroundChange = { viewModel.setBackgroundColor(it) },
                onDynamicFromArtChange = { viewModel.setDynamicFromArt(it) },
                onClose = { viewModel.closeSettings() },
                onClearYoutubeHistory = { viewModel.clearYoutubeHistory() },
                onClearLibraryHistory = { viewModel.clearLibraryHistory() },
                contentPadding = scaffoldPadding,
            )
        }
    }
    }
}
