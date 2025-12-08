package com.android.joinme.ui.chat

// Implemented with help of Claude AI

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.android.joinme.R
import java.io.File
import kotlinx.coroutines.launch

/**
 * Untestable launcher code extracted from ChatScreen.
 *
 * This file contains ActivityResultLauncher callbacks that cannot be tested in unit or
 * instrumentation tests because:
 * - ActivityResultLauncher callbacks cannot be triggered programmatically
 * - They require real system dialogs (gallery picker, camera, permission prompts)
 * - They are instantiated by the Android framework
 *
 * The business logic these launchers invoke (e.g., ChatViewModel.uploadAndSendImage) IS fully
 * tested in ChatViewModelTest.kt.
 *
 * This file is excluded from SonarCloud coverage analysis.
 */

/**
 * Creates a temporary file URI for camera image capture.
 *
 * @param context Android context
 * @return URI pointing to a temporary file in the app's cache directory
 */
fun createImageUri(context: Context): Uri {
  val timeStamp = System.currentTimeMillis()
  val imageFile = File(context.cacheDir, "camera_image_${timeStamp}.jpg")
  return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", imageFile)
}

/**
 * Creates an image picker launcher for selecting images from gallery.
 *
 * @param viewModel ChatViewModel for uploading images
 * @param currentUserName Name of the current user
 * @param onDismiss Callback to dismiss the attachment menu
 * @return ActivityResultLauncher for picking images
 */
@Composable
fun rememberImagePickerLauncher(
    viewModel: ChatViewModel,
    currentUserName: String,
    onDismiss: () -> Unit
): ManagedActivityResultLauncher<String, Uri?> {
  val context = LocalContext.current
  val imageSentSuccess = context.getString(R.string.image_sent_success)
  val coroutineScope = rememberCoroutineScope()

  return rememberLauncherForActivityResult(
      contract = ActivityResultContracts.GetContent(),
      onResult = { uri ->
        if (uri != null) {
          viewModel.uploadAndSendImage(
              context = context,
              imageUri = uri,
              senderName = currentUserName,
              onSuccess = {
                coroutineScope.launch {
                  Toast.makeText(context, imageSentSuccess, Toast.LENGTH_SHORT).show()
                }
              },
              onError = { error ->
                android.util.Log.e("ChatScreen", "Upload ERROR callback triggered: $error")
                coroutineScope.launch { Toast.makeText(context, error, Toast.LENGTH_LONG).show() }
              })
          onDismiss()
        } else {
          android.util.Log.w("ChatScreen", "Image picker returned null URI")
        }
      })
}

/**
 * Creates a camera launcher for capturing photos.
 *
 * @param viewModel ChatViewModel for uploading images
 * @param currentUserName Name of the current user
 * @param cameraImageUri URI of the image to be captured (must be set before launching)
 * @param onDismiss Callback to dismiss the attachment menu
 * @return ActivityResultLauncher for taking pictures
 */
@Composable
fun rememberCameraLauncher(
    viewModel: ChatViewModel,
    currentUserName: String,
    cameraImageUri: () -> Uri?,
    onDismiss: () -> Unit
): ManagedActivityResultLauncher<Uri, Boolean> {
  val context = LocalContext.current
  val imageSentSuccess = context.getString(R.string.image_sent_success)
  val coroutineScope = rememberCoroutineScope()

  return rememberLauncherForActivityResult(
      contract = ActivityResultContracts.TakePicture(),
      onResult = { success ->
        if (success && cameraImageUri() != null) {
          viewModel.uploadAndSendImage(
              context = context,
              imageUri = cameraImageUri()!!,
              senderName = currentUserName,
              onSuccess = {
                coroutineScope.launch {
                  Toast.makeText(context, imageSentSuccess, Toast.LENGTH_SHORT).show()
                }
              },
              onError = { error ->
                android.util.Log.e("ChatScreen", "Camera upload ERROR: $error")
                coroutineScope.launch { Toast.makeText(context, error, Toast.LENGTH_LONG).show() }
              })
          onDismiss()
        } else {
          android.util.Log.w("ChatScreen", "Camera capture failed or was cancelled")
        }
      })
}

/**
 * Creates a camera permission launcher.
 *
 * @param cameraLauncher The camera launcher to invoke when permission is granted
 * @param onCameraImageUriSet Callback to set the camera image URI before launching camera
 * @return ActivityResultLauncher for requesting camera permission
 */
@Composable
fun rememberCameraPermissionLauncher(
    cameraLauncher: ManagedActivityResultLauncher<Uri, Boolean>,
    onCameraImageUriSet: () -> Uri
): ManagedActivityResultLauncher<String, Boolean> {
  val context = LocalContext.current

  return rememberLauncherForActivityResult(
      contract = ActivityResultContracts.RequestPermission(),
      onResult = { isGranted ->
        if (isGranted) {
          // Permission granted, launch camera
          val uri = onCameraImageUriSet()
          cameraLauncher.launch(uri)
        } else {
          // Permission denied
          Toast.makeText(context, "Camera permission is required to take photos", Toast.LENGTH_LONG)
              .show()
        }
      })
}

/**
 * Creates a location permissions launcher for accessing user location.
 *
 * @param onLocationRetrieved Callback invoked when location is successfully retrieved
 * @return ActivityResultLauncher for requesting multiple location permissions
 */
@Composable
fun rememberLocationPermissionsLauncher(
    onLocationRetrieved: (com.android.joinme.model.map.UserLocation) -> Unit
): ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>> {
  val context = LocalContext.current
  val locationPermissionRequired = context.getString(R.string.location_permission_required)
  val coroutineScope = rememberCoroutineScope()

  return rememberLauncherForActivityResult(
      contract = ActivityResultContracts.RequestMultiplePermissions(),
      onResult = { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
          // All location permissions granted, fetch location
          coroutineScope.launch {
            try {
              val locationService =
                  com.android.joinme.ui.map.userLocation.LocationServiceImpl(context)
              val location = locationService.getCurrentLocation()
              if (location != null) {
                onLocationRetrieved(location)
              } else {
                Toast.makeText(
                        context,
                        context.getString(R.string.failed_to_send_location, "Location unavailable"),
                        Toast.LENGTH_SHORT)
                    .show()
              }
            } catch (e: Exception) {
              Toast.makeText(
                      context,
                      context.getString(R.string.failed_to_send_location, e.message),
                      Toast.LENGTH_SHORT)
                  .show()
            }
          }
        } else {
          // Permissions denied
          Toast.makeText(context, locationPermissionRequired, Toast.LENGTH_LONG).show()
        }
      })
}
