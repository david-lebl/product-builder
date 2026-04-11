package mpbuilder.domain.model

import zio.prelude.*

opaque type PresetId = String
object PresetId:
  def apply(value: String): Validation[String, PresetId] =
    if value.nonEmpty then Validation.succeed(value)
    else Validation.fail("PresetId must not be empty")

  def unsafe(value: String): PresetId = value

  extension (id: PresetId) def value: String = id

/** Default selections for one component within a preset. */
final case class ComponentPreset(
    role: ComponentRole,
    materialId: MaterialId,
    inkConfiguration: InkConfiguration,
    finishSelections: List[FinishSelection] = List.empty,
)

/** A curated starting configuration for a category.
  *
  * Each category carries one or more presets. The first preset in the list is
  * the default, auto-applied when the user selects the category.
  *
  * @param id             unique identifier
  * @param name           localized display name (e.g. "Basic", "Premium")
  * @param description    short localized blurb shown on the card
  * @param printingMethodId  default printing method
  * @param componentPresets  one entry per required component
  * @param specOverrides  spec values that replace the generic category defaults
  */
final case class CategoryPreset(
    id: PresetId,
    name: LocalizedString,
    description: Option[LocalizedString] = None,
    printingMethodId: PrintingMethodId,
    componentPresets: List[ComponentPreset],
    specOverrides: List[SpecValue] = List.empty,
)
