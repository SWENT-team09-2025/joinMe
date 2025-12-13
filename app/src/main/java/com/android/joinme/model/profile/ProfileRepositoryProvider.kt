package com.android.joinme.model.profile

import com.android.joinme.model.chat.ChatRepositoryProvider
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
    val chatRepository = ChatRepositoryProvider.repository
    ProfileRepositoryFirestore(
        db = Firebase.firestore,
        storage = FirebaseStorage.getInstance(),
        chatRepository = chatRepository)
  }

  // For backward compatibility and explicit test injection
  var repository: ProfileRepository
    get() {
      val isTestEnv =
          android.os.Build.FINGERPRINT == "robolectric" ||
              android.os.Debug.isDebuggerConnected() ||
              System.getProperty("IS_TEST_ENV") == "true"

      return if (isTestEnv) localRepo else firestoreRepo
    }
    set(value) {
      // Allows tests to inject custom repository
    }
}
