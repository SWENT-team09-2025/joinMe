package com.android.joinme.e2e

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.android.joinme.JoinMe
import com.android.joinme.model.event.EventsRepositoryLocal
import com.android.joinme.model.event.EventsRepositoryProvider
import com.android.joinme.repository.GroupRepositoryLocal
import com.android.joinme.repository.GroupRepositoryProvider
import com.android.joinme.ui.groups.GroupListScreenTestTags
import com.android.joinme.ui.history.HistoryScreenTestTags
import com.android.joinme.ui.navigation.NavigationTestTags
import com.android.joinme.ui.navigation.Screen
import com.android.joinme.ui.overview.CreateEventScreenTestTags
import com.android.joinme.ui.overview.OverviewScreenTestTags
import com.android.joinme.ui.overview.SearchScreenTestTags
import com.android.joinme.ui.profile.ViewProfileTestTags
import com.android.joinme.utils.FakeJwtGenerator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Comprehensive End-to-End tests for the JoinMe application.
 *
 * These tests verify complete user workflows from start to finish, testing the entire system
 * including authentication, navigation, data persistence, and UI interactions across multiple
 * screens.
 *
 * Uses FakeCredentialManager to simulate Google Sign-In without requiring manual authentication.
 */
@RunWith(AndroidJUnit4::class)
class JoinMeE2ETest {

  @get:Rule
  val permissionRule: GrantPermissionRule =
      GrantPermissionRule.grant(
          android.Manifest.permission.ACCESS_FINE_LOCATION,
          android.Manifest.permission.ACCESS_COARSE_LOCATION)

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var auth: FirebaseAuth
  private lateinit var device: UiDevice

  @Before
  fun setup() {
    // Setup test environment
    System.setProperty("IS_TEST_ENV", "true")
    auth = FirebaseAuth.getInstance()
    auth.signOut()

    // Initialize UiDevice for interacting with native dialogs
    device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    // Create fake Google ID token
    val fakeIdToken =
        FakeJwtGenerator.createFakeGoogleIdToken(
            uid = "test-user-123", name = "Test User", email = "test@joinme.com")

    // Sign in to Firebase with fake token
    val credential = GoogleAuthProvider.getCredential(fakeIdToken, null)
    runBlocking {
      try {
        auth.signInWithCredential(credential).await()
      } catch (e: Exception) {
        // If Firebase emulator is not running, we skip
        println("Warning: Could not sign in to Firebase: ${e.message}")
      }
    }

    // Setup local repository with clean state
    val repo = EventsRepositoryProvider.getRepository(isOnline = false)
    if (repo is EventsRepositoryLocal) {
      runBlocking {
        // Clear existing events
        val events = repo.getAllEvents().toList()
        events.forEach { repo.deleteEvent(it.eventId) }
      }
    }

    // Setup group repository with clean state
    val groupRepo = GroupRepositoryProvider.repository
    if (groupRepo is GroupRepositoryLocal) {
      groupRepo.clearAllGroups()
    }

    // Start app at Overview screen since we've already authenticated
    composeTestRule.setContent { JoinMe(startDestination = Screen.Overview.route) }

    // Wait for initial load
    composeTestRule.waitForIdle()
    Thread.sleep(1000) // Give time for initial screen to load
    composeTestRule.waitForIdle()
  }

  // ==================== HELPER METHODS ====================

