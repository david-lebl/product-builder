package mpbuilder.domain.json

import zio.json.*
import mpbuilder.domain.model.*
import mpbuilder.domain.pricing.*
import mpbuilder.domain.rules.*

/** JSON codecs for all domain model types, pricing types, and rule types.
  *
  * Opaque types use custom codecs that encode/decode as their underlying representation.
  * Enums use string-based codecs derived from ordinal names.
  * Sealed trait hierarchies use discriminator-based codecs.
  */
object DomainCodecs:

  // ── Language ──────────────────────────────────────────────────────

  given JsonCodec[Language] = JsonCodec.string.transform(
    s => Language.fromCode(s),
    l => l.toCode,
  )

  given JsonCodec[LocalizedString] =
    val mapEncoder = JsonEncoder[Map[String, String]]
    val mapDecoder = JsonDecoder[Map[String, String]]
    val encoder: JsonEncoder[LocalizedString] = mapEncoder.contramap { ls =>
      val en = ls(Language.En)
      val cs = ls(Language.Cs)
      val result = scala.collection.mutable.Map[String, String]()
      if en.nonEmpty then result += ("en" -> en)
      if cs.nonEmpty then result += ("cs" -> cs)
      result.toMap
    }
    val decoder: JsonDecoder[LocalizedString] = mapDecoder.map { m =>
      LocalizedString(m.map { case (k, v) => Language.fromCode(k) -> v })
    }
    JsonCodec(encoder, decoder)

  // ── Opaque ID types ──────────────────────────────────────────────

  given JsonCodec[CategoryId] =
    JsonCodec.string.transform(CategoryId.unsafe(_), _.value)

  given JsonFieldEncoder[CategoryId] = JsonFieldEncoder.string.contramap(_.value)
  given JsonFieldDecoder[CategoryId] = JsonFieldDecoder.string.map(CategoryId.unsafe(_))

  given JsonCodec[MaterialId] =
    JsonCodec.string.transform(MaterialId.unsafe(_), _.value)

  given JsonFieldEncoder[MaterialId] = JsonFieldEncoder.string.contramap(_.value)
  given JsonFieldDecoder[MaterialId] = JsonFieldDecoder.string.map(MaterialId.unsafe(_))

  given JsonCodec[FinishId] =
    JsonCodec.string.transform(FinishId.unsafe(_), _.value)

  given JsonFieldEncoder[FinishId] = JsonFieldEncoder.string.contramap(_.value)
  given JsonFieldDecoder[FinishId] = JsonFieldDecoder.string.map(FinishId.unsafe(_))

  given JsonCodec[PrintingMethodId] =
    JsonCodec.string.transform(PrintingMethodId.unsafe(_), _.value)

  given JsonFieldEncoder[PrintingMethodId] = JsonFieldEncoder.string.contramap(_.value)
  given JsonFieldDecoder[PrintingMethodId] = JsonFieldDecoder.string.map(PrintingMethodId.unsafe(_))

  given JsonCodec[ConfigurationId] =
    JsonCodec.string.transform(ConfigurationId.unsafe(_), _.value)

  given JsonCodec[BasketId] =
    JsonCodec.string.transform(BasketId.unsafe(_), _.value)

  given JsonCodec[OrderId] =
    JsonCodec.string.transform(OrderId.unsafe(_), _.value)

  // ── Opaque numeric types ─────────────────────────────────────────

  given JsonCodec[PaperWeight] =
    JsonCodec.int.transform(PaperWeight.unsafe(_), _.gsm)

  given JsonCodec[Quantity] =
    JsonCodec.int.transform(Quantity.unsafe(_), _.value)

  given JsonCodec[Money] =
    JsonCodec.string.transform(s => Money(BigDecimal(s)), _.value.toString)

  // ── Simple enums ─────────────────────────────────────────────────

  given JsonCodec[MaterialFamily] = JsonCodec.string.transform(
    s => MaterialFamily.valueOf(s),
    _.toString,
  )

  given JsonCodec[MaterialProperty] = JsonCodec.string.transform(
    s => MaterialProperty.valueOf(s),
    _.toString,
  )

  given JsonCodec[FinishCategory] = JsonCodec.string.transform(
    s => FinishCategory.valueOf(s),
    _.toString,
  )

  given JsonCodec[FinishType] = JsonCodec.string.transform(
    s => FinishType.valueOf(s),
    _.toString,
  )

  given JsonCodec[FinishSide] = JsonCodec.string.transform(
    s => FinishSide.valueOf(s),
    _.toString,
  )

  given JsonCodec[FoilColor] = JsonCodec.string.transform(
    s => FoilColor.valueOf(s),
    _.toString,
  )

  given JsonCodec[ComponentRole] = JsonCodec.string.transform(
    s => ComponentRole.valueOf(s),
    _.toString,
  )

  given JsonCodec[InkType] = JsonCodec.string.transform(
    s => InkType.valueOf(s),
    _.toString,
  )

  given JsonCodec[Orientation] = JsonCodec.string.transform(
    s => Orientation.valueOf(s),
    _.toString,
  )

  given JsonCodec[FoldType] = JsonCodec.string.transform(
    s => FoldType.valueOf(s),
    _.toString,
  )

  given JsonCodec[BindingMethod] = JsonCodec.string.transform(
    s => BindingMethod.valueOf(s),
    _.toString,
  )

  given JsonCodec[SpecKind] = JsonCodec.string.transform(
    s => SpecKind.valueOf(s),
    _.toString,
  )

  given JsonFieldEncoder[SpecKind] = JsonFieldEncoder.string.contramap(_.toString)
  given JsonFieldDecoder[SpecKind] = JsonFieldDecoder.string.map(SpecKind.valueOf(_))

  given JsonCodec[Currency] = JsonCodec.string.transform(
    s => Currency.valueOf(s),
    _.toString,
  )

  given JsonCodec[PrintingProcessType] = JsonCodec.string.transform(
    s => PrintingProcessType.valueOf(s),
    _.toString,
  )

  // ── Case classes ─────────────────────────────────────────────────

  given JsonCodec[Dimension] = DeriveJsonCodec.gen[Dimension]
  given JsonCodec[InkSetup] = DeriveJsonCodec.gen[InkSetup]
  given JsonCodec[InkConfiguration] = DeriveJsonCodec.gen[InkConfiguration]
  given JsonCodec[Material] = DeriveJsonCodec.gen[Material]
  given JsonCodec[PrintingMethod] = DeriveJsonCodec.gen[PrintingMethod]
  given JsonCodec[Finish] = DeriveJsonCodec.gen[Finish]

  // ── FinishParameters sealed trait ────────────────────────────────

  given JsonCodec[FinishParameters.RoundCornersParams] = DeriveJsonCodec.gen
  given JsonCodec[FinishParameters.LaminationParams] = DeriveJsonCodec.gen
  given JsonCodec[FinishParameters.FoilStampingParams] = DeriveJsonCodec.gen
  given JsonCodec[FinishParameters.GrommetParams] = DeriveJsonCodec.gen
  given JsonCodec[FinishParameters.PerforationParams] = DeriveJsonCodec.gen

  given JsonCodec[FinishParameters] =
    val encoder: JsonEncoder[FinishParameters] = JsonEncoder[zio.json.ast.Json].contramap { fp =>
      import zio.json.ast.Json.*
      fp match
        case p: FinishParameters.RoundCornersParams =>
          Obj("type" -> Str("RoundCornersParams"), "cornerCount" -> Num(p.cornerCount), "radiusMm" -> Num(p.radiusMm))
        case p: FinishParameters.LaminationParams =>
          Obj("type" -> Str("LaminationParams"), "side" -> Str(p.side.toString))
        case p: FinishParameters.FoilStampingParams =>
          Obj("type" -> Str("FoilStampingParams"), "color" -> Str(p.color.toString))
        case p: FinishParameters.GrommetParams =>
          Obj("type" -> Str("GrommetParams"), "spacingMm" -> Num(p.spacingMm))
        case p: FinishParameters.PerforationParams =>
          Obj("type" -> Str("PerforationParams"), "pitchMm" -> Num(p.pitchMm))
    }
    val decoder: JsonDecoder[FinishParameters] = JsonDecoder[zio.json.ast.Json].mapOrFail { json =>
      import zio.json.ast.Json.*
      json match
        case Obj(fields) =>
          val fieldMap = fields.toMap
          fieldMap.get("type") match
            case Some(Str("RoundCornersParams")) =>
              for
                cc <- fieldMap.get("cornerCount").collect { case Num(n) => n.intValue }.toRight("missing cornerCount")
                r  <- fieldMap.get("radiusMm").collect { case Num(n) => n.intValue }.toRight("missing radiusMm")
              yield FinishParameters.RoundCornersParams(cc, r)
            case Some(Str("LaminationParams")) =>
              fieldMap.get("side").collect { case Str(s) => FinishParameters.LaminationParams(FinishSide.valueOf(s)) }
                .toRight("missing or invalid side")
            case Some(Str("FoilStampingParams")) =>
              fieldMap.get("color").collect { case Str(s) => FinishParameters.FoilStampingParams(FoilColor.valueOf(s)) }
                .toRight("missing or invalid color")
            case Some(Str("GrommetParams")) =>
              fieldMap.get("spacingMm").collect { case Num(n) => FinishParameters.GrommetParams(n.intValue) }
                .toRight("missing spacingMm")
            case Some(Str("PerforationParams")) =>
              fieldMap.get("pitchMm").collect { case Num(n) => FinishParameters.PerforationParams(n.intValue) }
                .toRight("missing pitchMm")
            case _ => Left("unknown FinishParameters type")
        case _ => Left("expected JSON object for FinishParameters")
    }
    JsonCodec(encoder, decoder)

  given JsonCodec[SelectedFinish] = DeriveJsonCodec.gen[SelectedFinish]
  given JsonCodec[FinishSelection] = DeriveJsonCodec.gen[FinishSelection]

  // ── Component types ──────────────────────────────────────────────

  given JsonCodec[ComponentTemplate] = DeriveJsonCodec.gen[ComponentTemplate]
  given JsonCodec[ProductComponent] = DeriveJsonCodec.gen[ProductComponent]
  given JsonCodec[ComponentRequest] = DeriveJsonCodec.gen[ComponentRequest]

  // ── Category & Catalog ───────────────────────────────────────────

  given JsonCodec[ProductCategory] = DeriveJsonCodec.gen[ProductCategory]
  given JsonCodec[ProductCatalog] = DeriveJsonCodec.gen[ProductCatalog]

  // ── Specification types ──────────────────────────────────────────

  given JsonCodec[SpecValue] =
    val encoder: JsonEncoder[SpecValue] = JsonEncoder[zio.json.ast.Json].contramap { sv =>
      import zio.json.ast.Json.*
      sv match
        case SpecValue.SizeSpec(d) =>
          Obj("type" -> Str("SizeSpec"), "widthMm" -> Num(d.widthMm), "heightMm" -> Num(d.heightMm))
        case SpecValue.QuantitySpec(q) =>
          Obj("type" -> Str("QuantitySpec"), "quantity" -> Num(q.value))
        case SpecValue.OrientationSpec(o) =>
          Obj("type" -> Str("OrientationSpec"), "orientation" -> Str(o.toString))
        case SpecValue.BleedSpec(b) =>
          Obj("type" -> Str("BleedSpec"), "bleedMm" -> Num(b))
        case SpecValue.PagesSpec(c) =>
          Obj("type" -> Str("PagesSpec"), "count" -> Num(c))
        case SpecValue.FoldTypeSpec(f) =>
          Obj("type" -> Str("FoldTypeSpec"), "foldType" -> Str(f.toString))
        case SpecValue.BindingMethodSpec(m) =>
          Obj("type" -> Str("BindingMethodSpec"), "method" -> Str(m.toString))
    }
    val decoder: JsonDecoder[SpecValue] = JsonDecoder[zio.json.ast.Json].mapOrFail { json =>
      import zio.json.ast.Json.*
      json match
        case Obj(fields) =>
          val m = fields.toMap
          m.get("type") match
            case Some(Str("SizeSpec")) =>
              for
                w <- m.get("widthMm").collect { case Num(n) => n.doubleValue }.toRight("missing widthMm")
                h <- m.get("heightMm").collect { case Num(n) => n.doubleValue }.toRight("missing heightMm")
              yield SpecValue.SizeSpec(Dimension(w, h))
            case Some(Str("QuantitySpec")) =>
              m.get("quantity").collect { case Num(n) => SpecValue.QuantitySpec(Quantity.unsafe(n.intValue)) }
                .toRight("missing quantity")
            case Some(Str("OrientationSpec")) =>
              m.get("orientation").collect { case Str(s) => SpecValue.OrientationSpec(Orientation.valueOf(s)) }
                .toRight("missing orientation")
            case Some(Str("BleedSpec")) =>
              m.get("bleedMm").collect { case Num(n) => SpecValue.BleedSpec(n.doubleValue) }
                .toRight("missing bleedMm")
            case Some(Str("PagesSpec")) =>
              m.get("count").collect { case Num(n) => SpecValue.PagesSpec(n.intValue) }
                .toRight("missing count")
            case Some(Str("FoldTypeSpec")) =>
              m.get("foldType").collect { case Str(s) => SpecValue.FoldTypeSpec(FoldType.valueOf(s)) }
                .toRight("missing foldType")
            case Some(Str("BindingMethodSpec")) =>
              m.get("method").collect { case Str(s) => SpecValue.BindingMethodSpec(BindingMethod.valueOf(s)) }
                .toRight("missing method")
            case _ => Left("unknown SpecValue type")
        case _ => Left("expected JSON object for SpecValue")
    }
    JsonCodec(encoder, decoder)

  given JsonCodec[ProductSpecifications] = DeriveJsonCodec.gen[ProductSpecifications]

  // ── Pricing types ────────────────────────────────────────────────

  given JsonCodec[Price] = DeriveJsonCodec.gen[Price]

  given JsonCodec[PricingRule] =
    val encoder: JsonEncoder[PricingRule] = JsonEncoder[zio.json.ast.Json].contramap { rule =>
      import zio.json.ast.Json.*
      def moneyField(m: Money): zio.json.ast.Json = Str(m.value.toString)
      def optInt(o: Option[Int]): zio.json.ast.Json = o.fold[zio.json.ast.Json](Null)(n => Num(n))
      rule match
        case PricingRule.MaterialBasePrice(mid, up) =>
          Obj("type" -> Str("MaterialBasePrice"), "materialId" -> Str(mid.value), "unitPrice" -> moneyField(up))
        case PricingRule.MaterialAreaPrice(mid, p) =>
          Obj("type" -> Str("MaterialAreaPrice"), "materialId" -> Str(mid.value), "pricePerSqMeter" -> moneyField(p))
        case PricingRule.MaterialSheetPrice(mid, pps, sw, sh, b, g) =>
          Obj("type" -> Str("MaterialSheetPrice"), "materialId" -> Str(mid.value),
            "pricePerSheet" -> moneyField(pps), "sheetWidthMm" -> Num(sw), "sheetHeightMm" -> Num(sh),
            "bleedMm" -> Num(b), "gutterMm" -> Num(g))
        case PricingRule.FinishSurcharge(fid, s) =>
          Obj("type" -> Str("FinishSurcharge"), "finishId" -> Str(fid.value), "surchargePerUnit" -> moneyField(s))
        case PricingRule.FinishTypeSurcharge(ft, s) =>
          Obj("type" -> Str("FinishTypeSurcharge"), "finishType" -> Str(ft.toString), "surchargePerUnit" -> moneyField(s))
        case PricingRule.PrintingProcessSurcharge(pt, s) =>
          Obj("type" -> Str("PrintingProcessSurcharge"), "processType" -> Str(pt.toString), "surchargePerUnit" -> moneyField(s))
        case PricingRule.CategorySurcharge(cid, s) =>
          Obj("type" -> Str("CategorySurcharge"), "categoryId" -> Str(cid.value), "surchargePerUnit" -> moneyField(s))
        case PricingRule.QuantityTier(min, max, mult) =>
          Obj("type" -> Str("QuantityTier"), "minQuantity" -> Num(min), "maxQuantity" -> optInt(max), "multiplier" -> Str(mult.toString))
        case PricingRule.SheetQuantityTier(min, max, mult) =>
          Obj("type" -> Str("SheetQuantityTier"), "minSheets" -> Num(min), "maxSheets" -> optInt(max), "multiplier" -> Str(mult.toString))
        case PricingRule.InkConfigurationFactor(fc, bc, mult) =>
          Obj("type" -> Str("InkConfigurationFactor"), "frontColorCount" -> Num(fc), "backColorCount" -> Num(bc), "materialMultiplier" -> Str(mult.toString))
        case PricingRule.CuttingSurcharge(c) =>
          Obj("type" -> Str("CuttingSurcharge"), "costPerCut" -> moneyField(c))
        case PricingRule.FinishTypeSetupFee(ft, c) =>
          Obj("type" -> Str("FinishTypeSetupFee"), "finishType" -> Str(ft.toString), "setupCost" -> moneyField(c))
        case PricingRule.FinishSetupFee(fid, c) =>
          Obj("type" -> Str("FinishSetupFee"), "finishId" -> Str(fid.value), "setupCost" -> moneyField(c))
        case PricingRule.FoldTypeSurcharge(ft, s) =>
          Obj("type" -> Str("FoldTypeSurcharge"), "foldType" -> Str(ft.toString), "surchargePerUnit" -> moneyField(s))
        case PricingRule.BindingMethodSurcharge(bm, s) =>
          Obj("type" -> Str("BindingMethodSurcharge"), "bindingMethod" -> Str(bm.toString), "surchargePerUnit" -> moneyField(s))
        case PricingRule.FoldTypeSetupFee(ft, c) =>
          Obj("type" -> Str("FoldTypeSetupFee"), "foldType" -> Str(ft.toString), "setupCost" -> moneyField(c))
        case PricingRule.BindingMethodSetupFee(bm, c) =>
          Obj("type" -> Str("BindingMethodSetupFee"), "bindingMethod" -> Str(bm.toString), "setupCost" -> moneyField(c))
        case PricingRule.MinimumOrderPrice(m) =>
          Obj("type" -> Str("MinimumOrderPrice"), "minTotal" -> moneyField(m))
    }
    val decoder: JsonDecoder[PricingRule] = JsonDecoder[zio.json.ast.Json].mapOrFail { json =>
      import zio.json.ast.Json.*
      json match
        case Obj(fields) =>
          val f = fields.toMap
          def money(key: String): Either[String, Money] =
            f.get(key).collect { case Str(s) => Money(BigDecimal(s)) }.toRight(s"missing $key")
          def str(key: String): Either[String, String] =
            f.get(key).collect { case Str(s) => s }.toRight(s"missing $key")
          def num(key: String): Either[String, scala.BigDecimal] =
            f.get(key).collect { case Num(n) => scala.BigDecimal(n) }.toRight(s"missing $key")
          def intF(key: String): Either[String, Int] = num(key).map(_.intValue)
          def optIntF(key: String): Either[String, Option[Int]] =
            Right(f.get(key).flatMap { case Num(n) => Some(scala.BigDecimal(n).intValue); case Null => None; case _ => None })
          def doubleF(key: String): Either[String, Double] = num(key).map(_.doubleValue)

          f.get("type") match
            case Some(Str("MaterialBasePrice")) =>
              for mid <- str("materialId"); up <- money("unitPrice")
              yield PricingRule.MaterialBasePrice(MaterialId.unsafe(mid), up)
            case Some(Str("MaterialAreaPrice")) =>
              for mid <- str("materialId"); p <- money("pricePerSqMeter")
              yield PricingRule.MaterialAreaPrice(MaterialId.unsafe(mid), p)
            case Some(Str("MaterialSheetPrice")) =>
              for
                mid <- str("materialId"); pps <- money("pricePerSheet")
                sw <- doubleF("sheetWidthMm"); sh <- doubleF("sheetHeightMm")
                b <- doubleF("bleedMm"); g <- doubleF("gutterMm")
              yield PricingRule.MaterialSheetPrice(MaterialId.unsafe(mid), pps, sw, sh, b, g)
            case Some(Str("FinishSurcharge")) =>
              for fid <- str("finishId"); s <- money("surchargePerUnit")
              yield PricingRule.FinishSurcharge(FinishId.unsafe(fid), s)
            case Some(Str("FinishTypeSurcharge")) =>
              for ft <- str("finishType"); s <- money("surchargePerUnit")
              yield PricingRule.FinishTypeSurcharge(FinishType.valueOf(ft), s)
            case Some(Str("PrintingProcessSurcharge")) =>
              for pt <- str("processType"); s <- money("surchargePerUnit")
              yield PricingRule.PrintingProcessSurcharge(PrintingProcessType.valueOf(pt), s)
            case Some(Str("CategorySurcharge")) =>
              for cid <- str("categoryId"); s <- money("surchargePerUnit")
              yield PricingRule.CategorySurcharge(CategoryId.unsafe(cid), s)
            case Some(Str("QuantityTier")) =>
              for min <- intF("minQuantity"); max <- optIntF("maxQuantity"); m <- str("multiplier")
              yield PricingRule.QuantityTier(min, max, BigDecimal(m))
            case Some(Str("SheetQuantityTier")) =>
              for min <- intF("minSheets"); max <- optIntF("maxSheets"); m <- str("multiplier")
              yield PricingRule.SheetQuantityTier(min, max, BigDecimal(m))
            case Some(Str("InkConfigurationFactor")) =>
              for fc <- intF("frontColorCount"); bc <- intF("backColorCount"); m <- str("materialMultiplier")
              yield PricingRule.InkConfigurationFactor(fc, bc, BigDecimal(m))
            case Some(Str("CuttingSurcharge")) =>
              money("costPerCut").map(PricingRule.CuttingSurcharge(_))
            case Some(Str("FinishTypeSetupFee")) =>
              for ft <- str("finishType"); c <- money("setupCost")
              yield PricingRule.FinishTypeSetupFee(FinishType.valueOf(ft), c)
            case Some(Str("FinishSetupFee")) =>
              for fid <- str("finishId"); c <- money("setupCost")
              yield PricingRule.FinishSetupFee(FinishId.unsafe(fid), c)
            case Some(Str("FoldTypeSurcharge")) =>
              for ft <- str("foldType"); s <- money("surchargePerUnit")
              yield PricingRule.FoldTypeSurcharge(FoldType.valueOf(ft), s)
            case Some(Str("BindingMethodSurcharge")) =>
              for bm <- str("bindingMethod"); s <- money("surchargePerUnit")
              yield PricingRule.BindingMethodSurcharge(BindingMethod.valueOf(bm), s)
            case Some(Str("FoldTypeSetupFee")) =>
              for ft <- str("foldType"); c <- money("setupCost")
              yield PricingRule.FoldTypeSetupFee(FoldType.valueOf(ft), c)
            case Some(Str("BindingMethodSetupFee")) =>
              for bm <- str("bindingMethod"); c <- money("setupCost")
              yield PricingRule.BindingMethodSetupFee(BindingMethod.valueOf(bm), c)
            case Some(Str("MinimumOrderPrice")) =>
              money("minTotal").map(PricingRule.MinimumOrderPrice(_))
            case _ => Left("unknown PricingRule type")
        case _ => Left("expected JSON object for PricingRule")
    }
    JsonCodec(encoder, decoder)

  given JsonCodec[Pricelist] = DeriveJsonCodec.gen[Pricelist]

  // ── Rules / Predicates ───────────────────────────────────────────

  given JsonCodec[SpecPredicate] =
    val encoder: JsonEncoder[SpecPredicate] = JsonEncoder[zio.json.ast.Json].contramap { sp =>
      import zio.json.ast.Json.*
      sp match
        case SpecPredicate.MinDimension(w, h) =>
          Obj("type" -> Str("MinDimension"), "minWidthMm" -> Num(w), "minHeightMm" -> Num(h))
        case SpecPredicate.MaxDimension(w, h) =>
          Obj("type" -> Str("MaxDimension"), "maxWidthMm" -> Num(w), "maxHeightMm" -> Num(h))
        case SpecPredicate.MinQuantity(m) =>
          Obj("type" -> Str("MinQuantity"), "min" -> Num(m))
        case SpecPredicate.MaxQuantity(m) =>
          Obj("type" -> Str("MaxQuantity"), "max" -> Num(m))
        case SpecPredicate.AllowedBindingMethods(ms) =>
          Obj("type" -> Str("AllowedBindingMethods"), "methods" -> Arr(ms.map(m => Str(m.toString)).toSeq*))
        case SpecPredicate.AllowedFoldTypes(fts) =>
          Obj("type" -> Str("AllowedFoldTypes"), "foldTypes" -> Arr(fts.map(f => Str(f.toString)).toSeq*))
        case SpecPredicate.MinPages(m) =>
          Obj("type" -> Str("MinPages"), "min" -> Num(m))
        case SpecPredicate.MaxPages(m) =>
          Obj("type" -> Str("MaxPages"), "max" -> Num(m))
        case SpecPredicate.PagesDivisibleBy(n) =>
          Obj("type" -> Str("PagesDivisibleBy"), "n" -> Num(n))
    }
    val decoder: JsonDecoder[SpecPredicate] = JsonDecoder[zio.json.ast.Json].mapOrFail { json =>
      import zio.json.ast.Json.*
      json match
        case Obj(fields) =>
          val f = fields.toMap
          def num(k: String) = f.get(k).collect { case Num(n) => n }.toRight(s"missing $k")
          def strSet(k: String) = f.get(k).collect { case Arr(a) => a.collect { case Str(s) => s }.toSet }.toRight(s"missing $k")
          f.get("type") match
            case Some(Str("MinDimension")) => for w <- num("minWidthMm"); h <- num("minHeightMm") yield SpecPredicate.MinDimension(w.doubleValue, h.doubleValue)
            case Some(Str("MaxDimension")) => for w <- num("maxWidthMm"); h <- num("maxHeightMm") yield SpecPredicate.MaxDimension(w.doubleValue, h.doubleValue)
            case Some(Str("MinQuantity")) => num("min").map(n => SpecPredicate.MinQuantity(n.intValue))
            case Some(Str("MaxQuantity")) => num("max").map(n => SpecPredicate.MaxQuantity(n.intValue))
            case Some(Str("AllowedBindingMethods")) => strSet("methods").map(s => SpecPredicate.AllowedBindingMethods(s.map(BindingMethod.valueOf)))
            case Some(Str("AllowedFoldTypes")) => strSet("foldTypes").map(s => SpecPredicate.AllowedFoldTypes(s.map(FoldType.valueOf)))
            case Some(Str("MinPages")) => num("min").map(n => SpecPredicate.MinPages(n.intValue))
            case Some(Str("MaxPages")) => num("max").map(n => SpecPredicate.MaxPages(n.intValue))
            case Some(Str("PagesDivisibleBy")) => num("n").map(n => SpecPredicate.PagesDivisibleBy(n.intValue))
            case _ => Left("unknown SpecPredicate type")
        case _ => Left("expected JSON object for SpecPredicate")
    }
    JsonCodec(encoder, decoder)

  given JsonCodec[ConfigurationPredicate] =
    given spEncoder: JsonEncoder[SpecPredicate] = summon[JsonCodec[SpecPredicate]].encoder
    given spDecoder: JsonDecoder[SpecPredicate] = summon[JsonCodec[SpecPredicate]].decoder

    lazy val enc: JsonEncoder[ConfigurationPredicate] = JsonEncoder[zio.json.ast.Json].contramap { cp =>
      import zio.json.ast.Json.*
      cp match
        case ConfigurationPredicate.Spec(p) =>
          Obj("type" -> Str("Spec"), "predicate" -> spEncoder.encodeJson(p).fromJson[zio.json.ast.Json].getOrElse(Null))
        case ConfigurationPredicate.HasMaterialProperty(p) =>
          Obj("type" -> Str("HasMaterialProperty"), "property" -> Str(p.toString))
        case ConfigurationPredicate.HasMaterialFamily(f) =>
          Obj("type" -> Str("HasMaterialFamily"), "family" -> Str(f.toString))
        case ConfigurationPredicate.HasPrintingProcess(pt) =>
          Obj("type" -> Str("HasPrintingProcess"), "processType" -> Str(pt.toString))
        case ConfigurationPredicate.HasMinWeight(w) =>
          Obj("type" -> Str("HasMinWeight"), "minGsm" -> Num(w))
        case ConfigurationPredicate.AllowedInkTypes(its) =>
          Obj("type" -> Str("AllowedInkTypes"), "inkTypes" -> Arr(its.map(i => Str(i.toString)).toSeq*))
        case ConfigurationPredicate.MaxColorCountPerSide(m) =>
          Obj("type" -> Str("MaxColorCountPerSide"), "max" -> Num(m))
        case ConfigurationPredicate.BindingMethodIs(ms) =>
          Obj("type" -> Str("BindingMethodIs"), "methods" -> Arr(ms.map(m => Str(m.toString)).toSeq*))
        case ConfigurationPredicate.HasInkType(it) =>
          Obj("type" -> Str("HasInkType"), "inkType" -> Str(it.toString))
        case ConfigurationPredicate.And(l, r) =>
          Obj("type" -> Str("And"),
            "left" -> enc.encodeJson(l).fromJson[zio.json.ast.Json].getOrElse(Null),
            "right" -> enc.encodeJson(r).fromJson[zio.json.ast.Json].getOrElse(Null))
        case ConfigurationPredicate.Or(l, r) =>
          Obj("type" -> Str("Or"),
            "left" -> enc.encodeJson(l).fromJson[zio.json.ast.Json].getOrElse(Null),
            "right" -> enc.encodeJson(r).fromJson[zio.json.ast.Json].getOrElse(Null))
        case ConfigurationPredicate.Not(inner) =>
          Obj("type" -> Str("Not"),
            "inner" -> enc.encodeJson(inner).fromJson[zio.json.ast.Json].getOrElse(Null))
    }

    lazy val dec: JsonDecoder[ConfigurationPredicate] = JsonDecoder[zio.json.ast.Json].mapOrFail { json =>
      import zio.json.ast.Json.*
      json match
        case Obj(fields) =>
          val f = fields.toMap
          def str(k: String) = f.get(k).collect { case Str(s) => s }.toRight(s"missing $k")
          def num(k: String) = f.get(k).collect { case Num(n) => n }.toRight(s"missing $k")
          def strSet(k: String) = f.get(k).collect { case Arr(a) => a.collect { case Str(s) => s }.toSet }.toRight(s"missing $k")
          def subJson(k: String) = f.get(k).toRight(s"missing $k")

          f.get("type") match
            case Some(Str("Spec")) =>
              subJson("predicate").flatMap(j => spDecoder.decodeJson(j.toJson)).map(ConfigurationPredicate.Spec(_))
            case Some(Str("HasMaterialProperty")) =>
              str("property").map(s => ConfigurationPredicate.HasMaterialProperty(MaterialProperty.valueOf(s)))
            case Some(Str("HasMaterialFamily")) =>
              str("family").map(s => ConfigurationPredicate.HasMaterialFamily(MaterialFamily.valueOf(s)))
            case Some(Str("HasPrintingProcess")) =>
              str("processType").map(s => ConfigurationPredicate.HasPrintingProcess(PrintingProcessType.valueOf(s)))
            case Some(Str("HasMinWeight")) =>
              num("minGsm").map(n => ConfigurationPredicate.HasMinWeight(n.intValue))
            case Some(Str("AllowedInkTypes")) =>
              strSet("inkTypes").map(s => ConfigurationPredicate.AllowedInkTypes(s.map(InkType.valueOf)))
            case Some(Str("MaxColorCountPerSide")) =>
              num("max").map(n => ConfigurationPredicate.MaxColorCountPerSide(n.intValue))
            case Some(Str("BindingMethodIs")) =>
              strSet("methods").map(s => ConfigurationPredicate.BindingMethodIs(s.map(BindingMethod.valueOf)))
            case Some(Str("HasInkType")) =>
              str("inkType").map(s => ConfigurationPredicate.HasInkType(InkType.valueOf(s)))
            case Some(Str("And")) =>
              for
                l <- subJson("left").flatMap(j => dec.decodeJson(j.toJson))
                r <- subJson("right").flatMap(j => dec.decodeJson(j.toJson))
              yield ConfigurationPredicate.And(l, r)
            case Some(Str("Or")) =>
              for
                l <- subJson("left").flatMap(j => dec.decodeJson(j.toJson))
                r <- subJson("right").flatMap(j => dec.decodeJson(j.toJson))
              yield ConfigurationPredicate.Or(l, r)
            case Some(Str("Not")) =>
              subJson("inner").flatMap(j => dec.decodeJson(j.toJson)).map(ConfigurationPredicate.Not(_))
            case _ => Left("unknown ConfigurationPredicate type")
        case _ => Left("expected JSON object for ConfigurationPredicate")
    }
    JsonCodec(enc, dec)

  given JsonCodec[CompatibilityRule] =
    val encoder: JsonEncoder[CompatibilityRule] = JsonEncoder[zio.json.ast.Json].contramap { rule =>
      import zio.json.ast.Json.*
      rule match
        case CompatibilityRule.MaterialFinishIncompatible(mid, fid, reason) =>
          Obj("type" -> Str("MaterialFinishIncompatible"), "materialId" -> Str(mid.value), "finishId" -> Str(fid.value), "reason" -> Str(reason))
        case CompatibilityRule.MaterialRequiresFinish(mid, fids, reason) =>
          Obj("type" -> Str("MaterialRequiresFinish"), "materialId" -> Str(mid.value), "requiredFinishIds" -> Arr(fids.map(id => Str(id.value)).toSeq*), "reason" -> Str(reason))
        case CompatibilityRule.FinishRequiresMaterialProperty(fid, prop, reason) =>
          Obj("type" -> Str("FinishRequiresMaterialProperty"), "finishId" -> Str(fid.value), "requiredProperty" -> Str(prop.toString), "reason" -> Str(reason))
        case CompatibilityRule.FinishMutuallyExclusive(a, b, reason) =>
          Obj("type" -> Str("FinishMutuallyExclusive"), "finishIdA" -> Str(a.value), "finishIdB" -> Str(b.value), "reason" -> Str(reason))
        case CompatibilityRule.SpecConstraint(cid, pred, reason) =>
          Obj("type" -> Str("SpecConstraint"), "categoryId" -> Str(cid.value),
            "predicate" -> summon[JsonCodec[SpecPredicate]].encoder.encodeJson(pred).fromJson[zio.json.ast.Json].getOrElse(Null),
            "reason" -> Str(reason))
        case CompatibilityRule.MaterialPropertyFinishTypeIncompatible(prop, ft, reason) =>
          Obj("type" -> Str("MaterialPropertyFinishTypeIncompatible"), "property" -> Str(prop.toString), "finishType" -> Str(ft.toString), "reason" -> Str(reason))
        case CompatibilityRule.MaterialFamilyFinishTypeIncompatible(fam, ft, reason) =>
          Obj("type" -> Str("MaterialFamilyFinishTypeIncompatible"), "family" -> Str(fam.toString), "finishType" -> Str(ft.toString), "reason" -> Str(reason))
        case CompatibilityRule.MaterialWeightFinishType(ft, minW, reason) =>
          Obj("type" -> Str("MaterialWeightFinishType"), "finishType" -> Str(ft.toString), "minWeightGsm" -> Num(minW), "reason" -> Str(reason))
        case CompatibilityRule.FinishTypeMutuallyExclusive(a, b, reason) =>
          Obj("type" -> Str("FinishTypeMutuallyExclusive"), "finishTypeA" -> Str(a.toString), "finishTypeB" -> Str(b.toString), "reason" -> Str(reason))
        case CompatibilityRule.FinishCategoryExclusive(cat, reason) =>
          Obj("type" -> Str("FinishCategoryExclusive"), "category" -> Str(cat.toString), "reason" -> Str(reason))
        case CompatibilityRule.FinishRequiresFinishType(fid, rft, reason) =>
          Obj("type" -> Str("FinishRequiresFinishType"), "finishId" -> Str(fid.value), "requiredFinishType" -> Str(rft.toString), "reason" -> Str(reason))
        case CompatibilityRule.FinishRequiresPrintingProcess(ft, rpts, reason) =>
          Obj("type" -> Str("FinishRequiresPrintingProcess"), "finishType" -> Str(ft.toString),
            "requiredProcessTypes" -> Arr(rpts.map(p => Str(p.toString)).toSeq*), "reason" -> Str(reason))
        case CompatibilityRule.ConfigurationConstraint(cid, pred, reason) =>
          Obj("type" -> Str("ConfigurationConstraint"), "categoryId" -> Str(cid.value),
            "predicate" -> summon[JsonCodec[ConfigurationPredicate]].encoder.encodeJson(pred).fromJson[zio.json.ast.Json].getOrElse(Null),
            "reason" -> Str(reason))
        case CompatibilityRule.TechnologyConstraint(pred, reason) =>
          Obj("type" -> Str("TechnologyConstraint"),
            "predicate" -> summon[JsonCodec[ConfigurationPredicate]].encoder.encodeJson(pred).fromJson[zio.json.ast.Json].getOrElse(Null),
            "reason" -> Str(reason))
    }
    val decoder: JsonDecoder[CompatibilityRule] = JsonDecoder[zio.json.ast.Json].mapOrFail { json =>
      import zio.json.ast.Json.*
      json match
        case Obj(fields) =>
          val f = fields.toMap
          def str(k: String) = f.get(k).collect { case Str(s) => s }.toRight(s"missing $k")
          def num(k: String) = f.get(k).collect { case Num(n) => n }.toRight(s"missing $k")
          def strSet(k: String) = f.get(k).collect { case Arr(a) => a.collect { case Str(s) => s }.toSet }.toRight(s"missing $k")
          def subJson(k: String) = f.get(k).toRight(s"missing $k")

          f.get("type") match
            case Some(Str("MaterialFinishIncompatible")) =>
              for mid <- str("materialId"); fid <- str("finishId"); r <- str("reason")
              yield CompatibilityRule.MaterialFinishIncompatible(MaterialId.unsafe(mid), FinishId.unsafe(fid), r)
            case Some(Str("MaterialRequiresFinish")) =>
              for mid <- str("materialId"); fids <- strSet("requiredFinishIds"); r <- str("reason")
              yield CompatibilityRule.MaterialRequiresFinish(MaterialId.unsafe(mid), fids.map(FinishId.unsafe), r)
            case Some(Str("FinishRequiresMaterialProperty")) =>
              for fid <- str("finishId"); prop <- str("requiredProperty"); r <- str("reason")
              yield CompatibilityRule.FinishRequiresMaterialProperty(FinishId.unsafe(fid), MaterialProperty.valueOf(prop), r)
            case Some(Str("FinishMutuallyExclusive")) =>
              for a <- str("finishIdA"); b <- str("finishIdB"); r <- str("reason")
              yield CompatibilityRule.FinishMutuallyExclusive(FinishId.unsafe(a), FinishId.unsafe(b), r)
            case Some(Str("SpecConstraint")) =>
              for
                cid <- str("categoryId")
                pred <- subJson("predicate").flatMap(j => summon[JsonCodec[SpecPredicate]].decoder.decodeJson(j.toJson))
                r <- str("reason")
              yield CompatibilityRule.SpecConstraint(CategoryId.unsafe(cid), pred, r)
            case Some(Str("MaterialPropertyFinishTypeIncompatible")) =>
              for p <- str("property"); ft <- str("finishType"); r <- str("reason")
              yield CompatibilityRule.MaterialPropertyFinishTypeIncompatible(MaterialProperty.valueOf(p), FinishType.valueOf(ft), r)
            case Some(Str("MaterialFamilyFinishTypeIncompatible")) =>
              for fam <- str("family"); ft <- str("finishType"); r <- str("reason")
              yield CompatibilityRule.MaterialFamilyFinishTypeIncompatible(MaterialFamily.valueOf(fam), FinishType.valueOf(ft), r)
            case Some(Str("MaterialWeightFinishType")) =>
              for ft <- str("finishType"); w <- num("minWeightGsm"); r <- str("reason")
              yield CompatibilityRule.MaterialWeightFinishType(FinishType.valueOf(ft), w.intValue, r)
            case Some(Str("FinishTypeMutuallyExclusive")) =>
              for a <- str("finishTypeA"); b <- str("finishTypeB"); r <- str("reason")
              yield CompatibilityRule.FinishTypeMutuallyExclusive(FinishType.valueOf(a), FinishType.valueOf(b), r)
            case Some(Str("FinishCategoryExclusive")) =>
              for c <- str("category"); r <- str("reason")
              yield CompatibilityRule.FinishCategoryExclusive(FinishCategory.valueOf(c), r)
            case Some(Str("FinishRequiresFinishType")) =>
              for fid <- str("finishId"); rft <- str("requiredFinishType"); r <- str("reason")
              yield CompatibilityRule.FinishRequiresFinishType(FinishId.unsafe(fid), FinishType.valueOf(rft), r)
            case Some(Str("FinishRequiresPrintingProcess")) =>
              for ft <- str("finishType"); pts <- strSet("requiredProcessTypes"); r <- str("reason")
              yield CompatibilityRule.FinishRequiresPrintingProcess(FinishType.valueOf(ft), pts.map(PrintingProcessType.valueOf), r)
            case Some(Str("ConfigurationConstraint")) =>
              for
                cid <- str("categoryId")
                pred <- subJson("predicate").flatMap(j => summon[JsonCodec[ConfigurationPredicate]].decoder.decodeJson(j.toJson))
                r <- str("reason")
              yield CompatibilityRule.ConfigurationConstraint(CategoryId.unsafe(cid), pred, r)
            case Some(Str("TechnologyConstraint")) =>
              for
                pred <- subJson("predicate").flatMap(j => summon[JsonCodec[ConfigurationPredicate]].decoder.decodeJson(j.toJson))
                r <- str("reason")
              yield CompatibilityRule.TechnologyConstraint(pred, r)
            case _ => Left("unknown CompatibilityRule type")
        case _ => Left("expected JSON object for CompatibilityRule")
    }
    JsonCodec(encoder, decoder)

  given JsonCodec[CompatibilityRuleset] = DeriveJsonCodec.gen[CompatibilityRuleset]
