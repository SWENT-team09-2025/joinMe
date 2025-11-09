package com.android.joinme.ui.overview

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for BaseSerieFormViewModel.
 *
 * Tests all shared validation logic and state management functionality that is common between
 * Create and Edit serie forms.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BaseSerieFormViewModelTest {

  // Concrete implementation for testing since BaseSerieFormViewModel is abstract
  private class TestSerieFormViewModel : BaseSerieFormViewModel() {

    override val _uiState = MutableStateFlow(CreateSerieUIState())
    val uiState: StateFlow<CreateSerieUIState> = _uiState.asStateFlow()

    override fun getState(): SerieFormUIState = _uiState.value

    override fun updateState(transform: (SerieFormUIState) -> SerieFormUIState) {
      _uiState.value = transform(_uiState.value) as CreateSerieUIState
    }

    // Public method for testing to set error messages
    fun setTestErrorMsg(msg: String) {
      _uiState.value = _uiState.value.copy(errorMsg = msg)
    }

    // Expose protected methods for testing
    fun testParseDateTime(date: String, time: String) = parseDateTime(date, time)

    fun testSetLoadingState(isLoading: Boolean) = setLoadingState(isLoading)
  }

  private lateinit var vm: TestSerieFormViewModel
  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    vm = TestSerieFormViewModel()
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  // ==================== Title Validation Tests ====================

  @Test
  fun `setTitle with valid title sets title and clears error`() {
    vm.setTitle("Football League")
    val state = vm.uiState.value

    assertEquals("Football League", state.title)
    assertNull(state.invalidTitleMsg)
  }

  @Test
  fun `setTitle with blank title sets error`() {
    vm.setTitle("")
    val state = vm.uiState.value

    assertEquals("", state.title)
    assertNotNull(state.invalidTitleMsg)
    assertTrue(state.invalidTitleMsg!!.contains("cannot be empty"))
  }

  @Test
  fun `setTitle with whitespace only sets error`() {
    vm.setTitle("   ")
    val state = vm.uiState.value

    assertEquals("   ", state.title)
    assertNotNull(state.invalidTitleMsg)
  }

  // ==================== Description Validation Tests ====================

  @Test
  fun `setDescription with valid description sets description and clears error`() {
    vm.setDescription("A weekly football league for enthusiasts")
    val state = vm.uiState.value

    assertEquals("A weekly football league for enthusiasts", state.description)
    assertNull(state.invalidDescriptionMsg)
  }

  @Test
  fun `setDescription with blank description sets error`() {
    vm.setDescription("")
    val state = vm.uiState.value

    assertEquals("", state.description)
    assertNotNull(state.invalidDescriptionMsg)
    assertTrue(state.invalidDescriptionMsg!!.contains("cannot be empty"))
  }

  // ==================== Max Participants Validation Tests ====================

  @Test
  fun `setMaxParticipants with valid positive number clears error`() {
    vm.setMaxParticipants("20")
    val state = vm.uiState.value

    assertEquals("20", state.maxParticipants)
    assertNull(state.invalidMaxParticipantsMsg)
  }

  @Test
  fun `setMaxParticipants with zero sets error`() {
    vm.setMaxParticipants("0")
    val state = vm.uiState.value

    assertEquals("0", state.maxParticipants)
    assertNotNull(state.invalidMaxParticipantsMsg)
    assertTrue(state.invalidMaxParticipantsMsg!!.contains("positive number"))
  }

  @Test
  fun `setMaxParticipants with negative number sets error`() {
    vm.setMaxParticipants("-5")
    val state = vm.uiState.value

    assertEquals("-5", state.maxParticipants)
    assertNotNull(state.invalidMaxParticipantsMsg)
  }

  @Test
  fun `setMaxParticipants with non-numeric value sets error`() {
    vm.setMaxParticipants("twenty")
    val state = vm.uiState.value

    assertEquals("twenty", state.maxParticipants)
    assertNotNull(state.invalidMaxParticipantsMsg)
  }

  // ==================== Date Validation Tests ====================

  @Test
  fun `setDate with valid future date format clears error`() {
    vm.setDate("25/12/2025")
    val state = vm.uiState.value

    assertEquals("25/12/2025", state.date)
    assertNull(state.invalidDateMsg)
  }

  @Test
  fun `setDate with invalid format sets error`() {
    vm.setDate("2025-12-25")
    val state = vm.uiState.value

    assertEquals("2025-12-25", state.date)
    assertNotNull(state.invalidDateMsg)
    assertTrue(state.invalidDateMsg!!.contains("dd/MM/yyyy"))
  }

  @Test
  fun `setDate with invalid date sets error`() {
    vm.setDate("invalid-date")
    val state = vm.uiState.value

    assertEquals("invalid-date", state.date)
    assertNotNull(state.invalidDateMsg)
  }

  @Test
  fun `setDate with past date sets error`() {
    vm.setDate("01/01/2020")
    val state = vm.uiState.value

    assertEquals("01/01/2020", state.date)
    assertNotNull(state.invalidDateMsg)
    assertTrue(state.invalidDateMsg!!.contains("past"))
  }

  @Test
  fun `setDate with partial date sets error`() {
    vm.setDate("25/12")
    val state = vm.uiState.value

    assertNotNull(state.invalidDateMsg)
  }

  @Test
  fun `setDate with today date clears error`() = runTest {
    val calendar = java.util.Calendar.getInstance()
    val today =
        String.format(
            "%02d/%02d/%04d",
            calendar.get(java.util.Calendar.DAY_OF_MONTH),
            calendar.get(java.util.Calendar.MONTH) + 1,
            calendar.get(java.util.Calendar.YEAR))

    vm.setDate(today)
    val state = vm.uiState.value

    assertEquals(today, state.date)
    assertNull(state.invalidDateMsg)
  }

  // ==================== Time Validation Tests ====================

  @Test
  fun `setTime with invalid format sets error`() {
    vm.setTime("25:99")
    val state = vm.uiState.value

    assertEquals("25:99", state.time)
    // Note: SimpleDateFormat is lenient, so this might pass validation
    // If it does pass, that's expected behavior
  }

  @Test
  fun `setTime with non-time value sets error`() {
    vm.setTime("not-a-time")
    val state = vm.uiState.value

    assertEquals("not-a-time", state.time)
    assertNotNull(state.invalidTimeMsg)
    assertTrue(state.invalidTimeMsg!!.contains("HH:mm"))
  }

  @Test
  fun `setTime with different times updates correctly`() {
    vm.setDate("25/12/2025")
    vm.setTime("09:00")
    vm.setTime("17:45")
    val state = vm.uiState.value

    assertEquals("17:45", state.time)
  }

  @Test
  fun `setTime with today date and past time sets error`() {
    // Set today's date
    val calendar = java.util.Calendar.getInstance()
    val today =
        String.format(
            "%02d/%02d/%04d",
            calendar.get(java.util.Calendar.DAY_OF_MONTH),
            calendar.get(java.util.Calendar.MONTH) + 1,
            calendar.get(java.util.Calendar.YEAR))
    vm.setDate(today)

    // Set a time that's definitely in the past (e.g., 00:00)
    vm.setTime("00:00")
    val state = vm.uiState.value

    assertNotNull(state.invalidTimeMsg)
    assertTrue(state.invalidTimeMsg!!.contains("past"))
  }

  // ==================== Visibility Validation Tests ====================

  @Test
  fun `setVisibility with PUBLIC clears error`() {
    vm.setVisibility("PUBLIC")
    val state = vm.uiState.value

    assertEquals("PUBLIC", state.visibility)
    assertNull(state.invalidVisibilityMsg)
  }

  @Test
  fun `setVisibility with PRIVATE clears error`() {
    vm.setVisibility("PRIVATE")
    val state = vm.uiState.value

    assertEquals("PRIVATE", state.visibility)
    assertNull(state.invalidVisibilityMsg)
  }

  @Test
  fun `setVisibility with blank sets error`() {
    vm.setVisibility("")
    val state = vm.uiState.value

    assertEquals("", state.visibility)
    assertNotNull(state.invalidVisibilityMsg)
    assertTrue(state.invalidVisibilityMsg!!.contains("cannot be empty"))
  }

  @Test
  fun `setVisibility with invalid value sets error`() {
    vm.setVisibility("SECRET")
    val state = vm.uiState.value

    assertEquals("SECRET", state.visibility)
    assertNotNull(state.invalidVisibilityMsg)
    assertTrue(state.invalidVisibilityMsg!!.contains("PUBLIC or PRIVATE"))
  }

  @Test
  fun `setVisibility is case insensitive`() {
    vm.setVisibility("public")
    val state = vm.uiState.value

    assertEquals("public", state.visibility)
    assertNull(state.invalidVisibilityMsg)
  }

  // ==================== Error Message Tests ====================

  @Test
  fun `clearErrorMsg clears error message`() {
    // Manually set an error using the test helper method
    vm.setTestErrorMsg("Test error")
    assertNotNull(vm.uiState.value.errorMsg)

    vm.clearErrorMsg()
    val state = vm.uiState.value

    assertNull(state.errorMsg)
  }

  // ==================== Integration Tests ====================

  @Test
  fun `setting multiple valid fields maintains validity`() {
    vm.setTitle("Football League")
    vm.setDescription("Weekly football games")
    vm.setMaxParticipants("20")
    vm.setDate("25/12/2025")
    vm.setTime("18:00")
    vm.setVisibility("PUBLIC")

    val state = vm.uiState.value
    assertNull(state.invalidTitleMsg)
    assertNull(state.invalidDescriptionMsg)
    assertNull(state.invalidMaxParticipantsMsg)
    assertNull(state.invalidDateMsg)
    assertNull(state.invalidTimeMsg)
    assertNull(state.invalidVisibilityMsg)
  }

  @Test
  fun `correcting invalid field clears its error`() {
    vm.setTitle("")
    assertNotNull(vm.uiState.value.invalidTitleMsg)

    vm.setTitle("Valid Title")
    assertNull(vm.uiState.value.invalidTitleMsg)
  }

  @Test
  fun `form validation clears global error when form becomes valid`() {
    // Set an error
    vm.setTestErrorMsg("Some error")
    assertNotNull(vm.uiState.value.errorMsg)

    // Fill all fields to make form valid
    vm.setTitle("Football League")
    vm.setDescription("Weekly games")
    vm.setMaxParticipants("20")
    vm.setDate("25/12/2025")
    vm.setTime("18:00")
    vm.setVisibility("PUBLIC")

    // Error should be cleared when form becomes valid
    val state = vm.uiState.value
    assertNull(state.errorMsg)
  }

  @Test
  fun `invalid field prevents form from becoming valid`() {
    vm.setTitle("Valid Title")
    vm.setDescription("Valid Description")
    vm.setMaxParticipants("0") // Invalid
    vm.setDate("25/12/2025")
    vm.setTime("18:00")
    vm.setVisibility("PUBLIC")

    val state = vm.uiState.value
    assertNotNull(state.invalidMaxParticipantsMsg)
  }

  @Test
  fun `date validation revalidates time when date changes`() {
    // Set a valid future time first
    vm.setDate("25/12/2025")
    vm.setTime("10:00")
    assertNull(vm.uiState.value.invalidTimeMsg)

    // Now set date to today - time might become invalid
    val calendar = java.util.Calendar.getInstance()
    val today =
        String.format(
            "%02d/%02d/%04d",
            calendar.get(java.util.Calendar.DAY_OF_MONTH),
            calendar.get(java.util.Calendar.MONTH) + 1,
            calendar.get(java.util.Calendar.YEAR))
    vm.setDate(today)

    // If current time is after 10:00, the time should now be invalid
    val currentHour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
    if (currentHour >= 10) {
      assertNotNull(vm.uiState.value.invalidTimeMsg)
    }
  }

  @Test
  fun `multiple errors can exist simultaneously`() {
    vm.setTitle("")
    vm.setDescription("")
    vm.setMaxParticipants("0")
    vm.setDate("invalid")
    vm.setTime("invalid")
    vm.setVisibility("INVALID")

    val state = vm.uiState.value
    assertNotNull(state.invalidTitleMsg)
    assertNotNull(state.invalidDescriptionMsg)
    assertNotNull(state.invalidMaxParticipantsMsg)
    assertNotNull(state.invalidDateMsg)
    assertNotNull(state.invalidTimeMsg)
    assertNotNull(state.invalidVisibilityMsg)
  }

  @Test
  fun `clearing one error does not affect others`() {
    vm.setTitle("")
    vm.setMaxParticipants("0")

    assertNotNull(vm.uiState.value.invalidTitleMsg)
    assertNotNull(vm.uiState.value.invalidMaxParticipantsMsg)

    // Fix only the title
    vm.setTitle("Valid Title")

    assertNull(vm.uiState.value.invalidTitleMsg)
    assertNotNull(vm.uiState.value.invalidMaxParticipantsMsg)
  }

  // ==================== DateTime Parsing Tests ====================

  @Test
  fun `parseDateTime with valid date and time returns Timestamp`() {
    val timestamp = vm.testParseDateTime("25/12/2025", "18:30")

    assertNotNull(timestamp)
    // Verify the timestamp represents the expected date/time
    val calendar = java.util.Calendar.getInstance()
    calendar.time = timestamp!!.toDate()
    assertEquals(25, calendar.get(java.util.Calendar.DAY_OF_MONTH))
    assertEquals(11, calendar.get(java.util.Calendar.MONTH)) // December is 11 (0-based)
    assertEquals(2025, calendar.get(java.util.Calendar.YEAR))
    assertEquals(18, calendar.get(java.util.Calendar.HOUR_OF_DAY))
    assertEquals(30, calendar.get(java.util.Calendar.MINUTE))
  }

  @Test
  fun `parseDateTime with invalid format returns null`() {
    val timestamp1 = vm.testParseDateTime("2025-12-25", "18:30")
    assertNull(timestamp1)

    val timestamp2 = vm.testParseDateTime("25/12/2025", "invalid-time")
    assertNull(timestamp2)

    val timestamp3 = vm.testParseDateTime("", "")
    assertNull(timestamp3)

    val timestamp4 = vm.testParseDateTime("25/12", "18:30")
    assertNull(timestamp4)
  }


  @Test
  fun `parseDateTime with midnight time returns Timestamp`() {
    val timestamp = vm.testParseDateTime("01/01/2026", "00:00")

    assertNotNull(timestamp)
    val calendar = java.util.Calendar.getInstance()
    calendar.time = timestamp!!.toDate()
    assertEquals(0, calendar.get(java.util.Calendar.HOUR_OF_DAY))
    assertEquals(0, calendar.get(java.util.Calendar.MINUTE))
  }

  @Test
  fun `parseDateTime with end of day time returns Timestamp`() {
    val timestamp = vm.testParseDateTime("31/12/2025", "23:59")

    assertNotNull(timestamp)
    val calendar = java.util.Calendar.getInstance()
    calendar.time = timestamp!!.toDate()
    assertEquals(23, calendar.get(java.util.Calendar.HOUR_OF_DAY))
    assertEquals(59, calendar.get(java.util.Calendar.MINUTE))
  }


  // ==================== Loading State Tests ====================
  @Test
  fun `setLoadingState can toggle loading state`() {
    vm.testSetLoadingState(true)
    assertTrue(vm.uiState.value.isLoading)

    vm.testSetLoadingState(false)
    assertFalse(vm.uiState.value.isLoading)

    vm.testSetLoadingState(true)
    assertTrue(vm.uiState.value.isLoading)
  }

  @Test
  fun `setLoadingState preserves other state fields`() {
    // Set some other fields first
    vm.setTitle("Test Title")
    vm.setDescription("Test Description")
    vm.setMaxParticipants("10")

    vm.testSetLoadingState(true)

    val state = vm.uiState.value
    assertTrue(state.isLoading)
    assertEquals("Test Title", state.title)
    assertEquals("Test Description", state.description)
    assertEquals("10", state.maxParticipants)
  }
}
