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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chiko.musicplayer.ui.MusicViewModel
import com.chiko.musicplayer.ui.components.MiniPlayer
import com.chiko.musicplayer.ui.screens.EqualizerScreen
import com.chiko.musicplayer.ui.screens.HomeScreen
import com.chiko.musicplayer.ui.screens.PermissionScreen
import com.chiko.musicplayer.ui.screens.PlayerScreen
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

@OptIn(ExperimentalPermissionsApi::class)
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

@Composable
private fun PlayerHost(scaffoldPadding: PaddingValues) {
    val viewModel: MusicViewModel = viewModel()

    LaunchedEffect(Unit) {
        viewModel.connect()
        viewModel.loadSongs()
    }

    val displayedSongs by viewModel.displayedSongs.collectAsState()
    val allSongs by viewModel.songs.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val selectedFolder by viewModel.selectedFolder.collectAsState()
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
    val showPlayer by viewModel.showPlayer.collectAsState()
    val showEqualizer by viewModel.showEqualizer.collectAsState()
    val searchActive by viewModel.searchActive.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val currentSong = allSongs.firstOrNull { it.id == currentId }
    val progress = if (durationMs > 0) positionMs.toFloat() / durationMs.toFloat() else 0f

    BackHandler(enabled = showEqualizer) { viewModel.closeEqualizer() }
    BackHandler(enabled = !showEqualizer && showPlayer) { viewModel.closePlayer() }
    BackHandler(enabled = !showEqualizer && !showPlayer && searchActive) { viewModel.closeSearch() }
    BackHandler(enabled = !showEqualizer && !showPlayer && !searchActive && selectedFolder != null) { viewModel.closeFolder() }

    Box(modifier = Modifier.fillMaxSize()) {
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
            onTabChange = { viewModel.setTab(it) },
            onSortChange = { viewModel.setSort(it) },
            onToggleViewMode = { viewModel.toggleViewMode() },
            onOpenSearch = { viewModel.openSearch() },
            onCloseSearch = { viewModel.closeSearch() },
            onSearchQueryChange = { viewModel.setSearchQuery(it) },
            onFolderClick = { viewModel.openFolder(it) },
            onFolderBack = { viewModel.closeFolder() },
            onSongClick = { viewModel.playSong(it) },
        )

        MiniPlayer(
            song = if (!showPlayer) currentSong else null,
            isPlaying = isPlaying,
            progress = progress,
            onClick = { viewModel.openPlayer() },
            onPlayPause = { viewModel.togglePlayPause() },
            onNext = { viewModel.next() },
            modifier = Modifier
                .align(Alignment.BottomCenter)
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
                    contentPadding = scaffoldPadding,
                    onClose = { viewModel.closePlayer() },
                    onPlayPause = { viewModel.togglePlayPause() },
                    onNext = { viewModel.next() },
                    onPrevious = { viewModel.previous() },
                    onSeek = { viewModel.seekTo(it) },
                    onToggleShuffle = { viewModel.toggleShuffle() },
                    onCycleRepeat = { viewModel.cycleRepeat() },
                    onOpenEqualizer = { viewModel.openEqualizer() },
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
    }
}
