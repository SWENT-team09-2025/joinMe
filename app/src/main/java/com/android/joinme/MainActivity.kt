// Implemented with help of Claude AI
package com.android.joinme

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
import androidx.credentials.CredentialManager
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import com.android.joinme.model.chat.ChatRepositoryProvider
import com.android.joinme.model.groups.GroupRepositoryProvider
import com.android.joinme.model.notification.FCMTokenManager
import com.android.joinme.model.profile.ProfileRepositoryProvider
import com.android.joinme.ui.chat.ChatScreen
import com.android.joinme.ui.chat.ChatViewModel
import com.android.joinme.ui.groups.CreateGroupScreen
import com.android.joinme.ui.groups.EditGroupScreen
import com.android.joinme.ui.groups.GroupDetailScreen
import com.android.joinme.ui.groups.GroupListScreen
import com.android.joinme.ui.groups.GroupListViewModel
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
import com.android.joinme.ui.profile.ViewProfileScreen
import com.android.joinme.ui.signIn.SignInScreen
import com.android.joinme.ui.theme.JoinMeTheme
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

/** Provides a singleton OkHttpClient instance for network operations. */
object HttpClientProvider {
  var client: OkHttpClient = OkHttpClient()
}

/** Exception message for when a serie ID is null. */
const val SERIES_ID_NULL = "Serie ID is null"

/** Key for the series ID in the bundle. */
const val SERIES_ID = "serieId"

/** Key for the event ID in the bundle. */
const val EVENT_ID = "eventId"
/**
 * MainActivity is the single activity for the JoinMe application.
 *
 * This activity follows the single-activity architecture pattern, hosting all navigation and
 * screens within Jetpack Compose. The activity's only responsibility is to set up the content view
 * with the JoinMe composable, which handles all navigation and UI logic.
 */
