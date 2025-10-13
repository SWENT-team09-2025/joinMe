package com.android.joinme.ui.map

// import com.google.android.gms.maps.model.LatLng
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.joinme.model.event.Event
import com.android.joinme.model.map.Location
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MapUIState(
    // val target: LatLng = LatLng(0.0, 0.0),
    val todos: List<Event> = emptyList(),
    val errorMsg: String? = null,
    val isLoading: Boolean = false
)

// class MapViewModel(private val repository: EventsRepository) : ViewModel() {
class MapViewModel() : ViewModel() {
  private val _uiState = MutableStateFlow(MapUIState())
  val uiState: StateFlow<MapUIState> = _uiState.asStateFlow()

  companion object {
    private val EPFL_LOCATION =
        Location(46.5191, 6.5668, "École Polytechnique Fédérale de Lausanne (EPFL), Switzerland")

    /*private fun toLatLng(location: Location): LatLng {
      return LatLng(location.latitude, location.longitude)
    }*/
  }

  init {
    Firebase.auth.addAuthStateListener {
      if (it.currentUser != null) {
        // ToDo fetchLocalizableTodos()
      }
    }
  }

  private fun setErrorMsg(errorMsg: String) {
    _uiState.value = _uiState.value.copy(errorMsg = errorMsg)
  }

  fun clearErrorMsg() {
    _uiState.value = _uiState.value.copy(errorMsg = null)
  }

  fun refreshUIState() {
    // ToDo
  }

  private fun fetchLocalizableEvent() {
    viewModelScope.launch {
      try {
        // ToDo
      } catch (e: Exception) {
        setErrorMsg("Failed to load todos: ${e.message}")
      }
    }
  }
}
