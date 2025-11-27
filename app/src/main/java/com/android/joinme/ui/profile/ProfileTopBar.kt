package com.android.joinme.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * ProfileScreen enum defines the different profile-related screens in the application. Used to
 * determine which top bar configuration to display.
 */
enum class ProfileScreen {
  VIEW_PROFILE,
  GROUPS,
  EDIT_PROFILE
}

/**
 * ProfileTopBar displays a customized top app bar for profile-related screens.
 *
 * This composable renders different configurations based on the current profile screen
 *
 * The top bar follows Material Design 3 guidelines and uses the app's branded color scheme.
 *
 * @param currentScreen The current profile screen being displayed, determining which buttons to
 *   show.
 * @param showBackButton Whether to show the back button. Defaults to true.
 * @param onBackClick Callback invoked when the back navigation button is pressed.
 * @param onProfileClick Callback invoked when the profile icon is pressed (currently not shown).
 * @param onGroupClick Callback invoked when the group icon button is pressed.
 * @param onEditClick Callback invoked when the edit icon button is pressed. Only shown on
 *   VIEW_PROFILE.
 */
@Composable
fun ProfileTopBar(
    currentScreen: ProfileScreen,
    showBackButton: Boolean = true,
    onBackClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onGroupClick: () -> Unit = {},
    onEditClick: () -> Unit = {}
) {
  Column {
    // Navigation Bar
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
          if (showBackButton) {
            IconButton(onClick = onBackClick) {
              Icon(
                  imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                  contentDescription = "Back",
                  modifier = Modifier.size(24.dp),
                  tint = MaterialTheme.colorScheme.primary)
            }
          } else {
            // Empty spacer to maintain layout balance
            Spacer(modifier = Modifier.size(48.dp))
          }
          Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // Profile Icon
            IconButton(onClick = onProfileClick) {
              Icon(
                  imageVector = Icons.Default.Person,
                  contentDescription = "Profile",
                  modifier =
                      Modifier.size(
                          if (currentScreen == ProfileScreen.VIEW_PROFILE) 32.dp else 24.dp),
                  tint =
                      if (currentScreen == ProfileScreen.VIEW_PROFILE)
                          MaterialTheme.colorScheme.primary
                      else MaterialTheme.colorScheme.outlineVariant)
            }

            // Group Icon
            IconButton(onClick = onGroupClick) {
              Icon(
                  imageVector = Icons.Filled.Group,
                  contentDescription = "Group",
                  modifier =
                      Modifier.size(if (currentScreen == ProfileScreen.GROUPS) 32.dp else 24.dp),
                  tint =
                      if (currentScreen == ProfileScreen.GROUPS) MaterialTheme.colorScheme.primary
                      else MaterialTheme.colorScheme.outlineVariant)
            }

            // Edit Icon
            IconButton(onClick = onEditClick) {
              Icon(
                  imageVector = Icons.Default.Edit,
                  contentDescription = "Edit",
                  modifier =
                      Modifier.size(
                          if (currentScreen == ProfileScreen.EDIT_PROFILE) 32.dp else 24.dp),
                  tint =
                      if (currentScreen == ProfileScreen.EDIT_PROFILE)
                          MaterialTheme.colorScheme.primary
                      else MaterialTheme.colorScheme.outlineVariant)
            }
          }
        }

    HorizontalDivider(color = MaterialTheme.colorScheme.primary, thickness = 1.dp)
  }
}
