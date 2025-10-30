package com.android.joinme.ui.overview

import android.annotation.SuppressLint
import android.widget.NumberPicker
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.joinme.ui.theme.ButtonSaveColor
import com.android.joinme.ui.theme.DarkButtonColor
import java.util.*
import kotlinx.coroutines.launch

/**
 * Test tags for UI testing of the Create Serie screen components.
 *
 * Provides consistent identifiers for testing individual UI elements including input fields,
 * buttons, and error messages.
 */
object CreateSerieScreenTestTags {
  /** Test tag for the serie title input field */
  const val INPUT_SERIE_TITLE = "inputSerieTitle"

  /** Test tag for the serie description input field */
  const val INPUT_SERIE_DESCRIPTION = "inputSerieDescription"

  /** Test tag for the max participants input field (read-only, opens NumberPicker dialog) */
  const val INPUT_SERIE_MAX_PARTICIPANTS = "inputSerieMaxParticipants"

  /** Test tag for the date input field (read-only, opens DatePickerDialog) */
  const val INPUT_SERIE_DATE = "inputSerieDate"

  /** Test tag for the time input field (read-only, opens TimePickerDialog) */
  const val INPUT_SERIE_TIME = "inputSerieTime"

  /** Test tag for the visibility dropdown field (PUBLIC/PRIVATE) */
  const val INPUT_SERIE_VISIBILITY = "inputSerieVisibility"

  /** Test tag for the save/next button */
  const val BUTTON_SAVE_SERIE = "buttonSaveSerie"

  /** Test tag for error messages displayed via Toast */
  const val ERROR_MESSAGE = "errorMessage"
}

/**
 * Screen for creating a new event serie.
 *
 * Provides a form to create a serie with title, description, max participants, date, time, and
 * visibility fields. Uses native dialogs for date/time/number selection. Real-time validation
 * displays errors below each field. The "Next" button is enabled only when all fields are valid.
 *
 * @param createSerieViewModel ViewModel managing the screen state and business logic
 * @param onGoBack Callback invoked when the back button is pressed
 * @param onDone Callback invoked when the serie is successfully created
 */
