package com.android.joinme.model.chat

// Implemented with help of Claude AI

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * In-memory implementation of [ChatRepository] for offline mode or testing.
 *
 * This implementation stores messages in a mutable list. Useful for testing without Firebase
 * dependencies.
 *
 * Thread-safe: Uses Mutex to prevent race conditions when multiple coroutines access the messages
 * list concurrently.
 */
class ChatRepositoryLocal : ChatRepository {
  private val messages: MutableList<Message> = mutableListOf()
  private val mutex = Mutex()
  private var counter = 0

  override fun getNewMessageId(): String {
    return (counter++).toString()
  }

  override suspend fun getMessage(messageId: String): Message {
    return mutex.withLock {
      messages.find { it.id == messageId }
          ?: throw Exception("ChatRepositoryLocal: Message not found")
    }
  }

  override suspend fun addMessage(message: Message) {
    mutex.withLock { messages.add(message) }
  }

  override suspend fun editMessage(messageId: String, newValue: Message) {
    mutex.withLock {
      val index = messages.indexOfFirst { it.id == messageId }
      if (index != -1) {
        messages[index] = newValue
      } else {
        throw Exception("ChatRepositoryLocal: Message not found")
      }
    }
  }

  override suspend fun deleteMessage(messageId: String) {
    mutex.withLock {
      val index = messages.indexOfFirst { it.id == messageId }
      if (index != -1) {
        messages.removeAt(index)
      } else {
        throw Exception("ChatRepositoryLocal: Message not found")
      }
    }
  }

  override suspend fun markMessageAsRead(messageId: String, userId: String) {
    mutex.withLock {
      val index = messages.indexOfFirst { it.id == messageId }
      if (index != -1) {
        val message = messages[index]
        if (userId !in message.readBy) {
          messages[index] = message.copy(readBy = message.readBy + userId)
        }
      } else {
        throw Exception("ChatRepositoryLocal: Message not found")
      }
    }
  }
}
