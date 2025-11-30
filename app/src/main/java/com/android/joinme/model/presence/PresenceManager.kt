package com.android.joinme.model.presence

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Interface for providing context IDs that a user belongs to.
 *
 * A "context" can be any feature that needs presence tracking, such as:
 * - Chat conversations (group chats, event chats)
 * - Live events or activities
 * - Collaborative features
 *
 * This is used by PresenceManager to fetch all context IDs when setting user online.
 */
interface ContextIdProvider {
  /**
   * Fetches all context IDs that the user belongs to.
   *
   * @param userId The user ID to fetch context IDs for.
   * @return List of context IDs where the user should be tracked as online.
   */
  suspend fun getContextIdsForUser(userId: String): List<String>
}

// Backward compatibility alias
@Deprecated("Use ContextIdProvider instead", ReplaceWith("ContextIdProvider"))
typealias ChatIdProvider = ContextIdProvider

/**
 * Manages user presence tracking based on app lifecycle.
 *
 * This class observes the app's activity lifecycle and automatically sets the user as online in all
 * their contexts when the app comes to the foreground, and offline when going to background.
 *
 * A user is considered "online" when they have the app open, regardless of which screen they are
 * on.
 *
 * Usage:
 * ```
 * // In MainActivity when user logs in
 * PresenceManager.getInstance().startTracking(application, userId, contextIdProvider)
 *
 * // When user logs out
 * PresenceManager.getInstance().stopTracking()
 * ```
 */
class PresenceManager(
    private val presenceRepository: PresenceRepository = PresenceRepositoryProvider.repository
) {

  companion object {
    private const val TAG = "PresenceManager"

    @Volatile private var instance: PresenceManager? = null

    /** Gets or creates the singleton instance of PresenceManager. */
    fun getInstance(
        presenceRepository: PresenceRepository = PresenceRepositoryProvider.repository
    ): PresenceManager {
      return instance
          ?: synchronized(this) {
            instance ?: PresenceManager(presenceRepository).also { instance = it }
          }
    }

    /** Clears the singleton instance. Useful for testing. */
    fun clearInstance() {
      instance?.unregisterCallbacks()
      instance = null
    }
  }

  private var currentUserId: String? = null
  private var contextIdProvider: ContextIdProvider? = null
  private var isTracking = false
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private var application: Application? = null
  private var startedActivityCount = 0

  private val activityLifecycleCallbacks =
      object : Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

        override fun onActivityStarted(activity: Activity) {
          val wasInBackground = startedActivityCount == 0
          startedActivityCount++

          if (wasInBackground) {
            Log.d(TAG, "App came to foreground")
            // Set user online in all their contexts
            scope.launch { setUserOnlineInAllContexts() }
          }
        }

        override fun onActivityResumed(activity: Activity) {}

        override fun onActivityPaused(activity: Activity) {}

        override fun onActivityStopped(activity: Activity) {
          startedActivityCount--

          if (startedActivityCount == 0) {
            // App went to background - set user offline in all contexts
            Log.d(TAG, "App went to background")
            scope.launch {
              try {
                currentUserId?.let { userId ->
                  presenceRepository.setUserOffline(userId)
                  Log.d(TAG, "User $userId set as offline globally on background")
                }
              } catch (e: Exception) {
                Log.e(TAG, "Failed to set offline status on background", e)
              }
            }
          }
        }

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

        override fun onActivityDestroyed(activity: Activity) {}
      }

  /** Sets the user online in all their contexts by fetching context IDs from the provider. */
  private suspend fun setUserOnlineInAllContexts() {
    val userId = currentUserId ?: return
    val provider =
        contextIdProvider
            ?: run {
              Log.w(TAG, "ContextIdProvider not set, cannot set user online")
              return
            }

    try {
      val contextIds = provider.getContextIdsForUser(userId)
      if (contextIds.isNotEmpty()) {
        presenceRepository.setUserOnline(userId, contextIds)
        Log.d(TAG, "User $userId set as online in ${contextIds.size} contexts")
      } else {
        Log.d(TAG, "User $userId has no contexts to set online in")
      }
    } catch (e: Exception) {
      Log.e(TAG, "Failed to set user online in contexts", e)
    }
  }

  /**
   * Starts tracking presence for the given user.
   *
   * This method registers activity lifecycle callbacks to track when the app goes to foreground (to
   * set user online in all contexts) and background (to set user offline globally).
   *
   * @param application The application instance for registering callbacks.
   * @param userId The unique identifier of the current user.
   * @param contextIdProvider Provider to fetch all context IDs the user belongs to.
   */
  fun startTracking(
      application: Application,
      userId: String,
      contextIdProvider: ContextIdProvider
  ) {
    if (isTracking && currentUserId == userId) {
      Log.d(TAG, "Already tracking presence for user: $userId")
      return
    }

    this.application = application
    this.contextIdProvider = contextIdProvider
    currentUserId = userId
    isTracking = true
    startedActivityCount = 1 // App is already in foreground when startTracking is called

    application.registerActivityLifecycleCallbacks(activityLifecycleCallbacks)

    // Set user online immediately since app is in foreground
    scope.launch { setUserOnlineInAllContexts() }

    Log.d(TAG, "Started presence tracking for user: $userId")
  }

  /**
   * Starts tracking presence for the given user without Application instance.
   *
   * This is a simplified version that just stores the userId for later use. Without the Application
   * instance, automatic background detection won't work.
   *
   * @param userId The unique identifier of the current user.
   */
  fun startTracking(userId: String) {
    if (isTracking && currentUserId == userId) {
      Log.d(TAG, "Already tracking presence for user: $userId")
      return
    }

    currentUserId = userId
    isTracking = true

    Log.d(TAG, "Started presence tracking for user: $userId (simplified mode)")
  }

  /**
   * Stops tracking presence for the current user.
   *
   * This should be called when the user logs out. It will mark the user as offline globally and
   * unregister lifecycle callbacks.
   */
  fun stopTracking() {
    if (!isTracking) {
      Log.d(TAG, "Not currently tracking presence")
      return
    }

    scope.launch {
      try {
        currentUserId?.let { userId ->
          presenceRepository.setUserOffline(userId)
          Log.d(TAG, "User $userId set as offline during stop tracking")
        }
      } catch (e: Exception) {
        Log.e(TAG, "Failed to set offline status during stop", e)
      }
    }

    unregisterCallbacks()
    isTracking = false
    currentUserId = null
    contextIdProvider = null
    startedActivityCount = 0
    Log.d(TAG, "Stopped presence tracking")
  }

  /** Unregisters the activity lifecycle callbacks. */
  private fun unregisterCallbacks() {
    application?.unregisterActivityLifecycleCallbacks(activityLifecycleCallbacks)
    application = null
  }

  /** Gets the current user ID being tracked. */
  fun getCurrentUserId(): String? = currentUserId
}
