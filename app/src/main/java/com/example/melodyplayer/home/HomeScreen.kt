package com.example.melodyplayer.home

import androidx.compose.animation.core.*
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
import com.example.melodyplayer.navigation.Routes
import com.example.melodyplayer.player.MiniPlayer
import com.example.melodyplayer.player.PlayerViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

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

    // Load Firestore
    LaunchedEffect(Unit) {
        try {
            val db = FirebaseFirestore.getInstance()
            val snapshot = db.collection("songs").get().await()
            songs = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Song::class.java)
            }
        } finally {
            isLoading = false
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFF1a1a1a),
                drawerContentColor = Color.White
            ) {
                // Header Drawer
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF1DB954),
                                    Color(0xFF1a1a1a)
                                )
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
                            FirebaseAuth.getInstance().currentUser?.email?.split("@")?.first()
                                ?: "User",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                NavigationDrawerItem(
                    label = {
                        Text(
                            "Trang chủ",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    selected = selectedTab == 0,
                    onClick = {
                        selectedTab = 0
                        scope.launch { drawerState.close() }
                    },
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = Color(0xFF1DB954).copy(alpha = 0.2f),
                        selectedTextColor = Color(0xFF1DB954),
                        selectedIconColor = Color(0xFF1DB954),
                        unselectedTextColor = Color.White.copy(alpha = 0.7f),
                        unselectedIconColor = Color.White.copy(alpha = 0.7f)
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                NavigationDrawerItem(
                    label = {
                        Text(
                            "Playlist",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    icon = { Icon(Icons.Default.QueueMusic, contentDescription = null) },
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        scope.launch { drawerState.close() }
                    },
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = Color(0xFF1DB954).copy(alpha = 0.2f),
                        selectedTextColor = Color(0xFF1DB954),
                        selectedIconColor = Color(0xFF1DB954),
                        unselectedTextColor = Color.White.copy(alpha = 0.7f),
                        unselectedIconColor = Color.White.copy(alpha = 0.7f)
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                NavigationDrawerItem(
                    label = {
                        Text(
                            "Cài đặt",
                            fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    selected = selectedTab == 2,
                    onClick = {
                        selectedTab = 2
                        scope.launch { drawerState.close() }
                    },
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = Color(0xFF1DB954).copy(alpha = 0.2f),
                        selectedTextColor = Color(0xFF1DB954),
                        selectedIconColor = Color(0xFF1DB954),
                        unselectedTextColor = Color.White.copy(alpha = 0.7f),
                        unselectedIconColor = Color.White.copy(alpha = 0.7f)
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                Spacer(modifier = Modifier.weight(1f))

                Divider(
                    color = Color.White.copy(alpha = 0.1f),
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                NavigationDrawerItem(
                    label = { Text("Đăng xuất") },
                    icon = { Icon(Icons.Default.ExitToApp, contentDescription = null) },
                    selected = false,
                    onClick = {
                        FirebaseAuth.getInstance().signOut()
                        navController.navigate(Routes.AUTH) {
                            popUpTo(Routes.HOME) { inclusive = true }
                        }
                    },
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedTextColor = Color(0xFFFF3B30),
                        unselectedIconColor = Color(0xFFFF3B30)
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)
                )
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
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = "Menu",
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { navController.navigate(Routes.SEARCH) }) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Search",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color(0xFF0D0D0D)
                    )
                )
            },
            containerColor = Color(0xFF0D0D0D)
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                when (selectedTab) {
                    0 -> HomeScreenUI(
                        songs = songs,
                        isLoading = isLoading,
                        navController = navController,
                        playerVM = playerVM
                    )
                    1 -> PlaylistContent()
                    2 -> SettingsContent(navController)
                }

                // MiniPlayer
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
fun HomeScreenUI(
    songs: List<Song>,
    isLoading: Boolean,
    navController: NavController,
    playerVM: PlayerViewModel
) {
    var currentBannerIndex by remember { mutableStateOf(0) }

    LaunchedEffect(songs) {
        if (songs.isNotEmpty()) {
            while (true) {
                kotlinx.coroutines.delay(5000)
                currentBannerIndex = (currentBannerIndex + 1) % songs.size
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            // Banner
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .padding(16.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxSize()
                            .shadow(16.dp, RoundedCornerShape(20.dp)),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Box {
                            if (songs.isNotEmpty()) {
                                AsyncImage(
                                    model = songs[currentBannerIndex % songs.size].coverUrl
                                        ?: android.R.drawable.ic_media_play,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                Color.Black.copy(alpha = 0.8f)
                                            )
                                        )
                                    )
                            )

                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(20.dp)
                            ) {
                                Surface(
                                    color = Color(0xFF1DB954),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        "🔥 TRENDING",
                                        modifier = Modifier.padding(
                                            horizontal = 12.dp,
                                            vertical = 6.dp
                                        ),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Khám phá âm nhạc",
                                    color = Color.White,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    "Những bản hit mới nhất",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }

            // Playlist nổi bật
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Playlist nổi bật",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    TextButton(onClick = { navController.navigate(Routes.PLAYLIST_ALL) }) {
                        Text(
                            "Xem tất cả",
                            color = Color(0xFF1DB954),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            item {
                if (isLoading) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF1DB954),
                            modifier = Modifier.size(40.dp)
                        )
                    }
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        items(songs.take(10)) { song ->
                            PlaylistCard(song) {
                                playerVM.setPlaylist(songs, songs.indexOf(song))
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        "Gợi ý cho bạn",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            items(songs) { song ->
                SongListItem(song) {
                    playerVM.setPlaylist(songs, songs.indexOf(song))
                }
            }
        }
    }
}

@Composable
fun PlaylistCard(song: Song, onSongClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .shadow(8.dp, RoundedCornerShape(16.dp))
            .clickable { onSongClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1a1a1a)
        )
    ) {
        Column {
            Box {
                AsyncImage(
                    model = song.coverUrl ?: android.R.drawable.ic_media_play,
                    contentDescription = song.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentScale = ContentScale.Crop
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier.size(50.dp),
                        shape = CircleShape,
                        color = Color(0xFF1DB954).copy(alpha = 0.9f)
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = Color.White,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    song.title,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    song.artist,
                    color = Color.White.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun SongListItem(song: Song, onSongClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onSongClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1a1a1a)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Card(
                modifier = Modifier.size(60.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                AsyncImage(
                    model = song.coverUrl ?: android.R.drawable.ic_media_play,
                    contentDescription = song.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    song.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    song.artist,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = { /* TODO: favorite */ },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = Color.White.copy(alpha = 0.6f)
                )
            }

            IconButton(
                onClick = { onSongClick() },
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFF1DB954), CircleShape)
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun PlaylistContent() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.QueueMusic,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = Color.White.copy(alpha = 0.3f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Playlist của bạn",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Chưa có playlist nào 🎶",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun SettingsContent(navController: NavController) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "Cài đặt",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1a1a1a)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SettingItem(
                        icon = Icons.Default.Person,
                        title = "Tài khoản",
                        subtitle = FirebaseAuth.getInstance().currentUser?.email
                            ?: "Không xác định",
                        onClick = { }
                    )
                    Divider(
                        color = Color.White.copy(alpha = 0.1f),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    SettingItem(
                        icon = Icons.Default.Notifications,
                        title = "Thông báo",
                        subtitle = "Quản lý thông báo",
                        onClick = { }
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }

        item {
            Button(
                onClick = {
                    FirebaseAuth.getInstance().signOut()
                    navController.navigate(Routes.AUTH) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .shadow(8.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFFFF3B30),
                                    Color(0xFFFF6B6B)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.ExitToApp,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Đăng xuất",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(45.dp),
            shape = CircleShape,
            color = Color(0xFF1DB954).copy(alpha = 0.2f)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = Color(0xFF1DB954),
                modifier = Modifier.padding(10.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
            Text(
                subtitle,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 13.sp
            )
        }

        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.4f)
        )
    }
}