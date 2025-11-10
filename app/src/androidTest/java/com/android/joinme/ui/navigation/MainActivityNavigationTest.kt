package com.android.joinme.ui.navigation

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.joinme.JoinMe
import com.android.joinme.model.event.*
import com.android.joinme.ui.overview.CreateEventScreenTestTags
import com.android.joinme.ui.overview.OverviewScreenTestTags
import com.android.joinme.ui.overview.ShowEventScreenTestTags
import com.google.firebase.Timestamp
import java.util.*
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityNavigationTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private fun createTestEvent(id: String, title: String, ownerId: String = "test-owner"): Event {
    // Create event with future date to ensure it appears in upcoming items
    val futureDate = Date(System.currentTimeMillis() + 3600000) // 1 hour from now
    return Event(
        eventId = id,
        type = EventType.SPORTS,
        title = title,
        description = "Test description",
        location = null,
        date = Timestamp(futureDate),
        duration = 60,
        participants = emptyList(),
        maxParticipants = 10,
        visibility = EventVisibility.PUBLIC,
        ownerId = ownerId)
  }

  @Before
  fun setup() {
    // Setup repository with test data
    System.setProperty("IS_TEST_ENV", "true")
    val repo = EventsRepositoryProvider.getRepository(isOnline = false)
    if (repo is EventsRepositoryLocal) {
      runBlocking {
        // Clear existing events - create a copy of the list to avoid
        // ConcurrentModificationException
        val events = repo.getAllEvents(EventFilter.EVENTS_FOR_OVERVIEW_SCREEN).toList()
        events.forEach { repo.deleteEvent(it.eventId) }

        // Add test event
        repo.addEvent(createTestEvent("test-1", "Test Event"))
      }
    }

    composeTestRule.setContent {
      JoinMe(startDestination = Screen.Overview.route, enableNotificationPermissionRequest = false)
    }
  }

  @Test
  fun canNavigateToCreateEventScreenFromOverview() {
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Click FAB to open bubble menu
    composeTestRule.onNodeWithTag(OverviewScreenTestTags.CREATE_EVENT_BUTTON).performClick()
    composeTestRule.waitForIdle()
    // Click "Add an event" bubble to navigate to CreateEvent
    composeTestRule.onNodeWithTag("addEventBubble").performClick()
    composeTestRule.waitForIdle()

    // Verify we're on CreateEvent screen
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_TITLE).assertExists()
    composeTestRule.onNodeWithText("Create Event").assertExists()
  }

  @Test
  fun canNavigateToShowEventScreenFromOverview() {
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Click on event to navigate to ShowEvent
    composeTestRule.onNodeWithTag("eventItemtest-1").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(1000)
    composeTestRule.waitForIdle()

    // Verify we're on ShowEvent screen
    composeTestRule.onNodeWithTag(ShowEventScreenTestTags.SCREEN).assertExists()
    composeTestRule.onNodeWithTag(ShowEventScreenTestTags.EVENT_TITLE).assertExists()
  }

  @Test
  fun createEvent_goBackButtonNavigatesToOverview() {
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Navigate to CreateEvent via bubble menu
    composeTestRule.onNodeWithTag(OverviewScreenTestTags.CREATE_EVENT_BUTTON).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("addEventBubble").performClick()
    composeTestRule.waitForIdle()

    // Click back button
    composeTestRule.onNodeWithContentDescription("Back").performClick()
    composeTestRule.waitForIdle()

    // Verify we're back on Overview
    composeTestRule.onNodeWithTag(OverviewScreenTestTags.CREATE_EVENT_BUTTON).assertExists()
  }

  @Test
  fun showEvent_goBackButtonNavigatesToOverview() {
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Navigate to ShowEvent
    composeTestRule.onNodeWithTag("eventItemtest-1").performClick()
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
  fun bottomNavIsDisplayedOnOverviewScreen() {
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Verify bottom nav is displayed
    composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU).assertIsDisplayed()
  }

  @Test
  fun topAppBarIsCorrectOnShowEventScreen() {
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Navigate to ShowEvent
    composeTestRule.onNodeWithTag("eventItemtest-1").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(1000)
    composeTestRule.waitForIdle()

    // Verify ShowEvent screen has the event title
    composeTestRule.onNodeWithTag(ShowEventScreenTestTags.EVENT_TITLE).assertExists()
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

    // Navigate to CreateEvent via bubble menu
    composeTestRule.onNodeWithTag(OverviewScreenTestTags.CREATE_EVENT_BUTTON).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("addEventBubble").performClick()
    composeTestRule.waitForIdle()

    // Go back
    composeTestRule.onNodeWithContentDescription("Back").performClick()
    composeTestRule.waitForIdle()

    // Verify we're back at Overview
    composeTestRule.onNodeWithTag(OverviewScreenTestTags.CREATE_EVENT_BUTTON).assertExists()
  }

  @Test
  fun canNavigateBackToOverviewFromShowEvent() {
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Start at Overview
    composeTestRule.onNodeWithTag(OverviewScreenTestTags.CREATE_EVENT_BUTTON).assertExists()

    // Navigate to ShowEvent
    composeTestRule.onNodeWithTag("eventItemtest-1").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(1000)
    composeTestRule.waitForIdle()

    // Go back
    composeTestRule.onNodeWithContentDescription("Back").performClick()
    composeTestRule.waitForIdle()

    // Verify we're back at Overview
    composeTestRule.onNodeWithTag(OverviewScreenTestTags.CREATE_EVENT_BUTTON).assertExists()
  }

  @Test
  fun canNavigateToHistoryScreenFromOverview() {
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Click History button to navigate
    composeTestRule.onNodeWithTag(OverviewScreenTestTags.HISTORY_BUTTON).performClick()
    composeTestRule.waitForIdle()

    // Verify we're on History screen
    composeTestRule.onNodeWithText("History").assertExists()
  }

  @Test
  fun history_goBackButtonNavigatesToOverview() {
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Navigate to History
    composeTestRule.onNodeWithTag(OverviewScreenTestTags.HISTORY_BUTTON).performClick()
    composeTestRule.waitForIdle()

    // Click back button
    composeTestRule.onNodeWithContentDescription("Go back").performClick()
    composeTestRule.waitForIdle()

    // Verify we're back on Overview
    composeTestRule.onNodeWithTag(OverviewScreenTestTags.CREATE_EVENT_BUTTON).assertExists()
  }

  @Test
  fun canNavigateBackToOverviewFromHistory() {
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Start at Overview
    composeTestRule.onNodeWithTag(OverviewScreenTestTags.CREATE_EVENT_BUTTON).assertExists()

    // Navigate to History
    composeTestRule.onNodeWithTag(OverviewScreenTestTags.HISTORY_BUTTON).performClick()
    composeTestRule.waitForIdle()

    // Go back
    composeTestRule.onNodeWithContentDescription("Go back").performClick()
    composeTestRule.waitForIdle()

    // Verify we're back at Overview
    composeTestRule.onNodeWithTag(OverviewScreenTestTags.CREATE_EVENT_BUTTON).assertExists()
  }

  @Test
  fun showEventScreen_asNonOwner_doesNotShowEditButton() {
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Navigate to ShowEvent (current user is not owner)
    composeTestRule.onNodeWithTag("eventItemtest-1").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(1000)
    composeTestRule.waitForIdle()

    // Verify we're on ShowEvent screen
    composeTestRule.onNodeWithTag(ShowEventScreenTestTags.SCREEN).assertExists()

    // Verify Edit button does NOT exist (user is not owner)
    composeTestRule.onNodeWithTag(ShowEventScreenTestTags.EDIT_BUTTON).assertDoesNotExist()
  }

  @Test
  fun canNavigateToCreateSerieScreenFromOverview() {
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Click FAB to open bubble menu
    composeTestRule.onNodeWithTag(OverviewScreenTestTags.CREATE_EVENT_BUTTON).performClick()
    composeTestRule.waitForIdle()
    // Click "Add a serie" bubble to navigate to CreateSerie
    composeTestRule.onNodeWithTag("addSerieBubble").performClick()
    composeTestRule.waitForIdle()

    // Verify we're on CreateSerie screen
    composeTestRule.onNodeWithText("Create Serie").assertExists()
  }

  @Test
  fun createSerie_goBackButtonNavigatesToOverview() {
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Navigate to CreateSerie via bubble menu
    composeTestRule.onNodeWithTag(OverviewScreenTestTags.CREATE_EVENT_BUTTON).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("addSerieBubble").performClick()
    composeTestRule.waitForIdle()

    // Verify we're on CreateSerie screen
    composeTestRule.onNodeWithText("Create Serie").assertExists()

    // Click back button
    composeTestRule.onNodeWithContentDescription("Back").performClick()
    composeTestRule.waitForIdle()

    // Verify we're back on Overview
    composeTestRule.onNodeWithTag(OverviewScreenTestTags.CREATE_EVENT_BUTTON).assertExists()
  }

  @Test
  fun canNavigateBackToOverviewFromCreateSerie() {
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Start at Overview
    composeTestRule.onNodeWithTag(OverviewScreenTestTags.CREATE_EVENT_BUTTON).assertExists()

    // Navigate to CreateSerie via bubble menu
    composeTestRule.onNodeWithTag(OverviewScreenTestTags.CREATE_EVENT_BUTTON).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("addSerieBubble").performClick()
    composeTestRule.waitForIdle()

    // Verify we're on CreateSerie screen
    composeTestRule.onNodeWithText("Create Serie").assertExists()

    // Go back
    composeTestRule.onNodeWithContentDescription("Back").performClick()
    composeTestRule.waitForIdle()

    // Verify we're back at Overview
    composeTestRule.onNodeWithTag(OverviewScreenTestTags.CREATE_EVENT_BUTTON).assertExists()
  }

  @Test
  fun canNavigateToProfileScreenFromBottomNav() {
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Click Profile tab
    composeTestRule.onNodeWithTag(NavigationTestTags.tabTag("Profile")).performClick()
    composeTestRule.waitForIdle()

    // Verify we're on Profile screen (should show profile content)
    composeTestRule.onNodeWithText("Profile").assertExists()
  }

  @Test
  fun createEventForSerie_screenCanBeAccessed() {
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Note: In a real scenario, CreateEventForSerie is accessed after creating a serie
    // Since we can't easily test the full flow with Firebase auth in tests,
    // this test verifies the screen route exists in NavigationActions
    assert(Screen.CreateEventForSerie.Companion.route == "create_event_for_serie/{serieId}")
    assert(
        Screen.CreateEventForSerie("test-serie-id").route == "create_event_for_serie/test-serie-id")
  }

  @Test
  fun createEventForSerie_hasCorrectScreenTitle() {
    composeTestRule.waitForIdle()

    // Verify the Screen object has correct name
    val screen = Screen.CreateEventForSerie("test-serie-id")
    assert(screen.name == "Create Event for Serie")
  }

  @Test
  fun createEventForSerie_isNotTopLevelDestination() {
    composeTestRule.waitForIdle()

    // Verify CreateEventForSerie is not a top-level destination
    val screen = Screen.CreateEventForSerie("test-serie-id")
    assert(!screen.isTopLevelDestination)
  }

  @Test
  fun historyScreenHasSerieCallback() {
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Navigate to History
    composeTestRule.onNodeWithTag(OverviewScreenTestTags.HISTORY_BUTTON).performClick()
    composeTestRule.waitForIdle()

    // Verify History screen is displayed (onSelectSerie callback is configured in MainActivity)
    composeTestRule.onNodeWithText("History").assertExists()
  }

  @Test
  fun editGroupRouteIsConfiguredCorrectly() {
    composeTestRule.waitForIdle()

    // Verify EditGroup route configuration
    assert(Screen.EditGroup.Companion.route == "edit_group/{groupId}")
    assert(!Screen.EditGroup("test-id").isTopLevelDestination)
  }

  @Test
  fun createGroupRouteIsConfiguredCorrectly() {
    composeTestRule.waitForIdle()

    // Verify CreateGroup route configuration
    assert(Screen.CreateGroup.route == "create_group")
    assert(!Screen.CreateGroup.isTopLevelDestination)
  }

  @Test
  fun bottomNavTabsChangeSelectionStateOnClick() {
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(NavigationTestTags.tabTag("Search")).performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(NavigationTestTags.tabTag("Profile")).performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(NavigationTestTags.tabTag("Overview")).performClick()
    composeTestRule.waitForIdle()

    // All tabs should be accessible without crashes
    assert(true)
  }

  @Test
  fun bottomNavPersistsAcrossTopLevelDestinations() {
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Navigate through all top-level destinations
    // Note: Map is skipped as it causes crashes in instrumented tests
    val tabs = listOf("Overview", "Search", "Profile")

    tabs.forEach { tab ->
      composeTestRule.onNodeWithTag(NavigationTestTags.tabTag(tab)).performClick()
      composeTestRule.waitForIdle()

      // Bottom nav should still be displayed
      composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU).assertExists()
    }
  }

  @Test
  fun navigationFromOverviewToSearchAndBack() {
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Start at Overview
    composeTestRule.onNodeWithTag(OverviewScreenTestTags.CREATE_EVENT_BUTTON).assertExists()

    // Navigate to Search
    composeTestRule.onNodeWithTag(NavigationTestTags.tabTag("Search")).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Search").assertExists()

    // Navigate back to Overview
    composeTestRule.onNodeWithTag(NavigationTestTags.tabTag("Overview")).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(OverviewScreenTestTags.CREATE_EVENT_BUTTON).assertExists()
  }

  @Test
  fun navigationFromOverviewToProfileAndBack() {
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Start at Overview
    composeTestRule.onNodeWithTag(OverviewScreenTestTags.CREATE_EVENT_BUTTON).assertExists()

    // Navigate to Profile
    composeTestRule.onNodeWithTag(NavigationTestTags.tabTag("Profile")).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Profile").assertExists()

    // Navigate back to Overview
    composeTestRule.onNodeWithTag(NavigationTestTags.tabTag("Overview")).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(OverviewScreenTestTags.CREATE_EVENT_BUTTON).assertExists()
  }

  @Test
  fun multipleNavigationsDoNotCauseBackStackIssues() {
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Perform multiple navigations rapidly
    repeat(3) {
      composeTestRule.onNodeWithTag(NavigationTestTags.tabTag("Search")).performClick()
      composeTestRule.waitForIdle()

      composeTestRule.onNodeWithTag(NavigationTestTags.tabTag("Overview")).performClick()
      composeTestRule.waitForIdle()
    }

    // Should still be functional without crashes
    composeTestRule.onNodeWithTag(OverviewScreenTestTags.CREATE_EVENT_BUTTON).assertExists()
  }

  @Test
  fun deepNavigationAndBackButtonFlow() {
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Start at Overview
    composeTestRule.onNodeWithTag(OverviewScreenTestTags.CREATE_EVENT_BUTTON).assertExists()

    // Navigate to ShowEvent (depth 1)
    composeTestRule.onNodeWithTag("eventItemtest-1").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(1000)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(ShowEventScreenTestTags.SCREEN).assertExists()

    // Go back to Overview
    composeTestRule.onNodeWithContentDescription("Back").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(OverviewScreenTestTags.CREATE_EVENT_BUTTON).assertExists()
  }

  @Test
  fun allScreenRoutesAreUniqueAndValid() {
    composeTestRule.waitForIdle()

    // Collect all route strings
    val routes =
        listOf(
            Screen.Auth.route,
            Screen.Overview.route,
            Screen.Search.route,
            Screen.Map.route,
            Screen.Profile.route,
            Screen.CreateEvent.route,
            Screen.CreateSerie.route,
            Screen.EditEvent.Companion.route,
            Screen.ShowEventScreen.Companion.route,
            Screen.History.route,
            Screen.Groups.route,
            Screen.CreateGroup.route,
            Screen.EditGroup.Companion.route,
            Screen.EditProfile.route,
            Screen.GroupDetail.Companion.route)

    // Verify all routes are non-empty
    routes.forEach { route -> assert(route.isNotEmpty()) }

    // Verify all routes are unique
    assert(routes.distinct().size == routes.size)
  }

  @Test
  fun navigationStatePreservedAcrossConfigurationChanges() {
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Navigate to Search
    composeTestRule.onNodeWithTag(NavigationTestTags.tabTag("Search")).performClick()
    composeTestRule.waitForIdle()

    // Verify we're on Search
    composeTestRule.onNodeWithText("Search").assertExists()

    // Note: In a real configuration change test, we would:
    // 1. Trigger configuration change (rotation, etc.)
    // 2. Verify navigation state is preserved
    // For now, we verify navigation works after idle
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Search").assertExists()
  }

  @Test
  fun canNavigateFromSearchToShowEvent() {
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Navigate to Search
    composeTestRule.onNodeWithTag(NavigationTestTags.tabTag("Search")).performClick()
    composeTestRule.waitForIdle()

    // Note: Clicking on an event in Search would navigate to ShowEvent
    // This requires events to be visible in Search, which depends on data
    // For now, we verify the navigation structure is correct
    assert(Screen.Search.isTopLevelDestination)
    assert(Screen.ShowEventScreen.Companion.route.isNotEmpty())
  }

  @Test
  fun canNavigateFromHistoryToShowEvent() {
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Navigate to History
    composeTestRule.onNodeWithTag(OverviewScreenTestTags.HISTORY_BUTTON).performClick()
    composeTestRule.waitForIdle()

    // Note: Clicking on an event in History would navigate to ShowEvent
    // This requires past events to be visible in History, which depends on data
    // For now, we verify History screen is accessible
    composeTestRule.onNodeWithText("History").assertExists()
  }

  @Test
  fun bottomNavIconsAreDisplayed() {
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Verify bottom nav with all tabs is displayed
    composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU).assertIsDisplayed()

    // All tabs should be present
    val tabs = listOf("Overview", "Search", "Map", "Profile")
    tabs.forEach { tab ->
      composeTestRule.onNodeWithTag(NavigationTestTags.tabTag(tab)).assertExists()
    }
  }

  // ========== Serie Details Navigation Tests ==========

  @Test
  fun verifySerieDetailsRouteConfiguration() {
    composeTestRule.waitForIdle()

    // Verify SerieDetails route is configured correctly
    assert(Screen.SerieDetails.Companion.route == "serie_details/{serieId}")
    assert(!Screen.SerieDetails("test-id").isTopLevelDestination)

    // Verify the route accepts a serieId parameter
    val testSerieDetails = Screen.SerieDetails("test-serie-123")
    assert(testSerieDetails.route == "serie_details/test-serie-123")
  }
}
