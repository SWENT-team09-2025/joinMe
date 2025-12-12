package com.android.joinme.ui.profile

import android.content.Context
import android.net.Uri
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
          photoUrl = null,
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
  fun `clearError sets and clears error state`() {
    viewModel.setError("Error message")
    assertEquals("Error message", viewModel.error.value)
    viewModel.clearError()
    assertNull(viewModel.error.value)
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

  @Test
  fun `loadProfile handles blank uid error`() = runTest {
    viewModel.loadProfile("")
    testScheduler.advanceUntilIdle()

    assertFalse(viewModel.isLoading.value)
    assertEquals("User not authenticated. Please sign in again.", viewModel.error.value)
    assertNull(viewModel.profile.value)
  }

  @Test
  fun `loadProfile handles timeout exception`() = runTest {
    coEvery { mockProfileRepository.getProfile("test-uid") } coAnswers
        {
          kotlinx.coroutines.delay(15000L) // Longer than 10s timeout
          testProfile
        }

    viewModel.loadProfile("test-uid")
    testScheduler.advanceUntilIdle()

    assertNull(viewModel.profile.value)
    assertEquals(
        "Connection timeout. Please check your internet connection and try again.",
        viewModel.error.value)
    assertFalse(viewModel.isLoading.value)
  }

  @Test
  fun `loadProfile skips loading if profile already loaded for same uid`() = runTest {
    // First load
    coEvery { mockProfileRepository.getProfile("test-uid") } returns testProfile
    viewModel.loadProfile("test-uid")
    testScheduler.advanceUntilIdle()
    assertEquals(testProfile, viewModel.profile.value)

    // Clear the mock to verify it's not called again
    clearMocks(mockProfileRepository, answers = false)

    // Second load with same uid should skip
    viewModel.loadProfile("test-uid")
    testScheduler.advanceUntilIdle()

    // Profile should still be there, but repository should not be called again
    assertEquals(testProfile, viewModel.profile.value)
    coVerify(exactly = 0) { mockProfileRepository.getProfile(any()) }
  }

  @Test
  fun `loadProfile reloads if uid is different`() = runTest {
    // First load for user 1
    coEvery { mockProfileRepository.getProfile("test-uid-1") } returns testProfile
    viewModel.loadProfile("test-uid-1")
    testScheduler.advanceUntilIdle()
    assertEquals(testProfile, viewModel.profile.value)

    // Second load for user 2 should actually load
    val testProfile2 = testProfile.copy(uid = "test-uid-2", username = "User2")
    coEvery { mockProfileRepository.getProfile("test-uid-2") } returns testProfile2
    viewModel.loadProfile("test-uid-2")
    testScheduler.advanceUntilIdle()

    // Should have new profile
    assertEquals(testProfile2, viewModel.profile.value)
    coVerify(exactly = 1) { mockProfileRepository.getProfile("test-uid-2") }
  }

  // ==================== CREATE/UPDATE PROFILE TESTS ====================

  @Test
  fun `createOrUpdateProfile updates profile successfully`() = runTest {
    coEvery { mockProfileRepository.createOrUpdateProfile(testProfile) } just Runs
    var successCalled = false

    viewModel.createOrUpdateProfile(
        profile = testProfile, onSuccess = { successCalled = true }, onError = {})
    testScheduler.advanceUntilIdle()

    assertEquals(testProfile, viewModel.profile.value)
    assertNull(viewModel.error.value)
    assertFalse(viewModel.isLoading.value)
    assertTrue(successCalled)
    coVerify { mockProfileRepository.createOrUpdateProfile(testProfile) }
  }

  @Test
  fun `createOrUpdateProfile sets error on exception`() = runTest {
    coEvery { mockProfileRepository.createOrUpdateProfile(testProfile) } throws
        Exception("Save failed")
    var errorMessage = ""

    viewModel.createOrUpdateProfile(
        profile = testProfile, onSuccess = {}, onError = { errorMessage = it })
    testScheduler.advanceUntilIdle()

    assertTrue(viewModel.error.value?.contains("Failed to save profile") == true)
    assertTrue(errorMessage.contains("Failed to save profile"))
    assertFalse(viewModel.isLoading.value)
  }

  @Test
  fun `createOrUpdateProfile sets error on timeout`() = runTest {
    coEvery { mockProfileRepository.createOrUpdateProfile(testProfile) } coAnswers
        {
          kotlinx.coroutines.delay(15000L) // Delay longer than the 10s timeout
        }
    var errorMessage = ""

    viewModel.createOrUpdateProfile(
        profile = testProfile, onSuccess = {}, onError = { errorMessage = it })
    testScheduler.advanceUntilIdle()

    assertTrue(viewModel.error.value?.contains("Connection timeout") == true)
    assertTrue(errorMessage.contains("Connection timeout"))
    assertFalse(viewModel.isLoading.value)
  }

  @Test
  fun `createOrUpdateProfile works with default parameters`() = runTest {
    coEvery { mockProfileRepository.createOrUpdateProfile(testProfile) } just Runs

    viewModel.createOrUpdateProfile(profile = testProfile)
    testScheduler.advanceUntilIdle()

    assertEquals(testProfile, viewModel.profile.value)
    assertNull(viewModel.error.value)
    assertFalse(viewModel.isLoading.value)
    coVerify { mockProfileRepository.createOrUpdateProfile(testProfile) }
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

  // ==================== PHOTO UPLOAD TESTS ====================

  @Test
  fun `uploadProfilePhoto successful upload updates state correctly`() = runTest {
    // Given
    val mockContext = mockk<Context>()
    val mockUri = mockk<Uri>()
    val expectedUrl = "https://firebase.storage/test-uid/profile.jpg"

    // Load profile first
    coEvery { mockProfileRepository.getProfile("test-uid") } returns testProfile
    viewModel.loadProfile("test-uid")
    testScheduler.advanceUntilIdle()

    // Mock upload with delay to capture loading state
    coEvery { mockProfileRepository.uploadProfilePhoto(mockContext, "test-uid", mockUri) } coAnswers
        {
          kotlinx.coroutines.delay(100) // Small delay to keep loading state true
          expectedUrl
        }

    var successCalled = false
    var errorCalled = false

    // When
    viewModel.uploadProfilePhoto(
        context = mockContext,
        imageUri = mockUri,
        onSuccess = { successCalled = true },
        onError = { errorCalled = true })

    // Run current coroutine to set loading state
    testScheduler.runCurrent()

    // Then: Check loading state is true
    assertTrue("isUploadingPhoto should be true during upload", viewModel.isUploadingPhoto.value)

    // Complete the upload
    testScheduler.advanceUntilIdle()

    // Then: Check final state
    assertFalse("isUploadingPhoto should be false after upload", viewModel.isUploadingPhoto.value)
    assertNull("photoUploadError should be null on success", viewModel.photoUploadError.value)
    assertEquals(
        "Profile photoUrl should be updated", expectedUrl, viewModel.profile.value?.photoUrl)
    assertTrue("onSuccess callback should be called", successCalled)
    assertFalse("onError callback should not be called", errorCalled)

    coVerify { mockProfileRepository.uploadProfilePhoto(mockContext, "test-uid", mockUri) }
  }

  @Test
  fun `uploadProfilePhoto failure sets error and keeps state`() = runTest {
    // Given
    val mockContext = mockk<Context>()
    val mockUri = mockk<Uri>()
    val testProfile =
        Profile(
            uid = "test-uid",
            username = "TestUser",
            email = "test@example.com",
            photoUrl = "https://old-url.com/photo.jpg")

    // Load profile first
    coEvery { mockProfileRepository.getProfile("test-uid") } returns testProfile
    viewModel.loadProfile("test-uid")
    testScheduler.advanceUntilIdle()

    val originalPhotoUrl = viewModel.profile.value?.photoUrl

    // Mock upload failure
    coEvery { mockProfileRepository.uploadProfilePhoto(mockContext, "test-uid", mockUri) } throws
        Exception("Upload failed")

    var successCalled = false
    var errorMessage: String? = null

    // When
    viewModel.uploadProfilePhoto(
        context = mockContext,
        imageUri = mockUri,
        onSuccess = { successCalled = true },
        onError = { errorMessage = it })

    testScheduler.advanceUntilIdle()

    // Then
    assertFalse(
        "isUploadingPhoto should be false after failed upload", viewModel.isUploadingPhoto.value)
    assertNotNull(
        "photoUploadError should not be null on failure", viewModel.photoUploadError.value)
    assertTrue(
        "photoUploadError should contain error message",
        viewModel.photoUploadError.value?.contains("Failed to upload photo") == true)
    assertEquals(
        "Profile photoUrl should remain unchanged",
        originalPhotoUrl,
        viewModel.profile.value?.photoUrl)
    assertFalse("onSuccess callback should not be called", successCalled)
    assertNotNull("onError callback should be called with message", errorMessage)
    assertTrue(
        "Error message should contain failure info",
        errorMessage?.contains("Failed to upload photo") == true)
  }

  @Test
  fun `uploadProfilePhoto with no profile loaded does not call repository`() = runTest {
    // Given: No profile loaded
    val mockContext = mockk<Context>()
    val mockUri = mockk<Uri>()

    var successCalled = false
    var errorMessage: String? = null

    // When
    viewModel.uploadProfilePhoto(
        context = mockContext,
        imageUri = mockUri,
        onSuccess = { successCalled = true },
        onError = { errorMessage = it })

    testScheduler.advanceUntilIdle()

    // Then
    assertFalse("onSuccess should not be called", successCalled)
    assertNotNull("onError should be called with error message", errorMessage)
    assertEquals(
        "Error message should indicate no profile loaded", "No profile loaded", errorMessage)
    coVerify(exactly = 0) { mockProfileRepository.uploadProfilePhoto(any(), any(), any()) }
  }

  @Test
  fun `uploadProfilePhoto timeout sets appropriate error`() = runTest {
    // Given
    val mockContext = mockk<Context>()
    val mockUri = mockk<Uri>()
    val testProfile = Profile(uid = "test-uid", username = "TestUser", email = "test@example.com")

    coEvery { mockProfileRepository.getProfile("test-uid") } returns testProfile
    viewModel.loadProfile("test-uid")
    testScheduler.advanceUntilIdle()

    // Mock timeout
    coEvery { mockProfileRepository.uploadProfilePhoto(mockContext, "test-uid", mockUri) } coAnswers
        {
          kotlinx.coroutines.delay(35000) // Longer than 30s timeout
          "should-not-reach"
        }

    var errorMessage: String? = null

    // When
    viewModel.uploadProfilePhoto(
        context = mockContext, imageUri = mockUri, onError = { errorMessage = it })

    testScheduler.advanceUntilIdle()

    // Then
    assertNotNull("Error message should be set", errorMessage)
    assertTrue("Error should mention timeout", errorMessage?.contains("timeout") == true)
  }

  @Test
  fun `deleteProfilePhoto successful deletion updates state`() = runTest {
    // Given
    val testProfile =
        Profile(
            uid = "test-uid",
            username = "TestUser",
            email = "test@example.com",
            photoUrl = "https://firebase.storage/test-uid/profile.jpg")

    coEvery { mockProfileRepository.getProfile("test-uid") } returns testProfile
    viewModel.loadProfile("test-uid")
    testScheduler.advanceUntilIdle()

    coEvery { mockProfileRepository.deleteProfilePhoto("test-uid") } just Runs

    var successCalled = false
    var errorCalled = false

    // When
    viewModel.deleteProfilePhoto(
        onSuccess = { successCalled = true }, onError = { errorCalled = true })

    testScheduler.advanceUntilIdle()

    // Then
    assertFalse("isUploadingPhoto should be false after deletion", viewModel.isUploadingPhoto.value)
    assertNull("photoUploadError should be null on success", viewModel.photoUploadError.value)
    assertNull("Profile photoUrl should be null after deletion", viewModel.profile.value?.photoUrl)
    assertTrue("onSuccess callback should be called", successCalled)
    assertFalse("onError callback should not be called", errorCalled)

    coVerify { mockProfileRepository.deleteProfilePhoto("test-uid") }
  }

  @Test
  fun `deleteProfilePhoto failure sets error`() = runTest {
    // Given
    val testProfile =
        Profile(
            uid = "test-uid",
            username = "TestUser",
            email = "test@example.com",
            photoUrl = "https://firebase.storage/test-uid/profile.jpg")

    coEvery { mockProfileRepository.getProfile("test-uid") } returns testProfile
    viewModel.loadProfile("test-uid")
    testScheduler.advanceUntilIdle()

    val originalPhotoUrl = viewModel.profile.value?.photoUrl

    coEvery { mockProfileRepository.deleteProfilePhoto("test-uid") } throws
        Exception("Delete failed")

    var errorMessage: String? = null

    // When
    viewModel.deleteProfilePhoto(onError = { errorMessage = it })

    testScheduler.advanceUntilIdle()

    // Then
    assertNotNull("photoUploadError should be set", viewModel.photoUploadError.value)
    assertTrue(
        "Error should mention deletion failure",
        viewModel.photoUploadError.value?.contains("Failed to delete photo") == true)
    assertEquals(
        "photoUrl should remain unchanged on failure",
        originalPhotoUrl,
        viewModel.profile.value?.photoUrl)
    assertNotNull("onError callback should receive message", errorMessage)
  }

  @Test
  fun `deleteProfilePhoto with no profile loaded does not call repository`() = runTest {
    // Given: No profile loaded
    var errorMessage: String? = null

    // When
    viewModel.deleteProfilePhoto(onError = { errorMessage = it })

    testScheduler.advanceUntilIdle()

    // Then
    assertNotNull("onError should be called", errorMessage)
    assertEquals("Error should indicate no profile", "No profile loaded", errorMessage)
    coVerify(exactly = 0) { mockProfileRepository.deleteProfilePhoto(any()) }
  }

  @Test
  fun `clearPhotoUploadError clears error state`() {
    // Given
    viewModel.uploadProfilePhoto(context = mockk(), imageUri = mockk(), onError = {})

    // When
    viewModel.clearPhotoUploadError()

    // Then
    assertNull("photoUploadError should be null after clear", viewModel.photoUploadError.value)
  }

  // ==================== USERNAME VALIDATION TESTS ====================

  @Test
  fun `getUsernameError returns good error reasons`() {
    assertEquals("Username is required", viewModel.getUsernameError(""))
    assertEquals("Username must be at least 3 characters", viewModel.getUsernameError("ab"))
    assertEquals("Username must be at least 3 characters", viewModel.getUsernameError("a"))
    assertEquals(
        "Username must not exceed 30 characters", viewModel.getUsernameError("a".repeat(31)))
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
