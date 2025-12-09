package com.android.joinme.ui.chat

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.android.joinme.R
import com.android.joinme.model.chat.Poll
import com.android.joinme.model.profile.Profile
import com.android.joinme.ui.profile.ProfilePhotoImage
import com.android.joinme.ui.theme.Dimens
import com.android.joinme.ui.theme.customColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Test tags for Poll display UI testing. */
object PollDisplayTestTags {
  const val POLL_CARD_PREFIX = "pollCard_"
  const val POLL_QUESTION_PREFIX = "pollQuestion_"
  const val POLL_OPTION_PREFIX = "pollOption_"
  const val POLL_VOTE_COUNT_PREFIX = "pollVoteCount_"
  const val POLL_TOTAL_VOTES_PREFIX = "pollTotalVotes_"
  const val POLL_MENU_BUTTON_PREFIX = "pollMenuButton_"
  const val POLL_CLOSED_BADGE_PREFIX = "pollClosedBadge_"
  const val POLL_ANONYMOUS_BADGE_PREFIX = "pollAnonymousBadge_"
  const val POLL_VOTERS_DIALOG = "pollVotersDialog"

  fun getPollCardTag(pollId: String): String = "$POLL_CARD_PREFIX$pollId"

  fun getPollQuestionTag(pollId: String): String = "$POLL_QUESTION_PREFIX$pollId"

  fun getPollOptionTag(pollId: String, optionId: Int): String =
      "$POLL_OPTION_PREFIX${pollId}_$optionId"

  fun getPollVoteCountTag(pollId: String): String = "$POLL_VOTE_COUNT_PREFIX$pollId"

  fun getPollTotalVotesTag(pollId: String): String = "$POLL_TOTAL_VOTES_PREFIX$pollId"

  fun getPollMenuButtonTag(pollId: String): String = "$POLL_MENU_BUTTON_PREFIX$pollId"

  fun getPollClosedBadgeTag(pollId: String): String = "$POLL_CLOSED_BADGE_PREFIX$pollId"

  fun getPollAnonymousBadgeTag(pollId: String): String = "$POLL_ANONYMOUS_BADGE_PREFIX$pollId"
}

/**
 * Displays a poll card in the chat.
 *
 * Shows the poll question, options with vote counts and progress bars, and controls for voting and
 * poll management.
 *
 * @param poll The poll to display
 * @param currentUserId The ID of the current user
 * @param voterProfiles Map of user IDs to their profiles (for showing who voted)
 * @param onVote Callback when user votes for an option
 * @param onClosePoll Callback when poll creator closes the poll
 * @param onReopenPoll Callback when poll creator reopens the poll
 * @param onDeletePoll Callback when poll creator deletes the poll
 */
