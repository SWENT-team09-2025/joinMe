package com.android.joinme.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Data class for custom semantic colors.
 */
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
    val onSocialContainer: Color
)

/**
 * Custom light color palette.
 */
val lightCustomColors = CustomColors(
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
    onSocialContainer = onSocialContainerLight
)

/**
 * Custom dark color palette.
 */
val darkCustomColors = CustomColors(
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
    onSocialContainer = onSocialContainerDark
)

internal val LocalCustomColors = staticCompositionLocalOf { lightCustomColors }

/**
 * Extension property on [MaterialTheme] to provide easy access to custom colors.
 */
val MaterialTheme.customColors: CustomColors
    @Composable
    @ReadOnlyComposable
    get() = LocalCustomColors.current
