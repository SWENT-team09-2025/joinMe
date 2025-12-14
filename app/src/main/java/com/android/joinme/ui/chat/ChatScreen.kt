package com.android.joinme.ui.chat

// Implemented with help of Claude AI

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.android.joinme.R
import com.android.joinme.model.chat.ChatListItem
import com.android.joinme.model.chat.Message
import com.android.joinme.model.chat.MessageType
import com.android.joinme.model.chat.Poll
import com.android.joinme.model.map.Location
import com.android.joinme.model.map.UserLocation
import com.android.joinme.model.profile.Profile
import com.android.joinme.ui.profile.ProfilePhotoImage
import com.android.joinme.ui.theme.Dimens
import com.android.joinme.ui.theme.buttonColors
import com.android.joinme.ui.theme.customColors
import com.android.joinme.ui.theme.getUserColor
import com.android.joinme.ui.theme.outlinedTextField
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

/**
 * Type of chat determining UI behavior.
 *
 * - INDIVIDUAL: For one-on-one chats, shows "online"/"offline" status
 * - GROUP: For group/event chats, shows "X users online" count
 */
enum class ChatType {
  INDIVIDUAL,
  GROUP
}

/** Shared constants for chat screens. */
private object ChatConstants {
  const val SENDER_NAME_ALPHA = 0.8f
  const val TIMESTAMP_ALPHA = 0.7f
  const val MESSAGE_INPUT_MAX_LINES = 4
  const val TIMESTAMP_FORMAT = "HH:mm"
}

/**
 * Formats a Unix timestamp into a readable time string.
 *
 * @param timestamp Unix timestamp in milliseconds
 * @return Formatted time string (e.g., "14:32")
 */
private fun formatTimestamp(timestamp: Long): String {
  val dateFormat = SimpleDateFormat(ChatConstants.TIMESTAMP_FORMAT, Locale.getDefault())
  return dateFormat.format(Date(timestamp))
}

/**
 * Disabled MapUiSettings for location previews.
 *
 * Disables all user interaction with the map preview to prevent interference with message
 * click/long-press gestures.
 */
private val disabledMapUiSettings =
    MapUiSettings(
        zoomControlsEnabled = false,
        scrollGesturesEnabled = false,
        zoomGesturesEnabled = false,
        tiltGesturesEnabled = false,
        rotationGesturesEnabled = false,
        mapToolbarEnabled = false)

/**
 * Test tags for UI testing of the Chat screen components.
 *
 * Provides consistent identifiers for testing individual UI elements including input fields,
 * buttons, message lists, and loading indicators.
 */
object ChatScreenTestTags {
  const val SCREEN = "chatScreen"
  const val TOP_BAR = "chatTopBar"
  const val LEAVE_BUTTON = "chatLeaveButton"
  const val TITLE = "chatTitle"
  const val MESSAGE_LIST = "messageList"
  const val MESSAGE_INPUT = "messageInput"
  const val SEND_BUTTON = "sendButton"
  const val MIC_BUTTON = "micButton"
  const val ATTACHMENT_BUTTON = "attachmentButton"
  const val ATTACHMENT_MENU = "attachmentMenu"
  const val ATTACHMENT_PHOTO = "attachmentPhoto"
  const val PHOTO_SOURCE_DIALOG = "photoSourceDialog"
  const val PHOTO_SOURCE_GALLERY = "photoSourceGallery"
  const val PHOTO_SOURCE_CAMERA = "photoSourceCamera"
  const val ATTACHMENT_LOCATION = "attachmentLocation"
  const val ATTACHMENT_POLL = "attachmentPoll"
  const val LOADING_INDICATOR = "chatLoadingIndicator"
  const val EMPTY_MESSAGE = "emptyMessageText"

  /**
   * Generates a unique test tag for a specific message item.
   *
   * @param messageId The ID of the message to generate a tag for
   * @return A string combining "message" with the message's unique ID
   */
  fun getTestTagForMessage(messageId: String): String = "message_$messageId"

  /**
   * Generates a unique test tag for a message bubble.
   *
   * @param messageId The ID of the message to generate a tag for
   * @return A string combining "messageBubble" with the message's unique ID
   */
  fun getTestTagForMessageBubble(messageId: String): String = "messageBubble_$messageId"

  const val CHAT_IMAGE_CONTAINER = "chatImageContainer"
  const val CHAT_IMAGE_LOADING = "chatImageLoading"
  const val CHAT_IMAGE_REMOTE = "chatImageRemote"
  const val CHAT_IMAGE_ERROR = "chatImageError"
  const val EDIT_MESSAGE_DIALOG = "editMessageDialog"
  const val EDIT_MESSAGE_INPUT = "editMessageInput"
  const val EDIT_MESSAGE_SAVE_BUTTON = "editMessageSaveButton"
  const val LOCATION_PREVIEW_DIALOG = "locationPreviewDialog"
  const val LOCATION_PREVIEW_MAP = "locationPreviewMap"
  const val LOCATION_PREVIEW_SEND_BUTTON = "locationPreviewSendButton"
  const val LOCATION_PREVIEW_CANCEL_BUTTON = "locationPreviewCancelButton"
  const val LOCATION_MESSAGE_PREVIEW = "locationMessagePreview"
}

