package com.android.joinme.ui.navigation

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.joinme.JoinMe
import com.android.joinme.model.event.*
import com.android.joinme.ui.overview.CreateEventScreenTestTags
import com.android.joinme.ui.overview.EditEventScreenTestTags
import com.android.joinme.ui.overview.OverviewScreenTestTags
import com.google.firebase.Timestamp
import java.util.*
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityNavigationTest {

  @get:Rule val composeTestRule = createComposeRule()

  private fun createTestEvent(id: String, title: String): Event {
    return Event(
        eventId = id,
        type = EventType.SPORTS,
        title = title,
        description = "Test description",
        location = null,
        date = Timestamp(Date()),
        duration = 60,
        participants = emptyList(),
        maxParticipants = 10,
        visibility = EventVisibility.PUBLIC,
        ownerId = "test-owner")
  }

  @Before
  fun setup() {
    // Setup repository with test data
    System.setProperty("IS_TEST_ENV", "true")
    val repo = EventsRepositoryProvider.getRepository(isOnline = false)
    if (repo is EventsRepositoryLocal) {
      runBlocking {
        // Clear existing events
        val events = repo.getAllEvents()
        events.forEach { repo.deleteEvent(it.eventId) }

        // Add test event
        repo.addEvent(createTestEvent("test-1", "Test Event"))
      }
    }

    composeTestRule.setContent { JoinMe(startDestination = Screen.Overview.route) }
  }

  @Test
  fun canNavigateToCreateEventScreenFromOverview() {
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Click FAB to navigate to CreateEvent
    composeTestRule.onNodeWithTag(OverviewScreenTestTags.CREATE_EVENT_BUTTON).performClick()
    composeTestRule.waitForIdle()

    // Verify we're on CreateEvent screen
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_TITLE).assertExists()
    composeTestRule.onNodeWithText("Create Event").assertExists()
  }

  @Test
  fun canNavigateToEditEventScreenFromOverview() {
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Click on event to navigate to EditEvent
    composeTestRule.onNodeWithText("Test Event").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(1000)
    composeTestRule.waitForIdle()

    // Verify we're on EditEvent screen
    composeTestRule.onNodeWithTag(EditEventScreenTestTags.INPUT_EVENT_TITLE).assertExists()
    composeTestRule.onNodeWithText("Edit Event").assertExists()
  }

  @Test
  fun createEvent_goBackButtonNavigatesToOverview() {
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Navigate to CreateEvent
    composeTestRule.onNodeWithTag(OverviewScreenTestTags.CREATE_EVENT_BUTTON).performClick()
    composeTestRule.waitForIdle()

    // Click back button
    composeTestRule.onNodeWithContentDescription("Back").performClick()
    composeTestRule.waitForIdle()

    // Verify we're back on Overview
    composeTestRule.onNodeWithTag(OverviewScreenTestTags.CREATE_EVENT_BUTTON).assertExists()
  }

  @Test
  fun editEvent_goBackButtonNavigatesToOverview() {
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Navigate to EditEvent
    composeTestRule.onNodeWithText("Test Event").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(1000)
    composeTestRule.waitForIdle()

    // Click back button
    composeTestRule.onNodeWithContentDescription("Back").performClick()
    composeTestRule.waitForIdle()

    // Verify we're back on Overview
    composeTestRule.onNodeWithTag(OverviewScreenTestTags.CREATE_EVENT_BUTTON).assertExists()
  }

  @Test
  fun canNavigateToSearchScreenFromBottomNav() {
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Click Search tab
    composeTestRule.onNodeWithTag(NavigationTestTags.tabTag("Search")).performClick()
    composeTestRule.waitForIdle()

    // Verify we're on Search screen
    composeTestRule.onNodeWithText("Search").assertExists()
  }

  @Test
  fun canNavigateToMapScreenFromBottomNav() {
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Click Map tab
    composeTestRule.onNodeWithTag(NavigationTestTags.tabTag("Map")).performClick()
    composeTestRule.waitForIdle()

    // Map screen navigated (no crash)
  }

  @Test
  fun canNavigateToProfileScreenFromBottomNav() {
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Click Profile tab
    composeTestRule.onNodeWithTag(NavigationTestTags.tabTag("Profile")).performClick()
    composeTestRule.waitForIdle()

    // Profile screen navigated (no crash)
  }

  @Test
  fun bottomNavIsDisplayedOnOverviewScreen() {
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Verify bottom nav is displayed
    composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU).assertIsDisplayed()
  }

  @Test
  fun topAppBarIsCorrectOnEditEventScreen() {
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Navigate to EditEvent
    composeTestRule.onNodeWithText("Test Event").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(1000)
    composeTestRule.waitForIdle()

    // Verify Edit Event title is displayed
    composeTestRule.onNodeWithText("Edit Event").assertExists()
  }

  @Test
  fun allBottomNavTabsAreAccessible() {
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    val tabs = listOf("Overview", "Search", "Map", "Profile")

    // Verify all tabs are visible and exist on Overview screen
    tabs.forEach { tab ->
      composeTestRule.onNodeWithTag(NavigationTestTags.tabTag(tab)).assertExists()
    }
  }

  @Test
  fun canNavigateBackToOverviewFromCreateEvent() {
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Start at Overview
    composeTestRule.onNodeWithTag(OverviewScreenTestTags.CREATE_EVENT_BUTTON).assertExists()

    // Navigate to CreateEvent
    composeTestRule.onNodeWithTag(OverviewScreenTestTags.CREATE_EVENT_BUTTON).performClick()
    composeTestRule.waitForIdle()

    // Go back
    composeTestRule.onNodeWithContentDescription("Back").performClick()
    composeTestRule.waitForIdle()

    // Verify we're back at Overview
    composeTestRule.onNodeWithTag(OverviewScreenTestTags.CREATE_EVENT_BUTTON).assertExists()
  }

  @Test
  fun canNavigateBackToOverviewFromEditEvent() {
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Start at Overview
    composeTestRule.onNodeWithTag(OverviewScreenTestTags.CREATE_EVENT_BUTTON).assertExists()

    // Navigate to EditEvent
    composeTestRule.onNodeWithText("Test Event").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(1000)
    composeTestRule.waitForIdle()

    // Go back
    composeTestRule.onNodeWithContentDescription("Back").performClick()
    composeTestRule.waitForIdle()

    // Verify we're back at Overview
    composeTestRule.onNodeWithTag(OverviewScreenTestTags.CREATE_EVENT_BUTTON).assertExists()
  }
}
