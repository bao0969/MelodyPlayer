package com.example.melodyplayer.data

import com.example.melodyplayer.model.Song
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirestoreRepository {

    private val db = FirebaseFirestore.getInstance()

    suspend fun getAllSongs(): List<Song> {
        return try {
            val snap = db.collection("songs").get().await()
            snap.documents.mapNotNull { it.toObject(Song::class.java) }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
