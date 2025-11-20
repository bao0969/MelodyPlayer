package com.example.melodyplayer.home

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.melodyplayer.model.Song
import com.example.melodyplayer.navigation.Routes
import com.example.melodyplayer.player.MiniPlayer
import com.example.melodyplayer.player.PlayerViewModel
import com.example.melodyplayer.search.VoiceSearchDialog // ĐÃ THÊM IMPORT
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.Normalizer
import java.util.*

// ======================================================
// DATASTORE
// ======================================================

private val Context.dataStore by preferencesDataStore(name = "user_added_songs_storage")
private val LOCAL_SONGS_KEY = stringPreferencesKey("local_songs_list_json")

// ======================================================
// PALETTE
// ======================================================

private val ColorPalette = listOf(
    listOf(Color(0xFFFF6B9D), Color(0xFFC44569)),
    listOf(Color(0xFF4FACFE), Color(0xFF00F2FE)),
    listOf(Color(0xFFFA709A), Color(0xFFFEE140)),
    listOf(Color(0xFF30CFD0), Color(0xFF330867)),
    listOf(Color(0xFFA8EDEA), Color(0xFFFED6E3)),
    listOf(Color(0xFFFF9A56), Color(0xFFFF6A88)),
    listOf(Color(0xFF667EEA), Color(0xFF764BA2))
)

