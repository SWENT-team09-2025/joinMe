package com.android.joinme.model.group

/**
 * Local in-memory implementation of [GroupRepository].
 *
 * This repository stores groups in a mutable list, providing a lightweight, non-persistent storage
 * solution. It's primarily used for:
 * - Local caching of groups fetched from remote sources
 * - Testing without requiring network access or Firebase setup
 * - Offline-first functionality (when combined with sync mechanisms)
 *
 * This implementation does not persist data across app restarts. All data is lost when the app
 * process terminates.
 */
class GroupRepositoryLocal : GroupRepository {

  /** In-memory storage for group objects. */
  private val groups: MutableList<Group> = mutableListOf()

  override suspend fun userGroups(): List<Group> {
    return groups
  }

  /**
   * Refreshes the local group cache by fetching fresh data from the remote repository.
   *
   * This method clears all locally stored groups and replaces them with the latest data from the
   * provider's repository (typically Firestore). Use this to sync local state with remote data.
   *
   * @throws Exception if the remote fetch operation fails.
   */
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

  override suspend fun getGroup(id: String): Group? = groups.find { it.id == id }

  /**
   * Test helper method to add a group to the local repository.
   *
   * This method simplifies test data creation by automatically generating member IDs based on the
   * provided member count. Member IDs are generated as "member0", "member1", etc.
   *
   * **Note:** This method should only be used in test code.
   *
   * @param id The unique identifier for the group.
   * @param name The name of the group.
   * @param ownerId The ID of the group owner.
   * @param description The description of the group. Defaults to empty string.
   * @param memberCount The number of members in the group. Defaults to 0.
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

  /**
   * Test helper method to clear all groups from the local repository.
   *
   * This method removes all stored groups, providing a clean slate for test isolation. Typically
   * used in test setup or teardown methods.
   *
   * **Note:** This method should only be used in test code.
   */
  fun clearAllGroups() {
    groups.clear()
  }
}