/**
 * Main Chat screen displaying messages in a conversation.
 *
 * This screen shows a conversation between users with:
 * - Messages displayed in bubbles (user's messages on the right, others on the left)
 * - Real-time message updates
 * - Input field for sending new messages
 * - Loading state while fetching messages
 * - Error handling with snackbar notifications
 *
 * @param chatId The unique identifier of the conversation to display
 * @param chatTitle The title to display in the top app bar (e.g., event name, group name)
 * @param currentUserId The ID of the current user viewing the chat
 * @param currentUserName The display name of the current user
 * @param viewModel The ViewModel managing the chat state and operations
 * @param onLeaveClick Callback invoked when the back button is clicked
 * @param chatColor Optional color for the chat theme (top bar, message bubbles, send button).
 *   Defaults to chatDefault if not provided
 * @param onChatColor Optional color for text/icons on chat-colored elements. Defaults to
 *   onChatDefault if not provided. Must provide proper contrast with chatColor
 * @param totalParticipants Total number of participants in the event/group
 * @param presenceViewModel Optional presence view model for online tracking
 * @param onNavigateToMap Callback invoked when user clicks on a location message to view on map
 * @param chatType Type of chat (INDIVIDUAL or GROUP), determines UI behavior like online status
 *   display. Defaults to INDIVIDUAL.
 * @param pollViewModel Optional poll view model for group/event chats with poll support
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatId: String,
    chatTitle: String,
    currentUserId: String,
    currentUserName: String,
    viewModel: ChatViewModel,
    onLeaveClick: () -> Unit = {},
    chatColor: Color? = null,
    onChatColor: Color? = null,
    totalParticipants: Int = 1, // Total number of participants in the event/group
    presenceViewModel: PresenceViewModel? =
        null, // Optional presence view model for online tracking
    onNavigateToMap: (Location, String) -> Unit = { _, _ -> },
    chatType: ChatType = ChatType.INDIVIDUAL,
    pollViewModel: PollViewModel? = null
) {
  val uiState by viewModel.uiState.collectAsState()
  val snackbarHostState = remember { SnackbarHostState() }
  val coroutineScope = rememberCoroutineScope()

  // Collect presence state if available
  val presenceState = presenceViewModel?.presenceState?.collectAsState()

  // Use chatDefault colors if no specific colors provided
  val effectiveChatColor = chatColor ?: MaterialTheme.customColors.chatDefault
  val effectiveOnChatColor = onChatColor ?: MaterialTheme.customColors.onChatDefault

  // Initialize chat when screen loads
  LaunchedEffect(chatId, currentUserId) { viewModel.initializeChat(chatId, currentUserId) }

  // Initialize presence tracking when screen loads
  LaunchedEffect(chatId, currentUserId, presenceViewModel) {
    presenceViewModel?.initialize(chatId, currentUserId)
  }

  // Show error messages in snackbar
  LaunchedEffect(uiState.errorMsg) {
    uiState.errorMsg?.let { errorMsg ->
      coroutineScope.launch {
        snackbarHostState.showSnackbar(errorMsg)
        viewModel.clearErrorMsg()
      }
    }
  }

  Scaffold(
      modifier = Modifier.fillMaxSize().testTag(ChatScreenTestTags.SCREEN),
      topBar = {
        ChatTopBar(
            chatTitle = chatTitle,
            onLeaveClick = onLeaveClick, // Leave chat navigates back
            topBarColor = effectiveChatColor,
            onTopBarColor = effectiveOnChatColor,
            onlineUsersCount = presenceState?.value?.onlineUsersCount ?: 0,
            chatType = chatType)
      },
      snackbarHost = { SnackbarHost(snackbarHostState) },
      contentWindowInsets = WindowInsets.systemBars, // Only consume system bars, not IME
      containerColor = MaterialTheme.colorScheme.background) { paddingValues ->
        if (uiState.isLoading) {
          Box(
              modifier = Modifier.fillMaxSize().padding(paddingValues),
              contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    modifier = Modifier.testTag(ChatScreenTestTags.LOADING_INDICATOR),
                    color = effectiveChatColor)
              }
        } else {
          ChatContent(
              messages = uiState.messages,
              currentUserId = currentUserId,
              currentUserName = currentUserName,
              senderProfiles = uiState.senderProfiles,
              onSendMessage = { content -> viewModel.sendMessage(content, currentUserName) },
              paddingValues = paddingValues,
              chatColor = effectiveChatColor,
              onChatColor = effectiveOnChatColor,
              viewModel = viewModel,
              totalParticipants = totalParticipants,
              onNavigateToMap = onNavigateToMap,
              pollViewModel = pollViewModel)
        }
      }
}

/**
 * Custom top bar for the chat screen matching Figma design.
 *
 * @param chatTitle The title to display
 * @param onLeaveClick Callback for leave button (not yet implemented)
 * @param topBarColor Color for the top bar background
 * @param onTopBarColor Color for text/icons on the top bar (must provide proper contrast with
 *   topBarColor)
 * @param onlineUsersCount Number of users currently online in the chat
 * @param chatType Type of chat (INDIVIDUAL or GROUP), determines online status display format
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatTopBar(
    chatTitle: String,
    onLeaveClick: () -> Unit,
    topBarColor: Color,
    onTopBarColor: Color,
    onlineUsersCount: Int = 0,
    chatType: ChatType = ChatType.INDIVIDUAL
) {
  // Define colors for the online indicator dot
  val onlineIndicatorColor = Color(0xFF4CAF50) // Green
  val offlineIndicatorColor = Color(0xFFF44336) // Red

  Surface(
      modifier = Modifier.fillMaxWidth().testTag(ChatScreenTestTags.TOP_BAR),
      color = topBarColor,
      shadowElevation = Dimens.Elevation.small) {
        Row(
            modifier =
                Modifier.fillMaxWidth()
                    .padding(horizontal = Dimens.Padding.medium, vertical = Dimens.Padding.small),
            verticalAlignment = Alignment.CenterVertically) {
              // Title and online status column
              Column(modifier = Modifier.weight(1f)) {
                // Title
                Text(
                    text = chatTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = onTopBarColor,
                    modifier = Modifier.testTag(ChatScreenTestTags.TITLE))

                // Online users count with indicator dot (always displayed)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing.extraSmall),
                    modifier = Modifier.testTag("onlineUsersRow")) {
                      // Colored indicator dot
                      Box(
                          modifier =
                              Modifier.size(Dimens.Spacing.small)
                                  .background(
                                      color =
                                          if (onlineUsersCount > 0) onlineIndicatorColor
                                          else offlineIndicatorColor,
                                      shape = CircleShape)
                                  .testTag("onlineIndicatorDot"))

                      // Display text based on chat type
                      val statusText =
                          when (chatType) {
                            ChatType.INDIVIDUAL ->
                                if (onlineUsersCount > 0) stringResource(R.string.status_online)
                                else stringResource(R.string.status_offline)
                            ChatType.GROUP ->
                                when (onlineUsersCount) {
                                  0 -> stringResource(R.string.online_users_zero)
                                  1 -> stringResource(R.string.online_users_one)
                                  else -> stringResource(R.string.online_users_many, onlineUsersCount)
                                }
                          }
                      Text(
                          text = statusText,
                          style = MaterialTheme.typography.bodySmall,
                          color = onTopBarColor.copy(alpha = 0.8f),
                          modifier = Modifier.testTag("onlineUsersCount"))
                    }
              }
              // Leave button - background color matches top bar
              IconButton(
                  onClick = onLeaveClick,
                  modifier =
                      Modifier.size(Dimens.IconSize.medium)
                          .background(topBarColor.copy(alpha = 0.8f), CircleShape)
                          .testTag(ChatScreenTestTags.LEAVE_BUTTON)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = stringResource(R.string.leave_chat),
                        tint = onTopBarColor,
                        modifier = Modifier.size(Dimens.IconSize.small))
                  }
            }
      }
}

/**
 * Chat content displaying messages and input field.
 *
 * @param messages The list of messages to display
 * @param currentUserId The ID of the current user
 * @param currentUserName The display name of the current user
 * @param senderProfiles A map of sender IDs to their Profile objects containing photo URLs
 * @param onSendMessage Callback invoked when sending a new message
 * @param paddingValues Padding from the Scaffold
 * @param chatColor The chat color for message bubbles and send button
 * @param onChatColor The color for text on chat-colored elements (must provide proper contrast)
 * @param viewModel ChatViewModel for managing chat state
 * @param totalParticipants Total number of participants in the chat
 * @param onNavigateToMap Callback to navigate to map screen with a location
 * @param pollViewModel Optional poll view model for group/event chats with poll support
 */
