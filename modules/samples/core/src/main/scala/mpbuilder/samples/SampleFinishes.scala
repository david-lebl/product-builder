package mpbuilder.samples

import mpbuilder.catalog.*
import mpbuilder.kernel.*
import SampleIds.*

object SampleFinishes:

  // --- Finishes ---
  val matteLamination: Finish = Finish(
    id = matteLaminationId,
    name = LocalizedString("Matte Lamination", "Matná laminace"),
    finishType = FinishType.Lamination,
    side = FinishSide.Both,
    description = Some(LocalizedString(
      "Protective matte film applied to the surface. Provides a smooth, non-reflective finish with a velvety feel. Reduces glare and fingerprints.",
      "Ochranná matná fólie aplikovaná na povrch. Poskytuje hladký, nereflexní povrch s hedvábným dojmem. Snižuje odlesky a otisky prstů.",
    )),
  )

  val glossLamination: Finish = Finish(
    id = glossLaminationId,
    name = LocalizedString("Gloss Lamination", "Lesklá laminace"),
    finishType = FinishType.Lamination,
    side = FinishSide.Both,
    description = Some(LocalizedString(
      "Shiny protective film that enhances color vibrancy and contrast. Makes images pop with a high-gloss mirror-like finish. Adds durability and water resistance.",
      "Lesklá ochranná fólie, která zvyšuje sytost barev a kontrast. Dodává obrázkům výraznost s vysoce lesklým zrcadlovým povrchem. Přidává odolnost a voděodolnost.",
    )),
  )

  val uvCoating: Finish = Finish(
    id = uvCoatingId,
    name = LocalizedString("UV Coating", "UV lak"),
    finishType = FinishType.UVCoating,
    side = FinishSide.Front,
    description = Some(LocalizedString(
      "Liquid coating cured with ultraviolet light. Provides a high-gloss, scratch-resistant surface. Can be applied as full coverage or spot UV for selective highlighting.",
      "Tekutý lak vytvrzený ultrafialovým světlem. Poskytuje vysoce lesklý povrch odolný proti poškrábání. Lze aplikovat plošně nebo jako parciální UV pro selektivní zvýraznění.",
    )),
  )

  val embossing: Finish = Finish(
    id = embossingId,
    name = LocalizedString("Embossing", "Slepotisk"),
    finishType = FinishType.Embossing,
    side = FinishSide.Front,
    description = Some(LocalizedString(
      "Raised relief pressing that creates a 3D tactile effect on the paper surface. Adds a premium, luxurious feel to logos, text, or patterns.",
      "Reliéfní lisování, které vytváří 3D hmatový efekt na povrchu papíru. Dodává prémiový, luxusní dojem logům, textu nebo vzorům.",
    )),
  )

  val foilStamping: Finish = Finish(
    id = foilStampingId,
    name = LocalizedString("Foil Stamping", "Ražba fólií"),
    finishType = FinishType.FoilStamping,
    side = FinishSide.Front,
    description = Some(LocalizedString(
      "Metallic or colored foil pressed onto the surface using heat and pressure. Available in gold, silver, copper, rose gold, and holographic. Creates an eye-catching luxurious accent.",
      "Kovová nebo barevná fólie vtlačená na povrch pomocí tepla a tlaku. K dispozici ve zlaté, stříbrné, měděné, růžovozlaté a holografické. Vytváří poutavý luxusní akcent.",
    )),
  )

  val dieCut: Finish = Finish(
    id = dieCutId,
    name = LocalizedString("Die Cut", "Výsek"),
    finishType = FinishType.DieCut,
    side = FinishSide.Both,
    description = Some(LocalizedString(
      "Custom shape cutting using a metal die. Allows creating unique product shapes beyond standard rectangles — rounded edges, windows, or custom contours.",
      "Řezání do vlastního tvaru pomocí kovové formy. Umožňuje vytvořit jedinečné tvary produktů mimo standardní obdélníky — zaoblené hrany, okénka nebo vlastní kontury.",
    )),
  )

  val varnish: Finish = Finish(
    id = varnishId,
    name = LocalizedString("Spot Varnish", "Parciální lak"),
    finishType = FinishType.Varnish,
    side = FinishSide.Front,
    description = Some(LocalizedString(
      "Selective glossy coating applied to specific areas (logos, images, text) to create contrast against a matte background. Adds depth and visual interest.",
      "Selektivní lesklý lak aplikovaný na konkrétní oblasti (loga, obrázky, text) pro vytvoření kontrastu oproti matnému pozadí. Přidává hloubku a vizuální zajímavost.",
    )),
  )

  val softTouchCoating: Finish = Finish(
    id = softTouchCoatingId,
    name = LocalizedString("Soft Touch Coating", "Soft touch laminace"),
    finishType = FinishType.SoftTouchCoating,
    side = FinishSide.Both,
    description = Some(LocalizedString(
      "Luxurious velvet-like coating that feels like suede. Creates an ultra-premium tactile experience. Subdues colors slightly for an elegant, sophisticated look.",
      "Luxusní sametový povlak, který na dotek připomíná semiš. Vytváří ultra-prémiový hmatový zážitek. Mírně tlumí barvy pro elegantní, sofistikovaný vzhled.",
    )),
  )

  val aqueousCoating: Finish = Finish(
    id = aqueousCoatingId,
    name = LocalizedString("Aqueous Coating", "Disperzní lak"),
    finishType = FinishType.AqueousCoating,
    side = FinishSide.Both,
    description = Some(LocalizedString(
      "Water-based protective coating applied during printing. Eco-friendly option that provides basic scuff protection and enhances print quality. Fast-drying and recyclable.",
      "Ochranný lak na vodní bázi aplikovaný během tisku. Ekologická volba, která poskytuje základní ochranu proti oděru a zlepšuje kvalitu tisku. Rychleschnoucí a recyklovatelný.",
    )),
  )

  val debossing: Finish = Finish(
    id = debossingId,
    name = LocalizedString("Debossing", "Slepotisk do hloubky"),
    finishType = FinishType.Debossing,
    side = FinishSide.Front,
    description = Some(LocalizedString(
      "Pressed indentation creating a sunken relief in the paper. The opposite of embossing — pushes the design into the material for a subtle, elegant tactile effect.",
      "Vtlačený otisk vytvářející zapuštěný reliéf v papíru. Opak slepotisku — vtlačuje design do materiálu pro jemný, elegantní hmatový efekt.",
    )),
  )

  val scoring: Finish = Finish(
    id = scoringId,
    name = LocalizedString("Scoring", "Bigování"),
    finishType = FinishType.Scoring,
    side = FinishSide.Both,
    description = Some(LocalizedString(
      "Creased line pressed into the paper to enable clean, precise folding without cracking. Essential for thick paper stocks and folded products like brochures.",
      "Rýha vtlačená do papíru pro čisté, přesné skládání bez praskání. Nezbytné pro silné papíry a skládané produkty jako brožury.",
    )),
  )

  val perforation: Finish = Finish(
    id = perforationId,
    name = LocalizedString("Perforation", "Perforace"),
    finishType = FinishType.Perforation,
    side = FinishSide.Both,
    description = Some(LocalizedString(
      "Line of tiny holes punched into the paper allowing easy tearing along a straight line. Used for tear-off coupons, tickets, and response cards.",
      "Řada drobných otvorů vyražených do papíru umožňující snadné odtržení podél přímky. Používá se pro odtrhávací kupóny, vstupenky a odpovědní karty.",
    )),
  )

  val roundCorners: Finish = Finish(
    id = roundCornersId,
    name = LocalizedString("Round Corners", "Zaoblené rohy"),
    finishType = FinishType.RoundCorners,
    side = FinishSide.Both,
    description = Some(LocalizedString(
      "Rounded corner cutting for a modern, friendly look. Prevents dog-earing and gives a polished appearance. Configurable corner count (1–4) and radius.",
      "Zaoblení rohů pro moderní, přátelský vzhled. Zabraňuje ohýbání a dodává uhlazenou podobu. Konfigurovatelný počet rohů (1–4) a poloměr.",
    )),
  )

  val grommets: Finish = Finish(
    id = grommetsId,
    name = LocalizedString("Grommets", "Průchodky"),
    finishType = FinishType.Grommets,
    side = FinishSide.Both,
    description = Some(LocalizedString(
      "Metal eyelets punched along the edges for hanging banners with hooks, ropes, or zip ties. Spacing is configurable. Essential for large-format outdoor displays.",
      "Kovové průchodky vyražené podél okrajů pro zavěšení bannerů pomocí háčků, lan nebo stahovacích pásků. Rozestupy jsou konfigurovatelné. Nezbytné pro velkoformátové venkovní displeje.",
    )),
  )

  val kissCut: Finish = Finish(
    id = kissCutId,
    name = LocalizedString("Kiss Cut", "Výsek bez podkladu"),
    finishType = FinishType.KissCut,
    side = FinishSide.Both,
    description = Some(LocalizedString(
      "Precision cutting through the top layer only, leaving the backing sheet intact. Ideal for sticker sheets where individual stickers peel off easily.",
      "Přesné řezání pouze skrze horní vrstvu, přičemž podkladový arch zůstává neporušený. Ideální pro samolepicí archy, kde se jednotlivé samolepky snadno odlepují.",
    )),
  )

  val overlamination: Finish = Finish(
    id = overlaminationId,
    name = LocalizedString("Overlamination", "Ochranná laminace"),
    finishType = FinishType.Overlamination,
    side = FinishSide.Front,
    description = Some(LocalizedString(
      "Additional protective laminate layer for large-format prints. Shields against UV fading, scratches, and moisture. Extends the life of outdoor graphics significantly.",
      "Dodatečná ochranná laminátová vrstva pro velkoformátové tisky. Chrání před UV vyblednutím, poškrábáním a vlhkostí. Výrazně prodlužuje životnost venkovní grafiky.",
    )),
  )

  val gumRope: Finish = Finish(
    id = gumRopeId,
    name = LocalizedString("Gum rope", "Gumový provaz"),
    finishType = FinishType.RopeAccessory,
    side = FinishSide.Both,
    description = Some(LocalizedString(
      "Elastic tension rope threaded through the grommets for easy hanging and tensioning of large-format banners. Priced per metre. Requires grommets.",
      "Elastický napínací provaz navlečený průchodkami pro snadné zavěšení a napnutí velkoformátových bannerů. Cena za metr. Vyžaduje průchodky.",
    )),
  )

  // --- Promotional Finishes ---
  val heatPress: Finish = Finish(
    id = heatPressId,
    name = LocalizedString("Heat Press Transfer", "Přenos tepelným lisem"),
    finishType = FinishType.Mounting,
    side = FinishSide.Front,
    description = Some(LocalizedString("Design transferred via heat press onto garment", "Design přenesený tepelným lisem na textil")),
  )

  val labelPrint: Finish = Finish(
    id = labelPrintId,
    name = LocalizedString("Label / Tag Printing", "Tisk štítků / visaček"),
    finishType = FinishType.Numbering,
    side = FinishSide.Back,
    description = Some(LocalizedString("Custom labels sewn or printed on collar/hem", "Vlastní štítky všité nebo potištěné na límci/lemu")),
  )

  val foldBag: Finish = Finish(
    id = foldBagId,
    name = LocalizedString("Fold & Bag Packaging", "Složení a balení do sáčku"),
    finishType = FinishType.Binding,
    side = FinishSide.Both,
    description = Some(LocalizedString("Individual folding and polybag packaging", "Individuální složení a balení do polyethylenového sáčku")),
  )

  val mylarOverlay: Finish = Finish(
    id = mylarOverlayId,
    name = LocalizedString("Mylar Film Overlay", "Mylarová fólie"),
    finishType = FinishType.Overlamination,
    side = FinishSide.Front,
    description = Some(LocalizedString("Protective clear film over printed design", "Ochranná průhledná fólie přes potisk")),
  )

  val safetyPin: Finish = Finish(
    id = safetyPinId,
    name = LocalizedString("Safety Pin Back", "Zadní špendlík"),
    finishType = FinishType.Mounting,
    side = FinishSide.Back,
    description = Some(LocalizedString("Standard safety pin mechanism", "Standardní zavírací špendlík")),
  )

  val magnetBack: Finish = Finish(
    id = magnetBackId,
    name = LocalizedString("Magnet Back", "Magnetické uchycení"),
    finishType = FinishType.Mounting,
    side = FinishSide.Back,
    description = Some(LocalizedString("Magnetic backing instead of pin", "Magnetické uchycení místo špendlíku")),
  )

  val bottleOpener: Finish = Finish(
    id = bottleOpenerId,
    name = LocalizedString("Bottle Opener Back", "Otvírák na lahve"),
    finishType = FinishType.Mounting,
    side = FinishSide.Back,
    description = Some(LocalizedString("Dual-purpose badge with bottle opener", "Dvojúčelový odznak s otvírákem na lahve")),
  )

  val dishwasherCoat: Finish = Finish(
    id = dishwasherCoatId,
    name = LocalizedString("Dishwasher-Safe Coating", "Nátěr odolný myčce"),
    finishType = FinishType.Overlamination,
    side = FinishSide.Both,
    description = Some(LocalizedString("Protective coating for durability in dishwashers", "Ochranný nátěr pro odolnost v myčce")),
  )

  val giftBox: Finish = Finish(
    id = giftBoxId,
    name = LocalizedString("Gift Box Packaging", "Dárková krabička"),
    finishType = FinishType.Binding,
    side = FinishSide.Both,
    description = Some(LocalizedString("Individual gift box for each item", "Individuální dárková krabička pro každý kus")),
  )

  val glossyGlaze: Finish = Finish(
    id = glossyGlazeId,
    name = LocalizedString("Glossy Ceramic Glaze", "Lesklá keramická glazura"),
    finishType = FinishType.UVCoating,
    side = FinishSide.Both,
    description = Some(LocalizedString("High-gloss ceramic glaze finish", "Vysokolesklá keramická glazura")),
  )

  val embroideryFinish: Finish = Finish(
    id = embroideryId,
    name = LocalizedString("Embroidery", "Výšivka"),
    finishType = FinishType.Embroidery,
    side = FinishSide.Front,
    description = Some(LocalizedString("Thread-based logo/design application", "Aplikace loga/designu výšivkou")),
  )

  val reinforcedHandles: Finish = Finish(
    id = reinforcedHandlesId,
    name = LocalizedString("Reinforced Handles", "Zpevněná ucha"),
    finishType = FinishType.Binding,
    side = FinishSide.Both,
    description = Some(LocalizedString("Double-stitched handles for durability", "Dvojitě prošitá ucha pro odolnost")),
  )

