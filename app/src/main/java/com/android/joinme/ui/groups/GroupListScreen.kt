package com.android.joinme.ui.groups

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.runtime.*
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.joinme.R
import com.android.joinme.model.event.getColor
import com.android.joinme.model.event.getOnContainerColor
import com.android.joinme.model.groups.Group
import com.android.joinme.model.invitation.InvitationType
import com.android.joinme.ui.components.shareInvitation
import com.android.joinme.ui.profile.ProfileScreen
import com.android.joinme.ui.profile.ProfileTopBar
import com.android.joinme.ui.theme.Dimens
import com.android.joinme.ui.theme.customColors
import com.android.joinme.util.TestEnvironmentDetector
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

/** Dimensions and styling constants for GroupListScreen and its components */
// Constants for GroupListScreen
private const val SCRIM_ANIMATION_DURATION = 300 // milliseconds
private const val SCRIM_Z_INDEX = 1000f
private const val DESCRIPTION_ALPHA = 0.8f
private const val MEMBERS_ALPHA = 0.7f

/**
 * Gets the current user ID, with special handling for test environments.
 *
 * @return The current user's UID, or "test-user-id" in test environments
 */
private fun getCurrentUserId(): String? {
  return Firebase.auth.currentUser?.uid
      ?: if (TestEnvironmentDetector.shouldUseTestUserId()) TestEnvironmentDetector.getTestUserId()
      else null
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

  /** Test tag for the "Create a group" bubble action. */
  const val CREATE_GROUP_BUBBLE = "groupCreateBubble"
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

/** State holder for GroupListScreen to manage UI state and reduce complexity. */
@Stable
private class GroupListScreenState {
  var openMenuGroupId by mutableStateOf<String?>(null)
  var menuButtonYPosition by mutableFloatStateOf(0f)
  var groupToLeave by mutableStateOf<Group?>(null)
  var groupToDelete by mutableStateOf<Group?>(null)

  fun toggleGroupMenu(groupId: String, yPosition: Float) {
    openMenuGroupId = if (openMenuGroupId == groupId) null else groupId
    menuButtonYPosition = yPosition
  }
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
 * @param onCreateGroup Callback invoked when user taps "Create a group" option
 * @param onGroup Callback invoked when the user taps on a group card, receiving the selected
 *   [Group].
 * @param onLeaveGroup Callback invoked when the user taps "Leave Group" for a group.
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
    onCreateGroup: () -> Unit = {},
    onGroup: (Group) -> Unit = {},
    onLeaveGroup: (Group) -> Unit = {},
    onEditGroup: (Group) -> Unit = {},
    onDeleteGroup: (Group) -> Unit = {},
    onBackClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onEditClick: () -> Unit = {}
) {
  val uiState by viewModel.uiState.collectAsState()
  val groups = uiState.groups
  val currentUserId = getCurrentUserId()
  val screenState = remember { GroupListScreenState() }

  // Refresh groups when screen becomes visible (e.g., after joining via deep link)
  val lifecycleOwner = LocalLifecycleOwner.current
  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        viewModel.refreshUIState()
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
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
        floatingActionButton = { GroupListFab(onClick = onCreateGroup) },
        floatingActionButtonPosition = FabPosition.Center,
    ) { pd ->
      GroupListContent(
          groups = groups,
          paddingValues = pd,
          onGroup = onGroup,
          onMoreOptions = { group, yPosition -> screenState.toggleGroupMenu(group.id, yPosition) })
    }

    // Card menu overlay - OUTSIDE Scaffold, covers everything including FAB
    screenState.openMenuGroupId?.let { groupId ->
      groups
          .find { it.id == groupId }
          ?.let { group ->
            GroupCardMenuOverlay(
                group = group,
                currentUserId = currentUserId,
                menuButtonYPosition = screenState.menuButtonYPosition,
                onDismiss = { screenState.openMenuGroupId = null },
                onLeaveGroup = { screenState.groupToLeave = it },
                onEditGroup = onEditGroup,
                onDeleteGroup = { screenState.groupToDelete = it })
          }
    }

    // Dialogs
    GroupListDialogs(
        screenState = screenState, onLeaveGroup = onLeaveGroup, onDeleteGroup = onDeleteGroup)
  }
}

