package mpbuilder.ui.storefront

import kyo.*
import kyo.UI.*
import kyo.Style.*
import mpbuilder.catalog.*
import mpbuilder.shared.*
import mpbuilder.ui.components.CommonComponents.*
import mpbuilder.ui.theme.AppTheme.*
import mpbuilder.ui.theme.AppTheme.Palette.*

/** Responsive product catalog page with hero banner, live search, group filter pills & product cards. */
object CatalogView:

  def render(state: StorefrontState): UI < Async =
    for
      searchRef <- Signal.initRef("")
      groupRef  <- Signal.initRef[Option[CatalogGroup]](None)
    yield
      val combined = searchRef.combineLatest(groupRef).combineLatest(state.i18n.language)

      div.style(Style.column.gap(24.px).width(100.pct))(
        // Hero Banner
        section.cssClass("mp-hero")(
          div.style(Style.row.align(Alignment.center).gap(8.px))(
            span(state.i18n.t("catalog-hero-badge")).style(
              Style.padding(4.px, 12.px)
                .rounded(999.px)
                .bg(Color.rgba(255, 255, 255, 0.16))
                .color(Color.white)
                .fontSize(12.px)
                .fontWeight(FontWeight.w700)
                .letterSpacing(0.4.px),
            ),
          ),
          h1(state.i18n.t("catalog-hero-title")).cssClass("mp-hero-title").style(Style.margin(0.px)),
          p(state.i18n.t("catalog-hero-subtitle")).style(
            Style.fontSize(16.px)
              .lineHeight(1.6)
              .color(Color.rgba(255, 255, 255, 0.9))
              .maxWidth(680.px)
              .margin(0.px),
          ),
          div.style(Style.row.flexWrap(FlexWrap.wrap).gap(12.px).margin(6.px, 0.px, 0.px, 0.px))(
            button
              .cssClass("mp-btn")
              .style(Style.bg(Color.white).color(brandDark).padding(11.px, 20.px).rounded(12.px).fontWeight(FontWeight.w700))
              .onClick(state.openBuilder())(state.i18n.t("catalog-hero-cta")),
          ),
        ),

        // Search & Group Filter Bar
        div.style(Style.column.gap(12.px))(
          div.style(Style.row.flexWrap(FlexWrap.wrap).gap(12.px).align(Alignment.center).justify(Justification.spaceBetween))(
            searchInput
              .cssClass("mp-input")
              .style(Style.maxWidth(420.px).flexGrow(1.0))
              .value(searchRef)
              .placeholder("Search products, formats, materials... / Hledat produkty..."),
            groupRef.combineLatest(state.i18n.language).render { case (activeGroup, lang) =>
              div.style(Style.row.flexWrap(FlexWrap.wrap).gap(8.px))(
                button
                  .cssClass(if activeGroup.isEmpty then "mp-pill-active" else "mp-pill")
                  .onClick(groupRef.set(None))(state.i18n.t("catalog-filter-all")),
                UI.fragment(
                  CatalogGroup.values.toList.map { grp =>
                    val isSel = activeGroup.contains(grp)
                    button
                      .cssClass(if isSel then "mp-pill-active" else "mp-pill")
                      .onClick(groupRef.set(Some(grp)))(catalogGroupLabel(grp, lang))
                  }*
                ),
              )
            },
          ),
        ),

        // Product Card Grid
        combined.render { case ((query, groupOpt), lang) =>
          val q = query.trim.toLowerCase
          val filtered = state.listings.filter { listing =>
            val catOpt = state.catalog.categories.get(listing.categoryId)
            val matchesGroup = groupOpt.forall(_ == listing.group)
            val matchesQuery =
              if q.isEmpty then true
              else
                val nameHit    = catOpt.exists(c => c.name(lang).toLowerCase.contains(q) || c.name(Language.En).toLowerCase.contains(q))
                val taglineHit = listing.tagline(lang).toLowerCase.contains(q) || listing.detailedDescription(lang).toLowerCase.contains(q)
                val groupHit   = catalogGroupLabel(listing.group, lang).toLowerCase.contains(q)
                nameHit || taglineHit || groupHit
            matchesGroup && matchesQuery
          }

          if filtered.isEmpty then
            div.cssClass("mp-card").style(Style.align(Alignment.center).textAlign(TextAlign.center).padding(40.px, 20.px))(
              h3(state.i18n.t("catalog-empty-title")).style(Style.margin(0.px).fontSize(18.px).color(ink)),
              p(state.i18n.t("catalog-empty-desc")).style(mutedText.margin(0.px)),
              button
                .cssClass("mp-btn mp-btn-secondary")
                .onClick(searchRef.set("").andThen(groupRef.set(None)))(state.i18n.t("catalog-clear-filters")),
            )
          else
            div.cssClass("mp-card-grid")(
              filtered.map { listing =>
                val catOpt      = state.catalog.categories.get(listing.categoryId)
                val title       = catOpt.map(_.name(lang)).getOrElse(listing.categoryId.value)
                val presetCount = catOpt.map(_.presets.size).getOrElse(0)
                val compCount   = catOpt.map(_.components.size).getOrElse(1)

                div.cssClass("mp-card-product")(
                  div.style(Style.row.gap(12.px).align(Alignment.start))(
                    groupIcon(listing.group),
                    div.style(Style.column.gap(4.px).flexGrow(1.0).minWidth(0.px))(
                      div.style(Style.row.flexWrap(FlexWrap.wrap).gap(6.px).align(Alignment.center))(
                        badge(catalogGroupLabel(listing.group, lang), BadgeTone.Brand),
                        if presetCount > 0 then
                          reactiveBadge(
                            state.i18n.t("catalog-presets-count", Dict("count" -> presetCount.toString)),
                            BadgeTone.Neutral,
                          )
                        else UI.empty,
                        if compCount > 1 then
                          badge(s"$compCount parts", BadgeTone.Emerald)
                        else UI.empty,
                      ),
                      h3(title).style(Style.margin(2.px, 0.px, 0.px, 0.px).fontSize(17.px).fontWeight(FontWeight.w700).color(ink)),
                    ),
                  ),
                  p(listing.tagline(lang)).style(mutedText.margin(0.px).flexGrow(1.0)),
                  div.style(Style.row.gap(8.px).flexWrap(FlexWrap.wrap).padding(8.px, 0.px, 0.px, 0.px).borderTop(1.px, Palette.border))(
                    button
                      .cssClass("mp-btn mp-btn-secondary mp-btn-sm")
                      .style(Style.flexGrow(1.0))
                      .onClick(state.openProductDetail(listing.categoryId))(state.i18n.t("catalog-btn-details")),
                    button
                      .cssClass("mp-btn mp-btn-primary mp-btn-sm")
                      .style(Style.flexGrow(1.0))
                      .onClick(state.openBuilder(Some(listing.categoryId)))(state.i18n.t("catalog-btn-configure")),
                  ),
                )
              }*
            )
        },
      )
