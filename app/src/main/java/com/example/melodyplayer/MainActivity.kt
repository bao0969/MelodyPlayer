package com.example.melodyplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.melodyplayer.auth.AuthScreen
import com.example.melodyplayer.home.HomeScreen
import com.example.melodyplayer.navigation.Routes
import com.example.melodyplayer.player.MusicPlayerScreen
import com.example.melodyplayer.player.PlayerViewModel
import com.example.melodyplayer.search.SearchScreen
import com.example.melodyplayer.settings.SettingsScreen
import com.example.melodyplayer.playlist.PlaylistScreen
import com.example.melodyplayer.ui.theme.MelodyPlayerTheme
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    private val playerVM: PlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MelodyPlayerTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    MainApp(playerVM)
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
        startDestination = if (FirebaseAuth.getInstance().currentUser == null) {
            Routes.AUTH
        } else {
            Routes.HOME
        }
    ) {
        composable(Routes.AUTH) {
            AuthScreen(onLoginSuccess = {
                navController.navigate(Routes.HOME) {
                    popUpTo(Routes.AUTH) { inclusive = true }
                }
            })
        }
        composable(Routes.HOME) {
            HomeScreen(navController = navController, playerVM = playerVM)
        }
        composable(Routes.PLAYER) {
            MusicPlayerScreen(navController = navController, playerVM = playerVM)
        }
        composable(Routes.SEARCH) {
            SearchScreen(navController = navController, playerVM = playerVM)
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(navController = navController)
        }
        composable(Routes.PLAYLIST_ALL) {
            PlaylistScreen(navController = navController, playerVM = playerVM)
        }
    }
}