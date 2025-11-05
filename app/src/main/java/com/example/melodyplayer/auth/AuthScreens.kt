package com.example.melodyplayer.auth

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import kotlin.random.Random

@Composable
fun AuthScreen(
    onLoginSuccess: () -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current
    val sharedPrefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    var email by remember { mutableStateOf(sharedPrefs.getString("saved_email", "") ?: "") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var captchaInput by remember { mutableStateOf("") }
    var captchaCode by remember { mutableStateOf(generateCaptcha()) }

    var isLogin by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var rememberLogin by remember { mutableStateOf(sharedPrefs.getBoolean("remember_login", false)) }

    LaunchedEffect(Unit) {
        if (rememberLogin && auth.currentUser != null) {
            onLoginSuccess()
        }
    }

    // 🌌 Nền neon tím - hồng - xanh
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A0014),
                        Color(0xFF160028),
                        Color(0xFF22003E)
                    )
                )
            )
    ) {
        // Glow hồng
        Box(
            modifier = Modifier
                .size(300.dp)
                .offset(x = (-120).dp, y = 100.dp)
                .clip(CircleShape)
                .background(Color(0xFFFF00FF).copy(alpha = 0.15f))
                .blur(100.dp)
        )

        // Glow xanh cyan
        Box(
            modifier = Modifier
                .size(280.dp)
                .offset(x = 240.dp, y = 450.dp)
                .clip(CircleShape)
                .background(Color(0xFF00FFFF).copy(alpha = 0.12f))
                .blur(120.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // 🌠 Logo neon nổi khối
            Surface(
                modifier = Modifier
                    .size(110.dp)
                    .shadow(20.dp, CircleShape),
                shape = CircleShape,
                color = Color.Transparent
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFFF00FF).copy(alpha = 0.3f),
                                    Color.Transparent
                                )
                            )
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = "App Logo",
                        modifier = Modifier.size(70.dp),
                        tint = Color(0xFF00FFFF)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Melody Player",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFFFF00FF),
                letterSpacing = 0.5.sp
            )

            Text(
                text = "Neon Sound Experience 🎶",
                fontSize = 15.sp,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Light
            )

            Spacer(modifier = Modifier.height(40.dp))

            // 💎 Card login neon
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(20.dp, RoundedCornerShape(28.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1C002F).copy(alpha = 0.85f)
                ),
                shape = RoundedCornerShape(28.dp)
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isLogin) "Đăng nhập" else "Đăng ký",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00FFFF)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    @Composable
                    fun fieldColors() = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF2A0046),
                        unfocusedContainerColor = Color(0xFF1A002E),
                        focusedIndicatorColor = Color(0xFFFF00FF),
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color(0xFF00FFFF)
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        placeholder = { Text("Email", color = Color.White.copy(alpha = 0.4f)) },
                        leadingIcon = { Icon(Icons.Default.Email, null, tint = Color(0xFF00FFFF)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = fieldColors()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = { Text("Mật khẩu", color = Color.White.copy(alpha = 0.4f)) },
                        leadingIcon = { Icon(Icons.Default.Lock, null, tint = Color(0xFFFF00FF)) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    null,
                                    tint = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        colors = fieldColors()
                    )

                    AnimatedVisibility(visible = !isLogin) {
                        Column {
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it },
                                placeholder = { Text("Xác nhận mật khẩu", color = Color.White.copy(alpha = 0.4f)) },
                                leadingIcon = { Icon(Icons.Default.Lock, null, tint = Color(0xFF00FFFF)) },
                                trailingIcon = {
                                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                        Icon(
                                            if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                            null,
                                            tint = Color.White.copy(alpha = 0.7f)
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                colors = fieldColors()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(26.dp))

                    // 🌈 Nút neon gradient
                    Button(
                        onClick = {
                            isLoading = true
                            errorMessage = null
                            successMessage = null

                            if (isLogin) {
                                auth.signInWithEmailAndPassword(email, password)
                                    .addOnCompleteListener {
                                        isLoading = false
                                        if (it.isSuccessful) onLoginSuccess()
                                        else errorMessage = it.exception?.message
                                    }
                            } else {
                                if (password != confirmPassword) {
                                    isLoading = false
                                    errorMessage = "Mật khẩu không khớp!"
                                    return@Button
                                }
                                auth.createUserWithEmailAndPassword(email, password)
                                    .addOnCompleteListener {
                                        isLoading = false
                                        if (it.isSuccessful) {
                                            successMessage = "Đăng ký thành công!"
                                            isLogin = true
                                        } else errorMessage = it.exception?.message
                                    }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .shadow(10.dp, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xFF00FFFF),
                                            Color(0xFFFF00FF),
                                            Color(0xFF7B2FF7)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoading)
                                CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp)
                            else
                                Text(
                                    if (isLogin) "Đăng nhập" else "Đăng ký",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp
                                )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 🔁 Toggle chế độ
                    TextButton(onClick = {
                        isLogin = !isLogin
                        errorMessage = null
                        successMessage = null
                    }) {
                        Text(
                            if (isLogin) "Chưa có tài khoản? " else "Đã có tài khoản? ",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp
                        )
                        Text(
                            if (isLogin) "Đăng ký ngay" else "Đăng nhập",
                            color = Color(0xFF00FFFF),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    // ⚠️ Thông báo
                    errorMessage?.let {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(it, color = Color(0xFFFF4081), fontSize = 13.sp)
                    }

                    successMessage?.let {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(it, color = Color(0xFF00FFFF), fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

fun generateCaptcha(): String {
    val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    return (1..5).map { chars[Random.nextInt(chars.length)] }.joinToString("")
}
