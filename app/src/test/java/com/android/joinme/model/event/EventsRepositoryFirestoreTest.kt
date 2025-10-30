package com.android.joinme.model.event

import com.android.joinme.model.map.Location
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

class EventsRepositoryFirestoreTest {

  private lateinit var mockDb: FirebaseFirestore
  private lateinit var mockCollection: CollectionReference
  private lateinit var mockDocument: DocumentReference
  private lateinit var mockAuth: FirebaseAuth
  private lateinit var mockUser: FirebaseUser
  private lateinit var repository: EventsRepositoryFirestore

  private val testEventId = "testEvent123"
  private val testUserId = "testUser456"
  private val testEvent =
      Event(
          eventId = testEventId,
          type = EventType.SOCIAL,
          title = "Coffee Meetup",
          description = "Grab a coffee at EPFL Café",
          location = Location(46.52, 6.63, "EPFL Café"),
          date = Timestamp(Date()),
          duration = 60,
          participants = listOf("user1", "user2", testUserId),
          maxParticipants = 5,
          visibility = EventVisibility.PUBLIC,
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

    every { mockDb.collection(EVENTS_COLLECTION_PATH) } returns mockCollection
    every { mockCollection.document(any()) } returns mockDocument
    every { mockCollection.document() } returns mockDocument
    every { mockDocument.id } returns testEventId

    repository = EventsRepositoryFirestore(mockDb)
  }

  @Test
  fun getNewEventId_returnsValidId() {
    // When
    val eventId = repository.getNewEventId()

    // Then
    assertNotNull(eventId)
    assertEquals(testEventId, eventId)
    verify { mockDb.collection(EVENTS_COLLECTION_PATH) }
    verify { mockCollection.document() }
  }

  @Test
  fun addEvent_callsFirestoreSet() = runTest {
    // Given
    every { mockDocument.set(any()) } returns Tasks.forResult(null)

    // When
    repository.addEvent(testEvent)

    // Then
    verify { mockCollection.document(testEventId) }
    verify { mockDocument.set(testEvent) }
  }

  @Test
  fun getEvent_returnsEventSuccessfully() = runTest {
    // Given
    val mockSnapshot = mockk<DocumentSnapshot>(relaxed = true)
    every { mockDocument.get() } returns Tasks.forResult(mockSnapshot)
    every { mockSnapshot.id } returns testEventId
    every { mockSnapshot.getString("type") } returns EventType.SOCIAL.name
    every { mockSnapshot.getString("visibility") } returns EventVisibility.PUBLIC.name
    every { mockSnapshot.getString("title") } returns "Coffee Meetup"
    every { mockSnapshot.getString("description") } returns "Grab a coffee at EPFL Café"
    every { mockSnapshot.getTimestamp("date") } returns testEvent.date
    every { mockSnapshot.getLong("duration") } returns 60L
    every { mockSnapshot.get("participants") } returns listOf("user1", "user2")
    every { mockSnapshot.getLong("maxParticipants") } returns 5L
    every { mockSnapshot.getString("ownerId") } returns testUserId
    every { mockSnapshot.get("location") } returns
        mapOf("latitude" to 46.52, "longitude" to 6.63, "name" to "EPFL Café")

    // When
    val result = repository.getEvent(testEventId)

    // Then
    assertNotNull(result)
    assertEquals(testEventId, result.eventId)
    assertEquals("Coffee Meetup", result.title)
    assertEquals(EventType.SOCIAL, result.type)
    assertEquals(testUserId, result.ownerId)
  }

  @Test
  fun editEvent_callsFirestoreUpdate() = runTest {
    // Given
    val updatedEvent = testEvent.copy(title = "Updated Title")
    every { mockDocument.set(any()) } returns Tasks.forResult(null)

    // When
    repository.editEvent(testEventId, updatedEvent)

    // Then
    verify { mockCollection.document(testEventId) }
    verify { mockDocument.set(updatedEvent) }
  }

  @Test
  fun deleteEvent_callsFirestoreDelete() = runTest {
    // Given
    every { mockDocument.delete() } returns Tasks.forResult(null)

    // When
    repository.deleteEvent(testEventId)

    // Then
    verify { mockCollection.document(testEventId) }
    verify { mockDocument.delete() }
  }

