package com.android.joinme.utils

import android.content.Context
import android.util.Base64
import androidx.core.os.bundleOf
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import org.json.JSONObject

/**
 * Generates fake JWT tokens for testing authentication.
 *
 * These tokens have the correct structure to be parsed by Firebase Auth but use fake signatures.
 */
object FakeJwtGenerator {
  private var _counter = 0
  private val counter
    get() = _counter++

  private fun base64UrlEncode(input: ByteArray): String {
    return Base64.encodeToString(input, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
  }

  /**
   * Creates a fake Google ID token (JWT) for testing.
   *
   * @param uid The user ID (sub claim)
   * @param name The user's display name
   * @param email The user's email
   * @return A fake JWT token string
   */
  fun createFakeGoogleIdToken(
      uid: String = counter.toString(),
      name: String = "Test User",
      email: String = "test@example.com"
  ): String {
    val header = JSONObject(mapOf("alg" to "none"))
    val payload =
        JSONObject(
            mapOf(
                "sub" to uid,
                "email" to email,
                "name" to name,
                "picture" to "http://example.com/avatar.png",
                "iss" to "https://accounts.google.com",
                "aud" to "test-client-id",
                "iat" to (System.currentTimeMillis() / 1000),
                "exp" to (System.currentTimeMillis() / 1000 + 3600)))

    val headerEncoded = base64UrlEncode(header.toString().toByteArray())
    val payloadEncoded = base64UrlEncode(payload.toString().toByteArray())

    // Signature can be anything, emulator doesn't check it
    val signature = "sig"

    return "$headerEncoded.$payloadEncoded.$signature"
  }
}

/**
 * Fake CredentialManager for testing Google Sign-In flows without actual authentication.
 *
 * This uses MockK to create a mock CredentialManager that returns fake Google ID tokens.
 */
object FakeCredentialManager {
  /**
   * Creates a mock CredentialManager that always returns a CustomCredential containing the given
   * fakeUserIdToken when getCredential() is called.
   *
   * @param context Android context
   * @param fakeIdToken The fake JWT token to return
   */
  fun create(context: Context, fakeIdToken: String): CredentialManager {
    mockkObject(GoogleIdTokenCredential)
    val googleIdTokenCredential = mockk<GoogleIdTokenCredential>()
    every { googleIdTokenCredential.idToken } returns fakeIdToken
    every { GoogleIdTokenCredential.createFrom(any()) } returns googleIdTokenCredential

    val fakeCredentialManager = mockk<CredentialManager>()
    val mockGetCredentialResponse = mockk<GetCredentialResponse>()

    val fakeCustomCredential =
        CustomCredential(
            type = TYPE_GOOGLE_ID_TOKEN_CREDENTIAL, data = bundleOf("id_token" to fakeIdToken))

    every { mockGetCredentialResponse.credential } returns fakeCustomCredential
    coEvery {
      fakeCredentialManager.getCredential(any<Context>(), any<GetCredentialRequest>())
    } returns mockGetCredentialResponse

    return fakeCredentialManager
  }
}
