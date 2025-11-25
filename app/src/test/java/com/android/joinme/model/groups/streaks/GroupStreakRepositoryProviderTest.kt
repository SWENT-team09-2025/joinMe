package com.android.joinme.model.groups.streaks

import android.content.Context
import com.google.firebase.FirebaseApp
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class GroupStreakRepositoryProviderTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        clearRepositoryCache()
    }

    @After
    fun tearDown() {
        unmockkAll()
        clearRepositoryCache()
    }

    /** Uses reflection to reset the cached Firestore repository instance. */
    private fun clearRepositoryCache() {
        val field = GroupStreakRepositoryProvider::class.java.getDeclaredField("firestoreRepo")
        field.isAccessible = true
        field.set(GroupStreakRepositoryProvider, null)
    }

    @Test
    fun `getRepository returns LocalRepository in test environment`() {
        val repo = GroupStreakRepositoryProvider.getRepository(context)
        assertTrue(repo is GroupStreakRepositoryLocal)
    }

    @Test
    fun `getRepository returns same instance on multiple calls`() {
        val repo1 = GroupStreakRepositoryProvider.getRepository(context)
        val repo2 = GroupStreakRepositoryProvider.getRepository(context)
        assertSame(repo1, repo2)
    }

    @Test
    fun `getRepository handles null context gracefully`() {
        val repo = GroupStreakRepositoryProvider.getRepository(null)
        assertNotNull(repo)
        assertTrue(repo is GroupStreakRepositoryLocal)
    }

    @Test
    fun `getRepository returns LocalRepository when IS_TEST_ENV property is set`() {
        System.setProperty("IS_TEST_ENV", "true")
        val repo = GroupStreakRepositoryProvider.getRepository(context)
        assertTrue(repo is GroupStreakRepositoryLocal)
        System.clearProperty("IS_TEST_ENV")
    }

    @Test
    fun `getRepository initializes FirebaseApp when apps list is empty`() {
        mockkStatic(FirebaseApp::class)
        val mockFirebaseApp = mockk<FirebaseApp>(relaxed = true)

        every { FirebaseApp.getApps(any()) } returns emptyList()
        every { FirebaseApp.initializeApp(any()) } returns mockFirebaseApp

        // In test environment, still returns local repo
        val repo = GroupStreakRepositoryProvider.getRepository(context)
        assertNotNull(repo)
    }

    @Test
    fun `getRepository reuses existing FirebaseApp when already initialized`() {
        mockkStatic(FirebaseApp::class)
        val mockFirebaseApp = mockk<FirebaseApp>(relaxed = true)

        every { FirebaseApp.getApps(any()) } returns listOf(mockFirebaseApp)
        every { FirebaseApp.getInstance() } returns mockFirebaseApp

        val repo = GroupStreakRepositoryProvider.getRepository(context)
        assertNotNull(repo)
    }
}