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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import com.android.joinme.R
import com.android.joinme.model.groups.Group
import com.android.joinme.ui.theme.Dimens
import com.android.joinme.ui.theme.buttonColors
import com.android.joinme.ui.theme.customColors
import com.android.joinme.ui.theme.outlinedTextField
import java.util.*

/** Note: This file was co-written with AI (Claude). */

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
 * @param selectedGroupId The ID of the selected group (null for standalone series)
 * @param availableGroups List of available groups to select from
 * @param groupTestTag Test tag for the group dropdown
 * @param onGroupChange Callback when group selection changes (null for no group)
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
    selectedGroupId: String? = null,
    availableGroups: List<Group> = emptyList(),
    groupTestTag: String = "",
    onGroupChange: ((String?) -> Unit)? = null,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onMaxParticipantsChange: (String) -> Unit,
    onDateChange: (String) -> Unit,
    onTimeChange: (String) -> Unit,
    onVisibilityChange: (String) -> Unit,
    onSave: () -> Boolean,
    onGoBack: () -> Unit,
    saveButtonText: String = "NEXT"
) {
  Scaffold(topBar = { SerieFormTopBar(title = title, onGoBack = onGoBack) }) { paddingValues ->
    SerieFormContent(
        paddingValues = paddingValues,
        formState = formState,
        testTags = testTags,
        selectedGroupId = selectedGroupId,
        availableGroups = availableGroups,
        groupTestTag = groupTestTag,
        onGroupChange = onGroupChange,
        onTitleChange = onTitleChange,
        onDescriptionChange = onDescriptionChange,
        onMaxParticipantsChange = onMaxParticipantsChange,
        onDateChange = onDateChange,
        onTimeChange = onTimeChange,
        onVisibilityChange = onVisibilityChange,
        onSave = onSave,
        saveButtonText = saveButtonText)
  }
}

/** Top bar for serie form screen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SerieFormTopBar(title: String, onGoBack: () -> Unit) {
  Column {
    CenterAlignedTopAppBar(
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        navigationIcon = {
          IconButton(onClick = onGoBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.back))
          }
        })
    HorizontalDivider(
        thickness = Dimens.BorderWidth.thin, color = MaterialTheme.colorScheme.primary)
  }
}

/** Form content composable. */
@Composable
private fun SerieFormContent(
    paddingValues: PaddingValues,
    formState: SerieFormState,
    testTags: SerieFormTestTags,
    selectedGroupId: String?,
    availableGroups: List<Group>,
    groupTestTag: String,
    onGroupChange: ((String?) -> Unit)?,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onMaxParticipantsChange: (String) -> Unit,
    onDateChange: (String) -> Unit,
    onTimeChange: (String) -> Unit,
    onVisibilityChange: (String) -> Unit,
    onSave: () -> Boolean,
    saveButtonText: String
) {
  Column(
      modifier =
          Modifier.fillMaxSize()
              .padding(paddingValues)
              .padding(horizontal = Dimens.Padding.medium)
              .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(Dimens.Padding.medium)) {
        Spacer(modifier = Modifier.height(Dimens.Padding.small))

        onGroupChange?.let {
          GroupDropdownField(
              selectedGroupId = selectedGroupId,
              availableGroups = availableGroups,
              groupTestTag = groupTestTag,
              onGroupChange = it)
        }

        TitleField(formState = formState, testTags = testTags, onTitleChange = onTitleChange)

        DescriptionField(
            formState = formState, testTags = testTags, onDescriptionChange = onDescriptionChange)

        MaxParticipantsField(
            formState = formState,
            testTags = testTags,
            onMaxParticipantsChange = onMaxParticipantsChange)

        DateTimePickerRow(
            formState = formState,
            testTags = testTags,
            onDateChange = onDateChange,
            onTimeChange = onTimeChange)

        if (selectedGroupId == null) {
          VisibilityDropdownField(
              formState = formState, testTags = testTags, onVisibilityChange = onVisibilityChange)
        }

        Spacer(modifier = Modifier.height(Dimens.Padding.medium))

        SaveButton(
            formState = formState,
            testTags = testTags,
            saveButtonText = saveButtonText,
            onSave = onSave)

        Spacer(modifier = Modifier.height(Dimens.Spacing.medium))
      }
}

