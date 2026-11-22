package mpbuilder.json

import com.github.plokhotnyuk.jsoniter_scala.core.*
import mpbuilder.catalog.*
import mpbuilder.catalog.sample.{SampleCatalog, SampleRules}
import mpbuilder.json.DomainCodecs.given
import mpbuilder.pricing.*
import mpbuilder.pricing.sample.*
import mpbuilder.shared.*
import zio.*
import zio.test.*

object DomainCodecsSpec extends ZIOSpecDefault:

  private def roundTrip[A](value: A)(using JsonValueCodec[A]): (A, String) =
    val json = writeToString(value)
    (readFromString[A](json), json)

  def spec = suite("DomainCodecs")(
    test("ProductCatalog sample round-trips losslessly") {
      val (back, json) = roundTrip(SampleCatalog.catalog)
      assertTrue(back == SampleCatalog.catalog, json.length > 1000)
    },
    test("recursive CompatibilityRuleset round-trips losslessly") {
      val (back, _) = roundTrip(SampleRules.ruleset)
      assertTrue(back == SampleRules.ruleset)
    },
    test("pricelists, agreements, promos and cost sheets round-trip") {
      val pricelists = List(SamplePricelist.pricelistUsd, SamplePricelist.pricelistCzk, SamplePricelist.pricelistCzkSheet)
      val sheets     = List(SampleCostSheets.costSheetUsd, SampleCostSheets.costSheetCzk)
      assertTrue(
        pricelists.forall(p => roundTrip(p)._1 == p),
        SampleAgreements.all.forall(a => roundTrip(a)._1 == a),
        SamplePromoCodes.all.forall(p => roundTrip(p)._1 == p),
        sheets.forall(s => roundTrip(s)._1 == s),
      )
    },
    test("opaque types are validated on decode") {
      val bad = scala.util.Try(readFromString[EmailAddress]("\"not-an-email\""))
      val ok  = readFromString[EmailAddress]("\"Jan@Example.COM\"")
      assertTrue(bad.isFailure, ok == EmailAddress.unsafe("jan@example.com"))
    },
    test("LocalizedString and Money encode compactly") {
      val ls = LocalizedString("Hello", "Ahoj")
      assertTrue(
        writeToString(ls) == """{"En":"Hello","Cs":"Ahoj"}""",
        writeToString(Money("12.50")) == "12.50",
        readFromString[LocalizedString](writeToString(ls)) == ls,
      )
    },
    test("recursive error ADTs with NonEmptyChunk round-trip") {
      val leaf: CatalogError = CatalogError.CategoryNotFound(CategoryId.unsafe("flyers"))
      val err: CatalogError = CatalogError.ConfigurationValidationFailed(
        NonEmptyChunk(leaf, CatalogError.ConfigurationValidationFailed(NonEmptyChunk.single(leaf)))
      )
      val (back, json) = roundTrip(err)
      assertTrue(back == err, json.contains("\"type\":\"CategoryNotFound\""))
    },
  )
