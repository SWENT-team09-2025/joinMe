package com.android.joinme.ui.profile

/** This file was implemented with the help of AI */
import com.android.joinme.model.profile.Profile
import com.android.joinme.model.profile.ProfileRepository
import com.google.firebase.Timestamp
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FollowListViewModelTest {

  private val testDispatcher = StandardTestDispatcher()

  private lateinit var mockProfileRepository: ProfileRepository
  private lateinit var viewModel: FollowListViewModel

  private val testUserId = "test-user-id"
  private val testUsername = "TestUser"

  private val testProfile =
      Profile(
          uid = testUserId,
          username = testUsername,
          email = "test@example.com",
          dateOfBirth = "01/01/1990",
          country = "Switzerland",
          interests = listOf("Testing"),
          bio = "Test bio",
          photoUrl = null,
          createdAt = Timestamp.now(),
          updatedAt = Timestamp.now(),
          eventsJoinedCount = 5,
          followersCount = 10,
          followingCount = 15)

  private val follower1 =
      Profile(
          uid = "follower1", username = "Follower1", email = "follower1@example.com", bio = "Bio 1")

  private val follower2 =
      Profile(
          uid = "follower2", username = "Follower2", email = "follower2@example.com", bio = "Bio 2")

  private val following1 =
      Profile(
          uid = "following1",
          username = "Following1",
          email = "following1@example.com",
          bio = "Bio 1")

  private val following2 =
      Profile(
          uid = "following2",
          username = "Following2",
          email = "following2@example.com",
          bio = "Bio 2")

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    mockProfileRepository = mockk(relaxed = true)
    viewModel = FollowListViewModel(mockProfileRepository)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
    clearAllMocks()
  }

  // ==================== INITIAL STATE TESTS ====================

  @Test
  fun `initial state is correct`() {
    assertEquals(FollowTab.FOLLOWERS, viewModel.selectedTab.value)
    assertEquals("", viewModel.profileUsername.value)
    assertTrue(viewModel.followers.value.isEmpty())
    assertTrue(viewModel.following.value.isEmpty())
    assertFalse(viewModel.isLoading.value)
    assertNull(viewModel.error.value)
  }

  // ==================== INITIALIZATION TESTS ====================

  @Test
  fun `initialize loads profile username and followers by default`() = runTest {
    coEvery { mockProfileRepository.getProfile(testUserId) } returns testProfile
    coEvery { mockProfileRepository.getFollowers(testUserId, any()) } returns
        listOf(follower1, follower2)

    viewModel.initialize(testUserId)
    testDispatcher.scheduler.advanceUntilIdle()

    assertEquals(testUsername, viewModel.profileUsername.value)
    assertEquals(FollowTab.FOLLOWERS, viewModel.selectedTab.value)
    assertEquals(2, viewModel.followers.value.size)
    assertTrue(viewModel.following.value.isEmpty())
    assertFalse(viewModel.isLoading.value)
    assertNull(viewModel.error.value)

    coVerify { mockProfileRepository.getProfile(testUserId) }
    coVerify { mockProfileRepository.getFollowers(testUserId, 50) }
    coVerify(exactly = 0) { mockProfileRepository.getFollowing(any(), any()) }
  }

  @Test
  fun `initialize with FOLLOWING tab loads following list`() = runTest {
    coEvery { mockProfileRepository.getProfile(testUserId) } returns testProfile
    coEvery { mockProfileRepository.getFollowing(testUserId, any()) } returns
        listOf(following1, following2)

    viewModel.initialize(testUserId, FollowTab.FOLLOWING)
    testDispatcher.scheduler.advanceUntilIdle()

    assertEquals(testUsername, viewModel.profileUsername.value)
    assertEquals(FollowTab.FOLLOWING, viewModel.selectedTab.value)
    assertEquals(2, viewModel.following.value.size)
    assertTrue(viewModel.followers.value.isEmpty())
    assertFalse(viewModel.isLoading.value)

    coVerify { mockProfileRepository.getProfile(testUserId) }
    coVerify { mockProfileRepository.getFollowing(testUserId, 50) }
    coVerify(exactly = 0) { mockProfileRepository.getFollowers(any(), any()) }
  }

  @Test
  fun `initialize sets loading state correctly`() = runTest {
    coEvery { mockProfileRepository.getProfile(testUserId) } returns testProfile
    coEvery { mockProfileRepository.getFollowers(testUserId, any()) } coAnswers
        {
          assertTrue(viewModel.isLoading.value)
          listOf(follower1)
        }

    assertFalse(viewModel.isLoading.value)

    viewModel.initialize(testUserId)
    testDispatcher.scheduler.advanceUntilIdle()

    assertFalse(viewModel.isLoading.value)
  }

  @Test
  fun `initialize handles null profile username gracefully`() = runTest {
    coEvery { mockProfileRepository.getProfile(testUserId) } returns null
    coEvery { mockProfileRepository.getFollowers(testUserId, any()) } returns emptyList()

    viewModel.initialize(testUserId)
    testDispatcher.scheduler.advanceUntilIdle()

    assertEquals("", viewModel.profileUsername.value)
    assertTrue(viewModel.followers.value.isEmpty())
    assertFalse(viewModel.isLoading.value)
  }

  @Test
  fun `initialize handles username fetch exception`() = runTest {
    coEvery { mockProfileRepository.getProfile(testUserId) } throws Exception("Network error")
    coEvery { mockProfileRepository.getFollowers(testUserId, any()) } returns
        listOf(follower1, follower2)

    viewModel.initialize(testUserId)
    testDispatcher.scheduler.advanceUntilIdle()

    assertEquals("", viewModel.profileUsername.value)
    assertEquals(2, viewModel.followers.value.size)
    assertFalse(viewModel.isLoading.value)
  }

  // ==================== TAB SWITCHING / LAZY LOADING TESTS ====================

  @Test
  fun `lazy loading works correctly across multiple tab switches`() = runTest {
    coEvery { mockProfileRepository.getProfile(testUserId) } returns testProfile
    coEvery { mockProfileRepository.getFollowers(testUserId, any()) } returns
        listOf(follower1, follower2)
    coEvery { mockProfileRepository.getFollowing(testUserId, any()) } returns
        listOf(following1, following2)

    // Initialize with FOLLOWERS
    viewModel.initialize(testUserId, FollowTab.FOLLOWERS)
    testDispatcher.scheduler.advanceUntilIdle()

    assertEquals(FollowTab.FOLLOWERS, viewModel.selectedTab.value)
    assertEquals(2, viewModel.followers.value.size)
    assertTrue(viewModel.following.value.isEmpty())

    // Switch to FOLLOWING - should load data
    viewModel.selectTab(FollowTab.FOLLOWING)
    testDispatcher.scheduler.advanceUntilIdle()

    assertEquals(FollowTab.FOLLOWING, viewModel.selectedTab.value)
    assertEquals(2, viewModel.following.value.size)

    // Switch back to FOLLOWERS - should NOT reload
    viewModel.selectTab(FollowTab.FOLLOWERS)
    testDispatcher.scheduler.advanceUntilIdle()

    // Switch to FOLLOWING again - should NOT reload
    viewModel.selectTab(FollowTab.FOLLOWING)
    testDispatcher.scheduler.advanceUntilIdle()

    // Each should only be loaded once (lazy loading + caching)
    coVerify(exactly = 1) { mockProfileRepository.getFollowers(testUserId, 50) }
    coVerify(exactly = 1) { mockProfileRepository.getFollowing(testUserId, 50) }
  }

  @Test
  fun `selectTab without initialize sets error`() = runTest {
    assertNull(viewModel.error.value)

    viewModel.selectTab(FollowTab.FOLLOWING)
    testDispatcher.scheduler.advanceUntilIdle()

    assertEquals("User ID not set", viewModel.error.value)
  }

  // ==================== ERROR HANDLING TESTS ====================

  @Test
  fun `initialize handles fetch exceptions on both tabs`() = runTest {
    coEvery { mockProfileRepository.getProfile(testUserId) } returns testProfile

    // Test FOLLOWERS tab error
    coEvery { mockProfileRepository.getFollowers(testUserId, any()) } throws
        Exception("Network error")

    viewModel.initialize(testUserId, FollowTab.FOLLOWERS)
    testDispatcher.scheduler.advanceUntilIdle()

    assertNotNull(viewModel.error.value)
    assertTrue(viewModel.error.value!!.contains("Failed to load followers"))
    assertTrue(viewModel.followers.value.isEmpty())
    assertFalse(viewModel.isLoading.value)

    // Test FOLLOWING tab error
    coEvery { mockProfileRepository.getFollowing(testUserId, any()) } throws
        Exception("Database error")

    viewModel.clearError()
    viewModel.initialize(testUserId, FollowTab.FOLLOWING)
    testDispatcher.scheduler.advanceUntilIdle()

    assertNotNull(viewModel.error.value)
    assertTrue(viewModel.error.value!!.contains("Failed to load following"))
    assertTrue(viewModel.following.value.isEmpty())
    assertFalse(viewModel.isLoading.value)
  }

  @Test
  fun `selectTab handles exception when switching tabs`() = runTest {
    coEvery { mockProfileRepository.getProfile(testUserId) } returns testProfile
    coEvery { mockProfileRepository.getFollowers(testUserId, any()) } returns listOf(follower1)
    coEvery { mockProfileRepository.getFollowing(testUserId, any()) } throws
        Exception("Server error")

    viewModel.initialize(testUserId)
    testDispatcher.scheduler.advanceUntilIdle()

    assertNull(viewModel.error.value)

    viewModel.selectTab(FollowTab.FOLLOWING)
    testDispatcher.scheduler.advanceUntilIdle()

    assertNotNull(viewModel.error.value)
    assertTrue(viewModel.error.value!!.contains("Failed to load following"))
  }

  @Test
  fun `clearError clears error state`() = runTest {
    coEvery { mockProfileRepository.getProfile(testUserId) } returns testProfile
    coEvery { mockProfileRepository.getFollowers(testUserId, any()) } throws
        Exception("Network error")

    viewModel.initialize(testUserId)
    testDispatcher.scheduler.advanceUntilIdle()

    assertNotNull(viewModel.error.value)

    viewModel.clearError()

    assertNull(viewModel.error.value)
  }

  @Test
  fun `error is cleared before loading new tab`() = runTest {
    coEvery { mockProfileRepository.getProfile(testUserId) } returns testProfile
    coEvery { mockProfileRepository.getFollowers(testUserId, any()) } throws Exception("Error 1")
    coEvery { mockProfileRepository.getFollowing(testUserId, any()) } returns listOf(following1)

    viewModel.initialize(testUserId)
    testDispatcher.scheduler.advanceUntilIdle()

    assertNotNull(viewModel.error.value)

    viewModel.selectTab(FollowTab.FOLLOWING)
    testDispatcher.scheduler.advanceUntilIdle()

    // Error should be cleared on successful load
    assertNull(viewModel.error.value)
  }

  // ==================== REFRESH TESTS ====================

  @Test
  fun `refresh reloads data on both tabs`() = runTest {
    coEvery { mockProfileRepository.getProfile(testUserId) } returns testProfile

    // Test refresh on FOLLOWERS tab
    coEvery { mockProfileRepository.getFollowers(testUserId, any()) } returns
        listOf(follower1, follower2) andThen
        listOf(follower1, follower2, follower1)

    viewModel.initialize(testUserId, FollowTab.FOLLOWERS)
    testDispatcher.scheduler.advanceUntilIdle()

    assertEquals(2, viewModel.followers.value.size)

    viewModel.refresh()
    testDispatcher.scheduler.advanceUntilIdle()

    assertEquals(3, viewModel.followers.value.size)
    coVerify(exactly = 2) { mockProfileRepository.getFollowers(testUserId, 50) }

    // Test refresh on FOLLOWING tab
    coEvery { mockProfileRepository.getFollowing(testUserId, any()) } returns
        listOf(following1) andThen
        listOf(following1, following2)

    viewModel.selectTab(FollowTab.FOLLOWING)
    testDispatcher.scheduler.advanceUntilIdle()

    assertEquals(1, viewModel.following.value.size)

    viewModel.refresh()
    testDispatcher.scheduler.advanceUntilIdle()

    assertEquals(2, viewModel.following.value.size)
    coVerify(exactly = 2) { mockProfileRepository.getFollowing(testUserId, 50) }
  }

  @Test
  fun `refresh clears error state`() = runTest {
    coEvery { mockProfileRepository.getProfile(testUserId) } returns testProfile
    coEvery { mockProfileRepository.getFollowers(testUserId, any()) } throws
        Exception("Network error") andThen
        listOf(follower1)

    viewModel.initialize(testUserId)
    testDispatcher.scheduler.advanceUntilIdle()

    assertNotNull(viewModel.error.value)

    viewModel.refresh()
    testDispatcher.scheduler.advanceUntilIdle()

    assertNull(viewModel.error.value)
    assertEquals(1, viewModel.followers.value.size)
  }

  // ==================== EDGE CASES ====================

  @Test
  fun `handles empty lists on both tabs`() = runTest {
    coEvery { mockProfileRepository.getProfile(testUserId) } returns testProfile

    // Test empty FOLLOWERS
    coEvery { mockProfileRepository.getFollowers(testUserId, any()) } returns emptyList()

    viewModel.initialize(testUserId, FollowTab.FOLLOWERS)
    testDispatcher.scheduler.advanceUntilIdle()

    assertTrue(viewModel.followers.value.isEmpty())
    assertNull(viewModel.error.value)
    assertFalse(viewModel.isLoading.value)

    // Test empty FOLLOWING
    coEvery { mockProfileRepository.getFollowing(testUserId, any()) } returns emptyList()

    viewModel.selectTab(FollowTab.FOLLOWING)
    testDispatcher.scheduler.advanceUntilIdle()

    assertTrue(viewModel.following.value.isEmpty())
    assertNull(viewModel.error.value)
    assertFalse(viewModel.isLoading.value)
  }

  @Test
  fun `handles large followers list`() = runTest {
    val manyFollowers = List(50) { index -> follower1.copy(uid = "follower$index") }
    coEvery { mockProfileRepository.getProfile(testUserId) } returns testProfile
    coEvery { mockProfileRepository.getFollowers(testUserId, any()) } returns manyFollowers

    viewModel.initialize(testUserId)
    testDispatcher.scheduler.advanceUntilIdle()

    assertEquals(50, viewModel.followers.value.size)
    assertNull(viewModel.error.value)
  }

  @Test
  fun `handles profile with null bio in lists`() = runTest {
    val followerNullBio = follower1.copy(bio = null)
    coEvery { mockProfileRepository.getProfile(testUserId) } returns testProfile
    coEvery { mockProfileRepository.getFollowers(testUserId, any()) } returns
        listOf(followerNullBio)

    viewModel.initialize(testUserId)
    testDispatcher.scheduler.advanceUntilIdle()

    assertEquals(1, viewModel.followers.value.size)
    assertNull(viewModel.followers.value[0].bio)
  }

  @Test
  fun `handles profile with empty username`() = runTest {
    val profileEmptyUsername = testProfile.copy(username = "")
    coEvery { mockProfileRepository.getProfile(testUserId) } returns profileEmptyUsername
    coEvery { mockProfileRepository.getFollowers(testUserId, any()) } returns emptyList()

    viewModel.initialize(testUserId)
    testDispatcher.scheduler.advanceUntilIdle()

    assertEquals("", viewModel.profileUsername.value)
  }

  @Test
  fun `multiple initialize calls reset state correctly`() = runTest {
    coEvery { mockProfileRepository.getProfile(any()) } returns testProfile
    coEvery { mockProfileRepository.getFollowers(any(), any()) } returns listOf(follower1)

    // First initialize
    viewModel.initialize("user1")
    testDispatcher.scheduler.advanceUntilIdle()
    assertEquals(1, viewModel.followers.value.size)

    // Second initialize with different user
    val anotherUser = testProfile.copy(uid = "user2", username = "AnotherUser")
    coEvery { mockProfileRepository.getProfile("user2") } returns anotherUser
    coEvery { mockProfileRepository.getFollowers("user2", any()) } returns
        listOf(follower1, follower2)

    viewModel.initialize("user2")
    testDispatcher.scheduler.advanceUntilIdle()

    assertEquals("AnotherUser", viewModel.profileUsername.value)
    assertEquals(2, viewModel.followers.value.size)
  }

  // ==================== REPOSITORY INTERACTION TESTS ====================

  @Test
  fun `passes correct limit to repository methods`() = runTest {
    coEvery { mockProfileRepository.getProfile(testUserId) } returns testProfile
    coEvery { mockProfileRepository.getFollowers(testUserId, any()) } returns emptyList()
    coEvery { mockProfileRepository.getFollowing(testUserId, any()) } returns emptyList()

    viewModel.initialize(testUserId, FollowTab.FOLLOWERS)
    testDispatcher.scheduler.advanceUntilIdle()

    coVerify { mockProfileRepository.getFollowers(testUserId, 50) }

    viewModel.selectTab(FollowTab.FOLLOWING)
    testDispatcher.scheduler.advanceUntilIdle()

    coVerify { mockProfileRepository.getFollowing(testUserId, 50) }
  }
}
