package com.android.joinme.model.event

import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

/**
 * Provides a single instance of the repository in the app. 'repository' is mutable for testing
 * purposes.
 */
object EventsRepositoryProvider {

  private val firestoreRepo: EventsRepository by lazy {
    EventsRepositoryFirestore(db = Firebase.firestore)
  }

  private val localRepo: EventsRepository by lazy { EventsRepositoryLocal() }

  fun getRepository(isOnline: Boolean): EventsRepository {
    return if (isOnline) firestoreRepo else localRepo
  }
}
