package com.android.joinme.ui.theme

// Adapted and refactored with AI assistance — reviewed and adjusted for project standards.

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
 * Design Principles:
 * - **Consistency**: Use the same spacing values across the app for visual harmony
 * - **Material Design**: Follows Material Design spacing (4dp base unit) and type scale
 * - **Semantic Naming**: Names describe purpose (e.g., `photoLarge`) not value (e.g., `size140`)
 * - **Accessibility**: All dimensions scale properly with system font size settings
 * - **Maintainability**: Update once, apply everywhere
 *
 * Organization:
 * - `Spacing`: Gaps between elements (4dp base unit)
 * - `Padding`: Internal spacing within components
 * - `CornerRadius`: Rounded corners for various components
 * - `BorderWidth`: Standard border thicknesses
 * - `FontSize`: Material Design type scale for typography
 * - `IconSize`: Standard icon dimensions
 * - `Button`: Button-specific dimensions
 * - `Profile`: Profile feature dimensions (photos, fields, etc.)
 * - `TextField`: Text input field dimensions
 * - `LoadingIndicator`: Loading spinner dimensions
 * - `TouchTarget`: Accessibility-compliant touch targets
 * - `Elevation`: Shadow and depth values
 * - `SerieCard`: Dimensions specific to the SerieCard component
 *
 * Usage example:
 * ```
 * import com.android.joinme.ui.theme.Dimens
 *
 * // Screen padding
 * Column(
 *     modifier = Modifier.padding(horizontal = Dimens.Padding.large)
 * ) {
 *     // Title text
 *     Text(
 *         text = "Profile",
 *         fontSize = Dimens.FontSize.headlineMedium,
 *         fontWeight = FontWeight.Bold
 *     )
 *
 *     Spacer(modifier = Modifier.height(Dimens.Spacing.medium))
 *
 *     // Profile photo
 *     ProfilePhotoImage(
 *         photoUrl = profile.photoUrl,
 *         size = Dimens.Profile.photoLarge
 *     )
 * }
 * ```
 *
 * Adding New Dimensions:
 * 1. Determine the appropriate category (Spacing, Padding, Profile, etc.)
 * 2. Use semantic naming that describes purpose
 * 3. Check if similar dimension already exists before adding
 * 4. Document the use case if specific to a feature
 *
 * (Adapted and refactored with AI assistance; reviewed and verified for project standards.)
 */
object Dimens {

  /**
   * Standard spacing values used throughout the app.
   *
   * Based on Material Design spacing guidelines using a 4dp base unit. Use these for gaps between
   * elements (spacers, arrangement spacing).
   *
   * For internal padding within components, see [Padding] instead.
   *
   * Size progression: 4dp -> 8dp -> 16dp -> 24dp -> 32dp -> 48dp
   */
  object Spacing {
    /** 4dp - Minimal spacing, rarely used */
    val extraSmall: Dp = 4.dp

    /** 8dp - Tight spacing between closely related elements */
    val small: Dp = 8.dp

    /** 16dp - Standard spacing between elements */
    val medium: Dp = 16.dp

    /** 24dp - Spacing between sections or groups */
    val large: Dp = 24.dp

    /** 32dp - Large spacing between major sections */
    val extraLarge: Dp = 32.dp

    /** 48dp - Extra large spacing, used sparingly */
    val huge: Dp = 48.dp

    // Specific spacing patterns
    /** 24dp - Standard spacing between form fields */
    val fieldSpacing: Dp = 24.dp

    /** 16dp - Standard spacing between sections */
    val sectionSpacing: Dp = 16.dp

    /** 12dp - Standard spacing between list items */
    val itemSpacing: Dp = 12.dp
  }

  /**
   * Padding values for consistent internal spacing.
   *
   * Use these for internal padding within components (padding inside boxes, cards, etc.). For gaps
   * between elements, see [Spacing] instead.
   */
  object Padding {
    /** 0dp - No padding */
    val none: Dp = 0.dp

    /** 4dp - Minimal internal padding */
    val extraSmall: Dp = 4.dp

    /** 8dp - Tight internal padding */
    val small: Dp = 8.dp

    /** 16dp - Standard internal padding */
    val medium: Dp = 16.dp

    /** 24dp - Large internal padding */
    val large: Dp = 24.dp

    /** 32dp - Extra large internal padding */
    val extraLarge: Dp = 32.dp

    // Screen-level padding
    /** 24dp - Standard horizontal padding for screen edges */
    val screenHorizontal: Dp = 24.dp

    /** 16dp - Standard vertical padding for screen edges */
    val screenVertical: Dp = 16.dp

    // FloatingActionBubbles padding
    /** 6dp - Small bubble padding */
    val smallBubble: Dp = 6.dp

    /** 16dp - Medium bubble padding */
    val mediumBubble: Dp = 16.dp

    /** 32.dp - Large bubble padding */
    val largeBubble: Dp = 32.dp

    /** 80.dp - Extra large bubble padding */
    val extraLargeBubble: Dp = 80.dp
  }

