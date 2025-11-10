package com.android.joinme.model.chat

// Implemented with help of Claude AI

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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
        throw Exception("ChatRepositoryLocal: Message not found")
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
        throw Exception("ChatRepositoryLocal: Message not found")
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
        throw Exception("ChatRepositoryLocal: Message not found")
      }
    }
  }
}
