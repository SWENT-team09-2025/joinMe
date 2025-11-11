package com.android.joinme.model.authentification

import android.os.Bundle
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.AuthCredential
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class GoogleSignInHelperTest {

  @Test
  fun defaultGoogleSignInHelper_canBeInstantiated() {
    // Verify that the default implementation can be created
    val instance = DefaultGoogleSignInHelper()
    assertNotNull(instance)
  }

  @Test
  fun defaultGoogleSignInHelper_implementsGoogleSignInHelper() {
    // Verify that DefaultGoogleSignInHelper implements the interface
    val instance: GoogleSignInHelper = DefaultGoogleSignInHelper()
    assertNotNull(instance)
  }

  @Test
  fun googleSignInHelper_interface_canBeMocked() {
    // Verify that the interface can be mocked for dependency injection
    val mockHelper: GoogleSignInHelper = mockk(relaxed = true)
    val mockBundle: Bundle = mockk(relaxed = true)
    val mockCredential: GoogleIdTokenCredential = mockk(relaxed = true)
    val mockAuthCredential: AuthCredential = mockk(relaxed = true)

    every { mockHelper.extractIdTokenCredential(mockBundle) } returns mockCredential
    every { mockHelper.toFirebaseCredential("test-token") } returns mockAuthCredential

    // Verify mocking works as expected
    assertEquals(mockCredential, mockHelper.extractIdTokenCredential(mockBundle))
    assertEquals(mockAuthCredential, mockHelper.toFirebaseCredential("test-token"))

    verify { mockHelper.extractIdTokenCredential(mockBundle) }
    verify { mockHelper.toFirebaseCredential("test-token") }
  }

  @Test
  fun googleSignInHelper_mock_extractIdTokenCredential_worksCorrectly() {
    // Test that mocking extractIdTokenCredential works properly
    val mockHelper: GoogleSignInHelper = mockk()
    val mockBundle: Bundle = mockk()
    val mockCredential: GoogleIdTokenCredential = mockk()

    every { mockHelper.extractIdTokenCredential(mockBundle) } returns mockCredential

    val result = mockHelper.extractIdTokenCredential(mockBundle)

    assertEquals(mockCredential, result)
    verify(exactly = 1) { mockHelper.extractIdTokenCredential(mockBundle) }
  }

  @Test
  fun googleSignInHelper_mock_toFirebaseCredential_worksCorrectly() {
    // Test that mocking toFirebaseCredential works properly
    val mockHelper: GoogleSignInHelper = mockk()
    val mockAuthCredential: AuthCredential = mockk()
    val idToken = "test-id-token"

    every { mockHelper.toFirebaseCredential(idToken) } returns mockAuthCredential

    val result = mockHelper.toFirebaseCredential(idToken)

    assertEquals(mockAuthCredential, result)
    verify(exactly = 1) { mockHelper.toFirebaseCredential(idToken) }
  }
}
