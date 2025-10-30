package com.android.joinme.ui.groups

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.joinme.R
import com.android.joinme.model.event.EventType
import com.android.joinme.model.event.displayString
import com.android.joinme.ui.navigation.NavigationTestTags
import com.android.joinme.ui.theme.*

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
@OptIn(ExperimentalMaterial3Api::class)
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

  Scaffold(
      modifier = Modifier.fillMaxSize().testTag(EditGroupScreenTags.SCREEN),
      topBar = {
        Column {
          CenterAlignedTopAppBar(
              modifier = Modifier.testTag(NavigationTestTags.TOP_BAR_TITLE),
              title = {
                Text(
                    text = "Edit Group",
                    modifier = Modifier.testTag(EditGroupScreenTags.TITLE),
                    style = MaterialTheme.typography.titleLarge)
              },
              navigationIcon = {
                IconButton(
                    onClick = { onBackClick() },
                    modifier = Modifier.testTag(NavigationTestTags.GO_BACK_BUTTON)) {
                      Icon(
                          imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                          contentDescription = "Back")
                    }
              },
              colors =
                  TopAppBarDefaults.topAppBarColors(
                      containerColor = MaterialTheme.colorScheme.surface))
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
        }
      }) { paddingValues ->
        when {
          uiState.isLoading -> {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center) {
                  CircularProgressIndicator(
                      modifier = Modifier.testTag(EditGroupScreenTags.LOADING_INDICATOR))
                }
          }
          uiState.errorMsg != null -> {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center) {
                  Text(
                      text = uiState.errorMsg ?: "Unknown error",
                      color = MaterialTheme.colorScheme.error,
                      modifier = Modifier.testTag(EditGroupScreenTags.ERROR_MESSAGE))
                }
          }
          else -> {
            Column(
                modifier =
                    Modifier.fillMaxSize()
                        .padding(paddingValues)
                        .background(MaterialTheme.colorScheme.background)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                  GroupPictureSection(
                      onPictureEditClick = {
                        Toast.makeText(context, "Not yet implemented", Toast.LENGTH_SHORT).show()
                      })

                  Spacer(modifier = Modifier.height(32.dp))

                  GroupNameInput(
                      value = uiState.name,
                      onValueChange = { viewModel.setName(it) },
                      error = uiState.nameError)

                  Spacer(modifier = Modifier.height(24.dp))

                  CategoryDropdown(
                      selectedCategory = uiState.category,
                      onCategorySelected = { viewModel.setCategory(it) })

                  Spacer(modifier = Modifier.height(24.dp))

                  DescriptionInput(
                      value = uiState.description,
                      onValueChange = { viewModel.setDescription(it) },
                      error = uiState.descriptionError)

                  Spacer(modifier = Modifier.weight(1f))
                  Spacer(modifier = Modifier.height(32.dp))

                  SaveButton(
                      enabled = uiState.isValid && !uiState.isLoading,
                      onClick = { viewModel.updateGroup(groupId) })
                  Spacer(modifier = Modifier.height(16.dp))
                }
          }
        }
      }
}

@Composable
private fun GroupPictureSection(onPictureEditClick: () -> Unit = {}) {
  Box(
      modifier =
          Modifier.fillMaxWidth()
              .padding(vertical = 24.dp)
              .testTag(EditGroupScreenTags.GROUP_PICTURE),
      contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.size(140.dp), contentAlignment = Alignment.Center) {
          Image(
              painter = painterResource(id = R.drawable.group_default_picture),
              contentDescription = "Group picture placeholder",
              modifier = Modifier.matchParentSize().blur(5.dp).clip(CircleShape))

          Button(
              onClick = { onPictureEditClick() },
              colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
              shape = CircleShape,
              modifier = Modifier.size(120.dp).testTag(EditGroupScreenTags.EDIT_PHOTO_BUTTON)) {
                Box(
                    modifier = Modifier.size(50.dp).clip(CircleShape).background(Color.Transparent),
                    contentAlignment = Alignment.Center) {
                      Icon(
                          imageVector = Icons.Outlined.Edit,
                          contentDescription = "Edit Photo",
                          tint = MaterialTheme.colorScheme.onSurface,
                          modifier = Modifier.size(56.dp))
                    }
              }
        }
      }
}

@Composable
private fun GroupNameInput(value: String, onValueChange: (String) -> Unit, error: String?) {
  Column(modifier = Modifier.fillMaxWidth()) {
    Text(
        text = "Group name",
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(bottom = 8.dp))

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth().testTag(EditGroupScreenTags.GROUP_NAME_TEXT_FIELD),
        placeholder = { Text("Group name", color = MaterialTheme.colorScheme.onSurfaceVariant) },
        isError = error != null,
        singleLine = true,
        colors =
            OutlinedTextFieldDefaults.colors(
                focusedBorderColor =
                    if (error != null) ErrorBorderColor else MaterialTheme.colorScheme.primary,
                unfocusedBorderColor =
                    if (error != null) ErrorBorderColor else MaterialTheme.colorScheme.outline,
                errorBorderColor = ErrorBorderColor,
                cursorColor = MaterialTheme.colorScheme.primary))

    Text(
        text = error ?: "3-30 characters. Letters, numbers, spaces, or underscores only",
        fontSize = 12.sp,
        color = if (error != null) ErrorBorderColor else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier =
            Modifier.padding(start = 16.dp, top = 4.dp)
                .testTag(EditGroupScreenTags.NAME_SUPPORTING_TEXT))
  }
}