@Composable
private fun ChatContent(
    messages: List<Message>,
    currentUserId: String,
    currentUserName: String,
    senderProfiles: Map<String, Profile>,
    onSendMessage: (String) -> Unit,
    paddingValues: PaddingValues,
    chatColor: Color,
    onChatColor: Color,
    viewModel: ChatViewModel,
    totalParticipants: Int,
    onNavigateToMap: (Location, String) -> Unit,
    pollViewModel: PollViewModel? = null
) {
  val uiState by viewModel.uiState.collectAsState()
  var messageText by remember { mutableStateOf("") }
  val listState = rememberLazyListState()
  var selectedMessage by remember { mutableStateOf<Message?>(null) }
  var showEditDialog by remember { mutableStateOf(false) }
  var showDeleteDialog by remember { mutableStateOf(false) }
  var showWhoReadDialog by remember { mutableStateOf(false) }
  var fullScreenImageUrl by remember { mutableStateOf<String?>(null) }

  // Auto-scroll to bottom when new messages arrive
  LaunchedEffect(messages.size) {
    if (messages.isNotEmpty()) {
      listState.animateScrollToItem(messages.size - 1)
    }
  }

  // Mark all messages as read when chat is opened or new messages arrive
  LaunchedEffect(messages.size) {
    if (messages.isNotEmpty()) {
      viewModel.markAllMessagesAsRead()
    }
  }

  Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
    Column(
        modifier =
            Modifier.fillMaxSize()
                .then(
                    if (selectedMessage != null) Modifier.blur(Dimens.Profile.photoBlurRadius * 2)
                    else Modifier)) {
          // Messages list
          MessageList(
              messages = messages,
              currentUserId = currentUserId,
              senderProfiles = senderProfiles,
              totalParticipants = totalParticipants,
              listState = listState,
              onMessageLongPress = { message ->
                // Only show context menu if there are items to display
                val isCurrentUser = message.senderId == currentUserId
                // Location and Image messages: only delete and see who read
                val hasMenuItems = message.type == MessageType.TEXT || isCurrentUser
                if (hasMenuItems) {
                  selectedMessage = message
                }
              },
              onImageClick = { imageUrl -> fullScreenImageUrl = imageUrl },
              onLocationClick = { location, senderId -> onNavigateToMap(location, senderId) },
              chatColor = chatColor,
              modifier = Modifier.weight(1f))

          // Message input
          MessageInput(
              text = messageText,
              onTextChange = { messageText = it },
              onSendClick = {
                if (messageText.isNotBlank()) {
                  onSendMessage(messageText)
                  messageText = ""
                }
              },
              sendButtonColor = chatColor,
              onSendButtonColor = onChatColor,
              viewModel = viewModel,
              currentUserName = currentUserName,
              isUploadingImage = uiState.isUploadingImage)
        }

    // Message interaction overlays
    MessageInteractionOverlays(
        selectedMessage = selectedMessage,
        currentUserId = currentUserId,
        senderProfiles = senderProfiles,
        dialogState =
            DialogState(
                showEditDialog = showEditDialog,
                showDeleteDialog = showDeleteDialog,
                showWhoReadDialog = showWhoReadDialog),
        callbacks =
            DialogCallbacks(
                onDismissContextMenu = { selectedMessage = null },
                onShowEditDialog = { showEditDialog = true },
                onShowDeleteDialog = { showDeleteDialog = true },
                onShowWhoReadDialog = { showWhoReadDialog = true },
                onDismissEditDialog = {
                  showEditDialog = false
                  selectedMessage = null
                },
                onDismissDeleteDialog = {
                  showDeleteDialog = false
                  selectedMessage = null
                },
                onDismissWhoReadDialog = {
                  showWhoReadDialog = false
                  selectedMessage = null
                }),
        viewModel = viewModel)

    // Full-screen image viewer
    fullScreenImageUrl?.let { imageUrl ->
      FullScreenImageViewer(imageUrl = imageUrl, onDismiss = { fullScreenImageUrl = null })
    }
  }
}

/**
 * Displays the list of messages in a LazyColumn.
 *
 * @param messages The list of messages to display
 * @param currentUserId The ID of the current user
 * @param senderProfiles A map of sender IDs to their Profile objects
 * @param totalParticipants Total number of participants in the chat
 * @param listState LazyListState for controlling scroll
 * @param onMessageLongPress Callback when a message is long-pressed
 * @param onImageClick Callback when an image message is clicked
 * @param onLocationClick Callback when a location message is clicked
 * @param chatColor The chat color for the date headers (matches topbar)
 * @param modifier Modifier for the LazyColumn
 */
@Composable
private fun MessageList(
    messages: List<Message>,
    currentUserId: String,
    senderProfiles: Map<String, Profile>,
    totalParticipants: Int,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onMessageLongPress: (Message) -> Unit,
    onImageClick: (String) -> Unit,
    onLocationClick: (Location, String) -> Unit,
    chatColor: Color,
    modifier: Modifier = Modifier
) {
  // Group messages by date and insert date headers
  val listItems = remember(messages) { groupMessagesByDate(messages) }

  LazyColumn(
      modifier = modifier.fillMaxWidth().testTag(ChatScreenTestTags.MESSAGE_LIST),
      state = listState,
      contentPadding = PaddingValues(Dimens.Padding.medium),
      verticalArrangement = Arrangement.spacedBy(Dimens.Spacing.itemSpacing)) {
        if (messages.isEmpty()) {
          item {
            Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
              Text(
                  text = stringResource(R.string.empty_chat_message),
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  textAlign = TextAlign.Center,
                  modifier = Modifier.testTag(ChatScreenTestTags.EMPTY_MESSAGE))
            }
          }
        } else {
          items(
              listItems,
              key = { item ->
                when (item) {
                  is ChatListItem.MessageItem -> item.message.id
                  is ChatListItem.DateHeader -> "date_${item.timestamp}"
                }
              }) { item ->
                when (item) {
                  is ChatListItem.DateHeader -> {
                    DateHeader(timestamp = item.timestamp, chatColor = chatColor)
                  }
                  is ChatListItem.MessageItem -> {
                    val message = item.message
                    // Get user-specific color for this message sender
                    val userColors = getUserColor(message.senderId)
                    MessageItem(
                        message = message,
                        isCurrentUser = message.senderId == currentUserId,
                        senderPhotoUrl = senderProfiles[message.senderId]?.photoUrl,
                        currentUserPhotoUrl = senderProfiles[currentUserId]?.photoUrl,
                        bubbleColor = userColors.first,
                        onBubbleColor = userColors.second,
                        totalUsersInChat = totalParticipants,
                        onLongPress = { onMessageLongPress(message) },
                        onImageClick = onImageClick,
                        onLocationClick = onLocationClick)
                  }
                }
              }
        }
      }
}

/**
 * State holder for dialog visibility states.
 *
 * @property showEditDialog Whether to show the edit dialog
 * @property showDeleteDialog Whether to show the delete dialog
 * @property showWhoReadDialog Whether to show the "who read" dialog
 */
private data class DialogState(
    val showEditDialog: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val showWhoReadDialog: Boolean = false
)

/**
 * Callbacks for dialog state changes.
 *
 * @property onDismissContextMenu Callback to dismiss the context menu
 * @property onShowEditDialog Callback to show the edit dialog
 * @property onShowDeleteDialog Callback to show the delete dialog
 * @property onShowWhoReadDialog Callback to show the "who read" dialog
 * @property onDismissEditDialog Callback to dismiss the edit dialog
 * @property onDismissDeleteDialog Callback to dismiss the delete dialog
 * @property onDismissWhoReadDialog Callback to dismiss the "who read" dialog
 */
private data class DialogCallbacks(
    val onDismissContextMenu: () -> Unit,
    val onShowEditDialog: () -> Unit,
    val onShowDeleteDialog: () -> Unit,
    val onShowWhoReadDialog: () -> Unit,
    val onDismissEditDialog: () -> Unit,
    val onDismissDeleteDialog: () -> Unit,
    val onDismissWhoReadDialog: () -> Unit
)

