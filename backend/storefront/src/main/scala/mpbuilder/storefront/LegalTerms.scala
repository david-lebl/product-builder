package mpbuilder.storefront

import mpbuilder.shared.*

/** The store's legal documents a customer must accept before placing an order.
  *
  * @param version    identifier recorded on every order as proof of consent (`TermsConsent`); bump it
  *                   whenever the Terms & Conditions or the privacy notice change
  * @param termsUrl   Terms & Conditions page per language (relative to the storefront page or absolute)
  * @param privacyUrl privacy notice / GDPR personal-data processing page per language
  */
final case class LegalTerms(
    version: String,
    termsUrl: LocalizedString,
    privacyUrl: LocalizedString,
)

/** Store configuration: the legal documents currently in force.
  *
  * Single source of truth — the storefront facade serves it with the checkout options, refuses
  * orders without consent and stamps `version` on the order. To publish new terms, edit the pages
  * under `modules/ui/src/main/resources/legal/` (or point the URLs elsewhere) and bump `version`.
  */
object LegalTerms:
  val Current: LegalTerms = LegalTerms(
    version = "2026-11-28",
    termsUrl = LocalizedString(en = "legal/terms.html", cs = "legal/terms.html#cs"),
    privacyUrl = LocalizedString(en = "legal/privacy.html", cs = "legal/privacy.html#cs"),
  )
