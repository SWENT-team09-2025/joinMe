package com.android.joinme.ui.profile

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
 * Usage:
 * ```
 * val photoPicker = rememberPhotoPickerLauncher(
 *     onPhotoPicked = { uri ->
 *         // Handle the selected photo URI
 *         viewModel.uploadProfilePhoto(context, uri)
 *     },
 *     onError = { error ->
 *         // Handle errors (e.g., show toast)
 *     }
 * )
 *
 * // Later, to launch the picker:
 * Button(onClick = { photoPicker.launch() }) {
 *     Text("Pick Photo")
 * }
 * ```
 *
 * @param onPhotoPicked Callback invoked when user successfully selects a photo
 * @param onError Callback invoked if an error occurs (e.g., no permission, picker unavailable)
 * @return PhotoPickerLauncher object with launch() method to trigger the picker
 */
@Composable
fun rememberPhotoPickerLauncher(
    onPhotoPicked: (Uri) -> Unit,
    onError: (String) -> Unit = {}
): PhotoPickerLauncher {

  // Set up the photo picker launcher
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
            // Launch the photo picker requesting only images
            launcher.launch(
                PickVisualMediaRequest(
                    mediaType = ActivityResultContracts.PickVisualMedia.ImageOnly))
          } catch (e: Exception) {
            onError("Failed to open photo picker: ${e.message}")
          }
        })
  }
}

/**
 * Wrapper class for the photo picker launcher.
 *
 * Provides a clean interface for launching the photo picker from anywhere in the UI.
 *
 * @property launch Function to trigger the photo picker
 */
data class PhotoPickerLauncher(val launch: () -> Unit)
