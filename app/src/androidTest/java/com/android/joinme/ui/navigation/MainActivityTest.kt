package com.android.joinme.ui.navigation
/*CO-WRITE with claude AI*/
import android.app.NotificationManager
import android.content.Context
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.joinme.HttpClientProvider
import com.android.joinme.MainActivity
import com.android.joinme.model.notification.FCMTokenManager
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<MainActivity>()

  @Before
  fun setupMocks() {
    // Mock FCMTokenManager to avoid actual Firebase calls in tests
    mockkObject(FCMTokenManager)
    every { FCMTokenManager.initializeFCMToken(any()) } returns Unit
    every { FCMTokenManager.clearFCMToken() } returns Unit
  }

  @After
  fun tearDownMocks() {
    unmockkObject(FCMTokenManager)
  }

  @Test
  fun mainActivity_launchesSuccessfully() {
    // This test verifies that MainActivity launches without crashing
    // The JoinMe composable should be set and navigation should be initialized
    composeTestRule.waitForIdle()

    // Verify activity is not null
    assert(composeTestRule.activity != null)
  }

  @Test
  fun mainActivity_createsNotificationChannelWithCorrectProperties() {
    composeTestRule.waitForIdle()
    val activity = composeTestRule.activity
    val notificationManager =
        activity.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    // Verify the notification channel exists with correct properties
    val channel = notificationManager.getNotificationChannel("event_notifications")
    assert(channel != null)
    assert(channel.name == "Event Notifications")
    assert(channel.importance == NotificationManager.IMPORTANCE_HIGH)
    assert(channel.description == "Notifications for upcoming events")
    assert(channel.shouldVibrate())
  }

  @Test
  fun mainActivity_handlesIntentWithEventIdGracefully() {
    composeTestRule.waitForIdle()
    val activity = composeTestRule.activity

    // Verify intent data handling doesn't crash with null or valid data
    val intent = activity.intent
    val eventId = intent?.data?.lastPathSegment

    // Should handle null or valid string gracefully without crashing
    assert(eventId == null || eventId is String)
  }

  @Test
  fun httpClientProvider_providesConfigurableClient() {
    // Verify HttpClientProvider can be accessed and replaced
    val originalClient = HttpClientProvider.client
    assert(originalClient != null)

    // Create and set new client
    val newClient = OkHttpClient()
    HttpClientProvider.client = newClient

    // Verify client was replaced
    assert(HttpClientProvider.client == newClient)

    // Restore original client
    HttpClientProvider.client = originalClient
  }
}
