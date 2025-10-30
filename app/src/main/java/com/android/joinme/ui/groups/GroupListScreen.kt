package com.android.joinme.ui.groups

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.joinme.model.groups.Group
import com.android.joinme.ui.components.BubbleAction
import com.android.joinme.ui.components.BubbleAlignment
import com.android.joinme.ui.components.FloatingActionBubbles
import com.android.joinme.ui.profile.ProfileScreen
import com.android.joinme.ui.profile.ProfileTopBar
import com.android.joinme.ui.theme.ScrimOverlayColorDarkTheme
import com.android.joinme.ui.theme.ScrimOverlayColorLightTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

/** Dimensions and styling constants for GroupListScreen and its components */
private object GroupListScreenDimensions {
  // FAB (Floating Action Button)
  val fabCornerRadius = 24.dp
  val fabBottomPadding = 84.dp

  // List Layout
  val listHorizontalPadding = 16.dp
  val listVerticalPadding = 12.dp
  val cardSpacing = 12.dp

  // Card Menu Overlay
  val menuBubbleSpacing = 8.dp
  val menuRightPadding = 60.dp
  val menuHeight = 272.dp // 5 bubbles × 48dp + 4 spacers × 8dp
  val menuBottomMargin = 80.dp // Space for FAB
  val menuTopMargin = 80.dp // Space below app bar

  // Scrim Animation
  const val scrimAnimationDuration = 300 // milliseconds
  const val scrimZIndex = 1000f

  // GroupCard
  val cardMinHeight = 86.dp
  val cardPadding = 12.dp
  val titleDescriptionSpacing = 4.dp
  val descriptionMembersSpacing = 6.dp
  const val descriptionAlpha = 0.8f
  const val membersAlpha = 0.7f

  // MenuBubble
  val bubbleCornerRadius = 24.dp
  val bubbleHeight = 48.dp
  val bubbleHorizontalPadding = 16.dp
  val bubbleVerticalPadding = 8.dp
  val bubbleIconTextSpacing = 8.dp
  val bubbleIconSize = 20.dp
}

/**
 * Contains test tags for UI elements in the GroupListScreen.
 *
 * These tags are used in instrumentation tests to identify and interact with specific UI
 * components.
 */
object GroupListScreenTestTags {
  /** Test tag for the screen title. */
  const val TITLE = "groups:title"

  /** Test tag for the "Join a new group" floating action button. */
  const val ADD_NEW_GROUP = "groups:addNewGroup"

  /** Test tag for the lazy column containing the list of groups. */
  const val LIST = "groups:list"

  /** Test tag for the empty state view when no groups are present. */
  const val EMPTY = "groups:empty"

  /** Test tag for the "Join with link" bubble action. */
  const val JOIN_WITH_LINK_BUBBLE = "groupJoinWithLinkBubble"

  /** Test tag for the "Create a group" bubble action. */
  const val CREATE_GROUP_BUBBLE = "groupCreateBubble"

  // Group card menu bubbles
  const val VIEW_GROUP_DETAILS_BUBBLE = "viewGroupDetailsBubble"
  const val LEAVE_GROUP_BUBBLE = "leaveGroupBubble"
  const val SHARE_GROUP_BUBBLE = "shareGroupBubble"
  const val EDIT_GROUP_BUBBLE = "editGroupBubble"
  const val DELETE_GROUP_BUBBLE = "deleteGroupBubble"

  /**
   * Generates a test tag for a specific group card.
   *
   * @param id The unique identifier of the group.
   * @return A string test tag in the format "group:card:{id}".
   */
  fun cardTag(id: String) = "group:card:$id"

  /**
   * Generates a test tag for the "more options" button of a specific group.
   *
   * @param id The unique identifier of the group.
   * @return A string test tag in the format "group:more:{id}".
   */
  fun moreTag(id: String) = "group:more:$id"
}

