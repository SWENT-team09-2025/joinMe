// Implemented with help of Claude AI
package com.android.joinme

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import com.android.joinme.model.chat.ChatRepositoryProvider
import com.android.joinme.model.event.EventsRepositoryProvider
import com.android.joinme.model.groups.GroupRepositoryProvider
import com.android.joinme.model.invitation.InvitationRepositoryProvider
import com.android.joinme.model.invitation.InvitationType
import com.android.joinme.model.invitation.deepLink.DeepLinkService
import com.android.joinme.model.notification.FCMTokenManager
import com.android.joinme.model.profile.ProfileRepositoryProvider
import com.android.joinme.ui.calendar.CalendarScreen
import com.android.joinme.ui.chat.ChatScreen
import com.android.joinme.ui.chat.ChatViewModel
import com.android.joinme.ui.groups.ActivityGroupScreen
import com.android.joinme.ui.groups.CreateGroupScreen
import com.android.joinme.ui.groups.EditGroupScreen
import com.android.joinme.ui.groups.GroupDetailScreen
import com.android.joinme.ui.groups.GroupListScreen
import com.android.joinme.ui.groups.GroupListViewModel
import com.android.joinme.ui.groups.leaderboard.GroupLeaderboardScreen
import com.android.joinme.ui.groups.leaderboard.GroupLeaderboardViewModel
import com.android.joinme.ui.history.HistoryScreen
import com.android.joinme.ui.map.MapScreen
import com.android.joinme.ui.map.MapViewModel
import com.android.joinme.ui.navigation.NavigationActions
import com.android.joinme.ui.navigation.Screen
import com.android.joinme.ui.overview.CreateEventForSerieScreen
import com.android.joinme.ui.overview.CreateEventScreen
import com.android.joinme.ui.overview.CreateSerieScreen
import com.android.joinme.ui.overview.EditEventForSerieScreen
import com.android.joinme.ui.overview.EditEventScreen
import com.android.joinme.ui.overview.EditSerieScreen
import com.android.joinme.ui.overview.OverviewScreen
import com.android.joinme.ui.overview.SearchScreen
import com.android.joinme.ui.overview.SerieDetailsScreen
import com.android.joinme.ui.overview.ShowEventScreen
import com.android.joinme.ui.profile.EditProfileScreen
import com.android.joinme.ui.profile.ProfileViewModel
import com.android.joinme.ui.profile.PublicProfileScreen
import com.android.joinme.ui.profile.ViewProfileScreen
import com.android.joinme.ui.signIn.SignInScreen
import com.android.joinme.ui.theme.JoinMeTheme
import com.android.joinme.util.TestEnvironmentDetector
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient

/** Provides a singleton OkHttpClient instance for network operations. */
object HttpClientProvider {
  var client: OkHttpClient = OkHttpClient()
}

/** Intent extra keys and navigation argument keys. */
private const val KEY_TYPE = "type"
private const val KEY_CHAT_NAME = "chatName"
private const val KEY_CONVERSATION_ID = "conversationId"
private const val KEY_FOLLOWER_ID = "followerId"
private const val KEY_EVENT_ID = "eventId"
private const val KEY_GROUP_ID = "groupId"
private const val KEY_LAT = "lat"
private const val KEY_LON = "lon"
private const val KEY_MARKER = "marker"
private const val KEY_USER_ID = "userId"
private const val KEY_INITIAL_TAB = "initialTab"
private const val KEY_CHAT_ID = "chatId"
private const val KEY_CHAT_TITLE = "chatTitle"
private const val KEY_TOTAL_PARTICIPANTS = "totalParticipants"

/** Deep link hosts. */
private const val HOST_EVENT = "event"
private const val HOST_GROUP = "group"

/** Notification type values. */
private const val NOTIFICATION_TYPE_NEW_FOLLOWER = "new_follower"

/** Navigation argument keys. */
const val SERIES_ID = "serieId"
const val EVENT_ID = "eventId"

/**
 * Handles the logic for joining a group from an invitation link.
 *
 * @param groupId The ID of the group to join
 * @param userId The current user's ID, or null if not authenticated
 * @param context The context for showing toasts
 * @param navigationActions Actions for navigation after successful join
 */
private suspend fun handleGroupJoin(
    groupId: String,
    userId: String?,
    context: Context,
    navigationActions: NavigationActions
) {
  if (userId == null) {
    withContext(Dispatchers.Main) {
      Toast.makeText(context, context.getString(R.string.sign_in_to_join_group), Toast.LENGTH_SHORT)
          .show()
    }
    return
  }

  try {
    val groupRepository = GroupRepositoryProvider.repository
    groupRepository.joinGroup(groupId, userId)
    withContext(Dispatchers.Main) {
      Toast.makeText(context, context.getString(R.string.success_joining_group), Toast.LENGTH_SHORT)
          .show()
    }
    navigationActions.navigateTo(Screen.Groups)
    navigationActions.navigateTo(Screen.GroupDetail(groupId))
  } catch (e: Exception) {
    withContext(Dispatchers.Main) {
      Toast.makeText(
              context, context.getString(R.string.fail_joining_group, e.message), Toast.LENGTH_LONG)
          .show()
    }
  }
}

