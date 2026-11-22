package mpbuilder.json

import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*
import mpbuilder.customers.*
import mpbuilder.json.JsonSupport.*
import mpbuilder.json.SharedCodecs.given

/** Codecs for the public `customers` model. */
object CustomerCodecs:
  given JsonValueCodec[CustomerStatus]             = JsonCodecMaker.make(config)
  given JsonValueCodec[CompanyProfile]             = JsonCodecMaker.make(config)
  given JsonValueCodec[ContactPerson]              = JsonCodecMaker.make(config)
  given JsonValueCodec[PaymentTerms]               = JsonCodecMaker.make(config)
  given JsonValueCodec[CustomerAuditEntry]         = JsonCodecMaker.make(config)
  given JsonValueCodec[AccountNote]                = JsonCodecMaker.make(config)
  given JsonValueCodec[CustomerView]               = JsonCodecMaker.make(config)
  given JsonValueCodec[CustomerFilter]             = JsonCodecMaker.make(config)
  given JsonValueCodec[RegisterCustomerCmd]        = JsonCodecMaker.make(config)
  given JsonValueCodec[UpdateCustomerProfileCmd]   = JsonCodecMaker.make(config)
  given JsonValueCodec[UpdateCustomerAddressesCmd] = JsonCodecMaker.make(config)
  given JsonValueCodec[CustomerIdentifier]         = JsonCodecMaker.make(config)
  given JsonValueCodec[CustomerError]              = JsonCodecMaker.make(config)
