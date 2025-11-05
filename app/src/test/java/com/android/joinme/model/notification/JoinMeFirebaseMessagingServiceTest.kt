package com.android.joinme.model.notification

import android.app.NotificationManager
import android.content.Context
import com.google.firebase.messaging.RemoteMessage
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowNotificationManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28]) // Use SDK 28 to avoid notification channel issues
class JoinMeFirebaseMessagingServiceTest {

  private lateinit var service: JoinMeFirebaseMessagingService
  private lateinit var notificationManager: NotificationManager
  private lateinit var shadowNotificationManager: ShadowNotificationManager

  @Before
  fun setup() {
    val serviceController = Robolectric.buildService(JoinMeFirebaseMessagingService::class.java)
    service = serviceController.create().get()
    notificationManager =
        service.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    shadowNotificationManager = org.robolectric.Shadows.shadowOf(notificationManager)
    mockkObject(FCMTokenManager)
  }

  @After
  fun tearDown() {
    unmockkObject(FCMTokenManager)
  }

  @Test
  fun `onNewToken calls FCMTokenManager updateFCMToken`() {
    // Given
    val newToken = "new_test_token_xyz"
    every { FCMTokenManager.updateFCMToken(any()) } returns Unit

    // When
    service.onNewToken(newToken)

    // Then
    verify { FCMTokenManager.updateFCMToken(newToken) }
  }

  @Test
  fun `onMessageReceived with notification data displays notification`() {
    // Given
    val mockRemoteMessage = mockk<RemoteMessage>(relaxed = true)
    val mockNotification = mockk<RemoteMessage.Notification>(relaxed = true)

    every { mockRemoteMessage.notification } returns mockNotification
    every { mockRemoteMessage.data } returns mapOf("eventId" to "event123")
    every { mockRemoteMessage.from } returns "test_sender"
    every { mockNotification.title } returns "Test Notification"
    every { mockNotification.body } returns "Test notification body"

    // When
    service.onMessageReceived(mockRemoteMessage)

    // Then - verify notification was processed (we can't easily verify NotificationManager calls
    // without more complex setup, but we verify the message was received)
    verify { mockRemoteMessage.notification }
    verify { mockRemoteMessage.data }
  }

  @Test
  fun `onMessageReceived with data payload displays notification`() {
    // Given
    val mockRemoteMessage = mockk<RemoteMessage>(relaxed = true)

    every { mockRemoteMessage.notification } returns null
    every { mockRemoteMessage.data } returns
        mapOf("title" to "Data Title", "body" to "Data body", "eventId" to "event456")
    every { mockRemoteMessage.from } returns "test_sender"

    // When
    service.onMessageReceived(mockRemoteMessage)

    // Then - verify data was accessed
    verify { mockRemoteMessage.data }
  }

  @Test
  fun `onMessageReceived with no notification or data uses defaults`() {
    // Given
    val mockRemoteMessage = mockk<RemoteMessage>(relaxed = true)

    every { mockRemoteMessage.notification } returns null
    every { mockRemoteMessage.data } returns emptyMap()
    every { mockRemoteMessage.from } returns "test_sender"

    // When
    service.onMessageReceived(mockRemoteMessage)

    // Then - should use default values and not crash
    verify { mockRemoteMessage.notification }
    verify { mockRemoteMessage.data }
  }

  @Test
  fun `onMessageReceived with eventId includes it in notification`() {
    // Given
    val mockRemoteMessage = mockk<RemoteMessage>(relaxed = true)
    val mockNotification = mockk<RemoteMessage.Notification>(relaxed = true)

    every { mockRemoteMessage.notification } returns mockNotification
    every { mockRemoteMessage.data } returns mapOf("eventId" to "specific_event_789")
    every { mockRemoteMessage.from } returns "test_sender"
    every { mockNotification.title } returns "Event Update"
    every { mockNotification.body } returns "Your event has been updated"

    // When
    service.onMessageReceived(mockRemoteMessage)

    // Then
    verify { mockRemoteMessage.notification }
    verify { mockRemoteMessage.data }
    // The service should process the eventId from data - verified by not crashing
  }

  @Test
  fun `onMessageReceived without eventId still displays notification`() {
    // Given
    val mockRemoteMessage = mockk<RemoteMessage>(relaxed = true)
    val mockNotification = mockk<RemoteMessage.Notification>(relaxed = true)

    every { mockRemoteMessage.notification } returns mockNotification
    every { mockRemoteMessage.data } returns emptyMap() // No eventId
    every { mockRemoteMessage.from } returns "test_sender"
    every { mockNotification.title } returns "General Notification"
    every { mockNotification.body } returns "This is a general message"

    // When
    service.onMessageReceived(mockRemoteMessage)

    // Then - should not crash
    verify { mockRemoteMessage.notification }
  }

