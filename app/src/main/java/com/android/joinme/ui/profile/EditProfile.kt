package com.android.joinme.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.joinme.model.profile.Profile
import com.android.joinme.ui.theme.JoinMeColor
import com.google.firebase.Timestamp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    uid: String,
    profileViewModel: ProfileViewModel = viewModel(),
    onBackClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onGroupClick: () -> Unit = {},
    onChangePasswordClick: () -> Unit = {},
    onSaveSuccess: () -> Unit = {}
) {
    val profile by profileViewModel.profile.collectAsState()
    val isLoading by profileViewModel.isLoading.collectAsState()

    // Form state
    var username by remember { mutableStateOf("") }
    var dateOfBirth by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }
    var interests by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }

    // Validation states
    var usernameError by remember { mutableStateOf<String?>(null) }
    var dateOfBirthError by remember { mutableStateOf<String?>(null) }
    var hasInteracted by remember { mutableStateOf(false) }

    // Load profile data
    LaunchedEffect(uid) {
        profileViewModel.loadProfile(uid)
    }

    LaunchedEffect(profile) {
        profile?.let {
            username = it.username
            dateOfBirth = it.dateOfBirth ?: ""
            country = it.country ?: ""
            interests = it.interests.joinToString(", ")
            bio = it.bio ?: ""
        }
    }

    // Validate on changes
    LaunchedEffect(username, dateOfBirth) {
        if (hasInteracted) {
            usernameError = profileViewModel.getUsernameError(username)
            dateOfBirthError = profileViewModel.getDateOfBirthError(dateOfBirth)
        }
    }

    val isFormValid = usernameError == null &&
            dateOfBirthError == null &&
            username.isNotEmpty()

    Scaffold(
        containerColor = Color.White,
        topBar = {
            ProfileTopBar(
                currentScreen = ProfileScreen.EDIT_PROFILE,
                onBackClick = onBackClick,
                onProfileClick = onProfileClick,
                onGroupClick = onGroupClick,
                onEditClick = {}
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                profile != null -> {
                    EditProfileContent(
                        profile = profile!!,
                        username = username,
                        onUsernameChange = {
                            username = it
                            hasInteracted = true
                        },
                        usernameError = usernameError,
                        dateOfBirth = dateOfBirth,
                        onDateOfBirthChange = {
                            dateOfBirth = it
                            hasInteracted = true
                        },
                        dateOfBirthError = dateOfBirthError,
                        country = country,
                        onCountryChange = { country = it },
                        interests = interests,
                        onInterestsChange = { interests = it },
                        bio = bio,
                        onBioChange = { bio = it },
                        isFormValid = isFormValid,
                        onChangePasswordClick = onChangePasswordClick,
                        onSaveClick = {
                            if (isFormValid) {
                                val updatedProfile = profile!!.copy(
                                    username = username,
                                    dateOfBirth = dateOfBirth.ifBlank { null },
                                    country = country.ifBlank { null },
                                    interests = interests
                                        .split(",")
                                        .map { it.trim() }
                                        .filter { it.isNotEmpty() },
                                    bio = bio.ifBlank { null }
                                )
                                profileViewModel.createOrUpdateProfile(updatedProfile)
                                onSaveSuccess()
                            }
                        }
                    )
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

@Composable
private fun EditProfileContent(
    profile: Profile,
    username: String,
    onUsernameChange: (String) -> Unit,
    usernameError: String?,
    dateOfBirth: String,
    onDateOfBirthChange: (String) -> Unit,
    dateOfBirthError: String?,
    country: String,
    onCountryChange: (String) -> Unit,
    interests: String,
    onInterestsChange: (String) -> Unit,
    bio: String,
    onBioChange: (String) -> Unit,
    isFormValid: Boolean,
    onChangePasswordClick: () -> Unit,
    onSaveClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        // Title
        Text(
            text = "Edit Profile",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        // Profile Picture with edit icon
        // TODO: Implement photo upload functionality
        ProfilePictureSection()

        Spacer(modifier = Modifier.height(8.dp))

        // Username field
        EditTextField(
            label = "Username",
            value = username,
            onValueChange = onUsernameChange,
            isError = usernameError != null,
            errorMessage = usernameError,
            supportingText = "3-30 characters. Letters, numbers, spaces, or underscores only"
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Email field (read-only)
        EditTextField(
            label = "Email",
            value = profile.email,
            onValueChange = {},
            enabled = false
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Date of Birth and Interests Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Date of Birth
            Column(modifier = Modifier.weight(1f)) {
                EditTextField(
                    label = "Date of Birth",
                    value = dateOfBirth,
                    onValueChange = onDateOfBirthChange,
                    isError = dateOfBirthError != null,
                    errorMessage = dateOfBirthError,
                    supportingText = "dd/mm/yyyy"
                )
            }

            // Interests
            // TODO: Replace with multi-select chip/tag component
            Column(modifier = Modifier.weight(1f)) {
                EditTextField(
                    label = "Interests",
                    value = interests,
                    onValueChange = onInterestsChange,
                    placeholder = "Golf, Nature"
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Password field
        PasswordSection(onChangePasswordClick = onChangePasswordClick)

        Spacer(modifier = Modifier.height(16.dp))

        // Country/Region
        // TODO: Replace with dropdown/autocomplete with country list
        EditTextField(
            label = "Country/Region",
            value = country,
            onValueChange = onCountryChange,
            placeholder = "Nigeria"
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Bio
        BioSection(bio = bio, onBioChange = onBioChange)

        Spacer(modifier = Modifier.height(32.dp))

        // Save Button
        Button(
            onClick = onSaveClick,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = isFormValid,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isFormValid) JoinMeColor else Color.LightGray,
                disabledContainerColor = Color.LightGray
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                "SAVE",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun ProfilePictureSection() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.size(140.dp),
            contentAlignment = Alignment.Center
        ) {
            // TODO: Load actual profile picture from URL if available
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = "Profile Picture",
                modifier = Modifier.size(140.dp).blur(5.dp),
                tint = JoinMeColor
            )
            Button(
                onClick = { /* TODO: Implement photo upload */ },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                ),
                shape = CircleShape,
                modifier = Modifier.size(120.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "Edit Photo",
                        tint = Color.White,
                        modifier = Modifier.size(56.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PasswordSection(onChangePasswordClick: () -> Unit) {
    Text(
        text = "Password",
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 8.dp)
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.LightGray, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "************",
                    fontSize = 16.sp,
                    color = Color.Gray
                )
            }

            Button(
                onClick = onChangePasswordClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = JoinMeColor
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.height(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Change password", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun BioSection(bio: String, onBioChange: (String) -> Unit) {
    Column {
        Text(
            text = "Bio",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        OutlinedTextField(
            value = bio,
            onValueChange = onBioChange,
            modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
            placeholder = {
                Text(
                    "Tell others a bit about yourself — your passions, what you enjoy doing, or what you're looking for on JoinMe.",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color.LightGray,
                focusedBorderColor = JoinMeColor
            ),
            shape = RoundedCornerShape(12.dp)
        )
        Text(
            "Tell others a bit about yourself — your passions, what you enjoy doing, or what you're looking for on JoinMe.",
            fontSize = 11.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun EditTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    errorMessage: String? = null,
    supportingText: String? = null,
    placeholder: String? = null
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            isError = isError,
            placeholder = placeholder?.let { { Text(it, color = Color.LightGray) } },
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = if (isError) Color.Red else Color.LightGray,
                focusedBorderColor = if (isError) Color.Red else JoinMeColor,
                disabledBorderColor = Color.LightGray,
                disabledTextColor = Color.Gray
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        val textToShow = if (isError && errorMessage != null) errorMessage else supportingText

        if (textToShow != null) {
            Text(
                text = textToShow,
                fontSize = 11.sp,
                color = if (isError) Color.Red else Color.Gray,
                modifier = Modifier.padding(start = 12.dp, top = 4.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EditProfileScreenPreview() {
    val mockProfile = Profile(
        uid = "preview-uid",
        username = "Mathieu Pfeffer",
        email = "pfeffer@gmail.com",
        dateOfBirth = "12/12/2012",
        country = "Nigeria",
        interests = listOf("Golf", "Nature"),
        bio = "I am a EPFL student, 21 and I like horses and golf.",
        createdAt = Timestamp.now(),
        updatedAt = Timestamp.now()
    )

    Scaffold(
        containerColor = Color.White,
        topBar = {
            ProfileTopBar(
                currentScreen = ProfileScreen.EDIT_PROFILE,
                onBackClick = {},
                onProfileClick = {},
                onGroupClick = {},
                onEditClick = {}
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            EditProfileContent(
                profile = mockProfile,
                username = "Mathieu Pfeffer",
                onUsernameChange = {},
                usernameError = null,
                dateOfBirth = "12/12/2012",
                onDateOfBirthChange = {},
                dateOfBirthError = null,
                country = "Nigeria",
                onCountryChange = {},
                interests = "Golf, Nature",
                onInterestsChange = {},
                bio = "I am a EPFL student, 21 and I like horses and golf.",
                onBioChange = {},
                isFormValid = true,
                onChangePasswordClick = {},
                onSaveClick = {}
            )
        }
    }
}