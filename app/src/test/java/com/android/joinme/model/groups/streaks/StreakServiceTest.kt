package com.android.joinme.model.groups.streaks

import com.android.joinme.model.event.Event
import com.android.joinme.model.event.EventType
import com.android.joinme.model.event.EventVisibility
import com.android.joinme.model.event.EventsRepository
import com.android.joinme.model.event.EventsRepositoryProvider
import com.android.joinme.model.groups.Group
import com.android.joinme.model.groups.GroupRepository
import com.android.joinme.model.groups.GroupRepositoryProvider
import com.android.joinme.model.serie.SeriesRepository
import com.android.joinme.model.serie.SeriesRepositoryProvider
import com.google.firebase.Timestamp
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import java.util.Calendar
import java.util.TimeZone
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for [StreakService].
 *
 * Tests are organized by functionality:
 * - Week boundary calculations
 * - onActivityJoined scenarios
 * - onActivityDeleted scenarios
 */
class StreakServiceTest {

  private lateinit var mockStreakRepository: GroupStreakRepository
  private lateinit var mockEventsRepository: EventsRepository
  private lateinit var mockSeriesRepository: SeriesRepository
  private lateinit var mockGroupRepository: GroupRepository

  private val testGroupId = "group123"
  private val testUserId = "user456"

  // Fixed timestamps for testing (Monday, Jan 6, 2025 at 10:00 UTC)
  private val mondayWeek1 = createTimestamp(2025, Calendar.JANUARY, 6, 10, 0)
  private val wednesdayWeek1 = createTimestamp(2025, Calendar.JANUARY, 8, 14, 30)
  private val sundayWeek1 = createTimestamp(2025, Calendar.JANUARY, 12, 23, 59)
  private val mondayWeek2 = createTimestamp(2025, Calendar.JANUARY, 13, 10, 0)
  private val fridayWeek2 = createTimestamp(2025, Calendar.JANUARY, 17, 16, 0)
  private val mondayWeek3 = createTimestamp(2025, Calendar.JANUARY, 20, 10, 0)
  private val mondayWeek5 = createTimestamp(2025, Calendar.FEBRUARY, 3, 10, 0) // Skipped week 4

  @Before
  fun setup() {
    mockStreakRepository = mockk(relaxed = true)
    mockEventsRepository = mockk(relaxed = true)
    mockSeriesRepository = mockk(relaxed = true)
    mockGroupRepository = mockk(relaxed = true)

    // Mock all providers
    mockkObject(GroupStreakRepositoryProvider)
    mockkObject(EventsRepositoryProvider)
    mockkObject(SeriesRepositoryProvider)
    mockkObject(GroupRepositoryProvider)

    every { GroupStreakRepositoryProvider.getRepository(any()) } returns mockStreakRepository
    every { EventsRepositoryProvider.getRepository(any(), any()) } returns mockEventsRepository
    every { SeriesRepositoryProvider.repository } returns mockSeriesRepository
    every { GroupRepositoryProvider.repository } returns mockGroupRepository

    // Default: empty group
    coEvery { mockGroupRepository.getGroup(testGroupId) } returns
        Group(id = testGroupId, eventIds = emptyList(), serieIds = emptyList())
  }

  @After
  fun tearDown() {
    unmockkAll()
    StreakService.resetClock()
  }

  // ==================== Week Boundary Tests ====================

  @Test
  fun `getWeekStart returns Monday 00-00 UTC for any day in the week`() {
    // Monday should return itself at 00:00
    val mondayResult = StreakService.getWeekStart(mondayWeek1)
    assertTimestampEquals(createTimestamp(2025, Calendar.JANUARY, 6, 0, 0), mondayResult)

    // Wednesday should return Monday
    val wednesdayResult = StreakService.getWeekStart(wednesdayWeek1)
    assertTimestampEquals(createTimestamp(2025, Calendar.JANUARY, 6, 0, 0), wednesdayResult)

    // Sunday should return Monday of same week
    val sundayResult = StreakService.getWeekStart(sundayWeek1)
    assertTimestampEquals(createTimestamp(2025, Calendar.JANUARY, 6, 0, 0), sundayResult)
  }

  @Test
  fun `isSameWeek returns true for dates in same week`() {
    assertTrue(StreakService.isSameWeek(mondayWeek1, wednesdayWeek1))
    assertTrue(StreakService.isSameWeek(mondayWeek1, sundayWeek1))
    assertTrue(StreakService.isSameWeek(wednesdayWeek1, sundayWeek1))
  }

  @Test
  fun `isSameWeek returns false for dates in different weeks`() {
    assertFalse(StreakService.isSameWeek(sundayWeek1, mondayWeek2))
    assertFalse(StreakService.isSameWeek(mondayWeek1, mondayWeek2))
  }

