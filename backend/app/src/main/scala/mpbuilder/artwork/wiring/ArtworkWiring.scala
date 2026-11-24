package mpbuilder.artwork
package wiring

import db.ArtworkDb
import kyo.*
import mpbuilder.artwork.sample.ArtworkSampleWiring
import mpbuilder.platform.db.Database

object ArtworkWiring:
  /** Seeds the demo artwork submissions attached to the seeded demo orders. */
  def seedIfEmpty(db: Database): Boolean < Async =
    ArtworkDb.seedIfEmpty(db, ArtworkSampleWiring.seedArtworks.values.toList.sortBy(_.id.value))
