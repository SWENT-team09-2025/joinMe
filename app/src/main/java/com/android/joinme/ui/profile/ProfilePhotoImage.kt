package com.android.joinme.ui.profile

// AI-assisted implementation — reviewed and adapted for project standards.

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.AccountCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest

/**
 * Test tags for ProfilePhotoImage components to enable UI testing.
 *
 * These tags allow tests to distinguish between different photo states (loading, error, success).
 */
object ProfilePhotoImageTestTags {
  const val REMOTE_IMAGE = "profilePhotoRemoteImage"
  const val DEFAULT_AVATAR = "profilePhotoDefaultAvatar"
  const val LOADING_INDICATOR = "profilePhotoLoadingIndicator"
}

/**
 * Displays a profile photo using Coil with proper loading and error states.
 *
 * This composable handles:
 * - Loading remote images from URLs with automatic caching
 * - Showing a loading indicator while the image loads (optional)
 * - Falling back to default avatar icon if no photo URL provided or on error
 * - Proper content scaling to fill the available space without distortion
 * - Smooth crossfade transition when image loads
 *
 * The component uses Coil's SubcomposeAsyncImage which provides:
 * - Automatic memory and disk caching for performance
 * - Lifecycle-aware loading (pauses when app is backgrounded)
 * - Efficient bitmap pooling
 *
 * Usage examples:
 * ```
 * // Large profile photo with loading indicator
 * ProfilePhotoImage(
 *     photoUrl = profile.photoUrl,
 *     contentDescription = "Profile Picture",
 *     size = 210.dp
 * )
 *
 * // Small avatar without loading indicator
 * ProfilePhotoImage(
 *     photoUrl = user.photoUrl,
 *     contentDescription = "User Avatar",
 *     size = 40.dp,
 *     showLoadingIndicator = false
 * )
 * ```
 *
 * @param photoUrl The URL of the profile photo, or null to show default avatar. Empty strings are
 *   treated as null.
 * @param contentDescription Accessibility description for the image. Should describe what the photo
 *   shows (e.g., "User's profile picture").
 * @param modifier Modifier for styling and positioning the container.
 * @param size The size (width and height) of the profile photo. Default is 140.dp, suitable for
 *   medium-sized avatars.
 * @param shape The shape to clip the image to. Default is CircleShape for typical profile photos.
 *   Can be changed to RoundedCornerShape or other shapes as needed.
 * @param showLoadingIndicator Whether to show a loading spinner while image loads. Default is true.
 *   Set to false for small avatars or when a custom loading indicator is used externally.
 *
 * (AI-assisted implementation; reviewed and verified for project standards.)
 */
@Composable
fun ProfilePhotoImage(
    photoUrl: String?,
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: Dp = 140.dp,
    shape: Shape = androidx.compose.foundation.shape.CircleShape,
    showLoadingIndicator: Boolean = true
) {
  Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
    if (photoUrl != null && photoUrl.isNotEmpty()) {
      // Load remote image with Coil
      SubcomposeAsyncImage(
          model =
              ImageRequest.Builder(LocalContext.current)
                  .data(photoUrl)
                  .crossfade(true) // Smooth transition when image loads
                  .build(),
          contentDescription = contentDescription,
          modifier =
              Modifier.size(size).clip(shape).testTag(ProfilePhotoImageTestTags.REMOTE_IMAGE),
          contentScale = ContentScale.Crop, // Fill the space, cropping if needed
          loading = {
            // Show loading state
            if (showLoadingIndicator) {
              Box(
                  modifier =
                      Modifier.size(size).testTag(ProfilePhotoImageTestTags.LOADING_INDICATOR),
                  contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(size / 3),
                        color = MaterialTheme.colorScheme.primary)
                  }
            }
          },
          error = {
            // Show default avatar on error (e.g., invalid URL, network error)
            DefaultProfileAvatar(contentDescription = contentDescription, size = size)
          })
    } else {
      // No photo URL or empty string, show default avatar
      DefaultProfileAvatar(contentDescription = contentDescription, size = size)
    }
  }
}

/**
 * The default profile avatar icon shown when no photo is available.
 *
 * Uses Material's AccountCircle icon as a fallback avatar. The icon color adapts to the app theme
 * (primary color) for consistency.
 *
 * @param contentDescription Accessibility description passed through from parent.
 * @param size The size of the icon, matching the parent container size.
 */
@Composable
private fun DefaultProfileAvatar(contentDescription: String, size: Dp) {
  Icon(
      imageVector = Icons.Sharp.AccountCircle,
      contentDescription = contentDescription,
      modifier = Modifier.size(size).testTag(ProfilePhotoImageTestTags.DEFAULT_AVATAR),
      tint = MaterialTheme.colorScheme.primary)
}
