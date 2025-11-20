package com.example.melodyplayer.search

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.sin

@Composable
fun VoiceSearchDialog(
    onDismiss: () -> Unit,
    onResult: (String) -> Unit
) {
    val context = LocalContext.current
    var isListening by remember { mutableStateOf(false) }
    var transcribedText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var hasPermission by remember { mutableStateOf(false) }

    // Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        if (!isGranted) {
            errorMessage = "Cần cấp quyền ghi âm"
        } else {
            errorMessage = null
        }
    }

    // Speech Recognizer
    val voiceRecognizer = remember {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            SpeechRecognizer.createSpeechRecognizer(context)
        } else null
    }

    fun startListening() {
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }

        if (voiceRecognizer == null) {
            errorMessage = "Thiết bị không hỗ trợ"
            return
        }

        transcribedText = ""
        errorMessage = null

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "vi-VN")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
        }

        voiceRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
                errorMessage = null
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { isListening = false }
            override fun onError(error: Int) {
                isListening = false
                errorMessage = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "Không nghe rõ, thử lại"
                    SpeechRecognizer.ERROR_NETWORK -> "Lỗi mạng"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Không nghe thấy"
                    else -> "Lỗi không xác định"
                }
            }
            override fun onResults(results: Bundle?) {
                isListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    transcribedText = matches[0]
                    // Auto close after 500ms
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        onResult(matches[0])
                    }, 500)
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    transcribedText = matches[0]
                }
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        try {
            voiceRecognizer.startListening(intent)
        } catch (e: Exception) {
            isListening = false
            errorMessage = "Lỗi: ${e.message}"
        }
    }

    fun stopListening() {
        voiceRecognizer?.stopListening()
        isListening = false
    }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(300)
        startListening()
    }

    DisposableEffect(Unit) {
        onDispose {
            voiceRecognizer?.destroy()
        }
    }

    Dialog(
        onDismissRequest = {
            stopListening()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1a1a1a)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 24.dp)
            ) {
                Box {
                    // Gradient Background
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp)
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0xFF1DB954).copy(alpha = 0.15f),
                                        Color(0xFF1a1a1a),
                                        Color(0xFF0D0D0D)
                                    )
                                )
                            )
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Close Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            IconButton(
                                onClick = {
                                    stopListening()
                                    onDismiss()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Modern Animated Mic
                        ModernMicIcon(isListening = isListening)

                        Spacer(modifier = Modifier.height(32.dp))

                        // Status Text
                        Text(
                            text = when {
                                errorMessage != null -> "Lỗi"
                                isListening -> "Đang nghe..."
                                transcribedText.isNotEmpty() -> "Hoàn tất!"
                                else -> "Chạm để nói"
                            },
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Transcribed Text or Instruction
                        if (transcribedText.isNotEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF1DB954).copy(alpha = 0.15f)
                                )
                            ) {
                                Text(
                                    text = "\"$transcribedText\"",
                                    fontSize = 16.sp,
                                    color = Color(0xFF1DB954),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        } else if (errorMessage != null) {
                            Text(
                                text = errorMessage!!,
                                fontSize = 14.sp,
                                color = Color(0xFFFF6B6B),
                                textAlign = TextAlign.Center
                            )
                        } else {
                            Text(
                                text = "Nói tên bài hát hoặc ca sĩ",
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.5f),
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // Control Button
                        FloatingActionButton(
                            onClick = {
                                if (isListening) {
                                    stopListening()
                                } else {
                                    startListening()
                                }
                            },
                            containerColor = if (isListening) Color(0xFFFF4444) else Color(0xFF1DB954),
                            contentColor = Color.White,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Icon(
                                if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                                contentDescription = if (isListening) "Stop" else "Start",
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ModernMicIcon(isListening: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "mic")

    // Wave animation for listening state
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave"
    )

    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(140.dp)
    ) {
        // Animated waves when listening
        if (isListening) {
            repeat(3) { index ->
                Canvas(
                    modifier = Modifier
                        .size(140.dp - (index * 20).dp)
                        .scale(if (isListening) scale else 1f)
                ) {
                    val radius = size.minDimension / 2
                    val path = Path()

                    for (angle in 0..360 step 5) {
                        val rad = Math.toRadians(angle.toDouble())
                        val wave = sin((angle + wavePhase + (index * 60)) * Math.PI / 180) * 8
                        val r = radius + wave.toFloat()
                        val x = center.x + (r * kotlin.math.cos(rad)).toFloat()
                        val y = center.y + (r * kotlin.math.sin(rad)).toFloat()

                        if (angle == 0) path.moveTo(x, y)
                        else path.lineTo(x, y)
                    }
                    path.close()

                    drawPath(
                        path = path,
                        color = Color(0xFF1DB954).copy(alpha = 0.3f - (index * 0.1f)),
                        style = Stroke(width = 2f)
                    )
                }
            }
        }

        // Main mic circle
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(
                    if (isListening)
                        Brush.radialGradient(
                            listOf(
                                Color(0xFF1DB954),
                                Color(0xFF1DB954).copy(alpha = 0.8f)
                            )
                        )
                    else
                        Brush.radialGradient(
                            listOf(
                                Color(0xFF2a2a2a),
                                Color(0xFF1a1a1a)
                            )
                        )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Mic,
                contentDescription = "Microphone",
                tint = Color.White,
                modifier = Modifier
                    .size(48.dp)
                    .scale(if (isListening) scale else 1f)
            )
        }

        // Outer glow ring
        if (isListening) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(
                        Color(0xFF1DB954).copy(alpha = 0.2f)
                    )
                    .blur(8.dp)
            )
        }
    }
}