  /**
   * Corner radius values for consistent rounded corners.
   *
   * Use these for buttons, cards, text fields, and other components with rounded corners. Larger
   * radius = more rounded appearance.
   */
  object CornerRadius {
    /** 0dp - Square corners, no rounding */
    val none: Dp = 0.dp

    /** 4dp - Subtle rounding */
    val small: Dp = 4.dp

    /** 8dp - Medium rounding for small buttons */
    val medium: Dp = 8.dp

    /** 12dp - Standard rounding for text fields, cards, buttons */
    val large: Dp = 12.dp

    /** 16dp - Prominent rounding */
    val extraLarge: Dp = 16.dp

    /** 24dp - Pill-shaped buttons and tags */
    val pill: Dp = 24.dp

    /** 999dp - Perfect circles (use with equal width/height) */
    val circle: Dp = 999.dp
  }

  /**
   * Border width values.
   *
   * Standard border thicknesses for outlines, dividers, and strokes.
   */
  object BorderWidth {
    /** 1dp - Standard thin border (most common) */
    val thin: Dp = 1.dp

    /** 2dp - Medium border for emphasis */
    val medium: Dp = 2.dp

    /** 4dp - Thick border for strong emphasis */
    val thick: Dp = 4.dp
  }

  /**
   * Typography font sizes based on Material Design type scale.
   *
   * Use these for all text in the app to maintain consistent typography hierarchy. The names
   * correspond to Material 3 typography roles.
   *
   * Type Scale (largest to smallest):
   * - Display: Largest text, used for hero sections
   * - Headline: Large text for major headings
   * - Title: Medium text for section headings and labels
   * - Body: Standard text for content
   * - Label: Small text for captions and supporting text
   */
  object FontSize {
    // Display sizes (rarely used)
    /** 57sp - Largest display text */
    val displayLarge: TextUnit = 57.sp

    /** 45sp - Medium display text */
    val displayMedium: TextUnit = 45.sp

    /** 36sp - Small display text */
    val displaySmall: TextUnit = 36.sp

    // Headline sizes
    /** 32sp - Largest headline */
    val headlineLarge: TextUnit = 32.sp

    /** 28sp - Medium headline (screen titles) - Most common for titles */
    val headlineMedium: TextUnit = 28.sp

    /** 24sp - Small headline */
    val headlineSmall: TextUnit = 24.sp

    // Title sizes
    /** 22sp - Large title */
    val titleLarge: TextUnit = 22.sp

    /** 18sp - Medium title (field labels) - Most common for labels */
    val titleMedium: TextUnit = 18.sp

    /** 16sp - Small title */
    val titleSmall: TextUnit = 16.sp

    // Body sizes
    /** 16sp - Large body text - Most common for content */
    val bodyLarge: TextUnit = 16.sp

    /** 14sp - Medium body text */
    val bodyMedium: TextUnit = 14.sp

    /** 12sp - Small body text (placeholders) */
    val bodySmall: TextUnit = 12.sp

    // Label sizes
    /** 14sp - Large label */
    val labelLarge: TextUnit = 14.sp

    /** 12sp - Medium label */
    val labelMedium: TextUnit = 12.sp

    /** 11sp - Small label (hints, errors) - Most common for supporting text */
    val labelSmall: TextUnit = 11.sp
  }

  /**
   * Icon sizes for consistent icon dimensions.
   *
   * Use these for all icons to maintain visual consistency. Choose size based on context and
   * importance.
   */
  object IconSize {
    /** 16dp - Small icons (inline with text) */
    val small: Dp = 16.dp

    /** 24dp - Standard icons (buttons, navigation) - Most common */
    val medium: Dp = 24.dp

    /** 32dp - Large icons (prominent actions) */
    val large: Dp = 32.dp

    /** 48dp - Extra large icons (feature graphics) */
    val extraLarge: Dp = 48.dp
  }

  /**
   * Button dimensions.
   *
   * Standard heights and corner radius for buttons throughout the app. Ensures consistent button
   * appearance and meets accessibility standards.
   */
  object Button {
    /** 48dp - Minimum button height (accessibility standard) */
    val minHeight: Dp = 48.dp

    /** 56dp - Standard button height - Most common */
    val standardHeight: Dp = 56.dp

    /** 64dp - Large button height */
    val largeHeight: Dp = 64.dp

    /** 12dp - Standard button corner radius */
    val cornerRadius: Dp = 12.dp
  }

  /**
   * Profile feature dimensions.
   *
   * Dimensions specific to profile screens including photo sizes, edit controls, and form fields.
   * Centralized here to maintain consistency across ViewProfile and EditProfile screens.
   */
  object Profile {
    // Profile photo sizes
    /** 40dp - Small profile photo (list items, comments) */
    val photoSmall: Dp = 40.dp

    /** 80dp - Medium profile photo */
    val photoMedium: Dp = 80.dp

    /** 140dp - Large profile photo (edit screen) */
    val photoLarge: Dp = 140.dp

    /** 210dp - Extra large profile photo (view screen) */
    val photoExtraLarge: Dp = 210.dp

    // Profile picture section
    /** 24dp - Vertical padding around profile photo section */
    val photoPadding: Dp = 24.dp

