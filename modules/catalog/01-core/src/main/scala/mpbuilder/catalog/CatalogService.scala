package mpbuilder.catalog

import mpbuilder.commons.*
import zio.{IO, UIO, ZIO}

/** The catalog context's public contract: what can be built, and whether a given configuration is
  * valid.
  *
  * This trait is the stable surface other contexts depend on. It speaks only DTOs, [[CatalogError]]
  * and [[ConfigurationSnapshot]] — never the catalog's internal model — so the model can be
  * reshaped freely without breaking a caller.
  */
trait CatalogService:

  /** The catalog state configurations are currently being resolved against. Stamped into every
    * snapshot so a stored configuration can be told apart from a freshly built one.
    */
  def currentVersion: UIO[CatalogVersion]

  /** Resolve and validate a request into a storable snapshot.
    *
    * Fails with [[CatalogError.Rejected]] carrying *all* problems — unknown ids and rule violations
    * alike — so a configurator can show every issue at once rather than one per round trip.
    */
  def configure(request: ConfigurationRequestDto): IO[CatalogError, ConfigurationView]

  /** Is a previously stored snapshot still buildable against today's catalog?
    *
    * Used at checkout: a configuration added to a basket weeks ago may since have become
    * impossible, because a material was withdrawn or a compatibility rule tightened. Succeeds when
    * the product can still be made; fails with the reasons when it cannot.
    */
  def revalidate(snapshot: ConfigurationSnapshot): IO[CatalogError, Unit]

  /** A one-line human description, rendered once and then stored on the order line. */
  def describe(snapshot: ConfigurationSnapshot, lang: Language): IO[CatalogError, String]

object CatalogService:
  def currentVersion: ZIO[CatalogService, Nothing, CatalogVersion] =
    ZIO.serviceWithZIO(_.currentVersion)

  def configure(request: ConfigurationRequestDto): ZIO[CatalogService, CatalogError, ConfigurationView] =
    ZIO.serviceWithZIO(_.configure(request))

  def revalidate(snapshot: ConfigurationSnapshot): ZIO[CatalogService, CatalogError, Unit] =
    ZIO.serviceWithZIO(_.revalidate(snapshot))

  def describe(snapshot: ConfigurationSnapshot, lang: Language): ZIO[CatalogService, CatalogError, String] =
    ZIO.serviceWithZIO(_.describe(snapshot, lang))
