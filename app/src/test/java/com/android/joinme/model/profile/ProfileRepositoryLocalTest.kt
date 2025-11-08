package com.android.joinme.model.profile

// Tests partially written with AI assistance; reviewed for correctness.

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.Timestamp
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileRepositoryLocalTest {
  private lateinit var repository: ProfileRepositoryLocal
  private lateinit var context: Context
  private lateinit var testImageUri: Uri

  @Before
  fun setUp() {
    repository = ProfileRepositoryLocal()
    context = ApplicationProvider.getApplicationContext()

    // Create a test image file
    testImageUri = createTestImageFile()
  }

  @After
  fun tearDown() {
    // Clean up test files
    val profilePhotosDir = File(context.filesDir, "profile_photos")
    profilePhotosDir.deleteRecursively()

    // Clean up test image
    testImageUri.path?.let { File(it).delete() }
  }

  // ==================== PROFILE MANAGEMENT TESTS ====================

  @Test
  fun testDefaultTestProfileExists() = runTest {
    val profile = repository.getProfile("test-user-123")
    assertNotNull("Profile should not be null", profile)
    profile?.let {
      assertEquals("test-user-123", it.uid)
      assertEquals("Test User", it.username)
      assertEquals("test@joinme.com", it.email)
      assertEquals("http://example.com/avatar.png", it.photoUrl)
      assertEquals("Switzerland", it.country)
      assertEquals("Test user for E2E testing", it.bio)
      assertEquals(listOf("Sports", "Technology"), it.interests)
      assertEquals("1990-01-01", it.dateOfBirth)
    }
  }

  @Test
  fun testCreateNewProfile() = runTest {
    val now = Timestamp.now()
    val newProfile =
        Profile(
            uid = "new-user-456",
            username = "New User",
            email = "new@joinme.com",
            photoUrl = "http://example.com/new-avatar.png",
            country = "France",
            bio = "New test user",
            interests = listOf("Music", "Travel"),
            dateOfBirth = "1995-05-05",
            createdAt = now,
            updatedAt = now)

    repository.createOrUpdateProfile(newProfile)
    val retrievedProfile = repository.getProfile("new-user-456")
    assertNotNull("Created profile should not be null", retrievedProfile)
    assertEquals(newProfile, retrievedProfile)
  }

  @Test
  fun testUpdateExistingProfile() = runTest {
    val profile = repository.getProfile("test-user-123")
    assertNotNull("Initial profile should not be null", profile)

    profile?.let {
      val updatedProfile = it.copy(username = "Updated User", bio = "Updated bio")

      repository.createOrUpdateProfile(updatedProfile)
      val retrievedProfile = repository.getProfile("test-user-123")
      assertNotNull("Updated profile should not be null", retrievedProfile)
      assertEquals("Updated User", retrievedProfile?.username)
      assertEquals("Updated bio", retrievedProfile?.bio)
    }
  }

  @Test
  fun testDeleteProfile() = runTest {
    // Verify profile exists first
    val profile = repository.getProfile("test-user-123")
    assertNotNull("Profile should exist before deletion", profile)

    // Delete the profile
    repository.deleteProfile("test-user-123")

    // Verify profile was deleted
    val deletedProfile = repository.getProfile("test-user-123")
    assertNull("Profile should be null after deletion", deletedProfile)
  }

  @Test
  fun testGetNonExistentProfileReturnsNull() = runTest {
    val profile = repository.getProfile("non-existent-uid")
    assertNull("Non-existent profile should be null", profile)
  }

  // ==================== PHOTO UPLOAD TESTS ====================

  @Test
  fun uploadProfilePhoto_success_returnsFileUrlAndUpdatesProfile() = runTest {
    // Given
    val uid = "test-user-123"
    val originalProfile = repository.getProfile(uid)
    assertNotNull("Test profile should exist", originalProfile)

    // When
    val photoUrl = repository.uploadProfilePhoto(context, uid, testImageUri)

    // Then
    assertNotNull("Photo URL should not be null", photoUrl)
    assertTrue("Photo URL should start with file:", photoUrl.startsWith("file:"))
    assertTrue("Photo URL should contain uid", photoUrl.contains(uid))
    assertTrue("Photo URL should end with .jpg", photoUrl.endsWith(".jpg"))

    // Verify profile was updated
    val updatedProfile = repository.getProfile(uid)
    assertNotNull("Updated profile should exist", updatedProfile)
    assertEquals("Profile photoUrl should be updated", photoUrl, updatedProfile?.photoUrl)

    // Verify file exists
    val fileUri = photoUrl.toUri()
    val file = File(fileUri.path!!)
    assertTrue("Photo file should exist", file.exists())
    assertTrue(
        "Photo file should be in profile_photos directory",
        file.parentFile?.name == "profile_photos")
  }

  @Test
  fun uploadProfilePhoto_createsDirectoryIfNotExists() = runTest {
    // Given
    val uid = "test-user-123"
    val profilePhotosDir = File(context.filesDir, "profile_photos")

    // Ensure directory doesn't exist
    profilePhotosDir.deleteRecursively()
    assertFalse("Directory should not exist", profilePhotosDir.exists())

    // When
    repository.uploadProfilePhoto(context, uid, testImageUri)

    // Then
    assertTrue("Directory should be created", profilePhotosDir.exists())
    assertTrue("Should be a directory", profilePhotosDir.isDirectory)
  }

  @Test
  fun uploadProfilePhoto_replacesExistingPhoto() = runTest {
    // Given
    val uid = "test-user-123"

    // First upload
    val firstPhotoUrl = repository.uploadProfilePhoto(context, uid, testImageUri)
    val firstFileUri = firstPhotoUrl.toUri()
    val firstFile = File(firstFileUri.path!!)
    assertTrue("First photo should exist", firstFile.exists())

    // Small delay to ensure different timestamp
    Thread.sleep(10)

    // When - Second upload
    val secondPhotoUrl = repository.uploadProfilePhoto(context, uid, testImageUri)

    // Then
    assertNotEquals(
        "URLs should be different (different timestamps)", firstPhotoUrl, secondPhotoUrl)

    // Verify old photo was deleted
    assertFalse("Old photo file should be deleted", firstFile.exists())

    // Verify new photo exists
    val secondFileUri = secondPhotoUrl.toUri()
    val secondFile = File(secondFileUri.path!!)
    assertTrue("New photo file should exist", secondFile.exists())

    // Verify profile has new URL
    val profile = repository.getProfile(uid)
    assertEquals("Profile should have new photo URL", secondPhotoUrl, profile?.photoUrl)
  }

  @Test
  fun uploadProfilePhoto_nonExistentProfile_throwsException() = runTest {
    // Given
    val nonExistentUid = "non-existent-uid"

    // When/Then
    val exception =
        assertThrows(IllegalArgumentException::class.java) {
          runBlocking { repository.uploadProfilePhoto(context, nonExistentUid, testImageUri) }
        }

    assertTrue(
        "Exception message should mention profile doesn't exist",
        exception.message?.contains("does not exist") == true)
  }

  @Test
  fun uploadProfilePhoto_invalidUri_throwsException() = runTest {
    // Given
    val uid = "test-user-123"
    val invalidUri = Uri.parse("content://invalid/uri/that/does/not/exist")

    // When/Then
    assertThrows(Exception::class.java) {
      runBlocking { repository.uploadProfilePhoto(context, uid, invalidUri) }
    }
  }

  @Test
  fun uploadProfilePhoto_copiesFileContent() = runTest {
    // Given
    val uid = "test-user-123"
    val testContent = "Test image content"
    val testUri = createTestFileWithContent(testContent)

    // When
    val photoUrl = repository.uploadProfilePhoto(context, uid, testUri)

    // Then
    val copiedFile = File(photoUrl.toUri().path!!)
    val copiedContent = copiedFile.readText()
    assertEquals("File content should be copied correctly", testContent, copiedContent)

    // Cleanup
    testUri.path?.let { File(it).delete() }
  }

  // ==================== PHOTO DELETE TESTS ====================

  @Test
  fun deleteProfilePhoto_success_removesFileAndClearsUrl() = runTest {
    // Given
    val uid = "test-user-123"

    // Upload a photo first
    val photoUrl = repository.uploadProfilePhoto(context, uid, testImageUri)
    val photoFile = File(photoUrl.toUri().path!!)
    assertTrue("Photo should exist before delete", photoFile.exists())

    // When
    repository.deleteProfilePhoto(uid)

    // Then
    assertFalse("Photo file should be deleted", photoFile.exists())

    val profile = repository.getProfile(uid)
    assertNull("Profile photoUrl should be null", profile?.photoUrl)
  }

  @Test
  fun deleteProfilePhoto_nonExistentProfile_completesGracefully() = runTest {
    // Given
    val nonExistentUid = "non-existent-uid"

    // When/Then - Should not throw
    repository.deleteProfilePhoto(nonExistentUid)

    // Verify no profile was created
    assertNull("No profile should be created", repository.getProfile(nonExistentUid))
  }

  @Test
  fun deleteProfilePhoto_profileWithoutPhoto_completesSuccessfully() = runTest {
    // Given
    val uid = "test-user-123"
    val profile = repository.getProfile(uid)!!
    val profileWithoutPhoto = profile.copy(photoUrl = null)
    repository.createOrUpdateProfile(profileWithoutPhoto)

    // When/Then - Should not throw
    repository.deleteProfilePhoto(uid)

    val updatedProfile = repository.getProfile(uid)
    assertNull("PhotoUrl should still be null", updatedProfile?.photoUrl)
  }

  @Test
  fun deleteProfilePhoto_nonFileUri_completesGracefully() = runTest {
    // Given
    val uid = "test-user-123"
    val profile = repository.getProfile(uid)!!

    // Set a non-file URI (e.g., http URL)
    val profileWithHttpUrl = profile.copy(photoUrl = "http://example.com/photo.jpg")
    repository.createOrUpdateProfile(profileWithHttpUrl)

    // When
    repository.deleteProfilePhoto(uid)

    // Then
    val updatedProfile = repository.getProfile(uid)
    assertNull("PhotoUrl should be cleared", updatedProfile?.photoUrl)
  }

  @Test
  fun deleteProfilePhoto_idempotent_canDeleteTwice() = runTest {
    // Given
    val uid = "test-user-123"
    repository.uploadProfilePhoto(context, uid, testImageUri)

    // When - Delete twice
    repository.deleteProfilePhoto(uid)
    repository.deleteProfilePhoto(uid) // Should not throw

    // Then
    val profile = repository.getProfile(uid)
    assertNull("PhotoUrl should be null after both deletes", profile?.photoUrl)
  }

  @Test
  fun deleteProfilePhoto_alreadyDeletedFile_completesGracefully() = runTest {
    // Given
    val uid = "test-user-123"
    val photoUrl = repository.uploadProfilePhoto(context, uid, testImageUri)

    // Manually delete the file (simulating external deletion)
    val photoFile = File(photoUrl.toUri().path!!)
    photoFile.delete()
    assertFalse("File should be deleted", photoFile.exists())

    // When - Try to delete through repository
    repository.deleteProfilePhoto(uid) // Should not throw

    // Then
    val profile = repository.getProfile(uid)
    assertNull("PhotoUrl should be cleared", profile?.photoUrl)
  }

  // ==================== HELPER METHODS ====================

  private fun createTestImageFile(): Uri {
    val testFile = File(context.cacheDir, "test_image.jpg")
    FileOutputStream(testFile).use { out ->
      // Write some dummy content
      out.write(ByteArray(100) { it.toByte() })
    }
    return Uri.fromFile(testFile)
  }

  private fun createTestFileWithContent(content: String): Uri {
    val testFile = File(context.cacheDir, "test_content_${System.currentTimeMillis()}.txt")
    testFile.writeText(content)
    return Uri.fromFile(testFile)
  }
}
