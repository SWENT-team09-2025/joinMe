package com.android.joinme.ui.groups.leaderboard

// Implemented with the help of AI tools, adapted and refined to follow project conventions.

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.joinme.R
import com.android.joinme.ui.profile.ProfilePhotoImage
import com.android.joinme.ui.theme.Dimens
import com.android.joinme.ui.theme.customColors

/** Test tags for UI testing */
object LeaderboardTestTags {
  const val SCREEN = "leaderboard_screen"
  const val TOP_BAR = "leaderboard_top_bar"
  const val BACK_BUTTON = "leaderboard_back_button"
  const val INFO_BUTTON = "leaderboard_info_button"
  const val TAB_ROW = "leaderboard_tab_row"
  const val TAB_CURRENT = "leaderboard_tab_current"
  const val TAB_ALL_TIME = "leaderboard_tab_all_time"
  const val LIST = "leaderboard_list"
  const val ITEM_PREFIX = "leaderboard_item_"
  const val LOADING = "leaderboard_loading"
  const val EMPTY_STATE = "leaderboard_empty_state"
}

/**
 * Main leaderboard screen composable.
 *
 * @param groupId The ID of the group to display the leaderboard for.
 * @param viewModel The ViewModel providing leaderboard data.
 * @param onNavigateBack Callback to navigate back to the previous screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupLeaderboardScreen(
    groupId: String,
    viewModel: GroupLeaderboardViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
  val uiState by viewModel.uiState.collectAsState()
  var selectedTabIndex by remember { mutableIntStateOf(0) }
  var showInfoDialog by remember { mutableStateOf(false) }
  val snackbarHostState = remember { SnackbarHostState() }

  // Load leaderboard data when the screen is first displayed
  LaunchedEffect(groupId) { viewModel.loadLeaderboard(groupId) }

  // Show error message in snackbar
  LaunchedEffect(uiState.errorMsg) {
    uiState.errorMsg?.let { error ->
      snackbarHostState.showSnackbar(error)
      viewModel.clearErrorMsg()
    }
  }

  Scaffold(
      modifier = Modifier.fillMaxSize().testTag(LeaderboardTestTags.SCREEN),
      topBar = {
        LeaderboardTopBar(onNavigateBack = onNavigateBack, onInfoClick = { showInfoDialog = true })
      },
      snackbarHost = { SnackbarHost(snackbarHostState) },
      containerColor = MaterialTheme.colorScheme.background) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
          LeaderboardTabs(
              selectedTabIndex = selectedTabIndex, onTabSelected = { selectedTabIndex = it })

          when {
            uiState.isLoading -> {
              LoadingState()
            }
            else -> {
              val entries =
                  if (selectedTabIndex == 0) {
                    uiState.currentLeaderboard
                  } else {
                    uiState.allTimeLeaderboard
                  }

              if (entries.isEmpty()) {
                EmptyState()
              } else {
                LeaderboardList(entries = entries)
              }
            }
          }
        }
      }

  // Show streak info dialog
  if (showInfoDialog) {
    StreakInfoDialog(onDismiss = { showInfoDialog = false })
  }
}

/** Top app bar for the leaderboard screen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LeaderboardTopBar(onNavigateBack: () -> Unit, onInfoClick: () -> Unit) {
  TopAppBar(
      modifier = Modifier.testTag(LeaderboardTestTags.TOP_BAR),
      title = {
        Text(
            text = stringResource(R.string.leaderboard_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center)
      },
      navigationIcon = {
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier.testTag(LeaderboardTestTags.BACK_BUTTON)) {
              Icon(
                  imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                  contentDescription = stringResource(R.string.back))
            }
      },
      actions = {
        IconButton(
            onClick = onInfoClick, modifier = Modifier.testTag(LeaderboardTestTags.INFO_BUTTON)) {
              Icon(
                  imageVector = Icons.Outlined.Info,
                  contentDescription = stringResource(R.string.leaderboard_info_button_description))
            }
      },
      colors =
          TopAppBarDefaults.topAppBarColors(
              containerColor = MaterialTheme.colorScheme.background,
              titleContentColor = MaterialTheme.colorScheme.onBackground))
}

/** Tab row for switching between Current and All Time leaderboards. */
@Composable
private fun LeaderboardTabs(selectedTabIndex: Int, onTabSelected: (Int) -> Unit) {
  val tabs =
      listOf(
          stringResource(R.string.leaderboard_tab_current),
          stringResource(R.string.leaderboard_tab_all_time))

  TabRow(
      selectedTabIndex = selectedTabIndex,
      modifier =
          Modifier.fillMaxWidth()
              .padding(horizontal = Dimens.Padding.medium)
              .testTag(LeaderboardTestTags.TAB_ROW),
      containerColor = Color.Transparent,
      contentColor = MaterialTheme.colorScheme.primary,
      indicator = { tabPositions ->
        TabRowDefaults.SecondaryIndicator(
            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
            color = MaterialTheme.colorScheme.primary)
      },
      divider = {}) {
        tabs.forEachIndexed { index, title ->
          val isSelected = selectedTabIndex == index
          Tab(
              selected = isSelected,
              onClick = { onTabSelected(index) },
              modifier =
                  Modifier.testTag(
                          if (index == 0) LeaderboardTestTags.TAB_CURRENT
                          else LeaderboardTestTags.TAB_ALL_TIME)
                      .clip(RoundedCornerShape(Dimens.CornerRadius.pill))
                      .then(
                          if (isSelected) {
                            Modifier.background(
                                MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(Dimens.CornerRadius.pill))
                          } else {
                            Modifier
                          }),
              text = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color =
                        if (isSelected) {
                          MaterialTheme.colorScheme.onPrimary
                        } else {
                          MaterialTheme.colorScheme.onSurfaceVariant
                        })
              })
        }
      }
}

