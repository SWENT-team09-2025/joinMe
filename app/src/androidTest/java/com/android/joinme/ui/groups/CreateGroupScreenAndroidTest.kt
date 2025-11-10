package com.android.joinme.ui.groups

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.android.joinme.model.groups.Group
import com.android.joinme.model.groups.GroupRepository
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Android instrumented test for CreateGroupScreen components that don't work with Robolectric.
 * Other tests for this screen are in the unit test version.
 */
class CreateGroupScreenAndroidTest {

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
}
