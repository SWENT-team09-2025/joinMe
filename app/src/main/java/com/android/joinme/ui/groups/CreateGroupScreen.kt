package com.android.joinme.ui.groups

import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel

object CreateGroupScreenTestTags {
    const val SCREEN = "create_group_screen"
    const val TITLE = "create_group_title"
    const val GROUP_PICTURE = "create_group_picture_section"
    const val EDIT_PHOTO_BUTTON = "create_edit_photo_button"
    const val LOADING_INDICATOR = "create_group_loading_indicator"
    const val ERROR_MESSAGE = "create_group_error_message"
    const val GROUP_NAME_TEXT_FIELD = "create_group_screen_group_name_text_field"
    const val GROUP_DESCRIPTION_TEXT_FIELD = "create_group_screen_group_description_text_field"
    const val CATEGORY_DROPDOWN = "create_group_screen_category_dropdown"
    const val SAVE_BUTTON = "create_group_screen_save_button"
    const val NAME_SUPPORTING_TEXT = "create_name_supporting_text"
    const val DESCRIPTION_SUPPORTING_TEXT = "create_description_supporting_text"
    const val CATEGORY_MENU = "create_category_menu"
    const val CATEGORY_OPTION_PREFIX = "create_category_option_"

    const val DELETE_PHOTO_BUTTON = "delete_photo_button"
}

/**
 * Create Group Screen
 *
 * Allows users to create a new group with:
 * - Group name (required, 3-30 characters)
 * - Category (Social/Activity/Sports)
 * - Description (optional, max 300 characters)
 * - Group photo (not yet implemented)
 *
 * @param viewModel The ViewModel managing the create group state
 * @param onBackClick Callback when user navigates back
 * @param onCreateSuccess Callback when group is successfully created
 */
@Composable
fun CreateGroupScreen(
    viewModel: CreateGroupViewModel = viewModel(),
    onBackClick: () -> Unit = {},
    onCreateSuccess: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Handle success navigation
    LaunchedEffect(uiState.createdGroupId) {
        uiState.createdGroupId?.let {
            onCreateSuccess()
            viewModel.clearSuccessState()
        }
    }

    // Handle error messages
    LaunchedEffect(uiState.errorMsg) {
        uiState.errorMsg?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            viewModel.clearErrorMsg()
        }
    }

    val testTags =
        GroupFormTestTags(
            screen = CreateGroupScreenTestTags.SCREEN,
            title = CreateGroupScreenTestTags.TITLE,
            groupPicture = CreateGroupScreenTestTags.GROUP_PICTURE,
            editPhotoButton = CreateGroupScreenTestTags.EDIT_PHOTO_BUTTON,
            loadingIndicator = CreateGroupScreenTestTags.LOADING_INDICATOR,
            errorMessage = CreateGroupScreenTestTags.ERROR_MESSAGE,
            groupNameTextField = CreateGroupScreenTestTags.GROUP_NAME_TEXT_FIELD,
            categoryDropdown = CreateGroupScreenTestTags.CATEGORY_DROPDOWN,
            groupDescriptionTextField = CreateGroupScreenTestTags.GROUP_DESCRIPTION_TEXT_FIELD,
            saveButton = CreateGroupScreenTestTags.SAVE_BUTTON,
            nameSupportingText = CreateGroupScreenTestTags.NAME_SUPPORTING_TEXT,
            descriptionSupportingText = CreateGroupScreenTestTags.DESCRIPTION_SUPPORTING_TEXT,
            categoryMenu = CreateGroupScreenTestTags.CATEGORY_MENU,
            categoryOptionPrefix = CreateGroupScreenTestTags.CATEGORY_OPTION_PREFIX,
            deletePhotoButton = CreateGroupScreenTestTags.DELETE_PHOTO_BUTTON
        )

    val formState =
        GroupFormState(
            name = uiState.name,
            category = uiState.category,
            description = uiState.description,
            nameError = uiState.nameError,
            descriptionError = uiState.descriptionError,
            isValid = uiState.isValid,
            isLoading = uiState.isLoading,
            errorMsg = uiState.errorMsg
        )

    GroupFormScreen(
        title = "Create Group",
        formState = formState,
        testTags = testTags,
        onNameChange = { viewModel.setName(it) },
        onCategoryChange = { viewModel.setCategory(it) },
        onDescriptionChange = { viewModel.setDescription(it) },
        onSave = { viewModel.createGroup() },
        onGoBack = onBackClick,
        saveButtonText = "Create Group",
        onPictureEditClick = {
            Toast.makeText(context, "Not yet implemented", Toast.LENGTH_SHORT).show()
        })
}
