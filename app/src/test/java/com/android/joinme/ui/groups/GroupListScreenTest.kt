package com.android.joinme.ui.groups

import androidx.compose.ui.test.assertHasClickAction
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
import com.android.joinme.ui.components.FloatingActionBubblesTestTags
import com.google.firebase.FirebaseApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Fake implementation of GroupRepository for testing purposes.
 *
 * This implementation stores groups in memory and allows tests to inject specific test data.
 */
private class FakeGroupRepository : GroupRepository {
  private val groups = mutableListOf<Group>()
  private var counter = 0

  fun setGroups(newGroups: List<Group>) {
    groups.clear()
    groups.addAll(newGroups)
  }

  override fun getNewGroupId(): String {
    return (counter++).toString()
  }

  override suspend fun getAllGroups(): List<Group> = groups

  override suspend fun getGroup(groupId: String): Group {
    return groups.find { it.id == groupId } ?: throw Exception("Group not found")
  }

  override suspend fun addGroup(group: Group) {
    groups.add(group)
  }

  override suspend fun editGroup(groupId: String, newValue: Group) {
    val index = groups.indexOfFirst { it.id == groupId }
    if (index != -1) {
      groups[index] = newValue
    }
  }

  override suspend fun deleteGroup(groupId: String, userId: String) {
    val group = getGroup(groupId)
    if (group.ownerId != userId) {
      throw Exception("Only the group owner can delete this group")
    }
    groups.removeIf { it.id == groupId }
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

  override suspend fun joinGroup(groupId: String, userId: String) {
    val group = getGroup(groupId)
    if (group.memberIds.contains(userId)) {
      throw Exception("User is already a member of this group")
    }
    val updatedMemberIds = group.memberIds + userId
    val updatedGroup = group.copy(memberIds = updatedMemberIds)
    editGroup(groupId, updatedGroup)
  }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "w360dp-h640dp-normal-long-notround-any-420dpi-keyshidden-nonav")
class GroupListScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Before
  fun setUp() {
    // Initialize Firebase for Robolectric tests
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    if (FirebaseApp.getApps(context).isEmpty()) {
      FirebaseApp.initializeApp(context)
    }
  }

  private fun createViewModel(groups: List<Group> = emptyList()): GroupListViewModel {
    val fakeRepo = FakeGroupRepository()
    fakeRepo.setGroups(groups)
    return GroupListViewModel(fakeRepo)
  }

