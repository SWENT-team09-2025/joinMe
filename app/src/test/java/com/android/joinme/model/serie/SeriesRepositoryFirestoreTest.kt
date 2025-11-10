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
    val testDate = Timestamp(Date())
    every { mockDocument.get() } returns Tasks.forResult(mockSnapshot)
    every { mockSnapshot.id } returns testSerieId
    every { mockSnapshot.getString("title") } returns "Weekly Football"
    every { mockSnapshot.getString("description") } returns "Weekly football series at the park"
    every { mockSnapshot.getTimestamp("date") } returns testDate
    every { mockSnapshot.get("participants") } returns listOf("user1", "user2")
    every { mockSnapshot.getLong("maxParticipants") } returns 10L
    every { mockSnapshot.getString("visibility") } returns Visibility.PUBLIC.name
    every { mockSnapshot.get("eventIds") } returns listOf("event1", "event2", "event3")
    every { mockSnapshot.getString("ownerId") } returns testUserId
    every { mockSnapshot.getTimestamp("lastEventEndTime") } returns testDate

    // When
    val result = repository.getSerie(testSerieId)

    // Then
    assertNotNull(result)
    assertEquals(testSerieId, result.serieId)
    assertEquals("Weekly Football", result.title)
    assertEquals(testUserId, result.ownerId)
    assertEquals(3, result.eventIds.size)
    assertEquals(testDate, result.lastEventEndTime)
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
    val date1 = Timestamp(Date())
    every { mockSnapshot1.id } returns "serie1"
    every { mockSnapshot1.getString("title") } returns "Serie 1"
    every { mockSnapshot1.getString("description") } returns "Description 1"
    every { mockSnapshot1.getTimestamp("date") } returns date1
    every { mockSnapshot1.get("participants") } returns listOf(testUserId, "user1")
    every { mockSnapshot1.getLong("maxParticipants") } returns 10L
    every { mockSnapshot1.getString("visibility") } returns Visibility.PUBLIC.name
    every { mockSnapshot1.get("eventIds") } returns listOf("event1")
    every { mockSnapshot1.getString("ownerId") } returns testUserId
    every { mockSnapshot1.getTimestamp("lastEventEndTime") } returns date1

    // Setup second serie - testUserId must be in participants
    val date2 = Timestamp(Date())
    every { mockSnapshot2.id } returns "serie2"
    every { mockSnapshot2.getString("title") } returns "Serie 2"
    every { mockSnapshot2.getString("description") } returns "Description 2"
    every { mockSnapshot2.getTimestamp("date") } returns date2
    every { mockSnapshot2.get("participants") } returns listOf(testUserId, "user1", "user2")
    every { mockSnapshot2.getLong("maxParticipants") } returns 8L
    every { mockSnapshot2.getString("visibility") } returns Visibility.PRIVATE.name
    every { mockSnapshot2.get("eventIds") } returns listOf("event2", "event3")
    every { mockSnapshot2.getString("ownerId") } returns testUserId
    every { mockSnapshot2.getTimestamp("lastEventEndTime") } returns date2

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
    val testDate = Timestamp(Date())
    every { mockDocument.get() } returns Tasks.forResult(mockSnapshot)
    every { mockSnapshot.id } returns testSerieId
    every { mockSnapshot.getString("title") } returns "Empty Serie"
    every { mockSnapshot.getString("description") } returns "Serie with no events"
    every { mockSnapshot.getTimestamp("date") } returns testDate
    every { mockSnapshot.get("participants") } returns emptyList<String>()
    every { mockSnapshot.getLong("maxParticipants") } returns 5L
    every { mockSnapshot.getString("visibility") } returns Visibility.PUBLIC.name
    every { mockSnapshot.get("eventIds") } returns emptyList<String>()
    every { mockSnapshot.getString("ownerId") } returns testUserId
    every { mockSnapshot.getTimestamp("lastEventEndTime") } returns null // Missing field

    // When
    val result = repository.getSerie(testSerieId)

    // Then
    assertNotNull(result)
    assertEquals(0, result.eventIds.size)
    assertEquals(testDate, result.lastEventEndTime) // Should default to date
  }

  @Test
  fun getSerie_withDefaultVisibilityWhenNull() = runTest {
    // Given
    val mockSnapshot = mockk<DocumentSnapshot>(relaxed = true)
    val testDate = Timestamp(Date())
    every { mockDocument.get() } returns Tasks.forResult(mockSnapshot)
    every { mockSnapshot.id } returns testSerieId
    every { mockSnapshot.getString("title") } returns "Serie with default visibility"
    every { mockSnapshot.getString("description") } returns "Description"
    every { mockSnapshot.getTimestamp("date") } returns testDate
    every { mockSnapshot.get("participants") } returns emptyList<String>()
    every { mockSnapshot.getLong("maxParticipants") } returns 10L
    every { mockSnapshot.getString("visibility") } returns null // Null visibility
    every { mockSnapshot.get("eventIds") } returns listOf("event1")
    every { mockSnapshot.getString("ownerId") } returns testUserId
    every { mockSnapshot.getTimestamp("lastEventEndTime") } returns testDate

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
    val testDate = Timestamp(Date())
    every { mockDocument.get() } returns Tasks.forResult(mockSnapshot)
    every { mockSnapshot.id } returns testSerieId
    every { mockSnapshot.getString("title") } returns "Serie Title"
    every { mockSnapshot.getString("description") } returns null // Null description
    every { mockSnapshot.getTimestamp("date") } returns testDate
    every { mockSnapshot.get("participants") } returns emptyList<String>()
    every { mockSnapshot.getLong("maxParticipants") } returns 10L
    every { mockSnapshot.getString("visibility") } returns Visibility.PUBLIC.name
    every { mockSnapshot.get("eventIds") } returns listOf("event1")
    every { mockSnapshot.getString("ownerId") } returns testUserId
    every { mockSnapshot.getTimestamp("lastEventEndTime") } returns testDate

    // When
    val result = repository.getSerie(testSerieId)

    // Then
    assertNotNull(result)
    assertEquals("", result.description) // Should default to empty string
  }

  @Test
  fun getSerie_withLastEventEndTime_returnsCorrectValue() = runTest {
    // Given
    val mockSnapshot = mockk<DocumentSnapshot>(relaxed = true)
    val testDate = Timestamp(Date())
    val lastEventDate = Timestamp(Date(testDate.toDate().time + 3600000)) // 1 hour later
    every { mockDocument.get() } returns Tasks.forResult(mockSnapshot)
    every { mockSnapshot.id } returns testSerieId
    every { mockSnapshot.getString("title") } returns "Serie with lastEventEndTime"
    every { mockSnapshot.getString("description") } returns "Test"
    every { mockSnapshot.getTimestamp("date") } returns testDate
    every { mockSnapshot.get("participants") } returns emptyList<String>()
    every { mockSnapshot.getLong("maxParticipants") } returns 10L
    every { mockSnapshot.getString("visibility") } returns Visibility.PUBLIC.name
    every { mockSnapshot.get("eventIds") } returns listOf("event1")
    every { mockSnapshot.getString("ownerId") } returns testUserId
    every { mockSnapshot.getTimestamp("lastEventEndTime") } returns lastEventDate

    // When
    val result = repository.getSerie(testSerieId)

    // Then
    assertNotNull(result)
    assertEquals(lastEventDate, result.lastEventEndTime)
  }

  @Test
  fun getSerie_withMissingLastEventEndTime_defaultsToDate() = runTest {
    // Given
    val mockSnapshot = mockk<DocumentSnapshot>(relaxed = true)
    val testDate = Timestamp(Date())
    every { mockDocument.get() } returns Tasks.forResult(mockSnapshot)
    every { mockSnapshot.id } returns testSerieId
    every { mockSnapshot.getString("title") } returns "Old Serie without lastEventEndTime"
    every { mockSnapshot.getString("description") } returns "Old data"
    every { mockSnapshot.getTimestamp("date") } returns testDate
    every { mockSnapshot.get("participants") } returns emptyList<String>()
    every { mockSnapshot.getLong("maxParticipants") } returns 10L
    every { mockSnapshot.getString("visibility") } returns Visibility.PUBLIC.name
    every { mockSnapshot.get("eventIds") } returns listOf("event1")
    every { mockSnapshot.getString("ownerId") } returns testUserId
    every { mockSnapshot.getTimestamp("lastEventEndTime") } returns null // Old data without field

    // When
    val result = repository.getSerie(testSerieId)

    // Then
    assertNotNull(result)
    assertEquals(testDate, result.lastEventEndTime) // Should default to serie date
  }

  @Test
  fun getAllSeries_withHistoryFilter_returnsOnlyExpiredSeriesSortedByDate() = runTest {
    // Given
    mockkStatic(FirebaseAuth::class)
    every { FirebaseAuth.getInstance() } returns mockAuth
    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns testUserId

    val mockQuerySnapshot = mockk<QuerySnapshot>(relaxed = true)
    val mockSnapshot1 = mockk<com.google.firebase.firestore.QueryDocumentSnapshot>(relaxed = true)
    val mockSnapshot2 = mockk<com.google.firebase.firestore.QueryDocumentSnapshot>(relaxed = true)
    val mockSnapshot3 = mockk<com.google.firebase.firestore.QueryDocumentSnapshot>(relaxed = true)
    val mockQuery = mockk<com.google.firebase.firestore.Query>(relaxed = true)

    // Mock whereArrayContains query
    every { mockCollection.whereArrayContains("participants", testUserId) } returns mockQuery
    every { mockQuery.get() } returns Tasks.forResult(mockQuerySnapshot)
    every { mockQuerySnapshot.iterator() } returns
        mutableListOf(mockSnapshot1, mockSnapshot2, mockSnapshot3).iterator()

    // Setup expired serie 1 (older)
    val expiredDate1 = Timestamp(Date(System.currentTimeMillis() - 10000000L))
    val expiredEndTime1 = Timestamp(Date(System.currentTimeMillis() - 5000000L))
    every { mockSnapshot1.id } returns "expiredSerie1"
    every { mockSnapshot1.getString("title") } returns "Expired Serie 1"
    every { mockSnapshot1.getString("description") } returns "Old series"
    every { mockSnapshot1.getTimestamp("date") } returns expiredDate1
    every { mockSnapshot1.get("participants") } returns listOf(testUserId)
    every { mockSnapshot1.getLong("maxParticipants") } returns 10L
    every { mockSnapshot1.getString("visibility") } returns Visibility.PUBLIC.name
    every { mockSnapshot1.get("eventIds") } returns listOf("event1")
    every { mockSnapshot1.getString("ownerId") } returns testUserId
    every { mockSnapshot1.getTimestamp("lastEventEndTime") } returns expiredEndTime1

    // Setup expired serie 2 (newer)
    val expiredDate2 = Timestamp(Date(System.currentTimeMillis() - 3000000L))
    val expiredEndTime2 = Timestamp(Date(System.currentTimeMillis() - 1000000L))
    every { mockSnapshot2.id } returns "expiredSerie2"
    every { mockSnapshot2.getString("title") } returns "Expired Serie 2"
    every { mockSnapshot2.getString("description") } returns "Recent old series"
    every { mockSnapshot2.getTimestamp("date") } returns expiredDate2
    every { mockSnapshot2.get("participants") } returns listOf(testUserId)
    every { mockSnapshot2.getLong("maxParticipants") } returns 10L
    every { mockSnapshot2.getString("visibility") } returns Visibility.PUBLIC.name
    every { mockSnapshot2.get("eventIds") } returns listOf("event2")
    every { mockSnapshot2.getString("ownerId") } returns testUserId
    every { mockSnapshot2.getTimestamp("lastEventEndTime") } returns expiredEndTime2

    // Setup upcoming serie (should be filtered out)
    val upcomingDate = Timestamp(Date(System.currentTimeMillis() + 5000000L))
    every { mockSnapshot3.id } returns "upcomingSerie"
    every { mockSnapshot3.getString("title") } returns "Upcoming Serie"
    every { mockSnapshot3.getString("description") } returns "Future series"
    every { mockSnapshot3.getTimestamp("date") } returns upcomingDate
    every { mockSnapshot3.get("participants") } returns listOf(testUserId)
    every { mockSnapshot3.getLong("maxParticipants") } returns 10L
    every { mockSnapshot3.getString("visibility") } returns Visibility.PUBLIC.name
    every { mockSnapshot3.get("eventIds") } returns listOf("event3")
    every { mockSnapshot3.getString("ownerId") } returns testUserId
    every { mockSnapshot3.getTimestamp("lastEventEndTime") } returns upcomingDate

    // When
    val result = repository.getAllSeries(SerieFilter.SERIES_FOR_HISTORY_SCREEN)

    // Then
    assertEquals(2, result.size)
    // Should be sorted by date descending (newest first)
    assertEquals("Expired Serie 2", result[0].title)
    assertEquals("Expired Serie 1", result[1].title)
  }

  @Test
  fun getAllSeries_withSearchFilter_returnsOnlyPublicUpcomingSeriesExcludingUser() = runTest {
    // Given
    mockkStatic(FirebaseAuth::class)
    every { FirebaseAuth.getInstance() } returns mockAuth
    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns testUserId

    val mockQuerySnapshot = mockk<QuerySnapshot>(relaxed = true)
    val mockSnapshot1 = mockk<com.google.firebase.firestore.QueryDocumentSnapshot>(relaxed = true)
    val mockSnapshot2 = mockk<com.google.firebase.firestore.QueryDocumentSnapshot>(relaxed = true)
    val mockSnapshot3 = mockk<com.google.firebase.firestore.QueryDocumentSnapshot>(relaxed = true)
    val mockSnapshot4 = mockk<com.google.firebase.firestore.QueryDocumentSnapshot>(relaxed = true)
    val mockQuery = mockk<com.google.firebase.firestore.Query>(relaxed = true)

    // Mock whereEqualTo visibility query
    every { mockCollection.whereEqualTo("visibility", any()) } returns mockQuery
    every { mockQuery.get() } returns Tasks.forResult(mockQuerySnapshot)
    every { mockQuerySnapshot.iterator() } returns
        mutableListOf(mockSnapshot1, mockSnapshot2, mockSnapshot3, mockSnapshot4).iterator()

    // Setup valid serie for search (public, upcoming, user not participant or owner)
    val upcomingDate1 = Timestamp(Date(System.currentTimeMillis() + 5000000L))
    every { mockSnapshot1.id } returns "searchSerie1"
    every { mockSnapshot1.getString("title") } returns "Search Serie 1"
    every { mockSnapshot1.getString("description") } returns "Public upcoming series"
    every { mockSnapshot1.getTimestamp("date") } returns upcomingDate1
    every { mockSnapshot1.get("participants") } returns listOf("otherUser1", "otherUser2")
    every { mockSnapshot1.getLong("maxParticipants") } returns 10L
    every { mockSnapshot1.getString("visibility") } returns Visibility.PUBLIC.name
    every { mockSnapshot1.get("eventIds") } returns listOf("event1")
    every { mockSnapshot1.getString("ownerId") } returns "otherUser1"
    every { mockSnapshot1.getTimestamp("lastEventEndTime") } returns upcomingDate1

    // Setup serie where user is participant (should be filtered out)
    val upcomingDate2 = Timestamp(Date(System.currentTimeMillis() + 6000000L))
    every { mockSnapshot2.id } returns "searchSerie2"
    every { mockSnapshot2.getString("title") } returns "Search Serie 2"
    every { mockSnapshot2.getString("description") } returns "User is participant"
    every { mockSnapshot2.getTimestamp("date") } returns upcomingDate2
    every { mockSnapshot2.get("participants") } returns listOf(testUserId, "otherUser1")
    every { mockSnapshot2.getLong("maxParticipants") } returns 10L
    every { mockSnapshot2.getString("visibility") } returns Visibility.PUBLIC.name
    every { mockSnapshot2.get("eventIds") } returns listOf("event2")
    every { mockSnapshot2.getString("ownerId") } returns "otherUser1"
    every { mockSnapshot2.getTimestamp("lastEventEndTime") } returns upcomingDate2

    // Setup serie where user is owner (should be filtered out)
    val upcomingDate3 = Timestamp(Date(System.currentTimeMillis() + 7000000L))
    every { mockSnapshot3.id } returns "searchSerie3"
    every { mockSnapshot3.getString("title") } returns "Search Serie 3"
    every { mockSnapshot3.getString("description") } returns "User is owner"
    every { mockSnapshot3.getTimestamp("date") } returns upcomingDate3
    every { mockSnapshot3.get("participants") } returns listOf("otherUser1")
    every { mockSnapshot3.getLong("maxParticipants") } returns 10L
    every { mockSnapshot3.getString("visibility") } returns Visibility.PUBLIC.name
    every { mockSnapshot3.get("eventIds") } returns listOf("event3")
    every { mockSnapshot3.getString("ownerId") } returns testUserId
    every { mockSnapshot3.getTimestamp("lastEventEndTime") } returns upcomingDate3

    // Setup expired serie (should be filtered out)
    val expiredDate = Timestamp(Date(System.currentTimeMillis() - 5000000L))
    val expiredEndTime = Timestamp(Date(System.currentTimeMillis() - 1000000L))
    every { mockSnapshot4.id } returns "searchSerie4"
    every { mockSnapshot4.getString("title") } returns "Search Serie 4"
    every { mockSnapshot4.getString("description") } returns "Expired series"
    every { mockSnapshot4.getTimestamp("date") } returns expiredDate
    every { mockSnapshot4.get("participants") } returns listOf("otherUser1")
    every { mockSnapshot4.getLong("maxParticipants") } returns 10L
    every { mockSnapshot4.getString("visibility") } returns Visibility.PUBLIC.name
    every { mockSnapshot4.get("eventIds") } returns listOf("event4")
    every { mockSnapshot4.getString("ownerId") } returns "otherUser1"
    every { mockSnapshot4.getTimestamp("lastEventEndTime") } returns expiredEndTime

    // When
    val result = repository.getAllSeries(SerieFilter.SERIES_FOR_SEARCH_SCREEN)

    // Then
    assertEquals(1, result.size)
    assertEquals("Search Serie 1", result[0].title)
  }

  @Test
  fun getAllSeries_withMapFilter_returnsUpcomingOrActiveSeriesForParticipants() = runTest {
    // Given
    mockkStatic(FirebaseAuth::class)
    every { FirebaseAuth.getInstance() } returns mockAuth
    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns testUserId

    val mockQuerySnapshot1 = mockk<QuerySnapshot>(relaxed = true)
    val mockQuerySnapshot2 = mockk<QuerySnapshot>(relaxed = true)
    val mockSnapshot1 = mockk<com.google.firebase.firestore.QueryDocumentSnapshot>(relaxed = true)
    val mockSnapshot2 = mockk<com.google.firebase.firestore.QueryDocumentSnapshot>(relaxed = true)
    val mockSnapshot3 = mockk<com.google.firebase.firestore.QueryDocumentSnapshot>(relaxed = true)
    val mockSnapshot4 = mockk<com.google.firebase.firestore.QueryDocumentSnapshot>(relaxed = true)
    val mockQuery1 = mockk<com.google.firebase.firestore.Query>(relaxed = true)
    val mockQuery2 = mockk<com.google.firebase.firestore.Query>(relaxed = true)

    // Mock two parallel queries
    every { mockCollection.whereArrayContains("participants", testUserId) } returns mockQuery1
    every { mockCollection.whereEqualTo("visibility", any()) } returns mockQuery2
    every { mockQuery1.get() } returns Tasks.forResult(mockQuerySnapshot1)
    every { mockQuery2.get() } returns Tasks.forResult(mockQuerySnapshot2)

    // First query results (user's series)
    every { mockQuerySnapshot1.iterator() } returns
        mutableListOf(mockSnapshot1, mockSnapshot2).iterator()

    // Second query results (public series)
    every { mockQuerySnapshot2.iterator() } returns
        mutableListOf(mockSnapshot3, mockSnapshot4).iterator()

    // Setup active serie where user is participant (should be included)
    val activeDate1 = Timestamp(Date(System.currentTimeMillis() - 1000000L))
    val activeEndTime1 = Timestamp(Date(System.currentTimeMillis() + 5000000L))
    every { mockSnapshot1.id } returns "activeSerie1"
    every { mockSnapshot1.getString("title") } returns "Active Serie 1"
    every { mockSnapshot1.getString("description") } returns "User is participant in active serie"
    every { mockSnapshot1.getTimestamp("date") } returns activeDate1
    every { mockSnapshot1.get("participants") } returns listOf(testUserId, "otherUser1")
    every { mockSnapshot1.getLong("maxParticipants") } returns 10L
    every { mockSnapshot1.getString("visibility") } returns Visibility.PUBLIC.name
    every { mockSnapshot1.get("eventIds") } returns listOf("event1")
    every { mockSnapshot1.getString("ownerId") } returns "otherUser1"
    every { mockSnapshot1.getTimestamp("lastEventEndTime") } returns activeEndTime1

    // Setup expired serie where user is participant (should be filtered out)
    val expiredDate1 = Timestamp(Date(System.currentTimeMillis() - 5000000L))
    val expiredEndTime1 = Timestamp(Date(System.currentTimeMillis() - 1000000L))
    every { mockSnapshot2.id } returns "expiredSerie1"
    every { mockSnapshot2.getString("title") } returns "Expired Serie 1"
    every { mockSnapshot2.getString("description") } returns "User is participant in expired serie"
    every { mockSnapshot2.getTimestamp("date") } returns expiredDate1
    every { mockSnapshot2.get("participants") } returns listOf(testUserId)
    every { mockSnapshot2.getLong("maxParticipants") } returns 10L
    every { mockSnapshot2.getString("visibility") } returns Visibility.PUBLIC.name
    every { mockSnapshot2.get("eventIds") } returns listOf("event2")
    every { mockSnapshot2.getString("ownerId") } returns "otherUser1"
    every { mockSnapshot2.getTimestamp("lastEventEndTime") } returns expiredEndTime1

    // Setup upcoming public serie (should be included)
    val upcomingDate1 = Timestamp(Date(System.currentTimeMillis() + 5000000L))
    every { mockSnapshot3.id } returns "upcomingSerie1"
    every { mockSnapshot3.getString("title") } returns "Upcoming Public Serie"
    every { mockSnapshot3.getString("description") } returns "Public upcoming series"
    every { mockSnapshot3.getTimestamp("date") } returns upcomingDate1
    every { mockSnapshot3.get("participants") } returns listOf("otherUser1")
    every { mockSnapshot3.getLong("maxParticipants") } returns 10L
    every { mockSnapshot3.getString("visibility") } returns Visibility.PUBLIC.name
    every { mockSnapshot3.get("eventIds") } returns listOf("event3")
    every { mockSnapshot3.getString("ownerId") } returns "otherUser1"
    every { mockSnapshot3.getTimestamp("lastEventEndTime") } returns upcomingDate1

    // Setup active public serie where user is NOT participant (should be filtered out)
    val activeDate2 = Timestamp(Date(System.currentTimeMillis() - 1000000L))
    val activeEndTime2 = Timestamp(Date(System.currentTimeMillis() + 5000000L))
    every { mockSnapshot4.id } returns "activeSerie2"
    every { mockSnapshot4.getString("title") } returns "Active Serie 2"
    every { mockSnapshot4.getString("description") } returns
        "User is NOT participant in active serie"
    every { mockSnapshot4.getTimestamp("date") } returns activeDate2
    every { mockSnapshot4.get("participants") } returns listOf("otherUser1", "otherUser2")
    every { mockSnapshot4.getLong("maxParticipants") } returns 10L
    every { mockSnapshot4.getString("visibility") } returns Visibility.PUBLIC.name
    every { mockSnapshot4.get("eventIds") } returns listOf("event4")
    every { mockSnapshot4.getString("ownerId") } returns "otherUser1"
    every { mockSnapshot4.getTimestamp("lastEventEndTime") } returns activeEndTime2

    // When
    val result = repository.getAllSeries(SerieFilter.SERIES_FOR_MAP_SCREEN)

    // Then
    assertEquals(2, result.size)
    // Should include: Active Serie 1 (active + user is participant) and Upcoming Public Serie
    // (upcoming)
    val resultTitles = result.map { it.title }
    assert(resultTitles.contains("Active Serie 1"))
    assert(resultTitles.contains("Upcoming Public Serie"))
  }
}
