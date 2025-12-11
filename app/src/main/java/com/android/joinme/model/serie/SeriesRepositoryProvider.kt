package com.android.joinme.model.serie

import android.content.Context
import com.android.joinme.network.NetworkMonitor
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.firestore

/**
 * Provides the correct [SeriesRepository] implementation depending on the environment.
 * - Test environment: Uses local (in-memory) repository
 * - Production: Uses cached repository with offline support
 */
object SeriesRepositoryProvider {
  /** Lazily initialized private instance of the local (in-memory) repository for testing. */
  private val _localRepository: SeriesRepository by lazy { SeriesRepositoryLocal() }

  /**
   * Lazily initialized private instance of the repository using Firebase Firestore as the backend.
   */
  private var _firestoreRepository: SeriesRepository? = null

  /** Cached repository (initialized lazily) */
  private var _cachedRepository: SeriesRepository? = null

  /**
   * Returns the appropriate repository implementation.
   *
   * @param context Application context (required for production, optional for tests)
   * @return SeriesRepository implementation
   */
  fun getRepository(context: Context? = null): SeriesRepository {
    // Test environment: use local repository
    if (isTestEnvironment()) return _localRepository

    // Production: use cached repository with offline support
    requireNotNull(context) { "Context is required for production repository" }
    return getCachedRepo(context)
  }

  /**
   * The current repository instance used throughout the application. Automatically returns the
   * local repository in test environments, otherwise returns the Firestore repository.
   *
   * @deprecated Use getRepository(context) instead for offline support
   */
  val repository: SeriesRepository
    get() {
      if (isTestEnvironment()) return _localRepository

      // Try to get context from FirebaseApp
      val context =
          try {
            FirebaseApp.getInstance().applicationContext
          } catch (e: Exception) {
            throw e
          }

      return getRepository(context)
    }

  private fun getFirestoreRepo(): SeriesRepository {
    if (_firestoreRepository == null) {
      _firestoreRepository = SeriesRepositoryFirestore(Firebase.firestore)
    }
    return _firestoreRepository!!
  }

  private fun getCachedRepo(context: Context): SeriesRepository {
    if (_cachedRepository == null) {
      val firestore = getFirestoreRepo()
      val networkMonitor = NetworkMonitor(context)
      _cachedRepository = SeriesRepositoryCached(context, firestore, networkMonitor)
    }
    return _cachedRepository!!
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
    _firestoreRepository = null
    _cachedRepository = null
  }
}
