package com.example.melodyplayer

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.lifecycle.viewmodel.compose.viewModel
//import com.example.melodyplayer.chatbot.ChatViewModel
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
// Animation & Gesture
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

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
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    Box(modifier = Modifier.fillMaxSize()) {

        // ====== LẤY KÍCH THƯỚC MÀN HÌNH ======
        val configuration = LocalConfiguration.current
        val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
        val bubbleSize = with(density) { 60.dp.toPx() }
        val targetX = screenWidthPx - bubbleSize - with(density) { 16.dp.toPx() }
        val targetY = with(density) { 48.dp.toPx() }

        // ====== ANIMATION ======
        LaunchedEffect(expanded) {
            if (expanded) {
                coroutineScope.launch {
                    offsetX.animateTo(
                        targetX,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
                }
                coroutineScope.launch {
                    offsetY.animateTo(
                        targetY,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
                }
            }
        }

        // ====== BẢNG CHAT ======
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            // --- State chat ---
            data class Message(val text: String, val isUser: Boolean)

            val coroutine = rememberCoroutineScope()
            var messages by remember { mutableStateOf(listOf<Message>()) }
            var input by remember { mutableStateOf("") }
            var isLoading by remember { mutableStateOf(false) }

            // 🔑 Dán API key Gemini của bạn vào đây
            val apiKey = "AIzaSyA8BXqqIqTbyEJuhM2LgxrAtjw02rkr6tU"

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .background(
                        color = Color(0xFF0A1F1A),
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    )
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // ----- Header -----
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0D2D20))
                            .padding(vertical = 20.dp, horizontal = 24.dp)
                    ) {
                        Column {
                            Text(
                                "Chat với AI Assistant",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Hỏi tôi bất cứ điều gì",
                                color = Color(0xFF9DB6A8),
                                fontSize = 13.sp
                            )
                        }
                    }

                    // ----- Danh sách tin nhắn -----
                    val scrollState = rememberScrollState()

                    LaunchedEffect(messages.size) {
                        scrollState.animateScrollTo(scrollState.maxValue)
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(scrollState)
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        if (messages.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        "👋",
                                        fontSize = 48.sp
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        "Xin chào! Tôi có thể giúp gì cho bạn?",
                                        color = Color(0xFF9DB6A8),
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }

                        messages.forEach { message ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
                            ) {
                                if (!message.isUser) {
                                    // Avatar bot
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF1DB954)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("🤖", fontSize = 16.sp)
                                    }
                                    Spacer(Modifier.width(8.dp))
                                }

                                // Khung tin nhắn
                                Box(
                                    modifier = Modifier
                                        .widthIn(max = 260.dp)
                                        .background(
                                            color = if (message.isUser) Color(0xFF1DB954) else Color(0xFF1A3D2E),
                                            shape = RoundedCornerShape(
                                                topStart = 18.dp,
                                                topEnd = 18.dp,
                                                bottomStart = if (message.isUser) 18.dp else 4.dp,
                                                bottomEnd = if (message.isUser) 4.dp else 18.dp
                                            )
                                        )
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = message.text,
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        lineHeight = 20.sp
                                    )
                                }

                                if (message.isUser) {
                                    Spacer(Modifier.width(8.dp))
                                    // Avatar user
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF007AFF)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("👤", fontSize = 16.sp)
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }

                        // Loading indicator
                        if (isLoading) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF1DB954)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🤖", fontSize = 16.sp)
                                }
                                Spacer(Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = Color(0xFF1A3D2E),
                                            shape = RoundedCornerShape(18.dp)
                                        )
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    Text("Đang suy nghĩ...", color = Color(0xFF9DB6A8), fontSize = 15.sp)
                                }
                            }
                        }
                    }

                    // ----- Ô nhập và nút gửi -----
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0D2D20))
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextField(
                                value = input,
                                onValueChange = { input = it },
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(24.dp)),
                                placeholder = { Text("Nhập tin nhắn...", fontSize = 15.sp, color = Color(0xFF9DB6A8)) },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFF1A3D2E),
                                    unfocusedContainerColor = Color(0xFF1A3D2E),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    cursorColor = Color(0xFF1DB954)
                                ),
                                singleLine = true,
                                enabled = !isLoading
                            )
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (input.isNotBlank() && !isLoading) {
                                        val userText = input
                                        input = ""
                                        messages = messages + Message(userText, isUser = true)
                                        isLoading = true
                                        coroutine.launch {
                                            val reply = com.example.melodyplayer.chatbot.GeminiApi.sendMessage(apiKey, userText)
                                            messages = messages + Message(reply, isUser = false)
                                            isLoading = false
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1DB954),
                                    disabledContainerColor = Color(0xFF666666)
                                ),
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape),
                                contentPadding = PaddingValues(0.dp),
                                enabled = !isLoading && input.isNotBlank()
                            ) {
                                Text("➤", fontSize = 20.sp, color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // ====== BONG BÓNG ======
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt()) }
                .size(60.dp)
                .clip(CircleShape)
                .shadow(if (expanded) 8.dp else 4.dp, CircleShape)
                .background(Color(0xFF1DB954))
                .pointerInput(expanded) {
                    detectDragGestures(
                        onDragEnd = {
                            coroutineScope.launch {
                                offsetX.animateTo(
                                    offsetX.value,
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)
                                )
                                offsetY.animateTo(
                                    offsetY.value,
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)
                                )
                            }
                        }
                    ) { _, dragAmount ->
                        if (!expanded) {
                            coroutineScope.launch {
                                offsetX.snapTo(offsetX.value + dragAmount.x)
                                offsetY.snapTo(offsetY.value + dragAmount.y)
                            }
                        }
                    }
                }
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
    }
}