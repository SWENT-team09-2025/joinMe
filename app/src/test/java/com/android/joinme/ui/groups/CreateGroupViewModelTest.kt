package com.android.joinme.ui.groups

import com.android.joinme.model.event.EventType
import com.android.joinme.model.group.GroupRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import io.mockk.*
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

@OptIn(ExperimentalCoroutinesApi::class)
class CreateGroupViewModelTest {

  private lateinit var viewModel: CreateGroupViewModel
  private lateinit var mockRepository: GroupRepository
  private lateinit var mockAuth: FirebaseAuth
  private lateinit var mockUser: FirebaseUser
  private val testDispatcher = StandardTestDispatcher()
  private val testUid = "test-user-123"

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)

    mockRepository = mockk(relaxed = true)
    mockAuth = mockk(relaxed = true)
    mockUser = mockk(relaxed = true)

    mockkStatic(FirebaseAuth::class)
    every { Firebase.auth } returns mockAuth
    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns testUid

    viewModel = CreateGroupViewModel(mockRepository)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
    unmockkAll()
  }

  // =======================================
  // Name Validation Tests
  // =======================================

  @Test
  fun `setName with blank input sets nameError`() = runTest {
    viewModel.setName("")

    val state = viewModel.uiState.value
    assertEquals("Name is required", state.nameError)
    assertFalse(state.isValid)
  }

  @Test
  fun `setName with short name sets nameError`() = runTest {
    viewModel.setName("ab")

    val state = viewModel.uiState.value
    assertEquals("Name must be at least 3 characters", state.nameError)
    assertFalse(state.isValid)
  }

  @Test
  fun `setName with long name sets nameError`() = runTest {
    viewModel.setName("a".repeat(31))

    val state = viewModel.uiState.value
    assertEquals("Name must not exceed 30 characters", state.nameError)
    assertFalse(state.isValid)
  }

  @Test
  fun `setName with invalid characters sets nameError`() = runTest {
    viewModel.setName("Test@Group!")

    val state = viewModel.uiState.value
    assertEquals("Only letters, numbers, spaces, and underscores allowed", state.nameError)
    assertFalse(state.isValid)
  }

  @Test
  fun `setName with valid name clears nameError`() = runTest {
    viewModel.setName("Valid Group Name")

    val state = viewModel.uiState.value
    assertNull(state.nameError)
    assertEquals("Valid Group Name", state.name)
  }

  @Test
  fun `setName with underscores and numbers is valid`() = runTest {
    viewModel.setName("Test_Group_123")

    val state = viewModel.uiState.value
    assertNull(state.nameError)
    assertEquals("Test_Group_123", state.name)
  }

  @Test
  fun `setName trims leading spaces`() = runTest {
    viewModel.setName("   Valid Name")

    val state = viewModel.uiState.value
    assertEquals("Valid Name", state.name)
    assertNull(state.nameError)
    assertTrue(state.isValid)
  }

  @Test
  fun `setName trims trailing spaces`() = runTest {
    viewModel.setName("Valid Name   ")

    val state = viewModel.uiState.value
    assertEquals("Valid Name", state.name)
    assertNull(state.nameError)
    assertTrue(state.isValid)
  }

  @Test
  fun `setName trims both leading and trailing spaces`() = runTest {
    viewModel.setName("   Valid Name   ")

    val state = viewModel.uiState.value
    assertEquals("Valid Name", state.name)
    assertNull(state.nameError)
    assertTrue(state.isValid)
  }

  @Test
  fun `setName with only spaces becomes blank after trim`() = runTest {
    viewModel.setName("     ")

    val state = viewModel.uiState.value
    assertEquals("", state.name)
    assertEquals("Name is required", state.nameError)
    assertFalse(state.isValid)
  }

  @Test
  fun `setName with leading spaces that makes name too short after trim`() = runTest {
    viewModel.setName("   ab")

    val state = viewModel.uiState.value
    assertEquals("ab", state.name)
    assertEquals("Name must be at least 3 characters", state.nameError)
    assertFalse(state.isValid)
  }

  @Test
  fun `setName with spaces preserves single spaces between words`() = runTest {
    viewModel.setName("My Group Name")

    val state = viewModel.uiState.value
    assertEquals("My Group Name", state.name)
    assertNull(state.nameError)
    assertTrue(state.isValid)
  }

  @Test
  fun `setName with multiple consecutive spaces sets error`() = runTest {
    viewModel.setName("My  Group")

    val state = viewModel.uiState.value
    assertEquals("My  Group", state.name)
    assertEquals("Multiple consecutive spaces not allowed", state.nameError)
    assertFalse(state.isValid)
  }

  @Test
  fun `setName with three consecutive spaces sets error`() = runTest {
    viewModel.setName("My   Group")

    val state = viewModel.uiState.value
    assertEquals("My   Group", state.name)
    assertEquals("Multiple consecutive spaces not allowed", state.nameError)
    assertFalse(state.isValid)
  }

  @Test
  fun `setName with multiple spaces in different positions sets error`() = runTest {
    viewModel.setName("My  Group  Name")

    val state = viewModel.uiState.value
    assertEquals("My  Group  Name", state.name)
    assertEquals("Multiple consecutive spaces not allowed", state.nameError)
    assertFalse(state.isValid)
  }

  @Test
  fun `setName validates multiple spaces before other rules`() = runTest {
    viewModel.setName("My  Group@Name")

    val state = viewModel.uiState.value
    // Should catch multiple spaces error first
    assertEquals("Multiple consecutive spaces not allowed", state.nameError)
    assertFalse(state.isValid)
  }

  // =======================================
  // Category Tests
  // =======================================

  @Test
  fun `setCategory with SOCIAL updates category`() = runTest {
    viewModel.setCategory(EventType.SOCIAL)

    val state = viewModel.uiState.value
    assertEquals(EventType.SOCIAL, state.category)
  }

  @Test
  fun `setCategory with SPORTS updates category`() = runTest {
    viewModel.setCategory(EventType.SPORTS)

    val state = viewModel.uiState.value
    assertEquals(EventType.SPORTS, state.category)
  }

  @Test
  fun `setCategory with ACTIVITY updates category`() = runTest {
    viewModel.setCategory(EventType.ACTIVITY)

    val state = viewModel.uiState.value
    assertEquals(EventType.ACTIVITY, state.category)
  }

  @Test
  fun `default category is ACTIVITY`() = runTest {
    val state = viewModel.uiState.value
    assertEquals(EventType.ACTIVITY, state.category)
  }

  // =======================================
  // Description Validation Tests
  // =======================================

  @Test
  fun `setDescription with empty string is valid`() = runTest {
    viewModel.setDescription("")

    val state = viewModel.uiState.value
    assertNull(state.descriptionError)
    assertEquals("", state.description)
  }

  @Test
  fun `setDescription with long text sets descriptionError`() = runTest {
    viewModel.setDescription("a".repeat(301))

    val state = viewModel.uiState.value
    assertEquals("Description must not exceed 300 characters", state.descriptionError)
    assertFalse(state.isValid)
  }

  @Test
  fun `setDescription with 300 characters is valid`() = runTest {
    val description = "a".repeat(300)
    viewModel.setDescription(description)

    val state = viewModel.uiState.value
    assertNull(state.descriptionError)
    assertEquals(description, state.description)
  }

  @Test
  fun `setDescription with valid text clears descriptionError`() = runTest {
    viewModel.setDescription("A valid description")

    val state = viewModel.uiState.value
    assertNull(state.descriptionError)
    assertEquals("A valid description", state.description)
  }

  // =======================================
  // Form Validity Tests
  // =======================================

  @Test
  fun `form is invalid initially`() = runTest {
    val state = viewModel.uiState.value
    assertFalse(state.isValid)
  }

  @Test
  fun `form becomes valid when name is valid`() = runTest {
    viewModel.setName("Test Group")

    val state = viewModel.uiState.value
    assertTrue(state.isValid)
  }

  @Test
  fun `form is valid with empty description`() = runTest {
    viewModel.setName("Test Group")
    viewModel.setCategory(EventType.ACTIVITY)
    viewModel.setDescription("")

    val state = viewModel.uiState.value
    assertTrue(state.isValid)
  }

  @Test
  fun `form is invalid when name has error`() = runTest {
    viewModel.setName("ab")
    viewModel.setCategory(EventType.SPORTS)

    val state = viewModel.uiState.value
    assertFalse(state.isValid)
  }

  @Test
  fun `form is invalid when description has error`() = runTest {
    viewModel.setName("Valid Name")
    viewModel.setCategory(EventType.SOCIAL)
    viewModel.setDescription("a".repeat(301))

    val state = viewModel.uiState.value
    assertFalse(state.isValid)
  }

  @Test
  fun `form validity updates when switching between valid and invalid name`() = runTest {
    viewModel.setName("Valid Name")
    var state = viewModel.uiState.value
    assertTrue(state.isValid)

    viewModel.setName("ab")
    state = viewModel.uiState.value
    assertFalse(state.isValid)

    viewModel.setName("Another Valid Name")
    state = viewModel.uiState.value
    assertTrue(state.isValid)
  }

  @Test
  fun `form is invalid when name has multiple consecutive spaces`() = runTest {
    viewModel.setName("My  Group")
    viewModel.setCategory(EventType.SOCIAL)

    val state = viewModel.uiState.value
    assertFalse(state.isValid)
    assertEquals("Multiple consecutive spaces not allowed", state.nameError)
  }

  @Test
  fun `setName with 30 chars after trimming is valid`() = runTest {
    val name = "  " + "a".repeat(30) + "  "
    viewModel.setName(name)

    val state = viewModel.uiState.value
    assertEquals("a".repeat(30), state.name)
    assertNull(state.nameError)
    assertTrue(state.isValid)
  }

  @Test
  fun `setName with 31 chars after trimming is invalid`() = runTest {
    val name = "  " + "a".repeat(31) + "  "
    viewModel.setName(name)

    val state = viewModel.uiState.value
    assertEquals("a".repeat(31), state.name)
    assertEquals("Name must not exceed 30 characters", state.nameError)
    assertFalse(state.isValid)
  }

  // =======================================
  // Create Group Success Tests
  // =======================================

  @Test
  fun `createGroup calls repository with correct group data`() = runTest {
    val groupId = "test-group-id"
    every { mockRepository.getNewGroupId() } returns groupId
    coEvery { mockRepository.addGroup(any()) } just Runs

    viewModel.setName("Test Group")
    viewModel.setCategory(EventType.SOCIAL)
    viewModel.setDescription("Test description")
    viewModel.createGroup()

    advanceUntilIdle()

    coVerify {
      mockRepository.addGroup(
          match {
            it.id == groupId &&
                it.name == "Test Group" &&
                it.category == EventType.SOCIAL &&
                it.description == "Test description" &&
                it.ownerId == testUid &&
                it.memberIds == listOf(testUid)
          })
    }
  }

  @Test
  fun `createGroup trims name before creating group`() = runTest {
    val groupId = "test-group-id"
    every { mockRepository.getNewGroupId() } returns groupId
    coEvery { mockRepository.addGroup(any()) } just Runs

    viewModel.setName("  Trimmed Name  ")
    viewModel.setCategory(EventType.ACTIVITY)
    viewModel.createGroup()

    advanceUntilIdle()

    coVerify { mockRepository.addGroup(match { it.name == "Trimmed Name" }) }
  }

  @Test
  fun `createGroup with blank description sends empty string`() = runTest {
    val groupId = "test-group-id"
    every { mockRepository.getNewGroupId() } returns groupId
    coEvery { mockRepository.addGroup(any()) } just Runs

    viewModel.setName("Test Group")
    viewModel.setCategory(EventType.SPORTS)
    viewModel.setDescription("   ")
    viewModel.createGroup()

    advanceUntilIdle()

    coVerify { mockRepository.addGroup(match { it.description == "" }) }
  }

  @Test
  fun `createGroup with default category uses ACTIVITY`() = runTest {
    val groupId = "test-group-id"
    every { mockRepository.getNewGroupId() } returns groupId
    coEvery { mockRepository.addGroup(any()) } just Runs

    viewModel.setName("Test Group")
    viewModel.createGroup()

    advanceUntilIdle()

    coVerify { mockRepository.addGroup(match { it.category == EventType.ACTIVITY }) }
  }

  @Test
  fun `createGroup sets success state with groupId on success`() = runTest {
    val groupId = "test-group-id-123"
    every { mockRepository.getNewGroupId() } returns groupId
    coEvery { mockRepository.addGroup(any()) } just Runs

    viewModel.setName("Success Group")
    viewModel.setCategory(EventType.SOCIAL)
    viewModel.createGroup()

    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertEquals(groupId, state.createdGroupId)
    assertNull(state.errorMsg)
  }

  // =======================================
  // Create Group Error Tests
  // =======================================

  @Test
  fun `createGroup does not call repository when form is invalid`() = runTest {
    viewModel.setName("ab")
    viewModel.setCategory(EventType.SOCIAL)
    viewModel.createGroup()

    advanceUntilIdle()

    coVerify(exactly = 0) { mockRepository.getNewGroupId() }
    coVerify(exactly = 0) { mockRepository.addGroup(any()) }
  }

  @Test
  fun `createGroup handles unauthenticated user`() = runTest {
    every { mockAuth.currentUser } returns null

    viewModel.setName("Test Group")
    viewModel.setCategory(EventType.ACTIVITY)
    viewModel.createGroup()

    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertEquals("You must be logged in to create a group", state.errorMsg)
    assertNull(state.createdGroupId)
  }

  @Test
  fun `createGroup handles repository exception`() = runTest {
    every { mockRepository.getNewGroupId() } returns "group-id"
    coEvery { mockRepository.addGroup(any()) } throws RuntimeException("Network timeout")

    viewModel.setName("Test Group")
    viewModel.setCategory(EventType.SPORTS)
    viewModel.createGroup()

    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertEquals("Failed to create group: Network timeout", state.errorMsg)
    assertNull(state.createdGroupId)
  }

  @Test
  fun `createGroup handles exception without message`() = runTest {
    every { mockRepository.getNewGroupId() } returns "group-id"
    coEvery { mockRepository.addGroup(any()) } throws RuntimeException(null as String?)

    viewModel.setName("Test Group")
    viewModel.setCategory(EventType.SOCIAL)
    viewModel.createGroup()

    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertEquals("Failed to create group: Unknown error", state.errorMsg)
    assertNull(state.createdGroupId)
  }

  @Test
  fun `createGroup with different categories all work correctly`() = runTest {
    val categories = listOf(EventType.SOCIAL, EventType.ACTIVITY, EventType.SPORTS)

    categories.forEachIndexed { index, category ->
      val groupId = "group-$index"
      every { mockRepository.getNewGroupId() } returns groupId
      coEvery { mockRepository.addGroup(any()) } just Runs

      viewModel.setName("Test Group $index")
      viewModel.setCategory(category)
      viewModel.createGroup()
      advanceUntilIdle()

      coVerify {
        mockRepository.addGroup(match { it.name == "Test Group $index" && it.category == category })
      }

      viewModel.clearSuccessState()
    }
  }

  // =======================================
  // State Management Tests
  // =======================================

  @Test
  fun `clearErrorMsg clears error message`() = runTest {
    every { mockRepository.getNewGroupId() } returns "group-id"
    coEvery { mockRepository.addGroup(any()) } throws RuntimeException("Test error")

    viewModel.setName("Test Group")
    viewModel.setCategory(EventType.ACTIVITY)
    viewModel.createGroup()
    advanceUntilIdle()

    viewModel.clearErrorMsg()

    val state = viewModel.uiState.value
    assertNull(state.errorMsg)
  }

  @Test
  fun `clearSuccessState clears createdGroupId`() = runTest {
    val groupId = "test-id"
    every { mockRepository.getNewGroupId() } returns groupId
    coEvery { mockRepository.addGroup(any()) } just Runs

    viewModel.setName("Test Group")
    viewModel.setCategory(EventType.SPORTS)
    viewModel.createGroup()
    advanceUntilIdle()

    viewModel.clearSuccessState()

    val state = viewModel.uiState.value
    assertNull(state.createdGroupId)
  }

  @Test
  fun `initial state is not loading`() = runTest {
    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
  }

  @Test
  fun `initial state has no errors`() = runTest {
    val state = viewModel.uiState.value
    assertNull(state.nameError)
    assertNull(state.descriptionError)
    assertNull(state.errorMsg)
  }

  @Test
  fun `initial state has no success`() = runTest {
    val state = viewModel.uiState.value
    assertNull(state.createdGroupId)
  }

  @Test
  fun `initial state has empty name and description`() = runTest {
    val state = viewModel.uiState.value
    assertEquals("", state.name)
    assertEquals("", state.description)
  }
}
