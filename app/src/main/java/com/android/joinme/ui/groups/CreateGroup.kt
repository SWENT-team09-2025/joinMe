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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.joinme.R
import com.android.joinme.model.event.EventType
import com.android.joinme.ui.navigation.NavigationTestTags
import com.android.joinme.ui.theme.*

object CreateGroupTestTags {
  const val SCREEN = "create_group_screen"
  const val TITLE = "create_group_title"
  const val GROUP_PICTURE = "group_picture_section"
  const val EDIT_PHOTO_BUTTON = "edit_photo_button"
  const val NAME_INPUT = "name_input"
  const val NAME_SUPPORTING_TEXT = "name_supporting_text"
  const val CATEGORY_DROPDOWN = "category_dropdown"
  const val CATEGORY_MENU = "category_menu"
  const val CATEGORY_OPTION_PREFIX = "category_option_"
  const val DESCRIPTION_INPUT = "description_input"
  const val DESCRIPTION_SUPPORTING_TEXT = "description_supporting_text"
  const val SAVE_BUTTON = "save_button"
}

/**
 * Create Group Screen
 *
 * Allows users to create a new group by filling in:
 * - Group name (required, 3-30 characters)
 * - Category (Social/Activity/Sports)
 * - Description (optional, max 300 characters)
 * - Group photo (not yet implemented)
 *
 * @param viewModel The ViewModel managing the create group state
 * @param onNavigateBack Callback when user navigates back
 * @param onGroupCreated Callback with groupId when group is successfully created
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupScreen(
    viewModel: CreateGroupViewModel = viewModel(),
    onNavigateBack: () -> Unit = {},
    onGroupCreated: (String) -> Unit = {}
) {
  val uiState by viewModel.uiState.collectAsState()
  val context = LocalContext.current

  // Handle success navigation
  LaunchedEffect(uiState.createdGroupId) {
    uiState.createdGroupId?.let { groupId ->
      onGroupCreated(groupId)
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
      modifier = Modifier.fillMaxSize().testTag(CreateGroupTestTags.SCREEN),
      topBar = {
        Column {
          CenterAlignedTopAppBar(
              modifier = Modifier.testTag(NavigationTestTags.TOP_BAR_TITLE),
              title = {
                Text(
                    text = "Create Group",
                    modifier = Modifier.testTag(CreateGroupTestTags.TITLE),
                    style = MaterialTheme.typography.titleLarge)
              },
              navigationIcon = {
                IconButton(
                    onClick = { onNavigateBack() },
                    modifier = Modifier.testTag(NavigationTestTags.GO_BACK_BUTTON)) {
                      Icon(
                          imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                          contentDescription = "Back")
                    }
              },
              colors =
                  TopAppBarDefaults.topAppBarColors(
                      containerColor = MaterialTheme.colorScheme.surface))
          HorizontalDivider(color = DividerColor, thickness = 1.dp)
        }
      }) { paddingValues ->
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .padding(paddingValues)
                    .background(ScreenBackgroundColor)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
              // Group Picture Section
              GroupPictureSection(
                  onPictureEditClick = {
                    Toast.makeText(context, "Not yet implemented", Toast.LENGTH_SHORT).show()
                  })

              Spacer(modifier = Modifier.height(32.dp))

              // Group Name Input
              GroupNameInput(
                  value = uiState.name,
                  onValueChange = { viewModel.setName(it) },
                  error = uiState.nameError)

              Spacer(modifier = Modifier.height(24.dp))

              // Category Dropdown
              CategoryDropdown(
                  selectedCategory = uiState.category,
                  onCategorySelected = { viewModel.setCategory(it) })

              Spacer(modifier = Modifier.height(24.dp))

              // Description Input
              DescriptionInput(
                  value = uiState.description,
                  onValueChange = { viewModel.setDescription(it) },
                  error = uiState.descriptionError)

              Spacer(modifier = Modifier.weight(1f))
              Spacer(modifier = Modifier.height(32.dp))

              // Save Button
              SaveButton(
                  enabled = uiState.isValid && !uiState.isLoading,
                  onClick = { viewModel.createGroup() })

              Spacer(modifier = Modifier.height(16.dp))
            }
      }
}

@Composable
private fun GroupPictureSection(onPictureEditClick: () -> Unit = {}) {
  Box(
      modifier =
          Modifier.fillMaxWidth()
              .padding(vertical = 24.dp)
              .testTag(CreateGroupTestTags.GROUP_PICTURE),
      contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.size(140.dp), contentAlignment = Alignment.Center) {
          // Blurred group picture placeholder
          Image(
              painter = painterResource(id = R.drawable.group_default_picture),
              contentDescription = "Group picture placeholder",
              modifier =
                  Modifier.matchParentSize()
                      .blur(5.dp) // blur the image
                      .clip(CircleShape),
              // contentScale = ContentScale.Crop
          )

          // Edit button overlay
          Button(
              onClick = { onPictureEditClick() },
              colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
              shape = CircleShape,
              modifier = Modifier.size(120.dp).testTag(CreateGroupTestTags.EDIT_PHOTO_BUTTON)) {
                Box(
                    modifier = Modifier.size(50.dp).clip(CircleShape).background(Color.Transparent),
                    contentAlignment = Alignment.Center) {
                      Icon(
                          imageVector = Icons.Outlined.Edit,
                          contentDescription = "Edit Photo",
                          tint = ButtonSaveColor,
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
        modifier = Modifier.fillMaxWidth().testTag(CreateGroupTestTags.NAME_INPUT),
        placeholder = { Text("Group name", color = PlaceholderTextColor) },
        isError = error != null,
        singleLine = true,
        colors =
            OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (error != null) ErrorBorderColor else FocusedBorderColor,
                unfocusedBorderColor = if (error != null) ErrorBorderColor else BorderColor,
                errorBorderColor = ErrorBorderColor,
                cursorColor = FocusedBorderColor))

    Text(
        text = error ?: "3-30 characters. Letters, numbers, spaces, or underscores only",
        fontSize = 12.sp,
        color = if (error != null) ErrorBorderColor else DescriptionTextColor,
        modifier =
            Modifier.padding(start = 16.dp, top = 4.dp)
                .testTag(CreateGroupTestTags.NAME_SUPPORTING_TEXT))
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
          value =
              when (selectedCategory) {
                EventType.SOCIAL -> "Social"
                EventType.ACTIVITY -> "Activity"
                EventType.SPORTS -> "Sports"
              },
          onValueChange = {},
          modifier = Modifier.fillMaxWidth().testTag(CreateGroupTestTags.CATEGORY_DROPDOWN),
          readOnly = true,
          placeholder = { Text("Group Type", color = PlaceholderTextColor) },
          trailingIcon = {
            IconButton(onClick = { expanded = !expanded }) {
              Icon(
                  imageVector = Icons.Default.ArrowDropDown,
                  contentDescription = "Dropdown",
                  tint = IconColor)
            }
          },
          colors =
              OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = FocusedBorderColor,
                  unfocusedBorderColor = BorderColor,
                  cursorColor = FocusedBorderColor,
                  disabledTextColor = Color.Black))

      DropdownMenu(
          expanded = expanded,
          onDismissRequest = { expanded = false },
          modifier = Modifier.fillMaxWidth(0.9f).testTag(CreateGroupTestTags.CATEGORY_MENU)) {
            DropdownMenuItem(
                text = { Text("Social") },
                onClick = {
                  onCategorySelected(EventType.SOCIAL)
                  expanded = false
                },
                modifier = Modifier.testTag(CreateGroupTestTags.CATEGORY_OPTION_PREFIX + "SOCIAL"))
            DropdownMenuItem(
                text = { Text("Activity") },
                onClick = {
                  onCategorySelected(EventType.ACTIVITY)
                  expanded = false
                },
                modifier =
                    Modifier.testTag(CreateGroupTestTags.CATEGORY_OPTION_PREFIX + "ACTIVITY"))
            DropdownMenuItem(
                text = { Text("Sports") },
                onClick = {
                  onCategorySelected(EventType.SPORTS)
                  expanded = false
                },
                modifier = Modifier.testTag(CreateGroupTestTags.CATEGORY_OPTION_PREFIX + "SPORTS"))
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
            Modifier.fillMaxWidth().height(120.dp).testTag(CreateGroupTestTags.DESCRIPTION_INPUT),
        placeholder = { Text("Tell us what your group is about", color = PlaceholderTextColor) },
        isError = error != null,
        maxLines = 4,
        colors =
            OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (error != null) ErrorBorderColor else FocusedBorderColor,
                unfocusedBorderColor = if (error != null) ErrorBorderColor else BorderColor,
                errorBorderColor = ErrorBorderColor,
                cursorColor = FocusedBorderColor))

    Text(
        text = error ?: "0-300 characters. Letters, numbers, spaces, or underscores only",
        fontSize = 12.sp,
        color = if (error != null) ErrorBorderColor else DescriptionTextColor,
        modifier =
            Modifier.padding(start = 16.dp, top = 4.dp)
                .testTag(CreateGroupTestTags.DESCRIPTION_SUPPORTING_TEXT))
  }
}

@Composable
private fun SaveButton(enabled: Boolean, onClick: () -> Unit) {
  Button(
      onClick = onClick,
      enabled = enabled,
      modifier = Modifier.fillMaxWidth().height(56.dp).testTag(CreateGroupTestTags.SAVE_BUTTON),
      colors =
          ButtonDefaults.buttonColors(
              containerColor = DarkButtonColor,
              disabledContainerColor = DisabledButtonColor,
              contentColor = ButtonSaveColor,
              disabledContentColor = DisabledTextColor),
      shape = MaterialTheme.shapes.medium) {
        Text(text = "SAVE", fontSize = 16.sp, fontWeight = FontWeight.Bold)
      }
}
