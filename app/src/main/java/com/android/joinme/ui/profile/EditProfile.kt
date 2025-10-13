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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.joinme.model.profile.Profile
import com.android.joinme.ui.navigation.BottomNavigationMenu
import com.android.joinme.ui.navigation.Tab
import com.android.joinme.ui.theme.JoinMeColor
import com.google.firebase.Timestamp

object EditProfileTestTags {
    const val NO_LOADING_PROFILE_MESSAGE = "noLoadingProfileMessage"
    const val SCREEN = "editProfileScreen"
    const val LOADING_INDICATOR = "editProfileLoadingIndicator"
    const val TITLE = "editProfileTitle"
    const val PROFILE_PICTURE = "editProfilePicture"
    const val EDIT_PHOTO_BUTTON = "editProfilePhotoButton"
    const val USERNAME_FIELD = "editProfileUsernameField"
    const val USERNAME_ERROR = "editProfileUsernameError"
    const val EMAIL_FIELD = "editProfileEmailField"
    const val DATE_OF_BIRTH_FIELD = "editProfileDateOfBirthField"
    const val DATE_OF_BIRTH_ERROR = "editProfileDateOfBirthError"
    const val INTERESTS_FIELD = "editProfileInterestsField"
    const val PASSWORD_SECTION = "editProfilePasswordSection"
    const val CHANGE_PASSWORD_BUTTON = "editProfileChangePasswordButton"
    const val COUNTRY_FIELD = "editProfileCountryField"
    const val BIO_FIELD = "editProfileBioField"
    const val SAVE_BUTTON = "editProfileSaveButton"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    uid: String,
    profileViewModel: ProfileViewModel = viewModel(),
    onTabSelected: (Tab) -> Unit = {},
    onBackClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onGroupClick: () -> Unit = {},
    onChangePasswordClick: () -> Unit = {},
    onSaveSuccess: () -> Unit = {}
) {
    val profile by profileViewModel.profile.collectAsState()
    val isLoading by profileViewModel.isLoading.collectAsState()

    var username by remember { mutableStateOf("") }
    var dateOfBirth by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }
    var interests by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }

    var usernameError by remember { mutableStateOf<String?>(null) }
    var dateOfBirthError by remember { mutableStateOf<String?>(null) }
    var hasInteracted by remember { mutableStateOf(false) }

    LaunchedEffect(uid) { profileViewModel.loadProfile(uid) }

    LaunchedEffect(profile) {
        profile?.let {
            username = it.username
            dateOfBirth = it.dateOfBirth ?: ""
            country = it.country ?: ""
            interests = it.interests.joinToString(", ")
            bio = it.bio ?: ""
        }
    }

    LaunchedEffect(username, dateOfBirth) {
        if (hasInteracted) {
            usernameError = profileViewModel.getUsernameError(username)
            dateOfBirthError = profileViewModel.getDateOfBirthError(dateOfBirth)
        }
    }

    val isFormValid = usernameError == null && dateOfBirthError == null && username.isNotEmpty()

    Scaffold(
        modifier = Modifier.testTag(EditProfileTestTags.SCREEN),
        containerColor = Color.White,
        topBar = {
            ProfileTopBar(
                currentScreen = ProfileScreen.EDIT_PROFILE,
                onBackClick = onBackClick,
                onProfileClick = onProfileClick,
                onGroupClick = onGroupClick,
                onEditClick = {})
        },
        bottomBar = {
            BottomNavigationMenu(selectedTab = Tab.Profile, onTabSelected = onTabSelected)
        }) { padding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(padding)) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier =
                            Modifier
                                .align(Alignment.Center)
                                .testTag(EditProfileTestTags.LOADING_INDICATOR)
                    )
                }

                profile != null -> {
                    EditProfileContent(
                        profile = profile!!,
                        onPictureEditClick = {},
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
                                val updatedProfile =
                                    profile!!.copy(
                                        username = username,
                                        dateOfBirth = dateOfBirth.ifBlank { null },
                                        country = country.ifBlank { null },
                                        interests =
                                            interests.split(",").map { it.trim() }
                                                .filter { it.isNotEmpty() },
                                        bio = bio.ifBlank { null })
                                profileViewModel.createOrUpdateProfile(updatedProfile)
                                onSaveSuccess()
                            }
                        })
                }

                else -> {
                    Text(
                        text = "No profile data available",
                        modifier =
                            Modifier
                                .align(Alignment.Center)
                                .testTag(EditProfileTestTags.NO_LOADING_PROFILE_MESSAGE)
                    )
                }
            }
        }
    }
}

