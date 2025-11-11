package com.android.joinme.ui.overview

import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel

/** Note: this file was co-written using the help of AI (Claude). */

/** Test tags for EditEventForSerie screen UI elements. */
object EditEventForSerieScreenTestTags {
  const val INPUT_EVENT_TYPE = "inputEventType"
  const val INPUT_EVENT_TITLE = "inputEventTitle"
  const val INPUT_EVENT_DESCRIPTION = "inputEventDescription"
  const val INPUT_EVENT_LOCATION = "inputEventLocation"
  const val INPUT_EVENT_LOCATION_SUGGESTIONS = "inputEventLocationSuggestions"
  const val INPUT_EVENT_DURATION = "inputEventDuration"
  const val BUTTON_SAVE_EVENT = "buttonSaveEvent"
  const val ERROR_MESSAGE = "errorMessage"
}

/**
 * Screen for editing an event within a serie.
 *
 * Allows editing event-specific fields (type, title, description, duration, location). When the
 * event's duration is changed, all subsequent events in the serie will have their dates
 * recalculated to maintain proper scheduling.
 *
 * @param serieId The ID of the serie containing this event
 * @param eventId The ID of the event to edit
 * @param editEventForSerieViewModel ViewModel for managing screen state
 * @param onGoBack Callback when back button is pressed
 * @param onDone Callback when event is successfully edited
 */
@Composable
fun EditEventForSerieScreen(
    serieId: String,
    eventId: String,
    editEventForSerieViewModel: EditEventForSerieViewModel = viewModel(),
    onGoBack: () -> Unit = {},
    onDone: () -> Unit = {}
) {
  val uiState by editEventForSerieViewModel.uiState.collectAsState()
  val errorMsg = uiState.errorMsg
  val context = LocalContext.current

  // Load the event data when the screen first appears
  LaunchedEffect(eventId) { editEventForSerieViewModel.loadEvent(eventId) }

  LaunchedEffect(errorMsg) {
    if (errorMsg != null) {
      Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
      editEventForSerieViewModel.clearErrorMsg()
    }
  }

  val testTags =
      EventForSerieFormTestTags(
          inputEventType = EditEventForSerieScreenTestTags.INPUT_EVENT_TYPE,
          inputEventTitle = EditEventForSerieScreenTestTags.INPUT_EVENT_TITLE,
          inputEventDescription = EditEventForSerieScreenTestTags.INPUT_EVENT_DESCRIPTION,
          inputEventLocation = EditEventForSerieScreenTestTags.INPUT_EVENT_LOCATION,
          inputEventLocationSuggestions =
              EditEventForSerieScreenTestTags.INPUT_EVENT_LOCATION_SUGGESTIONS,
          inputEventDuration = EditEventForSerieScreenTestTags.INPUT_EVENT_DURATION,
          buttonSaveEvent = EditEventForSerieScreenTestTags.BUTTON_SAVE_EVENT,
          errorMessage = EditEventForSerieScreenTestTags.ERROR_MESSAGE)

  EventForSerieFormScreen(
      title = "Edit Event for Serie",
      formState = uiState,
      isFormValid = uiState.isValid,
      testTags = testTags,
      onTypeChange = { editEventForSerieViewModel.setType(it) },
      onTitleChange = { editEventForSerieViewModel.setTitle(it) },
      onDescriptionChange = { editEventForSerieViewModel.setDescription(it) },
      onDurationChange = { editEventForSerieViewModel.setDuration(it) },
      onLocationQueryChange = { editEventForSerieViewModel.setLocationQuery(it) },
      onLocationSelected = { editEventForSerieViewModel.selectLocation(it) },
      onSearchLocations = { editEventForSerieViewModel.searchLocations(it) },
      onSave = {
        val success = editEventForSerieViewModel.editEventForSerie(serieId, eventId)
        if (success) {
          onDone()
        }
        success
      },
      onGoBack = onGoBack,
      saveButtonText = "SAVE CHANGES")
}
