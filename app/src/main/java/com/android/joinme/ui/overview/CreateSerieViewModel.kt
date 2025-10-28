package com.android.joinme.ui.overview

import android.util.Log
import androidx.lifecycle.ViewModel
import com.android.joinme.model.serie.Serie
import com.android.joinme.model.serie.SeriesRepository
import com.android.joinme.model.serie.SeriesRepositoryProvider
import com.android.joinme.model.utils.Visibility
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * UI state for the CreateSerie screen.
 *
 * Holds all form field values and their corresponding validation messages for creating a new serie.
 *
 * @property title The title of the serie
 * @property description The description of the serie
 * @property maxParticipants String representation of the maximum number of participants
 * @property date The date in dd/MM/yyyy format
 * @property time The time in HH:mm format
 * @property visibility The visibility setting (PUBLIC or PRIVATE)
 * @property isLoading Indicates whether the serie is currently being created
 * @property errorMsg Global error message for the form
 * @property invalidTitleMsg Validation message for the title field
 * @property invalidDescriptionMsg Validation message for the description field
 * @property invalidMaxParticipantsMsg Validation message for the max participants field
 * @property invalidDateMsg Validation message for the date field
 * @property invalidTimeMsg Validation message for the time field
 * @property invalidVisibilityMsg Validation message for the visibility field
 */
data class CreateSerieUIState(
    val title: String = "",
    val description: String = "",
    val maxParticipants: String = "",
    val date: String = "",
    val time: String = "",
    val visibility: String = "",
    val isLoading: Boolean = false,
    val errorMsg: String? = null,

    // validation messages
    val invalidTitleMsg: String? = null,
    val invalidDescriptionMsg: String? = null,
    val invalidMaxParticipantsMsg: String? = null,
    val invalidDateMsg: String? = null,
    val invalidTimeMsg: String? = null,
    val invalidVisibilityMsg: String? = null,
) {
  /**
   * Checks if all form fields are valid and filled.
   *
   * @return True if all validation messages are null and all fields are not blank
   */
  val isValid: Boolean
    get() =
        invalidTitleMsg == null &&
            invalidDescriptionMsg == null &&
            invalidMaxParticipantsMsg == null &&
            invalidDateMsg == null &&
            invalidTimeMsg == null &&
            invalidVisibilityMsg == null &&
            title.isNotBlank() &&
            description.isNotBlank() &&
            maxParticipants.isNotBlank() &&
            date.isNotBlank() &&
            time.isNotBlank() &&
            visibility.isNotBlank()
}

/**
 * ViewModel for the CreateSerie screen.
 *
 * Manages the UI state and business logic for creating a new serie. Handles form validation,
 * date/time parsing, and repository interactions for saving series.
 *
 * @property repository The SeriesRepository used for data operations
 */
