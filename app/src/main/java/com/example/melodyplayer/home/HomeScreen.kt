package com.example.melodyplayer.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.melodyplayer.R
import com.example.melodyplayer.model.Song
import com.example.melodyplayer.navigation.Routes
import com.example.melodyplayer.player.MiniPlayer
import com.example.melodyplayer.player.PlayerViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    playerVM: PlayerViewModel
) {
    var selectedTab by remember { mutableStateOf(0) }
    var songs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val currentSong by playerVM.currentSong.collectAsState()
    val isPlaying by playerVM.isPlaying.collectAsState()

    LaunchedEffect(Unit) {
        songs = listOf(
            Song("Bốn chữ lắm", "Trúc Nhân", null, R.raw.bon_chu_lam),
            Song("Đừng trách câu vì đậm", "Cáp Anh tài", null, R.raw.dung_trach_cau_vi_dam),
            Song("Em là cô giáo vùng cao", "Sến Hoàng Mỹ Lam", null, R.raw.em_la_co_giao_vung_cao),
            Song("Ngắm hoa lệ rơi", "Châu Khải Phong", null, R.raw.ngam_hoa_le_roi),
            Song("Original Me", "Astra Yao", null, R.raw.original_me),
            Song("Vùng lá me bay", "Như Quỳnh", null, R.raw.vung_la_me_bay),
            Song("Xin lỗi người anh yêu", "Châu Khải Phong", null, R.raw.xin_loi_nguoi_anh_yeu)
        )
        isLoading = false
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFF1a1a1a),
                drawerContentColor = Color.White
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFF1DB954), Color(0xFF1a1a1a))
                            )
                        )
                        .padding(24.dp),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Column {
                        Surface(
                            modifier = Modifier.size(70.dp),
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.2f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp),
                                    tint = Color.White
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            FirebaseAuth.getInstance().currentUser?.email?.split("@")?.first() ?: "User",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                DrawerItem("Trang chủ", Icons.Default.Home, selectedTab == 0) {
                    selectedTab = 0; scope.launch { drawerState.close() }
                }
                DrawerItem("Playlist", Icons.Default.QueueMusic, selectedTab == 1) {
                    selectedTab = 1; scope.launch { drawerState.close() }
                }
                DrawerItem("Cài đặt", Icons.Default.Settings, selectedTab == 2) {
                    selectedTab = 2; scope.launch { drawerState.close() }
                }

                Spacer(modifier = Modifier.weight(1f))
                Divider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(12.dp))
                DrawerItem("Đăng xuất", Icons.Default.ExitToApp, false, logout = true) {
                    FirebaseAuth.getInstance().signOut()
                    navController.navigate(Routes.AUTH) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "Melody Player",
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            fontSize = 20.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
                        }
                    },
                    actions = {
                        IconButton(onClick = { navController.navigate(Routes.SEARCH) }) {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color(0xFF0D0D0D))
                )
            },
            containerColor = Color(0xFF0D0D0D)
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                HomeScreenUI(
                    songs = songs,
                    isLoading = isLoading,
                    navController = navController,
                    playerVM = playerVM
                )

                if (currentSong != null) {
                    MiniPlayer(
                        song = currentSong,
                        isPlaying = isPlaying,
                        onPlayPause = { playerVM.togglePlayPause() },
                        onNext = { playerVM.nextSong() },
                        onPrev = { playerVM.prevSong() },
                        onClick = { navController.navigate(Routes.PLAYER) },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        }
    }
}

@Composable
fun DrawerItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    logout: Boolean = false,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        label = { Text(title, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
        icon = { Icon(icon, contentDescription = null) },
        selected = selected,
        onClick = onClick,
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = if (!logout) Color(0xFF1DB954).copy(alpha = 0.2f) else Color.Transparent,
            selectedTextColor = if (!logout) Color(0xFF1DB954) else Color(0xFFFF3B30),
            selectedIconColor = if (!logout) Color(0xFF1DB954) else Color(0xFFFF3B30),
            unselectedTextColor = if (!logout) Color.White.copy(alpha = 0.7f) else Color(0xFFFF3B30),
            unselectedIconColor = if (!logout) Color.White.copy(alpha = 0.7f) else Color(0xFFFF3B30)
        ),
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
    )
}

@Composable
fun HomeScreenUI(
    songs: List<Song>,
    isLoading: Boolean,
    navController: NavController,
    playerVM: PlayerViewModel
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 80.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Banner "Khám phá âm nhạc"
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clickable { },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF404040)
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF606060),
                                    Color(0xFF404040)
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(modifier = Modifier.align(Alignment.BottomStart)) {
                        Surface(
                            color = Color(0xFF1DB954),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.TrendingUp,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "TRENDING",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            "Khám phá âm nhạc",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            "Những bản hit mới nhất",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // Playlist nổi bật
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Playlist nổi bật",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        "Xem tất cả",
                        fontSize = 14.sp,
                        color = Color(0xFF1DB954),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable { }
                    )
                }
                Spacer(Modifier.height(16.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(songs.take(4)) { song ->
                        PlaylistCard(song) {
                            playerVM.setPlaylist(songs, songs.indexOf(song))
                            navController.navigate(Routes.PLAYER)
                        }
                    }
                }
            }
        }

        // Gợi ý cho bạn
        item {
            Text(
                "Gợi ý cho bạn",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        if (!isLoading && songs.isNotEmpty()) {
            items(songs.size) { index ->
                val song = songs[index]
                SongItem(
                    song = song,
                    onSongClick = {
                        playerVM.setPlaylist(songs, index)
                        navController.navigate(Routes.PLAYER)
                    },
                    onPlayClick = {
                        playerVM.setPlaylist(songs, index)
                    }
                )
            }
        }

        if (isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF1DB954))
                }
            }
        }
    }
}

@Composable
fun PlaylistCard(song: Song, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(180.dp)
            .height(240.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF282828))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(Color(0xFF404040)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(70.dp),
                        tint = Color.White.copy(alpha = 0.5f)
                    )
                }
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        song.title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        song.artist,
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            FloatingActionButton(
                onClick = { onClick() },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .size(50.dp),
                containerColor = Color(0xFF1DB954),
                elevation = FloatingActionButtonDefaults.elevation(6.dp)
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
fun SongItem(
    song: Song,
    onSongClick: () -> Unit,
    onPlayClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSongClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF282828))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF404040)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(30.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    song.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    song.artist,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = { /* Add to favorites */ }) {
                Icon(
                    Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = Color.White.copy(alpha = 0.6f)
                )
            }

            FloatingActionButton(
                onClick = { onPlayClick() },
                modifier = Modifier.size(48.dp),
                containerColor = Color(0xFF1DB954),
                elevation = FloatingActionButtonDefaults.elevation(4.dp)
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}