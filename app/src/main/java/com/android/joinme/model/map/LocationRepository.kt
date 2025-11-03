package com.android.joinme.model.map

interface LocationRepository {
  suspend fun search(query: String): List<Location>
}
