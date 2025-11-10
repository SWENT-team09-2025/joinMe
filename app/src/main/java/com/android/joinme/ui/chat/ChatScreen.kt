package com.android.joinme.ui.chat

// Implemented with help of Claude AI

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.android.joinme.model.chat.Message
import com.android.joinme.model.chat.MessageType
import com.android.joinme.ui.theme.BorderColor
import com.android.joinme.ui.theme.ChatAvatarBackgroundColor
import com.android.joinme.ui.theme.ChatAvatarBorderColor
import com.android.joinme.ui.theme.ChatLeaveButtonColor
import com.android.joinme.ui.theme.ChatMessageBubbleColor
import com.android.joinme.ui.theme.ChatMessageTextColor
import com.android.joinme.ui.theme.ChatTopBarColor
import com.android.joinme.ui.theme.Dimens
import com.android.joinme.ui.theme.DisabledButtonColor
import com.android.joinme.ui.theme.FocusedBorderColor
import com.android.joinme.ui.theme.JoinMeColor
import com.android.joinme.ui.theme.PlaceholderTextColor
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
  const val BACK_BUTTON = "chatBackButton"
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
    topBarColor: androidx.compose.ui.graphics.Color = ChatTopBarColor
) {
  val uiState by viewModel.uiState.collectAsState()
  val snackbarHostState = remember { SnackbarHostState() }
  val coroutineScope = rememberCoroutineScope()

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
            onLeaveClick = { /* TODO: Implement leave functionality */ },
            topBarColor = topBarColor)
      },
      snackbarHost = { SnackbarHost(snackbarHostState) },
      containerColor = MaterialTheme.colorScheme.background) { paddingValues ->
        if (uiState.isLoading) {
          Box(
              modifier = Modifier.fillMaxSize().padding(paddingValues),
              contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    modifier = Modifier.testTag(ChatScreenTestTags.LOADING_INDICATOR),
                    color = JoinMeColor)
              }
        } else {
          ChatContent(
              messages = uiState.messages,
              currentUserId = currentUserId,
              currentUserName = currentUserName,
              onSendMessage = { content -> viewModel.sendMessage(content, currentUserName) },
              paddingValues = paddingValues)
        }
      }
}

/**
 * Custom top bar for the chat screen matching Figma design.
 *
 * @param chatTitle The title to display
 * @param onBackClick Callback for back button
 * @param onLeaveClick Callback for leave button (not yet implemented)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatTopBar(
    chatTitle: String,
    onBackClick: () -> Unit,
    onLeaveClick: () -> Unit,
    topBarColor: androidx.compose.ui.graphics.Color = ChatTopBarColor
) {
  Surface(
      modifier = Modifier.fillMaxWidth().testTag(ChatScreenTestTags.TOP_BAR),
      color = topBarColor,
      shadowElevation = 4.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically) {
              // Back button
              IconButton(
                  onClick = onBackClick,
                  modifier = Modifier.testTag(ChatScreenTestTags.BACK_BUTTON)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = ChatMessageTextColor)
                  }

              // Avatar
              Box(
                  modifier =
                      Modifier.size(32.dp)
                          .background(ChatAvatarBorderColor, CircleShape)
                          .border(2.dp, ChatAvatarBorderColor, CircleShape),
                  contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Group avatar",
                        tint = topBarColor,
                        modifier = Modifier.size(20.dp))
                  }

              Spacer(modifier = Modifier.width(12.dp))

              // Title
              Text(
                  text = chatTitle,
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  color = ChatMessageTextColor,
                  modifier = Modifier.weight(1f).testTag(ChatScreenTestTags.TITLE))

              // Leave button - background color matches top bar
              IconButton(
                  onClick = onLeaveClick,
                  modifier =
                      Modifier.size(36.dp)
                          .background(topBarColor.copy(alpha = 0.8f), CircleShape)
                          .testTag(ChatScreenTestTags.LEAVE_BUTTON)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = "Leave chat",
                        tint = ChatMessageTextColor,
                        modifier = Modifier.size(20.dp))
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
 * @param onSendMessage Callback invoked when sending a new message
 * @param paddingValues Padding from the Scaffold
 */
