package com.android.joinme.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Centralized dimension constants for the JoinMe app.
 *
 * This object provides a single source of truth for all spacing, sizing, and typography dimensions
 * used throughout the application. Dimensions are organized by feature/component for easy
 * navigation and maintenance.
 *
 * Benefits:
 * - Consistent spacing and sizing across the app
 * - Easy to update dimensions globally
 * - Better readability in components
 * - Scales properly with accessibility settings
 *
 * Usage:
 * ```
 * import com.android.joinme.ui.theme.Dimens
 *
 * Text(
 *     text = "Title",
 *     fontSize = Dimens.FontSize.title,
 *     modifier = Modifier.padding(Dimens.Spacing.medium)
 * )
 * ```
 *
 * AI-assisted implementation — manually reviewed and adjusted for project style.
 */
object Dimens {

  /**
   * Standard spacing values used throughout the app. Follow Material Design spacing guidelines (4dp
   * base unit).
   */
  object Spacing {
    val extraSmall: Dp = 4.dp
    val small: Dp = 8.dp
    val medium: Dp = 16.dp
    val large: Dp = 24.dp
    val extraLarge: Dp = 32.dp
    val huge: Dp = 48.dp

    // Specific spacing for common patterns
    val fieldSpacing: Dp = 24.dp
    val sectionSpacing: Dp = 16.dp
    val itemSpacing: Dp = 12.dp
  }

  /** Padding values for consistent internal spacing. */
  object Padding {
    val none: Dp = 0.dp
    val extraSmall: Dp = 4.dp
    val small: Dp = 8.dp
    val medium: Dp = 16.dp
    val large: Dp = 24.dp
    val extraLarge: Dp = 32.dp

    // Screen-level padding
    val screenHorizontal: Dp = 24.dp
    val screenVertical: Dp = 16.dp
  }

  /** Corner radius values for consistent rounded corners. */
  object CornerRadius {
    val none: Dp = 0.dp
    val small: Dp = 4.dp
    val medium: Dp = 8.dp
    val large: Dp = 12.dp
    val extraLarge: Dp = 16.dp
    val pill: Dp = 24.dp
    val circle: Dp = 999.dp // Use with circular components
  }

  /** Border width values. */
  object BorderWidth {
    val thin: Dp = 1.dp
    val medium: Dp = 2.dp
    val thick: Dp = 4.dp
  }

  /** Typography font sizes. Based on Material Design type scale. */
  object FontSize {
    // Display sizes
    val displayLarge: TextUnit = 57.sp
    val displayMedium: TextUnit = 45.sp
    val displaySmall: TextUnit = 36.sp

    // Headline sizes
    val headlineLarge: TextUnit = 32.sp
    val headlineMedium: TextUnit = 28.sp
    val headlineSmall: TextUnit = 24.sp

    // Title sizes
    val titleLarge: TextUnit = 22.sp
    val titleMedium: TextUnit = 18.sp
    val titleSmall: TextUnit = 16.sp

    // Body sizes
    val bodyLarge: TextUnit = 16.sp
    val bodyMedium: TextUnit = 14.sp
    val bodySmall: TextUnit = 12.sp

    // Label sizes
    val labelLarge: TextUnit = 14.sp
    val labelMedium: TextUnit = 12.sp
    val labelSmall: TextUnit = 11.sp
  }

  /** Icon sizes for consistent icon dimensions. */
  object IconSize {
    val small: Dp = 16.dp
    val medium: Dp = 24.dp
    val large: Dp = 32.dp
    val extraLarge: Dp = 48.dp
  }

  /** Button dimensions. */
  object Button {
    val minHeight: Dp = 48.dp
    val standardHeight: Dp = 56.dp
    val largeHeight: Dp = 64.dp
    val cornerRadius: Dp = 12.dp
  }

  /** Profile-related dimensions. */
  object Profile {
    // Profile photos
    val photoSmall: Dp = 40.dp
    val photoMedium: Dp = 80.dp
    val photoLarge: Dp = 140.dp
    val photoExtraLarge: Dp = 210.dp

    // Profile picture section padding
    val photoPadding: Dp = 24.dp

    // Edit photo button overlay
    val editButtonSize: Dp = 120.dp
    val editIconContainer: Dp = 50.dp
    val editIconSize: Dp = 56.dp

    // Delete photo button
    val deleteButtonSize: Dp = 40.dp
    val deleteIconSize: Dp = 24.dp

    // Profile fields
    val fieldMinHeight: Dp = 56.dp
    val fieldCornerRadius: Dp = 12.dp
    val fieldInternalPadding: Dp = 20.dp
    val fieldLabelSpacing: Dp = 12.dp

    // Bio field
    val bioMinHeight: Dp = 120.dp
  }

  /** Text field dimensions. */
  object TextField {
    val minHeight: Dp = 56.dp
    val cornerRadius: Dp = 12.dp
    val labelSpacing: Dp = 8.dp
    val supportingTextSpacing: Dp = 4.dp
    val supportingTextPadding: Dp = 12.dp
  }

  /** Loading indicator dimensions. */
  object LoadingIndicator {
    val small: Dp = 24.dp
    val medium: Dp = 40.dp
    val large: Dp = 56.dp
    val strokeWidth: Dp = 4.dp
  }

  /** Minimum touch target size for accessibility. Following Android accessibility guidelines. */
  object TouchTarget {
    val minimum: Dp = 48.dp
  }

  /** Elevation values for shadows and depth. */
  object Elevation {
    val none: Dp = 0.dp
    val extraSmall: Dp = 1.dp
    val small: Dp = 2.dp
    val medium: Dp = 4.dp
    val large: Dp = 8.dp
    val extraLarge: Dp = 16.dp
  }
}
