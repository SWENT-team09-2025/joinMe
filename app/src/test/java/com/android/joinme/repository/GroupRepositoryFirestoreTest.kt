package com.android.joinme.repository

import com.android.joinme.model.event.EventType
import com.android.joinme.model.group.GROUPS_COLLECTION_PATH
import com.android.joinme.model.group.GroupRepositoryFirestore
import com.android.joinme.model.group.MEMBERSHIPS_COLLECTION_PATH
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for GroupRepositoryFirestore.
 *
 * Tests Firestore integration with mocked Firebase dependencies.
 */
class GroupRepositoryFirestoreTest {

  private lateinit var mockFirestore: FirebaseFirestore
  private lateinit var mockGroupsCollection: CollectionReference
  private lateinit var mockMembershipsCollection: CollectionReference
  private lateinit var mockAuth: FirebaseAuth
  private lateinit var mockUser: FirebaseUser
  private lateinit var repository: GroupRepositoryFirestore

  private val testUid = "test-user-123"

  @Before
  fun setUp() {
    mockFirestore = mockk(relaxed = true)
    mockGroupsCollection = mockk(relaxed = true)
    mockMembershipsCollection = mockk(relaxed = true)

    mockAuth = mockk(relaxed = true)
    mockUser = mockk(relaxed = true)

    mockkStatic(FirebaseAuth::class)
    every { Firebase.auth } returns mockAuth
    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns testUid

    every { mockFirestore.collection(GROUPS_COLLECTION_PATH) } returns mockGroupsCollection
    every { mockFirestore.collection(MEMBERSHIPS_COLLECTION_PATH) } returns
        mockMembershipsCollection

    repository = GroupRepositoryFirestore(mockFirestore)
  }

  @After
  fun tearDown() {
    clearAllMocks()
    unmockkAll()
  }

  @Test
  fun userGroups_withNoMemberships_returnsEmptyList() = runTest {
    val mockMembershipQuery = mockk<Query>(relaxed = true)
    val mockMembershipSnapshot = mockk<QuerySnapshot>(relaxed = true)

    every { mockMembershipsCollection.whereEqualTo("userId", testUid) } returns mockMembershipQuery
    every { mockMembershipQuery.get() } returns Tasks.forResult(mockMembershipSnapshot)
    every { mockMembershipSnapshot.documents } returns emptyList()

    val result = repository.userGroups()

    assertTrue(result.isEmpty())
    verify { mockMembershipsCollection.whereEqualTo("userId", testUid) }
  }

  @Test
  fun leaveGroup_successfullyDeletesMembership() = runTest {
    val groupId = "group-to-leave"
    val mockMembershipQuery = mockk<Query>(relaxed = true)
    val mockMembershipSnapshot = mockk<QuerySnapshot>(relaxed = true)
    val mockMembershipDoc = mockk<DocumentSnapshot>(relaxed = true)
    val mockDocRef = mockk<DocumentReference>(relaxed = true)

    every {
      mockMembershipsCollection.whereEqualTo("userId", testUid).whereEqualTo("groupId", groupId)
    } returns mockMembershipQuery
    every { mockMembershipQuery.get() } returns Tasks.forResult(mockMembershipSnapshot)
    every { mockMembershipSnapshot.documents } returns listOf(mockMembershipDoc)
    every { mockMembershipDoc.reference } returns mockDocRef
    every { mockDocRef.delete() } returns Tasks.forResult(null)

    repository.leaveGroup(groupId)

    verify { mockMembershipsCollection.whereEqualTo("userId", testUid) }
    verify { mockDocRef.delete() }
  }

  @Test
  fun leaveGroup_withMultipleMemberships_deletesAll() = runTest {
    val groupId = "group-1"
    val mockMembershipQuery = mockk<Query>(relaxed = true)
    val mockMembershipSnapshot = mockk<QuerySnapshot>(relaxed = true)
    val mockMembershipDocs =
        listOf(mockk<DocumentSnapshot>(relaxed = true), mockk<DocumentSnapshot>(relaxed = true))
    val mockDocRefs =
        listOf(mockk<DocumentReference>(relaxed = true), mockk<DocumentReference>(relaxed = true))

    every {
      mockMembershipsCollection.whereEqualTo("userId", testUid).whereEqualTo("groupId", groupId)
    } returns mockMembershipQuery
    every { mockMembershipQuery.get() } returns Tasks.forResult(mockMembershipSnapshot)
    every { mockMembershipSnapshot.documents } returns mockMembershipDocs
    mockMembershipDocs.forEachIndexed { index, doc ->
      every { doc.reference } returns mockDocRefs[index]
      every { mockDocRefs[index].delete() } returns Tasks.forResult(null)
    }

    repository.leaveGroup(groupId)

    verify(exactly = 1) { mockDocRefs[0].delete() }
    verify(exactly = 1) { mockDocRefs[1].delete() }
  }