class CreateSerieViewModel(
    private val repository: SeriesRepository = SeriesRepositoryProvider.repository
) : ViewModel() {

  private val _uiState = MutableStateFlow(CreateSerieUIState())
  val uiState: StateFlow<CreateSerieUIState> = _uiState.asStateFlow()

  /** Clears the global error message from the UI state. */
  fun clearErrorMsg() {
    _uiState.value = _uiState.value.copy(errorMsg = null)
  }

  /**
   * Sets a global error message in the UI state.
   *
   * @param msg The error message to display
   */
  private fun setErrorMsg(msg: String) {
    _uiState.value = _uiState.value.copy(errorMsg = msg)
  }

  /**
   * Creates a new serie and adds it to the repository.
   *
   * Sets the loading state to true at the start and false upon completion. Validates all form
   * fields, parses the date and time, creates a Serie object with a unique ID and the current user
   * as owner, then saves it to the repository. If any error occurs during the process, the loading
   * state is reset and an error message is set.
   *
   * @return True if the serie was created successfully, false otherwise
   */
  suspend fun createSerie(): Boolean {
    val state = _uiState.value
    if (!state.isValid) {
      setErrorMsg("At least one field is not valid")
      return false
    }

    _uiState.value = _uiState.value.copy(isLoading = true)

    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val combinedDateTime = "${state.date} ${state.time}"
    val parsedDate =
        try {
          Timestamp(sdf.parse(combinedDateTime)!!)
        } catch (_: Exception) {
          null
        }

    if (parsedDate == null) {
      setErrorMsg("Invalid date format (must be dd/MM/yyyy HH:mm)")
      _uiState.value = _uiState.value.copy(isLoading = false)
      return false
    }

    val serie =
        Serie(
            serieId = repository.getNewSerieId(),
            title = state.title,
            description = state.description,
            date = parsedDate,
            participants = emptyList(),
            maxParticipants = state.maxParticipants.toInt(),
            visibility = Visibility.valueOf(state.visibility.uppercase(Locale.ROOT)),
            eventIds = emptyList(),
            ownerId = Firebase.auth.currentUser?.uid ?: "unknown")

    return try {
      repository.addSerie(serie)
      clearErrorMsg()
      _uiState.value = _uiState.value.copy(isLoading = false)
      true
    } catch (e: Exception) {
      Log.e("CreateSerieViewModel", "Error creating serie", e)
      setErrorMsg("Failed to create serie: ${e.message}")
      _uiState.value = _uiState.value.copy(isLoading = false)
      false
    }
  }

  // Update functions for all fields

  /**
   * Updates the serie title and validates it.
   *
   * @param title The new title value
   */
  fun setTitle(title: String) {
    _uiState.value =
        _uiState.value.copy(
            title = title, invalidTitleMsg = if (title.isBlank()) "Title cannot be empty" else null)
    updateFormValidity()
  }

  /**
   * Updates the serie description and validates it.
   *
   * @param description The new description value
   */
  fun setDescription(description: String) {
    _uiState.value =
        _uiState.value.copy(
            description = description,
            invalidDescriptionMsg =
                if (description.isBlank()) "Description cannot be empty" else null)
    updateFormValidity()
  }

  /**
   * Updates the maximum number of participants and validates it.
   *
   * @param value The new max participants value as a string
   */
  fun setMaxParticipants(value: String) {
    val num = value.toIntOrNull()
    _uiState.value =
        _uiState.value.copy(
            maxParticipants = value,
            invalidMaxParticipantsMsg =
                if (num == null || num <= 0) "Must be a positive number" else null)
    updateFormValidity()
  }

  /**
   * Updates the serie date and validates the format and ensures it's not in the past.
   *
   * Checks that the date is in dd/MM/yyyy format and is either today or a future date. Dates more
   * than 24 hours in the past are rejected.
   *
   * @param date The new date value in dd/MM/yyyy format
   */
  fun setDate(date: String) {
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val parsedDate =
        try {
          sdf.parse(date)
        } catch (_: Exception) {
          null
        }

    val errorMsg =
        when {
          parsedDate == null -> "Invalid format (must be dd/MM/yyyy)"
          parsedDate.time < System.currentTimeMillis() - (24 * 60 * 60 * 1000) ->
              "Date cannot be in the past"
          else -> null
        }

    _uiState.value = _uiState.value.copy(date = date, invalidDateMsg = errorMsg)
    updateFormValidity()
  }

  /**
   * Updates the serie time and validates the format.
   *
   * @param time The new time value in HH:mm format
   */
  fun setTime(time: String) {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    val valid =
        try {
          sdf.parse(time) != null
        } catch (_: Exception) {
          false
        }
    _uiState.value =
        _uiState.value.copy(
            time = time, invalidTimeMsg = if (!valid) "Invalid format (must be HH:mm)" else null)
    updateFormValidity()
  }

  /**
   * Updates the serie visibility and validates it.
   *
   * @param visibility The new visibility value (PUBLIC or PRIVATE)
   */
  fun setVisibility(visibility: String) {
    val validVisibilities = listOf("PUBLIC", "PRIVATE")
    _uiState.value =
        _uiState.value.copy(
            visibility = visibility,
            invalidVisibilityMsg =
                if (visibility.isBlank()) "Serie visibility cannot be empty"
                else if (visibility.uppercase(Locale.ROOT) !in validVisibilities)
                    "Visibility must be PUBLIC or PRIVATE"
                else null)
    updateFormValidity()
  }

  /**
   * Updates the overall form validity by checking all field validations.
   *
   * Clears the global error message if all fields are valid.
   */
  private fun updateFormValidity() {
    val state = _uiState.value
    val isValid =
        state.invalidTitleMsg == null &&
            state.invalidDescriptionMsg == null &&
            state.invalidMaxParticipantsMsg == null &&
            state.invalidDateMsg == null &&
            state.invalidVisibilityMsg == null &&
            state.invalidTimeMsg == null &&
            state.title.isNotBlank() &&
            state.description.isNotBlank() &&
            state.maxParticipants.isNotBlank() &&
            state.date.isNotBlank() &&
            state.time.isNotBlank() &&
            state.visibility.isNotBlank()

    _uiState.value = state.copy(errorMsg = null)
    if (state.isValid != isValid) {
      _uiState.value = state.copy()
    }
  }
}
