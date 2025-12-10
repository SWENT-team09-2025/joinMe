// Implemented with help of Claude AI
package com.android.joinme.ui.groups

import android.content.Context
import android.net.Uri
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.android.joinme.model.event.EventType
import com.android.joinme.model.groups.Group
import com.android.joinme.model.groups.GroupRepository
import com.google.firebase.FirebaseApp
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "w360dp-h640dp-normal-long-notround-any-420dpi-keyshidden-nonav")
class EditGroupScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var fakeRepository: FakeGroupRepository
  private lateinit var viewModel: EditGroupViewModel
  private lateinit var testGroup: Group

  @Before
  fun setUp() {
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    if (FirebaseApp.getApps(context).isEmpty()) {
      FirebaseApp.initializeApp(context)
    }

    fakeRepository = FakeGroupRepository()
    viewModel = EditGroupViewModel(fakeRepository)

    testGroup = createTestGroup()
    fakeRepository.addGroupForTest(testGroup)
  }

  // ==========================================
  // Test Helpers
  // ==========================================

  private fun createTestGroup(
      id: String = "test-group-1",
      name: String = "Basketball Team",
      category: EventType = EventType.SPORTS,
      description: String = "Weekly basketball games",
      photoUrl: String? = null
  ) =
      Group(
          id = id,
          name = name,
          category = category,
          description = description,
          ownerId = "owner-123",
          memberIds = listOf("owner-123", "member-456"),
          eventIds = listOf("event-1", "event-2"),
          photoUrl = photoUrl)

  private fun setScreenContent(groupId: String = testGroup.id) {
    composeTestRule.setContent { EditGroupScreen(groupId = groupId, viewModel = viewModel) }
    composeTestRule.waitForIdle()
  }

  private fun waitForFormLoaded() {
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(EditGroupScreenTags.GROUP_NAME_TEXT_FIELD)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
  }

  private fun waitForDeleteButtonGone() {
    composeTestRule.waitUntil(timeoutMillis = 3000) {
      composeTestRule
          .onAllNodesWithTag(EditGroupScreenTags.DELETE_PHOTO_BUTTON)
          .fetchSemanticsNodes()
          .isEmpty()
    }
  }

  // ==========================================
  // Fake Repository
  // ==========================================

  private class FakeGroupRepository : GroupRepository {
    private val groups = mutableListOf<Group>()
    private var idCounter = 0

    fun addGroupForTest(group: Group) {
      groups.removeAll { it.id == group.id }
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
      if (group.ownerId != userId) throw Exception("Only the group owner can delete this group")
      if (!groups.removeIf { it.id == groupId }) throw Exception("Group not found")
    }

    override suspend fun leaveGroup(groupId: String, userId: String) {
      val group = getGroup(groupId)
      val updatedMemberIds = group.memberIds.filter { it != userId }
      if (updatedMemberIds.size == group.memberIds.size)
          throw Exception("User is not a member of this group")
      editGroup(groupId, group.copy(memberIds = updatedMemberIds))
    }

    override suspend fun joinGroup(groupId: String, userId: String) {
      val group = getGroup(groupId)
      if (group.memberIds.contains(userId))
          throw Exception("User is already a member of this group")
      editGroup(groupId, group.copy(memberIds = group.memberIds + userId))
    }

    override suspend fun getCommonGroups(userIds: List<String>): List<Group> {
      if (userIds.isEmpty()) return emptyList()
      return groups.filter { group -> userIds.all { userId -> group.memberIds.contains(userId) } }
    }

    override suspend fun uploadGroupPhoto(
        context: Context,
        groupId: String,
        imageUri: Uri
    ): String {
      val newUrl = "https://fake-storage.com/${groupId}.jpg"
      val group = getGroup(groupId)
      editGroup(groupId, group.copy(photoUrl = newUrl))
      return newUrl
    }

    override suspend fun deleteGroupPhoto(groupId: String) {
      val group = getGroup(groupId)
      editGroup(groupId, group.copy(photoUrl = null))
    }
  }

  // ==========================================
  // Display Tests
  // ==========================================

  @Test
  fun editGroupScreen_displaysAllComponents() {
    setScreenContent()
    waitForFormLoaded()

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
    composeTestRule.onNodeWithTag(EditGroupScreenTags.DESCRIPTION_SUPPORTING_TEXT).assertExists()
    composeTestRule.onNodeWithTag(EditGroupScreenTags.SAVE_BUTTON).assertExists()
  }

  @Test
  fun editGroupScreen_loadsAndDisplaysOriginalGroupData() {
    setScreenContent()
    waitForFormLoaded()

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
    setScreenContent()
    waitForFormLoaded()

    composeTestRule.onNodeWithTag(EditGroupScreenTags.SAVE_BUTTON).assertIsEnabled()
  }

  @Test
  fun editGroupScreen_titleDisplaysCorrectText() {
    setScreenContent()
    waitForFormLoaded()

    composeTestRule.onNodeWithTag(EditGroupScreenTags.TITLE).assertTextEquals("Edit Group")
  }

  // ==========================================
  // Name Input Tests
  // ==========================================

  @Test
  fun nameInput_canModifyExistingName() {
    setScreenContent()
    waitForFormLoaded()

    composeTestRule.onNodeWithTag(EditGroupScreenTags.GROUP_NAME_TEXT_FIELD).performTextClearance()
    composeTestRule
        .onNodeWithTag(EditGroupScreenTags.GROUP_NAME_TEXT_FIELD)
        .performTextInput("Soccer Team")

    composeTestRule
        .onNodeWithTag(EditGroupScreenTags.GROUP_NAME_TEXT_FIELD)
        .assertTextContains("Soccer Team")
  }

  @Test
  fun nameInput_invalidShortName_disablesSaveButton() {
    setScreenContent()
    waitForFormLoaded()

    composeTestRule.onNodeWithTag(EditGroupScreenTags.GROUP_NAME_TEXT_FIELD).performTextClearance()
    composeTestRule.onNodeWithTag(EditGroupScreenTags.GROUP_NAME_TEXT_FIELD).performTextInput("ab")
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(EditGroupScreenTags.SAVE_BUTTON).assertIsNotEnabled()
  }

  @Test
  fun nameInput_emptyName_disablesSaveButton() {
    setScreenContent()
    waitForFormLoaded()

    composeTestRule.onNodeWithTag(EditGroupScreenTags.GROUP_NAME_TEXT_FIELD).performTextClearance()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(EditGroupScreenTags.SAVE_BUTTON).assertIsNotEnabled()
  }

  // ==========================================
  // Category Dropdown Tests
  // ==========================================

  @Test
  fun categoryDropdown_opensMenuOnClick() {
    setScreenContent()
    waitForFormLoaded()

    composeTestRule.onNodeWithTag(EditGroupScreenTags.CATEGORY_DROPDOWN).performClick()
    composeTestRule
        .onNodeWithTag(EditGroupScreenTags.CATEGORY_DROPDOWN_TOUCHABLE_AREA)
        .assertIsDisplayed()
  }

  @Test
  fun categoryDropdown_showsAllThreeOptions() {
    setScreenContent()
    waitForFormLoaded()

    composeTestRule.onNodeWithTag(EditGroupScreenTags.CATEGORY_DROPDOWN).performClick()
    composeTestRule.onNodeWithText("SOCIAL").assertIsDisplayed()
    composeTestRule.onNodeWithText("ACTIVITY").assertIsDisplayed()
    composeTestRule.onNodeWithText("SPORTS").assertIsDisplayed()
  }

  @Test
  fun categoryDropdown_canChangeCategoryFromSportsToSocial() {
    setScreenContent()
    waitForFormLoaded()

    composeTestRule
        .onNodeWithTag(EditGroupScreenTags.CATEGORY_DROPDOWN)
        .assertTextContains("Sports")

    composeTestRule.onNodeWithTag(EditGroupScreenTags.CATEGORY_DROPDOWN).performClick()
    composeTestRule.onNodeWithText("SOCIAL").performClick()

    composeTestRule
        .onNodeWithTag(EditGroupScreenTags.CATEGORY_DROPDOWN)
        .assertTextContains("Social")
  }

  @Test
  fun categoryDropdown_canChangeCategoryFromSportsToActivity() {
    setScreenContent()
    waitForFormLoaded()

    composeTestRule.onNodeWithTag(EditGroupScreenTags.CATEGORY_DROPDOWN).performClick()
    composeTestRule.onNodeWithText("ACTIVITY").performClick()

    composeTestRule
        .onNodeWithTag(EditGroupScreenTags.CATEGORY_DROPDOWN)
        .assertTextContains("Activity")
  }

  @Test
  fun categoryDropdown_closesAfterSelection() {
    setScreenContent()
    waitForFormLoaded()

    composeTestRule.onNodeWithTag(EditGroupScreenTags.CATEGORY_DROPDOWN).performClick()
    composeTestRule.onNodeWithText("SOCIAL").performClick()

    composeTestRule
        .onNodeWithTag(EditGroupScreenTags.CATEGORY_DROPDOWN_TOUCHABLE_AREA)
        .assertDoesNotExist()
  }

  // ==========================================
  // Description Input Tests
  // ==========================================

  @Test
  fun descriptionInput_canModifyDescription() {
    setScreenContent()
    waitForFormLoaded()

    composeTestRule
        .onNodeWithTag(EditGroupScreenTags.GROUP_DESCRIPTION_TEXT_FIELD)
        .performTextClearance()
    composeTestRule
        .onNodeWithTag(EditGroupScreenTags.GROUP_DESCRIPTION_TEXT_FIELD)
        .performTextInput("New description")

    composeTestRule
        .onNodeWithTag(EditGroupScreenTags.GROUP_DESCRIPTION_TEXT_FIELD)
        .assertTextContains("New description")
  }

  @Test
  fun descriptionInput_accepts300Characters() {
    setScreenContent()
    waitForFormLoaded()

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
  fun descriptionInput_canClearDescriptionCompletely() {
    setScreenContent()
    waitForFormLoaded()

    composeTestRule
        .onNodeWithTag(EditGroupScreenTags.GROUP_DESCRIPTION_TEXT_FIELD)
        .performTextClearance()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(EditGroupScreenTags.SAVE_BUTTON).assertIsEnabled()
  }

  @Test
  fun descriptionInput_tooLong_disablesSaveButton() {
    setScreenContent()
    waitForFormLoaded()

    composeTestRule
        .onNodeWithTag(EditGroupScreenTags.GROUP_DESCRIPTION_TEXT_FIELD)
        .performTextClearance()
    composeTestRule
        .onNodeWithTag(EditGroupScreenTags.GROUP_DESCRIPTION_TEXT_FIELD)
        .performTextInput("a".repeat(301))
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(EditGroupScreenTags.SAVE_BUTTON).assertIsNotEnabled()
  }

  // ==========================================
  // Save Button Tests
  // ==========================================

  @Test
  fun saveButton_enabledWhenFormIsValid() {
    setScreenContent()
    waitForFormLoaded()

    composeTestRule.onNodeWithTag(EditGroupScreenTags.GROUP_NAME_TEXT_FIELD).performTextClearance()
    composeTestRule
        .onNodeWithTag(EditGroupScreenTags.GROUP_NAME_TEXT_FIELD)
        .performTextInput("Valid Group")
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(EditGroupScreenTags.SAVE_BUTTON).assertIsEnabled()
  }

  @Test
  fun saveButton_disabledWhenNameIsInvalid() {
    setScreenContent()
    waitForFormLoaded()

    composeTestRule.onNodeWithTag(EditGroupScreenTags.GROUP_NAME_TEXT_FIELD).performTextClearance()
    composeTestRule.onNodeWithTag(EditGroupScreenTags.GROUP_NAME_TEXT_FIELD).performTextInput("ab")
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(EditGroupScreenTags.SAVE_BUTTON).assertIsNotEnabled()
  }

  @Test
  fun saveButton_disabledWhenDescriptionIsTooLong() {
    setScreenContent()
    waitForFormLoaded()

    composeTestRule
        .onNodeWithTag(EditGroupScreenTags.GROUP_DESCRIPTION_TEXT_FIELD)
        .performTextClearance()
    composeTestRule
        .onNodeWithTag(EditGroupScreenTags.GROUP_DESCRIPTION_TEXT_FIELD)
        .performTextInput("a".repeat(301))
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(EditGroupScreenTags.SAVE_BUTTON).assertIsNotEnabled()
  }

  // ==========================================
  // Complete Form Tests
  // ==========================================

  @Test
  fun completeForm_modifyAllFields_enablesSaveButton() {
    setScreenContent()
    waitForFormLoaded()

    composeTestRule.onNodeWithTag(EditGroupScreenTags.GROUP_NAME_TEXT_FIELD).performTextClearance()
    composeTestRule
        .onNodeWithTag(EditGroupScreenTags.GROUP_NAME_TEXT_FIELD)
        .performTextInput("Updated Team")
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(EditGroupScreenTags.CATEGORY_DROPDOWN).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("SOCIAL").performClick()
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

  // ==========================================
  // Group Photo Tests
  // ==========================================

  @Test
  fun photoSection_displaysPlaceholder_whenNoPhoto() {
    setScreenContent()
    waitForFormLoaded()

    // Use test tag for reliable detection (content description may vary with Coil loading states)
    composeTestRule
        .onNodeWithTag(GroupPhotoImageTestTags.GROUP_PHOTO_PLACEHOLDER)
        .assertIsDisplayed()
    composeTestRule.onNodeWithTag(EditGroupScreenTags.EDIT_PHOTO_BUTTON).assertIsDisplayed()
  }

  @Test
  fun photoSection_displaysPhotoSection_whenPhotoExists() {
    val groupWithPhoto = createTestGroup(photoUrl = "https://example.com/existing.jpg")
    fakeRepository.addGroupForTest(groupWithPhoto)
    viewModel = EditGroupViewModel(fakeRepository)

    setScreenContent()
    waitForFormLoaded()

    // Photo section should be displayed (actual image loading may fail in Robolectric)
    composeTestRule.onNodeWithTag(EditGroupScreenTags.GROUP_PICTURE).assertIsDisplayed()
  }

  @Test
  fun deletePhotoButton_isHidden_whenNoPhoto() {
    setScreenContent()
    waitForFormLoaded()

    composeTestRule.onNodeWithTag(EditGroupScreenTags.DELETE_PHOTO_BUTTON).assertDoesNotExist()
  }

  @Test
  fun deletePhotoButton_isVisible_whenPhotoExists() {
    val groupWithPhoto = createTestGroup(photoUrl = "https://example.com/existing.jpg")
    fakeRepository.addGroupForTest(groupWithPhoto)
    viewModel = EditGroupViewModel(fakeRepository)

    setScreenContent()
    waitForFormLoaded()

    composeTestRule
        .onNodeWithTag(EditGroupScreenTags.DELETE_PHOTO_BUTTON)
        .assertIsDisplayed()
        .assertHasClickAction()
  }

  @Test
  fun deletePhotoButton_click_removesPhotoAndHidesButton() {
    val groupWithPhoto = createTestGroup(photoUrl = "https://example.com/existing.jpg")
    fakeRepository.addGroupForTest(groupWithPhoto)
    viewModel = EditGroupViewModel(fakeRepository)

    setScreenContent()
    waitForFormLoaded()

    composeTestRule.onNodeWithTag(EditGroupScreenTags.DELETE_PHOTO_BUTTON).performClick()

    waitForDeleteButtonGone()

    // Verify placeholder is shown after deletion
    composeTestRule
        .onNodeWithTag(GroupPhotoImageTestTags.GROUP_PHOTO_PLACEHOLDER)
        .assertIsDisplayed()
  }

  @Test
  fun editPhotoButton_isAlwaysVisible() {
    setScreenContent()
    waitForFormLoaded()

    composeTestRule
        .onNodeWithTag(EditGroupScreenTags.EDIT_PHOTO_BUTTON)
        .assertIsDisplayed()
        .assertHasClickAction()
  }

  @Test
  fun editPhotoButton_isVisibleEvenWithPhoto() {
    val groupWithPhoto = createTestGroup(photoUrl = "https://example.com/existing.jpg")
    fakeRepository.addGroupForTest(groupWithPhoto)
    viewModel = EditGroupViewModel(fakeRepository)

    setScreenContent()
    waitForFormLoaded()

    composeTestRule
        .onNodeWithTag(EditGroupScreenTags.EDIT_PHOTO_BUTTON)
        .assertIsDisplayed()
        .assertHasClickAction()
  }
}
