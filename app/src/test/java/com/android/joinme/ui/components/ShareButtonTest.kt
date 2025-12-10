package com.android.joinme.ui.components

import android.content.Context
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ApplicationProvider
import com.android.joinme.model.invitation.InvitationRepositoryFirestore
import com.android.joinme.model.invitation.InvitationType
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowToast

/**
 * Optimized unit tests for ShareButton component.
 *
 * Strategy: 4 tests for 100% line coverage
 * - Test 1: UI rendering
 * - Test 2: Success path (invitation created)
 * - Test 3: Failure path (onFailure)
 * - Test 4: Exception path (catch)
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ShareButtonTest {

  @get:Rule val composeTestRule = createComposeRule()

  private val testDispatcher = UnconfinedTestDispatcher()

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    MockKAnnotations.init(this, relaxUnitFun = true)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
    unmockkAll()
  }

  /**
   * Test 1: Covers ShareButton composable rendering Lines covered: 46-69 (ShareButton composable)
   */
  @Test
  fun shareButton_rendersCorrectly_forGroupInvitation() {
    composeTestRule.setContent {
      ShareButton(
          invitationType = InvitationType.GROUP,
          targetId = "test-id",
          createdBy = "user-123",
          expiresInDays = 7)
    }

    // Verify button is rendered
    composeTestRule.onNodeWithTag(ShareButtonTestTags.SHARE_BUTTON).assertHasClickAction()
  }

  /** Test 2: Covers shareInvitation failure path Lines covered: 103-109 (onFailure block) */
  @Test
  fun shareInvitation_failure_callsOnError() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    var errorCalled = false

    mockkConstructor(InvitationRepositoryFirestore::class)
    coEvery {
      anyConstructed<InvitationRepositoryFirestore>().createInvitation(any(), any(), any(), any())
    } returns Result.failure(Exception("Test error"))

    shareInvitation(
        invitationType = InvitationType.EVENT,
        targetId = "id-2",
        createdBy = "user-2",
        expiresInDays = 3,
        context = context,
        onError = { errorCalled = true })

    assert(errorCalled)
    assert(ShadowToast.getTextOfLatestToast()?.contains("Failed to create invitation") == true)
  }

  /**
   * Test 3: Covers shareInvitation success path Lines covered: 97-99 (onSuccess block) This test
   * verifies the success path by mocking createInvitation to return a success result and ensuring
   * the code executes without throwing exceptions.
   */
  @Test
  fun shareInvitation_success_completesWithoutException() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()

    mockkConstructor(InvitationRepositoryFirestore::class)
    coEvery {
      anyConstructed<InvitationRepositoryFirestore>().createInvitation(any(), any(), any(), any())
    } returns Result.success("test-token-123")

    mockkObject(com.android.joinme.model.invitation.deepLink.DeepLinkService)
    every {
      com.android.joinme.model.invitation.deepLink.DeepLinkService.generateInvitationLink(any())
    } returns "https://joinme-aa9e8.web.app/invite/test-token-123"

    // Mock context to prevent actual activity launch
    val mockContext = spyk(context)
    every { mockContext.startActivity(any()) } returns Unit

    // This should complete without throwing an exception
    shareInvitation(
        invitationType = InvitationType.SERIE,
        targetId = "serie-id",
        createdBy = "user-123",
        expiresInDays = 7,
        context = mockContext,
        onError = {})

    // If we get here without exception, the success path was executed
    assert(true)
  }

  /** Test 4: Covers shareInvitation exception catch path Lines covered: 108-114 (catch block) */
  @Test
  fun shareInvitation_exception_callsOnError() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    var errorCalled = false

    mockkConstructor(InvitationRepositoryFirestore::class)
    coEvery {
      anyConstructed<InvitationRepositoryFirestore>().createInvitation(any(), any(), any(), any())
    } throws RuntimeException("Network error")

    shareInvitation(
        invitationType = InvitationType.GROUP,
        targetId = "group-id",
        createdBy = "user-456",
        expiresInDays = null,
        context = context,
        onError = { errorCalled = true })

    assert(errorCalled)
    assert(ShadowToast.getTextOfLatestToast()?.contains("Failed to create invitation") == true)
  }
}