/** Group dropdown field composable. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroupDropdownField(
    selectedGroupId: String?,
    availableGroups: List<Group>,
    groupTestTag: String,
    onGroupChange: (String?) -> Unit
) {
  var expandedGroup by remember { mutableStateOf(false) }
  val selectedGroupName =
      if (selectedGroupId == null) stringResource(R.string.none_standalone)
      else
          availableGroups.find { it.id == selectedGroupId }?.name
              ?: stringResource(R.string.unknown_group)

  ExposedDropdownMenuBox(
      expanded = expandedGroup, onExpandedChange = { expandedGroup = !expandedGroup }) {
        OutlinedTextField(
            value = selectedGroupName,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.group_optional)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedGroup) },
            modifier = Modifier.fillMaxWidth().menuAnchor().testTag(groupTestTag),
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors())

        ExposedDropdownMenu(
            expanded = expandedGroup,
            onDismissRequest = { expandedGroup = false },
            modifier = Modifier.background(MaterialTheme.customColors.backgroundMenu)) {
              // "None" option for standalone series
              DropdownMenuItem(
                  text = {
                    Text(
                        text = stringResource(R.string.none_standalone),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.headlineSmall)
                  },
                  onClick = {
                    onGroupChange(null)
                    expandedGroup = false
                  },
                  colors = MaterialTheme.customColors.dropdownMenu)

              // Group options (only show divider and groups if groups exist)
              if (availableGroups.isNotEmpty()) {
                HorizontalDivider(thickness = Dimens.BorderWidth.thin)

                availableGroups.forEachIndexed { index, group ->
                  DropdownMenuItem(
                      text = {
                        Text(
                            text = group.name,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            style = MaterialTheme.typography.headlineSmall)
                      },
                      onClick = {
                        onGroupChange(group.id)
                        expandedGroup = false
                      },
                      colors = MaterialTheme.customColors.dropdownMenu)
                  if (index < availableGroups.lastIndex) {
                    HorizontalDivider(thickness = Dimens.BorderWidth.thin)
                  }
                }
              }
            }
      }
}

/** Title field composable. */
@Composable
private fun TitleField(
    formState: SerieFormState,
    testTags: SerieFormTestTags,
    onTitleChange: (String) -> Unit
) {
  OutlinedTextField(
      value = formState.title,
      onValueChange = onTitleChange,
      label = { Text(stringResource(R.string.title)) },
      modifier = Modifier.fillMaxWidth().testTag(testTags.inputSerieTitle),
      isError = formState.invalidTitleMsg != null,
      supportingText = {
        ErrorText(errorMsg = formState.invalidTitleMsg, testTag = testTags.errorMessage)
      },
      singleLine = true)
}

/** Description field composable. */
@Composable
private fun DescriptionField(
    formState: SerieFormState,
    testTags: SerieFormTestTags,
    onDescriptionChange: (String) -> Unit
) {
  OutlinedTextField(
      value = formState.description,
      onValueChange = onDescriptionChange,
      label = { Text(stringResource(R.string.description)) },
      modifier =
          Modifier.fillMaxWidth()
              .height(Dimens.SerieForm.descriptionField)
              .testTag(testTags.inputSerieDescription),
      isError = formState.invalidDescriptionMsg != null,
      supportingText = {
        ErrorText(errorMsg = formState.invalidDescriptionMsg, testTag = testTags.errorMessage)
      },
      maxLines = 4)
}

/** Max Participants field with NumberPicker dialog. */
@Composable
private fun MaxParticipantsField(
    formState: SerieFormState,
    testTags: SerieFormTestTags,
    onMaxParticipantsChange: (String) -> Unit
) {
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
            label = { Text(stringResource(R.string.max_participants)) },
            placeholder = { Text(stringResource(R.string.select_number)) },
            modifier = Modifier.fillMaxWidth().testTag(testTags.inputSerieMaxParticipants),
            isError = formState.invalidMaxParticipantsMsg != null,
            supportingText = {
              ErrorText(
                  errorMsg = formState.invalidMaxParticipantsMsg, testTag = testTags.errorMessage)
            },
            colors = MaterialTheme.customColors.outlinedTextField(),
            singleLine = true)
      }

  if (showMaxParticipantsPicker) {
    MaxParticipantsDialog(
        tempParticipants = tempParticipants,
        onTempParticipantsChange = { tempParticipants = it },
        onConfirm = {
          onMaxParticipantsChange(tempParticipants.toString())
          showMaxParticipantsPicker = false
        },
        onDismiss = { showMaxParticipantsPicker = false })
  }
}

/** Date and Time picker fields row. */
@Composable
private fun DateTimePickerRow(
    formState: SerieFormState,
    testTags: SerieFormTestTags,
    onDateChange: (String) -> Unit,
    onTimeChange: (String) -> Unit
) {
  Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(Dimens.Padding.medium)) {
        DatePickerField(
            date = formState.date,
            invalidDateMsg = formState.invalidDateMsg,
            testTag = testTags.inputSerieDate,
            errorTestTag = testTags.errorMessage,
            onDateChange = onDateChange,
            modifier = Modifier.weight(1f))

        TimePickerField(
            time = formState.time,
            invalidTimeMsg = formState.invalidTimeMsg,
            testTag = testTags.inputSerieTime,
            errorTestTag = testTags.errorMessage,
            onTimeChange = onTimeChange,
            modifier = Modifier.weight(1f))
      }
}

