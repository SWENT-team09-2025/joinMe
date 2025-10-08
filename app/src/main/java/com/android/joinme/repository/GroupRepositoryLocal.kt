package com.android.joinme.repository

import com.android.joinme.model.group.Group

class GroupRepositoryLocal(): GroupRepository {

    private val groups: MutableList<Group> = mutableListOf()

    override fun userGroups(): List<Group> {
        return groups
    }

    override suspend fun refreshUserGroups() {
        val remote = GroupRepositoryFirestore() // Modify this line of code once I implemented the
                                                // GroupRepositoryFirestore file
        val recentGroups = remote.userGroups()
        groups.clear()
        groups.addAll(recentGroups)
    }

    override suspend fun leaveGroup(id: String) {
        val index = groups.indexOfFirst{it.id == id}
        if (index != -1){
            groups.removeAt(index)
        } else {
            throw Exception("GroupRepositoryLocal: Group not found")
        }
    }

    override suspend fun shareLink(id: String): String {
        TODO("Not yet implemented")
    }

    override suspend fun getGroup(id: String): Group? {
        return groups[id.toInt()]
    }
}