/**
 * Processes an invitation by resolving it and navigating to the appropriate screen.
 *
 * @param token The invitation token to process
 * @param userId The current user's ID
 * @param context The context for showing toasts
 * @param navigationActions Actions for navigation after successful processing
 */
private suspend fun processInvitation(
    token: String,
    userId: String,
    context: Context,
    navigationActions: NavigationActions
) {
  try {
    val invitationRepository = InvitationRepositoryProvider.repository
    val result = invitationRepository.resolveInvitation(token)

    result
        .onSuccess { invitation ->
          if (invitation != null && invitation.isValid()) {
            when (invitation.type) {
              InvitationType.GROUP ->
                  handleGroupJoin(invitation.targetId, userId, context, navigationActions)
              InvitationType.EVENT ->
                  navigationActions.navigateTo(Screen.ShowEventScreen(invitation.targetId))
              InvitationType.SERIE ->
                  navigationActions.navigateTo(Screen.SerieDetails(invitation.targetId))
            }
          } else {
            withContext(Dispatchers.Main) {
              Toast.makeText(
                      context,
                      context.getString(R.string.invalid_invitation_link),
                      Toast.LENGTH_LONG)
                  .show()
            }
          }
        }
        .onFailure { e ->
          withContext(Dispatchers.Main) {
            Toast.makeText(
                    context,
                    context.getString(R.string.process_invitation_failed, e.message),
                    Toast.LENGTH_LONG)
                .show()
          }
        }
  } catch (e: Exception) {
    withContext(Dispatchers.Main) {
      Toast.makeText(
              context,
              context.getString(R.string.process_invitation_failed, e.message),
              Toast.LENGTH_LONG)
          .show()
    }
  }
}

/**
 * Gets the current user ID, with support for test environments.
 *
 * @param currentUser The current Firebase user
 * @return The user ID (test ID in test environments, Firebase UID otherwise)
 */
private fun getCurrentUserId(currentUser: FirebaseUser?): String {
  return currentUser?.uid
      ?: if (TestEnvironmentDetector.shouldUseTestUserId()) TestEnvironmentDetector.getTestUserId()
      else ""
}

/**
 * MainActivity is the single activity for the JoinMe application.
 *
 * This activity follows the single-activity architecture pattern, hosting all navigation and
 * screens within Jetpack Compose. The activity's only responsibility is to set up the content view
 * with the JoinMe composable, which handles all navigation and UI logic.
 */
class MainActivity : ComponentActivity() {
  // State to hold new invitation tokens when app receives deep link while running
  private val newInvitationToken = mutableStateOf<String?>(null)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    createNotificationChannel()

    val deepLinkData = intent?.data
    val notificationType = intent?.getStringExtra(KEY_TYPE)
    val chatName = intent?.getStringExtra(KEY_CHAT_NAME)
    val conversationId = intent?.getStringExtra(KEY_CONVERSATION_ID)
    val followerId = intent?.getStringExtra(KEY_FOLLOWER_ID)

    // Parse invitations from deep links
    val invitationToken = DeepLinkService.parseInvitationLink(intent)

    // for notifications from events and groups
    val initialEventId =
        intent?.getStringExtra(KEY_EVENT_ID)
            ?: (if (deepLinkData?.host == HOST_EVENT) deepLinkData.lastPathSegment else null)
    val initialGroupId =
        intent?.getStringExtra(KEY_GROUP_ID)
            ?: (if (deepLinkData?.host == HOST_GROUP) deepLinkData.lastPathSegment else null)

    setContent {
      JoinMeTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
          val dynamicInvitationToken by newInvitationToken
          JoinMe(
              initialEventId = initialEventId,
              initialGroupId = initialGroupId,
              notificationType = notificationType,
              chatName = chatName,
              conversationId = conversationId,
              invitationToken = invitationToken,
              newInvitationToken = dynamicInvitationToken,
              onInvitationProcessed = { newInvitationToken.value = null },
              followerId = followerId)
        }
      }
    }
  }

  override fun onNewIntent(newIntent: Intent) {
    super.onNewIntent(newIntent)
    intent = newIntent
    // Check if this is an invitation link and update state to trigger recomposition
    val token = DeepLinkService.parseInvitationLink(newIntent)
    if (token != null) {
      newInvitationToken.value = token
    }
  }

  private fun createNotificationChannel() {
    val channel =
        NotificationChannel(
            getString(R.string.notification_channel_id),
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH)
    channel.description = getString(R.string.notification_channel_description)
    channel.enableVibration(true)

    val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.createNotificationChannel(channel)
  }
}

