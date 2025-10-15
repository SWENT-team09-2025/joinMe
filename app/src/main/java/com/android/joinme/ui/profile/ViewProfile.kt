package com.android.joinme.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.joinme.model.profile.Profile
import com.android.joinme.ui.navigation.BottomNavigationMenu
import com.android.joinme.ui.navigation.Tab
import com.android.joinme.ui.theme.JoinMeColor
import com.google.firebase.Timestamp

/**
 * ViewProfileTestTags contains test tag constants for the ViewProfile screen. These tags enable UI
 * testing by providing consistent identifiers for composables.
 */
object ViewProfileTestTags {
  const val SCREEN = "viewProfileScreen"
  const val LOADING_INDICATOR = "viewProfileLoadingIndicator"
  const val ERROR_MESSAGE = "viewProfileErrorMessage"
  const val RETRY_BUTTON = "viewProfileRetryButton"
  const val PROFILE_TITLE = "viewProfileTitle"
  const val LOGOUT_BUTTON = "viewProfileLogoutButton"
  const val SCROLL_CONTAINER = "viewProfileScrollContainer"
  const val PROFILE_PICTURE = "viewProfilePicture"
  const val USERNAME_FIELD = "viewProfileUsernameField"
  const val EMAIL_FIELD = "viewProfileEmailField"
  const val DATE_OF_BIRTH_FIELD = "viewProfileDateOfBirthField"
  const val COUNTRY_FIELD = "viewProfileCountryField"
  const val INTERESTS_FIELD = "viewProfileInterestsField"
  const val BIO_FIELD = "viewProfileBioField"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewProfileScreen(
    uid: String,
    profileViewModel: ProfileViewModel = viewModel(),
    onTabSelected: (Tab) -> Unit = {},
    onBackClick: () -> Unit = {},
    onGroupClick: () -> Unit = {},
    onEditClick: () -> Unit = {},
    onSignOutComplete: () -> Unit = {},
) {
  val profile by profileViewModel.profile.collectAsState()
  val isLoading by profileViewModel.isLoading.collectAsState()
  val error by profileViewModel.error.collectAsState()

  // Load profile when screen is first displayed
  LaunchedEffect(uid) { profileViewModel.loadProfile(uid) }

  Scaffold(
      modifier = Modifier.testTag(ViewProfileTestTags.SCREEN),
      containerColor = Color.White,
      topBar = {
        ProfileTopBar(
            currentScreen = ProfileScreen.VIEW_PROFILE,
            onBackClick = onBackClick,
            onProfileClick = {},
            onGroupClick = onGroupClick,
            onEditClick = onEditClick)
      },
      bottomBar = {
        BottomNavigationMenu(selectedTab = Tab.Profile, onTabSelected = onTabSelected)
      }) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
          when {
            isLoading -> {
              CircularProgressIndicator(
                  modifier =
                      Modifier.align(Alignment.Center)
                          .testTag(ViewProfileTestTags.LOADING_INDICATOR))
            }
            error != null -> {
              Column(
                  modifier =
                      Modifier.align(Alignment.Center)
                          .padding(24.dp)
                          .testTag(ViewProfileTestTags.ERROR_MESSAGE),
                  horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = error ?: "Unknown error",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { profileViewModel.loadProfile(uid) },
                        modifier = Modifier.testTag(ViewProfileTestTags.RETRY_BUTTON)) {
                          Text("Retry")
                        }
                  }
            }
            profile != null -> {
              ProfileContent(
                  profile = profile!!,
                  onLogoutClick = {
                    profileViewModel.signOut(
                        onSignOutComplete,
                        onError = { profileViewModel.setError("Error while logging out") })
                  })
            }
            else -> {
              Text(text = "No profile data available", modifier = Modifier.align(Alignment.Center))
            }
          }
        }
      }
}