@Composable
private fun EditProfileContent(
    profile: Profile,
    onPictureEditClick: () -> Unit,
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
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
    ) {
        Text(
            text = "Edit Profile",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(vertical = 16.dp)
                .testTag(EditProfileTestTags.TITLE)
        )

        ProfilePictureSection(onPictureEditClick)

        Spacer(modifier = Modifier.height(8.dp))

        EditTextField(
            label = "Username",
            value = username,
            onValueChange = onUsernameChange,
            isError = usernameError != null,
            errorMessage = usernameError,
            supportingText = "3-30 characters. Letters, numbers, spaces, or underscores only",
            testTag = EditProfileTestTags.USERNAME_FIELD,
            errorTestTag = EditProfileTestTags.USERNAME_ERROR
        )

        Spacer(modifier = Modifier.height(16.dp))

        EditTextField(
            label = "Email",
            value = profile.email,
            onValueChange = {},
            enabled = false,
            testTag = EditProfileTestTags.EMAIL_FIELD
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                EditTextField(
                    label = "Date of Birth",
                    value = dateOfBirth,
                    onValueChange = onDateOfBirthChange,
                    isError = dateOfBirthError != null,
                    errorMessage = dateOfBirthError,
                    supportingText = "dd/mm/yyyy",
                    testTag = EditProfileTestTags.DATE_OF_BIRTH_FIELD,
                    errorTestTag = EditProfileTestTags.DATE_OF_BIRTH_ERROR
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                EditTextField(
                    label = "Interests",
                    value = interests,
                    onValueChange = onInterestsChange,
                    placeholder = "Golf, Nature",
                    testTag = EditProfileTestTags.INTERESTS_FIELD
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        PasswordSection(onChangePasswordClick = onChangePasswordClick)

        Spacer(modifier = Modifier.height(16.dp))

        EditTextField(
            label = "Country/Region",
            value = country,
            onValueChange = onCountryChange,
            placeholder = "Nigeria",
            testTag = EditProfileTestTags.COUNTRY_FIELD
        )

        Spacer(modifier = Modifier.height(16.dp))

        BioSection(bio = bio, onBioChange = onBioChange)

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onSaveClick,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag(EditProfileTestTags.SAVE_BUTTON),
            enabled = isFormValid,
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = if (isFormValid) JoinMeColor else Color.LightGray,
                    disabledContainerColor = Color.LightGray
                ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("SAVE", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun ProfilePictureSection(onPictureEditClick: () -> Unit = {}) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp)
                .testTag(EditProfileTestTags.PROFILE_PICTURE),
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = Modifier.size(140.dp), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = "Profile Picture",
                modifier = Modifier
                    .size(140.dp)
                    .blur(5.dp),
                tint = JoinMeColor
            )
            Button(
                onClick = { onPictureEditClick() },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                shape = CircleShape,
                modifier = Modifier
                    .size(120.dp)
                    .testTag(EditProfileTestTags.EDIT_PHOTO_BUTTON)
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
    Column(modifier = Modifier.testTag(EditProfileTestTags.PASSWORD_SECTION)) {
        Text(
            text = "Password",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Box(
            modifier =
                Modifier
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
                    Text(text = "************", fontSize = 16.sp, color = Color.Gray)
                }

                Button(
                    onClick = onChangePasswordClick,
                    colors = ButtonDefaults.buttonColors(containerColor = JoinMeColor),
                    shape = RoundedCornerShape(24.dp),
                    modifier =
                        Modifier
                            .height(40.dp)
                            .testTag(EditProfileTestTags.CHANGE_PASSWORD_BUTTON)
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
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp)
                    .testTag(EditProfileTestTags.BIO_FIELD),
            placeholder = {
                Text(
                    "Tell others a bit about yourself — your passions, what you enjoy doing, or what you're looking for on JoinMe.",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            },
            colors =
                OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.LightGray, focusedBorderColor = JoinMeColor
                ),
            shape = RoundedCornerShape(12.dp)
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
    placeholder: String? = null,
    testTag: String = "",
    errorTestTag: String = ""
) {
    Column {
        Text(
            text = label,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier =
                if (testTag.isNotEmpty()) modifier
                    .fillMaxWidth()
                    .testTag(testTag)
                else modifier.fillMaxWidth(),
            enabled = enabled,
            isError = isError,
            placeholder = placeholder?.let { { Text(it, color = Color.LightGray) } },
            colors =
                OutlinedTextFieldDefaults.colors(
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
                modifier =
                    if (isError) Modifier.padding(start = 12.dp, top = 4.dp).testTag(errorTestTag)
                    else Modifier.padding(start = 12.dp, top = 4.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EditProfileScreenPreview() {
    val mockProfile =
        Profile(
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
                onEditClick = {})
        },
        bottomBar = {
            BottomNavigationMenu(
                selectedTab = Tab.Profile,
                onTabSelected = {})
        }) { padding
        ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(padding)) {
            EditProfileContent(
                profile = mockProfile,
                username = "Mathieu Pfeffer",
                onPictureEditClick = {},
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
                onSaveClick = {})
        }
    }
}
