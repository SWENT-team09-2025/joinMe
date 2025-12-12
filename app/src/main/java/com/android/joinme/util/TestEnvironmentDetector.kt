package com.android.joinme.util

/**
 * Utility object for detecting if the application is running in a test environment.
 *
 * This centralized utility consolidates all test environment detection logic to ensure consistent
 * behavior across the application.
 */
object TestEnvironmentDetector {

  /**
   * Checks if the application is currently running in a test environment.
   *
   * Detection methods include:
   * - Robolectric test framework (Build.FINGERPRINT == "robolectric")
   * - Debugger connection (Debug.isDebuggerConnected())
   * - System property flag (IS_TEST_ENV)
   * - Android instrumentation test runner
   *
   * @return true if running in a test environment, false otherwise
   */
  fun isTestEnvironment(): Boolean {
    return android.os.Build.FINGERPRINT == "robolectric" ||
        android.os.Debug.isDebuggerConnected() ||
        System.getProperty("IS_TEST_ENV") == "true" ||
        try {
          Class.forName("androidx.test.runner.AndroidJUnitRunner")
          true
        } catch (e: ClassNotFoundException) {
          false
        }
  }

  /**
   * Returns a test user ID for use in test environments.
   *
   * This is useful when Firebase authentication is not available or mocked during testing.
   *
   * @return "test-user-id" constant for test environments
   */
  fun getTestUserId(): String = "test-user-id"
}