/** Handles navigation to event chat from notifications. */
@Composable
private fun EventChatNavigationEffect(
    initialEventId: String?,
    notificationType: String?,
    conversationId: String?,
    chatName: String?,
    currentUserId: String,
    eventChatMessageType: String,
    context: Context,
    navigationActions: NavigationActions,
    coroutineScope: CoroutineScope
) {
  LaunchedEffect(initialEventId, notificationType, currentUserId) {
    if (notificationType == eventChatMessageType &&
        conversationId != null &&
        chatName != null &&
        initialEventId != null &&
        currentUserId.isNotEmpty()) {
      coroutineScope.launch {
        try {
          val eventRepository = EventsRepositoryProvider.getRepository(isOnline = true, context)
          val event = eventRepository.getEvent(initialEventId)
          navigationActions.navigateTo(
              Screen.Chat(
                  chatId = conversationId,
                  chatTitle = chatName,
                  totalParticipants = event.participants.size))
        } catch (_: Exception) {
          navigationActions.navigateTo(
              Screen.Chat(chatId = conversationId, chatTitle = chatName, totalParticipants = 1))
        }
      }
    }
  }
}

/** Handles navigation to group chat from notifications. */
@Composable
private fun GroupChatNavigationEffect(
    initialGroupId: String?,
    notificationType: String?,
    conversationId: String?,
    chatName: String?,
    currentUserId: String,
    groupChatMessageType: String,
    context: Context,
    navigationActions: NavigationActions,
    coroutineScope: CoroutineScope
) {
  LaunchedEffect(initialGroupId, notificationType, currentUserId) {
    if (notificationType == groupChatMessageType &&
        conversationId != null &&
        chatName != null &&
        currentUserId.isNotEmpty() &&
        initialGroupId != null) {
      coroutineScope.launch {
        try {
          val groupRepository = GroupRepositoryProvider.repository
          val group = groupRepository.getGroup(initialGroupId)
          navigationActions.navigateTo(
              Screen.Chat(
                  chatId = conversationId,
                  chatTitle = chatName,
                  totalParticipants = group.memberIds.size))
        } catch (e: Exception) {
          withContext(Dispatchers.Main) {
            Toast.makeText(
                    context,
                    context.getString(R.string.error_failed_access_group, e.message),
                    Toast.LENGTH_LONG)
                .show()
          }
        }
      }
    }
  }
}

/** Handles navigation to follower profile from notifications. */
@Composable
private fun FollowerNavigationEffect(
    followerId: String?,
    notificationType: String?,
    currentUserId: String,
    navigationActions: NavigationActions
) {
  LaunchedEffect(followerId, notificationType, currentUserId) {
    if (followerId != null &&
        notificationType == NOTIFICATION_TYPE_NEW_FOLLOWER &&
        currentUserId.isNotEmpty()) {
      navigationActions.navigateTo(Screen.PublicProfile(followerId))
    }
  }
}

/** Handles the invitation token when user is not logged in. */
private fun handleUnauthenticatedInvitation(
    tokenToProcess: String,
    invitationToken: String?,
    onPendingTokenChange: (String?) -> Unit,
    onInitialTokenProcessed: () -> Unit
) {
  onPendingTokenChange(tokenToProcess)
  if (tokenToProcess == invitationToken) {
    onInitialTokenProcessed()
  }
}

/** Handles the invitation token when user is logged in. */
private suspend fun handleAuthenticatedInvitation(
    tokenToProcess: String,
    invitationToken: String?,
    newInvitationToken: String?,
    currentUserId: String,
    context: Context,
    navigationActions: NavigationActions,
    onInitialTokenProcessed: () -> Unit,
    onInvitationProcessed: () -> Unit
) {
  processInvitation(tokenToProcess, currentUserId, context, navigationActions)

  if (tokenToProcess == invitationToken) {
    onInitialTokenProcessed()
  }
  if (newInvitationToken != null) {
    onInvitationProcessed()
  }
}

/** Handles invitation token processing and authentication state. */
@Composable
private fun InvitationHandlingEffect(
    invitationToken: String?,
    newInvitationToken: String?,
    currentUserId: String,
    initialTokenProcessed: Boolean,
    context: Context,
    navigationActions: NavigationActions,
    coroutineScope: CoroutineScope,
    onPendingTokenChange: (String?) -> Unit,
    onInitialTokenProcessed: () -> Unit,
    onInvitationProcessed: () -> Unit
) {
  LaunchedEffect(invitationToken, newInvitationToken, currentUserId) {
    val tokenToProcess =
        newInvitationToken ?: (if (!initialTokenProcessed) invitationToken else null)

    tokenToProcess?.let { token ->
      if (currentUserId.isEmpty()) {
        handleUnauthenticatedInvitation(
            token, invitationToken, onPendingTokenChange, onInitialTokenProcessed)
      } else {
        coroutineScope.launch {
          handleAuthenticatedInvitation(
              token,
              invitationToken,
              newInvitationToken,
              currentUserId,
              context,
              navigationActions,
              onInitialTokenProcessed,
              onInvitationProcessed)
        }
      }
    }
  }
}

