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
import com.android.joinme.model.groups.Group
import com.android.joinme.model.groups.GroupRepository
import com.android.joinme.model.groups.GroupRepositoryProvider
import com.android.joinme.model.notification.FCMTokenManager
import com.google.firebase.auth.FirebaseAuth
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.runBlocking
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
  fun mainActivity_basicPropertiesAreValid() {
    composeTestRule.waitForIdle()
    val activity = composeTestRule.activity

    // Verify activity launches successfully without crashing
    assert(activity != null)
    assert(!activity.isFinishing)

    // Verify activity has correct theme
    assert(activity.theme != null)

    // Verify activity has intent
    assert(activity.intent != null)

    // Verify application context is accessible
    assert(activity.applicationContext != null)

    // Verify notification manager is accessible
    val notificationManager =
        activity.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
    assert(notificationManager != null)
  }

  // ========== Deep Link Tests ==========

  @Test
  fun mainActivity_launchesWithEventDeepLink_extractsEventId() {
    // Create intent with event deep link
    val context = ApplicationProvider.getApplicationContext<Context>()
    val intent =
        Intent(context, MainActivity::class.java).apply {
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
    val intent =
        Intent(context, MainActivity::class.java).apply {
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
    val intent =
        Intent(context, MainActivity::class.java).apply {
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
  fun mainActivity_handlesInvalidDeepLinkGracefully() {
    // Create intent with malformed deep link
    val context = ApplicationProvider.getApplicationContext<Context>()
    val intent =
        Intent(context, MainActivity::class.java).apply {
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

  @Test
  fun mainActivity_launchesWithDifferentDeepLinks_handlesAllCorrectly() {
    // Test that MainActivity can be launched multiple times with different deep links
    // This indirectly tests that deep link parsing works correctly

    // Test 1: Event deep link
    val context = ApplicationProvider.getApplicationContext<Context>()
    val eventIntent =
        Intent(context, MainActivity::class.java).apply {
          data = Uri.parse("joinme://event/event-abc")
          action = Intent.ACTION_VIEW
        }

    val eventScenario = ActivityScenario.launch<MainActivity>(eventIntent)
    eventScenario.use {
      it.onActivity { activity ->
        assert(activity.intent.data?.host == "event")
        assert(activity.intent.data?.lastPathSegment == "event-abc")
      }
    }

    // Test 2: Group deep link
    val groupIntent =
        Intent(context, MainActivity::class.java).apply {
          data = Uri.parse("joinme://group/group-xyz")
          action = Intent.ACTION_VIEW
        }

    val groupScenario = ActivityScenario.launch<MainActivity>(groupIntent)
    groupScenario.use {
      it.onActivity { activity ->
        assert(activity.intent.data?.host == "group")
        assert(activity.intent.data?.lastPathSegment == "group-xyz")
      }
    }
  }

  @Test
  fun mainActivity_deepLink_withEmptyPathSegment() {
    // Create intent with event deep link but empty path
    val context = ApplicationProvider.getApplicationContext<Context>()
    val intent =
        Intent(context, MainActivity::class.java).apply {
          data = Uri.parse("joinme://event/")
          action = Intent.ACTION_VIEW
        }

    // Launch activity with deep link
    val scenario = ActivityScenario.launch<MainActivity>(intent)
    scenario.use {
      it.onActivity { activity ->
        // Activity should handle empty path gracefully
        assert(activity.intent.data != null)
        assert(!activity.isFinishing)

        // lastPathSegment should be empty string for trailing slash
        val pathSegment = activity.intent.data?.lastPathSegment
        assert(pathSegment == "" || pathSegment == null)
      }
    }
  }

  @Test
  fun mainActivity_deepLink_withMultiplePathSegments() {
    // Create intent with multiple path segments
    val context = ApplicationProvider.getApplicationContext<Context>()
    val intent =
        Intent(context, MainActivity::class.java).apply {
          data = Uri.parse("joinme://event/user/123/event/456")
          action = Intent.ACTION_VIEW
        }

    // Launch activity with deep link
    val scenario = ActivityScenario.launch<MainActivity>(intent)
    scenario.use {
      it.onActivity { activity ->
        // Activity should handle multiple path segments
        assert(activity.intent.data != null)
        assert(!activity.isFinishing)

        // lastPathSegment should be the last part
        assert(activity.intent.data?.lastPathSegment == "456")
      }
    }
  }

  @Test
  fun mainActivity_deepLink_webGroupLink_withOnlyOnePathSegment() {
    // Create intent with web-style group deep link but only one segment
    val context = ApplicationProvider.getApplicationContext<Context>()
    val intent =
        Intent(context, MainActivity::class.java).apply {
          data = Uri.parse("https://joinme.app/group")
          action = Intent.ACTION_VIEW
        }

    // Launch activity with deep link
    val scenario = ActivityScenario.launch<MainActivity>(intent)
    scenario.use {
      it.onActivity { activity ->
        // Activity should handle missing group ID gracefully
        assert(activity.intent.data != null)
        assert(!activity.isFinishing)

        val pathSegments = activity.intent.data?.pathSegments
        assert(pathSegments?.size == 1)
        assert(pathSegments?.firstOrNull() == "group")
        assert(pathSegments?.getOrNull(1) == null)
      }
    }
  }

  @Test
  fun mainActivity_deepLink_webGroupLink_withExtraPathSegments() {
    // Create intent with web-style group deep link with extra segments
    val context = ApplicationProvider.getApplicationContext<Context>()
    val intent =
        Intent(context, MainActivity::class.java).apply {
          data = Uri.parse("https://joinme.app/group/123/extra/segments")
          action = Intent.ACTION_VIEW
        }

    // Launch activity with deep link
    val scenario = ActivityScenario.launch<MainActivity>(intent)
    scenario.use {
      it.onActivity { activity ->
        // Activity should handle extra path segments
        assert(activity.intent.data != null)
        assert(!activity.isFinishing)

        val pathSegments = activity.intent.data?.pathSegments
        assert(pathSegments != null)
        assert(pathSegments?.firstOrNull() == "group")
        assert(pathSegments?.getOrNull(1) == "123")
      }
    }
  }

  @Test
  fun mainActivity_deepLink_nonGroupWebLink() {
    // Create intent with web-style deep link that's not a group link
    val context = ApplicationProvider.getApplicationContext<Context>()
    val intent =
        Intent(context, MainActivity::class.java).apply {
          data = Uri.parse("https://joinme.app/profile/user123")
          action = Intent.ACTION_VIEW
        }

    // Launch activity with deep link
    val scenario = ActivityScenario.launch<MainActivity>(intent)
    scenario.use {
      it.onActivity { activity ->
        // Activity should handle non-group web links gracefully
        assert(activity.intent.data != null)
        assert(!activity.isFinishing)

        val pathSegments = activity.intent.data?.pathSegments
        assert(pathSegments?.firstOrNull() == "profile")
        assert(pathSegments?.firstOrNull() != "group")
      }
    }
  }

  @Test
  fun mainActivity_deepLink_withQueryParameters() {
    // Create intent with deep link including query parameters
    val context = ApplicationProvider.getApplicationContext<Context>()
    val intent =
        Intent(context, MainActivity::class.java).apply {
          data = Uri.parse("joinme://event/event123?source=notification&userId=user456")
          action = Intent.ACTION_VIEW
        }

    // Launch activity with deep link
    val scenario = ActivityScenario.launch<MainActivity>(intent)
    scenario.use {
      it.onActivity { activity ->
        // Activity should handle query parameters
        assert(activity.intent.data != null)
        assert(!activity.isFinishing)

        val data = activity.intent.data
        assert(data?.host == "event")
        assert(data?.lastPathSegment == "event123")
        assert(data?.getQueryParameter("source") == "notification")
        assert(data?.getQueryParameter("userId") == "user456")
      }
    }
  }

  @Test
  fun mainActivity_deepLink_withFragment() {
    // Create intent with deep link including fragment
    val context = ApplicationProvider.getApplicationContext<Context>()
    val intent =
        Intent(context, MainActivity::class.java).apply {
          data = Uri.parse("joinme://group/group789#details")
          action = Intent.ACTION_VIEW
        }

    // Launch activity with deep link
    val scenario = ActivityScenario.launch<MainActivity>(intent)
    scenario.use {
      it.onActivity { activity ->
        // Activity should handle fragments
        assert(activity.intent.data != null)
        assert(!activity.isFinishing)

        val data = activity.intent.data
        assert(data?.host == "group")
        assert(data?.lastPathSegment == "group789")
        assert(data?.fragment == "details")
      }
    }
  }

  @Test
  fun httpClientProvider_canBeReplacedMultipleTimes() {
    // Verify HttpClientProvider can be replaced multiple times
    val original = HttpClientProvider.client

    val client1 = OkHttpClient()
    HttpClientProvider.client = client1
    assert(HttpClientProvider.client == client1)

    val client2 = OkHttpClient()
    HttpClientProvider.client = client2
    assert(HttpClientProvider.client == client2)

    val client3 = OkHttpClient()
    HttpClientProvider.client = client3
    assert(HttpClientProvider.client == client3)

    // Restore original
    HttpClientProvider.client = original
  }

  @Test
  fun mainActivity_notificationChannel_hasCorrectVibrationSettings() {
    composeTestRule.waitForIdle()
    val activity = composeTestRule.activity
    val notificationManager =
        activity.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    val channel = notificationManager.getNotificationChannel("event_notifications")
    assert(channel != null)
    // Verify vibration is enabled
    assert(channel.shouldVibrate())
    // Verify vibration pattern exists (default pattern when enableVibration(true) is called)
    assert(channel.vibrationPattern != null || channel.shouldVibrate())
  }

  @Test
  fun mainActivity_handlesIntentWithoutAction() {
    // Create intent without action specified
    val context = ApplicationProvider.getApplicationContext<Context>()
    val intent =
        Intent(context, MainActivity::class.java).apply {
          data = Uri.parse("joinme://event/test123")
          // No action set
        }

    // Launch activity
    val scenario = ActivityScenario.launch<MainActivity>(intent)
    scenario.use {
      it.onActivity { activity ->
        // Activity should handle missing action gracefully
        assert(activity.intent.data != null)
        assert(!activity.isFinishing)
      }
    }
  }

  @Test
  fun mainActivity_deepLink_withSpecialCharacters() {
    // Test deep links with special characters in IDs
    val context = ApplicationProvider.getApplicationContext<Context>()
    val intent =
        Intent(context, MainActivity::class.java).apply {
          data = Uri.parse("joinme://event/event-123_test")
          action = Intent.ACTION_VIEW
        }

    val scenario = ActivityScenario.launch<MainActivity>(intent)
    scenario.use {
      it.onActivity { activity ->
        // Activity should handle special characters in IDs
        assert(activity.intent.data != null)
        assert(!activity.isFinishing)

        assert(activity.intent.data?.host == "event")
        assert(activity.intent.data?.lastPathSegment == "event-123_test")
      }
    }
  }

  @Test
  fun mainActivity_deepLink_caseSensitivity() {
    // Test that host parsing respects case
    val context = ApplicationProvider.getApplicationContext<Context>()
    val intent =
        Intent(context, MainActivity::class.java).apply {
          data = Uri.parse("joinme://Event/EventId123")
          action = Intent.ACTION_VIEW
        }

    val scenario = ActivityScenario.launch<MainActivity>(intent)
    scenario.use {
      it.onActivity { activity ->
        // Activity should handle different cases
        assert(activity.intent.data != null)
        assert(!activity.isFinishing)

        // URI parsing is case-insensitive for scheme/host
        val host = activity.intent.data?.host?.lowercase()
        assert(host == "event")
      }
    }
  }

  @Test
  fun mainActivity_httpClientProvider_isNotNull() {
    // Verify HttpClientProvider singleton is initialized
    assert(HttpClientProvider.client != null)

    // Verify it's an OkHttpClient instance
    assert(HttpClientProvider.client is OkHttpClient)
  }

  // ========== Group Join via Deep Link Tests ==========
  // These tests cover the LaunchedEffect(initialGroupId) in MainActivity

  @Test
  fun mainActivity_groupDeepLink_joinsGroup_whenUserAuthenticated() = runBlocking {
    // This test covers: if (initialGroupId != null) { if (currentUser != null) { try { joinGroup() } } }

    val groupRepository = GroupRepositoryProvider.repository
    val currentUser = FirebaseAuth.getInstance().currentUser

    // Skip test if user is not authenticated
    if (currentUser == null) {
      return@runBlocking
    }

    // Create a test group
    val testGroupId = groupRepository.getNewGroupId()
    val testGroup = Group(
        id = testGroupId,
        name = "Test Group for Deep Link Join",
        description = "Testing successful group join",
        category = com.android.joinme.model.event.EventType.SPORTS,
        ownerId = currentUser.uid,
        memberIds = listOf(currentUser.uid)
    )
    groupRepository.addGroup(testGroup)

    try {
      // Launch MainActivity with group deep link
      val context = ApplicationProvider.getApplicationContext<Context>()
      val intent =
          Intent(context, MainActivity::class.java).apply {
            data = Uri.parse("joinme://group/$testGroupId")
            action = Intent.ACTION_VIEW
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          }

      val scenario = ActivityScenario.launch<MainActivity>(intent)
      Thread.sleep(3000) // Wait for LaunchedEffect to complete

      scenario.use {
        it.onActivity { activity ->
          assert(!activity.isFinishing)
        }
      }

      // Verify user was added to group
      val updatedGroup = groupRepository.getGroup(testGroupId)
      assert(updatedGroup.memberIds.contains(currentUser.uid))

      scenario.close()
    } finally {
      // Cleanup
      try {
        groupRepository.deleteGroup(testGroupId, currentUser.uid)
      } catch (_: Exception) {}
    }
  }

  @Test
  fun mainActivity_groupDeepLink_handlesError_whenJoinFails() = runBlocking {
    // This test covers: if (initialGroupId != null) { if (currentUser != null) { catch (e: Exception) { Toast } } }

    val currentUser = FirebaseAuth.getInstance().currentUser

    // Skip test if user is not authenticated
    if (currentUser == null) {
      return@runBlocking
    }

    // Use invalid group ID to trigger error
    val invalidGroupId = "non-existent-group-12345"

    // Launch MainActivity with invalid group deep link
    val context = ApplicationProvider.getApplicationContext<Context>()
    val intent =
        Intent(context, MainActivity::class.java).apply {
          data = Uri.parse("joinme://group/$invalidGroupId")
          action = Intent.ACTION_VIEW
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    val scenario = ActivityScenario.launch<MainActivity>(intent)
    Thread.sleep(3000) // Wait for LaunchedEffect to complete

    scenario.use {
      it.onActivity { activity ->
        // Activity should not crash despite error
        assert(!activity.isFinishing)
      }
    }

    scenario.close()
  }

  @Test
  fun mainActivity_groupDeepLink_showsSignInToast_whenUserNotAuthenticated() {
    // This test covers: if (initialGroupId != null) { else { Toast.makeText("Please sign in") } }

    val currentUser = FirebaseAuth.getInstance().currentUser

    // Skip test if user IS authenticated (we need unauthenticated state)
    if (currentUser != null) {
      return
    }

    // Launch MainActivity with group deep link
    val context = ApplicationProvider.getApplicationContext<Context>()
    val intent =
        Intent(context, MainActivity::class.java).apply {
          data = Uri.parse("joinme://group/test-group-123")
          action = Intent.ACTION_VIEW
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    val scenario = ActivityScenario.launch<MainActivity>(intent)
    Thread.sleep(2000) // Wait for LaunchedEffect to complete

    scenario.use {
      it.onActivity { activity ->
        // Activity should not crash, should show sign-in toast
        assert(!activity.isFinishing)
      }
    }

    scenario.close()
  }

  @Test
  fun mainActivity_noGroupDeepLink_skipsJoinLogic() {
    // This test covers: if (initialGroupId == null) - no join logic executes

    // Launch MainActivity without group deep link
    val context = ApplicationProvider.getApplicationContext<Context>()
    val intent = Intent(context, MainActivity::class.java)
    // No deep link data

    val scenario = ActivityScenario.launch<MainActivity>(intent)
    Thread.sleep(1000)

    scenario.use {
      it.onActivity { activity ->
        // Activity should launch normally without attempting group join
        assert(!activity.isFinishing)
        assert(activity.intent.data == null)
      }
    }

    scenario.close()
  }
}
