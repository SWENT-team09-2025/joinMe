package com.android.joinme.model.eventItem

import com.android.joinme.model.event.Event
import com.android.joinme.model.event.EventType
import com.android.joinme.model.event.EventVisibility
import com.android.joinme.model.map.Location
import com.android.joinme.model.serie.Serie
import com.android.joinme.model.utils.Visibility
import com.google.firebase.Timestamp
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EventItemTest {

  private val sampleLocation = Location(46.5191, 6.5668, "EPFL")
  private val sampleTimestamp = Timestamp(Date())

  private val sampleEvent =
      Event(
          eventId = "event123",
          type = EventType.SPORTS,
          title = "Football Game",
          description = "Friendly football match",
          location = sampleLocation,
          date = sampleTimestamp,
          duration = 90,
          participants = listOf("Alice", "Bob"),
          maxParticipants = 10,
          visibility = EventVisibility.PUBLIC,
          ownerId = "owner123")

  private val sampleSerie =
      Serie(
          serieId = "serie123",
          title = "Weekly Football",
          description = "Weekly football series",
          date = sampleTimestamp,
          participants = listOf("user1", "user2"),
          maxParticipants = 10,
          visibility = Visibility.PUBLIC,
          eventIds = listOf("event1", "event2", "event3"),
          ownerId = "owner123")

  @Test
  fun `SingleEvent wraps event correctly`() {
    val singleEvent = EventItem.SingleEvent(sampleEvent)
    assertEquals(sampleEvent, singleEvent.event)
  }

  @Test
  fun `SingleEvent exposes correct eventItemId`() {
    val singleEvent = EventItem.SingleEvent(sampleEvent)
    assertEquals("event123", singleEvent.eventItemId)
    assertEquals(sampleEvent.eventId, singleEvent.eventItemId)
  }

  @Test
  fun `SingleEvent exposes correct title`() {
    val singleEvent = EventItem.SingleEvent(sampleEvent)
    assertEquals("Football Game", singleEvent.title)
    assertEquals(sampleEvent.title, singleEvent.title)
  }

  @Test
  fun `SingleEvent exposes correct date`() {
    val singleEvent = EventItem.SingleEvent(sampleEvent)
    assertEquals(sampleTimestamp, singleEvent.date)
    assertEquals(sampleEvent.date, singleEvent.date)
  }

  @Test
  fun `EventSerie wraps serie correctly`() {
    val eventSerie = EventItem.EventSerie(sampleSerie)
    assertEquals(sampleSerie, eventSerie.serie)
  }

  @Test
  fun `EventSerie exposes correct eventItemId`() {
    val eventSerie = EventItem.EventSerie(sampleSerie)
    assertEquals("serie123", eventSerie.eventItemId)
    assertEquals(sampleSerie.serieId, eventSerie.eventItemId)
  }

  @Test
  fun `EventSerie exposes correct title`() {
    val eventSerie = EventItem.EventSerie(sampleSerie)
    assertEquals("Weekly Football", eventSerie.title)
    assertEquals(sampleSerie.title, eventSerie.title)
  }

  @Test
  fun `EventSerie exposes correct date`() {
    val eventSerie = EventItem.EventSerie(sampleSerie)
    assertEquals(sampleTimestamp, eventSerie.date)
    assertEquals(sampleSerie.date, eventSerie.date)
  }

  @Test
  fun `SingleEvent equality works correctly`() {
    val singleEvent1 = EventItem.SingleEvent(sampleEvent)
    val singleEvent2 = EventItem.SingleEvent(sampleEvent)
    assertEquals(singleEvent1, singleEvent2)
  }

  @Test
  fun `SingleEvent inequality works correctly`() {
    val singleEvent1 = EventItem.SingleEvent(sampleEvent)
    val singleEvent2 = EventItem.SingleEvent(sampleEvent.copy(eventId = "different123"))
    assertNotEquals(singleEvent1, singleEvent2)
  }

  @Test
  fun `EventSerie equality works correctly`() {
    val eventSerie1 = EventItem.EventSerie(sampleSerie)
    val eventSerie2 = EventItem.EventSerie(sampleSerie)
    assertEquals(eventSerie1, eventSerie2)
  }

  @Test
  fun `EventSerie inequality works correctly`() {
    val eventSerie1 = EventItem.EventSerie(sampleSerie)
    val eventSerie2 = EventItem.EventSerie(sampleSerie.copy(serieId = "different123"))
    assertNotEquals(eventSerie1, eventSerie2)
  }

  @Test
  fun `SingleEvent and EventSerie are not equal`() {
    val singleEvent = EventItem.SingleEvent(sampleEvent)
    val eventSerie = EventItem.EventSerie(sampleSerie)
    assertNotEquals(singleEvent as EventItem, eventSerie as EventItem)
  }

  @Test
  fun `SingleEvent hashCode is consistent`() {
    val singleEvent1 = EventItem.SingleEvent(sampleEvent)
    val singleEvent2 = EventItem.SingleEvent(sampleEvent)
    assertEquals(singleEvent1.hashCode(), singleEvent2.hashCode())
  }

  @Test
  fun `EventSerie hashCode is consistent`() {
    val eventSerie1 = EventItem.EventSerie(sampleSerie)
    val eventSerie2 = EventItem.EventSerie(sampleSerie)
    assertEquals(eventSerie1.hashCode(), eventSerie2.hashCode())
  }

  @Test
  fun `SingleEvent copy creates new instance`() {
    val singleEvent = EventItem.SingleEvent(sampleEvent)
    val copiedEvent = singleEvent.copy(event = sampleEvent.copy(title = "New Title"))
    assertEquals("New Title", copiedEvent.title)
    assertNotEquals(singleEvent, copiedEvent)
  }

  @Test
  fun `EventSerie copy creates new instance`() {
    val eventSerie = EventItem.EventSerie(sampleSerie)
    val copiedSerie = eventSerie.copy(serie = sampleSerie.copy(title = "New Title"))
    assertEquals("New Title", copiedSerie.title)
    assertNotEquals(eventSerie, copiedSerie)
  }

  @Test
  fun `SingleEvent toString contains event details`() {
    val singleEvent = EventItem.SingleEvent(sampleEvent)
    val result = singleEvent.toString()
    assertTrue(result.contains("SingleEvent"))
    assertTrue(result.contains("event123"))
  }

  @Test
  fun `EventSerie toString contains serie details`() {
    val eventSerie = EventItem.EventSerie(sampleSerie)
    val result = eventSerie.toString()
    assertTrue(result.contains("EventSerie"))
    assertTrue(result.contains("serie123"))
  }

  @Test
  fun `mixed list of EventItems can be created`() {
    val items: List<EventItem> =
        listOf(
            EventItem.SingleEvent(sampleEvent),
            EventItem.EventSerie(sampleSerie),
            EventItem.SingleEvent(sampleEvent.copy(eventId = "event456")))

    assertEquals(3, items.size)
    assertTrue(items[0] is EventItem.SingleEvent)
    assertTrue(items[1] is EventItem.EventSerie)
    assertTrue(items[2] is EventItem.SingleEvent)
  }

  @Test
  fun `EventItems can be sorted by date`() {
    val olderDate = Timestamp(Date(System.currentTimeMillis() - 100000))
    val newerDate = Timestamp(Date(System.currentTimeMillis() + 100000))

    val olderEvent = sampleEvent.copy(date = olderDate)
    val newerEvent = sampleEvent.copy(eventId = "newer123", date = newerDate)

    val items: List<EventItem> =
        listOf(EventItem.SingleEvent(newerEvent), EventItem.SingleEvent(olderEvent))

    val sortedItems = items.sortedBy { it.date.seconds }

    assertEquals("event123", sortedItems[0].eventItemId)
    assertEquals("newer123", sortedItems[1].eventItemId)
  }

  @Test
  fun `mixed EventItems can be sorted by date`() {
    val olderDate = Timestamp(Date(System.currentTimeMillis() - 100000))
    val newerDate = Timestamp(Date(System.currentTimeMillis() + 100000))

    val olderEvent = sampleEvent.copy(date = olderDate)
    val newerSerie = sampleSerie.copy(date = newerDate)

    val items: List<EventItem> =
        listOf(EventItem.EventSerie(newerSerie), EventItem.SingleEvent(olderEvent))

    val sortedItems = items.sortedBy { it.date.seconds }

    assertEquals("event123", sortedItems[0].eventItemId)
    assertEquals("serie123", sortedItems[1].eventItemId)
  }

  @Test
  fun `EventItem can be pattern matched using when expression`() {
    val singleEvent: EventItem = EventItem.SingleEvent(sampleEvent)

    val result =
        when (singleEvent) {
          is EventItem.SingleEvent -> "single"
          is EventItem.EventSerie -> "serie"
        }

    assertEquals("single", result)
  }

  @Test
  fun `EventSerie can be pattern matched using when expression`() {
    val eventSerie: EventItem = EventItem.EventSerie(sampleSerie)

    val result =
        when (eventSerie) {
          is EventItem.SingleEvent -> "single"
          is EventItem.EventSerie -> "serie"
        }

    assertEquals("serie", result)
  }

  @Test
  fun `SingleEvent with different titles are not equal`() {
    val event1 = EventItem.SingleEvent(sampleEvent.copy(title = "Title 1"))
    val event2 = EventItem.SingleEvent(sampleEvent.copy(title = "Title 2"))
    assertNotEquals(event1, event2)
  }

  @Test
  fun `EventSerie with different titles are not equal`() {
    val serie1 = EventItem.EventSerie(sampleSerie.copy(title = "Title 1"))
    val serie2 = EventItem.EventSerie(sampleSerie.copy(title = "Title 2"))
    assertNotEquals(serie1, serie2)
  }

  @Test
  fun `SingleEvent with different dates are not equal`() {
    val date1 = Timestamp(Date(System.currentTimeMillis()))
    val date2 = Timestamp(Date(System.currentTimeMillis() + 100000))
    val event1 = EventItem.SingleEvent(sampleEvent.copy(date = date1))
    val event2 = EventItem.SingleEvent(sampleEvent.copy(date = date2))
    assertNotEquals(event1, event2)
  }

  @Test
  fun `EventSerie with different dates are not equal`() {
    val date1 = Timestamp(Date(System.currentTimeMillis()))
    val date2 = Timestamp(Date(System.currentTimeMillis() + 100000))
    val serie1 = EventItem.EventSerie(sampleSerie.copy(date = date1))
    val serie2 = EventItem.EventSerie(sampleSerie.copy(date = date2))
    assertNotEquals(serie1, serie2)
  }

  @Test
  fun `EventItems list can be filtered by type`() {
    val items: List<EventItem> =
        listOf(
            EventItem.SingleEvent(sampleEvent),
            EventItem.EventSerie(sampleSerie),
            EventItem.SingleEvent(sampleEvent.copy(eventId = "event456")))

    val singleEvents = items.filterIsInstance<EventItem.SingleEvent>()
    val eventSeries = items.filterIsInstance<EventItem.EventSerie>()

    assertEquals(2, singleEvents.size)
    assertEquals(1, eventSeries.size)
  }

  @Test
  fun `EventItems can access underlying data through casting`() {
    val singleEvent: EventItem = EventItem.SingleEvent(sampleEvent)

    when (singleEvent) {
      is EventItem.SingleEvent -> {
        assertEquals("owner123", singleEvent.event.ownerId)
        assertEquals(EventType.SPORTS, singleEvent.event.type)
      }
      is EventItem.EventSerie -> {}
    }
  }

  @Test
  fun `EventSerie can access underlying data through casting`() {
    val eventSerie: EventItem = EventItem.EventSerie(sampleSerie)

    when (eventSerie) {
      is EventItem.SingleEvent -> {}
      is EventItem.EventSerie -> {
        assertEquals("owner123", eventSerie.serie.ownerId)
        assertEquals(3, eventSerie.serie.eventIds.size)
      }
    }
  }
}
