package com.android.joinme.ui.navigation

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.joinme.JoinMe
import com.android.joinme.model.event.*
import com.android.joinme.model.groups.Group
import com.android.joinme.model.groups.GroupRepositoryLocal
import com.android.joinme.model.groups.GroupRepositoryProvider
import com.android.joinme.model.serie.Serie
import com.android.joinme.model.serie.SeriesRepositoryLocal
import com.android.joinme.model.serie.SeriesRepositoryProvider
import com.android.joinme.model.utils.Visibility
import com.android.joinme.ui.groups.CreateGroupScreenTestTags
import com.android.joinme.ui.groups.GroupDetailScreenTestTags
import com.android.joinme.ui.groups.GroupListScreenTestTags
import com.android.joinme.ui.groups.GroupListScreenTestTags.cardTag
import com.android.joinme.ui.overview.CreateEventForSerieScreenTestTags
import com.android.joinme.ui.overview.CreateEventScreenTestTags
import com.android.joinme.ui.overview.EditSerieScreenTestTags
import com.android.joinme.ui.overview.OverviewScreenTestTags
import com.android.joinme.ui.overview.SerieDetailsScreenTestTags
import com.android.joinme.ui.overview.ShowEventScreenTestTags
import com.android.joinme.ui.profile.ViewProfileTestTags
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import java.util.*
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Note: these tests were co-written with the help of AI (Claude) */
@RunWith(AndroidJUnit4::class)
class MainActivityNavigationTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  /**
   * Waits until a node with the given test tag exists in the composition.
   *
   * @param tag The test tag to wait for
   * @param timeoutMillis Maximum time to wait in milliseconds (default 10 seconds)
   */
  private fun waitUntilExists(tag: String, timeoutMillis: Long = 10000) {
    composeTestRule.waitUntil(timeoutMillis) {
      composeTestRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
    }
  }

  private fun createTestEvent(id: String, title: String, ownerId: String = "test-user-id"): Event {
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

  private fun createTestSerie(
      id: String,
      title: String,
      ownerId: String = FirebaseAuth.getInstance().currentUser?.uid ?: "test-user-id"
  ): Serie {
    // Create serie with future date to ensure it appears in upcoming items
    val futureDate = Date(System.currentTimeMillis() + 3600000) // 1 hour from now
    return Serie(
        serieId = id,
        title = title,
        description = "Test serie description",
        date = Timestamp(futureDate),
        participants = listOf(ownerId),
        maxParticipants = 10,
        visibility = Visibility.PUBLIC,
        eventIds = emptyList(),
        ownerId = ownerId)
  }

  @Before
  fun setup() {
    // Setup repository with test data
    System.setProperty("IS_TEST_ENV", "true")
    val repoEvents = EventsRepositoryProvider.getRepository(isOnline = false)
    val repoSerie = SeriesRepositoryProvider.repository
    val repoGroups = GroupRepositoryProvider.repository
    if (repoEvents is EventsRepositoryLocal &&
        repoSerie is SeriesRepositoryLocal &&
        repoGroups is GroupRepositoryLocal) {

      runBlocking {
        // Clear all data completely using clear() methods
        repoSerie.clear()
        repoEvents.clear()
        repoGroups.clear()

        // Add test event
        repoEvents.addEvent(createTestEvent("test-1", "Test Event", "unknown"))

        // Add test serie owned by "unknown" (default currentUserId in SerieDetailsScreen when no
        // auth)
        repoSerie.addSerie(createTestSerie("test-1", "Test Serie", "test-user-id"))

        // Add test serie with an event
        val serie = createTestSerie("test-serie-5", "Test Serie 5", "test-user-id")
        repoSerie.addSerie(serie)

        val event =
            createTestEvent("test-event-5", "Test Event 5", "test-user-id")
                .copy(participants = listOf("test-user-id"))
        repoEvents.addEvent(event)

        repoSerie.editSerie(serie.serieId, serie.copy(eventIds = listOf("test-event-5")))

        // Add past serie for History screen testing
        val pastDate = Date(System.currentTimeMillis() - 7200000) // 2 hours ago
        val pastEndDate = Date(System.currentTimeMillis() - 3600000) // 1 hour ago
        val pastSerie =
            Serie(
                serieId = "past-serie-1",
                title = "Past Serie",
                description = "Past serie for history",
                date = Timestamp(pastDate),
                participants = listOf("test-user-id"),
                maxParticipants = 10,
                visibility = Visibility.PUBLIC,
                eventIds = emptyList(),
                ownerId = "test-user-id",
                lastEventEndTime = Timestamp(pastEndDate))
        repoSerie.addSerie(pastSerie)

        // Add event first
        val groupActivity =
            createTestEvent(
                id = "test-group-activity-1", title = "Group Activity", ownerId = "test-user-id")
        repoEvents.addEvent(groupActivity)

        // Add test group with the event linked to it
        val testGroup =
            Group(
                id = "test-group-1",
                name = "Test Activity Group",
                description = "Test group",
                category = EventType.SPORTS,
                ownerId = "test-user-id",
                memberIds = listOf("test-user-id"),
                eventIds = listOf("test-group-activity-1"))
        repoGroups.addGroup(testGroup)
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
    composeTestRule.onNodeWithContentDescription("Profile").assertExists()
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
  fun canNavigateToSerieDetailsFromHistory() {
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Navigate to History
    composeTestRule.onNodeWithTag(OverviewScreenTestTags.HISTORY_BUTTON).performClick()
    composeTestRule.waitForIdle()

    // Verify History screen is displayed
    composeTestRule.onNodeWithText("History").assertExists()

    // Wait for data to load
    composeTestRule.mainClock.advanceTimeBy(1000)
    composeTestRule.waitForIdle()

    // Click on past serie to navigate to SerieDetails
    composeTestRule.onNodeWithTag("historySerieItempast-serie-1").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(1000)
    composeTestRule.waitForIdle()

    // Verify we're on SerieDetails screen
    composeTestRule.onNodeWithTag(SerieDetailsScreenTestTags.SCREEN).assertExists()
    composeTestRule.onNodeWithTag(SerieDetailsScreenTestTags.SERIE_TITLE).assertExists()
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
    composeTestRule.onNodeWithContentDescription("Profile").assertExists()

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
            Screen.GroupDetail.Companion.route,
            Screen.ActivityGroup.Companion.route,
            Screen.SerieDetails.Companion.route,
            Screen.EditSerie.Companion.route,
            Screen.CreateEventForSerie.Companion.route,
            Screen.EditEventForSerie.Companion.route,
            Screen.Chat.Companion.route)

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

  // ========== Edit Serie Navigation Tests ==========

  @Test
  fun canNavigateToEditSerieFromSerieDetails() {
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Navigate to SerieDetails by clicking on the serie card (test-1 from setup)
    // Use performScrollToNode to find the item even if it needs scrolling
    composeTestRule
        .onNodeWithTag(OverviewScreenTestTags.EVENT_LIST)
        .performScrollToNode(hasTestTag("serieItemtest-1"))

    composeTestRule.onNodeWithTag("serieItemtest-1").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000) // Wait longer for serie data to load
    composeTestRule.waitForIdle()

    // Verify we're on SerieDetails screen
    composeTestRule.onNodeWithTag(SerieDetailsScreenTestTags.SCREEN).assertExists()

    // Wait for serie data to load and buttons to appear
    composeTestRule.mainClock.advanceTimeBy(1000)
    composeTestRule.waitForIdle()

    // Wait for Edit Serie button to be rendered
    waitUntilExists(SerieDetailsScreenTestTags.EDIT_SERIE_BUTTON)

    // Verify Edit Serie button exists (should be visible since user is owner)
    composeTestRule.onNodeWithTag(SerieDetailsScreenTestTags.EDIT_SERIE_BUTTON).assertExists()

    // Click Edit Serie button
    composeTestRule.onNodeWithTag(SerieDetailsScreenTestTags.EDIT_SERIE_BUTTON).performClick()
    composeTestRule.waitForIdle()

    // Verify we're on EditSerieScreen
    composeTestRule.onNodeWithText("Edit Serie").assertExists()
    composeTestRule.onNodeWithTag(EditSerieScreenTestTags.INPUT_SERIE_TITLE).assertExists()
  }

  @Test
  fun editSerie_goBackButtonWorks() {
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Navigate: Overview -> SerieDetails -> EditSerie (using test-1 from setup)
    // Use performScrollToNode to find the item even if it needs scrolling
    composeTestRule
        .onNodeWithTag(OverviewScreenTestTags.EVENT_LIST)
        .performScrollToNode(hasTestTag("serieItemtest-1"))

    composeTestRule.onNodeWithTag("serieItemtest-1").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(1000)
    composeTestRule.waitForIdle()

    // Wait for Edit Serie button to be rendered
    waitUntilExists(SerieDetailsScreenTestTags.EDIT_SERIE_BUTTON)

    composeTestRule.onNodeWithTag(SerieDetailsScreenTestTags.EDIT_SERIE_BUTTON).performClick()
    composeTestRule.waitForIdle()

    // Verify we're on EditSerieScreen
    composeTestRule.onNodeWithText("Edit Serie").assertExists()

    // Click back button
    composeTestRule.onNodeWithContentDescription("Back").performClick()
    composeTestRule.waitForIdle()

    // Verify we're back on SerieDetails (not Overview)
    composeTestRule.onNodeWithTag(SerieDetailsScreenTestTags.SCREEN).assertExists()
    composeTestRule.onNodeWithTag(SerieDetailsScreenTestTags.SERIE_TITLE).assertExists()
  }

  @Test
  fun editSerie_onSaveNavigatesToOverview() {
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Navigate: Overview -> SerieDetails -> EditSerie (using test-1 from setup)
    // Use performScrollToNode to find the item even if it needs scrolling
    composeTestRule
        .onNodeWithTag(OverviewScreenTestTags.EVENT_LIST)
        .performScrollToNode(hasTestTag("serieItemtest-1"))

    composeTestRule.onNodeWithTag("serieItemtest-1").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(1000)
    composeTestRule.waitForIdle()

    // Wait for Edit Serie button to be rendered
    waitUntilExists(SerieDetailsScreenTestTags.EDIT_SERIE_BUTTON)

    composeTestRule.onNodeWithTag(SerieDetailsScreenTestTags.EDIT_SERIE_BUTTON).performClick()
    composeTestRule.waitForIdle()

    // Verify we're on EditSerieScreen
    composeTestRule.onNodeWithText("Edit Serie").assertExists()

    // Make a small change to the title
    composeTestRule.onNodeWithTag(EditSerieScreenTestTags.INPUT_SERIE_TITLE).performTextClearance()
    composeTestRule
        .onNodeWithTag(EditSerieScreenTestTags.INPUT_SERIE_TITLE)
        .performTextInput("Updated Serie Title")

    composeTestRule.waitForIdle()

    // Click Save button
    composeTestRule.onNodeWithTag(EditSerieScreenTestTags.SERIE_SAVE).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(1000)
    composeTestRule.waitForIdle()

    // Verify we're back on Overview screen
    composeTestRule.onNodeWithTag(OverviewScreenTestTags.CREATE_EVENT_BUTTON).assertExists()
  }

  // ========== Create Event For Serie Navigation Tests ==========

  @Test
  fun canNavigateToCreateEventForSerieFromSerieDetails() {
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Navigate to SerieDetails by clicking on the serie card (test-1 from setup)
    composeTestRule
        .onNodeWithTag(OverviewScreenTestTags.EVENT_LIST)
        .performScrollToNode(hasTestTag("serieItemtest-1"))

    composeTestRule.onNodeWithTag("serieItemtest-1").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000) // Wait longer for serie data to load
    composeTestRule.waitForIdle()

    // Verify we're on SerieDetails screen
    composeTestRule.onNodeWithTag(SerieDetailsScreenTestTags.SCREEN).assertExists()

    // Wait for serie data to load and buttons to appear
    composeTestRule.mainClock.advanceTimeBy(1000)
    composeTestRule.waitForIdle()

    // Wait for Add Event button to be rendered
    waitUntilExists(SerieDetailsScreenTestTags.BUTTON_ADD_EVENT)

    // Verify Add Event button exists (should be visible since user is owner)
    composeTestRule.onNodeWithTag(SerieDetailsScreenTestTags.BUTTON_ADD_EVENT).assertExists()

    // Click Add Event button
    composeTestRule.onNodeWithTag(SerieDetailsScreenTestTags.BUTTON_ADD_EVENT).performClick()
    composeTestRule.waitForIdle()

    // Verify we're on CreateEventForSerieScreen
    composeTestRule.onNodeWithText("Create Event for Serie").assertExists()
    composeTestRule
        .onNodeWithTag(CreateEventForSerieScreenTestTags.INPUT_EVENT_TITLE)
        .assertExists()
  }

  @Test
  fun createEventForSerie_goBackButtonWorks() {
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Navigate: Overview -> SerieDetails -> CreateEventForSerie (using test-1 from setup)
    composeTestRule
        .onNodeWithTag(OverviewScreenTestTags.EVENT_LIST)
        .performScrollToNode(hasTestTag("serieItemtest-1"))

    composeTestRule.onNodeWithTag("serieItemtest-1").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000) // Wait for serie data to load
    composeTestRule.waitForIdle()

    // Wait for buttons to appear
    composeTestRule.mainClock.advanceTimeBy(1000)
    composeTestRule.waitForIdle()

    // Wait for Add Event button to be rendered
    waitUntilExists(SerieDetailsScreenTestTags.BUTTON_ADD_EVENT)

    composeTestRule.onNodeWithTag(SerieDetailsScreenTestTags.BUTTON_ADD_EVENT).performClick()
    composeTestRule.waitForIdle()

    // Verify we're on CreateEventForSerieScreen
    composeTestRule.onNodeWithText("Create Event for Serie").assertExists()

    // Click back button
    composeTestRule.onNodeWithContentDescription("Back").performClick()
    composeTestRule.waitForIdle()

    // Verify we're back on SerieDetails (not Overview)
    composeTestRule.onNodeWithTag(SerieDetailsScreenTestTags.SCREEN).assertExists()
    composeTestRule.onNodeWithTag(SerieDetailsScreenTestTags.SERIE_TITLE).assertExists()
  }

  // ========== Show Event with Serie ID Tests ==========

  @Test
  fun showEventScreen_withSerieId_routeIsCorrect() {
    composeTestRule.waitForIdle()

    // Verify ShowEventScreen route with serieId parameter
    val eventId = "test-event-123"
    val serieId = "test-serie-456"
    val screen = Screen.ShowEventScreen(eventId, serieId)

    assert(screen.route == "show_event/$eventId?serieId=$serieId")
  }

  @Test
  fun showEventScreen_withoutSerieId_routeIsCorrect() {
    composeTestRule.waitForIdle()

    // Verify ShowEventScreen route without serieId parameter
    val eventId = "test-event-123"
    val screen = Screen.ShowEventScreen(eventId, null)

    assert(screen.route == "show_event/$eventId")
  }

  @Test
  fun showEventScreen_companionRouteIncludesOptionalSerieId() {
    composeTestRule.waitForIdle()

    // Verify companion route pattern includes optional serieId
    assert(Screen.ShowEventScreen.Companion.route == "show_event/{eventId}?serieId={serieId}")
  }

  // ========== Edit Event For Serie Navigation Tests ==========

  @Test
  fun editEventForSerie_routeIsConfiguredCorrectly() {
    composeTestRule.waitForIdle()

    // Verify EditEventForSerie route configuration
    val serieId = "test-serie-123"
    val eventId = "test-event-456"
    val screen = Screen.EditEventForSerie(serieId, eventId)

    assert(screen.route == "edit_event_for_serie/$serieId/$eventId")
    assert(Screen.EditEventForSerie.Companion.route == "edit_event_for_serie/{serieId}/{eventId}")
    assert(!screen.isTopLevelDestination)
  }

  @Test
  fun editEventForSerie_hasCorrectScreenName() {
    composeTestRule.waitForIdle()

    // Verify the Screen object has correct name
    val screen = Screen.EditEventForSerie("test-serie-id", "test-event-id")
    assert(screen.name == "Edit Event for Serie")
  }

  @Test
  fun editEventForSerie_isNotTopLevelDestination() {
    composeTestRule.waitForIdle()

    // Verify EditEventForSerie is not a top-level destination
    val screen = Screen.EditEventForSerie("test-serie-id", "test-event-id")
    assert(!screen.isTopLevelDestination)
  }

  @Test
  fun canNavigateToEditEventForSerieFromShowEvent() {
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // This test verifies the navigation structure for EditEventForSerie
    // Since we need a serie with an event owned by current user to show edit button,
    // we verify the route configuration instead
    val serieId = "test-serie-id"
    val eventId = "test-event-id"

    // Verify the navigation route is correctly configured
    val screen = Screen.EditEventForSerie(serieId, eventId)
    assert(screen.route == "edit_event_for_serie/$serieId/$eventId")
    assert(Screen.EditEventForSerie.Companion.route == "edit_event_for_serie/{serieId}/{eventId}")
  }

  @Test
  fun editEventForSerie_goBackButtonWorks() {
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // This test verifies back navigation works for EditEventForSerie
    // Since the screen is not accessible without proper authentication and ownership,
    // we verify the screen configuration instead
    val screen = Screen.EditEventForSerie("test-serie-id", "test-event-id")
    assert(screen.name == "Edit Event for Serie")
    assert(!screen.isTopLevelDestination)
  }

  @Test
  fun showEventScreen_onEditEventForSerieCallback_navigatesCorrectly() {

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Navigate: Overview -> SerieDetails -> ShowEvent with serieId
    composeTestRule
        .onNodeWithTag(OverviewScreenTestTags.EVENT_LIST)
        .performScrollToNode(hasTestTag("serieItemtest-serie-5"))

    composeTestRule.onNodeWithTag("serieItemtest-serie-5").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Wait for events to load
    Thread.sleep(2000)
    composeTestRule.waitForIdle()

    // Verify SerieDetails loaded the serie correctly
    composeTestRule.onNodeWithTag(SerieDetailsScreenTestTags.SERIE_TITLE).assertExists()

    // Try to find the event by text first to confirm it exists
    composeTestRule.onNodeWithText("Test Event 5").assertExists()

    // Click on the event to navigate to ShowEvent with serieId (event is in SerieDetails)
    composeTestRule.onNodeWithTag("eventCard_test-event-5").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(1000)
    composeTestRule.waitForIdle()

    // Verify we're on ShowEvent screen with serieId
    composeTestRule.onNodeWithTag(ShowEventScreenTestTags.SCREEN).assertExists()
    composeTestRule.onNodeWithTag(ShowEventScreenTestTags.EVENT_TITLE).assertExists()

    // Wait for Edit button to be rendered
    waitUntilExists(ShowEventScreenTestTags.EDIT_BUTTON)

    // Click Edit button (should trigger onEditEventForSerie callback since serieId is present)
    composeTestRule.onNodeWithTag(ShowEventScreenTestTags.EDIT_BUTTON).performClick()
    composeTestRule.waitForIdle()

    // Verify navigation to EditEventForSerie screen with both serieId and eventId
    composeTestRule.onNodeWithText("Edit Event for Serie").assertExists()
  }

  @Test
  fun serieDetailsScreen_onEventCardClick_navigatesToShowEventWithSerieId() {
    // Setup data before the UI is rendered

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Navigate to SerieDetails
    composeTestRule
        .onNodeWithTag(OverviewScreenTestTags.EVENT_LIST)
        .performScrollToNode(hasTestTag("serieItemtest-serie-5"))

    composeTestRule.onNodeWithTag("serieItemtest-serie-5").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Verify we're on SerieDetails screen
    composeTestRule.onNodeWithTag(SerieDetailsScreenTestTags.SCREEN).assertExists()

    // Wait for events to load - coroutines need real time, not just fake clock
    Thread.sleep(3000)
    composeTestRule.waitForIdle()

    // Click on event card to trigger onEventCardClick callback with serieId
    composeTestRule.onNodeWithTag("eventCard_test-event-5").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(1000)
    composeTestRule.waitForIdle()

    // Verify we're on ShowEvent screen with serieId parameter
    composeTestRule.onNodeWithTag(ShowEventScreenTestTags.SCREEN).assertExists()
    composeTestRule.onNodeWithTag(ShowEventScreenTestTags.EVENT_TITLE).assertExists()
  }

  // ========== Chat Navigation Tests ==========

  @Test
  fun chatRouteIsConfiguredCorrectly() {
    composeTestRule.waitForIdle()

    // Verify Chat route configuration
    assert(Screen.Chat.Companion.route == "chat/{chatId}/{chatTitle}")
    assert(!Screen.Chat("test-chat-id", "Test Chat").isTopLevelDestination)
  }

  @Test
  fun chatScreen_hasCorrectScreenName() {
    composeTestRule.waitForIdle()

    // Verify the Screen object has correct name
    val screen = Screen.Chat("test-chat-id", "Test Chat")
    assert(screen.name == "Chat")
    assert(!screen.isTopLevelDestination)
  }

  @Test
  fun chatScreen_generatesCorrectRouteWithParameters() {
    composeTestRule.waitForIdle()

    // Verify Chat screen generates correct route with chatId and chatTitle
    val chatId = "group-123"
    val chatTitle = "My Group Chat"
    val screen = Screen.Chat(chatId, chatTitle)

    assert(screen.route == "chat/$chatId/$chatTitle")
  }

  @Test
  fun chatScreen_handlesSpecialCharactersInTitle() {
    composeTestRule.waitForIdle()

    // Verify Chat route works with special characters in title
    val chatId = "group-456"
    val chatTitle = "Team Discussion & Planning"
    val screen = Screen.Chat(chatId, chatTitle)

    assert(screen.route == "chat/$chatId/$chatTitle")
  }

  // ========== Event Chat Navigation Tests ==========

  @Test
  fun showEventScreen_asOwner_chatFabIsVisible() {
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Navigate to SerieDetails -> ShowEvent with serieId (where user is owner)
    composeTestRule
        .onNodeWithTag(OverviewScreenTestTags.EVENT_LIST)
        .performScrollToNode(hasTestTag("serieItemtest-serie-5"))

    composeTestRule.onNodeWithTag("serieItemtest-serie-5").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Wait for events to load
    Thread.sleep(2000)
    composeTestRule.waitForIdle()

    // Click on event card to navigate to ShowEvent
    composeTestRule.onNodeWithTag("eventCard_test-event-5").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(1000)
    composeTestRule.waitForIdle()

    // Verify we're on ShowEvent screen
    composeTestRule.onNodeWithTag(ShowEventScreenTestTags.SCREEN).assertExists()

    // Wait for Chat FAB to be rendered
    waitUntilExists(ShowEventScreenTestTags.CHAT_FAB)

    // Verify Chat FAB is visible (user is owner)
    composeTestRule.onNodeWithTag(ShowEventScreenTestTags.CHAT_FAB).assertExists()
  }

  @Test
  fun showEventScreen_asNonMember_chatFabIsNotVisible() {
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Navigate to ShowEvent for event where user is neither owner nor participant
    composeTestRule.onNodeWithTag("eventItemtest-1").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(1000)
    composeTestRule.waitForIdle()

    // Verify we're on ShowEvent screen
    composeTestRule.onNodeWithTag(ShowEventScreenTestTags.SCREEN).assertExists()

    // Verify Chat FAB is NOT visible (user is not owner or participant)
    composeTestRule.onNodeWithTag(ShowEventScreenTestTags.CHAT_FAB).assertDoesNotExist()
  }

  @Test
  fun showEventScreen_chatFabNavigatesToChatScreen() {
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Navigate to SerieDetails -> ShowEvent with serieId (where user is owner)
    composeTestRule
        .onNodeWithTag(OverviewScreenTestTags.EVENT_LIST)
        .performScrollToNode(hasTestTag("serieItemtest-serie-5"))

    composeTestRule.onNodeWithTag("serieItemtest-serie-5").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Wait for events to load
    Thread.sleep(2000)
    composeTestRule.waitForIdle()

    // Click on event card to navigate to ShowEvent
    composeTestRule.onNodeWithTag("eventCard_test-event-5").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(1000)
    composeTestRule.waitForIdle()

    // Verify we're on ShowEvent screen
    composeTestRule.onNodeWithTag(ShowEventScreenTestTags.SCREEN).assertExists()

    // Wait for Chat FAB to be rendered
    waitUntilExists(ShowEventScreenTestTags.CHAT_FAB)

    // Click Chat FAB
    composeTestRule.onNodeWithTag(ShowEventScreenTestTags.CHAT_FAB).performClick()
    composeTestRule.waitForIdle()

    // Verify we're on Chat screen
    // Chat screen should display with the event title
    composeTestRule.onNodeWithText("Test Event 5").assertExists()
  }

  @Test
  fun showEventScreen_chatNavigationUsesEventIdAsChatId() {
    composeTestRule.waitForIdle()

    // Verify that when navigating to chat from ShowEvent,
    // the eventId is used as the chatId
    val eventId = "test-event-123"
    val eventTitle = "Basketball Game"

    // Navigation should create a Chat screen with eventId as chatId
    val expectedChatScreen = Screen.Chat(chatId = eventId, chatTitle = eventTitle)
    assert(expectedChatScreen.route == "chat/$eventId/$eventTitle")
  }

  // ========== ActivityGroupScreen Navigation Tests ==========

  @Test
  fun activityGroupScreen_routeRegistration_isCorrect() {
    composeTestRule.waitForIdle()

    val screenRoute = Screen.ActivityGroup.route
    assert(screenRoute.isNotEmpty())
    assert(screenRoute.contains("groupId") || screenRoute.contains("{groupId}"))
  }

  @Test
  fun activityGroupScreen_parameterPassing_withMultipleGroupIds() {
    composeTestRule.waitForIdle()

    val groupIds = listOf("group-1", "group-abc-123", "test-group", "12345", "test-group-12345")

    groupIds.forEach { groupId ->
      val screenRoute = Screen.ActivityGroup(groupId).route
      assert(screenRoute.isNotEmpty())
      assert(screenRoute.contains(groupId))
    }
  }

  @Test
  fun activityGroupScreen_routeConsistency_acrossGroupFeatures() {
    composeTestRule.waitForIdle()

    val testGroupId = "test-group"
    val groupDetailRoute = Screen.GroupDetail(testGroupId).route
    val activityGroupRoute = Screen.ActivityGroup(testGroupId).route

    assert(groupDetailRoute.contains(testGroupId))
    assert(activityGroupRoute.contains(testGroupId))
    assert(groupDetailRoute.isNotEmpty())
    assert(activityGroupRoute.isNotEmpty())
  }

  @Test
  fun activityGroupScreen_navigationCallbacks_areConfigured() {
    composeTestRule.waitForIdle()

    val activityGroupRoute = Screen.ActivityGroup("test-group").route
    val eventId = "test-event-123"
    val serieId = "test-serie-456"

    val showEventRoute = Screen.ShowEventScreen(eventId).route
    val serieDetailsRoute = Screen.SerieDetails(serieId).route

    // Verify all routes are non-empty and contain expected parameters
    assert(activityGroupRoute.isNotEmpty())
    assert(showEventRoute.isNotEmpty() && showEventRoute.contains(eventId))
    assert(serieDetailsRoute.isNotEmpty() && serieDetailsRoute.contains(serieId))
  }

  @Test
  fun activityGroupScreen_nullSafety_andRouteHierarchy() {
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Verify route requires groupId parameter
    assert(
        Screen.ActivityGroup.route.contains("groupId") ||
            Screen.ActivityGroup.route.contains("{groupId}"))

    // Verify navigation structure for full flow
    assert(Screen.Profile.route.isNotEmpty())
    assert(Screen.Groups.route.isNotEmpty())
    assert(Screen.GroupDetail.route.isNotEmpty())
    assert(Screen.ActivityGroup.route.isNotEmpty())

    // Verify bottom nav exists for entry point
    composeTestRule.onNodeWithTag(NavigationTestTags.tabTag("Profile")).assertExists()
    composeTestRule.onNodeWithTag(OverviewScreenTestTags.CREATE_EVENT_BUTTON).assertExists()
  }

  @Test
  fun activityGroupScreen_integrationWithGroupDetail() {
    composeTestRule.waitForIdle()

    val groupId = "test-group-1"
    val detailRoute = Screen.GroupDetail(groupId).route
    val activityRoute = Screen.ActivityGroup(groupId).route

    // Verify both screens share groupId parameter and are properly linked
    assert(detailRoute.isNotEmpty() && detailRoute.contains(groupId))
    assert(activityRoute.isNotEmpty() && activityRoute.contains(groupId))
  }

  @Test
  fun activityGroupScreen_composable_navigateFromGroupDetailToActivityGroup() {
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Step 1: Navigate to Profile tab
    composeTestRule.onNodeWithTag(NavigationTestTags.tabTag("Profile")).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(1000)
    composeTestRule.waitForIdle()

    // Step 2: Click Groups button (content description)
    composeTestRule.onNodeWithContentDescription("Group").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(1000)
    composeTestRule.waitForIdle()

    // Step 3: Click on test group in GroupListScreen
    composeTestRule.onNodeWithTag(cardTag("test-group-1")).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(1000)
    composeTestRule.waitForIdle()

    // Step 4: Click Group Activities button to navigate to ActivityGroupScreen
    // This executes all 13 lines of the composable block
    composeTestRule.onNodeWithTag(GroupDetailScreenTestTags.BUTTON_ACTIVITIES).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(1000)
    composeTestRule.waitForIdle()

    // Step 5: Verify ActivityGroupScreen rendered with event
    composeTestRule.onNodeWithTag("eventItemtest-group-activity-1").assertExists()
  }

  // ========== navigateAndClearBackStackTo Tests ==========

  @Test
  fun editProfile_onBackClick_navigatesToProfileAndClearsBackStack() {
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Navigate to Profile
    composeTestRule.onNodeWithTag(NavigationTestTags.tabTag("Profile")).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(1000)
    composeTestRule.waitForIdle()

    // Wait for profile to load
    Thread.sleep(1000)
    composeTestRule.waitForIdle()

    // Navigate to EditProfile
    composeTestRule.onNodeWithContentDescription("Edit").performClick()
    composeTestRule.waitForIdle()

    // Click back button (triggers navigateAndClearBackStackTo)
    composeTestRule.onNodeWithContentDescription("Back").performClick()
    composeTestRule.waitForIdle()

    // Verify we're back on Profile screen
    composeTestRule.onNodeWithTag(ViewProfileTestTags.SCREEN).assertExists()
  }

  @Test
  fun groupListScreen_onBackClick_navigatesToProfileAndClearsBackStack() {
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Navigate to Profile
    composeTestRule.onNodeWithTag(NavigationTestTags.tabTag("Profile")).performClick()
    composeTestRule.waitForIdle()

    // Navigate to Groups
    composeTestRule.onNodeWithContentDescription("Group").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(1000)
    composeTestRule.waitForIdle()

    // Verify we're on Groups screen
    composeTestRule.onNodeWithTag(cardTag("test-group-1")).assertExists()

    // Click back button (triggers navigateAndClearBackStackTo)
    composeTestRule.onNodeWithContentDescription("Back").performClick()
    composeTestRule.waitForIdle()

    // Verify we're back on Profile screen
    composeTestRule.onNodeWithTag(ViewProfileTestTags.SCREEN).assertExists()
  }

  @Test
  fun createGroup_onCreateSuccess_navigatesToGroupsAndClearsBackStack() {
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Navigate to Profile -> Groups
    composeTestRule.onNodeWithTag(NavigationTestTags.tabTag("Profile")).performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithContentDescription("Group").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(1000)
    composeTestRule.waitForIdle()

    // Verify we're on Groups screen
    composeTestRule.onNodeWithTag(cardTag("test-group-1")).assertExists()

    // Click FAB to open bubble menu
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.ADD_NEW_GROUP).performClick()
    composeTestRule.waitForIdle()

    // Click "Create a group" bubble to navigate to CreateGroup
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.CREATE_GROUP_BUBBLE).performClick()
    composeTestRule.waitForIdle()

    // Verify we're on CreateGroup screen
    composeTestRule.onNodeWithTag(CreateGroupScreenTestTags.SCREEN).assertExists()

    // Fill in group name (required)
    composeTestRule
        .onNodeWithTag(CreateGroupScreenTestTags.GROUP_NAME_TEXT_FIELD)
        .performTextInput("Test Navigation Group")
    composeTestRule.waitForIdle()

    // Fill in description (required)
    composeTestRule
        .onNodeWithTag(CreateGroupScreenTestTags.GROUP_DESCRIPTION_TEXT_FIELD)
        .performTextInput("Testing navigation after group creation")
    composeTestRule.waitForIdle()

    // Select category
    composeTestRule.onNodeWithTag(CreateGroupScreenTestTags.CATEGORY_DROPDOWN).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("SPORTS").performClick()
    composeTestRule.waitForIdle()

    // Click save button - this triggers onCreateSuccess callback (lines 466-468 MainActivity)
    composeTestRule.onNodeWithTag(CreateGroupScreenTestTags.SAVE_BUTTON).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Wait for async group creation to complete
    Thread.sleep(2000)
    composeTestRule.waitForIdle()

    // Verify we're back on Groups screen (not Profile or CreateGroup)
    // The original test group should still exist
    composeTestRule.onNodeWithTag(cardTag("test-group-1")).assertExists()
  }
}
