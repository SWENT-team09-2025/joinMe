package com.android.joinme.ui.profile
/* This file was implemented with the help of Claude AI */
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.joinme.R
import com.android.joinme.model.event.Event
import com.android.joinme.model.groups.Group
import com.android.joinme.model.profile.Profile
import com.android.joinme.ui.components.EventCard
import com.android.joinme.ui.components.GroupCard
import com.android.joinme.ui.theme.Dimens
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

/**
 * Contains test tags for UI elements in the PublicProfileScreen.
 *
 * These tags are used in instrumentation tests to identify and interact with specific UI
 * components.
 */
object PublicProfileScreenTestTags {
  const val SCREEN = "publicProfileScreen"
  const val TOP_BAR = "publicProfileTopBar"
  const val BACK_BUTTON = "publicProfileBackButton"
  const val PROFILE_PHOTO = "publicProfilePhoto"
  const val STATS_ROW = "publicProfileStatsRow"
  const val EVENTS_JOINED_STAT = "publicProfileEventsJoinedStat"
  const val FOLLOWERS_STAT = "publicProfileFollowersStat"
  const val FOLLOWING_STAT = "publicProfileFollowingStat"
  const val USERNAME = "publicProfileUsername"
  const val BIO = "publicProfileBio"
  const val INTERESTS_SECTION = "publicProfileInterestsSection"
  const val EVENT_STREAKS_SECTION = "publicProfileEventStreaksSection"
  const val FOLLOW_BUTTON = "publicProfileFollowButton"
  const val MESSAGE_BUTTON = "publicProfileMessageButton"
  const val COMMON_EVENTS_TITLE = "publicProfileCommonEventsTitle"
  const val COMMON_EVENTS_LIST = "publicProfileCommonEventsList"
  const val COMMON_GROUPS_TITLE = "publicProfileCommonGroupsTitle"
  const val COMMON_GROUPS_LIST = "publicProfileCommonGroupsList"
  const val LOADING_INDICATOR = "publicProfileLoadingIndicator"
  const val ERROR_MESSAGE = "publicProfileErrorMessage"
  const val EMPTY_EVENTS_MESSAGE = "publicProfileEmptyEventsMessage"
  const val EMPTY_GROUPS_MESSAGE = "publicProfileEmptyGroupsMessage"

  fun eventCardTag(eventId: String) = "publicProfileEventCard:$eventId"

  fun groupCardTag(groupId: String) = "publicProfileGroupCard:$groupId"
}

