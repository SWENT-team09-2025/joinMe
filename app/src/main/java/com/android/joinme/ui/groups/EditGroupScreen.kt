package com.android.joinme.ui.groups

import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel

object EditGroupScreenTags {
    const val SCREEN = "edit_group_screen"
    const val TITLE = "edit_group_title"
    const val GROUP_PICTURE = "group_picture_section"
    const val EDIT_PHOTO_BUTTON = "edit_photo_button"
    const val LOADING_INDICATOR = "edit_group_loading_indicator"
    const val ERROR_MESSAGE = "edit_group_error_message"
    const val GROUP_NAME_TEXT_FIELD = "edit_group_screen_group_name_text_field"
    const val GROUP_DESCRIPTION_TEXT_FIELD = "edit_group_screen_group_description_text_field"
    const val CATEGORY_DROPDOWN = "edit_group_screen_category_dropdown"
    const val SAVE_BUTTON = "edit_group_screen_save_button"
    const val NAME_SUPPORTING_TEXT = "name_supporting_text"
    const val DESCRIPTION_SUPPORTING_TEXT = "description_supporting_text"
    const val CATEGORY_MENU = "category_menu"
    const val CATEGORY_OPTION_PREFIX = "category_option_"

    const val DELETE_PHOTO_BUTTON = "delete_photo_button"
}

/**
 * Edit Group Screen
 *
 * Allows users to edit an existing group:
 * - Group name (required, 3-30 characters)
 * - Category (Social/Activity/Sports)
 * - Description (optional, max 300 characters)
 * - Group photo (not yet implemented)
 *
 * @param groupId The ID of the group to edit
 * @param viewModel The ViewModel managing the edit group state
 * @param onBackClick Callback when user navigates back
 * @param onSaveSuccess Callback when group is successfully updated
 */
@Composable
fun EditGroupScreen(
    groupId: String,
    viewModel: EditGroupViewModel = viewModel(),
    onBackClick: () -> Unit = {},
    onSaveSuccess: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(groupId) { viewModel.loadGroup(groupId) }

    // Handle success navigation
    LaunchedEffect(uiState.editedGroupId) {
        uiState.editedGroupId?.let {
            onSaveSuccess()
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
            screen = EditGroupScreenTags.SCREEN,
            title = EditGroupScreenTags.TITLE,
            groupPicture = EditGroupScreenTags.GROUP_PICTURE,
            editPhotoButton = EditGroupScreenTags.EDIT_PHOTO_BUTTON,
            loadingIndicator = EditGroupScreenTags.LOADING_INDICATOR,
            errorMessage = EditGroupScreenTags.ERROR_MESSAGE,
            groupNameTextField = EditGroupScreenTags.GROUP_NAME_TEXT_FIELD,
            categoryDropdown = EditGroupScreenTags.CATEGORY_DROPDOWN,
            groupDescriptionTextField = EditGroupScreenTags.GROUP_DESCRIPTION_TEXT_FIELD,
            saveButton = EditGroupScreenTags.SAVE_BUTTON,
            nameSupportingText = EditGroupScreenTags.NAME_SUPPORTING_TEXT,
            descriptionSupportingText = EditGroupScreenTags.DESCRIPTION_SUPPORTING_TEXT,
            categoryMenu = EditGroupScreenTags.CATEGORY_MENU,
            categoryOptionPrefix = EditGroupScreenTags.CATEGORY_OPTION_PREFIX,
            deletePhotoButton = EditGroupScreenTags.DELETE_PHOTO_BUTTON
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
        title = "Edit Group",
        formState = formState,
        testTags = testTags,
        onNameChange = { viewModel.setName(it) },
        onCategoryChange = { viewModel.setCategory(it) },
        onDescriptionChange = { viewModel.setDescription(it) },
        onSave = { viewModel.updateGroup(groupId) },
        onGoBack = onBackClick,
        saveButtonText = "Save Changes",
        onPictureEditClick = {
            Toast.makeText(context, "Not yet implemented", Toast.LENGTH_SHORT).show()
        })
}
