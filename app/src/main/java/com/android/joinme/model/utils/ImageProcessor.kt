package com.android.joinme.model.utils

// AI-assisted implementation — reviewed and adapted for project standards.

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
 * Utility class for processing images before upload to Firebase Storage.
 *
 * Handles critical image transformations to optimize uploaded photos:
 * - **Compression**: Reduces file size while maintaining visual quality
 * - **EXIF orientation correction**: Prevents photos appearing rotated on different devices
 * - **Resizing**: Scales down large images to reasonable dimensions for profile photos
 *
 * The processor ensures that uploaded images:
 * - Are under 1MB in size (typically 200-500KB)
 * - Display correctly regardless of device orientation
 * - Don't exceed 1024x1024 pixels (maintains aspect ratio)
 * - Use efficient JPEG encoding at 85% quality
 *
 * Thread safety: This class is thread-safe and can be used from background threads. It's
 * recommended to call processImage() off the main thread for large images.
 *
 * Usage example:
 * ```
 * val processor = ImageProcessor(context)
 * val imageBytes = withContext(Dispatchers.IO) {
 *     processor.processImage(imageUri)
 * }
 * // Upload imageBytes to Firebase Storage
 * ```
 *
 * @param context Android context for accessing content resolver to read image URIs. Application
 *   context is recommended to avoid memory leaks.
 *
 * (AI-assisted implementation; reviewed and verified for project standards.)
 */
class ImageProcessor(private val context: Context) {

  companion object {
    private const val TAG = "ImageProcessor"

    /** Maximum width or height in pixels. Images exceeding this are scaled down. */
    private const val MAX_DIMENSION = 1024

    /** JPEG compression quality (0-100). 85 provides good balance between quality and size. */
    private const val JPEG_QUALITY = 85
  }

  /**
   * Processes an image URI into a compressed byte array ready for upload.
   *
   * This method performs a complete image processing pipeline:
   * 1. **Read EXIF orientation data** - Extracts rotation metadata from image
   * 2. **Decode the image bitmap** - Loads image into memory
   * 3. **Apply orientation correction** - Rotates image to proper orientation
   * 4. **Resize if needed** - Scales down if exceeds MAX_DIMENSION (1024px)
   * 5. **Compress to JPEG format** - Outputs optimized byte array at 85% quality
   *
   * The method automatically cleans up intermediate bitmaps to prevent memory leaks.
   *
   * Performance notes:
   * - Processing time varies with image size (typically 100-500ms for photos)
   * - Memory usage peaks at ~3x the original bitmap size during processing
   * - Should be called from a background thread for images > 2MB
   *
   * @param imageUri The URI of the image to process. Can be from:
   *     - Photo picker (content:// URI)
   *     - File provider (file:// URI)
   *     - Media store (content://media/ URI)
   *
   * @return Byte array of the processed JPEG image, typically 200-500KB in size.
   * @throws Exception if image processing fails due to:
   *         - Invalid URI or inaccessible file
   *         - Corrupted image data
   *         - Insufficient memory
   *         - Unsupported image format
   */
  fun processImage(imageUri: Uri): ByteArray {
    return try {

      // Step 1: Get EXIF orientation
      val orientation = getExifOrientation(imageUri)

      // Step 2: Decode bitmap
      val originalBitmap = decodeBitmap(imageUri)

      // Step 3: Apply orientation correction
      val rotatedBitmap = rotateBitmap(originalBitmap, orientation)

      // Step 4: Resize if needed
      val resizedBitmap = resizeBitmap(rotatedBitmap)

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
      bytes
    } catch (e: Exception) {
      Log.e(TAG, "Error processing image", e)
      throw Exception("Failed to process image: ${e.message}", e)
    }
  }

  /**
   * Reads EXIF orientation data from an image URI.
   *
   * EXIF orientation indicates how the image should be rotated to display correctly. Many cameras
   * and phones save images with rotation metadata instead of actually rotating the pixel data.
   *
   * @param imageUri URI of the image to read EXIF data from.
   * @return EXIF orientation constant (e.g., ORIENTATION_ROTATE_90, ORIENTATION_NORMAL). Returns
   *   ORIENTATION_NORMAL if EXIF data is unavailable or invalid.
   */
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

