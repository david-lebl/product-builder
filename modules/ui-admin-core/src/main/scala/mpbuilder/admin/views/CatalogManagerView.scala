package mpbuilder.admin.views

import kyo.*
import kyo.UI.*
import kyo.Style.*
import mpbuilder.admin.state.BackofficeState
import mpbuilder.catalog.*
import mpbuilder.shared.*
import mpbuilder.ui.components.CommonComponents.*
import mpbuilder.ui.theme.AppTheme.*
import mpbuilder.ui.theme.AppTheme.Palette.*
import zio.NonEmptyChunk

/** Tabbed Catalog & Capabilities Manager:
  * Categories, Materials, Finishes, Printing Methods, Compatibility Rules & Showcase Listings.
  */
object CatalogManagerView:

  private enum Tab:
    case Categories, Materials, Finishes, Methods, Rules, Showcases

  def render(state: BackofficeState): UI < Sync =
    for
      tabRef <- Signal.initRef[Tab](Tab.Categories)
      // Category form
      catIdRef     <- Signal.initRef("")
      catNameEnRef <- Signal.initRef("")
      catNameCsRef <- Signal.initRef("")
      // Material form
      matIdRef     <- Signal.initRef("")
      matNameEnRef <- Signal.initRef("")
      matNameCsRef <- Signal.initRef("")
      matFamilyRef <- Signal.initRef[MaterialFamily](MaterialFamily.Paper)
      matCoatRef   <- Signal.initRef[SurfaceCoating](SurfaceCoating.CoatedSilk)
      matGsmRef    <- Signal.initRef("300")
      // Finish form
      finIdRef     <- Signal.initRef("")
      finNameEnRef <- Signal.initRef("")
      finNameCsRef <- Signal.initRef("")
      finTypeRef   <- Signal.initRef[FinishType](FinishType.Lamination)
      // Method form
      pmIdRef      <- Signal.initRef("")
      pmNameEnRef  <- Signal.initRef("")
      pmNameCsRef  <- Signal.initRef("")
      pmProcRef    <- Signal.initRef[PrintingProcessType](PrintingProcessType.Digital)
      pmDuplexRef  <- Signal.initRef(true)
      // Rule form
      ruleIdRef    <- Signal.initRef("")
      ruleSevRef   <- Signal.initRef[RuleSeverity](RuleSeverity.Warning)
      ruleEnRef    <- Signal.initRef("")
      ruleCsRef    <- Signal.initRef("")
      // Showcase form
      scCatIdRef   <- Signal.initRef("")
      scGroupRef   <- Signal.initRef[CatalogGroup](CatalogGroup.Sheet)
      scTagEnRef   <- Signal.initRef("")
      scTagCsRef   <- Signal.initRef("")
      scSortRef    <- Signal.initRef("10")
    yield
      val combined =
        tabRef
          .combineLatest(state.catalog)
          .combineLatest(state.ruleset)
          .combineLatest(state.auth)
          .combineLatest(state.i18n.language)

      combined.render { case ((((activeTab, catOpt), rulesOpt), authOpt), lang) =>
        val canManage = state.canManage(authOpt)

        div.style(stackMd.width(100.pct))(
          // Header
          div.style(rowBetween.flexWrap(FlexWrap.wrap))(
            div.style(stackXs)(
              h1(state.i18n.t("adm-cat-title")).style(Style.margin(0.px).fontSize(24.px).fontWeight(FontWeight.w800).color(ink)),
              p(state.i18n.t("adm-cat-subtitle")).style(mutedText.margin(0.px)),
            ),
            div.style(rowSm)(
              catOpt.map(c => badge(s"Catalog v${c.version.value}", BadgeTone.Brand)).getOrElse(UI.empty),
              rulesOpt.map(r => badge(s"Ruleset v${r.version.value}", BadgeTone.Neutral)).getOrElse(UI.empty),
            ),
          ),

          if !canManage then
            alertBanner(BadgeTone.Amber, span("Read-Only"), List(if lang == Language.Cs then "Úpravy katalogu vyžadují roli Manager nebo Admin." else "Catalog mutations require Manager or Admin role."))
          else UI.empty,

          // Sub-tabs
          div.style(rowWrap)(
            tabBtn(Tab.Categories, activeTab, tabRef, state.i18n.t("adm-cat-tab-categories"), catOpt.map(_.categories.size)),
            tabBtn(Tab.Materials, activeTab, tabRef, state.i18n.t("adm-cat-tab-materials"), catOpt.map(_.materials.size)),
            tabBtn(Tab.Finishes, activeTab, tabRef, state.i18n.t("adm-cat-tab-finishes"), catOpt.map(_.finishes.size)),
            tabBtn(Tab.Methods, activeTab, tabRef, state.i18n.t("adm-cat-tab-methods"), catOpt.map(_.printingMethods.size)),
            tabBtn(Tab.Rules, activeTab, tabRef, state.i18n.t("adm-cat-tab-rules"), rulesOpt.map(_.rules.size)),
            tabBtn(Tab.Showcases, activeTab, tabRef, state.i18n.t("adm-cat-tab-showcases"), catOpt.map(_.showcases.size)),
          ),

          // Tab Content
          (catOpt, activeTab) match
            case (None, _) =>
              div.cssClass("mp-card")(span("Loading catalog...").style(mutedText))

            case (Some(cat), Tab.Categories) =>
              div.style(stackMd)(
                if canManage then
                  div.cssClass("mp-card")(
                    h2(state.i18n.t("adm-cat-add-category")).style(sectionTitle.margin(0.px)),
                    div.cssClass("mp-field-row")(
                      textField("Category ID", "e.g. door-hangers", catIdRef),
                      textField("Name (EN)", "Door Hangers", catNameEnRef),
                      textField("Name (CS)", "Visačky na dveře", catNameCsRef),
                    ),
                    div.style(rowSm)(
                      button
                        .cssClass("mp-btn mp-btn-primary mp-btn-sm")
                        .onClick {
                          for
                            rawId <- catIdRef.get.map(_.trim)
                            en    <- catNameEnRef.get.map(_.trim)
                            cs    <- catNameCsRef.get.map(_.trim)
                            _ <-
                              if rawId.isEmpty || en.isEmpty then
                                state.errorBanner.set(Some("Category ID and English Name are required."))
                              else
                                val cid      = CategoryId.unsafe(rawId)
                                val existing = cat.categories.get(cid)
                                val locName  = LocalizedString(en, if cs.nonEmpty then cs else en)
                                val updated = existing match
                                  case Some(ex) => ex.copy(name = locName)
                                  case None =>
                                    val defaultMatIds = cat.materials.keys.take(3).toSet
                                    val defaultPmIds  = cat.printingMethods.keys.take(2).toSet
                                    val tmpl = ComponentTemplate(
                                      key = ComponentKey.Main,
                                      label = LocalizedString("Main", "Hlavní"),
                                      required = true,
                                      printable = true,
                                      allowedMaterialIds = defaultMatIds,
                                      allowedFinishIds = cat.finishes.keys.take(3).toSet,
                                    )
                                    ProductCategory(
                                      id = cid,
                                      name = locName,
                                      components = NonEmptyChunk.single(tmpl),
                                      allowedPrintingMethodIds = defaultPmIds,
                                    )
                                state.upsertCategory(updated).andThen(catIdRef.set("")).andThen(catNameEnRef.set("")).andThen(catNameCsRef.set(""))
                          yield ()
                        }(state.i18n.t("adm-cat-save")),
                    ),
                  )
                else UI.empty,

                div.cssClass("mp-card")(
                  div.style(stackSm)(
                    cat.categories.values.toList.sortBy(_.id.value).map { pc =>
                      div.style(rowBetween.flexWrap(FlexWrap.wrap).padding(12.px).rounded(10.px).bg(surfaceAlt).border(1.px, Palette.border))(
                        div.style(stackXs)(
                          div.style(rowSm.flexWrap(FlexWrap.wrap))(
                            span(pc.name(lang)).style(Style.fontWeight(FontWeight.w700).color(ink)),
                            badge(pc.id.value, BadgeTone.Neutral),
                            badge(s"${pc.presets.size} presets", BadgeTone.Brand),
                            badge(s"${pc.components.size} comp.", BadgeTone.Neutral),
                          ),
                          span(s"EN: ${pc.name(Language.En)} · CS: ${pc.name(Language.Cs)}").style(smallText),
                        ),
                        if canManage then
                          div.style(rowXs)(
                            button
                              .cssClass("mp-btn mp-btn-secondary mp-btn-sm")
                              .onClick(
                                catIdRef.set(pc.id.value)
                                  .andThen(catNameEnRef.set(pc.name(Language.En)))
                                  .andThen(catNameCsRef.set(pc.name(Language.Cs)))
                              )(state.i18n.t("adm-cat-edit")),
                            button
                              .cssClass("mp-btn mp-btn-danger mp-btn-sm")
                              .onClick(state.deleteCategory(pc.id))(state.i18n.t("adm-cat-delete")),
                          )
                        else UI.empty,
                      )
                    }*
                  ),
                ),
              )

            case (Some(cat), Tab.Materials) =>
              div.style(stackMd)(
                if canManage then
                  div.cssClass("mp-card")(
                    h2(state.i18n.t("adm-cat-add-material")).style(sectionTitle.margin(0.px)),
                    div.cssClass("mp-field-row")(
                      textField("Material ID", "e.g. mat-kraft-280", matIdRef),
                      textField("Name (EN)", "Kraft Recycled 280g", matNameEnRef),
                      textField("Name (CS)", "Recyklovaný kraft 280g", matNameCsRef),
                      textField("Grammage (g/m²)", "300", matGsmRef),
                    ),
                    div.cssClass("mp-field-row")(
                      enumSelect("Family", MaterialFamily.values.toList, matFamilyRef),
                      enumSelect("Surface Coating", SurfaceCoating.values.toList, matCoatRef),
                    ),
                    div.style(rowSm)(
                      button
                        .cssClass("mp-btn mp-btn-primary mp-btn-sm")
                        .onClick {
                          for
                            rawId <- matIdRef.get.map(_.trim)
                            en    <- matNameEnRef.get.map(_.trim)
                            cs    <- matNameCsRef.get.map(_.trim)
                            fam   <- matFamilyRef.get
                            coat  <- matCoatRef.get
                            gsmS  <- matGsmRef.get.map(_.trim)
                            _ <-
                              if rawId.isEmpty || en.isEmpty then
                                state.errorBanner.set(Some("Material ID and English Name are required."))
                              else
                                val gsm   = gsmS.toIntOption.filter(_ > 0).getOrElse(300)
                                val mid   = MaterialId.unsafe(rawId)
                                val name  = LocalizedString(en, if cs.nonEmpty then cs else en)
                                val stock = MediaFormat.SheetStock(
                                  formatId = StockFormatId.unsafe("sra3"),
                                  sheetSize = Dimension2D(PositiveMm.unsafe(320), PositiveMm.unsafe(450)),
                                  defaultBleed = NonNegativeMm.unsafe(3),
                                  defaultGutter = NonNegativeMm.unsafe(4),
                                )
                                val mat = cat.materials.get(mid) match
                                  case Some(ex) =>
                                    ex.copy(name = name, family = fam, surfaceCoating = coat, weight = MaterialWeight.Grammage(PaperWeight.unsafe(gsm)))
                                  case None =>
                                    Material(
                                      id = mid,
                                      name = name,
                                      family = fam,
                                      surfaceCoating = coat,
                                      weight = MaterialWeight.Grammage(PaperWeight.unsafe(gsm)),
                                      stockFormats = NonEmptyChunk.single(stock),
                                      properties = Set(MaterialProperty.SmoothSurface),
                                    )
                                state.upsertMaterial(mat).andThen(matIdRef.set("")).andThen(matNameEnRef.set("")).andThen(matNameCsRef.set(""))
                          yield ()
                        }(state.i18n.t("adm-cat-save")),
                    ),
                  )
                else UI.empty,

                div.cssClass("mp-card")(
                  div.style(stackSm)(
                    cat.materials.values.toList.sortBy(_.id.value).map { m =>
                      div.style(rowBetween.flexWrap(FlexWrap.wrap).padding(12.px).rounded(10.px).bg(surfaceAlt).border(1.px, Palette.border))(
                        div.style(stackXs)(
                          div.style(rowSm.flexWrap(FlexWrap.wrap))(
                            span(m.name(lang)).style(Style.fontWeight(FontWeight.w700).color(ink)),
                            badge(m.id.value, BadgeTone.Neutral),
                            badge(m.family.toString, BadgeTone.Brand),
                            badge(formatMaterialWeight(m.weight), BadgeTone.Emerald),
                          ),
                          span(s"${m.surfaceCoating} · ${m.properties.mkString(", ")}").style(smallText),
                        ),
                        if canManage then
                          div.style(rowXs)(
                            button
                              .cssClass("mp-btn mp-btn-secondary mp-btn-sm")
                              .onClick(
                                matIdRef.set(m.id.value)
                                  .andThen(matNameEnRef.set(m.name(Language.En)))
                                  .andThen(matNameCsRef.set(m.name(Language.Cs)))
                                  .andThen(matFamilyRef.set(m.family))
                                  .andThen(matCoatRef.set(m.surfaceCoating))
                                  .andThen(matGsmRef.set(m.weight.grammageGsm.getOrElse(300).toString))
                              )(state.i18n.t("adm-cat-edit")),
                            button
                              .cssClass("mp-btn mp-btn-danger mp-btn-sm")
                              .onClick(state.deleteMaterial(m.id))(state.i18n.t("adm-cat-delete")),
                          )
                        else UI.empty,
                      )
                    }*
                  ),
                ),
              )

            case (Some(cat), Tab.Finishes) =>
              div.style(stackMd)(
                if canManage then
                  div.cssClass("mp-card")(
                    h2(state.i18n.t("adm-cat-add-finish")).style(sectionTitle.margin(0.px)),
                    div.cssClass("mp-field-row")(
                      textField("Finish ID", "e.g. fin-velvet-lam", finIdRef),
                      textField("Name (EN)", "Velvet Touch Lamination", finNameEnRef),
                      textField("Name (CS)", "Sametová laminace", finNameCsRef),
                      enumSelect("Finish Type", FinishType.values.toList, finTypeRef),
                    ),
                    div.style(rowSm)(
                      button
                        .cssClass("mp-btn mp-btn-primary mp-btn-sm")
                        .onClick {
                          for
                            rawId <- finIdRef.get.map(_.trim)
                            en    <- finNameEnRef.get.map(_.trim)
                            cs    <- finNameCsRef.get.map(_.trim)
                            ft    <- finTypeRef.get
                            _ <-
                              if rawId.isEmpty || en.isEmpty then
                                state.errorBanner.set(Some("Finish ID and English Name are required."))
                              else
                                val fid  = FinishId.unsafe(rawId)
                                val name = LocalizedString(en, if cs.nonEmpty then cs else en)
                                val fin = cat.finishes.get(fid) match
                                  case Some(ex) => ex.copy(name = name, finishType = ft)
                                  case None =>
                                    Finish(
                                      id = fid,
                                      name = name,
                                      finishType = ft,
                                      sidePolicy = FinishSidePolicy.Selectable(Set(FinishSide.Front, FinishSide.Both), FinishSide.Both),
                                    )
                                state.upsertFinish(fin).andThen(finIdRef.set("")).andThen(finNameEnRef.set("")).andThen(finNameCsRef.set(""))
                          yield ()
                        }(state.i18n.t("adm-cat-save")),
                    ),
                  )
                else UI.empty,

                div.cssClass("mp-card")(
                  div.style(stackSm)(
                    cat.finishes.values.toList.sortBy(_.id.value).map { f =>
                      div.style(rowBetween.flexWrap(FlexWrap.wrap).padding(12.px).rounded(10.px).bg(surfaceAlt).border(1.px, Palette.border))(
                        div.style(rowSm.flexWrap(FlexWrap.wrap))(
                          span(f.name(lang)).style(Style.fontWeight(FontWeight.w700).color(ink)),
                          badge(f.id.value, BadgeTone.Neutral),
                          badge(f.finishType.toString, BadgeTone.Brand),
                          badge(finishCategoryLabel(f.finishType.category, lang), BadgeTone.Neutral),
                        ),
                        if canManage then
                          div.style(rowXs)(
                            button
                              .cssClass("mp-btn mp-btn-secondary mp-btn-sm")
                              .onClick(
                                finIdRef.set(f.id.value)
                                  .andThen(finNameEnRef.set(f.name(Language.En)))
                                  .andThen(finNameCsRef.set(f.name(Language.Cs)))
                                  .andThen(finTypeRef.set(f.finishType))
                              )(state.i18n.t("adm-cat-edit")),
                            button
                              .cssClass("mp-btn mp-btn-danger mp-btn-sm")
                              .onClick(state.deleteFinish(f.id))(state.i18n.t("adm-cat-delete")),
                          )
                        else UI.empty,
                      )
                    }*
                  ),
                ),
              )

            case (Some(cat), Tab.Methods) =>
              div.style(stackMd)(
                if canManage then
                  div.cssClass("mp-card")(
                    h2(state.i18n.t("adm-cat-add-method")).style(sectionTitle.margin(0.px)),
                    div.cssClass("mp-field-row")(
                      textField("Method ID", "e.g. pm-indigo-digital", pmIdRef),
                      textField("Name (EN)", "HP Indigo Digital", pmNameEnRef),
                      textField("Name (CS)", "HP Indigo digitální tisk", pmNameCsRef),
                      enumSelect("Process Type", PrintingProcessType.values.toList, pmProcRef),
                    ),
                    div.style(rowSm)(
                      button
                        .cssClass("mp-btn mp-btn-primary mp-btn-sm")
                        .onClick {
                          for
                            rawId  <- pmIdRef.get.map(_.trim)
                            en     <- pmNameEnRef.get.map(_.trim)
                            cs     <- pmNameCsRef.get.map(_.trim)
                            proc   <- pmProcRef.get
                            duplex <- pmDuplexRef.get
                            _ <-
                              if rawId.isEmpty || en.isEmpty then
                                state.errorBanner.set(Some("Method ID and English Name are required."))
                              else
                                val id   = PrintingMethodId.unsafe(rawId)
                                val name = LocalizedString(en, if cs.nonEmpty then cs else en)
                                val pm = cat.printingMethods.get(id) match
                                  case Some(ex) => ex.copy(name = name, processType = proc, supportsDoubleSided = duplex)
                                  case None =>
                                    PrintingMethod(id = id, name = name, processType = proc, maxColorsPerSide = Some(4), supportsDoubleSided = duplex)
                                state.upsertPrintingMethod(pm).andThen(pmIdRef.set("")).andThen(pmNameEnRef.set("")).andThen(pmNameCsRef.set(""))
                          yield ()
                        }(state.i18n.t("adm-cat-save")),
                    ),
                  )
                else UI.empty,

                div.cssClass("mp-card")(
                  div.style(stackSm)(
                    cat.printingMethods.values.toList.sortBy(_.id.value).map { pm =>
                      div.style(rowBetween.flexWrap(FlexWrap.wrap).padding(12.px).rounded(10.px).bg(surfaceAlt).border(1.px, Palette.border))(
                        div.style(rowSm.flexWrap(FlexWrap.wrap))(
                          span(pm.name(lang)).style(Style.fontWeight(FontWeight.w700).color(ink)),
                          badge(pm.id.value, BadgeTone.Neutral),
                          badge(pm.processType.toString, BadgeTone.Brand),
                          if pm.supportsDoubleSided then badge("Duplex", BadgeTone.Emerald) else badge("Simplex", BadgeTone.Amber),
                        ),
                        if canManage then
                          div.style(rowXs)(
                            button
                              .cssClass("mp-btn mp-btn-secondary mp-btn-sm")
                              .onClick(
                                pmIdRef.set(pm.id.value)
                                  .andThen(pmNameEnRef.set(pm.name(Language.En)))
                                  .andThen(pmNameCsRef.set(pm.name(Language.Cs)))
                                  .andThen(pmProcRef.set(pm.processType))
                              )(state.i18n.t("adm-cat-edit")),
                            button
                              .cssClass("mp-btn mp-btn-danger mp-btn-sm")
                              .onClick(state.deletePrintingMethod(pm.id))(state.i18n.t("adm-cat-delete")),
                          )
                        else UI.empty,
                      )
                    }*
                  ),
                ),
              )

            case (Some(_), Tab.Rules) =>
              val rules = rulesOpt.map(_.rules).getOrElse(Nil)
              div.style(stackMd)(
                if canManage then
                  div.cssClass("mp-card")(
                    h2(state.i18n.t("adm-cat-add-rule")).style(sectionTitle.margin(0.px)),
                    div.cssClass("mp-field-row")(
                      textField("Rule ID", "e.g. rule-advisory-bleed", ruleIdRef),
                      enumSelect("Severity", RuleSeverity.values.toList, ruleSevRef),
                      textField("Reason (EN)", "Ensure 3mm bleed on heavy stocks", ruleEnRef),
                      textField("Reason (CS)", "Zajistěte spadávku 3 mm", ruleCsRef),
                    ),
                    div.style(rowSm)(
                      button
                        .cssClass("mp-btn mp-btn-primary mp-btn-sm")
                        .onClick {
                          for
                            rawId <- ruleIdRef.get.map(_.trim)
                            sev   <- ruleSevRef.get
                            en    <- ruleEnRef.get.map(_.trim)
                            cs    <- ruleCsRef.get.map(_.trim)
                            _ <-
                              if rawId.isEmpty || en.isEmpty then
                                state.errorBanner.set(Some("Rule ID and English Reason are required."))
                              else
                                val rule = CompatibilityRule(
                                  id = RuleId.unsafe(rawId),
                                  scope = RuleScope.Global,
                                  severity = sev,
                                  when = ConfigPredicate.Always,
                                  mustHold = ConfigPredicate.Always,
                                  reason = LocalizedString(en, if cs.nonEmpty then cs else en),
                                )
                                state.upsertRule(rule).andThen(ruleIdRef.set("")).andThen(ruleEnRef.set("")).andThen(ruleCsRef.set(""))
                          yield ()
                        }(state.i18n.t("adm-cat-save")),
                    ),
                  )
                else UI.empty,

                div.cssClass("mp-card")(
                  div.style(stackSm)(
                    rules.map { r =>
                      val sevTone = if r.severity == RuleSeverity.Error then BadgeTone.Rose else BadgeTone.Amber
                      div.style(rowBetween.flexWrap(FlexWrap.wrap).padding(12.px).rounded(10.px).bg(surfaceAlt).border(1.px, Palette.border))(
                        div.style(stackXs)(
                          div.style(rowSm.flexWrap(FlexWrap.wrap))(
                            badge(r.severity.toString, sevTone),
                            span(r.id.value).style(Style.fontWeight(FontWeight.w700).color(ink)),
                            badge(r.scope.toString, BadgeTone.Neutral),
                          ),
                          span(r.reason(lang)).style(Style.fontSize(13.px).color(inkSecondary)),
                        ),
                        if canManage then
                          button
                            .cssClass("mp-btn mp-btn-danger mp-btn-sm")
                            .onClick(state.deleteRule(r.id))(state.i18n.t("adm-cat-delete"))
                        else UI.empty,
                      )
                    }*
                  ),
                ),
              )

            case (Some(cat), Tab.Showcases) =>
              div.style(stackMd)(
                if canManage then
                  div.cssClass("mp-card")(
                    h2(state.i18n.t("adm-cat-add-showcase")).style(sectionTitle.margin(0.px)),
                    div.cssClass("mp-field-row")(
                      textField("Category ID", "e.g. business-cards", scCatIdRef),
                      enumSelect("Catalog Group", CatalogGroup.values.toList, scGroupRef),
                      textField("Tagline (EN)", "Crisp full-color print", scTagEnRef),
                      textField("Tagline (CS)", "Ostrý plnobarevný tisk", scTagCsRef),
                      textField("Sort Order", "10", scSortRef),
                    ),
                    div.style(rowSm)(
                      button
                        .cssClass("mp-btn mp-btn-primary mp-btn-sm")
                        .onClick {
                          for
                            rawId <- scCatIdRef.get.map(_.trim)
                            grp   <- scGroupRef.get
                            en    <- scTagEnRef.get.map(_.trim)
                            cs    <- scTagCsRef.get.map(_.trim)
                            sortS <- scSortRef.get.map(_.trim)
                            _ <-
                              if rawId.isEmpty || en.isEmpty then
                                state.errorBanner.set(Some("Category ID and English Tagline are required."))
                              else
                                val cid      = CategoryId.unsafe(rawId)
                                val tag      = LocalizedString(en, if cs.nonEmpty then cs else en)
                                val sortOrd  = sortS.toIntOption.getOrElse(10)
                                val existing = cat.showcases.get(cid)
                                val listing = ShowcaseListing(
                                  categoryId = cid,
                                  group = grp,
                                  tagline = tag,
                                  detailedDescription = existing.map(_.detailedDescription).getOrElse(tag),
                                  sortOrder = sortOrd,
                                )
                                state.upsertShowcaseListing(listing)
                          yield ()
                        }(state.i18n.t("adm-cat-save")),
                    ),
                  )
                else UI.empty,

                div.cssClass("mp-card")(
                  div.style(stackSm)(
                    cat.showcases.values.toList.sortBy(_.sortOrder).map { sc =>
                      val catName = cat.categories.get(sc.categoryId).map(_.name(lang)).getOrElse(sc.categoryId.value)
                      div.style(rowBetween.flexWrap(FlexWrap.wrap).padding(12.px).rounded(10.px).bg(surfaceAlt).border(1.px, Palette.border))(
                        div.style(stackXs)(
                          div.style(rowSm.flexWrap(FlexWrap.wrap))(
                            badge(s"#${sc.sortOrder}", BadgeTone.Neutral),
                            span(catName).style(Style.fontWeight(FontWeight.w700).color(ink)),
                            badge(catalogGroupLabel(sc.group, lang), BadgeTone.Brand),
                          ),
                          span(sc.tagline(lang)).style(smallText),
                        ),
                        if canManage then
                          div.style(rowXs)(
                            button
                              .cssClass("mp-btn mp-btn-secondary mp-btn-sm")
                              .onClick(
                                scCatIdRef.set(sc.categoryId.value)
                                  .andThen(scGroupRef.set(sc.group))
                                  .andThen(scTagEnRef.set(sc.tagline(Language.En)))
                                  .andThen(scTagCsRef.set(sc.tagline(Language.Cs)))
                                  .andThen(scSortRef.set(sc.sortOrder.toString))
                              )(state.i18n.t("adm-cat-edit")),
                            button
                              .cssClass("mp-btn mp-btn-danger mp-btn-sm")
                              .onClick(state.deleteShowcaseListing(sc.categoryId))(state.i18n.t("adm-cat-delete")),
                          )
                        else UI.empty,
                      )
                    }*
                  ),
                ),
              )
        )
      }

  private def tabBtn(
      tab: Tab,
      active: Tab,
      ref: SignalRef[Tab],
      labelNode: UI,
      countOpt: Option[Int],
  ): UI =
    button
      .cssClass(if tab == active then "mp-pill-active" else "mp-pill")
      .onClick(ref.set(tab))(
        span(labelNode),
        countOpt.map(c => span(s"($c)").style(Style.fontSize(11.px).opacity(0.85))).getOrElse(UI.empty),
      )

  private def textField(lbl: String, ph: String, ref: SignalRef[String]): UI =
    div.cssClass("mp-field")(
      label(lbl).style(labelStyle),
      input.cssClass("mp-input").placeholder(ph).value(ref).onInput(v => ref.set(v)),
    )

  private def enumSelect[A](lbl: String, values: List[A], ref: SignalRef[A]): UI =
    div.cssClass("mp-field")(
      label(lbl).style(labelStyle),
      ref.render { cur =>
        select
          .cssClass("mp-input")
          .value(cur.toString)
          .onChange(v => values.find(_.toString == v).map(ref.set).getOrElse(Kyo.unit))(
            values.map(v => option.value(v.toString)(v.toString))*
          )
      },
    )
