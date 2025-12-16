package com.android.joinme.ui.overview

import android.widget.NumberPicker
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import com.android.joinme.R
import com.android.joinme.model.event.EventType
import com.android.joinme.ui.theme.Dimens
import com.android.joinme.ui.theme.customColors
import com.android.joinme.ui.theme.outlinedTextField
import java.util.Locale

/** Note: This file was created with the help of IA (Claude) */

/**
 * Reusable event type dropdown field.
 *
 * @param value The current type value
 * @param onValueChange Callback when type changes
 * @param isError Whether the field has a validation error
 * @param errorMessage The error message to display
 * @param testTag The test tag for this field
 * @param modifier Optional modifier for the field
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventTypeField(
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean,
    errorMessage: String?,
    testTag: String,
    modifier: Modifier = Modifier
) {
  val eventTypes = EventType.values().map { it.name.uppercase(Locale.ROOT) }
  var showDropdown by remember { mutableStateOf(false) }

  ExposedDropdownMenuBox(
      expanded = showDropdown, onExpandedChange = { showDropdown = !showDropdown }) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.event_type)) },
            placeholder = { Text(stringResource(R.string.select_event_type)) },
            isError = isError,
            supportingText = {
              errorMessage?.let { Text(text = it, color = MaterialTheme.colorScheme.error) }
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showDropdown) },
            modifier = modifier.menuAnchor().fillMaxWidth().testTag(testTag))
        ExposedDropdownMenu(
            expanded = showDropdown,
            onDismissRequest = { showDropdown = false },
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
                      onValueChange(type)
                      showDropdown = false
                    },
                    colors = MaterialTheme.customColors.dropdownMenu)
                if (index < eventTypes.lastIndex) {
                  HorizontalDivider(thickness = Dimens.BorderWidth.thin)
                }
              }
            }
      }
}

/**
 * Reusable event title text field.
 *
 * @param value The current title value
 * @param onValueChange Callback when title changes
 * @param isError Whether the field has a validation error
 * @param errorMessage The error message to display
 * @param testTag The test tag for this field
 * @param errorTestTag The test tag for the error message
 * @param modifier Optional modifier for the field
 */
@Composable
fun EventTitleField(
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean,
    errorMessage: String?,
    testTag: String,
    errorTestTag: String,
    modifier: Modifier = Modifier
) {
  OutlinedTextField(
      value = value,
      onValueChange = onValueChange,
      label = { Text(stringResource(R.string.title)) },
      placeholder = { Text(stringResource(R.string.enter_event_title)) },
      isError = isError,
      supportingText = {
        errorMessage?.let {
          Text(
              it,
              color = MaterialTheme.colorScheme.error,
              modifier = Modifier.testTag(errorTestTag))
        }
      },
      modifier = modifier.fillMaxWidth().testTag(testTag))
}

/**
 * Reusable event description text field.
 *
 * @param value The current description value
 * @param onValueChange Callback when description changes
 * @param isError Whether the field has a validation error
 * @param errorMessage The error message to display
 * @param testTag The test tag for this field
 * @param errorTestTag The test tag for the error message
 * @param modifier Optional modifier for the field
 */
@Composable
fun EventDescriptionField(
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean,
    errorMessage: String?,
    testTag: String,
    errorTestTag: String,
    modifier: Modifier = Modifier
) {
  OutlinedTextField(
      value = value,
      onValueChange = onValueChange,
      label = { Text(stringResource(R.string.description)) },
      placeholder = { Text(stringResource(R.string.describe_your_event)) },
      isError = isError,
      supportingText = {
        errorMessage?.let {
          Text(
              it,
              color = MaterialTheme.colorScheme.error,
              modifier = Modifier.testTag(errorTestTag))
        }
      },
      modifier =
          modifier.fillMaxWidth().height(Dimens.EventFormFields.descriptionField).testTag(testTag))
}

/**
 * Reusable event duration picker field.
 *
 * Opens a dialog with a NumberPicker to select duration in minutes.
 *
 * @param value The current duration value as a string
 * @param onValueChange Callback when duration changes
 * @param isError Whether the field has a validation error
 * @param errorMessage The error message to display
 * @param testTag The test tag for this field
 * @param modifier Optional modifier for the field
 */
@Composable
fun EventDurationField(
    durationValue: String,
    onValueChange: (String) -> Unit,
    isError: Boolean,
    errorMessage: String?,
    testTag: String,
    modifier: Modifier = Modifier
) {
  var showDialog by remember { mutableStateOf(false) }
  var tempDuration by remember { mutableIntStateOf(durationValue.toIntOrNull() ?: 10) }

  // Update tempDuration when durationValue changes
  LaunchedEffect(durationValue) { durationValue.toIntOrNull()?.let { tempDuration = it } }

  Box(modifier = modifier.fillMaxWidth().clickable { showDialog = true }) {
    OutlinedTextField(
        value = durationValue,
        onValueChange = {},
        readOnly = true,
        label = { Text(stringResource(R.string.duration_min)) },
        placeholder = { Text(stringResource(R.string.select_duration)) },
        isError = isError,
        supportingText = {
          errorMessage?.let { Text(text = it, color = MaterialTheme.colorScheme.error) }
        },
        colors = MaterialTheme.customColors.outlinedTextField(),
        enabled = false,
        modifier = Modifier.fillMaxWidth().testTag(testTag))
  }

  if (showDialog) {
    AlertDialog(
        onDismissRequest = { showDialog = false },
        title = { Text(stringResource(R.string.select_duration_min)) },
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
                onValueChange(tempDuration.toString())
                showDialog = false
              }) {
                Text(stringResource(R.string.ok))
              }
        },
        dismissButton = {
          TextButton(onClick = { showDialog = false }) { Text(stringResource(R.string.cancel)) }
        })
  }
}
