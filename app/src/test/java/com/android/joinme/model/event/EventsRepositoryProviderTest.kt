package com.android.joinme.model.event

import org.junit.Assert.*
import org.junit.Test

class EventsRepositoryProviderTest {

  @Test
  fun usesLocalRepo_whenOffline() {
    val repo = EventsRepositoryProvider.getRepository(isOnline = false)
    assertTrue(repo is EventsRepositoryLocal)
  }
}
