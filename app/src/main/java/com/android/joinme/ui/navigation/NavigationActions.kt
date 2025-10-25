package com.android.joinme.ui.navigation

import androidx.navigation.NavHostController

sealed class Screen(
    val route: String,
    val name: String,
    val isTopLevelDestination: Boolean = false
) {
  object Auth : Screen(route = "auth", name = "Authentication")

  object Overview : Screen(route = "overview", name = "Overview", isTopLevelDestination = true)

  object Search : Screen(route = "search", name = "Search", isTopLevelDestination = true)

  object Map : Screen(route = "map", name = "Map", isTopLevelDestination = true)

  object Profile : Screen(route = "profile", name = "Profile", isTopLevelDestination = true)

  object CreateEvent : Screen(route = "create_event", name = "Create a new task")

  object EditProfile : Screen(route = "edit_profile", name = "Edit Profile")

  object History : Screen(route = "history", name = "History")

  object Groups : Screen(route = "groups", name = "Groups")

  object CreateGroup : Screen(route = "create_group", name = "Create Group")

  data class EditEvent(val eventId: String) :
      Screen(route = "edit_event/${eventId}", name = "Edit Event") {
    companion object {
      const val route = "edit_event/{eventId}"
    }
  }

  data class ShowEventScreen(val eventId: String) :
      Screen(route = "show_event/${eventId}", name = "Show Event") {
    companion object {
      const val route = "show_event/{eventId}"
    }
  }
}

/**
 * NavigationActions provides type-safe navigation methods for the JoinMe application.
 *
 * This class wraps a [NavHostController] and provides convenient methods for navigating between
 * screens and navigation graphs. It ensures consistent navigation behavior throughout the app and
 * reduces boilerplate in UI code.
 *
 * @param navController The [NavHostController] that manages navigation state and performs
 *   navigation actions.
 */
open class NavigationActions(
    private val navController: NavHostController,
) {
  /**
   * Navigate to the specified screen.
   *
   * @param screen The screen to navigate to
   */
  open fun navigateTo(screen: Screen) {
    if (screen.isTopLevelDestination && currentRoute() == screen.route) {
      // If the user is already on the top-level destination, do nothing
      return
    }
    navController.navigate(screen.route) {
      if (screen.isTopLevelDestination) {
        launchSingleTop = true
        popUpTo(screen.route) { inclusive = true }
      }
      //      restoreState = true
      //      restoreState = true
      //      if (screen.isTopLevelDestination) {
      //        // Pop up to the start destination of the graph to
      //        // avoid building up a large stack of destinations
      //        popUpTo(navController.graph.findStartDestination().id) {
      //          saveState = true
      //          inclusive = true
      //        }
      //        // Avoid multiple copies of the same destination when selecting again the same item
      //        launchSingleTop = true
      //      }
      //
      if (screen !is Screen.Auth) {
        // Restore state when navigating to a previously selected item
        restoreState = true
      }
    }
  }

  /** Navigate back to the previous screen. */
  open fun goBack() {
    navController.popBackStack()
  }

  /**
   * Get the current route of the navigation controller.
   *
   * @return The current route
   */
  open fun currentRoute(): String {
    return navController.currentDestination?.route ?: ""
  }
}
