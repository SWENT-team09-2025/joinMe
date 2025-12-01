package com.android.joinme.model.presence

import com.google.firebase.Firebase
import com.google.firebase.database.database

/**
 * Provides a singleton instance of the PresenceRepository for dependency injection.
 *
 * This provider follows the repository pattern and enables easy testing by allowing the repository
 * instance to be swapped with a mock or fake implementation. By default, it provides a Realtime
 * Database-backed implementation.
 */
object PresenceRepositoryProvider {
  /**
   * Lazily initialized private instance of the repository using Firebase Realtime Database as the
   * backend.
   */
  private val _repository: PresenceRepository by lazy {
    PresenceRepositoryRealtimeDatabase(Firebase.database)
  }

  /** Backing field for custom repository injection (used in tests). */
  private var _customRepository: PresenceRepository? = null

  /**
   * The current repository instance used throughout the application. Can be reassigned for testing
   * purposes to inject fake or mock implementations.
   */
  var repository: PresenceRepository
    get() = _customRepository ?: _repository
    set(value) {
      _customRepository = value
    }
}