/**
 * Date header component displaying a clean divider with centered date text.
 *
 * Shows a subtle horizontal line with the date centered on top, separating messages from different
 * days. The date text uses the chat color to match the topbar.
 *
 * @param timestamp The timestamp representing the start of the day
 * @param chatColor The color to use for the date text (matches topbar)
 */
@Composable
private fun DateHeader(timestamp: Long, chatColor: Color) {
  val context = LocalContext.current
  val dateText = formatDateHeader(context, timestamp)

  Box(
      modifier = Modifier.fillMaxWidth().padding(vertical = Dimens.Spacing.medium),
      contentAlignment = Alignment.Center) {
        // Horizontal divider line
        androidx.compose.material3.HorizontalDivider(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.Padding.large),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            thickness = Dimens.BorderWidth.thin)

        // Date text centered on white background
        Surface(
            shape = RoundedCornerShape(Dimens.CornerRadius.small),
            color = MaterialTheme.colorScheme.background) {
              Text(
                  text = dateText,
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.SemiBold,
                  color = chatColor,
                  modifier = Modifier.padding(horizontal = Dimens.Padding.medium))
            }
      }
}

/**
 * Manages all message interaction overlays (context menu and dialogs).
 *
 * @param selectedMessage The currently selected message
 * @param currentUserId The ID of the current user
 * @param senderProfiles A map of sender IDs to their Profile objects
 * @param dialogState State of all dialogs
 * @param callbacks Callbacks for dialog interactions
 * @param viewModel ChatViewModel for edit/delete operations
 */
@Composable
private fun MessageInteractionOverlays(
    selectedMessage: Message?,
    currentUserId: String,
    senderProfiles: Map<String, Profile>,
    dialogState: DialogState,
    callbacks: DialogCallbacks,
    viewModel: ChatViewModel
) {
  val clipboardManager = LocalClipboardManager.current

  // Context menu overlay (shown when a message is selected)
  selectedMessage?.let { message ->
    val isCurrentUser = message.senderId == currentUserId
    val hasMenuItems = message.type == MessageType.TEXT || isCurrentUser

    // Only show context menu if there are items to display
    if (hasMenuItems) {
      MessageContextMenu(
          isCurrentUser = isCurrentUser,
          messageType = message.type,
          onDismiss = callbacks.onDismissContextMenu,
          onCopy = {
            clipboardManager.setText(AnnotatedString(message.content))
            callbacks.onDismissContextMenu()
          },
          onEdit = callbacks.onShowEditDialog,
          onDelete = callbacks.onShowDeleteDialog,
          onSeeWhoRead = callbacks.onShowWhoReadDialog)
    }
  }

  // Edit dialog
  if (dialogState.showEditDialog && selectedMessage != null) {
    EditMessageDialog(
        message = selectedMessage,
        onDismiss = callbacks.onDismissEditDialog,
        onConfirm = { newContent ->
          viewModel.editMessage(selectedMessage.id, newContent)
          callbacks.onDismissEditDialog()
        })
  }

  // Delete confirmation dialog
  if (dialogState.showDeleteDialog && selectedMessage != null) {
    DeleteMessageDialog(
        onDismiss = callbacks.onDismissDeleteDialog,
        onConfirm = {
          viewModel.deleteMessage(selectedMessage.id)
          callbacks.onDismissDeleteDialog()
        })
  }

  // Who read dialog
  if (dialogState.showWhoReadDialog && selectedMessage != null) {
    WhoReadDialog(
        message = selectedMessage,
        senderProfiles = senderProfiles,
        onDismiss = callbacks.onDismissWhoReadDialog)
  }
}

/**
 * Calculates the shape of a message bubble based on the sender.
 *
 * @param isCurrentUser Whether the message belongs to the current user
 * @return RoundedCornerShape with appropriate corner radii
 */
private fun getMessageBubbleShape(isCurrentUser: Boolean): RoundedCornerShape {
  return RoundedCornerShape(
      topStart = Dimens.CornerRadius.extraLarge,
      topEnd = Dimens.CornerRadius.extraLarge,
      bottomStart =
          if (isCurrentUser) Dimens.CornerRadius.extraLarge else Dimens.CornerRadius.small,
      bottomEnd = if (isCurrentUser) Dimens.CornerRadius.small else Dimens.CornerRadius.extraLarge)
}

/**
 * Renders the content of a message based on its type.
 *
 * @param message The message to display
 * @param isCurrentUser Whether the message was sent by the current user
 * @param onBubbleColor Color for text on the bubble
 * @param bubbleColor Color for the bubble background
 * @param onImageClick Callback when an image message is clicked
 * @param onLocationClick Callback when a location message is clicked, with the location
 */
@Composable
private fun MessageContent(
    message: Message,
    isCurrentUser: Boolean,
    onBubbleColor: Color,
    bubbleColor: Color,
    onImageClick: (String) -> Unit,
    onLocationClick: (Location, String) -> Unit = { _, _ -> },
) {
  if (message.type == MessageType.SYSTEM) {
    Text(
        text = message.content,
        style = MaterialTheme.typography.bodySmall,
        color = onBubbleColor.copy(alpha = 0.9f),
        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
  } else {
    if (!isCurrentUser) {
      Text(
          text = message.senderName,
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          color = onBubbleColor.copy(alpha = 0.8f))
      Spacer(modifier = Modifier.height(Dimens.Spacing.extraSmall))
    }

    // Message content
    when (message.type) {
      MessageType.IMAGE -> {
        // Display image message
        ChatImageMessage(
            imageUrl = message.content,
            bubbleColor = bubbleColor,
            onClick = { onImageClick(message.content) })
      }
      MessageType.LOCATION -> {
        // Display location message
        message.location?.let { location ->
          ChatLocationMessage(
              location = location, onClick = { onLocationClick(location, message.senderId) })
        }
      }
      else -> {
        // Display text message
        Text(
            text = message.content,
            style = MaterialTheme.typography.bodyMedium,
            color = onBubbleColor)
      }
    }
  }
}

/**
 * Renders message metadata including timestamp, edited indicator, and read receipts.
 *
 * @param message The message to display metadata for
 * @param isCurrentUser Whether the message was sent by the current user
 * @param onBubbleColor Color for text on the bubble
 * @param totalUsersInChat Total number of users in the chat
 */
@Composable
private fun MessageMetadata(
    message: Message,
    isCurrentUser: Boolean,
    onBubbleColor: Color,
    totalUsersInChat: Int
) {
  Spacer(modifier = Modifier.height(Dimens.Spacing.extraSmall))
  Row(
      horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing.extraSmall),
      verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = formatTimestamp(message.timestamp),
            style = MaterialTheme.typography.labelSmall,
            color = onBubbleColor)

        if (message.isEdited) {
          Text(
              text = stringResource(R.string.message_edited),
              style = MaterialTheme.typography.labelSmall,
              color = onBubbleColor,
              fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
        }

        val shouldShowReadReceipt = isCurrentUser && message.type != MessageType.SYSTEM
        if (shouldShowReadReceipt) {
          ReadReceiptIcon(message, onBubbleColor, totalUsersInChat)
        }
      }
}

/**
 * Displays a read receipt icon indicating whether a message has been read by all recipients.
 *
 * @param message The message to display read receipt for
 * @param onBubbleColor Color for the icon
 * @param totalUsersInChat Total number of users in the chat
 */
