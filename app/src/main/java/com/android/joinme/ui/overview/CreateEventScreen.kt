package com.android.joinme.ui.overview

import android.annotation.SuppressLint
import android.widget.NumberPicker
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.joinme.model.event.EventType
import com.android.joinme.model.event.EventVisibility
import java.util.Locale

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

@SuppressLint("DefaultLocale")
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

  val eventTypes = EventType.values().map { it.name.uppercase(Locale.ROOT) }
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

              // Max Participants and Duration pickers
              Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    var showParticipantsDialog by remember { mutableStateOf(false) }
                    var tempParticipants by remember {
                      mutableIntStateOf(uiState.maxParticipants.toIntOrNull() ?: 1)
                    }

                    Box(
                        modifier =
                            Modifier.weight(1f).clickable { showParticipantsDialog = true }) {
                          OutlinedTextField(
                              value = uiState.maxParticipants,
                              onValueChange = {},
                              readOnly = true,
                              label = { Text("Max Participants") },
                              placeholder = { Text("Select number") },
                              colors =
                                  OutlinedTextFieldDefaults.colors(
                                      disabledTextColor =
                                          LocalContentColor.current.copy(
                                              LocalContentColor.current.alpha),
                                      disabledBorderColor = MaterialTheme.colorScheme.outline,
                                      disabledLabelColor =
                                          MaterialTheme.colorScheme.onSurfaceVariant,
                                      disabledPlaceholderColor =
                                          MaterialTheme.colorScheme.onSurfaceVariant,
                                      disabledLeadingIconColor =
                                          MaterialTheme.colorScheme.onSurfaceVariant,
                                      disabledTrailingIconColor =
                                          MaterialTheme.colorScheme.onSurfaceVariant),
                              enabled = false,
                              modifier = Modifier.fillMaxWidth())
                        }

                    if (showParticipantsDialog) {
                      AlertDialog(
                          onDismissRequest = { showParticipantsDialog = false },
                          title = { Text("Select Max Participants") },
                          text = {
                            AndroidView(
                                factory = { context ->
                                  NumberPicker(context).apply {
                                    minValue = 1
                                    maxValue = 100
                                    value = tempParticipants
                                    wrapSelectorWheel = true
                                    setOnValueChangedListener { _, _, newVal ->
                                      tempParticipants = newVal
                                    }
                                  }
                                },
                                update = { picker -> picker.value = tempParticipants },
                                modifier = Modifier.fillMaxWidth())
                          },
                          confirmButton = {
                            TextButton(
                                onClick = {
                                  createEventViewModel.setMaxParticipants(
                                      tempParticipants.toString())
                                  showParticipantsDialog = false
                                }) {
                                  Text("OK")
                                }
                          },
                          dismissButton = {
                            TextButton(onClick = { showParticipantsDialog = false }) {
                              Text("Cancel")
                            }
                          })
                    }

                    var showDurationDialog by remember { mutableStateOf(false) }
                    var tempDuration by remember {
                      mutableIntStateOf(uiState.duration.toIntOrNull() ?: 10)
                    }

                    Box(modifier = Modifier.weight(1f).clickable { showDurationDialog = true }) {
                      OutlinedTextField(
                          value = uiState.duration,
                          onValueChange = {},
                          readOnly = true,
                          label = { Text("Duration (min)") },
                          placeholder = { Text("Select duration") },
                          colors =
                              OutlinedTextFieldDefaults.colors(
                                  disabledTextColor =
                                      LocalContentColor.current.copy(
                                          LocalContentColor.current.alpha),
                                  disabledBorderColor = MaterialTheme.colorScheme.outline,
                                  disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                  disabledPlaceholderColor =
                                      MaterialTheme.colorScheme.onSurfaceVariant,
                                  disabledLeadingIconColor =
                                      MaterialTheme.colorScheme.onSurfaceVariant,
                                  disabledTrailingIconColor =
                                      MaterialTheme.colorScheme.onSurfaceVariant),
                          enabled = false,
                          modifier = Modifier.fillMaxWidth())
                    }

                    if (showDurationDialog) {
                      AlertDialog(
                          onDismissRequest = { showDurationDialog = false },
                          title = { Text("Select Duration (min)") },
                          text = {
                            AndroidView(
                                factory = { context ->
                                  NumberPicker(context).apply {
                                    minValue = 10
                                    maxValue = 300
                                    value = tempDuration
                                    wrapSelectorWheel = true
                                    setOnValueChangedListener { _, _, newVal ->
                                      tempDuration = newVal
                                    }
                                  }
                                },
                                update = { picker -> picker.value = tempDuration },
                                modifier = Modifier.fillMaxWidth())
                          },
                          confirmButton = {
                            TextButton(
                                onClick = {
                                  createEventViewModel.setDuration(tempDuration.toString())
                                  showDurationDialog = false
                                }) {
                                  Text("OK")
                                }
                          },
                          dismissButton = {
                            TextButton(onClick = { showDurationDialog = false }) { Text("Cancel") }
                          })
                    }
                  }

              // Date and Time pickers
              Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val context = LocalContext.current
                    val calendar = remember { java.util.Calendar.getInstance() }
                    val (year, month, day) =
                        listOf(
                            calendar.get(java.util.Calendar.YEAR),
                            calendar.get(java.util.Calendar.MONTH),
                            calendar.get(java.util.Calendar.DAY_OF_MONTH))
                    val (hour, minute) =
                        listOf(
                            calendar.get(java.util.Calendar.HOUR_OF_DAY),
                            calendar.get(java.util.Calendar.MINUTE))

                    val datePickerDialog = remember {
                      android.app.DatePickerDialog(
                          context,
                          { _, selectedYear, selectedMonth, selectedDay ->
                            val newDate =
                                String.format(
                                    "%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear)
                            createEventViewModel.setDate(newDate)
                          },
                          year,
                          month,
                          day)
                    }

                    Box(modifier = Modifier.weight(1f).clickable { datePickerDialog.show() }) {
                      OutlinedTextField(
                          value = uiState.date,
                          onValueChange = {},
                          readOnly = true,
                          label = { Text("Date") },
                          placeholder = { Text("Select date") },
                          colors =
                              OutlinedTextFieldDefaults.colors(
                                  disabledTextColor =
                                      LocalContentColor.current.copy(
                                          LocalContentColor.current.alpha),
                                  disabledBorderColor = MaterialTheme.colorScheme.outline,
                                  disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                  disabledPlaceholderColor =
                                      MaterialTheme.colorScheme.onSurfaceVariant,
                                  disabledLeadingIconColor =
                                      MaterialTheme.colorScheme.onSurfaceVariant,
                                  disabledTrailingIconColor =
                                      MaterialTheme.colorScheme.onSurfaceVariant),
                          enabled = false,
                          modifier = Modifier.fillMaxWidth())
                    }

                    val timePickerDialog = remember {
                      android.app.TimePickerDialog(
                          context,
                          { _, selectedHour, selectedMinute ->
                            val newTime = String.format("%02d:%02d", selectedHour, selectedMinute)
                            createEventViewModel.setTime(newTime)
                          },
                          hour,
                          minute,
                          true)
                    }

                    Box(modifier = Modifier.weight(1f).clickable { timePickerDialog.show() }) {
                      OutlinedTextField(
                          value = uiState.time,
                          onValueChange = {},
                          readOnly = true,
                          label = { Text("Time") },
                          placeholder = { Text("Select time") },
                          colors =
                              OutlinedTextFieldDefaults.colors(
                                  disabledTextColor =
                                      LocalContentColor.current.copy(
                                          LocalContentColor.current.alpha),
                                  disabledBorderColor = MaterialTheme.colorScheme.outline,
                                  disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                  disabledPlaceholderColor =
                                      MaterialTheme.colorScheme.onSurfaceVariant,
                                  disabledLeadingIconColor =
                                      MaterialTheme.colorScheme.onSurfaceVariant,
                                  disabledTrailingIconColor =
                                      MaterialTheme.colorScheme.onSurfaceVariant),
                          enabled = false,
                          modifier = Modifier.fillMaxWidth())
                    }
                  }

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
