package com.android.joinme.model.chat

// Implemented with help of Claude AI

import com.google.firebase.Firebase
import com.google.firebase.database.database

/**
 * Provides a singleton instance of the PollRepository for dependency injection.
 *
 * This provider follows the repository pattern and enables easy testing by allowing the repository
 * instance to be swapped with a mock or fake implementation. By default, it provides a Realtime
 * Database-backed implementation.
 */
object PollRepositoryProvider {
  /**
   * Lazily initialized private instance of the repository using Firebase Realtime Database as the
   * backend.
   */
  private val _repository: PollRepository by lazy {
    PollRepositoryRealtimeDatabase(Firebase.database)
  }

  private var _overrideRepository: PollRepository? = null

  /**
   * The current repository instance used throughout the application. Can be reassigned for testing
   * purposes to inject fake or mock implementations.
   */
  var repository: PollRepository
    get() = _overrideRepository ?: _repository
    set(value) {
      _overrideRepository = value
    }
}
