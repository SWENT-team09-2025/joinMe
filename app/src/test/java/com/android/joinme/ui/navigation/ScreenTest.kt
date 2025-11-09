package com.android.joinme.ui.navigation
/* CO-WRITE with Claude AI*/
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Unit tests for Screen sealed class and its properties. */
class ScreenTest {

  @Test
  fun allScreens_haveValidRoutes() {
    // Verifies that all Screen objects have valid, non-empty routes
    assertTrue(Screen.Auth.route.isNotEmpty())
    assertTrue(Screen.Overview.route.isNotEmpty())
    assertTrue(Screen.Search.route.isNotEmpty())
    assertTrue(Screen.Map.route.isNotEmpty())
    assertTrue(Screen.Profile.route.isNotEmpty())
    assertTrue(Screen.CreateEvent.route.isNotEmpty())
    assertTrue(Screen.CreateSerie.route.isNotEmpty())
    assertTrue(Screen.History.route.isNotEmpty())
    assertTrue(Screen.Groups.route.isNotEmpty())
    assertTrue(Screen.CreateGroup.route.isNotEmpty())
    assertTrue(Screen.EditProfile.route.isNotEmpty())
  }

  @Test
  fun allScreens_haveValidNames() {
    // Verifies that all Screen objects have valid, non-empty names
    assertTrue(Screen.Auth.name.isNotEmpty())
    assertTrue(Screen.Overview.name.isNotEmpty())
    assertTrue(Screen.Search.name.isNotEmpty())
    assertTrue(Screen.Map.name.isNotEmpty())
    assertTrue(Screen.Profile.name.isNotEmpty())
    assertTrue(Screen.CreateEvent.name.isNotEmpty())
    assertTrue(Screen.CreateSerie.name.isNotEmpty())
    assertTrue(Screen.History.name.isNotEmpty())
    assertTrue(Screen.Groups.name.isNotEmpty())
    assertTrue(Screen.CreateGroup.name.isNotEmpty())
    assertTrue(Screen.EditProfile.name.isNotEmpty())
  }

  @Test
  fun topLevelDestinations_areFlaggedCorrectly() {
    // Verifies that top-level destinations (bottom nav items) are properly flagged
    assertTrue(Screen.Overview.isTopLevelDestination)
    assertTrue(Screen.Search.isTopLevelDestination)
    assertTrue(Screen.Map.isTopLevelDestination)
    assertTrue(Screen.Profile.isTopLevelDestination)
  }

  @Test
  fun nonTopLevelDestinations_areNotFlagged() {
    // Verifies that non-top-level destinations are not marked as top-level
    assertFalse(Screen.Auth.isTopLevelDestination)
    assertFalse(Screen.CreateEvent.isTopLevelDestination)
    assertFalse(Screen.CreateSerie.isTopLevelDestination)
    assertFalse(Screen.History.isTopLevelDestination)
    assertFalse(Screen.CreateGroup.isTopLevelDestination)
    assertFalse(Screen.Groups.isTopLevelDestination)
    assertFalse(Screen.EditProfile.isTopLevelDestination)
    assertFalse(Screen.ShowEventScreen("test-id").isTopLevelDestination)
    assertFalse(Screen.EditEvent("test-id").isTopLevelDestination)
    assertFalse(Screen.EditGroup("test-id").isTopLevelDestination)
    assertFalse(Screen.GroupDetail("test-id").isTopLevelDestination)
    assertFalse(Screen.CreateEventForSerie("test-id").isTopLevelDestination)
  }

  @Test
  fun parametrizedScreens_haveCorrectRouteFormat() {
    // Verifies that routes with parameters follow the correct format
    assertEquals("edit_event/{eventId}", Screen.EditEvent.Companion.route)
    assertEquals("show_event/{eventId}", Screen.ShowEventScreen.Companion.route)
    assertEquals("edit_group/{groupId}", Screen.EditGroup.Companion.route)
    assertEquals("groupId/{groupId}", Screen.GroupDetail.Companion.route)
    assertEquals("create_event_for_serie/{serieId}", Screen.CreateEventForSerie.Companion.route)
  }

  @Test
  fun parametrizedScreens_canBeInstantiatedWithIds() {
    // Verifies that parameterized screens can be instantiated with IDs
    val testEventId = "test-event-123"
    val testGroupId = "test-group-456"
    val testSerieId = "test-serie-789"

    val editEvent = Screen.EditEvent(testEventId)
    val showEvent = Screen.ShowEventScreen(testEventId)
    val editGroup = Screen.EditGroup(testGroupId)
    val groupDetail = Screen.GroupDetail(testGroupId)
    val createEventForSerie = Screen.CreateEventForSerie(testSerieId)

    assertEquals("edit_event/$testEventId", editEvent.route)
    assertEquals("show_event/$testEventId", showEvent.route)
    assertEquals("edit_group/$testGroupId", editGroup.route)
    assertEquals("groupId/$testGroupId", groupDetail.route)
    assertEquals("create_event_for_serie/$testSerieId", createEventForSerie.route)

    assertTrue(editEvent.name.isNotEmpty())
    assertTrue(showEvent.name.isNotEmpty())
    assertTrue(editGroup.name.isNotEmpty())
    assertTrue(groupDetail.name.isNotEmpty())
    assertTrue(createEventForSerie.name.isNotEmpty())
  }

  @Test
  fun specificScreens_haveExpectedRoutes() {
    // Verifies specific route values for key screens
    assertEquals("auth", Screen.Auth.route)
    assertEquals("overview", Screen.Overview.route)
    assertEquals("search", Screen.Search.route)
    assertEquals("map", Screen.Map.route)
    assertEquals("profile", Screen.Profile.route)
    assertEquals("create_event", Screen.CreateEvent.route)
    assertEquals("create_serie", Screen.CreateSerie.route)
    assertEquals("history", Screen.History.route)
    assertEquals("groups", Screen.Groups.route)
    assertEquals("create_group", Screen.CreateGroup.route)
    assertEquals("edit_profile", Screen.EditProfile.route)
  }

  @Test
  fun specificScreens_haveExpectedNames() {
    // Verifies specific name values for key screens
    assertEquals("Overview", Screen.Overview.name)
    assertEquals("Search", Screen.Search.name)
    assertEquals("Map", Screen.Map.name)
    assertEquals("Profile", Screen.Profile.name)
  }

  @Test
  fun onlyFourScreens_areTopLevelDestinations() {
    // Verifies that exactly 4 screens are marked as top-level (bottom nav items)
    val allScreens =
        listOf(
            Screen.Auth,
            Screen.Overview,
            Screen.Search,
            Screen.Map,
            Screen.Profile,
            Screen.CreateEvent,
            Screen.CreateSerie,
            Screen.History,
            Screen.Groups,
            Screen.CreateGroup,
            Screen.EditProfile)

    val topLevelCount = allScreens.count { it.isTopLevelDestination }
    assertEquals(4, topLevelCount)
  }
}
