package com.android.joinme.model.presence

// Implemented with help of Claude AI

import com.android.joinme.model.event.Event
import com.android.joinme.model.event.EventType
import com.android.joinme.model.event.EventVisibility
import com.android.joinme.model.event.EventsRepositoryLocal
import com.android.joinme.model.event.EventsRepositoryProvider
import com.android.joinme.model.groups.Group
import com.android.joinme.model.groups.GroupRepositoryLocal
import com.android.joinme.model.groups.GroupRepositoryProvider
import com.android.joinme.model.map.Location
import com.google.firebase.Timestamp
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tests for JoinMeContextIdProvider.
 *
 * Verifies that the provider correctly fetches context IDs from groups and events.
 */
@RunWith(RobolectricTestRunner::class)
class JoinMeContextIdProviderTest {

  private lateinit var provider: JoinMeContextIdProvider
  private lateinit var groupRepo: GroupRepositoryLocal
  private lateinit var eventRepo: EventsRepositoryLocal

  private val testUserId = "testUser123"

  @Before
  fun setup() {
    provider = JoinMeContextIdProvider()
    // Get the local repositories that will be used in test environment
    groupRepo = GroupRepositoryProvider.repository as GroupRepositoryLocal
    eventRepo = EventsRepositoryProvider.getRepository(isOnline = false) as EventsRepositoryLocal
    groupRepo.clear()
    eventRepo.clear()
  }

  @Test
  fun getContextIdsForUser_blankUserId_returnsEmptyList() = runTest {
    val contextIds = provider.getContextIdsForUser("")
    assertTrue(contextIds.isEmpty())
  }

  @Test
  fun getContextIdsForUser_noGroupsOrEvents_returnsEmptyList() = runTest {
    val contextIds = provider.getContextIdsForUser(testUserId)
    assertTrue(contextIds.isEmpty())
  }

  @Test
  fun getContextIdsForUser_withGroups_returnsGroupIds() = runTest {
    // Add groups
    val group1 = createTestGroup("group1")
    val group2 = createTestGroup("group2")
    groupRepo.addGroup(group1)
    groupRepo.addGroup(group2)

    val contextIds = provider.getContextIdsForUser(testUserId)

    assertEquals(2, contextIds.size)
    assertTrue(contextIds.contains("group1"))
    assertTrue(contextIds.contains("group2"))
  }

  @Test
  fun getContextIdsForUser_withEvents_returnsEventIds() = runTest {
    // Add events
    val event1 = createTestEvent("event1")
    val event2 = createTestEvent("event2")
    eventRepo.addEvent(event1)
    eventRepo.addEvent(event2)

    val contextIds = provider.getContextIdsForUser(testUserId)

    assertEquals(2, contextIds.size)
    assertTrue(contextIds.contains("event1"))
    assertTrue(contextIds.contains("event2"))
  }

  @Test
  fun getContextIdsForUser_withGroupsAndEvents_returnsCombinedIds() = runTest {
    // Add groups
    val group1 = createTestGroup("group1")
    val group2 = createTestGroup("group2")
    groupRepo.addGroup(group1)
    groupRepo.addGroup(group2)

    // Add events
    val event1 = createTestEvent("event1")
    val event2 = createTestEvent("event2")
    eventRepo.addEvent(event1)
    eventRepo.addEvent(event2)

    val contextIds = provider.getContextIdsForUser(testUserId)

    assertEquals(4, contextIds.size)
    assertTrue(contextIds.containsAll(listOf("group1", "group2", "event1", "event2")))
  }

  private fun createTestGroup(id: String): Group {
    return Group(
        id = id,
        name = "Test Group $id",
        description = "Description",
        ownerId = testUserId,
        memberIds = listOf(testUserId))
  }

  private fun createTestEvent(id: String): Event {
    return Event(
        eventId = id,
        type = EventType.ACTIVITY,
        title = "Test Event $id",
        description = "Description",
        location = Location(0.0, 0.0, "Test Location"),
        date = Timestamp.now(),
        duration = 60,
        participants = listOf(testUserId),
        maxParticipants = 10,
        visibility = EventVisibility.PUBLIC,
        ownerId = testUserId)
  }
}
