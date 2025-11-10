package com.android.joinme.ui.groups

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import com.android.joinme.R
import com.android.joinme.model.event.EventType
import com.android.joinme.model.event.displayString
import com.android.joinme.ui.theme.Dimens
import com.android.joinme.ui.theme.TransparentColor
import com.android.joinme.ui.theme.buttonColors
import com.android.joinme.ui.theme.customColors
import com.android.joinme.ui.theme.outlinedTextField
import java.util.Locale

/** Data class representing the test tags for group form fields. */
data class GroupFormTestTags(
    val screen: String,
    val title: String,
    val groupPicture: String,
    val editPhotoButton: String,
    val deletePhotoButton: String,
    val loadingIndicator: String,
    val errorMessage: String,
    val groupNameTextField: String,
    val categoryDropdown: String,
    val groupDescriptionTextField: String,
    val saveButton: String,
    val nameSupportingText: String,
    val descriptionSupportingText: String,
    val categoryMenu: String,
    val categoryOptionPrefix: String
)

/** Data class representing the state of the group form. */
data class GroupFormState(
    val name: String,
    val category: EventType,
    val description: String,
    val nameError: String?,
    val descriptionError: String?,
    val isValid: Boolean,
    val isLoading: Boolean,
    val errorMsg: String?
)

/**
 * Generic group form screen component used by both Create and Edit group screens.
 *
 * @param title The title to display in the top bar
 * @param formState The current state of the form
 * @param testTags Test tags for UI testing
 * @param onNameChange Callback when name changes
 * @param onCategoryChange Callback when category changes
 * @param onDescriptionChange Callback when description changes
 * @param onSave Callback when save button is clicked
 * @param onGoBack Callback when back button is clicked
 * @param saveButtonText Text to display on the save button
 * @param onPictureEditClick Callback when picture edit button is clicked
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupFormScreen(
    title: String,
    formState: GroupFormState,
    testTags: GroupFormTestTags,
    onNameChange: (String) -> Unit,
    onCategoryChange: (EventType) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onSave: () -> Unit,
    onGoBack: () -> Unit,
    saveButtonText: String = "Save",
    onPictureEditClick: () -> Unit = {},
    onPictureDeleteClick: () -> Unit = {},
    groupPictureUrl: String? = null,
    isUploadingPicture: Boolean = false
) {
  Scaffold(
      modifier = Modifier.fillMaxSize().testTag(testTags.screen),
      topBar = {
        Column {
          CenterAlignedTopAppBar(
              title = {
                Text(
                    text = title,
                    modifier = Modifier.testTag(testTags.title),
                    style = MaterialTheme.typography.titleLarge)
              },
              navigationIcon = {
                IconButton(onClick = onGoBack) {
                  Icon(
                      imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                      contentDescription = "Back",
                      tint = MaterialTheme.colorScheme.primary)
                }
              },
              colors =
                  TopAppBarDefaults.topAppBarColors(
                      containerColor = MaterialTheme.colorScheme.surface))
          HorizontalDivider(
              color = MaterialTheme.colorScheme.primary,
              thickness = Dimens.History.dividerThickness)
        }
      }) { paddingValues ->
        when {
          formState.isLoading -> {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center) {
                  CircularProgressIndicator(
                      color = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.testTag(testTags.loadingIndicator))
                }
          }
          formState.errorMsg != null -> {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center) {
                  Text(
                      text = formState.errorMsg,
                      color = MaterialTheme.colorScheme.error,
                      modifier = Modifier.testTag(testTags.errorMessage))
                }
          }
          else -> {
            Column(
                modifier =
                    Modifier.fillMaxSize()
                        .padding(paddingValues)
                        .background(MaterialTheme.colorScheme.background)
                        .verticalScroll(rememberScrollState())
                        .padding(
                            horizontal = Dimens.Padding.screenHorizontal,
                            vertical = Dimens.Padding.screenVertical),
                horizontalAlignment = Alignment.CenterHorizontally) {
                  GroupPictureSection(
                      onPictureEditClick = onPictureEditClick,
                      onPictureDeleteClick = onPictureDeleteClick,
                      groupPictureUrl = groupPictureUrl,
                      isUploadingPicture = isUploadingPicture,
                      testTags = testTags)

                  Spacer(modifier = Modifier.height(Dimens.Spacing.extraLarge))

                  GroupNameInput(
                      value = formState.name,
                      onValueChange = onNameChange,
                      error = formState.nameError,
                      testTags = testTags)

                  Spacer(modifier = Modifier.height(Dimens.Spacing.fieldSpacing))

                  CategoryDropdown(
                      selectedCategory = formState.category,
                      onCategorySelected = onCategoryChange,
                      testTags = testTags)

                  Spacer(modifier = Modifier.height(Dimens.Spacing.fieldSpacing))

                  DescriptionInput(
                      value = formState.description,
                      onValueChange = onDescriptionChange,
                      error = formState.descriptionError,
                      testTags = testTags)

                  Spacer(modifier = Modifier.weight(1f))
                  Spacer(modifier = Modifier.height(Dimens.Group.saveButtonTopSpacing))

                  SaveButton(
                      enabled = formState.isValid,
                      onClick = onSave,
                      text = saveButtonText,
                      testTags = testTags)
                  Spacer(modifier = Modifier.height(Dimens.Spacing.medium))
                }
          }
        }
      }
}

/**
 * Displays the group picture section with edit and delete button overlays.
 *
 * This component shows a circular placeholder or actual image for the group with a blurred
 * background effect. Edit and delete icon buttons are overlaid on the image to allow users to
 * change or remove the group picture. The implementation mirrors ProfilePictureSection for
 * consistency.
 *
 * @param onPictureEditClick Callback invoked when the user taps the edit button to change the
 *   picture
 * @param onPictureDeleteClick Callback invoked when the user taps the delete button to remove the
 *   picture
 * @param groupPictureUrl Optional URL of the current group picture
 * @param isUploadingPicture Whether a picture upload is in progress
 * @param testTags Test tags for UI testing of the picture section and buttons
 */
