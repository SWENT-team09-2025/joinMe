package com.android.joinme

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.android.joinme.model.groups.Group
import com.android.joinme.model.groups.GroupRepositoryLocal
import com.android.joinme.model.groups.GroupRepositoryProvider
import com.android.joinme.model.profile.Profile
import com.android.joinme.model.profile.ProfileRepositoryLocal
import com.android.joinme.model.profile.ProfileRepositoryProvider
import com.android.joinme.ui.groups.GroupDetailScreenTestTags
import com.android.joinme.ui.groups.GroupListScreenTestTags
import com.android.joinme.ui.navigation.NavigationTestTags
import com.android.joinme.ui.navigation.Screen
import com.android.joinme.ui.chat.ChatScreenTestTags
import com.android.joinme.ui.overview.OverviewScreenTestTags
import com.android.joinme.ui.profile.FollowListScreenTestTags
import com.android.joinme.ui.profile.PublicProfileScreenTestTags
import com.android.joinme.ui.profile.ViewProfileTestTags
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Milestone 3 End-to-End tests for the JoinMe application.
 *
 * These tests verify complete user workflows for M3 features including:
 * - Public profile viewing
 * - Follow/unfollow functionality
 * - Following list navigation
 * - Direct messaging from public profile
 *
 * Tests complete user journeys from start to finish, testing the entire system including
 * navigation, data persistence, and UI interactions across multiple screens.
 */
@RunWith(AndroidJUnit4::class)
class M3JoinMeE2ETest {

  @get:Rule
  val permissionRule: GrantPermissionRule =
      GrantPermissionRule.grant(
          android.Manifest.permission.ACCESS_FINE_LOCATION,
          android.Manifest.permission.ACCESS_COARSE_LOCATION)

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var auth: FirebaseAuth

  // Test Data
  private val currentUserId = "test-user-id"
  private val targetUserId = "target-user-b"
  
  private val targetProfile = Profile(
      uid = targetUserId,
      username = "Target User B",
      email = "target@example.com",
      bio = "I am the target user for E2E testing",
      interests = listOf("Coding", "Testing"),
      createdAt = Timestamp.now(),
      updatedAt = Timestamp.now()
  )

  private val testGroup = Group(
      id = "test-group-id",
      name = "E2E Shared Group",
      description = "Group for testing profiles",
      memberIds = listOf(currentUserId, targetUserId),
      ownerId = currentUserId
  )

  @Before
  fun setup() {
    System.setProperty("IS_TEST_ENV", "true")
    auth = FirebaseAuth.getInstance()
    auth.signOut()

    // 1. Setup Repositories with Injected Data
    val profileRepo = ProfileRepositoryProvider.repository
    if (profileRepo is ProfileRepositoryLocal) {
      runBlocking {
        // Ensure current user exists
        profileRepo.createOrUpdateProfile(Profile(
            uid = currentUserId,
            username = "Current User",
            email = "current@joinme.com"
        ))
        // Inject Target User
        profileRepo.createOrUpdateProfile(targetProfile)
      }
    }

    val groupRepo = GroupRepositoryProvider.repository
    if (groupRepo is GroupRepositoryLocal) {
      // Clear previous groups to be safe
      groupRepo.clear()
      runBlocking {
        // Inject Group containing both users
        groupRepo.addGroup(testGroup)
      }
    }

    // 2. Start App
    composeTestRule.setContent {
      JoinMe(startDestination = Screen.Overview.route, enableNotificationPermissionRequest = false)
    }

    // Wait for app to settle
    composeTestRule.waitForIdle()
  }

  @Test
  fun e2e_viewPublicProfile_viaGroupDetails() {
    // GIVEN: User is on Overview, and shares a group with Target User

    // WHEN: Navigate to Profile -> Groups -> Group Details
    
    // 1. Go to Profile Tab
    composeTestRule
        .onNodeWithTag(NavigationTestTags.tabTag("Profile"), useUnmergedTree = true)
        .performClick()
    
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule.onAllNodesWithContentDescription("Profile").fetchSemanticsNodes().isNotEmpty()
    }

    // 2. Go to Groups List
    composeTestRule.waitForIdle()
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule.onAllNodesWithContentDescription("Group").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithContentDescription("Group").performClick()
    
    composeTestRule.waitUntil(timeoutMillis = 5000) {
        composeTestRule.onAllNodesWithTag(GroupListScreenTestTags.LIST, useUnmergedTree = true)
            .fetchSemanticsNodes().isNotEmpty()
    }

    // 3. Click on the Shared Group
    composeTestRule
        .onNodeWithTag(GroupListScreenTestTags.LIST, useUnmergedTree = true)
        .performScrollToNode(hasText(testGroup.name))
    
    composeTestRule.onNodeWithText(testGroup.name).performClick()

    // 4. Wait for Group Details
    composeTestRule.waitUntil(timeoutMillis = 5000) {
        composeTestRule.onAllNodesWithText(testGroup.name).fetchSemanticsNodes().isNotEmpty()
    }

