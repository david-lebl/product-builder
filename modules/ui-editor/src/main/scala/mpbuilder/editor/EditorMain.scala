package mpbuilder.editor

import kyo.*
import mpbuilder.design.DesignProductKind
import mpbuilder.design.ProductBinding
import mpbuilder.editor.browser.{BrowserEditor, IndexedDbStores}
import mpbuilder.editor.i18n.EditorTranslations
import mpbuilder.shared.{DesignId, Language}
import mpbuilder.ui.theme.AppTheme
import org.scalajs.dom

/** Standalone Design Studio page (`_site/editor/`): "My designs" + the editor, everything stored on this device.
  *
  * Deep links: `#/design/<designId>` opens a stored design, `#/new/<kind>` (e.g. `#/new/wallcalendar`) opens the
  * template gallery of one product kind.
  */
object EditorMain extends KyoApp:

  import kyo.AllowUnsafe.embrace.danger

  private def detectLanguage: Language =
    val saved = try Option(dom.window.localStorage.getItem("mpbuilder.lang")) catch case _: Throwable => None
    val raw   = saved.orElse(Option(dom.window.navigator.language)).getOrElse("en").toLowerCase
    if raw.startsWith("cs") then Language.Cs else Language.En

  private def launchFromHash: EditorLaunch =
    dom.window.location.hash.stripPrefix("#").split('/').toList.filter(_.nonEmpty) match
      case "design" :: id :: _ => EditorLaunch(designId = Some(DesignId.unsafe(id)))
      case "new" :: kind :: _ =>
        DesignProductKind.values.find(_.toString.equalsIgnoreCase(kind)) match
          case Some(k) => EditorLaunch(product = Some(ProductBinding(k)))
          case None    => EditorLaunch()
      case _ => EditorLaunch()

  private def installPage(themeOverride: Option[String]): Unit =
    val theme = themeOverride.getOrElse(
      try Option(dom.window.localStorage.getItem(AppTheme.ThemeStorageKey)).getOrElse("system") catch case _: Throwable => "system"
    )
    dom.document.documentElement.setAttribute("data-theme", theme)
    val style = dom.document.createElement("style")
    style.textContent = AppTheme.documentCss
    val _ = dom.document.head.appendChild(style)
    BrowserEditor.installStyles()
    BrowserEditor.install()

  run {
    val embed = EmbedProtocol.parse(launchFromHash)
    val lang  = embed.language.getOrElse(detectLanguage)
    for
      _    <- Sync.defer(installPage(embed.theme))
      _    <- UI.runStylesheet(AppTheme.storefrontStylesheet)
      i18n <- EditorTranslations.init(lang)
      store = if IndexedDbStores.available then IndexedDbStores.designStore else null
      mem  <- InMemoryDesignStore.make
      host  = EditorHost(
                deviceId = BrowserEditor.deviceId,
                store = Option(store).getOrElse(mem),
                assets = IndexedDbStores.assetResolver,
                onClose = Option.when(embed.embedded)(() => Sync.defer(EmbedProtocol.postClose())),
                onComplete = Option.when(embed.embedded)((r: EditorResult) => Sync.defer(EmbedProtocol.postComplete(r))),
                completeLabelKey = "ve-complete-cart",
                customerId = embed.customerId,
              )
      _ <- I18n.let(i18n.handle) {
             for
               ctrl <- EditorController.init(i18n, host, embed.launch)
               _ <- Sync.defer(EditorBridge.attach(ctrl, eff => { val _ = Sync.Unsafe.evalOrThrow(Fiber.initUnscoped(I18n.let(i18n.handle)(eff))) }))
               _ <- UI.runMount(EditorApp.render(ctrl, embedded = true), "#app-root")
               _ <- Async.never
             yield ()
           }
    yield ()
  }
