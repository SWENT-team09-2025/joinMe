package com.android.joinme

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import com.android.joinme.ui.groups.CreateGroupScreen
import com.android.joinme.ui.groups.GroupListScreen
import com.android.joinme.ui.history.HistoryScreen
import com.android.joinme.ui.map.MapScreen
import com.android.joinme.ui.map.MapViewModel
import com.android.joinme.ui.navigation.NavigationActions
import com.android.joinme.ui.navigation.Screen
import com.android.joinme.ui.overview.CreateEventScreen
import com.android.joinme.ui.overview.EditEventScreen
import com.android.joinme.ui.overview.OverviewScreen
import com.android.joinme.ui.overview.SearchScreen
import com.android.joinme.ui.overview.ShowEventScreen
import com.android.joinme.ui.profile.EditProfileScreen
import com.android.joinme.ui.profile.ViewProfileScreen
import com.android.joinme.ui.signIn.SignInScreen
import com.android.joinme.ui.theme.SampleAppTheme
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

    setContent { SampleAppTheme { Surface(modifier = Modifier.fillMaxSize()) { JoinMe() } } }
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
) {
  val navController = rememberNavController()
  val navigationActions = NavigationActions(navController)
  val initialDestination =
      startDestination
          ?: if (FirebaseAuth.getInstance().currentUser == null) Screen.Auth.name
          else Screen.Overview.route

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
            onSignedIn = { navigationActions.navigateTo(Screen.Overview) })
      }
    }

    // ============================================================================
    // Events & History
    // ============================================================================
    navigation(
        startDestination = Screen.Overview.route,
        route = Screen.Overview.name,
    ) {
      composable(Screen.Overview.route) {
        OverviewScreen(
            onSelectEvent = { navigationActions.navigateTo(Screen.ShowEventScreen(it.eventId)) },
            onAddEvent = { navigationActions.navigateTo(Screen.CreateEvent) },
            onGoToHistory = { navigationActions.navigateTo(Screen.History) },
            navigationActions = navigationActions,
            credentialManager = credentialManager)
      }
      composable(Screen.CreateEvent.route) {
        CreateEventScreen(
            onDone = { navigationActions.navigateTo(Screen.Overview) },
            onGoBack = { navigationActions.goBack() })
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
            onGoBack = { navigationActions.goBack() })
      }
      composable(Screen.ShowEventScreen.route) { navBackStackEntry ->
        val eventId = navBackStackEntry.arguments?.getString("eventId")

        eventId?.let {
          ShowEventScreen(
              eventId = eventId,
              onGoBack = { navigationActions.goBack() },
              onEditEvent = { id -> navigationActions.navigateTo(Screen.EditEvent(id)) })
        } ?: run { Toast.makeText(context, "Event UID is null", Toast.LENGTH_SHORT).show() }
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
            onSelectEvent = { navigationActions.navigateTo(Screen.ShowEventScreen(it.eventId)) })
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
            onSignOutComplete = { navigationActions.navigateTo(Screen.Auth) })
      }

      composable(Screen.EditProfile.route) {
        EditProfileScreen(
            uid = FirebaseAuth.getInstance().currentUser?.uid ?: "",
            onBackClick = { navigationActions.goBack() },
            onProfileClick = { navigationActions.navigateTo(Screen.Profile) },
            onGroupClick = { navigationActions.navigateTo(Screen.Groups) },
            onChangePasswordClick = {
              Toast.makeText(context, "Not yet implemented ", Toast.LENGTH_SHORT).show()
            }, // TODO implement change password flow in a future update
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
            onMoreOptionMenu = {
              Toast.makeText(context, "Not yet implemented ", Toast.LENGTH_SHORT).show()
            }, // TODO show more options menu
            onBackClick = { navigationActions.goBack() },
            onProfileClick = { navigationActions.navigateTo(Screen.Profile) },
            onEditClick = { navigationActions.navigateTo(Screen.EditProfile) })
      }

      composable(route = Screen.CreateGroup.route) {
        CreateGroupScreen(
            onNavigateBack = { navigationActions.goBack() },
            onGroupCreated = { navigationActions.navigateTo(Screen.Groups) })
      }
    }
  }
}
