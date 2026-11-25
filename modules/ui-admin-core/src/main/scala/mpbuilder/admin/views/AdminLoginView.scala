package mpbuilder.admin.views

import kyo.*
import kyo.UI.*
import kyo.Style.*
import mpbuilder.admin.state.BackofficeState
import mpbuilder.shared.Language
import mpbuilder.ui.components.{Controls, Icons}
import mpbuilder.ui.components.Icons.Icon
import mpbuilder.ui.components.CommonComponents.*
import mpbuilder.ui.theme.AppTheme.*
import mpbuilder.ui.theme.AppTheme.Palette.*

/** Staff Backoffice login screen with quick-fill demo staff cards for Admin, Manager, and Prepress roles. */
object AdminLoginView:

  private final case class DemoStaff(
      label: String,
      email: String,
      roles: List[String],
      roleKey: String,
      tone: BadgeTone,
  )

  private val demoStaff: List[DemoStaff] = List(
    DemoStaff("Admin Operator", "admin@printshop.cz", List("Admin", "Manager", "Operator"), "adm-login-role-admin", BadgeTone.Brand),
    DemoStaff("Operations Manager", "manager@printshop.cz", List("Manager", "Operator"), "adm-login-role-manager", BadgeTone.Emerald),
    DemoStaff("Prepress Specialist", "prepress@printshop.cz", List("Prepress"), "adm-login-role-prepress", BadgeTone.Amber),
  )

  def render(
      state: BackofficeState,
      onBackToStorefront: Option[() => Unit < Async] = None,
  ): UI < Sync =
    for
      emailRef    <- Signal.initRef("admin@printshop.cz")
      passwordRef <- Signal.initRef("staff1234")
      errorRef    <- Signal.initRef[Option[String]](None)
    yield
      def submitLogin: Unit < Async =
        for
          _   <- errorRef.set(None)
          em  <- emailRef.get
          pw  <- passwordRef.get
          res <- state.login(em, pw)
          _ <- res match
                 case Left(msg) => errorRef.set(Some(msg))
                 case Right(_)  => errorRef.set(None)
        yield ()

      div.cssClass("mp-admin-login-shell")(
        // Top utility bar (Storefront link + EN/CS)
        div.style(Style.row.width(500.px).maxWidth(100.pct).justify(Justification.spaceBetween).align(Alignment.center).margin(0.px, 0.px, 14.px, 0.px))(
          onBackToStorefront match
            case Some(goBack) =>
              button.cssClass("mp-btn mp-btn-secondary mp-btn-sm").onClick(goBack())(Icons(Icon.ArrowLeft, 15), span(state.i18n.t("adm-back-to-storefront")))
            case None => div(),
          div.style(rowSm)(
            Controls.languageSwitch(state.i18n, l => state.setLanguage(l)),
            Controls.themeToggle(state.theme, state.i18n, outlined = true),
          ),
        ),

        div.cssClass("mp-admin-login-card")(
          // Header
          div.style(Style.row.align(Alignment.center).gap(14.px))(
            div.cssClass("mp-logo-mark").style(Style.width(46.px).height(46.px).rounded(14.px))(Icons(Icon.Printer, 24)),
            div.style(Style.column.gap(2.px))(
              h1(state.i18n.t("adm-login-title")).style(Style.margin(0.px).fontSize(21.px).fontWeight(FontWeight.w800).letterSpacing((-0.3).px).color(ink)),
              span(state.i18n.t("brand-name")).style(smallText),
            ),
          ),

          p(state.i18n.t("adm-login-subtitle")).style(mutedText.margin(0.px)),

          // Error Banner
          errorRef.render {
            case Some(err) =>
              state.i18n.language.render { lang =>
                alertBanner(BadgeTone.Rose, span(if lang == Language.Cs then "Přihlášení se nezdařilo" else "Sign-in failed"), List(err))
              }: UI
            case None      => UI.empty: UI
          },

          // Credentials Form
          div.style(stackSm)(
            div.cssClass("mp-field")(
              label(state.i18n.t("adm-login-email")).style(labelStyle),
              emailInput
                .cssClass("mp-input")
                .value(emailRef)
                .onInput(v => emailRef.set(v)),
            ),
            div.cssClass("mp-field")(
              label(state.i18n.t("adm-login-password")).style(labelStyle),
              passwordInput
                .cssClass("mp-input")
                .value(passwordRef)
                .onInput(v => passwordRef.set(v)),
            ),
          ),

          state.busy.render { isBusy =>
            button
              .cssClass("mp-btn mp-btn-primary mp-btn-lg")
              .disabled(isBusy)
              .onClick(submitLogin)(
                if isBusy then span(state.i18n.t("adm-login-signing-in"))
                else UI.fragment(Icons(Icon.Lock, 16), span(state.i18n.t("adm-login-submit")))
              )
          },

          hr.style(dividerStyle),

          // Quick-fill Demo Staff
          div.style(stackSm)(
            div.style(Style.fontSize(13.px).fontWeight(FontWeight.w700).color(ink))(state.i18n.t("adm-login-demo-title")),
            div.style(smallText)(state.i18n.t("adm-login-demo-hint")),
            div.style(stackSm)(
              demoStaff.map { staff =>
                button
                  .cssClass("mp-option-card")
                  .style(Style.flexBasis(Length.Auto).width(100.pct).padding(10.px, 12.px))
                  .onClick(
                    emailRef.set(staff.email)
                      .andThen(passwordRef.set("staff1234"))
                      .andThen(errorRef.set(None))
                  )(
                    div.style(Style.row.align(Alignment.start).gap(12.px).width(100.pct))(
                      Controls.avatar(staff.label),
                      div.style(Style.column.gap(3.px).flexGrow(1.0).minWidth(0.px))(
                        div.style(rowBetween.width(100.pct).flexWrap(FlexWrap.wrap).gap(6.px))(
                          span(staff.label).style(Style.fontWeight(FontWeight.w700).fontSize(13.px).color(ink)),
                          div.style(Style.row.gap(4.px).flexWrap(FlexWrap.wrap))(
                            staff.roles.map(r => badge(r, staff.tone))*
                          ),
                        ),
                        span(staff.email).style(smallText),
                        span(state.i18n.t(staff.roleKey)).style(Style.fontSize(12.px).color(inkSecondary)),
                      ),
                    ),
                  )
              }*
            ),
          ),
        ),
      )
