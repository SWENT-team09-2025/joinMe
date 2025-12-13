package com.android.joinme.model.event

import android.content.Context
import com.android.joinme.model.chat.ChatRepositoryProvider
import com.android.joinme.network.NetworkMonitor
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

/**
 * Provides the correct [EventsRepository] implementation depending on the environment.
 * - Test environment: Uses local (in-memory) repository
 * - Production: Uses cached repository with offline support
 */
object EventsRepositoryProvider {

  // Local repository (in-memory, for testing only)
  private val localRepo: EventsRepository by lazy { EventsRepositoryLocal() }

  // Firestore repository (initialized lazily)
  private var firestoreRepo: EventsRepository? = null

  // Cached repository (initialized lazily)
  private var cachedRepo: EventsRepository? = null

  /**
   * Returns the appropriate repository implementation.
   *
   * @param context Application context (required for production, optional for tests)
   * @return EventsRepository implementation
   */
  fun getRepository(context: Context? = null): EventsRepository {
    // Test environment: use local repository
    if (isTestEnvironment()) return localRepo

    // Production: use cached repository with offline support
    requireNotNull(context) { "Context is required for production repository" }
    return getCachedRepo(context)
  }

  /**
   * Legacy method for backward compatibility. Prefers to use context-based approach.
   *
   * @param isOnline Ignored - network state is handled internally by cached repo
   * @param context Application context (if null, attempts to get from Firebase)
   * @return EventsRepository implementation
   */
  fun getRepository(isOnline: Boolean, context: Context? = null): EventsRepository {
    if (isTestEnvironment()) return localRepo

    // Try to get context from Firebase if not provided
    val ctx =
        context
            ?: try {
              FirebaseApp.getInstance().applicationContext
            } catch (e: Exception) {
              null
            }

    return getRepository(ctx)
  }

  private fun getFirestoreRepo(context: Context): EventsRepository {
    if (firestoreRepo == null) {
      val apps = FirebaseApp.getApps(context)
      if (apps.isEmpty()) {
        FirebaseApp.initializeApp(context)
      }
      val chatRepository = ChatRepositoryProvider.repository
      firestoreRepo =
          EventsRepositoryFirestore(
              db = Firebase.firestore, context = context, chatRepository = chatRepository)
    }
    return firestoreRepo!!
  }

  private fun getCachedRepo(context: Context): EventsRepository {
    if (cachedRepo == null) {
      val firestore = getFirestoreRepo(context)
      val networkMonitor = NetworkMonitor(context)
      cachedRepo = EventsRepositoryCached(context, firestore, networkMonitor)
    }
    return cachedRepo!!
  }

  /**
   * Checks if the current environment is a test environment.
   *
   * @return true if running in a test environment, false otherwise
   */
  private fun isTestEnvironment(): Boolean {
    return android.os.Build.FINGERPRINT == "robolectric" ||
        android.os.Debug.isDebuggerConnected() ||
        System.getProperty("IS_TEST_ENV") == "true" ||
        try {
          Class.forName("androidx.test.runner.AndroidJUnitRunner")
          true
        } catch (e: ClassNotFoundException) {
          false
        }
  }

  /** For testing only - allows resetting the singleton state. */
  @androidx.annotation.VisibleForTesting
  fun resetForTesting() {
    firestoreRepo = null
    cachedRepo = null
  }
}
