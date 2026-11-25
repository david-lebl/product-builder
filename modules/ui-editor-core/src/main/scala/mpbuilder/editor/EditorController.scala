package mpbuilder.editor

import kyo.*
import mpbuilder.design.*
import mpbuilder.shared.*
import mpbuilder.ui.i18n.AppI18n

/** Undo/redo stacks of immutable document snapshots (cheap thanks to structural sharing). */
final case class History(past: List[DesignDocument], future: List[DesignDocument], lastKey: Option[String], lastAt: Long)

private final case class ActiveGesture(kind: GestureKind, origin: Point, starts: List[(ElementId, Placement)], snapLines: (List[Double], List[Double]))

private enum GestureKind:
  case Move
  case Resize(handle: Handle, keepAspectDefault: Boolean)
  case Rotate

/** Session metadata of the design being edited (the document itself lives in `doc`). */
final case class EditingSession(
    id: DesignId,
    name: String,
    owner: DesignOwner,
    createdAt: java.time.Instant,
    revision: Long,
    syncedRevision: Option[Long],
) derives CanEqual

/** All Visual Editor state and behaviour. Pure Kyo (no DOM): views render from its signals and the browser
  * bridge feeds pointer / keyboard input through [[pointerDown]] / [[pointerMove]] / [[pointerUp]] / [[run]].
  */
