package com.android.joinme.utils

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.Assert.*
import org.junit.Test

/** Sanity test to ensure Firestore emulator works properly with our Events collection. */
class FirestoreEventsTest : FirestoreBaseTest() {

  private val firestore = FirebaseFirestore.getInstance()

  @Test
  fun canInsertAndReadEvent() = runBlocking {
    val eventData =
        mapOf(
            "title" to "Test Event",
            "type" to "SOCIAL",
            "location" to "Lausanne",
            "date" to "2025-10-13")

    val docRef = firestore.collection("events").add(eventData).await()
    val snapshot = docRef.get().await()

    assertTrue(snapshot.exists())
    assertEquals("Test Event", snapshot.getString("title"))
  }

  @Test
  fun deletingEventRemovesItFromCollection() = runBlocking {
    val docRef = firestore.collection("events").add(mapOf("title" to "Temp Event")).await()

    // Ensure created
    assertTrue(docRef.get().await().exists())

    // Delete and confirm gone
    docRef.delete().await()
    val snapshot = docRef.get().await()
    assertFalse(snapshot.exists())
  }
}
