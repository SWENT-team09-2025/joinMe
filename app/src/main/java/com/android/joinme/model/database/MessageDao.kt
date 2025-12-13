package com.android.joinme.model.database

import androidx.room.*

/**
 * Data Access Object for Message entities. Provides methods to query, insert, update, and delete
 * messages from the local Room database.
 *
 * All queries are conversation-scoped since messages are never queried globally across
 * conversations.
 */
@Dao
interface MessageDao {
  /**
   * Retrieves all messages for a specific conversation, ordered by timestamp ascending.
   *
   * This matches the real-time ordering from Firebase Realtime Database.
   *
   * @param conversationId The unique identifier of the conversation
   * @return List of message entities ordered by timestamp (oldest first)
   */
  @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
  suspend fun getMessagesForConversation(conversationId: String): List<MessageEntity>

  /**
   * Retrieves a single message by its ID and conversation ID.
   *
   * @param conversationId The unique identifier of the conversation
   * @param messageId The unique identifier of the message
   * @return The message entity if found, null otherwise
   */
  @Query("SELECT * FROM messages WHERE id = :messageId AND conversationId = :conversationId")
  suspend fun getMessage(conversationId: String, messageId: String): MessageEntity?

  /**
   * Inserts or updates a single message in the database.
   *
   * If a message with the same ID already exists, it will be replaced with the new data.
   *
   * @param message The message entity to insert or update
   */
  @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertMessage(message: MessageEntity)

  /**
   * Inserts or updates multiple messages in a batch.
   *
   * Used when caching messages fetched from Firebase. More efficient than individual inserts.
   *
   * @param messages List of message entities to insert or update
   */
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertMessages(messages: List<MessageEntity>)

  /**
   * Deletes a specific message from the database.
   *
   * @param conversationId The unique identifier of the conversation
   * @param messageId The unique identifier of the message to delete
   */
  @Query("DELETE FROM messages WHERE id = :messageId AND conversationId = :conversationId")
  suspend fun deleteMessage(conversationId: String, messageId: String)

  /**
   * Deletes all messages for a specific conversation.
   *
   * Useful for cache cleanup or when a user leaves a conversation.
   *
   * @param conversationId The unique identifier of the conversation
   */
  @Query("DELETE FROM messages WHERE conversationId = :conversationId")
  suspend fun deleteMessagesForConversation(conversationId: String)

  /**
   * Deletes all cached messages from the database.
   *
   * Useful for cache reset or troubleshooting.
   */
  @Query("DELETE FROM messages") suspend fun deleteAllMessages()

  /**
   * Deletes messages cached before a specific timestamp.
   *
   * Used for cache maintenance to remove old messages and free up storage.
   *
   * @param timestamp Unix timestamp in milliseconds. Messages cached before this will be deleted.
   */
  @Query("DELETE FROM messages WHERE cachedAt < :timestamp")
  suspend fun deleteOldMessages(timestamp: Long)
}
