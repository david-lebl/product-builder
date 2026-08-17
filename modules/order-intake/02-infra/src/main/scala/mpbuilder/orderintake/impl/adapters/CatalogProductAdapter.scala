package mpbuilder.orderintake
package impl
package adapters

import mpbuilder.catalog as cat
import mpbuilder.commons.*
import zio.*
import mpbuilder.catalog.json.given
import zio.json.*

/** [[ProductPort]] satisfied by calling catalog's public service.
  *
  * One of only two files in this context that know catalog exists. When catalog becomes a separate
  * deployable, this class is replaced by an HTTP client against the same port and nothing in
  * `01-core` changes.
  */
private[orderintake] final class CatalogProductAdapter(catalog: cat.CatalogService)
    extends ProductPort:

  def specFor(configurationJson: String): IO[BasketError, (ProductSpec, LocalizedString)] =
    for
      request <- ZIO
        .fromEither(configurationJson.fromJson[cat.ConfigurationRequestDto])
        .mapError(detail =>
          BasketError.Rejected(
            NonEmptyChunk(
              Problem(
                "MalformedConfiguration",
                s"Configuration could not be read: $detail",
                s"Konfiguraci nelze přečíst: $detail",
              )
            )
          )
        )
      view <- catalog.configure(request).mapError(toBasketError)
    yield (ProductSpec(view.snapshot.payload, view.snapshot.catalogVersion.value), view.description)

  def stillBuildable(spec: ProductSpec): IO[BasketError, Unit] =
    catalog
      .revalidate(cat.ConfigurationSnapshot(spec.payload, cat.CatalogVersion(spec.catalogVersion)))
      .mapError(toBasketError)

  /** Catalog's vocabulary does not cross the boundary: its errors become this context's, keeping
    * their localized text so the customer still learns what is actually wrong.
    */
  private def toBasketError(error: cat.CatalogError): BasketError = error match
    case cat.CatalogError.Rejected(problems) => BasketError.Rejected(problems)
    case cat.CatalogError.UnknownValue(field, value) => BasketError.UnknownValue(field, value)
    case other =>
      BasketError.Rejected(
        NonEmptyChunk(
          Problem(
            other.toString.takeWhile(_ != '('),
            other.message(Language.En),
            other.message(Language.Cs),
          )
        )
      )

private[orderintake] object CatalogProductAdapter:
  val layer: URLayer[cat.CatalogService, ProductPort] =
    ZLayer.fromFunction(new CatalogProductAdapter(_))
