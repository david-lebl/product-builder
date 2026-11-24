package mpbuilder.admin

import kyo.*
import kyo.UI.*
import kyo.Style.*
import mpbuilder.admin.state.{AdminRoute, BackofficeState}
import mpbuilder.admin.theme.AdminTheme.AdminPalette.*
import mpbuilder.admin.views.*
import mpbuilder.shared.Language
import mpbuilder.ui.components.CommonComponents.*
import mpbuilder.ui.components.{Controls, Icons}
import mpbuilder.ui.components.Icons.Icon
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
      orderIntakeUI  <- OrderIntakeView.render(state)
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
                      div
                        .role("alert")
                        .style(rowBetween.padding(10.px, 10.px, 10.px, 14.px).rounded(12.px).bg(roseSoft).border(1.px, roseBorder).color(roseInk))(
                          div.style(rowSm)(
                            Icons(Icon.AlertTriangle, 17),
                            span(err).style(Style.fontSize(13.px).fontWeight(FontWeight.w600)),
                          ),
                          button
                            .cssClass("mp-icon-btn mp-icon-btn-sm")
                            .style(Style.color(roseInk))
                            .aria("label", "Dismiss")
                            .onClick(state.dismissError)(Icons(Icon.Close, 15)),
                        )
                  },

                  // Routed View
                  state.route.render {
                    case AdminRoute.Dashboard        => DashboardView.render(state)
                    case AdminRoute.Orders           => orderIntakeUI
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
        state.artworkPreview.render(state.i18n.language, url => state.notifyLinkCopied(url)),
        renderToast(state),
      )

  private final case class NavItem(route: AdminRoute, icon: Icon, labelKey: String)

  private val navItems: List[NavItem] = List(
    NavItem(AdminRoute.Dashboard, Icon.Dashboard, "adm-nav-dashboard"),
    NavItem(AdminRoute.Orders, Icon.Inbox, "adm-nav-orders"),
    NavItem(AdminRoute.Catalog, Icon.Layers, "adm-nav-catalog"),
    NavItem(AdminRoute.Pricing, Icon.Tag, "adm-nav-pricing"),
    NavItem(AdminRoute.Customers, Icon.Users, "adm-nav-customers"),
    NavItem(AdminRoute.StaffAndSettings, Icon.Settings, "adm-nav-settings"),
  )

  private def isActive(item: NavItem, active: AdminRoute): Boolean = (item.route, active) match
    case (AdminRoute.Orders, AdminRoute.OrderDetail(_)) => true
    case (r, a)                                         => r == a

  private def navCount(item: NavItem, dashOpt: Option[mpbuilder.backoffice.BackofficeDashboardSummary]): Option[Int] =
    item.route match
      case AdminRoute.Orders    => dashOpt.map(_.ordersAwaitingIntake).filter(_ > 0)
      case AdminRoute.Customers => dashOpt.map(_.pendingB2BCustomers).filter(_ > 0)
      case _                    => None

  private def renderSidebar(
      state: BackofficeState,
      staffAuth: mpbuilder.backoffice.StaffAuthResult,
      onBackToStorefront: Option[() => Unit < Async],
  ): UI =
    val navSignal = state.route.combineLatest(state.dashboard)

    div.cssClass("mp-admin-sidebar")(
      // Brand Header
      div.style(Style.row.align(Alignment.center).gap(11.px).padding(4.px, 6.px, 8.px, 6.px))(
        div.cssClass("mp-logo-mark")(Icons(Icon.Printer, 19)),
        div.style(Style.column.gap(1.px).minWidth(0.px))(
          span(state.i18n.t("brand-name")).style(Style.fontSize(15.px).fontWeight(FontWeight.w800).letterSpacing((-0.2).px).color(Color.white)),
          span(state.i18n.t("adm-brand-subtitle")).style(Style.fontSize(11.px).color(sidebarMuted).fontWeight(FontWeight.w600)),
        ),
      ),

      // Navigation Links
      navSignal.render { case (activeRoute, dashOpt) =>
        nav.style(Style.column.gap(3.px).flexGrow(1.0)).aria("label", "Backoffice")(
          UI.fragment(
            navItems.map { item =>
              val active = isActive(item, activeRoute)
              button
                .cssClass(if active then "mp-admin-nav-btn-active" else "mp-admin-nav-btn")
                .aria("current", if active then "page" else "false")
                .onClick(state.navigate(item.route))(
                  span.style(Style.color(if active then sidebarAccent else sidebarMuted).row)(Icons(item.icon, 18)),
                  span(state.i18n.t(item.labelKey)).style(Style.flexGrow(1.0).minWidth(0.px)),
                  navCount(item, dashOpt) match
                    case Some(cnt) => span(cnt.toString).cssClass("mp-admin-nav-count")
                    case None      => UI.empty,
                )
            }*
          ),
        )
      },

      // Staff Profile Card
      div.cssClass("mp-admin-profile")(
        div.style(Style.row.align(Alignment.center).gap(10.px))(
          Controls.avatar(staffAuth.employee.displayName.value),
          div.style(Style.column.gap(1.px).minWidth(0.px).flexGrow(1.0))(
            span(staffAuth.employee.displayName.value).style(Style.fontSize(13.px).fontWeight(FontWeight.w700).color(Color.white)),
            span(staffAuth.employee.email.value).style(
              Style.fontSize(11.px).color(sidebarMuted).overflow(Overflow.hidden).textOverflow(TextOverflow.ellipsis).whiteSpace(WhiteSpace.noWrap),
            ),
          ),
        ),
        div.style(rowWrap.gap(4.px))(
          staffAuth.employee.roles.toList.sortBy(_.ordinal).map { r =>
            span(r.toString).style(
              Style.padding(2.px, 7.px)
                .rounded(6.px)
                .bg(sidebarHover)
                .color(sidebarText)
                .fontSize(10.px)
                .fontWeight(FontWeight.w700)
                .letterSpacing(0.3.px),
            )
          }*
        ),
        div.style(Style.row.gap(6.px))(
          onBackToStorefront match
            case Some(goBack) =>
              button.cssClass("mp-admin-side-btn").onClick(goBack())(Icons(Icon.Store, 15), span(state.i18n.t("adm-back-to-storefront")))
            case None => UI.empty,
          button
            .cssClass("mp-admin-side-btn")
            .style(Style.flexGrow(0.0))
            .aria("label", "Sign out")
            .onClick(state.logout)(Icons(Icon.LogOut, 15)),
        ),
      ),
    )

  private def renderTopbar(
      state: BackofficeState,
      staffAuth: mpbuilder.backoffice.StaffAuthResult,
      onBackToStorefront: Option[() => Unit < Async],
  ): UI =
    header.cssClass("mp-admin-topbar")(
      div.style(rowSm.minWidth(0.px))(
        div.cssClass("mp-mobile-only")(
          state.mobileNavOpen.render { open =>
            Controls.iconButton(if open then Icon.Close else Icon.Menu, "Menu", state.mobileNavOpen.updateAndGet(!_).unit, outlined = true)
          },
        ),
        // Breadcrumb: Backoffice / <Section>
        state.route.render { r =>
          val item = navItems.find(i => isActive(i, r)).getOrElse(navItems.head)
          div.style(rowSm.minWidth(0.px)).aria("label", "Breadcrumb")(
            span(state.i18n.t("adm-brand-subtitle")).cssClass("mp-hide-sm").style(Style.fontSize(14.px).fontWeight(FontWeight.w600).color(inkMuted)),
            span.cssClass("mp-hide-sm").style(Style.color(borderStrong).row)(Icons(Icon.ChevronRight, 15)),
            span.style(Style.color(brand).row)(Icons(item.icon, 17)),
            span(state.i18n.t(item.labelKey)).style(Style.fontSize(15.px).fontWeight(FontWeight.w700).color(ink).whiteSpace(WhiteSpace.noWrap)),
          )
        },
      ),
      div.style(rowSm)(
        onBackToStorefront match
          case Some(goBack) =>
            button
              .cssClass("mp-btn mp-btn-ghost mp-btn-sm mp-hide-sm")
              .onClick(goBack())(Icons(Icon.Store, 15), span(state.i18n.t("adm-back-to-storefront")))
          case None => UI.empty,
        div.cssClass("mp-hide-sm")(Controls.languageSwitch(state.i18n, l => state.setLanguage(l))),
        Controls.themeToggle(state.theme, state.i18n),
        state.i18n.language.render { lang =>
          Controls.iconButton(Icon.Refresh, if lang == Language.Cs then "Obnovit" else "Refresh", state.refreshCurrentRoute)
        },
        div.style(Style.width(1.px).height(24.px).bg(Palette.border).margin(0.px, 4.px)).cssClass("mp-hide-sm"),
        button
          .cssClass("mp-btn mp-btn-ghost mp-btn-sm")
          .style(Style.padding(3.px, 10.px, 3.px, 3.px).rounded(999.px))
          .onClick(state.logout)(
            Controls.avatar(staffAuth.employee.displayName.value, "sm"),
            span(state.i18n.t("adm-sign-out")).cssClass("mp-hide-sm"),
            Icons(Icon.LogOut, 15),
          ),
      ),
    )

  private def renderMobileNav(
      state: BackofficeState,
      onBackToStorefront: Option[() => Unit < Async],
  ): UI =
    UI.when(state.mobileNavOpen)(
      state.route.render { activeRoute =>
        div.cssClass("mp-admin-mobile-nav")(
          UI.fragment(
            navItems.map { item =>
              val active = isActive(item, activeRoute)
              button
                .cssClass(if active then "mp-btn mp-btn-nav-active" else "mp-btn mp-btn-ghost")
                .style(Style.justify(Justification.start).width(100.pct).padding(10.px, 12.px))
                .onClick(state.mobileNavOpen.set(false).andThen(state.navigate(item.route)))(Icons(item.icon, 18), span(state.i18n.t(item.labelKey)))
            }*
          ),
          div.style(Style.row.gap(8.px).align(Alignment.center).padding(10.px, 0.px, 2.px, 0.px).borderTop(1.px, Palette.border))(
            Controls.languageSwitch(state.i18n, l => state.setLanguage(l)),
            onBackToStorefront match
              case Some(goBack) =>
                button
                  .cssClass("mp-btn mp-btn-secondary mp-btn-sm")
                  .style(Style.flexGrow(1.0))
                  .onClick(state.mobileNavOpen.set(false).andThen(goBack()))(Icons(Icon.Store, 15), span(state.i18n.t("adm-back-to-storefront")))
              case None => UI.empty,
          ),
        )
      },
    )

  private def renderToast(state: BackofficeState): UI =
    state.toast.render {
      case None      => UI.empty
      case Some(msg) => Controls.toast(msg, state.dismissToast)
    }
