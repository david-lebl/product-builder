package mpbuilder.domain

import mpbuilder.domain.catalog.*

/** Bilingual display names for enum-based catalog concepts (fold types,
  * binding methods, speed tiers). Real catalog entities (materials, finishes,
  * categories) carry their own LocalizedText.
  */
object Labels:

  def fold(f: FoldType): LocalizedText = f match
    case FoldType.HalfFold      => LocalizedText("Half fold", "Půlený lom (V)")
    case FoldType.TriFold       => LocalizedText("Tri-fold", "Trojlom (C)")
    case FoldType.GateFold      => LocalizedText("Gate fold", "Okenní lom")
    case FoldType.AccordionFold => LocalizedText("Accordion fold", "Harmonikový lom")
    case FoldType.ZFold         => LocalizedText("Z-fold", "Z lom")
    case FoldType.RollFold      => LocalizedText("Roll fold", "Rolovaný lom")
    case FoldType.FrenchFold    => LocalizedText("French fold", "Francouzský lom")
    case FoldType.CrossFold     => LocalizedText("Cross fold", "Křížový lom")

  def binding(b: BindingMethod): LocalizedText = b match
    case BindingMethod.SaddleStitch   => LocalizedText("Saddle stitch", "Šitá vazba (V1)")
    case BindingMethod.PerfectBinding => LocalizedText("Perfect binding", "Lepená vazba (V2)")
    case BindingMethod.SpiralBinding  => LocalizedText("Spiral binding", "Spirálová vazba")
    case BindingMethod.WireOBinding   => LocalizedText("Wire-O binding", "Vazba Wire-O")
    case BindingMethod.CaseBinding    => LocalizedText("Case binding", "Pevná vazba (V8)")

  def speed(s: SpeedTier): LocalizedText = s match
    case SpeedTier.Express  => LocalizedText("Express", "Expresní")
    case SpeedTier.Standard => LocalizedText("Standard", "Standardní")
    case SpeedTier.Economy  => LocalizedText("Economy", "Ekonomická")

  def componentRole(r: ComponentRole): LocalizedText = r match
    case ComponentRole.Main  => LocalizedText("Main", "Hlavní část")
    case ComponentRole.Cover => LocalizedText("Cover", "Obálka")
    case ComponentRole.Body  => LocalizedText("Body", "Vnitřní listy")
    case ComponentRole.Stand => LocalizedText("Stand", "Stojan")

  val cutting: LocalizedText  = LocalizedText("Cutting", "Řezání")
  val creasing: LocalizedText = LocalizedText("Creasing setup", "Příprava rylování")
