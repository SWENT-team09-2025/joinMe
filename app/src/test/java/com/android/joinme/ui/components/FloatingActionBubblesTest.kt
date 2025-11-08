package com.android.joinme.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Test suite for FloatingActionBubbles component with Robolectric
 *
 * Tests cover:
 * - Visibility and animation behavior
 * - Bubble action display and interaction
 * - Positioning (BubbleAlignment)
 * - Dismissal mechanisms (scrim, action click)
 * - Customization (colors, icons, test tags)
 * - Accessibility
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "w360dp-h640dp-normal-long-notround-any-420dpi-keyshidden-nonav")
class FloatingActionBubblesTest {

  @get:Rule val composeTestRule = createComposeRule()

  // ==================== Visibility Tests ====================

  @Test
  fun floatingActionBubbles_whenVisible_isDisplayed() {
    // Given: Bubbles with visible = true
    composeTestRule.setContent {
      FloatingActionBubbles(
          visible = true,
          onDismiss = {},
          actions = listOf(BubbleAction(text = "Action 1", onClick = {})))
    }

    // Then: Container is displayed
    composeTestRule
        .onNodeWithTag(FloatingActionBubblesTestTags.BUBBLE_CONTAINER)
        .assertIsDisplayed()
  }

  @Test
  fun floatingActionBubbles_whenNotVisible_isNotDisplayed() {
    // Given: Bubbles with visible = false
    composeTestRule.setContent {
      FloatingActionBubbles(
          visible = false,
          onDismiss = {},
          actions = listOf(BubbleAction(text = "Action 1", onClick = {})))
    }

    // Then: Container does not exist
    composeTestRule
        .onNodeWithTag(FloatingActionBubblesTestTags.BUBBLE_CONTAINER)
        .assertDoesNotExist()
  }

  // ==================== Action Display Tests ====================

  @Test
  fun floatingActionBubbles_displaysMultipleActions() {
    // Given: Bubbles with multiple actions
    composeTestRule.setContent {
      FloatingActionBubbles(
          visible = true,
          onDismiss = {},
          actions =
              listOf(
                  BubbleAction(text = "Action 1", onClick = {}),
                  BubbleAction(text = "Action 2", onClick = {}),
                  BubbleAction(text = "Action 3", onClick = {})))
    }

    // Then: All actions are displayed
    composeTestRule.onNodeWithText("Action 1").assertIsDisplayed()
    composeTestRule.onNodeWithText("Action 2").assertIsDisplayed()
    composeTestRule.onNodeWithText("Action 3").assertIsDisplayed()
  }

  @Test
  fun floatingActionBubbles_displaysActionsWithIcons() {
    // Given: Bubbles with icons
    composeTestRule.setContent {
      FloatingActionBubbles(
          visible = true,
          onDismiss = {},
          actions =
              listOf(
                  BubbleAction(text = "Add", icon = Icons.Default.Add, onClick = {}),
                  BubbleAction(text = "Edit", icon = Icons.Default.Edit, onClick = {})))
    }

    // Then: Actions with content descriptions are displayed
    composeTestRule.onNodeWithContentDescription("Add").assertIsDisplayed()
    composeTestRule.onNodeWithContentDescription("Edit").assertIsDisplayed()
  }

  // ==================== Interaction Tests ====================

  @Test
  fun floatingActionBubbles_scrimClick_callsOnDismiss() {
    // Given: Bubbles with dismiss callback
    var dismissCalled = false

    composeTestRule.setContent {
      FloatingActionBubbles(
          visible = true,
          onDismiss = { dismissCalled = true },
          actions = listOf(BubbleAction(text = "Action", onClick = {})))
    }

    // When: User clicks scrim
    composeTestRule.onNodeWithTag(FloatingActionBubblesTestTags.SCRIM).performClick()

    // Then: onDismiss was called
    assert(dismissCalled) { "onDismiss should have been called" }
  }

  @Test
  fun floatingActionBubbles_bubbleClick_callsOnClick() {
    // Given: Bubbles with action callback
    var actionClicked = false

    composeTestRule.setContent {
      FloatingActionBubbles(
          visible = true,
          onDismiss = {},
          actions =
              listOf(BubbleAction(text = "Clickable Action", onClick = { actionClicked = true })))
    }

    // When: User clicks the bubble
    composeTestRule.onNodeWithText("Clickable Action").performClick()

    // Then: onClick was called
    assert(actionClicked) { "Action onClick should have been called" }
  }

  @Test
  fun floatingActionBubbles_bubbleClick_callsOnDismiss() {
    // Given: Bubbles with dismiss callback
    var dismissCalled = false

    composeTestRule.setContent {
      FloatingActionBubbles(
          visible = true,
          onDismiss = { dismissCalled = true },
          actions = listOf(BubbleAction(text = "Action", onClick = {})))
    }

    // When: User clicks a bubble
    composeTestRule.onNodeWithText("Action").performClick()

    // Then: onDismiss was called (auto-dismiss after action)
    assert(dismissCalled) { "onDismiss should have been called after action" }
  }

  // ==================== Test Tags Tests ====================

  @Test
  fun floatingActionBubbles_usesCustomTestTags() {
    // Given: Bubbles with custom test tags
    composeTestRule.setContent {
      FloatingActionBubbles(
          visible = true,
          onDismiss = {},
          actions =
              listOf(
                  BubbleAction(
                      text = "Custom Tag Action", onClick = {}, testTag = "customTestTag")))
    }

    // Then: Custom test tag is present
    composeTestRule.onNodeWithTag("customTestTag").assertIsDisplayed().assertHasClickAction()
  }

  @Test
  fun floatingActionBubbles_usesDefaultTestTagsWhenNotProvided() {
    // Given: Bubbles without custom test tags
    composeTestRule.setContent {
      FloatingActionBubbles(
          visible = true,
          onDismiss = {},
          actions =
              listOf(
                  BubbleAction(text = "Action 1", onClick = {}),
                  BubbleAction(text = "Action 2", onClick = {})))
    }

    // Then: Default test tags are present (floatingBubble_0, floatingBubble_1)
    composeTestRule.onNodeWithTag(FloatingActionBubblesTestTags.bubbleTag(0)).assertIsDisplayed()

    composeTestRule.onNodeWithTag(FloatingActionBubblesTestTags.bubbleTag(1)).assertIsDisplayed()
  }

  // ==================== Positioning Tests ====================

  @Test
  fun floatingActionBubbles_bottomCenterAlignment_isDisplayed() {
    // Given: Bubbles with BOTTOM_CENTER alignment
    composeTestRule.setContent {
      FloatingActionBubbles(
          visible = true,
          onDismiss = {},
          actions = listOf(BubbleAction(text = "Bottom Center", onClick = {})),
          bubbleAlignment = BubbleAlignment.BOTTOM_CENTER)
    }

    // Then: Bubbles are displayed (positioning verified visually)
    composeTestRule.onNodeWithText("Bottom Center").assertIsDisplayed()
  }

  // ==================== Edge Cases Tests ====================

  @Test
  fun floatingActionBubbles_emptyActionsList_displaysNothing() {
    // Given: Bubbles with empty actions list
    composeTestRule.setContent {
      FloatingActionBubbles(visible = true, onDismiss = {}, actions = emptyList())
    }

    // Then: Container is displayed but no actions
    composeTestRule
        .onNodeWithTag(FloatingActionBubblesTestTags.BUBBLE_CONTAINER)
        .assertIsDisplayed()

    // Verify no bubble actions exist
    composeTestRule.onNodeWithTag(FloatingActionBubblesTestTags.bubbleTag(0)).assertDoesNotExist()
  }
}
