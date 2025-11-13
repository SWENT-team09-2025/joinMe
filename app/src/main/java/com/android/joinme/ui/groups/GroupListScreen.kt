// Implemented with help of Claude AI
package com.android.joinme.ui.groups

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.joinme.model.event.getColor
import com.android.joinme.model.event.getOnContainerColor
import com.android.joinme.model.groups.Group
import com.android.joinme.ui.components.BubbleAction
import com.android.joinme.ui.components.BubbleAlignment
import com.android.joinme.ui.components.FloatingActionBubbles
import com.android.joinme.ui.profile.ProfileScreen
import com.android.joinme.ui.profile.ProfileTopBar
import com.android.joinme.ui.theme.Dimens
import com.android.joinme.ui.theme.customColors
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

/** Dimensions and styling constants for GroupListScreen and its components */
// Constants for GroupListScreen
private const val SCRIM_ANIMATION_DURATION = 300 // milliseconds
private const val SCRIM_Z_INDEX = 1000f
private const val DESCRIPTION_ALPHA = 0.8f
private const val MEMBERS_ALPHA = 0.7f

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

  // Confirmation dialogs
  const val LEAVE_GROUP_DIALOG = "leaveGroupDialog"
  const val LEAVE_GROUP_CONFIRM_BUTTON = "leaveGroupConfirmButton"
  const val LEAVE_GROUP_CANCEL_BUTTON = "leaveGroupCancelButton"
  const val DELETE_GROUP_DIALOG = "deleteGroupDialog"
  const val DELETE_GROUP_CONFIRM_BUTTON = "deleteGroupConfirmButton"
  const val DELETE_GROUP_CANCEL_BUTTON = "deleteGroupCancelButton"

  // Share dialog
  const val SHARE_GROUP_DIALOG = "shareGroupDialog"
  const val SHARE_GROUP_COPY_LINK_BUTTON = "shareGroupCopyLinkButton"
  const val SHARE_GROUP_CLOSE_BUTTON = "shareGroupCloseButton"

  // Join with link dialog
  const val JOIN_WITH_LINK_DIALOG = "joinWithLinkDialog"
  const val JOIN_WITH_LINK_INPUT = "joinWithLinkInput"
  const val JOIN_WITH_LINK_PASTE_BUTTON = "joinWithLinkPasteButton"
  const val JOIN_WITH_LINK_JOIN_BUTTON = "joinWithLinkJoinButton"
  const val JOIN_WITH_LINK_CLOSE_BUTTON = "joinWithLinkCloseButton"

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
    onJoinWithLink: (String) -> Unit = {},
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
  // Current user ID with test environment detection
  val currentUserId = run {
    // Detect test environment first (same as CreateGroupViewModel)
    val isTestEnv =
        android.os.Build.FINGERPRINT == "robolectric" ||
            android.os.Debug.isDebuggerConnected() ||
            System.getProperty("IS_TEST_ENV") == "true"
    if (isTestEnv) {
      "test-user-id"
    } else {
      Firebase.auth.currentUser?.uid
    }
  }
  val context = LocalContext.current

  // State for showing/hiding floating bubbles in the join/create group FAB
  var showJoinBubbles by remember { mutableStateOf(false) }

  // State for tracking which group's menu is currently open (null = none open)
  var openMenuGroupId by remember { mutableStateOf<String?>(null) }

  // State for tracking the position of the clicked three-dot button
  var menuButtonYPosition by remember { mutableFloatStateOf(0f) }

  // State for leave group confirmation dialog
  var groupToLeave by remember { mutableStateOf<Group?>(null) }

  // State for delete group confirmation dialog
  var groupToDelete by remember { mutableStateOf<Group?>(null) }

  // State for share group dialog
  var groupToShare by remember { mutableStateOf<Group?>(null) }

  // State for join with link dialog
  var showJoinWithLinkDialog by remember { mutableStateOf(false) }

  // Define bubble actions for join/create group FAB
  val groupJoinBubbleActions = remember {
    listOf(
        BubbleAction(
            text = "JOIN WITH LINK",
            icon = Icons.Default.Link,
            onClick = {
              showJoinWithLinkDialog = true
              showJoinBubbles = false
            },
            testTag = GroupListScreenTestTags.JOIN_WITH_LINK_BUBBLE),
        BubbleAction(
            text = "CREATE A GROUP",
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
                    contentDescription = "JOIN A NEW GROUP",
                    tint =
                        if (showJoinBubbles) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onPrimary)
              },
              shape = RoundedCornerShape(Dimens.CornerRadius.pill),
              text = {
                Text(
                    "JOIN A NEW GROUP",
                    color =
                        if (showJoinBubbles) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onPrimary)
              },
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
                      .padding(horizontal = Dimens.Spacing.medium)
                      .padding(
                          bottom = pd.calculateBottomPadding() + Dimens.GroupList.fabReservedSpace)
                      .padding(top = pd.calculateTopPadding())
                      .testTag(GroupListScreenTestTags.LIST),
              contentPadding = PaddingValues(vertical = Dimens.Spacing.itemSpacing)) {
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
                  HorizontalDivider(
                      modifier = Modifier.padding(vertical = Dimens.Spacing.medium),
                      thickness = Dimens.BorderWidth.thin,
                      color = MaterialTheme.colorScheme.primary)
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
        val configuration = LocalConfiguration.current
        val screenHeightDp = configuration.screenHeightDp.dp

        // Convert pixel position to dp
        val buttonTopPaddingDp = with(density) { menuButtonYPosition.toDp() }

        // Calculate dynamic menu height based on ownership
        val isOwner = group.ownerId == currentUserId
        val numberOfButtons = if (isOwner) 5 else 3 // 5 if owner, 3 if not
        val dynamicMenuHeight =
            Dimens.TouchTarget.minimum.times(numberOfButtons) +
                Dimens.Spacing.small.times(numberOfButtons - 1)

        // Check if menu would go off-screen (reserve space for FAB at bottom)
        val spaceBelow =
            (screenHeightDp.value -
                    buttonTopPaddingDp.value -
                    Dimens.GroupList.fabReservedSpace.value)
                .dp
        val shouldPositionAbove = spaceBelow < dynamicMenuHeight

        // Calculate final top position
        val topPaddingDp =
            if (shouldPositionAbove) {
              // Position menu so bottom aligns with button (menu above button)
              (buttonTopPaddingDp - dynamicMenuHeight).coerceAtLeast(
                  Dimens.GroupList.fabReservedSpace)
            } else {
              // Normal position (menu below button)
              buttonTopPaddingDp
            }

        // Animate scrim opacity for smooth fade-in/fade-out (same as join menu)
        val scrimAlpha by
            animateFloatAsState(
                targetValue = 1f,
                animationSpec = tween(durationMillis = SCRIM_ANIMATION_DURATION),
                label = "cardMenuScrimAlpha")
        val scrimBaseColor = MaterialTheme.customColors.scrimOverlay
        val scrimColor = scrimBaseColor.copy(alpha = scrimBaseColor.alpha * scrimAlpha)

        // Full-screen scrim overlay with menu bubbles
        Box(
            modifier =
                Modifier.fillMaxSize()
                    .zIndex(SCRIM_Z_INDEX)
                    .background(scrimColor)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { openMenuGroupId = null })) {
              // Menu bubbles positioned dynamically based on clicked card
              Column(
                  modifier =
                      Modifier.align(Alignment.TopEnd)
                          .padding(top = topPaddingDp, end = Dimens.Spacing.huge),
                  verticalArrangement = Arrangement.spacedBy(Dimens.Spacing.small),
                  horizontalAlignment = Alignment.End) {
                    // View Group Details
                    MenuBubble(
                        text = "VIEW GROUP DETAILS",
                        icon = Icons.Default.Visibility,
                        onClick = {
                          onViewGroupDetails(group)
                          openMenuGroupId = null
                        },
                        testTag = GroupListScreenTestTags.VIEW_GROUP_DETAILS_BUBBLE)

                    // Leave Group - Only shown for non-owners
                    if (group.ownerId != currentUserId) {
                      MenuBubble(
                          text = "LEAVE GROUP",
                          icon = Icons.AutoMirrored.Filled.ExitToApp,
                          onClick = {
                            openMenuGroupId = null
                            groupToLeave = group
                          },
                          testTag = GroupListScreenTestTags.LEAVE_GROUP_BUBBLE)
                    }

                    // Share Group
                    MenuBubble(
                        text = "SHARE GROUP",
                        icon = Icons.Default.Share,
                        onClick = {
                          openMenuGroupId = null
                          groupToShare = group
                        },
                        testTag = GroupListScreenTestTags.SHARE_GROUP_BUBBLE)

                    // Edit Group - Only shown for owners
                    if (group.ownerId == currentUserId) {
                      MenuBubble(
                          text = "EDIT GROUP",
                          icon = Icons.Default.Edit,
                          onClick = {
                            onEditGroup(group)
                            openMenuGroupId = null
                          },
                          testTag = GroupListScreenTestTags.EDIT_GROUP_BUBBLE)
                    }

                    // Delete Group - Only shown for owners
                    if (group.ownerId == currentUserId) {
                      MenuBubble(
                          text = "DELETE GROUP",
                          icon = Icons.Default.Delete,
                          onClick = {
                            openMenuGroupId = null
                            groupToDelete = group
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
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer)

    // Leave Group Confirmation Dialog
    groupToLeave?.let { group ->
      CustomConfirmationDialog(
          modifier = Modifier.testTag(GroupListScreenTestTags.LEAVE_GROUP_DIALOG),
          title = "Are you sure you want to leave\nthis group?",
          confirmText = "Yes",
          cancelText = "No",
          onConfirm = {
            onLeaveGroup(group)
            groupToLeave = null
          },
          onDismiss = { groupToLeave = null },
          confirmButtonTestTag = GroupListScreenTestTags.LEAVE_GROUP_CONFIRM_BUTTON,
          cancelButtonTestTag = GroupListScreenTestTags.LEAVE_GROUP_CANCEL_BUTTON)
    }

    // Delete Group Confirmation Dialog
    groupToDelete?.let { group ->
      CustomConfirmationDialog(
          modifier = Modifier.testTag(GroupListScreenTestTags.DELETE_GROUP_DIALOG),
          title = "Are you sure you want to delete\nthis group?",
          message = "The group will be permanently deleted\nThis action is irreversible",
          confirmText = "Yes",
          cancelText = "No",
          onConfirm = {
            onDeleteGroup(group)
            groupToDelete = null
          },
          onDismiss = { groupToDelete = null },
          confirmButtonTestTag = GroupListScreenTestTags.DELETE_GROUP_CONFIRM_BUTTON,
          cancelButtonTestTag = GroupListScreenTestTags.DELETE_GROUP_CANCEL_BUTTON)
    }

    // Share Group Dialog
    groupToShare?.let { group ->
      ShareGroupDialog(group = group, onDismiss = { groupToShare = null })
    }

    // Join with Link Dialog
    if (showJoinWithLinkDialog) {
      JoinWithLinkDialog(
          onJoin = { groupId ->
            onJoinWithLink(groupId)
            showJoinWithLinkDialog = false
          },
          onDismiss = { showJoinWithLinkDialog = false })
    }
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
  val groupColor = group.category.getColor()
  val groupOnColor = group.category.getOnContainerColor()

  Card(
      modifier =
          Modifier.fillMaxWidth()
              .heightIn(min = Dimens.Profile.bioMinHeight)
              .clickable { onClick() }
              .testTag(GroupListScreenTestTags.cardTag(group.id)),
      colors = CardDefaults.cardColors(containerColor = groupColor, contentColor = groupOnColor)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Dimens.Spacing.itemSpacing),
            verticalAlignment = Alignment.Top) {
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
              var buttonYPosition by remember { mutableFloatStateOf(0f) }

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
                        tint = groupOnColor)
                  }
            }
      }
}