    // Edit photo controls (overlay on photo in edit screen)
    /** 120dp - Size of edit photo button */
    val editButtonSize: Dp = 120.dp

    /** 50dp - Size of edit icon container */
    val editIconContainer: Dp = 50.dp

    /** 56dp - Size of edit icon itself */
    val editIconSize: Dp = 56.dp

    // Delete photo button (bottom-right corner)
    /** 40dp - Size of delete photo button */
    val deleteButtonSize: Dp = 40.dp

    /** 24dp - Size of delete icon */
    val deleteIconSize: Dp = 24.dp

    // Profile form fields (read-only display)
    /** 56dp - Minimum height for profile fields */
    val fieldMinHeight: Dp = 56.dp

    /** 12dp - Corner radius for profile fields */
    val fieldCornerRadius: Dp = 12.dp

    /** 20dp - Internal padding inside profile fields */
    val fieldInternalPadding: Dp = 20.dp

    /** 12dp - Spacing between field label and field box */
    val fieldLabelSpacing: Dp = 12.dp

    // Bio field (multi-line text)
    /** 120dp - Minimum height for bio field */
    val bioMinHeight: Dp = 120.dp
  }

  /**
   * Text field dimensions.
   *
   * Standard dimensions for text input fields (TextField, OutlinedTextField). Used in forms and
   * editable screens.
   */
  object TextField {
    /** 56dp - Minimum height for text fields */
    val minHeight: Dp = 56.dp

    /** 12dp - Corner radius for text fields */
    val cornerRadius: Dp = 12.dp

    /** 8dp - Spacing between label and text field */
    val labelSpacing: Dp = 8.dp

    /** 4dp - Spacing between field and supporting text */
    val supportingTextSpacing: Dp = 4.dp

    /** 12dp - Horizontal padding for supporting text */
    val supportingTextPadding: Dp = 12.dp
  }

  /**
   * Loading indicator dimensions.
   *
   * Standard sizes for CircularProgressIndicator and other loading states.
   */
  object LoadingIndicator {
    /** 24dp - Small loading indicator */
    val small: Dp = 24.dp

    /** 40dp - Medium loading indicator - Most common */
    val medium: Dp = 40.dp

    /** 56dp - Large loading indicator */
    val large: Dp = 56.dp

    /** 4dp - Stroke width for circular indicators */
    val strokeWidth: Dp = 4.dp
  }

  /**
   * Minimum touch target size for accessibility.
   *
   * Following Android accessibility guidelines, all interactive elements should have a minimum
   * touch target of 48dp in both dimensions.
   *
   * Use this to ensure buttons, icons, and other clickable elements are large enough for
   * comfortable interaction.
   */
  object TouchTarget {
    /** 48dp - Minimum touch target size (WCAG 2.1 Level AAA) */
    val minimum: Dp = 48.dp
  }

  /**
   * Elevation values for shadows and depth.
   *
   * Use these for Material elevation (shadows) on cards, app bars, and FABs. Higher values = higher
   * elevation = stronger shadow.
   *
   * Note: In Material 3, elevation is often replaced by tonal surfaces, but these values are still
   * useful for specific cases.
   */
  object Elevation {
    /** 0dp - No elevation (flat surface) */
    val none: Dp = 0.dp

    /** 1dp - Minimal elevation */
    val extraSmall: Dp = 1.dp

    /** 2dp - Small elevation (cards) */
    val small: Dp = 2.dp

    /** 4dp - Medium elevation */
    val medium: Dp = 4.dp

    /** 8dp - Large elevation (app bar) */
    val large: Dp = 8.dp

    /** 16dp - Extra large elevation (modals) */
    val extraLarge: Dp = 16.dp
  }

  /**
   * SerieCard dimensions.
   *
   * Dimensions specific to the SerieCard component including card height, layer offsets, and
   * stacking effects.
   */
  object SerieCard {
    /** 100dp - Height of the serie card */
    val cardHeight: Dp = 100.dp

    /** 12dp - Corner radius for serie cards */
    val cornerRadius: Dp = 12.dp

    /** 12dp - Bottom padding for card container */
    val bottomPadding: Dp = 12.dp

    /** 6dp - Horizontal padding for second layer */
    val secondLayerHorizontalPadding: Dp = 6.dp

    /** 12dp - Horizontal padding for third layer */
    val thirdLayerHorizontalPadding: Dp = 12.dp

    /** 6dp - Vertical offset for second layer */
    val secondLayerOffset: Dp = 6.dp

    /** 12dp - Vertical offset for third layer */
    val thirdLayerOffset: Dp = 12.dp

    /** 2dp - Border width for layers */
    val layerBorderWidth: Dp = 2.dp

    /** 12dp - Internal padding for card content */
    val contentPadding: Dp = 12.dp

    /** 6dp - Spacing after top row */
    val topRowSpacing: Dp = 6.dp
  }

  object Serie {
    /** 120dp - height for description field */
    val descriptionField: Dp = 120.dp
    /** 180.dp - height for maxParticipants field */
    val maxParticipantsField: Dp = 180.dp
  }
}
