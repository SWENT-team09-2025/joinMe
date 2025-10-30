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
  fun screen_CreateSerie_hasCorrectRoute() {
    assertEquals("create_serie", Screen.CreateSerie.route)
    assertEquals("Create a new serie", Screen.CreateSerie.name)
    assertFalse(Screen.CreateSerie.isTopLevelDestination)
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
  fun screen_Groups_hasCorrectRoute() {
    assertEquals("groups", Screen.Groups.route)
    assertEquals("Groups", Screen.Groups.name)
    assertFalse(Screen.Groups.isTopLevelDestination)
  }

  @Test
  fun screen_CreateGroup_hasCorrectRoute() {
    assertEquals("create_group", Screen.CreateGroup.route)
    assertEquals("Create Group", Screen.CreateGroup.name)
    assertFalse(Screen.CreateGroup.isTopLevelDestination)
  }

  @Test
  fun screen_GroupDetail_hasCorrectRoutePattern() {
    assertEquals("groupId/{groupId}", Screen.GroupDetail.Companion.route)
  }

  @Test
  fun screen_GroupDetail_generatesCorrectRouteWithId() {
    val groupId = "test-group-123"
    val groupDetailScreen = Screen.GroupDetail(groupId)
    assertEquals("groupId/$groupId", groupDetailScreen.route)
    assertEquals("Group Detail", groupDetailScreen.name)
    assertFalse(groupDetailScreen.isTopLevelDestination)
  }

  @Test
  fun screen_GroupDetail_handlesSpecialCharactersInId() {
    val groupId = "test-group-with-special-chars-!@#"
    val groupDetailScreen = Screen.GroupDetail(groupId)
    assertEquals("groupId/$groupId", groupDetailScreen.route)
  }

  @Test
  fun screen_ShowEventScreen_hasCorrectRoutePattern() {
    assertEquals("show_event/{eventId}", Screen.ShowEventScreen.Companion.route)
  }

  @Test
  fun screen_ShowEventScreen_generatesCorrectRouteWithId() {
    val eventId = "test-event-456"
    val showEventScreen = Screen.ShowEventScreen(eventId)
    assertEquals("show_event/$eventId", showEventScreen.route)
    assertEquals("Show Event", showEventScreen.name)
    assertFalse(showEventScreen.isTopLevelDestination)
  }

  @Test
  fun screen_EditProfile_hasCorrectRoute() {
    assertEquals("edit_profile", Screen.EditProfile.route)
    assertEquals("Edit Profile", Screen.EditProfile.name)
    assertFalse(Screen.EditProfile.isTopLevelDestination)
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
        listOf(
            Screen.Auth,
            Screen.CreateEvent,
            Screen.EditEvent("test-id"),
            Screen.History,
            Screen.Groups,
            Screen.CreateGroup,
            Screen.GroupDetail("test-id"),
            Screen.ShowEventScreen("test-id"),
            Screen.EditProfile)

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
            Screen.CreateSerie.route,
            Screen.History.route,
            Screen.EditEvent.Companion.route,
            Screen.Groups.route,
            Screen.CreateGroup.route,
            Screen.GroupDetail.Companion.route,
            Screen.ShowEventScreen.Companion.route,
            Screen.EditProfile.route)

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
            Screen.History,
            Screen.Groups,
            Screen.CreateGroup,
            Screen.EditProfile,
            Screen.CreateSerie)

    screens.forEach { screen ->
      assertTrue("${screen.name} route should not be empty", screen.route.isNotEmpty())
    }

    // Check EditEvent companion route
    assertTrue(
        "EditEvent route pattern should not be empty",
        Screen.EditEvent.Companion.route.isNotEmpty())
    assertTrue(
        "GroupDetail route pattern should not be empty",
        Screen.GroupDetail.Companion.route.isNotEmpty())
    assertTrue(
        "ShowEventScreen route pattern should not be empty",
        Screen.ShowEventScreen.Companion.route.isNotEmpty())
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
            Screen.CreateSerie,
            Screen.History,
            Screen.EditEvent("test-id"),
            Screen.Groups,
            Screen.CreateGroup,
            Screen.GroupDetail("test-id"),
            Screen.ShowEventScreen("test-id"),
            Screen.EditProfile)

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
  fun groupDetailRoute_containsGroupIdPlaceholder() {
    assertTrue(
        "GroupDetail companion route should contain {groupId} placeholder",
        Screen.GroupDetail.Companion.route.contains("{groupId}"))
  }

  @Test
  fun groupDetailRoute_followsExpectedPattern() {
    val pattern = "groupId/\\{groupId\\}".toRegex()
    assertTrue(
        "GroupDetail route should match pattern 'groupId/{groupId}'",
        pattern.matches(Screen.GroupDetail.Companion.route))
  }

  @Test
  fun showEventScreenRoute_containsEventIdPlaceholder() {
    assertTrue(
        "ShowEventScreen companion route should contain {eventId} placeholder",
        Screen.ShowEventScreen.Companion.route.contains("{eventId}"))
  }

  @Test
  fun showEventScreenRoute_followsExpectedPattern() {
    val pattern = "show_event/\\{eventId\\}".toRegex()
    assertTrue(
        "ShowEventScreen route should match pattern 'show_event/{eventId}'",
        pattern.matches(Screen.ShowEventScreen.Companion.route))
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
