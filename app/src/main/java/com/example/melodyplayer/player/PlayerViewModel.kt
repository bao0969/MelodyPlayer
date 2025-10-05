package com.example.melodyplayer.player

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.webkit.URLUtil
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.melodyplayer.model.Song
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.guava.await
import kotlin.math.max
import com.example.melodyplayer.player.service.PlaybackService


@OptIn(UnstableApi::class)
class PlayerViewModel(app: Application) : AndroidViewModel(app) {

    private val TAG = "PlayerViewModel"
    private val context = app.applicationContext
    private val controllerFuture: ListenableFuture<MediaController>
    private var controller: MediaController? = null
    private var positionJob: Job? = null

    private val mediaIdToSong = mutableMapOf<String, Song>()
    private val fallbackMediaIds = mutableSetOf<String>()

    private val _playlist = MutableStateFlow<List<Song>>(emptyList())
    val playlist: StateFlow<List<Song>> = _playlist.asStateFlow()

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackPosition = MutableStateFlow(0L)
    val playbackPosition: StateFlow<Long> = _playbackPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _bufferedPosition = MutableStateFlow(0L)
    val bufferedPosition: StateFlow<Long> = _bufferedPosition.asStateFlow()

    private val _shuffleEnabled = MutableStateFlow(false)
    val shuffleEnabled: StateFlow<Boolean> = _shuffleEnabled.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val _playbackError = MutableStateFlow<String?>(null)
    val playbackError: StateFlow<String?> = _playbackError.asStateFlow()