  @Test(expected = Exception::class)
  fun leaveGroup_whenUserNotLoggedIn_throwsException() = runTest {
    every { mockAuth.currentUser } returns null

    repository.leaveGroup("group-1")
  }

  @Test
  fun leaveGroup_withNoMemberships_completesWithoutError() = runTest {
    val groupId = "non-member-group"
    val mockMembershipQuery = mockk<Query>(relaxed = true)
    val mockMembershipSnapshot = mockk<QuerySnapshot>(relaxed = true)

    every {
      mockMembershipsCollection.whereEqualTo("userId", testUid).whereEqualTo("groupId", groupId)
    } returns mockMembershipQuery
    every { mockMembershipQuery.get() } returns Tasks.forResult(mockMembershipSnapshot)
    every { mockMembershipSnapshot.documents } returns emptyList()

    repository.leaveGroup(groupId)

    verify { mockMembershipQuery.get() }
  }

  @Test
  fun getGroup_withExistingGroup_returnsGroup() = runTest {
    val groupId = "test-group-1"
    val mockDocRef = mockk<DocumentReference>(relaxed = true)
    val mockDocSnapshot = mockk<DocumentSnapshot>(relaxed = true)

    every { mockGroupsCollection.document(groupId) } returns mockDocRef
    every { mockDocRef.get() } returns Tasks.forResult(mockDocSnapshot)
    every { mockDocSnapshot.id } returns groupId
    every { mockDocSnapshot.getString("name") } returns "Test Group"
    every { mockDocSnapshot.getString("category") } returns "SPORTS" // Return as String
    every { mockDocSnapshot.getString("ownerId") } returns "owner-123"
    every { mockDocSnapshot.getString("description") } returns "Test Description"
    every { mockDocSnapshot.getString("photoUrl") } returns null
    every { mockDocSnapshot.get("memberIds") } returns listOf("user1", "user2", "user3")
    every { mockDocSnapshot.get("eventIds") } returns listOf("event1", "event2")

    val result = repository.getGroup(groupId)

    assertNotNull(result)
    assertEquals(groupId, result?.id)
    assertEquals("Test Group", result?.name)
    assertEquals(EventType.SPORTS, result?.category)
    assertEquals("owner-123", result?.ownerId)
    assertEquals("Test Description", result?.description)
    assertEquals(3, result?.membersCount)
    assertEquals(listOf("user1", "user2", "user3"), result?.memberIds)
    assertEquals(listOf("event1", "event2"), result?.eventIds)
    verify { mockGroupsCollection.document(groupId) }
  }

  @Test
  fun getGroup_withNonExistentGroup_returnsNull() = runTest {
    val groupId = "non-existent"
    val mockDocRef = mockk<DocumentReference>(relaxed = true)
    val mockDocSnapshot = mockk<DocumentSnapshot>(relaxed = true)

    every { mockGroupsCollection.document(groupId) } returns mockDocRef
    every { mockDocRef.get() } returns Tasks.forResult(mockDocSnapshot)
    every { mockDocSnapshot.id } returns groupId
    every { mockDocSnapshot.getString("name") } returns null
    every { mockDocSnapshot.getString("ownerId") } returns null

    val result = repository.getGroup(groupId)

    assertNull(result)
  }

  @Test
  fun getGroup_withMissingOptionalFields_returnsGroupWithDefaults() = runTest {
    val groupId = "minimal-group"
    val mockDocRef = mockk<DocumentReference>(relaxed = true)
    val mockDocSnapshot = mockk<DocumentSnapshot>(relaxed = true)

    every { mockGroupsCollection.document(groupId) } returns mockDocRef
    every { mockDocRef.get() } returns Tasks.forResult(mockDocSnapshot)
    every { mockDocSnapshot.id } returns groupId
    every { mockDocSnapshot.getString("name") } returns "Minimal Group"
    every { mockDocSnapshot.getString("category") } returns null // Missing category
    every { mockDocSnapshot.getString("ownerId") } returns "owner-456"
    every { mockDocSnapshot.getString("description") } returns null
    every { mockDocSnapshot.getString("photoUrl") } returns null
    every { mockDocSnapshot.get("memberIds") } returns null
    every { mockDocSnapshot.get("eventIds") } returns null

    val result = repository.getGroup(groupId)

    assertNotNull(result)
    assertEquals("Minimal Group", result?.name)
    assertEquals("owner-456", result?.ownerId)
    assertEquals(EventType.ACTIVITY, result?.category) // Default category
    assertEquals("", result?.description)
    assertEquals(0, result?.membersCount)
    assertEquals(emptyList<String>(), result?.memberIds)
    assertEquals(emptyList<String>(), result?.eventIds)
    assertNull(result?.photoUrl)
  }

