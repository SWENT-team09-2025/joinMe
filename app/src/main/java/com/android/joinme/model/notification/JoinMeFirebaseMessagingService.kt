package com.android.joinme.model.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.android.joinme.MainActivity
import com.android.joinme.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Service to handle Firebase Cloud Messaging notifications.
 *
 * This service receives push notifications from Firebase and displays them to the user. It also
 * handles FCM token refresh events.
 */
class JoinMeFirebaseMessagingService : FirebaseMessagingService() {

  companion object {
    private const val TAG = "JoinMeFCMService"
    private const val CHANNEL_ID = "joinme_notifications"
    private const val CHANNEL_NAME = "JoinMe Notifications"
    private const val CHANNEL_DESCRIPTION = "Notifications for event updates and invitations"
  }

  override fun onCreate() {
    super.onCreate()
    createNotificationChannel()
  }

  /**
   * Called when a new FCM token is generated.
   *
   * This occurs when the app is first installed, when the user reinstalls the app, or when the
   * token is refreshed. We update the token in Firestore so the backend can send notifications.
   */
  override fun onNewToken(token: String) {
    super.onNewToken(token)
    Log.d(TAG, "New FCM token: $token")

    // Update the token in Firestore
    FCMTokenManager.updateFCMToken(token)
  }

  /**
   * Called when a message is received from Firebase.
   *
   * The message contains notification data such as title, body, and custom data payload.
   */
  override fun onMessageReceived(message: RemoteMessage) {
    super.onMessageReceived(message)
    Log.d(TAG, "Message received from: ${message.from}")

    // Extract notification data
    val notification = message.notification
    val data = message.data

    val title = notification?.title ?: data["title"] ?: "JoinMe"
    val body = notification?.body ?: data["body"] ?: ""
    val eventId = data["eventId"]

    Log.d(TAG, "Notification - Title: $title, Body: $body, EventId: $eventId")

    // Display the notification
    showNotification(title, body, eventId)
  }

  /**
   * Creates the notification channel for Android O and above.
   *
   * Notification channels are required for Android 8.0 (API level 26) and higher.
   */
  private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel =
          NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
            description = CHANNEL_DESCRIPTION
            enableVibration(true)
            enableLights(true)
          }

      val notificationManager =
          getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      notificationManager.createNotificationChannel(channel)
    }
  }

  /**
   * Displays a notification to the user.
   *
   * @param title The notification title
   * @param body The notification body text
   * @param eventId Optional event ID to open when the notification is tapped
   */
  private fun showNotification(title: String, body: String, eventId: String?) {
    val intent =
        Intent(this, MainActivity::class.java).apply {
          flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
          eventId?.let { putExtra("eventId", it) }
        }

    val pendingIntent =
        PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    val notificationBuilder =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)

    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
  }
}
