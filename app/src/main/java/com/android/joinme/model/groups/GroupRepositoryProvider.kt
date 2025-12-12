package com.android.joinme.model.groups

import com.android.joinme.util.TestEnvironmentDetector
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage

/**
 * Provides a singleton instance of the GroupRepository for dependency injection. Uses a local
 * in-memory repository when running in a test environment, and a Firestore-backed implementation
 * otherwise. The `repository` property is mutable so tests can inject a fake or mock
 * implementation.
 */
object GroupRepositoryProvider {
  // Local repository (in-memory)
  private val localRepo: GroupRepository by lazy { GroupRepositoryLocal() }

  // Firestore-backed repository
  private val firestoreRepo: GroupRepository by lazy {
    GroupRepositoryFirestore(Firebase.firestore, Firebase.storage)
  }

  // For backward compatibility and explicit test injection
  var repository: GroupRepository
    get() {
      return if (TestEnvironmentDetector.isTestEnvironment()) localRepo else firestoreRepo
    }
    set(_) {
      // Allows tests to inject custom repository
    }
}
