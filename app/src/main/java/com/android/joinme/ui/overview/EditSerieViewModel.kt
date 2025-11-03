package com.android.joinme.ui.overview

import android.util.Log
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
 * UI state for the EditSerie screen.
 *
 * Holds all form field values and their corresponding validation messages for editing an existing
 * serie.
 *
 * @property serieId The ID of the serie being edited
 * @property title The title of the serie
 * @property description The description of the serie
 * @property maxParticipants String representation of the maximum number of participants
 * @property date The date in dd/MM/yyyy format
 * @property time The time in HH:mm format
 * @property visibility The visibility setting (PUBLIC or PRIVATE)
 * @property isLoading Indicates whether the serie is currently being updated or loaded
 * @property errorMsg Global error message for the form
 * @property invalidTitleMsg Validation message for the title field
 * @property invalidDescriptionMsg Validation message for the description field
 * @property invalidMaxParticipantsMsg Validation message for the max participants field
 * @property invalidDateMsg Validation message for the date field
 * @property invalidTimeMsg Validation message for the time field
 * @property invalidVisibilityMsg Validation message for the visibility field
 */
data class EditSerieUIState(
    override val serieId: String = "",
    override val title: String = "",
    override val description: String = "",
    override val maxParticipants: String = "",
    override val date: String = "",
    override val time: String = "",
    override val visibility: String = "",
    val isLoading: Boolean = false,
    override val errorMsg: String? = null,

    // validation messages
    override val invalidTitleMsg: String? = null,
    override val invalidDescriptionMsg: String? = null,
    override val invalidMaxParticipantsMsg: String? = null,
    override val invalidDateMsg: String? = null,
    override val invalidTimeMsg: String? = null,
    override val invalidVisibilityMsg: String? = null,
) : SerieFormUIState {
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
            visibility.isNotBlank() &&
            serieId.isNotBlank()
}

/**
 * ViewModel for the EditSerie screen.
 *
 * Manages the UI state and business logic for editing an existing serie. Handles form validation,
 * date/time parsing, and repository interactions for updating series.
 *
 * @property repository The SeriesRepository used for data operations
 */
class EditSerieViewModel(
    private val repository: SeriesRepository = SeriesRepositoryProvider.repository
) : BaseSerieFormViewModel() {

  override val _uiState = MutableStateFlow(EditSerieUIState())
  val uiState: StateFlow<EditSerieUIState> = _uiState.asStateFlow()

  override fun getState(): SerieFormUIState = _uiState.value

  override fun updateState(transform: (SerieFormUIState) -> SerieFormUIState) {
    _uiState.value = transform(_uiState.value) as EditSerieUIState
  }

  /**
   * Initializes the UI state with data from an existing serie.
   *
   * This function loads the serie data and populates the form fields. It converts the Timestamp to
   * the appropriate date and time string formats.
   *
   * @param serieId The ID of the serie to load
   */
  suspend fun loadSerie(serieId: String) {
    _uiState.value = _uiState.value.copy(isLoading = true)

    try {
      val serie = repository.getSerie(serieId)
      val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
      val dateTime = sdf.format(serie.date.toDate())
      val datePart = dateTime.substring(0, 10) // dd/MM/yyyy
      val timePart = dateTime.substring(11) // HH:mm

      _uiState.value =
          EditSerieUIState(
              serieId = serie.serieId,
              title = serie.title,
              description = serie.description,
              maxParticipants = serie.maxParticipants.toString(),
              date = datePart,
              time = timePart,
              visibility = serie.visibility.name,
              isLoading = false)
    } catch (e: Exception) {
      Log.e("EditSerieViewModel", "Error loading serie", e)
      setErrorMsg("Failed to load serie: ${e.message}")
      _uiState.value = _uiState.value.copy(isLoading = false)
    }
  }

  /**
   * Updates an existing serie and saves it to the repository.
   *
   * This function performs the following steps:
   * 1. Validates that all form fields are valid
   * 2. Checks that the user is authenticated (must be signed in)
   * 3. Parses the date and time into a single Timestamp
   * 4. Updates the Serie object with the new values
   * 5. Saves the updated serie to the repository
   *
   * The loading state is set to true at the start and false upon completion. If any error occurs
   * during the process (validation failure, authentication check failure, date parsing error, or
   * repository error), an appropriate error message is set and the function returns false.
   *
   * @return True if the serie was updated successfully, false if validation failed, user is not
   *   authenticated, date parsing failed, or repository save failed
   */
  suspend fun updateSerie(): Boolean {
    val state = _uiState.value
    if (!state.isValid) {
      setErrorMsg("At least one field is not valid")
      return false
    }

    // Check if user is authenticated
    val currentUserId = Firebase.auth.currentUser?.uid
    if (currentUserId == null) {
      setErrorMsg("You must be signed in to update a serie")
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

    return try {
      // Get the existing serie to preserve certain fields
      val existingSerie = repository.getSerie(state.serieId)

      val updatedSerie =
          existingSerie.copy(
              title = state.title,
              description = state.description,
              date = parsedDate,
              maxParticipants = state.maxParticipants.toInt(),
              visibility = Visibility.valueOf(state.visibility.uppercase(Locale.ROOT)))

      repository.editSerie(state.serieId, updatedSerie)
      clearErrorMsg()
      _uiState.value = _uiState.value.copy(isLoading = false)
      true
    } catch (e: Exception) {
      Log.e("EditSerieViewModel", "Error updating serie", e)
      setErrorMsg("Failed to update serie: ${e.message}")
      _uiState.value = _uiState.value.copy(isLoading = false)
      false
    }
  }

  // All field validation methods (setTitle, setDescription, setMaxParticipants,
  // setDate, setTime, setVisibility) are inherited from BaseSerieFormViewModel
}
