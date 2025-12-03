package com.android.joinme.model.presence

// Implemented with help of Claude AI

import android.util.Log
import com.android.joinme.model.event.EventFilter
import com.android.joinme.model.event.EventsRepositoryProvider
import com.android.joinme.model.groups.GroupRepositoryProvider

/**
 * Implementation of [ContextIdProvider] for the JoinMe app.
 *
 * This provider fetches all context IDs (chat IDs) that a user belongs to:
 * - Group IDs: From groups where the user is a member
 * - Event IDs: From events where the user is a participant
 *
 * These IDs are used by [PresenceManager] to set the user as online in all their chat contexts when
 * the app is in the foreground.
 */
class JoinMeContextIdProvider : ContextIdProvider {

  companion object {
    private const val TAG = "JoinMeContextIdProvider"
  }

  /**
   * Fetches all context IDs (group IDs and event IDs) that the user belongs to.
   *
   * @param userId The user ID to fetch context IDs for.
   * @return List of context IDs where the user should be tracked as online.
   */
  override suspend fun getContextIdsForUser(userId: String): List<String> {
    if (userId.isBlank()) {
      Log.w(TAG, "getContextIdsForUser called with blank userId")
      return emptyList()
    }

    val contextIds = mutableListOf<String>()

    // Fetch group IDs where user is a member
    try {
      val groups = GroupRepositoryProvider.repository.getAllGroups()
      val groupIds = groups.map { it.id }
      contextIds.addAll(groupIds)
    } catch (e: Exception) {
      Log.e(TAG, "Failed to fetch groups for user $userId", e)
    }

    // Fetch event IDs where user is a participant
    try {
      val events =
          EventsRepositoryProvider.getRepository(isOnline = true)
              .getAllEvents(EventFilter.EVENTS_FOR_OVERVIEW_SCREEN)
      val eventIds = events.map { it.eventId }
      contextIds.addAll(eventIds)
    } catch (e: Exception) {
      Log.e(TAG, "Failed to fetch events for user $userId", e)
    }
    return contextIds
  }
}
