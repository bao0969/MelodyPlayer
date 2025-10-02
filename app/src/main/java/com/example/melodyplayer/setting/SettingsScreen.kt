package com.example.melodyplayer.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.melodyplayer.navigation.Routes
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cài đặt", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0D0D0D)
                )
            )
        },
        containerColor = Color(0xFF0D0D0D)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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
                            subtitle = FirebaseAuth.getInstance().currentUser?.email ?: "Không xác định",
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
                        Divider(
                            color = Color.White.copy(alpha = 0.1f),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        SettingItem(
                            icon = Icons.Default.Palette,
                            title = "Giao diện",
                            subtitle = "Tùy chỉnh giao diện",
                            onClick = { }
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }

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
                            icon = Icons.Default.Info,
                            title = "Về ứng dụng",
                            subtitle = "Version 1.0.0",
                            onClick = { }
                        )
                        Divider(
                            color = Color.White.copy(alpha = 0.1f),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        SettingItem(
                            icon = Icons.Default.Help,
                            title = "Trợ giúp",
                            subtitle = "Hỗ trợ & câu hỏi",
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
                            popUpTo(0) { inclusive = true }
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
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
}

@Composable
private fun SettingItem(
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