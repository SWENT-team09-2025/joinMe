package com.android.joinme.model.groups

/** Represents a repository that manages Group items. */
interface GroupRepository {

  /** Generates and returns a new unique identifier for a Group item. */
  fun getNewGroupId(): String

  /**
   * Retrieves all Group items that the current user belongs to.
   *
   * @return A list of all Group items the user is part of.
   */
  suspend fun getAllGroups(): List<Group>

  /**
   * Retrieves a specific Group item by its unique identifier.
   *
   * @param groupId The unique identifier of the Group item to retrieve.
   * @return The Group item with the specified identifier.
   * @throws Exception if the group item is not found.
   */
  suspend fun getGroup(groupId: String): Group

  /**
   * Adds a new Group item to the repository.
   *
   * @param group The Group item to add.
   */
  suspend fun addGroup(group: Group)

  /**
   * Edits an existing Group item in the repository.
   *
   * @param groupId The unique identifier of the Group item to edit.
   * @param newValue The new value for the Group item.
   * @throws Exception if the Group item is not found.
   */
  suspend fun editGroup(groupId: String, newValue: Group)

  /**
   * Deletes a Group item from the repository.
   *
   * @param groupId The unique identifier of the Group item to delete.
   * @throws Exception if the Group item is not found.
   */
  suspend fun deleteGroup(groupId: String)
}
