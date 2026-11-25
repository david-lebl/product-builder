package mpbuilder.design

enum AlignMode derives CanEqual:
  case Left, CenterH, Right, Top, Middle, Bottom

enum LayerMove derives CanEqual:
  case Forward, Backward, ToFront, ToBack

/** Deterministic, collision-free id allocation inside a document (`prefix-N`). */
object DesignIds:
  def freshElementId(doc: DesignDocument, prefix: String = "el"): ElementId =
    val taken = doc.allElements.map(_.id.value).toSet
    ElementId.unsafe(Iterator.from(doc.allElements.size + 1).map(n => s"$prefix-$n").find(id => !taken.contains(id)).get)

  def freshElementIds(doc: DesignDocument, count: Int, prefix: String = "el"): List[ElementId] =
    val taken = scala.collection.mutable.Set.from(doc.allElements.map(_.id.value))
    val it    = Iterator.from(doc.allElements.size + 1).map(n => s"$prefix-$n").filterNot(taken.contains)
    List.fill(count)(it.next()).map { id =>
      taken += id
      ElementId.unsafe(id)
    }

  def freshPageId(doc: DesignDocument): PageId =
    val taken = doc.pages.map(_.id.value).toSet
    PageId.unsafe(Iterator.from(doc.pages.size + 1).map(n => s"page-$n").find(id => !taken.contains(id)).get)

/** Pure document transformations. Every editor action is one of these, so the whole editing behaviour is
  * unit-testable on the JVM and replayable on the server.
  */
