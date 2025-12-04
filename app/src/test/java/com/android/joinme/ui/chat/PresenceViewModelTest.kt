package com.android.joinme.ui.chat

// Implemented with help of Claude AI

import com.android.joinme.model.presence.PresenceRepositoryLocal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for PresenceViewModel.
 *
 * Verifies initialization and state updates from repository.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PresenceViewModelTest {

  private lateinit var viewModel: PresenceViewModel
  private lateinit var repository: PresenceRepositoryLocal
  private val testDispatcher = StandardTestDispatcher()

  private val testContextId = "chat123"
  private val currentUserId = "currentUser"

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    repository = PresenceRepositoryLocal()
    viewModel = PresenceViewModel(repository)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  // ============ Initialize ============

  @Test
  fun initialize_blankInputs_doesNotChangeState() = runTest {
    val initialState = viewModel.presenceState.value

    viewModel.initialize("", currentUserId)
    viewModel.initialize(testContextId, "")
    viewModel.initialize("", "")

    assertEquals(initialState, viewModel.presenceState.value)
  }

  @Test
  fun initialize_validInputs_startsObservingAndUpdatesState() = runTest {
    viewModel.initialize(testContextId, currentUserId)
    advanceUntilIdle()

    assertFalse(viewModel.presenceState.value.isLoading)
    assertEquals(0, viewModel.presenceState.value.onlineUsersCount)
    assertTrue(viewModel.presenceState.value.onlineUserIds.isEmpty())
  }

  @Test
  fun initialize_sameContextTwice_doesNotReinitialize() = runTest {
    viewModel.initialize(testContextId, currentUserId)
    advanceUntilIdle()

    // Add users after first init
    repository.setUserOnline("user1", listOf(testContextId))
    advanceUntilIdle()
    val countAfterFirstInit = viewModel.presenceState.value.onlineUsersCount

    // Re-initialize with same context - should not reset
    viewModel.initialize(testContextId, currentUserId)
    advanceUntilIdle()

    assertEquals(countAfterFirstInit, viewModel.presenceState.value.onlineUsersCount)
  }

  @Test
  fun initialize_differentContext_reinitializes() = runTest {
    viewModel.initialize(testContextId, currentUserId)
    advanceUntilIdle()

    repository.setUserOnline("user1", listOf(testContextId))
    advanceUntilIdle()
    assertEquals(1, viewModel.presenceState.value.onlineUsersCount)

    // Initialize with different context
    viewModel.initialize("differentChat", currentUserId)
    advanceUntilIdle()

    assertEquals(0, viewModel.presenceState.value.onlineUsersCount)
  }

  // ============ State Updates ============

  @Test
  fun presenceState_updatesWhenUsersGoOnlineAndOffline() = runTest {
    viewModel.initialize(testContextId, currentUserId)
    advanceUntilIdle()

    // Add users
    repository.setUserOnline("user1", listOf(testContextId))
    repository.setUserOnline("user2", listOf(testContextId))
    advanceUntilIdle()

    assertEquals(2, viewModel.presenceState.value.onlineUsersCount)
    assertTrue(viewModel.presenceState.value.onlineUserIds.containsAll(listOf("user1", "user2")))

    // User goes offline
    repository.setUserOffline("user1")
    advanceUntilIdle()

    assertEquals(1, viewModel.presenceState.value.onlineUsersCount)
    assertFalse(viewModel.presenceState.value.onlineUserIds.contains("user1"))
    assertTrue(viewModel.presenceState.value.onlineUserIds.contains("user2"))
  }

  @Test
  fun presenceState_excludesCurrentUser() = runTest {
    viewModel.initialize(testContextId, currentUserId)
    advanceUntilIdle()

    // Current user goes online - should not be counted
    repository.setUserOnline(currentUserId, listOf(testContextId))
    repository.setUserOnline("otherUser", listOf(testContextId))
    advanceUntilIdle()

    assertEquals(1, viewModel.presenceState.value.onlineUsersCount)
    assertFalse(viewModel.presenceState.value.onlineUserIds.contains(currentUserId))
  }
}
