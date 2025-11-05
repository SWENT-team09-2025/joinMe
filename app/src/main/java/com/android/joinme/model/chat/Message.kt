package com.android.joinme.model.chat

enum class MessageType {
    TEXT,
    SYSTEM, // For "User joined," "User left," etc.
    IMAGE, // Implement this later
    VIDEO, // Implement this later
    AUDIO, // Implement this later
}

data class Message(
    val id: String,
    val senderId: String,
    val senderName: String,
    val content: String,
    val timestamp: Long,
    val type: MessageType = MessageType.TEXT
)