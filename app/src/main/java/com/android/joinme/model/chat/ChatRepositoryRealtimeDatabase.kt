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
 *         isEdited: false
 * ```
 *
 * This provides real-time synchronization where all clients receive updates instantly when messages
 * are added, edited, or deleted.
 */
class ChatRepositoryRealtimeDatabase(private val database: FirebaseDatabase) : ChatRepository {

  companion object {
    private const val TAG = "ChatRepositoryRTDB"
    private const val CONVERSATIONS_PATH = "conversations"
    private const val MESSAGES_PATH = "messages"

    // Message field names
    private const val FIELD_SENDER_ID = "senderId"
    private const val FIELD_SENDER_NAME = "senderName"
    private const val FIELD_CONTENT = "content"
    private const val FIELD_TIMESTAMP = "timestamp"
    private const val FIELD_TYPE = "type"
    private const val FIELD_READ_BY = "readBy"
    private const val FIELD_IS_PINNED = "isPinned"
    private const val FIELD_IS_EDITED = "isEdited"

    // Default values
    private const val DEFAULT_MESSAGE_TYPE = "TEXT"

    // Type indicator for deserializing List<String> from Realtime Database
    private val STRING_LIST_TYPE_INDICATOR =
        object : com.google.firebase.database.GenericTypeIndicator<List<String>>() {}
  }

  private val conversationsRef: DatabaseReference = database.getReference(CONVERSATIONS_PATH)

  override fun getNewMessageId(): String {
    // Generate a unique ID using Firebase's push() method
    return conversationsRef.push().key
        ?: throw IllegalStateException("Failed to generate message ID from Firebase")
  }

  override fun observeMessagesForConversation(conversationId: String): Flow<List<Message>> =
      callbackFlow {
        val messagesRef =
            conversationsRef
                .child(conversationId)
                .child(MESSAGES_PATH)
                .orderByChild(FIELD_TIMESTAMP)

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

                // Messages are already ordered by timestamp due to orderByChild(FIELD_TIMESTAMP)
                trySend(messages)
              }

              override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error observing messages", error.toException())
                trySend(emptyList())
              }
            }

        messagesRef.addValueEventListener(listener)

        awaitClose { messagesRef.removeEventListener(listener) }
      }

  override suspend fun addMessage(message: Message) {
    val messageRef =
        conversationsRef.child(message.conversationId).child(MESSAGES_PATH).child(message.id)
    messageRef.setValue(messageToMap(message)).await()
  }

  override suspend fun editMessage(conversationId: String, messageId: String, newValue: Message) {
    val messageRef = conversationsRef.child(conversationId).child(MESSAGES_PATH).child(messageId)
    messageRef.updateChildren(messageToMap(newValue)).await()
  }

  override suspend fun deleteMessage(conversationId: String, messageId: String) {
    val messageRef = conversationsRef.child(conversationId).child(MESSAGES_PATH).child(messageId)
    messageRef.removeValue().await()
  }

  override suspend fun markMessageAsRead(
      conversationId: String,
      messageId: String,
      userId: String
  ) {
    val messageRef = conversationsRef.child(conversationId).child(MESSAGES_PATH).child(messageId)

    // Get current readBy list
    val snapshot = messageRef.get().await()
    val currentReadBy =
        snapshot.child(FIELD_READ_BY).getValue(STRING_LIST_TYPE_INDICATOR) ?: emptyList()

    // Add userId if not already present
    if (!currentReadBy.contains(userId)) {
      val updatedReadBy = currentReadBy + userId
      messageRef.child(FIELD_READ_BY).setValue(updatedReadBy).await()
    }
  }

  /**
   * Converts a [Message] object to a map for storing in Realtime Database.
   *
   * @param message The message to convert
   * @return Map of field names to values
   */
  private fun messageToMap(message: Message): Map<String, Any?> {
    return mapOf(
        FIELD_SENDER_ID to message.senderId,
        FIELD_SENDER_NAME to message.senderName,
        FIELD_CONTENT to message.content,
        FIELD_TIMESTAMP to message.timestamp,
        FIELD_TYPE to message.type.name,
        FIELD_READ_BY to message.readBy,
        FIELD_IS_PINNED to message.isPinned,
        FIELD_IS_EDITED to message.isEdited)
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
      val senderId = snapshot.child(FIELD_SENDER_ID).getValue(String::class.java) ?: return null
      val senderName = snapshot.child(FIELD_SENDER_NAME).getValue(String::class.java) ?: return null
      val content = snapshot.child(FIELD_CONTENT).getValue(String::class.java) ?: return null
      val timestamp = snapshot.child(FIELD_TIMESTAMP).getValue(Long::class.java) ?: return null

      val typeString =
          snapshot.child(FIELD_TYPE).getValue(String::class.java) ?: DEFAULT_MESSAGE_TYPE
      val type =
          try {
            MessageType.valueOf(typeString)
          } catch (_: IllegalArgumentException) {
            MessageType.TEXT
          }

      val readBy = snapshot.child(FIELD_READ_BY).getValue(STRING_LIST_TYPE_INDICATOR) ?: emptyList()
      val isPinned = snapshot.child(FIELD_IS_PINNED).getValue(Boolean::class.java) ?: false
      val isEdited = snapshot.child(FIELD_IS_EDITED).getValue(Boolean::class.java) ?: false

      Message(
          id = id,
          conversationId = conversationId,
          senderId = senderId,
          senderName = senderName,
          content = content,
          timestamp = timestamp,
          type = type,
          readBy = readBy,
          isPinned = isPinned,
          isEdited = isEdited)
    } catch (e: Exception) {
      Log.e(TAG, "Error converting snapshot to Message", e)
      null
    }
  }
}
