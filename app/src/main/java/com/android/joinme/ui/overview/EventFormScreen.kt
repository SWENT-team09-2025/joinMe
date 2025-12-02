package com.android.joinme.ui.overview

import android.annotation.SuppressLint
import android.widget.NumberPicker
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import com.android.joinme.model.event.EventType
import com.android.joinme.model.event.EventVisibility
import com.android.joinme.model.map.Location
import com.android.joinme.ui.theme.Dimens
import com.android.joinme.ui.theme.buttonColors
import com.android.joinme.ui.theme.customColors
import com.android.joinme.ui.theme.outlinedTextField
import java.util.Locale

/** Event Type dropdown field composable. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventTypeDropdown(
    formState: EventFormState,
    testTags: EventFormTestTags,
    onTypeChange: (String) -> Unit
) {
  val eventTypes = EventType.values().map { it.name.uppercase(Locale.ROOT) }
  var showTypeDropdown by remember { mutableStateOf(false) }

  ExposedDropdownMenuBox(
      expanded = showTypeDropdown, onExpandedChange = { showTypeDropdown = !showTypeDropdown }) {
        OutlinedTextField(
            value = formState.type,
            onValueChange = {},
            readOnly = true,
            label = { Text("Event Type") },
            placeholder = { Text("Select event type") },
            isError = formState.invalidTypeMsg != null,
            supportingText = {
              if (formState.invalidTypeMsg != null) {
                Text(text = formState.invalidTypeMsg, color = MaterialTheme.colorScheme.error)
              }
            },
            trailingIcon = {
              ExposedDropdownMenuDefaults.TrailingIcon(expanded = showTypeDropdown)
            },
            modifier = Modifier.menuAnchor().fillMaxWidth().testTag(testTags.inputEventType))
        ExposedDropdownMenu(
            expanded = showTypeDropdown,
            onDismissRequest = { showTypeDropdown = false },
            modifier = Modifier.background(MaterialTheme.customColors.backgroundMenu)) {
              eventTypes.forEachIndexed { index, type ->
                DropdownMenuItem(
                    text = {
                      Text(
                          text = type,
                          color = MaterialTheme.colorScheme.onPrimaryContainer,
                          style = MaterialTheme.typography.headlineSmall)
                    },
                    onClick = {
                      onTypeChange(type)
                      showTypeDropdown = false
                    },
                    colors = MaterialTheme.customColors.dropdownMenu)
                if (index < eventTypes.lastIndex) {
                  HorizontalDivider(thickness = Dimens.BorderWidth.thin)
                }
              }
            }
      }
}

/** Max Participants field with NumberPicker dialog. */
@Composable
private fun MaxParticipantsField(
    formState: EventFormState,
    testTags: EventFormTestTags,
    onMaxParticipantsChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
  var showParticipantsDialog by remember { mutableStateOf(false) }
  var tempParticipants by remember {
    mutableIntStateOf(formState.maxParticipants.toIntOrNull() ?: 1)
  }

  Box(modifier = modifier.clickable { showParticipantsDialog = true }) {
    OutlinedTextField(
        value = formState.maxParticipants,
        onValueChange = {},
        readOnly = true,
        label = { Text("Max Participants") },
        placeholder = { Text("Select number") },
        isError = formState.invalidMaxParticipantsMsg != null,
        supportingText = {
          if (formState.invalidMaxParticipantsMsg != null) {
            Text(
                text = formState.invalidMaxParticipantsMsg, color = MaterialTheme.colorScheme.error)
          }
        },
        colors =
            OutlinedTextFieldDefaults.colors(
                disabledTextColor = LocalContentColor.current.copy(LocalContentColor.current.alpha),
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant),
        enabled = false,
        modifier = Modifier.fillMaxWidth().testTag(testTags.inputEventMaxParticipants))
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
                  setOnValueChangedListener { _, _, newVal -> tempParticipants = newVal }
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
          TextButton(onClick = { showParticipantsDialog = false }) { Text("Cancel") }
        })
  }
}

