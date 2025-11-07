package com.android.joinme.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for MathUtils.
 *
 * These tests verify the correctness of basic mathematical operations.
 */
class MathUtilsTest {

  @Test
  fun add_positiveNumbers_returnsCorrectSum() {
    val result = MathUtils.add(5, 3)
    assertEquals(8, result)
  }

  @Test
  fun add_negativeNumbers_returnsCorrectSum() {
    val result = MathUtils.add(-5, -3)
    assertEquals(-8, result)
  }

  @Test
  fun add_mixedNumbers_returnsCorrectSum() {
    val result = MathUtils.add(10, -3)
    assertEquals(7, result)
  }

  @Test
  fun add_withZero_returnsOriginalNumber() {
    val result = MathUtils.add(5, 0)
    assertEquals(5, result)
  }

  @Test
  fun subtract_positiveNumbers_returnsCorrectDifference() {
    val result = MathUtils.subtract(10, 3)
    assertEquals(7, result)
  }

  @Test
  fun subtract_negativeNumbers_returnsCorrectDifference() {
    val result = MathUtils.subtract(-5, -3)
    assertEquals(-2, result)
  }

  @Test
  fun subtract_resultIsNegative_returnsCorrectDifference() {
    val result = MathUtils.subtract(3, 10)
    assertEquals(-7, result)
  }

  @Test
  fun multiply_positiveNumbers_returnsCorrectProduct() {
    val result = MathUtils.multiply(5, 3)
    assertEquals(15, result)
  }

  @Test
  fun multiply_negativeNumbers_returnsCorrectProduct() {
    val result = MathUtils.multiply(-5, -3)
    assertEquals(15, result)
  }

  @Test
  fun multiply_withZero_returnsZero() {
    val result = MathUtils.multiply(5, 0)
    assertEquals(0, result)
  }

  @Test
  fun multiply_mixedNumbers_returnsCorrectProduct() {
    val result = MathUtils.multiply(5, -3)
    assertEquals(-15, result)
  }

  @Test
  fun divide_positiveNumbers_returnsCorrectQuotient() {
    val result = MathUtils.divide(10, 2)
    assertEquals(5, result)
  }

  @Test
  fun divide_negativeNumbers_returnsCorrectQuotient() {
    val result = MathUtils.divide(-10, -2)
    assertEquals(5, result)
  }

  @Test
  fun divide_mixedNumbers_returnsCorrectQuotient() {
    val result = MathUtils.divide(-10, 2)
    assertEquals(-5, result)
  }

  @Test
  fun divide_byZero_throwsIllegalArgumentException() {
    val exception = assertThrows(IllegalArgumentException::class.java) { MathUtils.divide(10, 0) }
    assertEquals("Cannot divide by zero", exception.message)
  }

  @Test
  fun isEven_evenNumber_returnsTrue() {
    assertTrue(MathUtils.isEven(4))
    assertTrue(MathUtils.isEven(0))
    assertTrue(MathUtils.isEven(-2))
  }

  @Test
  fun isEven_oddNumber_returnsFalse() {
    assertFalse(MathUtils.isEven(3))
    assertFalse(MathUtils.isEven(1))
    assertFalse(MathUtils.isEven(-3))
  }

  @Test
  fun isPositive_positiveNumber_returnsTrue() {
    assertTrue(MathUtils.isPositive(1))
    assertTrue(MathUtils.isPositive(100))
  }

  @Test
  fun isPositive_negativeNumber_returnsFalse() {
    assertFalse(MathUtils.isPositive(-1))
    assertFalse(MathUtils.isPositive(-100))
  }

  @Test
  fun isPositive_zero_returnsFalse() {
    assertFalse(MathUtils.isPositive(0))
  }
}
