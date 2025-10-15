package com.android.joinme.ui.navigation

import com.android.joinme.HttpClientProvider
import org.junit.Assert.*
import org.junit.Test

/** Unit tests for JoinMe navigation configuration */
class JoinMeNavigationTest {

  @Test
  fun screen_Auth_hasCorrectRoute() {
    assertEquals("auth", Screen.Auth.route)
    assertEquals("Authentication", Screen.Auth.name)
    assertFalse(Screen.Auth.isTopLevelDestination)
  }

  @Test
  fun screen_Overview_hasCorrectRoute() {
    assertEquals("overview", Screen.Overview.route)
    assertEquals("Overview", Screen.Overview.name)
    assertTrue(Screen.Overview.isTopLevelDestination)
  }

  @Test
  fun screen_Search_hasCorrectRoute() {
    assertEquals("search", Screen.Search.route)
    assertEquals("Search", Screen.Search.name)
    assertTrue(Screen.Search.isTopLevelDestination)
  }

  @Test
  fun screen_Map_hasCorrectRoute() {
    assertEquals("map", Screen.Map.route)
    assertEquals("Map", Screen.Map.name)
    assertTrue(Screen.Map.isTopLevelDestination)
  }

  @Test
  fun screen_Profile_hasCorrectRoute() {
    assertEquals("profile", Screen.Profile.route)
    assertEquals("Profile", Screen.Profile.name)
    assertTrue(Screen.Profile.isTopLevelDestination)
  }

  @Test
  fun screen_CreateEvent_hasCorrectRoute() {
    assertEquals("create_event", Screen.CreateEvent.route)
    assertEquals("Create a new task", Screen.CreateEvent.name)
    assertFalse(Screen.CreateEvent.isTopLevelDestination)
  }

  @Test
  fun screen_History_hasCorrectRoute() {
    assertEquals("history", Screen.History.route)
    assertEquals("History", Screen.History.name)
    assertFalse(Screen.History.isTopLevelDestination)
  }

  @Test
  fun screen_EditEvent_hasCorrectRoutePattern() {
    assertEquals("edit_event/{eventId}", Screen.EditEvent.Companion.route)
  }

  @Test
  fun screen_EditEvent_generatesCorrectRouteWithId() {
    val eventId = "test-event-123"
    val editEventScreen = Screen.EditEvent(eventId)
    assertEquals("edit_event/$eventId", editEventScreen.route)
    assertEquals("Edit Event", editEventScreen.name)
    assertFalse(editEventScreen.isTopLevelDestination)
  }

  @Test
  fun screen_EditEvent_handlesSpecialCharactersInId() {
    val eventId = "test-event-with-special-chars-!@#"
    val editEventScreen = Screen.EditEvent(eventId)
    assertEquals("edit_event/$eventId", editEventScreen.route)
  }

  @Test
  fun topLevelDestinations_areCorrectlyFlagged() {
    val topLevelScreens = listOf(Screen.Overview, Screen.Search, Screen.Map, Screen.Profile)

    topLevelScreens.forEach { screen ->
      assertTrue("${screen.name} should be a top-level destination", screen.isTopLevelDestination)
    }
  }

  @Test
  fun nonTopLevelDestinations_areCorrectlyFlagged() {
    val nonTopLevelScreens =
        listOf(Screen.Auth, Screen.CreateEvent, Screen.EditEvent("test-id"), Screen.History)

    nonTopLevelScreens.forEach { screen ->
      assertFalse(
          "${screen.name} should not be a top-level destination", screen.isTopLevelDestination)
    }
  }

  @Test
  fun allScreenRoutes_areUnique() {
    val routes =
        listOf(
            Screen.Auth.route,
            Screen.Overview.route,
            Screen.Search.route,
            Screen.Map.route,
            Screen.Profile.route,
            Screen.CreateEvent.route,
            Screen.History.route,
            Screen.EditEvent.Companion.route)

    val uniqueRoutes = routes.toSet()
    assertEquals("All screen routes should be unique", routes.size, uniqueRoutes.size)
  }

  @Test
  fun allScreenRoutes_areNonEmpty() {
    val screens =
        listOf(
            Screen.Auth,
            Screen.Overview,
            Screen.Search,
            Screen.Map,
            Screen.Profile,
            Screen.CreateEvent,
            Screen.History)

    screens.forEach { screen ->
      assertTrue("${screen.name} route should not be empty", screen.route.isNotEmpty())
    }

    // Check EditEvent companion route
    assertTrue(
        "EditEvent route pattern should not be empty",
        Screen.EditEvent.Companion.route.isNotEmpty())
  }

  @Test
  fun allScreenNames_areNonEmpty() {
    val screens =
        listOf(
            Screen.Auth,
            Screen.Overview,
            Screen.Search,
            Screen.Map,
            Screen.Profile,
            Screen.CreateEvent,
            Screen.History,
            Screen.EditEvent("test-id"))

    screens.forEach { screen ->
      assertTrue("${screen.name} name should not be empty", screen.name.isNotEmpty())
    }
  }

  @Test
  fun editEventRoute_containsEventIdPlaceholder() {
    assertTrue(
        "EditEvent companion route should contain {eventId} placeholder",
        Screen.EditEvent.Companion.route.contains("{eventId}"))
  }

  @Test
  fun editEventRoute_followsExpectedPattern() {
    val pattern = "edit_event/\\{eventId\\}".toRegex()
    assertTrue(
        "EditEvent route should match pattern 'edit_event/{eventId}'",
        pattern.matches(Screen.EditEvent.Companion.route))
  }

  @Test
  fun httpClientProvider_isAccessible() {
    // Verify that HttpClientProvider singleton is accessible
    assertNotNull("HttpClientProvider should be accessible", HttpClientProvider.client)
  }

  @Test
  fun httpClientProvider_hasDefaultClient() {
    // Verify that HttpClientProvider has a default OkHttpClient
    val client = HttpClientProvider.client
    assertNotNull("HttpClientProvider should have a default client", client)
  }
}
