package com.android.joinme.ui.overview

import android.annotation.SuppressLint
import android.widget.NumberPicker
import android.widget.Toast
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.joinme.model.event.EventType
import com.android.joinme.ui.theme.ButtonSaveColor
import com.android.joinme.ui.theme.DarkButtonColor
import com.android.joinme.ui.theme.DividerColor
import java.util.Locale
import kotlinx.coroutines.launch

/** Test tags for CreateEventForSerie screen UI elements. */
object CreateEventForSerieScreenTestTags {
  const val INPUT_EVENT_TYPE = "inputEventType"
  const val INPUT_EVENT_TITLE = "inputEventTitle"
  const val INPUT_EVENT_DESCRIPTION = "inputEventDescription"
  const val INPUT_EVENT_LOCATION = "inputEventLocation"
  const val INPUT_EVENT_LOCATION_SUGGESTIONS = "inputEventLocationSuggestions"
  const val INPUT_EVENT_DURATION = "inputEventDuration"
  const val BUTTON_SAVE_EVENT = "buttonSaveEvent"
  const val ERROR_MESSAGE = "errorMessage"
}

/**
 * Screen for creating an event within an existing serie.
 *
 * Displays after serie creation to add the first event. Only requires event-specific fields (type,
 * title, description, duration, location). Inherits maxParticipants, visibility, and ownerId from
 * the parent serie. Event date is calculated automatically.
 *
 * @param serieId The ID of the serie to add this event to
 * @param createEventForSerieViewModel ViewModel for managing screen state
 * @param onGoBack Callback when back button is pressed
 * @param onDone Callback when event is successfully created
 */
