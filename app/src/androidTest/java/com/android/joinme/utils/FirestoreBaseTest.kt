package com.android.joinme.utils

import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before

/** Base test class that ensures Firestore emulator is initialized and cleaned up. */
open class FirestoreBaseTest {

  @Before
  fun setupFirestore() {
    FirebaseEmulator.initialize()
    FirebaseEmulator.nukeFirestore()
  }

  @After fun teardownFirestore() = runBlocking { FirebaseEmulator.clearFirestore() }
}
