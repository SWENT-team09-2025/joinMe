package com.android.joinme.model.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import androidx.core.graphics.scale
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream

/**
 * Utility class for processing images before upload.
 *
 * Handles:
 * - Image compression to reduce file size
 * - EXIF orientation correction to prevent rotated images
 * - Resizing to reasonable dimensions for profile photos
 *
 * @param context Android context for accessing content resolver
 */
class ImageProcessor(private val context: Context) {

  companion object {
    private const val TAG = "ImageProcessor"
    private const val MAX_DIMENSION = 1024 // Max width/height in pixels
    private const val JPEG_QUALITY = 85 // 0-100, balance between quality and size
  }

  /**
   * Processes an image URI into a compressed byte array ready for upload.
   *
   * Steps:
   * 1. Read EXIF orientation data
   * 2. Decode the image bitmap
   * 3. Apply orientation correction
   * 4. Resize if needed
   * 5. Compress to JPEG format
   *
   * @param imageUri The URI of the image to process
   * @return Byte array of the processed image
   * @throws Exception if image processing fails
   */
  fun processImage(imageUri: Uri): ByteArray {
    return try {
      Log.d(TAG, "Processing image from URI: $imageUri")

      // Step 1: Get EXIF orientation
      val orientation = getExifOrientation(imageUri)
      Log.d(TAG, "EXIF orientation: $orientation")

      // Step 2: Decode bitmap
      val originalBitmap = decodeBitmap(imageUri)
      Log.d(TAG, "Original bitmap size: ${originalBitmap.width}x${originalBitmap.height}")

      // Step 3: Apply orientation correction
      val rotatedBitmap = rotateBitmap(originalBitmap, orientation)

      // Step 4: Resize if needed
      val resizedBitmap = resizeBitmap(rotatedBitmap)
      Log.d(TAG, "Final bitmap size: ${resizedBitmap.width}x${resizedBitmap.height}")

      // Step 5: Compress to byte array
      val outputStream = ByteArrayOutputStream()
      resizedBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream)
      val bytes = outputStream.toByteArray()

      // Clean up bitmaps
      if (rotatedBitmap != originalBitmap) {
        originalBitmap.recycle()
      }
      if (resizedBitmap != rotatedBitmap) {
        rotatedBitmap.recycle()
      }
      resizedBitmap.recycle()

      Log.d(TAG, "Image processed successfully, final size: ${bytes.size} bytes")
      bytes
    } catch (e: Exception) {
      Log.e(TAG, "Error processing image", e)
      throw Exception("Failed to process image: ${e.message}", e)
    }
  }

  /** Reads EXIF orientation data from an image URI. */
  private fun getExifOrientation(imageUri: Uri): Int {
    return try {
      context.contentResolver.openInputStream(imageUri)?.use { inputStream ->
        val exif = ExifInterface(inputStream)
        exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
      } ?: ExifInterface.ORIENTATION_NORMAL
    } catch (e: Exception) {
      Log.w(TAG, "Failed to read EXIF data, using default orientation", e)
      ExifInterface.ORIENTATION_NORMAL
    }
  }

  /** Decodes a bitmap from a URI. */
  private fun decodeBitmap(imageUri: Uri): Bitmap {
    val inputStream =
        context.contentResolver.openInputStream(imageUri)
            ?: throw Exception("Failed to open input stream")

    return inputStream.use {
      BitmapFactory.decodeStream(it) ?: throw Exception("Failed to decode bitmap")
    }
  }

  /** Rotates a bitmap according to EXIF orientation. */
  private fun rotateBitmap(bitmap: Bitmap, orientation: Int): Bitmap {
    val matrix = Matrix()

    when (orientation) {
      ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
      ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
      ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
      ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
      ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
      ExifInterface.ORIENTATION_TRANSPOSE -> {
        matrix.postRotate(90f)
        matrix.postScale(-1f, 1f)
      }
      ExifInterface.ORIENTATION_TRANSVERSE -> {
        matrix.postRotate(270f)
        matrix.postScale(-1f, 1f)
      }
      else -> return bitmap // No rotation needed
    }

    return try {
      Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    } catch (e: Exception) {
      Log.e(TAG, "Error rotating bitmap", e)
      bitmap
    }
  }

  /**
   * Resizes a bitmap if it exceeds MAX_DIMENSION in either width or height. Maintains aspect ratio.
   */
  private fun resizeBitmap(bitmap: Bitmap): Bitmap {
    val width = bitmap.width
    val height = bitmap.height

    // Check if resize is needed
    if (width <= MAX_DIMENSION && height <= MAX_DIMENSION) {
      return bitmap
    }

    // Calculate scaling factor
    val scale =
        if (width > height) {
          MAX_DIMENSION.toFloat() / width
        } else {
          MAX_DIMENSION.toFloat() / height
        }

    val newWidth = (width * scale).toInt()
    val newHeight = (height * scale).toInt()

    return try {
      bitmap.scale(newWidth, newHeight)
    } catch (e: Exception) {
      Log.e(TAG, "Error resizing bitmap", e)
      bitmap
    }
  }
}
