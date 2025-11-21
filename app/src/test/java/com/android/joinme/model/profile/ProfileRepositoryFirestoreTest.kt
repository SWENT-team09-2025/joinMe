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

    every { mockFirestore.collection(PROFILES_COLLECTION_PATH) } returns mockCollection
    every { mockCollection.document(any()) } returns mockDocument

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
}
