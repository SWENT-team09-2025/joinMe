package com.android.joinme.model.profile

import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.firestore.*
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ProfileRepositoryFirestoreTest {

  private lateinit var mockFirestore: FirebaseFirestore
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
    mockCollection = mockk(relaxed = true)
    mockDocument = mockk(relaxed = true)

    every { mockFirestore.collection(PROFILES_COLLECTION_PATH) } returns mockCollection
    every { mockCollection.document(any()) } returns mockDocument

    repository = ProfileRepositoryFirestore(mockFirestore)
  }

  @After
  fun tearDown() {
    clearAllMocks()
  }

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
}