@Composable
private fun ReadReceiptIcon(message: Message, onBubbleColor: Color, totalUsersInChat: Int) {
  val readByOthersCount = message.readBy.count { it != message.senderId }
  val otherUsersCount = totalUsersInChat - 1
  val isReadByAll = otherUsersCount > 0 && readByOthersCount >= otherUsersCount

  Icon(
      imageVector = Icons.Default.DoneAll,
      contentDescription =
          stringResource(if (isReadByAll) R.string.read_by_all else R.string.message_sent),
      modifier = Modifier.size(Dimens.IconSize.small),
      tint = if (isReadByAll) MaterialTheme.colorScheme.primary else onBubbleColor)
}

/**
 * Individual message item with bubble design and avatar.
 *
 * @param message The message to display
 * @param isCurrentUser Whether the message was sent by the current user
 * @param senderPhotoUrl Photo URL of the message sender
 * @param currentUserPhotoUrl Photo URL of the current user (for displaying their avatar)
 * @param bubbleColor The color for the message bubble
 * @param onBubbleColor The color for text on the message bubble (must provide proper contrast)
 * @param totalUsersInChat Total number of users in the chat (for read receipts)
 * @param onLongPress Callback invoked when the message is long-pressed
 * @param onImageClick Callback invoked when an image message is clicked for full-screen view
 * @param onLocationClick Callback invoked when a location message is clicked
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageItem(
    message: Message,
    isCurrentUser: Boolean,
    senderPhotoUrl: String? = null,
    currentUserPhotoUrl: String? = null,
    bubbleColor: Color,
    onBubbleColor: Color,
    totalUsersInChat: Int = 0,
    onLongPress: () -> Unit = {},
    onImageClick: (String) -> Unit = {},
    onLocationClick: (Location, String) -> Unit = { _, _ -> }
) {
  val horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start

  Row(
      modifier =
          Modifier.fillMaxWidth()
              .padding(horizontal = Dimens.Padding.small)
              .testTag(ChatScreenTestTags.getTestTagForMessage(message.id)),
      horizontalArrangement = horizontalArrangement) {
        if (!isCurrentUser) {
          UserAvatar(
              photoUrl = senderPhotoUrl,
              userName = message.senderName,
              modifier = Modifier.align(Alignment.Bottom))
          Spacer(modifier = Modifier.width(Dimens.Spacing.small))
        }

        Surface(
            modifier =
                Modifier.widthIn(max = Dimens.Chat.messageBubbleMaxWidth)
                    .testTag(ChatScreenTestTags.getTestTagForMessageBubble(message.id))
                    .combinedClickable(onClick = {}, onLongClick = onLongPress),
            shape = getMessageBubbleShape(isCurrentUser),
            color = bubbleColor,
            shadowElevation = Dimens.Elevation.small) {
              Column(modifier = Modifier.padding(Dimens.Padding.small)) {
                MessageContent(
                    message,
                    isCurrentUser,
                    onBubbleColor,
                    bubbleColor,
                    onImageClick,
                    onLocationClick)
                if (message.type != MessageType.SYSTEM) {
                  MessageMetadata(message, isCurrentUser, onBubbleColor, totalUsersInChat)
                }
              }
            }

        if (isCurrentUser) {
          Spacer(modifier = Modifier.width(Dimens.Spacing.small))
          UserAvatar(
              photoUrl = currentUserPhotoUrl,
              userName = message.senderName,
              modifier = Modifier.align(Alignment.Bottom))
        }
      }
}

/**
 * Displays user avatar in chat messages with actual profile photo.
 *
 * Shows the user's profile photo if available, or falls back to default avatar icon. Integrates
 * with existing ProfilePhotoImage component for consistency across the app.
 *
 * @param photoUrl URL of the user's profile photo, or null for default avatar
 * @param userName Name of the user for accessibility descriptions
 * @param modifier Modifier for positioning (typically Alignment.Bottom in message row)
 */
@Composable
private fun UserAvatar(photoUrl: String?, userName: String, modifier: Modifier = Modifier) {
  ProfilePhotoImage(
      photoUrl = photoUrl,
      contentDescription = stringResource(R.string.user_avatar_description, userName),
      modifier = modifier,
      size = Dimens.IconSize.medium,
      shape = CircleShape,
      showLoadingIndicator = false // Don't show spinner for small avatars in chat
      )
}

/**
 * Displays a location message in the chat with a Google Maps preview.
 *
 * This composable handles:
 * - Displaying an interactive map preview of the location
 * - Showing the location name
 * - Click to view on full map screen
 *
 * @param location The Location object containing coordinates and name
 * @param onClick Callback when the location is clicked
 */
@Composable
private fun ChatLocationMessage(location: Location, onClick: () -> Unit = {}) {
  val locationLatLng = LatLng(location.latitude, location.longitude)
  val cameraPositionState = rememberCameraPositionState {
    position = CameraPosition.fromLatLngZoom(locationLatLng, Dimens.Chat.locationPreviewZoom)
  }

  Column(
      modifier =
          Modifier.fillMaxWidth()
              .clickable(onClick = { onClick() })
              .testTag(ChatScreenTestTags.LOCATION_MESSAGE_PREVIEW)) {
        // Google Map preview
        Box(
            modifier =
                Modifier.fillMaxWidth()
                    .height(
                        Dimens.Chat.messageBubbleMaxWidth *
                            Dimens.Chat.locationMessageHeightMultiplier)
                    .clip(RoundedCornerShape(Dimens.CornerRadius.small))) {
              GoogleMap(
                  modifier = Modifier.fillMaxSize(),
                  cameraPositionState = cameraPositionState,
                  properties = MapProperties(),
                  uiSettings = disabledMapUiSettings) {
                    Marker(state = MarkerState(position = locationLatLng), title = location.name)
                  }
            }

        // Location name
        Spacer(modifier = Modifier.height(Dimens.Spacing.small))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.Padding.extraSmall),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start) {
              Icon(
                  imageVector = Icons.Default.LocationOn,
                  contentDescription = null,
                  modifier = Modifier.size(Dimens.IconSize.small),
                  tint = MaterialTheme.colorScheme.primary)
              Spacer(modifier = Modifier.width(Dimens.Spacing.extraSmall))
              Text(
                  text = location.name,
                  style = MaterialTheme.typography.bodySmall,
                  fontWeight = FontWeight.Medium,
                  color = MaterialTheme.colorScheme.onSurface)
            }

        Spacer(modifier = Modifier.height(Dimens.Spacing.extraSmall))
      }
}

/**
 * Displays an image message in the chat using Coil with proper loading states.
 *
 * This composable handles:
 * - Loading remote images from Firebase Storage URLs with automatic caching
 * - Showing a loading indicator while the image loads
 * - Proper content scaling to maintain aspect ratio
 * - Smooth crossfade transition when image loads
 * - Consistent styling with message bubbles
 * - Click to view in full screen
 *
 * @param imageUrl The Firebase Storage URL of the image to display
 * @param bubbleColor The color for the loading indicator background
 * @param onClick Callback when the image is clicked
 */
