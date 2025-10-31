package com.example.melodyplayer.chatbot

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import android.util.Log

@Serializable
data class GeminiRequest(val contents: List<Content>)

@Serializable
data class Content(
    val role: String = "user",           // ✅ thêm role để API hiểu người gửi
    val parts: List<Part>
)

@Serializable
data class Part(val text: String)

@Serializable
data class GeminiResponse(val candidates: List<Candidate> = emptyList())

@Serializable
data class Candidate(val content: Content)

object GeminiApi {
    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = false
                isLenient = true
            })
        }
    }

    suspend fun sendMessage(apiKey: String, userMsg: String): String {
        val request = GeminiRequest(
            contents = listOf(Content(role = "user", parts = listOf(Part(text = userMsg))))
        )

        return try {
            val response: GeminiResponse = client.post(
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"
            ) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()


            Log.d("GeminiAPI", "✅ Response: $response")

            response.candidates.firstOrNull()
                ?.content?.parts?.firstOrNull()?.text
                ?: "(Không có phản hồi)"
        } catch (e: Exception) {
            Log.e("GeminiAPI", "❌ Lỗi gọi API: ${e.message}")
            "(Lỗi: ${e.message})"
        }
    }
}
