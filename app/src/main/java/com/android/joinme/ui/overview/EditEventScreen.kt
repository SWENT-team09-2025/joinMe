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
  const val INPUT_EVENT_MAX_PARTICIPANTS = "inputEventMaxParticipants"
  const val INPUT_EVENT_DURATION = "inputEventDuration"
  const val INPUT_EVENT_DATE = "inputEventDate"
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
          inputEventMaxParticipants = EditEventScreenTestTags.INPUT_EVENT_MAX_PARTICIPANTS,
          inputEventDuration = EditEventScreenTestTags.INPUT_EVENT_DURATION,
          inputEventDate = EditEventScreenTestTags.INPUT_EVENT_DATE,
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
          isValid = eventUIState.isValid,
          invalidTitleMsg = eventUIState.invalidTitleMsg,
          invalidDescriptionMsg = eventUIState.invalidDescriptionMsg,
          invalidLocationMsg = eventUIState.invalidLocationMsg)

  EventFormScreen(
      title = "Edit Event",
      formState = formState,
      testTags = testTags,
      onTypeChange = { editEventViewModel.setType(it) },
      onTitleChange = { editEventViewModel.setTitle(it) },
      onDescriptionChange = { editEventViewModel.setDescription(it) },
      onLocationChange = { editEventViewModel.setLocation(it) },
      onMaxParticipantsChange = { editEventViewModel.setMaxParticipants(it) },
      onDurationChange = { editEventViewModel.setDuration(it) },
      onDateChange = { editEventViewModel.setDate(it) },
      onTimeChange = { editEventViewModel.setTime(it) },
      onVisibilityChange = { editEventViewModel.setVisibility(it) },
      onSave = {
        var result = false
        coroutineScope.launch {
          result = editEventViewModel.editEvent(eventId)
          if (result) {
            onDone()
          }
        }
        result
      },
      onGoBack = onGoBack,
      saveButtonText = "Save")
}