  @Test
  fun getGroup_withSpecialCharacters_handlesCorrectly() = runTest {
    val groupId = "special-group"
    val mockDocRef = mockk<DocumentReference>(relaxed = true)
    val mockDocSnapshot = mockk<DocumentSnapshot>(relaxed = true)

    every { mockGroupsCollection.document(groupId) } returns mockDocRef
    every { mockDocRef.get() } returns Tasks.forResult(mockDocSnapshot)
    every { mockDocSnapshot.id } returns groupId
    every { mockDocSnapshot.getString("name") } returns "Café & Restaurant 🍽️"
    every { mockDocSnapshot.getString("category") } returns "SOCIAL"
    every { mockDocSnapshot.getString("ownerId") } returns "owner-James-123"
    every { mockDocSnapshot.getString("description") } returns "Special: €$£¥"
    every { mockDocSnapshot.getString("photoUrl") } returns null
    every { mockDocSnapshot.get("memberIds") } returns listOf("user1", "user2")
    every { mockDocSnapshot.get("eventIds") } returns emptyList<String>()

    val result = repository.getGroup(groupId)

    assertNotNull(result)
    assertEquals("Café & Restaurant 🍽️", result?.name)
    assertEquals(EventType.SOCIAL, result?.category)
    assertEquals("owner-James-123", result?.ownerId)
    assertEquals("Special: €$£¥", result?.description)
    assertEquals(2, result?.membersCount)
  }

  @Test
  fun getGroup_withInvalidCategory_defaultsToActivity() = runTest {
    val groupId = "invalid-category-group"
    val mockDocRef = mockk<DocumentReference>(relaxed = true)
    val mockDocSnapshot = mockk<DocumentSnapshot>(relaxed = true)

    every { mockGroupsCollection.document(groupId) } returns mockDocRef
    every { mockDocRef.get() } returns Tasks.forResult(mockDocSnapshot)
    every { mockDocSnapshot.id } returns groupId
    every { mockDocSnapshot.getString("name") } returns "Group with Invalid Category"
    every { mockDocSnapshot.getString("category") } returns "INVALID_CATEGORY"
    every { mockDocSnapshot.getString("ownerId") } returns "owner-789"
    every { mockDocSnapshot.getString("description") } returns ""
    every { mockDocSnapshot.getString("photoUrl") } returns null
    every { mockDocSnapshot.get("memberIds") } returns emptyList<String>()
    every { mockDocSnapshot.get("eventIds") } returns emptyList<String>()

    val result = repository.getGroup(groupId)

    assertNotNull(result)
    assertEquals(EventType.ACTIVITY, result?.category) // Should default to ACTIVITY
  }

  @Test
  fun userGroups_withFirestoreException_propagatesException() = runTest {
    val mockMembershipQuery = mockk<Query>(relaxed = true)
    every { mockMembershipsCollection.whereEqualTo("userId", testUid) } returns mockMembershipQuery
    every { mockMembershipQuery.get() } returns
        Tasks.forException(Exception("Firestore connection error"))

    try {
      repository.userGroups()
      fail("Expected exception to be thrown")
    } catch (e: Exception) {
      assertTrue(e.message?.contains("Firestore connection error") ?: false)
    }
  }

  @Test
  fun getGroup_withCorruptedData_returnsNull() = runTest {
    val groupId = "corrupted-group"
    val mockDocRef = mockk<DocumentReference>(relaxed = true)
    val mockDocSnapshot = mockk<DocumentSnapshot>(relaxed = true)

    every { mockGroupsCollection.document(groupId) } returns mockDocRef
    every { mockDocRef.get() } returns Tasks.forResult(mockDocSnapshot)
    every { mockDocSnapshot.id } returns groupId
    every { mockDocSnapshot.getString("name") } throws RuntimeException("Data corruption")

    val result = repository.getGroup(groupId)

    assertNull(result)
  }

  // =======================================
  // Create Group Tests
  // =======================================

