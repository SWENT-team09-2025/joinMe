package com.android.joinme.model.database

// Tests partially written with AI assistance; reviewed for correctness.

import com.android.joinme.model.profile.Profile
import com.google.firebase.Timestamp
import org.junit.Assert.*
import org.junit.Test

class ProfileEntityTest {

  private val testTimestamp = Timestamp(1234567890L, 123456789)
  private val testProfile =
      Profile(
          uid = "test-uid-123",
          photoUrl = "https://example.com/photo.jpg",
          username = "TestUser",
          email = "test@example.com",
          dateOfBirth = "01/01/2000",
          country = "Switzerland",
          interests = listOf("Coding", "Testing", "Android"),
          bio = "Test bio",
          createdAt = testTimestamp,
          updatedAt = testTimestamp,
          fcmToken = "test-fcm-token",
          eventsJoinedCount = 5,
          followersCount = 10,
          followingCount = 15)

  // ==================== Profile.toEntity() Tests ====================

  @Test
  fun `toEntity converts core fields correctly`() {
    // When
    val entity = testProfile.toEntity()

    // Then - Core identity fields
    assertEquals(testProfile.uid, entity.uid)
    assertEquals(testProfile.username, entity.username)
    assertEquals(testProfile.email, entity.email)
    // Timestamp fields
    assertEquals(testProfile.createdAt?.seconds, entity.createdAtSeconds)
    assertEquals(testProfile.createdAt?.nanoseconds, entity.createdAtNanoseconds)
    assertEquals(testProfile.updatedAt?.seconds, entity.updatedAtSeconds)
    assertEquals(testProfile.updatedAt?.nanoseconds, entity.updatedAtNanoseconds)
    // Counter fields
    assertEquals(testProfile.eventsJoinedCount, entity.eventsJoinedCount)
    assertEquals(testProfile.followersCount, entity.followersCount)
    assertEquals(testProfile.followingCount, entity.followingCount)
  }

  @Test
  fun `toEntity converts interests to JSON with various cases`() {
    // Test multiple interests
    val entity1 = testProfile.toEntity()
    assertEquals("[\"Coding\",\"Testing\",\"Android\"]", entity1.interestsJson)

    // Test empty interests
    val entity2 = testProfile.copy(interests = emptyList()).toEntity()
    assertEquals("[]", entity2.interestsJson)

    // Test special characters
    val entity3 = testProfile.copy(interests = listOf("Rock & Roll", "C++", "AI/ML")).toEntity()
    assertEquals("[\"Rock & Roll\",\"C++\",\"AI/ML\"]", entity3.interestsJson)
  }

  @Test
  fun `toEntity handles null optional fields`() {
    // Given
    val profile =
        Profile(
            uid = "test-uid",
            username = "TestUser",
            email = "test@example.com",
            photoUrl = null,
            dateOfBirth = null,
            country = null,
            interests = emptyList(),
            bio = null,
            createdAt = null,
            updatedAt = null,
            fcmToken = null,
            eventsJoinedCount = 0,
            followersCount = 0,
            followingCount = 0)

    // When
    val entity = profile.toEntity()

    // Then
    assertNull(entity.photoUrl)
    assertNull(entity.dateOfBirth)
    assertNull(entity.country)
    assertNull(entity.bio)
    assertNull(entity.createdAtSeconds)
    assertNull(entity.createdAtNanoseconds)
    assertNull(entity.updatedAtSeconds)
    assertNull(entity.updatedAtNanoseconds)
    assertNull(entity.fcmToken)
    assertEquals("[]", entity.interestsJson)
  }

  // ==================== ProfileEntity.toProfile() Tests ====================

  @Test
  fun `toProfile converts core fields correctly`() {
    // Given
    val entity =
        ProfileEntity(
            uid = "test-uid-123",
            photoUrl = "https://example.com/photo.jpg",
            username = "TestUser",
            email = "test@example.com",
            dateOfBirth = "01/01/2000",
            country = "Switzerland",
            interestsJson = "[\"Coding\",\"Testing\",\"Android\"]",
            bio = "Test bio",
            createdAtSeconds = 1234567890L,
            createdAtNanoseconds = 123456789,
            updatedAtSeconds = 1234567890L,
            updatedAtNanoseconds = 123456789,
            fcmToken = "test-fcm-token",
            eventsJoinedCount = 5,
            followersCount = 10,
            followingCount = 15,
            cachedAt = System.currentTimeMillis())

    // When
    val profile = entity.toProfile()

    // Then - Core identity fields
    assertEquals(entity.uid, profile.uid)
    assertEquals(entity.username, profile.username)
    assertEquals(entity.email, profile.email)
    // Counter fields
    assertEquals(entity.eventsJoinedCount, profile.eventsJoinedCount)
    assertEquals(entity.followersCount, profile.followersCount)
    assertEquals(entity.followingCount, profile.followingCount)
  }

