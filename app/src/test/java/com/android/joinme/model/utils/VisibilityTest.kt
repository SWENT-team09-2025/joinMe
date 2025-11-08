package com.android.joinme.model.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class VisibilityTest {

  @Test
  fun `displayString returns Public for PUBLIC`() {
    val formatted = Visibility.PUBLIC.displayString()
    assertEquals("Public", formatted)
  }

  @Test
  fun `displayString returns Private for PRIVATE`() {
    val formatted = Visibility.PRIVATE.displayString()
    assertEquals("Private", formatted)
  }
}
