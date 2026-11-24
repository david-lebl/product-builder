package mpbuilder.json

import kyo.*
import kyo.test.*
import mpbuilder.catalog.*
import mpbuilder.catalog.sample.{SampleCatalog, SampleRules}
import mpbuilder.json.DomainCodecs.given
import mpbuilder.json.JsonSupport.*
import mpbuilder.pricing.*
import mpbuilder.pricing.sample.*
import mpbuilder.shared.*

class DomainCodecsSpec extends Test[Any]:

  private def roundTrip[A](value: A)(using Schema[A]): (A, String) =
    val json = toJson(value)
    (fromJsonUnsafe[A](json), json)

  "DomainCodecs" - {
    "ProductCatalog sample round-trips losslessly" in {
      val (back, json) = roundTrip(SampleCatalog.catalog)
      assert(back == SampleCatalog.catalog)
      assert(json.length > 1000)
    }

    "recursive CompatibilityRuleset round-trips losslessly" in {
      val (back, _) = roundTrip(SampleRules.ruleset)
      assert(back == SampleRules.ruleset)
    }

    "pricelists, agreements, promos and cost sheets round-trip" in {
      val pricelists = List(SamplePricelist.pricelistUsd, SamplePricelist.pricelistCzk, SamplePricelist.pricelistCzkSheet)
      val sheets     = List(SampleCostSheets.costSheetUsd, SampleCostSheets.costSheetCzk)
      assert(pricelists.forall(p => roundTrip(p)._1 == p))
      assert(SampleAgreements.all.forall(a => roundTrip(a)._1 == a))
      assert(SamplePromoCodes.all.forall(p => roundTrip(p)._1 == p))
      assert(sheets.forall(s => roundTrip(s)._1 == s))
    }

    "opaque types are validated on decode" in {
      val bad = fromJson[EmailAddress]("\"not-an-email\"")
      val ok  = fromJson[EmailAddress]("\"Jan@Example.COM\"")
      assert(bad.isLeft)
      assert(ok == Right(EmailAddress.unsafe("jan@example.com")))
      assert(fromJson[NonEmptyChunk[Int]]("[]").isLeft)
      assert(fromJson[Map[CategoryId, Int]]("""{"  ":1}""").isLeft)
    }

    "LocalizedString, Money and id-keyed maps encode compactly" in {
      val ls = LocalizedString("Hello", "Ahoj")
      assert(toJson(ls) == """{"En":"Hello","Cs":"Ahoj"}""")
      // kyo-schema writes BigDecimal as a JSON string, so amounts never lose precision in transit.
      assert(toJson(Money("12.50")) == "\"12.50\"")
      assert(fromJsonUnsafe[Money]("\"12.50\"") == Money("12.50"))
      assert(fromJsonUnsafe[LocalizedString](toJson(ls)) == ls)
      assert(toJson(Map(CategoryId.unsafe("flyers") -> 1)) == """{"flyers":1}""")
    }

    "recursive error ADTs with NonEmptyChunk round-trip" in {
      val leaf: CatalogError = CatalogError.CategoryNotFound(CategoryId.unsafe("flyers"))
      val err: CatalogError = CatalogError.ConfigurationValidationFailed(
        NonEmptyChunk(leaf, CatalogError.ConfigurationValidationFailed(NonEmptyChunk.single(leaf)))
      )
      val (back, json) = roundTrip(err)
      assert(back == err)
      assert(json.contains("CategoryNotFound"))
    }
  }
