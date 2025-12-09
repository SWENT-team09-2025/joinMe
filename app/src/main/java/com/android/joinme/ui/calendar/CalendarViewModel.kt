package com.android.joinme.ui.calendar

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.joinme.model.event.EventFilter
import com.android.joinme.model.event.EventsRepository
import com.android.joinme.model.event.EventsRepositoryProvider
import com.android.joinme.model.eventItem.EventItem
import com.android.joinme.model.serie.SerieFilter
import com.android.joinme.model.serie.SeriesRepository
import com.android.joinme.model.serie.SeriesRepositoryProvider
import java.util.Calendar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Note: This file was co-written with AI (Claude). */

/**
 * UI state for the Calendar screen.
 *
 * @property selectedDate The currently selected date (timestamp in millis)
 * @property currentMonth The month being displayed (0-11)
 * @property currentYear The year being displayed
 * @property itemsForDate List of events and series for the selected date
 * @property daysWithItems Set of day numbers (1-31) in the current month that have items
 * @property isLoading Whether data is being loaded
 * @property error Error message if any operation failed
 */
data class CalendarUIState(
    val selectedDate: Long = System.currentTimeMillis(),
    val currentMonth: Int = Calendar.getInstance()[Calendar.MONTH],
    val currentYear: Int = Calendar.getInstance()[Calendar.YEAR],
    val itemsForDate: List<EventItem> = emptyList(),
    val daysWithItems: Set<Int> = emptySet(),
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * ViewModel for managing calendar state and operations.
 *
 * Handles:
 * - Date selection and navigation
 * - Loading events and series for selected dates
 * - Filtering items by date
 *
 * @param eventsRepository Repository for event data operations
 * @param seriesRepository Repository for series data operations
 */
class CalendarViewModel(
    private val eventsRepository: EventsRepository = EventsRepositoryProvider.getRepository(true),
    private val seriesRepository: SeriesRepository = SeriesRepositoryProvider.repository
) : ViewModel() {

  companion object {
    private const val TAG = "CalendarViewModel"
  }

  private val _uiState = MutableStateFlow(CalendarUIState())
  val uiState: StateFlow<CalendarUIState> = _uiState.asStateFlow()

  // Cache all items to avoid reloading on every date selection
  private var cachedItems: List<EventItem> = emptyList()

  init {
    loadAllItems()
  }

  /**
   * Selects a new date and filters cached items for that date.
   *
   * @param timestamp The selected date timestamp in milliseconds
   */
  fun selectDate(timestamp: Long) {
    _uiState.value = _uiState.value.copy(selectedDate = timestamp)
    updateItemsForSelectedDate()
  }

  /**
   * Changes the displayed month and updates days with items.
   *
   * @param month The month (0-11)
   * @param year The year
   */
  fun changeMonth(month: Int, year: Int) {
    _uiState.value =
        _uiState.value.copy(
            currentMonth = month,
            currentYear = year,
            daysWithItems = getDaysWithItemsForCurrentMonth())
  }

  /** Navigates to the next month. */
  fun nextMonth() {
    val currentState = _uiState.value
    val calendar =
        Calendar.getInstance().apply {
          set(Calendar.MONTH, currentState.currentMonth)
          set(Calendar.YEAR, currentState.currentYear)
          add(Calendar.MONTH, 1)
        }
    changeMonth(calendar.get(Calendar.MONTH), calendar.get(Calendar.YEAR))
  }

  /** Navigates to the previous month. */
  fun previousMonth() {
    val currentState = _uiState.value
    val calendar =
        Calendar.getInstance().apply {
          set(Calendar.MONTH, currentState.currentMonth)
          set(Calendar.YEAR, currentState.currentYear)
          add(Calendar.MONTH, -1)
        }
    changeMonth(calendar.get(Calendar.MONTH), calendar.get(Calendar.YEAR))
  }

  /**
   * Loads all events and series from repositories and caches them. This is called once on
   * initialization and when refreshing.
   */
  private fun loadAllItems() {
    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isLoading = true, error = null)

      try {
        // Load all events and series
        val allEvents = eventsRepository.getAllEvents(EventFilter.EVENTS_FOR_OVERVIEW_SCREEN)
        val allSeries = seriesRepository.getAllSeries(SerieFilter.SERIES_FOR_OVERVIEW_SCREEN)

        // Identify events that belong to series
        val serieEventIds = allSeries.flatMap { it.eventIds }.toSet()

        // Filter out standalone events (events not in any serie)
        val standaloneEvents = allEvents.filterNot { it.eventId in serieEventIds }

        // Create EventItems
        val eventItems = standaloneEvents.map { EventItem.SingleEvent(it) }
        val serieItems = allSeries.map { EventItem.EventSerie(it) }

        // Combine and cache all items
        cachedItems = eventItems + serieItems

        // Update UI state with filtered items and days with items
        updateItemsForSelectedDate()
        _uiState.value = _uiState.value.copy(daysWithItems = getDaysWithItemsForCurrentMonth())
      } catch (e: Exception) {
        Log.e(TAG, "Error loading items", e)
        _uiState.value =
            _uiState.value.copy(
                itemsForDate = emptyList(),
                isLoading = false,
                error = "Failed to load items: ${e.message}")
      }
    }
  }

  /** Filters cached items for the currently selected date and updates UI state. */
  private fun updateItemsForSelectedDate() {
    val itemsForSelectedDate = filterItemsForDate(cachedItems, _uiState.value.selectedDate)
    _uiState.value = _uiState.value.copy(itemsForDate = itemsForSelectedDate, isLoading = false)
  }

  /**
   * Gets the set of day numbers in the current month that have items.
   *
   * @return Set of day numbers (1-31) that have events or series
   */
  private fun getDaysWithItemsForCurrentMonth(): Set<Int> {
    val currentState = _uiState.value
    return cachedItems
        .mapNotNull { item ->
          val itemCalendar = Calendar.getInstance().apply { timeInMillis = item.date.toDate().time }
          if (itemCalendar.get(Calendar.MONTH) == currentState.currentMonth &&
              itemCalendar.get(Calendar.YEAR) == currentState.currentYear) {
            itemCalendar.get(Calendar.DAY_OF_MONTH)
          } else null
        }
        .toSet()
  }

  /**
   * Filters event items (events and series) that occur on the given date.
   *
   * @param items List of all event items
   * @param dateTimestamp The date to filter for (timestamp in millis)
   * @return List of event items occurring on that date, sorted by time
   */
  private fun filterItemsForDate(items: List<EventItem>, dateTimestamp: Long): List<EventItem> {
    val calendar = Calendar.getInstance().apply { timeInMillis = dateTimestamp }
    val targetDay = calendar.get(Calendar.DAY_OF_MONTH)
    val targetMonth = calendar.get(Calendar.MONTH)
    val targetYear = calendar.get(Calendar.YEAR)

    return items
        .filter { item ->
          val itemDate = item.date.toDate().time
          val itemCalendar = Calendar.getInstance().apply { timeInMillis = itemDate }
          itemCalendar.get(Calendar.DAY_OF_MONTH) == targetDay &&
              itemCalendar.get(Calendar.MONTH) == targetMonth &&
              itemCalendar.get(Calendar.YEAR) == targetYear
        }
        .sortedBy { it.date.toDate().time }
  }

  /**
   * Reloads events and series from repositories (useful after creating/editing an event or serie).
   */
  fun refreshItems() {
    loadAllItems()
  }

  /** Clears the error state. */
  fun clearError() {
    _uiState.value = _uiState.value.copy(error = null)
  }
}