@SuppressLint("DefaultLocale")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEventForSerieScreen(
    serieId: String,
    createEventForSerieViewModel: CreateEventForSerieViewModel = viewModel(),
    onGoBack: () -> Unit = {},
    onDone: () -> Unit = {}
) {
  val uiState by createEventForSerieViewModel.uiState.collectAsState()
  val errorMsg = uiState.errorMsg
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()

  LaunchedEffect(errorMsg) {
    if (errorMsg != null) {
      Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
      createEventForSerieViewModel.clearErrorMsg()
    }
  }

  LaunchedEffect(uiState.locationQuery) {
    if (uiState.locationQuery.isNotBlank()) {
      createEventForSerieViewModel.searchLocations(uiState.locationQuery)
    }
  }

  val eventTypes = EventType.values().map { it.name.uppercase(Locale.ROOT) }
  var showTypeDropdown by remember { mutableStateOf(false) }

  Scaffold(
      topBar = {
        Column {
          TopAppBar(
              title = { Text("Create Event for Serie") },
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
                        value = uiState.type,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Event Type") },
                        placeholder = { Text("Select event type") },
                        isError = uiState.invalidTypeMsg != null,
                        supportingText = {
                          uiState.invalidTypeMsg?.let {
                            Text(text = it, color = MaterialTheme.colorScheme.error)
                          }
                        },
                        trailingIcon = {
                          ExposedDropdownMenuDefaults.TrailingIcon(expanded = showTypeDropdown)
                        },
                        modifier =
                            Modifier.menuAnchor()
                                .fillMaxWidth()
                                .testTag(CreateEventForSerieScreenTestTags.INPUT_EVENT_TYPE))
                    ExposedDropdownMenu(
                        expanded = showTypeDropdown,
                        onDismissRequest = { showTypeDropdown = false }) {
                          eventTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                  createEventForSerieViewModel.setType(type)
                                  showTypeDropdown = false
                                })
                          }
                        }
                  }

              // Title
              OutlinedTextField(
                  value = uiState.title,
                  onValueChange = { createEventForSerieViewModel.setTitle(it) },
                  label = { Text("Title") },
                  placeholder = { Text("Enter event title") },
                  isError = uiState.invalidTitleMsg != null,
                  supportingText = {
                    uiState.invalidTitleMsg?.let {
                      Text(
                          it,
                          color = MaterialTheme.colorScheme.error,
                          modifier =
                              Modifier.testTag(CreateEventForSerieScreenTestTags.ERROR_MESSAGE))
                    }
                  },
                  modifier =
                      Modifier.fillMaxWidth()
                          .testTag(CreateEventForSerieScreenTestTags.INPUT_EVENT_TITLE))

              // Description
              OutlinedTextField(
                  value = uiState.description,
                  onValueChange = { createEventForSerieViewModel.setDescription(it) },
                  label = { Text("Description") },
                  placeholder = { Text("Describe your event") },
                  isError = uiState.invalidDescriptionMsg != null,
                  supportingText = {
                    uiState.invalidDescriptionMsg?.let {
                      Text(
                          it,
                          color = MaterialTheme.colorScheme.error,
                          modifier =
                              Modifier.testTag(CreateEventForSerieScreenTestTags.ERROR_MESSAGE))
                    }
                  },
                  modifier =
                      Modifier.fillMaxWidth()
                          .height(150.dp)
                          .testTag(CreateEventForSerieScreenTestTags.INPUT_EVENT_DESCRIPTION))

              // Duration picker
              var showDurationDialog by remember { mutableStateOf(false) }
              var tempDuration by remember {
                mutableIntStateOf(uiState.duration.toIntOrNull() ?: 10)
              }

              Box(modifier = Modifier.fillMaxWidth().clickable { showDurationDialog = true }) {
                OutlinedTextField(
                    value = uiState.duration,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Duration (min)") },
                    placeholder = { Text("Select duration") },
                    isError = uiState.invalidDurationMsg != null,
                    supportingText = {
                      uiState.invalidDurationMsg?.let {
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
                    modifier =
                        Modifier.fillMaxWidth()
                            .testTag(CreateEventForSerieScreenTestTags.INPUT_EVENT_DURATION))
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
                            createEventForSerieViewModel.setDuration(tempDuration.toString())
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
              val testTags =
                  EventFormTestTags(
                      inputEventType = CreateEventForSerieScreenTestTags.INPUT_EVENT_TYPE,
                      inputEventTitle = CreateEventForSerieScreenTestTags.INPUT_EVENT_TITLE,
                      inputEventDescription =
                          CreateEventForSerieScreenTestTags.INPUT_EVENT_DESCRIPTION,
                      inputEventLocation = CreateEventForSerieScreenTestTags.INPUT_EVENT_LOCATION,
                      inputEventLocationSuggestions =
                          CreateEventForSerieScreenTestTags.INPUT_EVENT_LOCATION_SUGGESTIONS,
                      inputEventMaxParticipants = "",
                      inputEventDuration = CreateEventForSerieScreenTestTags.INPUT_EVENT_DURATION,
                      inputEventDate = "",
                      inputEventTime = "",
                      inputEventVisibility = "",
                      buttonSaveEvent = CreateEventForSerieScreenTestTags.BUTTON_SAVE_EVENT,
                      errorMessage = CreateEventForSerieScreenTestTags.ERROR_MESSAGE)

              LocationField(
                  query = uiState.locationQuery,
                  suggestions = uiState.locationSuggestions,
                  isError = uiState.invalidLocationMsg != null,
                  supportingText = uiState.invalidLocationMsg,
                  onQueryChange = { createEventForSerieViewModel.setLocationQuery(it) },
                  onSuggestionSelected = { createEventForSerieViewModel.selectLocation(it) },
                  testTags = testTags)

              Spacer(modifier = Modifier.height(16.dp))

              // Save button
              Button(
                  onClick = {
                    coroutineScope.launch {
                      if (createEventForSerieViewModel.createEventForSerie(serieId)) {
                        onDone()
                      }
                    }
                  },
                  modifier =
                      Modifier.fillMaxWidth()
                          .testTag(CreateEventForSerieScreenTestTags.BUTTON_SAVE_EVENT),
                  enabled = uiState.isValid,
                  colors =
                      ButtonDefaults.buttonColors(
                          containerColor = DarkButtonColor, contentColor = ButtonSaveColor)) {
                    Text("Create Event")
                  }
            }
      }
}
