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

object NotificationScheduler {
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

  fun cancelEventNotification(context: Context, eventId: String) {
    WorkManager.getInstance(context).cancelAllWorkByTag("event_notification_$eventId")
  }
}

class NotificationWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

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
