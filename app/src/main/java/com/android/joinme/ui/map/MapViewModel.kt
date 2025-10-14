package com.android.joinme.ui.map

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.joinme.model.event.Event
import com.android.joinme.model.map.UserLocation
import com.android.joinme.ui.map.userLocation.UserLocationService
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

data class MapUIState(
    val userLocation: UserLocation? = null,
    val todos: List<Event> = emptyList(),
    val errorMsg: String? = null,
    val isLoading: Boolean = false
)

class MapViewModel(private var locationService: UserLocationService? = null) : ViewModel() {
  private val _uiState = MutableStateFlow(MapUIState())
  val uiState: StateFlow<MapUIState> = _uiState.asStateFlow()

  init {
    Firebase.auth.addAuthStateListener {
      if (it.currentUser != null) {
        // ToDo fetchLocalizableEvent()
      }
    }
  }

  private fun startLocationUpdates() {
    locationService?.let { service ->
      viewModelScope.launch {
        service.getUserLocationFlow().filterNotNull().collect { location ->
          Log.d("MapViewModel", "New location received: $location")
          _uiState.value = _uiState.value.copy(userLocation = location)
        }
      }
    }
  }

  private fun setErrorMsg(errorMsg: String) {
    _uiState.value = _uiState.value.copy(errorMsg = errorMsg)
  }

  fun clearErrorMsg() {
    _uiState.value = _uiState.value.copy(errorMsg = null)
  }

  fun initLocationService(service: UserLocationService) {
    locationService = service
    startLocationUpdates()
  }

  private fun fetchLocalizableEvent() {
    viewModelScope.launch {
      try {
        // ToDo
      } catch (e: Exception) {
        setErrorMsg("Failed to load events: ${e.message}")
      }
    }
  }

  override fun onCleared() {
    super.onCleared()
    locationService?.stopLocationUpdates()
  }
}
