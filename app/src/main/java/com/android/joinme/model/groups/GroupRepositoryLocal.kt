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

  override suspend fun deleteGroup(groupId: String) {
    val index = groups.indexOfFirst { it.id == groupId }
    if (index != -1) {
      groups.removeAt(index)
    } else {
      throw Exception("GroupRepositoryLocal: Group not found")
    }
  }
}
