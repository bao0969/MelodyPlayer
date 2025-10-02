package com.example.melodyplayer.player

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.melodyplayer.model.Song
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlayerViewModel(app: Application) : AndroidViewModel(app) {

    private val TAG = "PlayerViewModel"

    // ExoPlayer instance
    val player: ExoPlayer = ExoPlayer.Builder(app.applicationContext).build().apply {
        // Configure player
        playWhenReady = true

        // Add listener
        addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                Log.d(TAG, "▶️ Player state changed: isPlaying = $isPlaying")
                _isPlaying.value = isPlaying
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                val stateString = when (playbackState) {
                    Player.STATE_IDLE -> "IDLE"
                    Player.STATE_BUFFERING -> "BUFFERING"
                    Player.STATE_READY -> "READY"
                    Player.STATE_ENDED -> "ENDED"
                    else -> "UNKNOWN"
                }
                Log.d(TAG, "🎵 Playback state: $stateString")

                if (playbackState == Player.STATE_ENDED) {
                    nextSong()
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                Log.e(TAG, "❌ Player error: ${error.message}")
            }
        })
    }

    private val _playlist = MutableStateFlow<List<Song>>(emptyList())
    val playlist: StateFlow<List<Song>> = _playlist.asStateFlow()

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private var currentIndex = 0

    init {
        Log.d(TAG, "🎬 PlayerViewModel initialized")
    }

    fun setPlaylist(songs: List<Song>, startIndex: Int = 0) {
        Log.d(TAG, "📝 Setting playlist: ${songs.size} songs, start at $startIndex")

        if (songs.isEmpty()) {
            Log.w(TAG, "⚠️ Empty playlist!")
            return
        }

        _playlist.value = songs
        currentIndex = startIndex.coerceIn(0, songs.lastIndex)

        val song = songs[currentIndex]
        Log.d(TAG, "🎵 First song: ${song.title}")
        Log.d(TAG, "🔗 URL: ${song.audioUrl}")

        playSong(currentIndex)
    }

    private fun playSong(index: Int) {
        val songs = _playlist.value
        if (songs.isEmpty() || index !in songs.indices) {
            Log.w(TAG, "⚠️ Invalid index: $index")
            return
        }

        val song = songs[index]
        _currentSong.value = song
        currentIndex = index

        Log.d(TAG, "▶️ Playing: ${song.title}")
        Log.d(TAG, "🔗 URL: ${song.audioUrl}")

        try {
            // Stop current playback
            player.stop()

            // Set new media item
            val mediaItem = MediaItem.fromUri(song.audioUrl)
            player.setMediaItem(mediaItem)

            // Prepare and play
            player.prepare()
            player.playWhenReady = true

            Log.d(TAG, "✅ Media item set and preparing...")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error playing song: ${e.message}", e)
        }
    }

    fun togglePlayPause() {
        Log.d(TAG, "🎛️ Toggle Play/Pause - Current: ${player.isPlaying}")

        try {
            if (player.isPlaying) {
                player.pause()
                Log.d(TAG, "⏸️ Paused")
            } else {
                player.play()
                Log.d(TAG, "▶️ Playing")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error toggling playback: ${e.message}", e)
        }
    }

    fun nextSong() {
        val songs = _playlist.value
        if (songs.isEmpty()) return

        currentIndex = (currentIndex + 1) % songs.size
        Log.d(TAG, "⏭️ Next song: index $currentIndex")
        playSong(currentIndex)
    }

    fun prevSong() {
        val songs = _playlist.value
        if (songs.isEmpty()) return

        currentIndex = if (currentIndex > 0) currentIndex - 1 else songs.lastIndex
        Log.d(TAG, "⏮️ Previous song: index $currentIndex")
        playSong(currentIndex)
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "🛑 Releasing player")
        player.release()
    }
}