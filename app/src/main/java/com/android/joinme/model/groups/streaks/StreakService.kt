package com.android.joinme.model.groups.streaks

// Implemented with the help of AI tools, adapted and refactored to follow project pattern.

import android.util.Log
import com.android.joinme.model.event.Event
import com.android.joinme.model.event.EventsRepository
import com.android.joinme.model.event.EventsRepositoryProvider
import com.android.joinme.model.groups.GroupRepository
import com.android.joinme.model.groups.GroupRepositoryProvider
import com.android.joinme.model.serie.Serie
import com.android.joinme.model.serie.SeriesRepository
import com.android.joinme.model.serie.SeriesRepositoryProvider
import com.google.firebase.Timestamp
import java.util.Calendar
import java.util.TimeZone

/**
 * Service responsible for computing and updating user streaks within groups.
 *
 * A streak is maintained by participating in group activities across consecutive weeks. Week
 * boundaries are defined as Monday 00:00 UTC to Sunday 23:59 UTC.
 *
 * This service should be called when:
 * - A user joins or creates an event/serie linked to a group
 * - An activity is deleted before it occurs (to revert streak changes)
 * - A user leaves a group (to delete their streak)
 */
object StreakService {

  private val streakRepository: GroupStreakRepository
    get() = GroupStreakRepositoryProvider.getRepository()

  private val eventsRepository: EventsRepository
    get() = EventsRepositoryProvider.getRepository(isOnline = true)

  private val seriesRepository: SeriesRepository
    get() = SeriesRepositoryProvider.repository

  private val groupRepository: GroupRepository
    get() = GroupRepositoryProvider.repository

  /** Injectable clock for testing. Returns current time by default. */
  var clock: () -> Timestamp = { Timestamp.now() }

  /**
   * Called when a user joins or creates a group activity (event or serie).
   *
   * Updates the user's streak by:
   * 1. Incrementing currentStreakActivities
   * 2. Incrementing currentStreakWeeks if this is a new week
   * 3. Resetting the streak if lastActiveWeekStart is more than 1 week ago
   * 4. Updating bestStreak fields if current exceeds best
   *
   * @param groupId The group the activity belongs to. If null, no streak update occurs.
   * @param userId The user who joined the activity.
   * @param activityDate The date of the activity.
   */
  suspend fun onActivityJoined(groupId: String?, userId: String, activityDate: Timestamp) {
    if (groupId == null) return

    val currentWeekStart = getWeekStart(activityDate)
    val existingStreak = streakRepository.getStreakForUser(groupId, userId)

    val updatedStreak =
        if (existingStreak == null) {
          // First activity in this group, we start a new streak
          GroupStreak(
              groupId = groupId,
              userId = userId,
              currentStreakWeeks = 1,
              currentStreakActivities = 1,
              currentStreakStartDate = currentWeekStart,
              lastActiveWeekStart = currentWeekStart,
              bestStreakWeeks = 1,
              bestStreakActivities = 1)
        } else {
          computeUpdatedStreak(existingStreak, currentWeekStart)
        }

    streakRepository.updateStreak(groupId, userId, updatedStreak)
  }

  /**
   * Called when a group activity is deleted BEFORE it occurs.
   *
   * Reverts streak changes for all affected users by:
   * 1. Decrementing currentStreakActivities
   * 2. Adjusting currentStreakWeeks and lastActiveWeekStart if needed
   * 3. Never modifies bestStreak fields (historical records)
   *
   * @param groupId The group the activity belongs to. If null, no streak update occurs.
   * @param userIds List of users who were participants in the deleted activity.
   * @param activityDate The date of the deleted activity.
   */
  suspend fun onActivityDeleted(groupId: String?, userIds: List<String>, activityDate: Timestamp) {
    if (groupId == null || userIds.isEmpty()) return

    val deletedActivityWeekStart = getWeekStart(activityDate)

    for (userId in userIds) {
      val existingStreak = streakRepository.getStreakForUser(groupId, userId) ?: continue

      // Check if user has other activities in the same week for this group
      val hasOtherActivitiesInWeek =
          hasOtherActivitiesInWeek(
              groupId = groupId,
              userId = userId,
              weekStart = deletedActivityWeekStart,
              excludeDate = activityDate)

      val updatedStreak =
          computeStreakAfterDeletion(
              existingStreak = existingStreak,
              hasOtherActivitiesInWeek = hasOtherActivitiesInWeek,
              groupId = groupId,
              userId = userId)

      streakRepository.updateStreak(groupId, userId, updatedStreak)
    }
  }