/** Visibility dropdown field. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VisibilityDropdownField(
    formState: SerieFormState,
    testTags: SerieFormTestTags,
    onVisibilityChange: (String) -> Unit
) {
  val visibilityOptions = listOf("PUBLIC", "PRIVATE")
  var expandedVisibility by remember { mutableStateOf(false) }

  ExposedDropdownMenuBox(
      expanded = expandedVisibility,
      onExpandedChange = { expandedVisibility = !expandedVisibility }) {
        OutlinedTextField(
            value = formState.visibility,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.visibility)) },
            trailingIcon = {
              ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedVisibility)
            },
            modifier = Modifier.fillMaxWidth().menuAnchor().testTag(testTags.inputSerieVisibility),
            isError = formState.invalidVisibilityMsg != null,
            supportingText = {
              ErrorText(errorMsg = formState.invalidVisibilityMsg, testTag = testTags.errorMessage)
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
}

/** Save button composable. */
@Composable
private fun SaveButton(
    formState: SerieFormState,
    testTags: SerieFormTestTags,
    saveButtonText: String,
    onSave: () -> Boolean
) {
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
}

/** Error text composable to reduce duplication. */
@Composable
private fun ErrorText(errorMsg: String?, testTag: String) {
  errorMsg?.let {
    Text(text = it, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag(testTag))
  }
}

/** Max Participants picker dialog. */
@Composable
private fun MaxParticipantsDialog(
    tempParticipants: Int,
    onTempParticipantsChange: (Int) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
  AlertDialog(
      onDismissRequest = onDismiss,
      title = { Text(stringResource(R.string.select_max_participants)) },
      text = {
        AndroidView(
            factory = { context ->
              NumberPicker(context).apply {
                minValue = 1
                maxValue = 100
                value = tempParticipants
                wrapSelectorWheel = true
                setOnValueChangedListener { _, _, newVal -> onTempParticipantsChange(newVal) }
              }
            },
            update = { picker -> picker.value = tempParticipants },
            modifier = Modifier.fillMaxWidth())
      },
      confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.ok)) } },
      dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } })
}

/** Date picker field. */
@SuppressLint("DefaultLocale")
@Composable
private fun DatePickerField(
    date: String,
    invalidDateMsg: String?,
    testTag: String,
    errorTestTag: String,
    onDateChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val calendar = remember { Calendar.getInstance() }
  val datePickerDialog = remember {
    android.app.DatePickerDialog(
        context,
        { _, selectedYear, selectedMonth, selectedDay ->
          val newDate =
              String.format("%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear)
          onDateChange(newDate)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH))
  }

  Box(modifier = modifier.clickable { datePickerDialog.show() }) {
    OutlinedTextField(
        value = date,
        onValueChange = {},
        readOnly = true,
        enabled = false,
        label = { Text(stringResource(R.string.date)) },
        placeholder = { Text(stringResource(R.string.select_date)) },
        modifier = Modifier.fillMaxWidth().testTag(testTag),
        isError = invalidDateMsg != null,
        supportingText = { ErrorText(errorMsg = invalidDateMsg, testTag = errorTestTag) },
        colors = MaterialTheme.customColors.outlinedTextField(),
        singleLine = true)
  }
}

/** Time picker field. */
@Composable
private fun TimePickerField(
    time: String,
    invalidTimeMsg: String?,
    testTag: String,
    errorTestTag: String,
    onTimeChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val calendar = remember { Calendar.getInstance() }
  val timePickerDialog = remember {
    android.app.TimePickerDialog(
        context,
        { _, selectedHour, selectedMinute ->
          val newTime = String.format("%02d:%02d", selectedHour, selectedMinute)
          onTimeChange(newTime)
        },
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
        true)
  }

  Box(modifier = modifier.clickable { timePickerDialog.show() }) {
    OutlinedTextField(
        value = time,
        onValueChange = {},
        readOnly = true,
        enabled = false,
        label = { Text(stringResource(R.string.time)) },
        placeholder = { Text(stringResource(R.string.select_time)) },
        modifier = Modifier.fillMaxWidth().testTag(testTag),
        isError = invalidTimeMsg != null,
        supportingText = { ErrorText(errorMsg = invalidTimeMsg, testTag = errorTestTag) },
        colors = MaterialTheme.customColors.outlinedTextField(),
        singleLine = true)
  }
}