@Composable
fun PollCard(
    poll: Poll,
    currentUserId: String,
    voterProfiles: Map<String, Profile> = emptyMap(),
    onVote: (optionId: Int) -> Unit,
    onClosePoll: () -> Unit,
    onReopenPoll: () -> Unit,
    onDeletePoll: () -> Unit
) {
  val isCreator = poll.creatorId == currentUserId
  var showMenu by remember { mutableStateOf(false) }
  var showVotersDialog by remember { mutableStateOf<Int?>(null) }
  var showDeleteConfirmation by remember { mutableStateOf(false) }

  Card(
      modifier =
          Modifier.fillMaxWidth()
              .padding(vertical = Dimens.Padding.small)
              .testTag(PollDisplayTestTags.getPollCardTag(poll.id)),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
      shape = RoundedCornerShape(Dimens.CornerRadius.large)) {
        Column(modifier = Modifier.fillMaxWidth().padding(Dimens.Padding.medium)) {
          // Header with poll icon, creator info, and menu
          PollHeader(
              poll = poll,
              isCreator = isCreator,
              showMenu = showMenu,
              onMenuToggle = { showMenu = !showMenu },
              onClosePoll = {
                onClosePoll()
                showMenu = false
              },
              onReopenPoll = {
                onReopenPoll()
                showMenu = false
              },
              onDeletePoll = {
                showDeleteConfirmation = true
                showMenu = false
              })

          Spacer(modifier = Modifier.height(Dimens.Spacing.small))

          // Poll badges (anonymous, closed)
          PollBadges(poll = poll)

          // Question
          Text(
              text = poll.question,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              modifier =
                  Modifier.fillMaxWidth().testTag(PollDisplayTestTags.getPollQuestionTag(poll.id)))

          Spacer(modifier = Modifier.height(Dimens.Spacing.medium))

          // Options
          poll.options.forEach { option ->
            PollOptionItem(
                poll = poll,
                option = option,
                currentUserId = currentUserId,
                voterProfiles = voterProfiles,
                onVote = { onVote(option.id) },
                onShowVoters = {
                  if (!poll.isAnonymous) {
                    showVotersDialog = option.id
                  }
                })
            Spacer(modifier = Modifier.height(Dimens.Spacing.small))
          }

          // Total votes
          Text(
              text = stringResource(R.string.poll_total_votes, poll.getUniqueVoterCount()),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.testTag(PollDisplayTestTags.getPollTotalVotesTag(poll.id)))

          // Created timestamp
          Text(
              text = formatPollTimestamp(poll.createdAt),
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }

  // Voters dialog
  showVotersDialog?.let { optionId ->
    VotersDialog(
        poll = poll,
        optionId = optionId,
        voterProfiles = voterProfiles,
        onDismiss = { showVotersDialog = null })
  }

  // Delete confirmation dialog
  if (showDeleteConfirmation) {
    DeletePollDialog(
        onConfirm = {
          onDeletePoll()
          showDeleteConfirmation = false
        },
        onDismiss = { showDeleteConfirmation = false })
  }
}

/** Poll header with icon, creator info, and menu button. */
@Composable
private fun PollHeader(
    poll: Poll,
    isCreator: Boolean,
    showMenu: Boolean,
    onMenuToggle: () -> Unit,
    onClosePoll: () -> Unit,
    onReopenPoll: () -> Unit,
    onDeletePoll: () -> Unit
) {
  Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
              imageVector = Icons.Default.HowToVote,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(Dimens.IconSize.medium))
          Spacer(modifier = Modifier.width(Dimens.Spacing.small))
          Column {
            Text(
                text = stringResource(R.string.poll),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary)
            Text(
                text = stringResource(R.string.poll_created_by, poll.creatorName),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }

        // Menu button (only for creator)
        if (isCreator) {
          Box {
            IconButton(
                onClick = onMenuToggle,
                modifier = Modifier.testTag(PollDisplayTestTags.getPollMenuButtonTag(poll.id))) {
                  Icon(
                      imageVector = Icons.Default.MoreVert,
                      contentDescription = stringResource(R.string.poll_menu),
                      tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }

            DropdownMenu(expanded = showMenu, onDismissRequest = { onMenuToggle() }) {
              if (poll.isClosed) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.poll_reopen)) },
                    onClick = onReopenPoll,
                    leadingIcon = {
                      Icon(imageVector = Icons.Default.LockOpen, contentDescription = null)
                    })
              } else {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.poll_close)) },
                    onClick = onClosePoll,
                    leadingIcon = {
                      Icon(imageVector = Icons.Default.Lock, contentDescription = null)
                    })
              }
              DropdownMenuItem(
                  text = {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                  },
                  onClick = onDeletePoll,
                  leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error)
                  })
            }
          }
        }
      }
}

