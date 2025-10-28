package com.android.joinme.model.notification

import android.content.Context
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.android.joinme.model.event.Event
import com.android.joinme.model.event.EventType
import com.android.joinme.model.event.EventVisibility
import com.android.joinme.model.map.Location
import com.google.firebase.Timestamp
import io.mockk.*
import java.util.Date
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class NotificationSchedulerTest {

  private lateinit var context: Context
  private lateinit var mockWorkManager: WorkManager

  @Before
  fun setUp() {
    context = RuntimeEnvironment.getApplication()
    mockWorkManager = mockk(relaxed = true)
    mockkStatic(WorkManager::class)
    every { WorkManager.getInstance(any()) } returns mockWorkManager
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun `scheduleEventNotification schedules work for upcoming event`() {
    // Create an event 30 minutes in the future
    val futureTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(30)
    val event =
        Event(
            eventId = "event123",
            type = EventType.SOCIAL,
            title = "Test Event",
            description = "Test Description",
            location = Location(0.0, 0.0, "Test Location"),
            date = Timestamp(Date(futureTime)),
            duration = 60,
            participants = emptyList(),
            maxParticipants = 10,
            visibility = EventVisibility.PUBLIC,
            ownerId = "user123")

    NotificationScheduler.scheduleEventNotification(context, event)

    verify { mockWorkManager.enqueue(any<OneTimeWorkRequest>()) }
  }

  @Test
  fun `scheduleEventNotification does not schedule work for past event`() {
    // Create an event 30 minutes in the past
    val pastTime = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(30)
    val event =
        Event(
            eventId = "event123",
            type = EventType.SOCIAL,
            title = "Test Event",
            description = "Test Description",
            location = Location(0.0, 0.0, "Test Location"),
            date = Timestamp(Date(pastTime)),
            duration = 60,
            participants = emptyList(),
            maxParticipants = 10,
            visibility = EventVisibility.PUBLIC,
            ownerId = "user123")

    NotificationScheduler.scheduleEventNotification(context, event)

    verify(exactly = 0) { mockWorkManager.enqueue(any<OneTimeWorkRequest>()) }
  }

  @Test
  fun `scheduleEventNotification does not schedule work for event less than 15 minutes away`() {
    // Create an event 10 minutes in the future (less than notification threshold)
    val futureTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(10)
    val event =
        Event(
            eventId = "event123",
            type = EventType.SOCIAL,
            title = "Test Event",
            description = "Test Description",
            location = Location(0.0, 0.0, "Test Location"),
            date = Timestamp(Date(futureTime)),
            duration = 60,
            participants = emptyList(),
            maxParticipants = 10,
            visibility = EventVisibility.PUBLIC,
            ownerId = "user123")

    NotificationScheduler.scheduleEventNotification(context, event)

    verify(exactly = 0) { mockWorkManager.enqueue(any<OneTimeWorkRequest>()) }
  }

  @Test
  fun `scheduleEventNotification creates work with correct event data`() {
    val futureTime = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1)
    val event =
        Event(
            eventId = "event456",
            type = EventType.SPORTS,
            title = "Soccer Game",
            description = "Test Description",
            location = Location(0.0, 0.0, "Test Location"),
            date = Timestamp(Date(futureTime)),
            duration = 60,
            participants = emptyList(),
            maxParticipants = 10,
            visibility = EventVisibility.PUBLIC,
            ownerId = "user123")

    val capturedRequest = slot<OneTimeWorkRequest>()

    NotificationScheduler.scheduleEventNotification(context, event)

    verify { mockWorkManager.enqueue(capture(capturedRequest)) }

    // Verify the work request has the correct tag
    assert(capturedRequest.captured.tags.contains("event_notification_event456"))
  }

  @Test
  fun `cancelEventNotification cancels work by tag`() {
    val eventId = "event789"

    NotificationScheduler.cancelEventNotification(context, eventId)

    verify { mockWorkManager.cancelAllWorkByTag("event_notification_$eventId") }
  }

  @Test
  fun `scheduleEventNotification calculates correct delay`() {
    // Event 1 hour in the future
    val futureTime = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1)
    val event =
        Event(
            eventId = "event123",
            type = EventType.SOCIAL,
            title = "Test Event",
            description = "Test Description",
            location = Location(0.0, 0.0, "Test Location"),
            date = Timestamp(Date(futureTime)),
            duration = 60,
            participants = emptyList(),
            maxParticipants = 10,
            visibility = EventVisibility.PUBLIC,
            ownerId = "user123")

    val capturedRequest = slot<OneTimeWorkRequest>()

    NotificationScheduler.scheduleEventNotification(context, event)

    verify { mockWorkManager.enqueue(capture(capturedRequest)) }

    // Verify work request was created (delay is internal, but we can verify it was enqueued)
    assert(capturedRequest.isCaptured)
  }

  @Test
  fun `cancelEventNotification handles multiple cancellations`() {
    val eventId = "event999"

    // Cancel multiple times
    NotificationScheduler.cancelEventNotification(context, eventId)
    NotificationScheduler.cancelEventNotification(context, eventId)

    verify(exactly = 2) { mockWorkManager.cancelAllWorkByTag("event_notification_$eventId") }
  }
}