final class EditorController private (
    val i18n: AppI18n,
    val host: EditorHost,
    val launch: EditorLaunch,
    val screen: SignalRef[EditorScreen],
    val doc: SignalRef[DesignDocument],
    val session: SignalRef[Option[EditingSession]],
    val pageId: SignalRef[PageId],
    val selection: SignalRef[List[ElementId]],
    val panel: SignalRef[Option[LeftPanel]],
    val zoom: SignalRef[Double],
    val zoomMode: SignalRef[ZoomMode],
    val preview: SignalRef[List[GesturePreview]],
    val guides: SignalRef[List[SnapGuide]],
    val saveStatus: SignalRef[SaveStatus],
    val assetUrls: SignalRef[Map[AssetId, String]],
    val designs: SignalRef[List[LocalDesign]],
    val history: SignalRef[History],
    val showGuides: SignalRef[Boolean],
    val toast: SignalRef[Option[String]],
    val reviewOpen: SignalRef[Boolean],
    val homeKind: SignalRef[Option[DesignProductKind]],
    val confirmTemplate: SignalRef[Option[String]],
    private val gesture: AtomicRef[Option[ActiveGesture]],
    private val clipboard: AtomicRef[List[DesignElement]],
    private val dirtyTicket: AtomicRef[Long],
    private val viewport: AtomicRef[(Double, Double)],
):

  import EditorController.*

  // ── Derived signals ──────────────────────────────────────────────────────

  def currentPage: Signal[Option[DesignPage]] =
    doc.combineLatest(pageId).map((d, id) => d.page(id).orElse(d.pages.headOption))

  def selectedElements: Signal[List[DesignElement]] =
    currentPage.combineLatest(selection).map { (page, sel) =>
      page.toList.flatMap(p => sel.flatMap(id => p.element(id)))
    }

  def issues: Signal[List[DesignIssue]] = doc.map(d => DesignPreflight.check(d))

  def language: Signal[Language] = i18n.language

  def canUndo: Signal[Boolean] = history.map(_.past.nonEmpty)
  def canRedo: Signal[Boolean] = history.map(_.future.nonEmpty)

  // ── Document commits & history ───────────────────────────────────────────

  /** Applies a change as one undo step. `coalesce` merges rapid edits of the same field (typing, sliders). */
  def commit(f: DesignDocument => DesignDocument, coalesce: Option[String] = None): Unit < Sync =
    for
      current <- doc.get
      next     = f(current)
      _ <-
        if next == current then Kyo.unit
        else
          for
            now <- Sync.defer(java.lang.System.currentTimeMillis())
            _ <- history.getAndUpdate { h =>
                   val merge = coalesce.isDefined && h.lastKey == coalesce && now - h.lastAt < 1500
                   if merge then h.copy(lastAt = now, future = Nil)
                   else History((current :: h.past).take(MaxHistory), Nil, coalesce, now)
                 }.unit
            _ <- doc.set(next)
            _ <- markDirty
          yield ()
    yield ()

  def undo: Unit < Sync =
    for
      h   <- history.get
      cur <- doc.get
      _ <- h.past match
             case prev :: rest =>
               history.set(History(rest, cur :: h.future, None, 0L)).andThen(doc.set(prev)).andThen(fixSelection).andThen(markDirty)
             case Nil => Kyo.unit
    yield ()

  def redo: Unit < Sync =
    for
      h   <- history.get
      cur <- doc.get
      _ <- h.future match
             case next :: rest =>
               history.set(History(cur :: h.past, rest, None, 0L)).andThen(doc.set(next)).andThen(fixSelection).andThen(markDirty)
             case Nil => Kyo.unit
    yield ()

  private def fixSelection: Unit < Sync =
    for
      d   <- doc.get
      pid <- pageId.get
      _   <- if d.page(pid).isEmpty then pageId.set(d.pages.head.id) else Kyo.unit
      p   <- pageId.get
      _   <- selection.getAndUpdate(sel => sel.filter(id => d.page(p).exists(_.element(id).isDefined))).unit
    yield ()

  // ── Autosave ─────────────────────────────────────────────────────────────

  private def markDirty: Unit < Sync =
    for
      ticket <- dirtyTicket.updateAndGet(_ + 1)
      _      <- saveStatus.set(SaveStatus.Dirty)
      _      <- Fiber.initUnscoped(Async.sleep(AutosaveDelay).andThen(saveIfTicket(ticket))).unit
    yield ()

  private def saveIfTicket(ticket: Long): Unit < Async =
    dirtyTicket.get.map(current => if current == ticket then saveNow else Kyo.unit)

  /** Persists the current design locally (and pushes it to the server when sync is available). */
  def saveNow: Unit < Async =
    session.get.map {
      case None => Kyo.unit
      case Some(s) =>
        for
          _   <- saveStatus.set(SaveStatus.Saving)
          d   <- doc.get
          now <- Effects.now
          rev  = s.revision + 1
          project = DesignProject(s.id, s.name, s.owner, d, rev, DesignStatus.Draft, s.createdAt, now)
          _   <- host.store.save(LocalDesign(project, s.syncedRevision, pendingPush = true))
          _   <- session.set(Some(s.copy(revision = rev)))
          _   <- saveStatus.set(SaveStatus.Saved)
          _   <- refreshDesigns
        yield ()
    }

  def refreshDesigns: Unit < Async =
    host.store.list.map(list => designs.set(list.filterNot(_.project.status == DesignStatus.Archived)))

  // ── Opening / creating designs ───────────────────────────────────────────

  private def newDesignId: DesignId < Sync =
    Sync.defer {
      val stamp = java.lang.Long.toString(java.lang.System.currentTimeMillis(), 36)
      val rnd   = scala.util.Random.alphanumeric.filter(_.isLetterOrDigit).take(6).mkString.toLowerCase
      DesignId.unsafe(s"design-$stamp-$rnd")
    }

  def templateContext: TemplateContext < Sync =
    for
      lang <- i18n.currentLanguage
      year <- Sync.defer {
                val today = java.time.Instant.ofEpochMilli(java.lang.System.currentTimeMillis()).atOffset(java.time.ZoneOffset.UTC).toLocalDate
                // From September on, customers design next year's calendar.
                launch.year.getOrElse(today.getYear + (if today.getMonthValue >= 9 then 1 else 0))
              }
    yield TemplateContext(lang, year, 1, launch.format, launch.pageCount, launch.product)

  /** Starts a new design from a template (or blank) and opens the editor. */
  def createFromTemplate(templateKey: Option[String], kind: DesignProductKind): Unit < Async =
    for
      ctx  <- templateContext
      id   <- newDesignId
      now  <- Effects.now
      lang <- i18n.currentLanguage
      document = templateKey.flatMap(DesignTemplates.find) match
                   case Some(t) => DesignTemplates.instantiate(t, ctx)
                   case None    => DesignTemplates.blank(ctx, kind)
      name = launch.title.getOrElse(DesignPresets.kindLabel(document.product.kind)(lang))
      _ <- openDocument(EditingSession(id, name, host.owner, now, 0L, None), document)
      _ <- saveNow
    yield ()

  /** Opens a stored design for editing. Submitted designs are duplicated first (they are frozen). */
  def openDesign(id: DesignId): Unit < Async =
    host.store.load(id).map {
      case None => toastMessage("ve-toast-not-found")
      case Some(local) =>
        val p = local.project
        if p.isEditable then openDocument(EditingSession(p.id, p.name, p.owner, p.createdAt, p.revision, local.syncedRevision), p.document)
        else
          for
            newId <- newDesignId
            now   <- Effects.now
            lang  <- i18n.currentLanguage
            _     <- openDocument(EditingSession(newId, s"${p.name} (${if lang == Language.Cs then "kopie" else "copy"})", host.owner, now, 0L, None), p.document)
            _     <- saveNow
          yield ()
    }

  private def openDocument(s: EditingSession, d: DesignDocument): Unit < Async =
    for
      _ <- session.set(Some(s))
      _ <- doc.set(d)
      _ <- pageId.set(d.pages.head.id)
      _ <- selection.set(Nil)
      _ <- preview.set(Nil)
      _ <- guides.set(Nil)
      _ <- history.set(History(Nil, Nil, None, 0L))
      _ <- panel.set(Some(if d.allElements.exists(_.isImage) then LeftPanel.Photos else LeftPanel.Text))
      _ <- saveStatus.set(SaveStatus.Saved)
      _ <- zoomMode.set(ZoomMode.Fit)
      _ <- applyFit
      _ <- resolveAssets(d)
      _ <- screen.set(EditorScreen.Editing)
    yield ()

  def goHome: Unit < Async =
    for
      status <- saveStatus.get
      _      <- if status == SaveStatus.Dirty then saveNow else Kyo.unit
      _      <- reviewOpen.set(false)
      _      <- screen.set(EditorScreen.Home)
      _      <- refreshDesigns
    yield ()

  def deleteDesign(id: DesignId): Unit < Async =
    host.store.delete(id).andThen(refreshDesigns)

  def duplicateDesign(id: DesignId): Unit < Async =
    host.store.load(id).map {
      case None => Kyo.unit
      case Some(local) =>
        for
          newId <- newDesignId
          now   <- Effects.now
          lang  <- i18n.currentLanguage
          copy   = local.project.copy(
                     id = newId,
                     name = s"${local.project.name} (${if lang == Language.Cs then "kopie" else "copy"})",
                     owner = host.owner,
                     revision = 1,
                     status = DesignStatus.Draft,
                     createdAt = now,
                     updatedAt = now,
                   )
          _ <- host.store.save(LocalDesign(copy))
          _ <- refreshDesigns
        yield ()
    }

  def rename(name: String): Unit < Sync =
    val trimmed = name.trim.take(120)
    if trimmed.isEmpty then Kyo.unit
    else session.getAndUpdate(_.map(_.copy(name = trimmed))).andThen(markDirty)

  /** Replaces the current document with another template, carrying the customer's photos over into its slots. */
  def applyTemplate(key: String): Unit < Sync =
    DesignTemplates.find(key) match
      case None => Kyo.unit
      case Some(t) =>
        for
          ctx <- templateContext
          cur <- doc.get
          fresh = DesignTemplates.instantiate(t, ctx.copy(format = Some(cur.format), product = Some(cur.product)))
          carried = carryPhotos(cur, fresh)
          _ <- commit(_ => carried)
          _ <- pageId.set(carried.pages.head.id)
          _ <- selection.set(Nil)
          _ <- confirmTemplate.set(None)
        yield ()

  private def carryPhotos(from: DesignDocument, to: DesignDocument): DesignDocument =
    val used = from.allElements.collect { case DesignElement(_, _, ElementContent.Image(Some(a), _, _, _, _), _, _, _) => a }.distinct
    val withAssets = from.assets.foldLeft(to)((d, a) => DesignOps.addAsset(d, a))
    used.foldLeft(withAssets) { (d, assetId) =>
      d.pages.iterator.flatMap(p => DesignOps.firstEmptySlot(p).map(p.id -> _)).nextOption() match
        case Some((pid, slot)) => DesignOps.placeImage(d, pid, slot, assetId)
        case None              => d
    }

  // ── Pages ────────────────────────────────────────────────────────────────

  def selectPage(id: PageId): Unit < Sync =
    pageId.set(id).andThen(selection.set(Nil))

  def stepPage(delta: Int): Unit < Sync =
    for
      d   <- doc.get
      pid <- pageId.get
      idx  = d.pageIndex(pid)
      to   = math.max(0, math.min(d.pages.size - 1, idx + delta))
      _   <- selectPage(d.pages(to).id)
    yield ()

  def addPage: Unit < Sync =
    for
      d   <- doc.get
      pid <- pageId.get
      _ <- DesignOps.addPages(d, pid) match
             case Right((next, newId)) => commit(_ => next).andThen(selectPage(newId))
             case Left(_)              => toastMessage("ve-toast-page-limit")
    yield ()

  def duplicatePage: Unit < Sync =
    for
      d   <- doc.get
      pid <- pageId.get
      _ <- DesignOps.duplicatePage(d, pid) match
             case Right((next, newId)) => commit(_ => next).andThen(selectPage(newId))
             case Left(_)              => toastMessage("ve-toast-page-limit")
    yield ()

  def removePage: Unit < Sync =
    for
      d   <- doc.get
      pid <- pageId.get
      idx  = d.pageIndex(pid)
      _ <- DesignOps.removePage(d, pid) match
             case Right(next) => commit(_ => next).andThen(selectPage(next.pages(math.max(0, math.min(next.pages.size - 1, idx))).id))
             case Left(_)     => toastMessage("ve-toast-page-limit")
    yield ()

  def movePage(delta: Int): Unit < Sync =
    pageId.get.map(pid => commit(d => DesignOps.movePage(d, pid, delta)))

  // ── Selection & element editing ──────────────────────────────────────────

  def select(ids: List[ElementId]): Unit < Sync = selection.set(ids)
  def clearSelection: Unit < Sync              = selection.set(Nil)

  private def withPage[A](f: (DesignDocument, PageId) => A < Sync): A < Sync =
    doc.get.map(d => pageId.get.map(pid => f(d, pid)))

  def updateSelected(f: DesignElement => DesignElement, coalesce: Option[String] = None): Unit < Sync =
    for
      pid <- pageId.get
      sel <- selection.get
      _   <- commit(d => DesignOps.updateElements(d, pid, sel.toSet)(e => if e.locked && !lockNeutral(e, f) then e else f(e)), coalesce)
    yield ()

  /** Locking itself must still be toggleable on a locked element. */
  private def lockNeutral(e: DesignElement, f: DesignElement => DesignElement): Boolean =
    val after = f(e)
    after.copy(locked = e.locked) == e

  def updateSelectedContent(f: ElementContent => ElementContent, coalesce: Option[String] = None): Unit < Sync =
    updateSelected(e => e.copy(content = f(e.content)), coalesce)

  def setSelectedPlacement(f: Placement => Placement): Unit < Sync =
    updateSelected(e => e.copy(frame = f(e.frame)), Some("placement"))

  def addElement(content: ElementContent, width: Double, height: Double, name: Option[String] = None): Unit < Sync =
    for
      d   <- doc.get
      pid <- pageId.get
      id   = DesignIds.freshElementId(d)
      fmt  = d.format
      w    = math.min(width, fmt.trimWidthMm * 0.9)
      h    = math.min(height, fmt.trimHeightMm * 0.9)
      // Cascade new elements slightly so repeated clicks don't stack exactly.
      n    = d.page(pid).map(_.elements.size).getOrElse(0) % 5
      el   = DesignElement(id, Placement((fmt.trimWidthMm - w) / 2 + n * 3, (fmt.trimHeightMm - h) / 2 + n * 3, w, h), content, name = name)
      _   <- commit(dd => DesignOps.addElement(dd, pid, el))
      _   <- selection.set(List(id))
    yield ()

  def addText(kind: TextPreset): Unit < Sync =
    for
      d    <- doc.get
      lang <- i18n.currentLanguage
      short = math.min(d.format.trimWidthMm, d.format.trimHeightMm)
      (txt, style, h) = kind match
        case TextPreset.Heading =>
          (if lang == Language.Cs then "Nadpis" else "Heading", TextStyle(fontKey = "montserrat", sizePt = math.max(10, Units.mmToPt(short * 0.09)), weight = 700), short * 0.14)
        case TextPreset.Subheading =>
          (if lang == Language.Cs then "Podnadpis" else "Subheading", TextStyle(fontKey = "montserrat", sizePt = math.max(8, Units.mmToPt(short * 0.05)), weight = 600), short * 0.09)
        case TextPreset.Body =>
          (if lang == Language.Cs then "Sem napište svůj text." else "Write your text here.", TextStyle(sizePt = math.max(7, Units.mmToPt(short * 0.032)), lineHeight = 1.4), short * 0.12)
        case TextPreset.Script =>
          (if lang == Language.Cs then "S láskou" else "With love", TextStyle(fontKey = "dancing", sizePt = math.max(10, Units.mmToPt(short * 0.08)), weight = 700), short * 0.13)
      _ <- addElement(ElementContent.Text(txt, style), d.format.trimWidthMm * 0.7, h)
    yield ()

  def addShape(kind: ShapeKind): Unit < Sync =
    doc.get.map { d =>
      val s = math.min(d.format.trimWidthMm, d.format.trimHeightMm) * 0.3
      kind match
        case ShapeKind.Line => addElement(ElementContent.Shape(kind, Fill.Solid(DesignColor.Ink), None), s * 1.5, 0.6)
        case _              => addElement(ElementContent.Shape(kind, Fill.Solid(DesignColor.unsafe("#4f46e5"))), s, s)
    }

  def addClipart(kind: ClipartKind): Unit < Sync =
    doc.get.map { d =>
      val s = math.min(d.format.trimWidthMm, d.format.trimHeightMm) * 0.22
      addElement(ElementContent.Clipart(kind, DesignColor.unsafe("#f97316")), s, s)
    }

  def addPhotoSlot: Unit < Sync =
    doc.get.map(d => addElement(ElementContent.Image(None), d.format.trimWidthMm * 0.5, d.format.trimHeightMm * 0.35))

  def addCalendar: Unit < Sync =
    for
      ctx <- templateContext
      d   <- doc.get
      _   <- addElement(
               ElementContent.Calendar(CalendarSpec(ctx.year, 1, ctx.language, holidays = if ctx.language == Language.Cs then HolidaySet.Czech else HolidaySet.NoHolidays)),
               d.format.safeBox.width,
               d.format.safeBox.height * 0.4,
             )
    yield ()

  def deleteSelected: Unit < Sync =
    for
      pid <- pageId.get
      sel <- selection.get
      _   <- commit(d => DesignOps.removeElements(d, pid, sel.toSet))
      _   <- fixSelection
    yield ()

  def duplicateSelected: Unit < Sync =
    for
      pid <- pageId.get
      sel <- selection.get
      d   <- doc.get
      (next, ids) = DesignOps.duplicateElements(d, pid, sel.toSet)
      _ <- commit(_ => next)
      _ <- selection.set(ids)
    yield ()

  def reorderSelected(move: LayerMove): Unit < Sync =
    for
      pid <- pageId.get
      sel <- selection.get
      _   <- commit(d => sel.foldLeft(d)((acc, id) => DesignOps.reorder(acc, pid, id, move)))
    yield ()

  def alignSelected(mode: AlignMode): Unit < Sync =
    for
      pid <- pageId.get
      sel <- selection.get
      _   <- commit(d => DesignOps.align(d, pid, sel.toSet, mode))
    yield ()

  def nudge(dx: Double, dy: Double): Unit < Sync =
    for
      pid <- pageId.get
      sel <- selection.get
      _   <- commit(d => DesignOps.moveElements(d, pid, sel.toSet, dx, dy), Some(s"nudge-${sel.mkString}"))
    yield ()

  def setBackground(fill: Fill, allPages: Boolean): Unit < Sync =
    pageId.get.map(pid => commit(d => if allPages then DesignOps.setBackgroundAll(d, fill) else DesignOps.setBackground(d, pid, fill), Some("background")))

  def applyLayout(layout: PageLayout): Unit < Sync =
    for
      pid  <- pageId.get
      lang <- i18n.currentLanguage
      _    <- commit(d => PageLayouts.apply(d, pid, layout, lang))
      _    <- selection.set(Nil)
    yield ()

  // ── Photos ───────────────────────────────────────────────────────────────

  /** Registers a picked/dropped photo (bytes already stored by the platform) and places it. */
  def addPhoto(asset: ImageAsset, displayUrl: String, dropPoint: Option[Point] = None): Unit < Sync =
    for
      _   <- assetUrls.getAndUpdate(_ + (asset.id -> displayUrl)).unit
      _   <- commit(d => DesignOps.addAsset(d, asset))
      _   <- placePhoto(asset.id, dropPoint)
    yield ()

  /** Puts a photo into: the slot under the drop point, the selected photo slot, the first empty slot, or a new frame. */
  def placePhoto(assetId: AssetId, dropPoint: Option[Point] = None): Unit < Sync =
    for
      d   <- doc.get
      pid <- pageId.get
      sel <- selection.get
      page = d.page(pid)
      asset = d.asset(assetId)
      target = page.flatMap { p =>
                 dropPoint.flatMap(pt => p.elements.reverse.find(e => e.isImage && !e.locked && e.frame.contains(pt)).map(_.id))
                   .orElse(sel.flatMap(p.element).find(e => e.isImage && !e.locked).map(_.id))
                   .orElse(DesignOps.firstEmptySlot(p))
               }
      _ <- target match
             case Some(id) => commit(dd => DesignOps.placeImage(dd, pid, id, assetId)).andThen(selection.set(List(id)))
             case None =>
               val aspect = asset.map(_.aspect).getOrElse(1.5)
               val w      = d.format.trimWidthMm * 0.6
               addElement(ElementContent.Image(Some(assetId)), w, w / aspect)
    yield ()

  def registerAssetUrl(id: AssetId, url: String): Unit < Sync = assetUrls.getAndUpdate(_ + (id -> url)).unit

  private def resolveAssets(d: DesignDocument): Unit < Async =
    for
      known <- assetUrls.get
      missing = d.assets.filterNot(a => known.contains(a.id))
      resolved <- Kyo.foreach(missing)(a => host.assets.resolve(a).map(_.map(a.id -> _)))
      _ <- assetUrls.getAndUpdate(_ ++ resolved.flatten).unit
    yield ()

  // ── Calendar (document-wide) ─────────────────────────────────────────────

  def calendarStart: Signal[Option[(Int, Int)]] =
    doc.map(_.allElements.collectFirst { case DesignElement(_, _, ElementContent.Calendar(s), _, _, _) => (s.year, s.month) })

  def setCalendarStart(year: Int, month: Int): Unit < Sync =
    commit(d => DesignOps.setCalendarStart(d, year, month))

  def updateCalendars(f: CalendarSpec => CalendarSpec): Unit < Sync =
    commit(d => DesignOps.updateAllCalendars(d)(f), Some("calendar-all"))

  // ── Zoom & viewport ──────────────────────────────────────────────────────

  def setViewport(width: Double, height: Double): Unit < Sync =
    viewport.set((width, height)).andThen(zoomMode.get.map(m => if m == ZoomMode.Fit then applyFit else Kyo.unit))

  private def fitZoom(d: DesignDocument, vp: (Double, Double)): Double =
    val b      = d.format.bleedBox
    val (w, h) = vp
    val z      = math.min((w - 56) / b.width, (h - 56) / b.height)
    math.max(MinZoom, math.min(MaxZoom, z))

  def applyFit: Unit < Sync =
    for
      d  <- doc.get
      vp <- viewport.get
      _  <- zoom.set(fitZoom(d, vp))
      _  <- zoomMode.set(ZoomMode.Fit)
    yield ()

  def zoomBy(factor: Double): Unit < Sync =
    zoom.getAndUpdate(z => math.max(MinZoom, math.min(MaxZoom, z * factor))).andThen(zoomMode.set(ZoomMode.Manual))

  // ── Pointer gestures (called synchronously by the browser bridge) ───────

  def pointerDown(target: CanvasTarget, point: Point, shift: Boolean): Unit < Sync =
    for
      d   <- doc.get
      pid <- pageId.get
      sel <- selection.get
      page = d.page(pid)
      _ <- target match
             case CanvasTarget.Background =>
               gesture.set(None).andThen(if shift then Kyo.unit else selection.set(Nil))
             case CanvasTarget.Element(id) =>
               page.flatMap(_.element(id)) match
                 case None => Kyo.unit
                 case Some(el) =>
                   if shift then
                     gesture.set(None).andThen(selection.set(if sel.contains(id) then sel.filterNot(_ == id) else sel :+ id))
                   else
                     val newSel = if sel.contains(id) then sel else List(id)
                     val starts = newSel.flatMap(i => page.flatMap(_.element(i))).filterNot(_.locked).map(e => e.id -> e.frame)
                     val others = page.toList.flatMap(_.elements).filterNot(e => newSel.contains(e.id)).map(_.frame.bounds)
                     selection.set(newSel).andThen(
                       if starts.isEmpty then gesture.set(None)
                       else gesture.set(Some(ActiveGesture(GestureKind.Move, point, starts, DesignGeometry.snapLines(d.format, others))))
                     )
             case CanvasTarget.ResizeHandle(handle) =>
               sel.flatMap(i => page.flatMap(_.element(i))) match
                 case el :: Nil if !el.locked =>
                   val aspect = el.content match
                     case _: ElementContent.Clipart => true
                     case _: ElementContent.Image   => false
                     case _                          => false
                   gesture.set(Some(ActiveGesture(GestureKind.Resize(handle, aspect), point, List(el.id -> el.frame), (Nil, Nil))))
                 case _ => gesture.set(None)
             case CanvasTarget.RotateHandle =>
               sel.flatMap(i => page.flatMap(_.element(i))) match
                 case el :: Nil if !el.locked =>
                   gesture.set(Some(ActiveGesture(GestureKind.Rotate, point, List(el.id -> el.frame), (Nil, Nil))))
                 case _ => gesture.set(None)
    yield ()

  /** Updates the live preview; returns the previews so the bridge can transform the drawn elements directly. */
  def pointerMove(point: Point, shift: Boolean, alt: Boolean): List[GesturePreview] < Sync =
    gesture.get.map {
      case None => (Nil: List[GesturePreview] < Sync)
      case Some(g) =>
        val (previews, snapGuides) = computePreview(g, point, shift, alt)
        preview.set(previews).andThen(guides.set(snapGuides)).andThen(previews)
    }

  def zoomTolerance: Double < Sync = zoom.get.map(z => 6.0 / math.max(0.1, z))

  private def computePreview(g: ActiveGesture, point: Point, shift: Boolean, alt: Boolean): (List[GesturePreview], List[SnapGuide]) =
    g.kind match
      case GestureKind.Move =>
        val dx     = point.x - g.origin.x
        val dy     = point.y - g.origin.y
        val bounds = g.starts.map(_._2.bounds).reduce((a, b) => a.union(b))
        val snap =
          if alt then SnapResult(dx, dy, Nil)
          else DesignGeometry.snapMove(bounds, dx, dy, g.snapLines, tolerance = 1.6)
        val (fdx, fdy) =
          if shift then (if math.abs(snap.dx) >= math.abs(snap.dy) then (snap.dx, 0.0) else (0.0, snap.dy))
          else (snap.dx, snap.dy)
        (g.starts.map((id, s) => GesturePreview(id, s, s.moveBy(DesignGeometry.round2(fdx), DesignGeometry.round2(fdy)))), snap.guides)
      case GestureKind.Resize(handle, aspectDefault) =>
        val (id, s) = g.starts.head
        val keep    = if handle.isCorner then aspectDefault != shift else false
        (List(GesturePreview(id, s, DesignGeometry.resize(s, handle, point, keepAspect = keep, fromCenter = alt))), Nil)
      case GestureKind.Rotate =>
        val (id, s) = g.starts.head
        (List(GesturePreview(id, s, DesignGeometry.rotate(s, point, snap = shift))), Nil)

  def pointerUp(point: Point, shift: Boolean, alt: Boolean): Unit < Sync =
    gesture.getAndSet(None).map {
      case None => preview.set(Nil).andThen(guides.set(Nil))
      case Some(g) =>
        val (previews, _) = computePreview(g, point, shift, alt)
        val moved         = previews.exists(p => p.start != p.current && distance(p.start, p.current) > 0.05)
        for
          pid <- pageId.get
          _ <-
            if moved then commit(d => previews.foldLeft(d)((acc, p) => DesignOps.setFrame(acc, pid, p.id, p.current)))
            else Kyo.unit
          _ <- preview.set(Nil)
          _ <- guides.set(Nil)
        yield ()
    }

  def pointerCancel: Unit < Sync =
    gesture.set(None).andThen(preview.set(Nil)).andThen(guides.set(Nil))

  def gestureActive: Boolean < Sync = gesture.get.map(_.isDefined)

  private def distance(a: Placement, b: Placement): Double =
    math.abs(a.x - b.x) + math.abs(a.y - b.y) + math.abs(a.width - b.width) + math.abs(a.height - b.height) + math.abs(a.rotation - b.rotation)

  // ── Commands (keyboard & toolbar) ────────────────────────────────────────

  def run(cmd: EditorCommand): Unit < Async =
    cmd match
      case EditorCommand.Undo      => undo
      case EditorCommand.Redo      => redo
      case EditorCommand.Delete    => deleteSelected
      case EditorCommand.Duplicate => duplicateSelected
      case EditorCommand.Copy =>
        selectedNow.map(els => clipboard.set(els))
      case EditorCommand.Paste =>
        for
          els <- clipboard.get
          pid <- pageId.get
          d   <- doc.get
          ids  = DesignIds.freshElementIds(d, els.size)
          copies = els.zip(ids).map((e, id) => e.copy(id = id, locked = false, frame = e.frame.moveBy(4, 4)))
          _ <- if copies.isEmpty then Kyo.unit else commit(dd => copies.foldLeft(dd)((acc, e) => DesignOps.addElement(acc, pid, e))).andThen(selection.set(ids))
          _ <- clipboard.set(copies)
        yield ()
      case EditorCommand.Deselect  => clearSelection
      case EditorCommand.SelectAll =>
        currentPageNow.map(p => selection.set(p.toList.flatMap(_.elements.filterNot(_.locked).map(_.id))))
      case EditorCommand.NextPage   => stepPage(1)
      case EditorCommand.PrevPage   => stepPage(-1)
      case EditorCommand.ZoomIn     => zoomBy(1.2)
      case EditorCommand.ZoomOut    => zoomBy(1 / 1.2)
      case EditorCommand.ZoomFit    => applyFit
      case EditorCommand.Save       => saveNow
      case EditorCommand.Nudge(dx, dy) => nudge(dx, dy)

  private def selectedNow: List[DesignElement] < Sync =
    for
      p   <- currentPageNow
      sel <- selection.get
    yield p.toList.flatMap(pg => sel.flatMap(pg.element))

  private def currentPageNow: Option[DesignPage] < Sync =
    doc.get.map(d => pageId.get.map(pid => d.page(pid)))

  // ── Review & completion ──────────────────────────────────────────────────

  def openReview: Unit < Async = saveNow.andThen(reviewOpen.set(true))

  /** Final hand-off to the host ("Use this design"): proof of the first page + preflight summary. */
  def complete: Unit < Async =
    host.onComplete match
      case None => reviewOpen.set(false)
      case Some(callback) =>
        for
          _    <- saveNow
          s    <- session.get
          d    <- doc.get
          urls <- assetUrls.get
          now  <- Effects.now
          _ <- s match
                 case None => Kyo.unit
                 case Some(sess) =>
                   val proof   = DesignSvg.renderPage(d, d.pages.head, RenderOptions(urls.get, RenderMode.Preview, idPrefix = "proof", clipToTrim = true))
                   val project = DesignProject(sess.id, sess.name, sess.owner, d, sess.revision, DesignStatus.Draft, sess.createdAt, now)
                   reviewOpen.set(false).andThen(
                     callback(EditorResult(project, proof, DesignSvg.svgDataUrl(proof), DesignPreflight.check(d), DesignPreflight.minimumDpi(d)))
                   )
        yield ()

  def close: Unit < Async =
    for
      status <- saveStatus.get
      _      <- if status == SaveStatus.Dirty then saveNow else Kyo.unit
      _      <- host.onClose match
                  case Some(cb) => cb()
                  case None     => goHome
    yield ()

  /** Shows a transient message (an i18n key, translated by the view). */
  def toastMessage(key: String): Unit < Sync =
    toast.set(Some(key))

