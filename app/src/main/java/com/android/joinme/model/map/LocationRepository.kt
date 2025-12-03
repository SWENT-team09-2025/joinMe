package com.android.joinme.model.map

fun interface LocationRepository {
  suspend fun search(query: String): List<Location>
}
