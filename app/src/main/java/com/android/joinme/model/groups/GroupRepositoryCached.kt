package com.android.joinme.model.groups

import android.content.Context
import android.net.Uri
import android.util.Log
import com.android.joinme.model.database.AppDatabase
import com.android.joinme.model.database.toEntity
import com.android.joinme.model.database.toGroup
import com.android.joinme.model.event.OfflineException
import com.android.joinme.network.NetworkMonitor
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.withTimeout

/**
 * Cached implementation of GroupRepository. Implements offline-first read strategy and online-only
 * write strategy.
 *
 * Read strategy:
 * 1. Try to fetch from Firestore if online (and cache the result)
 * 2. If offline or fetch fails, return cached data from Room
 *
 * Write strategy:
 * 1. Require network connectivity
 * 2. Write to Firestore
 * 3. Update local cache
 *
 * @param context Application context for database access
 * @param firestoreRepo The Firestore repository implementation to delegate online operations to
 * @param networkMonitor Network connectivity monitor
 */
class GroupRepositoryCached(
    private val context: Context,
    private val firestoreRepo: GroupRepository,
    private val networkMonitor: NetworkMonitor
) : GroupRepository {
  private var firestoreErrorMsg = "Failed to fetch from Firestore, falling back to cache"
  private val database = AppDatabase.getDatabase(context)
  private val groupDao = database.groupDao()

  companion object {
    /** Timeout for Firestore operations in milliseconds (3 seconds) */
    private const val FIRESTORE_TIMEOUT_MS = 3000L
  }

  override fun getNewGroupId(): String = firestoreRepo.getNewGroupId()

  override suspend fun getAllGroups(): List<Group> {
    // Try to fetch from Firestore if online
    if (networkMonitor.isOnline()) {
      try {
        val groups = withTimeout(FIRESTORE_TIMEOUT_MS) { firestoreRepo.getAllGroups() }
        // Clear cache and update with fresh data to remove any deleted groups
        groupDao.deleteAllGroups()
        // Always update cache, even if empty, to ensure consistency
        if (groups.isNotEmpty()) {
          groupDao.insertGroups(groups.map { it.toEntity() })
        }
        return groups
      } catch (e: Exception) {
        Log.w("GroupRepositoryCached", firestoreErrorMsg, e)
      }
    }

    // Offline or network error - return cached groups for current user
    val userId =
        Firebase.auth.currentUser?.uid
            ?: throw Exception("GroupRepositoryCached: User not logged in.")
    return groupDao.getAllGroups().map { it.toGroup() }.filter { group ->
      group.memberIds.contains(userId)
    }
  }

  override suspend fun getGroup(groupId: String): Group {
    // Try to fetch from Firestore if online
    if (networkMonitor.isOnline()) {
      try {
        val group = withTimeout(FIRESTORE_TIMEOUT_MS) { firestoreRepo.getGroup(groupId) }
        // Delete from cache first to handle any staleness, then insert fresh data
        groupDao.deleteGroup(groupId)
        groupDao.insertGroup(group.toEntity())
        return group
      } catch (e: Exception) {
        Log.w("GroupRepositoryCached", firestoreErrorMsg, e)
      }
    }

    // Offline or network error - try cached version
    val cached = groupDao.getGroupById(groupId)?.toGroup()
    return cached
        ?: throw OfflineException(
            "Cannot fetch group while offline and no cached version available")
  }

  override suspend fun addGroup(group: Group) {
    requireOnline()
    firestoreRepo.addGroup(group)
    // Cache the newly created group
    groupDao.insertGroup(group.toEntity())
  }

  override suspend fun editGroup(groupId: String, newValue: Group) {
    requireOnline()
    firestoreRepo.editGroup(groupId, newValue)
    // Update cache
    groupDao.insertGroup(newValue.toEntity())
  }

  override suspend fun deleteGroup(groupId: String, userId: String) {
    requireOnline()
    firestoreRepo.deleteGroup(groupId, userId)
    // Remove from cache
    groupDao.deleteGroup(groupId)
  }

  override suspend fun leaveGroup(groupId: String, userId: String) {
    requireOnline()
    firestoreRepo.leaveGroup(groupId, userId)
    // Refresh cache with updated group
    try {
      val updatedGroup = withTimeout(FIRESTORE_TIMEOUT_MS) { firestoreRepo.getGroup(groupId) }
      groupDao.insertGroup(updatedGroup.toEntity())
    } catch (e: Exception) {
      // If we can't refresh, remove from cache to avoid showing stale data
      groupDao.deleteGroup(groupId)
    }
  }

  override suspend fun joinGroup(groupId: String, userId: String) {
    requireOnline()
    firestoreRepo.joinGroup(groupId, userId)
    // Refresh cache with updated group
    try {
      val updatedGroup = withTimeout(FIRESTORE_TIMEOUT_MS) { firestoreRepo.getGroup(groupId) }
      groupDao.insertGroup(updatedGroup.toEntity())
    } catch (e: Exception) {
      // If we can't refresh, at least try to keep existing cache
      Log.w("GroupRepositoryCached", "Failed to refresh group after join", e)
    }
  }

  override suspend fun getCommonGroups(userIds: List<String>): List<Group> {
    if (networkMonitor.isOnline()) {
      try {
        val groups = withTimeout(FIRESTORE_TIMEOUT_MS) { firestoreRepo.getCommonGroups(userIds) }
        if (groups.isNotEmpty()) {
          groupDao.insertGroups(groups.map { it.toEntity() })
        }
        return groups
      } catch (e: Exception) {
        Log.w("GroupRepositoryCached", firestoreErrorMsg, e)
      }
    }

    // Offline - filter cached groups locally
    // This is a best-effort implementation - may not be perfectly accurate
    return groupDao
        .getAllGroups()
        .map { it.toGroup() }
        .filter { group -> userIds.all { userId -> group.memberIds.contains(userId) } }
  }

  override suspend fun uploadGroupPhoto(context: Context, groupId: String, imageUri: Uri): String {
    requireOnline()
    val photoUrl = firestoreRepo.uploadGroupPhoto(context, groupId, imageUri)
    // Refresh cache with updated group
    try {
      val updatedGroup = withTimeout(FIRESTORE_TIMEOUT_MS) { firestoreRepo.getGroup(groupId) }
      groupDao.insertGroup(updatedGroup.toEntity())
    } catch (e: Exception) {
      Log.w("GroupRepositoryCached", "Failed to refresh group after photo upload", e)
    }
    return photoUrl
  }

  override suspend fun deleteGroupPhoto(groupId: String) {
    requireOnline()
    firestoreRepo.deleteGroupPhoto(groupId)
    // Refresh cache with updated group
    try {
      val updatedGroup = withTimeout(FIRESTORE_TIMEOUT_MS) { firestoreRepo.getGroup(groupId) }
      groupDao.insertGroup(updatedGroup.toEntity())
    } catch (e: Exception) {
      Log.w("GroupRepositoryCached", "Failed to refresh group after photo deletion", e)
    }
  }

  /**
   * Checks if the device is online and throws an exception if not.
   *
   * This method is used to guard write operations that require network connectivity.
   *
   * @throws OfflineException if the device is offline
   */
  private fun requireOnline() {
    if (!networkMonitor.isOnline()) {
      throw OfflineException("This operation requires an internet connection")
    }
  }
}
