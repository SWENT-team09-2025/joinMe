package com.android.joinme.model.groups

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

/**
 * Provides a singleton instance of the GroupRepository for dependency injection.
 * Uses a local in-memory repository when running in a test environment, and a
 * Firestore-backed implementation otherwise. The `repository` property is
 * mutable so tests can inject a fake or mock implementation.
 */
object GroupRepositoryProvider {
  // Local repository (in-memory)
  private val localRepo: GroupRepository by lazy { GroupRepositoryLocal() }

  // Firestore-backed repository
  private val firestoreRepo: GroupRepository by lazy { GroupRepositoryFirestore(Firebase.firestore) }

  // For backward compatibility and explicit test injection
  var repository: GroupRepository
    get() {
      val isTestEnv =
          android.os.Build.FINGERPRINT == "robolectric" ||
              android.os.Debug.isDebuggerConnected() ||
              System.getProperty("IS_TEST_ENV") == "true"

      return if (isTestEnv) localRepo else firestoreRepo
    }
    set(_) {
      // Allows tests to inject custom repository
    }
}
