package com.android.joinme.ui.chat

// Implemented with help of Claude AI, essentially rewritten for clarity, structure and
// documentation
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.joinme.model.chat.ChatRepository
import com.android.joinme.model.chat.Message
import com.android.joinme.model.chat.MessageType
import com.android.joinme.model.profile.Profile
import com.android.joinme.model.profile.ProfileRepository
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/**
 * Represents the UI state for the Chat screen.
 *
 * @property messages The list of messages in the current chat.
 * @property isLoading Indicates whether the screen is currently loading data.
 * @property errorMsg An error message to be shown when operations fail.
 * @property currentUserId The ID of the current user viewing the chat.
 * @property senderProfiles A map of sender IDs to their complete Profile objects.
 * @property isUploadingImage Indicates whether an image is currently being uploaded.
 * @property imageUploadError An error message specific to image upload failures.
 */
data class ChatUIState(
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = true,
    val errorMsg: String? = null,
    val currentUserId: String = "",
    val senderProfiles: Map<String, Profile> = emptyMap(),
    val isUploadingImage: Boolean = false,
    val imageUploadError: String? = null
)

/**
 * ViewModel for the Chat screen.
 *
 * Responsible for managing the UI state by fetching and providing Message items via the
 * [ChatRepository]. Handles sending messages, editing, deleting, and marking messages as read. Also
 * fetches profile photos for message senders.
 *
 * @property chatRepository The repository used to fetch and manage Message items.
 * @property profileRepository The repository used to fetch user profile information.
 */
