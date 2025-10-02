//package com.example.melodyplayer
//
//import androidx.compose.animation.core.*
//import androidx.compose.foundation.background
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.itemsIndexed
//import androidx.compose.foundation.shape.CircleShape
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.*
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.draw.rotate
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import androidx.navigation.NavController
//import com.example.melodyplayer.model.Song
//import kotlinx.coroutines.delay
//import kotlinx.coroutines.isActive
//
//// Media3
//import androidx.media3.common.MediaItem
//import androidx.media3.common.Player
//import androidx.media3.exoplayer.ExoPlayer
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun MusicPlayerScreen(
//    navController: NavController,
//    song: Song?,
//) {
//    val context = LocalContext.current
//
//    // Dùng mutableStateListOf để thêm/xoá động
//    val playlist = remember {
//        mutableStateListOf(
//            Song(
//                title = "Shape of You",
//                artist = "Ed Sheeran",
//                coverResId = android.R.drawable.ic_media_play,
//                audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3"
//            ),
//            Song(
//                title = "Señorita",
//                artist = "Shawn Mendes",
//                coverResId = android.R.drawable.ic_media_play,
//                audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3"
//            ),
//            Song(
//                title = "Believer",
//                artist = "Imagine Dragons",
//                coverResId = android.R.drawable.ic_media_play,
//                audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3"
//            ),
//            Song(
//                title = "Nơi này có anh",
//                artist = "Sơn Tùng M-TP",
//                coverResId = android.R.drawable.ic_media_play,
//                audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3"
//            )
//        )
//    }
//
//    // Tìm index khởi tạo theo bài từ Home (nếu có)
//    var currentIndex by remember {
//        mutableStateOf(
//            if (song == null) 0 else playlist.indexOfFirst {
//                it.title == song.title && it.artist == song.artist
//            }.takeIf { it >= 0 } ?: 0
//        )
//    }
//    var currentSong by remember { mutableStateOf(playlist[currentIndex]) }
//
//    // ===== ExoPlayer =====
//    val player = remember {
//        ExoPlayer.Builder(context).build()
//    }
//
//    fun loadAndPlay(index: Int, autoPlay: Boolean = true) {
//        if (playlist.isEmpty()) {
//            player.stop()
//            return
//        }
//        val item = playlist[index % playlist.size]
//        player.setMediaItem(MediaItem.fromUri(item.audioUrl))
//        player.prepare()
//        player.playWhenReady = autoPlay
//    }
//
//    // Lần đầu
//    LaunchedEffect(Unit) { loadAndPlay(currentIndex, autoPlay = true) }
//
//    // Dọn tài nguyên
//    DisposableEffect(Unit) { onDispose { player.release() } }
//
//    // Trạng thái UI
//    var isPlaying by remember { mutableStateOf(true) }
//    var isShuffle by remember { mutableStateOf(false) }
//    var isRepeat by remember { mutableStateOf(false) }
//    var progress by remember { mutableStateOf(0f) } // 0..1
//
//    // Lắng nghe player
//    DisposableEffect(player, isRepeat, isShuffle, playlist.size) {
//        val listener = object : Player.Listener {
//            override fun onIsPlayingChanged(isPlayingNow: Boolean) {
//                isPlaying = isPlayingNow
//            }
//
//            override fun onPlaybackStateChanged(playbackState: Int) {
//                if (playbackState == Player.STATE_ENDED && playlist.isNotEmpty()) {
//                    if (isRepeat) {
//                        player.seekTo(0)
//                        player.playWhenReady = true
//                    } else {
//                        currentIndex = if (isShuffle) {
//                            playlist.indices.random()
//                        } else {
//                            (currentIndex + 1) % playlist.size
//                        }
//                        currentSong = playlist[currentIndex]
//                        loadAndPlay(currentIndex, autoPlay = true)
//                    }
//                }
//            }
//        }
//        player.addListener(listener)
//        onDispose { player.removeListener(listener) }
//    }
//
//    // Cập nhật progress ~20fps
//    LaunchedEffect(player) {
//        while (isActive) {
//            val d = player.duration.takeIf { it > 0 } ?: -1L
//            val p = player.currentPosition
//            progress = if (d > 0) (p.toFloat() / d.toFloat()).coerceIn(0f, 1f) else 0f
//            delay(50)
//        }
//    }
//
//    // Xoay album art
//    val rotation by rememberInfiniteTransition(label = "")
//        .animateFloat(
//            initialValue = 0f,
//            targetValue = 360f,
//            animationSpec = infiniteRepeatable(
//                animation = tween(8000, easing = LinearEasing),
//                repeatMode = RepeatMode.Restart
//            ),
//            label = ""
//        )
//
//    fun fmt(ms: Long): String {
//        if (ms <= 0) return "0:00"
//        val totalSec = (ms / 1000).toInt()
//        val m = totalSec / 60
//        val s = totalSec % 60
//        return "%d:%02d".format(m, s)
//    }
//
//    // ===== State dialog thêm bài =====
//    var showDialog by remember { mutableStateOf(false) }
//    var newTitle by remember { mutableStateOf("") }
//    var newArtist by remember { mutableStateOf("") }
//    var newUrl by remember { mutableStateOf("") }
//
//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = { Text("Now Playing", color = Color.White) },
//                navigationIcon = {
//                    IconButton(onClick = { navController.popBackStack() }) {
//                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
//                    }
//                },
//                colors = TopAppBarDefaults.topAppBarColors(
//                    containerColor = Color(0xFF121212),
//                    titleContentColor = Color.White,
//                    navigationIconContentColor = Color.White
//                )
//            )
//        },
//        floatingActionButton = {
//            FloatingActionButton(
//                onClick = { showDialog = true },
//                containerColor = Color(0xFF1DB954)
//            ) { Icon(Icons.Default.Add, contentDescription = "Thêm bài", tint = Color.White) }
//        },
//        containerColor = Color.Black
//    ) { padding ->
//        Column(
//            modifier = Modifier
//                .padding(padding)
//                .fillMaxSize()
//                .background(Color(0xFF121212))
//                .padding(16.dp)
//        ) {
//            // Album art
//            Box(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .aspectRatio(1f)
//                    .clip(CircleShape)
//                    .background(Color(0xFF2A2A2A))
//                    .rotate(if (isPlaying) rotation else 0f),
//                contentAlignment = Alignment.Center
//            ) {
//                Icon(
//                    Icons.Filled.MusicNote,
//                    contentDescription = null,
//                    tint = Color.White,
//                    modifier = Modifier.size(120.dp)
//                )
//            }
//
//            Spacer(Modifier.height(20.dp))
//
//            // Tiêu đề
//            Text(
//                text = currentSong.title,
//                fontSize = 22.sp,
//                fontWeight = FontWeight.Bold,
//                color = Color.White,
//                modifier = Modifier.align(Alignment.CenterHorizontally)
//            )
//            Text(
//                text = currentSong.artist,
//                fontSize = 14.sp,
//                color = Color.Gray,
//                modifier = Modifier.align(Alignment.CenterHorizontally)
//            )
//
//            Spacer(Modifier.height(12.dp))
//
//            // Seekbar
//            val duration = player.duration.takeIf { it > 0 } ?: 0L
//            val position = player.currentPosition
//            Slider(
//                value = progress,
//                onValueChange = { progress = it },
//                onValueChangeFinished = {
//                    if (duration > 0) player.seekTo((progress * duration).toLong())
//                },
//                modifier = Modifier.fillMaxWidth(),
//                colors = SliderDefaults.colors(
//                    thumbColor = Color(0xFF1DB954),
//                    activeTrackColor = Color(0xFF1DB954)
//                )
//            )
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.SpaceBetween
//            ) {
//                Text(fmt(position), color = Color.Gray)
//                Text(fmt(duration), color = Color.Gray)
//            }
//
//            Spacer(Modifier.height(8.dp))
//
//            // Controls
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.SpaceEvenly,
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                IconButton(onClick = { isShuffle = !isShuffle }) {
//                    Icon(
//                        Icons.Default.Shuffle,
//                        contentDescription = "Shuffle",
//                        tint = if (isShuffle) Color(0xFF1DB954) else Color.White
//                    )
//                }
//                IconButton(onClick = {
//                    if (playlist.isNotEmpty()) {
//                        currentIndex =
//                            if (currentIndex > 0) currentIndex - 1 else playlist.lastIndex
//                        currentSong = playlist[currentIndex]
//                        loadAndPlay(currentIndex, autoPlay = true)
//                    }
//                }) {
//                    Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous", tint = Color.White, modifier = Modifier.size(40.dp))
//                }
//                IconButton(
//                    onClick = { if (player.isPlaying) player.pause() else player.play() },
//                    modifier = Modifier
//                        .size(80.dp)
//                        .background(Color(0xFF1DB954), CircleShape)
//                ) {
//                    Icon(
//                        if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
//                        contentDescription = "Play/Pause",
//                        tint = Color.White,
//                        modifier = Modifier.size(50.dp)
//                    )
//                }
//                IconButton(onClick = {
//                    if (playlist.isNotEmpty()) {
//                        currentIndex =
//                            if (currentIndex < playlist.lastIndex) currentIndex + 1 else 0
//                        currentSong = playlist[currentIndex]
//                        loadAndPlay(currentIndex, autoPlay = true)
//                    }
//                }) {
//                    Icon(Icons.Filled.SkipNext, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(40.dp))
//                }
//                IconButton(onClick = { isRepeat = !isRepeat }) {
//                    Icon(
//                        Icons.Default.Repeat,
//                        contentDescription = "Repeat",
//                        tint = if (isRepeat) Color(0xFF1DB954) else Color.White
//                    )
//                }
//            }
//
//            Spacer(Modifier.height(16.dp))
//
//            // Danh sách phát
//            Text("Danh sách phát", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
//            Spacer(Modifier.height(8.dp))
//            LazyColumn {
//                itemsIndexed(playlist) { index, item ->
//                    Row(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .clickable {
//                                currentIndex = index
//                                currentSong = item
//                                loadAndPlay(index, autoPlay = true)
//                            }
//                            .padding(vertical = 12.dp),
//                        verticalAlignment = Alignment.CenterVertically
//                    ) {
//                        Icon(
//                            imageVector = Icons.Filled.MusicNote,
//                            contentDescription = null,
//                            tint = if (currentSong.title == item.title) Color(0xFF1DB954) else Color.White
//                        )
//                        Spacer(Modifier.width(8.dp))
//                        Column(Modifier.weight(1f)) {
//                            Text(item.title, color = if (currentSong.title == item.title) Color(0xFF1DB954) else Color.White)
//                            Text(item.artist, color = Color.Gray, fontSize = 12.sp)
//                        }
//                        Text("~4:00", color = Color.Gray)
//
//                        // Nút xoá bài
//                        IconButton(onClick = {
//                            val removingCurrent = currentIndex == index
//                            playlist.removeAt(index)
//
//                            if (playlist.isEmpty()) {
//                                player.stop()
//                                currentIndex = 0
//                                currentSong = Song("Không có bài", "", coverResId = android.R.drawable.ic_media_play)
//                                return@IconButton
//                            }
//
//                            if (removingCurrent) {
//                                currentIndex = currentIndex % playlist.size
//                                currentSong = playlist[currentIndex]
//                                loadAndPlay(currentIndex, autoPlay = true)
//                            } else if (index < currentIndex) {
//                                currentIndex -= 1
//                            }
//                        }) {
//                            Icon(Icons.Default.Delete, contentDescription = "Xoá", tint = Color.Red)
//                        }
//                    }
//                    Divider(color = Color(0xFF2A2A2A))
//                }
//            }
//        }
//    }
//
//    // ===== Dialog thêm bài mới =====
//    if (showDialog) {
//        AlertDialog(
//            onDismissRequest = { showDialog = false },
//            title = { Text("Thêm bài hát mới") },
//            text = {
//                Column {
//                    OutlinedTextField(
//                        value = newTitle,
//                        onValueChange = { newTitle = it },
//                        label = { Text("Tên bài hát") },
//                        singleLine = true
//                    )
//                    Spacer(Modifier.height(8.dp))
//                    OutlinedTextField(
//                        value = newArtist,
//                        onValueChange = { newArtist = it },
//                        label = { Text("Ca sĩ") },
//                        singleLine = true
//                    )
//                    Spacer(Modifier.height(8.dp))
//                    OutlinedTextField(
//                        value = newUrl,
//                        onValueChange = { newUrl = it },
//                        label = { Text("URL (MP3)") },
//                        singleLine = true
//                    )
//                }
//            },
//            confirmButton = {
//                TextButton(onClick = {
//                    if (newTitle.isNotBlank() && newUrl.startsWith("http")) {
//                        playlist.add(
//                            Song(
//                                title = newTitle.trim(),
//                                artist = newArtist.ifBlank { "Unknown" }.trim(),
//                                coverResId = android.R.drawable.ic_media_play,
//                                audioUrl = newUrl.trim()
//                            )
//                        )
//                        newTitle = ""; newArtist = ""; newUrl = ""
//                        showDialog = false
//                    }
//                }) { Text("Thêm") }
//            },
//            dismissButton = {
//                TextButton(onClick = { showDialog = false }) { Text("Hủy") }
//            }
//        )
//    }
//}
