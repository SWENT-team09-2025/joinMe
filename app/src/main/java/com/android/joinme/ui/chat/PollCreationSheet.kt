package com.android.joinme.ui.chat

// Implemented with help of Claude AI

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.android.joinme.R
import com.android.joinme.model.chat.Poll
import com.android.joinme.ui.theme.Dimens
import com.android.joinme.ui.theme.buttonColors
import com.android.joinme.ui.theme.customColors
import com.android.joinme.ui.theme.outlinedTextField

/** Test tags for Poll creation UI testing. */
object PollCreationTestTags {
  const val BOTTOM_SHEET = "pollCreationSheet"
  const val CLOSE_BUTTON = "pollCloseButton"
  const val QUESTION_FIELD = "pollQuestionField"
  const val OPTION_FIELD_PREFIX = "pollOptionField_"
  const val ADD_OPTION_BUTTON = "pollAddOptionButton"
  const val REMOVE_OPTION_PREFIX = "pollRemoveOption_"
  const val ANONYMOUS_SWITCH = "pollAnonymousSwitch"
  const val MULTIPLE_ANSWERS_SWITCH = "pollMultipleAnswersSwitch"
  const val CREATE_BUTTON = "pollCreateButton"
  const val REMAINING_OPTIONS_TEXT = "pollRemainingOptionsText"
  const val VALIDATION_ERROR = "pollValidationError"
  const val LOADING_INDICATOR = "pollCreationLoading"

  fun getOptionFieldTag(index: Int): String = "$OPTION_FIELD_PREFIX$index"

  fun getRemoveOptionTag(index: Int): String = "$REMOVE_OPTION_PREFIX$index"
}

/**
 * Bottom sheet for creating a new poll.
 *
 * Displays a form with:
 * - Question input field
 * - Option input fields with add/remove buttons
 * - Anonymous voting toggle
 * - Multiple answers toggle
 * - Create button
 *
 * @param viewModel The PollViewModel managing poll creation state
 * @param creatorName The name of the user creating the poll
 * @param onDismiss Callback when the sheet is dismissed
 * @param onPollCreated Callback when a poll is successfully created
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PollCreationSheet(
    viewModel: PollViewModel,
    creatorName: String,
    onDismiss: () -> Unit,
    onPollCreated: () -> Unit
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val creationState by viewModel.creationState.collectAsState()
  val scrollState = rememberScrollState()
  val context = LocalContext.current

  ModalBottomSheet(
      onDismissRequest = {
        viewModel.resetCreationState()
        onDismiss()
      },
      sheetState = sheetState,
      modifier = Modifier.testTag(PollCreationTestTags.BOTTOM_SHEET)) {
        Column(
            modifier =
                Modifier.fillMaxWidth()
                    .padding(horizontal = Dimens.Padding.large)
                    .padding(bottom = Dimens.Padding.large)
                    .verticalScroll(scrollState)) {
              // Header with close button
              PollCreationHeader(
                  onClose = {
                    viewModel.resetCreationState()
                    onDismiss()
                  })

              Spacer(modifier = Modifier.height(Dimens.Spacing.medium))

              // Question section
              PollQuestionSection(
                  question = creationState.question, onQuestionChange = viewModel::updateQuestion)

              Spacer(modifier = Modifier.height(Dimens.Spacing.large))

              // Options section
              PollOptionsSection(
                  options = creationState.options,
                  onOptionChange = viewModel::updateOption,
                  onAddOption = viewModel::addOption,
                  onRemoveOption = viewModel::removeOption,
                  canAddOption = creationState.canAddOption(),
                  canRemoveOption = creationState.canRemoveOption(),
                  remainingOptionsCount = creationState.getRemainingOptionsCount())

              Spacer(modifier = Modifier.height(Dimens.Spacing.large))

              // Settings section
              PollSettingsSection(
                  isAnonymous = creationState.isAnonymous,
                  allowMultipleAnswers = creationState.allowMultipleAnswers,
                  onAnonymousToggle = viewModel::toggleAnonymous,
                  onMultipleAnswersToggle = viewModel::toggleMultipleAnswers)

              // Validation error
              creationState.validationError?.let { error ->
                Spacer(modifier = Modifier.height(Dimens.Spacing.small))
                Text(
                    text = error.getMessage(context),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier =
                        Modifier.fillMaxWidth().testTag(PollCreationTestTags.VALIDATION_ERROR),
                    textAlign = TextAlign.Center)
              }

              Spacer(modifier = Modifier.height(Dimens.Spacing.large))

              // Create button
              Button(
                  onClick = {
                    viewModel.createPoll(
                        creatorName = creatorName,
                        onSuccess = {
                          onPollCreated()
                          onDismiss()
                        },
                        onError = { /* Error is shown in UI via state */})
                  },
                  enabled = creationState.isValid() && !creationState.isCreating,
                  modifier =
                      Modifier.fillMaxWidth()
                          .height(Dimens.Button.standardHeight)
                          .testTag(PollCreationTestTags.CREATE_BUTTON),
                  colors = MaterialTheme.customColors.buttonColors()) {
                    if (creationState.isCreating) {
                      CircularProgressIndicator(
                          modifier =
                              Modifier.size(Dimens.IconSize.medium)
                                  .testTag(PollCreationTestTags.LOADING_INDICATOR),
                          color = MaterialTheme.colorScheme.onPrimary,
                          strokeWidth = Dimens.BorderWidth.medium)
                    } else {
                      Text(
                          text = stringResource(R.string.poll_create),
                          style = MaterialTheme.typography.titleMedium)
                    }
                  }
            }
      }
}

