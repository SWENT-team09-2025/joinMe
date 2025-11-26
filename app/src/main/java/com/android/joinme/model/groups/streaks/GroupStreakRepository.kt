package com.android.joinme.model.groups.streaks

/**
 * Represents a repository that manages GroupStreak items for tracking individual user streaks in a
 * given Group.
 */
interface GroupStreakRepository {

  /**
   * Retrieves all streaks for a specific group.
   *
   * @param groupId The unique identifier of the group.
   * @return A list of all GroupStreak items for the specified group.
   */
  suspend fun getStreaksForGroup(groupId: String): List<GroupStreak>

  /**
   * Retrieves the streak for a specific user within a group.
   *
   * @param groupId The unique identifier of the group.
   * @param userId The unique identifier of the user.
   * @return The GroupStreak for the specified user, or null if not found.
   */
  suspend fun getStreakForUser(groupId: String, userId: String): GroupStreak?

  /**
   * Updates or creates a streak for a specific user within a group.
   *
   * @param groupId The unique identifier of the group.
   * @param userId The unique identifier of the user.
   * @param streak The GroupStreak data to save.
   */
  suspend fun updateStreak(groupId: String, userId: String, streak: GroupStreak)
}
