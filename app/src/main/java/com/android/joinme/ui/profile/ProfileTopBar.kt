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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.android.joinme.ui.theme.JoinMeColor

enum class ProfileScreen {
  VIEW_PROFILE,
  GROUPS,
  EDIT_PROFILE
}

@Composable
fun ProfileTopBar(
    currentScreen: ProfileScreen,
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
          IconButton(onClick = onBackClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                modifier = Modifier.size(24.dp))
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
                      if (currentScreen == ProfileScreen.VIEW_PROFILE) JoinMeColor
                      else Color.LightGray)
            }

            // Group Icon
            IconButton(onClick = onGroupClick) {
              Icon(
                  imageVector = Icons.Filled.Group,
                  contentDescription = "Group",
                  modifier =
                      Modifier.size(if (currentScreen == ProfileScreen.GROUPS) 32.dp else 24.dp),
                  tint =
                      if (currentScreen == ProfileScreen.GROUPS) JoinMeColor else Color.LightGray)
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
                      if (currentScreen == ProfileScreen.EDIT_PROFILE) JoinMeColor
                      else Color.LightGray)
            }
          }
        }

    HorizontalDivider(color = Color.Black, thickness = 1.dp)
  }
}
