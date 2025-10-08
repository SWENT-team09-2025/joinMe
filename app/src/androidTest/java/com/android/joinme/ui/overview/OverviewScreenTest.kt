package com.android.joinme.ui.overview

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.android.joinme.model.event.*
import com.google.firebase.Timestamp
import java.util.*
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

class OverviewScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  private fun createEvent(id: String, title: String, type: EventType): Event {
    return Event(
        eventId = id,
        type = type,
        title = title,
        description = "desc",
        location = null,
        date = Timestamp(Date()),
        duration = 60,
        participants = emptyList(),
        maxParticipants = 5,
        visibility = EventVisibility.PUBLIC,
        ownerId = "owner")
  }

  @Test
  fun overviewScreen_showsEmptyMessage_whenNoEvents() {
    val repo = EventsRepositoryLocal()
    val viewModel = OverviewViewModel(repo)

    composeTestRule.setContent { OverviewScreen(overviewViewModel = viewModel) }

    // Attends que le LaunchedEffect se termine
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(1000)
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(OverviewScreenTestTags.EMPTY_EVENT_LIST_MSG).assertExists()
  }

  @Test
  fun overviewScreen_showsList_whenEventsExist() {
    val repo = EventsRepositoryLocal()
    val viewModel = OverviewViewModel(repo)

    // Ajoute les events AVANT de créer la UI
    runBlocking {
      repo.addEvent(createEvent("1", "Basketball", EventType.SPORTS))
      repo.addEvent(createEvent("2", "Bar", EventType.SOCIAL))
    }

    composeTestRule.setContent { OverviewScreen(overviewViewModel = viewModel) }

    // Attends que le LaunchedEffect charge les données
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Vérifie que la liste existe
    composeTestRule.onNodeWithTag(OverviewScreenTestTags.EVENT_LIST).assertExists()

    // Vérifie que les événements sont affichés
    composeTestRule
        .onNodeWithTag(
            OverviewScreenTestTags.getTestTagForEventItem(
                createEvent("1", "Basketball", EventType.SPORTS)))
        .assertExists()

    composeTestRule
        .onNodeWithTag(
            OverviewScreenTestTags.getTestTagForEventItem(
                createEvent("2", "Bar", EventType.SOCIAL)))
        .assertExists()
  }

  @Test
  fun clickingFab_triggersOnAddEvent() {
    val repo = EventsRepositoryLocal()
    val viewModel = OverviewViewModel(repo)
    var clicked = false

    composeTestRule.setContent {
      OverviewScreen(overviewViewModel = viewModel, onAddEvent = { clicked = true })
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(OverviewScreenTestTags.CREATE_EVENT_BUTTON).performClick()

    assert(clicked)
  }

  @Test
  fun clickingEvent_triggersOnSelectEvent() {
    val repo = EventsRepositoryLocal()
    val viewModel = OverviewViewModel(repo)

    val event = createEvent("1", "Basketball", EventType.SPORTS)

    // Ajoute l'event AVANT de créer la UI
    runBlocking { repo.addEvent(event) }

    var selected: Event? = null

    composeTestRule.setContent {
      OverviewScreen(overviewViewModel = viewModel, onSelectEvent = { selected = it })
    }

    // Attends que le LaunchedEffect charge les données
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Clique sur l'événement
    composeTestRule
        .onNodeWithTag(OverviewScreenTestTags.getTestTagForEventItem(event))
        .performClick()

    // Vérifie que l'événement a été sélectionné
    assert(selected == event)
  }

  @Test
  fun eventCard_displaysCorrectInformation() {
    val event = createEvent("1", "Basketball Match", EventType.SPORTS)

    composeTestRule.setContent { EventCard(event = event, onClick = {}) }

    composeTestRule.onNodeWithText("Basketball Match").assertExists()
    composeTestRule.onNodeWithText("Place : Unknown").assertExists()
  }

  @Test
  fun eventCard_clickTriggersCallback() {
    var clicked = false
    val event = createEvent("1", "Test Event", EventType.SPORTS)

    composeTestRule.setContent { EventCard(event = event, onClick = { clicked = true }) }

    composeTestRule.onNodeWithText("Test Event").performClick()

    assert(clicked)
  }
}
