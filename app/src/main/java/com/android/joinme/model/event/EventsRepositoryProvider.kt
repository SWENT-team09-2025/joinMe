package com.android.joinme.model.event

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

/**
 * Provides the correct [EventsRepository] implementation depending on connectivity. Uses Firestore
 * when online, local repository otherwise.
 */
object EventsRepositoryProvider {

  // Local repository (in-memory)
  private val localRepo: EventsRepository by lazy { EventsRepositoryLocal() }

  // Firestore repository (initialized lazily)
  private var firestoreRepo: EventsRepository? = null

  /**
   * Returns the appropriate repository based on network availability.
   *
   * @param isOnline whether the app is online
   * @param context required for initializing Firebase if needed
   */
  fun getRepository(isOnline: Boolean, context: Context? = null): EventsRepository {
    val isTestEnv =
        android.os.Build.FINGERPRINT == "robolectric" ||
            android.os.Debug.isDebuggerConnected() ||
            System.getProperty("IS_TEST_ENV") == "true" ||
            try {
              // Check if we're running in an instrumented test by looking for the test runner
              Class.forName("androidx.test.runner.AndroidJUnitRunner")
              true
            } catch (e: ClassNotFoundException) {
              false
            }

    return if (isTestEnv || !isOnline) localRepo else getFirestoreRepo(context)
  }

  private fun getFirestoreRepo(context: Context?): EventsRepository {
    if (firestoreRepo == null) {
      val ctx = context ?: FirebaseApp.getInstance().applicationContext
      val apps = FirebaseApp.getApps(ctx)
      if (apps.isEmpty()) {
        FirebaseApp.initializeApp(ctx)
      }
      firestoreRepo = EventsRepositoryFirestore(Firebase.firestore, ctx)
    }
    return firestoreRepo!!
  }
}
