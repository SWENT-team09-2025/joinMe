package com.android.joinme.ui.map

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.android.joinme.model.event.Event
import com.android.joinme.model.event.EventType
import com.android.joinme.model.map.Location
import com.android.joinme.model.serie.Serie
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GroupedItemsBottomSheetTest {

  @get:Rule val composeTestRule = createComposeRule()

  private val testLocation = Location(latitude = 46.5191, longitude = 6.5668, name = "EPFL")
  private val testLatLng = LatLng(46.5191, 6.5668)

  private val testEvent1 =
      Event(
          eventId = "event1",
          type = EventType.SPORTS,
          title = "Sports Event",
          description = "A sports event",
          location = testLocation,
          date = Timestamp.now(),
          duration = 120,
          participants = emptyList(),
          maxParticipants = 10,
          visibility = com.android.joinme.model.event.EventVisibility.PUBLIC,
          ownerId = "user1")

  private val testEvent2 =
      Event(
          eventId = "event2",
          type = EventType.SOCIAL,
          title = "Social Event",
          description = "A social event",
          location = testLocation,
          date = Timestamp.now(),
          duration = 60,
          participants = emptyList(),
          maxParticipants = 20,
          visibility = com.android.joinme.model.event.EventVisibility.PUBLIC,
          ownerId = "user2")

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
  fun `GroupedItemsBottomSheet displays correct title for multiple events`() {
    val item1 = MapItem.EventItem(event = testEvent1, color = Color.Red)
    val item2 = MapItem.EventItem(event = testEvent2, color = Color.Blue)
    val group = MapMarkerGroup(position = testLatLng, items = listOf(item1, item2))

    composeTestRule.setContent {
      GroupedItemsBottomSheet(group = group, onItemClick = {}, onDismiss = {})
    }

    composeTestRule
        .onNodeWithText("2 activities in this place", substring = true)
        .assertIsDisplayed()
  }

  @Test
  fun `GroupedItemsBottomSheet displays event title`() {
    val eventItem = MapItem.EventItem(event = testEvent1, color = Color.Red)
    val group = MapMarkerGroup(position = testLatLng, items = listOf(eventItem))

    composeTestRule.setContent {
      GroupedItemsBottomSheet(group = group, onItemClick = {}, onDismiss = {})
    }

    composeTestRule.onNodeWithText("Sports Event").assertIsDisplayed()
  }

  @Test
  fun `GroupedItemsBottomSheet displays event type for EventItem`() {
    val eventItem = MapItem.EventItem(event = testEvent1, color = Color.Red)
    val group = MapMarkerGroup(position = testLatLng, items = listOf(eventItem))

    composeTestRule.setContent {
      GroupedItemsBottomSheet(group = group, onItemClick = {}, onDismiss = {})
    }

    composeTestRule.onNodeWithText("Sports").assertIsDisplayed()
  }

  @Test
  fun `GroupedItemsBottomSheet displays all event titles`() {
    val item1 = MapItem.EventItem(event = testEvent1, color = Color.Red)
    val item2 = MapItem.EventItem(event = testEvent2, color = Color.Blue)
    val group = MapMarkerGroup(position = testLatLng, items = listOf(item1, item2))

    composeTestRule.setContent {
      GroupedItemsBottomSheet(group = group, onItemClick = {}, onDismiss = {})
    }

    composeTestRule.onNodeWithText("Sports Event").assertIsDisplayed()
    composeTestRule.onNodeWithText("Social Event").assertIsDisplayed()
  }

  @Test
  fun `GroupedItemsBottomSheet displays serie title`() {
    val serieItem =
        MapItem.SerieItem(serie = testSerie, location = testLocation, color = Color.Green)
    val group = MapMarkerGroup(position = testLatLng, items = listOf(serieItem))

    composeTestRule.setContent {
      GroupedItemsBottomSheet(group = group, onItemClick = {}, onDismiss = {})
    }

    composeTestRule.onNodeWithText("Test Serie").assertIsDisplayed()
  }

  @Test
  fun `GroupedItemsBottomSheet has correct test tag`() {
    val eventItem = MapItem.EventItem(event = testEvent1, color = Color.Red)
    val group = MapMarkerGroup(position = testLatLng, items = listOf(eventItem))

    composeTestRule.setContent {
      GroupedItemsBottomSheet(group = group, onItemClick = {}, onDismiss = {})
    }

    composeTestRule.onNodeWithTag(MapScreenTestTags.GROUPED_INFO_WINDOW).assertIsDisplayed()
  }

  @Test
  fun `GroupedItemsBottomSheet has test tags for each item`() {
    val item1 = MapItem.EventItem(event = testEvent1, color = Color.Red)
    val item2 = MapItem.EventItem(event = testEvent2, color = Color.Blue)
    val group = MapMarkerGroup(position = testLatLng, items = listOf(item1, item2))

    composeTestRule.setContent {
      GroupedItemsBottomSheet(group = group, onItemClick = {}, onDismiss = {})
    }

    composeTestRule.onNodeWithTag(MapScreenTestTags.getTestTagForGroupedItem(0)).assertIsDisplayed()
    composeTestRule.onNodeWithTag(MapScreenTestTags.getTestTagForGroupedItem(1)).assertIsDisplayed()
  }

  @Test
  fun `clicking on event item triggers onItemClick callback`() {
    val eventItem = MapItem.EventItem(event = testEvent1, color = Color.Red)
    val group = MapMarkerGroup(position = testLatLng, items = listOf(eventItem))
    var clickedItem: MapItem? = null

    composeTestRule.setContent {
      GroupedItemsBottomSheet(group = group, onItemClick = { clickedItem = it }, onDismiss = {})
    }

    composeTestRule.onNodeWithText("Sports Event").performClick()

    assertEquals(eventItem, clickedItem)
  }

  @Test
  fun `GroupedItemsBottomSheet displays mixed event and serie items`() {
    val eventItem = MapItem.EventItem(event = testEvent1, color = Color.Red)
    val serieItem =
        MapItem.SerieItem(serie = testSerie, location = testLocation, color = Color.Green)
    val group = MapMarkerGroup(position = testLatLng, items = listOf(eventItem, serieItem))

    composeTestRule.setContent {
      GroupedItemsBottomSheet(group = group, onItemClick = {}, onDismiss = {})
    }

    composeTestRule.onNodeWithText("Sports Event").assertIsDisplayed()
    composeTestRule.onNodeWithText("Test Serie").assertIsDisplayed()
    composeTestRule.onNodeWithText("Sports").assertIsDisplayed()
  }

  @Test
  fun `GroupedItemsBottomSheet displays three events correctly`() {
    val testEvent3 =
        testEvent1.copy(eventId = "event3", title = "Activity Event", type = EventType.ACTIVITY)
    val item1 = MapItem.EventItem(event = testEvent1, color = Color.Red)
    val item2 = MapItem.EventItem(event = testEvent2, color = Color.Blue)
    val item3 = MapItem.EventItem(event = testEvent3, color = Color.Yellow)
    val group = MapMarkerGroup(position = testLatLng, items = listOf(item1, item2, item3))

    composeTestRule.setContent {
      GroupedItemsBottomSheet(group = group, onItemClick = {}, onDismiss = {})
    }

    composeTestRule
        .onNodeWithText("3 activities in this place", substring = true)
        .assertIsDisplayed()
    composeTestRule.onNodeWithText("Sports Event").assertIsDisplayed()
    composeTestRule.onNodeWithText("Social Event").assertIsDisplayed()
    composeTestRule.onNodeWithText("Activity Event").assertIsDisplayed()
  }

  @Test
  fun `clicking on different items triggers correct callbacks`() {
    val item1 = MapItem.EventItem(event = testEvent1, color = Color.Red)
    val item2 = MapItem.EventItem(event = testEvent2, color = Color.Blue)
    val group = MapMarkerGroup(position = testLatLng, items = listOf(item1, item2))
    val clickedItems = mutableListOf<MapItem>()

    composeTestRule.setContent {
      GroupedItemsBottomSheet(group = group, onItemClick = { clickedItems.add(it) }, onDismiss = {})
    }

    composeTestRule.onNodeWithText("Sports Event").performClick()
    composeTestRule.onNodeWithText("Social Event").performClick()

    assertEquals(2, clickedItems.size)
    assertEquals(item1, clickedItems[0])
    assertEquals(item2, clickedItems[1])
  }

  @Test
  fun `GroupedItemRow displays item correctly`() {
    val eventItem = MapItem.EventItem(event = testEvent1, color = Color.Red)
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    composeTestRule.setContent {
      GroupedItemRow(item = eventItem, onItemClick = {}, context = context)
    }

    composeTestRule.onNodeWithText("Sports Event").assertIsDisplayed()
    composeTestRule.onNodeWithText("Sports").assertIsDisplayed()
  }

  @Test
  fun `GroupedItemRow click triggers callback`() {
    val eventItem = MapItem.EventItem(event = testEvent1, color = Color.Red)
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    var clicked = false

    composeTestRule.setContent {
      GroupedItemRow(item = eventItem, onItemClick = { clicked = true }, context = context)
    }

    composeTestRule.onNodeWithText("Sports Event").performClick()

    assertTrue(clicked)
  }
}
