package mpbuilder.editor

import mpbuilder.design.*
import mpbuilder.shared.*
import org.scalajs.dom

import scala.scalajs.js

/** Contract between a host page (the storefront) and the Design Studio opened in an `<iframe>`.
  *
  * **Host → editor**: query parameters of `editor/index.html` (version `v=1`):
  *
  * | param       | meaning                                                         |
  * |-------------|-----------------------------------------------------------------|
  * | `embed=1`   | embedded mode: "Back to shop" closes, "Use this design" returns |
  * | `lang`      | `en` / `cs`                                                     |
  * | `theme`     | `system` / `light` / `dark`                                     |
  * | `category`  | storefront `CategoryId` (→ product kind, e.g. calendars)        |
  * | `component` | `ComponentKey` of the printable part                            |
  * | `title`     | human label ("Business Cards · Main")                           |
  * | `w`, `h`    | trim size in mm                                                 |
  * | `pages`     | configured page count (bound products)                          |
  * | `design`    | existing `DesignId` to reopen ("Edit design")                   |
  * | `customer`  | signed-in `CustomerId` (designs owned by the account)           |
  *
  * **Editor → host** (`window.parent.postMessage`, same origin only):
  *   - `{ type: "mpbuilder.editor.close", v: 1 }`
  *   - `{ type: "mpbuilder.editor.complete", v: 1, design: { id, name, proofDataUrl, minimumDpi, bleedMm, pageCount,
  *     trimWidthMm, trimHeightMm, issues } }`
  *
  * Designs and photos are stored in the browser's IndexedDB of the shared origin, so the host only receives ids
  * and a proof; the server-side PDF pipeline will fetch the full design by id once designs sync to the backend.
  */
object EmbedProtocol:

  val Version = 1

  final case class EmbedParams(
      embedded: Boolean,
      language: Option[Language],
      theme: Option[String],
      launch: EditorLaunch,
      customerId: Option[CustomerId],
  )

  private def params: Map[String, String] =
    val search = dom.window.location.search.stripPrefix("?")
    search.split('&').toList.filter(_.contains('=')).map { kv =>
      val idx = kv.indexOf('=')
      js.URIUtils.decodeURIComponent(kv.take(idx)) -> js.URIUtils.decodeURIComponent(kv.drop(idx + 1).replace('+', ' '))
    }.toMap

  def parse(hashLaunch: EditorLaunch): EmbedParams =
    val p        = params
    val category = p.get("category").filter(_.nonEmpty)
    val pages    = p.get("pages").flatMap(_.toIntOption).filter(_ > 0)
    val trim     = for
      w <- p.get("w").flatMap(_.toDoubleOption).filter(_ > 0)
      h <- p.get("h").flatMap(_.toDoubleOption).filter(_ > 0)
    yield (w, h)
    val launch = category match
      case None => hashLaunch.copy(designId = p.get("design").map(DesignId.unsafe).orElse(hashLaunch.designId))
      case Some(cat) =>
        val kind   = DesignPresets.kindForCategory(cat, pages)
        val format = trim.map((w, h) => DesignPresets.format(kind).copy(trimWidthMm = w, trimHeightMm = h))
        EditorLaunch(
          product = Some(ProductBinding(kind, Some(CategoryId.unsafe(cat)), p.get("component").map(ComponentKey.unsafe), p.get("title"))),
          format = format,
          // Calendars & cards keep their template page structure; bound products follow the configuration.
          pageCount = if kind == DesignProductKind.PhotoBook || kind == DesignProductKind.Custom then pages else None,
          designId = p.get("design").filter(_.nonEmpty).map(DesignId.unsafe),
          title = p.get("title"),
        )
    EmbedParams(
      embedded = p.get("embed").contains("1") && dom.window.parent != dom.window,
      language = p.get("lang").flatMap(l => Language.fromCode(l).toOption),
      theme = p.get("theme").filter(t => t == "light" || t == "dark" || t == "system"),
      launch = launch,
      customerId = p.get("customer").filter(_.nonEmpty).map(CustomerId.unsafe),
    )

  private def post(message: js.Object): Unit =
    dom.window.parent.postMessage(message, dom.window.location.origin)

  def postClose(): Unit =
    post(js.Dynamic.literal(`type` = "mpbuilder.editor.close", v = Version))

  def postComplete(result: EditorResult): Unit =
    val doc = result.project.document
    post(
      js.Dynamic.literal(
        `type` = "mpbuilder.editor.complete",
        v = Version,
        design = js.Dynamic.literal(
          id = result.project.id.value,
          name = result.project.name,
          proofDataUrl = result.proofDataUrl,
          minimumDpi = result.minimumDpi.fold[js.Any](null)(d => d),
          bleedMm = doc.format.bleedMm,
          pageCount = doc.pages.size,
          trimWidthMm = doc.format.trimWidthMm,
          trimHeightMm = doc.format.trimHeightMm,
          issues = result.issues.size,
        ),
      )
    )
