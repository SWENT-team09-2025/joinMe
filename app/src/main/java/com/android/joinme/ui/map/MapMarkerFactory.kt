package com.android.joinme.ui.map

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt
import com.android.joinme.ui.theme.Dimens
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory

/** Diameter of the circle marker for shared locations */
private const val CIRCLE_MARKER_SIZE = 48

private const val LOW_SATURATION_THRESHOLD = 0.1f
private const val LOW_VALUE_THRESHOLD = 0.1f

private const val BADGE_MARKER_COLOR = "#2196F3"
private const val MAX_NUMBER_ELEMENTS_COUNTER = 99
private const val MAX_NUMBER_ELEMENTS_COUNTER_TEXT = "99+"
private const val BADGE_SIZE = 80

/**
 * Creates a circular marker for shared locations.
 *
 * @param color The color of the circle marker
 * @return A BitmapDescriptor for the circular marker
 */
internal fun createCircleMarker(color: Color): BitmapDescriptor {
  val bitmap = createBitmap(CIRCLE_MARKER_SIZE, CIRCLE_MARKER_SIZE)
  val canvas = Canvas(bitmap)

  // Draw the circle
  val paint = Paint()
  paint.isAntiAlias = true
  paint.color = color.toArgb()
  paint.style = Paint.Style.FILL

  val radius = CIRCLE_MARKER_SIZE / 2f
  canvas.drawCircle(radius, radius, radius - 4f, paint)

  // Draw white border
  val borderPaint = Paint()
  borderPaint.isAntiAlias = true
  borderPaint.color = android.graphics.Color.WHITE
  borderPaint.style = Paint.Style.STROKE
  borderPaint.strokeWidth = 4f
  canvas.drawCircle(radius, radius, radius - 4f, borderPaint)

  return BitmapDescriptorFactory.fromBitmap(bitmap)
}

/**
 * Creates a pin marker with the specified color.
 *
 * For black or very dark colors, creates a custom black pin marker. For other colors, uses the
 * default Google Maps marker with the appropriate hue.
 *
 * @param color The Compose Color to convert to a marker
 * @return A BitmapDescriptor for the marker
 */
internal fun createMarkerForColor(color: Color): BitmapDescriptor {
  val hsv = FloatArray(3)
  android.graphics.Color.colorToHSV(color.toArgb(), hsv)

  // Check if color is black or very dark (low saturation and low value)
  val isBlackish = hsv[1] < LOW_SATURATION_THRESHOLD && hsv[2] < LOW_VALUE_THRESHOLD

  return if (isBlackish) {
    // Create custom black marker (3x size to match default markers)
    val width = Dimens.PinMark.PIN_MARK_WIDTH
    val height = Dimens.PinMark.PIN_MARK_HEIGHT
    val bitmap = createBitmap(width, height)
    val canvas = Canvas(bitmap)

    // Create the pin shape with black paint
    val blackPaint = Paint()
    blackPaint.isAntiAlias = true
    blackPaint.color = android.graphics.Color.BLACK
    blackPaint.style = Paint.Style.FILL

    // Draw the pin circle (top part)
    val circleRadius = width / 2f - 4f
    canvas.drawCircle(width / 2f, circleRadius + 4f, circleRadius, blackPaint)

    // Draw the pin pointer (bottom part)
    val path =
        Path().apply {
          moveTo(width / 2f, height.toFloat())
          lineTo(width / 2f - circleRadius / 2f, circleRadius * 2f)
          lineTo(width / 2f + circleRadius / 2f, circleRadius * 2f)
          close()
        }
    canvas.drawPath(path, blackPaint)

    // Draw white circle in the middle
    val whitePaint = Paint()
    whitePaint.isAntiAlias = true
    whitePaint.color = android.graphics.Color.GRAY
    whitePaint.style = Paint.Style.FILL
    canvas.drawCircle(width / 2f, circleRadius + 4f, circleRadius / 2.5f, whitePaint)

    BitmapDescriptorFactory.fromBitmap(bitmap)
  } else {
    // Use default marker with hue
    BitmapDescriptorFactory.defaultMarker(hsv[0])
  }
}

/**
 * Creates a circular badge marker with a count number displayed in the center.
 *
 * @param count The number to display on the badge
 * @return A BitmapDescriptor for the badge marker
 */
internal fun createBadgeMarker(count: Int): BitmapDescriptor {
  val bitmap = createBitmap(BADGE_SIZE, BADGE_SIZE)
  val canvas = Canvas(bitmap)

  // Draw the background circle
  val backgroundPaint =
      Paint().apply {
        isAntiAlias = true
        color = BADGE_MARKER_COLOR.toColorInt()
        style = Paint.Style.FILL
      }
  val radius = BADGE_SIZE / 2f
  canvas.drawCircle(radius, radius, radius - 3f, backgroundPaint)

  // Draw white border
  val borderPaint =
      Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 6f
      }
  canvas.drawCircle(radius, radius, radius - 3f, borderPaint)

  // Draw the count text
  val textPaint =
      Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.WHITE
        textAlign = Paint.Align.CENTER
        textSize = if (count > MAX_NUMBER_ELEMENTS_COUNTER) 26f else 32f
        isFakeBoldText = true
      }

  val countText =
      if (count > MAX_NUMBER_ELEMENTS_COUNTER) MAX_NUMBER_ELEMENTS_COUNTER_TEXT
      else count.toString()
  val textY = radius - (textPaint.descent() + textPaint.ascent()) / 2
  canvas.drawText(countText, radius, textY, textPaint)

  return BitmapDescriptorFactory.fromBitmap(bitmap)
}
