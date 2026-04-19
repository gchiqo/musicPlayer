package com.chiko.musicplayer.ui

import android.app.Application
import android.content.ComponentName
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.chiko.musicplayer.data.Folder
import com.chiko.musicplayer.data.MusicRepository
import com.chiko.musicplayer.data.Song
import com.chiko.musicplayer.player.MusicService
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SortBy(val label: String) {
    Title("Title"),
    Artist("Artist"),
    Album("Album"),
    Duration("Duration"),
    DateNewest("Newest first"),
    DateOldest("Oldest first"),
}

enum class ViewMode { List, Grid }

enum class LibraryTab { Songs, Folders }

class MusicViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = MusicRepository(app)

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _currentSongId = MutableStateFlow<Long?>(null)
    val currentSongId: StateFlow<Long?> = _currentSongId.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _shuffle = MutableStateFlow(false)
    val shuffle: StateFlow<Boolean> = _shuffle.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val _showPlayer = MutableStateFlow(false)
    val showPlayer: StateFlow<Boolean> = _showPlayer.asStateFlow()

    private val _sortBy = MutableStateFlow(SortBy.Title)
    val sortBy: StateFlow<SortBy> = _sortBy.asStateFlow()

    private val _viewMode = MutableStateFlow(ViewMode.List)
    val viewMode: StateFlow<ViewMode> = _viewMode.asStateFlow()

    private val _tab = MutableStateFlow(LibraryTab.Songs)
    val tab: StateFlow<LibraryTab> = _tab.asStateFlow()

    private val _selectedFolderId = MutableStateFlow<Long?>(null)
    val selectedFolderId: StateFlow<Long?> = _selectedFolderId.asStateFlow()

    private val _searchActive = MutableStateFlow(false)
    val searchActive: StateFlow<Boolean> = _searchActive.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val folders: StateFlow<List<Folder>> = combine(_songs, _searchQuery) { all, query ->
        val q = query.trim().lowercase()
        val filtered = if (q.isEmpty()) all
        else all.filter { it.matchesQuery(q) || it.folderName.lowercase().contains(q) }
        filtered.groupBy { it.folderId }
            .map { (id, list) -> Folder(id, list.first().folderName, list.size, list.firstOrNull()) }
            .sortedBy { it.name.lowercase() }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val displayedSongs: StateFlow<List<Song>> = combine(
        _songs, _sortBy, _selectedFolderId, _searchQuery,
    ) { all, sort, folderId, query ->
        val byFolder = if (folderId == null) all else all.filter { it.folderId == folderId }
        val q = query.trim().lowercase()
        val byQuery = if (q.isEmpty()) byFolder else byFolder.filter { it.matchesQuery(q) }
        byQuery.sortedWith(comparatorFor(sort))
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val selectedFolder: StateFlow<Folder?> = combine(folders, _selectedFolderId) { fs, id ->
        if (id == null) null else fs.firstOrNull { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            _currentSongId.value = mediaItem?.mediaId?.toLongOrNull()
            _durationMs.value = controller?.duration?.coerceAtLeast(0L) ?: 0L
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY) {
                _durationMs.value = controller?.duration?.coerceAtLeast(0L) ?: 0L
            }
        }

        override fun onShuffleModeEnabledChanged(enabled: Boolean) {
            _shuffle.value = enabled
        }

        override fun onRepeatModeChanged(mode: Int) {
            _repeatMode.value = mode
        }
    }

    fun connect() {
        if (controllerFuture != null) return
        val app = getApplication<Application>()
        val token = SessionToken(app, ComponentName(app, MusicService::class.java))
        val future = MediaController.Builder(app, token).buildAsync()
        controllerFuture = future
        future.addListener({
            controller = future.get().also { it.addListener(playerListener) }
            controller?.let { c ->
                _isPlaying.value = c.isPlaying
                _currentSongId.value = c.currentMediaItem?.mediaId?.toLongOrNull()
                _durationMs.value = c.duration.coerceAtLeast(0L)
                _shuffle.value = c.shuffleModeEnabled
                _repeatMode.value = c.repeatMode
            }
            startPositionLoop()
        }, MoreExecutors.directExecutor())
    }

    private fun startPositionLoop() {
        viewModelScope.launch {
            while (true) {
                controller?.let { c ->
                    _positionMs.value = c.currentPosition.coerceAtLeast(0L)
                    if (_durationMs.value <= 0L) {
                        _durationMs.value = c.duration.coerceAtLeast(0L)
                    }
                }
                delay(500)
            }
        }
    }

    fun loadSongs() {
        viewModelScope.launch {
            _isLoading.value = true
            _songs.value = repository.loadSongs()
            _isLoading.value = false
        }
    }

    fun playSong(song: Song) {
        val c = controller ?: return
        val list = displayedSongs.value
        val index = list.indexOfFirst { it.id == song.id }.takeIf { it >= 0 } ?: return
        val items = list.map { it.toMediaItem() }
        c.setMediaItems(items, index, 0L)
        c.prepare()
        c.play()
        _showPlayer.value = true
    }

    fun togglePlayPause() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else {
            if (c.playbackState == Player.STATE_IDLE) c.prepare()
            c.play()
        }
    }

    fun next() {
        controller?.takeIf { it.hasNextMediaItem() }?.seekToNext()
    }

    fun previous() {
        val c = controller ?: return
        if (c.currentPosition > 3000 || !c.hasPreviousMediaItem()) {
            c.seekTo(0)
        } else {
            c.seekToPrevious()
        }
    }

    fun seekTo(ms: Long) {
        controller?.seekTo(ms)
        _positionMs.value = ms
    }

    fun toggleShuffle() {
        val c = controller ?: return
        c.shuffleModeEnabled = !c.shuffleModeEnabled
    }

    fun cycleRepeat() {
        val c = controller ?: return
        c.repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun openPlayer() { _showPlayer.value = true }
    fun closePlayer() { _showPlayer.value = false }

    fun setSort(sort: SortBy) { _sortBy.value = sort }
    fun setViewMode(mode: ViewMode) { _viewMode.value = mode }
    fun toggleViewMode() {
        _viewMode.value = if (_viewMode.value == ViewMode.List) ViewMode.Grid else ViewMode.List
    }
    fun setTab(t: LibraryTab) {
        _tab.value = t
        if (t == LibraryTab.Songs) _selectedFolderId.value = null
    }
    fun openFolder(folder: Folder) { _selectedFolderId.value = folder.id }
    fun closeFolder() { _selectedFolderId.value = null }

    fun openSearch() { _searchActive.value = true }
    fun closeSearch() {
        _searchActive.value = false
        _searchQuery.value = ""
    }
    fun setSearchQuery(q: String) { _searchQuery.value = q }

    private fun Song.matchesQuery(needle: String): Boolean =
        title.lowercase().contains(needle) ||
            artist.lowercase().contains(needle) ||
            album.lowercase().contains(needle)

    private fun comparatorFor(sort: SortBy): Comparator<Song> = when (sort) {
        SortBy.Title -> compareBy { it.title.lowercase() }
        SortBy.Artist -> compareBy({ it.artist.lowercase() }, { it.title.lowercase() })
        SortBy.Album -> compareBy({ it.album.lowercase() }, { it.title.lowercase() })
        SortBy.Duration -> compareBy { it.durationMs }
        SortBy.DateNewest -> compareByDescending { it.dateAddedSec }
        SortBy.DateOldest -> compareBy { it.dateAddedSec }
    }

    override fun onCleared() {
        controller?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        controller = null
        super.onCleared()
    }
}
