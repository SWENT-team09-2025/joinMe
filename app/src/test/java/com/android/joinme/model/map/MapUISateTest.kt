package com.android.joinme.model.map

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.joinme.ui.map.MapUIState
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MapUIStateTest {

  @Test
  fun `default MapUIState has correct values`() {
    val state = MapUIState()

    assertNull(state.userLocation)
    assertTrue(state.events.isEmpty())
    assertNull(state.errorMsg)
    assertFalse(state.isLoading)
  }

  @Test
  fun `MapUIState with userLocation`() {
    val location = UserLocation(46.5187, 6.5629, 5.0f)
    val state = MapUIState(userLocation = location)

    assertEquals(location, state.userLocation)
    assertTrue(state.events.isEmpty())
    assertNull(state.errorMsg)
    assertFalse(state.isLoading)
  }

  @Test
  fun `MapUIState copy works correctly`() {
    val initialState = MapUIState()
    val location = UserLocation(46.5187, 6.5629, 5.0f)

    val newState = initialState.copy(userLocation = location, isLoading = true)

    assertEquals(location, newState.userLocation)
    assertTrue(newState.isLoading)
    assertNull(newState.errorMsg)
  }

  @Test
  fun `MapUIState with error message`() {
    val errorMsg = "Test error"
    val state = MapUIState(errorMsg = errorMsg)

    assertEquals(errorMsg, state.errorMsg)
    assertNull(state.userLocation)
  }
}
