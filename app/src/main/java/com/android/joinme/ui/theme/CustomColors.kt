package com.android.joinme.ui.theme

import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuItemColors
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SelectableChipColors
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.android.joinme.model.event.EventType

/** Data class for custom semantic colors. */
data class CustomColors(
    val activity: Color,
    val onActivity: Color,
    val activityContainer: Color,
    val onActivityContainer: Color,
    val sports: Color,
    val onSports: Color,
    val sportsContainer: Color,
    val onSportsContainer: Color,
    val social: Color,
    val onSocial: Color,
    val socialContainer: Color,
    val onSocialContainer: Color,
    val serieContainer: Color,
    val onSerieContainer: Color,
    val seriePinMark: Color,
    val chatDefault: Color,
    val onChatDefault: Color,
    val containerColor: Color,
    val selectedIconColor: Color,
    val selectedTextColor: Color,
    val selectedIndicatorColor: Color,
    val unselectedIconColor: Color,
    val unselectedTextColor: Color,
    val disabledIconColor: Color,
    val disabledTextColor: Color,
    val filterChip: SelectableChipColors,
    val dropdownMenu: MenuItemColors,
    val backgroundMenu: Color,
    val buttonContainerColor: Color,
    val buttonContentColor: Color,
    val outlinedTextFieldDisabledTextColor: Color,
    val outlinedTextFieldDisabledBorderColor: Color,
    val outlinedTextFieldDisabledLabelColor: Color,
    val outlinedTextFieldDisabledPlaceholderColor: Color,
    val outlinedTextFieldDisabledLeadingIconColor: Color,
    val outlinedTextFieldDisabledTrailingIconColor: Color,
    val deleteButton: Color,
    val editIcon: Color,
    val scrimOverlay: Color
)

/** Custom light color palette. */
val lightCustomColors =
    CustomColors(
        // GROUP/EVENT TYPE: ACTIVITY, SPORTS, SOCIAL
        activity = activityLight,
        onActivity = onActivityLight,
        activityContainer = activityContainerLight,
        onActivityContainer = onActivityContainerLight,
        sports = sportsLight,
        onSports = onSportsLight,
        sportsContainer = sportsContainerLight,
        onSportsContainer = onSportsContainerLight,
        social = socialLight,
        onSocial = onSocialLight,
        socialContainer = socialContainerLight,
        onSocialContainer = onSocialContainerLight,

        // SERIE CARD COLORS
        serieContainer = serieContainerLight,
        onSerieContainer = onSerieContainerLight,

        // SERIE PIN MARKS COLORS
        seriePinMark = seriePinMark,

        // CHAT DEFAULT COLORS
        chatDefault = chatDefaultLight,
        onChatDefault = onChatDefaultLight,

        // BOTTOM NAVIGATION BAR COLORS
        containerColor = primaryLight,
        selectedIconColor = onPrimaryLight,
        selectedTextColor = onPrimaryLight,
        selectedIndicatorColor = primaryContainerLight,
        unselectedIconColor = onPrimaryContainerLight.copy(0.6f),
        unselectedTextColor = onPrimaryContainerLight.copy(0.6f),
        disabledIconColor = onPrimaryLight.copy(0.6f),
        disabledTextColor = onPrimaryLight.copy(0.6f),

        // FILTER CHIP COLORS FOR SEARCH SCREEN
        filterChip =
            SelectableChipColors(
                containerColor = surfaceLight,
                labelColor = onSurfaceVariantLight,
                disabledContainerColor = surfaceLight,
                disabledLabelColor = secondaryLight,
                leadingIconColor = secondaryLight,
                trailingIconColor = secondaryLight,
                disabledLeadingIconColor = secondaryLight,
                disabledTrailingIconColor = secondaryLight,
                selectedContainerColor = primaryLight,
                disabledSelectedContainerColor = surfaceLight,
                selectedLabelColor = onPrimaryLight,
                selectedLeadingIconColor = secondaryLight,
                selectedTrailingIconColor = secondaryLight,
            ),

        // DROPDOWN MENU COLORS DOR SEARCH SCREEN
        dropdownMenu =
            MenuItemColors(
                textColor = surfaceLight,
                leadingIconColor = onSecondaryContainerLight,
                trailingIconColor = onSecondaryContainerLight,
                disabledTextColor = onSecondaryContainerLight,
                disabledLeadingIconColor = onSecondaryContainerLight,
                disabledTrailingIconColor = onSecondaryContainerLight,
            ),
        backgroundMenu = primaryLight,
        buttonContainerColor = primaryLight,
        buttonContentColor = onPrimaryLight,
        outlinedTextFieldDisabledTextColor = onSurfaceLight,
        outlinedTextFieldDisabledBorderColor = outlineLight,
        outlinedTextFieldDisabledLabelColor = onSurfaceVariantLight,
        outlinedTextFieldDisabledPlaceholderColor = onSurfaceVariantLight,
        outlinedTextFieldDisabledLeadingIconColor = onSurfaceVariantLight,
        outlinedTextFieldDisabledTrailingIconColor = onSurfaceVariantLight,
        deleteButton = errorLight,

        // Edit icon color in group/profile pictures
        editIcon = onPrimaryLight,

        // Scrim overlay color for ShowMultiplesOptionsBubbles
        scrimOverlay = scrimLight)

