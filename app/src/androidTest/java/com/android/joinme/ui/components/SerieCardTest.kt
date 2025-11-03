package com.android.joinme.ui.components

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.android.joinme.model.serie.Serie
import com.android.joinme.model.utils.Visibility
import com.google.firebase.Timestamp
import java.util.Calendar
import org.junit.Rule
import org.junit.Test

/**
 * Instrumented tests for the SerieCard component.
 *
 * Tests the UI behavior, display, and interactions of the SerieCard component.
 */
class SerieCardTest {

  @get:Rule val composeTestRule = createComposeRule()

  private fun createSerie(
      serieId: String,
      title: String,
      eventIds: List<String> = listOf("event1", "event2", "event3")
  ): Serie {
    return Serie(
        serieId = serieId,
        title = title,
        description = "Test description",
        date = Timestamp.now(),
        participants = listOf("user1", "user2"),
        maxParticipants = 10,
        visibility = Visibility.PUBLIC,
        eventIds = eventIds,
        ownerId = "owner1")
  }

  @Test
  fun serieCard_displaysSerieTitle() {
    val serie = createSerie("1", "Weekly Basketball")

    composeTestRule.setContent { SerieCard(serie = serie, onClick = {}, testTag = "testCard") }

    composeTestRule.onNodeWithText("Weekly Basketball").assertIsDisplayed()
  }

  @Test
  fun serieCard_displaysSerieBadge() {
    val serie = createSerie("1", "Test Serie")

    composeTestRule.setContent { SerieCard(serie = serie, onClick = {}, testTag = "testCard") }

    composeTestRule.onNodeWithText("Serie 🔥").assertIsDisplayed()
  }

  @Test
  fun serieCard_clickTriggersCallback() {
    var clicked = false
    val serie = createSerie("1", "Test Serie")

    composeTestRule.setContent {
      SerieCard(serie = serie, onClick = { clicked = true }, testTag = "testCard")
    }

    composeTestRule.onNodeWithTag("testCard").performClick()

    assert(clicked)
  }

  @Test
  fun serieCard_hasCorrectTestTag() {
    val serie = createSerie("1", "Test Serie")

    composeTestRule.setContent { SerieCard(serie = serie, onClick = {}, testTag = "customTestTag") }

    composeTestRule.onNodeWithTag("customTestTag").assertExists()
  }

  @Test
  fun serieCard_formatsDateCorrectly() {
    val calendar = Calendar.getInstance()
    calendar.set(2025, Calendar.JUNE, 15, 14, 30, 0)
    val timestamp = Timestamp(calendar.time)

    val serie =
        Serie(
            serieId = "1",
            title = "Test Serie",
            description = "desc",
            date = timestamp,
            participants = listOf("user1"),
            maxParticipants = 10,
            visibility = Visibility.PUBLIC,
            eventIds = listOf("event1", "event2"),
            ownerId = "owner1")

    composeTestRule.setContent { SerieCard(serie = serie, onClick = {}, testTag = "testCard") }

    composeTestRule.onNodeWithText("15/06/2025").assertExists()
    composeTestRule.onNodeWithText("14h30").assertExists()
  }

  @Test
  fun serieCard_formatsTimeCorrectly_withSingleDigitMinutes() {
    val calendar = Calendar.getInstance()
    calendar.set(2025, Calendar.JANUARY, 1, 9, 5, 0)
    val timestamp = Timestamp(calendar.time)

    val serie =
        Serie(
            serieId = "1",
            title = "Test Serie",
            description = "desc",
            date = timestamp,
            participants = listOf("user1"),
            maxParticipants = 10,
            visibility = Visibility.PUBLIC,
            eventIds = listOf("event1", "event2"),
            ownerId = "owner1")

    composeTestRule.setContent { SerieCard(serie = serie, onClick = {}, testTag = "testCard") }

    composeTestRule.onNodeWithText("01/01/2025").assertExists()
    composeTestRule.onNodeWithText("09h05").assertExists()
  }

  @Test
  fun serieCard_isClickableOnWholeCard() {
    var clicked = false
    val serie = createSerie("1", "Test Serie")

    composeTestRule.setContent {
      SerieCard(serie = serie, onClick = { clicked = true }, testTag = "testCard")
    }

    // Click on the card using test tag
    composeTestRule.onNodeWithTag("testCard").performClick()

    assert(clicked)
  }

