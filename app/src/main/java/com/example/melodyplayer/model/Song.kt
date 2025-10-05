package com.example.melodyplayer.model

data class Song(
    val title: String = "",
    val artist: String = "",
    val coverUrl: String? = null,  // ✅ thêm dòng này để không lỗi
    val resId: Int? = null,        // ✅ dùng khi phát nhạc từ raw
    val audioUrl: String? = null,  // dùng khi phát online (nếu sau này cần)
    val duration: Int = 0
)
