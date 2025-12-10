package com.android.joinme.model.invitation

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

/**
 * Provides a singleton instance of the InvitationsRepository for dependency injection. Uses a
 * Firestore-backed implementation in production. The `repository` property is mutable so tests can
 * inject a fake or mock implementation.
 */
object InvitationRepositoryProvider {
  /** Lazily initialized private instance of the repository using Firestore as the backend. */
  private val _repository: InvitationsRepository by lazy {
    InvitationRepositoryFirestore(Firebase.firestore)
  }

  /** Backing field for custom repository injection (used in tests). */
  private var _customRepository: InvitationsRepository? = null

  /**
   * The current repository instance used throughout the application. Can be reassigned for testing
   * purposes to inject fake or mock implementations.
   */
  var repository: InvitationsRepository
    get() = _customRepository ?: _repository
    set(value) {
      _customRepository = value
    }
}
