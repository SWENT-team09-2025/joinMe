package com.android.joinme.model.event

import android.content.Context
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
    val isTestEnv =
        android.os.Build.FINGERPRINT == "robolectric" ||
            android.os.Debug.isDebuggerConnected() ||
            System.getProperty("IS_TEST_ENV") == "true" ||
            try {
              // Check if we're running in an instrumented test by looking for the test runner
              Class.forName("androidx.test.runner.AndroidJUnitRunner")
              true
            } catch (e: ClassNotFoundException) {
              false
            }

    // Test environment: use local repository
    if (isTestEnv) return localRepo

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
    val isTestEnv =
        android.os.Build.FINGERPRINT == "robolectric" ||
            android.os.Debug.isDebuggerConnected() ||
            System.getProperty("IS_TEST_ENV") == "true" ||
            try {
              Class.forName("androidx.test.runner.AndroidJUnitRunner")
              true
            } catch (e: ClassNotFoundException) {
              false
            }

    if (isTestEnv) return localRepo

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
      firestoreRepo = EventsRepositoryFirestore(Firebase.firestore, context)
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

  /** For testing only - allows resetting the singleton state. */
  @androidx.annotation.VisibleForTesting
  fun resetForTesting() {
    firestoreRepo = null
    cachedRepo = null
  }
}