  @Test
  fun getAllEvents_returnsEventsForCurrentUser() = runTest {
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

    // Setup first event - testUserId must be in participants
    every { mockSnapshot1.id } returns "event1"
    every { mockSnapshot1.getString("type") } returns EventType.SOCIAL.name
    every { mockSnapshot1.getString("visibility") } returns EventVisibility.PUBLIC.name
    every { mockSnapshot1.getString("title") } returns "Event 1"
    every { mockSnapshot1.getString("description") } returns "Description 1"
    every { mockSnapshot1.getTimestamp("date") } returns Timestamp(Date())
    every { mockSnapshot1.getLong("duration") } returns 30L
    every { mockSnapshot1.get("participants") } returns listOf(testUserId, "user1")
    every { mockSnapshot1.getLong("maxParticipants") } returns 10L
    every { mockSnapshot1.getString("ownerId") } returns testUserId
    every { mockSnapshot1.get("location") } returns null

    // Setup second event - testUserId must be in participants
    every { mockSnapshot2.id } returns "event2"
    every { mockSnapshot2.getString("type") } returns EventType.SPORTS.name
    every { mockSnapshot2.getString("visibility") } returns EventVisibility.PRIVATE.name
    every { mockSnapshot2.getString("title") } returns "Event 2"
    every { mockSnapshot2.getString("description") } returns "Description 2"
    every { mockSnapshot2.getTimestamp("date") } returns Timestamp(Date())
    every { mockSnapshot2.getLong("duration") } returns 45L
    every { mockSnapshot2.get("participants") } returns listOf(testUserId, "user3")
    every { mockSnapshot2.getLong("maxParticipants") } returns 8L
    every { mockSnapshot2.getString("ownerId") } returns testUserId
    every { mockSnapshot2.get("location") } returns null

    val result = repository.getAllEvents(EventFilter.EVENTS_FOR_OVERVIEW_SCREEN)

    // Then
    assertEquals(2, result.size)
    assertEquals("Event 1", result[0].title)
    assertEquals("Event 2", result[1].title)
  }

  @Test(expected = Exception::class)
  fun getEvent_throwsExceptionWhenNotFound() = runTest {
    // Given
    val mockSnapshot = mockk<DocumentSnapshot>(relaxed = true)
    every { mockDocument.get() } returns Tasks.forResult(mockSnapshot)
    every { mockSnapshot.getString("title") } returns null // Missing required field

    // When
    repository.getEvent(testEventId)

    // Then - exception is thrown
  }

  @Test(expected = Exception::class)
  fun getAllEvents_throwsExceptionWhenUserNotLoggedIn() = runTest {
    // Given
    mockkStatic(FirebaseAuth::class)
    every { FirebaseAuth.getInstance() } returns mockAuth
    every { mockAuth.currentUser } returns null // User not logged in

    // When
    repository.getAllEvents(EventFilter.EVENTS_FOR_OVERVIEW_SCREEN)

    // Then - exception is thrown
  }

  @Test(expected = Exception::class)
  fun documentToEvent_handlesExceptionGracefully() = runTest {
    // Given
    val mockSnapshot = mockk<DocumentSnapshot>(relaxed = true)
    every { mockDocument.get() } returns Tasks.forResult(mockSnapshot)
    every { mockSnapshot.id } returns testEventId
    every { mockSnapshot.getString("type") } returns "INVALID_TYPE" // Invalid enum value
    every { mockSnapshot.getString("visibility") } returns EventVisibility.PUBLIC.name
    every { mockSnapshot.getString("title") } returns "Test Event"
    every { mockSnapshot.getString("description") } returns "Description"
    every { mockSnapshot.getTimestamp("date") } returns Timestamp(Date())
    every { mockSnapshot.getLong("duration") } returns 60L
    every { mockSnapshot.get("participants") } returns emptyList<String>()
    every { mockSnapshot.getLong("maxParticipants") } returns 5L
    every { mockSnapshot.getString("ownerId") } returns testUserId
    every { mockSnapshot.get("location") } returns null

    // When - documentToEvent catches the exception and returns null
    // getEvent then throws because documentToEvent returned null
    repository.getEvent(testEventId)

    // Then - exception is thrown because documentToEvent returned null
  }

