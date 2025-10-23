package com.android.joinme.model.group

import com.android.joinme.model.event.EventType

/**
 * Repository interface for managing group-related data operations.
 *
 * This interface defines the contract for interacting with group data, supporting operations such
 * as fetching user groups, leaving groups, creating new groups, and retrieving individual group
 * details. Implementations can use different data sources (e.g., Firestore, local storage).
 */
interface GroupRepository {
  /**
   * Retrieves all groups that the current user belongs to.
   *
   * @return A list of [Group] objects representing the user's groups. Returns an empty list if the
   *   user is not part of any groups.
   * @throws Exception if there is an error fetching groups from the data source.
   */
  suspend fun userGroups(): List<Group>

  /**
   * Removes the current user from the specified group.
   *
   * @param id The unique identifier of the group to leave.
   * @throws Exception if the group cannot be found or if the operation fails.
   */
  suspend fun leaveGroup(id: String)

  /**
   * Retrieves detailed information about a specific group.
   *
   * @param id The unique identifier of the group to retrieve.
   * @return The [Group] object if found, or null if no group exists with the given ID.
   * @throws Exception if there is an error accessing the data source.
   */
  suspend fun getGroup(id: String): Group?

  /**
   * Creates a new group with the current user as owner and admin member.
   *
   * This operation atomically:
   * 1. Creates a new group document with the provided details
   * 2. Adds the creator as a member with admin role
   * 3. Initializes the group with the creator in the memberIds list
   *
   * @param name The name of the group (required, 3-30 characters)
   * @param category The category of the group (Social/Activity/Sports)
   * @param description Optional description of the group (max 300 characters)
   * @return The ID of the newly created group
   * @throws IllegalStateException if the user is not authenticated
   * @throws Exception if the group creation fails
   */
  suspend fun createGroup(name: String, category: EventType, description: String = ""): String
}
