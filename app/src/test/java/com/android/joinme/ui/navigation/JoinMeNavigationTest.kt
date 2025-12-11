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
    val map = Screen.Map()
    assertEquals("map", map.route)
    assertEquals("Map", map.name)
    assertTrue(map.isTopLevelDestination)
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
  fun screen_Calendar_hasCorrectRoute() {
    assertEquals("calendar", Screen.Calendar.route)
    assertEquals("Calendar", Screen.Calendar.name)
    assertFalse(Screen.Calendar.isTopLevelDestination)
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
    assertEquals("show_event/{eventId}?serieId={serieId}", Screen.ShowEventScreen.Companion.route)
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
  fun screen_ShowEventScreen_generatesCorrectRouteWithSerieId() {
    val eventId = "test-event-456"
    val serieId = "test-serie-789"
    val showEventScreen = Screen.ShowEventScreen(eventId, serieId)
    assertEquals("show_event/$eventId?serieId=$serieId", showEventScreen.route)
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
    val topLevelScreens = listOf(Screen.Overview, Screen.Search, Screen.Map(), Screen.Profile)

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
            Screen.CreateSerie,
            Screen.EditEvent("test-id"),
            Screen.History,
            Screen.Calendar,
            Screen.Groups,
            Screen.CreateGroup,
            Screen.GroupDetail("test-id"),
            Screen.ShowEventScreen("test-id"),
            Screen.EditProfile,
            Screen.SerieDetails("test-id"),
            Screen.EditSerie("test-id"),
            Screen.CreateEventForSerie("test-id"),
            Screen.EditEventForSerie("test-serie-id", "test-event-id"),
            Screen.Chat("test-chat-id", "Test Chat"))

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
            Screen.Map().route,
            Screen.Profile.route,
            Screen.CreateEvent.route,
            Screen.CreateSerie.route,
            Screen.History.route,
            Screen.Calendar.route,
            Screen.EditEvent.Companion.route,
            Screen.Groups.route,
            Screen.CreateGroup.route,
            Screen.GroupDetail.Companion.route,
            Screen.ShowEventScreen.Companion.route,
            Screen.EditProfile.route,
            Screen.SerieDetails.Companion.route,
            Screen.EditSerie.Companion.route,
            Screen.CreateEventForSerie.Companion.route,
            Screen.EditEventForSerie.Companion.route,
            Screen.Chat.Companion.route)

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
            Screen.Map(),
            Screen.Profile,
            Screen.CreateEvent,
            Screen.History,
            Screen.Calendar,
            Screen.Groups,
            Screen.CreateGroup,
            Screen.EditProfile,
            Screen.CreateSerie)

    screens.forEach { screen ->
      assertTrue("${screen.name} route should not be empty", screen.route.isNotEmpty())
    }

    // Check companion routes for parameterized screens
    assertTrue(
        "EditEvent route pattern should not be empty",
        Screen.EditEvent.Companion.route.isNotEmpty())
    assertTrue(
        "GroupDetail route pattern should not be empty",
        Screen.GroupDetail.Companion.route.isNotEmpty())
    assertTrue(
        "ShowEventScreen route pattern should not be empty",
        Screen.ShowEventScreen.Companion.route.isNotEmpty())
    assertTrue("Chat route pattern should not be empty", Screen.Chat.Companion.route.isNotEmpty())
  }

  @Test
  fun allScreenNames_areNonEmpty() {
    val screens =
        listOf(
            Screen.Auth,
            Screen.Overview,
            Screen.Search,
            Screen.Map(),
            Screen.Profile,
            Screen.CreateEvent,
            Screen.CreateSerie,
            Screen.History,
            Screen.Calendar,
            Screen.EditEvent("test-id"),
            Screen.Groups,
            Screen.CreateGroup,
            Screen.GroupDetail("test-id"),
            Screen.ShowEventScreen("test-id"),
            Screen.EditProfile,
            Screen.Chat("test-chat-id", "Test Chat"))

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

  // ========== Chat Tests ==========

  @Test
  fun screen_Chat_generatesCorrectRouteWithParams() {
    val chatId = "test-chat-123"
    val chatTitle = "Test Chat"
    val totalParticipants = 1
    val chatScreen = Screen.Chat(chatId, chatTitle, totalParticipants)
    assertEquals("chat/$chatId/$chatTitle/$totalParticipants", chatScreen.route)
    assertEquals("Chat", chatScreen.name)
    assertFalse(chatScreen.isTopLevelDestination)
  }

  @Test
  fun screen_Chat_handlesSpecialCharactersInParams() {
    val chatId = "test-chat-!@#"
    val chatTitle = "Group Name with Spaces"
    val totalParticipants = 1
    val chatScreen = Screen.Chat(chatId, chatTitle, totalParticipants)
    assertEquals("chat/$chatId/$chatTitle/$totalParticipants", chatScreen.route)
  }

  @Test
  fun screen_Chat_handlesEmptyTitle() {
    val chatId = "test-chat-456"
    val chatTitle = ""
    val totalParticipants = 1
    val chatScreen = Screen.Chat(chatId, chatTitle, totalParticipants)
    assertEquals("chat/$chatId/$chatTitle/$totalParticipants", chatScreen.route)
  }

  @Test
  fun chatRoute_containsChatIdPlaceholder() {
    assertTrue(
        "Chat companion route should contain {chatId} placeholder",
        Screen.Chat.Companion.route.contains("{chatId}"))
  }

  @Test
  fun chatRoute_containsChatTitlePlaceholder() {
    assertTrue(
        "Chat companion route should contain {chatTitle} placeholder",
        Screen.Chat.Companion.route.contains("{chatTitle}"))
  }

  @Test
  fun chatRoute_followsExpectedPattern() {
    val pattern = "chat/\\{chatId\\}/\\{chatTitle\\}/\\{totalParticipants\\}".toRegex()
    assertTrue(
        "Chat route should match pattern 'chat/{chatId}/{chatTitle}/{totalParticipants}'",
        pattern.matches(Screen.Chat.Companion.route))
  }

  @Test
  fun screen_Chat_canUseEventIdAsChatId() {
    // Verify that event chat can use eventId as chatId
    val eventId = "event-789"
    val eventTitle = "Basketball Game"
    val totalParticipants = 1
    val chatScreen =
        Screen.Chat(chatId = eventId, chatTitle = eventTitle, totalParticipants = totalParticipants)
    assertEquals("chat/$eventId/$eventTitle/$totalParticipants", chatScreen.route)
    assertEquals("Chat", chatScreen.name)
    assertFalse(chatScreen.isTopLevelDestination)
  }

  @Test
  fun showEventScreenRoute_containsEventIdPlaceholder() {
    assertTrue(
        "ShowEventScreen companion route should contain {eventId} placeholder",
        Screen.ShowEventScreen.Companion.route.contains("{eventId}"))
  }

  @Test
  fun showEventScreenRoute_followsExpectedPattern() {
    val pattern = "show_event/\\{eventId\\}\\?serieId=\\{serieId\\}".toRegex()
    assertTrue(
        "ShowEventScreen route should match pattern 'show_event/{eventId}?serieId={serieId}'",
        pattern.matches(Screen.ShowEventScreen.Companion.route))
  }

  @Test
  fun showEventScreenRoute_containsSerieIdPlaceholder() {
    assertTrue(
        "ShowEventScreen companion route should contain {serieId} placeholder",
        Screen.ShowEventScreen.Companion.route.contains("{serieId}"))
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

  // ========== Serie Details Tests ==========

  @Test
  fun screen_SerieDetails_hasCorrectRoutePattern() {
    assertEquals("serie_details/{serieId}", Screen.SerieDetails.Companion.route)
  }

  @Test
  fun screen_SerieDetails_generatesCorrectRouteWithId() {
    val serieId = "test-serie-123"
    val serieDetailsScreen = Screen.SerieDetails(serieId)
    assertEquals("serie_details/$serieId", serieDetailsScreen.route)
    assertEquals("Serie Details", serieDetailsScreen.name)
    assertFalse(serieDetailsScreen.isTopLevelDestination)
  }

  @Test
  fun screen_SerieDetails_handlesSpecialCharactersInId() {
    val serieId = "test-serie-with-special-chars-!@#"
    val serieDetailsScreen = Screen.SerieDetails(serieId)
    assertEquals("serie_details/$serieId", serieDetailsScreen.route)
  }

  @Test
  fun serieDetailsRoute_containsSerieIdPlaceholder() {
    assertTrue(
        "SerieDetails companion route should contain {serieId} placeholder",
        Screen.SerieDetails.Companion.route.contains("{serieId}"))
  }

  // ========== Edit Serie Tests ==========

  @Test
  fun screen_EditSerie_hasCorrectRoutePattern() {
    assertEquals("edit_serie/{serieId}", Screen.EditSerie.Companion.route)
  }

  @Test
  fun screen_EditSerie_generatesCorrectRouteWithId() {
    val serieId = "test-serie-456"
    val editSerieScreen = Screen.EditSerie(serieId)
    assertEquals("edit_serie/$serieId", editSerieScreen.route)
    assertEquals("Edit Serie", editSerieScreen.name)
    assertFalse(editSerieScreen.isTopLevelDestination)
  }

  @Test
  fun screen_EditSerie_handlesSpecialCharactersInId() {
    val serieId = "test-serie-with-special-chars-!@#"
    val editSerieScreen = Screen.EditSerie(serieId)
    assertEquals("edit_serie/$serieId", editSerieScreen.route)
  }

  @Test
  fun editSerieRoute_containsSerieIdPlaceholder() {
    assertTrue(
        "EditSerie companion route should contain {serieId} placeholder",
        Screen.EditSerie.Companion.route.contains("{serieId}"))
  }

  // ========== Create Event For Serie Tests ==========

  @Test
  fun screen_CreateEventForSerie_hasCorrectRoutePattern() {
    assertEquals("create_event_for_serie/{serieId}", Screen.CreateEventForSerie.Companion.route)
  }

  @Test
  fun screen_CreateEventForSerie_generatesCorrectRouteWithId() {
    val serieId = "test-serie-789"
    val createEventForSerieScreen = Screen.CreateEventForSerie(serieId)
    assertEquals("create_event_for_serie/$serieId", createEventForSerieScreen.route)
    assertEquals("Create Event for Serie", createEventForSerieScreen.name)
    assertFalse(createEventForSerieScreen.isTopLevelDestination)
  }

  @Test
  fun screen_CreateEventForSerie_handlesSpecialCharactersInId() {
    val serieId = "test-serie-with-special-chars-!@#"
    val createEventForSerieScreen = Screen.CreateEventForSerie(serieId)
    assertEquals("create_event_for_serie/$serieId", createEventForSerieScreen.route)
  }

  @Test
  fun createEventForSerieRoute_containsSerieIdPlaceholder() {
    assertTrue(
        "CreateEventForSerie companion route should contain {serieId} placeholder",
        Screen.CreateEventForSerie.Companion.route.contains("{serieId}"))
  }

  // ========== Edit Event For Serie Tests ==========

  @Test
  fun screen_EditEventForSerie_hasCorrectRoutePattern() {
    assertEquals(
        "edit_event_for_serie/{serieId}/{eventId}", Screen.EditEventForSerie.Companion.route)
  }

  @Test
  fun screen_EditEventForSerie_generatesCorrectRouteWithIds() {
    val serieId = "test-serie-123"
    val eventId = "test-event-456"
    val editEventForSerieScreen = Screen.EditEventForSerie(serieId, eventId)
    assertEquals("edit_event_for_serie/$serieId/$eventId", editEventForSerieScreen.route)
    assertEquals("Edit Event for Serie", editEventForSerieScreen.name)
    assertFalse(editEventForSerieScreen.isTopLevelDestination)
  }

  @Test
  fun screen_EditEventForSerie_handlesSpecialCharactersInIds() {
    val serieId = "test-serie-!@#"
    val eventId = "test-event-$%^"
    val editEventForSerieScreen = Screen.EditEventForSerie(serieId, eventId)
    assertEquals("edit_event_for_serie/$serieId/$eventId", editEventForSerieScreen.route)
  }

  @Test
  fun editEventForSerieRoute_containsSerieIdPlaceholder() {
    assertTrue(
        "EditEventForSerie companion route should contain {serieId} placeholder",
        Screen.EditEventForSerie.Companion.route.contains("{serieId}"))
  }

  @Test
  fun editEventForSerieRoute_containsEventIdPlaceholder() {
    assertTrue(
        "EditEventForSerie companion route should contain {eventId} placeholder",
        Screen.EditEventForSerie.Companion.route.contains("{eventId}"))
  }
}
