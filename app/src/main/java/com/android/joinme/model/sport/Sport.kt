package com.android.joinme.model.sport

/** Represents a sport category */
data class Sport(val id: String, val name: String)

/** Immutable list of all available sports */
object Sports {
  val ALL: List<Sport> =
      listOf(
          Sport("basket", "Basket"),
          Sport("football", "Football"),
          Sport("tennis", "Tennis"),
          Sport("running", "Running"))
}
