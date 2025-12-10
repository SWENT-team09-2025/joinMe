// Implemented with help of Claude AI

package com.android.joinme.ui.groups.leaderboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.android.joinme.R
import com.android.joinme.ui.theme.Dimens

/** Test tags for the StreakInfoDialog */
object StreakInfoDialogTestTags {
    const val DIALOG = "streak_info_dialog"
    const val CLOSE_BUTTON = "streak_info_close_button"
    const val TITLE_HOW = "streak_info_title_how"
    const val TITLE_WHAT = "streak_info_title_what"
    const val TITLE_WHY = "streak_info_title_why"
}

/**
 * Dialog explaining the leaderboard and streak rules.
 *
 * @param onDismiss Callback when the dialog should be dismissed.
 */
@Composable
fun StreakInfoDialog(
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(StreakInfoDialogTestTags.DIALOG),
            shape = RoundedCornerShape(Dimens.CornerRadius.extraLarge),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = Dimens.Elevation.large
        ) {
            Column(
                modifier = Modifier
                    .padding(Dimens.Padding.large)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header with title and close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = stringResource(R.string.leaderboard_rules_title_how),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .weight(1f)
                            .testTag(StreakInfoDialogTestTags.TITLE_HOW)
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(24.dp)
                            .testTag(StreakInfoDialogTestTags.CLOSE_BUTTON)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.close),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.leaderboard_rules_content_how),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Dimens.Padding.small)
                )

                Spacer(modifier = Modifier.height(Dimens.Spacing.large))

                // What is a streak section
                Text(
                    text = stringResource(R.string.leaderboard_rules_title_what),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag(StreakInfoDialogTestTags.TITLE_WHAT)
                )

                Text(
                    text = stringResource(R.string.leaderboard_rules_content_what),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Dimens.Padding.small)
                )

                Spacer(modifier = Modifier.height(Dimens.Spacing.large))

                // Why it matters section
                Text(
                    text = stringResource(R.string.leaderboard_rules_title_why),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag(StreakInfoDialogTestTags.TITLE_WHY)
                )

                Text(
                    text = stringResource(R.string.leaderboard_rules_content_why),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Dimens.Padding.small)
                )
            }
        }
    }
}