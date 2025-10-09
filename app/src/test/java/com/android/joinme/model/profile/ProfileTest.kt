package com.android.joinme.model.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileTest {
  @Test
  fun defaultsAreSensibleAndStable() {
    val p = Profile() // uses default values

    // Act + Assert
    assertEquals("", p.uid)
    assertEquals(null, p.photoUrl)
    assertEquals("", p.username)
    assertEquals("", p.email)
    assertEquals(null, p.dateOfBirth)
    assertEquals(null, p.country)
    assertTrue(p.interests.isEmpty())
    assertEquals(null, p.bio)
    assertEquals(null, p.createdAt)
    assertEquals(null, p.updatedAt)
  }
}