/** Processes pending invitations after user signs in. */
@Composable
private fun PendingInvitationEffect(
    pendingInvitationToken: String?,
    currentUserId: String,
    context: Context,
    navigationActions: NavigationActions,
    coroutineScope: CoroutineScope,
    onPendingTokenChange: (String?) -> Unit
) {
  LaunchedEffect(pendingInvitationToken, currentUserId) {
    if (pendingInvitationToken != null && currentUserId.isNotEmpty()) {
      coroutineScope.launch {
        processInvitation(pendingInvitationToken, currentUserId, context, navigationActions)
        onPendingTokenChange(null)
      }
    }
  }
}

/** Sets up the authentication navigation graph. */
private fun NavGraphBuilder.authNavigation(
    credentialManager: CredentialManager,
    context: Context,
    navigationActions: NavigationActions
) {
  navigation(
      startDestination = Screen.Auth.route,
      route = Screen.Auth.name,
  ) {
    composable(Screen.Auth.route) {
      SignInScreen(
          credentialManager = credentialManager,
          onSignedIn = {
            FCMTokenManager.initializeFCMToken(context)
            navigationActions.navigateTo(Screen.Overview)
          })
    }
  }
}

/** Sets up the events and series navigation graph. */
private fun NavGraphBuilder.eventsNavigation(
    context: Context,
    navigationActions: NavigationActions,
    enableNotificationPermissionRequest: Boolean
) {
  navigation(
      startDestination = Screen.Overview.route,
      route = Screen.Overview.name,
  ) {
    composable(Screen.Overview.route) {
      OverviewScreen(
          onSelectEvent = { navigationActions.navigateTo(Screen.ShowEventScreen(it.eventId)) },
          onAddEvent = { navigationActions.navigateTo(Screen.CreateEvent) },
          onAddSerie = { navigationActions.navigateTo(Screen.CreateSerie) },
          onSelectedSerie = { navigationActions.navigateTo(Screen.SerieDetails(it.serieId)) },
          onGoToHistory = { navigationActions.navigateTo(Screen.History) },
          onGoToCalendar = { navigationActions.navigateTo(Screen.Calendar) },
          navigationActions = navigationActions,
          enableNotificationPermissionRequest = enableNotificationPermissionRequest)
    }
    eventAndSerieManagementScreens(context, navigationActions)
    eventViewingScreens(context, navigationActions)
  }
}

/** Event and series creation/editing screens. */
private fun NavGraphBuilder.eventAndSerieManagementScreens(
    context: Context,
    navigationActions: NavigationActions
) {
  composable(Screen.CreateEvent.route) {
    CreateEventScreen(
        onDone = { navigationActions.navigateTo(Screen.Overview) },
        onGoBack = { navigationActions.goBack() })
  }
  composable(Screen.CreateSerie.route) {
    CreateSerieScreen(
        onDone = { serieId -> navigationActions.navigateTo(Screen.CreateEventForSerie(serieId)) },
        onGoBack = { navigationActions.goBack() })
  }
  composable(Screen.CreateEventForSerie.route) { navBackStackEntry ->
    val serieId = navBackStackEntry.arguments?.getString(SERIES_ID)
    serieId?.let {
      CreateEventForSerieScreen(
          serieId = serieId,
          onDone = { navigationActions.navigateTo(Screen.Overview) },
          onGoBack = { navigationActions.goBack() })
    }
        ?: run {
          Toast.makeText(
                  context, context.getString(R.string.error_series_id_null), Toast.LENGTH_SHORT)
              .show()
        }
  }
  composable(Screen.EditEvent.route) { navBackStackEntry ->
    val eventId = navBackStackEntry.arguments?.getString(EVENT_ID)
    eventId?.let {
      EditEventScreen(
          onDone = { navigationActions.navigateTo(Screen.Overview) },
          onGoBack = { navigationActions.goBack() },
          eventId = eventId)
    }
        ?: run {
          Toast.makeText(
                  context, context.getString(R.string.error_event_id_null), Toast.LENGTH_SHORT)
              .show()
        }
  }
  composable(Screen.EditSerie.route) { navBackStackEntry ->
    val serieId = navBackStackEntry.arguments?.getString(SERIES_ID)
    serieId?.let {
      EditSerieScreen(
          serieId = serieId,
          onGoBack = { navigationActions.goBack() },
          onDone = { navigationActions.navigateTo(Screen.Overview) })
    }
        ?: run {
          Toast.makeText(
                  context, context.getString(R.string.error_series_id_null), Toast.LENGTH_SHORT)
              .show()
        }
  }
  composable(Screen.EditEventForSerie.route) { navBackStackEntry ->
    val serieId = navBackStackEntry.arguments?.getString(SERIES_ID)
    val eventId = navBackStackEntry.arguments?.getString(EVENT_ID)
    if (serieId != null && eventId != null) {
      EditEventForSerieScreen(
          serieId = serieId,
          eventId = eventId,
          onDone = { navigationActions.navigateTo(Screen.Overview) },
          onGoBack = { navigationActions.goBack() })
    } else {
      Toast.makeText(
              context,
              context.getString(R.string.error_series_or_event_id_null),
              Toast.LENGTH_SHORT)
          .show()
    }
  }
}

