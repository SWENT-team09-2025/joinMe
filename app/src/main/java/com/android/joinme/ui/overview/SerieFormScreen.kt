package com.android.joinme.ui.overview

import android.annotation.SuppressLint
import android.widget.NumberPicker
import androidx.compose.foundation.background
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
import androidx.compose.ui.viewinterop.AndroidView
import com.android.joinme.ui.theme.Dimens
import com.android.joinme.ui.theme.buttonColors
import com.android.joinme.ui.theme.customColors
import com.android.joinme.ui.theme.outlinedTextField
import java.util.*

/** Data class representing the test tags for serie form fields. */
data class SerieFormTestTags(
    val inputSerieTitle: String,
    val inputSerieDescription: String,
    val inputSerieMaxParticipants: String,
    val inputSerieDate: String,
    val inputSerieTime: String,
    val inputSerieVisibility: String,
    val buttonSaveSerie: String,
    val errorMessage: String
)

/** Data class representing the state of the serie form. */
data class SerieFormState(
    val title: String,
    val description: String,
    val maxParticipants: String,
    val date: String,
    val time: String,
    val visibility: String,
    val isValid: Boolean,
    val isLoading: Boolean,
    val invalidTitleMsg: String?,
    val invalidDescriptionMsg: String?,
    val invalidMaxParticipantsMsg: String?,
    val invalidDateMsg: String?,
    val invalidTimeMsg: String?,
    val invalidVisibilityMsg: String?
)

/**
 * Creates a SerieFormState from CreateSerieUIState.
 *
 * This helper function reduces code duplication between CreateSerieScreen and EditSerieScreen by
 * centralizing the mapping logic from UI state to form state.
 *
 * @param uiState The UI state from CreateSerieViewModel
 * @return A SerieFormState instance populated with the UI state values
 */
fun createSerieFormState(uiState: CreateSerieUIState): SerieFormState {
  return SerieFormState(
      title = uiState.title,
      description = uiState.description,
      maxParticipants = uiState.maxParticipants,
      date = uiState.date,
      time = uiState.time,
      visibility = uiState.visibility,
      isValid = uiState.isValid,
      isLoading = uiState.isLoading,
      invalidTitleMsg = uiState.invalidTitleMsg,
      invalidDescriptionMsg = uiState.invalidDescriptionMsg,
      invalidMaxParticipantsMsg = uiState.invalidMaxParticipantsMsg,
      invalidDateMsg = uiState.invalidDateMsg,
      invalidTimeMsg = uiState.invalidTimeMsg,
      invalidVisibilityMsg = uiState.invalidVisibilityMsg)
}

/**
 * Creates a SerieFormState from EditSerieUIState.
 *
 * This helper function reduces code duplication between CreateSerieScreen and EditSerieScreen by
 * centralizing the mapping logic from UI state to form state.
 *
 * @param uiState The UI state from EditSerieViewModel
 * @return A SerieFormState instance populated with the UI state values
 */
fun createSerieFormState(uiState: EditSerieUIState): SerieFormState {
  return SerieFormState(
      title = uiState.title,
      description = uiState.description,
      maxParticipants = uiState.maxParticipants,
      date = uiState.date,
      time = uiState.time,
      visibility = uiState.visibility,
      isValid = uiState.isValid,
      isLoading = uiState.isLoading,
      invalidTitleMsg = uiState.invalidTitleMsg,
      invalidDescriptionMsg = uiState.invalidDescriptionMsg,
      invalidMaxParticipantsMsg = uiState.invalidMaxParticipantsMsg,
      invalidDateMsg = uiState.invalidDateMsg,
      invalidTimeMsg = uiState.invalidTimeMsg,
      invalidVisibilityMsg = uiState.invalidVisibilityMsg)
}

