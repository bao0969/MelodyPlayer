import com.example.melodyplayer.model.Song
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

/**
 * Helper seed data to quickly populate Firestore with valid streaming URLs.
 */
object SongSeedData {
    val sampleSongs: List<Song> = listOf(
        Song(
            title = "Sunrise Prelude",
            artist = "SampleLib Ensemble",
            audioUrl = "https://samplelib.com/lib/preview/mp3/sample-6s.mp3",
            duration = 6
        ),
        Song(
            title = "Moonlight Echoes",
            artist = "SampleLib Ensemble",
            audioUrl = "https://samplelib.com/lib/preview/mp3/sample-9s.mp3",
            duration = 9
        ),
        Song(
            title = "Stardust Journey",
            artist = "SampleLib Ensemble",
            audioUrl = "https://samplelib.com/lib/preview/mp3/sample-12s.mp3",
            duration = 12
        ),
        Song(
            title = "Aurora Dreams",
            artist = "SampleLib Ensemble",
            audioUrl = "https://samplelib.com/lib/preview/mp3/sample-15s.mp3",
            duration = 15
        )
    )

    suspend fun seed(database: FirebaseFirestore) {
        val collection = database.collection("songs")

        sampleSongs.forEach { song ->
            val documentId = buildDocumentId(song)
            collection.document(documentId)
                .set(song, SetOptions.merge())
                .await()
        }
    }

    private fun buildDocumentId(song: Song): String {
        val rawId = "${song.title}-${song.artist}".lowercase()
        val normalized = rawId.replace(Regex("[^a-z0-9]+"), "-").trim('-')
        return normalized.ifBlank { song.title.hashCode().toString() }
    }
}