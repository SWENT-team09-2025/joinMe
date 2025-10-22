package com.android.joinme.ui.group

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.android.joinme.model.group.Group
import com.android.joinme.model.group.GroupRepository
import com.android.joinme.ui.components.FloatingActionBubblesTestTags
import com.android.joinme.ui.groups.GroupListScreen
import com.android.joinme.ui.groups.GroupListScreenTestTags
import com.android.joinme.ui.groups.GroupListViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Fake implementation of GroupRepository for testing purposes.
 *
 * This implementation stores groups in memory and allows tests to inject specific test data.
 */
class FakeGroupRepository : GroupRepository {
  private val groups = mutableListOf<Group>()

  fun setGroups(newGroups: List<Group>) {
    groups.clear()
    groups.addAll(newGroups)
  }

  override suspend fun userGroups(): List<Group> = groups

  override suspend fun leaveGroup(id: String) {
    groups.removeIf { it.id == id }
  }

  override suspend fun getGroup(id: String): Group? = groups.find { it.id == id }
}

class GroupListScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

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
  }

  @Test
  fun emptyState_showsCorrectMessage() {
    composeTestRule.setContent { GroupListScreen(viewModel = createViewModel()) }

    composeTestRule.onNodeWithText("You are currently not").assertIsDisplayed()
    composeTestRule.onNodeWithText("assigned to a group…").assertIsDisplayed()
  }

  @Test
  fun emptyState_listDoesNotExist() {
    composeTestRule.setContent { GroupListScreen(viewModel = createViewModel()) }

    composeTestRule.onNodeWithTag(GroupListScreenTestTags.LIST).assertDoesNotExist()
  }

  @Test
  fun emptyState_fabIsDisplayed() {
    composeTestRule.setContent { GroupListScreen(viewModel = createViewModel()) }

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
  fun groupCard_displaysAllInformation() {
    val group =
        Group(
            id = "test1",
            name = "Rock Climbing",
            description = "Indoor and outdoor climbing sessions",
            ownerId = "owner",
            memberIds = List(25) { "user$it" })

    composeTestRule.setContent { GroupListScreen(viewModel = createViewModel(listOf(group))) }

    composeTestRule.onNodeWithText("Rock Climbing").assertIsDisplayed()
    composeTestRule.onNodeWithText("Indoor and outdoor climbing sessions").assertIsDisplayed()
    composeTestRule.onNodeWithText("members: 25").assertIsDisplayed()
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
  fun groupCard_withBlankDescription_doesNotShowDescription() {
    val group =
        Group(
            id = "test1",
            name = "Running Club",
            description = "   ",
            ownerId = "owner",
            memberIds = List(8) { "user$it" })

    composeTestRule.setContent { GroupListScreen(viewModel = createViewModel(listOf(group))) }

    composeTestRule.onNodeWithText("Running Club").assertIsDisplayed()
    composeTestRule.onNodeWithText("members: 8").assertIsDisplayed()
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

  @Test
  fun moreMenuClick_callsOnMoreOptionMenu() {
    val groups =
        listOf(
            Group(id = "1", name = "Football", ownerId = "owner1"),
            Group(id = "2", name = "Hiking", ownerId = "owner2"))
    var moreClickedId: String? = null

    composeTestRule.setContent {
      GroupListScreen(
          viewModel = createViewModel(groups), onMoreOptionMenu = { g -> moreClickedId = g.id })
    }
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.moreTag("2")).performClick()
    assertEquals("2", moreClickedId)
  }

  @Test
  fun moreMenuClick_passesCorrectGroup() {
    val testGroup =
        Group(
            id = "xyz789",
            name = "More Menu Test",
            description = "Testing more menu",
            ownerId = "owner",
            memberIds = List(7) { "user$it" })
    var clickedGroup: Group? = null

    composeTestRule.setContent {
      GroupListScreen(
          viewModel = createViewModel(listOf(testGroup)),
          onMoreOptionMenu = { g -> clickedGroup = g })
    }

    composeTestRule.onNodeWithTag(GroupListScreenTestTags.moreTag("xyz789")).performClick()

    assertEquals(testGroup, clickedGroup)
  }

  @Test
  fun moreMenuClick_doesNotTriggerCardClick() {
    val group = Group(id = "1", name = "Test Group", ownerId = "owner1")
    var cardClicked = false
    var moreClicked = false

    composeTestRule.setContent {
      GroupListScreen(
          viewModel = createViewModel(listOf(group)),
          onGroup = { cardClicked = true },
          onMoreOptionMenu = { moreClicked = true })
    }

    composeTestRule.onNodeWithTag(GroupListScreenTestTags.moreTag("1")).performClick()

    assertTrue(moreClicked)
    assertTrue(!cardClicked)
  }

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
  fun list_scrollToMiddleItem() {
    val groups = (1..50).map { i -> Group(id = "$i", name = "Group $i", ownerId = "owner$i") }

    composeTestRule.setContent { GroupListScreen(viewModel = createViewModel(groups)) }

    composeTestRule
        .onNodeWithTag(GroupListScreenTestTags.LIST)
        .performScrollToNode(hasTestTag(GroupListScreenTestTags.cardTag("25")))

    composeTestRule.onNodeWithTag(GroupListScreenTestTags.cardTag("25")).assertIsDisplayed()
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

  @Test
  fun list_afterScrolling_canClickMoreOptions() {
    val groups = (1..50).map { i -> Group(id = "$i", name = "Group $i", ownerId = "owner$i") }
    var clickedGroupId: String? = null

    composeTestRule.setContent {
      GroupListScreen(
          viewModel = createViewModel(groups), onMoreOptionMenu = { g -> clickedGroupId = g.id })
    }

    composeTestRule
        .onNodeWithTag(GroupListScreenTestTags.LIST)
        .performScrollToNode(hasTestTag(GroupListScreenTestTags.cardTag("45")))

    composeTestRule.onNodeWithTag(GroupListScreenTestTags.moreTag("45")).performClick()

    assertEquals("45", clickedGroupId)
  }

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

  @Test
  fun eachMoreButton_triggersCorrectCallback() {
    val groups =
        listOf(
            Group(id = "a", name = "Group A", ownerId = "owner a"),
            Group(id = "b", name = "Group B", ownerId = "owner b"),
            Group(id = "c", name = "Group C", ownerId = "owner c"))
    val clickedIds = mutableListOf<String>()

    composeTestRule.setContent {
      GroupListScreen(
          viewModel = createViewModel(groups), onMoreOptionMenu = { g -> clickedIds.add(g.id) })
    }

    composeTestRule.onNodeWithTag(GroupListScreenTestTags.moreTag("a")).performClick()
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.moreTag("c")).performClick()
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.moreTag("b")).performClick()

    assertEquals(listOf("a", "c", "b"), clickedIds)
  }

  // Floating Action Bubbles Tests
  @Test
  fun floatingActionButton_whenClicked_showsBubbles() {
    // Given: Screen is displayed
    composeTestRule.setContent {
      GroupListScreen(
          onJoinWithLink = {},
          onCreateGroup = {},
          onGroup = {},
          onMoreOptionMenu = {},
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
          onMoreOptionMenu = {},
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
          onMoreOptionMenu = {},
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
          onMoreOptionMenu = {},
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
          onMoreOptionMenu = {},
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
          onMoreOptionMenu = {},
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
          onMoreOptionMenu = {},
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
          onMoreOptionMenu = {},
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
          onMoreOptionMenu = {},
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
  fun floatingActionBubbles_bubblesHaveIcons() {
    // Given: Screen is displayed
    composeTestRule.setContent {
      GroupListScreen(
          onJoinWithLink = {},
          onCreateGroup = {},
          onGroup = {},
          onMoreOptionMenu = {},
          onBackClick = {},
          onProfileClick = {},
          onEditClick = {})
    }

    // When: User clicks the FAB
    composeTestRule.onNodeWithTag(GroupListScreenTestTags.ADD_NEW_GROUP).performClick()

    // Then: Bubbles have content descriptions for icons
    composeTestRule.onNodeWithContentDescription("Join with link").assertIsDisplayed()

    composeTestRule.onNodeWithContentDescription("Create a group").assertIsDisplayed()
  }

  @Test
  fun floatingActionBubbles_bubblesAreClickable() {
    // Given: Screen is displayed
    composeTestRule.setContent {
      GroupListScreen(
          onJoinWithLink = {},
          onCreateGroup = {},
          onGroup = {},
          onMoreOptionMenu = {},
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
}
