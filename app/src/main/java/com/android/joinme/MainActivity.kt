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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import com.android.joinme.model.authentification.AuthRepository
import com.android.joinme.ui.history.HistoryScreen
import com.android.joinme.ui.navigation.NavigationActions
import com.android.joinme.ui.navigation.Screen
import com.android.joinme.ui.overview.CreateEventScreen
import com.android.joinme.ui.overview.EditEventScreen
import com.android.joinme.ui.overview.OverviewScreen
import com.android.joinme.ui.overview.SearchScreen
import com.android.joinme.ui.signIn.SignInScreen
import com.android.joinme.ui.theme.SampleAppTheme
import com.google.firebase.auth.FirebaseAuth
import okhttp3.OkHttpClient

object HttpClientProvider {
  var client: OkHttpClient = OkHttpClient()
}

class MainActivity : ComponentActivity() {

  private lateinit var auth: FirebaseAuth
  private lateinit var authRepository: AuthRepository

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContent { SampleAppTheme { Surface(modifier = Modifier.fillMaxSize()) { JoinMe() } } }
  }
}

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

    navigation(
        startDestination = Screen.Overview.route,
        route = Screen.Overview.name,
    ) {
      composable(Screen.Overview.route) {
        OverviewScreen(
            onSelectEvent = {
              navigationActions.navigateTo(Screen.EditEvent(it.eventId))
            }, // to be modified need to naviagte to ShowEvent
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
            onSelectEvent = {}, // to be modified need to navigate to ShowEvent},
            onGoBack = { navigationActions.goBack() })
      }
    }

    navigation(
        startDestination = Screen.Search.route,
        route = Screen.Search.name,
    ) {
      composable(Screen.Search.route) { SearchScreen(navigationActions = navigationActions) }
    }

    navigation(
        startDestination = Screen.Map.route,
        route = Screen.Map.name,
    ) {
      composable(Screen.Map.route) {}
    }
    navigation(
        startDestination = Screen.Profile.route,
        route = Screen.Profile.name,
    ) {
      composable(Screen.Profile.route) {}
    }
  }
}
