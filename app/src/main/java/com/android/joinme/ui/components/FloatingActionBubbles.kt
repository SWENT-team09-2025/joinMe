package com.android.joinme.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.android.joinme.ui.theme.ScrimOverlayColorDarkTheme
import com.android.joinme.ui.theme.ScrimOverlayColorLightTheme

/**
 * Data class representing a single action bubble option
 *
 * @property text The label text to display (required)
 * @property icon The icon to display (optional, defaults to Add icon)
 * @property onClick Callback when this bubble is tapped
 * @property testTag Optional test tag for UI testing
 */
data class BubbleAction(
    val text: String,
    val icon: ImageVector? = null,
    val onClick: () -> Unit,
    val testTag: String? = null
)

/** Enum class defining where bubbles should be positioned relative to the trigger */
enum class BubbleAlignment {
  /** Bubbles appear at bottom center (above FAB) */
  BOTTOM_CENTER,
  /** Bubbles appear at bottom right (above and slightly right of FAB) */
  BOTTOM_END,
  /** Bubbles appear at top left (below and left of trigger) */
  TOP_START,
  /** Bubbles appear at top right (below and right of trigger) */
  TOP_END,
  /** Bubbles appear at center */
  CENTER
}

/**
 * Floating action bubbles test tags
 *
 * Used for UI testing to identify components
 */
object FloatingActionBubblesTestTags {
  const val BUBBLE_CONTAINER = "floatingBubblesContainer"
  const val SCRIM = "floatingBubblesScrim"

  fun bubbleTag(index: Int) = "floatingBubble_$index"
}