  @Test
  fun createGroup_successfullyCreatesGroupAndMembership() = runTest {
    val mockGroupDocRef = mockk<DocumentReference>(relaxed = true)
    val mockMembershipDocRef = mockk<DocumentReference>(relaxed = true)
    val mockBatch = mockk<com.google.firebase.firestore.WriteBatch>(relaxed = true)

    every { mockGroupsCollection.document() } returns mockGroupDocRef
    every { mockGroupDocRef.id } returns "generated-group-id"
    every { mockMembershipsCollection.document() } returns mockMembershipDocRef
    every { mockFirestore.batch() } returns mockBatch
    every { mockBatch.set(any<DocumentReference>(), any<Map<String, Any>>()) } returns mockBatch
    every { mockBatch.commit() } returns Tasks.forResult(null)

    val result =
        repository.createGroup(
            name = "Test Group", category = EventType.SOCIAL, description = "Test description")

    assertEquals("generated-group-id", result)
    verify(exactly = 1) {
      mockBatch.set(
          any<DocumentReference>(), match<Map<String, Any>> { it["name"] == "Test Group" })
    }
    verify(exactly = 1) {
      mockBatch.set(
          any<DocumentReference>(),
          match<Map<String, Any>> { it["userId"] == testUid && it["role"] == "admin" })
    }
    verify(exactly = 1) { mockBatch.commit() }
  }

  @Test
  fun createGroup_storesCorrectGroupData() = runTest {
    val mockGroupDocRef = mockk<DocumentReference>(relaxed = true)
    val mockMembershipDocRef = mockk<DocumentReference>(relaxed = true)
    val mockBatch = mockk<com.google.firebase.firestore.WriteBatch>(relaxed = true)
    val capturedData = mutableListOf<Map<String, Any>>()

    every { mockGroupsCollection.document() } returns mockGroupDocRef
    every { mockGroupDocRef.id } returns "group-123"
    every { mockMembershipsCollection.document() } returns mockMembershipDocRef
    every { mockFirestore.batch() } returns mockBatch
    every { mockBatch.set(any<DocumentReference>(), capture(capturedData)) } returns mockBatch
    every { mockBatch.commit() } returns Tasks.forResult(null)

    repository.createGroup(
        name = "Sports Team", category = EventType.SPORTS, description = "Weekly games")

    // Find the group data (has "name" field)
    val groupData = capturedData.find { it.containsKey("name") }
    assertNotNull("Group data should be captured", groupData)
    assertEquals("Sports Team", groupData!!["name"])
    assertEquals("SPORTS", groupData["category"]) // Stored as string
    assertEquals("Weekly games", groupData["description"])
    assertEquals(testUid, groupData["ownerId"])
    assertEquals(listOf(testUid), groupData["memberIds"])
    assertEquals(emptyList<String>(), groupData["eventIds"])
    assertNull(groupData["photoUrl"])
    assertNotNull(groupData["createdAt"])
    assertEquals(testUid, groupData["createdBy"])
  }

  @Test
  fun createGroup_storesCorrectMembershipData() = runTest {
    val mockGroupDocRef = mockk<DocumentReference>(relaxed = true)
    val mockMembershipDocRef = mockk<DocumentReference>(relaxed = true)
    val mockBatch = mockk<com.google.firebase.firestore.WriteBatch>(relaxed = true)
    val capturedData = mutableListOf<Map<String, Any>>()

    every { mockGroupsCollection.document() } returns mockGroupDocRef
    every { mockGroupDocRef.id } returns "group-456"
    every { mockMembershipsCollection.document() } returns mockMembershipDocRef
    every { mockFirestore.batch() } returns mockBatch
    every { mockBatch.set(any<DocumentReference>(), capture(capturedData)) } returns mockBatch
    every { mockBatch.commit() } returns Tasks.forResult(null)

    repository.createGroup(
        name = "Activity Group", category = EventType.ACTIVITY, description = "Fun activities")

    // Find the membership data (has "userId" field)
    val membershipData = capturedData.find { it.containsKey("userId") }
    assertNotNull("Membership data should be captured", membershipData)
    assertEquals(testUid, membershipData!!["userId"])
    assertEquals("group-456", membershipData["groupId"])
    assertEquals("admin", membershipData["role"])
  }

