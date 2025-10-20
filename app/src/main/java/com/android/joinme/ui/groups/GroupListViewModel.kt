package com.android.joinme.ui.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.joinme.model.group.Group
import com.android.joinme.model.group.GroupRepository
import com.android.joinme.model.group.GroupRepositoryProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI state data class representing the state of the group list screen.
 *
 * @property groups The list of groups to display. Defaults to an empty list.
 * @property errorMsg An optional error message to display when an operation fails. Null if no
 *   error.
 * @property isLoading Indicates whether the screen is currently loading data.
 */
data class GroupListUIState(
    val groups: List<Group> = emptyList(),
    val errorMsg: String? = null,
    val isLoading: Boolean = false
)

/**
 * ViewModel for managing the group list screen state and business logic.
 *
 * This ViewModel handles fetching the current user's groups, exposing UI state via StateFlow, and
 * managing loading and error states. It follows the unidirectional data flow pattern.
 *
 * @property repo The repository used to fetch group data. Defaults to the repository provided by
 *   [GroupRepositoryProvider], but can be injected for testing purposes.
 */
class GroupListViewModel(private val repo: GroupRepository = GroupRepositoryProvider.repository) :
    ViewModel() {

  /** Mutable internal state flow for UI state updates. */
  private val _uiState = MutableStateFlow(GroupListUIState())

  /** Publicly exposed immutable state flow that the UI observes for state changes. */
  val uiState: StateFlow<GroupListUIState> = _uiState.asStateFlow()

  init {
    getAllGroups()
  }

  /**
   * Refreshes the group list by re-fetching all groups from the repository.
   *
   * This method can be called when the user performs a pull-to-refresh action or when the screen
   * needs to reload its data.
   */
  fun refreshUIState() = getAllGroups()

  /**
   * Clears any error message from the UI state.
   *
   * This is typically called after the user has acknowledged an error message.
   */
  fun clearErrorMsg() {
    _uiState.value = _uiState.value.copy(errorMsg = null)
  }

  /**
   * Fetches all groups that the current user belongs to from the repository.
   *
   * This private method handles the asynchronous operation of fetching groups, managing loading
   * states, and catching any errors that occur during the fetch operation. Errors are logged and
   * exposed via the UI state.
   */
  private fun getAllGroups() {
    viewModelScope.launch {
      try {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMsg = null)
        val groups = repo.userGroups()
        _uiState.value = _uiState.value.copy(groups = groups)
      } catch (e: Exception) {
        _uiState.value =
            _uiState.value.copy(groups = emptyList(), errorMsg = e.message ?: "Unknown error")
      } finally {
        _uiState.value = _uiState.value.copy(isLoading = false)
      }
    }
  }
}
