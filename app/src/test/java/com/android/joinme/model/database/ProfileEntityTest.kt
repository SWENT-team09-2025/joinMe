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
  fun `toEntity converts all fields correctly`() {
    // When
    val entity = testProfile.toEntity()

    // Then
    assertEquals(testProfile.uid, entity.uid)
    assertEquals(testProfile.photoUrl, entity.photoUrl)
    assertEquals(testProfile.username, entity.username)
    assertEquals(testProfile.email, entity.email)
    assertEquals(testProfile.dateOfBirth, entity.dateOfBirth)
    assertEquals(testProfile.country, entity.country)
    assertEquals(testProfile.bio, entity.bio)
    assertEquals(testProfile.createdAt?.seconds, entity.createdAtSeconds)
    assertEquals(testProfile.createdAt?.nanoseconds, entity.createdAtNanoseconds)
    assertEquals(testProfile.updatedAt?.seconds, entity.updatedAtSeconds)
    assertEquals(testProfile.updatedAt?.nanoseconds, entity.updatedAtNanoseconds)
    assertEquals(testProfile.fcmToken, entity.fcmToken)
    assertEquals(testProfile.eventsJoinedCount, entity.eventsJoinedCount)
    assertEquals(testProfile.followersCount, entity.followersCount)
    assertEquals(testProfile.followingCount, entity.followingCount)
  }

  @Test
  fun `toEntity converts interests to JSON array format`() {
    // When
    val entity = testProfile.toEntity()

    // Then
    assertEquals("[\"Coding\",\"Testing\",\"Android\"]", entity.interestsJson)
  }

  @Test
  fun `toEntity converts empty interests list to empty JSON array`() {
    // Given
    val profile = testProfile.copy(interests = emptyList())

    // When
    val entity = profile.toEntity()

    // Then
    assertEquals("[]", entity.interestsJson)
  }

  @Test
  fun `toEntity converts single interest correctly`() {
    // Given
    val profile = testProfile.copy(interests = listOf("Running"))

    // When
    val entity = profile.toEntity()

    // Then
    assertEquals("[\"Running\"]", entity.interestsJson)
  }

  @Test
  fun `toEntity handles interests with special characters`() {
    // Given
    val profile = testProfile.copy(interests = listOf("Rock & Roll", "C++", "AI/ML"))

    // When
    val entity = profile.toEntity()

    // Then
    assertEquals("[\"Rock & Roll\",\"C++\",\"AI/ML\"]", entity.interestsJson)
  }

  @Test
  fun `toEntity handles null timestamp fields`() {
    // Given
    val profile = testProfile.copy(createdAt = null, updatedAt = null)

    // When
    val entity = profile.toEntity()

    // Then
    assertNull(entity.createdAtSeconds)
    assertNull(entity.createdAtNanoseconds)
    assertNull(entity.updatedAtSeconds)
    assertNull(entity.updatedAtNanoseconds)
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

  @Test
  fun `toEntity sets cachedAt timestamp`() {
    // Given
    val beforeTime = System.currentTimeMillis()

    // When
    val entity = testProfile.toEntity()

    // Then
    val afterTime = System.currentTimeMillis()
    assertTrue(entity.cachedAt >= beforeTime)
    assertTrue(entity.cachedAt <= afterTime)
  }

  // ==================== ProfileEntity.toProfile() Tests ====================

  @Test
  fun `toProfile converts all fields correctly`() {
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

    // Then
    assertEquals(entity.uid, profile.uid)
    assertEquals(entity.photoUrl, profile.photoUrl)
    assertEquals(entity.username, profile.username)
    assertEquals(entity.email, profile.email)
    assertEquals(entity.dateOfBirth, profile.dateOfBirth)
    assertEquals(entity.country, profile.country)
    assertEquals(entity.bio, profile.bio)
    assertEquals(entity.fcmToken, profile.fcmToken)
    assertEquals(entity.eventsJoinedCount, profile.eventsJoinedCount)
    assertEquals(entity.followersCount, profile.followersCount)
    assertEquals(entity.followingCount, profile.followingCount)
  }

  @Test
  fun `toProfile parses JSON interests correctly`() {
    // Given
    val entity =
        ProfileEntity(
            uid = "test-uid",
            username = "TestUser",
            email = "test@example.com",
            interestsJson = "[\"Coding\",\"Testing\",\"Android\"]",
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

    // When
    val profile = entity.toProfile()

    // Then
    assertEquals(listOf("Coding", "Testing", "Android"), profile.interests)
  }

  @Test
  fun `toProfile parses empty JSON interests array`() {
    // Given
    val entity =
        ProfileEntity(
            uid = "test-uid",
            username = "TestUser",
            email = "test@example.com",
            interestsJson = "[]",
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

    // When
    val profile = entity.toProfile()

    // Then
    assertTrue(profile.interests.isEmpty())
  }

  @Test
  fun `toProfile parses single interest correctly`() {
    // Given
    val entity =
        ProfileEntity(
            uid = "test-uid",
            username = "TestUser",
            email = "test@example.com",
            interestsJson = "[\"Running\"]",
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

    // When
    val profile = entity.toProfile()

    // Then
    assertEquals(listOf("Running"), profile.interests)
  }

  @Test
  fun `toProfile handles interests with special characters`() {
    // Given
    val entity =
        ProfileEntity(
            uid = "test-uid",
            username = "TestUser",
            email = "test@example.com",
            interestsJson = "[\"Rock & Roll\",\"C++\",\"AI/ML\"]",
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

    // When
    val profile = entity.toProfile()

    // Then
    assertEquals(listOf("Rock & Roll", "C++", "AI/ML"), profile.interests)
  }

  @Test
  fun `toProfile parses interests with spaces correctly`() {
    // Given
    val entity =
        ProfileEntity(
            uid = "test-uid",
            username = "TestUser",
            email = "test@example.com",
            interestsJson = "[\"Coding\", \"Testing\", \"Android\"]",
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

    // When
    val profile = entity.toProfile()

    // Then
    assertEquals(listOf("Coding", "Testing", "Android"), profile.interests)
  }

  @Test
  fun `toProfile converts timestamps correctly`() {
    // Given
    val entity =
        ProfileEntity(
            uid = "test-uid",
            username = "TestUser",
            email = "test@example.com",
            interestsJson = "[]",
            photoUrl = null,
            dateOfBirth = null,
            country = null,
            bio = null,
            createdAtSeconds = 1234567890L,
            createdAtNanoseconds = 123456789,
            updatedAtSeconds = 9876543210L,
            updatedAtNanoseconds = 987654321,
            fcmToken = null,
            eventsJoinedCount = 0,
            followersCount = 0,
            followingCount = 0)

    // When
    val profile = entity.toProfile()

    // Then
    assertNotNull(profile.createdAt)
    assertEquals(1234567890L, profile.createdAt?.seconds)
    assertEquals(123456789, profile.createdAt?.nanoseconds)
    assertNotNull(profile.updatedAt)
    assertEquals(9876543210L, profile.updatedAt?.seconds)
    assertEquals(987654321, profile.updatedAt?.nanoseconds)
  }

  @Test
  fun `toProfile handles null timestamps`() {
    // Given
    val entity =
        ProfileEntity(
            uid = "test-uid",
            username = "TestUser",
            email = "test@example.com",
            interestsJson = "[]",
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

    // When
    val profile = entity.toProfile()

    // Then
    assertNull(profile.createdAt)
    assertNull(profile.updatedAt)
  }

  @Test
  fun `toProfile handles partial timestamp (only seconds)`() {
    // Given - Missing nanoseconds should result in null timestamp
    val entity =
        ProfileEntity(
            uid = "test-uid",
            username = "TestUser",
            email = "test@example.com",
            interestsJson = "[]",
            photoUrl = null,
            dateOfBirth = null,
            country = null,
            bio = null,
            createdAtSeconds = 1234567890L,
            createdAtNanoseconds = null,
            updatedAtSeconds = null,
            updatedAtNanoseconds = null,
            fcmToken = null,
            eventsJoinedCount = 0,
            followersCount = 0,
            followingCount = 0)

    // When
    val profile = entity.toProfile()

    // Then - Both parts are required, so timestamp should be null
    assertNull(profile.createdAt)
    assertNull(profile.updatedAt)
  }

  // ==================== Round-trip Conversion Tests ====================

  @Test
  fun `round-trip conversion preserves all data`() {
    // Given
    val originalProfile = testProfile

    // When
    val entity = originalProfile.toEntity()
    val convertedProfile = entity.toProfile()

    // Then
    assertEquals(originalProfile.uid, convertedProfile.uid)
    assertEquals(originalProfile.photoUrl, convertedProfile.photoUrl)
    assertEquals(originalProfile.username, convertedProfile.username)
    assertEquals(originalProfile.email, convertedProfile.email)
    assertEquals(originalProfile.dateOfBirth, convertedProfile.dateOfBirth)
    assertEquals(originalProfile.country, convertedProfile.country)
    assertEquals(originalProfile.interests, convertedProfile.interests)
    assertEquals(originalProfile.bio, convertedProfile.bio)
    assertEquals(originalProfile.createdAt?.seconds, convertedProfile.createdAt?.seconds)
    assertEquals(originalProfile.createdAt?.nanoseconds, convertedProfile.createdAt?.nanoseconds)
    assertEquals(originalProfile.updatedAt?.seconds, convertedProfile.updatedAt?.seconds)
    assertEquals(originalProfile.updatedAt?.nanoseconds, convertedProfile.updatedAt?.nanoseconds)
    assertEquals(originalProfile.fcmToken, convertedProfile.fcmToken)
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

  @Test
  fun `round-trip conversion with empty interests`() {
    // Given
    val originalProfile = testProfile.copy(interests = emptyList())

    // When
    val entity = originalProfile.toEntity()
    val convertedProfile = entity.toProfile()

    // Then
    assertTrue(convertedProfile.interests.isEmpty())
  }

  @Test
  fun `round-trip conversion with many interests`() {
    // Given
    val manyInterests =
        listOf(
            "Coding",
            "Testing",
            "Android",
            "Kotlin",
            "Java",
            "Firebase",
            "Room",
            "Coroutines",
            "UI/UX",
            "Architecture")
    val originalProfile = testProfile.copy(interests = manyInterests)

    // When
    val entity = originalProfile.toEntity()
    val convertedProfile = entity.toProfile()

    // Then
    assertEquals(manyInterests, convertedProfile.interests)
  }
}
