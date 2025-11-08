package com.android.joinme.model.serie

import com.android.joinme.model.utils.Visibility
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
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
import java.util.Date
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class SeriesRepositoryFirestoreTest {

  private lateinit var mockDb: FirebaseFirestore
  private lateinit var mockCollection: CollectionReference
  private lateinit var mockDocument: DocumentReference
  private lateinit var mockAuth: FirebaseAuth
  private lateinit var mockUser: FirebaseUser
  private lateinit var repository: SeriesRepositoryFirestore

  private val testSerieId = "testSerie123"
  private val testUserId = "testUser456"
  private val testSerie =
      Serie(
          serieId = testSerieId,
          title = "Weekly Football",
          description = "Weekly football series at the park",
          date = Timestamp(Date()),
          participants = listOf("user1", "user2"),
          maxParticipants = 10,
          visibility = Visibility.PUBLIC,
          eventIds = listOf("event1", "event2", "event3"),
          ownerId = testUserId)

  @Before
  fun setup() {
    // Mock Firestore
    mockDb = mockk(relaxed = true)
    mockCollection = mockk(relaxed = true)
    mockDocument = mockk(relaxed = true)

    // Mock Firebase Auth
    mockAuth = mockk(relaxed = true)
    mockUser = mockk(relaxed = true)

    every { mockDb.collection(SERIES_COLLECTION_PATH) } returns mockCollection
    every { mockCollection.document(any()) } returns mockDocument
    every { mockCollection.document() } returns mockDocument
    every { mockDocument.id } returns testSerieId

    repository = SeriesRepositoryFirestore(mockDb)
  }

  @Test
  fun getNewSerieId_returnsValidId() {
    // When
    val serieId = repository.getNewSerieId()

    // Then
    assertNotNull(serieId)
    assertEquals(testSerieId, serieId)
    verify { mockDb.collection(SERIES_COLLECTION_PATH) }
    verify { mockCollection.document() }
  }

  @Test
  fun addSerie_callsFirestoreSet() = runTest {
    // Given
    every { mockDocument.set(any()) } returns Tasks.forResult(null)

    // When
    repository.addSerie(testSerie)

    // Then
    verify { mockCollection.document(testSerieId) }
    verify { mockDocument.set(testSerie) }
  }

  @Test
  fun getSerie_returnsSerieSuccessfully() = runTest {
    // Given
    val mockSnapshot = mockk<DocumentSnapshot>(relaxed = true)
    every { mockDocument.get() } returns Tasks.forResult(mockSnapshot)
    every { mockSnapshot.id } returns testSerieId
    every { mockSnapshot.getString("title") } returns "Weekly Football"
    every { mockSnapshot.getString("description") } returns "Weekly football series at the park"
    every { mockSnapshot.getTimestamp("date") } returns testSerie.date
    every { mockSnapshot.get("participants") } returns listOf("user1", "user2")
    every { mockSnapshot.getLong("maxParticipants") } returns 10L
    every { mockSnapshot.getString("visibility") } returns Visibility.PUBLIC.name
    every { mockSnapshot.get("eventIds") } returns listOf("event1", "event2", "event3")
    every { mockSnapshot.getString("ownerId") } returns testUserId

    // When
    val result = repository.getSerie(testSerieId)

    // Then
    assertNotNull(result)
    assertEquals(testSerieId, result.serieId)
    assertEquals("Weekly Football", result.title)
    assertEquals(testUserId, result.ownerId)
    assertEquals(3, result.eventIds.size)
  }

  @Test
  fun editSerie_callsFirestoreUpdate() = runTest {
    // Given
    val updatedSerie = testSerie.copy(title = "Updated Title")
    every { mockDocument.set(any()) } returns Tasks.forResult(null)

    // When
    repository.editSerie(testSerieId, updatedSerie)

    // Then
    verify { mockCollection.document(testSerieId) }
    verify { mockDocument.set(updatedSerie) }
  }

  @Test
  fun deleteSerie_callsFirestoreDelete() = runTest {
    // Given
    every { mockDocument.delete() } returns Tasks.forResult(null)

    // When
    repository.deleteSerie(testSerieId)

    // Then
    verify { mockCollection.document(testSerieId) }
    verify { mockDocument.delete() }
  }

  @Test
  fun getAllSeries_returnsSeriesForCurrentUser() = runTest {
    // Given
    mockkStatic(FirebaseAuth::class)
    every { FirebaseAuth.getInstance() } returns mockAuth
    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns testUserId

    val mockQuerySnapshot = mockk<QuerySnapshot>(relaxed = true)
    val mockSnapshot1 = mockk<com.google.firebase.firestore.QueryDocumentSnapshot>(relaxed = true)
    val mockSnapshot2 = mockk<com.google.firebase.firestore.QueryDocumentSnapshot>(relaxed = true)
    val mockQuery = mockk<com.google.firebase.firestore.Query>(relaxed = true)

    // Mock whereArrayContains query
    every { mockCollection.whereArrayContains("participants", testUserId) } returns mockQuery
    every { mockQuery.get() } returns Tasks.forResult(mockQuerySnapshot)
    every { mockQuerySnapshot.iterator() } returns
        mutableListOf(mockSnapshot1, mockSnapshot2).iterator()

    // Setup first serie - testUserId must be in participants
    every { mockSnapshot1.id } returns "serie1"
    every { mockSnapshot1.getString("title") } returns "Serie 1"
    every { mockSnapshot1.getString("description") } returns "Description 1"
    every { mockSnapshot1.getTimestamp("date") } returns Timestamp(Date())
    every { mockSnapshot1.get("participants") } returns listOf(testUserId, "user1")
    every { mockSnapshot1.getLong("maxParticipants") } returns 10L
    every { mockSnapshot1.getString("visibility") } returns Visibility.PUBLIC.name
    every { mockSnapshot1.get("eventIds") } returns listOf("event1")
    every { mockSnapshot1.getString("ownerId") } returns testUserId

    // Setup second serie - testUserId must be in participants
    every { mockSnapshot2.id } returns "serie2"
    every { mockSnapshot2.getString("title") } returns "Serie 2"
    every { mockSnapshot2.getString("description") } returns "Description 2"
    every { mockSnapshot2.getTimestamp("date") } returns Timestamp(Date())
    every { mockSnapshot2.get("participants") } returns listOf(testUserId, "user1", "user2")
    every { mockSnapshot2.getLong("maxParticipants") } returns 8L
    every { mockSnapshot2.getString("visibility") } returns Visibility.PRIVATE.name
    every { mockSnapshot2.get("eventIds") } returns listOf("event2", "event3")
    every { mockSnapshot2.getString("ownerId") } returns testUserId

    // When
    val result = repository.getAllSeries(SerieFilter.SERIES_FOR_OVERVIEW_SCREEN)

    // Then
    assertEquals(2, result.size)
    assertEquals("Serie 1", result[0].title)
    assertEquals("Serie 2", result[1].title)
  }

  @Test(expected = Exception::class)
  fun getSerie_throwsExceptionWhenNotFound() = runTest {
    // Given
    val mockSnapshot = mockk<DocumentSnapshot>(relaxed = true)
    every { mockDocument.get() } returns Tasks.forResult(mockSnapshot)
    every { mockSnapshot.getString("title") } returns null // Missing required field

    // When
    repository.getSerie(testSerieId)

    // Then - exception is thrown
  }

  @Test(expected = Exception::class)
  fun getAllSeries_throwsExceptionWhenUserNotLoggedIn() = runTest {
    // Given
    mockkStatic(FirebaseAuth::class)
    every { FirebaseAuth.getInstance() } returns mockAuth
    every { mockAuth.currentUser } returns null // User not logged in

    // When
    repository.getAllSeries(SerieFilter.SERIES_FOR_OVERVIEW_SCREEN)

    // Then - exception is thrown
  }

  @Test(expected = Exception::class)
  fun documentToSerie_handlesExceptionGracefully() = runTest {
    // Given
    val mockSnapshot = mockk<DocumentSnapshot>(relaxed = true)
    every { mockDocument.get() } returns Tasks.forResult(mockSnapshot)
    every { mockSnapshot.id } returns testSerieId
    every { mockSnapshot.getString("title") } returns "Test Serie"
    every { mockSnapshot.getString("description") } returns "Description"
    every { mockSnapshot.getTimestamp("date") } returns Timestamp(Date())
    every { mockSnapshot.get("participants") } returns emptyList<String>()
    every { mockSnapshot.getLong("maxParticipants") } returns 10L
    every { mockSnapshot.getString("visibility") } returns
        "INVALID_VISIBILITY" // Invalid enum value
    every { mockSnapshot.get("eventIds") } returns listOf("event1")
    every { mockSnapshot.getString("ownerId") } returns testUserId

    // When - documentToSerie catches the exception and returns null
    // getSerie then throws because documentToSerie returned null
    repository.getSerie(testSerieId)

    // Then - exception is thrown because documentToSerie returned null
  }

  @Test
  fun getSerie_withEmptyEventIds() = runTest {
    // Given
    val mockSnapshot = mockk<DocumentSnapshot>(relaxed = true)
    every { mockDocument.get() } returns Tasks.forResult(mockSnapshot)
    every { mockSnapshot.id } returns testSerieId
    every { mockSnapshot.getString("title") } returns "Empty Serie"
    every { mockSnapshot.getString("description") } returns "Serie with no events"
    every { mockSnapshot.getTimestamp("date") } returns testSerie.date
    every { mockSnapshot.get("participants") } returns emptyList<String>()
    every { mockSnapshot.getLong("maxParticipants") } returns 5L
    every { mockSnapshot.getString("visibility") } returns Visibility.PUBLIC.name
    every { mockSnapshot.get("eventIds") } returns emptyList<String>()
    every { mockSnapshot.getString("ownerId") } returns testUserId

    // When
    val result = repository.getSerie(testSerieId)

    // Then
    assertNotNull(result)
    assertEquals(0, result.eventIds.size)
  }


  @Test
  fun getSerie_withDefaultVisibilityWhenNull() = runTest {
    // Given
    val mockSnapshot = mockk<DocumentSnapshot>(relaxed = true)
    every { mockDocument.get() } returns Tasks.forResult(mockSnapshot)
    every { mockSnapshot.id } returns testSerieId
    every { mockSnapshot.getString("title") } returns "Serie with default visibility"
    every { mockSnapshot.getString("description") } returns "Description"
    every { mockSnapshot.getTimestamp("date") } returns testSerie.date
    every { mockSnapshot.get("participants") } returns emptyList<String>()
    every { mockSnapshot.getLong("maxParticipants") } returns 10L
    every { mockSnapshot.getString("visibility") } returns null // Null visibility
    every { mockSnapshot.get("eventIds") } returns listOf("event1")
    every { mockSnapshot.getString("ownerId") } returns testUserId

    // When
    val result = repository.getSerie(testSerieId)

    // Then
    assertNotNull(result)
    assertEquals(Visibility.PUBLIC, result.visibility) // Should default to PUBLIC
  }

  @Test
  fun getSerie_withEmptyDescriptionDefaultsToEmpty() = runTest {
    // Given
    val mockSnapshot = mockk<DocumentSnapshot>(relaxed = true)
    every { mockDocument.get() } returns Tasks.forResult(mockSnapshot)
    every { mockSnapshot.id } returns testSerieId
    every { mockSnapshot.getString("title") } returns "Serie Title"
    every { mockSnapshot.getString("description") } returns null // Null description
    every { mockSnapshot.getTimestamp("date") } returns testSerie.date
    every { mockSnapshot.get("participants") } returns emptyList<String>()
    every { mockSnapshot.getLong("maxParticipants") } returns 10L
    every { mockSnapshot.getString("visibility") } returns Visibility.PUBLIC.name
    every { mockSnapshot.get("eventIds") } returns listOf("event1")
    every { mockSnapshot.getString("ownerId") } returns testUserId

    // When
    val result = repository.getSerie(testSerieId)

    // Then
    assertNotNull(result)
    assertEquals("", result.description) // Should default to empty string
  }
}
