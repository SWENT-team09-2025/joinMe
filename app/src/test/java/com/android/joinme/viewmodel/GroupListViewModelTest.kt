package com.android.joinme.viewmodel

import com.android.joinme.model.group.Group
import com.android.joinme.repository.GroupRepository
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

    fun setGroups(newGroups: List<Group>) {
      groups.clear()
      groups.addAll(newGroups)
    }

    override suspend fun userGroups(): List<Group> {
      if (shouldThrowError) {
        throw Exception(errorMessage)
      }
      return groups.toList()
    }

    override suspend fun leaveGroup(id: String) {
      groups.removeIf { it.id == id }
    }

    override suspend fun getGroup(id: String): Group? {
      return groups.find { it.id == id }
    }
  }

  private lateinit var fakeRepo: FakeGroupRepository
  private lateinit var viewModel: GroupListViewModel
  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    fakeRepo = FakeGroupRepository()
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun init_loadsGroupsAutomatically() = runTest {
    val testGroups =
        listOf(
            Group(id = "1", name = "Group 1", membersCount = 10),
            Group(id = "2", name = "Group 2", membersCount = 20))
    fakeRepo.setGroups(testGroups)

    viewModel = GroupListViewModel(fakeRepo)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals(2, state.groups.size)
    assertEquals("Group 1", state.groups[0].name)
    assertEquals("Group 2", state.groups[1].name)
    assertNull(state.errorMsg)
    assertFalse(state.isLoading)
  }

  @Test
  fun init_withEmptyRepository_returnsEmptyList() = runTest {
    fakeRepo.setGroups(emptyList())

    viewModel = GroupListViewModel(fakeRepo)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertTrue(state.groups.isEmpty())
    assertNull(state.errorMsg)
    assertFalse(state.isLoading)
  }

  @Test
  fun init_withRepositoryError_setsErrorMessage() = runTest {
    fakeRepo.shouldThrowError = true
    fakeRepo.errorMessage = "Failed to load groups"

    viewModel = GroupListViewModel(fakeRepo)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertTrue(state.groups.isEmpty())
    assertEquals("Failed to load groups", state.errorMsg)
    assertFalse(state.isLoading)
  }

  @Test
  fun refreshUIState_reloadsGroups() = runTest {
    fakeRepo.setGroups(listOf(Group(id = "1", name = "Initial Group")))
    viewModel = GroupListViewModel(fakeRepo)
    advanceUntilIdle()

    fakeRepo.setGroups(
        listOf(
            Group(id = "2", name = "Updated Group 1"), Group(id = "3", name = "Updated Group 2")))
    viewModel.refreshUIState()
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals(2, state.groups.size)
    assertEquals("Updated Group 1", state.groups[0].name)
    assertEquals("Updated Group 2", state.groups[1].name)
    assertNull(state.errorMsg)
  }

  @Test
  fun refreshUIState_afterError_canRecover() = runTest {
    fakeRepo.shouldThrowError = true
    viewModel = GroupListViewModel(fakeRepo)
    advanceUntilIdle()
    assertNotNull(viewModel.uiState.value.errorMsg)

    fakeRepo.shouldThrowError = false
    fakeRepo.setGroups(listOf(Group(id = "1", name = "Recovered Group")))
    viewModel.refreshUIState()
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals(1, state.groups.size)
    assertEquals("Recovered Group", state.groups[0].name)
    assertNull(state.errorMsg)
  }

  @Test
  fun refreshUIState_withError_setsErrorMessage() = runTest {
    fakeRepo.setGroups(listOf(Group(id = "1", name = "Initial")))
    viewModel = GroupListViewModel(fakeRepo)
    advanceUntilIdle()

    fakeRepo.shouldThrowError = true
    fakeRepo.errorMessage = "Network error"
    viewModel.refreshUIState()
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertTrue(state.groups.isEmpty())
    assertEquals("Network error", state.errorMsg)
  }

  @Test
  fun multipleRefreshCalls_workCorrectly() = runTest {
    fakeRepo.setGroups(listOf(Group(id = "1", name = "First")))
    viewModel = GroupListViewModel(fakeRepo)
    advanceUntilIdle()

    fakeRepo.setGroups(listOf(Group(id = "2", name = "Second")))
    viewModel.refreshUIState()
    advanceUntilIdle()

    fakeRepo.setGroups(listOf(Group(id = "3", name = "Third")))
    viewModel.refreshUIState()
    advanceUntilIdle()

    fakeRepo.setGroups(listOf(Group(id = "4", name = "Fourth")))
    viewModel.refreshUIState()
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals(1, state.groups.size)
    assertEquals("Fourth", state.groups[0].name)
  }

  @Test
  fun clearErrorMsg_removesErrorMessage() = runTest {
    fakeRepo.shouldThrowError = true
    fakeRepo.errorMessage = "Test error message"
    viewModel = GroupListViewModel(fakeRepo)
    advanceUntilIdle()
    assertNotNull(viewModel.uiState.value.errorMsg)

    viewModel.clearErrorMsg()

    assertNull(viewModel.uiState.value.errorMsg)
  }

  @Test
  fun clearErrorMsg_doesNotAffectOtherState() = runTest {
    fakeRepo.shouldThrowError = true
    viewModel = GroupListViewModel(fakeRepo)
    advanceUntilIdle()

    viewModel.clearErrorMsg()

    val state = viewModel.uiState.value
    assertNull(state.errorMsg)
    assertTrue(state.groups.isEmpty())
    assertFalse(state.isLoading)
  }

  @Test
  fun clearErrorMsg_whenNoError_doesNothing() = runTest {
    fakeRepo.setGroups(listOf(Group(id = "1", name = "Test")))
    viewModel = GroupListViewModel(fakeRepo)
    advanceUntilIdle()
    assertNull(viewModel.uiState.value.errorMsg)

    viewModel.clearErrorMsg()

    val state = viewModel.uiState.value
    assertNull(state.errorMsg)
    assertEquals(1, state.groups.size)
  }

  @Test
  fun loadGroups_preservesAllGroupProperties() = runTest {
    val testGroup =
        Group(
            id = "test-123",
            name = "Test Group",
            category = "Sports",
            description = "A test group for sports",
            membersCount = 42)
    fakeRepo.setGroups(listOf(testGroup))

    viewModel = GroupListViewModel(fakeRepo)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    val loadedGroup = state.groups.first()
    assertEquals("test-123", loadedGroup.id)
    assertEquals("Test Group", loadedGroup.name)
    assertEquals("Sports", loadedGroup.category)
    assertEquals("A test group for sports", loadedGroup.description)
    assertEquals(42, loadedGroup.membersCount)
  }

  @Test
  fun loadGroups_withMultipleGroups_maintainsOrder() = runTest {
    val groups =
        listOf(
            Group(id = "1", name = "First", membersCount = 10),
            Group(id = "2", name = "Second", membersCount = 20),
            Group(id = "3", name = "Third", membersCount = 30))
    fakeRepo.setGroups(groups)

    viewModel = GroupListViewModel(fakeRepo)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals(3, state.groups.size)
    assertEquals("First", state.groups[0].name)
    assertEquals("Second", state.groups[1].name)
    assertEquals("Third", state.groups[2].name)
  }

  @Test
  fun loadGroups_withManyGroups_loadsAll() = runTest {
    val manyGroups = (1..100).map { Group(id = "$it", name = "Group $it", membersCount = it) }
    fakeRepo.setGroups(manyGroups)

    viewModel = GroupListViewModel(fakeRepo)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals(100, state.groups.size)
    assertEquals("Group 1", state.groups[0].name)
    assertEquals("Group 100", state.groups[99].name)
  }

  @Test
  fun loadGroups_withSpecialCharacters_handlesCorrectly() = runTest {
    val groups =
        listOf(
            Group(id = "1", name = "Café & Brunch", membersCount = 5),
            Group(id = "2", name = "Chess Game", membersCount = 10),
            Group(id = "3", name = "Group with emojis", membersCount = 15))
    fakeRepo.setGroups(groups)

    viewModel = GroupListViewModel(fakeRepo)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals(3, state.groups.size)
    assertEquals("Café & Brunch", state.groups[0].name)
    assertEquals("Chess Game", state.groups[1].name)
    assertEquals("Group with emojis", state.groups[2].name)
  }

  @Test
  fun loadGroups_withZeroMembers_handlesCorrectly() = runTest {
    val group = Group(id = "1", name = "Empty Group", membersCount = 0)
    fakeRepo.setGroups(listOf(group))

    viewModel = GroupListViewModel(fakeRepo)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals(0, state.groups[0].membersCount)
  }

  @Test
  fun loadGroups_withEmptyDescription_handlesCorrectly() = runTest {
    val group = Group(id = "1", name = "No Description", description = "", membersCount = 10)
    fakeRepo.setGroups(listOf(group))

    viewModel = GroupListViewModel(fakeRepo)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals("", state.groups[0].description)
  }

  @Test
  fun errorMessage_isPreservedCorrectly() = runTest {
    fakeRepo.shouldThrowError = true
    fakeRepo.errorMessage = "Detailed error: Connection timeout after 30 seconds"

    viewModel = GroupListViewModel(fakeRepo)
    advanceUntilIdle()

    assertEquals(
        "Detailed error: Connection timeout after 30 seconds", viewModel.uiState.value.errorMsg)
  }

  @Test
  fun successfulLoad_afterError_clearsErrorAndSetsGroups() = runTest {
    fakeRepo.shouldThrowError = true
    viewModel = GroupListViewModel(fakeRepo)
    advanceUntilIdle()
    assertNotNull(viewModel.uiState.value.errorMsg)

    fakeRepo.shouldThrowError = false
    fakeRepo.setGroups(listOf(Group(id = "1", name = "Success")))
    viewModel.refreshUIState()
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertNull(state.errorMsg)
    assertEquals(1, state.groups.size)
    assertEquals("Success", state.groups[0].name)
  }

  @Test
  fun uiState_afterLoading_hasConsistentState() = runTest {
    fakeRepo.setGroups(listOf(Group(id = "1", name = "Test")))

    viewModel = GroupListViewModel(fakeRepo)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertNull(state.errorMsg)
    assertNotNull(state.groups)
    assertEquals(1, state.groups.size)
  }

  @Test
  fun uiState_afterError_hasConsistentState() = runTest {
    fakeRepo.shouldThrowError = true

    viewModel = GroupListViewModel(fakeRepo)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertNotNull(state.errorMsg)
    assertTrue(state.groups.isEmpty())
  }

  @Test
  fun refreshUIState_clearsGroupsOnError() = runTest {
    fakeRepo.setGroups(listOf(Group(id = "1", name = "Test")))
    viewModel = GroupListViewModel(fakeRepo)
    advanceUntilIdle()
    assertEquals(1, viewModel.uiState.value.groups.size)

    fakeRepo.shouldThrowError = true
    viewModel.refreshUIState()
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertTrue(state.groups.isEmpty())
    assertNotNull(state.errorMsg)
  }
}
