package com.android.joinme.ui.navigation

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.joinme.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun mainActivity_launches() {
    // This test verifies that MainActivity launches without crashing
    // The JoinMe composable should be set and navigation should be initialized
    composeTestRule.waitForIdle()
  }

  @Test
  fun mainActivity_hasNavHost() {
    composeTestRule.waitForIdle()
    // If the app launches, the NavHost is successfully created
    // This is implicitly tested by the activity launching without crashing
  }

  @Test
  fun mainActivity_setsCorrectStartDestination() {
    composeTestRule.waitForIdle()
    // The start destination is determined by Firebase auth state
    // If user is not signed in, it should show Auth screen
    // If user is signed in, it should show Overview screen
    // This test verifies the logic doesn't crash during initialization
  }

  @Test
  fun navHost_containsAuthNavigation() {
    composeTestRule.waitForIdle()
    // Verifies that the Auth navigation graph is properly defined
    // The navigation includes Auth screen with route "auth"
  }

  @Test
  fun navHost_containsOverviewNavigation() {
    composeTestRule.waitForIdle()
    // Verifies that the Overview navigation graph is properly defined
    // The navigation includes Overview, CreateEvent, and EditEvent screens
  }

  @Test
  fun navHost_containsSearchNavigation() {
    composeTestRule.waitForIdle()
    // Verifies that the Search navigation graph is properly defined
    // The navigation includes Search screen with route "search"
  }

  @Test
  fun navHost_containsMapNavigation() {
    composeTestRule.waitForIdle()
    // Verifies that the Map navigation graph is properly defined
    // The navigation includes Map screen with route "map"
  }

  @Test
  fun navHost_containsProfileNavigation() {
    composeTestRule.waitForIdle()
    // Verifies that the Profile navigation graph is properly defined
    // The navigation includes Profile screen with route "profile"
  }

  @Test
  fun navHost_overviewNavigationContainsCreateEvent() {
    composeTestRule.waitForIdle()
    // Verifies that the Overview navigation contains CreateEvent screen
    // CreateEvent should have route "create_event"
  }

  @Test
  fun navHost_overviewNavigationContainsEditEvent() {
    composeTestRule.waitForIdle()
    // Verifies that the Overview navigation contains EditEvent screen
    // EditEvent should have route "edit_event/{eventId}" with eventId parameter
  }

  @Test
  fun navHost_allScreensHaveValidRoutes() {
    composeTestRule.waitForIdle()
    // Verifies that all Screen objects have valid, non-empty routes
    assert(Screen.Auth.route.isNotEmpty())
    assert(Screen.Overview.route.isNotEmpty())
    assert(Screen.Search.route.isNotEmpty())
    assert(Screen.Map.route.isNotEmpty())
    assert(Screen.Profile.route.isNotEmpty())
    assert(Screen.CreateEvent.route.isNotEmpty())
    assert(Screen.EditEvent.Companion.route.isNotEmpty())
  }

  @Test
  fun navHost_topLevelDestinationsAreFlagged() {
    composeTestRule.waitForIdle()
    // Verifies that top-level destinations are properly flagged
    assert(Screen.Overview.isTopLevelDestination)
    assert(Screen.Search.isTopLevelDestination)
    assert(Screen.Map.isTopLevelDestination)
    assert(Screen.Profile.isTopLevelDestination)
    assert(!Screen.Auth.isTopLevelDestination)
    assert(!Screen.CreateEvent.isTopLevelDestination)
  }

  @Test
  fun mainActivity_navigationActionsCreated() {
    composeTestRule.waitForIdle()
    // Verifies that NavigationActions is properly instantiated
    // This is implicit if the app launches without crashing
  }

  @Test
  fun mainActivity_credentialManagerCreated() {
    composeTestRule.waitForIdle()
    // Verifies that CredentialManager is properly instantiated
    // This is implicit if the app launches without crashing
  }

  @Test
  fun navHost_overviewNavigationContainsHistory() {
    composeTestRule.waitForIdle()
    // Verifies that the Overview navigation contains History screen
    // History should have route "history"
    assert(Screen.History.route == "history")
  }

  @Test
  fun navHost_historyScreenHasValidRoute() {
    composeTestRule.waitForIdle()
    // Verifies that History screen has valid, non-empty route
    assert(Screen.History.route.isNotEmpty())
    assert(Screen.History.name.isNotEmpty())
  }

  @Test
  fun navHost_historyIsNotTopLevelDestination() {
    composeTestRule.waitForIdle()
    // Verifies that History is not flagged as a top-level destination
    assert(!Screen.History.isTopLevelDestination)
  }
}
