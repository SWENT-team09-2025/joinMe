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
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Send
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
import com.android.joinme.ui.theme.Dimens
import com.android.joinme.ui.theme.customColors
import com.android.joinme.ui.theme.sportsContainerLight
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
    topBarColor: androidx.compose.ui.graphics.Color = MaterialTheme.customColors.sportsContainer
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
            onLeaveClick = { /* TODO: Implement leave functionality */},
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
                    color = MaterialTheme.colorScheme.primary)
              }
        } else {
          ChatContent(
              messages = uiState.messages,
              currentUserId = currentUserId,
              currentUserName = currentUserName,
              onSendMessage = { content -> viewModel.sendMessage(content, currentUserName) },
              paddingValues = paddingValues,
              categoryColor = topBarColor)
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
    topBarColor: androidx.compose.ui.graphics.Color = sportsContainerLight
) {
  Surface(
      modifier = Modifier.fillMaxWidth().testTag(ChatScreenTestTags.TOP_BAR),
      color = topBarColor,
      shadowElevation = 4.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically) {
              // Avatar
              Box(
                  modifier =
                      Modifier.size(32.dp)
                          .background(MaterialTheme.colorScheme.surface, CircleShape)
                          .border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape),
                  contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Group avatar",
                        tint = topBarColor,
                        modifier = Modifier.size(20.dp))
                  }

              Spacer(modifier = Modifier.width(16.dp))

              // Title
              Text(
                  text = chatTitle,
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface,
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
                        tint = MaterialTheme.colorScheme.onSurface,
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
 * @param categoryColor The category color for message bubbles and send button
 */
@Composable
private fun ChatContent(
    messages: List<Message>,
    currentUserId: String,
    currentUserName: String,
    onSendMessage: (String) -> Unit,
    paddingValues: PaddingValues,
    categoryColor: androidx.compose.ui.graphics.Color
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
            modifier = Modifier.weight(1f).fillMaxWidth().testTag(ChatScreenTestTags.MESSAGE_LIST),
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.testTag(ChatScreenTestTags.EMPTY_MESSAGE))
                      }
                }
              } else {
                items(messages, key = { it.id }) { message ->
                  MessageItem(
                      message = message,
                      isCurrentUser = message.senderId == currentUserId,
                      bubbleColor = categoryColor)
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
            sendButtonColor = categoryColor)
      }
}

/**
 * Individual message item with bubble design and avatar.
 *
 * @param message The message to display
 * @param isCurrentUser Whether the message was sent by the current user
 * @param bubbleColor The color for the message bubble
 */
@Composable
private fun MessageItem(
    message: Message,
    isCurrentUser: Boolean,
    bubbleColor: androidx.compose.ui.graphics.Color
) {
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
            color = bubbleColor,
            shadowElevation = 2.dp) {
              Column(modifier = Modifier.padding(12.dp)) {
                // System messages
                if (message.type == MessageType.SYSTEM) {
                  Text(
                      text = message.content,
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                      fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                } else {
                  // Show sender name for other users
                  if (!isCurrentUser) {
                    Text(
                        text = message.senderName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                    Spacer(modifier = Modifier.height(4.dp))
                  }

                  // Message content
                  Text(
                      text = message.content,
                      style = MaterialTheme.typography.bodyMedium,
                      color = MaterialTheme.colorScheme.onSurface)

                  // Timestamp
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                      text = formatTimestamp(message.timestamp),
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
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
              .background(MaterialTheme.colorScheme.surface, CircleShape)
              .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape),
      contentAlignment = Alignment.Center) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = "User avatar",
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(14.dp))
      }
}

/**
 * Message input field with send button.
 *
 * @param text Current input text
 * @param onTextChange Callback when text changes
 * @param onSendClick Callback when send button is clicked
 * @param sendButtonColor The color for the send button
 */
@Composable
private fun MessageInput(
    text: String,
    onTextChange: (String) -> Unit,
    onSendClick: () -> Unit,
    sendButtonColor: androidx.compose.ui.graphics.Color
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
              shape = RoundedCornerShape(24.dp),
              colors =
                  OutlinedTextFieldDefaults.colors(
                      focusedBorderColor = MaterialTheme.colorScheme.primary,
                      unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                      disabledBorderColor = MaterialTheme.colorScheme.outline,
                      focusedTextColor = MaterialTheme.colorScheme.onSurface,
                      unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                      cursorColor = MaterialTheme.colorScheme.primary),
              maxLines = 4)

          // Send button
          IconButton(
              onClick = onSendClick,
              enabled = text.isNotBlank(),
              modifier =
                  Modifier.size(48.dp)
                      .background(
                          color =
                              if (text.isNotBlank()) sendButtonColor
                              else MaterialTheme.colorScheme.surfaceVariant,
                          shape = CircleShape)
                      .testTag(ChatScreenTestTags.SEND_BUTTON)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send message",
                    tint = MaterialTheme.colorScheme.onSurface)
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