/** Event viewing and history screens. */
private fun NavGraphBuilder.eventViewingScreens(
    context: Context,
    navigationActions: NavigationActions
) {
  composable(Screen.History.route) {
    HistoryScreen(
        onSelectEvent = { navigationActions.navigateTo(Screen.ShowEventScreen(it.eventId)) },
        onSelectSerie = { navigationActions.navigateTo(Screen.SerieDetails(it.serieId)) },
        onGoBack = { navigationActions.goBack() })
  }
  composable(Screen.Calendar.route) {
    CalendarScreen(
        onSelectEvent = { navigationActions.navigateTo(Screen.ShowEventScreen(it.eventId)) },
        onSelectSerie = { navigationActions.navigateTo(Screen.SerieDetails(it.serieId)) },
        onGoBack = { navigationActions.goBack() })
  }
  composable(Screen.ShowEventScreen.route) { navBackStackEntry ->
    val eventId = navBackStackEntry.arguments?.getString(EVENT_ID)
    val serieId = navBackStackEntry.arguments?.getString(SERIES_ID)
    eventId?.let {
      ShowEventScreen(
          eventId = eventId,
          serieId = serieId,
          onGoBack = { navigationActions.goBack() },
          onEditEvent = { id -> navigationActions.navigateTo(Screen.EditEvent(id)) },
          onEditEventForSerie = { sId, eId ->
            navigationActions.navigateTo(Screen.EditEventForSerie(sId, eId))
          },
          onNavigateToChat = { chatId, chatTitle, totalParticipants ->
            navigationActions.navigateTo(Screen.Chat(chatId, chatTitle, totalParticipants))
          },
          onNavigateToMap = { location ->
            navigationActions.navigateTo(Screen.Map(location.latitude, location.longitude))
          })
    } ?: run { Toast.makeText(context, "Event UID is null", Toast.LENGTH_SHORT).show() }
  }
  composable(Screen.SerieDetails.route) { navBackStackEntry ->
    val serieId = navBackStackEntry.arguments?.getString(SERIES_ID)
    serieId?.let {
      SerieDetailsScreen(
          serieId = serieId,
          onGoBack = { navigationActions.goBack() },
          onEventCardClick = { eventId ->
            navigationActions.navigateTo(Screen.ShowEventScreen(eventId, serieId))
          },
          onAddEventClick = { navigationActions.navigateTo(Screen.CreateEventForSerie(serieId)) },
          onEditSerieClick = { id -> navigationActions.navigateTo(Screen.EditSerie(id)) },
          onQuitSerieSuccess = { navigationActions.goBack() })
    }
        ?: run {
          Toast.makeText(
                  context, context.getString(R.string.error_series_id_null), Toast.LENGTH_SHORT)
              .show()
        }
  }
}

/** Search navigation graph. */
private fun NavGraphBuilder.searchNavigation(navigationActions: NavigationActions) {
  navigation(
      startDestination = Screen.Search.route,
      route = Screen.Search.name,
  ) {
    composable(Screen.Search.route) {
      SearchScreen(
          navigationActions = navigationActions,
          onSelectEvent = { navigationActions.navigateTo(Screen.ShowEventScreen(it.eventId)) },
          onSelectSerie = { serieId -> navigationActions.navigateTo(Screen.SerieDetails(serieId)) })
    }
  }
}

/** Map navigation graph. */
private fun NavGraphBuilder.mapNavigation(
    navigationActions: NavigationActions,
    currentUserId: String
) {
  navigation(
      startDestination = Screen.Map.defaultRoute,
      route = Screen.Map().name,
  ) {
    composable(Screen.Map.route) { backStackEntry ->
      val mapViewModel: MapViewModel = viewModel(backStackEntry)
      val lat = backStackEntry.arguments?.getString(KEY_LAT)?.toDoubleOrNull()
      val lon = backStackEntry.arguments?.getString(KEY_LON)?.toDoubleOrNull()
      val showMarker = backStackEntry.arguments?.getString(KEY_MARKER)?.toBoolean() ?: false
      val userId = backStackEntry.arguments?.getString(KEY_USER_ID)
      MapScreen(
          viewModel = mapViewModel,
          navigationActions = navigationActions,
          initialLatitude = lat,
          initialLongitude = lon,
          showLocationMarker = showMarker,
          sharedLocationUserId = userId,
          currentUserId = currentUserId)
    }
  }
}

/** Profile and Groups navigation graph. */
private fun NavGraphBuilder.profileAndGroupsNavigation(
    context: Context,
    currentUserId: String,
    currentUser: FirebaseUser?,
    sharedProfileViewModel: ProfileViewModel,
    navigationActions: NavigationActions
) {
  navigation(
      startDestination = Screen.Profile.route,
      route = Screen.Profile.name,
  ) {
    profileScreens(currentUserId, sharedProfileViewModel, navigationActions, context)
    groupScreens(context, navigationActions)
    chatScreen(context, currentUserId, currentUser, navigationActions)
  }
}

