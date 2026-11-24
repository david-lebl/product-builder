package mpbuilder.json

import kyo.*
import mpbuilder.json.JsonSupport.*
import mpbuilder.shared.*
import mpbuilder.json.SharedCodecs.given
import mpbuilder.json.JsonSupport.given
import mpbuilder.pricing.*

/** Schemas for the public `pricing` model (pricelists, agreements, promos, cost sheets, quotes). */
object PricingCodecs:

  given Schema[PriceEntryId] = stringOpaque("PriceEntryId")(PriceEntryId(_))(_.value)


  given Schema[Pricelist]              = Schema.derived
  given Schema[AgreementTarget]        = Schema.derived
  given Schema[CustomerPriceAgreement] = Schema.derived
  given Schema[PromoCode]              = Schema.derived
  given Schema[ProductionCostSheet]    = Schema.derived
  given Schema[DynamicPricingContext]  = Schema.derived
  given Schema[PricingSubject]         = Schema.derived
  given Schema[PriceQuote]             = Schema.derived
  given Schema[MarginAnalysis]         = Schema.derived
  given Schema[PricingError]           = Schema.derived
