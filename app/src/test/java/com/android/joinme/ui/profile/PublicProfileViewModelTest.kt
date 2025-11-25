package com.android.joinme.ui.profile

import com.android.joinme.model.event.Event
import com.android.joinme.model.event.EventType
import com.android.joinme.model.event.EventVisibility
import com.android.joinme.model.event.EventsRepository
import com.android.joinme.model.groups.Group
import com.android.joinme.model.groups.GroupRepository
import com.android.joinme.model.map.Location
import com.android.joinme.model.profile.Profile
import com.android.joinme.model.profile.ProfileRepository
import com.google.firebase.Timestamp
import io.mockk.*
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PublicProfileViewModelTest {

  private val testDispatcher = StandardTestDispatcher()

  private lateinit var mockProfileRepository: ProfileRepository
  private lateinit var mockEventsRepository: EventsRepository
  private lateinit var mockGroupRepository: GroupRepository
  private lateinit var viewModel: PublicProfileViewModel

  private val currentUserId = "current-user-id"
  private val otherUserId = "other-user-id"

  private val testProfile =
      Profile(
          uid = otherUserId,
          username = "OtherUser",
          email = "other@example.com",
          dateOfBirth = "15/03/1995",
          country = "Switzerland",
          interests = listOf("Coding", "Testing"),
          bio = "Test bio for other user",
          photoUrl = null,
          createdAt = Timestamp.now(),
          updatedAt = Timestamp.now(),
          eventsJoinedCount = 10,
          followersCount = 100,
          followingCount = 50)

  private val testEvent =
      Event(
          eventId = "event1",
          type = EventType.SPORTS,
          title = "Basketball Game",
          description = "Friendly game",
          location = Location(latitude = 46.5197, longitude = 6.6323, name = "Unil sports"),
          date = Timestamp(Date()),
          duration = 120,
          participants = listOf(currentUserId, otherUserId),
          maxParticipants = 10,
          visibility = EventVisibility.PUBLIC,
          ownerId = otherUserId,
          partOfASerie = false,
          groupId = null)

  private val testGroup =
      Group(
          id = "group1",
          name = "Running club",
          category = EventType.SPORTS,
          description = "Running group",
          ownerId = otherUserId,
          memberIds = listOf(currentUserId, otherUserId),
          eventIds = listOf("event1"),
          serieIds = emptyList(),
          photoUrl = null)

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    mockProfileRepository = mockk(relaxed = true)
    mockEventsRepository = mockk(relaxed = true)
    mockGroupRepository = mockk(relaxed = true)
    viewModel =
        PublicProfileViewModel(mockProfileRepository, mockEventsRepository, mockGroupRepository)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
    clearAllMocks()
  }

  // ==================== STATE MANAGEMENT TESTS ====================

  @Test
  fun `initial state is correct`() {
    assertNull(viewModel.profile.value)
    assertTrue(viewModel.commonEvents.value.isEmpty())
    assertTrue(viewModel.commonGroups.value.isEmpty())
    assertFalse(viewModel.isLoading.value)
    assertNull(viewModel.error.value)
  }

  @Test
  fun `clearError sets and clears error state`() {
    viewModel.loadPublicProfile("", currentUserId)
    testDispatcher.scheduler.advanceUntilIdle()

    assertEquals("Invalid user ID", viewModel.error.value)

    viewModel.clearError()
    testDispatcher.scheduler.advanceUntilIdle()

    assertNull(viewModel.error.value)
  }

  // ==================== VALIDATION TESTS ====================

  @Test
  fun `loadPublicProfile fails with invalid userId`() = runTest {
    // Test blank userId
    viewModel.loadPublicProfile("", currentUserId)
    testDispatcher.scheduler.advanceUntilIdle()
    assertEquals("Invalid user ID", viewModel.error.value)
    assertFalse(viewModel.isLoading.value)

    // Test whitespace userId
    viewModel.clearError()
    viewModel.loadPublicProfile("   ", currentUserId)
    testDispatcher.scheduler.advanceUntilIdle()
    assertEquals("Invalid user ID", viewModel.error.value)
    assertFalse(viewModel.isLoading.value)
  }

  @Test
  fun `loadPublicProfile fails with invalid currentUserId`() = runTest {
    // Test null currentUserId
    viewModel.loadPublicProfile(otherUserId, null)
    testDispatcher.scheduler.advanceUntilIdle()
    assertEquals("Not authenticated. Please sign in.", viewModel.error.value)
    assertFalse(viewModel.isLoading.value)

    // Test blank currentUserId
    viewModel.clearError()
    viewModel.loadPublicProfile(otherUserId, "")
    testDispatcher.scheduler.advanceUntilIdle()
    assertEquals("Not authenticated. Please sign in.", viewModel.error.value)
    assertFalse(viewModel.isLoading.value)
  }

  @Test
  fun `loadPublicProfile fails when currentUserId equals userId`() = runTest {
    viewModel.loadPublicProfile(currentUserId, currentUserId)
    testDispatcher.scheduler.advanceUntilIdle()

    assertEquals("Cannot view your own profile here", viewModel.error.value)
    assertFalse(viewModel.isLoading.value)
    assertNull(viewModel.profile.value)
  }

  // ==================== SUCCESS CASES TESTS ====================

  @Test
  fun `loadPublicProfile successfully loads profile with events and groups`() = runTest {
    coEvery { mockProfileRepository.getProfile(otherUserId) } returns testProfile
    coEvery { mockEventsRepository.getCommonEvents(any()) } returns listOf(testEvent)
    coEvery { mockGroupRepository.getCommonGroups(any()) } returns listOf(testGroup)

    viewModel.loadPublicProfile(otherUserId, currentUserId)
    testDispatcher.scheduler.advanceUntilIdle()

    assertEquals(testProfile, viewModel.profile.value)
    assertEquals(1, viewModel.commonEvents.value.size)
    assertEquals(testEvent, viewModel.commonEvents.value[0])
    assertEquals(1, viewModel.commonGroups.value.size)
    assertEquals(testGroup, viewModel.commonGroups.value[0])
    assertFalse(viewModel.isLoading.value)
    assertNull(viewModel.error.value)

    coVerify { mockProfileRepository.getProfile(otherUserId) }
    coVerify { mockEventsRepository.getCommonEvents(listOf(currentUserId, otherUserId)) }
    coVerify { mockGroupRepository.getCommonGroups(listOf(currentUserId, otherUserId)) }
  }

  @Test
  fun `loadPublicProfile sets loading state correctly during operation`() = runTest {
    coEvery { mockProfileRepository.getProfile(otherUserId) } coAnswers
        {
          // Check loading is true during operation
          assertTrue(viewModel.isLoading.value)
          testProfile
        }
    coEvery { mockEventsRepository.getCommonEvents(any()) } returns emptyList()
    coEvery { mockGroupRepository.getCommonGroups(any()) } returns emptyList()

    assertFalse(viewModel.isLoading.value)

    viewModel.loadPublicProfile(otherUserId, currentUserId)
    testDispatcher.scheduler.advanceUntilIdle()

    assertFalse(viewModel.isLoading.value)
  }

  @Test
  fun `loadPublicProfile successfully loads profile with empty events and groups`() = runTest {
    coEvery { mockProfileRepository.getProfile(otherUserId) } returns testProfile
    coEvery { mockEventsRepository.getCommonEvents(any()) } returns emptyList()
    coEvery { mockGroupRepository.getCommonGroups(any()) } returns emptyList()

    viewModel.loadPublicProfile(otherUserId, currentUserId)
    testDispatcher.scheduler.advanceUntilIdle()

    assertEquals(testProfile, viewModel.profile.value)
    assertTrue(viewModel.commonEvents.value.isEmpty())
    assertTrue(viewModel.commonGroups.value.isEmpty())
    assertFalse(viewModel.isLoading.value)
    assertNull(viewModel.error.value)
  }

  @Test
  fun `loadPublicProfile clears previous error before loading`() = runTest {
    // First load with error
    viewModel.loadPublicProfile("", currentUserId)
    testDispatcher.scheduler.advanceUntilIdle()
    assertNotNull(viewModel.error.value)

    // Second load should clear error
    coEvery { mockProfileRepository.getProfile(otherUserId) } returns testProfile
    coEvery { mockEventsRepository.getCommonEvents(any()) } returns emptyList()
    coEvery { mockGroupRepository.getCommonGroups(any()) } returns emptyList()

    viewModel.loadPublicProfile(otherUserId, currentUserId)
    testDispatcher.scheduler.advanceUntilIdle()

    assertNull(viewModel.error.value)
  }

  // ==================== ERROR HANDLING TESTS ====================

  @Test
  fun `loadPublicProfile fails when profile is not found`() = runTest {
    coEvery { mockProfileRepository.getProfile(otherUserId) } returns null

    viewModel.loadPublicProfile(otherUserId, currentUserId)
    testDispatcher.scheduler.advanceUntilIdle()

    assertEquals("Profile not found", viewModel.error.value)
    assertFalse(viewModel.isLoading.value)
    assertNull(viewModel.profile.value)

    coVerify(exactly = 0) { mockEventsRepository.getCommonEvents(any()) }
    coVerify(exactly = 0) { mockGroupRepository.getCommonGroups(any()) }
  }

  @Test
  fun `loadPublicProfile handles exception when fetching profile`() = runTest {
    coEvery { mockProfileRepository.getProfile(otherUserId) } throws Exception("Network error")

    viewModel.loadPublicProfile(otherUserId, currentUserId)
    testDispatcher.scheduler.advanceUntilIdle()

    assertEquals("Profile not found", viewModel.error.value)
    assertFalse(viewModel.isLoading.value)
    assertNull(viewModel.profile.value)
  }

  @Test
  fun `loadPublicProfile continues when fetching events fails`() = runTest {
    coEvery { mockProfileRepository.getProfile(otherUserId) } returns testProfile
    coEvery { mockEventsRepository.getCommonEvents(any()) } throws Exception("Events error")
    coEvery { mockGroupRepository.getCommonGroups(any()) } returns listOf(testGroup)

    viewModel.loadPublicProfile(otherUserId, currentUserId)
    testDispatcher.scheduler.advanceUntilIdle()

    assertEquals(testProfile, viewModel.profile.value)
    assertTrue(viewModel.commonEvents.value.isEmpty())
    assertEquals(1, viewModel.commonGroups.value.size)
    assertFalse(viewModel.isLoading.value)
    assertNull(viewModel.error.value)
  }

  @Test
  fun `loadPublicProfile continues when fetching groups fails`() = runTest {
    coEvery { mockProfileRepository.getProfile(otherUserId) } returns testProfile
    coEvery { mockEventsRepository.getCommonEvents(any()) } returns listOf(testEvent)
    coEvery { mockGroupRepository.getCommonGroups(any()) } throws Exception("Groups error")

    viewModel.loadPublicProfile(otherUserId, currentUserId)
    testDispatcher.scheduler.advanceUntilIdle()

    assertEquals(testProfile, viewModel.profile.value)
    assertEquals(1, viewModel.commonEvents.value.size)
    assertTrue(viewModel.commonGroups.value.isEmpty())
    assertFalse(viewModel.isLoading.value)
    assertNull(viewModel.error.value)
  }

  @Test
  fun `loadPublicProfile continues when both events and groups fail`() = runTest {
    coEvery { mockProfileRepository.getProfile(otherUserId) } returns testProfile
    coEvery { mockEventsRepository.getCommonEvents(any()) } throws Exception("Events error")
    coEvery { mockGroupRepository.getCommonGroups(any()) } throws Exception("Groups error")

    viewModel.loadPublicProfile(otherUserId, currentUserId)
    testDispatcher.scheduler.advanceUntilIdle()

    assertEquals(testProfile, viewModel.profile.value)
    assertTrue(viewModel.commonEvents.value.isEmpty())
    assertTrue(viewModel.commonGroups.value.isEmpty())
    assertFalse(viewModel.isLoading.value)
    assertNull(viewModel.error.value)
  }

  // ==================== EDGE CASES ====================

  @Test
  fun `loadPublicProfile handles profile with null bio`() = runTest {
    val profileWithNullBio = testProfile.copy(bio = null)
    coEvery { mockProfileRepository.getProfile(otherUserId) } returns profileWithNullBio
    coEvery { mockEventsRepository.getCommonEvents(any()) } returns emptyList()
    coEvery { mockGroupRepository.getCommonGroups(any()) } returns emptyList()

    viewModel.loadPublicProfile(otherUserId, currentUserId)
    testDispatcher.scheduler.advanceUntilIdle()

    assertEquals(profileWithNullBio, viewModel.profile.value)
    assertNull(viewModel.profile.value?.bio)
  }

  @Test
  fun `loadPublicProfile handles profile with empty interests`() = runTest {
    val profileWithEmptyInterests = testProfile.copy(interests = emptyList())
    coEvery { mockProfileRepository.getProfile(otherUserId) } returns profileWithEmptyInterests
    coEvery { mockEventsRepository.getCommonEvents(any()) } returns emptyList()
    coEvery { mockGroupRepository.getCommonGroups(any()) } returns emptyList()

    viewModel.loadPublicProfile(otherUserId, currentUserId)
    testDispatcher.scheduler.advanceUntilIdle()

    assertEquals(profileWithEmptyInterests, viewModel.profile.value)
    assertTrue(viewModel.profile.value?.interests?.isEmpty() == true)
  }

  @Test
  fun `loadPublicProfile handles large number of common events and groups`() = runTest {
    val manyEvents = List(50) { index -> testEvent.copy(eventId = "event$index") }
    val manyGroups = List(50) { index -> testGroup.copy(id = "group$index") }
    coEvery { mockProfileRepository.getProfile(otherUserId) } returns testProfile
    coEvery { mockEventsRepository.getCommonEvents(any()) } returns manyEvents
    coEvery { mockGroupRepository.getCommonGroups(any()) } returns manyGroups

    viewModel.loadPublicProfile(otherUserId, currentUserId)
    testDispatcher.scheduler.advanceUntilIdle()

    assertEquals(50, viewModel.commonEvents.value.size)
    assertEquals(50, viewModel.commonGroups.value.size)
  }

  @Test
  fun `loadPublicProfile passes correct user IDs to repository methods`() = runTest {
    coEvery { mockProfileRepository.getProfile(otherUserId) } returns testProfile
    coEvery { mockEventsRepository.getCommonEvents(any()) } returns emptyList()
    coEvery { mockGroupRepository.getCommonGroups(any()) } returns emptyList()

    viewModel.loadPublicProfile(otherUserId, currentUserId)
    testDispatcher.scheduler.advanceUntilIdle()

    coVerify { mockEventsRepository.getCommonEvents(listOf(currentUserId, otherUserId)) }
    coVerify { mockGroupRepository.getCommonGroups(listOf(currentUserId, otherUserId)) }
  }

  @Test
  fun `multiple loadPublicProfile calls handle state correctly`() = runTest {
    coEvery { mockProfileRepository.getProfile(any()) } returns testProfile
    coEvery { mockEventsRepository.getCommonEvents(any()) } returns emptyList()
    coEvery { mockGroupRepository.getCommonGroups(any()) } returns emptyList()

    // First call
    viewModel.loadPublicProfile(otherUserId, currentUserId)
    testDispatcher.scheduler.advanceUntilIdle()
    assertEquals(testProfile, viewModel.profile.value)

    // Second call with different userId
    val anotherUserId = "another-user-id"
    val anotherProfile = testProfile.copy(uid = anotherUserId, username = "AnotherUser")
    coEvery { mockProfileRepository.getProfile(anotherUserId) } returns anotherProfile

    viewModel.loadPublicProfile(anotherUserId, currentUserId)
    testDispatcher.scheduler.advanceUntilIdle()

    assertEquals(anotherProfile, viewModel.profile.value)
    assertFalse(viewModel.isLoading.value)
  }
}
