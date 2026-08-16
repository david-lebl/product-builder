package mpbuilder.catalog
package impl
package legacy

import mpbuilder.commons.*
import mpbuilder.domain.codec.ConfigurationCodecs.given
import mpbuilder.domain.model as dm
import mpbuilder.domain.rules.CompatibilityRuleset
import mpbuilder.domain.sample.{SampleCatalog, SampleRules}
import mpbuilder.domain.service.{ConfigurationBuilder, ConfigurationRequest}
import mpbuilder.domain.validation.ConfigurationError
import zio.*
import zio.json.*
import zio.prelude.Validation

/** [[CatalogService]] backed by the legacy `domain` module and its in-memory sample data.
  *
  * This is the seam that lets order-intake be built against a real contract before the catalog
  * model has actually been extracted. Everything catalog-specific stays behind
  * [[CatalogService]]; when the model moves into `catalog/01-core/impl` (Phase 7) this class is
  * deleted and the callers do not change.
  */
private[catalog] final class LegacyCatalogService(
    catalog: dm.ProductCatalog,
    ruleset: CompatibilityRuleset,
) extends CatalogService:

  // The legacy ProductCatalog carries no version of its own. A content hash is honest — it changes
  // exactly when the catalog changes — and is stable across restarts, unlike a timestamp.
  private val version: CatalogVersion =
    CatalogVersion(
      f"sample-${(catalog.categories.keySet.## * 31 + catalog.materials.keySet.##).toHexString}%s"
    )

  def currentVersion: UIO[CatalogVersion] = ZIO.succeed(version)

  def configure(request: ConfigurationRequestDto): IO[CatalogError, ConfigurationView] =
    for
      domainRequest <- ZIO.fromEither(Mapping.toDomain(request))
      config <- build(domainRequest)
    yield ConfigurationView(
      snapshot = ConfigurationSnapshot(config.toJson, version),
      description = LocalizedString(
        Describe(config, Language.En),
        Describe(config, Language.Cs),
      ),
    )

  def revalidate(snapshot: ConfigurationSnapshot): IO[CatalogError, Unit] =
    // "Still buildable" is answered by rebuilding: take the ids the snapshot was made from and run
    // them through today's catalog and rules. A withdrawn material or a tightened compatibility
    // rule shows up as a build failure, which is exactly the question being asked.
    for
      stored <- decode(snapshot)
      _ <- build(Mapping.toRequest(stored))
    yield ()

  def describe(snapshot: ConfigurationSnapshot, lang: Language): IO[CatalogError, String] =
    decode(snapshot).map(Describe(_, lang))

  // ── internals ────────────────────────────────────────────────────────────

  private def decode(snapshot: ConfigurationSnapshot): IO[CatalogError, dm.ProductConfiguration] =
    ZIO
      .fromEither(snapshot.payload.fromJson[dm.ProductConfiguration])
      .mapError(CatalogError.MalformedSnapshot(_))

  // A snapshot is a *value*, not an entity: two identical configurations are the same thing, and
  // must serialize identically so `fingerprint` can deduplicate basket items. A per-build random
  // ConfigurationId would be baked into the payload and defeat that. Whatever identity a stored
  // configuration needs is supplied by the context that stores it (an order line id, a basket item
  // id) — never by the snapshot itself.
  private val snapshotId = dm.ConfigurationId.unsafe("snapshot")

  private def build(request: ConfigurationRequest): IO[CatalogError, dm.ProductConfiguration] =
    toZIO(ConfigurationBuilder.build(request, catalog, ruleset, snapshotId))

  /** Carry the accumulated errors across into the public error type, keeping every one of them —
    * collapsing to the first would defeat the point of `Validation`.
    */
  private def toZIO[A](v: Validation[ConfigurationError, A]): IO[CatalogError, A] =
    v.toEither match
      case Right(a) => ZIO.succeed(a)
      case Left(errors) =>
        val problems = NonEmptyChunk
          .fromIterableOption(errors.toList.map(Mapping.toProblem))
          .getOrElse(NonEmptyChunk(Problem("Unknown", "Configuration is not valid", "Konfigurace není platná")))
        ZIO.fail(CatalogError.Rejected(problems))

object LegacyCatalogService:

  /** Wired against the bundled sample catalog — the only catalog that exists until the model is
    * extracted and persisted.
    */
  val layer: ULayer[CatalogService] =
    ZLayer.succeed(new LegacyCatalogService(SampleCatalog.catalog, SampleRules.ruleset))

  def of(catalog: dm.ProductCatalog, ruleset: CompatibilityRuleset): CatalogService =
    new LegacyCatalogService(catalog, ruleset)
