package com.android.joinme.model.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class VisibilityTest {

  @Test
  fun `test Visibility enum has PUBLIC value`() {
    val visibility = Visibility.PUBLIC
    assertEquals(Visibility.PUBLIC, visibility)
  }

  @Test
  fun `test Visibility enum has PRIVATE value`() {
    val visibility = Visibility.PRIVATE
    assertEquals(Visibility.PRIVATE, visibility)
  }

  @Test
  fun `test Visibility PUBLIC and PRIVATE are different`() {
    assertNotEquals(Visibility.PUBLIC, Visibility.PRIVATE)
  }

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

  @Test
  fun `displayString capitalizes first letter correctly`() {
    val publicString = Visibility.PUBLIC.displayString()
    val privateString = Visibility.PRIVATE.displayString()

    // First character should be uppercase
    assertEquals('P', publicString[0])
    assertEquals('P', privateString[0])

    // Rest should be lowercase
    assertEquals("ublic", publicString.substring(1))
    assertEquals("rivate", privateString.substring(1))
  }

  @Test
  fun `displayString returns different strings for different values`() {
    val publicString = Visibility.PUBLIC.displayString()
    val privateString = Visibility.PRIVATE.displayString()

    assertNotEquals(publicString, privateString)
  }

  @Test
  fun `Visibility enum has exactly two values`() {
    val values = Visibility.values()
    assertEquals(2, values.size)
  }

  @Test
  fun `Visibility values returns PUBLIC and PRIVATE`() {
    val values = Visibility.values()
    assertEquals(Visibility.PUBLIC, values[0])
    assertEquals(Visibility.PRIVATE, values[1])
  }

  @Test
  fun `Visibility valueOf returns correct enum for PUBLIC`() {
    val visibility = Visibility.valueOf("PUBLIC")
    assertEquals(Visibility.PUBLIC, visibility)
  }

  @Test
  fun `Visibility valueOf returns correct enum for PRIVATE`() {
    val visibility = Visibility.valueOf("PRIVATE")
    assertEquals(Visibility.PRIVATE, visibility)
  }

  @Test
  fun `Visibility name property returns correct string`() {
    assertEquals("PUBLIC", Visibility.PUBLIC.name)
    assertEquals("PRIVATE", Visibility.PRIVATE.name)
  }

  @Test
  fun `Visibility ordinal values are correct`() {
    assertEquals(0, Visibility.PUBLIC.ordinal)
    assertEquals(1, Visibility.PRIVATE.ordinal)
  }

  @Test
  fun `displayString is consistent across multiple calls`() {
    val first = Visibility.PUBLIC.displayString()
    val second = Visibility.PUBLIC.displayString()
    assertEquals(first, second)
  }

  @Test
  fun `displayString does not modify original enum name`() {
    val visibility = Visibility.PUBLIC
    visibility.displayString()
    // Original name should remain unchanged
    assertEquals("PUBLIC", visibility.name)
  }
}
