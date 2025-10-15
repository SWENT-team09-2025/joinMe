package com.android.joinme.model.authentification

import androidx.credentials.Credential
import com.google.firebase.auth.FirebaseUser

/** Handles authentication operations such as signing in with Google and signing out. */
interface AuthRepository {

  /**
   * Signs in the user using a Google account through the Credential Manager API.
   *
   * @return A [Result] containing a [FirebaseUser] on success, or an exception on failure.
   */
  suspend fun signInWithGoogle(credential: Credential): Result<FirebaseUser>

  /** Returns the currently authenticated [FirebaseUser], or `null` if no user is signed in. */
  suspend fun getCurrentUser(): FirebaseUser?

  /** Returns the email of the currently authenticated user, or `null` if no user is signed in. */
  suspend fun getCurrentUserEmail(): String?

  /**
   * Signs out the currently authenticated user and clears the credential state.
   *
   * @return A [Result] indicating success or failure.
   */
  suspend fun signOut(): Result<Unit>
}
