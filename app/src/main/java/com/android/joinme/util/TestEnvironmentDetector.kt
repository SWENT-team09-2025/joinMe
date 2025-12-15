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
   * Used by repository providers to determine whether to return local or Firestore repositories.
   *
   * Detection methods include:
   * - Robolectric test framework (Build.FINGERPRINT == "robolectric")
   * - Debugger connection (Debug.isDebuggerConnected())
   * - System property flag (IS_TEST_ENV)
   * - Android instrumentation test runner (AndroidJUnitRunner)
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
   * Checks if code should use a test user ID instead of Firebase authentication.
   *
   * This is more restrictive than [isTestEnvironment] - it does NOT detect unit tests
   * (AndroidJUnitRunner), allowing unit tests to properly test authentication failure scenarios.
   *
   * Detection methods include:
   * - Robolectric test framework (Build.FINGERPRINT == "robolectric")
   * - Debugger connection (Debug.isDebuggerConnected())
   * - System property flag (IS_TEST_ENV)
   *
   * @return true if a test user ID should be used, false otherwise
   */
  fun shouldUseTestUserId(): Boolean {
    return android.os.Build.FINGERPRINT == "robolectric" ||
        android.os.Debug.isDebuggerConnected() ||
        System.getProperty("IS_TEST_ENV") == "true"
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
