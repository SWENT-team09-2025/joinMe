package com.android.joinme.ui.navigation
/* CO-WRITE with Claude AI*/
import org.junit.Test

/** Unit tests for Screen sealed class and its properties. */
class ScreenTest {

  @Test
  fun allScreens_haveValidRoutes() {
    // Verifies that all Screen objects have valid, non-empty routes
    assert(Screen.Auth.route.isNotEmpty())
    assert(Screen.Overview.route.isNotEmpty())
    assert(Screen.Search.route.isNotEmpty())
    assert(Screen.Map.route.isNotEmpty())
    assert(Screen.Profile.route.isNotEmpty())
    assert(Screen.CreateEvent.route.isNotEmpty())
    assert(Screen.CreateSerie.route.isNotEmpty())
    assert(Screen.History.route.isNotEmpty())
    assert(Screen.Groups.route.isNotEmpty())
    assert(Screen.CreateGroup.route.isNotEmpty())
    assert(Screen.EditProfile.route.isNotEmpty())
    assert(Screen.CreateEventForSerie.Companion.route.isNotEmpty())
    assert(Screen.EditEvent.Companion.route.isNotEmpty())
    assert(Screen.ShowEventScreen.Companion.route.isNotEmpty())
    assert(Screen.EditGroup.Companion.route.isNotEmpty())
    assert(Screen.GroupDetail.Companion.route.isNotEmpty())
  }

  @Test
  fun allScreens_haveValidNames() {
    // Verifies that all Screen objects have valid, non-empty names
    assert(Screen.Auth.name.isNotEmpty())
    assert(Screen.Overview.name.isNotEmpty())
    assert(Screen.Search.name.isNotEmpty())
    assert(Screen.Map.name.isNotEmpty())
    assert(Screen.Profile.name.isNotEmpty())
    assert(Screen.CreateEvent.name.isNotEmpty())
    assert(Screen.CreateSerie.name.isNotEmpty())
    assert(Screen.History.name.isNotEmpty())
    assert(Screen.Groups.name.isNotEmpty())
    assert(Screen.CreateGroup.name.isNotEmpty())
    assert(Screen.EditProfile.name.isNotEmpty())
  }

  @Test
  fun topLevelDestinations_areFlaggedCorrectly() {
    // Verifies that top-level destinations (bottom nav items) are properly flagged
    assert(Screen.Overview.isTopLevelDestination)
    assert(Screen.Search.isTopLevelDestination)
    assert(Screen.Map.isTopLevelDestination)
    assert(Screen.Profile.isTopLevelDestination)
  }

  @Test
  fun nonTopLevelDestinations_areNotFlagged() {
    // Verifies that non-top-level destinations are not marked as top-level
    assert(!Screen.Auth.isTopLevelDestination)
    assert(!Screen.CreateEvent.isTopLevelDestination)
    assert(!Screen.CreateSerie.isTopLevelDestination)
    assert(!Screen.History.isTopLevelDestination)
    assert(!Screen.CreateGroup.isTopLevelDestination)
    assert(!Screen.Groups.isTopLevelDestination)
    assert(!Screen.EditProfile.isTopLevelDestination)
    assert(!Screen.ShowEventScreen("test-id").isTopLevelDestination)
    assert(!Screen.EditEvent("test-id").isTopLevelDestination)
    assert(!Screen.EditGroup("test-id").isTopLevelDestination)
    assert(!Screen.GroupDetail("test-id").isTopLevelDestination)
    assert(!Screen.CreateEventForSerie("test-id").isTopLevelDestination)
  }

  @Test
  fun parametrizedScreens_haveCorrectRouteFormat() {
    // Verifies that routes with parameters follow the correct format
    assert(Screen.EditEvent.Companion.route == "edit_event/{eventId}")
    assert(Screen.ShowEventScreen.Companion.route == "show_event/{eventId}")
    assert(Screen.EditGroup.Companion.route == "edit_group/{groupId}")
    assert(Screen.GroupDetail.Companion.route == "group_detail/{groupId}")
    assert(Screen.CreateEventForSerie.Companion.route == "create_event_for_serie/{serieId}")
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

    assert(editEvent.route == "edit_event/$testEventId")
    assert(showEvent.route == "show_event/$testEventId")
    assert(editGroup.route == "edit_group/$testGroupId")
    assert(groupDetail.route == "group_detail/$testGroupId")
    assert(createEventForSerie.route == "create_event_for_serie/$testSerieId")

    assert(editEvent.name.isNotEmpty())
    assert(showEvent.name.isNotEmpty())
    assert(editGroup.name.isNotEmpty())
    assert(groupDetail.name.isNotEmpty())
    assert(createEventForSerie.name.isNotEmpty())
  }

  @Test
  fun specificScreens_haveExpectedRoutes() {
    // Verifies specific route values for key screens
    assert(Screen.Auth.route == "auth")
    assert(Screen.Overview.route == "overview")
    assert(Screen.Search.route == "search")
    assert(Screen.Map.route == "map")
    assert(Screen.Profile.route == "profile")
    assert(Screen.CreateEvent.route == "create_event")
    assert(Screen.CreateSerie.route == "create_serie")
    assert(Screen.History.route == "history")
    assert(Screen.Groups.route == "groups")
    assert(Screen.CreateGroup.route == "create_group")
    assert(Screen.EditProfile.route == "edit_profile")
  }

  @Test
  fun specificScreens_haveExpectedNames() {
    // Verifies specific name values for key screens
    assert(Screen.Overview.name == "Overview")
    assert(Screen.Search.name == "Search")
    assert(Screen.Map.name == "Map")
    assert(Screen.Profile.name == "Profile")
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
    assert(topLevelCount == 4)
  }
}
