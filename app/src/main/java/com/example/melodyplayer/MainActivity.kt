package com.example.melodyplayer

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.melodyplayer.auth.AuthScreen
import com.example.melodyplayer.home.HomeScreen
import com.example.melodyplayer.navigation.Routes
import com.example.melodyplayer.player.MusicPlayerScreen
import com.example.melodyplayer.player.PlayerViewModel
import com.example.melodyplayer.playlist.PlaylistScreen
import com.example.melodyplayer.search.SearchScreen
import com.example.melodyplayer.settings.SettingsScreen
import com.example.melodyplayer.ui.screens.CollectionDetailScreen
import com.example.melodyplayer.ui.screens.CollectionsScreen
import com.example.melodyplayer.ui.theme.MelodyPlayerTheme
import com.google.firebase.auth.FirebaseAuth
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import com.example.melodyplayer.model.Song

class MainActivity : ComponentActivity() {
    private val playerVM: PlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MelodyPlayerTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        MainApp(playerVM)
                        FloatingChatBubble()
                    }
                }
            }
        }
    }
}

@Composable
fun MainApp(playerVM: PlayerViewModel) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = if (FirebaseAuth.getInstance().currentUser == null)
            Routes.AUTH else Routes.HOME
    ) {

        // ---- AUTH ----
        composable(Routes.AUTH) {
            AuthScreen(onLoginSuccess = {
                navController.navigate(Routes.HOME) {
                    popUpTo(Routes.AUTH) { inclusive = true }
                }
            })
        }

        // ---- HOME ----
        composable(Routes.HOME) {
            HomeScreen(navController = navController, playerVM = playerVM)
        }

        // ---- PLAYER ----
        composable(Routes.PLAYER) {
            MusicPlayerScreen(navController = navController, playerVM = playerVM)
        }

        // ---- SEARCH ----
        composable(Routes.SEARCH) {
            SearchScreen(navController = navController, playerVM = playerVM)
        }

        // ---- SETTINGS ----
        composable(Routes.SETTINGS) {
            SettingsScreen(navController = navController)
        }

        // ---- PLAYLIST ----
        composable(Routes.PLAYLIST_ALL) {
            PlaylistScreen(navController = navController, playerVM = playerVM)
        }

        // ✅ ---- COLLECTIONS (Danh sách bộ sưu tập) ----
        composable(Routes.COLLECTIONS) {
            CollectionsScreen(navController = navController, playerVM = playerVM)
        }

        // ✅ ---- COLLECTION DETAIL (Chi tiết bộ sưu tập) ----
        composable(
            route = "${Routes.COLLECTION}/{title}/{songsJson}",
            arguments = listOf(
                navArgument("title") { type = NavType.StringType },
                navArgument("songsJson") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val title = Uri.decode(backStackEntry.arguments?.getString("title") ?: "Bộ sưu tập")
            val songsJson = Uri.decode(backStackEntry.arguments?.getString("songsJson") ?: "[]")

            val songs = try {
                Json.decodeFromString<List<Song>>(songsJson)
            } catch (e: Exception) {
                emptyList()
            }

            CollectionDetailScreen(
                navController = navController,
                title = title,
                songs = songs,
                playerVM = playerVM
            )
        }
    }
}

@Composable
fun FloatingChatBubble() {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 🔘 Nút bong bóng chat
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(60.dp)
                .clip(CircleShape)
                .background(Color(0xFF1DB954))
                .clickable { expanded = !expanded },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (expanded) Icons.Default.Close else Icons.Default.Chat,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }

        // 💬 Hộp chat mini
        if (expanded) {
            Card(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 80.dp)
                    .width(240.dp)
                    .shadow(12.dp, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Bạn muốn nghe gì?",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Divider(color = Color.White.copy(alpha = 0.2f))
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Gợi ý: mở danh sách yêu thích hoặc nhập tên bài hát.",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
