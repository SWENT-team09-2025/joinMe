package com.android.joinme.model.chat

// Implemented with help of Claude AI

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Firebase Realtime Database implementation of [ChatRepository].
 *
 * Messages are stored in the following structure:
 * ```
 * conversations/
 *   {conversationId}/
 *     messages/
 *       {messageId}/
 *         senderId: "..."
 *         senderName: "..."
 *         content: "..."
 *         timestamp: 123456789
 *         type: "TEXT"
 *         readBy: ["userId1", "userId2"]
 *         isPinned: false
 * ```
 *
 * This provides real-time synchronization where all clients receive updates instantly when messages
 * are added, edited, or deleted.
 */
class ChatRepositoryRealtimeDatabase(private val database: FirebaseDatabase) : ChatRepository {

  private val conversationsRef: DatabaseReference = database.getReference("conversations")

  override fun getNewMessageId(): String {
    // Generate a unique ID using Firebase's push() method
    return conversationsRef.push().key ?: System.currentTimeMillis().toString()
  }

  override fun observeMessagesForConversation(conversationId: String): Flow<List<Message>> =
      callbackFlow {
        val messagesRef =
            conversationsRef.child(conversationId).child("messages").orderByChild("timestamp")

        val listener =
            object : ValueEventListener {
              override fun onDataChange(snapshot: DataSnapshot) {
                val messages = mutableListOf<Message>()

                for (messageSnapshot in snapshot.children) {
                  val message = dataSnapshotToMessage(messageSnapshot, conversationId)
                  if (message != null) {
                    messages.add(message)
                  }
                }

                // Messages are already ordered by timestamp due to orderByChild("timestamp")
                trySend(messages)
              }

              override fun onCancelled(error: DatabaseError) {
                Log.e("ChatRepositoryRTDB", "Error observing messages", error.toException())
                trySend(emptyList())
              }
            }

        messagesRef.addValueEventListener(listener)

        awaitClose { messagesRef.removeEventListener(listener) }
      }

  override suspend fun addMessage(message: Message) {
    val messageRef =
        conversationsRef.child(message.conversationId).child("messages").child(message.id)

    val messageMap =
        mapOf(
            "senderId" to message.senderId,
            "senderName" to message.senderName,
            "content" to message.content,
            "timestamp" to message.timestamp,
            "type" to message.type.name,
            "readBy" to message.readBy,
            "isPinned" to message.isPinned)

    messageRef.setValue(messageMap).await()
  }

  override suspend fun editMessage(conversationId: String, messageId: String, newValue: Message) {
    val messageRef = conversationsRef.child(conversationId).child("messages").child(messageId)

    val updates =
        mapOf(
            "senderId" to newValue.senderId,
            "senderName" to newValue.senderName,
            "content" to newValue.content,
            "timestamp" to newValue.timestamp,
            "type" to newValue.type.name,
            "readBy" to newValue.readBy,
            "isPinned" to newValue.isPinned)

    messageRef.updateChildren(updates).await()
  }

  override suspend fun deleteMessage(conversationId: String, messageId: String) {
    val messageRef = conversationsRef.child(conversationId).child("messages").child(messageId)
    messageRef.removeValue().await()
  }

  override suspend fun markMessageAsRead(
      conversationId: String,
      messageId: String,
      userId: String
  ) {
    val messageRef = conversationsRef.child(conversationId).child("messages").child(messageId)

    // Get current readBy list
    val snapshot = messageRef.get().await()
    val currentReadBy =
        snapshot
            .child("readBy")
            .getValue(object : com.google.firebase.database.GenericTypeIndicator<List<String>>() {})
            ?: emptyList()

    // Add userId if not already present
    if (!currentReadBy.contains(userId)) {
      val updatedReadBy = currentReadBy + userId
      messageRef.child("readBy").setValue(updatedReadBy).await()
    }
  }

  /**
   * Converts a Realtime Database DataSnapshot to a [Message] object.
   *
   * @param snapshot The DataSnapshot from Realtime Database
   * @param conversationId The conversation ID this message belongs to
   * @return The [Message] object, or null if conversion fails
   */
  private fun dataSnapshotToMessage(snapshot: DataSnapshot, conversationId: String): Message? {
    return try {
      val id = snapshot.key ?: return null
      val senderId = snapshot.child("senderId").getValue(String::class.java) ?: return null
      val senderName = snapshot.child("senderName").getValue(String::class.java) ?: return null
      val content = snapshot.child("content").getValue(String::class.java) ?: return null
      val timestamp = snapshot.child("timestamp").getValue(Long::class.java) ?: return null

      val typeString = snapshot.child("type").getValue(String::class.java) ?: "TEXT"
      val type =
          try {
            MessageType.valueOf(typeString)
          } catch (_: IllegalArgumentException) {
            MessageType.TEXT
          }

      val readBy =
          snapshot
              .child("readBy")
              .getValue(
                  object : com.google.firebase.database.GenericTypeIndicator<List<String>>() {})
              ?: emptyList()
      val isPinned = snapshot.child("isPinned").getValue(Boolean::class.java) ?: false

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
      Log.e("ChatRepositoryRTDB", "Error converting snapshot to Message", e)
      null
    }
  }
}
