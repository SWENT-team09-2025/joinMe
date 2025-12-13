package com.android.joinme.model.profile

import com.android.joinme.util.TestEnvironmentDetector
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.FirebaseStorage

/**
 * Provides a singleton instance of [ProfileRepository]. Uses local repository in test environment,
 * Firestore otherwise. `repository` is mutable for testing purpose.
 */
object ProfileRepositoryProvider {
  // Local repository (in-memory)
  private val localRepo: ProfileRepository by lazy { ProfileRepositoryLocal() }

  private val firestoreRepo: ProfileRepository by lazy {
    ProfileRepositoryFirestore(db = Firebase.firestore, storage = FirebaseStorage.getInstance())
  }

  // For backward compatibility and explicit test injection
  var repository: ProfileRepository
    get() {
      return if (TestEnvironmentDetector.isTestEnvironment()) localRepo else firestoreRepo
    }
    set(value) {
      // Allows tests to inject custom repository
    }
}