object DesignOps:

  // ── Pages ────────────────────────────────────────────────────────────────

  def updatePage(doc: DesignDocument, pageId: PageId)(f: DesignPage => DesignPage): DesignDocument =
    doc.copy(pages = doc.pages.map(p => if p.id == pageId then f(p) else p))

  def setBackground(doc: DesignDocument, pageId: PageId, fill: Fill): DesignDocument =
    updatePage(doc, pageId)(_.copy(background = fill))

  def setBackgroundAll(doc: DesignDocument, fill: Fill): DesignDocument =
    doc.copy(pages = doc.pages.map(_.copy(background = fill)))

  /** Inserts blank pages (as many as the policy step requires) after `afterPageId`. */
  def addPages(doc: DesignDocument, afterPageId: PageId): Either[String, (DesignDocument, PageId)] =
    val step = math.max(1, doc.pagePolicy.step)
    if !doc.pagePolicy.canAdd(doc.pages.size) then Left("page-limit")
    else
      val idx   = doc.pageIndex(afterPageId)
      val at    = if idx < 0 then doc.pages.size else idx + 1
      var draft = doc
      val fresh = (0 until step).map { _ =>
        val id = DesignIds.freshPageId(draft)
        val p  = DesignPage(id, PageRole.Inner)
        draft = draft.copy(pages = draft.pages :+ p)
        p
      }
      val pages = doc.pages.patch(at, fresh, 0)
      Right((doc.copy(pages = pages), fresh.head.id))

  def duplicatePage(doc: DesignDocument, pageId: PageId): Either[String, (DesignDocument, PageId)] =
    if doc.pagePolicy.step != 1 || !doc.pagePolicy.canAdd(doc.pages.size) then Left("page-limit")
    else
      doc.page(pageId) match
        case None => Left("page-not-found")
        case Some(page) =>
          val newId  = DesignIds.freshPageId(doc)
          val ids    = DesignIds.freshElementIds(doc, page.elements.size)
          val copied = page.copy(id = newId, role = PageRole.Inner, elements = page.elements.zip(ids).map((e, id) => e.copy(id = id)))
          val idx    = doc.pageIndex(pageId)
          Right((doc.copy(pages = doc.pages.patch(idx + 1, Seq(copied), 0)), newId))

  def removePage(doc: DesignDocument, pageId: PageId): Either[String, DesignDocument] =
    val step = math.max(1, doc.pagePolicy.step)
    if !doc.pagePolicy.canRemove(doc.pages.size) then Left("page-limit")
    else
      val idx = doc.pageIndex(pageId)
      if idx < 0 then Left("page-not-found")
      else if step == 1 then Right(doc.copy(pages = doc.pages.patch(idx, Nil, 1)))
      else
        // Remove the whole sheet group containing the page (keeps front/back pairing aligned).
        val groupStart = (idx / step) * step
        Right(doc.copy(pages = doc.pages.patch(groupStart, Nil, step)))

  def movePage(doc: DesignDocument, pageId: PageId, delta: Int): DesignDocument =
    val idx = doc.pageIndex(pageId)
    val to  = idx + delta
    if idx < 0 || to < 0 || to >= doc.pages.size || doc.pagePolicy.step != 1 then doc
    else
      val page = doc.pages(idx)
      doc.copy(pages = doc.pages.patch(idx, Nil, 1).patch(to, Seq(page), 0))

  // ── Elements ─────────────────────────────────────────────────────────────

  def addElement(doc: DesignDocument, pageId: PageId, element: DesignElement): DesignDocument =
    updatePage(doc, pageId)(p => p.copy(elements = p.elements :+ element))

  def updateElement(doc: DesignDocument, pageId: PageId, id: ElementId)(f: DesignElement => DesignElement): DesignDocument =
    updatePage(doc, pageId)(p => p.copy(elements = p.elements.map(e => if e.id == id then f(e) else e)))

  def updateElements(doc: DesignDocument, pageId: PageId, ids: Set[ElementId])(f: DesignElement => DesignElement): DesignDocument =
    updatePage(doc, pageId)(p => p.copy(elements = p.elements.map(e => if ids.contains(e.id) then f(e) else e)))

  def updateContent(doc: DesignDocument, pageId: PageId, id: ElementId)(f: ElementContent => ElementContent): DesignDocument =
    updateElement(doc, pageId, id)(e => e.copy(content = f(e.content)))

  def removeElements(doc: DesignDocument, pageId: PageId, ids: Set[ElementId]): DesignDocument =
    updatePage(doc, pageId)(p => p.copy(elements = p.elements.filterNot(e => ids.contains(e.id) && !e.locked)))

  /** Duplicates the elements with a small offset; returns the new ids (selected by the editor). */
  def duplicateElements(doc: DesignDocument, pageId: PageId, ids: Set[ElementId], offsetMm: Double = 4.0): (DesignDocument, List[ElementId]) =
    doc.page(pageId) match
      case None => (doc, Nil)
      case Some(page) =>
        val sources = page.elements.filter(e => ids.contains(e.id))
        val newIds  = DesignIds.freshElementIds(doc, sources.size)
        val copies  = sources.zip(newIds).map((e, id) => e.copy(id = id, locked = false, frame = e.frame.moveBy(offsetMm, offsetMm)))
        (updatePage(doc, pageId)(p => p.copy(elements = p.elements ++ copies)), newIds)

  def moveElements(doc: DesignDocument, pageId: PageId, ids: Set[ElementId], dx: Double, dy: Double): DesignDocument =
    updateElements(doc, pageId, ids)(e =>
      if e.locked then e else e.copy(frame = e.frame.moveBy(DesignGeometry.round2(dx), DesignGeometry.round2(dy)))
    )

  def setFrame(doc: DesignDocument, pageId: PageId, id: ElementId, frame: Placement): DesignDocument =
    updateElement(doc, pageId, id)(e => if e.locked then e else e.copy(frame = frame))

  def reorder(doc: DesignDocument, pageId: PageId, id: ElementId, move: LayerMove): DesignDocument =
    updatePage(doc, pageId) { p =>
      val idx = p.elements.indexWhere(_.id == id)
      if idx < 0 then p
      else
        val el   = p.elements(idx)
        val rest = p.elements.patch(idx, Nil, 1)
        val to = move match
          case LayerMove.Forward  => math.min(rest.size, idx + 1)
          case LayerMove.Backward => math.max(0, idx - 1)
          case LayerMove.ToFront  => rest.size
          case LayerMove.ToBack   => 0
        p.copy(elements = rest.patch(to, Seq(el), 0))
    }

  /** Aligns elements to the page trim box (single element) or to the selection bounds (several). */
  def align(doc: DesignDocument, pageId: PageId, ids: Set[ElementId], mode: AlignMode): DesignDocument =
    doc.page(pageId) match
      case None => doc
      case Some(page) =>
        val selected = page.elements.filter(e => ids.contains(e.id) && !e.locked)
        if selected.isEmpty then doc
        else
          val target =
            if selected.size == 1 then doc.format.trimBox
            else selected.map(_.frame.bounds).reduce((a, b) => a.union(b))
          updateElements(doc, pageId, selected.map(_.id).toSet) { e =>
            val b = e.frame.bounds
            val (dx, dy) = mode match
              case AlignMode.Left    => (target.x - b.x, 0.0)
              case AlignMode.CenterH => (target.centerX - b.centerX, 0.0)
              case AlignMode.Right   => (target.right - b.right, 0.0)
              case AlignMode.Top     => (0.0, target.y - b.y)
              case AlignMode.Middle  => (0.0, target.centerY - b.centerY)
              case AlignMode.Bottom  => (0.0, target.bottom - b.bottom)
            e.copy(frame = e.frame.moveBy(DesignGeometry.round2(dx), DesignGeometry.round2(dy)))
          }

  // ── Assets ───────────────────────────────────────────────────────────────

  def addAsset(doc: DesignDocument, asset: ImageAsset): DesignDocument =
    if doc.assets.exists(_.id == asset.id) then doc.copy(assets = doc.assets.map(a => if a.id == asset.id then asset else a))
    else doc.copy(assets = doc.assets :+ asset)

  def updateAssetStorage(doc: DesignDocument, id: AssetId, storage: AssetStorage): DesignDocument =
    doc.copy(assets = doc.assets.map(a => if a.id == id then a.copy(storage = storage) else a))

  /** Drops asset entries no element references any more (keeps the document small before syncing). */
  def pruneAssets(doc: DesignDocument): DesignDocument =
    val used = doc.usedAssetIds
    doc.copy(assets = doc.assets.filter(a => used.contains(a.id)))

  /** Puts the image into an element (photo slot) and resets the crop so the new photo is centred. */
  def placeImage(doc: DesignDocument, pageId: PageId, id: ElementId, asset: AssetId): DesignDocument =
    updateContent(doc, pageId, id) {
      case img: ElementContent.Image => img.copy(asset = Some(asset), crop = img.crop.copy(zoom = 1.0, focusX = 0.5, focusY = 0.5))
      case other                     => other
    }

  /** First empty photo slot on the page (used when the customer clicks a photo in the gallery). */
  def firstEmptySlot(page: DesignPage): Option[ElementId] =
    page.elements.collectFirst { case e @ DesignElement(_, _, ElementContent.Image(None, _, _, _, _), _, false, _) => e.id }

  // ── Calendar ─────────────────────────────────────────────────────────────

  /** Re-targets every calendar grid (and month page) to a new starting year/month, keeping the sequence. */
  def setCalendarStart(doc: DesignDocument, year: Int, month: Int): DesignDocument =
    var offset = 0
    val pages = doc.pages.map { page =>
      val hasGrid = page.elements.exists(_.content.isInstanceOf[ElementContent.Calendar])
      if !hasGrid then page
      else
        val total = (month - 1) + offset
        val y     = year + total / 12
        val m     = total % 12 + 1
        offset += 1
        page.copy(
          role = page.role match
            case PageRole.Month(_) => PageRole.Month(m)
            case other             => other
          ,
          elements = page.elements.map(e =>
            e.content match
              case ElementContent.Calendar(spec) => e.copy(content = ElementContent.Calendar(spec.copy(year = y, month = m)))
              case _                             => e
          ),
        )
    }
    doc.copy(pages = pages)

  /** Applies a property change to every calendar grid in the document (language, week start, holidays, colours). */
  def updateAllCalendars(doc: DesignDocument)(f: CalendarSpec => CalendarSpec): DesignDocument =
    doc.copy(pages = doc.pages.map(p =>
      p.copy(elements = p.elements.map(e =>
        e.content match
          case ElementContent.Calendar(spec) => e.copy(content = ElementContent.Calendar(f(spec)))
          case _                             => e
      ))
    ))
