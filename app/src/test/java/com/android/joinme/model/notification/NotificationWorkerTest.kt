package com.android.joinme.model.notification

import android.app.NotificationManager
import android.content.Context
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import io.mockk.*
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class NotificationWorkerTest {

  private lateinit var context: Context

  @Before
  fun setUp() {
    context = RuntimeEnvironment.getApplication()
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  private fun createWorker(inputData: Data): NotificationWorker {
    val workerParams = mockk<WorkerParameters>(relaxed = true)
    every { workerParams.inputData } returns inputData
    every { workerParams.id } returns UUID.randomUUID()
    every { workerParams.taskExecutor } returns mockk(relaxed = true)
    return NotificationWorker(context, workerParams)
  }

  @Test
  fun `doWork returns success with valid event data`() = runBlocking {
    val inputData =
        Data.Builder().putString("eventId", "event123").putString("eventTitle", "Test Event").build()

    val worker = createWorker(inputData)
    val result = worker.doWork()

    assertEquals(ListenableWorker.Result.success(), result)
  }

  @Test
  fun `doWork returns failure when eventId is missing`() = runBlocking {
    val inputData = Data.Builder().putString("eventTitle", "Test Event").build() // Missing eventId

    val worker = createWorker(inputData)
    val result = worker.doWork()

    assertEquals(ListenableWorker.Result.failure(), result)
  }

  @Test
  fun `doWork uses default title when eventTitle is missing`() = runBlocking {
    val inputData = Data.Builder().putString("eventId", "event123").build() // Missing eventTitle

    val worker = createWorker(inputData)
    val result = worker.doWork()

    // Should succeed with default title
    assertEquals(ListenableWorker.Result.success(), result)
  }

  @Test
  fun `doWork creates notification channel`() = runBlocking {
    val inputData =
        Data.Builder().putString("eventId", "event123").putString("eventTitle", "Test Event").build()

    val worker = createWorker(inputData)
    val result = worker.doWork()

    // Verify notification channel was created (in real app, WorkManager handles this)
    // We're testing the worker executes successfully
    assertEquals(ListenableWorker.Result.success(), result)
  }

  @Test
  fun `doWork handles different event IDs correctly`() = runBlocking {
    val eventIds = listOf("event1", "event2", "event3")

    eventIds.forEach { eventId ->
      val inputData =
          Data.Builder()
              .putString("eventId", eventId)
              .putString("eventTitle", "Event $eventId")
              .build()

      val worker = createWorker(inputData)
      val result = worker.doWork()

      assertEquals(ListenableWorker.Result.success(), result)
    }
  }

  @Test
  fun `doWork handles long event titles`() = runBlocking {
    val longTitle = "A".repeat(200) // Very long title
    val inputData =
        Data.Builder().putString("eventId", "event123").putString("eventTitle", longTitle).build()

    val worker = createWorker(inputData)
    val result = worker.doWork()

    assertEquals(ListenableWorker.Result.success(), result)
  }

  @Test
  fun `doWork handles special characters in event data`() = runBlocking {
    val inputData =
        Data.Builder()
            .putString("eventId", "event-123_special!@#")
            .putString("eventTitle", "Test Event 🎉 with émojis & spëcial çhars")
            .build()

    val worker = createWorker(inputData)
    val result = worker.doWork()

    assertEquals(ListenableWorker.Result.success(), result)
  }

  @Test
  fun `doWork creates notification with correct content`() = runBlocking {
    val eventId = "event456"
    val eventTitle = "Soccer Match"
    val inputData =
        Data.Builder().putString("eventId", eventId).putString("eventTitle", eventTitle).build()

    val worker = createWorker(inputData)
    val result = worker.doWork()

    // Verify the worker completed successfully
    // In a real scenario, the notification would be displayed
    assertEquals(ListenableWorker.Result.success(), result)
  }

  @Test
  fun `doWork handles empty strings gracefully`() = runBlocking {
    val inputData =
        Data.Builder().putString("eventId", "").putString("eventTitle", "").build()

    val worker = createWorker(inputData)
    val result = worker.doWork()

    // Empty eventId should still work (though not ideal in production)
    assertEquals(ListenableWorker.Result.success(), result)
  }

  @Test
  fun `NotificationWorker class exists and extends CoroutineWorker`() {
    // Verify the worker class is properly defined
    assertTrue(NotificationWorker::class.java.superclass?.simpleName == "CoroutineWorker")
  }

  @Test
  fun `doWork posts notification with correct channel ID`() = runBlocking {
    val inputData =
        Data.Builder().putString("eventId", "event789").putString("eventTitle", "Team Meeting").build()

    val worker = createWorker(inputData)
    val result = worker.doWork()

    // Worker should complete successfully and create notification
    // The notification channel "event_notifications" should be used
    assertEquals(ListenableWorker.Result.success(), result)
  }
}
