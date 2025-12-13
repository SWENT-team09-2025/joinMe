package com.android.joinme.ui.chat

// Implemented with help of Claude AI

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.android.joinme.R
import com.android.joinme.model.chat.Message
import com.android.joinme.model.chat.MessageType
import com.android.joinme.model.chat.Poll
import com.android.joinme.model.profile.Profile
import com.android.joinme.ui.theme.Dimens
import com.android.joinme.ui.theme.customColors
import com.android.joinme.ui.theme.getUserColor
import com.android.joinme.ui.theme.outlinedTextField
import kotlinx.coroutines.launch

private const val SENDER_NAME_ALPHA = 0.8f
private const val TIMESTAMP_ALPHA = 0.7f
private const val MESSAGE_INPUT_MAX_LINES = 4
private const val TIMESTAMP_FORMAT = "HH:mm"

/**
 * Data class representing an item that can appear in the chat timeline. This allows interleaving
 * messages and polls based on their timestamps.
 */
sealed class ChatTimelineItem {
  abstract val timestamp: Long

  data class MessageItem(val message: Message) : ChatTimelineItem() {
    override val timestamp: Long = message.timestamp
  }

  data class PollItem(val poll: Poll) : ChatTimelineItem() {
    override val timestamp: Long = poll.createdAt
  }
}

/** Configuration for the chat screen appearance and user context. */
data class ChatScreenConfig(
    val chatId: String,
    val chatTitle: String,
    val currentUserId: String,
    val currentUserName: String,
    val chatColor: Color? = null,
    val onChatColor: Color? = null
)

/** Callbacks for poll actions. */
private data class PollCallbacks(
    val onVote: (String, Int) -> Unit,
    val onClosePoll: (String) -> Unit,
    val onReopenPoll: (String) -> Unit,
    val onDeletePoll: (String) -> Unit
)