/**
 * Displays a list of groups that the user belongs to.
 *
 * This screen shows all groups with their names, descriptions, and member counts. If no groups
 * exist, an empty state message is displayed. Users can tap on a group card to view details, access
 * a menu via the more options button, or join a new group via the floating action button.
 *
 * This screen follows the MVVM pattern by observing the ViewModel's state via StateFlow.
 *
 * @param viewModel The ViewModel managing the group list state and business logic. Defaults to a
 *   new instance provided by viewModel().
 * @param onJoinWithLink Callback invoked when user taps "Join with link" option
 * @param onCreateGroup Callback invoked when user taps "Create a group" option
 * @param onGroup Callback invoked when the user taps on a group card, receiving the selected
 *   [Group].
 * @param onViewGroupDetails Callback invoked when the user taps "View Group Details" for a group.
 * @param onLeaveGroup Callback invoked when the user taps "Leave Group" for a group.
 * @param onShareGroup Callback invoked when the user taps "Share Group" for a group.
 * @param onEditGroup Callback invoked when the user taps "Edit Group" for a group.
 * @param onDeleteGroup Callback invoked when the user taps "Delete Group" for a group.
 * @param onBackClick Callback invoked when the user taps the back button in the top bar.
 * @param onProfileClick Callback invoked when the user taps the profile icon in the top bar.
 * @param onEditClick Callback invoked when the user taps the edit icon in the top bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupListScreen(
    viewModel: GroupListViewModel = viewModel(),
    onJoinWithLink: () -> Unit = {},
    onCreateGroup: () -> Unit = {},
    onGroup: (Group) -> Unit = {},
    onViewGroupDetails: (Group) -> Unit = {},
    onLeaveGroup: (Group) -> Unit = {},
    onShareGroup: (Group) -> Unit = {},
    onEditGroup: (Group) -> Unit = {},
    onDeleteGroup: (Group) -> Unit = {},
    onBackClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onEditClick: () -> Unit = {}
) {
  val uiState by viewModel.uiState.collectAsState()
  val groups = uiState.groups
  val currentUserId = Firebase.auth.currentUser?.uid // Current user ID from Firebase Auth
  val context = LocalContext.current

  // State for showing/hiding floating bubbles in the join/create group FAB
  var showJoinBubbles by remember { mutableStateOf(false) }

  // State for tracking which group's menu is currently open (null = none open)
  var openMenuGroupId by remember { mutableStateOf<String?>(null) }

  // State for tracking the position of the clicked three-dot button
  var menuButtonYPosition by remember { mutableStateOf(0f) }

  // Define bubble actions for join/create group FAB
  val groupJoinBubbleActions = remember {
    listOf(
        BubbleAction(
            text = "Join with link",
            icon = Icons.Default.Link,
            onClick = {
              Toast.makeText(context, "Not implemented yet", Toast.LENGTH_SHORT).show()
              onJoinWithLink()
            },
            testTag = GroupListScreenTestTags.JOIN_WITH_LINK_BUBBLE),
        BubbleAction(
            text = "Create a group",
            icon = Icons.Default.Add,
            onClick = onCreateGroup,
            testTag = GroupListScreenTestTags.CREATE_GROUP_BUBBLE))
  }

  Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        topBar = {
          ProfileTopBar(
              currentScreen = ProfileScreen.GROUPS,
              onBackClick = onBackClick,
              onProfileClick = onProfileClick,
              onGroupClick = {},
              onEditClick = onEditClick)
        },
        floatingActionButton = {
          ExtendedFloatingActionButton(
              modifier = Modifier.testTag(GroupListScreenTestTags.ADD_NEW_GROUP),
              onClick = {
                // Close any open card menu
                openMenuGroupId = null
                // Toggle the join/create menu
                showJoinBubbles = !showJoinBubbles
              },
              icon = {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Join a new group",
                    tint = MaterialTheme.colorScheme.onPrimary)
              },
              shape = RoundedCornerShape(GroupListScreenDimensions.fabCornerRadius),
              text = { Text("Join a new group", color = MaterialTheme.colorScheme.onPrimary) },
              containerColor =
                  if (showJoinBubbles) MaterialTheme.colorScheme.surfaceVariant
                  else MaterialTheme.colorScheme.primary)
        },
        floatingActionButtonPosition = FabPosition.Center,
    ) { pd ->
      Box(modifier = Modifier.fillMaxSize()) {
        // Main content
        if (groups.isNotEmpty()) {
          LazyColumn(
              modifier =
                  Modifier.fillMaxSize()
                      .padding(horizontal = GroupListScreenDimensions.listHorizontalPadding)
                      .padding(
                          bottom =
                              pd.calculateBottomPadding() +
                                  GroupListScreenDimensions.fabBottomPadding)
                      .padding(top = pd.calculateTopPadding())
                      .testTag(GroupListScreenTestTags.LIST),
              contentPadding =
                  PaddingValues(vertical = GroupListScreenDimensions.listVerticalPadding)) {
                items(groups) { group ->
                  GroupCard(
                      group = group,
                      onClick = { onGroup(group) },
                      onMoreOptions = { yPosition ->
                        // Close the join/create group menu if it's open
                        showJoinBubbles = false
                        // Toggle the card menu
                        openMenuGroupId = if (openMenuGroupId == group.id) null else group.id
                        menuButtonYPosition = yPosition
                      })
                  Spacer(Modifier.height(GroupListScreenDimensions.cardSpacing))
                }
              }
        } else {
          Box(
              modifier = Modifier.fillMaxSize().padding(pd).testTag(GroupListScreenTestTags.EMPTY),
              contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Text(
                      text = "You are currently not",
                      style = MaterialTheme.typography.bodyMedium,
                      color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                  Text(
                      text = "assigned to a group…",
                      style = MaterialTheme.typography.bodyMedium,
                      color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                }
              }
        }
      }
    }

    // Card menu overlay - OUTSIDE Scaffold, covers everything including FAB
    openMenuGroupId?.let { groupId ->
      val selectedGroup = groups.find { it.id == groupId }
      selectedGroup?.let { group ->
        val density = LocalDensity.current
        val configuration = androidx.compose.ui.platform.LocalConfiguration.current
        val screenHeightDp = configuration.screenHeightDp.dp

        // Convert pixel position to dp
        val buttonTopPaddingDp = with(density) { menuButtonYPosition.toDp() }

        // Calculate dynamic menu height based on ownership
        val isOwner = group.ownerId == currentUserId
        val numberOfButtons = if (isOwner) 5 else 3 // 5 if owner, 3 if not
        val dynamicMenuHeight =
            GroupListScreenDimensions.bubbleHeight.times(numberOfButtons) +
                GroupListScreenDimensions.menuBubbleSpacing.times(numberOfButtons - 1)

        // Check if menu would go off-screen
        val spaceBelow =
            screenHeightDp - buttonTopPaddingDp - GroupListScreenDimensions.menuBottomMargin
        val shouldPositionAbove = spaceBelow < dynamicMenuHeight

        // Calculate final top position
        val topPaddingDp =
            if (shouldPositionAbove) {
              // Position menu so bottom aligns with button (menu above button)
              (buttonTopPaddingDp - dynamicMenuHeight).coerceAtLeast(
                  GroupListScreenDimensions.menuTopMargin)
            } else {
              // Normal position (menu below button)
              buttonTopPaddingDp
            }

        // Animate scrim opacity for smooth fade-in/fade-out (same as join menu)
        val scrimAlpha by
            animateFloatAsState(
                targetValue = 1f,
                animationSpec =
                    tween(durationMillis = GroupListScreenDimensions.scrimAnimationDuration),
                label = "cardMenuScrimAlpha")
        val isDarkTheme = isSystemInDarkTheme()
        val scrimBaseColor =
            if (isDarkTheme) ScrimOverlayColorDarkTheme else ScrimOverlayColorLightTheme
        val scrimColor = scrimBaseColor.copy(alpha = scrimBaseColor.alpha * scrimAlpha)

        // Full-screen scrim overlay with menu bubbles
        Box(
            modifier =
                Modifier.fillMaxSize()
                    .zIndex(GroupListScreenDimensions.scrimZIndex)
                    .background(scrimColor)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { openMenuGroupId = null })) {
              // Menu bubbles positioned dynamically based on clicked card
              Column(
                  modifier =
                      Modifier.align(Alignment.TopEnd)
                          .padding(
                              top = topPaddingDp, end = GroupListScreenDimensions.menuRightPadding),
                  verticalArrangement =
                      Arrangement.spacedBy(GroupListScreenDimensions.menuBubbleSpacing),
                  horizontalAlignment = Alignment.End) {
                    // View Group Details
                    MenuBubble(
                        text = "View Group Details",
                        icon = Icons.Default.Visibility,
                        onClick = {
                          onViewGroupDetails(group)
                          openMenuGroupId = null
                        },
                        testTag = GroupListScreenTestTags.VIEW_GROUP_DETAILS_BUBBLE)

                    // Leave Group
                    MenuBubble(
                        text = "Leave Group",
                        icon = Icons.Default.ExitToApp,
                        onClick = {
                          onLeaveGroup(group)
                          openMenuGroupId = null
                        },
                        testTag = GroupListScreenTestTags.LEAVE_GROUP_BUBBLE)

                    // Share Group
                    MenuBubble(
                        text = "Share Group",
                        icon = Icons.Default.Share,
                        onClick = {
                          onShareGroup(group)
                          openMenuGroupId = null
                        },
                        testTag = GroupListScreenTestTags.SHARE_GROUP_BUBBLE)

                    // Edit Group - Only shown if current user is the owner
                    if (group.ownerId == currentUserId) {
                      MenuBubble(
                          text = "Edit Group",
                          icon = Icons.Default.Edit,
                          onClick = {
                            onEditGroup(group)
                            openMenuGroupId = null
                          },
                          testTag = GroupListScreenTestTags.EDIT_GROUP_BUBBLE)
                    }

                    // Delete Group - Only shown if current user is the owner
                    if (group.ownerId == currentUserId) {
                      MenuBubble(
                          text = "Delete Group",
                          icon = Icons.Default.Delete,
                          onClick = {
                            onDeleteGroup(group)
                            openMenuGroupId = null
                          },
                          testTag = GroupListScreenTestTags.DELETE_GROUP_BUBBLE)
                    }
                  }
            }
      }
    }

    // Floating action bubbles overlay for join/create group
    // Positioned at bottom-right, using theme colors for dark mode support
    FloatingActionBubbles(
        visible = showJoinBubbles,
        onDismiss = { showJoinBubbles = false },
        actions = groupJoinBubbleActions,
        bubbleAlignment = BubbleAlignment.BOTTOM_END,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface)
  } // Close outer Box
}

/**
 * Displays a single group as a card with its information and action buttons.
 *
 * The card shows the group's name, description (if present), and member count. It includes a
 * clickable surface to view the group details and a more options button for additional actions.
 *
 * @param group The [Group] object containing the group's data to display.
 * @param onClick Callback invoked when the user taps anywhere on the card.
 * @param onMoreOptions Callback invoked when the user taps the more options button, receives Y
 *   position.
 */
