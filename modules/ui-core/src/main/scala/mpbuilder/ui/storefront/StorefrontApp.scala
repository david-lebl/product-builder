package mpbuilder.ui.storefront

import kyo.*
import kyo.UI.*
import kyo.Style.*
import mpbuilder.shared.*
import mpbuilder.ui.components.CommonComponents.*
import mpbuilder.ui.configurator.{ConfiguratorHost, ConfiguratorView}
import mpbuilder.ui.theme.AppTheme.*
import mpbuilder.ui.theme.AppTheme.Palette.*

/** Complete responsive Storefront SPA shell (Header, Mobile Drawer, Routed Views, Cart Drawer, Auth Modal, Toast & Footer). */
object StorefrontApp:

  def render(
      state: StorefrontState,
      onOpenAdmin: Option[() => Unit < Async] = None,
  ): UI < Async =
    for
      catalogUI    <- CatalogView.render(state)
      accountUI    <- AccountView.render(state)
      cartDrawerUI <- CartDrawer.render(state)
      authModalUI  <- AuthModal.render(state)
    yield
      val builderHost = ConfiguratorHost.Storefront(
        onAddToCart = (draft, _) => state.addDraftToCart(draft),
        onQuickOrder = (draft, quoteRes) => state.startQuickOrder(quoteRes, draft.speed),
      )

      val builderUI: UI = ConfiguratorView.render(state.configurator, builderHost, showHeader = true)

      div.cssClass("mp-app")(
        renderHeader(state, onOpenAdmin),
        renderMobileMenu(state, onOpenAdmin),
        // Main Content
        main.cssClass("mp-container").style(Style.padding(28.px, 24.px, 56.px, 24.px).flexGrow(1.0))(
          state.route.combineLatest(state.checkoutSession).render { case (currentRoute, checkoutOpt) =>
            currentRoute match
              case AppRoute.Catalog => catalogUI
              case AppRoute.ProductDetail(catId) =>
                ProductDetailView.render(state, catId)
              case AppRoute.Builder(_, _) => builderUI
              case AppRoute.Checkout(_) =>
                checkoutOpt match
                  case Some(sess) => CheckoutView.render(state, sess)
                  case None       => catalogUI
              case AppRoute.Account => accountUI
          },
        ),
        // Footer
        footer.style(
          Style.width(100.pct)
            .borderTop(1.px, Palette.border)
            .bg(surface)
            .padding(20.px, 24.px)
            .margin(24.px, 0.px, 0.px, 0.px),
        )(
          div.cssClass("mp-container").style(Style.row.flexWrap(FlexWrap.wrap).justify(Justification.spaceBetween).align(Alignment.center).gap(12.px))(
            span(state.i18n.t("brand-name")).style(Style.fontWeight(FontWeight.w700).fontSize(14.px).color(ink)),
            div.style(Style.row.flexWrap(FlexWrap.wrap).align(Alignment.center).gap(12.px))(
              span(state.i18n.t("footer-static-note")).style(smallText),
              onOpenAdmin match
                case Some(openAdmin) =>
                  button
                    .cssClass("mp-btn mp-btn-secondary mp-btn-sm")
                    .onClick(openAdmin())(state.i18n.t("nav-admin"))
                case None => UI.empty,
            ),
          ),
        ),
        // Overlays
        cartDrawerUI,
        authModalUI,
        renderToast(state),
      )

  private def renderHeader(
      state: StorefrontState,
      onOpenAdmin: Option[() => Unit < Async],
  ): UI =
    val headerSignal =
      state.route
        .combineLatest(state.currency)
        .combineLatest(state.i18n.language)
        .combineLatest(state.authSession)
        .combineLatest(state.currentCustomer)
        .combineLatest(state.cartItemCountSignal)

    header.cssClass("mp-header")(
      div.cssClass("mp-header-inner")(
        // Brand Logo Button
        button
          .cssClass("mp-btn mp-btn-ghost")
          .style(Style.padding(4.px, 8.px).gap(10.px))
          .onClick(state.openCatalog)(
            div.style(
              Style.width(32.px)
                .height(32.px)
                .rounded(10.px)
                .bgGradient(GradientDirection.toBottomRight, (brandDark, 0.pct), (brand, 100.pct))
                .color(Color.white)
                .row
                .align(Alignment.center)
                .justify(Justification.center)
                .fontWeight(FontWeight.w800)
                .fontSize(15.px),
            )("P"),
            div.style(Style.column.textAlign(TextAlign.left))(
              span(state.i18n.t("brand-name")).style(Style.fontSize(17.px).fontWeight(FontWeight.w800).color(ink)),
            ),
          ),

        // Desktop Nav & Controls
        headerSignal.render { case (((((activeRoute, cur), lang), sessOpt), custOpt), cartCount) =>
          val isCatalog = activeRoute match
            case AppRoute.Catalog | _: AppRoute.ProductDetail => true
            case _                                            => false
          val isBuilder = activeRoute.isInstanceOf[AppRoute.Builder]
          val isAccount = activeRoute == AppRoute.Account

          div.style(Style.row.align(Alignment.center).gap(12.px).flexWrap(FlexWrap.noWrap))(
            // Desktop nav links
            nav.cssClass("mp-desktop-nav")(
              button
                .cssClass(if isCatalog then "mp-btn mp-btn-nav-active mp-btn-sm" else "mp-btn mp-btn-ghost mp-btn-sm")
                .onClick(state.openCatalog)(state.i18n.t("nav-catalog")),
              button
                .cssClass(if isBuilder then "mp-btn mp-btn-nav-active mp-btn-sm" else "mp-btn mp-btn-ghost mp-btn-sm")
                .onClick(state.openBuilder())(state.i18n.t("nav-builder")),
              if sessOpt.isDefined then
                button
                  .cssClass(if isAccount then "mp-btn mp-btn-nav-active mp-btn-sm" else "mp-btn mp-btn-ghost mp-btn-sm")
                  .onClick(state.openAccount)(state.i18n.t("nav-account"))
              else UI.empty,
              onOpenAdmin match
                case Some(openAdmin) =>
                  button
                    .cssClass("mp-btn mp-btn-ghost mp-btn-sm")
                    .style(Style.color(brand))
                    .onClick(openAdmin())(state.i18n.t("nav-admin"))
                case None => UI.empty,
            ),

            // Currency switcher
            div.cssClass("mp-desktop-nav").style(Style.gap(2.px).padding(3.px).rounded(10.px).bg(surfaceAlt).border(1.px, Palette.border))(
              List(Currency.CZK, Currency.USD).map { c =>
                button
                  .cssClass(if cur == c then "mp-btn mp-btn-primary mp-btn-sm" else "mp-btn mp-btn-ghost mp-btn-sm")
                  .style(Style.padding(4.px, 9.px).fontSize(12.px))
                  .onClick(state.setCurrency(c))(c.toString)
              }*
            ),

            // Language switcher
            div.style(Style.row.gap(2.px).padding(3.px).rounded(10.px).bg(surfaceAlt).border(1.px, Palette.border))(
              List(Language.En -> "EN", Language.Cs -> "CS").map { case (l, code) =>
                button
                  .cssClass(if lang == l then "mp-btn mp-btn-primary mp-btn-sm" else "mp-btn mp-btn-ghost mp-btn-sm")
                  .style(Style.padding(4.px, 8.px).fontSize(12.px))
                  .onClick(state.setLanguage(l))(code)
              }*
            ),

            // Auth button (desktop) — displays customer name + color-coded tier directly underneath
            div.cssClass("mp-desktop-nav")(
              sessOpt match
                case Some(sess) =>
                  button
                    .cssClass("mp-btn mp-btn-secondary mp-btn-sm")
                    .style(Style.padding(4.px, 11.px))
                    .onClick(state.openAccount)(
                      div.style(Style.column.align(Alignment.start).gap(2.px).lineHeight(1.15))(
                        span(sess.principal.displayName.value).style(Style.fontSize(12.px).fontWeight(FontWeight.w700).color(ink)),
                        custOpt match
                          case Some(c) => tierBadge(c.tier, compact = true)
                          case None    => UI.empty,
                      ),
                    )
                case None =>
                  button
                    .cssClass("mp-btn mp-btn-secondary mp-btn-sm")
                    .onClick(state.authModalOpen.set(true))(state.i18n.t("nav-sign-in")),
            ),

            // Cart Drawer button
            button
              .cssClass("mp-btn mp-btn-primary mp-btn-sm")
              .onClick(state.cartDrawerOpen.set(true))(
                span(state.i18n.t("nav-cart")),
                if cartCount > 0 then
                  span(cartCount.toString).style(
                    Style.padding(1.px, 7.px)
                      .rounded(999.px)
                      .bg(Color.rgba(255, 255, 255, 0.24))
                      .color(Color.white)
                      .fontSize(12.px)
                      .fontWeight(FontWeight.w700),
                  )
                else UI.empty,
              ),

            // Mobile Hamburger button
            div.cssClass("mp-mobile-only")(
              button
                .cssClass("mp-btn mp-btn-secondary mp-btn-sm")
                .onClick(state.mobileMenuOpen.updateAndGet(!_).unit)(state.i18n.t("nav-menu")),
            ),
          )
        },
      ),
    )

  private def renderMobileMenu(
      state: StorefrontState,
      onOpenAdmin: Option[() => Unit < Async],
  ): UI =
    UI.when(state.mobileMenuOpen)(
      state.currency.combineLatest(state.authSession).combineLatest(state.currentCustomer).render { case ((cur, sessOpt), custOpt) =>
        div.cssClass("mp-mobile-menu")(
          div.style(Style.row.flexWrap(FlexWrap.wrap).gap(8.px))(
            button.cssClass("mp-btn mp-btn-secondary mp-btn-sm").onClick(state.openCatalog)(state.i18n.t("nav-catalog")),
            button.cssClass("mp-btn mp-btn-secondary mp-btn-sm").onClick(state.openBuilder())(state.i18n.t("nav-builder")),
            sessOpt match
              case Some(sess) =>
                button.cssClass("mp-btn mp-btn-secondary mp-btn-sm").onClick(state.openAccount)(
                  div.style(Style.column.align(Alignment.start).gap(2.px))(
                    span(sess.principal.displayName.value).style(Style.fontSize(12.px).fontWeight(FontWeight.w700)),
                    custOpt.fold(UI.empty: UI)(c => tierBadge(c.tier, compact = true)),
                  ),
                )
              case None =>
                button
                  .cssClass("mp-btn mp-btn-primary mp-btn-sm")
                  .onClick(state.mobileMenuOpen.set(false).andThen(state.authModalOpen.set(true)))(state.i18n.t("nav-sign-in")),
            onOpenAdmin match
              case Some(openAdmin) =>
                button
                  .cssClass("mp-btn mp-btn-secondary mp-btn-sm")
                  .onClick(state.mobileMenuOpen.set(false).andThen(openAdmin()))(state.i18n.t("nav-admin"))
              case None => UI.empty,
          ),
          div.style(Style.row.gap(8.px).align(Alignment.center))(
            List(Currency.CZK, Currency.USD).map { c =>
              button
                .cssClass(if cur == c then "mp-pill-active" else "mp-pill")
                .style(Style.padding(4.px, 10.px).fontSize(12.px))
                .onClick(state.setCurrency(c))(c.toString)
            }*
          ),
        )
      },
    )

  private def renderToast(state: StorefrontState): UI =
    state.toast.render {
      case None => UI.empty
      case Some(msg) =>
        div.cssClass("mp-toast").style(Style.row.align(Alignment.center).gap(12.px))(
          span(msg),
          button
            .cssClass("mp-btn mp-btn-ghost mp-btn-sm")
            .style(Style.color(Color.white).padding(2.px, 6.px))
            .onClick(state.dismissToast)("✕"),
        )
    }