class ChatViewModel(
    private val chatRepository: ChatRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

  private val _uiState = MutableStateFlow(ChatUIState())
  val uiState: StateFlow<ChatUIState> = _uiState.asStateFlow()

  private var currentConversationId: String = ""

  companion object {
    private const val TAG = "ChatViewModel"
    private const val IMAGE_UPLOAD_TIMEOUT_MS = 30000L // 30 seconds
  }

  /**
   * Initializes the chat by loading messages for the specified chat ID.
   *
   * @param chatId The unique identifier of the chat to load.
   * @param userId The ID of the current user.
   */
  fun initializeChat(chatId: String, userId: String) {
    currentConversationId = chatId
    _uiState.value = _uiState.value.copy(currentUserId = userId)
    observeMessages()
  }

  /** Clears the error message in the UI state. */
  fun clearErrorMsg() {
    _uiState.value = _uiState.value.copy(errorMsg = null)
  }

  /** Sets an error message in the UI state. */
  private fun setErrorMsg(errorMsg: String) {
    _uiState.value = _uiState.value.copy(errorMsg = errorMsg)
  }

  /** Observes messages for the current chat and updates the UI state in real-time. */
  private fun observeMessages() {
    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isLoading = true)
      chatRepository
          .observeMessagesForConversation(currentConversationId)
          .catch { e ->
            Log.e(TAG, "Error observing messages", e)
            setErrorMsg("Failed to load messages: ${e.message}")
            _uiState.value = _uiState.value.copy(isLoading = false)
          }
          .collect { messageList ->
            // Sort messages by timestamp (oldest first, newest last)
            val sortedMessages = messageList.sortedBy { it.timestamp }
            _uiState.value = _uiState.value.copy(messages = sortedMessages, isLoading = false)

            // Fetch profile information for all unique senders
            fetchSenderProfiles(sortedMessages)
          }
    }
  }

  /**
   * Fetches profile information for all unique message senders.
   *
   * @param messages The list of messages to extract sender IDs from.
   */
  private fun fetchSenderProfiles(messages: List<Message>) {
    viewModelScope.launch {
      try {
        // Get unique sender IDs
        val senderIds = messages.map { it.senderId }.distinct()

        // Fetch profiles for all senders (similar to GroupDetailViewModel)
        val profiles =
            senderIds.mapNotNull { senderId ->
              try {
                profileRepository.getProfile(senderId)
              } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch profile for sender: $senderId", e)
                null
              }
            }

        // Create a map of sender ID to Profile
        val profilesMap = profiles.associateBy { it.uid }

        _uiState.value = _uiState.value.copy(senderProfiles = profilesMap)
      } catch (e: Exception) {
        Log.e(TAG, "Failed to fetch sender profiles", e)
        // Don't show error to user, profile photos are optional
      }
    }
  }

  /**
   * Sends a new message to the current chat.
   *
   * @param content The text content of the message.
   * @param senderName The name of the user sending the message.
   * @param type The type of message (default is TEXT).
   */
  fun sendMessage(content: String, senderName: String, type: MessageType = MessageType.TEXT) {
    if (content.isBlank() && type == MessageType.TEXT) {
      setErrorMsg("Message cannot be empty")
      return
    }

    viewModelScope.launch {
      try {
        val messageId = chatRepository.getNewMessageId()
        val message =
            Message(
                id = messageId,
                conversationId = currentConversationId,
                senderId = _uiState.value.currentUserId,
                senderName = senderName,
                content = content,
                timestamp = System.currentTimeMillis(),
                type = type)

        chatRepository.addMessage(message)
      } catch (e: Exception) {
        setErrorMsg("Failed to send message: ${e.message}")
      }
    }
  }

  /**
   * Edits an existing message.
   *
   * @param messageId The ID of the message to edit.
   * @param newContent The new content for the message.
   */
  fun editMessage(messageId: String, newContent: String) {
    if (newContent.isBlank()) {
      setErrorMsg("Message cannot be empty")
      return
    }

    viewModelScope.launch {
      try {
        // Find the message in the current UI state
        val message = _uiState.value.messages.find { it.id == messageId }
        if (message == null) {
          setErrorMsg("Message not found")
          return@launch
        }

        // Only allow editing your own messages
        if (message.senderId != _uiState.value.currentUserId) {
          setErrorMsg("You can only edit your own messages")
          return@launch
        }

        val updatedMessage = message.copy(content = newContent, isEdited = true)
        chatRepository.editMessage(currentConversationId, messageId, updatedMessage)
      } catch (e: Exception) {
        setErrorMsg("Failed to edit message: ${e.message}")
      }
    }
  }

  /**
   * Deletes a message.
   *
   * @param messageId The ID of the message to delete.
   */
  fun deleteMessage(messageId: String) {
    viewModelScope.launch {
      try {
        // Find the message in the current UI state
        val message = _uiState.value.messages.find { it.id == messageId }
        if (message == null) {
          setErrorMsg("Message not found")
          return@launch
        }

        // Only allow deleting your own messages
        if (message.senderId != _uiState.value.currentUserId) {
          setErrorMsg("You can only delete your own messages")
          return@launch
        }

        chatRepository.deleteMessage(currentConversationId, messageId)
      } catch (e: Exception) {
        setErrorMsg("Failed to delete message: ${e.message}")
      }
    }
  }

  /**
   * Marks a message as read by the current user.
   *
   * @param messageId The ID of the message to mark as read.
   */
  fun markMessageAsRead(messageId: String) {
    viewModelScope.launch {
      try {
        chatRepository.markMessageAsRead(
            currentConversationId, messageId, _uiState.value.currentUserId)
      } catch (e: Exception) {
        Log.e(TAG, "Failed to mark message as read: messageId=$messageId", e)
        // Silently fail for read receipts to avoid disrupting user experience
      }
    }
  }

  /** Marks all messages in the current chat as read by the current user. */
  fun markAllMessagesAsRead() {
    viewModelScope.launch {
      try {
        _uiState.value.messages.forEach { message ->
          if (_uiState.value.currentUserId !in message.readBy) {
            chatRepository.markMessageAsRead(
                currentConversationId, message.id, _uiState.value.currentUserId)
          }
        }
      } catch (e: Exception) {
        Log.e(TAG, "Failed to mark all messages as read", e)
        // Silently fail for read receipts
      }
    }
  }

  /**
   * Gets the unread message count for the current user.
   *
   * @return The number of messages not yet read by the current user.
   */
  fun getUnreadCount(): Int {
    return _uiState.value.messages.count { message ->
      _uiState.value.currentUserId !in message.readBy &&
          message.senderId != _uiState.value.currentUserId
    }
  }

  /** Clears the image upload error message in the UI state. */
  fun clearImageUploadError() {
    _uiState.value = _uiState.value.copy(imageUploadError = null)
  }

  /**
   * Uploads an image and sends it as a message in the chat.
   *
   * This method handles the entire flow:
   * 1. Uploads the image to Firebase Storage
   * 2. Creates a message with type IMAGE and the download URL
   * 3. Adds the message to the chat
   *
   * Error handling: Errors are communicated via the onError callback. The UI state's
   * imageUploadError field is updated as well for observability, but the primary error notification
   * mechanism is the onError callback.
   *
   * @param context Android context for accessing content resolver.
   * @param imageUri The URI of the image to upload (from camera or gallery).
   * @param senderName The name of the user sending the image.
   * @param onSuccess Callback invoked when the image is successfully uploaded and sent.
   * @param onError Callback invoked if the upload fails, with an error message.
   */
  fun uploadAndSendImage(
      context: Context,
      imageUri: Uri,
      senderName: String,
      onSuccess: () -> Unit = {},
      onError: (String) -> Unit = {}
  ) {
    viewModelScope.launch {
      try {
        _uiState.value = _uiState.value.copy(isUploadingImage = true, imageUploadError = null)

        // Generate message ID first (before upload).
        // Note: If upload fails, this ID won't be used, which is acceptable.
        // Firebase push keys are infinite and cheap, but gaps in sequences may occur during
        // debugging.
        val messageId = chatRepository.getNewMessageId()

        // Upload image with timeout
        val downloadUrl =
            withTimeout(IMAGE_UPLOAD_TIMEOUT_MS) {
              chatRepository.uploadChatImage(context, currentConversationId, messageId, imageUri)
            }

        // Create and send the image message
        val message =
            Message(
                id = messageId,
                conversationId = currentConversationId,
                senderId = _uiState.value.currentUserId,
                senderName = senderName,
                content = downloadUrl,
                timestamp = System.currentTimeMillis(),
                type = MessageType.IMAGE)

        chatRepository.addMessage(message)

        onSuccess()
      } catch (e: TimeoutCancellationException) {
        val errorMsg = "Image upload timeout. Please check your connection and try again."
        Log.e(TAG, errorMsg, e)
        _uiState.value = _uiState.value.copy(imageUploadError = errorMsg)
        onError(errorMsg)
      } catch (e: Exception) {
        val errorMsg = "Failed to upload image: ${e.message}"
        Log.e(TAG, errorMsg, e)
        _uiState.value = _uiState.value.copy(imageUploadError = errorMsg)
        onError(errorMsg)
      } finally {
        _uiState.value = _uiState.value.copy(isUploadingImage = false)
      }
    }
  }
}
