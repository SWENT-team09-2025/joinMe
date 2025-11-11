package com.android.joinme.ui.overview

import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel

/** Note: this file was refactored using IA (Claude). */

/** Test tags for CreateEventForSerie screen UI elements. */
object CreateEventForSerieScreenTestTags {
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
 * Screen for creating an event within an existing serie.
 *
 * Displays after serie creation to add the first event. Only requires event-specific fields (type,
 * title, description, duration, location). Inherits maxParticipants, visibility, and ownerId from
 * the parent serie. Event date is calculated automatically.
 *
 * @param serieId The ID of the serie to add this event to
 * @param createEventForSerieViewModel ViewModel for managing screen state
 * @param onGoBack Callback when back button is pressed
 * @param onDone Callback when event is successfully created
 */
@Composable
fun CreateEventForSerieScreen(
    serieId: String,
    createEventForSerieViewModel: CreateEventForSerieViewModel = viewModel(),
    onGoBack: () -> Unit = {},
    onDone: () -> Unit = {}
) {
  val uiState by createEventForSerieViewModel.uiState.collectAsState()
  val errorMsg = uiState.errorMsg
  val context = LocalContext.current

  LaunchedEffect(errorMsg) {
    if (errorMsg != null) {
      Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
      createEventForSerieViewModel.clearErrorMsg()
    }
  }

  val testTags =
      EventForSerieFormTestTags(
          inputEventType = CreateEventForSerieScreenTestTags.INPUT_EVENT_TYPE,
          inputEventTitle = CreateEventForSerieScreenTestTags.INPUT_EVENT_TITLE,
          inputEventDescription = CreateEventForSerieScreenTestTags.INPUT_EVENT_DESCRIPTION,
          inputEventLocation = CreateEventForSerieScreenTestTags.INPUT_EVENT_LOCATION,
          inputEventLocationSuggestions =
              CreateEventForSerieScreenTestTags.INPUT_EVENT_LOCATION_SUGGESTIONS,
          inputEventDuration = CreateEventForSerieScreenTestTags.INPUT_EVENT_DURATION,
          buttonSaveEvent = CreateEventForSerieScreenTestTags.BUTTON_SAVE_EVENT,
          errorMessage = CreateEventForSerieScreenTestTags.ERROR_MESSAGE)

  EventForSerieFormScreen(
      title = "Create Event for Serie",
      formState = uiState,
      isFormValid = uiState.isValid,
      testTags = testTags,
      onTypeChange = { createEventForSerieViewModel.setType(it) },
      onTitleChange = { createEventForSerieViewModel.setTitle(it) },
      onDescriptionChange = { createEventForSerieViewModel.setDescription(it) },
      onDurationChange = { createEventForSerieViewModel.setDuration(it) },
      onLocationQueryChange = { createEventForSerieViewModel.setLocationQuery(it) },
      onLocationSelected = { createEventForSerieViewModel.selectLocation(it) },
      onSearchLocations = { createEventForSerieViewModel.searchLocations(it) },
      onSave = {
        val success = createEventForSerieViewModel.createEventForSerie(serieId)
        if (success) {
          onDone()
        }
        success
      },
      onGoBack = onGoBack,
      saveButtonText = "CREATE EVENT")
}