class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    createNotificationChannel()

    val deepLinkData = intent?.data
    val initialEventId = if (deepLinkData?.host == "event") deepLinkData.lastPathSegment else null
    val initialGroupId =
        when {
          deepLinkData?.host == "group" -> deepLinkData.lastPathSegment
          deepLinkData?.host == "joinme.app" &&
              deepLinkData.pathSegments?.firstOrNull() == "group" ->
              deepLinkData.pathSegments?.getOrNull(1)
          else -> null
        }

    setContent {
      JoinMeTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
          JoinMe(initialEventId = initialEventId, initialGroupId = initialGroupId)
        }
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
  }

  private fun createNotificationChannel() {
    val channel =
        NotificationChannel(
            "event_notifications", "Event Notifications", NotificationManager.IMPORTANCE_HIGH)
    channel.description = "Notifications for upcoming events"
    channel.enableVibration(true)

    val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.createNotificationChannel(channel)
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
    enableNotificationPermissionRequest: Boolean = true,
) {
  val navController = rememberNavController()
  val navigationActions = NavigationActions(navController)
  val coroutineScope = rememberCoroutineScope()

  var currentUser by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser) }

  // Listen for auth state changes
  LaunchedEffect(Unit) {
    val authStateListener =
        FirebaseAuth.AuthStateListener { auth -> currentUser = auth.currentUser }
    FirebaseAuth.getInstance().addAuthStateListener(authStateListener)
  }
  val initialDestination =
      startDestination ?: if (currentUser == null) Screen.Auth.name else Screen.Overview.route

  // Initialize FCM token when user is logged in
  LaunchedEffect(Unit) {
    if (currentUser != null) {
      FCMTokenManager.initializeFCMToken(context)
    }
  }

  // Navigate to event if opened from notification
  LaunchedEffect(initialEventId) {
    if (initialEventId != null && currentUser != null) {
      navigationActions.navigateTo(Screen.ShowEventScreen(initialEventId))
    }
  }

  // Join group if opened from invitation link
  LaunchedEffect(initialGroupId) {
    if (initialGroupId != null) {
      if (currentUser != null) {
        coroutineScope.launch {
          try {
            val groupRepository = GroupRepositoryProvider.repository
            groupRepository.joinGroup(initialGroupId, currentUser!!.uid)
            Toast.makeText(context, "Successfully joined the group!", Toast.LENGTH_SHORT).show()
            navigationActions.navigateTo(Screen.GroupDetail(initialGroupId))
          } catch (e: Exception) {
            Toast.makeText(context, "Failed to join group: ${e.message}", Toast.LENGTH_LONG).show()
          }
        }
      } else {
        Toast.makeText(context, "Please sign in to join the group", Toast.LENGTH_SHORT).show()
      }
    }
  }

  NavHost(navController = navController, startDestination = initialDestination) {
    // ============================================================================
    // Authentication
    // ============================================================================
    navigation(
        startDestination = Screen.Auth.route,
        route = Screen.Auth.name,
    ) {
      composable(Screen.Auth.route) {
        SignInScreen(
            credentialManager = credentialManager,
            onSignedIn = {
              // Initialize FCM token after successful sign-in
              FCMTokenManager.initializeFCMToken(context)
              navigationActions.navigateTo(Screen.Overview)
            })
      }
    }

    // ============================================================================
    // Events, Series & History
    // ============================================================================
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
            navigationActions = navigationActions,
            credentialManager = credentialManager,
            enableNotificationPermissionRequest = enableNotificationPermissionRequest)
      }
      composable(Screen.CreateEvent.route) {
        CreateEventScreen(
            onDone = { navigationActions.navigateTo(Screen.Overview) },
            onGoBack = { navigationActions.goBack() })
      }
      composable(Screen.CreateSerie.route) {
        CreateSerieScreen(
            onDone = { serieId ->
              navigationActions.navigateTo(Screen.CreateEventForSerie(serieId))
            },
            onGoBack = { navigationActions.goBack() })
      }
      composable(Screen.CreateEventForSerie.route) { navBackStackEntry ->
        val serieId = navBackStackEntry.arguments?.getString(SERIES_ID)

        serieId?.let {
          CreateEventForSerieScreen(
              serieId = serieId,
              onDone = { navigationActions.navigateTo(Screen.Overview) },
              onGoBack = { navigationActions.goBack() })
        } ?: run { Toast.makeText(context, SERIES_ID_NULL, Toast.LENGTH_SHORT).show() }
      }
      composable(Screen.EditEvent.route) { navBackStackEntry ->
        val eventId = navBackStackEntry.arguments?.getString(EVENT_ID)

        eventId?.let {
          EditEventScreen(
              onDone = { navigationActions.navigateTo(Screen.Overview) },
              onGoBack = { navigationActions.goBack() },
              eventId = eventId)
        } ?: run { Toast.makeText(context, "Event UID is null", Toast.LENGTH_SHORT).show() }
      }
      composable(Screen.History.route) {
        HistoryScreen(
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
              onAddEventClick = {
                navigationActions.navigateTo(Screen.CreateEventForSerie(serieId))
              },
              onEditSerieClick = { id -> navigationActions.navigateTo(Screen.EditSerie(id)) },
              onQuitSerieSuccess = { navigationActions.goBack() })
        } ?: run { Toast.makeText(context, SERIES_ID_NULL, Toast.LENGTH_SHORT).show() }
      }
      composable(Screen.EditSerie.route) { navBackStackEntry ->
        val serieId = navBackStackEntry.arguments?.getString(SERIES_ID)

        serieId?.let {
          EditSerieScreen(
              serieId = serieId,
              onGoBack = { navigationActions.goBack() },
              onDone = { navigationActions.navigateTo(Screen.Overview) })
        } ?: run { Toast.makeText(context, SERIES_ID_NULL, Toast.LENGTH_SHORT).show() }
      }
      composable(Screen.CreateEventForSerie.route) { navBackStackEntry ->
        val serieId = navBackStackEntry.arguments?.getString(SERIES_ID)

        serieId?.let {
          CreateEventForSerieScreen(
              serieId = serieId,
              onDone = { navigationActions.navigateTo(Screen.Overview) },
              onGoBack = { navigationActions.goBack() })
        } ?: run { Toast.makeText(context, SERIES_ID_NULL, Toast.LENGTH_SHORT).show() }
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
          Toast.makeText(context, "Serie ID or Event ID is null", Toast.LENGTH_SHORT).show()
        }
      }
    }

    // ============================================================================
    // Search
    // ============================================================================
    navigation(
        startDestination = Screen.Search.route,
        route = Screen.Search.name,
    ) {
      composable(Screen.Search.route) {
        SearchScreen(
            navigationActions = navigationActions,
            onSelectEvent = { navigationActions.navigateTo(Screen.ShowEventScreen(it.eventId)) },
            onSelectSerie = { serieId ->
              navigationActions.navigateTo(Screen.SerieDetails(serieId))
            })
      }
    }

    // ============================================================================
    // Map
    // ============================================================================
    navigation(
        startDestination = Screen.Map.route,
        route = Screen.Map.name,
    ) {
      composable(Screen.Map.route) { backStackEntry ->
        val mapViewModel: MapViewModel = viewModel(backStackEntry)
        MapScreen(viewModel = mapViewModel, navigationActions = navigationActions)
      }
    }

    // ============================================================================
    // Profile & Groups
    // ============================================================================
    navigation(
        startDestination = Screen.Profile.route,
        route = Screen.Profile.name,
    ) {
      composable(Screen.Profile.route) {
        ViewProfileScreen(
            uid = currentUser?.uid ?: "",
            onTabSelected = { tab -> navigationActions.navigateTo(tab.destination) },
            onBackClick = { navigationActions.goBack() },
            onGroupClick = { navigationActions.navigateTo(Screen.Groups) },
            onEditClick = { navigationActions.navigateTo(Screen.EditProfile) },
            onSignOutComplete = {
              // Clear FCM token on sign-out
              FCMTokenManager.clearFCMToken()
              navigationActions.navigateTo(Screen.Auth)
            })
      }

      composable(Screen.EditProfile.route) {
        EditProfileScreen(
            uid = currentUser?.uid ?: "",
            onBackClick = { navigationActions.goBack() },
            onProfileClick = { navigationActions.navigateTo(Screen.Profile) },
            onGroupClick = { navigationActions.navigateTo(Screen.Groups) },
            onSaveSuccess = { navigationActions.navigateTo(Screen.Profile) })
      }

      composable(Screen.Groups.route) {
        val groupListViewModel: GroupListViewModel = viewModel()

        GroupListScreen(
            viewModel = groupListViewModel,
            onJoinWithLink = { groupId ->
              groupListViewModel.joinGroup(
                  groupId = groupId,
                  onSuccess = {
                    Toast.makeText(context, "Successfully joined the group!", Toast.LENGTH_SHORT)
                        .show()
                  },
                  onError = { error -> Toast.makeText(context, error, Toast.LENGTH_LONG).show() })
            },
            onCreateGroup = { navigationActions.navigateTo(Screen.CreateGroup) },
            onGroup = { group ->
              // Navigate to group details
              navigationActions.navigateTo(Screen.GroupDetail(group.id))
            },
            onBackClick = { navigationActions.goBack() },
            onProfileClick = { navigationActions.navigateTo(Screen.Profile) },
            onEditClick = { navigationActions.navigateTo(Screen.EditProfile) },
            onViewGroupDetails = { navigationActions.navigateTo(Screen.GroupDetail(it.id)) },
            onLeaveGroup = { group ->
              groupListViewModel.leaveGroup(
                  groupId = group.id,
                  onSuccess = {
                    Toast.makeText(context, "Left group successfully", Toast.LENGTH_SHORT).show()
                  },
                  onError = { error -> Toast.makeText(context, error, Toast.LENGTH_LONG).show() })
            },
            onShareGroup = { group ->
              val deepLink = "joinme://group/${group.id}"
              val shareIntent =
                  Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_SUBJECT, "Join my group on JoinMe!")
                    putExtra(
                        Intent.EXTRA_TEXT,
                        "Join '${group.name}' on JoinMe!\n\nCategory: ${group.category}\n${if (group.description.isNotBlank()) "Description: ${group.description}\n\n" else "\n"}Click the link to join: $deepLink")
                    type = "text/plain"
                  }
              context.startActivity(Intent.createChooser(shareIntent, "Share Group via"))
            },
            onEditGroup = { group -> navigationActions.navigateTo(Screen.EditGroup(group.id)) },
            onDeleteGroup = { group ->
              groupListViewModel.deleteGroup(
                  groupId = group.id,
                  onSuccess = {
                    Toast.makeText(context, "Group deleted successfully", Toast.LENGTH_SHORT).show()
                  },
                  onError = { error -> Toast.makeText(context, error, Toast.LENGTH_LONG).show() })
            })
      }

      composable(route = Screen.CreateGroup.route) {
        CreateGroupScreen(
            onBackClick = { navigationActions.goBack() },
            onCreateSuccess = { navigationActions.navigateTo(Screen.Groups) })
      }

      composable(route = Screen.EditGroup.route) { navBackStackEntry ->
        val groupId = navBackStackEntry.arguments?.getString("groupId")

        groupId?.let {
          EditGroupScreen(
              groupId = groupId,
              onBackClick = { navigationActions.goBack() },
              onSaveSuccess = { navigationActions.navigateTo(Screen.Groups) })
        } ?: run { Toast.makeText(context, "Group ID is null", Toast.LENGTH_SHORT).show() }
      }

      composable(route = Screen.GroupDetail.route) { navBackStackEntry ->
        val groupId = navBackStackEntry.arguments?.getString("groupId")

        groupId?.let {
          GroupDetailScreen(
              groupId = groupId,
              onBackClick = { navigationActions.goBack() },
              onGroupEventsClick = {
                Toast.makeText(context, "Not yet implemented ", Toast.LENGTH_SHORT).show()
              },
              onMemberClick = {
                Toast.makeText(context, "Not yet implemented ", Toast.LENGTH_SHORT).show()
              },
              onNavigateToChat = { chatId, chatTitle, totalParticipants ->
                navigationActions.navigateTo(Screen.Chat(chatId, chatTitle, totalParticipants))
              })
        }
      }

      composable(route = Screen.Chat.route) { navBackStackEntry ->
        val chatId = navBackStackEntry.arguments?.getString("chatId")
        val chatTitle = navBackStackEntry.arguments?.getString("chatTitle")
        val totalParticipants =
            navBackStackEntry.arguments?.getString("totalParticipants")?.toIntOrNull() ?: 1

        if (chatId != null && chatTitle != null) {
          // Use viewModel() factory pattern to get a properly scoped ViewModel
          // that survives recompositions
          val chatViewModel: ChatViewModel =
              viewModel(
                  factory =
                      object : androidx.lifecycle.ViewModelProvider.Factory {
                        override fun <T : androidx.lifecycle.ViewModel> create(
                            modelClass: Class<T>
                        ): T {
                          @Suppress("UNCHECKED_CAST")
                          return ChatViewModel(
                              ChatRepositoryProvider.repository,
                              ProfileRepositoryProvider.repository)
                              as T
                        }
                      })

          val currentUserId = currentUser?.uid ?: ""
          val currentUserName = currentUser?.displayName ?: "Unknown User"

          ChatScreen(
              chatId = chatId,
              chatTitle = chatTitle,
              currentUserId = currentUserId,
              currentUserName = currentUserName,
              viewModel = chatViewModel,
              onBackClick = { navigationActions.goBack() },
              totalParticipants = totalParticipants)
        } else {
          Toast.makeText(context, "Chat ID or title is null", Toast.LENGTH_SHORT).show()
        }
      }
    }
  }
}
