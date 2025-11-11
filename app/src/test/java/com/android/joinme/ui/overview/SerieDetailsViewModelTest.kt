package com.android.joinme.ui.overview

import com.android.joinme.model.event.Event
import com.android.joinme.model.event.EventType
import com.android.joinme.model.event.EventVisibility
import com.android.joinme.model.event.EventsRepositoryLocal
import com.android.joinme.model.map.Location
import com.android.joinme.model.profile.Profile
import com.android.joinme.model.profile.ProfileRepository
import com.android.joinme.model.serie.Serie
import com.android.joinme.model.serie.SeriesRepositoryLocal
import com.android.joinme.model.utils.Visibility
import com.google.firebase.Timestamp
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class SerieDetailsViewModelTest {

  private lateinit var seriesRepository: SeriesRepositoryLocal
  private lateinit var eventsRepository: EventsRepositoryLocal
  private lateinit var profileRepository: ProfileRepository
  private lateinit var viewModel: SerieDetailsViewModel
  private val testDispatcher = StandardTestDispatcher()

  private fun createTestSerie(
      serieId: String = "test-serie-1",
      ownerId: String = "owner123",
      participants: List<String> = listOf("user1", "user2"),
      maxParticipants: Int = 10,
      eventIds: List<String> = emptyList()
  ): Serie {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DAY_OF_YEAR, 7)

    return Serie(
        serieId = serieId,
        title = "Weekly Basketball",
        description = "Weekly basketball games",
        ownerId = ownerId,
        date = Timestamp(calendar.time),
        visibility = Visibility.PUBLIC,
        eventIds = eventIds,
        participants = participants,
        maxParticipants = maxParticipants)
  }

  private fun createTestEvent(
      eventId: String,
      ownerId: String = "owner123",
      daysFromNow: Int = 7
  ): Event {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DAY_OF_YEAR, daysFromNow)

    return Event(
        eventId = eventId,
        type = EventType.SPORTS,
        title = "Basketball Game",
        description = "Friendly match",
        location = Location(46.5197, 6.6323, "EPFL"),
        date = Timestamp(calendar.time),
        duration = 60,
        participants = listOf(ownerId),
        maxParticipants = 10,
        visibility = EventVisibility.PUBLIC,
        ownerId = ownerId)
  }

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    seriesRepository = SeriesRepositoryLocal()
    eventsRepository = EventsRepositoryLocal()
    profileRepository = mock(ProfileRepository::class.java)
    viewModel = SerieDetailsViewModel(seriesRepository, eventsRepository, profileRepository)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  /** --- LOAD SERIE DETAILS TESTS --- */
  @Test
  fun loadSerieDetails_validSerieId_updatesUIState() = runTest {
    val event1 = createTestEvent("event1")
    val event2 = createTestEvent("event2")
    eventsRepository.addEvent(event1)
    eventsRepository.addEvent(event2)

    val serie = createTestSerie(eventIds = listOf("event1", "event2"))
    seriesRepository.addSerie(serie)

    viewModel.loadSerieDetails(serie.serieId)
    advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertFalse(state.isLoading)
    assertNotNull(state.serie)
    assertEquals("Weekly Basketball", state.serie?.title)
    assertEquals(2, state.events.size)
    assertNull(state.errorMsg)
  }

  @Test
  fun loadSerieDetails_invalidSerieId_setsErrorMessage() = runTest {
    viewModel.loadSerieDetails("non-existent-id")
    advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertFalse(state.isLoading)
    assertNotNull(state.errorMsg)
    assertTrue(state.errorMsg!!.contains("Failed to load serie"))
  }

  @Test
  fun loadSerieDetails_setsLoadingState() = runTest {
    val serie = createTestSerie()
    seriesRepository.addSerie(serie)

    viewModel.loadSerieDetails(serie.serieId)

    val initialState = viewModel.uiState.value
    assertTrue(initialState.isLoading)
  }

  /** --- GET OWNER DISPLAY NAME TESTS --- */
  @Test
  fun getOwnerDisplayName_validOwnerId_returnsUsername() = runTest {
    val mockProfile = Profile(uid = "owner123", username = "JohnDoe", email = "john@example.com")
    whenever(profileRepository.getProfile("owner123")).thenReturn(mockProfile)

    val displayName = viewModel.getOwnerDisplayName("owner123")

    assertEquals("JohnDoe", displayName)
  }

  @Test
  fun getOwnerDisplayName_profileNotFound_returnsUnknown() = runTest {
    whenever(profileRepository.getProfile("unknown-owner")).thenReturn(null)

    val displayName = viewModel.getOwnerDisplayName("unknown-owner")

    assertEquals("UNKNOWN", displayName)
  }

  @Test
  fun getOwnerDisplayName_emptyOwnerId_returnsUnknown() = runTest {
    val displayName = viewModel.getOwnerDisplayName("")

    assertEquals("UNKNOWN", displayName)
  }

  @Test
  fun getOwnerDisplayName_repositoryThrows_returnsUnknown() = runTest {
    whenever(profileRepository.getProfile("error-owner"))
        .thenThrow(RuntimeException("Network error"))

    val displayName = viewModel.getOwnerDisplayName("error-owner")

    assertEquals("UNKNOWN", displayName)
  }

  /** --- JOIN SERIE TESTS --- */
  @Test
  fun joinSerie_validUser_addsToParticipants() = runTest {
    val serie = createTestSerie(participants = listOf("user1"))
    seriesRepository.addSerie(serie)

    viewModel.loadSerieDetails(serie.serieId)
    advanceUntilIdle()

    val success = viewModel.joinSerie("user2")

    assertTrue(success)
    val state = viewModel.uiState.first()
    assertTrue(state.serie?.participants?.contains("user2") == true)
    assertNull(state.errorMsg)
  }

  @Test
  fun joinSerie_owner_fails() = runTest {
    val serie = createTestSerie(ownerId = "owner123")
    seriesRepository.addSerie(serie)

    viewModel.loadSerieDetails(serie.serieId)
    advanceUntilIdle()

    val success = viewModel.joinSerie("owner123")

    assertFalse(success)
    val state = viewModel.uiState.first()
    assertNotNull(state.errorMsg)
    assertTrue(state.errorMsg!!.contains("You are the owner"))
  }

  @Test
  fun joinSerie_alreadyParticipant_fails() = runTest {
    val serie = createTestSerie(participants = listOf("user1"))
    seriesRepository.addSerie(serie)

    viewModel.loadSerieDetails(serie.serieId)
    advanceUntilIdle()

    val success = viewModel.joinSerie("user1")

    assertFalse(success)
    val state = viewModel.uiState.first()
    assertNotNull(state.errorMsg)
    assertTrue(state.errorMsg!!.contains("already a participant"))
  }

  @Test
  fun joinSerie_serieFull_fails() = runTest {
    val serie = createTestSerie(participants = listOf("user1", "user2"), maxParticipants = 2)
    seriesRepository.addSerie(serie)

    viewModel.loadSerieDetails(serie.serieId)
    advanceUntilIdle()

    val success = viewModel.joinSerie("user3")

    assertFalse(success)
    val state = viewModel.uiState.first()
    assertNotNull(state.errorMsg)
    assertTrue(state.errorMsg!!.contains("Serie is full"))
  }

  @Test
  fun joinSerie_serieNotLoaded_fails() = runTest {
    val success = viewModel.joinSerie("user1")

    assertFalse(success)
    val state = viewModel.uiState.first()
    assertNotNull(state.errorMsg)
    assertTrue(state.errorMsg!!.contains("Serie not loaded"))
  }

  /** --- QUIT SERIE TESTS --- */
  @Test
  fun quitSerie_validParticipant_removesFromParticipants() = runTest {
    val serie = createTestSerie(participants = listOf("user1", "user2"))
    seriesRepository.addSerie(serie)

    viewModel.loadSerieDetails(serie.serieId)
    advanceUntilIdle()

    val success = viewModel.quitSerie("user1")

    assertTrue(success)
    val state = viewModel.uiState.first()
    assertFalse(state.serie?.participants?.contains("user1") == true)
    assertTrue(state.serie?.participants?.contains("user2") == true)
    assertNull(state.errorMsg)
  }

  @Test
  fun quitSerie_owner_fails() = runTest {
    val serie = createTestSerie(ownerId = "owner123", participants = listOf("owner123", "user1"))
    seriesRepository.addSerie(serie)

    viewModel.loadSerieDetails(serie.serieId)
    advanceUntilIdle()

    val success = viewModel.quitSerie("owner123")

    assertFalse(success)
    val state = viewModel.uiState.first()
    assertNotNull(state.errorMsg)
    assertTrue(state.errorMsg!!.contains("You are the owner"))
  }

  @Test
  fun quitSerie_notParticipant_fails() = runTest {
    val serie = createTestSerie(participants = listOf("user1"))
    seriesRepository.addSerie(serie)

    viewModel.loadSerieDetails(serie.serieId)
    advanceUntilIdle()

    val success = viewModel.quitSerie("user2")

    assertFalse(success)
    val state = viewModel.uiState.first()
    assertNotNull(state.errorMsg)
    assertTrue(state.errorMsg!!.contains("You are not a participant"))
  }

  @Test
  fun quitSerie_serieNotLoaded_fails() = runTest {
    val success = viewModel.quitSerie("user1")

    assertFalse(success)
    val state = viewModel.uiState.first()
    assertNotNull(state.errorMsg)
    assertTrue(state.errorMsg!!.contains("Serie not loaded"))
  }

  /** --- ERROR MESSAGE MANAGEMENT TESTS --- */
  @Test
  fun setErrorMsg_updatesErrorMessage() = runTest {
    viewModel.setErrorMsg("Test error")

    val state = viewModel.uiState.first()
    assertEquals("Test error", state.errorMsg)
  }

  @Test
  fun clearErrorMsg_removesErrorMessage() = runTest {
    viewModel.setErrorMsg("Test error")
    viewModel.clearErrorMsg()

    val state = viewModel.uiState.first()
    assertNull(state.errorMsg)
  }

  /** --- UI STATE HELPER METHODS TESTS --- */
  @Test
  fun uiState_isOwner_returnsTrue_whenUserIsOwner() {
    val state = SerieDetailsUIState(serie = createTestSerie(ownerId = "user123"))
    assertTrue(state.isOwner("user123"))
  }

  @Test
  fun uiState_isOwner_returnsFalse_whenUserIsNotOwner() {
    val state = SerieDetailsUIState(serie = createTestSerie(ownerId = "owner123"))
    assertFalse(state.isOwner("user456"))
  }

  @Test
  fun uiState_isOwner_returnsFalse_whenUserIdIsNull() {
    val state = SerieDetailsUIState(serie = createTestSerie(ownerId = "owner123"))
    assertFalse(state.isOwner(null))
  }

  @Test
  fun uiState_isParticipant_returnsTrue_whenUserIsParticipant() {
    val state =
        SerieDetailsUIState(
            serie = createTestSerie(participants = listOf("user1", "user2", "user3")))
    assertTrue(state.isParticipant("user2"))
  }

  @Test
  fun uiState_isParticipant_returnsFalse_whenUserIsNotParticipant() {
    val state =
        SerieDetailsUIState(
            serie = createTestSerie(participants = listOf("user1", "user2", "user3")))
    assertFalse(state.isParticipant("user4"))
  }

  @Test
  fun uiState_canJoin_returnsTrue_whenUserCanJoin() {
    val state =
        SerieDetailsUIState(
            serie =
                createTestSerie(
                    ownerId = "owner", participants = listOf("user1"), maxParticipants = 5))
    assertTrue(state.canJoin("user2"))
  }

  @Test
  fun uiState_canJoin_returnsFalse_whenUserIsOwner() {
    val state =
        SerieDetailsUIState(
            serie = createTestSerie(ownerId = "owner", participants = listOf("user1")))
    assertFalse(state.canJoin("owner"))
  }

  @Test
  fun uiState_canJoin_returnsFalse_whenUserIsParticipant() {
    val state =
        SerieDetailsUIState(serie = createTestSerie(participants = listOf("user1", "user2")))
    assertFalse(state.canJoin("user1"))
  }

  @Test
  fun uiState_canJoin_returnsFalse_whenSerieFull() {
    val state =
        SerieDetailsUIState(
            serie = createTestSerie(participants = listOf("user1", "user2"), maxParticipants = 2))
    assertFalse(state.canJoin("user3"))
  }

  /** --- FORMATTED DATA TESTS --- */
  @Test
  fun uiState_formattedDateTime_returnsCorrectFormat() {
    val calendar = Calendar.getInstance()
    calendar.set(2024, Calendar.DECEMBER, 25, 14, 30, 0)

    val serie = createTestSerie().copy(date = Timestamp(calendar.time))
    val state = SerieDetailsUIState(serie = serie)

    assertTrue(state.formattedDateTime.contains("25/12/2024"))
    assertTrue(state.formattedDateTime.contains("14:30"))
  }

  @Test
  fun uiState_participantsCount_returnsCorrectFormat() {
    val serie = createTestSerie(participants = listOf("user1", "user2"), maxParticipants = 5)
    val state = SerieDetailsUIState(serie = serie)

    assertEquals("2/5", state.participantsCount)
  }

  @Test
  fun uiState_visibilityDisplay_returnsCorrectFormat() {
    val serie = createTestSerie().copy(visibility = Visibility.PUBLIC)
    val state = SerieDetailsUIState(serie = serie)

    assertEquals("PUBLIC", state.visibilityDisplay)
  }
}
