package com.android.joinme.ui.components

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.joinme.model.invitation.InvitationRepositoryFirestore
import com.android.joinme.model.invitation.InvitationType
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Android instrumented tests for ShareButton component.
 *
 * This test covers:
 * - shareInvitation success path (lines 81-102)
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class ShareButtonAndroidTest {

  @Before
  fun setup() {
    MockKAnnotations.init(this, relaxUnitFun = true)
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  /**
   * Test: Covers shareInvitation success path Lines covered: 81-102 (shareInvitation success +
   * openShareSheet call)
   */
  @Test
  fun shareInvitation_success_completesWithoutError() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    var errorCalled = false

    // Mock InvitationRepositoryFirestore
    mockkConstructor(InvitationRepositoryFirestore::class)
    coEvery {
      anyConstructed<InvitationRepositoryFirestore>()
          .createInvitation(
              type = InvitationType.INVITATION_TO_GROUP,
              targetId = "test-group-id",
              createdBy = "test-user",
              expiresInDays = 7)
    } returns Result.success("mock-token-123")

    // Call shareInvitation
    shareInvitation(
        invitationType = InvitationType.INVITATION_TO_GROUP,
        targetId = "test-group-id",
        createdBy = "test-user",
        expiresInDays = 7,
        context = context,
        onError = { errorCalled = true })

    // Verify no error was called (success path executed)
    assert(!errorCalled) { "Error callback should not be called on success" }
  }
}
