package com.android.joinme.ui.map

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.joinme.R
import com.android.joinme.model.event.getColor
import com.android.joinme.ui.map.MapScreenTestTags.getTestTagForEventMarker
import com.android.joinme.ui.map.userLocation.LocationServiceImpl
import com.android.joinme.ui.navigation.BottomNavigationMenu
import com.android.joinme.ui.navigation.NavigationActions
import com.android.joinme.ui.navigation.Screen
import com.android.joinme.ui.navigation.Tab
import com.android.joinme.ui.theme.IconColor
import com.android.joinme.ui.theme.MapControlBackgroundColor
import com.android.joinme.ui.theme.SerieCardLayer3Color
import com.android.joinme.ui.theme.Dimens
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

object MapScreenTestTags {
  const val GOOGLE_MAP_SCREEN = "mapScreen"
  const val FILTER_BUTTON = "filterButton"
  const val MAP_CONTAINER = "mapContainer"

  fun getTestTagForEventMarker(eventId: String): String = "eventMarker$eventId"
}

const val SNIPPET_MESSAGE = "Tap to see more & join me"

/**
 * Converts a Compose Color to a hue value (0-360) for Google Maps marker coloring.
 *
 * @param color The Compose Color to convert
 * @return The hue value in degrees (0-360)
 */
private fun colorToHue(color: Color): Float {
  val hsv = FloatArray(3)
  android.graphics.Color.colorToHSV(color.toArgb(), hsv)
  return hsv[0]
}

/**
 * Displays the main map screen of the application.
 *
 * This composable handles:
 * - Initializing the user location service via the [MapViewModel].
 * - Requesting and managing location permissions.
 * - Displaying a Google Map centered on the user's location (if available).
 * - Rendering a bottom navigation menu and a filter button overlay.
 *
 * @param viewModel The [MapViewModel] managing location and UI state.
 * @param navigationActions Optional navigation actions for switching tabs or screens.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun MapScreen(viewModel: MapViewModel = viewModel(), navigationActions: NavigationActions? = null) {
  val context = LocalContext.current

  // --- Initialization of localisation service ---
  LaunchedEffect(Unit) { viewModel.initLocationService(LocationServiceImpl(context)) }

  // --- Collect the current UI state from the ViewModel ---
  val uiState by
      produceState(initialValue = MapUIState(), viewModel) {
        viewModel.uiState.collect { newState -> value = newState }
      }

  // --- Permissions management---
  val locationPermissionsState =
      rememberMultiplePermissionsState(
          listOf(
              android.Manifest.permission.ACCESS_FINE_LOCATION,
              android.Manifest.permission.ACCESS_COARSE_LOCATION,
          ))

  LaunchedEffect(Unit) {
    if (!locationPermissionsState.allPermissionsGranted) {
      locationPermissionsState.launchMultiplePermissionRequest()
    }
  }

  // --- Initialize the map camera position ---
  val cameraPositionState = rememberCameraPositionState()

  val currentLat = uiState.userLocation?.latitude
  val currentLng = uiState.userLocation?.longitude

  // --- Center the map when the user location changes ---
  LaunchedEffect(currentLat, currentLng) {
    if (currentLat != null && currentLng != null) {
      try {
        cameraPositionState.animate(
            update = CameraUpdateFactory.newLatLngZoom(LatLng(currentLat, currentLng), 15f),
            durationMs = 1000)
      } catch (e: Exception) {}
    }
  }

  // --- Map properties configuration ---
  val mapStyle =
      if (isSystemInDarkTheme()) {
        MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_dark)
      } else {
        null
      }

  val mapProperties =
      MapProperties(
          isMyLocationEnabled = locationPermissionsState.allPermissionsGranted,
          mapStyleOptions = mapStyle)

  // --- Composable Structure ---
  Scaffold(
      modifier = Modifier.fillMaxSize().testTag(MapScreenTestTags.GOOGLE_MAP_SCREEN),
      bottomBar = {
        BottomNavigationMenu(
            selectedTab = Tab.Map,
            onTabSelected = { tab -> navigationActions?.navigateTo(tab.destination) })
      }) { paddingValues ->
        Box(
            modifier =
                Modifier.fillMaxSize()
                    .padding(paddingValues)
                    .testTag(MapScreenTestTags.MAP_CONTAINER)) {
              GoogleMap(
                  modifier = Modifier.fillMaxSize(),
                  cameraPositionState = cameraPositionState,
                  properties = mapProperties,
                  uiSettings =
                      MapUiSettings(zoomControlsEnabled = true, myLocationButtonEnabled = true)) {
                    uiState.events.forEach { event ->
                      event.location?.let { location ->
                        val position = LatLng(location.latitude, location.longitude)
                        val hue = colorToHue(event.type.getColor())

                        Marker(
                            state = MarkerState(position = position),
                            icon = BitmapDescriptorFactory.defaultMarker(hue),
                            tag = getTestTagForEventMarker(event.eventId),
                            title = event.title,
                            snippet = SNIPPET_MESSAGE,
                            onInfoWindowClick = {
                              navigationActions?.navigateTo(Screen.ShowEventScreen(event.eventId))
                            })
                      }
                    }
                    uiState.series.forEach { (location, serie) ->
                      val position = LatLng(location.latitude, location.longitude)
                      val hue = colorToHue(SerieCardLayer3Color)
                      Marker(
                          state = MarkerState(position = position),
                          icon = BitmapDescriptorFactory.defaultMarker(hue),
                          tag = getTestTagForEventMarker(serie.serieId),
                          title = serie.title,
                          snippet = SNIPPET_MESSAGE,
                          onInfoWindowClick = {
                            navigationActions?.navigateTo(Screen.SerieDetails(serie.serieId))
                          })
                    }
                  }

              IconButton(
                  onClick = {
                    Toast.makeText(context, "Not yet implemented ", Toast.LENGTH_SHORT).show()
                  },
                  modifier =
                      Modifier.align(Alignment.TopStart)
                          .padding(Dimens.Padding.medium)
                          .testTag(MapScreenTestTags.FILTER_BUTTON)
                          .background(
                              color = MaterialTheme.colorScheme.surface,
                              shape = MaterialTheme.shapes.medium)) {
                    Icon(
                        imageVector = Icons.Filled.Tune,
                        contentDescription = "Filter",
                        tint = MaterialTheme.colorScheme.onSurface)
                  }
            }
      }
}
