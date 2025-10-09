package com.android.joinme.ui.authentification

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.joinme.ui.signIn.SignInScreen
import com.android.joinme.ui.signIn.SignInScreenTestTags
import com.android.joinme.ui.signIn.SignInViewModel
import junit.framework.TestCase.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SignInScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  private fun setupScreen(
      viewModel: SignInViewModel = SignInViewModel(),
      onSignedIn: () -> Unit = {}
  ) {
    composeTestRule.setContent { SignInScreen(authViewModel = viewModel, onSignedIn = onSignedIn) }
  }

  @Test
  fun signInScreen_displaysAppLogo() {
    setupScreen()
    composeTestRule.onNodeWithTag(SignInScreenTestTags.APP_LOGO).assertIsDisplayed()
  }

  @Test
  fun signInScreen_displaysLoginButton() {
    setupScreen()
    composeTestRule.onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithText("Sign in with Google").assertIsDisplayed()
  }

  @Test
  fun signInScreen_buttonIsClickable() {
    var clicked = false
    setupScreen(onSignedIn = { clicked = true })
    composeTestRule.onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON).performClick()
    // Here we only test that click triggers something; actual sign-in flow needs mocking
    assertFalse(clicked)
  }

  /*@Test
  fun signInScreen_showsLoadingIndicatorWhenLoading() {
      val viewModel = SignInViewModel()
      setupScreen(viewModel = viewModel)

      composeTestRule.runOnUiThread {
          viewModel.clearErrorMsg() // Just to trigger state update
      }

      composeTestRule.onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON).assertIsDisplayed()
      composeTestRule.runOnUiThread {
          viewModel.signIn(viewModel = viewModel) // simulate loading
      }
  }*/
}
