package com.android.joinme.model.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.android.joinme.R
import com.android.joinme.model.event.Event
import java.util.concurrent.TimeUnit

/**
 * Manages the scheduling and cancellation of event notifications.
 *
 * This singleton object provides functionality to schedule notifications for events that will
 * trigger 15 minutes before the event starts. It uses WorkManager to ensure notifications are
 * delivered reliably even if the app is closed or the device is restarted.
 *
 * Example usage:
 * ```
 * val event = Event(...)
 * NotificationScheduler.scheduleEventNotification(context, event)
 * ```
 */
object NotificationScheduler {
  /**
   * Schedules a notification to be displayed 15 minutes before the event starts.
   *
   * The notification will only be scheduled if the event time minus 15 minutes is in the future. If
   * the event is starting in less than 15 minutes or has already passed, no notification will be
   * scheduled.
   *
   * @param context The application context used to access the WorkManager
   * @param event The event for which to schedule a notification. Must have a valid eventId, title,
   *   and date.
   * @see cancelEventNotification
   */
  fun scheduleEventNotification(context: Context, event: Event) {
    val now = System.currentTimeMillis()
    val eventTime = event.date.toDate().time
    val delay = eventTime - now - TimeUnit.MINUTES.toMillis(15)

    if (delay > 0) {
      val data =
          Data.Builder()
              .putString("eventId", event.eventId)
              .putString("eventTitle", event.title)
              .build()

      val notificationWork =
          OneTimeWorkRequestBuilder<NotificationWorker>()
              .setInitialDelay(delay, TimeUnit.MILLISECONDS)
              .setInputData(data)
              .addTag("event_notification_${event.eventId}")
              .build()

      WorkManager.getInstance(context).enqueue(notificationWork)
    }
  }

  /**
   * Cancels a previously scheduled notification for an event.
   *
   * This method will remove any pending notification work for the specified event. If no
   * notification was scheduled for the event, this method has no effect.
   *
   * @param context The application context used to access the WorkManager
   * @param eventId The unique identifier of the event whose notification should be cancelled
   * @see scheduleEventNotification
   */
  fun cancelEventNotification(context: Context, eventId: String) {
    WorkManager.getInstance(context).cancelAllWorkByTag("event_notification_$eventId")
  }
}

/**
 * Worker class that handles the actual display of event notifications.
 *
 * This worker is scheduled by [NotificationScheduler] and executed by WorkManager when it's time to
 * display a notification. It creates a notification channel, builds the notification with the event
 * details, and displays it to the user.
 *
 * The notification includes:
 * - Event title as the notification title
 * - "Starts in 15 minutes" as the content text
 * - A clickable intent that opens the event details screen
 * - High priority and vibration enabled
 *
 * @param appContext The application context
 * @param workerParams Parameters for this work request, including the event ID and title
 * @see NotificationScheduler
 */
class NotificationWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

  /**
   * Performs the work of creating and displaying the event notification.
   *
   * This method:
   * 1. Extracts event details (ID and title) from the input data
   * 2. Creates a notification channel for event notifications
   * 3. Builds a notification with the event information
   * 4. Creates a deep link to open the event details when tapped
   * 5. Displays the notification to the user
   *
   * @return [Result.success] if the notification was displayed successfully, [Result.failure] if
   *   the event ID is missing from the input data
   */
  override suspend fun doWork(): Result {
    val eventId = inputData.getString("eventId") ?: return Result.failure()
    val eventTitle = inputData.getString("eventTitle") ?: "Your event is starting soon!"

    val notificationManager =
        applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    val channel =
        NotificationChannel(
            "event_notifications", "Event Notifications", NotificationManager.IMPORTANCE_HIGH)
    channel.enableVibration(true)
    notificationManager.createNotificationChannel(channel)

    // Create an intent to open the event screen
    val intent =
        Intent(Intent.ACTION_VIEW, Uri.parse("joinme://event/$eventId")).apply {
          setPackage(applicationContext.packageName)
          flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

    val pendingIntent =
        PendingIntent.getActivity(
            applicationContext, eventId.hashCode(), intent, PendingIntent.FLAG_IMMUTABLE)

    val notification =
        NotificationCompat.Builder(applicationContext, "event_notifications")
            .setContentTitle(eventTitle)
            .setContentText("Starts in 15 minutes.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

    notificationManager.notify(eventId.hashCode(), notification)

    return Result.success()
  }
}
