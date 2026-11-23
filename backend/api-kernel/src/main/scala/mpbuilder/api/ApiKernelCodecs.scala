package mpbuilder.api

import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*
import mpbuilder.artwork.*
import mpbuilder.catalog.*
import mpbuilder.customers.*
import mpbuilder.identity.*
import mpbuilder.json.DomainCodecs.given
import mpbuilder.json.JsonSupport.config
import mpbuilder.ordering.*
import mpbuilder.pricing.*
import mpbuilder.shared.*

/** Codecs shared by every BFF contract: all domain codecs plus collections of domain types. */
object ApiKernelCodecs:
  export mpbuilder.json.DomainCodecs.given

  given listShowcaseListing: JsonValueCodec[List[ShowcaseListing]]        = JsonCodecMaker.make(config)
  given listFinish: JsonValueCodec[List[Finish]]                 = JsonCodecMaker.make(config)
  given listOrderView: JsonValueCodec[List[OrderView]]              = JsonCodecMaker.make(config)
  given listArtworkSubmissionView: JsonValueCodec[List[ArtworkSubmissionView]]  = JsonCodecMaker.make(config)
  given listPricelist: JsonValueCodec[List[Pricelist]]              = JsonCodecMaker.make(config)
  given listCustomerPriceAgreement: JsonValueCodec[List[CustomerPriceAgreement]] = JsonCodecMaker.make(config)
  given listPromoCode: JsonValueCodec[List[PromoCode]]              = JsonCodecMaker.make(config)
  given listProductionCostSheet: JsonValueCodec[List[ProductionCostSheet]]    = JsonCodecMaker.make(config)
  given listCustomerView: JsonValueCodec[List[CustomerView]]           = JsonCodecMaker.make(config)
  given listStaffMemberView: JsonValueCodec[List[StaffMemberView]]        = JsonCodecMaker.make(config)
  given listShopPickupLocation: JsonValueCodec[List[ShopPickupLocation]]     = JsonCodecMaker.make(config)
  given listCourierServiceOption: JsonValueCodec[List[CourierServiceOption]]   = JsonCodecMaker.make(config)
  given mapCurrencyBankAccountConfig: JsonValueCodec[Map[Currency, BankAccountConfig]] = JsonCodecMaker.make(config)

  given JsonValueCodec[HealthStatus] = JsonCodecMaker.make(config)
