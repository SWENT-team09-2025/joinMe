package com.android.joinme.model.chat

enum class MessageType {
  TEXT,
  SYSTEM, // For "User joined," "User left," etc.
  // IMAGE, Implement this later if possible
  // VIDEO, Implement this later if possible
  // AUDIO, Implement this later if possible
}

data class Message(
    val id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val content: String = "",
    val timestamp: Long = 0L,
    val type: MessageType = MessageType.TEXT,
    val readBy: List<String> = emptyList(),
    val isPinned: Boolean = false
)
