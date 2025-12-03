package com.android.joinme.ui.chat

// Implemented with help of Claude AI

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.joinme.model.presence.PresenceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * Represents the UI state for presence (online users) tracking.
 *
 * @property onlineUsersCount The number of other users currently online in the chat.
 * @property onlineUserIds List of user IDs who are currently online.
 * @property isLoading Whether presence data is still loading.
 */
data class PresenceUIState(
    val onlineUsersCount: Int = 0,
    val onlineUserIds: List<String> = emptyList(),
    val isLoading: Boolean = true
)

/**
 * ViewModel for managing presence (online status) tracking in chat.
 *
 * This ViewModel observes the online users in a specific chat context and provides real-time
 * updates to the UI. It follows the MVVM architecture pattern and uses Kotlin Flows for reactive
 * updates.
 *
 * @property presenceRepository The repository used to observe online users.
 */
class PresenceViewModel(private val presenceRepository: PresenceRepository) : ViewModel() {

  private val _presenceState = MutableStateFlow(PresenceUIState())
  val presenceState: StateFlow<PresenceUIState> = _presenceState.asStateFlow()

  private var currentContextId: String = ""
  private var currentUserId: String = ""

  companion object {
    private const val TAG = "PresenceViewModel"
  }

  /**
   * Initializes presence tracking for a specific chat context.
   *
   * This should be called when the chat screen is opened. It starts observing the online users
   * count in the specified chat.
   *
   * @param contextId The unique identifier of the chat context (e.g., chatId).
   * @param userId The ID of the current user (excluded from online count).
   */
  fun initialize(contextId: String, userId: String) {
    if (contextId.isBlank() || userId.isBlank()) {
      Log.w(TAG, "initialize called with blank contextId or userId")
      return
    }

    // Avoid re-initialization if already tracking the same context
    if (currentContextId == contextId && currentUserId == userId) {
      return
    }

    currentContextId = contextId
    currentUserId = userId
    _presenceState.value = PresenceUIState(isLoading = true)

    observeOnlineUsers()
  }

  /** Observes the online users count for the current chat context. */
  private fun observeOnlineUsers() {
    viewModelScope.launch {
      presenceRepository
          .observeOnlineUsersCount(currentContextId, currentUserId)
          .catch { e ->
            Log.e(TAG, "Error observing online users count", e)
            _presenceState.value =
                _presenceState.value.copy(onlineUsersCount = 0, isLoading = false)
          }
          .collect { count ->
            _presenceState.value =
                _presenceState.value.copy(onlineUsersCount = count, isLoading = false)
          }
    }

    // Also observe the list of online user IDs for potential future use
    viewModelScope.launch {
      presenceRepository
          .observeOnlineUserIds(currentContextId, currentUserId)
          .catch { e ->
            Log.e(TAG, "Error observing online user IDs", e)
            _presenceState.value = _presenceState.value.copy(onlineUserIds = emptyList())
          }
          .collect { userIds ->
            _presenceState.value = _presenceState.value.copy(onlineUserIds = userIds)
          }
    }
  }

  /**
   * Gets the formatted online status text.
   *
   * @return A human-readable string describing the online status.
   */
  fun getOnlineStatusText(): String {
    val count = _presenceState.value.onlineUsersCount
    return when {
      _presenceState.value.isLoading -> ""
      count == 0 -> ""
      count == 1 -> "1 online"
      else -> "$count online"
    }
  }
}