  @Test
  fun `toProfile parses JSON interests with various cases`() {
    // Helper function to create minimal entity
    fun createEntity(interestsJson: String) =
        ProfileEntity(
            uid = "test-uid",
            username = "TestUser",
            email = "test@example.com",
            interestsJson = interestsJson,
            photoUrl = null,
            dateOfBirth = null,
            country = null,
            bio = null,
            createdAtSeconds = null,
            createdAtNanoseconds = null,
            updatedAtSeconds = null,
            updatedAtNanoseconds = null,
            fcmToken = null,
            eventsJoinedCount = 0,
            followersCount = 0,
            followingCount = 0)

    // Test multiple interests
    val profile1 = createEntity("[\"Coding\",\"Testing\",\"Android\"]").toProfile()
    assertEquals(listOf("Coding", "Testing", "Android"), profile1.interests)

    // Test empty interests
    val profile2 = createEntity("[]").toProfile()
    assertTrue(profile2.interests.isEmpty())

    // Test special characters
    val profile3 = createEntity("[\"Rock & Roll\",\"C++\",\"AI/ML\"]").toProfile()
    assertEquals(listOf("Rock & Roll", "C++", "AI/ML"), profile3.interests)

    // Test JSON with spaces
    val profile4 = createEntity("[\"Coding\", \"Testing\", \"Android\"]").toProfile()
    assertEquals(listOf("Coding", "Testing", "Android"), profile4.interests)
  }

  @Test
  fun `toProfile handles timestamps in various states`() {
    // Helper function to create minimal entity with timestamps
    fun createEntity(createdSec: Long?, createdNano: Int?, updatedSec: Long?, updatedNano: Int?) =
        ProfileEntity(
            uid = "test-uid",
            username = "TestUser",
            email = "test@example.com",
            interestsJson = "[]",
            photoUrl = null,
            dateOfBirth = null,
            country = null,
            bio = null,
            createdAtSeconds = createdSec,
            createdAtNanoseconds = createdNano,
            updatedAtSeconds = updatedSec,
            updatedAtNanoseconds = updatedNano,
            fcmToken = null,
            eventsJoinedCount = 0,
            followersCount = 0,
            followingCount = 0)

    // Test complete timestamps
    val profile1 = createEntity(1234567890L, 123456789, 9876543210L, 987654321).toProfile()
    assertNotNull(profile1.createdAt)
    assertEquals(1234567890L, profile1.createdAt?.seconds)
    assertEquals(123456789, profile1.createdAt?.nanoseconds)
    assertNotNull(profile1.updatedAt)
    assertEquals(9876543210L, profile1.updatedAt?.seconds)
    assertEquals(987654321, profile1.updatedAt?.nanoseconds)

    // Test null timestamps
    val profile2 = createEntity(null, null, null, null).toProfile()
    assertNull(profile2.createdAt)
    assertNull(profile2.updatedAt)

    // Test partial timestamp (both parts required)
    val profile3 = createEntity(1234567890L, null, null, null).toProfile()
    assertNull(profile3.createdAt)
    assertNull(profile3.updatedAt)
  }

  // ==================== Round-trip Conversion Tests ====================

  @Test
  fun `round-trip conversion preserves critical data`() {
    // Given - Complete profile
    val originalProfile = testProfile

    // When
    val entity = originalProfile.toEntity()
    val convertedProfile = entity.toProfile()

    // Then - Core fields
    assertEquals(originalProfile.uid, convertedProfile.uid)
    assertEquals(originalProfile.username, convertedProfile.username)
    assertEquals(originalProfile.email, convertedProfile.email)
    // Interests
    assertEquals(originalProfile.interests, convertedProfile.interests)
    // Timestamps
    assertEquals(originalProfile.createdAt?.seconds, convertedProfile.createdAt?.seconds)
    assertEquals(originalProfile.createdAt?.nanoseconds, convertedProfile.createdAt?.nanoseconds)
    assertEquals(originalProfile.updatedAt?.seconds, convertedProfile.updatedAt?.seconds)
    assertEquals(originalProfile.updatedAt?.nanoseconds, convertedProfile.updatedAt?.nanoseconds)
    // Counters
    assertEquals(originalProfile.eventsJoinedCount, convertedProfile.eventsJoinedCount)
    assertEquals(originalProfile.followersCount, convertedProfile.followersCount)
    assertEquals(originalProfile.followingCount, convertedProfile.followingCount)
  }

  @Test
  fun `round-trip conversion with minimal profile data`() {
    // Given
    val originalProfile =
        Profile(
            uid = "test-uid",
            username = "TestUser",
            email = "test@example.com",
            interests = emptyList())

    // When
    val entity = originalProfile.toEntity()
    val convertedProfile = entity.toProfile()

    // Then
    assertEquals(originalProfile.uid, convertedProfile.uid)
    assertEquals(originalProfile.username, convertedProfile.username)
    assertEquals(originalProfile.email, convertedProfile.email)
    assertEquals(originalProfile.interests, convertedProfile.interests)
    assertNull(convertedProfile.photoUrl)
    assertNull(convertedProfile.dateOfBirth)
    assertNull(convertedProfile.country)
    assertNull(convertedProfile.bio)
    assertNull(convertedProfile.createdAt)
    assertNull(convertedProfile.updatedAt)
  }
}
