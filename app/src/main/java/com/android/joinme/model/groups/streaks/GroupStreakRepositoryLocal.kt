package com.android.joinme.model.groups.streaks

/**
 * In-memory implementation of [GroupStreakRepository] for testing and offline mode.
 *
 * Stores streaks in a map keyed by "{groupId}_{userId}" for efficient lookup.
 */
class GroupStreakRepositoryLocal : GroupStreakRepository {

    private val streaks: MutableMap<String, GroupStreak> = mutableMapOf()

    /** Generates a composite key for the streak map. */
    private fun key(groupId: String, userId: String): String = "${groupId}_${userId}"

    override suspend fun getStreaksForGroup(groupId: String): List<GroupStreak> {
        return streaks.values.filter { it.groupId == groupId }
    }

    override suspend fun getStreakForUser(groupId: String, userId: String): GroupStreak? {
        return streaks[key(groupId, userId)]
    }

    override suspend fun updateStreak(groupId: String, userId: String, streak: GroupStreak) {
        streaks[key(groupId, userId)] = streak.copy(groupId = groupId, userId = userId)
    }

    /** Clears all streaks. Useful for test setup/teardown. */
    fun clear() {
        streaks.clear()
    }
}