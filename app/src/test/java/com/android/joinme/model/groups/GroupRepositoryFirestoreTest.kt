// Implemented with help of Claude AI
package com.android.joinme.model.groups

import com.android.joinme.model.event.EventType
import com.android.joinme.model.utils.ImageProcessor
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QueryDocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
  private lateinit var mockStorage: FirebaseStorage
  private lateinit var mockStorageRef: StorageReference
  private lateinit var mockImageProcessor: ImageProcessor
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

    // Mock Firebase Storage
    mockStorage = mockk(relaxed = true)
    mockStorageRef = mockk(relaxed = true)

    // Mock ImageProcessor
    mockImageProcessor = mockk(relaxed = true)

    every { mockDb.collection(GROUPS_COLLECTION_PATH) } returns mockCollection
    every { mockCollection.document(any()) } returns mockDocument
    every { mockCollection.document() } returns mockDocument
    every { mockDocument.id } returns testGroupId

    repository = GroupRepositoryFirestore(mockDb, mockStorage) { mockImageProcessor }
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
        Group(
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
        Group(
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

    val mockQuery = mockk<Query>(relaxed = true)
    val mockQuerySnapshot = mockk<QuerySnapshot>(relaxed = true)
    val mockSnapshot1 = mockk<QueryDocumentSnapshot>(relaxed = true)

    every { mockCollection.whereArrayContains("memberIds", testUserId) } returns mockQuery
    every { mockQuery.get() } returns Tasks.forResult(mockQuerySnapshot)
    every { mockQuerySnapshot.iterator() } returns mutableListOf(mockSnapshot1).iterator()

    // Mock document data for mockSnapshot1
    every { mockSnapshot1.id } returns testGroupId
    every { mockSnapshot1.getString("name") } returns "Test Group"
    every { mockSnapshot1.getString("description") } returns "A test group"
    every { mockSnapshot1.getString("ownerId") } returns testUserId
    every { mockSnapshot1.getString("category") } returns EventType.SPORTS.name
    every { mockSnapshot1.get("memberIds") } returns listOf(testUserId)
    every { mockSnapshot1.get("eventIds") } returns emptyList<String>()
    every { mockSnapshot1.get("serieIds") } returns emptyList<String>()
    every { mockSnapshot1.getString("photoUrl") } returns null

    // When
    val result = repository.getAllGroups()

    // Then
    assertEquals(1, result.size)
    assertEquals("Test Group", result[0].name)

    unmockkStatic(FirebaseAuth::class)
  }

  @Test
  fun joinGroup_addsUserToMemberList() = runTest {
    // Given
    val newUserId = "newUser789"
    val testGroup = createTestGroup()

    every { mockDocument.get() } returns Tasks.forResult(mockSnapshot)
    every { mockSnapshot.id } returns testGroupId
    every { mockSnapshot.getString("name") } returns testGroup.name
    every { mockSnapshot.getString("description") } returns testGroup.description
    every { mockSnapshot.getString("ownerId") } returns testGroup.ownerId
    every { mockSnapshot.getString("category") } returns EventType.SPORTS.name
    every { mockSnapshot.get("memberIds") } returns testGroup.memberIds
    every { mockSnapshot.get("eventIds") } returns testGroup.eventIds
    every { mockSnapshot.get("serieIds") } returns emptyList<String>()
    every { mockSnapshot.getString("photoUrl") } returns null
    every { mockDocument.set(any()) } returns Tasks.forResult(null)

    // When
    repository.joinGroup(testGroupId, newUserId)

    // Then
    verify { mockDocument.get() }
    verify {
      mockDocument.set(match<Group> { it.memberIds.contains(newUserId) && it.memberIds.size == 3 })
    }
  }

  @Test(expected = Exception::class)
  fun joinGroup_alreadyMember_throwsException() = runTest {
    // Given
    val testGroup = createTestGroup()

    every { mockDocument.get() } returns Tasks.forResult(mockSnapshot)
    every { mockSnapshot.id } returns testGroupId
    every { mockSnapshot.getString("name") } returns testGroup.name
    every { mockSnapshot.getString("description") } returns testGroup.description
    every { mockSnapshot.getString("ownerId") } returns testGroup.ownerId
    every { mockSnapshot.getString("category") } returns EventType.SPORTS.name
    every { mockSnapshot.get("memberIds") } returns testGroup.memberIds
    every { mockSnapshot.get("eventIds") } returns testGroup.eventIds
    every { mockSnapshot.get("serieIds") } returns emptyList<String>()
    every { mockSnapshot.getString("photoUrl") } returns null

    // When - user2 is already a member
    repository.joinGroup(testGroupId, "user2")
  }

  @Test
  fun leaveGroup_removesUserFromMemberList() = runTest {
    // Given
    val testGroup = createTestGroup()

    every { mockDocument.get() } returns Tasks.forResult(mockSnapshot)
    every { mockSnapshot.id } returns testGroupId
    every { mockSnapshot.getString("name") } returns testGroup.name
    every { mockSnapshot.getString("description") } returns testGroup.description
    every { mockSnapshot.getString("ownerId") } returns testGroup.ownerId
    every { mockSnapshot.getString("category") } returns EventType.SPORTS.name
    every { mockSnapshot.get("memberIds") } returns testGroup.memberIds
    every { mockSnapshot.get("eventIds") } returns testGroup.eventIds
    every { mockSnapshot.get("serieIds") } returns emptyList<String>()
    every { mockSnapshot.getString("photoUrl") } returns null
    every { mockDocument.set(any()) } returns Tasks.forResult(null)

    // When
    repository.leaveGroup(testGroupId, "user2")

    // Then
    verify {
      mockDocument.set(match<Group> { !it.memberIds.contains("user2") && it.memberIds.size == 1 })
    }
  }

  @Test(expected = Exception::class)
  fun leaveGroup_notMember_throwsException() = runTest {
    // Given
    val testGroup = createTestGroup()

    every { mockDocument.get() } returns Tasks.forResult(mockSnapshot)
    every { mockSnapshot.id } returns testGroupId
    every { mockSnapshot.getString("name") } returns testGroup.name
    every { mockSnapshot.getString("description") } returns testGroup.description
    every { mockSnapshot.getString("ownerId") } returns testGroup.ownerId
    every { mockSnapshot.getString("category") } returns EventType.SPORTS.name
    every { mockSnapshot.get("memberIds") } returns testGroup.memberIds
    every { mockSnapshot.get("eventIds") } returns testGroup.eventIds
    every { mockSnapshot.get("serieIds") } returns emptyList<String>()
    every { mockSnapshot.getString("photoUrl") } returns null

    // When - user not in member list
    repository.leaveGroup(testGroupId, "nonMember")
  }

  @Test(expected = Exception::class)
  fun deleteGroup_nonOwner_throwsException() = runTest {
    // Given
    val testGroup = createTestGroup()

    every { mockDocument.get() } returns Tasks.forResult(mockSnapshot)
    every { mockSnapshot.id } returns testGroupId
    every { mockSnapshot.getString("name") } returns testGroup.name
    every { mockSnapshot.getString("description") } returns testGroup.description
    every { mockSnapshot.getString("ownerId") } returns testGroup.ownerId
    every { mockSnapshot.getString("category") } returns EventType.SPORTS.name
    every { mockSnapshot.get("memberIds") } returns testGroup.memberIds
    every { mockSnapshot.get("eventIds") } returns testGroup.eventIds
    every { mockSnapshot.get("serieIds") } returns emptyList<String>()
    every { mockSnapshot.getString("photoUrl") } returns null

    // When - different user tries to delete
    repository.deleteGroup(testGroupId, "differentUser")
  }

  @Test(expected = Exception::class)
  fun getGroup_notFound_throwsException() = runTest {
    // Given
    every { mockDocument.get() } returns Tasks.forResult(mockSnapshot)
    every { mockSnapshot.getString("name") } returns null // Missing required field

    // When
    repository.getGroup(testGroupId)
  }

  // ---------------- GROUP PHOTO TESTS ----------------

  @Test
  fun deleteGroupPhoto_deletesFromStorageAndUpdatesGroup() = runTest {
    // Given
    val testGroup = createTestGroup().copy(photoUrl = "https://storage.example.com/photo.jpg")

    // Mock storage refs
    val groupsRef = mockk<StorageReference>(relaxed = true)
    val groupIdRef = mockk<StorageReference>(relaxed = true)
    val photoRef = mockk<StorageReference>(relaxed = true)

    every { mockStorage.reference } returns mockStorageRef
    every { mockStorageRef.child("groups") } returns groupsRef
    every { groupsRef.child(testGroupId) } returns groupIdRef
    every { groupIdRef.child("group.jpg") } returns photoRef
    every { photoRef.delete() } returns Tasks.forResult(null)

    // Mock getGroup
    every { mockDocument.get() } returns Tasks.forResult(mockSnapshot)
    every { mockSnapshot.id } returns testGroupId
    every { mockSnapshot.getString("name") } returns testGroup.name
    every { mockSnapshot.getString("description") } returns testGroup.description
    every { mockSnapshot.getString("ownerId") } returns testGroup.ownerId
    every { mockSnapshot.getString("category") } returns EventType.SPORTS.name
    every { mockSnapshot.get("memberIds") } returns testGroup.memberIds
    every { mockSnapshot.get("eventIds") } returns testGroup.eventIds
    every { mockSnapshot.get("serieIds") } returns emptyList<String>()
    every { mockSnapshot.getString("photoUrl") } returns testGroup.photoUrl
    every { mockDocument.set(any()) } returns Tasks.forResult(null)

    // When
    repository.deleteGroupPhoto(testGroupId)

    // Then
    verify { photoRef.delete() }
    verify { mockDocument.set(match<Group> { it.photoUrl == null }) }
  }

  @Test
  fun deleteGroupPhoto_continuesWhenStorageFileNotFound() = runTest {
    // Given
    val testGroup = createTestGroup().copy(photoUrl = "https://storage.example.com/photo.jpg")

    // Mock storage refs - delete throws exception (file not found)
    val groupsRef = mockk<StorageReference>(relaxed = true)
    val groupIdRef = mockk<StorageReference>(relaxed = true)
    val photoRef = mockk<StorageReference>(relaxed = true)

    every { mockStorage.reference } returns mockStorageRef
    every { mockStorageRef.child("groups") } returns groupsRef
    every { groupsRef.child(testGroupId) } returns groupIdRef
    every { groupIdRef.child("group.jpg") } returns photoRef
    every { photoRef.delete() } throws Exception("File not found")

    // Mock getGroup
    every { mockDocument.get() } returns Tasks.forResult(mockSnapshot)
    every { mockSnapshot.id } returns testGroupId
    every { mockSnapshot.getString("name") } returns testGroup.name
    every { mockSnapshot.getString("description") } returns testGroup.description
    every { mockSnapshot.getString("ownerId") } returns testGroup.ownerId
    every { mockSnapshot.getString("category") } returns EventType.SPORTS.name
    every { mockSnapshot.get("memberIds") } returns testGroup.memberIds
    every { mockSnapshot.get("eventIds") } returns testGroup.eventIds
    every { mockSnapshot.get("serieIds") } returns emptyList<String>()
    every { mockSnapshot.getString("photoUrl") } returns testGroup.photoUrl
    every { mockDocument.set(any()) } returns Tasks.forResult(null)

    // When - should not throw, continues to clear photoUrl
    repository.deleteGroupPhoto(testGroupId)

    // Then - still updates Firestore even if storage delete failed
    verify { mockDocument.set(match<Group> { it.photoUrl == null }) }
  }

  // ---------------- GET COMMON GROUPS TESTS ----------------

  @Test
  fun getCommonGroups_returnsEmptyListWhenNoUserIds() = runTest {
    // When
    val result = repository.getCommonGroups(emptyList())

    // Then
    assertTrue(result.isEmpty())
  }

  @Test
  fun getCommonGroups_returnsGroupsForSingleUser() = runTest {
    // Given
    val userId = "user1"
    val mockQuery = mockk<Query>(relaxed = true)
    val mockQuerySnapshot = mockk<QuerySnapshot>(relaxed = true)
    val mockSnapshot1 = mockk<QueryDocumentSnapshot>(relaxed = true)
    val mockSnapshot2 = mockk<QueryDocumentSnapshot>(relaxed = true)

    every { mockCollection.whereArrayContains("memberIds", userId) } returns mockQuery
    every { mockQuery.get() } returns Tasks.forResult(mockQuerySnapshot)
    every { mockQuerySnapshot.iterator() } returns
        mutableListOf(mockSnapshot1, mockSnapshot2).iterator()

    // Group 1
    every { mockSnapshot1.id } returns "group1"
    every { mockSnapshot1.getString("name") } returns "Basketball Club"
    every { mockSnapshot1.getString("ownerId") } returns userId
    every { mockSnapshot1.getString("category") } returns EventType.SPORTS.name
    every { mockSnapshot1.getString("description") } returns "Play basketball"
    every { mockSnapshot1.get("memberIds") } returns listOf(userId, "user2")
    every { mockSnapshot1.get("eventIds") } returns emptyList<String>()
    every { mockSnapshot1.get("serieIds") } returns emptyList<String>()
    every { mockSnapshot1.getString("photoUrl") } returns null

    // Group 2
    every { mockSnapshot2.id } returns "group2"
    every { mockSnapshot2.getString("name") } returns "Running Club"
    every { mockSnapshot2.getString("ownerId") } returns "user2"
    every { mockSnapshot2.getString("category") } returns EventType.SPORTS.name
    every { mockSnapshot2.getString("description") } returns "Go running"
    every { mockSnapshot2.get("memberIds") } returns listOf(userId, "user3")
    every { mockSnapshot2.get("eventIds") } returns emptyList<String>()
    every { mockSnapshot2.get("serieIds") } returns emptyList<String>()
    every { mockSnapshot2.getString("photoUrl") } returns null

    // When
    val result = repository.getCommonGroups(listOf(userId))

    // Then
    assertEquals(2, result.size)
    val names = result.map { it.name }
    assert(names.contains("Basketball Club"))
    assert(names.contains("Running Club"))
  }

  @Test
  fun getCommonGroups_filtersForMultipleUsers() = runTest {
    // Given
    val user1 = "user1"
    val user2 = "user2"
    val mockQuery = mockk<Query>(relaxed = true)
    val mockQuerySnapshot = mockk<QuerySnapshot>(relaxed = true)
    val mockSnapshot1 = mockk<QueryDocumentSnapshot>(relaxed = true)
    val mockSnapshot2 = mockk<QueryDocumentSnapshot>(relaxed = true)
    val mockSnapshot3 = mockk<QueryDocumentSnapshot>(relaxed = true)

    every { mockCollection.whereArrayContains("memberIds", user1) } returns mockQuery
    every { mockQuery.get() } returns Tasks.forResult(mockQuerySnapshot)
    every { mockQuerySnapshot.iterator() } returns
        mutableListOf(mockSnapshot1, mockSnapshot2, mockSnapshot3).iterator()

    // Group 1 - has both users
    every { mockSnapshot1.id } returns "group1"
    every { mockSnapshot1.getString("name") } returns "Common Group"
    every { mockSnapshot1.getString("ownerId") } returns user1
    every { mockSnapshot1.getString("category") } returns EventType.SPORTS.name
    every { mockSnapshot1.getString("description") } returns "Both users"
    every { mockSnapshot1.get("memberIds") } returns listOf(user1, user2, "user3")
    every { mockSnapshot1.get("eventIds") } returns emptyList<String>()
    every { mockSnapshot1.get("serieIds") } returns emptyList<String>()
    every { mockSnapshot1.getString("photoUrl") } returns null

    // Group 2 - only user1
    every { mockSnapshot2.id } returns "group2"
    every { mockSnapshot2.getString("name") } returns "User1 Only"
    every { mockSnapshot2.getString("ownerId") } returns user1
    every { mockSnapshot2.getString("category") } returns EventType.ACTIVITY.name
    every { mockSnapshot2.getString("description") } returns "Only user1"
    every { mockSnapshot2.get("memberIds") } returns listOf(user1, "user3")
    every { mockSnapshot2.get("eventIds") } returns emptyList<String>()
    every { mockSnapshot2.get("serieIds") } returns emptyList<String>()
    every { mockSnapshot2.getString("photoUrl") } returns null

    // Group 3 - has both users
    every { mockSnapshot3.id } returns "group3"
    every { mockSnapshot3.getString("name") } returns "Another Common"
    every { mockSnapshot3.getString("ownerId") } returns user2
    every { mockSnapshot3.getString("category") } returns EventType.SOCIAL.name
    every { mockSnapshot3.getString("description") } returns "Both again"
    every { mockSnapshot3.get("memberIds") } returns listOf(user1, user2)
    every { mockSnapshot3.get("eventIds") } returns emptyList<String>()
    every { mockSnapshot3.get("serieIds") } returns emptyList<String>()
    every { mockSnapshot3.getString("photoUrl") } returns null

    // When
    val result = repository.getCommonGroups(listOf(user1, user2))

    // Then - only groups with both users
    assertEquals(2, result.size)
    val names = result.map { it.name }
    assert(names.contains("Common Group"))
    assert(names.contains("Another Common"))
    assert(!names.contains("User1 Only"))
  }

  @Test
  fun getCommonGroups_returnsEmptyWhenNoCommonGroups() = runTest {
    // Given
    val user1 = "user1"
    val user2 = "user2"
    val mockQuery = mockk<Query>(relaxed = true)
    val mockQuerySnapshot = mockk<QuerySnapshot>(relaxed = true)
    val mockSnapshot = mockk<QueryDocumentSnapshot>(relaxed = true)

    every { mockCollection.whereArrayContains("memberIds", user1) } returns mockQuery
    every { mockQuery.get() } returns Tasks.forResult(mockQuerySnapshot)
    every { mockQuerySnapshot.iterator() } returns mutableListOf(mockSnapshot).iterator()

    // Group only has user1, not user2
    every { mockSnapshot.id } returns "group1"
    every { mockSnapshot.getString("name") } returns "No Common"
    every { mockSnapshot.getString("ownerId") } returns user1
    every { mockSnapshot.getString("category") } returns EventType.SPORTS.name
    every { mockSnapshot.getString("description") } returns "Only user1"
    every { mockSnapshot.get("memberIds") } returns listOf(user1)
    every { mockSnapshot.get("eventIds") } returns emptyList<String>()
    every { mockSnapshot.get("serieIds") } returns emptyList<String>()
    every { mockSnapshot.getString("photoUrl") } returns null

    // When
    val result = repository.getCommonGroups(listOf(user1, user2))

    // Then
    assertEquals(0, result.size)
  }

  @Test
  fun getCommonGroups_handlesInvalidDocuments() = runTest {
    // Given
    val userId = "user1"
    val mockQuery = mockk<Query>(relaxed = true)
    val mockQuerySnapshot = mockk<QuerySnapshot>(relaxed = true)
    val mockSnapshot1 = mockk<QueryDocumentSnapshot>(relaxed = true)
    val mockSnapshot2 = mockk<QueryDocumentSnapshot>(relaxed = true)

    every { mockCollection.whereArrayContains("memberIds", userId) } returns mockQuery
    every { mockQuery.get() } returns Tasks.forResult(mockQuerySnapshot)
    every { mockQuerySnapshot.iterator() } returns
        mutableListOf(mockSnapshot1, mockSnapshot2).iterator()

    // Valid group
    every { mockSnapshot1.id } returns "group1"
    every { mockSnapshot1.getString("name") } returns "Valid Group"
    every { mockSnapshot1.getString("ownerId") } returns userId
    every { mockSnapshot1.getString("category") } returns EventType.SPORTS.name
    every { mockSnapshot1.getString("description") } returns "Good"
    every { mockSnapshot1.get("memberIds") } returns listOf(userId)
    every { mockSnapshot1.get("eventIds") } returns emptyList<String>()
    every { mockSnapshot1.get("serieIds") } returns emptyList<String>()
    every { mockSnapshot1.getString("photoUrl") } returns null

    // Invalid group (missing name)
    every { mockSnapshot2.id } returns "group2"
    every { mockSnapshot2.getString("name") } returns null
    every { mockSnapshot2.getString("ownerId") } returns userId
    every { mockSnapshot2.getString("category") } returns EventType.ACTIVITY.name
    every { mockSnapshot2.getString("description") } returns "Bad"
    every { mockSnapshot2.get("memberIds") } returns listOf(userId)
    every { mockSnapshot2.get("eventIds") } returns emptyList<String>()
    every { mockSnapshot2.get("serieIds") } returns emptyList<String>()
    every { mockSnapshot2.getString("photoUrl") } returns null

    // When
    val result = repository.getCommonGroups(listOf(userId))

    // Then - only valid group is returned
    assertEquals(1, result.size)
    assertEquals("Valid Group", result[0].name)
  }

  @Test
  fun getCommonGroups_requiresAllUsersToBeMembers() = runTest {
    // Given
    val user1 = "user1"
    val user2 = "user2"
    val user3 = "user3"
    val mockQuery = mockk<Query>(relaxed = true)
    val mockQuerySnapshot = mockk<QuerySnapshot>(relaxed = true)
    val mockSnapshot1 = mockk<QueryDocumentSnapshot>(relaxed = true)
    val mockSnapshot2 = mockk<QueryDocumentSnapshot>(relaxed = true)

    every { mockCollection.whereArrayContains("memberIds", user1) } returns mockQuery
    every { mockQuery.get() } returns Tasks.forResult(mockQuerySnapshot)
    every { mockQuerySnapshot.iterator() } returns
        mutableListOf(mockSnapshot1, mockSnapshot2).iterator()

    // Group 1 - has all three users
    every { mockSnapshot1.id } returns "group1"
    every { mockSnapshot1.getString("name") } returns "All Three"
    every { mockSnapshot1.getString("ownerId") } returns user1
    every { mockSnapshot1.getString("category") } returns EventType.SPORTS.name
    every { mockSnapshot1.getString("description") } returns "All members"
    every { mockSnapshot1.get("memberIds") } returns listOf(user1, user2, user3)
    every { mockSnapshot1.get("eventIds") } returns emptyList<String>()
    every { mockSnapshot1.get("serieIds") } returns emptyList<String>()
    every { mockSnapshot1.getString("photoUrl") } returns null

    // Group 2 - only has user1 and user2
    every { mockSnapshot2.id } returns "group2"
    every { mockSnapshot2.getString("name") } returns "Only Two"
    every { mockSnapshot2.getString("ownerId") } returns user1
    every { mockSnapshot2.getString("category") } returns EventType.ACTIVITY.name
    every { mockSnapshot2.getString("description") } returns "Missing user3"
    every { mockSnapshot2.get("memberIds") } returns listOf(user1, user2)
    every { mockSnapshot2.get("eventIds") } returns emptyList<String>()
    every { mockSnapshot2.get("serieIds") } returns emptyList<String>()
    every { mockSnapshot2.getString("photoUrl") } returns null

    // When
    val result = repository.getCommonGroups(listOf(user1, user2, user3))

    // Then - only group with all three users
    assertEquals(1, result.size)
    assertEquals("All Three", result[0].name)
  }
}
