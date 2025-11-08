package com.android.joinme.model.chat

// Implemented with help of Claude AI

interface ChatRepository {

  /**
   * Generates and returns a new unique identifier for a Message item.
   *
   * @return A new unique Message identifier.
   */
  fun getNewMessageId(): String

  /**
   * Retrieves a specific Message item by its unique identifier.
   *
   * @param messageId The unique identifier of the Message item to retrieve.
   * @return The Message item with the specified identifier.
   * @throws Exception if the message item is not found.
   */
  suspend fun getMessage(messageId: String): Message

  /**
   * Adds a new Message item to the repository.
   *
   * @param message The Message item to add.
   */
  suspend fun addMessage(message: Message)

  /**
   * Edits an existing Message item in the repository.
   *
   * @param messageId The unique identifier of the Message item to edit.
   * @param newValue The new value for the Message item.
   * @throws Exception if the Message item is not found.
   */
  suspend fun editMessage(messageId: String, newValue: Message)

  /**
   * Deletes a Message item from the repository.
   *
   * @param messageId The unique identifier of the Message item to delete.
   * @throws Exception if the Message item is not found.
   */
  suspend fun deleteMessage(messageId: String)

  /**
   * Marks a message as read by a specific user.
   *
   * @param messageId The unique identifier of the Message item to mark as read.
   * @param userId The unique identifier of the user who has read the message.
   */
  suspend fun markMessageAsRead(messageId: String, userId: String)
}
