package com.android.joinme.utils

/**
 * Utility class for mathematical operations. This class is used to test SonarCloud code coverage
 * reporting.
 */
object MathUtils {

  /**
   * Adds two integers and returns the result.
   *
   * @param a First number
   * @param b Second number
   * @return Sum of a and b
   */
  fun add(a: Int, b: Int): Int {
    return a + b
  }

  /**
   * Subtracts b from a and returns the result.
   *
   * @param a First number
   * @param b Second number
   * @return Difference of a and b
   */
  fun subtract(a: Int, b: Int): Int {
    return a - b
  }

  /**
   * Multiplies two integers and returns the result.
   *
   * @param a First number
   * @param b Second number
   * @return Product of a and b
   */
  fun multiply(a: Int, b: Int): Int {
    return a * b
  }

  /**
   * Divides a by b and returns the result.
   *
   * @param a Dividend
   * @param b Divisor
   * @return Quotient of a divided by b
   * @throws ArithmeticException if b is zero
   */
  fun divide(a: Int, b: Int): Int {
    if (b == 0) {
      throw ArithmeticException("Division by zero is not allowed")
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

  /**
   * Calculates the factorial of a non-negative integer.
   *
   * @param n Non-negative integer
   * @return Factorial of n
   * @throws IllegalArgumentException if n is negative
   */
  fun factorial(n: Int): Long {
    if (n < 0) {
      throw IllegalArgumentException("Factorial is not defined for negative numbers")
    }
    if (n == 0 || n == 1) {
      return 1
    }
    var result = 1L
    for (i in 2..n) {
      result *= i
    }
    return result
  }

  /**
   * Calculates the maximum of two integers.
   *
   * @param a First number
   * @param b Second number
   * @return Maximum of a and b
   */
  fun max(a: Int, b: Int): Int {
    return if (a > b) a else b
  }

  /**
   * Calculates the minimum of two integers.
   *
   * @param a First number
   * @param b Second number
   * @return Minimum of a and b
   */
  fun min(a: Int, b: Int): Int {
    return if (a < b) a else b
  }
}
