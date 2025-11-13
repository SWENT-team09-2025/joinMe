package com.android.joinme.ui.overview

import com.android.joinme.model.map.Location
import com.android.joinme.model.map.LocationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for BaseEventFormViewModel.
 *
 * Tests all shared validation logic and state management functionality that is common between
 * Create and Edit event forms.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BaseEventFormViewModelTest {

  // Fake LocationRepository for testing
  private class FakeLocationRepository : LocationRepository {
    var shouldThrow = false
    var searchResults = emptyList<Location>()

    override suspend fun search(query: String): List<Location> {
      if (shouldThrow) throw Exception("Network error")
      return searchResults
    }
  }

  // Concrete implementation for testing since BaseEventFormViewModel is abstract
  private class TestEventFormViewModel(locationRepository: LocationRepository) :
      BaseEventFormViewModel(locationRepository) {

    override val _uiState = MutableStateFlow(CreateEventUIState())
    val uiState: StateFlow<CreateEventUIState> = _uiState.asStateFlow()

    override fun getState(): EventFormUIState = _uiState.value

    override fun updateState(transform: (EventFormUIState) -> EventFormUIState) {
      _uiState.value = transform(_uiState.value) as CreateEventUIState
    }

    // Public method for testing to set error messages
    fun setTestErrorMsg(msg: String) {
      _uiState.value = _uiState.value.copy(errorMsg = msg)
    }
  }

  private lateinit var locationRepo: FakeLocationRepository
  private lateinit var vm: TestEventFormViewModel
  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    locationRepo = FakeLocationRepository()
    vm = TestEventFormViewModel(locationRepo)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  // ==================== Type Validation Tests ====================

  @Test
  fun `setType with valid type sets type and clears error`() {
    vm.setType("SPORTS")
    val state = vm.uiState.value

    assertEquals("SPORTS", state.type)
    assertNull(state.invalidTypeMsg)
  }

  @Test
  fun `setType with blank type sets error`() {
    vm.setType("")
    val state = vm.uiState.value

    assertEquals("", state.type)
    assertNotNull(state.invalidTypeMsg)
    assertTrue(state.invalidTypeMsg!!.contains("cannot be empty"))
  }

  @Test
  fun `setType with invalid type sets error`() {
    vm.setType("MUSIC")
    val state = vm.uiState.value

    assertEquals("MUSIC", state.type)
    assertNotNull(state.invalidTypeMsg)
  }

  @Test
  fun `setType is case insensitive`() {
    vm.setType("sports")
    val state = vm.uiState.value

    assertEquals("sports", state.type)
    assertNull(state.invalidTypeMsg)
  }

  // ==================== Title Validation Tests ====================

  @Test
  fun `setTitle with valid title sets title and clears error`() {
    vm.setTitle("Football Match")
    val state = vm.uiState.value

    assertEquals("Football Match", state.title)
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
    vm.setDescription("A friendly football match")
    val state = vm.uiState.value

    assertEquals("A friendly football match", state.description)
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

  // ==================== Location Query Tests ====================

  @Test
  fun `setLocationQuery updates query in state`() {
    vm.setLocationQuery("Lausanne")
    val state = vm.uiState.value

    assertEquals("Lausanne", state.locationQuery)
  }

  @Test
  fun `setLocationQuery with non-empty query fetches suggestions`() = runTest {
    locationRepo.searchResults =
        listOf(
            Location(46.52, 6.63, "Lausanne, Switzerland"),
            Location(46.53, 6.64, "Lausanne Station"))

    vm.setLocationQuery("Lausanne")
    advanceUntilIdle()

    val state = vm.uiState.value
    assertEquals(2, state.locationSuggestions.size)
    assertEquals("Lausanne, Switzerland", state.locationSuggestions[0].name)
  }

  @Test
  fun `setLocationQuery with empty query clears suggestions`() {
    vm.setLocationQuery("")
    val state = vm.uiState.value

    assertEquals("", state.locationQuery)
    assertTrue(state.locationSuggestions.isEmpty())
  }

  @Test
  fun `setLocationQuery handles network error gracefully`() = runTest {
    locationRepo.shouldThrow = true

    vm.setLocationQuery("test")
    advanceUntilIdle()

    val state = vm.uiState.value
    assertTrue(state.locationSuggestions.isEmpty())
  }

  // ==================== Location Selection Tests ====================

  @Test
  fun `selectLocation sets location and clears error`() {
    val location = Location(46.52, 6.63, "EPFL")

    vm.selectLocation(location)
    val state = vm.uiState.value

    assertEquals("EPFL", state.location)
    assertEquals("EPFL", state.locationQuery)
    assertEquals(location, state.selectedLocation)
    assertNull(state.invalidLocationMsg)
  }

  @Test
  fun `selectLocation with different locations updates correctly`() {
    val location1 = Location(46.52, 6.63, "EPFL")
    val location2 = Location(46.53, 6.64, "Lausanne Station")

    vm.selectLocation(location1)
    vm.selectLocation(location2)
    val state = vm.uiState.value

    assertEquals("Lausanne Station", state.location)
    assertEquals(location2, state.selectedLocation)
  }

  // ==================== Duration Validation Tests ====================

  @Test
  fun `setDuration with valid positive number clears error`() {
    vm.setDuration("90")
    val state = vm.uiState.value

    assertEquals("90", state.duration)
    assertNull(state.invalidDurationMsg)
  }

  @Test
  fun `setDuration with zero sets error`() {
    vm.setDuration("0")
    val state = vm.uiState.value

    assertEquals("0", state.duration)
    assertNotNull(state.invalidDurationMsg)
    assertTrue(state.invalidDurationMsg!!.contains("positive number"))
  }

  @Test
  fun `setDuration with negative number sets error`() {
    vm.setDuration("-10")
    val state = vm.uiState.value

    assertEquals("-10", state.duration)
    assertNotNull(state.invalidDurationMsg)
  }

  @Test
  fun `setDuration with non-numeric value sets error`() {
    vm.setDuration("abc")
    val state = vm.uiState.value

    assertEquals("abc", state.duration)
    assertNotNull(state.invalidDurationMsg)
  }

  // ==================== Date Validation Tests ====================

  @Test
  fun `setDate with valid date format clears error`() {
    vm.setDate("25/12/2023")
    val state = vm.uiState.value

    assertEquals("25/12/2023", state.date)
    assertNull(state.invalidDateMsg)
  }

  @Test
  fun `setDate with invalid format sets error`() {
    vm.setDate("2023-12-25")
    val state = vm.uiState.value

    assertEquals("2023-12-25", state.date)
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
  fun `setDate with partial date sets error`() {
    vm.setDate("25/12")
    val state = vm.uiState.value

    assertNotNull(state.invalidDateMsg)
  }

  // ==================== Time Tests ====================

  @Test
  fun `setTime with different times updates correctly`() {
    vm.setTime("09:00")
    vm.setTime("17:45")
    val state = vm.uiState.value

    assertEquals("17:45", state.time)
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
    vm.setVisibility("CUSTOM")
    val state = vm.uiState.value

    assertEquals("CUSTOM", state.visibility)
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
    vm.setType("SPORTS")
    vm.setTitle("Football")
    vm.setDescription("5v5 friendly match")
    vm.setDuration("90")
    vm.setDate("25/12/2023")
    vm.setVisibility("PUBLIC")

    val state = vm.uiState.value
    assertNull(state.invalidTypeMsg)
    assertNull(state.invalidTitleMsg)
    assertNull(state.invalidDescriptionMsg)
    assertNull(state.invalidDurationMsg)
    assertNull(state.invalidDateMsg)
    assertNull(state.invalidVisibilityMsg)
  }

  @Test
  fun `correcting invalid field clears its error`() {
    vm.setType("INVALID")
    assertNotNull(vm.uiState.value.invalidTypeMsg)

    vm.setType("SPORTS")
    assertNull(vm.uiState.value.invalidTypeMsg)
  }

  @Test
  fun `location search and selection workflow`() = runTest {
    locationRepo.searchResults = listOf(Location(46.52, 6.63, "EPFL"))

    vm.setLocationQuery("EPFL")
    advanceUntilIdle()

    assertEquals(1, vm.uiState.value.locationSuggestions.size)

    vm.selectLocation(vm.uiState.value.locationSuggestions[0])

    val state = vm.uiState.value
    assertEquals("EPFL", state.location)
    assertEquals("EPFL", state.locationQuery)
    assertNotNull(state.selectedLocation)
  }
}