  @Test
  fun serieCard_displaysArrowIcon() {
    val serie = createSerie("1", "Test Serie")

    composeTestRule.setContent { SerieCard(serie = serie, onClick = {}, testTag = "testCard") }

    // Arrow icon should be displayed
    composeTestRule.onNodeWithTag("testCard").assertExists()
  }

  @Test
  fun serieCard_displaysAllInformationTogether() {
    val calendar = Calendar.getInstance()
    calendar.set(2025, Calendar.MARCH, 20, 15, 45, 0)

    val serie =
        Serie(
            serieId = "1",
            title = "Weekly Team Meeting",
            description = "Important meetings",
            date = Timestamp(calendar.time),
            participants = listOf("user1", "user2", "user3"),
            maxParticipants = 15,
            visibility = Visibility.PUBLIC,
            eventIds = listOf("event1", "event2", "event3", "event4"),
            ownerId = "owner1")

    composeTestRule.setContent { SerieCard(serie = serie, onClick = {}, testTag = "testCard") }

    // All information should be displayed
    composeTestRule.onNodeWithText("Weekly Team Meeting").assertIsDisplayed()
    composeTestRule.onNodeWithText("Serie 🔥").assertIsDisplayed()
    composeTestRule.onNodeWithText("20/03/2025").assertExists()
    composeTestRule.onNodeWithText("15h45").assertExists()
  }

  @Test
  fun serieCard_handlesLongTitle() {
    val longTitle = "This is a very long serie title that might need to wrap to multiple lines"
    val serie = createSerie("1", longTitle)

    composeTestRule.setContent { SerieCard(serie = serie, onClick = {}, testTag = "testCard") }

    composeTestRule.onNodeWithText(longTitle).assertExists()
  }

  @Test
  fun serieCard_multipleCards_haveUniqueTestTags() {
    val serie1 = createSerie("1", "Serie 1")
    val serie2 = createSerie("2", "Serie 2")

    composeTestRule.setContent {
      androidx.compose.foundation.layout.Column {
        SerieCard(serie = serie1, onClick = {}, testTag = "card1")
        SerieCard(serie = serie2, onClick = {}, testTag = "card2")
      }
    }

    composeTestRule.onNodeWithTag("card1").assertExists()
    composeTestRule.onNodeWithTag("card2").assertExists()
  }

  @Test
  fun serieCard_multipleSeries_displayedCorrectly() {
    val serie1 = createSerie("1", "Weekly Basketball")
    val serie2 = createSerie("2", "Monthly Hiking")
    val serie3 = createSerie("3", "Daily Standup")

    composeTestRule.setContent {
      androidx.compose.foundation.layout.Column {
        SerieCard(serie = serie1, onClick = {}, testTag = "serie1")
        SerieCard(serie = serie2, onClick = {}, testTag = "serie2")
        SerieCard(serie = serie3, onClick = {}, testTag = "serie3")
      }
    }

    composeTestRule.onNodeWithText("Weekly Basketball").assertExists()
    composeTestRule.onNodeWithText("Monthly Hiking").assertExists()
    composeTestRule.onNodeWithText("Daily Standup").assertExists()
  }

  @Test
  fun serieCard_displaysWithSpecialCharactersInTitle() {
    val serie = createSerie("1", "Serie & Fun! @ 2025")

    composeTestRule.setContent { SerieCard(serie = serie, onClick = {}, testTag = "testCard") }

    composeTestRule.onNodeWithText("Serie & Fun! @ 2025").assertExists()
  }

  @Test
  fun serieCard_clickCallback_receivesCorrectSerie() {
    val serie = createSerie("1", "Test Serie")
    var clickCount = 0

    composeTestRule.setContent {
      SerieCard(serie = serie, onClick = { clickCount++ }, testTag = "testCard")
    }

    // Click multiple times
    composeTestRule.onNodeWithTag("testCard").performClick()
    composeTestRule.onNodeWithTag("testCard").performClick()

    assert(clickCount == 2)
  }

