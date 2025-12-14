package com.android.joinme.ui.map

import android.Manifest
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.res.stringResource
import androidx.core.graphics.createBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.joinme.R
import com.android.joinme.model.event.getColor
import com.android.joinme.model.filter.FilterState
import com.android.joinme.ui.map.MapScreenTestTags.getTestTagForMarker
import com.android.joinme.ui.map.userLocation.LocationServiceImpl
import com.android.joinme.ui.navigation.BottomNavigationMenu
import com.android.joinme.ui.navigation.NavigationActions
import com.android.joinme.ui.navigation.Screen
import com.android.joinme.ui.navigation.Tab
import com.android.joinme.ui.theme.Dimens
import com.android.joinme.ui.theme.customColors
import com.android.joinme.ui.theme.getUserColor
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.CameraPositionState
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
  const val FILTER_BOTTOM_SHEET = "filterBottomSheet"
  const val FILTER_TYPE_SOCIAL = "filterTypeSocial"
  const val FILTER_TYPE_ACTIVITY = "filterTypeActivity"
  const val FILTER_TYPE_SPORT = "filterTypeSport"
  const val FILTER_PARTICIPATION_MY_EVENTS = "filterParticipationMyEvents"
  const val FILTER_PARTICIPATION_JOINED_EVENTS = "filterParticipationJoinedEvents"
  const val FILTER_PARTICIPATION_OTHER_EVENTS = "filterParticipationOtherEvents"
  const val FILTER_CLOSE_BUTTON = "filterCloseButton"
  const val LOCATION_MARKER_TAG = "locationMarker"

  fun getTestTagForMarker(id: String): String = "marker$id"
}

/** Diameter of the circle marker for shared locations */
const val CIRCLE_MARKER_SIZE = 48

const val LOW_SATURATION_THRESHOLD = 0.1f
const val LOW_VALUE_THRESHOLD = 0.1f
const val ONE_S_IN_MS = 1000
const val ZOOM_PROPORTION = 15f
const val CONTAINER_COLOR = true
const val NOT_CONTAINER_COLOR = false
const val BORDER_WIDTH_DP = 4

/**
 * Creates a circular marker icon for shared locations based on user color.
 *
 * Creates a filled circle with a white border to represent a user's shared location on the map,
 * similar to the current location indicator but with the user's unique color.
 *
 * @param color The user's color for the circle
 * @return A BitmapDescriptor for the circle marker
 */
internal fun createCircleMarker(color: Color): BitmapDescriptor {
  val bitmap = createBitmap(CIRCLE_MARKER_SIZE, CIRCLE_MARKER_SIZE)
  val canvas = Canvas(bitmap)

  // Draw outer white circle (border)
  val borderPaint =
      Paint().apply {
        this.color = android.graphics.Color.WHITE
        isAntiAlias = true
        style = Paint.Style.FILL
      }
  canvas.drawCircle(
      CIRCLE_MARKER_SIZE / 2f, CIRCLE_MARKER_SIZE / 2f, CIRCLE_MARKER_SIZE / 2f, borderPaint)

  // Draw inner colored circle
  val circlePaint =
      Paint().apply {
        this.color = color.toArgb()
        isAntiAlias = true
        style = Paint.Style.FILL
      }
  canvas.drawCircle(
      CIRCLE_MARKER_SIZE / 2f,
      CIRCLE_MARKER_SIZE / 2f,
      (CIRCLE_MARKER_SIZE / 2f) - BORDER_WIDTH_DP,
      circlePaint)

  return BitmapDescriptorFactory.fromBitmap(bitmap)
}

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
 * Handles camera positioning when centering on a specific location.
 *
 * This is used when navigating to a specific location (e.g., from a chat location message). It
 * centers the map on the provided coordinates and disables user following.
 *
 * @param initialLatitude Optional latitude to center on
 * @param initialLongitude Optional longitude to center on
 * @param cameraPositionState Camera position state to animate
 * @param onProgrammaticMoveStart Callback when programmatic move starts
 * @param onProgrammaticMoveEnd Callback when programmatic move ends
 * @param onDisableFollowing Callback to disable user following
 */
