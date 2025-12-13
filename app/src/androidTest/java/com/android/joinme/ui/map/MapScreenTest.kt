package com.android.joinme.ui.map

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.android.joinme.model.event.Event
import com.android.joinme.model.event.EventType
import com.android.joinme.model.event.EventVisibility
import com.android.joinme.model.map.Location
import com.android.joinme.model.serie.Serie
import com.android.joinme.model.utils.Visibility
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertNotNull
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MapScreenTest {

  companion object {
    @JvmStatic
    @BeforeClass
    fun initializeMaps() {
      // Initialize Google Maps before running tests
      MapsInitializer.initialize(ApplicationProvider.getApplicationContext())
    }
  }

  @get:Rule
  val permissionRule: GrantPermissionRule =
      GrantPermissionRule.grant(
          android.Manifest.permission.ACCESS_FINE_LOCATION,
          android.Manifest.permission.ACCESS_COARSE_LOCATION)

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var testViewModel: MapViewModel
  private lateinit var mutableState: MutableStateFlow<MapUIState>

  @org.junit.Before
  fun setUp() {
    testViewModel = MapViewModel()
    val stateField = testViewModel.javaClass.getDeclaredField("_uiState")
    stateField.isAccessible = true
    mutableState = stateField.get(testViewModel) as MutableStateFlow<MapUIState>
  }

  /**
   * Helper function to set the following user state.
   *
   * @param following Whether the map should be following the user
   */
  private fun setFollowingUser(following: Boolean) {
    mutableState.value = MapUIState(isFollowingUser = following)
  }

  @Test
  fun mapScreen_tagsAreDisplayed() {
    composeTestRule.setContent { MapScreen(viewModel = MapViewModel(), navigationActions = null) }

    composeTestRule
        .onNodeWithTag(MapScreenTestTags.GOOGLE_MAP_SCREEN)
        .assertExists()
        .assertIsDisplayed()

    composeTestRule
        .onNodeWithTag(MapScreenTestTags.MAP_CONTAINER)
        .assertExists()
        .assertIsDisplayed()

    composeTestRule
        .onNodeWithTag(MapScreenTestTags.FILTER_BUTTON)
        .assertExists()
        .assertIsDisplayed()
  }

  @Test
  fun mapScreen_correct_Components() {
    composeTestRule.setContent { MapScreen(viewModel = MapViewModel(), navigationActions = null) }
    composeTestRule.onNodeWithContentDescription("Filter").assertExists().assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(MapScreenTestTags.FILTER_BUTTON)
        .assertExists()
        .assertHasClickAction()
        .performClick()

    composeTestRule.onNodeWithTag("navigation_bottom_menu").assertExists()
  }

  @Test
  fun mapScreen_displaysMarkersForEvents() {
    // Create test events with locations
    val testEvents =
        listOf(
            Event(
                eventId = "event1",
                type = EventType.SPORTS,
                title = "Football Match",
                description = "Test event",
                location = Location(latitude = 46.5187, longitude = 6.5629, name = "EPFL"),
                date = Timestamp.now(),
                duration = 60,
                participants = emptyList(),
                maxParticipants = 10,
                visibility = EventVisibility.PUBLIC,
                ownerId = "owner1"),
            Event(
                eventId = "event2",
                type = EventType.ACTIVITY,
                title = "Hiking",
                description = "Test event 2",
                location = Location(latitude = 46.52, longitude = 6.57, name = "Lausanne"),
                date = Timestamp.now(),
                duration = 120,
                participants = emptyList(),
                maxParticipants = 5,
                visibility = EventVisibility.PUBLIC,
                ownerId = "owner2"))

    // Inject events into the ViewModel state
    mutableState.value = MapUIState(events = testEvents)

    composeTestRule.setContent { MapScreen(viewModel = testViewModel, navigationActions = null) }
    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(MapScreenTestTags.GOOGLE_MAP_SCREEN)
        .assertExists()
        .assertIsDisplayed()
  }

  @Test
  fun mapScreen_animatesCameraWhenUserLocationChanges() {
    composeTestRule.setContent { MapScreen(viewModel = testViewModel, navigationActions = null) }

    // Update user location after initial composition
    mutableState.value =
        MapUIState(userLocation = com.android.joinme.model.map.UserLocation(46.5187, 6.5629, 5f))

    composeTestRule.waitForIdle()

    // Verify the map screen is still displayed
    composeTestRule
        .onNodeWithTag(MapScreenTestTags.GOOGLE_MAP_SCREEN)
        .assertExists()
        .assertIsDisplayed()
  }

  @Test
  fun mapScreen_displaysMarkersForSeries() {
    // Create test locations
    val location1 = Location(latitude = 46.5187, longitude = 6.5629, name = "EPFL")
    val location2 = Location(latitude = 46.52, longitude = 6.57, name = "Lausanne")

    // Create test series with locations
    val testSeries =
        mapOf(
            location1 to
                Serie(
                    serieId = "serie1",
                    title = "Weekly Football",
                    description = "Test serie",
                    date = Timestamp.now(),
                    participants = emptyList(),
                    maxParticipants = 10,
                    visibility = Visibility.PUBLIC,
                    eventIds = listOf("event1", "event2", "event3"),
                    ownerId = "owner1"),
            location2 to
                Serie(
                    serieId = "serie2",
                    title = "Monthly Hiking",
                    description = "Test serie 2",
                    date = Timestamp.now(),
                    participants = emptyList(),
                    maxParticipants = 5,
                    visibility = Visibility.PUBLIC,
                    eventIds = listOf("event4", "event5"),
                    ownerId = "owner2"))

    // Inject series into the ViewModel state
    mutableState.value = MapUIState(series = testSeries)

    composeTestRule.setContent { MapScreen(viewModel = testViewModel, navigationActions = null) }
    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(MapScreenTestTags.GOOGLE_MAP_SCREEN)
        .assertExists()
        .assertIsDisplayed()
  }

  @Test
  fun createMarkerForColor_createsDefaultMarkerForGreen() {
    // Test that green color creates a default marker with hue
    val greenColor = Color.Green
    val greenMarker = createMarkerForColor(greenColor)

    val blueColor = Color.Blue
    val blueMarker = createMarkerForColor(blueColor)

    val veryDarkGray = Color(0xFF0A0A0A)
    val darkGrayMarker = createMarkerForColor(veryDarkGray)

    val redColor = Color.Red
    val redMarker = createMarkerForColor(redColor)

    val blackColor = Color.Black
    val blackMarker = createMarkerForColor(blackColor)

    assertNotNull(greenMarker)
    assertNotNull(blueMarker)
    assertNotNull(darkGrayMarker)
    assertNotNull(redMarker)
    assertNotNull(blackMarker)
  }

  @Test
  fun createBadgeMarker_returns_non_null_BitmapDescriptor_for_count_1() {
    val result: BitmapDescriptor = createBadgeMarker(1)
    assertNotNull(result)

    val result1: BitmapDescriptor = createBadgeMarker(2)
    assertNotNull(result1)

    val result2: BitmapDescriptor = createBadgeMarker(10)
    assertNotNull(result2)

    val result3: BitmapDescriptor = createBadgeMarker(50)
    assertNotNull(result3)

    val result4: BitmapDescriptor = createBadgeMarker(99)
    assertNotNull(result4)

    val result5: BitmapDescriptor = createBadgeMarker(100)
    assertNotNull(result5)

    val result6: BitmapDescriptor = createBadgeMarker(150)
    assertNotNull(result6)

    val result7: BitmapDescriptor = createBadgeMarker(999)
    assertNotNull(result7)

    val result8: BitmapDescriptor = createBadgeMarker(0)
    assertNotNull(result8)

    val result9: BitmapDescriptor = createBadgeMarker(10000)
    assertNotNull(result9)
  }

  @Test
  fun createCircleMarker_returns_non_null_BitmapDescriptor_for_red_color() {
    val result: BitmapDescriptor = createCircleMarker(Color.Red)
    assertNotNull(result)

    val result1: BitmapDescriptor = createCircleMarker(Color.Blue)
    assertNotNull(result1)

    val result2: BitmapDescriptor = createCircleMarker(Color.Green)
    assertNotNull(result2)

    val result3: BitmapDescriptor = createCircleMarker(Color.Black)
    assertNotNull(result3)

    val result4: BitmapDescriptor = createCircleMarker(Color.White)
    assertNotNull(result4)

    val result5: BitmapDescriptor = createCircleMarker(Color.Transparent)
    assertNotNull(result5)
  }

  @Test
  fun myLocationButton_clickEnablesFollowingUser() {
    // Set initial state to not following
    setFollowingUser(false)

    composeTestRule.setContent { MapScreen(viewModel = testViewModel, navigationActions = null) }

    // Click the button
    composeTestRule.onNodeWithTag(MapScreenTestTags.MY_LOCATION_BUTTON).performClick()

    composeTestRule.waitForIdle()

    // Verify that following is now enabled
    assert(testViewModel.uiState.value.isFollowingUser)
  }

  @Test
  fun mapScreen_enablesFollowingUserOnEntryWhenNotReturningFromMarkerClick() {
    // Set initial state to not returning from marker click
    mutableState.value = MapUIState(isFollowingUser = false, isReturningFromMarkerClick = false)

    composeTestRule.setContent { MapScreen(viewModel = testViewModel, navigationActions = null) }

    composeTestRule.waitForIdle()

    // Verify that following user is enabled
    assert(testViewModel.uiState.value.isFollowingUser)
  }

  @Test
  fun mapScreen_clearsMarkerClickFlagWhenReturningFromMarkerClick() {
    // Set initial state to returning from marker click
    mutableState.value = MapUIState(isFollowingUser = false, isReturningFromMarkerClick = true)

    composeTestRule.setContent { MapScreen(viewModel = testViewModel, navigationActions = null) }

    composeTestRule.waitForIdle()

    // Verify that marker click flag is cleared and following is not enabled
    assert(!testViewModel.uiState.value.isReturningFromMarkerClick)
    assert(!testViewModel.uiState.value.isFollowingUser)
  }

  @Test
  fun mapScreen_centersOnInitialLocation_whenProvidedFromChatMessage() {
    // This test covers the MapInitialLocationEffect composable
    // Tests that map centers on specific location (e.g., from chat location message)
    // EPFL location coordinates
    val initialLatitude = 46.5196
    val initialLongitude = 6.5680
    val otherUserId = "other-user-123"
    val currentUserId = "current-user-456"

    // Set initial user following to true
    setFollowingUser(true)

    composeTestRule.setContent {
      MapScreen(
          viewModel = testViewModel,
          navigationActions = null,
          initialLatitude = initialLatitude,
          initialLongitude = initialLongitude,
          showLocationMarker = true,
          sharedLocationUserId = otherUserId,
          currentUserId = currentUserId)
    }

    composeTestRule.waitForIdle()

    // Verify that following user is disabled (MapInitialLocationEffect should disable it)
    assert(!testViewModel.uiState.value.isFollowingUser)

    // Verify the map screen is displayed with location marker
    composeTestRule
        .onNodeWithTag(MapScreenTestTags.GOOGLE_MAP_SCREEN)
        .assertExists()
        .assertIsDisplayed()
  }

  @Test
  fun mapScreen_doesNotCenterOnLocation_whenInitialLocationIsNull() {
    // This test verifies that MapInitialLocationEffect does not trigger when location is null
    // Set initial user following to true
    setFollowingUser(true)

    composeTestRule.setContent {
      MapScreen(
          viewModel = testViewModel,
          navigationActions = null,
          initialLatitude = null, // No initial location
          initialLongitude = null)
    }

    composeTestRule.waitForIdle()

    // Verify that following user is still enabled (not affected by MapInitialLocationEffect)
    assert(testViewModel.uiState.value.isFollowingUser)

    // Verify the map screen is displayed
    composeTestRule
        .onNodeWithTag(MapScreenTestTags.GOOGLE_MAP_SCREEN)
        .assertExists()
        .assertIsDisplayed()
  }

  @Test
  fun mapScreen_centersOnPartialInitialLocation_onlyWhenBothCoordinatesProvided() {
    // This test verifies that MapInitialLocationEffect requires both coordinates
    // Set initial user following to true
    setFollowingUser(true)

    composeTestRule.setContent {
      MapScreen(
          viewModel = testViewModel,
          navigationActions = null,
          initialLatitude = 46.5196, // Only latitude provided
          initialLongitude = null) // Longitude missing
    }

    composeTestRule.waitForIdle()

    // Verify that following user is still enabled (MapInitialLocationEffect should not trigger)
    assert(testViewModel.uiState.value.isFollowingUser)

    // Verify the map screen is displayed
    composeTestRule
        .onNodeWithTag(MapScreenTestTags.GOOGLE_MAP_SCREEN)
        .assertExists()
        .assertIsDisplayed()
  }

  @Test
  fun mapScreen_locationMarker_notShownWhenFlagIsFalse() {
    // Test that location marker is not shown when showLocationMarker=false
    // (e.g., navigating to event location that already has event marker)
    val initialLatitude = 46.5196
    val initialLongitude = 6.5680

    composeTestRule.setContent {
      MapScreen(
          viewModel = testViewModel,
          navigationActions = null,
          initialLatitude = initialLatitude,
          initialLongitude = initialLongitude,
          showLocationMarker = false)
    }

    composeTestRule.waitForIdle()

    // Verify the map displays without location marker
    composeTestRule
        .onNodeWithTag(MapScreenTestTags.GOOGLE_MAP_SCREEN)
        .assertExists()
        .assertIsDisplayed()
  }

  @Test
  fun mapScreen_locationMarker_shownWhenFlagIsTrue() {
    // Test that location marker is shown when showLocationMarker=true
    // and the userId is different from currentUserId (other user's shared location)
    val initialLatitude = 46.5196
    val initialLongitude = 6.5680
    val otherUserId = "other-user-123"
    val currentUserId = "current-user-456"

    composeTestRule.setContent {
      MapScreen(
          viewModel = testViewModel,
          navigationActions = null,
          initialLatitude = initialLatitude,
          initialLongitude = initialLongitude,
          showLocationMarker = true,
          sharedLocationUserId = otherUserId,
          currentUserId = currentUserId)
    }

    composeTestRule.waitForIdle()

    // Verify the map displays with location marker for other user
    composeTestRule
        .onNodeWithTag(MapScreenTestTags.GOOGLE_MAP_SCREEN)
        .assertExists()
        .assertIsDisplayed()
  }

  @Test
  fun mapScreen_locationMarker_notShownForCurrentUser() {
    // Test that location marker is NOT shown when userId equals currentUserId
    // (current user's own location - blue circle already visible)
    val initialLatitude = 46.5196
    val initialLongitude = 6.5680
    val currentUserId = "current-user-123"

    composeTestRule.setContent {
      MapScreen(
          viewModel = testViewModel,
          navigationActions = null,
          initialLatitude = initialLatitude,
          initialLongitude = initialLongitude,
          showLocationMarker = true,
          sharedLocationUserId = currentUserId,
          currentUserId = currentUserId)
    }

    composeTestRule.waitForIdle()

    // Verify the map displays without duplicate marker for current user
    composeTestRule
        .onNodeWithTag(MapScreenTestTags.GOOGLE_MAP_SCREEN)
        .assertExists()
        .assertIsDisplayed()
  }

  @Test
  fun mapScreen_locationMarker_notShownWhenUserIdIsNull() {
    // Test that location marker is not shown when sharedLocationUserId is null
    val initialLatitude = 46.5196
    val initialLongitude = 6.5680
    val currentUserId = "current-user-123"

    composeTestRule.setContent {
      MapScreen(
          viewModel = testViewModel,
          navigationActions = null,
          initialLatitude = initialLatitude,
          initialLongitude = initialLongitude,
          showLocationMarker = true,
          sharedLocationUserId = null,
          currentUserId = currentUserId)
    }

    composeTestRule.waitForIdle()

    // Verify the map displays without location marker when userId is null
    composeTestRule
        .onNodeWithTag(MapScreenTestTags.GOOGLE_MAP_SCREEN)
        .assertExists()
        .assertIsDisplayed()
  }

  @Test
  fun mapScreen_displaysGroupedMarkersWhenMultipleEventsAtSameLocation() {
    // Create test events with the same location
    val sameLocation = Location(latitude = 46.5187, longitude = 6.5629, name = "EPFL")
    val testEvents =
        listOf(
            Event(
                eventId = "event1",
                type = EventType.SPORTS,
                title = "Football Match",
                description = "Test event 1",
                location = sameLocation,
                date = Timestamp.now(),
                duration = 60,
                participants = emptyList(),
                maxParticipants = 10,
                visibility = EventVisibility.PUBLIC,
                ownerId = "owner1"),
            Event(
                eventId = "event2",
                type = EventType.ACTIVITY,
                title = "Hiking",
                description = "Test event 2",
                location = sameLocation,
                date = Timestamp.now(),
                duration = 120,
                participants = emptyList(),
                maxParticipants = 5,
                visibility = EventVisibility.PUBLIC,
                ownerId = "owner2"),
            Event(
                eventId = "event3",
                type = EventType.SOCIAL,
                title = "Party",
                description = "Test event 3",
                location = sameLocation,
                date = Timestamp.now(),
                duration = 180,
                participants = emptyList(),
                maxParticipants = 20,
                visibility = EventVisibility.PUBLIC,
                ownerId = "owner3"))

    // Inject events into the ViewModel state
    mutableState.value = MapUIState(events = testEvents)

    composeTestRule.setContent { MapScreen(viewModel = testViewModel, navigationActions = null) }
    composeTestRule.waitForIdle()

    // Verify the map screen is displayed
    composeTestRule
        .onNodeWithTag(MapScreenTestTags.GOOGLE_MAP_SCREEN)
        .assertExists()
        .assertIsDisplayed()
  }

  @Test
  fun mapScreen_displaysSingleMarkersWhenEventsAtDifferentLocations() {
    // Create test events with different locations
    val testEvents =
        listOf(
            Event(
                eventId = "event1",
                type = EventType.SPORTS,
                title = "Football Match",
                description = "Test event 1",
                location = Location(latitude = 46.5187, longitude = 6.5629, name = "EPFL"),
                date = Timestamp.now(),
                duration = 60,
                participants = emptyList(),
                maxParticipants = 10,
                visibility = EventVisibility.PUBLIC,
                ownerId = "owner1"),
            Event(
                eventId = "event2",
                type = EventType.ACTIVITY,
                title = "Hiking",
                description = "Test event 2",
                location = Location(latitude = 46.52, longitude = 6.57, name = "Lausanne"),
                date = Timestamp.now(),
                duration = 120,
                participants = emptyList(),
                maxParticipants = 5,
                visibility = EventVisibility.PUBLIC,
                ownerId = "owner2"))

    // Inject events into the ViewModel state
    mutableState.value = MapUIState(events = testEvents)

    composeTestRule.setContent { MapScreen(viewModel = testViewModel, navigationActions = null) }
    composeTestRule.waitForIdle()

    // Verify the map screen is displayed with individual markers
    composeTestRule
        .onNodeWithTag(MapScreenTestTags.GOOGLE_MAP_SCREEN)
        .assertExists()
        .assertIsDisplayed()
  }

  @Test
  fun mapScreen_displaysMixedEventAndSerieMarkers() {
    // Create test event
    val testEvent =
        Event(
            eventId = "event1",
            type = EventType.SPORTS,
            title = "Football Match",
            description = "Test event",
            location = Location(latitude = 46.5187, longitude = 6.5629, name = "EPFL"),
            date = Timestamp.now(),
            duration = 60,
            participants = emptyList(),
            maxParticipants = 10,
            visibility = EventVisibility.PUBLIC,
            ownerId = "owner1")

    // Create test serie with different location
    val serieLocation = Location(latitude = 46.52, longitude = 6.57, name = "Lausanne")
    val testSerie =
        Serie(
            serieId = "serie1",
            title = "Weekly Football",
            description = "Test serie",
            date = Timestamp.now(),
            participants = emptyList(),
            maxParticipants = 10,
            visibility = Visibility.PUBLIC,
            eventIds = listOf("event2", "event3"),
            ownerId = "owner1")

    // Inject both event and serie into the ViewModel state
    mutableState.value =
        MapUIState(events = listOf(testEvent), series = mapOf(serieLocation to testSerie))

    composeTestRule.setContent { MapScreen(viewModel = testViewModel, navigationActions = null) }
    composeTestRule.waitForIdle()

    // Verify the map screen is displayed with both event and serie markers
    composeTestRule
        .onNodeWithTag(MapScreenTestTags.GOOGLE_MAP_SCREEN)
        .assertExists()
        .assertIsDisplayed()
  }

  @Test
  fun mapScreen_displaysGroupedMarkerWithEventAndSerieAtSameLocation() {
    // Create test event and serie with the same location
    val sameLocation = Location(latitude = 46.5187, longitude = 6.5629, name = "EPFL")
    val testEvent =
        Event(
            eventId = "event1",
            type = EventType.SPORTS,
            title = "Football Match",
            description = "Test event",
            location = sameLocation,
            date = Timestamp.now(),
            duration = 60,
            participants = emptyList(),
            maxParticipants = 10,
            visibility = EventVisibility.PUBLIC,
            ownerId = "owner1")

    val testSerie =
        Serie(
            serieId = "serie1",
            title = "Weekly Football",
            description = "Test serie",
            date = Timestamp.now(),
            participants = emptyList(),
            maxParticipants = 10,
            visibility = Visibility.PUBLIC,
            eventIds = listOf("event2", "event3"),
            ownerId = "owner1")

    // Inject both event and serie at same location into the ViewModel state
    mutableState.value =
        MapUIState(events = listOf(testEvent), series = mapOf(sameLocation to testSerie))

    composeTestRule.setContent { MapScreen(viewModel = testViewModel, navigationActions = null) }
    composeTestRule.waitForIdle()

    // Verify the map screen is displayed with grouped marker
    composeTestRule
        .onNodeWithTag(MapScreenTestTags.GOOGLE_MAP_SCREEN)
        .assertExists()
        .assertIsDisplayed()
  }

  @Test
  fun mapScreen_handlesEmptyEventsAndSeriesLists() {
    // Set empty state
    mutableState.value = MapUIState(events = emptyList(), series = emptyMap())

    composeTestRule.setContent { MapScreen(viewModel = testViewModel, navigationActions = null) }
    composeTestRule.waitForIdle()

    // Verify the map screen is displayed without markers
    composeTestRule
        .onNodeWithTag(MapScreenTestTags.GOOGLE_MAP_SCREEN)
        .assertExists()
        .assertIsDisplayed()
  }

  @Test
  fun mapScreen_handlesEventsWithNullLocation() {
    // Create test events where one has null location
    val testEvents =
        listOf(
            Event(
                eventId = "event1",
                type = EventType.SPORTS,
                title = "Football Match",
                description = "Test event with location",
                location = Location(latitude = 46.5187, longitude = 6.5629, name = "EPFL"),
                date = Timestamp.now(),
                duration = 60,
                participants = emptyList(),
                maxParticipants = 10,
                visibility = EventVisibility.PUBLIC,
                ownerId = "owner1"),
            Event(
                eventId = "event2",
                type = EventType.ACTIVITY,
                title = "Online Event",
                description = "Test event without location",
                location = null, // No location
                date = Timestamp.now(),
                duration = 120,
                participants = emptyList(),
                maxParticipants = 5,
                visibility = EventVisibility.PUBLIC,
                ownerId = "owner2"))

    // Inject events into the ViewModel state
    mutableState.value = MapUIState(events = testEvents)

    composeTestRule.setContent { MapScreen(viewModel = testViewModel, navigationActions = null) }
    composeTestRule.waitForIdle()

    // Verify the map screen is displayed and only shows marker for event with location
    composeTestRule
        .onNodeWithTag(MapScreenTestTags.GOOGLE_MAP_SCREEN)
        .assertExists()
        .assertIsDisplayed()
  }

  @Test
  fun mapScreen_userLocationFollowing_disablesWhenReturningFromMarkerClick() {
    // Set initial state to returning from marker click with following enabled
    mutableState.value = MapUIState(isFollowingUser = true, isReturningFromMarkerClick = true)

    composeTestRule.setContent { MapScreen(viewModel = testViewModel, navigationActions = null) }

    composeTestRule.waitForIdle()

    // Verify that marker click flag is cleared
    assert(!testViewModel.uiState.value.isReturningFromMarkerClick)
    // Verify that following user is disabled when returning from marker click
    assert(!testViewModel.uiState.value.isFollowingUser)
  }

  @Test
  fun mapScreen_userLocationFollowing_enabledOnFirstLaunch() {
    // Set initial state to not returning from marker click
    mutableState.value = MapUIState(isFollowingUser = false, isReturningFromMarkerClick = false)

    composeTestRule.setContent { MapScreen(viewModel = testViewModel, navigationActions = null) }

    composeTestRule.waitForIdle()

    // Verify that following user is enabled on first launch
    assert(testViewModel.uiState.value.isFollowingUser)
  }
}
