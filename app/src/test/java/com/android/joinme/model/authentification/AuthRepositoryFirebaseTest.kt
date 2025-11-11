package com.android.joinme.model.authentification

import android.os.Bundle
import androidx.credentials.Credential
import androidx.credentials.CustomCredential
import com.google.android.gms.tasks.Tasks
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthRepositoryFirebaseTest {

  private lateinit var mockAuth: FirebaseAuth
  private lateinit var mockHelper: GoogleSignInHelper
  private lateinit var repository: AuthRepositoryFirebase

  @Before
  fun setup() {
    mockAuth = mockk(relaxed = true)
    mockHelper = mockk(relaxed = true)
    repository = AuthRepositoryFirebase(mockAuth, mockHelper)
  }

  @Test
  fun getGoogleSignInOption_returnsConfiguredOption() {
    val serverClientId = "test-client-id"
    val option = repository.getGoogleSignInOption(serverClientId)

    assertNotNull(option)
  }

  @Test
  fun signInWithGoogle_success_returnsUser() = runTest {
    // Arrange
    val mockCredential: CustomCredential = mockk(relaxed = true)
    val mockBundle: Bundle = mockk(relaxed = true)
    val mockGoogleIdToken: GoogleIdTokenCredential = mockk(relaxed = true)
    val mockFirebaseCred: AuthCredential = mockk(relaxed = true)
    val mockAuthResult: AuthResult = mockk(relaxed = true)
    val mockUser: FirebaseUser = mockk(relaxed = true)

    every { mockCredential.type } returns
        "com.google.android.libraries.identity.googleid.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL"
    every { mockCredential.data } returns mockBundle
    every { mockHelper.extractIdTokenCredential(mockBundle) } returns mockGoogleIdToken
    every { mockGoogleIdToken.idToken } returns "test-id-token"
    every { mockHelper.toFirebaseCredential("test-id-token") } returns mockFirebaseCred
    every { mockAuth.signInWithCredential(mockFirebaseCred) } returns
        Tasks.forResult(mockAuthResult)
    every { mockAuthResult.user } returns mockUser

    // Act
    val result = repository.signInWithGoogle(mockCredential)

    // Assert
    assertTrue(result.isSuccess)
    assertEquals(mockUser, result.getOrNull())
    verify { mockHelper.extractIdTokenCredential(mockBundle) }
    verify { mockHelper.toFirebaseCredential("test-id-token") }
    verify { mockAuth.signInWithCredential(mockFirebaseCred) }
  }

  @Test
  fun signInWithGoogle_nullUser_returnsFailure() = runTest {
    // Arrange
    val mockCredential: CustomCredential = mockk(relaxed = true)
    val mockBundle: Bundle = mockk(relaxed = true)
    val mockGoogleIdToken: GoogleIdTokenCredential = mockk(relaxed = true)
    val mockFirebaseCred: AuthCredential = mockk(relaxed = true)
    val mockAuthResult: AuthResult = mockk(relaxed = true)

    every { mockCredential.type } returns
        "com.google.android.libraries.identity.googleid.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL"
    every { mockCredential.data } returns mockBundle
    every { mockHelper.extractIdTokenCredential(mockBundle) } returns mockGoogleIdToken
    every { mockGoogleIdToken.idToken } returns "test-id-token"
    every { mockHelper.toFirebaseCredential("test-id-token") } returns mockFirebaseCred
    every { mockAuth.signInWithCredential(mockFirebaseCred) } returns
        Tasks.forResult(mockAuthResult)
    every { mockAuthResult.user } returns null

    // Act
    val result = repository.signInWithGoogle(mockCredential)

    // Assert
    assertTrue(result.isFailure)
    assertEquals(
        "Login failed : Could not retrieve user information", result.exceptionOrNull()?.message)
  }

  @Test
  fun signInWithGoogle_invalidCredentialType_returnsFailure() = runTest {
    // Arrange
    val mockCredential: CustomCredential = mockk(relaxed = true)
    every { mockCredential.type } returns "invalid-credential-type"

    // Act
    val result = repository.signInWithGoogle(mockCredential)

    // Assert
    assertTrue(result.isFailure)
    assertEquals(
        "Login failed: Credential is not of type Google ID", result.exceptionOrNull()?.message)
  }

  @Test
  fun signInWithGoogle_notCustomCredential_returnsFailure() = runTest {
    // Arrange
    val mockCredential: Credential = mockk(relaxed = true)

    // Act
    val result = repository.signInWithGoogle(mockCredential)

    // Assert
    assertTrue(result.isFailure)
    assertEquals(
        "Login failed: Credential is not of type Google ID", result.exceptionOrNull()?.message)
  }

  @Test
  fun signInWithGoogle_exceptionThrown_returnsFailure() = runTest {
    // Arrange
    val mockCredential: CustomCredential = mockk(relaxed = true)
    val mockBundle: Bundle = mockk(relaxed = true)

    every { mockCredential.type } returns
        "com.google.android.libraries.identity.googleid.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL"
    every { mockCredential.data } returns mockBundle
    every { mockHelper.extractIdTokenCredential(mockBundle) } throws RuntimeException("Test error")

    // Act
    val result = repository.signInWithGoogle(mockCredential)

    // Assert
    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull()?.message?.contains("Login failed") ?: false)
  }

  @Test
  fun getCurrentUser_returnsCurrentUser() = runTest {
    // Arrange
    val mockUser: FirebaseUser = mockk(relaxed = true)
    every { mockAuth.currentUser } returns mockUser

    // Act
    val user = repository.getCurrentUser()

    // Assert
    assertEquals(mockUser, user)
  }

  @Test
  fun getCurrentUser_noUser_returnsNull() = runTest {
    // Arrange
    every { mockAuth.currentUser } returns null

    // Act
    val user = repository.getCurrentUser()

    // Assert
    assertNull(user)
  }

  @Test
  fun getCurrentUserEmail_returnsEmail() = runTest {
    // Arrange
    val mockUser: FirebaseUser = mockk(relaxed = true)
    val expectedEmail = "test@example.com"
    every { mockAuth.currentUser } returns mockUser
    every { mockUser.email } returns expectedEmail

    // Act
    val email = repository.getCurrentUserEmail()

    // Assert
    assertEquals(expectedEmail, email)
  }

  @Test
  fun getCurrentUserEmail_noUser_returnsNull() = runTest {
    // Arrange
    every { mockAuth.currentUser } returns null

    // Act
    val email = repository.getCurrentUserEmail()

    // Assert
    assertNull(email)
  }

  @Test
  fun signOut_success_returnsSuccess() = runTest {
    // Arrange
    every { mockAuth.signOut() } returns Unit

    // Act
    val result = repository.signOut()

    // Assert
    assertTrue(result.isSuccess)
    verify { mockAuth.signOut() }
  }

  @Test
  fun signOut_exceptionThrown_returnsFailure() = runTest {
    // Arrange
    every { mockAuth.signOut() } throws RuntimeException("Sign out error")

    // Act
    val result = repository.signOut()

    // Assert
    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull()?.message?.contains("Logout failed") ?: false)
  }
}
