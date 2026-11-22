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
      |catalog-btn-details = Details & Presets
      |catalog-btn-configure = Configure
      |catalog-empty-title = No matching products
      |catalog-empty-desc = Try clearing your search term or switching product categories.
      |catalog-clear-filters = Clear filters
      |
      |# Product Detail
      |detail-back = Back to Products
      |detail-presets-title = Popular Presets
      |detail-presets-subtitle = Start from a pre-configured template with live pricing, or customize every parameter in the configurator.
      |detail-custom-cta = Configure from Scratch
      |detail-use-preset = Customize Preset
      |detail-quick-order = Quick Order
      |detail-add-cart = Add to Cart
      |detail-loading-price = Calculating...
      |detail-components-label = Components
      |detail-starting-specs = Default Specs
      |
      |# Configurator
      |cfg-title = Product Configurator
      |cfg-subtitle = Real-time compatibility rules, press-sheet imposition & dynamic pricing
      |cfg-section-product = 1. Product & Preset
      |cfg-section-components = 2. Material, Print & Finishes
      |cfg-section-specs = 3. Specifications & Quantity
      |cfg-section-speed = 4. Production Speed
      |cfg-category-label = Product Category
      |cfg-presets-label = Quick-Start Presets
      |cfg-component-material = Substrate / Material
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
      |catalog-btn-details = Detail a šablony
      |catalog-btn-configure = Konfigurovat
      |catalog-empty-title = Žádné odpovídající produkty
      |catalog-empty-desc = Zkuste upravit hledaný výraz nebo přepnout kategorii.
      |catalog-clear-filters = Vymazat filtry
      |
      |# Detail produktu
      |detail-back = Zpět na produkty
      |detail-presets-title = Oblíbené konfigurace
      |detail-presets-subtitle = Začněte z připravené šablony s živou cenou, nebo si upravte každý parametr v konfigurátoru.
      |detail-custom-cta = Konfigurovat od začátku
      |detail-use-preset = Upravit šablonu
      |detail-quick-order = Rychlá objednávka
      |detail-add-cart = Přidat do košíku
      |detail-loading-price = Počítám cenu...
      |detail-components-label = Komponenty
      |detail-starting-specs = Výchozí specifikace
      |
      |# Konfigurátor
      |cfg-title = Konfigurátor produktu
      |cfg-subtitle = Pravidla kompatibility, archová montáž a dynamická cenotvorba v reálném čase
      |cfg-section-product = 1. Produkt a šablona
      |cfg-section-components = 2. Materiál, tisk a dokončení
      |cfg-section-specs = 3. Rozměry, náklad a vazba
      |cfg-section-speed = 4. Rychlost výroby
      |cfg-category-label = Kategorie produktu
      |cfg-presets-label = Rychlé šablony
      |cfg-component-material = Materiál / substrát
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
