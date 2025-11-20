package com.example.melodyplayer.data

import com.example.melodyplayer.model.Song

class SongRepository(
    private val firestoreRepo: FirestoreRepository = FirestoreRepository()
) {

    /**
     * Lấy toàn bộ bài hát từ Firestore
     */
    suspend fun getAllSongs(): List<Song> {
        val list = firestoreRepo.getAllSongs()
        android.util.Log.d("DEBUG_SONG", "Đã load Firestore: ${list.size} bài")
        return list
    }


    /**
     * Tìm bài hát theo keyword (áp dụng trên danh sách từ Firestore)
     */
    fun searchSongs(allSongs: List<Song>, keyword: String): List<Song> {
        val query = keyword.trim()
        if (query.isEmpty()) return emptyList()

        return allSongs.filter { song ->
            (song.title?.contains(query, ignoreCase = true) == true) ||
                    (song.artist?.contains(query, ignoreCase = true) == true)
        }
    }

}