  @Test
  fun `isConsecutiveWeek returns true for adjacent weeks`() {
    assertTrue(StreakService.isConsecutiveWeek(mondayWeek1, mondayWeek2))
    assertTrue(StreakService.isConsecutiveWeek(mondayWeek2, mondayWeek3))
  }

  @Test
  fun `isConsecutiveWeek returns false for non-adjacent weeks`() {
    assertFalse(StreakService.isConsecutiveWeek(mondayWeek1, mondayWeek3)) // Skipped week 2
    assertFalse(StreakService.isConsecutiveWeek(mondayWeek3, mondayWeek5)) // Skipped week 4
  }

  // ==================== onActivityJoined Tests ====================

  @Test
  fun `onActivityJoined with null groupId does nothing`() = runTest {
    StreakService.onActivityJoined(null, testUserId, mondayWeek1)

    coVerify(exactly = 0) { mockStreakRepository.getStreakForUser(any(), any()) }
    coVerify(exactly = 0) { mockStreakRepository.updateStreak(any(), any(), any()) }
  }

  @Test
  fun `onActivityJoined creates new streak for first activity`() = runTest {
    coEvery { mockStreakRepository.getStreakForUser(testGroupId, testUserId) } returns null

    StreakService.onActivityJoined(testGroupId, testUserId, wednesdayWeek1)

    coVerify {
      mockStreakRepository.updateStreak(
          testGroupId,
          testUserId,
          match { streak ->
            streak.groupId == testGroupId &&
                streak.userId == testUserId &&
                streak.currentStreakWeeks == 1 &&
                streak.currentStreakActivities == 1 &&
                streak.bestStreakWeeks == 1 &&
                streak.bestStreakActivities == 1
          })
    }
  }

  @Test
  fun `onActivityJoined increments activities for same week`() = runTest {
    val existingStreak =
        GroupStreak(
            groupId = testGroupId,
            userId = testUserId,
            currentStreakWeeks = 1,
            currentStreakActivities = 2,
            currentStreakStartDate = StreakService.getWeekStart(mondayWeek1),
            lastActiveWeekStart = StreakService.getWeekStart(mondayWeek1),
            bestStreakWeeks = 1,
            bestStreakActivities = 2)
    coEvery { mockStreakRepository.getStreakForUser(testGroupId, testUserId) } returns
        existingStreak

    // Join another activity in the same week
    StreakService.onActivityJoined(testGroupId, testUserId, wednesdayWeek1)

    coVerify {
      mockStreakRepository.updateStreak(
          testGroupId,
          testUserId,
          match { streak ->
            streak.currentStreakWeeks == 1 && // Weeks unchanged
                streak.currentStreakActivities == 3 && // Activities incremented
                streak.bestStreakActivities == 3 // Best updated
          })
    }
  }

  @Test
  fun `onActivityJoined increments weeks for consecutive week`() = runTest {
    val existingStreak =
        GroupStreak(
            groupId = testGroupId,
            userId = testUserId,
            currentStreakWeeks = 1,
            currentStreakActivities = 2,
            currentStreakStartDate = StreakService.getWeekStart(mondayWeek1),
            lastActiveWeekStart = StreakService.getWeekStart(mondayWeek1),
            bestStreakWeeks = 1,
            bestStreakActivities = 2)
    coEvery { mockStreakRepository.getStreakForUser(testGroupId, testUserId) } returns
        existingStreak

    // Join activity in the next week
    StreakService.onActivityJoined(testGroupId, testUserId, fridayWeek2)

    coVerify {
      mockStreakRepository.updateStreak(
          testGroupId,
          testUserId,
          match { streak ->
            streak.currentStreakWeeks == 2 && // Weeks incremented
                streak.currentStreakActivities == 3 && // Activities incremented
                streak.bestStreakWeeks == 2 && // Best weeks updated
                streak.bestStreakActivities == 3 // Best activities updated
          })
    }
  }

  @Test
  fun `onActivityJoined resets streak when week is skipped`() = runTest {
    val existingStreak =
        GroupStreak(
            groupId = testGroupId,
            userId = testUserId,
            currentStreakWeeks = 3,
            currentStreakActivities = 5,
            currentStreakStartDate = StreakService.getWeekStart(mondayWeek1),
            lastActiveWeekStart = StreakService.getWeekStart(mondayWeek3),
            bestStreakWeeks = 3,
            bestStreakActivities = 5)
    coEvery { mockStreakRepository.getStreakForUser(testGroupId, testUserId) } returns
        existingStreak

    // Join activity after skipping a week (week 4)
    StreakService.onActivityJoined(testGroupId, testUserId, mondayWeek5)

    coVerify {
      mockStreakRepository.updateStreak(
          testGroupId,
          testUserId,
          match { streak ->
            streak.currentStreakWeeks == 1 && // Reset to 1
                streak.currentStreakActivities == 1 && // Reset to 1
                streak.bestStreakWeeks == 3 && // Best unchanged
                streak.bestStreakActivities == 5 // Best unchanged
          })
    }
  }

