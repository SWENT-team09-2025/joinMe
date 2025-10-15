package com.android.joinme.ui.overview

import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel

object CreateEventScreenTestTags {
  const val INPUT_EVENT_TYPE = "inputEventType"
  const val INPUT_EVENT_TITLE = "inputEventTitle"
  const val INPUT_EVENT_DESCRIPTION = "inputEventDescription"
  const val INPUT_EVENT_LOCATION = "inputEventLocation"
  const val INPUT_EVENT_MAX_PARTICIPANTS = "inputEventMaxParticipants"
  const val INPUT_EVENT_DURATION = "inputEventDuration"
  const val INPUT_EVENT_DATE = "inputEventDate"
  const val INPUT_EVENT_VISIBILITY = "inputEventVisibility"
  const val BUTTON_SAVE_EVENT = "buttonSaveEvent"
  const val ERROR_MESSAGE = "errorMessage"
}

@Composable
fun CreateEventScreen(
    createEventViewModel: CreateEventViewModel = viewModel(),
    onGoBack: () -> Unit = {},
    onDone: () -> Unit = {}
) {
  val uiState by createEventViewModel.uiState.collectAsState()
  val errorMsg = uiState.errorMsg
  val context = LocalContext.current

  LaunchedEffect(errorMsg) {
    if (errorMsg != null) {
      Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
      createEventViewModel.clearErrorMsg()
    }
  }

  val testTags =
      EventFormTestTags(
          inputEventType = CreateEventScreenTestTags.INPUT_EVENT_TYPE,
          inputEventTitle = CreateEventScreenTestTags.INPUT_EVENT_TITLE,
          inputEventDescription = CreateEventScreenTestTags.INPUT_EVENT_DESCRIPTION,
          inputEventLocation = CreateEventScreenTestTags.INPUT_EVENT_LOCATION,
          inputEventMaxParticipants = CreateEventScreenTestTags.INPUT_EVENT_MAX_PARTICIPANTS,
          inputEventDuration = CreateEventScreenTestTags.INPUT_EVENT_DURATION,
          inputEventDate = CreateEventScreenTestTags.INPUT_EVENT_DATE,
          inputEventVisibility = CreateEventScreenTestTags.INPUT_EVENT_VISIBILITY,
          buttonSaveEvent = CreateEventScreenTestTags.BUTTON_SAVE_EVENT,
          errorMessage = CreateEventScreenTestTags.ERROR_MESSAGE)

  val formState =
      EventFormState(
          type = uiState.type,
          title = uiState.title,
          description = uiState.description,
          location = uiState.location,
          maxParticipants = uiState.maxParticipants,
          duration = uiState.duration,
          date = uiState.date,
          time = uiState.time,
          visibility = uiState.visibility,
          isValid = uiState.isValid,
          invalidTitleMsg = uiState.invalidTitleMsg,
          invalidDescriptionMsg = uiState.invalidDescriptionMsg,
          invalidLocationMsg = uiState.invalidLocationMsg)

  EventFormScreen(
      title = "Create Event",
      formState = formState,
      testTags = testTags,
      onTypeChange = { createEventViewModel.setType(it) },
      onTitleChange = { createEventViewModel.setTitle(it) },
      onDescriptionChange = { createEventViewModel.setDescription(it) },
      onLocationChange = { createEventViewModel.setLocation(it) },
      onMaxParticipantsChange = { createEventViewModel.setMaxParticipants(it) },
      onDurationChange = { createEventViewModel.setDuration(it) },
      onDateChange = { createEventViewModel.setDate(it) },
      onTimeChange = { createEventViewModel.setTime(it) },
      onVisibilityChange = { createEventViewModel.setVisibility(it) },
      onSave = {
        if (createEventViewModel.createEvent()) {
          onDone()
          true
        } else {
          false
        }
      },
      onGoBack = onGoBack,
      saveButtonText = "Save")
}