/** Poll badges showing status (closed, anonymous). */
@Composable
private fun PollBadges(poll: Poll) {
  Row(horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing.small)) {
    if (poll.isClosed) {
      PollBadge(
          icon = Icons.Default.Lock,
          text = stringResource(R.string.poll_closed),
          color = MaterialTheme.colorScheme.error,
          testTag = PollDisplayTestTags.getPollClosedBadgeTag(poll.id))
    }

    if (poll.isAnonymous) {
      PollBadge(
          icon = Icons.Default.VisibilityOff,
          text = stringResource(R.string.poll_anonymous),
          color = MaterialTheme.colorScheme.tertiary,
          testTag = PollDisplayTestTags.getPollAnonymousBadgeTag(poll.id))
    }

    if (poll.allowMultipleAnswers) {
      PollBadge(
          icon = Icons.Default.Check,
          text = stringResource(R.string.poll_multiple_selection),
          color = MaterialTheme.colorScheme.secondary,
          testTag = "")
    }
  }

  if (poll.isClosed || poll.isAnonymous || poll.allowMultipleAnswers) {
    Spacer(modifier = Modifier.height(Dimens.Spacing.small))
  }
}

/** Individual badge showing a poll property. */
@Composable
private fun PollBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    color: androidx.compose.ui.graphics.Color,
    testTag: String
) {
  Surface(
      color = color.copy(alpha = 0.1f),
      shape = RoundedCornerShape(Dimens.CornerRadius.small),
      modifier = if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier) {
        Row(
            modifier =
                Modifier.padding(
                    horizontal = Dimens.Padding.small, vertical = Dimens.Padding.extraSmall),
            verticalAlignment = Alignment.CenterVertically) {
              Icon(
                  imageVector = icon,
                  contentDescription = null,
                  tint = color,
                  modifier = Modifier.size(Dimens.IconSize.small))
              Spacer(modifier = Modifier.width(Dimens.Spacing.extraSmall))
              Text(text = text, style = MaterialTheme.typography.labelSmall, color = color)
            }
      }
}

/** Individual poll option with progress bar and vote count. */
@Composable
private fun PollOptionItem(
    poll: Poll,
    option: com.android.joinme.model.chat.PollOption,
    currentUserId: String,
    voterProfiles: Map<String, Profile>,
    onVote: () -> Unit,
    onShowVoters: () -> Unit
) {
  val isSelected = poll.hasUserVotedForOption(currentUserId, option.id)
  val votePercentage = poll.getVotePercentage(option.id)
  val voteCount = poll.getVoteCount(option.id)
  val canVote = !poll.isClosed

  Surface(
      modifier =
          Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(Dimens.CornerRadius.medium))
              .clickable(enabled = canVote) { onVote() }
              .testTag(PollDisplayTestTags.getPollOptionTag(poll.id, option.id)),
      color =
          if (isSelected) MaterialTheme.colorScheme.primaryContainer
          else MaterialTheme.colorScheme.surface,
      shape = RoundedCornerShape(Dimens.CornerRadius.medium)) {
        Box(modifier = Modifier.fillMaxWidth()) {
          // Progress bar background
          LinearProgressIndicator(
              progress = { votePercentage / 100f },
              modifier =
                  Modifier.fillMaxWidth().height(Dimens.Button.minHeight).animateContentSize(),
              color =
                  if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                  else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
              trackColor = androidx.compose.ui.graphics.Color.Transparent,
          )

          // Content overlay
          Row(
              modifier =
                  Modifier.fillMaxWidth()
                      .height(Dimens.Button.minHeight)
                      .padding(horizontal = Dimens.Padding.medium),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically) {
                // Option text with checkmark if selected
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)) {
                      if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = stringResource(R.string.poll_selected),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(Dimens.IconSize.small))
                        Spacer(modifier = Modifier.width(Dimens.Spacing.small))
                      }

                      Text(
                          text = option.text,
                          style = MaterialTheme.typography.bodyMedium,
                          fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                          maxLines = 2,
                          overflow = TextOverflow.Ellipsis)
                    }

                // Vote count and percentage
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing.small)) {
                      // Show voters preview (if not anonymous)
                      if (!poll.isAnonymous && voteCount > 0) {
                        VotersPreview(
                            voterIds = poll.getVotersForOption(option.id),
                            voterProfiles = voterProfiles,
                            onClick = onShowVoters)
                      }

                      Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${votePercentage.toInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color =
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface)
                        Text(
                            text = stringResource(R.string.poll_votes, voteCount),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                      }
                    }
              }
        }
      }
}

