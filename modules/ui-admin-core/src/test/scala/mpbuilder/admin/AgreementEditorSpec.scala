package mpbuilder.admin

import kyo.*
import mpbuilder.admin.i18n.AdminTranslations
import mpbuilder.admin.port.BackofficeClient
import mpbuilder.admin.pricing.{AgreementDraft, AgreementJson}
import mpbuilder.admin.pricing.AgreementDraft.{DiscountRow, FixedRateRow, RateKind, TargetKind}
import mpbuilder.admin.state.{AdminRoute, BackofficeState}
import mpbuilder.admin.views.AgreementEditorView
import mpbuilder.pricing.*
import mpbuilder.pricing.sample.SampleAgreements
import mpbuilder.sample.SampleShowcaseRuntime
import mpbuilder.shared.*
import mpbuilder.ui.i18n.AppI18n
import kyo.test.*

class AgreementEditorSpec extends Test[Any]:

  private def renderHtml(ui: UI, i18n: AppI18n): String < Async =
    I18n.let(i18n.handle)(UI.runRender(ui).take(1).run.map(_.headMaybe.getOrElse("")))

  "B2B agreement editor" - {
    "every sample agreement round-trips through the form draft" in {
      val results = SampleAgreements.all.map(a => AgreementDraft.from(a).toAgreement)
      assert(results.zip(SampleAgreements.all).forall { case (r, a) => r == Right(a) })
    }

    "draft builds category, material, finish discounts and fixed rates" in {
      val draft = AgreementDraft(
        targetKind = TargetKind.Customer,
        customerId = "cust-mega-corp",
        currency = Currency.USD,
        globalDiscount = "7,5",
        minimumOrder = "250",
        categoryDiscounts = Vector(DiscountRow("business-cards", "20")),
        materialDiscounts = Vector(DiscountRow("mat-coated-300", "12.5")),
        finishDiscounts = Vector(DiscountRow("fin-matte-lam", "30")),
        fixedMaterialRates = Vector(FixedRateRow("mat-vinyl", RateKind.PerSqMeter, "4.20")),
      )
      val agr = draft.toAgreement
      assert(agr.isRight)
      val a = agr.toOption.get
      assert(a.target == AgreementTarget.SpecificCustomer(CustomerId.unsafe("cust-mega-corp")))
      assert(a.globalDiscount.map(_.value).contains(BigDecimal("7.5")))
      assert(a.minimumOrderOverride.map(_.value).contains(BigDecimal(250)))
      assert(a.categoryDiscounts == Map(CategoryId.unsafe("business-cards") -> Percentage.unsafe(BigDecimal(20))))
      assert(a.materialDiscounts == Map(MaterialId.unsafe("mat-coated-300") -> Percentage.unsafe(BigDecimal("12.5"))))
      assert(a.finishDiscounts == Map(FinishId.unsafe("fin-matte-lam") -> Percentage.unsafe(BigDecimal(30))))
      assert(a.fixedMaterialRates == Map(MaterialId.unsafe("mat-vinyl") -> RateModel.PerSqMeter(Money("4.20"))))
    }

    "draft validation reports every problem at once" in {
      val draft = AgreementDraft(
        targetKind = TargetKind.Customer,
        customerId = "",
        globalDiscount = "120",
        minimumOrder = "abc",
        categoryDiscounts = Vector(DiscountRow("flyers", "10"), DiscountRow("flyers", "5"), DiscountRow("", "3")),
        fixedMaterialRates = Vector(FixedRateRow("mat-a", RateKind.PerSheet, "-1")),
      )
      val issues = draft.toAgreement.left.toOption.getOrElse(Nil).map(_.en)
      assert(issues.exists(_.contains("Select a customer")))
      assert(issues.exists(_.contains("between 0 and 100")))
      assert(issues.exists(_.contains("'abc' is not a number")))
      assert(issues.exists(_.contains("'flyers' is listed more than once")))
      assert(issues.exists(_.contains("row 3: select an item")))
      assert(issues.exists(_.contains("must not be negative")))
    }

    "advanced rate models are preserved by the form" in {
      val tiered = RateModel.TieredArea(NonEmptyChunk(AreaTier(SquareMeters.unsafe(BigDecimal(0)), Money(10))))
      val a = CustomerPriceAgreement(
        target = AgreementTarget.TierDefault(CustomerTier.Silver),
        currency = Currency.CZK,
        fixedMaterialRates = Map(MaterialId.unsafe("mat-vinyl") -> tiered),
      )
      val d = AgreementDraft.from(a)
      assert(d.fixedMaterialRates.head.kind == RateKind.Advanced)
      assert(d.toAgreement == Right(a))
    }

    "JSON mode round-trips every sample agreement and validates values" in {
      assert(SampleAgreements.all.forall(a => AgreementJson.decode(AgreementJson.encode(a)) == Right(a)))
      val pretty = AgreementJson.encode(SampleAgreements.all.find(_.customVolumeCurves.isDefined).get)
      assert(pretty.contains("\n  \"globalDiscount\": \"15\""))
      val badPct = pretty.replaceFirst("\"globalDiscount\": \"15\"", "\"globalDiscount\": \"140\"")
      assert(badPct != pretty)
      assert(AgreementJson.decode(badPct).isLeft)
      assert(AgreementJson.decode("{ not json").isLeft)
      assert(AgreementJson.decode("").isLeft)
    }

    "editor renders form and JSON modes in EN & CS and saves a re-targeted agreement" in {
      SampleShowcaseRuntime.make().map { rt =>
        val client = BackofficeClient.fromFacade(rt.backoffice)
        for
          i18n    <- AdminTranslations.init(Language.En)
          bo      <- BackofficeState.init(client, i18n, AdminRoute.Pricing)
          _       <- I18n.let(i18n.handle)(bo.login("admin@printshop.cz", "staff1234"))
          catalog <- bo.catalog.get
          custs   <- bo.customers.get
          agrs    <- bo.agreements.get
          original = agrs.find(_.customVolumeCurves.isDefined).get
          session <- AgreementEditorView.Session.make(AgreementDraft.from(original))
          ui       = AgreementEditorView.render(bo, session, catalog, custs, agrs, Some(original), None, Language.En, () => Kyo.unit)
          formEn  <- renderHtml(ui, i18n)
          uiCs     = AgreementEditorView.render(bo, session, catalog, custs, agrs, Some(original), None, Language.Cs, () => Kyo.unit)
          formCs  <- renderHtml(uiCs, i18n)
          _       <- session.json.set(AgreementJson.encode(original))
          _       <- session.mode.set(AgreementEditorView.Mode.Json)
          jsonEn  <- renderHtml(ui, i18n)
          // Re-target to the GBP currency through the form and save: the old agreement is replaced
          _       <- session.mode.set(AgreementEditorView.Mode.Form)
          _       <- session.draft.updateAndGet(_.copy(currency = Currency.GBP, globalDiscount = "22"))
          resolved <- AgreementEditorView.resolve(session, None)
          ok      <- bo.replaceAgreement(Some((original.target, original.currency)), resolved.toOption.get)
          after   <- bo.agreements.get
        yield
          assert(formEn.contains("Product category discounts"))
          assert(formEn.contains("Fixed material rates"))
          assert(formEn.contains("Custom volume curves"))
          // Every select re-renders with the draft's value marked `selected` (no snap-back to option 1)
          val sel = (v: String) => s"""value="$v" selected"""
          assert(formEn.contains(sel("Customer")) && !formEn.contains(sel("Tier")))
          assert(formEn.contains(sel(original.target.asInstanceOf[AgreementTarget.SpecificCustomer].customerId.value)))
          assert(formEn.contains(sel(original.currency.toString)))
          original.categoryDiscounts.keys.foreach(k => assert(formEn.contains(sel(k.value))))
          original.fixedMaterialRates.keys.foreach(k => assert(formEn.contains(sel(k.value))))
          assert(formCs.contains("Slevy na kategorie produktů"))
          assert(jsonEn.contains("<textarea"))
          assert(!formEn.contains("‹") && !formCs.contains("‹") && !jsonEn.contains("‹"))
          assert(ok)
          assert(!after.exists(a => a.target == original.target && a.currency == original.currency))
          assert(after.exists(a => a.target == original.target && a.currency == Currency.GBP && a.globalDiscount.exists(_.value == 22)))
        end for
      }
    }
  }