/** Duration field with NumberPicker dialog. */
@Composable
private fun DurationField(
    formState: EventFormState,
    testTags: EventFormTestTags,
    onDurationChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
  var showDurationDialog by remember { mutableStateOf(false) }
  var tempDuration by remember { mutableIntStateOf(formState.duration.toIntOrNull() ?: 10) }

  Box(modifier = modifier.clickable { showDurationDialog = true }) {
    OutlinedTextField(
        value = formState.duration,
        onValueChange = {},
        readOnly = true,
        label = { Text("Duration (min)") },
        placeholder = { Text("Select duration") },
        isError = formState.invalidDurationMsg != null,
        supportingText = {
          if (formState.invalidDurationMsg != null) {
            Text(text = formState.invalidDurationMsg, color = MaterialTheme.colorScheme.error)
          }
        },
        colors = MaterialTheme.customColors.outlinedTextField(),
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
        dismissButton = { TextButton(onClick = { showDurationDialog = false }) { Text("Cancel") } })
  }
}

/** Date and Time picker fields row. */
@SuppressLint("DefaultLocale")
@Composable
private fun DateTimePickerRow(
    formState: EventFormState,
    testTags: EventFormTestTags,
    onDateChange: (String) -> Unit,
    onTimeChange: (String) -> Unit
) {
  Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(Dimens.Padding.medium)) {
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
                    String.format("%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear)
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
              isError = formState.invalidDateMsg != null,
              supportingText = {
                if (formState.invalidDateMsg != null) {
                  Text(text = formState.invalidDateMsg, color = MaterialTheme.colorScheme.error)
                }
              },
              colors = MaterialTheme.customColors.outlinedTextField(),
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
              colors = MaterialTheme.customColors.outlinedTextField(),
              enabled = false,
              modifier = Modifier.fillMaxWidth().testTag(testTags.inputEventTime))
        }
      }
}

