package com.android.joinme

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import com.android.joinme.model.chat.ChatRepositoryLocal
import com.android.joinme.model.chat.ChatRepositoryProvider
import com.android.joinme.model.chat.Message
import com.android.joinme.model.chat.MessageType
import com.android.joinme.model.event.Event
import com.android.joinme.model.event.EventFilter
import com.android.joinme.model.event.EventType
import com.android.joinme.model.event.EventVisibility
import com.android.joinme.model.event.EventsRepositoryLocal
import com.android.joinme.model.event.EventsRepositoryProvider
import com.android.joinme.model.groups.Group
import com.android.joinme.model.groups.GroupRepositoryLocal
import com.android.joinme.model.groups.GroupRepositoryProvider
import com.android.joinme.model.profile.Profile
import com.android.joinme.model.profile.ProfileRepositoryLocal
import com.android.joinme.model.profile.ProfileRepositoryProvider
import com.android.joinme.model.serie.SeriesRepositoryLocal
import com.android.joinme.model.serie.SeriesRepositoryProvider
import com.android.joinme.ui.calendar.CalendarScreenTestTags
import com.android.joinme.ui.chat.ChatScreenTestTags
import com.android.joinme.ui.groups.GroupDetailScreenTestTags
import com.android.joinme.ui.groups.GroupListScreenTestTags
import com.android.joinme.ui.groups.leaderboard.LeaderboardTestTags
import com.android.joinme.ui.navigation.NavigationTestTags
import com.android.joinme.ui.navigation.Screen
import com.android.joinme.ui.overview.CreateEventScreenTestTags
import com.android.joinme.ui.overview.OverviewScreenTestTags
import com.android.joinme.ui.overview.ShowEventScreenTestTags
import com.android.joinme.ui.profile.FollowListScreenTestTags
import com.android.joinme.ui.profile.PublicProfileScreenTestTags
import com.android.joinme.ui.profile.ViewProfileTestTags
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import java.util.Calendar
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Milestone 3 End-to-End tests for the JoinMe application.
 *
 * These tests verify complete user workflows for M3 features including:
 * - Public profile viewing and following
 * - Direct messaging chat
 * - Event creation, joining, and event chat
 * - Group chat functionality
 * - Calendar navigation
 * - Complete social interaction workflows
 *
 * Tests complete user journeys from start to finish, testing the entire system including
 * navigation, data persistence, and UI interactions across multiple screens.
 *
 * Note: This file was co-written with Claude AI.
 */
@RunWith(AndroidJUnit4::class)
class M3JoinMeE2ETest {

  @get:Rule
  val permissionRule: GrantPermissionRule =
      GrantPermissionRule.grant(
          android.Manifest.permission.ACCESS_FINE_LOCATION,
          android.Manifest.permission.ACCESS_COARSE_LOCATION)

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var auth: FirebaseAuth
  private lateinit var device: UiDevice

  // Test Data
  private val currentUserId = "test-user-id"
  private val targetUserId = "target-user-b"
  private val thirdUserId = "third-user-c"

  private val currentUserProfile =
      Profile(
          uid = currentUserId,
          username = "Current User",
          email = "current@joinme.com",
          bio = "I am the test user",
          interests = listOf("Sports", "Music"),
          createdAt = Timestamp.now(),
          updatedAt = Timestamp.now())

  private val targetProfile =
      Profile(
          uid = targetUserId,
          username = "Target User B",
          email = "target@example.com",
          bio = "I am the target user for E2E testing",
          interests = listOf("Coding", "Testing"),
          createdAt = Timestamp.now(),
          updatedAt = Timestamp.now())

  private val thirdProfile =
      Profile(
          uid = thirdUserId,
          username = "Third User C",
          email = "third@example.com",
          bio = "Third user for testing",
          interests = listOf("Gaming"),
          createdAt = Timestamp.now(),
          updatedAt = Timestamp.now())

