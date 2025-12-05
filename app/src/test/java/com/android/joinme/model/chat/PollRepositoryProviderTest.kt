package com.android.joinme.model.chat

// Implemented with help of Claude AI

import io.mockk.mockk
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tests for [PollRepositoryProvider].
 *
 * Verifies the repository provider pattern works correctly for dependency injection and testing.
 */
@RunWith(RobolectricTestRunner::class)
class PollRepositoryProviderTest {

  @Before
  fun setUp() {
    // Set a local repository to prevent lazy initialization of Firebase-dependent repository
    PollRepositoryProvider.repository = PollRepositoryLocal()
  }

  @After
  fun tearDown() {
    PollRepositoryProvider.repository = PollRepositoryLocal()
    unmockkAll()
  }

  @Test
  fun repository_returnsNonNullPollRepositoryInstance() {
    val repo = PollRepositoryProvider.repository

    assertNotNull(repo)
    assertTrue(repo is PollRepository)
  }

  @Test
  fun repository_canBeSwappedAndReturnsSameInstance() {
    val mockRepo = mockk<PollRepository>(relaxed = true)
    PollRepositoryProvider.repository = mockRepo

    assertSame(mockRepo, PollRepositoryProvider.repository)
    assertSame(PollRepositoryProvider.repository, PollRepositoryProvider.repository)
  }

  @Test
  fun repository_swappedLocalRepositoryIsFullyFunctional() {
    val localRepo = PollRepositoryLocal()
    PollRepositoryProvider.repository = localRepo

    assertTrue(PollRepositoryProvider.repository is PollRepositoryLocal)

    val pollId = PollRepositoryProvider.repository.getNewPollId()
    assertNotNull(pollId)
    assertTrue(pollId.isNotEmpty())
  }
}
