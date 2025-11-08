package com.android.joinme.model.map

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.joinme.model.event.Event
import com.android.joinme.model.event.EventFilter
import com.android.joinme.model.event.EventType
import com.android.joinme.model.event.EventVisibility
import com.android.joinme.model.event.EventsRepository
import com.android.joinme.ui.map.MapUIState
import com.android.joinme.ui.map.MapViewModel
import com.android.joinme.ui.map.userLocation.UserLocationService
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.Timestamp
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class MapViewModelTest {

  @get:Rule val instantExecutorRule = InstantTaskExecutorRule()

  private val testDispatcher = StandardTestDispatcher()
  private lateinit var viewModel: MapViewModel
  private lateinit var mockLocationService: UserLocationService
  private lateinit var mockEventsRepository: EventsRepository

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)

    val context = ApplicationProvider.getApplicationContext<Context>()

    if (FirebaseApp.getApps(context).isEmpty()) {
      val options =
          FirebaseOptions.Builder()
              .setApplicationId("1:1234567890:android:abcdef")
              .setApiKey("fakeKey")
              .setProjectId("fakeProject")
              .build()
      FirebaseApp.initializeApp(context, options)
    }

    mockLocationService = mock(UserLocationService::class.java)
    mockEventsRepository = mock(EventsRepository::class.java)

    viewModel = MapViewModel(eventsRepository = mockEventsRepository)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `initial state is correct`() = runTest {
    val initialState = viewModel.uiState.value

    assertNull(initialState.userLocation)
    assertTrue(initialState.todos.isEmpty())
    assertNull(initialState.errorMsg)
    assertFalse(initialState.isLoading)
  }

    @Test
    fun `clearErrorMsg is tested with initial error`() = runTest {
        val stateField = viewModel.javaClass.getDeclaredField("_uiState")
        stateField.isAccessible = true
        val mutableState = stateField.get(viewModel) as MutableStateFlow<MapUIState>
        mutableState.value = MapUIState(errorMsg = "Some error")

        assertEquals("Some error", viewModel.uiState.value.errorMsg)

        viewModel.clearErrorMsg()

        assertNull(viewModel.uiState.value.errorMsg)
    }
  @Test
  fun `initLocationService starts location updates`() = runTest {
    val testLocation = UserLocation(latitude = 46.5187, longitude = 6.5629, accuracy = 5.0f)

    whenever(mockLocationService.getUserLocationFlow()).thenReturn(flowOf(testLocation))

    viewModel.initLocationService(mockLocationService)
    advanceUntilIdle()

    assertEquals(testLocation, viewModel.uiState.value.userLocation)
  }

  @Test
  fun `location service with null location is filtered`() = runTest {
    whenever(mockLocationService.getUserLocationFlow()).thenReturn(flowOf(null))

    viewModel.initLocationService(mockLocationService)
    advanceUntilIdle()

    assertNull(viewModel.uiState.value.userLocation)
  }

  @Test
  fun `onCleared stops location service`() = runTest {
    whenever(mockLocationService.getUserLocationFlow())
        .thenReturn(flowOf(UserLocation(0.0, 0.0, 0.0f)))

    viewModel.initLocationService(mockLocationService)

    val onClearedMethod = viewModel.javaClass.superclass?.getDeclaredMethod("onCleared")
    onClearedMethod?.isAccessible = true
    onClearedMethod?.invoke(viewModel)

    verify(mockLocationService).stopLocationUpdates()
  }


  @Test
  fun `fetchLocalizableEvents loads events successfully`() =
      runTest(testDispatcher) {
        // Create test events
        val testEvents =
            listOf(
                Event(
                    eventId = "event1",
                    type = EventType.SPORTS,
                    title = "Test Event",
                    description = "Description",
                    location = Location(46.5, 6.6, "Test Location"),
                    date = Timestamp.now(),
                    duration = 60,
                    participants = emptyList(),
                    maxParticipants = 10,
                    visibility = EventVisibility.PUBLIC,
                    ownerId = "owner1"))

        // Mock repository to return test events
        whenever(mockEventsRepository.getAllEvents(EventFilter.EVENTS_FOR_MAP_SCREEN))
            .thenReturn(testEvents)

        // Call fetchLocalizableEvents via reflection
        val method = viewModel.javaClass.getDeclaredMethod("fetchLocalizableEvents")
        method.isAccessible = true
        method.invoke(viewModel)

        // Advance coroutines
        advanceUntilIdle()

        // Verify the state is updated with events
        assertEquals(testEvents, viewModel.uiState.value.todos)
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.errorMsg)
      }

  @Test
  fun `fetchLocalizableEvents handles error correctly`() =
      runTest(testDispatcher) {
        // Mock repository to throw exception
        whenever(mockEventsRepository.getAllEvents(any()))
            .thenThrow(RuntimeException("Network error"))

        // Call fetchLocalizableEvents via reflection
        val method = viewModel.javaClass.getDeclaredMethod("fetchLocalizableEvents")
        method.isAccessible = true
        method.invoke(viewModel)

        // Advance coroutines
        advanceUntilIdle()

        // Verify error state
        assertTrue(viewModel.uiState.value.todos.isEmpty())
        assertFalse(viewModel.uiState.value.isLoading)
        assertNotNull(viewModel.uiState.value.errorMsg)
        assertTrue(viewModel.uiState.value.errorMsg!!.contains("Failed to load events"))
      }
}
