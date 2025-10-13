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
  fun getRepository(isOnline: Boolean = true, context: Context? = null): EventsRepository {
    return if (isOnline) getFirestoreRepo(context) else localRepo
  }

  private fun getFirestoreRepo(context: Context?): EventsRepository {
    if (firestoreRepo == null) {
      if (FirebaseApp.getApps(context ?: FirebaseApp.getInstance().applicationContext).isEmpty()) {
        requireNotNull(context) { "Context is required to initialize Firebase" }
        FirebaseApp.initializeApp(context)
      }
      firestoreRepo = EventsRepositoryFirestore(Firebase.firestore)
    }
    return firestoreRepo!!
  }
}