  @Test
  fun createGroup_withEmptyDescription_storesEmptyString() = runTest {
    val mockGroupDocRef = mockk<DocumentReference>(relaxed = true)
    val mockMembershipDocRef = mockk<DocumentReference>(relaxed = true)
    val mockBatch = mockk<com.google.firebase.firestore.WriteBatch>(relaxed = true)
    val capturedData = mutableListOf<Map<String, Any>>()

    every { mockGroupsCollection.document() } returns mockGroupDocRef
    every { mockGroupDocRef.id } returns "group-789"
    every { mockMembershipsCollection.document() } returns mockMembershipDocRef
    every { mockFirestore.batch() } returns mockBatch
    every { mockBatch.set(any<DocumentReference>(), capture(capturedData)) } returns mockBatch
    every { mockBatch.commit() } returns Tasks.forResult(null)

    repository.createGroup(name = "Minimal Group", category = EventType.ACTIVITY, description = "")

    // Find the group data (has "name" field)
    val groupData = capturedData.find { it.containsKey("name") }
    assertNotNull("Group data should be captured", groupData)
    assertEquals("", groupData!!["description"])
  }

  @Test
  fun createGroup_withAllCategories_storesCorrectly() = runTest {
    val categories = listOf(EventType.SOCIAL, EventType.ACTIVITY, EventType.SPORTS)

    categories.forEach { category ->
      val mockGroupDocRef = mockk<DocumentReference>(relaxed = true)
      val mockMembershipDocRef = mockk<DocumentReference>(relaxed = true)
      val mockBatch = mockk<com.google.firebase.firestore.WriteBatch>(relaxed = true)
      val capturedData = mutableListOf<Map<String, Any>>()

      every { mockGroupsCollection.document() } returns mockGroupDocRef
      every { mockGroupDocRef.id } returns "group-test-${category.name}"
      every { mockMembershipsCollection.document() } returns mockMembershipDocRef
      every { mockFirestore.batch() } returns mockBatch
      every { mockBatch.set(any<DocumentReference>(), capture(capturedData)) } returns mockBatch
      every { mockBatch.commit() } returns Tasks.forResult(null)

      repository.createGroup(name = "Test", category = category, description = "Test")

      // Find the group data (has "name" field)
      val groupData = capturedData.find { it.containsKey("name") }
      assertNotNull("Group data should be captured", groupData)
      assertEquals(category.name, groupData!!["category"])
    }
  }

  @Test(expected = IllegalStateException::class)
  fun createGroup_whenUserNotAuthenticated_throwsIllegalStateException() = runTest {
    every { mockAuth.currentUser } returns null

    repository.createGroup(name = "Test", category = EventType.SOCIAL, description = "Test")
  }

  @Test
  fun createGroup_whenBatchFails_propagatesException() = runTest {
    val mockGroupDocRef = mockk<DocumentReference>(relaxed = true)
    val mockMembershipDocRef = mockk<DocumentReference>(relaxed = true)
    val mockBatch = mockk<com.google.firebase.firestore.WriteBatch>(relaxed = true)

    every { mockGroupsCollection.document() } returns mockGroupDocRef
    every { mockGroupDocRef.id } returns "group-fail"
    every { mockMembershipsCollection.document() } returns mockMembershipDocRef
    every { mockFirestore.batch() } returns mockBatch
    every { mockBatch.set(any(), any<Map<String, Any>>()) } returns mockBatch
    every { mockBatch.commit() } returns Tasks.forException(Exception("Firestore error"))

    try {
      repository.createGroup(name = "Test", category = EventType.ACTIVITY, description = "Test")
      fail("Expected exception to be thrown")
    } catch (e: Exception) {
      assertTrue(e.message?.contains("Firestore error") ?: false)
    }
  }

  @Test
  fun createGroup_usesAtomicBatchWrite() = runTest {
    val mockGroupDocRef = mockk<DocumentReference>(relaxed = true)
    val mockMembershipDocRef = mockk<DocumentReference>(relaxed = true)
    val mockBatch = mockk<com.google.firebase.firestore.WriteBatch>(relaxed = true)

    every { mockGroupsCollection.document() } returns mockGroupDocRef
    every { mockGroupDocRef.id } returns "atomic-group"
    every { mockMembershipsCollection.document() } returns mockMembershipDocRef
    every { mockFirestore.batch() } returns mockBatch
    every { mockBatch.set(any(), any<Map<String, Any>>()) } returns mockBatch
    every { mockBatch.commit() } returns Tasks.forResult(null)

    repository.createGroup(
        name = "Atomic Test", category = EventType.SOCIAL, description = "Testing atomicity")

    // Verify batch was created and committed
    verify(exactly = 1) { mockFirestore.batch() }
    verify(exactly = 2) { mockBatch.set(any(), any<Map<String, Any>>()) }
    verify(exactly = 1) { mockBatch.commit() }
  }
}
