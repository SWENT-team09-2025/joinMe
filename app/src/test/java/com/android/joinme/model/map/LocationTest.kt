package com.android.joinme.model.map

import org.junit.Assert.assertEquals
import org.junit.Test

class LocationTest {

  @Test
  fun `test location properties`() {
    val location = Location(latitude = 46.5191, longitude = 6.5668, name = "EPFL")

    assertEquals(46.5191, location.latitude, 0.0001)
    assertEquals(6.5668, location.longitude, 0.0001)
    assertEquals("EPFL", location.name)
  }

  @Test
  fun `test locations with same values are equal`() {
    val loc1 = Location(46.5191, 6.5668, "EPFL")
    val loc2 = Location(46.5191, 6.5668, "EPFL")

    assertEquals(loc1, loc2)
  }
}
