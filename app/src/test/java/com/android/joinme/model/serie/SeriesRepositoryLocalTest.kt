package com.android.joinme.model.serie

import com.android.joinme.model.event.Event
import com.android.joinme.model.event.EventType
import com.android.joinme.model.event.EventVisibility
import com.android.joinme.model.event.EventsRepositoryLocal
import com.android.joinme.model.utils.Visibility
import com.google.firebase.Timestamp
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class SeriesRepositoryLocalTest {

  private lateinit var repo: SeriesRepositoryLocal
  private lateinit var eventsRepo: EventsRepositoryLocal
  private lateinit var sampleSerie: Serie

  @Before
  fun setup() {
    eventsRepo = EventsRepositoryLocal()
    repo = SeriesRepositoryLocal(eventsRepo)
    sampleSerie =
        Serie(
            serieId = "1",
            title = "Weekly Football",
            description = "Weekly football series at the park",
            date = Timestamp.now(),
            participants = listOf("user1", "user2"),
            maxParticipants = 10,
            visibility = Visibility.PUBLIC,
            eventIds = listOf("event1", "event2", "event3"),
            ownerId = "user1")
  }

  // ---------------- BASIC CRUD ----------------

  @Test
  fun addAndGetSerie_success() {
    runBlocking {
      repo.addSerie(sampleSerie)
      val serie = repo.getSerie("1")
      Assert.assertEquals(sampleSerie.title, serie.title)
    }
  }

  @Test(expected = Exception::class)
  fun getSerie_notFound_throwsException() {
    runBlocking { repo.getSerie("unknown") }
  }

  @Test
  fun editSerie_updatesSuccessfully() {
    runBlocking {
      repo.addSerie(sampleSerie)
      val updated = sampleSerie.copy(title = "Updated Title")
      repo.editSerie("1", updated)
      val serie = repo.getSerie("1")
      Assert.assertEquals("Updated Title", serie.title)
    }
  }

  @Test
  fun deleteSerie_removesSuccessfully() {
    runBlocking {
      // Add events that are referenced by the serie
      val event1 =
          Event(
              eventId = "event1",
              type = EventType.SPORTS,
              title = "Event 1",
              description = "First event",
              location = null,
              date = Timestamp.now(),
              duration = 60,
              participants = listOf("user1"),
              maxParticipants = 10,
              visibility = EventVisibility.PUBLIC,
              ownerId = "user1")
      val event2 =
          Event(
              eventId = "event2",
              type = EventType.SPORTS,
              title = "Event 2",
              description = "Second event",
              location = null,
              date = Timestamp.now(),
              duration = 60,
              participants = listOf("user1"),
              maxParticipants = 10,
              visibility = EventVisibility.PUBLIC,
              ownerId = "user1")
      val event3 =
          Event(
              eventId = "event3",
              type = EventType.SPORTS,
              title = "Event 3",
              description = "Third event",
              location = null,
              date = Timestamp.now(),
              duration = 60,
              participants = listOf("user1"),
              maxParticipants = 10,
              visibility = EventVisibility.PUBLIC,
              ownerId = "user1")

      eventsRepo.addEvent(event1)
      eventsRepo.addEvent(event2)
      eventsRepo.addEvent(event3)

      // Add the serie
      repo.addSerie(sampleSerie)

      // Delete the serie
      repo.deleteSerie("1")

      // Verify serie is deleted
      val allSeries = repo.getAllSeries(SerieFilter.SERIES_FOR_OVERVIEW_SCREEN)
      Assert.assertTrue(allSeries.isEmpty())

      // Verify all associated events are also deleted
      Assert.assertThrows(Exception::class.java) { runBlocking { eventsRepo.getEvent("event1") } }
      Assert.assertThrows(Exception::class.java) { runBlocking { eventsRepo.getEvent("event2") } }
      Assert.assertThrows(Exception::class.java) { runBlocking { eventsRepo.getEvent("event3") } }
    }
  }

  // ---------------- ADDITIONAL TESTS ----------------

  @Test
  fun addMultipleSeries_storesAll() {
    runBlocking {
      val s1 = sampleSerie
      val s2 = sampleSerie.copy(serieId = "2", title = "Basketball Series")
      val s3 = sampleSerie.copy(serieId = "3", title = "Tennis Series")
      repo.addSerie(s1)
      repo.addSerie(s2)
      repo.addSerie(s3)

      val all = repo.getAllSeries(SerieFilter.SERIES_FOR_OVERVIEW_SCREEN)
      Assert.assertEquals(3, all.size)
      Assert.assertTrue(all.any { it.title == "Tennis Series" })
    }
  }

  @Test
  fun getNewSerieId_incrementsSequentially() {
    val id1 = repo.getNewSerieId()
    val id2 = repo.getNewSerieId()
    val id3 = repo.getNewSerieId()
    Assert.assertEquals((id1.toInt() + 1).toString(), id2)
    Assert.assertEquals((id2.toInt() + 1).toString(), id3)
  }

  @Test(expected = Exception::class)
  fun editSerie_notFound_throwsException() {
    runBlocking {
      val fake = sampleSerie.copy(serieId = "999", title = "DoesNotExist")
      repo.editSerie(fake.serieId, fake)
    }
  }

  @Test(expected = Exception::class)
  fun deleteSerie_notFound_throwsException() {
    runBlocking {
      repo.addSerie(sampleSerie)
      repo.deleteSerie("nonexistent")
    }
  }

  @Test
  fun addDuplicateId_keepsOriginalOnGet() {
    runBlocking {
      repo.addSerie(sampleSerie)
      val duplicate = sampleSerie.copy(title = "Duplicate Serie")
      repo.addSerie(duplicate)

      // getSerie returns the first matching item (original)
      val fetched = repo.getSerie("1")
      Assert.assertEquals("Weekly Football", fetched.title)

      // and both entries exist with the same ID
      val allWithSameId =
          repo.getAllSeries(SerieFilter.SERIES_FOR_OVERVIEW_SCREEN).filter { it.serieId == "1" }
      Assert.assertEquals(2, allWithSameId.size)
    }
  }

  @Test
  fun getAllSeries_returnsEmptyListInitially() {
    runBlocking {
      val all = repo.getAllSeries(SerieFilter.SERIES_FOR_OVERVIEW_SCREEN)
      Assert.assertTrue(all.isEmpty())
    }
  }

  @Test
  fun addSerie_withEmptyEventIds() {
    runBlocking {
      val emptyEventsSerie = sampleSerie.copy(serieId = "2", eventIds = emptyList())
      repo.addSerie(emptyEventsSerie)
      val serie = repo.getSerie("2")
      Assert.assertTrue(serie.eventIds.isEmpty())
    }
  }

  @Test
  fun addSerie_withEmptyParticipants() {
    runBlocking {
      val emptyParticipantsSerie = sampleSerie.copy(serieId = "2", participants = emptyList())
      repo.addSerie(emptyParticipantsSerie)
      val serie = repo.getSerie("2")
      Assert.assertTrue(serie.participants.isEmpty())
    }
  }

  @Test
  fun clear_removesAllSeriesAndResetsCounter() {
    runBlocking {
      repo.addSerie(sampleSerie)
      repo.addSerie(sampleSerie.copy(serieId = "2"))
      repo.getNewSerieId() // increment counter

      repo.clear()

      Assert.assertTrue(repo.getAllSeries(SerieFilter.SERIES_FOR_OVERVIEW_SCREEN).isEmpty())
      Assert.assertEquals("0", repo.getNewSerieId())
    }
  }

  @Test
  fun getSeriesByIds_returnsMatchingSeries() {
    runBlocking {
      val s1 = sampleSerie.copy(serieId = "1", title = "Serie 1")
      val s2 = sampleSerie.copy(serieId = "2", title = "Serie 2")
      val s3 = sampleSerie.copy(serieId = "3", title = "Serie 3")
      repo.addSerie(s1)
      repo.addSerie(s2)
      repo.addSerie(s3)

      val result = repo.getSeriesByIds(listOf("1", "3"))

      Assert.assertEquals(2, result.size)
      Assert.assertTrue(result.any { it.serieId == "1" })
      Assert.assertTrue(result.any { it.serieId == "3" })
      Assert.assertFalse(result.any { it.serieId == "2" })
    }
  }

  @Test
  fun getSeriesByIds_returnsEmptyListWhenNoMatches() {
    runBlocking {
      repo.addSerie(sampleSerie.copy(serieId = "1"))

      val result = repo.getSeriesByIds(listOf("999", "888"))

      Assert.assertTrue(result.isEmpty())
    }
  }

  @Test
  fun getSeriesByIds_returnsEmptyListWhenEmptyInput() {
    runBlocking {
      repo.addSerie(sampleSerie)

      val result = repo.getSeriesByIds(emptyList())

      Assert.assertTrue(result.isEmpty())
    }
  }
}
