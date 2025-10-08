package com.android.joinme

import android.content.Context
import android.os.Bundle
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
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.android.joinme.model.authentification.AuthRepository
import com.android.joinme.navigation.NavigationActions
import com.android.joinme.navigation.Screen
import com.android.joinme.signIn.SignInScreen
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
) {
    val navController = rememberNavController()
    val navigationActions = NavigationActions(navController)
    val startDestination = Screen.Auth.name
        //if (FirebaseAuth.getInstance().currentUser == null)

    NavHost(navController = navController, startDestination = startDestination) {
        navigation(
            startDestination = Screen.Auth.route,
            route = Screen.Auth.name,
        ) {
            composable(Screen.Auth.route) {
                SignInScreen(
                    credentialManager = credentialManager,
                    onSignedIn = { navigationActions.navigateTo(Screen.SecondActivity) })
            }
        }
    }
}
