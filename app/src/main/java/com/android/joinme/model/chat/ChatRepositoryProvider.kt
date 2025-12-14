package com.android.joinme.model.chat

// Implemented with help of Claude AI

import android.content.Context
import com.android.joinme.network.NetworkMonitor
import com.android.joinme.util.TestEnvironmentDetector
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.database.database
import com.google.firebase.storage.storage

/**
 * Provides the correct [ChatRepository] implementation depending on the environment.
 * - Test environment: Uses local (in-memory) repository
 * - Production: Uses cached repository with offline support
 *
 * This provider follows the repository pattern and enables easy testing by allowing the repository
 * instance to be swapped with a mock or fake implementation.
 */
object ChatRepositoryProvider {

  // Local repository (in-memory, for testing only)
  private val localRepo: ChatRepository by lazy { ChatRepositoryLocal() }

  // Realtime Database repository (initialized lazily)
  private var realtimeDbRepo: ChatRepository? = null

  // Cached repository (initialized lazily)
  private var cachedRepo: ChatRepository? = null

  /**
   * Returns the appropriate repository implementation based on the environment.
   *
   * @param context Application context (if null, attempts to get from Firebase)
   * @return ChatRepository implementation
   */
  fun getRepository(context: Context? = null): ChatRepository {
    // If a repository is injected for testing, return it
    injectedRepository?.let {
      return it
    }

    // Test environment: use local repository
    if (TestEnvironmentDetector.isTestEnvironment()) return localRepo

    // Production: use cached repository with offline support
    // Try to get context from Firebase if not provided
    val ctx =
        context
            ?: try {
              FirebaseApp.getInstance().applicationContext
            } catch (e: Exception) {
              null
            }
    requireNotNull(ctx) { "Context is required for production repository" }
    return getCachedRepo(ctx)
  }

  // Injected repository for testing (takes precedence over default behavior)
  private var injectedRepository: ChatRepository? = null

  // For backward compatibility and explicit test injection
  val repository: ChatRepository
    get() {
      return injectedRepository
          ?: if (TestEnvironmentDetector.isTestEnvironment()) localRepo else getRepository()
    }

  /**
   * Injects a custom repository for testing purposes.
   *
   * @param repository The repository to use, or null to reset to default behavior
   */
  @androidx.annotation.VisibleForTesting
  fun setRepositoryForTesting(repository: ChatRepository?) {
    injectedRepository = repository
  }

  private fun getRealtimeDbRepo(): ChatRepository {
    if (realtimeDbRepo == null) {
      realtimeDbRepo = ChatRepositoryRealtimeDatabase(Firebase.database, Firebase.storage)
    }
    return realtimeDbRepo!!
  }

  private fun getCachedRepo(context: Context): ChatRepository {
    if (cachedRepo == null) {
      val realtimeDb = getRealtimeDbRepo()
      val networkMonitor = NetworkMonitor(context)
      cachedRepo = ChatRepositoryCached(context, realtimeDb, networkMonitor)
    }
    return cachedRepo!!
  }

  /** For testing only - allows resetting the singleton state. */
  @androidx.annotation.VisibleForTesting
  fun resetForTesting() {
    realtimeDbRepo = null
    cachedRepo = null
    injectedRepository = null
  }
}