@Composable
private fun ChatContent(
    messages: List<Message>,
    currentUserId: String,
    currentUserName: String,
    onSendMessage: (String) -> Unit,
    paddingValues: PaddingValues
) {
  var messageText by remember { mutableStateOf("") }
  val listState = rememberLazyListState()

  // Auto-scroll to bottom when new messages arrive
  LaunchedEffect(messages.size) {
    if (messages.isNotEmpty()) {
      listState.animateScrollToItem(messages.size - 1)
    }
  }

  Column(
      modifier =
          Modifier.fillMaxSize()
              .padding(paddingValues)
              .imePadding() // Push content up when keyboard appears
      ) {
        // Messages list
        LazyColumn(
            modifier =
                Modifier.weight(1f).fillMaxWidth().testTag(ChatScreenTestTags.MESSAGE_LIST),
            state = listState,
            contentPadding = PaddingValues(Dimens.Padding.medium),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
              if (messages.isEmpty()) {
                item {
                  Box(
                      modifier = Modifier.fillParentMaxSize(),
                      contentAlignment = Alignment.Center) {
                        Text(
                            text = "No messages yet. Start the conversation!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PlaceholderTextColor,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.testTag(ChatScreenTestTags.EMPTY_MESSAGE))
                      }
                }
              } else {
                items(messages, key = { it.id }) { message ->
                  MessageItem(
                      message = message, isCurrentUser = message.senderId == currentUserId)
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
            })
      }
}

/**
 * Individual message item with bubble design and avatar.
 *
 * @param message The message to display
 * @param isCurrentUser Whether the message was sent by the current user
 */
@Composable
private fun MessageItem(message: Message, isCurrentUser: Boolean) {
  Row(
      modifier =
          Modifier.fillMaxWidth()
              .padding(horizontal = Dimens.Padding.small)
              .testTag(ChatScreenTestTags.getTestTagForMessage(message.id)),
      horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start) {
        if (!isCurrentUser) {
          // Avatar for other users (on left)
          UserAvatar(modifier = Modifier.align(Alignment.Bottom))
          Spacer(modifier = Modifier.width(8.dp))
        }

        // Message bubble
        Surface(
            modifier =
                Modifier.widthIn(max = 280.dp)
                    .testTag(ChatScreenTestTags.getTestTagForMessageBubble(message.id)),
            shape =
                RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isCurrentUser) 16.dp else 4.dp,
                    bottomEnd = if (isCurrentUser) 4.dp else 16.dp),
            color = ChatMessageBubbleColor,
            shadowElevation = 2.dp) {
              Column(modifier = Modifier.padding(12.dp)) {
                // System messages
                if (message.type == MessageType.SYSTEM) {
                  Text(
                      text = message.content,
                      style = MaterialTheme.typography.bodySmall,
                      color = ChatMessageTextColor.copy(alpha = 0.9f),
                      fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                } else {
                  // Show sender name for other users
                  if (!isCurrentUser) {
                    Text(
                        text = message.senderName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = ChatMessageTextColor.copy(alpha = 0.8f))
                    Spacer(modifier = Modifier.height(4.dp))
                  }

                  // Message content
                  Text(
                      text = message.content,
                      style = MaterialTheme.typography.bodyMedium,
                      color = ChatMessageTextColor)

                  // Timestamp
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                      text = formatTimestamp(message.timestamp),
                      style = MaterialTheme.typography.labelSmall,
                      color = ChatMessageTextColor.copy(alpha = 0.6f))
                }
              }
            }

        if (isCurrentUser) {
          // Avatar for current user (on right)
          Spacer(modifier = Modifier.width(8.dp))
          UserAvatar(modifier = Modifier.align(Alignment.Bottom))
        }
      }
}

/**
 * User avatar component.
 *
 * @param modifier Modifier for positioning
 */
@Composable
private fun UserAvatar(modifier: Modifier = Modifier) {
  Box(
      modifier =
          modifier
              .size(24.dp)
              .background(ChatAvatarBackgroundColor, CircleShape)
              .border(1.5.dp, ChatAvatarBorderColor, CircleShape),
      contentAlignment = Alignment.Center) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = "User avatar",
            tint = ChatMessageTextColor,
            modifier = Modifier.size(14.dp))
      }
}

/**
 * Message input field with send button.
 *
 * @param text Current input text
 * @param onTextChange Callback when text changes
 * @param onSendClick Callback when send button is clicked
 */
@Composable
private fun MessageInput(text: String, onTextChange: (String) -> Unit, onSendClick: () -> Unit) {
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
                Text(text = "Type your message here...", color = PlaceholderTextColor)
              },
              shape = RoundedCornerShape(24.dp),
              colors =
                  OutlinedTextFieldDefaults.colors(
                      focusedBorderColor = FocusedBorderColor,
                      unfocusedBorderColor = BorderColor,
                      disabledBorderColor = BorderColor,
                      focusedTextColor = MaterialTheme.colorScheme.onSurface,
                      unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                      cursorColor = FocusedBorderColor),
              maxLines = 4)

          // Send button
          IconButton(
              onClick = onSendClick,
              enabled = text.isNotBlank(),
              modifier =
                  Modifier.size(48.dp)
                      .background(
                          color =
                              if (text.isNotBlank()) ChatMessageBubbleColor
                              else DisabledButtonColor,
                          shape = CircleShape)
                      .testTag(ChatScreenTestTags.SEND_BUTTON)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send message",
                    tint = ChatMessageTextColor)
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