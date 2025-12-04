package com.android.joinme.ui.profile

/** This file was implemented with the help of AI */
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.joinme.R
import com.android.joinme.model.profile.Profile
import com.android.joinme.ui.theme.Dimens

/**
 * Contains test tags for UI elements in the FollowListScreen.
 *
 * These tags are used in instrumentation tests to identify and interact with specific UI
 * components.
 */
object FollowListScreenTestTags {
  const val SCREEN = "followListScreen"
  const val TOP_BAR = "followListTopBar"
  const val BACK_BUTTON = "followListBackButton"
  const val TAB_ROW = "followListTabRow"
  const val FOLLOWERS_TAB = "followListFollowersTab"
  const val FOLLOWING_TAB = "followListFollowingTab"
  const val LIST = "followListList"
  const val LOADING_INDICATOR = "followListLoadingIndicator"
  const val PAGINATION_LOADING_INDICATOR = "followListPaginationLoadingIndicator"
  const val ERROR_MESSAGE = "followListErrorMessage"
  const val EMPTY_MESSAGE = "followListEmptyMessage"

  fun profileItemTag(userId: String) = "followListProfileItem:$userId"
}

/**
 * Screen displaying a user's followers and following lists with tab switching.
 *
 * This screen shows:
 * - Tab navigation to switch between Followers and Following
 * - List of user profiles with photo, username, and bio
 * - Tap on a profile to navigate to their public profile
 * - Loading, error, and empty states
 *
 * Uses lazy loading - only fetches data for a tab when it's first selected.
 *
 * @param userId The unique identifier of the user whose followers/following to display
 * @param initialTab The tab to show initially (defaults to FOLLOWERS)
 * @param viewModel The ViewModel managing the list state and business logic
 * @param onBackClick Callback invoked when the user taps the back button
 * @param onProfileClick Callback invoked when the user taps on a profile in the list
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowListScreen(
    userId: String,
    initialTab: FollowTab = FollowTab.FOLLOWERS,
    viewModel: FollowListViewModel = viewModel(),
    onBackClick: () -> Unit = {},
    onProfileClick: (String) -> Unit = {}
) {
  val selectedTab by viewModel.selectedTab.collectAsState()
  val profileUsername by viewModel.profileUsername.collectAsState()
  val followers by viewModel.followers.collectAsState()
  val following by viewModel.following.collectAsState()
  val isLoading by viewModel.isLoading.collectAsState()
  val isLoadingMore by viewModel.isLoadingMore.collectAsState()
  val hasMoreFollowers by viewModel.hasMoreFollowers.collectAsState()
  val hasMoreFollowing by viewModel.hasMoreFollowing.collectAsState()
  val error by viewModel.error.collectAsState()

  // Initialize the screen with the userId and initial tab
  LaunchedEffect(userId, initialTab) { viewModel.initialize(userId, initialTab) }

  Scaffold(
      modifier = Modifier.fillMaxSize().testTag(FollowListScreenTestTags.SCREEN),
      topBar = {
        Column {
          CenterAlignedTopAppBar(
              modifier = Modifier.testTag(FollowListScreenTestTags.TOP_BAR),
              title = { Text(text = profileUsername, style = MaterialTheme.typography.titleLarge) },
              navigationIcon = {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.testTag(FollowListScreenTestTags.BACK_BUTTON)) {
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
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)) {
              // Tab Row for switching between Followers and Following
              TabRow(
                  selectedTabIndex = selectedTab.ordinal,
                  modifier = Modifier.fillMaxWidth().testTag(FollowListScreenTestTags.TAB_ROW),
                  containerColor = MaterialTheme.colorScheme.surface,
                  contentColor = MaterialTheme.colorScheme.primary) {
                    Tab(
                        selected = selectedTab == FollowTab.FOLLOWERS,
                        onClick = { viewModel.selectTab(FollowTab.FOLLOWERS) },
                        modifier = Modifier.testTag(FollowListScreenTestTags.FOLLOWERS_TAB),
                        text = {
                          Text(
                              text = stringResource(R.string.followers),
                              style = MaterialTheme.typography.titleMedium)
                        })
                    Tab(
                        selected = selectedTab == FollowTab.FOLLOWING,
                        onClick = { viewModel.selectTab(FollowTab.FOLLOWING) },
                        modifier = Modifier.testTag(FollowListScreenTestTags.FOLLOWING_TAB),
                        text = {
                          Text(
                              text = stringResource(R.string.following),
                              style = MaterialTheme.typography.titleMedium)
                        })
                  }

              // Content based on loading/error/data state
              FollowListContent(
                  isLoading = isLoading,
                  error = error,
                  selectedTab = selectedTab,
                  followers = followers,
                  following = following,
                  hasMoreFollowers = hasMoreFollowers,
                  hasMoreFollowing = hasMoreFollowing,
                  isLoadingMore = isLoadingMore,
                  onProfileClick = onProfileClick,
                  onLoadMore = { viewModel.loadMore() })
            }
      }
}

/**
 * Displays the main content area with loading, error, or data states.
 *
 * @param isLoading Whether the initial data is loading
 * @param error Error message if loading failed
 * @param selectedTab The currently selected tab
 * @param followers List of followers
 * @param following List of following
 * @param hasMoreFollowers Whether there are more followers to load
 * @param hasMoreFollowing Whether there are more following to load
 * @param isLoadingMore Whether pagination is in progress
 * @param onProfileClick Callback when a profile is clicked
 * @param onLoadMore Callback to load more data
 */
