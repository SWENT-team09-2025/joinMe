package com.android.joinme.utils

import android.content.Context
import androidx.test.core.app.ApplicationProvider

/**
 * Provides a test-safe application context. Used to initialize Firebase or repositories in
 * instrumentation tests.
 */
object TestApplicationProvider {
  val context: Context
    get() = ApplicationProvider.getApplicationContext()
}