@Composable
private fun ChatImageMessage(imageUrl: String, bubbleColor: Color, onClick: () -> Unit = {}) {
  val context = LocalContext.current

  Box(
      modifier =
          Modifier.widthIn(max = Dimens.Chat.messageBubbleMaxWidth)
              .testTag(ChatScreenTestTags.CHAT_IMAGE_CONTAINER),
      contentAlignment = Alignment.Center) {
        coil.compose.SubcomposeAsyncImage(
            model =
                coil.request.ImageRequest.Builder(context)
                    .data(imageUrl)
                    .crossfade(true) // Smooth transition when image loads
                    .build(),
            contentDescription = stringResource(R.string.image_message),
            modifier =
                Modifier.widthIn(max = Dimens.Chat.messageBubbleMaxWidth)
                    .clip(RoundedCornerShape(Dimens.CornerRadius.medium))
                    .clickable(onClick = onClick)
                    .testTag(ChatScreenTestTags.CHAT_IMAGE_REMOTE),
            contentScale = ContentScale.Fit, // Maintain aspect ratio
            loading = {
              // Show loading indicator
              Box(
                  modifier =
                      Modifier.widthIn(max = Dimens.Chat.messageBubbleMaxWidth)
                          .height(Dimens.Chat.messageBubbleMaxWidth * 0.75f)
                          .testTag(ChatScreenTestTags.CHAT_IMAGE_LOADING), // 4:3 aspect ratio
                  contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = bubbleColor)
                  }
            },
            error = {
              // Show simple error icon like ProfilePhotoImage
              Box(
                  modifier =
                      Modifier.widthIn(max = Dimens.Chat.messageBubbleMaxWidth)
                          .height(Dimens.Chat.messageBubbleMaxWidth * 0.75f)
                          .clip(RoundedCornerShape(Dimens.CornerRadius.medium))
                          .background(MaterialTheme.colorScheme.errorContainer)
                          .testTag(ChatScreenTestTags.CHAT_IMAGE_ERROR),
                  contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.BrokenImage,
                        contentDescription = stringResource(R.string.unknown_error),
                        modifier = Modifier.size(Dimens.IconSize.large),
                        tint = MaterialTheme.colorScheme.onErrorContainer)
                  }
            })
      }
}

/**
 * Message input field with attachment button, text field, and dynamic send/mic button.
 *
 * Features:
 * - Attachment button (left): Opens a menu with Gallery, Location, and Poll options
 * - Text input field (center): For typing messages
 * - Dynamic button (right): Shows microphone when text is empty, send button when text is present
 *
 * @param text Current input text
 * @param onTextChange Callback when text changes
 * @param onSendClick Callback when send button is clicked
 * @param sendButtonColor The color for the send button background
 * @param onSendButtonColor The color for the send button icon (must provide proper contrast)
 * @param viewModel ChatViewModel for handling image uploads
 * @param currentUserName The display name of the current user for image messages
 * @param isUploadingImage Whether an image is currently being uploaded
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MessageInput(
    text: String,
    onTextChange: (String) -> Unit,
    onSendClick: () -> Unit,
    sendButtonColor: Color,
    onSendButtonColor: Color,
    viewModel: ChatViewModel,
    currentUserName: String,
    isUploadingImage: Boolean
) {
  var showAttachmentMenu by remember { mutableStateOf(false) }

  Column {
    // Upload progress indicator
    if (isUploadingImage) {
      LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = sendButtonColor)
    }

    Surface(shadowElevation = Dimens.Elevation.small, color = MaterialTheme.colorScheme.surface) {
      Row(
          modifier =
              Modifier.fillMaxWidth()
                  .padding(horizontal = Dimens.Padding.small, vertical = Dimens.Padding.small),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing.small)) {
            // Attachment button (left)
            IconButton(
                onClick = { showAttachmentMenu = true },
                enabled = !isUploadingImage,
                modifier =
                    Modifier.size(Dimens.TouchTarget.minimum)
                        .testTag(ChatScreenTestTags.ATTACHMENT_BUTTON)) {
                  Icon(
                      imageVector = Icons.Default.AttachFile,
                      contentDescription = stringResource(R.string.add_attachment),
                      tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }

            // Text input field (center)
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                enabled = !isUploadingImage,
                modifier = Modifier.weight(1f).testTag(ChatScreenTestTags.MESSAGE_INPUT),
                placeholder = {
                  Text(
                      text = stringResource(R.string.message_placeholder),
                      color = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                shape = RoundedCornerShape(Dimens.CornerRadius.pill),
                colors = MaterialTheme.customColors.outlinedTextField(),
                maxLines = 4)

            // Dynamic send button (right) - disabled appearance when no text
            if (text.isEmpty()) {
              // Disabled send button appearance when no text
              IconButton(
                  onClick = {},
                  enabled = false,
                  modifier =
                      Modifier.size(Dimens.TouchTarget.minimum)
                          .background(
                              color = MaterialTheme.colorScheme.surfaceVariant, shape = CircleShape)
                          .testTag(ChatScreenTestTags.SEND_BUTTON)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = stringResource(R.string.send_message),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                  }
            } else {
              // Send button
              IconButton(
                  onClick = onSendClick,
                  enabled = !isUploadingImage,
                  modifier =
                      Modifier.size(Dimens.TouchTarget.minimum)
                          .background(color = sendButtonColor, shape = CircleShape)
                          .testTag(ChatScreenTestTags.SEND_BUTTON)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = stringResource(R.string.send_message),
                        tint = onSendButtonColor)
                  }
            }
          }
    }
  }

  // Attachment menu bottom sheet
  if (showAttachmentMenu) {
    AttachmentMenu(
        onDismiss = { showAttachmentMenu = false },
        viewModel = viewModel,
        currentUserName = currentUserName)
  }
}

/**
 * Bottom sheet menu for attachment options.
 *
 * Displays two options in a horizontal row:
 * - Photo: For taking photos or selecting from gallery (opens dialog to choose)
 * - Location: For sharing current location (shows preview before sending)
 *
 * Note: Polls are only available in group/event chats via ChatScreenWithPolls.
 *
 * @param onDismiss Callback when the menu should be dismissed
 * @param viewModel ChatViewModel for handling image uploads and location messages
 * @param currentUserName The display name of the current user for image messages
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AttachmentMenu(
    onDismiss: () -> Unit,
    viewModel: ChatViewModel,
    currentUserName: String
) {
  val sheetState = rememberModalBottomSheetState()
  val context = LocalContext.current

  // State to hold the camera image URI
  var cameraImageUri by remember { mutableStateOf<Uri?>(null) }

  // State to show photo source selection dialog
  var showPhotoSourceDialog by remember { mutableStateOf(false) }

  // State for location preview
  var showLocationPreview by remember { mutableStateOf(false) }
  var currentUserLocation by remember { mutableStateOf<UserLocation?>(null) }

  // Image picker launcher for gallery (untestable - extracted to ChatImageLaunchers.kt)
  val imagePickerLauncher =
      rememberImagePickerLauncher(
          viewModel = viewModel, currentUserName = currentUserName, onDismiss = onDismiss)

  // Camera launcher (untestable - extracted to ChatImageLaunchers.kt)
  val cameraLauncher =
      rememberCameraLauncher(
          viewModel = viewModel,
          currentUserName = currentUserName,
          cameraImageUri = { cameraImageUri },
          onDismiss = onDismiss)

  // Camera permission launcher (untestable - extracted to ChatImageLaunchers.kt)
  val cameraPermissionLauncher =
      rememberCameraPermissionLauncher(
          cameraLauncher = cameraLauncher,
          onCameraImageUriSet = {
            val uri = createImageUri(context)
            cameraImageUri = uri
            uri
          })

  // Location permission launcher (untestable - extracted to ChatImageLaunchers.kt)
  val locationPermissionsLauncher =
      rememberLocationPermissionsLauncher(
          onLocationRetrieved = { location ->
            currentUserLocation = location
            showLocationPreview = true
          })

  ModalBottomSheet(
      onDismissRequest = onDismiss,
      sheetState = sheetState,
      modifier = Modifier.testTag(ChatScreenTestTags.ATTACHMENT_MENU)) {
        Column(modifier = Modifier.fillMaxWidth().padding(Dimens.Padding.large)) {

          // Options row - horizontal layout matching Figma
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {

            // Photo option - opens dialog to choose between gallery and camera
            AttachmentOption(
                icon = Icons.Default.Image,
                label = stringResource(R.string.photo),
                onClick = { showPhotoSourceDialog = true },
                modifier = Modifier.testTag(ChatScreenTestTags.ATTACHMENT_PHOTO))

            // Location option
            AttachmentOption(
                icon = Icons.Default.LocationOn,
                label = stringResource(R.string.location),
                onClick = {
                  locationPermissionsLauncher.launch(
                      arrayOf(
                          android.Manifest.permission.ACCESS_FINE_LOCATION,
                          android.Manifest.permission.ACCESS_COARSE_LOCATION))
                },
                modifier = Modifier.testTag(ChatScreenTestTags.ATTACHMENT_LOCATION))
          }

          Spacer(modifier = Modifier.height(Dimens.Padding.large))
        }
      }

  // Photo source selection dialog
  if (showPhotoSourceDialog) {
    PhotoSourceDialog(
        onDismiss = { showPhotoSourceDialog = false },
        onGalleryClick = {
          showPhotoSourceDialog = false
          imagePickerLauncher.launch("image/*")
        },
        onCameraClick = {
          showPhotoSourceDialog = false
          cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        })
  }

  // Location preview dialog
  if (showLocationPreview) {
    currentUserLocation?.let { location ->
      LocationPreviewDialog(
          userLocation = location,
          onDismiss = {
            showLocationPreview = false
            currentUserLocation = null
          },
          onSendLocation = {
            viewModel.sendCurrentLocation(
                context = context,
                userLocation = location,
                senderName = currentUserName,
                onSuccess = {
                  showLocationPreview = false
                  currentUserLocation = null
                  onDismiss()
                },
                onError = { error -> Toast.makeText(context, error, Toast.LENGTH_SHORT).show() })
          })
    }
  }
}

/**
 * Dialog for previewing and sending current location.
 *
 * Displays a Google Maps preview of the user's current location with options to send or cancel.
 *
 * @param userLocation The current user location to preview
 * @param onDismiss Callback when dialog is dismissed
 * @param onSendLocation Callback when send button is clicked
 */
