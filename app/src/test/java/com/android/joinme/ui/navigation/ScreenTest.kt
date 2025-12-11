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
    assertTrue(Screen.Map().route.isNotEmpty())
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
    assertTrue(Screen.Map().name.isNotEmpty())
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
    assertTrue(Screen.Map().isTopLevelDestination)
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
    assertFalse(Screen.SerieDetails("test-id").isTopLevelDestination)
    assertFalse(Screen.EditSerie("test-id").isTopLevelDestination)
    assertFalse(Screen.EditEventForSerie("test-serie-id", "test-event-id").isTopLevelDestination)
  }

  @Test
  fun parametrizedScreens_haveCorrectRouteFormat() {
    // Verifies that routes with parameters follow the correct format
    assertEquals("edit_event/{eventId}", Screen.EditEvent.Companion.route)
    assertEquals("show_event/{eventId}?serieId={serieId}", Screen.ShowEventScreen.Companion.route)
    assertEquals("edit_group/{groupId}", Screen.EditGroup.Companion.route)
    assertEquals("groupId/{groupId}", Screen.GroupDetail.Companion.route)
    assertEquals("create_event_for_serie/{serieId}", Screen.CreateEventForSerie.Companion.route)
    assertEquals("serie_details/{serieId}", Screen.SerieDetails.Companion.route)
    assertEquals("edit_serie/{serieId}", Screen.EditSerie.Companion.route)
    assertEquals(
        "edit_event_for_serie/{serieId}/{eventId}", Screen.EditEventForSerie.Companion.route)
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
    val serieDetails = Screen.SerieDetails(testSerieId)
    val editSerie = Screen.EditSerie(testSerieId)
    val editEventForSerie = Screen.EditEventForSerie(testSerieId, testEventId)

    assertEquals("edit_event/$testEventId", editEvent.route)
    assertEquals("show_event/$testEventId", showEvent.route)
    assertEquals("edit_group/$testGroupId", editGroup.route)
    assertEquals("groupId/$testGroupId", groupDetail.route)
    assertEquals("create_event_for_serie/$testSerieId", createEventForSerie.route)
    assertEquals("serie_details/$testSerieId", serieDetails.route)
    assertEquals("edit_serie/$testSerieId", editSerie.route)
    assertEquals("edit_event_for_serie/$testSerieId/$testEventId", editEventForSerie.route)

    assertTrue(editEvent.name.isNotEmpty())
    assertTrue(showEvent.name.isNotEmpty())
    assertTrue(editGroup.name.isNotEmpty())
    assertTrue(groupDetail.name.isNotEmpty())
    assertTrue(createEventForSerie.name.isNotEmpty())
    assertTrue(serieDetails.name.isNotEmpty())
    assertTrue(editSerie.name.isNotEmpty())
    assertTrue(editEventForSerie.name.isNotEmpty())
  }

  @Test
  fun specificScreens_haveExpectedRoutes() {
    // Verifies specific route values for key screens
    assertEquals("auth", Screen.Auth.route)
    assertEquals("overview", Screen.Overview.route)
    assertEquals("search", Screen.Search.route)
    assertEquals("map", Screen.Map().route)
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
    assertEquals("Map", Screen.Map().name)
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
            Screen.Map(),
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

  // ========== Screen.Map with Location Parameters Tests ==========

  @Test
  fun map_behavesCorrectlyWithAndWithoutParameters() {
    // Test without parameters (default route for bottom nav)
    val mapDefault = Screen.Map()
    assertEquals("map", mapDefault.route)
    assertEquals("Map", mapDefault.name)
    assertTrue(mapDefault.isTopLevelDestination)

    // Test with parameters (for location sharing navigation)
    val mapWithLocation = Screen.Map(latitude = 46.5197, longitude = 6.6323)
    assertEquals("map?lat=46.5197&lon=6.6323&marker=false", mapWithLocation.route)
    assertEquals("Map", mapWithLocation.name)
    assertTrue(mapWithLocation.isTopLevelDestination)

    // Test with location and marker
    val mapWithLocationAndMarker =
        Screen.Map(latitude = 46.5197, longitude = 6.6323, showMarker = true)
    assertEquals("map?lat=46.5197&lon=6.6323&marker=true", mapWithLocationAndMarker.route)
    assertEquals("Map", mapWithLocationAndMarker.name)
    assertTrue(mapWithLocationAndMarker.isTopLevelDestination)

    // Test companion route pattern and default route
    assertEquals("map?lat={lat}&lon={lon}&marker={marker}&userId={userId}", Screen.Map.route)
    assertEquals("map", Screen.Map.defaultRoute)
  }

  @Test
  fun map_handlesNullAndEdgeCaseCoordinates() {
    // Partial null should use default route
    assertEquals("map", Screen.Map(latitude = null, longitude = 6.6323).route)
    assertEquals("map", Screen.Map(latitude = 46.5197, longitude = null).route)
    assertEquals("map", Screen.Map(latitude = null, longitude = null).route)

    // Negative coordinates (Southern/Western hemispheres)
    assertEquals(
        "map?lat=-33.8688&lon=-151.2093&marker=false",
        Screen.Map(latitude = -33.8688, longitude = -151.2093).route)

    // Zero coordinates and extreme values
    assertEquals(
        "map?lat=0.0&lon=0.0&marker=false", Screen.Map(latitude = 0.0, longitude = 0.0).route)
    assertEquals(
        "map?lat=90.0&lon=180.0&marker=false", Screen.Map(latitude = 90.0, longitude = 180.0).route)
  }

  // ========== ShowEventScreen with SerieId Tests ==========

  @Test
  fun showEventScreen_withSerieId_generatesCorrectRoute() {
    val testEventId = "test-event-123"
    val testSerieId = "test-serie-456"

    val showEventWithSerie = Screen.ShowEventScreen(testEventId, testSerieId)
    assertEquals("show_event/$testEventId?serieId=$testSerieId", showEventWithSerie.route)
  }

  @Test
  fun showEventScreen_withoutSerieId_generatesCorrectRoute() {
    val testEventId = "test-event-123"

    val showEventWithoutSerie = Screen.ShowEventScreen(testEventId, null)
    assertEquals("show_event/$testEventId", showEventWithoutSerie.route)
  }

  @Test
  fun showEventScreen_withSerieId_hasCorrectName() {
    val showEventWithSerie = Screen.ShowEventScreen("test-event", "test-serie")
    assertEquals("Show Event", showEventWithSerie.name)
  }

  // ========== SerieDetails Tests ==========

  @Test
  fun serieDetails_hasCorrectRoutePattern() {
    assertEquals("serie_details/{serieId}", Screen.SerieDetails.Companion.route)
  }

  @Test
  fun serieDetails_generatesCorrectRoute() {
    val testSerieId = "test-serie-123"
    val serieDetails = Screen.SerieDetails(testSerieId)
    assertEquals("serie_details/$testSerieId", serieDetails.route)
    assertEquals("Serie Details", serieDetails.name)
  }

  // ========== EditSerie Tests ==========

  @Test
  fun editSerie_hasCorrectRoutePattern() {
    assertEquals("edit_serie/{serieId}", Screen.EditSerie.Companion.route)
  }

  @Test
  fun editSerie_generatesCorrectRoute() {
    val testSerieId = "test-serie-456"
    val editSerie = Screen.EditSerie(testSerieId)
    assertEquals("edit_serie/$testSerieId", editSerie.route)
    assertEquals("Edit Serie", editSerie.name)
  }

  // ========== EditEventForSerie Tests ==========

  @Test
  fun editEventForSerie_hasCorrectRoutePattern() {
    assertEquals(
        "edit_event_for_serie/{serieId}/{eventId}", Screen.EditEventForSerie.Companion.route)
  }

  @Test
  fun editEventForSerie_generatesCorrectRoute() {
    val testSerieId = "test-serie-789"
    val testEventId = "test-event-321"
    val editEventForSerie = Screen.EditEventForSerie(testSerieId, testEventId)
    assertEquals("edit_event_for_serie/$testSerieId/$testEventId", editEventForSerie.route)
    assertEquals("Edit Event for Serie", editEventForSerie.name)
  }

  @Test
  fun editEventForSerie_requiresBothIds() {
    val testSerieId = "serie-1"
    val testEventId = "event-1"
    val editEventForSerie = Screen.EditEventForSerie(testSerieId, testEventId)

    assertTrue(editEventForSerie.route.contains(testSerieId))
    assertTrue(editEventForSerie.route.contains(testEventId))
  }
}
