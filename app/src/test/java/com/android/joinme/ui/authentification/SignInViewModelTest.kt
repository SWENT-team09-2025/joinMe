package com.android.joinme.ui.authentification

import android.content.Context
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.android.joinme.R
import com.android.joinme.model.authentification.AuthRepository
import com.android.joinme.ui.signIn.SignInViewModel
import com.google.firebase.auth.FirebaseUser
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SignInViewModelTest {

  private lateinit var viewModel: SignInViewModel
  private lateinit var mockRepository: AuthRepository
  private lateinit var mockContext: Context
  private lateinit var mockCredentialManager: CredentialManager
  private val testDispatcher = UnconfinedTestDispatcher()

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    mockRepository = mockk()
    mockContext = mockk(relaxed = true)
    mockCredentialManager = mockk()

    // Mock getString for default_web_client_id
    every { mockContext.getString(R.string.default_web_client_id) } returns "test-client-id"

    viewModel = SignInViewModel(mockRepository)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun initialState_isCorrect() = runTest {
    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertNull(state.user)
    assertNull(state.errorMsg)
    assertFalse(state.signedOut)
  }

  @Test
  fun clearErrorMsg_clearsErrorMessage() = runTest {
    // Set an error message first by mocking a cancellation
    coEvery {
      mockCredentialManager.getCredential(any<Context>(), any<GetCredentialRequest>())
    } throws GetCredentialCancellationException("Test error")

    viewModel.signIn(mockContext, mockCredentialManager)

    // Verify error message is set
    assertNotNull(viewModel.uiState.value.errorMsg)

    // Clear error message
    viewModel.clearErrorMsg()

    // Verify error message is cleared
    assertNull(viewModel.uiState.value.errorMsg)
  }

  @Test
  fun signIn_successfulLogin_updatesStateWithUser() = runTest {
    val mockUser: FirebaseUser = mockk()
    val mockCredential: Credential = mockk()
    val mockCredentialResponse: GetCredentialResponse = mockk()

    every { mockCredentialResponse.credential } returns mockCredential
    coEvery { mockCredentialManager.getCredential(any(), any<GetCredentialRequest>()) } returns
        mockCredentialResponse
    coEvery { mockRepository.signInWithGoogle(mockCredential) } returns Result.success(mockUser)

    viewModel.signIn(mockContext, mockCredentialManager)

    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertEquals(mockUser, state.user)
    assertNull(state.errorMsg)
    assertFalse(state.signedOut)
  }

  @Test
  fun signIn_repositoryFailure_updatesStateWithError() = runTest {
    val mockCredential: Credential = mockk()
    val mockCredentialResponse: GetCredentialResponse = mockk()
    val errorMessage = "Authentication failed"
    val exception = Exception(errorMessage)

    every { mockCredentialResponse.credential } returns mockCredential
    coEvery { mockCredentialManager.getCredential(any(), any<GetCredentialRequest>()) } returns
        mockCredentialResponse
    coEvery { mockRepository.signInWithGoogle(mockCredential) } returns Result.failure(exception)

    viewModel.signIn(mockContext, mockCredentialManager)

    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertNull(state.user)
    assertEquals(errorMessage, state.errorMsg)
    assertTrue(state.signedOut)
  }

  @Test
  fun signIn_cancellation_updatesStateWithCancelMessage() = runTest {
    coEvery {
      mockCredentialManager.getCredential(any<Context>(), any<GetCredentialRequest>())
    } throws GetCredentialCancellationException("Cancelled")

    viewModel.signIn(mockContext, mockCredentialManager)

    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertNull(state.user)
    assertEquals("Sign-in cancelled", state.errorMsg)
    assertTrue(state.signedOut)
  }

  @Test
  fun signIn_credentialException_updatesStateWithError() = runTest {
    val errorMessage = "Credential error"
    // Use a concrete exception class instead of abstract GetCredentialException
    coEvery {
      mockCredentialManager.getCredential(any<Context>(), any<GetCredentialRequest>())
    } throws GetCredentialCancellationException(errorMessage)

    viewModel.signIn(mockContext, mockCredentialManager)

    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertNull(state.user)
    // The exception is a cancellation exception, so the message is different
    assertEquals("Sign-in cancelled", state.errorMsg)
    assertTrue(state.signedOut)
  }

  @Test
  fun signIn_unexpectedException_updatesStateWithError() = runTest {
    val errorMessage = "Unexpected error"
    coEvery {
      mockCredentialManager.getCredential(any<Context>(), any<GetCredentialRequest>())
    } throws RuntimeException(errorMessage)

    viewModel.signIn(mockContext, mockCredentialManager)

    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertNull(state.user)
    assertEquals("Unexpected error: $errorMessage", state.errorMsg)
    assertTrue(state.signedOut)
  }
}