/** Preview of voters with overlapping avatars. */
@Composable
private fun VotersPreview(
    voterIds: List<String>,
    voterProfiles: Map<String, Profile>,
    onClick: () -> Unit
) {
  val maxDisplay = 3
  val displayedVoters = voterIds.take(maxDisplay)
  val remainingCount = voterIds.size - maxDisplay

  Row(
      modifier = Modifier.clickable(onClick = onClick),
      horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
        displayedVoters.forEach { voterId ->
          val profile = voterProfiles[voterId]
          ProfilePhotoImage(
              photoUrl = profile?.photoUrl,
              contentDescription = profile?.username ?: stringResource(R.string.unknown_user),
              size = Dimens.IconSize.medium,
              shape = CircleShape,
              showLoadingIndicator = false)
        }

        if (remainingCount > 0) {
          Surface(
              modifier = Modifier.size(Dimens.IconSize.medium),
              shape = CircleShape,
              color = MaterialTheme.colorScheme.surfaceVariant) {
                Box(contentAlignment = Alignment.Center) {
                  Text(
                      text = "+$remainingCount",
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.Bold)
                }
              }
        }
      }
}

// dp extension for negative spacing
private val Int.dp: androidx.compose.ui.unit.Dp
  get() = androidx.compose.ui.unit.Dp(this.toFloat())

/** Dialog showing all voters for an option. */
@Composable
private fun VotersDialog(
    poll: Poll,
    optionId: Int,
    voterProfiles: Map<String, Profile>,
    onDismiss: () -> Unit
) {
  val option = poll.options.find { it.id == optionId } ?: return
  val voters = option.voterIds

  AlertDialog(
      onDismissRequest = onDismiss,
      modifier = Modifier.testTag(PollDisplayTestTags.POLL_VOTERS_DIALOG),
      title = {
        Text(
            text = stringResource(R.string.poll_voters_for, option.text),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis)
      },
      text = {
        Column {
          if (voters.isEmpty()) {
            Text(
                text = stringResource(R.string.poll_no_votes_yet),
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
          } else {
            voters.forEach { voterId ->
              val profile = voterProfiles[voterId]
              Row(
                  modifier = Modifier.fillMaxWidth().padding(vertical = Dimens.Padding.extraSmall),
                  verticalAlignment = Alignment.CenterVertically) {
                    ProfilePhotoImage(
                        photoUrl = profile?.photoUrl,
                        contentDescription =
                            profile?.username ?: stringResource(R.string.unknown_user),
                        size = Dimens.Profile.photoSmall,
                        shape = CircleShape,
                        showLoadingIndicator = false)
                    Spacer(modifier = Modifier.width(Dimens.Spacing.small))
                    Text(
                        text = profile?.username ?: stringResource(R.string.unknown_user),
                        style = MaterialTheme.typography.bodyMedium)
                  }
            }
          }
        }
      },
      confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } })
}

/** Confirmation dialog for deleting a poll. */
@Composable
private fun DeletePollDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
  AlertDialog(
      onDismissRequest = onDismiss,
      title = { Text(stringResource(R.string.poll_delete_title)) },
      text = { Text(stringResource(R.string.poll_delete_confirmation)) },
      confirmButton = {
        Button(
            onClick = onConfirm,
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.customColors.deleteButton)) {
              Text(stringResource(R.string.delete))
            }
      },
      dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } })
}

/** Formats a poll timestamp into a readable string. */
private fun formatPollTimestamp(timestamp: Long): String {
  val dateFormat = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
  return dateFormat.format(Date(timestamp))
}
