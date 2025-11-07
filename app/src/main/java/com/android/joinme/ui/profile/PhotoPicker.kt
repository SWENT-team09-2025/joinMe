package com.android.joinme.ui.profile

// AI-assisted implementation — reviewed and adapted for project standards.

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * Composable that provides a photo picker launcher for selecting images.
 *
 * Uses the Android Photo Picker (PickVisualMedia) which provides a modern, system-integrated UI for
 * selecting photos. Falls back gracefully on older devices.
 *
 * The picker automatically handles:
 * - Permission requests (READ_MEDIA_IMAGES on Android 13+, READ_EXTERNAL_STORAGE on older versions)
 * - User-friendly photo selection UI
 * - Temporary URI access for the selected photo
 *
 * Note: The returned URI is temporary and should be processed/uploaded immediately. It may become
 * invalid after the app is closed or after some time.
 *
 * Usage example:
 * ```
 * val photoPicker = rememberPhotoPickerLauncher(
 *     onPhotoPicked = { uri ->
 *         // Handle the selected photo URI
 *         viewModel.uploadProfilePhoto(context, uri)
 *     },
 *     onError = { error ->
 *         // Handle errors (e.g., show toast)
 *         Toast.makeText(context, error, Toast.LENGTH_LONG).show()
 *     }
 * )
 *
 * // Later, to launch the picker:
 * Button(onClick = { photoPicker.launch() }) {
 *     Text("Pick Photo")
 * }
 * ```
 *
 * @param onPhotoPicked Callback invoked when user successfully selects a photo. Receives the URI of
 *   the selected image.
 * @param onError Optional callback invoked if an error occurs (e.g., picker unavailable). Default
 *   is an empty lambda (no-op).
 * @return PhotoPickerLauncher object with launch() method to trigger the picker.
 *
 * (AI-assisted implementation; reviewed and verified for project standards.)
 */
@Composable
fun rememberPhotoPickerLauncher(
    onPhotoPicked: (Uri) -> Unit,
    onError: (String) -> Unit = {}
): PhotoPickerLauncher {

  // Set up the photo picker launcher using Android's PickVisualMedia contract
  val launcher =
      rememberLauncherForActivityResult(contract = ActivityResultContracts.PickVisualMedia()) {
          uri: Uri? ->
        if (uri != null) {
          onPhotoPicked(uri)
        } else {
          // User cancelled or no photo was selected
          // Not necessarily an error, so we don't call onError
        }
      }

  return remember {
    PhotoPickerLauncher(
        launch = {
          try {
            // Launch the photo picker requesting only images (no videos/documents)
            launcher.launch(
                PickVisualMediaRequest(
                    mediaType = ActivityResultContracts.PickVisualMedia.ImageOnly))
          } catch (e: Exception) {
            // Handle cases where photo picker is unavailable (e.g., very old devices)
            onError("Failed to open photo picker: ${e.message}")
          }
        })
  }
}

/**
 * Wrapper class for the photo picker launcher.
 *
 * Provides a clean, testable interface for launching the photo picker from anywhere in the UI. The
 * launcher is remembered across recompositions to maintain stable identity.
 *
 * @property launch Function to trigger the photo picker UI. Call this from button clicks or other
 *   user interactions to open the system photo picker.
 */
data class PhotoPickerLauncher(val launch: () -> Unit)
