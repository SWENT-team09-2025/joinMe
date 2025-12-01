package com.android.joinme.ui.overview

import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

/** Note: this file was co-written with AI (Claude). */

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

  /** Test tag for the group dropdown field */
  const val INPUT_SERIE_GROUP = "inputSerieGroup"

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
@Composable
fun CreateSerieScreen(
    createSerieViewModel: CreateSerieViewModel = viewModel(),
    onGoBack: () -> Unit = {},
    onDone: (String) -> Unit = {}
) {
  val uiState by createSerieViewModel.uiState.collectAsState()
  val errorMsg = uiState.errorMsg
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()

  // Load user groups when screen is displayed
  LaunchedEffect(Unit) { createSerieViewModel.loadUserGroups() }

  LaunchedEffect(errorMsg) {
    if (errorMsg != null) {
      Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
      createSerieViewModel.clearErrorMsg()
    }
  }

  val testTags =
      SerieFormTestTags(
          inputSerieTitle = CreateSerieScreenTestTags.INPUT_SERIE_TITLE,
          inputSerieDescription = CreateSerieScreenTestTags.INPUT_SERIE_DESCRIPTION,
          inputSerieMaxParticipants = CreateSerieScreenTestTags.INPUT_SERIE_MAX_PARTICIPANTS,
          inputSerieDate = CreateSerieScreenTestTags.INPUT_SERIE_DATE,
          inputSerieTime = CreateSerieScreenTestTags.INPUT_SERIE_TIME,
          inputSerieVisibility = CreateSerieScreenTestTags.INPUT_SERIE_VISIBILITY,
          buttonSaveSerie = CreateSerieScreenTestTags.BUTTON_SAVE_SERIE,
          errorMessage = CreateSerieScreenTestTags.ERROR_MESSAGE)

  val formState = createSerieFormState(uiState)

  SerieFormScreen(
      title = "Create Serie",
      formState = formState,
      testTags = testTags,
      selectedGroupId = uiState.selectedGroupId,
      availableGroups = uiState.availableGroups,
      groupTestTag = CreateSerieScreenTestTags.INPUT_SERIE_GROUP,
      onGroupChange = { createSerieViewModel.setSelectedGroup(it) },
      onTitleChange = { createSerieViewModel.setTitle(it) },
      onDescriptionChange = { createSerieViewModel.setDescription(it) },
      onMaxParticipantsChange = { createSerieViewModel.setMaxParticipants(it) },
      onDateChange = { createSerieViewModel.setDate(it) },
      onTimeChange = { createSerieViewModel.setTime(it) },
      onVisibilityChange = { createSerieViewModel.setVisibility(it) },
      onSave = {
        coroutineScope.launch {
          val serieId = createSerieViewModel.createSerie()
          if (serieId != null) {
            onDone(serieId)
          }
        }
        true
      },
      onGoBack = {
        coroutineScope.launch {
          createSerieViewModel.deleteCreatedSerieIfExists()
          onGoBack()
        }
      },
      saveButtonText = "NEXT")
}
