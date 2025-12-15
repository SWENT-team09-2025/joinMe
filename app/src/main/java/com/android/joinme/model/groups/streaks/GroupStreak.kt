package com.android.joinme.model.groups.streaks

import com.google.firebase.Timestamp

/**
 * Represents an individual user's streak within a group.
 *
 * Tracks both the current ongoing streak and the user's best historical streak. A streak is
 * maintained by participating in group activities across consecutive weeks.
 *
 * @property groupId The unique identifier of the group this streak belongs to.
 * @property userId The unique identifier of the user who owns this streak.
 * @property currentStreakWeeks The number of consecutive weeks the user has been active.
 * @property currentStreakActivities The total number of activities completed in the current streak.
 * @property currentStreakStartDate The timestamp when the current streak began, or null if not set.
 * @property lastActiveWeekStart The start of the most recent week the user was active, or null if
 *   not set.
 * @property bestStreakWeeks The user's longest streak in weeks (historical best).
 * @property bestStreakActivities The total activities completed during the best streak.
 */
data class GroupStreak(
    val groupId: String = "",
    val userId: String = "",
    val currentStreakWeeks: Int = 0,
    val currentStreakActivities: Int = 0,
    val currentStreakStartDate: Timestamp? = null,
    val lastActiveWeekStart: Timestamp? = null,
    val bestStreakWeeks: Int = 0,
    val bestStreakActivities: Int = 0
)
