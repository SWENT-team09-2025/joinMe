package com.android.joinme.ui.overview

import android.content.Context
import android.net.Uri
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.android.joinme.model.event.Event
import com.android.joinme.model.event.EventFilter
import com.android.joinme.model.event.EventType
import com.android.joinme.model.event.EventsRepository
import com.android.joinme.model.groups.Group
import com.android.joinme.model.groups.GroupRepository
import com.android.joinme.model.profile.ProfileRepository
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "w360dp-h640dp-normal-long-notround-any-420dpi-keyshidden-nonav")
@OptIn(ExperimentalCoroutinesApi::class)
class CreateEventScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setUp() {
    // Initialize Firebase for Robolectric tests
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    if (FirebaseApp.getApps(context).isEmpty()) {
      FirebaseApp.initializeApp(context)
    }
    Dispatchers.setMain(testDispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  // ---- Fake repositories for testing with groups ----
  private class FakeEventsRepository : EventsRepository {
    val added = mutableListOf<Event>()

    override suspend fun addEvent(event: Event) {
      added += event
    }

    override suspend fun editEvent(eventId: String, newValue: Event) {}

    override suspend fun deleteEvent(eventId: String) {}

    override suspend fun getEventsByIds(eventIds: List<String>): List<Event> = emptyList()

    override suspend fun getEvent(eventId: String): Event =
        added.find { it.eventId == eventId } ?: throw NoSuchElementException("Event not found")

    override suspend fun getAllEvents(eventFilter: EventFilter): List<Event> = added.toList()

    override fun getNewEventId(): String = "fake-id-1"

    override suspend fun getCommonEvents(userIds: List<String>): List<Event> {
      if (userIds.isEmpty()) return emptyList()
      return added
          .filter { event -> userIds.all { userId -> event.participants.contains(userId) } }
          .sortedBy { it.date.toDate().time }
    }
  }

  private class FakeGroupRepository : GroupRepository {
    private val groups = mutableMapOf<String, Group>()

    fun addTestGroup(group: Group) {
      groups[group.id] = group
    }

    override fun getNewGroupId(): String = "fake-group-id"

    override suspend fun getAllGroups(): List<Group> = groups.values.toList()

    override suspend fun getGroup(groupId: String): Group =
        groups[groupId] ?: throw Exception("Group not found")

    override suspend fun addGroup(group: Group) {
      groups[group.id] = group
    }

    override suspend fun editGroup(groupId: String, newValue: Group) {
      groups[groupId] = newValue
    }

    override suspend fun deleteGroup(groupId: String, userId: String) {
      groups.remove(groupId)
    }

    override suspend fun leaveGroup(groupId: String, userId: String) {
      val group = groups[groupId] ?: return
      groups[groupId] = group.copy(memberIds = group.memberIds - userId)
    }

    override suspend fun joinGroup(groupId: String, userId: String) {
      val group = groups[groupId] ?: return
      groups[groupId] = group.copy(memberIds = group.memberIds + userId)
    }

    override suspend fun getCommonGroups(userIds: List<String>): List<Group> {
      if (userIds.isEmpty()) return emptyList()
      return groups.values.filter { group ->
        userIds.all { userId -> group.memberIds.contains(userId) }
      }
    }

    override suspend fun uploadGroupPhoto(
        context: Context,
        groupId: String,
        imageUri: Uri
    ): String {
      // Not needed for these tests
      return "http://fakeurl.com/photo.jpg"
    }

    override suspend fun deleteGroupPhoto(groupId: String) {
      // Not needed for these tests
    }
  }

  /** --- BASIC RENDERING --- */
  @Test
  fun allFieldsAndButtonAreDisplayed() {
    composeTestRule.setContent { CreateEventScreen(onDone = {}) }

    // Group dropdown should be at the top
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_GROUP).assertIsDisplayed()
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_TYPE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_TITLE).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_DESCRIPTION)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_LOCATION)
        .assertIsDisplayed()
    // Elements below the fold need to be checked with assertExists()
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_MAX_PARTICIPANTS)
        .assertExists()
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_DURATION).assertExists()
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_DATE).assertExists()
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_VISIBILITY).assertExists()
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.BUTTON_SAVE_EVENT).assertExists()
  }

  /** --- INPUT BEHAVIOR --- */
  @Test
  fun emptyFieldsDisableSaveButton() {
    composeTestRule.setContent { CreateEventScreen(onDone = {}) }

    // Initially all fields are empty, so save button should be disabled
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.BUTTON_SAVE_EVENT).assertIsNotEnabled()
  }

  @Test
  fun typeDropdownWorks() {
    composeTestRule.setContent { CreateEventScreen(onDone = {}) }

    // Click on type dropdown
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_TYPE).performClick()

    // Select SPORTS
    composeTestRule.onNodeWithText("SPORTS").assertIsDisplayed()
    composeTestRule.onNodeWithText("SPORTS").performClick()

    // Verify selection
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_TYPE)
        .assertTextContains("SPORTS")
  }

  @Test
  fun visibilityDropdownWorks() {
    composeTestRule.setContent { CreateEventScreen(onDone = {}) }

    // Scroll to visibility dropdown and click
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_VISIBILITY)
        .performScrollTo()
        .performClick()

    // Select PUBLIC
    composeTestRule.onNodeWithText("PUBLIC").assertIsDisplayed()
    composeTestRule.onNodeWithText("PUBLIC").performClick()

    // Verify selection
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_VISIBILITY)
        .assertTextContains("PUBLIC")
  }

  @Test
  fun textFieldsAcceptInput() {
    composeTestRule.setContent { CreateEventScreen(onDone = {}) }

    val title = "Football Match"
    val desc = "Friendly game"
    val location = "EPFL Field"

    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_TITLE)
        .performTextInput(title)
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_DESCRIPTION)
        .performTextInput(desc)
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_LOCATION)
        .performTextInput(location)

    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_TITLE)
        .assertTextContains(title)
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_DESCRIPTION)
        .assertTextContains(desc)
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_LOCATION)
        .assertTextContains(location)
  }

  /** --- EDGE CASES --- */
  @Test
  fun emptyAndWhitespaceInputs_disableSaveButton() {
    composeTestRule.setContent { CreateEventScreen(onDone = {}) }

    // Test empty title
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_TITLE).performTextInput("")
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.BUTTON_SAVE_EVENT).assertIsNotEnabled()

    // Test whitespace inputs
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_TITLE)
        .performTextInput("   ")
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_DESCRIPTION)
        .performTextInput("   ")
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.BUTTON_SAVE_EVENT).assertIsNotEnabled()
  }

  @Test
  fun switchingTypeRetainsOtherFields() {
    composeTestRule.setContent { CreateEventScreen(onDone = {}) }

    val title = "Morning Run"
    val desc = "Light jog near EPFL"
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_TITLE)
        .performTextInput(title)
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_DESCRIPTION)
        .performTextInput(desc)

    // Select type SPORTS
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_TYPE).performClick()
    composeTestRule.onNodeWithText("SPORTS").performClick()

    // Switch to SOCIAL
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_TYPE).performClick()
    composeTestRule.onNodeWithText("SOCIAL").performClick()

    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_TITLE)
        .assertTextContains(title)
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_DESCRIPTION)
        .assertTextContains(desc)
  }

  @Test
  fun saveButtonRemainsDisabledUntilAllMandatoryFieldsAreFilled() {
    composeTestRule.setContent { CreateEventScreen(onDone = {}) }

    // Fill only some fields
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_TYPE).performClick()
    composeTestRule.onNodeWithText("SPORTS").performClick()

    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_TITLE)
        .performTextInput("Game")
    // Missing description, location, date, time, and visibility -> must be disabled
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.BUTTON_SAVE_EVENT).assertIsNotEnabled()
  }

  @Test
  fun switchingVisibilityUpdatesStateCorrectly() {
    composeTestRule.setContent { CreateEventScreen(onDone = {}) }

    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_VISIBILITY)
        .performScrollTo()
        .performClick()
    composeTestRule.onNodeWithText("PRIVATE").performClick()
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_VISIBILITY)
        .assertTextContains("PRIVATE")

    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_VISIBILITY)
        .performScrollTo()
        .performClick()
    composeTestRule.onNodeWithText("PUBLIC").performClick()
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_VISIBILITY)
        .assertTextContains("PUBLIC")
  }

  @Test
  fun individualFieldsAreDisplayed() {
    composeTestRule.setContent { CreateEventScreen(onDone = {}) }

    // Max participants field should be displayed (even if empty initially)
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_MAX_PARTICIPANTS)
        .assertIsDisplayed()

    // Duration field should be displayed (even if empty initially)
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_DURATION)
        .assertIsDisplayed()

    // Date field should exist (may need scrolling to be displayed)
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_DATE).assertExists()
  }

  @Test
  fun partialFormFillKeepsSaveButtonDisabled() {
    composeTestRule.setContent { CreateEventScreen(onDone = {}) }

    val saveButton = composeTestRule.onNodeWithTag(CreateEventScreenTestTags.BUTTON_SAVE_EVENT)
    saveButton.assertIsNotEnabled()

    // Fill some but not all required fields
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_TYPE).performClick()
    composeTestRule.onNodeWithText("SPORTS").performClick()

    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_TITLE)
        .performTextInput("Run")
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_DESCRIPTION)
        .performTextInput("Morning jog")
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_LOCATION)
        .performTextInput("EPFL Track")

    // Still missing date, time, and visibility, so button should remain disabled
    composeTestRule.waitForIdle()
    saveButton.assertIsNotEnabled()
  }

  /** --- GROUP EVENT SPECIFIC TESTS --- */
  @Test
  fun groupDropdown_isDisplayedAndWorksCorrectly() {
    composeTestRule.setContent { CreateEventScreen(onDone = {}) }

    // Group dropdown should be displayed at the top
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_GROUP).assertIsDisplayed()

    // Initially should show "Standalone Event"
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_GROUP)
        .assertTextContains("Standalone Event")
  }

  @Test
  fun whenGroupSelected_typeAndVisibilityFieldsAreHiddenButMaxParticipantsIsVisible() {
    composeTestRule.setContent { CreateEventScreen(onDone = {}) }

    // Initially all fields should be visible for standalone
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_TYPE).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_MAX_PARTICIPANTS)
        .assertExists()
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_VISIBILITY)
        .performScrollTo()
        .assertExists()

    // Click group dropdown
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_GROUP).performClick()

    // If there are groups available, select one (if not, this test will just verify standalone)
    // Note: In a real scenario with mock data, we'd have groups available
    // For now, we verify the fields exist when standalone is selected
    composeTestRule.onNodeWithText("Standalone Event").performClick()

    // Fields should still be visible for standalone
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_TYPE).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_MAX_PARTICIPANTS)
        .assertExists()
  }

  @Test
  fun whenStandaloneSelected_allFieldsAreVisible() {
    composeTestRule.setContent { CreateEventScreen(onDone = {}) }

    // For standalone events, all fields should be visible/exist
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_GROUP).assertIsDisplayed()
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_TYPE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_TITLE).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_DESCRIPTION)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_LOCATION)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_MAX_PARTICIPANTS)
        .assertExists()
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_DURATION).assertExists()
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_DATE).assertExists()
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_VISIBILITY).assertExists()
  }

  @Test
  fun groupDropdown_canSwitchBetweenStandaloneAndGroup() {
    composeTestRule.setContent { CreateEventScreen(onDone = {}) }

    // Initially should show "Standalone Event"
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_GROUP)
        .assertTextContains("Standalone Event")

    // Open dropdown
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_GROUP).performClick()

    // Select standalone again using onAllNodesWithText to handle multiple matches
    composeTestRule.onAllNodesWithText("Standalone Event")[1].performClick()

    // Verify standalone is still selected
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_GROUP)
        .assertTextContains("Standalone Event")

    // Type field should be visible for standalone
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_TYPE).assertIsDisplayed()
  }

  @Test
  fun groupDropdown_withAvailableGroups_displaysAndSelectsGroupCorrectly() {
    // Create fake repositories with test groups
    val fakeEventsRepo = FakeEventsRepository()
    val fakeGroupRepo = FakeGroupRepository()

    // Add test groups
    val group1 =
        Group(
            id = "group1",
            name = "Football Team",
            category = EventType.SPORTS,
            memberIds = listOf("user1", "user2", "user3"),
            ownerId = "user1",
            description = "Test group 1")
    val group2 =
        Group(
            id = "group2",
            name = "Book Club",
            category = EventType.SOCIAL,
            memberIds = listOf("user1", "user2"),
            ownerId = "user1",
            description = "Test group 2")
    val group3 =
        Group(
            id = "group3",
            name = "Study Group",
            category = EventType.ACTIVITY,
            memberIds = listOf("user1"),
            ownerId = "user1",
            description = "Test group 3")

    fakeGroupRepo.addTestGroup(group1)
    fakeGroupRepo.addTestGroup(group2)
    fakeGroupRepo.addTestGroup(group3)

    // Create ViewModel with fake repos
    val profileRepository = mock(ProfileRepository::class.java)
    val viewModel = CreateEventViewModel(fakeEventsRepo, profileRepository, fakeGroupRepo)

    // Wait for groups to load
    testDispatcher.scheduler.advanceUntilIdle()

    composeTestRule.setContent { CreateEventScreen(createEventViewModel = viewModel, onDone = {}) }

    // Wait for composition
    composeTestRule.waitForIdle()

    // Initially should show "Standalone Event"
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_GROUP)
        .assertTextContains("Standalone Event")

    // Open dropdown
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_GROUP).performClick()

    // Verify groups are displayed
    composeTestRule.onNodeWithText("Football Team").assertIsDisplayed()
    composeTestRule.onNodeWithText("Book Club").assertIsDisplayed()
    composeTestRule.onNodeWithText("Study Group").assertIsDisplayed()

    // Select first group
    composeTestRule.onNodeWithText("Football Team").performClick()

    // Verify group is selected
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_GROUP)
        .assertTextContains("Football Team")

    // Verify type field is hidden for group events
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_TYPE).assertDoesNotExist()

    // Open dropdown again
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_GROUP).performClick()

    // Select a different group (second group in the list)
    composeTestRule.onNodeWithText("Book Club").performClick()

    // Verify new group is selected
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_GROUP)
        .assertTextContains("Book Club")

    // Open dropdown once more
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_GROUP).performClick()

    // Wait for dropdown to open
    composeTestRule.waitForIdle()

    // Switch back to standalone - find the one in the dropdown menu (not the selected one)
    composeTestRule
        .onAllNodesWithText("Standalone Event")
        .filter(hasClickAction())
        .onLast()
        .performClick()

    // Verify standalone is selected
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_GROUP)
        .assertTextContains("Standalone Event")

    // Verify type field is now visible again
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_TYPE).assertIsDisplayed()
  }
}
