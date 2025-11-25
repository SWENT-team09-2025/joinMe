package com.android.joinme.ui.profile
/* This file was implemented with the help of Claude AI */
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.joinme.model.event.Event
import com.android.joinme.model.event.EventType
import com.android.joinme.model.event.EventVisibility
import com.android.joinme.model.groups.Group
import com.android.joinme.model.map.Location
import com.android.joinme.model.profile.Profile
import com.android.joinme.ui.components.EventCard
import com.android.joinme.ui.components.GroupCard
import com.android.joinme.ui.theme.Dimens
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import java.util.Date

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
                          contentDescription = "Back")
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
                        text = error ?: "Unknown error",
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
                    label = "Events Joined",
                    testTag = PublicProfileScreenTestTags.EVENTS_JOINED_STAT)
                StatItem(
                    value = formatCount(profile.followersCount),
                    label = "Followers",
                    testTag = PublicProfileScreenTestTags.FOLLOWERS_STAT)
                StatItem(
                    value = profile.followingCount.toString(),
                    label = "Following",
                    testTag = PublicProfileScreenTestTags.FOLLOWING_STAT)
              }
              ProfilePhotoImage(
                  photoUrl = profile.photoUrl,
                  contentDescription = "Profile photo",
                  size = 100.dp,
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
                    text = "Bio",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
                Spacer(modifier = Modifier.height(Dimens.Spacing.extraSmall))
                Text(
                    text =
                        if (profile.bio?.isNotBlank() == true) profile.bio else "No bio available",
                    style = MaterialTheme.typography.bodyMedium)
              }
              // Follow Button on the right
              Button(
                  onClick = { /* TODO: Implement follow functionality */},
                  modifier =
                      Modifier.height(Dimens.Button.minHeight)
                          .testTag(PublicProfileScreenTestTags.FOLLOW_BUTTON)
                          .width(120.dp),
                  colors =
                      ButtonDefaults.buttonColors(
                          containerColor = MaterialTheme.colorScheme.primary),
                  shape = RoundedCornerShape(Dimens.CornerRadius.circle)) {
                    Text("Follow", color = MaterialTheme.colorScheme.onPrimary)
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
                        text = "Interests",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
                    Spacer(modifier = Modifier.height(Dimens.Spacing.extraSmall))
                    Text(
                        text =
                            if (profile.interests.isNotEmpty()) profile.interests.joinToString(", ")
                            else "No interests available",
                        style = MaterialTheme.typography.bodyMedium)
                  }
              // Message Button on the right
              Button(
                  onClick = { /* TODO: Implement message functionality */},
                  modifier =
                      Modifier.height(Dimens.Button.minHeight)
                          .width(120.dp)
                          .testTag(PublicProfileScreenTestTags.MESSAGE_BUTTON),
                  colors =
                      ButtonDefaults.buttonColors(
                          containerColor = MaterialTheme.colorScheme.secondary),
                  shape = RoundedCornerShape(Dimens.CornerRadius.circle)) {
                    Text("Message", color = MaterialTheme.colorScheme.onSecondary)
                  }
            }

        // Event Streaks (set to 0 for now)
        Column(
            modifier =
                Modifier.fillMaxWidth()
                    .testTag(PublicProfileScreenTestTags.EVENT_STREAKS_SECTION)) {
              Text(
                  text = "Event Streaks",
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
              Spacer(modifier = Modifier.height(Dimens.Spacing.extraSmall))
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "🔥", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.width(Dimens.Spacing.small))
                Text(text = "0 days", style = MaterialTheme.typography.bodyMedium)
              }
            }

        // Common Events Section
        Text(
            text = "Common events",
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
                    text = "No common events",
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
            text = "Common groups",
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
                    text = "No common groups",
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

// ========================================
// Preview Functions
// ========================================

/** Creates sample profile data for preview purposes. */
private fun createSampleProfile(): Profile {
  return Profile(
      uid = "user123",
      photoUrl = null,
      username = "Mathieu_pfr",
      email = "mathieu@example.com",
      dateOfBirth = "15/03/1995",
      country = "Switzerland",
      interests = listOf("Musculation", "Ginko"),
      bio = "EPFL student in IC section🎓",
      createdAt = Timestamp.now(),
      updatedAt = Timestamp.now(),
      fcmToken = null,
      eventsJoinedCount = 6956,
      followersCount = 28_800_000,
      followingCount = 218)
}

/** Creates sample event data for preview purposes. */
private fun createSampleEvents(): List<Event> {
  val now = Date()
  return listOf(
      Event(
          eventId = "event1",
          type = EventType.SPORTS,
          title = "BasketBall",
          description = "Friendly basketball game at Unil sports center",
          location = Location(latitude = 46.5197, longitude = 6.6323, name = "Unil sports"),
          date = Timestamp(now),
          duration = 120,
          participants = listOf("user123", "currentUser"),
          maxParticipants = 10,
          visibility = EventVisibility.PUBLIC,
          ownerId = "user123",
          partOfASerie = false,
          groupId = null),
      Event(
          eventId = "event2",
          type = EventType.ACTIVITY,
          title = "Study Session",
          description = "Group study for algorithms exam",
          location = Location(latitude = 46.5191, longitude = 6.5668, name = "BC Building"),
          date = Timestamp(Date(now.time + 86400000)), // +1 day
          duration = 180,
          participants = listOf("user123", "currentUser", "user456"),
          maxParticipants = 6,
          visibility = EventVisibility.PUBLIC,
          ownerId = "currentUser",
          partOfASerie = false,
          groupId = null))
}

/** Creates sample group data for preview purposes. */
private fun createSampleGroups(): List<Group> {
  return listOf(
      Group(
          id = "group1",
          name = "Running club",
          category = EventType.SPORTS,
          description = "We just like running between men.",
          ownerId = "user123",
          memberIds = listOf("user123", "currentUser", "user456", "user789"),
          eventIds = listOf("event1", "event2"),
          serieIds = emptyList(),
          photoUrl = null),
      Group(
          id = "group2",
          name = "EPFL Students",
          category = EventType.ACTIVITY,
          description = "Group for EPFL IC students to organize study sessions and hangouts",
          ownerId = "currentUser",
          memberIds = listOf("user123", "currentUser", "user999"),
          eventIds = emptyList(),
          serieIds = emptyList(),
          photoUrl = null))
}

/** Preview helper that shows the full screen with TopAppBar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PublicProfileScreenPreviewWrapper(
    profile: Profile,
    commonEvents: List<Event>,
    commonGroups: List<Group>
) {
  Scaffold(
      modifier = Modifier.fillMaxSize(),
      topBar = {
        Column {
          CenterAlignedTopAppBar(
              title = {
                Text(text = profile.username, style = MaterialTheme.typography.titleLarge)
              },
              navigationIcon = {
                IconButton(onClick = {}) {
                  Icon(
                      imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                      contentDescription = "Back")
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
              ProfileContent(
                  profile = profile,
                  commonEvents = commonEvents,
                  commonGroups = commonGroups,
                  onEventClick = {},
                  onGroupClick = {})
            }
      }
}

/** Preview of the PublicProfileScreen with sample data showing a complete profile. */
@Preview(showBackground = true)
@Composable
fun PublicProfileScreenPreview() {
  MaterialTheme {
    PublicProfileScreenPreviewWrapper(
        profile = createSampleProfile(),
        commonEvents = createSampleEvents(),
        commonGroups = createSampleGroups())
  }
}

/** Preview of the PublicProfileScreen with no common events or groups. */
@Preview(showBackground = true)
@Composable
fun PublicProfileScreenEmptyPreview() {
  MaterialTheme {
    PublicProfileScreenPreviewWrapper(
        profile = createSampleProfile(), commonEvents = emptyList(), commonGroups = emptyList())
  }
}

/** Preview of the PublicProfileScreen with minimal profile information. */
@Preview(showBackground = true)
@Composable
fun PublicProfileScreenMinimalPreview() {
  MaterialTheme {
    PublicProfileScreenPreviewWrapper(
        profile =
            Profile(
                uid = "user456",
                username = "john_doe",
                email = "john@example.com",
                interests = emptyList(),
                bio = null,
                eventsJoinedCount = 42,
                followersCount = 1_250,
                followingCount = 89),
        commonEvents = createSampleEvents().take(1),
        commonGroups = createSampleGroups().take(1))
  }
}
