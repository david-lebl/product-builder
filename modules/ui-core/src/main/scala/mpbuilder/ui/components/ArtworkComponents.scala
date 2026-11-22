package mpbuilder.ui.components

import kyo.*
import kyo.UI.*
import kyo.Style.*
import mpbuilder.artwork.*
import mpbuilder.shared.*
import mpbuilder.storefront.ArtworkUploadPayload
import mpbuilder.ui.components.CommonComponents.*
import mpbuilder.ui.theme.AppTheme.*
import mpbuilder.ui.theme.AppTheme.Palette.*
import scala.collection.mutable

/** Pure, browser-agnostic file descriptor produced either by the JS drag-and-drop / file-picker
  * bridge (in `Main.scala` / `AdminMain.scala`) or by demo presets & JVM tests.
  */
final case class BrowserPickedFile(
    fileName: String,
    mimeType: String,
    sizeBytes: Long,
    downloadUrl: String,
    previewUrl: Option[String] = None,
    pixelWidth: Option[Int] = None,
    pixelHeight: Option[Int] = None,
)

/** Global pure-Scala drop-zone registry allowing the thin browser JS listener to dispatch real
  * HTML5 drag-and-drop & native `<input type="file">` events into Kyo UI handlers without
  * importing `org.scalajs.dom` anywhere in `ui-core` or `ui-admin-core`.
  */
object FileDropRegistry:

  private val handlers = mutable.HashMap.empty[String, BrowserPickedFile => Unit < Async]

  def register(zoneId: String, handler: BrowserPickedFile => Unit < Async): Unit =
    handlers.synchronized {
      handlers.put(zoneId, handler)
      ()
    }

  def unregister(zoneId: String): Unit =
    handlers.synchronized {
      handlers.remove(zoneId)
      ()
    }

  def lookup(zoneId: String): Option[BrowserPickedFile => Unit < Async] =
    handlers.synchronized(handlers.get(zoneId))

  def dispatch(zoneId: String, file: BrowserPickedFile): Unit < Async =
    lookup(zoneId) match
      case Some(fn) => fn(file)
      case None     => Kyo.unit

/** Shared artwork utilities, Drag & Drop upload zone, compact file card, and Lightbox Preview Modal
  * used across Storefront Configurator, Customer Order History, Staff Order Detail, and Customer Detail.
  */