// ======================================================
// HOME SCREEN
// ======================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    playerVM: PlayerViewModel
) {
    var allSongs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val context = LocalContext.current

    // load data
    LaunchedEffect(Unit) {
        isLoading = true
        val defaultSongs = getDefaultSongs()
        val localSongs = getLocalSongs(context)
        allSongs = defaultSongs + localSongs
        isLoading = false
    }

    var showAddDialog by remember { mutableStateOf(false) }

    // --- ĐÃ THÊM: Biến quản lý dialog giọng nói ---
    var showVoiceDialog by remember { mutableStateOf(false) }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val currentSong by playerVM.currentSong.collectAsState()
    val isPlaying by playerVM.isPlaying.collectAsState()
    var isSearching by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredSongs by remember(searchQuery, allSongs) {
        derivedStateOf {
            if (searchQuery.isBlank()) {
                emptyList()
            } else {
                val normalizedQuery =
                    searchQuery.unaccent().lowercase(Locale.getDefault())
                allSongs.filter { song ->
                    val normalizedTitle =
                        song.title.unaccent().lowercase(Locale.getDefault())
                    val normalizedArtist =
                        song.artist.unaccent().lowercase(Locale.getDefault())
                    normalizedTitle.contains(normalizedQuery) ||
                            normalizedArtist.contains(normalizedQuery)
                }
            }
        }
    }

    // greeting
    var greeting by remember { mutableStateOf("") }
    var greetingIcon by remember { mutableStateOf("☀️") }

    LaunchedEffect(Unit) {
        while (true) {
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            when (hour) {
                in 0..11 -> {
                    greeting = "Chào buổi sáng"
                    greetingIcon = "☀️"
                }
                in 12..17 -> {
                    greeting = "Chào buổi chiều"
                    greetingIcon = "🌤️"
                }
                else -> {
                    greeting = "Chào buổi tối"
                    greetingIcon = "🌙"
                }
            }
            delay(60000L)
        }
    }

    // dialog thêm bài hát
    if (showAddDialog) {
        AddSongDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title, artist, audioUri, imageUri ->
                scope.launch {
                    val newSong = Song(
                        title = title,
                        artist = artist,
                        audioUrl = audioUri.toString(),
                        imageUrl = imageUri?.toString()
                    )
                    val currentLocalSongs = getLocalSongs(context)
                    saveLocalSongs(context, currentLocalSongs + newSong)
                    val updatedSongs = allSongs + newSong
                    allSongs = updatedSongs
                    playerVM.setPlaylist(updatedSongs, updatedSongs.lastIndex)
                    showAddDialog = false
                }
            }
        )
    }

    // --- ĐÃ THÊM: Hiển thị Dialog Voice Search ---
    if (showVoiceDialog) {
        VoiceSearchDialog(
            onDismiss = { showVoiceDialog = false },
            onResult = { result ->
                searchQuery = result
                showVoiceDialog = false
            }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModernDrawer(
                onItemClick = { scope.launch { drawerState.close() } },
                onLogout = {
                    FirebaseAuth.getInstance().signOut()
                    navController.navigate(Routes.AUTH) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                }
            )
        }
    ) {
        // === Scaffold với nền neon ===
        Scaffold(
            topBar = {
                if (isSearching) {
                    SearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        onBackClick = {
                            isSearching = false
                            searchQuery = ""
                        },
                        // --- ĐÃ THÊM: Sự kiện mở dialog giọng nói ---
                        onVoiceClick = { showVoiceDialog = true }
                    )
                } else {
                    MainTopBar(
                        onMenuClick = { scope.launch { drawerState.open() } },
                        onAddClick = { showAddDialog = true },
                        onSearchClick = { isSearching = true }
                    )
                }
            },
            containerColor = Color.Transparent
        ) { padding ->

            // layer 1: nền gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF0A0018),
                                Color(0xFF19003A),
                                Color(0xFF270050),
                                Color(0xFF0A0018)
                            )
                        )
                    )
            ) {
                // layer 2: glow trung tâm
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFFF00FF).copy(alpha = 0.25f),
                                    Color(0xFF00FFFF).copy(alpha = 0.15f),
                                    Color.Transparent
                                ),
                                center = Offset(600f, 1200f),
                                radius = 900f
                            )

                        )
                )

                // layer 3: glow bên trái
                Box(
                    modifier = Modifier
                        .size(350.dp)
                        .offset(x = (-100).dp, y = 150.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF00FF).copy(alpha = 0.1f))
                        .blur(140.dp)
                )

                // layer 4: glow bên phải
                Box(
                    modifier = Modifier
                        .size(300.dp)
                        .offset(x = 240.dp, y = 650.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00FFFF).copy(alpha = 0.1f))
                        .blur(120.dp)
                )

                // nội dung chính
                Box(modifier = Modifier.padding(padding)) {
                    if (isLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color(0xFF1DB954))
                        }
                    } else {
                        if (isSearching) {
                            SearchResults(
                                query = searchQuery,
                                filteredSongs = filteredSongs,
                                allSongs = allSongs,
                                navController = navController,
                                playerVM = playerVM
                            )
                        } else {
                            MainContent(
                                greeting = greeting,
                                greetingIcon = greetingIcon,
                                songs = allSongs,
                                navController = navController,
                                playerVM = playerVM
                            )
                        }
                    }

                    // mini player
                    currentSong?.let {
                        MiniPlayer(
                            song = it,
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
}

// ======================================================
// DIALOG THÊM BÀI HÁT (GIỮ NGUYÊN)
// ======================================================

@Composable
private fun AddSongDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Uri, Uri?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var artist by remember { mutableStateOf("") }
    var selectedAudioUri by remember { mutableStateOf<Uri?>(null) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current

    fun safePersistPermission(uri: Uri) {
        val flag = Intent.FLAG_GRANT_READ_URI_PERMISSION
        try {
            context.contentResolver.takePersistableUriPermission(uri, flag)
        } catch (_: SecurityException) {
        } catch (_: Exception) {
        }
    }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            safePersistPermission(uri)
            selectedAudioUri = uri
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            safePersistPermission(uri)
            selectedImageUri = uri
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1F3A),
        title = {
            Text(
                "Thêm bài hát",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 20.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

                // Ảnh bìa
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .align(Alignment.CenterHorizontally)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF2A3050))
                        .clickable { imagePickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedImageUri != null) {
                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.Default.AddPhotoAlternate,
                            null,
                            tint = Color(0xFF7A8BA0),
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                // Tên bài hát
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Tên bài hát") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF1DB954),
                        unfocusedBorderColor = Color(0xFF4A5568)
                    )
                )

                // Nghệ sĩ
                OutlinedTextField(
                    value = artist,
                    onValueChange = { artist = it },
                    label = { Text("Nghệ sĩ") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF1DB954),
                        unfocusedBorderColor = Color(0xFF4A5568)
                    )
                )

                // Chọn tệp nhạc
                Button(
                    onClick = { audioPickerLauncher.launch("audio/*") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedAudioUri == null)
                            Color(0xFF2A3050)
                        else
                            Color(0xFF1DB954)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        if (selectedAudioUri == null) Icons.Default.MusicNote else Icons.Default.CheckCircle,
                        null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(if (selectedAudioUri == null) "Chọn tệp nhạc" else "Đã chọn")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedAudioUri?.let { audioUri ->
                        onConfirm(title.trim(), artist.trim(), audioUri, selectedImageUri)
                    }
                },
                enabled = title.isNotBlank() && artist.isNotBlank() && selectedAudioUri != null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1DB954)
                )
            ) {
                Text("Thêm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy", color = Color(0xFFB3B3B3))
            }
        }
    )
}

// ======================================================
// DATASTORE HELPER (GIỮ NGUYÊN)
// ======================================================

