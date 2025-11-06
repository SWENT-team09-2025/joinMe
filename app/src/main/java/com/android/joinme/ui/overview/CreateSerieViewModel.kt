package com.android.joinme.ui.overview

import com.android.joinme.model.serie.Serie
import com.android.joinme.model.serie.SeriesRepository
import com.android.joinme.model.serie.SeriesRepositoryProvider
import com.android.joinme.model.utils.Visibility
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * UI state for the CreateSerie screen.
 *
 * Holds all form field values and their corresponding validation messages for creating a new serie.
 *
 * @property serieId Empty for CreateSerie (not used during creation)
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
) : BaseSerieFormViewModel() {

  override val _uiState = MutableStateFlow(CreateSerieUIState())
  val uiState: StateFlow<CreateSerieUIState> = _uiState.asStateFlow()

  override fun getState(): SerieFormUIState = _uiState.value

  override fun updateState(transform: (SerieFormUIState) -> SerieFormUIState) {
    _uiState.value = transform(_uiState.value) as CreateSerieUIState
  }

  /**
   * Creates a new serie and adds it to the repository.
   *
   * This function performs the following steps:
   * 1. Validates that all form fields are valid
   * 2. Checks that the user is authenticated (must be signed in)
   * 3. Parses the date and time into a single Timestamp
   * 4. Creates a Serie object with the current user as owner
   * 5. Saves the serie to the repository
   *
   * The loading state is set to true at the start and false upon completion. If any error occurs
   * during the process (validation failure, authentication check failure, date parsing error, or
   * repository error), an appropriate error message is set and the function returns null.
   *
   * @return The serie ID if the serie was created successfully, null if validation failed, user is
   *   not authenticated, date parsing failed, or repository save failed
   */
  suspend fun createSerie(): String? {
    val state = _uiState.value
    if (!state.isValid) {
      setErrorMsg("At least one field is not valid")
      return null
    }

    // Check if user is authenticated
    val currentUserId = getCurrentUserId()
    if (currentUserId == null) {
      setErrorMsg("You must be signed in to create a serie")
      return null
    }

    setLoadingState(true)

    val parsedDate = parseDateTime(state.date, state.time)
    if (parsedDate == null) {
      setErrorMsg("Invalid date format (must be dd/MM/yyyy HH:mm)")
      setLoadingState(false)
      return null
    }

    val serieId = repository.getNewSerieId()
    val serie =
        Serie(
            serieId = serieId,
            title = state.title,
            description = state.description,
            date = parsedDate,
            participants = listOf(currentUserId),
            maxParticipants = state.maxParticipants.toInt(),
            visibility = Visibility.valueOf(state.visibility.uppercase(Locale.ROOT)),
            eventIds = emptyList(),
            ownerId = currentUserId)

    return try {
      repository.addSerie(serie)
      clearErrorMsg()
      setLoadingState(false)
      serieId
    } catch (e: Exception) {
      setErrorMsg("Failed to create serie: ${e.message}")
      setLoadingState(false)
      null
    }
  }

  // All field validation methods (setTitle, setDescription, setMaxParticipants,
  // setDate, setTime, setVisibility) are inherited from BaseSerieFormViewModel
}