/**
 * Enhanced chat screen that includes poll functionality.
 *
 * This composable extends the base chat functionality to include:
 * - Poll creation via the attachment menu
 * - Poll display in the chat timeline (interleaved with messages)
 * - Real-time poll updates and voting
 *
 * @param config Configuration for the chat screen including IDs, title, and colors
 * @param chatViewModel The ViewModel for chat message operations
 * @param pollViewModel The ViewModel for poll operations
 * @param onLeaveClick Callback when leaving the chat
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreenWithPolls(
    config: ChatScreenConfig,
    chatViewModel: ChatViewModel,
    pollViewModel: PollViewModel,
    onLeaveClick: () -> Unit = {}
) {
  val chatUiState by chatViewModel.uiState.collectAsState()
  val pollsState by pollViewModel.pollsState.collectAsState()
  val snackbarHostState = remember { SnackbarHostState() }
  val coroutineScope = rememberCoroutineScope()
  val context = LocalContext.current

  val effectiveChatColor = config.chatColor ?: MaterialTheme.customColors.chatDefault
  val effectiveOnChatColor = config.onChatColor ?: MaterialTheme.customColors.onChatDefault

  var showPollCreationSheet by remember { mutableStateOf(false) }

  // Initialize both ViewModels
  LaunchedEffect(config.chatId, config.currentUserId) {
    chatViewModel.initializeChat(config.chatId, config.currentUserId)
    pollViewModel.initialize(config.chatId, config.currentUserId)
  }

  // Show chat error messages
  LaunchedEffect(chatUiState.errorMsg) {
    chatUiState.errorMsg?.let { errorMsg ->
      coroutineScope.launch {
        snackbarHostState.showSnackbar(errorMsg)
        chatViewModel.clearErrorMsg()
      }
    }
  }

  // Show poll error messages
  LaunchedEffect(pollsState.error) {
    pollsState.error?.let { error ->
      coroutineScope.launch {
        snackbarHostState.showSnackbar(error.getMessage(context))
        pollViewModel.clearError()
      }
    }
  }

  Scaffold(
      modifier = Modifier.fillMaxSize().testTag(ChatScreenTestTags.SCREEN),
      topBar = {
        ChatTopBarWithPolls(
            chatTitle = config.chatTitle,
            onLeaveClick = onLeaveClick,
            topBarColor = effectiveChatColor,
            onTopBarColor = effectiveOnChatColor)
      },
      snackbarHost = { SnackbarHost(snackbarHostState) },
      contentWindowInsets = WindowInsets.systemBars,
      containerColor = MaterialTheme.colorScheme.background) { paddingValues ->
        if (chatUiState.isLoading || pollsState.isLoading) {
          Box(
              modifier = Modifier.fillMaxSize().padding(paddingValues),
              contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    modifier = Modifier.testTag(ChatScreenTestTags.LOADING_INDICATOR),
                    color = effectiveChatColor)
              }
        } else {
          val pollCallbacks =
              PollCallbacks(
                  onVote = { pollId, optionId -> pollViewModel.vote(pollId, optionId) },
                  onClosePoll = { pollId -> pollViewModel.closePoll(pollId) },
                  onReopenPoll = { pollId -> pollViewModel.reopenPoll(pollId) },
                  onDeletePoll = { pollId -> pollViewModel.deletePoll(pollId) })
          ChatContentWithPolls(
              messages = chatUiState.messages,
              polls = pollsState.polls,
              currentUserId = config.currentUserId,
              senderProfiles = chatUiState.senderProfiles,
              voterProfiles = pollsState.voterProfiles,
              onSendMessage = { content ->
                chatViewModel.sendMessage(content, config.currentUserName)
              },
              pollCallbacks = pollCallbacks,
              onOpenPollCreation = { showPollCreationSheet = true },
              paddingValues = paddingValues,
              chatColor = effectiveChatColor,
              onChatColor = effectiveOnChatColor)
        }
      }

  // Poll creation sheet
  if (showPollCreationSheet) {
    PollCreationSheet(
        viewModel = pollViewModel,
        creatorName = config.currentUserName,
        onDismiss = { showPollCreationSheet = false },
        onPollCreated = { showPollCreationSheet = false })
  }
}

/** Top bar for chat with polls (reusing style from ChatScreen). */
@Composable
private fun ChatTopBarWithPolls(
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
              Text(
                  text = chatTitle,
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                  color = onTopBarColor,
                  modifier = Modifier.weight(1f).testTag(ChatScreenTestTags.TITLE))

              IconButton(
                  onClick = onLeaveClick,
                  modifier =
                      Modifier.size(Dimens.IconSize.medium)
                          .background(topBarColor.copy(alpha = SENDER_NAME_ALPHA), CircleShape)
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

/** Chat content with interleaved messages and polls. */
@Composable
private fun ChatContentWithPolls(
    messages: List<Message>,
    polls: List<Poll>,
    currentUserId: String,
    senderProfiles: Map<String, Profile>,
    voterProfiles: Map<String, Profile>,
    onSendMessage: (String) -> Unit,
    pollCallbacks: PollCallbacks,
    onOpenPollCreation: () -> Unit,
    paddingValues: PaddingValues,
    chatColor: Color,
    onChatColor: Color
) {
  var messageText by remember { mutableStateOf("") }
  val listState = rememberLazyListState()

  val timelineItems = rememberTimelineItems(messages, polls)

  // Auto-scroll to bottom when new items arrive
  LaunchedEffect(timelineItems.size) {
    if (timelineItems.isNotEmpty()) {
      listState.animateScrollToItem(timelineItems.size - 1)
    }
  }

  Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
    Column(modifier = Modifier.fillMaxSize()) {
      // Timeline list (messages and polls)
      ChatTimeline(
          timelineItems = timelineItems,
          listState = listState,
          currentUserId = currentUserId,
          senderProfiles = senderProfiles,
          voterProfiles = voterProfiles,
          pollCallbacks = pollCallbacks,
          modifier = Modifier.weight(1f))

      // Message input with poll support
      MessageInputWithPolls(
          text = messageText,
          onTextChange = { messageText = it },
          onSendClick = {
            if (messageText.isNotBlank()) {
              onSendMessage(messageText)
              messageText = ""
            }
          },
          onOpenPollCreation = onOpenPollCreation,
          sendButtonColor = chatColor,
          onSendButtonColor = onChatColor)
    }
  }
}

/** Creates timeline items from messages and polls. */
@Composable
private fun rememberTimelineItems(
    messages: List<Message>,
    polls: List<Poll>
): List<ChatTimelineItem> {
  val pollsMap = remember(polls) { polls.associateBy { it.id } }
  return remember(messages, pollsMap) {
    messages
        .mapNotNull { message ->
          if (message.type == MessageType.POLL) {
            pollsMap[message.content]?.let { ChatTimelineItem.PollItem(it) }
          } else {
            ChatTimelineItem.MessageItem(message)
          }
        }
        .sortedBy { it.timestamp }
  }
}

/** Displays the chat timeline with messages and polls. */
@Composable
private fun ChatTimeline(
    timelineItems: List<ChatTimelineItem>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    currentUserId: String,
    senderProfiles: Map<String, Profile>,
    voterProfiles: Map<String, Profile>,
    pollCallbacks: PollCallbacks,
    modifier: Modifier = Modifier
) {
  LazyColumn(
      modifier = modifier.fillMaxWidth().testTag(ChatScreenTestTags.MESSAGE_LIST),
      state = listState,
      contentPadding = PaddingValues(Dimens.Padding.medium),
      verticalArrangement = Arrangement.spacedBy(Dimens.Spacing.itemSpacing)) {
        if (timelineItems.isEmpty()) {
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
          items(items = timelineItems, key = { it.getKey() }) { item ->
            TimelineItemContent(
                item = item,
                currentUserId = currentUserId,
                senderProfiles = senderProfiles,
                voterProfiles = voterProfiles,
                pollCallbacks = pollCallbacks)
          }
        }
      }
}

/** Returns a unique key for this timeline item. */
private fun ChatTimelineItem.getKey(): String =
    when (this) {
      is ChatTimelineItem.MessageItem -> "msg_${message.id}"
      is ChatTimelineItem.PollItem -> "poll_${poll.id}"
    }

/** Renders the content for a single timeline item. */
@Composable
private fun TimelineItemContent(
    item: ChatTimelineItem,
    currentUserId: String,
    senderProfiles: Map<String, Profile>,
    voterProfiles: Map<String, Profile>,
    pollCallbacks: PollCallbacks
) {
  when (item) {
    is ChatTimelineItem.MessageItem -> {
      val userColors = getUserColor(item.message.senderId)
      MessageItemWithPolls(
          message = item.message,
          isCurrentUser = item.message.senderId == currentUserId,
          senderPhotoUrl = senderProfiles[item.message.senderId]?.photoUrl,
          currentUserPhotoUrl = senderProfiles[currentUserId]?.photoUrl,
          bubbleColor = userColors.first,
          onBubbleColor = userColors.second)
    }
    is ChatTimelineItem.PollItem -> {
      PollCard(
          poll = item.poll,
          currentUserId = currentUserId,
          voterProfiles = voterProfiles,
          onVote = { optionId -> pollCallbacks.onVote(item.poll.id, optionId) },
          onClosePoll = { pollCallbacks.onClosePoll(item.poll.id) },
          onReopenPoll = { pollCallbacks.onReopenPoll(item.poll.id) },
          onDeletePoll = { pollCallbacks.onDeletePoll(item.poll.id) })
    }
  }
}

/** Simplified message item for use in the mixed timeline. */
@Composable
private fun MessageItemWithPolls(
    message: Message,
    isCurrentUser: Boolean,
    senderPhotoUrl: String?,
    currentUserPhotoUrl: String?,
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
          com.android.joinme.ui.profile.ProfilePhotoImage(
              photoUrl = senderPhotoUrl,
              contentDescription = message.senderName,
              size = Dimens.IconSize.medium,
              shape = CircleShape,
              showLoadingIndicator = false,
              modifier = Modifier.align(Alignment.Bottom))
          Spacer(modifier = Modifier.size(Dimens.Spacing.small))
        }

        Surface(
            modifier =
                Modifier.weight(1f, fill = false)
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
                if (!isCurrentUser) {
                  Text(
                      text = message.senderName,
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                      color = onBubbleColor.copy(alpha = SENDER_NAME_ALPHA))
                  Spacer(modifier = Modifier.height(Dimens.Spacing.extraSmall))
                }

                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = onBubbleColor)

                Spacer(modifier = Modifier.height(Dimens.Spacing.extraSmall))

                Text(
                    text = formatMessageTimestamp(message.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = onBubbleColor.copy(alpha = TIMESTAMP_ALPHA))
              }
            }

        if (isCurrentUser) {
          Spacer(modifier = Modifier.size(Dimens.Spacing.small))
          com.android.joinme.ui.profile.ProfilePhotoImage(
              photoUrl = currentUserPhotoUrl,
              contentDescription = message.senderName,
              size = Dimens.IconSize.medium,
              shape = CircleShape,
              showLoadingIndicator = false,
              modifier = Modifier.align(Alignment.Bottom))
        }
      }
}

