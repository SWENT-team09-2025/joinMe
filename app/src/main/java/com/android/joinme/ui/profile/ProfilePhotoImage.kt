package com.android.joinme.ui.profile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.AccountCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import com.android.joinme.ui.theme.JoinMeColor

/**
 * Test tags for ProfilePhotoImage components to enable UI testing. These tags allow tests to
 * distinguish between different photo states.
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
 * - Loading remote images from URLs (photoUrl)
 * - Showing a loading indicator while the image loads
 * - Falling back to default avatar icon if no photo or on error
 * - Caching for offline support and performance
 * - Proper content scaling to fill the available space
 *
 * @param photoUrl The URL of the profile photo, or null to show default avatar
 * @param contentDescription Accessibility description for the image
 * @param modifier Modifier for styling and positioning
 * @param size The size of the profile photo
 * @param shape The shape to clip the image to (default: CircleShape for most profile photos)
 * @param showLoadingIndicator Whether to show a loading spinner while image loads
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
                        modifier = Modifier.size(size / 3), color = JoinMeColor)
                  }
            }
          },
          error = {
            // Show default avatar on error
            DefaultProfileAvatar(contentDescription = contentDescription, size = size)
          })
    } else {
      // No photo URL or empty string, show default avatar
      DefaultProfileAvatar(contentDescription = contentDescription, size = size)
    }
  }
}

/** The default profile avatar icon shown when no photo is available. */
@Composable
private fun DefaultProfileAvatar(contentDescription: String, size: Dp) {
  Icon(
      imageVector = Icons.Sharp.AccountCircle,
      contentDescription = contentDescription,
      modifier = Modifier.size(size).testTag(ProfilePhotoImageTestTags.DEFAULT_AVATAR),
      tint = JoinMeColor)
}
