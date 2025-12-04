package com.android.joinme.model.presence

// Implemented with help of Claude AI

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * In-memory implementation of [PresenceRepository] for offline mode or testing.
 *
 * This implementation stores presence data in memory and provides real-time updates through
 * StateFlow. Useful for testing without Firebase dependencies.
 *
 * Thread-safe: Uses Mutex to prevent race conditions when multiple coroutines access the presence
 * data concurrently.
 *
 * Data structure:
 * - presenceData: Map<contextId, Map<userId, PresenceEntry>>
 * - userContexts: Map<userId, Set<contextId>>
 */
open class PresenceRepositoryLocal : PresenceRepository {

  /**
   * Represents a user's presence entry in a context.
   *
   * @property visitorId The unique identifier of the user.
   * @property online Whether the user is currently online.
   * @property lastSeen Timestamp of when the user was last seen.
   */
  data class PresenceEntry(val visitorId: String, val online: Boolean, val lastSeen: Long)

  // Map<contextId, Map<userId, PresenceEntry>>
  private val presenceData = mutableMapOf<String, MutableMap<String, PresenceEntry>>()

  // Map<userId, Set<contextId>> - index for quick lookup during setUserOffline
  private val userContexts = mutableMapOf<String, MutableSet<String>>()

  // StateFlow for reactive updates
  private val presenceFlow = MutableStateFlow<Map<String, Map<String, PresenceEntry>>>(emptyMap())

  private val mutex = Mutex()

  override suspend fun setUserOnline(userId: String, contextIds: List<String>) {
    if (userId.isBlank()) return

    mutex.withLock {
      for (contextId in contextIds) {
        if (contextId.isBlank()) continue

        // Initialize context map if needed
        if (!presenceData.containsKey(contextId)) {
          presenceData[contextId] = mutableMapOf()
        }

        // Set user as online
        val entry = PresenceEntry(visitorId = userId, online = true, lastSeen = currentTimeMillis())
        presenceData[contextId]!![userId] = entry

        // Update userContexts index
        if (!userContexts.containsKey(userId)) {
          userContexts[userId] = mutableSetOf()
        }
        userContexts[userId]!!.add(contextId)
      }

      // Emit updated state
      presenceFlow.value = presenceData.toMap().mapValues { it.value.toMap() }
    }
  }

  override suspend fun setUserOffline(userId: String) {
    if (userId.isBlank()) return

    mutex.withLock {
      val contexts = userContexts[userId] ?: return@withLock

      for (contextId in contexts) {
        val contextPresence = presenceData[contextId] ?: continue
        val existingEntry = contextPresence[userId] ?: continue

        // Update to offline
        contextPresence[userId] = existingEntry.copy(online = false, lastSeen = currentTimeMillis())
      }

      // Emit updated state
      presenceFlow.value = presenceData.toMap().mapValues { it.value.toMap() }
    }
  }

  override fun observeOnlineUsersCount(contextId: String, currentUserId: String): Flow<Int> {
    if (contextId.isBlank() || currentUserId.isBlank()) {
      return MutableStateFlow(0)
    }

    return presenceFlow.map { allPresence ->
      val contextPresence = allPresence[contextId] ?: return@map 0
      contextPresence.values.count { entry -> entry.visitorId != currentUserId && entry.online }
    }
  }

  override fun observeOnlineUserIds(contextId: String, currentUserId: String): Flow<List<String>> {
    if (contextId.isBlank() || currentUserId.isBlank()) {
      return MutableStateFlow(emptyList())
    }

    return presenceFlow.map { allPresence ->
      val contextPresence = allPresence[contextId] ?: return@map emptyList()
      contextPresence.values
          .filter { entry -> entry.visitorId != currentUserId && entry.online }
          .map { it.visitorId }
    }
  }

  // -------------- Test Helper Methods --------------

  /**
   * Provides current time in milliseconds. Can be overridden for testing.
   *
   * @return Current time in milliseconds.
   */
  internal open fun currentTimeMillis(): Long = System.currentTimeMillis()

  /**
   * Gets the current presence data for a specific context. For testing purposes only.
   *
   * @param contextId The context ID to get presence data for.
   * @return Map of userId to PresenceEntry for the context, or null if context doesn't exist.
   */
  fun getPresenceDataForContext(contextId: String): Map<String, PresenceEntry>? {
    return presenceData[contextId]?.toMap()
  }

  /**
   * Gets all context IDs that a user is tracked in. For testing purposes only.
   *
   * @param userId The user ID to get contexts for.
   * @return Set of context IDs, or null if user is not tracked.
   */
  fun getUserContexts(userId: String): Set<String>? {
    return userContexts[userId]?.toSet()
  }

  /** Clears all presence data. For testing purposes only. */
  suspend fun clearAll() {
    mutex.withLock {
      presenceData.clear()
      userContexts.clear()
      presenceFlow.value = emptyMap()
    }
  }
}
