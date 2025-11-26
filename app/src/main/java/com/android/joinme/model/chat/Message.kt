package com.android.joinme.model.chat

// Implemented with help of Claude AI

/** Enum representing the type of message. */
enum class MessageType {
  /** Standard text message */
  TEXT,
  /** System-generated message (e.g., "User joined," "User left") */
  SYSTEM,
  /** Image message */
  IMAGE
  // VIDEO, Implement this later if possible
  // AUDIO, Implement this later if possible
}

/**
 * Represents a chat message in a conversation.
 *
 * Messages are stored as subcollections under conversations in Firestore:
 * `conversations/{conversationId}/messages/{messageId}`
 *
 * @property id Unique identifier for the message
 * @property conversationId ID of the conversation this message belongs to
 * @property senderId ID of the user who sent the message
 * @property senderName Display name of the sender
 * @property content Text content of the message
 * @property timestamp Unix timestamp (milliseconds) when the message was created
 * @property type Type of message (TEXT, SYSTEM, IMAGE)
 * @property readBy List of user IDs who have read this message
 * @property isPinned Whether this message is pinned in the conversation
 * @property isEdited Whether this message has been edited
 */
data class Message(
    val id: String = "",
    val conversationId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val content: String = "",
    val timestamp: Long = 0L,
    val type: MessageType = MessageType.TEXT,
    val readBy: List<String> = emptyList(),
    val isPinned: Boolean = false,
    val isEdited: Boolean = false
)
