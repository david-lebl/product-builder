package mpbuilder.ui.i18n

import kyo.*
import mpbuilder.shared.Language

/** Reactive multi-language runtime backed by `kyo-i18n` Fluent bundles,
  * kept in lockstep with `mpbuilder.shared.Language` for backend `LocalizedString`s.
  */
final case class AppI18n(
    handle: I18n,
    language: SignalRef[Language],
):
  def setLanguage(lang: Language): Unit < Sync =
    for
      _ <- language.set(lang)
      _ <- handle.setLocale(Translations.toLocale(lang))
    yield ()

  def currentLanguage: Language < Sync = language.get

  def t(key: String, args: I18n.Args = Dict.empty): Signal[String] =
    I18n.t(key, args)

  def now(key: String, args: I18n.Args = Dict.empty): String < Sync =
    I18n.now(key, args)

object Translations:

  val enLocale: Locale = Locale("en")
  val csLocale: Locale = Locale("cs")

  val supportedLocales: Seq[Locale] = Seq(enLocale, csLocale)

  def toLocale(lang: Language): Locale = lang match
    case Language.En => enLocale
    case Language.Cs => csLocale

  def fromLocale(locale: Locale): Language =
    if locale.code == "cs" then Language.Cs else Language.En

  def init(initial: Language): AppI18n < Sync =
    for
      handle <- I18n.initInMemory(bundles, toLocale(initial))
      ref    <- Signal.initRef(initial)
    yield AppI18n(handle, ref)

  lazy val bundles: Dict[Locale, String] = Dict(
    enLocale -> enFtl,
    csLocale -> csFtl,
  )

  val enFtl: String =
    """# Brand & Navigation
      |brand-name = PrintCraft Studio
      |brand-tagline = Custom Print & Packaging
      |nav-catalog = Products
      |nav-builder = Configurator
      |nav-design-studio = Design Studio
      |nav-account = My Account
      |nav-admin = Staff Backoffice
      |nav-cart = Cart
      |nav-sign-in = Sign In
      |nav-sign-out = Sign Out
      |nav-menu = Menu
      |nav-close = Close
      |
      |# Hero & Catalog
      |catalog-hero-badge = Online Print Configurator
      |catalog-hero-title = Precision printing, instant live pricing.
      |catalog-hero-subtitle = Configure business cards, brochures, large-format banners, packaging, and promotional items with real press-sheet imposition and transparent B2B discounts.
      |catalog-hero-cta = Open Configurator
      |catalog-search-placeholder = Search products, formats, or materials...
      |catalog-filter-all = All Products
      |catalog-group-sheet = Sheet Print
      |catalog-group-largeformat = Large Format
      |catalog-group-bound = Bound & Books
      |catalog-group-specialty = Specialty & Packaging
      |catalog-group-promotional = Promotional
      |catalog-presets-count = { $count } presets
      |catalog-presets-one = 1 preset
      |catalog-presets-few = { $count } presets
      |catalog-parts-count = { $count } parts
      |catalog-parts-few = { $count } parts
      |catalog-results-count = { $count } products
      |catalog-hero-feat-pricing = Live pricing
      |catalog-hero-feat-imposition = Press-sheet imposition
      |catalog-hero-feat-b2b = B2B discounts
      |catalog-hero-browse = Browse products
      |catalog-btn-details = Details & Presets
      |catalog-btn-configure = Configure
      |catalog-empty-title = No matching products
      |catalog-empty-desc = Try clearing your search term or switching product categories.
      |catalog-clear-filters = Clear filters
      |
      |# Product Detail
      |detail-back = Back to Products
      |detail-presets-title = Popular Presets
      |detail-presets-subtitle = Popular starting points with live pricing — order one as is, or open it in the configurator and make it your own.
      |detail-custom-cta = Configure from Scratch
      |detail-quick-order = Quick Order
      |detail-add-cart = Add to Cart
      |detail-loading-price = Calculating...
      |detail-components-label = Components
      |detail-starting-specs = Default Specs
      |detail-not-found-title = Product not found
      |detail-unavailable-title = Product temporarily unavailable
      |detail-feat-live-price = Live pricing
      |detail-feat-preflight = Free file check
      |detail-feat-design-online = Design online
      |detail-choose-preset = Choose a configuration
      |detail-no-presets = This product is configured individually — set every parameter in the configurator.
      |detail-dispatch-note = Standard production, dispatched in 3–5 business days. Express available in the configurator.
      |detail-design-studio-cta = Design it in Design Studio
      |detail-trust-price-title = Transparent pricing
      |detail-trust-price-desc = Live quotes from real press-sheet imposition
      |detail-trust-proof-title = Prepress check
      |detail-trust-proof-desc = Every file is preflighted before printing
      |detail-trust-ship-title = Fast dispatch
      |detail-trust-ship-desc = Economy, standard or express production
      |detail-trust-b2b-title = B2B discounts
      |detail-trust-b2b-desc = Agreement pricing for business accounts
      |detail-about-title = About this product
      |detail-bullet-materials = Choose from curated papers and substrates with professional finishes.
      |detail-bullet-artwork = Upload print-ready PDFs or design your artwork online in the Design Studio.
      |detail-bullet-imposition = Prices are calculated from the real sheet layout — no hidden surcharges.
      |detail-select = Select
      |detail-selected = Selected
      |detail-specs-title = Specifications
      |detail-spec-printing = Printing methods
      |detail-spec-bleed = Bleed
      |detail-how-title = How it works
      |detail-make-own-title = Make it your own
      |detail-configure-title = Configure your own
      |detail-configure-desc = Choose format, paper, colours, finishes and quantity — the price updates live with every change.
      |detail-configure-hint = The configurator opens pre-filled with “{ $preset }”.
      |detail-start-blank = Start blank instead
      |detail-studio-title = Design it online
      |detail-studio-desc = Lay out your artwork in the Design Studio in the exact print format — templates, photos and bleed guides included.
      |detail-from-price = From { $price }
      |detail-from-price-note = incl. VAT · final price follows your configuration
      |detail-quick-start-title = Quick start: ready-made presets
      |detail-quantity-label = Quantity
      |detail-quantity-reset = Reset to preset
      |detail-quick-start-desc = Not sure where to begin? Pick a popular configuration, set the quantity and order it right away.
      |detail-how-1 = Configure every parameter yourself — or start from a ready-made preset.
      |detail-how-2 = Upload your artwork or design it in the Design Studio.
      |detail-how-3 = We check your files and send a proof if needed.
      |detail-how-4 = Your order is printed, finished and dispatched.
      |studio-eyebrow = New · Design Studio
      |studio-title = No artwork? Design it right here.
      |studio-subtitle = Our online Design Studio lets you create business cards, flyers, posters, calendars and photo books in the browser — print-ready, with the exact format of your order.
      |studio-feat-templates = Ready-made templates
      |studio-feat-photos = Your own photos & text
      |studio-feat-bleed = Correct trim & bleed
      |studio-feat-preflight = Automatic print check
      |studio-open = Open Design Studio
      |catalog-hero-studio-cta = Design Studio
      |catalog-hero-feat-studio = Design online
      |
      |# Configurator
      |cfg-title = Product Configurator
      |cfg-subtitle = Real-time compatibility rules, press-sheet imposition & dynamic pricing
      |cfg-section-product = 1. Product & Preset
      |cfg-section-components = 2. Material, Print & Finishes
      |cfg-section-specs = 3. Specifications & Quantity
      |cfg-section-speed = 4. Production Speed
      |cfg-section-artwork = 5. Print Artwork (Optional)
      |cfg-artwork-subtitle = Drag & drop your print files now to run automatic preflight, or attach them later in Order History before acceptance.
      |cfg-production-note-label = Instructions for production (optional)
      |cfg-production-note-placeholder = e.g. which side is the front, colour to match, how to cut or pack…
      |cfg-production-note-hint = Our prepress team reads this before printing. You can still change it in your orders until we accept the order.
      |note-production-label = Production instructions
      |note-production-add = + Add production instructions
      |note-production-edit = Edit
      |note-production-save = Save
      |note-production-cancel = Cancel
      |note-production-saved = Production instructions saved.
      |note-production-badge = Instructions attached
      |cfg-artwork-attached-badge = ✓ { $count } artwork attached
      |cfg-artwork-none-badge = No artwork yet — upload after order
      |cfg-artwork-preview = Preview
      |cfg-artwork-remove = Remove
      |cfg-artwork-replace = Replace file
      |cfg-artwork-uploaded-toast = Print artwork attached and checked
      |cfg-design-online-title = No print file? Design it online
      |cfg-design-online-hint = Templates, your photos and live print checks — saved automatically on this device.
      |cfg-design-online = Design online
      |cfg-design-edit-title = Designed in the Design Studio
      |cfg-design-edit = Edit design
      |cfg-design-attached-toast = Your design was attached to this product
      |cfg-design-loading = Opening the Design Studio…
      |cfg-design-attach-failed = The design could not be attached — please try again
      |cfg-artwork-removed-toast = Draft artwork removed
      |cfg-artwork-link-copied = Artwork URL copied to clipboard
      |cfg-category-label = Product Category
      |cfg-presets-label = Quick-Start Presets
      |cfg-component-material = Substrate / Material
      |cfg-material-search-placeholder = Search material, surface or weight (e.g. matte 300, 200-350)…
      |cfg-material-choose = Choose a material
      |cfg-material-no-results = No material matches your search.
      |cfg-material-clear-search = Clear search
      |cfg-material-keyboard-hint = ↑↓ navigate · Enter select · Esc close
      |cfg-material-count = { $shown } of { $total } materials
      |cfg-material-weight-variants = Also available in
      |cfg-component-print-method = Printing Method
      |cfg-component-ink = Ink Configuration
      |cfg-component-finishes = Finishes & Post-Press
      |cfg-no-finishes = No compatible finishes available for this material and print method.
      |cfg-non-printable = Non-printable hardware / structural component
      |cfg-finish-side = Applied Side
      |cfg-finish-side-front = Front
      |cfg-finish-side-back = Back
      |cfg-finish-side-both = Both Sides
      |cfg-param-corners = Corners
      |cfg-param-radius = Radius (mm)
      |cfg-param-foil-color = Foil Color
      |cfg-param-spacing = Spacing (mm)
      |cfg-param-pitch = Pitch (mm)
      |cfg-param-length = Length (m)
      |cfg-param-creases = Creases
      |cfg-param-max-hint = max { $max }
      |cfg-dimensions-label = Dimensions
      |cfg-size-presets = Standard Sizes
      |cfg-custom-option = Custom
      |cfg-width-mm = Width (mm)
      |cfg-height-mm = Height (mm)
      |cfg-length-mm = Length (mm)
      |cfg-diameter-mm = Diameter (mm)
      |cfg-fold-type = Fold Type
      |cfg-quantity-label = Quantity (pcs)
      |cfg-versions-label = Artwork Versions
      |cfg-versions-hint = Multiple designs sharing the same print run
      |cfg-orientation-label = Orientation
      |cfg-orientation-portrait = Portrait
      |cfg-orientation-landscape = Landscape
      |cfg-binding-method = Binding Method
      |cfg-binding-pages = Total Pages
      |cfg-binding-self-cover = Self-cover (same stock as interior)
      |cfg-delivery-format = Delivery Format
      |cfg-speed-express = Express
      |cfg-speed-standard = Standard
      |cfg-speed-economy = Economy
      |cfg-speed-express-desc = Priority queue, fastest turnaround
      |cfg-speed-standard-desc = Balanced turnaround & price
      |cfg-speed-economy-desc = Extended production window, best value
      |cfg-speed-unavailable = Unavailable
      |cfg-quote-title = Live Price Summary
      |cfg-quote-calculating = Calculating quote...
      |cfg-quote-net = Net Total (excl. VAT)
      |cfg-quote-vat = VAT ({ $rate }%)
      |cfg-quote-gross = Total incl. VAT
      |cfg-quote-unit = Per piece (net)
      |cfg-quote-walkin = Walk-in price
      |cfg-quote-savings = Your B2B discount saves { $amount } ({ $pct }%)
      |cfg-quote-volume-discount = Volume multiplier: { $mult }x
      |cfg-quote-breakdown = Price Breakdown
      |cfg-quote-imposition = Press & Weight Details
      |cfg-quote-weight = Est. total weight
      |cfg-quote-sheets = Press sheets
      |cfg-quote-pcs-sheet = pcs/sheet
      |cfg-quote-roll = Roll usage
      |cfg-warnings-title = Prepress Advisories
      |cfg-errors-title = Configuration Issues
      |cfg-btn-add-cart = Add to Cart
      |cfg-btn-quick-order = Quick Order
      |cfg-btn-email-order = Request Quote / Order by Email
      |cfg-added-toast = Added to your shopping cart
      |cfg-btn-adding = Adding…
      |cfg-btn-added = Added to cart
      |cfg-dock-add = Add to cart
      |cfg-dock-email = Order by email
      |cfg-dock-unit = { $qty } pcs · { $unit } / pc excl. VAT
      |cfg-dock-fix = Check configuration ({ $count })
      |cfg-dock-recap = Your Configuration
      |cfg-dock-recap-product = Product
      |cfg-dock-recap-size = Size
      |cfg-dock-recap-quantity = Quantity
      |cfg-dock-recap-speed = Production speed
      |cfg-dock-recap-material = Material
      |
      |# Cart Drawer
      |cart-title = Shopping Cart
      |cart-empty-title = Your cart is empty
      |cart-empty-desc = Configure a product or pick a preset from our catalog to get started.
      |cart-browse-cta = Browse Products
      |cart-copies = Copies / Sets
      |cart-remove = Remove
      |cart-clear = Clear Cart
      |cart-promo-label = Promo Code
      |cart-promo-placeholder = Enter code (e.g. SAVE10, FREESHIP)
      |cart-promo-apply = Apply
      |cart-promo-remove = Remove
      |cart-promo-try = Try demo codes:
      |cart-subtotal-net = Items Net
      |cart-promo-discount = Promo Discount
      |cart-tax = VAT
      |cart-total-gross = Total incl. VAT
      |cart-checkout-cta = Proceed to Checkout
      |
      |# Checkout
      |checkout-title = Checkout
      |checkout-back-catalog = Continue Shopping
      |checkout-quick-banner = Quick Single-Item Checkout — your shopping cart remains untouched.
      |checkout-step-contact = 1. Contact & Billing Details
      |checkout-login-title = Already have an account?
      |checkout-login-hint = Sign in and we'll fill in your contact and address details.
      |checkout-login-cta = Sign In
      |checkout-logged-in-as = Signed in as { $name } — details filled from your account.
      |checkout-step-delivery = 2. Delivery Method
      |checkout-step-payment = 3. Payment Method
      |checkout-step-note = 4. Promo & Order Notes
      |checkout-full-name = Full Name
      |checkout-email = Email Address
      |checkout-phone = Phone
      |checkout-company = Company Name (optional)
      |checkout-ico = Company Reg. No. (IČO)
      |checkout-dic = VAT ID (DIČ)
      |checkout-street = Street & Number
      |checkout-city = City
      |checkout-postal = Postal Code
      |checkout-country = Country Code
      |checkout-same-shipping = Shipping address same as billing
      |checkout-shipping-title = Shipping Address
      |checkout-ship-to = Ship to
      |checkout-ship-same-billing = Billing address
      |checkout-ship-same-billing-desc = Deliver to the billing address above
      |checkout-ship-new = Different address
      |checkout-ship-new-desc = Enter a one-off shipping address
      |checkout-free-badge = FREE
      |checkout-pickup-badge = Pickup
      |checkout-courier-badge = Courier
      |checkout-pay-qr = Bank Transfer / QR Payment
      |checkout-pay-qr-desc = Instant Czech SPD 1.0 QR code & IBAN instructions
      |checkout-pay-card = Payment Card
      |checkout-pay-card-desc = Online card payment
      |checkout-pay-invoice = Invoice on Account
      |checkout-pay-invoice-desc = Deferred B2B invoice payment
      |checkout-pay-invoice-days = Net { $days } days credit terms approved
      |checkout-note-placeholder = Delivery instructions, artwork notes, or PO reference...
      |checkout-summary-title = Order Summary
      |checkout-shipping = Shipping (net)
      |checkout-place-order = Place Order
      |checkout-placing = Placing Order...
      |checkout-validation-error = Please complete all required contact and address fields.
      |checkout-consent-prefix = I have read and agree to the
      |checkout-consent-terms-link = Terms & Conditions
      |checkout-consent-middle = and consent to the processing of my personal data under the
      |checkout-consent-privacy-link = Privacy Policy (GDPR)
      |checkout-consent-suffix = .
      |checkout-consent-required = Please accept the Terms & Conditions and the Privacy Policy to place your order.
      |
      |# Order Confirmation
      |confirm-badge = Order Confirmed
      |confirm-title = Thank you for your order!
      |confirm-order-number = Order Number
      |confirm-status = Status
      |confirm-payment-title = Payment Instructions
      |confirm-iban = IBAN
      |confirm-bic = BIC / SWIFT
      |confirm-holder = Account Holder
      |confirm-vs = Variable Symbol
      |confirm-amount = Amount Due
      |confirm-qr-title = Scan to Pay (SPD 1.0)
      |confirm-qr-payload = QR Payment Payload
      |confirm-invoice-title = B2B Invoice Scheduled
      |confirm-invoice-desc = Your order is confirmed for production. An invoice (ref. { $ref }) is payable within { $days } days.
      |confirm-card-title = Online Card Payment
      |confirm-items-title = Ordered Items
      |confirm-btn-orders = View My Orders
      |confirm-btn-new = Configure Another Product
      |
      |# Authentication Modal
      |auth-title = Sign In
      |auth-subtitle = Unlock your B2B pricing tier, custom volume curves, and invoice terms.
      |auth-tab-otp = Passwordless OTP
      |auth-tab-password = Email & Password
      |auth-id-type = Identifier Type
      |auth-id-email = Email
      |auth-id-ico = Company Reg. No. (IČO)
      |auth-id-dic = VAT ID (DIČ)
      |auth-id-placeholder = Enter Email, IČO (e.g. 12345678), or DIČ
      |auth-send-otp = Send 6-Digit Code
      |auth-otp-sent = We sent a 6-digit verification code to { $dest }.
      |auth-otp-code = 6-Digit Code
      |auth-otp-verify = Verify & Sign In
      |auth-otp-back = Change Identifier
      |auth-otp-demo-title = Static Demo OTP Inbox
      |auth-otp-demo-hint = Generated code: { $code }
      |auth-otp-demo-fill = Use Demo Code
      |auth-email-label = Email Address
      |auth-password-label = Password
      |auth-submit-password = Sign In
      |auth-demo-accounts = Quick Demo Accounts (password: demo1234)
      |auth-welcome-toast = Signed in as { $name }
      |auth-signed-out-toast = You have been signed out
      |
      |# Account & Order History
      |account-title = My Account
      |account-profile-title = Organization & Billing Profile
      |account-tier = Pricing Tier
      |account-payment-terms = Payment Terms
      |account-prepaid = Prepaid Only
      |account-net-invoice = Net { $days } Days (Limit: { $limit })
      |account-orders-title = Order History
      |account-orders-empty = You haven't placed any orders in this session yet.
      |account-orders-cta = Start Configuring
      |account-not-signed-in = Sign in to view your B2B profile and order history.
      |account-addresses-title = Billing & Delivery Addresses
      |account-edit-addresses = Edit Addresses
      |account-save-addresses = Save Addresses
      |account-cancel-edit = Cancel
      |account-billing-address = Billing Address
      |account-delivery-addresses = Delivery Addresses
      |account-default-badge = Default
      |account-set-default = Set as Default
      |account-add-delivery = + Add Delivery Address
      |account-remove-address = Remove
      |account-addresses-saved = Addresses updated successfully
      |account-view-payment = View Payment Options
      |account-payment-modal-title = Payment Options
      |account-artwork-title = Artwork & Preflight
      |account-artwork-reupload = Re-upload Corrected File (300 DPI CMYK)
      |account-artwork-reuploaded = Corrected artwork uploaded and sent for preflight!
      |account-artwork-uploaded = Print artwork uploaded and checked!
      |account-artwork-upload-prompt = Upload print artwork for { $component }
      |account-artwork-replace-prompt = Replace artwork for { $component }
      |account-artwork-locked = Artwork uploads locked — order has already been accepted
      |account-artwork-no-files = No artwork attached to this item yet.
      |account-close = Close
      |
      |# Standalone Calculator
      |calc-title = Instant Print Price Calculator
      |calc-disclaimer = Prices and turnaround windows are indicative. Final completion dates are confirmed after artwork preflight.
      |calc-speed-express-range = 1–2 business days (indicative)
      |calc-speed-standard-range = 3–5 business days (indicative)
      |calc-speed-economy-range = 6–10 business days (indicative)
      |calc-email-title = Order / Quote Request by Email
      |calc-email-recipient = Recipient Email
      |calc-email-your-name = Your Name
      |calc-email-your-email = Your Email
      |calc-email-note = Additional Notes
      |calc-email-preview = Generated Specification Summary
      |calc-email-open-client = Open in Email Client
      |calc-email-copy = Copy Summary
      |calc-email-copied = Copied to clipboard!
      |
      |# Footer
      |footer-static-note = Running 100% client-side with Kyo UI + modular Scala 3 backend sample runtime (no server required).
      |""".stripMargin

  val csFtl: String =
    """# Značka & Navigace
      |brand-name = PrintCraft Studio
      |brand-tagline = Zakázkový tisk a obaly
      |nav-catalog = Produkty
      |nav-builder = Konfigurátor
      |nav-design-studio = Design Studio
      |nav-account = Můj účet
      |nav-admin = Administrace
      |nav-cart = Košík
      |nav-sign-in = Přihlásit se
      |nav-sign-out = Odhlásit se
      |nav-menu = Menu
      |nav-close = Zavřít
      |
      |# Hero & Katalog
      |catalog-hero-badge = Online tiskový konfigurátor
      |catalog-hero-title = Precizní tisk, okamžitá kalkulace ceny.
      |catalog-hero-subtitle = Nakonfigurujte vizitky, brožury, velkoformátové bannery, obaly i reklamní předměty se skutečnou archovou montáží a transparentními B2B slevami.
      |catalog-hero-cta = Otevřít konfigurátor
      |catalog-search-placeholder = Hledat produkty, formáty nebo materiály...
      |catalog-filter-all = Všechny produkty
      |catalog-group-sheet = Archový tisk
      |catalog-group-largeformat = Velkoformát
      |catalog-group-bound = Vazby a brožury
      |catalog-group-specialty = Speciální a obaly
      |catalog-group-promotional = Reklamní předměty
      |catalog-presets-count = { $count } šablon
      |catalog-presets-one = 1 šablona
      |catalog-presets-few = { $count } šablony
      |catalog-parts-count = { $count } částí
      |catalog-parts-few = { $count } části
      |catalog-results-count = Produktů: { $count }
      |catalog-hero-feat-pricing = Živá kalkulace
      |catalog-hero-feat-imposition = Archová montáž
      |catalog-hero-feat-b2b = B2B slevy
      |catalog-hero-browse = Procházet produkty
      |catalog-btn-details = Detail a šablony
      |catalog-btn-configure = Konfigurovat
      |catalog-empty-title = Žádné odpovídající produkty
      |catalog-empty-desc = Zkuste upravit hledaný výraz nebo přepnout kategorii.
      |catalog-clear-filters = Vymazat filtry
      |
      |# Detail produktu
      |detail-back = Zpět na produkty
      |detail-presets-title = Oblíbené konfigurace
      |detail-presets-subtitle = Oblíbené výchozí konfigurace s živou cenou — objednejte je rovnou, nebo je v konfigurátoru upravte podle sebe.
      |detail-custom-cta = Konfigurovat od začátku
      |detail-quick-order = Rychlá objednávka
      |detail-add-cart = Přidat do košíku
      |detail-loading-price = Počítám cenu...
      |detail-components-label = Komponenty
      |detail-starting-specs = Výchozí specifikace
      |detail-not-found-title = Produkt nenalezen
      |detail-unavailable-title = Produkt je dočasně nedostupný
      |detail-feat-live-price = Živá cena
      |detail-feat-preflight = Kontrola dat zdarma
      |detail-feat-design-online = Návrh online
      |detail-choose-preset = Vyberte konfiguraci
      |detail-no-presets = Tento produkt se konfiguruje individuálně — nastavte všechny parametry v konfigurátoru.
      |detail-dispatch-note = Standardní výroba, expedice za 3–5 pracovních dní. Expres je k dispozici v konfigurátoru.
      |detail-design-studio-cta = Navrhnout v Design Studiu
      |detail-trust-price-title = Transparentní ceny
      |detail-trust-price-desc = Živá kalkulace ze skutečné archové montáže
      |detail-trust-proof-title = Předtisková kontrola
      |detail-trust-proof-desc = Každý soubor před tiskem zkontrolujeme
      |detail-trust-ship-title = Rychlá expedice
      |detail-trust-ship-desc = Ekonomická, standardní nebo expresní výroba
      |detail-trust-b2b-title = B2B slevy
      |detail-trust-b2b-desc = Smluvní ceny pro firemní účty
      |detail-about-title = O produktu
      |detail-bullet-materials = Vybírejte z ověřených papírů a materiálů s profesionálními úpravami.
      |detail-bullet-artwork = Nahrajte tiskové PDF, nebo si podklady navrhněte online v Design Studiu.
      |detail-bullet-imposition = Cena se počítá ze skutečného rozložení na archu — bez skrytých příplatků.
      |detail-select = Vybrat
      |detail-selected = Vybráno
      |detail-specs-title = Specifikace
      |detail-spec-printing = Tiskové technologie
      |detail-spec-bleed = Spadávka
      |detail-how-title = Jak to funguje
      |detail-make-own-title = Vytvořte si vlastní
      |detail-configure-title = Vlastní konfigurace
      |detail-configure-desc = Zvolte formát, papír, barevnost, úpravy a náklad — cena se přepočítá při každé změně.
      |detail-configure-hint = Konfigurátor se otevře s předvyplněnou konfigurací „{ $preset }“.
      |detail-start-blank = Začít od nuly
      |detail-studio-title = Navrhněte online
      |detail-studio-desc = Připravte si podklady v Design Studiu v přesném tiskovém formátu — se šablonami, fotkami i vodítky spadávky.
      |detail-from-price = Od { $price }
      |detail-from-price-note = vč. DPH · konečná cena podle vaší konfigurace
      |detail-quick-start-title = Rychlý start: připravené konfigurace
      |detail-quantity-label = Počet kusů
      |detail-quantity-reset = Vrátit výchozí
      |detail-quick-start-desc = Nevíte, jak začít? Vyberte oblíbenou konfiguraci, nastavte počet kusů a objednejte ji rovnou.
      |detail-how-1 = Nastavte si každý parametr sami — nebo začněte z připravené konfigurace.
      |detail-how-2 = Nahrajte podklady, nebo je navrhněte v Design Studiu.
      |detail-how-3 = Soubory zkontrolujeme a v případě potřeby pošleme náhled.
      |detail-how-4 = Zakázku vytiskneme, dokončíme a odešleme.
      |studio-eyebrow = Novinka · Design Studio
      |studio-title = Nemáte podklady? Navrhněte je přímo u nás.
      |studio-subtitle = V online Design Studiu vytvoříte vizitky, letáky, plakáty, kalendáře i fotoknihy přímo v prohlížeči — připravené k tisku a v přesném formátu vaší objednávky.
      |studio-feat-templates = Hotové šablony
      |studio-feat-photos = Vlastní fotky a texty
      |studio-feat-bleed = Správný ořez a spadávka
      |studio-feat-preflight = Automatická kontrola tisku
      |studio-open = Otevřít Design Studio
      |catalog-hero-studio-cta = Design Studio
      |catalog-hero-feat-studio = Návrh online
      |
      |# Konfigurátor
      |cfg-title = Konfigurátor produktu
      |cfg-subtitle = Pravidla kompatibility, archová montáž a dynamická cenotvorba v reálném čase
      |cfg-section-product = 1. Produkt a šablona
      |cfg-section-components = 2. Materiál, tisk a dokončení
      |cfg-section-specs = 3. Rozměry, náklad a vazba
      |cfg-section-speed = 4. Rychlost výroby
      |cfg-section-artwork = 5. Tisková data (volitelné)
      |cfg-artwork-subtitle = Přetáhněte tisková data rovnou pro automatický preflight, nebo je nahrajte později v historii objednávek před přijetím zakázky.
      |cfg-production-note-label = Pokyny pro výrobu (volitelné)
      |cfg-production-note-placeholder = např. která strana je přední, barva ke shodě, jak řezat nebo balit…
      |cfg-production-note-hint = Naše předtisková příprava si je přečte před tiskem. Upravit je můžete v objednávkách, dokud zakázku nepřijmeme.
      |note-production-label = Pokyny pro výrobu
      |note-production-add = + Přidat pokyny pro výrobu
      |note-production-edit = Upravit
      |note-production-save = Uložit
      |note-production-cancel = Zrušit
      |note-production-saved = Pokyny pro výrobu uloženy.
      |note-production-badge = Pokyny přiloženy
      |cfg-artwork-attached-badge = ✓ Přiloženo souborů: { $count }
      |cfg-artwork-none-badge = Zatím bez tiskových dat — lze nahrát po objednání
      |cfg-artwork-preview = Náhled
      |cfg-artwork-remove = Odebrat
      |cfg-artwork-replace = Nahradit soubor
      |cfg-artwork-uploaded-toast = Tisková data byla přiložena a zkontrolována
      |cfg-design-online-title = Nemáte tisková data? Navrhněte je online
      |cfg-design-online-hint = Šablony, vaše fotky a kontrola tisku v reálném čase — vše se ukládá v tomto zařízení.
      |cfg-design-online = Navrhnout online
      |cfg-design-edit-title = Navrženo v návrhovém studiu
      |cfg-design-edit = Upravit návrh
      |cfg-design-attached-toast = Váš návrh byl připojen k produktu
      |cfg-design-loading = Otevírám návrhové studio…
      |cfg-design-attach-failed = Návrh se nepodařilo připojit — zkuste to prosím znovu
      |cfg-artwork-removed-toast = Rozpracovaný soubor odebrán
      |cfg-artwork-link-copied = Odkaz na soubor zkopírován do schránky
      |cfg-category-label = Kategorie produktu
      |cfg-presets-label = Rychlé šablony
      |cfg-component-material = Materiál / substrát
      |cfg-material-search-placeholder = Hledat materiál, povrch nebo gramáž (např. matný 300, 200-350)…
      |cfg-material-choose = Vyberte materiál
      |cfg-material-no-results = Hledání neodpovídá žádný materiál.
      |cfg-material-clear-search = Vymazat hledání
      |cfg-material-keyboard-hint = ↑↓ pohyb · Enter výběr · Esc zavřít
      |cfg-material-count = { $shown } z { $total } materiálů
      |cfg-material-weight-variants = K dispozici také v gramáži
      |cfg-component-print-method = Technologie tisku
      |cfg-component-ink = Barevnost tisku
      |cfg-component-finishes = Dokončovací zpracování
      |cfg-no-finishes = Pro zvolený materiál a technologii tisku nejsou k dispozici žádné povrchové úpravy.
      |cfg-non-printable = Netisknutelný konstrukční / hardwarový díl
      |cfg-finish-side = Strana aplikace
      |cfg-finish-side-front = Přední
      |cfg-finish-side-back = Zadní
      |cfg-finish-side-both = Oboustranně
      |cfg-param-corners = Počet rohů
      |cfg-param-radius = Poloměr (mm)
      |cfg-param-foil-color = Barva fólie
      |cfg-param-spacing = Rozteč ok (mm)
      |cfg-param-pitch = Rozteč perforace (mm)
      |cfg-param-length = Délka (m)
      |cfg-param-creases = Počet bigů
      |cfg-param-max-hint = max { $max }
      |cfg-dimensions-label = Rozměry
      |cfg-size-presets = Standardní formáty
      |cfg-custom-option = Vlastní
      |cfg-width-mm = Šířka (mm)
      |cfg-height-mm = Výška (mm)
      |cfg-length-mm = Délka (mm)
      |cfg-diameter-mm = Průměr (mm)
      |cfg-fold-type = Typ skladu
      |cfg-quantity-label = Náklad (ks)
      |cfg-versions-label = Počet mutací (verzí)
      |cfg-versions-hint = Více grafických verzí v jedné zakázce
      |cfg-orientation-label = Orientace
      |cfg-orientation-portrait = Na výšku
      |cfg-orientation-landscape = Na šířku
      |cfg-binding-method = Typ vazby
      |cfg-binding-pages = Počet stran
      |cfg-binding-self-cover = Obálka ze stejného papíru (self-cover)
      |cfg-delivery-format = Formát dodání
      |cfg-speed-express = Expresní
      |cfg-speed-standard = Standardní
      |cfg-speed-economy = Ekonomická
      |cfg-speed-express-desc = Prioritní fronta, nejrychlejší dodání
      |cfg-speed-standard-desc = Vyvážený poměr ceny a termínu
      |cfg-speed-economy-desc = Delší výrobní okno, nejnižší cena
      |cfg-speed-unavailable = Nedostupné
      |cfg-quote-title = Živá kalkulace ceny
      |cfg-quote-calculating = Přepočítávám...
      |cfg-quote-net = Celkem bez DPH
      |cfg-quote-vat = DPH ({ $rate } %)
      |cfg-quote-gross = Celkem s DPH
      |cfg-quote-unit = Cena za kus (bez DPH)
      |cfg-quote-walkin = Běžná cena
      |cfg-quote-savings = Vaše B2B sleva šetří { $amount } ({ $pct } %)
      |cfg-quote-volume-discount = Množstevní koeficient: { $mult }x
      |cfg-quote-breakdown = Rozpis ceny
      |cfg-quote-imposition = Archová montáž a hmotnost
      |cfg-quote-weight = Odhad hmotnosti
      |cfg-quote-sheets = Tiskových archů
      |cfg-quote-pcs-sheet = ks/arch
      |cfg-quote-roll = Spotřeba role
      |cfg-warnings-title = Doporučení předtiskové přípravy
      |cfg-errors-title = Problémy v konfiguraci
      |cfg-btn-add-cart = Přidat do košíku
      |cfg-btn-quick-order = Rychlá objednávka
      |cfg-btn-email-order = Poptat / objednat e-mailem
      |cfg-added-toast = Položka byla přidána do košíku
      |cfg-btn-adding = Přidávám…
      |cfg-btn-added = Přidáno do košíku
      |cfg-dock-add = Do košíku
      |cfg-dock-email = Objednat e-mailem
      |cfg-dock-unit = { $qty } ks · { $unit } / ks bez DPH
      |cfg-dock-fix = Zkontrolujte konfiguraci ({ $count })
      |cfg-dock-recap = Vaše konfigurace
      |cfg-dock-recap-product = Produkt
      |cfg-dock-recap-size = Rozměr
      |cfg-dock-recap-quantity = Množství
      |cfg-dock-recap-speed = Rychlost výroby
      |cfg-dock-recap-material = Materiál
      |
      |# Košík
      |cart-title = Nákupní košík
      |cart-empty-title = Váš košík je prázdný
      |cart-empty-desc = Nakonfigurujte produkt nebo vyberte šablonu z katalogu.
      |cart-browse-cta = Prohlédnout produkty
      |cart-copies = Počet sad
      |cart-remove = Odebrat
      |cart-clear = Vyprázdnit košík
      |cart-promo-label = Slevový kód
      |cart-promo-placeholder = Zadejte kód (např. SAVE10, FREESHIP)
      |cart-promo-apply = Použít
      |cart-promo-remove = Odebrat
      |cart-promo-try = Vyzkoušejte demo kódy:
      |cart-subtotal-net = Položky bez DPH
      |cart-promo-discount = Sleva (kód)
      |cart-tax = DPH
      |cart-total-gross = Celkem s DPH
      |cart-checkout-cta = Přejít k pokladně
      |
      |# Pokladna
      |checkout-title = Dokončení objednávky
      |checkout-back-catalog = Pokračovat v nákupu
      |checkout-quick-banner = Rychlá objednávka jedné položky — váš košík zůstane zachován.
      |checkout-step-contact = 1. Kontaktní a fakturační údaje
      |checkout-login-title = Už u nás máte účet?
      |checkout-login-hint = Přihlaste se a kontaktní údaje i adresu vyplníme za vás.
      |checkout-login-cta = Přihlásit se
      |checkout-logged-in-as = Přihlášen(a) jako { $name } — údaje jsme vyplnili z vašeho účtu.
      |checkout-step-delivery = 2. Způsob doručení
      |checkout-step-payment = 3. Způsob platby
      |checkout-step-note = 4. Slevový kód a poznámka
      |checkout-full-name = Jméno a příjmení
      |checkout-email = E-mailová adresa
      |checkout-phone = Telefon
      |checkout-company = Název firmy (volitelné)
      |checkout-ico = IČO
      |checkout-dic = DIČ
      |checkout-street = Ulice a číslo popisné
      |checkout-city = Město
      |checkout-postal = PSČ
      |checkout-country = Kód země
      |checkout-same-shipping = Doručovací adresa je stejná jako fakturační
      |checkout-shipping-title = Doručovací adresa
      |checkout-ship-to = Doručit na
      |checkout-ship-same-billing = Fakturační adresa
      |checkout-ship-same-billing-desc = Doručit na fakturační adresu výše
      |checkout-ship-new = Jiná adresa
      |checkout-ship-new-desc = Zadat jednorázovou doručovací adresu
      |checkout-free-badge = ZDARMA
      |checkout-pickup-badge = Osobní odběr
      |checkout-courier-badge = Kurýr
      |checkout-pay-qr = Bankovní převod / QR platba
      |checkout-pay-qr-desc = Okamžitý QR kód (SPD 1.0) a platební údaje
      |checkout-pay-card = Platební karta
      |checkout-pay-card-desc = Online platba kartou
      |checkout-pay-invoice = Faktura se splatností
      |checkout-pay-invoice-desc = B2B platba na fakturu
      |checkout-pay-invoice-days = Schválená splatnost { $days } dní
      |checkout-note-placeholder = Pokyny pro dopravce, poznámka k datům nebo číslo objednávky...
      |checkout-summary-title = Shrnutí objednávky
      |checkout-shipping = Doprava (bez DPH)
      |checkout-place-order = Odeslat objednávku
      |checkout-placing = Odesílám objednávku...
      |checkout-validation-error = Vyplňte prosím všechna povinná kontaktní a adresní pole.
      |checkout-consent-prefix = Přečetl(a) jsem si a souhlasím s
      |checkout-consent-terms-link = obchodními podmínkami
      |checkout-consent-middle = a se zpracováním osobních údajů podle
      |checkout-consent-privacy-link = zásad ochrany osobních údajů (GDPR)
      |checkout-consent-suffix = .
      |checkout-consent-required = Pro odeslání objednávky prosím potvrďte souhlas s obchodními podmínkami a zásadami ochrany osobních údajů.
      |
      |# Potvrzení objednávky
      |confirm-badge = Objednávka přijata
      |confirm-title = Děkujeme za vaši objednávku!
      |confirm-order-number = Číslo objednávky
      |confirm-status = Stav
      |confirm-payment-title = Platební instrukce
      |confirm-iban = IBAN
      |confirm-bic = BIC / SWIFT
      |confirm-holder = Majitel účtu
      |confirm-vs = Variabilní symbol
      |confirm-amount = Částka k úhradě
      |confirm-qr-title = QR platba (SPD 1.0)
      |confirm-qr-payload = Řetězec QR platby
      |confirm-invoice-title = Faktura na splatnost naplánována
      |confirm-invoice-desc = Objednávka je potvrzena do výroby. Faktura (č. { $ref }) má splatnost { $days } dní.
      |confirm-card-title = Online platba kartou
      |confirm-items-title = Objednané položky
      |confirm-btn-orders = Zobrazit moje objednávky
      |confirm-btn-new = Konfigurovat další produkt
      |
      |# Přihlášení
      |auth-title = Přihlášení
      |auth-subtitle = Získejte přístup k B2B ceníkům, individuálním slevám a platbě na fakturu.
      |auth-tab-otp = Kód bez hesla (OTP)
      |auth-tab-password = E-mail a heslo
      |auth-id-type = Identifikátor
      |auth-id-email = E-mail
      |auth-id-ico = IČO
      |auth-id-dic = DIČ
      |auth-id-placeholder = Zadejte e-mail, IČO (např. 12345678) nebo DIČ
      |auth-send-otp = Poslat 6místný kód
      |auth-otp-sent = 6místný ověřovací kód jsme odeslali na { $dest }.
      |auth-otp-code = 6místný kód
      |auth-otp-verify = Ověřit a přihlásit
      |auth-otp-back = Změnit identifikátor
      |auth-otp-demo-title = Demo OTP schránka
      |auth-otp-demo-hint = Vygenerovaný kód: { $code }
      |auth-otp-demo-fill = Použít demo kód
      |auth-email-label = E-mailová adresa
      |auth-password-label = Heslo
      |auth-submit-password = Přihlásit se
      |auth-demo-accounts = Rychlé demo účty (heslo: demo1234)
      |auth-welcome-toast = Přihlášeno jako { $name }
      |auth-signed-out-toast = Byl(a) jste odhlášen(a)
      |
      |# Můj účet a historie objednávek
      |account-title = Můj účet
      |account-profile-title = Firemní a fakturační profil
      |account-tier = Cenová úroveň
      |account-payment-terms = Platební podmínky
      |account-prepaid = Platba předem
      |account-net-invoice = Faktura { $days } dní (Limit: { $limit })
      |account-orders-title = Historie objednávek
      |account-orders-empty = V této relaci jste zatím nevytvořil(a) žádnou objednávku.
      |account-orders-cta = Začít konfigurovat
      |account-not-signed-in = Přihlaste se pro zobrazení B2B profilu a historie objednávek.
      |account-addresses-title = Fakturační a doručovací adresy
      |account-edit-addresses = Upravit adresy
      |account-save-addresses = Uložit adresy
      |account-cancel-edit = Zrušit
      |account-billing-address = Fakturační adresa
      |account-delivery-addresses = Doručovací adresy
      |account-default-badge = Výchozí
      |account-set-default = Nastavit jako výchozí
      |account-add-delivery = + Přidat doručovací adresu
      |account-remove-address = Odebrat
      |account-addresses-saved = Adresy byly úspěšně uloženy
      |account-view-payment = Zobrazit možnosti platby
      |account-payment-modal-title = Možnosti platby
      |account-artwork-title = Tisková data a preflight
      |account-artwork-reupload = Nahrát opravený soubor (300 DPI CMYK)
      |account-artwork-reuploaded = Opravená tisková data byla nahrána a odeslána na preflight!
      |account-artwork-uploaded = Tisková data byla nahrána a zkontrolována!
      |account-artwork-upload-prompt = Nahrát tisková data pro { $component }
      |account-artwork-replace-prompt = Nahradit soubor pro { $component }
      |account-artwork-locked = Nahrávání tiskových dat uzamčeno — objednávka již byla přijata
      |account-artwork-no-files = K této položce zatím nejsou přiložena žádná tisková data.
      |account-close = Zavřít
      |
      |# Samostatná kalkulačka
      |calc-title = Online kalkulačka tisku
      |calc-disclaimer = Uvedené ceny a výrobní lhůty jsou orientační. Přesný termín dokončení potvrdíme po kontrole tiskových dat.
      |calc-speed-express-range = 1–2 pracovní dny (orientačně)
      |calc-speed-standard-range = 3–5 pracovních dnů (orientačně)
      |calc-speed-economy-range = 6–10 pracovních dnů (orientačně)
      |calc-email-title = Poptávka / objednávka e-mailem
      |calc-email-recipient = E-mail příjemce
      |calc-email-your-name = Vaše jméno
      |calc-email-your-email = Váš e-mail
      |calc-email-note = Poznámka k zakázce
      |calc-email-preview = Vygenerovaný souhrn specifikace
      |calc-email-open-client = Otevřít v e-mailovém klientu
      |calc-email-copy = Kopírovat souhrn
      |calc-email-copied = Zkopírováno do schránky!
      |
      |# Patička
      |footer-static-note = Běží 100 % na klientu v Kyo UI + modulární Scala 3 sample runtime (bez backendu).
      |""".stripMargin
