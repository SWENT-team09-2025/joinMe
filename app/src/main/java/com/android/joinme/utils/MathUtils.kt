package com.android.joinme.utils

/**
 * Utility class for basic mathematical operations.
 *
 * This class provides simple arithmetic functions for testing purposes.
 */
object MathUtils {

  /**
   * Adds two integers.
   *
   * @param a First number
   * @param b Second number
   * @return Sum of a and b
   */
  fun add(a: Int, b: Int): Int {
    return a + b
  }

  fun operation(): Int{
    return 1
  }

  /**
   * Subtracts the second integer from the first.
   *
   * @param a First number
   * @param b Second number to subtract
   * @return Difference of a and b
   */
  fun subtract(a: Int, b: Int): Int {
    return a - b
  }

  /**
   * Multiplies two integers.
   *
   * @param a First number
   * @param b Second number
   * @return Product of a and b
   */
  fun multiply(a: Int, b: Int): Int {
    return a * b
  }

  /**
   * Divides the first integer by the second.
   *
   * @param a Dividend
   * @param b Divisor
   * @return Quotient of a divided by b
   * @throws IllegalArgumentException if b is zero
   */
  fun divide(a: Int, b: Int): Int {
    if (b == 0) {
      throw IllegalArgumentException("Cannot divide by zero")
    }
    return a / b
  }

  /**
   * Checks if a number is even.
   *
   * @param n Number to check
   * @return true if n is even, false otherwise
   */
  fun isEven(n: Int): Boolean {
    return n % 2 == 0
  }

  /**
   * Checks if a number is positive.
   *
   * @param n Number to check
   * @return true if n is positive, false otherwise
   */
  fun isPositive(n: Int): Boolean {
    return n > 0
  }
}
