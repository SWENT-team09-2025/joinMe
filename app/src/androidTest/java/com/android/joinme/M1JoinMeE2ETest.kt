package com.android.joinme

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.android.joinme.model.event.EventFilter
import com.android.joinme.model.event.EventsRepositoryLocal
import com.android.joinme.model.event.EventsRepositoryProvider
import com.android.joinme.model.serie.SeriesRepositoryLocal
import com.android.joinme.model.serie.SeriesRepositoryProvider
import com.android.joinme.ui.history.HistoryScreenTestTags
import com.android.joinme.ui.navigation.NavigationTestTags
import com.android.joinme.ui.navigation.Screen
import com.android.joinme.ui.overview.CreateEventScreenTestTags
import com.android.joinme.ui.overview.OverviewScreenTestTags
import com.android.joinme.ui.overview.SearchScreenTestTags
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
class M1JoinMeE2ETest {

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
            uid = "test-user-id", name = "Test User", email = "test@joinme.com")

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

    // IMPORTANT: Clear ALL repositories to prevent test interference
    runBlocking {
      // Clear events repository
      val repo = EventsRepositoryProvider.getRepository(isOnline = false)
      if (repo is EventsRepositoryLocal) {
        val events =
            repo.getAllEvents(eventFilter = EventFilter.EVENTS_FOR_OVERVIEW_SCREEN).toList()
        events.forEach { repo.deleteEvent(it.eventId) }
      }

      // Clear series repository
      val seriesRepo = SeriesRepositoryProvider.repository
      if (seriesRepo is SeriesRepositoryLocal) {
        seriesRepo.clear()
      }

      // Clear groups repository
      val groupRepo = com.android.joinme.model.groups.GroupRepositoryProvider.repository
      if (groupRepo is com.android.joinme.model.groups.GroupRepositoryLocal) {
        groupRepo.clear()
      }

      // Create/update test profile
      val profileRepo = com.android.joinme.model.profile.ProfileRepositoryProvider.repository
      if (profileRepo is com.android.joinme.model.profile.ProfileRepositoryLocal) {
        val testProfile =
            com.android.joinme.model.profile.Profile(
                uid = "test-user-id",
                username = "Test User",
                email = "test@joinme.com",
                dateOfBirth = "01/01/2000",
                country = "Switzerland",
                interests = listOf("Sports"),
                bio = "E2E Test User",
                createdAt = com.google.firebase.Timestamp.now(),
                updatedAt = com.google.firebase.Timestamp.now())
        profileRepo.createOrUpdateProfile(testProfile)
      }
    }
    composeTestRule.waitForIdle()

    // Start app at Overview screen since we've already authenticated
    composeTestRule.setContent {
      JoinMe(startDestination = Screen.Overview.route, enableNotificationPermissionRequest = false)
    }

    // Wait for initial load - increased for CI environments
    composeTestRule.waitForIdle()
    Thread.sleep(3000) // Give time for initial screen to load (longer for CI)
    composeTestRule.waitForIdle()

    // Ensure Overview screen is fully loaded before tests start
    composeTestRule.waitUntil(timeoutMillis = 15000) {
      composeTestRule
          .onAllNodesWithTag(OverviewScreenTestTags.CREATE_EVENT_BUTTON, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
  }

  // ==================== HELPER METHODS ====================

  /** Wait for loading indicators to disappear */
  private fun waitForLoading() {
    composeTestRule.waitForIdle()
    Thread.sleep(500) // Give UI time to settle
    composeTestRule.waitForIdle()
  }

  /** Find and click the confirm button in native dialogs (date/time pickers) */
  private fun clickNativeDialogConfirmButton(timeoutMs: Long = 5000) {
    // Try multiple strategies to find the confirm button
    val confirmButton = device.wait(Until.findObject(By.text("OK").clickable(true)), timeoutMs)

    if (confirmButton != null) {
      confirmButton.click()
      return
    }

    // Try other common button texts
    val alternativeTexts = listOf("Done", "Confirm", "Set", "OK")
    for (text in alternativeTexts) {
      val button = device.findObject(By.text(text).clickable(true))
      if (button != null) {
        button.click()
        return
      }
    }

    // Try using Android resource IDs (button1 is typically the positive button)
    val button1 = device.findObject(By.res("android:id/button1"))
    if (button1 != null) {
      button1.click()
      return
    }

    // Last resort: find any clickable button in the dialog
    val anyButton = device.findObject(By.clickable(true).clazz("android.widget.Button"))
    if (anyButton != null) {
      anyButton.click()
      return
    }

    throw AssertionError("Could not find confirm button in native dialog")
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
      location: String = "Lausanne, Switzerland",
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

    // Wait for suggestions to load - increased timeout for CI (geocoding can be slow)
    composeTestRule.waitUntil(timeoutMillis = 30000) {
      composeTestRule
          .onAllNodesWithTag(CreateEventScreenTestTags.INPUT_EVENT_LOCATION_SUGGESTIONS)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    // Select first suggestion
    composeTestRule
        .onAllNodesWithTag(CreateEventScreenTestTags.FOR_EACH_INPUT_EVENT_LOCATION_SUGGESTION)[0]
        .performClick()

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
    // Wait for native date picker dialog and click confirm button
    clickNativeDialogConfirmButton()
    composeTestRule.waitForIdle()

    // Fill time (opens native Android dialog)
    // Note: After filling date, the placeholder might change, so we find by scrolling to last
    // scrollable element
    composeTestRule.waitForIdle()
    // Find time field - it should contain "time" or have a time icon
    composeTestRule
        .onAllNodesWithText("Time", substring = true, ignoreCase = true)[0]
        .performClick()
    composeTestRule.waitForIdle()
    // Wait for native time picker dialog and click confirm button
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

    // 2. Click add event bubble
    composeTestRule
        .onNodeWithTag(OverviewScreenTestTags.ADD_EVENT_BUBBLE, useUnmergedTree = true)
        .performClick()
    waitForLoading()

    // 3. Fill out the form
    fillEventForm(title = eventTitle)

    // 4. Save the event (scroll to it first to make sure it's visible)
    waitForLoading()
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.BUTTON_SAVE_EVENT, useUnmergedTree = true)
        .performScrollTo()
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.BUTTON_SAVE_EVENT, useUnmergedTree = true)
        .performClick()
    waitForLoading()
    // Give extra time for the async save operation to complete
    composeTestRule.waitForIdle()

    // THEN: Event should appear in Overview screen
    // Wait for the event list to appear (may take time for data to load)
    composeTestRule.waitUntil(timeoutMillis = 20000) {
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
      composeTestRule.onNodeWithContentDescription("Profile").isDisplayed()
    }

    navigateToTab("Overview")
    composeTestRule
        .onNodeWithTag(OverviewScreenTestTags.CREATE_EVENT_BUTTON, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun e2e_backNavigationFlow_worksCorrectly() {
    // GIVEN: User is on Overview
    composeTestRule.onNodeWithTag(OverviewScreenTestTags.CREATE_EVENT_BUTTON).assertExists()

    // WHEN: User presses create event and create serie option bubbles
    composeTestRule.onNodeWithTag(OverviewScreenTestTags.CREATE_EVENT_BUTTON).performClick()
    waitForLoading()

    // THEN: Click on Add Event Bubble
    composeTestRule.onNodeWithTag(OverviewScreenTestTags.ADD_EVENT_BUBBLE).performClick()
    waitForLoading()

    // WHEN: User presses back
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
      composeTestRule.onNodeWithContentDescription("Profile").isDisplayed()
    }
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
      composeTestRule
          .onNodeWithTag(OverviewScreenTestTags.ADD_EVENT_BUBBLE, useUnmergedTree = true)
          .performClick()
      waitForLoading()
      fillEventForm(title = title)
      waitForLoading()
      composeTestRule
          .onNodeWithTag(CreateEventScreenTestTags.BUTTON_SAVE_EVENT, useUnmergedTree = true)
          .performScrollTo()
      composeTestRule
          .onNodeWithTag(CreateEventScreenTestTags.BUTTON_SAVE_EVENT, useUnmergedTree = true)
          .performClick()
      waitForLoading()
      composeTestRule.waitForIdle()
    }

    // THEN: All events should be visible
    // Wait for the event list to appear
    composeTestRule.waitUntil(timeoutMillis = 20000) {
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
    composeTestRule
        .onNodeWithTag(OverviewScreenTestTags.ADD_EVENT_BUBBLE, useUnmergedTree = true)
        .performClick()
    waitForLoading()
    fillEventForm(title = eventTitle)
    waitForLoading()
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.BUTTON_SAVE_EVENT, useUnmergedTree = true)
        .performScrollTo()
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.BUTTON_SAVE_EVENT, useUnmergedTree = true)
        .performClick()
    waitForLoading()
    composeTestRule.waitForIdle()

    // Navigate to Profile
    navigateToTab("Profile")
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule.onNodeWithContentDescription("Profile").isDisplayed()
    }

    // Return to Overview
    navigateToTab("Overview")

    // Event should still be visible
    composeTestRule.waitUntil(timeoutMillis = 20000) {
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
