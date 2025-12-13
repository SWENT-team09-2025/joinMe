package com.android.joinme.model.chat

import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

/**
 * Service responsible for cleaning up conversations and all associated data.
 *
 * This service provides centralized methods to delete conversations and their related data
 * (messages, polls, images) when entities like events, groups, or series are deleted.
 *
 * By centralizing cleanup logic here, we avoid circular dependencies between repositories and
 * maintain separation of concerns.
 */
object ConversationCleanupService {

  private const val CONVERSATIONS_PATH = "conversations"
  private const val MESSAGES_PATH = "messages"
  private const val IMAGES_PATH = "images"
  private const val FIELD_TYPE = "type"

  /**
   * Deletes an entire conversation and all its associated data.
   *
   * This method performs a complete cleanup including:
   * - All messages in the conversation
   * - All polls in the conversation
   * - All images stored in Firebase Storage for this conversation
   * - The conversation node itself
   *
   * This operation is irreversible and should be called when entities (events, groups, series) that
   * own conversations are deleted.
   *
   * @param conversationId The unique identifier of the conversation to delete. This is typically
   *   the same as the event ID, group ID, or series ID.
   * @throws Exception if the deletion fails at any step.
   */
  suspend fun cleanupConversation(conversationId: String) {
    try {
      val database = FirebaseDatabase.getInstance()
      val storage = FirebaseStorage.getInstance()
      val conversationRef = database.getReference(CONVERSATIONS_PATH).child(conversationId)

      // Step 1: Get all messages to find IMAGE type messages that need storage cleanup
      val messagesSnapshot = conversationRef.child(MESSAGES_PATH).get().await()

      val imageMessageIds = mutableListOf<String>()
      messagesSnapshot.children.forEach { messageSnapshot ->
        val typeString = messageSnapshot.child(FIELD_TYPE).getValue(String::class.java)
        if (typeString == MessageType.IMAGE.name) {
          messageSnapshot.key?.let { messageId -> imageMessageIds.add(messageId) }
        }
      }

      // Step 2: Delete all images from Firebase Storage
      val imagesRef =
          storage.reference.child(CONVERSATIONS_PATH).child(conversationId).child(IMAGES_PATH)

      imageMessageIds.forEach { messageId ->
        try {
          imagesRef.child("$messageId.jpg").delete().await()
        } catch (_: Exception) {
          // Image might not exist, continue with other deletions
        }
      }

      // Step 3: Delete the entire conversation node (includes messages, polls, and any other data)
      conversationRef.removeValue().await()
    } catch (e: Exception) {
      throw Exception("Failed to cleanup conversation $conversationId: ${e.message}", e)
    }
  }

  /**
   * Deletes all direct message conversations involving a specific user.
   *
   * This method finds and deletes all conversations where the user is a participant in a direct
   * message. Direct message conversation IDs follow the pattern "dm_{userId1}_{userId2}" where
   * userIds are sorted alphabetically.
   *
   * This should be called when a user profile is deleted to clean up all their private
   * conversations.
   *
   * For each DM conversation found, this will delete:
   * - All messages in the conversation
   * - All polls in the conversation
   * - All images stored in Firebase Storage
   * - The conversation node itself
   *
   * @param userId The unique identifier of the user whose conversations should be deleted.
   * @throws Exception if the deletion fails.
   */
  suspend fun cleanupUserConversations(userId: String) {
    try {
      val database = FirebaseDatabase.getInstance()
      val conversationsRef = database.getReference(CONVERSATIONS_PATH)

      // Get all conversation nodes from Firebase Realtime Database
      val allConversationsSnapshot = conversationsRef.get().await()

      // Find all conversations that are DMs involving this user
      // DM conversation IDs follow the pattern: dm_{userId1}_{userId2}
      val conversationsToDelete = mutableListOf<String>()

      allConversationsSnapshot.children.forEach { conversationSnapshot ->
        val conversationId = conversationSnapshot.key ?: return@forEach

        // Check if this is a DM conversation involving the user
        if (conversationId.startsWith("dm_")) {
          val parts = conversationId.split("_")
          // parts[0] = "dm", parts[1] = userId1, parts[2] = userId2
          if ((parts.size == 3) && (parts[1] == userId || parts[2] == userId)) {
            conversationsToDelete.add(conversationId)
          }
        }
      }

      // Delete each conversation
      conversationsToDelete.forEach { conversationId -> cleanupConversation(conversationId) }
    } catch (e: Exception) {
      throw Exception("Failed to cleanup conversations for user $userId: ${e.message}", e)
    }
  }
}