/** Message input with integrated poll creation support. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MessageInputWithPolls(
    text: String,
    onTextChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onOpenPollCreation: () -> Unit,
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
          // Attachment button
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

          // Text field
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
              maxLines = MESSAGE_INPUT_MAX_LINES)

          // Send/Mic button
          if (text.isEmpty()) {
            IconButton(
                onClick = { Toast.makeText(context, notImplementedMsg, Toast.LENGTH_SHORT).show() },
                modifier =
                    Modifier.size(Dimens.TouchTarget.minimum)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        .testTag(ChatScreenTestTags.MIC_BUTTON)) {
                  Icon(
                      imageVector = Icons.Default.Mic,
                      contentDescription = stringResource(R.string.record_audio),
                      tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
          } else {
            IconButton(
                onClick = onSendClick,
                modifier =
                    Modifier.size(Dimens.TouchTarget.minimum)
                        .background(sendButtonColor, CircleShape)
                        .testTag(ChatScreenTestTags.SEND_BUTTON)) {
                  Icon(
                      imageVector = Icons.AutoMirrored.Filled.Send,
                      contentDescription = stringResource(R.string.send_message),
                      tint = onSendButtonColor)
                }
          }
        }
  }

  // Attachment menu with poll option
  if (showAttachmentMenu) {
    AttachmentMenuWithPolls(
        onDismiss = { showAttachmentMenu = false },
        onPollClick = {
          showAttachmentMenu = false
          onOpenPollCreation()
        })
  }
}

/** Attachment menu that includes poll creation option. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AttachmentMenuWithPolls(onDismiss: () -> Unit, onPollClick: () -> Unit) {
  val sheetState = rememberModalBottomSheetState()
  val context = LocalContext.current
  val notImplementedMsg = stringResource(R.string.not_yet_implemented)

  ModalBottomSheet(
      onDismissRequest = onDismiss,
      sheetState = sheetState,
      modifier = Modifier.testTag(ChatScreenTestTags.ATTACHMENT_MENU)) {
        Column(modifier = Modifier.fillMaxWidth().padding(Dimens.Padding.large)) {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            // Gallery
            AttachmentOptionWithPolls(
                icon = Icons.Default.Image,
                label = stringResource(R.string.gallery),
                onClick = {
                  Toast.makeText(context, notImplementedMsg, Toast.LENGTH_SHORT).show()
                  onDismiss()
                },
                modifier = Modifier.testTag(ChatScreenTestTags.PHOTO_SOURCE_GALLERY))

            // Location
            AttachmentOptionWithPolls(
                icon = Icons.Default.LocationOn,
                label = stringResource(R.string.location),
                onClick = {
                  Toast.makeText(context, notImplementedMsg, Toast.LENGTH_SHORT).show()
                  onDismiss()
                },
                modifier = Modifier.testTag(ChatScreenTestTags.ATTACHMENT_LOCATION))

            // Poll - Now functional!
            AttachmentOptionWithPolls(
                icon = Icons.Default.BarChart,
                label = stringResource(R.string.poll),
                onClick = onPollClick,
                modifier = Modifier.testTag(ChatScreenTestTags.ATTACHMENT_POLL))
          }

          Spacer(modifier = Modifier.height(Dimens.Padding.large))
        }
      }
}

@Composable
private fun AttachmentOptionWithPolls(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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

private fun formatMessageTimestamp(timestamp: Long): String {
  val dateFormat = java.text.SimpleDateFormat(TIMESTAMP_FORMAT, java.util.Locale.getDefault())
  return dateFormat.format(java.util.Date(timestamp))
}
