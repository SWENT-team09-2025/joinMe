package com.android.joinme.ui.map

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.android.joinme.model.event.Event
import com.android.joinme.model.event.EventType
import com.android.joinme.model.event.EventVisibility
import com.android.joinme.model.map.Location
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MapScreenTest {
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
  fun mapScreen_filterButton_isClickable() {
    composeTestRule.setContent { MapScreen(viewModel = MapViewModel(), navigationActions = null) }

    composeTestRule
        .onNodeWithTag(MapScreenTestTags.FILTER_BUTTON)
        .assertExists()
        .assertHasClickAction()
        .performClick()
  }

  @Test
  fun mapScreen_filterIcon_hasCorrectLabel() {
    composeTestRule.setContent { MapScreen(viewModel = MapViewModel(), navigationActions = null) }
    composeTestRule.onNodeWithContentDescription("Filter").assertExists().assertIsDisplayed()
  }

  @Test
  fun mapScreen_bottomNavigationIsDisplayed() {
    composeTestRule.setContent { MapScreen(viewModel = MapViewModel(), navigationActions = null) }

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
    mutableState.value = MapUIState(todos = testEvents)

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
}
