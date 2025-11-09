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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.android.joinme.model.event.EventType
import com.android.joinme.model.map.Location
import com.android.joinme.ui.theme.ButtonSaveColor
import com.android.joinme.ui.theme.DarkButtonColor
import com.android.joinme.ui.theme.DividerColor
import java.util.Locale
import kotlinx.coroutines.launch

/** Note: This file was created with the help of IA (Claude) */

/** Data class representing the test tags for event for serie form fields. */
data class EventForSerieFormTestTags(
    val inputEventType: String,
    val inputEventTitle: String,
    val inputEventDescription: String,
    val inputEventLocation: String,
    val inputEventLocationSuggestions: String,
    val inputEventDuration: String,
    val buttonSaveEvent: String,
    val errorMessage: String
)

/** Data class representing the state of the event for serie form. */
data class EventForSerieFormState(
    val type: String,
    val title: String,
    val description: String,
    val duration: String,
    val locationQuery: String,
    val locationSuggestions: List<Location>,
    val selectedLocation: Location?,
    val isValid: Boolean,
    val invalidTypeMsg: String?,
    val invalidTitleMsg: String?,
    val invalidDescriptionMsg: String?,
    val invalidDurationMsg: String?,
    val invalidLocationMsg: String?
)

/**
 * Generic event for serie form screen component used by both Create and Edit event for serie
 * screens.
 *
 * @param title The title to display in the top bar
 * @param formState The current state of the form
 * @param testTags Test tags for UI testing
 * @param onTypeChange Callback when event type changes
 * @param onTitleChange Callback when title changes
 * @param onDescriptionChange Callback when description changes
 * @param onDurationChange Callback when duration changes
 * @param onLocationQueryChange Callback when location query changes
 * @param onLocationSelected Callback when a location is selected
 * @param onSearchLocations Callback to trigger location search
 * @param onSave Callback when save button is clicked, returns true if save was successful
 * @param onGoBack Callback when back button is clicked
 * @param saveButtonText Text to display on the save button (default: "Save")
 */
@SuppressLint("DefaultLocale")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventForSerieFormScreen(
    title: String,
    formState: EventForSerieFormState,
    testTags: EventForSerieFormTestTags,
    onTypeChange: (String) -> Unit,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onDurationChange: (String) -> Unit,
    onLocationQueryChange: (String) -> Unit,
    onLocationSelected: (Location) -> Unit,
    onSearchLocations: suspend (String) -> Unit,
    onSave: suspend () -> Boolean,
    onGoBack: () -> Unit,
    saveButtonText: String = "Save"
) {
  val eventTypes = EventType.values().map { it.name.uppercase(Locale.ROOT) }
  var showTypeDropdown by remember { mutableStateOf(false) }
  var showDurationDialog by remember { mutableStateOf(false) }
  var tempDuration by remember { mutableIntStateOf(formState.duration.toIntOrNull() ?: 10) }

  // Update tempDuration when formState.duration changes
  LaunchedEffect(formState.duration) { formState.duration.toIntOrNull()?.let { tempDuration = it } }

  // Trigger location search when query changes
  LaunchedEffect(formState.locationQuery) {
    if (formState.locationQuery.isNotBlank()) {
      onSearchLocations(formState.locationQuery)
    }
  }

  Scaffold(
      topBar = {
        Column {
          TopAppBar(
              title = { Text(title) },
              navigationIcon = {
                IconButton(onClick = onGoBack) {
                  Icon(
                      imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                      contentDescription = "Back")
                }
              },
              colors =
                  TopAppBarDefaults.topAppBarColors(
                      containerColor = MaterialTheme.colorScheme.surface))
          HorizontalDivider(color = DividerColor, thickness = 1.dp)
        }
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
                        isError = formState.invalidTypeMsg != null,
                        supportingText = {
                          formState.invalidTypeMsg?.let {
                            Text(text = it, color = MaterialTheme.colorScheme.error)
                          }
                        },
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

              // Duration picker
              Box(modifier = Modifier.fillMaxWidth().clickable { showDurationDialog = true }) {
                OutlinedTextField(
                    value = formState.duration,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Duration (min)") },
                    placeholder = { Text("Select duration") },
                    isError = formState.invalidDurationMsg != null,
                    supportingText = {
                      formState.invalidDurationMsg?.let {
                        Text(text = it, color = MaterialTheme.colorScheme.error)
                      }
                    },
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            disabledTextColor =
                                LocalContentColor.current.copy(LocalContentColor.current.alpha),
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant),
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
                              setOnValueChangedListener { _, _, newVal -> tempDuration = newVal }
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

              // Location field with search functionality
              val eventFormTestTags =
                  EventFormTestTags(
                      inputEventType = testTags.inputEventType,
                      inputEventTitle = testTags.inputEventTitle,
                      inputEventDescription = testTags.inputEventDescription,
                      inputEventLocation = testTags.inputEventLocation,
                      inputEventLocationSuggestions = testTags.inputEventLocationSuggestions,
                      inputEventMaxParticipants = "",
                      inputEventDuration = testTags.inputEventDuration,
                      inputEventDate = "",
                      inputEventTime = "",
                      inputEventVisibility = "",
                      buttonSaveEvent = testTags.buttonSaveEvent,
                      errorMessage = testTags.errorMessage)

              LocationField(
                  query = formState.locationQuery,
                  suggestions = formState.locationSuggestions,
                  isError = formState.invalidLocationMsg != null,
                  supportingText = formState.invalidLocationMsg,
                  onQueryChange = onLocationQueryChange,
                  onSuggestionSelected = onLocationSelected,
                  testTags = eventFormTestTags)

              Spacer(modifier = Modifier.height(16.dp))

              // Save button
              Button(
                  onClick = {
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                      onSave()
                    }
                  },
                  modifier = Modifier.fillMaxWidth().testTag(testTags.buttonSaveEvent),
                  enabled = formState.isValid,
                  colors =
                      ButtonDefaults.buttonColors(
                          containerColor = DarkButtonColor, contentColor = ButtonSaveColor)) {
                    Text(saveButtonText)
                  }
            }
      }
}
