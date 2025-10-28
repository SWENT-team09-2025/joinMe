package com.android.joinme.model.serie

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

/**
 * Provides a singleton instance of the SeriesRepository for dependency injection.
 *
 * This provider follows the repository pattern and enables easy testing by allowing the repository
 * instance to be swapped with a mock or fake implementation. By default, it provides a Firestore-
 * backed implementation.
 */
object SeriesRepositoryProvider {
  /** Lazily initialized private instance of the local (in-memory) repository for testing. */
  private val _localRepository: SeriesRepository by lazy { SeriesRepositoryLocal() }

  /**
   * Lazily initialized private instance of the repository using Firebase Firestore as the backend.
   */
  private val _firestoreRepository: SeriesRepository by lazy {
    SeriesRepositoryFirestore(Firebase.firestore)
  }

  /**
   * The current repository instance used throughout the application. Automatically returns the
   * local repository in test environments, otherwise returns the Firestore repository.
   */
  val repository: SeriesRepository
    get() {
      // ✅ detect instrumented test environment
      val isTestEnv =
          android.os.Build.FINGERPRINT == "robolectric" ||
              android.os.Debug.isDebuggerConnected() ||
              System.getProperty("IS_TEST_ENV") == "true"

      return if (isTestEnv) _localRepository else _firestoreRepository
    }
}
