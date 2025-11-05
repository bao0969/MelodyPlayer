package com.example.melodyplayer.player

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.melodyplayer.model.Song
import androidx.media3.common.Player
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicPlayerScreen(
    navController: NavController,
    playerVM: PlayerViewModel
) {
    val currentSong by playerVM.currentSong.collectAsState()
    val isPlaying by playerVM.isPlaying.collectAsState()
    val playlist by playerVM.playlist.collectAsState()
    val playbackPosition by playerVM.playbackPosition.collectAsState()
    val playbackDuration by playerVM.duration.collectAsState()
    val shuffleEnabled by playerVM.shuffleEnabled.collectAsState()
    val repeatMode by playerVM.repeatMode.collectAsState()
    val playbackError by playerVM.playbackError.collectAsState()
    val favoriteSongs by playerVM.favoriteSongs.collectAsState()

    var showPlaylist by remember { mutableStateOf(false) }
    var showCollectionDialog by remember { mutableStateOf(false) }
    var sliderValue by remember { mutableStateOf(0f) }
    var isSeeking by remember { mutableStateOf(false) }

    val duration = playbackDuration.coerceAtLeast(0L)
    val currentProgress = if (duration > 0) {
        (playbackPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val displayedProgress = if (isSeeking) sliderValue else currentProgress
    val elapsedPosition = if (isSeeking) (sliderValue * duration).toLong() else playbackPosition

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(playbackError) {
        playbackError?.let {
            snackbarHostState.showSnackbar(it)
            playerVM.clearError()
        }
    }

    val rotation by rememberInfiniteTransition(label = "").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = ""
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D))
    ) {
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.5f)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1DB954).copy(alpha = 0.3f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                }
                Text(
                    "Đang phát",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                IconButton(onClick = { showPlaylist = !showPlaylist }) {
                    Icon(Icons.Default.QueueMusic, null, tint = Color.White)
                }
            }

            // Phần giữa màn hình - Đĩa nhạc và tiêu đề
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Đĩa nhạc tròn
                    Box(
                        modifier = Modifier
                            .size(280.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2A2A2A))
                            .rotate(if (isPlaying) rotation else 0f)
                            .shadow(16.dp, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        // Ảnh bìa album
                        AsyncImage(
                            model = currentSong?.imageUrl ?: "",
                            contentDescription = "Album Art",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(240.dp)
                                .clip(CircleShape)
                        )

                        // Tâm đĩa
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1A1A1A))
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF404040))
                                    .align(Alignment.Center)
                            )
                        }
                    }

                    Spacer(Modifier.height(32.dp))

                    // Tiêu đề bài hát
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    ) {
                        Text(
                            text = currentSong?.title ?: "No song playing",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = currentSong?.artist ?: "Unknown Artist",
                            fontSize = 16.sp,
                            color = Color.White.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Progress bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
            ) {
                Slider(
                    value = displayedProgress,
                    onValueChange = {
                        isSeeking = true
                        sliderValue = it
                    },
                    onValueChangeFinished = {
                        if (duration > 0) playerVM.seekTo((sliderValue * duration).toLong())
                        isSeeking = false
                    },
                    valueRange = 0f..1f,
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF1DB954),
                        activeTrackColor = Color(0xFF1DB954),
                        inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(formatTime(elapsedPosition), color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                    Text(formatTime(duration), color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                }
            }

            Spacer(Modifier.height(24.dp))

            // Control buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { playerVM.toggleShuffle() }, modifier = Modifier.size(48.dp)) {
                    Icon(
                        Icons.Default.Shuffle,
                        null,
                        tint = if (shuffleEnabled) Color(0xFF1DB954) else Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(28.dp)
                    )
                }

                IconButton(onClick = { playerVM.prevSong() }, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Default.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(40.dp))
                }

                FloatingActionButton(
                    onClick = { playerVM.togglePlayPause() },
                    modifier = Modifier.size(80.dp),
                    containerColor = Color(0xFF1DB954),
                    elevation = FloatingActionButtonDefaults.elevation(8.dp, 12.dp)
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }

                IconButton(onClick = { playerVM.nextSong() }, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Default.SkipNext, null, tint = Color.White, modifier = Modifier.size(40.dp))
                }

                IconButton(onClick = { playerVM.cycleRepeatMode() }, modifier = Modifier.size(48.dp)) {
                    val repeatIcon =
                        if (repeatMode == Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat
                    val repeatTint =
                        if (repeatMode != Player.REPEAT_MODE_OFF) Color(0xFF1DB954) else Color.White.copy(alpha = 0.6f)
                    Icon(repeatIcon, null, tint = repeatTint, modifier = Modifier.size(28.dp))
                }
            }

            Spacer(Modifier.height(16.dp))

            // Bottom section - Nút yêu thích
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val isFavorite = currentSong?.let { "${it.title}||${it.artist}" in favoriteSongs } == true

                IconButton(
                    onClick = {
                        currentSong?.let { song ->
                            playerVM.toggleFavorite(song)
                            // Nếu đang thêm vào yêu thích (chưa có trong danh sách)
                            if (!isFavorite) {
                                showCollectionDialog = true
                            }
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.White.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) Color(0xFF1DB954) else Color.White
                    )
                }
            }
        }

        // Playlist overlay
        if (showPlaylist) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xAA000000))
                    .clickable { showPlaylist = false }
            ) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .fillMaxHeight(0.7f)
                        .background(
                            Color(0xFF1E1E1E),
                            RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                        )
                        .clickable(enabled = false) {} // Ngăn click through
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Đang phát (${playlist.size})",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        IconButton(onClick = { showPlaylist = false }) {
                            Icon(Icons.Default.Close, null, tint = Color.White)
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(playlist) { index, song ->
                            PlaylistItemCard(
                                song = song,
                                isPlaying = song == currentSong,
                                onClick = {
                                    playerVM.playSong(index)
                                    showPlaylist = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // Collection dialog
        if (showCollectionDialog) {
            AddToCollectionDialog(
                currentSong = currentSong,
                snackbarHostState = snackbarHostState,
                onDismiss = { showCollectionDialog = false },
                onAddToCollection = { song, collectionName ->
                    // TODO: Thêm function vào ViewModel để lưu vào collection
                    // playerVM.addSongToCollection(song, collectionName)
                    showCollectionDialog = false
                }
            )
        }
    }
}

@Composable
fun PlaylistItemCard(song: Song, isPlaying: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying) Color(0xFF1DB954).copy(alpha = 0.2f) else Color(0xFF2C2C2C)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = song.imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    song.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    song.artist,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (isPlaying) {
                Icon(Icons.Default.Equalizer, null, tint = Color(0xFF1DB954))
            }
        }
    }
}

