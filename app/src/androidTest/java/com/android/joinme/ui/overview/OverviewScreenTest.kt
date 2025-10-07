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

    composeTestRule.onNodeWithTag(OverviewScreenTestTags.EMPTY_EVENT_LIST_MSG).assertExists()
  }

  @Test
  fun overviewScreen_showsList_whenEventsExist() {
    runBlocking {
      val repo = EventsRepositoryLocal()
      val viewModel = OverviewViewModel(repo)

      // Add some events into the local repository
      repo.addEvent(createEvent("1", "Basketball", EventType.SPORTS))
      repo.addEvent(createEvent("2", "Bar", EventType.SOCIAL))

      composeTestRule.setContent { OverviewScreen(overviewViewModel = viewModel) }

      composeTestRule.waitForIdle()

      composeTestRule.onNodeWithTag(OverviewScreenTestTags.EVENT_LIST).assertExists()

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
  }

  @Test
  fun clickingFab_triggersOnAddEvent() {
    val repo = EventsRepositoryLocal()
    val viewModel = OverviewViewModel(repo)
    var clicked = false

    composeTestRule.setContent {
      OverviewScreen(overviewViewModel = viewModel, onAddEvent = { clicked = true })
    }

    composeTestRule.onNodeWithTag(OverviewScreenTestTags.CREATE_EVENT_BUTTON).performClick()

    assert(clicked)
  }

  @Test
  fun clickingEvent_triggersOnSelectEvent() = runBlocking {
    val repo = EventsRepositoryLocal()
    val viewModel = OverviewViewModel(repo)

    val event = createEvent("1", "Basketball", EventType.SPORTS)
    repo.addEvent(event)

    var selected: Event? = null

    composeTestRule.setContent {
      OverviewScreen(overviewViewModel = viewModel, onSelectEvent = { selected = it })
    }

    composeTestRule
        .onNodeWithTag(OverviewScreenTestTags.getTestTagForEventItem(event))
        .performClick()

    assert(selected == event)
  }
}