  /**
   * Called when a user leaves a group.
   *
   * Deletes the user's streak data for that group.
   *
   * @param groupId The group the user is leaving.
   * @param userId The user who is leaving the group.
   */
  suspend fun onUserLeftGroup(groupId: String, userId: String) {
    streakRepository.deleteStreakForUser(groupId, userId)
  }

  /**
   * Called when a group is deleted.
   *
   * Deletes all streak data for that group.
   *
   * @param groupId The group being deleted.
   */
  suspend fun onGroupDeleted(groupId: String) {
    streakRepository.deleteAllStreaksForGroup(groupId)
  }

  /**
   * Called when a user quits a group activity (event or serie).
   *
   * Reverts streak changes for the user by:
   * 1. Decrementing currentStreakActivities
   * 2. Adjusting currentStreakWeeks and lastActiveWeekStart if needed
   * 3. Never modifies bestStreak fields (historical records)
   *
   * @param groupId The group the activity belongs to. If null, no streak update occurs.
   * @param userId The user who quit the activity.
   * @param activityDate The date of the activity.
   */
  suspend fun onActivityLeft(groupId: String?, userId: String, activityDate: Timestamp) {
    if (groupId == null) return

    val existingStreak = streakRepository.getStreakForUser(groupId, userId) ?: return

    val leftActivityWeekStart = getWeekStart(activityDate)

    // Check if user has other activities in the same week for this group
    val hasOtherActivitiesInWeek =
        hasOtherActivitiesInWeek(
            groupId = groupId,
            userId = userId,
            weekStart = leftActivityWeekStart,
            excludeDate = activityDate)

    val updatedStreak =
        computeStreakAfterDeletion(
            existingStreak = existingStreak,
            hasOtherActivitiesInWeek = hasOtherActivitiesInWeek,
            groupId = groupId,
            userId = userId)

    streakRepository.updateStreak(groupId, userId, updatedStreak)
  }

  /** Computes the updated streak after a user joins an activity. */
  private fun computeUpdatedStreak(
      existingStreak: GroupStreak,
      currentWeekStart: Timestamp
  ): GroupStreak {
    val lastActiveWeek = existingStreak.lastActiveWeekStart

    return when {
      // Same week, we just increment activities
      lastActiveWeek != null && isSameWeek(lastActiveWeek, currentWeekStart) -> {
        val newActivities = existingStreak.currentStreakActivities + 1
        existingStreak.copy(
            currentStreakActivities = newActivities,
            bestStreakActivities = maxOf(existingStreak.bestStreakActivities, newActivities))
      }

      // Consecutive week, we increment both weeks and activities
      lastActiveWeek != null && isConsecutiveWeek(lastActiveWeek, currentWeekStart) -> {
        val newWeeks = existingStreak.currentStreakWeeks + 1
        val newActivities = existingStreak.currentStreakActivities + 1
        existingStreak.copy(
            currentStreakWeeks = newWeeks,
            currentStreakActivities = newActivities,
            lastActiveWeekStart = currentWeekStart,
            bestStreakWeeks = maxOf(existingStreak.bestStreakWeeks, newWeeks),
            bestStreakActivities = maxOf(existingStreak.bestStreakActivities, newActivities))
      }

      // Streak broken, we reset and start fresh
      else -> {
        existingStreak.copy(
            currentStreakWeeks = 1,
            currentStreakActivities = 1,
            currentStreakStartDate = currentWeekStart,
            lastActiveWeekStart = currentWeekStart
            // bestStreak fields remain unchanged
            )
      }
    }
  }

  /** Computes the updated streak after an activity is deleted or user quits. */
  private suspend fun computeStreakAfterDeletion(
      existingStreak: GroupStreak,
      hasOtherActivitiesInWeek: Boolean,
      groupId: String,
      userId: String
  ): GroupStreak {
    val newActivities = maxOf(0, existingStreak.currentStreakActivities - 1)

    return if (hasOtherActivitiesInWeek) {
      // Other activities exist in the same week, we just decrement activities
      existingStreak.copy(currentStreakActivities = newActivities)
    } else {
      // No other activities in this week, we need to adjust weeks and timestamps
      val newWeeks = maxOf(0, existingStreak.currentStreakWeeks - 1)

      if (newWeeks == 0) {
        // Streak completely removed
        existingStreak.copy(
            currentStreakWeeks = 0,
            currentStreakActivities = 0,
            currentStreakStartDate = null,
            lastActiveWeekStart = null)
      } else {
        // Find the new lastActiveWeekStart by querying remaining activities
        val newLastActiveWeekStart = findLastActiveWeekStart(groupId, userId)

        existingStreak.copy(
            currentStreakWeeks = newWeeks,
            currentStreakActivities = newActivities,
            lastActiveWeekStart = newLastActiveWeekStart)
      }
    }
  }

