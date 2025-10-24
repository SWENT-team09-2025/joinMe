package com.android.joinme.repository

import com.android.joinme.model.group.GROUPS_COLLECTION_PATH
import com.android.joinme.model.group.GroupRepositoryFirestore
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class GroupRepositoryFirestoreTest {

  private lateinit var mockDb: FirebaseFirestore
  private lateinit var mockCollection: CollectionReference
  private lateinit var mockDocument: DocumentReference
  private lateinit var mockAuth: FirebaseAuth
  private lateinit var mockUser: FirebaseUser
  private lateinit var repository: GroupRepositoryFirestore

  private val testGroupId = "testGroup123"
  private val testUserId = "testUser456"

  @Before
  fun setup() {
    // Mock Firestore
    mockDb = mockk(relaxed = true)
    mockCollection = mockk(relaxed = true)
    mockDocument = mockk(relaxed = true)

    // Mock Firebase Auth
    mockAuth = mockk(relaxed = true)
    mockUser = mockk(relaxed = true)

    every { mockDb.collection(GROUPS_COLLECTION_PATH) } returns mockCollection
    every { mockCollection.document(any()) } returns mockDocument
    every { mockCollection.document() } returns mockDocument
    every { mockDocument.id } returns testGroupId

    repository = GroupRepositoryFirestore(mockDb)
  }

  @Test
  fun getNewGroupId_returnsValidId() {
    // When
    val groupId = repository.getNewGroupId()

    // Then
    assertNotNull(groupId)
    assertEquals(testGroupId, groupId)
    verify { mockDb.collection(GROUPS_COLLECTION_PATH) }
    verify { mockCollection.document() }
  }

  @Test
  fun addGroup_callsFirestoreSet() = runTest {
    // Given
    val testGroup =
        com.android.joinme.model.group.Group(
            id = testGroupId,
            name = "Test Group",
            description = "A test group",
            ownerId = testUserId,
            memberIds = listOf("user1", "user2"),
            eventIds = listOf("event1", "event2"))
    every { mockDocument.set(any()) } returns Tasks.forResult(null)

    // When
    repository.addGroup(testGroup)

    // Then
    verify { mockCollection.document(testGroupId) }
    verify { mockDocument.set(testGroup) }
  }

  @Test
  fun getGroup_returnsGroupSuccessfully() = runTest {
    // Given
    val mockSnapshot = mockk<DocumentSnapshot>(relaxed = true)
    every { mockDocument.get() } returns Tasks.forResult(mockSnapshot)
    every { mockSnapshot.id } returns testGroupId
    every { mockSnapshot.getString("name") } returns "Test Group"
    every { mockSnapshot.getString("description") } returns "A test group"
    every { mockSnapshot.getString("ownerId") } returns testUserId
    every { mockSnapshot.get("memberIds") } returns listOf("user1", "user2")
    every { mockSnapshot.get("eventIds") } returns listOf("event1", "event2")

    // When
    val result = repository.getGroup(testGroupId)

    // Then
    assertNotNull(result)
    assertEquals(testGroupId, result.id)
    assertEquals("Test Group", result.name)
    assertEquals(testUserId, result.ownerId)
  }

  @Test
  fun editGroup_callsFirestoreUpdate() = runTest {
    // Given
    val updatedGroup =
        com.android.joinme.model.group.Group(
            id = testGroupId,
            name = "Updated Name",
            description = "Updated description",
            ownerId = testUserId)
    every { mockDocument.set(any()) } returns Tasks.forResult(null)

    // When
    repository.editGroup(testGroupId, updatedGroup)

    // Then
    verify { mockCollection.document(testGroupId) }
    verify { mockDocument.set(updatedGroup) }
  }

  @Test
  fun deleteGroup_callsFirestoreDelete() = runTest {
    // Given
    every { mockDocument.delete() } returns Tasks.forResult(null)

    // When
    repository.deleteGroup(testGroupId)

    // Then
    verify { mockCollection.document(testGroupId) }
    verify { mockDocument.delete() }
  }

  @Test
  fun getAllGroups_returnsGroupsForCurrentUser() = runTest {
    // Given
    mockkStatic(FirebaseAuth::class)
    every { FirebaseAuth.getInstance() } returns mockAuth
    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns testUserId

    val mockQuery = mockk<com.google.firebase.firestore.Query>(relaxed = true)
    val mockQuerySnapshot = mockk<QuerySnapshot>(relaxed = true)
    val mockSnapshot1 = mockk<com.google.firebase.firestore.QueryDocumentSnapshot>(relaxed = true)
    val mockSnapshot2 = mockk<com.google.firebase.firestore.QueryDocumentSnapshot>(relaxed = true)

    every { mockCollection.whereEqualTo("ownerId", testUserId) } returns mockQuery
    every { mockQuery.get() } returns Tasks.forResult(mockQuerySnapshot)
    every { mockQuerySnapshot.iterator() } returns
        mutableListOf(mockSnapshot1, mockSnapshot2).iterator()

    // Setup first group
    every { mockSnapshot1.id } returns "group1"
    every { mockSnapshot1.getString("name") } returns "Group 1"
    every { mockSnapshot1.getString("description") } returns "Description 1"
    every { mockSnapshot1.getString("ownerId") } returns testUserId
    every { mockSnapshot1.get("memberIds") } returns emptyList<String>()
    every { mockSnapshot1.get("eventIds") } returns emptyList<String>()

    // Setup second group
    every { mockSnapshot2.id } returns "group2"
    every { mockSnapshot2.getString("name") } returns "Group 2"
    every { mockSnapshot2.getString("description") } returns "Description 2"
    every { mockSnapshot2.getString("ownerId") } returns testUserId
    every { mockSnapshot2.get("memberIds") } returns listOf("user3")
    every { mockSnapshot2.get("eventIds") } returns emptyList<String>()

    // When
    val result = repository.getAllGroups()

    // Then
    assertEquals(2, result.size)
    assertEquals("Group 1", result[0].name)
    assertEquals("Group 2", result[1].name)
  }

  @Test(expected = Exception::class)
  fun getGroup_throwsExceptionWhenNotFound() = runTest {
    // Given
    val mockSnapshot = mockk<DocumentSnapshot>(relaxed = true)
    every { mockDocument.get() } returns Tasks.forResult(mockSnapshot)
    every { mockSnapshot.getString("name") } returns null // Missing required field

    // When
    repository.getGroup(testGroupId)

    // Then - exception is thrown
  }

  @Test(expected = Exception::class)
  fun getAllGroups_throwsExceptionWhenUserNotLoggedIn() = runTest {
    // Given
    mockkStatic(FirebaseAuth::class)
    every { FirebaseAuth.getInstance() } returns mockAuth
    every { mockAuth.currentUser } returns null // User not logged in

    // When
    repository.getAllGroups()

    // Then - exception is thrown
  }

  @Test
  fun getGroup_withMissingOptionalFields_returnsGroupWithDefaults() = runTest {
    // Given
    val mockSnapshot = mockk<DocumentSnapshot>(relaxed = true)
    every { mockDocument.get() } returns Tasks.forResult(mockSnapshot)
    every { mockSnapshot.id } returns testGroupId
    every { mockSnapshot.getString("name") } returns "Minimal Group"
    every { mockSnapshot.getString("ownerId") } returns testUserId
    every { mockSnapshot.getString("description") } returns null
    every { mockSnapshot.get("memberIds") } returns null
    every { mockSnapshot.get("eventIds") } returns null

    // When
    val result = repository.getGroup(testGroupId)

    // Then
    assertNotNull(result)
    assertEquals("Minimal Group", result.name)
    assertEquals(testUserId, result.ownerId)
    assertEquals("", result.description)
    assertEquals(0, result.membersCount)
    assertEquals(emptyList<String>(), result.memberIds)
    assertEquals(emptyList<String>(), result.eventIds)
  }
}