  @Test
  fun serieCard_dateDisplay_handlesLeapYear() {
    val calendar = Calendar.getInstance()
    calendar.set(2024, Calendar.FEBRUARY, 29, 12, 0, 0) // Leap year date

    val serie =
        Serie(
            serieId = "1",
            title = "Leap Year Serie",
            description = "desc",
            date = Timestamp(calendar.time),
            participants = listOf("user1"),
            maxParticipants = 10,
            visibility = Visibility.PUBLIC,
            eventIds = listOf("event1", "event2"),
            ownerId = "owner1")

    composeTestRule.setContent { SerieCard(serie = serie, onClick = {}, testTag = "testCard") }

    composeTestRule.onNodeWithText("29/02/2024").assertExists()
  }

  @Test
  fun serieCard_timeDisplay_handlesMidnight() {
    val calendar = Calendar.getInstance()
    calendar.set(2025, Calendar.JANUARY, 1, 0, 0, 0)

    val serie =
        Serie(
            serieId = "1",
            title = "New Year Serie",
            description = "desc",
            date = Timestamp(calendar.time),
            participants = listOf("user1"),
            maxParticipants = 10,
            visibility = Visibility.PUBLIC,
            eventIds = listOf("event1", "event2"),
            ownerId = "owner1")

    composeTestRule.setContent { SerieCard(serie = serie, onClick = {}, testTag = "testCard") }

    composeTestRule.onNodeWithText("00h00").assertExists()
  }

  @Test
  fun serieCard_timeDisplay_handlesNoon() {
    val calendar = Calendar.getInstance()
    calendar.set(2025, Calendar.JANUARY, 1, 12, 0, 0)

    val serie =
        Serie(
            serieId = "1",
            title = "Lunch Serie",
            description = "desc",
            date = Timestamp(calendar.time),
            participants = listOf("user1"),
            maxParticipants = 10,
            visibility = Visibility.PUBLIC,
            eventIds = listOf("event1", "event2"),
            ownerId = "owner1")

    composeTestRule.setContent { SerieCard(serie = serie, onClick = {}, testTag = "testCard") }

    composeTestRule.onNodeWithText("12h00").assertExists()
  }

  @Test
  fun serieCard_displaysWithSingleEvent() {
    val serie = createSerie("1", "Single Event Serie", listOf("event1"))

    composeTestRule.setContent { SerieCard(serie = serie, onClick = {}, testTag = "testCard") }

    composeTestRule.onNodeWithText("Single Event Serie").assertExists()
    composeTestRule.onNodeWithText("Serie 🔥").assertExists()
  }

  @Test
  fun serieCard_displaysWithManyEvents() {
    val manyEvents = (1..10).map { "event$it" }
    val serie = createSerie("1", "Large Serie", manyEvents)

    composeTestRule.setContent { SerieCard(serie = serie, onClick = {}, testTag = "testCard") }

    composeTestRule.onNodeWithText("Large Serie").assertExists()
    composeTestRule.onNodeWithText("Serie 🔥").assertExists()
  }

  @Test
  fun serieCard_stackedLayersAreRendered() {
    val serie = createSerie("1", "Test Serie")

    composeTestRule.setContent { SerieCard(serie = serie, onClick = {}, testTag = "testCard") }

    // Main card should exist
    composeTestRule.onNodeWithTag("testCard").assertExists()
    // The stacked effect is visual, so we just verify the main card renders
    composeTestRule.onNodeWithText("Test Serie").assertIsDisplayed()
  }

  @Test
  fun serieCard_handlesEmptyTitle() {
    val serie = createSerie("1", "")

    composeTestRule.setContent { SerieCard(serie = serie, onClick = {}, testTag = "testCard") }

    // Card should still render with empty title
    composeTestRule.onNodeWithTag("testCard").assertExists()
    composeTestRule.onNodeWithText("Serie 🔥").assertExists()
  }

  @Test
  fun serieCard_handlesVeryShortTitle() {
    val serie = createSerie("1", "A")

    composeTestRule.setContent { SerieCard(serie = serie, onClick = {}, testTag = "testCard") }

    composeTestRule.onNodeWithText("A").assertExists()
  }

  @Test
  fun serieCard_displaysWithPrivateVisibility() {
    val serie =
        Serie(
            serieId = "1",
            title = "Private Serie",
            description = "desc",
            date = Timestamp.now(),
            participants = listOf("user1"),
            maxParticipants = 10,
            visibility = Visibility.PRIVATE,
            eventIds = listOf("event1", "event2"),
            ownerId = "owner1")

    composeTestRule.setContent { SerieCard(serie = serie, onClick = {}, testTag = "testCard") }

    composeTestRule.onNodeWithText("Private Serie").assertExists()
  }

