package com.android.joinme.model.authentification

/**
 * Provides a singleton instance of [AuthRepository] backed by Firebase Authentication.
 *
 * This provider object follows the dependency injection pattern, offering a centralized
 * access point for the authentication repository throughout the application. The repository
 * is lazily initialized on first access.
 *
 * The [repository] property is mutable to allow for dependency injection in tests,
 * enabling the use of fake or mock implementations during testing.
 */
object AuthRepositoryProvider {
  private val _repository: AuthRepository by lazy { AuthRepositoryFirebase() }

  var repository: AuthRepository = _repository
}
