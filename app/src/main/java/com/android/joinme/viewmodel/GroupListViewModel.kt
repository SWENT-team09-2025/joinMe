package com.android.joinme.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.joinme.model.group.Group
import com.android.joinme.repository.GroupRepositoryFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class GroupListUIState(
    val groups: List<Group> = emptyList(),
    val errorMsg: String? = null,
    val isLoading: Boolean = false
)

class GroupListViewModel(
    private val groupRepository:
    GroupRepositoryFirestore = GroupRepositoryFirestore() // Modify this line of code once I
                                                          // implemented the
                                                          // GroupRepositoryFirestore file
): ViewModel()  {

    private val _uiState = MutableStateFlow(GroupListUIState())
    val uiState: StateFlow<GroupListUIState> = _uiState.asStateFlow()

    init {
        getAllGroups()
    }

    fun refreshUIState() {
        getAllGroups()
    }

    fun clearErrorMsg() {
        _uiState.value = _uiState.value.copy(errorMsg = null)
    }

    private fun getAllGroups() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)

                val groups = groupRepository.userGroups("")

                _uiState.value = GroupListUIState(
                    groups = groups,
                    isLoading = false
                )
            } catch (e: Exception) {
                Log.e("GroupListViewModel", "Error fetching groups", e)
                _uiState.value = GroupListUIState(
                    errorMsg = "Failed to load groups: ${e.message}",
                    isLoading = false
                )
            }
        }
    }
}