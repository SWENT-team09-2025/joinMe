package com.android.joinme.model.profile

// Tests partially written with AI assistance; reviewed for correctness.

import android.content.Context
import android.net.Uri
import com.android.joinme.model.database.AppDatabase
import com.android.joinme.model.database.ProfileDao
import com.android.joinme.model.database.toEntity
import com.android.joinme.model.event.OfflineException
import com.android.joinme.network.NetworkMonitor
import com.google.firebase.Timestamp
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ProfileRepositoryCachedTest {

  private lateinit var mockContext: Context
  private lateinit var mockFirestoreRepo: ProfileRepository
  private lateinit var mockNetworkMonitor: NetworkMonitor
  private lateinit var mockDatabase: AppDatabase
  private lateinit var mockProfileDao: ProfileDao
  private lateinit var cachedRepository: ProfileRepositoryCached

  private val testUid = "test-uid-123"
  private val testUid2 = "test-uid-456"
  private val testProfile =
      Profile(
          uid = testUid,
          username = "TestUser",
          email = "test@example.com",
          dateOfBirth = "01/01/2000",
          country = "Switzerland",
          interests = listOf("Coding", "Testing"),
          bio = "Test bio",
          photoUrl = "https://example.com/photo.jpg",
          createdAt = Timestamp(1234567890L, 123456789),
          updatedAt = Timestamp(1234567890L, 123456789),
          fcmToken = "test-fcm-token",
          eventsJoinedCount = 5,
          followersCount = 10,
          followingCount = 15)

  private val testProfile2 =
      Profile(
          uid = testUid2,
          username = "TestUser2",
          email = "test2@example.com",
          dateOfBirth = "02/02/1999",
          country = "France",
          interests = listOf("Running", "Swimming"),
          bio = "Test bio 2",
          photoUrl = "https://example.com/photo2.jpg")

  @Before
  fun setup() {
    mockContext = mockk(relaxed = true)
    mockFirestoreRepo = mockk(relaxed = true)
    mockNetworkMonitor = mockk(relaxed = true)
    mockDatabase = mockk(relaxed = true)
    mockProfileDao = mockk(relaxed = true)

    // Mock AppDatabase.getDatabase to return our mock
    mockkObject(AppDatabase.Companion)
    every { AppDatabase.getDatabase(any()) } returns mockDatabase
    every { mockDatabase.profileDao() } returns mockProfileDao

    cachedRepository =
        ProfileRepositoryCached(
            context = mockContext,
            firestoreRepo = mockFirestoreRepo,
            networkMonitor = mockNetworkMonitor)
  }

  @After
  fun tearDown() {
    clearAllMocks()
    unmockkObject(AppDatabase.Companion)
  }

  // ==================== getProfile Tests ====================

  @Test
  fun `getProfile returns profile from Firestore when online and caches it`() = runTest {
    // Given
    every { mockNetworkMonitor.isOnline() } returns true
    coEvery { mockFirestoreRepo.getProfile(testUid) } returns testProfile
    coEvery { mockProfileDao.deleteProfile(testUid) } just Runs
    coEvery { mockProfileDao.insertProfile(any()) } just Runs

    // When
    val result = cachedRepository.getProfile(testUid)

    // Then
    assertNotNull(result)
    assertEquals(testProfile.uid, result?.uid)
    assertEquals(testProfile.username, result?.username)

    // Verify Firestore was called
    coVerify { mockFirestoreRepo.getProfile(testUid) }
    // Verify cache was updated
    coVerify { mockProfileDao.deleteProfile(testUid) }
    coVerify { mockProfileDao.insertProfile(any()) }
  }

  @Test
  fun `getProfile returns cached profile when online but Firestore fails`() = runTest {
    // Given
    val cachedEntity = testProfile.toEntity()
    every { mockNetworkMonitor.isOnline() } returns true
    coEvery { mockFirestoreRepo.getProfile(testUid) } throws Exception("Network error")
    coEvery { mockProfileDao.getProfileById(testUid) } returns cachedEntity

    // When
    val result = cachedRepository.getProfile(testUid)

    // Then
    assertNotNull(result)
    assertEquals(testProfile.uid, result?.uid)
    assertEquals(testProfile.username, result?.username)

    // Verify Firestore was attempted
    coVerify { mockFirestoreRepo.getProfile(testUid) }
    // Verify fallback to cache
    coVerify { mockProfileDao.getProfileById(testUid) }
  }

  @Test
  fun `getProfile returns cached profile when offline`() = runTest {
    // Given
    val cachedEntity = testProfile.toEntity()
    every { mockNetworkMonitor.isOnline() } returns false
    coEvery { mockProfileDao.getProfileById(testUid) } returns cachedEntity

    // When
    val result = cachedRepository.getProfile(testUid)

    // Then
    assertNotNull(result)
    assertEquals(testProfile.uid, result?.uid)
    assertEquals(testProfile.username, result?.username)

    // Verify Firestore was NOT called
    coVerify(exactly = 0) { mockFirestoreRepo.getProfile(any()) }
    // Verify cache was used
    coVerify { mockProfileDao.getProfileById(testUid) }
  }

  @Test
  fun `getProfile returns null when offline and no cache`() = runTest {
    // Given
    every { mockNetworkMonitor.isOnline() } returns false
    coEvery { mockProfileDao.getProfileById(testUid) } returns null

    // When
    val result = cachedRepository.getProfile(testUid)

    // Then
    assertNull(result)
    coVerify { mockProfileDao.getProfileById(testUid) }
  }

  @Test
  fun `getProfile returns null when online and Firestore returns null`() = runTest {
    // Given
    every { mockNetworkMonitor.isOnline() } returns true
    coEvery { mockFirestoreRepo.getProfile(testUid) } returns null

    // When
    val result = cachedRepository.getProfile(testUid)

    // Then
    assertNull(result)
    coVerify { mockFirestoreRepo.getProfile(testUid) }
    // Cache should NOT be called when Firestore successfully returns null
    coVerify(exactly = 0) { mockProfileDao.getProfileById(any()) }
  }

  @Test
  fun `getProfile does not cache when Firestore returns null`() = runTest {
    // Given
    every { mockNetworkMonitor.isOnline() } returns true
    coEvery { mockFirestoreRepo.getProfile(testUid) } returns null
    coEvery { mockProfileDao.getProfileById(testUid) } returns null

    // When
    cachedRepository.getProfile(testUid)

    // Then
    // Verify cache was NOT updated
    coVerify(exactly = 0) { mockProfileDao.insertProfile(any()) }
    coVerify(exactly = 0) { mockProfileDao.deleteProfile(any()) }
  }

  // ==================== getProfilesByIds Tests ====================

  @Test
  fun `getProfilesByIds returns profiles from Firestore when online and caches them`() = runTest {
    // Given
    val uids = listOf(testUid, testUid2)
    val profiles = listOf(testProfile, testProfile2)
    every { mockNetworkMonitor.isOnline() } returns true
    coEvery { mockFirestoreRepo.getProfilesByIds(uids) } returns profiles
    coEvery { mockProfileDao.deleteProfile(any()) } just Runs
    coEvery { mockProfileDao.insertProfiles(any()) } just Runs

    // When
    val result = cachedRepository.getProfilesByIds(uids)

    // Then
    assertNotNull(result)
    assertEquals(2, result?.size)
    assertEquals(testProfile.uid, result?.get(0)?.uid)
    assertEquals(testProfile2.uid, result?.get(1)?.uid)

    // Verify Firestore was called
    coVerify { mockFirestoreRepo.getProfilesByIds(uids) }
    // Verify cache was updated
    coVerify { mockProfileDao.deleteProfile(testUid) }
    coVerify { mockProfileDao.deleteProfile(testUid2) }
    coVerify { mockProfileDao.insertProfiles(any()) }
  }

  @Test
  fun `getProfilesByIds returns cached profiles when offline`() = runTest {
    // Given
    val uids = listOf(testUid, testUid2)
    val cachedEntity1 = testProfile.toEntity()
    val cachedEntity2 = testProfile2.toEntity()
    every { mockNetworkMonitor.isOnline() } returns false
    coEvery { mockProfileDao.getProfileById(testUid) } returns cachedEntity1
    coEvery { mockProfileDao.getProfileById(testUid2) } returns cachedEntity2

    // When
    val result = cachedRepository.getProfilesByIds(uids)

    // Then
    assertNotNull(result)
    assertEquals(2, result?.size)

    // Verify Firestore was NOT called
    coVerify(exactly = 0) { mockFirestoreRepo.getProfilesByIds(any()) }
    // Verify cache was used
    coVerify { mockProfileDao.getProfileById(testUid) }
    coVerify { mockProfileDao.getProfileById(testUid2) }
  }

  @Test
  fun `getProfilesByIds returns null when offline and no cached profiles`() = runTest {
    // Given
    val uids = listOf(testUid, testUid2)
    every { mockNetworkMonitor.isOnline() } returns false
    coEvery { mockProfileDao.getProfileById(any()) } returns null

    // When
    val result = cachedRepository.getProfilesByIds(uids)

    // Then
    assertNull(result)
  }

  @Test
  fun `getProfilesByIds returns partial cached profiles when offline`() = runTest {
    // Given
    val uids = listOf(testUid, testUid2)
    val cachedEntity1 = testProfile.toEntity()
    every { mockNetworkMonitor.isOnline() } returns false
    coEvery { mockProfileDao.getProfileById(testUid) } returns cachedEntity1
    coEvery { mockProfileDao.getProfileById(testUid2) } returns null

    // When
    val result = cachedRepository.getProfilesByIds(uids)

    // Then
    assertNotNull(result)
    assertEquals(1, result?.size)
    assertEquals(testProfile.uid, result?.get(0)?.uid)
  }

  @Test
  fun `getProfilesByIds returns cached profiles when Firestore fails`() = runTest {
    // Given
    val uids = listOf(testUid, testUid2)
    val cachedEntity1 = testProfile.toEntity()
    val cachedEntity2 = testProfile2.toEntity()
    every { mockNetworkMonitor.isOnline() } returns true
    coEvery { mockFirestoreRepo.getProfilesByIds(uids) } throws Exception("Network error")
    coEvery { mockProfileDao.getProfileById(testUid) } returns cachedEntity1
    coEvery { mockProfileDao.getProfileById(testUid2) } returns cachedEntity2

    // When
    val result = cachedRepository.getProfilesByIds(uids)

    // Then
    assertNotNull(result)
    assertEquals(2, result?.size)

    // Verify Firestore was attempted
    coVerify { mockFirestoreRepo.getProfilesByIds(uids) }
    // Verify fallback to cache
    coVerify { mockProfileDao.getProfileById(testUid) }
    coVerify { mockProfileDao.getProfileById(testUid2) }
  }

  // ==================== createOrUpdateProfile Tests ====================

  @Test
  fun `createOrUpdateProfile succeeds when online and updates cache`() = runTest {
    // Given
    every { mockNetworkMonitor.isOnline() } returns true
    coEvery { mockFirestoreRepo.createOrUpdateProfile(testProfile) } just Runs
    coEvery { mockProfileDao.insertProfile(any()) } just Runs

    // When
    cachedRepository.createOrUpdateProfile(testProfile)

    // Then
    coVerify { mockFirestoreRepo.createOrUpdateProfile(testProfile) }
    coVerify { mockProfileDao.insertProfile(any()) }
  }

  @Test(expected = OfflineException::class)
  fun `createOrUpdateProfile throws OfflineException when offline`() = runTest {
    // Given
    every { mockNetworkMonitor.isOnline() } returns false

    // When
    cachedRepository.createOrUpdateProfile(testProfile)

    // Then - exception is thrown
  }

  @Test
  fun `createOrUpdateProfile does not update cache when Firestore fails`() = runTest {
    // Given
    every { mockNetworkMonitor.isOnline() } returns true
    coEvery { mockFirestoreRepo.createOrUpdateProfile(testProfile) } throws
        Exception("Firestore error")

    // When/Then
    try {
      cachedRepository.createOrUpdateProfile(testProfile)
      fail("Expected exception to be thrown")
    } catch (e: Exception) {
      // Verify cache was NOT updated
      coVerify(exactly = 0) { mockProfileDao.insertProfile(any()) }
    }
  }

  // ==================== deleteProfile Tests ====================

  @Test
  fun `deleteProfile succeeds when online and removes from cache`() = runTest {
    // Given
    every { mockNetworkMonitor.isOnline() } returns true
    coEvery { mockFirestoreRepo.deleteProfile(testUid) } just Runs
    coEvery { mockProfileDao.deleteProfile(testUid) } just Runs

    // When
    cachedRepository.deleteProfile(testUid)

    // Then
    coVerify { mockFirestoreRepo.deleteProfile(testUid) }
    coVerify { mockProfileDao.deleteProfile(testUid) }
  }

  @Test(expected = OfflineException::class)
  fun `deleteProfile throws OfflineException when offline`() = runTest {
    // Given
    every { mockNetworkMonitor.isOnline() } returns false

    // When
    cachedRepository.deleteProfile(testUid)

    // Then - exception is thrown
  }

  // ==================== uploadProfilePhoto Tests ====================

  @Test
  fun `uploadProfilePhoto succeeds when online and refreshes cache`() = runTest {
    // Given
    val mockUri = mockk<Uri>(relaxed = true)
    val photoUrl = "https://example.com/new-photo.jpg"
    val updatedProfile = testProfile.copy(photoUrl = photoUrl)

    every { mockNetworkMonitor.isOnline() } returns true
    coEvery { mockFirestoreRepo.uploadProfilePhoto(mockContext, testUid, mockUri) } returns photoUrl
    coEvery { mockFirestoreRepo.getProfile(testUid) } returns updatedProfile
    coEvery { mockProfileDao.insertProfile(any()) } just Runs

    // When
    val result = cachedRepository.uploadProfilePhoto(mockContext, testUid, mockUri)

    // Then
    assertEquals(photoUrl, result)
    coVerify { mockFirestoreRepo.uploadProfilePhoto(mockContext, testUid, mockUri) }
    coVerify { mockFirestoreRepo.getProfile(testUid) }
    coVerify { mockProfileDao.insertProfile(any()) }
  }

  @Test(expected = OfflineException::class)
  fun `uploadProfilePhoto throws OfflineException when offline`() = runTest {
    // Given
    val mockUri = mockk<Uri>(relaxed = true)
    every { mockNetworkMonitor.isOnline() } returns false

    // When
    cachedRepository.uploadProfilePhoto(mockContext, testUid, mockUri)

    // Then - exception is thrown
  }

  @Test
  fun `uploadProfilePhoto succeeds even if cache refresh fails`() = runTest {
    // Given
    val mockUri = mockk<Uri>(relaxed = true)
    val photoUrl = "https://example.com/new-photo.jpg"

    every { mockNetworkMonitor.isOnline() } returns true
    coEvery { mockFirestoreRepo.uploadProfilePhoto(mockContext, testUid, mockUri) } returns photoUrl
    coEvery { mockFirestoreRepo.getProfile(testUid) } throws Exception("Failed to get profile")

    // When
    val result = cachedRepository.uploadProfilePhoto(mockContext, testUid, mockUri)

    // Then - Should still return photoUrl even if refresh fails
    assertEquals(photoUrl, result)
    coVerify { mockFirestoreRepo.uploadProfilePhoto(mockContext, testUid, mockUri) }
    coVerify { mockFirestoreRepo.getProfile(testUid) }
  }

  // ==================== deleteProfilePhoto Tests ====================

  @Test
  fun `deleteProfilePhoto succeeds when online and refreshes cache`() = runTest {
    // Given
    val updatedProfile = testProfile.copy(photoUrl = null)

    every { mockNetworkMonitor.isOnline() } returns true
    coEvery { mockFirestoreRepo.deleteProfilePhoto(testUid) } just Runs
    coEvery { mockFirestoreRepo.getProfile(testUid) } returns updatedProfile
    coEvery { mockProfileDao.insertProfile(any()) } just Runs

    // When
    cachedRepository.deleteProfilePhoto(testUid)

    // Then
    coVerify { mockFirestoreRepo.deleteProfilePhoto(testUid) }
    coVerify { mockFirestoreRepo.getProfile(testUid) }
    coVerify { mockProfileDao.insertProfile(any()) }
  }

  @Test(expected = OfflineException::class)
  fun `deleteProfilePhoto throws OfflineException when offline`() = runTest {
    // Given
    every { mockNetworkMonitor.isOnline() } returns false

    // When
    cachedRepository.deleteProfilePhoto(testUid)

    // Then - exception is thrown
  }

  // ==================== followUser Tests ====================

  @Test
  fun `followUser succeeds when online and refreshes both profiles in cache`() = runTest {
    // Given
    val followerId = testUid
    val followedId = testUid2
    val followerProfile = testProfile.copy(followingCount = 16)
    val followedProfile = testProfile2.copy(followersCount = 11)

    every { mockNetworkMonitor.isOnline() } returns true
    coEvery { mockFirestoreRepo.followUser(followerId, followedId) } just Runs
    coEvery { mockFirestoreRepo.getProfile(followerId) } returns followerProfile
    coEvery { mockFirestoreRepo.getProfile(followedId) } returns followedProfile
    coEvery { mockProfileDao.insertProfile(any()) } just Runs

    // When
    cachedRepository.followUser(followerId, followedId)

    // Then
    coVerify { mockFirestoreRepo.followUser(followerId, followedId) }
    coVerify { mockFirestoreRepo.getProfile(followerId) }
    coVerify { mockFirestoreRepo.getProfile(followedId) }
    coVerify(exactly = 2) { mockProfileDao.insertProfile(any()) }
  }

  @Test(expected = OfflineException::class)
  fun `followUser throws OfflineException when offline`() = runTest {
    // Given
    every { mockNetworkMonitor.isOnline() } returns false

    // When
    cachedRepository.followUser(testUid, testUid2)

    // Then - exception is thrown
  }

  @Test
  fun `followUser succeeds even if cache refresh fails`() = runTest {
    // Given
    val followerId = testUid
    val followedId = testUid2

    every { mockNetworkMonitor.isOnline() } returns true
    coEvery { mockFirestoreRepo.followUser(followerId, followedId) } just Runs
    coEvery { mockFirestoreRepo.getProfile(any()) } throws Exception("Failed to get profile")

    // When
    cachedRepository.followUser(followerId, followedId)

    // Then - Should succeed even if refresh fails
    coVerify { mockFirestoreRepo.followUser(followerId, followedId) }
  }

  // ==================== unfollowUser Tests ====================

  @Test
  fun `unfollowUser succeeds when online and refreshes both profiles in cache`() = runTest {
    // Given
    val followerId = testUid
    val followedId = testUid2
    val followerProfile = testProfile.copy(followingCount = 14)
    val followedProfile = testProfile2.copy(followersCount = 9)

    every { mockNetworkMonitor.isOnline() } returns true
    coEvery { mockFirestoreRepo.unfollowUser(followerId, followedId) } just Runs
    coEvery { mockFirestoreRepo.getProfile(followerId) } returns followerProfile
    coEvery { mockFirestoreRepo.getProfile(followedId) } returns followedProfile
    coEvery { mockProfileDao.insertProfile(any()) } just Runs

    // When
    cachedRepository.unfollowUser(followerId, followedId)

    // Then
    coVerify { mockFirestoreRepo.unfollowUser(followerId, followedId) }
    coVerify { mockFirestoreRepo.getProfile(followerId) }
    coVerify { mockFirestoreRepo.getProfile(followedId) }
    coVerify(exactly = 2) { mockProfileDao.insertProfile(any()) }
  }

  @Test(expected = OfflineException::class)
  fun `unfollowUser throws OfflineException when offline`() = runTest {
    // Given
    every { mockNetworkMonitor.isOnline() } returns false

    // When
    cachedRepository.unfollowUser(testUid, testUid2)

    // Then - exception is thrown
  }

  // ==================== isFollowing Tests ====================

  @Test
  fun `isFollowing returns true when online and user is following`() = runTest {
    // Given
    every { mockNetworkMonitor.isOnline() } returns true
    coEvery { mockFirestoreRepo.isFollowing(testUid, testUid2) } returns true

    // When
    val result = cachedRepository.isFollowing(testUid, testUid2)

    // Then
    assertTrue(result)
    coVerify { mockFirestoreRepo.isFollowing(testUid, testUid2) }
  }

  @Test
  fun `isFollowing returns false when online and user is not following`() = runTest {
    // Given
    every { mockNetworkMonitor.isOnline() } returns true
    coEvery { mockFirestoreRepo.isFollowing(testUid, testUid2) } returns false

    // When
    val result = cachedRepository.isFollowing(testUid, testUid2)

    // Then
    assertFalse(result)
    coVerify { mockFirestoreRepo.isFollowing(testUid, testUid2) }
  }

  @Test(expected = OfflineException::class)
  fun `isFollowing throws OfflineException when offline`() = runTest {
    // Given
    every { mockNetworkMonitor.isOnline() } returns false

    // When
    cachedRepository.isFollowing(testUid, testUid2)

    // Then - exception is thrown
  }

  // ==================== getFollowing Tests ====================

  @Test
  fun `getFollowing returns profiles from Firestore when online and caches them`() = runTest {
    // Given
    val profiles = listOf(testProfile, testProfile2)
    every { mockNetworkMonitor.isOnline() } returns true
    coEvery { mockFirestoreRepo.getFollowing(testUid, 10) } returns profiles
    coEvery { mockProfileDao.insertProfiles(any()) } just Runs

    // When
    val result = cachedRepository.getFollowing(testUid, 10)

    // Then
    assertEquals(2, result.size)
    coVerify { mockFirestoreRepo.getFollowing(testUid, 10) }
    coVerify { mockProfileDao.insertProfiles(any()) }
  }

  @Test
  fun `getFollowing returns empty list when offline`() = runTest {
    // Given
    every { mockNetworkMonitor.isOnline() } returns false

    // When
    val result = cachedRepository.getFollowing(testUid, 10)

    // Then
    assertTrue(result.isEmpty())
    coVerify(exactly = 0) { mockFirestoreRepo.getFollowing(any(), any()) }
  }

  @Test
  fun `getFollowing returns empty list when Firestore fails`() = runTest {
    // Given
    every { mockNetworkMonitor.isOnline() } returns true
    coEvery { mockFirestoreRepo.getFollowing(testUid, 10) } throws Exception("Network error")

    // When
    val result = cachedRepository.getFollowing(testUid, 10)

    // Then
    assertTrue(result.isEmpty())
    coVerify { mockFirestoreRepo.getFollowing(testUid, 10) }
  }

  @Test
  fun `getFollowing does not cache when list is empty`() = runTest {
    // Given
    every { mockNetworkMonitor.isOnline() } returns true
    coEvery { mockFirestoreRepo.getFollowing(testUid, 10) } returns emptyList()

    // When
    val result = cachedRepository.getFollowing(testUid, 10)

    // Then
    assertTrue(result.isEmpty())
    coVerify(exactly = 0) { mockProfileDao.insertProfiles(any()) }
  }

  // ==================== getFollowers Tests ====================

  @Test
  fun `getFollowers returns profiles from Firestore when online and caches them`() = runTest {
    // Given
    val profiles = listOf(testProfile, testProfile2)
    every { mockNetworkMonitor.isOnline() } returns true
    coEvery { mockFirestoreRepo.getFollowers(testUid, 10) } returns profiles
    coEvery { mockProfileDao.insertProfiles(any()) } just Runs

    // When
    val result = cachedRepository.getFollowers(testUid, 10)

    // Then
    assertEquals(2, result.size)
    coVerify { mockFirestoreRepo.getFollowers(testUid, 10) }
    coVerify { mockProfileDao.insertProfiles(any()) }
  }

  @Test
  fun `getFollowers returns empty list when offline`() = runTest {
    // Given
    every { mockNetworkMonitor.isOnline() } returns false

    // When
    val result = cachedRepository.getFollowers(testUid, 10)

    // Then
    assertTrue(result.isEmpty())
    coVerify(exactly = 0) { mockFirestoreRepo.getFollowers(any(), any()) }
  }

  @Test
  fun `getFollowers returns empty list when Firestore fails`() = runTest {
    // Given
    every { mockNetworkMonitor.isOnline() } returns true
    coEvery { mockFirestoreRepo.getFollowers(testUid, 10) } throws Exception("Network error")

    // When
    val result = cachedRepository.getFollowers(testUid, 10)

    // Then
    assertTrue(result.isEmpty())
    coVerify { mockFirestoreRepo.getFollowers(testUid, 10) }
  }

  // ==================== getMutualFollowing Tests ====================

  @Test
  fun `getMutualFollowing returns profiles from Firestore when online and caches them`() = runTest {
    // Given
    val profiles = listOf(testProfile, testProfile2)
    every { mockNetworkMonitor.isOnline() } returns true
    coEvery { mockFirestoreRepo.getMutualFollowing(testUid, testUid2) } returns profiles
    coEvery { mockProfileDao.insertProfiles(any()) } just Runs

    // When
    val result = cachedRepository.getMutualFollowing(testUid, testUid2)

    // Then
    assertEquals(2, result.size)
    coVerify { mockFirestoreRepo.getMutualFollowing(testUid, testUid2) }
    coVerify { mockProfileDao.insertProfiles(any()) }
  }

  @Test
  fun `getMutualFollowing returns empty list when offline`() = runTest {
    // Given
    every { mockNetworkMonitor.isOnline() } returns false

    // When
    val result = cachedRepository.getMutualFollowing(testUid, testUid2)

    // Then
    assertTrue(result.isEmpty())
    coVerify(exactly = 0) { mockFirestoreRepo.getMutualFollowing(any(), any()) }
  }

  @Test
  fun `getMutualFollowing returns empty list when Firestore fails`() = runTest {
    // Given
    every { mockNetworkMonitor.isOnline() } returns true
    coEvery { mockFirestoreRepo.getMutualFollowing(testUid, testUid2) } throws
        Exception("Network error")

    // When
    val result = cachedRepository.getMutualFollowing(testUid, testUid2)

    // Then
    assertTrue(result.isEmpty())
    coVerify { mockFirestoreRepo.getMutualFollowing(testUid, testUid2) }
  }

  // ==================== Timeout Tests ====================

  @Test
  fun `getProfile falls back to cache when Firestore times out`() = runTest {
    // Given
    val cachedEntity = testProfile.toEntity()
    every { mockNetworkMonitor.isOnline() } returns true
    coEvery { mockFirestoreRepo.getProfile(testUid) } coAnswers
        {
          kotlinx.coroutines.delay(5000)
          null
        }
    coEvery { mockProfileDao.getProfileById(testUid) } returns cachedEntity

    // When
    val result = cachedRepository.getProfile(testUid)

    // Then
    assertNotNull(result)
    assertEquals(testProfile.uid, result?.uid)
    // Verify fallback to cache
    coVerify { mockProfileDao.getProfileById(testUid) }
  }
}
