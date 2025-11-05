package com.example.melodyplayer.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.melodyplayer.model.Song
import com.example.melodyplayer.navigation.Routes
import com.example.melodyplayer.player.PlayerViewModel
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionsScreen(
    navController: NavController,
    playerVM: PlayerViewModel
) {
    val collections by playerVM.collections.collectAsState()
    val favoriteSongs by playerVM.favoriteSongs.collectAsState()
    var showDeleteDialog by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // ✅ Đảm bảo có bộ sưu tập "Yêu thích" và đồng bộ bài hát yêu thích
    LaunchedEffect(Unit) {
        playerVM.ensureCollectionExists("Yêu thích")
        favoriteSongs.forEach { fav ->
            val parts = fav.split("||")
            if (parts.size >= 2) {
                val song = Song(
                    title = parts[0],
                    artist = parts[1],
                    imageUrl = null,
                    audioUrl = null,
                    resId = null
                )
                playerVM.addSongToCollection(song, "Yêu thích")
            }
        }
    }

    Scaffold(
        containerColor = Color(0xFF121212)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 🎵 Header
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                ) {
                    // Gradient nền xanh lá - đen
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF1DB954),
                                        Color(0xFF121212)
                                    ),
                                    startY = 0f,
                                    endY = 800f
                                )
                            )
                    )

                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Nút quay lại
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(
                                    Icons.Default.ArrowBack,
                                    contentDescription = "Quay lại",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        // Tiêu đề
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Outlined.LibraryMusic,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(80.dp)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "Bộ sưu tập của tôi",
                                color = Color.White,
                                fontSize = 30.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "${collections.size} bộ sưu tập",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            // 🎧 Nếu chưa có bộ sưu tập
            if (collections.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp)
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                Icons.Outlined.LibraryMusic,
                                contentDescription = null,
                                tint = Color.Gray.copy(0.4f),
                                modifier = Modifier.size(80.dp)
                            )
                            Text(
                                "Chưa có bộ sưu tập nào",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Thêm bài hát yêu thích vào bộ sưu tập\nđể dễ dàng tìm kiếm và phát nhạc",
                                color = Color.Gray,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                // 📀 Danh sách collection
                items(collections) { collectionName ->
                    val songCount = playerVM.getSongsInCollection(collectionName).size
                    CollectionCard(
                        collectionName = collectionName,
                        songCount = songCount,
                        onClick = {
                            val songs: List<Song>

                            // ✅ Nếu là bộ "Yêu thích" thì lấy từ favoriteSongs
                            if (collectionName == "Yêu thích") {
                                songs = playerVM.favoriteSongs.value.mapNotNull { fav ->
                                    val parts = fav.split("||")
                                    if (parts.size >= 2) {
                                        Song(
                                            title = parts[0],
                                            artist = parts[1],
                                            imageUrl = null,
                                            audioUrl = null,
                                            resId = null
                                        )
                                    } else null
                                }
                            } else {
                                // ✅ Ngược lại thì lấy từ danh sách bộ sưu tập
                                songs = playerVM.getSongsInCollection(collectionName)
                            }

                            if (songs.isEmpty()) {
                                println("⚠️ Bộ '$collectionName' trống.")
                                return@CollectionCard
                            }

                            val songsJson = Json.encodeToString(songs)
                            val encodedTitle = Uri.encode(collectionName)
                            val encodedJson = Uri.encode(songsJson)

                            // ✅ Điều hướng đúng route chi tiết
                            navController.navigate("collection/$encodedTitle/$encodedJson") {
                                launchSingleTop = true
                            }
                        },
                        onDelete = {
                            showDeleteDialog = collectionName
                        }
                    )


                }

                item {
                    Spacer(Modifier.height(100.dp))
                }
            }
        }
    }

    // ❌ Dialog xác nhận xóa
    showDeleteDialog?.let { collectionName ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            containerColor = Color(0xFF282828),
            shape = RoundedCornerShape(16.dp),
            title = {
                Text(
                    "Xóa bộ sưu tập?",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            },
            text = {
                Text(
                    "Bạn có chắc muốn xóa \"$collectionName\"?\nHành động này không thể hoàn tác.",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch { playerVM.deleteCollection(collectionName) }
                        showDeleteDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF5252)
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text("Xóa", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = null },
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text("Hủy", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun CollectionCard(
    collectionName: String,
    songCount: Int,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF1ED760),
                                Color(0xFF1DB954),
                                Color(0xFF169C46)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.LibraryMusic,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    collectionName,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "$songCount bài hát",
                    color = Color.Gray,
                    fontSize = 13.sp
                )
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Menu",
                        tint = Color.Gray
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(Color(0xFF282828))
                ) {
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                                Spacer(Modifier.width(16.dp))
                                Text("Phát tất cả", color = Color.White)
                            }
                        },
                        onClick = {
                            showMenu = false
                            onClick()
                        }
                    )

                    Divider(color = Color.Gray.copy(0.2f))

                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.Delete,
                                    contentDescription = null,
                                    tint = Color(0xFFFF5252)
                                )
                                Spacer(Modifier.width(16.dp))
                                Text("Xóa bộ sưu tập", color = Color(0xFFFF5252))
                            }
                        },
                        onClick = {
                            showMenu = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}
