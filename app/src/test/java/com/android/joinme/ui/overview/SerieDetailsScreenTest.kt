package com.android.joinme.ui.overview

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.android.joinme.model.event.Event
import com.android.joinme.model.event.EventType
import com.android.joinme.model.event.EventVisibility
import com.android.joinme.model.map.Location
import com.android.joinme.model.serie.Serie
import com.android.joinme.model.utils.Visibility
import com.google.firebase.Timestamp
import java.util.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SerieDetailsScreenTest {

    @get:Rule val composeTestRule = createComposeRule()

    private lateinit var mockViewModel: SerieDetailsViewModel
    private lateinit var uiStateFlow: MutableStateFlow<SerieDetailsUIState>

    private val testSerieId = "test-serie-1"
    private val testUserId = "user123"
    private val testOwnerId = "owner456"

    @Before
    fun setup() {
        uiStateFlow = MutableStateFlow(SerieDetailsUIState())
        mockViewModel = mock(SerieDetailsViewModel::class.java)
        whenever(mockViewModel.uiState).thenReturn(uiStateFlow)
    }

    private fun createTestSerie(
        serieId: String = testSerieId,
        ownerId: String = testOwnerId,
        participants: List<String> = listOf(ownerId, "user1"),
        maxParticipants: Int = 10
    ): Serie {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, 7)
        calendar.set(Calendar.HOUR_OF_DAY, 14)
        calendar.set(Calendar.MINUTE, 30)

        return Serie(
            serieId = serieId,
            title = "Weekly Basketball",
            description = "Weekly basketball sessions",
            ownerId = ownerId,
            date = Timestamp(calendar.time),
            visibility = Visibility.PUBLIC,
            eventIds = listOf("event1", "event2"),
            participants = participants,
            maxParticipants = maxParticipants)
    }

    private fun createTestEvent(eventId: String): Event {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, 7)

        return Event(
            eventId = eventId,
            type = EventType.SPORTS,
            title = "Basketball Game",
            description = "Friendly match",
            location = Location(46.5197, 6.6323, "EPFL"),
            date = Timestamp(calendar.time),
            duration = 60,
            participants = listOf(testOwnerId),
            maxParticipants = 10,
            visibility = EventVisibility.PUBLIC,
            ownerId = testOwnerId)
    }

    @Test
    fun serieDetailsScreen_displaysSerieDetails() {
        val serie = createTestSerie()
        val events = listOf(createTestEvent("event1"), createTestEvent("event2"))

        uiStateFlow.value =
            SerieDetailsUIState(serie = serie, events = events, isLoading = false, errorMsg = null)

        composeTestRule.setContent {
            SerieDetailsScreen(
                serieId = testSerieId,
                serieDetailsViewModel = mockViewModel,
                currentUserId = testUserId)
        }

        composeTestRule.onNodeWithTag(SerieDetailsScreenTestTags.SERIE_TITLE).assertTextEquals("Weekly Basketball")
        composeTestRule.onNodeWithTag(SerieDetailsScreenTestTags.DESCRIPTION).assertTextEquals("Weekly basketball sessions")
        composeTestRule.onNodeWithTag(SerieDetailsScreenTestTags.VISIBILITY).assertTextEquals("PUBLIC")
    }

    @Test
    fun serieDetailsScreen_displaysOwnerName() {
        val serie = createTestSerie()
        uiStateFlow.value = SerieDetailsUIState(serie = serie, isLoading = false)

        composeTestRule.setContent {
            SerieDetailsScreen(
                serieId = testSerieId,
                serieDetailsViewModel = mockViewModel,
                currentUserId = testUserId)
        }

        composeTestRule.onNodeWithTag(SerieDetailsScreenTestTags.OWNER_INFO).assertExists()
    }

    @Test
    fun serieDetailsScreen_ownerSeesAddEventButton() {
        val serie = createTestSerie(ownerId = testUserId)
        uiStateFlow.value = SerieDetailsUIState(serie = serie, isLoading = false)

        composeTestRule.setContent {
            SerieDetailsScreen(
                serieId = testSerieId,
                serieDetailsViewModel = mockViewModel,
                currentUserId = testUserId)
        }
    // Verify owner buttons are shown
    composeTestRule.onNodeWithTag(SerieDetailsScreenTestTags.BUTTON_ADD_EVENT).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SerieDetailsScreenTestTags.EDIT_SERIE_BUTTON).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(SerieDetailsScreenTestTags.BUTTON_ADD_EVENT)
        .assertTextContains("ADD EVENT")
    composeTestRule
        .onNodeWithTag(SerieDetailsScreenTestTags.EDIT_SERIE_BUTTON)
        .assertTextContains("EDIT SERIE")

    // Verify non-owner button is hidden
    composeTestRule.onNodeWithTag(SerieDetailsScreenTestTags.BUTTON_QUIT_SERIE).assertDoesNotExist()

    // Test button callbacks
    composeTestRule.onNodeWithTag(SerieDetailsScreenTestTags.BUTTON_ADD_EVENT).performClick()
    assertTrue(addEventClicked)

    composeTestRule.onNodeWithTag(SerieDetailsScreenTestTags.EDIT_SERIE_BUTTON).performClick()
    assertTrue(editSerieClicked)
  }

  @Test
  fun ownerViewShowsDeleteButton() {
    setup()
    val serie = createTestSerie(ownerId = "owner123")
    fakeSeriesRepo.setSerie(serie)

    val viewModel = createViewModel()

    composeTestRule.setContent {
      SerieDetailsScreen(
          serieId = serie.serieId, serieDetailsViewModel = viewModel, currentUserId = "owner123")
    }

    composeTestRule.waitUntil(timeoutMillis = 3000) {
      composeTestRule
          .onAllNodesWithTag(SerieDetailsScreenTestTags.DELETE_SERIE_BUTTON)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Verify delete button is shown
    composeTestRule
        .onNodeWithTag(SerieDetailsScreenTestTags.DELETE_SERIE_BUTTON)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(SerieDetailsScreenTestTags.DELETE_SERIE_BUTTON)
        .assertTextContains("DELETE SERIE")
  }

  @Test
  fun nonOwnerDoesNotSeeDeleteButton() {
    setup()
    val serie =
        createTestSerie(ownerId = "owner123", participants = listOf("user1", "user2", "owner123"))
    fakeSeriesRepo.setSerie(serie)

    val viewModel = createViewModel()

    composeTestRule.setContent {
      SerieDetailsScreen(
          serieId = serie.serieId, serieDetailsViewModel = viewModel, currentUserId = "user1")
    }

    composeTestRule.waitUntil(timeoutMillis = 3000) {
      composeTestRule
          .onAllNodesWithTag(SerieDetailsScreenTestTags.BUTTON_QUIT_SERIE)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Verify delete button is NOT shown for non-owner
    composeTestRule
        .onNodeWithTag(SerieDetailsScreenTestTags.DELETE_SERIE_BUTTON)
        .assertDoesNotExist()
  }

  @Test
  fun deleteButtonShowsConfirmationDialog() {
    setup()
    val serie = createTestSerie(ownerId = "owner123")
    fakeSeriesRepo.setSerie(serie)

    val viewModel = createViewModel()

    composeTestRule.setContent {
      SerieDetailsScreen(
          serieId = serie.serieId, serieDetailsViewModel = viewModel, currentUserId = "owner123")
    }

    composeTestRule.waitUntil(timeoutMillis = 3000) {
      composeTestRule
          .onAllNodesWithTag(SerieDetailsScreenTestTags.DELETE_SERIE_BUTTON)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Click delete button
    composeTestRule.onNodeWithTag(SerieDetailsScreenTestTags.DELETE_SERIE_BUTTON).performClick()

    composeTestRule.waitForIdle()

    // Verify confirmation dialog appears
    composeTestRule.onNodeWithText("Delete Serie").assertIsDisplayed()
    composeTestRule
        .onNodeWithText("Are you sure you want to delete this serie? This action cannot be undone.")
        .assertIsDisplayed()
    composeTestRule.onNodeWithText("Delete").assertIsDisplayed()
    composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()
  }

  @Test
  fun deleteDialogCancelButtonWorks() {
    setup()
    val serie = createTestSerie(ownerId = "owner123")
    fakeSeriesRepo.setSerie(serie)

    val viewModel = createViewModel()

    composeTestRule.setContent {
      SerieDetailsScreen(
          serieId = serie.serieId, serieDetailsViewModel = viewModel, currentUserId = "owner123")
    }

    composeTestRule.waitUntil(timeoutMillis = 3000) {
      composeTestRule
          .onAllNodesWithTag(SerieDetailsScreenTestTags.DELETE_SERIE_BUTTON)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Click delete button
    composeTestRule.onNodeWithTag(SerieDetailsScreenTestTags.DELETE_SERIE_BUTTON).performClick()

    composeTestRule.waitForIdle()

    // Click cancel
    composeTestRule.onNodeWithText("Cancel").performClick()

    composeTestRule.waitForIdle()

    // Verify dialog is dismissed
    composeTestRule.onNodeWithText("Delete Serie").assertDoesNotExist()

    // Verify serie still exists (wasn't deleted)
    composeTestRule.onNodeWithTag(SerieDetailsScreenTestTags.SCREEN).assertIsDisplayed()
  }

  @Test
  fun deleteSerieSuccessfullyDeletesAndNavigatesBack() {
    setup()
    val serie = createTestSerie(ownerId = "owner123")
    fakeSeriesRepo.setSerie(serie)

    var goBackCalled = false
    val viewModel = createViewModel()

    composeTestRule.setContent {
      SerieDetailsScreen(
          serieId = serie.serieId,
          serieDetailsViewModel = viewModel,
          currentUserId = "owner123",
          onGoBack = { goBackCalled = true })
    }

    composeTestRule.waitUntil(timeoutMillis = 3000) {
      composeTestRule
          .onAllNodesWithTag(SerieDetailsScreenTestTags.DELETE_SERIE_BUTTON)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Click delete button
    composeTestRule.onNodeWithTag(SerieDetailsScreenTestTags.DELETE_SERIE_BUTTON).performClick()

    composeTestRule.waitForIdle()

    // Click delete in confirmation dialog
    composeTestRule.onNodeWithText("Delete").performClick()

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(1000)
    composeTestRule.waitForIdle()

    // Verify onGoBack was called
    assertTrue(goBackCalled)

    // Verify serie was deleted from repository
    runBlocking {
      val allSeries = fakeSeriesRepo.getAllSeries(SerieFilter.SERIES_FOR_OVERVIEW_SCREEN)
      assertTrue(allSeries.none { it.serieId == serie.serieId })
    }
  }

  // ========== Participant Tests ==========

  @Test
  fun participantCanQuitSerieSuccessfully() {
    setup()
    val serie =
        createTestSerie(ownerId = "owner123", participants = listOf("user1", "user2", "owner123"))
    fakeSeriesRepo.setSerie(serie)

    var quitSerieSuccessCalled = false
    val viewModel = createViewModel()

    composeTestRule.setContent {
      SerieDetailsScreen(
          serieId = serie.serieId,
          serieDetailsViewModel = viewModel,
          currentUserId = "user1",
          onQuitSerieSuccess = { quitSerieSuccessCalled = true })
    }

        composeTestRule.onNodeWithTag(SerieDetailsScreenTestTags.BUTTON_ADD_EVENT).assertExists()
        composeTestRule.onNodeWithTag(SerieDetailsScreenTestTags.EDIT_SERIE_BUTTON).assertExists()
        composeTestRule.onNodeWithTag(SerieDetailsScreenTestTags.BUTTON_QUIT_SERIE).assertDoesNotExist()
    }

    @Test
    fun serieDetailsScreen_participantSeesQuitButton() {
        val serie = createTestSerie(participants = listOf(testOwnerId, testUserId))
        uiStateFlow.value = SerieDetailsUIState(serie = serie, isLoading = false)

        composeTestRule.setContent {
            SerieDetailsScreen(
                serieId = testSerieId,
                serieDetailsViewModel = mockViewModel,
                currentUserId = testUserId)
        }

        composeTestRule.onNodeWithTag(SerieDetailsScreenTestTags.BUTTON_QUIT_SERIE).assertExists()
        composeTestRule.onNodeWithTag(SerieDetailsScreenTestTags.BUTTON_QUIT_SERIE).assertTextContains("QUIT SERIE")
        composeTestRule.onNodeWithTag(SerieDetailsScreenTestTags.BUTTON_ADD_EVENT).assertDoesNotExist()
    }

    @Test
    fun serieDetailsScreen_nonParticipantSeesJoinButton() {
        val serie = createTestSerie(participants = listOf(testOwnerId), maxParticipants = 10)
        uiStateFlow.value = SerieDetailsUIState(serie = serie, isLoading = false)

        composeTestRule.setContent {
            SerieDetailsScreen(
                serieId = testSerieId,
                serieDetailsViewModel = mockViewModel,
                currentUserId = testUserId)
        }

        composeTestRule.onNodeWithTag(SerieDetailsScreenTestTags.BUTTON_QUIT_SERIE).assertExists()
        composeTestRule.onNodeWithTag(SerieDetailsScreenTestTags.BUTTON_QUIT_SERIE).assertTextContains("JOIN SERIE")
    }

    @Test
    fun serieDetailsScreen_fullSerie_showsFullMessage() {
        val serie = createTestSerie(participants = listOf(testOwnerId, "user1"), maxParticipants = 2)
        uiStateFlow.value = SerieDetailsUIState(serie = serie, isLoading = false)

        composeTestRule.setContent {
            SerieDetailsScreen(
                serieId = testSerieId,
                serieDetailsViewModel = mockViewModel,
                currentUserId = testUserId)
        }

        composeTestRule.onNodeWithTag(SerieDetailsScreenTestTags.MESSAGE_FULL_SERIE).assertExists()
        composeTestRule.onNodeWithTag(SerieDetailsScreenTestTags.MESSAGE_FULL_SERIE).assertTextEquals("Sorry this serie is full")
        composeTestRule.onNodeWithTag(SerieDetailsScreenTestTags.BUTTON_QUIT_SERIE).assertDoesNotExist()
    }

    @Test
    fun serieDetailsScreen_loadingState_showsLoadingIndicator() {
        uiStateFlow.value = SerieDetailsUIState(isLoading = true)

        composeTestRule.setContent {
            SerieDetailsScreen(
                serieId = testSerieId,
                serieDetailsViewModel = mockViewModel,
                currentUserId = testUserId)
        }

        composeTestRule.onNodeWithTag(SerieDetailsScreenTestTags.LOADING).assertExists()
    }

    @Test
    fun serieDetailsScreen_displaysEventList() {
        val serie = createTestSerie()
        val events = listOf(createTestEvent("event1"), createTestEvent("event2"))

        uiStateFlow.value =
            SerieDetailsUIState(serie = serie, events = events, isLoading = false)

        composeTestRule.setContent {
            SerieDetailsScreen(
                serieId = testSerieId,
                serieDetailsViewModel = mockViewModel,
                currentUserId = testUserId)
        }

        composeTestRule.onNodeWithTag(SerieDetailsScreenTestTags.EVENT_LIST).assertExists()
        composeTestRule.onNodeWithTag("${SerieDetailsScreenTestTags.EVENT_CARD}_event1").assertExists()
        composeTestRule.onNodeWithTag("${SerieDetailsScreenTestTags.EVENT_CARD}_event2").assertExists()
    }

    @Test
    fun serieDetailsScreen_emptyEventList_showsEmptyMessage() {
        val serie = createTestSerie()
        uiStateFlow.value =
            SerieDetailsUIState(serie = serie, events = emptyList(), isLoading = false)

        composeTestRule.setContent {
            SerieDetailsScreen(
                serieId = testSerieId,
                serieDetailsViewModel = mockViewModel,
                currentUserId = testUserId)
        }

        composeTestRule.onNodeWithTag(SerieDetailsScreenTestTags.EVENT_LIST).assertExists()
        composeTestRule.onNode(hasText("No events in this serie yet")).assertExists()
    }
}