package com.android.joinme.model.profile

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

/**
 * Provides a singleton instance of [ProfileRepository] backed by Firestore. `repository` is mutable
 * for testing purpose.
 */
object ProfileRepositoryProvider {
  private val _repository: ProfileRepository by lazy {
    ProfileRepositoryFirestore(Firebase.firestore)
  }

  var repository: ProfileRepository = _repository
}
