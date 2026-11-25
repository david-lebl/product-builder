package mpbuilder.editor.i18n

import kyo.*
import mpbuilder.shared.Language
import mpbuilder.ui.i18n.{AppI18n, Translations}

/** EN & CS Fluent bundles of the Visual Editor, composed with the storefront bundles (`Translations`) so one
  * `AppI18n` serves the storefront host and the lazily loaded editor alike.
  */
object EditorTranslations:

  def init(initial: Language): AppI18n < Sync =
    for
      handle <- I18n.initInMemory(mergedBundles, Translations.toLocale(initial))
      ref    <- Signal.initRef(initial)
    yield AppI18n(handle, ref)

  lazy val mergedBundles: Dict[Locale, String] = Dict(
    Translations.enLocale -> s"${Translations.enFtl}\n$editorEnFtl",
    Translations.csLocale -> s"${Translations.csFtl}\n$editorCsFtl",
  )

  val editorEnFtl: String =
    """# Visual Editor — shell
      |ve-back-to-shop = Back to shop
      |ve-my-designs = My designs
      |ve-preview = Preview
      |ve-complete-default = Use this design
      |ve-complete-cart = Use this design
      |ve-cancel = Cancel
      |ve-status-unsaved = Unsaved changes
      |ve-status-saving = Saving…
      |ve-status-saved = Saved on this device
      |ve-status-failed = Saving failed
      |
      |# Tool rail
      |ve-tab-templates = Templates
      |ve-tab-text = Text
      |ve-tab-photos = Photos
      |ve-tab-elements = Elements
      |ve-tab-background = Background
      |ve-tab-layouts = Layouts
      |
      |ve-templates-hint = Start from a ready-made layout. Your photos are moved into the new template.
      |ve-templates-confirm = Replace the current design with this template? You can undo it.
      |ve-templates-apply = Apply template
      |ve-templates-none = No templates for this product yet.
      |
      |ve-text-add-heading = Add a heading
      |ve-text-add-subheading = Add a subheading
      |ve-text-add-body = Add body text
      |ve-text-add-script = Add handwritten text
      |ve-text-hint = Double-click text on the page to edit it. Fonts are print-licensed and embedded in the final PDF.
      |
      |ve-photos-upload = Upload photos
      |ve-photos-hint = JPG, PNG or WebP. Photos stay on your device until you order. Drag them straight onto a photo frame.
      |ve-photos-empty = No photos yet
      |ve-photos-add-slot = Add photo frame
      |
      |ve-elements-shapes = Shapes
      |ve-elements-clipart = Decorations
      |ve-elements-calendar = Calendar
      |ve-elements-add-calendar = Add month grid
      |ve-shape-rect = Rectangle
      |ve-shape-ellipse = Ellipse
      |ve-shape-triangle = Triangle
      |ve-shape-line = Line
      |
      |ve-bg-color = Page colour
      |ve-bg-gradients = Gradients
      |ve-bg-reset = Reset to white
      |ve-bg-apply-all = Apply to all pages
      |
      |ve-layouts-hint = Arrange the photo frames of this page. Photos you already placed are kept.
      |ve-layout-full = Full bleed
      |ve-layout-framed = Framed
      |ve-layout-two-cols = Two columns
      |ve-layout-two-rows = Two rows
      |ve-layout-grid4 = Grid of four
      |ve-layout-caption = Photo + caption
      |
      |# Inspector
      |ve-insp-arrange = Arrange
      |ve-insp-to-front = To front
      |ve-insp-forward = Forward
      |ve-insp-backward = Backward
      |ve-insp-to-back = To back
      |ve-insp-position = Position & size (mm)
      |ve-insp-x = X
      |ve-insp-y = Y
      |ve-insp-w = Width
      |ve-insp-h = Height
      |ve-insp-rotation = Rotation °
      |ve-insp-opacity = Opacity
      |ve-insp-text = Text
      |ve-insp-image = Photo
      |ve-insp-shape = Shape
      |ve-insp-clipart = Decoration
      |ve-insp-calendar = Month grid
      |ve-insp-locked-hint = This element is locked. Unlock it to move or edit it.
      |ve-insp-multi = Selection
      |ve-insp-multi-count = { $count } elements selected
      |ve-insp-font = Font
      |ve-insp-size = Size (pt)
      |ve-insp-line-height = Line height
      |ve-insp-valign = Vertical alignment
      |ve-insp-letter-spacing = Letter spacing
      |ve-insp-color = Colour
      |ve-insp-image-empty = This frame is empty. Upload a photo or pick one from the Photos panel.
      |ve-insp-image-remove = Remove photo
      |ve-insp-zoom = Zoom
      |ve-insp-focus-x = Horizontal crop
      |ve-insp-focus-y = Vertical crop
      |ve-insp-filter = Filter
      |ve-insp-radius = Rounded corners
      |ve-insp-border = Border
      |ve-insp-border-width = Border width
      |ve-insp-fill = Fill
      |
      |ve-cal-settings = Calendar
      |ve-cal-start-month = Starting month
      |ve-cal-month = Month
      |ve-cal-year = Year
      |ve-cal-language = Language
      |ve-cal-week-start = Week starts on
      |ve-cal-accent = Heading colour
      |ve-cal-weekend = Weekend & holiday colour
      |
      |ve-fact-format = Trim size
      |ve-fact-bleed = Bleed
      |ve-fact-safe = Safe margin
      |ve-fact-pages = Pages
      |ve-legend-bleed = Bleed — trimmed off
      |ve-legend-safe = Keep text inside
      |
      |# Design checks
      |ve-checks-title = Print checks
      |ve-checks-ok = Everything looks print-ready.
      |ve-issue-very-low-res = Photo resolution too low ({ $dpi } DPI) — it will print blurry
      |ve-issue-low-res = Photo resolution is low ({ $dpi } DPI)
      |ve-issue-empty-slot = Empty photo frame
      |ve-issue-safe-area = Text is close to the edge and may be cut off
      |ve-issue-bleed = Stretch to the bleed line to avoid white edges
      |ve-issue-binding = Overlaps the binding holes
      |ve-issue-text-small = Text is very small ({ $size } pt)
      |ve-issue-empty-text = Empty text box
      |ve-issue-missing-image = Photo is missing — upload it again
      |ve-issue-not-uploaded = Photo is only stored on this device
      |
      |# Review
      |ve-review-title = Check your design
      |ve-review-subtitle = This is how your pages will look after trimming. Please check spelling, dates and photos.
      |ve-review-colour-note = Screens show RGB colours; printing uses CMYK inks, so very bright colours may print slightly softer.
      |ve-review-download = Download proof
      |ve-review-back = Keep editing
      |ve-review-confirm-anyway = Use design anyway
      |
      |# Home
      |ve-home-title = Design Studio
      |ve-home-subtitle = Design business cards, posters, calendars and photo books right in your browser.
      |ve-home-subtitle-product = Design your { $product }
      |ve-home-format = Trim size { $w } × { $h } mm, plus { $bleed } mm bleed on every side.
      |ve-home-continue = Continue a design
      |ve-home-my-designs = My designs
      |ve-home-local-note = Designs are saved automatically on this device.
      |ve-home-new = Start a new design
      |ve-home-blank = Blank
      |ve-home-blank-hint = Start from an empty page
      |
      |ve-toast-not-found = That design is no longer on this device.
      |ve-toast-page-limit = This product does not allow another page change.
      |ve-toast-upload-failed = The photo could not be read.
      |ve-toast-storage-full = Your browser storage is full — delete old designs to make room.
      |ve-toast-attached = Design attached to your order.
      |ve-loading = Loading the design studio…
      |ve-load-failed = The design studio could not be loaded.
      |""".stripMargin

  val editorCsFtl: String =
    """# Vizuální editor — rámec
      |ve-back-to-shop = Zpět do obchodu
      |ve-my-designs = Moje návrhy
      |ve-preview = Náhled
      |ve-complete-default = Použít tento návrh
      |ve-complete-cart = Použít tento návrh
      |ve-cancel = Zrušit
      |ve-status-unsaved = Neuložené změny
      |ve-status-saving = Ukládám…
      |ve-status-saved = Uloženo v tomto zařízení
      |ve-status-failed = Uložení selhalo
      |
      |ve-tab-templates = Šablony
      |ve-tab-text = Text
      |ve-tab-photos = Fotky
      |ve-tab-elements = Prvky
      |ve-tab-background = Pozadí
      |ve-tab-layouts = Rozvržení
      |
      |ve-templates-hint = Začněte z hotového rozvržení. Vaše fotky se přesunou do nové šablony.
      |ve-templates-confirm = Nahradit současný návrh touto šablonou? Lze to vrátit zpět.
      |ve-templates-apply = Použít šablonu
      |ve-templates-none = Pro tento produkt zatím nemáme šablony.
      |
      |ve-text-add-heading = Přidat nadpis
      |ve-text-add-subheading = Přidat podnadpis
      |ve-text-add-body = Přidat odstavec
      |ve-text-add-script = Přidat ručně psaný text
      |ve-text-hint = Text upravíte dvojklikem na stránce. Písma mají licenci pro tisk a vloží se do výsledného PDF.
      |
      |ve-photos-upload = Nahrát fotky
      |ve-photos-hint = JPG, PNG nebo WebP. Fotky zůstávají ve vašem zařízení až do objednání. Přetáhněte je rovnou do rámečku.
      |ve-photos-empty = Zatím žádné fotky
      |ve-photos-add-slot = Přidat rámeček na fotku
      |
      |ve-elements-shapes = Tvary
      |ve-elements-clipart = Ozdoby
      |ve-elements-calendar = Kalendář
      |ve-elements-add-calendar = Přidat kalendárium
      |ve-shape-rect = Obdélník
      |ve-shape-ellipse = Elipsa
      |ve-shape-triangle = Trojúhelník
      |ve-shape-line = Čára
      |
      |ve-bg-color = Barva stránky
      |ve-bg-gradients = Přechody
      |ve-bg-reset = Zpět na bílou
      |ve-bg-apply-all = Použít na všechny stránky
      |
      |ve-layouts-hint = Uspořádejte rámečky na fotky na této stránce. Již vložené fotky zůstanou.
      |ve-layout-full = Na spad
      |ve-layout-framed = S okrajem
      |ve-layout-two-cols = Dva sloupce
      |ve-layout-two-rows = Dva řádky
      |ve-layout-grid4 = Mřížka čtyř
      |ve-layout-caption = Fotka s popiskem
      |
      |ve-insp-arrange = Uspořádání
      |ve-insp-to-front = Navrch
      |ve-insp-forward = Dopředu
      |ve-insp-backward = Dozadu
      |ve-insp-to-back = Úplně dozadu
      |ve-insp-position = Poloha a velikost (mm)
      |ve-insp-x = X
      |ve-insp-y = Y
      |ve-insp-w = Šířka
      |ve-insp-h = Výška
      |ve-insp-rotation = Otočení °
      |ve-insp-opacity = Krytí
      |ve-insp-text = Text
      |ve-insp-image = Fotka
      |ve-insp-shape = Tvar
      |ve-insp-clipart = Ozdoba
      |ve-insp-calendar = Kalendárium
      |ve-insp-locked-hint = Prvek je zamčený. Odemkněte ho, chcete-li jím pohnout nebo ho upravit.
      |ve-insp-multi = Výběr
      |ve-insp-multi-count = Vybraných prvků: { $count }
      |ve-insp-font = Písmo
      |ve-insp-size = Velikost (pt)
      |ve-insp-line-height = Řádkování
      |ve-insp-valign = Svislé zarovnání
      |ve-insp-letter-spacing = Prostrkání
      |ve-insp-color = Barva
      |ve-insp-image-empty = Rámeček je prázdný. Nahrajte fotku nebo ji vyberte v panelu Fotky.
      |ve-insp-image-remove = Odebrat fotku
      |ve-insp-zoom = Přiblížení
      |ve-insp-focus-x = Ořez vodorovně
      |ve-insp-focus-y = Ořez svisle
      |ve-insp-filter = Filtr
      |ve-insp-radius = Zaoblené rohy
      |ve-insp-border = Rámeček
      |ve-insp-border-width = Šířka rámečku
      |ve-insp-fill = Výplň
      |
      |ve-cal-settings = Kalendář
      |ve-cal-start-month = Počáteční měsíc
      |ve-cal-month = Měsíc
      |ve-cal-year = Rok
      |ve-cal-language = Jazyk
      |ve-cal-week-start = Týden začíná
      |ve-cal-accent = Barva nadpisu
      |ve-cal-weekend = Barva víkendů a svátků
      |
      |ve-fact-format = Čistý formát
      |ve-fact-bleed = Spadávka
      |ve-fact-safe = Bezpečný okraj
      |ve-fact-pages = Stran
      |ve-legend-bleed = Spadávka — odřízne se
      |ve-legend-safe = Text držte uvnitř
      |
      |ve-checks-title = Kontrola tisku
      |ve-checks-ok = Vše vypadá připravené k tisku.
      |ve-issue-very-low-res = Příliš nízké rozlišení fotky ({ $dpi } DPI) — vytiskne se rozmazaně
      |ve-issue-low-res = Nízké rozlišení fotky ({ $dpi } DPI)
      |ve-issue-empty-slot = Prázdný rámeček na fotku
      |ve-issue-safe-area = Text je blízko okraje a může být oříznut
      |ve-issue-bleed = Roztáhněte až ke spadávce, aby nevznikly bílé okraje
      |ve-issue-binding = Zasahuje do otvorů pro vazbu
      |ve-issue-text-small = Velmi malé písmo ({ $size } pt)
      |ve-issue-empty-text = Prázdné textové pole
      |ve-issue-missing-image = Fotka chybí — nahrajte ji znovu
      |ve-issue-not-uploaded = Fotka je uložená jen v tomto zařízení
      |
      |ve-review-title = Zkontrolujte svůj návrh
      |ve-review-subtitle = Takto budou stránky vypadat po oříznutí. Zkontrolujte prosím pravopis, data a fotky.
      |ve-review-colour-note = Obrazovka zobrazuje barvy RGB, tisk používá CMYK, takže velmi syté barvy mohou vyjít o něco jemněji.
      |ve-review-download = Stáhnout náhled
      |ve-review-back = Pokračovat v úpravách
      |ve-review-confirm-anyway = Přesto použít
      |
      |ve-home-title = Návrhové studio
      |ve-home-subtitle = Navrhněte vizitky, plakáty, kalendáře a fotoknihy přímo v prohlížeči.
      |ve-home-subtitle-product = Navrhněte: { $product }
      |ve-home-format = Čistý formát { $w } × { $h } mm a { $bleed } mm spadávka na každé straně.
      |ve-home-continue = Pokračovat v návrhu
      |ve-home-my-designs = Moje návrhy
      |ve-home-local-note = Návrhy se automaticky ukládají v tomto zařízení.
      |ve-home-new = Nový návrh
      |ve-home-blank = Prázdný
      |ve-home-blank-hint = Začít s prázdnou stránkou
      |
      |ve-toast-not-found = Tento návrh už v zařízení není.
      |ve-toast-page-limit = Tento produkt další změnu počtu stran nedovoluje.
      |ve-toast-upload-failed = Fotku se nepodařilo načíst.
      |ve-toast-storage-full = Úložiště prohlížeče je plné — smažte staré návrhy.
      |ve-toast-attached = Návrh byl připojen k objednávce.
      |ve-loading = Načítám návrhové studio…
      |ve-load-failed = Návrhové studio se nepodařilo načíst.
      |""".stripMargin
