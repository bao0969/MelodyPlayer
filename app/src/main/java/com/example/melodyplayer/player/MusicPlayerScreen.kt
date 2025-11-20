package com.example.melodyplayer.player

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
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
import androidx.compose.ui.platform.LocalContext
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.ImageLoader
import android.graphics.drawable.BitmapDrawable
import androidx.palette.graphics.Palette
import java.util.Calendar

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
    val repeatMode by playerVM.repeatMode.collectAsState()
    val playbackError by playerVM.playbackError.collectAsState()
    val favoriteSongs by playerVM.favoriteSongs.collectAsState()
    val sleepEndTime by playerVM.sleepEndTime.collectAsState()

    var showPlaylist by remember { mutableStateOf(false) }
    var showCollectionDialog by remember { mutableStateOf(false) }
    var showSleepDialog by remember { mutableStateOf(false) }
    var showSleepConfirmation by remember { mutableStateOf(false) }

    var sliderValue by remember { mutableStateOf(0f) }
    var isSeeking by remember { mutableStateOf(false) }

    var dominantColor by remember { mutableStateOf(Color(0xFF1DB954)) }
    var vibrantColor by remember { mutableStateOf(Color(0xFF1DB954)) }

    val context = LocalContext.current

    // Extract palette
    LaunchedEffect(currentSong?.imageUrl) {
        currentSong?.imageUrl?.let { url ->
            try {
                val loader = ImageLoader(context)
                val req = ImageRequest.Builder(context)
                    .data(url)
                    .allowHardware(false)
                    .build()

                val result = loader.execute(req) as SuccessResult
                val bmp = (result.drawable as? BitmapDrawable)?.bitmap

                bmp?.let {
                    Palette.from(it).generate { p ->
                        if (p != null) {
                            dominantColor = Color(p.getDominantColor(0xFF1DB954.toInt()))
                            vibrantColor = Color(p.getVibrantColor(0xFF1DB954.toInt()))
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    val duration = playbackDuration.coerceAtLeast(0L)
    val progress = if (duration > 0) playbackPosition.toFloat() / duration else 0f
    val displayedProgress = if (isSeeking) sliderValue else progress
    val elapsed = if (isSeeking) (sliderValue * duration).toLong() else playbackPosition

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(playbackError) {
        playbackError?.let {
            snackbarHostState.showSnackbar(it)
            playerVM.clearError()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        dominantColor.copy(0.7f),
                        dominantColor.copy(0.5f),
                        Color(0xFF0D0D0D),
                        Color(0xFF0D0D0D)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {

            // TOP BAR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.KeyboardArrowDown, null, tint = Color.White, modifier = Modifier.size(32.dp))
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text("ĐANG PHÁT TỪ DANH SÁCH PHÁT", color = Color.White.copy(0.7f), fontSize = 11.sp)
                    Text("Daily Mix 2", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Row {
                    IconButton(onClick = { showSleepDialog = true }) {
                        Icon(Icons.Default.Timer, null, tint = Color.White)
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.MoreVert, null, tint = Color.White)
                    }
                }
            }

            // CENTER SECTION
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {

                    Box(
                        modifier = Modifier
                            .size(340.dp)
                            .padding(24.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .shadow(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = currentSong?.imageUrl ?: "",
                            contentDescription = "",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(Modifier.height(30.dp))

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                    ) {

                        // Countdown timer display
                        sleepEndTime?.let { endTime ->
                            val currentTime = remember { mutableStateOf(System.currentTimeMillis()) }

                            LaunchedEffect(Unit) {
                                while (true) {
                                    currentTime.value = System.currentTimeMillis()
                                    kotlinx.coroutines.delay(1000)
                                }
                            }

                            val remainingMs = endTime - currentTime.value
                            if (remainingMs > 0) {
                                val remainingMinutes = (remainingMs / 1000 / 60).toInt()
                                val remainingSeconds = ((remainingMs / 1000) % 60).toInt()
                                Text(
                                    text = "Tắt sau: ${remainingMinutes}:${String.format("%02d", remainingSeconds)}",
                                    fontSize = 13.sp,
                                    color = vibrantColor,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(Modifier.height(8.dp))
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(34.dp)
                                .clipToBounds(),
                            contentAlignment = Alignment.Center
                        ) {
                            MarqueeText(
                                text = currentSong?.title ?: "No song",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Spacer(Modifier.height(4.dp))

                        Text(
                            text = currentSong?.artist ?: "Unknown Artist",
                            fontSize = 16.sp,
                            color = Color.White.copy(0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // PROGRESS BAR
            Column(
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                Slider(
                    value = displayedProgress,
                    onValueChange = {
                        sliderValue = it
                        isSeeking = true
                    },
                    onValueChangeFinished = {
                        isSeeking = false
                        playerVM.seekTo((sliderValue * duration).toLong())
                    },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color.White.copy(0.3f)
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(formatTime(elapsed), color = Color.White.copy(0.7f), fontSize = 12.sp)
                    Text(formatTime(duration), color = Color.White.copy(0.7f), fontSize = 12.sp)
                }
            }

            Spacer(Modifier.height(16.dp))

            // CONTROLS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isFavorite =
                    currentSong?.let { "${it.title}||${it.artist}" in favoriteSongs } == true

                IconButton(
                    onClick = {
                        currentSong?.let { playerVM.toggleFavorite(it) }
                        if (!isFavorite) showCollectionDialog = true
                    }
                ) {
                    Icon(
                        if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        null,
                        tint = if (isFavorite) vibrantColor else Color.White.copy(0.7f)
                    )
                }

                IconButton(onClick = { playerVM.prevSong() }) {
                    Icon(Icons.Default.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(44.dp))
                }

                FloatingActionButton(
                    onClick = { playerVM.togglePlayPause() },
                    containerColor = Color.White,
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        null,
                        tint = Color.Black,
                        modifier = Modifier.size(36.dp)
                    )
                }

                IconButton(onClick = { playerVM.nextSong() }) {
                    Icon(Icons.Default.SkipNext, null, tint = Color.White, modifier = Modifier.size(44.dp))
                }

                // REPEAT
                IconButton(onClick = { playerVM.cycleRepeatMode() }) {
                    val icon =
                        if (repeatMode == Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat
                    val tint =
                        if (repeatMode != Player.REPEAT_MODE_OFF) vibrantColor else Color.White.copy(0.7f)
                    Icon(icon, null, tint = tint)
                }
            }

            Spacer(Modifier.height(10.dp))

            // BOTTOM CONTROLS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Devices, null, tint = vibrantColor)
                Icon(Icons.Default.Share, null, tint = Color.White.copy(0.7f))
                IconButton(onClick = { showPlaylist = !showPlaylist }) {
                    Icon(Icons.Default.QueueMusic, null, tint = Color.White.copy(0.7f))
                }
            }
        }

        // PLAYLIST OVERLAY
        if (showPlaylist) {
            PlaylistOverlay(
                playlist = playlist,
                currentSong = currentSong,
                onClose = { showPlaylist = false },
                onSelect = {
                    playerVM.playSong(it)
                    showPlaylist = false
                }
            )
        }

        // ADD COLLECTION
        if (showCollectionDialog) {
            AddToCollectionDialog(
                currentSong = currentSong,
                snackbarHostState = snackbarHostState,
                onDismiss = { showCollectionDialog = false },
                onAddToCollection = { _, _ ->
                    showCollectionDialog = false
                }
            )
        }

        // SLEEP TIMER DIALOG
        if (showSleepDialog) {
            SleepTimerDialog(
                onDismiss = { showSleepDialog = false },
                onSelect = { duration ->
                    if (duration == 0L) {
                        playerVM.cancelSleepTimer()
                    } else {
                        playerVM.startSleepTimer(duration)
                        showSleepConfirmation = true
                    }
                    showSleepDialog = false
                }
            )
        }

        // SLEEP TIMER CONFIRMATION
        if (showSleepConfirmation) {
            AlertDialog(
                onDismissRequest = { showSleepConfirmation = false },
                title = {
                    Text("Đã thiết lập thời gian ngủ", color = Color.White, fontWeight = FontWeight.Bold)
                },
                text = {
                    sleepEndTime?.let { endTime ->
                        val remainingMs = endTime - System.currentTimeMillis()
                        val remainingMinutes = (remainingMs / 1000 / 60).toInt()
                        Text(
                            "Nhạc sẽ tự động tắt sau $remainingMinutes phút",
                            color = Color.White.copy(0.8f)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showSleepConfirmation = false },
                        colors = ButtonDefaults.buttonColors(containerColor = vibrantColor)
                    ) {
                        Text("OK", color = Color.White)
                    }
                },
                containerColor = Color(0xFF1E1E1E)
            )
        }
    }
}

/* ============================================================
   PLAYLIST OVERLAY
   ============================================================ */
@Composable
fun PlaylistOverlay(
    playlist: List<Song>,
    currentSong: Song?,
    onClose: () -> Unit,
    onSelect: (Int) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xAA000000))
            .clickable { onClose() }
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.7f)
                .background(Color(0xFF1E1E1E), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .clickable(enabled = false) {}
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Đang phát (${playlist.size})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                IconButton(onClick = onClose) { Icon(Icons.Default.Close, null, tint = Color.White) }
            }

            Spacer(Modifier.height(16.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(playlist) { index, song ->
                    PlaylistItemCard(
                        song = song,
                        isPlaying = currentSong == song,
                        onClick = { onSelect(index) }
                    )
                }
            }
        }
    }
}

/* ============================================================
   PLAYLIST ITEM CARD
   ============================================================ */
@Composable
fun PlaylistItemCard(song: Song, isPlaying: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying) Color(0xFF1DB954).copy(0.2f) else Color(0xFF2C2C2C)
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
                Text(song.title, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(song.artist, color = Color.White.copy(0.7f), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            if (isPlaying) {
                Icon(Icons.Default.Equalizer, null, tint = Color(0xFF1DB954))
            }
        }
    }
}

/* ============================================================
   ADD TO COLLECTION DIALOG
   ============================================================ */
@Composable
fun AddToCollectionDialog(
    currentSong: Song?,
    snackbarHostState: SnackbarHostState,
    onDismiss: () -> Unit,
    onAddToCollection: (Song, String) -> Unit
) {
    val collections = listOf("Yêu thích", "Playlist của tôi", "Nhạc buồn", "Nhạc vui", "Tập trung", "Thư giãn")
    var selected by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Thêm vào bộ sưu tập", color = Color.White, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                collections.forEach {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selected = it }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selected == it, onClick = { selected = it })
                        Spacer(Modifier.width(8.dp))
                        Text(it, color = Color.White)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (currentSong != null && selected != null) {
                        onAddToCollection(currentSong, selected!!)
                    }
                }
            ) {
                Text("Thêm", color = Color.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy", color = Color.White)
            }
        },
        containerColor = Color(0xFF1E1E1E)
    )
}

/* ============================================================
   SLEEP TIMER DIALOG
   ============================================================ */
@Composable
fun SleepTimerDialog(
    onDismiss: () -> Unit,
    onSelect: (Long) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    var selectedHour by remember { mutableStateOf(0) }
    var selectedMinute by remember { mutableStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Hẹn giờ tắt nhạc", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF2C2C2C), RoundedCornerShape(8.dp)),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TabButton("Nhanh", selectedTab == 0) { selectedTab = 0 }
                    TabButton("Tùy chỉnh", selectedTab == 1) { selectedTab = 1 }
                }

                Spacer(Modifier.height(16.dp))

                if (selectedTab == 0) {
                    // Quick options
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        TimerOptionCard("5 phút") { onSelect(5 * 60 * 1000L) }
                        TimerOptionCard("10 phút") { onSelect(10 * 60 * 1000L) }
                        TimerOptionCard("20 phút") { onSelect(20 * 60 * 1000L) }
                        TimerOptionCard("30 phút") { onSelect(30 * 60 * 1000L) }
                        TimerOptionCard("1 giờ") { onSelect(60 * 60 * 1000L) }
                        Divider(color = Color.White.copy(0.2f), modifier = Modifier.padding(vertical = 8.dp))
                        TimerOptionCard("Hủy hẹn giờ", isCancel = true) { onSelect(0L) }
                    }
                } else {
                    // Custom time picker
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Chọn thời gian cụ thể", color = Color.White.copy(0.7f), fontSize = 14.sp)
                        Spacer(Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Hour picker
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                IconButton(onClick = { selectedHour = (selectedHour + 1) % 24 }) {
                                    Icon(Icons.Default.KeyboardArrowUp, null, tint = Color.White)
                                }
                                Text(
                                    text = String.format("%02d", selectedHour),
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                IconButton(onClick = { selectedHour = if (selectedHour > 0) selectedHour - 1 else 23 }) {
                                    Icon(Icons.Default.KeyboardArrowDown, null, tint = Color.White)
                                }
                                Text("Giờ", fontSize = 12.sp, color = Color.White.copy(0.6f))
                            }

                            Text(":", fontSize = 32.sp, color = Color.White)

                            // Minute picker
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                IconButton(onClick = { selectedMinute = (selectedMinute + 5) % 60 }) {
                                    Icon(Icons.Default.KeyboardArrowUp, null, tint = Color.White)
                                }
                                Text(
                                    text = String.format("%02d", selectedMinute),
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                IconButton(onClick = { selectedMinute = if (selectedMinute >= 5) selectedMinute - 5 else 55 }) {
                                    Icon(Icons.Default.KeyboardArrowDown, null, tint = Color.White)
                                }
                                Text("Phút", fontSize = 12.sp, color = Color.White.copy(0.6f))
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        Button(
                            onClick = {
                                val calendar = Calendar.getInstance()
                                val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
                                val currentMinute = calendar.get(Calendar.MINUTE)

                                // Tính toán thời gian còn lại đến giờ đã chọn
                                var targetHour = selectedHour
                                var targetMinute = selectedMinute

                                // Nếu thời gian đã chọn đã qua trong ngày, cộng thêm 24 giờ
                                if (targetHour < currentHour || (targetHour == currentHour && targetMinute <= currentMinute)) {
                                    targetHour += 24
                                }

                                val totalMinutesNow = currentHour * 60 + currentMinute
                                val totalMinutesTarget = targetHour * 60 + targetMinute
                                val diffMinutes = totalMinutesTarget - totalMinutesNow

                                val totalMs = diffMinutes * 60 * 1000L
                                if (totalMs > 0) onSelect(totalMs)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DB954))
                        ) {
                            Text("Thiết lập", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        containerColor = Color(0xFF1E1E1E)
    )
}
/* ============================================================
   SỬA LẠI HÀM TABBUTTON NHƯ SAU
   (Chỉ copy 1 hàm này thôi)
   ============================================================ */
@Composable
fun RowScope.TabButton(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .weight(1f) // Lệnh này chỉ chạy được nhờ có RowScope ở dòng trên
            .clickable { onClick() }
            .background(
                if (selected) Color(0xFF1DB954) else Color.Transparent,
                RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 14.sp
        )
    }
}

@Composable
fun TimerOptionCard(text: String, isCancel: Boolean = false, action: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { action() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2C2C2C)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                color = if (isCancel) Color(0xFFFF6B6B) else Color.White,
                fontSize = 16.sp,
                fontWeight = if (isCancel) FontWeight.Bold else FontWeight.Normal
            )
            if (!isCancel) {
                Icon(
                    Icons.Default.Timer,
                    contentDescription = null,
                    tint = Color.White.copy(0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/* ============================================================
   TIME FORMAT
   ============================================================ */
fun formatTime(ms: Long): String {
    val s = ms / 1000
    return "%02d:%02d".format(s / 60, s % 60)
}