@Composable
private fun GroupPictureSection(
    onPictureEditClick: () -> Unit,
    onPictureDeleteClick: () -> Unit,
    groupPictureUrl: String?,
    isUploadingPicture: Boolean,
    testTags: GroupFormTestTags
) {
  Box(
      modifier =
          Modifier.fillMaxWidth()
              .padding(vertical = Dimens.Group.picturePadding)
              .testTag(testTags.groupPicture),
      contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier.size(Dimens.Group.pictureLarge),
            contentAlignment = Alignment.Center) {
              // Show loading indicator while uploading
              if (isUploadingPicture) {
                CircularProgressIndicator(
                    modifier = Modifier.size(Dimens.Group.pictureLarge),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = Dimens.LoadingIndicator.strokeWidth)
              } else {
                Box(
                    modifier =
                        Modifier.size(Dimens.Group.pictureLarge)
                            .clip(CircleShape)
                            .blur(Dimens.Group.pictureBlurRadius),
                    contentAlignment = Alignment.Center) {
                      // Tint scrim for a frosted glass look
                      Box(modifier = Modifier.matchParentSize().background(TransparentColor)) {}

                      // TODO: Replace with actual GroupPhotoImage component when available
                      Image(
                          painter = painterResource(id = R.drawable.group_default_picture),
                          contentDescription = "Group picture",
                          modifier = Modifier.matchParentSize().clip(CircleShape))
                    }

                // Edit button overlay
                Button(
                    onClick = onPictureEditClick,
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = TransparentColor,
                            disabledContainerColor = TransparentColor),
                    shape = CircleShape,
                    modifier =
                        Modifier.size(Dimens.Group.editButtonSize)
                            .testTag(testTags.editPhotoButton)) {
                      Box(
                          modifier =
                              Modifier.size(Dimens.Group.editIconContainer)
                                  .clip(CircleShape)
                                  .background(TransparentColor),
                          contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = "Edit Picture",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(Dimens.Group.editIconSize))
                          }
                    }

                // Delete Icon Button (Bottom Right)
                // Show only if a picture exists and we are not loading
                if (groupPictureUrl != null && groupPictureUrl.isNotEmpty()) {
                  IconButton(
                      onClick = onPictureDeleteClick,
                      modifier =
                          Modifier.align(Alignment.BottomEnd)
                              .size(Dimens.Group.deleteButtonSize)
                              .clip(CircleShape)
                              .background(MaterialTheme.colorScheme.surface)
                              .border(
                                  Dimens.BorderWidth.thin,
                                  MaterialTheme.colorScheme.outlineVariant,
                                  CircleShape)
                              .testTag(testTags.deletePhotoButton)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Picture",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(Dimens.Group.deleteIconSize))
                      }
                }
              }
            }
      }
}

/**
 * Input field for the group name with validation support.
 *
 * This component displays a labeled text field for entering the group name. It provides real-time
 * validation feedback through visual cues (border color changes) and supporting text messages. The
 * field is restricted to a single line and validates that the name is 3-30 characters long and
 * contains only letters, numbers, spaces, or underscores.
 *
 * @param value The current value of the group name input
 * @param onValueChange Callback invoked when the user types in the text field
 * @param error Optional error message to display. When null, displays default helper text
 * @param testTags Test tags for UI testing of the text field and supporting text
 */
@Composable
private fun GroupNameInput(
    value: String,
    onValueChange: (String) -> Unit,
    error: String?,
    testTags: GroupFormTestTags
) {
  Column(modifier = Modifier.fillMaxWidth()) {
    Text(
        text = "Group name",
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(bottom = Dimens.TextField.labelSpacing))

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth().testTag(testTags.groupNameTextField),
        placeholder = { Text("Group name", color = MaterialTheme.colorScheme.onSurfaceVariant) },
        isError = error != null,
        singleLine = true,
        colors = MaterialTheme.customColors.outlinedTextField())

    Text(
        text = error ?: "3-30 characters. Letters, numbers, spaces, or underscores only",
        style = MaterialTheme.typography.bodySmall,
        color =
            if (error != null) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier =
            Modifier.padding(
                    start = Dimens.TextField.supportingTextPadding,
                    top = Dimens.TextField.supportingTextSpacing)
                .testTag(testTags.nameSupportingText))
  }
}

