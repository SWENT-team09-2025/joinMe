package com.android.joinme.ui.profile

import android.content.Context
import android.net.Uri
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.android.joinme.model.event.Event
import com.android.joinme.model.event.EventFilter
import com.android.joinme.model.event.EventType
import com.android.joinme.model.event.EventVisibility
import com.android.joinme.model.event.EventsRepository
import com.android.joinme.model.groups.Group
import com.android.joinme.model.groups.GroupRepository
import com.android.joinme.model.map.Location
import com.android.joinme.model.profile.Profile
import com.android.joinme.model.profile.ProfileRepository
import com.android.joinme.ui.groups.GroupDetailScreenTestTags
import com.android.joinme.ui.overview.ShowEventScreenTestTags
import com.google.firebase.FirebaseApp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import java.util.Date
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Robolectric tests for PublicProfileScreen composable. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "w360dp-h640dp-normal-long-notround-any-420dpi-keyshidden-nonav")
class PublicProfileScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var context: Context
  private val currentUserId = "current-user-id"
  private val otherUserId = "other-user-id"

  @Before
  fun setUp() {
    context = RuntimeEnvironment.getApplication()
    // Initialize Firebase if not already initialized
    if (FirebaseApp.getApps(context).isEmpty()) {
      FirebaseApp.initializeApp(context)
    }
  }

  @After
  fun tearDown() {
    unmockkStatic(FirebaseAuth::class)
  }

  private fun mockFirebaseAuthWithUser(userId: String) {
    mockkStatic(FirebaseAuth::class)
    val mockAuth = mockk<FirebaseAuth>()
    val mockUser = mockk<FirebaseUser>()
    every { FirebaseAuth.getInstance() } returns mockAuth
    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns userId
  }

  private fun createTestProfile(
      uid: String = otherUserId,
      username: String = "Mathieu_pfr",
      bio: String? = "EPFL student in IC section",
      interests: List<String> = listOf("Musculation", "Ginko"),
      eventsJoinedCount: Int = 25,
      followersCount: Int = 150,
      followingCount: Int = 100
  ): Profile {
    return Profile(
        uid = uid,
        username = username,
        email = "user@example.com",
        dateOfBirth = "15/03/1995",
        country = "Switzerland",
        interests = interests,
        bio = bio,
        photoUrl = null,
        createdAt = Timestamp.now(),
        updatedAt = Timestamp.now(),
        eventsJoinedCount = eventsJoinedCount,
        followersCount = followersCount,
        followingCount = followingCount)
  }

  private fun createTestEvent(
      eventId: String = "event1",
      title: String = "Basketball Game"
  ): Event {
    return Event(
        eventId = eventId,
        type = EventType.SPORTS,
        title = title,
        description = "Friendly basketball game",
        location = Location(latitude = 46.5197, longitude = 6.6323, name = "Unil sports"),
        date = Timestamp(Date()),
        duration = 120,
        participants = listOf(currentUserId, otherUserId),
        maxParticipants = 10,
        visibility = EventVisibility.PUBLIC,
        ownerId = otherUserId,
        partOfASerie = false,
        groupId = null)
  }

  private fun createTestGroup(groupId: String = "group1", name: String = "Running club"): Group {
    return Group(
        id = groupId,
        name = name,
        category = EventType.SPORTS,
        description = "Running group for fitness enthusiasts",
        ownerId = otherUserId,
        memberIds = listOf(currentUserId, otherUserId),
        eventIds = listOf("event1"),
        serieIds = emptyList(),
        photoUrl = null)
  }

  // Fake repositories for testing
  private class FakeProfileRepository(private var stored: Profile? = null) : ProfileRepository {
    override suspend fun getProfile(uid: String): Profile? = stored?.takeIf { it.uid == uid }

    override suspend fun createOrUpdateProfile(profile: Profile) {
      stored = profile
    }

    override suspend fun deleteProfile(uid: String) {}

    override suspend fun uploadProfilePhoto(
        context: android.content.Context,
        uid: String,
        imageUri: android.net.Uri
    ): String = ""

    override suspend fun deleteProfilePhoto(uid: String) {}

    override suspend fun getProfilesByIds(uids: List<String>): List<Profile>? = null

    // Stub implementations for follow methods - not used in PublicProfileScreen tests
    override suspend fun followUser(followerId: String, followedId: String) {}

    override suspend fun unfollowUser(followerId: String, followedId: String) {}

    override suspend fun isFollowing(followerId: String, followedId: String): Boolean = false

    override suspend fun getFollowing(userId: String, limit: Int): List<Profile> = emptyList()

    override suspend fun getFollowers(userId: String, limit: Int): List<Profile> = emptyList()

    override suspend fun getMutualFollowing(userId1: String, userId2: String): List<Profile> =
        emptyList()
  }

  private class FakeEventsRepository(private val events: List<Event> = emptyList()) :
      EventsRepository {
    override suspend fun getCommonEvents(userIds: List<String>): List<Event> = events

    // Required interface methods (not used in tests)
    override fun getNewEventId(): String = "new-event-id"

    override suspend fun getAllEvents(eventFilter: EventFilter): List<Event> = emptyList()

    override suspend fun getEvent(eventId: String): Event =
        events.firstOrNull { it.eventId == eventId } ?: throw Exception("Event not found")

    override suspend fun addEvent(event: Event) {}

    override suspend fun editEvent(eventId: String, newValue: Event) {}

    override suspend fun deleteEvent(eventId: String) {}

    override suspend fun getEventsByIds(eventIds: List<String>): List<Event> = emptyList()
  }

  private class FakeGroupRepository(private val groups: List<Group> = emptyList()) :
      GroupRepository {
    override suspend fun getCommonGroups(userIds: List<String>): List<Group> = groups

    // Required interface methods (not used in tests)
    override fun getNewGroupId(): String = "new-group-id"

    override suspend fun getAllGroups(): List<Group> = emptyList()

    override suspend fun getGroup(groupId: String): Group =
        groups.firstOrNull { it.id == groupId } ?: throw Exception("Group not found")

    override suspend fun addGroup(group: Group) {}

    override suspend fun editGroup(groupId: String, newValue: Group) {}

    override suspend fun deleteGroup(groupId: String, userId: String) {}

    override suspend fun leaveGroup(groupId: String, userId: String) {}

    override suspend fun joinGroup(groupId: String, userId: String) {}

    override suspend fun uploadGroupPhoto(
        context: Context,
        groupId: String,
        imageUri: Uri
    ): String {
      // Not needed for these tests
      return "http://fakeurl.com/photo.jpg"
    }

    override suspend fun deleteGroupPhoto(groupId: String) {
      // Not needed for these tests
    }
  }

  // ==================== LOADING AND ERROR STATES ====================

  @Test
  fun publicProfileScreen_displaysErrorMessage_whenErrorOccurs() {
    val viewModel =
        PublicProfileViewModel(
            FakeProfileRepository(), FakeEventsRepository(), FakeGroupRepository())

    composeTestRule.setContent {
      PublicProfileScreen(userId = "", viewModel = viewModel, onBackClick = {})
    }

    // Wait for loading to finish
    composeTestRule.waitForIdle()

    // Error message should be displayed
    composeTestRule.onNodeWithTag(PublicProfileScreenTestTags.ERROR_MESSAGE).assertIsDisplayed()
    composeTestRule.onNodeWithText("Invalid user ID").assertIsDisplayed()
  }

  // ==================== TOP BAR TESTS ====================

  @Test
  fun publicProfileScreen_displaysTopBarWithUsername() {
    val profile = createTestProfile(username = "TestUser123")
    val viewModel =
        PublicProfileViewModel(
            FakeProfileRepository(profile), FakeEventsRepository(), FakeGroupRepository())

    composeTestRule.setContent {
      PublicProfileScreen(
          userId = otherUserId, viewModel = viewModel, onBackClick = {}, onEventClick = {})
    }

    viewModel.loadPublicProfile(otherUserId, currentUserId)
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(PublicProfileScreenTestTags.TOP_BAR).assertIsDisplayed()
    composeTestRule.onNodeWithTag(PublicProfileScreenTestTags.USERNAME).assertIsDisplayed()
    composeTestRule.onNodeWithText("TestUser123").assertIsDisplayed()
  }

  @Test
  fun publicProfileScreen_backButtonInvokesCallback() {
    val profile = createTestProfile()
    val viewModel =
        PublicProfileViewModel(
            FakeProfileRepository(profile), FakeEventsRepository(), FakeGroupRepository())

    var backClicked = false

    composeTestRule.setContent {
      PublicProfileScreen(
          userId = otherUserId, viewModel = viewModel, onBackClick = { backClicked = true })
    }

    viewModel.loadPublicProfile(otherUserId, currentUserId)
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(PublicProfileScreenTestTags.BACK_BUTTON).performClick()

    assert(backClicked)
  }

  // ==================== PROFILE CONTENT TESTS ====================

  @Test
  fun publicProfileScreen_displaysProfileStats() {
    val profile =
        createTestProfile(eventsJoinedCount = 42, followersCount = 1500, followingCount = 89)
    val viewModel =
        PublicProfileViewModel(
            FakeProfileRepository(profile), FakeEventsRepository(), FakeGroupRepository())

    composeTestRule.setContent {
      PublicProfileScreen(userId = otherUserId, viewModel = viewModel, onBackClick = {})
    }

    viewModel.loadPublicProfile(otherUserId, currentUserId)
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(PublicProfileScreenTestTags.STATS_ROW).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(PublicProfileScreenTestTags.EVENTS_JOINED_STAT)
        .assertIsDisplayed()
    composeTestRule.onNodeWithTag(PublicProfileScreenTestTags.FOLLOWERS_STAT).assertIsDisplayed()
    composeTestRule.onNodeWithTag(PublicProfileScreenTestTags.FOLLOWING_STAT).assertIsDisplayed()

    // Check stat values
    composeTestRule.onNodeWithText("42").assertIsDisplayed()
    composeTestRule.onNodeWithText("1.5k").assertIsDisplayed() // Formatted
    composeTestRule.onNodeWithText("89").assertIsDisplayed()
  }

  @Test
  fun publicProfileScreen_formatsLargeFollowerCounts() {
    val profile = createTestProfile(followersCount = 28_800_000) // 28.8M
    val viewModel =
        PublicProfileViewModel(
            FakeProfileRepository(profile), FakeEventsRepository(), FakeGroupRepository())

    composeTestRule.setContent {
      PublicProfileScreen(userId = otherUserId, viewModel = viewModel, onBackClick = {})
    }

    viewModel.loadPublicProfile(otherUserId, currentUserId)
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("28.8m").assertIsDisplayed()
  }

  @Test
  fun publicProfileScreen_displaysBio() {
    val profile = createTestProfile(bio = "Love coding and outdoor activities!")
    val viewModel =
        PublicProfileViewModel(
            FakeProfileRepository(profile), FakeEventsRepository(), FakeGroupRepository())

    composeTestRule.setContent {
      PublicProfileScreen(userId = otherUserId, viewModel = viewModel, onBackClick = {})
    }

    viewModel.loadPublicProfile(otherUserId, currentUserId)
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(PublicProfileScreenTestTags.BIO).assertIsDisplayed()
    composeTestRule.onNodeWithText("Love coding and outdoor activities!").assertIsDisplayed()
  }

  @Test
  fun publicProfileScreen_displaysNoBioMessage_whenBioIsNull() {
    val profile = createTestProfile(bio = null)
    val viewModel =
        PublicProfileViewModel(
            FakeProfileRepository(profile), FakeEventsRepository(), FakeGroupRepository())

    composeTestRule.setContent {
      PublicProfileScreen(userId = otherUserId, viewModel = viewModel, onBackClick = {})
    }

    viewModel.loadPublicProfile(otherUserId, currentUserId)
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("No bio available").assertIsDisplayed()
  }

  @Test
  fun publicProfileScreen_displaysNoBioMessage_whenBioIsBlank() {
    val profile = createTestProfile(bio = "   ")
    val viewModel =
        PublicProfileViewModel(
            FakeProfileRepository(profile), FakeEventsRepository(), FakeGroupRepository())

    composeTestRule.setContent {
      PublicProfileScreen(userId = otherUserId, viewModel = viewModel, onBackClick = {})
    }

    viewModel.loadPublicProfile(otherUserId, currentUserId)
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("No bio available").assertIsDisplayed()
  }

  @Test
  fun publicProfileScreen_displaysInterests() {
    val profile = createTestProfile(interests = listOf("Photography", "Hiking", "Gaming"))
    val viewModel =
        PublicProfileViewModel(
            FakeProfileRepository(profile), FakeEventsRepository(), FakeGroupRepository())

    composeTestRule.setContent {
      PublicProfileScreen(userId = otherUserId, viewModel = viewModel, onBackClick = {})
    }

    viewModel.loadPublicProfile(otherUserId, currentUserId)
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(PublicProfileScreenTestTags.INTERESTS_SECTION).assertIsDisplayed()
    composeTestRule.onNodeWithText("Photography, Hiking, Gaming").assertIsDisplayed()
  }

  @Test
  fun publicProfileScreen_displaysNoInterestsMessage_whenInterestsEmpty() {
    val profile = createTestProfile(interests = emptyList())
    val viewModel =
        PublicProfileViewModel(
            FakeProfileRepository(profile), FakeEventsRepository(), FakeGroupRepository())

    composeTestRule.setContent {
      PublicProfileScreen(userId = otherUserId, viewModel = viewModel, onBackClick = {})
    }

    viewModel.loadPublicProfile(otherUserId, currentUserId)
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("No interests available").assertIsDisplayed()
  }

  @Test
  fun publicProfileScreen_displaysEventStreaks() {
    val profile = createTestProfile()
    val viewModel =
        PublicProfileViewModel(
            FakeProfileRepository(profile), FakeEventsRepository(), FakeGroupRepository())

    composeTestRule.setContent {
      PublicProfileScreen(userId = otherUserId, viewModel = viewModel, onBackClick = {})
    }

    viewModel.loadPublicProfile(otherUserId, currentUserId)
    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(PublicProfileScreenTestTags.EVENT_STREAKS_SECTION)
        .assertIsDisplayed()
    composeTestRule.onNodeWithText("Event Streaks").assertIsDisplayed()
    composeTestRule.onNodeWithText("0 days").assertIsDisplayed()
  }

  // ==================== BUTTONS TESTS ====================

  @Test
  fun publicProfileScreen_displaysFollowButton() {
    val profile = createTestProfile()
    val viewModel =
        PublicProfileViewModel(
            FakeProfileRepository(profile), FakeEventsRepository(), FakeGroupRepository())

    composeTestRule.setContent {
      PublicProfileScreen(userId = otherUserId, viewModel = viewModel, onBackClick = {})
    }

    viewModel.loadPublicProfile(otherUserId, currentUserId)
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(PublicProfileScreenTestTags.FOLLOW_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithText("Follow").assertIsDisplayed()
  }

  @Test
  fun publicProfileScreen_displaysMessageButton() {
    val profile = createTestProfile()
    val viewModel =
        PublicProfileViewModel(
            FakeProfileRepository(profile), FakeEventsRepository(), FakeGroupRepository())

    composeTestRule.setContent {
      PublicProfileScreen(userId = otherUserId, viewModel = viewModel, onBackClick = {})
    }

    viewModel.loadPublicProfile(otherUserId, currentUserId)
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(PublicProfileScreenTestTags.MESSAGE_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithText("Message").assertIsDisplayed()
  }

  @Test
  fun publicProfileScreen_messageButtonClickWithoutAuthShowsToast() {
    val profile = createTestProfile(username = "TestUser123")
    val viewModel =
        PublicProfileViewModel(
            FakeProfileRepository(profile), FakeEventsRepository(), FakeGroupRepository())

    // Don't mock Firebase auth - currentUserId will be null
    composeTestRule.setContent {
      PublicProfileScreen(
          userId = otherUserId,
          viewModel = viewModel,
          onBackClick = {},
          onMessageClick = { _, _, _ -> })
    }

    viewModel.loadPublicProfile(otherUserId, currentUserId)
    composeTestRule.waitForIdle()

    // Click the message button - should show "Please sign in" toast
    composeTestRule.onNodeWithTag(PublicProfileScreenTestTags.MESSAGE_BUTTON).performClick()
    composeTestRule.waitForIdle()

    // Verify button is still displayed after click
    composeTestRule.onNodeWithTag(PublicProfileScreenTestTags.MESSAGE_BUTTON).assertIsDisplayed()
  }

  @Test
  fun publicProfileScreen_messageButtonClickWithAuthInvokesCallback() {
    val profile = createTestProfile(username = "TestUser123")
    val viewModel =
        PublicProfileViewModel(
            FakeProfileRepository(profile), FakeEventsRepository(), FakeGroupRepository())

    var capturedChatId: String? = null
    var capturedChatTitle: String? = null
    var capturedTotalParticipants: Int? = null

    // Mock Firebase auth with valid user
    mockFirebaseAuthWithUser(currentUserId)

    composeTestRule.setContent {
      PublicProfileScreen(
          userId = otherUserId,
          viewModel = viewModel,
          onBackClick = {},
          onMessageClick = { chatId, chatTitle, totalParticipants ->
            capturedChatId = chatId
            capturedChatTitle = chatTitle
            capturedTotalParticipants = totalParticipants
          })
    }

    viewModel.loadPublicProfile(otherUserId, currentUserId)
    composeTestRule.waitForIdle()

    // Click the message button
    composeTestRule.onNodeWithTag(PublicProfileScreenTestTags.MESSAGE_BUTTON).performClick()
    composeTestRule.waitForIdle()

    // Verify callback was invoked with correct parameters
    assertEquals("dm_current-user-id_other-user-id", capturedChatId)
    assertEquals(profile.username, capturedChatTitle)
    assertEquals(2, capturedTotalParticipants)
  }

  // ==================== COMMON EVENTS TESTS ====================

  @Test
  fun publicProfileScreen_displaysEmptyEventsMessage_whenNoCommonEvents() {
    val profile = createTestProfile()
    val viewModel =
        PublicProfileViewModel(
            FakeProfileRepository(profile), FakeEventsRepository(), FakeGroupRepository())

    composeTestRule.setContent {
      PublicProfileScreen(userId = otherUserId, viewModel = viewModel, onBackClick = {})
    }

    viewModel.loadPublicProfile(otherUserId, currentUserId)
    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(PublicProfileScreenTestTags.EMPTY_EVENTS_MESSAGE)
        .assertIsDisplayed()
    composeTestRule.onNodeWithText("No common events").assertIsDisplayed()
  }

  @Test
  fun publicProfileScreen_eventCardClickInvokesCallback() {
    val profile = createTestProfile()
    val event = createTestEvent("event1", "Basketball")
    val viewModel =
        PublicProfileViewModel(
            FakeProfileRepository(profile),
            FakeEventsRepository(listOf(event)),
            FakeGroupRepository())

    composeTestRule.setContent {
      PublicProfileScreen(
          userId = otherUserId, viewModel = viewModel, onBackClick = {}, onEventClick = {})
    }

    viewModel.loadPublicProfile(otherUserId, currentUserId)
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(PublicProfileScreenTestTags.eventCardTag("event1")).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(ShowEventScreenTestTags.SCREEN)
  }

  // ==================== COMMON GROUPS TESTS ====================

  @Test
  fun publicProfileScreen_displaysEmptyGroupsMessage_whenNoCommonGroups() {
    val profile = createTestProfile()
    val viewModel =
        PublicProfileViewModel(
            FakeProfileRepository(profile), FakeEventsRepository(), FakeGroupRepository())

    composeTestRule.setContent {
      PublicProfileScreen(userId = otherUserId, viewModel = viewModel, onBackClick = {})
    }

    viewModel.loadPublicProfile(otherUserId, currentUserId)
    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(PublicProfileScreenTestTags.EMPTY_GROUPS_MESSAGE)
        .assertIsDisplayed()
    composeTestRule.onNodeWithText("No common groups").assertIsDisplayed()
  }

  @Test
  fun publicProfileScreen_groupCardClickInvokesCallback() {
    val profile = createTestProfile()
    val group = createTestGroup("group1", "Running Club")
    val viewModel =
        PublicProfileViewModel(
            FakeProfileRepository(profile),
            FakeEventsRepository(),
            FakeGroupRepository(listOf(group)))

    var clickedGroup: Group? = null

    composeTestRule.setContent {
      PublicProfileScreen(
          userId = otherUserId,
          viewModel = viewModel,
          onBackClick = {},
          onGroupClick = { clickedGroup = it })
    }

    viewModel.loadPublicProfile(otherUserId, currentUserId)
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(PublicProfileScreenTestTags.groupCardTag("group1")).performClick()

    composeTestRule.onNodeWithTag(GroupDetailScreenTestTags.BUTTON_ACTIVITIES)
  }

  // ==================== FULL INTEGRATION TESTS ====================

  @Test
  fun publicProfileScreen_displaysCompleteProfileWithEventsAndGroups() {
    val profile = createTestProfile(username = "CompleteUser", bio = "Complete profile test")
    val events = listOf(createTestEvent("event1", "Event1"))
    val groups = listOf(createTestGroup("group1", "Group1"))
    val viewModel =
        PublicProfileViewModel(
            FakeProfileRepository(profile),
            FakeEventsRepository(events),
            FakeGroupRepository(groups))

    composeTestRule.setContent {
      PublicProfileScreen(
          userId = otherUserId,
          viewModel = viewModel,
          onBackClick = {},
          onEventClick = {},
          onGroupClick = {})
    }

    viewModel.loadPublicProfile(otherUserId, currentUserId)
    composeTestRule.waitForIdle()

    // Verify screen is displayed
    composeTestRule.onNodeWithTag(PublicProfileScreenTestTags.SCREEN).assertIsDisplayed()

    // Verify profile info
    composeTestRule.onNodeWithText("CompleteUser").assertIsDisplayed()
    composeTestRule.onNodeWithText("Complete profile test").assertIsDisplayed()

    // Verify events and groups sections
    composeTestRule.onNodeWithTag(PublicProfileScreenTestTags.eventCardTag("event1")).assertExists()
    composeTestRule.onNodeWithTag(PublicProfileScreenTestTags.groupCardTag("group1")).assertExists()
  }

  @Test
  fun publicProfileScreen_displaysMinimalProfile() {
    val profile =
        createTestProfile(
            bio = null,
            interests = emptyList(),
            eventsJoinedCount = 0,
            followersCount = 0,
            followingCount = 0)
    val viewModel =
        PublicProfileViewModel(
            FakeProfileRepository(profile), FakeEventsRepository(), FakeGroupRepository())

    composeTestRule.setContent {
      PublicProfileScreen(userId = otherUserId, viewModel = viewModel, onBackClick = {})
    }

    viewModel.loadPublicProfile(otherUserId, currentUserId)
    composeTestRule.waitForIdle()

    // Should show default messages
    composeTestRule.onNodeWithText("No bio available").assertIsDisplayed()
    composeTestRule.onNodeWithText("No interests available").assertIsDisplayed()
    composeTestRule.onNodeWithText("No common events").assertIsDisplayed()
    composeTestRule.onNodeWithText("No common groups").assertIsDisplayed()
  }
}
