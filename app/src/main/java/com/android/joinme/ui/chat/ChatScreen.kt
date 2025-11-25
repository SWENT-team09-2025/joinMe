package com.android.joinme.ui.chat

// Implemented with help of Claude AI

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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.android.joinme.R
import com.android.joinme.model.chat.Message
import com.android.joinme.model.chat.MessageType
import com.android.joinme.ui.profile.ProfilePhotoImage
import com.android.joinme.ui.theme.Dimens
import com.android.joinme.ui.theme.buttonColors
import com.android.joinme.ui.theme.customColors
import com.android.joinme.ui.theme.getUserColor
import com.android.joinme.ui.theme.outlinedTextField
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

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
  const val ATTACHMENT_GALLERY = "attachmentGallery"
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
    totalParticipants: Int = 1 // Total number of participants in the event/group
) {
  val uiState by viewModel.uiState.collectAsState()
  val snackbarHostState = remember { SnackbarHostState() }
  val coroutineScope = rememberCoroutineScope()

  // Use chatDefault colors if no specific colors provided
  val effectiveChatColor = chatColor ?: MaterialTheme.customColors.chatDefault
  val effectiveOnChatColor = onChatColor ?: MaterialTheme.customColors.onChatDefault

  // Initialize chat when screen loads
  LaunchedEffect(chatId, currentUserId) { viewModel.initializeChat(chatId, currentUserId) }

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
            onTopBarColor = effectiveOnChatColor)
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
              senderProfiles = uiState.senderProfiles,
              onSendMessage = { content -> viewModel.sendMessage(content, currentUserName) },
              paddingValues = paddingValues,
              chatColor = effectiveChatColor,
              onChatColor = effectiveOnChatColor,
              viewModel = viewModel,
              totalParticipants = totalParticipants)
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
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatTopBar(
    chatTitle: String,
    onLeaveClick: () -> Unit,
    topBarColor: Color,
    onTopBarColor: Color
) {
  Surface(
      modifier = Modifier.fillMaxWidth().testTag(ChatScreenTestTags.TOP_BAR),
      color = topBarColor,
      shadowElevation = Dimens.Elevation.small) {
        Row(
            modifier =
                Modifier.fillMaxWidth()
                    .padding(horizontal = Dimens.Padding.medium, vertical = Dimens.Padding.small),
            verticalAlignment = Alignment.CenterVertically) {
              // Title
              Text(
                  text = chatTitle,
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  color = onTopBarColor,
                  modifier = Modifier.weight(1f).testTag(ChatScreenTestTags.TITLE))

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
 * @param senderProfiles A map of sender IDs to their Profile objects containing photo URLs
 * @param onSendMessage Callback invoked when sending a new message
 * @param paddingValues Padding from the Scaffold
 * @param chatColor The chat color for message bubbles and send button
 * @param onChatColor The color for text on chat-colored elements (must provide proper contrast)
 */
@Composable
private fun ChatContent(
    messages: List<Message>,
    currentUserId: String,
    senderProfiles: Map<String, com.android.joinme.model.profile.Profile>,
    onSendMessage: (String) -> Unit,
    paddingValues: PaddingValues,
    chatColor: Color,
    onChatColor: Color,
    viewModel: ChatViewModel,
    totalParticipants: Int
) {
  var messageText by remember { mutableStateOf("") }
  val listState = rememberLazyListState()
  var selectedMessage by remember { mutableStateOf<Message?>(null) }
  var showEditDialog by remember { mutableStateOf(false) }
  var showDeleteDialog by remember { mutableStateOf(false) }
  var showWhoReadDialog by remember { mutableStateOf(false) }
  val clipboardManager = LocalClipboardManager.current

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
          LazyColumn(
              modifier =
                  Modifier.weight(1f).fillMaxWidth().testTag(ChatScreenTestTags.MESSAGE_LIST),
              state = listState,
              contentPadding = PaddingValues(Dimens.Padding.medium),
              verticalArrangement = Arrangement.spacedBy(Dimens.Spacing.itemSpacing)) {
                if (messages.isEmpty()) {
                  item {
                    Box(
                        modifier = Modifier.fillParentMaxSize(),
                        contentAlignment = Alignment.Center) {
                          Text(
                              text = stringResource(R.string.empty_chat_message),
                              style = MaterialTheme.typography.bodyMedium,
                              color = MaterialTheme.colorScheme.onSurfaceVariant,
                              textAlign = TextAlign.Center,
                              modifier = Modifier.testTag(ChatScreenTestTags.EMPTY_MESSAGE))
                        }
                  }
                } else {
                  items(messages, key = { it.id }) { message ->
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
                        onLongPress = { selectedMessage = message })
                  }
                }
              }

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
              onSendButtonColor = onChatColor)
        }

    // Context menu overlay (shown when a message is selected)
    selectedMessage?.let { message ->
      MessageContextMenu(
          message = message,
          isCurrentUser = message.senderId == currentUserId,
          onDismiss = { selectedMessage = null },
          onCopy = {
            clipboardManager.setText(AnnotatedString(message.content))
            selectedMessage = null
          },
          onEdit = {
            showEditDialog = true
            // Keep selectedMessage for the dialog
          },
          onDelete = {
            showDeleteDialog = true
            // Keep selectedMessage for the dialog
          },
          onSeeWhoRead = {
            showWhoReadDialog = true
            // Keep selectedMessage for the dialog
          })
    }

    // Edit dialog
    if (showEditDialog && selectedMessage != null) {
      EditMessageDialog(
          message = selectedMessage!!,
          onDismiss = {
            showEditDialog = false
            selectedMessage = null
          },
          onConfirm = { newContent ->
            viewModel.editMessage(selectedMessage!!.id, newContent)
            showEditDialog = false
            selectedMessage = null
          })
    }

    // Delete confirmation dialog
    if (showDeleteDialog && selectedMessage != null) {
      DeleteMessageDialog(
          onDismiss = {
            showDeleteDialog = false
            selectedMessage = null
          },
          onConfirm = {
            viewModel.deleteMessage(selectedMessage!!.id)
            showDeleteDialog = false
            selectedMessage = null
          })
    }

    // Who read dialog
    if (showWhoReadDialog && selectedMessage != null) {
      WhoReadDialog(
          message = selectedMessage!!,
          senderProfiles = senderProfiles,
          onDismiss = {
            showWhoReadDialog = false
            selectedMessage = null
          })
    }
  }
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
    onLongPress: () -> Unit = {}
) {
  Row(
      modifier =
          Modifier.fillMaxWidth()
              .padding(horizontal = Dimens.Padding.small)
              .testTag(ChatScreenTestTags.getTestTagForMessage(message.id)),
      horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start) {
        if (!isCurrentUser) {
          // Avatar for other users (on left)
          UserAvatar(
              photoUrl = senderPhotoUrl,
              userName = message.senderName,
              modifier = Modifier.align(Alignment.Bottom))
          Spacer(modifier = Modifier.width(Dimens.Spacing.small))
        }

        // Message bubble
        Surface(
            modifier =
                Modifier.widthIn(max = Dimens.Chat.messageBubbleMaxWidth)
                    .testTag(ChatScreenTestTags.getTestTagForMessageBubble(message.id))
                    .combinedClickable(onClick = {}, onLongClick = onLongPress),
            shape =
                RoundedCornerShape(
                    topStart = Dimens.CornerRadius.extraLarge,
                    topEnd = Dimens.CornerRadius.extraLarge,
                    bottomStart =
                        if (isCurrentUser) Dimens.CornerRadius.extraLarge
                        else Dimens.CornerRadius.small,
                    bottomEnd =
                        if (isCurrentUser) Dimens.CornerRadius.small
                        else Dimens.CornerRadius.extraLarge),
            color = bubbleColor,
            shadowElevation = Dimens.Elevation.small) {
              Column(modifier = Modifier.padding(Dimens.Padding.small)) {
                // System messages
                if (message.type == MessageType.SYSTEM) {
                  Text(
                      text = message.content,
                      style = MaterialTheme.typography.bodySmall,
                      color = onBubbleColor.copy(alpha = 0.9f),
                      fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                } else {
                  // Show sender name for other users
                  if (!isCurrentUser) {
                    Text(
                        text = message.senderName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = onBubbleColor.copy(alpha = 0.8f))
                    Spacer(modifier = Modifier.height(Dimens.Spacing.extraSmall))
                  }

                  // Message content
                  Text(
                      text = message.content,
                      style = MaterialTheme.typography.bodyMedium,
                      color = onBubbleColor)

                  // Timestamp with read receipts and edited indicator
                  Spacer(modifier = Modifier.height(Dimens.Spacing.extraSmall))
                  Row(
                      horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing.extraSmall),
                      verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = formatTimestamp(message.timestamp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)

                        // Show "edited" text if message has been edited
                        if (message.isEdited) {
                          Text(
                              text = stringResource(R.string.message_edited),
                              style = MaterialTheme.typography.labelSmall,
                              color = MaterialTheme.colorScheme.onSurfaceVariant,
                              fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                        }

                        // Show read receipts only for current user's messages
                        if (isCurrentUser && message.type != MessageType.SYSTEM) {
                          // Calculate if read by all (excluding sender)
                          val readByOthersCount = message.readBy.count { it != message.senderId }
                          val otherUsersCount = totalUsersInChat - 1 // Exclude sender
                          val isReadByAll =
                              otherUsersCount > 0 && readByOthersCount >= otherUsersCount

                          Icon(
                              imageVector = Icons.Default.DoneAll,
                              contentDescription =
                                  stringResource(
                                      if (isReadByAll) R.string.read_by_all
                                      else R.string.message_sent),
                              modifier = Modifier.size(Dimens.IconSize.small),
                              tint =
                                  if (isReadByAll) MaterialTheme.colorScheme.primary
                                  else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                      }
                }
              }
            }

        if (isCurrentUser) {
          // Avatar for current user (on right)
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
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MessageInput(
    text: String,
    onTextChange: (String) -> Unit,
    onSendClick: () -> Unit,
    sendButtonColor: Color,
    onSendButtonColor: Color
) {
  var showAttachmentMenu by remember { mutableStateOf(false) }
  val context = LocalContext.current
  val notImplementedMsg = stringResource(R.string.not_yet_implemented)

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
              modifier = Modifier.weight(1f).testTag(ChatScreenTestTags.MESSAGE_INPUT),
              placeholder = {
                Text(
                    text = stringResource(R.string.message_placeholder),
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
              },
              shape = RoundedCornerShape(Dimens.CornerRadius.pill),
              colors = MaterialTheme.customColors.outlinedTextField(),
              maxLines = 4)

          // Dynamic send/mic button (right)
          if (text.isEmpty()) {
            // Microphone button (placeholder for future audio recording)
            // TODO(#364 and #367): Audio recording - Feature coming soon
            IconButton(
                onClick = { Toast.makeText(context, notImplementedMsg, Toast.LENGTH_SHORT).show() },
                modifier =
                    Modifier.size(Dimens.TouchTarget.minimum)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant, shape = CircleShape)
                        .testTag(ChatScreenTestTags.MIC_BUTTON)) {
                  Icon(
                      imageVector = Icons.Default.Mic,
                      contentDescription = stringResource(R.string.record_audio),
                      tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
          } else {
            // Send button
            IconButton(
                onClick = onSendClick,
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

  // Attachment menu bottom sheet
  if (showAttachmentMenu) {
    AttachmentMenu(onDismiss = { showAttachmentMenu = false })
  }
}

/**
 * Bottom sheet menu for attachment options.
 *
 * Displays three options in a horizontal row:
 * - Gallery: For sending images (not yet implemented)
 * - Location: For sharing location (not yet implemented)
 * - Poll: For creating polls (not yet implemented)
 *
 * @param onDismiss Callback when the menu should be dismissed
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AttachmentMenu(onDismiss: () -> Unit) {
  val sheetState = rememberModalBottomSheetState()
  val context = LocalContext.current
  val notImplementedMsg = stringResource(R.string.not_yet_implemented)

  ModalBottomSheet(
      onDismissRequest = onDismiss,
      sheetState = sheetState,
      modifier = Modifier.testTag(ChatScreenTestTags.ATTACHMENT_MENU)) {
        Column(modifier = Modifier.fillMaxWidth().padding(Dimens.Padding.large)) {

          // Options row - horizontal layout matching Figma
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {

            // Gallery option
            // TODO (#352): Implement image sending
            AttachmentOption(
                icon = Icons.Default.Image,
                label = stringResource(R.string.gallery),
                onClick = {
                  Toast.makeText(context, notImplementedMsg, Toast.LENGTH_SHORT).show()
                  onDismiss()
                },
                modifier = Modifier.testTag(ChatScreenTestTags.ATTACHMENT_GALLERY))

            // Location option
            // TODO (#362): Implement location sharing
            AttachmentOption(
                icon = Icons.Default.LocationOn,
                label = stringResource(R.string.location),
                onClick = {
                  Toast.makeText(context, notImplementedMsg, Toast.LENGTH_SHORT).show()
                  onDismiss()
                },
                modifier = Modifier.testTag(ChatScreenTestTags.ATTACHMENT_LOCATION))

            // Poll option
            // TODO (#363): Implement poll creation
            AttachmentOption(
                icon = Icons.Default.BarChart,
                label = stringResource(R.string.poll),
                onClick = {
                  Toast.makeText(context, notImplementedMsg, Toast.LENGTH_SHORT).show()
                  onDismiss()
                },
                modifier = Modifier.testTag(ChatScreenTestTags.ATTACHMENT_POLL))
          }

          Spacer(modifier = Modifier.height(Dimens.Padding.large))
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
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
  Column(
      modifier = modifier.clickable(onClick = onClick).padding(Dimens.Padding.medium),
      horizontalAlignment = Alignment.CenterHorizontally) {
        // Icon
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(Dimens.IconSize.large))

        Spacer(modifier = Modifier.height(Dimens.Spacing.small))

        // Label
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface)
      }
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
      title = { Text(stringResource(R.string.edit_message_title)) },
      text = {
        OutlinedTextField(
            value = editedText,
            onValueChange = { editedText = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.edit_message_placeholder)) },
            colors = MaterialTheme.customColors.outlinedTextField(),
            maxLines = 4)
      },
      confirmButton = {
        Button(
            onClick = { onConfirm(editedText) },
            enabled = editedText.isNotBlank(),
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
 * Dialog showing who has read a message.
 *
 * @param message The message to show read receipts for
 * @param senderProfiles Map of user IDs to their profiles
 * @param onDismiss Callback when dialog is dismissed
 */
@Composable
private fun WhoReadDialog(
    message: Message,
    senderProfiles: Map<String, com.android.joinme.model.profile.Profile>,
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
 * @param message The selected message
 * @param isCurrentUser Whether the message belongs to the current user
 * @param onDismiss Callback to close the menu
 * @param onCopy Callback to copy message content
 * @param onEdit Callback to edit the message (only for current user)
 * @param onDelete Callback to delete the message (only for current user)
 * @param onSeeWhoRead Callback to see who read the message (only for current user)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageContextMenu(
    message: Message,
    isCurrentUser: Boolean,
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
                // Copy option (available for all messages)
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.copy)) },
                    onClick = onCopy,
                    leadingIcon = {
                      Icon(
                          Icons.Default.ContentCopy,
                          contentDescription = stringResource(R.string.copy),
                          modifier = Modifier.size(Dimens.IconSize.medium))
                    })

                // Options only for current user's messages
                if (isCurrentUser) {
                  DropdownMenuItem(
                      text = { Text(stringResource(R.string.edit)) },
                      onClick = onEdit,
                      leadingIcon = {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = stringResource(R.string.edit),
                            modifier = Modifier.size(Dimens.IconSize.medium))
                      })

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
 * Formats a Unix timestamp into a readable time string.
 *
 * @param timestamp Unix timestamp in milliseconds
 * @return Formatted time string (e.g., "14:32")
 */
private fun formatTimestamp(timestamp: Long): String {
  val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
  return dateFormat.format(Date(timestamp))
}
