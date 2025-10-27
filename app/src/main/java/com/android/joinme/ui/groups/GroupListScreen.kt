package com.android.joinme.ui.groups

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
 * @param onMoreOptionMenu Callback invoked when the user taps the more options button for a group.
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
    onMoreOptionMenu: (Group) -> Unit = {},
    onBackClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onEditClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val groups = uiState.groups
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
                testTag = GroupListScreenTestTags.JOIN_WITH_LINK_BUBBLE
            ),
            BubbleAction(
                text = "Create a group",
                icon = Icons.Default.Add,
                onClick = onCreateGroup,
                testTag = GroupListScreenTestTags.CREATE_GROUP_BUBBLE
            )
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                ProfileTopBar(
                    currentScreen = ProfileScreen.GROUPS,
                    onBackClick = onBackClick,
                    onProfileClick = onProfileClick,
                    onGroupClick = {},
                    onEditClick = onEditClick
                )
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
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    },
                    shape = RoundedCornerShape(24.dp),
                    text = {
                        Text(
                            "Join a new group",
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    },
                    containerColor =
                        if (showJoinBubbles) MaterialTheme.colorScheme.surfaceVariant
                        else MaterialTheme.colorScheme.primary
                )
            },
            floatingActionButtonPosition = FabPosition.Center,
        ) { pd ->
            Box(modifier = Modifier.fillMaxSize()) {
                // Main content
                if (groups.isNotEmpty()) {
                    LazyColumn(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp)
                                .padding(bottom = pd.calculateBottomPadding() + 84.dp)
                                .padding(top = pd.calculateTopPadding())
                                .testTag(GroupListScreenTestTags.LIST),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        items(groups) { group ->
                            GroupCard(
                                group = group,
                                onClick = { onGroup(group) },
                                onMoreOptions = { yPosition ->
                                    // Close the join/create group menu if it's open
                                    showJoinBubbles = false
                                    // Toggle the card menu
                                    openMenuGroupId =
                                        if (openMenuGroupId == group.id) null else group.id
                                    menuButtonYPosition = yPosition
                                })
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(pd)
                            .testTag(GroupListScreenTestTags.EMPTY),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "You are currently not",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "assigned to a group…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
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
                // Convert pixel position to dp
                val topPaddingDp = with(density) { menuButtonYPosition.toDp() }

                // Animate scrim opacity for smooth fade-in/fade-out (same as join menu)
                val scrimAlpha by animateFloatAsState(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 300),
                    label = "cardMenuScrimAlpha"
                )
                val isDarkTheme = isSystemInDarkTheme()
                val scrimBaseColor =
                    if (isDarkTheme) ScrimOverlayColorDarkTheme else ScrimOverlayColorLightTheme
                val scrimColor = scrimBaseColor.copy(alpha = scrimBaseColor.alpha * scrimAlpha)

                // Full-screen scrim overlay with menu bubbles
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(1000f)  // Very high z-index to be above FAB
                        .background(scrimColor)  // Use animated scrim color
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { openMenuGroupId = null }
                        )
                ) {
                    // Menu bubbles positioned dynamically based on clicked card
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(
                                top = topPaddingDp,
                                end = 60.dp
                            ),  // More space from right edge
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        // View Group Details
                        MenuBubble(
                            text = "View Group Details",
                            icon = Icons.Default.Visibility,
                            onClick = {
                                onGroup(group)
                                openMenuGroupId = null
                            }
                        )

                        // Leave Group
                        MenuBubble(
                            text = "Leave Group",
                            icon = Icons.AutoMirrored.Filled.ExitToApp,
                            onClick = {
                                Toast.makeText(
                                    context,
                                    "Leave group: ${group.name}",
                                    Toast.LENGTH_SHORT
                                ).show()
                                openMenuGroupId = null
                            }
                        )

                        // Share Group
                        MenuBubble(
                            text = "Share Group",
                            icon = Icons.Default.Share,
                            onClick = {
                                Toast.makeText(
                                    context,
                                    "Share group: ${group.name}",
                                    Toast.LENGTH_SHORT
                                ).show()
                                openMenuGroupId = null
                            }
                        )

                        // Edit Group
                        MenuBubble(
                            text = "Edit Group",
                            icon = Icons.Default.Edit,
                            onClick = {
                                Toast.makeText(
                                    context,
                                    "Edit group: ${group.name}",
                                    Toast.LENGTH_SHORT
                                ).show()
                                openMenuGroupId = null
                            }
                        )

                        // Delete Group
                        MenuBubble(
                            text = "Delete Group",
                            icon = Icons.Default.Delete,
                            onClick = {
                                Toast.makeText(
                                    context,
                                    "Delete group: ${group.name}",
                                    Toast.LENGTH_SHORT
                                ).show()
                                openMenuGroupId = null
                            }
                        )
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
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    }  // Close outer Box
}

/**
 * Displays a single group as a card with its information and action buttons.
 *
 * The card shows the group's name, description (if present), and member count. It includes a
 * clickable surface to view the group details and a more options button for additional actions.
 *
 * @param group The [Group] object containing the group's data to display.
 * @param onClick Callback invoked when the user taps anywhere on the card.
 * @param onMoreOptions Callback invoked when the user taps the more options button, receives Y position.
 */
@Composable
private fun GroupCard(group: Group, onClick: () -> Unit, onMoreOptions: (Float) -> Unit) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 86.dp)
                .clickable { onClick() }
                .testTag(GroupListScreenTestTags.cardTag(group.id)),
        colors =
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Row(modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp), verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.height(4.dp))
                if (group.description.isNotBlank()) {
                    Text(
                        text = group.description,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "members: ${group.memberIds.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
            var buttonYPosition by remember { mutableStateOf(0f) }

            IconButton(
                onClick = { onMoreOptions(buttonYPosition) },
                modifier = Modifier
                    .testTag(GroupListScreenTestTags.moreTag(group.id))
                    .onGloballyPositioned { coordinates ->
                        buttonYPosition = coordinates.positionInRoot().y
                    }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More options",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

/**
 * Individual menu bubble button for group card menus
 */
@Composable
private fun MenuBubble(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.height(48.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = text,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}