/**
 * Generic serie form screen component used by both Create and Edit serie screens.
 *
 * Provides a form to manage a serie with title, description, max participants, date, time, and
 * visibility fields. Uses native dialogs for date/time/number selection. Real-time validation
 * displays errors below each field.
 *
 * @param title The title to display in the top bar
 * @param formState The current state of the form
 * @param testTags Test tags for UI testing
 * @param onTitleChange Callback when title changes
 * @param onDescriptionChange Callback when description changes
 * @param onMaxParticipantsChange Callback when max participants changes
 * @param onDateChange Callback when date changes
 * @param onTimeChange Callback when time changes
 * @param onVisibilityChange Callback when visibility changes
 * @param onSave Callback when save button is clicked
 * @param onGoBack Callback when back button is clicked
 * @param saveButtonText Text to display on the save button (default: "Next")
 */
@SuppressLint("DefaultLocale")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SerieFormScreen(
    title: String,
    formState: SerieFormState,
    testTags: SerieFormTestTags,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onMaxParticipantsChange: (String) -> Unit,
    onDateChange: (String) -> Unit,
    onTimeChange: (String) -> Unit,
    onVisibilityChange: (String) -> Unit,
    onSave: () -> Boolean,
    onGoBack: () -> Unit,
    saveButtonText: String = "Next"
) {
  Scaffold(
      topBar = {
        Column {
          CenterAlignedTopAppBar(
              title = { Text(title, style = MaterialTheme.typography.titleLarge) },
              navigationIcon = {
                IconButton(onClick = onGoBack) {
                  Icon(
                      imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                      contentDescription = "Back")
                }
              })
          HorizontalDivider(
              thickness = Dimens.BorderWidth.thin, color = MaterialTheme.colorScheme.primary)
        }
      }) { paddingValues ->
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = Dimens.Padding.medium)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Dimens.Padding.medium)) {
              Spacer(modifier = Modifier.height(Dimens.Padding.small))

              // Title field
              OutlinedTextField(
                  value = formState.title,
                  onValueChange = onTitleChange,
                  label = { Text("Title") },
                  modifier = Modifier.fillMaxWidth().testTag(testTags.inputSerieTitle),
                  isError = formState.invalidTitleMsg != null,
                  supportingText = {
                    if (formState.invalidTitleMsg != null) {
                      Text(
                          text = formState.invalidTitleMsg,
                          color = MaterialTheme.colorScheme.error,
                          modifier = Modifier.testTag(testTags.errorMessage))
                    }
                  },
                  singleLine = true)

              // Description field
              OutlinedTextField(
                  value = formState.description,
                  onValueChange = onDescriptionChange,
                  label = { Text("Description") },
                  modifier =
                      Modifier.fillMaxWidth()
                          .height(Dimens.SerieForm.descriptionField)
                          .testTag(testTags.inputSerieDescription),
                  isError = formState.invalidDescriptionMsg != null,
                  supportingText = {
                    if (formState.invalidDescriptionMsg != null) {
                      Text(
                          text = formState.invalidDescriptionMsg,
                          color = MaterialTheme.colorScheme.error,
                          modifier = Modifier.testTag(testTags.errorMessage))
                    }
                  },
                  maxLines = 4)

              // Max Participants field with NumberPicker
              var showMaxParticipantsPicker by remember { mutableStateOf(false) }
              var tempParticipants by remember {
                mutableIntStateOf(formState.maxParticipants.toIntOrNull() ?: 10)
              }

              Box(
                  modifier =
                      Modifier.width(Dimens.SerieForm.maxParticipantsField).clickable {
                        showMaxParticipantsPicker = true
                      }) {
                    OutlinedTextField(
                        value = formState.maxParticipants,
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        label = { Text("Max Participants") },
                        placeholder = { Text("Select number") },
                        modifier =
                            Modifier.fillMaxWidth().testTag(testTags.inputSerieMaxParticipants),
                        isError = formState.invalidMaxParticipantsMsg != null,
                        supportingText = {
                          if (formState.invalidMaxParticipantsMsg != null) {
                            Text(
                                text = formState.invalidMaxParticipantsMsg,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.testTag(testTags.errorMessage))
                          }
                        },
                        colors = MaterialTheme.customColors.outlinedTextField(),
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
                            onMaxParticipantsChange(tempParticipants.toString())
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
                  horizontalArrangement = Arrangement.spacedBy(Dimens.Padding.medium)) {
                    val context = LocalContext.current
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
                            onDateChange(newDate)
                          },
                          year,
                          month,
                          day)
                    }

                    // Date field
                    Box(modifier = Modifier.weight(1f).clickable { datePickerDialog.show() }) {
                      OutlinedTextField(
                          value = formState.date,
                          onValueChange = {},
                          readOnly = true,
                          enabled = false,
                          label = { Text("Date") },
                          placeholder = { Text("Select date") },
                          modifier = Modifier.fillMaxWidth().testTag(testTags.inputSerieDate),
                          isError = formState.invalidDateMsg != null,
                          supportingText = {
                            if (formState.invalidDateMsg != null) {
                              Text(
                                  text = formState.invalidDateMsg,
                                  color = MaterialTheme.colorScheme.error,
                                  modifier = Modifier.testTag(testTags.errorMessage))
                            }
                          },
                          colors = MaterialTheme.customColors.outlinedTextField(),
                          singleLine = true)
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

                    // Time field
                    Box(modifier = Modifier.weight(1f).clickable { timePickerDialog.show() }) {
                      OutlinedTextField(
                          value = formState.time,
                          onValueChange = {},
                          readOnly = true,
                          enabled = false,
                          label = { Text("Time") },
                          placeholder = { Text("Select time") },
                          modifier = Modifier.fillMaxWidth().testTag(testTags.inputSerieTime),
                          isError = formState.invalidTimeMsg != null,
                          supportingText = {
                            if (formState.invalidTimeMsg != null) {
                              Text(
                                  text = formState.invalidTimeMsg,
                                  color = MaterialTheme.colorScheme.error,
                                  modifier = Modifier.testTag(testTags.errorMessage))
                            }
                          },
                          colors = MaterialTheme.customColors.outlinedTextField(),
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
                        value = formState.visibility,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Visibility") },
                        trailingIcon = {
                          ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedVisibility)
                        },
                        modifier =
                            Modifier.fillMaxWidth()
                                .menuAnchor()
                                .testTag(testTags.inputSerieVisibility),
                        isError = formState.invalidVisibilityMsg != null,
                        supportingText = {
                          if (formState.invalidVisibilityMsg != null) {
                            Text(
                                text = formState.invalidVisibilityMsg,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.testTag(testTags.errorMessage))
                          }
                        },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors())

                    ExposedDropdownMenu(
                        expanded = expandedVisibility,
                        onDismissRequest = { expandedVisibility = false },
                        modifier = Modifier.background(MaterialTheme.customColors.backgroundMenu)) {
                          visibilityOptions.forEachIndexed { index, option ->
                            DropdownMenuItem(
                                text = {
                                  Text(
                                      text = option,
                                      color = MaterialTheme.colorScheme.onPrimaryContainer,
                                      style = MaterialTheme.typography.headlineSmall)
                                },
                                onClick = {
                                  onVisibilityChange(option)
                                  expandedVisibility = false
                                },
                                colors = MaterialTheme.customColors.dropdownMenu)
                            if (index < visibilityOptions.lastIndex) {
                              HorizontalDivider(thickness = Dimens.BorderWidth.thin)
                            }
                          }
                        }
                  }

              Spacer(modifier = Modifier.height(Dimens.Padding.medium))

              // Save button
              Button(
                  onClick = { onSave() },
                  modifier =
                      Modifier.fillMaxWidth()
                          .height(Dimens.Button.standardHeight)
                          .testTag(testTags.buttonSaveSerie),
                  colors = MaterialTheme.customColors.buttonColors(),
                  enabled = formState.isValid && !formState.isLoading) {
                    if (formState.isLoading) {
                      CircularProgressIndicator(
                          modifier = Modifier.size(Dimens.IconSize.medium),
                          color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                      Text(saveButtonText)
                    }
                  }

              Spacer(modifier = Modifier.height(Dimens.Spacing.medium))
            }
      }
}
