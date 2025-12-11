package com.android.joinme.model.serie
/** This file was implemented with the help of AI * */
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.android.joinme.model.database.AppDatabase
import com.android.joinme.model.database.toEntity
import com.android.joinme.model.event.OfflineException
import com.android.joinme.model.utils.Visibility
import com.android.joinme.network.NetworkMonitor
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.*
import java.util.Calendar
import java.util.Date
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SeriesRepositoryCachedTest {

  private lateinit var context: Context
  private lateinit var database: AppDatabase
  private lateinit var firestoreRepo: SeriesRepository
  private lateinit var networkMonitor: NetworkMonitor
  private lateinit var cachedRepo: SeriesRepositoryCached
  private lateinit var mockAuth: FirebaseAuth
  private lateinit var mockUser: FirebaseUser

  private val currentTimestamp = Timestamp(Date())

  private fun createSerie(
      id: String,
      title: String = "Serie $id",
      visibility: Visibility = Visibility.PUBLIC,
      ownerId: String = "owner1",
      participants: List<String> = emptyList(),
      date: Timestamp = currentTimestamp,
      eventIds: List<String> = listOf("event1"),
      lastEventEndTime: Timestamp? = null,
      groupId: String? = null
  ): Serie {
    val serie =
        Serie(
            serieId = id,
            title = title,
            description = "Description for $title",
            date = date,
            participants = participants,
            maxParticipants = 10,
            visibility = visibility,
            eventIds = eventIds,
            ownerId = ownerId,
            groupId = groupId)
    return if (lastEventEndTime != null) {
      serie.copy(lastEventEndTime = lastEventEndTime)
    } else {
      serie
    }
  }

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    database =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    AppDatabase.setTestInstance(database)

    firestoreRepo = mockk(relaxed = true)
    networkMonitor = mockk(relaxed = true)

    // Mock Firebase Auth
    mockAuth = mockk(relaxed = true)
    mockUser = mockk(relaxed = true)
    mockkStatic(FirebaseAuth::class)
    every { FirebaseAuth.getInstance() } returns mockAuth
    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns "testUser"

    cachedRepo = SeriesRepositoryCached(context, firestoreRepo, networkMonitor)
  }

  @After
  fun tearDown() {
    database.close()
    AppDatabase.setTestInstance(null)
    unmockkAll()
  }

  // ========== getNewSerieId Tests ==========

  @Test
  fun `getNewSerieId delegates to firestore repository`() = runBlocking {
    every { firestoreRepo.getNewSerieId() } returns "newId123"

    val result = cachedRepo.getNewSerieId()

    assertEquals("newId123", result)
    verify { firestoreRepo.getNewSerieId() }
  }

  // ========== getAllSeries Tests ==========

  @Test
  fun `getAllSeries fetches from firestore when online`() = runBlocking {
    val series = listOf(createSerie("1"), createSerie("2"))
    every { networkMonitor.isOnline() } returns true
    coEvery { firestoreRepo.getAllSeries(any()) } returns series

    val result = cachedRepo.getAllSeries(SerieFilter.SERIES_FOR_OVERVIEW_SCREEN)

    assertEquals(2, result.size)
    coVerify { firestoreRepo.getAllSeries(SerieFilter.SERIES_FOR_OVERVIEW_SCREEN) }
  }

  @Test
  fun `getAllSeries caches series from firestore`() = runBlocking {
    val series = listOf(createSerie("1", participants = listOf("testUser")))
    every { networkMonitor.isOnline() } returns true
    coEvery { firestoreRepo.getAllSeries(any()) } returns series

    cachedRepo.getAllSeries(SerieFilter.SERIES_FOR_OVERVIEW_SCREEN)

    // Verify serie was cached
    val cached = database.serieDao().getSerieById("1")
    assertNotNull(cached)
    assertEquals("Serie 1", cached?.title)
  }

  @Test
  fun `getAllSeries returns cached data when offline`() = runBlocking {
    val serie = createSerie("1", participants = listOf("testUser"))
    database.serieDao().insertSerie(serie.toEntity())

    every { networkMonitor.isOnline() } returns false

    val result = cachedRepo.getAllSeries(SerieFilter.SERIES_FOR_OVERVIEW_SCREEN)

    assertEquals(1, result.size)
    assertEquals("1", result[0].serieId)
  }

  @Test
  fun `getAllSeries applies OVERVIEW filter correctly when offline`() = runBlocking {
    val serie1 = createSerie("1", participants = listOf("testUser", "other"))
    val serie2 = createSerie("2", participants = listOf("other"))
    database.serieDao().insertSeries(listOf(serie1.toEntity(), serie2.toEntity()))

    every { networkMonitor.isOnline() } returns false

    val result = cachedRepo.getAllSeries(SerieFilter.SERIES_FOR_OVERVIEW_SCREEN)

    assertEquals(1, result.size)
    assertEquals("1", result[0].serieId)
  }

  @Test
  fun `getAllSeries applies HISTORY filter correctly when offline`() = runBlocking {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.HOUR, -3)
    val pastDate = Timestamp(calendar.time)
    val pastEndTime = Timestamp(calendar.apply { add(Calendar.HOUR, -2) }.time)

    val expiredSerie =
        createSerie(
            "1", participants = listOf("testUser"), date = pastDate, lastEventEndTime = pastEndTime)
    val futureSerie = createSerie("2", participants = listOf("testUser"))

    database.serieDao().insertSeries(listOf(expiredSerie.toEntity(), futureSerie.toEntity()))

    every { networkMonitor.isOnline() } returns false

    val result = cachedRepo.getAllSeries(SerieFilter.SERIES_FOR_HISTORY_SCREEN)

    assertEquals(1, result.size)
    assertEquals("1", result[0].serieId)
  }

  @Test
  fun `getAllSeries applies SEARCH filter correctly when offline`() = runBlocking {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.HOUR, 2)
    val futureDate = Timestamp(calendar.time)

    val publicUpcomingSerie =
        createSerie(
            "1", visibility = Visibility.PUBLIC, date = futureDate, participants = listOf("other"))
    val privateSerie =
        createSerie(
            "2", visibility = Visibility.PRIVATE, date = futureDate, participants = listOf("other"))
    val userParticipatingSerie =
        createSerie(
            "3",
            visibility = Visibility.PUBLIC,
            date = futureDate,
            participants = listOf("testUser"))

    database
        .serieDao()
        .insertSeries(
            listOf(
                publicUpcomingSerie.toEntity(),
                privateSerie.toEntity(),
                userParticipatingSerie.toEntity()))

    every { networkMonitor.isOnline() } returns false

    val result = cachedRepo.getAllSeries(SerieFilter.SERIES_FOR_SEARCH_SCREEN)

    assertEquals(1, result.size)
    assertEquals("1", result[0].serieId)
  }

  @Test
  fun `getAllSeries applies MAP filter correctly when offline`() = runBlocking {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.HOUR, 2)
    val futureDate = Timestamp(calendar.time)

    val calendarPast = Calendar.getInstance()
    calendarPast.add(Calendar.HOUR, -2)
    val pastDate = Timestamp(calendarPast.time)
    val activeEndTime = Timestamp(Calendar.getInstance().apply { add(Calendar.HOUR, 1) }.time)

    val upcomingSerie = createSerie("1", date = futureDate)
    val activeSerie =
        createSerie(
            "2",
            date = pastDate,
            lastEventEndTime = activeEndTime,
            participants = listOf("testUser"))
    val expiredSerie =
        createSerie("3", date = pastDate, lastEventEndTime = Timestamp(calendarPast.time))

    database
        .serieDao()
        .insertSeries(
            listOf(upcomingSerie.toEntity(), activeSerie.toEntity(), expiredSerie.toEntity()))

    every { networkMonitor.isOnline() } returns false

    val result = cachedRepo.getAllSeries(SerieFilter.SERIES_FOR_MAP_SCREEN)

    assertEquals(2, result.size)
    assertTrue(result.any { it.serieId == "1" })
    assertTrue(result.any { it.serieId == "2" })
  }

  @Test
  fun `getAllSeries falls back to cache when firestore throws exception`() = runBlocking {
    val cachedSerie = createSerie("1", participants = listOf("testUser"))
    database.serieDao().insertSerie(cachedSerie.toEntity())

    every { networkMonitor.isOnline() } returns true
    coEvery { firestoreRepo.getAllSeries(any()) } throws Exception("Network error")

    val result = cachedRepo.getAllSeries(SerieFilter.SERIES_FOR_OVERVIEW_SCREEN)

    assertEquals(1, result.size)
    assertEquals("1", result[0].serieId)
  }

  @Test
  fun `getAllSeries throws exception when offline and no auth user`() = runBlocking {
    every { networkMonitor.isOnline() } returns false
    every { mockAuth.currentUser } returns null

    try {
      cachedRepo.getAllSeries(SerieFilter.SERIES_FOR_OVERVIEW_SCREEN)
      fail("Should have thrown Exception")
    } catch (e: Exception) {
      assertTrue(e.message!!.contains("User not logged in"))
    }
  }

  @Test
  fun `getAllSeries does not cache empty list from firestore`() = runBlocking {
    every { networkMonitor.isOnline() } returns true
    coEvery { firestoreRepo.getAllSeries(any()) } returns emptyList()

    val result = cachedRepo.getAllSeries(SerieFilter.SERIES_FOR_OVERVIEW_SCREEN)

    assertTrue(result.isEmpty())
    assertEquals(0, database.serieDao().getAllSeries().size)
  }

  // ========== getSerie Tests ==========

  @Test
  fun `getSerie fetches from firestore when online and caches result`() = runBlocking {
    val serie = createSerie("1")
    every { networkMonitor.isOnline() } returns true
    coEvery { firestoreRepo.getSerie("1") } returns serie

    val result = cachedRepo.getSerie("1")

    assertEquals("1", result.serieId)
    assertNotNull(database.serieDao().getSerieById("1"))
    coVerify { firestoreRepo.getSerie("1") }
  }

  @Test
  fun `getSerie returns cached version when offline`() = runBlocking {
    val serie = createSerie("1")
    database.serieDao().insertSerie(serie.toEntity())

    every { networkMonitor.isOnline() } returns false

    val result = cachedRepo.getSerie("1")

    assertEquals("1", result.serieId)
    coVerify(exactly = 0) { firestoreRepo.getSerie(any()) }
  }

  @Test
  fun `getSerie throws OfflineException when offline and no cache`() = runBlocking {
    every { networkMonitor.isOnline() } returns false

    try {
      cachedRepo.getSerie("nonexistent")
      fail("Should have thrown OfflineException")
    } catch (e: OfflineException) {
      assertTrue(e.message!!.contains("offline"))
    }
  }

  @Test
  fun `getSerie falls back to cache when firestore fails`() = runBlocking {
    val serie = createSerie("1")
    database.serieDao().insertSerie(serie.toEntity())

    every { networkMonitor.isOnline() } returns true
    coEvery { firestoreRepo.getSerie("1") } throws Exception("Network error")

    val result = cachedRepo.getSerie("1")

    assertEquals("1", result.serieId)
  }

  @Test
  fun `getSerie throws exception when firestore fails and no cache`() = runBlocking {
    every { networkMonitor.isOnline() } returns true
    coEvery { firestoreRepo.getSerie("1") } throws Exception("Network error")

    try {
      cachedRepo.getSerie("1")
      fail("Should have thrown exception")
    } catch (e: Exception) {
      assertEquals("Network error", e.message)
    }
  }

  // ========== addSerie Tests ==========

  @Test
  fun `addSerie adds to firestore and caches when online`() = runBlocking {
    val serie = createSerie("1")
    every { networkMonitor.isOnline() } returns true
    coEvery { firestoreRepo.addSerie(serie) } just Runs

    cachedRepo.addSerie(serie)

    coVerify { firestoreRepo.addSerie(serie) }
    assertNotNull(database.serieDao().getSerieById("1"))
  }

  @Test
  fun `addSerie throws OfflineException when offline`() = runBlocking {
    val serie = createSerie("1")
    every { networkMonitor.isOnline() } returns false

    try {
      cachedRepo.addSerie(serie)
      fail("Should have thrown OfflineException")
    } catch (e: OfflineException) {
      assertTrue(e.message!!.contains("internet connection"))
    }

    coVerify(exactly = 0) { firestoreRepo.addSerie(any()) }
  }

  // ========== editSerie Tests ==========

  @Test
  fun `editSerie updates firestore and cache when online`() = runBlocking {
    val serie = createSerie("1", title = "Updated Title")
    every { networkMonitor.isOnline() } returns true
    coEvery { firestoreRepo.editSerie("1", serie) } just Runs

    cachedRepo.editSerie("1", serie)

    coVerify { firestoreRepo.editSerie("1", serie) }
    val cached = database.serieDao().getSerieById("1")
    assertEquals("Updated Title", cached?.title)
  }

  @Test
  fun `editSerie throws OfflineException when offline`() = runBlocking {
    val serie = createSerie("1")
    every { networkMonitor.isOnline() } returns false

    try {
      cachedRepo.editSerie("1", serie)
      fail("Should have thrown OfflineException")
    } catch (e: OfflineException) {
      assertTrue(e.message!!.contains("internet connection"))
    }

    coVerify(exactly = 0) { firestoreRepo.editSerie(any(), any()) }
  }

  // ========== deleteSerie Tests ==========

  @Test
  fun `deleteSerie removes from firestore and cache when online`() = runBlocking {
    val serie = createSerie("1")
    database.serieDao().insertSerie(serie.toEntity())

    every { networkMonitor.isOnline() } returns true
    coEvery { firestoreRepo.deleteSerie("1") } just Runs

    cachedRepo.deleteSerie("1")

    coVerify { firestoreRepo.deleteSerie("1") }
    assertNull(database.serieDao().getSerieById("1"))
  }

  @Test
  fun `deleteSerie throws OfflineException when offline`() = runBlocking {
    every { networkMonitor.isOnline() } returns false

    try {
      cachedRepo.deleteSerie("1")
      fail("Should have thrown OfflineException")
    } catch (e: OfflineException) {
      assertTrue(e.message!!.contains("internet connection"))
    }

    coVerify(exactly = 0) { firestoreRepo.deleteSerie(any()) }
  }

  // ========== getSeriesByIds Tests ==========

  @Test
  fun `getSeriesByIds fetches from firestore when online and caches`() = runBlocking {
    val series = listOf(createSerie("1"), createSerie("2"))
    every { networkMonitor.isOnline() } returns true
    coEvery { firestoreRepo.getSeriesByIds(listOf("1", "2")) } returns series

    val result = cachedRepo.getSeriesByIds(listOf("1", "2"))

    assertEquals(2, result.size)
    assertNotNull(database.serieDao().getSerieById("1"))
    assertNotNull(database.serieDao().getSerieById("2"))
  }

  @Test
  fun `getSeriesByIds returns cached series when offline`() = runBlocking {
    val serie1 = createSerie("1")
    val serie2 = createSerie("2")
    database.serieDao().insertSeries(listOf(serie1.toEntity(), serie2.toEntity()))

    every { networkMonitor.isOnline() } returns false

    val result = cachedRepo.getSeriesByIds(listOf("1", "2"))

    assertEquals(2, result.size)
    coVerify(exactly = 0) { firestoreRepo.getSeriesByIds(any()) }
  }

  @Test
  fun `getSeriesByIds returns only cached series available when offline`() = runBlocking {
    val serie1 = createSerie("1")
    database.serieDao().insertSerie(serie1.toEntity())

    every { networkMonitor.isOnline() } returns false

    val result = cachedRepo.getSeriesByIds(listOf("1", "2", "3"))

    assertEquals(1, result.size)
    assertEquals("1", result[0].serieId)
  }

  @Test
  fun `getSeriesByIds falls back to cache when firestore fails`() = runBlocking {
    val serie = createSerie("1")
    database.serieDao().insertSerie(serie.toEntity())

    every { networkMonitor.isOnline() } returns true
    coEvery { firestoreRepo.getSeriesByIds(any()) } throws Exception("Network error")

    val result = cachedRepo.getSeriesByIds(listOf("1"))

    assertEquals(1, result.size)
    assertEquals("1", result[0].serieId)
  }

  @Test
  fun `getSeriesByIds does not cache empty list from firestore`() = runBlocking {
    every { networkMonitor.isOnline() } returns true
    coEvery { firestoreRepo.getSeriesByIds(any()) } returns emptyList()

    val result = cachedRepo.getSeriesByIds(listOf("1", "2"))

    assertTrue(result.isEmpty())
    assertEquals(0, database.serieDao().getAllSeries().size)
  }
}