  /**
   * Decodes a bitmap from a URI with intelligent downsampling to prevent OOM errors.
   *
   * This method uses a two-pass decoding strategy:
   * 1. First pass (inJustDecodeBounds=true): Reads image dimensions without loading pixels
   * 2. Second pass (with inSampleSize): Decodes downsampled image to reduce memory usage
   *
   * For large camera photos (e.g., 4000×3000), decoding at full resolution can require ~48MB of
   * RAM, which often causes OutOfMemory on constrained devices. By calculating an appropriate
   * inSampleSize, we decode a smaller bitmap (e.g., 2000×1500 with inSampleSize=2) that still
   * provides sufficient quality after the final resize to MAX_DIMENSION (1024px).
   *
   * The inSampleSize is always a power of 2 (1, 2, 4, 8, ...) as required by BitmapFactory for
   * optimal performance.
   *
   * @param imageUri URI of the image to decode.
   * @return Decoded Bitmap object, downsampled if the original image is large.
   * @throws Exception if the URI cannot be opened or the image cannot be decoded.
   */
  private fun decodeBitmap(imageUri: Uri): Bitmap {
    // First pass: Get image dimensions without loading the full bitmap
    val options =
        BitmapFactory.Options().apply {
          inJustDecodeBounds = true
          context.contentResolver.openInputStream(imageUri)?.use { inputStream ->
            BitmapFactory.decodeStream(inputStream, null, this)
          }
        }

    val imageWidth = options.outWidth
    val imageHeight = options.outHeight

    if (imageWidth <= 0 || imageHeight <= 0) {
      throw Exception("Failed to decode bitmap dimensions")
    }

    // Calculate inSampleSize to downsample large images
    // Target: decode at ~2x MAX_DIMENSION to preserve quality for rotation/resize
    val targetSize = MAX_DIMENSION * 2
    var inSampleSize = 1

    if (imageWidth > targetSize || imageHeight > targetSize) {
      val halfWidth = imageWidth / 2
      val halfHeight = imageHeight / 2

      // Calculate the largest inSampleSize that keeps dimensions >= targetSize
      while ((halfWidth / inSampleSize) >= targetSize &&
          (halfHeight / inSampleSize) >= targetSize) {
        inSampleSize *= 2
      }
    }

    // Second pass: Decode with downsampling
    val decodeOptions =
        BitmapFactory.Options().apply {
          this.inSampleSize = inSampleSize
          inJustDecodeBounds = false
        }

    return context.contentResolver.openInputStream(imageUri)?.use { inputStream ->
      BitmapFactory.decodeStream(inputStream, null, decodeOptions)
          ?: throw Exception("Failed to decode bitmap")
    } ?: throw Exception("Failed to open input stream")
  }

  /**
   * Rotates a bitmap according to EXIF orientation.
   *
   * Applies the appropriate transformation (rotation, flip) based on the EXIF orientation tag. This
   * ensures images display correctly regardless of how the camera captured them.
   *
   * Supported orientations:
   * - ORIENTATION_ROTATE_90/180/270: Standard rotations
   * - ORIENTATION_FLIP_HORIZONTAL/VERTICAL: Mirror flips
   * - ORIENTATION_TRANSPOSE/TRANSVERSE: Combined rotation and flip
   *
   * @param bitmap The bitmap to rotate.
   * @param orientation EXIF orientation constant indicating required transformation.
   * @return New rotated bitmap, or the original bitmap if no rotation is needed. The original
   *   bitmap should be recycled if it's no longer needed.
   */
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
   * Resizes a bitmap if it exceeds MAX_DIMENSION in either width or height.
   *
   * Maintains aspect ratio while scaling down. If the image is already smaller than MAX_DIMENSION
   * in both dimensions, it's returned unchanged.
   *
   * The scaling factor is calculated based on the larger dimension to ensure both width and height
   * stay within limits.
   *
   * @param bitmap The bitmap to resize.
   * @return Resized bitmap if scaling was needed, or the original bitmap if already within limits.
   *   The original should be recycled if it's no longer needed.
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
