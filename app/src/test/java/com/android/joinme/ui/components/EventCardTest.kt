package com.android.joinme.ui.components

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.android.joinme.model.event.Event
import com.android.joinme.model.event.EventType
import com.android.joinme.model.event.EventVisibility
import com.android.joinme.model.map.Location
import com.google.firebase.Timestamp
import java.util.Calendar
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests with Robolectric for the EventCard component.
 *
 * Tests the UI behavior, display, and interactions of the EventCard component.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "w360dp-h640dp-normal-long-notround-any-420dpi-keyshidden-nonav")
class EventCardTest {

  @get:Rule val composeTestRule = createComposeRule()

  private fun createEvent(
      eventId: String,
      title: String,
      type: EventType,
      location: Location? = null
  ): Event {
    return Event(
        eventId = eventId,
        type = type,
        title = title,
        description = "Test description",
        location = location,
        date = Timestamp.now(),
        duration = 60,
        participants = listOf("user1"),
        maxParticipants = 10,
        visibility = EventVisibility.PUBLIC,
        ownerId = "owner1")
  }

  @Test
  fun eventCard_displaysEventTitle() {
    val event = createEvent("1", "Basketball Game", EventType.SPORTS)

    composeTestRule.setContent { EventCard(event = event, onClick = {}, testTag = "testCard") }

    composeTestRule.onNodeWithText("Basketball Game").assertIsDisplayed()
  }

  @Test
  fun eventCard_displaysLocation_whenLocationProvided() {
    val location = Location(46.5191, 6.5668, "EPFL")
    val event = createEvent("1", "Test Event", EventType.SPORTS, location)

    composeTestRule.setContent { EventCard(event = event, onClick = {}, testTag = "testCard") }

    composeTestRule.onNodeWithText("Place : EPFL").assertIsDisplayed()
  }

  @Test
  fun eventCard_displaysUnknown_whenLocationIsNull() {
    val event = createEvent("1", "Test Event", EventType.SPORTS, null)

    composeTestRule.setContent { EventCard(event = event, onClick = {}, testTag = "testCard") }

    composeTestRule.onNodeWithText("Place : Unknown").assertIsDisplayed()
  }

  @Test
  fun eventCard_clickTriggersCallback() {
    var clicked = false
    val event = createEvent("1", "Test Event", EventType.SPORTS)

    composeTestRule.setContent {
      EventCard(event = event, onClick = { clicked = true }, testTag = "testCard")
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("testCard").performClick()
    composeTestRule.waitForIdle()

    assert(clicked)
  }

  @Test
  fun eventCard_hasCorrectTestTag() {
    val event = createEvent("1", "Test Event", EventType.SPORTS)

    composeTestRule.setContent { EventCard(event = event, onClick = {}, testTag = "customTestTag") }

    composeTestRule.onNodeWithTag("customTestTag").assertExists()
  }

  @Test
  fun eventCard_formatsDateCorrectly() {
    val calendar = Calendar.getInstance()
    calendar.set(2025, Calendar.JUNE, 15, 14, 30, 0)
    val timestamp = Timestamp(calendar.time)

    val event =
        Event(
            eventId = "1",
            type = EventType.SPORTS,
            title = "Test Event",
            description = "desc",
            location = null,
            date = timestamp,
            duration = 60,
            participants = listOf("user1"),
            maxParticipants = 10,
            visibility = EventVisibility.PUBLIC,
            ownerId = "owner1")

    composeTestRule.setContent { EventCard(event = event, onClick = {}, testTag = "testCard") }

    composeTestRule.onNodeWithText("15/06/2025").assertExists()
    composeTestRule.onNodeWithText("14h30").assertExists()
  }

  @Test
  fun eventCard_formatsTimeCorrectly_withSingleDigitMinutes() {
    val calendar = Calendar.getInstance()
    calendar.set(2025, Calendar.JANUARY, 1, 9, 5, 0)
    val timestamp = Timestamp(calendar.time)

    val event =
        Event(
            eventId = "1",
            type = EventType.SPORTS,
            title = "Test Event",
            description = "desc",
            location = null,
            date = timestamp,
            duration = 60,
            participants = listOf("user1"),
            maxParticipants = 10,
            visibility = EventVisibility.PUBLIC,
            ownerId = "owner1")

    composeTestRule.setContent { EventCard(event = event, onClick = {}, testTag = "testCard") }

    composeTestRule.onNodeWithText("01/01/2025").assertExists()
    composeTestRule.onNodeWithText("09h05").assertExists()
  }

  @Test
  fun eventCard_handlesLongTitle() {
    val longTitle = "This is a very long event title that might need to wrap to multiple lines"
    val event = createEvent("1", longTitle, EventType.SPORTS)

    composeTestRule.setContent { EventCard(event = event, onClick = {}, testTag = "testCard") }

    composeTestRule.onNodeWithText(longTitle).assertExists()
  }

  @Test
  fun eventCard_handlesLongLocationName() {
    val longLocationName = "This is a very long location name that might need special handling"
    val location = Location(46.5191, 6.5668, longLocationName)
    val event = createEvent("1", "Test Event", EventType.SPORTS, location)

    composeTestRule.setContent { EventCard(event = event, onClick = {}, testTag = "testCard") }

    composeTestRule.onNodeWithText("Place : $longLocationName").assertExists()
  }

  @Test
  fun eventCard_differentEventTypesDisplayed() {
    val sportsEvent = createEvent("1", "Basketball", EventType.SPORTS)
    val socialEvent = createEvent("2", "Party", EventType.SOCIAL)
    val activityEvent = createEvent("3", "Hiking", EventType.ACTIVITY)

    composeTestRule.setContent {
      androidx.compose.foundation.layout.Column {
        EventCard(event = sportsEvent, onClick = {}, testTag = "sports")
        EventCard(event = socialEvent, onClick = {}, testTag = "social")
        EventCard(event = activityEvent, onClick = {}, testTag = "activity")
      }
    }

    composeTestRule.onNodeWithText("Basketball").assertExists()
    composeTestRule.onNodeWithText("Party").assertExists()
    composeTestRule.onNodeWithText("Hiking").assertExists()
  }
}