    init {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, PlaybackService::class.java)
        )
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture.addListener(
            {
                try {
                    val controllerInstance = controllerFuture.get()
                    setupController(controllerInstance)
                } catch (e: Exception) {
                    Log.e(TAG, "Không thể khởi tạo MediaController", e)
                    _playbackError.value = "Không thể kết nối đến trình phát"
                }
            },
            ContextCompat.getMainExecutor(context)
        )
    }

    fun setPlaylist(songs: List<Song>, startIndex: Int = 0) {
        if (songs.isEmpty()) {
            _playbackError.value = "Danh sách phát trống"
            return
        }

        if (_playlist.value == songs) {
            playSong(startIndex)
            return
        }

        viewModelScope.launch {
            val controller = awaitController() ?: return@launch
            ensureServiceRunning()

            val safeIndex = startIndex.coerceIn(0, songs.lastIndex)
            val mediaItems = buildMediaItems(songs)

            try {
                controller.setMediaItems(mediaItems, safeIndex, 0L)
                controller.prepare()
                controller.play()

                _playlist.value = songs
                _currentSong.value = songs[safeIndex]
                _playbackError.value = fallbackWarningFor(mediaItems[safeIndex].mediaId)
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi khi thiết lập danh sách phát", e)
                _playbackError.value = "Không thể phát danh sách nhạc"
            }
        }
    }

    fun playSong(index: Int) {
        val songs = _playlist.value
        if (songs.isEmpty() || index !in songs.indices) return

        viewModelScope.launch {
            val controller = awaitController() ?: return@launch
            ensureServiceRunning()
            try {
                val targetMediaId = runCatching { controller.getMediaItemAt(index).mediaId }.getOrNull()
                controller.seekTo(index, 0L)
                controller.play()
                _currentSong.value = songs[index]
                _playbackError.value = fallbackWarningFor(targetMediaId)
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi khi chuyển bài", e)
                _playbackError.value = "Không thể phát bài hát đã chọn"
            }
        }
    }

    fun togglePlayPause() {
        controller?.let {
            try {
                if (it.isPlaying) {
                    it.pause()
                } else {
                    it.play()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi khi tạm dừng/phát", e)
                _playbackError.value = "Không thể điều khiển phát nhạc"
            }
        }
    }

    fun nextSong() {
        controller?.let {
            try {
                it.seekToNextMediaItem()
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi khi chuyển sang bài tiếp theo", e)
            }
        }
    }

    fun prevSong() {
        controller?.let {
            try {
                it.seekToPreviousMediaItem()
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi khi quay lại bài trước", e)
            }
        }
    }

    fun seekTo(positionMs: Long) {
        controller?.let {
            try {
                it.seekTo(positionMs)
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi khi tua bài hát", e)
            }
        }
    }

    fun toggleShuffle() {
        controller?.let {
            try {
                val enabled = !it.shuffleModeEnabled
                it.shuffleModeEnabled = enabled
                _shuffleEnabled.value = enabled
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi khi đổi chế độ phát ngẫu nhiên", e)
            }
        }
    }

    fun cycleRepeatMode() {
        controller?.let {
            try {
                val newMode = when (it.repeatMode) {
                    Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                    Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                    else -> Player.REPEAT_MODE_OFF
                }
                it.repeatMode = newMode
                _repeatMode.value = newMode
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi khi đổi chế độ lặp", e)
            }
        }
    }

    fun clearError() {
        _playbackError.value = null
    }

    override fun onCleared() {
        super.onCleared()
        positionJob?.cancel()
        controller?.release()
        controller = null
        controllerFuture.cancel(true)
    }

    private suspend fun awaitController(): MediaController? {
        controller?.let { return it }
        return try {
            val controllerInstance = controllerFuture.await()
            if (controller == null) setupController(controllerInstance)
            controllerInstance
        } catch (e: Exception) {
            Log.e(TAG, "Không thể kết nối đến MediaController", e)
            _playbackError.value = "Không thể kết nối đến trình phát"
            null
        }
    }

    private fun setupController(controllerInstance: MediaController) {
        controller = controllerInstance
        _isPlaying.value = controllerInstance.isPlaying
        _shuffleEnabled.value = controllerInstance.shuffleModeEnabled
        _repeatMode.value = controllerInstance.repeatMode

        controllerInstance.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_IDLE && controllerInstance.playerError != null) {
                    onPlayerError(controllerInstance.playerError!!)
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val song = mediaItem?.mediaId?.let { mediaIdToSong[it] }
                _currentSong.value = song
                _playbackError.value = fallbackWarningFor(mediaItem?.mediaId)
            }

            override fun onPlayerError(error: PlaybackException) {
                onPlayerError(error)
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                _shuffleEnabled.value = shuffleModeEnabled
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                _repeatMode.value = repeatMode
            }
        })

        positionJob?.cancel()
        positionJob = viewModelScope.launch {
            while (isActive) {
                val duration = controllerInstance.duration
                val buffered = controllerInstance.bufferedPosition
                _playbackPosition.value = max(0L, controllerInstance.currentPosition)
                _duration.value = if (duration != C.TIME_UNSET && duration > 0) duration else 0L
                _bufferedPosition.value = if (buffered != C.TIME_UNSET && buffered > 0) buffered else 0L
                delay(500)
            }
        }
    }

    private fun onPlayerError(error: PlaybackException) {
        Log.e(TAG, "Lỗi phát nhạc", error)
        _playbackError.value = error.localizedMessage ?: "Đã xảy ra lỗi khi phát nhạc"
    }

    private fun ensureServiceRunning() {
        try {
            val intent = Intent(context, PlaybackService::class.java)
            ContextCompat.startForegroundService(context, intent)
        } catch (e: IllegalStateException) {
            Log.w(TAG, "Không thể khởi chạy service phát nhạc ở chế độ nền", e)
        } catch (e: SecurityException) {
            Log.w(TAG, "Thiếu quyền để khởi chạy service phát nhạc", e)
        }
    }

    private fun buildMediaItems(songs: List<Song>): List<MediaItem> {
        mediaIdToSong.clear()
        fallbackMediaIds.clear()

        return songs.mapIndexed { index, song ->
            val (mediaItem, usedFallback) = createMediaItem(song, index)
            mediaIdToSong[mediaItem.mediaId] = song
            if (usedFallback) fallbackMediaIds.add(mediaItem.mediaId)
            mediaItem
        }
    }

    private fun createMediaItem(song: Song, index: Int): Pair<MediaItem, Boolean> {
        val trimmedUrl = song.audioUrl.trim()
        val hasValidUrl = trimmedUrl.isNotEmpty() && URLUtil.isValidUrl(trimmedUrl)
        val uri = if (hasValidUrl) Uri.parse(trimmedUrl)
        else Uri.parse("android.resource://${context.packageName}/raw/vung_la_me_bay")

        val metadata = MediaMetadata.Builder()
            .setTitle(song.title.ifBlank { "Unknown" })
            .setArtist(song.artist.ifBlank { null })
            .apply {
                song.coverUrl?.takeIf { it.isNotBlank() }?.let { cover ->
                    runCatching { Uri.parse(cover) }
                        .onSuccess { setArtworkUri(it) }
                        .onFailure { Log.w(TAG, "Không thể phân tích ảnh bìa cho ${song.title}", it) }
                }
            }
            .build()

        val mediaId = if (hasValidUrl) {
            "remote-$index-${song.title}"
        } else {
            "fallback-$index-${song.title}"
        }

        val mediaItem = MediaItem.Builder()
            .setMediaId(mediaId)
            .setUri(uri)
            .setMediaMetadata(metadata)
            .build()

        return mediaItem to !hasValidUrl
    }

    private fun fallbackWarningFor(mediaId: String?): String? {
        if (mediaId == null || !fallbackMediaIds.contains(mediaId)) return null
        val song = mediaIdToSong[mediaId]
        return song?.let { "Bài hát \"${it.title}\" chưa có liên kết phát – đang sử dụng bản mẫu." }
    }
}
