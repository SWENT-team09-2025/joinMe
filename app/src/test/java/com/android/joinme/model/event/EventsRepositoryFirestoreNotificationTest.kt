package com.android.joinme.model.event

import android.content.Context
import com.android.joinme.model.map.Location
import com.android.joinme.model.notification.NotificationScheduler
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import io.mockk.*
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class EventsRepositoryFirestoreNotificationTest {

  private lateinit var mockDb: FirebaseFirestore
  private lateinit var mockCollection: CollectionReference
  private lateinit var mockDocument: DocumentReference
  private lateinit var mockContext: Context
  private lateinit var mockAuth: FirebaseAuth
  private lateinit var mockUser: FirebaseUser
  private lateinit var repository: EventsRepositoryFirestore

  private val testEventId = "testEvent123"
  private val testUserId = "testUser456"

  @Before
  fun setup() {
    // Mock Firestore
    mockDb = mockk(relaxed = true)
    mockCollection = mockk(relaxed = true)
    mockDocument = mockk(relaxed = true)
    mockContext = mockk(relaxed = true)

    every { mockDb.collection(EVENTS_COLLECTION_PATH) } returns mockCollection
    every { mockCollection.document(any()) } returns mockDocument
    every { mockCollection.document() } returns mockDocument
    every { mockDocument.id } returns testEventId
    every { mockDocument.set(any()) } returns Tasks.forResult(null)
    every { mockDocument.delete() } returns Tasks.forResult(null)

    // Mock Firebase Auth
    mockAuth = mockk(relaxed = true)
    mockUser = mockk(relaxed = true)
    mockkStatic(FirebaseAuth::class)
    mockkStatic("com.google.firebase.auth.AuthKt")
    every { Firebase.auth } returns mockAuth
    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns testUserId

    // Mock NotificationScheduler
    mockkObject(NotificationScheduler)
    every { NotificationScheduler.scheduleEventNotification(any(), any()) } just Runs
    every { NotificationScheduler.cancelEventNotification(any(), any()) } just Runs

    repository = EventsRepositoryFirestore(mockDb, mockContext)
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun `addEvent schedules notification for upcoming event`() = runTest {
    // Given - event 1 hour in the future
    val futureTime = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1)
    val upcomingEvent =
        Event(
            eventId = testEventId,
            type = EventType.SOCIAL,
            title = "Future Event",
            description = "Description",
            location = Location(46.52, 6.63, "Location"),
            date = Timestamp(Date(futureTime)),
            duration = 60,
            participants = emptyList(),
            maxParticipants = 10,
            visibility = EventVisibility.PUBLIC,
            ownerId = testUserId)

    // Expected event with ownerId added to participants
    val expectedEvent = upcomingEvent.copy(participants = listOf(testUserId))

    // When
    repository.addEvent(upcomingEvent)

    // Then
    verify { mockDocument.set(expectedEvent) }
    verify { NotificationScheduler.scheduleEventNotification(mockContext, expectedEvent) }
  }

  @Test
  fun `addEvent does not schedule notification for past event`() = runTest {
    // Given - event 1 hour in the past
    val pastTime = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1)
    val pastEvent =
        Event(
            eventId = testEventId,
            type = EventType.SOCIAL,
            title = "Past Event",
            description = "Description",
            location = Location(46.52, 6.63, "Location"),
            date = Timestamp(Date(pastTime)),
            duration = 60,
            participants = emptyList(),
            maxParticipants = 10,
            visibility = EventVisibility.PUBLIC,
            ownerId = testUserId)

    // Expected event with ownerId added to participants
    val expectedEvent = pastEvent.copy(participants = listOf(testUserId))

    // When
    repository.addEvent(pastEvent)

    // Then
    verify { mockDocument.set(expectedEvent) }
    verify(exactly = 0) { NotificationScheduler.scheduleEventNotification(any(), any()) }
  }

  @Test
  fun `addEvent does not schedule notification for active event`() = runTest {
    // Given - event that started 30 minutes ago and lasts 60 minutes
    val activeEventTime = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(30)
    val activeEvent =
        Event(
            eventId = testEventId,
            type = EventType.SOCIAL,
            title = "Active Event",
            description = "Description",
            location = Location(46.52, 6.63, "Location"),
            date = Timestamp(Date(activeEventTime)),
            duration = 60,
            participants = emptyList(),
            maxParticipants = 10,
            visibility = EventVisibility.PUBLIC,
            ownerId = testUserId)

    // Expected event with ownerId added to participants
    val expectedEvent = activeEvent.copy(participants = listOf(testUserId))

    // When
    repository.addEvent(activeEvent)

    // Then
    verify { mockDocument.set(expectedEvent) }
    verify(exactly = 0) { NotificationScheduler.scheduleEventNotification(any(), any()) }
  }

  @Test
  fun `addEvent does not schedule notification when context is null`() = runTest {
    // Given
    val repositoryWithoutContext = EventsRepositoryFirestore(mockDb, null)
    val futureTime = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1)
    val upcomingEvent =
        Event(
            eventId = testEventId,
            type = EventType.SOCIAL,
            title = "Future Event",
            description = "Description",
            location = Location(46.52, 6.63, "Location"),
            date = Timestamp(Date(futureTime)),
            duration = 60,
            participants = emptyList(),
            maxParticipants = 10,
            visibility = EventVisibility.PUBLIC,
            ownerId = testUserId)

    // Expected event with ownerId added to participants
    val expectedEvent = upcomingEvent.copy(participants = listOf(testUserId))

    // When
    repositoryWithoutContext.addEvent(upcomingEvent)

    // Then
    verify { mockDocument.set(expectedEvent) }
    verify(exactly = 0) { NotificationScheduler.scheduleEventNotification(any(), any()) }
  }

  @Test
  fun `editEvent cancels old notification and schedules new one for upcoming event when user is participant`() =
      runTest {
        // Given - current user is in participants list
        val futureTime = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(2)
        val updatedEvent =
            Event(
                eventId = testEventId,
                type = EventType.SOCIAL,
                title = "Updated Event",
                description = "Updated Description",
                location = Location(46.52, 6.63, "Location"),
                date = Timestamp(Date(futureTime)),
                duration = 90,
                participants = listOf(testUserId),
                maxParticipants = 15,
                visibility = EventVisibility.PUBLIC,
                ownerId = testUserId)

        // When
        repository.editEvent(testEventId, updatedEvent)

        // Then
        verify { mockDocument.set(updatedEvent) }
        verify { NotificationScheduler.cancelEventNotification(mockContext, testEventId) }
        verify { NotificationScheduler.scheduleEventNotification(mockContext, updatedEvent) }
      }

  @Test
  fun `editEvent cancels notification but does not reschedule when user is not participant`() =
      runTest {
        // Given - current user is NOT in participants list
        val futureTime = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(2)
        val updatedEvent =
            Event(
                eventId = testEventId,
                type = EventType.SOCIAL,
                title = "Updated Event",
                description = "Updated Description",
                location = Location(46.52, 6.63, "Location"),
                date = Timestamp(Date(futureTime)),
                duration = 90,
                participants = listOf("otherUser123"),
                maxParticipants = 15,
                visibility = EventVisibility.PUBLIC,
                ownerId = "otherUser123")

        // When
        repository.editEvent(testEventId, updatedEvent)

        // Then
        verify { mockDocument.set(updatedEvent) }
        verify { NotificationScheduler.cancelEventNotification(mockContext, testEventId) }
        verify(exactly = 0) { NotificationScheduler.scheduleEventNotification(any(), any()) }
      }

  @Test
  fun `editEvent cancels notification but does not reschedule for past event`() = runTest {
    // Given
    val pastTime = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1)
    val pastEvent =
        Event(
            eventId = testEventId,
            type = EventType.SOCIAL,
            title = "Past Event",
            description = "Description",
            location = Location(46.52, 6.63, "Location"),
            date = Timestamp(Date(pastTime)),
            duration = 60,
            participants = emptyList(),
            maxParticipants = 10,
            visibility = EventVisibility.PUBLIC,
            ownerId = testUserId)

    // When
    repository.editEvent(testEventId, pastEvent)

    // Then
    verify { mockDocument.set(pastEvent) }
    verify { NotificationScheduler.cancelEventNotification(mockContext, testEventId) }
    verify(exactly = 0) { NotificationScheduler.scheduleEventNotification(any(), any()) }
  }

  @Test
  fun `editEvent does not modify notifications when context is null`() = runTest {
    // Given
    val repositoryWithoutContext = EventsRepositoryFirestore(mockDb, null)
    val futureTime = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(2)
    val updatedEvent =
        Event(
            eventId = testEventId,
            type = EventType.SOCIAL,
            title = "Updated Event",
            description = "Description",
            location = Location(46.52, 6.63, "Location"),
            date = Timestamp(Date(futureTime)),
            duration = 60,
            participants = emptyList(),
            maxParticipants = 10,
            visibility = EventVisibility.PUBLIC,
            ownerId = testUserId)

    // When
    repositoryWithoutContext.editEvent(testEventId, updatedEvent)

    // Then
    verify { mockDocument.set(updatedEvent) }
    verify(exactly = 0) { NotificationScheduler.cancelEventNotification(any(), any()) }
    verify(exactly = 0) { NotificationScheduler.scheduleEventNotification(any(), any()) }
  }

  @Test
  fun `deleteEvent cancels notification`() = runTest {
    // When
    repository.deleteEvent(testEventId)

    // Then
    verify { mockDocument.delete() }
    verify { NotificationScheduler.cancelEventNotification(mockContext, testEventId) }
  }

  @Test
  fun `deleteEvent does not cancel notification when context is null`() = runTest {
    // Given
    val repositoryWithoutContext = EventsRepositoryFirestore(mockDb, null)

    // When
    repositoryWithoutContext.deleteEvent(testEventId)

    // Then
    verify { mockDocument.delete() }
    verify(exactly = 0) { NotificationScheduler.cancelEventNotification(any(), any()) }
  }

  @Test
  fun `editEvent handles event date change correctly`() = runTest {
    // Given - change event from 1 hour in future to 3 hours in future, current user is participant
    val newTime = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(3)
    val rescheduledEvent =
        Event(
            eventId = testEventId,
            type = EventType.SOCIAL,
            title = "Rescheduled Event",
            description = "Description",
            location = Location(46.52, 6.63, "Location"),
            date = Timestamp(Date(newTime)),
            duration = 60,
            participants = listOf(testUserId),
            maxParticipants = 10,
            visibility = EventVisibility.PUBLIC,
            ownerId = testUserId)

    // When
    repository.editEvent(testEventId, rescheduledEvent)

    // Then - old notification cancelled, new one scheduled
    verify(exactly = 1) { NotificationScheduler.cancelEventNotification(mockContext, testEventId) }
    verify(exactly = 1) {
      NotificationScheduler.scheduleEventNotification(mockContext, rescheduledEvent)
    }
  }

  @Test
  fun `editEvent handles event becoming past event`() = runTest {
    // Given - update event to be in the past
    val pastTime = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(30)
    val nowPastEvent =
        Event(
            eventId = testEventId,
            type = EventType.SOCIAL,
            title = "Now Past Event",
            description = "Description",
            location = Location(46.52, 6.63, "Location"),
            date = Timestamp(Date(pastTime)),
            duration = 60,
            participants = emptyList(),
            maxParticipants = 10,
            visibility = EventVisibility.PUBLIC,
            ownerId = testUserId)

    // When
    repository.editEvent(testEventId, nowPastEvent)

    // Then - notification cancelled but not rescheduled
    verify { NotificationScheduler.cancelEventNotification(mockContext, testEventId) }
    verify(exactly = 0) { NotificationScheduler.scheduleEventNotification(any(), any()) }
  }

  @Test
  fun `multiple addEvent calls schedule notifications independently`() = runTest {
    // Given
    val futureTime1 = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1)
    val futureTime2 = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(2)

    val event1 =
        Event(
            eventId = "event1",
            type = EventType.SOCIAL,
            title = "Event 1",
            description = "Description",
            location = Location(46.52, 6.63, "Location"),
            date = Timestamp(Date(futureTime1)),
            duration = 60,
            participants = emptyList(),
            maxParticipants = 10,
            visibility = EventVisibility.PUBLIC,
            ownerId = testUserId)

    val event2 =
        Event(
            eventId = "event2",
            type = EventType.SPORTS,
            title = "Event 2",
            description = "Description",
            location = Location(46.52, 6.63, "Location"),
            date = Timestamp(Date(futureTime2)),
            duration = 90,
            participants = emptyList(),
            maxParticipants = 20,
            visibility = EventVisibility.PUBLIC,
            ownerId = testUserId)

    // Expected events with ownerId added to participants
    val expectedEvent1 = event1.copy(participants = listOf(testUserId))
    val expectedEvent2 = event2.copy(participants = listOf(testUserId))

    // When
    repository.addEvent(event1)
    repository.addEvent(event2)

    // Then
    verify(exactly = 1) {
      NotificationScheduler.scheduleEventNotification(mockContext, expectedEvent1)
    }
    verify(exactly = 1) {
      NotificationScheduler.scheduleEventNotification(mockContext, expectedEvent2)
    }
  }
}