private suspend fun saveLocalSongs(context: Context, songs: List<Song>) {
    val jsonString = Json.encodeToString(songs)
    context.dataStore.edit { preferences ->
        preferences[LOCAL_SONGS_KEY] = jsonString
    }
}

private suspend fun getLocalSongs(context: Context): List<Song> {
    return try {
        val jsonString =
            context.dataStore.data.map { it[LOCAL_SONGS_KEY] ?: "[]" }.first()
        Json.decodeFromString(jsonString)
    } catch (e: Exception) {
        emptyList()
    }
}

// ======================================================
// TOP BAR
// ======================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainTopBar(
    onMenuClick: () -> Unit,
    onAddClick: () -> Unit,
    onSearchClick: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                "MelodyPlayer",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF00FFFF)
            )
        },
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Default.Menu, null, tint = Color(0xFFFF00FF))
            }
        },
        actions = {
            IconButton(onClick = onSearchClick) {
                Icon(Icons.Default.Search, null, tint = Color(0xFF00FFFF))
            }
            IconButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, null, tint = Color(0xFFFF66FF))
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color(0xFF120028)
        )
    )
}

// ======================================================
// SEARCH BAR (ĐÃ SỬA - QUAN TRỌNG)
// ======================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onBackClick: () -> Unit,
    onVoiceClick: () -> Unit // --- ĐÃ THÊM THAM SỐ NÀY ---
) {
    TopAppBar(
        title = {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("Tìm kiếm...", color = Color.White.copy(0.5f)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color(0xFF1DB954),
                    focusedBorderColor = Color(0xFF1DB954),
                    unfocusedBorderColor = Color.White.copy(0.3f)
                ),
                shape = RoundedCornerShape(24.dp),
                leadingIcon = {
                    Icon(Icons.Default.Search, null, tint = Color.White.copy(0.7f))
                },
                // --- ĐÃ SỬA: Logic hiển thị Micro/Clear ---
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Default.Clear, null, tint = Color.White)
                        }
                    } else {
                        // Khi chưa nhập gì -> Hiện Micro
                        IconButton(onClick = onVoiceClick) {
                            Icon(
                                Icons.Default.Mic,
                                null,
                                tint = Color(0xFF1DB954), // Màu xanh Spotify nổi bật
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(Icons.Default.ArrowBack, null, tint = Color.White)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color(0xFF0D0F1F)
        )
    )
}

// ======================================================
// DRAWER (GIỮ NGUYÊN)
// ======================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModernDrawer(onItemClick: () -> Unit, onLogout: () -> Unit) {
    ModalDrawerSheet(
        drawerContainerColor = Color(0xFF0A0018),
        drawerContentColor = Color.White
    ) {
        // header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFFFF00FF), Color(0xFF120028))
                    )
                )
                .padding(24.dp),
            contentAlignment = Alignment.BottomStart
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(Color(0xFF00FFFF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        null,
                        tint = Color.Black,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    FirebaseAuth.getInstance().currentUser?.email?.split("@")?.first()
                        ?: "NeonUser",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    "Synthwave Lover 💿",
                    fontSize = 14.sp,
                    color = Color.White.copy(0.7f)
                )
            }
        }

        // logout
        NavigationDrawerItem(
            label = { Text("Đăng xuất", fontSize = 16.sp) },
            icon = { Icon(Icons.Default.ExitToApp, null) },
            selected = false,
            onClick = onLogout,
            colors = NavigationDrawerItemDefaults.colors(
                unselectedTextColor = Color(0xFFFF66FF),
                unselectedIconColor = Color(0xFFFF66FF)
            ),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)
        )
    }
}

// ======================================================
// MAIN CONTENT (GIỮ NGUYÊN)
// ======================================================