    // WHEN: Click on Target User in Member List
    // We need to find the member item. We know the tag is memberItemTag(targetUserId)
    composeTestRule
        .onNodeWithTag("membersList")
        .performScrollToNode(hasTestTag(GroupDetailScreenTestTags.memberItemTag(targetUserId)))

    composeTestRule
        .onNodeWithTag(GroupDetailScreenTestTags.memberItemTag(targetUserId))
        .performClick()

    // THEN: Public Profile Screen should be displayed with correct info
    composeTestRule.waitForIdle()

    // Verify Screen
    composeTestRule.waitUntil(timeoutMillis = 5000) {
        composeTestRule.onAllNodesWithTag(PublicProfileScreenTestTags.SCREEN).fetchSemanticsNodes().isNotEmpty()
    }
    
    // Verify Username
    composeTestRule
        .onNodeWithTag(PublicProfileScreenTestTags.USERNAME)
        .assertTextEquals(targetProfile.username)

    // Verify Bio
    composeTestRule
        .onNodeWithTag(PublicProfileScreenTestTags.BIO, useUnmergedTree = true)
        .assertExists()
    composeTestRule
        .onNodeWithText(targetProfile.bio!!, substring = true)
        .assertExists()

    // Verify Interests - check each interest individually
    targetProfile.interests.forEach { interest ->
      composeTestRule
          .onNodeWithText(interest, substring = true)
          .assertExists()
    }

    // WHEN: Click the follow button
    composeTestRule
        .onNodeWithTag(PublicProfileScreenTestTags.FOLLOW_BUTTON)
        .performClick()

    composeTestRule.waitForIdle()

    // WHEN: Navigate back to ViewProfile screen using back button
    composeTestRule.onNodeWithContentDescription("Back").performClick()
    composeTestRule.waitForIdle()

    // Back through navigation stack: Public Profile -> Group Detail
    composeTestRule.onNodeWithContentDescription("Back").performClick()
    composeTestRule.waitForIdle()
    // Group Detail -> Groups List
    composeTestRule.onNodeWithContentDescription("Back").performClick()
    composeTestRule.waitForIdle()
    // THEN: Should be on ViewProfile screen
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule.onAllNodesWithTag(ViewProfileTestTags.SCREEN).fetchSemanticsNodes().isNotEmpty()
    }

    // Verify we're on ViewProfile screen
    composeTestRule
        .onNodeWithTag(ViewProfileTestTags.SCREEN)
        .assertExists()

    // Verify the Following count updated to 1
    composeTestRule
        .onNodeWithTag(ViewProfileTestTags.FOLLOWING_STAT, useUnmergedTree = true)
        .assertExists()
    composeTestRule.waitForIdle()
    composeTestRule
        .onAllNodesWithText("1")
        .filterToOne(hasTestTag(ViewProfileTestTags.FOLLOWING_STAT))
        .assertExists()

    // WHEN: Click on Following stat to see the list of followed users
    composeTestRule
        .onNodeWithTag(ViewProfileTestTags.FOLLOWING_STAT)
        .performClick()

    composeTestRule.waitForIdle()

    // THEN: Should be on FollowList screen showing following tab
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule.onAllNodesWithTag(FollowListScreenTestTags.SCREEN).fetchSemanticsNodes().isNotEmpty()
    }

    // Verify we're on the following tab
    composeTestRule
        .onNodeWithTag(FollowListScreenTestTags.FOLLOWING_TAB)
        .assertExists()

    // Verify the followed user appears in the list
    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(FollowListScreenTestTags.profileItemTag(targetUserId))
        .assertExists()

    // Verify the username is displayed
    composeTestRule
        .onNodeWithText(targetProfile.username, substring = true)
        .assertExists()

    // WHEN: Click on the followed user to navigate to their public profile
    composeTestRule
        .onNodeWithTag(FollowListScreenTestTags.profileItemTag(targetUserId))
        .performClick()

    composeTestRule.waitForIdle()

    // THEN: Should be on Public Profile screen
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule.onAllNodesWithTag(PublicProfileScreenTestTags.SCREEN).fetchSemanticsNodes().isNotEmpty()
    }

    // Verify we're on the public profile
    composeTestRule
        .onNodeWithTag(PublicProfileScreenTestTags.SCREEN)
        .assertExists()

    // WHEN: Click the Message button to navigate to chat
    composeTestRule
        .onNodeWithTag(PublicProfileScreenTestTags.MESSAGE_BUTTON)
        .performClick()

    composeTestRule.waitForIdle()

    // THEN: Chat screen should open
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule.onAllNodesWithTag(ChatScreenTestTags.SCREEN).fetchSemanticsNodes().isNotEmpty()
    }
      Thread.sleep(2000)


    // Verify chat screen is displayed
    composeTestRule
        .onNodeWithTag(ChatScreenTestTags.SCREEN)
        .assertExists()

    // Verify chat title shows the target user's username
    composeTestRule
        .onNodeWithTag(ChatScreenTestTags.TITLE, useUnmergedTree = true)
        .assertExists()
    composeTestRule
        .onNodeWithText(targetProfile.username, substring = true)
        .assertExists()
  }
}
