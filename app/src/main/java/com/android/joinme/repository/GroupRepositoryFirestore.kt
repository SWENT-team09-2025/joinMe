package com.android.joinme.repository

import com.android.joinme.model.group.Group
import com.google.firebase.firestore.FirebaseFirestore

class GroupRepositoryFirestore(private val db: FirebaseFirestore): GroupRepository {
    override fun userGroups(): List<Group> {
        TODO("Not yet implemented")
    }

    override suspend fun refreshUserGroups() {
        TODO("Not yet implemented")
    }

    override suspend fun leaveGroup(id: String) {
        TODO("Not yet implemented")
    }

    override suspend fun shareLink(id: String): String {
        TODO("Not yet implemented")
    }

    override suspend fun getGroup(id: String): Group? {
        TODO("Not yet implemented")
    }
}