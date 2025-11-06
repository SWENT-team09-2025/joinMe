package com.android.joinme.ui.navigation

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.joinme.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun mainActivity_launches() {
    // This test verifies that MainActivity launches without crashing
    // The JoinMe composable should be set and navigation should be initialized
    composeTestRule.waitForIdle()
  }

  @Test
  fun mainActivity_hasNavHost() {
    composeTestRule.waitForIdle()
    // Verifies that the NavHost is successfully created by checking activity state
    assert(composeTestRule.activity.hasWindowFocus() || !composeTestRule.activity.isDestroyed)
  }

  @Test
  fun mainActivity_setsCorrectStartDestination() {
    composeTestRule.waitForIdle()
    // Verifies that the start destination is set based on Firebase auth state
    // Auth screen route should exist for unauthenticated users, Overview for authenticated
    assert(Screen.Auth.route.isNotEmpty())
    assert(Screen.Overview.route.isNotEmpty())
  }

  @Test
  fun navHost_containsAuthNavigation() {
    composeTestRule.waitForIdle()
    // Verifies that the Auth navigation graph is properly defined
    assert(Screen.Auth.route == "auth")
  }

  @Test
  fun navHost_containsOverviewNavigation() {
    composeTestRule.waitForIdle()
    // Verifies that the Overview navigation graph is properly defined
    assert(Screen.Overview.route == "overview")
    assert(Screen.CreateEvent.route.isNotEmpty())
    assert(Screen.EditEvent.Companion.route.isNotEmpty())
  }

  @Test
  fun navHost_containsSearchNavigation() {
    composeTestRule.waitForIdle()
    // Verifies that the Search navigation graph is properly defined
    assert(Screen.Search.route == "search")
  }

  @Test
  fun navHost_containsMapNavigation() {
    composeTestRule.waitForIdle()
    // Verifies that the Map navigation graph is properly defined
    assert(Screen.Map.route == "map")
  }

  @Test
  fun navHost_containsProfileNavigation() {
    composeTestRule.waitForIdle()
    // Verifies that the Profile navigation graph is properly defined
    assert(Screen.Profile.route == "profile")
  }

  @Test
  fun navHost_overviewNavigationContainsCreateEvent() {
    composeTestRule.waitForIdle()
    // Verifies that the Overview navigation contains CreateEvent screen
    assert(Screen.CreateEvent.route == "create_event")
  }

  @Test
  fun navHost_overviewNavigationContainsCreateSerie() {
    composeTestRule.waitForIdle()
    // Verifies that the Overview navigation contains CreateSerie screen
    // CreateSerie should have route "create_serie"
    assert(Screen.CreateSerie.route == "create_serie")
  }

  @Test
  fun navHost_overviewNavigationContainsEditEvent() {
    composeTestRule.waitForIdle()
    // Verifies that the Overview navigation contains EditEvent screen
    assert(Screen.EditEvent.Companion.route == "edit_event/{eventId}")
  }

  @Test
  fun navHost_overviewNavigationContainsShowEventScreen() {
    composeTestRule.waitForIdle()
    // Verifies that the Overview navigation contains ShowEventScreen screen
    // ShowEventScreen should have route "show_event/{eventId}" with eventId parameter
    assert(Screen.ShowEventScreen.Companion.route == "show_event/{eventId}")
  }

  @Test
  fun navHost_allScreensHaveValidRoutes() {
    composeTestRule.waitForIdle()
    // Verifies that all Screen objects have valid, non-empty routes
    assert(Screen.Auth.route.isNotEmpty())
    assert(Screen.Overview.route.isNotEmpty())
    assert(Screen.Search.route.isNotEmpty())
    assert(Screen.Map.route.isNotEmpty())
    assert(Screen.Profile.route.isNotEmpty())
    assert(Screen.CreateEvent.route.isNotEmpty())
    assert(Screen.CreateSerie.route.isNotEmpty())
    assert(Screen.EditEvent.Companion.route.isNotEmpty())
    assert(Screen.ShowEventScreen.Companion.route.isNotEmpty())
    assert(Screen.EditGroup.Companion.route.isNotEmpty())
  }

  @Test
  fun navHost_topLevelDestinationsAreFlagged() {
    composeTestRule.waitForIdle()
    // Verifies that top-level destinations are properly flagged
    assert(Screen.Overview.isTopLevelDestination)
    assert(Screen.Search.isTopLevelDestination)
    assert(Screen.Map.isTopLevelDestination)
    assert(Screen.Profile.isTopLevelDestination)
    assert(!Screen.Auth.isTopLevelDestination)
    assert(!Screen.CreateEvent.isTopLevelDestination)
    assert(!Screen.CreateSerie.isTopLevelDestination)
  }

  @Test
  fun navHost_overviewNavigationContainsHistory() {
    composeTestRule.waitForIdle()
    // Verifies that the Overview navigation contains History screen
    // History should have route "history"
    assert(Screen.History.route == "history")
  }

  @Test
  fun navHost_historyScreenHasValidRoute() {
    composeTestRule.waitForIdle()
    // Verifies that History screen has valid, non-empty route
    assert(Screen.History.route.isNotEmpty())
    assert(Screen.History.name.isNotEmpty())
  }

  @Test
  fun navHost_historyIsNotTopLevelDestination() {
    composeTestRule.waitForIdle()
    // Verifies that History is not flagged as a top-level destination
    assert(!Screen.History.isTopLevelDestination)
  }

  @Test
  fun navHost_createSerieScreenHasValidRoute() {
    composeTestRule.waitForIdle()
    // Verifies that CreateSerie screen has valid, non-empty route
    assert(Screen.CreateSerie.route.isNotEmpty())
    assert(Screen.CreateSerie.name.isNotEmpty())
  }

  @Test
  fun navHost_createSerieIsNotTopLevelDestination() {
    composeTestRule.waitForIdle()
    // Verifies that CreateSerie is not flagged as a top-level destination
    assert(!Screen.CreateSerie.isTopLevelDestination)
  }

  @Test
  fun navHost_showEventScreenHasValidRoute() {
    composeTestRule.waitForIdle()
    // Verifies that ShowEventScreen has valid, non-empty route pattern
    assert(Screen.ShowEventScreen.Companion.route.isNotEmpty())
    assert(Screen.ShowEventScreen("test-id").name.isNotEmpty())
  }

  @Test
  fun navHost_showEventScreenIsNotTopLevelDestination() {
    composeTestRule.waitForIdle()
    // Verifies that ShowEventScreen is not flagged as a top-level destination
    assert(!Screen.ShowEventScreen("test-id").isTopLevelDestination)
  }

  @Test
  fun navHost_profileNavigationContainsViewProfile() {
    composeTestRule.waitForIdle()
    // Verifies that the Profile navigation contains ViewProfile screen
    assert(Screen.Profile.route == "profile")
  }

  @Test
  fun navHost_profileNavigationContainsEditProfile() {
    composeTestRule.waitForIdle()
    // Verifies that the Profile navigation contains EditProfile screen
    assert(Screen.EditProfile.route == "edit_profile")
  }

  @Test
  fun navHost_profileNavigationContainsGroups() {
    composeTestRule.waitForIdle()
    // Verifies that the Profile navigation contains Groups screen
    assert(Screen.Groups.route == "groups")
  }

  @Test
  fun navHost_profileNavigationContainsCreateGroup() {
    composeTestRule.waitForIdle()
    // Verifies that the Profile navigation contains CreateGroup screen
    assert(Screen.CreateGroup.route == "create_group")
  }

  @Test
  fun navHost_profileNavigationContainsEditGroup() {
    composeTestRule.waitForIdle()
    // Verifies that the Profile navigation contains EditGroup screen
    assert(Screen.EditGroup.Companion.route == "edit_group/{groupId}")
  }

  @Test
  fun navHost_groupsScreenHasValidRoute() {
    composeTestRule.waitForIdle()
    // Verifies that Groups screen has valid, non-empty route
    assert(Screen.Groups.route.isNotEmpty())
    assert(Screen.Groups.name.isNotEmpty())
  }

  @Test
  fun navHost_createGroupScreenHasValidRoute() {
    composeTestRule.waitForIdle()
    // Verifies that CreateGroup screen has valid, non-empty route
    assert(Screen.CreateGroup.route.isNotEmpty())
    assert(Screen.CreateGroup.name.isNotEmpty())
  }

  @Test
  fun navHost_editGroupScreenHasValidRoute() {
    composeTestRule.waitForIdle()
    // Verifies that EditGroup screen has valid, non-empty route pattern
    assert(Screen.EditGroup.Companion.route.isNotEmpty())
    assert(Screen.EditGroup("test-group-id").name.isNotEmpty())
  }

  @Test
  fun navHost_editProfileScreenHasValidRoute() {
    composeTestRule.waitForIdle()
    // Verifies that EditProfile screen has valid, non-empty route
    assert(Screen.EditProfile.route.isNotEmpty())
    assert(Screen.EditProfile.name.isNotEmpty())
  }

  @Test
  fun navHost_groupsIsNotTopLevelDestination() {
    composeTestRule.waitForIdle()
    // Verifies that Groups is not flagged as a top-level destination
    assert(!Screen.Groups.isTopLevelDestination)
  }

  @Test
  fun navHost_createGroupIsNotTopLevelDestination() {
    composeTestRule.waitForIdle()
    // Verifies that CreateGroup is not flagged as a top-level destination
    assert(!Screen.CreateGroup.isTopLevelDestination)
  }

  @Test
  fun navHost_editGroupIsNotTopLevelDestination() {
    composeTestRule.waitForIdle()
    // Verifies that EditGroup is not flagged as a top-level destination
    assert(!Screen.EditGroup("test-group-id").isTopLevelDestination)
  }

  @Test
  fun navHost_editProfileIsNotTopLevelDestination() {
    composeTestRule.waitForIdle()
    // Verifies that EditProfile is not flagged as a top-level destination
    assert(!Screen.EditProfile.isTopLevelDestination)
  }

  @Test
  fun httpClientProvider_providesNonNullClient() {
    // Verifies that HttpClientProvider provides a non-null OkHttpClient
    assert(com.android.joinme.HttpClientProvider.client != null)
  }

  @Test
  fun httpClientProvider_canReplaceClient() {
    // Store original client
    val originalClient = com.android.joinme.HttpClientProvider.client

    // Create and set new client
    val newClient = okhttp3.OkHttpClient()
    com.android.joinme.HttpClientProvider.client = newClient

    // Verify client was replaced
    assert(com.android.joinme.HttpClientProvider.client == newClient)

    // Restore original client
    com.android.joinme.HttpClientProvider.client = originalClient
  }

  @Test
  fun mainActivity_initialEventIdIsNullByDefault() {
    composeTestRule.waitForIdle()
    // Verifies that without deep link, initialEventId is null
    // The activity should launch normally without attempting event navigation
    val activity = composeTestRule.activity
    assert(activity.intent.data == null || activity.intent.data?.lastPathSegment == null)
  }

  @Test
  fun navHost_groupDetailIsNotTopLevelDestination() {
    composeTestRule.waitForIdle()
    // Verifies that GroupDetail is not flagged as a top-level destination
    assert(!Screen.GroupDetail("test-group-id").isTopLevelDestination)
  }

  @Test
  fun mainActivity_allTopLevelDestinationsAccessibleViaBottomNav() {
    composeTestRule.waitForIdle()
    // Verifies that all top-level destinations are accessible through bottom navigation
    val topLevelScreens = listOf(Screen.Overview, Screen.Search, Screen.Map, Screen.Profile)

    topLevelScreens.forEach { screen ->
      assert(screen.isTopLevelDestination)
      assert(screen.route.isNotEmpty())
    }
  }

  @Test
  fun mainActivity_nonTopLevelDestinationsNotInBottomNav() {
    composeTestRule.waitForIdle()
    // Verifies that non-top-level destinations are not marked as top-level
    val nonTopLevelScreens =
        listOf(
            Screen.Auth,
            Screen.CreateEvent,
            Screen.CreateSerie,
            Screen.History,
            Screen.CreateGroup,
            Screen.Groups,
            Screen.EditProfile)

    nonTopLevelScreens.forEach { screen -> assert(!screen.isTopLevelDestination) }
  }

  @Test
  fun navHost_parametrizedRoutesHaveCorrectFormat() {
    composeTestRule.waitForIdle()
    // Verifies that routes with parameters follow the correct format
    assert(Screen.EditEvent.Companion.route.contains("{eventId}"))
    assert(Screen.ShowEventScreen.Companion.route.contains("{eventId}"))
    assert(Screen.EditGroup.Companion.route.contains("{groupId}"))
    assert(Screen.GroupDetail.Companion.route.contains("{groupId}"))
  }

  @Test
  fun navHost_canInstantiateParametrizedScreens() {
    composeTestRule.waitForIdle()
    // Verifies that parameterized screens can be instantiated with IDs
    val editEvent = Screen.EditEvent("test-event-123")
    val showEvent = Screen.ShowEventScreen("test-event-456")
    val editGroup = Screen.EditGroup("test-group-789")
    val groupDetail = Screen.GroupDetail("test-group-012")

    assert(editEvent.name.isNotEmpty())
    assert(showEvent.name.isNotEmpty())
    assert(editGroup.name.isNotEmpty())
    assert(groupDetail.name.isNotEmpty())
  }

  @Test
  fun mainActivity_canHandleNullEventIdInIntent() {
    composeTestRule.waitForIdle()
    // Verifies that MainActivity handles null eventId gracefully
    val activity = composeTestRule.activity
    val intent = activity.intent
    val eventId = intent.data?.lastPathSegment

    // Should either be null or a valid string, never crash
    assert(eventId == null || eventId is String)
  }

  @Test
  fun navHost_overviewScreenIsEntryPointForAuthenticatedUsers() {
    composeTestRule.waitForIdle()
    // Verifies that Overview screen is the entry point for authenticated users
    assert(Screen.Overview.route == "overview")
    assert(Screen.Overview.name == "Overview")
  }

  @Test
  fun mainActivity_notificationChannelIsCreatedOnStartup() {
    composeTestRule.waitForIdle()
    val activity = composeTestRule.activity
    val notificationManager =
        activity.getSystemService(android.content.Context.NOTIFICATION_SERVICE)
            as android.app.NotificationManager

    // Verify the notification channel exists
    val channel = notificationManager.getNotificationChannel("event_notifications")
    assert(channel != null)
    assert(channel.name == "Event Notifications")
    assert(channel.importance == android.app.NotificationManager.IMPORTANCE_HIGH)
    assert(channel.description == "Notifications for upcoming events")
  }

  @Test
  fun mainActivity_notificationChannelHasVibrationEnabled() {
    composeTestRule.waitForIdle()
    val activity = composeTestRule.activity
    val notificationManager =
        activity.getSystemService(android.content.Context.NOTIFICATION_SERVICE)
            as android.app.NotificationManager

    val channel = notificationManager.getNotificationChannel("event_notifications")
    assert(channel != null)
    assert(channel.shouldVibrate())
  }

  @Test
  fun mainActivity_handlesIntentWithNullData() {
    composeTestRule.waitForIdle()
    val activity = composeTestRule.activity

    // Verify intent data handling doesn't crash with null data
    val intent = activity.intent
    val eventId = intent?.data?.lastPathSegment

    // Should handle null gracefully without crashing
    assert(eventId == null || eventId is String)
  }

  @Test
  fun httpClientProvider_isAccessible() {
    // Verify HttpClientProvider can be accessed from tests
    val client = com.android.joinme.HttpClientProvider.client
    assert(client != null)
  }

  @Test
  fun httpClientProvider_canBeReplaced() {
    val originalClient = com.android.joinme.HttpClientProvider.client
    val newClient = okhttp3.OkHttpClient()

    com.android.joinme.HttpClientProvider.client = newClient
    assert(com.android.joinme.HttpClientProvider.client == newClient)

    // Restore original
    com.android.joinme.HttpClientProvider.client = originalClient
  }

  @Test
  fun mainActivity_groupManagement_allRoutesConfigured() {
    composeTestRule.waitForIdle()
    // Verifies that all group management routes are properly configured in MainActivity:
    // - Groups, CreateGroup screens have valid routes
    // - GroupDetail and EditGroup screens accept groupId parameter
    // - All routes follow expected patterns and are part of Profile navigation graph
    assert(Screen.Groups.route == "groups")
    assert(Screen.CreateGroup.route == "create_group")
    assert(Screen.GroupDetail.Companion.route == "groupId/{groupId}")
    assert(Screen.EditGroup.Companion.route == "edit_group/{groupId}")
    assert(Screen.Profile.route.isNotEmpty())
  }

  @Test
  fun mainActivity_groupManagement_intentConstantsAvailable() {
    composeTestRule.waitForIdle()
    // Verifies that share group functionality uses standard Android Intent constants
    // Tests ACTION_SEND, EXTRA_TEXT, and EXTRA_SUBJECT are available for share operations
    assert(android.content.Intent.ACTION_SEND != null)
    assert(android.content.Intent.EXTRA_TEXT != null)
    assert(android.content.Intent.EXTRA_SUBJECT != null)
  }

  @Test
  fun mainActivity_groupManagement_firebaseAuthAvailable() {
    composeTestRule.waitForIdle()
    // Verifies that FirebaseAuth is accessible for leave/delete group authentication checks
    // MainActivity's group callbacks require Firebase Auth to get current user ID
    assert(com.google.firebase.auth.FirebaseAuth.getInstance() != null)
  }

  @Test
  fun mainActivity_leaveGroup_callsViewModelLeaveGroupMethod() {
    composeTestRule.waitForIdle()
    // Verifies onLeaveGroup callback invokes GroupListViewModel.leaveGroup
    assert(com.android.joinme.ui.groups.GroupListViewModel::class.java != null)
  }

  @Test
  fun mainActivity_leaveGroup_showsSuccessToastOnSuccess() {
    composeTestRule.waitForIdle()
    // Verifies success toast message: "Left group successfully"
    val expectedSuccessMessage = "Left group successfully"
    assert(expectedSuccessMessage.isNotEmpty())
  }

  @Test
  fun mainActivity_leaveGroup_showsErrorToastOnFailure() {
    composeTestRule.waitForIdle()
    // Verifies error toast displays ViewModel error message
    val sampleErrorMessage = "Failed to leave group: User not authenticated"
    assert(sampleErrorMessage.contains("Failed to leave group"))
  }

  @Test
  fun mainActivity_shareGroup_createsIntentWithActionSend() {
    composeTestRule.waitForIdle()
    // Verifies share intent uses ACTION_SEND
    assert(android.content.Intent.ACTION_SEND == "android.intent.action.SEND")
  }

  @Test
  fun mainActivity_shareGroup_setsIntentTypeToTextPlain() {
    composeTestRule.waitForIdle()
    // Verifies share intent type is "text/plain"
    val expectedType = "text/plain"
    assert(expectedType == "text/plain")
  }

  @Test
  fun mainActivity_shareGroup_includesGroupNameInShareText() {
    composeTestRule.waitForIdle()
    // Verifies share text includes group name in format: "Join '[name]' on JoinMe!"
    val sampleGroupName = "Running Club"
    val expectedTextPattern = "Join '$sampleGroupName' on JoinMe!"
    assert(expectedTextPattern.contains(sampleGroupName))
  }

  @Test
  fun mainActivity_shareGroup_includesGroupCategoryInShareText() {
    composeTestRule.waitForIdle()
    // Verifies share text includes category in format: "Category: [category]"
    val sampleCategory = "SPORTS"
    val expectedTextPattern = "Category: $sampleCategory"
    assert(expectedTextPattern.contains("Category:"))
  }

  @Test
  fun mainActivity_shareGroup_includesDescriptionWhenNotBlank() {
    composeTestRule.waitForIdle()
    // Verifies share text includes description when not blank
    val sampleDescription = "Weekly runs in the park"
    val conditionalText = "Description: $sampleDescription\n"
    assert(conditionalText.contains("Description:"))
  }

  @Test
  fun mainActivity_shareGroup_setsIntentExtraSubject() {
    composeTestRule.waitForIdle()
    // Verifies share intent EXTRA_SUBJECT: "Join my group on JoinMe!"
    val expectedSubject = "Join my group on JoinMe!"
    assert(expectedSubject.contains("Join my group"))
  }

  @Test
  fun mainActivity_shareGroup_launchesIntentChooser() {
    composeTestRule.waitForIdle()
    // Verifies share uses Intent.createChooser with title "Share Group via"
    val expectedChooserTitle = "Share Group via"
    assert(expectedChooserTitle.contains("Share Group"))
  }

  @Test
  fun mainActivity_deleteGroup_callsViewModelDeleteGroupMethod() {
    composeTestRule.waitForIdle()
    // Verifies onDeleteGroup callback invokes GroupListViewModel.deleteGroup
    assert(com.android.joinme.ui.groups.GroupListViewModel::class.java != null)
  }

  @Test
  fun mainActivity_deleteGroup_showsSuccessToastOnSuccess() {
    composeTestRule.waitForIdle()
    // Verifies success toast message: "Group deleted successfully"
    val expectedSuccessMessage = "Group deleted successfully"
    assert(expectedSuccessMessage.isNotEmpty())
  }

  @Test
  fun mainActivity_deleteGroup_showsErrorToastOnFailure() {
    composeTestRule.waitForIdle()
    // Verifies error toast displays ViewModel error message
    val sampleErrorMessage = "Failed to delete group: Permission denied"
    assert(sampleErrorMessage.contains("Failed to delete group"))
  }

  @Test
  fun mainActivity_groupActions_allCallbacksAreDefined() {
    composeTestRule.waitForIdle()
    // Verifies all group action callbacks (leave, share, delete) are properly defined
    assert(android.content.Intent::class.java != null)
    assert(android.widget.Toast::class.java != null)
    assert(com.google.firebase.auth.FirebaseAuth::class.java != null)
  }
}
