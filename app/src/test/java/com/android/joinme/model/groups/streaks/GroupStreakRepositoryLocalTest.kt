package com.android.joinme.model.groups.streaks

// Implemented with the help of AI tools, adapted and refactored to follow project pattern.

import com.google.firebase.Timestamp
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for [GroupStreakRepositoryLocal].
 *
 * Tests the in-memory repository implementation without mocking.
 */
class GroupStreakRepositoryLocalTest {

  private lateinit var repository: GroupStreakRepositoryLocal

  private val testGroupId = "group123"
  private val testUserId = "user456"

  private fun createTestStreak(
      groupId: String = testGroupId,
      userId: String = testUserId,
      currentWeeks: Int = 3,
      bestWeeks: Int = 5
  ): GroupStreak {
    return GroupStreak(
        groupId = groupId,
        userId = userId,
        currentStreakWeeks = currentWeeks,
        currentStreakActivities = currentWeeks * 2,
        currentStreakStartDate = Timestamp.now(),
        lastActiveWeekStart = Timestamp.now(),
        bestStreakWeeks = bestWeeks,
        bestStreakActivities = bestWeeks * 3)
  }

  @Before
  fun setup() {
    repository = GroupStreakRepositoryLocal()
  }

  // ==================== getStreaksForGroup ====================

  @Test
  fun `getStreaksForGroup returns empty list when no streaks exist`() = runTest {
    val result = repository.getStreaksForGroup(testGroupId)
    assertTrue(result.isEmpty())
  }

  @Test
  fun `getStreaksForGroup returns only streaks for specified group`() = runTest {
    // Given: streaks in two different groups
    val streak1 = createTestStreak(groupId = testGroupId, userId = "userA")
    val streak2 = createTestStreak(groupId = testGroupId, userId = "userB")
    val streak3 = createTestStreak(groupId = "otherGroup", userId = "userC")

    repository.updateStreak(testGroupId, "userA", streak1)
    repository.updateStreak(testGroupId, "userB", streak2)
    repository.updateStreak("otherGroup", "userC", streak3)

    // When
    val result = repository.getStreaksForGroup(testGroupId)

    // Then
    assertEquals(2, result.size)
    assertTrue(result.all { it.groupId == testGroupId })
  }

  // ==================== getStreakForUser ====================

  @Test
  fun `getStreakForUser returns null when streak does not exist`() = runTest {
    val result = repository.getStreakForUser(testGroupId, testUserId)
    assertNull(result)
  }

  @Test
  fun `getStreakForUser returns correct streak after update`() = runTest {
    // Given
    val streak = createTestStreak(currentWeeks = 7, bestWeeks = 10)
    repository.updateStreak(testGroupId, testUserId, streak)

    // When
    val result = repository.getStreakForUser(testGroupId, testUserId)

    // Then
    assertEquals(7, result?.currentStreakWeeks)
    assertEquals(10, result?.bestStreakWeeks)
    assertEquals(testGroupId, result?.groupId)
    assertEquals(testUserId, result?.userId)
  }

  // ==================== updateStreak ====================

  @Test
  fun `updateStreak creates new streak when none exists`() = runTest {
    // Given
    val streak = createTestStreak()

    // When
    repository.updateStreak(testGroupId, testUserId, streak)

    // Then
    val result = repository.getStreakForUser(testGroupId, testUserId)
    assertEquals(streak.currentStreakWeeks, result?.currentStreakWeeks)
  }

  @Test
  fun `updateStreak overwrites existing streak`() = runTest {
    // Given: existing streak
    val oldStreak = createTestStreak(currentWeeks = 2, bestWeeks = 2)
    repository.updateStreak(testGroupId, testUserId, oldStreak)

    // When: update with new values
    val newStreak = createTestStreak(currentWeeks = 5, bestWeeks = 8)
    repository.updateStreak(testGroupId, testUserId, newStreak)

    // Then
    val result = repository.getStreakForUser(testGroupId, testUserId)
    assertEquals(5, result?.currentStreakWeeks)
    assertEquals(8, result?.bestStreakWeeks)
  }

  @Test
  fun `updateStreak enforces groupId and userId from parameters`() = runTest {
    // Given: streak with mismatched ids
    val streak = createTestStreak(groupId = "wrongGroup", userId = "wrongUser")

    // When: update with correct ids as parameters
    repository.updateStreak(testGroupId, testUserId, streak)

    // Then: stored streak should have parameter ids, not the ones in the object
    val result = repository.getStreakForUser(testGroupId, testUserId)
    assertEquals(testGroupId, result?.groupId)
    assertEquals(testUserId, result?.userId)
  }

  // ==================== deleteStreakForUser ====================

  @Test
  fun `deleteStreakForUser removes streak and handles non-existent streak`() = runTest {
    // Given: add a streak
    val streak = createTestStreak()
    repository.updateStreak(testGroupId, testUserId, streak)
    assertEquals(
        streak.currentStreakWeeks,
        repository.getStreakForUser(testGroupId, testUserId)?.currentStreakWeeks)

    // When: delete the streak
    repository.deleteStreakForUser(testGroupId, testUserId)

    // Then: streak is removed
    assertNull(repository.getStreakForUser(testGroupId, testUserId))

    // Deleting non-existent streak should not throw
    repository.deleteStreakForUser(testGroupId, "nonExistentUser")
    repository.deleteStreakForUser("nonExistentGroup", testUserId)
  }

  // ==================== deleteAllStreaksForGroup ====================

  @Test
  fun `deleteAllStreaksForGroup removes all streaks for group and preserves others`() = runTest {
    // Given: streaks in multiple groups
    repository.updateStreak(testGroupId, "userA", createTestStreak(testGroupId, "userA"))
    repository.updateStreak(testGroupId, "userB", createTestStreak(testGroupId, "userB"))
    repository.updateStreak(testGroupId, "userC", createTestStreak(testGroupId, "userC"))
    repository.updateStreak("otherGroup", "userD", createTestStreak("otherGroup", "userD"))

    assertEquals(3, repository.getStreaksForGroup(testGroupId).size)
    assertEquals(1, repository.getStreaksForGroup("otherGroup").size)

    // When: delete all streaks for testGroupId
    repository.deleteAllStreaksForGroup(testGroupId)

    // Then: testGroupId streaks are removed, otherGroup streak preserved
    assertTrue(repository.getStreaksForGroup(testGroupId).isEmpty())
    assertEquals(1, repository.getStreaksForGroup("otherGroup").size)

    // Deleting from empty/non-existent group should not throw
    repository.deleteAllStreaksForGroup("nonExistentGroup")
    repository.deleteAllStreaksForGroup(testGroupId)
  }

  // ==================== clear ====================

  @Test
  fun `clear removes all streaks`() = runTest {
    // Given
    repository.updateStreak("group1", "user1", createTestStreak("group1", "user1"))
    repository.updateStreak("group2", "user2", createTestStreak("group2", "user2"))

    // When
    repository.clear()

    // Then
    assertTrue(repository.getStreaksForGroup("group1").isEmpty())
    assertTrue(repository.getStreaksForGroup("group2").isEmpty())
  }
}
