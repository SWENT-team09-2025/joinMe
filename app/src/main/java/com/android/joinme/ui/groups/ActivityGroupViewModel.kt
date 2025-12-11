package com.android.joinme.ui.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.joinme.model.event.EventsRepository
import com.android.joinme.model.event.EventsRepositoryProvider
import com.android.joinme.model.event.isExpired
import com.android.joinme.model.eventItem.EventItem
import com.android.joinme.model.groups.GroupRepository
import com.android.joinme.model.groups.GroupRepositoryProvider
import com.android.joinme.model.serie.SeriesRepository
import com.android.joinme.model.serie.SeriesRepositoryProvider
import com.android.joinme.model.serie.isExpired
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Note: This file was co-written with the help of AI (Claude). */

/**
 * UI state for the ActivityGroup screen.
 *
 * Represents the current state of group activities including events and series.
 *
 * @property isLoading Whether the data is currently being loaded
 * @property groupName The name of the group
 * @property items List of EventItem (events and series mixed) associated with the group
 * @property error Error message if loading failed, null otherwise
 */
data class ActivityGroupUiState(
    val isLoading: Boolean = false,
    val groupName: String = "",
    val items: List<EventItem> = emptyList(),
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
              eventsRepository.getEventsByIds(group.eventIds).filterNot { it.isExpired() }
            } else {
              emptyList()
            }

        // Load series using the group's serie IDs
        val loadedSeries =
            if (group.serieIds.isNotEmpty()) {
              seriesRepository.getSeriesByIds(group.serieIds).filterNot { it.isExpired() }
            } else {
              emptyList()
            }

        // Identify events that belong to series
        val serieEventIds = loadedSeries.flatMap { it.eventIds }.toSet()

        // Filter out standalone events (events not in any serie)
        val standaloneEvents = loadedEvents.filterNot { it.eventId in serieEventIds }

        // Create EventItems
        val eventItems = standaloneEvents.map { EventItem.SingleEvent(it) }
        val serieItems = loadedSeries.map { EventItem.EventSerie(it) }

        // Combine all items and sort by date
        val allItems = (eventItems + serieItems).sortedBy { it.date.toDate().time }

        _uiState.value =
            ActivityGroupUiState(
                isLoading = false, groupName = group.name, items = allItems, error = null)
      } catch (e: Exception) {
        _uiState.value =
            ActivityGroupUiState(
                isLoading = false,
                items = emptyList(),
                error = "Failed to load group activities: ${e.message}")
      }
    }
  }
}
