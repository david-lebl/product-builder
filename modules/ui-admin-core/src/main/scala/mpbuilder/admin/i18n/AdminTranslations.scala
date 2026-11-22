package mpbuilder.admin.i18n

import kyo.*
import mpbuilder.shared.Language
import mpbuilder.ui.i18n.{AppI18n, Translations}

/** EN & CS Fluent translation bundles for the Backoffice / Administration UI.
  *
  * Composes with `Translations.{enFtl, csFtl}` so a single `AppI18n` instance
  * seamlessly serves both Storefront and Backoffice in unified demo mode as well
  * as the standalone `ui-admin` bundle.
  */
object AdminTranslations:

  def init(initial: Language): AppI18n < Sync =
    for
      handle <- I18n.initInMemory(mergedBundles, Translations.toLocale(initial))
      ref    <- Signal.initRef(initial)
    yield AppI18n(handle, ref)

  lazy val mergedBundles: Dict[Locale, String] = Dict(
    Translations.enLocale -> s"${Translations.enFtl}\n$adminEnFtl",
    Translations.csLocale -> s"${Translations.csFtl}\n$adminCsFtl",
  )

  val adminEnFtl: String =
    """# Backoffice Shell & Navigation
      |adm-brand-subtitle = Staff Backoffice
      |adm-nav-dashboard = Dashboard
      |adm-nav-orders = Order Intake
      |adm-nav-catalog = Catalog
      |adm-nav-pricing = Pricing & Margin
      |adm-nav-customers = Customers
      |adm-nav-settings = Staff & Store Settings
      |adm-back-to-storefront = ← Storefront
      |adm-refresh = Refresh
      |adm-sign-out = Sign Out
      |adm-readonly-banner = Read-only view for your current role. Management actions require Manager or Admin privileges.
      |adm-admin-only-note = Staff account creation and role changes require the Admin role.
      |
      |# Staff Login
      |adm-login-title = Staff Backoffice Sign In
      |adm-login-subtitle = Manage incoming print orders, customer accounts, catalog configuration, and commercial pricing.
      |adm-login-email = Work Email
      |adm-login-password = Password
      |adm-login-submit = Sign In to Backoffice
      |adm-login-signing-in = Signing in...
      |adm-login-demo-title = Quick-fill Demo Staff Accounts
      |adm-login-demo-hint = Click any role below to fill credentials (demo password: staff1234).
      |adm-login-role-admin = Full access: Orders, Catalog, Pricing, Customers, Staff & Settings
      |adm-login-role-manager = Operations: Orders, Catalog, Pricing, Customers & Store Settings
      |adm-login-role-prepress = Read-only backoffice + Artwork Review & Internal Notes
      |
      |# Dashboard
      |adm-dash-title = Operations Dashboard
      |adm-dash-subtitle = Live overview of order intake, fulfillment stages, and B2B account approvals.
      |adm-kpi-awaiting-intake = Needs Intake Review
      |adm-kpi-awaiting-payment = Awaiting Payment
      |adm-kpi-accepted = Accepted
      |adm-kpi-in-production = In Progress
      |adm-kpi-ready-dispatch = Ready for Dispatch
      |adm-kpi-on-hold = On Hold
      |adm-kpi-completed = Completed
      |adm-kpi-pending-b2b = Pending B2B Approvals
      |adm-kpi-active-promos = Active Promo Codes
      |adm-kpi-catalog = Catalog Categories / Materials
      |adm-dash-urgent-orders = Orders Awaiting Intake / Recent Queue
      |adm-dash-view-all-orders = Open Order Queue
      |adm-dash-pending-customers = Pending B2B Customer Approvals
      |adm-dash-no-pending-customers = All B2B accounts are reviewed.
      |adm-dash-no-orders = No orders in the system yet. Place one in the Storefront to see it appear live!
      |
      |# Order Intake Queue & Detail
      |adm-orders-title = Order Intake Queue
      |adm-orders-subtitle = Review incoming customer orders, verify payments, accept into production, and track manual fulfillment stages.
      |adm-orders-search-placeholder = Search order #, customer name, company, or email...
      |adm-orders-filter-all = All Orders
      |adm-orders-filter-status = Intake Status
      |adm-orders-filter-payment = Payment Status
      |adm-orders-filter-priority = Priority
      |adm-orders-col-number = Order #
      |adm-orders-col-customer = Customer
      |adm-orders-col-priority = Priority & Deadline
      |adm-orders-col-status = Intake Status
      |adm-orders-col-payment = Payment
      |adm-orders-col-items = Items
      |adm-orders-col-total = Total
      |adm-orders-col-placed = Placed
      |adm-orders-open = Open Detail →
      |adm-orders-empty = No orders match the current filters.
      |
      |adm-od-back = ← Back to Queue
      |adm-od-actions-title = Intake & Stage Actions
      |adm-od-action-note-placeholder = Optional staff note for this transition...
      |adm-od-confirm-payment = Mark Paid
      |adm-od-mark-unpaid = Mark Unpaid
      |adm-od-payment-card = Payment Status (Independent of Production)
      |adm-od-payment-b2b-hint = Production can proceed before payment is settled (e.g. B2B Net Invoice).
      |adm-od-priority-deadline = Priority & Target Deadline
      |adm-od-mixed-speeds = Mixed production speeds across lines — order deadline reflects the fastest item.
      |adm-od-accept = Accept Order
      |adm-od-resume = Resume from Hold
      |adm-od-stage-inprod = Mark In Progress
      |adm-od-stage-ready = Mark Ready for Dispatch
      |adm-od-stage-dispatched = Mark Dispatched
      |adm-od-stage-completed = Mark Completed
      |adm-od-hold-reason-placeholder = Reason for placing order on hold...
      |adm-od-put-hold = Put On Hold
      |adm-od-cancel-reason-placeholder = Cancellation reason...
      |adm-od-cancel = Cancel Order
      |adm-od-customer-card = Customer & Delivery
      |adm-od-lines-card = Order Lines & Technical Specifications
      |adm-od-line-breakdown = Price Breakdown
      |adm-od-line-edit = Edit Line
      |adm-od-line-spec-note = Additional Specification Note
      |adm-od-totals-card = Commercial Summary
      |adm-od-artwork-card = Artwork & Preflight Review
      |adm-od-artwork-empty = No artwork files attached to this order yet.
      |adm-od-artwork-approve = Approve Artwork
      |adm-od-artwork-override = Approve with Warning
      |adm-od-artwork-request-changes = Request Re-upload
      |adm-od-artwork-reject = Reject Artwork
      |adm-od-artwork-changes-placeholder = Describe required artwork fixes (DPI, bleed, color profile)...
      |adm-od-notes-card = Internal Staff Notes
      |adm-od-notes-empty = No internal staff notes yet.
      |adm-od-notes-placeholder = Write an internal note visible only to staff...
      |adm-od-notes-add = Add Note
      |adm-od-timeline-card = Status Audit Trail
      |adm-od-subtotal = Lines Net Subtotal
      |adm-od-promo = Promo Discount
      |adm-od-shipping = Shipping Net
      |adm-od-vat = VAT
      |adm-od-grand-total = Grand Total (incl. VAT)
      |adm-od-customer-note = Customer Note
      |
      |# Catalog Manager
      |adm-cat-title = Catalog & Capabilities Inspector
      |adm-cat-subtitle = Browse product categories, substrates, finishes, printing methods, compatibility rules, and showcase listings.
      |adm-cat-tab-categories = Categories
      |adm-cat-tab-materials = Materials
      |adm-cat-tab-finishes = Finishes
      |adm-cat-tab-methods = Print Methods
      |adm-cat-tab-rules = Rules
      |adm-cat-tab-showcases = Showcase
      |adm-cat-add-category = Add Category
      |adm-cat-add-material = Add Material
      |adm-cat-add-finish = Add Finish
      |adm-cat-add-method = Add Print Method
      |adm-cat-add-rule = Add Rule
      |adm-cat-add-showcase = Save Showcase Listing
      |adm-cat-save = Save
      |adm-cat-delete = Delete
      |adm-cat-edit = Edit
      |adm-cat-cancel = Cancel
      |adm-cat-version = Catalog v{ $version }
      |adm-cat-ruleset-version = Ruleset v{ $version }
      |adm-cat-inspect-note = Click any row to expand full technical attributes and configuration details. Catalog authoring is planned for a dedicated phase.
      |
      |# Pricing & Margin Manager
      |adm-pri-title = Pricing, Promos & Margin Analysis
      |adm-pri-subtitle = Simulate real-time production margins, manage promo codes, B2B customer agreements, pricelists, and production cost sheets.
      |adm-pri-tab-simulator = Margin Simulator
      |adm-pri-tab-promos = Promo Codes
      |adm-pri-tab-agreements = B2B Agreements
      |adm-pri-tab-pricelists = Pricelists
      |adm-pri-tab-costs = Cost Sheets
      |adm-pri-sim-run = Calculate Margin
      |adm-pri-sim-net = Selling Price (Net)
      |adm-pri-sim-gross = Selling Price (Gross)
      |adm-pri-sim-cost = Production Cost (Net)
      |adm-pri-sim-margin = Gross Margin
      |adm-pri-sim-sheets = Press Sheets Used
      |adm-pri-sim-warnings = Margin Alerts
      |adm-pri-sim-healthy = Healthy margin — above production cost and threshold.
      |adm-pri-promo-create = Create Promo Code
      |adm-pri-promo-activate = Activate
      |adm-pri-promo-deactivate = Pause
      |adm-pri-promo-filter-lifecycle = Lifecycle
      |adm-pri-promo-active = Active
      |adm-pri-promo-expired = Expired
      |adm-pri-promo-exhausted = Exhausted
      |adm-pri-promo-inactive = Inactive
      |adm-pri-agr-create = Save Agreement
      |adm-pri-agr-edit-global = Update Global Terms
      |adm-pri-agr-scoped-readonly = Category, Material & Finish Overrides (View Only)
      |adm-pri-cost-save = Update Overhead Factor
      |
      |# Customer Manager
      |adm-cust-title = Customer Accounts & B2B Approvals
      |adm-cust-subtitle = Inspect accounts, edit profiles & contact persons, review custom pricing agreements, audit trail, and order history.
      |adm-cust-search-placeholder = Search name, email, company, IČO, or contact person...
      |adm-cust-filter-all = All
      |adm-cust-filter-status = Status
      |adm-cust-filter-tier = Tier
      |adm-cust-register-new = + Register Customer
      |adm-cust-approve = Approve Account
      |adm-cust-edit-profile = Edit Profile & Contacts
      |adm-cust-suspend = Suspend
      |adm-cust-deactivate = Deactivate
      |adm-cust-tier-label = Pricing Tier
      |adm-cust-terms-label = Payment Terms
      |adm-cust-terms-prepaid = Prepaid Only
      |adm-cust-terms-net = Net Invoice Credit
      |adm-cust-save-terms = Save Payment Terms
      |adm-cust-contacts-title = Contact Persons
      |adm-cust-add-contact = + Add Contact Person
      |adm-cust-main-contact = Primary Contact
      |adm-cust-notes-title = Internal Account Notes
      |adm-cust-orders-title = Order History
      |adm-cust-agreements-title = Customer Pricing & Agreements
      |adm-cust-edit-in-pricing = Edit in Pricing →
      |adm-cust-audit-title = Audit Trail
      |adm-cust-select-hint = Click any customer row to expand profile, contacts, custom pricing, audit trail, and orders.
      |
      |# Staff & Store Settings
      |adm-set-title = Staff Directory & Store Settings
      |adm-set-subtitle = Administer employee roles, pickup locations, courier options, and Bank Transfer QR accounts.
      |adm-set-tab-staff = Staff Directory
      |adm-set-tab-delivery = Pickup & Couriers
      |adm-set-tab-bank = Bank QR Accounts
      |adm-set-staff-add = Add Staff Member
      |adm-set-staff-activate = Activate
      |adm-set-staff-deactivate = Deactivate
      |adm-set-pickup-add = Save Pickup Location
      |adm-set-courier-add = Save Courier Option
      |adm-set-bank-save = Save Bank Account
      |
      |# Common Toasts
      |adm-toast-saved = Changes saved.
      |adm-toast-deleted = Item deleted.
      |adm-toast-order-updated = Order updated.
      |adm-toast-signed-in = Signed in as { $name }.
      |adm-toast-signed-out = Signed out of Backoffice.
      |""".stripMargin

  val adminCsFtl: String =
    """# Backoffice Shell & Navigace
      |adm-brand-subtitle = Administrace
      |adm-nav-dashboard = Přehled
      |adm-nav-orders = Příjem objednávek
      |adm-nav-catalog = Katalog
      |adm-nav-pricing = Ceníky a marže
      |adm-nav-customers = Zákazníci
      |adm-nav-settings = Zaměstnanci a nastavení
      |adm-back-to-storefront = ← E-shop
      |adm-refresh = Obnovit
      |adm-sign-out = Odhlásit se
      |adm-readonly-banner = Režim pouze pro čtení pro vaši roli. Správcovské akce vyžadují roli Manager nebo Admin.
      |adm-admin-only-note = Zakládání zaměstnanců a změna rolí vyžaduje roli Admin.
      |
      |# Přihlášení zaměstnance
      |adm-login-title = Přihlášení do administrace
      |adm-login-subtitle = Spravujte příchozí tiskové objednávky, B2B zákazníky, katalog produktů a obchodní ceníky.
      |adm-login-email = Pracovní e-mail
      |adm-login-password = Heslo
      |adm-login-submit = Přihlásit se do administrace
      |adm-login-signing-in = Přihlašování...
      |adm-login-demo-title = Rychlé demo účty zaměstnanců
      |adm-login-demo-hint = Kliknutím předvyplníte přihlašovací údaje (demo heslo: staff1234).
      |adm-login-role-admin = Plný přístup: Objednávky, Katalog, Ceníky, Zákazníci, Zaměstnanci a Nastavení
      |adm-login-role-manager = Provoz: Objednávky, Katalog, Ceníky, Zákazníci a Nastavení obchodu
      |adm-login-role-prepress = Pouze čtení + kontrola tiskových dat a interní poznámky
      |
      |# Přehled (Dashboard)
      |adm-dash-title = Provozní přehled
      |adm-dash-subtitle = Aktuální stav příjmu objednávek, manuálních fází zpracování a schvalování B2B účtů.
      |adm-kpi-awaiting-intake = Čeká na příjem
      |adm-kpi-awaiting-payment = Čeká na platbu
      |adm-kpi-accepted = Přijato
      |adm-kpi-in-production = Ve výrobě
      |adm-kpi-ready-dispatch = K expedici
      |adm-kpi-on-hold = Pozastaveno
      |adm-kpi-completed = Dokončeno
      |adm-kpi-pending-b2b = B2B ke schválení
      |adm-kpi-active-promos = Aktivní slevové kódy
      |adm-kpi-catalog = Kategorie / Materiály v katalogu
      |adm-dash-urgent-orders = Objednávky k přijetí / Poslední fronta
      |adm-dash-view-all-orders = Otevřít frontu objednávek
      |adm-dash-pending-customers = B2B zákazníci čekající na schválení
      |adm-dash-no-pending-customers = Všechny B2B účty jsou zkontrolovány.
      |adm-dash-no-orders = Zatím zde nejsou žádné objednávky. Vytvořte objednávku v e-shopu a ihned se zde objeví!
      |
      |# Fronta objednávek a detail
      |adm-orders-title = Příjem objednávek
      |adm-orders-subtitle = Kontrolujte příchozí objednávky, potvrzujte platby, přijímejte zakázky a posouvejte manuální stavy.
      |adm-orders-search-placeholder = Hledat číslo objednávky, zákazníka, firmu nebo e-mail...
      |adm-orders-filter-all = Všechny
      |adm-orders-filter-status = Stav příjmu
      |adm-orders-filter-payment = Stav platby
      |adm-orders-filter-priority = Priorita
      |adm-orders-col-number = Číslo obj.
      |adm-orders-col-customer = Zákazník
      |adm-orders-col-priority = Priorita a termín
      |adm-orders-col-status = Stav příjmu
      |adm-orders-col-payment = Platba
      |adm-orders-col-items = Položky
      |adm-orders-col-total = Celkem
      |adm-orders-col-placed = Vytvořeno
      |adm-orders-open = Otevřít detail →
      |adm-orders-empty = Aktuálním filtrům neodpovídají žádné objednávky.
      |
      |adm-od-back = ← Zpět na frontu
      |adm-od-actions-title = Akce příjmu a posun stavu
      |adm-od-action-note-placeholder = Volitelná interní poznámka k přechodu stavu...
      |adm-od-confirm-payment = Označit jako zaplaceno
      |adm-od-mark-unpaid = Označit jako nezaplaceno
      |adm-od-payment-card = Stav platby (nezávisle na výrobě)
      |adm-od-payment-b2b-hint = Výroba může pokračovat i před uhrazením faktury (např. B2B se splatností).
      |adm-od-priority-deadline = Priorita a cílový termín
      |adm-od-mixed-speeds = Smíšené rychlosti výroby u položek — termín objednávky odpovídá nejrychlejší položce.
      |adm-od-accept = Přijmout objednávku
      |adm-od-resume = Obnovit z pozastavení
      |adm-od-stage-inprod = Označit: Ve výrobě
      |adm-od-stage-ready = Označit: K expedici
      |adm-od-stage-dispatched = Označit: Expedováno
      |adm-od-stage-completed = Označit: Dokončeno
      |adm-od-hold-reason-placeholder = Důvod pozastavení objednávky...
      |adm-od-put-hold = Pozastavit
      |adm-od-cancel-reason-placeholder = Důvod zrušení objednávky...
      |adm-od-cancel = Zrušit objednávku
      |adm-od-customer-card = Zákazník a doručení
      |adm-od-lines-card = Položky objednávky a technická specifikace
      |adm-od-line-breakdown = Rozpis ceny
      |adm-od-line-edit = Upravit položku
      |adm-od-line-spec-note = Doplňující textová specifikace
      |adm-od-totals-card = Obchodní souhrn
      |adm-od-artwork-card = Tisková data a Preflight
      |adm-od-artwork-empty = K této objednávce zatím nejsou přiložena žádná tisková data.
      |adm-od-artwork-approve = Schválit data
      |adm-od-artwork-override = Schválit s upozorněním
      |adm-od-artwork-request-changes = Vyžádat nové nahrání
      |adm-od-artwork-reject = Zamítnout data
      |adm-od-artwork-changes-placeholder = Popište požadované opravy tiskových dat (DPI, spadávka, barevný profil)...
      |adm-od-notes-card = Interní poznámky zaměstnanců
      |adm-od-notes-empty = Zatím žádné interní poznámky.
      |adm-od-notes-placeholder = Napište interní poznámku viditelnou pouze pro tým...
      |adm-od-notes-add = Přidat poznámku
      |adm-od-timeline-card = Auditní historie stavů
      |adm-od-subtotal = Mezisoučet položek bez DPH
      |adm-od-promo = Slevový kód
      |adm-od-shipping = Doprava bez DPH
      |adm-od-vat = DPH
      |adm-od-grand-total = Celkem k úhradě (vč. DPH)
      |adm-od-customer-note = Poznámka zákazníka
      |
      |# Správa katalogu
      |adm-cat-title = Přehled katalogu a možností výroby
      |adm-cat-subtitle = Prohlížejte produktové kategorie, materiály, zušlechtění, tiskové technologie, pravidla a výlohu e-shopu.
      |adm-cat-tab-categories = Kategorie
      |adm-cat-tab-materials = Materiály
      |adm-cat-tab-finishes = Zušlechtění
      |adm-cat-tab-methods = Tiskové metody
      |adm-cat-tab-rules = Pravidla
      |adm-cat-tab-showcases = Výloha e-shopu
      |adm-cat-add-category = Přidat kategorii
      |adm-cat-add-material = Přidat materiál
      |adm-cat-add-finish = Přidat zušlechtění
      |adm-cat-add-method = Přidat tiskovou metodu
      |adm-cat-add-rule = Přidat pravidlo
      |adm-cat-add-showcase = Uložit kartu výlohy
      |adm-cat-save = Uložit
      |adm-cat-delete = Smazat
      |adm-cat-edit = Upravit
      |adm-cat-cancel = Zrušit
      |adm-cat-version = Katalog v{ $version }
      |adm-cat-ruleset-version = Pravidla v{ $version }
      |adm-cat-inspect-note = Kliknutím na řádek zobrazíte kompletní technické parametry. Editor katalogu je plánován v samostatné fázi.
      |
      |# Ceníky a simulátor marže
      |adm-pri-title = Ceníky, slevové kódy a analýza marže
      |adm-pri-subtitle = Simulujte výrobní marže v reálném čase, spravujte promo kódy, B2B smlouvy, ceníky a nákladové listy.
      |adm-pri-tab-simulator = Simulátor marže
      |adm-pri-tab-promos = Slevové kódy
      |adm-pri-tab-agreements = B2B smlouvy
      |adm-pri-tab-pricelists = Ceníky
      |adm-pri-tab-costs = Nákladové listy
      |adm-pri-sim-run = Spočítat marži
      |adm-pri-sim-net = Prodejní cena (bez DPH)
      |adm-pri-sim-gross = Prodejní cena (vč. DPH)
      |adm-pri-sim-cost = Výrobní náklady (bez DPH)
      |adm-pri-sim-margin = Hrubá marže
      |adm-pri-sim-sheets = Spotřeba tiskových archů
      |adm-pri-sim-warnings = Upozornění na marži
      |adm-pri-sim-healthy = Zdravá marže — nad výrobními náklady i prahem.
      |adm-pri-promo-create = Vytvořit slevový kód
      |adm-pri-promo-activate = Aktivovat
      |adm-pri-promo-deactivate = Pozastavit
      |adm-pri-promo-filter-lifecycle = Stav kódu
      |adm-pri-promo-active = Aktivní
      |adm-pri-promo-expired = Expirované
      |adm-pri-promo-exhausted = Vyčerpané
      |adm-pri-promo-inactive = Neaktivní
      |adm-pri-agr-create = Uložit smlouvu
      |adm-pri-agr-edit-global = Uložit globální podmínky
      |adm-pri-agr-scoped-readonly = Slevy na kategorie, materiály a zušlechtění (pouze pro čtení)
      |adm-pri-cost-save = Uložit režijní koeficient
      |
      |# Správa zákazníků
      |adm-cust-title = Zákaznické účty a schvalování B2B
      |adm-cust-subtitle = Spravujte účty, upravujte údaje a kontaktní osoby, zobrazujte cenové smlouvy, auditní historii a objednávky.
      |adm-cust-search-placeholder = Hledat jméno, e-mail, firmu, IČO nebo kontaktní osobu...
      |adm-cust-filter-all = Všichni
      |adm-cust-filter-status = Stav účtu
      |adm-cust-filter-tier = Cenová hladina
      |adm-cust-register-new = + Registrovat zákazníka
      |adm-cust-approve = Schválit účet
      |adm-cust-edit-profile = Upravit profil a kontakty
      |adm-cust-suspend = Pozastavit
      |adm-cust-deactivate = Deaktivovat
      |adm-cust-tier-label = Cenová hladina
      |adm-cust-terms-label = Platební podmínky
      |adm-cust-terms-prepaid = Pouze platba předem
      |adm-cust-terms-net = Faktura se splatností
      |adm-cust-save-terms = Uložit platební podmínky
      |adm-cust-contacts-title = Kontaktní osoby
      |adm-cust-add-contact = + Přidat kontaktní osobu
      |adm-cust-main-contact = Hlavní kontakt
      |adm-cust-notes-title = Interní poznámky k účtu
      |adm-cust-orders-title = Historie objednávek
      |adm-cust-agreements-title = Zákaznické ceny a smlouvy
      |adm-cust-edit-in-pricing = Upravit v Cenících →
      |adm-cust-audit-title = Auditní záznamy
      |adm-cust-select-hint = Kliknutím na zákazníka zobrazíte profil, kontakty, individuální ceny, audit a objednávky.
      |
      |# Zaměstnanci a nastavení obchodu
      |adm-set-title = Zaměstnanci a nastavení obchodu
      |adm-set-subtitle = Spravujte role zaměstnanců, odběrná místa, kurýrní dopravce a bankovní účty pro QR platby.
      |adm-set-tab-staff = Zaměstnanci
      |adm-set-tab-delivery = Odběr a dopravci
      |adm-set-tab-bank = Bankovní QR účty
      |adm-set-staff-add = Přidat zaměstnance
      |adm-set-staff-activate = Aktivovat
      |adm-set-staff-deactivate = Deaktivovat
      |adm-set-pickup-add = Uložit odběrné místo
      |adm-set-courier-add = Uložit dopravce
      |adm-set-bank-save = Uložit bankovní účet
      |
      |# Společná oznámení
      |adm-toast-saved = Změny uloženy.
      |adm-toast-deleted = Položka smazána.
      |adm-toast-order-updated = Objednávka aktualizována.
      |adm-toast-signed-in = Přihlášen jako { $name }.
      |adm-toast-signed-out = Odhlášeno z administrace.
      |""".stripMargin