  @Test
  fun getAllEvents_returnsHistoryEventsFilteredAndSorted() = runTest {
    // Given
    mockkStatic(FirebaseAuth::class)
    every { FirebaseAuth.getInstance() } returns mockAuth
    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns testUserId

    val mockQuerySnapshot = mockk<QuerySnapshot>(relaxed = true)
    val mockSnapshot1 = mockk<com.google.firebase.firestore.QueryDocumentSnapshot>(relaxed = true)
    val mockSnapshot2 = mockk<com.google.firebase.firestore.QueryDocumentSnapshot>(relaxed = true)
    val mockQuery = mockk<com.google.firebase.firestore.Query>(relaxed = true)

    // Mock whereArrayContains query for history screen
    every { mockCollection.whereArrayContains("participants", testUserId) } returns mockQuery
    every { mockQuery.get() } returns Tasks.forResult(mockQuerySnapshot)
    every { mockQuerySnapshot.iterator() } returns
        mutableListOf(mockSnapshot1, mockSnapshot2).iterator()

    // Setup first event - expired event (old date)
    val oldDate = Timestamp(Date(System.currentTimeMillis() - 86400000 * 2)) // 2 days ago
    every { mockSnapshot1.id } returns "event1"
    every { mockSnapshot1.getString("type") } returns EventType.SOCIAL.name
    every { mockSnapshot1.getString("visibility") } returns EventVisibility.PUBLIC.name
    every { mockSnapshot1.getString("title") } returns "Past Event 1"
    every { mockSnapshot1.getString("description") } returns "Description 1"
    every { mockSnapshot1.getTimestamp("date") } returns oldDate
    every { mockSnapshot1.getLong("duration") } returns 30L
    every { mockSnapshot1.get("participants") } returns listOf(testUserId)
    every { mockSnapshot1.getLong("maxParticipants") } returns 10L
    every { mockSnapshot1.getString("ownerId") } returns testUserId
    every { mockSnapshot1.get("location") } returns null

    // Setup second event - expired event (older date)
    val olderDate = Timestamp(Date(System.currentTimeMillis() - 86400000 * 5)) // 5 days ago
    every { mockSnapshot2.id } returns "event2"
    every { mockSnapshot2.getString("type") } returns EventType.SPORTS.name
    every { mockSnapshot2.getString("visibility") } returns EventVisibility.PRIVATE.name
    every { mockSnapshot2.getString("title") } returns "Past Event 2"
    every { mockSnapshot2.getString("description") } returns "Description 2"
    every { mockSnapshot2.getTimestamp("date") } returns olderDate
    every { mockSnapshot2.getLong("duration") } returns 45L
    every { mockSnapshot2.get("participants") } returns listOf(testUserId)
    every { mockSnapshot2.getLong("maxParticipants") } returns 8L
    every { mockSnapshot2.getString("ownerId") } returns testUserId
    every { mockSnapshot2.get("location") } returns null

    // When
    val result = repository.getAllEvents(EventFilter.EVENTS_FOR_HISTORY_SCREEN)

    // Then - events should be filtered and sorted by date descending
    assertEquals(2, result.size)
    assertEquals("Past Event 1", result[0].title) // More recent expired event first
    assertEquals("Past Event 2", result[1].title)
  }

  @Test
  fun getAllEvents_returnsSearchEventsFiltered() = runTest {
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

    // Mock whereEqualTo query for search screen
    every { mockCollection.whereEqualTo("visibility", EventVisibility.PUBLIC.name) } returns
        mockQuery
    every { mockQuery.get() } returns Tasks.forResult(mockQuerySnapshot)
    every { mockQuerySnapshot.iterator() } returns
        mutableListOf(mockSnapshot1, mockSnapshot2, mockSnapshot3).iterator()

    // Setup first event - upcoming event not owned by user, user not participant
    val futureDate = Timestamp(Date(System.currentTimeMillis() + 86400000)) // Tomorrow
    every { mockSnapshot1.id } returns "event1"
    every { mockSnapshot1.getString("type") } returns EventType.SOCIAL.name
    every { mockSnapshot1.getString("visibility") } returns EventVisibility.PUBLIC.name
    every { mockSnapshot1.getString("title") } returns "Available Event"
    every { mockSnapshot1.getString("description") } returns "Description 1"
    every { mockSnapshot1.getTimestamp("date") } returns futureDate
    every { mockSnapshot1.getLong("duration") } returns 30L
    every { mockSnapshot1.get("participants") } returns listOf("otherUser1")
    every { mockSnapshot1.getLong("maxParticipants") } returns 10L
    every { mockSnapshot1.getString("ownerId") } returns "otherUser1"
    every { mockSnapshot1.get("location") } returns null

    // Setup second event - user is already participant (should be filtered out)
    every { mockSnapshot2.id } returns "event2"
    every { mockSnapshot2.getString("type") } returns EventType.SPORTS.name
    every { mockSnapshot2.getString("visibility") } returns EventVisibility.PUBLIC.name
    every { mockSnapshot2.getString("title") } returns "Event With User"
    every { mockSnapshot2.getString("description") } returns "Description 2"
    every { mockSnapshot2.getTimestamp("date") } returns futureDate
    every { mockSnapshot2.getLong("duration") } returns 45L
    every { mockSnapshot2.get("participants") } returns listOf(testUserId, "otherUser2")
    every { mockSnapshot2.getLong("maxParticipants") } returns 8L
    every { mockSnapshot2.getString("ownerId") } returns "otherUser2"
    every { mockSnapshot2.get("location") } returns null

    // Setup third event - user is owner (should be filtered out)
    every { mockSnapshot3.id } returns "event3"
    every { mockSnapshot3.getString("type") } returns EventType.SOCIAL.name
    every { mockSnapshot3.getString("visibility") } returns EventVisibility.PUBLIC.name
    every { mockSnapshot3.getString("title") } returns "User's Own Event"
    every { mockSnapshot3.getString("description") } returns "Description 3"
    every { mockSnapshot3.getTimestamp("date") } returns futureDate
    every { mockSnapshot3.getLong("duration") } returns 60L
    every { mockSnapshot3.get("participants") } returns listOf(testUserId)
    every { mockSnapshot3.getLong("maxParticipants") } returns 5L
    every { mockSnapshot3.getString("ownerId") } returns testUserId
    every { mockSnapshot3.get("location") } returns null

    // When
    val result = repository.getAllEvents(EventFilter.EVENTS_FOR_SEARCH_SCREEN)

    // Then - only events where user is not participant and not owner should be returned
    assertEquals(1, result.size)
    assertEquals("Available Event", result[0].title)
  }

