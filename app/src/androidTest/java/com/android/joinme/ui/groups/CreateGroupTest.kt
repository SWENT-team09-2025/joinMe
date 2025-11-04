package com.android.joinme.ui.groups

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.android.joinme.model.groups.Group
import com.android.joinme.model.groups.GroupRepository
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class CreateGroupScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var fakeRepository: FakeGroupRepository
  private lateinit var viewModel: CreateGroupViewModel

  @Before
  fun setup() {
    fakeRepository = FakeGroupRepository()
    viewModel = CreateGroupViewModel(fakeRepository)
  }

  private class FakeGroupRepository : GroupRepository {
    private val groups = mutableListOf<Group>()
    private var idCounter = 0

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

    override suspend fun deleteGroup(groupId: String) {
      if (!groups.removeIf { it.id == groupId }) throw Exception("Group not found")
    }
  }

  @Test
  fun createGroupScreen_displaysAllComponents() {
    composeTestRule.setContent { CreateGroupScreen(viewModel = viewModel) }
    composeTestRule.onNodeWithTag(CreateGroupScreenTestTags.SCREEN).assertIsDisplayed()
    composeTestRule.onNodeWithTag(CreateGroupScreenTestTags.TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(CreateGroupScreenTestTags.GROUP_PICTURE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(CreateGroupScreenTestTags.EDIT_PHOTO_BUTTON).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(CreateGroupScreenTestTags.GROUP_NAME_TEXT_FIELD)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(CreateGroupScreenTestTags.NAME_SUPPORTING_TEXT)
        .assertIsDisplayed()
    composeTestRule.onNodeWithTag(CreateGroupScreenTestTags.CATEGORY_DROPDOWN).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(CreateGroupScreenTestTags.GROUP_DESCRIPTION_TEXT_FIELD)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(CreateGroupScreenTestTags.DESCRIPTION_SUPPORTING_TEXT)
        .assertIsDisplayed()
    composeTestRule.onNodeWithTag(CreateGroupScreenTestTags.SAVE_BUTTON).assertIsDisplayed()
  }

  @Test
  fun createGroupScreen_initialState_saveButtonIsDisabled() {
    composeTestRule.setContent { CreateGroupScreen(viewModel = viewModel) }
    composeTestRule.onNodeWithTag(CreateGroupScreenTestTags.SAVE_BUTTON).assertIsNotEnabled()
  }

  @Test
  fun createGroupScreen_initialState_fieldsAreEmpty() {
    composeTestRule.setContent { CreateGroupScreen(viewModel = viewModel) }
    // Check that name field has only placeholder text (empty value)
    composeTestRule
        .onNodeWithTag(CreateGroupScreenTestTags.GROUP_NAME_TEXT_FIELD)
        .assert(hasText("", substring = true))
    // Check that description field has only placeholder text (empty value)
    composeTestRule
        .onNodeWithTag(CreateGroupScreenTestTags.GROUP_DESCRIPTION_TEXT_FIELD)
        .assert(hasText("", substring = true))
  }

  @Test
  fun createGroupScreen_initialState_categoryShowsActivity() {
    composeTestRule.setContent { CreateGroupScreen(viewModel = viewModel) }
    composeTestRule
        .onNodeWithTag(CreateGroupScreenTestTags.CATEGORY_DROPDOWN)
        .assertTextContains("Activity")
  }

  @Test
  fun nameInput_acceptsValidInput() {
    composeTestRule.setContent { CreateGroupScreen(viewModel = viewModel) }
    composeTestRule
        .onNodeWithTag(CreateGroupScreenTestTags.GROUP_NAME_TEXT_FIELD)
        .performTextInput("Test Group")
    composeTestRule
        .onNodeWithTag(CreateGroupScreenTestTags.GROUP_NAME_TEXT_FIELD)
        .assertTextContains("Test Group")
  }

  @Test
  fun nameInput_showsErrorForShortName() {
    composeTestRule.setContent { CreateGroupScreen(viewModel = viewModel) }
    composeTestRule
        .onNodeWithTag(CreateGroupScreenTestTags.GROUP_NAME_TEXT_FIELD)
        .performTextInput("ab")
    composeTestRule
        .onNodeWithTag(CreateGroupScreenTestTags.NAME_SUPPORTING_TEXT)
        .assertTextEquals("Name must be at least 3 characters")
  }

  @Test
  fun nameInput_showsErrorForLongName() {
    composeTestRule.setContent { CreateGroupScreen(viewModel = viewModel) }
    composeTestRule
        .onNodeWithTag(CreateGroupScreenTestTags.GROUP_NAME_TEXT_FIELD)
        .performTextInput("a".repeat(31))
    composeTestRule
        .onNodeWithTag(CreateGroupScreenTestTags.NAME_SUPPORTING_TEXT)
        .assertTextEquals("Name must not exceed 30 characters")
  }

  @Test
  fun nameInput_showsErrorForInvalidCharacters() {
    composeTestRule.setContent { CreateGroupScreen(viewModel = viewModel) }
    composeTestRule
        .onNodeWithTag(CreateGroupScreenTestTags.GROUP_NAME_TEXT_FIELD)
        .performTextInput("Test@Group!")
    composeTestRule
        .onNodeWithTag(CreateGroupScreenTestTags.NAME_SUPPORTING_TEXT)
        .assertTextEquals("Only letters, numbers, spaces, and underscores allowed")
  }

  @Test
  fun nameInput_showsErrorForMultipleConsecutiveSpaces() {
    composeTestRule.setContent { CreateGroupScreen(viewModel = viewModel) }
    composeTestRule
        .onNodeWithTag(CreateGroupScreenTestTags.GROUP_NAME_TEXT_FIELD)
        .performTextInput("Test  Group")
    composeTestRule
        .onNodeWithTag(CreateGroupScreenTestTags.NAME_SUPPORTING_TEXT)
        .assertTextEquals("Multiple consecutive spaces not allowed")
  }

  @Test
  fun categoryDropdown_opensMenuOnClick() {
    composeTestRule.setContent { CreateGroupScreen(viewModel = viewModel) }
    // Click on the dropdown arrow icon, not the text field
    composeTestRule.onNodeWithContentDescription("Dropdown").performClick()
    composeTestRule.onNodeWithTag(CreateGroupScreenTestTags.CATEGORY_MENU).assertIsDisplayed()
  }

  @Test
  fun categoryDropdown_showsAllThreeOptions() {
    composeTestRule.setContent { CreateGroupScreen(viewModel = viewModel) }
    // Click the dropdown arrow to open menu
    composeTestRule.onNodeWithContentDescription("Dropdown").performClick()
    composeTestRule
        .onNodeWithTag(CreateGroupScreenTestTags.CATEGORY_OPTION_PREFIX + "SOCIAL")
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(CreateGroupScreenTestTags.CATEGORY_OPTION_PREFIX + "ACTIVITY")
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(CreateGroupScreenTestTags.CATEGORY_OPTION_PREFIX + "SPORTS")
        .assertIsDisplayed()
  }

  @Test
  fun categoryDropdown_selectsSocialCategory() {
    composeTestRule.setContent { CreateGroupScreen(viewModel = viewModel) }
    // Click the dropdown arrow to open menu
    composeTestRule.onNodeWithContentDescription("Dropdown").performClick()
    composeTestRule
        .onNodeWithTag(CreateGroupScreenTestTags.CATEGORY_OPTION_PREFIX + "SOCIAL")
        .performClick()
    composeTestRule
        .onNodeWithTag(CreateGroupScreenTestTags.CATEGORY_DROPDOWN)
        .assertTextContains("Social")
  }

  @Test
  fun categoryDropdown_selectsSportsCategory() {
    composeTestRule.setContent { CreateGroupScreen(viewModel = viewModel) }
    // Click the dropdown arrow to open menu
    composeTestRule.onNodeWithContentDescription("Dropdown").performClick()
    composeTestRule
        .onNodeWithTag(CreateGroupScreenTestTags.CATEGORY_OPTION_PREFIX + "SPORTS")
        .performClick()
    composeTestRule
        .onNodeWithTag(CreateGroupScreenTestTags.CATEGORY_DROPDOWN)
        .assertTextContains("Sports")
  }

  @Test
  fun categoryDropdown_closesAfterSelection() {
    composeTestRule.setContent { CreateGroupScreen(viewModel = viewModel) }
    // Click the dropdown arrow to open menu
    composeTestRule.onNodeWithContentDescription("Dropdown").performClick()
    composeTestRule
        .onNodeWithTag(CreateGroupScreenTestTags.CATEGORY_OPTION_PREFIX + "SOCIAL")
        .performClick()
    composeTestRule.onNodeWithTag(CreateGroupScreenTestTags.CATEGORY_MENU).assertDoesNotExist()
  }

  @Test
  fun descriptionInput_acceptsValidInput() {
    composeTestRule.setContent { CreateGroupScreen(viewModel = viewModel) }
    composeTestRule
        .onNodeWithTag(CreateGroupScreenTestTags.GROUP_DESCRIPTION_TEXT_FIELD)
        .performTextInput("A great group")
    composeTestRule
        .onNodeWithTag(CreateGroupScreenTestTags.GROUP_DESCRIPTION_TEXT_FIELD)
        .assertTextContains("A great group")
  }

  @Test
  fun descriptionInput_showsErrorForTooLongText() {
    composeTestRule.setContent { CreateGroupScreen(viewModel = viewModel) }
    composeTestRule
        .onNodeWithTag(CreateGroupScreenTestTags.GROUP_DESCRIPTION_TEXT_FIELD)
        .performTextInput("a".repeat(301))
    composeTestRule
        .onNodeWithTag(CreateGroupScreenTestTags.DESCRIPTION_SUPPORTING_TEXT)
        .assertTextEquals("Description must not exceed 300 characters")
  }

  @Test
  fun descriptionInput_accepts300Characters() {
    composeTestRule.setContent { CreateGroupScreen(viewModel = viewModel) }
    composeTestRule
        .onNodeWithTag(CreateGroupScreenTestTags.GROUP_DESCRIPTION_TEXT_FIELD)
        .performTextInput("a".repeat(300))
    composeTestRule
        .onNodeWithTag(CreateGroupScreenTestTags.DESCRIPTION_SUPPORTING_TEXT)
        .assertTextEquals("0-300 characters. Letters, numbers, spaces, or underscores only")
  }

  @Test
  fun saveButton_enabledWhenFormIsValid() {
    composeTestRule.setContent { CreateGroupScreen(viewModel = viewModel) }
    composeTestRule
        .onNodeWithTag(CreateGroupScreenTestTags.GROUP_NAME_TEXT_FIELD)
        .performTextInput("Valid Group")
    // Wait for validation to process
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(CreateGroupScreenTestTags.SAVE_BUTTON).assertIsEnabled()
  }

  @Test
  fun saveButton_disabledWhenNameIsInvalid() {
    composeTestRule.setContent { CreateGroupScreen(viewModel = viewModel) }
    composeTestRule
        .onNodeWithTag(CreateGroupScreenTestTags.GROUP_NAME_TEXT_FIELD)
        .performTextInput("ab")
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(CreateGroupScreenTestTags.SAVE_BUTTON).assertIsNotEnabled()
  }

  @Test
  fun saveButton_disabledWhenDescriptionIsTooLong() {
    composeTestRule.setContent { CreateGroupScreen(viewModel = viewModel) }
    composeTestRule
        .onNodeWithTag(CreateGroupScreenTestTags.GROUP_NAME_TEXT_FIELD)
        .performTextInput("Valid Group")
    composeTestRule
        .onNodeWithTag(CreateGroupScreenTestTags.GROUP_DESCRIPTION_TEXT_FIELD)
        .performTextInput("a".repeat(301))
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(CreateGroupScreenTestTags.SAVE_BUTTON).assertIsNotEnabled()
  }

  @Test
  fun completeForm_withValidData_enablesSaveButton() {
    composeTestRule.setContent { CreateGroupScreen(viewModel = viewModel) }

    // Fill in name
    composeTestRule
        .onNodeWithTag(CreateGroupScreenTestTags.GROUP_NAME_TEXT_FIELD)
        .performTextInput("Test Group")
    composeTestRule.waitForIdle()

    // Select category
    composeTestRule.onNodeWithContentDescription("Dropdown").performClick()
    composeTestRule.waitForIdle()
    composeTestRule
        .onNodeWithTag(CreateGroupScreenTestTags.CATEGORY_OPTION_PREFIX + "SPORTS")
        .performClick()
    composeTestRule.waitForIdle()

    // Fill in description
    composeTestRule
        .onNodeWithTag(CreateGroupScreenTestTags.GROUP_DESCRIPTION_TEXT_FIELD)
        .performTextInput("A sports group")
    composeTestRule.waitForIdle()

    // Verify save button is enabled
    composeTestRule.onNodeWithTag(CreateGroupScreenTestTags.SAVE_BUTTON).assertIsEnabled()
  }

  @Test
  fun completeForm_switchingCategories_updatesDropdown() {
    composeTestRule.setContent { CreateGroupScreen(viewModel = viewModel) }

    // Select Social
    composeTestRule.onNodeWithContentDescription("Dropdown").performClick()
    composeTestRule.waitForIdle()
    composeTestRule
        .onNodeWithTag(CreateGroupScreenTestTags.CATEGORY_OPTION_PREFIX + "SOCIAL")
        .performClick()
    composeTestRule.waitForIdle()
    composeTestRule
        .onNodeWithTag(CreateGroupScreenTestTags.CATEGORY_DROPDOWN)
        .assertTextContains("Social")

    // Switch to Sports
    composeTestRule.onNodeWithContentDescription("Dropdown").performClick()
    composeTestRule.waitForIdle()
    composeTestRule
        .onNodeWithTag(CreateGroupScreenTestTags.CATEGORY_OPTION_PREFIX + "SPORTS")
        .performClick()
    composeTestRule.waitForIdle()
    composeTestRule
        .onNodeWithTag(CreateGroupScreenTestTags.CATEGORY_DROPDOWN)
        .assertTextContains("Sports")
  }

  @Test
  fun nameValidation_updatesInRealTime() {
    composeTestRule.setContent { CreateGroupScreen(viewModel = viewModel) }

    // Start with invalid name
    composeTestRule
        .onNodeWithTag(CreateGroupScreenTestTags.GROUP_NAME_TEXT_FIELD)
        .performTextInput("ab")
    composeTestRule.waitForIdle()
    composeTestRule
        .onNodeWithTag(CreateGroupScreenTestTags.NAME_SUPPORTING_TEXT)
        .assertTextEquals("Name must be at least 3 characters")

    // Add more characters to make it valid
    composeTestRule
        .onNodeWithTag(CreateGroupScreenTestTags.GROUP_NAME_TEXT_FIELD)
        .performTextInput("c")
    composeTestRule.waitForIdle()
    composeTestRule
        .onNodeWithTag(CreateGroupScreenTestTags.NAME_SUPPORTING_TEXT)
        .assertTextEquals("3-30 characters. Letters, numbers, spaces, or underscores only")
  }
}
