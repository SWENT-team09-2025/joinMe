package com.android.joinme.model.database

// Tests partially written with AI assistance; reviewed for correctness.

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ProfileDaoTest {

  private lateinit var database: AppDatabase
  private lateinit var profileDao: ProfileDao

  private val testProfile1 =
      ProfileEntity(
          uid = "test-uid-1",
          photoUrl = "https://example.com/photo1.jpg",
          username = "TestUser1",
          email = "test1@example.com",
          dateOfBirth = "01/01/2000",
          country = "Switzerland",
          interestsJson = "[\"Coding\",\"Testing\"]",
          bio = "Test bio 1",
          createdAtSeconds = 1234567890L,
          createdAtNanoseconds = 123456789,
          updatedAtSeconds = 1234567890L,
          updatedAtNanoseconds = 123456789,
          fcmToken = "fcm-token-1",
          eventsJoinedCount = 5,
          followersCount = 10,
          followingCount = 15,
          cachedAt = System.currentTimeMillis())

  private val testProfile2 =
      ProfileEntity(
          uid = "test-uid-2",
          photoUrl = "https://example.com/photo2.jpg",
          username = "TestUser2",
          email = "test2@example.com",
          dateOfBirth = "02/02/1999",
          country = "France",
          interestsJson = "[\"Running\",\"Swimming\"]",
          bio = "Test bio 2",
          createdAtSeconds = 9876543210L,
          createdAtNanoseconds = 987654321,
          updatedAtSeconds = 9876543210L,
          updatedAtNanoseconds = 987654321,
          fcmToken = "fcm-token-2",
          eventsJoinedCount = 3,
          followersCount = 8,
          followingCount = 12,
          cachedAt = System.currentTimeMillis())

  private val testProfile3 =
      ProfileEntity(
          uid = "test-uid-3",
          photoUrl = null,
          username = "TestUser3",
          email = "test3@example.com",
          dateOfBirth = null,
          country = null,
          interestsJson = "[]",
          bio = null,
          createdAtSeconds = null,
          createdAtNanoseconds = null,
          updatedAtSeconds = null,
          updatedAtNanoseconds = null,
          fcmToken = null,
          eventsJoinedCount = 0,
          followersCount = 0,
          followingCount = 0,
          cachedAt = System.currentTimeMillis() - 10000) // Older cache

  @Before
  fun setup() {
    // Create an in-memory database for testing
    database =
        Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    profileDao = database.profileDao()
  }

  @After
  fun tearDown() {
    database.close()
  }

  // ==================== Insert Tests ====================

  @Test
  fun insertProfile_insertsSuccessfully() = runTest {
    // When
    profileDao.insertProfile(testProfile1)

    // Then
    val retrieved = profileDao.getProfileById(testProfile1.uid)
    assertNotNull(retrieved)
    assertEquals(testProfile1.uid, retrieved?.uid)
    assertEquals(testProfile1.username, retrieved?.username)
    assertEquals(testProfile1.email, retrieved?.email)
  }

  @Test
  fun insertProfile_withNullFields_insertsSuccessfully() = runTest {
    // When
    profileDao.insertProfile(testProfile3)

    // Then
    val retrieved = profileDao.getProfileById(testProfile3.uid)
    assertNotNull(retrieved)
    assertEquals(testProfile3.uid, retrieved?.uid)
    assertNull(retrieved?.photoUrl)
    assertNull(retrieved?.dateOfBirth)
    assertNull(retrieved?.country)
    assertNull(retrieved?.bio)
  }

  @Test
  fun insertProfile_replacesExistingProfile() = runTest {
    // Given
    profileDao.insertProfile(testProfile1)

    // When - Insert profile with same UID but different data
    val updatedProfile = testProfile1.copy(username = "UpdatedUser", bio = "Updated bio")
    profileDao.insertProfile(updatedProfile)

    // Then
    val retrieved = profileDao.getProfileById(testProfile1.uid)
    assertNotNull(retrieved)
    assertEquals("UpdatedUser", retrieved?.username)
    assertEquals("Updated bio", retrieved?.bio)

    // Verify only one profile exists
    val allProfiles = profileDao.getAllProfiles()
    assertEquals(1, allProfiles.size)
  }

  @Test
  fun insertProfiles_insertsMultipleProfilesSuccessfully() = runTest {
    // When
    profileDao.insertProfiles(listOf(testProfile1, testProfile2, testProfile3))

    // Then
    val allProfiles = profileDao.getAllProfiles()
    assertEquals(3, allProfiles.size)

    val uids = allProfiles.map { it.uid }
    assertTrue(uids.contains(testProfile1.uid))
    assertTrue(uids.contains(testProfile2.uid))
    assertTrue(uids.contains(testProfile3.uid))
  }

  @Test
  fun insertProfiles_withEmptyList_doesNothing() = runTest {
    // When
    profileDao.insertProfiles(emptyList())

    // Then
    val allProfiles = profileDao.getAllProfiles()
    assertTrue(allProfiles.isEmpty())
  }

  @Test
  fun insertProfiles_replacesDuplicates() = runTest {
    // Given
    profileDao.insertProfiles(listOf(testProfile1, testProfile2))

    // When - Insert list with one updated profile and one new profile
    val updatedProfile1 = testProfile1.copy(username = "UpdatedUser1")
    profileDao.insertProfiles(listOf(updatedProfile1, testProfile3))

    // Then
    val allProfiles = profileDao.getAllProfiles()
    assertEquals(3, allProfiles.size) // 2 original + 1 new

    val profile1 = profileDao.getProfileById(testProfile1.uid)
    assertEquals("UpdatedUser1", profile1?.username)
  }

  // ==================== Get Tests ====================

  @Test
  fun getProfileById_returnsNullForNonexistentProfile() = runTest {
    // When
    val result = profileDao.getProfileById("nonexistent-uid")

    // Then
    assertNull(result)
  }

  @Test
  fun getProfileById_returnsCorrectProfile() = runTest {
    // Given
    profileDao.insertProfiles(listOf(testProfile1, testProfile2, testProfile3))

    // When
    val result = profileDao.getProfileById(testProfile2.uid)

    // Then
    assertNotNull(result)
    assertEquals(testProfile2.uid, result?.uid)
    assertEquals(testProfile2.username, result?.username)
    assertEquals(testProfile2.email, result?.email)
    assertEquals(testProfile2.country, result?.country)
  }

  @Test
  fun getAllProfiles_returnsEmptyListWhenDatabaseEmpty() = runTest {
    // When
    val result = profileDao.getAllProfiles()

    // Then
    assertTrue(result.isEmpty())
  }

  @Test
  fun getAllProfiles_returnsAllProfiles() = runTest {
    // Given
    profileDao.insertProfiles(listOf(testProfile1, testProfile2, testProfile3))

    // When
    val result = profileDao.getAllProfiles()

    // Then
    assertEquals(3, result.size)
    val uids = result.map { it.uid }
    assertTrue(uids.contains(testProfile1.uid))
    assertTrue(uids.contains(testProfile2.uid))
    assertTrue(uids.contains(testProfile3.uid))
  }

  // ==================== Delete Tests ====================

  @Test
  fun deleteProfile_removesSpecificProfile() = runTest {
    // Given
    profileDao.insertProfiles(listOf(testProfile1, testProfile2, testProfile3))

    // When
    profileDao.deleteProfile(testProfile2.uid)

    // Then
    val result = profileDao.getProfileById(testProfile2.uid)
    assertNull(result)

    // Verify other profiles still exist
    val allProfiles = profileDao.getAllProfiles()
    assertEquals(2, allProfiles.size)
    assertNotNull(profileDao.getProfileById(testProfile1.uid))
    assertNotNull(profileDao.getProfileById(testProfile3.uid))
  }

  @Test
  fun deleteProfile_withNonexistentUid_doesNothing() = runTest {
    // Given
    profileDao.insertProfile(testProfile1)

    // When
    profileDao.deleteProfile("nonexistent-uid")

    // Then - Original profile still exists
    val allProfiles = profileDao.getAllProfiles()
    assertEquals(1, allProfiles.size)
  }

  @Test
  fun deleteAllProfiles_removesAllProfiles() = runTest {
    // Given
    profileDao.insertProfiles(listOf(testProfile1, testProfile2, testProfile3))

    // When
    profileDao.deleteAllProfiles()

    // Then
    val result = profileDao.getAllProfiles()
    assertTrue(result.isEmpty())
  }

  @Test
  fun deleteAllProfiles_onEmptyDatabase_doesNothing() = runTest {
    // When
    profileDao.deleteAllProfiles()

    // Then
    val result = profileDao.getAllProfiles()
    assertTrue(result.isEmpty())
  }

  @Test
  fun deleteOldProfiles_removesProfilesBeforeTimestamp() = runTest {
    // Given
    val currentTime = System.currentTimeMillis()
    val oldProfile1 =
        testProfile1.copy(uid = "old-1", cachedAt = currentTime - 10000) // 10 seconds ago
    val oldProfile2 =
        testProfile2.copy(uid = "old-2", cachedAt = currentTime - 5000) // 5 seconds ago
    val recentProfile =
        testProfile3.copy(uid = "recent", cachedAt = currentTime - 1000) // 1 second ago

    profileDao.insertProfiles(listOf(oldProfile1, oldProfile2, recentProfile))

    // When - Delete profiles older than 3 seconds ago
    val cutoffTime = currentTime - 3000
    profileDao.deleteOldProfiles(cutoffTime)

    // Then
    val remainingProfiles = profileDao.getAllProfiles()
    assertEquals(1, remainingProfiles.size)
    assertEquals("recent", remainingProfiles[0].uid)

    // Verify old profiles are deleted
    assertNull(profileDao.getProfileById("old-1"))
    assertNull(profileDao.getProfileById("old-2"))
  }

  @Test
  fun deleteOldProfiles_withFutureTimestamp_removesAllProfiles() = runTest {
    // Given
    profileDao.insertProfiles(listOf(testProfile1, testProfile2, testProfile3))

    // When - Delete profiles older than a future timestamp
    val futureTime = System.currentTimeMillis() + 10000
    profileDao.deleteOldProfiles(futureTime)

    // Then
    val remainingProfiles = profileDao.getAllProfiles()
    assertTrue(remainingProfiles.isEmpty())
  }

  @Test
  fun deleteOldProfiles_withVeryOldTimestamp_removesNoProfiles() = runTest {
    // Given
    profileDao.insertProfiles(listOf(testProfile1, testProfile2, testProfile3))

    // When - Delete profiles older than a very old timestamp
    val veryOldTime = 1000L // Very old timestamp
    profileDao.deleteOldProfiles(veryOldTime)

    // Then
    val remainingProfiles = profileDao.getAllProfiles()
    assertEquals(3, remainingProfiles.size)
  }

  // ==================== Complex Scenario Tests ====================

  @Test
  fun complexScenario_insertUpdateDeleteOperations() = runTest {
    // Insert initial profiles
    profileDao.insertProfiles(listOf(testProfile1, testProfile2))
    assertEquals(2, profileDao.getAllProfiles().size)

    // Update one profile
    val updatedProfile1 = testProfile1.copy(bio = "Updated bio", followersCount = 20)
    profileDao.insertProfile(updatedProfile1)
    val retrieved1 = profileDao.getProfileById(testProfile1.uid)
    assertEquals("Updated bio", retrieved1?.bio)
    assertEquals(20, retrieved1?.followersCount)

    // Add a new profile
    profileDao.insertProfile(testProfile3)
    assertEquals(3, profileDao.getAllProfiles().size)

    // Delete one profile
    profileDao.deleteProfile(testProfile2.uid)
    assertEquals(2, profileDao.getAllProfiles().size)
    assertNull(profileDao.getProfileById(testProfile2.uid))

    // Verify remaining profiles
    assertNotNull(profileDao.getProfileById(testProfile1.uid))
    assertNotNull(profileDao.getProfileById(testProfile3.uid))
  }

  @Test
  fun testDataIntegrity_allFieldsPreserved() = runTest {
    // Given
    val profile =
        ProfileEntity(
            uid = "integrity-test-uid",
            photoUrl = "https://example.com/photo.jpg",
            username = "IntegrityTest",
            email = "integrity@test.com",
            dateOfBirth = "15/06/1995",
            country = "Germany",
            interestsJson = "[\"Music\",\"Art\",\"Technology\"]",
            bio = "Testing data integrity with special chars: !@#$%^&*()",
            createdAtSeconds = 1609459200L,
            createdAtNanoseconds = 500000000,
            updatedAtSeconds = 1640995200L,
            updatedAtNanoseconds = 750000000,
            fcmToken = "fcm-token-integrity-test",
            eventsJoinedCount = 42,
            followersCount = 123,
            followingCount = 456,
            cachedAt = System.currentTimeMillis())

    // When
    profileDao.insertProfile(profile)
    val retrieved = profileDao.getProfileById(profile.uid)

    // Then - Verify all fields are preserved exactly
    assertNotNull(retrieved)
    assertEquals(profile.uid, retrieved?.uid)
    assertEquals(profile.photoUrl, retrieved?.photoUrl)
    assertEquals(profile.username, retrieved?.username)
    assertEquals(profile.email, retrieved?.email)
    assertEquals(profile.dateOfBirth, retrieved?.dateOfBirth)
    assertEquals(profile.country, retrieved?.country)
    assertEquals(profile.interestsJson, retrieved?.interestsJson)
    assertEquals(profile.bio, retrieved?.bio)
    assertEquals(profile.createdAtSeconds, retrieved?.createdAtSeconds)
    assertEquals(profile.createdAtNanoseconds, retrieved?.createdAtNanoseconds)
    assertEquals(profile.updatedAtSeconds, retrieved?.updatedAtSeconds)
    assertEquals(profile.updatedAtNanoseconds, retrieved?.updatedAtNanoseconds)
    assertEquals(profile.fcmToken, retrieved?.fcmToken)
    assertEquals(profile.eventsJoinedCount, retrieved?.eventsJoinedCount)
    assertEquals(profile.followersCount, retrieved?.followersCount)
    assertEquals(profile.followingCount, retrieved?.followingCount)
    assertEquals(profile.cachedAt, retrieved?.cachedAt)
  }

  @Test
  fun testConcurrentOperations_maintainsConsistency() = runTest {
    // Insert multiple profiles
    profileDao.insertProfiles(listOf(testProfile1, testProfile2, testProfile3))

    // Perform multiple operations
    profileDao.deleteProfile(testProfile1.uid)
    profileDao.insertProfile(testProfile1.copy(username = "Reinserted"))
    val updated = testProfile2.copy(bio = "Updated concurrently")
    profileDao.insertProfile(updated)

    // Verify final state
    val allProfiles = profileDao.getAllProfiles()
    assertEquals(3, allProfiles.size)

    val profile1 = profileDao.getProfileById(testProfile1.uid)
    assertEquals("Reinserted", profile1?.username)

    val profile2 = profileDao.getProfileById(testProfile2.uid)
    assertEquals("Updated concurrently", profile2?.bio)
  }
}
