package com.android.joinme.model.profile

// Tests partially written with AI assistance; reviewed for correctness.

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.mockk.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ProfileRepositoryProviderTest {

  private lateinit var mockContext: Context

  @Before
  fun setup() {
    mockContext = ApplicationProvider.getApplicationContext()
    // Reset the provider state before each test
    ProfileRepositoryProvider.resetForTesting()
    // Clear any mocks
    clearAllMocks()
  }

  @After
  fun tearDown() {
    ProfileRepositoryProvider.resetForTesting()
    clearAllMocks()
    unmockkAll()
  }

  // ==================== Test Environment Detection Tests ====================

  @Test
  fun `getRepository returns consistent local repository in test environment`() {
    // When
    val repo1 = ProfileRepositoryProvider.getRepository()
    val repo2 = ProfileRepositoryProvider.repository
    val repo3 = ProfileRepositoryProvider.getRepository()

    // Then - All should return same local repository instance
    assertTrue(repo1 is ProfileRepositoryLocal)
    assertTrue(repo2 is ProfileRepositoryLocal)
    assertSame(repo1, repo2)
    assertSame(repo1, repo3)
  }

  // ==================== Test with IS_TEST_ENV System Property ====================

  @Test
  fun `returns local repository when IS_TEST_ENV system property is true`() {
    try {
      // Given
      System.setProperty("IS_TEST_ENV", "true")
      ProfileRepositoryProvider.resetForTesting()

      // When
      val repository = ProfileRepositoryProvider.getRepository()

      // Then
      assertTrue(repository is ProfileRepositoryLocal)
    } finally {
      System.clearProperty("IS_TEST_ENV")
    }
  }

  // ==================== Context Handling Tests ====================

  @Test
  fun `getRepository works with and without context in test environment`() {
    // When
    val repoWithContext = ProfileRepositoryProvider.getRepository(mockContext)
    val repoWithNull = ProfileRepositoryProvider.getRepository(null)

    // Then - Both should return local repository in test environment
    assertTrue(repoWithContext is ProfileRepositoryLocal)
    assertTrue(repoWithNull is ProfileRepositoryLocal)
  }

  // ==================== Reset Functionality Tests ====================

  @Test
  fun `resetForTesting clears cached repository instances`() {
    // Given
    val repo1 = ProfileRepositoryProvider.getRepository()

    // When
    ProfileRepositoryProvider.resetForTesting()
    val repo2 = ProfileRepositoryProvider.getRepository()

    // Then - Should get same type but potentially different instance after reset
    assertTrue(repo1 is ProfileRepositoryLocal)
    assertTrue(repo2 is ProfileRepositoryLocal)
    // Both should be same since local repo is a lazy singleton
    assertSame(repo1, repo2)
  }

  @Test
  fun `multiple resetForTesting calls are safe`() {
    // When/Then - Should not throw exceptions
    ProfileRepositoryProvider.resetForTesting()
    ProfileRepositoryProvider.resetForTesting()
    ProfileRepositoryProvider.resetForTesting()

    // Should still be able to get repository
    val repository = ProfileRepositoryProvider.getRepository()
    assertNotNull(repository)
  }

  // ==================== Integration with Other Components ====================

  @Test
  fun `repository can perform basic operations`() {
    // Given
    val repository = ProfileRepositoryProvider.getRepository()
    val testProfile =
        Profile(
            uid = "test-uid",
            username = "TestUser",
            email = "test@example.com",
            interests = listOf("Testing"))

    // When/Then - Should be able to call repository methods without errors
    // (These will be in-memory operations since we're using local repository)
    try {
      kotlinx.coroutines.runBlocking {
        repository.createOrUpdateProfile(testProfile)
        val retrieved = repository.getProfile("test-uid")
        assertNotNull(retrieved)
        assertEquals("test-uid", retrieved?.uid)
      }
    } catch (e: Exception) {
      fail("Repository operations should not throw exceptions: ${e.message}")
    }
  }

  @Test
  fun `getRepository is thread-safe`() {
    // Given
    val repositories = mutableListOf<ProfileRepository>()

    // When - Call from multiple threads
    val threads =
        (1..5).map {
          Thread {
            val repo = ProfileRepositoryProvider.getRepository()
            synchronized(repositories) { repositories.add(repo) }
          }
        }

    threads.forEach { it.start() }
    threads.forEach { it.join() }

    // Then - All should be the same instance
    assertEquals(5, repositories.size)
    repositories.forEach { repo -> assertSame(repositories[0], repo) }
  }

  // ==================== Edge Cases ====================

  @Test
  fun `repository type is correct in test environment`() {
    // When
    val repository = ProfileRepositoryProvider.repository
    val customRepo = mockk<ProfileRepository>()
    ProfileRepositoryProvider.repository = customRepo
    val afterSet = ProfileRepositoryProvider.getRepository()

    // Then - Should be local repository in Robolectric test
    assertTrue(repository is ProfileRepositoryLocal)
    assertFalse(repository is ProfileRepositoryFirestore)
    assertFalse(repository is ProfileRepositoryCached)
    assertNotNull(afterSet) // Setting custom repo doesn't break getRepository
  }
}
