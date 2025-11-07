package com.android.joinme.ui.profile

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.joinme.model.profile.Profile
import com.android.joinme.ui.theme.Dimens
import com.android.joinme.ui.theme.ScrimOverlayColorLightTheme

/**
 * Test tags for EditProfileScreen components to enable UI testing.
 */
object EditProfileTestTags {
    const val NO_LOADING_PROFILE_MESSAGE = "noLoadingProfileMessage"
    const val SCREEN = "editProfileScreen"
    const val LOADING_INDICATOR = "editProfileLoadingIndicator"
    const val TITLE = "editProfileTitle"
    const val PROFILE_PICTURE = "editProfilePicture"
    const val EDIT_PHOTO_BUTTON = "editProfilePhotoButton"
    const val DELETE_PHOTO_BUTTON = "editProfileDeletePhotoButton"
    const val USERNAME_FIELD = "editProfileUsernameField"
    const val USERNAME_ERROR = "editProfileUsernameError"
    const val EMAIL_FIELD = "editProfileEmailField"
    const val DATE_OF_BIRTH_FIELD = "editProfileDateOfBirthField"
    const val DATE_OF_BIRTH_ERROR = "editProfileDateOfBirthError"
    const val INTERESTS_FIELD = "editProfileInterestsField"
    const val COUNTRY_FIELD = "editProfileCountryField"
    const val BIO_FIELD = "editProfileBioField"
    const val SAVE_BUTTON = "editProfileSaveButton"
    const val PHOTO_UPLOADING_INDICATOR = "editProfilePhotoUploadingIndicator"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    uid: String,
    profileViewModel: ProfileViewModel = viewModel(),
    onBackClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onGroupClick: () -> Unit = {},
    onSaveSuccess: () -> Unit = {}
) {
    val context = LocalContext.current
    val profile by profileViewModel.profile.collectAsState()
    val isLoading by profileViewModel.isLoading.collectAsState()
    val isUploadingPhoto by profileViewModel.isUploadingPhoto.collectAsState()
    val photoUploadError by profileViewModel.photoUploadError.collectAsState()

    var username by remember { mutableStateOf("") }
    var dateOfBirth by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }
    var interests by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }

    var usernameError by remember { mutableStateOf<String?>(null) }
    var dateOfBirthError by remember { mutableStateOf<String?>(null) }
    var hasInteracted by remember { mutableStateOf(false) }

    // Set up the photo picker
    val photoPicker =
        rememberPhotoPickerLauncher(
            onPhotoPicked = { uri ->
                profileViewModel.uploadProfilePhoto(
                    context = context,
                    imageUri = uri,
                    onSuccess = {
                        Toast.makeText(context, "Photo uploaded successfully", Toast.LENGTH_SHORT).show()
                    },
                    onError = { error -> Toast.makeText(context, error, Toast.LENGTH_LONG).show() })
            },
            onError = { error -> Toast.makeText(context, error, Toast.LENGTH_LONG).show() })

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

    // Show photo upload errors as toasts
    LaunchedEffect(photoUploadError) {
        photoUploadError?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            profileViewModel.clearPhotoUploadError()
        }
    }

    val isFormValid = usernameError == null && dateOfBirthError == null && username.isNotEmpty()

    Scaffold(
        modifier = Modifier.testTag(EditProfileTestTags.SCREEN),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            ProfileTopBar(
                currentScreen = ProfileScreen.EDIT_PROFILE,
                onBackClick = onBackClick,
                onProfileClick = onProfileClick,
                onGroupClick = onGroupClick,
                onEditClick = {})
        }) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier =
                            Modifier.align(Alignment.Center)
                                .testTag(EditProfileTestTags.LOADING_INDICATOR))
                }
                profile != null -> {
                    EditProfileContent(
                        profile = profile!!,
                        isUploadingPhoto = isUploadingPhoto,
                        onPictureEditClick = { photoPicker.launch() },
                        onPictureDeleteClick = {
                            profileViewModel.deleteProfilePhoto(
                                onSuccess = {
                                    Toast.makeText(context, "Photo deleted", Toast.LENGTH_SHORT).show()
                                },
                                onError = { error ->
                                    Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                                })
                        },
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
                        onSaveClick = {
                            if (isFormValid) {
                                val updatedProfile =
                                    profile!!.copy(
                                        username = username,
                                        dateOfBirth = dateOfBirth.ifBlank { null },
                                        country = country.ifBlank { null },
                                        interests =
                                            interests.split(",").map { it.trim() }.filter { it.isNotEmpty() },
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
                            Modifier.align(Alignment.Center)
                                .testTag(EditProfileTestTags.NO_LOADING_PROFILE_MESSAGE))
                }
            }
        }
    }
}

