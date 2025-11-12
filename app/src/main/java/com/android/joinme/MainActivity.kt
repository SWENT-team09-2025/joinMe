package com.android.joinme

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import com.android.joinme.model.notification.FCMTokenManager
import com.android.joinme.ui.groups.CreateGroupScreen
import com.android.joinme.ui.groups.EditGroupScreen
import com.android.joinme.ui.groups.GroupDetailScreen
import com.android.joinme.ui.groups.GroupListScreen
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
import okhttp3.OkHttpClient

/** Provides a singleton OkHttpClient instance for network operations. */
object HttpClientProvider {
  var client: OkHttpClient = OkHttpClient()
}

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

    val initialEventId = intent?.data?.lastPathSegment

    setContent {
      JoinMeTheme {
        Surface(modifier = Modifier.fillMaxSize()) { JoinMe(initialEventId = initialEventId) }
      }
    }
  }

  override fun onNewIntent(intent: android.content.Intent) {
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
    enableNotificationPermissionRequest: Boolean = true,
) {
  val navController = rememberNavController()
  val navigationActions = NavigationActions(navController)
  val initialDestination =
      startDestination
          ?: if (FirebaseAuth.getInstance().currentUser == null) Screen.Auth.name
          else Screen.Overview.route

  // Initialize FCM token when user is logged in
  LaunchedEffect(Unit) {
    if (FirebaseAuth.getInstance().currentUser != null) {
      FCMTokenManager.initializeFCMToken(context)
    }
  }

  // Navigate to event if opened from notification
  LaunchedEffect(initialEventId) {
    if (initialEventId != null && FirebaseAuth.getInstance().currentUser != null) {
      navigationActions.navigateTo(Screen.ShowEventScreen(initialEventId))
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
        val serieId = navBackStackEntry.arguments?.getString("serieId")

        serieId?.let {
          CreateEventForSerieScreen(
              serieId = serieId,
              onDone = { navigationActions.navigateTo(Screen.Overview) },
              onGoBack = { navigationActions.goBack() })
        } ?: run { Toast.makeText(context, "Serie ID is null", Toast.LENGTH_SHORT).show() }
      }
      composable(Screen.EditEvent.route) { navBackStackEntry ->
        val eventId = navBackStackEntry.arguments?.getString("eventId")

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
            onSelectSerie = {
              Toast.makeText(context, "Not Implemented", Toast.LENGTH_SHORT).show()
            },
            onGoBack = { navigationActions.goBack() })
      }
      composable(Screen.ShowEventScreen.route) { navBackStackEntry ->
        val eventId = navBackStackEntry.arguments?.getString("eventId")
        val serieId = navBackStackEntry.arguments?.getString("serieId")

        eventId?.let {
          ShowEventScreen(
              eventId = eventId,
              serieId = serieId,
              onGoBack = { navigationActions.goBack() },
              onEditEvent = { id -> navigationActions.navigateTo(Screen.EditEvent(id)) },
              onEditEventForSerie = { sId, eId ->
                navigationActions.navigateTo(Screen.EditEventForSerie(sId, eId))
              })
        } ?: run { Toast.makeText(context, "Event UID is null", Toast.LENGTH_SHORT).show() }
      }
      composable(Screen.SerieDetails.route) { navBackStackEntry ->
        val serieId = navBackStackEntry.arguments?.getString("serieId")

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
        } ?: run { Toast.makeText(context, "Serie ID is null", Toast.LENGTH_SHORT).show() }
      }
      composable(Screen.EditSerie.route) { navBackStackEntry ->
        val serieId = navBackStackEntry.arguments?.getString("serieId")

        serieId?.let {
          EditSerieScreen(
              serieId = serieId,
              onGoBack = { navigationActions.goBack() },
              onDone = { navigationActions.navigateTo(Screen.Overview) })
        } ?: run { Toast.makeText(context, "Serie ID is null", Toast.LENGTH_SHORT).show() }
      }
      composable(Screen.CreateEventForSerie.route) { navBackStackEntry ->
        val serieId = navBackStackEntry.arguments?.getString("serieId")

        serieId?.let {
          CreateEventForSerieScreen(
              serieId = serieId,
              onDone = { navigationActions.navigateTo(Screen.Overview) },
              onGoBack = { navigationActions.goBack() })
        } ?: run { Toast.makeText(context, "Serie ID is null", Toast.LENGTH_SHORT).show() }
      }

      composable(Screen.EditEventForSerie.route) { navBackStackEntry ->
        val serieId = navBackStackEntry.arguments?.getString("serieId")
        val eventId = navBackStackEntry.arguments?.getString("eventId")

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
            uid = FirebaseAuth.getInstance().currentUser?.uid ?: "",
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
            uid = FirebaseAuth.getInstance().currentUser?.uid ?: "",
            onBackClick = { navigationActions.goBack() },
            onProfileClick = { navigationActions.navigateTo(Screen.Profile) },
            onGroupClick = { navigationActions.navigateTo(Screen.Groups) },
            onSaveSuccess = { navigationActions.navigateTo(Screen.Profile) })
      }

      composable(Screen.Groups.route) {
        GroupListScreen(
            onJoinWithLink = {
              Toast.makeText(context, "Not yet implemented ", Toast.LENGTH_SHORT).show()
            }, // TODO navigate to join with link screen or popup
            onCreateGroup = { navigationActions.navigateTo(Screen.CreateGroup) },
            onGroup = {
              Toast.makeText(context, "Not yet implemented ", Toast.LENGTH_SHORT).show()
            }, // TODO navigate to group details screen
            onBackClick = { navigationActions.goBack() },
            onProfileClick = { navigationActions.navigateTo(Screen.Profile) },
            onEditClick = { navigationActions.navigateTo(Screen.EditProfile) },
            onViewGroupDetails = { navigationActions.navigateTo(Screen.GroupDetail(it.id)) },
            onLeaveGroup = {
              Toast.makeText(context, "Not yet implemented ", Toast.LENGTH_SHORT).show()
            },
            onShareGroup = {
              Toast.makeText(context, "Not yet implemented ", Toast.LENGTH_SHORT).show()
            },
            onEditGroup = { group -> navigationActions.navigateTo(Screen.EditGroup(group.id)) },
            onDeleteGroup = {
              Toast.makeText(context, "Not yet implemented ", Toast.LENGTH_SHORT).show()
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
              })
        }
      }
    }
  }
}
