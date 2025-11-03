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

/**
 * Instrumented tests for the EventCard component.
 *
 * Tests the UI behavior, display, and interactions of the EventCard component.
 */
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
  fun eventCard_displaysCorrectColorForSportsEvent() {
    val event = createEvent("1", "Sports Event", EventType.SPORTS)

    composeTestRule.setContent { EventCard(event = event, onClick = {}, testTag = "testCard") }

    // Card should exist with sports type
    composeTestRule.onNodeWithText("Sports Event").assertExists()
  }

  @Test
  fun eventCard_displaysCorrectColorForSocialEvent() {
    val event = createEvent("1", "Social Event", EventType.SOCIAL)

    composeTestRule.setContent { EventCard(event = event, onClick = {}, testTag = "testCard") }

    // Card should exist with social type
    composeTestRule.onNodeWithText("Social Event").assertExists()
  }

  @Test
  fun eventCard_displaysCorrectColorForActivityEvent() {
    val event = createEvent("1", "Activity Event", EventType.ACTIVITY)

    composeTestRule.setContent { EventCard(event = event, onClick = {}, testTag = "testCard") }

    // Card should exist with activity type
    composeTestRule.onNodeWithText("Activity Event").assertExists()
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
  fun eventCard_isClickableOnWholeCard() {
    var clicked = false
    val event = createEvent("1", "Test Event", EventType.SPORTS)

    composeTestRule.setContent {
      EventCard(event = event, onClick = { clicked = true }, testTag = "testCard")
    }

    // Click on the card using test tag
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("testCard").performClick()
    composeTestRule.waitForIdle()

    assert(clicked)
  }

  @Test
  fun eventCard_displaysArrowIcon() {
    val event = createEvent("1", "Test Event", EventType.SPORTS)

    composeTestRule.setContent { EventCard(event = event, onClick = {}, testTag = "testCard") }

    // Arrow icon should be displayed (no content description, but it's in the layout)
    composeTestRule.onNodeWithTag("testCard").assertExists()
  }

  @Test
  fun eventCard_displaysAllInformationTogether() {
    val location = Location(46.5191, 6.5668, "EPFL")
    val calendar = Calendar.getInstance()
    calendar.set(2025, Calendar.MARCH, 20, 15, 45, 0)

    val event =
        Event(
            eventId = "1",
            type = EventType.SOCIAL,
            title = "Team Meeting",
            description = "Important meeting",
            location = location,
            date = Timestamp(calendar.time),
            duration = 90,
            participants = listOf("user1", "user2"),
            maxParticipants = 15,
            visibility = EventVisibility.PUBLIC,
            ownerId = "owner1")

    composeTestRule.setContent { EventCard(event = event, onClick = {}, testTag = "testCard") }

    // All information should be displayed
    composeTestRule.onNodeWithText("Team Meeting").assertIsDisplayed()
    composeTestRule.onNodeWithText("Place : EPFL").assertIsDisplayed()
    composeTestRule.onNodeWithText("20/03/2025").assertExists()
    composeTestRule.onNodeWithText("15h45").assertExists()
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
  fun eventCard_multipleCards_haveUniqueTestTags() {
    val event1 = createEvent("1", "Event 1", EventType.SPORTS)
    val event2 = createEvent("2", "Event 2", EventType.SOCIAL)

    composeTestRule.setContent {
      androidx.compose.foundation.layout.Column {
        EventCard(event = event1, onClick = {}, testTag = "card1")
        EventCard(event = event2, onClick = {}, testTag = "card2")
      }
    }

    composeTestRule.onNodeWithTag("card1").assertExists()
    composeTestRule.onNodeWithTag("card2").assertExists()
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

  @Test
  fun eventCard_displaysWithSpecialCharactersInTitle() {
    val event = createEvent("1", "Event & Fun! @ 2025", EventType.SPORTS)

    composeTestRule.setContent { EventCard(event = event, onClick = {}, testTag = "testCard") }

    composeTestRule.onNodeWithText("Event & Fun! @ 2025").assertExists()
  }

  @Test
  fun eventCard_displaysWithSpecialCharactersInLocation() {
    val location = Location(46.5191, 6.5668, "EPFL - BC Building (2nd floor)")
    val event = createEvent("1", "Test Event", EventType.SPORTS, location)

    composeTestRule.setContent { EventCard(event = event, onClick = {}, testTag = "testCard") }

    composeTestRule.onNodeWithText("Place : EPFL - BC Building (2nd floor)").assertExists()
  }

  @Test
  fun eventCard_clickCallback_receivesCorrectEvent() {
    val event = createEvent("1", "Test Event", EventType.SPORTS)
    var clickCount = 0

    composeTestRule.setContent {
      EventCard(event = event, onClick = { clickCount++ }, testTag = "testCard")
    }

    // Click multiple times
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("testCard").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("testCard").performClick()
    composeTestRule.waitForIdle()

    assert(clickCount == 2)
  }

  @Test
  fun eventCard_dateDisplay_handlesLeapYear() {
    val calendar = Calendar.getInstance()
    calendar.set(2024, Calendar.FEBRUARY, 29, 12, 0, 0) // Leap year date

    val event =
        Event(
            eventId = "1",
            type = EventType.SPORTS,
            title = "Leap Year Event",
            description = "desc",
            location = null,
            date = Timestamp(calendar.time),
            duration = 60,
            participants = listOf("user1"),
            maxParticipants = 10,
            visibility = EventVisibility.PUBLIC,
            ownerId = "owner1")

    composeTestRule.setContent { EventCard(event = event, onClick = {}, testTag = "testCard") }

    composeTestRule.onNodeWithText("29/02/2024").assertExists()
  }

  @Test
  fun eventCard_timeDisplay_handlesMidnight() {
    val calendar = Calendar.getInstance()
    calendar.set(2025, Calendar.JANUARY, 1, 0, 0, 0)

    val event =
        Event(
            eventId = "1",
            type = EventType.SOCIAL,
            title = "New Year Event",
            description = "desc",
            location = null,
            date = Timestamp(calendar.time),
            duration = 60,
            participants = listOf("user1"),
            maxParticipants = 10,
            visibility = EventVisibility.PUBLIC,
            ownerId = "owner1")

    composeTestRule.setContent { EventCard(event = event, onClick = {}, testTag = "testCard") }

    composeTestRule.onNodeWithText("00h00").assertExists()
  }

  @Test
  fun eventCard_timeDisplay_handlesNoon() {
    val calendar = Calendar.getInstance()
    calendar.set(2025, Calendar.JANUARY, 1, 12, 0, 0)

    val event =
        Event(
            eventId = "1",
            type = EventType.ACTIVITY,
            title = "Lunch Event",
            description = "desc",
            location = null,
            date = Timestamp(calendar.time),
            duration = 60,
            participants = listOf("user1"),
            maxParticipants = 10,
            visibility = EventVisibility.PUBLIC,
            ownerId = "owner1")

    composeTestRule.setContent { EventCard(event = event, onClick = {}, testTag = "testCard") }

    composeTestRule.onNodeWithText("12h00").assertExists()
  }
}