/** Profile-related screens. */
private fun NavGraphBuilder.profileScreens(
    currentUserId: String,
    sharedProfileViewModel: ProfileViewModel,
    navigationActions: NavigationActions,
    context: Context
) {
  composable(Screen.Profile.route) {
    ViewProfileScreen(
        uid = currentUserId,
        profileViewModel = sharedProfileViewModel,
        onTabSelected = { tab -> navigationActions.navigateTo(tab.destination) },
        onGroupClick = { navigationActions.navigateTo(Screen.Groups) },
        onEditClick = { navigationActions.navigateTo(Screen.EditProfile) },
        onSignOutComplete = {
          FCMTokenManager.clearFCMToken()
          navigationActions.navigateTo(Screen.Auth)
        })
  }
  composable(Screen.PublicProfile.route) { navBackStackEntry ->
    val userId = navBackStackEntry.arguments?.getString(KEY_USER_ID)
    userId?.let {
      PublicProfileScreen(
          userId = userId,
          onBackClick = { navigationActions.goBack() },
          onEventClick = { event ->
            navigationActions.navigateTo(Screen.ShowEventScreen(event.eventId))
          },
          onGroupClick = { group -> navigationActions.navigateTo(Screen.GroupDetail(group.id)) },
          onFollowersClick = { profileUserId ->
            navigationActions.navigateTo(
                Screen.FollowList(
                    profileUserId, com.android.joinme.ui.profile.FollowTab.FOLLOWERS.name))
          },
          onFollowingClick = { profileUserId ->
            navigationActions.navigateTo(
                Screen.FollowList(
                    profileUserId, com.android.joinme.ui.profile.FollowTab.FOLLOWING.name))
          },
          onMessageClick = { chatId, chatTitle, totalParticipants ->
            navigationActions.navigateTo(Screen.Chat(chatId, chatTitle, totalParticipants))
          })
    }
        ?: run {
          Toast.makeText(
                  context, context.getString(R.string.error_user_id_null), Toast.LENGTH_SHORT)
              .show()
        }
  }
  composable(Screen.FollowList.route) { navBackStackEntry ->
    val userId = navBackStackEntry.arguments?.getString(KEY_USER_ID)
    val initialTabString =
        navBackStackEntry.arguments?.getString(KEY_INITIAL_TAB)
            ?: com.android.joinme.ui.profile.FollowTab.FOLLOWERS.name
    userId?.let {
      com.android.joinme.ui.profile.FollowListScreen(
          userId = userId,
          initialTab =
              if (initialTabString == com.android.joinme.ui.profile.FollowTab.FOLLOWING.name)
                  com.android.joinme.ui.profile.FollowTab.FOLLOWING
              else com.android.joinme.ui.profile.FollowTab.FOLLOWERS,
          onBackClick = { navigationActions.goBack() },
          onProfileClick = { profileUserId ->
            navigationActions.navigateTo(Screen.PublicProfile(profileUserId))
          })
    }
        ?: run {
          Toast.makeText(
                  context, context.getString(R.string.error_user_id_null), Toast.LENGTH_SHORT)
              .show()
        }
  }
  composable(Screen.EditProfile.route) {
    EditProfileScreen(
        uid = currentUserId,
        profileViewModel = sharedProfileViewModel,
        onBackClick = {
          navigationActions.navigateAndClearBackStackTo(
              screen = Screen.Profile, popUpToRoute = Screen.Profile.route, inclusive = false)
        },
        onProfileClick = { navigationActions.navigateTo(Screen.Profile) },
        onGroupClick = { navigationActions.navigateTo(Screen.Groups) },
        onSaveSuccess = { navigationActions.navigateTo(Screen.Profile) })
  }
}

