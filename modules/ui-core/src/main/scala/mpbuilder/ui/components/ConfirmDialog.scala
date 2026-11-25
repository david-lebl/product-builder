package mpbuilder.ui.components

import kyo.*
import kyo.UI.*
import kyo.Style.*
import mpbuilder.ui.theme.AppTheme.*
import mpbuilder.ui.theme.AppTheme.Palette.*

/** Reusable confirmation modal dialog supporting optional or required reason text. */
object ConfirmDialog:

  enum DialogTone:
    case Primary, Amber, Danger

  final case class Config(
      title: String,
      message: String,
      confirmLabel: String,
      cancelLabel: String,
      tone: DialogTone = DialogTone.Primary,
      reasonLabel: Option[String] = None,
      reasonPlaceholder: Option[String] = None,
      reasonRequired: Boolean = false,
      reasonErrorText: String = "Please provide a reason",
      onConfirm: Option[String] => Unit < Async,
  )

  /** Stateful dialog handle that can be opened from any action bar button. */
  final class Handle(
      val config: SignalRef[Option[Config]],
      val reason: SignalRef[String],
      val error: SignalRef[Option[String]],
  ):
    def open(cfg: Config): Unit < Async =
      for
        _ <- reason.set("")
        _ <- error.set(None)
        _ <- config.set(Some(cfg))
      yield ()

    def close: Unit < Async =
      for
        _ <- config.set(None)
        _ <- reason.set("")
        _ <- error.set(None)
      yield ()

    def render: UI =
      config
        .combineLatest(reason)
        .combineLatest(error)
        .render { case ((cfgOpt, reasonVal), errOpt) =>
          cfgOpt match
            case None => UI.empty: UI
            case Some(cfg) =>
              val confirmClass = cfg.tone match
                case DialogTone.Danger  => "mp-btn mp-btn-danger"
                case DialogTone.Amber   => "mp-btn mp-btn-secondary"
                case DialogTone.Primary => "mp-btn mp-btn-primary"

              val confirmStyle = cfg.tone match
                case DialogTone.Amber =>
                  Style.bg(amberSoft).color(amberInk).border(1.px, amberBorder)
                case _ => Style.empty

              div.cssClass("mp-modal-backdrop")(
                div.cssClass("mp-modal")(
                  div.style(rowBetween)(
                    h3(cfg.title).style(Style.margin(0.px).fontSize(18.px).fontWeight(FontWeight.w800).color(ink)),
                    button.cssClass("mp-icon-btn mp-icon-btn-sm").aria("label", "Close").onClick(close)(Icons(Icons.Icon.Close, 16)),
                  ),
                  p(cfg.message).style(Style.margin(0.px).fontSize(14.px).color(inkSecondary).lineHeight(1.5)),
                  cfg.reasonLabel match
                    case Some(lbl) =>
                      div.style(stackXs)(
                        label(lbl).style(labelStyle),
                        textarea
                          .cssClass("mp-input")
                          .style(Style.minHeight(84.px))
                          .value(reasonVal)
                          .placeholder(cfg.reasonPlaceholder.getOrElse(""))
                          .onInput(v => reason.set(v).andThen(error.set(None))),
                        errOpt match
                          case Some(e) =>
                            span(e).style(Style.fontSize(12.px).fontWeight(FontWeight.w600).color(roseInk))
                          case None => UI.empty,
                      )
                    case None => UI.empty,
                  div.style(Style.row.justify(Justification.end).gap(10.px).padding(8.px, 0.px, 0.px, 0.px))(
                    button.cssClass("mp-btn mp-btn-secondary").onClick(close)(cfg.cancelLabel),
                    button
                      .cssClass(confirmClass)
                      .style(confirmStyle)
                      .onClick {
                        val trimmed = reasonVal.trim
                        if cfg.reasonLabel.isDefined && cfg.reasonRequired && trimmed.isEmpty then
                          error.set(Some(cfg.reasonErrorText))
                        else
                          val arg = Option.when(trimmed.nonEmpty)(trimmed)
                          for
                            _ <- close
                            _ <- cfg.onConfirm(arg)
                          yield ()
                      }(cfg.confirmLabel),
                  ),
                ),
              )
        }

  object Handle:
    def make: Handle < Sync =
      for
        c <- Signal.initRef[Option[Config]](None)
        r <- Signal.initRef("")
        e <- Signal.initRef[Option[String]](None)
      yield new Handle(c, r, e)
