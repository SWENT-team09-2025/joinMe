package com.android.joinme.model.chat

// Implemented with help of Claude AI

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.flow.Flow

interface ChatRepository {

  /**
   * Generates and returns a new unique identifier for a Message item.
   *
   * @return A new unique Message identifier.
   */
  fun getNewMessageId(): String

  /**
   * Observes messages for a specific conversation and returns them as a Flow.
   *
   * This method provides real-time updates whenever messages are added, edited, or deleted in the
   * conversation. Messages are ordered by timestamp in ascending order.
   *
   * @param conversationId The unique identifier of the conversation to observe messages for.
   * @return A Flow of Message lists that updates in real-time.
   */
  fun observeMessagesForConversation(conversationId: String): Flow<List<Message>>

  /**
   * Adds a new Message item to the repository.
   *
   * The message must have a conversationId set to identify which conversation it belongs to.
   *
   * @param message The Message item to add.
   */
  suspend fun addMessage(message: Message)

  /**
   * Edits an existing Message item in the repository.
   *
   * @param conversationId The unique identifier of the conversation containing the message.
   * @param messageId The unique identifier of the Message item to edit.
   * @param newValue The new value for the Message item.
   * @throws Exception if the Message item is not found.
   */
  suspend fun editMessage(conversationId: String, messageId: String, newValue: Message)

  /**
   * Deletes a Message item from the repository.
   *
   * @param conversationId The unique identifier of the conversation containing the message.
   * @param messageId The unique identifier of the Message item to delete.
   * @throws Exception if the Message item is not found.
   */
  suspend fun deleteMessage(conversationId: String, messageId: String)

  /**
   * Marks a message as read by a specific user.
   *
   * @param conversationId The unique identifier of the conversation containing the message.
   * @param messageId The unique identifier of the Message item to mark as read.
   * @param userId The unique identifier of the user who has read the message.
   * @throws Exception if the Message item is not found.
   */
  suspend fun markMessageAsRead(conversationId: String, messageId: String, userId: String)

  /**
   * Uploads an image to Firebase Storage for use in a chat message.
   *
   * The image is processed (compressed, oriented, resized) before upload to optimize storage and
   * bandwidth. The resulting download URL should be stored in the Message content field.
   *
   * @param context Android context for accessing the content resolver.
   * @param conversationId The unique identifier of the conversation.
   * @param messageId The unique identifier for the message that will contain this image.
   * @param imageUri The URI of the image to upload (from camera or gallery).
   * @return The Firebase Storage download URL for the uploaded image.
   * @throws Exception wrapping the underlying error, which may include:
   *     - IOException for network failures or file access issues
   *     - SecurityException for permission denials
   *     - Storage quota exceeded errors
   *     - Image processing failures (invalid format, corrupted file, etc.)
   */
  suspend fun uploadChatImage(
      context: Context,
      conversationId: String,
      messageId: String,
      imageUri: Uri
  ): String

  /**
   * Deletes an entire conversation and all its associated data.
   *
   * This method performs a complete cleanup of a conversation including:
   * - All messages in the conversation
   * - All polls in the conversation (if PollRepository is provided)
   * - All images stored in Firebase Storage for this conversation
   * - The conversation node itself
   *
   * This operation is irreversible and should be called when entities (events, groups, series) that
   * own conversations are deleted.
   *
   * @param conversationId The unique identifier of the conversation to delete.
   * @param pollRepository Optional PollRepository instance to also delete polls. If null, only
   *   messages and images will be deleted.
   * @throws Exception if the deletion fails at any step.
   */
  suspend fun deleteConversation(conversationId: String, pollRepository: PollRepository? = null)

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
   * Note: This method only works with Firebase Realtime Database implementation as it requires
   * querying conversation nodes. The local implementation is a no-op.
   *
   * @param userId The unique identifier of the user whose conversations should be deleted.
   * @throws Exception if the deletion fails.
   */
  suspend fun deleteAllUserConversations(userId: String)
}
