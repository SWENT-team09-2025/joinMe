package com.android.joinme.ui.authentification

import com.android.joinme.ui.signIn.SignInViewModel
import junit.framework.TestCase.assertFalse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SignInViewModelTest {

  private lateinit var viewModel: SignInViewModel
  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    viewModel = SignInViewModel()
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

  /*@Test
  fun clearErrorMsg_resetsError() = runTest {
      viewModel.signIn(context = mockContext, credentialManager = mockCredentialManager) // simulate error
      viewModel.clearErrorMsg()
      assertThat(viewModel.uiState.value.errorMsg).isNull()
  }*/

  // TODO: You need to mock CredentialManager and AuthRepository to test signIn properly
}