@Composable
internal fun LocationPreviewDialog(
    userLocation: UserLocation,
    onDismiss: () -> Unit,
    onSendLocation: () -> Unit
) {
  val locationLatLng = LatLng(userLocation.latitude, userLocation.longitude)
  val cameraPositionState = rememberCameraPositionState {
    position = CameraPosition.fromLatLngZoom(locationLatLng, Dimens.Chat.locationPreviewZoom)
  }

  AlertDialog(
      onDismissRequest = onDismiss,
      modifier = Modifier.testTag(ChatScreenTestTags.LOCATION_PREVIEW_DIALOG),
      title = { Text(stringResource(R.string.location_preview)) },
      text = {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally) {
              // Google Map preview
              Box(
                  modifier =
                      Modifier.fillMaxWidth()
                          .height(
                              Dimens.Chat.messageBubbleMaxWidth *
                                  Dimens.Chat.locationPreviewDialogHeightMultiplier)
                          .clip(RoundedCornerShape(Dimens.CornerRadius.medium))
                          .testTag(ChatScreenTestTags.LOCATION_PREVIEW_MAP)) {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        properties = MapProperties(),
                        uiSettings = disabledMapUiSettings) {
                          Marker(
                              state = MarkerState(position = locationLatLng),
                              title = stringResource(R.string.current_location))
                        }
                  }

              Spacer(modifier = Modifier.height(Dimens.Spacing.small))

              // Location coordinates
              Text(
                  text = "${userLocation.latitude}, ${userLocation.longitude}",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
      },
      confirmButton = {
        Button(
            onClick = onSendLocation,
            modifier = Modifier.testTag(ChatScreenTestTags.LOCATION_PREVIEW_SEND_BUTTON),
            colors = MaterialTheme.customColors.buttonColors()) {
              Text(stringResource(R.string.send_location))
            }
      },
      dismissButton = {
        TextButton(
            onClick = onDismiss,
            modifier = Modifier.testTag(ChatScreenTestTags.LOCATION_PREVIEW_CANCEL_BUTTON)) {
              Text(stringResource(R.string.cancel))
            }
      })
}

/**
 * Dialog for editing a message.
 *
 * @param message The message to edit
 * @param onDismiss Callback when dialog is dismissed
 * @param onConfirm Callback when edit is confirmed with new content
 */
@Composable
private fun EditMessageDialog(
    message: Message,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
  var editedText by remember { mutableStateOf(message.content) }

  AlertDialog(
      onDismissRequest = onDismiss,
      modifier = Modifier.testTag(ChatScreenTestTags.EDIT_MESSAGE_DIALOG),
      title = { Text(stringResource(R.string.edit_message_title)) },
      text = {
        OutlinedTextField(
            value = editedText,
            onValueChange = { editedText = it },
            modifier = Modifier.fillMaxWidth().testTag(ChatScreenTestTags.EDIT_MESSAGE_INPUT),
            placeholder = { Text(stringResource(R.string.edit_message_placeholder)) },
            colors = MaterialTheme.customColors.outlinedTextField(),
            maxLines = 4)
      },
      confirmButton = {
        Button(
            onClick = { onConfirm(editedText) },
            enabled = editedText.isNotBlank(),
            modifier = Modifier.testTag(ChatScreenTestTags.EDIT_MESSAGE_SAVE_BUTTON),
            colors = MaterialTheme.customColors.buttonColors()) {
              Text(stringResource(R.string.save))
            }
      },
      dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } })
}

/**
 * Dialog for confirming message deletion.
 *
 * @param onDismiss Callback when dialog is dismissed
 * @param onConfirm Callback when deletion is confirmed
 */
