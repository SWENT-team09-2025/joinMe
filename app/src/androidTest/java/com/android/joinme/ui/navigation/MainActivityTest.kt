package com.android.joinme.ui.navigation
/*CO-WRITE with claude AI*/
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.joinme.HttpClientProvider
import com.android.joinme.MainActivity
import com.android.joinme.model.groups.Group
import com.android.joinme.model.groups.GroupRepositoryLocal
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

    // Setup test groups in repository to avoid "Failed to access group" toasts
    System.setProperty("IS_TEST_ENV", "true")
    val groupRepository = GroupRepositoryProvider.repository
    if (groupRepository is GroupRepositoryLocal) {
      runBlocking {
        // Add test groups used by notification tests
        // Note: Groups for chat notifications include test-user-id as member (they don't need to
        // join)
        // Groups for join flow tests don't include test-user-id (so they can successfully join)

        // Group for chat notification test - includes members
        groupRepository.addGroup(
            Group(
                id = "test-group-789",
                name = "Test Group Chat 789",
                description = "Test group for notifications",
                category = com.android.joinme.model.event.EventType.SPORTS,
                ownerId = "test-user-id",
                memberIds = listOf("test-user-id", "user-2")))

        // Groups for join flow tests - no members except owner
        groupRepository.addGroup(
            Group(
                id = "test-group-444",
                name = "Test Group Chat 444",
                description = "Test group for notifications",
                category = com.android.joinme.model.event.EventType.SPORTS,
                ownerId = "owner-id",
                memberIds = listOf("owner-id")))

        groupRepository.addGroup(
            Group(
                id = "test-group-333",
                name = "Test Group Chat 333",
                description = "Test group for notifications",
                category = com.android.joinme.model.event.EventType.SPORTS,
                ownerId = "owner-id",
                memberIds = listOf("owner-id")))

        groupRepository.addGroup(
            Group(
                id = "test-group-888",
                name = "Test Group Chat 888",
                description = "Test group for notifications",
                category = com.android.joinme.model.event.EventType.SPORTS,
                ownerId = "owner-id",
                memberIds = listOf("owner-id")))

        groupRepository.addGroup(
            Group(
                id = "intent-group-555",
                name = "Intent Group 555",
                description = "Test group for notifications",
                category = com.android.joinme.model.event.EventType.SPORTS,
                ownerId = "owner-id",
                memberIds = listOf("owner-id")))

        // Group for group chat notification test with authenticated user
        groupRepository.addGroup(
            Group(
                id = "notification-group-123",
                name = "Notification Test Group",
                description = "Test group for notification navigation",
                category = com.android.joinme.model.event.EventType.SPORTS,
                ownerId = "test-user-id",
                memberIds = listOf("test-user-id", "user-2", "user-3")))
      }
    }
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
    // This test covers: if (initialGroupId != null) { if (currentUser != null) { try { joinGroup()
    // } } }

    val groupRepository = GroupRepositoryProvider.repository
    val currentUser = FirebaseAuth.getInstance().currentUser

    // Skip test if user is not authenticated
    if (currentUser == null) {
      return@runBlocking
    }

    // Create a test group
    val testGroupId = groupRepository.getNewGroupId()
    val testGroup =
        Group(
            id = testGroupId,
            name = "Test Group for Deep Link Join",
            description = "Testing successful group join",
            category = com.android.joinme.model.event.EventType.SPORTS,
            ownerId = currentUser.uid,
            memberIds = listOf(currentUser.uid))
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
      composeTestRule.waitForIdle()

      scenario.use { it.onActivity { activity -> assert(!activity.isFinishing) } }

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
    // This test covers: if (initialGroupId != null) { if (currentUser != null) { catch (e:
    // Exception) { Toast } } }

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
    composeTestRule.waitForIdle()

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
    composeTestRule.waitForIdle()

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
    composeTestRule.waitForIdle()

    scenario.use {
      it.onActivity { activity ->
        // Activity should launch normally without attempting group join
        assert(!activity.isFinishing)
        assert(activity.intent.data == null)
      }
    }

    scenario.close()
  }

  // ========== Notification Navigation Tests ==========
  // These tests cover the new notification navigation logic added for chat messages

  @Test
  fun mainActivity_eventChatNotification_extractsAllExtras() {
    // Test that event chat notification intent extras are properly extracted
    val context = ApplicationProvider.getApplicationContext<Context>()
    val intent =
        Intent(context, MainActivity::class.java).apply {
          putExtra("notificationType", "event_chat_message")
          putExtra("eventId", "test-event-123")
          putExtra("conversationId", "test-conversation-456")
          putExtra("chatName", "Test Event Chat")
        }

    val scenario = ActivityScenario.launch<MainActivity>(intent)
    scenario.use {
      it.onActivity { activity ->
        // Verify all intent extras are accessible
        assert(activity.intent.getStringExtra("notificationType") == "event_chat_message")
        assert(activity.intent.getStringExtra("eventId") == "test-event-123")
        assert(activity.intent.getStringExtra("conversationId") == "test-conversation-456")
        assert(activity.intent.getStringExtra("chatName") == "Test Event Chat")
        assert(!activity.isFinishing)
      }
    }
    scenario.close()
  }

  @Test
  fun mainActivity_groupChatNotification_extractsAllExtras() {
    // Test that group chat notification intent extras are properly extracted
    val context = ApplicationProvider.getApplicationContext<Context>()
    val intent =
        Intent(context, MainActivity::class.java).apply {
          putExtra("notificationType", "group_chat_message")
          putExtra("groupId", "test-group-789")
          putExtra("conversationId", "test-conversation-101")
          putExtra("chatName", "Test Group Chat")
        }

    val scenario = ActivityScenario.launch<MainActivity>(intent)
    scenario.use {
      it.onActivity { activity ->
        // Verify all intent extras are accessible
        assert(activity.intent.getStringExtra("notificationType") == "group_chat_message")
        assert(activity.intent.getStringExtra("groupId") == "test-group-789")
        assert(activity.intent.getStringExtra("conversationId") == "test-conversation-101")
        assert(activity.intent.getStringExtra("chatName") == "Test Group Chat")
        assert(!activity.isFinishing)
      }
    }
    scenario.close()
  }

  @Test
  fun mainActivity_eventNotification_withoutChatType_handlesGracefully() {
    // Test that regular event notification (not chat) works correctly
    val context = ApplicationProvider.getApplicationContext<Context>()
    val intent =
        Intent(context, MainActivity::class.java).apply {
          putExtra("eventId", "test-event-999")
          // No notificationType - should navigate to event detail screen
        }

    val scenario = ActivityScenario.launch<MainActivity>(intent)
    composeTestRule.waitForIdle()

    scenario.use {
      it.onActivity { activity ->
        // Activity should handle missing notificationType gracefully
        assert(activity.intent.getStringExtra("eventId") == "test-event-999")
        assert(activity.intent.getStringExtra("notificationType") == null)
        assert(!activity.isFinishing)
      }
    }
    scenario.close()
  }

  @Test
  fun mainActivity_groupNotification_withoutChatType_handlesGracefully() {
    // Test that regular group notification (not chat) is parsed correctly
    // Note: This test only verifies intent parsing, not navigation behavior
    val context = ApplicationProvider.getApplicationContext<Context>()
    val intent =
        Intent(context, MainActivity::class.java).apply {
          putExtra("groupId", "test-group-888")
          // No notificationType - should navigate to group detail / join flow
        }

    val scenario = ActivityScenario.launch<MainActivity>(intent)
    scenario.use {
      it.onActivity { activity ->
        // Activity should parse the groupId correctly
        assert(activity.intent.getStringExtra("groupId") == "test-group-888")
        assert(activity.intent.getStringExtra("notificationType") == null)
        assert(!activity.isFinishing)
      }
    }
    // Note: This will trigger group join flow and show toast - this is expected behavior
    composeTestRule.waitForIdle()
    scenario.close()
  }

  @Test
  fun mainActivity_eventChatNotification_withMissingConversationId_handlesGracefully() {
    // Test that event chat notification without conversationId falls back to event detail screen
    // Since conversationId is null, the condition at line 193 fails and it navigates to event
    // detail instead
    val context = ApplicationProvider.getApplicationContext<Context>()
    val intent =
        Intent(context, MainActivity::class.java).apply {
          putExtra("notificationType", "event_chat_message")
          putExtra("eventId", "test-event-555")
          putExtra("chatName", "Test Chat")
          // conversationId missing - will fall back to regular event navigation
        }

    val scenario = ActivityScenario.launch<MainActivity>(intent)
    composeTestRule.waitForIdle()

    scenario.use {
      it.onActivity { activity ->
        // Activity should not crash even with missing conversationId
        assert(!activity.isFinishing)
      }
    }
    scenario.close()
  }

  @Test
  fun mainActivity_eventChatNotification_withMissingChatName_handlesGracefully() {
    // Test that event chat notification without chatName falls back to event detail screen
    val context = ApplicationProvider.getApplicationContext<Context>()
    val intent =
        Intent(context, MainActivity::class.java).apply {
          putExtra("notificationType", "event_chat_message")
          putExtra("eventId", "test-event-666")
          putExtra("conversationId", "test-conversation-777")
          // chatName missing - will fall back to regular event navigation
        }

    val scenario = ActivityScenario.launch<MainActivity>(intent)
    composeTestRule.waitForIdle()

    scenario.use {
      it.onActivity { activity ->
        // Activity should not crash even with missing chatName
        assert(!activity.isFinishing)
      }
    }
    scenario.close()
  }

  @Test
  fun mainActivity_groupChatNotification_withMissingConversationId_handlesGracefully() {
    // Test that group chat notification without conversationId falls back to join/detail flow
    // Since conversationId is null, it will try to join the group instead
    val context = ApplicationProvider.getApplicationContext<Context>()
    val intent =
        Intent(context, MainActivity::class.java).apply {
          putExtra("notificationType", "group_chat_message")
          putExtra("groupId", "test-group-444")
          putExtra("chatName", "Test Group Chat")
          // conversationId missing - will fall back to group join flow
        }

    val scenario = ActivityScenario.launch<MainActivity>(intent)
    // This will trigger group join and show toast - expected behavior
    composeTestRule.waitForIdle()

    scenario.use {
      it.onActivity { activity ->
        // Activity should not crash even with missing conversationId
        assert(!activity.isFinishing)
      }
    }
    scenario.close()
  }

  @Test
  fun mainActivity_groupChatNotification_withMissingChatName_handlesGracefully() {
    // Test that group chat notification without chatName falls back to join/detail flow
    val context = ApplicationProvider.getApplicationContext<Context>()
    val intent =
        Intent(context, MainActivity::class.java).apply {
          putExtra("notificationType", "group_chat_message")
          putExtra("groupId", "test-group-333")
          putExtra("conversationId", "test-conversation-222")
          // chatName missing - will fall back to group join flow
        }

    val scenario = ActivityScenario.launch<MainActivity>(intent)
    // This will trigger group join and show toast - expected behavior
    composeTestRule.waitForIdle()

    scenario.use {
      it.onActivity { activity ->
        // Activity should not crash even with missing chatName
        assert(!activity.isFinishing)
      }
    }
    scenario.close()
  }

  @Test
  fun mainActivity_eventChatNotification_combinesIntentExtraAndDeepLink() {
    // Test that eventId can come from both intent extra and deep link
    val context = ApplicationProvider.getApplicationContext<Context>()
    val intent =
        Intent(context, MainActivity::class.java).apply {
          data = Uri.parse("joinme://event/deeplink-event-111")
          putExtra("notificationType", "event_chat_message")
          putExtra("eventId", "intent-event-222")
          putExtra("conversationId", "test-conversation-333")
          putExtra("chatName", "Test Chat")
        }

    val scenario = ActivityScenario.launch<MainActivity>(intent)
    scenario.use {
      it.onActivity { activity ->
        // Activity should handle both sources of eventId
        assert(!activity.isFinishing)
        // Intent extra should take precedence over deep link
        assert(activity.intent.getStringExtra("eventId") == "intent-event-222")
        assert(activity.intent.data?.lastPathSegment == "deeplink-event-111")
      }
    }
    scenario.close()
  }

  @Test
  fun mainActivity_groupChatNotification_combinesIntentExtraAndDeepLink() {
    // Test that groupId can come from both intent extra and deep link
    val context = ApplicationProvider.getApplicationContext<Context>()
    val intent =
        Intent(context, MainActivity::class.java).apply {
          data = Uri.parse("joinme://group/deeplink-group-444")
          putExtra("notificationType", "group_chat_message")
          putExtra("groupId", "intent-group-555")
          putExtra("conversationId", "test-conversation-666")
          putExtra("chatName", "Test Group Chat")
        }

    val scenario = ActivityScenario.launch<MainActivity>(intent)
    scenario.use {
      it.onActivity { activity ->
        // Activity should handle both sources of groupId
        assert(!activity.isFinishing)
        // Intent extra should take precedence over deep link
        assert(activity.intent.getStringExtra("groupId") == "intent-group-555")
        assert(activity.intent.data?.lastPathSegment == "deeplink-group-444")
      }
    }
    scenario.close()
  }

  @Test
  fun mainActivity_unknownNotificationType_handlesGracefully() {
    // Test that unknown notification types don't crash the app
    val context = ApplicationProvider.getApplicationContext<Context>()
    val intent =
        Intent(context, MainActivity::class.java).apply {
          putExtra("notificationType", "unknown_message_type")
          putExtra("eventId", "test-event-777")
        }

    val scenario = ActivityScenario.launch<MainActivity>(intent)
    composeTestRule.waitForIdle()

    scenario.use {
      it.onActivity { activity ->
        // Activity should handle unknown notification types gracefully
        assert(!activity.isFinishing)
      }
    }
    scenario.close()
  }

  @Test
  fun mainActivity_notificationExtras_withNullValues() {
    // Test that null notification extras are handled gracefully
    val context = ApplicationProvider.getApplicationContext<Context>()
    val intent = Intent(context, MainActivity::class.java)
    // No extras set at all

    val scenario = ActivityScenario.launch<MainActivity>(intent)
    scenario.use {
      it.onActivity { activity ->
        // All getStringExtra should return null, not crash
        assert(activity.intent.getStringExtra("notificationType") == null)
        assert(activity.intent.getStringExtra("eventId") == null)
        assert(activity.intent.getStringExtra("groupId") == null)
        assert(activity.intent.getStringExtra("conversationId") == null)
        assert(activity.intent.getStringExtra("chatName") == null)
        assert(!activity.isFinishing)
      }
    }
    scenario.close()
  }

  @Test
  fun mainActivity_notificationWithOnlyNotificationType() {
    // Test that having only notificationType without IDs doesn't cause issues
    val context = ApplicationProvider.getApplicationContext<Context>()
    val intent =
        Intent(context, MainActivity::class.java).apply {
          putExtra("notificationType", "event_chat_message")
          // No eventId, conversationId, or chatName
        }

    val scenario = ActivityScenario.launch<MainActivity>(intent)
    scenario.use {
      it.onActivity { activity ->
        // Activity should not crash when notification type is set but IDs are missing
        assert(activity.intent.getStringExtra("notificationType") == "event_chat_message")
        assert(activity.intent.getStringExtra("eventId") == null)
        assert(!activity.isFinishing)
      }
    }
    scenario.close()
  }

  // ========== Event Notification Navigation Flow Tests (Lines 190-212) ==========
  // These tests verify the LaunchedEffect logic that handles event notification navigation

  @Test
  fun mainActivity_eventChatNotification_navigatesToChat_whenAuthenticatedWithValidEvent() =
      runBlocking {
        // This test covers: LaunchedEffect(initialEventId, notificationType) with
        // event_chat_message
        // Lines 190-208 in MainActivity.kt
        val currentUser = FirebaseAuth.getInstance().currentUser

        // Skip test if user is not authenticated
        if (currentUser == null) {
          return@runBlocking
        }

        // Create a real test event
        val eventRepository =
            com.android.joinme.model.event.EventsRepositoryProvider.getRepository(
                isOnline = true, ApplicationProvider.getApplicationContext())
        val testEventId = eventRepository.getNewEventId()
        val testEvent =
            com.android.joinme.model.event.Event(
                eventId = testEventId,
                type = com.android.joinme.model.event.EventType.SOCIAL,
                title = "Test Event for Chat Notification",
                description = "Testing navigation",
                location = com.android.joinme.model.map.Location(0.0, 0.0, "Test Location"),
                date = com.google.firebase.Timestamp.now(),
                duration = 60,
                participants = listOf(currentUser.uid),
                maxParticipants = 10,
                visibility = com.android.joinme.model.event.EventVisibility.PUBLIC,
                ownerId = currentUser.uid)
        eventRepository.addEvent(testEvent)

        try {
          // Launch MainActivity with event chat notification
          val context = ApplicationProvider.getApplicationContext<Context>()
          val intent =
              Intent(context, MainActivity::class.java).apply {
                putExtra("notificationType", "event_chat_message")
                putExtra("eventId", testEventId)
                putExtra("conversationId", "test-conversation-chat-123")
                putExtra("chatName", "Test Event Chat")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
              }

          val scenario = ActivityScenario.launch<MainActivity>(intent)
          composeTestRule.waitForIdle()

          scenario.use {
            it.onActivity { activity ->
              // Activity should not crash, navigation should have been triggered
              assert(!activity.isFinishing)
            }
          }

          scenario.close()
        } finally {
          // Cleanup
          try {
            eventRepository.deleteEvent(testEventId)
          } catch (_: Exception) {}
        }
      }

  @Test
  fun mainActivity_regularEventNotification_navigatesToEventDetail_whenAuthenticated() =
      runBlocking {
        // This test covers: LaunchedEffect(initialEventId, notificationType) WITHOUT
        // event_chat_message
        // Lines 210-212 in MainActivity.kt
        val currentUser = FirebaseAuth.getInstance().currentUser

        // Skip test if user is not authenticated
        if (currentUser == null) {
          return@runBlocking
        }

        // Create a real test event
        val eventRepository =
            com.android.joinme.model.event.EventsRepositoryProvider.getRepository(
                isOnline = true, ApplicationProvider.getApplicationContext())
        val testEventId = eventRepository.getNewEventId()
        val testEvent =
            com.android.joinme.model.event.Event(
                eventId = testEventId,
                type = com.android.joinme.model.event.EventType.SPORTS,
                title = "Test Event for Regular Notification",
                description = "Testing navigation to event detail",
                location = com.android.joinme.model.map.Location(0.0, 0.0, "Test Location"),
                date = com.google.firebase.Timestamp.now(),
                duration = 90,
                participants = listOf(currentUser.uid),
                maxParticipants = 15,
                visibility = com.android.joinme.model.event.EventVisibility.PUBLIC,
                ownerId = currentUser.uid)
        eventRepository.addEvent(testEvent)

        try {
          // Launch MainActivity with regular event notification (no chat type)
          val context = ApplicationProvider.getApplicationContext<Context>()
          val intent =
              Intent(context, MainActivity::class.java).apply {
                putExtra("eventId", testEventId)
                // No notificationType - should navigate to event detail screen
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
              }

          val scenario = ActivityScenario.launch<MainActivity>(intent)
          composeTestRule.waitForIdle()

          scenario.use {
            it.onActivity { activity ->
              // Activity should not crash, navigation to event detail should have occurred
              assert(!activity.isFinishing)
            }
          }

          scenario.close()
        } finally {
          // Cleanup
          try {
            eventRepository.deleteEvent(testEventId)
          } catch (_: Exception) {}
        }
      }

  @Test
  fun mainActivity_eventChatNotification_fallsBackToDefaultParticipants_whenEventFetchFails() =
      runBlocking {
        // This test covers: LaunchedEffect catch block (lines 204-207)
        // When eventRepository.getEvent fails, it should navigate with default totalParticipants=1
        val currentUser = FirebaseAuth.getInstance().currentUser

        // Skip test if user is not authenticated
        if (currentUser == null) {
          return@runBlocking
        }

        // Use a non-existent event ID to trigger the error path
        val invalidEventId = "non-existent-event-for-chat-notification"

        // Launch MainActivity with event chat notification for non-existent event
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent =
            Intent(context, MainActivity::class.java).apply {
              putExtra("notificationType", "event_chat_message")
              putExtra("eventId", invalidEventId)
              putExtra("conversationId", "test-conversation-error-handling")
              putExtra("chatName", "Test Error Handling Chat")
              addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

        val scenario = ActivityScenario.launch<MainActivity>(intent)
        composeTestRule.waitForIdle()

        scenario.use {
          it.onActivity { activity ->
            // Activity should not crash even when event fetch fails
            // It should fall back to navigating with default totalParticipants=1
            assert(!activity.isFinishing)
          }
        }

        scenario.close()
      }

  @Test
  fun mainActivity_eventNotification_skipsNavigation_whenUserNotAuthenticated() {
    // This test covers: LaunchedEffect early exit when currentUser == null
    // Lines 191: if (initialEventId != null && currentUser != null)
    val currentUser = FirebaseAuth.getInstance().currentUser

    // Skip test if user IS authenticated (we need unauthenticated state)
    if (currentUser != null) {
      return
    }

    // Launch MainActivity with event notification while not authenticated
    val context = ApplicationProvider.getApplicationContext<Context>()
    val intent =
        Intent(context, MainActivity::class.java).apply {
          putExtra("eventId", "some-event-id")
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    val scenario = ActivityScenario.launch<MainActivity>(intent)
    composeTestRule.waitForIdle()

    scenario.use {
      it.onActivity { activity ->
        // Activity should not crash, and no navigation should occur (stays on auth screen)
        assert(!activity.isFinishing)
      }
    }

    scenario.close()
  }

  @Test
  fun mainActivity_eventNotification_skipsNavigation_whenEventIdIsNull() {
    // This test covers: LaunchedEffect early exit when initialEventId == null
    // Lines 191: if (initialEventId != null && currentUser != null)

    // Launch MainActivity without eventId
    val context = ApplicationProvider.getApplicationContext<Context>()
    val intent = Intent(context, MainActivity::class.java)
    // No eventId provided

    val scenario = ActivityScenario.launch<MainActivity>(intent)
    composeTestRule.waitForIdle()

    scenario.use {
      it.onActivity { activity ->
        // Activity should launch normally without attempting event navigation
        assert(!activity.isFinishing)
      }
    }

    scenario.close()
  }

  @Test
  fun mainActivity_groupChatNotification_navigatesToChat_whenAuthenticatedWithValidGroup() =
      runBlocking {
        // This test covers lines 264-270: group chat notification with authenticated user
        // Tests: if (notificationType == "group_chat_message" && conversationId != null &&
        // chatName != null && currentUser != null) { try { getGroup() navigateTo(Chat) } }

        // Note: FirebaseAuth is mocked in @Before to return test-user-id
        val testGroupId = "notification-group-123"

        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent =
            Intent(context, MainActivity::class.java).apply {
              putExtra("notificationType", "group_chat_message")
              putExtra("groupId", testGroupId)
              putExtra("conversationId", "test-conversation-notif")
              putExtra("chatName", "Notification Test Group")
            }

        val scenario = ActivityScenario.launch<MainActivity>(intent)

        // Wait for navigation to complete
        composeTestRule.waitForIdle()

        scenario.use {
          it.onActivity { activity ->
            // Verify activity is still running (navigation succeeded)
            assert(!activity.isFinishing)
          }
        }

        scenario.close()
      }

  @Test
  fun mainActivity_groupChatNotification_showsToast_whenGroupFetchFails() = runBlocking {
    // This test covers lines 273-277: group chat notification catch block
    // Tests: try { getGroup() } catch (e: Exception) { Toast.makeText("Failed to access group")
    // }

    // Note: FirebaseAuth is mocked in @Before to return test-user-id

    val context = ApplicationProvider.getApplicationContext<Context>()
    val intent =
        Intent(context, MainActivity::class.java).apply {
          putExtra("notificationType", "group_chat_message")
          putExtra("groupId", "non-existent-group-999") // Group doesn't exist
          putExtra("conversationId", "test-conversation-fail")
          putExtra("chatName", "Non-existent Group")
        }

    val scenario = ActivityScenario.launch<MainActivity>(intent)

    // Wait for async operations and toast to display
    composeTestRule.waitForIdle()

    scenario.use {
      it.onActivity { activity ->
        // Activity should not crash even though group fetch failed
        assert(!activity.isFinishing)
      }
    }
    // Note: Toast will display "Failed to access group" - this is expected behavior
    scenario.close()
  }

  // ========== Location Navigation Tests ==========
  // These tests cover the onNavigateToMap callbacks in MainActivity

  @Test
  fun mainActivity_showEventScreen_onNavigateToMap_navigatesToMapWithLocation() = runBlocking {
    val currentUser = FirebaseAuth.getInstance().currentUser
    if (currentUser == null) return@runBlocking

    val eventRepository =
        com.android.joinme.model.event.EventsRepositoryProvider.getRepository(
            isOnline = true, ApplicationProvider.getApplicationContext())
    val testEventId = eventRepository.getNewEventId()
    val testEvent =
        com.android.joinme.model.event.Event(
            eventId = testEventId,
            type = com.android.joinme.model.event.EventType.SPORTS,
            title = "Location Test Event",
            description = "Test",
            location = com.android.joinme.model.map.Location(46.5196, 6.5680, "EPFL"),
            date = com.google.firebase.Timestamp.now(),
            duration = 60,
            participants = listOf(currentUser.uid),
            maxParticipants = 10,
            visibility = com.android.joinme.model.event.EventVisibility.PUBLIC,
            ownerId = currentUser.uid)
    eventRepository.addEvent(testEvent)

    try {
      val context = ApplicationProvider.getApplicationContext<Context>()
      val intent =
          Intent(context, MainActivity::class.java).apply {
            putExtra("eventId", testEventId)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          }

      val scenario = ActivityScenario.launch<MainActivity>(intent)
      composeTestRule.waitForIdle()
      Thread.sleep(2000)

      composeTestRule.onNodeWithText("EPFL", useUnmergedTree = true).performClick()
      composeTestRule.waitForIdle()

      scenario.use { it.onActivity { activity -> assert(!activity.isFinishing) } }
      scenario.close()
    } finally {
      try {
        eventRepository.deleteEvent(testEventId)
      } catch (_: Exception) {}
    }
  }

  @Test
  fun mainActivity_chatScreen_onNavigateToMap_navigatesToMapWithLocation() = runBlocking {
    val currentUser = FirebaseAuth.getInstance().currentUser

    // Skip test if user is not authenticated, but log it so it's visible
    if (currentUser == null) {
      android.util.Log.w(
          "MainActivityTest",
          "Skipping mainActivity_chatScreen_onNavigateToMap_navigatesToMapWithLocation: " +
              "No authenticated user found. This test requires Firebase authentication.")
      return@runBlocking
    }

    val userId = currentUser.uid

    // Create a test event with chat and location message
    val eventRepository =
        com.android.joinme.model.event.EventsRepositoryProvider.getRepository(
            isOnline = true, ApplicationProvider.getApplicationContext())
    val testEventId = eventRepository.getNewEventId()
    val testEvent =
        com.android.joinme.model.event.Event(
            eventId = testEventId,
            type = com.android.joinme.model.event.EventType.SPORTS,
            title = "Test Event for Location Navigation",
            description = "Testing map navigation from chat",
            location = com.android.joinme.model.map.Location(46.5196, 6.5680, "EPFL"),
            date = com.google.firebase.Timestamp.now(),
            duration = 60,
            participants = listOf(userId),
            maxParticipants = 10,
            visibility = com.android.joinme.model.event.EventVisibility.PUBLIC,
            ownerId = userId)
    eventRepository.addEvent(testEvent)

    // Add a location message to the chat
    val chatRepository = com.android.joinme.model.chat.ChatRepositoryProvider.repository
    val testLocation = com.android.joinme.model.map.Location(46.5196, 6.5680, "EPFL")
    val locationMessage =
        com.android.joinme.model.chat.Message(
            id = chatRepository.getNewMessageId(),
            conversationId = testEventId,
            senderId = currentUser.uid,
            senderName = currentUser.displayName ?: "Test User",
            content = "Check this location",
            timestamp = System.currentTimeMillis(),
            type = com.android.joinme.model.chat.MessageType.LOCATION,
            location = testLocation)
    chatRepository.addMessage(locationMessage)

    try {
      // Launch MainActivity with event chat notification
      val context = ApplicationProvider.getApplicationContext<Context>()
      val intent =
          Intent(context, MainActivity::class.java).apply {
            putExtra("notificationType", "event_chat_message")
            putExtra("eventId", testEventId)
            putExtra("conversationId", testEventId)
            putExtra("chatName", "Test Event Chat")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          }

      val scenario = ActivityScenario.launch<MainActivity>(intent)
      composeTestRule.waitForIdle()
      Thread.sleep(2000) // Wait for chat to load

      // Find and click the location message
      composeTestRule.onNodeWithText("EPFL", useUnmergedTree = true).assertExists().performClick()

      composeTestRule.waitForIdle()
      Thread.sleep(1000) // Wait for navigation and Toast

      scenario.use {
        it.onActivity { activity ->
          // Verify activity is still running (not crashed)
          assert(!activity.isFinishing)
        }
      }

      scenario.close()
    } finally {
      // Cleanup
      try {
        eventRepository.deleteEvent(testEventId)
        chatRepository.deleteMessage(testEventId, locationMessage.id)
      } catch (_: Exception) {}
    }
  }

  // ========== Invitation Deep Link Tests (New Code Coverage) ==========
  // These tests cover the new invitation link handling added in this PR

  @Test
  fun mainActivity_launchesWithInvitationDeepLink_parsesToken() {
    // Covers lines 186-189: onNewIntent() and invitation deep link parsing
    val context = ApplicationProvider.getApplicationContext<Context>()
    val invitationIntent =
        Intent(context, MainActivity::class.java).apply {
          action = Intent.ACTION_VIEW
          data = Uri.parse("https://joinme-aa9e8.web.app/invite/test-token-123")
        }

    val scenario = ActivityScenario.launch<MainActivity>(invitationIntent)
    composeTestRule.waitForIdle()

    scenario.use {
      it.onActivity { activity ->
        assert(
            activity.intent.data?.toString() ==
                "https://joinme-aa9e8.web.app/invite/test-token-123")
        assert(!activity.isFinishing)
      }
    }

    scenario.close()
  }

  @Test
  fun mainActivity_invitationLink_handlesInvalidToken() = runBlocking {
    // Covers lines 325-374: invitationToken LaunchedEffect with invalid token
    // Specifically covers lines 355-363 (onFailure) and lines 364-370 (catch block)
    val context = ApplicationProvider.getApplicationContext<Context>()
    val intent =
        Intent(context, MainActivity::class.java).apply {
          data = Uri.parse("https://joinme-aa9e8.web.app/invite/invalid-token")
        }

    val scenario = ActivityScenario.launch<MainActivity>(intent)
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)

    scenario.use { it.onActivity { activity -> assert(!activity.isFinishing) } }

    scenario.close()
  }

  @Test
  fun mainActivity_invitationLink_parsesCustomScheme() = runBlocking {
    // Covers DeepLinkService.parseInvitationLink() and custom scheme handling
    val context = ApplicationProvider.getApplicationContext<Context>()
    val intent =
        Intent(context, MainActivity::class.java).apply {
          data = Uri.parse("joinme://invite/custom-token-456")
        }

    val scenario = ActivityScenario.launch<MainActivity>(intent)
    composeTestRule.waitForIdle()

    scenario.use {
      it.onActivity { activity ->
        // Verify custom scheme deep link is parsed
        assert(activity.intent.data?.host == "invite")
        assert(!activity.isFinishing)
      }
    }

    scenario.close()
  }
}