@Composable
private fun MainContent(
    greeting: String,
    greetingIcon: String,
    songs: List<Song>,
    navController: NavController,
    playerVM: PlayerViewModel
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 80.dp)
    ) {
        // greeting
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF1E003A).copy(0.8f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(20.dp)
            ) {
                Text(
                    greetingIcon,
                    fontSize = 40.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    greeting,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFFF66FF)
                )
                Text(
                    "Sẵn sàng khám phá thế giới nhạc neon ✨",
                    fontSize = 15.sp,
                    color = Color(0xFF00FFFF),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        item { Spacer(Modifier.height(20.dp)) }

        // quick access
        item {
            Text(
                "Truy cập nhanh",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
            )
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                songs.take(6).chunked(2).forEachIndexed { rowIndex, rowSongs ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowSongs.forEachIndexed { colIndex, song ->
                            val colorIndex = rowIndex * 2 + colIndex
                            QuickAccessCard(
                                song = song,
                                colors = ColorPalette[colorIndex % ColorPalette.size],
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    playerVM.setPlaylist(songs, songs.indexOf(song))
                                    navController.navigate(Routes.PLAYER)
                                }
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }

        // banner
        item {
            FeaturedCard(
                onClick = {
                    navController.navigate(Routes.COLLECTIONS) {
                        launchSingleTop = true
                    }

                }
            )

        }

        item { Spacer(Modifier.height(24.dp)) }

        // section: dành cho bạn
        item {
            SectionTitle("Dành cho bạn", "Xem tất cả", onActionClick = {
                if (songs.isNotEmpty()) {
                    val title = "Dành cho bạn"
                    val playlist = songs.take(10)
                    val songsJson = Json.encodeToString(playlist)

                    val encodedTitle = Uri.encode(title)
                    val encodedJson = Uri.encode(songsJson)

                    navController.navigate("collection/$encodedTitle/$encodedJson") {
                        launchSingleTop = true
                    }
                }
            })
        }

        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
            ) {
                items(songs.take(10)) { song ->
                    SongCard(
                        song = song,
                        colors = ColorPalette[songs.indexOf(song) % ColorPalette.size],
                        onClick = {
                            playerVM.setPlaylist(songs, songs.indexOf(song))
                            navController.navigate(Routes.PLAYER)
                        }
                    )
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }

        // section: trending
        item {
            SectionTitle("Trending 🔥", "Xem tất cả", onActionClick = {
                if (songs.isNotEmpty()) {
                    val title = "Trending 🔥"
                    val playlist = songs.takeLast(10)
                    val songsJson = Json.encodeToString(playlist)

                    val encodedTitle = Uri.encode(title)
                    val encodedJson = Uri.encode(songsJson)

                    navController.navigate("collection/$encodedTitle/$encodedJson") {
                        launchSingleTop = true
                    }
                }
            })
        }

        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
            ) {
                items(songs.takeLast(10)) { song ->
                    SongCard(
                        song = song,
                        colors = ColorPalette[songs.indexOf(song) % ColorPalette.size],
                        onClick = {
                            playerVM.setPlaylist(songs, songs.indexOf(song))
                            navController.navigate(Routes.PLAYER)
                        }
                    )
                }
            }
        }

        item { Spacer(Modifier.height(32.dp)) }
    }
}

// ======================================================
// CARDS (GIỮ NGUYÊN)
// ======================================================

@Composable
private fun QuickAccessCard(
    song: Song,
    colors: List<Color>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(70.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(Color.Transparent)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            if (!song.imageUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = song.imageUrl,
                    contentDescription = song.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    error = painterResource(android.R.drawable.ic_menu_gallery),
                    placeholder = painterResource(android.R.drawable.ic_menu_gallery)
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(0.4f))
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xFFFF00FF),
                                    Color(0xFF00FFFF)
                                )
                            )
                        )
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.MusicNote,
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    song.title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun FeaturedCard(onClick: () -> Unit) {
    val rotation by rememberInfiniteTransition(label = "rotation").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing)
        ),
        label = "rotation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .padding(horizontal = 20.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFFFF00FF),
                            Color(0xFF00FFFF),
                            Color(0xFF7B2FF7)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Surface(
                    color = Color.White.copy(0.1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        "NEON MIX",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Vibes Of The Night 🌙",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Text(
                    "Synthwave • Electro • Lo-Fi",
                    fontSize = 14.sp,
                    color = Color.White.copy(0.8f)
                )
            }

            Icon(
                Icons.Default.MusicNote,
                null,
                modifier = Modifier
                    .size(70.dp)
                    .align(Alignment.CenterEnd)
                    .rotate(rotation),
                tint = Color.White.copy(0.4f)
            )
        }
    }
}

@Composable
private fun SectionTitle(title: String, action: String, onActionClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        TextButton(onClick = onActionClick) {
            Text(
                action,
                fontSize = 14.sp,
                color = Color(0xFF1DB954)
            )
        }
    }
}


