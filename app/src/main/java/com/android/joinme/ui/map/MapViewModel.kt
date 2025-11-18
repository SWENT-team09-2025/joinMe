package com.android.joinme.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.joinme.model.event.Event
import com.android.joinme.model.event.EventFilter
import com.android.joinme.model.event.EventsRepository
import com.android.joinme.model.event.EventsRepositoryProvider
import com.android.joinme.model.map.Location
import com.android.joinme.model.map.UserLocation
import com.android.joinme.model.serie.Serie
import com.android.joinme.model.serie.SerieFilter
import com.android.joinme.model.serie.SeriesRepository
import com.android.joinme.model.serie.SeriesRepositoryProvider
import com.android.joinme.ui.map.userLocation.UserLocationService
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

/**
 * Represents the UI state of the map screen.
 *
 * @property userLocation The current location of the user, or null if not yet available.
 * @property events The list of events displayed on the map.
 * @property series The list of series displayed on the map.
 * @property errorMsg An optional error message to be shown to the user.
 * @property isLoading Indicates whether a loading operation is currently in progress.
 * @property isFollowingUser Indicates whether the camera should automatically follow user location updates.
 * @property isReturningFromMarkerClick Indicates if the user is returning from a marker click navigation.
 */
data class MapUIState(
    val userLocation: UserLocation? = null,
    val events: List<Event> = emptyList(),
    val series: Map<Location, Serie> = emptyMap(),
    val errorMsg: String? = null,
    val isLoading: Boolean = false,
    val isFollowingUser: Boolean = true,
    val isReturningFromMarkerClick: Boolean = false
)

/**
 * ViewModel responsible for managing the map screen logic and user location updates.
 *
 * @param locationService The service used to retrieve the user's location.
 * @param eventsRepository The repository used to retrieve events for the map.
 */
class MapViewModel(
    private var locationService: UserLocationService? = null,
    private val eventsRepository: EventsRepository? = null,
    private val seriesRepository: SeriesRepository? = null
) : ViewModel() {

  /** The repository used to retrieve events et series for the map. */
  private val repoEvent: EventsRepository by lazy {
    eventsRepository ?: EventsRepositoryProvider.getRepository(isOnline = true)
  }
  private val repoSeries: SeriesRepository by lazy {
    seriesRepository ?: SeriesRepositoryProvider.repository
  }

  /** A mutable state flow holding the current UI state. */
  private val _uiState = MutableStateFlow(MapUIState())

  /** A read-only state flow exposed to the UI (observed by Jetpack Compose). */
  val uiState: StateFlow<MapUIState> = _uiState.asStateFlow()

  /**
   * Initializes a Firebase authentication state listener. When a user logs in, this can trigger
   * fetching of localizable events.
   */
  init {
    Firebase.auth.addAuthStateListener {
      if (it.currentUser != null) {
        fetchLocalizableEvents()
      }
    }
  }

  /**
   * Fetches all upcoming events with a location from the repository and updates the UI state. This
   * includes owned events, joined events, and public events.
   */
  private fun fetchLocalizableEvents() {
    viewModelScope.launch {
      try {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMsg = null)
        val events = repoEvent.getAllEvents(EventFilter.EVENTS_FOR_MAP_SCREEN)
        val series = repoSeries.getAllSeries(SerieFilter.SERIES_FOR_MAP_SCREEN)

        val eventsById = events.associateBy { it.eventId }

        val seriesEventIds = series.flatMap { it.eventIds }.toSet()
        val eventsNotInSeries = events.filterNot { it.eventId in seriesEventIds }

        val seriesMap =
            series
                .mapNotNull { serie ->
                  if (serie.eventIds.isEmpty()) {
                    null
                  } else {
                    val firstEvent = eventsById[serie.eventIds[0]]
                    firstEvent?.location?.let { location -> location to serie }
                  }
                }
                .toMap()

        _uiState.value =
            _uiState.value.copy(events = eventsNotInSeries, series = seriesMap, isLoading = false)
      } catch (e: Exception) {
        _uiState.value =
            _uiState.value.copy(errorMsg = "Failed to load events: ${e.message}", isLoading = false)
      }
    }
  }

  /**
   * Starts listening for user location updates using the provided service. Updates the UI state
   * whenever a new location is received.
   */
  private fun startLocationUpdates() {
    locationService?.let { service ->
      viewModelScope.launch {
        service.getUserLocationFlow().filterNotNull().collect { location ->
          _uiState.value = _uiState.value.copy(userLocation = location)
        }
      }
    }
  }

  /**
   * Clears the current error message from the UI state. Useful after displaying a Snack bar or
   * dialog.
   */
  fun clearErrorMsg() {
    _uiState.value = _uiState.value.copy(errorMsg = null)
  }

  /**
   * Enables automatic camera following of user location updates.
   * Called when the user wants to re-center on their location.
   */
  fun enableFollowingUser() {
    _uiState.value = _uiState.value.copy(isFollowingUser = true)
  }

  /**
   * Disables automatic camera following of user location updates.
   * Called when the user manually interacts with the map.
   */
  fun disableFollowingUser() {
    _uiState.value = _uiState.value.copy(isFollowingUser = false)
  }

  /**
   * Marks that the user is navigating away from the map by clicking on a marker.
   * This prevents automatic re-centering when returning to the map.
   */
  fun onMarkerClick() {
    _uiState.value = _uiState.value.copy(isReturningFromMarkerClick = true, isFollowingUser = false)
  }

  /**
   * Resets the marker click flag after handling the return.
   */
  fun clearMarkerClickFlag() {
    _uiState.value = _uiState.value.copy(isReturningFromMarkerClick = false)
  }

  /**
   * Initializes the location service and starts receiving updates.
   *
   * @param service The [UserLocationService] to use for location updates.
   */
  fun initLocationService(service: UserLocationService) {
    locationService = service
    startLocationUpdates()
  }

  /** Called when the ViewModel is cleared. Stops location updates to prevent memory leaks. */
  override fun onCleared() {
    super.onCleared()
    locationService?.stopLocationUpdates()
  }
}
