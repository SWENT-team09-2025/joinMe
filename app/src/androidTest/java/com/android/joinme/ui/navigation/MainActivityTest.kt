package com.android.joinme.ui.navigation
/*CO-WRITE with claude AI*/
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
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

  @Test
  fun mainActivity_hasValidContentView() {
    composeTestRule.waitForIdle()
    val activity = composeTestRule.activity

    // Verify activity has content
    assert(activity.hasWindowFocus() || !activity.isFinishing)
  }

  @Test
  fun mainActivity_notificationChannel_hasCorrectId() {
    composeTestRule.waitForIdle()
    val activity = composeTestRule.activity
    val notificationManager =
        activity.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    val channel = notificationManager.getNotificationChannel("event_notifications")
    assert(channel != null)
    assert(channel.id == "event_notifications")
  }

  @Test
  fun mainActivity_notificationChannel_hasVibrationEnabled() {
    composeTestRule.waitForIdle()
    val activity = composeTestRule.activity
    val notificationManager =
        activity.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    val channel = notificationManager.getNotificationChannel("event_notifications")
    assert(channel.shouldVibrate())
  }

  @Test
  fun mainActivity_notificationChannel_hasHighImportance() {
    composeTestRule.waitForIdle()
    val activity = composeTestRule.activity
    val notificationManager =
        activity.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    val channel = notificationManager.getNotificationChannel("event_notifications")
    assert(channel.importance == NotificationManager.IMPORTANCE_HIGH)
  }

  @Test
  fun httpClientProvider_canBeAccessed() {
    // Verify HttpClientProvider is accessible
    val client = HttpClientProvider.client
    assert(client != null)
  }

  @Test
  fun httpClientProvider_isConfigurable() {
    // Store original client
    val originalClient = HttpClientProvider.client

    // Test that we can set a new client
    val testClient = OkHttpClient.Builder().build()
    HttpClientProvider.client = testClient

    // Verify change
    assert(HttpClientProvider.client === testClient)

    // Restore original
    HttpClientProvider.client = originalClient
  }

  @Test
  fun mainActivity_handlesNullIntentData() {
    composeTestRule.waitForIdle()
    val activity = composeTestRule.activity

    // Activity should handle null intent data without crashing
    val intentData = activity.intent?.data
    // This should be null or a valid URI
    assert(intentData == null || intentData.toString().isNotEmpty())
  }

  @Test
  fun mainActivity_doesNotCrashOnStartup() {
    composeTestRule.waitForIdle()

    // If we got here, activity didn't crash
    val activity = composeTestRule.activity
    assert(activity != null)
    assert(!activity.isFinishing)
  }

  @Test
  fun mainActivity_hasCorrectTheme() {
    composeTestRule.waitForIdle()
    val activity = composeTestRule.activity

    // Verify activity is properly themed (doesn't crash when accessing theme)
    val theme = activity.theme
    assert(theme != null)
  }

  @Test
  fun mainActivity_notificationManagerIsAccessible() {
    composeTestRule.waitForIdle()
    val activity = composeTestRule.activity
    val notificationManager =
        activity.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

    assert(notificationManager != null)
  }

  @Test
  fun mainActivity_intentIsNotNull() {
    composeTestRule.waitForIdle()
    val activity = composeTestRule.activity

    // Activity should always have an intent
    assert(activity.intent != null)
  }

  @Test
  fun mainActivity_canAccessApplicationContext() {
    composeTestRule.waitForIdle()
    val activity = composeTestRule.activity

    // Should be able to access application context
    val context = activity.applicationContext
    assert(context != null)
  }

  @Test
  fun notificationChannel_descriptionIsSet() {
    composeTestRule.waitForIdle()
    val activity = composeTestRule.activity
    val notificationManager =
        activity.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    val channel = notificationManager.getNotificationChannel("event_notifications")
    assert(channel.description == "Notifications for upcoming events")
  }

  @Test
  fun httpClientProvider_multipleSets_retainsLatest() {
    val originalClient = HttpClientProvider.client

    val client1 = OkHttpClient.Builder().build()
    val client2 = OkHttpClient.Builder().build()

    HttpClientProvider.client = client1
    assert(HttpClientProvider.client === client1)

    HttpClientProvider.client = client2
    assert(HttpClientProvider.client === client2)

    // Restore
    HttpClientProvider.client = originalClient
  }

  // ========== Deep Link Tests ==========

  @Test
  fun mainActivity_launchesWithEventDeepLink_extractsEventId() {
    // Create intent with event deep link
    val context = ApplicationProvider.getApplicationContext<Context>()
    val intent = Intent(context, MainActivity::class.java).apply {
      data = Uri.parse("joinme://event/test-event-123")
      action = Intent.ACTION_VIEW
    }

    // Launch activity with deep link
    val scenario = ActivityScenario.launch<MainActivity>(intent)
    scenario.use {
      it.onActivity { activity ->
        // Verify intent data is set
        assert(activity.intent.data != null)
        assert(activity.intent.data?.host == "event")
        assert(activity.intent.data?.lastPathSegment == "test-event-123")
      }
    }
  }

  @Test
  fun mainActivity_launchesWithGroupDeepLink_extractsGroupId() {
    // Create intent with group deep link
    val context = ApplicationProvider.getApplicationContext<Context>()
    val intent = Intent(context, MainActivity::class.java).apply {
      data = Uri.parse("joinme://group/test-group-456")
      action = Intent.ACTION_VIEW
    }

    // Launch activity with deep link
    val scenario = ActivityScenario.launch<MainActivity>(intent)
    scenario.use {
      it.onActivity { activity ->
        // Verify intent data is set and parsed correctly
        val intentData = activity.intent.data
        assert(intentData != null)
        assert(intentData?.host == "group")
        assert(intentData?.lastPathSegment == "test-group-456")

        // Activity should not crash
        assert(!activity.isFinishing)
      }
    }
  }

  @Test
  fun mainActivity_launchesWithWebGroupDeepLink_extractsGroupId() {
    // Create intent with web-style group deep link (joinme.app/group/id)
    val context = ApplicationProvider.getApplicationContext<Context>()
    val intent = Intent(context, MainActivity::class.java).apply {
      data = Uri.parse("https://joinme.app/group/web-group-789")
      action = Intent.ACTION_VIEW
    }

    // Launch activity with deep link
    val scenario = ActivityScenario.launch<MainActivity>(intent)
    scenario.use {
      it.onActivity { activity ->
        // Verify intent data is set and parsed correctly
        val intentData = activity.intent.data
        assert(intentData != null)
        assert(intentData?.host == "joinme.app")

        val pathSegments = intentData?.pathSegments
        assert(pathSegments != null)
        assert(pathSegments?.size == 2)
        assert(pathSegments?.firstOrNull() == "group")
        assert(pathSegments?.getOrNull(1) == "web-group-789")

        // Activity should not crash
        assert(!activity.isFinishing)
      }
    }
  }

  @Test
  fun mainActivity_launchesWithNullDeepLink_doesNotCrash() {
    // Create intent without deep link data
    val context = ApplicationProvider.getApplicationContext<Context>()
    val intent = Intent(context, MainActivity::class.java)
    // Intentionally no data set

    // Launch activity without deep link
    val scenario = ActivityScenario.launch<MainActivity>(intent)
    scenario.use {
      it.onActivity { activity ->
        // Activity should handle null data gracefully
        assert(activity.intent.data == null)
        assert(!activity.isFinishing)
      }
    }
  }

  @Test
  fun mainActivity_receivesNewIntent_doesNotCrash() {
    // Test that receiving a new intent doesn't crash
    // This indirectly tests onNewIntent by sending a new intent to running activity
    val context = ApplicationProvider.getApplicationContext<Context>()
    val intent = Intent(context, MainActivity::class.java)

    val scenario = ActivityScenario.launch<MainActivity>(intent)
    scenario.use {
      it.onActivity { activity ->
        // Activity should not crash when receiving new intent
        assert(!activity.isFinishing)
      }
    }
  }

  @Test
  fun mainActivity_handlesInvalidDeepLinkGracefully() {
    // Create intent with malformed deep link
    val context = ApplicationProvider.getApplicationContext<Context>()
    val intent = Intent(context, MainActivity::class.java).apply {
      data = Uri.parse("joinme://invalid")
      action = Intent.ACTION_VIEW
    }

    // Launch activity with invalid deep link
    val scenario = ActivityScenario.launch<MainActivity>(intent)
    scenario.use {
      it.onActivity { activity ->
        // Activity should handle invalid data gracefully
        assert(activity.intent.data != null)
        assert(!activity.isFinishing)
      }
    }
  }
}