/** Group-related screens. */
private fun NavGraphBuilder.groupScreens(context: Context, navigationActions: NavigationActions) {
  composable(Screen.Groups.route) {
    val groupListViewModel: GroupListViewModel = viewModel()
    GroupListScreen(
        viewModel = groupListViewModel,
        onCreateGroup = { navigationActions.navigateTo(Screen.CreateGroup) },
        onGroup = { group -> navigationActions.navigateTo(Screen.GroupDetail(group.id)) },
        onBackClick = {
          navigationActions.navigateAndClearBackStackTo(
              screen = Screen.Profile, popUpToRoute = Screen.Profile.route, inclusive = false)
        },
        onProfileClick = { navigationActions.navigateTo(Screen.Profile) },
        onEditClick = { navigationActions.navigateTo(Screen.EditProfile) },
        onLeaveGroup = { group ->
          groupListViewModel.leaveGroup(
              groupId = group.id,
              onSuccess = {
                Toast.makeText(
                        context, context.getString(R.string.success_left_group), Toast.LENGTH_SHORT)
                    .show()
              },
              onError = { error -> Toast.makeText(context, error, Toast.LENGTH_LONG).show() })
        },
        onEditGroup = { group -> navigationActions.navigateTo(Screen.EditGroup(group.id)) },
        onDeleteGroup = { group ->
          groupListViewModel.deleteGroup(
              groupId = group.id,
              onSuccess = {
                Toast.makeText(
                        context,
                        context.getString(R.string.success_deleted_group),
                        Toast.LENGTH_SHORT)
                    .show()
              },
              onError = { error -> Toast.makeText(context, error, Toast.LENGTH_LONG).show() })
        })
  }
  composable(route = Screen.CreateGroup.route) {
    CreateGroupScreen(
        onBackClick = { navigationActions.goBack() },
        onCreateSuccess = {
          navigationActions.navigateAndClearBackStackTo(
              screen = Screen.Groups, popUpToRoute = Screen.Profile.route, inclusive = false)
        })
  }
  composable(route = Screen.EditGroup.route) { navBackStackEntry ->
    val groupId = navBackStackEntry.arguments?.getString(KEY_GROUP_ID)
    groupId?.let {
      EditGroupScreen(
          groupId = groupId,
          onBackClick = { navigationActions.goBack() },
          onSaveSuccess = { navigationActions.navigateTo(Screen.Groups) })
    }
        ?: run {
          Toast.makeText(
                  context, context.getString(R.string.error_group_id_null), Toast.LENGTH_SHORT)
              .show()
        }
  }
  composable(route = Screen.GroupDetail.route) { navBackStackEntry ->
    val groupId = navBackStackEntry.arguments?.getString(KEY_GROUP_ID)
    groupId?.let {
      GroupDetailScreen(
          groupId = groupId,
          onBackClick = { navigationActions.goBack() },
          onActivityGroupClick = { navigationActions.navigateTo(Screen.ActivityGroup(groupId)) },
          onMemberClick = { userId -> navigationActions.navigateTo(Screen.PublicProfile(userId)) },
          onNavigateToChat = { chatId, chatTitle, totalParticipants ->
            navigationActions.navigateTo(Screen.Chat(chatId, chatTitle, totalParticipants))
          },
          onNavigateToLeaderboard = {
            navigationActions.navigateTo(Screen.GroupLeaderboard(groupId))
          })
    }
  }
  composable(route = Screen.ActivityGroup.route) { navBackStackEntry ->
    val groupId = navBackStackEntry.arguments?.getString(KEY_GROUP_ID)
    groupId?.let {
      ActivityGroupScreen(
          groupId = groupId,
          onNavigateBack = { navigationActions.goBack() },
          onSelectedEvent = { eventId ->
            navigationActions.navigateTo(Screen.ShowEventScreen(eventId))
          },
          onSelectedSerie = { serieId ->
            navigationActions.navigateTo(Screen.SerieDetails(serieId))
          })
    }
        ?: run {
          Toast.makeText(
                  context, context.getString(R.string.error_group_id_null), Toast.LENGTH_SHORT)
              .show()
        }
  }
  composable(route = Screen.GroupLeaderboard.route) { navBackStackEntry ->
    val groupId = navBackStackEntry.arguments?.getString(KEY_GROUP_ID)
    groupId?.let {
      val leaderboardViewModel: GroupLeaderboardViewModel =
          viewModel(
              factory =
                  object : ViewModelProvider.Factory {
                    // Cannot put hardcoded string in a const, else we have a cast warning
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                      return GroupLeaderboardViewModel(
                          application = (context.applicationContext as Application))
                          as T
                    }
                  })
      GroupLeaderboardScreen(
          groupId = groupId,
          viewModel = leaderboardViewModel,
          onNavigateBack = { navigationActions.goBack() })
    }
        ?: run {
          Toast.makeText(
                  context, context.getString(R.string.error_group_id_null), Toast.LENGTH_SHORT)
              .show()
        }
  }
}

/** Chat screen. */
private fun NavGraphBuilder.chatScreen(
    context: Context,
    currentUserId: String,
    currentUser: FirebaseUser?,
    navigationActions: NavigationActions
) {
  composable(route = Screen.Chat.route) { navBackStackEntry ->
    val chatId = navBackStackEntry.arguments?.getString(KEY_CHAT_ID)
    val chatTitle = navBackStackEntry.arguments?.getString(KEY_CHAT_TITLE)
    val totalParticipants =
        navBackStackEntry.arguments?.getString(KEY_TOTAL_PARTICIPANTS)?.toIntOrNull() ?: 1
    if (chatId != null && chatTitle != null) {
      val chatViewModel: ChatViewModel =
          viewModel(
              factory =
                  object : ViewModelProvider.Factory {
                    // Cannot put hardcoded string in a const, else we have a cast warning
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                      return ChatViewModel(
                          ChatRepositoryProvider.repository, ProfileRepositoryProvider.repository)
                          as T
                    }
                  })
      val currentUserName = currentUser?.displayName ?: stringResource(R.string.unknown_user)
      ChatScreen(
          chatId = chatId,
          chatTitle = chatTitle,
          currentUserId = currentUserId,
          currentUserName = currentUserName,
          viewModel = chatViewModel,
          totalParticipants = totalParticipants,
          onLeaveClick = { navigationActions.goBack() },
          onNavigateToMap = { location, senderId ->
            navigationActions.navigateTo(
                Screen.Map(
                    location.latitude, location.longitude, showMarker = true, userId = senderId))
            Toast.makeText(
                    context,
                    context.getString(R.string.toast_viewing_location, location.name),
                    Toast.LENGTH_SHORT)
                .show()
          })
    } else {
      Toast.makeText(
              context, context.getString(R.string.error_chat_id_or_title_null), Toast.LENGTH_SHORT)
          .show()
    }
  }
}

