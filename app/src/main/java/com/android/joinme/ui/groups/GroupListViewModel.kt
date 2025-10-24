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
 * Represents the UI state for the Group List screen.
 *
 * @property groups The list of groups to display.
 * @property isLoading Indicates whether the screen is currently loading data.
 * @property errorMsg An error message to be shown when fetching groups fails
 */
data class GroupListUIState(
    val groups: List<Group> = emptyList(),
    val isLoading: Boolean = true,
    val errorMsg: String? = null,
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
        val allGroups = groupRepository.getAllGroups()

        _uiState.value = GroupListUIState(groups = allGroups, isLoading = false)
      } catch (e: Exception) {
        setErrorMsg("Failed to load groups: ${e.message}")
        _uiState.value = _uiState.value.copy(isLoading = false)
      }
    }
  }
}
