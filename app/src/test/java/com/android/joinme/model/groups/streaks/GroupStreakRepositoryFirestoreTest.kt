package com.android.joinme.model.groups.streaks

import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.SetOptions
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Tests for [GroupStreakRepositoryFirestore].
 *
 * Tests are grouped by shared setup to minimize CI execution time.
 */
class GroupStreakRepositoryFirestoreTest {

  private lateinit var mockDb: FirebaseFirestore
  private lateinit var mockGroupsCollection: CollectionReference
  private lateinit var mockGroupDocument: DocumentReference
  private lateinit var mockStreaksCollection: CollectionReference
  private lateinit var mockStreakDocument: DocumentReference
  private lateinit var repository: GroupStreakRepositoryFirestore

  private val testGroupId = "group123"
  private val testUserId = "user456"

  @Before
  fun setup() {
    mockDb = mockk(relaxed = true)
    mockGroupsCollection = mockk(relaxed = true)
    mockGroupDocument = mockk(relaxed = true)
    mockStreaksCollection = mockk(relaxed = true)
    mockStreakDocument = mockk(relaxed = true)

    // Wire up the collection path: groups/{groupId}/streaks/{userId}
    every { mockDb.collection("groups") } returns mockGroupsCollection
    every { mockGroupsCollection.document(testGroupId) } returns mockGroupDocument
    every { mockGroupDocument.collection("streaks") } returns mockStreaksCollection
    every { mockStreaksCollection.document(testUserId) } returns mockStreakDocument

    repository = GroupStreakRepositoryFirestore(mockDb)
  }

  // ==================== getStreaksForGroup ====================

  @Test
  fun `getStreaksForGroup returns all streaks for group`() = runTest {
    // Given
    val mockQuerySnapshot = mockk<QuerySnapshot>(relaxed = true)
    val mockDoc1 = mockk<DocumentSnapshot>(relaxed = true)
    val mockDoc2 = mockk<DocumentSnapshot>(relaxed = true)
    val timestamp = Timestamp.now()

    every { mockStreaksCollection.get() } returns Tasks.forResult(mockQuerySnapshot)
    every { mockQuerySnapshot.documents } returns listOf(mockDoc1, mockDoc2)

    // First user streak
    every { mockDoc1.id } returns "userA"
    every { mockDoc1.getLong("currentStreakWeeks") } returns 3L
    every { mockDoc1.getLong("currentStreakActivities") } returns 7L
    every { mockDoc1.getTimestamp("currentStreakStartDate") } returns timestamp
    every { mockDoc1.getTimestamp("lastActiveWeekStart") } returns timestamp
    every { mockDoc1.getLong("bestStreakWeeks") } returns 5L
    every { mockDoc1.getLong("bestStreakActivities") } returns 12L

    // Second user streak
    every { mockDoc2.id } returns "userB"
    every { mockDoc2.getLong("currentStreakWeeks") } returns 1L
    every { mockDoc2.getLong("currentStreakActivities") } returns 2L
    every { mockDoc2.getTimestamp("currentStreakStartDate") } returns timestamp
    every { mockDoc2.getTimestamp("lastActiveWeekStart") } returns timestamp
    every { mockDoc2.getLong("bestStreakWeeks") } returns 1L
    every { mockDoc2.getLong("bestStreakActivities") } returns 2L

    // When
    val result = repository.getStreaksForGroup(testGroupId)

    // Then
    assertEquals(2, result.size)
    assertEquals("userA", result[0].userId)
    assertEquals(testGroupId, result[0].groupId)
    assertEquals(3, result[0].currentStreakWeeks)
    assertEquals("userB", result[1].userId)
  }

  @Test
  fun `getStreaksForGroup returns empty list when no streaks exist`() = runTest {
    // Given
    val mockQuerySnapshot = mockk<QuerySnapshot>(relaxed = true)
    every { mockStreaksCollection.get() } returns Tasks.forResult(mockQuerySnapshot)
    every { mockQuerySnapshot.documents } returns emptyList()

    // When
    val result = repository.getStreaksForGroup(testGroupId)

    // Then
    assertEquals(0, result.size)
  }

  // ==================== getStreakForUser ====================