@Composable
private fun DeleteMessageDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
  AlertDialog(
      onDismissRequest = onDismiss,
      title = { Text(stringResource(R.string.delete_message_title)) },
      text = { Text(stringResource(R.string.delete_message_confirmation)) },
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

/**
 * Full-screen image viewer overlay.
 *
 * Displays an image in full screen with a close button. The background is dimmed and clicking
 * outside the image dismisses the viewer.
 *
 * @param imageUrl The URL of the image to display in full screen
 * @param onDismiss Callback when the viewer is dismissed
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FullScreenImageViewer(imageUrl: String, onDismiss: () -> Unit) {
  val context = LocalContext.current

  Box(
      modifier =
          Modifier.fillMaxSize()
              .background(MaterialTheme.customColors.scrimOverlay)
              .combinedClickable(onClick = onDismiss, onLongClick = {}),
      contentAlignment = Alignment.Center) {
        // Close button (top right)
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.align(Alignment.TopEnd).padding(Dimens.Padding.medium)) {
              Icon(
                  imageVector = Icons.Default.Close,
                  contentDescription = stringResource(R.string.close),
                  tint = MaterialTheme.colorScheme.onSurface,
                  modifier = Modifier.size(Dimens.IconSize.large))
            }

        // Full screen image
        coil.compose.SubcomposeAsyncImage(
            model =
                coil.request.ImageRequest.Builder(context).data(imageUrl).crossfade(true).build(),
            contentDescription = stringResource(R.string.image_message),
            modifier = Modifier.fillMaxWidth().padding(Dimens.Padding.medium),
            contentScale = ContentScale.Fit,
            loading = {
              Box(
                  modifier = Modifier.fillMaxSize().padding(Dimens.Padding.medium),
                  contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                  }
            },
            error = {
              Box(
                  modifier = Modifier.fillMaxSize().padding(Dimens.Padding.medium),
                  contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.unknown_error),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface)
                  }
            })
      }
}

/**
 * Dialog showing who has read a message.
 *
 * @param message The message to show read receipts for
 * @param senderProfiles Map of user IDs to their profiles
 * @param onDismiss Callback when dialog is dismissed
 */
@Composable
private fun WhoReadDialog(
    message: Message,
    senderProfiles: Map<String, Profile>,
    onDismiss: () -> Unit
) {
  // Filter out the sender from the readBy list
  val readByOthers = message.readBy.filter { it != message.senderId }

  AlertDialog(
      onDismissRequest = onDismiss,
      title = { Text(stringResource(R.string.read_by_title)) },
      text = {
        Column(modifier = Modifier.fillMaxWidth()) {
          if (readByOthers.isEmpty()) {
            Text(
                stringResource(R.string.no_one_read_yet),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
          } else {
            readByOthers.forEach { userId ->
              val profile = senderProfiles[userId]
              Row(
                  modifier = Modifier.fillMaxWidth().padding(vertical = Dimens.Padding.extraSmall),
                  verticalAlignment = Alignment.CenterVertically) {
                    UserAvatar(
                        photoUrl = profile?.photoUrl,
                        userName = profile?.username ?: stringResource(R.string.unknown_user),
                        modifier = Modifier.size(Dimens.Profile.photoSmall))
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

/**
 * Context menu displayed when a message is long-pressed.
 *
 * Shows options based on whether the message belongs to the current user. The menu appears as a
 * centered overlay with blur effect in the background.
 *
 * @param isCurrentUser Whether the message belongs to the current user
 * @param messageType The type of message (TEXT, IMAGE, SYSTEM)
 * @param onDismiss Callback to close the menu
 * @param onCopy Callback to copy message content (only shown for text messages)
 * @param onEdit Callback to edit the message (only for current user's text messages)
 * @param onDelete Callback to delete the message (only for current user)
 * @param onSeeWhoRead Callback to see who read the message (only for current user)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageContextMenu(
    isCurrentUser: Boolean,
    messageType: MessageType,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSeeWhoRead: () -> Unit
) {
  Box(
      modifier =
          Modifier.fillMaxSize()
              .background(MaterialTheme.customColors.scrimOverlay)
              .combinedClickable(onClick = onDismiss, onLongClick = {}),
      contentAlignment = Alignment.Center) {
        Surface(
            shape = RoundedCornerShape(Dimens.CornerRadius.large),
            shadowElevation = Dimens.Elevation.large,
            color = MaterialTheme.colorScheme.surface) {
              Column(modifier = Modifier.padding(Dimens.Padding.small)) {
                // Copy option (only for text messages)
                if (messageType == MessageType.TEXT) {
                  DropdownMenuItem(
                      text = { Text(stringResource(R.string.copy)) },
                      onClick = onCopy,
                      leadingIcon = {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = stringResource(R.string.copy),
                            modifier = Modifier.size(Dimens.IconSize.medium))
                      })
                }

                // Options only for current user's messages
                if (isCurrentUser) {
                  // Edit option (only for text messages)
                  if (messageType == MessageType.TEXT) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.edit)) },
                        onClick = onEdit,
                        leadingIcon = {
                          Icon(
                              Icons.Default.Edit,
                              contentDescription = stringResource(R.string.edit),
                              modifier = Modifier.size(Dimens.IconSize.medium))
                        })
                  }

                  DropdownMenuItem(
                      text = { Text(stringResource(R.string.delete)) },
                      onClick = onDelete,
                      leadingIcon = {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = stringResource(R.string.delete),
                            tint = MaterialTheme.customColors.deleteButton,
                            modifier = Modifier.size(Dimens.IconSize.medium))
                      })

                  DropdownMenuItem(
                      text = { Text(stringResource(R.string.see_who_read)) },
                      onClick = onSeeWhoRead,
                      leadingIcon = {
                        Icon(
                            Icons.Default.Visibility,
                            contentDescription = stringResource(R.string.see_who_read),
                            modifier = Modifier.size(Dimens.IconSize.medium))
                      })
                }
              }
            }
      }
}

/**
 * Individual attachment option with icon and label.
 *
 * @param icon The icon to display
 * @param label The text label below the icon
 * @param onClick Callback when the option is clicked
 * @param modifier Modifier for the component
 */
@Composable
private fun AttachmentOption(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
  Column(
      modifier = modifier.clickable(onClick = onClick).padding(Dimens.Padding.medium),
      horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(Dimens.IconSize.large))

        Spacer(modifier = Modifier.height(Dimens.Spacing.small))

        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface)
      }
}

/**
 * Dialog for choosing photo source (Gallery or Camera).
 *
 * @param onDismiss Callback when dialog is dismissed
 * @param onGalleryClick Callback when gallery option is selected
 * @param onCameraClick Callback when camera option is selected
 */
@Composable
private fun PhotoSourceDialog(
    onDismiss: () -> Unit,
    onGalleryClick: () -> Unit,
    onCameraClick: () -> Unit
) {
  AlertDialog(
      onDismissRequest = onDismiss,
      modifier = Modifier.testTag(ChatScreenTestTags.PHOTO_SOURCE_DIALOG),
      title = { Text(text = stringResource(R.string.choose_photo_source)) },
      text = {
        Column(modifier = Modifier.fillMaxWidth()) {
          // Gallery option
          TextButton(
              onClick = onGalleryClick,
              modifier = Modifier.fillMaxWidth().testTag(ChatScreenTestTags.PHOTO_SOURCE_GALLERY)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing.small)) {
                      Icon(
                          imageVector = Icons.Default.Image,
                          contentDescription = stringResource(R.string.gallery))
                      Text(text = stringResource(R.string.gallery))
                    }
              }

          // Camera option
          TextButton(
              onClick = onCameraClick,
              modifier = Modifier.fillMaxWidth().testTag(ChatScreenTestTags.PHOTO_SOURCE_CAMERA)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing.small)) {
                      Icon(
                          imageVector = Icons.Default.CameraAlt,
                          contentDescription = stringResource(R.string.camera))
                      Text(text = stringResource(R.string.camera))
                    }
              }
        }
      },
      confirmButton = {},
      dismissButton = {
        TextButton(onClick = onDismiss) { Text(text = stringResource(R.string.cancel)) }
      })
}