/**
 * Composable displaying the content of the Edit Profile screen.
 */
@Composable
private fun EditProfileContent(
    profile: Profile,
    isUploadingPhoto: Boolean,
    onPictureEditClick: () -> Unit,
    onPictureDeleteClick: () -> Unit,
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
    onSaveClick: () -> Unit
) {
    Column(
        modifier =
            Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.Padding.large)) {
        Text(
            text = "Edit Profile",
            fontSize = Dimens.FontSize.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(vertical = Dimens.Spacing.medium)
                .testTag(EditProfileTestTags.TITLE))

        ProfilePictureSection(
            photoUrl = profile.photoUrl,
            isUploadingPhoto = isUploadingPhoto,
            onPictureEditClick = onPictureEditClick,
            onPictureDeleteClick = onPictureDeleteClick)

        Spacer(modifier = Modifier.height(Dimens.Spacing.small))

        EditTextField(
            label = "Username",
            value = username,
            onValueChange = onUsernameChange,
            isError = usernameError != null,
            errorMessage = usernameError,
            supportingText = "3-30 characters. Letters, numbers, spaces, or underscores only",
            testTag = EditProfileTestTags.USERNAME_FIELD,
            errorTestTag = EditProfileTestTags.USERNAME_ERROR)

        Spacer(modifier = Modifier.height(Dimens.Spacing.medium))

        EditTextField(
            label = "Email",
            value = profile.email,
            onValueChange = {},
            enabled = false,
            testTag = EditProfileTestTags.EMAIL_FIELD)

        Spacer(modifier = Modifier.height(Dimens.Spacing.medium))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing.itemSpacing)) {
            Column(modifier = Modifier.weight(1f)) {
                EditTextField(
                    label = "Date of Birth",
                    value = dateOfBirth,
                    onValueChange = onDateOfBirthChange,
                    isError = dateOfBirthError != null,
                    errorMessage = dateOfBirthError,
                    supportingText = "dd/mm/yyyy",
                    testTag = EditProfileTestTags.DATE_OF_BIRTH_FIELD,
                    errorTestTag = EditProfileTestTags.DATE_OF_BIRTH_ERROR)
            }

            Column(modifier = Modifier.weight(1f)) {
                EditTextField(
                    label = "Interests",
                    value = interests,
                    onValueChange = onInterestsChange,
                    placeholder = "Golf, Nature",
                    testTag = EditProfileTestTags.INTERESTS_FIELD)
            }
        }

        Spacer(modifier = Modifier.height(Dimens.Spacing.medium))

        EditTextField(
            label = "Country/Region",
            value = country,
            onValueChange = onCountryChange,
            placeholder = "Nigeria",
            testTag = EditProfileTestTags.COUNTRY_FIELD)

        Spacer(modifier = Modifier.height(Dimens.Spacing.medium))

        BioSection(bio = bio, onBioChange = onBioChange)

        Spacer(modifier = Modifier.height(Dimens.Padding.extraLarge))

        Button(
            onClick = onSaveClick,
            modifier =
                Modifier.fillMaxWidth()
                    .height(Dimens.Button.standardHeight)
                    .testTag(EditProfileTestTags.SAVE_BUTTON),
            enabled = isFormValid,
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(Dimens.Button.cornerRadius)) {
            Text(
                "SAVE",
                fontSize = Dimens.FontSize.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary)
        }

        Spacer(modifier = Modifier.height(Dimens.Padding.extraLarge))
    }
}

/**
 * Composable displaying the profile picture section with edit and delete options.
 */