@Composable
private fun MapInitialLocationEffect(
    initialLatitude: Double?,
    initialLongitude: Double?,
    cameraPositionState: com.google.maps.android.compose.CameraPositionState,
    onProgrammaticMoveStart: () -> Unit,
    onProgrammaticMoveEnd: () -> Unit,
    onDisableFollowing: () -> Unit
) {
  LaunchedEffect(initialLatitude, initialLongitude) {
    if (initialLatitude != null && initialLongitude != null) {
      try {
        onProgrammaticMoveStart()
        onDisableFollowing() // Don't follow user when viewing specific location
        cameraPositionState.animate(
            update =
                CameraUpdateFactory.newLatLngZoom(LatLng(initialLatitude, initialLongitude), 16f),
            durationMs = 1000)
      } catch (e: Exception) {
        // Animation was interrupted or failed
      } finally {
        onProgrammaticMoveEnd()
      }
    }
  }
}

/** Checks if the camera should follow the user location. */
private fun shouldFollowUserLocation(
    currentLat: Double?,
    currentLng: Double?,
    isFollowingUser: Boolean
): Boolean = currentLat != null && currentLng != null && isFollowingUser

/** Checks if user interaction should disable following mode. */
private fun shouldDisableFollowing(
    isMoving: Boolean,
    isProgrammaticMove: Boolean,
    isFollowingUser: Boolean,
    isMapInitialized: Boolean
): Boolean = isMoving && !isProgrammaticMove && isFollowingUser && isMapInitialized

/** Handles the camera animation to follow user location. */
private suspend fun animateCameraToLocation(
    cameraPositionState: CameraPositionState,
    latitude: Double,
    longitude: Double,
    onMoveStart: () -> Unit,
    onMoveEnd: () -> Unit
) {
  try {
    onMoveStart()
    cameraPositionState.animate(
        update = CameraUpdateFactory.newLatLngZoom(LatLng(latitude, longitude), ZOOM_PROPORTION),
        durationMs = ONE_S_IN_MS)
  } catch (e: Exception) {
    // Animation was interrupted or failed
  } finally {
    onMoveEnd()
  }
}

/** Handles initial following user setup and filter state based on marker click state. */
private fun handleInitialFollowing(viewModel: MapViewModel, isReturningFromMarkerClick: Boolean) {
  if (isReturningFromMarkerClick) {
    viewModel.clearMarkerClickFlag()
  } else {
    viewModel.enableFollowingUser()
    viewModel.clearFilters()
  }
}

/** Checks if any filters are currently active (different from default state). */
private fun hasActiveFilters(filterState: FilterState): Boolean {
  return filterState.isSocialSelected ||
      filterState.isActivitySelected ||
      filterState.isSportSelected ||
      filterState.showMyEvents ||
      filterState.showJoinedEvents ||
      filterState.showOtherEvents ||
      filterState.sportCategories.any { it.isChecked }
}

/** Requests location permissions if not already granted. */
private fun requestPermissionsIfNeeded(allPermissionsGranted: Boolean, launchRequest: () -> Unit) {
  if (!allPermissionsGranted) {
    launchRequest()
  }
}

/** Initializes location service when permissions are granted. */
private fun initLocationServiceIfGranted(
    permissionsGranted: Boolean,
    viewModel: MapViewModel,
    context: Context
) {
  if (permissionsGranted) {
    viewModel.initLocationService(LocationServiceImpl(context))
  }
}

@Composable
private fun buttonColor(condition: Boolean, containerColor: Boolean): Color {
  return if (condition) {
    if (containerColor) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimary
  } else {
    if (containerColor) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface
  }
}

@Composable
private fun FilterChipConstructor(
    name: String,
    filterState: Boolean,
    onToggleAction: () -> Unit,
    testTag: String
) {
  FilterChip(
      selected = filterState,
      onClick = onToggleAction,
      label = { Text(name) },
      colors = MaterialTheme.customColors.filterChip,
      modifier = Modifier.testTag(testTag))
}