  @Test
  fun `getStreakForUser returns streak when document exists`() = runTest {
    // Given
    val mockSnapshot = mockk<DocumentSnapshot>(relaxed = true)
    val timestamp = Timestamp.now()

    every { mockStreakDocument.get() } returns Tasks.forResult(mockSnapshot)
    every { mockSnapshot.exists() } returns true
    every { mockSnapshot.id } returns testUserId
    every { mockSnapshot.getLong("currentStreakWeeks") } returns 4L
    every { mockSnapshot.getLong("currentStreakActivities") } returns 10L
    every { mockSnapshot.getTimestamp("currentStreakStartDate") } returns timestamp
    every { mockSnapshot.getTimestamp("lastActiveWeekStart") } returns timestamp
    every { mockSnapshot.getLong("bestStreakWeeks") } returns 8L
    every { mockSnapshot.getLong("bestStreakActivities") } returns 20L

    // When
    val result = repository.getStreakForUser(testGroupId, testUserId)

    // Then
    assertNotNull(result)
    assertEquals(testGroupId, result!!.groupId)
    assertEquals(testUserId, result.userId)
    assertEquals(4, result.currentStreakWeeks)
    assertEquals(10, result.currentStreakActivities)
    assertEquals(8, result.bestStreakWeeks)
    assertEquals(20, result.bestStreakActivities)
  }

  @Test
  fun `getStreakForUser returns null when document does not exist`() = runTest {
    // Given
    val mockSnapshot = mockk<DocumentSnapshot>(relaxed = true)
    every { mockStreakDocument.get() } returns Tasks.forResult(mockSnapshot)
    every { mockSnapshot.exists() } returns false

    // When
    val result = repository.getStreakForUser(testGroupId, testUserId)

    // Then
    assertNull(result)
  }

  @Test
  fun `getStreakForUser handles null fields with defaults`() = runTest {
    // Given
    val mockSnapshot = mockk<DocumentSnapshot>(relaxed = true)
    every { mockStreakDocument.get() } returns Tasks.forResult(mockSnapshot)
    every { mockSnapshot.exists() } returns true
    every { mockSnapshot.id } returns testUserId
    every { mockSnapshot.getLong("currentStreakWeeks") } returns null
    every { mockSnapshot.getLong("currentStreakActivities") } returns null
    every { mockSnapshot.getTimestamp("currentStreakStartDate") } returns null
    every { mockSnapshot.getTimestamp("lastActiveWeekStart") } returns null
    every { mockSnapshot.getLong("bestStreakWeeks") } returns null
    every { mockSnapshot.getLong("bestStreakActivities") } returns null

    // When
    val result = repository.getStreakForUser(testGroupId, testUserId)

    // Then
    assertNotNull(result)
    assertEquals(0, result!!.currentStreakWeeks)
    assertEquals(0, result.currentStreakActivities)
    assertEquals(0, result.bestStreakWeeks)
    assertEquals(0, result.bestStreakActivities)
  }

  // ==================== updateStreak ====================

  @Test
  fun `updateStreak writes correct data to Firestore`() = runTest {
    // Given
    val startDate = Timestamp.now()
    val lastActive = Timestamp.now()
    val streak =
        GroupStreak(
            groupId = testGroupId,
            userId = testUserId,
            currentStreakWeeks = 3,
            currentStreakActivities = 8,
            currentStreakStartDate = startDate,
            lastActiveWeekStart = lastActive,
            bestStreakWeeks = 5,
            bestStreakActivities = 15)
    every { mockStreakDocument.set(any<Map<String, Any?>>(), any<SetOptions>()) } returns
        Tasks.forResult(null)

    // When
    repository.updateStreak(testGroupId, testUserId, streak)

    // Then
    verify {
      mockStreakDocument.set(
          match<Map<String, Any?>> { data ->
            data["currentStreakWeeks"] == 3 &&
                data["currentStreakActivities"] == 8 &&
                data["currentStreakStartDate"] == startDate &&
                data["lastActiveWeekStart"] == lastActive &&
                data["bestStreakWeeks"] == 5 &&
                data["bestStreakActivities"] == 15 &&
                !data.containsKey("groupId") &&
                !data.containsKey("userId")
          },
          eq(SetOptions.merge()))
    }
  }
}
