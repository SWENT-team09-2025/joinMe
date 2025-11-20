package com.android.joinme.ui.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.joinme.model.event.Event
import com.android.joinme.model.event.EventsRepository
import com.android.joinme.model.event.EventsRepositoryProvider
import com.android.joinme.model.groups.GroupRepository
import com.android.joinme.model.groups.GroupRepositoryProvider
import com.android.joinme.model.serie.Serie
import com.android.joinme.model.serie.SeriesRepository
import com.android.joinme.model.serie.SeriesRepositoryProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI state for the ActivityGroup screen.
 *
 * Represents the current state of group activities including events and series.
 *
 * @property isLoading Whether the data is currently being loaded
 * @property events List of events associated with the group
 * @property series List of series associated with the group
 * @property error Error message if loading failed, null otherwise
 */
data class ActivityGroupUiState(
    val isLoading: Boolean = false,
    val events: List<Event> = emptyList(),
    val series: List<Serie> = emptyList(),
    val error: String? = null
)

/**
 * ViewModel for the ActivityGroup screen.
 *
 * Manages the state and loading of group activities including events and series. Loads events and
 * series based on the group's event IDs and serie IDs respectively.
 *
 * @property groupRepository Repository for managing group data
 * @property eventsRepository Repository for managing event data
 * @property seriesRepository Repository for managing series data
 */
class ActivityGroupViewModel(
    private val groupRepository: GroupRepository = GroupRepositoryProvider.repository,
    private val eventsRepository: EventsRepository =
        EventsRepositoryProvider.getRepository(isOnline = true),
    private val seriesRepository: SeriesRepository = SeriesRepositoryProvider.repository
) : ViewModel() {

  private val _uiState = MutableStateFlow(ActivityGroupUiState())
  val uiState: StateFlow<ActivityGroupUiState> = _uiState.asStateFlow()

  /**
   * Loads the activities (events and series) for the specified group.
   *
   * Loads events based on the group's event IDs and series based on the group's serie IDs.
   *
   * @param groupId The unique identifier of the group whose activities are to be loaded
   */
  fun load(groupId: String) {
    viewModelScope.launch {
      _uiState.value = ActivityGroupUiState(isLoading = true)

      try {
        val group = groupRepository.getGroup(groupId)

        // Load events using the group's event IDs
        val loadedEvents =
            if (group.eventIds.isNotEmpty()) {
              eventsRepository.getEventsByIds(group.eventIds)
            } else {
              emptyList()
            }

        // Load series using the group's serie IDs
        val loadedSeries =
            if (group.serieIds.isNotEmpty()) {
              seriesRepository.getSeriesByIds(group.serieIds)
            } else {
              emptyList()
            }

        _uiState.value =
            ActivityGroupUiState(
                isLoading = false, events = loadedEvents, series = loadedSeries, error = null)
      } catch (e: Exception) {
        _uiState.value =
            ActivityGroupUiState(
                isLoading = false,
                events = emptyList(),
                series = emptyList(),
                error = "Failed to load group activities: ${e.message}")
      }
    }
  }
}
