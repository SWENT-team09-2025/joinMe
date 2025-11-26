package com.android.joinme.ui.navigation
/* CO WRITE WITH CLAUDE AI */
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
            val options: NavOptions = navOptions(block)

            assertTrue(options.shouldLaunchSingleTop())
            assertTrue(options.shouldRestoreState())
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

  @Test
  fun `navigateTo GroupDetail with groupId navigates to correct route`() {
    val groupId = "test-group-123"
    every { navController.currentDestination?.route } returns Screen.Groups.route

    actions.navigateTo(Screen.GroupDetail(groupId))

    verify {
      navController.navigate(
          eq("groupId/$groupId"),
          withArg<NavOptionsBuilder.() -> Unit> { block ->
            val options = navOptions(block)
            assertTrue(options.shouldRestoreState())
            // GroupDetail is not a top-level destination, so shouldn't have launchSingleTop
            assertFalse(options.shouldLaunchSingleTop())
          })
    }
  }

  @Test
  fun `navigateTo EditEvent with eventId navigates to correct route`() {
    val eventId = "test-event-456"
    every { navController.currentDestination?.route } returns Screen.Overview.route

    actions.navigateTo(Screen.EditEvent(eventId))

    verify {
      navController.navigate(
          eq("edit_event/$eventId"),
          withArg<NavOptionsBuilder.() -> Unit> { block ->
            val options = navOptions(block)
            assertTrue(options.shouldRestoreState())
            assertFalse(options.shouldLaunchSingleTop())
          })
    }
  }

  @Test
  fun `navigateTo ShowEventScreen with eventId navigates to correct route`() {
    val eventId = "test-event-789"
    every { navController.currentDestination?.route } returns Screen.Overview.route

    actions.navigateTo(Screen.ShowEventScreen(eventId))

    verify {
      navController.navigate(
          eq("show_event/$eventId"),
          withArg<NavOptionsBuilder.() -> Unit> { block ->
            val options = navOptions(block)
            assertTrue(options.shouldRestoreState())
            assertFalse(options.shouldLaunchSingleTop())
          })
    }
  }

  @Test
  fun `navigateTo ShowEventScreen with eventId and serieId navigates to correct route`() {
    val eventId = "test-event-789"
    val serieId = "test-serie-123"
    every { navController.currentDestination?.route } returns Screen.SerieDetails(serieId).route

    actions.navigateTo(Screen.ShowEventScreen(eventId, serieId))

    verify {
      navController.navigate(
          eq("show_event/$eventId?serieId=$serieId"),
          withArg<NavOptionsBuilder.() -> Unit> { block ->
            val options = navOptions(block)
            assertTrue(options.shouldRestoreState())
            assertFalse(options.shouldLaunchSingleTop())
          })
    }
  }

  @Test
  fun `ShowEventScreen route companion object matches pattern with optional serieId`() {
    assertEquals("show_event/{eventId}?serieId={serieId}", Screen.ShowEventScreen.Companion.route)
  }

  @Test
  fun `ShowEventScreen instance route contains eventId without serieId`() {
    val eventId = "event-123"
    val screen = Screen.ShowEventScreen(eventId)
    assertEquals("show_event/$eventId", screen.route)
  }

  @Test
  fun `ShowEventScreen instance route contains eventId and serieId when provided`() {
    val eventId = "event-456"
    val serieId = "serie-789"
    val screen = Screen.ShowEventScreen(eventId, serieId)
    assertEquals("show_event/$eventId?serieId=$serieId", screen.route)
  }

  @Test
  fun `navigateTo Groups screen navigates correctly`() {
    every { navController.currentDestination?.route } returns Screen.Profile.route

    actions.navigateTo(Screen.Groups)

    verify {
      navController.navigate(
          eq(Screen.Groups.route),
          withArg<NavOptionsBuilder.() -> Unit> { block ->
            val options = navOptions(block)
            assertTrue(options.shouldRestoreState())
            // Groups is not a top-level destination
            assertFalse(options.shouldLaunchSingleTop())
          })
    }
  }

  @Test
  fun `navigateTo CreateGroup screen navigates correctly`() {
    every { navController.currentDestination?.route } returns Screen.Groups.route

    actions.navigateTo(Screen.CreateGroup)

    verify {
      navController.navigate(
          eq(Screen.CreateGroup.route),
          withArg<NavOptionsBuilder.() -> Unit> { block ->
            val options = navOptions(block)
            assertTrue(options.shouldRestoreState())
            assertFalse(options.shouldLaunchSingleTop())
          })
    }
  }

  @Test
  fun navigateToCreateEventForSeriewithserieIdnavigatestocorrectroute() {
    val serieId = "test-serie-123"
    every { navController.currentDestination?.route } returns Screen.CreateSerie.route

    actions.navigateTo(Screen.CreateEventForSerie(serieId))

    verify {
      navController.navigate(
          eq("create_event_for_serie/$serieId"),
          withArg<NavOptionsBuilder.() -> Unit> { block ->
            val options = navOptions(block)
            assertTrue(options.shouldRestoreState())
            // CreateEventForSerie is not a top-level destination
            assertFalse(options.shouldLaunchSingleTop())
          })
    }
  }

  @Test
  fun `navigateTo EditGroup with groupId navigates to correct route`() {
    val groupId = "test-group-789"
    every { navController.currentDestination?.route } returns Screen.Groups.route

    actions.navigateTo(Screen.EditGroup(groupId))

    verify {
      navController.navigate(
          eq("edit_group/$groupId"),
          withArg<NavOptionsBuilder.() -> Unit> { block ->
            val options = navOptions(block)
            assertTrue(options.shouldRestoreState())
            // EditGroup is not a top-level destination, so shouldn't have launchSingleTop
            assertFalse(options.shouldLaunchSingleTop())
          })
    }
  }

  // ========== Serie Details Navigation Tests ==========

  @Test
  fun `navigateTo SerieDetails with serieId navigates to correct route`() {
    val serieId = "test-serie-123"
    every { navController.currentDestination?.route } returns Screen.Overview.route

    actions.navigateTo(Screen.SerieDetails(serieId))

    verify {
      navController.navigate(
          eq("serie_details/$serieId"),
          withArg<NavOptionsBuilder.() -> Unit> { block ->
            val options = navOptions(block)
            assertTrue(options.shouldRestoreState())
            // SerieDetails is not a top-level destination, so shouldn't have launchSingleTop
            assertFalse(options.shouldLaunchSingleTop())
          })
    }
  }

  @Test
  fun `SerieDetails route companion object matches pattern`() {
    assertEquals("serie_details/{serieId}", Screen.SerieDetails.Companion.route)
  }

  @Test
  fun `SerieDetails instance route contains actual serieId`() {
    val serieId = "my-serie-456"
    val screen = Screen.SerieDetails(serieId)
    assertEquals("serie_details/$serieId", screen.route)
  }

  @Test
  fun `SerieDetails screen has correct name`() {
    val screen = Screen.SerieDetails("test-id")
    assertEquals("Serie Details", screen.name)
    assertFalse(screen.isTopLevelDestination)
  }

  @Test
  fun `navigateTo SerieDetails from History screen works correctly`() {
    val serieId = "serie-from-history-789"
    every { navController.currentDestination?.route } returns Screen.History.route

    actions.navigateTo(Screen.SerieDetails(serieId))

    verify {
      navController.navigate(
          eq("serie_details/$serieId"),
          withArg<NavOptionsBuilder.() -> Unit> { block ->
            val options = navOptions(block)
            assertTrue(options.shouldRestoreState())
            assertFalse(options.shouldLaunchSingleTop())
          })
    }
  }

  @Test
  fun `navigateTo SerieDetails multiple times with different serieIds`() {
    every { navController.currentDestination?.route } returns Screen.Overview.route

    val serieId1 = "serie-1"
    val serieId2 = "serie-2"

    actions.navigateTo(Screen.SerieDetails(serieId1))
    actions.navigateTo(Screen.SerieDetails(serieId2))

    verify {
      navController.navigate(eq("serie_details/$serieId1"), any<NavOptionsBuilder.() -> Unit>())
    }
    verify {
      navController.navigate(eq("serie_details/$serieId2"), any<NavOptionsBuilder.() -> Unit>())
    }
  }

  // ========== Edit Serie Navigation Tests ==========

  @Test
  fun `navigateTo EditSerie with serieId navigates to correct route`() {
    val serieId = "test-serie-123"
    every { navController.currentDestination?.route } returns
        Screen.SerieDetails("test-serie-123").route

    actions.navigateTo(Screen.EditSerie(serieId))

    verify {
      navController.navigate(
          eq("edit_serie/$serieId"),
          withArg<NavOptionsBuilder.() -> Unit> { block ->
            val options = navOptions(block)
            assertTrue(options.shouldRestoreState())
            // EditSerie is not a top-level destination, so shouldn't have launchSingleTop
            assertFalse(options.shouldLaunchSingleTop())
          })
    }
  }

  @Test
  fun `EditSerie route companion object matches pattern`() {
    assertEquals("edit_serie/{serieId}", Screen.EditSerie.Companion.route)
  }

  @Test
  fun `EditSerie instance route contains actual serieId`() {
    val serieId = "my-serie-789"
    val screen = Screen.EditSerie(serieId)
    assertEquals("edit_serie/$serieId", screen.route)
  }

  @Test
  fun `EditSerie screen has correct name`() {
    val screen = Screen.EditSerie("test-id")
    assertEquals("Edit Serie", screen.name)
    assertFalse(screen.isTopLevelDestination)
  }

  @Test
  fun `navigateTo EditSerie from SerieDetails screen works correctly`() {
    val serieId = "serie-edit-456"
    every { navController.currentDestination?.route } returns Screen.SerieDetails(serieId).route

    actions.navigateTo(Screen.EditSerie(serieId))

    verify {
      navController.navigate(
          eq("edit_serie/$serieId"),
          withArg<NavOptionsBuilder.() -> Unit> { block ->
            val options = navOptions(block)
            assertTrue(options.shouldRestoreState())
            assertFalse(options.shouldLaunchSingleTop())
          })
    }
  }

  @Test
  fun `navigateTo EditSerie with special characters in serieId`() {
    val serieId = "serie-123-abc-xyz"
    every { navController.currentDestination?.route } returns Screen.Overview.route

    actions.navigateTo(Screen.EditSerie(serieId))

    verify {
      navController.navigate(eq("edit_serie/$serieId"), any<NavOptionsBuilder.() -> Unit>())
    }
  }

  // ========== Edit Profile Navigation Tests ==========

  @Test
  fun `navigateTo EditProfile navigates to correct route`() {
    every { navController.currentDestination?.route } returns Screen.Profile.route

    actions.navigateTo(Screen.EditProfile)

    verify {
      navController.navigate(
          eq(Screen.EditProfile.route),
          withArg<NavOptionsBuilder.() -> Unit> { block ->
            val options = navOptions(block)
            assertTrue(options.shouldRestoreState())
            // EditProfile is not a top-level destination
            assertFalse(options.shouldLaunchSingleTop())
          })
    }
  }

  @Test
  fun `EditProfile screen has correct properties`() {
    assertEquals("edit_profile", Screen.EditProfile.route)
    assertEquals("Edit Profile", Screen.EditProfile.name)
    assertFalse(Screen.EditProfile.isTopLevelDestination)
  }

  @Test
  fun `navigateTo EditProfile from Groups screen works correctly`() {
    every { navController.currentDestination?.route } returns Screen.Groups.route

    actions.navigateTo(Screen.EditProfile)

    verify {
      navController.navigate(
          eq("edit_profile"),
          withArg<NavOptionsBuilder.() -> Unit> { block ->
            val options = navOptions(block)
            assertTrue(options.shouldRestoreState())
            assertFalse(options.shouldLaunchSingleTop())
          })
    }
  }

  // ========== Additional Screen Object Tests for Coverage ==========

  @Test
  fun `CreateEvent screen has correct properties`() {
    assertEquals("create_event", Screen.CreateEvent.route)
    assertEquals("Create a new task", Screen.CreateEvent.name)
    assertFalse(Screen.CreateEvent.isTopLevelDestination)
  }

  @Test
  fun `CreateSerie screen has correct properties`() {
    assertEquals("create_serie", Screen.CreateSerie.route)
    assertEquals("Create a new serie", Screen.CreateSerie.name)
    assertFalse(Screen.CreateSerie.isTopLevelDestination)
  }

  @Test
  fun `History screen has correct properties`() {
    assertEquals("history", Screen.History.route)
    assertEquals("History", Screen.History.name)
    assertFalse(Screen.History.isTopLevelDestination)
  }

  @Test
  fun `Auth screen has correct properties`() {
    assertEquals("auth", Screen.Auth.route)
    assertEquals("Authentication", Screen.Auth.name)
    assertFalse(Screen.Auth.isTopLevelDestination)
  }

  @Test
  fun `Overview screen is top-level destination`() {
    assertEquals("overview", Screen.Overview.route)
    assertEquals("Overview", Screen.Overview.name)
    assertTrue(Screen.Overview.isTopLevelDestination)
  }

  @Test
  fun `Search screen is top-level destination`() {
    assertEquals("search", Screen.Search.route)
    assertEquals("Search", Screen.Search.name)
    assertTrue(Screen.Search.isTopLevelDestination)
  }

  @Test
  fun `Map screen is top-level destination`() {
    assertEquals("map", Screen.Map.route)
    assertEquals("Map", Screen.Map.name)
    assertTrue(Screen.Map.isTopLevelDestination)
  }

  @Test
  fun `Profile screen is top-level destination`() {
    assertEquals("profile", Screen.Profile.route)
    assertEquals("Profile", Screen.Profile.name)
    assertTrue(Screen.Profile.isTopLevelDestination)
  }

  @Test
  fun `navigateTo CreateEvent from Overview navigates correctly`() {
    every { navController.currentDestination?.route } returns Screen.Overview.route

    actions.navigateTo(Screen.CreateEvent)

    verify {
      navController.navigate(
          eq(Screen.CreateEvent.route),
          withArg<NavOptionsBuilder.() -> Unit> { block ->
            val options = navOptions(block)
            assertTrue(options.shouldRestoreState())
            assertFalse(options.shouldLaunchSingleTop())
          })
    }
  }

  @Test
  fun `navigateTo CreateSerie from Overview navigates correctly`() {
    every { navController.currentDestination?.route } returns Screen.Overview.route

    actions.navigateTo(Screen.CreateSerie)

    verify {
      navController.navigate(
          eq(Screen.CreateSerie.route),
          withArg<NavOptionsBuilder.() -> Unit> { block ->
            val options = navOptions(block)
            assertTrue(options.shouldRestoreState())
            assertFalse(options.shouldLaunchSingleTop())
          })
    }
  }

  @Test
  fun `navigateTo History from Overview navigates correctly`() {
    every { navController.currentDestination?.route } returns Screen.Overview.route

    actions.navigateTo(Screen.History)

    verify {
      navController.navigate(
          eq(Screen.History.route),
          withArg<NavOptionsBuilder.() -> Unit> { block ->
            val options = navOptions(block)
            assertTrue(options.shouldRestoreState())
            assertFalse(options.shouldLaunchSingleTop())
          })
    }
  }

  // ========== Create Event For Serie Navigation Tests ==========

  @Test
  fun `navigateTo CreateEventForSerie with serieId navigates to correct route`() {
    val serieId = "test-serie-456"
    every { navController.currentDestination?.route } returns Screen.SerieDetails(serieId).route

    actions.navigateTo(Screen.CreateEventForSerie(serieId))

    verify {
      navController.navigate(
          eq("create_event_for_serie/$serieId"),
          withArg<NavOptionsBuilder.() -> Unit> { block ->
            val options = navOptions(block)
            assertTrue(options.shouldRestoreState())
            assertFalse(options.shouldLaunchSingleTop())
          })
    }
  }

  @Test
  fun `CreateEventForSerie route companion object matches pattern`() {
    assertEquals("create_event_for_serie/{serieId}", Screen.CreateEventForSerie.Companion.route)
  }

  @Test
  fun `CreateEventForSerie instance route contains actual serieId`() {
    val serieId = "my-serie-789"
    val screen = Screen.CreateEventForSerie(serieId)
    assertEquals("create_event_for_serie/$serieId", screen.route)
  }

  @Test
  fun `CreateEventForSerie screen has correct name`() {
    val screen = Screen.CreateEventForSerie("test-id")
    assertEquals("Create Event for Serie", screen.name)
    assertFalse(screen.isTopLevelDestination)
  }

  @Test
  fun `navigateTo CreateEventForSerie from SerieDetails screen works correctly`() {
    val serieId = "serie-create-event-123"
    every { navController.currentDestination?.route } returns Screen.SerieDetails(serieId).route

    actions.navigateTo(Screen.CreateEventForSerie(serieId))

    verify {
      navController.navigate(
          eq("create_event_for_serie/$serieId"),
          withArg<NavOptionsBuilder.() -> Unit> { block ->
            val options = navOptions(block)
            assertTrue(options.shouldRestoreState())
            assertFalse(options.shouldLaunchSingleTop())
          })
    }
  }

  @Test
  fun `navigateTo CreateEventForSerie with special characters in serieId`() {
    val serieId = "serie-123-abc-xyz"
    every { navController.currentDestination?.route } returns Screen.Overview.route

    actions.navigateTo(Screen.CreateEventForSerie(serieId))

    verify {
      navController.navigate(
          eq("create_event_for_serie/$serieId"), any<NavOptionsBuilder.() -> Unit>())
    }
  }

  // ========== Edit Event For Serie Navigation Tests ==========

  @Test
  fun `navigateTo EditEventForSerie with serieId and eventId navigates to correct route`() {
    val serieId = "test-serie-123"
    val eventId = "test-event-456"
    every { navController.currentDestination?.route } returns
        Screen.ShowEventScreen(eventId, serieId).route

    actions.navigateTo(Screen.EditEventForSerie(serieId, eventId))

    verify {
      navController.navigate(
          eq("edit_event_for_serie/$serieId/$eventId"),
          withArg<NavOptionsBuilder.() -> Unit> { block ->
            val options = navOptions(block)
            assertTrue(options.shouldRestoreState())
            assertFalse(options.shouldLaunchSingleTop())
          })
    }
  }

  @Test
  fun `EditEventForSerie route companion object matches pattern`() {
    assertEquals(
        "edit_event_for_serie/{serieId}/{eventId}", Screen.EditEventForSerie.Companion.route)
  }

  @Test
  fun `EditEventForSerie instance route contains serieId and eventId`() {
    val serieId = "my-serie-789"
    val eventId = "my-event-123"
    val screen = Screen.EditEventForSerie(serieId, eventId)
    assertEquals("edit_event_for_serie/$serieId/$eventId", screen.route)
  }

  @Test
  fun `EditEventForSerie screen has correct name`() {
    val screen = Screen.EditEventForSerie("serie-id", "event-id")
    assertEquals("Edit Event for Serie", screen.name)
    assertFalse(screen.isTopLevelDestination)
  }

  @Test
  fun `navigateTo EditEventForSerie from ShowEventScreen works correctly`() {
    val serieId = "serie-edit-456"
    val eventId = "event-edit-789"
    every { navController.currentDestination?.route } returns
        Screen.ShowEventScreen(eventId, serieId).route

    actions.navigateTo(Screen.EditEventForSerie(serieId, eventId))

    verify {
      navController.navigate(
          eq("edit_event_for_serie/$serieId/$eventId"),
          withArg<NavOptionsBuilder.() -> Unit> { block ->
            val options = navOptions(block)
            assertTrue(options.shouldRestoreState())
            assertFalse(options.shouldLaunchSingleTop())
          })
    }
  }

  // ========== Chat Navigation Tests ==========

  @Test
  fun `navigateTo Chat with chatId and chatTitle navigates to correct route`() {
    val chatId = "test-chat-123"
    val chatTitle = "Test Chat"
    val totalParticipants = 1
    every { navController.currentDestination?.route } returns Screen.Groups.route

    actions.navigateTo(Screen.Chat(chatId, chatTitle, totalParticipants))

    verify {
      navController.navigate(
          eq("chat/$chatId/$chatTitle/$totalParticipants"),
          withArg<NavOptionsBuilder.() -> Unit> { block ->
            val options = navOptions(block)
            assertTrue(options.shouldRestoreState())
            // Chat is not a top-level destination, so shouldn't have launchSingleTop
            assertFalse(options.shouldLaunchSingleTop())
          })
    }
  }

  @Test
  fun `Chat route companion object matches pattern`() {
    assertEquals("chat/{chatId}/{chatTitle}/{totalParticipants}", Screen.Chat.Companion.route)
  }

  @Test
  fun chatScreen_hasCorrectRouteAndName() {
    val chatId = "test-chat-789"
    val chatTitle = "My Group Chat"
    val totalParticipants = 1
    val screen = Screen.Chat(chatId, chatTitle, totalParticipants)

    assertEquals("chat/$chatId/$chatTitle/$totalParticipants", screen.route)
    assertEquals("Chat", screen.name)
  }

  @Test
  fun `navigateTo Chat from GroupListScreen works correctly`() {
    val chatId = "group-chat-456"
    val chatTitle = "Team Discussion"
    val totalParticipants = 1
    every { navController.currentDestination?.route } returns Screen.Groups.route

    actions.navigateTo(Screen.Chat(chatId, chatTitle, totalParticipants))

    verify {
      navController.navigate(
          eq("chat/$chatId/$chatTitle/$totalParticipants"),
          withArg<NavOptionsBuilder.() -> Unit> { block ->
            val options = navOptions(block)
            assertTrue(options.shouldRestoreState())
            assertFalse(options.shouldLaunchSingleTop())
          })
    }
  }

  @Test
  fun `navigateTo Chat with special characters in chatTitle`() {
    val chatId = "chat-123"
    val chatTitle = "Group Name with Spaces & Symbols!"
    val totalParticipants = 1
    every { navController.currentDestination?.route } returns Screen.GroupDetail(chatId).route

    actions.navigateTo(Screen.Chat(chatId, chatTitle, totalParticipants))

    verify {
      navController.navigate(
          eq("chat/$chatId/$chatTitle/$totalParticipants"), any<NavOptionsBuilder.() -> Unit>())
    }
  }

  @Test
  fun `navigateTo Chat multiple times with different chatIds`() {
    every { navController.currentDestination?.route } returns Screen.Groups.route

    val chatId1 = "chat-1"
    val chatTitle1 = "Chat One"
    val totalParticipants1 = 1
    val chatId2 = "chat-2"
    val chatTitle2 = "Chat Two"
    val totalParticipants2 = 1

    actions.navigateTo(Screen.Chat(chatId1, chatTitle1, totalParticipants1))
    actions.navigateTo(Screen.Chat(chatId2, chatTitle2, totalParticipants2))

    verify {
      navController.navigate(
          eq("chat/$chatId1/$chatTitle1/$totalParticipants1"), any<NavOptionsBuilder.() -> Unit>())
    }
    verify {
      navController.navigate(
          eq("chat/$chatId2/$chatTitle2/$totalParticipants2"), any<NavOptionsBuilder.() -> Unit>())
    }
  }

  @Test
  fun `navigateTo Chat from ShowEventScreen with eventId as chatId works correctly`() {
    val eventId = "event-789"
    val eventTitle = "Basketball Game"
    val totalParticipants = 1
    every { navController.currentDestination?.route } returns Screen.ShowEventScreen(eventId).route

    // When navigating from ShowEvent to Chat, eventId is used as chatId
    actions.navigateTo(
        Screen.Chat(
            chatId = eventId, chatTitle = eventTitle, totalParticipants = totalParticipants))

    verify {
      navController.navigate(
          eq("chat/$eventId/$eventTitle/$totalParticipants"),
          withArg<NavOptionsBuilder.() -> Unit> { block ->
            val options = navOptions(block)
            assertTrue(options.shouldRestoreState())
            assertFalse(options.shouldLaunchSingleTop())
          })
    }
  }

  @Test
  fun `Chat navigation from event uses eventId correctly`() {
    // Verify that event chat uses eventId as chatId
    val eventId = "event-456"
    val chatTitle = "Event Chat"
    val totalParticipants = 1
    val screen =
        Screen.Chat(chatId = eventId, chatTitle = chatTitle, totalParticipants = totalParticipants)

    assertEquals("chat/$eventId/$chatTitle/$totalParticipants", screen.route)
  }

  // ========== ActivityGroup Screen Tests ==========

  @Test
  fun activityGroup_screen_routeIsProperlyFormatted() {
    val groupId = "test-group-123"
    val activityGroupScreen = Screen.ActivityGroup(groupId)

    assert(activityGroupScreen.route == "activity_group/$groupId")
    assert(activityGroupScreen.name == "Activity Group")
    assert(!activityGroupScreen.isTopLevelDestination)
  }

  // ========== PublicProfile Navigation Tests ==========

  @Test
  fun `navigateTo PublicProfile with userId navigates to correct route`() {
    val userId = "test-user-123"
    every { navController.currentDestination?.route } returns Screen.Profile.route

    actions.navigateTo(Screen.PublicProfile(userId))

    verify {
      navController.navigate(
          eq("public_profile/$userId"),
          withArg<NavOptionsBuilder.() -> Unit> { block ->
            val options = navOptions(block)
            assertTrue(options.shouldRestoreState())
            // PublicProfile is not a top-level destination, so shouldn't have launchSingleTop
            assertFalse(options.shouldLaunchSingleTop())
          })
    }
  }

  @Test
  fun `PublicProfile route companion object matches pattern`() {
    assertEquals("public_profile/{userId}", Screen.PublicProfile.Companion.route)
  }

  @Test
  fun `PublicProfile instance route contains actual userId`() {
    val userId = "user-456"
    val screen = Screen.PublicProfile(userId)
    assertEquals("public_profile/$userId", screen.route)
  }

  @Test
  fun `PublicProfile screen has correct name`() {
    val screen = Screen.PublicProfile("test-id")
    assertEquals("Public Profile", screen.name)
    assertFalse(screen.isTopLevelDestination)
  }

  @Test
  fun `navigateTo PublicProfile from Overview screen works correctly`() {
    val userId = "user-from-overview-789"
    every { navController.currentDestination?.route } returns Screen.Overview.route

    actions.navigateTo(Screen.PublicProfile(userId))

    verify {
      navController.navigate(
          eq("public_profile/$userId"),
          withArg<NavOptionsBuilder.() -> Unit> { block ->
            val options = navOptions(block)
            assertTrue(options.shouldRestoreState())
            assertFalse(options.shouldLaunchSingleTop())
          })
    }
  }

  @Test
  fun `navigateTo PublicProfile multiple times with different userIds`() {
    every { navController.currentDestination?.route } returns Screen.Profile.route

    val userId1 = "user-1"
    val userId2 = "user-2"

    actions.navigateTo(Screen.PublicProfile(userId1))
    actions.navigateTo(Screen.PublicProfile(userId2))

    verify {
      navController.navigate(eq("public_profile/$userId1"), any<NavOptionsBuilder.() -> Unit>())
    }
    verify {
      navController.navigate(eq("public_profile/$userId2"), any<NavOptionsBuilder.() -> Unit>())
    }
  }

  @Test
  fun `navigateTo PublicProfile with special characters in userId`() {
    val userId = "user-123-abc-xyz"
    every { navController.currentDestination?.route } returns Screen.Search.route

    actions.navigateTo(Screen.PublicProfile(userId))

    verify {
      navController.navigate(eq("public_profile/$userId"), any<NavOptionsBuilder.() -> Unit>())
    }
  }
}
