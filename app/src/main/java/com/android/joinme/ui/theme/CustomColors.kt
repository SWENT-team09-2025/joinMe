package com.android.joinme.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuItemColors
import androidx.compose.material3.SelectableChipColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

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
        backgroundMenu = primaryLight)

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
                containerColor = onPrimaryDark,
                labelColor = onPrimaryDark,
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
                textColor = surfaceLight,
                leadingIconColor = onSecondaryContainerLight,
                trailingIconColor = onSecondaryContainerLight,
                disabledTextColor = onSecondaryContainerLight,
                disabledLeadingIconColor = onSecondaryContainerLight,
                disabledTrailingIconColor = onSecondaryContainerLight,
            ),

        // DROPDOWN MENU BACKGROUND FOR SEARCH SCREEN
        backgroundMenu = surfaceContainerDark)

internal val LocalCustomColors = staticCompositionLocalOf { lightCustomColors }

/** Extension property on [MaterialTheme] to provide easy access to custom colors. */
val MaterialTheme.customColors: CustomColors
  @Composable @ReadOnlyComposable get() = LocalCustomColors.current
