package mpbuilder.product.codec

import mpbuilder.commons.*
import mpbuilder.product.model.*
import zio.json.*

/** JSON codecs for everything catalog owns.
  *
  * They live with the model they encode, so a catalog type and its wire form move together. What
  * stayed behind in the legacy module is pricing's half — pricelists and pricing rules — which
  * follows pricing when that context is extracted.
  */
object ProductCodecs:


  // ── Opaque type IDs ──────────────────────────────────────────────────────

  given JsonEncoder[CategoryId] = JsonEncoder[String].contramap(_.value)
  given JsonDecoder[CategoryId] = JsonDecoder[String].map(CategoryId.unsafe)

  given JsonEncoder[MaterialId] = JsonEncoder[String].contramap(_.value)
  given JsonDecoder[MaterialId] = JsonDecoder[String].map(MaterialId.unsafe)

  given JsonEncoder[FinishId] = JsonEncoder[String].contramap(_.value)
  given JsonDecoder[FinishId] = JsonDecoder[String].map(FinishId.unsafe)

  given JsonEncoder[PrintingMethodId] = JsonEncoder[String].contramap(_.value)
  given JsonDecoder[PrintingMethodId] = JsonDecoder[String].map(PrintingMethodId.unsafe)

  given JsonEncoder[ConfigurationId] = JsonEncoder[String].contramap(_.value)
  given JsonDecoder[ConfigurationId] = JsonDecoder[String].map(ConfigurationId.unsafe)

  // ── Language & LocalizedString ───────────────────────────────────────────

  given JsonEncoder[Language] = JsonEncoder[String].contramap(_.toCode)
  given JsonDecoder[Language] = JsonDecoder[String].map(Language.fromCode)

  given JsonEncoder[LocalizedString] =
    JsonEncoder[Map[String, String]].contramap { ls =>
      Language.values.flatMap(l => Option(ls(l)).filter(_.nonEmpty).map(l.toCode -> _)).toMap
    }
  given JsonDecoder[LocalizedString] =
    JsonDecoder[Map[String, String]].map { m =>
      LocalizedString(m.map { case (k, v) => Language.fromCode(k) -> v })
    }

  // ── Money & Currency ─────────────────────────────────────────────────────

  given JsonEncoder[Money] = JsonEncoder[BigDecimal].contramap(_.value)
  given JsonDecoder[Money] = JsonDecoder[BigDecimal].map(Money.apply)

  given JsonEncoder[Currency] = JsonEncoder[String].contramap(_.toString)
  given JsonDecoder[Currency] = JsonDecoder[String].map(Currency.valueOf)

  given JsonCodec[Price] = DeriveJsonCodec.gen[Price]

  // ── Material types ───────────────────────────────────────────────────────

  given JsonEncoder[MaterialFamily] = JsonEncoder[String].contramap(_.toString)
  given JsonDecoder[MaterialFamily] = JsonDecoder[String].map(MaterialFamily.valueOf)

  given JsonEncoder[MaterialProperty] = JsonEncoder[String].contramap(_.toString)
  given JsonDecoder[MaterialProperty] = JsonDecoder[String].map(MaterialProperty.valueOf)

  given JsonEncoder[PaperWeight] = JsonEncoder[Int].contramap(_.gsm)
  given JsonDecoder[PaperWeight] = JsonDecoder[Int].map(PaperWeight.unsafe)

  given JsonCodec[Material] = DeriveJsonCodec.gen[Material]

  // ── Finish types ─────────────────────────────────────────────────────────

  given JsonEncoder[FinishCategory] = JsonEncoder[String].contramap(_.toString)
  given JsonDecoder[FinishCategory] = JsonDecoder[String].map(FinishCategory.valueOf)

  given JsonEncoder[FinishType] = JsonEncoder[String].contramap(_.toString)
  given JsonDecoder[FinishType] = JsonDecoder[String].map(FinishType.valueOf)

  given JsonEncoder[FinishSide] = JsonEncoder[String].contramap(_.toString)
  given JsonDecoder[FinishSide] = JsonDecoder[String].map(FinishSide.valueOf)

  given JsonCodec[Finish] = DeriveJsonCodec.gen[Finish]

  given JsonEncoder[FoilColor] = JsonEncoder[String].contramap(_.toString)
  given JsonDecoder[FoilColor] = JsonDecoder[String].map(FoilColor.valueOf)

  // ── Printing method types ────────────────────────────────────────────────

  given JsonEncoder[PrintingProcessType] = JsonEncoder[String].contramap(_.toString)
  given JsonDecoder[PrintingProcessType] = JsonDecoder[String].map(PrintingProcessType.valueOf)

  given JsonCodec[PrintingMethod] = DeriveJsonCodec.gen[PrintingMethod]

  // ── Specification types ──────────────────────────────────────────────────

  given JsonEncoder[SpecKind] = JsonEncoder[String].contramap(_.toString)
  given JsonDecoder[SpecKind] = JsonDecoder[String].map(SpecKind.valueOf)

  given JsonCodec[Dimension] = DeriveJsonCodec.gen[Dimension]

  given JsonEncoder[Quantity] = JsonEncoder[Int].contramap(_.value)
  given JsonDecoder[Quantity] = JsonDecoder[Int].map(Quantity.unsafe)

  given JsonEncoder[InkType] = JsonEncoder[String].contramap(_.toString)
  given JsonDecoder[InkType] = JsonDecoder[String].map(InkType.valueOf)

  given JsonCodec[InkSetup] = DeriveJsonCodec.gen[InkSetup]
  given JsonCodec[InkConfiguration] = DeriveJsonCodec.gen[InkConfiguration]

  given JsonEncoder[Orientation] = JsonEncoder[String].contramap(_.toString)
  given JsonDecoder[Orientation] = JsonDecoder[String].map(Orientation.valueOf)

  given JsonEncoder[FoldType] = JsonEncoder[String].contramap(_.toString)
  given JsonDecoder[FoldType] = JsonDecoder[String].map(FoldType.valueOf)

  given JsonEncoder[BindingMethod] = JsonEncoder[String].contramap(_.toString)
  given JsonDecoder[BindingMethod] = JsonDecoder[String].map(BindingMethod.valueOf)

  // ── Manufacturing speed ──────────────────────────────────────────────────

  given JsonEncoder[ManufacturingSpeed] = JsonEncoder[String].contramap(_.toString)
  given JsonDecoder[ManufacturingSpeed] = JsonDecoder[String].map(ManufacturingSpeed.valueOf)


  // ── Component types ──────────────────────────────────────────────────────

  given JsonEncoder[ComponentRole] = JsonEncoder[String].contramap(_.toString)
  given JsonDecoder[ComponentRole] = JsonDecoder[String].map(ComponentRole.valueOf)

  given JsonCodec[ComponentTemplate] = DeriveJsonCodec.gen[ComponentTemplate]

  // ── Finish parameters & selections (needed by presets) ───────────────

  given JsonCodec[FinishParameters.RoundCornersParams] = DeriveJsonCodec.gen[FinishParameters.RoundCornersParams]
  given JsonCodec[FinishParameters.LaminationParams] = DeriveJsonCodec.gen[FinishParameters.LaminationParams]
  given JsonCodec[FinishParameters.FoilStampingParams] = DeriveJsonCodec.gen[FinishParameters.FoilStampingParams]
  given JsonCodec[FinishParameters.GrommetParams] = DeriveJsonCodec.gen[FinishParameters.GrommetParams]
  given JsonCodec[FinishParameters.PerforationParams] = DeriveJsonCodec.gen[FinishParameters.PerforationParams]
  given JsonCodec[FinishParameters.RopeParams] = DeriveJsonCodec.gen[FinishParameters.RopeParams]
  given JsonCodec[FinishParameters] = DeriveJsonCodec.gen[FinishParameters]

  given JsonCodec[FinishSelection] = DeriveJsonCodec.gen[FinishSelection]

  // ── Spec values (needed by presets) ──────────────────────────────────

  given JsonCodec[SpecValue] = DeriveJsonCodec.gen[SpecValue]

  // ── Preset types ─────────────────────────────────────────────────────

  given JsonEncoder[PresetId] = JsonEncoder[String].contramap(_.value)
  given JsonDecoder[PresetId] = JsonDecoder[String].map(PresetId.unsafe)

  given JsonCodec[ComponentPreset] = DeriveJsonCodec.gen[ComponentPreset]
  given JsonCodec[CategoryPreset] = DeriveJsonCodec.gen[CategoryPreset]

  // ── Category ─────────────────────────────────────────────────────────────

  given JsonCodec[ProductCategory] = DeriveJsonCodec.gen[ProductCategory]

  // ── Catalog ──────────────────────────────────────────────────────────────

  // Map codecs for ID-keyed maps — encode as arrays of [key, value] pairs
  given catMapEnc: JsonEncoder[Map[CategoryId, ProductCategory]] =
    JsonEncoder[List[(String, ProductCategory)]].contramap(_.map { case (k, v) => k.value -> v }.toList)
  given catMapDec: JsonDecoder[Map[CategoryId, ProductCategory]] =
    JsonDecoder[List[(String, ProductCategory)]].map(_.map { case (k, v) => CategoryId.unsafe(k) -> v }.toMap)

  given matMapEnc: JsonEncoder[Map[MaterialId, Material]] =
    JsonEncoder[List[(String, Material)]].contramap(_.map { case (k, v) => k.value -> v }.toList)
  given matMapDec: JsonDecoder[Map[MaterialId, Material]] =
    JsonDecoder[List[(String, Material)]].map(_.map { case (k, v) => MaterialId.unsafe(k) -> v }.toMap)

  given finMapEnc: JsonEncoder[Map[FinishId, Finish]] =
    JsonEncoder[List[(String, Finish)]].contramap(_.map { case (k, v) => k.value -> v }.toList)
  given finMapDec: JsonDecoder[Map[FinishId, Finish]] =
    JsonDecoder[List[(String, Finish)]].map(_.map { case (k, v) => FinishId.unsafe(k) -> v }.toMap)

  given pmMapEnc: JsonEncoder[Map[PrintingMethodId, PrintingMethod]] =
    JsonEncoder[List[(String, PrintingMethod)]].contramap(_.map { case (k, v) => k.value -> v }.toList)
  given pmMapDec: JsonDecoder[Map[PrintingMethodId, PrintingMethod]] =
    JsonDecoder[List[(String, PrintingMethod)]].map(_.map { case (k, v) => PrintingMethodId.unsafe(k) -> v }.toMap)

  given JsonCodec[ProductCatalog] = DeriveJsonCodec.gen[ProductCatalog]