/** Event Visibility dropdown field. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventVisibilityDropdown(
    formState: EventFormState,
    testTags: EventFormTestTags,
    onVisibilityChange: (String) -> Unit
) {
  val visibilities = listOf(EventVisibility.PUBLIC.name, EventVisibility.PRIVATE.name)
  var showVisibilityDropdown by remember { mutableStateOf(false) }

  ExposedDropdownMenuBox(
      expanded = showVisibilityDropdown,
      onExpandedChange = { showVisibilityDropdown = !showVisibilityDropdown }) {
        OutlinedTextField(
            value = formState.visibility,
            onValueChange = {},
            readOnly = true,
            label = { Text("Event Visibility") },
            placeholder = { Text("Select visibility") },
            isError = formState.invalidVisibilityMsg != null,
            supportingText = {
              if (formState.invalidVisibilityMsg != null) {
                Text(text = formState.invalidVisibilityMsg, color = MaterialTheme.colorScheme.error)
              }
            },
            trailingIcon = {
              ExposedDropdownMenuDefaults.TrailingIcon(expanded = showVisibilityDropdown)
            },
            modifier = Modifier.menuAnchor().fillMaxWidth().testTag(testTags.inputEventVisibility))
        ExposedDropdownMenu(
            expanded = showVisibilityDropdown,
            onDismissRequest = { showVisibilityDropdown = false },
            modifier = Modifier.background(MaterialTheme.customColors.backgroundMenu)) {
              visibilities.forEachIndexed { index, vis ->
                DropdownMenuItem(
                    text = {
                      Text(
                          text = vis,
                          color = MaterialTheme.colorScheme.onPrimaryContainer,
                          style = MaterialTheme.typography.headlineSmall)
                    },
                    onClick = {
                      onVisibilityChange(vis)
                      showVisibilityDropdown = false
                    },
                    colors = MaterialTheme.customColors.dropdownMenu)
                if (index < visibilities.lastIndex) {
                  HorizontalDivider(thickness = Dimens.BorderWidth.thin)
                }
              }
            }
      }
}

/** Data class representing the test tags for event form fields. */
data class EventFormTestTags(
    val inputEventType: String,
    val inputEventTitle: String,
    val inputEventDescription: String,
    val inputEventLocation: String,
    val inputEventLocationSuggestions: String,
    val forEachInputEventLocationSuggestion: String,
    val inputEventMaxParticipants: String,
    val inputEventDuration: String,
    val inputEventDate: String,
    val inputEventTime: String,
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
    val locationQuery: String,
    val locationSuggestions: List<Location>,
    val selectedLocation: Location?,
    val isValid: Boolean,
    val invalidTypeMsg: String?,
    val invalidTitleMsg: String?,
    val invalidDescriptionMsg: String?,
    val invalidLocationMsg: String?,
    val invalidMaxParticipantsMsg: String?,
    val invalidDurationMsg: String?,
    val invalidDateMsg: String?,
    val invalidVisibilityMsg: String?
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
 * @param onLocationQueryChange Callback when location changes
 * @param onMaxParticipantsChange Callback when max participants changes
 * @param onDurationChange Callback when duration changes
 * @param onDateChange Callback when date changes
 * @param onTimeChange Callback when time changes
 * @param onVisibilityChange Callback when visibility changes
 * @param onSave Callback when save button is clicked, returns true if save was successful
 * @param onGoBack Callback when back button is clicked
 * @param saveButtonText Text to display on the save button (default: "SAVE")
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
    onLocationQueryChange: (String) -> Unit,
    onSelectLocationChange: (Location) -> Unit,
    onMaxParticipantsChange: (String) -> Unit,
    onDurationChange: (String) -> Unit,
    onDateChange: (String) -> Unit,
    onTimeChange: (String) -> Unit,
    onVisibilityChange: (String) -> Unit,
    onSave: () -> Boolean,
    onGoBack: () -> Unit,
    saveButtonText: String = "SAVE",
    extraContent: (@Composable () -> Unit)? = null,
    isGroupEvent: Boolean = false
) {
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
          HorizontalDivider(
              color = MaterialTheme.colorScheme.outlineVariant, thickness = Dimens.BorderWidth.thin)
        }
      }) { paddingValues ->
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(Dimens.Padding.medium)
                    .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(Dimens.Padding.small)) {
              // Extra content slot (e.g., group selection for CreateEvent)
              extraContent?.invoke()

              // Type dropdown (hidden for group events)
              if (!isGroupEvent) {
                EventTypeDropdown(
                    formState = formState, testTags = testTags, onTypeChange = onTypeChange)
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
                          .height(Dimens.EventForm.descriptionField)
                          .testTag(testTags.inputEventDescription))

              // Location
              LocationField(
                  query = formState.locationQuery,
                  suggestions = formState.locationSuggestions,
                  isError = formState.invalidLocationMsg != null,
                  supportingText = formState.invalidLocationMsg,
                  onQueryChange = { selection -> onLocationQueryChange(selection) },
                  onSuggestionSelected = { name -> onSelectLocationChange(name) },
                  testTags = testTags)

              // Max Participants and Duration pickers
              Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing.medium)) {
                    // Max Participants (hidden for group events)
                    if (!isGroupEvent) {
                      MaxParticipantsField(
                          formState = formState,
                          testTags = testTags,
                          onMaxParticipantsChange = onMaxParticipantsChange,
                          modifier = Modifier.weight(1f))
                    }

                    DurationField(
                        formState = formState,
                        testTags = testTags,
                        onDurationChange = onDurationChange,
                        modifier = Modifier.weight(1f))
                  }

              // Date and Time pickers
              DateTimePickerRow(
                  formState = formState,
                  testTags = testTags,
                  onDateChange = onDateChange,
                  onTimeChange = onTimeChange)

              // Visibility dropdown (hidden for group events)
              if (!isGroupEvent) {
                EventVisibilityDropdown(
                    formState = formState,
                    testTags = testTags,
                    onVisibilityChange = onVisibilityChange)
              }

              Spacer(modifier = Modifier.height(Dimens.Spacing.medium))

              // Save button
              Button(
                  onClick = { onSave() },
                  modifier = Modifier.fillMaxWidth().testTag(testTags.buttonSaveEvent),
                  enabled = formState.isValid,
                  colors = MaterialTheme.customColors.buttonColors()) {
                    Text(saveButtonText, style = MaterialTheme.typography.titleSmall)
                  }
            }
      }
}