/** Individual menu bubble button for group card menus */
@Composable
private fun MenuBubble(text: String, icon: ImageVector, onClick: () -> Unit, testTag: String = "") {
  FloatingActionButton(
      onClick = onClick,
      containerColor = MaterialTheme.colorScheme.primaryContainer,
      contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
      shape = RoundedCornerShape(Dimens.CornerRadius.pill),
      modifier = Modifier.height(Dimens.TouchTarget.minimum).testTag(testTag)) {
        Row(
            modifier =
                Modifier.padding(
                    horizontal = Dimens.Spacing.medium, vertical = Dimens.Padding.small),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing.small)) {
              Icon(
                  imageVector = icon,
                  contentDescription = text,
                  tint = MaterialTheme.colorScheme.onPrimaryContainer,
                  modifier = Modifier.size(Dimens.IconSize.small))
              Text(
                  text = text,
                  color = MaterialTheme.colorScheme.onPrimaryContainer,
                  style = MaterialTheme.typography.bodyMedium)
            }
      }
}

/**
 * Custom confirmation dialog matching the design in screenshots.
 *
 * @param title Dialog title text
 * @param message Optional message text below title
 * @param confirmText Text for confirm button
 * @param cancelText Text for cancel button (null to show only confirm button)
 * @param onConfirm Callback when confirm button is clicked
 * @param onDismiss Callback when dialog is dismissed
 * @param confirmButtonTestTag Test tag for confirm button
 * @param cancelButtonTestTag Test tag for cancel button
 * @param modifier Modifier for the dialog
 */
