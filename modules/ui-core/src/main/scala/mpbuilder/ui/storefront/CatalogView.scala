package mpbuilder.ui.storefront

import kyo.*
import kyo.UI.*
import kyo.Style.*
import mpbuilder.catalog.*
import mpbuilder.shared.*
import mpbuilder.ui.components.CommonComponents.*
import mpbuilder.ui.components.{Controls, Icons}
import mpbuilder.ui.components.Icons.Icon
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

      def filteredListings(query: String, groupOpt: Option[CatalogGroup], lang: Language): List[ShowcaseListing] =
        val q = query.trim.toLowerCase
        state.listings.filter(l => groupOpt.forall(_ == l.group) && matches(state, l, q, lang))

      def heroFeature(icon: Icon, key: String): UI =
        span.style(
          Style.row
            .align(Alignment.center)
            .gap(7.px)
            .padding(6.px, 12.px)
            .rounded(999.px)
            .bg(Color.rgba(255, 255, 255, 0.12))
            .border(1.px, Color.rgba(255, 255, 255, 0.22))
            .color(Color.white)
            .fontSize(13.px)
            .fontWeight(FontWeight.w600),
        )(Icons(icon, 15), span(state.i18n.t(key)))

      div.style(Style.column.gap(28.px).width(100.pct))(
        // Hero Banner
        section.cssClass("mp-hero")(
          span(state.i18n.t("catalog-hero-badge")).style(
            Style.row
              .align(Alignment.center)
              .alignSelf(Alignment.start)
              .padding(5.px, 12.px)
              .rounded(999.px)
              .bg(Color.rgba(255, 255, 255, 0.16))
              .border(1.px, Color.rgba(255, 255, 255, 0.24))
              .color(Color.white)
              .fontSize(12.px)
              .fontWeight(FontWeight.w700)
              .letterSpacing(0.4.px),
          ),
          h1(state.i18n.t("catalog-hero-title")).cssClass("mp-hero-title").style(Style.margin(0.px).maxWidth(760.px)),
          p(state.i18n.t("catalog-hero-subtitle")).style(
            Style.fontSize(16.px)
              .lineHeight(1.65)
              .color(Color.rgba(255, 255, 255, 0.88))
              .maxWidth(660.px)
              .margin(0.px),
          ),
          div.style(Style.row.flexWrap(FlexWrap.wrap).gap(10.px).margin(8.px, 0.px, 0.px, 0.px))(
            button
              .cssClass("mp-btn")
              .style(
                Style.bg(Color.white)
                  .color(brandDeep)
                  .padding(12.px, 20.px)
                  .rounded(12.px)
                  .fontWeight(FontWeight.w700)
                  .shadow(0.px, 8.px, 20.px, (-6).px, Color.rgba(15, 23, 42, 0.35))
                  .hover(Style.bg(Color.hex("#EEF2FF").getOrElse(Color.white))),
              )
              .onClick(state.openBuilder())(Icons(Icon.Wand, 17), span(state.i18n.t("catalog-hero-cta")), Icons(Icon.ArrowRight, 16)),
          ),
          div.style(Style.row.flexWrap(FlexWrap.wrap).gap(8.px).margin(6.px, 0.px, 0.px, 0.px))(
            heroFeature(Icon.Zap, "catalog-hero-feat-pricing"),
            heroFeature(Icon.Layers, "catalog-hero-feat-imposition"),
            heroFeature(Icon.Percent, "catalog-hero-feat-b2b"),
          ),
        ),

        // Search & Group Filter Bar
        div.style(Style.column.gap(14.px))(
          div.style(Style.row.flexWrap(FlexWrap.wrap).gap(12.px).align(Alignment.center).justify(Justification.spaceBetween))(
            div.cssClass("mp-search").style(Style.maxWidth(440.px).flexGrow(1.0).flexBasis(300.px))(
              span.cssClass("mp-search-icon")(Icons(Icon.Search, 17)),
              state.i18n.t("catalog-search-placeholder").render { ph =>
                searchInput
                  .cssClass("mp-input mp-search-input")
                  .aria("label", ph)
                  .value(searchRef)
                  .placeholder(ph)
              },
            ),
            combined.render { case ((query, groupOpt), lang) =>
              val n = filteredListings(query, groupOpt, lang).size
              span(state.i18n.t("catalog-results-count", Dict("count" -> n.toString))).style(smallText.fontWeight(FontWeight.w600))
            },
          ),
          groupRef.combineLatest(state.i18n.language).render { case (activeGroup, lang) =>
            def countFor(g: Option[CatalogGroup]): Int = state.listings.count(l => g.forall(_ == l.group))
            def pillCount(n: Int, active: Boolean): UI =
              span(n.toString).style(
                Style.padding(1.px, 7.px)
                  .rounded(999.px)
                  .fontSize(11.px)
                  .fontWeight(FontWeight.w700)
                  .bg(if active then Color.rgba(255, 255, 255, 0.22) else surfaceAlt)
                  .color(if active then Color.white else inkMuted),
              )
            div.style(Style.row.flexWrap(FlexWrap.wrap).gap(8.px)).role("group")(
              button
                .cssClass(if activeGroup.isEmpty then "mp-pill-active" else "mp-pill")
                .aria("pressed", activeGroup.isEmpty.toString)
                .onClick(groupRef.set(None))(
                  Icons(Icon.Grid, 14),
                  span(state.i18n.t("catalog-filter-all")),
                  pillCount(countFor(None), activeGroup.isEmpty),
                ),
              UI.fragment(
                CatalogGroup.values.toList.map { grp =>
                  val isSel = activeGroup.contains(grp)
                  button
                    .cssClass(if isSel then "mp-pill-active" else "mp-pill")
                    .aria("pressed", isSel.toString)
                    .onClick(groupRef.set(Some(grp)))(
                      Icons(groupGlyph(grp), 14),
                      span(catalogGroupLabel(grp, lang)),
                      pillCount(countFor(Some(grp)), isSel),
                    )
                }*
              ),
            )
          },
        ),

        // Product Card Grid
        combined.render { case ((query, groupOpt), lang) =>
          val filtered = filteredListings(query, groupOpt, lang)

          if filtered.isEmpty then
            Controls.emptyState(
              Icon.Search,
              span(state.i18n.t("catalog-empty-title")),
              span(state.i18n.t("catalog-empty-desc")),
              button
                .cssClass("mp-btn mp-btn-secondary")
                .onClick(searchRef.set("").andThen(groupRef.set(None)))(Icons(Icon.Refresh, 15), span(state.i18n.t("catalog-clear-filters"))),
            )
          else
            div.cssClass("mp-card-grid")(
              filtered.map { listing =>
                val catOpt         = state.catalog.categories.get(listing.categoryId)
                val title          = catOpt.map(_.name(lang)).getOrElse(listing.categoryId.value)
                val presetCount    = catOpt.map(_.presets.size).getOrElse(0)
                val compCount      = catOpt.map(_.components.size).getOrElse(1)
                val (accent, soft) = groupAccent(listing.group)

                div.cssClass("mp-card-product")(
                  div
                    .cssClass("mp-product-visual")
                    .style(Style.bgGradient(GradientDirection.toBottomRight, (soft, 0.pct), (surface, 100.pct)).color(accent))(
                      div.cssClass("mp-product-glyph").style(Style.color(accent))(Icons(groupGlyph(listing.group), 26, 1.8)),
                      span(catalogGroupLabel(listing.group, lang)).style(
                        Style.padding(4.px, 10.px)
                          .rounded(999.px)
                          .bg(surface)
                          .border(1.px, Palette.border)
                          .color(accent)
                          .fontSize(12.px)
                          .fontWeight(FontWeight.w700)
                          .position(Position.relative)
                          .zIndex(1),
                      ),
                    ),
                  div.cssClass("mp-product-body")(
                    h3(title).style(Style.margin(0.px).fontSize(17.px).fontWeight(FontWeight.w700).letterSpacing((-0.2).px).color(ink)),
                    p(listing.tagline(lang)).style(mutedText.margin(0.px).flexGrow(1.0)),
                    div.style(Style.row.flexWrap(FlexWrap.wrap).gap(6.px))(
                      if presetCount > 0 then
                        span.cssClass("mp-chip")(
                          Icons(Icon.Sparkles, 13),
                          span(state.i18n.t(pluralKey("catalog-presets", presetCount, lang), Dict("count" -> presetCount.toString))),
                        )
                      else UI.empty,
                      if compCount > 1 then
                        span.cssClass("mp-chip")(
                          Icons(Icon.Layers, 13),
                          span(state.i18n.t(pluralKey("catalog-parts", compCount, lang), Dict("count" -> compCount.toString))),
                        )
                      else UI.empty,
                    ),
                    div.style(Style.row.gap(8.px).flexWrap(FlexWrap.wrap).padding(6.px, 0.px, 0.px, 0.px))(
                      button
                        .cssClass("mp-btn mp-btn-secondary mp-btn-sm")
                        .style(Style.flexGrow(1.0))
                        .onClick(state.openProductDetail(listing.categoryId))(span(state.i18n.t("catalog-btn-details"))),
                      button
                        .cssClass("mp-btn mp-btn-primary mp-btn-sm")
                        .style(Style.flexGrow(1.0))
                        .onClick(state.openBuilder(Some(listing.categoryId)))(
                          span(state.i18n.t("catalog-btn-configure")),
                          Icons(Icon.ArrowRight, 15),
                        ),
                    ),
                  ),
                )
              }*
            )
        },
      )

  private def matches(state: StorefrontState, listing: ShowcaseListing, q: String, lang: Language): Boolean =
    if q.isEmpty then true
    else
      val catOpt     = state.catalog.categories.get(listing.categoryId)
      val nameHit    = catOpt.exists(c => c.name(lang).toLowerCase.contains(q) || c.name(Language.En).toLowerCase.contains(q))
      val taglineHit = listing.tagline(lang).toLowerCase.contains(q) || listing.detailedDescription(lang).toLowerCase.contains(q)
      val groupHit   = catalogGroupLabel(listing.group, lang).toLowerCase.contains(q)
      nameHit || taglineHit || groupHit
