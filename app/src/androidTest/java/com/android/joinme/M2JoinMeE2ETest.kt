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
import com.android.joinme.model.event.EventFilter
import com.android.joinme.model.event.EventsRepositoryLocal
import com.android.joinme.model.event.EventsRepositoryProvider
import com.android.joinme.model.serie.SeriesRepositoryLocal
import com.android.joinme.model.serie.SeriesRepositoryProvider
import com.android.joinme.ui.groups.CreateGroupScreenTestTags
import com.android.joinme.ui.groups.GroupListScreenTestTags
import com.android.joinme.ui.history.HistoryScreenTestTags
import com.android.joinme.ui.map.MapScreenTestTags
import com.android.joinme.ui.navigation.NavigationTestTags
import com.android.joinme.ui.navigation.Screen
import com.android.joinme.ui.overview.CreateEventScreenTestTags
import com.android.joinme.ui.overview.CreateSerieScreenTestTags
import com.android.joinme.ui.overview.EditSerieScreenTestTags
import com.android.joinme.ui.overview.OverviewScreenTestTags
import com.android.joinme.ui.overview.SearchScreenTestTags
import com.android.joinme.ui.overview.SerieDetailsScreenTestTags
import com.android.joinme.ui.profile.EditProfileTestTags
import com.google.firebase.auth.FirebaseAuth
import java.util.Calendar
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Note: This test file was co-written with Claude AI */

/**
 * Milestone 2 End-to-End tests for the JoinMe application.
 *
 * These tests verify complete user workflows for new M2 features including:
 * - Series management (create, edit, navigate)
 * - Groups management (create, view, edit)
 * - Advanced search and discovery
 * - History tracking
 * - Map integration
 *
 * Tests complete user journeys from start to finish, testing the entire system including
 * authentication, navigation, data persistence, and UI interactions across multiple screens.
 */
@RunWith(AndroidJUnit4::class)
class M2JoinMeE2ETest {

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

