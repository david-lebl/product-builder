package mpbuilder.artwork
package db

import core.*
import kyo.*
import kyo.test.*
import mpbuilder.artwork.sample.ArtworkSampleWiring
import mpbuilder.platform.PostgresTestSupport
import mpbuilder.shared.*

class ArtworkRepositoryContractSpec extends Test[Any]:

  private type Fresh = ArtworkRepository < (Async & Scope & Abort[Throwable])

  private def inMemory: Fresh = ArtworkSampleWiring.repository(seedSampleArtworks = false)
  private def postgres: Fresh = PostgresTestSupport.database(List(ArtworkDb.migrations)).map(ArtworkDb.repository)

  private val seeds = ArtworkSampleWiring.seedArtworks.values.toList.sortBy(_.id.value)

  private def contract(label: String, fresh: => Fresh): Unit =
    label - {
      "sample submissions (with revisions & preflight reports) round-trip" in {
        for
          repo <- fresh
          _    <- Kyo.foreachDiscard(seeds)(repo.save)
          back <- Kyo.foreach(seeds)(s => repo.findById(s.id))
        yield assert(back == seeds.map(Some(_)))
      }

      "find by owner distinguishes draft sessions and order lines" in {
        val draft = seeds.head.copy(id = ArtworkId.unsafe("art-draft-x"), owner = ArtworkOwner.DraftSession(NonEmptyString.unsafe("sess-42")))
        val line0 = seeds.head.copy(id = ArtworkId.unsafe("art-l0"), owner = ArtworkOwner.OrderLineRef(OrderId.unsafe("ord-x"), 0))
        val line1 = seeds.head.copy(id = ArtworkId.unsafe("art-l1"), owner = ArtworkOwner.OrderLineRef(OrderId.unsafe("ord-x"), 1))
        for
          repo  <- fresh
          _     <- Kyo.foreachDiscard(List(draft, line0, line1))(repo.save)
          d     <- repo.findByOwner(ArtworkOwner.DraftSession(NonEmptyString.unsafe("sess-42")))
          l1    <- repo.findByOwner(ArtworkOwner.OrderLineRef(OrderId.unsafe("ord-x"), 1))
          _     <- repo.delete(draft.id)
          dGone <- repo.findByOwner(ArtworkOwner.DraftSession(NonEmptyString.unsafe("sess-42")))
        yield
          assert(d.map(_.id) == List(draft.id))
          assert(l1.map(_.id) == List(line1.id))
          assert(dGone.isEmpty)
      }
    }

  "Artwork repository contract" - {
    contract("in-memory", inMemory)
    contract("postgres", postgres)
  }
