package com.android.joinme.ui.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.android.joinme.R
import com.android.joinme.ui.theme.Dimens
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Shared constants for chat screens. */
object ChatConstants {
  const val SENDER_NAME_ALPHA = 0.8f
  const val TIMESTAMP_ALPHA = 0.7f
  const val MESSAGE_INPUT_MAX_LINES = 4
  const val TIMESTAMP_FORMAT = "HH:mm"
}

/**
 * Formats a Unix timestamp into a readable time string.
 *
 * @param timestamp Unix timestamp in milliseconds
 * @return Formatted time string (e.g., "14:32")
 */
fun formatTimestamp(timestamp: Long): String {
  val dateFormat = SimpleDateFormat(ChatConstants.TIMESTAMP_FORMAT, Locale.getDefault())
  return dateFormat.format(Date(timestamp))
}

/**
 * Individual attachment option with icon and label.
 *
 * Used in both ChatScreen and ChatScreenWithPolls attachment menus.
 *
 * @param icon The icon to display
 * @param label The text label below the icon
 * @param onClick Callback when the option is clicked
 * @param modifier Modifier for the component
 */
@Composable
internal fun AttachmentOption(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
  Column(
      modifier = modifier.clickable(onClick = onClick).padding(Dimens.Padding.medium),
      horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(Dimens.IconSize.large))

        Spacer(modifier = Modifier.height(Dimens.Spacing.small))

        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface)
      }
}

/**
 * Dialog for choosing photo source (Gallery or Camera).
 *
 * Used in both ChatScreen and ChatScreenWithPolls.
 *
 * @param onDismiss Callback when dialog is dismissed
 * @param onGalleryClick Callback when gallery option is selected
 * @param onCameraClick Callback when camera option is selected
 */
@Composable
internal fun PhotoSourceDialog(
    onDismiss: () -> Unit,
    onGalleryClick: () -> Unit,
    onCameraClick: () -> Unit
) {
  AlertDialog(
      onDismissRequest = onDismiss,
      modifier = Modifier.testTag(ChatScreenTestTags.PHOTO_SOURCE_DIALOG),
      title = { Text(text = stringResource(R.string.choose_photo_source)) },
      text = {
        Column(modifier = Modifier.fillMaxWidth()) {
          // Gallery option
          TextButton(
              onClick = onGalleryClick,
              modifier = Modifier.fillMaxWidth().testTag(ChatScreenTestTags.PHOTO_SOURCE_GALLERY)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing.small)) {
                      Icon(
                          imageVector = Icons.Default.Image,
                          contentDescription = stringResource(R.string.gallery))
                      Text(text = stringResource(R.string.gallery))
                    }
              }

          // Camera option
          TextButton(
              onClick = onCameraClick,
              modifier = Modifier.fillMaxWidth().testTag(ChatScreenTestTags.PHOTO_SOURCE_CAMERA)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing.small)) {
                      Icon(
                          imageVector = Icons.Default.CameraAlt,
                          contentDescription = stringResource(R.string.camera))
                      Text(text = stringResource(R.string.camera))
                    }
              }
        }
      },
      confirmButton = {},
      dismissButton = {
        TextButton(onClick = onDismiss) { Text(text = stringResource(R.string.cancel)) }
      })
}
