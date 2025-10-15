package com.android.joinme.ui.overview

import android.annotation.SuppressLint
import android.widget.NumberPicker
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.android.joinme.model.event.EventType
import com.android.joinme.model.event.EventVisibility
import java.util.Locale

/** Data class representing the test tags for event form fields. */
data class EventFormTestTags(
    val inputEventType: String,
    val inputEventTitle: String,
    val inputEventDescription: String,
    val inputEventLocation: String,
    val inputEventMaxParticipants: String,
    val inputEventDuration: String,
    val inputEventDate: String,
    val inputEventVisibility: String,
    val buttonSaveEvent: String,
    val errorMessage: String
)

/** Data class representing the state of the event form. */
data class EventFormState(
    val type: String,
    val title: String,
    val description: String,
    val location: String,
    val maxParticipants: String,
    val duration: String,
    val date: String,
    val time: String,
    val visibility: String,
    val isValid: Boolean,
    val invalidTitleMsg: String?,
    val invalidDescriptionMsg: String?,
    val invalidLocationMsg: String?
)

/**
 * Generic event form screen component used by both Create and Edit event screens.
 *
 * @param title The title to display in the top bar
 * @param formState The current state of the form
 * @param testTags Test tags for UI testing
 * @param onTypeChange Callback when event type changes
 * @param onTitleChange Callback when title changes
 * @param onDescriptionChange Callback when description changes
 * @param onLocationChange Callback when location changes
 * @param onMaxParticipantsChange Callback when max participants changes
 * @param onDurationChange Callback when duration changes
 * @param onDateChange Callback when date changes
 * @param onTimeChange Callback when time changes
 * @param onVisibilityChange Callback when visibility changes
 * @param onSave Callback when save button is clicked, returns true if save was successful
 * @param onGoBack Callback when back button is clicked
 * @param saveButtonText Text to display on the save button (default: "Save")
 */
