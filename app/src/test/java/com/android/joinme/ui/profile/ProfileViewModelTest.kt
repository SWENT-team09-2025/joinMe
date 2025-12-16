package com.android.joinme.ui.profile

import android.content.Context
import android.net.Uri
import com.android.joinme.model.authentification.AuthRepository
import com.android.joinme.model.invitation.InvitationsRepository
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
  private lateinit var mockInvitationRepository: InvitationsRepository
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
    mockInvitationRepository = mockk(relaxed = true)
    viewModel =
        ProfileViewModel(mockProfileRepository, mockAuthRepository, mockInvitationRepository)
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
  fun `loadProfile always reloads profile even for same uid`() = runTest {
    // First load
    coEvery { mockProfileRepository.getProfile("test-uid") } returns testProfile
    viewModel.loadProfile("test-uid")
    testScheduler.advanceUntilIdle()
    assertEquals(testProfile, viewModel.profile.value)

    // Second load with same uid should still call repository (no caching)
    viewModel.loadProfile("test-uid")
    testScheduler.advanceUntilIdle()

    // Profile should still be there, repository should be called twice
    assertEquals(testProfile, viewModel.profile.value)
    coVerify(exactly = 2) { mockProfileRepository.getProfile("test-uid") }
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
  fun `createOrUpdateProfile handles OfflineException correctly`() = runTest {
    coEvery { mockProfileRepository.createOrUpdateProfile(testProfile) } throws
        com.android.joinme.model.event.OfflineException(
            "This operation requires an internet connection")
    var errorMessage = ""

    viewModel.createOrUpdateProfile(
        profile = testProfile, onSuccess = {}, onError = { errorMessage = it })
    testScheduler.advanceUntilIdle()

    // Should call onError with offline message
    assertEquals("This operation requires an internet connection", errorMessage)
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
    coEvery { mockInvitationRepository.getInvitationsByUser("test-uid") } returns
        Result.success(emptyList())

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

  // ==================== PENDING PHOTO TESTS ====================

  @Test
  fun `setPendingPhoto stores photo URI locally without uploading`() = runTest {
    val mockUri = mockk<Uri>()

    viewModel.setPendingPhoto(mockUri)

    assertEquals(mockUri, viewModel.pendingPhotoUri.value)
    assertFalse(viewModel.pendingPhotoDelete.value)
    assertNull(viewModel.photoUploadError.value)

    // Verify no upload happened
    coVerify(exactly = 0) { mockProfileRepository.uploadProfilePhoto(any(), any(), any()) }
  }

  @Test
  fun `setPendingPhoto clears pending delete flag`() = runTest {
    val mockUri = mockk<Uri>()

    // First mark for deletion
    viewModel.markPhotoForDeletion()
    assertTrue(viewModel.pendingPhotoDelete.value)

    // Then set pending photo
    viewModel.setPendingPhoto(mockUri)

    assertEquals(mockUri, viewModel.pendingPhotoUri.value)
    assertFalse(viewModel.pendingPhotoDelete.value)
  }

  @Test
  fun `markPhotoForDeletion sets delete flag without deleting from storage`() = runTest {
    viewModel.markPhotoForDeletion()

    assertTrue(viewModel.pendingPhotoDelete.value)
    assertNull(viewModel.pendingPhotoUri.value)
    assertNull(viewModel.photoUploadError.value)

    // Verify no deletion happened
    coVerify(exactly = 0) { mockProfileRepository.deleteProfilePhoto(any()) }
  }

  @Test
  fun `markPhotoForDeletion clears pending photo URI`() = runTest {
    val mockUri = mockk<Uri>()

    // First set pending photo
    viewModel.setPendingPhoto(mockUri)
    assertEquals(mockUri, viewModel.pendingPhotoUri.value)

    // Then mark for deletion
    viewModel.markPhotoForDeletion()

    assertTrue(viewModel.pendingPhotoDelete.value)
    assertNull(viewModel.pendingPhotoUri.value)
  }

  @Test
  fun `clearPendingPhotoChanges clears both pending upload and delete`() = runTest {
    val mockUri = mockk<Uri>()

    // Set pending photo
    viewModel.setPendingPhoto(mockUri)
    viewModel.clearPendingPhotoChanges()

    assertNull(viewModel.pendingPhotoUri.value)
    assertFalse(viewModel.pendingPhotoDelete.value)

    // Mark for deletion
    viewModel.markPhotoForDeletion()
    viewModel.clearPendingPhotoChanges()

    assertNull(viewModel.pendingPhotoUri.value)
    assertFalse(viewModel.pendingPhotoDelete.value)
  }

  @Test
  fun `clearPhotoUploadError clears error state`() {
    viewModel.setPendingPhoto(mockk())

    viewModel.clearPhotoUploadError()

    assertNull(viewModel.photoUploadError.value)
  }

  // ==================== CREATE/UPDATE PROFILE WITH PHOTO TESTS ====================

  @Test
  fun `createOrUpdateProfile uploads pending photo when context provided`() = runTest {
    val mockContext = mockk<Context>()
    val mockUri = mockk<Uri>()
    val downloadUrl = "https://firebase.storage/test-uid/profile.jpg"

    coEvery { mockProfileRepository.createOrUpdateProfile(testProfile) } just Runs
    coEvery {
      mockProfileRepository.uploadProfilePhoto(mockContext, testProfile.uid, mockUri)
    } returns downloadUrl

    viewModel.setPendingPhoto(mockUri)
    var successCalled = false

    viewModel.createOrUpdateProfile(
        profile = testProfile, context = mockContext, onSuccess = { successCalled = true })
    testScheduler.advanceUntilIdle()

    // Verify photo was uploaded
    coVerify { mockProfileRepository.uploadProfilePhoto(mockContext, testProfile.uid, mockUri) }

    assertEquals(downloadUrl, viewModel.profile.value?.photoUrl)
    assertNull(viewModel.pendingPhotoUri.value) // Cleared after upload
    assertFalse(viewModel.pendingPhotoDelete.value)
    assertNull(viewModel.photoUploadError.value)
    assertTrue(successCalled)
  }

  @Test
  fun `createOrUpdateProfile deletes photo when marked for deletion`() = runTest {
    coEvery { mockProfileRepository.createOrUpdateProfile(testProfile) } just Runs
    coEvery { mockProfileRepository.deleteProfilePhoto(testProfile.uid) } just Runs

    viewModel.markPhotoForDeletion()
    var successCalled = false

    viewModel.createOrUpdateProfile(profile = testProfile, onSuccess = { successCalled = true })
    testScheduler.advanceUntilIdle()

    // Verify photo was deleted
    coVerify { mockProfileRepository.deleteProfilePhoto(testProfile.uid) }

    assertNull(viewModel.profile.value?.photoUrl)
    assertNull(viewModel.pendingPhotoUri.value)
    assertFalse(viewModel.pendingPhotoDelete.value) // Cleared after delete
    assertNull(viewModel.photoUploadError.value)
    assertTrue(successCalled)
  }

  @Test
  fun `createOrUpdateProfile without pending photo changes does not call photo operations`() =
      runTest {
        coEvery { mockProfileRepository.createOrUpdateProfile(testProfile) } just Runs

        var successCalled = false
        viewModel.createOrUpdateProfile(profile = testProfile, onSuccess = { successCalled = true })
        testScheduler.advanceUntilIdle()

        // Verify no photo operations
        coVerify(exactly = 0) { mockProfileRepository.uploadProfilePhoto(any(), any(), any()) }
        coVerify(exactly = 0) { mockProfileRepository.deleteProfilePhoto(any()) }

        assertEquals(testProfile, viewModel.profile.value)
        assertTrue(successCalled)
      }

  @Test
  fun `createOrUpdateProfile handles photo upload failure gracefully`() = runTest {
    val mockContext = mockk<Context>()
    val mockUri = mockk<Uri>()

    coEvery { mockProfileRepository.createOrUpdateProfile(testProfile) } just Runs
    coEvery {
      mockProfileRepository.uploadProfilePhoto(mockContext, testProfile.uid, mockUri)
    } throws Exception("Upload failed")

    viewModel.setPendingPhoto(mockUri)
    var successCalled = false

    viewModel.createOrUpdateProfile(
        profile = testProfile, context = mockContext, onSuccess = { successCalled = true })
    testScheduler.advanceUntilIdle()

    // Profile should still be updated
    assertEquals(testProfile.uid, viewModel.profile.value?.uid)
    // But photo error should be set
    assertNotNull(viewModel.photoUploadError.value)
    assertTrue(viewModel.photoUploadError.value!!.contains("photo upload failed"))
    // Pending state should be cleared
    assertNull(viewModel.pendingPhotoUri.value)
    assertFalse(viewModel.pendingPhotoDelete.value)
    assertTrue(successCalled)
  }

  @Test
  fun `createOrUpdateProfile handles photo deletion failure gracefully`() = runTest {
    coEvery { mockProfileRepository.createOrUpdateProfile(testProfile) } just Runs
    coEvery { mockProfileRepository.deleteProfilePhoto(testProfile.uid) } throws
        Exception("Delete failed")

    viewModel.markPhotoForDeletion()
    var successCalled = false

    viewModel.createOrUpdateProfile(profile = testProfile, onSuccess = { successCalled = true })
    testScheduler.advanceUntilIdle()

    // Profile should still be updated
    assertEquals(testProfile.uid, viewModel.profile.value?.uid)
    // But photo error should be set
    assertNotNull(viewModel.photoUploadError.value)
    assertTrue(viewModel.photoUploadError.value!!.contains("photo deletion failed"))
    // Pending state should be cleared
    assertNull(viewModel.pendingPhotoUri.value)
    assertFalse(viewModel.pendingPhotoDelete.value)
    assertTrue(successCalled)
  }

  @Test
  fun `createOrUpdateProfile without context does not upload pending photo`() = runTest {
    val mockUri = mockk<Uri>()

    coEvery { mockProfileRepository.createOrUpdateProfile(testProfile) } just Runs

    viewModel.setPendingPhoto(mockUri)
    var successCalled = false

    viewModel.createOrUpdateProfile(
        profile = testProfile, onSuccess = { successCalled = true }) // No context
    testScheduler.advanceUntilIdle()

    // Verify photo was NOT uploaded
    coVerify(exactly = 0) { mockProfileRepository.uploadProfilePhoto(any(), any(), any()) }

    assertEquals(testProfile, viewModel.profile.value)
    assertTrue(successCalled)
    // Pending photo should be cleared even though upload didn't happen
    assertNull(viewModel.pendingPhotoUri.value)
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
