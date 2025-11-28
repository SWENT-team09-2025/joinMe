package com.android.joinme.model.profile

// Tests partially written with AI assistance; reviewed for correctness.

import android.content.Context
import android.net.Uri
import com.android.joinme.model.utils.ImageProcessor
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.firestore.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import io.mockk.*
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ProfileRepositoryFirestoreTest {

  private lateinit var mockFirestore: FirebaseFirestore
  private lateinit var mockStorage: FirebaseStorage
  private lateinit var mockContext: Context
  private lateinit var mockCollection: CollectionReference
  private lateinit var mockDocument: DocumentReference
  private lateinit var repository: ProfileRepositoryFirestore

  private val testUid = "test-uid-123"
  private val testProfile =
      Profile(
          uid = testUid,
          username = "TestUser",
          email = "test@example.com",
          dateOfBirth = "01/01/2000",
          country = "Switzerland",
          interests = listOf("Coding", "Testing"),
          bio = "Test bio",
          photoUrl = "https://example.com/photo.jpg")

  @Before
  fun setup() {
    mockFirestore = mockk(relaxed = true)
    mockStorage = mockk(relaxed = true)
    mockContext = mockk(relaxed = true)
    mockCollection = mockk(relaxed = true)
    mockDocument = mockk(relaxed = true)

    // Set up follows collection mocks early
    mockFollowsCollection = mockk(relaxed = true)
    mockFollowDocument = mockk(relaxed = true)
    mockQuery = mockk(relaxed = true)
    mockBatch = mockk(relaxed = true)

    every { mockFirestore.collection(PROFILES_COLLECTION_PATH) } returns mockCollection
    every { mockCollection.document(any()) } returns mockDocument
    every { mockFirestore.collection(FOLLOWS_COLLECTION_PATH) } returns mockFollowsCollection
    every { mockFirestore.batch() } returns mockBatch

    repository = ProfileRepositoryFirestore(db = mockFirestore, storage = mockStorage)

    // Mock the constructor for ImageProcessor
    mockkConstructor(ImageProcessor::class)

    // Mock the await() extension (used on UploadTask in tests)
    mockkStatic("kotlinx.coroutines.tasks.TasksKt")
  }

  @After
  fun tearDown() {
    clearAllMocks()
    unmockkConstructor(ImageProcessor::class)
    // Unmock the static await extension
    unmockkStatic("kotlinx.coroutines.tasks.TasksKt")
  }

  // ==================== GET & CRUD TESTS ====================

  @Test
  fun `getProfile returns profile when document exists`() = runTest {
    // Given
    val mockSnapshot = mockk<DocumentSnapshot>(relaxed = true)
    every { mockSnapshot.exists() } returns true
    every { mockSnapshot.id } returns testUid
    every { mockSnapshot.getString("username") } returns testProfile.username
    every { mockSnapshot.getString("email") } returns testProfile.email
    every { mockSnapshot.getString("dateOfBirth") } returns testProfile.dateOfBirth
    every { mockSnapshot.getString("country") } returns testProfile.country
    every { mockSnapshot.getString("bio") } returns testProfile.bio
    every { mockSnapshot.getString("photoUrl") } returns testProfile.photoUrl
    every { mockSnapshot.get("interests") } returns testProfile.interests
    every { mockSnapshot.getTimestamp("createdAt") } returns Timestamp.now()
    every { mockSnapshot.getTimestamp("updatedAt") } returns Timestamp.now()

    val mockTask = Tasks.forResult(mockSnapshot)
    every { mockDocument.get() } returns mockTask

    // When
    val result = repository.getProfile(testUid)

    // Then
    assertNotNull(result)
    assertEquals(testUid, result.uid)
    assertEquals(testProfile.username, result.username)
    assertEquals(testProfile.email, result.email)
    verify { mockCollection.document(testUid) }
    verify { mockDocument.get() }
  }

  @Test(expected = NoSuchElementException::class)
  fun `getProfile throws NoSuchElementException when document does not exist`() = runTest {
    // Given
    val mockSnapshot = mockk<DocumentSnapshot>(relaxed = true)
    every { mockSnapshot.exists() } returns false

    val mockTask = Tasks.forResult(mockSnapshot)
    every { mockDocument.get() } returns mockTask

    // When
    repository.getProfile(testUid)

    // Then - exception is thrown
  }

  @Test(expected = NoSuchElementException::class)
  fun `getProfile throws NoSuchElementException when required fields are missing`() = runTest {
    // Given
    val mockSnapshot = mockk<DocumentSnapshot>(relaxed = true)
    every { mockSnapshot.exists() } returns true
    every { mockSnapshot.id } returns testUid
    every { mockSnapshot.getString("username") } returns "" // Empty username
    every { mockSnapshot.getString("email") } returns testProfile.email

    val mockTask = Tasks.forResult(mockSnapshot)
    every { mockDocument.get() } returns mockTask

    // When
    repository.getProfile(testUid)

    // Then - exception is thrown
  }

  @Test
  fun `createOrUpdateProfile creates new profile when document does not exist`() = runTest {
    // Given
    val mockSnapshot = mockk<DocumentSnapshot>(relaxed = true)
    every { mockSnapshot.exists() } returns false

    val getTask = Tasks.forResult(mockSnapshot)
    val setTask = Tasks.forResult<Void>(null)

    every { mockDocument.get() } returns getTask
    every { mockDocument.set(any<Map<String, Any?>>(), any<SetOptions>()) } returns setTask

    // When
    repository.createOrUpdateProfile(testProfile)

    // Then
    verify { mockDocument.get() }
    verify {
      mockDocument.set(
          match<Map<String, Any?>> { map ->
            map["username"] == testProfile.username &&
                map["email"] == testProfile.email &&
                map.containsKey("createdAt") &&
                map.containsKey("updatedAt")
          },
          any<SetOptions>())
    }
  }

  @Test
  fun `createOrUpdateProfile updates existing profile`() = runTest {
    // Given
    val mockSnapshot = mockk<DocumentSnapshot>(relaxed = true)
    every { mockSnapshot.exists() } returns true

    val getTask = Tasks.forResult(mockSnapshot)
    val setTask = Tasks.forResult<Void>(null)

    every { mockDocument.get() } returns getTask
    every { mockDocument.set(any<Map<String, Any?>>(), any<SetOptions>()) } returns setTask

    // When
    repository.createOrUpdateProfile(testProfile)

    // Then
    verify { mockDocument.get() }
    verify {
      mockDocument.set(
          match<Map<String, Any?>> { map ->
            map["username"] == testProfile.username &&
                !map.containsKey("email") && // Email should not be updated
                map.containsKey("updatedAt") &&
                !map.containsKey("createdAt") // createdAt should not be set again
          },
          any<SetOptions>())
    }
  }

  @Test
  fun `deleteProfile successfully deletes document`() = runTest {
    // Given
    val deleteTask = Tasks.forResult<Void>(null)
    every { mockDocument.delete() } returns deleteTask

    // When
    repository.deleteProfile(testUid)

    // Then
    verify { mockCollection.document(testUid) }
    verify { mockDocument.delete() }
  }

  @Test
  fun `getProfile handles null optional fields correctly`() = runTest {
    // Given
    val mockSnapshot = mockk<DocumentSnapshot>(relaxed = true)
    every { mockSnapshot.exists() } returns true
    every { mockSnapshot.id } returns testUid
    every { mockSnapshot.getString("username") } returns testProfile.username
    every { mockSnapshot.getString("email") } returns testProfile.email
    every { mockSnapshot.getString("dateOfBirth") } returns null
    every { mockSnapshot.getString("country") } returns null
    every { mockSnapshot.getString("bio") } returns null
    every { mockSnapshot.getString("photoUrl") } returns null
    every { mockSnapshot.get("interests") } returns emptyList<String>()
    every { mockSnapshot.getTimestamp("createdAt") } returns null
    every { mockSnapshot.getTimestamp("updatedAt") } returns null

    val mockTask = Tasks.forResult(mockSnapshot)
    every { mockDocument.get() } returns mockTask

    // When
    val result = repository.getProfile(testUid)

    // Then
    assertNotNull(result)
    assertNull(result.dateOfBirth)
    assertNull(result.country)
    assertNull(result.bio)
    assertNull(result.photoUrl)
    assertTrue(result.interests.isEmpty())
  }

  // ==================== GET PROFILES BY IDS TESTS ====================

  @Test
  fun `getProfilesByIds returns empty list for empty input`() = runTest {
    // When
    val result = repository.getProfilesByIds(emptyList())

    // Then
    assertNotNull(result)
    assertTrue(result!!.isEmpty())
  }

  @Test
  fun `getProfilesByIds returns profiles when all exist`() = runTest {
    // Given
    val mockSnapshot = mockk<DocumentSnapshot>(relaxed = true)
    every { mockSnapshot.exists() } returns true
    every { mockSnapshot.id } returns testUid
    every { mockSnapshot.getString("username") } returns testProfile.username
    every { mockSnapshot.getString("email") } returns testProfile.email
    every { mockSnapshot.getString("dateOfBirth") } returns testProfile.dateOfBirth
    every { mockSnapshot.getString("country") } returns testProfile.country
    every { mockSnapshot.getString("bio") } returns testProfile.bio
    every { mockSnapshot.getString("photoUrl") } returns testProfile.photoUrl
    every { mockSnapshot.get("interests") } returns testProfile.interests
    every { mockSnapshot.getTimestamp("createdAt") } returns Timestamp.now()
    every { mockSnapshot.getTimestamp("updatedAt") } returns Timestamp.now()

    val mockTask = Tasks.forResult(mockSnapshot)
    every { mockDocument.get() } returns mockTask

    // When
    val result = repository.getProfilesByIds(listOf(testUid))

    // Then
    assertNotNull(result)
    assertEquals(1, result!!.size)
    assertEquals(testUid, result[0].uid)
  }

  @Test
  fun `getProfilesByIds returns null when profile not found`() = runTest {
    // Given
    val mockSnapshot = mockk<DocumentSnapshot>(relaxed = true)
    every { mockSnapshot.exists() } returns false

    val mockTask = Tasks.forResult(mockSnapshot)
    every { mockDocument.get() } returns mockTask

    // When
    val result = repository.getProfilesByIds(listOf(testUid))

    // Then
    assertNull(result)
  }

  @Test
  fun `getProfilesByIds returns null when getProfile throws exception`() = runTest {
    // Given
    every { mockDocument.get() } returns Tasks.forException(Exception("Network error"))

    // When
    val result = repository.getProfilesByIds(listOf(testUid))

    // Then
    assertNull(result)
  }

  // ==================== PHOTO UPLOAD TESTS ====================

  private fun setupStorageMocks(
      mockStorageRef: StorageReference,
      expectedUid: String = testUid,
      expectedPath: String = "profile.jpg"
  ) {
    val mockRootRef = mockk<StorageReference>(relaxed = true)
    val mockUsersRef = mockk<StorageReference>(relaxed = true)
    val mockUserRef = mockk<StorageReference>(relaxed = true)

    every { mockStorage.reference } returns mockRootRef
    every { mockRootRef.child("users") } returns mockUsersRef
    every { mockUsersRef.child(expectedUid) } returns mockUserRef
    every { mockUserRef.child(expectedPath) } returns mockStorageRef
  }

  @Test(expected = Exception::class)
  fun `uploadProfilePhoto throws error if image processing fails`() = runTest {
    // Given
    val fakeImageUri = mockk<Uri>(relaxed = true)
    coEvery { anyConstructed<ImageProcessor>().processImage(fakeImageUri) } throws
        Exception("Processing failed")

    // When
    repository.uploadProfilePhoto(mockContext, testUid, fakeImageUri)

    // Then - exception is thrown
    // Verify no storage or firestore calls were made
    coVerify(exactly = 0) { mockStorage.reference.child(any()) }
    coVerify(exactly = 0) { mockDocument.update(any<Map<String, Any?>>()) }
  }

  @Test(expected = Exception::class)
  fun `uploadProfilePhoto throws error if storage upload fails`() = runTest {
    // Given
    val fakeImageUri = mockk<Uri>(relaxed = true)
    val fakeBytes = "fake-bytes".toByteArray()
    val mockStorageRef = mockk<StorageReference>(relaxed = true)

    coEvery { anyConstructed<ImageProcessor>().processImage(fakeImageUri) } returns fakeBytes
    setupStorageMocks(mockStorageRef)

    // Mock storage upload failure by stubbing await() to throw
    val mockUploadTask = mockk<UploadTask>(relaxed = true)
    coEvery { mockUploadTask.await() } throws Exception("Storage error")
    every { mockStorageRef.putBytes(fakeBytes) } returns mockUploadTask

    // When
    repository.uploadProfilePhoto(mockContext, testUid, fakeImageUri)

    // Then - exception is thrown
    // Verify Firestore was not updated
    coVerify(exactly = 0) { mockDocument.update(any<Map<String, Any?>>()) }
  }

  @Test(expected = Exception::class)
  fun `uploadProfilePhoto throws error if firestore update fails`() = runTest {
    // Given
    val fakeImageUri = mockk<Uri>(relaxed = true)
    val fakeBytes = "fake-bytes".toByteArray()
    val fakeDownloadUrl = "https://fake.url/photo.jpg"
    val mockStorageRef = mockk<StorageReference>(relaxed = true)
    val mockTaskSnapshot = mockk<UploadTask.TaskSnapshot>(relaxed = true)
    val mockMetadata = mockk<StorageMetadata>(relaxed = true)
    val mockDownloadUri = mockk<Uri>(relaxed = true)

    coEvery { anyConstructed<ImageProcessor>().processImage(fakeImageUri) } returns fakeBytes
    setupStorageMocks(mockStorageRef)
    // Use a mock UploadTask and stub await() to return the snapshot
    val mockUploadTask = mockk<UploadTask>(relaxed = true)
    coEvery { mockUploadTask.await() } returns mockTaskSnapshot
    every { mockStorageRef.putBytes(fakeBytes) } returns mockUploadTask
    every { mockTaskSnapshot.metadata } returns mockMetadata

    every { mockDownloadUri.toString() } returns fakeDownloadUrl
    every { mockStorageRef.downloadUrl } returns Tasks.forResult(mockDownloadUri)

    // Mock Firestore update failure
    every { mockDocument.set(any<Map<String, Any?>>(), any<SetOptions>()) } returns
        Tasks.forException(Exception("Firestore error"))

    // When
    repository.uploadProfilePhoto(mockContext, testUid, fakeImageUri)

    // Then - exception is thrown
  }

  @Test
  fun `deleteProfilePhoto storageFailure still updates Firestore`() = runTest {
    // Given
    val mockStorageRef = mockk<StorageReference>(relaxed = true)
    setupStorageMocks(mockStorageRef, testUid, "profile.jpg")

    // Mock Storage Delete failure
    every { mockStorageRef.delete() } returns Tasks.forException(Exception("File not found"))
    // Mock Firestore Update success
    every { mockDocument.set(any<Map<String, Any?>>(), any<SetOptions>()) } returns
        Tasks.forResult(null)

    // When
    repository.deleteProfilePhoto(testUid) // Should not throw an exception

    // Then
    // Verify storage delete was attempted
    verify { mockStorageRef.delete() }
    // Verify Firestore update was *still* called
    verify {
      mockDocument.set(
          match<Map<String, Any?>> {
            it["photoUrl"] == null && // Verify photoUrl is set to null
                it.containsKey("updatedAt") // Verify updatedAt is present
          },
          any<SetOptions>())
    }
  }

  @Test(expected = Exception::class)
  fun `deleteProfilePhoto throws error if firestore update fails`() = runTest {
    // Given
    val mockStorageRef = mockk<StorageReference>(relaxed = true)
    setupStorageMocks(mockStorageRef, testUid, "profile.jpg")

    // Mock Storage Delete success
    every { mockStorageRef.delete() } returns Tasks.forResult(null)
    // Mock Firestore Update failure
    every { mockDocument.set(any<Map<String, Any?>>(), any<SetOptions>()) } returns
        Tasks.forException(Exception("Firestore error"))

    // When
    repository.deleteProfilePhoto(testUid)

    // Then - exception is thrown
    // Verify storage delete was called
    verify { mockStorageRef.delete() }
    // Verify Firestore update was attempted
    verify { mockDocument.set(any(), any<SetOptions>()) }
  }

  // ==================== FOLLOWER TESTS ====================

  private lateinit var mockFollowsCollection: CollectionReference
  private lateinit var mockFollowDocument: DocumentReference
  private lateinit var mockQuery: Query
  private lateinit var mockBatch: WriteBatch

  private fun setupFollowerMocks() {
    every { mockFollowsCollection.document() } returns mockFollowDocument
    every { mockFollowDocument.id } returns "follow-doc-id"
    every { mockBatch.set(any(), any<Map<String, Any?>>()) } returns mockBatch
    every { mockBatch.update(any(), any<String>(), any()) } returns mockBatch
    every { mockBatch.delete(any()) } returns mockBatch
  }

  @Test
  fun `followUser successfully creates follow relationship`() = runTest {
    // Given
    setupFollowerMocks()
    val followerId = "user1"
    val followedId = "user2"

    // Mock isFollowing to return false (not already following)
    val mockQuerySnapshot = mockk<QuerySnapshot>(relaxed = true)
    every { mockQuerySnapshot.isEmpty } returns true
    every { mockFollowsCollection.whereEqualTo("followerId", followerId) } returns mockQuery
    every { mockQuery.whereEqualTo("followedId", followedId) } returns mockQuery
    every { mockQuery.limit(1) } returns mockQuery
    every { mockQuery.get() } returns Tasks.forResult(mockQuerySnapshot)

    // Mock follower and followed profile documents
    val mockFollowerDoc = mockk<DocumentReference>(relaxed = true)
    val mockFollowedDoc = mockk<DocumentReference>(relaxed = true)
    every { mockCollection.document(followerId) } returns mockFollowerDoc
    every { mockCollection.document(followedId) } returns mockFollowedDoc

    // Mock batch commit
    every { mockBatch.commit() } returns Tasks.forResult(null)

    // When
    repository.followUser(followerId, followedId)

    // Then
    verify { mockBatch.set(mockFollowDocument, any<Map<String, Any?>>()) }
    verify { mockBatch.update(mockFollowerDoc, "followingCount", any()) }
    verify { mockBatch.update(mockFollowedDoc, "followersCount", any()) }
    verify { mockBatch.commit() }
  }

  @Test
  fun `followUser throws exception when trying to follow yourself`() = runTest {
    // Given
    setupFollowerMocks()
    val userId = "user1"

    // When & Then
    try {
      repository.followUser(userId, userId)
      fail("Expected Exception to be thrown")
    } catch (e: Exception) {
      assertEquals("Cannot follow yourself", e.message)
    }
  }

  @Test
  fun `followUser throws exception when already following`() = runTest {
    // Given
    setupFollowerMocks()
    val followerId = "user1"
    val followedId = "user2"

    // Mock isFollowing to return true (already following)
    val mockQuerySnapshot = mockk<QuerySnapshot>(relaxed = true)
    every { mockQuerySnapshot.isEmpty } returns false
    every { mockFollowsCollection.whereEqualTo("followerId", followerId) } returns mockQuery
    every { mockQuery.whereEqualTo("followedId", followedId) } returns mockQuery
    every { mockQuery.limit(1) } returns mockQuery
    every { mockQuery.get() } returns Tasks.forResult(mockQuerySnapshot)

    // When & Then
    try {
      repository.followUser(followerId, followedId)
      fail("Expected Exception to be thrown")
    } catch (e: Exception) {
      assertEquals("Already following this user", e.message)
    }
  }

  @Test
  fun `unfollowUser successfully removes follow relationship`() = runTest {
    // Given
    setupFollowerMocks()
    val followerId = "user1"
    val followedId = "user2"

    // Mock finding the follow relationship
    val mockFollowDoc = mockk<DocumentSnapshot>(relaxed = true)
    val mockDocRef = mockk<DocumentReference>(relaxed = true)
    every { mockFollowDoc.reference } returns mockDocRef

    val mockQuerySnapshot = mockk<QuerySnapshot>(relaxed = true)
    every { mockQuerySnapshot.isEmpty } returns false
    every { mockQuerySnapshot.documents } returns listOf(mockFollowDoc)

    every { mockFollowsCollection.whereEqualTo("followerId", followerId) } returns mockQuery
    every { mockQuery.whereEqualTo("followedId", followedId) } returns mockQuery
    every { mockQuery.get() } returns Tasks.forResult(mockQuerySnapshot)

    // Mock follower and followed profile documents
    val mockFollowerDoc = mockk<DocumentReference>(relaxed = true)
    val mockFollowedDoc = mockk<DocumentReference>(relaxed = true)
    every { mockCollection.document(followerId) } returns mockFollowerDoc
    every { mockCollection.document(followedId) } returns mockFollowedDoc

    // Mock batch commit
    every { mockBatch.commit() } returns Tasks.forResult(null)

    // When
    repository.unfollowUser(followerId, followedId)

    // Then
    verify { mockBatch.delete(mockDocRef) }
    verify { mockBatch.update(mockFollowerDoc, "followingCount", any()) }
    verify { mockBatch.update(mockFollowedDoc, "followersCount", any()) }
    verify { mockBatch.commit() }
  }

  @Test
  fun `unfollowUser throws exception when not following`() = runTest {
    // Given
    setupFollowerMocks()
    val followerId = "user1"
    val followedId = "user2"

    // Mock not finding the follow relationship
    val mockQuerySnapshot = mockk<QuerySnapshot>(relaxed = true)
    every { mockQuerySnapshot.isEmpty } returns true

    every { mockFollowsCollection.whereEqualTo("followerId", followerId) } returns mockQuery
    every { mockQuery.whereEqualTo("followedId", followedId) } returns mockQuery
    every { mockQuery.get() } returns Tasks.forResult(mockQuerySnapshot)

    // When & Then
    try {
      repository.unfollowUser(followerId, followedId)
      fail("Expected Exception to be thrown")
    } catch (e: Exception) {
      assertEquals("Not currently following this user", e.message)
    }
  }

  @Test
  fun `isFollowing returns true when follow relationship exists`() = runTest {
    // Given
    setupFollowerMocks()
    val followerId = "user1"
    val followedId = "user2"

    val mockQuerySnapshot = mockk<QuerySnapshot>(relaxed = true)
    every { mockQuerySnapshot.isEmpty } returns false

    every { mockFollowsCollection.whereEqualTo("followerId", followerId) } returns mockQuery
    every { mockQuery.whereEqualTo("followedId", followedId) } returns mockQuery
    every { mockQuery.limit(1) } returns mockQuery
    every { mockQuery.get() } returns Tasks.forResult(mockQuerySnapshot)

    // When
    val result = repository.isFollowing(followerId, followedId)

    // Then
    assertTrue(result)
  }

  @Test
  fun `isFollowing returns false when follow relationship does not exist`() = runTest {
    // Given
    setupFollowerMocks()
    val followerId = "user1"
    val followedId = "user2"

    val mockQuerySnapshot = mockk<QuerySnapshot>(relaxed = true)
    every { mockQuerySnapshot.isEmpty } returns true

    every { mockFollowsCollection.whereEqualTo("followerId", followerId) } returns mockQuery
    every { mockQuery.whereEqualTo("followedId", followedId) } returns mockQuery
    every { mockQuery.limit(1) } returns mockQuery
    every { mockQuery.get() } returns Tasks.forResult(mockQuerySnapshot)

    // When
    val result = repository.isFollowing(followerId, followedId)

    // Then
    assertFalse(result)
  }

  @Test
  fun `getFollowing returns list of followed profiles`() = runTest {
    // Given
    setupFollowerMocks()
    val userId = "user1"
    val followedId1 = "user2"
    val followedId2 = "user3"

    // Mock follow documents
    val mockFollowDoc1 = mockk<QueryDocumentSnapshot>(relaxed = true)
    val mockFollowDoc2 = mockk<QueryDocumentSnapshot>(relaxed = true)
    every { mockFollowDoc1.getString("followedId") } returns followedId1
    every { mockFollowDoc2.getString("followedId") } returns followedId2

    val mockFollowsSnapshot = mockk<QuerySnapshot>(relaxed = true)
    every { mockFollowsSnapshot.documents } returns listOf(mockFollowDoc1, mockFollowDoc2)

    val mockOrderedQuery = mockk<Query>(relaxed = true)
    val mockLimitQuery = mockk<Query>(relaxed = true)

    every { mockFollowsCollection.whereEqualTo("followerId", userId) } returns mockQuery
    every { mockQuery.orderBy(any<FieldPath>(), any()) } returns mockOrderedQuery
    every { mockOrderedQuery.limit(50L) } returns mockLimitQuery
    every { mockLimitQuery.get() } returns Tasks.forResult(mockFollowsSnapshot)

    // Mock profile documents
    val mockProfile1 = mockk<QueryDocumentSnapshot>(relaxed = true)
    val mockProfile2 = mockk<QueryDocumentSnapshot>(relaxed = true)
    every { mockProfile1.exists() } returns true
    every { mockProfile1.id } returns followedId1
    every { mockProfile1.getString("username") } returns "User2"
    every { mockProfile1.getString("email") } returns "user2@example.com"
    every { mockProfile2.exists() } returns true
    every { mockProfile2.id } returns followedId2
    every { mockProfile2.getString("username") } returns "User3"
    every { mockProfile2.getString("email") } returns "user3@example.com"

    val mockProfilesQuery = mockk<Query>(relaxed = true)
    val mockProfilesSnapshot = mockk<QuerySnapshot>(relaxed = true)
    // Mock documents instead of mapNotNull
    every { mockProfilesSnapshot.documents } returns listOf(mockProfile1, mockProfile2)
    // Mock iterator for mapNotNull to work
    every { mockProfilesSnapshot.iterator() } returns
        mutableListOf(mockProfile1, mockProfile2).iterator()

    every { mockCollection.whereIn(any<FieldPath>(), listOf(followedId1, followedId2)) } returns
        mockProfilesQuery
    every { mockProfilesQuery.get() } returns Tasks.forResult(mockProfilesSnapshot)

    // When
    val result = repository.getFollowing(userId, 50)

    // Then
    assertEquals(2, result.size)
    verify { mockFollowsCollection.whereEqualTo("followerId", userId) }
  }

  @Test
  fun `getFollowing and getFollowers return empty list when no relationships exist`() = runTest {
    // Given
    setupFollowerMocks()
    val userId = "user1"

    val mockFollowsSnapshot = mockk<QuerySnapshot>(relaxed = true)
    every { mockFollowsSnapshot.documents } returns emptyList()

    val mockOrderedQuery = mockk<Query>(relaxed = true)
    val mockLimitQuery = mockk<Query>(relaxed = true)

    // Setup for both getFollowing and getFollowers queries
    every { mockFollowsCollection.whereEqualTo(any<String>(), any()) } returns mockQuery
    every { mockQuery.orderBy(any<FieldPath>(), any()) } returns mockOrderedQuery
    every { mockOrderedQuery.limit(50L) } returns mockLimitQuery
    every { mockLimitQuery.get() } returns Tasks.forResult(mockFollowsSnapshot)

    // When
    val followingResult = repository.getFollowing(userId, 50)
    val followersResult = repository.getFollowers(userId, 50)

    // Then
    assertTrue(followingResult.isEmpty())
    assertTrue(followersResult.isEmpty())
  }

  @Test
  fun `getFollowers returns list of follower profiles`() = runTest {
    // Given
    setupFollowerMocks()
    val userId = "user1"
    val followerId1 = "user2"
    val followerId2 = "user3"

    // Mock follow documents
    val mockFollowDoc1 = mockk<QueryDocumentSnapshot>(relaxed = true)
    val mockFollowDoc2 = mockk<QueryDocumentSnapshot>(relaxed = true)
    every { mockFollowDoc1.getString("followerId") } returns followerId1
    every { mockFollowDoc2.getString("followerId") } returns followerId2

    val mockFollowsSnapshot = mockk<QuerySnapshot>(relaxed = true)
    every { mockFollowsSnapshot.documents } returns listOf(mockFollowDoc1, mockFollowDoc2)

    val mockOrderedQuery = mockk<Query>(relaxed = true)
    val mockLimitQuery = mockk<Query>(relaxed = true)

    every { mockFollowsCollection.whereEqualTo("followedId", userId) } returns mockQuery
    every { mockQuery.orderBy(any<FieldPath>(), any()) } returns mockOrderedQuery
    every { mockOrderedQuery.limit(50L) } returns mockLimitQuery
    every { mockLimitQuery.get() } returns Tasks.forResult(mockFollowsSnapshot)

    // Mock profile documents
    val mockProfile1 = mockk<QueryDocumentSnapshot>(relaxed = true)
    val mockProfile2 = mockk<QueryDocumentSnapshot>(relaxed = true)
    every { mockProfile1.exists() } returns true
    every { mockProfile1.id } returns followerId1
    every { mockProfile1.getString("username") } returns "User2"
    every { mockProfile1.getString("email") } returns "user2@example.com"
    every { mockProfile2.exists() } returns true
    every { mockProfile2.id } returns followerId2
    every { mockProfile2.getString("username") } returns "User3"
    every { mockProfile2.getString("email") } returns "user3@example.com"

    val mockProfilesQuery = mockk<Query>(relaxed = true)
    val mockProfilesSnapshot = mockk<QuerySnapshot>(relaxed = true)
    // Mock documents instead of mapNotNull
    every { mockProfilesSnapshot.documents } returns listOf(mockProfile1, mockProfile2)
    // Mock iterator for mapNotNull to work
    every { mockProfilesSnapshot.iterator() } returns
        mutableListOf(mockProfile1, mockProfile2).iterator()

    every { mockCollection.whereIn(any<FieldPath>(), listOf(followerId1, followerId2)) } returns
        mockProfilesQuery
    every { mockProfilesQuery.get() } returns Tasks.forResult(mockProfilesSnapshot)

    // When
    val result = repository.getFollowers(userId, 50)

    // Then
    assertEquals(2, result.size)
    verify { mockFollowsCollection.whereEqualTo("followedId", userId) }
  }

  @Test
  fun `getMutualFollowing returns profiles followed by both users`() = runTest {
    // Given
    setupFollowerMocks()
    val user1Id = "user1"
    val user2Id = "user2"
    val mutualId1 = "user3"
    val mutualId2 = "user4"
    val user1OnlyId = "user5"
    val user2OnlyId = "user6"

    // Mock user1's following
    val mockUser1FollowDoc1 = mockk<QueryDocumentSnapshot>(relaxed = true)
    val mockUser1FollowDoc2 = mockk<QueryDocumentSnapshot>(relaxed = true)
    val mockUser1FollowDoc3 = mockk<QueryDocumentSnapshot>(relaxed = true)
    every { mockUser1FollowDoc1.getString("followedId") } returns mutualId1
    every { mockUser1FollowDoc2.getString("followedId") } returns mutualId2
    every { mockUser1FollowDoc3.getString("followedId") } returns user1OnlyId

    val mockUser1FollowsSnapshot = mockk<QuerySnapshot>(relaxed = true)
    // Mock documents and iterator for mapNotNull
    every { mockUser1FollowsSnapshot.documents } returns
        listOf(mockUser1FollowDoc1, mockUser1FollowDoc2, mockUser1FollowDoc3)
    every { mockUser1FollowsSnapshot.iterator() } returns
        mutableListOf(mockUser1FollowDoc1, mockUser1FollowDoc2, mockUser1FollowDoc3).iterator()

    // Mock user2's following
    val mockUser2FollowDoc1 = mockk<QueryDocumentSnapshot>(relaxed = true)
    val mockUser2FollowDoc2 = mockk<QueryDocumentSnapshot>(relaxed = true)
    val mockUser2FollowDoc3 = mockk<QueryDocumentSnapshot>(relaxed = true)
    every { mockUser2FollowDoc1.getString("followedId") } returns mutualId1
    every { mockUser2FollowDoc2.getString("followedId") } returns mutualId2
    every { mockUser2FollowDoc3.getString("followedId") } returns user2OnlyId

    val mockUser2FollowsSnapshot = mockk<QuerySnapshot>(relaxed = true)
    // Mock documents and iterator for mapNotNull
    every { mockUser2FollowsSnapshot.documents } returns
        listOf(mockUser2FollowDoc1, mockUser2FollowDoc2, mockUser2FollowDoc3)
    every { mockUser2FollowsSnapshot.iterator() } returns
        mutableListOf(mockUser2FollowDoc1, mockUser2FollowDoc2, mockUser2FollowDoc3).iterator()

    val mockQuery1 = mockk<Query>(relaxed = true)
    val mockQuery2 = mockk<Query>(relaxed = true)

    every { mockFollowsCollection.whereEqualTo("followerId", user1Id) } returns mockQuery1
    every { mockQuery1.get() } returns Tasks.forResult(mockUser1FollowsSnapshot)

    every { mockFollowsCollection.whereEqualTo("followerId", user2Id) } returns mockQuery2
    every { mockQuery2.get() } returns Tasks.forResult(mockUser2FollowsSnapshot)

    // Mock profile documents for mutual follows
    val mockProfile1 = mockk<QueryDocumentSnapshot>(relaxed = true)
    val mockProfile2 = mockk<QueryDocumentSnapshot>(relaxed = true)
    every { mockProfile1.exists() } returns true
    every { mockProfile1.id } returns mutualId1
    every { mockProfile1.getString("username") } returns "User3"
    every { mockProfile1.getString("email") } returns "user3@example.com"
    every { mockProfile2.exists() } returns true
    every { mockProfile2.id } returns mutualId2
    every { mockProfile2.getString("username") } returns "User4"
    every { mockProfile2.getString("email") } returns "user4@example.com"

    val mockProfilesQuery = mockk<Query>(relaxed = true)
    val mockProfilesSnapshot = mockk<QuerySnapshot>(relaxed = true)
    // Mock documents instead of mapNotNull
    every { mockProfilesSnapshot.documents } returns listOf(mockProfile1, mockProfile2)
    // Mock iterator for mapNotNull to work
    every { mockProfilesSnapshot.iterator() } returns
        mutableListOf(mockProfile1, mockProfile2).iterator()

    every { mockCollection.whereIn(any<FieldPath>(), listOf(mutualId1, mutualId2)) } returns
        mockProfilesQuery
    every { mockProfilesQuery.get() } returns Tasks.forResult(mockProfilesSnapshot)

    // When
    val result = repository.getMutualFollowing(user1Id, user2Id)

    // Then
    assertEquals(2, result.size)
    verify { mockFollowsCollection.whereEqualTo("followerId", user1Id) }
    verify { mockFollowsCollection.whereEqualTo("followerId", user2Id) }
  }

  @Test
  fun `getMutualFollowing returns empty list when no mutual follows`() = runTest {
    // Given
    setupFollowerMocks()
    val user1Id = "user1"
    val user2Id = "user2"

    // Mock user1's following
    val mockUser1FollowDoc = mockk<QueryDocumentSnapshot>(relaxed = true)
    every { mockUser1FollowDoc.getString("followedId") } returns "user3"
    val mockUser1FollowsSnapshot = mockk<QuerySnapshot>(relaxed = true)
    // Mock documents and iterator for mapNotNull
    every { mockUser1FollowsSnapshot.documents } returns listOf(mockUser1FollowDoc)
    every { mockUser1FollowsSnapshot.iterator() } returns
        mutableListOf(mockUser1FollowDoc).iterator()

    // Mock user2's following
    val mockUser2FollowDoc = mockk<QueryDocumentSnapshot>(relaxed = true)
    every { mockUser2FollowDoc.getString("followedId") } returns "user4"
    val mockUser2FollowsSnapshot = mockk<QuerySnapshot>(relaxed = true)
    // Mock documents and iterator for mapNotNull
    every { mockUser2FollowsSnapshot.documents } returns listOf(mockUser2FollowDoc)
    every { mockUser2FollowsSnapshot.iterator() } returns
        mutableListOf(mockUser2FollowDoc).iterator()

    val mockQuery1 = mockk<Query>(relaxed = true)
    val mockQuery2 = mockk<Query>(relaxed = true)

    every { mockFollowsCollection.whereEqualTo("followerId", user1Id) } returns mockQuery1
    every { mockQuery1.get() } returns Tasks.forResult(mockUser1FollowsSnapshot)

    every { mockFollowsCollection.whereEqualTo("followerId", user2Id) } returns mockQuery2
    every { mockQuery2.get() } returns Tasks.forResult(mockUser2FollowsSnapshot)

    // When
    val result = repository.getMutualFollowing(user1Id, user2Id)

    // Then
    assertTrue(result.isEmpty())
  }
}