/**
 * JoinMe is the root composable of the application, managing all navigation and screen routing.
 *
 * This composable sets up the NavHost and defines all navigation graphs for the application,
 * including authentication, overview/events, search, map, and profile flows. It uses
 * AuthRepositoryProvider to determine the initial destination based on user authentication state.
 *
 * @param context The context used for showing toasts and other system interactions. Defaults to
 *   LocalContext.
 * @param credentialManager The CredentialManager used for Google Sign-In. Defaults to a new
 *   instance.
 * @param startDestination Optional override for the initial navigation destination. Primarily used
 *   for testing. If null, determines destination based on authentication state (Auth or Overview).
 */
@Composable
fun JoinMe(
    context: Context = LocalContext.current,
    credentialManager: CredentialManager = CredentialManager.create(context),
    startDestination: String? = null,
    initialEventId: String? = null,
    initialGroupId: String? = null,
    notificationType: String? = null,
    chatName: String? = null,
    conversationId: String? = null,
    followerId: String? = null,
    invitationToken: String? = null,
    newInvitationToken: String? = null,
    onInvitationProcessed: () -> Unit = {},
    enableNotificationPermissionRequest: Boolean = true
) {
  val navController = rememberNavController()
  val navigationActions = NavigationActions(navController)
  val coroutineScope = rememberCoroutineScope()

  var currentUser by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser) }

  // Shared ProfileViewModel for ViewProfile and EditProfile screens
  // Key it to currentUserId so it gets recreated when user changes
  val sharedProfileViewModel: ProfileViewModel =
      viewModel(key = currentUser?.uid ?: context.getString(R.string.unknown_user_key))
  var pendingInvitationToken by remember { mutableStateOf<String?>(null) }
  var initialTokenProcessed by remember { mutableStateOf(false) }

  // Listen for auth state changes
  LaunchedEffect(Unit) {
    val authStateListener =
        FirebaseAuth.AuthStateListener { auth -> currentUser = auth.currentUser }
    FirebaseAuth.getInstance().addAuthStateListener(authStateListener)
  }

  // Get current user ID with test mode support
  val currentUserId = remember(currentUser) { getCurrentUserId(currentUser) }

  // Get notification type strings
  val eventChatMessageType = context.getString(R.string.notification_type_event_chat_message)
  val groupChatMessageType = context.getString(R.string.notification_type_group_chat_message)

  val initialDestination =
      startDestination ?: if (currentUser == null) Screen.Auth.name else Screen.Overview.route

  // Initialize FCM token when user is logged in
  LaunchedEffect(Unit) {
    if (currentUser != null) {
      FCMTokenManager.initializeFCMToken(context)
    }
  }

  // Handle various navigation effects from notifications and deep links
  EventChatNavigationEffect(
      initialEventId,
      notificationType,
      conversationId,
      chatName,
      currentUserId,
      eventChatMessageType,
      context,
      navigationActions,
      coroutineScope)

  GroupChatNavigationEffect(
      initialGroupId,
      notificationType,
      conversationId,
      chatName,
      currentUserId,
      groupChatMessageType,
      context,
      navigationActions,
      coroutineScope)

  FollowerNavigationEffect(followerId, notificationType, currentUserId, navigationActions)

  InvitationHandlingEffect(
      invitationToken,
      newInvitationToken,
      currentUserId,
      initialTokenProcessed,
      context,
      navigationActions,
      coroutineScope,
      onPendingTokenChange = { pendingInvitationToken = it },
      onInitialTokenProcessed = { initialTokenProcessed = true },
      onInvitationProcessed)

  PendingInvitationEffect(
      pendingInvitationToken,
      currentUserId,
      context,
      navigationActions,
      coroutineScope,
      onPendingTokenChange = { pendingInvitationToken = null })

  NavHost(navController = navController, startDestination = initialDestination) {
    authNavigation(credentialManager, context, navigationActions)
    eventsNavigation(context, navigationActions, enableNotificationPermissionRequest)
    searchNavigation(navigationActions)
    mapNavigation(navigationActions, currentUserId)
    profileAndGroupsNavigation(
        context, currentUserId, currentUser, sharedProfileViewModel, navigationActions)
  }
}
