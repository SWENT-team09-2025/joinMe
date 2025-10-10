package com.android.joinme.repository

import com.android.joinme.model.group.Group
import com.google.firebase.firestore.FirebaseFirestore

class GroupRepositoryLocal(
    private val db: FirebaseFirestore
) : GroupRepository {

    private val groups: MutableList<Group> = mutableListOf()

    override suspend fun userGroups(): List<Group> {
        return groups
    }

    suspend fun refreshUserGroups() {
        val remote = GroupRepositoryProvider.repository
        val recentGroups = remote.userGroups()
        groups.clear()
        groups.addAll(recentGroups)
    }

    override suspend fun leaveGroup(id: String) {
        val index = groups.indexOfFirst { it.id == id }
        if (index != -1) {
            groups.removeAt(index)
        } else {
            throw Exception("GroupRepositoryLocal: Group not found")
        }
    }

//    override suspend fun shareLink(id: String): String {
//        TODO("Not yet implemented")
//    }

    override suspend fun getGroup(id: String): Group? {
        return groups[id.toInt()]
    }
}