/**
 * Generic floating action bubbles component
 *
 * Displays multiple action bubbles above a main FAB. This is a reusable component that can be used
 * across different screens (e.g. Groups; for joining a group with a link or create a group,
 * Overview; for adding an event or adding a series of events, etc.)
 *
 * Features:
 * - Animated appearance/disappearance with smooth fade-in
 * - Dismissible by tapping outside (scrim) or back press
 * - Configurable number of bubbles with custom text, icons, and actions
 * - Customizable positioning (bottom-right, top-left, etc.)
 * - Dark mode support with adaptive scrim opacity
 *
 * Usage example 1 (GroupListScreen - bottom right):
 * ```
 * FloatingActionBubbles(
 *     visible = showBubbles,
 *     onDismiss = { showBubbles = false },
 *     actions = listOf(
 *         BubbleAction(
 *             text = "Join with link",
 *             icon = Icons.Default.Link,
 *             onClick = { /* action */ }
 *         ),
 *         BubbleAction(
 *             text = "Create a group",
 *             icon = Icons.Default.Add,
 *             onClick = { /* action */ }
 *         )
 *     ),
 *     bubbleAlignment = BubbleAlignment.BOTTOM_END
 * )
 * ```
 *
 * Usage example 2 (Group card menu - top left):
 * ```
 * FloatingActionBubbles(
 *     visible = showMenu,
 *     onDismiss = { showMenu = false },
 *     actions = listOf(
 *         BubbleAction(text = "View details", onClick = { /* ... */ }),
 *         BubbleAction(text = "Leave group", onClick = { /* ... */ }),
 *         BubbleAction(text = "Share group", onClick = { /* ... */ })
 *     ),
 *     bubbleAlignment = BubbleAlignment.TOP_START,
 *     bottomPadding = 0.dp  // No bottom padding for top-aligned menus
 * )
 * ```
 *
 * @param visible Whether the bubbles are visible
 * @param onDismiss Callback when user dismisses the bubbles (tap outside or back)
 * @param actions List of bubble actions to display (from top to bottom)
 * @param modifier Optional modifier for the container
 * @param bubbleAlignment Where to position the bubbles. Default: BOTTOM_CENTER
 * @param bottomPadding Padding from the bottom (for bottom-aligned bubbles). Default: 80.dp
 * @param horizontalPadding Horizontal padding from edges. Default: 16.dp
 * @param containerColor Background color of the bubbles. Default: secondary color
 * @param contentColor Text and icon color. Default: onSecondary color
 * @param useZIndex Whether to use zIndex to float over content without pushing it. Use true for
 *   card menus. Default: false
 */
@Composable
fun FloatingActionBubbles(
    visible: Boolean,
    onDismiss: () -> Unit,
    actions: List<BubbleAction>,
    modifier: Modifier = Modifier,
    bubbleAlignment: BubbleAlignment = BubbleAlignment.BOTTOM_CENTER,
    bottomPadding: Dp = 80.dp,
    horizontalPadding: Dp = 16.dp,
    containerColor: Color = MaterialTheme.colorScheme.secondary,
    contentColor: Color = MaterialTheme.colorScheme.onSecondary,
    useZIndex: Boolean = false
) {
  // Animate scrim opacity for smooth fade-in/fade-out
  val scrimAlpha by
      animateFloatAsState(
          targetValue = if (visible) 1f else 0f,
          animationSpec = tween(durationMillis = 300),
          label = "scrimAlpha")

  // Use semantic scrim colors from Color.kt for consistency
  val isDarkTheme = isSystemInDarkTheme()
  val scrimBaseColor = if (isDarkTheme) ScrimOverlayColorDarkTheme else ScrimOverlayColorLightTheme
  val scrimColor = scrimBaseColor.copy(alpha = scrimBaseColor.alpha * scrimAlpha)

  // Animated visibility for bubbles
  AnimatedVisibility(
      visible = visible,
      enter = fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.8f),
      exit = fadeOut(animationSpec = tween(300)) + scaleOut(targetScale = 0.8f),
      modifier = modifier.testTag(FloatingActionBubblesTestTags.BUBBLE_CONTAINER)) {
        Box(
            modifier =
                Modifier.fillMaxSize().then(if (useZIndex) Modifier.zIndex(10f) else Modifier)) {
              // Scrim (transparent overlay) to dismiss bubbles with animated fade
              Box(
                  modifier =
                      Modifier.fillMaxSize()
                          .background(scrimColor)
                          .clickable(
                              interactionSource = remember { MutableInteractionSource() },
                              indication = null,
                              onClick = onDismiss)
                          .testTag(FloatingActionBubblesTestTags.SCRIM))

              // Bubbles positioned based on alignment parameter
              val alignment =
                  when (bubbleAlignment) {
                    BubbleAlignment.BOTTOM_CENTER -> Alignment.BottomCenter
                    BubbleAlignment.BOTTOM_END -> Alignment.BottomEnd
                    BubbleAlignment.TOP_START -> Alignment.TopStart
                    BubbleAlignment.TOP_END -> Alignment.TopEnd
                    BubbleAlignment.CENTER -> Alignment.Center
                  }

              Column(
                  modifier =
                      Modifier.align(alignment)
                          .then(
                              when (bubbleAlignment) {
                                BubbleAlignment.BOTTOM_CENTER ->
                                    Modifier.padding(bottom = bottomPadding)
                                BubbleAlignment.BOTTOM_END ->
                                    Modifier.padding(
                                        bottom = bottomPadding, end = horizontalPadding)
                                BubbleAlignment.TOP_START ->
                                    Modifier.padding(
                                        top = horizontalPadding, start = horizontalPadding)
                                BubbleAlignment.TOP_END ->
                                    Modifier.padding(
                                        top = horizontalPadding, end = horizontalPadding)
                                BubbleAlignment.CENTER -> Modifier.padding(horizontalPadding)
                              }),
                  horizontalAlignment =
                      when (bubbleAlignment) {
                        BubbleAlignment.BOTTOM_CENTER,
                        BubbleAlignment.CENTER -> Alignment.CenterHorizontally
                        BubbleAlignment.BOTTOM_END,
                        BubbleAlignment.TOP_END -> Alignment.End
                        BubbleAlignment.TOP_START -> Alignment.Start
                      },
                  verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Display all action bubbles
                    actions.forEachIndexed { index, action ->
                      FloatingActionBubble(
                          text = action.text,
                          icon = action.icon,
                          onClick = {
                            action.onClick()
                            onDismiss()
                          },
                          testTag =
                              action.testTag ?: FloatingActionBubblesTestTags.bubbleTag(index),
                          containerColor = containerColor,
                          contentColor = contentColor)
                    }
                  }
            }
      }
}

/**
 * Individual floating action bubble
 *
 * Smaller and more rounded than standard FABs to match Figma design
 *
 * @param text Label text for the bubble (required)
 * @param icon Icon to display (optional)
 * @param onClick Callback when bubble is tapped
 * @param testTag Test tag for UI testing
 * @param containerColor Background color of the bubble
 * @param contentColor Color of the text and icon
 */
@Composable
private fun FloatingActionBubble(
    text: String,
    icon: ImageVector?,
    onClick: () -> Unit,
    testTag: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
  // Use smaller FAB with more rounded corners to match Figma
  FloatingActionButton(
      onClick = onClick,
      containerColor = containerColor,
      shape = RoundedCornerShape(24.dp), // More rounded corners
      modifier = modifier.testTag(testTag).height(48.dp) // Smaller height
      ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), // Reduced padding
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)) { // Reduced spacing
              // Show icon only if provided
              icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = text,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp) // Smaller icon
                    )
              }
              Text(
                  text = text,
                  color = contentColor,
                  style = MaterialTheme.typography.bodyMedium // Slightly smaller text
                  )
            }
      }
}
