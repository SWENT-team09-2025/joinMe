package com.android.joinme.model.chat

// Implemented with help of Claude AI

import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

/**
 * Provides a singleton instance of [PollRepository].
 *
 * By default, uses [PollRepositoryRealtimeDatabase] for production. The repository can be swapped
 * for testing by setting the [repository] property.
 *
 * Usage:
 * ```
 * // Production (default)
 * val pollRepository = PollRepositoryProvider.repository
 *
 * // Testing
 * PollRepositoryProvider.repository = PollRepositoryLocal()
 * ```
 */
object PollRepositoryProvider {
  private val _repository: PollRepository by lazy {
    PollRepositoryRealtimeDatabase(Firebase.database)
  }

  private var _testRepository: PollRepository? = null

  /**
   * The poll repository instance.
   *
   * Defaults to [PollRepositoryRealtimeDatabase] but can be set to a different implementation
   * (e.g., [PollRepositoryLocal] for testing).
   */
  var repository: PollRepository
    get() = _testRepository ?: _repository
    set(value) {
      _testRepository = value
    }
}
