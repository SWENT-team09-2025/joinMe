package com.android.joinme.model.groups

/** Represents a repository that manages a local list of groups (for offline mode or testing). */
class GroupRepositoryLocal : GroupRepository {
  private val groups: MutableList<Group> = mutableListOf()
  private var counter = 0

  override fun getNewGroupId(): String {
    return (counter++).toString()
  }

  override suspend fun getAllGroups(): List<Group> {
    return groups
  }

  override suspend fun getGroup(groupId: String): Group {
    return groups.find { it.id == groupId }
        ?: throw Exception("GroupRepositoryLocal: Group not found")
  }

  override suspend fun addGroup(group: Group) {
    groups.add(group)
  }

  override suspend fun editGroup(groupId: String, newValue: Group) {
    val index = groups.indexOfFirst { it.id == groupId }
    if (index != -1) {
      groups[index] = newValue
    } else {
      throw Exception("GroupRepositoryLocal: Group not found")
    }
  }

  override suspend fun deleteGroup(groupId: String, userId: String) {
    val group = getGroup(groupId)

    if (group.ownerId != userId) {
      throw Exception("GroupRepositoryLocal: Only the group owner can delete this group")
    }

    val index = groups.indexOfFirst { it.id == groupId }
    if (index != -1) {
      groups.removeAt(index)
    } else {
      throw Exception("GroupRepositoryLocal: Group not found")
    }
  }

  override suspend fun leaveGroup(groupId: String, userId: String) {
    val group = getGroup(groupId)
    val updatedMemberIds = group.memberIds.filter { it != userId }

    if (updatedMemberIds.size == group.memberIds.size) {
      throw Exception("GroupRepositoryLocal: User is not a member of this group")
    }

    val updatedGroup = group.copy(memberIds = updatedMemberIds)
    editGroup(groupId, updatedGroup)
  }

  override suspend fun joinGroup(groupId: String, userId: String) {
    val group = getGroup(groupId)

    if (group.memberIds.contains(userId)) {
      throw Exception("GroupRepositoryLocal: User is already a member of this group")
    }

    val updatedMemberIds = group.memberIds + userId
    val updatedGroup = group.copy(memberIds = updatedMemberIds)
    editGroup(groupId, updatedGroup)
  }
}
