package com.android.joinme.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
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

  // ==================== Alignment Tests ====================

  @Test
  fun floatingActionBubbles_bottomEndAlignment_isDisplayed() {
    // Given: Bubbles with BOTTOM_END alignment
    composeTestRule.setContent {
      FloatingActionBubbles(
          visible = true,
          onDismiss = {},
          actions = listOf(BubbleAction(text = "Bottom End", onClick = {})),
          bubbleAlignment = BubbleAlignment.BOTTOM_END)
    }

    // Then: Bubbles are displayed
    composeTestRule.onNodeWithText("Bottom End").assertIsDisplayed()
  }

  @Test
  fun floatingActionBubbles_topStartAlignment_isDisplayed() {
    // Given: Bubbles with TOP_START alignment
    composeTestRule.setContent {
      FloatingActionBubbles(
          visible = true,
          onDismiss = {},
          actions = listOf(BubbleAction(text = "Top Start", onClick = {})),
          bubbleAlignment = BubbleAlignment.TOP_START)
    }

    // Then: Bubbles are displayed
    composeTestRule.onNodeWithText("Top Start").assertIsDisplayed()
  }

  @Test
  fun floatingActionBubbles_topEndAlignment_isDisplayed() {
    // Given: Bubbles with TOP_END alignment
    composeTestRule.setContent {
      FloatingActionBubbles(
          visible = true,
          onDismiss = {},
          actions = listOf(BubbleAction(text = "Top End", onClick = {})),
          bubbleAlignment = BubbleAlignment.TOP_END)
    }

    // Then: Bubbles are displayed
    composeTestRule.onNodeWithText("Top End").assertIsDisplayed()
  }

  @Test
  fun floatingActionBubbles_centerAlignment_isDisplayed() {
    // Given: Bubbles with CENTER alignment
    composeTestRule.setContent {
      FloatingActionBubbles(
          visible = true,
          onDismiss = {},
          actions = listOf(BubbleAction(text = "Center", onClick = {})),
          bubbleAlignment = BubbleAlignment.CENTER)
    }

    // Then: Bubbles are displayed
    composeTestRule.onNodeWithText("Center").assertIsDisplayed()
  }

  // ==================== Customization Tests ====================

  @Test
  fun floatingActionBubbles_withCustomColors_isDisplayed() {
    // Given: Bubbles with custom colors
    composeTestRule.setContent {
      FloatingActionBubbles(
          visible = true,
          onDismiss = {},
          actions = listOf(BubbleAction(text = "Custom Colors", onClick = {})),
          containerColor = androidx.compose.ui.graphics.Color.Red,
          contentColor = androidx.compose.ui.graphics.Color.White)
    }

    // Then: Bubbles are displayed with custom colors
    composeTestRule.onNodeWithText("Custom Colors").assertIsDisplayed()
  }

  @Test
  fun floatingActionBubbles_withCustomPadding_isDisplayed() {
    // Given: Bubbles with custom padding
    composeTestRule.setContent {
      FloatingActionBubbles(
          visible = true,
          onDismiss = {},
          actions = listOf(BubbleAction(text = "Custom Padding", onClick = {})),
          bottomPadding = 100.dp,
          horizontalPadding = 32.dp)
    }

    // Then: Bubbles are displayed
    composeTestRule.onNodeWithText("Custom Padding").assertIsDisplayed()
  }

  @Test
  fun floatingActionBubbles_withUseZIndex_isDisplayed() {
    // Given: Bubbles with useZIndex = true
    composeTestRule.setContent {
      FloatingActionBubbles(
          visible = true,
          onDismiss = {},
          actions = listOf(BubbleAction(text = "With ZIndex", onClick = {})),
          useZIndex = true)
    }

    // Then: Bubbles are displayed
    composeTestRule.onNodeWithText("With ZIndex").assertIsDisplayed()
  }

  @Test
  fun floatingActionBubbles_withCustomModifier_isDisplayed() {
    // Given: Bubbles with custom modifier
    composeTestRule.setContent {
      FloatingActionBubbles(
          visible = true,
          onDismiss = {},
          actions = listOf(BubbleAction(text = "Custom Modifier", onClick = {})),
          modifier = Modifier.testTag("customModifier"))
    }

    // Then: Bubbles are displayed
    composeTestRule.onNodeWithText("Custom Modifier").assertIsDisplayed()
  }

  // ==================== Icon Tests ====================

  @Test
  fun floatingActionBubbles_withoutIcon_displaysTextOnly() {
    // Given: Bubble action without icon
    composeTestRule.setContent {
      FloatingActionBubbles(
          visible = true,
          onDismiss = {},
          actions = listOf(BubbleAction(text = "No Icon", icon = null, onClick = {})))
    }

    // Then: Text is displayed
    composeTestRule.onNodeWithText("No Icon").assertIsDisplayed()
  }

  @Test
  fun floatingActionBubbles_mixedIconsAndNoIcons_displaysAll() {
    // Given: Mix of actions with and without icons
    composeTestRule.setContent {
      FloatingActionBubbles(
          visible = true,
          onDismiss = {},
          actions =
              listOf(
                  BubbleAction(text = "With Icon", icon = Icons.Default.Add, onClick = {}),
                  BubbleAction(text = "No Icon", icon = null, onClick = {})))
    }

    // Then: Both are displayed
    composeTestRule.onNodeWithText("With Icon").assertIsDisplayed()
    composeTestRule.onNodeWithText("No Icon").assertIsDisplayed()
  }

  // ==================== Scrim Tests ====================

  @Test
  fun floatingActionBubbles_scrim_isDisplayed() {
    // Given: Visible bubbles
    composeTestRule.setContent {
      FloatingActionBubbles(
          visible = true, onDismiss = {}, actions = listOf(BubbleAction(text = "Action", onClick = {})))
    }

    // Then: Scrim is displayed
    composeTestRule.onNodeWithTag(FloatingActionBubblesTestTags.SCRIM).assertExists()
  }

  @Test
  fun floatingActionBubbles_whenNotVisible_scrimDoesNotExist() {
    // Given: Invisible bubbles
    composeTestRule.setContent {
      FloatingActionBubbles(
          visible = false,
          onDismiss = {},
          actions = listOf(BubbleAction(text = "Action", onClick = {})))
    }

    // Then: Scrim does not exist
    composeTestRule.onNodeWithTag(FloatingActionBubblesTestTags.SCRIM).assertDoesNotExist()
  }

  // ==================== Multiple Actions Tests ====================

  @Test
  fun floatingActionBubbles_multipleActions_allHaveClickAction() {
    // Given: Multiple action bubbles
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

    // Then: All actions are displayed and clickable
    composeTestRule.onNodeWithText("Action 1").assertIsDisplayed().assertHasClickAction()
    composeTestRule.onNodeWithText("Action 2").assertIsDisplayed().assertHasClickAction()
    composeTestRule.onNodeWithText("Action 3").assertIsDisplayed().assertHasClickAction()
  }

  @Test
  fun floatingActionBubbles_firstAction_triggersCallback() {
    // Given: Multiple action bubbles
    var action1Clicked = false

    composeTestRule.setContent {
      FloatingActionBubbles(
          visible = true,
          onDismiss = {},
          actions =
              listOf(
                  BubbleAction(text = "Action 1", onClick = { action1Clicked = true }),
                  BubbleAction(text = "Action 2", onClick = {}),
                  BubbleAction(text = "Action 3", onClick = {})))
    }

    // When: Click first action
    composeTestRule.onNodeWithText("Action 1").performClick()

    // Then: Action 1 callback was called
    assert(action1Clicked) { "Action 1 should have been clicked" }
  }

  @Test
  fun floatingActionBubbles_lastAction_triggersCallback() {
    // Given: Multiple action bubbles
    var action3Clicked = false

    composeTestRule.setContent {
      FloatingActionBubbles(
          visible = true,
          onDismiss = {},
          actions =
              listOf(
                  BubbleAction(text = "Action 1", onClick = {}),
                  BubbleAction(text = "Action 2", onClick = {}),
                  BubbleAction(text = "Action 3", onClick = { action3Clicked = true })))
    }

    // When: Click last action
    composeTestRule.onNodeWithText("Action 3").performClick()

    // Then: Action 3 callback was called
    assert(action3Clicked) { "Action 3 should have been clicked" }
  }

  // ==================== Accessibility Tests ====================

  @Test
  fun floatingActionBubbles_hasClickableActions() {
    // Given: Bubbles with actions
    composeTestRule.setContent {
      FloatingActionBubbles(
          visible = true,
          onDismiss = {},
          actions =
              listOf(
                  BubbleAction(text = "Action 1", onClick = {}),
                  BubbleAction(text = "Action 2", onClick = {})))
    }

    // Then: Actions have click actions
    composeTestRule.onNodeWithText("Action 1").assertHasClickAction()
    composeTestRule.onNodeWithText("Action 2").assertHasClickAction()
  }

  @Test
  fun floatingActionBubbles_scrimHasClickAction() {
    // Given: Visible bubbles
    composeTestRule.setContent {
      FloatingActionBubbles(
          visible = true, onDismiss = {}, actions = listOf(BubbleAction(text = "Action", onClick = {})))
    }

    // Then: Scrim has click action
    composeTestRule.onNodeWithTag(FloatingActionBubblesTestTags.SCRIM).assertHasClickAction()
  }

  // ==================== Container Tests ====================

  @Test
  fun floatingActionBubbles_container_hasCorrectTestTag() {
    // Given: Visible bubbles
    composeTestRule.setContent {
      FloatingActionBubbles(
          visible = true, onDismiss = {}, actions = listOf(BubbleAction(text = "Action", onClick = {})))
    }

    // Then: Container has correct test tag
    composeTestRule
        .onNodeWithTag(FloatingActionBubblesTestTags.BUBBLE_CONTAINER)
        .assertExists()
        .assertIsDisplayed()
  }

  // ==================== Dismiss Behavior Tests ====================

  @Test
  fun floatingActionBubbles_dismissCalledOnce_whenBubbleClicked() {
    // Given: Bubbles with dismiss callback
    var dismissCount = 0

    composeTestRule.setContent {
      FloatingActionBubbles(
          visible = true,
          onDismiss = { dismissCount++ },
          actions = listOf(BubbleAction(text = "Action", onClick = {})))
    }

    // When: Click bubble
    composeTestRule.onNodeWithText("Action").performClick()

    // Then: Dismiss called exactly once
    assert(dismissCount == 1) { "Dismiss should be called exactly once, but was called $dismissCount times" }
  }

  @Test
  fun floatingActionBubbles_bothCallbacksCalled_whenBubbleClicked() {
    // Given: Bubbles with both callbacks
    var actionCalled = false
    var dismissCalled = false

    composeTestRule.setContent {
      FloatingActionBubbles(
          visible = true,
          onDismiss = { dismissCalled = true },
          actions = listOf(BubbleAction(text = "Action", onClick = { actionCalled = true })))
    }

    // When: Click bubble
    composeTestRule.onNodeWithText("Action").performClick()

    // Then: Both callbacks were called
    assert(actionCalled) { "Action onClick should have been called" }
    assert(dismissCalled) { "onDismiss should have been called" }
  }
}
