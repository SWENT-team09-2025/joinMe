package com.android.joinme.ui.chat

// Implemented with help of Claude AI

import androidx.compose.foundation.background
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.android.joinme.model.chat.Message
import com.android.joinme.model.chat.MessageType
import com.android.joinme.ui.profile.ProfilePhotoImage
import com.android.joinme.ui.theme.Dimens
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
 * @param onBackClick Callback invoked when the back button is clicked
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
    onBackClick: () -> Unit = {},
    chatColor: Color? = null,
    onChatColor: Color? = null
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
            onBackClick = onBackClick,
            onLeaveClick = onBackClick, // Leave chat navigates back
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
              currentUserName = currentUserName,
              senderProfiles = uiState.senderProfiles,
              onSendMessage = { content -> viewModel.sendMessage(content, currentUserName) },
              paddingValues = paddingValues,
              chatColor = effectiveChatColor,
              onChatColor = effectiveOnChatColor)
        }
      }
}

/**
 * Custom top bar for the chat screen matching Figma design.
 *
 * @param chatTitle The title to display
 * @param onBackClick Callback for back button
 * @param onLeaveClick Callback for leave button (not yet implemented)
 * @param topBarColor Color for the top bar background
 * @param onTopBarColor Color for text/icons on the top bar (must provide proper contrast with
 *   topBarColor)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatTopBar(
    chatTitle: String,
    onBackClick: () -> Unit,
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
                        contentDescription = "Leave chat",
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
 */
@Composable
private fun ChatContent(
    messages: List<Message>,
    currentUserId: String,
    currentUserName: String,
    senderProfiles: Map<String, com.android.joinme.model.profile.Profile>,
    onSendMessage: (String) -> Unit,
    paddingValues: PaddingValues,
    chatColor: Color,
    onChatColor: Color,
) {
  var messageText by remember { mutableStateOf("") }
  val listState = rememberLazyListState()

  // Auto-scroll to bottom when new messages arrive
  LaunchedEffect(messages.size) {
    if (messages.isNotEmpty()) {
      listState.animateScrollToItem(messages.size - 1)
    }
  }

  Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
    // Messages list
    LazyColumn(
        modifier = Modifier.weight(1f).fillMaxWidth().testTag(ChatScreenTestTags.MESSAGE_LIST),
        state = listState,
        contentPadding = PaddingValues(Dimens.Padding.medium),
        verticalArrangement = Arrangement.spacedBy(Dimens.Spacing.itemSpacing)) {
          if (messages.isEmpty()) {
            item {
              Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No messages yet. Start the conversation!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag(ChatScreenTestTags.EMPTY_MESSAGE))
              }
            }
          } else {
            items(messages, key = { it.id }) { message ->
              // Get user-specific color for this message sender
              val userColors = MaterialTheme.customColors.getUserColor(message.senderId)
              MessageItem(
                  message = message,
                  isCurrentUser = message.senderId == currentUserId,
                  senderPhotoUrl = senderProfiles[message.senderId]?.photoUrl,
                  currentUserPhotoUrl = senderProfiles[currentUserId]?.photoUrl,
                  bubbleColor = userColors.first,
                  onBubbleColor = userColors.second)
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
 */
@Composable
private fun MessageItem(
    message: Message,
    isCurrentUser: Boolean,
    senderPhotoUrl: String? = null,
    currentUserPhotoUrl: String? = null,
    bubbleColor: Color,
    onBubbleColor: Color
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
                    .testTag(ChatScreenTestTags.getTestTagForMessageBubble(message.id)),
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

                  // Timestamp
                  Spacer(modifier = Modifier.height(Dimens.Spacing.extraSmall))
                  Text(
                      text = formatTimestamp(message.timestamp),
                      style = MaterialTheme.typography.labelSmall,
                      color = onBubbleColor.copy(alpha = 0.6f))
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
      contentDescription = "$userName's avatar",
      modifier = modifier,
      size = Dimens.IconSize.medium,
      shape = CircleShape,
      showLoadingIndicator = false // Don't show spinner for small avatars in chat
      )
}

/**
 * Message input field with send button.
 *
 * @param text Current input text
 * @param onTextChange Callback when text changes
 * @param onSendClick Callback when send button is clicked
 * @param sendButtonColor The color for the send button background
 * @param onSendButtonColor The color for the send button icon (must provide proper contrast)
 */
@Composable
private fun MessageInput(
    text: String,
    onTextChange: (String) -> Unit,
    onSendClick: () -> Unit,
    sendButtonColor: Color,
    onSendButtonColor: Color
) {
  Surface(shadowElevation = Dimens.Elevation.small, color = MaterialTheme.colorScheme.surface) {
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .padding(horizontal = Dimens.Padding.medium, vertical = Dimens.Padding.small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing.small)) {
          OutlinedTextField(
              value = text,
              onValueChange = onTextChange,
              modifier = Modifier.weight(1f).testTag(ChatScreenTestTags.MESSAGE_INPUT),
              placeholder = {
                Text(
                    text = "Type your message here...",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
              },
              shape = RoundedCornerShape(Dimens.CornerRadius.pill),
              colors = MaterialTheme.customColors.outlinedTextField(),
              maxLines = 4)

          // Send button
          IconButton(
              onClick = onSendClick,
              enabled = text.isNotBlank(),
              modifier =
                  Modifier.size(Dimens.TouchTarget.minimum)
                      .background(
                          color =
                              if (text.isNotBlank()) sendButtonColor
                              else MaterialTheme.colorScheme.surfaceVariant,
                          shape = CircleShape)
                      .testTag(ChatScreenTestTags.SEND_BUTTON)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send message button",
                    tint =
                        if (text.isNotBlank()) onSendButtonColor
                        else MaterialTheme.colorScheme.onSurfaceVariant)
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
