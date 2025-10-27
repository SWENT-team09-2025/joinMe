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
    // Events & History
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

    /**
     * Screen for viewing event details
     *
     * @param eventId The ID of the event to display
     */
    data class ShowEventScreen(val eventId: String) :
        Screen(route = "show_event/${eventId}", name = "Show Event") {
        companion object {
            const val route = "show_event/{eventId}"
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
    data class GroupDetail(val groupId: String) :
        Screen(route = "group_Id/${groupId}", name = "Group Detail") {
        companion object {
            const val route = "group_Id/{group_Id}"
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
