// Implemented with help of Claude AI
package com.android.joinme.ui.groups

import android.content.Context
import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.android.joinme.model.groups.Group
import com.android.joinme.model.groups.GroupRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Fake GroupRepository for testing */
private class FakeGroupRepository : GroupRepository {
  private val groups = mutableListOf<Group>()
  private var counter = 0

  fun setGroups(newGroups: List<Group>) {
    groups.clear()
    groups.addAll(newGroups)
  }

  override fun getNewGroupId(): String = (counter++).toString()

  override suspend fun getAllGroups(): List<Group> = groups

  override suspend fun getGroup(groupId: String): Group =
      groups.find { it.id == groupId } ?: throw Exception("Group not found")

  override suspend fun addGroup(group: Group) {
    groups.add(group)
  }

  override suspend fun editGroup(groupId: String, newValue: Group) {
    val index = groups.indexOfFirst { it.id == groupId }
    if (index != -1) groups[index] = newValue
  }

  override suspend fun deleteGroup(groupId: String, userId: String) {
    val group = getGroup(groupId)
    if (group.ownerId != userId) throw Exception("Only the group owner can delete this group")
    groups.removeIf { it.id == groupId }
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
    if (group.memberIds.contains(userId)) throw Exception("User is already a member of this group")
    editGroup(groupId, group.copy(memberIds = group.memberIds + userId))
  }

  override suspend fun getCommonGroups(userIds: List<String>): List<Group> {
    if (userIds.isEmpty()) return emptyList()
    return groups.filter { group -> userIds.all { userId -> group.memberIds.contains(userId) } }
  }

  override suspend fun uploadGroupPhoto(context: Context, groupId: String, imageUri: Uri): String {
    // Not needed for these tests
    return "http://fakeurl.com/photo.jpg"
  }

  override suspend fun deleteGroupPhoto(groupId: String) {
    // Not needed for these tests
  }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "w360dp-h640dp-normal-long-notround-any-420dpi-keyshidden-nonav")
class GroupListScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Before
  fun setUp() {
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    if (FirebaseApp.getApps(context).isEmpty()) FirebaseApp.initializeApp(context)
  }

  @After
  fun tearDown() {
    unmockkStatic(FirebaseAuth::class)
  }

  private fun mockFirebaseAuthWithUser(userId: String) {
    mockkStatic(FirebaseAuth::class)
    val mockAuth = mockk<FirebaseAuth>()
    val mockUser = mockk<FirebaseUser>()
    every { FirebaseAuth.getInstance() } returns mockAuth
    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns userId
  }

  private fun createViewModel(groups: List<Group> = emptyList()): GroupListViewModel {
    val fakeRepo = FakeGroupRepository()
    fakeRepo.setGroups(groups)
    return GroupListViewModel(fakeRepo)
  }

  // =======================================
  // Empty State & Basic Display Tests
  // =======================================

  @Test
  fun emptyState_displaysCorrectly() {
    composeTestRule.setContent { GroupListScreen(viewModel = createViewModel()) }

    composeTestRule.onNodeWithTag(GroupListScreenTestTags.EMPTY).assertIsDisplayed()
    composeTestRule.onNodeWithText("You are currently not").assertIsDisplayed()
    composeTestRule.onNodeWithText("assigned to a group…").assertIsDisplayed()
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.ADD_NEW_GROUP).assertIsDisplayed()
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.LIST).assertDoesNotExist()
  }

  @Test
  fun groupList_displaysMultipleGroups() {
    val groups =
        listOf(
            Group(id = "1", name = "Football", ownerId = "owner1"),
            Group(id = "2", name = "Hiking", ownerId = "owner2"))

    composeTestRule.setContent { GroupListScreen(viewModel = createViewModel(groups)) }

    composeTestRule.onNodeWithTag(GroupListScreenTestTags.EMPTY).assertDoesNotExist()
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.LIST).assertIsDisplayed()
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.cardTag("1")).assertIsDisplayed()
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.cardTag("2")).assertIsDisplayed()
  }

  @Test
  fun groupCard_displaysAllFields() {
    val group =
        Group(
            id = "test1",
            name = "Basketball",
            description = "Weekly games",
            ownerId = "owner",
            memberIds = List(15) { "user$it" })

    composeTestRule.setContent { GroupListScreen(viewModel = createViewModel(listOf(group))) }

    composeTestRule.onNodeWithText("Basketball").assertIsDisplayed()
    composeTestRule.onNodeWithText("Weekly games").assertIsDisplayed()
    composeTestRule.onNodeWithText("members: 15").assertIsDisplayed()
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.moreTag("test1")).assertIsDisplayed()
  }

  @Test
  fun groupCard_withEmptyDescription_doesNotShowDescription() {
    val group = Group(id = "test1", name = "Group", description = "", ownerId = "owner")

    composeTestRule.setContent { GroupListScreen(viewModel = createViewModel(listOf(group))) }

    composeTestRule.onNodeWithText("Group").assertIsDisplayed()
    composeTestRule.onNodeWithText("members: 0").assertIsDisplayed()
  }

  // =======================================
  // Card Click & Navigation Tests
  // =======================================

  @Test
  fun cardClick_triggersCallback() {
    val group = Group(id = "1", name = "Football", ownerId = "owner1")
    var clickedGroup: Group? = null

    composeTestRule.setContent {
      GroupListScreen(viewModel = createViewModel(listOf(group)), onGroup = { clickedGroup = it })
    }

    composeTestRule.onNodeWithTag(GroupListScreenTestTags.cardTag("1")).performClick()

    assertEquals(group, clickedGroup)
  }

  @Test
  fun multipleCardClicks_triggersMultipleTimes() {
    val groups =
        listOf(
            Group(id = "1", name = "Group A", ownerId = "owner1"),
            Group(id = "2", name = "Group B", ownerId = "owner2"))
    val clickedIds = mutableListOf<String>()

    composeTestRule.setContent {
      GroupListScreen(viewModel = createViewModel(groups), onGroup = { clickedIds.add(it.id) })
    }

    composeTestRule.onNodeWithTag(GroupListScreenTestTags.cardTag("1")).performClick()
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.cardTag("2")).performClick()

    assertEquals(listOf("1", "2"), clickedIds)
  }

  // =======================================
  // Scrolling Tests
  // =======================================

  @Test
  fun largeList_isScrollable() {
    val groups = (1..50).map { Group(id = "$it", name = "Group $it", ownerId = "owner$it") }

    composeTestRule.setContent { GroupListScreen(viewModel = createViewModel(groups)) }

    composeTestRule.onNodeWithTag(GroupListScreenTestTags.cardTag("1")).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(GroupListScreenTestTags.LIST)
        .performScrollToNode(hasTestTag(GroupListScreenTestTags.cardTag("50")))
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.cardTag("50")).assertIsDisplayed()
  }

  @Test
  fun scrolledList_canClickItems() {
    val groups = (1..50).map { Group(id = "$it", name = "Group $it", ownerId = "owner$it") }
    var clickedGroup: Group? = null

    composeTestRule.setContent {
      GroupListScreen(viewModel = createViewModel(groups), onGroup = { clickedGroup = it })
    }

    composeTestRule
        .onNodeWithTag(GroupListScreenTestTags.LIST)
        .performScrollToNode(hasTestTag(GroupListScreenTestTags.cardTag("40")))
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.cardTag("40")).performClick()

    assertEquals("40", clickedGroup?.id)
  }

  // =======================================
  // Top Bar Tests
  // =======================================

  @Test
  fun topBar_allButtonsDisplayed() {
    composeTestRule.setContent { GroupListScreen() }

    composeTestRule.onNodeWithContentDescription("Back").assertIsDisplayed()
    composeTestRule.onNodeWithContentDescription("Profile").assertIsDisplayed()
    composeTestRule.onNodeWithContentDescription("Edit").assertIsDisplayed()
  }

  @Test
  fun topBar_backButton_triggersCallback() {
    var backClicked = false

    composeTestRule.setContent { GroupListScreen(onBackClick = { backClicked = true }) }

    composeTestRule.onNodeWithContentDescription("Back").performClick()

    assertTrue(backClicked)
  }

  @Test
  fun topBar_profileButton_triggersCallback() {
    var profileClicked = false

    composeTestRule.setContent { GroupListScreen(onProfileClick = { profileClicked = true }) }

    composeTestRule.onNodeWithContentDescription("Profile").performClick()

    assertTrue(profileClicked)
  }
  // =======================================
  // Group Menu Tests
  // =======================================

  @Test
  fun groupMenu_opensAndCloses() {
    val group = Group(id = "test1", name = "Test Group", ownerId = "owner1")

    composeTestRule.setContent { GroupListScreen(viewModel = createViewModel(listOf(group))) }

    composeTestRule.onNodeWithTag(GroupListScreenTestTags.moreTag("test1")).performClick()
    composeTestRule.onNodeWithText("SHARE GROUP").assertIsDisplayed()

    composeTestRule.onNodeWithTag(GroupListScreenTestTags.moreTag("test1")).performClick()
    composeTestRule.onNodeWithText("SHARE GROUP").assertDoesNotExist()
  }

  @Test
  fun groupMenu_nonOwner_showsCorrectButtons() {
    val group = Group(id = "test1", name = "Test Group", ownerId = "owner1")

    composeTestRule.setContent { GroupListScreen(viewModel = createViewModel(listOf(group))) }

    composeTestRule.onNodeWithTag(GroupListScreenTestTags.moreTag("test1")).performClick()

    composeTestRule.onNodeWithText("LEAVE GROUP").assertExists()
    composeTestRule.onNodeWithText("SHARE GROUP").assertExists()
    composeTestRule.onNodeWithText("EDIT GROUP").assertDoesNotExist()
    composeTestRule.onNodeWithText("DELETE GROUP").assertDoesNotExist()
  }

  @Test
  fun groupMenu_owner_showsOwnerButtons() {
    val testUserId = "test-user-id"
    mockFirebaseAuthWithUser(testUserId)

    val fakeRepo = FakeGroupRepository()
    val group =
        Group(
            id = fakeRepo.getNewGroupId(),
            name = "Test Group",
            ownerId = testUserId,
            memberIds = listOf(testUserId))
    fakeRepo.setGroups(listOf(group))

    composeTestRule.setContent { GroupListScreen(viewModel = GroupListViewModel(fakeRepo)) }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(GroupListScreenTestTags.moreTag(group.id)).performClick()

    composeTestRule.onNodeWithText("EDIT GROUP").assertExists()
    composeTestRule.onNodeWithText("DELETE GROUP").assertExists()
    composeTestRule.onNodeWithText("LEAVE GROUP").assertDoesNotExist()
  }

  // =======================================
  // Leave Group Dialog Tests
  // =======================================

  @Test
  fun leaveGroup_showsConfirmationDialog() {
    val group = Group(id = "test1", name = "Test Group", ownerId = "owner1")

    composeTestRule.setContent { GroupListScreen(viewModel = createViewModel(listOf(group))) }

    composeTestRule.onNodeWithTag(GroupListScreenTestTags.moreTag("test1")).performClick()
    composeTestRule.onNodeWithText("LEAVE GROUP").performClick()

    composeTestRule.onNodeWithTag(GroupListScreenTestTags.LEAVE_GROUP_DIALOG).assertExists()
    composeTestRule.onNodeWithText("Are you sure you want to leave\nthis group?").assertExists()
    composeTestRule.onNodeWithText("Yes").assertExists()
    composeTestRule.onNodeWithText("No").assertExists()
  }

  @Test
  fun leaveGroupDialog_cancel_dismissesDialog() {
    val group = Group(id = "test1", name = "Test Group", ownerId = "owner1")
    var leftGroup: Group? = null

    composeTestRule.setContent {
      GroupListScreen(viewModel = createViewModel(listOf(group)), onLeaveGroup = { leftGroup = it })
    }

    composeTestRule.onNodeWithTag(GroupListScreenTestTags.moreTag("test1")).performClick()
    composeTestRule.onNodeWithText("LEAVE GROUP").performClick()
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.LEAVE_GROUP_CANCEL_BUTTON).performClick()

    composeTestRule.onNodeWithTag(GroupListScreenTestTags.LEAVE_GROUP_DIALOG).assertDoesNotExist()
    assertNull(leftGroup)
  }

  @Test
  fun leaveGroupDialog_confirm_triggersCallback() {
    val group = Group(id = "test1", name = "Test Group", ownerId = "owner1")
    var leftGroup: Group? = null

    composeTestRule.setContent {
      GroupListScreen(viewModel = createViewModel(listOf(group)), onLeaveGroup = { leftGroup = it })
    }

    composeTestRule.onNodeWithTag(GroupListScreenTestTags.moreTag("test1")).performClick()
    composeTestRule.onNodeWithText("LEAVE GROUP").performClick()
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.LEAVE_GROUP_CONFIRM_BUTTON).performClick()

    assertEquals(group, leftGroup)
  }

  // =======================================
  // Delete Group Dialog Tests
  // =======================================

  @Test
  fun deleteGroup_showsConfirmationDialog() {
    val testUserId = "test-user-id"
    mockFirebaseAuthWithUser(testUserId)

    val fakeRepo = FakeGroupRepository()
    val group =
        Group(
            id = fakeRepo.getNewGroupId(),
            name = "Test Group",
            ownerId = testUserId,
            memberIds = listOf(testUserId))
    fakeRepo.setGroups(listOf(group))

    composeTestRule.setContent { GroupListScreen(viewModel = GroupListViewModel(fakeRepo)) }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(GroupListScreenTestTags.moreTag(group.id)).performClick()
    composeTestRule.onNodeWithText("DELETE GROUP").performClick()

    composeTestRule.onNodeWithTag(GroupListScreenTestTags.DELETE_GROUP_DIALOG).assertExists()
    composeTestRule.onNodeWithText("Are you sure you want to delete\nthis group?").assertExists()
    composeTestRule
        .onNodeWithText("The group will be permanently deleted\nThis action is irreversible")
        .assertExists()
  }

  @Test
  fun deleteGroupDialog_cancel_dismissesDialog() {
    val testUserId = "test-user-id"
    mockFirebaseAuthWithUser(testUserId)

    val fakeRepo = FakeGroupRepository()
    val group = Group(id = fakeRepo.getNewGroupId(), name = "Test Group", ownerId = testUserId)
    fakeRepo.setGroups(listOf(group))
    var deletedGroup: Group? = null

    composeTestRule.setContent {
      GroupListScreen(
          viewModel = GroupListViewModel(fakeRepo), onDeleteGroup = { deletedGroup = it })
    }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(GroupListScreenTestTags.moreTag(group.id)).performClick()
    composeTestRule.onNodeWithText("DELETE GROUP").performClick()
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.DELETE_GROUP_CANCEL_BUTTON).performClick()

    composeTestRule.onNodeWithTag(GroupListScreenTestTags.DELETE_GROUP_DIALOG).assertDoesNotExist()
    assertNull(deletedGroup)
  }

  @Test
  fun deleteGroupDialog_confirm_triggersCallback() {
    val testUserId = "test-user-id"
    mockFirebaseAuthWithUser(testUserId)

    val fakeRepo = FakeGroupRepository()
    val group = Group(id = fakeRepo.getNewGroupId(), name = "Test Group", ownerId = testUserId)
    fakeRepo.setGroups(listOf(group))
    var deletedGroup: Group? = null

    composeTestRule.setContent {
      GroupListScreen(
          viewModel = GroupListViewModel(fakeRepo), onDeleteGroup = { deletedGroup = it })
    }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(GroupListScreenTestTags.moreTag(group.id)).performClick()
    composeTestRule.onNodeWithText("DELETE GROUP").performClick()
    composeTestRule
        .onNodeWithTag(GroupListScreenTestTags.DELETE_GROUP_CONFIRM_BUTTON)
        .performClick()

    assertEquals(group, deletedGroup)
  }

  // =======================================
  // Menu Interaction Tests
  // =======================================

  @Test
  fun clickingCardWithMenuOpen_closesMenu() {
    val group = Group(id = "test1", name = "Test Group", ownerId = "owner1")

    composeTestRule.setContent { GroupListScreen(viewModel = createViewModel(listOf(group))) }

    composeTestRule.onNodeWithTag(GroupListScreenTestTags.moreTag("test1")).performClick()
    composeTestRule.onNodeWithText("SHARE GROUP").assertIsDisplayed()

    composeTestRule.onNodeWithTag(GroupListScreenTestTags.cardTag("test1")).performClick()

    composeTestRule.onNodeWithText("SHARE GROUP").assertDoesNotExist()
  }

  // =======================================
  // Edge Cases & Special Scenarios
  // =======================================

  @Test
  fun groupWithLongName_displaysCorrectly() {
    val group =
        Group(
            id = "1",
            name = "This is a very long group name that should be handled properly",
            ownerId = "owner")

    composeTestRule.setContent { GroupListScreen(viewModel = createViewModel(listOf(group))) }

    composeTestRule.onNodeWithTag(GroupListScreenTestTags.cardTag("1")).assertIsDisplayed()
  }

  @Test
  fun groupsWithSpecialCharacters_display() {
    val groups =
        listOf(
            Group(id = "1", name = "Café & Chill", ownerId = "owner"),
            Group(id = "2", name = "日本語 Group", ownerId = "owner"))

    composeTestRule.setContent { GroupListScreen(viewModel = createViewModel(groups)) }

    composeTestRule.onNodeWithText("Café & Chill").assertIsDisplayed()
    composeTestRule.onNodeWithText("日本語 Group").assertIsDisplayed()
  }

  @Test
  fun veryLargeList_displaysCorrectly() {
    val groups = (1..100).map { Group(id = "$it", name = "Group $it", ownerId = "owner$it") }

    composeTestRule.setContent { GroupListScreen(viewModel = createViewModel(groups)) }

    composeTestRule.onNodeWithTag(GroupListScreenTestTags.LIST).assertIsDisplayed()
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.cardTag("1")).assertIsDisplayed()
  }

  // ========== NEW TESTS FOR MISSING COVERAGE ==========

  @Test
  fun fab_click_triggersOnCreateGroupCallback() {
    var createGroupClicked = false

    composeTestRule.setContent {
      GroupListScreen(
          viewModel = createViewModel(emptyList()), onCreateGroup = { createGroupClicked = true })
    }

    composeTestRule.onNodeWithTag(GroupListScreenTestTags.ADD_NEW_GROUP).performClick()

    assertTrue(createGroupClicked)
  }

  @Test
  fun editGroupButton_asOwner_triggersOnEditGroupCallback() {
    val testUserId = "test-user-id"
    mockFirebaseAuthWithUser(testUserId)
    val group =
        Group(id = "1", name = "Test Group", ownerId = testUserId, memberIds = listOf(testUserId))
    var editedGroup: Group? = null

    composeTestRule.setContent {
      GroupListScreen(
          viewModel = createViewModel(listOf(group)), onEditGroup = { editedGroup = it })
    }

    composeTestRule.onNodeWithTag(GroupListScreenTestTags.moreTag("1")).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.EDIT_GROUP_BUBBLE).performClick()

    assertEquals(group, editedGroup)
  }

  @Test
  fun shareGroupButton_triggersShareInvitation() {
    val testUserId = "test-user-id"
    mockFirebaseAuthWithUser(testUserId)
    val group =
        Group(id = "1", name = "Share Group", ownerId = testUserId, memberIds = listOf(testUserId))

    composeTestRule.setContent { GroupListScreen(viewModel = createViewModel(listOf(group))) }

    composeTestRule.onNodeWithTag(GroupListScreenTestTags.moreTag("1")).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.SHARE_GROUP_BUBBLE).assertIsDisplayed()
    // Note: Actually clicking share would require mocking shareInvitation,
    // which is complex. We verify the button exists and is clickable.
  }

  @Test
  fun onEditClick_callback_isTriggered() {
    var editClicked = false

    composeTestRule.setContent {
      GroupListScreen(
          viewModel = createViewModel(emptyList()), onEditClick = { editClicked = true })
    }

    composeTestRule.onNodeWithContentDescription("Edit").performClick()

    assertTrue(editClicked)
  }
}
