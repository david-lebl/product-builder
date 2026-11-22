package mpbuilder.admin.views

import kyo.*
import kyo.UI.*
import kyo.Style.*
import mpbuilder.admin.state.BackofficeState
import mpbuilder.catalog.*
import mpbuilder.shared.*
import mpbuilder.ui.components.CommonComponents.*
import mpbuilder.ui.components.{DataTable, TabBar}
import mpbuilder.ui.theme.AppTheme.*
import mpbuilder.ui.theme.AppTheme.Palette.*

/** Tabbed Catalog & Capabilities Inspector:
  *   - Underline `TabBar` across all 6 catalog facets
  *   - Universal `DataTable` on every tab with search, quick filters, sorting, pagination,
  *     and full expandable row detail (read-only browse/inspect; catalog authoring planned for a later phase)
  */
object CatalogManagerView:

  private enum Tab:
    case Categories, Materials, Finishes, Methods, Rules, Showcases

  def render(state: BackofficeState): UI < Sync =
    for
      tabRef      <- Signal.initRef[Tab](Tab.Categories)
      catTable    <- DataTable.makeState[CategoryId](defaultSort = Some(DataTable.SortSpec("name", ascending = true)), pageSize = 10)
      matTable    <- DataTable.makeState[MaterialId](defaultSort = Some(DataTable.SortSpec("name", ascending = true)), pageSize = 10)
      finTable    <- DataTable.makeState[FinishId](defaultSort = Some(DataTable.SortSpec("name", ascending = true)), pageSize = 10)
      pmTable     <- DataTable.makeState[PrintingMethodId](defaultSort = Some(DataTable.SortSpec("name", ascending = true)), pageSize = 10)
      ruleTable   <- DataTable.makeState[RuleId](defaultSort = Some(DataTable.SortSpec("id", ascending = true)), pageSize = 10)
      scTable     <- DataTable.makeState[CategoryId](defaultSort = Some(DataTable.SortSpec("sortOrder", ascending = true)), pageSize = 25)
    yield
      val combined =
        tabRef
          .combineLatest(state.catalog)
          .combineLatest(state.ruleset)
          .combineLatest(state.i18n.language)

      combined.render { case (((activeTab, catOpt), rulesOpt), lang) =>
        val isCs = lang == Language.Cs

        val tabs = List(
          TabBar.TabItem(Tab.Categories, if isCs then "Kategorie" else "Categories", catOpt.map(_.categories.size)),
          TabBar.TabItem(Tab.Materials, if isCs then "Materiály" else "Materials", catOpt.map(_.materials.size)),
          TabBar.TabItem(Tab.Finishes, if isCs then "Zušlechtění" else "Finishes", catOpt.map(_.finishes.size)),
          TabBar.TabItem(Tab.Methods, if isCs then "Tiskové metody" else "Print Methods", catOpt.map(_.printingMethods.size)),
          TabBar.TabItem(Tab.Rules, if isCs then "Pravidla" else "Rules", rulesOpt.map(_.rules.size)),
          TabBar.TabItem(Tab.Showcases, if isCs then "Výloha e-shopu" else "Showcase", catOpt.map(_.showcases.size)),
        )

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

          div.style(Style.padding(10.px, 14.px).rounded(10.px).bg(surfaceAlt).border(1.px, Palette.border).fontSize(12.px).color(inkSecondary))(
            state.i18n.t("adm-cat-inspect-note"),
          ),

          // Underline TabBar
          TabBar.render[Tab](
            items = tabs,
            active = activeTab,
            onSelect = t => tabRef.set(t),
          ),

          // Tab Content
          (catOpt, activeTab) match
            case (None, _) =>
              div.cssClass("mp-card")(span(if isCs then "Načítání katalogu..." else "Loading catalog...").style(mutedText))

            case (Some(cat), Tab.Categories) =>
              renderCategoriesTable(cat, catTable, lang)

            case (Some(cat), Tab.Materials) =>
              renderMaterialsTable(cat, matTable, lang)

            case (Some(cat), Tab.Finishes) =>
              renderFinishesTable(cat, finTable, lang)

            case (Some(cat), Tab.Methods) =>
              renderMethodsTable(cat, pmTable, lang)

            case (Some(_), Tab.Rules) =>
              renderRulesTable(rulesOpt.map(_.rules).getOrElse(Nil), ruleTable, lang)

            case (Some(cat), Tab.Showcases) =>
              renderShowcasesTable(cat, scTable, lang)
        )
      }

  private def renderCategoriesTable(
      cat: ProductCatalog,
      tableState: SignalRef[DataTable.State[CategoryId]],
      lang: Language,
  ): UI =
    val isCs = lang == Language.Cs
    val rows = cat.categories.values.toList

    val columns = List(
      DataTable.Column[ProductCategory](
        id = "name",
        header = if isCs then "Kategorie" else "Category",
        sortKey = Some(_.name(lang)),
        cell = pc =>
          div.style(Style.column.gap(2.px))(
            span(pc.name(lang)).style(Style.fontWeight(FontWeight.w700).color(ink)),
            span(pc.id.value).style(smallText),
          ),
      ),
      DataTable.Column[ProductCategory](
        id = "components",
        header = if isCs then "Komponenty" else "Components",
        numericSortKey = Some(pc => BigDecimal(pc.components.size)),
        alignRight = true,
        cell = pc => badge(s"${pc.components.size}", BadgeTone.Neutral),
      ),
      DataTable.Column[ProductCategory](
        id = "presets",
        header = if isCs then "Šablony" else "Presets",
        numericSortKey = Some(pc => BigDecimal(pc.presets.size)),
        alignRight = true,
        cell = pc => badge(s"${pc.presets.size}", BadgeTone.Brand),
      ),
      DataTable.Column[ProductCategory](
        id = "methods",
        header = if isCs then "Tiskové metody" else "Print Methods",
        numericSortKey = Some(pc => BigDecimal(pc.allowedPrintingMethodIds.size)),
        alignRight = true,
        cell = pc => span(pc.allowedPrintingMethodIds.size.toString).style(smallText),
      ),
    )

    val filters = List(
      DataTable.FilterGroup[ProductCategory](
        id = "presets",
        label = if isCs then "Šablony" else "Presets",
        options = List(
          DataTable.FilterOption("hasPresets", if isCs then "Se šablonami" else "With Presets", _.presets.nonEmpty),
          DataTable.FilterOption("multiComp", if isCs then "Vícekomponentní" else "Multi-Component", _.components.size > 1),
        ),
      )
    )

    DataTable.render[ProductCategory, CategoryId](
      rows = rows,
      rowKey = _.id,
      columns = columns,
      state = tableState,
      searchPlaceholder = if isCs then "Hledat kategorii nebo ID..." else "Search category name or ID...",
      matchesSearch = (pc, q) =>
        pc.id.value.toLowerCase.contains(q) ||
          pc.name(Language.En).toLowerCase.contains(q) ||
          pc.name(Language.Cs).toLowerCase.contains(q),
      filterGroups = filters,
      renderDetail = Some { pc =>
        div.style(stackSm)(
          div.style(rowWrap)(
            badge(s"ID: ${pc.id.value}", BadgeTone.Neutral),
            span(s"EN: ${pc.name(Language.En)} · CS: ${pc.name(Language.Cs)}").style(smallText),
          ),
          div.style(stackXs)(
            span(if isCs then "Komponenty" else "Component Templates").style(labelStyle),
            UI.fragment(
              pc.components.toList.map { comp =>
                div.style(Style.column.gap(4.px).padding(10.px).rounded(8.px).bg(surface).border(1.px, Palette.border))(
                  div.style(rowWrap)(
                    span(s"${comp.key.value} (${comp.label(lang)})").style(Style.fontWeight(FontWeight.w700).color(ink)),
                    if comp.required then badge(if isCs then "Povinná" else "Required", BadgeTone.Brand) else badge(if isCs then "Volitelná" else "Optional", BadgeTone.Neutral),
                    if comp.printable then badge(if isCs then "Tisknutelná" else "Printable", BadgeTone.Emerald) else UI.empty,
                  ),
                  span(s"${if isCs then "Povolené materiály" else "Allowed Materials"} (${comp.allowedMaterialIds.size}): ${comp.allowedMaterialIds.map(_.value).toList.sorted.mkString(", ")}").style(smallText),
                  span(s"${if isCs then "Povolená zušlechtění" else "Allowed Finishes"} (${comp.allowedFinishIds.size}): ${comp.allowedFinishIds.map(_.value).toList.sorted.mkString(", ")}").style(smallText),
                )
              }*
            ),
          ),
          if pc.presets.nonEmpty then
            div.style(stackXs)(
              span(if isCs then "Přednastavené šablony (Presets)" else "Product Presets").style(labelStyle),
              div.style(rowWrap)(
                pc.presets.map { pr =>
                  badge(s"${pr.name(lang)} (${pr.id.value})", BadgeTone.Brand)
                }*
              ),
            )
          else UI.empty,
          div.style(rowWrap)(
            span(if isCs then "Povolené tiskové metody:" else "Allowed Print Methods:").style(smallText.fontWeight(FontWeight.w700)),
            UI.fragment(pc.allowedPrintingMethodIds.toList.sortBy(_.value).map(id => badge(id.value, BadgeTone.Neutral))*),
          ),
        )
      },
      emptyMessage = if isCs then "Žádné kategorie neodpovídají filtru." else "No categories match the current filter.",
      lang = lang,
    )

  private def renderMaterialsTable(
      cat: ProductCatalog,
      tableState: SignalRef[DataTable.State[MaterialId]],
      lang: Language,
  ): UI =
    val isCs = lang == Language.Cs
    val rows = cat.materials.values.toList

    val columns = List(
      DataTable.Column[Material](
        id = "name",
        header = if isCs then "Materiál" else "Material",
        sortKey = Some(_.name(lang)),
        cell = m =>
          div.style(Style.column.gap(2.px))(
            span(m.name(lang)).style(Style.fontWeight(FontWeight.w700).color(ink)),
            span(m.id.value).style(smallText),
          ),
      ),
      DataTable.Column[Material](
        id = "family",
        header = if isCs then "Skupina" else "Family",
        sortKey = Some(_.family.toString),
        cell = m => badge(m.family.toString, BadgeTone.Brand),
      ),
      DataTable.Column[Material](
        id = "weight",
        header = if isCs then "Gramáž / Tloušťka" else "Weight / Thickness",
        numericSortKey = Some(m => BigDecimal(m.weight.grammageGsm.getOrElse(0))),
        cell = m => badge(formatMaterialWeight(m.weight), BadgeTone.Emerald),
      ),
      DataTable.Column[Material](
        id = "coating",
        header = if isCs then "Povrch" else "Surface Coating",
        sortKey = Some(_.surfaceCoating.toString),
        cell = m => span(m.surfaceCoating.toString).style(smallText),
      ),
      DataTable.Column[Material](
        id = "formats",
        header = if isCs then "Formáty" else "Stock Formats",
        numericSortKey = Some(m => BigDecimal(m.stockFormats.size)),
        alignRight = true,
        cell = m => span(m.stockFormats.size.toString).style(smallText),
      ),
    )

    val filters = List(
      DataTable.FilterGroup[Material](
        id = "family",
        label = if isCs then "Skupina" else "Family",
        options = MaterialFamily.values.toList.map(f => DataTable.FilterOption(f.toString, f.toString, _.family == f)),
      )
    )

    DataTable.render[Material, MaterialId](
      rows = rows,
      rowKey = _.id,
      columns = columns,
      state = tableState,
      searchPlaceholder = if isCs then "Hledat materiál, ID nebo vlastnost..." else "Search material name, ID, or property...",
      matchesSearch = (m, q) =>
        m.id.value.toLowerCase.contains(q) ||
          m.name(Language.En).toLowerCase.contains(q) ||
          m.name(Language.Cs).toLowerCase.contains(q) ||
          m.family.toString.toLowerCase.contains(q) ||
          m.surfaceCoating.toString.toLowerCase.contains(q),
      filterGroups = filters,
      renderDetail = Some { m =>
        div.style(stackSm)(
          span(s"EN: ${m.name(Language.En)} · CS: ${m.name(Language.Cs)}").style(smallText),
          div.style(rowWrap)(
            span(if isCs then "Vlastnosti:" else "Properties:").style(smallText.fontWeight(FontWeight.w700)),
            UI.fragment(m.properties.toList.sortBy(_.toString).map(p => badge(p.toString, BadgeTone.Neutral))*),
          ),
          div.style(stackXs)(
            span(if isCs then "Dostupné formáty archů / rolí" else "Available Stock Formats").style(labelStyle),
            UI.fragment(
              m.stockFormats.toList.map { sf =>
                div.style(Style.padding(6.px, 10.px).rounded(8.px).bg(surface).border(1.px, Palette.border).fontSize(12.px).color(inkSecondary))(
                  sf.toString,
                )
              }*
            ),
          ),
        )
      },
      emptyMessage = if isCs then "Žádné materiály neodpovídají filtru." else "No materials match the current filter.",
      lang = lang,
    )

  private def renderFinishesTable(
      cat: ProductCatalog,
      tableState: SignalRef[DataTable.State[FinishId]],
      lang: Language,
  ): UI =
    val isCs = lang == Language.Cs
    val rows = cat.finishes.values.toList

    val columns = List(
      DataTable.Column[Finish](
        id = "name",
        header = if isCs then "Zušlechtění" else "Finish",
        sortKey = Some(_.name(lang)),
        cell = f =>
          div.style(Style.column.gap(2.px))(
            span(f.name(lang)).style(Style.fontWeight(FontWeight.w700).color(ink)),
            span(f.id.value).style(smallText),
          ),
      ),
      DataTable.Column[Finish](
        id = "type",
        header = if isCs then "Typ" else "Finish Type",
        sortKey = Some(_.finishType.toString),
        cell = f => badge(f.finishType.toString, BadgeTone.Brand),
      ),
      DataTable.Column[Finish](
        id = "category",
        header = if isCs then "Kategorie" else "Category",
        sortKey = Some(_.finishType.category.toString),
        cell = f => badge(finishCategoryLabel(f.finishType.category, lang), BadgeTone.Neutral),
      ),
      DataTable.Column[Finish](
        id = "sidePolicy",
        header = if isCs then "Strany" else "Side Policy",
        sortKey = Some(_.sidePolicy.toString),
        cell = f => span(f.sidePolicy.toString).style(smallText),
      ),
    )

    val filters = List(
      DataTable.FilterGroup[Finish](
        id = "category",
        label = if isCs then "Kategorie" else "Category",
        options = FinishCategory.values.toList.map(fc =>
          DataTable.FilterOption(fc.toString, finishCategoryLabel(fc, lang), _.finishType.category == fc)
        ),
      )
    )

    DataTable.render[Finish, FinishId](
      rows = rows,
      rowKey = _.id,
      columns = columns,
      state = tableState,
      searchPlaceholder = if isCs then "Hledat zušlechtění nebo ID..." else "Search finish name, ID, or type...",
      matchesSearch = (f, q) =>
        f.id.value.toLowerCase.contains(q) ||
          f.name(Language.En).toLowerCase.contains(q) ||
          f.name(Language.Cs).toLowerCase.contains(q) ||
          f.finishType.toString.toLowerCase.contains(q),
      filterGroups = filters,
      renderDetail = Some { f =>
        div.style(stackXs)(
          span(s"EN: ${f.name(Language.En)} · CS: ${f.name(Language.Cs)}").style(smallText),
          span(s"${if isCs then "Typ" else "Type"}: ${f.finishType} (${finishCategoryLabel(f.finishType.category, lang)})").style(Style.fontSize(13.px).color(ink)),
          span(s"${if isCs then "Pravidlo stran" else "Side Policy"}: ${f.sidePolicy}").style(smallText),
        )
      },
      emptyMessage = if isCs then "Žádná zušlechtění neodpovídají filtru." else "No finishes match the current filter.",
      lang = lang,
    )

  private def renderMethodsTable(
      cat: ProductCatalog,
      tableState: SignalRef[DataTable.State[PrintingMethodId]],
      lang: Language,
  ): UI =
    val isCs = lang == Language.Cs
    val rows = cat.printingMethods.values.toList

    val columns = List(
      DataTable.Column[PrintingMethod](
        id = "name",
        header = if isCs then "Tisková metoda" else "Print Method",
        sortKey = Some(_.name(lang)),
        cell = pm =>
          div.style(Style.column.gap(2.px))(
            span(pm.name(lang)).style(Style.fontWeight(FontWeight.w700).color(ink)),
            span(pm.id.value).style(smallText),
          ),
      ),
      DataTable.Column[PrintingMethod](
        id = "process",
        header = if isCs then "Technologie" else "Process Type",
        sortKey = Some(_.processType.toString),
        cell = pm => badge(pm.processType.toString, BadgeTone.Brand),
      ),
      DataTable.Column[PrintingMethod](
        id = "duplex",
        header = if isCs then "Oboustranný tisk" else "Duplex",
        sortKey = Some(pm => if pm.supportsDoubleSided then "Duplex" else "Simplex"),
        cell = pm =>
          if pm.supportsDoubleSided then badge("Duplex", BadgeTone.Emerald)
          else badge("Simplex", BadgeTone.Amber),
      ),
      DataTable.Column[PrintingMethod](
        id = "colors",
        header = if isCs then "Max barev / stranu" else "Max Colors / Side",
        numericSortKey = Some(pm => BigDecimal(pm.maxColorsPerSide.getOrElse(0))),
        alignRight = true,
        cell = pm => span(pm.maxColorsPerSide.map(_.toString).getOrElse("∞")).style(smallText),
      ),
    )

    val filters = List(
      DataTable.FilterGroup[PrintingMethod](
        id = "process",
        label = if isCs then "Technologie" else "Process",
        options = PrintingProcessType.values.toList.map(pt =>
          DataTable.FilterOption(pt.toString, pt.toString, _.processType == pt)
        ),
      ),
      DataTable.FilterGroup[PrintingMethod](
        id = "duplex",
        label = if isCs then "Strany" else "Siding",
        options = List(
          DataTable.FilterOption("duplex", "Duplex", _.supportsDoubleSided),
          DataTable.FilterOption("simplex", "Simplex", !_.supportsDoubleSided),
        ),
      ),
    )

    DataTable.render[PrintingMethod, PrintingMethodId](
      rows = rows,
      rowKey = _.id,
      columns = columns,
      state = tableState,
      searchPlaceholder = if isCs then "Hledat tiskovou metodu nebo technologii..." else "Search print method or process...",
      matchesSearch = (pm, q) =>
        pm.id.value.toLowerCase.contains(q) ||
          pm.name(Language.En).toLowerCase.contains(q) ||
          pm.name(Language.Cs).toLowerCase.contains(q) ||
          pm.processType.toString.toLowerCase.contains(q),
      filterGroups = filters,
      renderDetail = Some { pm =>
        div.style(stackXs)(
          span(s"EN: ${pm.name(Language.En)} · CS: ${pm.name(Language.Cs)}").style(smallText),
          div.style(rowWrap)(
            badge(pm.processType.toString, BadgeTone.Brand),
            if pm.supportsDoubleSided then badge("Duplex Supported", BadgeTone.Emerald) else badge("Single-Sided Only", BadgeTone.Amber),
            pm.maxColorsPerSide.map(c => badge(s"Max $c colors/side", BadgeTone.Neutral)).getOrElse(UI.empty),
          ),
        )
      },
      emptyMessage = if isCs then "Žádné tiskové metody neodpovídají filtru." else "No print methods match the current filter.",
      lang = lang,
    )

  private def renderRulesTable(
      rules: List[CompatibilityRule],
      tableState: SignalRef[DataTable.State[RuleId]],
      lang: Language,
  ): UI =
    val isCs = lang == Language.Cs

    val columns = List(
      DataTable.Column[CompatibilityRule](
        id = "id",
        header = if isCs then "ID pravidla" else "Rule ID",
        sortKey = Some(_.id.value),
        cell = r => span(r.id.value).style(Style.fontWeight(FontWeight.w700).color(ink)),
      ),
      DataTable.Column[CompatibilityRule](
        id = "severity",
        header = if isCs then "Závažnost" else "Severity",
        sortKey = Some(_.severity.toString),
        cell = r => badge(r.severity.toString, if r.severity == RuleSeverity.Error then BadgeTone.Rose else BadgeTone.Amber),
      ),
      DataTable.Column[CompatibilityRule](
        id = "scope",
        header = if isCs then "Rozsah" else "Scope",
        sortKey = Some(_.scope.toString),
        cell = r => badge(r.scope.toString, BadgeTone.Neutral),
      ),
      DataTable.Column[CompatibilityRule](
        id = "reason",
        header = if isCs then "Důvod / Zpráva" else "Reason",
        sortKey = Some(_.reason(lang)),
        cell = r => span(r.reason(lang)).style(Style.fontSize(12.px).color(inkSecondary)),
      ),
    )

    val filters = List(
      DataTable.FilterGroup[CompatibilityRule](
        id = "severity",
        label = if isCs then "Závažnost" else "Severity",
        options = RuleSeverity.values.toList.map(s => DataTable.FilterOption(s.toString, s.toString, _.severity == s)),
      )
    )

    DataTable.render[CompatibilityRule, RuleId](
      rows = rules,
      rowKey = _.id,
      columns = columns,
      state = tableState,
      searchPlaceholder = if isCs then "Hledat pravidlo nebo důvod..." else "Search rule ID, scope, or reason...",
      matchesSearch = (r, q) =>
        r.id.value.toLowerCase.contains(q) ||
          r.reason(Language.En).toLowerCase.contains(q) ||
          r.reason(Language.Cs).toLowerCase.contains(q) ||
          r.scope.toString.toLowerCase.contains(q),
      filterGroups = filters,
      renderDetail = Some { r =>
        div.style(stackXs)(
          span(s"EN: ${r.reason(Language.En)}").style(Style.fontSize(13.px).color(ink)),
          span(s"CS: ${r.reason(Language.Cs)}").style(smallText),
          div.style(Style.padding(8.px, 10.px).rounded(8.px).bg(surface).border(1.px, Palette.border).fontSize(12.px).color(inkSecondary))(
            s"WHEN: ${r.when}  ⟹  MUST HOLD: ${r.mustHold}",
          ),
        )
      },
      emptyMessage = if isCs then "Žádná pravidla neodpovídají filtru." else "No compatibility rules match the current filter.",
      lang = lang,
    )

  private def renderShowcasesTable(
      cat: ProductCatalog,
      tableState: SignalRef[DataTable.State[CategoryId]],
      lang: Language,
  ): UI =
    val isCs = lang == Language.Cs
    val rows = cat.showcases.values.toList

    def catName(sc: ShowcaseListing): String =
      cat.categories.get(sc.categoryId).map(_.name(lang)).getOrElse(sc.categoryId.value)

    val columns = List(
      DataTable.Column[ShowcaseListing](
        id = "sortOrder",
        header = "#",
        numericSortKey = Some(sc => BigDecimal(sc.sortOrder)),
        cell = sc => badge(s"#${sc.sortOrder}", BadgeTone.Neutral),
      ),
      DataTable.Column[ShowcaseListing](
        id = "category",
        header = if isCs then "Produkt" else "Product",
        sortKey = Some(catName),
        cell = sc =>
          div.style(Style.column.gap(2.px))(
            span(catName(sc)).style(Style.fontWeight(FontWeight.w700).color(ink)),
            span(sc.categoryId.value).style(smallText),
          ),
      ),
      DataTable.Column[ShowcaseListing](
        id = "group",
        header = if isCs then "Sekce" else "Group",
        sortKey = Some(sc => catalogGroupLabel(sc.group, lang)),
        cell = sc => badge(catalogGroupLabel(sc.group, lang), BadgeTone.Brand),
      ),
      DataTable.Column[ShowcaseListing](
        id = "tagline",
        header = if isCs then "Podtitulek" else "Tagline",
        sortKey = Some(_.tagline(lang)),
        cell = sc => span(sc.tagline(lang)).style(Style.fontSize(12.px).color(inkSecondary)),
      ),
    )

    val filters = List(
      DataTable.FilterGroup[ShowcaseListing](
        id = "group",
        label = if isCs then "Sekce výlohy" else "Catalog Group",
        options = CatalogGroup.values.toList.map(g =>
          DataTable.FilterOption(g.toString, catalogGroupLabel(g, lang), _.group == g)
        ),
      )
    )

    DataTable.render[ShowcaseListing, CategoryId](
      rows = rows,
      rowKey = _.categoryId,
      columns = columns,
      state = tableState,
      searchPlaceholder = if isCs then "Hledat produkt, sekci nebo popis..." else "Search product, group, or tagline...",
      matchesSearch = (sc, q) =>
        catName(sc).toLowerCase.contains(q) ||
          sc.categoryId.value.toLowerCase.contains(q) ||
          sc.tagline(Language.En).toLowerCase.contains(q) ||
          sc.tagline(Language.Cs).toLowerCase.contains(q),
      filterGroups = filters,
      renderDetail = Some { sc =>
        div.style(stackXs)(
          span(s"Tagline EN: ${sc.tagline(Language.En)}").style(Style.fontSize(13.px).fontWeight(FontWeight.w600).color(ink)),
          span(s"Tagline CS: ${sc.tagline(Language.Cs)}").style(smallText),
          p(sc.detailedDescription(lang)).style(Style.margin(4.px, 0.px, 0.px, 0.px).fontSize(13.px).color(inkSecondary).lineHeight(1.5)),
        )
      },
      emptyMessage = if isCs then "Žádné karty výlohy neodpovídají filtru." else "No showcase listings match the current filter.",
      lang = lang,
    )