/**
 * Dropdown selector for the group category.
 *
 * This component displays a read-only text field that opens a dropdown menu when clicked, allowing
 * users to select from available EventType categories (SPORTS, ACTIVITY, SOCIAL). The dropdown is
 * implemented using Material3's OutlinedTextField with a trailing dropdown icon and a DropdownMenu
 * that displays all available event types. The text field is read-only to prevent direct text input
 * and ensure users can only select predefined categories.
 *
 * @param selectedCategory The currently selected EventType category
 * @param onCategorySelected Callback invoked when a user selects a category from the dropdown
 * @param testTags Test tags for UI testing of the dropdown field and menu items
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(
    selectedCategory: EventType,
    onCategorySelected: (EventType) -> Unit,
    testTags: GroupFormTestTags
) {
  var expanded by remember { mutableStateOf(false) }
  val eventTypes = EventType.values()

  Column(modifier = Modifier.fillMaxWidth()) {
    Text(
        text = "Category",
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(bottom = Dimens.TextField.labelSpacing))

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
      OutlinedTextField(
          value = selectedCategory.displayString(),
          onValueChange = {},
          modifier = Modifier.fillMaxWidth().menuAnchor().testTag(testTags.categoryDropdown),
          readOnly = true,
          placeholder = { Text("Group Type", color = MaterialTheme.colorScheme.onSurfaceVariant) },
          trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
          colors = MaterialTheme.customColors.outlinedTextField())

      ExposedDropdownMenu(
          expanded = expanded,
          onDismissRequest = { expanded = false },
          modifier = Modifier.background(MaterialTheme.customColors.backgroundMenu)) {
            eventTypes.forEachIndexed { index, type ->
              DropdownMenuItem(
                  text = {
                    Text(
                        text = type.displayString().uppercase(Locale.ROOT),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.headlineSmall)
                  },
                  onClick = {
                    onCategorySelected(type)
                    expanded = false
                  },
                  colors = MaterialTheme.customColors.dropdownMenu)
              if (index < eventTypes.lastIndex) {
                HorizontalDivider(thickness = Dimens.BorderWidth.thin)
              }
            }
          }
    }
  }
}

/**
 * Multi-line input field for the group description with validation support.
 *
 * This component displays a labeled text field for entering a longer description of the group. It
 * provides real-time validation feedback through visual cues (border color changes) and supporting
 * text messages. The field supports up to 4 lines of text and validates that the description is
 * 0-300 characters long and contains only letters, numbers, spaces, or underscores. Unlike the name
 * field, the description is optional.
 *
 * @param value The current value of the group description input
 * @param onValueChange Callback invoked when the user types in the text field
 * @param error Optional error message to display. When null, displays default helper text
 * @param testTags Test tags for UI testing of the text field and supporting text
 */
@Composable
private fun DescriptionInput(
    value: String,
    onValueChange: (String) -> Unit,
    error: String?,
    testTags: GroupFormTestTags
) {
  Column(modifier = Modifier.fillMaxWidth()) {
    Text(
        text = "Description",
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(bottom = Dimens.TextField.labelSpacing))

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier =
            Modifier.fillMaxWidth()
                .height(Dimens.Group.descriptionMinHeight)
                .testTag(testTags.groupDescriptionTextField),
        placeholder = {
          Text(
              "Tell us what your group is about",
              color = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        isError = error != null,
        maxLines = 4,
        colors = MaterialTheme.customColors.outlinedTextField())

    Text(
        text = error ?: "0-300 characters. Letters, numbers, spaces, or underscores only",
        style = MaterialTheme.typography.bodySmall,
        color =
            if (error != null) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier =
            Modifier.padding(
                    start = Dimens.TextField.supportingTextPadding,
                    top = Dimens.TextField.supportingTextSpacing)
                .testTag(testTags.descriptionSupportingText))
  }
}

/**
 * Save button for the group form with enabled/disabled states.
 *
 * This component displays a full-width button that triggers the form submission. The button
 * automatically disables itself when the form is invalid or during loading states. The text is
 * displayed in uppercase and the button uses theme colors for both enabled and disabled states to
 * maintain visual consistency with the app's design system. The disabled state uses muted colors to
 * provide clear visual feedback about button availability.
 *
 * @param enabled Whether the button should be enabled (typically based on form validation)
 * @param onClick Callback when the user clicks the save button
 * @param text The text to display on the button (e.g., "Save", "Create Group")
 * @param testTags Test tags for UI testing of the save button
 */
@Composable
private fun SaveButton(
    enabled: Boolean,
    onClick: () -> Unit,
    text: String,
    testTags: GroupFormTestTags
) {
  Button(
      onClick = onClick,
      enabled = enabled,
      modifier =
          Modifier.fillMaxWidth().height(Dimens.Button.standardHeight).testTag(testTags.saveButton),
      colors = MaterialTheme.customColors.buttonColors(),
      shape = MaterialTheme.shapes.medium) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold)
      }
}
