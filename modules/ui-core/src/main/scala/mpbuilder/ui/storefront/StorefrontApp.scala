package mpbuilder.ui.storefront

import kyo.*
import kyo.UI.*
import kyo.Style.*
import mpbuilder.shared.*
import mpbuilder.ui.components.CommonComponents.*
import mpbuilder.ui.components.{Controls, Icons}
import mpbuilder.ui.components.Icons.Icon
import mpbuilder.ui.configurator.{ConfiguratorHost, ConfiguratorView, DesignOnlineRequest}
import mpbuilder.ui.theme.AppTheme.*
import mpbuilder.ui.theme.AppTheme.Palette.*

/** Complete responsive Storefront SPA shell (Header, Mobile Drawer, Routed Views, Cart Drawer, Auth Modal, Toast & Footer). */
object StorefrontApp:

  def render(
      state: StorefrontState,
      onOpenAdmin: Option[() => Unit < Async] = None,
      onDesignOnline: Option[DesignOnlineRequest => Unit < Async] = None,
      designStudioHref: Option[String] = None,
  ): UI < Async =
    for
      catalogUI    <- CatalogView.render(state)
      accountUI    <- AccountView.render(state)
      cartDrawerUI <- CartDrawer.render(state)
      authModalUI  <- AuthModal.render(state)
    yield
      val builderHost = ConfiguratorHost.Storefront(
        onAddToCart = (draft, _) => state.addDraftToCart(draft),
        onQuickOrder = (draft, _) => state.startQuickOrder(draft),
        onPreviewArtwork = (art, ctx) => state.artworkPreview.open(art, contextTitle = ctx),
        onLinkCopied = url => state.notifyLinkCopied(url),
        onArtworkUploaded = () => state.i18n.now("cfg-artwork-uploaded-toast").flatMap(state.showToast),
        onArtworkRemoved = () => state.i18n.now("cfg-artwork-removed-toast").flatMap(state.showToast),
        onDesignOnline = onDesignOnline,
      )

      val builderUI: UI = ConfiguratorView.render(state.configurator, builderHost, showHeader = true)

      div.cssClass("mp-app")(
        renderHeader(state, onOpenAdmin, designStudioHref),
        // Main Content
        main.cssClass("mp-container mp-main")(
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
            .padding(22.px, 0.px)
            .margin(32.px, 0.px, 0.px, 0.px),
        )(
          div.cssClass("mp-container").style(Style.row.flexWrap(FlexWrap.wrap).justify(Justification.spaceBetween).align(Alignment.center).gap(12.px))(
            div.style(rowSm)(
              logoMark(26),
              span(state.i18n.t("brand-name")).style(Style.fontWeight(FontWeight.w700).fontSize(14.px).color(ink)),
            ),
            div.style(Style.row.flexWrap(FlexWrap.wrap).align(Alignment.center).gap(12.px))(
              span(state.i18n.t("footer-static-note")).style(smallText),
              onOpenAdmin match
                case Some(openAdmin) =>
                  button
                    .cssClass("mp-btn mp-btn-secondary mp-btn-sm")
                    .onClick(openAdmin())(Icons(Icon.Shield, 14), span(state.i18n.t("nav-admin")))
                case None => UI.empty,
            ),
          ),
        ),
        // Overlays
        cartDrawerUI,
        authModalUI,
        state.artworkPreview.render(state.i18n.language, url => state.notifyLinkCopied(url)),
        renderToast(state),
      )

  private def logoMark(size: Int = 34): UI =
    div.cssClass("mp-logo-mark").style(Style.width(size.px).height(size.px))(Icons(Icon.Printer, (size * 0.55).toInt, 2.0))

  private def renderHeader(
      state: StorefrontState,
      onOpenAdmin: Option[() => Unit < Async],
      designStudioHref: Option[String],
  ): UI =
    val headerSignal =
      state.route
        .combineLatest(state.i18n.language)
        .combineLatest(state.authSession)
        .combineLatest(state.currentCustomer)
        .combineLatest(state.cartItemCountSignal)

    def navBtn(icon: Icon, labelKey: String, active: Boolean, action: => Unit < Async): UI =
      button
        .cssClass(if active then "mp-btn mp-btn-nav-active mp-btn-sm" else "mp-btn mp-btn-ghost mp-btn-sm")
        .aria("current", if active then "page" else "false")
        .onClick(action)(Icons(icon, 16), span(state.i18n.t(labelKey)).cssClass("mp-nav-label"))

    // The mobile menu lives inside the sticky header so it opens under the header
    // at the current scroll position instead of at the top of the document.
    header.cssClass("mp-header")(
      div.cssClass("mp-header-inner")(
        // Brand
        button
          .cssClass("mp-btn mp-btn-ghost")
          .style(Style.padding(4.px, 8.px, 4.px, 4.px).gap(10.px).hover(Style.bg(Color.transparent)))
          .aria("label", "PrintCraft Studio")
          .onClick(state.openCatalog)(
            logoMark(),
            span(state.i18n.t("brand-name")).style(Style.fontSize(17.px).fontWeight(FontWeight.w800).letterSpacing((-0.3).px).color(ink)),
          ),

        // Navigation & Controls
        headerSignal.render { case ((((activeRoute, lang), sessOpt), custOpt), cartCount) =>
          val isCatalog = activeRoute match
            case AppRoute.Catalog | _: AppRoute.ProductDetail => true
            case _                                            => false
          val isBuilder = activeRoute.isInstanceOf[AppRoute.Builder]
          val isAccount = activeRoute == AppRoute.Account
          val isCs      = lang == Language.Cs

          div.style(Style.row.align(Alignment.center).gap(10.px).flexWrap(FlexWrap.noWrap).minWidth(0.px))(
            nav.cssClass("mp-desktop-nav").aria("label", if isCs then "Hlavní navigace" else "Main navigation")(
              navBtn(Icon.Grid, "nav-catalog", isCatalog, state.openCatalog),
              navBtn(Icon.Wand, "nav-builder", isBuilder, state.openBuilder()),
              designStudioHref match
                case Some(href) =>
                  a.cssClass("mp-btn mp-btn-ghost mp-btn-sm")
                    .href(UI.Href.Path(href))(Icons(Icon.Pencil, 16), span(state.i18n.t("nav-design-studio")).cssClass("mp-nav-label"))
                case None => UI.empty,
              if sessOpt.isDefined then navBtn(Icon.User, "nav-account", isAccount, state.openAccount) else UI.empty,
              onOpenAdmin match
                case Some(openAdmin) => navBtn(Icon.Shield, "nav-admin", false, openAdmin())
                case None            => UI.empty,
            ),

            div.cssClass("mp-desktop-nav").style(Style.gap(8.px))(
              Controls.languageSwitch(state.i18n, l => state.setLanguage(l)),
            ),

            Controls.themeToggle(state.theme, state.i18n),

            // Account (desktop)
            div.cssClass("mp-desktop-nav")(
              sessOpt match
                case Some(sess) =>
                  button
                    .cssClass("mp-btn mp-btn-secondary mp-btn-sm")
                    .style(Style.padding(3.px, 10.px, 3.px, 3.px).gap(8.px).rounded(999.px))
                    .onClick(state.openAccount)(
                      Controls.avatar(sess.principal.displayName.value, "sm"),
                      span(sess.principal.displayName.value).cssClass("mp-nav-label").style(Style.fontSize(13.px).fontWeight(FontWeight.w700).color(ink)),
                      custOpt match
                        case Some(c) => tierBadge(c.tier, compact = true)
                        case None    => UI.empty,
                    )
                case None =>
                  button
                    .cssClass("mp-btn mp-btn-secondary mp-btn-sm")
                    .onClick(state.authModalOpen.set(true))(Icons(Icon.LogIn, 15), span(state.i18n.t("nav-sign-in")).cssClass("mp-nav-label")),
            ),

            // Cart
            button
              .cssClass("mp-btn mp-btn-primary mp-btn-sm")
              .style(Style.padding(7.px, 12.px))
              .aria("label", if isCs then s"Košík ($cartCount)" else s"Cart ($cartCount)")
              .onClick(state.cartDrawerOpen.set(true))(
                Icons(Icon.Cart, 16),
                span(state.i18n.t("nav-cart")).cssClass("mp-hide-sm"),
                Controls.countPill(cartCount),
              ),

            // Mobile menu toggle
            div.cssClass("mp-mobile-only")(
              state.mobileMenuOpen.render { open =>
                Controls.iconButton(
                  if open then Icon.Close else Icon.Menu,
                  if isCs then "Menu" else "Menu",
                  state.mobileMenuOpen.updateAndGet(!_).unit,
                  outlined = true,
                )
              },
            ),
          )
        },
      ),
      renderMobileMenu(state, onOpenAdmin, designStudioHref),
    )

  private def renderMobileMenu(
      state: StorefrontState,
      onOpenAdmin: Option[() => Unit < Async],
      designStudioHref: Option[String],
  ): UI =
    def item(icon: Icon, labelNode: UI, action: => Unit < Async): UI =
      button
        .cssClass("mp-btn mp-btn-ghost")
        .style(Style.justify(Justification.start).width(100.pct).padding(11.px, 12.px).fontSize(15.px))
        .onClick(state.mobileMenuOpen.set(false).andThen(action))(Icons(icon, 18), labelNode)

    UI.when(state.mobileMenuOpen)(
      state.authSession.combineLatest(state.currentCustomer).render { case (sessOpt, custOpt) =>
        div.cssClass("mp-mobile-menu").style(Style.animation("mp-drop-in", 160, Easing.easeOut))(
          sessOpt match
            case Some(sess) =>
              div.style(rowMd.padding(4.px, 4.px, 10.px, 4.px).borderBottom(1.px, Palette.border))(
                Controls.avatar(sess.principal.displayName.value, "lg"),
                div.style(stackXs)(
                  span(sess.principal.displayName.value).style(Style.fontWeight(FontWeight.w700).fontSize(15.px)),
                  custOpt.fold(UI.empty: UI)(c => tierBadge(c.tier, compact = true)),
                ),
              )
            case None =>
              button
                .cssClass("mp-btn mp-btn-primary")
                .style(Style.width(100.pct))
                .onClick(state.mobileMenuOpen.set(false).andThen(state.authModalOpen.set(true)))(
                  Icons(Icon.LogIn, 16),
                  span(state.i18n.t("nav-sign-in")),
                ),
          nav.style(Style.column.gap(2.px))(
            item(Icon.Grid, span(state.i18n.t("nav-catalog")), state.openCatalog),
            item(Icon.Wand, span(state.i18n.t("nav-builder")), state.openBuilder()),
            designStudioHref match
              case Some(href) =>
                a.cssClass("mp-btn mp-btn-ghost")
                  .style(Style.justify(Justification.start).width(100.pct).padding(11.px, 12.px).fontSize(15.px))
                  .href(UI.Href.Path(href))(Icons(Icon.Pencil, 18), span(state.i18n.t("nav-design-studio")))
              case None => UI.empty,
            if sessOpt.isDefined then item(Icon.User, span(state.i18n.t("nav-account")), state.openAccount) else UI.empty,
            onOpenAdmin match
              case Some(openAdmin) => item(Icon.Shield, span(state.i18n.t("nav-admin")), openAdmin())
              case None            => UI.empty,
          ),
          div.style(Style.row.gap(8.px).align(Alignment.center).padding(10.px, 0.px, 0.px, 0.px).borderTop(1.px, Palette.border))(
            Controls.languageSwitch(state.i18n, l => state.setLanguage(l)),
          ),
          Controls.themeSegmented(state.theme, state.i18n),
        )
      },
    )

  private def renderToast(state: StorefrontState): UI =
    state.toast.render {
      case None      => UI.empty
      case Some(msg) => Controls.toast(msg, state.dismissToast)
    }
