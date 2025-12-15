// Implemented with help of Claude AI
package com.android.joinme.model.groups

import android.content.Context
import android.net.Uri

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
   * @param userId The ID of the user attempting to delete the group (must be the owner).
   * @throws Exception if the Group item is not found or user is not the owner.
   */
  suspend fun deleteGroup(groupId: String, userId: String)

  /**
   * Removes the current user from a Group's member list.
   *
   * @param groupId The unique identifier of the Group item to leave.
   * @param userId The ID of the user who wants to leave the group.
   * @throws Exception if the Group item is not found.
   */
  suspend fun leaveGroup(groupId: String, userId: String)

  /**
   * Adds a user to a Group's member list.
   *
   * @param groupId The unique identifier of the Group to join.
   * @param userId The ID of the user who wants to join the group.
   * @throws Exception if the Group item is not found or user is already a member.
   */
  suspend fun joinGroup(groupId: String, userId: String)

  /**
   * Retrieves all groups that are common to all specified users (where all users are members).
   *
   * @param userIds The list of user IDs to find common groups for.
   * @return A list of Group items where all specified users are members.
   */
  suspend fun getCommonGroups(userIds: List<String>): List<Group>

  /**
   * Uploads a photo for the given group and updates its photoUrl field.
   *
   * The photo will be stored at a deterministic path (groups/{groupId}/group.jpg) to ensure
   * idempotency - subsequent uploads will replace the previous photo. After successful upload, the
   * group's photoUrl field is automatically updated.
   *
   * @param context Android context for reading and processing the image
   * @param groupId The unique identifier of the group
   * @param imageUri The local URI of the image to upload
   * @return The download URL of the uploaded image
   * @throws Exception if upload fails (network error, permissions, etc.)
   */
  suspend fun uploadGroupPhoto(context: Context, groupId: String, imageUri: Uri): String

  /**
   * Deletes the photo for the given group and clears the photoUrl field.
   *
   * @param groupId The unique identifier of the group
   * @throws Exception if deletion fails
   */
  suspend fun deleteGroupPhoto(groupId: String)
}
