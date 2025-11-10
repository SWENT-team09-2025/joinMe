package com.android.joinme.model.chat

// Implemented with help of Claude AI

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

const val CONVERSATIONS_COLLECTION_PATH = "conversations"
const val MESSAGES_SUBCOLLECTION_PATH = "messages"

/**
 * Firestore-backed implementation of [ChatRepository]. Manages CRUD operations for [Message]
 * objects using Firebase Firestore.
 *
 * Messages are stored as subcollections under conversations:
 * `conversations/{conversationId}/messages/{messageId}`
 */
class ChatRepositoryFirestore(private val db: FirebaseFirestore) : ChatRepository {

  override fun getNewMessageId(): String {
    // Generate a unique ID (can be used for any conversation)
    return db.collection("temp").document().id
  }

  override fun observeMessagesForConversation(conversationId: String): Flow<List<Message>> =
      callbackFlow {
        val listenerRegistration =
            db.collection(CONVERSATIONS_COLLECTION_PATH)
                .document(conversationId)
                .collection(MESSAGES_SUBCOLLECTION_PATH)
                .orderBy("timestamp")
                .addSnapshotListener { snapshot, error ->
                  if (error != null) {
                    Log.e("ChatRepositoryFirestore", "Error observing messages", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                  }

                  if (snapshot != null) {
                    val messages =
                        snapshot.documents.mapNotNull { document ->
                          documentToMessage(document, conversationId)
                        }
                    trySend(messages)
                  }
                }

        awaitClose { listenerRegistration.remove() }
      }

  override suspend fun addMessage(message: Message) {
    db.collection(CONVERSATIONS_COLLECTION_PATH)
        .document(message.conversationId)
        .collection(MESSAGES_SUBCOLLECTION_PATH)
        .document(message.id)
        .set(message)
        .await()
  }

  override suspend fun editMessage(conversationId: String, messageId: String, newValue: Message) {
    db.collection(CONVERSATIONS_COLLECTION_PATH)
        .document(conversationId)
        .collection(MESSAGES_SUBCOLLECTION_PATH)
        .document(messageId)
        // Use merge to avoid overwriting fields not included in newValue
        .set(newValue, SetOptions.merge())
        .await()
  }

  override suspend fun deleteMessage(conversationId: String, messageId: String) {
    db.collection(CONVERSATIONS_COLLECTION_PATH)
        .document(conversationId)
        .collection(MESSAGES_SUBCOLLECTION_PATH)
        .document(messageId)
        .delete()
        .await()
  }

  override suspend fun markMessageAsRead(
      conversationId: String,
      messageId: String,
      userId: String
  ) {
    db.collection(CONVERSATIONS_COLLECTION_PATH)
        .document(conversationId)
        .collection(MESSAGES_SUBCOLLECTION_PATH)
        .document(messageId)
        .update("readBy", FieldValue.arrayUnion(userId))
        .await()
  }

  /**
   * Converts a Firestore document snapshot to a [Message] object.
   *
   * Supports both Long timestamps and Firestore Timestamp objects.
   *
   * @param document The Firestore document to convert.
   * @param conversationId The conversation ID this message belongs to.
   * @return The [Message] object, or null if conversion fails.
   */
  private fun documentToMessage(
      document: com.google.firebase.firestore.DocumentSnapshot,
      conversationId: String
  ): Message? {
    return try {
      val id = document.id
      val senderId = document.getString("senderId") ?: return null
      val senderName = document.getString("senderName") ?: return null
      val content = document.getString("content") ?: return null

      val timestamp =
          document.getLong("timestamp")
              ?: document.getTimestamp("timestamp")?.toDate()?.time
              ?: return null

      val typeString = document.getString("type") ?: "TEXT"
      val type =
          try {
            MessageType.valueOf(typeString)
          } catch (_: IllegalArgumentException) {
            MessageType.TEXT
          }
      @Suppress("UNCHECKED_CAST")
      val readBy = document.get("readBy") as? List<String> ?: emptyList()
      val isPinned = document.getBoolean("isPinned") ?: false

      Message(
          id = id,
          conversationId = conversationId,
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