  /** Wait for loading indicators to disappear */
  private fun waitForLoading() {
    composeTestRule.waitForIdle()
    Thread.sleep(500) // Give UI time to settle
    composeTestRule.waitForIdle()
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
      location: String = "EPFL Campus",
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

    // Fill max participants (opens Compose dialog)
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_MAX_PARTICIPANTS)
        .performScrollTo()
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_MAX_PARTICIPANTS)
        .performClick()
    composeTestRule.waitForIdle()
    Thread.sleep(300) // Wait for dialog to open
    // Click OK on the dialog (uses default value)
    composeTestRule.onNodeWithText("OK").performClick()
    composeTestRule.waitForIdle()

    // Fill duration (opens Compose dialog)
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_DURATION).performScrollTo()
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_DURATION).performClick()
    composeTestRule.waitForIdle()
    Thread.sleep(300) // Wait for dialog to open
    // Click OK on the dialog (uses default value)
    composeTestRule.onNodeWithText("OK").performClick()
    composeTestRule.waitForIdle()

    // Fill date (opens native Android dialog)
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_DATE).performScrollTo()
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_DATE).performClick()
    composeTestRule.waitForIdle()
    // Wait for native date picker dialog and click OK using UiAutomator
    device.wait(Until.hasObject(By.text("OK")), 2000)
    device.findObject(By.text("OK")).click()
    Thread.sleep(300)
    composeTestRule.waitForIdle()

    // Fill time (opens native Android dialog)
    // Note: After filling date, the placeholder might change, so we find by scrolling to last
    // scrollable element
    Thread.sleep(500) // Let UI settle after date selection
    composeTestRule.waitForIdle()
    // Find time field - it should contain "time" or have a time icon
    composeTestRule
        .onAllNodesWithText("Time", substring = true, ignoreCase = true)[0]
        .performClick()
    composeTestRule.waitForIdle()
    // Wait for native time picker dialog and click OK using UiAutomator
    device.wait(Until.hasObject(By.text("OK")), 2000)
    device.findObject(By.text("OK")).click()
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

  /** Scroll to find and select an event by title */
  private fun selectEventByTitle(eventTitle: String) {
    composeTestRule
        .onNodeWithTag(OverviewScreenTestTags.EVENT_LIST)
        .performScrollToNode(hasText(eventTitle))
    composeTestRule.onNodeWithText(eventTitle).performClick()
    waitForLoading()
  }

  // ==================== E2E TEST FLOWS ====================

  @Test
  fun e2e_completeEventCreationFlow_createsAndDisplaysEvent() {
    // GIVEN: User is on Overview screen
    composeTestRule
        .onNodeWithTag(OverviewScreenTestTags.CREATE_EVENT_BUTTON, useUnmergedTree = true)
        .assertExists()

    // WHEN: User creates a new event
    val eventTitle = "E2E Basketball ${System.currentTimeMillis()}"

    // 1. Click create event button
    composeTestRule
        .onNodeWithTag(OverviewScreenTestTags.CREATE_EVENT_BUTTON, useUnmergedTree = true)
        .performClick()
    waitForLoading()

    // 2. Fill out the form
    fillEventForm(title = eventTitle)

    // 3. Save the event
    waitForLoading()
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.BUTTON_SAVE_EVENT, useUnmergedTree = true)
        .performClick()
    waitForLoading()
    // Give extra time for the async save operation to complete
    Thread.sleep(1000)
    composeTestRule.waitForIdle()

    // THEN: Event should appear in Overview screen
    // Wait for the event list to appear (may take time for data to load)
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(OverviewScreenTestTags.EVENT_LIST, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule
        .onNodeWithTag(OverviewScreenTestTags.EVENT_LIST, useUnmergedTree = true)
        .performScrollToNode(hasText(eventTitle))
    composeTestRule.onNodeWithText(eventTitle, useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun e2e_navigationBetweenAllTabs_worksCorrectly() {
    // GIVEN: User is on Overview screen
    composeTestRule
        .onNodeWithTag(OverviewScreenTestTags.CREATE_EVENT_BUTTON, useUnmergedTree = true)
        .assertExists()

    // WHEN & THEN: User navigates to each tab
    navigateToTab("Search")
    composeTestRule
        .onNodeWithTag(SearchScreenTestTags.SEARCH_TEXT_FIELD, useUnmergedTree = true)
        .assertIsDisplayed()

    navigateToTab("Map")
    composeTestRule
        .onNodeWithTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU, useUnmergedTree = true)
        .assertIsDisplayed()

    navigateToTab("Profile")
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(ViewProfileTestTags.PROFILE_TITLE, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    composeTestRule
        .onNodeWithTag(ViewProfileTestTags.PROFILE_TITLE, useUnmergedTree = true)
        .assertIsDisplayed()

    navigateToTab("Overview")
    composeTestRule
        .onNodeWithTag(OverviewScreenTestTags.CREATE_EVENT_BUTTON, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun e2e_backNavigationFlow_worksCorrectly() {
    // GIVEN: User is on Overview
    composeTestRule.onNodeWithTag(OverviewScreenTestTags.CREATE_EVENT_BUTTON).assertExists()

    // WHEN: User navigates forward and uses back button
    composeTestRule.onNodeWithTag(OverviewScreenTestTags.CREATE_EVENT_BUTTON).performClick()
    waitForLoading()

    composeTestRule.onNodeWithContentDescription("Back").performClick()
    waitForLoading()

    // THEN: Should be back on Overview
    composeTestRule.onNodeWithTag(OverviewScreenTestTags.CREATE_EVENT_BUTTON).assertIsDisplayed()
  }

  @Test
  fun e2e_historyFlow_showsHistoryScreen() {
    // GIVEN: User is on Overview screen
    composeTestRule.onNodeWithTag(OverviewScreenTestTags.CREATE_EVENT_BUTTON).assertExists()

    // WHEN: User navigates to history
    composeTestRule.onNodeWithTag(OverviewScreenTestTags.HISTORY_BUTTON).performClick()
    waitForLoading()

    // THEN: History screen should be displayed
    composeTestRule.onNodeWithTag(HistoryScreenTestTags.SCREEN).assertIsDisplayed()
  }

  @Test
  fun e2e_searchFlow_displaysSearchScreen() {
    // GIVEN: User is on Overview
    composeTestRule.onNodeWithTag(OverviewScreenTestTags.CREATE_EVENT_BUTTON).assertExists()

    // WHEN: User navigates to Search
    navigateToTab("Search")

    // THEN: Search screen should be displayed
    composeTestRule.onNodeWithTag(SearchScreenTestTags.SEARCH_TEXT_FIELD).assertIsDisplayed()
  }

  @Test
  fun e2e_profileFlow_displaysProfileScreen() {
    // GIVEN: User is on Overview
    composeTestRule
        .onNodeWithTag(OverviewScreenTestTags.CREATE_EVENT_BUTTON, useUnmergedTree = true)
        .assertExists()

    // WHEN: User navigates to Profile
    navigateToTab("Profile")

    // THEN: Profile screen should be displayed
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(ViewProfileTestTags.PROFILE_TITLE, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    composeTestRule
        .onNodeWithTag(ViewProfileTestTags.PROFILE_TITLE, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun e2e_createMultipleEvents_allEventsDisplayed() {
    // GIVEN: User wants to create multiple events
    val eventTitles =
        listOf(
            "E2E Event 1 ${System.currentTimeMillis()}",
            "E2E Event 2 ${System.currentTimeMillis() + 1}",
            "E2E Event 3 ${System.currentTimeMillis() + 2}")

    // WHEN: User creates multiple events
    eventTitles.forEach { title ->
      composeTestRule
          .onNodeWithTag(OverviewScreenTestTags.CREATE_EVENT_BUTTON, useUnmergedTree = true)
          .performClick()
      waitForLoading()
      fillEventForm(title = title)
      waitForLoading()
      composeTestRule
          .onNodeWithTag(CreateEventScreenTestTags.BUTTON_SAVE_EVENT, useUnmergedTree = true)
          .performClick()
      waitForLoading()
      // Give extra time for the async save operation to complete
      Thread.sleep(1000)
      composeTestRule.waitForIdle()
    }

    // THEN: All events should be visible
    // Wait for the event list to appear
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(OverviewScreenTestTags.EVENT_LIST, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    eventTitles.forEach { title ->
      composeTestRule
          .onNodeWithTag(OverviewScreenTestTags.EVENT_LIST, useUnmergedTree = true)
          .performScrollToNode(hasText(title))
      composeTestRule.onNodeWithText(title, useUnmergedTree = true).assertIsDisplayed()
    }
  }

  @Test
  fun e2e_completeUserJourney_navigationWorks() {
    // Complete journey test
    val eventTitle = "E2E Journey ${System.currentTimeMillis()}"

    // Create event
    composeTestRule
        .onNodeWithTag(OverviewScreenTestTags.CREATE_EVENT_BUTTON, useUnmergedTree = true)
        .performClick()
    waitForLoading()
    fillEventForm(title = eventTitle)
    waitForLoading()
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.BUTTON_SAVE_EVENT, useUnmergedTree = true)
        .performClick()
    waitForLoading()
    // Give extra time for the async save operation to complete
    Thread.sleep(1000)
    composeTestRule.waitForIdle()

    // Navigate to Profile
    navigateToTab("Profile")
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(ViewProfileTestTags.PROFILE_TITLE, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    composeTestRule
        .onNodeWithTag(ViewProfileTestTags.PROFILE_TITLE, useUnmergedTree = true)
        .assertIsDisplayed()

    // Return to Overview
    navigateToTab("Overview")

    // Event should still be visible
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(OverviewScreenTestTags.EVENT_LIST, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    composeTestRule
        .onNodeWithTag(OverviewScreenTestTags.EVENT_LIST, useUnmergedTree = true)
        .performScrollToNode(hasText(eventTitle))
    composeTestRule.onNodeWithText(eventTitle, useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun e2e_groupListFlow_displaysEmptyState() {
    // GIVEN: User is on Overview screen with no groups
    composeTestRule
        .onNodeWithTag(OverviewScreenTestTags.CREATE_EVENT_BUTTON, useUnmergedTree = true)
        .assertExists()

    // WHEN: User navigates to Profile then to Groups
    navigateToTab("Profile")
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(ViewProfileTestTags.PROFILE_TITLE, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Click the Group icon in the top bar to navigate to GroupListScreen
    composeTestRule.onNodeWithContentDescription("Group", useUnmergedTree = true).performClick()
    waitForLoading()

    // THEN: Empty state should be displayed
    composeTestRule
        .onNodeWithTag(GroupListScreenTestTags.EMPTY, useUnmergedTree = true)
        .assertIsDisplayed()

    // Join button should be visible
    composeTestRule
        .onNodeWithTag(GroupListScreenTestTags.ADD_NEW_GROUP, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun e2e_groupListFlow_displaysGroupsWithData() {
    // GIVEN: User has some groups
    val groupRepo = GroupRepositoryProvider.repository
    if (groupRepo is GroupRepositoryLocal) {
      groupRepo.addTestGroup(
          id = "group1",
          name = "Basketball Team",
          ownerId = "test-user-123",
          description = "Weekly basketball games",
          memberCount = 5)
      groupRepo.addTestGroup(
          id = "group2",
          name = "Running Club",
          ownerId = "test-user-123",
          description = "Morning runs every day",
          memberCount = 12)
    }

    // WHEN: User navigates to Groups
    navigateToTab("Profile")
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(ViewProfileTestTags.PROFILE_TITLE, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule.onNodeWithContentDescription("Group", useUnmergedTree = true).performClick()
    waitForLoading()

    // THEN: Groups should be displayed
    composeTestRule
        .onNodeWithTag(GroupListScreenTestTags.LIST, useUnmergedTree = true)
        .assertIsDisplayed()

    // Verify group cards are displayed
    composeTestRule
        .onNodeWithTag(GroupListScreenTestTags.cardTag("group1"), useUnmergedTree = true)
        .assertIsDisplayed()
    composeTestRule.onNodeWithText("Basketball Team", useUnmergedTree = true).assertIsDisplayed()
    composeTestRule.onNodeWithText("members: 5", useUnmergedTree = true).assertIsDisplayed()

    composeTestRule
        .onNodeWithTag(GroupListScreenTestTags.cardTag("group2"), useUnmergedTree = true)
        .assertIsDisplayed()
    composeTestRule.onNodeWithText("Running Club", useUnmergedTree = true).assertIsDisplayed()
    composeTestRule.onNodeWithText("members: 12", useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun e2e_completeUserJourneyWithGroups_navigationWorks() {
    // GIVEN: User has some groups and events
    val groupRepo = GroupRepositoryProvider.repository
    if (groupRepo is GroupRepositoryLocal) {
      groupRepo.addTestGroup(
          id = "group1",
          name = "E2E Test Group",
          ownerId = "test-user-123",
          description = "Test group for E2E",
          memberCount = 3)
    }

    val eventTitle = "E2E Group Journey ${System.currentTimeMillis()}"

    // WHEN: User navigates through the entire app
    // 1. Create an event
    composeTestRule
        .onNodeWithTag(OverviewScreenTestTags.CREATE_EVENT_BUTTON, useUnmergedTree = true)
        .performClick()
    waitForLoading()
    fillEventForm(title = eventTitle)
    waitForLoading()
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.BUTTON_SAVE_EVENT, useUnmergedTree = true)
        .performClick()
    waitForLoading()
    Thread.sleep(1000)
    composeTestRule.waitForIdle()

    // 2. Navigate to Profile
    navigateToTab("Profile")
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(ViewProfileTestTags.PROFILE_TITLE, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // 3. Navigate to Groups
    composeTestRule.onNodeWithContentDescription("Group", useUnmergedTree = true).performClick()
    waitForLoading()

    // THEN: Group should be visible
    composeTestRule
        .onNodeWithTag(GroupListScreenTestTags.LIST, useUnmergedTree = true)
        .assertIsDisplayed()
    composeTestRule.onNodeWithText("E2E Test Group", useUnmergedTree = true).assertIsDisplayed()

    // 4. Navigate back to Profile using back button
    composeTestRule.onNodeWithContentDescription("Back", useUnmergedTree = true).performClick()
    waitForLoading()
    composeTestRule
        .onNodeWithTag(ViewProfileTestTags.PROFILE_TITLE, useUnmergedTree = true)
        .assertIsDisplayed()

    // 5. Navigate back to Overview
    navigateToTab("Overview")

    // Event should still be visible
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(OverviewScreenTestTags.EVENT_LIST, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    composeTestRule
        .onNodeWithTag(OverviewScreenTestTags.EVENT_LIST, useUnmergedTree = true)
        .performScrollToNode(hasText(eventTitle))
    composeTestRule.onNodeWithText(eventTitle, useUnmergedTree = true).assertIsDisplayed()
  }
}