object ArtworkComponents:

  private val Base64Chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"

  /** Cross-platform (JVM + Scala.js) RFC 4648 Base64 encoder for UTF-8 strings. */
  private def base64EncodeUtf8(str: String): String =
    val bytes = str.getBytes(java.nio.charset.StandardCharsets.UTF_8)
    val sb    = new StringBuilder(((bytes.length + 2) / 3) * 4)
    var i     = 0
    while i < bytes.length do
      val b0 = bytes(i) & 0xff
      val b1 = if i + 1 < bytes.length then bytes(i + 1) & 0xff else 0
      val b2 = if i + 2 < bytes.length then bytes(i + 2) & 0xff else 0
      sb.append(Base64Chars.charAt(b0 >>> 2))
      sb.append(Base64Chars.charAt(((b0 & 0x03) << 4) | (b1 >>> 4)))
      sb.append(if i + 1 < bytes.length then Base64Chars.charAt(((b1 & 0x0f) << 2) | (b2 >>> 6)) else '=')
      sb.append(if i + 2 < bytes.length then Base64Chars.charAt(b2 & 0x3f) else '=')
      i += 3
    sb.toString

  def formatFileSize(bytes: Long): String =
    if bytes >= 1024L * 1024L then
      val mb = BigDecimal(bytes) / BigDecimal(1024 * 1024)
      s"${mb.setScale(1, BigDecimal.RoundingMode.HALF_UP)} MB"
    else if bytes >= 1024L then
      val kb = BigDecimal(bytes) / BigDecimal(1024)
      s"${kb.setScale(0, BigDecimal.RoundingMode.HALF_UP)} KB"
    else s"$bytes B"

  /** Builds a self-contained in-memory SVG print-proof data URL (with crop marks, bleed box,
    * trim box, and technical readout) so demo uploads & fallback previews render and download cleanly.
    */
  def generateProofSvgDataUrl(
      fileName: String,
      subtitle: String,
      accentHex: String,
      trimSize: Dimension2D,
      dpi: Int,
      colorSpace: ColorSpace,
      bleedMm: Int,
  ): String =
    val wMm      = trimSize.width.value.setScale(0, BigDecimal.RoundingMode.HALF_UP)
    val hMm      = trimSize.height.value.setScale(0, BigDecimal.RoundingMode.HALF_UP)
    val escTitle = fileName.take(42).replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    val escSub   = subtitle.take(54).replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    val svg =
      s"""<svg xmlns="http://www.w3.org/2000/svg" width="640" height="420" viewBox="0 0 640 420">
         |  <rect width="640" height="420" fill="#F1F5F9"/>
         |  <rect x="44" y="34" width="552" height="352" fill="#FFFFFF" stroke="#CBD5E1" stroke-width="1.5" stroke-dasharray="6 4"/>
         |  <rect x="64" y="54" width="512" height="312" fill="#FFFFFF" stroke="#0F172A" stroke-width="1.5"/>
         |  <line x1="64" y1="24" x2="64" y2="46" stroke="#0F172A" stroke-width="1"/>
         |  <line x1="34" y1="54" x2="56" y2="54" stroke="#0F172A" stroke-width="1"/>
         |  <line x1="576" y1="24" x2="576" y2="46" stroke="#0F172A" stroke-width="1"/>
         |  <line x1="584" y1="54" x2="606" y2="54" stroke="#0F172A" stroke-width="1"/>
         |  <line x1="64" y1="374" x2="64" y2="396" stroke="#0F172A" stroke-width="1"/>
         |  <line x1="34" y1="366" x2="56" y2="366" stroke="#0F172A" stroke-width="1"/>
         |  <line x1="576" y1="374" x2="576" y2="396" stroke="#0F172A" stroke-width="1"/>
         |  <line x1="584" y1="366" x2="606" y2="366" stroke="#0F172A" stroke-width="1"/>
         |  <rect x="64" y="54" width="512" height="72" fill="$accentHex"/>
         |  <text x="92" y="96" font-family="system-ui,-apple-system,sans-serif" font-size="20" font-weight="700" fill="#FFFFFF">$escTitle</text>
         |  <text x="92" y="158" font-family="system-ui,-apple-system,sans-serif" font-size="14" font-weight="600" fill="#1E293B">$escSub</text>
         |  <rect x="92" y="184" width="456" height="84" rx="8" fill="#F8FAFC" stroke="#E2E8F0"/>
         |  <text x="112" y="214" font-family="monospace" font-size="13" fill="#334155">TRIM: $wMm × $hMm mm   |   BLEED: ${bleedMm} mm</text>
         |  <text x="112" y="242" font-family="monospace" font-size="13" fill="#334155">RESOLUTION: $dpi DPI   |   COLOR: $colorSpace</text>
         |  <rect x="92" y="294" width="44" height="16" rx="3" fill="#00AEEF"/>
         |  <rect x="142" y="294" width="44" height="16" rx="3" fill="#EC008C"/>
         |  <rect x="192" y="294" width="44" height="16" rx="3" fill="#FFF200" stroke="#CBD5E1"/>
         |  <rect x="242" y="294" width="44" height="16" rx="3" fill="#111827"/>
         |  <text x="92" y="338" font-family="system-ui,-apple-system,sans-serif" font-size="11" fill="#64748B">PRINTCRAFT PROOF · SOLID = TRIM BOX · DASHED = BLEED</text>
         |</svg>""".stripMargin
    val b64 = base64EncodeUtf8(svg)
    s"data:image/svg+xml;base64,$b64"

  /** Converts a browser-dropped/picked file into an `ArtworkUploadPayload` with realistic
    * inferred print specs (calculating real DPI from pixel dimensions when available).
    */
  def toUploadPayload(
      file: BrowserPickedFile,
      expectedTrim: Dimension2D,
      expectedPages: Option[Int] = None,
  ): ArtworkUploadPayload =
    val lowerName = file.fileName.toLowerCase
    val isImage   = file.mimeType.startsWith("image/")
    val inferredDpi: Int = (file.pixelWidth, file.pixelHeight) match
      case (Some(pw), Some(ph)) if pw > 0 && ph > 0 =>
        val wInches = (expectedTrim.width.value.toDouble / 25.4).max(0.5)
        val hInches = (expectedTrim.height.value.toDouble / 25.4).max(0.5)
        val d1      = math.min(pw / wInches, ph / hInches)
        val d2      = math.min(ph / wInches, pw / hInches)
        math.max(72, math.min(600, math.max(d1, d2).round.toInt))
      case _ if lowerName.contains("lowres") || lowerName.contains("draft") || lowerName.contains("web") =>
        150
      case _ =>
        300
    val inferredColorSpace: ColorSpace =
      if lowerName.contains("cmyk") || lowerName.contains("print") || lowerName.contains("press") then ColorSpace.CMYK
      else if isImage || lowerName.contains("rgb") || lowerName.contains("web") then ColorSpace.RGB
      else ColorSpace.CMYK
    val inferredBleed: Int =
      if lowerName.contains("nobleed") || lowerName.contains("draft") then 0 else 3
    val proofFallback = generateProofSvgDataUrl(
      fileName = file.fileName,
      subtitle = s"${formatFileSize(file.sizeBytes)} · ${file.mimeType}",
      accentHex = if inferredColorSpace == ColorSpace.CMYK then "#059669" else "#4F46E5",
      trimSize = expectedTrim,
      dpi = inferredDpi,
      colorSpace = inferredColorSpace,
      bleedMm = inferredBleed,
    )
    val effectiveDownload = NonEmptyString(file.downloadUrl).toOption.orElse(Some(NonEmptyString.unsafe(proofFallback)))
    val effectivePreview  = file.previewUrl.flatMap(NonEmptyString(_).toOption).orElse(effectiveDownload)

    ArtworkUploadPayload(
      fileName = NonEmptyString.unsafe(if file.fileName.trim.nonEmpty then file.fileName.trim else "artwork.pdf"),
      mimeType = NonEmptyString.unsafe(if file.mimeType.trim.nonEmpty then file.mimeType.trim else "application/pdf"),
      sizeBytes = file.sizeBytes.max(1L),
      downloadUrl = effectiveDownload,
      previewUrl = effectivePreview,
      resolutionDpi = inferredDpi,
      bleedMm = inferredBleed,
      colorSpace = inferredColorSpace,
      pageCount = expectedPages,
      fontsEmbedded = true,
      hasUnflattenedTransparency = lowerName.contains("draft"),
    )

  /** Builds a press-ready sample `BrowserPickedFile` for one-click demo testing. */
  def demoPrintReadyFile(componentKey: ComponentKey, trimSize: Dimension2D): BrowserPickedFile =
    val name = s"${componentKey.value.toLowerCase}-press-ready-cmyk.pdf"
    val proof = generateProofSvgDataUrl(
      fileName = name,
      subtitle = "Press-Ready CMYK PDF/X-4 · 300 DPI · 3mm bleed",
      accentHex = "#059669",
      trimSize = trimSize,
      dpi = 300,
      colorSpace = ColorSpace.CMYK,
      bleedMm = 3,
    )
    BrowserPickedFile(
      fileName = name,
      mimeType = "application/pdf",
      sizeBytes = 6_450_000L,
      downloadUrl = proof,
      previewUrl = Some(proof),
    )

  /** Builds a low-DPI RGB draft sample `BrowserPickedFile` to demonstrate preflight warnings/failures. */
  def demoDraftRgbFile(componentKey: ComponentKey, trimSize: Dimension2D): BrowserPickedFile =
    val name = s"${componentKey.value.toLowerCase}-web-draft-rgb.png"
    val proof = generateProofSvgDataUrl(
      fileName = name,
      subtitle = "Web Draft RGB · 150 DPI · 0mm bleed (Preflight Check)",
      accentHex = "#D97706",
      trimSize = trimSize,
      dpi = 150,
      colorSpace = ColorSpace.RGB,
      bleedMm = 0,
    )
    BrowserPickedFile(
      fileName = name,
      mimeType = "image/png",
      sizeBytes = 1_280_000L,
      downloadUrl = proof,
      previewUrl = Some(proof),
    )

  /** Extracts typed `UI.ImgSrc` from a URL if it can be rendered as an `<img>` element. */
  def imageSrcFor(url: String, mimeType: String): Option[UI.ImgSrc] =
    if url.startsWith("data:") && url.contains(";base64,") then
      val idx      = url.indexOf(";base64,")
      val mimePart = url.substring(5, idx)
      val b64Part  = url.substring(idx + 8)
      if mimePart.startsWith("image/") && b64Part.nonEmpty then Some(UI.ImgSrc.Data(mimePart, b64Part))
      else None
    else if (url.startsWith("blob:") || url.startsWith("http://") || url.startsWith("https://")) && mimeType.startsWith("image/") then
      Some(UI.ImgSrc.Path(url))
    else None

  /** Renders a crisp thumbnail (real `<img>` for images/SVG proofs, or styled PDF/file badge). */
  def renderThumbnail(rev: ArtworkRevision, sizePx: Int = 56, onClick: Option[() => Unit < Async] = None): UI =
    val previewCandidate = rev.file.previewUrl.orElse(rev.file.downloadUrl).map(_.value)
    val imgOpt = previewCandidate.flatMap(u => imageSrcFor(u, rev.file.mimeType.value))
    val inner: UI = imgOpt match
      case Some(src) =>
        img(src, rev.file.fileName.value).style(
          Style
            .width(100.pct)
            .height(100.pct)
            .display(Display.block),
        )
      case None =>
        val ext =
          val n = rev.file.fileName.value
          val dot = n.lastIndexOf('.')
          if dot >= 0 && dot < n.length - 1 then n.substring(dot + 1).toUpperCase.take(4) else "FILE"
        div.style(
          Style.column
            .align(Alignment.center)
            .justify(Justification.center)
            .width(100.pct)
            .height(100.pct)
            .bg(brandSoft)
            .color(brand)
            .fontWeight(FontWeight.w800)
            .fontSize(11.px),
        )(ext)

    val thumbStyle =
      val base = Style
        .width(sizePx.px)
        .height(sizePx.px)
        .minWidth(sizePx.px)
        .rounded(8.px)
        .overflow(Overflow.hidden)
        .border(1.px, Palette.border)
        .bg(surface)
        .flexShrink(0.0)
      if onClick.isDefined then base.cursor(Cursor.pointer) else base

    val box = div
      .cssClass("mp-art-thumb")
      .style(thumbStyle)(inner)

    onClick match
      case Some(cb) => box.onClick(cb())
      case None     => box

  /** Renders Download & Copy Link action buttons backed by delegated browser listeners in Main/AdminMain. */
  def downloadButton(
      rev: ArtworkRevision,
      lang: Language,
      onDownloadFeedback: Option[() => Unit < Async] = None,
      compact: Boolean = true,
  ): UI =
    val isCs = lang == Language.Cs
    val url  = rev.file.downloadUrl.orElse(rev.file.previewUrl).map(_.value).getOrElse(rev.file.shareableUrl)
    val btn = button
      .cssClass(if compact then "mp-btn mp-btn-secondary mp-btn-sm" else "mp-btn mp-btn-secondary")
      .data("mp-download-url", url)
      .data("mp-download-name", rev.file.fileName.value)
    val label = if isCs then "⬇ Stáhnout" else "⬇ Download"
    onDownloadFeedback match
      case Some(cb) => btn.onClick(cb())(label)
      case None     => btn(label)

  def copyLinkButton(
      rev: ArtworkRevision,
      lang: Language,
      onCopied: () => Unit < Async,
      compact: Boolean = true,
  ): UI =
    val isCs = lang == Language.Cs
    val url  = rev.file.shareableUrl
    button
      .cssClass(if compact then "mp-btn mp-btn-ghost mp-btn-sm" else "mp-btn mp-btn-ghost")
      .data("mp-copy-url", url)
      .onClick(onCopied())(if isCs then "🔗 Kopírovat odkaz" else "🔗 Copy Link")

  /** Interactive Drag & Drop + Click-to-Browse Upload Zone (with optional quick demo buttons). */
  def dropZone(
      zoneId: String,
      expectedTrim: Dimension2D,
      componentKey: ComponentKey,
      lang: Language,
      onFilePicked: BrowserPickedFile => Unit < Async,
      titleOpt: Option[String] = None,
      compact: Boolean = false,
      showDemoQuickAttach: Boolean = true,
      disabled: Boolean = false,
  ): UI =
    val isCs = lang == Language.Cs
    if !disabled then FileDropRegistry.register(zoneId, onFilePicked)
    else FileDropRegistry.unregister(zoneId)

    val title = titleOpt.getOrElse(
      if isCs then "Přetáhněte tisková data sem nebo klikněte pro výběr"
      else "Drag & drop print artwork here, or click to browse"
    )
    val hint =
      if isCs then "PDF, PNG, JPG, SVG, TIFF, AI, EPS · min. 300 DPI, spadávka 3 mm, CMYK"
      else "PDF, PNG, JPG, SVG, TIFF, AI, EPS · 300 DPI, 3 mm bleed, CMYK recommended"

    div.style(stackXs.width(100.pct))(
      div
        .cssClass(if compact then "mp-dropzone mp-dropzone-compact" else "mp-dropzone")
        .data("mp-dropzone", zoneId)
        .data("mp-dropzone-disabled", disabled.toString)(
          div.style(Style.row.align(Alignment.center).gap(10.px).justify(Justification.center).flexWrap(FlexWrap.wrap))(
            span("⬆").style(
              Style
                .display(Display.inlineFlex)
                .align(Alignment.center)
                .justify(Justification.center)
                .width(if compact then 28.px else 36.px)
                .height(if compact then 28.px else 36.px)
                .rounded(999.px)
                .bg(brandSoft)
                .color(brand)
                .fontWeight(FontWeight.w800)
                .fontSize(if compact then 14.px else 16.px),
            ),
            div.style(Style.column.gap(2.px).textAlign(TextAlign.left))(
              span(title).style(Style.fontSize(if compact then 12.px else 13.px).fontWeight(FontWeight.w700).color(ink)),
              span(hint).style(Style.fontSize(11.px).color(inkMuted)),
            ),
          ),
        ),
      if showDemoQuickAttach && !disabled then
        div.style(Style.row.flexWrap(FlexWrap.wrap).gap(6.px).align(Alignment.center))(
          span(if isCs then "Ukázkový soubor:" else "Quick demo file:").style(Style.fontSize(11.px).color(inkMuted)),
          button
            .cssClass("mp-pill")
            .style(Style.padding(3.px, 9.px).fontSize(11.px))
            .onClick(onFilePicked(demoPrintReadyFile(componentKey, expectedTrim)))(
              if isCs then "✓ Tiskové PDF (CMYK 300 DPI)" else "✓ Press-Ready PDF (CMYK 300 DPI)",
            ),
          button
            .cssClass("mp-pill")
            .style(Style.padding(3.px, 9.px).fontSize(11.px))
            .onClick(onFilePicked(demoDraftRgbFile(componentKey, expectedTrim)))(
              if isCs then "⚠ Náhled RGB (150 DPI)" else "⚠ RGB Draft (150 DPI)",
            ),
        )
      else UI.empty,
    )

  // ── Lightbox Artwork Preview Modal ─────────────────────────────────────────

  object PreviewModal:

    final case class ModalState(
        artwork: ArtworkSubmissionView,
        selectedRevisionNumber: Int,
        contextTitle: Option[String] = None,
    )

    final class Handle(val stateRef: SignalRef[Option[ModalState]]):

      def open(
          artwork: ArtworkSubmissionView,
          revisionNumber: Option[Int] = None,
          contextTitle: Option[String] = None,
      ): Unit < Sync =
        stateRef.set(Some(ModalState(artwork, revisionNumber.getOrElse(artwork.latestRevision.revisionNumber), contextTitle)))

      def selectRevision(revNum: Int): Unit < Sync =
        stateRef.updateAndGet(_.map(_.copy(selectedRevisionNumber = revNum))).unit

      def close: Unit < Sync =
        stateRef.set(None)

      def render(
          langSignal: Signal[Language],
          onCopiedLink: String => Unit < Async,
      ): UI =
        stateRef.combineLatest(langSignal).render {
          case (None, _) => UI.empty
          case (Some(st), lang) =>
            val isCs = lang == Language.Cs
            val art  = st.artwork
            val revs = art.allRevisions
            val rev  = revs.find(_.revisionNumber == st.selectedRevisionNumber).getOrElse(art.latestRevision)

            val previewUrlOpt = rev.file.previewUrl.orElse(rev.file.downloadUrl).map(_.value)
            val previewVisual: UI = previewUrlOpt match
              case Some(url) =>
                imageSrcFor(url, rev.file.mimeType.value) match
                  case Some(imgSrc) =>
                    img(imgSrc, rev.file.fileName.value).style(
                      Style
                        .maxWidth(100.pct)
                        .maxHeight(440.px)
                        .rounded(8.px)
                        .border(1.px, Palette.border)
                        .bg(Color.white),
                    )
                  case None if rev.file.mimeType.value.toLowerCase.contains("pdf") && (url.startsWith("blob:") || url.startsWith("http")) =>
                    iframe(url).style(
                      Style
                        .width(100.pct)
                        .height(440.px)
                        .rounded(8.px)
                        .border(1.px, Palette.border)
                        .bg(Color.white),
                    )
                  case _ =>
                    val fallbackUrl = generateProofSvgDataUrl(
                      fileName = rev.file.fileName.value,
                      subtitle = s"${formatFileSize(rev.file.sizeBytes)} · ${rev.file.mimeType.value}",
                      accentHex = "#4F46E5",
                      trimSize = rev.extractedSpecs.trimSize,
                      dpi = rev.extractedSpecs.resolutionDpi,
                      colorSpace = rev.extractedSpecs.colorSpace,
                      bleedMm = rev.extractedSpecs.bleed.value.toInt,
                    )
                    imageSrcFor(fallbackUrl, "image/svg+xml")
                      .map(src => img(src, rev.file.fileName.value).style(Style.maxWidth(100.pct).maxHeight(420.px).rounded(8.px)))
                      .getOrElse(UI.empty)
              case None =>
                val fallbackUrl = generateProofSvgDataUrl(
                  fileName = rev.file.fileName.value,
                  subtitle = s"${formatFileSize(rev.file.sizeBytes)} · ${rev.file.mimeType.value}",
                  accentHex = "#4F46E5",
                  trimSize = rev.extractedSpecs.trimSize,
                  dpi = rev.extractedSpecs.resolutionDpi,
                  colorSpace = rev.extractedSpecs.colorSpace,
                  bleedMm = rev.extractedSpecs.bleed.value.toInt,
                )
                imageSrcFor(fallbackUrl, "image/svg+xml")
                  .map(src => img(src, rev.file.fileName.value).style(Style.maxWidth(100.pct).maxHeight(420.px).rounded(8.px)))
                  .getOrElse(UI.empty)

            div.cssClass("mp-backdrop").onClickSelf(close)(
              div.cssClass("mp-modal mp-art-preview-modal")(
                // Header
                div.style(rowBetween.flexWrap(FlexWrap.wrap))(
                  div.style(stackXs.minWidth(0.px))(
                    div.style(rowSm.flexWrap(FlexWrap.wrap))(
                      h2(rev.file.fileName.value).style(Style.margin(0.px).fontSize(17.px).fontWeight(FontWeight.w800).color(ink)),
                      artworkStatusBadge(art.status, lang),
                      badge(s"rev ${rev.revisionNumber}/${art.revisionCount}", BadgeTone.Neutral),
                    ),
                    span(
                      st.contextTitle.map(_ + " · ").getOrElse("") +
                        s"${art.componentKey.value} · ${formatFileSize(rev.file.sizeBytes)} · ${formatInstant(rev.uploadedAt)}",
                    ).style(smallText),
                  ),
                  button.cssClass("mp-btn mp-btn-ghost mp-btn-sm").onClick(close)("✕"),
                ),

                // Revision selector bar (when multiple revisions exist)
                if revs.size > 1 then
                  div.style(rowWrap)(
                    span(if isCs then "Revize:" else "Revisions:").style(smallText.fontWeight(FontWeight.w700)),
                    UI.fragment(
                      revs.reverse.map { r =>
                        val active = r.revisionNumber == rev.revisionNumber
                        button
                          .cssClass(if active then "mp-pill-active" else "mp-pill")
                          .style(Style.padding(3.px, 10.px).fontSize(12.px))
                          .onClick(selectRevision(r.revisionNumber))(
                            s"v${r.revisionNumber} · ${r.file.fileName.value}",
                          )
                      }*
                    ),
                  )
                else UI.empty,

                // Main Visual Preview Frame
                div.cssClass("mp-art-preview-frame")(previewVisual),

                // Technical Specifications & Preflight Strip
                div.style(Style.column.gap(8.px).padding(12.px).rounded(10.px).bg(surfaceAlt).border(1.px, Palette.border))(
                  div.style(rowWrap)(
                    badge(s"${rev.extractedSpecs.resolutionDpi} DPI", BadgeTone.Neutral),
                    badge(formatDimension(rev.extractedSpecs.trimSize), BadgeTone.Neutral),
                    badge(s"${if isCs then "Spadávka" else "Bleed"} ${rev.extractedSpecs.bleed.value} mm", BadgeTone.Neutral),
                    badge(rev.extractedSpecs.colorSpace.toString, BadgeTone.Brand),
                    badge(s"${rev.extractedSpecs.pageCount} ${if isCs then "str." else "p."}", BadgeTone.Neutral),
                    badge(rev.file.mimeType.value, BadgeTone.Neutral),
                  ),
                  rev.preflight match
                    case Some(rep) =>
                      div.style(stackXs)(
                        rep.checks.map { ch =>
                          val (tone, detail) = ch.outcome match
                            case CheckOutcome.Passed       => (BadgeTone.Emerald, "OK")
                            case CheckOutcome.Warning(msg) => (BadgeTone.Amber, msg)
                            case CheckOutcome.Failed(msg)  => (BadgeTone.Rose, msg)
                          div.style(rowSm.flexWrap(FlexWrap.wrap))(
                            badge(ch.kind.toString, tone),
                            span(detail).style(Style.fontSize(12.px).color(inkSecondary)),
                          )
                        }*
                      )
                    case None => UI.empty,
                  div.style(Style.row.align(Alignment.center).gap(8.px).flexWrap(FlexWrap.wrap).padding(6.px, 0.px, 0.px, 0.px).borderTop(1.px, Palette.border))(
                    span(if isCs then "URL souboru:" else "Asset URL:").style(smallText.fontWeight(FontWeight.w700)),
                    code(rev.file.shareableUrl).style(Style.fontSize(11.px).color(inkSecondary).fontFamily(FontFamily.Monospace)),
                  ),
                ),

                // Footer Actions
                div.style(rowBetween.flexWrap(FlexWrap.wrap))(
                  div.style(rowSm.flexWrap(FlexWrap.wrap))(
                    downloadButton(rev, lang, compact = false),
                    copyLinkButton(rev, lang, onCopied = () => onCopiedLink(rev.file.shareableUrl), compact = false),
                  ),
                  button.cssClass("mp-btn mp-btn-secondary").onClick(close)(if isCs then "Zavřít" else "Close"),
                ),
              ),
            )
        }

    object Handle:
      def make: Handle < Sync =
        Signal.initRef[Option[ModalState]](None).map(new Handle(_))
