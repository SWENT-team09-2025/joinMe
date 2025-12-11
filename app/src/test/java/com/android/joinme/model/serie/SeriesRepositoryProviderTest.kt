package com.android.joinme.model.serie

/** This file was implemented with the help of AI * */
import android.content.Context
import com.google.firebase.FirebaseApp
import io.mockk.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class SeriesRepositoryProviderTest {

  private lateinit var context: Context

  @Before
  fun setUp() {
    context = RuntimeEnvironment.getApplication()
    SeriesRepositoryProvider.resetForTesting()
  }

  @After
  fun tearDown() {
    unmockkAll()
    SeriesRepositoryProvider.resetForTesting()
  }

  // ========== Test Environment Detection ==========

  @Test
  fun `getRepository returns local repo in test environment with caching`() {
    // Test multiple scenarios that should all return local repo
    val repoWithContext = SeriesRepositoryProvider.getRepository(context)
    val repoWithNull = SeriesRepositoryProvider.getRepository(null)

    // Both should be local repo
    assertTrue(repoWithContext is SeriesRepositoryLocal)
    assertTrue(repoWithNull is SeriesRepositoryLocal)

    // Should cache and return same instance
    assertSame(repoWithContext, SeriesRepositoryProvider.getRepository(context))
    assertSame(repoWithContext, repoWithNull)
  }

  @Test
  fun `getRepository with IS_TEST_ENV property returns local repo`() {
    System.setProperty("IS_TEST_ENV", "true")

    val repo = SeriesRepositoryProvider.getRepository(context)

    assertTrue(repo is SeriesRepositoryLocal)
    System.clearProperty("IS_TEST_ENV")
  }

  // ========== Deprecated repository Property ==========

  @Test
  fun `deprecated repository property works correctly`() {
    // Test that deprecated property works without explicit context
    val repoFromProperty = SeriesRepositoryProvider.repository
    assertTrue(repoFromProperty is SeriesRepositoryLocal)

    // Should return same instance as getRepository
    val repoFromMethod = SeriesRepositoryProvider.getRepository(context)
    assertSame(repoFromProperty, repoFromMethod)
  }

  @Test
  fun `repository property handles FirebaseApp fallback gracefully`() {
    mockkStatic(FirebaseApp::class)
    val mockFirebaseApp = mockk<FirebaseApp>(relaxed = true)

    // Test with Firebase initialized
    every { FirebaseApp.getInstance() } returns mockFirebaseApp
    every { mockFirebaseApp.applicationContext } returns context

    val repo = SeriesRepositoryProvider.repository
    assertTrue(repo is SeriesRepositoryLocal)

    // Test with Firebase not initialized
    every { FirebaseApp.getInstance() } throws IllegalStateException("Firebase not initialized")

    // In test environment, should still return local repo
    val repoNoFirebase = SeriesRepositoryProvider.repository
    assertTrue(repoNoFirebase is SeriesRepositoryLocal)
  }

  // ========== resetForTesting ==========

  @Test
  fun `resetForTesting clears cached instances and allows multiple calls`() {
    // Get initial instance
    val repo1 = SeriesRepositoryProvider.getRepository(context)

    // Reset multiple times
    SeriesRepositoryProvider.resetForTesting()
    SeriesRepositoryProvider.resetForTesting()

    // Get new instance - should work without errors
    val repo2 = SeriesRepositoryProvider.getRepository(context)

    // Both should be local repos
    assertTrue(repo1 is SeriesRepositoryLocal)
    assertTrue(repo2 is SeriesRepositoryLocal)
  }

  // ========== Context Requirements ==========

  @Test
  fun `getRepository handles context requirement correctly`() {
    // In test environment, context is optional
    val repoWithoutContext = SeriesRepositoryProvider.getRepository(null)
    assertTrue(repoWithoutContext is SeriesRepositoryLocal)

    // In test environment with context
    val repoWithContext = SeriesRepositoryProvider.getRepository(context)
    assertTrue(repoWithContext is SeriesRepositoryLocal)

    // Should be same instance
    assertSame(repoWithoutContext, repoWithContext)
  }

  // ========== Repository Functionality ==========

  @Test
  fun `provider returns functional repository with unique IDs`() {
    val repo = SeriesRepositoryProvider.getRepository(context)

    // Verify it's functional by generating unique IDs
    val id1 = repo.getNewSerieId()
    val id2 = repo.getNewSerieId()

    assertNotNull(id1)
    assertNotNull(id2)
    assertFalse(id1.isEmpty())
    assertFalse(id2.isEmpty())
    assertNotEquals(id1, id2) // IDs should be unique
  }
}