@SuppressLint("DefaultLocale")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventFormScreen(
    title: String,
    formState: EventFormState,
    testTags: EventFormTestTags,
    onTypeChange: (String) -> Unit,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onLocationChange: (String) -> Unit,
    onMaxParticipantsChange: (String) -> Unit,
    onDurationChange: (String) -> Unit,
    onDateChange: (String) -> Unit,
    onTimeChange: (String) -> Unit,
    onVisibilityChange: (String) -> Unit,
    onSave: () -> Boolean,
    onGoBack: () -> Unit,
    saveButtonText: String = "Save"
) {
  val eventTypes = EventType.values().map { it.name.uppercase(Locale.ROOT) }
  val visibilities = listOf(EventVisibility.PUBLIC.name, EventVisibility.PRIVATE.name)

  var showTypeDropdown by remember { mutableStateOf(false) }
  var showVisibilityDropdown by remember { mutableStateOf(false) }

  Scaffold(
      topBar = {
        TopAppBar(
            title = { Text(title) },
            navigationIcon = {
              IconButton(onClick = onGoBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back")
              }
            })
      }) { paddingValues ->
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
                    .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
              // Type dropdown
              ExposedDropdownMenuBox(
                  expanded = showTypeDropdown,
                  onExpandedChange = { showTypeDropdown = !showTypeDropdown }) {
                    OutlinedTextField(
                        value = formState.type,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Event Type") },
                        placeholder = { Text("Select event type") },
                        trailingIcon = {
                          ExposedDropdownMenuDefaults.TrailingIcon(expanded = showTypeDropdown)
                        },
                        modifier =
                            Modifier.menuAnchor().fillMaxWidth().testTag(testTags.inputEventType))
                    ExposedDropdownMenu(
                        expanded = showTypeDropdown,
                        onDismissRequest = { showTypeDropdown = false }) {
                          eventTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                  onTypeChange(type)
                                  showTypeDropdown = false
                                })
                          }
                        }
                  }

              // Title
              OutlinedTextField(
                  value = formState.title,
                  onValueChange = onTitleChange,
                  label = { Text("Title") },
                  placeholder = { Text("Enter event title") },
                  isError = formState.invalidTitleMsg != null,
                  supportingText = {
                    formState.invalidTitleMsg?.let {
                      Text(
                          it,
                          color = MaterialTheme.colorScheme.error,
                          modifier = Modifier.testTag(testTags.errorMessage))
                    }
                  },
                  modifier = Modifier.fillMaxWidth().testTag(testTags.inputEventTitle))

              // Description
              OutlinedTextField(
                  value = formState.description,
                  onValueChange = onDescriptionChange,
                  label = { Text("Description") },
                  placeholder = { Text("Describe your event") },
                  isError = formState.invalidDescriptionMsg != null,
                  supportingText = {
                    formState.invalidDescriptionMsg?.let {
                      Text(
                          it,
                          color = MaterialTheme.colorScheme.error,
                          modifier = Modifier.testTag(testTags.errorMessage))
                    }
                  },
                  modifier =
                      Modifier.fillMaxWidth()
                          .height(150.dp)
                          .testTag(testTags.inputEventDescription))

              // Location
              OutlinedTextField(
                  value = formState.location,
                  onValueChange = onLocationChange,
                  label = { Text("Location") },
                  placeholder = { Text("Enter location name") },
                  isError = formState.invalidLocationMsg != null,
                  supportingText = {
                    formState.invalidLocationMsg?.let {
                      Text(
                          it,
                          color = MaterialTheme.colorScheme.error,
                          modifier = Modifier.testTag(testTags.errorMessage))
                    }
                  },
                  modifier = Modifier.fillMaxWidth().testTag(testTags.inputEventLocation))

              // Max Participants and Duration pickers
              Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    var showParticipantsDialog by remember { mutableStateOf(false) }
                    var tempParticipants by remember {
                      mutableIntStateOf(formState.maxParticipants.toIntOrNull() ?: 1)
                    }

                    Box(
                        modifier =
                            Modifier.weight(1f).clickable { showParticipantsDialog = true }) {
                          OutlinedTextField(
                              value = formState.maxParticipants,
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
                              modifier =
                                  Modifier.fillMaxWidth()
                                      .testTag(testTags.inputEventMaxParticipants))
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
                                  onMaxParticipantsChange(tempParticipants.toString())
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
                      mutableIntStateOf(formState.duration.toIntOrNull() ?: 10)
                    }

                    Box(modifier = Modifier.weight(1f).clickable { showDurationDialog = true }) {
                      OutlinedTextField(
                          value = formState.duration,
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
                          modifier = Modifier.fillMaxWidth().testTag(testTags.inputEventDuration))
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
                                  onDurationChange(tempDuration.toString())
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
                            onDateChange(newDate)
                          },
                          year,
                          month,
                          day)
                    }

                    Box(modifier = Modifier.weight(1f).clickable { datePickerDialog.show() }) {
                      OutlinedTextField(
                          value = formState.date,
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
                          modifier = Modifier.fillMaxWidth().testTag(testTags.inputEventDate))
                    }

                    val timePickerDialog = remember {
                      android.app.TimePickerDialog(
                          context,
                          { _, selectedHour, selectedMinute ->
                            val newTime = String.format("%02d:%02d", selectedHour, selectedMinute)
                            onTimeChange(newTime)
                          },
                          hour,
                          minute,
                          true)
                    }

                    Box(modifier = Modifier.weight(1f).clickable { timePickerDialog.show() }) {
                      OutlinedTextField(
                          value = formState.time,
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
                        value = formState.visibility,
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
                                .testTag(testTags.inputEventVisibility))
                    ExposedDropdownMenu(
                        expanded = showVisibilityDropdown,
                        onDismissRequest = { showVisibilityDropdown = false }) {
                          visibilities.forEach { vis ->
                            DropdownMenuItem(
                                text = { Text(vis) },
                                onClick = {
                                  onVisibilityChange(vis)
                                  showVisibilityDropdown = false
                                })
                          }
                        }
                  }

              Spacer(modifier = Modifier.height(16.dp))

              // Save button
              Button(
                  onClick = { if (onSave()) {} },
                  modifier = Modifier.fillMaxWidth().testTag(testTags.buttonSaveEvent),
                  enabled = formState.isValid) {
                    Text(saveButtonText)
                  }
            }
      }
}
