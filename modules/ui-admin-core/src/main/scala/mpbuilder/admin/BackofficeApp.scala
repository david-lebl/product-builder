package mpbuilder.admin

import kyo.*
import kyo.UI.*
import kyo.Style.*
import mpbuilder.admin.state.{AdminRoute, BackofficeState}
import mpbuilder.admin.theme.AdminTheme.AdminPalette.*
import mpbuilder.admin.views.*
import mpbuilder.shared.Language
import mpbuilder.ui.components.CommonComponents.*
import mpbuilder.ui.theme.AppTheme.*
import mpbuilder.ui.theme.AppTheme.Palette.*

/** Complete responsive Backoffice SPA shell (Login gate, Left Sidebar, Mobile Nav, Topbar, Routed Views & Toasts). */
object BackofficeApp:

  def render(
      state: BackofficeState,
      onBackToStorefront: Option[() => Unit < Async] = None,
  ): UI < Sync =
    for
      loginUI        <- AdminLoginView.render(state, onBackToStorefront)
      orderDetailUI  <- OrderDetailView.render(state)
      catalogMgrUI   <- CatalogManagerView.render(state)
      pricingMgrUI   <- PricingManagerView.render(state)
      customerMgrUI  <- CustomerManagerView.render(state)
      staffSetUI     <- StaffSettingsView.render(state)
    yield
      val shellUI =
        state.auth.render {
          case None => loginUI
          case Some(staffAuth) =>
            div.cssClass("mp-admin-shell")(
              // Desktop Left Sidebar
              renderSidebar(state, staffAuth, onBackToStorefront),

              // Fluid Workspace
              div.cssClass("mp-admin-workspace")(
                renderTopbar(state, staffAuth, onBackToStorefront),
                renderMobileNav(state, onBackToStorefront),
                main.cssClass("mp-admin-content")(
                  // Dismissible error banner
                  state.errorBanner.render {
                    case None => UI.empty: UI
                    case Some(err) =>
                      div.style(rowBetween.padding(12.px, 16.px).rounded(12.px).bg(roseSoft).border(1.px, roseBorder).color(roseInk))(
                        span(err).style(Style.fontSize(13.px).fontWeight(FontWeight.w600)),
                        button
                          .cssClass("mp-btn mp-btn-ghost mp-btn-sm")
                          .style(Style.color(roseInk).padding(2.px, 8.px))
                          .onClick(state.dismissError)("✕"),
                      )
                  },

                  // Routed View
                  state.route.render {
                    case AdminRoute.Dashboard        => DashboardView.render(state)
                    case AdminRoute.Orders           => OrderIntakeView.render(state)
                    case AdminRoute.OrderDetail(_)   => orderDetailUI
                    case AdminRoute.Catalog          => catalogMgrUI
                    case AdminRoute.Pricing          => pricingMgrUI
                    case AdminRoute.Customers        => customerMgrUI
                    case AdminRoute.StaffAndSettings => staffSetUI
                  },
                ),
              ),
            )
        }

      UI.fragment(
        shellUI,
        renderToast(state),
      )

  private def renderSidebar(
      state: BackofficeState,
      staffAuth: mpbuilder.backoffice.StaffAuthResult,
      onBackToStorefront: Option[() => Unit < Async],
  ): UI =
    val navSignal = state.route.combineLatest(state.dashboard)

    div.cssClass("mp-admin-sidebar")(
      // Brand Header
      div.style(Style.row.align(Alignment.center).gap(10.px).padding(4.px, 6.px))(
        div.style(
          Style.width(34.px)
            .height(34.px)
            .rounded(10.px)
            .bgGradient(GradientDirection.toBottomRight, (brand, 0.pct), (Color.hex("#7C3AED").getOrElse(Color.purple), 100.pct))
            .color(Color.white)
            .row
            .align(Alignment.center)
            .justify(Justification.center)
            .fontWeight(FontWeight.w800)
            .fontSize(15.px),
        )("P"),
        div.style(Style.column.gap(1.px))(
          span(state.i18n.t("brand-name")).style(Style.fontSize(15.px).fontWeight(FontWeight.w800).color(Color.white)),
          span(state.i18n.t("adm-brand-subtitle")).style(Style.fontSize(11.px).color(sidebarMuted).fontWeight(FontWeight.w600)),
        ),
      ),

      // Navigation Links
      navSignal.render { case (activeRoute, dashOpt) =>
        val intakeCount  = dashOpt.map(_.ordersAwaitingIntake).filter(_ > 0)
        val pendingB2B   = dashOpt.map(_.pendingB2BCustomers).filter(_ > 0)
        val isOrders     = activeRoute == AdminRoute.Orders || activeRoute.isInstanceOf[AdminRoute.OrderDetail]

        nav.style(Style.column.gap(4.px).flexGrow(1.0))(
          sidebarBtn(state.i18n.t("adm-nav-dashboard"), activeRoute == AdminRoute.Dashboard, None, state.navigate(AdminRoute.Dashboard)),
          sidebarBtn(state.i18n.t("adm-nav-orders"), isOrders, intakeCount, state.navigate(AdminRoute.Orders)),
          sidebarBtn(state.i18n.t("adm-nav-catalog"), activeRoute == AdminRoute.Catalog, None, state.navigate(AdminRoute.Catalog)),
          sidebarBtn(state.i18n.t("adm-nav-pricing"), activeRoute == AdminRoute.Pricing, None, state.navigate(AdminRoute.Pricing)),
          sidebarBtn(state.i18n.t("adm-nav-customers"), activeRoute == AdminRoute.Customers, pendingB2B, state.navigate(AdminRoute.Customers)),
          sidebarBtn(state.i18n.t("adm-nav-settings"), activeRoute == AdminRoute.StaffAndSettings, None, state.navigate(AdminRoute.StaffAndSettings)),
        )
      },

      // Bottom Staff Profile + Storefront link
      div.style(Style.column.gap(10.px).padding(12.px, 6.px, 4.px, 6.px).borderTop(1.px, sidebarBorder))(
        div.style(Style.column.gap(4.px))(
          span(staffAuth.employee.displayName.value).style(Style.fontSize(13.px).fontWeight(FontWeight.w700).color(Color.white)),
          span(staffAuth.employee.email.value).style(Style.fontSize(11.px).color(sidebarMuted)),
          div.style(rowWrap)(
            staffAuth.employee.roles.toList.sortBy(_.ordinal).map { r =>
              span(r.toString).style(
                Style.padding(2.px, 7.px)
                  .rounded(999.px)
                  .bg(sidebarHover)
                  .color(sidebarText)
                  .fontSize(11.px)
                  .fontWeight(FontWeight.w600),
              )
            }*
          ),
        ),
        onBackToStorefront match
          case Some(goBack) =>
            button
              .cssClass("mp-admin-nav-btn")
              .style(Style.bg(sidebarHover).color(Color.white).justify(Justification.center))
              .onClick(goBack())(state.i18n.t("adm-back-to-storefront"))
          case None => UI.empty,
      ),
    )

  private def sidebarBtn(
      labelNode: UI,
      active: Boolean,
      countOpt: Option[Int],
      action: => Unit < Async,
  ): UI =
    button
      .cssClass(if active then "mp-admin-nav-btn-active" else "mp-admin-nav-btn")
      .onClick(action)(
        span(labelNode),
        countOpt match
          case Some(cnt) =>
            span(cnt.toString).style(
              Style.padding(1.px, 7.px)
                .rounded(999.px)
                .bg(amber)
                .color(Color.white)
                .fontSize(11.px)
                .fontWeight(FontWeight.w700),
            )
          case None => UI.empty,
      )

  private def renderTopbar(
      state: BackofficeState,
      staffAuth: mpbuilder.backoffice.StaffAuthResult,
      onBackToStorefront: Option[() => Unit < Async],
  ): UI =
    header.cssClass("mp-admin-topbar")(
      div.style(rowSm)(
        div.cssClass("mp-mobile-only")(
          button
            .cssClass("mp-btn mp-btn-secondary mp-btn-sm")
            .onClick(state.mobileNavOpen.updateAndGet(!_).unit)(state.i18n.t("nav-menu")),
        ),
        span(state.i18n.t("adm-brand-subtitle")).style(Style.fontWeight(FontWeight.w700).fontSize(15.px).color(ink)),
        badge(staffAuth.employee.displayName.value, BadgeTone.Brand),
      ),
      div.style(rowSm.flexWrap(FlexWrap.wrap))(
        onBackToStorefront match
          case Some(goBack) =>
            button.cssClass("mp-btn mp-btn-secondary mp-btn-sm").onClick(goBack())(state.i18n.t("adm-back-to-storefront"))
          case None => UI.empty,
        // Language toggle
        state.i18n.language.render { lang =>
          div.style(Style.row.gap(2.px).padding(3.px).rounded(10.px).bg(surfaceAlt).border(1.px, Palette.border))(
            List(Language.En -> "EN", Language.Cs -> "CS").map { case (l, code) =>
              button
                .cssClass(if lang == l then "mp-btn mp-btn-primary mp-btn-sm" else "mp-btn mp-btn-ghost mp-btn-sm")
                .style(Style.padding(4.px, 8.px).fontSize(12.px))
                .onClick(state.setLanguage(l))(code)
            }*
          )
        },
        button.cssClass("mp-btn mp-btn-ghost mp-btn-sm").onClick(state.refreshCurrentRoute)(state.i18n.t("adm-refresh")),
        button.cssClass("mp-btn mp-btn-secondary mp-btn-sm").onClick(state.logout)(state.i18n.t("adm-sign-out")),
      ),
    )

  private def renderMobileNav(
      state: BackofficeState,
      onBackToStorefront: Option[() => Unit < Async],
  ): UI =
    UI.when(state.mobileNavOpen)(
      div.cssClass("mp-admin-mobile-nav")(
        div.style(rowWrap)(
          button.cssClass("mp-btn mp-btn-secondary mp-btn-sm").onClick(state.navigate(AdminRoute.Dashboard))(state.i18n.t("adm-nav-dashboard")),
          button.cssClass("mp-btn mp-btn-secondary mp-btn-sm").onClick(state.navigate(AdminRoute.Orders))(state.i18n.t("adm-nav-orders")),
          button.cssClass("mp-btn mp-btn-secondary mp-btn-sm").onClick(state.navigate(AdminRoute.Catalog))(state.i18n.t("adm-nav-catalog")),
          button.cssClass("mp-btn mp-btn-secondary mp-btn-sm").onClick(state.navigate(AdminRoute.Pricing))(state.i18n.t("adm-nav-pricing")),
          button.cssClass("mp-btn mp-btn-secondary mp-btn-sm").onClick(state.navigate(AdminRoute.Customers))(state.i18n.t("adm-nav-customers")),
          button.cssClass("mp-btn mp-btn-secondary mp-btn-sm").onClick(state.navigate(AdminRoute.StaffAndSettings))(state.i18n.t("adm-nav-settings")),
          onBackToStorefront match
            case Some(goBack) =>
              button.cssClass("mp-btn mp-btn-primary mp-btn-sm").onClick(state.mobileNavOpen.set(false).andThen(goBack()))(state.i18n.t("adm-back-to-storefront"))
            case None => UI.empty,
        ),
      ),
    )

  private def renderToast(state: BackofficeState): UI =
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
