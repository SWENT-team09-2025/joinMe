package com.android.joinme.model.event

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.joinme.model.map.Location
import com.google.firebase.FirebaseApp
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EventsRepositoryFirestoreTest {

  private lateinit var repo: EventsRepositoryFirestore
  private lateinit var db: FirebaseFirestore

  private val event =
      Event(
          eventId = "testEvent123",
          type = EventType.SOCIAL,
          title = "Coffee Meetup",
          description = "Grab a coffee at EPFL Café",
          // Location is nullable in your model — use a concrete value that matches your type
          location = Location(46.52, 6.63, "EPFL Café"),
          date = Timestamp.now(),
          duration = 60,
          participants = emptyList(),
          maxParticipants = 5,
          visibility = EventVisibility.PUBLIC,
          ownerId = "testUser")

  @Before
  fun setup() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    if (FirebaseApp.getApps(context).isEmpty()) {
      FirebaseApp.initializeApp(context)
    }
    db = FirebaseFirestore.getInstance()
    // Point to emulator (adjust port if your emulator uses a different one)
    db.useEmulator("10.0.2.2", 8080)

    repo = EventsRepositoryFirestore(db)
  }

  @After
  fun cleanup() = runBlocking {
    // Best-effort cleanup so tests are idempotent
    try {
      repo.deleteEvent(event.eventId)
    } catch (_: Exception) {}
  }

  @Test
  fun addAndGetEvent_firestore_roundTrip() = runBlocking {
    repo.addEvent(event)
    val fetched = repo.getEvent(event.eventId)
    assertEquals(event.title, fetched.title)
    assertEquals(event.description, fetched.description)
    assertEquals(event.type, fetched.type)
    assertEquals(event.visibility, fetched.visibility)
    assertEquals(event.ownerId, fetched.ownerId)
    // location is optional: verify the saved name to match your data class
    assertEquals(event.location?.name, fetched.location?.name)
  }

  @Test
  fun editEvent_updatesDocument() = runBlocking {
    repo.addEvent(event)
    val updated = event.copy(title = "Updated Coffee Meetup", maxParticipants = 8)
    repo.editEvent(event.eventId, updated)
    val fetched = repo.getEvent(event.eventId)
    assertEquals("Updated Coffee Meetup", fetched.title)
    assertEquals(8, fetched.maxParticipants)
  }

  @Test
  fun deleteEvent_thenGetByIdThrows() = runBlocking {
    repo.addEvent(event)
    repo.deleteEvent(event.eventId)

    var threw = false
    try {
      // getEvent should throw after deletion
      repo.getEvent(event.eventId)
    } catch (_: Exception) {
      threw = true
    }
    assertTrue("Expected getEvent to throw after deletion", threw)
  }
}