/** Floating Action Button for creating a new group. */
@Composable
private fun GroupListFab(onClick: () -> Unit) {
  ExtendedFloatingActionButton(
      modifier = Modifier.testTag(GroupListScreenTestTags.ADD_NEW_GROUP),
      onClick = onClick,
      icon = {
        Icon(
            Icons.Default.Add,
            contentDescription = stringResource(R.string.create_group_button),
            tint = MaterialTheme.colorScheme.onPrimary)
      },
      shape = RoundedCornerShape(Dimens.CornerRadius.pill),
      text = {
        Text(
            stringResource(R.string.create_group_button),
            color = MaterialTheme.colorScheme.onPrimary)
      },
      containerColor = MaterialTheme.colorScheme.primary)
}

/** Main content area showing groups list or empty state. */
@Composable
private fun GroupListContent(
    groups: List<Group>,
    paddingValues: PaddingValues,
    onGroup: (Group) -> Unit,
    onMoreOptions: (Group, Float) -> Unit
) {
  Box(modifier = Modifier.fillMaxSize()) {
    if (groups.isNotEmpty()) {
      LazyColumn(
          modifier =
              Modifier.fillMaxSize()
                  .padding(horizontal = Dimens.Spacing.medium)
                  .padding(
                      bottom =
                          paddingValues.calculateBottomPadding() +
                              Dimens.GroupList.fabReservedSpace)
                  .padding(top = paddingValues.calculateTopPadding())
                  .testTag(GroupListScreenTestTags.LIST),
          contentPadding = PaddingValues(vertical = Dimens.Spacing.itemSpacing)) {
            items(groups) { group ->
              GroupCard(
                  group = group,
                  onClick = { onGroup(group) },
                  onMoreOptions = { yPosition -> onMoreOptions(group, yPosition) })
              HorizontalDivider(
                  modifier = Modifier.padding(vertical = Dimens.Spacing.medium),
                  thickness = Dimens.BorderWidth.thin,
                  color = MaterialTheme.colorScheme.primary)
            }
          }
    } else {
      Box(
          modifier =
              Modifier.fillMaxSize().padding(paddingValues).testTag(GroupListScreenTestTags.EMPTY),
          contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                  text = stringResource(R.string.currently_not),
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
              Text(
                  text = stringResource(R.string.assigned_to_group),
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            }
          }
    }
  }
}

/** All dialogs for the group list screen. */
@Composable
private fun GroupListDialogs(
    screenState: GroupListScreenState,
    onLeaveGroup: (Group) -> Unit,
    onDeleteGroup: (Group) -> Unit
) {
  screenState.groupToLeave?.let { group ->
    CustomConfirmationDialog(
        modifier = Modifier.testTag(GroupListScreenTestTags.LEAVE_GROUP_DIALOG),
        title = stringResource(R.string.message_group_leaving),
        confirmText = stringResource(R.string.confirmation_group_button),
        cancelText = stringResource(R.string.negation_group_button),
        onConfirm = {
          onLeaveGroup(group)
          screenState.groupToLeave = null
        },
        onDismiss = { screenState.groupToLeave = null },
        confirmButtonTestTag = GroupListScreenTestTags.LEAVE_GROUP_CONFIRM_BUTTON,
        cancelButtonTestTag = GroupListScreenTestTags.LEAVE_GROUP_CANCEL_BUTTON)
  }

  screenState.groupToDelete?.let { group ->
    CustomConfirmationDialog(
        modifier = Modifier.testTag(GroupListScreenTestTags.DELETE_GROUP_DIALOG),
        title = stringResource(R.string.message_group_deletion),
        message = stringResource(R.string.advertisment_group_deletion),
        confirmText = stringResource(R.string.confirmation_group_button),
        cancelText = stringResource(R.string.negation_group_button),
        onConfirm = {
          onDeleteGroup(group)
          screenState.groupToDelete = null
        },
        onDismiss = { screenState.groupToDelete = null },
        confirmButtonTestTag = GroupListScreenTestTags.DELETE_GROUP_CONFIRM_BUTTON,
        cancelButtonTestTag = GroupListScreenTestTags.DELETE_GROUP_CANCEL_BUTTON)
  }
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
                        contentDescription = stringResource(R.string.more_options),
                        tint = groupOnColor)
                  }
            }
      }
}

