package com.android.joinme.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.joinme.model.event.Event
import com.android.joinme.model.event.EventFilter
import com.android.joinme.model.event.EventsRepository
import com.android.joinme.model.event.EventsRepositoryProvider
import com.android.joinme.model.map.UserLocation
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
 * @property todos The list of events displayed on the map.
 * @property errorMsg An optional error message to be shown to the user.
 * @property isLoading Indicates whether a loading operation is currently in progress.
 */
data class MapUIState(
    val userLocation: UserLocation? = null,
    val todos: List<Event> = emptyList(),
    val errorMsg: String? = null,
    val isLoading: Boolean = false
)

/**
 * ViewModel responsible for managing the map screen logic and user location updates.
 *
 * @param locationService The service used to retrieve the user's location.
 * @param eventsRepository The repository used to retrieve events for the map.
 */
class MapViewModel(
    private var locationService: UserLocationService? = null,
    private val eventsRepository: EventsRepository? = null
) : ViewModel() {

  /** The repository used to retrieve events for the map. */
  private val repo: EventsRepository by lazy {
    eventsRepository ?: EventsRepositoryProvider.getRepository(isOnline = true)
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
        val events = repo.getAllEvents(EventFilter.EVENTS_FOR_MAP_SCREEN)
        _uiState.value = _uiState.value.copy(todos = events, isLoading = false)
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
