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

/**
 * Test suite for FloatingActionBubbles component
 *
 * Tests cover:
 * - Visibility and animation behavior
 * - Bubble action display and interaction
 * - Positioning (BubbleAlignment)
 * - Dismissal mechanisms (scrim, action click)
 * - Customization (colors, icons, test tags)
 * - Accessibility
 */
class FloatingActionBubblesTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ==================== Visibility Tests ====================

    @Test
    fun floatingActionBubbles_whenVisible_isDisplayed() {
        // Given: Bubbles with visible = true
        composeTestRule.setContent {
            FloatingActionBubbles(
                visible = true,
                onDismiss = {},
                actions = listOf(
                    BubbleAction(text = "Action 1", onClick = {})
                )
            )
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
                actions = listOf(
                    BubbleAction(text = "Action 1", onClick = {})
                )
            )
        }

        // Then: Container does not exist
        composeTestRule
            .onNodeWithTag(FloatingActionBubblesTestTags.BUBBLE_CONTAINER)
            .assertDoesNotExist()
    }

    // ==================== Action Display Tests ====================

    @Test
    fun floatingActionBubbles_displaysSingleAction() {
        // Given: Bubbles with one action
        composeTestRule.setContent {
            FloatingActionBubbles(
                visible = true,
                onDismiss = {},
                actions = listOf(
                    BubbleAction(text = "Single Action", onClick = {})
                )
            )
        }

        // Then: Action is displayed
        composeTestRule
            .onNodeWithText("Single Action")
            .assertIsDisplayed()
    }

    @Test
    fun floatingActionBubbles_displaysMultipleActions() {
        // Given: Bubbles with multiple actions
        composeTestRule.setContent {
            FloatingActionBubbles(
                visible = true,
                onDismiss = {},
                actions = listOf(
                    BubbleAction(text = "Action 1", onClick = {}),
                    BubbleAction(text = "Action 2", onClick = {}),
                    BubbleAction(text = "Action 3", onClick = {})
                )
            )
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
                actions = listOf(
                    BubbleAction(
                        text = "Add",
                        icon = Icons.Default.Add,
                        onClick = {}
                    ),
                    BubbleAction(
                        text = "Edit",
                        icon = Icons.Default.Edit,
                        onClick = {}
                    )
                )
            )
        }

        // Then: Actions with content descriptions are displayed
        composeTestRule.onNodeWithContentDescription("Add").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Edit").assertIsDisplayed()
    }

    @Test
    fun floatingActionBubbles_displaysActionsWithoutIcons() {
        // Given: Bubbles without icons
        composeTestRule.setContent {
            FloatingActionBubbles(
                visible = true,
                onDismiss = {},
                actions = listOf(
                    BubbleAction(text = "No Icon Action", icon = null, onClick = {})
                )
            )
        }

        // Then: Action text is displayed (no icon)
        composeTestRule.onNodeWithText("No Icon Action").assertIsDisplayed()
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
                actions = listOf(
                    BubbleAction(text = "Action", onClick = {})
                )
            )
        }

        // When: User clicks scrim
        composeTestRule
            .onNodeWithTag(FloatingActionBubblesTestTags.SCRIM)
            .performClick()

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
                actions = listOf(
                    BubbleAction(
                        text = "Clickable Action",
                        onClick = { actionClicked = true }
                    )
                )
            )
        }

        // When: User clicks the bubble
        composeTestRule
            .onNodeWithText("Clickable Action")
            .performClick()

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
                actions = listOf(
                    BubbleAction(text = "Action", onClick = {})
                )
            )
        }

        // When: User clicks a bubble
        composeTestRule
            .onNodeWithText("Action")
            .performClick()

        // Then: onDismiss was called (auto-dismiss after action)
        assert(dismissCalled) { "onDismiss should have been called after action" }
    }

    @Test
    fun floatingActionBubbles_multipleActions_eachClickable() {
        // Given: Bubbles with multiple actions
        var action1Clicked = false
        var action2Clicked = false
        var action3Clicked = false

        composeTestRule.setContent {
            FloatingActionBubbles(
                visible = true,
                onDismiss = {},
                actions = listOf(
                    BubbleAction(text = "Action 1", onClick = { action1Clicked = true }),
                    BubbleAction(text = "Action 2", onClick = { action2Clicked = true }),
                    BubbleAction(text = "Action 3", onClick = { action3Clicked = true })
                )
            )
        }

        // When: User clicks each action
        composeTestRule.onNodeWithText("Action 1").performClick()
        assert(action1Clicked)

        composeTestRule.onNodeWithText("Action 2").performClick()
        assert(action2Clicked)

        composeTestRule.onNodeWithText("Action 3").performClick()
        assert(action3Clicked)
    }

    // ==================== Test Tags Tests ====================

    @Test
    fun floatingActionBubbles_usesCustomTestTags() {
        // Given: Bubbles with custom test tags
        composeTestRule.setContent {
            FloatingActionBubbles(
                visible = true,
                onDismiss = {},
                actions = listOf(
                    BubbleAction(
                        text = "Custom Tag Action",
                        onClick = {},
                        testTag = "customTestTag"
                    )
                )
            )
        }

        // Then: Custom test tag is present
        composeTestRule
            .onNodeWithTag("customTestTag")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun floatingActionBubbles_usesDefaultTestTagsWhenNotProvided() {
        // Given: Bubbles without custom test tags
        composeTestRule.setContent {
            FloatingActionBubbles(
                visible = true,
                onDismiss = {},
                actions = listOf(
                    BubbleAction(text = "Action 1", onClick = {}),
                    BubbleAction(text = "Action 2", onClick = {})
                )
            )
        }

        // Then: Default test tags are present (floatingBubble_0, floatingBubble_1)
        composeTestRule
            .onNodeWithTag(FloatingActionBubblesTestTags.bubbleTag(0))
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithTag(FloatingActionBubblesTestTags.bubbleTag(1))
            .assertIsDisplayed()
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
                bubbleAlignment = BubbleAlignment.BOTTOM_CENTER
            )
        }

        // Then: Bubbles are displayed (positioning verified visually)
        composeTestRule
            .onNodeWithText("Bottom Center")
            .assertIsDisplayed()
    }

    @Test
    fun floatingActionBubbles_bottomEndAlignment_isDisplayed() {
        // Given: Bubbles with BOTTOM_END alignment
        composeTestRule.setContent {
            FloatingActionBubbles(
                visible = true,
                onDismiss = {},
                actions = listOf(BubbleAction(text = "Bottom End", onClick = {})),
                bubbleAlignment = BubbleAlignment.BOTTOM_END
            )
        }

        // Then: Bubbles are displayed
        composeTestRule
            .onNodeWithText("Bottom End")
            .assertIsDisplayed()
    }

    @Test
    fun floatingActionBubbles_topStartAlignment_isDisplayed() {
        // Given: Bubbles with TOP_START alignment
        composeTestRule.setContent {
            FloatingActionBubbles(
                visible = true,
                onDismiss = {},
                actions = listOf(BubbleAction(text = "Top Start", onClick = {})),
                bubbleAlignment = BubbleAlignment.TOP_START,
                bottomPadding = 0.dp
            )
        }

        // Then: Bubbles are displayed
        composeTestRule
            .onNodeWithText("Top Start")
            .assertIsDisplayed()
    }

    @Test
    fun floatingActionBubbles_topEndAlignment_isDisplayed() {
        // Given: Bubbles with TOP_END alignment
        composeTestRule.setContent {
            FloatingActionBubbles(
                visible = true,
                onDismiss = {},
                actions = listOf(BubbleAction(text = "Top End", onClick = {})),
                bubbleAlignment = BubbleAlignment.TOP_END,
                bottomPadding = 0.dp
            )
        }

        // Then: Bubbles are displayed
        composeTestRule
            .onNodeWithText("Top End")
            .assertIsDisplayed()
    }

    @Test
    fun floatingActionBubbles_centerAlignment_isDisplayed() {
        // Given: Bubbles with CENTER alignment
        composeTestRule.setContent {
            FloatingActionBubbles(
                visible = true,
                onDismiss = {},
                actions = listOf(BubbleAction(text = "Center", onClick = {})),
                bubbleAlignment = BubbleAlignment.CENTER
            )
        }

        // Then: Bubbles are displayed
        composeTestRule
            .onNodeWithText("Center")
            .assertIsDisplayed()
    }

    // ==================== Accessibility Tests ====================

    @Test
    fun floatingActionBubbles_bubblesAreClickable() {
        // Given: Bubbles are displayed
        composeTestRule.setContent {
            FloatingActionBubbles(
                visible = true,
                onDismiss = {},
                actions = listOf(
                    BubbleAction(text = "Clickable", onClick = {})
                )
            )
        }

        // Then: Bubble has click action
        composeTestRule
            .onNodeWithText("Clickable")
            .assertHasClickAction()
    }

    @Test
    fun floatingActionBubbles_scrimIsClickable() {
        // Given: Bubbles are displayed
        composeTestRule.setContent {
            FloatingActionBubbles(
                visible = true,
                onDismiss = {},
                actions = listOf(BubbleAction(text = "Action", onClick = {}))
            )
        }

        // Then: Scrim has click action
        composeTestRule
            .onNodeWithTag(FloatingActionBubblesTestTags.SCRIM)
            .assertHasClickAction()
    }

    @Test
    fun floatingActionBubbles_iconsHaveContentDescriptions() {
        // Given: Bubbles with icons
        composeTestRule.setContent {
            FloatingActionBubbles(
                visible = true,
                onDismiss = {},
                actions = listOf(
                    BubbleAction(
                        text = "Delete Item",
                        icon = Icons.Default.Delete,
                        onClick = {}
                    )
                )
            )
        }

        // Then: Icon has content description matching text
        composeTestRule
            .onNodeWithContentDescription("Delete Item")
            .assertIsDisplayed()
    }

    // ==================== Edge Cases Tests ====================

    @Test
    fun floatingActionBubbles_emptyActionsList_displaysNothing() {
        // Given: Bubbles with empty actions list
        composeTestRule.setContent {
            FloatingActionBubbles(
                visible = true,
                onDismiss = {},
                actions = emptyList()
            )
        }

        // Then: Container is displayed but no actions
        composeTestRule
            .onNodeWithTag(FloatingActionBubblesTestTags.BUBBLE_CONTAINER)
            .assertIsDisplayed()

        // Verify no bubble actions exist
        composeTestRule
            .onNodeWithTag(FloatingActionBubblesTestTags.bubbleTag(0))
            .assertDoesNotExist()
    }

    @Test
    fun floatingActionBubbles_customColors_appliesCorrectly() {
        // Given: Bubbles with custom colors
        composeTestRule.setContent {
            FloatingActionBubbles(
                visible = true,
                onDismiss = {},
                actions = listOf(BubbleAction(text = "Custom Colors", onClick = {})),
                containerColor = Color.White,
                contentColor = Color.Black
            )
        }

        // Then: Bubbles are displayed with custom colors (visual verification)
        composeTestRule
            .onNodeWithText("Custom Colors")
            .assertIsDisplayed()
    }

    @Test
    fun floatingActionBubbles_customPadding_appliesCorrectly() {
        // Given: Bubbles with custom padding
        composeTestRule.setContent {
            FloatingActionBubbles(
                visible = true,
                onDismiss = {},
                actions = listOf(BubbleAction(text = "Custom Padding", onClick = {})),
                bottomPadding = 200.dp,
                horizontalPadding = 32.dp
            )
        }

        // Then: Bubbles are displayed with custom padding (visual verification)
        composeTestRule
            .onNodeWithText("Custom Padding")
            .assertIsDisplayed()
    }
}