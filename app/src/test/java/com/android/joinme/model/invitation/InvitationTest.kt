package com.android.joinme.model.invitation

import com.google.firebase.Timestamp
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InvitationTest {

  @Test
  fun `isValid returns true when expiresAt is null`() {
    val invitation =
        Invitation(
            token = "token123",
            type = InvitationType.INVITATION_TO_GROUP,
            targetId = "target123",
            createdBy = "user123",
            expiresAt = null)

    assertTrue(invitation.isValid())
  }

  @Test
  fun `isValid returns true when expiresAt is in the future`() {
    val futureDate = Date(System.currentTimeMillis() + 100000)
    val invitation =
        Invitation(
            token = "token123",
            type = InvitationType.INVITATION_TO_GROUP,
            targetId = "target123",
            createdBy = "user123",
            expiresAt = Timestamp(futureDate))

    assertTrue(invitation.isValid())
  }

  @Test
  fun `isValid returns false when expiresAt is in the past`() {
    val pastDate = Date(System.currentTimeMillis() - 100000)
    val invitation =
        Invitation(
            token = "token123",
            type = InvitationType.INVITATION_TO_GROUP,
            targetId = "target123",
            createdBy = "user123",
            expiresAt = Timestamp(pastDate))

    assertFalse(invitation.isValid())
  }

  @Test
  fun `InvitationType fromString returns correct type for valid string`() {
    assertEquals(
        InvitationType.INVITATION_TO_GROUP, InvitationType.fromString("INVITATION_TO_GROUP"))
    assertEquals(
        InvitationType.INVITATION_TO_EVENT, InvitationType.fromString("invitation_to_event"))
  }

  @Test
  fun `InvitationType fromString returns null for invalid string`() {
    assertNull(InvitationType.fromString("INVALID"))
  }

  @Test
  fun `InvitationType toDisplayString returns correct string`() {
    assertEquals("group", InvitationType.INVITATION_TO_GROUP.toDisplayString())
    assertEquals("event", InvitationType.INVITATION_TO_EVENT.toDisplayString())
    assertEquals("serie", InvitationType.INVITATION_TO_SERIES.toDisplayString())
  }
}
