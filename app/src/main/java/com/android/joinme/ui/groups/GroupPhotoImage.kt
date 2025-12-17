package com.android.joinme.ui.groups

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import coil.compose.SubcomposeAsyncImage
import com.android.joinme.R
import com.android.joinme.ui.theme.Dimens

/** Test tags for GroupPhotoImage component. */
object GroupPhotoImageTestTags {
  const val GROUP_PHOTO = "group_photo_image"
  const val GROUP_PHOTO_PLACEHOLDER = "group_photo_placeholder"
}

/**
 * Composable that displays a group photo with automatic placeholder handling.
 *
 * This component loads and displays a group photo from a URL using Coil's SubcomposeAsyncImage. If
 * the URL is null, empty, or fails to load, it displays the default group placeholder image. The
 * image is clipped to a circular shape by default.
 *
 * Unlike ProfilePhotoImage which uses a generated placeholder, this component uses a drawable
 * resource as the default group image.
 *
 * @param photoUrl The URL of the group photo to display, or null/empty for placeholder
 * @param contentDescription Accessibility description for the image
 * @param modifier Modifier to apply to the component
 * @param size The size of the circular image (default: Dimens.Group.pictureLarge)
 * @param showLoadingIndicator Whether to show a loading spinner while the image loads
 */
@Composable
fun GroupPhotoImage(
    photoUrl: String?,
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: Dp = Dimens.Group.pictureLarge,
    showLoadingIndicator: Boolean = true
) {
  val hasValidUrl = !photoUrl.isNullOrBlank()

  if (hasValidUrl) {
    SubcomposeAsyncImage(
        model = photoUrl,
        contentDescription = contentDescription,
        modifier =
            modifier.size(size).clip(CircleShape).testTag(GroupPhotoImageTestTags.GROUP_PHOTO),
        contentScale = ContentScale.Crop,
        loading = {
          if (showLoadingIndicator) {
            Box(contentAlignment = Alignment.Center) {
              CircularProgressIndicator(
                  modifier = Modifier.size(size / 3), color = MaterialTheme.colorScheme.primary)
            }
          } else {
            DefaultGroupPlaceholder(size = size)
          }
        },
        error = { DefaultGroupPlaceholder(size = size) })
  } else {
    DefaultGroupPlaceholder(modifier = modifier, size = size)
  }
}

/**
 * Default placeholder image for groups.
 *
 * Displays the group_default_picture drawable resource clipped to a circle.
 *
 * @param modifier Modifier to apply to the image
 * @param size The size of the placeholder image
 */
@Composable
private fun DefaultGroupPlaceholder(modifier: Modifier = Modifier, size: Dp) {
  Image(
      painter = painterResource(id = R.drawable.group_default_picture),
      contentDescription = stringResource(R.string.group_picture),
      modifier =
          modifier
              .size(size)
              .clip(CircleShape)
              .testTag(GroupPhotoImageTestTags.GROUP_PHOTO_PLACEHOLDER),
      contentScale = ContentScale.Crop)
}