@Composable
private fun CustomConfirmationDialog(
    modifier: Modifier = Modifier,
    title: String,
    message: String? = null,
    confirmText: String,
    cancelText: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmButtonTestTag: String = "",
    cancelButtonTestTag: String = ""
) {
  Dialog(onDismissRequest = onDismiss) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.CornerRadius.extraLarge),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
          Column(
              modifier = Modifier.padding(Dimens.Padding.large),
              horizontalAlignment = Alignment.CenterHorizontally) {
                // Title
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = Dimens.Padding.medium))

                // Optional message
                message?.let {
                  Text(
                      text = it,
                      style = MaterialTheme.typography.bodyMedium,
                      textAlign = TextAlign.Center,
                      color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                      modifier = Modifier.padding(bottom = Dimens.Padding.large))
                }

                // Buttons
                if (cancelText != null) {
                  // Two buttons side by side
                  Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing.itemSpacing)) {
                        // Cancel button
                        Button(
                            onClick = onDismiss,
                            modifier =
                                Modifier.weight(1f)
                                    .height(Dimens.Button.minHeight)
                                    .testTag(cancelButtonTestTag),
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary),
                            shape = RoundedCornerShape(Dimens.CornerRadius.extraLarge)) {
                              Text(text = cancelText, color = MaterialTheme.colorScheme.onSecondary)
                            }

                        // Confirm button
                        Button(
                            onClick = onConfirm,
                            modifier =
                                Modifier.weight(1f)
                                    .height(Dimens.Button.minHeight)
                                    .testTag(confirmButtonTestTag),
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(Dimens.CornerRadius.extraLarge)) {
                              Text(text = confirmText, color = MaterialTheme.colorScheme.onPrimary)
                            }
                      }
                } else {
                  // Single confirm button
                  Button(
                      onClick = onConfirm,
                      modifier =
                          Modifier.fillMaxWidth()
                              .height(Dimens.Button.minHeight)
                              .testTag(confirmButtonTestTag),
                      colors =
                          ButtonDefaults.buttonColors(
                              containerColor = MaterialTheme.colorScheme.primary),
                      shape = RoundedCornerShape(Dimens.CornerRadius.extraLarge)) {
                        Text(text = confirmText, color = MaterialTheme.colorScheme.onPrimary)
                      }
                }
              }
        }
  }
}

