package com.android.joinme.model.presence

// Implemented with help of Claude AI

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
    fun getInstance(): PresenceManager {
      return instance
          ?: synchronized(this) {
            instance
                ?: PresenceManager(PresenceRepositoryProvider.repository).also { instance = it }
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
  private var currentPresenceJob: Job? = null

  private val activityLifecycleCallbacks =
      object : Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

        override fun onActivityStarted(activity: Activity) {
          val wasInBackground = startedActivityCount == 0
          startedActivityCount++

          if (wasInBackground) {
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
            scope.launch {
              try {
                currentUserId?.let { userId -> presenceRepository.setUserOffline(userId) }
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
      if (contextIds.isNotEmpty()) presenceRepository.setUserOnline(userId, contextIds)
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
    if (userId.isBlank()) {
      Log.w(TAG, "startTracking called with blank userId")
      return
    }

    if (isTracking && currentUserId == userId) {
      return
    }

    // Cancel any pending presence job to avoid race conditions
    currentPresenceJob?.cancel()

    this.application = application
    this.contextIdProvider = contextIdProvider
    currentUserId = userId
    isTracking = true
    startedActivityCount = 1 // App is already in foreground when startTracking is called

    application.registerActivityLifecycleCallbacks(activityLifecycleCallbacks)

    // Set user online immediately since app is in foreground
    currentPresenceJob = scope.launch { setUserOnlineInAllContexts() }
  }

  /**
   * Stops tracking presence for the current user.
   *
   * This should be called when the user logs out. It will mark the user as offline globally and
   * unregister lifecycle callbacks.
   */
  fun stopTracking() {
    if (!isTracking) {
      return
    }

    // Cancel any pending presence job to avoid race conditions
    currentPresenceJob?.cancel()

    // Capture userId before clearing state
    val userId = currentUserId

    // Clear state first to prevent race conditions
    unregisterCallbacks()
    isTracking = false
    currentUserId = null
    contextIdProvider = null
    startedActivityCount = 0
    currentPresenceJob = null

    // Set user offline after clearing state
    userId?.let {
      scope.launch {
        try {
          presenceRepository.setUserOffline(it)
        } catch (e: Exception) {
          Log.e(TAG, "Failed to set offline status during stop", e)
        }
      }
    }
  }

  /** Unregisters the activity lifecycle callbacks. */
  private fun unregisterCallbacks() {
    application?.unregisterActivityLifecycleCallbacks(activityLifecycleCallbacks)
    application = null
  }

  /** Gets the current user ID being tracked. */
  fun getCurrentUserId(): String? = currentUserId
}
