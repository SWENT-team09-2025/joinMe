package com.android.joinme.ui.overview

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.joinme.ui.theme.Dimens
import com.android.joinme.ui.theme.customColors
import kotlinx.coroutines.launch

object CreateEventScreenTestTags {
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
  const val INPUT_EVENT_GROUP = "inputEventGroup"
  const val BUTTON_SAVE_EVENT = "buttonSaveEvent"
  const val ERROR_MESSAGE = "errorMessage"
}

private const val STANDALONE_EVENT_LABEL = "Standalone Event"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroupSelectionDropdown(
    selectedGroupId: String?,
    availableGroups: List<com.android.joinme.model.groups.Group>,
    onGroupSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
  var showGroupDropdown by remember { mutableStateOf(false) }

  ExposedDropdownMenuBox(
      expanded = showGroupDropdown, onExpandedChange = { showGroupDropdown = !showGroupDropdown }) {
        OutlinedTextField(
            value =
                when {
                  selectedGroupId == null -> STANDALONE_EVENT_LABEL
                  else ->
                      availableGroups.find { it.id == selectedGroupId }?.name
                          ?: STANDALONE_EVENT_LABEL
                },
            onValueChange = {},
            readOnly = true,
            label = { Text("Group") },
            placeholder = { Text("Select group or standalone") },
            trailingIcon = {
              ExposedDropdownMenuDefaults.TrailingIcon(expanded = showGroupDropdown)
            },
            modifier =
                modifier
                    .menuAnchor()
                    .fillMaxWidth()
                    .testTag(CreateEventScreenTestTags.INPUT_EVENT_GROUP))
        ExposedDropdownMenu(
            expanded = showGroupDropdown,
            onDismissRequest = { showGroupDropdown = false },
            modifier = Modifier.background(MaterialTheme.customColors.backgroundMenu)) {
              // Standalone option
              DropdownMenuItem(
                  text = {
                    Text(
                        text = STANDALONE_EVENT_LABEL,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.headlineSmall)
                  },
                  onClick = {
                    onGroupSelected(null)
                    showGroupDropdown = false
                  },
                  colors = MaterialTheme.customColors.dropdownMenu)

              if (availableGroups.isNotEmpty()) {
                HorizontalDivider(thickness = Dimens.BorderWidth.thin)
              }

              // Group options
              availableGroups.forEachIndexed { index, group ->
                DropdownMenuItem(
                    text = {
                      Text(
                          text = group.name,
                          color = MaterialTheme.colorScheme.onPrimaryContainer,
                          style = MaterialTheme.typography.headlineSmall)
                    },
                    onClick = {
                      onGroupSelected(group.id)
                      showGroupDropdown = false
                    },
                    colors = MaterialTheme.customColors.dropdownMenu)
                if (index < availableGroups.lastIndex) {
                  HorizontalDivider(thickness = Dimens.BorderWidth.thin)
                }
              }
            }
      }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEventScreen(
    createEventViewModel: CreateEventViewModel = viewModel(),
    onGoBack: () -> Unit = {},
    onDone: () -> Unit = {}
) {
  val uiState by createEventViewModel.uiState.collectAsState()
  val errorMsg = uiState.errorMsg
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()

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
          inputEventLocationSuggestions =
              CreateEventScreenTestTags.INPUT_EVENT_LOCATION_SUGGESTIONS,
          forEachInputEventLocationSuggestion =
              CreateEventScreenTestTags.FOR_EACH_INPUT_EVENT_LOCATION_SUGGESTION,
          inputEventMaxParticipants = CreateEventScreenTestTags.INPUT_EVENT_MAX_PARTICIPANTS,
          inputEventDuration = CreateEventScreenTestTags.INPUT_EVENT_DURATION,
          inputEventDate = CreateEventScreenTestTags.INPUT_EVENT_DATE,
          inputEventTime = CreateEventScreenTestTags.INPUT_EVENT_TIME,
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
          locationQuery = uiState.locationQuery,
          selectedLocation = uiState.selectedLocation,
          locationSuggestions = uiState.locationSuggestions,
          isValid = uiState.isValid,
          invalidTypeMsg = uiState.invalidTypeMsg,
          invalidTitleMsg = uiState.invalidTitleMsg,
          invalidDescriptionMsg = uiState.invalidDescriptionMsg,
          invalidLocationMsg = uiState.invalidLocationMsg,
          invalidMaxParticipantsMsg = uiState.invalidMaxParticipantsMsg,
          invalidDurationMsg = uiState.invalidDurationMsg,
          invalidDateMsg = uiState.invalidDateMsg,
          invalidVisibilityMsg = uiState.invalidVisibilityMsg)

  EventFormScreen(
      title = "Create Event",
      formState = formState,
      testTags = testTags,
      onTypeChange = { createEventViewModel.setType(it) },
      onTitleChange = { createEventViewModel.setTitle(it) },
      onDescriptionChange = { createEventViewModel.setDescription(it) },
      onLocationQueryChange = { createEventViewModel.setLocationQuery(it) },
      onSelectLocationChange = { createEventViewModel.selectLocation(it) },
      onMaxParticipantsChange = { createEventViewModel.setMaxParticipants(it) },
      onDurationChange = { createEventViewModel.setDuration(it) },
      onDateChange = { createEventViewModel.setDate(it) },
      onTimeChange = { createEventViewModel.setTime(it) },
      onVisibilityChange = { createEventViewModel.setVisibility(it) },
      onSave = {
        coroutineScope.launch {
          if (createEventViewModel.createEvent()) {
            onDone()
          }
        }
        true
      },
      onGoBack = onGoBack,
      extraContent = {
        GroupSelectionDropdown(
            selectedGroupId = uiState.selectedGroupId,
            availableGroups = uiState.availableGroups,
            onGroupSelected = { createEventViewModel.setSelectedGroup(it) })
      },
      isGroupEvent = uiState.selectedGroupId != null)
}