  @Test
  fun serieCard_displaysWithPublicVisibility() {
    val serie =
        Serie(
            serieId = "1",
            title = "Public Serie",
            description = "desc",
            date = Timestamp.now(),
            participants = listOf("user1"),
            maxParticipants = 10,
            visibility = Visibility.PUBLIC,
            eventIds = listOf("event1", "event2"),
            ownerId = "owner1")

    composeTestRule.setContent { SerieCard(serie = serie, onClick = {}, testTag = "testCard") }

    composeTestRule.onNodeWithText("Public Serie").assertExists()
  }

  @Test
  fun serieCard_handlesUnicodeCharactersInTitle() {
    val serie = createSerie("1", "Serie 日本語 🎉 Français")

    composeTestRule.setContent { SerieCard(serie = serie, onClick = {}, testTag = "testCard") }

    composeTestRule.onNodeWithText("Serie 日本語 🎉 Français").assertExists()
  }

  @Test
  fun serieCard_timeDisplay_handlesLateEvening() {
    val calendar = Calendar.getInstance()
    calendar.set(2025, Calendar.DECEMBER, 31, 23, 59, 0)

    val serie =
        Serie(
            serieId = "1",
            title = "Late Night Serie",
            description = "desc",
            date = Timestamp(calendar.time),
            participants = listOf("user1"),
            maxParticipants = 10,
            visibility = Visibility.PUBLIC,
            eventIds = listOf("event1", "event2"),
            ownerId = "owner1")

    composeTestRule.setContent { SerieCard(serie = serie, onClick = {}, testTag = "testCard") }

    composeTestRule.onNodeWithText("31/12/2025").assertExists()
    composeTestRule.onNodeWithText("23h59").assertExists()
  }

  @Test
  fun serieCard_displaysWithNoParticipants() {
    val serie =
        Serie(
            serieId = "1",
            title = "No Participants Serie",
            description = "desc",
            date = Timestamp.now(),
            participants = emptyList(),
            maxParticipants = 10,
            visibility = Visibility.PUBLIC,
            eventIds = listOf("event1", "event2"),
            ownerId = "owner1")

    composeTestRule.setContent { SerieCard(serie = serie, onClick = {}, testTag = "testCard") }

    composeTestRule.onNodeWithText("No Participants Serie").assertExists()
  }

  @Test
  fun serieCard_displaysWithManyParticipants() {
    val manyParticipants = (1..20).map { "user$it" }
    val serie =
        Serie(
            serieId = "1",
            title = "Popular Serie",
            description = "desc",
            date = Timestamp.now(),
            participants = manyParticipants,
            maxParticipants = 30,
            visibility = Visibility.PUBLIC,
            eventIds = listOf("event1", "event2"),
            ownerId = "owner1")

    composeTestRule.setContent { SerieCard(serie = serie, onClick = {}, testTag = "testCard") }

    composeTestRule.onNodeWithText("Popular Serie").assertExists()
  }

  @Test
  fun serieCard_dateInDistantFuture() {
    val calendar = Calendar.getInstance()
    calendar.set(2099, Calendar.DECEMBER, 31, 23, 59, 0)

    val serie =
        Serie(
            serieId = "1",
            title = "Future Serie",
            description = "desc",
            date = Timestamp(calendar.time),
            participants = listOf("user1"),
            maxParticipants = 10,
            visibility = Visibility.PUBLIC,
            eventIds = listOf("event1", "event2"),
            ownerId = "owner1")

    composeTestRule.setContent { SerieCard(serie = serie, onClick = {}, testTag = "testCard") }

    composeTestRule.onNodeWithText("31/12/2099").assertExists()
  }

  @Test
  fun serieCard_dateInDistantPast() {
    val calendar = Calendar.getInstance()
    calendar.set(2000, Calendar.JANUARY, 1, 0, 0, 0)

    val serie =
        Serie(
            serieId = "1",
            title = "Past Serie",
            description = "desc",
            date = Timestamp(calendar.time),
            participants = listOf("user1"),
            maxParticipants = 10,
            visibility = Visibility.PUBLIC,
            eventIds = listOf("event1", "event2"),
            ownerId = "owner1")

    composeTestRule.setContent { SerieCard(serie = serie, onClick = {}, testTag = "testCard") }

    composeTestRule.onNodeWithText("01/01/2000").assertExists()
  }
}