  @Test
  fun addEvent_addsOwnerToParticipants() = runTest {
    // Given
    val eventWithoutOwnerInParticipants =
        testEvent.copy(participants = listOf("user1", "user2")) // Owner not in list
    every { mockDocument.set(any()) } returns Tasks.forResult(null)

    // When
    repository.addEvent(eventWithoutOwnerInParticipants)

    // Then - verify the event was added with owner in participants
    verify {
      mockDocument.set(
          match { event: Event ->
            event.participants.contains(testUserId) &&
                event.participants.size == 3 &&
                event.participants.containsAll(listOf("user1", "user2", testUserId))
          })
    }
  }

  @Test
  fun documentToEvent_handlesDefaultValues() = runTest {
    // Given
    val mockSnapshot = mockk<DocumentSnapshot>(relaxed = true)
    every { mockDocument.get() } returns Tasks.forResult(mockSnapshot)
    every { mockSnapshot.id } returns testEventId
    every { mockSnapshot.getString("type") } returns null // Should default to SOCIAL
    every { mockSnapshot.getString("visibility") } returns null // Should default to PUBLIC
    every { mockSnapshot.getString("title") } returns "Test Event"
    every { mockSnapshot.getString("description") } returns null // Should default to ""
    every { mockSnapshot.getTimestamp("date") } returns Timestamp(Date())
    every { mockSnapshot.getLong("duration") } returns null // Should default to 0
    every { mockSnapshot.get("participants") } returns null // Should default to emptyList
    every { mockSnapshot.getLong("maxParticipants") } returns null // Should default to 0
    every { mockSnapshot.getString("ownerId") } returns testUserId
    every { mockSnapshot.get("location") } returns null

    // When
    val result = repository.getEvent(testEventId)

    // Then
    assertNotNull(result)
    assertEquals(EventType.SOCIAL, result.type) // Default value
    assertEquals(EventVisibility.PUBLIC, result.visibility) // Default value
    assertEquals("", result.description) // Default value
    assertEquals(0, result.duration) // Default value
    assertEquals(emptyList<String>(), result.participants) // Default value
    assertEquals(0, result.maxParticipants) // Default value
  }

  @Test
  fun documentToEvent_handlesLocationWithDefaults() = runTest {
    // Given
    val mockSnapshot = mockk<DocumentSnapshot>(relaxed = true)
    every { mockDocument.get() } returns Tasks.forResult(mockSnapshot)
    every { mockSnapshot.id } returns testEventId
    every { mockSnapshot.getString("type") } returns EventType.SOCIAL.name
    every { mockSnapshot.getString("visibility") } returns EventVisibility.PUBLIC.name
    every { mockSnapshot.getString("title") } returns "Test Event"
    every { mockSnapshot.getString("description") } returns "Description"
    every { mockSnapshot.getTimestamp("date") } returns Timestamp(Date())
    every { mockSnapshot.getLong("duration") } returns 60L
    every { mockSnapshot.get("participants") } returns listOf(testUserId)
    every { mockSnapshot.getLong("maxParticipants") } returns 5L
    every { mockSnapshot.getString("ownerId") } returns testUserId
    // Location with missing fields
    every { mockSnapshot.get("location") } returns
        mapOf("latitude" to null, "longitude" to null, "name" to null)

    // When
    val result = repository.getEvent(testEventId)

    // Then
    assertNotNull(result)
    assertNotNull(result.location)
    assertEquals(0.0, result.location?.latitude ?: -1.0, 0.001)
    assertEquals(0.0, result.location?.longitude ?: -1.0, 0.001)
    assertEquals("", result.location?.name)
  }
}
