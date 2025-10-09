package com.android.joinme.ui.overview

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.joinme.model.event.EventType
import com.android.joinme.model.event.EventVisibility
import com.android.joinme.model.event.displayString

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

@OptIn(ExperimentalMaterial3Api::class)
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

  var showTypeMenu by remember { mutableStateOf(false) }
  var showVisibilityMenu by remember { mutableStateOf(false) }

  val context = LocalContext.current

  LaunchedEffect(errorMsg) {
    if (errorMsg != null) {
      Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
      editEventViewModel.clearErrorMsg()
    }
  }

  Scaffold(
      topBar = {
        TopAppBar(
            title = { Text(text = "Edit Event", fontSize = 20.sp, fontWeight = FontWeight.Medium) },
            navigationIcon = {
              IconButton(onClick = onGoBack) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
              }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White))
      }) { paddingValues ->
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {

              // Event Type Dropdown
              Box {
                OutlinedTextField(
                    value = eventUIState.type.ifBlank { "" },
                    onValueChange = {},
                    label = { Text("Event Type") },
                    placeholder = { Text("Select event type") },
                    isError = eventUIState.invalidTypeMsg != null,
                    supportingText = {
                      eventUIState.invalidTypeMsg?.let {
                        Text(it, modifier = Modifier.testTag(EditEventScreenTestTags.ERROR_MESSAGE))
                      }
                    },
                    modifier =
                        Modifier.fillMaxWidth().testTag(EditEventScreenTestTags.INPUT_EVENT_TYPE),
                    shape = RoundedCornerShape(8.dp),
                    readOnly = true,
                    trailingIcon = {
                      IconButton(onClick = { showTypeMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select event type")
                      }
                    })

                DropdownMenu(expanded = showTypeMenu, onDismissRequest = { showTypeMenu = false }) {
                  EventType.values().forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type.displayString()) },
                        onClick = {
                          editEventViewModel.setType(type.name)
                          showTypeMenu = false
                        })
                  }
                }
              }

              // Title Field
              OutlinedTextField(
                  value = eventUIState.title,
                  onValueChange = { editEventViewModel.setTitle(it) },
                  label = { Text("Title") },
                  placeholder = { Text("Task Title") },
                  isError = eventUIState.invalidTitleMsg != null,
                  supportingText = {
                    eventUIState.invalidTitleMsg?.let {
                      Text(it, modifier = Modifier.testTag(EditEventScreenTestTags.ERROR_MESSAGE))
                    }
                  },
                  modifier =
                      Modifier.fillMaxWidth().testTag(EditEventScreenTestTags.INPUT_EVENT_TITLE),
                  shape = RoundedCornerShape(8.dp))

              // Description Field
              OutlinedTextField(
                  value = eventUIState.description,
                  onValueChange = { editEventViewModel.setDescription(it) },
                  label = { Text("Description") },
                  placeholder = { Text("Describe the event") },
                  isError = eventUIState.invalidDescriptionMsg != null,
                  supportingText = {
                    eventUIState.invalidDescriptionMsg?.let {
                      Text(it, modifier = Modifier.testTag(EditEventScreenTestTags.ERROR_MESSAGE))
                    }
                  },
                  modifier =
                      Modifier.fillMaxWidth()
                          .height(140.dp)
                          .testTag(EditEventScreenTestTags.INPUT_EVENT_DESCRIPTION),
                  shape = RoundedCornerShape(8.dp),
                  maxLines = 6)

              // Location Field (Simple text input without search)
              OutlinedTextField(
                  value = eventUIState.location,
                  onValueChange = { editEventViewModel.setLocation(it) },
                  label = { Text("Location") },
                  placeholder = { Text("Rte Cantonale, 1015 Lausanne") },
                  isError = eventUIState.invalidLocationMsg != null,
                  supportingText = {
                    eventUIState.invalidLocationMsg?.let {
                      Text(it, modifier = Modifier.testTag(EditEventScreenTestTags.ERROR_MESSAGE))
                    }
                  },
                  modifier =
                      Modifier.fillMaxWidth().testTag(EditEventScreenTestTags.INPUT_EVENT_LOCATION),
                  shape = RoundedCornerShape(8.dp))

              // Max Participant and Duration Row
              Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = eventUIState.maxParticipants,
                        onValueChange = {
                          editEventViewModel.setMaxParticipants(
                              it.filter { char -> char.isDigit() })
                        },
                        label = { Text("Max Participant") },
                        isError = eventUIState.invalidMaxParticipantsMsg != null,
                        supportingText = {
                          eventUIState.invalidMaxParticipantsMsg?.let {
                            Text(
                                it,
                                modifier = Modifier.testTag(EditEventScreenTestTags.ERROR_MESSAGE))
                          }
                        },
                        modifier =
                            Modifier.weight(1f)
                                .testTag(EditEventScreenTestTags.INPUT_EVENT_MAX_PARTICIPANTS),
                        shape = RoundedCornerShape(8.dp))

                    OutlinedTextField(
                        value = eventUIState.duration,
                        onValueChange = {
                          editEventViewModel.setDuration(it.filter { char -> char.isDigit() })
                        },
                        label = { Text("Duration") },
                        isError = eventUIState.invalidDurationMsg != null,
                        supportingText = {
                          eventUIState.invalidDurationMsg?.let {
                            Text(
                                it,
                                modifier = Modifier.testTag(EditEventScreenTestTags.ERROR_MESSAGE))
                          }
                        },
                        modifier =
                            Modifier.weight(1f)
                                .testTag(EditEventScreenTestTags.INPUT_EVENT_DURATION),
                        shape = RoundedCornerShape(8.dp))
                  }

              // Date Field
              OutlinedTextField(
                  value = eventUIState.date,
                  onValueChange = { editEventViewModel.setDate(it) },
                  label = { Text("Date") },
                  placeholder = { Text("24/12/2023 14:00") },
                  isError = eventUIState.invalidDateMsg != null,
                  supportingText = {
                    eventUIState.invalidDateMsg?.let {
                      Text(it, modifier = Modifier.testTag(EditEventScreenTestTags.ERROR_MESSAGE))
                    }
                  },
                  modifier =
                      Modifier.fillMaxWidth().testTag(EditEventScreenTestTags.INPUT_EVENT_DATE),
                  shape = RoundedCornerShape(8.dp),
                  trailingIcon = {
                    IconButton(onClick = { /* Open date picker dialog */}) {
                      Icon(
                          imageVector = Icons.Default.ArrowDropDown,
                          contentDescription = "Select date")
                    }
                  })

              // Event Visibility Field
              Box {
                OutlinedTextField(
                    value = eventUIState.visibility.ifBlank { "" },
                    onValueChange = {},
                    label = { Text("Event Visibility") },
                    placeholder = { Text("Select visibility") },
                    isError = eventUIState.invalidVisibilityMsg != null,
                    supportingText = {
                      eventUIState.invalidVisibilityMsg?.let {
                        Text(it, modifier = Modifier.testTag(EditEventScreenTestTags.ERROR_MESSAGE))
                      }
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                            .testTag(EditEventScreenTestTags.INPUT_EVENT_VISIBILITY),
                    shape = RoundedCornerShape(8.dp),
                    readOnly = true,
                    trailingIcon = {
                      IconButton(onClick = { showVisibilityMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select visibility")
                      }
                    })

                DropdownMenu(
                    expanded = showVisibilityMenu,
                    onDismissRequest = { showVisibilityMenu = false }) {
                      EventVisibility.values().forEach { vis ->
                        DropdownMenuItem(
                            text = { Text(vis.displayString()) },
                            onClick = {
                              editEventViewModel.setVisibility(vis.name)
                              showVisibilityMenu = false
                            })
                      }
                    }
              }

              // Save Button
              Button(
                  onClick = {
                    if (editEventViewModel.editEvent(eventId)) {
                      onDone()
                    }
                  },
                  modifier =
                      Modifier.fillMaxWidth()
                          .height(56.dp)
                          .testTag(EditEventScreenTestTags.EVENT_SAVE),
                  shape = RoundedCornerShape(8.dp),
                  colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C1C1E)),
                  enabled = eventUIState.isValid) {
                    Text(text = "Save", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                  }
            }
      }
}
