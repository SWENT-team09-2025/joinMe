// Implemented with help of Claude AI
package com.android.joinme.viewmodel

import android.content.Context
import android.net.Uri
import com.android.joinme.model.groups.Group
import com.android.joinme.model.groups.GroupRepository
import com.android.joinme.ui.groups.GroupListViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for GroupListViewModel.
 *
 * These tests use a fake repository to verify ViewModel behavior without Firebase dependencies.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GroupListViewModelTest {

  private class FakeGroupRepository : GroupRepository {
    var shouldThrowError = false
    var errorMessage = "Test error"
    private val groups = mutableListOf<Group>()
    private var counter = 0

    fun setGroups(newGroups: List<Group>) {
      groups.clear()
      groups.addAll(newGroups)
    }

    override fun getNewGroupId(): String {
      return (counter++).toString()
    }

    override suspend fun getAllGroups(): List<Group> {
      if (shouldThrowError) {
        throw Exception(errorMessage)
      }
      return groups.toList()
    }

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

      val removed = groups.removeIf { it.id == groupId }
      if (!removed) {
        throw Exception("Group not found")
      }
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

    override suspend fun getCommonGroups(userIds: List<String>): List<Group> {
      if (shouldThrowError) throw Exception(errorMessage)
      if (userIds.isEmpty()) return emptyList()
      return groups.filter { group -> userIds.all { userId -> group.memberIds.contains(userId) } }
    }

    override suspend fun uploadGroupPhoto(
        context: Context,
        groupId: String,
        imageUri: Uri
    ): String {
      // Not needed for these tests
      return "http://fakeurl.com/photo.jpg"
    }

    override suspend fun deleteGroupPhoto(groupId: String) {
      // Not needed for these tests
    }
  }

  private lateinit var fakeRepo: FakeGroupRepository
  private lateinit var viewModel: GroupListViewModel
  private val testDispatcher = StandardTestDispatcher()
  private val testUserId = "test-user-123"

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    fakeRepo = FakeGroupRepository()
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  /** Helper to mock Firebase Auth with a logged-in user */
  private fun mockFirebaseAuth() {
    mockkStatic(FirebaseAuth::class)
    val mockAuth = mockk<FirebaseAuth>()
    val mockUser = mockk<FirebaseUser>()
    every { FirebaseAuth.getInstance() } returns mockAuth
    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns testUserId
  }

  /** Helper to clean up Firebase Auth mocks */
  private fun cleanupFirebaseAuth() {
    unmockkStatic(FirebaseAuth::class)
  }

  @Test
  fun init_loadsGroupsAutomatically() = runTest {
    mockFirebaseAuth()
    val testGroups =
        listOf(
            Group(
                id = "1", name = "Group 1", ownerId = "owner1", memberIds = List(10) { "user$it" }),
            Group(
                id = "2", name = "Group 2", ownerId = "owner2", memberIds = List(20) { "user$it" }))
    fakeRepo.setGroups(testGroups)

    viewModel = GroupListViewModel(fakeRepo)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals(2, state.groups.size)
    assertEquals("Group 1", state.groups[0].name)
    assertEquals("Group 2", state.groups[1].name)
    assertNull(state.errorMsg)
    assertFalse(state.isLoading)

    cleanupFirebaseAuth()
  }

  @Test
  fun init_withEmptyRepository_returnsEmptyList() = runTest {
    mockFirebaseAuth()
    fakeRepo.setGroups(emptyList())

    viewModel = GroupListViewModel(fakeRepo)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertTrue(state.groups.isEmpty())
    assertNull(state.errorMsg)
    assertFalse(state.isLoading)

    cleanupFirebaseAuth()
  }

  @Test
  fun init_withRepositoryError_setsErrorMessage() = runTest {
    mockFirebaseAuth()
    fakeRepo.shouldThrowError = true
    fakeRepo.errorMessage = "Network error"

    viewModel = GroupListViewModel(fakeRepo)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertTrue(state.groups.isEmpty())
    assertEquals("Failed to load groups: Network error", state.errorMsg)
    assertFalse(state.isLoading)

    cleanupFirebaseAuth()
  }

  @Test
  fun refreshUIState_reloadsGroups() = runTest {
    mockFirebaseAuth()
    fakeRepo.setGroups(listOf(Group(id = "1", name = "Initial Group", ownerId = "owner1")))
    viewModel = GroupListViewModel(fakeRepo)
    advanceUntilIdle()

    fakeRepo.setGroups(
        listOf(
            Group(id = "2", name = "Updated Group 1", ownerId = "owner2"),
            Group(id = "3", name = "Updated Group 2", ownerId = "owner3")))
    viewModel.refreshUIState()
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals(2, state.groups.size)
    assertEquals("Updated Group 1", state.groups[0].name)
    assertEquals("Updated Group 2", state.groups[1].name)
    assertNull(state.errorMsg)

    cleanupFirebaseAuth()
  }

  @Test
  fun refreshUIState_afterError_canRecover() = runTest {
    mockFirebaseAuth()
    fakeRepo.shouldThrowError = true
    viewModel = GroupListViewModel(fakeRepo)
    advanceUntilIdle()
    assertNotNull(viewModel.uiState.value.errorMsg)

    fakeRepo.shouldThrowError = false
    fakeRepo.setGroups(listOf(Group(id = "1", name = "Recovered Group", ownerId = "owner1")))
    viewModel.refreshUIState()
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals(1, state.groups.size)
    assertEquals("Recovered Group", state.groups[0].name)
    assertNull(state.errorMsg)

    cleanupFirebaseAuth()
  }

  @Test
  fun refreshUIState_withError_setsErrorMessage() = runTest {
    mockFirebaseAuth()
    fakeRepo.setGroups(listOf(Group(id = "1", name = "Initial", ownerId = "owner1")))
    viewModel = GroupListViewModel(fakeRepo)
    advanceUntilIdle()

    fakeRepo.shouldThrowError = true
    fakeRepo.errorMessage = "Network error"
    viewModel.refreshUIState()
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals("Failed to load groups: Network error", state.errorMsg)
    // Groups are preserved on error
    assertEquals(1, state.groups.size)

    cleanupFirebaseAuth()
  }

  @Test
  fun clearErrorMsg_handlesAllCases() = runTest {
    mockFirebaseAuth()

    // Case 1: Removes error message when present
    fakeRepo.shouldThrowError = true
    fakeRepo.errorMessage = "Test error message"
    viewModel = GroupListViewModel(fakeRepo)
    advanceUntilIdle()
    assertNotNull(viewModel.uiState.value.errorMsg)

    viewModel.clearErrorMsg()

    assertNull(viewModel.uiState.value.errorMsg)

    // Case 2: Does not affect other state when clearing error
    val state = viewModel.uiState.value
    assertNull(state.errorMsg)
    assertTrue(state.groups.isEmpty())
    assertFalse(state.isLoading)

    // Case 3: Clearing error when no error present does nothing
    viewModel.clearErrorMsg()

    val stateAfterSecondClear = viewModel.uiState.value
    assertNull(stateAfterSecondClear.errorMsg)
    assertTrue(stateAfterSecondClear.groups.isEmpty())
    assertFalse(stateAfterSecondClear.isLoading)

    cleanupFirebaseAuth()
  }

  @Test
  fun loadGroups_preservesAllGroupProperties() = runTest {
    mockFirebaseAuth()
    val testGroup =
        Group(
            id = "test-123",
            name = "Test Group",
            ownerId = "owner-123",
            description = "A test group for sports",
            memberIds = List(42) { "user$it" },
            eventIds = listOf("event1", "event2"))
    fakeRepo.setGroups(listOf(testGroup))

    viewModel = GroupListViewModel(fakeRepo)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    val loadedGroup = state.groups.first()
    assertEquals("test-123", loadedGroup.id)
    assertEquals("Test Group", loadedGroup.name)
    assertEquals("owner-123", loadedGroup.ownerId)
    assertEquals("A test group for sports", loadedGroup.description)
    assertEquals(42, loadedGroup.membersCount)
    assertEquals(42, loadedGroup.memberIds.size)
    assertEquals(2, loadedGroup.eventIds.size)

    cleanupFirebaseAuth()
  }

  @Test
  fun loadGroups_withMultipleGroups_maintainsOrder() = runTest {
    mockFirebaseAuth()
    val groups =
        listOf(
            Group(id = "1", name = "First", ownerId = "owner1", memberIds = List(10) { "user$it" }),
            Group(
                id = "2", name = "Second", ownerId = "owner2", memberIds = List(20) { "user$it" }),
            Group(id = "3", name = "Third", ownerId = "owner3", memberIds = List(30) { "user$it" }))
    fakeRepo.setGroups(groups)

    viewModel = GroupListViewModel(fakeRepo)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals(3, state.groups.size)
    assertEquals("First", state.groups[0].name)
    assertEquals("Second", state.groups[1].name)
    assertEquals("Third", state.groups[2].name)

    cleanupFirebaseAuth()
  }

  @Test
  fun loadGroups_withManyGroups_loadsAll() = runTest {
    mockFirebaseAuth()
    val manyGroups =
        (1..100).map {
          Group(
              id = "$it",
              name = "Group $it",
              ownerId = "owner$it",
              memberIds = List(it) { i -> "user$i" })
        }
    fakeRepo.setGroups(manyGroups)

    viewModel = GroupListViewModel(fakeRepo)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals(100, state.groups.size)
    assertEquals("Group 1", state.groups[0].name)
    assertEquals("Group 100", state.groups[99].name)

    cleanupFirebaseAuth()
  }

  @Test
  fun errorMessage_isPreservedCorrectly() = runTest {
    mockFirebaseAuth()
    fakeRepo.shouldThrowError = true
    fakeRepo.errorMessage = "Connection timeout after 30 seconds"

    viewModel = GroupListViewModel(fakeRepo)
    advanceUntilIdle()

    assertEquals(
        "Failed to load groups: Connection timeout after 30 seconds",
        viewModel.uiState.value.errorMsg)

    cleanupFirebaseAuth()
  }

  @Test
  fun successfulLoad_afterError_clearsErrorAndSetsGroups() = runTest {
    mockFirebaseAuth()
    fakeRepo.shouldThrowError = true
    viewModel = GroupListViewModel(fakeRepo)
    advanceUntilIdle()
    assertNotNull(viewModel.uiState.value.errorMsg)

    fakeRepo.shouldThrowError = false
    fakeRepo.setGroups(listOf(Group(id = "1", name = "Success", ownerId = "owner1")))
    viewModel.refreshUIState()
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertNull(state.errorMsg)
    assertEquals(1, state.groups.size)
    assertEquals("Success", state.groups[0].name)

    cleanupFirebaseAuth()
  }

  @Test
  fun uiState_afterError_hasConsistentState() = runTest {
    mockFirebaseAuth()
    fakeRepo.shouldThrowError = true

    viewModel = GroupListViewModel(fakeRepo)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertNotNull(state.errorMsg)
    assertTrue(state.groups.isEmpty())

    cleanupFirebaseAuth()
  }

  @Test
  fun refreshUIState_preservesGroupsOnError() = runTest {
    mockFirebaseAuth()
    fakeRepo.setGroups(listOf(Group(id = "1", name = "Test", ownerId = "owner1")))
    viewModel = GroupListViewModel(fakeRepo)
    advanceUntilIdle()
    assertEquals(1, viewModel.uiState.value.groups.size)

    fakeRepo.shouldThrowError = true
    viewModel.refreshUIState()
    advanceUntilIdle()

    val state = viewModel.uiState.value
    // Groups are preserved on error to show stale data
    assertEquals(1, state.groups.size)
    assertNotNull(state.errorMsg)

    cleanupFirebaseAuth()
  }

  // =======================================
  // Delete Group Tests
  // =======================================

  @Test
  fun deleteGroup_removesGroupAndRefreshesState() = runTest {
    mockFirebaseAuth()
    val groups =
        listOf(
            Group(id = "1", name = "Group 1", ownerId = testUserId),
            Group(id = "2", name = "Group 2", ownerId = testUserId),
            Group(id = "3", name = "Group 3", ownerId = testUserId))
    fakeRepo.setGroups(groups)
    viewModel = GroupListViewModel(fakeRepo)
    advanceUntilIdle()
    assertEquals(3, viewModel.uiState.value.groups.size)

    var successCalled = false
    viewModel.deleteGroup("2", onSuccess = { successCalled = true })
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals(2, state.groups.size)
    assertEquals("Group 1", state.groups[0].name)
    assertEquals("Group 3", state.groups[1].name)
    assertTrue(successCalled)
    assertNull(state.errorMsg)

    cleanupFirebaseAuth()
  }

  @Test
  fun deleteGroup_setsErrorMessageOnFailure() = runTest {
    mockFirebaseAuth()
    fakeRepo.setGroups(listOf(Group(id = "1", name = "Test", ownerId = "owner1")))
    viewModel = GroupListViewModel(fakeRepo)
    advanceUntilIdle()

    viewModel.deleteGroup("non-existent")
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertNotNull(state.errorMsg)
    assertTrue(state.errorMsg!!.contains("Failed to delete group"))

    cleanupFirebaseAuth()
  }

  // =======================================
  // Leave Group Tests
  // =======================================

  @Test
  fun leaveGroup_removesUserFromMembersAndRefreshesState() = runTest {
    mockFirebaseAuth()
    val group =
        Group(
            id = "1",
            name = "Test Group",
            ownerId = "owner1",
            memberIds = listOf("owner1", testUserId, "user2"))
    fakeRepo.setGroups(listOf(group))
    viewModel = GroupListViewModel(fakeRepo)
    advanceUntilIdle()
    assertEquals(3, viewModel.uiState.value.groups[0].memberIds.size)

    var successCalled = false
    viewModel.leaveGroup("1", onSuccess = { successCalled = true })
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals(2, state.groups[0].memberIds.size)
    assertFalse(state.groups[0].memberIds.contains(testUserId))
    assertTrue(successCalled)
    assertNull(state.errorMsg)

    cleanupFirebaseAuth()
  }

  @Test
  fun leaveGroup_setsErrorMessageOnFailure() = runTest {
    mockFirebaseAuth()
    val group =
        Group(id = "1", name = "Test", ownerId = "owner1", memberIds = listOf("owner1", "user1"))
    fakeRepo.setGroups(listOf(group))
    viewModel = GroupListViewModel(fakeRepo)
    advanceUntilIdle()

    viewModel.leaveGroup("1")
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertNotNull(state.errorMsg)
    assertTrue(state.errorMsg!!.contains("Failed to leave group"))

    cleanupFirebaseAuth()
  }

  @Test
  fun leaveGroup_withNonExistentGroup_callsOnErrorCallback() = runTest {
    mockFirebaseAuth()
    fakeRepo.setGroups(listOf(Group(id = "1", name = "Test", ownerId = "owner1")))
    viewModel = GroupListViewModel(fakeRepo)
    advanceUntilIdle()

    var errorMessage: String? = null
    viewModel.leaveGroup("non-existent", onError = { errorMessage = it })
    advanceUntilIdle()

    assertNotNull(errorMessage)
    assertTrue(errorMessage!!.contains("Failed to leave group"))

    cleanupFirebaseAuth()
  }

  // =======================================
  // Join Group Tests
  // =======================================

  @Test
  fun joinGroup_addsUserToMembersAndRefreshesState() = runTest {
    mockFirebaseAuth()
    val group =
        Group(
            id = "1",
            name = "Test Group",
            ownerId = "owner1",
            memberIds = listOf("owner1", "user2"))
    fakeRepo.setGroups(listOf(group))
    viewModel = GroupListViewModel(fakeRepo)
    advanceUntilIdle()
    assertEquals(2, viewModel.uiState.value.groups[0].memberIds.size)

    var successCalled = false
    viewModel.joinGroup("1", onSuccess = { successCalled = true })
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals(3, state.groups[0].memberIds.size)
    assertTrue(state.groups[0].memberIds.contains(testUserId))
    assertTrue(successCalled)
    assertNull(state.errorMsg)

    cleanupFirebaseAuth()
  }

  @Test
  fun joinGroup_setsErrorMessageOnFailure() = runTest {
    mockFirebaseAuth()
    val group =
        Group(id = "1", name = "Test", ownerId = "owner1", memberIds = listOf("owner1", testUserId))
    fakeRepo.setGroups(listOf(group))
    viewModel = GroupListViewModel(fakeRepo)
    advanceUntilIdle()

    viewModel.joinGroup("1")
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertNotNull(state.errorMsg)
    assertTrue(state.errorMsg!!.contains("Failed to join group"))

    cleanupFirebaseAuth()
  }

  @Test
  fun joinGroup_multipleUsers_addsAllUsersCorrectly() = runTest {
    mockFirebaseAuth()
    val group =
        Group(id = "1", name = "Test Group", ownerId = "owner1", memberIds = listOf("owner1"))
    fakeRepo.setGroups(listOf(group))
    viewModel = GroupListViewModel(fakeRepo)
    advanceUntilIdle()
    assertEquals(1, viewModel.uiState.value.groups[0].memberIds.size)

    // First user joins
    viewModel.joinGroup("1")
    advanceUntilIdle()
    assertEquals(2, viewModel.uiState.value.groups[0].memberIds.size)

    // Simulate another user joining by manually updating the repository
    cleanupFirebaseAuth()
    val secondUserId = "second-user-456"
    mockkStatic(FirebaseAuth::class)
    val mockAuth = mockk<FirebaseAuth>()
    val mockUser = mockk<FirebaseUser>()
    every { FirebaseAuth.getInstance() } returns mockAuth
    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns secondUserId

    val updatedViewModel = GroupListViewModel(fakeRepo)
    advanceUntilIdle()
    updatedViewModel.joinGroup("1")
    advanceUntilIdle()

    val state = updatedViewModel.uiState.value
    assertEquals(3, state.groups[0].memberIds.size)
    assertTrue(state.groups[0].memberIds.contains(testUserId))
    assertTrue(state.groups[0].memberIds.contains(secondUserId))

    cleanupFirebaseAuth()
  }

  @Test
  fun joinGroup_preservesOtherGroupProperties() = runTest {
    mockFirebaseAuth()
    val group =
        Group(
            id = "test-123",
            name = "Sports Group",
            ownerId = "owner1",
            description = "Weekly soccer games",
            memberIds = listOf("owner1"),
            eventIds = listOf("event1", "event2"))
    fakeRepo.setGroups(listOf(group))
    viewModel = GroupListViewModel(fakeRepo)
    advanceUntilIdle()

    viewModel.joinGroup("test-123")
    advanceUntilIdle()

    val state = viewModel.uiState.value
    val updatedGroup = state.groups[0]
    assertEquals("test-123", updatedGroup.id)
    assertEquals("Sports Group", updatedGroup.name)
    assertEquals("owner1", updatedGroup.ownerId)
    assertEquals("Weekly soccer games", updatedGroup.description)
    assertEquals(2, updatedGroup.eventIds.size)
    assertEquals(2, updatedGroup.memberIds.size)

    cleanupFirebaseAuth()
  }
}