/** Header section with title and close button. */
@Composable
private fun PollCreationHeader(onClose: () -> Unit) {
  Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.poll_create_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold)

        TextButton(
            onClick = onClose, modifier = Modifier.testTag(PollCreationTestTags.CLOSE_BUTTON)) {
              Text(text = stringResource(R.string.close), color = MaterialTheme.colorScheme.primary)
            }
      }
}

/** Question input section. */
@Composable
private fun PollQuestionSection(question: String, onQuestionChange: (String) -> Unit) {
  Column {
    Text(
        text = stringResource(R.string.poll_question_label),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant)

    Spacer(modifier = Modifier.height(Dimens.Spacing.small))

    OutlinedTextField(
        value = question,
        onValueChange = onQuestionChange,
        modifier = Modifier.fillMaxWidth().testTag(PollCreationTestTags.QUESTION_FIELD),
        placeholder = {
          Text(
              text = stringResource(R.string.poll_question_placeholder),
              color = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        colors = MaterialTheme.customColors.outlinedTextField(),
        maxLines = 3,
        supportingText = {
          Text(
              text = "${question.length}/${Poll.MAX_QUESTION_LENGTH}",
              style = MaterialTheme.typography.labelSmall,
              color =
                  if (question.length > Poll.MAX_QUESTION_LENGTH) MaterialTheme.colorScheme.error
                  else MaterialTheme.colorScheme.onSurfaceVariant)
        })
  }
}

/** Options input section with add/remove functionality. */
@Composable
private fun PollOptionsSection(
    options: List<String>,
    onOptionChange: (Int, String) -> Unit,
    onAddOption: () -> Unit,
    onRemoveOption: (Int) -> Unit,
    canAddOption: Boolean,
    canRemoveOption: Boolean,
    remainingOptionsCount: Int
) {
  Column {
    Text(
        text = stringResource(R.string.poll_options_label),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant)

    Spacer(modifier = Modifier.height(Dimens.Spacing.small))

    // Option fields
    options.forEachIndexed { index, option ->
      PollOptionField(
          index = index,
          text = option,
          onTextChange = { onOptionChange(index, it) },
          onRemove = { onRemoveOption(index) },
          canRemove = canRemoveOption)

      if (index < options.size - 1) {
        Spacer(modifier = Modifier.height(Dimens.Spacing.small))
      }
    }

    Spacer(modifier = Modifier.height(Dimens.Spacing.small))

    // Add option button and remaining count
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
          TextButton(
              onClick = onAddOption,
              enabled = canAddOption,
              modifier = Modifier.testTag(PollCreationTestTags.ADD_OPTION_BUTTON)) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.poll_add_option),
                    modifier = Modifier.size(Dimens.IconSize.small))
                Spacer(modifier = Modifier.width(Dimens.Spacing.extraSmall))
                Text(text = stringResource(R.string.poll_add_option))
              }

          Text(
              text = stringResource(R.string.poll_remaining_options, remainingOptionsCount),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.testTag(PollCreationTestTags.REMAINING_OPTIONS_TEXT))
        }
  }
}

/** Individual option input field with remove button. */
@Composable
private fun PollOptionField(
    index: Int,
    text: String,
    onTextChange: (String) -> Unit,
    onRemove: () -> Unit,
    canRemove: Boolean
) {
  Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
    OutlinedTextField(
        value = text,
        onValueChange = onTextChange,
        modifier = Modifier.weight(1f).testTag(PollCreationTestTags.getOptionFieldTag(index)),
        placeholder = {
          Text(
              text = stringResource(R.string.poll_option_placeholder, index + 1),
              color = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        colors = MaterialTheme.customColors.outlinedTextField(),
        singleLine = true)

    if (canRemove) {
      IconButton(
          onClick = onRemove,
          modifier = Modifier.testTag(PollCreationTestTags.getRemoveOptionTag(index))) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.poll_remove_option),
                tint = MaterialTheme.colorScheme.error)
          }
    }
  }
}

/** Settings section with toggles for anonymous voting and multiple answers. */
@Composable
private fun PollSettingsSection(
    isAnonymous: Boolean,
    allowMultipleAnswers: Boolean,
    onAnonymousToggle: () -> Unit,
    onMultipleAnswersToggle: () -> Unit
) {
  Column {
    // Anonymous voting toggle
    PollSettingRow(
        label = stringResource(R.string.poll_anonymous_voting),
        description = stringResource(R.string.poll_anonymous_voting_description),
        isChecked = isAnonymous,
        onToggle = onAnonymousToggle,
        testTag = PollCreationTestTags.ANONYMOUS_SWITCH)

    Spacer(modifier = Modifier.height(Dimens.Spacing.medium))

    // Multiple answers toggle
    PollSettingRow(
        label = stringResource(R.string.poll_multiple_answers),
        description = stringResource(R.string.poll_multiple_answers_description),
        isChecked = allowMultipleAnswers,
        onToggle = onMultipleAnswersToggle,
        testTag = PollCreationTestTags.MULTIPLE_ANSWERS_SWITCH)
  }
}

/** Setting row with label, description, and toggle switch. */
@Composable
private fun PollSettingRow(
    label: String,
    description: String,
    isChecked: Boolean,
    onToggle: () -> Unit,
    testTag: String
) {
  Surface(
      modifier = Modifier.fillMaxWidth(),
      color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
      shape = MaterialTheme.shapes.medium) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Dimens.Padding.medium),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium)
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
              }

              Switch(
                  checked = isChecked,
                  onCheckedChange = { onToggle() },
                  modifier = Modifier.testTag(testTag),
                  colors =
                      SwitchDefaults.colors(
                          checkedTrackColor = MaterialTheme.colorScheme.primary,
                          checkedThumbColor = MaterialTheme.colorScheme.onPrimary))
            }
      }
}
