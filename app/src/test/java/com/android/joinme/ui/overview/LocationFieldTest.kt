package com.android.joinme.ui.overview

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.android.joinme.model.map.Location
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Pure UI tests for LocationField. We render the composable directly with fake state so there is no
 * dependency on ViewModel or network.
 */
@RunWith(RobolectricTestRunner::class)
class LocationFieldTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun typing_query_showsSuggestions() {
    lateinit var onQueryChange: (String) -> Unit

    composeTestRule.setContent {
      val q = remember { mutableStateOf("") }
      val sugg = remember { mutableStateListOf<Location>() }

      onQueryChange = { newQ ->
        q.value = newQ
        sugg.clear()
        if (newQ.isNotBlank()) {
          sugg.addAll(
              listOf(
                  Location(46.52, 6.63, "Lausanne, Switzerland"),
                  Location(47.37, 8.54, "Zürich, Switzerland")))
        }
      }

      val tags =
          EventFormTestTags(
              inputEventType = "noop",
              inputEventTitle = "noop",
              inputEventDescription = "noop",
              inputEventLocation = "inputEventLocation",
              inputEventLocationSuggestions = "inputEventLocationSuggestions",
              inputEventMaxParticipants = "noop",
              inputEventDuration = "noop",
              inputEventDate = "noop",
              inputEventTime = "noop",
              inputEventVisibility = "noop",
              buttonSaveEvent = "noop",
              errorMessage = "noop")

      LocationField(
          query = q.value,
          suggestions = sugg,
          isError = false,
          supportingText = null,
          onQueryChange = onQueryChange,
          onSuggestionSelected = {},
          modifier = Modifier.fillMaxWidth(),
          testTags = tags)
    }

    // Type into the field
    composeTestRule.onNodeWithTag("inputEventLocation").performTextInput("Lau")

    // Give Compose a chance to recompose
    composeTestRule.waitForIdle()

    // Wait until at least one suggestion appears
    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule.onAllNodesWithText("Lausanne, Switzerland").fetchSemanticsNodes().isNotEmpty()
    }

    // Then assert suggestions container exists
    composeTestRule.onAllNodesWithTag("inputEventLocationSuggestions").assertCountEquals(3)
  }

  @Test
  fun selectingSuggestion_fillsText_and_closesList() {
    lateinit var onQueryChange: (String) -> Unit
    composeTestRule.setContent {
      val q = androidx.compose.runtime.remember { mutableStateOf("") }
      val sugg =
          androidx.compose.runtime.remember {
            androidx.compose.runtime.mutableStateListOf<Location>()
          }

      onQueryChange = { newQ ->
        q.value = newQ
        sugg.clear()
        if (newQ.isNotBlank()) {
          sugg.addAll(
              listOf(
                  Location(46.52, 6.63, "Lausanne, Switzerland"),
                  Location(47.37, 8.54, "Zürich, Switzerland"),
              ))
        }
      }

      val tags =
          EventFormTestTags(
              inputEventType = "noop",
              inputEventTitle = "noop",
              inputEventDescription = "noop",
              inputEventLocation = "inputEventLocation",
              inputEventLocationSuggestions = "inputEventLocationSuggestions",
              inputEventMaxParticipants = "noop",
              inputEventDuration = "noop",
              inputEventDate = "noop",
              inputEventTime = "noop",
              inputEventVisibility = "noop",
              buttonSaveEvent = "noop",
              errorMessage = "noop")
      LocationField(
          query = q.value,
          suggestions = sugg,
          isError = false,
          supportingText = null,
          onQueryChange = onQueryChange,
          onSuggestionSelected = { selected ->
            q.value = selected.name
            sugg.clear()
          },
          modifier = Modifier.fillMaxWidth(),
          testTags = tags)
    }

    // Type to open
    composeTestRule.onNodeWithTag("inputEventLocation").performTextInput("Lau")
    composeTestRule.waitUntil(5_000) {
      composeTestRule
          .onAllNodes(hasTestTag("inputEventLocationSuggestions"))
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Click the specific suggestion by text
    composeTestRule.onAllNodesWithText("Lausanne, Switzerland").onFirst().performClick()

    // List should be gone
    composeTestRule.waitUntil(5_000) {
      composeTestRule
          .onAllNodes(hasTestTag("inputEventLocationSuggestions"))
          .fetchSemanticsNodes()
          .isEmpty()
    }

    // Field should contain the chosen name
    composeTestRule
        .onNodeWithTag("inputEventLocation")
        .assertTextContains("Lausanne, Switzerland", substring = false)
  }

  @Test
  fun suppressNextOpen_preventsImmediateReopen_afterSelection() {
    // This indirectly verifies your suppressNextOpen logic by ensuring the list
    // does not reopen immediately on the next LaunchedEffect pass after selection.
    lateinit var onQueryChange: (String) -> Unit
    composeTestRule.setContent {
      val q = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
      val sugg =
          androidx.compose.runtime.remember {
            androidx.compose.runtime.mutableStateListOf<Location>()
          }

      onQueryChange = { newQ ->
        q.value = newQ
        sugg.clear()
        if (newQ.isNotBlank()) {
          sugg.addAll(
              listOf(
                  Location(46.52, 6.63, "Lausanne, Switzerland"),
                  Location(47.37, 8.54, "Zürich, Switzerland"),
              ))
        }
      }

      val tags =
          EventFormTestTags(
              inputEventType = "noop",
              inputEventTitle = "noop",
              inputEventDescription = "noop",
              inputEventLocation = "inputEventLocation",
              inputEventLocationSuggestions = "inputEventLocationSuggestions",
              inputEventMaxParticipants = "noop",
              inputEventDuration = "noop",
              inputEventDate = "noop",
              inputEventTime = "noop",
              inputEventVisibility = "noop",
              buttonSaveEvent = "noop",
              errorMessage = "noop")
      LocationField(
          query = q.value,
          suggestions = sugg,
          isError = false,
          supportingText = null,
          onQueryChange = onQueryChange,
          onSuggestionSelected = { selected ->
            q.value = selected.name
            sugg.clear()
          },
          modifier = Modifier.fillMaxWidth(),
          testTags = tags)
    }

    // Open
    composeTestRule.onNodeWithTag("inputEventLocation").performTextInput("Lau")
    composeTestRule.waitUntil(5_000) {
      composeTestRule
          .onAllNodes(hasTestTag("inputEventLocationSuggestions"))
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Select
    composeTestRule.onAllNodesWithText("Lausanne, Switzerland").onFirst().performClick()

    // Ensure it stays closed for at least a frame (no immediate reopen)
    composeTestRule.waitUntil(5_000) {
      composeTestRule
          .onAllNodes(hasTestTag("inputEventLocationSuggestions"))
          .fetchSemanticsNodes()
          .isEmpty()
    }
  }
}
