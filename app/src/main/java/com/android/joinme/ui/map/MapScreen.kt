package com.android.joinme.ui.map

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.core.graphics.createBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.joinme.R
import com.android.joinme.model.event.getColor
import com.android.joinme.ui.map.MapScreenTestTags.getTestTagForMarker
import com.android.joinme.ui.map.userLocation.LocationServiceImpl
import com.android.joinme.ui.navigation.BottomNavigationMenu
import com.android.joinme.ui.navigation.NavigationActions
import com.android.joinme.ui.navigation.Screen
import com.android.joinme.ui.navigation.Tab
import com.android.joinme.ui.theme.Dimens
import com.android.joinme.ui.theme.customColors
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptor
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
  const val MY_LOCATION_BUTTON = "myLocationButton"

  fun getTestTagForMarker(id: String): String = "marker$id"
}

const val SNIPPET_MESSAGE = "Tap to see more & join me"
const val LOW_SATURATION_THRESHOLD = 0.1f
const val LOW_VALUE_THRESHOLD = 0.1f

/**
 * Creates a marker icon for Google Maps based on the given color.
 *
 * For black or very dark colors (where HSV saturation and value are very low), creates a custom
 * black bitmap marker. For other colors, uses the default marker with the color's hue.
 *
 * @param color The Compose Color to convert to a marker
 * @return A BitmapDescriptor for the marker
 */
internal fun createMarkerForColor(color: Color): BitmapDescriptor {
  val hsv = FloatArray(3)
  android.graphics.Color.colorToHSV(color.toArgb(), hsv)

  // Check if color is black or very dark (low saturation and low value)
  val isBlackish = hsv[1] < LOW_SATURATION_THRESHOLD && hsv[2] < LOW_VALUE_THRESHOLD

  return if (isBlackish) {
    // Create custom black marker (3x size to match default markers)
    val width = Dimens.PinMark.PIN_MARK_WIDTH
    val height = Dimens.PinMark.PIN_MARK_HEIGHT
    val bitmap = createBitmap(width, height)
    val canvas = Canvas(bitmap)

    // Create the pin shape with black paint
    val blackPaint = Paint()
    blackPaint.isAntiAlias = true
    blackPaint.color = android.graphics.Color.BLACK
    blackPaint.style = Paint.Style.FILL

    // Draw the pin circle (top part)
    val circleRadius = width / 2f - 4f
    canvas.drawCircle(width / 2f, circleRadius + 4f, circleRadius, blackPaint)

    // Draw the pin pointer (bottom part)
    val path =
        Path().apply {
          moveTo(width / 2f, height.toFloat())
          lineTo(width / 2f - circleRadius / 2f, circleRadius * 2f)
          lineTo(width / 2f + circleRadius / 2f, circleRadius * 2f)
          close()
        }
    canvas.drawPath(path, blackPaint)

    // Draw white circle in the middle
    val whitePaint = Paint()
    whitePaint.isAntiAlias = true
    whitePaint.color = android.graphics.Color.GRAY
    whitePaint.style = Paint.Style.FILL
    canvas.drawCircle(width / 2f, circleRadius + 4f, circleRadius / 2.5f, whitePaint)

    BitmapDescriptorFactory.fromBitmap(bitmap)
  } else {
    // Use default marker with hue
    BitmapDescriptorFactory.defaultMarker(hsv[0])
  }
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

  // --- Enable following user on screen entry (unless returning from marker double click) ---
  LaunchedEffect(Unit) {
    if (uiState.isReturningFromMarkerClick) {
      viewModel.clearMarkerClickFlag()
    } else {
      viewModel.enableFollowingUser()
    }
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
    viewModel.fetchLocalizableEvents()
  }

  // --- Initialize the map camera position ---
  val cameraPositionState = rememberCameraPositionState()

  val currentLat = uiState.userLocation?.latitude
  val currentLng = uiState.userLocation?.longitude
  val isFollowingUser = uiState.isFollowingUser

  // --- Track if we're programmatically animating the camera ---
  var isProgrammaticMove by remember { mutableStateOf(false) }

  // --- Track if the map has been initialized (to avoid detecting initial map movements) ---
  var isMapInitialized by remember { mutableStateOf(false) }

  // --- Center the map when the user location changes (only if following is enabled) ---
  LaunchedEffect(currentLat, currentLng, isFollowingUser) {
    if (currentLat != null && currentLng != null && isFollowingUser) {
      try {
        isProgrammaticMove = true
        cameraPositionState.animate(
            update = CameraUpdateFactory.newLatLngZoom(LatLng(currentLat, currentLng), 15f),
            durationMs = 1000)
        isProgrammaticMove = false

        if (!isMapInitialized) {
          isMapInitialized = true
        }
      } catch (e: Exception) {
        isProgrammaticMove = false
      }
    }
  }

  // --- Detect user interaction with the map to disable following ---
  LaunchedEffect(cameraPositionState) {
    snapshotFlow { cameraPositionState.isMoving }
        .collect { isMoving ->
          if (isMoving && !isProgrammaticMove && isFollowingUser && isMapInitialized) {
            viewModel.disableFollowingUser()
          }
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
                      MapUiSettings(zoomControlsEnabled = true, myLocationButtonEnabled = false)) {
                    uiState.events.forEach { event ->
                      event.location?.let { location ->
                        val position = LatLng(location.latitude, location.longitude)
                        val markerIcon = createMarkerForColor(event.type.getColor())

                        Marker(
                            state = MarkerState(position = position),
                            icon = markerIcon,
                            tag = getTestTagForMarker(event.eventId),
                            title = event.title,
                            snippet = SNIPPET_MESSAGE,
                            onInfoWindowClick = {
                              viewModel.onMarkerClick()
                              navigationActions?.navigateTo(Screen.ShowEventScreen(event.eventId))
                            })
                      }
                    }
                    uiState.series.forEach { (location, serie) ->
                      val position = LatLng(location.latitude, location.longitude)
                      val markerIcon = createMarkerForColor(MaterialTheme.customColors.seriePinMark)
                      Marker(
                          state = MarkerState(position = position),
                          icon = markerIcon,
                          tag = getTestTagForMarker(serie.serieId),
                          title = serie.title,
                          snippet = SNIPPET_MESSAGE,
                          onInfoWindowClick = {
                            viewModel.onMarkerClick()
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

              FloatingActionButton(
                  onClick = { viewModel.enableFollowingUser() },
                  modifier =
                      Modifier.align(Alignment.TopEnd)
                          .padding(Dimens.Padding.medium)
                          .testTag(MapScreenTestTags.MY_LOCATION_BUTTON),
                  containerColor =
                      if (isFollowingUser) MaterialTheme.colorScheme.primary
                      else MaterialTheme.colorScheme.surface) {
                    Icon(
                        imageVector = Icons.Filled.MyLocation,
                        contentDescription = "My Location",
                        tint =
                            if (isFollowingUser) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurface)
                  }
            }
      }
}
