package com.chiko.musicplayer.ui

import android.app.Application
import android.content.ComponentName
import android.content.IntentSender
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.chiko.musicplayer.audio.extractArtworkColors
import com.chiko.musicplayer.data.Folder
import com.chiko.musicplayer.data.MusicRepository
import com.chiko.musicplayer.data.SearchHistoryStore
import com.chiko.musicplayer.data.SettingsStore
import com.chiko.musicplayer.data.Song
import com.chiko.musicplayer.player.MusicService
import com.chiko.musicplayer.youtube.DownloadCenter
import com.chiko.musicplayer.youtube.SoundCloudRepository
import com.chiko.musicplayer.youtube.StreamSource
import com.chiko.musicplayer.youtube.YoutubeFeed
import com.chiko.musicplayer.youtube.YoutubeFileDownloader
import com.chiko.musicplayer.youtube.YoutubeFilter
import com.chiko.musicplayer.youtube.YoutubeRepository
import com.chiko.musicplayer.youtube.YoutubeResult
import com.chiko.musicplayer.youtube.YoutubeVideo
import org.schabi.newpipe.extractor.Page
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
    Custom("Custom order"),
}

enum class ViewMode { List, Grid }

enum class LibraryTab { Songs, Folders, YouTube }

class MusicViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = MusicRepository(app)
    private val youtubeRepository = YoutubeRepository()
    private val soundcloudRepository = SoundCloudRepository()

    private val _streamSource = MutableStateFlow(StreamSource.YouTube)
    val streamSource: StateFlow<StreamSource> = _streamSource.asStateFlow()

    fun setStreamSource(source: StreamSource) {
        if (_streamSource.value == source) return
        _streamSource.value = source
        _youtubeResults.value = emptyList()
        _youtubeFeed.value = null
        _youtubeQuery.value = ""
        _youtubeError.value = null
        searchNextPage = null
        feedNextPage = null
    }

    fun cycleStreamSource() {
        val values = StreamSource.values()
        val next = values[(values.indexOf(_streamSource.value) + 1) % values.size]
        setStreamSource(next)
    }

    private suspend fun searchFor(source: StreamSource, query: String, filter: YoutubeFilter) =
        when (source) {
            StreamSource.YouTube -> youtubeRepository.search(query, filter)
            StreamSource.SoundCloud -> soundcloudRepository.search(query, filter)
        }

    private suspend fun searchNextFor(source: StreamSource, query: String, filter: YoutubeFilter, page: Page) =
        when (source) {
            StreamSource.YouTube -> youtubeRepository.searchNext(query, filter, page)
            StreamSource.SoundCloud -> soundcloudRepository.searchNext(query, filter, page)
        }

    private suspend fun resolveAudioFor(video: YoutubeVideo) =
        when (video.source) {
            StreamSource.YouTube -> youtubeRepository.resolveAudioStream(video.url)
            StreamSource.SoundCloud -> soundcloudRepository.resolveAudioStream(video.url)
        }

    private suspend fun loadPlaylistFor(source: StreamSource, url: String) =
        when (source) {
            StreamSource.YouTube -> youtubeRepository.loadPlaylist(url)
            StreamSource.SoundCloud -> soundcloudRepository.loadPlaylist(url)
        }

    private suspend fun loadChannelFor(source: StreamSource, url: String) =
        when (source) {
            StreamSource.YouTube -> youtubeRepository.loadChannel(url)
            StreamSource.SoundCloud -> soundcloudRepository.loadChannel(url)
        }

    private suspend fun playlistNextFor(source: StreamSource, url: String, page: Page) =
        when (source) {
            StreamSource.YouTube -> youtubeRepository.playlistNext(url, page)
            StreamSource.SoundCloud -> soundcloudRepository.playlistNext(url, page)
        }
    private val youtubeFileDownloader = YoutubeFileDownloader(app)
    private val downloadCenter = DownloadCenter(app)
    private val youtubeHistoryStore = SearchHistoryStore(app, "youtube")
    private val libraryHistoryStore = SearchHistoryStore(app, "library")
    private val settingsStore = SettingsStore.getInstance(app)
    private val playlistOrderStore = com.chiko.musicplayer.data.PlaylistOrderStore.getInstance(app)

    val accentColor: StateFlow<Int> = settingsStore.accentColor
    val backgroundColor: StateFlow<Int> = settingsStore.backgroundColor
    val dynamicFromArt: StateFlow<Boolean> = settingsStore.dynamicFromArt

    fun setAccentColor(argb: Int) = settingsStore.setAccentColor(argb)
    fun setBackgroundColor(argb: Int) = settingsStore.setBackgroundColor(argb)
    fun setDynamicFromArt(v: Boolean) = settingsStore.setDynamicFromArt(v)

    private val _dynamicColors = MutableStateFlow(
        Pair(settingsStore.accentColor.value, settingsStore.backgroundColor.value)
    )

    val youtubeHistory: StateFlow<List<String>> = youtubeHistoryStore.history
    val libraryHistory: StateFlow<List<String>> = libraryHistoryStore.history

    fun removeYoutubeHistory(query: String) = youtubeHistoryStore.remove(query)
    fun clearYoutubeHistory() = youtubeHistoryStore.clear()
    fun removeLibraryHistory(query: String) = libraryHistoryStore.remove(query)
    fun clearLibraryHistory() = libraryHistoryStore.clear()

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

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _showPlayer = MutableStateFlow(false)
    val showPlayer: StateFlow<Boolean> = _showPlayer.asStateFlow()

    private val _showEqualizer = MutableStateFlow(false)
    val showEqualizer: StateFlow<Boolean> = _showEqualizer.asStateFlow()

    private val _showVisualizer = MutableStateFlow(false)
    val showVisualizer: StateFlow<Boolean> = _showVisualizer.asStateFlow()

    private val _youtubeQuery = MutableStateFlow("")
    val youtubeQuery: StateFlow<String> = _youtubeQuery.asStateFlow()

    private val _youtubeResults = MutableStateFlow<List<YoutubeResult>>(emptyList())
    val youtubeResults: StateFlow<List<YoutubeResult>> = _youtubeResults.asStateFlow()

    private val _youtubeFilter = MutableStateFlow(YoutubeFilter.Videos)
    val youtubeFilter: StateFlow<YoutubeFilter> = _youtubeFilter.asStateFlow()

    val youtubeGridView: StateFlow<Boolean> = settingsStore.youtubeGridView

    private val _youtubeSearching = MutableStateFlow(false)
    val youtubeSearching: StateFlow<Boolean> = _youtubeSearching.asStateFlow()

    private val _youtubeError = MutableStateFlow<String?>(null)
    val youtubeError: StateFlow<String?> = _youtubeError.asStateFlow()

    private val _youtubeCurrent = MutableStateFlow<YoutubeVideo?>(null)
    val youtubeCurrent: StateFlow<YoutubeVideo?> = _youtubeCurrent.asStateFlow()

    private var ytQueue: List<YoutubeVideo> = emptyList()
    private var ytQueueIndex: Int = -1
    @Volatile private var autoAdvancing: Boolean = false
    private val ytAudioStreamCache = mutableMapOf<String, String>()
    private val ytVideoStreamCache = mutableMapOf<String, String>()

    private enum class QueueSource { None, Search, Feed }
    private var queueSource: QueueSource = QueueSource.None
    private var searchNextPage: Page? = null
    private var feedNextPage: Page? = null
    @Volatile private var loadingMore: Boolean = false

    private val _showVideoPlayer = MutableStateFlow(false)
    val showVideoPlayer: StateFlow<Boolean> = _showVideoPlayer.asStateFlow()

    private val _showSettings = MutableStateFlow(false)
    val showSettings: StateFlow<Boolean> = _showSettings.asStateFlow()

    fun openSettings() { _showSettings.value = true }
    fun closeSettings() { _showSettings.value = false }

    private val _currentIsVideo = MutableStateFlow(false)
    val currentIsVideo: StateFlow<Boolean> = _currentIsVideo.asStateFlow()

    private val _youtubeResolving = MutableStateFlow(false)
    val youtubeResolving: StateFlow<Boolean> = _youtubeResolving.asStateFlow()

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

    private val _youtubeFeed = MutableStateFlow<YoutubeFeed?>(null)
    val youtubeFeed: StateFlow<YoutubeFeed?> = _youtubeFeed.asStateFlow()

    val sortBy: StateFlow<SortBy> = settingsStore.sortBy
    val viewMode: StateFlow<ViewMode> = settingsStore.viewMode

    private val _tab = MutableStateFlow(LibraryTab.Songs)
    val tab: StateFlow<LibraryTab> = _tab.asStateFlow()

    private val _selectedFolderId = MutableStateFlow<Long?>(null)
    val selectedFolderId: StateFlow<Long?> = _selectedFolderId.asStateFlow()

    private val _editMode = MutableStateFlow(false)
    val editMode: StateFlow<Boolean> = _editMode.asStateFlow()

    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds.asStateFlow()

    private val _showMoveDialog = MutableStateFlow(false)
    val showMoveDialog: StateFlow<Boolean> = _showMoveDialog.asStateFlow()

    private val _consentRequest = MutableStateFlow<IntentSender?>(null)
    val consentRequest: StateFlow<IntentSender?> = _consentRequest.asStateFlow()

    private var pendingMove: Pair<List<Uri>, String>? = null

    fun enterEditMode(initialId: Long? = null) {
        _editMode.value = true
        _selectedIds.value = if (initialId != null && initialId > 0) setOf(initialId) else emptySet()
    }

    fun exitEditMode() {
        _editMode.value = false
        _selectedIds.value = emptySet()
        _showMoveDialog.value = false
    }

    fun toggleSelection(id: Long) {
        val cur = _selectedIds.value
        _selectedIds.value = if (id in cur) cur - id else cur + id
    }

    fun openMoveDialog() {
        if (_selectedIds.value.isEmpty()) return
        _showMoveDialog.value = true
    }

    fun closeMoveDialog() { _showMoveDialog.value = false }

    fun moveSelectedTo(folderName: String) {
        val ids = _selectedIds.value
        if (ids.isEmpty() || folderName.isBlank()) return
        val songs = _songs.value.filter { it.id in ids }
        if (songs.isEmpty()) return
        val uris = songs.map { it.uri }
        pendingMove = uris to folderName
        _showMoveDialog.value = false
        val sender = repository.buildWriteConsent(uris)
        if (sender != null) {
            _consentRequest.value = sender
        } else {
            performPendingMove()
        }
    }

    fun onConsentResult(granted: Boolean) {
        _consentRequest.value = null
        if (granted) performPendingMove() else pendingMove = null
    }

    private fun performPendingMove() {
        val (uris, folderName) = pendingMove ?: return
        pendingMove = null
        viewModelScope.launch {
            val moved = repository.moveToFolder(uris, folderName)
            if (moved > 0) {
                _toast.value = "Moved $moved track${if (moved == 1) "" else "s"} to \"$folderName\""
                loadSongs()
            } else {
                _toast.value = "Move failed"
            }
            exitEditMode()
        }
    }

    fun reorderInFolder(folderId: Long, fromIndex: Int, toIndex: Int) {
        val ordered = displayedSongs.value.map { it.id }
        playlistOrderStore.moveItem(folderId, fromIndex, toIndex, ordered)
        if (settingsStore.sortBy.value != SortBy.Custom) {
            settingsStore.setSortBy(SortBy.Custom)
        }
    }

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
        _songs, settingsStore.sortBy, _selectedFolderId, _searchQuery, playlistOrderStore.orders,
    ) { all, sort, folderId, query, orders ->
        val byFolder = if (folderId == null) all else all.filter { it.folderId == folderId }
        val q = query.trim().lowercase()
        val byQuery = if (q.isEmpty()) byFolder else byFolder.filter { it.matchesQuery(q) }
        if (sort == SortBy.Custom && folderId != null) {
            val ids = byQuery.map { it.id }
            val order = playlistOrderStore.reconcile(folderId, ids)
            val byId = byQuery.associateBy { it.id }
            order.mapNotNull { byId[it] }
        } else {
            byQuery.sortedWith(comparatorFor(sort))
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val selectedFolder: StateFlow<Folder?> = combine(folders, _selectedFolderId) { fs, id ->
        if (id == null) null else fs.firstOrNull { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val currentSong: StateFlow<Song?> = combine(
        _currentSongId, _songs, _youtubeCurrent,
    ) { id, all, yt ->
        if (yt != null && id == yt.id) yt.toSong() else all.firstOrNull { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val effectiveAccent: StateFlow<Int> = combine(
        settingsStore.dynamicFromArt, settingsStore.accentColor, _dynamicColors,
    ) { dynamic, accent, dyn -> if (dynamic) dyn.first else accent }
        .stateIn(viewModelScope, SharingStarted.Eagerly, settingsStore.accentColor.value)

    val effectiveBackground: StateFlow<Int> = combine(
        settingsStore.dynamicFromArt, settingsStore.backgroundColor, _dynamicColors,
    ) { dynamic, bg, dyn -> if (dynamic) dyn.second else bg }
        .stateIn(viewModelScope, SharingStarted.Eagerly, settingsStore.backgroundColor.value)

    init {
        viewModelScope.launch {
            currentSong.combine(settingsStore.dynamicFromArt) { s, d -> s to d }
                .collect { (song, dynamic) ->
                    if (!dynamic || song == null) {
                        _dynamicColors.value = settingsStore.accentColor.value to
                            settingsStore.backgroundColor.value
                    } else {
                        val colors = extractArtworkColors(
                            getApplication(),
                            song,
                            fallbackAccent = settingsStore.accentColor.value,
                            fallbackBackground = settingsStore.backgroundColor.value,
                        )
                        _dynamicColors.value = colors.accent to colors.background
                    }
                }
        }
    }

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null

    private val mediaObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) { loadSongs() }
    }

    init {
        app.contentResolver.registerContentObserver(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            /* notifyForDescendants = */ true,
            mediaObserver,
        )
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            _currentSongId.value = mediaItem?.mediaId?.toLongOrNull()
            _durationMs.value = controller?.duration?.coerceAtLeast(0L) ?: 0L
            val newMediaId = mediaItem?.mediaId
            if (newMediaId != null && ytQueue.isNotEmpty()) {
                val newIdx = ytQueue.indexOfFirst { it.id.toString() == newMediaId }
                if (newIdx >= 0 && newIdx != ytQueueIndex) {
                    ytQueueIndex = newIdx
                    _youtubeCurrent.value = ytQueue[newIdx]
                    prefetchNextStreams(_currentIsVideo.value)
                    maybeLoadMoreForPlayback()
                }
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY) {
                _durationMs.value = controller?.duration?.coerceAtLeast(0L) ?: 0L
            }
            if (playbackState == Player.STATE_ENDED &&
                !autoAdvancing &&
                ytQueueIndex in 0 until ytQueue.size - 1
            ) {
                autoAdvancing = true
                viewModelScope.launch {
                    next()
                    autoAdvancing = false
                }
            }
        }

        override fun onShuffleModeEnabledChanged(enabled: Boolean) {
            _shuffle.value = enabled
        }

        override fun onRepeatModeChanged(mode: Int) {
            _repeatMode.value = mode
        }

        override fun onPlaybackParametersChanged(params: PlaybackParameters) {
            _playbackSpeed.value = params.speed
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
                _playbackSpeed.value = c.playbackParameters.speed
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
        _youtubeCurrent.value = null
        _currentIsVideo.value = false
        ytQueue = emptyList()
        ytQueueIndex = -1
        _showPlayer.value = true
    }

    fun getController(): MediaController? = controller

    fun searchYoutube(query: String) {
        _youtubeQuery.value = query
        if (query.isBlank()) {
            _youtubeResults.value = emptyList()
            _youtubeFeed.value = null
            _youtubeError.value = null
            return
        }
        val trimmed = query.trim()
        youtubeHistoryStore.add(trimmed)
        when (detectYoutubeUrl(trimmed)) {
            YoutubeUrlKind.Playlist -> loadFeed(trimmed, playlist = true)
            YoutubeUrlKind.Channel -> loadFeed(trimmed, playlist = false)
            else -> performSearch(trimmed)
        }
    }

    private fun performSearch(trimmed: String) {
        viewModelScope.launch {
            _youtubeSearching.value = true
            _youtubeError.value = null
            _youtubeFeed.value = null
            searchNextPage = null
            feedNextPage = null
            try {
                val page = searchFor(_streamSource.value, trimmed, _youtubeFilter.value)
                _youtubeResults.value = page.items
                searchNextPage = page.nextPage
            } catch (t: Throwable) {
                _youtubeError.value = t.message ?: "Search failed"
                _youtubeResults.value = emptyList()
            }
            _youtubeSearching.value = false
        }
    }

    fun loadMoreYoutube() {
        if (loadingMore) return
        val query = _youtubeQuery.value.trim()
        val feed = _youtubeFeed.value
        when {
            feed != null && feedNextPage != null -> loadMoreFeed(feed, feedNextPage!!)
            query.isNotBlank() && searchNextPage != null -> loadMoreSearch(query, searchNextPage!!)
        }
    }

    private fun loadMoreSearch(query: String, token: Page) {
        loadingMore = true
        viewModelScope.launch {
            try {
                val page = searchNextFor(_streamSource.value, query, _youtubeFilter.value, token)
                val merged = _youtubeResults.value + page.items
                _youtubeResults.value = merged
                searchNextPage = page.nextPage
                if (queueSource == QueueSource.Search) {
                    val newVideos = page.items.filterIsInstance<YoutubeResult.Video>().map { it.video }
                    if (newVideos.isNotEmpty()) ytQueue = ytQueue + newVideos
                }
            } catch (t: Throwable) {
                Log.w("Resonance", "loadMoreSearch failed", t)
            } finally {
                loadingMore = false
            }
        }
    }

    private fun loadMoreFeed(feed: YoutubeFeed, token: Page) {
        if (feed.kind != YoutubeFeed.Kind.Playlist) return
        loadingMore = true
        viewModelScope.launch {
            try {
                val (videos, next) = playlistNextFor(_streamSource.value, feed.url, token)
                val updatedFeed = feed.copy(videos = feed.videos + videos, nextPage = next)
                _youtubeFeed.value = updatedFeed
                feedNextPage = next
                if (queueSource == QueueSource.Feed && videos.isNotEmpty()) {
                    ytQueue = ytQueue + videos
                }
            } catch (t: Throwable) {
                Log.w("Resonance", "loadMoreFeed failed", t)
            } finally {
                loadingMore = false
            }
        }
    }

    private fun maybeLoadMoreForPlayback() {
        if (ytQueueIndex < 0) return
        if (ytQueue.size - ytQueueIndex > 4) return
        loadMoreYoutube()
    }

    fun setYoutubeFilter(filter: YoutubeFilter) {
        if (_youtubeFilter.value == filter) return
        _youtubeFilter.value = filter
        val q = _youtubeQuery.value.trim()
        if (q.isNotBlank() && detectYoutubeUrl(q) == YoutubeUrlKind.None) {
            performSearch(q)
        }
    }

    fun toggleYoutubeGridView() {
        settingsStore.setYoutubeGridView(!settingsStore.youtubeGridView.value)
    }

    private data class ResolvedStreamUrl(val url: String)

    private fun buildYoutubeMediaItem(video: YoutubeVideo, streamUrl: String): MediaItem {
        val thumbnailUri = video.thumbnailUrl?.let(Uri::parse)
        return MediaItem.Builder()
            .setMediaId(video.id.toString())
            .setUri(streamUrl)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(video.title)
                    .setArtist(video.uploader)
                    .setAlbumTitle(video.source.label)
                    .setArtworkUri(thumbnailUri)
                    .build()
            )
            .build()
    }

    private fun appendToControllerQueue(video: YoutubeVideo, streamUrl: String) {
        val c = controller ?: return
        val mediaId = video.id.toString()
        val idxInYtQueue = ytQueue.indexOfFirst { it.id.toString() == mediaId }
        if (idxInYtQueue <= ytQueueIndex) return
        for (i in 0 until c.mediaItemCount) {
            if (c.getMediaItemAt(i).mediaId == mediaId) return
        }
        c.addMediaItem(buildYoutubeMediaItem(video, streamUrl))
    }

    /** Resolves the next 2 items sequentially and appends them to the controller queue. */
    private fun prefetchNextStreams(forVideo: Boolean) {
        if (ytQueueIndex < 0) return
        val cache = if (forVideo) ytVideoStreamCache else ytAudioStreamCache
        viewModelScope.launch {
            val startIdx = ytQueueIndex + 1
            val endIdx = minOf(startIdx + 2, ytQueue.size)
            for (i in startIdx until endIdx) {
                if (i <= ytQueueIndex) break
                val v = ytQueue[i]
                val url = cache[v.url] ?: try {
                    val s = if (forVideo) {
                        if (v.source == StreamSource.YouTube) youtubeRepository.resolveVideoStream(v.url)
                        else null
                    } else {
                        resolveAudioFor(v)
                    }
                    s?.url?.also { cache[v.url] = it }
                } catch (_: Throwable) { null }
                if (url != null && i > ytQueueIndex) {
                    appendToControllerQueue(v, url)
                }
            }
        }
    }

    fun openYoutubeResult(result: YoutubeResult) {
        when (result) {
            is YoutubeResult.Video -> playYoutubeAudio(result.video)
            is YoutubeResult.Channel -> loadFeed(result.url, playlist = false)
            is YoutubeResult.Playlist -> loadFeed(result.url, playlist = true)
        }
    }

    private fun loadFeed(url: String, playlist: Boolean) {
        viewModelScope.launch {
            _youtubeSearching.value = true
            _youtubeError.value = null
            _youtubeResults.value = emptyList()
            try {
                val src = _streamSource.value
                _youtubeFeed.value = if (playlist) loadPlaylistFor(src, url)
                else loadChannelFor(src, url)
            } catch (t: Throwable) {
                _youtubeError.value = t.message ?: "Failed to load"
                _youtubeFeed.value = null
            }
            _youtubeSearching.value = false
        }
    }

    fun clearYoutubeFeed() {
        _youtubeFeed.value = null
        _youtubeQuery.value = ""
        _youtubeError.value = null
    }

    private fun detectYoutubeUrl(s: String): YoutubeUrlKind {
        val lower = s.lowercase()
        if (!(lower.startsWith("http://") || lower.startsWith("https://"))) return YoutubeUrlKind.None
        return when (_streamSource.value) {
            StreamSource.YouTube -> {
                if (!(lower.contains("youtube.com") || lower.contains("youtu.be"))) YoutubeUrlKind.None
                else when {
                    lower.contains("playlist?list=") ||
                        lower.contains("?list=") ||
                        lower.contains("&list=") -> YoutubeUrlKind.Playlist
                    lower.contains("/channel/") ||
                        lower.contains("/c/") ||
                        lower.contains("/@") ||
                        lower.contains("/user/") -> YoutubeUrlKind.Channel
                    else -> YoutubeUrlKind.None
                }
            }
            StreamSource.SoundCloud -> {
                if (!lower.contains("soundcloud.com")) YoutubeUrlKind.None
                else when {
                    lower.contains("/sets/") -> YoutubeUrlKind.Playlist
                    // Only users (no track slug after user name):
                    // Heuristic: soundcloud.com/<name> or soundcloud.com/<name>/tracks
                    Regex("^https?://(?:www\\.)?soundcloud\\.com/[^/?#]+/?(?:\\?.*)?$").containsMatchIn(s) -> YoutubeUrlKind.Channel
                    lower.endsWith("/tracks") || lower.endsWith("/tracks/") -> YoutubeUrlKind.Channel
                    else -> YoutubeUrlKind.None
                }
            }
        }
    }

    private enum class YoutubeUrlKind { None, Playlist, Channel }

    fun playYoutubeAudio(video: YoutubeVideo) {
        ytQueue = emptyList()
        ytQueueIndex = -1
        queueSource = QueueSource.None
        playYoutubeAudioInternal(video)
    }

    fun playYoutubeAudioFromPlaylist(videos: List<YoutubeVideo>, index: Int) {
        if (index !in videos.indices) return
        ytQueue = videos
        ytQueueIndex = index
        queueSource = if (_youtubeFeed.value != null) QueueSource.Feed else QueueSource.Search
        playYoutubeAudioInternal(videos[index])
    }

    fun playYoutubeVideoFromPlaylist(videos: List<YoutubeVideo>, index: Int) {
        if (index !in videos.indices) return
        ytQueue = videos
        ytQueueIndex = index
        queueSource = if (_youtubeFeed.value != null) QueueSource.Feed else QueueSource.Search
        playYoutubeVideoInternal(videos[index])
    }

    private fun playYoutubeAudioInternal(video: YoutubeVideo) {
        Log.d("Resonance", "playYoutubeAudio tap: ${video.title}")
        val c = controller ?: run {
            Log.w("Resonance", "playYoutubeAudio: controller null")
            _toast.value = "Player not ready yet"
            return
        }
        viewModelScope.launch {
            val cached = ytAudioStreamCache[video.url]
            val stream = if (cached != null) {
                Log.d("Resonance", "using cached audio stream for ${video.title}")
                ResolvedStreamUrl(cached)
            } else {
                _youtubeResolving.value = true
                val resolved = try {
                    resolveAudioFor(video)
                } catch (t: Throwable) {
                    Log.e("Resonance", "playYoutubeAudio resolve failed", t)
                    _toast.value = t.message ?: "Unable to load audio"
                    null
                } finally {
                    _youtubeResolving.value = false
                } ?: return@launch
                ytAudioStreamCache[video.url] = resolved.url
                ResolvedStreamUrl(resolved.url)
            }
            Log.d("Resonance", "playYoutubeAudio streaming: ${stream.url.take(80)}")
            val thumbnailUri = video.thumbnailUrl?.let(Uri::parse)
            val mediaItem = MediaItem.Builder()
                .setMediaId(video.id.toString())
                .setUri(stream.url)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(video.title)
                        .setArtist(video.uploader)
                        .setAlbumTitle(video.source.label)
                        .setArtworkUri(thumbnailUri)
                        .build()
                )
                .build()
            c.setMediaItems(listOf(mediaItem), 0, 0L)
            c.prepare()
            c.play()
            _youtubeCurrent.value = video
            _currentSongId.value = video.id
            _currentIsVideo.value = false
            _showPlayer.value = true
            prefetchNextStreams(forVideo = false)
        }
    }

    fun playYoutubeVideo(video: YoutubeVideo) {
        if (video.source != StreamSource.YouTube) {
            _toast.value = "Video playback is YouTube-only"
            return
        }
        ytQueue = emptyList()
        ytQueueIndex = -1
        queueSource = QueueSource.None
        playYoutubeVideoInternal(video)
    }

    private fun playYoutubeVideoInternal(video: YoutubeVideo) {
        val c = controller ?: run {
            _toast.value = "Player not ready yet"
            return
        }
        Log.d("Resonance", "playYoutubeVideo tap: ${video.title}")
        viewModelScope.launch {
            val cached = ytVideoStreamCache[video.url]
            val stream = if (cached != null) {
                ResolvedStreamUrl(cached)
            } else {
                _youtubeResolving.value = true
                val resolved = try {
                    youtubeRepository.resolveVideoStream(video.url)
                } catch (t: Throwable) {
                    Log.e("Resonance", "playYoutubeVideo resolve failed", t)
                    _toast.value = t.message ?: "Unable to load video"
                    null
                } finally {
                    _youtubeResolving.value = false
                } ?: return@launch
                ytVideoStreamCache[video.url] = resolved.url
                ResolvedStreamUrl(resolved.url)
            }
            Log.d("Resonance", "playYoutubeVideo streaming: ${stream.url.take(80)}")
            val thumbnailUri = video.thumbnailUrl?.let(Uri::parse)
            val mediaItem = MediaItem.Builder()
                .setMediaId(video.id.toString())
                .setUri(stream.url)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(video.title)
                        .setArtist(video.uploader)
                        .setAlbumTitle(video.source.label)
                        .setArtworkUri(thumbnailUri)
                        .build()
                )
                .build()
            c.setMediaItems(listOf(mediaItem), 0, 0L)
            c.prepare()
            c.play()
            _youtubeCurrent.value = video
            _currentSongId.value = video.id
            _currentIsVideo.value = true
            _showVideoPlayer.value = true
            prefetchNextStreams(forVideo = true)
        }
    }

    fun closeVideoPlayer() {
        _showVideoPlayer.value = false
    }

    fun reopenVideoPlayer() {
        if (_currentIsVideo.value) _showVideoPlayer.value = true
    }

    fun downloadYoutubeAudio(video: YoutubeVideo) {
        Log.d("Resonance", "downloadAudio tap: ${video.title} (${video.source.label})")
        viewModelScope.launch {
            _youtubeResolving.value = true
            val stream = try {
                resolveAudioFor(video)
            } catch (t: Throwable) {
                Log.e("Resonance", "downloadAudio resolve failed", t)
                _toast.value = t.message ?: "Unable to resolve audio"
                null
            } finally {
                _youtubeResolving.value = false
            } ?: return@launch
            val mime = stream.mimeType.orEmpty().lowercase()
            if (mime.contains("mpegurl") || stream.url.contains(".m3u8")) {
                _toast.value = "HLS download not supported yet"
                return@launch
            }
            _toast.value = "Starting download…"
            downloadCenter.downloadAudio(video, stream.url) { ok ->
                viewModelScope.launch {
                    _toast.value = if (ok) "Saved to Music with cover art" else "Download failed"
                    if (ok) loadSongs()
                }
            }
        }
    }

    fun downloadYoutubeVideo(video: YoutubeVideo) {
        if (video.source != StreamSource.YouTube) {
            _toast.value = "Video download is YouTube-only"
            return
        }
        Log.d("Resonance", "downloadYoutubeVideo tap: ${video.title}")
        viewModelScope.launch {
            _youtubeResolving.value = true
            val stream = try {
                youtubeRepository.resolveVideoStream(video.url)
            } catch (t: Throwable) {
                Log.e("Resonance", "downloadYoutubeVideo resolve failed", t)
                _toast.value = t.message ?: "Unable to resolve video"
                null
            } finally {
                _youtubeResolving.value = false
            } ?: return@launch
            val id = youtubeFileDownloader.downloadVideo(video, stream.url)
            Log.d("Resonance", "downloadYoutubeVideo enqueued id=$id")
            _toast.value = "Video download started"
        }
    }

    fun consumeToast() { _toast.value = null }

    fun togglePlayPause() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else {
            if (c.playbackState == Player.STATE_IDLE) c.prepare()
            c.play()
        }
    }

    fun next() {
        val c = controller ?: return
        if (c.hasNextMediaItem()) {
            c.seekToNext()
            return
        }
        if (ytQueueIndex in 0 until ytQueue.size - 1) {
            ytQueueIndex++
            val v = ytQueue[ytQueueIndex]
            if (_currentIsVideo.value) playYoutubeVideoInternal(v)
            else playYoutubeAudioInternal(v)
        }
    }

    fun previous() {
        val c = controller ?: return
        if (c.currentPosition > 3000) {
            c.seekTo(0)
            return
        }
        if (c.hasPreviousMediaItem()) {
            c.seekToPrevious()
            return
        }
        if (ytQueueIndex > 0) {
            ytQueueIndex--
            val v = ytQueue[ytQueueIndex]
            if (_currentIsVideo.value) playYoutubeVideoInternal(v)
            else playYoutubeAudioInternal(v)
        } else {
            c.seekTo(0)
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

    fun setPlaybackSpeed(speed: Float) {
        val s = speed.coerceIn(0.25f, 2.0f)
        controller?.setPlaybackSpeed(s)
        _playbackSpeed.value = s
    }

    fun openPlayer() { _showPlayer.value = true }
    fun closePlayer() { _showPlayer.value = false }

    fun openEqualizer() { _showEqualizer.value = true }
    fun closeEqualizer() { _showEqualizer.value = false }

    fun openVisualizer() { _showVisualizer.value = true }
    fun closeVisualizer() { _showVisualizer.value = false }

    fun setSort(sort: SortBy) = settingsStore.setSortBy(sort)
    fun setViewMode(mode: ViewMode) = settingsStore.setViewMode(mode)
    fun toggleViewMode() {
        settingsStore.setViewMode(
            if (settingsStore.viewMode.value == ViewMode.List) ViewMode.Grid else ViewMode.List
        )
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
    fun setSearchQuery(q: String) {
        _searchQuery.value = q
        val trimmed = q.trim()
        if (trimmed.isNotBlank() && trimmed.length >= 2) libraryHistoryStore.add(trimmed)
    }

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
        SortBy.Custom -> compareBy { it.title.lowercase() }
    }

    override fun onCleared() {
        getApplication<Application>().contentResolver.unregisterContentObserver(mediaObserver)
        controller?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        controller = null
        super.onCleared()
    }
}
