package com.android.joinme.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.sharp.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.joinme.model.profile.Profile
import com.android.joinme.ui.theme.JoinMeColor
import com.google.firebase.Timestamp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewProfileScreen(
    uid: String,
    profileViewModel: ProfileViewModel = viewModel(),
    onBackClick: () -> Unit = {},
    onGroupClick: () -> Unit = {},
    onEditClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {
    val profile by profileViewModel.profile.collectAsState()
    val isLoading by profileViewModel.isLoading.collectAsState()
    val error by profileViewModel.error.collectAsState()

    // Load profile when screen is first displayed
    LaunchedEffect(uid) { profileViewModel.loadProfile(uid) }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            ProfileTopBar(
                currentScreen = ProfileScreen.VIEW_PROFILE,
                onBackClick = onBackClick,
                onProfileClick = {}, // Already on profile screen
                onGroupClick = onGroupClick,
                onEditClick = onEditClick
            )
        }) { padding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(padding)) {
            when {
                isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                error != null -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = error ?: "Unknown error",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { profileViewModel.loadProfile(uid) }) { Text("Retry") }
                    }
                }

                profile != null -> {
                    ProfileContent(profile = profile!!, onLogoutClick = onLogoutClick)
                }

                else -> {
                    Text(
                        text = "No profile data available",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}

/** The main content of the profile screen, displaying profile details in read-only mode. */
@Composable
private fun ProfileContent(profile: Profile, onLogoutClick: () -> Unit) {
    Column(modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())) {
        // Profile Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Profile",
                color = JoinMeColor,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            OutlinedButton(
                onClick = onLogoutClick,
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(2.dp, JoinMeColor),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = JoinMeColor)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = "Logout",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("log out", fontWeight = FontWeight.Medium)
            }
        }

        // Profile Picture Placeholder
        // TODO: Replace with actual image picker and uploader
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Sharp.AccountCircle,
                contentDescription = "Profile Picture",
                modifier = Modifier.size(210.dp),
                tint = JoinMeColor
            )
        }

        // Form Fields (Read-only)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            ProfileField(label = "Username", value = profile.username)
            ProfileField(label = "Email", value = profile.email)
            ProfileField(label = "Date of Birth", value = profile.dateOfBirth ?: "Not specified")
            ProfileField(label = "Country/Region", value = profile.country ?: "Not specified")
            ProfileField(
                label = "Interests",
                value = profile.interests.joinToString(", ").ifEmpty { "None" })
            ProfileField(
                label = "Bio",
                value = profile.bio ?: "No bio available",
                minHeight = 120.dp
            )
        }
    }
}

/** A reusable composable for displaying a profile field with a label and value. */
@Composable
private fun ProfileField(label: String, value: String, minHeight: Dp = 56.dp) {
    Column {
        Text(
            text = label,
            color = JoinMeColor,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = minHeight)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, Color.LightGray, RoundedCornerShape(12.dp))
                    .padding(20.dp)
        ) {
            Text(text = value, fontSize = 16.sp, color = JoinMeColor)
        }
    }
}

// Preview with mock data
@Preview(showBackground = true)
@Composable
fun ViewProfileScreenPreview() {
    // Create a mock profile for preview
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
            updatedAt = Timestamp.now()
        )

    // Display the content directly without ViewModel for preview
    Scaffold(
        containerColor = Color.White,
        topBar = {
            ProfileTopBar(
                currentScreen = ProfileScreen.VIEW_PROFILE,
                onBackClick = {},
                onProfileClick = {}, // Already on profile screen
                onGroupClick = {},
                onEditClick = {})
        }) { padding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(padding)) {
            ProfileContent(profile = profileMathieu, onLogoutClick = {})
        }
    }
}
