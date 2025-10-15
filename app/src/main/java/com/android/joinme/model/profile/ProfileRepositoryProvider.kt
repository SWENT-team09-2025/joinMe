package com.android.joinme.model.profile

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.FirebaseStorage

/**
 * Provides a singleton instance of [ProfileRepository] backed by Firestore and Storage.
 *
 * `repository` is mutable for testing purposes.
 */
object ProfileRepositoryProvider {

  private val _repository: ProfileRepository by lazy {
    ProfileRepositoryFirestore(db = Firebase.firestore, storage = FirebaseStorage.getInstance())
  }

  var repository: ProfileRepository = _repository
}
