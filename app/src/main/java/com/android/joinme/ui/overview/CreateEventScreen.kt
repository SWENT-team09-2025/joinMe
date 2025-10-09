package com.android.joinme.ui.overview

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.joinme.model.event.EventType
import com.android.joinme.model.event.EventVisibility

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

  LaunchedEffect(errorMsg) {
    if (errorMsg != null) {
      Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
      createEventViewModel.clearErrorMsg()
    }
  }

  val eventTypes = listOf(EventType.SPORTS.name, EventType.ACTIVITY.name, EventType.SOCIAL.name)
  val visibilities = listOf(EventVisibility.PUBLIC.name, EventVisibility.PRIVATE.name)

  var showTypeDropdown by remember { mutableStateOf(false) }
  var showVisibilityDropdown by remember { mutableStateOf(false) }

  Scaffold(
      topBar = {
        TopAppBar(
            title = { Text("Create Event") },
            navigationIcon = {
              IconButton(onClick = { onGoBack() }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back")
              }
            })
      }) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp).padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
              // Type dropdown
              ExposedDropdownMenuBox(
                  expanded = showTypeDropdown,
                  onExpandedChange = { showTypeDropdown = !showTypeDropdown }) {
                    OutlinedTextField(
                        value = uiState.type,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Event Type") },
                        placeholder = { Text("Select event type") },
                        trailingIcon = {
                          ExposedDropdownMenuDefaults.TrailingIcon(expanded = showTypeDropdown)
                        },
                        modifier =
                            Modifier.menuAnchor()
                                .fillMaxWidth()
                                .testTag(CreateEventScreenTestTags.INPUT_EVENT_TYPE))
                    ExposedDropdownMenu(
                        expanded = showTypeDropdown,
                        onDismissRequest = { showTypeDropdown = false }) {
                          eventTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                  createEventViewModel.setType(type)
                                  showTypeDropdown = false
                                })
                          }
                        }
                  }

              // Title
              OutlinedTextField(
                  value = uiState.title,
                  onValueChange = { createEventViewModel.setTitle(it) },
                  label = { Text("Title") },
                  placeholder = { Text("Enter event title") },
                  isError = uiState.invalidTitleMsg != null,
                  supportingText = {
                    uiState.invalidTitleMsg?.let {
                      Text(
                          it,
                          color = MaterialTheme.colorScheme.error,
                          modifier = Modifier.testTag(CreateEventScreenTestTags.ERROR_MESSAGE))
                    }
                  },
                  modifier =
                      Modifier.fillMaxWidth().testTag(CreateEventScreenTestTags.INPUT_EVENT_TITLE))

              // Description
              OutlinedTextField(
                  value = uiState.description,
                  onValueChange = { createEventViewModel.setDescription(it) },
                  label = { Text("Description") },
                  placeholder = { Text("Describe your event") },
                  isError = uiState.invalidDescriptionMsg != null,
                  supportingText = {
                    uiState.invalidDescriptionMsg?.let {
                      Text(
                          it,
                          color = MaterialTheme.colorScheme.error,
                          modifier = Modifier.testTag(CreateEventScreenTestTags.ERROR_MESSAGE))
                    }
                  },
                  modifier =
                      Modifier.fillMaxWidth()
                          .height(150.dp)
                          .testTag(CreateEventScreenTestTags.INPUT_EVENT_DESCRIPTION))

              // Location
              OutlinedTextField(
                  value = uiState.location,
                  onValueChange = { createEventViewModel.setLocation(it) },
                  label = { Text("Location") },
                  placeholder = { Text("Enter location name") },
                  isError = uiState.invalidLocationMsg != null,
                  supportingText = {
                    uiState.invalidLocationMsg?.let {
                      Text(
                          it,
                          color = MaterialTheme.colorScheme.error,
                          modifier = Modifier.testTag(CreateEventScreenTestTags.ERROR_MESSAGE))
                    }
                  },
                  modifier =
                      Modifier.fillMaxWidth()
                          .testTag(CreateEventScreenTestTags.INPUT_EVENT_LOCATION))

              Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Max Participants
                    OutlinedTextField(
                        value = uiState.maxParticipants,
                        onValueChange = { createEventViewModel.setMaxParticipants(it) },
                        label = { Text("Max Participants") },
                        placeholder = { Text("e.g. 10") },
                        isError = uiState.invalidMaxParticipantsMsg != null,
                        supportingText = {
                          uiState.invalidMaxParticipantsMsg?.let {
                            Text(
                                it,
                                color = MaterialTheme.colorScheme.error,
                                modifier =
                                    Modifier.testTag(CreateEventScreenTestTags.ERROR_MESSAGE))
                          }
                        },
                        modifier =
                            Modifier.weight(1f)
                                .testTag(CreateEventScreenTestTags.INPUT_EVENT_MAX_PARTICIPANTS))

                    // Duration
                    OutlinedTextField(
                        value = uiState.duration,
                        onValueChange = { createEventViewModel.setDuration(it) },
                        label = { Text("Duration (minutes)") },
                        placeholder = { Text("e.g. 60") },
                        isError = uiState.invalidDurationMsg != null,
                        supportingText = {
                          uiState.invalidDurationMsg?.let {
                            Text(
                                it,
                                color = MaterialTheme.colorScheme.error,
                                modifier =
                                    Modifier.testTag(CreateEventScreenTestTags.ERROR_MESSAGE))
                          }
                        },
                        modifier =
                            Modifier.weight(1f)
                                .testTag(CreateEventScreenTestTags.INPUT_EVENT_DURATION))
                  }

              // Date + Time
              OutlinedTextField(
                  value = uiState.date,
                  onValueChange = { createEventViewModel.setDate(it) },
                  label = { Text("Date & Time") },
                  placeholder = { Text("dd/MM/yyyy HH:mm") },
                  isError = uiState.invalidDateMsg != null,
                  supportingText = {
                    uiState.invalidDateMsg?.let {
                      Text(
                          it,
                          color = MaterialTheme.colorScheme.error,
                          modifier = Modifier.testTag(CreateEventScreenTestTags.ERROR_MESSAGE))
                    }
                  },
                  modifier =
                      Modifier.fillMaxWidth().testTag(CreateEventScreenTestTags.INPUT_EVENT_DATE))

              // Visibility dropdown
              ExposedDropdownMenuBox(
                  expanded = showVisibilityDropdown,
                  onExpandedChange = { showVisibilityDropdown = !showVisibilityDropdown }) {
                    OutlinedTextField(
                        value = uiState.visibility,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Event Visibility") },
                        placeholder = { Text("Select visibility") },
                        trailingIcon = {
                          ExposedDropdownMenuDefaults.TrailingIcon(
                              expanded = showVisibilityDropdown)
                        },
                        modifier =
                            Modifier.menuAnchor()
                                .fillMaxWidth()
                                .testTag(CreateEventScreenTestTags.INPUT_EVENT_VISIBILITY))
                    ExposedDropdownMenu(
                        expanded = showVisibilityDropdown,
                        onDismissRequest = { showVisibilityDropdown = false }) {
                          visibilities.forEach { vis ->
                            DropdownMenuItem(
                                text = { Text(vis) },
                                onClick = {
                                  createEventViewModel.setVisibility(vis)
                                  showVisibilityDropdown = false
                                })
                          }
                        }
                  }

              Spacer(modifier = Modifier.height(16.dp))

              // Save button
              Button(
                  onClick = { if (createEventViewModel.createEvent()) onDone() },
                  modifier =
                      Modifier.fillMaxWidth().testTag(CreateEventScreenTestTags.BUTTON_SAVE_EVENT),
                  enabled = uiState.isValid) {
                    Text("Save")
                  }
            }
      }
}