@Composable
private fun SongCard(
    song: Song,
    colors: List<Color>,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .clickable(onClick = onClick)
    ) {
        Card(
            modifier = Modifier.size(140.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(Color.Transparent)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                val imageUrl = song.imageUrl
                if (!imageUrl.isNullOrEmpty()) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = song.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            error = painterResource(android.R.drawable.ic_menu_gallery),
                            placeholder = painterResource(android.R.drawable.ic_menu_gallery)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    listOf(Color(0xFFFF00FF), Color(0xFF00FFFF))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.MusicNote,
                            null,
                            modifier = Modifier.size(60.dp),
                            tint = Color.White.copy(0.9f)
                        )
                    }
                }

                FloatingActionButton(
                    onClick = onClick,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .size(40.dp),
                    containerColor = Color.White
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        null,
                        tint = Color(0xFFFF00FF),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(song.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(song.artist, color = Color(0xFFB3B3B3), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// ======================================================
// SEARCH RESULTS (GIỮ NGUYÊN)
// ======================================================

@Composable
private fun SearchResults(
    query: String,
    filteredSongs: List<Song>,
    allSongs: List<Song>,
    navController: NavController,
    playerVM: PlayerViewModel
) {
    if (query.isNotBlank() && filteredSongs.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp)
            ) {
                Icon(
                    Icons.Default.SearchOff,
                    null,
                    tint = Color(0xFF4A5568),
                    modifier = Modifier.size(80.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "Không tìm thấy",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Thử từ khóa khác",
                    color = Color(0xFF7A8BA0),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    } else if (query.isNotBlank()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredSongs) { song ->
                SearchResultCard(
                    song = song,
                    onClick = {
                        val index = allSongs.indexOf(song)
                        if (index != -1) {
                            playerVM.setPlaylist(allSongs, index)
                            navController.navigate(Routes.PLAYER)
                        }
                    }
                )
            }
        }
    } else {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp)
            ) {
                Icon(
                    Icons.Default.Search,
                    null,
                    tint = Color(0xFF4A5568),
                    modifier = Modifier.size(80.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "Tìm kiếm bài hát",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Nhập tên bài hát hoặc nghệ sĩ",
                    color = Color(0xFF7A8BA0),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
fun SearchResultCard(
    song: Song,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(Color(0xFF1A1F3A))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF1DB954), Color(0xFF127A3D))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.MusicNote,
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
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
                Text(
                    song.artist,
                    color = Color(0xFFB3B3B3),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Icon(
                Icons.Default.PlayArrow,
                null,
                tint = Color(0xFF1DB954),
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

// ======================================================
// HELPERS (GIỮ NGUYÊN)
// ======================================================

private val REGEX_UNACCENT = "\\p{InCombiningDiacriticalMarks}+".toRegex()
private fun String.unaccent(): String {
    val temp = Normalizer.normalize(this, Normalizer.Form.NFD)
    return REGEX_UNACCENT
        .replace(temp, "")
        .replace("đ", "d")
        .replace("Đ", "D")
}

// ======================================================
// DEFAULT SONGS (ĐÃ RÚT GỌN ĐỂ FILE KHÔNG QUÁ DÀI KHI COPY,
// BẠN CÓ THỂ DÙNG LẠI LIST CŨ CỦA BẠN NẾU MUỐN GIỮ FULL DATA)
// ======================================================

private fun getDefaultSongs() = listOf(
    Song(
        title = "Sao Cha Không Về (Bố Già OST)",
        artist = "Ali Hoàng Dương",
        resId = "ali_hoang_duong_bo_gia_ost_official_mv",
        imageUrl = "https://i.ytimg.com/vi/TD7sBUigDIU/maxresdefault.jpg"
    ),
    Song(
        title = "Ex's Hate Me",
        artist = "B-Ray x Masew ft. Amee",
        resId = "b_ray_x_masew_ft_amee_official_mv",
        imageUrl = "https://i.ytimg.com/vi/BxhYw888dPs/maxresdefault.jpg"
    ),
    // ... (Giữ nguyên danh sách bài hát cũ của bạn ở đây.
    // Mình không copy hết để tránh làm loãng câu trả lời,
    // nhưng code logic ở trên vẫn hoạt động hoàn hảo với list cũ của bạn)
    Song(
        title = "Bước Qua Mùa Cô Đơn",
        artist = "Vũ.",
        resId = "buoc_qua_mua_co_don_vu_official_mv",
        imageUrl = "https://i.ytimg.com/vi/B1EG31dqaF0/hq720.jpg"
    ),
    Song(
        title = "Bước Qua Nhau",
        artist = "Vũ.",
        resId = "buoc_qua_nhau_vu_official_mv",
        imageUrl = "https://i.ytimg.com/vi/yPHoBhEqhKU/maxresdefault.jpg"
    ),
    Song(
        title = "Bật Tình Yêu Lên",
        artist = "Hòa Minzy x Tăng Duy Tân",
        resId = "bat_tinh_yeu_len_hoa_minzy_x_tang_duy_tan_mv_lyrics",
        imageUrl = "https://i.ytimg.com/vi/a42p96AYZoE/maxresdefault.jpg"
    ),
    Song(
        title = "Bốn Chữ Lắm",
        artist = "Trúc Nhân ft. Trương Thảo Nhi",
        resId = "bon_chu_lam_mv_truc_nhan_truong_thao_nhi_chat_luong_4k",
        imageUrl = "https://i.ytimg.com/vi/s9gTb2WgenA/maxresdefault.jpg"
    ),
    Song(
        title = "Chiều Hôm Ấy",
        artist = "JayKii",
        resId = "chieu_hom_ay_official_mv",
        imageUrl = "https://i.ytimg.com/vi/l1JMh1G0g8U/maxresdefault.jpg"
    ),
    Song(
        title = "Chúng Ta Không Thuộc Về Nhau",
        artist = "Sơn Tùng M-TP",
        resId = "chung_ta_khong_thuoc_ve_nhau_official_music_video_son_tung_m_tp",
        imageUrl = "https://i.ytimg.com/vi/vWHBPVxVIjc/maxresdefault.jpg"
    ),
    Song(
        title = "Chúng Ta Của Hiện Tại",
        artist = "Sơn Tùng M-TP",
        resId = "chung_ta_cua_hien_tai",
        imageUrl = "https://i.ytimg.com/vi/psZ1g9fMfeo/maxresdefault.jpg"
    ),
    Song(
        title = "Còn Yêu, Đâu Ai Rời Đi",
        artist = "Đức Phúc",
        resId = "con_yeu_dau_ai_roi_di_duc_phuc_official_mv",
        imageUrl = "https://i.ytimg.com/vi/oZVCI_h8PYc/maxresdefault.jpg"
    ),
    Song(
        title = "Cơn Mưa Ngang Qua",
        artist = "Sơn Tùng M-TP",
        resId = "con_mua_ngang_qua_mtp_son_tung_k",
        imageUrl = "https://i.ytimg.com/vi/BhFwff96G5g/maxresdefault.jpg"
    ),
    Song(
        title = "Em Của Ngày Hôm Qua",
        artist = "Sơn Tùng M-TP",
        resId = "em_cua_ngay_hom_qua",
        imageUrl = "https://i.ytimg.com/vi/XOs9FnDIlUM/maxresdefault.jpg"
    ),
    Song(
        title = "Em Hát Ai Nghe",
        artist = "Orange",
        resId = "em_hat_ai_nghe_official_mv",
        imageUrl = "https://i.ytimg.com/vi/TlVTfQdWIV8/maxresdefault.jpg"
    ),
    Song(
        title = "Fake Love x Wolves x Nothing Stopping Me",
        artist = "Remix",
        resId = "fake_love_x_wolves_x_nothing_stopping_me_track_edm_remix_hot_tiktok_2024",
        imageUrl = "https://i.ytimg.com/vi/aLzWmaeA2EM/maxresdefault.jpg"
    ),
    Song(
        title = "Người Tình Mùa Đông",
        artist = "Hòa Minzy",
        resId = "hoa_minzy_nguoi_tinh_mua_dong_official_music_video",
        imageUrl = "https://i.ytimg.com/vi/U8MWAH53J1I/maxresdefault.jpg"
    ),
    Song(
        title = "Hơn Cả Yêu",
        artist = "Đức Phúc",
        resId = "hon_ca_yeu_duc_phuc_official_music_video",
        imageUrl = "https://i.ytimg.com/vi/cwBFu5CSnhA/maxresdefault.jpg"
    ),
    Song(
        title = "Rồi Mình Kể Nhau Nghe Chuyện Đêm",
        artist = "Hương Ly (Cover)",
        resId = "huong_ly_cover_vietz_remix_roi_minh_ke_nhau_nghe_chuyen_dem",
        imageUrl = "https://i.ytimg.com/vi/i78L5gq_w_o/maxresdefault.jpg"
    ),
    Song(
        title = "Hết Thương Cạn Nhớ",
        artist = "Đức Phúc",
        resId = "het_thuong_can_nho_duc_phuc_official_music_video",
        imageUrl = "https://i.ytimg.com/vi/X3JBXJDdgXw/maxresdefault.jpg"
    ),
    Song(
        title = "Sóng Gió",
        artist = "ICM x Jack",
        resId = "icm_x_jack_official_music_video",
        imageUrl = "https://i.ytimg.com/vi/1nDa3c8bfKE/maxresdefault.jpg"
    ),
    Song(
        title = "Hồng Nhan",
        artist = "Jack (G5R)",
        resId = "jack_hong_nhan_official_mv_g5r",
        imageUrl = "https://i.ytimg.com/vi/CyoqZfMxHLw/maxresdefault.jpg"
    ),
    Song(
        title = "Cuối Cùng Thì",
        artist = "Jack (J97)",
        resId = "jack_j97_cuoi_cung_thi_special_stage_video",
        imageUrl = "https://i.ytimg.com/vi/y-a1H0C6plo/maxresdefault.jpg"
    ),
    Song(
        title = "Thiên Lý Ơi",
        artist = "Jack (J97)",
        resId = "jack_j97_thien_ly_oi_official_music_video",
        imageUrl = "https://i.ytimg.com/vi/bM-3drXw8Xo/maxresdefault.jpg"
    ),
    Song(
        title = "Bạc Phận",
        artist = "Jack x K-ICM",
        resId = "jack_x_k_icm",
        imageUrl = "https://i.ytimg.com/vi/hGBHNS_TaXk/maxresdefault.jpg"
    ),
    Song(
        title = "Khi Em Lớn",
        artist = "Orange",
        resId = "khi_em_lon_official_mv",
        imageUrl = "https://i.ytimg.com/vi/x1rPpcXZicI/maxresdefault.jpg"
    ),
    Song(
        title = "Bước Qua Đời Nhau",
        artist = "Khắc Việt",
        resId = "khac_viet_lyrics_video",
        imageUrl = "https://i.ytimg.com/vi/DQNW9LoSjL8/maxresdefault.jpg"
    ),
    Song(
        title = "Love Is Gone",
        artist = "SLANDER ft. Dylan Matthew",
        resId = "love_is_gone_lyrics_ft_dylan_matthew",
        imageUrl = "https://i.ytimg.com/vi/7LYcVR0YXvc/maxresdefault.jpg"
    ),
    Song(
        title = "Lạ Lùng",
        artist = "Vũ.",
        resId = "la_lung_vu_original",
        imageUrl = "https://i.ytimg.com/vi/rwBGFGD4r9E/maxresdefault.jpg"
    ),
    Song(
        title = "Tình Yêu Chậm Trễ",
        artist = "MONSTAR",
        resId = "monstar_official_music_video",
        imageUrl = "https://i.ytimg.com/vi/_lbgfZAXCzA/maxresdefault.jpg"
    ),
    Song(
        title = "Thu Cuối",
        artist = "Mr.T ft. Yanbi & Hằng Bingboong",
        resId = "mrt_ft_yanbi_x_hang_bing_boong_nhac_remix_bat_hu_di_cung_thoi_gian",
        imageUrl = "https://i.ytimg.com/vi/xypzmu5mMPY/maxresdefault.jpg"
    ),
    Song(
        title = "Ngày Đầu Tiên",
        artist = "Đức Phúc",
        resId = "ngay_dau_tien_duc_phuc_official_music_video_valentine_2022",
        imageUrl = "https://i.ytimg.com/vi/1P-JtQP5BH0/maxresdefault.jpg"
    ),
    Song(
        title = "Những Lời Hứa Bỏ Quên",
        artist = "Vũ. x Dear Jane",
        resId = "nhung_loi_hua_bo_quen_vu_x_dear_jane_official_mv_tu_album_bao_tang_cua_nuoi_tiec",
        imageUrl = "https://i.ytimg.com/vi/qdkZbYMYiU4/maxresdefault.jpg"
    ),
    Song(
        title = "Nơi Này Có Anh",
        artist = "Sơn Tùng M-TP",
        resId = "noi_nay_co_anh_official_music_video_son_tung_m_tp",
        imageUrl = "https://i.ytimg.com/vi/BaHiG5jrMLc/maxresdefault.jpg"
    ),
    Song(
        title = "24H",
        artist = "LyLy ft. Magazine",
        resId = "official_music_video_lyly_ft_magazine",
        imageUrl = "https://i.ytimg.com/vi/WqOxNspDyBk/maxresdefault.jpg"
    ),
    Song(
        title = "Anh Không Đòi Quà",
        artist = "Only C ft. Karik",
        resId = "only_c_ft_karik_official_music_video",
        imageUrl = "https://i.ytimg.com/vi/PmI1kppNRpI/maxresdefault.jpg"
    ),
    Song(
        title = "Đừng Kết Thúc Hôm Nay",
        artist = "Orange (Prod. by Madihu)",
        resId = "orange_dung_ket_thuc_hom_nay_official_mv_prod_by_madihu",
        imageUrl = "https://i.ytimg.com/vi/lLChOb8eV_c/maxresdefault.jpg"
    ),
    Song(
        title = "Đừng Tốt Với Em",
        artist = "Orange x DT Tập Rap",
        resId = "orange_x_dt_tap_rap_dung_tot_voi_em_official_visualizer_album_cam_on",
        imageUrl = "https://i.ytimg.com/vi/mP5ksS_tpJA/maxresdefault.jpg"
    ),
    Song(
        title = "Mẹ Em Nhắc Anh",
        artist = "Orange x Hamlet Trương",
        resId = "orange_x_hamlet_truong_me_em_nhac_anh_official_mv",
        imageUrl = "https://i.ytimg.com/vi/tTUvyUUBe8M/maxresdefault.jpg"
    ),
    Song(
        title = "Khi Em Lớn (OST Bộ Tứ Báo Thủ)",
        artist = "Orange",
        resId = "orange_ost_bo_tu_bao_thu_dao_dien_tran_thanh",
        imageUrl = "https://i.ytimg.com/vi/x1rPpcXZicI/maxresdefault.jpg"
    ),
    Song(
        title = "Chạy Ngay Đi (Run Now)",
        artist = "Sơn Tùng M-TP",
        resId = "run_now_son_tung_m_tp_official_music_video",
        imageUrl = "https://i.ytimg.com/vi/bczm94CcL_A/maxresdefault.jpg"
    ),
    Song(
        title = "Thị Mầu",
        artist = "Hòa Minzy",
        resId = "roi_bo_hoa_minzy_official_lyrics_video",
        imageUrl = "https://i.ytimg.com/vi/RHE-RxPKkkw/maxresdefault.jpg"
    ),
    Song(
        title = "Yêu Thương Ngày Đó",
        artist = "Soobin Hoàng Sơn",
        resId = "soobin_hoang_son",
        imageUrl = "https://i.ytimg.com/vi/zOFLqnCMVZo/maxresdefault.jpg"
    ),
    Song(
        title = "Có Chắc Yêu Là Đây",
        artist = "Sơn Tùng M-TP",
        resId = "son_tung_m_tp_co_chac_yeu_la_day_official_music_video",
        imageUrl = "https://i.ytimg.com/vi/knW7-x7Y7RE/maxresdefault.jpg"
    ),
    Song(
        title = "Ta Còn Yêu Nhau",
        artist = "Đức Phúc",
        resId = "ta_con_yeu_nhau_official_mv_story_duc_phuc",
        imageUrl = "https://i.ytimg.com/vi/Ff6BHbMwdfw/maxresdefault.jpg"
    ),
    Song(
        title = "Thư Chưa Gửi Anh",
        artist = "Hòa Minzy",
        resId = "thu_chua_gui_anh_official_mv_hoa_minzy",
        imageUrl = "https://i.ytimg.com/vi/K9tOjVBMRts/maxresdefault.jpg"
    ),
    Song(
        title = "Yêu Được Không",
        artist = "Đức Phúc x ViruSs",
        resId = "yeu_duoc_khong_duc_phuc_x_viruss_official_music_video",
        imageUrl = "https://i.ytimg.com/vi/NvqfKytRSBA/maxresdefault.jpg"
    ),
    Song(
        title = "Chạy Về Khóc Với Anh",
        artist = "ERIK",
        resId = "yeu_duong_kho_qua_thi_chay_ve_khoc_voi_anh_official_music_video_genshin_impact",
        imageUrl = "https://i.ytimg.com/vi/h0WG0oB6rU0/maxresdefault.jpg"
    ),
    Song(
        title = "Về Bên Anh",
        artist = "Jack (G5R)",
        resId = "official_mv_ve_ben_anh_jack_g5r",
        imageUrl = "https://i.ytimg.com/vi/Q28O3_54VNo/maxresdefault.jpg"
    ),
    Song(
        title = "Ánh Nắng Của Anh",
        artist = "Đức Phúc",
        resId = "anh_nang_cua_anh_ost_cho_em_den_ngay_mai_duc_phuc_official_mv_nhac_tre_hay_moi_nhat",
        imageUrl = "https://i.ytimg.com/vi/bVV1OSpS-m4/maxresdefault.jpg"
    ),
    Song(
        title = "Âm Thầm Bên Em",
        artist = "Sơn Tùng M-TP",
        resId = "am_tham_ben_em",
        imageUrl = "https://i.ytimg.com/vi/Q0vNZVqh7w8/maxresdefault.jpg"
    ),
    Song(
        title = "Đau Nhất Là Lặng Im",
        artist = "ERIK",
        resId = "dau_nhat_la_lang_im_official_music_video",
        imageUrl = "https://i.ytimg.com/vi/dZAJYUzHPYE/maxresdefault.jpg"
    ),
    Song(
        title = "Đông Kiếm Em",
        artist = "Vũ.",
        resId = "dong_kiem_em_vu_original",
        imageUrl = "https://i.ytimg.com/vi/dDxWOvbdnYY/maxresdefault.jpg"
    )
)