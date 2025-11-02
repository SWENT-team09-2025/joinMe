package com.android.joinme.utils.coverage

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for CoverageTestUtil This test file is only for verifying SonarCloud coverage
 * functionality.
 */
class CoverageTestUtilTest {

  private lateinit var util: CoverageTestUtil

  @Before
  fun setUp() {
    util = CoverageTestUtil()
  }

  @Test
  fun testAdd() {
    assertEquals(5, util.add(2, 3))
    assertEquals(0, util.add(-1, 1))
    assertEquals(-5, util.add(-2, -3))
  }

  @Test
  fun testSubtract() {
    assertEquals(1, util.subtract(3, 2))
    assertEquals(-2, util.subtract(-1, 1))
    assertEquals(5, util.subtract(2, -3))
  }

  @Test
  fun testMultiply() {
    assertEquals(6, util.multiply(2, 3))
    assertEquals(-6, util.multiply(-2, 3))
    assertEquals(0, util.multiply(0, 5))
  }

  @Test
  fun testDivide() {
    assertEquals(2, util.divide(6, 3))
    assertEquals(-2, util.divide(-6, 3))
    assertEquals(0, util.divide(0, 5))
  }

  @Test(expected = IllegalArgumentException::class)
  fun testDivideByZero() {
    util.divide(5, 0)
  }

  @Test
  fun testIsEven() {
    assertTrue(util.isEven(2))
    assertTrue(util.isEven(0))
    assertTrue(util.isEven(-4))
    assertFalse(util.isEven(3))
    assertFalse(util.isEven(-5))
  }

  @Test
  fun testIsPositive() {
    assertTrue(util.isPositive(5))
    assertFalse(util.isPositive(0))
    assertFalse(util.isPositive(-5))
  }

  @Test
  fun testMax() {
    assertEquals(5, util.max(3, 5))
    assertEquals(5, util.max(5, 3))
    assertEquals(5, util.max(5, 5))
    assertEquals(0, util.max(-1, 0))
  }

  @Test
  fun testMin() {
    assertEquals(3, util.min(3, 5))
    assertEquals(3, util.min(5, 3))
    assertEquals(5, util.min(5, 5))
    assertEquals(-1, util.min(-1, 0))
  }

  @Test
  fun testIsNullOrEmpty() {
    assertTrue(util.isNullOrEmpty(null))
    assertTrue(util.isNullOrEmpty(""))
    assertFalse(util.isNullOrEmpty("test"))
    assertFalse(util.isNullOrEmpty(" "))
  }

  @Test
  fun testReverseString() {
    assertEquals("cba", util.reverseString("abc"))
    assertEquals("", util.reverseString(""))
    assertEquals("a", util.reverseString("a"))
    assertEquals("54321", util.reverseString("12345"))
  }
}
