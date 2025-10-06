package com.android.joinme.model.event

import org.junit.Assert.assertEquals
import org.junit.Test

class EventDisplayStringTest {

  @Test
  fun `displayString returns correct string for all EventType values`() {
    assertEquals("Sports", EventType.SPORTS.displayString())
    assertEquals("Activity", EventType.ACTIVITY.displayString())
    assertEquals("Social", EventType.SOCIAL.displayString())
  }

  @Test
  fun `displayString returns correct string for all EventVisibility values`() {
    assertEquals("Public", EventVisibility.PUBLIC.displayString())
    assertEquals("Private", EventVisibility.PRIVATE.displayString())
  }

  @Test
  fun `displayString handles lowercase correctly`() {
    // Simulate internal transformation — it should always return capitalized form
    val result = EventType.SPORTS.displayString()
    assertEquals("Sports", result)
  }

  @Test
  fun `displayString formatting is consistent`() {
    val visibility = EventVisibility.PRIVATE.displayString()
    val type = EventType.ACTIVITY.displayString()

    // Both should start with an uppercase letter
    assert(visibility.first().isUpperCase())
    assert(type.first().isUpperCase())
  }
}
