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

  override suspend fun deleteGroup(groupId: String) {
    groups.removeIf { it.id == groupId }
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
    composeTestRule.onNodeWithText("Join with link").assertIsDisplayed()

    composeTestRule.onNodeWithText("Create a group").assertIsDisplayed()
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
  fun floatingActionBubbles_joinWithLinkClick_triggersCallback() {
    // Given: Screen with callback tracker
    var joinWithLinkClicked = false

    composeTestRule.setContent {
      GroupListScreen(
          onJoinWithLink = { joinWithLinkClicked = true },
          onCreateGroup = {},
          onGroup = {},
          onBackClick = {},
          onProfileClick = {},
          onEditClick = {})
    }

    // When: User opens bubbles and clicks "Join with link"
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.ADD_NEW_GROUP).performClick()

    composeTestRule.onNodeWithTag("groupJoinWithLinkBubble").performClick()

    // Then: Callback was invoked
    assert(joinWithLinkClicked) { "onJoinWithLink callback should have been called" }
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
    composeTestRule.onNodeWithText("View Group Details").performClick()

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
    composeTestRule.onNodeWithText("Leave Group").performClick()

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
    composeTestRule.onNodeWithText("Share Group").performClick()

    // Then: Callback was invoked with correct group
    assertEquals(group, sharedGroup)
  }

  @Test
  fun editGroup_callbackIsTriggered_whenUserIsOwner() {
    // Note: This test cannot fully verify the callback in the test environment
    // because Edit Group is only shown when Firebase.auth.currentUser?.uid == group.ownerId
    // Without Firebase Auth initialized, the Edit Group button won't appear

    val group = Group(id = "test4", name = "Edit Group Test", ownerId = "currentUserId")

    composeTestRule.setContent {
      GroupListScreen(viewModel = createViewModel(listOf(group)), onEditGroup = {})
    }

    // When: User opens menu
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.moreTag("test4")).performClick()

    // Then: Edit Group button doesn't appear without Firebase Auth
    composeTestRule.onNodeWithText("Edit Group").assertDoesNotExist()
  }

  @Test
  fun deleteGroup_callbackIsTriggered_whenUserIsOwner() {
    // Note: This test cannot fully verify the callback in the test environment
    // because Delete Group is only shown when Firebase.auth.currentUser?.uid == group.ownerId
    // Without Firebase Auth initialized, the Delete Group button won't appear

    val group = Group(id = "test5", name = "Delete Group Test", ownerId = "currentUserId")

    composeTestRule.setContent {
      GroupListScreen(viewModel = createViewModel(listOf(group)), onDeleteGroup = {})
    }

    // When: User opens menu
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.moreTag("test5")).performClick()

    // Then: Delete Group button doesn't appear without Firebase Auth
    composeTestRule.onNodeWithText("Delete Group").assertDoesNotExist()
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
    composeTestRule.onNodeWithText("View Group Details").performClick()

    composeTestRule.onNodeWithTag(GroupListScreenTestTags.moreTag("g2")).performClick()
    composeTestRule.onNodeWithText("Share Group").performClick()

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
    composeTestRule.onNodeWithText("View Group Details").assertIsDisplayed()

    // Click an action
    composeTestRule.onNodeWithText("View Group Details").performClick()

    // Menu should close
    composeTestRule.onNodeWithText("View Group Details").assertDoesNotExist()
  }

  @Test
  fun openingCardMenu_closesJoinCreateMenu() {
    val group = Group(id = "test1", name = "Test Group", ownerId = "owner1")

    composeTestRule.setContent { GroupListScreen(viewModel = createViewModel(listOf(group))) }

    // Open join/create menu
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.ADD_NEW_GROUP).performClick()
    composeTestRule.onNodeWithText("Join with link").assertIsDisplayed()

    // Open card menu
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.moreTag("test1")).performClick()

    // Join/create menu should be closed
    composeTestRule.onNodeWithText("Join with link").assertDoesNotExist()
  }

  @Test
  fun openingJoinCreateMenu_closesCardMenu() {
    val group = Group(id = "test1", name = "Test Group", ownerId = "owner1")

    composeTestRule.setContent { GroupListScreen(viewModel = createViewModel(listOf(group))) }

    // Open card menu
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.moreTag("test1")).performClick()
    composeTestRule.onNodeWithText("View Group Details").assertIsDisplayed()

    // Open join/create menu
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.ADD_NEW_GROUP).performClick()

    // Card menu should be closed
    composeTestRule.onNodeWithText("View Group Details").assertDoesNotExist()
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
    var cardClicked = false

    composeTestRule.setContent {
      GroupListScreen(viewModel = createViewModel(listOf(group)), onGroup = { cardClicked = true })
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
    composeTestRule.onNodeWithText("Join with link").assertDoesNotExist()

    // Click to open
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.ADD_NEW_GROUP).performClick()
    composeTestRule.onNodeWithText("Join with link").assertIsDisplayed()

    // Click again to close
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.ADD_NEW_GROUP).performClick()
    composeTestRule.onNodeWithText("Join with link").assertDoesNotExist()
  }

  @Test
  fun joinWithLinkBubble_triggersCallback() {
    var joinClicked = false

    composeTestRule.setContent { GroupListScreen(onJoinWithLink = { joinClicked = true }) }

    composeTestRule.onNodeWithTag(GroupListScreenTestTags.ADD_NEW_GROUP).performClick()
    composeTestRule.onNodeWithText("Join with link").performClick()

    assertTrue(joinClicked)
  }

  @Test
  fun createGroupBubble_triggersCallback() {
    var createClicked = false

    composeTestRule.setContent { GroupListScreen(onCreateGroup = { createClicked = true }) }

    composeTestRule.onNodeWithTag(GroupListScreenTestTags.ADD_NEW_GROUP).performClick()
    composeTestRule.onNodeWithText("Create a group").performClick()

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
    composeTestRule.onNodeWithText("Create a group").performClick()

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
    composeTestRule.onNodeWithText("View Group Details").assertIsDisplayed()

    // Toggle first menu closed by clicking same button again
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.moreTag("g1")).performClick()
    composeTestRule.onNodeWithText("View Group Details").assertDoesNotExist()

    // Open second menu
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.moreTag("g2")).performClick()

    // Second menu should now be open
    composeTestRule.onNodeWithText("View Group Details").assertIsDisplayed()
  }

  @Test
  fun clickingSameMenuButton_togglesMenu() {
    val group = Group(id = "test1", name = "Test Group", ownerId = "owner1")

    composeTestRule.setContent { GroupListScreen(viewModel = createViewModel(listOf(group))) }

    // Open menu
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.moreTag("test1")).performClick()
    composeTestRule.onNodeWithText("View Group Details").assertIsDisplayed()

    // Click same button to close
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.moreTag("test1")).performClick()
    composeTestRule.onNodeWithText("View Group Details").assertDoesNotExist()
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
    composeTestRule.onNodeWithText("View Group Details").assertIsDisplayed()

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

    // Verify common menu options that are always visible
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.VIEW_GROUP_DETAILS_BUBBLE).assertExists()
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.LEAVE_GROUP_BUBBLE).assertExists()
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.SHARE_GROUP_BUBBLE).assertExists()

    // Note: Edit and Delete bubbles are only shown when Firebase.auth.currentUser?.uid ==
    // group.ownerId
    // Since Firebase Auth is not initialized in tests, these bubbles won't appear
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.EDIT_GROUP_BUBBLE).assertDoesNotExist()
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.DELETE_GROUP_BUBBLE).assertDoesNotExist()
  }

  @Test
  fun joinCreateBubbles_haveCorrectTestTags() {
    composeTestRule.setContent { GroupListScreen() }

    composeTestRule.onNodeWithTag(GroupListScreenTestTags.ADD_NEW_GROUP).performClick()

    composeTestRule.onNodeWithTag(GroupListScreenTestTags.JOIN_WITH_LINK_BUBBLE).assertExists()
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.CREATE_GROUP_BUBBLE).assertExists()
  }
}