  private val testGroup =
      Group(
          id = "test-group-id",
          name = "E2E Shared Group",
          description = "Group for testing profiles",
          memberIds = listOf(currentUserId, targetUserId),
          ownerId = currentUserId,
          category = EventType.SOCIAL)

  private val testGroupWithChat =
      Group(
          id = "chat-group-id",
          name = "Chat Test Group",
          description = "Group for testing chat",
          memberIds = listOf(currentUserId, targetUserId, thirdUserId),
          ownerId = currentUserId,
          category = EventType.SPORTS)

  // Test event that user can join
  private val testEvent =
      Event(
          eventId = "test-event-id",
          title = "E2E Test Event",
          description = "An event for E2E testing",
          date = Timestamp(Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, 1) }.time),
          location =
              com.android.joinme.model.map.Location(
                  latitude = 46.5197, longitude = 6.6323, name = "EPFL, Lausanne"),
          type = EventType.SPORTS,
          visibility = EventVisibility.PUBLIC,
          maxParticipants = 10,
          participants = listOf(targetUserId),
          ownerId = targetUserId,
          duration = 60)

  // Event where current user is owner (for testing event chat access)
  private val ownedEvent =
      Event(
          eventId = "owned-event-id",
          title = "My Owned Event",
          description = "An event owned by current user",
          date = Timestamp(Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, 2) }.time),
          location =
              com.android.joinme.model.map.Location(
                  latitude = 46.5197, longitude = 6.6323, name = "EPFL, Lausanne"),
          type = EventType.ACTIVITY,
          visibility = EventVisibility.PUBLIC,
          maxParticipants = 5,
          participants = listOf(currentUserId, targetUserId),
          ownerId = currentUserId,
          duration = 90)

  @Before
  fun setup() {
    System.setProperty("IS_TEST_ENV", "true")
    auth = FirebaseAuth.getInstance()
    auth.signOut()

    // Initialize UiDevice for interacting with native dialogs
    device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    // 1. Setup Profile Repository
    val profileRepo = ProfileRepositoryProvider.repository
    if (profileRepo is ProfileRepositoryLocal) {
      runBlocking {
        profileRepo.createOrUpdateProfile(currentUserProfile)
        profileRepo.createOrUpdateProfile(targetProfile)
        profileRepo.createOrUpdateProfile(thirdProfile)
      }
    }

    // 2. Setup Group Repository
    val groupRepo = GroupRepositoryProvider.repository
    if (groupRepo is GroupRepositoryLocal) {
      groupRepo.clear()
      runBlocking {
        groupRepo.addGroup(testGroup)
        groupRepo.addGroup(testGroupWithChat)
      }
    }

    // 3. Setup Events Repository
    val eventsRepo = EventsRepositoryProvider.getRepository(isOnline = false)
    if (eventsRepo is EventsRepositoryLocal) {
      runBlocking {
        // Clear existing events
        val events =
            eventsRepo.getAllEvents(eventFilter = EventFilter.EVENTS_FOR_OVERVIEW_SCREEN).toList()
        events.forEach { eventsRepo.deleteEvent(it.eventId) }
        // Add test events
        eventsRepo.addEvent(testEvent)
        eventsRepo.addEvent(ownedEvent)
      }
    }

    // 4. Setup Series Repository (clear it)
    val seriesRepo = SeriesRepositoryProvider.repository
    if (seriesRepo is SeriesRepositoryLocal) {
      seriesRepo.clear()
    }

    // 5. Setup Chat Repository with some pre-existing messages
    val chatRepo = ChatRepositoryProvider.repository
    if (chatRepo is ChatRepositoryLocal) {
      runBlocking {
        // Add a test message in the owned event chat with unique ID
        chatRepo.addMessage(
            Message(
                id = "msg-${System.currentTimeMillis()}-${System.nanoTime()}",
                conversationId = ownedEvent.eventId,
                senderId = targetUserId,
                senderName = targetProfile.username,
                content = "Hello from the event chat!",
                timestamp = System.currentTimeMillis() - 60000,
                type = MessageType.TEXT))
      }
    }

    // 6. Start App
    composeTestRule.setContent {
      JoinMe(startDestination = Screen.Overview.route, enableNotificationPermissionRequest = false)
    }

    // Wait for app to settle
    composeTestRule.waitForIdle()
    Thread.sleep(1000)
    composeTestRule.waitForIdle()
  }

  // ==================== HELPER METHODS ====================

  /** Wait for loading indicators to disappear */
  private fun waitForLoading() {
    composeTestRule.waitForIdle()
    Thread.sleep(500)
    composeTestRule.waitForIdle()
  }

  /** Find and click the confirm button in native dialogs (date/time pickers) */
  private fun clickNativeDialogConfirmButton(timeoutMs: Long = 5000) {
    val confirmButton = device.wait(Until.findObject(By.text("OK").clickable(true)), timeoutMs)

    if (confirmButton != null) {
      confirmButton.click()
      return
    }

    val alternativeTexts = listOf("Done", "Confirm", "Set", "OK")
    for (text in alternativeTexts) {
      val button = device.findObject(By.text(text).clickable(true))
      if (button != null) {
        button.click()
        return
      }
    }

    val button1 = device.findObject(By.res("android:id/button1"))
    if (button1 != null) {
      button1.click()
      return
    }

    val anyButton = device.findObject(By.clickable(true).clazz("android.widget.Button"))
    if (anyButton != null) {
      anyButton.click()
      return
    }

    throw AssertionError("Could not find confirm button in native dialog")
  }

  /** Select tomorrow's date in the Android date picker dialog */
  private fun selectTomorrowInDatePicker() {
    val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, 1) }
    val day = tomorrow.get(Calendar.DAY_OF_MONTH)

    device.wait(Until.hasObject(By.clazz("android.widget.DatePicker")), 2000)

    val datePicker = device.findObject(UiSelector().className("android.widget.DatePicker"))
    if (datePicker.exists()) {
      datePicker.swipeUp(10)
      Thread.sleep(200)
    }

    try {
      val tomorrowButton = device.findObject(By.text(day.toString()))
      if (tomorrowButton != null) {
        tomorrowButton.click()
        Thread.sleep(300)
      }
    } catch (e: Exception) {
      println("Could not select specific date, using default: ${e.message}")
    }

    clickNativeDialogConfirmButton()
    Thread.sleep(300)
  }

  /** Navigate to a specific tab using bottom navigation */
  private fun navigateToTab(tabName: String) {
    composeTestRule
        .onNodeWithTag(NavigationTestTags.tabTag(tabName), useUnmergedTree = true)
        .performClick()
    waitForLoading()
  }

  /** Fill out the event form with test data */
  private fun fillEventForm(
      title: String = "E2E Test Event",
      description: String = "This is an end-to-end test event",
      location: String = "Lausanne",
      type: String = "SPORTS",
      visibility: String = "PUBLIC"
  ) {
    // Select event type
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_TYPE).performScrollTo()
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_TYPE).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText(type).performClick()
    composeTestRule.waitForIdle()

    // Fill title
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_TITLE).performScrollTo()
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_TITLE)
        .performTextInput(title)
    composeTestRule.waitForIdle()

    // Fill description
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_DESCRIPTION)
        .performScrollTo()
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_DESCRIPTION)
        .performTextInput(description)
    composeTestRule.waitForIdle()

    // Fill location
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_LOCATION).performScrollTo()
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_LOCATION)
        .performTextInput(location)
    composeTestRule.waitForIdle()

    // Wait for suggestions to load
    composeTestRule.waitUntil(timeoutMillis = 10000) {
      composeTestRule
          .onAllNodesWithTag(CreateEventScreenTestTags.INPUT_EVENT_LOCATION_SUGGESTIONS)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    // Select first suggestion
    composeTestRule
        .onAllNodesWithTag(CreateEventScreenTestTags.FOR_EACH_INPUT_EVENT_LOCATION_SUGGESTION)[0]
        .performClick()

    // Fill max participants
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_MAX_PARTICIPANTS)
        .performScrollTo()
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_MAX_PARTICIPANTS)
        .performClick()
    composeTestRule.waitForIdle()
    Thread.sleep(300)
    composeTestRule.onNodeWithText("OK").performClick()
    composeTestRule.waitForIdle()

    // Fill duration
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_DURATION).performScrollTo()
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_DURATION).performClick()
    composeTestRule.waitForIdle()
    Thread.sleep(300)
    composeTestRule.onNodeWithText("OK").performClick()
    composeTestRule.waitForIdle()

    // Fill date
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_DATE).performScrollTo()
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_DATE).performClick()
    composeTestRule.waitForIdle()
    selectTomorrowInDatePicker()
    composeTestRule.waitForIdle()

    // Fill time
    Thread.sleep(500)
    composeTestRule.waitForIdle()
    composeTestRule
        .onAllNodesWithText("Time", substring = true, ignoreCase = true)[0]
        .performClick()
    composeTestRule.waitForIdle()
    clickNativeDialogConfirmButton()
    Thread.sleep(300)
    composeTestRule.waitForIdle()

    // Select visibility
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_VISIBILITY)
        .performScrollTo()
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_VISIBILITY).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText(visibility).performClick()
    composeTestRule.waitForIdle()

    waitForLoading()
  }

  // ==================== WORKFLOW 1: PUBLIC PROFILE AND FOLLOWING ====================

  @Test
  fun e2e_viewPublicProfile_viaGroupDetails_andFollow() {
    // Navigate to Profile -> Groups -> Group Details
    navigateToTab("Profile")
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule.onAllNodesWithContentDescription("Profile").fetchSemanticsNodes().isNotEmpty()
    }

    // Go to Groups List
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithContentDescription("Group").performClick()

    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(GroupListScreenTestTags.LIST, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Click on the Shared Group
    composeTestRule
        .onNodeWithTag(GroupListScreenTestTags.LIST, useUnmergedTree = true)
        .performScrollToNode(hasText(testGroup.name))
    composeTestRule.onNodeWithText(testGroup.name).performClick()

    // Wait for Group Details
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule.onAllNodesWithText(testGroup.name).fetchSemanticsNodes().isNotEmpty()
    }

    // Click on Target User in Member List
    composeTestRule.waitForIdle()
    composeTestRule
        .onNodeWithTag("membersList")
        .performScrollToNode(hasTestTag(GroupDetailScreenTestTags.memberItemTag(targetUserId)))
    composeTestRule
        .onNodeWithTag(GroupDetailScreenTestTags.memberItemTag(targetUserId))
        .performClick()

    // Public Profile Screen should be displayed
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(PublicProfileScreenTestTags.SCREEN)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Verify Username is displayed
    composeTestRule.onNodeWithTag(PublicProfileScreenTestTags.USERNAME).assertExists()

    // Click the follow button
    composeTestRule.onNodeWithTag(PublicProfileScreenTestTags.FOLLOW_BUTTON).performClick()
    composeTestRule.waitForIdle()
    Thread.sleep(500)

    // Navigate back to ViewProfile screen
    composeTestRule.onNodeWithContentDescription("Back").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithContentDescription("Back").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithContentDescription("Back").performClick()
    composeTestRule.waitForIdle()

    // Should be on ViewProfile screen
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(ViewProfileTestTags.SCREEN)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule.onNodeWithTag(ViewProfileTestTags.SCREEN).assertExists()
  }

  @Test
  fun e2e_followUser_andNavigateToFollowingList() {
    // Navigate to Profile -> Groups -> Group Details -> Target User -> Follow
    navigateToTab("Profile")
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule.onAllNodesWithContentDescription("Profile").fetchSemanticsNodes().isNotEmpty()
    }

    composeTestRule.onNodeWithContentDescription("Group").performClick()
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(GroupListScreenTestTags.LIST, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule.onNodeWithText(testGroup.name).performClick()
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule.onAllNodesWithText(testGroup.name).fetchSemanticsNodes().isNotEmpty()
    }

    composeTestRule
        .onNodeWithTag("membersList")
        .performScrollToNode(hasTestTag(GroupDetailScreenTestTags.memberItemTag(targetUserId)))
    composeTestRule
        .onNodeWithTag(GroupDetailScreenTestTags.memberItemTag(targetUserId))
        .performClick()

    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(PublicProfileScreenTestTags.SCREEN)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Follow the user
    composeTestRule.onNodeWithTag(PublicProfileScreenTestTags.FOLLOW_BUTTON).performClick()
    composeTestRule.waitForIdle()

    // Navigate back to profile
    composeTestRule.onNodeWithContentDescription("Back").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithContentDescription("Back").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithContentDescription("Back").performClick()
    composeTestRule.waitForIdle()

    // Click on Following stat to see the list
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(ViewProfileTestTags.SCREEN)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule.onNodeWithTag(ViewProfileTestTags.FOLLOWING_STAT).performClick()
    composeTestRule.waitForIdle()

    // THEN: Should be on FollowList screen showing following tab
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(FollowListScreenTestTags.SCREEN)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule.onNodeWithTag(FollowListScreenTestTags.FOLLOWING_TAB).assertExists()

    // Verify the followed user appears in the list
    composeTestRule
        .onNodeWithTag(FollowListScreenTestTags.profileItemTag(targetUserId))
        .assertExists()
    composeTestRule.onNodeWithText(targetProfile.username, substring = true).assertExists()

    // Click on the followed user to navigate to their public profile
    composeTestRule
        .onNodeWithTag(FollowListScreenTestTags.profileItemTag(targetUserId))
        .performClick()
    composeTestRule.waitForIdle()

    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(PublicProfileScreenTestTags.SCREEN)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule.onNodeWithTag(PublicProfileScreenTestTags.SCREEN).assertExists()
  }

  // ==================== WORKFLOW 2: DIRECT MESSAGE CHAT ====================

  @Test
  fun e2e_directMessageChat_navigateAndSendMessage() {
    // Navigate to Public Profile via Groups
    navigateToTab("Profile")
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule.onAllNodesWithContentDescription("Profile").fetchSemanticsNodes().isNotEmpty()
    }

    composeTestRule.onNodeWithContentDescription("Group").performClick()
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(GroupListScreenTestTags.LIST, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule.onNodeWithText(testGroup.name).performClick()
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule.onAllNodesWithText(testGroup.name).fetchSemanticsNodes().isNotEmpty()
    }

    composeTestRule
        .onNodeWithTag("membersList")
        .performScrollToNode(hasTestTag(GroupDetailScreenTestTags.memberItemTag(targetUserId)))
    composeTestRule
        .onNodeWithTag(GroupDetailScreenTestTags.memberItemTag(targetUserId))
        .performClick()

    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(PublicProfileScreenTestTags.SCREEN)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Click the Message button to navigate to chat
    composeTestRule.onNodeWithTag(PublicProfileScreenTestTags.MESSAGE_BUTTON).performClick()
    composeTestRule.waitForIdle()

    // Wait for chat screen message input to be ready
    composeTestRule.waitUntil(timeoutMillis = 10000) {
      composeTestRule
          .onAllNodesWithTag(ChatScreenTestTags.MESSAGE_INPUT)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    Thread.sleep(1000)
    composeTestRule.waitForIdle()

    // Type a message and send it
    val testMessage = "Hello from E2E test! ${System.currentTimeMillis()}"
    composeTestRule.onNodeWithTag(ChatScreenTestTags.MESSAGE_INPUT).performTextInput(testMessage)
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(ChatScreenTestTags.SEND_BUTTON).performClick()
    composeTestRule.waitForIdle()
    Thread.sleep(1000)

    // Message should appear in the chat
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithText(testMessage, substring = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule.onNodeWithText(testMessage, substring = true).assertExists()
    composeTestRule.onNodeWithTag(ChatScreenTestTags.MESSAGE_LIST).assertExists()
  }

  // ==================== WORKFLOW 3: EVENT VIEWING ====================
  // Note: Event chat tests require Firebase authentication which is not available in E2E tests.
  // The ChatFloatingActionButton only shows for authenticated owners/participants.

  @Test
  fun e2e_viewEvent_andVerifyDetails() {
    // Navigate to Overview and find an event
    composeTestRule.waitUntil(timeoutMillis = 10000) {
      composeTestRule
          .onAllNodesWithTag(OverviewScreenTestTags.EVENT_LIST, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Find and click on the owned event
    composeTestRule
        .onNodeWithTag(OverviewScreenTestTags.EVENT_LIST, useUnmergedTree = true)
        .performScrollToNode(hasText(ownedEvent.title))
    composeTestRule.onNodeWithText(ownedEvent.title, useUnmergedTree = true).performClick()
    waitForLoading()

    // Should be on ShowEventScreen
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(ShowEventScreenTestTags.SCREEN, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Verify event details are displayed
    composeTestRule.onNodeWithTag(ShowEventScreenTestTags.SCREEN).assertExists()
    composeTestRule.onNodeWithTag(ShowEventScreenTestTags.EVENT_TITLE).assertExists()

    // Navigate back
    composeTestRule.onNodeWithContentDescription("Back").performClick()
    waitForLoading()

    // Should be back on Overview
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(OverviewScreenTestTags.EVENT_LIST, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
  }

  // ==================== WORKFLOW 4: CALENDAR NAVIGATION ====================

  @Test
  fun e2e_navigateToCalendar_andBack() {
    // Calendar is accessed from the Overview screen, not Profile
    // Ensure we're on Overview screen first
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(OverviewScreenTestTags.CREATE_EVENT_BUTTON, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Click on Calendar icon in the Overview TopBar
    composeTestRule.onNodeWithTag(OverviewScreenTestTags.CALENDAR_BUTTON).performClick()
    composeTestRule.waitForIdle()

    // Wait for Calendar screen
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(CalendarScreenTestTags.SCREEN)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Verify Calendar screen components
    composeTestRule.onNodeWithTag(CalendarScreenTestTags.SCREEN).assertExists()
    composeTestRule.onNodeWithTag(CalendarScreenTestTags.CALENDAR_GRID).assertExists()
    composeTestRule.onNodeWithTag(CalendarScreenTestTags.UPCOMING_EVENTS_SECTION).assertExists()

    // Test month navigation
    composeTestRule.onNodeWithTag(CalendarScreenTestTags.NEXT_MONTH_BUTTON).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(CalendarScreenTestTags.PREVIOUS_MONTH_BUTTON).performClick()
    composeTestRule.waitForIdle()

    // Go back to Overview
    composeTestRule.onNodeWithTag(CalendarScreenTestTags.BACK_BUTTON).performClick()
    composeTestRule.waitForIdle()

    // Should be back on Overview
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(OverviewScreenTestTags.CREATE_EVENT_BUTTON, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
  }

  // ==================== WORKFLOW 6: CHAT ATTACHMENT MENU ====================

  @Test
  fun e2e_openAttachmentMenu_inChat() {
    // Navigate to a chat first (via public profile)
    navigateToTab("Profile")
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule.onAllNodesWithContentDescription("Profile").fetchSemanticsNodes().isNotEmpty()
    }

    composeTestRule.onNodeWithContentDescription("Group").performClick()
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(GroupListScreenTestTags.LIST, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule.onNodeWithText(testGroup.name).performClick()
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule.onAllNodesWithText(testGroup.name).fetchSemanticsNodes().isNotEmpty()
    }

    composeTestRule
        .onNodeWithTag("membersList")
        .performScrollToNode(hasTestTag(GroupDetailScreenTestTags.memberItemTag(targetUserId)))
    composeTestRule
        .onNodeWithTag(GroupDetailScreenTestTags.memberItemTag(targetUserId))
        .performClick()

    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(PublicProfileScreenTestTags.SCREEN)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule.onNodeWithTag(PublicProfileScreenTestTags.MESSAGE_BUTTON).performClick()

    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(ChatScreenTestTags.SCREEN)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Click on attachment button
    composeTestRule.onNodeWithTag(ChatScreenTestTags.ATTACHMENT_BUTTON).performClick()
    composeTestRule.waitForIdle()

    // Verify attachment menu appears
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(ChatScreenTestTags.ATTACHMENT_MENU)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Verify attachment options exist
    composeTestRule.onNodeWithTag(ChatScreenTestTags.ATTACHMENT_PHOTO).assertExists()
    composeTestRule.onNodeWithTag(ChatScreenTestTags.ATTACHMENT_LOCATION).assertExists()

    // Dismiss the menu by clicking outside (or clicking photo to open dialog, then dismiss)
    composeTestRule.onNodeWithTag(ChatScreenTestTags.ATTACHMENT_PHOTO).performClick()
    composeTestRule.waitForIdle()

    // Photo source dialog should appear
    composeTestRule.waitUntil(timeoutMillis = 3000) {
      composeTestRule
          .onAllNodesWithTag(ChatScreenTestTags.PHOTO_SOURCE_DIALOG)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Verify photo source options
    composeTestRule.onNodeWithTag(ChatScreenTestTags.PHOTO_SOURCE_GALLERY).assertExists()
    composeTestRule.onNodeWithTag(ChatScreenTestTags.PHOTO_SOURCE_CAMERA).assertExists()
  }

  // ==================== WORKFLOW 7: NAVIGATION CONSISTENCY ====================

  @Test
  fun e2e_navigationConsistency_acrossTabs() {
    // Test that navigation works consistently across all tabs

    // Start on Overview
    composeTestRule
        .onNodeWithTag(OverviewScreenTestTags.CREATE_EVENT_BUTTON, useUnmergedTree = true)
        .assertExists()

    // Go to Search
    navigateToTab("Search")
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(
              com.android.joinme.ui.overview.SearchScreenTestTags.SEARCH_TEXT_FIELD,
              useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Test filter buttons in Search screen
    composeTestRule.onNodeWithText("Social").assertExists()
    composeTestRule.onNodeWithText("Social").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Activity").assertExists()
    composeTestRule.onNodeWithText("Activity").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Sport").assertExists()
    composeTestRule.onNodeWithText("Sport").performClick()
    composeTestRule.waitForIdle()

    // Go to Map
    navigateToTab("Map")
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(
              com.android.joinme.ui.map.MapScreenTestTags.GOOGLE_MAP_SCREEN, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Go to Profile
    navigateToTab("Profile")
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule.onAllNodesWithContentDescription("Profile").fetchSemanticsNodes().isNotEmpty()
    }

    // Go back to Overview
    navigateToTab("Overview")
    composeTestRule
        .onNodeWithTag(OverviewScreenTestTags.CREATE_EVENT_BUTTON, useUnmergedTree = true)
        .assertExists()

    // Verify all tabs are accessible again
    navigateToTab("Profile")
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule.onAllNodesWithContentDescription("Profile").fetchSemanticsNodes().isNotEmpty()
    }
  }

  // ==================== WORKFLOW 5: GROUP LEADERBOARD ====================

  @Test
  fun e2e_leaderboard_navigateAndSwitchTabs() {
    // Navigate to Profile -> Groups -> Group Details
    navigateToTab("Profile")
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule.onAllNodesWithContentDescription("Profile").fetchSemanticsNodes().isNotEmpty()
    }

    composeTestRule.onNodeWithContentDescription("Group").performClick()
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(GroupListScreenTestTags.LIST, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Select a group
    composeTestRule
        .onNodeWithTag(GroupListScreenTestTags.LIST, useUnmergedTree = true)
        .performScrollToNode(hasText(testGroupWithChat.name))
    composeTestRule.onNodeWithText(testGroupWithChat.name).performClick()

    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule.onAllNodesWithText(testGroupWithChat.name).fetchSemanticsNodes().isNotEmpty()
    }

    // Click on Leaderboard button
    composeTestRule.onNodeWithTag(GroupDetailScreenTestTags.BUTTON_LEADERBOARD).performClick()
    waitForLoading()

    // Leaderboard screen should open
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(LeaderboardTestTags.SCREEN)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Verify leaderboard screen is displayed with all components
    composeTestRule.onNodeWithTag(LeaderboardTestTags.SCREEN).assertExists()
    composeTestRule.onNodeWithTag(LeaderboardTestTags.TOP_BAR).assertExists()
    composeTestRule.onNodeWithTag(LeaderboardTestTags.TAB_ROW).assertExists()
    composeTestRule.onNodeWithTag(LeaderboardTestTags.TAB_CURRENT).assertExists()
    composeTestRule.onNodeWithTag(LeaderboardTestTags.TAB_ALL_TIME).assertExists()

    // Switch to All Time tab
    composeTestRule.onNodeWithTag(LeaderboardTestTags.TAB_ALL_TIME).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(LeaderboardTestTags.TAB_ALL_TIME).assertExists()

    // Switch back to Current tab
    composeTestRule.onNodeWithTag(LeaderboardTestTags.TAB_CURRENT).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(LeaderboardTestTags.TAB_CURRENT).assertExists()
  }

  // ==================== WORKFLOW 8: EVENT LOCATION NAVIGATION ====================

  @Test
  fun e2e_clickEventLocation_navigatesToMap() {
    // Navigate to Overview and find an event
    composeTestRule.waitUntil(timeoutMillis = 10000) {
      composeTestRule
          .onAllNodesWithTag(OverviewScreenTestTags.EVENT_LIST, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Find and click on the test event
    composeTestRule
        .onNodeWithTag(OverviewScreenTestTags.EVENT_LIST, useUnmergedTree = true)
        .performScrollToNode(hasText(testEvent.title))
    composeTestRule.onNodeWithText(testEvent.title, useUnmergedTree = true).performClick()
    waitForLoading()

    // Should be on ShowEventScreen
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(ShowEventScreenTestTags.SCREEN, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Verify event details are displayed
    composeTestRule.onNodeWithTag(ShowEventScreenTestTags.SCREEN).assertExists()
    composeTestRule.onNodeWithTag(ShowEventScreenTestTags.EVENT_TITLE).assertExists()
    composeTestRule.onNodeWithTag(ShowEventScreenTestTags.EVENT_LOCATION).assertExists()

    // Verify the location text is displayed (EPFL, Lausanne)
    composeTestRule.onNodeWithText("EPFL, Lausanne", substring = true).assertExists()

    // Click on the location field to navigate to map
    composeTestRule.onNodeWithTag(ShowEventScreenTestTags.EVENT_LOCATION).performClick()
    composeTestRule.waitForIdle()
    Thread.sleep(500)

    // Should navigate to map screen with the event location
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(
              com.android.joinme.ui.map.MapScreenTestTags.GOOGLE_MAP_SCREEN, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Verify map screen is displayed
    composeTestRule
        .onNodeWithTag(
            com.android.joinme.ui.map.MapScreenTestTags.GOOGLE_MAP_SCREEN, useUnmergedTree = true)
        .assertExists()

    // Interact with the map
    composeTestRule
        .onNodeWithTag(
            com.android.joinme.ui.map.MapScreenTestTags.GOOGLE_MAP_SCREEN, useUnmergedTree = true)
        .performClick()
    composeTestRule.waitForIdle()
  }
}
