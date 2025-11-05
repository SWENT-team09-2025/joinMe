package com.android.joinme.ui.groups

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.android.joinme.model.event.EventType
import com.android.joinme.model.groups.Group
import com.android.joinme.model.groups.GroupRepository
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class EditGroupScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var fakeRepository: FakeGroupRepository
  private lateinit var viewModel: EditGroupViewModel
  private lateinit var testGroup: Group

  @Before
  fun setup() {
    fakeRepository = FakeGroupRepository()
    viewModel = EditGroupViewModel(fakeRepository)

    testGroup =
        Group(
            id = "test-group-1",
            name = "Basketball Team",
            category = EventType.SPORTS,
            description = "Weekly basketball games",
            ownerId = "owner-123",
            memberIds = listOf("owner-123", "member-456"),
            eventIds = listOf("event-1", "event-2"))

    fakeRepository.addGroupForTest(testGroup)
  }

  private class FakeGroupRepository : GroupRepository {
    private val groups = mutableListOf<Group>()
    private var idCounter = 0

    fun addGroupForTest(group: Group) {
      groups.add(group)
    }

    override fun getNewGroupId(): String = "test-group-${idCounter++}"

    override suspend fun getAllGroups(): List<Group> = groups.toList()

    override suspend fun getGroup(groupId: String): Group =
        groups.find { it.id == groupId } ?: throw Exception("Group not found")

    override suspend fun addGroup(group: Group) {
      groups.add(group)
    }

    override suspend fun editGroup(groupId: String, newValue: Group) {
      val index = groups.indexOfFirst { it.id == groupId }
      if (index != -1) groups[index] = newValue else throw Exception("Group not found")
    }

    override suspend fun deleteGroup(groupId: String, userId: String) {
      val group = getGroup(groupId)

      if (group.ownerId != userId) {
        throw Exception("Only the group owner can delete this group")
      }

      if (!groups.removeIf { it.id == groupId }) throw Exception("Group not found")
    }

    override suspend fun leaveGroup(groupId: String, userId: String) {
      val group = getGroup(groupId)
      val updatedMemberIds = group.memberIds.filter { it != userId }

      if (updatedMemberIds.size == group.memberIds.size) {
        throw Exception("User is not a member of this group")
      }

      val updatedGroup = group.copy(memberIds = updatedMemberIds)
      editGroup(groupId, updatedGroup)
    }
  }

  @Test
  fun editGroupScreen_displaysAllComponents() {
    composeTestRule.setContent { EditGroupScreen(groupId = testGroup.id, viewModel = viewModel) }

    composeTestRule.waitForIdle()

    // Wait for loading to complete
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(EditGroupScreenTags.GROUP_NAME_TEXT_FIELD)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule.onNodeWithTag(EditGroupScreenTags.SCREEN).assertIsDisplayed()
    composeTestRule.onNodeWithTag(EditGroupScreenTags.TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(EditGroupScreenTags.GROUP_PICTURE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(EditGroupScreenTags.EDIT_PHOTO_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(EditGroupScreenTags.GROUP_NAME_TEXT_FIELD).assertIsDisplayed()
    composeTestRule.onNodeWithTag(EditGroupScreenTags.NAME_SUPPORTING_TEXT).assertIsDisplayed()
    composeTestRule.onNodeWithTag(EditGroupScreenTags.CATEGORY_DROPDOWN).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(EditGroupScreenTags.GROUP_DESCRIPTION_TEXT_FIELD)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(EditGroupScreenTags.DESCRIPTION_SUPPORTING_TEXT)
        .assertIsDisplayed()
    composeTestRule.onNodeWithTag(EditGroupScreenTags.SAVE_BUTTON).assertIsDisplayed()
  }

  @Test
  fun editGroupScreen_loadsAndDisplaysOriginalGroupData() {
    composeTestRule.setContent { EditGroupScreen(groupId = testGroup.id, viewModel = viewModel) }

    composeTestRule.waitForIdle()

    // Wait for loading to complete
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(EditGroupScreenTags.GROUP_NAME_TEXT_FIELD)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule
        .onNodeWithTag(EditGroupScreenTags.GROUP_NAME_TEXT_FIELD)
        .assertTextContains("Basketball Team")

    composeTestRule
        .onNodeWithTag(EditGroupScreenTags.CATEGORY_DROPDOWN)
        .assertTextContains("Sports")

    composeTestRule
        .onNodeWithTag(EditGroupScreenTags.GROUP_DESCRIPTION_TEXT_FIELD)
        .assertTextContains("Weekly basketball games")
  }

  @Test
  fun editGroupScreen_initialState_saveButtonIsEnabled() {
    composeTestRule.setContent { EditGroupScreen(groupId = testGroup.id, viewModel = viewModel) }

    composeTestRule.waitForIdle()

    // Wait for loading to complete
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(EditGroupScreenTags.SAVE_BUTTON)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule.onNodeWithTag(EditGroupScreenTags.SAVE_BUTTON).assertIsEnabled()
  }

  @Test
  fun nameInput_canModifyExistingName() {
    composeTestRule.setContent { EditGroupScreen(groupId = testGroup.id, viewModel = viewModel) }

    composeTestRule.waitForIdle()

    // Wait for loading to complete
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(EditGroupScreenTags.GROUP_NAME_TEXT_FIELD)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule.onNodeWithTag(EditGroupScreenTags.GROUP_NAME_TEXT_FIELD).performTextClearance()
    composeTestRule
        .onNodeWithTag(EditGroupScreenTags.GROUP_NAME_TEXT_FIELD)
        .performTextInput("Soccer Team")

    composeTestRule
        .onNodeWithTag(EditGroupScreenTags.GROUP_NAME_TEXT_FIELD)
        .assertTextContains("Soccer Team")
  }

  @Test
  fun categoryDropdown_opensMenuOnClick() {
    composeTestRule.setContent { EditGroupScreen(groupId = testGroup.id, viewModel = viewModel) }

    composeTestRule.waitForIdle()

    // Wait for loading to complete
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(EditGroupScreenTags.CATEGORY_DROPDOWN)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule.onNodeWithContentDescription("Dropdown").performClick()
    composeTestRule.onNodeWithTag(EditGroupScreenTags.CATEGORY_MENU).assertIsDisplayed()
  }

  @Test
  fun categoryDropdown_showsAllThreeOptions() {
    composeTestRule.setContent { EditGroupScreen(groupId = testGroup.id, viewModel = viewModel) }

    composeTestRule.waitForIdle()

    // Wait for loading to complete
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(EditGroupScreenTags.CATEGORY_DROPDOWN)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule.onNodeWithContentDescription("Dropdown").performClick()
    composeTestRule
        .onNodeWithTag(EditGroupScreenTags.CATEGORY_OPTION_PREFIX + "SOCIAL")
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(EditGroupScreenTags.CATEGORY_OPTION_PREFIX + "ACTIVITY")
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(EditGroupScreenTags.CATEGORY_OPTION_PREFIX + "SPORTS")
        .assertIsDisplayed()
  }

  @Test
  fun categoryDropdown_canChangeCategoryFromSportsToSocial() {
    composeTestRule.setContent { EditGroupScreen(groupId = testGroup.id, viewModel = viewModel) }

    composeTestRule.waitForIdle()

    // Wait for loading to complete
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(EditGroupScreenTags.CATEGORY_DROPDOWN)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule
        .onNodeWithTag(EditGroupScreenTags.CATEGORY_DROPDOWN)
        .assertTextContains("Sports")

    composeTestRule.onNodeWithContentDescription("Dropdown").performClick()
    composeTestRule
        .onNodeWithTag(EditGroupScreenTags.CATEGORY_OPTION_PREFIX + "SOCIAL")
        .performClick()

    composeTestRule
        .onNodeWithTag(EditGroupScreenTags.CATEGORY_DROPDOWN)
        .assertTextContains("Social")
  }

  @Test
  fun categoryDropdown_canChangeCategoryFromSportsToActivity() {
    composeTestRule.setContent { EditGroupScreen(groupId = testGroup.id, viewModel = viewModel) }

    composeTestRule.waitForIdle()

    // Wait for loading to complete
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(EditGroupScreenTags.CATEGORY_DROPDOWN)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule.onNodeWithContentDescription("Dropdown").performClick()
    composeTestRule
        .onNodeWithTag(EditGroupScreenTags.CATEGORY_OPTION_PREFIX + "ACTIVITY")
        .performClick()

    composeTestRule
        .onNodeWithTag(EditGroupScreenTags.CATEGORY_DROPDOWN)
        .assertTextContains("Activity")
  }

  @Test
  fun categoryDropdown_closesAfterSelection() {
    composeTestRule.setContent { EditGroupScreen(groupId = testGroup.id, viewModel = viewModel) }

    composeTestRule.waitForIdle()

    // Wait for loading to complete
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(EditGroupScreenTags.CATEGORY_DROPDOWN)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule.onNodeWithContentDescription("Dropdown").performClick()
    composeTestRule
        .onNodeWithTag(EditGroupScreenTags.CATEGORY_OPTION_PREFIX + "SOCIAL")
        .performClick()

    composeTestRule.onNodeWithTag(EditGroupScreenTags.CATEGORY_MENU).assertDoesNotExist()
  }

  @Test
  fun descriptionInput_accepts300Characters() {
    composeTestRule.setContent { EditGroupScreen(groupId = testGroup.id, viewModel = viewModel) }

    composeTestRule.waitForIdle()

    // Wait for loading to complete
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(EditGroupScreenTags.GROUP_DESCRIPTION_TEXT_FIELD)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule
        .onNodeWithTag(EditGroupScreenTags.GROUP_DESCRIPTION_TEXT_FIELD)
        .performTextClearance()
    composeTestRule
        .onNodeWithTag(EditGroupScreenTags.GROUP_DESCRIPTION_TEXT_FIELD)
        .performTextInput("a".repeat(300))

    composeTestRule
        .onNodeWithTag(EditGroupScreenTags.DESCRIPTION_SUPPORTING_TEXT)
        .assertTextEquals("0-300 characters. Letters, numbers, spaces, or underscores only")
  }

  @Test
  fun saveButton_enabledWhenFormIsValid() {
    composeTestRule.setContent { EditGroupScreen(groupId = testGroup.id, viewModel = viewModel) }

    composeTestRule.waitForIdle()

    // Wait for loading to complete
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(EditGroupScreenTags.GROUP_NAME_TEXT_FIELD)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule.onNodeWithTag(EditGroupScreenTags.GROUP_NAME_TEXT_FIELD).performTextClearance()
    composeTestRule
        .onNodeWithTag(EditGroupScreenTags.GROUP_NAME_TEXT_FIELD)
        .performTextInput("Valid Group")

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(EditGroupScreenTags.SAVE_BUTTON).assertIsEnabled()
  }

  @Test
  fun saveButton_disabledWhenNameIsInvalid() {
    composeTestRule.setContent { EditGroupScreen(groupId = testGroup.id, viewModel = viewModel) }

    composeTestRule.waitForIdle()

    // Wait for loading to complete
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(EditGroupScreenTags.GROUP_NAME_TEXT_FIELD)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule.onNodeWithTag(EditGroupScreenTags.GROUP_NAME_TEXT_FIELD).performTextClearance()
    composeTestRule.onNodeWithTag(EditGroupScreenTags.GROUP_NAME_TEXT_FIELD).performTextInput("ab")

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(EditGroupScreenTags.SAVE_BUTTON).assertIsNotEnabled()
  }

  @Test
  fun saveButton_disabledWhenDescriptionIsTooLong() {
    composeTestRule.setContent { EditGroupScreen(groupId = testGroup.id, viewModel = viewModel) }

    composeTestRule.waitForIdle()

    // Wait for loading to complete
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(EditGroupScreenTags.GROUP_DESCRIPTION_TEXT_FIELD)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule
        .onNodeWithTag(EditGroupScreenTags.GROUP_DESCRIPTION_TEXT_FIELD)
        .performTextClearance()
    composeTestRule
        .onNodeWithTag(EditGroupScreenTags.GROUP_DESCRIPTION_TEXT_FIELD)
        .performTextInput("a".repeat(301))

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(EditGroupScreenTags.SAVE_BUTTON).assertIsNotEnabled()
  }

  @Test
  fun completeForm_modifyAllFields_enablesSaveButton() {
    composeTestRule.setContent { EditGroupScreen(groupId = testGroup.id, viewModel = viewModel) }

    composeTestRule.waitForIdle()

    // Wait for loading to complete
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(EditGroupScreenTags.GROUP_NAME_TEXT_FIELD)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule.onNodeWithTag(EditGroupScreenTags.GROUP_NAME_TEXT_FIELD).performTextClearance()
    composeTestRule
        .onNodeWithTag(EditGroupScreenTags.GROUP_NAME_TEXT_FIELD)
        .performTextInput("Updated Team")
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithContentDescription("Dropdown").performClick()
    composeTestRule.waitForIdle()
    composeTestRule
        .onNodeWithTag(EditGroupScreenTags.CATEGORY_OPTION_PREFIX + "SOCIAL")
        .performClick()
    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(EditGroupScreenTags.GROUP_DESCRIPTION_TEXT_FIELD)
        .performTextClearance()
    composeTestRule
        .onNodeWithTag(EditGroupScreenTags.GROUP_DESCRIPTION_TEXT_FIELD)
        .performTextInput("A social group for friends")
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(EditGroupScreenTags.SAVE_BUTTON).assertIsEnabled()
  }

  @Test
  fun editGroupScreen_titleDisplaysCorrectText() {
    composeTestRule.setContent { EditGroupScreen(groupId = testGroup.id, viewModel = viewModel) }

    composeTestRule.waitForIdle()

    // Wait for loading to complete
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(EditGroupScreenTags.TITLE)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule.onNodeWithTag(EditGroupScreenTags.TITLE).assertTextEquals("Edit Group")
  }

  @Test
  fun editGroupScreen_canClearDescriptionCompletely() {
    composeTestRule.setContent { EditGroupScreen(groupId = testGroup.id, viewModel = viewModel) }

    composeTestRule.waitForIdle()

    // Wait for loading to complete
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(EditGroupScreenTags.GROUP_DESCRIPTION_TEXT_FIELD)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule
        .onNodeWithTag(EditGroupScreenTags.GROUP_DESCRIPTION_TEXT_FIELD)
        .performTextClearance()

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(EditGroupScreenTags.SAVE_BUTTON).assertIsEnabled()
  }
}
