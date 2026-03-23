package mpbuilder.domain.service

import mpbuilder.domain.model.*

/** Maps visual product types and formats to domain model concepts (categories, specs). */
object VisualProductDomainMapping:

  /** Maps a VisualProductType to the corresponding domain CategoryId.
    *
    * - Calendar types (Monthly, Weekly, Bi-weekly) → cat-calendars
    * - Photo Book → cat-booklets
    * - Wall Picture → cat-banners (large format)
    */
  def toCategoryId(productType: VisualProductType): CategoryId = productType match
    case VisualProductType.MonthlyCalendar  => CategoryId.unsafe("cat-calendars")
    case VisualProductType.WeeklyCalendar   => CategoryId.unsafe("cat-calendars")
    case VisualProductType.BiweeklyCalendar => CategoryId.unsafe("cat-calendars")
    case VisualProductType.PhotoBook        => CategoryId.unsafe("cat-booklets")
    case VisualProductType.WallPicture      => CategoryId.unsafe("cat-banners")

  /** Builds the domain ProductSpecifications from a visual product type and format.
    *
    * Includes: SizeSpec (from format dimensions), PagesSpec (from product type page count),
    * and OrientationSpec (derived from format dimensions).
    */
  def toSpecifications(productType: VisualProductType, format: ProductFormat): ProductSpecifications =
    val specs = List(
      format.toSizeSpec,
      SpecValue.PagesSpec(productType.defaultPageCount),
      SpecValue.OrientationSpec(format.orientation),
    )
    ProductSpecifications.fromSpecs(specs)

  /** Returns the CategoryId string value for a product type. */
  def categoryIdValue(productType: VisualProductType): String =
    toCategoryId(productType).value

  /** Checks if a format is applicable to a given product type. */
  def isFormatApplicable(productType: VisualProductType, format: ProductFormat): Boolean =
    ProductFormat.formatsFor(productType).exists(_.id == format.id)
