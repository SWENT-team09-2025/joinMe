package com.android.joinme.ui.profile

import com.android.joinme.model.authentification.AuthRepository
import com.android.joinme.model.profile.Profile
import com.android.joinme.model.profile.ProfileRepository
import com.google.firebase.Timestamp
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

  private val testDispatcher = StandardTestDispatcher()

  private lateinit var mockProfileRepository: ProfileRepository
  private lateinit var mockAuthRepository: AuthRepository
  private lateinit var viewModel: ProfileViewModel

  private val testProfile =
      Profile(
          uid = "test-uid",
          username = "TestUser",
          email = "test@example.com",
          dateOfBirth = "01/01/2000",
          country = "TestCountry",
          interests = listOf("Testing", "Coding"),
          bio = "Test bio",
          createdAt = Timestamp.now(),
          updatedAt = Timestamp.now())

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    mockProfileRepository = mockk(relaxed = true)
    mockAuthRepository = mockk(relaxed = true)
    viewModel = ProfileViewModel(mockProfileRepository, mockAuthRepository)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
    clearAllMocks()
  }

  // ==================== STATE MANAGEMENT TESTS ====================

  @Test
  fun `clearError clears error state`() {
    viewModel.setError("Test error")
    viewModel.clearError()
    assertNull(viewModel.error.value)
  }

  @Test
  fun `setError sets error message`() {
    viewModel.setError("Error message")
    assertEquals("Error message", viewModel.error.value)
  }

  @Test
  fun `clearProfile clears profile state`() {
    viewModel.clearProfile()
    assertNull(viewModel.profile.value)
  }

  // ==================== LOAD PROFILE TESTS ====================

  @Test
  fun `loadProfile sets loading state correctly`() = runTest {
    coEvery { mockProfileRepository.getProfile(any()) } returns testProfile

    viewModel.loadProfile("test-uid")
    testScheduler.advanceUntilIdle()

    assertFalse(viewModel.isLoading.value)
  }

  @Test
  fun `loadProfile loads existing profile successfully`() = runTest {
    coEvery { mockProfileRepository.getProfile("test-uid") } returns testProfile

    viewModel.loadProfile("test-uid")
    testScheduler.advanceUntilIdle()

    assertEquals(testProfile, viewModel.profile.value)
    assertNull(viewModel.error.value)
    coVerify { mockProfileRepository.getProfile("test-uid") }
  }

  @Test
  fun `loadProfile creates new profile when profile does not exist`() = runTest {
    coEvery { mockProfileRepository.getProfile("test-uid") } returns null
    coEvery { mockAuthRepository.getCurrentUserEmail() } returns "new@example.com"
    coEvery { mockProfileRepository.createOrUpdateProfile(any()) } just Runs

    viewModel.loadProfile("test-uid")
    testScheduler.advanceUntilIdle()

    assertNotNull(viewModel.profile.value)
    assertEquals("test-uid", viewModel.profile.value?.uid)
    assertEquals("new@example.com", viewModel.profile.value?.email)
    assertEquals("new", viewModel.profile.value?.username)
    coVerify { mockProfileRepository.createOrUpdateProfile(any()) }
  }

  @Test
  fun `loadProfile handles NoSuchElementException and bootstraps profile`() = runTest {
    coEvery { mockProfileRepository.getProfile("test-uid") } throws NoSuchElementException()
    coEvery { mockAuthRepository.getCurrentUserEmail() } returns "test@example.com"
    coEvery { mockProfileRepository.createOrUpdateProfile(any()) } just Runs

    viewModel.loadProfile("test-uid")
    testScheduler.advanceUntilIdle()

    assertNotNull(viewModel.profile.value)
    assertEquals("test-uid", viewModel.profile.value?.uid)
    coVerify { mockProfileRepository.createOrUpdateProfile(any()) }
  }

  @Test
  fun `loadProfile handles error when email retrieval fails`() = runTest {
    coEvery { mockProfileRepository.getProfile("test-uid") } returns null
    coEvery { mockAuthRepository.getCurrentUserEmail() } throws Exception("Auth error")
    coEvery { mockProfileRepository.createOrUpdateProfile(any()) } just Runs

    viewModel.loadProfile("test-uid")
    testScheduler.advanceUntilIdle()

    // Should still create profile with empty email
    assertNotNull(viewModel.profile.value)
    assertEquals("", viewModel.profile.value?.email)
  }

  @Test
  fun `loadProfile sets error on general exception`() = runTest {
    coEvery { mockProfileRepository.getProfile("test-uid") } throws Exception("Database error")

    viewModel.loadProfile("test-uid")
    testScheduler.advanceUntilIdle()

    assertNull(viewModel.profile.value)
    assertTrue(viewModel.error.value?.contains("Failed to load profile") == true)
  }

  // ==================== CREATE/UPDATE PROFILE TESTS ====================

  @Test
  fun `createOrUpdateProfile updates profile successfully`() = runTest {
    coEvery { mockProfileRepository.createOrUpdateProfile(testProfile) } just Runs

    viewModel.createOrUpdateProfile(testProfile)
    testScheduler.advanceUntilIdle()

    assertEquals(testProfile, viewModel.profile.value)
    assertNull(viewModel.error.value)
    assertFalse(viewModel.isLoading.value)
    coVerify { mockProfileRepository.createOrUpdateProfile(testProfile) }
  }

  @Test
  fun `createOrUpdateProfile sets error on exception`() = runTest {
    coEvery { mockProfileRepository.createOrUpdateProfile(testProfile) } throws
        Exception("Save failed")

    viewModel.createOrUpdateProfile(testProfile)
    testScheduler.advanceUntilIdle()

    assertTrue(viewModel.error.value?.contains("Failed to save profile") == true)
    assertFalse(viewModel.isLoading.value)
  }

  // ==================== DELETE PROFILE TESTS ====================

  @Test
  fun `deleteProfile deletes profile successfully`() = runTest {
    coEvery { mockProfileRepository.deleteProfile("test-uid") } just Runs

    viewModel.deleteProfile("test-uid")
    testScheduler.advanceUntilIdle()

    assertNull(viewModel.profile.value)
    assertNull(viewModel.error.value)
    assertFalse(viewModel.isLoading.value)
    coVerify { mockProfileRepository.deleteProfile("test-uid") }
  }

  @Test
  fun `deleteProfile sets error on exception`() = runTest {
    coEvery { mockProfileRepository.deleteProfile("test-uid") } throws Exception("Delete failed")

    viewModel.deleteProfile("test-uid")
    testScheduler.advanceUntilIdle()

    assertTrue(viewModel.error.value?.contains("Failed to delete profile") == true)
    assertFalse(viewModel.isLoading.value)
  }

  // ==================== USERNAME VALIDATION TESTS ====================

  @Test
  fun `getUsernameError returns error for empty username`() {
    assertEquals("Username is required", viewModel.getUsernameError(""))
  }

  @Test
  fun `getUsernameError returns error for short username`() {
    assertEquals("Username must be at least 3 characters", viewModel.getUsernameError("ab"))
    assertEquals("Username must be at least 3 characters", viewModel.getUsernameError("a"))
  }

  @Test
  fun `getUsernameError returns error for long username`() {
    val longName = "a".repeat(31)
    assertEquals("Username must not exceed 30 characters", viewModel.getUsernameError(longName))
  }

  @Test
  fun `getUsernameError returns error for invalid characters`() {
    assertEquals(
        "Only letters, numbers, spaces, and underscores allowed",
        viewModel.getUsernameError("user@name"))
    assertEquals(
        "Only letters, numbers, spaces, and underscores allowed",
        viewModel.getUsernameError("user#name"))
    assertEquals(
        "Only letters, numbers, spaces, and underscores allowed",
        viewModel.getUsernameError("user!name"))
    assertEquals(
        "Only letters, numbers, spaces, and underscores allowed",
        viewModel.getUsernameError("user.name"))
  }

  @Test
  fun `getUsernameError returns null for valid username`() {
    assertNull(viewModel.getUsernameError("valid_user"))
    assertNull(viewModel.getUsernameError("user123"))
    assertNull(viewModel.getUsernameError("User Name"))
    assertNull(viewModel.getUsernameError("ABC"))
    assertNull(viewModel.getUsernameError("a".repeat(30))) // Exactly 30 chars
  }

  // ==================== DATE OF BIRTH VALIDATION TESTS ====================

  @Test
  fun `getDateOfBirthError returns null for empty string`() {
    assertNull(viewModel.getDateOfBirthError(""))
    assertNull(viewModel.getDateOfBirthError("   "))
  }

  @Test
  fun `getDateOfBirthError returns error for invalid format`() {
    assertEquals(
        "Enter your date in dd/mm/yyyy format.", viewModel.getDateOfBirthError("2000-01-01"))
    assertEquals("Enter your date in dd/mm/yyyy format.", viewModel.getDateOfBirthError("01/01/00"))
    assertEquals("Enter your date in dd/mm/yyyy format.", viewModel.getDateOfBirthError("1/1/2000"))
    assertEquals(
        "Enter your date in dd/mm/yyyy format.", viewModel.getDateOfBirthError("01-01-2000"))
  }

  @Test
  fun `getDateOfBirthError returns error for invalid date`() {
    assertEquals("Invalid date", viewModel.getDateOfBirthError("32/01/2000"))
    assertEquals("Invalid date", viewModel.getDateOfBirthError("01/13/2000"))
    assertEquals("Invalid date", viewModel.getDateOfBirthError("31/02/2000"))
    assertEquals("Invalid date", viewModel.getDateOfBirthError("00/01/2000"))
  }

  @Test
  fun `getDateOfBirthError returns null for valid date`() {
    assertNull(viewModel.getDateOfBirthError("01/01/2000"))
    assertNull(viewModel.getDateOfBirthError("31/12/1999"))
    assertNull(viewModel.getDateOfBirthError("29/02/2000")) // Leap year
    assertNull(viewModel.getDateOfBirthError("15/06/1995"))
  }

  // ==================== SIGN OUT TESTS ====================

  @Test
  fun `signOut calls onComplete on success`() = runTest {
    var completeCalled = false
    coEvery { mockAuthRepository.signOut() } returns Result.success(Unit)

    viewModel.signOut(onComplete = { completeCalled = true }, onError = {})
    testScheduler.advanceUntilIdle()

    assertTrue(completeCalled)
    coVerify { mockAuthRepository.signOut() }
  }

  @Test
  fun `signOut calls onError on failure`() = runTest {
    var errorMessage: String? = null
    coEvery { mockAuthRepository.signOut() } throws Exception("Sign out failed")

    viewModel.signOut(onComplete = {}, onError = { errorMessage = it })
    testScheduler.advanceUntilIdle()

    assertNotNull(errorMessage)
    assertTrue(errorMessage?.contains("Failed to sign out") == true)
  }
}