/**
 * Displays another user's public profile with shared events and groups.
 *
 * This screen shows:
 * - User's profile information (photo, username, bio, interests)
 * - Stats (events joined, followers, following)
 * - Event streak (currently set to 0)
 * - Common events shared with the current user
 * - Common groups shared with the current user
 * - Follow and Message buttons (UI only, not yet functional)
 *
 * @param userId The unique identifier of the user whose profile to display
 * @param viewModel The ViewModel managing the profile state and business logic
 * @param onBackClick Callback invoked when the user taps the back button
 * @param onEventClick Callback invoked when the user taps on an event card
 * @param onGroupClick Callback invoked when the user taps on a group card
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicProfileScreen(
    userId: String,
    viewModel: PublicProfileViewModel = viewModel(),
    onBackClick: () -> Unit = {},
    onEventClick: (Event) -> Unit = {},
    onGroupClick: (Group) -> Unit = {}
) {

  val profile by viewModel.profile.collectAsState()
  val commonEvents by viewModel.commonEvents.collectAsState()
  val commonGroups by viewModel.commonGroups.collectAsState()
  val isLoading by viewModel.isLoading.collectAsState()
  val error by viewModel.error.collectAsState()

  // Get current user ID
  val currentUserId = Firebase.auth.currentUser?.uid

  // Load profile data when screen is first displayed
  LaunchedEffect(userId) { viewModel.loadPublicProfile(userId, currentUserId) }

  Scaffold(
      modifier = Modifier.fillMaxSize().testTag(PublicProfileScreenTestTags.SCREEN),
      topBar = {
        Column {
          CenterAlignedTopAppBar(
              modifier = Modifier.testTag(PublicProfileScreenTestTags.TOP_BAR),
              title = {
                Text(
                    text = profile?.username ?: "",
                    modifier = Modifier.testTag(PublicProfileScreenTestTags.USERNAME),
                    style = MaterialTheme.typography.titleLarge)
              },
              navigationIcon = {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.testTag(PublicProfileScreenTestTags.BACK_BUTTON)) {
                      Icon(
                          imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                          contentDescription = stringResource(R.string.back))
                    }
              },
              colors =
                  TopAppBarDefaults.centerAlignedTopAppBarColors(
                      containerColor = MaterialTheme.colorScheme.surface))
          HorizontalDivider(
              color = MaterialTheme.colorScheme.primary, thickness = Dimens.BorderWidth.thin)
        }
      }) { paddingValues ->
        Box(
            modifier =
                Modifier.fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)) {
              when {
                isLoading -> {
                  // Loading state
                  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        modifier =
                            Modifier.size(Dimens.LoadingIndicator.large)
                                .testTag(PublicProfileScreenTestTags.LOADING_INDICATOR))
                  }
                }
                error != null -> {
                  // Error state
                  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = error ?: stringResource(R.string.unknown_error),
                        modifier =
                            Modifier.padding(Dimens.Padding.large)
                                .testTag(PublicProfileScreenTestTags.ERROR_MESSAGE),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center)
                  }
                }
                profile != null -> {
                  // Content state
                  ProfileContent(
                      profile = profile!!,
                      commonEvents = commonEvents,
                      commonGroups = commonGroups,
                      onEventClick = onEventClick,
                      onGroupClick = onGroupClick)
                }
              }
            }
      }
}

@Composable
private fun ProfileContent(
    profile: Profile,
    commonEvents: List<Event>,
    commonGroups: List<Group>,
    onEventClick: (Event) -> Unit,
    onGroupClick: (Group) -> Unit
) {
  val context = LocalContext.current
  Column(
      modifier =
          Modifier.fillMaxSize()
              .verticalScroll(rememberScrollState())
              .padding(Dimens.Padding.medium),
      verticalArrangement = Arrangement.spacedBy(Dimens.Spacing.medium)) {
        // Row 1: Stats (Events Joined, Followers, Following) and Profile Photo
        Row(
            modifier =
                Modifier.fillMaxWidth()
                    .padding(vertical = Dimens.Spacing.medium)
                    .testTag(PublicProfileScreenTestTags.STATS_ROW),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
              Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatItem(
                    value = profile.eventsJoinedCount.toString(),
                    label = stringResource(R.string.events_joined),
                    testTag = PublicProfileScreenTestTags.EVENTS_JOINED_STAT)
                StatItem(
                    value = formatCount(profile.followersCount),
                    label = stringResource(R.string.followers),
                    testTag = PublicProfileScreenTestTags.FOLLOWERS_STAT)
                StatItem(
                    value = profile.followingCount.toString(),
                    label = stringResource(R.string.following),
                    testTag = PublicProfileScreenTestTags.FOLLOWING_STAT)
              }
              ProfilePhotoImage(
                  photoUrl = profile.photoUrl,
                  contentDescription = stringResource(R.string.profile_photo),
                  size = Dimens.PublicProfile.photoSize,
                  modifier = Modifier.testTag(PublicProfileScreenTestTags.PROFILE_PHOTO))
            }

        // Row 2: Bio and Follow Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing.medium),
            verticalAlignment = Alignment.Top) {
              // Bio on the left
              Column(modifier = Modifier.weight(1f).testTag(PublicProfileScreenTestTags.BIO)) {
                Text(
                    text = stringResource(R.string.bio),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
                Spacer(modifier = Modifier.height(Dimens.Spacing.extraSmall))
                Text(
                    text =
                        if (profile.bio?.isNotBlank() == true) profile.bio
                        else stringResource(R.string.no_bio_available),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3)
              }
              // Follow Button on the right
              Button(
                  onClick = { /* TODO: Implement follow functionality */
                    Toast.makeText(context, "Follow functionality coming soon!", Toast.LENGTH_SHORT)
                        .show()
                  },
                  modifier =
                      Modifier.height(Dimens.Button.minHeight)
                          .testTag(PublicProfileScreenTestTags.FOLLOW_BUTTON)
                          .width(Dimens.PublicProfile.buttonWidth),
                  colors =
                      ButtonDefaults.buttonColors(
                          containerColor = MaterialTheme.colorScheme.primary),
                  shape = RoundedCornerShape(Dimens.CornerRadius.circle)) {
                    Text(
                        stringResource(R.string.follow),
                        color = MaterialTheme.colorScheme.onPrimary)
                  }
            }

        // Row 3: Interests and Message Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing.medium),
            verticalAlignment = Alignment.Top) {
              // Interests on the left
              Column(
                  modifier =
                      Modifier.weight(1f).testTag(PublicProfileScreenTestTags.INTERESTS_SECTION)) {
                    Text(
                        text = stringResource(R.string.interests),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
                    Spacer(modifier = Modifier.height(Dimens.Spacing.extraSmall))
                    Text(
                        text =
                            if (profile.interests.isNotEmpty()) profile.interests.joinToString(", ")
                            else stringResource(R.string.no_interests_available),
                        style = MaterialTheme.typography.bodyMedium)
                  }
              // Message Button on the right
              Button(
                  onClick = { /* TODO: Implement message functionality */
                    Toast.makeText(
                            context, "Message functionality coming soon!", Toast.LENGTH_SHORT)
                        .show()
                  },
                  modifier =
                      Modifier.height(Dimens.Button.minHeight)
                          .width(Dimens.PublicProfile.buttonWidth)
                          .testTag(PublicProfileScreenTestTags.MESSAGE_BUTTON),
                  colors =
                      ButtonDefaults.buttonColors(
                          containerColor = MaterialTheme.colorScheme.secondary),
                  shape = RoundedCornerShape(Dimens.CornerRadius.circle)) {
                    Text(
                        stringResource(R.string.message),
                        color = MaterialTheme.colorScheme.onSecondary)
                  }
            }

        // Event Streaks (set to 0 for now)
        Column(
            modifier =
                Modifier.fillMaxWidth()
                    .testTag(PublicProfileScreenTestTags.EVENT_STREAKS_SECTION)) {
              Text(
                  text = stringResource(R.string.event_streaks),
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
              Spacer(modifier = Modifier.height(Dimens.Spacing.extraSmall))
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "🔥", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.width(Dimens.Spacing.small))
                Text(
                    text = stringResource(R.string.zero_days),
                    style = MaterialTheme.typography.bodyMedium)
              }
            }

        // Common Events Section
        Text(
            text = stringResource(R.string.common_events),
            modifier =
                Modifier.fillMaxWidth()
                    .padding(top = Dimens.Spacing.medium)
                    .testTag(PublicProfileScreenTestTags.COMMON_EVENTS_TITLE),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold)

        if (commonEvents.isEmpty()) {
          Card(
              modifier =
                  Modifier.fillMaxWidth()
                      .padding(vertical = Dimens.Spacing.small)
                      .testTag(PublicProfileScreenTestTags.EMPTY_EVENTS_MESSAGE),
              colors =
                  CardDefaults.cardColors(
                      containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Text(
                    text = stringResource(R.string.no_common_events),
                    modifier = Modifier.fillMaxWidth().padding(Dimens.Padding.medium),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
        } else {
          LazyColumn(
              modifier =
                  Modifier.fillMaxWidth()
                      .weight(1f)
                      .border(
                          width = Dimens.BorderWidth.thin,
                          color = MaterialTheme.colorScheme.primary,
                          shape = RoundedCornerShape(Dimens.CornerRadius.medium))
                      .padding(Dimens.Padding.small)
                      .testTag(PublicProfileScreenTestTags.COMMON_EVENTS_LIST),
              verticalArrangement = Arrangement.spacedBy(Dimens.Spacing.small)) {
                items(commonEvents) { event ->
                  EventCard(
                      event = event,
                      onClick = { onEventClick(event) },
                      testTag = PublicProfileScreenTestTags.eventCardTag(event.eventId))
                }
              }
        }

        // Common Groups Section
        Text(
            text = stringResource(R.string.common_groups),
            modifier =
                Modifier.fillMaxWidth()
                    .padding(top = Dimens.Spacing.medium)
                    .testTag(PublicProfileScreenTestTags.COMMON_GROUPS_TITLE),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold)

        if (commonGroups.isEmpty()) {
          Card(
              modifier =
                  Modifier.fillMaxWidth()
                      .padding(vertical = Dimens.Spacing.small)
                      .testTag(PublicProfileScreenTestTags.EMPTY_GROUPS_MESSAGE),
              colors =
                  CardDefaults.cardColors(
                      containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Text(
                    text = stringResource(R.string.no_common_groups),
                    modifier = Modifier.fillMaxWidth().padding(Dimens.Padding.medium),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
        } else {
          LazyColumn(
              modifier =
                  Modifier.fillMaxWidth()
                      .weight(1f)
                      .border(
                          width = Dimens.BorderWidth.thin,
                          color = MaterialTheme.colorScheme.primary,
                          shape = RoundedCornerShape(Dimens.CornerRadius.medium))
                      .padding(Dimens.Padding.small)
                      .testTag(PublicProfileScreenTestTags.COMMON_GROUPS_LIST),
              verticalArrangement = Arrangement.spacedBy(Dimens.Spacing.small)) {
                items(commonGroups) { group ->
                  GroupCard(
                      group = group,
                      onClick = { onGroupClick(group) },
                      testTag = PublicProfileScreenTestTags.groupCardTag(group.id))
                }
              }
        }
      }
}

@Composable
private fun StatItem(value: String, label: String, testTag: String = "") {
  Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.testTag(testTag)) {
    Text(
        text = value,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground)
    Text(
        text = label,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
  }
}

/**
 * Formats large numbers into abbreviated form (e.g., 28800000 -> "28.8m")
 *
 * @param count The count to format
 * @return Formatted string with appropriate suffix (k, m, b)
 */
private fun formatCount(count: Int): String {
  return when {
    count >= 1_000_000_000 -> "%.1fb".format(count / 1_000_000_000.0)
    count >= 1_000_000 -> "%.1fm".format(count / 1_000_000.0)
    count >= 1_000 -> "%.1fk".format(count / 1_000.0)
    else -> count.toString()
  }
}
