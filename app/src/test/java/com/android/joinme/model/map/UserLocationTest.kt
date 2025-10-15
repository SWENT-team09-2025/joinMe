package com.android.joinme.model.map

import androidx.test.ext.junit.runners.AndroidJUnit4
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UserLocationTest {

  @Test
  fun `UserLocation creation with all parameters`() {
    val location =
        UserLocation(
            latitude = 46.5187, longitude = 6.5629, accuracy = 5.0f, timestamp = 1234567890L)

    assertEquals(46.5187, location.latitude, 0.0001)
    assertEquals(6.5629, location.longitude, 0.0001)
    assertEquals(5.0f, location.accuracy)
    assertEquals(1234567890L, location.timestamp)
  }

  @Test
  fun `UserLocation with default timestamp`() {
    val beforeCreation = System.currentTimeMillis()
    val location = UserLocation(latitude = 46.5187, longitude = 6.5629, accuracy = 5.0f)
    val afterCreation = System.currentTimeMillis()

    assertTrue(location.timestamp >= beforeCreation)
    assertTrue(location.timestamp <= afterCreation)
  }

  @Test
  fun `UserLocation without accuracy`() {
    val location = UserLocation(latitude = 46.5187, longitude = 6.5629)

    assertNull(location.accuracy)
    assertEquals(46.5187, location.latitude, 0.0001)
    assertEquals(6.5629, location.longitude, 0.0001)
  }

  @Test
  fun `UserLocation equality`() {
    val location1 = UserLocation(46.5187, 6.5629, 5.0f, 1234567890L)
    val location2 = UserLocation(46.5187, 6.5629, 5.0f, 1234567890L)

    assertEquals(location1, location2)
    assertEquals(location1.hashCode(), location2.hashCode())
  }

  @Test
  fun `UserLocation copy`() {
    val original = UserLocation(46.5187, 6.5629, 5.0f)
    val copied = original.copy(latitude = 46.5200)

    assertEquals(46.5200, copied.latitude, 0.0001)
    assertEquals(6.5629, copied.longitude, 0.0001)
    assertEquals(5.0f, copied.accuracy)
  }
}