/**
 * A composable text field with real-time location suggestions.
 *
 * Displays an `OutlinedTextField` where users can type a location name. As the user types, a
 * dropdown list of matching suggestions appears below the field. Selecting a suggestion fills the
 * field and hides the dropdown.
 *
 * This component is used in Create/Edit Event screens to allow users to easily search and select
 * real-world locations from Nominatim autocomplete results.
 *
 * ### Behavior
 * - The dropdown menu appears when the query is non-empty and suggestions are available.
 * - Selecting a suggestion hides the menu and updates the field.
 * - The menu will not immediately reopen after a selection.
 *
 * @param query The current text input value of the location field.
 * @param suggestions A list of [Location] suggestions returned by the repository.
 * @param isError Whether the field should display an error state.
 * @param supportingText Optional helper or error message displayed below the field.
 * @param onQueryChange Callback triggered when the user changes the query text.
 * @param onSuggestionSelected Callback invoked when a suggestion is selected from the dropdown.
 * @param modifier Optional [Modifier] for layout customization.
 * @param testTags [EventFormTestTags] providing identifiers for UI testing (e.g., text field and
 *   dropdown).
 */
@Composable
fun LocationField(
    query: String,
    suggestions: List<Location>,
    isError: Boolean,
    supportingText: String?,
    onQueryChange: (String) -> Unit,
    onSuggestionSelected: (Location) -> Unit,
    modifier: Modifier = Modifier,
    testTags: EventFormTestTags
) {
  var showSuggestions by remember { mutableStateOf(false) }
  var suppressNextOpen by remember { mutableStateOf(false) }

  LaunchedEffect(query, suggestions) {
    if (!suppressNextOpen) {
      showSuggestions = query.isNotBlank() && suggestions.isNotEmpty()
    } else {
      suppressNextOpen = false
    }
  }

  Box(modifier = modifier) {
    Column {
      OutlinedTextField(
          value = query,
          onValueChange = { onQueryChange(it) },
          label = { Text("Location") },
          isError = isError,
          singleLine = true,
          supportingText = {
            if (supportingText != null) {
              Text(text = supportingText, color = MaterialTheme.colorScheme.error)
            }
          },
          modifier = Modifier.fillMaxWidth().testTag(testTags.inputEventLocation))

      if (showSuggestions) {
        Spacer(Modifier.height(Dimens.Spacing.small))
        Surface(
            shape = RoundedCornerShape(Dimens.CornerRadius.large),
            tonalElevation = Dimens.Elevation.medium,
            modifier =
                Modifier.fillMaxWidth()
                    .heightIn(max = Dimens.EventForm.suggestionsField)
                    .border(
                        width = Dimens.BorderWidth.thin,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(Dimens.CornerRadius.large))) {
              LazyColumn(modifier = Modifier.testTag(testTags.inputEventLocationSuggestions)) {
                itemsIndexed(suggestions) { _, loc ->
                  ListItem(
                      headlineContent = { Text(loc.name) },
                      modifier =
                          Modifier.fillMaxWidth()
                              .clickable {
                                showSuggestions = false
                                suppressNextOpen = true
                                onSuggestionSelected(loc)
                              }
                              .padding(horizontal = Dimens.Padding.extraSmall)
                              .testTag(testTags.forEachInputEventLocationSuggestion))
                  HorizontalDivider(Modifier, thickness = Dimens.BorderWidth.thin)
                }
              }
            }
      }
    }
  }
}
