// Implemented with help of Claude AI
package com.android.joinme.model.groups

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Exception message for when a group is not found in the local repository. */
const val GROUP_NOT_FOUND = "GroupRepositoryLocal: Group not found"

/** Represents a repository that manages a local list of groups (for offline mode or testing). */
class GroupRepositoryLocal : GroupRepository {
  private val groups: MutableList<Group> = mutableListOf()
  private var counter = 0

  /** Clears all groups from the repository. Useful for testing. */
  fun clear() {
    groups.clear()
    counter = 0
  }

  override fun getNewGroupId(): String {
    return (counter++).toString()
  }

  override suspend fun getAllGroups(): List<Group> {
    return groups
  }

  override suspend fun getGroup(groupId: String): Group {
    return groups.find { it.id == groupId } ?: throw Exception(GROUP_NOT_FOUND)
  }

  override suspend fun addGroup(group: Group) {
    groups.add(group)
  }

  override suspend fun editGroup(groupId: String, newValue: Group) {
    val index = groups.indexOfFirst { it.id == groupId }
    if (index != -1) {
      groups[index] = newValue
    } else {
      throw Exception(GROUP_NOT_FOUND)
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
      throw Exception(GROUP_NOT_FOUND)
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

  override suspend fun getCommonGroups(userIds: List<String>): List<Group> {
    if (userIds.isEmpty()) return emptyList()

    // Filter groups where all specified users are members
    return groups.filter { group -> userIds.all { userId -> group.memberIds.contains(userId) } }
  }

  override suspend fun uploadGroupPhoto(context: Context, groupId: String, imageUri: Uri): String =
      withContext(Dispatchers.IO) {
        // 1) Resolve current group
        val current =
            groups.find { it.id == groupId }
                ?: throw IllegalArgumentException("Group with ID $groupId does not exist")

        // 2) Ensure app-local pictures dir
        val picturesDir = File(context.filesDir, "group_photos").apply { mkdirs() }

        // 3) If there was a previous local file, delete it
        current.photoUrl?.let { previousUrl ->
          runCatching {
            val prev = previousUrl.toUri()
            if (prev.scheme == "file") {
              File(prev.path ?: "").takeIf { it.exists() && it.parentFile == picturesDir }?.delete()
            }
          }
        }

        // 4) Copy new content into app storage with a stable name
        val filename = "${groupId}_${System.currentTimeMillis()}.jpg"
        val dest = File(picturesDir, filename)

        context.contentResolver.openInputStream(imageUri).use { inStream ->
          requireNotNull(inStream) { "Unable to open input stream for $imageUri" }
          FileOutputStream(dest).use { out -> inStream.copyTo(out) }
        }

        // 5) Build a file:// URL
        val fileUrl = dest.toURI().toURL().toString()

        // 6) Update group
        val index = groups.indexOfFirst { it.id == groupId }
        if (index != -1) {
          groups[index] = current.copy(photoUrl = fileUrl)
        }

        return@withContext fileUrl
      }

  override suspend fun deleteGroupPhoto(groupId: String) =
      withContext(Dispatchers.IO) {
        val current = groups.find { it.id == groupId } ?: return@withContext
        current.photoUrl?.let { url ->
          runCatching {
            val uri = url.toUri()
            if (uri.scheme == "file") {
              File(uri.path ?: "").takeIf { it.exists() }?.delete()
            }
          }
        }
        val index = groups.indexOfFirst { it.id == groupId }
        if (index != -1) {
          groups[index] = current.copy(photoUrl = null)
        }
      }
}
