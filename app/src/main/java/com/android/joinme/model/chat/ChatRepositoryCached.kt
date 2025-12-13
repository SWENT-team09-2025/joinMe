package com.android.joinme.model.chat

import android.content.Context
import android.net.Uri
import android.util.Log
import com.android.joinme.model.database.AppDatabase
import com.android.joinme.model.database.toEntity
import com.android.joinme.model.database.toMessage
import com.android.joinme.model.event.OfflineException
import com.android.joinme.network.NetworkMonitor
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/**
 * Cached implementation of ChatRepository. Implements online-first read strategy with offline
 * fallback and online-only write strategy.
 *
 * **Read Strategy:**
 * 1. Emit cached messages immediately for instant UI
 * 2. Switch between Firebase real-time updates and cache based on network connectivity
 * 3. Fall back to cache on timeout or network errors
 *
 * **Write Strategy:**
 * 1. Require network connectivity (throw OfflineException if offline)
 * 2. Execute operation on Firebase Realtime Database
 * 3. Update local cache optimistically
 *
 * @param context Application context for database access
 * @param realtimeDbRepo The Firebase Realtime Database repository to delegate online operations to
 * @param networkMonitor Network connectivity monitor
 */
class ChatRepositoryCached(
    private val context: Context,
    private val realtimeDbRepo: ChatRepository,
    private val networkMonitor: NetworkMonitor
) : ChatRepository {

  companion object {
    private const val TAG = "ChatRepositoryCached"
    /** Timeout for Firebase operations in milliseconds (3 seconds) */
    private const val TIMEOUT_MS = 3000L
    private const val FIREBASE_ERROR_MSG = "Firebase operation failed, falling back to cache"
  }

  private val database = AppDatabase.getDatabase(context)
  private val messageDao = database.messageDao()

  override fun getNewMessageId(): String = realtimeDbRepo.getNewMessageId()

  /**
   * Observes messages for a conversation with automatic cache/online switching.
   *
   * Flow behavior:
   * 1. Immediately emits cached messages (instant UI)
   * 2. When online: Subscribes to Firebase real-time updates and updates cache
   * 3. When offline: Emits cached messages
   * 4. On timeout: Falls back to cache
   *
   * @param conversationId The conversation ID to observe
   * @return Flow of message lists that updates based on network status
   */
  override fun observeMessagesForConversation(conversationId: String): Flow<List<Message>> =
      callbackFlow {
        // 1. Emit cached messages immediately for instant UI
        val cachedMessages = messageDao.getMessagesForConversation(conversationId)
        send(cachedMessages.map { it.toMessage() })

        // 2. Observe network status and switch between Firebase and cache
        networkMonitor.observeNetworkStatus().collectLatest { isOnline ->
          if (isOnline) {
            // Online: Subscribe to real-time Firebase updates
            try {
              withTimeout(TIMEOUT_MS) {
                realtimeDbRepo.observeMessagesForConversation(conversationId).collect { messages ->
                  // Update cache in background
                  launch { messageDao.insertMessages(messages.map { it.toEntity() }) }
                  // Emit to UI
                  send(messages)
                }
              }
            } catch (e: TimeoutCancellationException) {
              Log.w(TAG, "Firebase timeout, falling back to cache", e)
              // Timeout - fall back to cache
              val fallbackMessages = messageDao.getMessagesForConversation(conversationId)
              send(fallbackMessages.map { it.toMessage() })
            } catch (e: Exception) {
              Log.w(TAG, FIREBASE_ERROR_MSG, e)
              // On error, continue showing cache (don't re-emit to avoid duplicates)
            }
          } else {
            // Offline - emit cached messages
            val offlineMessages = messageDao.getMessagesForConversation(conversationId)
            send(offlineMessages.map { it.toMessage() })
          }
        }

        awaitClose {
          // Flow cleanup (if needed)
        }
      }

  /**
   * Adds a new message to the conversation.
   *
   * Requires online connection. Updates Firebase first, then optimistically caches the message.
   *
   * @param message The message to add
   * @throws OfflineException if offline
   */
  override suspend fun addMessage(message: Message) {
    requireOnline()
    realtimeDbRepo.addMessage(message)
    // Optimistically cache the message
    messageDao.insertMessage(message.toEntity())
  }

  /**
   * Edits an existing message.
   *
   * Requires online connection. Updates Firebase first, then optimistically updates cache.
   *
   * @param conversationId The conversation ID
   * @param messageId The message ID to edit
   * @param newValue The updated message
   * @throws OfflineException if offline
   */
  override suspend fun editMessage(conversationId: String, messageId: String, newValue: Message) {
    requireOnline()
    realtimeDbRepo.editMessage(conversationId, messageId, newValue)
    // Optimistically update cache
    messageDao.insertMessage(newValue.toEntity())
  }

  /**
   * Deletes a message from the conversation.
   *
   * Requires online connection. Deletes from Firebase first, then removes from cache.
   *
   * @param conversationId The conversation ID
   * @param messageId The message ID to delete
   * @throws OfflineException if offline
   */
  override suspend fun deleteMessage(conversationId: String, messageId: String) {
    requireOnline()
    realtimeDbRepo.deleteMessage(conversationId, messageId)
    // Remove from cache
    messageDao.deleteMessage(conversationId, messageId)
  }

  /**
   * Marks a message as read by a specific user.
   *
   * Requires online connection. Updates Firebase first, then updates the readBy list in cache.
   *
   * @param conversationId The conversation ID
   * @param messageId The message ID
   * @param userId The user ID who read the message
   * @throws OfflineException if offline
   */
  override suspend fun markMessageAsRead(
      conversationId: String,
      messageId: String,
      userId: String
  ) {
    requireOnline()
    realtimeDbRepo.markMessageAsRead(conversationId, messageId, userId)

    // Update cache: Add userId to readBy list if not already present
    try {
      val cachedMessage = messageDao.getMessage(conversationId, messageId)
      if (cachedMessage != null) {
        val message = cachedMessage.toMessage()
        val updatedReadBy =
            if (!message.readBy.contains(userId)) {
              message.readBy + userId
            } else {
              message.readBy
            }
        val updatedMessage = message.copy(readBy = updatedReadBy)
        messageDao.insertMessage(updatedMessage.toEntity())
      }
    } catch (e: Exception) {
      Log.w(TAG, "Failed to update cache after markAsRead", e)
    }
  }

  /**
   * Uploads a chat image to Firebase Storage.
   *
   * Requires online connection. Image upload is online-only; only the URL is cached in the message
   * content.
   *
   * @param context Android context
   * @param conversationId The conversation ID
   * @param messageId The message ID
   * @param imageUri The image URI to upload
   * @return The Firebase Storage download URL
   * @throws OfflineException if offline
   */
  override suspend fun uploadChatImage(
      context: Context,
      conversationId: String,
      messageId: String,
      imageUri: Uri
  ): String {
    requireOnline()
    // Image upload is online-only, no caching of actual images
    return realtimeDbRepo.uploadChatImage(context, conversationId, messageId, imageUri)
  }

  /**
   * Checks if the device is online and throws an exception if not.
   *
   * This method is used to guard write operations that require network connectivity.
   *
   * @throws OfflineException if the device is offline
   */
  private fun requireOnline() {
    if (!networkMonitor.isOnline()) {
      throw OfflineException("This operation requires an internet connection")
    }
  }
}