/**
 * Displays the contextual menu overlay for a group card.
 *
 * @param group The group to display menu for
 * @param currentUserId The current user's ID
 * @param menuButtonYPosition The Y position of the three-dot button
 * @param onDismiss Callback to dismiss the menu
 * @param onLeaveGroup Callback when leave group is selected
 * @param onEditGroup Callback when edit group is selected
 * @param onDeleteGroup Callback when delete group is selected
 */
@Composable
private fun GroupCardMenuOverlay(
    group: Group,
    currentUserId: String?,
    menuButtonYPosition: Float,
    onDismiss: () -> Unit,
    onLeaveGroup: (Group) -> Unit,
    onEditGroup: (Group) -> Unit,
    onDeleteGroup: (Group) -> Unit
) {
  val density = LocalDensity.current
  val configuration = LocalConfiguration.current
  val screenHeightDp = configuration.screenHeightDp.dp
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  val buttonTopPaddingDp = with(density) { menuButtonYPosition.toDp() }

  val isOwner = group.ownerId == currentUserId
  val numberOfButtons = if (isOwner) 4 else 2
  val dynamicMenuHeight =
      Dimens.TouchTarget.minimum.times(numberOfButtons) +
          Dimens.Spacing.small.times(numberOfButtons - 1)

  val spaceBelow =
      (screenHeightDp.value - buttonTopPaddingDp.value - Dimens.GroupList.fabReservedSpace.value).dp
  val shouldPositionAbove = spaceBelow < dynamicMenuHeight

  val topPaddingDp =
      if (shouldPositionAbove) {
        (buttonTopPaddingDp - dynamicMenuHeight).coerceAtLeast(Dimens.GroupList.fabReservedSpace)
      } else {
        buttonTopPaddingDp
      }

  val scrimAlpha by
      animateFloatAsState(
          targetValue = 1f,
          animationSpec = tween(durationMillis = SCRIM_ANIMATION_DURATION),
          label = "cardMenuScrimAlpha")
  val scrimBaseColor = MaterialTheme.customColors.scrimOverlay
  val scrimColor = scrimBaseColor.copy(alpha = scrimBaseColor.alpha * scrimAlpha)

  Box(
      modifier =
          Modifier.fillMaxSize()
              .zIndex(SCRIM_Z_INDEX)
              .background(scrimColor)
              .clickable(
                  interactionSource = remember { MutableInteractionSource() },
                  indication = null,
                  onClick = onDismiss)) {
        Column(
            modifier =
                Modifier.align(Alignment.TopEnd)
                    .padding(top = topPaddingDp, end = Dimens.Spacing.huge),
            verticalArrangement = Arrangement.spacedBy(Dimens.Spacing.small),
            horizontalAlignment = Alignment.End) {
              if (!isOwner) {
                MenuBubble(
                    text = stringResource(R.string.leave_group_button),
                    icon = Icons.AutoMirrored.Filled.ExitToApp,
                    onClick = {
                      onDismiss()
                      onLeaveGroup(group)
                    },
                    testTag = GroupListScreenTestTags.LEAVE_GROUP_BUBBLE)
              }

              MenuBubble(
                  text = stringResource(R.string.share_group_button),
                  icon = Icons.Default.Share,
                  onClick = {
                    scope.launch {
                      shareInvitation(
                          invitationType = InvitationType.GROUP,
                          targetId = group.id,
                          createdBy = currentUserId ?: "",
                          expiresInDays = 7,
                          context = context)
                      onDismiss()
                    }
                  },
                  testTag = GroupListScreenTestTags.SHARE_GROUP_BUBBLE)

              if (isOwner) {
                MenuBubble(
                    text = stringResource(R.string.edit_group_button),
                    icon = Icons.Default.Edit,
                    onClick = {
                      onEditGroup(group)
                      onDismiss()
                    },
                    testTag = GroupListScreenTestTags.EDIT_GROUP_BUBBLE)
              }

              if (isOwner) {
                MenuBubble(
                    text = stringResource(R.string.delete_group_button),
                    icon = Icons.Default.Delete,
                    onClick = {
                      onDismiss()
                      onDeleteGroup(group)
                    },
                    testTag = GroupListScreenTestTags.DELETE_GROUP_BUBBLE)
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
