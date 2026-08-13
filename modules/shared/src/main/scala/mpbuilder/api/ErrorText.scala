package mpbuilder.api

import mpbuilder.domain.*
import mpbuilder.domain.catalog.Catalog
import mpbuilder.domain.ids.*
import mpbuilder.domain.pricing.PricingError
import mpbuilder.domain.validation.ValidationError

/** Human-readable bilingual messages for validation and pricing errors — used
  * by the server for 422 payloads and by the UI for its live error panel.
  */
object ErrorText:

  def dto(catalog: Catalog, error: ValidationError | PricingError): ErrorDto =
    error match
      case v: ValidationError => ErrorDto(code(v), validationMessage(catalog, v))
      case p: PricingError    => ErrorDto(code(p), pricingMessage(catalog, p))

  private def code(e: ValidationError | PricingError): String =
    e.toString.takeWhile(_ != '(')

  private def materialName(catalog: Catalog, id: MaterialId): LocalizedText =
    catalog.materialsById.get(id).map(_.name).getOrElse(LocalizedText.plain(id.raw))

  private def finishName(catalog: Catalog, id: FinishId): LocalizedText =
    catalog.finishesById.get(id).map(_.name).getOrElse(LocalizedText.plain(id.raw))

  private def methodName(catalog: Catalog, id: PrintingMethodId): LocalizedText =
    catalog.printingMethodsById.get(id).map(_.name).getOrElse(LocalizedText.plain(id.raw))

  def validationMessage(catalog: Catalog, e: ValidationError): LocalizedText =
    import ValidationError.*
    e match
      case UnknownReference(kind, id) =>
        LocalizedText(s"Unknown $kind: $id", s"Neznámá položka ($kind): $id")
      case MissingDetail(detail) =>
        val name = detail.toString
        LocalizedText(s"Missing required detail: $name", s"Chybí povinný údaj: ${detailCs(detail)}")
      case InvalidQuantity(actual) =>
        LocalizedText(s"Quantity must be positive (got $actual)", s"Množství musí být kladné (zadáno $actual)")
      case MissingRequiredComponent(role) =>
        LocalizedText(
          s"Missing required part: ${Labels.componentRole(role).en}",
          s"Chybí povinná část produktu: ${Labels.componentRole(role).cs}",
        )
      case UnexpectedComponent(role) =>
        LocalizedText(
          s"This product has no ${Labels.componentRole(role).en} part",
          s"Tento produkt nemá část ${Labels.componentRole(role).cs}",
        )
      case MaterialNotAllowedInCategory(role, id) =>
        val m = materialName(catalog, id)
        LocalizedText(
          s"Material ${m.en} is not available for this product's ${Labels.componentRole(role).en}",
          s"Materiál ${m.cs} není pro tuto část produktu dostupný",
        )
      case FinishNotAllowedInCategory(role, id) =>
        val f = finishName(catalog, id)
        LocalizedText(
          s"Finish ${f.en} is not available for this product's ${Labels.componentRole(role).en}",
          s"Zušlechtění ${f.cs} není pro tuto část produktu dostupné",
        )
      case PrintingMethodNotAllowed(id) =>
        val m = methodName(catalog, id)
        LocalizedText(s"${m.en} is not available for this product", s"${m.cs} není pro tento produkt dostupný")
      case TooManyInkColors(id, max, requested) =>
        val m = methodName(catalog, id)
        LocalizedText(
          s"${m.en} supports at most $max colors ($requested requested)",
          s"${m.cs} umožňuje nejvýše $max barvy (požadováno $requested)",
        )
      case OnePerFinishTypeExceeded(_, finishType) =>
        LocalizedText(
          s"Only one $finishType finish can be selected",
          s"Lze zvolit pouze jedno zušlechtění typu $finishType",
        )
      case MutuallyExclusiveFinishTypes(_, a, b) =>
        LocalizedText(s"$a and $b cannot be combined", s"$a a $b nelze kombinovat")
      case MutuallyExclusiveFinishes(_, a, b) =>
        val (fa, fb) = (finishName(catalog, a), finishName(catalog, b))
        LocalizedText(s"${fa.en} and ${fb.en} cannot be combined", s"${fa.cs} a ${fb.cs} nelze kombinovat")
      case FinishRequiresMinWeight(_, finishType, minGsm, actualGsm) =>
        LocalizedText(
          s"$finishType needs a material of at least $minGsm gsm (selected: $actualGsm gsm)",
          s"$finishType vyžaduje materiál alespoň $minGsm g/m² (zvoleno $actualGsm g/m²)",
        )
      case QuantityOutOfRange(min, max, actual) =>
        LocalizedText(
          s"Quantity must be between $min and $max (got $actual)",
          s"Množství musí být mezi $min a $max (zadáno $actual)",
        )
      case SizeOutOfRange(min, max, actual) =>
        LocalizedText(
          s"Size ${actual.widthMm}×${actual.heightMm} mm is outside the allowed range " +
            s"${min.widthMm}×${min.heightMm}–${max.widthMm}×${max.heightMm} mm",
          s"Rozměr ${actual.widthMm}×${actual.heightMm} mm je mimo povolený rozsah " +
            s"${min.widthMm}×${min.heightMm}–${max.widthMm}×${max.heightMm} mm",
        )
      case TooManyCreases(max, actual) =>
        LocalizedText(s"At most $max creases are possible (got $actual)", s"Nejvýše $max rylů (zadáno $actual)")
      case MissingFinishParams(_, id) =>
        val f = finishName(catalog, id)
        LocalizedText(s"${f.en} needs additional parameters", s"${f.cs} vyžaduje doplňující údaje")
      case ContactInvalid(field, message) =>
        LocalizedText(message, contactCs(field))

  def pricingMessage(catalog: Catalog, e: PricingError): LocalizedText =
    import PricingError.*
    e match
      case NoQuantity =>
        LocalizedText("Enter a quantity to calculate the price", "Pro výpočet ceny zadejte množství")
      case NoMaterialPrice(id) =>
        val m = materialName(catalog, id)
        LocalizedText(s"No price configured for ${m.en}", s"Materiál ${m.cs} nemá nastavenou cenu")
      case SizeRequiredForPricing(id) =>
        val m = materialName(catalog, id)
        LocalizedText(s"${m.en} needs a size to be priced", s"Pro ocenění materiálu ${m.cs} zadejte rozměr")
      case DoesNotFitOnSheet(id, size) =>
        val m = materialName(catalog, id)
        LocalizedText(
          s"${size.widthMm}×${size.heightMm} mm does not fit on a press sheet of ${m.en}",
          s"Rozměr ${size.widthMm}×${size.heightMm} mm se nevejde na tiskový arch materiálu ${m.cs}",
        )
      case NoCreaseCountPrice(creases) =>
        LocalizedText(s"No price configured for $creases creases", s"Počet rylů $creases nemá nastavenou cenu")
      case NoGrommetSpacingPrice(spacing) =>
        LocalizedText(
          s"No price configured for $spacing mm grommet spacing",
          s"Rozteč oček $spacing mm nemá nastavenou cenu",
        )
      case MissingFinishParams(id) =>
        val f = finishName(catalog, id)
        LocalizedText(s"${f.en} needs additional parameters", s"${f.cs} vyžaduje doplňující údaje")
      case UnknownReference(kind, id) =>
        LocalizedText(s"Unknown $kind: $id", s"Neznámá položka ($kind): $id")

  private def detailCs(d: mpbuilder.domain.catalog.RequiredDetail): String =
    import mpbuilder.domain.catalog.RequiredDetail.*
    d match
      case Size        => "rozměr"
      case Quantity    => "množství"
      case Orientation => "orientace"
      case Pages       => "počet stran"
      case Fold        => "typ lomu"
      case Binding     => "vazba"

  private def contactCs(field: String): String = field match
    case "name"  => "Zadejte jméno"
    case "email" => "Zadejte platný e-mail"
    case "phone" => "Zadejte telefon"
    case other   => s"Neplatné pole: $other"
