// Implemented with help of Claude AI
package com.android.joinme.ui.groups

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.joinme.model.groups.Group
import com.android.joinme.model.groups.GroupRepository
import com.android.joinme.model.groups.GroupRepositoryProvider
import com.android.joinme.model.groups.streaks.StreakService
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val ERROR_USER_NOT_AUTHENTICATED = "User not authenticated"

/**
 * Represents the UI state for the Group List screen.
 *
 * @property groups The list of groups to display.
 * @property isLoading Indicates whether the screen is currently loading data.
 * @property errorMsg An error message to be shown when fetching groups fails
 * @property currentUserId The ID of the currently authenticated user
 */
data class GroupListUIState(
    val groups: List<Group> = emptyList(),
    val isLoading: Boolean = true,
    val errorMsg: String? = null,
    val currentUserId: String? = null
)

/**
 * ViewModel for the Group List screen.
 *
 * Responsible for managing the UI state, by fetching and providing Group items via the
 * [GroupRepository].
 *
 * @property groupRepository The repository used to fetch and manage Group items.
 */
class GroupListViewModel(
    private val groupRepository: GroupRepository = GroupRepositoryProvider.repository
) : ViewModel() {

  private val _uiState = MutableStateFlow(GroupListUIState())
  val uiState: StateFlow<GroupListUIState> = _uiState.asStateFlow()

  init {
    getAllGroups()
  }

  /** Clears the error message in the UI state. */
  fun clearErrorMsg() {
    _uiState.value = _uiState.value.copy(errorMsg = null)
  }

  /** Sets an error message in the UI state. */
  private fun setErrorMsg(errorMsg: String) {
    _uiState.value = _uiState.value.copy(errorMsg = errorMsg)
  }

  /** Refreshes the UI state by fetching all Group items from the repository. */
  fun refreshUIState() {
    getAllGroups()
  }

  /** Fetches all groups from the repository and updates the UI state. */
  private fun getAllGroups() {
    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isLoading = true)
      try {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        val allGroups = groupRepository.getAllGroups()

        _uiState.value =
            GroupListUIState(groups = allGroups, isLoading = false, currentUserId = currentUserId)
      } catch (e: Exception) {
        setErrorMsg("Failed to load groups: ${e.message}")
        _uiState.value = _uiState.value.copy(isLoading = false)
      }
    }
  }

  /**
   * Deletes a group from the repository and refreshes the UI state.
   *
   * @param groupId The ID of the group to delete.
   * @param onSuccess Callback invoked when the group is successfully deleted.
   * @param onError Callback invoked when deletion fails, receives error message.
   */
  fun deleteGroup(groupId: String, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
    viewModelScope.launch {
      try {
        val currentUserId =
            FirebaseAuth.getInstance().currentUser?.uid
                ?: throw Exception(ERROR_USER_NOT_AUTHENTICATED)

        groupRepository.deleteGroup(groupId, currentUserId)
        refreshUIState()
        onSuccess()
      } catch (e: Exception) {
        val errorMsg = "Failed to delete group: ${e.message}"
        setErrorMsg(errorMsg)
        onError(errorMsg)
      }
    }
  }

  /**
   * Removes the current user from a group and refreshes the UI state.
   *
   * Also deletes the user's streak data for that group.
   *
   * @param groupId The ID of the group to leave.
   * @param onSuccess Callback invoked when the user successfully leaves the group.
   * @param onError Callback invoked when leaving fails, receives error message.
   */
  fun leaveGroup(groupId: String, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
    viewModelScope.launch {
      try {
        val currentUserId =
            FirebaseAuth.getInstance().currentUser?.uid
                ?: throw Exception(ERROR_USER_NOT_AUTHENTICATED)

        groupRepository.leaveGroup(groupId, currentUserId)

        // Delete the user's streak for this group
        try {
          StreakService.onUserLeftGroup(groupId, currentUserId)
        } catch (e: Exception) {
          Log.e("GroupListViewModel", "Error deleting streak for user $currentUserId", e)
          // Non-critical: don't fail leave operation if streak deletion fails
        }

        refreshUIState()
        onSuccess()
      } catch (e: Exception) {
        val errorMsg = "Failed to leave group: ${e.message}"
        setErrorMsg(errorMsg)
        onError(errorMsg)
      }
    }
  }

  /**
   * Adds the current user to a group and refreshes the UI state.
   *
   * @param groupId The ID of the group to join.
   * @param onSuccess Callback invoked when the user successfully joins the group.
   * @param onError Callback invoked when joining fails, receives error message.
   */
  fun joinGroup(groupId: String, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
    viewModelScope.launch {
      try {
        val currentUserId =
            FirebaseAuth.getInstance().currentUser?.uid
                ?: throw Exception(ERROR_USER_NOT_AUTHENTICATED)

        groupRepository.joinGroup(groupId, currentUserId)
        refreshUIState()
        onSuccess()
      } catch (e: Exception) {
        val errorMsg = "Failed to join group: ${e.message}"
        setErrorMsg(errorMsg)
        onError(errorMsg)
      }
    }
  }
}
