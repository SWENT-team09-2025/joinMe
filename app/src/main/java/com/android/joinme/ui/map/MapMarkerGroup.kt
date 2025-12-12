package com.android.joinme.ui.map

import androidx.compose.ui.graphics.Color
import com.android.joinme.model.event.Event
import com.android.joinme.model.serie.Serie
import com.google.android.gms.maps.model.LatLng

/**
 * Represents an item (event or serie) at a specific location on the map.
 */
sealed class MapItem {
  abstract val id: String
  abstract val title: String
  abstract val color: Color
  abstract val position: LatLng

  data class EventItem(
      val event: Event,
      override val color: Color
  ) : MapItem() {
    override val id = event.eventId
    override val title = event.title
    override val position = event.location?.let { LatLng(it.latitude, it.longitude) } ?: LatLng(0.0, 0.0)
  }

  data class SerieItem(
      val serie: Serie,
      val location: com.android.joinme.model.map.Location,
      override val color: Color
  ) : MapItem() {
    override val id = serie.serieId
    override val title = serie.title
    override val position = LatLng(location.latitude, location.longitude)
  }
}

/**
 * Groups items at the same GPS position.
 */
data class MapMarkerGroup(
    val position: LatLng,
    val items: List<MapItem>
) {
  val isSingle = items.size == 1
  val count = items.size

  // For single items, use the item's color
  val singleItemColor: Color?
    get() = if (isSingle) items.first().color else null
}
