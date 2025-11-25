package com.android.joinme.ui.overview

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.android.joinme.model.map.Location
import com.android.joinme.ui.theme.Dimens
import com.android.joinme.ui.theme.buttonColors
import com.android.joinme.ui.theme.customColors
import kotlinx.coroutines.launch

/** Note: This file was created with the help of IA (Claude) */

/** Data class representing the test tags for event for serie form fields. */
data class EventForSerieFormTestTags(
    val inputEventType: String,
    val inputEventTitle: String,
    val inputEventDescription: String,
    val inputEventLocation: String,
    val inputEventLocationSuggestions: String,
    val forEachInputEventLocationSuggestion: String,
    val inputEventDuration: String,
    val buttonSaveEvent: String,
    val errorMessage: String
)

/**
 * Generic event for serie form screen component used by both Create and Edit event for serie
 * screens.
 *
 * @param title The title to display in the top bar
 * @param formState The current state of the form
 * @param isFormValid Whether the form is valid and can be submitted
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
 * @param saveButtonText Text to display on the save button (default: "SAVE")
 */
@SuppressLint("DefaultLocale")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventForSerieFormScreen(
    title: String,
    formState: EventForSerieFormUIState,
    isFormValid: Boolean,
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
    saveButtonText: String = "SAVE"
) {
  // Trigger location search when query changes
  LaunchedEffect(formState.locationQuery) {
    if (formState.locationQuery.isNotBlank()) {
      onSearchLocations(formState.locationQuery)
    }
  }
  val eventFormTestTags =
      EventFormTestTags(
          inputEventType = testTags.inputEventType,
          inputEventTitle = testTags.inputEventTitle,
          inputEventDescription = testTags.inputEventDescription,
          inputEventLocation = testTags.inputEventLocation,
          inputEventLocationSuggestions = testTags.inputEventLocationSuggestions,
          forEachInputEventLocationSuggestion = testTags.inputEventLocationSuggestions,
          inputEventMaxParticipants = "",
          inputEventDuration = testTags.inputEventDuration,
          inputEventDate = "",
          inputEventTime = "",
          inputEventVisibility = "",
          buttonSaveEvent = testTags.buttonSaveEvent,
          errorMessage = testTags.errorMessage)

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
              color = MaterialTheme.colorScheme.primary, thickness = Dimens.BorderWidth.thin)
        }
      }) { paddingValues ->
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(Dimens.Padding.medium)
                    .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(Dimens.Spacing.small)) {
              // Type field - show text if serie has a group, dropdown otherwise
              if (formState.serieHasGroup) {
                Column(modifier = Modifier.fillMaxWidth()) {
                  Text(
                      text = "Event Type (from Group)",
                      style = MaterialTheme.typography.labelMedium,
                      color = MaterialTheme.colorScheme.onSurfaceVariant)
                  Spacer(modifier = Modifier.height(Dimens.Spacing.extraSmall))
                  Text(
                      text = formState.type,
                      style = MaterialTheme.typography.bodyLarge,
                      modifier = Modifier.testTag(testTags.inputEventType))
                }
              } else {
                EventTypeField(
                    value = formState.type,
                    onValueChange = onTypeChange,
                    isError = formState.invalidTypeMsg != null,
                    errorMessage = formState.invalidTypeMsg,
                    testTag = testTags.inputEventType)
              }

              // Title
              EventTitleField(
                  value = formState.title,
                  onValueChange = onTitleChange,
                  isError = formState.invalidTitleMsg != null,
                  errorMessage = formState.invalidTitleMsg,
                  testTag = testTags.inputEventTitle,
                  errorTestTag = testTags.errorMessage)

              // Description
              EventDescriptionField(
                  value = formState.description,
                  onValueChange = onDescriptionChange,
                  isError = formState.invalidDescriptionMsg != null,
                  errorMessage = formState.invalidDescriptionMsg,
                  testTag = testTags.inputEventDescription,
                  errorTestTag = testTags.errorMessage)

              // Duration picker
              EventDurationField(
                  durationValue = formState.duration,
                  onValueChange = onDurationChange,
                  isError = formState.invalidDurationMsg != null,
                  errorMessage = formState.invalidDurationMsg,
                  testTag = testTags.inputEventDuration)

              // Location field with search functionality

              LocationField(
                  query = formState.locationQuery,
                  suggestions = formState.locationSuggestions,
                  isError = formState.invalidLocationMsg != null,
                  supportingText = formState.invalidLocationMsg,
                  onQueryChange = onLocationQueryChange,
                  onSuggestionSelected = onLocationSelected,
                  testTags = eventFormTestTags)

              Spacer(modifier = Modifier.height(Dimens.Spacing.medium))

              // Save button
              Button(
                  onClick = {
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                      onSave()
                    }
                  },
                  modifier = Modifier.fillMaxWidth().testTag(testTags.buttonSaveEvent),
                  enabled = isFormValid,
                  colors = MaterialTheme.customColors.buttonColors()) {
                    Text(saveButtonText)
                  }
            }
      }
}
