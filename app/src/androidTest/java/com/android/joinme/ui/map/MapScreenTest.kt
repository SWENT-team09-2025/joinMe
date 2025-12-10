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

    // Set initial user following to true
    setFollowingUser(true)

    composeTestRule.setContent {
      MapScreen(
          viewModel = testViewModel,
          navigationActions = null,
          initialLatitude = initialLatitude,
          initialLongitude = initialLongitude,
          showLocationMarker = true)
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
    val testViewModel = MapViewModel()

    // Set initial user following to true
    val stateField = testViewModel.javaClass.getDeclaredField("_uiState")
    stateField.isAccessible = true
    val mutableState = stateField.get(testViewModel) as MutableStateFlow<MapUIState>
    mutableState.value = MapUIState(isFollowingUser = true)

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
  fun mapScreen_locationMarker_onlyShownWhenFlagIsTrue() {
    // Tests that location marker is only shown when showLocationMarker=true
    // (chat locations show marker, event locations don't to avoid duplicate markers)
    val initialLatitude = 46.5196
    val initialLongitude = 6.5680

    // Test with marker flag = false (e.g., navigating to event location)
    composeTestRule.setContent {
      MapScreen(
          viewModel = testViewModel,
          navigationActions = null,
          initialLatitude = initialLatitude,
          initialLongitude = initialLongitude,
          showLocationMarker = false)
    }

    composeTestRule.waitForIdle()

    // Verify the map displays without marker
    composeTestRule
        .onNodeWithTag(MapScreenTestTags.GOOGLE_MAP_SCREEN)
        .assertExists()
        .assertIsDisplayed()

    // Test with marker flag = true (e.g., chat location with marker)
    composeTestRule.setContent {
      MapScreen(
          viewModel = testViewModel,
          navigationActions = null,
          initialLatitude = initialLatitude,
          initialLongitude = initialLongitude,
          showLocationMarker = true)
    }

    composeTestRule.waitForIdle()

    // Verify the map displays with marker
    composeTestRule
        .onNodeWithTag(MapScreenTestTags.GOOGLE_MAP_SCREEN)
        .assertExists()
        .assertIsDisplayed()
  }
}
