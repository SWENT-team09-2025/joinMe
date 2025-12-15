package com.android.joinme.model.invitation

import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.Date
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class InvitationRepositoryFirestoreTest {

  private lateinit var mockDb: FirebaseFirestore
  private lateinit var mockCollection: CollectionReference
  private lateinit var mockDocument: DocumentReference
  private lateinit var mockSnapshot: DocumentSnapshot
  private lateinit var mockQuery: Query
  private lateinit var mockQuerySnapshot: QuerySnapshot
  private lateinit var repository: InvitationRepositoryFirestore

  private val testToken = "test-token-123"
  private val testTargetId = "target-456"
  private val testCreatedBy = "user-789"

  @Before
  fun setup() {
    mockDb = mockk(relaxed = true)
    mockCollection = mockk(relaxed = true)
    mockDocument = mockk(relaxed = true)
    mockSnapshot = mockk(relaxed = true)
    mockQuery = mockk(relaxed = true)
    mockQuerySnapshot = mockk(relaxed = true)

    every { mockDb.collection("invitations") } returns mockCollection
    every { mockCollection.document(any()) } returns mockDocument
    every { mockCollection.whereEqualTo(any<String>(), any()) } returns mockQuery

    repository = InvitationRepositoryFirestore(mockDb)
  }

  @Test
  fun createInvitation_withExpirationDays_returnsToken() = runTest {
    every { mockDocument.set(any()) } returns Tasks.forResult(null)

    val result =
        repository.createInvitation(
            type = InvitationType.GROUP,
            targetId = testTargetId,
            createdBy = testCreatedBy,
            expiresInDays = 7.0)

    assertTrue(result.isSuccess)
    val token = result.getOrNull()
    assertNotNull(token)
    verify { mockDocument.set(any()) }
  }

  @Test
  fun createInvitation_withoutExpirationDays_returnsToken() = runTest {
    every { mockDocument.set(any()) } returns Tasks.forResult(null)

    val result =
        repository.createInvitation(
            type = InvitationType.EVENT,
            targetId = testTargetId,
            createdBy = testCreatedBy,
            expiresInDays = null)

    assertTrue(result.isSuccess)
    assertNotNull(result.getOrNull())
  }

  @Test
  fun createInvitation_firestoreFailure_returnsError() = runTest {
    val exception = Exception("Firestore error")
    every { mockDocument.set(any()) } returns Tasks.forException(exception)

    val result =
        repository.createInvitation(
            type = InvitationType.GROUP, targetId = testTargetId, createdBy = testCreatedBy)

    assertTrue(result.isFailure)
    assertEquals(exception, result.exceptionOrNull())
  }

  @Test
  fun resolveInvitation_validToken_returnsInvitation() = runTest {
    val futureDate = Timestamp(Date(System.currentTimeMillis() + 100000))
    every { mockDocument.get() } returns Tasks.forResult(mockSnapshot)
    every { mockSnapshot.exists() } returns true
    every { mockSnapshot.getString("token") } returns testToken
    every { mockSnapshot.getString("type") } returns "GROUP"
    every { mockSnapshot.getString("targetId") } returns testTargetId
    every { mockSnapshot.getString("createdBy") } returns testCreatedBy
    every { mockSnapshot.getTimestamp("createdAt") } returns Timestamp.now()
    every { mockSnapshot.getTimestamp("expiresAt") } returns futureDate

    val result = repository.resolveInvitation(testToken)

    assertTrue(result.isSuccess)
    val invitation = result.getOrNull()
    assertNotNull(invitation)
    assertEquals(testToken, invitation?.token)
    assertEquals(testTargetId, invitation?.targetId)
    assertEquals(InvitationType.GROUP, invitation?.type)
  }

  @Test
  fun resolveInvitation_tokenDoesNotExist_returnsNull() = runTest {
    every { mockDocument.get() } returns Tasks.forResult(mockSnapshot)
    every { mockSnapshot.exists() } returns false

    val result = repository.resolveInvitation(testToken)

    assertTrue(result.isSuccess)
    assertNull(result.getOrNull())
  }

  @Test
  fun resolveInvitation_invalidType_returnsNull() = runTest {
    every { mockDocument.get() } returns Tasks.forResult(mockSnapshot)
    every { mockSnapshot.exists() } returns true
    every { mockSnapshot.getString("type") } returns "INVALID_TYPE"

    val result = repository.resolveInvitation(testToken)

    assertTrue(result.isSuccess)
    assertNull(result.getOrNull())
  }

  @Test
  fun resolveInvitation_expiredInvitation_returnsNull() = runTest {
    val pastDate = Timestamp(Date(System.currentTimeMillis() - 100000))
    every { mockDocument.get() } returns Tasks.forResult(mockSnapshot)
    every { mockSnapshot.exists() } returns true
    every { mockSnapshot.getString("token") } returns testToken
    every { mockSnapshot.getString("type") } returns "GROUP"
    every { mockSnapshot.getString("targetId") } returns testTargetId
    every { mockSnapshot.getString("createdBy") } returns testCreatedBy
    every { mockSnapshot.getTimestamp("createdAt") } returns Timestamp.now()
    every { mockSnapshot.getTimestamp("expiresAt") } returns pastDate

    val result = repository.resolveInvitation(testToken)

    assertTrue(result.isSuccess)
    assertNull(result.getOrNull())
  }

  @Test
  fun revokeInvitation_success_deletesDocument() = runTest {
    every { mockDocument.delete() } returns Tasks.forResult(null)

    val result = repository.revokeInvitation(testToken)

    assertTrue(result.isSuccess)
    verify { mockCollection.document(testToken) }
    verify { mockDocument.delete() }
  }

  @Test
  fun revokeInvitation_failure_returnsError() = runTest {
    val exception = Exception("Delete failed")
    every { mockDocument.delete() } returns Tasks.forException(exception)

    val result = repository.revokeInvitation(testToken)

    assertTrue(result.isFailure)
  }

  @Test
  fun getInvitationsByUser_success_returnsInvitations() = runTest {
    val mockDoc1 = mockk<DocumentSnapshot>(relaxed = true)
    every { mockDoc1.getString("token") } returns "token1"
    every { mockDoc1.getString("type") } returns "GROUP"
    every { mockDoc1.getString("targetId") } returns "target1"
    every { mockDoc1.getString("createdBy") } returns testCreatedBy
    every { mockDoc1.getTimestamp("createdAt") } returns Timestamp.now()
    every { mockDoc1.getTimestamp("expiresAt") } returns null

    val mockDoc2 = mockk<DocumentSnapshot>(relaxed = true)
    every { mockDoc2.getString("token") } returns "token2"
    every { mockDoc2.getString("type") } returns "EVENT"
    every { mockDoc2.getString("targetId") } returns "target2"
    every { mockDoc2.getString("createdBy") } returns testCreatedBy
    every { mockDoc2.getTimestamp("createdAt") } returns Timestamp.now()
    every { mockDoc2.getTimestamp("expiresAt") } returns null

    every { mockQuery.get() } returns Tasks.forResult(mockQuerySnapshot)
    every { mockQuerySnapshot.documents } returns listOf(mockDoc1, mockDoc2)

    val result = repository.getInvitationsByUser(testCreatedBy)

    assertTrue(result.isSuccess)
    val invitations = result.getOrNull()
    assertEquals(2, invitations?.size)
    verify { mockCollection.whereEqualTo("createdBy", testCreatedBy) }
  }

  @Test
  fun getInvitationsByUser_emptyResult_returnsEmptyList() = runTest {
    every { mockQuery.get() } returns Tasks.forResult(mockQuerySnapshot)
    every { mockQuerySnapshot.documents } returns emptyList()

    val result = repository.getInvitationsByUser(testCreatedBy)

    assertTrue(result.isSuccess)
    assertTrue(result.getOrNull()?.isEmpty() == true)
  }

  @Test
  fun getInvitationsByUser_failure_returnsError() = runTest {
    val exception = Exception("Query failed")
    every { mockQuery.get() } returns Tasks.forException(exception)

    val result = repository.getInvitationsByUser(testCreatedBy)

    assertTrue(result.isFailure)
  }
}