/** Loading state indicator. */
@Composable
private fun LoadingState() {
  Box(
      modifier = Modifier.fillMaxSize().testTag(LeaderboardTestTags.LOADING),
      contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            modifier = Modifier.size(Dimens.LoadingIndicator.medium),
            color = MaterialTheme.colorScheme.primary)
      }
}

/** Empty state when no leaderboard entries exist. */
@Composable
private fun EmptyState() {
  Box(
      modifier = Modifier.fillMaxSize().testTag(LeaderboardTestTags.EMPTY_STATE),
      contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(R.string.leaderboard_empty_message),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(Dimens.Padding.large))
      }
}

/** Lazy column displaying leaderboard entries. */
@Composable
private fun LeaderboardList(entries: List<LeaderboardEntry>) {
  LazyColumn(
      modifier = Modifier.fillMaxSize().testTag(LeaderboardTestTags.LIST),
      contentPadding =
          PaddingValues(horizontal = Dimens.Padding.medium, vertical = Dimens.Padding.medium),
      verticalArrangement = Arrangement.spacedBy(Dimens.Spacing.itemSpacing)) {
        items(items = entries, key = { it.userId }) { entry -> LeaderboardItem(entry = entry) }
      }
}

/** Individual leaderboard item card. */
@Composable
private fun LeaderboardItem(entry: LeaderboardEntry) {
  val isTopThree = entry.rank <= 3

  Card(
      modifier =
          Modifier.fillMaxWidth().testTag("${LeaderboardTestTags.ITEM_PREFIX}${entry.userId}"),
      shape = RoundedCornerShape(Dimens.CornerRadius.large),
      colors =
          CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.surface,
              contentColor = MaterialTheme.colorScheme.onSurface),
      elevation = CardDefaults.cardElevation(defaultElevation = Dimens.Elevation.small)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Dimens.Padding.medium),
            verticalAlignment = Alignment.CenterVertically) {
              // Rank number
              RankBadge(rank = entry.rank)

              Spacer(modifier = Modifier.width(Dimens.Spacing.medium))

              // User avatar
              UserAvatar(photoUrl = entry.photoUrl, displayName = entry.displayName)

              Spacer(modifier = Modifier.width(Dimens.Spacing.medium))

              // User info
              Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
                Text(
                    text =
                        stringResource(
                            R.string.leaderboard_activities_joined, entry.streakActivities),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
              }

              // Crown badge for top 3
              if (isTopThree) {
                Spacer(modifier = Modifier.width(Dimens.Spacing.small))
                TopThreeBadge(rank = entry.rank)
              }
            }
      }
}

/** Rank number display on the left side of the card. */
@Composable
private fun RankBadge(rank: Int) {
  Box(
      modifier =
          Modifier.size(Dimens.Leaderboard.rankBadgeSize)
              .background(color = MaterialTheme.colorScheme.onSurface, shape = CircleShape),
      contentAlignment = Alignment.Center) {
        Text(
            text = rank.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface)
      }
}

/**
 * User avatar using ProfilePhotoImage component.
 *
 * @param photoUrl The user's profile photo URL, or null to show default avatar.
 * @param displayName The user's display name for accessibility.
 */
@Composable
private fun UserAvatar(photoUrl: String?, displayName: String) {
  ProfilePhotoImage(
      photoUrl = photoUrl,
      contentDescription = stringResource(R.string.user_avatar_description, displayName),
      size = Dimens.GroupDetail.memberProfilePictureSize,
      showLoadingIndicator = false)
}

/** Crown badge for top 3 positions with appropriate colors. */
@Composable
private fun TopThreeBadge(rank: Int) {
  val customColors = MaterialTheme.customColors

  val badgeColor =
      when (rank) {
        1 -> customColors.gold
        2 -> customColors.silver
        3 -> customColors.bronze
        else -> Color.Transparent
      }

  Box(
      modifier =
          Modifier.size(Dimens.Leaderboard.topThreeBadgeSize)
              .background(
                  color = badgeColor, shape = RoundedCornerShape(Dimens.CornerRadius.medium)),
      contentAlignment = Alignment.Center) {
        // Crown emoji
        Text(text = "👑", style = MaterialTheme.typography.titleMedium)
      }
}
