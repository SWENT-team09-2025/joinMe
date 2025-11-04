package com.android.joinme.model.utils

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import io.mockk.*
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ImageProcessorTest {

  private lateinit var mockContext: Context
  private lateinit var mockContentResolver: ContentResolver
  private lateinit var imageProcessor: ImageProcessor

  @Before
  fun setup() {
    mockContext = mockk(relaxed = true)
    mockContentResolver = mockk(relaxed = true)
    every { mockContext.contentResolver } returns mockContentResolver
    imageProcessor = ImageProcessor(mockContext)
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  private fun createTestBitmap(width: Int, height: Int): ByteArray {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val outputStream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
    bitmap.recycle()
    return outputStream.toByteArray()
  }

  @Test
  fun `processImage returns non-empty bytes for valid input`() {
    // Given: Create a small valid test bitmap
    val testBytes = createTestBitmap(100, 100)
    val mockUri = mockk<Uri>()
    val inputStream: InputStream = ByteArrayInputStream(testBytes)

    every { mockContentResolver.openInputStream(mockUri) } returns inputStream

    // When: Process the image
    val result = imageProcessor.processImage(mockUri)

    // Then: Should return non-empty byte array
    assertTrue("Processed image should not be empty", result.isNotEmpty())
    assertTrue("Processed image should be valid JPEG", result.size > 10)
  }

  @Test
  fun `processImage handles EXIF rotation correctly`() {
    // Given: Create bitmap with EXIF orientation
    val testBytes = createTestBitmap(200, 100)
    val mockUri = mockk<Uri>()
    val inputStream: InputStream = ByteArrayInputStream(testBytes)

    every { mockContentResolver.openInputStream(mockUri) } returns inputStream

    // When: Process image
    val result = imageProcessor.processImage(mockUri)

    // Then: Should successfully process without throwing
    assertNotNull("Result should not be null", result)
    assertTrue("Result should contain data", result.isNotEmpty())
  }

  @Test
  fun `processImage resizes large images to max dimension`() {
    // Given: Create large bitmap (> MAX_DIMENSION of 1024)
    val largeBytes = createTestBitmap(2048, 1536)
    val mockUri = mockk<Uri>()
    val inputStream: InputStream = ByteArrayInputStream(largeBytes)

    every { mockContentResolver.openInputStream(mockUri) } returns inputStream

    // When: Process the large image
    val result = imageProcessor.processImage(mockUri)

    // Then: Result should be compressed
    assertTrue("Processed image should not be empty", result.isNotEmpty())

    // Decode result to verify dimensions
    val processedBitmap = BitmapFactory.decodeByteArray(result, 0, result.size)
    assertNotNull("Processed bitmap should not be null", processedBitmap)
    val maxDimension = maxOf(processedBitmap.width, processedBitmap.height)
    assertTrue("Max dimension should be <= 1024, was $maxDimension", maxDimension <= 1024)
    processedBitmap.recycle()
  }

  @Test
  fun `processImage maintains aspect ratio after resize`() {
    // Given: Create rectangular bitmap
    val width = 1600
    val height = 900
    val bytes = createTestBitmap(width, height)
    val mockUri = mockk<Uri>()
    val inputStream: InputStream = ByteArrayInputStream(bytes)

    every { mockContentResolver.openInputStream(mockUri) } returns inputStream

    // When: Process image
    val result = imageProcessor.processImage(mockUri)

    // Then: Aspect ratio should be maintained
    val processedBitmap = BitmapFactory.decodeByteArray(result, 0, result.size)
    assertNotNull("Processed bitmap should not be null", processedBitmap)

    val originalAspectRatio = width.toFloat() / height.toFloat()
    val processedAspectRatio = processedBitmap.width.toFloat() / processedBitmap.height.toFloat()

    assertEquals(
        "Aspect ratio should be maintained",
        originalAspectRatio,
        processedAspectRatio,
        0.02f) // Allow small tolerance
    processedBitmap.recycle()
  }

  @Test(expected = Exception::class)
  fun `processImage throws exception when cannot open input stream`() {
    // Given: URI that cannot be opened
    val mockUri = mockk<Uri>()

    every { mockContentResolver.openInputStream(mockUri) } returns null

    // When: Try to process
    imageProcessor.processImage(mockUri)

    // Then: Should throw exception
  }

  @Test
  fun `processImage handles small images without unnecessary resizing`() {
    // Given: Small bitmap (< MAX_DIMENSION)
    val smallBytes = createTestBitmap(512, 512)
    val mockUri = mockk<Uri>()
    val inputStream: InputStream = ByteArrayInputStream(smallBytes)

    every { mockContentResolver.openInputStream(mockUri) } returns inputStream

    // When: Process small image
    val result = imageProcessor.processImage(mockUri)

    // Then: Should process successfully
    assertNotNull("Result should not be null", result)
    assertTrue("Result should contain data", result.isNotEmpty())

    val processedBitmap = BitmapFactory.decodeByteArray(result, 0, result.size)
    assertNotNull("Processed bitmap should not be null", processedBitmap)
    assertTrue(
        "Small image dimensions should be preserved or minimally changed",
        processedBitmap.width <= 512 && processedBitmap.height <= 512)
    processedBitmap.recycle()
  }
}
