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
    val profile = repository.getProfile("test-user-id")
    assertNotNull("Profile should not be null", profile)
    profile?.let {
      assertEquals("test-user-id", it.uid)
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
    val profile = repository.getProfile("test-user-id")
    assertNotNull("Initial profile should not be null", profile)

    profile?.let {
      val updatedProfile = it.copy(username = "Updated User", bio = "Updated bio")

      repository.createOrUpdateProfile(updatedProfile)
      val retrievedProfile = repository.getProfile("test-user-id")
      assertNotNull("Updated profile should not be null", retrievedProfile)
      assertEquals("Updated User", retrievedProfile?.username)
      assertEquals("Updated bio", retrievedProfile?.bio)
    }
  }

  @Test
  fun testDeleteProfile() = runTest {
    // Verify profile exists first
    val profile = repository.getProfile("test-user-id")
    assertNotNull("Profile should exist before deletion", profile)

    // Delete the profile
    repository.deleteProfile("test-user-id")

    // Verify profile was deleted
    val deletedProfile = repository.getProfile("test-user-id")
    assertNull("Profile should be null after deletion", deletedProfile)
  }

  @Test
  fun testGetNonExistentProfileReturnsNull() = runTest {
    val profile = repository.getProfile("non-existent-uid")
    assertNull("Non-existent profile should be null", profile)
  }

  // ==================== GET PROFILES BY IDS TESTS ====================

  @Test
  fun testGetProfilesByIds_emptyList_returnsEmptyList() = runTest {
    val profiles = repository.getProfilesByIds(emptyList())
    assertNotNull("Should return empty list, not null", profiles)
    assertTrue("List should be empty", profiles!!.isEmpty())
  }

  @Test
  fun testGetProfilesByIds_singleExistingProfile_returnsProfile() = runTest {
    val profiles = repository.getProfilesByIds(listOf("test-user-id"))
    assertNotNull("Should return list with profile", profiles)
    assertEquals(1, profiles!!.size)
    assertEquals("test-user-id", profiles[0].uid)
  }

  @Test
  fun testGetProfilesByIds_multipleExistingProfiles_returnsAllProfiles() = runTest {
    // Create additional test profiles
    val now = Timestamp.now()
    val profile2 =
        Profile(
            uid = "user-2",
            username = "User 2",
            email = "user2@test.com",
            createdAt = now,
            updatedAt = now)
    val profile3 =
        Profile(
            uid = "user-3",
            username = "User 3",
            email = "user3@test.com",
            createdAt = now,
            updatedAt = now)

    repository.createOrUpdateProfile(profile2)
    repository.createOrUpdateProfile(profile3)

    val profiles = repository.getProfilesByIds(listOf("test-user-id", "user-2", "user-3"))
    assertNotNull("Should return list with all profiles", profiles)
    assertEquals(3, profiles!!.size)
  }

  @Test
  fun testGetProfilesByIds_someProfilesNotFound_returnsNull() = runTest {
    val profiles = repository.getProfilesByIds(listOf("test-user-id", "non-existent-uid"))
    assertNull("Should return null when some profiles not found", profiles)
  }

  @Test
  fun testGetProfilesByIds_allProfilesNotFound_returnsNull() = runTest {
    val profiles = repository.getProfilesByIds(listOf("non-existent-1", "non-existent-2"))
    assertNull("Should return null when all profiles not found", profiles)
  }

  @Test
  fun testGetProfilesByIds_duplicateIds_returnsCorrectCount() = runTest {
    // Even with duplicates in the request, we should get correct results
    val profiles = repository.getProfilesByIds(listOf("test-user-id", "test-user-id"))
    assertNotNull("Should return profiles", profiles)
    assertEquals(2, profiles!!.size)
  }

  // ==================== PHOTO UPLOAD TESTS ====================

  @Test
  fun uploadProfilePhoto_success_returnsFileUrlAndUpdatesProfile() = runTest {
    // Given
    val uid = "test-user-id"
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
    val uid = "test-user-id"
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
    val uid = "test-user-id"

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
    val uid = "test-user-id"
    val invalidUri = Uri.parse("content://invalid/uri/that/does/not/exist")

    // When/Then
    assertThrows(Exception::class.java) {
      runBlocking { repository.uploadProfilePhoto(context, uid, invalidUri) }
    }
  }

  @Test
  fun uploadProfilePhoto_copiesFileContent() = runTest {
    // Given
    val uid = "test-user-id"
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
    val uid = "test-user-id"

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
    val uid = "test-user-id"
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
    val uid = "test-user-id"
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
    val uid = "test-user-id"
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
    val uid = "test-user-id"
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

  // ==================== FOLLOW USER TESTS ====================

  @Test
  fun followUser_success_createsFollowRelationship() = runTest {
    val now = Timestamp.now()
    val user1 =
        Profile(
            uid = "user-1",
            username = "User 1",
            email = "user1@test.com",
            createdAt = now,
            updatedAt = now,
            followingCount = 0,
            followersCount = 0)
    val user2 =
        Profile(
            uid = "user-2",
            username = "User 2",
            email = "user2@test.com",
            createdAt = now,
            updatedAt = now,
            followingCount = 0,
            followersCount = 0)

    repository.createOrUpdateProfile(user1)
    repository.createOrUpdateProfile(user2)

    // When
    repository.followUser("user-1", "user-2")

    // Then
    assertTrue("User 1 should be following User 2", repository.isFollowing("user-1", "user-2"))

    // Check counts updated
    val updatedUser1 = repository.getProfile("user-1")
    val updatedUser2 = repository.getProfile("user-2")
    assertEquals(1, updatedUser1?.followingCount)
    assertEquals(1, updatedUser2?.followersCount)
  }

  @Test
  fun followUser_throwsException_whenFollowingSelf() = runTest {
    val exception =
        assertThrows(Exception::class.java) {
          runBlocking { repository.followUser("test-user-id", "test-user-id") }
        }

    assertTrue(exception.message?.contains("Cannot follow yourself") == true)
  }

  @Test
  fun followUser_throwsException_whenAlreadyFollowing() = runTest {
    val now = Timestamp.now()
    val user1 =
        Profile(
            uid = "user-1",
            username = "User 1",
            email = "user1@test.com",
            createdAt = now,
            updatedAt = now)
    val user2 =
        Profile(
            uid = "user-2",
            username = "User 2",
            email = "user2@test.com",
            createdAt = now,
            updatedAt = now)

    repository.createOrUpdateProfile(user1)
    repository.createOrUpdateProfile(user2)

    // First follow
    repository.followUser("user-1", "user-2")

    // Try to follow again
    val exception =
        assertThrows(Exception::class.java) {
          runBlocking { repository.followUser("user-1", "user-2") }
        }

    assertTrue(exception.message?.contains("Already following this user") == true)
  }

  @Test
  fun followUser_updatesCountsCorrectly_forMultipleFollows() = runTest {
    val now = Timestamp.now()
    val user1 =
        Profile(
            uid = "user-1",
            username = "User 1",
            email = "user1@test.com",
            createdAt = now,
            updatedAt = now,
            followingCount = 0,
            followersCount = 0)
    val user2 =
        Profile(
            uid = "user-2",
            username = "User 2",
            email = "user2@test.com",
            createdAt = now,
            updatedAt = now,
            followingCount = 0,
            followersCount = 0)
    val user3 =
        Profile(
            uid = "user-3",
            username = "User 3",
            email = "user3@test.com",
            createdAt = now,
            updatedAt = now,
            followingCount = 0,
            followersCount = 0)

    repository.createOrUpdateProfile(user1)
    repository.createOrUpdateProfile(user2)
    repository.createOrUpdateProfile(user3)

    // User 1 follows User 2 and User 3
    repository.followUser("user-1", "user-2")
    repository.followUser("user-1", "user-3")

    val updatedUser1 = repository.getProfile("user-1")
    assertEquals(2, updatedUser1?.followingCount)

    // User 2 follows User 1
    repository.followUser("user-2", "user-1")

    val finalUser1 = repository.getProfile("user-1")
    val finalUser2 = repository.getProfile("user-2")

    assertEquals(2, finalUser1?.followingCount)
    assertEquals(1, finalUser1?.followersCount)
    assertEquals(1, finalUser2?.followingCount)
    assertEquals(1, finalUser2?.followersCount)
  }

  // ==================== UNFOLLOW USER TESTS ====================

  @Test
  fun unfollowUser_success_removesFollowRelationship() = runTest {
    val now = Timestamp.now()
    val user1 =
        Profile(
            uid = "user-1",
            username = "User 1",
            email = "user1@test.com",
            createdAt = now,
            updatedAt = now,
            followingCount = 0,
            followersCount = 0)
    val user2 =
        Profile(
            uid = "user-2",
            username = "User 2",
            email = "user2@test.com",
            createdAt = now,
            updatedAt = now,
            followingCount = 0,
            followersCount = 0)

    repository.createOrUpdateProfile(user1)
    repository.createOrUpdateProfile(user2)

    // Follow first
    repository.followUser("user-1", "user-2")
    assertTrue(repository.isFollowing("user-1", "user-2"))

    // When: Unfollow
    repository.unfollowUser("user-1", "user-2")

    // Then
    assertFalse(repository.isFollowing("user-1", "user-2"))

    // Check counts updated
    val updatedUser1 = repository.getProfile("user-1")
    val updatedUser2 = repository.getProfile("user-2")
    assertEquals(0, updatedUser1?.followingCount)
    assertEquals(0, updatedUser2?.followersCount)
  }

  @Test
  fun unfollowUser_throwsException_whenNotFollowing() = runTest {
    val now = Timestamp.now()
    val user1 =
        Profile(
            uid = "user-1",
            username = "User 1",
            email = "user1@test.com",
            createdAt = now,
            updatedAt = now)
    val user2 =
        Profile(
            uid = "user-2",
            username = "User 2",
            email = "user2@test.com",
            createdAt = now,
            updatedAt = now)

    repository.createOrUpdateProfile(user1)
    repository.createOrUpdateProfile(user2)

    // Try to unfollow without following first
    val exception =
        assertThrows(Exception::class.java) {
          runBlocking { repository.unfollowUser("user-1", "user-2") }
        }

    assertTrue(exception.message?.contains("Not currently following this user") == true)
  }

  @Test
  fun unfollowUser_canRefollow_afterUnfollowing() = runTest {
    val now = Timestamp.now()
    val user1 =
        Profile(
            uid = "user-1",
            username = "User 1",
            email = "user1@test.com",
            createdAt = now,
            updatedAt = now,
            followingCount = 0,
            followersCount = 0)
    val user2 =
        Profile(
            uid = "user-2",
            username = "User 2",
            email = "user2@test.com",
            createdAt = now,
            updatedAt = now,
            followingCount = 0,
            followersCount = 0)

    repository.createOrUpdateProfile(user1)
    repository.createOrUpdateProfile(user2)

    // Follow, unfollow, then follow again
    repository.followUser("user-1", "user-2")
    repository.unfollowUser("user-1", "user-2")
    repository.followUser("user-1", "user-2")

    assertTrue(repository.isFollowing("user-1", "user-2"))
    val updatedUser1 = repository.getProfile("user-1")
    val updatedUser2 = repository.getProfile("user-2")
    assertEquals(1, updatedUser1?.followingCount)
    assertEquals(1, updatedUser2?.followersCount)
  }

  // ==================== IS FOLLOWING TESTS ====================

  @Test
  fun isFollowing_returnsFalse_whenNotFollowing() = runTest {
    val now = Timestamp.now()
    val user1 =
        Profile(
            uid = "user-1",
            username = "User 1",
            email = "user1@test.com",
            createdAt = now,
            updatedAt = now)
    val user2 =
        Profile(
            uid = "user-2",
            username = "User 2",
            email = "user2@test.com",
            createdAt = now,
            updatedAt = now)

    repository.createOrUpdateProfile(user1)
    repository.createOrUpdateProfile(user2)

    assertFalse(repository.isFollowing("user-1", "user-2"))
  }

  @Test
  fun isFollowing_isNotSymmetric() = runTest {
    val now = Timestamp.now()
    val user1 =
        Profile(
            uid = "user-1",
            username = "User 1",
            email = "user1@test.com",
            createdAt = now,
            updatedAt = now)
    val user2 =
        Profile(
            uid = "user-2",
            username = "User 2",
            email = "user2@test.com",
            createdAt = now,
            updatedAt = now)

    repository.createOrUpdateProfile(user1)
    repository.createOrUpdateProfile(user2)

    repository.followUser("user-1", "user-2")

    assertTrue(repository.isFollowing("user-1", "user-2"))
    assertFalse(repository.isFollowing("user-2", "user-1"))
  }

  // ==================== GET FOLLOWERS TESTS ====================

  @Test
  fun getFollowers_getFollowing_and_getMutualFollowing_returnEmptyList_whenNoRelationships() =
      runTest {
        val now = Timestamp.now()
        val user1 =
            Profile(
                uid = "user-1",
                username = "User 1",
                email = "user1@test.com",
                createdAt = now,
                updatedAt = now)
        val user2 =
            Profile(
                uid = "user-2",
                username = "User 2",
                email = "user2@test.com",
                createdAt = now,
                updatedAt = now)

        repository.createOrUpdateProfile(user1)
        repository.createOrUpdateProfile(user2)

        // Test getFollowers
        val followers = repository.getFollowers("user-1")
        assertTrue(followers.isEmpty())

        // Test getFollowing
        val following = repository.getFollowing("user-1")
        assertTrue(following.isEmpty())

        // Test getMutualFollowing
        val mutual = repository.getMutualFollowing("user-1", "user-2")
        assertTrue(mutual.isEmpty())
      }

  @Test
  fun getFollowers_returnsCorrectFollowers() = runTest {
    val now = Timestamp.now()
    val targetUser =
        Profile(
            uid = "target",
            username = "Target User",
            email = "target@test.com",
            createdAt = now,
            updatedAt = now)
    val follower1 =
        Profile(
            uid = "follower-1",
            username = "Follower 1",
            email = "follower1@test.com",
            createdAt = now,
            updatedAt = now)
    val follower2 =
        Profile(
            uid = "follower-2",
            username = "Follower 2",
            email = "follower2@test.com",
            createdAt = now,
            updatedAt = now)

    repository.createOrUpdateProfile(targetUser)
    repository.createOrUpdateProfile(follower1)
    repository.createOrUpdateProfile(follower2)

    repository.followUser("follower-1", "target")
    repository.followUser("follower-2", "target")

    val followers = repository.getFollowers("target")
    assertEquals(2, followers.size)
    assertTrue(followers.any { it.uid == "follower-1" })
    assertTrue(followers.any { it.uid == "follower-2" })
  }

  @Test
  fun getFollowers_respectsLimit() = runTest {
    val now = Timestamp.now()
    val targetUser =
        Profile(
            uid = "target",
            username = "Target User",
            email = "target@test.com",
            createdAt = now,
            updatedAt = now)

    repository.createOrUpdateProfile(targetUser)

    // Create 5 followers
    for (i in 1..5) {
      val follower =
          Profile(
              uid = "follower-$i",
              username = "Follower $i",
              email = "follower$i@test.com",
              createdAt = now,
              updatedAt = now)
      repository.createOrUpdateProfile(follower)
      repository.followUser("follower-$i", "target")
    }

    val followers = repository.getFollowers("target", limit = 3)
    assertEquals(3, followers.size)
  }

  // ==================== GET FOLLOWING TESTS ====================

  @Test
  fun getFollowing_returnsCorrectFollowing() = runTest {
    val now = Timestamp.now()
    val user =
        Profile(
            uid = "user",
            username = "User",
            email = "user@test.com",
            createdAt = now,
            updatedAt = now)
    val target1 =
        Profile(
            uid = "target-1",
            username = "Target 1",
            email = "target1@test.com",
            createdAt = now,
            updatedAt = now)
    val target2 =
        Profile(
            uid = "target-2",
            username = "Target 2",
            email = "target2@test.com",
            createdAt = now,
            updatedAt = now)

    repository.createOrUpdateProfile(user)
    repository.createOrUpdateProfile(target1)
    repository.createOrUpdateProfile(target2)

    repository.followUser("user", "target-1")
    repository.followUser("user", "target-2")

    val following = repository.getFollowing("user")
    assertEquals(2, following.size)
    assertTrue(following.any { it.uid == "target-1" })
    assertTrue(following.any { it.uid == "target-2" })
  }

  @Test
  fun getFollowing_respectsLimit() = runTest {
    val now = Timestamp.now()
    val user =
        Profile(
            uid = "user",
            username = "User",
            email = "user@test.com",
            createdAt = now,
            updatedAt = now)

    repository.createOrUpdateProfile(user)

    // Follow 5 users
    for (i in 1..5) {
      val target =
          Profile(
              uid = "target-$i",
              username = "Target $i",
              email = "target$i@test.com",
              createdAt = now,
              updatedAt = now)
      repository.createOrUpdateProfile(target)
      repository.followUser("user", "target-$i")
    }

    val following = repository.getFollowing("user", limit = 3)
    assertEquals(3, following.size)
  }

  // ==================== GET MUTUAL FOLLOWING TESTS ====================

  @Test
  fun getMutualFollowing_returnsCorrectMutualFollowing() = runTest {
    val now = Timestamp.now()
    val user1 =
        Profile(
            uid = "user-1",
            username = "User 1",
            email = "user1@test.com",
            createdAt = now,
            updatedAt = now)
    val user2 =
        Profile(
            uid = "user-2",
            username = "User 2",
            email = "user2@test.com",
            createdAt = now,
            updatedAt = now)
    val mutual1 =
        Profile(
            uid = "mutual-1",
            username = "Mutual 1",
            email = "mutual1@test.com",
            createdAt = now,
            updatedAt = now)
    val mutual2 =
        Profile(
            uid = "mutual-2",
            username = "Mutual 2",
            email = "mutual2@test.com",
            createdAt = now,
            updatedAt = now)
    val onlyUser1 =
        Profile(
            uid = "only-user1",
            username = "Only User1",
            email = "onlyuser1@test.com",
            createdAt = now,
            updatedAt = now)

    repository.createOrUpdateProfile(user1)
    repository.createOrUpdateProfile(user2)
    repository.createOrUpdateProfile(mutual1)
    repository.createOrUpdateProfile(mutual2)
    repository.createOrUpdateProfile(onlyUser1)

    // Both follow mutual1 and mutual2
    repository.followUser("user-1", "mutual-1")
    repository.followUser("user-1", "mutual-2")
    repository.followUser("user-2", "mutual-1")
    repository.followUser("user-2", "mutual-2")

    // Only user1 follows onlyUser1
    repository.followUser("user-1", "only-user1")

    val mutual = repository.getMutualFollowing("user-1", "user-2")
    assertEquals(2, mutual.size)
    assertTrue(mutual.any { it.uid == "mutual-1" })
    assertTrue(mutual.any { it.uid == "mutual-2" })
    assertFalse(mutual.any { it.uid == "only-user1" })
  }

  @Test
  fun getMutualFollowing_isSymmetric() = runTest {
    val now = Timestamp.now()
    val user1 =
        Profile(
            uid = "user-1",
            username = "User 1",
            email = "user1@test.com",
            createdAt = now,
            updatedAt = now)
    val user2 =
        Profile(
            uid = "user-2",
            username = "User 2",
            email = "user2@test.com",
            createdAt = now,
            updatedAt = now)
    val mutual =
        Profile(
            uid = "mutual",
            username = "Mutual",
            email = "mutual@test.com",
            createdAt = now,
            updatedAt = now)

    repository.createOrUpdateProfile(user1)
    repository.createOrUpdateProfile(user2)
    repository.createOrUpdateProfile(mutual)

    repository.followUser("user-1", "mutual")
    repository.followUser("user-2", "mutual")

    val mutual1to2 = repository.getMutualFollowing("user-1", "user-2")
    val mutual2to1 = repository.getMutualFollowing("user-2", "user-1")

    assertEquals(mutual1to2.size, mutual2to1.size)
    assertEquals(1, mutual1to2.size)
    assertTrue(mutual1to2.any { it.uid == "mutual" })
    assertTrue(mutual2to1.any { it.uid == "mutual" })
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
