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
    coVerify { mockProfileRepository.getFollowers(testUserId, 25) }
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
    coVerify { mockProfileRepository.getFollowing(testUserId, 25) }
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
  fun `initialize handles username loading failures gracefully`() = runTest {
    // Test case 1: null profile - followers still load successfully
    coEvery { mockProfileRepository.getProfile(testUserId) } returns null
    coEvery { mockProfileRepository.getFollowers(testUserId, any()) } returns emptyList()

    viewModel.initialize(testUserId)
    testDispatcher.scheduler.advanceUntilIdle()

    assertEquals("", viewModel.profileUsername.value)
    assertTrue(viewModel.followers.value.isEmpty())
    assertFalse(viewModel.isLoading.value)

    // Test case 2: exception during username fetch - data loading continues
    // Username is now loaded after tab data, so error persists when tab loading succeeds
    val anotherUserId = "another-user-id"
    coEvery { mockProfileRepository.getProfile(anotherUserId) } throws Exception("Network error")
    coEvery { mockProfileRepository.getFollowers(anotherUserId, any()) } returns
        listOf(follower1, follower2)

    viewModel.initialize(anotherUserId)
    testDispatcher.scheduler.advanceUntilIdle()

    assertEquals("", viewModel.profileUsername.value)
    assertEquals(2, viewModel.followers.value.size)
    assertFalse(viewModel.isLoading.value)
    // Error from username fetch persists since it's loaded after tab data
    assertNotNull(viewModel.error.value)
    assertTrue(viewModel.error.value!!.contains("Failed to load Profile username"))
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
    coVerify(exactly = 1) { mockProfileRepository.getFollowers(testUserId, 25) }
    coVerify(exactly = 1) { mockProfileRepository.getFollowing(testUserId, 25) }
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
  fun `error state is properly managed and cleared`() = runTest {
    coEvery { mockProfileRepository.getProfile(testUserId) } returns testProfile
    coEvery { mockProfileRepository.getFollowers(testUserId, any()) } throws
        Exception("Network error")

    viewModel.initialize(testUserId)
    testDispatcher.scheduler.advanceUntilIdle()

    assertNotNull(viewModel.error.value)

    // Test case 1: Manual error clearing
    viewModel.clearError()
    assertNull(viewModel.error.value)

    // Test case 2: Error cleared automatically when loading new tab successfully
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
    coVerify(exactly = 2) { mockProfileRepository.getFollowers(testUserId, 25) }

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
    coVerify(exactly = 2) { mockProfileRepository.getFollowing(testUserId, 25) }
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
  fun `handles profiles with null or empty optional fields`() = runTest {
    // Test case 1: Profile with null bio in follower list
    val followerNullBio = follower1.copy(bio = null)
    coEvery { mockProfileRepository.getProfile(testUserId) } returns testProfile
    coEvery { mockProfileRepository.getFollowers(testUserId, any()) } returns
        listOf(followerNullBio)

    viewModel.initialize(testUserId)
    testDispatcher.scheduler.advanceUntilIdle()

    assertEquals(1, viewModel.followers.value.size)
    assertNull(viewModel.followers.value[0].bio)

    // Test case 2: Profile with empty username (different user)
    val anotherUserId = "another-user-id"
    val profileEmptyUsername = testProfile.copy(uid = anotherUserId, username = "")
    coEvery { mockProfileRepository.getProfile(anotherUserId) } returns profileEmptyUsername
    coEvery { mockProfileRepository.getFollowers(anotherUserId, any()) } returns emptyList()

    viewModel.initialize(anotherUserId)
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

    coVerify { mockProfileRepository.getFollowers(testUserId, 25) }

    viewModel.selectTab(FollowTab.FOLLOWING)
    testDispatcher.scheduler.advanceUntilIdle()

    coVerify { mockProfileRepository.getFollowing(testUserId, 25) }
  }

  // ==================== PAGINATION TESTS ====================

  @Test
  fun `hasMore flags reflect data availability correctly`() = runTest {
    // Test case 1: hasMore is true when exactly PAGE_SIZE items returned
    val manyFollowers = List(25) { index -> follower1.copy(uid = "follower$index") }
    coEvery { mockProfileRepository.getProfile(testUserId) } returns testProfile
    coEvery { mockProfileRepository.getFollowers(testUserId, any()) } returns manyFollowers

    viewModel.initialize(testUserId)
    testDispatcher.scheduler.advanceUntilIdle()

    assertTrue(viewModel.hasMoreFollowers.value)
    assertEquals(25, viewModel.followers.value.size)

    // Test case 2: hasMore is false when fewer than PAGE_SIZE items returned
    val anotherUserId = "another-user-id"
    coEvery { mockProfileRepository.getProfile(anotherUserId) } returns testProfile
    coEvery { mockProfileRepository.getFollowers(anotherUserId, any()) } returns
        listOf(follower1, follower2)

    viewModel.initialize(anotherUserId)
    testDispatcher.scheduler.advanceUntilIdle()

    assertFalse(viewModel.hasMoreFollowers.value)
    assertEquals(2, viewModel.followers.value.size)
  }

  @Test
  fun `loadMore fetches next page for both tabs`() = runTest {
    // Test case 1: Load more followers
    val followersPage1 = List(25) { index -> follower1.copy(uid = "follower$index") }
    val followersPage2 = List(50) { index -> follower1.copy(uid = "follower$index") }

    coEvery { mockProfileRepository.getProfile(testUserId) } returns testProfile
    coEvery { mockProfileRepository.getFollowers(testUserId, 25) } returns followersPage1
    coEvery { mockProfileRepository.getFollowers(testUserId, 50) } returns followersPage2

    viewModel.initialize(testUserId)
    testDispatcher.scheduler.advanceUntilIdle()

    assertEquals(25, viewModel.followers.value.size)
    assertTrue(viewModel.hasMoreFollowers.value)
    assertFalse(viewModel.isLoadingMore.value)

    viewModel.loadMore()
    testDispatcher.scheduler.advanceUntilIdle()

    assertEquals(50, viewModel.followers.value.size)
    assertTrue(viewModel.hasMoreFollowers.value)
    assertFalse(viewModel.isLoadingMore.value)
    coVerify { mockProfileRepository.getFollowers(testUserId, 25) }
    coVerify { mockProfileRepository.getFollowers(testUserId, 50) }

    // Test case 2: Load more following
    val followingPage1 = List(25) { index -> following1.copy(uid = "following$index") }
    val followingPage2 = List(50) { index -> following1.copy(uid = "following$index") }

    coEvery { mockProfileRepository.getFollowing(testUserId, 25) } returns followingPage1
    coEvery { mockProfileRepository.getFollowing(testUserId, 50) } returns followingPage2

    viewModel.selectTab(FollowTab.FOLLOWING)
    testDispatcher.scheduler.advanceUntilIdle()

    assertEquals(25, viewModel.following.value.size)
    assertTrue(viewModel.hasMoreFollowing.value)

    viewModel.loadMore()
    testDispatcher.scheduler.advanceUntilIdle()

    assertEquals(50, viewModel.following.value.size)
    assertTrue(viewModel.hasMoreFollowing.value)
    coVerify { mockProfileRepository.getFollowing(testUserId, 25) }
    coVerify { mockProfileRepository.getFollowing(testUserId, 50) }
  }

  @Test
  fun `loadMore respects guard conditions and loading state`() = runTest {
    // Test case 1: Does nothing when hasMore is false
    coEvery { mockProfileRepository.getProfile(testUserId) } returns testProfile
    coEvery { mockProfileRepository.getFollowers(testUserId, any()) } returns
        listOf(follower1, follower2)

    viewModel.initialize(testUserId)
    testDispatcher.scheduler.advanceUntilIdle()

    assertFalse(viewModel.hasMoreFollowers.value)
    val initialSize = viewModel.followers.value.size

    viewModel.loadMore()
    testDispatcher.scheduler.advanceUntilIdle()

    assertEquals(initialSize, viewModel.followers.value.size)
    coVerify(exactly = 1) { mockProfileRepository.getFollowers(any(), any()) }

    // Test case 2: Prevents concurrent loads
    val anotherUserId = "another-user-id"
    val firstPage = List(25) { index -> follower1.copy(uid = "follower$index") }
    coEvery { mockProfileRepository.getProfile(anotherUserId) } returns testProfile
    coEvery { mockProfileRepository.getFollowers(anotherUserId, 25) } returns firstPage
    coEvery { mockProfileRepository.getFollowers(anotherUserId, 50) } coAnswers
        {
          // Verify isLoadingMore is true during fetch
          assertTrue(viewModel.isLoadingMore.value)
          kotlinx.coroutines.delay(1000)
          firstPage
        }

    viewModel.initialize(anotherUserId)
    testDispatcher.scheduler.advanceUntilIdle()

    assertTrue(viewModel.hasMoreFollowers.value)
    assertFalse(viewModel.isLoadingMore.value)

    // Call loadMore twice quickly
    viewModel.loadMore()
    viewModel.loadMore()
    testDispatcher.scheduler.advanceUntilIdle()

    // Should only load once
    assertFalse(viewModel.isLoadingMore.value)
    coVerify(exactly = 1) { mockProfileRepository.getFollowers(anotherUserId, 50) }
  }

  @Test
  fun `loadMore handles errors and reverts page increment`() = runTest {
    val firstPage = List(25) { index -> follower1.copy(uid = "follower$index") }

    coEvery { mockProfileRepository.getProfile(testUserId) } returns testProfile
    coEvery { mockProfileRepository.getFollowers(testUserId, 25) } returns firstPage
    coEvery { mockProfileRepository.getFollowers(testUserId, 50) } throws Exception("Network error")

    viewModel.initialize(testUserId)
    testDispatcher.scheduler.advanceUntilIdle()

    assertEquals(25, viewModel.followers.value.size)
    assertTrue(viewModel.hasMoreFollowers.value)

    viewModel.loadMore()
    testDispatcher.scheduler.advanceUntilIdle()

    // Should have error and data should remain unchanged
    assertNotNull(viewModel.error.value)
    assertTrue(viewModel.error.value!!.contains("Failed to load more followers"))
    assertEquals(25, viewModel.followers.value.size)
    assertFalse(viewModel.isLoadingMore.value)
  }

  @Test
  fun `pagination state resets on refresh and user switch`() = runTest {
    val firstPage = List(25) { index -> follower1.copy(uid = "follower$index") }
    val secondPage = List(50) { index -> follower1.copy(uid = "follower$index") }
    val refreshedPage = List(25) { index -> follower2.copy(uid = "new$index") }

    coEvery { mockProfileRepository.getProfile(any()) } returns testProfile
    coEvery { mockProfileRepository.getFollowers(testUserId, 25) } returns
        firstPage andThen
        refreshedPage
    coEvery { mockProfileRepository.getFollowers(testUserId, 50) } returns secondPage

    viewModel.initialize(testUserId)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.loadMore()
    testDispatcher.scheduler.advanceUntilIdle()

    assertEquals(50, viewModel.followers.value.size)

    // Test case 1: Refresh resets to first page
    viewModel.refresh()
    testDispatcher.scheduler.advanceUntilIdle()

    assertEquals(25, viewModel.followers.value.size)
    assertEquals("new0", viewModel.followers.value[0].uid)

    // Load more again to set up for user switch test
    coEvery { mockProfileRepository.getFollowers(testUserId, 50) } returns secondPage
    viewModel.loadMore()
    testDispatcher.scheduler.advanceUntilIdle()
    assertEquals(50, viewModel.followers.value.size)

    // Test case 2: Switching users resets pagination
    val anotherUserId = "another-user-id"
    val anotherUserPage = List(10) { index -> follower1.copy(uid = "another$index") }
    coEvery { mockProfileRepository.getFollowers(anotherUserId, 25) } returns anotherUserPage

    viewModel.initialize(anotherUserId)
    testDispatcher.scheduler.advanceUntilIdle()

    assertEquals(10, viewModel.followers.value.size)
    assertFalse(viewModel.hasMoreFollowers.value)
  }
}
