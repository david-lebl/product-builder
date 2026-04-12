package mpbuilder.domain.model

/** High-level product grouping for the customer-facing catalog.
  *
  * Categories like business cards, flyers, brochures are grouped under
  * a broader heading so the catalog can present a tabbed or sectioned view.
  */
enum CatalogGroup:
  case Sheet       // Flat-sheet printed products (business cards, flyers, brochures, postcards, etc.)
  case LargeFormat // Wide-format printed products (banners, roll-ups, etc.)
  case Bound       // Multi-page bound products (booklets, calendars, etc.)
  case Specialty   // Specialty products (packaging, stickers & labels, etc.)
  case Promotional // Branded merchandise (T-shirts, bags, mugs, badges, etc.)

/** A variation of a showcase product (e.g. "Standard" vs "Folded" business cards).
  *
  * When `presetId` is set, clicking the variation in the catalog opens the
  * product builder with that preset pre-selected.
  */
final case class ProductVariation(
    name: LocalizedString,
    description: LocalizedString,
    imageUrl: Option[String] = None,
    presetId: Option[PresetId] = None,
)

/** An educational guide section displayed on the product detail page.
  *
  * Each section provides in-depth information aimed at newcomers — e.g.
  * explaining binding types, paper choices, finishing options, or how to
  * prepare artwork.
  */
final case class GuideSection(
    title: LocalizedString,
    body: LocalizedString,
    imageUrl: Option[String] = None,
)

/** A highlight / key feature shown on the product detail page. */
final case class ProductFeature(
    icon: String,
    title: LocalizedString,
    description: LocalizedString,
)

/** A single product displayed in the customer-facing catalog.
  *
  * Each ShowcaseProduct maps to exactly one [[ProductCategory]] in the
  * configuration engine. It enriches the category with marketing content:
  * hero images, detailed descriptions, variations, features, and ordering
  * instructions.
  */
final case class ShowcaseProduct(
    categoryId: CategoryId,
    group: CatalogGroup,
    tagline: LocalizedString,
    detailedDescription: LocalizedString,
    imageUrl: String,
    galleryImageUrls: List[String] = List.empty,
    variations: List[ProductVariation] = List.empty,
    features: List[ProductFeature] = List.empty,
    instructions: Option[LocalizedString] = None,
    guideSections: List[GuideSection] = List.empty,
    popularFinishes: List[String] = List.empty,
    turnaroundDays: Option[String] = None,
    sortOrder: Int = 0,
)