/**
 * Helper composable for preview that bypasses ViewModel initialization.
 *
 * This is used for previews to avoid ViewModel/coroutine issues in preview mode.
 */
@Composable
private fun ChatScreenContent(
    chatTitle: String,
    messages: List<Message>,
    currentUserId: String,
    currentUserName: String,
    onSendMessage: (String) -> Unit,
    onBackClick: () -> Unit,
    topBarColor: androidx.compose.ui.graphics.Color
) {
  Scaffold(
      modifier = Modifier.fillMaxSize(),
      topBar = {
        ChatTopBar(
            chatTitle = chatTitle,
            onBackClick = onBackClick,
            onLeaveClick = {},
            topBarColor = topBarColor)
      },
      containerColor = MaterialTheme.colorScheme.background) { paddingValues ->
        ChatContent(
            messages = messages,
            currentUserId = currentUserId,
            currentUserName = currentUserName,
            onSendMessage = onSendMessage,
            paddingValues = paddingValues,
            categoryColor = topBarColor)
      }
}

// Light Mode Previews
@androidx.compose.ui.tooling.preview.Preview(
    showBackground = true,
    name = "Sports Chat - Light",
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun ChatScreenPreviewSportsLight() {
  com.android.joinme.ui.theme.JoinMeTheme {
    val previewMessages =
        listOf(
            Message(
                id = "1",
                conversationId = "preview-chat-sports",
                senderId = "user2",
                senderName = "Alice",
                content = "Hey! Ready for the game?",
                timestamp = System.currentTimeMillis() - 3600000,
                type = MessageType.TEXT),
            Message(
                id = "2",
                conversationId = "preview-chat-sports",
                senderId = "user1",
                senderName = "John Doe",
                content = "Yes! Can't wait!",
                timestamp = System.currentTimeMillis() - 1800000,
                type = MessageType.TEXT))

    ChatScreenContent(
        chatTitle = "Basketball Game",
        messages = previewMessages,
        currentUserId = "user1",
        currentUserName = "John Doe",
        onSendMessage = {},
        onBackClick = {},
        topBarColor = MaterialTheme.customColors.sportsContainer)
  }
}

@androidx.compose.ui.tooling.preview.Preview(
    showBackground = true,
    name = "Activity Chat - Light",
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun ChatScreenPreviewActivityLight() {
  com.android.joinme.ui.theme.JoinMeTheme {
    val previewMessages =
        listOf(
            Message(
                id = "1",
                conversationId = "preview-chat-activity",
                senderId = "system",
                senderName = "System",
                content = "Bob joined the chat",
                timestamp = System.currentTimeMillis() - 3600000,
                type = MessageType.SYSTEM),
            Message(
                id = "2",
                conversationId = "preview-chat-activity",
                senderId = "user2",
                senderName = "Bob",
                content = "What time should we meet?",
                timestamp = System.currentTimeMillis() - 7200000,
                type = MessageType.TEXT),
            Message(
                id = "3",
                conversationId = "preview-chat-activity",
                senderId = "user1",
                senderName = "John Doe",
                content = "How about 8 AM at the trailhead?",
                timestamp = System.currentTimeMillis() - 10800000,
                type = MessageType.TEXT))

    ChatScreenContent(
        chatTitle = "Hiking Trip",
        messages = previewMessages,
        currentUserId = "user1",
        currentUserName = "John Doe",
        onSendMessage = {},
        onBackClick = {},
        topBarColor = MaterialTheme.customColors.activityContainer)
  }
}

@androidx.compose.ui.tooling.preview.Preview(
    showBackground = true,
    name = "Social Chat - Light",
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun ChatScreenPreviewSocialLight() {
  com.android.joinme.ui.theme.JoinMeTheme {
    val previewMessages =
        listOf(
            Message(
                id = "1",
                conversationId = "preview-chat-social",
                senderId = "user2",
                senderName = "Emma",
                content = "Looking forward to meeting everyone!",
                timestamp = System.currentTimeMillis() - 14400000,
                type = MessageType.TEXT),
            Message(
                id = "2",
                conversationId = "preview-chat-social",
                senderId = "user3",
                senderName = "Sarah",
                content = "Me too! Should we order something in advance?",
                timestamp = System.currentTimeMillis() - 10800000,
                type = MessageType.TEXT),
            Message(
                id = "3",
                conversationId = "preview-chat-social",
                senderId = "user1",
                senderName = "John Doe",
                content = "Good idea! I'll have a cappuccino",
                timestamp = System.currentTimeMillis() - 7200000,
                type = MessageType.TEXT))

    ChatScreenContent(
        chatTitle = "Coffee Meetup",
        messages = previewMessages,
        currentUserId = "user1",
        currentUserName = "John Doe",
        onSendMessage = {},
        onBackClick = {},
        topBarColor = MaterialTheme.customColors.socialContainer)
  }
}

// Dark Mode Previews
@androidx.compose.ui.tooling.preview.Preview(
    showBackground = true,
    name = "Sports Chat - Dark",
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ChatScreenPreviewSportsDark() {
  com.android.joinme.ui.theme.JoinMeTheme {
    val previewMessages =
        listOf(
            Message(
                id = "1",
                conversationId = "preview-chat-sports",
                senderId = "user2",
                senderName = "Alice",
                content = "Hey! Ready for the game?",
                timestamp = System.currentTimeMillis() - 3600000,
                type = MessageType.TEXT),
            Message(
                id = "2",
                conversationId = "preview-chat-sports",
                senderId = "user1",
                senderName = "John Doe",
                content = "Yes! Can't wait!",
                timestamp = System.currentTimeMillis() - 1800000,
                type = MessageType.TEXT))

    ChatScreenContent(
        chatTitle = "Basketball Game",
        messages = previewMessages,
        currentUserId = "user1",
        currentUserName = "John Doe",
        onSendMessage = {},
        onBackClick = {},
        topBarColor = MaterialTheme.customColors.sportsContainer)
  }
}

@androidx.compose.ui.tooling.preview.Preview(
    showBackground = true,
    name = "Activity Chat - Dark",
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ChatScreenPreviewActivityDark() {
  com.android.joinme.ui.theme.JoinMeTheme {
    val previewMessages =
        listOf(
            Message(
                id = "1",
                conversationId = "preview-chat-activity",
                senderId = "system",
                senderName = "System",
                content = "Bob joined the chat",
                timestamp = System.currentTimeMillis() - 3600000,
                type = MessageType.SYSTEM),
            Message(
                id = "2",
                conversationId = "preview-chat-activity",
                senderId = "user2",
                senderName = "Bob",
                content = "What time should we meet?",
                timestamp = System.currentTimeMillis() - 7200000,
                type = MessageType.TEXT),
            Message(
                id = "3",
                conversationId = "preview-chat-activity",
                senderId = "user1",
                senderName = "John Doe",
                content = "How about 8 AM at the trailhead?",
                timestamp = System.currentTimeMillis() - 10800000,
                type = MessageType.TEXT))

    ChatScreenContent(
        chatTitle = "Hiking Trip",
        messages = previewMessages,
        currentUserId = "user1",
        currentUserName = "John Doe",
        onSendMessage = {},
        onBackClick = {},
        topBarColor = MaterialTheme.customColors.activityContainer)
  }
}

@androidx.compose.ui.tooling.preview.Preview(
    showBackground = true,
    name = "Social Chat - Dark",
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ChatScreenPreviewSocialDark() {
  com.android.joinme.ui.theme.JoinMeTheme {
    val previewMessages =
        listOf(
            Message(
                id = "1",
                conversationId = "preview-chat-social",
                senderId = "user2",
                senderName = "Emma",
                content = "Looking forward to meeting everyone!",
                timestamp = System.currentTimeMillis() - 14400000,
                type = MessageType.TEXT),
            Message(
                id = "2",
                conversationId = "preview-chat-social",
                senderId = "user3",
                senderName = "Sarah",
                content = "Me too! Should we order something in advance?",
                timestamp = System.currentTimeMillis() - 10800000,
                type = MessageType.TEXT),
            Message(
                id = "3",
                conversationId = "preview-chat-social",
                senderId = "user1",
                senderName = "John Doe",
                content = "Good idea! I'll have a cappuccino",
                timestamp = System.currentTimeMillis() - 7200000,
                type = MessageType.TEXT))

    ChatScreenContent(
        chatTitle = "Coffee Meetup",
        messages = previewMessages,
        currentUserId = "user1",
        currentUserName = "John Doe",
        onSendMessage = {},
        onBackClick = {},
        topBarColor = MaterialTheme.customColors.socialContainer)
  }
}

@androidx.compose.ui.tooling.preview.Preview(
    showBackground = true,
    name = "Empty Chat - Light",
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun ChatScreenPreviewEmptyLight() {
  com.android.joinme.ui.theme.JoinMeTheme {
    ChatScreenContent(
        chatTitle = "New Group Chat",
        messages = emptyList(),
        currentUserId = "user1",
        currentUserName = "John Doe",
        onSendMessage = {},
        onBackClick = {},
        topBarColor = MaterialTheme.customColors.sportsContainer)
  }
}

@androidx.compose.ui.tooling.preview.Preview(
    showBackground = true,
    name = "Empty Chat - Dark",
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ChatScreenPreviewEmptyDark() {
  com.android.joinme.ui.theme.JoinMeTheme {
    ChatScreenContent(
        chatTitle = "New Group Chat",
        messages = emptyList(),
        currentUserId = "user1",
        currentUserName = "John Doe",
        onSendMessage = {},
        onBackClick = {},
        topBarColor = MaterialTheme.customColors.sportsContainer)
  }
}
