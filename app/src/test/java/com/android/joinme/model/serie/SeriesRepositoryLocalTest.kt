package com.android.joinme.model.serie

import com.android.joinme.model.utils.Visibility
import com.google.firebase.Timestamp
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class SeriesRepositoryLocalTest {

  private lateinit var repo: SeriesRepositoryLocal
  private lateinit var sampleSerie: Serie

  @Before
  fun setup() {
    repo = SeriesRepositoryLocal()
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
      repo.addSerie(sampleSerie)
      repo.deleteSerie("1")
      val all = repo.getAllSeries(SerieFilter.SERIES_FOR_OVERVIEW_SCREEN)
      Assert.assertTrue(all.isEmpty())
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
  fun editSerie_preservesOtherSeries() {
    runBlocking {
      val s1 = sampleSerie
      val s2 = sampleSerie.copy(serieId = "2", title = "Basketball Series")
      repo.addSerie(s1)
      repo.addSerie(s2)

      val updated = s1.copy(title = "Updated Football")
      repo.editSerie("1", updated)

      val serie1 = repo.getSerie("1")
      val serie2 = repo.getSerie("2")
      Assert.assertEquals("Updated Football", serie1.title)
      Assert.assertEquals("Basketball Series", serie2.title)
    }
  }

  @Test
  fun deleteSerie_preservesOtherSeries() {
    runBlocking {
      val s1 = sampleSerie
      val s2 = sampleSerie.copy(serieId = "2", title = "Basketball Series")
      val s3 = sampleSerie.copy(serieId = "3", title = "Tennis Series")
      repo.addSerie(s1)
      repo.addSerie(s2)
      repo.addSerie(s3)

      repo.deleteSerie("2")

      val all = repo.getAllSeries(SerieFilter.SERIES_FOR_OVERVIEW_SCREEN)
      Assert.assertEquals(2, all.size)
      Assert.assertTrue(all.any { it.serieId == "1" })
      Assert.assertTrue(all.any { it.serieId == "3" })
      Assert.assertFalse(all.any { it.serieId == "2" })
    }
  }

  @Test
  fun editSerie_withDifferentVisibility() {
    runBlocking {
      repo.addSerie(sampleSerie)
      val updated = sampleSerie.copy(visibility = Visibility.PRIVATE)
      repo.editSerie("1", updated)
      val serie = repo.getSerie("1")
      Assert.assertEquals(Visibility.PRIVATE, serie.visibility)
    }
  }

  @Test
  fun editSerie_withDifferentEventIds() {
    runBlocking {
      repo.addSerie(sampleSerie)
      val updated = sampleSerie.copy(eventIds = listOf("event4", "event5"))
      repo.editSerie("1", updated)
      val serie = repo.getSerie("1")
      Assert.assertEquals(2, serie.eventIds.size)
      Assert.assertTrue(serie.eventIds.contains("event4"))
      Assert.assertTrue(serie.eventIds.contains("event5"))
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
  fun addSerie_withPrivateVisibility() {
    runBlocking {
      val privateSerie = sampleSerie.copy(serieId = "2", visibility = Visibility.PRIVATE)
      repo.addSerie(privateSerie)
      val serie = repo.getSerie("2")
      Assert.assertEquals(Visibility.PRIVATE, serie.visibility)
    }
  }

  @Test
  fun addSerie_withParticipants() {
    runBlocking {
      repo.addSerie(sampleSerie)
      val serie = repo.getSerie("1")
      Assert.assertEquals(2, serie.participants.size)
      Assert.assertTrue(serie.participants.contains("user1"))
      Assert.assertTrue(serie.participants.contains("user2"))
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
  fun editSerie_withDifferentParticipants() {
    runBlocking {
      repo.addSerie(sampleSerie)
      val updated = sampleSerie.copy(participants = listOf("user3", "user4", "user5"))
      repo.editSerie("1", updated)
      val serie = repo.getSerie("1")
      Assert.assertEquals(3, serie.participants.size)
      Assert.assertTrue(serie.participants.contains("user3"))
      Assert.assertTrue(serie.participants.contains("user4"))
      Assert.assertTrue(serie.participants.contains("user5"))
      Assert.assertFalse(serie.participants.contains("user1"))
    }
  }

  @Test
  fun editSerie_addingParticipant() {
    runBlocking {
      repo.addSerie(sampleSerie)
      val updatedParticipants = sampleSerie.participants + "user3"
      val updated = sampleSerie.copy(participants = updatedParticipants)
      repo.editSerie("1", updated)
      val serie = repo.getSerie("1")
      Assert.assertEquals(3, serie.participants.size)
      Assert.assertTrue(serie.participants.contains("user3"))
    }
  }

  @Test
  fun editSerie_removingParticipant() {
    runBlocking {
      repo.addSerie(sampleSerie)
      val updatedParticipants = listOf("user1")
      val updated = sampleSerie.copy(participants = updatedParticipants)
      repo.editSerie("1", updated)
      val serie = repo.getSerie("1")
      Assert.assertEquals(1, serie.participants.size)
      Assert.assertTrue(serie.participants.contains("user1"))
      Assert.assertFalse(serie.participants.contains("user2"))
    }
  }
}
