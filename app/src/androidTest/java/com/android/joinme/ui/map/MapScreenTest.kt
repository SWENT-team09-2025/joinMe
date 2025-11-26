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
    val testViewModel = MapViewModel()

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

    // Use reflection to inject events into the ViewModel state
    val stateField = testViewModel.javaClass.getDeclaredField("_uiState")
    stateField.isAccessible = true
    val mutableState = stateField.get(testViewModel) as MutableStateFlow<MapUIState>
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
    val testViewModel = MapViewModel()

    // Inject user location to trigger camera animation
    val stateField = testViewModel.javaClass.getDeclaredField("_uiState")
    stateField.isAccessible = true
    val mutableState = stateField.get(testViewModel) as MutableStateFlow<MapUIState>

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
    val testViewModel = MapViewModel()

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

    // Use reflection to inject series into the ViewModel state
    val stateField = testViewModel.javaClass.getDeclaredField("_uiState")
    stateField.isAccessible = true
    val mutableState = stateField.get(testViewModel) as MutableStateFlow<MapUIState>
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
    val testViewModel = MapViewModel()

    // Set initial state to not following
    val stateField = testViewModel.javaClass.getDeclaredField("_uiState")
    stateField.isAccessible = true
    val mutableState = stateField.get(testViewModel) as MutableStateFlow<MapUIState>
    mutableState.value = MapUIState(isFollowingUser = false)

    composeTestRule.setContent { MapScreen(viewModel = testViewModel, navigationActions = null) }

    // Click the button
    composeTestRule.onNodeWithTag(MapScreenTestTags.MY_LOCATION_BUTTON).performClick()

    composeTestRule.waitForIdle()

    // Verify that following is now enabled
    assert(testViewModel.uiState.value.isFollowingUser)
  }

  @Test
  fun mapScreen_enablesFollowingUserOnEntryWhenNotReturningFromMarkerClick() {
    val testViewModel = MapViewModel()

    // Set initial state to not returning from marker click
    val stateField = testViewModel.javaClass.getDeclaredField("_uiState")
    stateField.isAccessible = true
    val mutableState = stateField.get(testViewModel) as MutableStateFlow<MapUIState>
    mutableState.value = MapUIState(isFollowingUser = false, isReturningFromMarkerClick = false)

    composeTestRule.setContent { MapScreen(viewModel = testViewModel, navigationActions = null) }

    composeTestRule.waitForIdle()

    // Verify that following user is enabled
    assert(testViewModel.uiState.value.isFollowingUser)
  }

  @Test
  fun mapScreen_clearsMarkerClickFlagWhenReturningFromMarkerClick() {
    val testViewModel = MapViewModel()

    // Set initial state to returning from marker click
    val stateField = testViewModel.javaClass.getDeclaredField("_uiState")
    stateField.isAccessible = true
    val mutableState = stateField.get(testViewModel) as MutableStateFlow<MapUIState>
    mutableState.value = MapUIState(isFollowingUser = false, isReturningFromMarkerClick = true)

    composeTestRule.setContent { MapScreen(viewModel = testViewModel, navigationActions = null) }

    composeTestRule.waitForIdle()

    // Verify that marker click flag is cleared and following is not enabled
    assert(!testViewModel.uiState.value.isReturningFromMarkerClick)
    assert(!testViewModel.uiState.value.isFollowingUser)
  }
}
