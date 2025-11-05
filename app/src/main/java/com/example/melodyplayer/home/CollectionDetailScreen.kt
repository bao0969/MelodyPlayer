package com.example.melodyplayer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.melodyplayer.model.Song
import com.example.melodyplayer.navigation.Routes
import com.example.melodyplayer.player.PlayerViewModel

@Composable
fun CollectionDetailScreen(
    navController: NavController,
    title: String,
    songs: List<Song>,
    playerVM: PlayerViewModel
) {
    Scaffold(
        containerColor = Color(0xFF121212),
        topBar = {
            // ✅ Thanh tiêu đề với gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF1DB954),
                                Color(0xFF169C46)
                            )
                        )
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }
            }
        }
    ) { padding ->

        // ✅ Nếu không có bài hát
        if (songs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Không có bài hát nào trong bộ sưu tập này",
                    color = Color.White.copy(0.8f),
                    fontSize = 16.sp
                )
            }
        } else {
            // ✅ Danh sách bài hát
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Color(0xFF121212))
            ) {
                items(songs) { song ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .clickable {
                                // ✅ Phát bài hát khi người dùng chọn
                                playerVM.setPlaylist(songs, songs.indexOf(song))
                                navController.navigate(Routes.PLAYER)
                            },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1C)),
                        shape = MaterialTheme.shapes.medium,
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Text(
                                text = song.title,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = song.artist,
                                color = Color(0xFFAAAAAA),
                                fontSize = 13.sp
                            )
                        }
                    }
                }
                item { Spacer(Modifier.height(60.dp)) }
            }
        }
    }
}
