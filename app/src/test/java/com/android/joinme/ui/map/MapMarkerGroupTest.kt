package com.android.joinme.ui.map

import androidx.compose.ui.graphics.Color
import com.android.joinme.model.event.Event
import com.android.joinme.model.event.EventType
import com.android.joinme.model.map.Location
import com.android.joinme.model.serie.Serie
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MapMarkerGroupTest {

  private val testLocation = Location(latitude = 46.5191, longitude = 6.5668, name = "EPFL")
  private val testLatLng = LatLng(46.5191, 6.5668)
  private val testColor = Color.Red

  private val testEvent =
      Event(
          eventId = "event1",
          type = EventType.SPORTS,
          title = "Test Event",
          description = "Test Description",
          location = testLocation,
          date = Timestamp.now(),
          duration = 120,
          participants = emptyList(),
          maxParticipants = 10,
          visibility = com.android.joinme.model.event.EventVisibility.PUBLIC,
          ownerId = "user1")

  private val testSerie =
      Serie(
          serieId = "serie1",
          title = "Test Serie",
          description = "Serie Description",
          date = Timestamp.now(),
          participants = emptyList(),
          maxParticipants = 10,
          visibility = com.android.joinme.model.utils.Visibility.PUBLIC,
          eventIds = listOf("event1", "event2"),
          ownerId = "user1")

  @Test
  fun `EventItem creates correct MapItem with event data`() {
    val eventItem = MapItem.EventItem(event = testEvent, color = testColor)

    assertEquals(testEvent.eventId, eventItem.id)
    assertEquals(testEvent.title, eventItem.title)
    assertEquals(testColor, eventItem.color)
    assertEquals(testLatLng.latitude, eventItem.position.latitude, 0.0001)
    assertEquals(testLatLng.longitude, eventItem.position.longitude, 0.0001)
    assertEquals(testEvent, eventItem.event)
  }

  @Test
  fun `EventItem with null location has default position`() {
    val eventWithNullLocation = testEvent.copy(location = null)
    val eventItem = MapItem.EventItem(event = eventWithNullLocation, color = testColor)

    assertEquals(0.0, eventItem.position.latitude, 0.0001)
    assertEquals(0.0, eventItem.position.longitude, 0.0001)
  }

  @Test
  fun `SerieItem creates correct MapItem with serie data`() {
    val serieItem = MapItem.SerieItem(serie = testSerie, location = testLocation, color = testColor)

    assertEquals(testSerie.serieId, serieItem.id)
    assertEquals(testSerie.title, serieItem.title)
    assertEquals(testColor, serieItem.color)
    assertEquals(testLatLng.latitude, serieItem.position.latitude, 0.0001)
    assertEquals(testLatLng.longitude, serieItem.position.longitude, 0.0001)
    assertEquals(testSerie, serieItem.serie)
    assertEquals(testLocation, serieItem.location)
  }

  @Test
  fun `MapMarkerGroup with single item returns isSingle true`() {
    val singleItem = MapItem.EventItem(event = testEvent, color = testColor)
    val group = MapMarkerGroup(position = testLatLng, items = listOf(singleItem))

    assertTrue(group.isSingle)
    assertEquals(1, group.count)
    assertNotNull(group.singleItemColor)
    assertEquals(testColor, group.singleItemColor)
  }

  @Test
  fun `MapMarkerGroup with multiple items returns isSingle false`() {
    val item1 = MapItem.EventItem(event = testEvent, color = Color.Red)
    val item2 = MapItem.EventItem(event = testEvent.copy(eventId = "event2"), color = Color.Blue)
    val group = MapMarkerGroup(position = testLatLng, items = listOf(item1, item2))

    assertFalse(group.isSingle)
    assertEquals(2, group.count)
    assertNull(group.singleItemColor)
  }

  @Test
  fun `MapMarkerGroup count reflects number of items`() {
    val items =
        listOf(
            MapItem.EventItem(event = testEvent, color = Color.Red),
            MapItem.EventItem(event = testEvent.copy(eventId = "event2"), color = Color.Blue),
            MapItem.EventItem(event = testEvent.copy(eventId = "event3"), color = Color.Green))
    val group = MapMarkerGroup(position = testLatLng, items = items)

    assertEquals(3, group.count)
    assertEquals(items.size, group.count)
  }

  @Test
  fun `MapMarkerGroup position matches provided LatLng`() {
    val customPosition = LatLng(47.0, 7.0)
    val item = MapItem.EventItem(event = testEvent, color = testColor)
    val group = MapMarkerGroup(position = customPosition, items = listOf(item))

    assertEquals(customPosition.latitude, group.position.latitude, 0.0001)
    assertEquals(customPosition.longitude, group.position.longitude, 0.0001)
  }

  @Test
  fun `MapMarkerGroup with empty list has count zero`() {
    val group = MapMarkerGroup(position = testLatLng, items = emptyList())

    assertEquals(0, group.count)
    assertFalse(group.isSingle)
    assertNull(group.singleItemColor)
  }

  @Test
  fun `singleItemColor returns correct color for single SerieItem`() {
    val item = MapItem.SerieItem(serie = testSerie, location = testLocation, color = Color.Cyan)
    val group = MapMarkerGroup(position = testLatLng, items = listOf(item))

    assertEquals(Color.Cyan, group.singleItemColor)
  }

  @Test
  fun `MapMarkerGroup can contain mixed EventItem and SerieItem`() {
    val eventItem = MapItem.EventItem(event = testEvent, color = Color.Red)
    val serieItem =
        MapItem.SerieItem(serie = testSerie, location = testLocation, color = Color.Blue)
    val group = MapMarkerGroup(position = testLatLng, items = listOf(eventItem, serieItem))

    assertEquals(2, group.count)
    assertFalse(group.isSingle)
    assertEquals(eventItem, group.items[0])
    assertEquals(serieItem, group.items[1])
  }
}
