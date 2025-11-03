package com.android.joinme.model.eventItem

import com.android.joinme.model.event.Event
import com.android.joinme.model.serie.Serie
import com.google.firebase.Timestamp

/**
 * Sealed class representing an item that can be displayed in the overview.
 *
 * This allows mixing events and series in a single list while maintaining type safety. Each subtype
 * provides common properties (eventItemId, title, date) for uniform sorting and display.
 */
sealed class EventItem {
  /** Unique identifier for this item */
  abstract val eventItemId: String

  /** Display title for this item */
  abstract val title: String

  /** Primary date/timestamp for this item (used for sorting) */
  abstract val date: Timestamp

  /**
   * Represents a single standalone event.
   *
   * @property event The underlying Event object
   */
  data class SingleEvent(val event: Event) : EventItem() {
    override val eventItemId: String = event.eventId
    override val title: String = event.title
    override val date: Timestamp = event.date
  }

  /**
   * Represents a series of related events.
   *
   * @property serie The underlying Serie object
   */
  data class EventSerie(val serie: Serie) : EventItem() {
    override val eventItemId: String = serie.serieId
    override val title: String = serie.title
    override val date: Timestamp = serie.date
  }
}
