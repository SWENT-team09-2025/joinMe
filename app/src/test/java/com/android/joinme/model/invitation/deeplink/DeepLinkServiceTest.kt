package com.android.joinme.model.invitation.deepLink

import android.content.Intent
import android.net.Uri
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeepLinkServiceTest {

  @Test
  fun generateInvitationLink_returnsCorrectUrl() {
    val token = "abc123xyz"
    val link = DeepLinkService.generateInvitationLink(token)

    assertEquals("https://joinme.app/invite/abc123xyz", link)
  }

  @Test
  fun parseInvitationLink_validHttpsUrl_returnsToken() {
    val mockIntent = mockk<Intent>(relaxed = true)
    val mockUri = mockk<Uri>(relaxed = true)
    every { mockIntent.data } returns mockUri
    every { mockUri.path } returns "/invite/abc123xyz"

    val token = DeepLinkService.parseInvitationLink(mockIntent)
    val hasLink = DeepLinkService.hasInvitationLink(mockIntent)

    assertTrue(hasLink)
    assertEquals("abc123xyz", token)
  }

  @Test
  fun parseInvitationLink_noData_returnsNull() {
    val mockIntent = mockk<Intent>(relaxed = true)
    every { mockIntent.data } returns null

    val token = DeepLinkService.parseInvitationLink(mockIntent)

    assertNull(token)
  }

  @Test
  fun parseInvitationLink_noPath_returnsNull() {
    val mockIntent = mockk<Intent>(relaxed = true)
    val mockUri = mockk<Uri>(relaxed = true)
    every { mockIntent.data } returns mockUri
    every { mockUri.path } returns null

    val token = DeepLinkService.parseInvitationLink(mockIntent)

    assertNull(token)
  }

  @Test
  fun parseInvitationLink_wrongPathPrefix_returnsNull() {
    val mockIntent = mockk<Intent>(relaxed = true)
    val mockUri = mockk<Uri>(relaxed = true)
    every { mockIntent.data } returns mockUri
    every { mockUri.path } returns "/wrongpath/abc123"

    val token = DeepLinkService.parseInvitationLink(mockIntent)

    assertNull(token)
  }

  @Test
  fun parseInvitationLink_tooManySegments_returnsNull() {
    val mockIntent = mockk<Intent>(relaxed = true)
    val mockUri = mockk<Uri>(relaxed = true)
    every { mockIntent.data } returns mockUri
    every { mockUri.path } returns "/invite/abc123/extra"

    val token = DeepLinkService.parseInvitationLink(mockIntent)

    assertNull(token)
  }

  @Test
  fun parseInvitationLink_tooFewSegments_returnsNull() {
    val mockIntent = mockk<Intent>(relaxed = true)
    val mockUri = mockk<Uri>(relaxed = true)
    every { mockIntent.data } returns mockUri
    every { mockUri.path } returns "/invite"

    val token = DeepLinkService.parseInvitationLink(mockIntent)

    assertNull(token)
  }

  @Test
  fun hasInvitationLink_invalidLink_returnsFalse() {
    val mockIntent = mockk<Intent>(relaxed = true)
    every { mockIntent.data } returns null

    val hasLink = DeepLinkService.hasInvitationLink(mockIntent)

    assertFalse(hasLink)
  }
}
