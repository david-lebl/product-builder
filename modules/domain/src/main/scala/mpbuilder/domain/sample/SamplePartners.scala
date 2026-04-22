package mpbuilder.domain.sample

import mpbuilder.domain.model.*
import mpbuilder.domain.manufacturing.*

object SamplePartners:

  val largePrintPartnerId: PartnerId   = PartnerId.unsafe("partner-large-format")
  val varnishPartnerId: PartnerId      = PartnerId.unsafe("partner-spot-varnish")

  val largePrintPartner: ExternalPartner = ExternalPartner(
    id = largePrintPartnerId,
    name = LocalizedString("LargePrint Partner", "Partner pro velkoplošný tisk"),
    capabilities = Set(StationType.LargeFormatPrinter, StationType.LargeFormatFinishing),
    supportedCategories = Set(SampleCatalog.bannersId),
    leadTimeBusinessDays = (10, 14),
    priceMarkup = BigDecimal("1.15"),
    availability = PartnerAvailability.available,
    contact = "partner-lf@example.com",
  )

  val varnishPartner: ExternalPartner = ExternalPartner(
    id = varnishPartnerId,
    name = LocalizedString("Varnish Specialist", "Specialista na lak"),
    capabilities = Set(StationType.UVCoater),
    supportedCategories = Set(SampleCatalog.flyersId, SampleCatalog.freeId),
    leadTimeBusinessDays = (7, 10),
    priceMarkup = BigDecimal("1.20"),
    availability = PartnerAvailability.available,
    contact = "partner-uv@example.com",
  )

  val allPartners: Map[PartnerId, ExternalPartner] = Map(
    largePrintPartnerId -> largePrintPartner,
    varnishPartnerId    -> varnishPartner,
  )
