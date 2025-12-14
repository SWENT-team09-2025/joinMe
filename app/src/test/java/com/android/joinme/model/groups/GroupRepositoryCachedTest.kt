package com.android.joinme.model.groups

// Tests partially written with AI assistance; reviewed for correctness.

import android.content.Context
import android.net.Uri
import com.android.joinme.model.database.AppDatabase
import com.android.joinme.model.database.GroupDao
import com.android.joinme.model.database.toEntity
import com.android.joinme.model.event.EventType
import com.android.joinme.model.event.OfflineException
import com.android.joinme.network.NetworkMonitor
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class GroupRepositoryCachedTest {

  private lateinit var mockContext: Context
  private lateinit var mockFirestoreRepo: GroupRepository
  private lateinit var mockNetworkMonitor: NetworkMonitor
  private lateinit var mockDatabase: AppDatabase
  private lateinit var mockGroupDao: GroupDao
  private lateinit var cachedRepository: GroupRepositoryCached

  private val testGroupId = "test-group-123"
  private val testGroupId2 = "test-group-456"
  private val testUserId = "test-user-123"

  private val testGroup =
      Group(
          id = testGroupId,
          name = "Test Group",
          category = EventType.SPORTS,
          description = "A test group",
          ownerId = "owner-123",
          memberIds = listOf("member1", "member2", testUserId),
          eventIds = listOf("event1", "event2"),
          serieIds = listOf("serie1"),
          photoUrl = "https://example.com/photo.jpg")

  private val testGroup2 =
      Group(
          id = testGroupId2,
          name = "Test Group 2",
          category = EventType.ACTIVITY,
          description = "Another test group",
          ownerId = "owner-456",
          memberIds = listOf("member3", "member4"),
          eventIds = emptyList(),
          serieIds = emptyList(),
          photoUrl = null)

  @Before
  fun setup() {
    mockContext = mockk(relaxed = true)
    mockFirestoreRepo = mockk(relaxed = true)
    mockNetworkMonitor = mockk(relaxed = true)
    mockDatabase = mockk(relaxed = true)
    mockGroupDao = mockk(relaxed = true)

    // Mock AppDatabase.getDatabase to return our mock
    mockkObject(AppDatabase.Companion)
    every { AppDatabase.getDatabase(any()) } returns mockDatabase
    every { mockDatabase.groupDao() } returns mockGroupDao

    // Mock Firebase Auth
    mockkStatic(FirebaseAuth::class)
    val mockAuth = mockk<FirebaseAuth>()
    val mockUser = mockk<FirebaseUser>()
    every { FirebaseAuth.getInstance() } returns mockAuth
    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns testUserId

    cachedRepository =
        GroupRepositoryCached(
            context = mockContext,
            firestoreRepo = mockFirestoreRepo,
            networkMonitor = mockNetworkMonitor)
  }

  @After
  fun tearDown() {
    clearAllMocks()
    unmockkObject(AppDatabase.Companion)
    unmockkStatic(FirebaseAuth::class)
  }

  // ==================== getNewGroupId Tests ====================

  @Test
  fun `getNewGroupId delegates to Firestore repository`() {
    // Given
    every { mockFirestoreRepo.getNewGroupId() } returns "new-group-id"

    // When
    val result = cachedRepository.getNewGroupId()

    // Then
    assertEquals("new-group-id", result)
    verify { mockFirestoreRepo.getNewGroupId() }
  }

  // ==================== getAllGroups Tests ====================

  @Test
  fun `getAllGroups returns groups from Firestore when online and clears cache`() = runTest {
    // Given
    val groups = listOf(testGroup, testGroup2)
    every { mockNetworkMonitor.isOnline() } returns true
    coEvery { mockFirestoreRepo.getAllGroups() } returns groups
    coEvery { mockGroupDao.deleteAllGroups() } just Runs
    coEvery { mockGroupDao.insertGroups(any()) } just Runs

    // When
    val result = cachedRepository.getAllGroups()

    // Then
    assertEquals(2, result.size)
    coVerify { mockFirestoreRepo.getAllGroups() }
    coVerify { mockGroupDao.deleteAllGroups() }
    coVerify { mockGroupDao.insertGroups(any()) }
  }

  @Test
  fun `getAllGroups returns cached groups for current user when offline`() = runTest {
    // Given
    val cachedEntity1 = testGroup.toEntity()
    val cachedEntity2 = testGroup2.toEntity()
    every { mockNetworkMonitor.isOnline() } returns false
    coEvery { mockGroupDao.getAllGroups() } returns listOf(cachedEntity1, cachedEntity2)

    // When
    val result = cachedRepository.getAllGroups()

    // Then
    // Should only return testGroup because testUserId is in its memberIds
    assertEquals(1, result.size)
    assertEquals(testGroup.id, result[0].id)
    coVerify(exactly = 0) { mockFirestoreRepo.getAllGroups() }
    coVerify { mockGroupDao.getAllGroups() }
  }

  @Test
  fun `getAllGroups clears cache even when Firestore returns empty list`() = runTest {
    // Given
    every { mockNetworkMonitor.isOnline() } returns true
    coEvery { mockFirestoreRepo.getAllGroups() } returns emptyList()
    coEvery { mockGroupDao.deleteAllGroups() } just Runs

    // When
    val result = cachedRepository.getAllGroups()

    // Then
    assertTrue(result.isEmpty())
    coVerify { mockGroupDao.deleteAllGroups() }
    coVerify(exactly = 0) { mockGroupDao.insertGroups(any()) }
  }

  @Test
  fun `getAllGroups returns cached groups when Firestore fails`() = runTest {
    // Given
    val cachedEntity = testGroup.toEntity()
    every { mockNetworkMonitor.isOnline() } returns true
    coEvery { mockFirestoreRepo.getAllGroups() } throws Exception("Network error")
    coEvery { mockGroupDao.getAllGroups() } returns listOf(cachedEntity)

    // When
    val result = cachedRepository.getAllGroups()

    // Then
    assertEquals(1, result.size)
    coVerify { mockFirestoreRepo.getAllGroups() }
    coVerify { mockGroupDao.getAllGroups() }
  }

  // ==================== getGroup Tests ====================

  @Test
  fun `getGroup returns group from Firestore when online and caches it`() = runTest {
    // Given
    every { mockNetworkMonitor.isOnline() } returns true
    coEvery { mockFirestoreRepo.getGroup(testGroupId) } returns testGroup
    coEvery { mockGroupDao.deleteGroup(testGroupId) } just Runs
    coEvery { mockGroupDao.insertGroup(any()) } just Runs

    // When
    val result = cachedRepository.getGroup(testGroupId)

    // Then
    assertEquals(testGroup.id, result.id)
    coVerify { mockFirestoreRepo.getGroup(testGroupId) }
    coVerify { mockGroupDao.deleteGroup(testGroupId) }
    coVerify { mockGroupDao.insertGroup(any()) }
  }

  @Test
  fun `getGroup returns cached group when offline`() = runTest {
    // Given
    val cachedEntity = testGroup.toEntity()
    every { mockNetworkMonitor.isOnline() } returns false
    coEvery { mockGroupDao.getGroupById(testGroupId) } returns cachedEntity

    // When
    val result = cachedRepository.getGroup(testGroupId)

    // Then
    assertEquals(testGroup.id, result.id)
    coVerify(exactly = 0) { mockFirestoreRepo.getGroup(any()) }
    coVerify { mockGroupDao.getGroupById(testGroupId) }
  }

  @Test(expected = OfflineException::class)
  fun `getGroup throws OfflineException when offline and no cache`() = runTest {
    // Given
    every { mockNetworkMonitor.isOnline() } returns false
    coEvery { mockGroupDao.getGroupById(testGroupId) } returns null

    // When
    cachedRepository.getGroup(testGroupId)

    // Then - exception is thrown
  }

  @Test
  fun `getGroup returns cached group when Firestore fails`() = runTest {
    // Given
    val cachedEntity = testGroup.toEntity()
    every { mockNetworkMonitor.isOnline() } returns true
    coEvery { mockFirestoreRepo.getGroup(testGroupId) } throws Exception("Network error")
    coEvery { mockGroupDao.getGroupById(testGroupId) } returns cachedEntity

    // When
    val result = cachedRepository.getGroup(testGroupId)

    // Then
    assertEquals(testGroup.id, result.id)
    coVerify { mockGroupDao.getGroupById(testGroupId) }
  }

  // ==================== addGroup Tests ====================

  @Test
  fun `addGroup succeeds when online and caches new group`() = runTest {
    // Given
    every { mockNetworkMonitor.isOnline() } returns true
    coEvery { mockFirestoreRepo.addGroup(testGroup) } just Runs
    coEvery { mockGroupDao.insertGroup(any()) } just Runs

    // When
    cachedRepository.addGroup(testGroup)

    // Then
    coVerify { mockFirestoreRepo.addGroup(testGroup) }
    coVerify { mockGroupDao.insertGroup(any()) }
  }

  @Test(expected = OfflineException::class)
  fun `addGroup throws OfflineException when offline`() = runTest {
    // Given
    every { mockNetworkMonitor.isOnline() } returns false

    // When
    cachedRepository.addGroup(testGroup)

    // Then - exception is thrown
  }

  // ==================== editGroup Tests ====================

  @Test
  fun `editGroup succeeds when online and updates cache`() = runTest {
    // Given
    every { mockNetworkMonitor.isOnline() } returns true
    coEvery { mockFirestoreRepo.editGroup(testGroupId, testGroup) } just Runs
    coEvery { mockGroupDao.insertGroup(any()) } just Runs

    // When
    cachedRepository.editGroup(testGroupId, testGroup)

    // Then
    coVerify { mockFirestoreRepo.editGroup(testGroupId, testGroup) }
    coVerify { mockGroupDao.insertGroup(any()) }
  }

  @Test(expected = OfflineException::class)
  fun `editGroup throws OfflineException when offline`() = runTest {
    // Given
    every { mockNetworkMonitor.isOnline() } returns false

    // When
    cachedRepository.editGroup(testGroupId, testGroup)

    // Then - exception is thrown
  }

  // ==================== deleteGroup Tests ====================

  @Test
  fun `deleteGroup succeeds when online and removes from cache`() = runTest {
    // Given
    every { mockNetworkMonitor.isOnline() } returns true
    coEvery { mockFirestoreRepo.deleteGroup(testGroupId, testUserId) } just Runs
    coEvery { mockGroupDao.deleteGroup(testGroupId) } just Runs

    // When
    cachedRepository.deleteGroup(testGroupId, testUserId)

    // Then
    coVerify { mockFirestoreRepo.deleteGroup(testGroupId, testUserId) }
    coVerify { mockGroupDao.deleteGroup(testGroupId) }
  }

  @Test(expected = OfflineException::class)
  fun `deleteGroup throws OfflineException when offline`() = runTest {
    // Given
    every { mockNetworkMonitor.isOnline() } returns false

    // When
    cachedRepository.deleteGroup(testGroupId, testUserId)

    // Then - exception is thrown
  }

  // ==================== leaveGroup Tests ====================

  @Test
  fun `leaveGroup succeeds when online and refreshes cache`() = runTest {
    // Given
    val updatedGroup = testGroup.copy(memberIds = listOf("member1", "member2"))
    every { mockNetworkMonitor.isOnline() } returns true
    coEvery { mockFirestoreRepo.leaveGroup(testGroupId, testUserId) } just Runs
    coEvery { mockFirestoreRepo.getGroup(testGroupId) } returns updatedGroup
    coEvery { mockGroupDao.insertGroup(any()) } just Runs

    // When
    cachedRepository.leaveGroup(testGroupId, testUserId)

    // Then
    coVerify { mockFirestoreRepo.leaveGroup(testGroupId, testUserId) }
    coVerify { mockFirestoreRepo.getGroup(testGroupId) }
    coVerify { mockGroupDao.insertGroup(any()) }
  }

  @Test
  fun `leaveGroup removes from cache when refresh fails`() = runTest {
    // Given
    every { mockNetworkMonitor.isOnline() } returns true
    coEvery { mockFirestoreRepo.leaveGroup(testGroupId, testUserId) } just Runs
    coEvery { mockFirestoreRepo.getGroup(testGroupId) } throws Exception("Failed to refresh")
    coEvery { mockGroupDao.deleteGroup(testGroupId) } just Runs

    // When
    cachedRepository.leaveGroup(testGroupId, testUserId)

    // Then
    coVerify { mockFirestoreRepo.leaveGroup(testGroupId, testUserId) }
    coVerify { mockGroupDao.deleteGroup(testGroupId) }
  }

  @Test(expected = OfflineException::class)
  fun `leaveGroup throws OfflineException when offline`() = runTest {
    // Given
    every { mockNetworkMonitor.isOnline() } returns false

    // When
    cachedRepository.leaveGroup(testGroupId, testUserId)

    // Then - exception is thrown
  }

  // ==================== joinGroup Tests ====================

  @Test
  fun `joinGroup succeeds when online and refreshes cache`() = runTest {
    // Given
    val updatedGroup = testGroup.copy(memberIds = listOf("member1", "member2", testUserId))
    every { mockNetworkMonitor.isOnline() } returns true
    coEvery { mockFirestoreRepo.joinGroup(testGroupId, testUserId) } just Runs
    coEvery { mockFirestoreRepo.getGroup(testGroupId) } returns updatedGroup
    coEvery { mockGroupDao.insertGroup(any()) } just Runs

    // When
    cachedRepository.joinGroup(testGroupId, testUserId)

    // Then
    coVerify { mockFirestoreRepo.joinGroup(testGroupId, testUserId) }
    coVerify { mockFirestoreRepo.getGroup(testGroupId) }
    coVerify { mockGroupDao.insertGroup(any()) }
  }

  @Test
  fun `joinGroup succeeds even when refresh fails`() = runTest {
    // Given
    every { mockNetworkMonitor.isOnline() } returns true
    coEvery { mockFirestoreRepo.joinGroup(testGroupId, testUserId) } just Runs
    coEvery { mockFirestoreRepo.getGroup(testGroupId) } throws Exception("Failed to refresh")

    // When
    cachedRepository.joinGroup(testGroupId, testUserId)

    // Then - Should succeed even though refresh failed
    coVerify { mockFirestoreRepo.joinGroup(testGroupId, testUserId) }
  }

  @Test(expected = OfflineException::class)
  fun `joinGroup throws OfflineException when offline`() = runTest {
    // Given
    every { mockNetworkMonitor.isOnline() } returns false

    // When
    cachedRepository.joinGroup(testGroupId, testUserId)

    // Then - exception is thrown
  }

  // ==================== getCommonGroups Tests ====================

  @Test
  fun `getCommonGroups returns groups from Firestore when online and caches them`() = runTest {
    // Given
    val userIds = listOf("user1", "user2")
    val groups = listOf(testGroup, testGroup2)
    every { mockNetworkMonitor.isOnline() } returns true
    coEvery { mockFirestoreRepo.getCommonGroups(userIds) } returns groups
    coEvery { mockGroupDao.insertGroups(any()) } just Runs

    // When
    val result = cachedRepository.getCommonGroups(userIds)

    // Then
    assertEquals(2, result.size)
    coVerify { mockFirestoreRepo.getCommonGroups(userIds) }
    coVerify { mockGroupDao.insertGroups(any()) }
  }

  @Test
  fun `getCommonGroups returns cached groups filtered by userIds when offline`() = runTest {
    // Given
    val userIds = listOf("member1", "member2")
    val groupWithBothMembers = testGroup.copy(memberIds = listOf("member1", "member2", "member3"))
    val groupWithOneMember = testGroup2.copy(memberIds = listOf("member1", "other"))
    val cachedEntity1 = groupWithBothMembers.toEntity()
    val cachedEntity2 = groupWithOneMember.toEntity()

    every { mockNetworkMonitor.isOnline() } returns false
    coEvery { mockGroupDao.getAllGroups() } returns listOf(cachedEntity1, cachedEntity2)

    // When
    val result = cachedRepository.getCommonGroups(userIds)

    // Then
    assertEquals(1, result.size)
    assertEquals(groupWithBothMembers.id, result[0].id)
    coVerify(exactly = 0) { mockFirestoreRepo.getCommonGroups(any()) }
  }

  @Test
  fun `getCommonGroups does not cache when Firestore returns empty list`() = runTest {
    // Given
    val userIds = listOf("user1", "user2")
    every { mockNetworkMonitor.isOnline() } returns true
    coEvery { mockFirestoreRepo.getCommonGroups(userIds) } returns emptyList()

    // When
    val result = cachedRepository.getCommonGroups(userIds)

    // Then
    assertTrue(result.isEmpty())
    coVerify(exactly = 0) { mockGroupDao.insertGroups(any()) }
  }

  // ==================== uploadGroupPhoto Tests ====================

  @Test
  fun `uploadGroupPhoto succeeds when online and refreshes cache`() = runTest {
    // Given
    val mockUri = mockk<Uri>(relaxed = true)
    val photoUrl = "https://example.com/new-photo.jpg"
    val updatedGroup = testGroup.copy(photoUrl = photoUrl)

    every { mockNetworkMonitor.isOnline() } returns true
    coEvery { mockFirestoreRepo.uploadGroupPhoto(mockContext, testGroupId, mockUri) } returns
        photoUrl
    coEvery { mockFirestoreRepo.getGroup(testGroupId) } returns updatedGroup
    coEvery { mockGroupDao.insertGroup(any()) } just Runs

    // When
    val result = cachedRepository.uploadGroupPhoto(mockContext, testGroupId, mockUri)

    // Then
    assertEquals(photoUrl, result)
    coVerify { mockFirestoreRepo.uploadGroupPhoto(mockContext, testGroupId, mockUri) }
    coVerify { mockGroupDao.insertGroup(any()) }
  }

  @Test(expected = OfflineException::class)
  fun `uploadGroupPhoto throws OfflineException when offline`() = runTest {
    // Given
    val mockUri = mockk<Uri>(relaxed = true)
    every { mockNetworkMonitor.isOnline() } returns false

    // When
    cachedRepository.uploadGroupPhoto(mockContext, testGroupId, mockUri)

    // Then - exception is thrown
  }

  // ==================== deleteGroupPhoto Tests ====================

  @Test
  fun `deleteGroupPhoto succeeds when online and refreshes cache`() = runTest {
    // Given
    val updatedGroup = testGroup.copy(photoUrl = null)
    every { mockNetworkMonitor.isOnline() } returns true
    coEvery { mockFirestoreRepo.deleteGroupPhoto(testGroupId) } just Runs
    coEvery { mockFirestoreRepo.getGroup(testGroupId) } returns updatedGroup
    coEvery { mockGroupDao.insertGroup(any()) } just Runs

    // When
    cachedRepository.deleteGroupPhoto(testGroupId)

    // Then
    coVerify { mockFirestoreRepo.deleteGroupPhoto(testGroupId) }
    coVerify { mockGroupDao.insertGroup(any()) }
  }

  @Test(expected = OfflineException::class)
  fun `deleteGroupPhoto throws OfflineException when offline`() = runTest {
    // Given
    every { mockNetworkMonitor.isOnline() } returns false

    // When
    cachedRepository.deleteGroupPhoto(testGroupId)

    // Then - exception is thrown
  }
}