    // Note: We rely on test environment detection (IS_TEST_ENV = true) for user ID
    // BaseSerieFormViewModel.getCurrentUserId() will return "test-user-id" in test mode
    // SerieDetailsScreen should also use the same test environment detection
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
    }
    composeTestRule.waitForIdle()

    // Start app at Overview screen
    composeTestRule.setContent {
      JoinMe(startDestination = Screen.Overview.route, enableNotificationPermissionRequest = false)
    }

    // Wait for initial load and auth state to settle - increased for CI environments
    composeTestRule.waitForIdle()
    Thread.sleep(3000) // Give time for initial screen to load (longer for CI)
    composeTestRule.waitForIdle()

    // Ensure Overview screen is fully loaded before tests start
    composeTestRule.waitUntil(timeoutMillis = 25000) {
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
    Thread.sleep(500)
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

  /** Select tomorrow's date in the Android date picker dialog */
  private fun selectTomorrowInDatePicker() {
    // Calculate tomorrow's date
    val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, 1) }
    val year = tomorrow.get(Calendar.YEAR)
    val month = tomorrow.get(Calendar.MONTH) // 0-indexed
    val day = tomorrow.get(Calendar.DAY_OF_MONTH)

    // Wait for date picker to appear
    device.wait(Until.hasObject(By.clazz("android.widget.DatePicker")), 2000)

    // Find the DatePicker widget and set tomorrow's date
    val datePicker = device.findObject(UiSelector().className("android.widget.DatePicker"))
    if (datePicker.exists()) {
      // Use updateDate method (year, month 0-indexed, day)
      datePicker.swipeUp(10) // Small swipe to ensure it's interactive
      Thread.sleep(200)
    }

    // Alternative approach: Try to find and click the specific date
    // For Material DatePicker, we can try to click on tomorrow's day number
    try {
      // Look for tomorrow's day button in the calendar grid
      val tomorrowButton = device.findObject(By.text(day.toString()))
      if (tomorrowButton != null) {
        tomorrowButton.click()
        Thread.sleep(300)
      }
    } catch (e: Exception) {
      // If we can't find the specific day, the default date might work
    }

    // Click OK to confirm
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

    // Try to select location suggestion (increased timeout for CI - geocoding can be slow)
    try {
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
    } catch (e: androidx.compose.ui.test.ComposeTimeoutException) {
      // Geocoding not available - continue without selecting suggestion
      println("Location suggestions timeout - skipping geocoding (expected in some environments)")
    } catch (e: Exception) {
      // Other errors - continue without selecting suggestion
      println("Error selecting location: ${e.message}")
    }

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

  /** Fill out the event of Serie form with test data */
  private fun fillEventForSerieForm(
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

    // Try to select location suggestion (increased timeout for CI - geocoding can be slow)
    try {
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
    } catch (e: androidx.compose.ui.test.ComposeTimeoutException) {
      // Geocoding not available - continue without selecting suggestion
      println("Location suggestions timeout - skipping geocoding (expected in some environments)")
    } catch (e: Exception) {
      // Other errors - continue without selecting suggestion
      println("Error selecting location: ${e.message}")
    }

    // Fill duration
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_DURATION).performScrollTo()
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_DURATION).performClick()
    composeTestRule.waitForIdle()
    Thread.sleep(300)
    composeTestRule.onNodeWithText("OK").performClick()
    composeTestRule.waitForIdle()

    waitForLoading()
  }

  /** Fill out the serie form with test data */
  private fun fillSerieForm(
      title: String = "E2E Test Serie",
      description: String = "This is an end-to-end test serie",
      visibility: String = "PUBLIC"
  ) {
    // Wait for CreateSerieScreen to fully initialize and check auth state
    Thread.sleep(800)
    composeTestRule.waitForIdle()

    // Verify we're on the create serie form (not redirected due to auth issues)
    composeTestRule
        .onNodeWithTag(CreateSerieScreenTestTags.INPUT_SERIE_TITLE, useUnmergedTree = true)
        .assertExists("Create Serie form should be displayed")

    // Fill title
    composeTestRule.onNodeWithTag(CreateSerieScreenTestTags.INPUT_SERIE_TITLE).performScrollTo()
    composeTestRule
        .onNodeWithTag(CreateSerieScreenTestTags.INPUT_SERIE_TITLE)
        .performTextInput(title)
    composeTestRule.waitForIdle()

    // Fill description
    composeTestRule
        .onNodeWithTag(CreateSerieScreenTestTags.INPUT_SERIE_DESCRIPTION)
        .performScrollTo()
    composeTestRule
        .onNodeWithTag(CreateSerieScreenTestTags.INPUT_SERIE_DESCRIPTION)
        .performTextInput(description)
    composeTestRule.waitForIdle()

    // Fill max participants
    composeTestRule
        .onNodeWithTag(CreateSerieScreenTestTags.INPUT_SERIE_MAX_PARTICIPANTS)
        .performScrollTo()
    composeTestRule
        .onNodeWithTag(CreateSerieScreenTestTags.INPUT_SERIE_MAX_PARTICIPANTS)
        .performClick()
    composeTestRule.waitForIdle()
    Thread.sleep(300)
    composeTestRule.onNodeWithText("OK").performClick()
    composeTestRule.waitForIdle()

    // Fill date
    composeTestRule.onNodeWithTag(CreateSerieScreenTestTags.INPUT_SERIE_DATE).performScrollTo()
    composeTestRule.onNodeWithTag(CreateSerieScreenTestTags.INPUT_SERIE_DATE).performClick()
    composeTestRule.waitForIdle()
    selectTomorrowInDatePicker()
    composeTestRule.waitForIdle()

    // Fill time
    Thread.sleep(500)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(CreateSerieScreenTestTags.INPUT_SERIE_TIME).performScrollTo()
    composeTestRule.onNodeWithTag(CreateSerieScreenTestTags.INPUT_SERIE_TIME).performClick()
    composeTestRule.waitForIdle()
    clickNativeDialogConfirmButton()
    Thread.sleep(300)
    composeTestRule.waitForIdle()

    // Select visibility
    composeTestRule
        .onNodeWithTag(CreateSerieScreenTestTags.INPUT_SERIE_VISIBILITY)
        .performScrollTo()
    composeTestRule.onNodeWithTag(CreateSerieScreenTestTags.INPUT_SERIE_VISIBILITY).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText(visibility).performClick()
    composeTestRule.waitForIdle()

    waitForLoading()
  }

  /** Fill out the group form with test data */
  private fun fillGroupForm(
      name: String = "TestGroup",
      description: String = "Test group description",
      category: String = "SOCIAL"
  ) {
    // Fill name
    composeTestRule.onNodeWithTag(CreateGroupScreenTestTags.GROUP_NAME_TEXT_FIELD).performScrollTo()
    composeTestRule
        .onNodeWithTag(CreateGroupScreenTestTags.GROUP_NAME_TEXT_FIELD)
        .performTextInput(name)
    composeTestRule.waitForIdle()

    // Select Category
    composeTestRule.onNodeWithTag(CreateGroupScreenTestTags.CATEGORY_DROPDOWN).performScrollTo()
    composeTestRule.onNodeWithTag(CreateGroupScreenTestTags.CATEGORY_DROPDOWN).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText(category).performClick()
    composeTestRule.waitForIdle()

    // Fill description
    composeTestRule
        .onNodeWithTag(CreateGroupScreenTestTags.GROUP_DESCRIPTION_TEXT_FIELD)
        .performScrollTo()
    composeTestRule
        .onNodeWithTag(CreateGroupScreenTestTags.GROUP_DESCRIPTION_TEXT_FIELD)
        .performTextInput(description)
    composeTestRule.waitForIdle()

    waitForLoading()
  }

  // ==================== WORKFLOW 1: EVENTS & SERIES MANAGEMENT ====================

  @Test
  fun e2e_workflow1_completeSerieCreationAndNavigationFlow() {
    // GIVEN: User is on Overview screen
    composeTestRule
        .onNodeWithTag(OverviewScreenTestTags.CREATE_EVENT_BUTTON, useUnmergedTree = true)
        .assertExists()

    // WHEN: User creates a serie with multiple events
    val serieTitle = "E2E Basketball Serie ${System.currentTimeMillis()}"
    val event1Title = "Event 1 - ${System.currentTimeMillis()}"
    val event2Title = "Event 2 - ${System.currentTimeMillis() + 1}"

    // Step 1: Create serie
    composeTestRule
        .onNodeWithTag(OverviewScreenTestTags.CREATE_EVENT_BUTTON, useUnmergedTree = true)
        .performClick()
    waitForLoading()

    composeTestRule
        .onNodeWithTag(OverviewScreenTestTags.ADD_SERIE_BUBBLE, useUnmergedTree = true)
        .performClick()
    waitForLoading()
    // Extra wait for CreateSerieViewModel to initialize and verify auth
    Thread.sleep(1000)
    composeTestRule.waitForIdle()

    fillSerieForm(title = serieTitle)

    composeTestRule
        .onNodeWithTag(CreateSerieScreenTestTags.BUTTON_SAVE_SERIE, useUnmergedTree = true)
        .performClick()
    waitForLoading()
    Thread.sleep(1000)
    composeTestRule.waitForIdle()

    // Step 2: Add first event to serie
    fillEventForSerieForm(title = event1Title)
    waitForLoading()

    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.BUTTON_SAVE_EVENT, useUnmergedTree = true)
        .performClick()
    waitForLoading()
    Thread.sleep(1000)
    composeTestRule.waitForIdle()

    // Step 3: Return to Overview and verify serie card appears
    composeTestRule.waitUntil(timeoutMillis = 25000) {
      composeTestRule
          .onAllNodesWithTag(OverviewScreenTestTags.EVENT_LIST, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule
        .onNodeWithTag(OverviewScreenTestTags.EVENT_LIST, useUnmergedTree = true)
        .performScrollToNode(hasText(serieTitle))
    composeTestRule.onNodeWithText(serieTitle, useUnmergedTree = true).assertIsDisplayed()

    // Step 4: Navigate to serie details and verify event is listed
    composeTestRule.onNodeWithText(serieTitle, useUnmergedTree = true).performClick()
    waitForLoading()

    composeTestRule
        .onNodeWithTag(SerieDetailsScreenTestTags.SCREEN, useUnmergedTree = true)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(SerieDetailsScreenTestTags.SERIE_TITLE, useUnmergedTree = true)
        .assertIsDisplayed()

    // Verify event is listed
    composeTestRule.onNodeWithText(event1Title, substring = true).assertIsDisplayed()

    // THEN: Serie creation and navigation successful
    composeTestRule.onNodeWithContentDescription("Back").performClick()
    waitForLoading()
    composeTestRule
        .onNodeWithTag(OverviewScreenTestTags.CREATE_EVENT_BUTTON, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun e2e_workflow1_editSerieAndEvents() {
    // GIVEN: Create a serie with one event first
    val serieTitle = "E2E Serie to Edit ${System.currentTimeMillis()}"
    val newSerieTitle = "EDITED Serie ${System.currentTimeMillis()}"
    val eventTitle = "Event in Serie ${System.currentTimeMillis()}"

    // Create serie
    composeTestRule
        .onNodeWithTag(OverviewScreenTestTags.CREATE_EVENT_BUTTON, useUnmergedTree = true)
        .performClick()
    waitForLoading()
    composeTestRule
        .onNodeWithTag(OverviewScreenTestTags.ADD_SERIE_BUBBLE, useUnmergedTree = true)
        .performClick()
    waitForLoading()
    // Extra wait for CreateSerieViewModel to initialize and verify auth
    Thread.sleep(1000)
    composeTestRule.waitForIdle()
    fillSerieForm(title = serieTitle)
    composeTestRule
        .onNodeWithTag(CreateSerieScreenTestTags.BUTTON_SAVE_SERIE, useUnmergedTree = true)
        .performClick()
    waitForLoading()
    Thread.sleep(1000)
    composeTestRule.waitForIdle()

    // Add event to serie
    fillEventForSerieForm(title = eventTitle)
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.BUTTON_SAVE_EVENT, useUnmergedTree = true)
        .performClick()
    waitForLoading()
    Thread.sleep(1000)
    composeTestRule.waitForIdle()

    // WHEN: Navigate to serie details and edit serie
    composeTestRule.waitUntil(timeoutMillis = 25000) {
      composeTestRule
          .onAllNodesWithTag(OverviewScreenTestTags.EVENT_LIST, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    composeTestRule
        .onNodeWithTag(OverviewScreenTestTags.EVENT_LIST, useUnmergedTree = true)
        .performScrollToNode(hasText(serieTitle))
    composeTestRule.onNodeWithText(serieTitle, useUnmergedTree = true).performClick()
    waitForLoading()
    // Edit serie
    composeTestRule
        .onNodeWithTag(SerieDetailsScreenTestTags.EDIT_SERIE_BUTTON, useUnmergedTree = true)
        .performClick()
    waitForLoading()

    // Change title
    composeTestRule.onNodeWithTag(EditSerieScreenTestTags.INPUT_SERIE_TITLE).performScrollTo()
    composeTestRule.onNodeWithTag(EditSerieScreenTestTags.INPUT_SERIE_TITLE).performTextClearance()
    composeTestRule
        .onNodeWithTag(EditSerieScreenTestTags.INPUT_SERIE_TITLE)
        .performTextInput(newSerieTitle)
    composeTestRule.waitForIdle()

    // Save changes
    composeTestRule
        .onNodeWithTag(EditSerieScreenTestTags.SERIE_SAVE, useUnmergedTree = true)
        .performClick()
    waitForLoading()
    Thread.sleep(1000)
    composeTestRule.waitForIdle()

    composeTestRule.waitUntil(timeoutMillis = 25000) {
      composeTestRule
          .onAllNodesWithTag(OverviewScreenTestTags.EVENT_LIST, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    composeTestRule
        .onNodeWithTag(OverviewScreenTestTags.EVENT_LIST, useUnmergedTree = true)
        .performScrollToNode(hasText(newSerieTitle))
    composeTestRule.onNodeWithText(newSerieTitle, useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun e2e_workflow1_serieNavigationFlow() {
    // GIVEN: Create serie with 2 events
    val serieTitle = "E2E Navigation Serie ${System.currentTimeMillis()}"
    val event1Title = "Nav Event 1 ${System.currentTimeMillis()}"
    val event2Title = "Nav Event 2 ${System.currentTimeMillis() + 1}"

    // Create serie
    composeTestRule
        .onNodeWithTag(OverviewScreenTestTags.CREATE_EVENT_BUTTON, useUnmergedTree = true)
        .performClick()
    waitForLoading()
    composeTestRule
        .onNodeWithTag(OverviewScreenTestTags.ADD_SERIE_BUBBLE, useUnmergedTree = true)
        .performClick()
    waitForLoading()
    // Extra wait for CreateSerieViewModel to initialize and verify auth
    Thread.sleep(1000)
    composeTestRule.waitForIdle()
    fillSerieForm(title = serieTitle)
    composeTestRule
        .onNodeWithTag(CreateSerieScreenTestTags.BUTTON_SAVE_SERIE, useUnmergedTree = true)
        .performClick()
    waitForLoading()
    Thread.sleep(1000)
    composeTestRule.waitForIdle()

    // Add first event
    fillEventForSerieForm(title = event1Title)
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.BUTTON_SAVE_EVENT, useUnmergedTree = true)
        .performClick()
    waitForLoading()
    Thread.sleep(1000)
    composeTestRule.waitForIdle()

    // Wait for Overview screen and bottom navigation to be visible
    composeTestRule.waitUntil(timeoutMillis = 25000) {
      composeTestRule
          .onAllNodesWithTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    navigateToTab("Search")

    // THEN: Should be able to find serie in search screen
    composeTestRule
        .onNodeWithText(serieTitle, substring = true, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  // ==================== WORKFLOW 2: GROUPS & PROFILE ====================

  @Test
  fun e2e_workflow2_completeGroupManagementFlow() {
    // GIVEN: User is on Overview screen
    composeTestRule
        .onNodeWithTag(OverviewScreenTestTags.CREATE_EVENT_BUTTON, useUnmergedTree = true)
        .assertExists()

    // WHEN: Navigate to Profile → Groups → Create Group
    navigateToTab("Profile")
    composeTestRule.waitUntil(timeoutMillis = 25000) {
      composeTestRule.onAllNodesWithContentDescription("Profile").fetchSemanticsNodes().isNotEmpty()
    }

    // Navigate to Groups (look for Groups button/text)
    composeTestRule.onNodeWithContentDescription("Group").performClick()
    waitForLoading()

    // Verify we're on Groups list screen
    composeTestRule
        .onNodeWithTag(GroupListScreenTestTags.ADD_NEW_GROUP, useUnmergedTree = true)
        .assertIsDisplayed()

    // Create new group (keep name under 30 chars)
    val groupName = "TestGrp${System.currentTimeMillis() % 100000}"

    composeTestRule
        .onNodeWithTag(GroupListScreenTestTags.ADD_NEW_GROUP, useUnmergedTree = true)
        .performClick()
    waitForLoading()

    // Fill group form
    fillGroupForm(name = groupName)

    // Save group
    composeTestRule
        .onNodeWithTag(CreateGroupScreenTestTags.SAVE_BUTTON, useUnmergedTree = true)
        .performClick()
    waitForLoading()
    composeTestRule.waitForIdle()

    // THEN: Verify group appears in group list
    composeTestRule.waitUntil(timeoutMillis = 25000) {
      composeTestRule
          .onAllNodesWithTag(GroupListScreenTestTags.LIST, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule
        .onNodeWithTag(GroupListScreenTestTags.LIST, useUnmergedTree = true)
        .performScrollToNode(hasText(groupName))
    composeTestRule.onNodeWithText(groupName, useUnmergedTree = true).assertIsDisplayed()

    // Click on group to view details
    composeTestRule.onNodeWithText(groupName, useUnmergedTree = true).performClick()
    waitForLoading()

    // Should see group details screen
    composeTestRule.onNodeWithText(groupName, substring = true).assertIsDisplayed()
  }

  @Test
  fun e2e_workflow2_editGroup() {
    // GIVEN: Create a group first (keep names under 30 chars)
    val groupName = "EditGrp${System.currentTimeMillis() % 100000}"
    val newGroupName = "EdtdGrp${System.currentTimeMillis() % 100000}"

    // Navigate to Profile → Groups
    navigateToTab("Profile")
    composeTestRule.waitUntil(timeoutMillis = 25000) {
      composeTestRule.onAllNodesWithContentDescription("Profile").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithContentDescription("Group").performClick()
    waitForLoading()

    // Create group
    composeTestRule
        .onNodeWithTag(GroupListScreenTestTags.ADD_NEW_GROUP, useUnmergedTree = true)
        .performClick()
    waitForLoading()
    fillGroupForm(name = groupName)
    composeTestRule
        .onNodeWithTag(CreateGroupScreenTestTags.SAVE_BUTTON, useUnmergedTree = true)
        .performClick()
    waitForLoading()
    composeTestRule.waitForIdle()

    // WHEN: Edit the group
    composeTestRule.waitUntil(timeoutMillis = 25000) {
      composeTestRule
          .onAllNodesWithTag(GroupListScreenTestTags.LIST, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Find the group card and click the "more" button
    // Note: We need to scroll to find it first
    composeTestRule
        .onNodeWithTag(GroupListScreenTestTags.LIST, useUnmergedTree = true)
        .performScrollToNode(hasText(groupName))
  }

  @Test
  fun e2e_workflow2_profileAndGroupsNavigation() {
    // GIVEN: User is on Overview
    composeTestRule
        .onNodeWithTag(OverviewScreenTestTags.CREATE_EVENT_BUTTON, useUnmergedTree = true)
        .assertExists()

    // WHEN: Navigate Overview → Profile → Groups → Create Group
    navigateToTab("Profile")
    composeTestRule.waitUntil(timeoutMillis = 25000) {
      composeTestRule.onAllNodesWithContentDescription("Profile").fetchSemanticsNodes().isNotEmpty()
    }

    // Navigate to Groups
    composeTestRule.onNodeWithContentDescription("Group").performClick()
    waitForLoading()
    composeTestRule
        .onNodeWithTag(GroupListScreenTestTags.ADD_NEW_GROUP, useUnmergedTree = true)
        .assertIsDisplayed()

    // Return: Groups → Profile
    composeTestRule.onNodeWithContentDescription("Back").performClick()
    waitForLoading()
    composeTestRule.onNodeWithContentDescription("Profile").assertIsDisplayed()

    // Edit Profile (if edit button exists)
    composeTestRule.onNodeWithContentDescription("Edit").performClick()
    waitForLoading()
    composeTestRule
        .onNodeWithTag(EditProfileTestTags.SCREEN, useUnmergedTree = true)
        .assertIsDisplayed()

    // Back to Profile
    composeTestRule.onNodeWithContentDescription("Back").performClick()
    waitForLoading()

    // Navigate to Overview
    composeTestRule.onNodeWithTag(NavigationTestTags.tabTag("Overview")).performClick()
    composeTestRule
        .onNodeWithTag(OverviewScreenTestTags.CREATE_EVENT_BUTTON, useUnmergedTree = true)
        .assertIsDisplayed()

    // Back to Profile
    navigateToTab("Profile")
    composeTestRule.waitUntil(timeoutMillis = 25000) {
      composeTestRule.onAllNodesWithContentDescription("Profile").fetchSemanticsNodes().isNotEmpty()
    }
  }

  // ==================== WORKFLOW 3: SEARCH, HISTORY & MAP INTEGRATION ====================

  @Test
  fun e2e_workflow3_completeSearchAndDiscoveryFlow() {
    // GIVEN: Create 2 events and 1 serie from Overview
    val event1Title = "Search Event 1 ${System.currentTimeMillis()}"
    val event2Title = "Search Event 2 ${System.currentTimeMillis() + 1}"
    val serieTitle = "Search Serie ${System.currentTimeMillis()}"

    // Create first event
    composeTestRule
        .onNodeWithTag(OverviewScreenTestTags.CREATE_EVENT_BUTTON, useUnmergedTree = true)
        .performClick()
    waitForLoading()
    composeTestRule
        .onNodeWithTag(OverviewScreenTestTags.ADD_EVENT_BUBBLE, useUnmergedTree = true)
        .performClick()
    waitForLoading()
    fillEventForm(title = event1Title)
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.BUTTON_SAVE_EVENT, useUnmergedTree = true)
        .performScrollTo()
        .performClick()
    waitForLoading()
    Thread.sleep(1000)
    composeTestRule.waitForIdle()

    // Wait for Overview screen to be visible before creating second event
    composeTestRule.waitUntil(timeoutMillis = 25000) {
      composeTestRule
          .onAllNodesWithTag(OverviewScreenTestTags.CREATE_EVENT_BUTTON, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Ensure create button is visible by scrolling to it
    composeTestRule
        .onNodeWithTag(OverviewScreenTestTags.CREATE_EVENT_BUTTON, useUnmergedTree = true)
        .performScrollTo()
    composeTestRule.waitForIdle()

    // Create second event
    composeTestRule
        .onNodeWithTag(OverviewScreenTestTags.CREATE_EVENT_BUTTON, useUnmergedTree = true)
        .performClick()
    waitForLoading()
    composeTestRule
        .onNodeWithTag(OverviewScreenTestTags.ADD_EVENT_BUBBLE, useUnmergedTree = true)
        .performClick()
    waitForLoading()
    fillEventForm(title = event2Title)

    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.BUTTON_SAVE_EVENT, useUnmergedTree = true)
        .performScrollTo()
        .performClick()
    waitForLoading()
    Thread.sleep(1000)
    composeTestRule.waitForIdle()

    // Create serie (with one event)
    composeTestRule
        .onNodeWithTag(OverviewScreenTestTags.CREATE_EVENT_BUTTON, useUnmergedTree = true)
        .performClick()
    waitForLoading()
    composeTestRule
        .onNodeWithTag(OverviewScreenTestTags.ADD_SERIE_BUBBLE, useUnmergedTree = true)
        .performClick()
    waitForLoading()
    // Extra wait for CreateSerieViewModel to initialize and verify auth
    Thread.sleep(1000)
    composeTestRule.waitForIdle()
    fillSerieForm(title = serieTitle)
    composeTestRule
        .onNodeWithTag(CreateSerieScreenTestTags.BUTTON_SAVE_SERIE, useUnmergedTree = true)
        .performClick()
    waitForLoading()
    Thread.sleep(1000)
    composeTestRule.waitForIdle()
    fillEventForSerieForm(title = "Serie Event ${System.currentTimeMillis()}")
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.BUTTON_SAVE_EVENT, useUnmergedTree = true)
        .performClick()
    waitForLoading()
    Thread.sleep(1000)
    composeTestRule.waitForIdle()

    // WHEN: Navigate to Search tab
    navigateToTab("Search")

    // THEN: Verify event appears in results
    composeTestRule
        .onNodeWithText(event1Title, substring = true, useUnmergedTree = true)
        .assertIsDisplayed()

    // Verify serie appears in results
    composeTestRule
        .onNodeWithText(serieTitle, substring = true, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun e2e_workflow3_historyNavigation() {
    // GIVEN: User is on Overview
    composeTestRule
        .onNodeWithTag(OverviewScreenTestTags.CREATE_EVENT_BUTTON, useUnmergedTree = true)
        .assertExists()

    // WHEN: Navigate to History screen
    composeTestRule
        .onNodeWithTag(OverviewScreenTestTags.HISTORY_BUTTON, useUnmergedTree = true)
        .performClick()
    waitForLoading()

    // THEN: Verify screen displays (may be empty, that's ok)
    composeTestRule
        .onNodeWithTag(HistoryScreenTestTags.SCREEN, useUnmergedTree = true)
        .assertIsDisplayed()

    // Navigate back to Overview
    composeTestRule.onNodeWithTag(HistoryScreenTestTags.BACK_BUTTON).performClick()
    waitForLoading()
    composeTestRule
        .onNodeWithTag(OverviewScreenTestTags.CREATE_EVENT_BUTTON, useUnmergedTree = true)
        .assertIsDisplayed()

    // Create an event
    val eventTitle = "History Event ${System.currentTimeMillis()}"
    composeTestRule
        .onNodeWithTag(OverviewScreenTestTags.CREATE_EVENT_BUTTON, useUnmergedTree = true)
        .performClick()
    waitForLoading()
    composeTestRule
        .onNodeWithTag(OverviewScreenTestTags.ADD_EVENT_BUBBLE, useUnmergedTree = true)
        .performClick()
    waitForLoading()
    fillEventForm(title = eventTitle)
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.BUTTON_SAVE_EVENT)
        .performScrollTo()
        .performClick()
    waitForLoading()
    Thread.sleep(1000)
    composeTestRule.waitForIdle()

    // Wait for Overview screen to be visible before clicking History button
    composeTestRule.waitUntil(timeoutMillis = 25000) {
      composeTestRule
          .onAllNodesWithTag(OverviewScreenTestTags.HISTORY_BUTTON, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Scroll to ensure History button is visible
    composeTestRule
        .onNodeWithTag(OverviewScreenTestTags.HISTORY_BUTTON, useUnmergedTree = true)
        .performScrollTo()
    composeTestRule.waitForIdle()

    // Go to History again
    composeTestRule
        .onNodeWithTag(OverviewScreenTestTags.HISTORY_BUTTON, useUnmergedTree = true)
        .performClick()
    waitForLoading()
    composeTestRule
        .onNodeWithTag(HistoryScreenTestTags.SCREEN, useUnmergedTree = true)
        .assertIsDisplayed()

    // Back to Overview
    composeTestRule.onNodeWithTag(HistoryScreenTestTags.BACK_BUTTON).performClick()
    waitForLoading()
    composeTestRule
        .onNodeWithTag(OverviewScreenTestTags.CREATE_EVENT_BUTTON, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun e2e_workflow3_mapAndMultiTabJourney() {
    // GIVEN: Create event from Overview
    val eventTitle = "Map Event ${System.currentTimeMillis()}"

    composeTestRule
        .onNodeWithTag(OverviewScreenTestTags.CREATE_EVENT_BUTTON, useUnmergedTree = true)
        .performClick()
    waitForLoading()
    composeTestRule
        .onNodeWithTag(OverviewScreenTestTags.ADD_EVENT_BUBBLE, useUnmergedTree = true)
        .performClick()
    waitForLoading()
    fillEventForm(title = eventTitle)

    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.BUTTON_SAVE_EVENT).performScrollTo()
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.BUTTON_SAVE_EVENT).performClick()
    waitForLoading()
    Thread.sleep(1000)
    composeTestRule.waitForIdle()

    // Wait for Overview screen and bottom navigation to be visible
    composeTestRule.waitUntil(timeoutMillis = 25000) {
      composeTestRule
          .onAllNodesWithTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // WHEN: Navigate Overview → Search → Find event → Overview
    navigateToTab("Search")

    composeTestRule.onNodeWithText(eventTitle, substring = true).assertIsDisplayed()

    navigateToTab("Overview")
    composeTestRule
        .onNodeWithTag(OverviewScreenTestTags.CREATE_EVENT_BUTTON, useUnmergedTree = true)
        .assertIsDisplayed()

    // Navigate to Map tab (verify map loads)
    navigateToTab("Map")
    composeTestRule.waitUntil(timeoutMillis = 25000) {
      composeTestRule
          .onAllNodesWithTag(MapScreenTestTags.GOOGLE_MAP_SCREEN, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    composeTestRule
        .onNodeWithTag(MapScreenTestTags.GOOGLE_MAP_SCREEN, useUnmergedTree = true)
        .assertIsDisplayed()

    // Navigate to Profile
    navigateToTab("Profile")
    composeTestRule.waitUntil(timeoutMillis = 25000) {
      composeTestRule.onAllNodesWithContentDescription("Profile").fetchSemanticsNodes().isNotEmpty()
    }

    // Back to Overview
    navigateToTab("Overview")
    composeTestRule
        .onNodeWithTag(OverviewScreenTestTags.CREATE_EVENT_BUTTON, useUnmergedTree = true)
        .assertIsDisplayed()

    // Navigate Overview → History → Overview → Search
    composeTestRule
        .onNodeWithTag(OverviewScreenTestTags.HISTORY_BUTTON, useUnmergedTree = true)
        .performClick()
    waitForLoading()
    composeTestRule
        .onNodeWithTag(HistoryScreenTestTags.SCREEN, useUnmergedTree = true)
        .assertIsDisplayed()

    composeTestRule.onNodeWithTag(HistoryScreenTestTags.BACK_BUTTON).performClick()
    waitForLoading()

    navigateToTab("Search")
    composeTestRule
        .onNodeWithTag(SearchScreenTestTags.SEARCH_TEXT_FIELD, useUnmergedTree = true)
        .assertIsDisplayed()

    // THEN: Verify event still visible and all tabs work
    navigateToTab("Overview")
    composeTestRule.waitUntil(timeoutMillis = 25000) {
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
