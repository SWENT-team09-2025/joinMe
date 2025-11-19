package com.android.joinme.ui.overview

import com.android.joinme.model.event.Event
import com.android.joinme.model.event.EventFilter
import com.android.joinme.model.event.EventsRepository
import com.android.joinme.model.map.Location
import com.android.joinme.model.profile.ProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

/**
 * Unit tests for CreateEventViewModel.
 *
 * These tests avoid asserting on exact error strings (which can change), and instead assert on
 * invalid* flags, isValid, and repository interactions.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CreateEventViewModelTest {

  // ---- Simple fake repo that records added events ----
  private class FakeEventsRepository : EventsRepository {
    val added = mutableListOf<Event>()

    override suspend fun addEvent(event: Event) {
      added += event
    }

    override suspend fun editEvent(eventId: String, newValue: Event) {
      /* no-op */
    }

    override suspend fun deleteEvent(eventId: String) {
      /* no-op */
    }

    override suspend fun getEventsByIds(eventIds: List<String>): List<Event> {
      /* no-op */
      return emptyList()
    }

    override suspend fun getEvent(eventId: String): Event =
        added.find { it.eventId == eventId } ?: throw NoSuchElementException("Event not found")

    override suspend fun getAllEvents(eventFilter: EventFilter): List<Event> = added.toList()

    override fun getNewEventId(): String = "fake-id-1"
  }

  private lateinit var repo: FakeEventsRepository
  private lateinit var profileRepository: ProfileRepository
  private lateinit var vm: CreateEventViewModel
  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    repo = FakeEventsRepository()
    profileRepository = mock(ProfileRepository::class.java)
    vm = CreateEventViewModel(repo, profileRepository)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  // ---------- Basic validity ----------

  @Test
  fun initialState_isInvalid() {
    val s = vm.uiState.value
    Assert.assertFalse(s.isValid)
  }

  @Test
  fun fillingAllFields_makesFormValid() {
    vm.setType("SPORTS")
    vm.setTitle("Football")
    vm.setDescription("Friendly 5v5")
    vm.selectLocation(Location(46.52, 6.63, "EPFL Field"))
    vm.setDate("25/12/2023")
    vm.setTime("10:00")
    vm.setMaxParticipants("10")
    vm.setDuration("90")
    vm.setVisibility("PUBLIC")

    Assert.assertTrue(vm.uiState.value.isValid)
  }

  // ---------- Field validation edges ----------

  @Test
  fun setType_invalid_marksInvalid() {
    vm.setType("MUSIC")
    val s = vm.uiState.value
    Assert.assertNotNull(s.invalidTypeMsg)
    Assert.assertFalse(s.isValid)
  }

  @Test
  fun setVisibility_invalid_marksInvalid() {
    vm.setVisibility("PUBLICK")
    val s = vm.uiState.value
    Assert.assertNotNull(s.invalidVisibilityMsg)
    Assert.assertFalse(s.isValid)
  }

  @Test
  fun setDate_wrongFormat_marksInvalid() {
    vm.setDate("2023-12-25 10:00")
    val s = vm.uiState.value
    Assert.assertNotNull(s.invalidDateMsg)
    Assert.assertFalse(s.isValid)
  }

  @Test
  fun setMaxParticipants_nonNumeric_marksInvalid() {
    vm.setMaxParticipants("ten")
    val s = vm.uiState.value
    Assert.assertNotNull(s.invalidMaxParticipantsMsg)
    Assert.assertFalse(s.isValid)
  }

  @Test
  fun setMaxParticipants_negative_marksInvalid() {
    vm.setMaxParticipants("-1")
    val s = vm.uiState.value
    Assert.assertNotNull(s.invalidMaxParticipantsMsg)
    Assert.assertFalse(s.isValid)
  }

  @Test
  fun setDuration_nonNumeric_marksInvalid() {
    vm.setDuration("abc")
    val s = vm.uiState.value
    Assert.assertNotNull(s.invalidDurationMsg)
    Assert.assertFalse(s.isValid)
  }

  @Test
  fun setDuration_zero_marksInvalid() {
    vm.setDuration("0")
    val s = vm.uiState.value
    Assert.assertNotNull(s.invalidDurationMsg)
    Assert.assertFalse(s.isValid)
  }

  // ---------- createEvent() behavior ----------

  @Test
  fun createEvent_withInvalidForm_returnsFalse_andDoesNotAdd() = runTest {
    // leave blank => invalid
    val ok = vm.createEvent()
    advanceUntilIdle()

    Assert.assertFalse(ok)
    Assert.assertTrue(repo.added.isEmpty())
    Assert.assertNotNull(vm.uiState.value.errorMsg) // some error is surfaced
  }

  @Test
  fun createEvent_withInvalidDate_returnsFalse_andDoesNotAdd() = runTest {
    vm.setType("SPORTS")
    vm.setTitle("Basketball")
    vm.setDescription("3v3")
    vm.setLocation("EPFL Gym")
    vm.setMaxParticipants("6")
    vm.setDuration("60")
    vm.setVisibility("PUBLIC")
    vm.setDate("not a date")

    val ok = vm.createEvent()
    advanceUntilIdle()

    Assert.assertFalse(ok)
    Assert.assertTrue(repo.added.isEmpty())
    Assert.assertNotNull(vm.uiState.value.errorMsg)
  }

  // ---------- error clearing ----------

  @Test
  fun clearErrorMsg_resetsErrorField() = runTest {
    // trigger an error
    val ok = vm.createEvent()
    Assert.assertFalse(ok)
    Assert.assertNotNull(vm.uiState.value.errorMsg)

    vm.clearErrorMsg()
    Assert.assertNull(vm.uiState.value.errorMsg)
  }
}
