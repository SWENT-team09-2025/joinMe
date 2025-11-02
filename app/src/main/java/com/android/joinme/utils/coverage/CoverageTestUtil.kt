package com.android.joinme.utils.coverage

/**
 * Simple utility class for testing CI coverage pipeline. This file is only for verifying SonarCloud
 * coverage functionality.
 */
class CoverageTestUtil {

  /** Adds two numbers together */
  fun add(a: Int, b: Int): Int {
    return a + b
  }

  /** Subtracts second number from first */
  fun subtract(a: Int, b: Int): Int {
    return a - b
  }

  /** Multiplies two numbers */
  fun multiply(a: Int, b: Int): Int {
    return a * b
  }

  /**
   * Divides first number by second
   *
   * @throws IllegalArgumentException if divisor is zero
   */
  fun divide(a: Int, b: Int): Int {
    if (b == 0) {
      throw IllegalArgumentException("Cannot divide by zero")
    }
    return a / b
  }

  /** Checks if a number is even */
  fun isEven(number: Int): Boolean {
    return number % 2 == 0
  }

  /** Checks if a number is positive */
  fun isPositive(number: Int): Boolean {
    return number > 0
  }

  /** Returns the maximum of two numbers */
  fun max(a: Int, b: Int): Int {
    return if (a > b) a else b
  }

  /** Returns the minimum of two numbers */
  fun min(a: Int, b: Int): Int {
    return if (a < b) a else b
  }

  /** Checks if a string is empty or null */
  fun isNullOrEmpty(str: String?): Boolean {
    return str.isNullOrEmpty()
  }

  /** Reverses a string */
  fun reverseString(str: String): String {
    return str.reversed()
  }
}