end EditorController

enum TextPreset derives CanEqual:
  case Heading, Subheading, Body, Script

object EditorController:

  val MaxHistory: Int       = 100
  val MinZoom: Double       = 0.4
  val MaxZoom: Double       = 16.0
  val AutosaveDelay: Duration = 900.millis

  /** Placeholder document shown before a design is opened. */
  def placeholderDocument: DesignDocument =
    DesignTemplates.blank(TemplateContext(Language.En, 2027), DesignProductKind.Custom)

  def init(i18n: AppI18n, host: EditorHost, launch: EditorLaunch): EditorController < Async =
    for
      screen    <- Signal.initRef[EditorScreen](EditorScreen.Home)
      doc       <- Signal.initRef(placeholderDocument)
      session   <- Signal.initRef[Option[EditingSession]](None)
      pageId    <- Signal.initRef(placeholderDocument.pages.head.id)
      selection <- Signal.initRef[List[ElementId]](Nil)
      panel     <- Signal.initRef[Option[LeftPanel]](Some(LeftPanel.Text))
      zoom      <- Signal.initRef(2.0)
      zoomMode  <- Signal.initRef[ZoomMode](ZoomMode.Fit)
      preview   <- Signal.initRef[List[GesturePreview]](Nil)
      guides    <- Signal.initRef[List[SnapGuide]](Nil)
      status    <- Signal.initRef[SaveStatus](SaveStatus.Idle)
      urls      <- Signal.initRef(Map.empty[AssetId, String])
      designs   <- Signal.initRef[List[LocalDesign]](Nil)
      history   <- Signal.initRef(History(Nil, Nil, None, 0L))
      showGuide <- Signal.initRef(true)
      toast     <- Signal.initRef[Option[String]](None)
      review    <- Signal.initRef(false)
      homeKind  <- Signal.initRef[Option[DesignProductKind]](launch.kind)
      confirmT  <- Signal.initRef[Option[String]](None)
      gesture   <- AtomicRef.init[Option[ActiveGesture]](None)
      clip      <- AtomicRef.init[List[DesignElement]](Nil)
      ticket    <- AtomicRef.init(0L)
      viewport  <- AtomicRef.init((960.0, 680.0))
      ctrl = new EditorController(
               i18n, host, launch, screen, doc, session, pageId, selection, panel, zoom, zoomMode, preview, guides, status, urls,
               designs, history, showGuide, toast, review, homeKind, confirmT, gesture, clip, ticket, viewport,
             )
      _ <- ctrl.refreshDesigns
      _ <- launch.designId match
             case Some(id) => ctrl.openDesign(id)
             case None     => Kyo.unit
    yield ctrl
