package com.android.joinme.repository

import com.android.joinme.model.event.EventType
import com.android.joinme.model.groups.GROUPS_COLLECTION_PATH
import com.android.joinme.model.groups.Group
import com.android.joinme.model.groups.GroupRepositoryFirestore
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
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GroupRepositoryFirestoreTest {

  private lateinit var mockDb: FirebaseFirestore
  private lateinit var mockCollection: CollectionReference
  private lateinit var mockDocument: DocumentReference
  private lateinit var mockSnapshot: DocumentSnapshot
  private lateinit var mockAuth: FirebaseAuth
  private lateinit var mockUser: FirebaseUser
  private lateinit var repository: GroupRepositoryFirestore

  private val testGroupId = "testGroup123"
  private val testUserId = "testUser456"

  /** Helper method to create a test group with default values. */
  private fun createTestGroup(): Group {
    return Group(
        id = testGroupId,
        name = "Test Group",
        category = EventType.SPORTS,
        description = "A test group",
        ownerId = testUserId,
        memberIds = listOf(testUserId, "user2"),
        eventIds = listOf("event1", "event2"))
  }

  @Before
  fun setup() {
    // Mock Firestore
    mockDb = mockk(relaxed = true)
    mockCollection = mockk(relaxed = true)
    mockDocument = mockk(relaxed = true)
    mockSnapshot = mockk(relaxed = true)

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
        com.android.joinme.model.groups.Group(
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
        com.android.joinme.model.groups.Group(
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
    // Given: Mock Firebase Auth
    mockkStatic(FirebaseAuth::class)
    every { FirebaseAuth.getInstance() } returns mockAuth
    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns testUserId

    // Mock getGroup call (deleteGroup validates ownership by calling getGroup)
    val testGroup = createTestGroup()
    every { mockDocument.get() } returns Tasks.forResult(mockSnapshot)
    every { mockSnapshot.exists() } returns true
    every { mockSnapshot.id } returns testGroupId
    every { mockSnapshot.getString("name") } returns testGroup.name
    every { mockSnapshot.getString("description") } returns testGroup.description
    every { mockSnapshot.getString("ownerId") } returns testUserId // User is owner
    every { mockSnapshot.get("memberIds") } returns testGroup.memberIds
    every { mockSnapshot.get("eventIds") } returns testGroup.eventIds
    every { mockSnapshot.get("category") } returns null

    // Mock delete operation
    every { mockDocument.delete() } returns Tasks.forResult(null)

    // When
    repository.deleteGroup(testGroupId, testUserId)

    // Then
    verify { mockCollection.document(testGroupId) }
    verify { mockDocument.get() } // Called by getGroup
    verify { mockDocument.delete() }

    // Cleanup
    unmockkStatic(FirebaseAuth::class)
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

    every { mockCollection.whereArrayContains("memberIds", testUserId) } returns mockQuery
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

  // =======================================
  // Delete Group Owner Validation Tests
  // =======================================

  @Test
  fun deleteGroup_asOwner_deletesSuccessfully() = runTest {
    // Given: Current user is the owner
    mockkStatic(FirebaseAuth::class)
    val mockAuth = mockk<FirebaseAuth>()
    val mockUser = mockk<FirebaseUser>()
    every { FirebaseAuth.getInstance() } returns mockAuth
    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns testUserId

    val group = createTestGroup()
    every { mockDocument.get() } returns Tasks.forResult(mockSnapshot)
    every { mockSnapshot.exists() } returns true
    every { mockSnapshot.id } returns testGroupId
    every { mockSnapshot.getString("name") } returns group.name
    every { mockSnapshot.getString("category") } returns "SPORTS"
    every { mockSnapshot.getString("description") } returns group.description
    every { mockSnapshot.getString("ownerId") } returns testUserId
    every { mockSnapshot.get("memberIds") } returns group.memberIds
    every { mockSnapshot.get("eventIds") } returns group.eventIds
    every { mockSnapshot.getString("photoUrl") } returns null
    every { mockDocument.delete() } returns Tasks.forResult(null)

    // When
    repository.deleteGroup(testGroupId, testUserId)

    // Then
    verify { mockDocument.delete() }

    unmockkStatic(FirebaseAuth::class)
  }

  @Test
  fun deleteGroup_asNonOwner_throwsException() = runTest {
    // Given: Current user is NOT the owner
    mockkStatic(FirebaseAuth::class)
    val mockAuth = mockk<FirebaseAuth>()
    val mockUser = mockk<FirebaseUser>()
    every { FirebaseAuth.getInstance() } returns mockAuth
    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns "different-user-id"

    val group = createTestGroup()
    every { mockDocument.get() } returns Tasks.forResult(mockSnapshot)
    every { mockSnapshot.exists() } returns true
    every { mockSnapshot.id } returns testGroupId
    every { mockSnapshot.getString("name") } returns group.name
    every { mockSnapshot.getString("category") } returns "SPORTS"
    every { mockSnapshot.getString("description") } returns group.description
    every { mockSnapshot.getString("ownerId") } returns testUserId
    every { mockSnapshot.get("memberIds") } returns group.memberIds
    every { mockSnapshot.get("eventIds") } returns group.eventIds
    every { mockSnapshot.getString("photoUrl") } returns null

    // When/Then
    val exception =
        assertThrows(Exception::class.java) {
          runBlocking { repository.deleteGroup(testGroupId, "different-user-id") }
        }
    assertTrue(exception.message!!.contains("Only the group owner can delete this group"))

    verify(exactly = 0) { mockDocument.delete() }

    unmockkStatic(FirebaseAuth::class)
  }

  @Test
  fun deleteGroup_whenNotLoggedIn_throwsException() = runTest {
    // Given: No user is logged in
    mockkStatic(FirebaseAuth::class)
    val mockAuth = mockk<FirebaseAuth>()
    every { FirebaseAuth.getInstance() } returns mockAuth
    every { mockAuth.currentUser } returns null

    // Mock the getGroup call that deleteGroup makes
    val group = createTestGroup()
    every { mockDocument.get() } returns Tasks.forResult(mockSnapshot)
    every { mockSnapshot.exists() } returns true
    every { mockSnapshot.id } returns testGroupId
    every { mockSnapshot.getString("name") } returns group.name
    every { mockSnapshot.getString("category") } returns "SPORTS"
    every { mockSnapshot.getString("description") } returns group.description
    every { mockSnapshot.getString("ownerId") } returns testUserId
    every { mockSnapshot.get("memberIds") } returns group.memberIds
    every { mockSnapshot.get("eventIds") } returns group.eventIds
    every { mockSnapshot.getString("photoUrl") } returns null

    // When/Then
    val exception =
        assertThrows(Exception::class.java) {
          runBlocking { repository.deleteGroup(testGroupId, "any-user-id") }
        }
    assertTrue(exception.message!!.contains("Only the group owner can delete this group"))

    verify(exactly = 0) { mockDocument.delete() }

    unmockkStatic(FirebaseAuth::class)
  }

  // =======================================
  // Leave Group Tests
  // =======================================

  @Test
  fun leaveGroup_removesUserFromMemberList() = runTest {
    // Given
    val userId = "user-to-remove"
    val group = createTestGroup().copy(memberIds = listOf(testUserId, userId, "other-user"))

    every { mockDocument.get() } returns Tasks.forResult(mockSnapshot)
    every { mockSnapshot.exists() } returns true
    every { mockSnapshot.id } returns testGroupId
    every { mockSnapshot.getString("name") } returns group.name
    every { mockSnapshot.getString("category") } returns "SPORTS"
    every { mockSnapshot.getString("description") } returns group.description
    every { mockSnapshot.getString("ownerId") } returns testUserId
    every { mockSnapshot.get("memberIds") } returns group.memberIds
    every { mockSnapshot.get("eventIds") } returns group.eventIds
    every { mockSnapshot.getString("photoUrl") } returns null
    every { mockDocument.set(any<Group>()) } returns Tasks.forResult(null)

    // When
    repository.leaveGroup(testGroupId, userId)

    // Then
    verify {
      mockDocument.set(
          match<Group> {
            it.memberIds.size == 2 &&
                it.memberIds.contains(testUserId) &&
                it.memberIds.contains("other-user") &&
                !it.memberIds.contains(userId)
          })
    }
  }

  @Test
  fun leaveGroup_withNonMember_throwsException() = runTest {
    // Given
    val nonMemberId = "non-member-user"
    val group = createTestGroup().copy(memberIds = listOf(testUserId, "other-user"))

    every { mockDocument.get() } returns Tasks.forResult(mockSnapshot)
    every { mockSnapshot.exists() } returns true
    every { mockSnapshot.id } returns testGroupId
    every { mockSnapshot.getString("name") } returns group.name
    every { mockSnapshot.getString("category") } returns "SPORTS"
    every { mockSnapshot.getString("description") } returns group.description
    every { mockSnapshot.getString("ownerId") } returns testUserId
    every { mockSnapshot.get("memberIds") } returns group.memberIds
    every { mockSnapshot.get("eventIds") } returns group.eventIds
    every { mockSnapshot.getString("photoUrl") } returns null

    // When/Then
    val exception =
        assertThrows(Exception::class.java) {
          runBlocking { repository.leaveGroup(testGroupId, nonMemberId) }
        }
    assertTrue(exception.message!!.contains("User is not a member of this group"))

    verify(exactly = 0) { mockDocument.set(any<Group>()) }
  }

  @Test
  fun leaveGroup_lastMemberLeaving_removesFromList() = runTest {
    // Given: User is the only member
    val userId = testUserId
    val group = createTestGroup().copy(memberIds = listOf(userId))

    every { mockDocument.get() } returns Tasks.forResult(mockSnapshot)
    every { mockSnapshot.exists() } returns true
    every { mockSnapshot.id } returns testGroupId
    every { mockSnapshot.getString("name") } returns group.name
    every { mockSnapshot.getString("category") } returns "SPORTS"
    every { mockSnapshot.getString("description") } returns group.description
    every { mockSnapshot.getString("ownerId") } returns testUserId
    every { mockSnapshot.get("memberIds") } returns group.memberIds
    every { mockSnapshot.get("eventIds") } returns group.eventIds
    every { mockSnapshot.getString("photoUrl") } returns null
    every { mockDocument.set(any<Group>()) } returns Tasks.forResult(null)

    // When
    repository.leaveGroup(testGroupId, userId)

    // Then
    verify { mockDocument.set(match<Group> { it.memberIds.isEmpty() }) }
  }

  // =======================================
  // Join Group Tests
  // =======================================

  @Test
  fun joinGroup_addsUserToMemberList() = runTest {
    // Given
    val newUserId = "new-user"
    val group = createTestGroup().copy(memberIds = listOf(testUserId, "other-user"))

    every { mockDocument.get() } returns Tasks.forResult(mockSnapshot)
    every { mockSnapshot.exists() } returns true
    every { mockSnapshot.id } returns testGroupId
    every { mockSnapshot.getString("name") } returns group.name
    every { mockSnapshot.getString("category") } returns "SPORTS"
    every { mockSnapshot.getString("description") } returns group.description
    every { mockSnapshot.getString("ownerId") } returns testUserId
    every { mockSnapshot.get("memberIds") } returns group.memberIds
    every { mockSnapshot.get("eventIds") } returns group.eventIds
    every { mockSnapshot.getString("photoUrl") } returns null
    every { mockDocument.set(any<Group>()) } returns Tasks.forResult(null)

    // When
    repository.joinGroup(testGroupId, newUserId)

    // Then
    verify {
      mockDocument.set(
          match<Group> {
            it.memberIds.size == 3 &&
                it.memberIds.contains(testUserId) &&
                it.memberIds.contains("other-user") &&
                it.memberIds.contains(newUserId)
          })
    }
  }

  @Test
  fun joinGroup_userAlreadyMember_throwsException() = runTest {
    // Given
    val existingUserId = testUserId
    val group = createTestGroup().copy(memberIds = listOf(testUserId, "other-user"))

    every { mockDocument.get() } returns Tasks.forResult(mockSnapshot)
    every { mockSnapshot.exists() } returns true
    every { mockSnapshot.id } returns testGroupId
    every { mockSnapshot.getString("name") } returns group.name
    every { mockSnapshot.getString("category") } returns "SPORTS"
    every { mockSnapshot.getString("description") } returns group.description
    every { mockSnapshot.getString("ownerId") } returns testUserId
    every { mockSnapshot.get("memberIds") } returns group.memberIds
    every { mockSnapshot.get("eventIds") } returns group.eventIds
    every { mockSnapshot.getString("photoUrl") } returns null

    // When/Then
    val exception =
        assertThrows(Exception::class.java) {
          runBlocking { repository.joinGroup(testGroupId, existingUserId) }
        }
    assertTrue(exception.message!!.contains("User is already a member of this group"))

    verify(exactly = 0) { mockDocument.set(any<Group>()) }
  }

  @Test
  fun joinGroup_emptyMemberList_addsFirstMember() = runTest {
    // Given
    val newUserId = "first-user"
    val group = createTestGroup().copy(memberIds = emptyList())

    every { mockDocument.get() } returns Tasks.forResult(mockSnapshot)
    every { mockSnapshot.exists() } returns true
    every { mockSnapshot.id } returns testGroupId
    every { mockSnapshot.getString("name") } returns group.name
    every { mockSnapshot.getString("category") } returns "SPORTS"
    every { mockSnapshot.getString("description") } returns group.description
    every { mockSnapshot.getString("ownerId") } returns testUserId
    every { mockSnapshot.get("memberIds") } returns group.memberIds
    every { mockSnapshot.get("eventIds") } returns group.eventIds
    every { mockSnapshot.getString("photoUrl") } returns null
    every { mockDocument.set(any<Group>()) } returns Tasks.forResult(null)

    // When
    repository.joinGroup(testGroupId, newUserId)

    // Then
    verify {
      mockDocument.set(match<Group> { it.memberIds.size == 1 && it.memberIds.contains(newUserId) })
    }
  }

  @Test
  fun joinGroup_groupNotFound_throwsException() = runTest {
    // Given
    every { mockDocument.get() } returns Tasks.forResult(mockSnapshot)
    every { mockSnapshot.getString("name") } returns null // Missing required field

    // When/Then
    val exception =
        assertThrows(Exception::class.java) {
          runBlocking { repository.joinGroup(testGroupId, "any-user") }
        }
    assertTrue(exception.message!!.contains("Group not found"))

    verify(exactly = 0) { mockDocument.set(any<Group>()) }
  }

  // =======================================
  // Additional Edge Case Tests
  // =======================================

  @Test
  fun getGroup_withInvalidCategory_defaultsToActivity() = runTest {
    // Given
    val mockSnapshot = mockk<DocumentSnapshot>(relaxed = true)
    every { mockDocument.get() } returns Tasks.forResult(mockSnapshot)
    every { mockSnapshot.id } returns testGroupId
    every { mockSnapshot.getString("name") } returns "Test Group"
    every { mockSnapshot.getString("ownerId") } returns testUserId
    every { mockSnapshot.getString("category") } returns "INVALID_CATEGORY"
    every { mockSnapshot.getString("description") } returns ""
    every { mockSnapshot.get("memberIds") } returns emptyList<String>()
    every { mockSnapshot.get("eventIds") } returns emptyList<String>()
    every { mockSnapshot.getString("photoUrl") } returns null

    // When
    val result = repository.getGroup(testGroupId)

    // Then
    assertNotNull(result)
    assertEquals(EventType.ACTIVITY, result.category)
  }

  @Test
  fun getGroup_withPhotoUrl_returnsGroupWithPhoto() = runTest {
    // Given
    val photoUrl = "https://example.com/photo.jpg"
    val mockSnapshot = mockk<DocumentSnapshot>(relaxed = true)
    every { mockDocument.get() } returns Tasks.forResult(mockSnapshot)
    every { mockSnapshot.id } returns testGroupId
    every { mockSnapshot.getString("name") } returns "Test Group"
    every { mockSnapshot.getString("ownerId") } returns testUserId
    every { mockSnapshot.getString("category") } returns "SPORTS"
    every { mockSnapshot.getString("description") } returns ""
    every { mockSnapshot.get("memberIds") } returns emptyList<String>()
    every { mockSnapshot.get("eventIds") } returns emptyList<String>()
    every { mockSnapshot.getString("photoUrl") } returns photoUrl

    // When
    val result = repository.getGroup(testGroupId)

    // Then
    assertNotNull(result)
    assertEquals(photoUrl, result.photoUrl)
  }

  @Test
  fun getAllGroups_removesDuplicates() = runTest {
    // Given: User appears in both owner and member queries (same group returned twice)
    mockkStatic(FirebaseAuth::class)
    every { FirebaseAuth.getInstance() } returns mockAuth
    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns testUserId

    val mockQuery = mockk<com.google.firebase.firestore.Query>(relaxed = true)
    val mockQuerySnapshot = mockk<QuerySnapshot>(relaxed = true)
    val mockSnapshot1 = mockk<com.google.firebase.firestore.QueryDocumentSnapshot>(relaxed = true)

    every { mockCollection.whereArrayContains("memberIds", testUserId) } returns mockQuery
    every { mockQuery.get() } returns Tasks.forResult(mockQuerySnapshot)
    // Return the same group twice (simulating duplicate from owner and member queries)
    every { mockQuerySnapshot.iterator() } returns
        mutableListOf(mockSnapshot1, mockSnapshot1).iterator()

    // Setup group
    every { mockSnapshot1.id } returns "group1"
    every { mockSnapshot1.getString("name") } returns "Group 1"
    every { mockSnapshot1.getString("description") } returns "Description 1"
    every { mockSnapshot1.getString("ownerId") } returns testUserId
    every { mockSnapshot1.get("memberIds") } returns emptyList<String>()
    every { mockSnapshot1.get("eventIds") } returns emptyList<String>()
    every { mockSnapshot1.getString("category") } returns "SPORTS"
    every { mockSnapshot1.getString("photoUrl") } returns null

    // When
    val result = repository.getAllGroups()

    // Then: Should only have 1 group (duplicates removed)
    assertEquals(1, result.size)

    unmockkStatic(FirebaseAuth::class)
  }

  @Test
  fun editGroup_updatesAllFields() = runTest {
    // Given
    val updatedGroup =
        Group(
            id = testGroupId,
            name = "New Name",
            category = EventType.ACTIVITY,
            description = "New Description",
            ownerId = "newOwner",
            memberIds = listOf("a", "b", "c"),
            eventIds = listOf("e1", "e2"),
            photoUrl = "https://example.com/newphoto.jpg")
    every { mockDocument.set(any()) } returns Tasks.forResult(null)

    // When
    repository.editGroup(testGroupId, updatedGroup)

    // Then
    verify {
      mockDocument.set(
          match<Group> {
            it.name == "New Name" &&
                it.category == EventType.ACTIVITY &&
                it.description == "New Description" &&
                it.ownerId == "newOwner" &&
                it.memberIds.size == 3 &&
                it.eventIds.size == 2 &&
                it.photoUrl == "https://example.com/newphoto.jpg"
          })
    }
  }

  @Test
  fun getGroup_withNullCategory_defaultsToActivity() = runTest {
    // Given
    val mockSnapshot = mockk<DocumentSnapshot>(relaxed = true)
    every { mockDocument.get() } returns Tasks.forResult(mockSnapshot)
    every { mockSnapshot.id } returns testGroupId
    every { mockSnapshot.getString("name") } returns "Test Group"
    every { mockSnapshot.getString("ownerId") } returns testUserId
    every { mockSnapshot.getString("category") } returns null
    every { mockSnapshot.getString("description") } returns ""
    every { mockSnapshot.get("memberIds") } returns emptyList<String>()
    every { mockSnapshot.get("eventIds") } returns emptyList<String>()
    every { mockSnapshot.getString("photoUrl") } returns null

    // When
    val result = repository.getGroup(testGroupId)

    // Then
    assertNotNull(result)
    assertEquals(EventType.ACTIVITY, result.category)
  }
}