  /** Checks if the user has other activities in the same week for the given group. */
  private suspend fun hasOtherActivitiesInWeek(
      groupId: String,
      userId: String,
      weekStart: Timestamp,
      excludeDate: Timestamp
  ): Boolean {
    // Check events
    val events = getEventsForUserInGroup(groupId, userId)
    val hasOtherEvent =
        events.any { event ->
          val eventWeekStart = getWeekStart(event.date)
          isSameWeek(eventWeekStart, weekStart) && event.date != excludeDate
        }

    if (hasOtherEvent) return true

    // Check series
    val series = getSeriesForUserInGroup(groupId, userId)
    val hasOtherSerie =
        series.any { serie ->
          val serieWeekStart = getWeekStart(serie.date)
          isSameWeek(serieWeekStart, weekStart) && serie.date != excludeDate
        }

    return hasOtherSerie
  }

  /** Finds the most recent week start where the user had activity in the group. */
  private suspend fun findLastActiveWeekStart(groupId: String, userId: String): Timestamp? {
    val events = getEventsForUserInGroup(groupId, userId)
    val series = getSeriesForUserInGroup(groupId, userId)

    val allDates = events.map { it.date } + series.map { it.date }

    return allDates.map { getWeekStart(it) }.maxByOrNull { it.toDate().time }
  }

  /** Gets all events for a user in a specific group. */
  private suspend fun getEventsForUserInGroup(groupId: String, userId: String): List<Event> {
    return try {
      val group = groupRepository.getGroup(groupId)
      if (group.eventIds.isEmpty()) return emptyList()

      eventsRepository.getEventsByIds(group.eventIds).filter { it.participants.contains(userId) }
    } catch (e: Exception) {
      Log.e("StreakService", "Failed to fetch events for user $userId in group $groupId", e)
      throw e
    }
  }

  /** Gets all series for a user in a specific group. */
  private suspend fun getSeriesForUserInGroup(groupId: String, userId: String): List<Serie> {
    return try {
      val group = groupRepository.getGroup(groupId)
      if (group.serieIds.isEmpty()) return emptyList()

      seriesRepository.getSeriesByIds(group.serieIds).filter { it.participants.contains(userId) }
    } catch (e: Exception) {
      Log.e("StreakService", "Failed to fetch series for user $userId in group $groupId", e)
      throw e
    }
  }

  /** Gets the start of the week (Monday 00:00 UTC) for the given timestamp. */
  internal fun getWeekStart(timestamp: Timestamp): Timestamp {
    val calendar =
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
          time = timestamp.toDate()
          set(Calendar.HOUR_OF_DAY, 0)
          set(Calendar.MINUTE, 0)
          set(Calendar.SECOND, 0)
          set(Calendar.MILLISECOND, 0)

          // Adjust to Monday (Calendar.MONDAY = 2, Calendar.SUNDAY = 1)
          val dayOfWeek = get(Calendar.DAY_OF_WEEK)
          val daysToSubtract = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY
          add(Calendar.DAY_OF_MONTH, -daysToSubtract)
        }
    return Timestamp(calendar.time)
  }

  /** Checks if two timestamps fall within the same week. */
  internal fun isSameWeek(t1: Timestamp, t2: Timestamp): Boolean {
    val week1 = getWeekStart(t1)
    val week2 = getWeekStart(t2)
    return week1.toDate().time == week2.toDate().time
  }

  /** Checks if currentWeekStart is exactly one week after lastWeekStart. */
  internal fun isConsecutiveWeek(lastWeekStart: Timestamp, currentWeekStart: Timestamp): Boolean {
    val lastWeek = getWeekStart(lastWeekStart)
    val currentWeek = getWeekStart(currentWeekStart)

    val diffMillis = currentWeek.toDate().time - lastWeek.toDate().time
    val oneWeekMillis = 7 * 24 * 60 * 60 * 1000L

    return diffMillis == oneWeekMillis
  }

  /** Resets the clock to default behavior. Used for test cleanup. */
  fun resetClock() {
    clock = { Timestamp.now() }
  }
}
