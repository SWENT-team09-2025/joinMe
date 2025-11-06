package com.android.joinme.model.chat

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory implementation of [ChatRepository] for offline mode or testing.
 *
 * This implementation stores messages in a mutable list and provides real-time updates through
 * StateFlow. Useful for testing without Firebase dependencies.
 */
class ChatRepositoryLocal : ChatRepository {
  private val messages: MutableList<Message> = mutableListOf()
  private val messagesFlow = MutableStateFlow<List<Message>>(emptyList())
  private var counter = 0

  override fun getNewMessageId(): String {
    return (counter++).toString()
  }

  override fun observeMessages(chatId: String): Flow<List<Message>> {
    return messagesFlow.map { allMessages ->
      allMessages.filter { it.chatId == chatId }.sortedBy { it.timestamp }
    }
  }

  override suspend fun getMessage(messageId: String): Message {
    return messages.find { it.id == messageId }
        ?: throw Exception("ChatRepositoryLocal: Message not found")
  }

  override suspend fun addMessage(message: Message) {
    messages.add(message)
    messagesFlow.value = messages.toList()
  }

  override suspend fun editMessage(messageId: String, newValue: Message) {
    val index = messages.indexOfFirst { it.id == messageId }
    if (index != -1) {
      messages[index] = newValue
      messagesFlow.value = messages.toList()
    } else {
      throw Exception("ChatRepositoryLocal: Message not found")
    }
  }

  override suspend fun deleteMessage(messageId: String) {
    val index = messages.indexOfFirst { it.id == messageId }
    if (index != -1) {
      messages.removeAt(index)
      messagesFlow.value = messages.toList()
    } else {
      throw Exception("ChatRepositoryLocal: Message not found")
    }
  }

  override suspend fun markMessageAsRead(messageId: String, userId: String) {
    val index = messages.indexOfFirst { it.id == messageId }
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
