package com.android.joinme.model.database

/** This file was implemented with the help of AI * */
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SerieDaoTest {

  private lateinit var database: AppDatabase
  private lateinit var serieDao: SerieDao
  private lateinit var context: Context

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    database =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    serieDao = database.serieDao()
  }

  @After
  fun tearDown() {
    database.close()
    AppDatabase.setTestInstance(null)
  }

  private fun createTestSerie(
      id: String,
      title: String = "Serie $id",
      ownerId: String = "owner1",
      visibility: String = "PUBLIC",
      participantsJson: String = "[]",
      eventIdsJson: String = "[\"event1\"]",
      dateSeconds: Long = 100,
      cachedAt: Long = System.currentTimeMillis()
  ) =
      SerieEntity(
          serieId = id,
          title = title,
          description = "Description for $title",
          dateSeconds = dateSeconds,
          dateNanoseconds = 0,
          participantsJson = participantsJson,
          maxParticipants = 10,
          visibility = visibility,
          eventIdsJson = eventIdsJson,
          ownerId = ownerId,
          lastEventEndTimeSeconds = null,
          lastEventEndTimeNanoseconds = null,
          groupId = null,
          cachedAt = cachedAt)

  @Test
  fun `insertSerie stores and retrieves serie correctly`() = runBlocking {
    val serie = createTestSerie("test1")
    serieDao.insertSerie(serie)

    val retrieved = serieDao.getSerieById("test1")
    assertNotNull(retrieved)
    assertEquals("test1", retrieved?.serieId)
    assertEquals("Serie test1", retrieved?.title)
  }

  @Test
  fun `getSerieById returns null for non-existent serie`() = runBlocking {
    assertNull(serieDao.getSerieById("nonexistent"))
  }

  @Test
  fun `getAllSeries returns all series`() = runBlocking {
    serieDao.insertSerie(createTestSerie("1"))
    serieDao.insertSerie(createTestSerie("2", title = "Basketball", visibility = "PRIVATE"))

    val allSeries = serieDao.getAllSeries()
    assertEquals(2, allSeries.size)
    assertTrue(allSeries.any { it.serieId == "1" })
    assertTrue(allSeries.any { it.serieId == "2" })
  }

  @Test
  fun `insertSerie with REPLACE strategy updates existing serie`() = runBlocking {
    val serie = createTestSerie("test1")
    serieDao.insertSerie(serie)
    serieDao.insertSerie(serie.copy(title = "Updated Title"))

    val retrieved = serieDao.getSerieById("test1")
    assertEquals("Updated Title", retrieved?.title)
    assertEquals(1, serieDao.getAllSeries().size)
  }

  @Test
  fun `insertSeries stores multiple series`() = runBlocking {
    val series = listOf(createTestSerie("1"), createTestSerie("2"), createTestSerie("3"))
    serieDao.insertSeries(series)
    assertEquals(3, serieDao.getAllSeries().size)
  }

  @Test
  fun `insertSeries with REPLACE strategy updates existing series`() = runBlocking {
    val serie1 = createTestSerie("1")
    val serie2 = createTestSerie("2")
    serieDao.insertSeries(listOf(serie1, serie2))

    serieDao.insertSeries(listOf(serie1.copy(title = "Updated")))

    val allSeries = serieDao.getAllSeries()
    assertEquals(2, allSeries.size)
    assertEquals("Updated", allSeries.find { it.serieId == "1" }?.title)
  }

  @Test
  fun `deleteSerie removes serie from database`() = runBlocking {
    serieDao.insertSerie(createTestSerie("delete_me"))
    assertEquals(1, serieDao.getAllSeries().size)

    serieDao.deleteSerie("delete_me")
    assertNull(serieDao.getSerieById("delete_me"))
    assertEquals(0, serieDao.getAllSeries().size)
  }

  @Test
  fun `deleteSerie does nothing for non-existent serie`() = runBlocking {
    serieDao.insertSerie(createTestSerie("test1"))
    serieDao.deleteSerie("nonexistent")
    assertEquals(1, serieDao.getAllSeries().size)
  }

  @Test
  fun `deleteAllSeries removes all series`() = runBlocking {
    repeat(3) { i -> serieDao.insertSerie(createTestSerie("serie$i")) }
    assertEquals(3, serieDao.getAllSeries().size)

    serieDao.deleteAllSeries()
    assertEquals(0, serieDao.getAllSeries().size)
  }

  @Test
  fun `deleteOldSeries removes series older than timestamp`() = runBlocking {
    val currentTime = System.currentTimeMillis()
    serieDao.insertSerie(createTestSerie("old", cachedAt = currentTime - 10000))
    serieDao.insertSerie(createTestSerie("recent", cachedAt = currentTime))

    serieDao.deleteOldSeries(currentTime - 5000)

    val remaining = serieDao.getAllSeries()
    assertEquals(1, remaining.size)
    assertEquals("recent", remaining[0].serieId)
  }

  @Test
  fun `deleteOldSeries keeps all series when timestamp is older than all`() = runBlocking {
    val currentTime = System.currentTimeMillis()
    serieDao.insertSerie(createTestSerie("1", cachedAt = currentTime))
    serieDao.insertSerie(createTestSerie("2", cachedAt = currentTime + 1000))

    serieDao.deleteOldSeries(currentTime - 10000)

    assertEquals(2, serieDao.getAllSeries().size)
  }

  @Test
  fun `serie with all optional fields null stores and retrieves correctly`() = runBlocking {
    val serieMinimal =
        SerieEntity(
            serieId = "minimal",
            title = "Minimal Serie",
            description = "Basic description",
            dateSeconds = 100,
            dateNanoseconds = 0,
            participantsJson = "[]",
            maxParticipants = 5,
            visibility = "PUBLIC",
            eventIdsJson = "[]",
            ownerId = "owner1",
            lastEventEndTimeSeconds = null,
            lastEventEndTimeNanoseconds = null,
            groupId = null)

    serieDao.insertSerie(serieMinimal)

    val retrieved = serieDao.getSerieById("minimal")
    assertNotNull(retrieved)
    assertNull(retrieved?.lastEventEndTimeSeconds)
    assertNull(retrieved?.lastEventEndTimeNanoseconds)
    assertNull(retrieved?.groupId)
  }

  @Test
  fun `serie with lastEventEndTime stores and retrieves correctly`() = runBlocking {
    val serieWithEndTime =
        createTestSerie("withEndTime")
            .copy(lastEventEndTimeSeconds = 200, lastEventEndTimeNanoseconds = 500)

    serieDao.insertSerie(serieWithEndTime)

    val retrieved = serieDao.getSerieById("withEndTime")
    assertNotNull(retrieved)
    assertEquals(200L, retrieved?.lastEventEndTimeSeconds)
    assertEquals(500, retrieved?.lastEventEndTimeNanoseconds)
  }

  @Test
  fun `serie with groupId stores and retrieves correctly`() = runBlocking {
    val serieWithGroup = createTestSerie("withGroup").copy(groupId = "group123")

    serieDao.insertSerie(serieWithGroup)

    val retrieved = serieDao.getSerieById("withGroup")
    assertNotNull(retrieved)
    assertEquals("group123", retrieved?.groupId)
  }

  @Test
  fun `serie with multiple participants stores and retrieves correctly`() = runBlocking {
    val serieWithParticipants =
        createTestSerie("withParticipants")
            .copy(participantsJson = "[\"user1\",\"user2\",\"user3\"]")

    serieDao.insertSerie(serieWithParticipants)

    val retrieved = serieDao.getSerieById("withParticipants")
    assertNotNull(retrieved)
    assertEquals("[\"user1\",\"user2\",\"user3\"]", retrieved?.participantsJson)
  }

  @Test
  fun `serie with multiple events stores and retrieves correctly`() = runBlocking {
    val serieWithEvents =
        createTestSerie("withEvents").copy(eventIdsJson = "[\"event1\",\"event2\",\"event3\"]")

    serieDao.insertSerie(serieWithEvents)

    val retrieved = serieDao.getSerieById("withEvents")
    assertNotNull(retrieved)
    assertEquals("[\"event1\",\"event2\",\"event3\"]", retrieved?.eventIdsJson)
  }

  @Test
  fun `cachedAt timestamp is stored correctly`() = runBlocking {
    val timestamp = System.currentTimeMillis()
    val serie = createTestSerie("timestampTest", cachedAt = timestamp)

    serieDao.insertSerie(serie)

    val retrieved = serieDao.getSerieById("timestampTest")
    assertNotNull(retrieved)
    assertEquals(timestamp, retrieved?.cachedAt)
  }

  @Test
  fun `visibility enum values store correctly`() = runBlocking {
    val publicSerie = createTestSerie("public", visibility = "PUBLIC")
    val privateSerie = createTestSerie("private", visibility = "PRIVATE")

    serieDao.insertSeries(listOf(publicSerie, privateSerie))

    assertEquals("PUBLIC", serieDao.getSerieById("public")?.visibility)
    assertEquals("PRIVATE", serieDao.getSerieById("private")?.visibility)
  }
}
