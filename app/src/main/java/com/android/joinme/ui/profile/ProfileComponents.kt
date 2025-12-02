package com.android.joinme.ui.profile

// AI-assisted implementation — reviewed and adapted for project standards.

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.android.joinme.R
import com.android.joinme.model.profile.Profile
import com.android.joinme.ui.theme.Dimens

/**
 * Displays a profile header with statistics and profile photo.
 *
 * Shows three statistics (events joined, followers, following) on the left and a circular profile
 * photo on the right. This component is designed to be reusable across different profile screens.
 *
 * @param profile The profile data to display
 * @param statsRowTestTag Test tag for the entire row container
 * @param eventsJoinedTestTag Test tag for the events joined statistic
 * @param followersTestTag Test tag for the followers statistic
 * @param followingTestTag Test tag for the following statistic
 * @param profilePhotoTestTag Test tag for the profile photo
 */
@Composable
fun ProfileHeader(
    profile: Profile,
    statsRowTestTag: String = "",
    eventsJoinedTestTag: String = "",
    followersTestTag: String = "",
    followingTestTag: String = "",
    profilePhotoTestTag: String = ""
) {
  Row(
      modifier =
          Modifier.fillMaxWidth()
              .padding(vertical = Dimens.Spacing.medium)
              .testTag(statsRowTestTag),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically) {
        Row(
            modifier = Modifier.weight(1f, fill = false),
            horizontalArrangement = Arrangement.SpaceEvenly) {
              StatItem(
                  value = profile.eventsJoinedCount.toString(),
                  label = stringResource(R.string.events_joined),
                  testTag = eventsJoinedTestTag)
              StatItem(
                  value = formatCount(profile.followersCount),
                  label = stringResource(R.string.followers),
                  testTag = followersTestTag)
              StatItem(
                  value = profile.followingCount.toString(),
                  label = stringResource(R.string.following),
                  testTag = followingTestTag)
            }
        Spacer(modifier = Modifier.width(Dimens.Spacing.medium))
        ProfilePhotoImage(
            photoUrl = profile.photoUrl,
            contentDescription = stringResource(R.string.profile_photo),
            size = Dimens.PublicProfile.photoSize,
            modifier = Modifier.testTag(profilePhotoTestTag))
      }
}

/**
 * Displays a single statistic item with a value and label.
 *
 * The value is displayed in large bold text, with the label shown below in smaller, lighter text.
 * Commonly used for displaying metrics like follower counts, event counts, etc.
 *
 * @param value The numeric or formatted value to display (e.g., "42", "1.2k")
 * @param label The descriptive label for the statistic (e.g., "Followers")
 * @param testTag Optional test tag for UI testing
 */
@Composable
fun StatItem(value: String, label: String, testTag: String = "") {
  Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.testTag(testTag)) {
    Text(
        text = value,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground)
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
  }
}

/**
 * Displays the event streaks section with a fire emoji and streak count.
 *
 * Currently displays a hardcoded "0 days" streak. This is intended to be replaced with actual
 * streak tracking in the future.
 *
 * @param testTag Optional test tag for UI testing
 */
@Composable
fun EventStreaksSection(testTag: String = "") {
  Column(modifier = Modifier.fillMaxWidth().testTag(testTag)) {
    Text(
        text = stringResource(R.string.event_streaks),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
    Spacer(modifier = Modifier.height(Dimens.Spacing.extraSmall))
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(text = "🔥", style = MaterialTheme.typography.titleMedium)
      Spacer(modifier = Modifier.width(Dimens.Spacing.small))
      Text(text = stringResource(R.string.zero_days), style = MaterialTheme.typography.bodyMedium)
    }
  }
}

/**
 * Formats large numbers into abbreviated form for display.
 *
 * Converts numbers to more readable formats:
 * - Numbers >= 1 billion: "X.Xb" (e.g., 1,500,000,000 -> "1.5b")
 * - Numbers >= 1 million: "X.Xm" (e.g., 2,800,000 -> "2.8m")
 * - Numbers >= 1 thousand: "X.Xk" (e.g., 1,200 -> "1.2k")
 * - Numbers < 1 thousand: raw number (e.g., 42 -> "42")
 *
 * @param count The count to format
 * @return Formatted string with appropriate suffix (k, m, b) or raw number
 */
fun formatCount(count: Int): String {
  return when {
    count >= 1_000_000_000 -> "%.1fb".format(count / 1_000_000_000.0)
    count >= 1_000_000 -> "%.1fm".format(count / 1_000_000.0)
    count >= 1_000 -> "%.1fk".format(count / 1_000.0)
    else -> count.toString()
  }
}