/**
 * Filter bottom sheet for the map screen.
 *
 * Displays event type filters (Social, Activity, Sport) and participation filters (My Events,
 * Joined Events, Other Events).
 *
 * @param filterState The current filter state
 * @param onToggleSocial Callback for toggling Social filter
 * @param onToggleActivity Callback for toggling Activity filter
 * @param onToggleSport Callback for toggling Sport filter
 * @param onToggleMyEvents Callback for toggling My Events filter
 * @param onToggleJoinedEvents Callback for toggling Joined Events filter
 * @param onToggleOtherEvents Callback for toggling Other Events filter
 * @param onClearFilters Callback for clearing all filters
 * @param onDismiss Callback when the bottom sheet is dismissed
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterBottomSheet(
    filterState: FilterState,
    onToggleSocial: () -> Unit,
    onToggleActivity: () -> Unit,
    onToggleSport: () -> Unit,
    onToggleMyEvents: () -> Unit,
    onToggleJoinedEvents: () -> Unit,
    onToggleOtherEvents: () -> Unit,
    onClearFilters: () -> Unit,
    onDismiss: () -> Unit
) {
  val sheetState = rememberModalBottomSheetState()

  ModalBottomSheet(
      onDismissRequest = onDismiss,
      sheetState = sheetState,
      modifier = Modifier.testTag(MapScreenTestTags.FILTER_BOTTOM_SHEET)) {
        Column(
            modifier =
                Modifier.fillMaxWidth()
                    .padding(horizontal = Dimens.Padding.medium)
                    .padding(bottom = Dimens.Padding.large)) {
              Text(
                  text = stringResource(R.string.type_object),
                  style = MaterialTheme.typography.titleMedium,
                  modifier = Modifier.padding(bottom = Dimens.Padding.small))

              Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing.small)) {
                    FilterChipConstructor(
                        name = stringResource(R.string.social_type),
                        filterState = filterState.isSocialSelected,
                        onToggleAction = onToggleSocial,
                        testTag = MapScreenTestTags.FILTER_TYPE_SOCIAL)
                    FilterChipConstructor(
                        name = stringResource(R.string.activity_type),
                        filterState = filterState.isActivitySelected,
                        onToggleAction = onToggleActivity,
                        testTag = MapScreenTestTags.FILTER_TYPE_ACTIVITY)
                    FilterChipConstructor(
                        name = stringResource(R.string.sport_type),
                        filterState = filterState.isSportSelected,
                        onToggleAction = onToggleSport,
                        testTag = MapScreenTestTags.FILTER_TYPE_SPORT)
                  }

              Spacer(modifier = Modifier.height(Dimens.Spacing.medium))
              HorizontalDivider()
              Spacer(modifier = Modifier.height(Dimens.Spacing.medium))

              Text(
                  text = stringResource(R.string.participation),
                  style = MaterialTheme.typography.titleMedium,
                  modifier = Modifier.padding(bottom = Dimens.Padding.small))

              Column(
                  modifier = Modifier.fillMaxWidth(),
                  verticalArrangement = Arrangement.spacedBy(Dimens.Spacing.small),
                  horizontalAlignment = Alignment.CenterHorizontally) {
                    FilterChipConstructor(
                        name = stringResource(R.string.my_events),
                        filterState = filterState.showMyEvents,
                        onToggleAction = onToggleMyEvents,
                        testTag = MapScreenTestTags.FILTER_PARTICIPATION_MY_EVENTS)

                    FilterChipConstructor(
                        name = stringResource(R.string.joined_events),
                        filterState = filterState.showJoinedEvents,
                        onToggleAction = onToggleJoinedEvents,
                        testTag = MapScreenTestTags.FILTER_PARTICIPATION_JOINED_EVENTS)
                    FilterChipConstructor(
                        name = stringResource(R.string.other_events),
                        filterState = filterState.showOtherEvents,
                        onToggleAction = onToggleOtherEvents,
                        testTag = MapScreenTestTags.FILTER_PARTICIPATION_OTHER_EVENTS)
                  }

              Spacer(modifier = Modifier.height(Dimens.Spacing.medium))

              Button(
                  onClick = onClearFilters,
                  modifier =
                      Modifier.fillMaxWidth().testTag(MapScreenTestTags.FILTER_CLOSE_BUTTON)) {
                    Text(stringResource(R.string.clear_filters))
                  }
            }
      }
}

/**
 * Displays a circle marker at the specified location when enabled.
 *
 * This is used to show a colored circle marker for locations shared in chat messages. The circle
 * uses the user's unique color (based on their userId) to match their chat message bubbles.
 *
 * Note: Does not show marker for current user's shared location, as the blue "my location"
 * indicator already shows their position.
 *
 * @param showLocationMarker Whether to show the location marker
 * @param initialLatitude The latitude of the location to mark
 * @param initialLongitude The longitude of the location to mark
 * @param userId The user ID who shared this location (used to generate their unique color)
 * @param currentUserId The current user's ID (to avoid duplicate markers)
 */