@Composable
private fun CategoryDropdown(selectedCategory: EventType, onCategorySelected: (EventType) -> Unit) {
  var expanded by remember { mutableStateOf(false) }

  Column(modifier = Modifier.fillMaxWidth()) {
    Text(
        text = "Category",
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(bottom = 8.dp))

    Box(modifier = Modifier.fillMaxWidth()) {
      OutlinedTextField(
          value = selectedCategory.displayString(),
          onValueChange = {},
          modifier = Modifier.fillMaxWidth().testTag(EditGroupScreenTags.CATEGORY_DROPDOWN),
          readOnly = true,
          placeholder = { Text("Group Type", color = MaterialTheme.colorScheme.onSurfaceVariant) },
          trailingIcon = {
            IconButton(onClick = { expanded = !expanded }) {
              Icon(
                  imageVector = Icons.Default.ArrowDropDown,
                  contentDescription = "Dropdown",
                  tint = MaterialTheme.colorScheme.onSurface)
            }
          },
          colors =
              OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = MaterialTheme.colorScheme.primary,
                  unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                  cursorColor = MaterialTheme.colorScheme.primary,
                  disabledTextColor = MaterialTheme.colorScheme.onSurface))

      DropdownMenu(
          expanded = expanded,
          onDismissRequest = { expanded = false },
          modifier = Modifier.fillMaxWidth(0.9f).testTag(EditGroupScreenTags.CATEGORY_MENU)) {
            EventType.values().forEach { eventType ->
              DropdownMenuItem(
                  text = { Text(eventType.displayString()) },
                  onClick = {
                    onCategorySelected(eventType)
                    expanded = false
                  },
                  modifier =
                      Modifier.testTag(EditGroupScreenTags.CATEGORY_OPTION_PREFIX + eventType.name))
            }
          }
    }
  }
}

@Composable
private fun DescriptionInput(value: String, onValueChange: (String) -> Unit, error: String?) {
  Column(modifier = Modifier.fillMaxWidth()) {
    Text(
        text = "Description",
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(bottom = 8.dp))

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier =
            Modifier.fillMaxWidth()
                .height(120.dp)
                .testTag(EditGroupScreenTags.GROUP_DESCRIPTION_TEXT_FIELD),
        placeholder = {
          Text(
              "Tell us what your group is about",
              color = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        isError = error != null,
        maxLines = 4,
        colors =
            OutlinedTextFieldDefaults.colors(
                focusedBorderColor =
                    if (error != null) ErrorBorderColor else MaterialTheme.colorScheme.primary,
                unfocusedBorderColor =
                    if (error != null) ErrorBorderColor else MaterialTheme.colorScheme.outline,
                errorBorderColor = ErrorBorderColor,
                cursorColor = MaterialTheme.colorScheme.primary))

    Text(
        text = error ?: "0-300 characters. Letters, numbers, spaces, or underscores only",
        fontSize = 12.sp,
        color = if (error != null) ErrorBorderColor else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier =
            Modifier.padding(start = 16.dp, top = 4.dp)
                .testTag(EditGroupScreenTags.DESCRIPTION_SUPPORTING_TEXT))
  }
}

@Composable
private fun SaveButton(enabled: Boolean, onClick: () -> Unit) {
  Button(
      onClick = onClick,
      enabled = enabled,
      modifier = Modifier.fillMaxWidth().height(56.dp).testTag(EditGroupScreenTags.SAVE_BUTTON),
      colors =
          ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary,
              disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
              contentColor = MaterialTheme.colorScheme.onPrimary,
              disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant),
      shape = MaterialTheme.shapes.medium) {
        Text(text = "SAVE CHANGES", fontSize = 16.sp, fontWeight = FontWeight.Bold)
      }
}

@Preview(showBackground = true, name = "Edit Group Screen - Light Mode")
@Preview(
    showBackground = true,
    name = "Edit Group Screen - Dark Mode",
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EditGroupScreenPreview() {
  MaterialTheme {
    Column(
        modifier =
            Modifier.fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
          GroupPictureSection(onPictureEditClick = {})

          Spacer(modifier = Modifier.height(32.dp))

          GroupNameInput(value = "Basketball Team", onValueChange = {}, error = null)

          Spacer(modifier = Modifier.height(24.dp))

          CategoryDropdown(selectedCategory = EventType.SPORTS, onCategorySelected = {})

          Spacer(modifier = Modifier.height(24.dp))

          DescriptionInput(
              value = "Weekly basketball games every Saturday morning. All skill levels welcome!",
              onValueChange = {},
              error = null)

          Spacer(modifier = Modifier.height(32.dp))

          SaveButton(enabled = true, onClick = {})

          Spacer(modifier = Modifier.height(16.dp))
        }
  }
}
