package mpbuilder.artwork
package db

import core.*
import mpbuilder.artwork.sample.ArtworkSampleLayer
import mpbuilder.platform.PostgresTestSupport
import mpbuilder.platform.db.Database
import mpbuilder.shared.*
import zio.*
import zio.test.*

object ArtworkRepositoryContractSpec extends ZIOSpecDefault:

  val inMemory: ULayer[ArtworkRepository] = ArtworkSampleLayer.repositoryLayer(seedSampleArtworks = false)

  val postgres: URLayer[Database, ArtworkRepository] =
    ZLayer.fromZIO(ZIO.serviceWithZIO[Database](PostgresTestSupport.truncateSchemas(_, List("artwork")))) >>> ArtworkDb.repository

  private val seeds = ArtworkSampleLayer.seedArtworks.values.toList.sortBy(_.id.value)

  def contract[R](label: String, fresh: ZLayer[R, Nothing, ArtworkRepository]) = suite(label)(
    test("sample submissions (with revisions & preflight reports) round-trip") {
      for
        repo <- ZIO.service[ArtworkRepository]
        _    <- ZIO.foreachDiscard(seeds)(repo.save)
        back <- ZIO.foreach(seeds)(s => repo.findById(s.id))
      yield assertTrue(back == seeds.map(Some(_)))
    },
    test("find by owner distinguishes draft sessions and order lines") {
      val draft = seeds.head.copy(id = ArtworkId.unsafe("art-draft-x"), owner = ArtworkOwner.DraftSession(NonEmptyString.unsafe("sess-42")))
      val line0 = seeds.head.copy(id = ArtworkId.unsafe("art-l0"), owner = ArtworkOwner.OrderLineRef(OrderId.unsafe("ord-x"), 0))
      val line1 = seeds.head.copy(id = ArtworkId.unsafe("art-l1"), owner = ArtworkOwner.OrderLineRef(OrderId.unsafe("ord-x"), 1))
      for
        repo  <- ZIO.service[ArtworkRepository]
        _     <- ZIO.foreachDiscard(List(draft, line0, line1))(repo.save)
        d     <- repo.findByOwner(ArtworkOwner.DraftSession(NonEmptyString.unsafe("sess-42")))
        l1    <- repo.findByOwner(ArtworkOwner.OrderLineRef(OrderId.unsafe("ord-x"), 1))
        _     <- repo.delete(draft.id)
        dGone <- repo.findByOwner(ArtworkOwner.DraftSession(NonEmptyString.unsafe("sess-42")))
      yield assertTrue(d.map(_.id) == List(draft.id), l1.map(_.id) == List(line1.id), dGone.isEmpty)
    },
  ).provideSomeLayer[R](fresh)

  def spec = suite("Artwork repository contract")(
    contract("in-memory", inMemory),
    contract("postgres", postgres).provideShared(PostgresTestSupport.database(List(ArtworkDb.migrations))),
  ) @@ TestAspect.sequential
