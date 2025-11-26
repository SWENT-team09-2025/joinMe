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
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.joinme.model.profile.Profile
import com.android.joinme.ui.navigation.BottomNavigationMenu
import com.android.joinme.ui.navigation.Tab
import com.android.joinme.ui.theme.Dimens

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
  const val LOGOUT_CONFIRM_DIALOG = "viewProfileLogoutConfirmDialog"
  const val LOGOUT_CONFIRM_BUTTON = "viewProfileLogoutConfirmButton"
  const val LOGOUT_CANCEL_BUTTON = "viewProfileLogoutCancelButton"
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
      containerColor = MaterialTheme.colorScheme.surface,
      topBar = {
        ProfileTopBar(
            currentScreen = ProfileScreen.VIEW_PROFILE,
            showBackButton = false,
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
                          .padding(Dimens.Padding.large)
                          .testTag(ViewProfileTestTags.ERROR_MESSAGE),
                  horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = error ?: "Unknown error",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(Dimens.Spacing.medium))
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
  var showLogoutDialog by remember { mutableStateOf(false) }

  Column(
      modifier =
          Modifier.fillMaxSize()
              .verticalScroll(rememberScrollState())
              .testTag(ViewProfileTestTags.SCROLL_CONTAINER)) {

        // Profile Header
        Row(
            modifier =
                Modifier.fillMaxWidth()
                    .padding(horizontal = Dimens.Padding.large, vertical = Dimens.Padding.large),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
              Text(
                  text = "Profile",
                  color = MaterialTheme.colorScheme.onSurface,
                  fontSize = Dimens.FontSize.headlineMedium,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.testTag(ViewProfileTestTags.PROFILE_TITLE))
              OutlinedButton(
                  onClick = { showLogoutDialog = true },
                  shape = RoundedCornerShape(Dimens.CornerRadius.pill),
                  border =
                      BorderStroke(Dimens.BorderWidth.medium, MaterialTheme.colorScheme.primary),
                  colors =
                      ButtonDefaults.outlinedButtonColors(
                          contentColor = MaterialTheme.colorScheme.primary),
                  modifier = Modifier.testTag(ViewProfileTestTags.LOGOUT_BUTTON)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = "Logout",
                        modifier = Modifier.size(Dimens.IconSize.medium))
                    Spacer(modifier = Modifier.width(Dimens.Spacing.small))
                    Text("LOG OUT", fontWeight = FontWeight.Medium)
                  }
            }

        // Profile Picture - Now displays actual photo if available
        Box(
            modifier =
                Modifier.fillMaxWidth()
                    .padding(vertical = Dimens.Padding.extraLarge)
                    .testTag(ViewProfileTestTags.PROFILE_PICTURE),
            contentAlignment = Alignment.Center) {
              ProfilePhotoImage(
                  photoUrl = profile.photoUrl,
                  contentDescription = "Profile Picture",
                  size = Dimens.Profile.photoExtraLarge)
            }

        // Form Fields (Read-only)
        Column(
            modifier =
                Modifier.fillMaxWidth()
                    .padding(horizontal = Dimens.Padding.large)
                    .padding(bottom = Dimens.Padding.extraLarge),
            verticalArrangement = Arrangement.spacedBy(Dimens.Spacing.fieldSpacing)) {
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
                  minHeight = Dimens.Profile.bioMinHeight,
                  testTag = ViewProfileTestTags.BIO_FIELD)
            }
      }

  // Logout confirmation dialog
  // Logout confirmation dialog
  if (showLogoutDialog) {
    AlertDialog(
        onDismissRequest = { showLogoutDialog = false },
        title = { Text("Log Out") },
        text = { Text("Are you sure you want to log out?") },
        confirmButton = {
          OutlinedButton(
              onClick = {
                showLogoutDialog = false
                onLogoutClick()
              },
              modifier = Modifier.testTag(ViewProfileTestTags.LOGOUT_CONFIRM_BUTTON),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)) {
                Text("Log Out", color = MaterialTheme.colorScheme.error)
              }
        },
        dismissButton = {
          OutlinedButton(
              onClick = { showLogoutDialog = false },
              modifier = Modifier.testTag(ViewProfileTestTags.LOGOUT_CANCEL_BUTTON),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
                Text("Cancel")
              }
        },
        modifier = Modifier.testTag(ViewProfileTestTags.LOGOUT_CONFIRM_DIALOG))
  }
}

/** A reusable composable for displaying a profile field with a label and value. */
@Composable
private fun ProfileField(
    label: String,
    value: String,
    minHeight: Dp = Dimens.Profile.fieldMinHeight,
    testTag: String = ""
) {
  Column(modifier = if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier) {
    Text(
        text = label,
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = Dimens.FontSize.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = Dimens.Profile.fieldLabelSpacing))
    Box(
        modifier =
            Modifier.fillMaxWidth()
                .heightIn(min = minHeight)
                .clip(RoundedCornerShape(Dimens.Profile.fieldCornerRadius))
                .border(
                    Dimens.BorderWidth.thin,
                    MaterialTheme.colorScheme.outline,
                    RoundedCornerShape(Dimens.Profile.fieldCornerRadius))
                .padding(Dimens.Profile.fieldInternalPadding)) {
          Text(
              text = value,
              fontSize = Dimens.FontSize.bodyLarge,
              color = MaterialTheme.colorScheme.onSurface)
        }
  }
}
