package com.android.joinme.model.chat

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

/**
 * Provides a singleton instance of the ChatRepository for dependency injection.
 *
 * This provider follows the repository pattern and enables easy testing by allowing the repository
 * instance to be swapped with a mock or fake implementation. By default, it provides a Firestore-
 * backed implementation.
 */
object ChatRepositoryProvider {
  /**
   * Lazily initialized private instance of the repository using Firebase Firestore as the backend.
   */
  private val _repository: ChatRepository by lazy { ChatRepositoryFirestore(Firebase.firestore) }

  /**
   * The current repository instance used throughout the application. Can be reassigned for testing
   * purposes to inject fake or mock implementations.
   */
  var repository: ChatRepository = _repository
}
