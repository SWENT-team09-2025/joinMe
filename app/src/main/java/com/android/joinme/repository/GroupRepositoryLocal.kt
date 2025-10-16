package com.android.joinme.repository

import com.android.joinme.model.group.Group

class GroupRepositoryLocal : GroupRepository {

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

  override suspend fun getGroup(id: String): Group? = groups.find { it.id == id }

  /**
   * Test helper method to add a group to the local repository.
   *
   * @param id The unique identifier for the group.
   * @param name The name of the group.
   * @param ownerId The ID of the group owner.
   * @param description The description of the group.
   * @param memberCount The number of members in the group.
   */
  fun addTestGroup(
      id: String,
      name: String,
      ownerId: String,
      description: String = "",
      memberCount: Int = 0
  ) {
    val memberIds = List(memberCount) { "member$it" }
    groups.add(
        Group(
            id = id,
            name = name,
            ownerId = ownerId,
            description = description,
            memberIds = memberIds))
  }

  /** Test helper method to clear all groups from the local repository. */
  fun clearAllGroups() {
    groups.clear()
  }
}
