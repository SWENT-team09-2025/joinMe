package com.android.joinme.utils

import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import kotlinx.coroutines.tasks.await
import okhttp3.OkHttpClient
import okhttp3.Request

/** Utility to configure Firestore to use the local emulator during tests. */
object FirebaseEmulator {

  private const val FIRESTORE_EMULATOR_HOST = "10.0.2.2"
  private const val FIRESTORE_EMULATOR_PORT = 8080
  private const val PROJECT_ID = "joinme-test"

  private var initialized = false

  fun initialize() {
    if (initialized) return
    val context = TestApplicationProvider.context

    // Initialize FirebaseApp if not already
    if (FirebaseApp.getApps(context).isEmpty()) {
      val options =
          FirebaseOptions.Builder()
              .setProjectId(PROJECT_ID)
              .setApplicationId("1:$PROJECT_ID:android:debug")
              .build()
      FirebaseApp.initializeApp(context, options)
    }

    val firestore = FirebaseFirestore.getInstance()

    // Connect to emulator
    firestore.useEmulator(FIRESTORE_EMULATOR_HOST, FIRESTORE_EMULATOR_PORT)

    // Disable persistence for clean test runs
    firestore.firestoreSettings =
        FirebaseFirestoreSettings.Builder().setPersistenceEnabled(false).build()

    initialized = true
    Log.d(
        "FirebaseEmulator",
        "Connected to Firestore emulator on $FIRESTORE_EMULATOR_HOST:$FIRESTORE_EMULATOR_PORT")
  }

  /**
   * Deletes all documents in known test collections. Add more collections here as your tests grow.
   */
  suspend fun clearFirestore() {
    val firestore = FirebaseFirestore.getInstance()
    val collectionNames = listOf("events")

    for (name in collectionNames) {
      val col = firestore.collection(name)
      val snap = col.get().await()
      for (doc in snap.documents) {
        doc.reference.delete().await()
      }
    }
  }

  /** Clears everything via the emulator’s REST API. This is fast and resets all data. */
  fun nukeFirestore() {
    try {
      val url =
          "http://$FIRESTORE_EMULATOR_HOST:$FIRESTORE_EMULATOR_PORT/emulator/v1/projects/$PROJECT_ID/databases/(default)/documents"
      OkHttpClient().newCall(Request.Builder().url(url).delete().build()).execute().close()
      Log.d("FirebaseEmulator", "Emulator data cleared successfully")
    } catch (e: Exception) {
      Log.e("FirebaseEmulator", "Failed to clear emulator: ${e.message}")
    }
  }
}
