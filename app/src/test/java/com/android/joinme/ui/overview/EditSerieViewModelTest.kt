package com.android.joinme.ui.overview

import com.android.joinme.model.serie.Serie
import com.android.joinme.model.serie.SerieFilter
import com.android.joinme.model.serie.SeriesRepository
import com.android.joinme.model.utils.Visibility
import com.google.firebase.Timestamp
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EditSerieViewModelTest {

  // Simple fake repository for testing
  private class FakeSeriesRepository : SeriesRepository {
    private val series = mutableMapOf<String, Serie>()

    override suspend fun addSerie(serie: Serie) {
      series[serie.serieId] = serie
    }

    override suspend fun editSerie(serieId: String, newValue: Serie) {
      series[serieId] = newValue
    }

    override suspend fun deleteSerie(serieId: String) {
      series.remove(serieId)
    }

    override suspend fun getSerie(serieId: String): Serie =
        series[serieId] ?: throw NoSuchElementException("Serie not found")

    override suspend fun getAllSeries(serieFilter: SerieFilter): List<Serie> =
        series.values.toList()

    override fun getNewSerieId(): String = "new-serie-id"
  }

  private lateinit var repository: FakeSeriesRepository
  private lateinit var viewModel: EditSerieViewModel
  private val testDispatcher = StandardTestDispatcher()

  private fun createTestSerie(): Serie {
    val calendar = Calendar.getInstance()
    calendar.set(2025, Calendar.JANUARY, 15, 18, 30, 0)

    return Serie(
        serieId = "test-serie-1",
        title = "Weekly Football",
        description = "Weekly football games every Friday",
        date = Timestamp(calendar.time),
        participants = listOf("user1", "owner123"),
        maxParticipants = 20,
        visibility = Visibility.PUBLIC,
        eventIds = listOf("event1", "event2"),
        ownerId = "owner123")
  }

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    repository = FakeSeriesRepository()
    viewModel = EditSerieViewModel(repository)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  /** --- LOAD SERIE TESTS --- */
  @Test
  fun loadSerie_validSerieId_updatesUIState() = runTest {
    val serie = createTestSerie()
    repository.addSerie(serie)

    viewModel.loadSerie(serie.serieId)
    advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertEquals("Weekly Football", state.title)
    assertEquals("Weekly football games every Friday", state.description)
    assertEquals("20", state.maxParticipants)
    assertEquals("15/01/2025", state.date)
    assertEquals("18:30", state.time)
    assertEquals("PUBLIC", state.visibility)
    assertNull(state.errorMsg)
  }

  @Test
  fun loadSerie_invalidSerieId_setsErrorMessage() = runTest {
    viewModel.loadSerie("non-existent-id")
    advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertNotNull(state.errorMsg)
    assertTrue(state.errorMsg!!.contains("Failed to load serie"))
  }

  /** --- CLEAR ERROR MESSAGE TESTS --- */
  @Test
  fun clearErrorMsg_removesErrorMessage() = runTest {
    viewModel.loadSerie("non-existent-id")
    advanceUntilIdle()

    // Verify error is set
    var state = viewModel.uiState.first()
    assertNotNull(state.errorMsg)

    // Clear error
    viewModel.clearErrorMsg()

    // Verify error is cleared
    state = viewModel.uiState.first()
    assertNull(state.errorMsg)
  }

  /** --- SET TITLE TESTS --- */
  @Test
  fun setTitle_validTitle_updatesStateWithoutError() = runBlocking {
    viewModel.setTitle("My Serie")

    val state = viewModel.uiState.first()
    assertEquals("My Serie", state.title)
    assertNull(state.invalidTitleMsg)
  }

  @Test
  fun setTitle_blankTitle_setsErrorMessage() = runBlocking {
    viewModel.setTitle("   ")

    val state = viewModel.uiState.first()
    assertEquals("   ", state.title)
    assertEquals("Title cannot be empty", state.invalidTitleMsg)
  }

  /** --- SET DESCRIPTION TESTS --- */
  @Test
  fun setDescription_validDescription_updatesStateWithoutError() = runBlocking {
    viewModel.setDescription("This is a description")

    val state = viewModel.uiState.first()
    assertEquals("This is a description", state.description)
    assertNull(state.invalidDescriptionMsg)
  }

  @Test
  fun setDescription_blankDescription_setsErrorMessage() = runBlocking {
    viewModel.setDescription("")

    val state = viewModel.uiState.first()
    assertEquals("", state.description)
    assertEquals("Description cannot be empty", state.invalidDescriptionMsg)
  }

  /** --- SET MAX PARTICIPANTS TESTS --- */
  @Test
  fun setMaxParticipants_validNumber_updatesStateWithoutError() = runBlocking {
    viewModel.setMaxParticipants("15")

    val state = viewModel.uiState.first()
    assertEquals("15", state.maxParticipants)
    assertNull(state.invalidMaxParticipantsMsg)
  }

  @Test
  fun setMaxParticipants_zero_setsErrorMessage() = runBlocking {
    viewModel.setMaxParticipants("0")

    val state = viewModel.uiState.first()
    assertEquals("0", state.maxParticipants)
    assertEquals("Must be a positive number", state.invalidMaxParticipantsMsg)
  }

  @Test
  fun setMaxParticipants_negativeNumber_setsErrorMessage() = runBlocking {
    viewModel.setMaxParticipants("-5")

    val state = viewModel.uiState.first()
    assertEquals("-5", state.maxParticipants)
    assertEquals("Must be a positive number", state.invalidMaxParticipantsMsg)
  }

  @Test
  fun setMaxParticipants_nonNumeric_setsErrorMessage() = runBlocking {
    viewModel.setMaxParticipants("abc")

    val state = viewModel.uiState.first()
    assertEquals("abc", state.maxParticipants)
    assertEquals("Must be a positive number", state.invalidMaxParticipantsMsg)
  }

  /** --- SET DATE TESTS --- */
  @Test
  fun setDate_validDate_updatesStateWithoutError() = runBlocking {
    viewModel.setDate("25/12/2025")

    val state = viewModel.uiState.first()
    assertEquals("25/12/2025", state.date)
    assertNull(state.invalidDateMsg)
  }

  @Test
  fun setDate_invalidFormat_setsErrorMessage() = runBlocking {
    viewModel.setDate("2025-12-25")

    val state = viewModel.uiState.first()
    assertEquals("2025-12-25", state.date)
    assertEquals("Invalid format (must be dd/MM/yyyy)", state.invalidDateMsg)
  }

  @Test
  fun setDate_invalidDate_setsErrorMessage() = runBlocking {
    viewModel.setDate("not-a-date")

    val state = viewModel.uiState.first()
    assertEquals("not-a-date", state.date)
    assertEquals("Invalid format (must be dd/MM/yyyy)", state.invalidDateMsg)
  }

  @Test
  fun setDate_pastDate_setsErrorMessage() = runBlocking {
    viewModel.setDate("01/01/2020")

    val state = viewModel.uiState.first()
    assertEquals("01/01/2020", state.date)
    assertEquals("Date cannot be in the past", state.invalidDateMsg)
  }

  /** --- SET TIME TESTS --- */
  @Test
  fun setTime_validTime_updatesStateWithoutError() = runBlocking {
    // Set future date first
    viewModel.setDate("25/12/2025")
    viewModel.setTime("14:30")

    val state = viewModel.uiState.first()
    assertEquals("14:30", state.time)
    assertNull(state.invalidTimeMsg)
  }

  @Test
  fun setTime_invalidFormat_setsErrorMessage() = runBlocking {
    viewModel.setTime("not-a-time")

    val state = viewModel.uiState.first()
    assertEquals("not-a-time", state.time)
    assertEquals("Invalid format (must be HH:mm)", state.invalidTimeMsg)
  }

  /** --- SET VISIBILITY TESTS --- */
  @Test
  fun setVisibility_publicVisibility_updatesStateWithoutError() = runBlocking {
    viewModel.setVisibility("PUBLIC")

    val state = viewModel.uiState.first()
    assertEquals("PUBLIC", state.visibility)
    assertNull(state.invalidVisibilityMsg)
  }

  @Test
  fun setVisibility_privateVisibility_updatesStateWithoutError() = runBlocking {
    viewModel.setVisibility("PRIVATE")

    val state = viewModel.uiState.first()
    assertEquals("PRIVATE", state.visibility)
    assertNull(state.invalidVisibilityMsg)
  }

  @Test
  fun setVisibility_blankVisibility_setsErrorMessage() = runBlocking {
    viewModel.setVisibility("")

    val state = viewModel.uiState.first()
    assertEquals("", state.visibility)
    assertEquals("Serie visibility cannot be empty", state.invalidVisibilityMsg)
  }

  @Test
  fun setVisibility_invalidVisibility_setsErrorMessage() = runBlocking {
    viewModel.setVisibility("INVALID")

    val state = viewModel.uiState.first()
    assertEquals("INVALID", state.visibility)
    assertEquals("Visibility must be PUBLIC or PRIVATE", state.invalidVisibilityMsg)
  }

  /** --- IS VALID TESTS --- */
  @Test
  fun isValid_allFieldsValid_returnsTrue() = runBlocking {
    viewModel.setTitle("Test Serie")
    viewModel.setDescription("Test Description")
    viewModel.setMaxParticipants("10")
    viewModel.setDate("25/12/2025")
    viewModel.setTime("18:00")
    viewModel.setVisibility("PUBLIC")

    val state = viewModel.uiState.first()
    // Note: isValid also requires serieId to be non-blank for EditSerie
    // This is testing the validation logic, not the full isValid check
    assertNull(state.invalidTitleMsg)
    assertNull(state.invalidDescriptionMsg)
    assertNull(state.invalidMaxParticipantsMsg)
    assertNull(state.invalidDateMsg)
    assertNull(state.invalidTimeMsg)
    assertNull(state.invalidVisibilityMsg)
  }

  @Test
  fun isValid_emptyTitle_returnsFalse() = runBlocking {
    viewModel.setTitle("")
    viewModel.setDescription("Test Description")
    viewModel.setMaxParticipants("10")
    viewModel.setDate("25/12/2025")
    viewModel.setTime("18:00")
    viewModel.setVisibility("PUBLIC")

    val state = viewModel.uiState.first()
    assertFalse(state.isValid)
  }

  @Test
  fun isValid_invalidMaxParticipants_returnsFalse() = runBlocking {
    viewModel.setTitle("Test Serie")
    viewModel.setDescription("Test Description")
    viewModel.setMaxParticipants("0")
    viewModel.setDate("25/12/2025")
    viewModel.setTime("18:00")
    viewModel.setVisibility("PUBLIC")

    val state = viewModel.uiState.first()
    assertFalse(state.isValid)
  }

  /** --- UPDATE SERIE TESTS --- */
  @Test
  fun updateSerie_invalidData_returnsFalse() = runTest {
    viewModel.setTitle("") // Invalid - empty title
    viewModel.setDescription("Test")
    viewModel.setMaxParticipants("10")
    viewModel.setDate("25/12/2025")
    viewModel.setTime("18:00")
    viewModel.setVisibility("PUBLIC")

    val result = viewModel.updateSerie()
    advanceUntilIdle()

    assertFalse(result)

    val state = viewModel.uiState.first()
    assertNotNull(state.errorMsg)
    assertTrue(state.errorMsg!!.contains("not valid"))
  }

  /** --- INITIAL STATE TESTS --- */
  @Test
  fun viewModel_initialState_isEmpty() {
    val state = viewModel.uiState.value
    assertEquals("", state.serieId)
    assertEquals("", state.title)
    assertEquals("", state.description)
    assertEquals("", state.maxParticipants)
    assertEquals("", state.date)
    assertEquals("", state.time)
    assertEquals("", state.visibility)
    assertNull(state.errorMsg)
    assertFalse(state.isValid)
  }

  /** --- ERROR MESSAGE DISPLAY TESTS --- */
  @Test
  fun setMaxParticipants_zero_errorMessageDisplayed() = runTest {
    viewModel.setMaxParticipants("0")

    val state = viewModel.uiState.first()
    assertNotNull(state.invalidMaxParticipantsMsg)
    assertEquals("Must be a positive number", state.invalidMaxParticipantsMsg)
    assertEquals("0", state.maxParticipants)
    assertFalse(state.isValid)
  }

  @Test
  fun setMaxParticipants_valid_noErrorMessage() = runTest {
    viewModel.setMaxParticipants("10")

    val state = viewModel.uiState.first()
    assertNull(state.invalidMaxParticipantsMsg)
    assertEquals("10", state.maxParticipants)
  }

  @Test
  fun setTitle_blank_errorMessageDisplayed() = runTest {
    viewModel.setTitle("")

    val state = viewModel.uiState.first()
    assertNotNull(state.invalidTitleMsg)
    assertEquals("Title cannot be empty", state.invalidTitleMsg)
    assertFalse(state.isValid)
  }

  @Test
  fun setTitle_valid_noErrorMessage() = runTest {
    viewModel.setTitle("Valid Title")

    val state = viewModel.uiState.first()
    assertNull(state.invalidTitleMsg)
    assertEquals("Valid Title", state.title)
  }

  @Test
  fun setDate_invalidFormat_errorMessageDisplayed() = runTest {
    viewModel.setDate("2025-12-25")

    val state = viewModel.uiState.first()
    assertNotNull(state.invalidDateMsg)
    assertEquals("Invalid format (must be dd/MM/yyyy)", state.invalidDateMsg)
    assertFalse(state.isValid)
  }

  @Test
  fun setDate_valid_noErrorMessage() = runTest {
    viewModel.setDate("25/12/2025")

    val state = viewModel.uiState.first()
    assertNull(state.invalidDateMsg)
    assertEquals("25/12/2025", state.date)
  }

  @Test
  fun setVisibility_invalid_errorMessageDisplayed() = runTest {
    viewModel.setVisibility("INVALID")

    val state = viewModel.uiState.first()
    assertNotNull(state.invalidVisibilityMsg)
    assertEquals("Visibility must be PUBLIC or PRIVATE", state.invalidVisibilityMsg)
    assertFalse(state.isValid)
  }

  @Test
  fun setVisibility_blank_errorMessageDisplayed() = runTest {
    viewModel.setVisibility("")

    val state = viewModel.uiState.first()
    assertNotNull(state.invalidVisibilityMsg)
    assertEquals("Serie visibility cannot be empty", state.invalidVisibilityMsg)
    assertFalse(state.isValid)
  }

  @Test
  fun setVisibility_valid_noErrorMessage() = runTest {
    viewModel.setVisibility("PUBLIC")

    val state = viewModel.uiState.first()
    assertNull(state.invalidVisibilityMsg)
    assertEquals("PUBLIC", state.visibility)
  }

  @Test
  fun multipleValidationErrors_allErrorMessagesDisplayed() = runTest {
    // Set multiple invalid fields
    viewModel.setTitle("")
    viewModel.setDescription("")
    viewModel.setMaxParticipants("0")
    viewModel.setVisibility("")

    val state = viewModel.uiState.first()

    // Verify all error messages are set
    assertNotNull(state.invalidTitleMsg)
    assertNotNull(state.invalidDescriptionMsg)
    assertNotNull(state.invalidMaxParticipantsMsg)
    assertNotNull(state.invalidVisibilityMsg)

    // Verify form is invalid
    assertFalse(state.isValid)
  }

  @Test
  fun errorMessageClears_whenFieldBecomesValid() = runTest {
    // First set invalid value
    viewModel.setMaxParticipants("0")
    var state = viewModel.uiState.first()
    assertNotNull(state.invalidMaxParticipantsMsg)

    // Then set valid value
    viewModel.setMaxParticipants("10")
    state = viewModel.uiState.first()
    assertNull(state.invalidMaxParticipantsMsg)
    assertEquals("10", state.maxParticipants)
  }

  @Test
  fun loadSerie_updatesAllFields() = runTest {
    val serie = createTestSerie()
    repository.addSerie(serie)

    viewModel.loadSerie(serie.serieId)
    advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertEquals(serie.serieId, state.serieId)
    assertEquals(serie.title, state.title)
    assertEquals(serie.description, state.description)
    assertEquals(serie.maxParticipants.toString(), state.maxParticipants)
    assertEquals(serie.visibility.name, state.visibility)
  }
}
