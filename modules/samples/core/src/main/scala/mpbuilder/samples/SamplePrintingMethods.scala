package mpbuilder.samples

import mpbuilder.catalog.*
import mpbuilder.kernel.*
import SampleIds.*

object SamplePrintingMethods:

  // --- Printing Methods ---
  val offsetMethod: PrintingMethod = PrintingMethod(
    id = offsetId,
    name = LocalizedString("Offset Printing", "Ofsetový tisk"),
    processType = PrintingProcessType.Offset,
    maxColorCount = Some(6),
    description = Some(LocalizedString(
      "Traditional high-quality printing using plates. Best for large runs (500+) with consistent color accuracy. Supports Pantone spot colors for precise brand matching.",
      "Tradiční vysoce kvalitní tisk pomocí tiskových desek. Nejlepší pro velké náklady (500+) s konzistentní přesností barev. Podporuje přímé barvy Pantone pro přesné shody značek.",
    )),
  )

  val digitalMethod: PrintingMethod = PrintingMethod(
    id = digitalId,
    name = LocalizedString("Digital Printing", "Digitální tisk"),
    processType = PrintingProcessType.Digital,
    maxColorCount = None,
    description = Some(LocalizedString(
      "Modern toner or inkjet-based printing. Cost-effective for short runs and variable data. No plate setup required — ideal for quick turnaround and personalized prints.",
      "Moderní tisk na bázi toneru nebo inkoustu. Ekonomický pro malé náklady a variabilní data. Nevyžaduje přípravu tiskových desek — ideální pro rychlou realizaci a personalizované tisky.",
    )),
  )

  val uvInkjetMethod: PrintingMethod = PrintingMethod(
    id = uvInkjetId,
    name = LocalizedString("UV Curable Inkjet", "UV inkoustový tisk"),
    processType = PrintingProcessType.UVCurableInkjet,
    maxColorCount = None,
    description = Some(LocalizedString(
      "Wide-format inkjet printing with UV-cured inks. Produces durable, scratch-resistant output for banners, signage, and outdoor graphics. Excellent on vinyl and rigid substrates.",
      "Velkoformátový inkoustový tisk s UV vytvrzovanými inkousty. Produkuje odolný, proti poškrábání odolný výstup pro bannery, značení a venkovní grafiku. Vynikající na vinylu a tuhých materiálech.",
    )),
  )

  val letterpressMethod: PrintingMethod = PrintingMethod(
    id = letterpressId,
    name = LocalizedString("Letterpress", "Knihtisk"),
    processType = PrintingProcessType.Letterpress,
    maxColorCount = Some(2),
    description = Some(LocalizedString(
      "Artisan relief printing that presses ink into thick paper creating a debossed tactile impression. Limited to 1–2 colors. Premium choice for luxury business cards and invitations.",
      "Řemeslný reliéfní tisk, který vtlačuje inkoust do silného papíru a vytváří hmatatelný dojem. Omezeno na 1–2 barvy. Prémiová volba pro luxusní vizitky a pozvánky.",
    )),
  )

  // --- Promotional Printing Methods ---
  val screenPrintMethod: PrintingMethod = PrintingMethod(
    id = screenPrintId,
    name = LocalizedString("Screen Printing", "Sítotisk"),
    processType = PrintingProcessType.ScreenPrint,
    maxColorCount = Some(8),
    description = Some(LocalizedString("Best for bulk orders with vibrant solid colors", "Nejlepší pro velké náklady se sytými plnými barvami")),
  )

  val dtgMethod: PrintingMethod = PrintingMethod(
    id = dtgId,
    name = LocalizedString("Direct-to-Garment (DTG)", "Přímý tisk na textil (DTG)"),
    processType = PrintingProcessType.Digital,
    maxColorCount = None,
    description = Some(LocalizedString("Full-color photo prints, best for small runs", "Plnobarevný fototisk, nejlepší pro malé náklady")),
  )

  val sublimationMethod: PrintingMethod = PrintingMethod(
    id = sublimationId,
    name = LocalizedString("Dye Sublimation", "Sublimační tisk"),
    processType = PrintingProcessType.Digital,
    maxColorCount = None,
    description = Some(LocalizedString("All-over prints on polyester and coated surfaces", "Celoplošný tisk na polyester a povrchově upravené materiály")),
  )

  val solventInkjetMethod: PrintingMethod = PrintingMethod(
    id = solventInkjetId,
    name = LocalizedString("Solvent Inkjet", "Solventový inkoustový tisk"),
    processType = PrintingProcessType.SolventInkjet,
    maxColorCount = None,
    description = Some(LocalizedString(
      "Wide-format printing with solvent-based inks on self-adhesive vinyl. Excellent outdoor durability and weather resistance. Ideal for outdoor stickers, vehicle wraps, and signage.",
      "Velkoformátový tisk se solventovými inkousty na samolepicím vinylu. Vynikající odolnost pro venkovní použití a povětrnostní vlivy. Ideální pro venkovní samolepky, polepy vozidel a značení.",
    )),
  )

  val epson8ColorMethod: PrintingMethod = PrintingMethod(
    id = epson8ColorId,
    name = LocalizedString("Epson 8-Color (Extended Gamut)", "Epson 8 barev (rozšířená barevná škála)"),
    processType = PrintingProcessType.LatexInkjet,
    maxColorCount = Some(8),
    description = Some(LocalizedString(
      "Premium 8-channel wide-gamut inkjet printing for stickers and labels. Extended color gamut with dedicated light-cyan, light-magenta, and additional ink channels. Delivers exceptional photo quality and color accuracy on vinyl substrates.",
      "Prémiový 8-kanálový inkoustový tisk s rozšířenou barevnou škálou pro samolepky a štítky. Dedikované kanály pro světle azurovou, světle purpurovou a další inkousty zajišťují výjimečnou foto-kvalitu a přesnost barev na vinylových materiálech.",
    )),
  )