@Composable
private fun ProfilePictureSection(
    photoUrl: String?,
    isUploadingPhoto: Boolean,
    onPictureEditClick: () -> Unit,
    onPictureDeleteClick: () -> Unit
) {
    Box(
        modifier =
            Modifier.fillMaxWidth()
                .padding(vertical = Dimens.Profile.photoPadding)
                .testTag(EditProfileTestTags.PROFILE_PICTURE),
        contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier.size(Dimens.Profile.photoLarge),
            contentAlignment = Alignment.Center) {
            // Show loading indicator while uploading OR deleting
            if (isUploadingPhoto) {
                CircularProgressIndicator(
                    modifier =
                        Modifier.size(Dimens.Profile.photoLarge)
                            .testTag(EditProfileTestTags.PHOTO_UPLOADING_INDICATOR),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = Dimens.LoadingIndicator.strokeWidth)
            } else {
                // Display actual profile photo with blur effect
                ProfilePhotoImage(
                    photoUrl = photoUrl,
                    contentDescription = "Profile Picture",
                    size = Dimens.Profile.photoLarge,
                    showLoadingIndicator = false) // Use our own indicator

                // Blur overlay using a semi-transparent box
                Box(
                    modifier =
                        Modifier.size(Dimens.Profile.photoLarge)
                            .clip(CircleShape)
                            .background(ScrimOverlayColorLightTheme))

                // Edit button
                Button(
                    onClick = onPictureEditClick,
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0f),
                            disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0f)),
                    shape = CircleShape,
                    modifier =
                        Modifier.size(Dimens.Profile.editButtonSize)
                            .testTag(EditProfileTestTags.EDIT_PHOTO_BUTTON)) {
                    Box(
                        modifier =
                            Modifier.size(Dimens.Profile.editIconContainer)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0f)),
                        contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Edit Photo",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(Dimens.Profile.editIconSize))
                    }
                }

                // Delete Icon Button (Bottom Right)
                // Show only if a photo exists and we are not loading
                if (photoUrl != null && photoUrl.isNotEmpty()) {
                    IconButton(
                        onClick = onPictureDeleteClick,
                        modifier =
                            Modifier.align(Alignment.BottomEnd)
                                .size(Dimens.Profile.deleteButtonSize)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                                .border(
                                    Dimens.BorderWidth.thin,
                                    MaterialTheme.colorScheme.outlineVariant,
                                    CircleShape)
                                .testTag(EditProfileTestTags.DELETE_PHOTO_BUTTON)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Photo",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(Dimens.Profile.deleteIconSize))
                    }
                }
            }
        }
    }
}

/**
 * Composable for the Bio section with a multi-line text field.
 */
@Composable
private fun BioSection(bio: String, onBioChange: (String) -> Unit) {
    Column {
        Text(
            text = "Bio",
            fontSize = Dimens.FontSize.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = Dimens.TextField.labelSpacing))
        OutlinedTextField(
            value = bio,
            onValueChange = onBioChange,
            modifier =
                Modifier.fillMaxWidth()
                    .heightIn(min = Dimens.Profile.bioMinHeight)
                    .testTag(EditProfileTestTags.BIO_FIELD),
            placeholder = {
                Text(
                    "Tell others a bit about yourself – your passions, what you enjoy doing, or what you're looking for on JoinMe.",
                    fontSize = Dimens.FontSize.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            },
            colors =
                OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedBorderColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(Dimens.TextField.cornerRadius))
    }
}

/**
 * Composable for a labeled editable text field with error and supporting text.
 */
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
            fontSize = Dimens.FontSize.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = Dimens.TextField.labelSpacing))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier =
                if (testTag.isNotEmpty()) modifier.fillMaxWidth().testTag(testTag)
                else modifier.fillMaxWidth(),
            enabled = enabled,
            isError = isError,
            placeholder =
                placeholder?.let {
                    { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                },
            colors =
                OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor =
                        if (isError) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.outline,
                    focusedBorderColor =
                        if (isError) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary,
                    disabledBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant),
            shape = RoundedCornerShape(Dimens.TextField.cornerRadius),
            singleLine = true)

        val textToShow = if (isError && errorMessage != null) errorMessage else supportingText

        if (textToShow != null) {
            Text(
                text = textToShow,
                fontSize = Dimens.FontSize.labelSmall,
                color =
                    if (isError) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier =
                    if (isError)
                        Modifier.padding(
                            start = Dimens.TextField.supportingTextPadding,
                            top = Dimens.TextField.supportingTextSpacing)
                            .testTag(errorTestTag)
                    else
                        Modifier.padding(
                            start = Dimens.TextField.supportingTextPadding,
                            top = Dimens.TextField.supportingTextSpacing))
        }
    }
}
