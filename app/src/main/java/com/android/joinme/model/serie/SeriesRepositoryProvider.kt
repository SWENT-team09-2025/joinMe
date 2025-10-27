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
  /**
   * Lazily initialized private instance of the repository using Firebase Firestore as the backend.
   */
  private val _repository: SeriesRepository by lazy {
    SeriesRepositoryFirestore(Firebase.firestore)
  }

  /**
   * The current repository instance used throughout the application. Can be reassigned for testing
   * purposes to inject fake or mock implementations.
   */
  var repository: SeriesRepository = _repository
}
