package com.android.joinme.model.event

import org.junit.Assert.*
import org.junit.Test

class EventsRepositoryProviderTest {

  @Test
  fun usesLocalRepo_whenOffline() {
    val repo = EventsRepositoryProvider.getRepository(isOnline = false)
    assertTrue(repo is EventsRepositoryLocal)
  }

  //    @Test
  //    fun usesFirestoreRepo_whenOnline() {
  //        val repo = EventsRepositoryProvider.getRepository(isOnline = true)
  //        assertTrue(repo is EventsRepositoryFirestore)
  //    }
  // firestore still not working
}