@Composable
private fun GroupCard(group: Group, onClick: () -> Unit, onMoreOptions: (Float) -> Unit) {
  Card(
      modifier =
          Modifier.fillMaxWidth()
              .heightIn(min = GroupListScreenDimensions.cardMinHeight)
              .clickable { onClick() }
              .testTag(GroupListScreenTestTags.cardTag(group.id)),
      colors =
          CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(GroupListScreenDimensions.cardPadding),
            verticalAlignment = Alignment.Top) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                Spacer(Modifier.height(GroupListScreenDimensions.titleDescriptionSpacing))
                if (group.description.isNotBlank()) {
                  Text(
                      text = group.description,
                      style = MaterialTheme.typography.bodySmall,
                      maxLines = 2,
                      overflow = TextOverflow.Ellipsis,
                      color =
                          MaterialTheme.colorScheme.onPrimaryContainer.copy(
                              alpha = GroupListScreenDimensions.descriptionAlpha))
                }
                Spacer(Modifier.height(GroupListScreenDimensions.descriptionMembersSpacing))
                Text(
                    text = "members: ${group.memberIds.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color =
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(
                            alpha = GroupListScreenDimensions.membersAlpha))
              }
              var buttonYPosition by remember { mutableStateOf(0f) }

              IconButton(
                  onClick = { onMoreOptions(buttonYPosition) },
                  modifier =
                      Modifier.testTag(GroupListScreenTestTags.moreTag(group.id))
                          .onGloballyPositioned { coordinates ->
                            buttonYPosition = coordinates.positionInRoot().y
                          }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More options",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer)
                  }
            }
      }
}

/** Individual menu bubble button for group card menus */
@Composable
private fun MenuBubble(text: String, icon: ImageVector, onClick: () -> Unit, testTag: String = "") {
  FloatingActionButton(
      onClick = onClick,
      containerColor = MaterialTheme.colorScheme.surface,
      shape = RoundedCornerShape(GroupListScreenDimensions.bubbleCornerRadius),
      modifier = Modifier.height(GroupListScreenDimensions.bubbleHeight).testTag(testTag)) {
        Row(
            modifier =
                Modifier.padding(
                    horizontal = GroupListScreenDimensions.bubbleHorizontalPadding,
                    vertical = GroupListScreenDimensions.bubbleVerticalPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement =
                Arrangement.spacedBy(GroupListScreenDimensions.bubbleIconTextSpacing)) {
              Icon(
                  imageVector = icon,
                  contentDescription = text,
                  tint = MaterialTheme.colorScheme.onSurface,
                  modifier = Modifier.size(GroupListScreenDimensions.bubbleIconSize))
              Text(
                  text = text,
                  color = MaterialTheme.colorScheme.onSurface,
                  style = MaterialTheme.typography.bodyMedium)
            }
      }
}
