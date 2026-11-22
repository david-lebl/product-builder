package mpbuilder.artwork
package wiring

import db.ArtworkDb
import mpbuilder.artwork.sample.ArtworkSampleLayer
import mpbuilder.platform.db.Database
import zio.*

object ArtworkWiring:
  /** Seeds the demo artwork submissions attached to the seeded demo orders. */
  val seedIfEmpty: URIO[Database, Boolean] =
    ArtworkDb.seedIfEmpty(ArtworkSampleLayer.seedArtworks.values.toList.sortBy(_.id.value))