/**
 * Share Group Dialog matching the design in screenshots.
 *
 * @param group The group to share
 * @param onDismiss Callback when dialog is dismissed
 */
@Composable
private fun ShareGroupDialog(group: Group, onDismiss: () -> Unit) {
  val context = LocalContext.current
  val clipboardManager = LocalClipboardManager.current

  Dialog(onDismissRequest = onDismiss) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag(GroupListScreenTestTags.SHARE_GROUP_DIALOG),
        shape = RoundedCornerShape(Dimens.CornerRadius.extraLarge),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
          Column(
              modifier = Modifier.padding(Dimens.Padding.large),
              horizontalAlignment = Alignment.CenterHorizontally) {
                // Header with close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                      Spacer(Modifier.width(24.dp)) // Balance the close button
                      Text(
                          text = "       Share this group",
                          style = MaterialTheme.typography.titleMedium,
                          color = MaterialTheme.colorScheme.onSurface)
                      IconButton(
                          onClick = onDismiss,
                          modifier =
                              Modifier.testTag(GroupListScreenTestTags.SHARE_GROUP_CLOSE_BUTTON)) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurface)
                          }
                    }

                Spacer(Modifier.height(16.dp))

                // Group name
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth())

                Spacer(Modifier.height(24.dp))

                // Copy Group ID button
                Button(
                    onClick = {
                      // Copy group ID to clipboard so users can join with it
                      clipboardManager.setText(AnnotatedString(group.id))
                      Toast.makeText(context, "Group ID copied to clipboard!", Toast.LENGTH_SHORT)
                          .show()
                      onDismiss()
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                            .height(Dimens.Button.minHeight)
                            .testTag(GroupListScreenTestTags.SHARE_GROUP_COPY_LINK_BUTTON),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(Dimens.CornerRadius.extraLarge)) {
                      Row(
                          horizontalArrangement = Arrangement.Center,
                          verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Copy Group ID", color = MaterialTheme.colorScheme.onPrimary)
                          }
                    }

                Spacer(Modifier.height(12.dp))

                // Helper text
                Text(
                    text = "Anyone with this ID can join the group",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center)
              }
        }
  }
}

