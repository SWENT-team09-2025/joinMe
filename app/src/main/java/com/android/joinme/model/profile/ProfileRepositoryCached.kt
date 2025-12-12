package com.android.joinme.model.profile

import android.content.Context
import android.net.Uri
import android.util.Log
import com.android.joinme.model.database.AppDatabase
import com.android.joinme.model.database.toEntity
import com.android.joinme.model.database.toProfile
import com.android.joinme.model.event.OfflineException
import com.android.joinme.network.NetworkMonitor
import kotlinx.coroutines.withTimeout

/**
 * Cached implementation of ProfileRepository. Implements offline-first read strategy and
 * online-only write strategy.
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
class ProfileRepositoryCached(
    private val context: Context,
    private val firestoreRepo: ProfileRepository,
    private val networkMonitor: NetworkMonitor
) : ProfileRepository {
  private var firestoreErrorMsg = "Failed to fetch from Firestore, falling back to cache"
  private val database = AppDatabase.getDatabase(context)
  private val profileDao = database.profileDao()

  companion object {
    /** Timeout for Firestore operations in milliseconds (3 seconds) */
    private const val FIRESTORE_TIMEOUT_MS = 3000L
  }

  override suspend fun getProfile(uid: String): Profile? {
    // Try to fetch from Firestore if online
    if (networkMonitor.isOnline()) {
      try {
        val profile = withTimeout(FIRESTORE_TIMEOUT_MS) { firestoreRepo.getProfile(uid) }
        if (profile != null) {
          // Delete from cache first to handle any staleness, then insert fresh data
          profileDao.deleteProfile(uid)
          profileDao.insertProfile(profile.toEntity())
        }
        return profile
      } catch (e: Exception) {
        Log.w("ProfileRepositoryCached", firestoreErrorMsg, e)
      }
    }

    // Offline or network error - try cached version
    return profileDao.getProfileById(uid)?.toProfile()
  }

  override suspend fun getProfilesByIds(uids: List<String>): List<Profile>? {
    if (networkMonitor.isOnline()) {
      try {
        val profiles = withTimeout(FIRESTORE_TIMEOUT_MS) { firestoreRepo.getProfilesByIds(uids) }
        if (profiles != null && profiles.isNotEmpty()) {
          // Delete requested UIDs from cache first to handle deleted profiles
          uids.forEach { profileDao.deleteProfile(it) }
          profileDao.insertProfiles(profiles.map { it.toEntity() })
        }
        return profiles
      } catch (e: Exception) {
        Log.w("ProfileRepositoryCached", firestoreErrorMsg, e)
      }
    }

    // Offline or error - get from cache
    val cachedProfiles = uids.mapNotNull { profileDao.getProfileById(it)?.toProfile() }
    return if (cachedProfiles.isEmpty()) null else cachedProfiles
  }

  override suspend fun createOrUpdateProfile(profile: Profile) {
    requireOnline()
    firestoreRepo.createOrUpdateProfile(profile)
    // Update cache
    profileDao.insertProfile(profile.toEntity())
  }

  override suspend fun deleteProfile(uid: String) {
    requireOnline()
    firestoreRepo.deleteProfile(uid)
    // Remove from cache
    profileDao.deleteProfile(uid)
  }

  override suspend fun uploadProfilePhoto(context: Context, uid: String, imageUri: Uri): String {
    requireOnline()
    val photoUrl = firestoreRepo.uploadProfilePhoto(context, uid, imageUri)
    // Refresh cache with updated profile
    try {
      val updatedProfile = withTimeout(FIRESTORE_TIMEOUT_MS) { firestoreRepo.getProfile(uid) }
      if (updatedProfile != null) {
        profileDao.insertProfile(updatedProfile.toEntity())
      }
    } catch (e: Exception) {
      Log.w("ProfileRepositoryCached", "Failed to refresh profile after photo upload", e)
    }
    return photoUrl
  }

  override suspend fun deleteProfilePhoto(uid: String) {
    requireOnline()
    firestoreRepo.deleteProfilePhoto(uid)
    // Refresh cache with updated profile
    try {
      val updatedProfile = withTimeout(FIRESTORE_TIMEOUT_MS) { firestoreRepo.getProfile(uid) }
      if (updatedProfile != null) {
        profileDao.insertProfile(updatedProfile.toEntity())
      }
    } catch (e: Exception) {
      Log.w("ProfileRepositoryCached", "Failed to refresh profile after photo deletion", e)
    }
  }

  override suspend fun followUser(followerId: String, followedId: String) {
    requireOnline()
    firestoreRepo.followUser(followerId, followedId)
    // Refresh both user profiles to update follower/following counts
    try {
      withTimeout(FIRESTORE_TIMEOUT_MS) {
        val followerProfile = firestoreRepo.getProfile(followerId)
        val followedProfile = firestoreRepo.getProfile(followedId)
        followerProfile?.let { profileDao.insertProfile(it.toEntity()) }
        followedProfile?.let { profileDao.insertProfile(it.toEntity()) }
      }
    } catch (e: Exception) {
      Log.w("ProfileRepositoryCached", "Failed to refresh profiles after follow", e)
    }
  }

  override suspend fun unfollowUser(followerId: String, followedId: String) {
    requireOnline()
    firestoreRepo.unfollowUser(followerId, followedId)
    // Refresh both user profiles to update follower/following counts
    try {
      withTimeout(FIRESTORE_TIMEOUT_MS) {
        val followerProfile = firestoreRepo.getProfile(followerId)
        val followedProfile = firestoreRepo.getProfile(followedId)
        followerProfile?.let { profileDao.insertProfile(it.toEntity()) }
        followedProfile?.let { profileDao.insertProfile(it.toEntity()) }
      }
    } catch (e: Exception) {
      Log.w("ProfileRepositoryCached", "Failed to refresh profiles after unfollow", e)
    }
  }

  override suspend fun isFollowing(followerId: String, followedId: String): Boolean {
    requireOnline()
    // This checks relationship state in Firestore, not profile data
    // Don't cache this as it's not profile information
    return firestoreRepo.isFollowing(followerId, followedId)
  }

  override suspend fun getFollowing(userId: String, limit: Int): List<Profile> {
    if (networkMonitor.isOnline()) {
      try {
        val profiles =
            withTimeout(FIRESTORE_TIMEOUT_MS) { firestoreRepo.getFollowing(userId, limit) }
        // Cache the profiles returned
        if (profiles.isNotEmpty()) {
          profileDao.insertProfiles(profiles.map { it.toEntity() })
        }
        return profiles
      } catch (e: Exception) {
        Log.w("ProfileRepositoryCached", firestoreErrorMsg, e)
      }
    }

    // Offline - can't determine following relationship without Firestore
    // Return empty list rather than incorrect cached data
    return emptyList()
  }

  override suspend fun getFollowers(userId: String, limit: Int): List<Profile> {
    if (networkMonitor.isOnline()) {
      try {
        val profiles =
            withTimeout(FIRESTORE_TIMEOUT_MS) { firestoreRepo.getFollowers(userId, limit) }
        // Cache the profiles returned
        if (profiles.isNotEmpty()) {
          profileDao.insertProfiles(profiles.map { it.toEntity() })
        }
        return profiles
      } catch (e: Exception) {
        Log.w("ProfileRepositoryCached", firestoreErrorMsg, e)
      }
    }

    // Offline - can't determine follower relationship without Firestore
    // Return empty list rather than incorrect cached data
    return emptyList()
  }

  override suspend fun getMutualFollowing(userId1: String, userId2: String): List<Profile> {
    if (networkMonitor.isOnline()) {
      try {
        val profiles =
            withTimeout(FIRESTORE_TIMEOUT_MS) { firestoreRepo.getMutualFollowing(userId1, userId2) }
        // Cache the profiles returned
        if (profiles.isNotEmpty()) {
          profileDao.insertProfiles(profiles.map { it.toEntity() })
        }
        return profiles
      } catch (e: Exception) {
        Log.w("ProfileRepositoryCached", firestoreErrorMsg, e)
      }
    }

    // Offline - can't determine mutual following without Firestore
    // Return empty list rather than incorrect cached data
    return emptyList()
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
