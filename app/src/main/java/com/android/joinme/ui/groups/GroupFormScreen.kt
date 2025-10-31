package com.android.joinme.ui.groups

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.joinme.R
import com.android.joinme.model.event.EventType
import com.android.joinme.model.event.displayString
import com.android.joinme.ui.theme.ErrorBorderColor

/** Data class representing the test tags for group form fields. */
data class GroupFormTestTags(
    val screen: String,
    val title: String,
    val groupPicture: String,
    val editPhotoButton: String,
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
    onPictureEditClick: () -> Unit = {}
) {
  Scaffold(
      modifier = Modifier.fillMaxSize().testTag(testTags.screen),
      topBar = {
        Column {
          CenterAlignedTopAppBar(
              title = {
                Text(text = title, modifier = Modifier.testTag(testTags.title), style = MaterialTheme.typography.titleLarge)
              },
              navigationIcon = {
                IconButton(onClick = onGoBack) {
                  Icon(imageVector = Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                }
              },
              colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface))
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
        }
      }) { paddingValues ->
        when {
          formState.isLoading -> {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
              CircularProgressIndicator(modifier = Modifier.testTag(testTags.loadingIndicator))
            }
          }
          formState.errorMsg != null -> {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
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
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                  GroupPictureSection(
                      onPictureEditClick = onPictureEditClick,
                      testTags = testTags)

                  Spacer(modifier = Modifier.height(32.dp))

                  GroupNameInput(
                      value = formState.name,
                      onValueChange = onNameChange,
                      error = formState.nameError,
                      testTags = testTags)

                  Spacer(modifier = Modifier.height(24.dp))

                  CategoryDropdown(
                      selectedCategory = formState.category,
                      onCategorySelected = onCategoryChange,
                      testTags = testTags)

                  Spacer(modifier = Modifier.height(24.dp))

                  DescriptionInput(
                      value = formState.description,
                      onValueChange = onDescriptionChange,
                      error = formState.descriptionError,
                      testTags = testTags)

                  Spacer(modifier = Modifier.weight(1f))
                  Spacer(modifier = Modifier.height(32.dp))

                  SaveButton(
                      enabled = formState.isValid && !formState.isLoading,
                      onClick = onSave,
                      text = saveButtonText,
                      testTags = testTags)
                  Spacer(modifier = Modifier.height(16.dp))
                }
          }
        }
      }
}

@Composable
private fun GroupPictureSection(
    onPictureEditClick: () -> Unit,
    testTags: GroupFormTestTags
) {
  Box(
      modifier =
          Modifier.fillMaxWidth()
              .padding(vertical = 24.dp)
              .testTag(testTags.groupPicture),
      contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.size(140.dp), contentAlignment = Alignment.Center) {
          Image(
              painter = painterResource(id = R.drawable.group_default_picture),
              contentDescription = "Group picture placeholder",
              modifier = Modifier.matchParentSize().blur(5.dp).clip(CircleShape))

          Button(
              onClick = onPictureEditClick,
              colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
              shape = CircleShape,
              modifier = Modifier.size(120.dp).testTag(testTags.editPhotoButton)) {
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
private fun GroupNameInput(
    value: String,
    onValueChange: (String) -> Unit,
    error: String?,
    testTags: GroupFormTestTags
) {
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
        modifier = Modifier.fillMaxWidth().testTag(testTags.groupNameTextField),
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
        modifier = Modifier.padding(start = 16.dp, top = 4.dp).testTag(testTags.nameSupportingText))
  }
}

@Composable
private fun CategoryDropdown(
    selectedCategory: EventType,
    onCategorySelected: (EventType) -> Unit,
    testTags: GroupFormTestTags
) {
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
          modifier = Modifier.fillMaxWidth().testTag(testTags.categoryDropdown),
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
          modifier = Modifier.fillMaxWidth(0.9f).testTag(testTags.categoryMenu)) {
            EventType.values().forEach { eventType ->
              DropdownMenuItem(
                  text = { Text(eventType.displayString()) },
                  onClick = {
                    onCategorySelected(eventType)
                    expanded = false
                  },
                  modifier = Modifier.testTag(testTags.categoryOptionPrefix + eventType.name))
            }
          }
    }
  }
}

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
                .testTag(testTags.groupDescriptionTextField),
        placeholder = {
          Text("Tell us what your group is about", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            Modifier.padding(start = 16.dp, top = 4.dp).testTag(testTags.descriptionSupportingText))
  }
}

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
      modifier = Modifier.fillMaxWidth().height(56.dp).testTag(testTags.saveButton),
      colors =
          ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary,
              disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
              contentColor = MaterialTheme.colorScheme.onPrimary,
              disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant),
      shape = MaterialTheme.shapes.medium) {
        Text(text = text.uppercase(), fontSize = 16.sp, fontWeight = FontWeight.Bold)
      }
}
