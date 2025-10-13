package com.android.joinme.model.authentification

/**
 * Provides a singleton instance of [AuthRepository] backed by Firebase Authentication. `repository`
 * is mutable for testing purpose.
 */
object AuthRepositoryProvider {
  private val _repository: AuthRepository by lazy { AuthRepositoryFirebase() }

  var repository: AuthRepository = _repository
}