/** Custom dark color palette. */
val darkCustomColors =
    CustomColors(
        // GROUP/EVENT TYPE: ACTIVITY, SPORTS, SOCIAL
        activity = activityDark,
        onActivity = onActivityDark,
        activityContainer = activityContainerDark,
        onActivityContainer = onActivityContainerDark,
        sports = sportsDark,
        onSports = onSportsDark,
        sportsContainer = sportsContainerDark,
        onSportsContainer = onSportsContainerDark,
        social = socialDark,
        onSocial = onSocialDark,
        socialContainer = socialContainerDark,
        onSocialContainer = onSocialContainerDark,

        // SERIE CARD COLORS
        serieContainer = serieContainerDark,
        onSerieContainer = onSerieContainerDark,

        // SERIE PIN MARKS COLORS
        seriePinMark = seriePinMark,

        // CHAT DEFAULT COLORS
        chatDefault = chatDefaultDark,
        onChatDefault = onChatDefaultDark,

        // BOTTOM NAVIGATION BAR COLORS
        containerColor = surfaceContainerDark,
        selectedIconColor = inverseSurfaceDark,
        selectedTextColor = inverseSurfaceDark,
        selectedIndicatorColor = surfaceContainerHighDark,
        unselectedIconColor = inverseSurfaceDark.copy(alpha = 0.6f),
        unselectedTextColor = inverseSurfaceDark.copy(alpha = 0.6f),
        disabledIconColor = inverseSurfaceDark.copy(alpha = 0.6f),
        disabledTextColor = inverseSurfaceDark.copy(alpha = 0.6f),

        // FILTER CHIP COLORS FOR SEARCH SCREEN
        // NOT IMPLEMENTED YET
        filterChip =
            SelectableChipColors(
                containerColor = surfaceContainerHighDark,
                labelColor = onSurfaceVariantDark,
                disabledContainerColor = surfaceContainerHighDark,
                disabledLabelColor = surfaceDark,
                leadingIconColor = surfaceDark,
                trailingIconColor = surfaceDark,
                disabledLeadingIconColor = surfaceDark,
                disabledTrailingIconColor = surfaceDark,
                selectedContainerColor = primaryDark,
                disabledSelectedContainerColor = surfaceContainerHighDark,
                selectedLabelColor = onPrimaryDark,
                selectedLeadingIconColor = surfaceDark,
                selectedTrailingIconColor = surfaceDark,
            ),

        // DROPDOWN MENU COLORS FOR SEARCH SCREEN
        dropdownMenu =
            MenuItemColors(
                textColor = onSurfaceDark,
                leadingIconColor = onSecondaryContainerDark,
                trailingIconColor = onSecondaryContainerDark,
                disabledTextColor = onSecondaryContainerDark,
                disabledLeadingIconColor = onSecondaryContainerDark,
                disabledTrailingIconColor = onSecondaryContainerDark,
            ),

        // DROPDOWN MENU BACKGROUND FOR SEARCH SCREEN
        backgroundMenu = surfaceContainerDark,
        buttonContainerColor = primaryDark,
        buttonContentColor = onPrimaryDark,
        outlinedTextFieldDisabledTextColor = onSurfaceDark,
        outlinedTextFieldDisabledBorderColor = outlineDark,
        outlinedTextFieldDisabledLabelColor = onSurfaceVariantDark,
        outlinedTextFieldDisabledPlaceholderColor = onSurfaceVariantDark,
        outlinedTextFieldDisabledLeadingIconColor = onSurfaceVariantDark,
        outlinedTextFieldDisabledTrailingIconColor = onSurfaceVariantDark,
        deleteButton = errorContainerDark,

        // Edit icon color in group/profile pictures
        editIcon = primaryDark,
        // Scrim overlay color for ShowMultiplesOptionsBubbles
        scrimOverlay = scrimDark)

internal val LocalCustomColors = staticCompositionLocalOf { lightCustomColors }

/** Extension property on [MaterialTheme] to provide easy access to custom colors. */
val MaterialTheme.customColors: CustomColors
  @Composable @ReadOnlyComposable get() = LocalCustomColors.current

/**
 * Returns customized colors for Button components.
 *
 * Provides a consistent color scheme for buttons.
 *
 * @return ButtonColors configured with custom colors.
 */
@Composable
fun CustomColors.buttonColors(): ButtonColors {
  return ButtonDefaults.buttonColors(
      containerColor = buttonContainerColor, contentColor = buttonContentColor)
}

/**
 * Returns customized colors for Button components based on the given [EventType].
 *
 * Provides a consistent color scheme for buttons corresponding to different event types.
 *
 * @param type The [EventType] to determine the button colors.
 * @return ButtonColors configured with custom colors for the specified event type.
 */
@Composable
fun CustomColors.buttonColorsForEventType(type: EventType): ButtonColors {
  return ButtonDefaults.buttonColors(
      containerColor =
          when (type) {
            EventType.ACTIVITY -> activity
            EventType.SPORTS -> sports
            EventType.SOCIAL -> social
          },
      contentColor =
          when (type) {
            EventType.ACTIVITY -> onActivity
            EventType.SPORTS -> onSports
            EventType.SOCIAL -> onSocial
          })
}

/**
 * Returns customized colors for OutlinedTextField components.
 *
 * Provides a consistent color scheme for disabled states of outlined text fields, including text,
 * borders, labels, placeholders, and icons.
 *
 * @return TextFieldColors configured with custom disabled state colors
 */
@Composable
fun CustomColors.outlinedTextField(): TextFieldColors {
  return OutlinedTextFieldDefaults.colors(
      disabledTextColor = outlinedTextFieldDisabledTextColor,
      disabledBorderColor = outlinedTextFieldDisabledBorderColor,
      disabledLabelColor = outlinedTextFieldDisabledLabelColor,
      disabledPlaceholderColor = outlinedTextFieldDisabledPlaceholderColor,
      disabledLeadingIconColor = outlinedTextFieldDisabledLeadingIconColor,
      disabledTrailingIconColor = outlinedTextFieldDisabledTrailingIconColor)
}
