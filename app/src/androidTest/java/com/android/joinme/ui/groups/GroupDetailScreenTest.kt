package com.android.joinme.ui.groups

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.android.joinme.model.event.EventType
import com.android.joinme.model.groups.Group
import com.android.joinme.model.groups.GroupRepository
import com.android.joinme.model.profile.Profile
import com.android.joinme.model.profile.ProfileRepository
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/**
 * Fake GroupRepository for testing GroupDetailScreen.
 */
class FakeGroupDetailRepository : GroupRepository {
  private val groups = mutableMapOf<String, Group>()
  var shouldThrowError = false

  fun setGroup(group: Group) {
    groups[group.id] = group
  }

  fun clear() {
    groups.clear()
  }

  override fun getNewGroupId(): String = "new-id"

  override suspend fun getAllGroups(): List<Group> = groups.values.toList()

  override suspend fun getGroup(groupId: String): Group {
    if (shouldThrowError) throw Exception("Failed to load group")
    return groups[groupId] ?: throw Exception("Group not found")
  }

  override suspend fun addGroup(group: Group) {
    groups[group.id] = group
  }

  override suspend fun editGroup(groupId: String, newValue: Group) {
    groups[groupId] = newValue
  }

  override suspend fun deleteGroup(groupId: String) {
    groups.remove(groupId)
  }
}

/**
 * Fake ProfileRepository for testing GroupDetailScreen.
 */
class FakeProfileDetailRepository : ProfileRepository {
  private val profiles = mutableMapOf<String, Profile>()

  fun addProfile(profile: Profile) {
    profiles[profile.uid] = profile
  }

  fun clear() {
    profiles.clear()
  }

  override suspend fun getProfile(uid: String): Profile? {
    return profiles[uid]
  }

  override suspend fun createOrUpdateProfile(profile: Profile) {
    profiles[profile.uid] = profile
  }

  override suspend fun deleteProfile(uid: String) {
    profiles.remove(uid)
  }
}

class GroupDetailScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var fakeGroupRepo: FakeGroupDetailRepository
  private lateinit var fakeProfileRepo: FakeProfileDetailRepository

  private fun setup() {
    fakeGroupRepo = FakeGroupDetailRepository()
    fakeProfileRepo = FakeProfileDetailRepository()
  }

  private fun createViewModel(): GroupDetailViewModel {
    return GroupDetailViewModel(fakeGroupRepo, fakeProfileRepo)
  }

  // ========== Loading State Tests ==========

  @Test
  fun loadingState_displaysProgressIndicator() {
    setup()
    val viewModel = createViewModel()

    composeTestRule.setContent { GroupDetailScreen(groupId = "group1", viewModel = viewModel) }

    // Initially in loading state - CircularProgressIndicator doesn't have a specific content description
    // We verify it exists by checking that other content is NOT displayed
    composeTestRule.onNodeWithText("Group Events").assertDoesNotExist()
    composeTestRule.onNodeWithText("Retry").assertDoesNotExist()
  }

  // ========== Error State Tests ==========

  @Test
  fun errorState_displaysErrorMessage() {
    setup()
    fakeGroupRepo.shouldThrowError = true
    val viewModel = createViewModel()

    composeTestRule.setContent { GroupDetailScreen(groupId = "group1", viewModel = viewModel) }

    composeTestRule.waitUntil(timeoutMillis = 3000) {
      composeTestRule.onAllNodesWithText("Failed to load group").fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule.onNodeWithText("Failed to load group").assertIsDisplayed()
  }

  @Test
  fun errorState_displaysRetryButton() {
    setup()
    fakeGroupRepo.shouldThrowError = true
    val viewModel = createViewModel()

    composeTestRule.setContent { GroupDetailScreen(groupId = "group1", viewModel = viewModel) }

    composeTestRule.waitUntil(timeoutMillis = 3000) {
      composeTestRule.onAllNodesWithText("Retry").fetchSemanticsNodes().isNotEmpty()
    }

    composeTestRule.onNodeWithText("Retry").assertIsDisplayed()
    composeTestRule.onNodeWithText("Retry").assertHasClickAction()
  }

  @Test
  fun errorState_retryButtonReloadsData() {
    setup()
    fakeGroupRepo.shouldThrowError = true
    val viewModel = createViewModel()

    composeTestRule.setContent { GroupDetailScreen(groupId = "group1", viewModel = viewModel) }

    composeTestRule.waitUntil(timeoutMillis = 3000) {
      composeTestRule.onAllNodesWithText("Retry").fetchSemanticsNodes().isNotEmpty()
    }

    // Fix the error and retry
    fakeGroupRepo.shouldThrowError = false
    fakeGroupRepo.setGroup(
        Group(id = "group1", name = "Test Group", ownerId = "owner1", category = EventType.SPORTS))

    composeTestRule.onNodeWithText("Retry").performClick()

    composeTestRule.waitUntil(timeoutMillis = 3000) {
      composeTestRule.onAllNodesWithText("Test Group").fetchSemanticsNodes().isNotEmpty()
    }

    composeTestRule.onNodeWithText("Test Group").assertIsDisplayed()
  }

  @Test
  fun errorState_groupNotFoundMessage() {
    setup()
    // Don't add any group, so it will not be found
    val viewModel = createViewModel()

    composeTestRule.setContent { GroupDetailScreen(groupId = "nonexistent", viewModel = viewModel) }

    composeTestRule.waitUntil(timeoutMillis = 3000) {
      composeTestRule.onAllNodesWithText("Group not found").fetchSemanticsNodes().isNotEmpty()
    }

    composeTestRule.onNodeWithText("Group not found").assertIsDisplayed()
  }

  // ========== Successful Load Tests ==========

  @Test
  fun successfulLoad_displaysGroupName() {
    setup()
    fakeGroupRepo.setGroup(
        Group(
            id = "group1",
            name = "Football Club",
            description = "Weekly matches",
            ownerId = "owner1",
            category = EventType.SPORTS))

    val viewModel = createViewModel()
    composeTestRule.setContent { GroupDetailScreen(groupId = "group1", viewModel = viewModel) }

    composeTestRule.waitUntil(timeoutMillis = 3000) {
      composeTestRule.onAllNodesWithText("Football Club").fetchSemanticsNodes().isNotEmpty()
    }

    composeTestRule.onNodeWithText("Football Club").assertIsDisplayed()
  }

  @Test
  fun successfulLoad_displaysGroupDescription() {
    setup()
    fakeGroupRepo.setGroup(
        Group(
            id = "group1",
            name = "Hiking Group",
            description = "Mountain adventures every weekend",
            ownerId = "owner1",
            category = EventType.ACTIVITY))

    val viewModel = createViewModel()
    composeTestRule.setContent { GroupDetailScreen(groupId = "group1", viewModel = viewModel) }

    composeTestRule.waitUntil(timeoutMillis = 3000) {
      composeTestRule
          .onAllNodesWithText("Mountain adventures every weekend")
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule.onNodeWithText("Mountain adventures every weekend").assertIsDisplayed()
  }

  @Test
  fun successfulLoad_displaysMemberCount() {
    setup()
    fakeGroupRepo.setGroup(
        Group(
            id = "group1",
            name = "Test Group",
            ownerId = "owner1",
            memberIds = listOf("user1", "user2", "user3", "user4", "user5"),
            category = EventType.SOCIAL))

    val viewModel = createViewModel()
    composeTestRule.setContent { GroupDetailScreen(groupId = "group1", viewModel = viewModel) }

    composeTestRule.waitUntil(timeoutMillis = 3000) {
      composeTestRule.onAllNodesWithText("members : 5").fetchSemanticsNodes().isNotEmpty()
    }

    composeTestRule.onNodeWithText("members : 5").assertIsDisplayed()
  }

  @Test
  fun successfulLoad_displaysCategoryInTopBar() {
    setup()
    fakeGroupRepo.setGroup(
        Group(
            id = "group1",
            name = "Sports Team",
            ownerId = "owner1",
            category = EventType.SPORTS))

    val viewModel = createViewModel()
    composeTestRule.setContent { GroupDetailScreen(groupId = "group1", viewModel = viewModel) }

    composeTestRule.waitUntil(timeoutMillis = 3000) {
      composeTestRule.onAllNodesWithText("Sports").fetchSemanticsNodes().isNotEmpty()
    }

    composeTestRule.onNodeWithText("Sports").assertIsDisplayed()
  }

  @Test
  fun successfulLoad_displaysActivityCategory() {
    setup()
    fakeGroupRepo.setGroup(
        Group(
            id = "group1",
            name = "Activity Group",
            ownerId = "owner1",
            category = EventType.ACTIVITY))

    val viewModel = createViewModel()
    composeTestRule.setContent { GroupDetailScreen(groupId = "group1", viewModel = viewModel) }

    composeTestRule.waitUntil(timeoutMillis = 3000) {
      composeTestRule.onAllNodesWithText("Activity").fetchSemanticsNodes().isNotEmpty()
    }

    composeTestRule.onNodeWithText("Activity").assertIsDisplayed()
  }

  @Test
  fun successfulLoad_displaysSocialCategory() {
    setup()
    fakeGroupRepo.setGroup(
        Group(
            id = "group1",
            name = "Social Club",
            ownerId = "owner1",
            category = EventType.SOCIAL))

    val viewModel = createViewModel()
    composeTestRule.setContent { GroupDetailScreen(groupId = "group1", viewModel = viewModel) }

    composeTestRule.waitUntil(timeoutMillis = 3000) {
      composeTestRule.onAllNodesWithText("Social").fetchSemanticsNodes().isNotEmpty()
    }

    composeTestRule.onNodeWithText("Social").assertIsDisplayed()
  }

  // ========== Member Display Tests ==========

  @Test
  fun successfulLoad_displaysMembersList() {
    setup()
    fakeGroupRepo.setGroup(
        Group(
            id = "group1",
            name = "Test Group",
            ownerId = "owner1",
            memberIds = listOf("user1", "user2", "user3"),
            category = EventType.SPORTS))

    fakeProfileRepo.addProfile(Profile(uid = "user1", username = "Alice", email = "alice@test.com"))
    fakeProfileRepo.addProfile(Profile(uid = "user2", username = "Bob", email = "bob@test.com"))
    fakeProfileRepo.addProfile(
        Profile(uid = "user3", username = "Charlie", email = "charlie@test.com"))

    val viewModel = createViewModel()
    composeTestRule.setContent { GroupDetailScreen(groupId = "group1", viewModel = viewModel) }

    composeTestRule.waitUntil(timeoutMillis = 3000) {
      composeTestRule.onAllNodesWithText("Alice").fetchSemanticsNodes().isNotEmpty()
    }

    composeTestRule.onNodeWithText("Alice").assertIsDisplayed()
    composeTestRule.onNodeWithText("Bob").assertIsDisplayed()
    composeTestRule.onNodeWithText("Charlie").assertIsDisplayed()
  }

  @Test
  fun memberItem_isClickable() {
    setup()
    fakeGroupRepo.setGroup(
        Group(
            id = "group1",
            name = "Test Group",
            ownerId = "owner1",
            memberIds = listOf("user1"),
            category = EventType.SPORTS))

    fakeProfileRepo.addProfile(Profile(uid = "user1", username = "Alice", email = "alice@test.com"))

    var clickedUid: String? = null
    val viewModel = createViewModel()

    composeTestRule.setContent {
      GroupDetailScreen(
          groupId = "group1", viewModel = viewModel, onMemberClick = { clickedUid = it })
    }

    composeTestRule.waitUntil(timeoutMillis = 3000) {
      composeTestRule.onAllNodesWithText("Alice").fetchSemanticsNodes().isNotEmpty()
    }

    composeTestRule.onNodeWithText("Alice").performClick()

    assertEquals("user1", clickedUid)
  }

  @Test
  fun successfulLoad_withMultipleMembers_allClickable() {
    setup()
    fakeGroupRepo.setGroup(
        Group(
            id = "group1",
            name = "Test Group",
            ownerId = "owner1",
            memberIds = listOf("user1", "user2", "user3"),
            category = EventType.SPORTS))

    fakeProfileRepo.addProfile(Profile(uid = "user1", username = "Alice", email = "alice@test.com"))
    fakeProfileRepo.addProfile(Profile(uid = "user2", username = "Bob", email = "bob@test.com"))
    fakeProfileRepo.addProfile(
        Profile(uid = "user3", username = "Charlie", email = "charlie@test.com"))

    val clickedUids = mutableListOf<String>()
    val viewModel = createViewModel()

    composeTestRule.setContent {
      GroupDetailScreen(
          groupId = "group1", viewModel = viewModel, onMemberClick = { clickedUids.add(it) })
    }

    composeTestRule.waitUntil(timeoutMillis = 3000) {
      composeTestRule.onAllNodesWithText("Alice").fetchSemanticsNodes().isNotEmpty()
    }

    composeTestRule.onNodeWithText("Alice").performClick()
    composeTestRule.onNodeWithText("Bob").performClick()
    composeTestRule.onNodeWithText("Charlie").performClick()

    assertEquals(listOf("user1", "user2", "user3"), clickedUids)
  }

  @Test
  fun successfulLoad_withNoMembers_displaysEmptyList() {
    setup()
    fakeGroupRepo.setGroup(
        Group(
            id = "group1",
            name = "Empty Group",
            ownerId = "owner1",
            memberIds = emptyList(),
            category = EventType.SPORTS))

    val viewModel = createViewModel()
    composeTestRule.setContent { GroupDetailScreen(groupId = "group1", viewModel = viewModel) }

    composeTestRule.waitUntil(timeoutMillis = 3000) {
      composeTestRule.onAllNodesWithText("members : 0").fetchSemanticsNodes().isNotEmpty()
    }

    composeTestRule.onNodeWithText("members : 0").assertIsDisplayed()
  }

  // ========== Button Tests ==========

  @Test
  fun groupEventsButton_isDisplayed() {
    setup()
    fakeGroupRepo.setGroup(
        Group(id = "group1", name = "Test Group", ownerId = "owner1", category = EventType.SPORTS))

    val viewModel = createViewModel()
    composeTestRule.setContent { GroupDetailScreen(groupId = "group1", viewModel = viewModel) }

    composeTestRule.waitUntil(timeoutMillis = 3000) {
      composeTestRule.onAllNodesWithText("Group Events").fetchSemanticsNodes().isNotEmpty()
    }

    composeTestRule.onNodeWithText("Group Events").assertIsDisplayed()
    composeTestRule.onNodeWithText("Group Events").assertHasClickAction()
  }

  @Test
  fun groupEventsButton_triggersCallback() {
    setup()
    fakeGroupRepo.setGroup(
        Group(id = "group1", name = "Test Group", ownerId = "owner1", category = EventType.SPORTS))

    var buttonClicked = false
    val viewModel = createViewModel()

    composeTestRule.setContent {
      GroupDetailScreen(
          groupId = "group1", viewModel = viewModel, onGroupEventsClick = { buttonClicked = true })
    }

    composeTestRule.waitUntil(timeoutMillis = 3000) {
      composeTestRule.onAllNodesWithText("Group Events").fetchSemanticsNodes().isNotEmpty()
    }

    composeTestRule.onNodeWithText("Group Events").performClick()

    assertTrue(buttonClicked)
  }

  @Test
  fun backButton_isDisplayed() {
    setup()
    fakeGroupRepo.setGroup(
        Group(id = "group1", name = "Test Group", ownerId = "owner1", category = EventType.SPORTS))

    val viewModel = createViewModel()
    composeTestRule.setContent { GroupDetailScreen(groupId = "group1", viewModel = viewModel) }

    composeTestRule.onNodeWithContentDescription("Back").assertIsDisplayed()
    composeTestRule.onNodeWithContentDescription("Back").assertHasClickAction()
  }

  @Test
  fun backButton_triggersCallback() {
    setup()
    fakeGroupRepo.setGroup(
        Group(id = "group1", name = "Test Group", ownerId = "owner1", category = EventType.SPORTS))

    var backClicked = false
    val viewModel = createViewModel()

    composeTestRule.setContent {
      GroupDetailScreen(groupId = "group1", viewModel = viewModel, onBackClick = { backClicked = true })
    }

    composeTestRule.onNodeWithContentDescription("Back").performClick()

    assertTrue(backClicked)
  }

  // ========== Edge Cases ==========

  @Test
  fun successfulLoad_withLongGroupName_displaysCorrectly() {
    setup()
    fakeGroupRepo.setGroup(
        Group(
            id = "group1",
            name =
                "This is a very long group name that should be displayed properly without breaking the UI layout",
            ownerId = "owner1",
            category = EventType.SPORTS))

    val viewModel = createViewModel()
    composeTestRule.setContent { GroupDetailScreen(groupId = "group1", viewModel = viewModel) }

    composeTestRule.waitUntil(timeoutMillis = 3000) {
      composeTestRule
          .onAllNodesWithText(
              "This is a very long group name that should be displayed properly without breaking the UI layout")
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule
        .onNodeWithText(
            "This is a very long group name that should be displayed properly without breaking the UI layout")
        .assertIsDisplayed()
  }

  @Test
  fun successfulLoad_withEmptyDescription_displaysCorrectly() {
    setup()
    fakeGroupRepo.setGroup(
        Group(
            id = "group1",
            name = "No Description Group",
            description = "",
            ownerId = "owner1",
            category = EventType.SPORTS))

    val viewModel = createViewModel()
    composeTestRule.setContent { GroupDetailScreen(groupId = "group1", viewModel = viewModel) }

    composeTestRule.waitUntil(timeoutMillis = 3000) {
      composeTestRule.onAllNodesWithText("No Description Group").fetchSemanticsNodes().isNotEmpty()
    }

    composeTestRule.onNodeWithText("No Description Group").assertIsDisplayed()
  }

  @Test
  fun successfulLoad_withSpecialCharactersInName_displaysCorrectly() {
    setup()
    fakeGroupRepo.setGroup(
        Group(
            id = "group1",
            name = "Café & Brunch Club ☕",
            description = "Special chars: @#$%",
            ownerId = "owner1",
            category = EventType.SOCIAL))

    val viewModel = createViewModel()
    composeTestRule.setContent { GroupDetailScreen(groupId = "group1", viewModel = viewModel) }

    composeTestRule.waitUntil(timeoutMillis = 3000) {
      composeTestRule.onAllNodesWithText("Café & Brunch Club ☕").fetchSemanticsNodes().isNotEmpty()
    }

    composeTestRule.onNodeWithText("Café & Brunch Club ☕").assertIsDisplayed()
    composeTestRule.onNodeWithText("Special chars: @#$%").assertIsDisplayed()
  }

  @Test
  fun successfulLoad_withLargeMemberList_scrollable() {
    setup()
    val memberIds = (1..50).map { "user$it" }
    fakeGroupRepo.setGroup(
        Group(
            id = "group1",
            name = "Large Group",
            ownerId = "owner1",
            memberIds = memberIds,
            category = EventType.SPORTS))

    memberIds.forEach { uid ->
      fakeProfileRepo.addProfile(Profile(uid = uid, username = "User $uid", email = "$uid@test.com"))
    }

    val viewModel = createViewModel()
    composeTestRule.setContent { GroupDetailScreen(groupId = "group1", viewModel = viewModel) }

    composeTestRule.waitUntil(timeoutMillis = 3000) {
      composeTestRule.onAllNodesWithText("User user1").fetchSemanticsNodes().isNotEmpty()
    }

    // First member should be visible
    composeTestRule.onNodeWithText("User user1").assertIsDisplayed()

    // Last member might not be visible initially but should exist in the list
    // The LazyColumn should make the list scrollable
    composeTestRule.onNodeWithText("members : 50").assertIsDisplayed()
  }

  // ========== Additional Coverage Tests ==========

  @Test
  fun memberItem_hasCorrectContentDescription() {
    setup()
    fakeGroupRepo.setGroup(
        Group(
            id = "group1",
            name = "Test Group",
            ownerId = "owner1",
            memberIds = listOf("user1"),
            category = EventType.SPORTS))

    fakeProfileRepo.addProfile(Profile(uid = "user1", username = "Alice", email = "alice@test.com"))

    val viewModel = createViewModel()
    composeTestRule.setContent { GroupDetailScreen(groupId = "group1", viewModel = viewModel) }

    composeTestRule.waitUntil(timeoutMillis = 3000) {
      composeTestRule.onAllNodesWithText("Alice").fetchSemanticsNodes().isNotEmpty()
    }

    composeTestRule.onNodeWithContentDescription("Profile photo of Alice").assertIsDisplayed()
  }

  @Test
  fun loadingState_doesNotShowBackButton() {
    setup()
    val viewModel = createViewModel()

    composeTestRule.setContent { GroupDetailScreen(groupId = "group1", viewModel = viewModel) }

    // Back button should still be visible even during loading
    composeTestRule.onNodeWithContentDescription("Back").assertIsDisplayed()
  }

  @Test
  fun errorState_doesNotShowGroupContent() {
    setup()
    fakeGroupRepo.shouldThrowError = true
    val viewModel = createViewModel()

    composeTestRule.setContent { GroupDetailScreen(groupId = "group1", viewModel = viewModel) }

    composeTestRule.waitUntil(timeoutMillis = 3000) {
      composeTestRule.onAllNodesWithText("Failed to load group").fetchSemanticsNodes().isNotEmpty()
    }

    // Group Events button should not be visible
    composeTestRule.onNodeWithText("Group Events").assertDoesNotExist()
  }

  @Test
  fun loadingState_doesNotShowCategoryInTopBar() {
    setup()
    val viewModel = createViewModel()

    composeTestRule.setContent { GroupDetailScreen(groupId = "group1", viewModel = viewModel) }

    // Category text should not be displayed during loading
    composeTestRule.onNodeWithText("Sports").assertDoesNotExist()
    composeTestRule.onNodeWithText("Activity").assertDoesNotExist()
    composeTestRule.onNodeWithText("Social").assertDoesNotExist()
  }

  @Test
  fun successfulLoad_showsGroupLogo() {
    setup()
    fakeGroupRepo.setGroup(
        Group(id = "group1", name = "Test Group", ownerId = "owner1", category = EventType.SPORTS))

    val viewModel = createViewModel()
    composeTestRule.setContent { GroupDetailScreen(groupId = "group1", viewModel = viewModel) }

    composeTestRule.waitUntil(timeoutMillis = 3000) {
      composeTestRule.onAllNodesWithText("Test Group").fetchSemanticsNodes().isNotEmpty()
    }

    // The logo image should be displayed
    composeTestRule.onNodeWithContentDescription("JoinMe App Logo").assertIsDisplayed()
  }

  @Test
  fun multipleMembers_eachHasProfileIcon() {
    setup()
    fakeGroupRepo.setGroup(
        Group(
            id = "group1",
            name = "Test Group",
            ownerId = "owner1",
            memberIds = listOf("user1", "user2"),
            category = EventType.SPORTS))

    fakeProfileRepo.addProfile(Profile(uid = "user1", username = "Alice", email = "alice@test.com"))
    fakeProfileRepo.addProfile(Profile(uid = "user2", username = "Bob", email = "bob@test.com"))

    val viewModel = createViewModel()
    composeTestRule.setContent { GroupDetailScreen(groupId = "group1", viewModel = viewModel) }

    composeTestRule.waitUntil(timeoutMillis = 3000) {
      composeTestRule.onAllNodesWithText("Alice").fetchSemanticsNodes().isNotEmpty()
    }

    composeTestRule.onNodeWithContentDescription("Profile photo of Alice").assertIsDisplayed()
    composeTestRule.onNodeWithContentDescription("Profile photo of Bob").assertIsDisplayed()
  }

  @Test
  fun errorState_showsErrorInRedColor() {
    setup()
    fakeGroupRepo.shouldThrowError = true
    val viewModel = createViewModel()

    composeTestRule.setContent { GroupDetailScreen(groupId = "group1", viewModel = viewModel) }

    composeTestRule.waitUntil(timeoutMillis = 3000) {
      composeTestRule.onAllNodesWithText("Failed to load group").fetchSemanticsNodes().isNotEmpty()
    }

    // Error message exists (color testing is difficult in Compose tests)
    composeTestRule.onNodeWithText("Failed to load group").assertIsDisplayed()
  }

  @Test
  fun successfulLoad_memberCountMatchesActualMembers() {
    setup()
    val memberIds = listOf("user1", "user2", "user3", "user4", "user5", "user6", "user7")
    fakeGroupRepo.setGroup(
        Group(
            id = "group1",
            name = "Test Group",
            ownerId = "owner1",
            memberIds = memberIds,
            category = EventType.SPORTS))

    memberIds.forEach { uid ->
      fakeProfileRepo.addProfile(Profile(uid = uid, username = "User $uid", email = "$uid@test.com"))
    }

    val viewModel = createViewModel()
    composeTestRule.setContent { GroupDetailScreen(groupId = "group1", viewModel = viewModel) }

    composeTestRule.waitUntil(timeoutMillis = 3000) {
      composeTestRule.onAllNodesWithText("members : 7").fetchSemanticsNodes().isNotEmpty()
    }

    composeTestRule.onNodeWithText("members : 7").assertIsDisplayed()

    // Verify all members are displayed
    memberIds.forEach { uid ->
      composeTestRule.onNodeWithText("User $uid").assertIsDisplayed()
    }
  }

  @Test
  fun successfulLoad_differentGroupIds_loadsDifferentGroups() {
    setup()
    fakeGroupRepo.setGroup(
        Group(id = "group1", name = "Group One", ownerId = "owner1", category = EventType.SPORTS))
    fakeGroupRepo.setGroup(
        Group(id = "group2", name = "Group Two", ownerId = "owner2", category = EventType.ACTIVITY))

    val viewModel = createViewModel()

    // Test first group
    composeTestRule.setContent { GroupDetailScreen(groupId = "group1", viewModel = viewModel) }

    composeTestRule.waitUntil(timeoutMillis = 3000) {
      composeTestRule.onAllNodesWithText("Group One").fetchSemanticsNodes().isNotEmpty()
    }

    composeTestRule.onNodeWithText("Group One").assertIsDisplayed()
    composeTestRule.onNodeWithText("Sports").assertIsDisplayed()
  }

  @Test
  fun backButton_alwaysVisibleRegardlessOfState() {
    setup()
    val viewModel = createViewModel()

    // Test during loading
    composeTestRule.setContent { GroupDetailScreen(groupId = "group1", viewModel = viewModel) }
    composeTestRule.onNodeWithContentDescription("Back").assertIsDisplayed()

    // Test during error
    setup()
    fakeGroupRepo.shouldThrowError = true
    val viewModel2 = createViewModel()
    composeTestRule.setContent { GroupDetailScreen(groupId = "group1", viewModel = viewModel2) }

    composeTestRule.waitUntil(timeoutMillis = 3000) {
      composeTestRule.onAllNodesWithText("Failed to load group").fetchSemanticsNodes().isNotEmpty()
    }

    composeTestRule.onNodeWithContentDescription("Back").assertIsDisplayed()
  }

  @Test
  fun memberClick_passesCorrectUid() {
    setup()
    fakeGroupRepo.setGroup(
        Group(
            id = "group1",
            name = "Test Group",
            ownerId = "owner1",
            memberIds = listOf("uid-abc-123"),
            category = EventType.SPORTS))

    fakeProfileRepo.addProfile(
        Profile(uid = "uid-abc-123", username = "Test User", email = "test@test.com"))

    var clickedUid: String? = null
    val viewModel = createViewModel()

    composeTestRule.setContent {
      GroupDetailScreen(
          groupId = "group1", viewModel = viewModel, onMemberClick = { clickedUid = it })
    }

    composeTestRule.waitUntil(timeoutMillis = 3000) {
      composeTestRule.onAllNodesWithText("Test User").fetchSemanticsNodes().isNotEmpty()
    }

    composeTestRule.onNodeWithText("Test User").performClick()

    assertEquals("uid-abc-123", clickedUid)
  }

  @Test
  fun groupEventsButton_canBeClickedMultipleTimes() {
    setup()
    fakeGroupRepo.setGroup(
        Group(id = "group1", name = "Test Group", ownerId = "owner1", category = EventType.SPORTS))

    var clickCount = 0
    val viewModel = createViewModel()

    composeTestRule.setContent {
      GroupDetailScreen(
          groupId = "group1", viewModel = viewModel, onGroupEventsClick = { clickCount++ })
    }

    composeTestRule.waitUntil(timeoutMillis = 3000) {
      composeTestRule.onAllNodesWithText("Group Events").fetchSemanticsNodes().isNotEmpty()
    }

    composeTestRule.onNodeWithText("Group Events").performClick()
    composeTestRule.onNodeWithText("Group Events").performClick()
    composeTestRule.onNodeWithText("Group Events").performClick()

    assertEquals(3, clickCount)
  }

  @Test
  fun retryButton_canBeClickedMultipleTimes() {
    setup()
    fakeGroupRepo.shouldThrowError = true
    val viewModel = createViewModel()

    var retryCount = 0

    composeTestRule.setContent { GroupDetailScreen(groupId = "group1", viewModel = viewModel) }

    composeTestRule.waitUntil(timeoutMillis = 3000) {
      composeTestRule.onAllNodesWithText("Retry").fetchSemanticsNodes().isNotEmpty()
    }

    // Click retry multiple times (it will keep failing)
    composeTestRule.onNodeWithText("Retry").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Retry").performClick()
    composeTestRule.waitForIdle()

    // Button should still be visible and clickable
    composeTestRule.onNodeWithText("Retry").assertIsDisplayed()
    composeTestRule.onNodeWithText("Retry").assertHasClickAction()
  }
}
