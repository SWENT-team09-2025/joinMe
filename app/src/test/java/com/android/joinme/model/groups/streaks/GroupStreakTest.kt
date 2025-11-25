package com.android.joinme.model.groups.streaks

import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests for [GroupStreak] data class.
 *
 * Verifies default values and basic construction.
 */
class GroupStreakTest {

  @Test
  fun `default values are correctly initialized`() {
    val streak = GroupStreak()

    assertEquals("", streak.groupId)
    assertEquals("", streak.userId)
    assertEquals(0, streak.currentStreakWeeks)
    assertEquals(0, streak.currentStreakActivities)
    assertEquals(0, streak.bestStreakWeeks)
    assertEquals(0, streak.bestStreakActivities)
  }

  @Test
  fun `constructor with custom values preserves all fields`() {
    val startDate = Timestamp.now()
    val lastActive = Timestamp.now()

    val streak =
        GroupStreak(
            groupId = "group123",
            userId = "user456",
            currentStreakWeeks = 5,
            currentStreakActivities = 12,
            currentStreakStartDate = startDate,
            lastActiveWeekStart = lastActive,
            bestStreakWeeks = 10,
            bestStreakActivities = 25)

    assertEquals("group123", streak.groupId)
    assertEquals("user456", streak.userId)
    assertEquals(5, streak.currentStreakWeeks)
    assertEquals(12, streak.currentStreakActivities)
    assertEquals(startDate, streak.currentStreakStartDate)
    assertEquals(lastActive, streak.lastActiveWeekStart)
    assertEquals(10, streak.bestStreakWeeks)
    assertEquals(25, streak.bestStreakActivities)
  }
}