@Composable
private fun ShowLocationMarker(
    showLocationMarker: Boolean,
    initialLatitude: Double?,
    initialLongitude: Double?,
    userId: String?,
    currentUserId: String?
) {
  // Don't show marker if it's the current user (their blue location circle is already visible)
  if (showLocationMarker &&
      initialLatitude != null &&
      initialLongitude != null &&
      userId != null &&
      userId != currentUserId) {
    val context = LocalContext.current
    val locationPosition = LatLng(initialLatitude, initialLongitude)

    // Get the user's color (same as their chat bubble color)
    val (userColor, _) = getUserColor(userId)
    val circleMarkerIcon = createCircleMarker(userColor)

    Marker(
        state = MarkerState(position = locationPosition),
        icon = circleMarkerIcon,
        tag = MapScreenTestTags.LOCATION_MARKER_TAG,
        title = context.getString(R.string.shared_location_marker_title),
        snippet = context.getString(R.string.shared_location_marker_snippet))
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
 * - Optionally centering on a specific location (e.g., from a chat message).
 *
 * @param viewModel The [MapViewModel] managing location and UI state.
 * @param navigationActions Optional navigation actions for switching tabs or screens.
 * @param initialLatitude Optional latitude to center the map on initially.
 * @param initialLongitude Optional longitude to center the map on initially.
 * @param showLocationMarker Whether to show a marker at the initial location (e.g., for chat
 *   locations).
 * @param sharedLocationUserId User ID of the person who shared the location (for colored circle
 *   marker).
 * @param currentUserId Current user's ID (to avoid showing duplicate marker for own location).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun MapScreen(
    viewModel: MapViewModel = viewModel(),
    navigationActions: NavigationActions? = null,
    initialLatitude: Double? = null,
    initialLongitude: Double? = null,
    showLocationMarker: Boolean = false,
    sharedLocationUserId: String? = null,
    currentUserId: String? = null
) {
  val context = LocalContext.current

  // --- Collect the current UI state from the ViewModel ---
  val uiState by
      produceState(initialValue = MapUIState(), viewModel) {
        viewModel.uiState.collect { newState -> value = newState }
      }

  // --- Collect filter state from the ViewModel ---
  val filterState by viewModel.filterState.collectAsState()

  // --- State for showing/hiding the filter bottom sheet ---
  var showFilterSheet by remember { mutableStateOf(false) }

  // --- Enable following user on screen entry (unless returning from marker double click) ---
  LaunchedEffect(Unit) { handleInitialFollowing(viewModel, uiState.isReturningFromMarkerClick) }

  // --- Permissions management---
  val locationPermissionsState =
      rememberMultiplePermissionsState(
          listOf(
              Manifest.permission.ACCESS_FINE_LOCATION,
              Manifest.permission.ACCESS_COARSE_LOCATION,
          ))

  LaunchedEffect(Unit) {
    requestPermissionsIfNeeded(locationPermissionsState.allPermissionsGranted) {
      locationPermissionsState.launchMultiplePermissionRequest()
    }
    viewModel.fetchLocalizableEvents()
  }

  // --- Reinitialize location service when permissions are granted ---
  LaunchedEffect(locationPermissionsState.allPermissionsGranted) {
    initLocationServiceIfGranted(locationPermissionsState.allPermissionsGranted, viewModel, context)
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

  // --- Mark map as initialized once the camera position state is ready ---
  LaunchedEffect(cameraPositionState) { isMapInitialized = true }

  // --- Center map on specific location if provided (e.g., from chat location message) ---
  MapInitialLocationEffect(
      initialLatitude = initialLatitude,
      initialLongitude = initialLongitude,
      cameraPositionState = cameraPositionState,
      onProgrammaticMoveStart = { isProgrammaticMove = true },
      onProgrammaticMoveEnd = { isProgrammaticMove = false },
      onDisableFollowing = { viewModel.disableFollowingUser() })

  // --- Center the map when the user location changes (only if following is enabled) ---
  LaunchedEffect(currentLat, currentLng, isFollowingUser) {
    if (shouldFollowUserLocation(currentLat, currentLng, isFollowingUser)) {
      animateCameraToLocation(
          cameraPositionState = cameraPositionState,
          latitude = currentLat!!,
          longitude = currentLng!!,
          onMoveStart = { isProgrammaticMove = true },
          onMoveEnd = { isProgrammaticMove = false })
    }
  }

  // --- Detect user interaction with the map to disable following ---
  LaunchedEffect(cameraPositionState) {
    snapshotFlow { cameraPositionState.isMoving }
        .collect { isMoving ->
          if (shouldDisableFollowing(
              isMoving, isProgrammaticMove, isFollowingUser, isMapInitialized)) {
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
                            snippet = stringResource(R.string.snippet_message),
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
                          snippet = stringResource(R.string.snippet_message),
                          onInfoWindowClick = {
                            viewModel.onMarkerClick()
                            navigationActions?.navigateTo(Screen.SerieDetails(serie.serieId))
                          })
                    }

                    // Show marker for initial location (e.g., from chat location messages)
                    ShowLocationMarker(
                        showLocationMarker = showLocationMarker,
                        initialLatitude = initialLatitude,
                        initialLongitude = initialLongitude,
                        userId = sharedLocationUserId,
                        currentUserId = currentUserId)
                  }

              val hasFilters = hasActiveFilters(filterState)

              FloatingActionButton(
                  onClick = { showFilterSheet = true },
                  modifier =
                      Modifier.align(Alignment.TopStart)
                          .padding(Dimens.Padding.medium)
                          .testTag(MapScreenTestTags.FILTER_BUTTON),
                  containerColor = buttonColor(hasFilters, CONTAINER_COLOR)) {
                    Icon(
                        imageVector = Icons.Filled.Tune,
                        contentDescription = stringResource(R.string.filter_button_description),
                        tint = buttonColor(hasFilters, NOT_CONTAINER_COLOR))
                  }

              FloatingActionButton(
                  onClick = { viewModel.enableFollowingUser() },
                  modifier =
                      Modifier.align(Alignment.TopEnd)
                          .padding(Dimens.Padding.medium)
                          .testTag(MapScreenTestTags.MY_LOCATION_BUTTON),
                  containerColor = buttonColor(isFollowingUser, CONTAINER_COLOR)) {
                    Icon(
                        imageVector = Icons.Filled.MyLocation,
                        contentDescription =
                            stringResource(R.string.my_location_button_description),
                        tint = buttonColor(isFollowingUser, NOT_CONTAINER_COLOR))
                  }
            }
      }

  // --- Filter Bottom Sheet ---
  if (showFilterSheet) {
    FilterBottomSheet(
        filterState = filterState,
        onToggleSocial = { viewModel.toggleSocial() },
        onToggleActivity = { viewModel.toggleActivity() },
        onToggleSport = { viewModel.toggleSport() },
        onToggleMyEvents = { viewModel.toggleMyEvents() },
        onToggleJoinedEvents = { viewModel.toggleJoinedEvents() },
        onToggleOtherEvents = { viewModel.toggleOtherEvents() },
        onClearFilters = { viewModel.clearFilters() },
        onDismiss = { showFilterSheet = false })
  }
}