@SuppressLint("DefaultLocale")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateSerieScreen(
    createSerieViewModel: CreateSerieViewModel = viewModel(),
    onGoBack: () -> Unit = {},
    onDone: () -> Unit = {}
) {
  val uiState by createSerieViewModel.uiState.collectAsState()
  val errorMsg = uiState.errorMsg
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()

  LaunchedEffect(errorMsg) {
    if (errorMsg != null) {
      Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
      createSerieViewModel.clearErrorMsg()
    }
  }

  Scaffold(
      topBar = {
        TopAppBar(
            title = { Text("Create Serie") },
            navigationIcon = {
              IconButton(onClick = onGoBack) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
              }
            })
      }) { paddingValues ->
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
              Spacer(modifier = Modifier.height(8.dp))

              // Title field
              OutlinedTextField(
                  value = uiState.title,
                  onValueChange = { createSerieViewModel.setTitle(it) },
                  label = { Text("Title") },
                  modifier =
                      Modifier.fillMaxWidth().testTag(CreateSerieScreenTestTags.INPUT_SERIE_TITLE),
                  isError = uiState.invalidTitleMsg != null,
                  supportingText = {
                    if (uiState.invalidTitleMsg != null) {
                      Text(
                          text = uiState.invalidTitleMsg!!, color = MaterialTheme.colorScheme.error)
                    }
                  },
                  singleLine = true)

              // Description field
              OutlinedTextField(
                  value = uiState.description,
                  onValueChange = { createSerieViewModel.setDescription(it) },
                  label = { Text("Description") },
                  modifier =
                      Modifier.fillMaxWidth()
                          .height(120.dp)
                          .testTag(CreateSerieScreenTestTags.INPUT_SERIE_DESCRIPTION),
                  isError = uiState.invalidDescriptionMsg != null,
                  supportingText = {
                    if (uiState.invalidDescriptionMsg != null) {
                      Text(
                          text = uiState.invalidDescriptionMsg!!,
                          color = MaterialTheme.colorScheme.error)
                    }
                  },
                  maxLines = 4)

              // Max Participants field with NumberPicker
              var showMaxParticipantsPicker by remember { mutableStateOf(false) }
              var tempParticipants by remember {
                mutableIntStateOf(uiState.maxParticipants.toIntOrNull() ?: 10)
              }

              Box(
                  modifier =
                      Modifier.width(180.dp).clickable { showMaxParticipantsPicker = true }) {
                    OutlinedTextField(
                        value = uiState.maxParticipants,
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        label = { Text("Max Participants") },
                        placeholder = { Text("Select number") },
                        modifier =
                            Modifier.fillMaxWidth()
                                .testTag(CreateSerieScreenTestTags.INPUT_SERIE_MAX_PARTICIPANTS),
                        isError = uiState.invalidMaxParticipantsMsg != null,
                        supportingText = {
                          if (uiState.invalidMaxParticipantsMsg != null) {
                            Text(
                                text = uiState.invalidMaxParticipantsMsg!!,
                                color = MaterialTheme.colorScheme.error)
                          }
                        },
                        colors = readOnlyTextFieldColors(),
                        singleLine = true)
                  }

              if (showMaxParticipantsPicker) {
                AlertDialog(
                    onDismissRequest = { showMaxParticipantsPicker = false },
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
                            createSerieViewModel.setMaxParticipants(tempParticipants.toString())
                            showMaxParticipantsPicker = false
                          }) {
                            Text("OK")
                          }
                    },
                    dismissButton = {
                      TextButton(onClick = { showMaxParticipantsPicker = false }) { Text("Cancel") }
                    })
              }

              // Date and Time row
              Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val calendar = remember { Calendar.getInstance() }
                    val (year, month, day) =
                        listOf(
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH))
                    val (hour, minute) =
                        listOf(calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE))

                    val datePickerDialog = remember {
                      android.app.DatePickerDialog(
                          context,
                          { _, selectedYear, selectedMonth, selectedDay ->
                            val newDate =
                                String.format(
                                    "%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear)
                            createSerieViewModel.setDate(newDate)
                          },
                          year,
                          month,
                          day)
                    }

                    // Date field
                    Box(modifier = Modifier.weight(1f).clickable { datePickerDialog.show() }) {
                      OutlinedTextField(
                          value = uiState.date,
                          onValueChange = {},
                          readOnly = true,
                          enabled = false,
                          label = { Text("Date") },
                          placeholder = { Text("Select date") },
                          modifier =
                              Modifier.fillMaxWidth()
                                  .testTag(CreateSerieScreenTestTags.INPUT_SERIE_DATE),
                          isError = uiState.invalidDateMsg != null,
                          supportingText = {
                            if (uiState.invalidDateMsg != null) {
                              Text(
                                  text = uiState.invalidDateMsg!!,
                                  color = MaterialTheme.colorScheme.error)
                            }
                          },
                          colors = readOnlyTextFieldColors(),
                          singleLine = true)
                    }

                    val timePickerDialog = remember {
                      android.app.TimePickerDialog(
                          context,
                          { _, selectedHour, selectedMinute ->
                            val newTime = String.format("%02d:%02d", selectedHour, selectedMinute)
                            createSerieViewModel.setTime(newTime)
                          },
                          hour,
                          minute,
                          true)
                    }

                    // Time field
                    Box(modifier = Modifier.weight(1f).clickable { timePickerDialog.show() }) {
                      OutlinedTextField(
                          value = uiState.time,
                          onValueChange = {},
                          readOnly = true,
                          enabled = false,
                          label = { Text("Time") },
                          placeholder = { Text("Select time") },
                          modifier =
                              Modifier.fillMaxWidth()
                                  .testTag(CreateSerieScreenTestTags.INPUT_SERIE_TIME),
                          isError = uiState.invalidTimeMsg != null,
                          supportingText = {
                            if (uiState.invalidTimeMsg != null) {
                              Text(
                                  text = uiState.invalidTimeMsg!!,
                                  color = MaterialTheme.colorScheme.error)
                            }
                          },
                          colors = readOnlyTextFieldColors(),
                          singleLine = true)
                    }
                  }

              // Serie Visibility field with dropdown
              val visibilityOptions = listOf("PUBLIC", "PRIVATE")
              var expandedVisibility by remember { mutableStateOf(false) }

              ExposedDropdownMenuBox(
                  expanded = expandedVisibility,
                  onExpandedChange = { expandedVisibility = !expandedVisibility }) {
                    OutlinedTextField(
                        value = uiState.visibility,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Visibility") },
                        trailingIcon = {
                          ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedVisibility)
                        },
                        modifier =
                            Modifier.fillMaxWidth()
                                .menuAnchor()
                                .testTag(CreateSerieScreenTestTags.INPUT_SERIE_VISIBILITY),
                        isError = uiState.invalidVisibilityMsg != null,
                        supportingText = {
                          if (uiState.invalidVisibilityMsg != null) {
                            Text(
                                text = uiState.invalidVisibilityMsg!!,
                                color = MaterialTheme.colorScheme.error)
                          }
                        },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors())

                    ExposedDropdownMenu(
                        expanded = expandedVisibility,
                        onDismissRequest = { expandedVisibility = false }) {
                          visibilityOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                  createSerieViewModel.setVisibility(option)
                                  expandedVisibility = false
                                })
                          }
                        }
                  }

              Spacer(modifier = Modifier.height(16.dp))

              // Next button
              Button(
                  onClick = {
                    coroutineScope.launch {
                      if (createSerieViewModel.createSerie()) {
                        onDone()
                      }
                    }
                  },
                  modifier =
                      Modifier.fillMaxWidth()
                          .height(56.dp)
                          .testTag(CreateSerieScreenTestTags.BUTTON_SAVE_SERIE),
                  colors =
                      ButtonDefaults.buttonColors(
                          containerColor = DarkButtonColor, contentColor = ButtonSaveColor),
                  enabled = uiState.isValid && !uiState.isLoading) {
                    if (uiState.isLoading) {
                      CircularProgressIndicator(
                          modifier = Modifier.size(24.dp),
                          color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                      Text("Next")
                    }
                  }

              Spacer(modifier = Modifier.height(16.dp))
            }
      }
}

/**
 * Provides consistent colors for read-only (disabled) text fields.
 *
 * This function returns TextFieldColors configured to make disabled fields appear enabled,
 * maintaining visual consistency across read-only input fields like date pickers and number
 * selectors.
 *
 * @return TextFieldColors configured for read-only fields
 */
@Composable
private fun readOnlyTextFieldColors(): TextFieldColors {
  return OutlinedTextFieldDefaults.colors(
      disabledTextColor = LocalContentColor.current.copy(LocalContentColor.current.alpha),
      disabledBorderColor = MaterialTheme.colorScheme.outline,
      disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
      disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant)
}
