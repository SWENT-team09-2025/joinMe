package com.android.joinme.repository

import com.android.joinme.model.group.Group

interface GroupRepository {
  suspend fun userGroups(): List<Group>

  // suspend fun refreshUserGroups()

  suspend fun leaveGroup(id: String)

  //    suspend fun shareLink(id: String): String

  suspend fun getGroup(id: String): Group?
}