@Composable
fun AddToCollectionDialog(
    currentSong: Song?,
    snackbarHostState: SnackbarHostState,
    onDismiss: () -> Unit,
    onAddToCollection: (Song, String) -> Unit
) {
    val scope = rememberCoroutineScope()
    // Danh sách collection mẫu - trong thực tế lấy từ ViewModel
    val collections = remember {
        listOf(
            "Yêu thích",
            "Playlist của tôi",
            "Nhạc buồn",
            "Nhạc vui",
            "Tập trung",
            "Thư giãn"
        )
    }

    var selectedCollection by remember { mutableStateOf<String?>(null) }
    var showNewCollectionField by remember { mutableStateOf(false) }
    var newCollectionName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF282828),
        shape = RoundedCornerShape(16.dp),
        title = {
            Column {
                Text(
                    "Thêm vào bộ sưu tập",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Chọn hoặc tạo bộ sưu tập mới",
                    color = Color.Gray,
                    fontSize = 13.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Hiển thị thông tin bài hát
                currentSong?.let { song ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF1E1E1E)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF404040)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!song.imageUrl.isNullOrEmpty()) {
                                    AsyncImage(
                                        model = song.imageUrl,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        Icons.Outlined.MusicNote,
                                        contentDescription = null,
                                        tint = Color.Gray
                                    )
                                }
                            }

                            Spacer(Modifier.width(12.dp))

                            Column {
                                Text(
                                    song.title,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    song.artist,
                                    color = Color.Gray,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Divider(color = Color.Gray.copy(0.2f))

                // Danh sách collection
                LazyColumn(
                    modifier = Modifier.height(250.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(collections) { collection ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedCollection = collection
                                    showNewCollectionField = false
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedCollection == collection)
                                    Color(0xFF1DB954).copy(0.3f)
                                else Color(0xFF1E1E1E)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Outlined.LibraryMusic,
                                    contentDescription = null,
                                    tint = if (selectedCollection == collection)
                                        Color(0xFF1DB954)
                                    else Color.Gray,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    collection,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = if (selectedCollection == collection)
                                        FontWeight.Bold
                                    else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                Divider(color = Color.Gray.copy(0.2f))

                // Tạo collection mới
                if (showNewCollectionField) {
                    OutlinedTextField(
                        value = newCollectionName,
                        onValueChange = {
                            newCollectionName = it
                            selectedCollection = null
                        },
                        label = { Text("Tên bộ sưu tập mới", color = Color.Gray) },
                        placeholder = { Text("Ví dụ: Nhạc thể thao", color = Color.Gray.copy(0.5f)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF1E1E1E),
                            unfocusedContainerColor = Color(0xFF1E1E1E),
                            focusedBorderColor = Color(0xFF1DB954),
                            unfocusedBorderColor = Color.Gray
                        ),
                        shape = RoundedCornerShape(8.dp),
                        leadingIcon = {
                            Icon(
                                Icons.Default.LibraryAdd,
                                contentDescription = null,
                                tint = Color(0xFF1DB954)
                            )
                        }
                    )
                } else {
                    Button(
                        onClick = {
                            showNewCollectionField = true
                            selectedCollection = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1E1E1E)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            tint = Color(0xFF1DB954),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Tạo bộ sưu tập mới",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    currentSong?.let { song ->
                        val collectionToAdd = if (showNewCollectionField && newCollectionName.isNotEmpty()) {
                            newCollectionName
                        } else {
                            selectedCollection
                        }

                        collectionToAdd?.let { collection ->
                            onAddToCollection(song, collection)

                            // Hiển thị thông báo
                            scope.launch {
                                snackbarHostState.showSnackbar("Đã thêm vào $collection")
                            }
                        }
                    }
                },
                enabled = selectedCollection != null || (showNewCollectionField && newCollectionName.isNotEmpty()),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1DB954),
                    disabledContainerColor = Color.Gray.copy(0.3f)
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "Thêm vào",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(24.dp)
            ) {
                Text(
                    "Hủy",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}

fun formatTime(ms: Long): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%02d:%02d".format(min, sec)
}