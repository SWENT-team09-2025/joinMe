package com.android.joinme.ui.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.joinme.model.event.EventType
import com.android.joinme.model.groups.Group
import com.android.joinme.model.groups.GroupRepository
import com.android.joinme.model.groups.GroupRepositoryProvider
import com.android.joinme.model.profile.Profile
import com.android.joinme.model.profile.ProfileRepository
import com.android.joinme.model.profile.ProfileRepositoryProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI state for the Group Detail screen.
 *
 * @property group The current group being displayed.
 * @property members List of member profiles in the group.
 * @property isLoading Whether data is currently being loaded.
 * @property error Error message if something went wrong.
 */
data class GroupDetailUiState(
    val group: Group? = null,
    val members: List<Profile> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

/**
 * ViewModel for managing the Group Detail screen state and business logic.
 *
 * @property groupRepository Repository for group data operations.
 * @property profileRepository Repository for profile data operations.
 */
class GroupDetailViewModel(
    private val groupRepository: GroupRepository = GroupRepositoryProvider.repository,
    private val profileRepository: ProfileRepository = ProfileRepositoryProvider.repository
) : ViewModel() {

  private val _uiState = MutableStateFlow(GroupDetailUiState())
  val uiState: StateFlow<GroupDetailUiState> = _uiState.asStateFlow()

  /**
   * Loads the group details and fetches all member profiles.
   *
   * @param groupId The unique identifier of the group to load.
   */
  fun loadGroupDetails(groupId: String) {
    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true, error = null) }

      try {
        // Fetch group details
        val group = groupRepository.getGroup(groupId)

        if (group == null) {
          _uiState.update { it.copy(isLoading = false, error = "Group not found") }
          return@launch
        }

        // Fetch all member profiles
        val members = fetchMemberProfiles(group.memberIds)

        _uiState.update {
          it.copy(group = group, members = members, isLoading = false, error = null)
        }
      } catch (e: Exception) {
        _uiState.update {
          it.copy(isLoading = false, error = e.message ?: "An unexpected error occurred")
        }
      }
    }
  }

  /**
   * Fetches profiles for all members in the group.
   *
   * @param memberIds List of member user IDs.
   * @return List of profiles, excluding any that couldn't be fetched.
   */
  private suspend fun fetchMemberProfiles(memberIds: List<String>): List<Profile> {
    return memberIds.mapNotNull { memberId ->
      try {
        profileRepository.getProfile(memberId)
      } catch (e: Exception) {
        null
      }
    }
  }

  fun getCategory(): EventType {
    return _uiState.value.group?.category ?: throw IllegalArgumentException("Not a Category")
  }
}