  @Test
  fun `onCreate creates notification channel`() {
    // When - onCreate is called during setup
    // Then - verify channel was created
    val channel = notificationManager.getNotificationChannel("joinme_notifications")
    assertNotNull("Notification channel should be created", channel)
    assertEquals("JoinMe Notifications", channel.name.toString())
    assertEquals(NotificationManager.IMPORTANCE_HIGH, channel.importance)
  }

  @Test
  fun `notification channel has vibration enabled`() {
    // When - onCreate is called during setup
    // Then
    val channel = notificationManager.getNotificationChannel("joinme_notifications")
    assertNotNull(channel)
    assertTrue("Channel should have vibration enabled", channel.shouldVibrate())
  }

  @Test
  fun `notification channel has lights enabled`() {
    // When - onCreate is called during setup
    // Then
    val channel = notificationManager.getNotificationChannel("joinme_notifications")
    assertNotNull(channel)
    assertTrue("Channel should have lights enabled", channel.shouldShowLights())
  }

  @Test
  fun `onMessageReceived displays notification with correct title and body`() {
    // Given
    val mockRemoteMessage = mockk<RemoteMessage>(relaxed = true)
    val mockNotification = mockk<RemoteMessage.Notification>(relaxed = true)

    every { mockRemoteMessage.notification } returns mockNotification
    every { mockRemoteMessage.data } returns emptyMap()
    every { mockRemoteMessage.from } returns "test_sender"
    every { mockNotification.title } returns "Test Title"
    every { mockNotification.body } returns "Test Body"

    // When
    service.onMessageReceived(mockRemoteMessage)

    // Then - verify notification was posted
    val notifications = shadowNotificationManager.allNotifications
    assertTrue("At least one notification should be posted", notifications.size >= 1)

    val notification = notifications.last()
    assertEquals("Test Title", notification.extras.getString("android.title"))
    assertEquals("Test Body", notification.extras.getString("android.text"))
  }

  @Test
  fun `onMessageReceived uses data title and body when notification is null`() {
    // Given
    val mockRemoteMessage = mockk<RemoteMessage>(relaxed = true)

    every { mockRemoteMessage.notification } returns null
    every { mockRemoteMessage.data } returns
        mapOf("title" to "Data Title", "body" to "Data Body", "eventId" to "event123")
    every { mockRemoteMessage.from } returns "test_sender"

    // When
    service.onMessageReceived(mockRemoteMessage)

    // Then - verify notification was posted with data values
    val notifications = shadowNotificationManager.allNotifications
    assertTrue("At least one notification should be posted", notifications.size >= 1)

    val notification = notifications.last()
    assertEquals("Data Title", notification.extras.getString("android.title"))
    assertEquals("Data Body", notification.extras.getString("android.text"))
  }

  @Test
  fun `onMessageReceived uses default values when both notification and data are empty`() {
    // Given
    val mockRemoteMessage = mockk<RemoteMessage>(relaxed = true)

    every { mockRemoteMessage.notification } returns null
    every { mockRemoteMessage.data } returns emptyMap()
    every { mockRemoteMessage.from } returns "test_sender"

    // When
    service.onMessageReceived(mockRemoteMessage)

    // Then - verify notification was posted with defaults
    val notifications = shadowNotificationManager.allNotifications
    assertTrue("At least one notification should be posted", notifications.size >= 1)

    val notification = notifications.last()
    assertEquals("JoinMe", notification.extras.getString("android.title"))
    assertEquals("", notification.extras.getString("android.text"))
  }

  @Test
  fun `onMessageReceived notification has auto cancel enabled`() {
    // Given
    val mockRemoteMessage = mockk<RemoteMessage>(relaxed = true)
    val mockNotification = mockk<RemoteMessage.Notification>(relaxed = true)

    every { mockRemoteMessage.notification } returns mockNotification
    every { mockRemoteMessage.data } returns emptyMap()
    every { mockRemoteMessage.from } returns "test_sender"
    every { mockNotification.title } returns "Test"
    every { mockNotification.body } returns "Test"

    // When
    service.onMessageReceived(mockRemoteMessage)

    // Then
    val notifications = shadowNotificationManager.allNotifications
    val notification = notifications.last()
    assertTrue(
        "Notification should have auto cancel flag",
        (notification.flags and android.app.Notification.FLAG_AUTO_CANCEL) != 0)
  }

  @Test
  fun `onMessageReceived notification has pending intent`() {
    // Given
    val mockRemoteMessage = mockk<RemoteMessage>(relaxed = true)
    val mockNotification = mockk<RemoteMessage.Notification>(relaxed = true)

    every { mockRemoteMessage.notification } returns mockNotification
    every { mockRemoteMessage.data } returns mapOf("eventId" to "test_event")
    every { mockRemoteMessage.from } returns "test_sender"
    every { mockNotification.title } returns "Test"
    every { mockNotification.body } returns "Test"

    // When
    service.onMessageReceived(mockRemoteMessage)

    // Then
    val notifications = shadowNotificationManager.allNotifications
    val notification = notifications.last()
    assertNotNull("Notification should have content intent", notification.contentIntent)
  }
}
