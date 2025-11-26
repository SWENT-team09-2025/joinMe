package com.android.joinme.ui.navigation

import androidx.navigation.NavHostController

/**
 * Sealed class representing all navigation destinations in the JoinMe application.
 *
 * Each screen defines its route (used for navigation) and name (displayed in UI). Top-level
 * destinations are marked with isTopLevelDestination=true and appear in the bottom navigation bar.
 *
 * @param route The navigation route used by NavController
 * @param name The human-readable name of the screen
 * @param isTopLevelDestination Whether this screen appears in bottom navigation (default: false)
 */
sealed class Screen(
    val route: String,
    val name: String,
    val isTopLevelDestination: Boolean = false
) {
  // ============================================================================
  // Authentication
  // ============================================================================

  /** Authentication screen for user sign-in/sign-up */
  object Auth : Screen(route = "auth", name = "Authentication")

  // ============================================================================
  // Events, Series & History
  // ============================================================================

  /** Main overview screen showing upcoming events (Top-level destination) */
  object Overview : Screen(route = "overview", name = "Overview", isTopLevelDestination = true)

  /** Screen for creating a new event */
  object CreateEvent : Screen(route = "create_event", name = "Create a new task")

  /**
   * Screen for editing an existing event
   *
   * @param eventId The ID of the event to edit
   */
  data class EditEvent(val eventId: String) :
      Screen(route = "edit_event/${eventId}", name = "Edit Event") {
    companion object {
      const val route = "edit_event/{eventId}"
    }
  }

  /** Screen for creating a new serie */
  object CreateSerie : Screen(route = "create_serie", name = "Create a new serie")

  /**
   * Screen for viewing serie details
   *
   * @param serieId The ID of the serie to display
   */
  data class SerieDetails(val serieId: String) :
      Screen(route = "serie_details/${serieId}", name = "Serie Details") {
    companion object {
      const val route = "serie_details/{serieId}"
    }
  }
  /**
   * Screen for editing an existing serie
   *
   * @param serieId The ID of the serie to edit
   */
  data class EditSerie(val serieId: String) :
      Screen(route = "edit_serie/${serieId}", name = "Edit Serie") {
    companion object {
      const val route = "edit_serie/{serieId}"
    }
  }

  /**
   * Screen for creating a new event for an existing serie
   *
   * @param serieId The ID of the serie to add the event to
   */
  data class CreateEventForSerie(val serieId: String) :
      Screen(route = "create_event_for_serie/${serieId}", name = "Create Event for Serie") {
    companion object {
      const val route = "create_event_for_serie/{serieId}"
    }
  }

  /**
   * Screen for editing an existing event within a serie
   *
   * @param serieId The ID of the serie containing the event
   * @param eventId The ID of the event to edit
   */
  data class EditEventForSerie(val serieId: String, val eventId: String) :
      Screen(route = "edit_event_for_serie/${serieId}/${eventId}", name = "Edit Event for Serie") {
    companion object {
      const val route = "edit_event_for_serie/{serieId}/{eventId}"
    }
  }

  /**
   * Screen for viewing event details
   *
   * @param eventId The ID of the event to display
   * @param serieId Optional ID of the serie if the event belongs to one
   */
  data class ShowEventScreen(val eventId: String, val serieId: String? = null) :
      Screen(
          route =
              if (serieId != null) "show_event/${eventId}?serieId=${serieId}"
              else "show_event/${eventId}",
          name = "Show Event") {
    companion object {
      const val route = "show_event/{eventId}?serieId={serieId}"
    }
  }

  /** History screen showing past events */
  object History : Screen(route = "history", name = "History")

  // ============================================================================
  // Search
  // ============================================================================

  /** Search screen for finding events (Top-level destination) */
  object Search : Screen(route = "search", name = "Search", isTopLevelDestination = true)

  // ============================================================================
  // Map
  // ============================================================================

  /** Map screen showing events geographically (Top-level destination) */
  object Map : Screen(route = "map", name = "Map", isTopLevelDestination = true)

  // ============================================================================
  // Profile & Groups
  // ============================================================================

  /** User profile view screen (Top-level destination) */
  object Profile : Screen(route = "profile", name = "Profile", isTopLevelDestination = true)

  /** Profile editing screen */
  object EditProfile : Screen(route = "edit_profile", name = "Edit Profile")

  /** Groups list screen showing user's groups */
  object Groups : Screen(route = "groups", name = "Groups")

  /** Screen for creating a new group */
  object CreateGroup : Screen(route = "create_group", name = "Create Group")

  /**
   * Screen for editing an existing group
   *
   * @param groupId The ID of the group to edit
   */
  data class EditGroup(val groupId: String) :
      Screen(route = "edit_group/${groupId}", name = "Edit Group") {
    companion object {
      const val route = "edit_group/{groupId}"
    }
  }

  /**
   * Screen for viewing group details
   *
   * @param groupId The ID of the group to display
   */
  data class GroupDetail(val groupId: String) :
      Screen(route = "groupId/${groupId}", name = "Group Detail") {
    companion object {
      const val route = "groupId/{groupId}"
    }
  }

  /**
   * Screen for viewing activities (events + series) within a group
   *
   * @param groupId The ID of the group whose activities to display
   */
  data class ActivityGroup(val groupId: String) :
      Screen(route = "activity_group/${groupId}", name = "Activity Group") {
    companion object {
      const val route = "activity_group/{groupId}"
    }
  }

  // ============================================================================
  // Chat
  // ============================================================================

  /**
   * Screen for viewing and participating in a chat conversation
   *
   * @param chatId The ID of the chat/conversation to display
   * @param chatTitle The title to display in the chat (e.g., group name or event name)
   * @param totalParticipants Total number of participants in the event/group
   */
  data class Chat(val chatId: String, val chatTitle: String, val totalParticipants: Int = 1) :
      Screen(route = "chat/${chatId}/${chatTitle}/${totalParticipants}", name = "Chat") {
    companion object {
      const val route = "chat/{chatId}/{chatTitle}/{totalParticipants}"
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
   * Handles special behavior for top-level destinations (bottom nav items) by using singleTop
   * launch mode and restoring state. Non-auth screens restore their state when navigated to.
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

      if (screen !is Screen.Auth) {
        // Restore state when navigating to a previously selected item
        restoreState = true
      }
    }
  }

  /**
   * Navigate to a screen and clear the back stack up to a specific route.
   *
   * This is useful when you want to navigate to a screen and remove intermediate screens from the
   * back stack (e.g., after creating a group, go to Groups and remove CreateGroup).
   *
   * @param screen The screen to navigate to
   * @param popUpToRoute The route to pop up to (this route will remain in the stack)
   * @param inclusive If true, also pop the popUpToRoute from the stack
   */
  open fun navigateAndClearBackStackTo(
      screen: Screen,
      popUpToRoute: String,
      inclusive: Boolean = false
  ) {
    navController.navigate(screen.route) {
      popUpTo(popUpToRoute) { this.inclusive = inclusive }
      launchSingleTop = true
    }
  }

  /**
   * Navigate back to the previous screen.
   *
   * Pops the current destination from the back stack.
   */
  open fun goBack() {
    navController.popBackStack()
  }

  /**
   * Get the current route of the navigation controller.
   *
   * @return The current route, or empty string if no destination is active
   */
  open fun currentRoute(): String {
    return navController.currentDestination?.route ?: ""
  }
}
