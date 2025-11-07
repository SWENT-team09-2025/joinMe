package com.android.joinme.model.chat

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

const val MESSAGES_COLLECTION_PATH = "messages"

/**
 * Firestore-backed implementation of [ChatRepository]. Manages CRUD operations for [Message]
 * objects using Firebase Firestore.
 */
class ChatRepositoryFirestore(private val db: FirebaseFirestore) : ChatRepository {

  override fun getNewMessageId(): String {
    return db.collection(MESSAGES_COLLECTION_PATH).document().id
  }

  override fun observeMessages(chatId: String): Flow<List<Message>> = callbackFlow {
    val listener: ListenerRegistration =
        db.collection(MESSAGES_COLLECTION_PATH)
            .whereEqualTo("chatId", chatId)
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, error ->
              if (error != null) {
                Log.e("ChatRepositoryFirestore", "Error observing messages", error)
                close(error)
                return@addSnapshotListener
              }

              if (snapshot != null) {
                val messages = snapshot.documents.mapNotNull { documentToMessage(it) }
                trySend(messages)
              }
            }

    awaitClose { listener.remove() }
  }

  override suspend fun getMessage(messageId: String): Message {
    val document = db.collection(MESSAGES_COLLECTION_PATH).document(messageId).get().await()
    return documentToMessage(document)
        ?: throw Exception("ChatRepositoryFirestore: Message not found ($messageId)")
  }

  override suspend fun addMessage(message: Message) {
    db.collection(MESSAGES_COLLECTION_PATH).document(message.id).set(message).await()
  }

  override suspend fun editMessage(messageId: String, newValue: Message) {
    db.collection(MESSAGES_COLLECTION_PATH).document(messageId).set(newValue).await()
  }

  override suspend fun deleteMessage(messageId: String) {
    db.collection(MESSAGES_COLLECTION_PATH).document(messageId).delete().await()
  }

  override suspend fun markMessageAsRead(messageId: String, userId: String) {
    val document = db.collection(MESSAGES_COLLECTION_PATH).document(messageId).get().await()
    val message =
        documentToMessage(document)
            ?: throw Exception("ChatRepositoryFirestore: Message not found ($messageId)")

    // Add userId to readBy list if not already present
    if (userId !in message.readBy) {
      val updatedReadBy = message.readBy + userId
      db.collection(MESSAGES_COLLECTION_PATH)
          .document(messageId)
          .update("readBy", updatedReadBy)
          .await()
    }
  }

  /**
   * Converts a Firestore document snapshot to a [Message] object.
   *
   * @param document The Firestore document to convert.
   * @return The [Message] object, or null if conversion fails.
   */
  private fun documentToMessage(
      document: com.google.firebase.firestore.DocumentSnapshot
  ): Message? {
    return try {
      val id = document.id
      val chatId = document.getString("chatId") ?: return null
      val senderId = document.getString("senderId") ?: return null
      val senderName = document.getString("senderName") ?: return null
      val content = document.getString("content") ?: return null
      val timestamp = document.getLong("timestamp") ?: return null
      val typeString = document.getString("type") ?: "TEXT"
      val type =
          try {
            MessageType.valueOf(typeString)
          } catch (_: IllegalArgumentException) {
            MessageType.TEXT
          }
      val readBy = document.get("readBy") as? List<String> ?: emptyList()
      val isPinned = document.getBoolean("isPinned") ?: false

      Message(
          id = id,
          chatId = chatId,
          senderId = senderId,
          senderName = senderName,
          content = content,
          timestamp = timestamp,
          type = type,
          readBy = readBy,
          isPinned = isPinned)
    } catch (e: Exception) {
      Log.e("ChatRepositoryFirestore", "Error converting document to Message", e)
      null
    }
  }
}
