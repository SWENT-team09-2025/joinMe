package com.android.joinme.model.map

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.joinme.ui.map.MapUIState
import com.android.joinme.ui.map.MapViewModel
import com.android.joinme.ui.map.userLocation.UserLocationService
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class MapViewModelTest {

  @get:Rule val instantExecutorRule = InstantTaskExecutorRule()

  private lateinit var viewModel: MapViewModel
  private lateinit var mockLocationService: UserLocationService

  @Before
  fun setup() {
    val context = ApplicationProvider.getApplicationContext<Context>()

    // Initialisation Firebase si nécessaire
    if (FirebaseApp.getApps(context).isEmpty()) {
      val options =
          FirebaseOptions.Builder()
              .setApplicationId("1:1234567890:android:abcdef") // fake id
              .setApiKey("fakeKey")
              .setProjectId("fakeProject")
              .build()
      FirebaseApp.initializeApp(context, options)
    }

    // Mock du service de localisation
    mockLocationService = mock(UserLocationService::class.java)

    // ViewModel à tester
    viewModel = MapViewModel()
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
  fun `clearErrorMsg clears error message`() = runTest {
    // Reflection pour setter l'état avec erreur
    val stateField = viewModel.javaClass.getDeclaredField("_uiState")
    stateField.isAccessible = true
    val mutableState = stateField.get(viewModel) as MutableStateFlow<MapUIState>
    mutableState.value = MapUIState(errorMsg = "Test error")

    viewModel.clearErrorMsg()

    assertNull(viewModel.uiState.value.errorMsg)
  }

  @Test
  fun `initLocationService starts location updates`() = runTest {
    val testLocation = UserLocation(latitude = 46.5187, longitude = 6.5629, accuracy = 5.0f)

    whenever(mockLocationService.getUserLocationFlow()).thenReturn(flowOf(testLocation))

    viewModel.initLocationService(mockLocationService)

    // Attendre que le flow soit collecté
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
    // Fournir un flux pour éviter NullPointerException
    whenever(mockLocationService.getUserLocationFlow())
        .thenReturn(flowOf(UserLocation(0.0, 0.0, 0.0f)))

    viewModel.initLocationService(mockLocationService)

    // Appeler onCleared via reflection
    val onClearedMethod = viewModel.javaClass.superclass?.getDeclaredMethod("onCleared")
    onClearedMethod?.isAccessible = true
    onClearedMethod?.invoke(viewModel)

    verify(mockLocationService).stopLocationUpdates()
  }
}
