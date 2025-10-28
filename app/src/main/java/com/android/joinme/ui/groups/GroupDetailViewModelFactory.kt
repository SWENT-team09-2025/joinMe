package com.android.joinme.ui.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.android.joinme.model.groups.GroupRepository
import com.android.joinme.model.groups.GroupRepositoryProvider
import com.android.joinme.model.profile.ProfileRepository
import com.android.joinme.model.profile.ProfileRepositoryProvider

/**
 * Factory for creating GroupDetailViewModel with repository dependencies.
 *
 * This factory follows the same pattern as other ViewModels in the codebase,
 * using repository providers for dependency injection.
 *
 * @property groupRepository Repository for group data operations.
 * @property profileRepository Repository for profile data operations.
 */
class GroupDetailViewModelFactory(
    private val groupRepository: GroupRepository = GroupRepositoryProvider.repository,
    private val profileRepository: ProfileRepository = ProfileRepositoryProvider.repository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GroupDetailViewModel::class.java)) {
            return GroupDetailViewModel(groupRepository, profileRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}