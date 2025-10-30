package com.android.joinme.ui.groups

import com.android.joinme.model.event.EventType
import com.android.joinme.model.groups.Group
import com.android.joinme.model.groups.GroupRepository
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
class EditGroupViewModelTest {

  private lateinit var viewModel: EditGroupViewModel
  private lateinit var mockRepository: GroupRepository
  private val testDispatcher = StandardTestDispatcher()
  private lateinit var testGroup: Group

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)

    mockRepository = mockk(relaxed = true)

    testGroup =
        Group(
            id = "test-group-123",
            name = "Basketball Team",
            category = EventType.SPORTS,
            description = "Weekly basketball games",
            ownerId = "owner-456",
            memberIds = listOf("owner-456", "member-789"),
            eventIds = listOf("event-1", "event-2"),
            photoUrl = null)

    viewModel = EditGroupViewModel(mockRepository)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
    unmockkAll()
  }

  // =======================================
  // Initial State Tests
  // =======================================

  @Test
  fun `initial state has empty name and description`() = runTest {
    val state = viewModel.uiState.value
    assertEquals("", state.name)
    assertEquals("", state.description)
  }

  @Test
  fun `initial state has default category ACTIVITY`() = runTest {
    val state = viewModel.uiState.value
    assertEquals(EventType.ACTIVITY, state.category)
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
  fun `initial state is not valid`() = runTest {
    val state = viewModel.uiState.value
    assertFalse(state.isValid)
  }

  @Test
  fun `initial state has no editedGroupId`() = runTest {
    val state = viewModel.uiState.value
    assertNull(state.editedGroupId)
  }

  // =======================================
  // Load Group Tests
  // =======================================

  @Test
  fun `loadGroup successfully loads group data`() = runTest {
    coEvery { mockRepository.getGroup(testGroup.id) } returns testGroup

    viewModel.loadGroup(testGroup.id)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals("Basketball Team", state.name)
    assertEquals("Weekly basketball games", state.description)
    assertEquals(EventType.SPORTS, state.category)
    assertFalse(state.isLoading)
    assertNull(state.errorMsg)
    assertTrue(state.isValid)
  }

  @Test
  fun `loadGroup sets loading state while loading`() = runTest {
    coEvery { mockRepository.getGroup(testGroup.id) } coAnswers
        {
          viewModel.uiState.value.let { state -> assertTrue(state.isLoading) }
          testGroup
        }

    viewModel.loadGroup(testGroup.id)
    advanceUntilIdle()

    val finalState = viewModel.uiState.value
    assertFalse(finalState.isLoading)
  }

  @Test
  fun `loadGroup handles group not found exception`() = runTest {
    coEvery { mockRepository.getGroup(testGroup.id) } throws Exception("Group not found")

    viewModel.loadGroup(testGroup.id)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertEquals("Failed to load group: Group not found", state.errorMsg)
  }

  @Test
  fun `loadGroup handles repository exception`() = runTest {
    coEvery { mockRepository.getGroup(testGroup.id) } throws
        RuntimeException("Database connection failed")

    viewModel.loadGroup(testGroup.id)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertEquals("Failed to load group: Database connection failed", state.errorMsg)
  }

  @Test
  fun `loadGroup handles exception without message`() = runTest {
    coEvery { mockRepository.getGroup(testGroup.id) } throws RuntimeException(null as String?)

    viewModel.loadGroup(testGroup.id)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertEquals("Failed to load group: Unknown error", state.errorMsg)
  }

  @Test
  fun `loadGroup with different categories loads correctly`() = runTest {
    val socialGroup = testGroup.copy(category = EventType.SOCIAL)
    coEvery { mockRepository.getGroup(socialGroup.id) } returns socialGroup

    viewModel.loadGroup(socialGroup.id)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals(EventType.SOCIAL, state.category)
  }

  @Test
  fun `loadGroup clears previous errors`() = runTest {
    coEvery { mockRepository.getGroup("bad-id") } throws Exception("Error")
    viewModel.loadGroup("bad-id")
    advanceUntilIdle()

    coEvery { mockRepository.getGroup(testGroup.id) } returns testGroup
    viewModel.loadGroup(testGroup.id)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertNull(state.errorMsg)
    assertEquals("Basketball Team", state.name)
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
  fun `setName with exactly 3 characters is valid`() = runTest {
    viewModel.setName("abc")

    val state = viewModel.uiState.value
    assertNull(state.nameError)
    assertEquals("abc", state.name)
    assertTrue(state.isValid)
  }

  @Test
  fun `setName with long name sets nameError`() = runTest {
    viewModel.setName("a".repeat(31))

    val state = viewModel.uiState.value
    assertEquals("Name must not exceed 30 characters", state.nameError)
    assertFalse(state.isValid)
  }

  @Test
  fun `setName with exactly 30 characters is valid`() = runTest {
    val name = "a".repeat(30)
    viewModel.setName(name)

    val state = viewModel.uiState.value
    assertNull(state.nameError)
    assertEquals(name, state.name)
    assertTrue(state.isValid)
  }

  @Test
  fun `setName with invalid characters sets nameError`() = runTest {
    viewModel.setName("Test@Group!")

    val state = viewModel.uiState.value
    assertEquals("Only letters, numbers, spaces, and underscores allowed", state.nameError)
    assertFalse(state.isValid)
  }

  @Test
  fun `setName with underscores is valid`() = runTest {
    viewModel.setName("Test_Group_Name")

    val state = viewModel.uiState.value
    assertNull(state.nameError)
    assertEquals("Test_Group_Name", state.name)
    assertTrue(state.isValid)
  }

  @Test
  fun `setName with numbers is valid`() = runTest {
    viewModel.setName("Group123")

    val state = viewModel.uiState.value
    assertNull(state.nameError)
    assertEquals("Group123", state.name)
    assertTrue(state.isValid)
  }

  @Test
  fun `setName with single spaces between words is valid`() = runTest {
    viewModel.setName("My Test Group")

    val state = viewModel.uiState.value
    assertNull(state.nameError)
    assertEquals("My Test Group", state.name)
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
  fun `setName trims leading and trailing spaces for validation`() = runTest {
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

  // =======================================
  // Description Validation Tests
  // =======================================

  @Test
  fun `setDescription with empty string is valid`() = runTest {
    viewModel.setName("Valid Group")
    viewModel.setDescription("")

    val state = viewModel.uiState.value
    assertNull(state.descriptionError)
    assertEquals("", state.description)
    assertTrue(state.isValid)
  }

  @Test
  fun `setDescription with valid text is valid`() = runTest {
    viewModel.setName("Valid Group")
    viewModel.setDescription("A great group for basketball")

    val state = viewModel.uiState.value
    assertNull(state.descriptionError)
    assertEquals("A great group for basketball", state.description)
    assertTrue(state.isValid)
  }

  @Test
  fun `setDescription with long text sets descriptionError`() = runTest {
    viewModel.setName("Valid Group")
    viewModel.setDescription("a".repeat(301))

    val state = viewModel.uiState.value
    assertEquals("Description must not exceed 300 characters", state.descriptionError)
    assertFalse(state.isValid)
  }

  @Test
  fun `setDescription with exactly 300 characters is valid`() = runTest {
    viewModel.setName("Valid Group")
    val description = "a".repeat(300)
    viewModel.setDescription(description)

    val state = viewModel.uiState.value
    assertNull(state.descriptionError)
    assertEquals(description, state.description)
    assertTrue(state.isValid)
  }

  // =======================================
  // Category Tests
  // =======================================

  @Test
  fun `setCategory updates category to SOCIAL`() = runTest {
    viewModel.setCategory(EventType.SOCIAL)

    val state = viewModel.uiState.value
    assertEquals(EventType.SOCIAL, state.category)
  }

  @Test
  fun `setCategory updates category to SPORTS`() = runTest {
    viewModel.setCategory(EventType.SPORTS)

    val state = viewModel.uiState.value
    assertEquals(EventType.SPORTS, state.category)
  }

  @Test
  fun `setCategory updates category to ACTIVITY`() = runTest {
    viewModel.setCategory(EventType.ACTIVITY)

    val state = viewModel.uiState.value
    assertEquals(EventType.ACTIVITY, state.category)
  }

  // =======================================
  // Form Validity Tests
  // =======================================

  @Test
  fun `form is valid with valid name and category`() = runTest {
    viewModel.setName("Test Group")
    viewModel.setCategory(EventType.SPORTS)

    val state = viewModel.uiState.value
    assertTrue(state.isValid)
  }

  @Test
  fun `form is valid with valid name, category, and empty description`() = runTest {
    viewModel.setName("Test Group")
    viewModel.setCategory(EventType.ACTIVITY)
    viewModel.setDescription("")

    val state = viewModel.uiState.value
    assertTrue(state.isValid)
  }

  @Test
  fun `form is valid with valid name, category, and description`() = runTest {
    viewModel.setName("Test Group")
    viewModel.setCategory(EventType.SOCIAL)
    viewModel.setDescription("A social group")

    val state = viewModel.uiState.value
    assertTrue(state.isValid)
  }

  @Test
  fun `form is invalid when name is blank`() = runTest {
    viewModel.setName("")
    viewModel.setCategory(EventType.SPORTS)

    val state = viewModel.uiState.value
    assertFalse(state.isValid)
  }

  @Test
  fun `form is invalid when name is too short`() = runTest {
    viewModel.setName("ab")
    viewModel.setCategory(EventType.SPORTS)

    val state = viewModel.uiState.value
    assertFalse(state.isValid)
  }

  @Test
  fun `form is invalid when name is too long`() = runTest {
    viewModel.setName("a".repeat(31))
    viewModel.setCategory(EventType.SPORTS)

    val state = viewModel.uiState.value
    assertFalse(state.isValid)
  }

  @Test
  fun `form is invalid when description is too long`() = runTest {
    viewModel.setName("Valid Group")
    viewModel.setCategory(EventType.ACTIVITY)
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

  // =======================================
  // Update Group Success Tests
  // =======================================

  @Test
  fun `updateGroup successfully updates group`() = runTest {
    coEvery { mockRepository.getGroup(testGroup.id) } returns testGroup
    coEvery { mockRepository.editGroup(testGroup.id, any()) } just Runs

    viewModel.setName("Updated Name")
    viewModel.setCategory(EventType.SOCIAL)
    viewModel.setDescription("Updated description")
    viewModel.updateGroup(testGroup.id)

    advanceUntilIdle()

    coVerify {
      mockRepository.editGroup(
          testGroup.id,
          match {
            it.name == "Updated Name" &&
                it.category == EventType.SOCIAL &&
                it.description == "Updated description" &&
                it.id == testGroup.id
          })
    }

    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertNull(state.errorMsg)
    assertEquals(testGroup.id, state.editedGroupId)
  }

  @Test
  fun `updateGroup trims name before updating`() = runTest {
    coEvery { mockRepository.getGroup(testGroup.id) } returns testGroup
    coEvery { mockRepository.editGroup(testGroup.id, any()) } just Runs

    viewModel.setName("  Trimmed Name  ")
    viewModel.setCategory(EventType.ACTIVITY)
    viewModel.updateGroup(testGroup.id)

    advanceUntilIdle()

    coVerify { mockRepository.editGroup(testGroup.id, match { it.name == "Trimmed Name" }) }
  }

  @Test
  fun `updateGroup with blank description sends empty string`() = runTest {
    coEvery { mockRepository.getGroup(testGroup.id) } returns testGroup
    coEvery { mockRepository.editGroup(testGroup.id, any()) } just Runs

    viewModel.setName("Test Group")
    viewModel.setCategory(EventType.SPORTS)
    viewModel.setDescription("   ")
    viewModel.updateGroup(testGroup.id)

    advanceUntilIdle()

    coVerify { mockRepository.editGroup(testGroup.id, match { it.description == "" }) }
  }

  @Test
  fun `updateGroup sets loading state while updating`() = runTest {
    coEvery { mockRepository.getGroup(testGroup.id) } returns testGroup
    coEvery { mockRepository.editGroup(testGroup.id, any()) } coAnswers
        {
          viewModel.uiState.value.let { state -> assertTrue(state.isLoading) }
        }

    viewModel.setName("Test Name")
    viewModel.setCategory(EventType.SPORTS)
    viewModel.updateGroup(testGroup.id)

    advanceUntilIdle()

    val finalState = viewModel.uiState.value
    assertFalse(finalState.isLoading)
  }

  @Test
  fun `updateGroup preserves other group properties`() = runTest {
    coEvery { mockRepository.getGroup(testGroup.id) } returns testGroup
    coEvery { mockRepository.editGroup(testGroup.id, any()) } just Runs

    viewModel.setName("New Name")
    viewModel.setCategory(EventType.ACTIVITY)
    viewModel.setDescription("New description")
    viewModel.updateGroup(testGroup.id)

    advanceUntilIdle()

    coVerify {
      mockRepository.editGroup(
          testGroup.id,
          match {
            it.ownerId == testGroup.ownerId &&
                it.memberIds == testGroup.memberIds &&
                it.eventIds == testGroup.eventIds &&
                it.photoUrl == testGroup.photoUrl
          })
    }
  }

  @Test
  fun `updateGroup with different categories works correctly`() = runTest {
    val categories = listOf(EventType.SOCIAL, EventType.ACTIVITY, EventType.SPORTS)

    categories.forEach { category ->
      coEvery { mockRepository.getGroup(testGroup.id) } returns testGroup
      coEvery { mockRepository.editGroup(testGroup.id, any()) } just Runs

      viewModel.setName("Test Group")
      viewModel.setCategory(category)
      viewModel.updateGroup(testGroup.id)

      advanceUntilIdle()

      coVerify { mockRepository.editGroup(testGroup.id, match { it.category == category }) }

      viewModel.clearSuccessState()
    }
  }

  // =======================================
  // Update Group Error Tests
  // =======================================

  @Test
  fun `updateGroup does not call repository when form is invalid`() = runTest {
    viewModel.setName("ab")
    viewModel.setCategory(EventType.SOCIAL)
    viewModel.updateGroup(testGroup.id)

    advanceUntilIdle()

    coVerify(exactly = 0) { mockRepository.getGroup(any()) }
    coVerify(exactly = 0) { mockRepository.editGroup(any(), any()) }
  }

  @Test
  fun `updateGroup handles repository exception during getGroup`() = runTest {
    coEvery { mockRepository.getGroup(testGroup.id) } throws RuntimeException("Network error")

    viewModel.setName("Test Name")
    viewModel.setCategory(EventType.ACTIVITY)
    viewModel.updateGroup(testGroup.id)

    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertEquals("Failed to update group: Network error", state.errorMsg)
    assertNull(state.editedGroupId)

    coVerify(exactly = 0) { mockRepository.editGroup(any(), any()) }
  }

  @Test
  fun `updateGroup handles repository exception during editGroup`() = runTest {
    coEvery { mockRepository.getGroup(testGroup.id) } returns testGroup
    coEvery { mockRepository.editGroup(testGroup.id, any()) } throws
        RuntimeException("Update failed")

    viewModel.setName("Test Name")
    viewModel.setCategory(EventType.SPORTS)
    viewModel.updateGroup(testGroup.id)

    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertEquals("Failed to update group: Update failed", state.errorMsg)
    assertNull(state.editedGroupId)
  }

  @Test
  fun `updateGroup handles exception without message`() = runTest {
    coEvery { mockRepository.getGroup(testGroup.id) } returns testGroup
    coEvery { mockRepository.editGroup(testGroup.id, any()) } throws
        RuntimeException(null as String?)

    viewModel.setName("Test Name")
    viewModel.setCategory(EventType.SOCIAL)
    viewModel.updateGroup(testGroup.id)

    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertEquals("Failed to update group: Unknown error", state.errorMsg)
    assertNull(state.editedGroupId)
  }

  @Test
  fun `updateGroup clears previous errors`() = runTest {
    coEvery { mockRepository.getGroup("bad-id") } throws Exception("Error")
    viewModel.setName("Test Name")
    viewModel.updateGroup("bad-id")
    advanceUntilIdle()

    coEvery { mockRepository.getGroup(testGroup.id) } returns testGroup
    coEvery { mockRepository.editGroup(testGroup.id, any()) } just Runs
    viewModel.updateGroup(testGroup.id)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertNull(state.errorMsg)
    assertEquals(testGroup.id, state.editedGroupId)
  }

  // =======================================
  // Integration Tests - Load and Update
  // =======================================

  @Test
  fun `loadGroup then updateGroup preserves loaded data and applies changes`() = runTest {
    coEvery { mockRepository.getGroup(testGroup.id) } returns testGroup
    coEvery { mockRepository.editGroup(testGroup.id, any()) } just Runs

    viewModel.loadGroup(testGroup.id)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals("Basketball Team", state.name)
    assertEquals(EventType.SPORTS, state.category)

    viewModel.setName("Updated Basketball Team")
    viewModel.updateGroup(testGroup.id)
    advanceUntilIdle()

    coVerify {
      mockRepository.editGroup(
          testGroup.id,
          match {
            it.name == "Updated Basketball Team" &&
                it.category == EventType.SPORTS &&
                it.description == "Weekly basketball games"
          })
    }
  }

  @Test
  fun `form validity updates correctly after loading group`() = runTest {
    coEvery { mockRepository.getGroup(testGroup.id) } returns testGroup

    var state = viewModel.uiState.value
    assertFalse(state.isValid)

    viewModel.loadGroup(testGroup.id)
    advanceUntilIdle()

    state = viewModel.uiState.value
    assertTrue(state.isValid)
  }

  @Test
  fun `modifying loaded group data maintains validity`() = runTest {
    coEvery { mockRepository.getGroup(testGroup.id) } returns testGroup

    viewModel.loadGroup(testGroup.id)
    advanceUntilIdle()

    viewModel.setName("Modified Name")

    val state = viewModel.uiState.value
    assertTrue(state.isValid)
  }

  @Test
  fun `modifying loaded group data to invalid value updates validity`() = runTest {
    coEvery { mockRepository.getGroup(testGroup.id) } returns testGroup

    viewModel.loadGroup(testGroup.id)
    advanceUntilIdle()

    viewModel.setName("ab")

    val state = viewModel.uiState.value
    assertFalse(state.isValid)
  }

  // =======================================
  // State Management Tests
  // =======================================

  @Test
  fun `clearErrorMsg clears error message`() = runTest {
    coEvery { mockRepository.getGroup(testGroup.id) } throws Exception("Test error")

    viewModel.loadGroup(testGroup.id)
    advanceUntilIdle()

    viewModel.clearErrorMsg()

    val state = viewModel.uiState.value
    assertNull(state.errorMsg)
  }

  @Test
  fun `clearSuccessState clears editedGroupId`() = runTest {
    coEvery { mockRepository.getGroup(testGroup.id) } returns testGroup
    coEvery { mockRepository.editGroup(testGroup.id, any()) } just Runs

    viewModel.setName("Test Group")
    viewModel.setCategory(EventType.SPORTS)
    viewModel.updateGroup(testGroup.id)
    advanceUntilIdle()

    viewModel.clearSuccessState()

    val state = viewModel.uiState.value
    assertNull(state.editedGroupId)
  }

  @Test
  fun `setName updates validation immediately`() = runTest {
    viewModel.setName("ab")
    var state = viewModel.uiState.value
    assertNotNull(state.nameError)
    assertFalse(state.isValid)

    viewModel.setName("Valid Name")
    state = viewModel.uiState.value
    assertNull(state.nameError)
    assertTrue(state.isValid)
  }

  @Test
  fun `setDescription updates validation immediately`() = runTest {
    viewModel.setName("Valid Name")
    viewModel.setCategory(EventType.SPORTS)
    viewModel.setDescription("a".repeat(301))

    var state = viewModel.uiState.value
    assertNotNull(state.descriptionError)
    assertFalse(state.isValid)

    viewModel.setDescription("Valid description")
    state = viewModel.uiState.value
    assertNull(state.descriptionError)
    assertTrue(state.isValid)
  }
}
