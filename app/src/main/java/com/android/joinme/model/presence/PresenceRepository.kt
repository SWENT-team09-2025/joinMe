package com.android.joinme.model.presence

import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing user online presence status.
 *
 * This interface provides methods to track when users are online (have the app open) and observe
 * online users within a specific context. A "context" can be any feature that needs presence
 * tracking, such as:
 * - Chat conversations (group chats, event chats)
 * - Live events or activities
 * - Collaborative features
 * - Any other feature requiring real-time user presence
 *
 * Presence data is stored in Firebase Realtime Database for optimal real-time performance.
 *
 * A user is considered "online" when they have the app open, regardless of which screen they are
 * on.
 *
 * Database structure:
 * ```
 * presence/
 *   {contextId}/
 *     {userId}/
 *       visitorId: "..."
 *       online: true/false
 *       lastSeen: <server timestamp>
 * ```
 */
interface PresenceRepository {

  /**
   * Sets the current user as online in all specified contexts.
   *
   * This should be called when the app comes to the foreground. It sets up Firebase presence
   * tracking with onDisconnect handlers to automatically mark the user as offline when they close
   * the app or lose connection.
   *
   * @param userId The unique identifier of the current user.
   * @param contextIds The list of context IDs the user belongs to (e.g., chat IDs, event IDs).
   */
  suspend fun setUserOnline(userId: String, contextIds: List<String>)

  /**
   * Sets the current user as offline in all contexts.
   *
   * This should be called when the app goes to the background or when the user logs out.
   *
   * @param userId The unique identifier of the user to mark as offline.
   */
  suspend fun setUserOffline(userId: String)

  /**
   * Observes the count of online users in a specific context.
   *
   * Returns a Flow that emits the number of users currently online within the specified context.
   * The count excludes the current user.
   *
   * @param contextId The unique identifier of the context to observe (e.g., chat ID, event ID).
   * @param currentUserId The ID of the current user (to exclude from the count).
   * @return A Flow emitting the count of other online users in the context.
   */
  fun observeOnlineUsersCount(contextId: String, currentUserId: String): Flow<Int>

  /**
   * Observes the list of online user IDs in a specific context.
   *
   * Returns a Flow that emits the list of user IDs who are currently online within the specified
   * context. The list excludes the current user.
   *
   * @param contextId The unique identifier of the context to observe (e.g., chat ID, event ID).
   * @param currentUserId The ID of the current user (to exclude from the list).
   * @return A Flow emitting the list of online user IDs in the context.
   */
  fun observeOnlineUserIds(contextId: String, currentUserId: String): Flow<List<String>>

  /**
   * Cleans up stale presence data.
   *
   * This method removes presence entries for users whose lastSeen timestamp is older than the
   * specified threshold. Should be called periodically to handle cases where onDisconnect handlers
   * failed.
   *
   * @param staleThresholdMs The threshold in milliseconds. Entries older than this are removed.
   */
  suspend fun cleanupStalePresence(staleThresholdMs: Long = STALE_THRESHOLD_MS)

  companion object {
    /** Default threshold for considering presence data stale (5 minutes). */
    const val STALE_THRESHOLD_MS = 5 * 60 * 1000L
  }
}
