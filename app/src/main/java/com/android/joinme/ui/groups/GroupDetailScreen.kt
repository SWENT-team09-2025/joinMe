package com.android.joinme.ui.groups

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Message
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.joinme.R
import com.android.joinme.model.event.EventType
import com.android.joinme.model.event.displayString
import com.android.joinme.model.event.getColor
import com.android.joinme.model.event.getOnColor
import com.android.joinme.model.event.getOnContainerColor
import com.android.joinme.model.profile.Profile
import com.android.joinme.ui.profile.ProfilePhotoImage
import com.android.joinme.ui.theme.Dimens
import com.android.joinme.ui.theme.buttonColorsForEventType
import com.android.joinme.ui.theme.customColors

/**
 * Main screen for displaying group details including members and events.
 *
 * @param groupId The unique identifier of the group to display.
 * @param viewModel The ViewModel managing the screen state and data.
 * @param onBackClick Callback when the back button is clicked.
 * @param onGroupEventsClick Callback when the "Group Events" button is clicked.
 * @param onMemberClick Callback when a member profile is clicked, receives the member's UID.
 * @param onNavigateToChat Callback invoked when the user wants to navigate to the group chat,
 *   receives chatId and chatTitle.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    groupId: String,
    viewModel: GroupDetailViewModel = viewModel(factory = GroupDetailViewModelFactory()),
    onBackClick: () -> Unit = {},
    onGroupEventsClick: () -> Unit = {},
    onMemberClick: (String) -> Unit = {},
    onNavigateToChat: (String, String, Int) -> Unit = { _, _, _ -> }
) {
  LaunchedEffect(groupId) { viewModel.loadGroupDetails(groupId) }

  val uiState by viewModel.uiState.collectAsState()

  Scaffold(
      topBar = {
        TopAppBar(
            title = {},
            navigationIcon = {
              IconButton(onClick = onBackClick) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
              }
            },
            actions = {
              // Only show category when group is loaded
              if (uiState.group != null) {
                Text(
                    text = uiState.group!!.category.displayString(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = uiState.group!!.category.getOnContainerColor(),
                    modifier = Modifier.padding(end = Dimens.Spacing.medium))
              }
            },
            colors =
                TopAppBarDefaults.topAppBarColors(
                    // Use group category color if loaded, otherwise default
                    containerColor =
                        uiState.group?.category?.getColor() ?: MaterialTheme.colorScheme.surface))
      }) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
          when {
            uiState.isLoading -> {
              CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            uiState.error != null -> {
              ErrorContent(
                  message = uiState.error ?: "Unknown error",
                  onRetry = { viewModel.loadGroupDetails(groupId) },
                  modifier = Modifier.align(Alignment.Center))
            }
            uiState.group != null -> {
              GroupContent(
                  groupId = groupId,
                  groupCategory = uiState.group!!.category,
                  groupName = uiState.group!!.name,
                  groupDescription = uiState.group!!.description,
                  members = uiState.members,
                  membersCount = uiState.group!!.membersCount,
                  onGroupEventsClick = onGroupEventsClick,
                  onMemberClick = onMemberClick,
                  onNavigateToChat = onNavigateToChat)
            }
          }
        }
      }
}

/** Main content displaying group information and members. */
@Composable
private fun GroupContent(
    groupId: String,
    groupCategory: EventType,
    groupName: String,
    groupDescription: String,
    members: List<Profile>,
    membersCount: Int,
    onGroupEventsClick: () -> Unit,
    onMemberClick: (String) -> Unit,
    onNavigateToChat: (String, String, Int) -> Unit
) {
  Column(modifier = Modifier.fillMaxSize().background(groupCategory.getColor())) {
    Column(modifier = Modifier.fillMaxWidth().padding(Dimens.Padding.large)) {
      Box(
          modifier =
              Modifier.size(Dimens.GroupDetail.pictureSize)
                  .clip(CircleShape)
                  .background(MaterialTheme.colorScheme.surface),
          contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(id = R.drawable.group_default_picture),
                contentDescription = "Group picture",
                modifier = Modifier.size(Dimens.GroupDetail.pictureImageSize),
                contentScale = ContentScale.Fit)
          }

      Spacer(modifier = Modifier.height(Dimens.Spacing.large))

      Text(
          text = groupName,
          style = MaterialTheme.typography.headlineMedium,
          fontWeight = FontWeight.Bold,
          color = groupCategory.getOnContainerColor())

      Spacer(modifier = Modifier.height(Dimens.Spacing.small))

      Text(
          text = groupDescription,
          style = MaterialTheme.typography.bodyLarge,
          color = groupCategory.getOnContainerColor())

      Spacer(modifier = Modifier.height(Dimens.Spacing.large))
    }

    Box(
        modifier =
            Modifier.weight(1f)
                .fillMaxWidth()
                .padding(horizontal = Dimens.Padding.large)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0f))
                .border(
                    width = Dimens.BorderWidth.medium,
                    color = groupCategory.getOnContainerColor(),
                    shape = RoundedCornerShape(Dimens.CornerRadius.large))
                .padding(Dimens.Padding.medium)) {
          LazyColumn(
              modifier = Modifier.fillMaxSize().testTag("membersList"),
              verticalArrangement = Arrangement.spacedBy(Dimens.Spacing.itemSpacing)) {
                items(members) { member ->
                  MemberItem(
                      profile = member,
                      categoryColor = groupCategory,
                      onClick = { onMemberClick(member.uid) })
                }
              }
        }

    Spacer(modifier = Modifier.height(Dimens.Spacing.medium))

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.Padding.large)) {
      // Row to hold chat FAB and Group Events button
      Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing.small),
          verticalAlignment = Alignment.CenterVertically) {
            // Chat FAB positioned to the left
            FloatingActionButton(
                onClick = { onNavigateToChat(groupId, groupName, membersCount) },
                containerColor = groupCategory.getColor(),
                contentColor = groupCategory.getOnColor(),
                shape = RoundedCornerShape(Dimens.GroupDetail.eventsButtonCornerRadius),
                modifier = Modifier.testTag("chatFabBottom")) {
                  Icon(
                      imageVector = Icons.AutoMirrored.Filled.Message,
                      contentDescription = "Open Chat",
                  )
                }

            // Group Events button - now using weight to fill remaining space
            Button(
                onClick = onGroupEventsClick,
                modifier = Modifier.weight(1f).height(Dimens.Button.standardHeight),
                colors = MaterialTheme.customColors.buttonColorsForEventType(groupCategory),
                border =
                    BorderStroke(
                        width = Dimens.BorderWidth.medium, color = groupCategory.getOnColor()),
                shape = RoundedCornerShape(Dimens.GroupDetail.eventsButtonCornerRadius)) {
                  Text(
                      text = "Group Events",
                      style = MaterialTheme.typography.headlineSmall,
                      color = groupCategory.getOnColor(),
                      fontWeight = FontWeight.Medium)
                }
          }

      Spacer(modifier = Modifier.height(Dimens.Spacing.medium))

      Text(
          text = "members : $membersCount",
          style = MaterialTheme.typography.bodyMedium,
          color = groupCategory.getOnContainerColor(),
          modifier = Modifier.align(Alignment.End))

      Spacer(modifier = Modifier.height(Dimens.Spacing.large))
    }
  }
}

@Composable
private fun MemberItem(profile: Profile, categoryColor: EventType, onClick: () -> Unit = {}) {
  Row(
      modifier =
          Modifier.fillMaxWidth()
              .clickable { onClick() }
              .padding(vertical = Dimens.Padding.extraSmall),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Start) {
        // Profile photo
        ProfilePhotoImage(
            photoUrl = profile.photoUrl,
            contentDescription = "Profile photo of ${profile.username}",
            modifier = Modifier.size(Dimens.GroupDetail.memberProfilePictureSize))

        Spacer(modifier = Modifier.width(Dimens.Spacing.medium))

        Text(
            text = profile.username,
            style = MaterialTheme.typography.titleMedium,
            color = categoryColor.getOnContainerColor(),
            fontWeight = FontWeight.Normal)
      }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
  Column(
      modifier = modifier.padding(Dimens.Spacing.medium),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.height(Dimens.Spacing.medium))
        Button(onClick = onRetry) { Text("Retry") }
      }
}
