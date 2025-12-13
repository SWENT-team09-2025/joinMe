package com.android.joinme.model.chat

// Implemented with help of Claude AI

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Exception message for when a message is not found in the local repository. */
const val MESSAGE_NOT_FOUND = "ChatRepositoryLocal: Message not found"

/**
 * In-memory implementation of [ChatRepository] for offline mode or testing.
 *
 * This implementation stores messages in a mutable list and provides real-time updates through
 * StateFlow. Useful for testing without Firebase dependencies.
 *
 * Thread-safe: Uses Mutex to prevent race conditions when multiple coroutines access the messages
 * list concurrently.
 */
class ChatRepositoryLocal : ChatRepository {
  private val messages: MutableList<Message> = mutableListOf()
  private val messagesFlow = MutableStateFlow<List<Message>>(emptyList())
  private val mutex = Mutex()
  private var counter = 0

  override fun getNewMessageId(): String {
    return (counter++).toString()
  }

  override fun observeMessagesForConversation(conversationId: String): Flow<List<Message>> {
    return messagesFlow.map { allMessages ->
      allMessages.filter { it.conversationId == conversationId }.sortedBy { it.timestamp }
    }
  }

  override suspend fun addMessage(message: Message) {
    mutex.withLock {
      messages.add(message)
      messagesFlow.value = messages.toList()
    }
  }

  override suspend fun editMessage(conversationId: String, messageId: String, newValue: Message) {
    mutex.withLock {
      val index =
          messages.indexOfFirst { it.id == messageId && it.conversationId == conversationId }
      if (index != -1) {
        messages[index] = newValue
        messagesFlow.value = messages.toList()
      } else {
        throw Exception(MESSAGE_NOT_FOUND)
      }
    }
  }

  override suspend fun deleteMessage(conversationId: String, messageId: String) {
    mutex.withLock {
      val index =
          messages.indexOfFirst { it.id == messageId && it.conversationId == conversationId }
      if (index != -1) {
        messages.removeAt(index)
        messagesFlow.value = messages.toList()
      } else {
        throw Exception(MESSAGE_NOT_FOUND)
      }
    }
  }

  override suspend fun markMessageAsRead(
      conversationId: String,
      messageId: String,
      userId: String
  ) {
    mutex.withLock {
      val index =
          messages.indexOfFirst { it.id == messageId && it.conversationId == conversationId }
      if (index != -1) {
        val message = messages[index]
        if (userId !in message.readBy) {
          messages[index] = message.copy(readBy = message.readBy + userId)
          messagesFlow.value = messages.toList()
        }
      } else {
        throw Exception(MESSAGE_NOT_FOUND)
      }
    }
  }

  override suspend fun uploadChatImage(
      context: Context,
      conversationId: String,
      messageId: String,
      imageUri: Uri
  ): String {
    // For local/testing implementation, return a mock URL
    return "mock://chat-image/$conversationId/$messageId.jpg"
  }

  override suspend fun deleteConversation(conversationId: String, pollRepository: PollRepository?) {
    mutex.withLock {
      // Remove all messages for this conversation
      messages.removeAll { it.conversationId == conversationId }
      messagesFlow.value = messages.toList()
    }
  }

  override suspend fun deleteAllUserConversations(userId: String) {
    mutex.withLock {
      // Find all DM conversations involving this user
      // DM conversation IDs follow the pattern: dm_{userId1}_{userId2}
      val conversationsToDelete =
          messages
              .map { it.conversationId }
              .distinct()
              .filter { conversationId ->
                if (conversationId.startsWith("dm_")) {
                  val parts = conversationId.split("_")
                  parts.size == 3 && (parts[1] == userId || parts[2] == userId)
                } else {
                  false
                }
              }

      // Remove all messages from those conversations
      messages.removeAll { conversationsToDelete.contains(it.conversationId) }
      messagesFlow.value = messages.toList()
    }
  }
}
