package com.android.joinme.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import com.android.joinme.model.event.getColor
import com.android.joinme.model.event.getOnContainerColor
import com.android.joinme.model.groups.Group
import com.android.joinme.ui.theme.Dimens

private const val DESCRIPTION_ALPHA = 0.8f
private const val MEMBERS_ALPHA = 0.7f

/**
 * A reusable card component for displaying group information.
 *
 * Displays group details including name, description (if present), and member count. The card is
 * styled with a color that corresponds to the group's category (Sports, Social, Activity).
 *
 * The card is clickable and includes proper accessibility support for screen readers.
 *
 * @param modifier Modifier to be applied to the card. Use this to add custom padding, sizing, or
 *   other modifications from the caller.
 * @param group The group to display, containing name, description, category, and member
 *   information.
 * @param onClick Callback invoked when the card is clicked by the user.
 * @param testTag Test tag identifier for UI testing purposes.
 */
@Composable
fun GroupCard(
    modifier: Modifier = Modifier,
    group: Group,
    onClick: () -> Unit,
    testTag: String = ""
) {
  val groupColor = group.category.getColor()
  val groupOnColor = group.category.getOnContainerColor()

  Card(
      modifier =
          modifier
              .fillMaxWidth()
              .heightIn(min = Dimens.Profile.bioMinHeight)
              .clickable { onClick() }
              .testTag(testTag),
      shape = RoundedCornerShape(Dimens.CornerRadius.large),
      colors = CardDefaults.cardColors(containerColor = groupColor, contentColor = groupOnColor),
      elevation = CardDefaults.cardElevation(defaultElevation = Dimens.Elevation.small)) {
        Row(modifier = Modifier.fillMaxWidth().padding(Dimens.Spacing.itemSpacing)) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
                text = group.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(Dimens.Spacing.extraSmall))
            if (group.description.isNotBlank()) {
              Text(
                  text = group.description,
                  style = MaterialTheme.typography.bodySmall,
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis,
                  color = groupOnColor.copy(alpha = DESCRIPTION_ALPHA))
            }
            Spacer(Modifier.height(Dimens.Spacing.small))
            Text(
                text = "members: ${group.memberIds.size}",
                style = MaterialTheme.typography.labelSmall,
                color = groupOnColor.copy(alpha = MEMBERS_ALPHA))
          }
        }
      }
}