@Composable
private fun FollowListContent(
    isLoading: Boolean,
    error: String?,
    selectedTab: FollowTab,
    followers: List<Profile>,
    following: List<Profile>,
    hasMoreFollowers: Boolean,
    hasMoreFollowing: Boolean,
    isLoadingMore: Boolean,
    onProfileClick: (String) -> Unit,
    onLoadMore: () -> Unit
) {
  Box(modifier = Modifier.fillMaxSize()) {
    when {
      isLoading -> {
        // Loading state
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          CircularProgressIndicator(
              modifier =
                  Modifier.size(Dimens.LoadingIndicator.large)
                      .testTag(FollowListScreenTestTags.LOADING_INDICATOR))
        }
      }
      error != null -> {
        // Error state
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Text(
              text = error,
              modifier =
                  Modifier.padding(Dimens.Padding.large)
                      .testTag(FollowListScreenTestTags.ERROR_MESSAGE),
              style = MaterialTheme.typography.bodyLarge,
              color = MaterialTheme.colorScheme.error,
              textAlign = TextAlign.Center)
        }
      }
      else -> {
        // Content state - show the appropriate list
        val currentList =
            when (selectedTab) {
              FollowTab.FOLLOWERS -> followers
              FollowTab.FOLLOWING -> following
            }

        val hasMore =
            when (selectedTab) {
              FollowTab.FOLLOWERS -> hasMoreFollowers
              FollowTab.FOLLOWING -> hasMoreFollowing
            }

        if (currentList.isEmpty()) {
          // Empty state
          EmptyListMessage(selectedTab)
        } else {
          // Profile list
          ProfileList(
              profiles = currentList,
              isLoadingMore = isLoadingMore,
              hasMore = hasMore,
              onProfileClick = onProfileClick,
              onLoadMore = onLoadMore)
        }
      }
    }
  }
}

/**
 * Displays an empty state message when there are no followers/following.
 *
 * @param tab The currently selected tab
 */
@Composable
private fun EmptyListMessage(tab: FollowTab) {
  Box(
      modifier = Modifier.fillMaxSize().testTag(FollowListScreenTestTags.EMPTY_MESSAGE),
      contentAlignment = Alignment.Center) {
        Text(
            text =
                when (tab) {
                  FollowTab.FOLLOWERS -> stringResource(R.string.no_followers_yet)
                  FollowTab.FOLLOWING -> stringResource(R.string.not_following_anyone_yet)
                },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(Dimens.Padding.large))
      }
}

/**
 * Displays a scrollable list of user profiles.
 *
 * @param profiles The list of profiles to display
 * @param isLoadingMore Whether more profiles are currently being loaded
 * @param hasMore Whether there are more profiles to load
 * @param onProfileClick Callback invoked when a profile is tapped
 * @param onLoadMore Callback invoked when the user scrolls near the end of the list
 */
@Composable
private fun ProfileList(
    profiles: List<Profile>,
    isLoadingMore: Boolean,
    hasMore: Boolean,
    onProfileClick: (String) -> Unit,
    onLoadMore: () -> Unit
) {
  val listState = rememberLazyListState()

  // Detect when we're near the end of the list
  val shouldLoadMore = remember {
    derivedStateOf {
      val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
      val totalItems = listState.layoutInfo.totalItemsCount
      lastVisibleItem != null &&
          lastVisibleItem.index >= totalItems - 5 &&
          !isLoadingMore &&
          hasMore
    }
  }

  // Trigger load more when scrolling near the end
  LaunchedEffect(shouldLoadMore.value) {
    if (shouldLoadMore.value) {
      onLoadMore()
    }
  }

  LazyColumn(
      state = listState,
      modifier = Modifier.fillMaxSize().testTag(FollowListScreenTestTags.LIST),
      verticalArrangement = Arrangement.spacedBy(Dimens.Spacing.small)) {
        item { Spacer(modifier = Modifier.height(Dimens.Spacing.small)) }

        items(profiles, key = { it.uid }) { profile ->
          ProfileListItem(profile = profile, onClick = { onProfileClick(profile.uid) })
        }

        // Pagination loading indicator
        if (isLoadingMore) {
          item {
            Box(
                modifier =
                    Modifier.fillMaxWidth()
                        .padding(Dimens.Padding.medium)
                        .testTag(FollowListScreenTestTags.PAGINATION_LOADING_INDICATOR),
                contentAlignment = Alignment.Center) {
                  CircularProgressIndicator(
                      modifier = Modifier.size(Dimens.LoadingIndicator.medium))
                }
          }
        }

        item { Spacer(modifier = Modifier.height(Dimens.Spacing.small)) }
      }
}

/**
 * A single profile item in the list.
 *
 * Displays:
 * - Profile photo (or default avatar)
 * - Username
 * - Bio (truncated to 2 lines)
 *
 * @param profile The profile to display
 * @param onClick Callback invoked when the item is tapped
 */
@Composable
private fun ProfileListItem(profile: Profile, onClick: () -> Unit) {
  Row(
      modifier =
          Modifier.fillMaxWidth()
              .clickable(onClick = onClick)
              .padding(horizontal = Dimens.Padding.medium, vertical = Dimens.Padding.small)
              .testTag(FollowListScreenTestTags.profileItemTag(profile.uid)),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Start) {
        // Profile photo
        ProfilePhotoImage(
            photoUrl = profile.photoUrl,
            contentDescription = "${profile.username}'s profile picture",
            size = Dimens.LoadingIndicator.large,
            showLoadingIndicator = false)

        Spacer(modifier = Modifier.width(Dimens.Spacing.medium))

        // Username and bio
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
          Text(
              text = profile.username,
              style = MaterialTheme.typography.bodyLarge,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis)

          if (!profile.bio.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(Dimens.Spacing.extraSmall))
            Text(
                text = profile.bio,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis)
          }
        }
      }
}