  // ==================== onActivityDeleted Tests ====================

  @Test
  fun `onActivityDeleted with null groupId does nothing`() = runTest {
    StreakService.onActivityDeleted(null, listOf(testUserId), mondayWeek1)

    coVerify(exactly = 0) { mockStreakRepository.getStreakForUser(any(), any()) }
  }

  @Test
  fun `onActivityDeleted with empty userIds does nothing`() = runTest {
    StreakService.onActivityDeleted(testGroupId, emptyList(), mondayWeek1)

    coVerify(exactly = 0) { mockStreakRepository.getStreakForUser(any(), any()) }
  }

  @Test
  fun `onActivityDeleted decrements activities when other activities exist in week`() = runTest {
    val existingStreak =
        GroupStreak(
            groupId = testGroupId,
            userId = testUserId,
            currentStreakWeeks = 1,
            currentStreakActivities = 3,
            currentStreakStartDate = StreakService.getWeekStart(mondayWeek1),
            lastActiveWeekStart = StreakService.getWeekStart(mondayWeek1),
            bestStreakWeeks = 1,
            bestStreakActivities = 3)
    coEvery { mockStreakRepository.getStreakForUser(testGroupId, testUserId) } returns
        existingStreak

    // Setup: user has another event in the same week
    val otherEvent = createTestEvent(testGroupId, testUserId, wednesdayWeek1)
    coEvery { mockGroupRepository.getGroup(testGroupId) } returns
        Group(id = testGroupId, eventIds = listOf("other-event"), serieIds = emptyList())
    coEvery { mockEventsRepository.getEventsByIds(listOf("other-event")) } returns
        listOf(otherEvent)

    // Delete an activity on Monday (but Wednesday activity still exists)
    StreakService.onActivityDeleted(testGroupId, listOf(testUserId), mondayWeek1)

    coVerify {
      mockStreakRepository.updateStreak(
          testGroupId,
          testUserId,
          match { streak ->
            streak.currentStreakWeeks == 1 && // Weeks unchanged
                streak.currentStreakActivities == 2 // Activities decremented
          })
    }
  }

  @Test
  fun `onActivityDeleted removes streak completely when last activity deleted`() = runTest {
    val existingStreak =
        GroupStreak(
            groupId = testGroupId,
            userId = testUserId,
            currentStreakWeeks = 1,
            currentStreakActivities = 1,
            currentStreakStartDate = StreakService.getWeekStart(mondayWeek1),
            lastActiveWeekStart = StreakService.getWeekStart(mondayWeek1),
            bestStreakWeeks = 1,
            bestStreakActivities = 1)
    coEvery { mockStreakRepository.getStreakForUser(testGroupId, testUserId) } returns
        existingStreak

    // No other activities exist
    coEvery { mockGroupRepository.getGroup(testGroupId) } returns
        Group(id = testGroupId, eventIds = emptyList(), serieIds = emptyList())

    StreakService.onActivityDeleted(testGroupId, listOf(testUserId), mondayWeek1)

    coVerify {
      mockStreakRepository.updateStreak(
          testGroupId,
          testUserId,
          match { streak ->
            streak.currentStreakWeeks == 0 &&
                streak.currentStreakActivities == 0 &&
                streak.currentStreakStartDate == null &&
                streak.lastActiveWeekStart == null
          })
    }
  }

  // ==================== Helper Functions ====================

  private fun createTimestamp(year: Int, month: Int, day: Int, hour: Int, minute: Int): Timestamp {
    val calendar =
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
          set(year, month, day, hour, minute, 0)
          set(Calendar.MILLISECOND, 0)
        }
    return Timestamp(calendar.time)
  }

  private fun assertTimestampEquals(expected: Timestamp, actual: Timestamp) {
    assertEquals("Timestamps should be equal", expected.toDate().time, actual.toDate().time)
  }

  private fun createTestEvent(groupId: String, participantId: String, date: Timestamp): Event {
    return Event(
        eventId = "event-${System.currentTimeMillis()}",
        type = EventType.ACTIVITY,
        title = "Test Event",
        description = "Test",
        location = null,
        date = date,
        duration = 60,
        participants = listOf(participantId),
        maxParticipants = 10,
        visibility = EventVisibility.PRIVATE,
        ownerId = participantId,
        groupId = groupId)
  }
}
