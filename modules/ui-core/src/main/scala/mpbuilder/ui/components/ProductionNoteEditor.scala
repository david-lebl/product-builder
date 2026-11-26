package mpbuilder.ui.components

import kyo.*
import kyo.UI.*
import kyo.Style.*
import mpbuilder.ui.i18n.AppI18n
import mpbuilder.ui.theme.AppTheme.*
import mpbuilder.ui.theme.AppTheme.Palette.*

/** Shows a line's customer production instructions and, while editable, lets the customer add / change /
  * clear them inline. One [[Handle]] serves a whole view: at most one line (identified by a string key) is
  * in edit mode at a time, and the text lives in its own ref so typing never re-renders the surrounding list.
  */
object ProductionNoteEditor:

  final class Handle(val editingKey: SignalRef[Option[String]], val text: SignalRef[String]):

    def start(key: String, current: Option[String]): Unit < Sync =
      text.set(current.getOrElse("")).andThen(editingKey.set(Some(key)))

    def cancel: Unit < Sync = editingKey.set(None)

    /** @param onSave receives the trimmed note (`None` when blank) and returns whether it was saved. */
    def render(
        key: String,
        current: Option[String],
        editable: Boolean,
        i18n: AppI18n,
        onSave: Option[String] => Boolean < Async,
    ): UI =
      editingKey.render { editing =>
        if editable && editing.contains(key) then editor(i18n, onSave)
        else display(key, current, editable, i18n)
      }

    private def editor(i18n: AppI18n, onSave: Option[String] => Boolean < Async): UI =
      div.cssClass("mp-production-note-editor").style(stackXs.width(100.pct))(
        label.style(labelStyle)(i18n.t("note-production-label")),
        i18n.t("cfg-production-note-placeholder").render { ph =>
          textarea.cssClass("mp-input").style(Style.minHeight(64.px)).placeholder(ph).value(text)
        },
        div.style(rowSm.flexWrap(FlexWrap.wrap))(
          button
            .cssClass("mp-btn mp-btn-primary mp-btn-sm")
            .onClick {
              for
                raw <- text.get
                ok  <- onSave(Option(raw.trim).filter(_.nonEmpty))
                _   <- if ok then cancel else Kyo.unit
              yield ()
            }(i18n.t("note-production-save")),
          button.cssClass("mp-btn mp-btn-ghost mp-btn-sm").onClick(cancel)(i18n.t("note-production-cancel")),
        ),
      )

    private def display(key: String, current: Option[String], editable: Boolean, i18n: AppI18n): UI =
      current match
        case Some(note) =>
          div
            .cssClass("mp-production-note")
            .style(stackXs.padding(8.px, 10.px).rounded(8.px).bg(surface).border(1.px, Palette.border))(
              div.style(rowBetween)(
                span(i18n.t("note-production-label")).style(smallText.fontWeight(FontWeight.w700)),
                if editable then
                  button
                    .cssClass("mp-btn mp-btn-ghost mp-btn-sm")
                    .style(Style.padding(2.px, 8.px))
                    .onClick(start(key, current))(i18n.t("note-production-edit"))
                else UI.empty,
              ),
              span(note).style(Style.fontSize(13.px).color(inkSecondary).whiteSpace(WhiteSpace.preWrap)),
            )
        case None if editable =>
          button
            .cssClass("mp-btn mp-btn-ghost mp-btn-sm mp-production-note-add")
            .style(Style.alignSelf(Alignment.start))
            .onClick(start(key, None))(i18n.t("note-production-add"))
        case None => UI.empty

  object Handle:
    def make: Handle < Sync =
      for
        keyRef  <- Signal.initRef[Option[String]](None)
        textRef <- Signal.initRef("")
      yield new Handle(keyRef, textRef)
