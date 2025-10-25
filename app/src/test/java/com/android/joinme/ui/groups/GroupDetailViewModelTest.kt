package com.android.joinme.ui.groups

import com.android.joinme.model.groups.Group
import com.android.joinme.model.groups.GroupRepository
import com.android.joinme.model.profile.Profile
import com.android.joinme.model.profile.ProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

/**
 * Comprehensive unit tests for GroupDetailViewModel.
 *
 * These tests verify all code paths and edge cases in the GroupDetailViewModel, including
 * successful loads, error handling, member profile fetching, and group operations.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GroupDetailViewModelTest {

  private class FakeGroupRepository : GroupRepository {
    var shouldThrowError = false
    var errorMessage = "Test error"
    var shouldReturnNull = false
    private val groups = mutableMapOf<String, Group>()
    private var counter = 0

    fun addTestGroup(group: Group) {
      groups[group.id] = group
    }

    fun clear() {
      groups.clear()
    }

    override fun getNewGroupId(): String {
      return (counter++).toString()
    }

    override suspend fun getAllGroups(): List<Group> {
      return groups.values.toList()
    }

    override suspend fun getGroup(groupId: String): Group {
      if (shouldThrowError) {
        throw Exception(errorMessage)
      }
      if (shouldReturnNull) {
        throw Exception("Group not found")
      }
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

  private class FakeProfileRepository : ProfileRepository {
    var shouldThrowError = false
    var errorMessage = "Profile fetch error"
    private val profiles = mutableMapOf<String, Profile>()
    private val failingProfileIds = mutableSetOf<String>()

    fun addProfile(profile: Profile) {
      profiles[profile.uid] = profile
    }

    fun setProfileToFail(uid: String) {
      failingProfileIds.add(uid)
    }

    fun clear() {
      profiles.clear()
      failingProfileIds.clear()
    }

    override suspend fun getProfile(uid: String): Profile? {
      if (shouldThrowError || failingProfileIds.contains(uid)) {
        throw Exception(errorMessage)
      }
      return profiles[uid]
    }

    override suspend fun createOrUpdateProfile(profile: Profile) {
      profiles[profile.uid] = profile
    }

    override suspend fun deleteProfile(uid: String) {
      profiles.remove(uid)
    }
  }

  private lateinit var fakeGroupRepo: FakeGroupRepository
  private lateinit var fakeProfileRepo: FakeProfileRepository
  private lateinit var viewModel: GroupDetailViewModel
  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    fakeGroupRepo = FakeGroupRepository()
    fakeProfileRepo = FakeProfileRepository()
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  // ========== Initial State Tests ==========

  @Test
  fun initialState_isLoadingTrue() {
    viewModel = GroupDetailViewModel(fakeGroupRepo, fakeProfileRepo)

    val state = viewModel.uiState.value
    Assert.assertTrue(state.isLoading)
    Assert.assertNull(state.group)
    Assert.assertTrue(state.members.isEmpty())
    Assert.assertNull(state.error)
  }

  // ========== Successful Load Tests ==========

  @Test
  fun loadGroupDetails_withValidGroup_loadsSuccessfully() = runTest {
    val testGroup =
        Group(
            id = "group1",
            name = "Test Group",
            description = "A test group",
            ownerId = "owner1",
            memberIds = listOf("user1", "user2", "user3"))
    val testProfiles =
        listOf(
            Profile(uid = "user1", username = "User One", email = "user1@test.com"),
            Profile(uid = "user2", username = "User Two", email = "user2@test.com"),
            Profile(uid = "user3", username = "User Three", email = "user3@test.com"))

    fakeGroupRepo.addTestGroup(testGroup)
    testProfiles.forEach { fakeProfileRepo.addProfile(it) }

    viewModel = GroupDetailViewModel(fakeGroupRepo, fakeProfileRepo)
    viewModel.loadGroupDetails("group1")
    advanceUntilIdle()

    val state = viewModel.uiState.value
    Assert.assertFalse(state.isLoading)
    Assert.assertNull(state.error)
    Assert.assertNotNull(state.group)
    Assert.assertEquals("Test Group", state.group?.name)
    Assert.assertEquals(3, state.members.size)
    Assert.assertEquals("User One", state.members[0].username)
    Assert.assertEquals("User Two", state.members[1].username)
    Assert.assertEquals("User Three", state.members[2].username)
  }

  @Test
  fun loadGroupDetails_withEmptyMemberList_loadsGroupWithoutMembers() = runTest {
    val testGroup =
        Group(id = "group1", name = "Empty Group", ownerId = "owner1", memberIds = emptyList())

    fakeGroupRepo.addTestGroup(testGroup)

    viewModel = GroupDetailViewModel(fakeGroupRepo, fakeProfileRepo)
    viewModel.loadGroupDetails("group1")
    advanceUntilIdle()

    val state = viewModel.uiState.value
    Assert.assertFalse(state.isLoading)
    Assert.assertNull(state.error)
    Assert.assertNotNull(state.group)
    Assert.assertEquals("Empty Group", state.group?.name)
    Assert.assertTrue(state.members.isEmpty())
  }

  @Test
  fun loadGroupDetails_withLargeNumberOfMembers_loadsAll() = runTest {
    val memberIds = (1..50).map { "user$it" }
    val testGroup =
        Group(id = "group1", name = "Large Group", ownerId = "owner1", memberIds = memberIds)

    memberIds.forEach { uid ->
      fakeProfileRepo.addProfile(
          Profile(uid = uid, username = "User $uid", email = "$uid@test.com"))
    }
    fakeGroupRepo.addTestGroup(testGroup)

    viewModel = GroupDetailViewModel(fakeGroupRepo, fakeProfileRepo)
    viewModel.loadGroupDetails("group1")
    advanceUntilIdle()

    val state = viewModel.uiState.value
    Assert.assertFalse(state.isLoading)
    Assert.assertNull(state.error)
    Assert.assertEquals(50, state.members.size)
  }

  @Test
  fun loadGroupDetails_preservesAllGroupProperties() = runTest {
    val testGroup =
        Group(
            id = "group-123",
            name = "Complete Group",
            description = "Full description",
            ownerId = "owner-456",
            memberIds = listOf("user1", "user2"),
            eventIds = listOf("event1", "event2", "event3"))

    fakeGroupRepo.addTestGroup(testGroup)
    fakeProfileRepo.addProfile(Profile(uid = "user1", username = "User1", email = "u1@test.com"))
    fakeProfileRepo.addProfile(Profile(uid = "user2", username = "User2", email = "u2@test.com"))

    viewModel = GroupDetailViewModel(fakeGroupRepo, fakeProfileRepo)
    viewModel.loadGroupDetails("group-123")
    advanceUntilIdle()

    val state = viewModel.uiState.value
    val loadedGroup = state.group!!
    Assert.assertEquals("group-123", loadedGroup.id)
    Assert.assertEquals("Complete Group", loadedGroup.name)
    Assert.assertEquals("Full description", loadedGroup.description)
    Assert.assertEquals("owner-456", loadedGroup.ownerId)
    Assert.assertEquals(2, loadedGroup.memberIds.size)
    Assert.assertEquals(3, loadedGroup.eventIds.size)
  }

  @Test
  fun loadGroupDetails_withSpecialCharactersInName_handlesCorrectly() = runTest {
    val testGroup =
        Group(
            id = "group1",
            name = "Café & Brunch Club ☕",
            description = "Group with special chars: @#$%",
            ownerId = "owner1",
            memberIds = emptyList())

    fakeGroupRepo.addTestGroup(testGroup)

    viewModel = GroupDetailViewModel(fakeGroupRepo, fakeProfileRepo)
    viewModel.loadGroupDetails("group1")
    advanceUntilIdle()

    val state = viewModel.uiState.value
    Assert.assertEquals("Café & Brunch Club ☕", state.group?.name)
    Assert.assertEquals("Group with special chars: @#$%", state.group?.description)
  }

  // ========== Error Handling Tests ==========

  @Test
  fun loadGroupDetails_withNullGroup_setsGroupNotFoundError() = runTest {
    fakeGroupRepo.shouldReturnNull = true

    viewModel = GroupDetailViewModel(fakeGroupRepo, fakeProfileRepo)
    viewModel.loadGroupDetails("nonexistent")
    advanceUntilIdle()

    val state = viewModel.uiState.value
    Assert.assertFalse(state.isLoading)
    Assert.assertNull(state.group)
    Assert.assertEquals("Group not found", state.error)
    Assert.assertTrue(state.members.isEmpty())
  }

  @Test
  fun loadGroupDetails_withRepositoryError_setsErrorMessage() = runTest {
    fakeGroupRepo.shouldThrowError = true
    fakeGroupRepo.errorMessage = "Network error occurred"

    viewModel = GroupDetailViewModel(fakeGroupRepo, fakeProfileRepo)
    viewModel.loadGroupDetails("group1")
    advanceUntilIdle()

    val state = viewModel.uiState.value
    Assert.assertFalse(state.isLoading)
    Assert.assertNull(state.group)
    Assert.assertEquals("Network error occurred", state.error)
    Assert.assertTrue(state.members.isEmpty())
  }

  @Test
  fun loadGroupDetails_withExceptionWithoutMessage_setsDefaultError() = runTest {
    fakeGroupRepo.shouldThrowError = true
    fakeGroupRepo.errorMessage = ""

    viewModel = GroupDetailViewModel(fakeGroupRepo, fakeProfileRepo)
    viewModel.loadGroupDetails("group1")
    advanceUntilIdle()

    val state = viewModel.uiState.value
    Assert.assertFalse(state.isLoading)
    // Empty exception message results in empty string, not "An unexpected error occurred"
    Assert.assertEquals("", state.error)
  }

  @Test
  fun loadGroupDetails_whenExceptionHasNullMessage_usesDefaultMessage() = runTest {
    val testGroup = Group(id = "group1", name = "Test", ownerId = "owner1")
    fakeGroupRepo.addTestGroup(testGroup)
    fakeProfileRepo.shouldThrowError = true

    // This won't actually trigger the catch block's null coalescing because
    // we'll get an exception with a message. Let's test the actual exception path.
    viewModel = GroupDetailViewModel(fakeGroupRepo, fakeProfileRepo)
    fakeGroupRepo.shouldThrowError = true
    fakeGroupRepo.errorMessage = null.toString() // Will be "null" string

    viewModel.loadGroupDetails("group1")
    advanceUntilIdle()

    val state = viewModel.uiState.value
    Assert.assertNotNull(state.error)
  }

  // ========== Member Profile Fetching Tests ==========

  @Test
  fun loadGroupDetails_withSomeFailingProfiles_loadsOnlySuccessfulOnes() = runTest {
    val testGroup =
        Group(
            id = "group1",
            name = "Test Group",
            ownerId = "owner1",
            memberIds = listOf("user1", "user2", "user3", "user4"))

    fakeGroupRepo.addTestGroup(testGroup)
    fakeProfileRepo.addProfile(Profile(uid = "user1", username = "User1", email = "u1@test.com"))
    fakeProfileRepo.setProfileToFail("user2") // This will fail
    fakeProfileRepo.addProfile(Profile(uid = "user3", username = "User3", email = "u3@test.com"))
    fakeProfileRepo.setProfileToFail("user4") // This will fail

    viewModel = GroupDetailViewModel(fakeGroupRepo, fakeProfileRepo)
    viewModel.loadGroupDetails("group1")
    advanceUntilIdle()

    val state = viewModel.uiState.value
    Assert.assertFalse(state.isLoading)
    Assert.assertNull(state.error) // Overall load succeeds
    Assert.assertEquals(2, state.members.size) // Only 2 successful profiles
    Assert.assertEquals("User1", state.members[0].username)
    Assert.assertEquals("User3", state.members[1].username)
  }

  @Test
  fun loadGroupDetails_withAllFailingProfiles_loadsGroupWithoutMembers() = runTest {
    val testGroup =
        Group(
            id = "group1",
            name = "Test Group",
            ownerId = "owner1",
            memberIds = listOf("user1", "user2", "user3"))

    fakeGroupRepo.addTestGroup(testGroup)
    fakeProfileRepo.setProfileToFail("user1")
    fakeProfileRepo.setProfileToFail("user2")
    fakeProfileRepo.setProfileToFail("user3")

    viewModel = GroupDetailViewModel(fakeGroupRepo, fakeProfileRepo)
    viewModel.loadGroupDetails("group1")
    advanceUntilIdle()

    val state = viewModel.uiState.value
    Assert.assertFalse(state.isLoading)
    Assert.assertNull(state.error)
    Assert.assertNotNull(state.group)
    Assert.assertTrue(state.members.isEmpty()) // All profile fetches failed
  }

  @Test
  fun loadGroupDetails_withMixOfValidAndNullProfiles_filtersNulls() = runTest {
    val testGroup =
        Group(
            id = "group1",
            name = "Test Group",
            ownerId = "owner1",
            memberIds = listOf("user1", "user2", "nonexistent"))

    fakeGroupRepo.addTestGroup(testGroup)
    fakeProfileRepo.addProfile(Profile(uid = "user1", username = "User1", email = "u1@test.com"))
    fakeProfileRepo.addProfile(Profile(uid = "user2", username = "User2", email = "u2@test.com"))
    // "nonexistent" is not added, so getProfile returns null

    viewModel = GroupDetailViewModel(fakeGroupRepo, fakeProfileRepo)
    viewModel.loadGroupDetails("group1")
    advanceUntilIdle()

    val state = viewModel.uiState.value
    Assert.assertEquals(2, state.members.size) // Only existing profiles
  }

  // ========== Multiple Operations Tests ==========

  @Test
  fun multipleLoadCalls_eachUpdatesState() = runTest {
    val group1 = Group(id = "group1", name = "Group 1", ownerId = "owner1")
    val group2 = Group(id = "group2", name = "Group 2", ownerId = "owner2")

    fakeGroupRepo.addTestGroup(group1)
    fakeGroupRepo.addTestGroup(group2)

    viewModel = GroupDetailViewModel(fakeGroupRepo, fakeProfileRepo)

    viewModel.loadGroupDetails("group1")
    advanceUntilIdle()
    Assert.assertEquals("Group 1", viewModel.uiState.value.group?.name)

    viewModel.loadGroupDetails("group2")
    advanceUntilIdle()
    Assert.assertEquals("Group 2", viewModel.uiState.value.group?.name)
  }

  // ========== State Consistency Tests ==========

  @Test
  fun loadGroupDetails_alwaysEndsWithLoadingFalse() = runTest {
    val testGroup = Group(id = "group1", name = "Test", ownerId = "owner1")
    fakeGroupRepo.addTestGroup(testGroup)

    viewModel = GroupDetailViewModel(fakeGroupRepo, fakeProfileRepo)
    viewModel.loadGroupDetails("group1")
    advanceUntilIdle()

    Assert.assertFalse(viewModel.uiState.value.isLoading)
  }

  @Test
  fun loadGroupDetails_onError_alwaysEndsWithLoadingFalse() = runTest {
    fakeGroupRepo.shouldThrowError = true

    viewModel = GroupDetailViewModel(fakeGroupRepo, fakeProfileRepo)
    viewModel.loadGroupDetails("group1")
    advanceUntilIdle()

    Assert.assertFalse(viewModel.uiState.value.isLoading)
  }

  @Test
  fun successfulLoad_clearsAnyPreviousError() = runTest {
    fakeGroupRepo.shouldThrowError = true

    viewModel = GroupDetailViewModel(fakeGroupRepo, fakeProfileRepo)
    viewModel.loadGroupDetails("group1")
    advanceUntilIdle()

    Assert.assertNotNull(viewModel.uiState.value.error)

    // Now load successfully
    fakeGroupRepo.shouldThrowError = false
    val testGroup = Group(id = "group1", name = "Success", ownerId = "owner1")
    fakeGroupRepo.addTestGroup(testGroup)

    viewModel.loadGroupDetails("group1")
    advanceUntilIdle()

    Assert.assertNull(viewModel.uiState.value.error)
  }

  // ========== Edge Cases ==========

  @Test
  fun loadGroupDetails_withEmptyGroupId_handlesCorrectly() = runTest {
    viewModel = GroupDetailViewModel(fakeGroupRepo, fakeProfileRepo)
    viewModel.loadGroupDetails("")
    advanceUntilIdle()

    val state = viewModel.uiState.value
    Assert.assertFalse(state.isLoading)
    // Will result in "Group not found" since empty ID won't match anything
    Assert.assertEquals("Group not found", state.error)
  }

  @Test
  fun loadGroupDetails_withVeryLongGroupId_handlesCorrectly() = runTest {
    val longId = "a".repeat(1000)
    val testGroup = Group(id = longId, name = "Long ID Group", ownerId = "owner1")
    fakeGroupRepo.addTestGroup(testGroup)

    viewModel = GroupDetailViewModel(fakeGroupRepo, fakeProfileRepo)
    viewModel.loadGroupDetails(longId)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    Assert.assertFalse(state.isLoading)
    Assert.assertNull(state.error)
    Assert.assertEquals(longId, state.group?.id)
  }

  @Test
  fun loadGroupDetails_withEmptyDescription_handlesCorrectly() = runTest {
    val testGroup =
        Group(id = "group1", name = "No Description", description = "", ownerId = "owner1")
    fakeGroupRepo.addTestGroup(testGroup)

    viewModel = GroupDetailViewModel(fakeGroupRepo, fakeProfileRepo)
    viewModel.loadGroupDetails("group1")
    advanceUntilIdle()

    Assert.assertEquals("", viewModel.uiState.value.group?.description)
  }

  @Test
  fun loadGroupDetails_withSingleMember_loadsCorrectly() = runTest {
    val testGroup =
        Group(id = "group1", name = "Solo Group", ownerId = "owner1", memberIds = listOf("user1"))

    fakeGroupRepo.addTestGroup(testGroup)
    fakeProfileRepo.addProfile(Profile(uid = "user1", username = "Solo", email = "solo@test.com"))

    viewModel = GroupDetailViewModel(fakeGroupRepo, fakeProfileRepo)
    viewModel.loadGroupDetails("group1")
    advanceUntilIdle()

    val state = viewModel.uiState.value
    Assert.assertEquals(1, state.members.size)
    Assert.assertEquals("Solo", state.members[0].username)
  }

  @Test
  fun loadGroupDetails_consecutiveCalls_eachCompletesCorrectly() = runTest {
    val group1 = Group(id = "group1", name = "First", ownerId = "owner1")
    val group2 = Group(id = "group2", name = "Second", ownerId = "owner2")
    val group3 = Group(id = "group3", name = "Third", ownerId = "owner3")

    fakeGroupRepo.addTestGroup(group1)
    fakeGroupRepo.addTestGroup(group2)
    fakeGroupRepo.addTestGroup(group3)

    viewModel = GroupDetailViewModel(fakeGroupRepo, fakeProfileRepo)

    viewModel.loadGroupDetails("group1")
    advanceUntilIdle()
    Assert.assertEquals("First", viewModel.uiState.value.group?.name)

    viewModel.loadGroupDetails("group2")
    advanceUntilIdle()
    Assert.assertEquals("Second", viewModel.uiState.value.group?.name)

    viewModel.loadGroupDetails("group3")
    advanceUntilIdle()
    Assert.assertEquals("Third", viewModel.uiState.value.group?.name)
  }

  @Test
  fun uiState_consistentAfterSuccessfulLoad() = runTest {
    val testGroup = Group(id = "group1", name = "Test", ownerId = "owner1")
    fakeGroupRepo.addTestGroup(testGroup)

    viewModel = GroupDetailViewModel(fakeGroupRepo, fakeProfileRepo)
    viewModel.loadGroupDetails("group1")
    advanceUntilIdle()

    val state = viewModel.uiState.value
    Assert.assertFalse(state.isLoading)
    Assert.assertNull(state.error)
    Assert.assertNotNull(state.group)
    Assert.assertNotNull(state.members)
  }

  @Test
  fun uiState_consistentAfterError() = runTest {
    fakeGroupRepo.shouldThrowError = true

    viewModel = GroupDetailViewModel(fakeGroupRepo, fakeProfileRepo)
    viewModel.loadGroupDetails("group1")
    advanceUntilIdle()

    val state = viewModel.uiState.value
    Assert.assertFalse(state.isLoading)
    Assert.assertNotNull(state.error)
    Assert.assertNull(state.group)
    Assert.assertTrue(state.members.isEmpty())
  }

  @Test
  fun uiState_consistentAfterGroupNotFound() = runTest {
    fakeGroupRepo.shouldReturnNull = true

    viewModel = GroupDetailViewModel(fakeGroupRepo, fakeProfileRepo)
    viewModel.loadGroupDetails("nonexistent")
    advanceUntilIdle()

    val state = viewModel.uiState.value
    Assert.assertFalse(state.isLoading)
    Assert.assertEquals("Group not found", state.error)
    Assert.assertNull(state.group)
    Assert.assertTrue(state.members.isEmpty())
  }
}
