package com.android.joinme.model.profile

import com.google.firebase.Timestamp
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ProfileRepositoryLocalTest {
  private lateinit var repository: ProfileRepositoryLocal

  @Before
  fun setUp() {
    repository = ProfileRepositoryLocal()
  }

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
}
