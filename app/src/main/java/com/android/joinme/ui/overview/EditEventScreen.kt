package com.android.joinme.ui.overview

import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

object EditEventScreenTestTags {
  const val INPUT_EVENT_TYPE = "inputEventType"
  const val INPUT_EVENT_TITLE = "inputEventTitle"
  const val INPUT_EVENT_DESCRIPTION = "inputEventDescription"
  const val INPUT_EVENT_LOCATION = "inputEventLocation"
  const val INPUT_EVENT_LOCATION_SUGGESTIONS = "inputEventLocationSuggestions"
  const val FOR_EACH_INPUT_EVENT_LOCATION_SUGGESTION = "inputEventLocationSuggestions"
  const val INPUT_EVENT_MAX_PARTICIPANTS = "inputEventMaxParticipants"
  const val INPUT_EVENT_DURATION = "inputEventDuration"
  const val INPUT_EVENT_DATE = "inputEventDate"
  const val INPUT_EVENT_TIME = "inputEventTime"
  const val INPUT_EVENT_VISIBILITY = "inputEventVisibility"
  const val EVENT_SAVE = "eventSave"
  const val ERROR_MESSAGE = "errorMessage"
}

@Composable
fun EditEventScreen(
    eventId: String,
    editEventViewModel: EditEventViewModel = viewModel(),
    onDone: () -> Unit = {},
    onGoBack: () -> Unit = {}
) {
  LaunchedEffect(eventId) { editEventViewModel.loadEvent(eventId) }

  val eventUIState by editEventViewModel.uiState.collectAsState()
  val errorMsg = eventUIState.errorMsg

  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()

  LaunchedEffect(errorMsg) {
    if (errorMsg != null) {
      Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
      editEventViewModel.clearErrorMsg()
    }
  }

  val testTags =
      EventFormTestTags(
          inputEventType = EditEventScreenTestTags.INPUT_EVENT_TYPE,
          inputEventTitle = EditEventScreenTestTags.INPUT_EVENT_TITLE,
          inputEventDescription = EditEventScreenTestTags.INPUT_EVENT_DESCRIPTION,
          inputEventLocation = EditEventScreenTestTags.INPUT_EVENT_LOCATION,
          inputEventLocationSuggestions = EditEventScreenTestTags.INPUT_EVENT_LOCATION_SUGGESTIONS,
          forEachInputEventLocationSuggestion =
              EditEventScreenTestTags.FOR_EACH_INPUT_EVENT_LOCATION_SUGGESTION,
          inputEventMaxParticipants = EditEventScreenTestTags.INPUT_EVENT_MAX_PARTICIPANTS,
          inputEventDuration = EditEventScreenTestTags.INPUT_EVENT_DURATION,
          inputEventDate = EditEventScreenTestTags.INPUT_EVENT_DATE,
          inputEventTime = EditEventScreenTestTags.INPUT_EVENT_TIME,
          inputEventVisibility = EditEventScreenTestTags.INPUT_EVENT_VISIBILITY,
          buttonSaveEvent = EditEventScreenTestTags.EVENT_SAVE,
          errorMessage = EditEventScreenTestTags.ERROR_MESSAGE)

  val formState =
      EventFormState(
          type = eventUIState.type,
          title = eventUIState.title,
          description = eventUIState.description,
          location = eventUIState.location,
          maxParticipants = eventUIState.maxParticipants,
          duration = eventUIState.duration,
          date = eventUIState.date,
          time = eventUIState.time,
          visibility = eventUIState.visibility,
          locationQuery = eventUIState.locationQuery,
          selectedLocation = eventUIState.selectedLocation,
          locationSuggestions = eventUIState.locationSuggestions,
          isValid = eventUIState.isValid,
          invalidTypeMsg = eventUIState.invalidTypeMsg,
          invalidTitleMsg = eventUIState.invalidTitleMsg,
          invalidDescriptionMsg = eventUIState.invalidDescriptionMsg,
          invalidLocationMsg = eventUIState.invalidLocationMsg,
          invalidMaxParticipantsMsg = eventUIState.invalidMaxParticipantsMsg,
          invalidDurationMsg = eventUIState.invalidDurationMsg,
          invalidDateMsg = eventUIState.invalidDateMsg,
          invalidVisibilityMsg = eventUIState.invalidVisibilityMsg)

  EventFormScreen(
      title = "Edit Event",
      formState = formState,
      testTags = testTags,
      isGroupEvent =
          eventUIState.isGroupEvent, // Hide type, maxParticipants, visibility for group events
      onTypeChange = { editEventViewModel.setType(it) },
      onTitleChange = { editEventViewModel.setTitle(it) },
      onDescriptionChange = { editEventViewModel.setDescription(it) },
      onLocationQueryChange = { editEventViewModel.setLocationQuery(it) },
      onSelectLocationChange = { editEventViewModel.selectLocation(it) },
      onMaxParticipantsChange = { editEventViewModel.setMaxParticipants(it) },
      onDurationChange = { editEventViewModel.setDuration(it) },
      onDateChange = { editEventViewModel.setDate(it) },
      onTimeChange = { editEventViewModel.setTime(it) },
      onVisibilityChange = { editEventViewModel.setVisibility(it) },
      onSave = {
        coroutineScope.launch {
          if (editEventViewModel.editEvent(eventId)) {
            onDone()
          }
        }
        true
      },
      onGoBack = onGoBack)
}
