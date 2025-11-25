package com.android.joinme.ui.overview

import com.android.joinme.model.event.EventType
import com.android.joinme.model.groups.Group
import com.android.joinme.model.groups.GroupRepository
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
      added.removeIf { it.serieId == serieId }
    }

    override suspend fun getSerie(serieId: String): Serie =
        added.find { it.serieId == serieId } ?: throw NoSuchElementException("Serie not found")

    override suspend fun getAllSeries(serieFilter: SerieFilter): List<Serie> = added.toList()

    override suspend fun getSeriesByIds(seriesIds: List<String>): List<Serie> {
      return added.filter { seriesIds.contains(it.serieId) }
    }

    override fun getNewSerieId(): String = "fake-serie-id-1"
  }

  // ---- Simple fake group repo ----
  private class FakeGroupRepository : GroupRepository {
    val groups = mutableListOf<Group>()
    private var idCounter = 1

    override fun getNewGroupId(): String = "group-${idCounter++}"

    override suspend fun getAllGroups(): List<Group> = groups.toList()

    override suspend fun getGroup(groupId: String): Group =
        groups.find { it.id == groupId } ?: throw NoSuchElementException("Group not found")

    override suspend fun addGroup(group: Group) {
      groups += group
    }

    override suspend fun editGroup(groupId: String, newValue: Group) {
      val index = groups.indexOfFirst { it.id == groupId }
      if (index != -1) {
        groups[index] = newValue
      }
    }

    override suspend fun deleteGroup(groupId: String, userId: String) {
      groups.removeIf { it.id == groupId }
    }

    override suspend fun leaveGroup(groupId: String, userId: String) {}

    override suspend fun joinGroup(groupId: String, userId: String) {}
  }

  private lateinit var repo: FakeSeriesRepository
  private lateinit var groupRepo: FakeGroupRepository
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
    groupRepo = FakeGroupRepository()
    vm = CreateSerieViewModel(repo, groupRepo)
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
  fun setDate_todayDate_marksValid() {
    val calendar = java.util.Calendar.getInstance()
    val today =
        String.format(
            "%02d/%02d/%04d",
            calendar.get(java.util.Calendar.DAY_OF_MONTH),
            calendar.get(java.util.Calendar.MONTH) + 1,
            calendar.get(java.util.Calendar.YEAR))
    vm.setDate(today)
    val s = vm.uiState.value
    assertNull(s.invalidDateMsg)
    assertEquals(today, s.date)
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
  fun setTime_withTodayDateAndPastTime_marksInvalid() {
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
    val s = vm.uiState.value
    assertNotNull(s.invalidTimeMsg)
    assertFalse(s.isValid)
  }

  @Test
  fun setTime_withTodayDateAndFutureTime_marksValid() {
    // Set today's date
    val calendar = java.util.Calendar.getInstance()
    val today =
        String.format(
            "%02d/%02d/%04d",
            calendar.get(java.util.Calendar.DAY_OF_MONTH),
            calendar.get(java.util.Calendar.MONTH) + 1,
            calendar.get(java.util.Calendar.YEAR))
    vm.setDate(today)

    // Set a time that's definitely in the future (23:59)
    vm.setTime("23:59")
    val s = vm.uiState.value
    assertNull(s.invalidTimeMsg)
    assertEquals("23:59", s.time)
  }

  @Test
  fun setTime_withFutureDateAndPastTime_marksValid() {
    // Set a future date
    vm.setDate("25/12/2025")

    // Even if time is "in the past" (e.g., 00:00), combined date-time is in the future
    vm.setTime("00:00")
    val s = vm.uiState.value
    assertNull(s.invalidTimeMsg)
    assertEquals("00:00", s.time)
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
    val serieId = vm.createSerie()
    advanceUntilIdle()

    assertNull(serieId)
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

    val serieId = vm.createSerie()
    advanceUntilIdle()

    assertNull(serieId)
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
    val serieId = vm.createSerie()
    advanceUntilIdle()

    assertNull(serieId)
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

    val serieId = vm.createSerie()
    advanceUntilIdle()

    assertNull(serieId)
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
    val serieId = vm.createSerie()
    advanceUntilIdle()

    // After completion, loading should be false
    assertNotNull(serieId)
    assertFalse(vm.uiState.value.isLoading)
  }

  // ---------- error clearing ----------

  @Test
  fun clearErrorMsg_resetsErrorField() = runTest {
    // trigger an error
    val serieId = vm.createSerie()
    advanceUntilIdle()

    assertNull(serieId)
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
    val unauthGroupRepo = FakeGroupRepository()
    val unauthVm = CreateSerieViewModel(unauthRepo, unauthGroupRepo)
    advanceUntilIdle() // Wait for init block to complete

    // Fill all fields
    unauthVm.setTitle("Test Serie")
    unauthVm.setDescription("Test description")
    unauthVm.setDate("25/12/2025")
    unauthVm.setTime("14:00")
    unauthVm.setMaxParticipants("10")
    unauthVm.setVisibility("PUBLIC")

    assertTrue(unauthVm.uiState.value.isValid)

    val serieId = unauthVm.createSerie()
    advanceUntilIdle()

    assertNull(serieId)
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

          override suspend fun getSeriesByIds(seriesIds: List<String>): List<Serie> {
            return emptyList()
          }

          override suspend fun getSerie(serieId: String): Serie {
            throw NoSuchElementException()
          }

          override suspend fun getAllSeries(serieFilter: SerieFilter): List<Serie> = emptyList()

          override fun getNewSerieId(): String = "fake-id"
        }

    val errorGroupRepo = FakeGroupRepository()
    val errorVm = CreateSerieViewModel(errorRepo, errorGroupRepo)
    advanceUntilIdle() // Wait for init block to complete

    // Fill all fields
    errorVm.setTitle("Error Serie")
    errorVm.setDescription("This will fail")
    errorVm.setDate("25/12/2025")
    errorVm.setTime("14:00")
    errorVm.setMaxParticipants("10")
    errorVm.setVisibility("PUBLIC")

    assertTrue(errorVm.uiState.value.isValid)

    val serieId = errorVm.createSerie()
    advanceUntilIdle()

    assertNull(serieId)
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

    val serieId = vm.createSerie()
    advanceUntilIdle()

    assertNotNull(serieId)
    assertEquals("fake-serie-id-1", serieId)
    assertEquals(1, repo.added.size)

    val serie = repo.added.first()
    assertEquals("fake-serie-id-1", serie.serieId)
    assertEquals("Volleyball League", serie.title)
    assertEquals("Monthly volleyball games on the beach", serie.description)
    assertEquals(12, serie.maxParticipants)
    assertEquals(0, serie.eventIds.size) // initially empty
    assertEquals("test-user-id", serie.ownerId)
    assertEquals(listOf("test-user-id"), serie.participants) // owner should be in participants
    assertNotNull(serie.date)
    assertFalse(vm.uiState.value.isLoading)
    assertNull(vm.uiState.value.errorMsg)
  }

  // ---------- Duplicate prevention tests ----------

  /** Helper function to set up a valid serie form for duplicate prevention tests */
  private fun setupValidSerieForm() {
    vm.setTitle("Test Serie")
    vm.setDescription("Test description")
    vm.setDate("25/12/2025")
    vm.setTime("18:00")
    vm.setMaxParticipants("10")
    vm.setVisibility("PUBLIC")
  }

  @Test
  fun createSerie_calledTwice_returnsSameId_withoutCreatingDuplicate() = runTest {
    setupValidSerieForm()

    // Initially, createdSerieId should be null
    assertNull(vm.uiState.value.createdSerieId)

    // First call - creates the serie
    val firstSerieId = vm.createSerie()
    advanceUntilIdle()

    assertNotNull(firstSerieId)
    assertEquals("fake-serie-id-1", firstSerieId)
    assertEquals("fake-serie-id-1", vm.uiState.value.createdSerieId)
    assertEquals(1, repo.added.size)

    // Second call - should return the same ID without creating a duplicate
    val secondSerieId = vm.createSerie()
    advanceUntilIdle()

    assertNotNull(secondSerieId)
    assertEquals("fake-serie-id-1", secondSerieId)
    assertEquals(firstSerieId, secondSerieId)
    // Verify no duplicate was created
    assertEquals(1, repo.added.size)
  }

  @Test
  fun createSerie_afterFailure_doesNotStoreCreatedSerieId() = runTest {
    // Don't fill all fields - form is invalid
    vm.setTitle("Test Serie")

    assertFalse(vm.uiState.value.isValid)

    val serieId = vm.createSerie()
    advanceUntilIdle()

    // Creation should fail
    assertNull(serieId)
    // createdSerieId should remain null
    assertNull(vm.uiState.value.createdSerieId)
    assertTrue(repo.added.isEmpty())
  }

  @Test
  fun createSerie_storesCreatedSerieId_forRenavigation() = runTest {
    setupValidSerieForm()

    val serieId = vm.createSerie()
    advanceUntilIdle()

    // Verify createdSerieId is stored to prevent duplicate creation on re-navigation
    assertNotNull(serieId)
    assertEquals("fake-serie-id-1", vm.uiState.value.createdSerieId)
  }

  // ---------- Delete created serie on goBack tests ----------

  @Test
  fun deleteCreatedSerieIfExists_withNoCreatedSerie_doesNothing() = runTest {
    // No serie created, createdSerieId is null
    assertNull(vm.uiState.value.createdSerieId)

    vm.deleteCreatedSerieIfExists()
    advanceUntilIdle()

    // No exceptions should be thrown
    assertTrue(repo.added.isEmpty())
  }

  @Test
  fun deleteCreatedSerieIfExists_withSerieWithNoEvents_deletesSerie() = runTest {
    setupValidSerieForm()

    // Create serie
    val serieId = vm.createSerie()
    advanceUntilIdle()

    assertNotNull(serieId)
    assertEquals(1, repo.added.size)

    // Delete it (simulating going back before creating event)
    vm.deleteCreatedSerieIfExists()
    advanceUntilIdle()

    // Serie should be deleted
    assertTrue(repo.added.isEmpty())
  }

  @Test
  fun deleteCreatedSerieIfExists_withSerieWithEvents_doesNotDelete() = runTest {
    setupValidSerieForm()

    // Create serie
    val serieId = vm.createSerie()
    advanceUntilIdle()

    assertNotNull(serieId)
    assertEquals(1, repo.added.size)

    // Simulate adding an event to the serie
    val serie = repo.added.first()
    val updatedSerie = serie.copy(eventIds = listOf("event-1"))
    repo.added[0] = updatedSerie

    // Try to delete (should not delete because serie has events)
    vm.deleteCreatedSerieIfExists()
    advanceUntilIdle()

    // Serie should NOT be deleted
    assertEquals(1, repo.added.size)
  }

  // ---------- Group selection tests ----------

  @Test
  fun groupSelection_loadsGroupsOnInit_autoFillsFieldsForGroup_createsSerieWithGroupId() = runTest {
    // Setup: Add test groups
    val sportsGroup =
        Group(
            id = "group-1",
            name = "Basketball Club",
            category = EventType.SPORTS,
            description = "Weekly basketball games",
            ownerId = "test-user-id",
            memberIds = listOf("test-user-id", "user-2", "user-3"))
    groupRepo.groups.add(sportsGroup)

    val socialGroup =
        Group(
            id = "group-2",
            name = "Movie Night",
            category = EventType.SOCIAL,
            description = "Monthly movie nights",
            ownerId = "test-user-id",
            memberIds = listOf("test-user-id", "user-4"))
    groupRepo.groups.add(socialGroup)

    // Create new ViewModel and load groups
    vm = CreateSerieViewModel(repo, groupRepo)
    vm.loadUserGroups()
    advanceUntilIdle()

    // Verify groups are loaded
    assertEquals(2, vm.uiState.value.availableGroups.size)
    assertNull(vm.uiState.value.selectedGroupId)

    // Test 1: Select group auto-fills fields
    vm.setSelectedGroup("group-1")
    val stateAfterSelection = vm.uiState.value
    assertEquals("group-1", stateAfterSelection.selectedGroupId)
    assertEquals("300", stateAfterSelection.maxParticipants) // Default group max
    assertEquals("PRIVATE", stateAfterSelection.visibility)
    assertNull(stateAfterSelection.invalidMaxParticipantsMsg)
    assertNull(stateAfterSelection.invalidVisibilityMsg)

    // Test 2: Create serie with group
    vm.setTitle("Basketball Tournament")
    vm.setDescription("Weekly tournament series")
    vm.setDate("25/12/2025")
    vm.setTime("18:00")

    assertTrue(vm.uiState.value.isValid)

    val serieId = vm.createSerie()
    advanceUntilIdle()

    assertNotNull(serieId)
    assertEquals(1, repo.added.size)

    // Test 3: Verify serie properties
    val createdSerie = repo.added.first()
    assertEquals("group-1", createdSerie.groupId)
    assertEquals(300, createdSerie.maxParticipants)
    assertEquals(
        listOf("test-user-id", "user-2", "user-3"), createdSerie.participants) // Group members

    // Test 4: Verify group's serieIds updated
    val updatedGroup = groupRepo.groups.find { it.id == "group-1" }
    assertNotNull(updatedGroup)
    assertTrue(updatedGroup!!.serieIds.contains(serieId))

    // Test 5: Deselect group clears fields
    vm.setSelectedGroup(null)
    val stateAfterDeselection = vm.uiState.value
    assertNull(stateAfterDeselection.selectedGroupId)
    assertEquals("", stateAfterDeselection.maxParticipants)
    assertEquals("", stateAfterDeselection.visibility)
  }

  @Test
  fun groupSelection_standaloneSerie_requiresManualFieldsAndNoGroupId() = runTest {
    // Don't select a group
    assertNull(vm.uiState.value.selectedGroupId)

    // Fill all fields manually
    vm.setTitle("Standalone Serie")
    vm.setDescription("No group required")
    vm.setDate("25/12/2025")
    vm.setTime("14:00")
    vm.setMaxParticipants("15")
    vm.setVisibility("PUBLIC")

    assertTrue(vm.uiState.value.isValid)

    val serieId = vm.createSerie()
    advanceUntilIdle()

    assertNotNull(serieId)
    val serie = repo.added.first()
    assertNull(serie.groupId) // No group
    assertEquals(15, serie.maxParticipants)
    assertEquals(listOf("test-user-id"), serie.participants) // Only owner
  }

  @Test
  fun groupSelection_switchingBetweenGroups_updatesFieldsCorrectly() = runTest {
    val group1 =
        Group(
            id = "group-1",
            name = "Group 1",
            category = EventType.SPORTS,
            ownerId = "test-user-id",
            memberIds = listOf("test-user-id", "user-2"))
    val group2 =
        Group(
            id = "group-2",
            name = "Group 2",
            category = EventType.SOCIAL,
            ownerId = "test-user-id",
            memberIds = listOf("test-user-id", "user-3", "user-4"))
    groupRepo.groups.addAll(listOf(group1, group2))

    vm = CreateSerieViewModel(repo, groupRepo)
    vm.loadUserGroups()
    advanceUntilIdle()

    // Select first group
    vm.setSelectedGroup("group-1")
    assertEquals("group-1", vm.uiState.value.selectedGroupId)
    assertEquals("300", vm.uiState.value.maxParticipants)

    // Switch to second group
    vm.setSelectedGroup("group-2")
    assertEquals("group-2", vm.uiState.value.selectedGroupId)
    assertEquals("300", vm.uiState.value.maxParticipants) // Still auto-filled

    // Deselect (go standalone)
    vm.setSelectedGroup(null)
    assertNull(vm.uiState.value.selectedGroupId)
    assertEquals("", vm.uiState.value.maxParticipants) // Cleared
    assertEquals("", vm.uiState.value.visibility) // Cleared
  }

  @Test
  fun groupSelection_groupRepositoryError_handlesGracefully() = runTest {
    val errorGroupRepo =
        object : GroupRepository {
          override fun getNewGroupId(): String = "group-id"

          override suspend fun getAllGroups(): List<Group> {
            throw RuntimeException("Network error")
          }

          override suspend fun getGroup(groupId: String): Group {
            throw NoSuchElementException("Group not found")
          }

          override suspend fun addGroup(group: Group) {}

          override suspend fun editGroup(groupId: String, newValue: Group) {}

          override suspend fun deleteGroup(groupId: String, userId: String) {}

          override suspend fun leaveGroup(groupId: String, userId: String) {}

          override suspend fun joinGroup(groupId: String, userId: String) {}
        }

    val errorVm = CreateSerieViewModel(repo, errorGroupRepo)
    errorVm.loadUserGroups()
    advanceUntilIdle()

    // Groups should be empty due to error (logged but not thrown)
    assertTrue(errorVm.uiState.value.availableGroups.isEmpty())
  }

  // ---------- Transaction-like behavior and rollback tests ----------

  @Test
  fun createSerie_withGroup_transactionBehavior() = runTest {
    // Test 1: Group update failure prevents serie creation
    val repo1 = FakeSeriesRepository()
    val groupRepo1 = FakeGroupRepository()
    val failingGroupRepo =
        object : GroupRepository by groupRepo1 {
          override suspend fun editGroup(groupId: String, newValue: Group) {
            throw RuntimeException("Group update failed")
          }
        }
    val vmWithFailingGroup = CreateSerieViewModel(repo1, failingGroupRepo)

    val group1 =
        Group(
            id = "group-1",
            name = "Test Group",
            category = EventType.SPORTS,
            ownerId = "test-user-id",
            memberIds = listOf("test-user-id", "user-2"),
            serieIds = emptyList())
    groupRepo1.groups.add(group1)
    vmWithFailingGroup.loadUserGroups()
    advanceUntilIdle()

    vmWithFailingGroup.setSelectedGroup("group-1")
    vmWithFailingGroup.setTitle("Test Serie")
    vmWithFailingGroup.setDescription("Test description")
    vmWithFailingGroup.setDate("25/12/2025")
    vmWithFailingGroup.setTime("18:00")

    val serieId1 = vmWithFailingGroup.createSerie()
    advanceUntilIdle()

    assertNull(serieId1)
    assertTrue(repo1.added.isEmpty())
    assertNotNull(vmWithFailingGroup.uiState.value.errorMsg)
    assertFalse(vmWithFailingGroup.uiState.value.isLoading)

    // Test 2: Serie creation failure rolls back group update
    val groupRepo2 = FakeGroupRepository()
    val failingSeriesRepo =
        object : SeriesRepository {
          override suspend fun addSerie(serie: Serie) {
            throw RuntimeException("Serie creation failed")
          }

          override suspend fun editSerie(serieId: String, newValue: Serie) {}

          override suspend fun deleteSerie(serieId: String) {}

          override suspend fun getSerie(serieId: String): Serie {
            throw NoSuchElementException()
          }

          override suspend fun getAllSeries(serieFilter: SerieFilter): List<Serie> = emptyList()

          override suspend fun getSeriesByIds(seriesIds: List<String>): List<Serie> = emptyList()

          override fun getNewSerieId(): String = "serie-1"
        }

    val vmWithFailingSeries = CreateSerieViewModel(failingSeriesRepo, groupRepo2)

    val group2 =
        Group(
            id = "group-1",
            name = "Test Group",
            category = EventType.SPORTS,
            ownerId = "test-user-id",
            memberIds = listOf("test-user-id", "user-2"),
            serieIds = emptyList())
    groupRepo2.groups.add(group2)
    vmWithFailingSeries.loadUserGroups()
    advanceUntilIdle()

    vmWithFailingSeries.setSelectedGroup("group-1")
    vmWithFailingSeries.setTitle("Test Serie")
    vmWithFailingSeries.setDescription("Test description")
    vmWithFailingSeries.setDate("25/12/2025")
    vmWithFailingSeries.setTime("18:00")

    val initialGroupState = groupRepo2.groups.first()
    assertEquals(0, initialGroupState.serieIds.size)

    val serieId2 = vmWithFailingSeries.createSerie()
    advanceUntilIdle()

    assertNull(serieId2)
    assertNotNull(vmWithFailingSeries.uiState.value.errorMsg)
    val groupAfterRollback = groupRepo2.groups.first()
    assertEquals(0, groupAfterRollback.serieIds.size)

    // Test 3: Success case - both group and serie are updated
    val repo3 = FakeSeriesRepository()
    val groupRepo3 = FakeGroupRepository()
    val vmSuccess = CreateSerieViewModel(repo3, groupRepo3)

    val group3 =
        Group(
            id = "group-1",
            name = "Test Group",
            category = EventType.SPORTS,
            ownerId = "test-user-id",
            memberIds = listOf("test-user-id", "user-2"),
            serieIds = emptyList())
    groupRepo3.groups.add(group3)
    vmSuccess.loadUserGroups()
    advanceUntilIdle()

    vmSuccess.setSelectedGroup("group-1")
    vmSuccess.setTitle("Test Serie")
    vmSuccess.setDescription("Test description")
    vmSuccess.setDate("25/12/2025")
    vmSuccess.setTime("18:00")

    val serieId3 = vmSuccess.createSerie()
    advanceUntilIdle()

    assertNotNull(serieId3)
    assertEquals(1, repo3.added.size)
    val createdSerie = repo3.added.first()
    assertEquals("group-1", createdSerie.groupId)
    val updatedGroup = groupRepo3.groups.first()
    assertTrue(updatedGroup.serieIds.contains(serieId3))
  }

  @Test
  fun deleteCreatedSerieIfExists_groupCleanup() = runTest {
    // Setup: Add a group
    val group =
        Group(
            id = "group-1",
            name = "Test Group",
            category = EventType.SPORTS,
            ownerId = "test-user-id",
            memberIds = listOf("test-user-id"),
            serieIds = emptyList())
    groupRepo.groups.add(group)

    vm.loadUserGroups()
    advanceUntilIdle()

    // Test 1: With group - removes serie from group
    vm.setSelectedGroup("group-1")
    vm.setTitle("Test Serie")
    vm.setDescription("Test description")
    vm.setDate("25/12/2025")
    vm.setTime("18:00")

    val serieId1 = vm.createSerie()
    advanceUntilIdle()

    assertNotNull(serieId1)
    val groupBefore = groupRepo.groups.first()
    assertTrue(groupBefore.serieIds.contains(serieId1))

    vm.deleteCreatedSerieIfExists()
    advanceUntilIdle()

    assertTrue(repo.added.isEmpty())
    val groupAfterDelete = groupRepo.groups.first()
    assertFalse(groupAfterDelete.serieIds.contains(serieId1))

    // Test 2: Without group - does not affect groups
    vm = CreateSerieViewModel(repo, groupRepo)
    vm.loadUserGroups()
    advanceUntilIdle()

    vm.setTitle("Standalone Serie")
    vm.setDescription("No group")
    vm.setDate("25/12/2025")
    vm.setTime("18:00")
    vm.setMaxParticipants("10")
    vm.setVisibility("PUBLIC")

    val serieId2 = vm.createSerie()
    advanceUntilIdle()

    assertNotNull(serieId2)
    assertNull(vm.uiState.value.selectedGroupId)

    vm.deleteCreatedSerieIfExists()
    advanceUntilIdle()

    assertTrue(repo.added.isEmpty())
    val groupUnaffected = groupRepo.groups.first()
    assertEquals(0, groupUnaffected.serieIds.size)
  }

  // ---------- loadUserGroups() tests ----------

  @Test
  fun loadUserGroups_refreshesGroupList() = runTest {
    // Initially no groups
    vm.loadUserGroups()
    advanceUntilIdle()
    assertEquals(0, vm.uiState.value.availableGroups.size)

    // Add groups to repo
    val group1 =
        Group(
            id = "group-1",
            name = "Group 1",
            category = EventType.SPORTS,
            ownerId = "test-user-id",
            memberIds = listOf("test-user-id"))
    groupRepo.groups.add(group1)

    // Reload groups
    vm.loadUserGroups()
    advanceUntilIdle()
    assertEquals(1, vm.uiState.value.availableGroups.size)
    assertEquals("group-1", vm.uiState.value.availableGroups.first().id)

    // Add another group
    val group2 =
        Group(
            id = "group-2",
            name = "Group 2",
            category = EventType.SOCIAL,
            ownerId = "test-user-id",
            memberIds = listOf("test-user-id"))
    groupRepo.groups.add(group2)

    // Reload again
    vm.loadUserGroups()
    advanceUntilIdle()
    assertEquals(2, vm.uiState.value.availableGroups.size)
  }
}
