package com.android.joinme.ui.overview

import com.android.joinme.model.serie.Serie
import com.android.joinme.model.serie.SerieFilter
import com.android.joinme.model.serie.SeriesRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
 * Unit tests for CreateSerieViewModel.
 *
 * These tests avoid asserting on exact error strings (which can change), and instead assert on
 * invalid* flags, isValid, loading state, and repository interactions.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CreateSerieViewModelTest {

  // ---- Simple fake repo that records added series ----
  private class FakeSeriesRepository : SeriesRepository {
    val added = mutableListOf<Serie>()

    override suspend fun addSerie(serie: Serie) {
      added += serie
    }

    override suspend fun editSerie(serieId: String, newValue: Serie) {
      /* no-op */
    }

    override suspend fun deleteSerie(serieId: String) {
      /* no-op */
    }

    override suspend fun getSerie(serieId: String): Serie =
        added.find { it.serieId == serieId } ?: throw NoSuchElementException("Serie not found")

    override suspend fun getAllSeries(serieFilter: SerieFilter): List<Serie> = added.toList()

    override fun getNewSerieId(): String = "fake-serie-id-1"
  }

  private lateinit var repo: FakeSeriesRepository
  private lateinit var vm: CreateSerieViewModel
  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)

    // Mock Firebase Auth
    mockkStatic(FirebaseAuth::class)
    mockkStatic("com.google.firebase.auth.AuthKt")
    val mockAuth = mockk<FirebaseAuth>(relaxed = true)
    val mockUser = mockk<FirebaseUser>(relaxed = true)

    every { Firebase.auth } returns mockAuth
    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns "test-user-id"

    repo = FakeSeriesRepository()
    vm = CreateSerieViewModel(repo)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
    unmockkAll()
  }

  // ---------- Basic validity ----------

  @Test
  fun initialState_isInvalid() {
    val s = vm.uiState.value
    assertFalse(s.isValid)
    assertFalse(s.isLoading)
  }

  @Test
  fun fillingAllFields_makesFormValid() {
    vm.setTitle("Weekly Soccer")
    vm.setDescription("Weekly soccer games every Friday")
    vm.setDate("25/12/2025")
    vm.setTime("18:00")
    vm.setMaxParticipants("20")
    vm.setVisibility("PUBLIC")

    assertTrue(vm.uiState.value.isValid)
  }

  // ---------- Field validation edges ----------

  @Test
  fun setTitle_blank_marksInvalid() {
    vm.setTitle("")
    val s = vm.uiState.value
    assertNotNull(s.invalidTitleMsg)
    assertFalse(s.isValid)
  }

  @Test
  fun setTitle_nonBlank_marksValid() {
    vm.setTitle("My Serie")
    val s = vm.uiState.value
    assertNull(s.invalidTitleMsg)
    assertEquals("My Serie", s.title)
  }

  @Test
  fun setDescription_blank_marksInvalid() {
    vm.setDescription("")
    val s = vm.uiState.value
    assertNotNull(s.invalidDescriptionMsg)
    assertFalse(s.isValid)
  }

  @Test
  fun setDescription_nonBlank_marksValid() {
    vm.setDescription("A great serie")
    val s = vm.uiState.value
    assertNull(s.invalidDescriptionMsg)
    assertEquals("A great serie", s.description)
  }

  @Test
  fun setMaxParticipants_nonNumeric_marksInvalid() {
    vm.setMaxParticipants("twenty")
    val s = vm.uiState.value
    assertNotNull(s.invalidMaxParticipantsMsg)
    assertFalse(s.isValid)
  }

  @Test
  fun setMaxParticipants_negative_marksInvalid() {
    vm.setMaxParticipants("-5")
    val s = vm.uiState.value
    assertNotNull(s.invalidMaxParticipantsMsg)
    assertFalse(s.isValid)
  }

  @Test
  fun setMaxParticipants_zero_marksInvalid() {
    vm.setMaxParticipants("0")
    val s = vm.uiState.value
    assertNotNull(s.invalidMaxParticipantsMsg)
    assertFalse(s.isValid)
  }

  @Test
  fun setMaxParticipants_positive_marksValid() {
    vm.setMaxParticipants("15")
    val s = vm.uiState.value
    assertNull(s.invalidMaxParticipantsMsg)
    assertEquals("15", s.maxParticipants)
  }

  @Test
  fun setDate_wrongFormat_marksInvalid() {
    vm.setDate("2025-12-25")
    val s = vm.uiState.value
    assertNotNull(s.invalidDateMsg)
    assertFalse(s.isValid)
  }

  @Test
  fun setDate_pastDate_marksInvalid() {
    vm.setDate("01/01/2020")
    val s = vm.uiState.value
    assertNotNull(s.invalidDateMsg)
    assertFalse(s.isValid)
  }

  @Test
  fun setDate_futureDate_marksValid() {
    vm.setDate("25/12/2025")
    val s = vm.uiState.value
    assertNull(s.invalidDateMsg)
    assertEquals("25/12/2025", s.date)
  }

  @Test
  fun setTime_wrongFormat_marksInvalid() {
    vm.setTime("not a time")
    val s = vm.uiState.value
    assertNotNull(s.invalidTimeMsg)
    assertFalse(s.isValid)
  }

  @Test
  fun setTime_validFormat_marksValid() {
    vm.setTime("14:30")
    val s = vm.uiState.value
    assertNull(s.invalidTimeMsg)
    assertEquals("14:30", s.time)
  }

  @Test
  fun setVisibility_blank_marksInvalid() {
    vm.setVisibility("")
    val s = vm.uiState.value
    assertNotNull(s.invalidVisibilityMsg)
    assertFalse(s.isValid)
  }

  @Test
  fun setVisibility_invalid_marksInvalid() {
    vm.setVisibility("SECRET")
    val s = vm.uiState.value
    assertNotNull(s.invalidVisibilityMsg)
    assertFalse(s.isValid)
  }

  @Test
  fun setVisibility_public_marksValid() {
    vm.setVisibility("PUBLIC")
    val s = vm.uiState.value
    assertNull(s.invalidVisibilityMsg)
    assertEquals("PUBLIC", s.visibility)
  }

  @Test
  fun setVisibility_private_marksValid() {
    vm.setVisibility("PRIVATE")
    val s = vm.uiState.value
    assertNull(s.invalidVisibilityMsg)
    assertEquals("PRIVATE", s.visibility)
  }

  @Test
  fun setVisibility_lowercasePublic_marksValid() {
    vm.setVisibility("public")
    val s = vm.uiState.value
    assertNull(s.invalidVisibilityMsg)
    assertEquals("public", s.visibility)
  }

  @Test
  fun setVisibility_lowercasePrivate_marksValid() {
    vm.setVisibility("private")
    val s = vm.uiState.value
    assertNull(s.invalidVisibilityMsg)
    assertEquals("private", s.visibility)
  }

  // ---------- createSerie() behavior ----------

  @Test
  fun createSerie_withInvalidForm_returnsFalse_andDoesNotAdd() = runTest {
    // leave blank => invalid
    val ok = vm.createSerie()
    advanceUntilIdle()

    assertFalse(ok)
    assertTrue(repo.added.isEmpty())
    assertNotNull(vm.uiState.value.errorMsg) // some error is surfaced
    assertFalse(vm.uiState.value.isLoading) // loading should be false after failure
  }

  @Test
  fun createSerie_withInvalidDate_returnsFalse_andDoesNotAdd() = runTest {
    vm.setTitle("Basketball League")
    vm.setDescription("Monthly basketball games")
    vm.setMaxParticipants("10")
    vm.setVisibility("PUBLIC")
    vm.setDate("not a date")
    vm.setTime("19:00")

    val ok = vm.createSerie()
    advanceUntilIdle()

    assertFalse(ok)
    assertTrue(repo.added.isEmpty())
    assertNotNull(vm.uiState.value.errorMsg)
    assertFalse(vm.uiState.value.isLoading)
  }

  @Test
  fun createSerie_withUnparseableDateTime_returnsFalse_andSetsError() = runTest {
    // This tests the date parsing logic in createSerie when the date passes
    // field validation but can't be parsed when combined with time
    vm.setTitle("Test Serie")
    vm.setDescription("Test description")
    vm.setMaxParticipants("10")
    vm.setVisibility("PUBLIC")
    // Set a date that passes basic format but might fail when parsing
    vm.setDate("31/02/2025") // Invalid date (Feb doesn't have 31 days)
    vm.setTime("14:00")

    // This might pass field validation but should fail in createSerie
    val ok = vm.createSerie()
    advanceUntilIdle()

    assertFalse(ok)
    assertTrue(repo.added.isEmpty())
    assertFalse(vm.uiState.value.isLoading)
  }

  @Test
  fun createSerie_withPastDate_returnsFalse_andDoesNotAdd() = runTest {
    vm.setTitle("Past Serie")
    vm.setDescription("This is in the past")
    vm.setMaxParticipants("5")
    vm.setVisibility("PRIVATE")
    vm.setDate("01/01/2020")
    vm.setTime("10:00")

    // Date validation happens in setDate, so form should be invalid
    assertFalse(vm.uiState.value.isValid)

    val ok = vm.createSerie()
    advanceUntilIdle()

    assertFalse(ok)
    assertTrue(repo.added.isEmpty())
  }

  @Test
  fun createSerie_setsLoadingState() = runTest {
    vm.setTitle("Tennis Tournament")
    vm.setDescription("Weekly tennis matches")
    vm.setDate("25/12/2025")
    vm.setTime("16:00")
    vm.setMaxParticipants("8")
    vm.setVisibility("PUBLIC")

    // Initial state
    assertFalse(vm.uiState.value.isLoading)

    // Create serie and verify loading state after completion
    val ok = vm.createSerie()
    advanceUntilIdle()

    // After completion, loading should be false
    assertTrue(ok)
    assertFalse(vm.uiState.value.isLoading)
  }

  // ---------- error clearing ----------

  @Test
  fun clearErrorMsg_resetsErrorField() = runTest {
    // trigger an error
    val ok = vm.createSerie()
    advanceUntilIdle()

    assertFalse(ok)
    assertNotNull(vm.uiState.value.errorMsg)

    vm.clearErrorMsg()
    assertNull(vm.uiState.value.errorMsg)
  }

  @Test
  fun updateFormValidity_clearsErrorMsg_whenFormBecomesValid() = runTest {
    // Set an error first
    vm.createSerie()
    advanceUntilIdle()
    assertNotNull(vm.uiState.value.errorMsg)

    // Fill all fields to make form valid
    vm.setTitle("Valid Serie")
    vm.setDescription("Valid description")
    vm.setDate("25/12/2025")
    vm.setTime("14:00")
    vm.setMaxParticipants("10")
    vm.setVisibility("PUBLIC")

    // After making form valid, error should be cleared
    assertTrue(vm.uiState.value.isValid)
    assertNull(vm.uiState.value.errorMsg)
  }

  // ---------- authentication tests ----------

  @Test
  fun createSerie_withoutAuthentication_returnsFalse() = runTest {
    // Mock unauthenticated user
    val mockAuth = mockk<FirebaseAuth>(relaxed = true)
    every { Firebase.auth } returns mockAuth
    every { mockAuth.currentUser } returns null

    val unauthRepo = FakeSeriesRepository()
    val unauthVm = CreateSerieViewModel(unauthRepo)

    // Fill all fields
    unauthVm.setTitle("Test Serie")
    unauthVm.setDescription("Test description")
    unauthVm.setDate("25/12/2025")
    unauthVm.setTime("14:00")
    unauthVm.setMaxParticipants("10")
    unauthVm.setVisibility("PUBLIC")

    assertTrue(unauthVm.uiState.value.isValid)

    val ok = unauthVm.createSerie()
    advanceUntilIdle()

    assertFalse(ok)
    assertTrue(unauthRepo.added.isEmpty())
    assertNotNull(unauthVm.uiState.value.errorMsg)
  }

  // ---------- repository error handling ----------

  @Test
  fun createSerie_withRepositoryError_returnsFalse_andSetsError() = runTest {
    // Create a repo that throws an exception
    val errorRepo =
        object : SeriesRepository {
          override suspend fun addSerie(serie: Serie) {
            throw RuntimeException("Network error")
          }

          override suspend fun editSerie(serieId: String, newValue: Serie) {}

          override suspend fun deleteSerie(serieId: String) {}

          override suspend fun getSerie(serieId: String): Serie {
            throw NoSuchElementException()
          }

          override suspend fun getAllSeries(serieFilter: SerieFilter): List<Serie> = emptyList()

          override fun getNewSerieId(): String = "fake-id"
        }

    val errorVm = CreateSerieViewModel(errorRepo)

    // Fill all fields
    errorVm.setTitle("Error Serie")
    errorVm.setDescription("This will fail")
    errorVm.setDate("25/12/2025")
    errorVm.setTime("14:00")
    errorVm.setMaxParticipants("10")
    errorVm.setVisibility("PUBLIC")

    assertTrue(errorVm.uiState.value.isValid)

    val ok = errorVm.createSerie()
    advanceUntilIdle()

    assertFalse(ok)
    assertNotNull(errorVm.uiState.value.errorMsg)
    assertFalse(errorVm.uiState.value.isLoading)
  }

  // ---------- Integration-like test ----------

  @Test
  fun createSerie_withValidForm_addsToRepository_andReturnsTrue() = runTest {
    vm.setTitle("Volleyball League")
    vm.setDescription("Monthly volleyball games on the beach")
    vm.setDate("30/12/2025")
    vm.setTime("15:00")
    vm.setMaxParticipants("12")
    vm.setVisibility("PUBLIC")

    assertTrue(vm.uiState.value.isValid)

    val ok = vm.createSerie()
    advanceUntilIdle()

    assertTrue(ok)
    assertEquals(1, repo.added.size)

    val serie = repo.added.first()
    assertEquals("fake-serie-id-1", serie.serieId)
    assertEquals("Volleyball League", serie.title)
    assertEquals("Monthly volleyball games on the beach", serie.description)
    assertEquals(12, serie.maxParticipants)
    assertEquals(0, serie.eventIds.size) // initially empty
    assertEquals("test-user-id", serie.ownerId)
    assertNotNull(serie.date)
    assertFalse(vm.uiState.value.isLoading)
    assertNull(vm.uiState.value.errorMsg)
  }
}