/** The main content of the profile screen, displaying profile details in read-only mode. */
@Composable
private fun ProfileContent(profile: Profile, onLogoutClick: () -> Unit) {
  Column(
      modifier =
          Modifier.fillMaxSize()
              .verticalScroll(rememberScrollState())
              .testTag(ViewProfileTestTags.SCROLL_CONTAINER)) {

        // Profile Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
              Text(
                  text = "Profile",
                  color = JoinMeColor,
                  fontSize = 28.sp,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.testTag(ViewProfileTestTags.PROFILE_TITLE))
              OutlinedButton(
                  onClick = onLogoutClick,
                  shape = RoundedCornerShape(24.dp),
                  border = BorderStroke(2.dp, JoinMeColor),
                  colors = ButtonDefaults.outlinedButtonColors(contentColor = JoinMeColor),
                  modifier = Modifier.testTag(ViewProfileTestTags.LOGOUT_BUTTON)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = "Logout",
                        modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("log out", fontWeight = FontWeight.Medium)
                  }
            }

        // Profile Picture - Now displays actual photo if available
        Box(
            modifier =
                Modifier.fillMaxWidth()
                    .padding(vertical = 32.dp)
                    .testTag(ViewProfileTestTags.PROFILE_PICTURE),
            contentAlignment = Alignment.Center) {
              ProfilePhotoImage(
                  photoUrl = profile.photoUrl,
                  contentDescription = "Profile Picture",
                  size = 210.dp)
            }

        // Form Fields (Read-only)
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)) {
              ProfileField(
                  label = "Username",
                  value = profile.username,
                  testTag = ViewProfileTestTags.USERNAME_FIELD)
              ProfileField(
                  label = "Email", value = profile.email, testTag = ViewProfileTestTags.EMAIL_FIELD)
              ProfileField(
                  label = "Date of Birth",
                  value = profile.dateOfBirth ?: "Date not specified",
                  testTag = ViewProfileTestTags.DATE_OF_BIRTH_FIELD)
              ProfileField(
                  label = "Country/Region",
                  value = profile.country ?: "Country not specified",
                  testTag = ViewProfileTestTags.COUNTRY_FIELD)
              ProfileField(
                  label = "Interests",
                  value = profile.interests.joinToString(", ").ifEmpty { "None" },
                  testTag = ViewProfileTestTags.INTERESTS_FIELD)
              ProfileField(
                  label = "Bio",
                  value = profile.bio ?: "No bio available",
                  minHeight = 120.dp,
                  testTag = ViewProfileTestTags.BIO_FIELD)
            }
      }
}

/** A reusable composable for displaying a profile field with a label and value. */
@Composable
private fun ProfileField(
    label: String,
    value: String,
    minHeight: Dp = 56.dp,
    testTag: String = ""
) {
  Column(modifier = if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier) {
    Text(
        text = label,
        color = JoinMeColor,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 12.dp))
    Box(
        modifier =
            Modifier.fillMaxWidth()
                .heightIn(min = minHeight)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, Color.LightGray, RoundedCornerShape(12.dp))
                .padding(20.dp)) {
          Text(text = value, fontSize = 16.sp, color = JoinMeColor)
        }
  }
}

@Preview(showBackground = true)
@Composable
fun ViewProfileScreenPreview() {
  val profileMathieu =
      Profile(
          uid = "preview-uid",
          username = "Mathieu Pfeffer",
          email = "pfeffer@gmail.com",
          dateOfBirth = "23/05/1995",
          country = "Nigeria",
          interests = listOf("Golf", "Nature"),
          bio = "I am a EPFL student, 21 and I like horses and golf.",
          createdAt = Timestamp.now(),
          updatedAt = Timestamp.now())

  Scaffold(
      containerColor = Color.White,
      topBar = {
        ProfileTopBar(
            currentScreen = ProfileScreen.VIEW_PROFILE,
            onBackClick = {},
            onProfileClick = {},
            onGroupClick = {},
            onEditClick = {})
      },
      bottomBar = { BottomNavigationMenu(selectedTab = Tab.Profile, onTabSelected = {}) }) { padding
        ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
          ProfileContent(profile = profileMathieu, onLogoutClick = {})
        }
      }
}
