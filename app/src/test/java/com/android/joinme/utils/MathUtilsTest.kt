package com.android.joinme.utils

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MathUtilsTest {

  @Test
  fun add_returnsSumOfTwoNumbers() {
    assertEquals(5, MathUtils.add(2, 3))
    assertEquals(0, MathUtils.add(-5, 5))
    assertEquals(-10, MathUtils.add(-5, -5))
  }

  @Test
  fun subtract_returnsDifferenceOfTwoNumbers() {
    assertEquals(2, MathUtils.subtract(5, 3))
    assertEquals(-10, MathUtils.subtract(-5, 5))
    assertEquals(0, MathUtils.subtract(5, 5))
  }

  @Test
  fun multiply_returnsProductOfTwoNumbers() {
    assertEquals(15, MathUtils.multiply(3, 5))
    assertEquals(-25, MathUtils.multiply(-5, 5))
    assertEquals(0, MathUtils.multiply(0, 5))
  }

  @Test
  fun divide_returnsQuotientOfTwoNumbers() {
    assertEquals(2, MathUtils.divide(10, 5))
    assertEquals(-2, MathUtils.divide(-10, 5))
    assertEquals(0, MathUtils.divide(0, 5))
  }

  @Test(expected = ArithmeticException::class)
  fun divide_throwsExceptionWhenDividingByZero() {
    MathUtils.divide(10, 0)
  }

  @Test
  fun isEven_returnsTrueForEvenNumbers() {
    assertTrue(MathUtils.isEven(0))
    assertTrue(MathUtils.isEven(2))
    assertTrue(MathUtils.isEven(-4))
  }

  @Test
  fun isEven_returnsFalseForOddNumbers() {
    assertFalse(MathUtils.isEven(1))
    assertFalse(MathUtils.isEven(3))
    assertFalse(MathUtils.isEven(-5))
  }

  @Test
  fun isPositive_returnsTrueForPositiveNumbers() {
    assertTrue(MathUtils.isPositive(1))
    assertTrue(MathUtils.isPositive(100))
  }

  @Test
  fun isPositive_returnsFalseForZeroAndNegativeNumbers() {
    assertFalse(MathUtils.isPositive(0))
    assertFalse(MathUtils.isPositive(-1))
    assertFalse(MathUtils.isPositive(-100))
  }

  @Test
  fun factorial_returnsCorrectFactorial() {
    assertEquals(1L, MathUtils.factorial(0))
    assertEquals(1L, MathUtils.factorial(1))
    assertEquals(2L, MathUtils.factorial(2))
    assertEquals(6L, MathUtils.factorial(3))
    assertEquals(24L, MathUtils.factorial(4))
    assertEquals(120L, MathUtils.factorial(5))
  }

  @Test(expected = IllegalArgumentException::class)
  fun factorial_throwsExceptionForNegativeNumbers() {
    MathUtils.factorial(-1)
  }

  @Test
  fun max_returnsMaximumOfTwoNumbers() {
    assertEquals(5, MathUtils.max(3, 5))
    assertEquals(5, MathUtils.max(5, 3))
    assertEquals(5, MathUtils.max(5, 5))
    assertEquals(0, MathUtils.max(-5, 0))
  }

  @Test
  fun min_returnsMinimumOfTwoNumbers() {
    assertEquals(3, MathUtils.min(3, 5))
    assertEquals(3, MathUtils.min(5, 3))
    assertEquals(5, MathUtils.min(5, 5))
    assertEquals(-5, MathUtils.min(-5, 0))
  }
}
