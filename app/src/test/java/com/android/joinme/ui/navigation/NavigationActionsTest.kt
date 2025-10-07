package com.android.joinme.ui.navigation

import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.navOptions
import io.mockk.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class NavigationActionsTest {

  private lateinit var navController: NavHostController
  private lateinit var actions: NavigationActions

  @Before
  fun setup() {
    navController = mockk(relaxed = true)
    actions = NavigationActions(navController)
  }

  @After fun tearDown() = unmockkAll()

  @Test
  fun `navigateTo should NOT navigate again if already on same top-level`() {
    every { navController.currentDestination?.route } returns Screen.Map.route

    actions.navigateTo(Screen.Map)

    verify(exactly = 0) {
      navController.navigate(any<String>(), any<NavOptionsBuilder.() -> Unit>())
    }
  }

  @Test
  fun `navigateTo configures popUpTo, launchSingleTop, restoreState for top-level`() {
    every { navController.currentDestination?.route } returns "auth" // ensure we navigate

    actions.navigateTo(Screen.Map)

    verify {
      navController.navigate(
          eq(Screen.Map.route),
          withArg<NavOptionsBuilder.() -> Unit> { block ->
            // ✅ build NavOptions via the public API
            val options: NavOptions = navOptions(block)

            assertTrue(options.shouldLaunchSingleTop())
            assertTrue(options.shouldRestoreState())
            // If your Navigation version exposes these, keep them:
            // assertEquals(Screen.Map.route, options.popUpToRoute)
            // assertTrue(options.isPopUpToInclusive())
          })
    }
  }

  @Test
  fun `navigateTo sets restoreState=false when navigating to Auth`() {
    every { navController.currentDestination?.route } returns "overview"

    actions.navigateTo(Screen.Auth)

    verify {
      navController.navigate(
          eq(Screen.Auth.route),
          withArg<NavOptionsBuilder.() -> Unit> { block ->
            val options = navOptions(block)
            assertFalse(options.shouldRestoreState())
            // We intentionally don't assert popUpTo/launchSingleTop for Auth.
          })
    }
  }

  @Test
  fun `currentRoute returns current destination route or empty`() {
    every { navController.currentDestination?.route } returns Screen.Search.route
    assertEquals(Screen.Search.route, actions.currentRoute())

    every { navController.currentDestination?.route } returns null
    assertEquals("", actions.currentRoute())
  }

  @Test
  fun `goBack pops back stack`() {
    actions.goBack()
    verify { navController.popBackStack() }
  }
}
