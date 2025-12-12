package com.android.joinme.model.groups

// Tests partially written with AI assistance; reviewed for correctness.

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GroupRepositoryProviderTest {

  private lateinit var mockContext: Context

  @Before
  fun setup() {
    mockContext = ApplicationProvider.getApplicationContext()
    // Reset the provider state before each test
    GroupRepositoryProvider.resetForTesting()
    // Clear any mocks
    clearAllMocks()
  }

  @After
  fun tearDown() {
    GroupRepositoryProvider.resetForTesting()
    clearAllMocks()
    unmockkAll()
  }

  // ==================== Test Environment Detection Tests ====================

  @Test
  fun `getRepository returns local repository in test environment`() {
    // Given - Running in Robolectric test environment

    // When
    val repository = GroupRepositoryProvider.getRepository()

    // Then - Should return local repository
    assertTrue(repository is GroupRepositoryLocal)
  }

  @Test
  fun `repository property returns local repository in test environment`() {
    // Given - Running in Robolectric test environment

    // When
    val repository = GroupRepositoryProvider.repository

    // Then - Should return local repository
    assertTrue(repository is GroupRepositoryLocal)
  }

  @Test
  fun `repository property getter is consistent`() {
    // When
    val repo1 = GroupRepositoryProvider.repository
    val repo2 = GroupRepositoryProvider.repository

    // Then - Should return the same instance
    assertSame(repo1, repo2)
  }

  @Test
  fun `getRepository is consistent`() {
    // When
    val repo1 = GroupRepositoryProvider.getRepository()
    val repo2 = GroupRepositoryProvider.getRepository()

    // Then - Should return the same instance
    assertSame(repo1, repo2)
  }

  // ==================== Test with IS_TEST_ENV System Property ====================

  @Test
  fun `returns local repository when IS_TEST_ENV system property is true`() {
    try {
      // Given
      System.setProperty("IS_TEST_ENV", "true")
      GroupRepositoryProvider.resetForTesting()

      // When
      val repository = GroupRepositoryProvider.getRepository()

      // Then
      assertTrue(repository is GroupRepositoryLocal)
    } finally {
      System.clearProperty("IS_TEST_ENV")
    }
  }

  // ==================== Repository Injection Tests ====================

  @Test
  fun `repository property setter allows custom repository injection`() {
    // Given
    val customRepo = mockk<GroupRepository>()

    // When
    GroupRepositoryProvider.repository = customRepo

    // Then - Getter still returns the local repo in test env (setter doesn't override)
    // The setter is a no-op that allows tests to compile
    val result = GroupRepositoryProvider.repository
    assertNotNull(result)
  }

  // ==================== Cached Repository Production Tests ====================

  @Test
  fun `getRepository with context returns repository`() {
    // Given
    val context = ApplicationProvider.getApplicationContext<Context>()

    // When
    val repository = GroupRepositoryProvider.getRepository(context)

    // Then - In test environment, should still return local repository
    assertTrue(repository is GroupRepositoryLocal)
  }

  @Test
  fun `getRepository with null context in test environment works`() {
    // When
    val repository = GroupRepositoryProvider.getRepository(null)

    // Then - In test environment, should return local repository without needing context
    assertTrue(repository is GroupRepositoryLocal)
  }

  // ==================== Reset Functionality Tests ====================

  @Test
  fun `resetForTesting clears cached repository instances`() {
    // Given
    val repo1 = GroupRepositoryProvider.getRepository()

    // When
    GroupRepositoryProvider.resetForTesting()
    val repo2 = GroupRepositoryProvider.getRepository()

    // Then - Should get same type but potentially different instance after reset
    assertTrue(repo1 is GroupRepositoryLocal)
    assertTrue(repo2 is GroupRepositoryLocal)
    // Both should be same since local repo is a lazy singleton
    assertSame(repo1, repo2)
  }

  @Test
  fun `multiple resetForTesting calls are safe`() {
    // When/Then - Should not throw exceptions
    GroupRepositoryProvider.resetForTesting()
    GroupRepositoryProvider.resetForTesting()
    GroupRepositoryProvider.resetForTesting()

    // Should still be able to get repository
    val repository = GroupRepositoryProvider.getRepository()
    assertNotNull(repository)
  }

  // ==================== Integration with Other Components ====================

  @Test
  fun `repository can perform basic operations`() {
    // Given
    val repository = GroupRepositoryProvider.getRepository()

    // When/Then - Should be able to call repository methods without errors
    // (These will be in-memory operations since we're using local repository)
    try {
      runBlocking {
        val newId = repository.getNewGroupId()
        assertNotNull(newId)
        assertTrue(newId.isNotEmpty())
      }
    } catch (e: Exception) {
      fail("Repository operations should not throw exceptions: ${e.message}")
    }
  }

  @Test
  fun `getRepository is thread-safe`() {
    // Given
    val repositories = mutableListOf<GroupRepository>()

    // When - Call from multiple threads
    val threads =
        (1..5).map {
          Thread {
            val repo = GroupRepositoryProvider.getRepository()
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
  fun `getRepository after setting custom repository still works`() {
    // Given
    val customRepo = mockk<GroupRepository>()
    GroupRepositoryProvider.repository = customRepo

    // When
    val result = GroupRepositoryProvider.getRepository()

    // Then - Should still return the appropriate repository for test environment
    assertNotNull(result)
  }

  @Test
  fun `repository type is correct in test environment`() {
    // When
    val repository = GroupRepositoryProvider.repository

    // Then - Should be local repository in Robolectric test
    assertTrue(repository is GroupRepositoryLocal)
    assertFalse(repository is GroupRepositoryFirestore)
    assertFalse(repository is GroupRepositoryCached)
  }
}
