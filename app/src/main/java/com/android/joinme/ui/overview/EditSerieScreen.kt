package com.android.joinme.ui.overview

import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

/**
 * Test tags for UI testing of the Edit Serie screen components.
 *
 * Provides consistent identifiers for testing individual UI elements including input fields,
 * buttons, and error messages.
 */
object EditSerieScreenTestTags {
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

  /** Test tag for the save button */
  const val SERIE_SAVE = "serieSave"

  /** Test tag for error messages displayed via Toast */
  const val ERROR_MESSAGE = "errorMessage"
}

/**
 * Screen for editing an existing event serie.
 *
 * Loads the serie data on mount and provides a form to edit the serie with title, description, max
 * participants, date, time, and visibility fields. Uses native dialogs for date/time/number
 * selection. Real-time validation displays errors below each field. The "Save" button is enabled
 * only when all fields are valid.
 *
 * @param serieId The ID of the serie to edit
 * @param editSerieViewModel ViewModel managing the screen state and business logic
 * @param onDone Callback invoked when the serie is successfully updated
 * @param onGoBack Callback invoked when the back button is pressed
 */
@Composable
fun EditSerieScreen(
    serieId: String,
    editSerieViewModel: EditSerieViewModel = viewModel(),
    onDone: () -> Unit = {},
    onGoBack: () -> Unit = {}
) {
  LaunchedEffect(serieId) { editSerieViewModel.loadSerie(serieId) }

  val uiState by editSerieViewModel.uiState.collectAsState()
  val errorMsg = uiState.errorMsg

  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()

  LaunchedEffect(errorMsg) {
    if (errorMsg != null) {
      Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
      editSerieViewModel.clearErrorMsg()
    }
  }

  val testTags =
      SerieFormTestTags(
          inputSerieTitle = EditSerieScreenTestTags.INPUT_SERIE_TITLE,
          inputSerieDescription = EditSerieScreenTestTags.INPUT_SERIE_DESCRIPTION,
          inputSerieMaxParticipants = EditSerieScreenTestTags.INPUT_SERIE_MAX_PARTICIPANTS,
          inputSerieDate = EditSerieScreenTestTags.INPUT_SERIE_DATE,
          inputSerieTime = EditSerieScreenTestTags.INPUT_SERIE_TIME,
          inputSerieVisibility = EditSerieScreenTestTags.INPUT_SERIE_VISIBILITY,
          buttonSaveSerie = EditSerieScreenTestTags.SERIE_SAVE,
          errorMessage = EditSerieScreenTestTags.ERROR_MESSAGE)

  val formState = createSerieFormState(uiState)

  SerieFormScreen(
      title = "Edit Serie",
      formState = formState,
      testTags = testTags,
      selectedGroupId = if (uiState.isGroupSerie) uiState.groupId else null,
      onTitleChange = { editSerieViewModel.setTitle(it) },
      onDescriptionChange = { editSerieViewModel.setDescription(it) },
      onMaxParticipantsChange = { editSerieViewModel.setMaxParticipants(it) },
      onDateChange = { editSerieViewModel.setDate(it) },
      onTimeChange = { editSerieViewModel.setTime(it) },
      onVisibilityChange = { editSerieViewModel.setVisibility(it) },
      onSave = {
        coroutineScope.launch {
          if (editSerieViewModel.updateSerie()) {
            onDone()
          }
        }
        true
      },
      onGoBack = onGoBack,
      saveButtonText = "SAVE")
}
