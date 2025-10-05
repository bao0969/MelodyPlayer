@file:OptIn(androidx.media3.common.util.UnstableApi::class)

package com.example.melodyplayer.player.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.ui.PlayerNotificationManager
import com.example.melodyplayer.MainActivity
import com.example.melodyplayer.R

class PlaybackService : MediaSessionService() {

    private lateinit var player: ExoPlayer
    private var mediaSession: MediaSession? = null
    private var notificationManager: PlayerNotificationManager? = null

    override fun onCreate() {
        super.onCreate()

        // 1️⃣ Tạo ExoPlayer
        player = ExoPlayer.Builder(this).build()

        // 2️⃣ Tạo danh sách bài hát
        val songs = listOf(
            Triple("Bốn chữ lắm", "Trúc Nhân", R.raw.bon_chu_lam),
            Triple("Đừng trách câu vì đậm", "Cáp Anh Tài", R.raw.dung_trach_cau_vi_dam),
            Triple("Em là cô giáo vùng cao", "Sên Hoàng Mỹ Lam", R.raw.em_la_co_giao_vung_cao),
            Triple("Ngắm hoa lệ rơi", "Châu Khải Phong", R.raw.ngam_hoa_le_roi),
            Triple("Original Me", "Astra Yao", R.raw.original_me),
            Triple("Vùng lá me bay", "Như Quỳnh", R.raw.vung_la_me_bay),
            Triple("Xin lỗi người anh yêu", "Châu Khải Phong", R.raw.xin_loi_nguoi_anh_yeu)
        )

        // 3️⃣ Thêm tất cả bài hát vào player
        for ((title, artist, resId) in songs) {
            val uri = Uri.parse("android.resource://${packageName}/$resId")
            val mediaItem = MediaItem.Builder()
                .setUri(uri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(title)
                        .setArtist(artist)
                        .build()
                )
                .build()
            player.addMediaItem(mediaItem)
        }

        player.prepare()
        player.play()

        // 4️⃣ Tạo MediaSession
        mediaSession = MediaSession.Builder(this, player)
            .setId("MelodyPlayerSession")
            .build()

        // 5️⃣ Tạo Notification Channel
        createNotificationChannel()

        // 6️⃣ Tạo PlayerNotificationManager
        notificationManager = PlayerNotificationManager.Builder(
            this,
            NOTIFICATION_ID,
            CHANNEL_ID
        )
            .setMediaDescriptionAdapter(object : PlayerNotificationManager.MediaDescriptionAdapter {
                override fun createCurrentContentIntent(player: Player): PendingIntent? {
                    val intent = Intent(this@PlaybackService, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                    return PendingIntent.getActivity(
                        this@PlaybackService,
                        0,
                        intent,
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    )
                }

                override fun getCurrentContentTitle(player: Player): CharSequence {
                    return player.mediaMetadata.title ?: getString(R.string.app_name)
                }

                override fun getCurrentContentText(player: Player): CharSequence? {
                    return player.mediaMetadata.artist
                }

                override fun getCurrentLargeIcon(
                    player: Player,
                    callback: PlayerNotificationManager.BitmapCallback
                ) = null
            })
            .setNotificationListener(object : PlayerNotificationManager.NotificationListener {
                override fun onNotificationPosted(
                    notificationId: Int,
                    notification: Notification,
                    ongoing: Boolean
                ) {
                    if (ongoing) startForeground(notificationId, notification)
                    else stopForeground(STOP_FOREGROUND_DETACH)
                }

                override fun onNotificationCancelled(
                    notificationId: Int,
                    dismissedByUser: Boolean
                ) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            })
            .build().apply {
                setSmallIcon(R.drawable.ic_launcher_foreground)
                setUseNextAction(true)
                setUsePreviousAction(true)
                setPlayer(player)
            }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        notificationManager?.setPlayer(null)
        notificationManager = null
        mediaSession?.release()
        mediaSession = null
        player.release()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = ContextCompat.getSystemService(this, NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.playback_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.playback_channel_description)
            }
            manager?.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "melody_playback"
        private const val NOTIFICATION_ID = 1001
    }
}