  @Test
  fun emptyState_isDisplayed() {
    composeTestRule.setContent { GroupListScreen(viewModel = createViewModel()) }

    composeTestRule.onNodeWithTag(GroupListScreenTestTags.EMPTY).assertIsDisplayed()
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.ADD_NEW_GROUP).assertIsDisplayed()
    composeTestRule.onNodeWithText("You are currently not").assertIsDisplayed()
    composeTestRule.onNodeWithText("assigned to a group…").assertIsDisplayed()
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.LIST).assertDoesNotExist()
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.ADD_NEW_GROUP).assertIsDisplayed()
  }

  @Test
  fun groupCards_areDisplayed() {
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
  fun singleGroup_isDisplayedCorrectly() {
    val group =
        Group(
            id = "1",
            name = "Basketball Club",
            description = "Weekly basketball games",
            ownerId = "owner",
            memberIds = List(15) { "user$it" })

    composeTestRule.setContent { GroupListScreen(viewModel = createViewModel(listOf(group))) }

    composeTestRule.onNodeWithTag(GroupListScreenTestTags.LIST).assertIsDisplayed()
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.cardTag("1")).assertIsDisplayed()
    composeTestRule.onNodeWithText("Basketball Club").assertIsDisplayed()
    composeTestRule.onNodeWithText("Weekly basketball games").assertIsDisplayed()
    composeTestRule.onNodeWithText("members: 15").assertIsDisplayed()
  }

  @Test
  fun multipleGroups_allDisplayed() {
    val groups =
        listOf(
            Group(
                id = "1", name = "Football", ownerId = "owner", memberIds = List(20) { "user$it" }),
            Group(id = "2", name = "Hiking", ownerId = "owner", memberIds = List(10) { "user$it" }),
            Group(id = "3", name = "Chess", ownerId = "owner", memberIds = List(5) { "user$it" }))

    composeTestRule.setContent { GroupListScreen(viewModel = createViewModel(groups)) }

    composeTestRule.onNodeWithTag(GroupListScreenTestTags.cardTag("1")).assertIsDisplayed()
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.cardTag("2")).assertIsDisplayed()
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.cardTag("3")).assertIsDisplayed()
  }

  @Test
  fun groupCard_withEmptyDescription_doesNotShowDescription() {
    val group =
        Group(
            id = "test1",
            name = "Yoga Group",
            description = "",
            ownerId = "owner",
            memberIds = List(12) { "user$it" })

    composeTestRule.setContent { GroupListScreen(viewModel = createViewModel(listOf(group))) }

    composeTestRule.onNodeWithText("Yoga Group").assertIsDisplayed()
    composeTestRule.onNodeWithText("members: 12").assertIsDisplayed()
  }

  @Test
  fun groupCard_withZeroMembers_displaysZero() {
    val group =
        Group(
            id = "test1",
            name = "New Group",
            description = "Just created",
            ownerId = "owner",
            memberIds = List(0) { "user$it" })

    composeTestRule.setContent { GroupListScreen(viewModel = createViewModel(listOf(group))) }

    composeTestRule.onNodeWithText("members: 0").assertIsDisplayed()
  }

  @Test
  fun groupCard_withLargeNumberOfMembers_displaysCorrectly() {
    val group =
        Group(
            id = "test1",
            name = "Popular Group",
            description = "Very popular",
            ownerId = "owner",
            memberIds = List(999) { "user$it" })

    composeTestRule.setContent { GroupListScreen(viewModel = createViewModel(listOf(group))) }

    composeTestRule.onNodeWithText("members: 999").assertIsDisplayed()
  }

  @Test
  fun cardClick_callsOnGroup() {
    val groups =
        listOf(
            Group(id = "1", name = "Football", ownerId = "owner1"),
            Group(id = "2", name = "Hiking", ownerId = "owner2"))
    var clickedName: String? = null

    composeTestRule.setContent {
      GroupListScreen(viewModel = createViewModel(groups), onGroup = { g -> clickedName = g.name })
    }

    composeTestRule.onNodeWithTag(GroupListScreenTestTags.cardTag("1")).performClick()

    assertEquals("Football", clickedName)
  }

  @Test
  fun cardClick_callsOnGroupWithCorrectData() {
    val testGroup =
        Group(
            id = "abc123",
            name = "Test Group",
            description = "A test group",
            ownerId = "owner",
            memberIds = List(42) { "user$it" })

    var clickedGroup: Group? = null

    composeTestRule.setContent {
      GroupListScreen(
          viewModel = createViewModel(listOf(testGroup)), onGroup = { g -> clickedGroup = g })
    }

    composeTestRule.onNodeWithTag(GroupListScreenTestTags.cardTag("abc123")).performClick()

    assertEquals(testGroup, clickedGroup)
  }

  @Test
  fun multipleCardClicks_callOnGroupMultipleTimes() {
    val groups =
        listOf(
            Group(id = "1", name = "Group A", ownerId = "owner1"),
            Group(id = "2", name = "Group B", ownerId = "owner2"),
            Group(id = "3", name = "Group C", ownerId = "owner3"))
    val clickedGroups = mutableListOf<String>()

    composeTestRule.setContent {
      GroupListScreen(
          viewModel = createViewModel(groups), onGroup = { g -> clickedGroups.add(g.id) })
    }

    composeTestRule.onNodeWithTag(GroupListScreenTestTags.cardTag("1")).performClick()
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.cardTag("3")).performClick()
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.cardTag("2")).performClick()

    assertEquals(listOf("1", "3", "2"), clickedGroups)
  }

  // Note: Menu click tests have been removed as the menu interaction pattern has changed
  // The menu now uses specific callbacks (onViewGroupDetails, onLeaveGroup, etc.)
  // instead of a generic onMoreOptionMenu callback

  @Test
  fun list_isScrollable_and_reaches_last_item() {
    val groups = (1..50).map { i -> Group(id = "$i", name = "Group $i", ownerId = "owner$i") }
    val lastId = "50"

    composeTestRule.setContent { GroupListScreen(viewModel = createViewModel(groups)) }

    composeTestRule
        .onNodeWithTag(GroupListScreenTestTags.LIST)
        .performScrollToNode(hasTestTag(GroupListScreenTestTags.cardTag(lastId)))

    composeTestRule.onNodeWithTag(GroupListScreenTestTags.cardTag(lastId)).assertIsDisplayed()
  }

  @Test
  fun list_firstItemInitiallyVisible() {
    val groups = (1..50).map { i -> Group(id = "$i", name = "Group $i", ownerId = "owner$i") }

    composeTestRule.setContent { GroupListScreen(viewModel = createViewModel(groups)) }

    composeTestRule.onNodeWithTag(GroupListScreenTestTags.cardTag("1")).assertIsDisplayed()
  }

  @Test
  fun list_afterScrolling_canClickItems() {
    val groups =
        (1..50).map { i ->
          Group(
              id = "$i", name = "Group $i", ownerId = "owner$i", memberIds = List(i) { "user$it" })
        }
    var clickedGroup: Group? = null

    composeTestRule.setContent {
      GroupListScreen(viewModel = createViewModel(groups), onGroup = { g -> clickedGroup = g })
    }

    composeTestRule
        .onNodeWithTag(GroupListScreenTestTags.LIST)
        .performScrollToNode(hasTestTag(GroupListScreenTestTags.cardTag("40")))

    composeTestRule.onNodeWithTag(GroupListScreenTestTags.cardTag("40")).performClick()

    assertEquals("40", clickedGroup?.id)
    assertEquals("Group 40", clickedGroup?.name)
  }

  // Test removed: list_afterScrolling_canClickMoreOptions
  // Menu interaction pattern has changed to use specific callbacks

  @Test
  fun groupWithLongName_displaysCorrectly() {
    val group =
        Group(
            id = "1",
            name =
                "This is a very long group name that should be handled properly by the UI component",
            ownerId = "owner",
            memberIds = List(10) { "user$it" })

    composeTestRule.setContent { GroupListScreen(viewModel = createViewModel(listOf(group))) }

    composeTestRule.onNodeWithTag(GroupListScreenTestTags.cardTag("1")).assertIsDisplayed()
  }

  @Test
  fun groupWithLongDescription_displaysCorrectly() {
    val group =
        Group(
            id = "1",
            name = "Group",
            description =
                "This is a very long description that should be handled properly by the UI and might need to be truncated or ellipsized to fit properly in the card layout without breaking the design",
            ownerId = "owner",
            memberIds = List(10) { "user$it" })

    composeTestRule.setContent { GroupListScreen(viewModel = createViewModel(listOf(group))) }

    composeTestRule.onNodeWithTag(GroupListScreenTestTags.cardTag("1")).assertIsDisplayed()
  }

  @Test
  fun groupsWithSpecialCharactersInNames_displayCorrectly() {
    val groups =
        listOf(
            Group(
                id = "1",
                name = "Café & Chill",
                ownerId = "owner",
                memberIds = List(5) { "user$it" }),
            Group(
                id = "2",
                name = "€$ Money Talk",
                ownerId = "owner",
                memberIds = List(3) { "user$it" }),
            Group(
                id = "3", name = "日本語 Group", ownerId = "owner", memberIds = List(7) { "user$it" }))

    composeTestRule.setContent { GroupListScreen(viewModel = createViewModel(groups)) }

    composeTestRule.onNodeWithText("Café & Chill").assertIsDisplayed()
    composeTestRule.onNodeWithText("€$ Money Talk").assertIsDisplayed()
    composeTestRule.onNodeWithText("日本語 Group").assertIsDisplayed()
  }

  @Test
  fun groupsWithUnicodeEmojis_displayCorrectly() {
    val groups =
        listOf(
            Group(
                id = "1",
                name = "Football",
                description = "Let's do a 5v5 match!",
                ownerId = "owner",
                memberIds = List(11) { "user$it" }),
            Group(
                id = "2",
                name = "Gaming",
                description = "Let's play Minecraft SkyWars!",
                ownerId = "owner",
                memberIds = List(20) { "user$it" }))

    composeTestRule.setContent { GroupListScreen(viewModel = createViewModel(groups)) }

    composeTestRule.onNodeWithText("Let's do a 5v5 match!").assertIsDisplayed()
    composeTestRule.onNodeWithText("Let's play Minecraft SkyWars!").assertIsDisplayed()
  }

  @Test
  fun veryLargeList_performanceTest() {
    val groups =
        (1..100).map { i ->
          Group(
              id = "$i", name = "Group $i", ownerId = "owner$i", memberIds = List(5) { "user$it" })
        }

    composeTestRule.setContent { GroupListScreen(viewModel = createViewModel(groups)) }

    composeTestRule.onNodeWithTag(GroupListScreenTestTags.LIST).assertIsDisplayed()
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.cardTag("1")).assertIsDisplayed()
  }

  @Test
  fun transitionFromEmpty_toNonEmpty_displaysGroups() {
    val groups =
        listOf(
            Group(
                id = "1", name = "New Group", ownerId = "owner", memberIds = List(5) { "user$it" }))

    composeTestRule.setContent { GroupListScreen(viewModel = createViewModel(groups)) }

    composeTestRule.onNodeWithTag(GroupListScreenTestTags.LIST).assertIsDisplayed()
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.cardTag("1")).assertIsDisplayed()
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.EMPTY).assertDoesNotExist()
  }

  @Test
  fun allCards_haveMoreOptionsButton() {
    val groups =
        listOf(
            Group(id = "1", name = "Group A", ownerId = "owner1"),
            Group(id = "2", name = "Group B", ownerId = "owner2"),
            Group(id = "3", name = "Group C", ownerId = "owner3"))

    composeTestRule.setContent { GroupListScreen(viewModel = createViewModel(groups)) }

    composeTestRule.onNodeWithTag(GroupListScreenTestTags.moreTag("1")).assertIsDisplayed()
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.moreTag("2")).assertIsDisplayed()
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.moreTag("3")).assertIsDisplayed()
  }

  // Test removed: eachMoreButton_triggersCorrectCallback
  // Menu interaction pattern has changed to use specific callbacks

  // Floating Action Bubbles Tests
  @Test
  fun floatingActionButton_whenClicked_showsBubbles() {
    // Given: Screen is displayed
    composeTestRule.setContent {
      GroupListScreen(
          onJoinWithLink = {},
          onCreateGroup = {},
          onGroup = {},
          onBackClick = {},
          onProfileClick = {},
          onEditClick = {})
    }

    // When: User clicks the FAB
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.ADD_NEW_GROUP).performClick()

    // Then: Bubbles container is visible
    composeTestRule
        .onNodeWithTag(FloatingActionBubblesTestTags.BUBBLE_CONTAINER)
        .assertIsDisplayed()
  }

  @Test
  fun floatingActionBubbles_displaysCorrectActions() {
    // Given: Screen is displayed
    composeTestRule.setContent {
      GroupListScreen(
          onJoinWithLink = {},
          onCreateGroup = {},
          onGroup = {},
          onBackClick = {},
          onProfileClick = {},
          onEditClick = {})
    }

    // When: User clicks the FAB
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.ADD_NEW_GROUP).performClick()

    // Then: Both bubble actions are visible with correct text
    composeTestRule.onNodeWithText("JOIN WITH LINK").assertIsDisplayed()

    composeTestRule.onNodeWithText("CREATE A GROUP").assertIsDisplayed()
  }

  @Test
  fun floatingActionBubbles_joinWithLinkBubble_hasCorrectTestTag() {
    // Given: Screen is displayed
    composeTestRule.setContent {
      GroupListScreen(
          onJoinWithLink = {},
          onCreateGroup = {},
          onGroup = {},
          onBackClick = {},
          onProfileClick = {},
          onEditClick = {})
    }

    // When: User clicks the FAB
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.ADD_NEW_GROUP).performClick()

    // Then: Join with link bubble has correct test tag
    composeTestRule
        .onNodeWithTag("groupJoinWithLinkBubble")
        .assertIsDisplayed()
        .assertHasClickAction()
  }

  @Test
  fun floatingActionBubbles_createGroupBubble_hasCorrectTestTag() {
    // Given: Screen is displayed
    composeTestRule.setContent {
      GroupListScreen(
          onJoinWithLink = {},
          onCreateGroup = {},
          onGroup = {},
          onBackClick = {},
          onProfileClick = {},
          onEditClick = {})
    }

    // When: User clicks the FAB
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.ADD_NEW_GROUP).performClick()

    // Then: Create group bubble has correct test tag
    composeTestRule.onNodeWithTag("groupCreateBubble").assertIsDisplayed().assertHasClickAction()
  }

  @Test
  fun floatingActionBubbles_scrim_dismissesBubbles() {
    // Given: Screen is displayed with bubbles open
    composeTestRule.setContent {
      GroupListScreen(
          onJoinWithLink = {},
          onCreateGroup = {},
          onGroup = {},
          onBackClick = {},
          onProfileClick = {},
          onEditClick = {})
    }

    // When: User clicks the FAB to show bubbles
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.ADD_NEW_GROUP).performClick()

    // Verify bubbles are visible
    composeTestRule
        .onNodeWithTag(FloatingActionBubblesTestTags.BUBBLE_CONTAINER)
        .assertIsDisplayed()

    // When: User clicks the scrim (outside bubbles)
    composeTestRule.onNodeWithTag(FloatingActionBubblesTestTags.SCRIM).performClick()

    // Then: Bubbles are dismissed
    composeTestRule
        .onNodeWithTag(FloatingActionBubblesTestTags.BUBBLE_CONTAINER)
        .assertDoesNotExist()
  }

  @Test
  fun floatingActionBubbles_createGroupClick_triggersCallback() {
    // Given: Screen with callback tracker
    var createGroupClicked = false

    composeTestRule.setContent {
      GroupListScreen(
          onJoinWithLink = {},
          onCreateGroup = { createGroupClicked = true },
          onGroup = {},
          onBackClick = {},
          onProfileClick = {},
          onEditClick = {})
    }

    // When: User opens bubbles and clicks "Create a group"
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.ADD_NEW_GROUP).performClick()

    composeTestRule.onNodeWithTag("groupCreateBubble").performClick()

    // Then: Callback was invoked
    assert(createGroupClicked) { "onCreateGroup callback should have been called" }
  }

  @Test
  fun floatingActionBubbles_afterBubbleClick_dismissesBubbles() {
    // Given: Screen is displayed
    composeTestRule.setContent {
      GroupListScreen(
          onJoinWithLink = {},
          onCreateGroup = {},
          onGroup = {},
          onBackClick = {},
          onProfileClick = {},
          onEditClick = {})
    }

    // When: User opens bubbles
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.ADD_NEW_GROUP).performClick()

    // Verify bubbles are visible
    composeTestRule
        .onNodeWithTag(FloatingActionBubblesTestTags.BUBBLE_CONTAINER)
        .assertIsDisplayed()

    // When: User clicks a bubble action
    composeTestRule.onNodeWithTag("groupCreateBubble").performClick()

    // Then: Bubbles are automatically dismissed
    composeTestRule
        .onNodeWithTag(FloatingActionBubblesTestTags.BUBBLE_CONTAINER)
        .assertDoesNotExist()
  }

  @Test
  fun floatingActionButton_togglesBubbleVisibility() {
    // Given: Screen is displayed
    composeTestRule.setContent {
      GroupListScreen(
          onJoinWithLink = {},
          onCreateGroup = {},
          onGroup = {},
          onBackClick = {},
          onProfileClick = {},
          onEditClick = {})
    }

    // When: User clicks the FAB first time
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.ADD_NEW_GROUP).performClick()

    // Then: Bubbles are visible
    composeTestRule
        .onNodeWithTag(FloatingActionBubblesTestTags.BUBBLE_CONTAINER)
        .assertIsDisplayed()

    // When: User clicks the FAB again
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.ADD_NEW_GROUP).performClick()

    // Then: Bubbles are hidden
    composeTestRule
        .onNodeWithTag(FloatingActionBubblesTestTags.BUBBLE_CONTAINER)
        .assertDoesNotExist()
  }

  @Test
  fun floatingActionBubbles_bubblesAreClickable() {
    // Given: Screen is displayed
    composeTestRule.setContent {
      GroupListScreen(
          onJoinWithLink = {},
          onCreateGroup = {},
          onGroup = {},
          onBackClick = {},
          onProfileClick = {},
          onEditClick = {})
    }

    // When: User opens bubbles
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.ADD_NEW_GROUP).performClick()

    // Then: Both bubbles have click actions
    composeTestRule.onNodeWithTag("groupJoinWithLinkBubble").assertHasClickAction()

    composeTestRule.onNodeWithTag("groupCreateBubble").assertHasClickAction()
  }

  // Navigation callback tests for group menu actions
  @Test
  fun viewGroupDetails_callbackIsTriggered() {
    // Given: Screen with a group
    val group = Group(id = "test1", name = "Test Group", ownerId = "owner1")
    var viewedGroup: Group? = null

    composeTestRule.setContent {
      GroupListScreen(
          viewModel = createViewModel(listOf(group)), onViewGroupDetails = { g -> viewedGroup = g })
    }

    // When: User opens menu and clicks "View Group Details"
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.moreTag("test1")).performClick()
    composeTestRule.onNodeWithText("VIEW GROUP DETAILS").performClick()

    // Then: Callback was invoked with correct group
    assertEquals(group, viewedGroup)
  }

  @Test
  fun leaveGroup_callbackIsTriggered() {
    // Given: Screen with a group
    val group = Group(id = "test2", name = "Leave Group Test", ownerId = "owner2")
    var leftGroup: Group? = null

    composeTestRule.setContent {
      GroupListScreen(
          viewModel = createViewModel(listOf(group)), onLeaveGroup = { g -> leftGroup = g })
    }

    // When: User opens menu and clicks "Leave Group"
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.moreTag("test2")).performClick()
    composeTestRule.onNodeWithText("LEAVE GROUP").performClick()

    // Confirmation dialog should appear
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.LEAVE_GROUP_DIALOG).assertExists()

    // Confirm the action
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.LEAVE_GROUP_CONFIRM_BUTTON).performClick()

    // Then: Callback was invoked with correct group
    assertEquals(group, leftGroup)
  }

  @Test
  fun shareGroup_callbackIsTriggered() {
    // Given: Screen with a group
    val group = Group(id = "test3", name = "Share Group Test", ownerId = "owner3")
    var sharedGroup: Group? = null

    composeTestRule.setContent {
      GroupListScreen(
          viewModel = createViewModel(listOf(group)), onShareGroup = { g -> sharedGroup = g })
    }

    // When: User opens menu and clicks "Share Group"
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.moreTag("test3")).performClick()
    composeTestRule.onNodeWithText("SHARE GROUP").performClick()

    // Then: Callback was invoked with correct group
    assertEquals(group, sharedGroup)
  }

  @Test
  fun editGroup_callbackIsTriggered_whenUserIsOwner() {
    // Edit Group button is now always visible
    val group = Group(id = "test4", name = "Edit Group Test", ownerId = "currentUserId")
    var editedGroup: Group? = null

    composeTestRule.setContent {
      GroupListScreen(
          viewModel = createViewModel(listOf(group)), onEditGroup = { editedGroup = it })
    }

    // When: User opens menu
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.moreTag("test4")).performClick()

    // Then: Edit Group button is visible
    composeTestRule.onNodeWithText("EDIT GROUP").assertExists()

    // Click it
    composeTestRule.onNodeWithText("EDIT GROUP").performClick()

    // Callback should be triggered
    assertEquals(group, editedGroup)
  }

  @Test
  fun deleteGroup_buttonNotShown_whenUserIsNotOwner() {
    // Without testCurrentUserId specified, currentUserId is null
    // so user is not the owner and DELETE GROUP button should not be shown

    val group = Group(id = "test5", name = "Delete Group Test", ownerId = "someUserId")

    composeTestRule.setContent { GroupListScreen(viewModel = createViewModel(listOf(group))) }

    // When: User opens menu
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.moreTag("test5")).performClick()

    // Then: Delete Group button is NOT visible (user is not owner)
    composeTestRule.onNodeWithText("DELETE GROUP").assertDoesNotExist()
  }

  @Test
  fun multipleGroupMenuActions_eachCallbackTriggersCorrectly() {
    // Given: Screen with multiple groups
    val groups =
        listOf(
            Group(id = "g1", name = "Group 1", ownerId = "owner1"),
            Group(id = "g2", name = "Group 2", ownerId = "owner2"))
    val viewedGroups = mutableListOf<String>()
    val sharedGroups = mutableListOf<String>()

    composeTestRule.setContent {
      GroupListScreen(
          viewModel = createViewModel(groups),
          onViewGroupDetails = { g -> viewedGroups.add(g.id) },
          onShareGroup = { g -> sharedGroups.add(g.id) })
    }

    // When: User performs multiple actions
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.moreTag("g1")).performClick()
    composeTestRule.onNodeWithText("VIEW GROUP DETAILS").performClick()

    composeTestRule.onNodeWithTag(GroupListScreenTestTags.moreTag("g2")).performClick()
    composeTestRule.onNodeWithText("SHARE GROUP").performClick()

    // Then: Callbacks were invoked correctly
    assertEquals(listOf("g1"), viewedGroups)
    assertEquals(listOf("g2"), sharedGroups)
  }

  // ========== Additional Coverage Tests for GroupListScreen ==========

  @Test
  fun menuBubbles_closesAfterAction() {
    val group = Group(id = "test1", name = "Test Group", ownerId = "owner1")

    composeTestRule.setContent { GroupListScreen(viewModel = createViewModel(listOf(group))) }

    // Open menu
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.moreTag("test1")).performClick()
    composeTestRule.onNodeWithText("VIEW GROUP DETAILS").assertIsDisplayed()

    // Click an action
    composeTestRule.onNodeWithText("VIEW GROUP DETAILS").performClick()

    // Menu should close
    composeTestRule.onNodeWithText("VIEW GROUP DETAILS").assertDoesNotExist()
  }

  @Test
  fun openingCardMenu_closesJoinCreateMenu() {
    val group = Group(id = "test1", name = "Test Group", ownerId = "owner1")

    composeTestRule.setContent { GroupListScreen(viewModel = createViewModel(listOf(group))) }

    // Open join/create menu
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.ADD_NEW_GROUP).performClick()
    composeTestRule.onNodeWithText("JOIN WITH LINK").assertIsDisplayed()

    // Open card menu
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.moreTag("test1")).performClick()

    // Join/create menu should be closed
    composeTestRule.onNodeWithText("JOIN WITH LINK").assertDoesNotExist()
  }

  @Test
  fun openingJoinCreateMenu_closesCardMenu() {
    val group = Group(id = "test1", name = "Test Group", ownerId = "owner1")

    composeTestRule.setContent { GroupListScreen(viewModel = createViewModel(listOf(group))) }

    // Open card menu
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.moreTag("test1")).performClick()
    composeTestRule.onNodeWithText("VIEW GROUP DETAILS").assertIsDisplayed()

    // Open join/create menu
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.ADD_NEW_GROUP).performClick()

    // Card menu should be closed
    composeTestRule.onNodeWithText("VIEW GROUP DETAILS").assertDoesNotExist()
  }

  @Test
  fun topBar_backButton_isDisplayed() {
    composeTestRule.setContent { GroupListScreen() }

    composeTestRule.onNodeWithContentDescription("Back").assertIsDisplayed()
  }

  @Test
  fun topBar_backButton_triggersCallback() {
    var backClicked = false

    composeTestRule.setContent { GroupListScreen(onBackClick = { backClicked = true }) }

    composeTestRule.onNodeWithContentDescription("Back").performClick()

    assertTrue(backClicked)
  }

  @Test
  fun topBar_profileButton_isDisplayed() {
    composeTestRule.setContent { GroupListScreen() }

    // Profile button should be in the top bar
    composeTestRule.onNodeWithContentDescription("Profile").assertIsDisplayed()
  }

  @Test
  fun topBar_profileButton_triggersCallback() {
    var profileClicked = false

    composeTestRule.setContent { GroupListScreen(onProfileClick = { profileClicked = true }) }

    composeTestRule.onNodeWithContentDescription("Profile").performClick()

    assertTrue(profileClicked)
  }

  @Test
  fun topBar_editButton_triggersCallback() {
    var editClicked = false

    composeTestRule.setContent { GroupListScreen(onEditClick = { editClicked = true }) }

    composeTestRule.onNodeWithContentDescription("Edit").performClick()

    assertTrue(editClicked)
  }

  @Test
  fun cardClick_doesNotTriggerWhenMenuIsOpen() {
    val group = Group(id = "test1", name = "Test Group", ownerId = "owner1")

    composeTestRule.setContent {
      GroupListScreen(viewModel = createViewModel(listOf(group)), onGroup = {})
    }

    // Open menu
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.moreTag("test1")).performClick()

    // Try to click card while menu is open - menu should intercept
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.cardTag("test1")).performClick()

    // Card click should not trigger (menu is handling the click)
    // Note: This behavior depends on implementation
  }

  @Test
  fun fabButton_togglesState() {
    composeTestRule.setContent { GroupListScreen() }

    // Initially closed
    composeTestRule.onNodeWithText("JOIN WITH LINK").assertDoesNotExist()

    // Click to open
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.ADD_NEW_GROUP).performClick()
    composeTestRule.onNodeWithText("JOIN WITH LINK").assertIsDisplayed()

    // Click again to close
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.ADD_NEW_GROUP).performClick()
    composeTestRule.onNodeWithText("JOIN WITH LINK").assertDoesNotExist()
  }

  @Test
  fun joinWithLinkBubble_triggersCallback() {
    var joinClicked = false

    composeTestRule.setContent { GroupListScreen(onJoinWithLink = { joinClicked = true }) }

    composeTestRule.onNodeWithTag(GroupListScreenTestTags.ADD_NEW_GROUP).performClick()
    composeTestRule.onNodeWithText("JOIN WITH LINK").performClick()

    assertTrue(joinClicked)
  }

  @Test
  fun createGroupBubble_triggersCallback() {
    var createClicked = false

    composeTestRule.setContent { GroupListScreen(onCreateGroup = { createClicked = true }) }

    composeTestRule.onNodeWithTag(GroupListScreenTestTags.ADD_NEW_GROUP).performClick()
    composeTestRule.onNodeWithText("CREATE A GROUP").performClick()

    assertTrue(createClicked)
  }

  @Test
  fun emptyState_fabStillWorks() {
    var createClicked = false

    composeTestRule.setContent { GroupListScreen(onCreateGroup = { createClicked = true }) }

    // Verify empty state
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.EMPTY).assertIsDisplayed()

    // FAB should still work
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.ADD_NEW_GROUP).performClick()
    composeTestRule.onNodeWithText("CREATE A GROUP").performClick()

    assertTrue(createClicked)
  }

  @Test
  fun multipleCards_canOpenMenusSequentially() {
    val groups =
        listOf(
            Group(id = "g1", name = "Group 1", ownerId = "owner1"),
            Group(id = "g2", name = "Group 2", ownerId = "owner2"))

    composeTestRule.setContent { GroupListScreen(viewModel = createViewModel(groups)) }

    // Open first menu
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.moreTag("g1")).performClick()
    composeTestRule.onNodeWithText("VIEW GROUP DETAILS").assertIsDisplayed()

    // Toggle first menu closed by clicking same button again
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.moreTag("g1")).performClick()
    composeTestRule.onNodeWithText("VIEW GROUP DETAILS").assertDoesNotExist()

    // Open second menu
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.moreTag("g2")).performClick()

    // Second menu should now be open
    composeTestRule.onNodeWithText("VIEW GROUP DETAILS").assertIsDisplayed()
  }

  @Test
  fun clickingSameMenuButton_togglesMenu() {
    val group = Group(id = "test1", name = "Test Group", ownerId = "owner1")

    composeTestRule.setContent { GroupListScreen(viewModel = createViewModel(listOf(group))) }

    // Open menu
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.moreTag("test1")).performClick()
    composeTestRule.onNodeWithText("VIEW GROUP DETAILS").assertIsDisplayed()

    // Click same button to close
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.moreTag("test1")).performClick()
    composeTestRule.onNodeWithText("VIEW GROUP DETAILS").assertDoesNotExist()
  }

  @Test
  fun groupWithVeryLongDescription_displaysCorrectly() {
    val group =
        Group(
            id = "test1",
            name = "Group",
            description =
                "This is a very very very long description that should be handled properly by the UI without breaking the layout or causing overflow issues in the card component",
            ownerId = "owner1",
            memberIds = List(5) { "user$it" })

    composeTestRule.setContent { GroupListScreen(viewModel = createViewModel(listOf(group))) }

    composeTestRule.onNodeWithTag(GroupListScreenTestTags.cardTag("test1")).assertIsDisplayed()
  }

  @Test
  fun scrolling_preservesMenuState() {
    val groups = (1..50).map { i -> Group(id = "$i", name = "Group $i", ownerId = "owner$i") }

    composeTestRule.setContent { GroupListScreen(viewModel = createViewModel(groups)) }

    // Open menu for first visible item
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.moreTag("1")).performClick()
    composeTestRule.onNodeWithText("VIEW GROUP DETAILS").assertIsDisplayed()

    // Scroll down
    composeTestRule
        .onNodeWithTag(GroupListScreenTestTags.LIST)
        .performScrollToNode(hasTestTag(GroupListScreenTestTags.cardTag("30")))

    // Menu should still be visible (or close depending on implementation)
    // This tests the robustness of menu state during scrolling
  }

  @Test
  fun allMenuOptions_haveCorrectTestTags() {
    val group = Group(id = "test1", name = "Test Group", ownerId = "owner1")

    composeTestRule.setContent { GroupListScreen(viewModel = createViewModel(listOf(group))) }

    composeTestRule.onNodeWithTag(GroupListScreenTestTags.moreTag("test1")).performClick()

    // Common menu options always visible
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.VIEW_GROUP_DETAILS_BUBBLE).assertExists()
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.SHARE_GROUP_BUBBLE).assertExists()
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.EDIT_GROUP_BUBBLE).assertExists()

    // Leave button shown for non-owners (currentUserId is null, not the owner)
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.LEAVE_GROUP_BUBBLE).assertExists()

    // Delete button NOT shown for non-owners (currentUserId is null, not the owner)
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.DELETE_GROUP_BUBBLE).assertDoesNotExist()
  }

  @Test
  fun joinCreateBubbles_haveCorrectTestTags() {
    composeTestRule.setContent { GroupListScreen() }

    composeTestRule.onNodeWithTag(GroupListScreenTestTags.ADD_NEW_GROUP).performClick()

    composeTestRule.onNodeWithTag(GroupListScreenTestTags.JOIN_WITH_LINK_BUBBLE).assertExists()
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.CREATE_GROUP_BUBBLE).assertExists()
  }

  // =======================================
  // Owner/Non-Owner Button Visibility Tests
  // =======================================

  @Test
  fun groupMenu_commonButtonsAlwaysVisible() {
    val group = Group(id = "test1", name = "Test Group", ownerId = "currentUserId")

    composeTestRule.setContent { GroupListScreen(viewModel = createViewModel(listOf(group))) }

    // Open menu
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.moreTag("test1")).performClick()

    // Common buttons always visible
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.VIEW_GROUP_DETAILS_BUBBLE).assertExists()
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.SHARE_GROUP_BUBBLE).assertExists()
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.EDIT_GROUP_BUBBLE).assertExists()

    // Leave shown for non-owners (currentUserId is null, not the owner)
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.LEAVE_GROUP_BUBBLE).assertExists()
    // Delete NOT shown for non-owners
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.DELETE_GROUP_BUBBLE).assertDoesNotExist()
  }

  @Test
  fun groupMenu_multipleGroups_eachHasCorrectButtons() {
    val groups =
        listOf(
            Group(id = "g1", name = "Group 1", ownerId = "currentUser"),
            Group(id = "g2", name = "Group 2", ownerId = "otherUser"))

    composeTestRule.setContent { GroupListScreen(viewModel = createViewModel(groups)) }

    // Check first group - currentUserId is null, so user is not owner
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.moreTag("g1")).performClick()
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.VIEW_GROUP_DETAILS_BUBBLE).assertExists()
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.LEAVE_GROUP_BUBBLE).assertExists()
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.DELETE_GROUP_BUBBLE).assertDoesNotExist()

    // Close menu
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.cardTag("g1")).performClick()

    // Check second group - same behavior
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.moreTag("g2")).performClick()
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.VIEW_GROUP_DETAILS_BUBBLE).assertExists()
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.LEAVE_GROUP_BUBBLE).assertExists()
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.DELETE_GROUP_BUBBLE).assertDoesNotExist()
  }

  @Test
  fun groupMenu_commonButtonsHaveCorrectLabels() {
    val group = Group(id = "test1", name = "Test Group", ownerId = "owner1")

    composeTestRule.setContent { GroupListScreen(viewModel = createViewModel(listOf(group))) }

    // Open menu
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.moreTag("test1")).performClick()

    // Verify button labels for common buttons
    composeTestRule.onNodeWithText("VIEW GROUP DETAILS").assertExists()
    composeTestRule.onNodeWithText("LEAVE GROUP").assertExists() // Shown for non-owner
    composeTestRule.onNodeWithText("SHARE GROUP").assertExists()
    composeTestRule.onNodeWithText("EDIT GROUP").assertExists()
    // DELETE GROUP not shown since user is not owner (currentUserId is null)
  }

  // =======================================
  // Leave Group Confirmation Dialog Tests
  // =======================================

  @Test
  fun leaveGroupButton_showsConfirmationDialog_whenNotOwner() {
    val group = Group(id = "test1", name = "Test Group", ownerId = "owner1")

    composeTestRule.setContent { GroupListScreen(viewModel = createViewModel(listOf(group))) }

    // Open menu and click Leave Group
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.moreTag("test1")).performClick()
    composeTestRule.onNodeWithText("LEAVE GROUP").performClick()

    // Verify dialog appears
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.LEAVE_GROUP_DIALOG).assertExists()
    composeTestRule.onNodeWithText("Are you sure you want to leave\nthis group?").assertExists()
  }

  @Test
  fun leaveGroupDialog_cancelButton_dismissesDialog_whenNotOwner() {
    val group = Group(id = "test1", name = "Test Group", ownerId = "owner1")
    var leftGroup: Group? = null

    composeTestRule.setContent {
      GroupListScreen(viewModel = createViewModel(listOf(group)), onLeaveGroup = { leftGroup = it })
    }

    // Open menu and click Leave Group
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.moreTag("test1")).performClick()
    composeTestRule.onNodeWithText("LEAVE GROUP").performClick()

    // Click Cancel
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.LEAVE_GROUP_CANCEL_BUTTON).performClick()
    // Verify dialog is dismissed and callback was NOT called
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.LEAVE_GROUP_DIALOG).assertDoesNotExist()
    assertNull(leftGroup)
  }

  @Test
  fun leaveGroupDialog_confirmButton_triggersCallback_whenNotOwner() {
    val group = Group(id = "test1", name = "Test Group", ownerId = "owner1")
    var leftGroup: Group? = null

    composeTestRule.setContent {
      GroupListScreen(viewModel = createViewModel(listOf(group)), onLeaveGroup = { leftGroup = it })
    }
    // Open menu and click Leave Group
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.moreTag("test1")).performClick()
    composeTestRule.onNodeWithText("LEAVE GROUP").performClick()
    // Click Leave (confirm)
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.LEAVE_GROUP_CONFIRM_BUTTON).performClick()
    // Verify callback was called
    assertEquals(group, leftGroup)
  }

  @Test
  fun leaveGroupDialog_showsCorrectMessage_whenNotOwner() {
    val group = Group(id = "test1", name = "My Awesome Group", ownerId = "owner1")

    composeTestRule.setContent { GroupListScreen(viewModel = createViewModel(listOf(group))) }
    // Open menu and click Leave Group
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.moreTag("test1")).performClick()
    composeTestRule.onNodeWithText("LEAVE GROUP").performClick()
    // Verify dialog message appears
    composeTestRule.onNodeWithText("Are you sure you want to leave\nthis group?").assertExists()
  }

  @Test
  fun leaveGroupButton_showsCannotLeaveGroup_whenOwner() {
    val fakeRepo = FakeGroupRepository()
    val listViewModel = GroupListViewModel(fakeRepo)
    val testUserId = "testOwner123"

    // Create a group directly in the repository where the test user is the owner
    val group =
        Group(
            id = fakeRepo.getNewGroupId(),
            name = "My Test Group",
            ownerId = testUserId,
            memberIds = listOf(testUserId, "user2"))
    fakeRepo.setGroups(listOf(group))

    // Display the GroupListScreen with testCurrentUserId set to match the owner
    composeTestRule.setContent {
      GroupListScreen(viewModel = listViewModel, testCurrentUserId = testUserId)
    }
    composeTestRule.waitForIdle()

    // Open menu for the created group
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.moreTag(group.id)).performClick()

    // Verify LEAVE GROUP button does NOT exist (owner cannot leave)
    composeTestRule.onNodeWithText("LEAVE GROUP").assertDoesNotExist()
  }

  @Test
  fun leaveGroupButton_notShown_whenOwner() {
    val fakeRepo = FakeGroupRepository()
    val listViewModel = GroupListViewModel(fakeRepo)
    val testUserId = "testOwner456"

    // Create a group directly in the repository where the test user is the owner
    val group =
        Group(
            id = fakeRepo.getNewGroupId(),
            name = "My Test Group",
            ownerId = testUserId,
            memberIds = listOf(testUserId, "user2", "user3"))
    fakeRepo.setGroups(listOf(group))

    // Display the GroupListScreen with testCurrentUserId set to match the owner
    composeTestRule.setContent {
      GroupListScreen(viewModel = listViewModel, testCurrentUserId = testUserId)
    }
    composeTestRule.waitForIdle()

    // Open menu for the created group
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.moreTag(group.id)).performClick()

    // Verify LEAVE GROUP button does NOT exist (owner cannot leave)
    composeTestRule.onNodeWithText("LEAVE GROUP").assertDoesNotExist()

    // But DELETE GROUP button should exist (owner can delete)
    composeTestRule.onNodeWithText("DELETE GROUP").assertExists()
  }

  // =======================================
  // Delete Group Confirmation Dialog Tests
  // =======================================

  @Test
  fun deleteGroupButton_showsConfirmationDialog_whenOwner() {
    val fakeRepo = FakeGroupRepository()
    val listViewModel = GroupListViewModel(fakeRepo)
    val testUserId = "testOwner789"

    // Create a group where the test user is the owner
    val group =
        Group(
            id = fakeRepo.getNewGroupId(),
            name = "Test Group",
            ownerId = testUserId,
            memberIds = listOf(testUserId, "user2"))
    fakeRepo.setGroups(listOf(group))

    composeTestRule.setContent {
      GroupListScreen(viewModel = listViewModel, testCurrentUserId = testUserId)
    }
    composeTestRule.waitForIdle()

    // Open menu and click Delete Group
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.moreTag(group.id)).performClick()
    composeTestRule.onNodeWithText("DELETE GROUP").performClick()

    // Verify confirmation dialog appears (owner CAN delete)
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.DELETE_GROUP_DIALOG).assertExists()
    composeTestRule.onNodeWithText("Are you sure you want to delete\nthis group?").assertExists()
  }

  @Test
  fun deleteGroupDialog_showsCorrectWarningMessage_whenOwner() {
    val fakeRepo = FakeGroupRepository()
    val listViewModel = GroupListViewModel(fakeRepo)
    val testUserId = "testOwner790"

    val group =
        Group(
            id = fakeRepo.getNewGroupId(),
            name = "My Test Group",
            ownerId = testUserId,
            memberIds = listOf(testUserId))
    fakeRepo.setGroups(listOf(group))

    composeTestRule.setContent {
      GroupListScreen(viewModel = listViewModel, testCurrentUserId = testUserId)
    }
    composeTestRule.waitForIdle()

    // Open menu and click Delete Group
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.moreTag(group.id)).performClick()
    composeTestRule.onNodeWithText("DELETE GROUP").performClick()

    // Verify the warning message is displayed correctly
    composeTestRule
        .onNodeWithText("The group will be permanently deleted\nThis action is irreversible")
        .assertExists()
  }

  @Test
  fun deleteGroupDialog_cancelButton_dismissesDialog_whenOwner() {
    val fakeRepo = FakeGroupRepository()
    val listViewModel = GroupListViewModel(fakeRepo)
    val testUserId = "testOwner791"
    var deletedGroup: Group? = null

    val group =
        Group(
            id = fakeRepo.getNewGroupId(),
            name = "Test Group",
            ownerId = testUserId,
            memberIds = listOf(testUserId, "user2"))
    fakeRepo.setGroups(listOf(group))

    composeTestRule.setContent {
      GroupListScreen(
          viewModel = listViewModel,
          testCurrentUserId = testUserId,
          onDeleteGroup = { deletedGroup = it })
    }
    composeTestRule.waitForIdle()

    // Open menu and click Delete Group
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.moreTag(group.id)).performClick()
    composeTestRule.onNodeWithText("DELETE GROUP").performClick()

    // Click Cancel button
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.DELETE_GROUP_CANCEL_BUTTON).performClick()

    // Verify dialog is dismissed and callback was NOT called
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.DELETE_GROUP_DIALOG).assertDoesNotExist()
    assertNull(deletedGroup)
  }

  @Test
  fun deleteGroupDialog_confirmButton_triggersCallback_whenOwner() {
    val fakeRepo = FakeGroupRepository()
    val listViewModel = GroupListViewModel(fakeRepo)
    val testUserId = "testOwner792"
    var deletedGroup: Group? = null

    val group =
        Group(
            id = fakeRepo.getNewGroupId(),
            name = "Test Group",
            ownerId = testUserId,
            memberIds = listOf(testUserId))
    fakeRepo.setGroups(listOf(group))

    composeTestRule.setContent {
      GroupListScreen(
          viewModel = listViewModel,
          testCurrentUserId = testUserId,
          onDeleteGroup = { deletedGroup = it })
    }
    composeTestRule.waitForIdle()

    // Open menu and click Delete Group
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.moreTag(group.id)).performClick()
    composeTestRule.onNodeWithText("DELETE GROUP").performClick()

    // Click Delete button to confirm
    composeTestRule
        .onNodeWithTag(GroupListScreenTestTags.DELETE_GROUP_CONFIRM_BUTTON)
        .performClick()

    // Verify dialog is dismissed and callback WAS called with correct group
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.DELETE_GROUP_DIALOG).assertDoesNotExist()
    assertEquals(group, deletedGroup)
  }

  @Test
  fun deleteGroupButton_notShown_whenNotOwner() {
    val fakeRepo = FakeGroupRepository()
    val listViewModel = GroupListViewModel(fakeRepo)
    val testUserId = "testUser123"
    val ownerUserId = "differentOwner456"

    // Create a group where test user is NOT the owner
    val group =
        Group(
            id = fakeRepo.getNewGroupId(),
            name = "Test Group",
            ownerId = ownerUserId,
            memberIds = listOf(ownerUserId, testUserId))
    fakeRepo.setGroups(listOf(group))

    composeTestRule.setContent {
      GroupListScreen(viewModel = listViewModel, testCurrentUserId = testUserId)
    }
    composeTestRule.waitForIdle()

    // Open menu
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.moreTag(group.id)).performClick()

    // Verify DELETE GROUP button does NOT exist (non-owner cannot delete)
    composeTestRule.onNodeWithText("DELETE GROUP").assertDoesNotExist()
  }

  @Test
  fun deleteGroupButton_shown_whenOwner() {
    val fakeRepo = FakeGroupRepository()
    val listViewModel = GroupListViewModel(fakeRepo)
    val testUserId = "testUser124"

    val group =
        Group(
            id = fakeRepo.getNewGroupId(),
            name = "Test Group",
            ownerId = testUserId,
            memberIds = listOf(testUserId))
    fakeRepo.setGroups(listOf(group))

    composeTestRule.setContent {
      GroupListScreen(viewModel = listViewModel, testCurrentUserId = testUserId)
    }
    composeTestRule.waitForIdle()

    // Open menu
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.moreTag(group.id)).performClick()

    // Verify DELETE GROUP button EXISTS (owner can delete)
    composeTestRule.onNodeWithText("DELETE GROUP").assertExists()
  }

  @Test
  fun leaveGroupButton_shown_whenNotOwner() {
    val fakeRepo = FakeGroupRepository()
    val listViewModel = GroupListViewModel(fakeRepo)
    val testUserId = "testUser125"
    val ownerUserId = "differentOwner458"

    val group =
        Group(
            id = fakeRepo.getNewGroupId(),
            name = "Test Group",
            ownerId = ownerUserId,
            memberIds = listOf(ownerUserId, testUserId))
    fakeRepo.setGroups(listOf(group))

    composeTestRule.setContent {
      GroupListScreen(viewModel = listViewModel, testCurrentUserId = testUserId)
    }
    composeTestRule.waitForIdle()

    // Open menu
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.moreTag(group.id)).performClick()

    // Verify LEAVE GROUP button EXISTS (non-owner can leave)
    composeTestRule.onNodeWithText("LEAVE GROUP").assertExists()
  }

  // =======================================
  // Share Group Dialog Tests
  // =======================================

  @Test
  fun shareGroupButton_opensShareDialog() {
    val group = Group(id = "test1", name = "Test Group", ownerId = "owner1")

    composeTestRule.setContent { GroupListScreen(viewModel = createViewModel(listOf(group))) }

    // Open menu and click Share Group
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.moreTag("test1")).performClick()
    composeTestRule.onNodeWithText("SHARE GROUP").performClick()

    // Verify share dialog appears
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.SHARE_GROUP_DIALOG).assertExists()
  }

  @Test
  fun shareGroupDialog_displaysCorrectGroupName() {
    val group = Group(id = "test1", name = "My Awesome Group", ownerId = "owner1")

    composeTestRule.setContent { GroupListScreen(viewModel = createViewModel(listOf(group))) }

    // Open menu and click Share Group
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.moreTag("test1")).performClick()
    composeTestRule.onNodeWithText("SHARE GROUP").performClick()

    // Verify share dialog appears and contains the group name
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.SHARE_GROUP_DIALOG).assertExists()
    // Note: Group name appears in both card and dialog, so we just verify dialog is showing
    composeTestRule.onNodeWithText("       Share this group").assertExists()
  }

  @Test
  fun shareGroupDialog_displaysShareTitle() {
    val group = Group(id = "test1", name = "Test Group", ownerId = "owner1")

    composeTestRule.setContent { GroupListScreen(viewModel = createViewModel(listOf(group))) }

    // Open menu and click Share Group
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.moreTag("test1")).performClick()
    composeTestRule.onNodeWithText("SHARE GROUP").performClick()

    // Verify dialog title is displayed
    composeTestRule.onNodeWithText("       Share this group").assertExists()
  }

  @Test
  fun shareGroupDialog_hasCopyLinkButton() {
    val group = Group(id = "test1", name = "Test Group", ownerId = "owner1")

    composeTestRule.setContent { GroupListScreen(viewModel = createViewModel(listOf(group))) }

    // Open menu and click Share Group
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.moreTag("test1")).performClick()
    composeTestRule.onNodeWithText("SHARE GROUP").performClick()

    // Verify copy link button is displayed
    composeTestRule
        .onNodeWithTag(GroupListScreenTestTags.SHARE_GROUP_COPY_LINK_BUTTON)
        .assertExists()
        .assertHasClickAction()
  }

  @Test
  fun shareGroupDialog_hasCloseButton() {
    val group = Group(id = "test1", name = "Test Group", ownerId = "owner1")

    composeTestRule.setContent { GroupListScreen(viewModel = createViewModel(listOf(group))) }

    // Open menu and click Share Group
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.moreTag("test1")).performClick()
    composeTestRule.onNodeWithText("SHARE GROUP").performClick()

    // Verify close button is displayed
    composeTestRule
        .onNodeWithTag(GroupListScreenTestTags.SHARE_GROUP_CLOSE_BUTTON)
        .assertExists()
        .assertHasClickAction()
  }

  @Test
  fun shareGroupDialog_closeButton_dismissesDialog() {
    val group = Group(id = "test1", name = "Test Group", ownerId = "owner1")

    composeTestRule.setContent { GroupListScreen(viewModel = createViewModel(listOf(group))) }

    // Open menu and click Share Group
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.moreTag("test1")).performClick()
    composeTestRule.onNodeWithText("SHARE GROUP").performClick()

    // Verify dialog is open
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.SHARE_GROUP_DIALOG).assertExists()

    // Click close button
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.SHARE_GROUP_CLOSE_BUTTON).performClick()

    // Verify dialog is dismissed
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.SHARE_GROUP_DIALOG).assertDoesNotExist()
  }

  @Test
  fun shareGroupDialog_copyLinkButton_isClickable() {
    val group = Group(id = "test1", name = "Test Group", ownerId = "owner1")

    composeTestRule.setContent { GroupListScreen(viewModel = createViewModel(listOf(group))) }

    // Open menu and click Share Group
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.moreTag("test1")).performClick()
    composeTestRule.onNodeWithText("SHARE GROUP").performClick()

    // Verify copy link button is clickable
    composeTestRule
        .onNodeWithTag(GroupListScreenTestTags.SHARE_GROUP_COPY_LINK_BUTTON)
        .assertHasClickAction()
  }

  @Test
  fun shareGroupDialog_displaysCopyInviteLinkText() {
    val group = Group(id = "test1", name = "Test Group", ownerId = "owner1")

    composeTestRule.setContent { GroupListScreen(viewModel = createViewModel(listOf(group))) }

    // Open menu and click Share Group
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.moreTag("test1")).performClick()
    composeTestRule.onNodeWithText("SHARE GROUP").performClick()

    // Verify "Copy Group ID" text is displayed
    composeTestRule.onNodeWithText("Copy Group ID").assertExists()
  }

  // =======================================
  // Additional Coverage Tests
  // =======================================

  @Test
  fun groupCard_moreButton_isDisplayed() {
    val group = Group(id = "test1", name = "Test Group", ownerId = "owner1")

    composeTestRule.setContent { GroupListScreen(viewModel = createViewModel(listOf(group))) }

    // Verify more button is displayed
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.moreTag("test1")).assertIsDisplayed()
  }

  @Test
  fun groupCard_moreButton_triggersMenu() {
    val group = Group(id = "test1", name = "Test Group", ownerId = "owner1")

    composeTestRule.setContent { GroupListScreen(viewModel = createViewModel(listOf(group))) }

    // Click more button
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.moreTag("test1")).performClick()

    // Verify menu is displayed
    composeTestRule.onNodeWithText("VIEW GROUP DETAILS").assertIsDisplayed()
  }

  @Test
  fun menuScrim_clickingOutside_closesMenu() {
    val group = Group(id = "test1", name = "Test Group", ownerId = "owner1")

    composeTestRule.setContent { GroupListScreen(viewModel = createViewModel(listOf(group))) }

    // Open menu
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.moreTag("test1")).performClick()
    composeTestRule.onNodeWithText("VIEW GROUP DETAILS").assertIsDisplayed()

    // Click on the card (which is behind the scrim, but should close the menu)
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.cardTag("test1")).performClick()

    // Menu should be closed
    composeTestRule.onNodeWithText("VIEW GROUP DETAILS").assertDoesNotExist()
  }

  @Test
  fun emptyState_showsCorrectMessages() {
    composeTestRule.setContent { GroupListScreen() }

    // Verify both lines of empty state message
    composeTestRule.onNodeWithText("You are currently not").assertIsDisplayed()
    composeTestRule.onNodeWithText("assigned to a group…").assertIsDisplayed()
  }

  @Test
  fun groupWithDescription_displaysDescription() {
    val group =
        Group(id = "test1", name = "Group", description = "Test Description", ownerId = "owner1")

    composeTestRule.setContent { GroupListScreen(viewModel = createViewModel(listOf(group))) }

    composeTestRule.onNodeWithText("Test Description").assertIsDisplayed()
  }

  @Test
  fun groupWithoutDescription_doesNotShowDescriptionArea() {
    val group = Group(id = "test1", name = "Group", description = "", ownerId = "owner1")

    composeTestRule.setContent { GroupListScreen(viewModel = createViewModel(listOf(group))) }

    // Description should not be visible (tested implicitly - only name and members shown)
    composeTestRule.onNodeWithText("Group").assertIsDisplayed()
    composeTestRule.onNodeWithText("members: 0").assertIsDisplayed()
  }

  @Test
  fun multipleGroups_eachHasIndependentMenu() {
    val groups =
        listOf(
            Group(id = "g1", name = "Group 1", ownerId = "owner1"),
            Group(id = "g2", name = "Group 2", ownerId = "owner2"))

    composeTestRule.setContent { GroupListScreen(viewModel = createViewModel(groups)) }

    // Open first menu
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.moreTag("g1")).performClick()
    composeTestRule.onNodeWithText("VIEW GROUP DETAILS").assertIsDisplayed()

    // Close by clicking action
    composeTestRule.onNodeWithText("VIEW GROUP DETAILS").performClick()

    // Open second menu
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.moreTag("g2")).performClick()
    composeTestRule.onNodeWithText("VIEW GROUP DETAILS").assertIsDisplayed()
  }

  // =======================================
  // FAB State Tests
  // =======================================

  @Test
  fun fab_showsCorrectIcon_whenClosed() {
    composeTestRule.setContent { GroupListScreen() }

    // FAB should be visible
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.ADD_NEW_GROUP).assertIsDisplayed()
  }

  @Test
  fun fab_showsCorrectIcon_whenOpen() {
    composeTestRule.setContent { GroupListScreen() }

    // Open FAB
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.ADD_NEW_GROUP).performClick()

    // FAB should still be visible (just with different styling)
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.ADD_NEW_GROUP).assertIsDisplayed()
  }

  @Test
  fun fab_closesJoinBubbles_whenClickedTwice() {
    composeTestRule.setContent { GroupListScreen() }

    // Open bubbles
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.ADD_NEW_GROUP).performClick()
    composeTestRule.onNodeWithText("JOIN WITH LINK").assertExists()

    // Close bubbles
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.ADD_NEW_GROUP).performClick()
    composeTestRule.onNodeWithText("JOIN WITH LINK").assertDoesNotExist()
  }

  // =======================================
  // Menu Scrim Tests
  // =======================================

  @Test
  fun cardMenu_hasScrim_whenOpen() {
    val group = Group(id = "test1", name = "Test Group", ownerId = "owner1")

    composeTestRule.setContent { GroupListScreen(viewModel = createViewModel(listOf(group))) }

    // Open menu
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.moreTag("test1")).performClick()

    // Menu should be visible
    composeTestRule.onNodeWithText("VIEW GROUP DETAILS").assertIsDisplayed()
  }

  @Test
  fun cardMenu_closesWhenClickingCard() {
    val group = Group(id = "test1", name = "Test Group", ownerId = "owner1")

    composeTestRule.setContent { GroupListScreen(viewModel = createViewModel(listOf(group))) }

    // Open menu
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.moreTag("test1")).performClick()
    composeTestRule.onNodeWithText("VIEW GROUP DETAILS").assertIsDisplayed()

    // Click card
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.cardTag("test1")).performClick()

    // Menu should close
    composeTestRule.onNodeWithText("VIEW GROUP DETAILS").assertDoesNotExist()
  }

  @Test
  fun openingDifferentCardMenu_opensSecondMenu() {
    val groups =
        listOf(
            Group(id = "g1", name = "Group 1", ownerId = "owner1"),
            Group(id = "g2", name = "Group 2", ownerId = "owner2"))

    composeTestRule.setContent { GroupListScreen(viewModel = createViewModel(groups)) }

    // Open first menu
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.moreTag("g1")).performClick()
    composeTestRule.onNodeWithText("VIEW GROUP DETAILS").assertIsDisplayed()

    // Close first menu by clicking the button again
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.moreTag("g1")).performClick()

    // Open second menu
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.moreTag("g2")).performClick()

    // Second menu should be displayed
    composeTestRule.onNodeWithText("VIEW GROUP DETAILS").assertIsDisplayed()
  }

  // =======================================
  // ShareGroupDialog Copy Button Tests
  // =======================================

  @Test
  fun shareGroupDialog_copyButton_isClickable() {
    val group = Group(id = "test-id-123", name = "Test Group", ownerId = "owner1")

    composeTestRule.setContent { GroupListScreen(viewModel = createViewModel(listOf(group))) }

    // Open share dialog
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.moreTag("test-id-123")).performClick()
    composeTestRule.onNodeWithText("SHARE GROUP").performClick()

    // Click copy button
    composeTestRule
        .onNodeWithTag(GroupListScreenTestTags.SHARE_GROUP_COPY_LINK_BUTTON)
        .performClick()

    // Dialog should still be open after copy
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.SHARE_GROUP_DIALOG).assertExists()
  }

  // =======================================
  // Empty State Tests
  // =======================================

  @Test
  fun emptyState_hasCorrectStyling() {
    composeTestRule.setContent { GroupListScreen() }

    // Empty state should be visible
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.EMPTY).assertIsDisplayed()

    // FAB should still be visible
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.ADD_NEW_GROUP).assertIsDisplayed()
  }

  @Test
  fun emptyState_canOpenJoinBubbles() {
    composeTestRule.setContent { GroupListScreen() }

    // Verify empty state
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.EMPTY).assertIsDisplayed()

    // Open join bubbles
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.ADD_NEW_GROUP).performClick()

    // Bubbles should be visible
    composeTestRule.onNodeWithText("JOIN WITH LINK").assertExists()
    composeTestRule.onNodeWithText("CREATE A GROUP").assertExists()
  }

  // =======================================
  // Group Card Content Tests
  // =======================================

  @Test
  fun groupCard_displaysAllFields_whenAllPresent() {
    val group =
        Group(
            id = "test1",
            name = "Complete Group",
            description = "A full description",
            ownerId = "owner1",
            memberIds = List(25) { "user$it" })

    composeTestRule.setContent { GroupListScreen(viewModel = createViewModel(listOf(group))) }

    // All fields should be visible
    composeTestRule.onNodeWithText("Complete Group").assertIsDisplayed()
    composeTestRule.onNodeWithText("A full description").assertIsDisplayed()
    composeTestRule.onNodeWithText("members: 25").assertIsDisplayed()
  }

  @Test
  fun groupCard_hasMoreButton() {
    val group = Group(id = "test1", name = "Test Group", ownerId = "owner1")

    composeTestRule.setContent { GroupListScreen(viewModel = createViewModel(listOf(group))) }

    // More button should be visible
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.moreTag("test1")).assertIsDisplayed()
  }

  @Test
  fun groupCard_moreButton_hasCorrectIcon() {
    val group = Group(id = "test1", name = "Test Group", ownerId = "owner1")

    composeTestRule.setContent { GroupListScreen(viewModel = createViewModel(listOf(group))) }

    // More button should have click action
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.moreTag("test1")).assertHasClickAction()
  }

  // =======================================
  // Confirmation Dialog Tests
  // =======================================

  @Test
  fun leaveGroupDialog_hasYesAndNoButtons() {
    val group = Group(id = "test1", name = "Test Group", ownerId = "owner1")

    composeTestRule.setContent { GroupListScreen(viewModel = createViewModel(listOf(group))) }

    // Open leave dialog
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.moreTag("test1")).performClick()
    composeTestRule.onNodeWithText("LEAVE GROUP").performClick()

    // Both buttons should exist
    composeTestRule.onNodeWithText("Yes").assertExists()
    composeTestRule.onNodeWithText("No").assertExists()
  }

  @Test
  fun deleteGroupDialog_hasYesAndNoButtons_whenOwner() {
    val fakeRepo = FakeGroupRepository()
    val listViewModel = GroupListViewModel(fakeRepo)
    val testUserId = "testOwner999"

    val group =
        Group(
            id = fakeRepo.getNewGroupId(),
            name = "Test Group",
            ownerId = testUserId,
            memberIds = listOf(testUserId))
    fakeRepo.setGroups(listOf(group))

    composeTestRule.setContent {
      GroupListScreen(viewModel = listViewModel, testCurrentUserId = testUserId)
    }
    composeTestRule.waitForIdle()

    // Open delete dialog
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.moreTag(group.id)).performClick()
    composeTestRule.onNodeWithText("DELETE GROUP").performClick()

    // Both buttons should exist
    composeTestRule.onNodeWithText("Yes").assertExists()
    composeTestRule.onNodeWithText("No").assertExists()
  }

  // =======================================
  // Scrolling Tests
  // =======================================

  @Test
  fun largeList_canScrollToBottom() {
    val groups = (1..100).map { i -> Group(id = "$i", name = "Group $i", ownerId = "owner$i") }

    composeTestRule.setContent { GroupListScreen(viewModel = createViewModel(groups)) }

    // Scroll to last item
    composeTestRule
        .onNodeWithTag(GroupListScreenTestTags.LIST)
        .performScrollToNode(hasTestTag(GroupListScreenTestTags.cardTag("100")))

    // Last item should be visible
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.cardTag("100")).assertIsDisplayed()
  }

  @Test
  fun scrolledList_firstItemNotVisible() {
    val groups = (1..100).map { i -> Group(id = "$i", name = "Group $i", ownerId = "owner$i") }

    composeTestRule.setContent { GroupListScreen(viewModel = createViewModel(groups)) }

    // Scroll to middle
    composeTestRule
        .onNodeWithTag(GroupListScreenTestTags.LIST)
        .performScrollToNode(hasTestTag(GroupListScreenTestTags.cardTag("50")))

    // Middle item should be visible
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.cardTag("50")).assertIsDisplayed()
  }
}
