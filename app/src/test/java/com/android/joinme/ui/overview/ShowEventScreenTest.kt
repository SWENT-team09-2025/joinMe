package com.android.joinme.ui.overview

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.android.joinme.model.event.Event
import com.android.joinme.model.event.EventType
import com.android.joinme.model.event.EventVisibility
import com.android.joinme.model.event.EventsRepositoryLocal
import com.android.joinme.model.map.Location
import com.android.joinme.model.profile.ProfileRepository
import com.google.firebase.Timestamp
import java.util.Calendar
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ShowEventScreenTest {

    @get:Rule val composeTestRule = createComposeRule()

    private lateinit var mockViewModel: ShowEventViewModel
    private lateinit var mockRepository: EventsRepositoryLocal
    private lateinit var mockProfileRepository: ProfileRepository
    private lateinit var uiStateFlow: MutableStateFlow<ShowEventUIState>

    private val testEventId = "test-event-1"
    private val testUserId = "user123"
    private val testOwnerId = "owner456"

    @Before
    fun setup() {
        mockRepository = EventsRepositoryLocal()
        mockProfileRepository = mock(ProfileRepository::class.java)
        uiStateFlow = MutableStateFlow(ShowEventUIState())
        mockViewModel = mock(ShowEventViewModel::class.java)
        whenever(mockViewModel.uiState).thenReturn(uiStateFlow)
    }

    private fun createTestEvent(
        eventId: String = testEventId,
        ownerId: String = testOwnerId,
        participants: List<String> = listOf(ownerId, "user1"),
        isPast: Boolean = false
    ): Event {
        val calendar = Calendar.getInstance()
        if (isPast) {
            calendar.add(Calendar.DAY_OF_YEAR, -7)
        } else {
            calendar.add(Calendar.DAY_OF_YEAR, 7)
        }

        return Event(
            eventId = eventId,
            type = EventType.SPORTS,
            title = "Basketball Game",
            description = "Friendly basketball match",
            location = Location(46.5197, 6.6323, "EPFL"),
            date = Timestamp(calendar.time),
            duration = 90,
            participants = participants,
            maxParticipants = 10,
            visibility = EventVisibility.PUBLIC,
            ownerId = ownerId)
    }

    @Test
    fun showEventScreen_displaysEventDetails() {
        uiStateFlow.value =
            ShowEventUIState(
                type = "SPORTS",
                title = "Basketball Game",
                description = "Friendly basketball match",
                location = "EPFL",
                maxParticipants = "10",
                participantsCount = "5",
                duration = "90",
                date = "SPORTS: 25/12/2024 14:30",
                visibility = "PUBLIC",
                ownerId = testOwnerId,
                ownerName = "Created by John Doe",
                participants = listOf(testOwnerId, "user1"),
                isPastEvent = false)

        composeTestRule.setContent {
            ShowEventScreen(
                eventId = testEventId, currentUserId = testUserId, showEventViewModel = mockViewModel)
        }

        composeTestRule.onNodeWithTag(ShowEventScreenTestTags.EVENT_TITLE).assertTextEquals("Basketball Game")
        composeTestRule.onNodeWithTag(ShowEventScreenTestTags.EVENT_TYPE).assertTextEquals("SPORTS")
        composeTestRule.onNodeWithTag(ShowEventScreenTestTags.EVENT_DESCRIPTION).assertTextEquals("Friendly basketball match")
        composeTestRule.onNodeWithTag(ShowEventScreenTestTags.EVENT_LOCATION).assertTextEquals("EPFL")
        composeTestRule.onNodeWithTag(ShowEventScreenTestTags.EVENT_VISIBILITY).assertTextEquals("PUBLIC")
        composeTestRule.onNodeWithTag(ShowEventScreenTestTags.EVENT_MEMBERS).assertTextEquals("MEMBERS : 5/10")
        composeTestRule.onNodeWithTag(ShowEventScreenTestTags.EVENT_DURATION).assertTextEquals("90min")
        composeTestRule.onNodeWithTag(ShowEventScreenTestTags.EVENT_DATE).assertTextEquals("SPORTS: 25/12/2024 14:30")
        composeTestRule.onNodeWithTag(ShowEventScreenTestTags.EVENT_OWNER).assertTextEquals("Created by John Doe")
    }

    @Test
    fun showEventScreen_ownerSeesEditAndDeleteButtons() {
        uiStateFlow.value =
            ShowEventUIState(
                type = "SPORTS",
                title = "Basketball Game",
                ownerId = testOwnerId,
                ownerName = "Created by EventOwner",
                isPastEvent = false)

        composeTestRule.setContent {
            ShowEventScreen(
                eventId = testEventId, currentUserId = testOwnerId, showEventViewModel = mockViewModel)
        }

        composeTestRule.onNodeWithTag(ShowEventScreenTestTags.EDIT_BUTTON).assertExists()
        composeTestRule.onNodeWithTag(ShowEventScreenTestTags.DELETE_BUTTON).assertExists()
        composeTestRule.onNodeWithTag(ShowEventScreenTestTags.JOIN_QUIT_BUTTON).assertDoesNotExist()
    }

    @Test
    fun showEventScreen_participantSeesQuitButton() {
        uiStateFlow.value =
            ShowEventUIState(
                type = "SPORTS",
                title = "Basketball Game",
                ownerId = testOwnerId,
                ownerName = "Created by EventOwner",
                participants = listOf(testOwnerId, testUserId),
                isPastEvent = false,
                maxParticipants = "10",
                participantsCount = "2")

        composeTestRule.setContent {
            ShowEventScreen(
                eventId = testEventId, currentUserId = testUserId, showEventViewModel = mockViewModel)
        }

        composeTestRule.onNodeWithTag(ShowEventScreenTestTags.JOIN_QUIT_BUTTON).assertExists()
        composeTestRule.onNodeWithTag(ShowEventScreenTestTags.JOIN_QUIT_BUTTON).assertTextContains("QUIT EVENT")
        composeTestRule.onNodeWithTag(ShowEventScreenTestTags.EDIT_BUTTON).assertDoesNotExist()
        composeTestRule.onNodeWithTag(ShowEventScreenTestTags.DELETE_BUTTON).assertDoesNotExist()
    }

    @Test
    fun showEventScreen_nonParticipantSeesJoinButton() {
        uiStateFlow.value =
            ShowEventUIState(
                type = "SPORTS",
                title = "Basketball Game",
                ownerId = testOwnerId,
                ownerName = "Created by EventOwner",
                participants = listOf(testOwnerId),
                isPastEvent = false,
                maxParticipants = "10",
                participantsCount = "1")

        composeTestRule.setContent {
            ShowEventScreen(
                eventId = testEventId, currentUserId = testUserId, showEventViewModel = mockViewModel)
        }

        composeTestRule.onNodeWithTag(ShowEventScreenTestTags.JOIN_QUIT_BUTTON).assertExists()
        composeTestRule.onNodeWithTag(ShowEventScreenTestTags.JOIN_QUIT_BUTTON).assertTextContains("JOIN EVENT")
    }

    @Test
    fun showEventScreen_pastEvent_hidesActionButtons() {
        uiStateFlow.value =
            ShowEventUIState(
                type = "SPORTS",
                title = "Basketball Game",
                ownerId = testOwnerId,
                ownerName = "Created by EventOwner",
                participants = listOf(testOwnerId),
                isPastEvent = true)

        composeTestRule.setContent {
            ShowEventScreen(
                eventId = testEventId, currentUserId = testUserId, showEventViewModel = mockViewModel)
        }

        composeTestRule.onNodeWithTag(ShowEventScreenTestTags.EDIT_BUTTON).assertDoesNotExist()
        composeTestRule.onNodeWithTag(ShowEventScreenTestTags.DELETE_BUTTON).assertDoesNotExist()
        composeTestRule.onNodeWithTag(ShowEventScreenTestTags.JOIN_QUIT_BUTTON).assertDoesNotExist()
    }

    @Test
    fun showEventScreen_fullEvent_showsFullMessage() {
        uiStateFlow.value =
            ShowEventUIState(
                type = "SPORTS",
                title = "Basketball Game",
                ownerId = testOwnerId,
                ownerName = "Created by EventOwner",
                participants = listOf(testOwnerId, "user1"),
                maxParticipants = "2",
                participantsCount = "2",
                isPastEvent = false)

        composeTestRule.setContent {
            ShowEventScreen(
                eventId = testEventId, currentUserId = testUserId, showEventViewModel = mockViewModel)
        }

        composeTestRule.onNodeWithTag(ShowEventScreenTestTags.FULL_EVENT_MESSAGE).assertExists()
        composeTestRule.onNodeWithTag(ShowEventScreenTestTags.FULL_EVENT_MESSAGE).assertTextEquals("Sorry this event is full")
        composeTestRule.onNodeWithTag(ShowEventScreenTestTags.JOIN_QUIT_BUTTON).assertDoesNotExist()
    }

    @Test
    fun showEventScreen_displaysOwnerName() {
        uiStateFlow.value =
            ShowEventUIState(
                type = "SPORTS",
                title = "Basketball Game",
                ownerId = "owner123",
                ownerName = "Created by Alice Johnson",
                isPastEvent = false)

        composeTestRule.setContent {
            ShowEventScreen(
                eventId = testEventId, currentUserId = testUserId, showEventViewModel = mockViewModel)
        }

        composeTestRule.onNodeWithTag(ShowEventScreenTestTags.EVENT_OWNER).assertTextEquals("Created by Alice Johnson")
    }
}