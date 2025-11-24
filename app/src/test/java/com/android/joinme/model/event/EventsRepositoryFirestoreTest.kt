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
import org.bouncycastle.util.test.SimpleTest.runTest
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

  @Test
  fun getAllEvents_returnsMapEventsFiltered() = runTest {
    // Given
    mockkStatic(FirebaseAuth::class)
    every { FirebaseAuth.getInstance() } returns mockAuth
    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns testUserId

    val mockParticipantQuerySnapshot = mockk<QuerySnapshot>(relaxed = true)
    val mockPublicQuerySnapshot = mockk<QuerySnapshot>(relaxed = true)
    val mockSnapshot1 = mockk<com.google.firebase.firestore.QueryDocumentSnapshot>(relaxed = true)
    val mockSnapshot2 = mockk<com.google.firebase.firestore.QueryDocumentSnapshot>(relaxed = true)
    val mockSnapshot3 = mockk<com.google.firebase.firestore.QueryDocumentSnapshot>(relaxed = true)
    val mockSnapshot4 = mockk<com.google.firebase.firestore.QueryDocumentSnapshot>(relaxed = true)
    val mockParticipantQuery = mockk<com.google.firebase.firestore.Query>(relaxed = true)
    val mockPublicQuery = mockk<com.google.firebase.firestore.Query>(relaxed = true)

    // Mock participant query
    every { mockCollection.whereArrayContains("participants", testUserId) } returns
        mockParticipantQuery
    every { mockParticipantQuery.get() } returns Tasks.forResult(mockParticipantQuerySnapshot)
    every { mockParticipantQuerySnapshot.iterator() } returns
        mutableListOf(mockSnapshot1, mockSnapshot2).iterator()

    // Mock public query
    every { mockCollection.whereEqualTo("visibility", EventVisibility.PUBLIC.name) } returns
        mockPublicQuery
    every { mockPublicQuery.get() } returns Tasks.forResult(mockPublicQuerySnapshot)
    every { mockPublicQuerySnapshot.iterator() } returns
        mutableListOf(mockSnapshot3, mockSnapshot4).iterator()

    // Setup event 1 - upcoming event with location (participant)
    val futureDate = Timestamp(Date(System.currentTimeMillis() + 86400000)) // Tomorrow
    every { mockSnapshot1.id } returns "event1"
    every { mockSnapshot1.getString("type") } returns EventType.SOCIAL.name
    every { mockSnapshot1.getString("visibility") } returns EventVisibility.PRIVATE.name
    every { mockSnapshot1.getString("title") } returns "Upcoming Participant Event"
    every { mockSnapshot1.getString("description") } returns "Description 1"
    every { mockSnapshot1.getTimestamp("date") } returns futureDate
    every { mockSnapshot1.getLong("duration") } returns 60L
    every { mockSnapshot1.get("participants") } returns listOf(testUserId)
    every { mockSnapshot1.getLong("maxParticipants") } returns 10L
    every { mockSnapshot1.getString("ownerId") } returns "otherUser"
    every { mockSnapshot1.get("location") } returns
        mapOf("latitude" to 46.52, "longitude" to 6.63, "name" to "Location 1")

    // Setup event 2 - active event with location where user is participant
    val activeDate = Timestamp(Date(System.currentTimeMillis() - 1800000)) // 30 minutes ago
    every { mockSnapshot2.id } returns "event2"
    every { mockSnapshot2.getString("type") } returns EventType.SPORTS.name
    every { mockSnapshot2.getString("visibility") } returns EventVisibility.PUBLIC.name
    every { mockSnapshot2.getString("title") } returns "Active Participant Event"
    every { mockSnapshot2.getString("description") } returns "Description 2"
    every { mockSnapshot2.getTimestamp("date") } returns activeDate
    every { mockSnapshot2.getLong("duration") } returns 120L // 2 hours, so still active
    every { mockSnapshot2.get("participants") } returns listOf(testUserId, "otherUser")
    every { mockSnapshot2.getLong("maxParticipants") } returns 15L
    every { mockSnapshot2.getString("ownerId") } returns "otherUser"
    every { mockSnapshot2.get("location") } returns
        mapOf("latitude" to 46.53, "longitude" to 6.64, "name" to "Location 2")

    // Setup event 3 - upcoming public event with location (not participant)
    every { mockSnapshot3.id } returns "event3"
    every { mockSnapshot3.getString("type") } returns EventType.ACTIVITY.name
    every { mockSnapshot3.getString("visibility") } returns EventVisibility.PUBLIC.name
    every { mockSnapshot3.getString("title") } returns "Upcoming Public Event"
    every { mockSnapshot3.getString("description") } returns "Description 3"
    every { mockSnapshot3.getTimestamp("date") } returns futureDate
    every { mockSnapshot3.getLong("duration") } returns 90L
    every { mockSnapshot3.get("participants") } returns listOf("otherUser1", "otherUser2")
    every { mockSnapshot3.getLong("maxParticipants") } returns 20L
    every { mockSnapshot3.getString("ownerId") } returns "otherUser1"
    every { mockSnapshot3.get("location") } returns
        mapOf("latitude" to 46.54, "longitude" to 6.65, "name" to "Location 3")

    // Setup event 4 - expired public event with location (should be filtered out)
    val expiredDate = Timestamp(Date(System.currentTimeMillis() - 86400000 * 2)) // 2 days ago
    every { mockSnapshot4.id } returns "event4"
    every { mockSnapshot4.getString("type") } returns EventType.SOCIAL.name
    every { mockSnapshot4.getString("visibility") } returns EventVisibility.PUBLIC.name
    every { mockSnapshot4.getString("title") } returns "Expired Public Event"
    every { mockSnapshot4.getString("description") } returns "Description 4"
    every { mockSnapshot4.getTimestamp("date") } returns expiredDate
    every { mockSnapshot4.getLong("duration") } returns 60L
    every { mockSnapshot4.get("participants") } returns listOf("otherUser")
    every { mockSnapshot4.getLong("maxParticipants") } returns 10L
    every { mockSnapshot4.getString("ownerId") } returns "otherUser"
    every { mockSnapshot4.get("location") } returns
        mapOf("latitude" to 46.55, "longitude" to 6.66, "name" to "Location 4")

    // When
    val result = repository.getAllEvents(EventFilter.EVENTS_FOR_MAP_SCREEN)

    // Then - should include upcoming and active participant events, exclude expired
    assertEquals(3, result.size)
    assertEquals("Upcoming Participant Event", result[0].title)
    assertEquals("Active Participant Event", result[1].title)
    assertEquals("Upcoming Public Event", result[2].title)
  }

  @Test
  fun getAllEvents_mapScreenFiltersEventsWithoutLocation() = runTest {
    // Given
    mockkStatic(FirebaseAuth::class)
    every { FirebaseAuth.getInstance() } returns mockAuth
    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns testUserId

    val mockParticipantQuerySnapshot = mockk<QuerySnapshot>(relaxed = true)
    val mockPublicQuerySnapshot = mockk<QuerySnapshot>(relaxed = true)
    val mockSnapshot1 = mockk<com.google.firebase.firestore.QueryDocumentSnapshot>(relaxed = true)
    val mockParticipantQuery = mockk<com.google.firebase.firestore.Query>(relaxed = true)
    val mockPublicQuery = mockk<com.google.firebase.firestore.Query>(relaxed = true)

    // Mock queries
    every { mockCollection.whereArrayContains("participants", testUserId) } returns
        mockParticipantQuery
    every { mockParticipantQuery.get() } returns Tasks.forResult(mockParticipantQuerySnapshot)
    every { mockParticipantQuerySnapshot.iterator() } returns
        mutableListOf(mockSnapshot1).iterator()

    every { mockCollection.whereEqualTo("visibility", EventVisibility.PUBLIC.name) } returns
        mockPublicQuery
    every { mockPublicQuery.get() } returns Tasks.forResult(mockPublicQuerySnapshot)
    every { mockPublicQuerySnapshot.iterator() } returns
        mutableListOf<com.google.firebase.firestore.QueryDocumentSnapshot>().iterator()

    // Setup event without location
    val futureDate = Timestamp(Date(System.currentTimeMillis() + 86400000)) // Tomorrow
    every { mockSnapshot1.id } returns "event1"
    every { mockSnapshot1.getString("type") } returns EventType.SOCIAL.name
    every { mockSnapshot1.getString("visibility") } returns EventVisibility.PUBLIC.name
    every { mockSnapshot1.getString("title") } returns "Event Without Location"
    every { mockSnapshot1.getString("description") } returns "Description"
    every { mockSnapshot1.getTimestamp("date") } returns futureDate
    every { mockSnapshot1.getLong("duration") } returns 60L
    every { mockSnapshot1.get("participants") } returns listOf(testUserId)
    every { mockSnapshot1.getLong("maxParticipants") } returns 10L
    every { mockSnapshot1.getString("ownerId") } returns testUserId
    every { mockSnapshot1.get("location") } returns null // No location

    // When
    val result = repository.getAllEvents(EventFilter.EVENTS_FOR_MAP_SCREEN)

    // Then - event should be filtered out because it has no location
    assertEquals(0, result.size)
  }

  @Test
  fun getAllEvents_mapScreenRemovesDuplicateEvents() = runTest {
    // Given
    mockkStatic(FirebaseAuth::class)
    every { FirebaseAuth.getInstance() } returns mockAuth
    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns testUserId

    val mockParticipantQuerySnapshot = mockk<QuerySnapshot>(relaxed = true)
    val mockPublicQuerySnapshot = mockk<QuerySnapshot>(relaxed = true)
    val mockSnapshot1 = mockk<com.google.firebase.firestore.QueryDocumentSnapshot>(relaxed = true)
    val mockSnapshot2 = mockk<com.google.firebase.firestore.QueryDocumentSnapshot>(relaxed = true)
    val mockParticipantQuery = mockk<com.google.firebase.firestore.Query>(relaxed = true)
    val mockPublicQuery = mockk<com.google.firebase.firestore.Query>(relaxed = true)

    // Mock queries
    every { mockCollection.whereArrayContains("participants", testUserId) } returns
        mockParticipantQuery
    every { mockParticipantQuery.get() } returns Tasks.forResult(mockParticipantQuerySnapshot)
    every { mockParticipantQuerySnapshot.iterator() } returns
        mutableListOf(mockSnapshot1).iterator()

    every { mockCollection.whereEqualTo("visibility", EventVisibility.PUBLIC.name) } returns
        mockPublicQuery
    every { mockPublicQuery.get() } returns Tasks.forResult(mockPublicQuerySnapshot)
    every { mockPublicQuerySnapshot.iterator() } returns mutableListOf(mockSnapshot2).iterator()

    val futureDate = Timestamp(Date(System.currentTimeMillis() + 86400000)) // Tomorrow

    // Setup same event appearing in both queries (public event where user is participant)
    every { mockSnapshot1.id } returns "duplicateEvent"
    every { mockSnapshot1.getString("type") } returns EventType.SOCIAL.name
    every { mockSnapshot1.getString("visibility") } returns EventVisibility.PUBLIC.name
    every { mockSnapshot1.getString("title") } returns "Duplicate Event"
    every { mockSnapshot1.getString("description") } returns "Description"
    every { mockSnapshot1.getTimestamp("date") } returns futureDate
    every { mockSnapshot1.getLong("duration") } returns 60L
    every { mockSnapshot1.get("participants") } returns listOf(testUserId)
    every { mockSnapshot1.getLong("maxParticipants") } returns 10L
    every { mockSnapshot1.getString("ownerId") } returns testUserId
    every { mockSnapshot1.get("location") } returns
        mapOf("latitude" to 46.52, "longitude" to 6.63, "name" to "Location")

    // Same event from public query
    every { mockSnapshot2.id } returns "duplicateEvent"
    every { mockSnapshot2.getString("type") } returns EventType.SOCIAL.name
    every { mockSnapshot2.getString("visibility") } returns EventVisibility.PUBLIC.name
    every { mockSnapshot2.getString("title") } returns "Duplicate Event"
    every { mockSnapshot2.getString("description") } returns "Description"
    every { mockSnapshot2.getTimestamp("date") } returns futureDate
    every { mockSnapshot2.getLong("duration") } returns 60L
    every { mockSnapshot2.get("participants") } returns listOf(testUserId)
    every { mockSnapshot2.getLong("maxParticipants") } returns 10L
    every { mockSnapshot2.getString("ownerId") } returns testUserId
    every { mockSnapshot2.get("location") } returns
        mapOf("latitude" to 46.52, "longitude" to 6.63, "name" to "Location")

    // When
    val result = repository.getAllEvents(EventFilter.EVENTS_FOR_MAP_SCREEN)

    // Then - should only return one event despite appearing in both queries
    assertEquals(1, result.size)
    assertEquals("Duplicate Event", result[0].title)
  }

  @Test
  fun getAllEvents_mapScreenFiltersActiveEventsWhereUserIsNotParticipant() = runTest {
    // Given
    mockkStatic(FirebaseAuth::class)
    every { FirebaseAuth.getInstance() } returns mockAuth
    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns testUserId

    val mockParticipantQuerySnapshot = mockk<QuerySnapshot>(relaxed = true)
    val mockPublicQuerySnapshot = mockk<QuerySnapshot>(relaxed = true)
    val mockSnapshot1 = mockk<com.google.firebase.firestore.QueryDocumentSnapshot>(relaxed = true)
    val mockParticipantQuery = mockk<com.google.firebase.firestore.Query>(relaxed = true)
    val mockPublicQuery = mockk<com.google.firebase.firestore.Query>(relaxed = true)

    // Mock queries
    every { mockCollection.whereArrayContains("participants", testUserId) } returns
        mockParticipantQuery
    every { mockParticipantQuery.get() } returns Tasks.forResult(mockParticipantQuerySnapshot)
    every { mockParticipantQuerySnapshot.iterator() } returns
        mutableListOf<com.google.firebase.firestore.QueryDocumentSnapshot>().iterator()

    every { mockCollection.whereEqualTo("visibility", EventVisibility.PUBLIC.name) } returns
        mockPublicQuery
    every { mockPublicQuery.get() } returns Tasks.forResult(mockPublicQuerySnapshot)
    every { mockPublicQuerySnapshot.iterator() } returns mutableListOf(mockSnapshot1).iterator()

    // Setup active public event where user is NOT a participant
    val activeDate = Timestamp(Date(System.currentTimeMillis() - 1800000))
    every { mockSnapshot1.id } returns "event1"
    every { mockSnapshot1.getString("type") } returns EventType.SPORTS.name
    every { mockSnapshot1.getString("visibility") } returns EventVisibility.PUBLIC.name
    every { mockSnapshot1.getString("title") } returns "Active Public Event"
    every { mockSnapshot1.getString("description") } returns "Description"
    every { mockSnapshot1.getTimestamp("date") } returns activeDate
    every { mockSnapshot1.getLong("duration") } returns 120L
    every { mockSnapshot1.get("participants") } returns listOf("otherUser1", "otherUser2")
    every { mockSnapshot1.getLong("maxParticipants") } returns 15L
    every { mockSnapshot1.getString("ownerId") } returns "otherUser1"
    every { mockSnapshot1.get("location") } returns
        mapOf("latitude" to 46.52, "longitude" to 6.63, "name" to "Location")

    // When
    val result = repository.getAllEvents(EventFilter.EVENTS_FOR_MAP_SCREEN)

    assertEquals(0, result.size)
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

  @Test(expected = Exception::class)
  fun documentToEvent_returnsNullWhenDateIsMissing() = runTest {
    // Given
    val mockSnapshot = mockk<DocumentSnapshot>(relaxed = true)
    every { mockDocument.get() } returns Tasks.forResult(mockSnapshot)
    every { mockSnapshot.id } returns testEventId
    every { mockSnapshot.getString("type") } returns EventType.SOCIAL.name
    every { mockSnapshot.getString("visibility") } returns EventVisibility.PUBLIC.name
    every { mockSnapshot.getString("title") } returns "Test Event"
    every { mockSnapshot.getString("description") } returns "Description"
    every { mockSnapshot.getTimestamp("date") } returns null // Missing required field
    every { mockSnapshot.getLong("duration") } returns 60L
    every { mockSnapshot.get("participants") } returns listOf(testUserId)
    every { mockSnapshot.getLong("maxParticipants") } returns 5L
    every { mockSnapshot.getString("ownerId") } returns testUserId
    every { mockSnapshot.get("location") } returns null

    // When - documentToEvent returns null because date is missing
    // getEvent throws exception because documentToEvent returned null
    repository.getEvent(testEventId)

    // Then - exception is thrown
  }

  @Test(expected = Exception::class)
  fun documentToEvent_returnsNullWhenOwnerIdIsMissing() = runTest {
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
    every { mockSnapshot.getString("ownerId") } returns null // Missing required field
    every { mockSnapshot.get("location") } returns null

    // When - documentToEvent returns null because ownerId is missing
    // getEvent throws exception because documentToEvent returned null
    repository.getEvent(testEventId)

    // Then - exception is thrown
  }

  @Test
  fun getEventsByIds_returnsMatchingEvents() = runTest {
    // Given
    val mockQuerySnapshot = mockk<QuerySnapshot>(relaxed = true)
    val mockSnapshot1 = mockk<com.google.firebase.firestore.QueryDocumentSnapshot>(relaxed = true)
    val mockSnapshot2 = mockk<com.google.firebase.firestore.QueryDocumentSnapshot>(relaxed = true)
    val mockQuery = mockk<com.google.firebase.firestore.Query>(relaxed = true)

    val eventIds = listOf("event1", "event2")

    // Mock whereIn query
    every { mockCollection.whereIn("eventId", eventIds) } returns mockQuery
    every { mockQuery.get() } returns Tasks.forResult(mockQuerySnapshot)
    every { mockQuerySnapshot.iterator() } returns
        mutableListOf(mockSnapshot1, mockSnapshot2).iterator()

    // Setup first event
    every { mockSnapshot1.id } returns "event1"
    every { mockSnapshot1.getString("type") } returns EventType.SOCIAL.name
    every { mockSnapshot1.getString("visibility") } returns EventVisibility.PUBLIC.name
    every { mockSnapshot1.getString("title") } returns "Event 1"
    every { mockSnapshot1.getString("description") } returns "Description 1"
    every { mockSnapshot1.getTimestamp("date") } returns Timestamp(Date())
    every { mockSnapshot1.getLong("duration") } returns 60L
    every { mockSnapshot1.get("participants") } returns listOf(testUserId)
    every { mockSnapshot1.getLong("maxParticipants") } returns 10L
    every { mockSnapshot1.getString("ownerId") } returns testUserId
    every { mockSnapshot1.get("location") } returns null

    // Setup second event
    every { mockSnapshot2.id } returns "event2"
    every { mockSnapshot2.getString("type") } returns EventType.SPORTS.name
    every { mockSnapshot2.getString("visibility") } returns EventVisibility.PRIVATE.name
    every { mockSnapshot2.getString("title") } returns "Event 2"
    every { mockSnapshot2.getString("description") } returns "Description 2"
    every { mockSnapshot2.getTimestamp("date") } returns Timestamp(Date())
    every { mockSnapshot2.getLong("duration") } returns 90L
    every { mockSnapshot2.get("participants") } returns listOf(testUserId)
    every { mockSnapshot2.getLong("maxParticipants") } returns 8L
    every { mockSnapshot2.getString("ownerId") } returns testUserId
    every { mockSnapshot2.get("location") } returns null

    // When
    val result = repository.getEventsByIds(eventIds)

    // Then
    assertEquals(2, result.size)
    assertEquals("Event 1", result[0].title)
    assertEquals("Event 2", result[1].title)
  }

  @Test
  fun getEventsByIds_returnsEmptyListWhenEmptyInput() = runTest {
    // When
    val result = repository.getEventsByIds(emptyList())

    // Then
    assertEquals(0, result.size)
  }

  @Test(expected = Exception::class)
  fun getEventsByIds_throwsExceptionWhenTooManyIds() = runTest {
    // Given
    val tooManyIds = (1..31).map { "event$it" }

    // When
    repository.getEventsByIds(tooManyIds)

    // Then - exception is thrown
  }

  @Test
  fun getEventsByIds_filtersOutInvalidEvents() = runTest {
    // Given
    val mockQuerySnapshot = mockk<QuerySnapshot>(relaxed = true)
    val mockSnapshot1 = mockk<com.google.firebase.firestore.QueryDocumentSnapshot>(relaxed = true)
    val mockSnapshot2 = mockk<com.google.firebase.firestore.QueryDocumentSnapshot>(relaxed = true)
    val mockQuery = mockk<com.google.firebase.firestore.Query>(relaxed = true)

    val eventIds = listOf("event1", "event2")

    // Mock whereIn query
    every { mockCollection.whereIn("eventId", eventIds) } returns mockQuery
    every { mockQuery.get() } returns Tasks.forResult(mockQuerySnapshot)
    every { mockQuerySnapshot.iterator() } returns
        mutableListOf(mockSnapshot1, mockSnapshot2).iterator()

    // Setup first event - valid
    every { mockSnapshot1.id } returns "event1"
    every { mockSnapshot1.getString("type") } returns EventType.SOCIAL.name
    every { mockSnapshot1.getString("visibility") } returns EventVisibility.PUBLIC.name
    every { mockSnapshot1.getString("title") } returns "Event 1"
    every { mockSnapshot1.getString("description") } returns "Description 1"
    every { mockSnapshot1.getTimestamp("date") } returns Timestamp(Date())
    every { mockSnapshot1.getLong("duration") } returns 60L
    every { mockSnapshot1.get("participants") } returns listOf(testUserId)
    every { mockSnapshot1.getLong("maxParticipants") } returns 10L
    every { mockSnapshot1.getString("ownerId") } returns testUserId
    every { mockSnapshot1.get("location") } returns null

    // Setup second event - invalid (missing title)
    every { mockSnapshot2.id } returns "event2"
    every { mockSnapshot2.getString("type") } returns EventType.SPORTS.name
    every { mockSnapshot2.getString("visibility") } returns EventVisibility.PRIVATE.name
    every { mockSnapshot2.getString("title") } returns null // Missing required field
    every { mockSnapshot2.getString("description") } returns "Description 2"
    every { mockSnapshot2.getTimestamp("date") } returns Timestamp(Date())
    every { mockSnapshot2.getLong("duration") } returns 90L
    every { mockSnapshot2.get("participants") } returns listOf(testUserId)
    every { mockSnapshot2.getLong("maxParticipants") } returns 8L
    every { mockSnapshot2.getString("ownerId") } returns testUserId
    every { mockSnapshot2.get("location") } returns null

    // When
    val result = repository.getEventsByIds(eventIds)

    // Then - only valid event is returned
    assertEquals(1, result.size)
    assertEquals("Event 1", result[0].title)
  }

  // ---------------- GET COMMON EVENTS TESTS ----------------

  @Test
  fun getCommonEvents_returnsEmptyListWhenNoUserIds() = runTest {
    // When
    val result = repository.getCommonEvents(emptyList())

    // Then
    assertEquals(0, result.size)
  }

  @Test
  fun getCommonEvents_returnsSortedEventsWithSingleUser() = runTest {
    // Given
    val userId = "user1"
    val mockQuery = mockk<com.google.firebase.firestore.Query>(relaxed = true)
    val mockQuerySnapshot = mockk<QuerySnapshot>(relaxed = true)
    val mockSnapshot1 = mockk<com.google.firebase.firestore.QueryDocumentSnapshot>(relaxed = true)
    val mockSnapshot2 = mockk<com.google.firebase.firestore.QueryDocumentSnapshot>(relaxed = true)

    every { mockCollection.whereArrayContains("participants", userId) } returns mockQuery
    every { mockQuery.get() } returns Tasks.forResult(mockQuerySnapshot)
    every { mockQuerySnapshot.iterator() } returns mutableListOf(mockSnapshot1, mockSnapshot2).iterator()


    val now = System.currentTimeMillis()
    val laterDate = Date(now + 3600000) // 1 hour later
    val earlierDate = Date(now)

    // First event - later date
    every { mockSnapshot1.id } returns "event1"
    every { mockSnapshot1.getString("type") } returns EventType.SOCIAL.name
    every { mockSnapshot1.getString("visibility") } returns EventVisibility.PUBLIC.name
    every { mockSnapshot1.getString("title") } returns "Later Event"
    every { mockSnapshot1.getString("description") } returns "Desc 1"
    every { mockSnapshot1.getTimestamp("date") } returns Timestamp(laterDate)
    every { mockSnapshot1.getLong("duration") } returns 60L
    every { mockSnapshot1.get("participants") } returns listOf(userId)
    every { mockSnapshot1.getLong("maxParticipants") } returns 10L
    every { mockSnapshot1.getString("ownerId") } returns userId
    every { mockSnapshot1.get("location") } returns null

    // Second event - earlier date
    every { mockSnapshot2.id } returns "event2"
    every { mockSnapshot2.getString("type") } returns EventType.SPORTS.name
    every { mockSnapshot2.getString("visibility") } returns EventVisibility.PUBLIC.name
    every { mockSnapshot2.getString("title") } returns "Earlier Event"
    every { mockSnapshot2.getString("description") } returns "Desc 2"
    every { mockSnapshot2.getTimestamp("date") } returns Timestamp(earlierDate)
    every { mockSnapshot2.getLong("duration") } returns 90L
    every { mockSnapshot2.get("participants") } returns listOf(userId)
    every { mockSnapshot2.getLong("maxParticipants") } returns 8L
    every { mockSnapshot2.getString("ownerId") } returns userId
    every { mockSnapshot2.get("location") } returns null

    // When
    val result = repository.getCommonEvents(listOf(userId))

    // Then - events should be sorted by date
    assertEquals(2, result.size)
    assertEquals("Earlier Event", result[0].title)
    assertEquals("Later Event", result[1].title)
  }

  @Test
  fun getCommonEvents_filtersForMultipleUsers() = runTest {
    // Given
    val user1 = "user1"
    val user2 = "user2"
    val mockQuery = mockk<com.google.firebase.firestore.Query>(relaxed = true)
    val mockQuerySnapshot = mockk<QuerySnapshot>(relaxed = true)
    val mockSnapshot1 = mockk<com.google.firebase.firestore.QueryDocumentSnapshot>(relaxed = true)
    val mockSnapshot2 = mockk<com.google.firebase.firestore.QueryDocumentSnapshot>(relaxed = true)
    val mockSnapshot3 = mockk<com.google.firebase.firestore.QueryDocumentSnapshot>(relaxed = true)

    every { mockCollection.whereArrayContains("participants", user1) } returns mockQuery
    every { mockQuery.get() } returns Tasks.forResult(mockQuerySnapshot)
    every { mockQuerySnapshot.iterator() } returns
        mutableListOf(mockSnapshot1, mockSnapshot2, mockSnapshot3).iterator()

    // Event 1 - has both users
    every { mockSnapshot1.id } returns "event1"
    every { mockSnapshot1.getString("type") } returns EventType.SOCIAL.name
    every { mockSnapshot1.getString("visibility") } returns EventVisibility.PUBLIC.name
    every { mockSnapshot1.getString("title") } returns "Common Event"
    every { mockSnapshot1.getString("description") } returns "Both users"
    every { mockSnapshot1.getTimestamp("date") } returns Timestamp(Date())
    every { mockSnapshot1.getLong("duration") } returns 60L
    every { mockSnapshot1.get("participants") } returns listOf(user1, user2, "user3")
    every { mockSnapshot1.getLong("maxParticipants") } returns 10L
    every { mockSnapshot1.getString("ownerId") } returns user1
    every { mockSnapshot1.get("location") } returns null

    // Event 2 - only user1
    every { mockSnapshot2.id } returns "event2"
    every { mockSnapshot2.getString("type") } returns EventType.SPORTS.name
    every { mockSnapshot2.getString("visibility") } returns EventVisibility.PUBLIC.name
    every { mockSnapshot2.getString("title") } returns "User1 Only"
    every { mockSnapshot2.getString("description") } returns "Only user1"
    every { mockSnapshot2.getTimestamp("date") } returns Timestamp(Date())
    every { mockSnapshot2.getLong("duration") } returns 90L
    every { mockSnapshot2.get("participants") } returns listOf(user1, "user3")
    every { mockSnapshot2.getLong("maxParticipants") } returns 8L
    every { mockSnapshot2.getString("ownerId") } returns user1
    every { mockSnapshot2.get("location") } returns null

    // Event 3 - has both users
    every { mockSnapshot3.id } returns "event3"
    every { mockSnapshot3.getString("type") } returns EventType.ACTIVITY.name
    every { mockSnapshot3.getString("visibility") } returns EventVisibility.PUBLIC.name
    every { mockSnapshot3.getString("title") } returns "Another Common"
    every { mockSnapshot3.getString("description") } returns "Both again"
    every { mockSnapshot3.getTimestamp("date") } returns Timestamp(Date())
    every { mockSnapshot3.getLong("duration") } returns 45L
    every { mockSnapshot3.get("participants") } returns listOf(user1, user2)
    every { mockSnapshot3.getLong("maxParticipants") } returns 5L
    every { mockSnapshot3.getString("ownerId") } returns user2
    every { mockSnapshot3.get("location") } returns null

    // When
    val result = repository.getCommonEvents(listOf(user1, user2))

    // Then - only events with both users
    assertEquals(2, result.size)
    val titles = result.map { it.title }
    assert(titles.contains("Common Event"))
    assert(titles.contains("Another Common"))
    assert(!titles.contains("User1 Only"))
  }

  @Test
  fun getCommonEvents_returnsEmptyWhenNoCommonEvents() = runTest {
    // Given
    val user1 = "user1"
    val user2 = "user2"
    val mockQuery = mockk<com.google.firebase.firestore.Query>(relaxed = true)
    val mockQuerySnapshot = mockk<QuerySnapshot>(relaxed = true)
    val mockSnapshot = mockk<com.google.firebase.firestore.QueryDocumentSnapshot>(relaxed = true)

    every { mockCollection.whereArrayContains("participants", user1) } returns mockQuery
    every { mockQuery.get() } returns Tasks.forResult(mockQuerySnapshot)
    every { mockQuerySnapshot.iterator() } returns mutableListOf(mockSnapshot).iterator()

    // Event only has user1, not user2
    every { mockSnapshot.id } returns "event1"
    every { mockSnapshot.getString("type") } returns EventType.SOCIAL.name
    every { mockSnapshot.getString("visibility") } returns EventVisibility.PUBLIC.name
    every { mockSnapshot.getString("title") } returns "No Common"
    every { mockSnapshot.getString("description") } returns "Only user1"
    every { mockSnapshot.getTimestamp("date") } returns Timestamp(Date())
    every { mockSnapshot.getLong("duration") } returns 60L
    every { mockSnapshot.get("participants") } returns listOf(user1)
    every { mockSnapshot.getLong("maxParticipants") } returns 10L
    every { mockSnapshot.getString("ownerId") } returns user1
    every { mockSnapshot.get("location") } returns null

    // When
    val result = repository.getCommonEvents(listOf(user1, user2))

    // Then
    assertEquals(0, result.size)
  }

  @Test
  fun getCommonEvents_handlesInvalidDocuments() = runTest {
    // Given
    val userId = "user1"
    val mockQuery = mockk<com.google.firebase.firestore.Query>(relaxed = true)
    val mockQuerySnapshot = mockk<QuerySnapshot>(relaxed = true)
    val mockSnapshot1 = mockk<com.google.firebase.firestore.QueryDocumentSnapshot>(relaxed = true)
    val mockSnapshot2 = mockk<com.google.firebase.firestore.QueryDocumentSnapshot>(relaxed = true)

    every { mockCollection.whereArrayContains("participants", userId) } returns mockQuery
    every { mockQuery.get() } returns Tasks.forResult(mockQuerySnapshot)
    every { mockQuerySnapshot.iterator() } returns
        mutableListOf(mockSnapshot1, mockSnapshot2).iterator()

    // Valid event
    every { mockSnapshot1.id } returns "event1"
    every { mockSnapshot1.getString("type") } returns EventType.SOCIAL.name
    every { mockSnapshot1.getString("visibility") } returns EventVisibility.PUBLIC.name
    every { mockSnapshot1.getString("title") } returns "Valid Event"
    every { mockSnapshot1.getString("description") } returns "Good"
    every { mockSnapshot1.getTimestamp("date") } returns Timestamp(Date())
    every { mockSnapshot1.getLong("duration") } returns 60L
    every { mockSnapshot1.get("participants") } returns listOf(userId)
    every { mockSnapshot1.getLong("maxParticipants") } returns 10L
    every { mockSnapshot1.getString("ownerId") } returns userId
    every { mockSnapshot1.get("location") } returns null

    // Invalid event (missing title)
    every { mockSnapshot2.id } returns "event2"
    every { mockSnapshot2.getString("type") } returns EventType.SPORTS.name
    every { mockSnapshot2.getString("title") } returns null
    every { mockSnapshot2.getString("description") } returns "Bad"
    every { mockSnapshot2.getTimestamp("date") } returns Timestamp(Date())
    every { mockSnapshot2.getLong("duration") } returns 90L
    every { mockSnapshot2.get("participants") } returns listOf(userId)
    every { mockSnapshot2.getLong("maxParticipants") } returns 8L
    every { mockSnapshot2.getString("ownerId") } returns userId

    // When
    val result = repository.getCommonEvents(listOf(userId))

    // Then - only valid event is returned
    assertEquals(1, result.size)
    assertEquals("Valid Event", result[0].title)
  }
}
