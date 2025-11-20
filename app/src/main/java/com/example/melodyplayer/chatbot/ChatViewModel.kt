//
//package com.example.melodyplayer.chatbot
//
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//import com.example.melodyplayer.data.SongRepository
//import com.example.melodyplayer.model.Song
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.StateFlow
//import kotlinx.coroutines.flow.asStateFlow
//import kotlinx.coroutines.flow.update
//import kotlinx.coroutines.launch
//
//data class ChatMessage(
//    val text: String,
//    val isUser: Boolean
//)
//
//class ChatViewModel(
//    private val songRepository: SongRepository = SongRepository(),
//    private val geminiSendMessage: suspend (String, String) -> String = GeminiApi::sendMessage,
//    private val useGeminiForSearchSummary: Boolean = true
//) : ViewModel() {
//
//    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
//    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()
//
//    fun sendMessage(apiKey: String, message: String) {
//        val trimmedMessage = message.trim()
//        if (trimmedMessage.isEmpty()) return
//
//        appendMessage(ChatMessage(trimmedMessage, isUser = true))
//
//        val keyword = extractSearchKeyword(trimmedMessage)
//        val containsSearchKeyword = SEARCH_PATTERN.containsMatchIn(trimmedMessage)
//
//        when {
//            containsSearchKeyword && keyword.isNotBlank() -> {
//                viewModelScope.launch {
//                    handleSongSearch(apiKey = apiKey, keyword = keyword)
//                }
//            }
//            containsSearchKeyword -> {
//                appendMessage(
//                    ChatMessage(
//                        text = "Mình cần biết cụ thể hơn bạn muốn tìm bài hát nào nhé!",
//                        isUser = false
//                    )
//                )
//            }
//            else -> {
//                viewModelScope.launch {
//                    handleGeneralMessage(apiKey = apiKey, prompt = trimmedMessage)
//                }
//            }
//        }
//    }
//
//    private suspend fun handleSongSearch(apiKey: String, keyword: String) {
//        val results = songRepository.searchSongs(keyword)
//        val formattedResponse = formatSearchResponse(keyword, results)
//        appendMessage(ChatMessage(formattedResponse, isUser = false))
//
//        if (!useGeminiForSearchSummary || results.isEmpty() || apiKey.isBlank()) {
//            return
//        }
//
//        val prompt = buildGeminiPrompt(keyword, results)
//        val response = runCatching { geminiSendMessage(apiKey, prompt) }
//            .getOrElse { error -> "(Lỗi: ${'$'}{error.message})" }
//
//        appendMessage(ChatMessage(response, isUser = false))
//    }
//
//    private suspend fun handleGeneralMessage(apiKey: String, prompt: String) {
//        if (apiKey.isBlank()) {
//            appendMessage(
//                ChatMessage(
//                    text = "Vui lòng cấu hình API key để trò chuyện với trợ lý nhé!",
//                    isUser = false
//                )
//            )
//            return
//        }
//
//        val response = runCatching { geminiSendMessage(apiKey, prompt) }
//            .getOrElse { error -> "(Lỗi: ${'$'}{error.message})" }
//
//        appendMessage(ChatMessage(response, isUser = false))
//    }
//
//    private fun formatSearchResponse(keyword: String, songs: List<Song>): String {
//        if (songs.isEmpty()) {
//            return "Mình chưa tìm thấy bài hát nào phù hợp với \"${'$'}keyword\". Bạn thử dùng từ khóa khác nhé!"
//        }
//
//        return buildString {
//            appendLine("Mình tìm được ${'$'}{songs.size} bài hát phù hợp với \"${'$'}keyword\":")
//            songs.forEachIndexed { index, song ->
//                val link = song.displayableLink()
//                if (link != null) {
//                    appendLine("${'$'}{index + 1}. ${'$'}{song.title} – ${'$'}{song.artist} (${link})")
//                } else {
//                    appendLine("${'$'}{index + 1}. ${'$'}{song.title} – ${'$'}{song.artist}")
//                }
//            }
//        }.trim()
//    }
//
//    private fun buildGeminiPrompt(keyword: String, songs: List<Song>): String {
//        return buildString {
//            appendLine("Người dùng đang tìm kiếm bài hát với từ khóa \"${'$'}keyword\".")
//            appendLine("Danh sách gợi ý:")
//            songs.forEachIndexed { index, song ->
//                val link = song.displayableLink()
//                if (link != null) {
//                    appendLine("${'$'}{index + 1}. ${'$'}{song.title} – ${'$'}{song.artist} – ${link}")
//                } else {
//                    appendLine("${'$'}{index + 1}. ${'$'}{song.title} – ${'$'}{song.artist}")
//                }
//            }
//            appendLine("Hãy viết câu trả lời tự nhiên, thân thiện bằng tiếng Việt để giới thiệu các bài hát trên.")
//        }.trim()
//    }
//
//    private fun appendMessage(message: ChatMessage) {
//        _messages.update { current -> current + message }
//    }
//
//    private fun extractSearchKeyword(message: String): String {
//        val cleaned = SEARCH_PATTERN.replace(message, " ")
//        return cleaned.replace(WHITESPACE_PATTERN, " ").trim()
//    }
//
//    private fun Song.displayableLink(): String? {
//        return audioUrl ?: imageUrl ?: resId
//    }
//
//    companion object {
//        private val SEARCH_PATTERN = Regex(
//            pattern = "(tìm|tim|search)\\s*((bài|bai)\\s*hát)?",
//            option = RegexOption.IGNORE_CASE
//        )
//        private val WHITESPACE_PATTERN = "\\s+".toRegex()
//    }
//}