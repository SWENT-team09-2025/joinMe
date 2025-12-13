package com.android.joinme.model.profile

import android.content.Context
import com.android.joinme.network.NetworkMonitor
import com.android.joinme.util.TestEnvironmentDetector
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.FirebaseStorage

/**
 * Provides the correct [ProfileRepository] implementation depending on the environment.
 * - Test environment: Uses local (in-memory) repository
 * - Production: Uses cached repository with offline support
 */
object ProfileRepositoryProvider {

  // Local repository (in-memory, for testing only)
  private val localRepo: ProfileRepository by lazy { ProfileRepositoryLocal() }

  // Firestore repository (initialized lazily)
  private var firestoreRepo: ProfileRepository? = null

  // Cached repository (initialized lazily)
  private var cachedRepo: ProfileRepository? = null

  /**
   * Returns the appropriate repository implementation.
   *
   * @param context Application context (if null, attempts to get from Firebase)
   * @return ProfileRepository implementation
   */
  fun getRepository(context: Context? = null): ProfileRepository {
    // Test environment: use local repository
    if (isTestEnvironment()) return localRepo

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

  // For backward compatibility and explicit test injection
  var repository: ProfileRepository
    get() {
      return if (TestEnvironmentDetector.isTestEnvironment()) localRepo else getRepository()
    }
    set(value) {
      // Allows tests to inject custom repository
    }

  private fun getFirestoreRepo(): ProfileRepository {
    if (firestoreRepo == null) {
      firestoreRepo =
          ProfileRepositoryFirestore(
              db = Firebase.firestore, storage = FirebaseStorage.getInstance())
    }
    return firestoreRepo!!
  }

  private fun getCachedRepo(context: Context): ProfileRepository {
    if (cachedRepo == null) {
      val firestore = getFirestoreRepo()
      val networkMonitor = NetworkMonitor(context)
      cachedRepo = ProfileRepositoryCached(context, firestore, networkMonitor)
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