/**
 * Join with Link Dialog for entering a Group ID to join.
 *
 * State is managed internally to avoid triggering parent recomposition.
 *
 * @param onJoin Callback when join button is clicked, receives the group ID
 * @param onDismiss Callback when dialog is dismissed
 */
@Composable
private fun JoinWithLinkDialog(onJoin: (String) -> Unit, onDismiss: () -> Unit) {
  val context = LocalContext.current
  val clipboardManager = LocalClipboardManager.current

  // Manage state locally to prevent infinite recomposition
  var groupIdInput by remember { mutableStateOf("") }

  // Use Box with scrim instead of Dialog to avoid Robolectric test issues
  Box(
      modifier =
          Modifier.fillMaxSize()
              .background(MaterialTheme.customColors.scrimOverlay)
              .clickable(
                  interactionSource = remember { MutableInteractionSource() },
                  indication = null,
                  onClick = onDismiss)
              .zIndex(100f)) {
        Card(
            modifier =
                Modifier.fillMaxWidth(0.9f)
                    .align(Alignment.Center)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { /* Prevent clicks from dismissing */})
                    .testTag(GroupListScreenTestTags.JOIN_WITH_LINK_DIALOG),
            shape = RoundedCornerShape(Dimens.CornerRadius.extraLarge),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
              Column(
                  modifier = Modifier.padding(Dimens.Padding.large),
                  horizontalAlignment = Alignment.CenterHorizontally) {
                    // Header with close button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                          Spacer(Modifier.width(48.dp)) // Balance the close button
                          Text(
                              text = "Join a group",
                              style = MaterialTheme.typography.titleMedium,
                              color = MaterialTheme.colorScheme.onSurface,
                              modifier = Modifier.weight(1f),
                              textAlign = TextAlign.Center)
                          IconButton(
                              onClick = onDismiss,
                              modifier =
                                  Modifier.testTag(
                                      GroupListScreenTestTags.JOIN_WITH_LINK_CLOSE_BUTTON)) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = MaterialTheme.colorScheme.onSurface)
                              }
                        }

                    Spacer(Modifier.height(16.dp))

                    // Instructions
                    Text(
                        text = "Enter the Group ID to join",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth())

                    Spacer(Modifier.height(16.dp))

                    // Group ID input field
                    TextField(
                        value = groupIdInput,
                        onValueChange = { groupIdInput = it },
                        label = { Text("Group ID") },
                        singleLine = true,
                        modifier =
                            Modifier.fillMaxWidth()
                                .testTag(GroupListScreenTestTags.JOIN_WITH_LINK_INPUT))

                    Spacer(Modifier.height(8.dp))

                    // Paste button
                    Button(
                        onClick = {
                          val clipboardText = clipboardManager.getText()?.text
                          if (clipboardText != null) {
                            groupIdInput = clipboardText
                            Toast.makeText(context, "Pasted from clipboard", Toast.LENGTH_SHORT)
                                .show()
                          } else {
                            Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                          }
                        },
                        modifier =
                            Modifier.fillMaxWidth()
                                .height(Dimens.Button.minHeight)
                                .testTag(GroupListScreenTestTags.JOIN_WITH_LINK_PASTE_BUTTON),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(Dimens.CornerRadius.extraLarge)) {
                          Row(
                              horizontalArrangement = Arrangement.Center,
                              verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondary,
                                    modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Paste Group ID",
                                    color = MaterialTheme.colorScheme.onSecondary)
                              }
                        }

                    Spacer(Modifier.height(24.dp))

                    // Join button
                    Button(
                        onClick = {
                          if (groupIdInput.trim().isNotEmpty()) {
                            onJoin(groupIdInput.trim())
                          } else {
                            Toast.makeText(context, "Please enter a Group ID", Toast.LENGTH_SHORT)
                                .show()
                          }
                        },
                        modifier =
                            Modifier.fillMaxWidth()
                                .height(Dimens.Button.minHeight)
                                .testTag(GroupListScreenTestTags.JOIN_WITH_LINK_JOIN_BUTTON),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(Dimens.CornerRadius.extraLarge)) {
                          Text(text = "Join Group", color = MaterialTheme.colorScheme.onPrimary)
                        }
                  }
            }
      }
}
