package mpbuilder.design

import mpbuilder.design.core.*
import mpbuilder.shared.*
import kyo.*
import kyo.test.*

class DesignCoreSpec extends Test[Any]:

  private val ctxEn = TemplateContext(Language.En, year = 2027)
  private val ctxCs = TemplateContext(Language.Cs, year = 2027)

  private def close(a: Double, b: Double, eps: Double = 0.02): Boolean = math.abs(a - b) <= eps

  private def inMemoryRepo: DesignRepository < Sync =
    AtomicRef.init(Map.empty[DesignId, DesignProject]).map { ref =>
      new DesignRepository:
        def findById(id: DesignId): Option[DesignProject] < Async           = ref.get.map(_.get(id))
        def findByOwner(owner: DesignOwner): List[DesignProject] < Async    = ref.get.map(_.values.filter(_.owner == owner).toList)
        def save(project: DesignProject): Unit < Async                      = ref.update(_ + (project.id -> project))
        def delete(id: DesignId): Unit < Async                              = ref.update(_ - id)
    }

  "Design model & geometry" - {
    "colours parse, normalise and reject garbage" in {
      assert(DesignColor.parse("#ABC").toOption.map(_.hex).contains("#aabbcc"))
      assert(DesignColor.parse(" #4F46E5 ").toOption.map(_.hex).contains("#4f46e5"))
      assert(DesignColor.parse("red").isFailure)
      assert(DesignColor.parse("#12345g").isFailure)
      assert(DesignColor.fromRgb(255, 0, 16).hex == "#ff0010")
    }

    "format boxes include bleed, safe margin and the binding strip" in {
      val f = DesignPresets.format(DesignProductKind.WallCalendar)
      assert(f.bleedBox == Rect(-3, -3, 216, 303))
      assert(f.safeBox.y == 10.0) // binding (10 mm) is deeper than the 8 mm safe margin
      assert(f.bindingZone.exists(_.height == 13.0))
    }

    "rotated frames report correct bounds and hit-testing" in {
      val fr = Placement(0, 0, 20, 10, rotation = 90)
      val b  = fr.bounds
      assert(close(b.width, 10) && close(b.height, 20))
      assert(close(b.x, 5) && close(b.y, -5))
      assert(fr.contains(Point(10, -4)))
      assert(!fr.contains(Point(1, 1)))
    }

    "resizing an unrotated frame from the SE handle keeps the NW corner" in {
      val start = Placement(10, 10, 40, 20)
      val res   = DesignGeometry.resize(start, Handle.SE, Point(70, 50), keepAspect = false, fromCenter = false)
      assert(res == Placement(10, 10, 60, 40))
    }

    "aspect-locked corner resize scales uniformly" in {
      val start = Placement(0, 0, 40, 20)
      val res   = DesignGeometry.resize(start, Handle.SE, Point(80, 25), keepAspect = true, fromCenter = false)
      assert(close(res.width / res.height, 2.0))
      assert(res.x == 0.0 && res.y == 0.0)
    }

    "resizing a rotated frame keeps the opposite corner fixed in page space" in {
      val start    = Placement(50, 50, 40, 20, rotation = 30)
      val anchor   = start.corners.head // NW corner stays put when dragging SE
      val pointer  = DesignGeometry.rotateAround(Point(start.x + 60, start.y + 35), start.center, 30)
      val res      = DesignGeometry.resize(start, Handle.SE, pointer, keepAspect = false, fromCenter = false)
      val newNw    = res.corners.head
      assert(close(newNw.x, anchor.x, 0.05) && close(newNw.y, anchor.y, 0.05))
      assert(close(res.width, 60, 0.05) && close(res.height, 35, 0.05))
    }

    "rotation snaps to right angles and 15° steps" in {
      val fr = Placement(0, 0, 10, 10)
      assert(DesignGeometry.rotate(fr, Point(5, -20), snap = false).rotation == 0.0)
      assert(DesignGeometry.rotate(fr, Point(30, 6), snap = false).rotation == 90.0)
      assert(DesignGeometry.rotate(fr, Point(20, -9), snap = true).rotation == 45.0)
    }

    "snapping pulls a moving box onto the page centre line" in {
      val f     = DesignPresets.format(DesignProductKind.Flyer)
      val lines = DesignGeometry.snapLines(f, Nil)
      val box   = Rect(10, 10, 20, 20)
      val res   = DesignGeometry.snapMove(box, 53.5, 0, lines, tolerance = 2)
      assert(close(box.centerX + res.dx, f.trimWidthMm / 2))
      assert(res.guides.exists(_.axis == GuideAxis.Vertical))
    }

    "image placement covers the frame and reports effective DPI" in {
      val fr = Placement(0, 0, 100, 50)
      val p  = DesignGeometry.imagePlacement(fr, ImageCrop(), 2000, 2000)
      assert(close(p.width, 100) && close(p.height, 100))
      assert(close(p.y, -25))
      // 2000 px over 100 mm (3.94 in) ≈ 508 DPI
      assert(DesignGeometry.effectiveDpi(fr, ImageCrop(), 2000, 2000) == 508)
      assert(DesignGeometry.effectiveDpi(fr, ImageCrop(zoom = 2.0), 2000, 2000) == 254)
    }
  }

  "Calendar math" - {
    "month grids start on the right weekday and know ISO weeks" in {
      val spec = CalendarSpec(year = 2027, month = 2, weekStart = WeekStart.Monday)
      val rows = CalendarMath.monthGrid(spec)
      // 1 Feb 2027 is a Monday
      assert(rows.head.cells.head.day.contains(1))
      assert(rows.flatMap(_.cells.flatMap(_.day)).size == 28)
      assert(rows.head.isoWeek == 5)
      val sunday = CalendarMath.monthGrid(spec.copy(weekStart = WeekStart.Sunday))
      assert(sunday.head.cells(1).day.contains(1))
    }

    "Czech holidays include Easter Monday and Good Friday" in {
      val h = CalendarMath.czechHolidays(2027)
      assert(CalendarMath.easterSunday(2027) == java.time.LocalDate.of(2027, 3, 28))
      assert(h.contains(java.time.LocalDate.of(2027, 3, 29)))
      assert(h.contains(java.time.LocalDate.of(2027, 3, 26)))
      assert(h.contains(java.time.LocalDate.of(2027, 10, 28)))
      assert(h.size == 13)
    }

    "labels are localised" in {
      assert(CalendarMath.monthName(3, Language.Cs) == "Březen")
      assert(CalendarMath.weekdayLabel(WeekStart.Monday, 0, Language.En) == "Mo")
      assert(CalendarMath.weekdayLabel(WeekStart.Sunday, 0, Language.Cs) == "Ne")
    }
  }

  "Templates" - {
    "every template builds a valid document in both languages" in {
      DesignTemplates.all.foreach { t =>
        List(ctxEn, ctxCs).foreach { ctx =>
          val doc = DesignTemplates.instantiate(t, ctx)
          assert(DesignValidation.validate(doc).isSuccess, s"${t.key} invalid: ${DesignValidation.validate(doc)}")
          assert(doc.pagePolicy.allows(doc.pages.size) || doc.pagePolicy.isFixed, t.key)
        }
      }
    }

    "wall calendar has a cover and 12 consecutive months" in {
      val doc = DesignTemplates.instantiate(DesignTemplates.find("wall-calendar-photo").get, ctxCs.copy(startMonth = 11))
      assert(doc.pages.size == 13)
      val months = doc.allElements.collect { case DesignElement(_, _, ElementContent.Calendar(s), _, _, _) => (s.year, s.month) }
      assert(months.head == (2027, 11))
      assert(months(2) == (2028, 1))
      assert(doc.allElements.collect { case DesignElement(_, _, ElementContent.Calendar(s), _, _, _) => s.holidays }.forall(_ == HolidaySet.Czech))
    }

    "templates adapt to the configured format and single-sided page count" in {
      val fmt = DesignFormat(90, 50, bleedMm = 2, safeMarginMm = 3)
      val doc = DesignTemplates.instantiate(
        DesignTemplates.find("bc-classic").get,
        ctxEn.copy(format = Some(fmt), pageCount = Some(1), product = Some(ProductBinding(DesignProductKind.BusinessCard, Some(CategoryId.unsafe("cat-business-cards"))))),
      )
      assert(doc.format == fmt)
      assert(doc.pages.size == 1 && doc.pagePolicy.isFixed)
      assert(doc.product.categoryId.contains(CategoryId.unsafe("cat-business-cards")))
      assert(doc.allElements.forall(_.frame.x < 90))
    }

    "category ids map to editor product kinds" in {
      assert(DesignPresets.kindForCategory("cat-business-cards") == DesignProductKind.BusinessCard)
      assert(DesignPresets.kindForCategory("cat-calendars") == DesignProductKind.WallCalendar)
      assert(DesignPresets.kindForCategory("cat-roll-ups") == DesignProductKind.Poster)
      assert(DesignPresets.kindForCategory("cat-cups") == DesignProductKind.Custom)
    }
  }

  "Operations" - {
    val base   = DesignTemplates.instantiate(DesignTemplates.find("photobook-classic").get, ctxEn)
    val page1  = base.pages.head

    "duplicate, move, reorder and delete keep ids unique" in {
      val ids          = page1.elements.take(2).map(_.id).toSet
      val (dup, newIds) = DesignOps.duplicateElements(base, page1.id, ids)
      assert(newIds.size == 2)
      assert(DesignValidation.validate(dup).isSuccess)
      val moved = DesignOps.moveElements(dup, page1.id, newIds.toSet, 5, -5)
      val back  = DesignOps.reorder(moved, page1.id, newIds.head, LayerMove.ToBack)
      assert(back.page(page1.id).get.elements.head.id == newIds.head)
      val removed = DesignOps.removeElements(back, page1.id, newIds.toSet)
      assert(removed.page(page1.id).get.elements.size == page1.elements.size)
    }

    "photo book pages are added and removed in sheet pairs within the policy" in {
      val (added, _) = DesignOps.addPages(base, page1.id).toOption.get
      assert(added.pages.size == base.pages.size + 2)
      val removed = DesignOps.removePage(added, added.pages(3).id).toOption.get
      assert(removed.pages.size == base.pages.size)
      assert(DesignOps.removePage(base, page1.id).isLeft) // already at minimum
    }

    "aligning a single element centres it on the trim box" in {
      val el  = page1.elements.last
      val doc = DesignOps.align(base, page1.id, Set(el.id), AlignMode.CenterH)
      val b   = doc.page(page1.id).get.element(el.id).get.frame.bounds
      assert(close(b.centerX, base.format.trimWidthMm / 2))
    }

    "layouts keep placed photos and calendar start re-targets all months" in {
      val asset  = ImageAsset(AssetId.unsafe("img-1"), "a.jpg", "image/jpeg", 3000, 2000, 1000)
      val inner  = base.pages(1)
      val slot   = DesignOps.firstEmptySlot(inner).get
      val placed = DesignOps.placeImage(DesignOps.addAsset(base, asset), inner.id, slot, asset.id)
      val grid   = PageLayouts.apply(placed, inner.id, PageLayout.Grid4, Language.En)
      val images = grid.page(inner.id).get.elements.collect { case DesignElement(_, _, i: ElementContent.Image, _, _, _) => i }
      assert(images.size == 4)
      assert(images.head.asset.contains(asset.id))
      assert(DesignValidation.validate(grid).isSuccess)

      val cal = DesignTemplates.instantiate(DesignTemplates.find("desk-calendar").get, ctxEn)
      val re  = DesignOps.setCalendarStart(cal, 2028, 7)
      val ms  = re.allElements.collect { case DesignElement(_, _, ElementContent.Calendar(s), _, _, _) => (s.year, s.month) }
      assert(ms.head == (2028, 7) && ms.last == (2029, 6))
    }
  }

  "Preflight & rendering" - {
    "preflight flags empty slots, low resolution, unsafe text and missing bleed" in {
      val f    = DesignPresets.format(DesignProductKind.Flyer)
      val low  = ImageAsset(AssetId.unsafe("low"), "low.jpg", "image/jpeg", 400, 300, 1000)
      val page = DesignPage(
        PageId.unsafe("p"),
        PageRole.Single,
        elements = Vector(
          DesignElement(ElementId.unsafe("e1"), Placement(10, 10, 50, 40), ElementContent.Image(None)),
          DesignElement(ElementId.unsafe("e2"), Placement(10, 60, 120, 90), ElementContent.Image(Some(low.id))),
          DesignElement(ElementId.unsafe("e3"), Placement(1, 160, 100, 20), ElementContent.Text("Hello", TextStyle(sizePt = 5))),
          DesignElement(ElementId.unsafe("e4"), Placement(0, 190, 148, 20), ElementContent.Shape(ShapeKind.Rectangle, Fill.Solid(DesignColor.Black))),
        ),
      )
      val doc    = DesignDocument(1, ProductBinding(DesignProductKind.Flyer), f, PagePolicy(1, 1), Vector(page), Vector(low))
      val kinds  = DesignPreflight.check(doc).map(i => (i.elementId.map(_.value), i.kind))
      assert(kinds.contains((Some("e1"), IssueKind.EmptyPhotoSlot)))
      assert(kinds.exists { case (Some("e2"), IssueKind.VeryLowResolution(_)) => true; case _ => false })
      assert(kinds.contains((Some("e3"), IssueKind.OutsideSafeArea)))
      assert(kinds.contains((Some("e3"), IssueKind.TextTooSmall(5))))
      assert(kinds.contains((Some("e4"), IssueKind.ExtendToBleed)))
      assert(DesignPreflight.minimumDpi(doc).exists(_ < 150))
    }

    "SVG renderer emits trim-relative geometry, escapes text and resolves assets" in {
      val asset = ImageAsset(AssetId.unsafe("ph"), "p.jpg", "image/jpeg", 1200, 800, 1000)
      val page = DesignPage(
        PageId.unsafe("p"),
        PageRole.Single,
        Fill.Gradient(DesignColor.White, DesignColor.Black, 90),
        Vector(
          DesignElement(ElementId.unsafe("t"), Placement(10, 10, 50, 10, rotation = 15), ElementContent.Text("Tom & <Jerry>", TextStyle(fontKey = "playfair"))),
          DesignElement(ElementId.unsafe("i"), Placement(0, 20, 60, 40), ElementContent.Image(Some(asset.id), filter = ImageFilter.Grayscale)),
          DesignElement(ElementId.unsafe("c"), Placement(0, 70, 80, 60), ElementContent.Calendar(CalendarSpec(2027, 5, Language.Cs, holidays = HolidaySet.Czech))),
          DesignElement(ElementId.unsafe("s"), Placement(5, 5, 10, 10), ElementContent.Clipart(ClipartKind.Heart, DesignColor.unsafe("#ff0000"))),
        ),
      )
      val doc = DesignDocument(1, ProductBinding(DesignProductKind.Poster), DesignFormat(100, 150), PagePolicy(1, 1), Vector(page), Vector(asset))
      val svg = DesignSvg.renderPage(doc, page, RenderOptions(id => Option.when(id == asset.id)("blob:photo-1"), RenderMode.Editor, idPrefix = "x"))
      assert(svg.startsWith("<svg"))
      assert(svg.contains("""viewBox="-30 -30 1060 1560""""))
      assert(svg.contains("Tom &amp; &lt;Jerry&gt;"))
      assert(svg.contains("Playfair Display"))
      assert(svg.contains("""rotate(15 350 150)"""))
      assert(svg.contains("blob:photo-1"))
      assert(svg.contains("url(#x-gray)"))
      assert(svg.contains("Květen 2027"))
      assert(svg.contains("""data-ve-el="i""""))
      val print = DesignSvg.renderPage(doc, page, RenderOptions(_ => None, RenderMode.Print))
      assert(!print.contains("data-ve-el"))
      assert(DesignSvg.svgDataUrl("<svg/>") == "data:image/svg+xml;base64,PHN2Zy8+")
    }
  }

  "DesignService" - {
    val device   = DesignOwner.Device(NonEmptyString.unsafe("device-1"))
    val customer = DesignOwner.Customer(CustomerId.unsafe("cust-1"))
    val doc      = DesignTemplates.instantiate(DesignTemplates.find("bc-classic").get, ctxEn)

    "saves with optimistic revisions, rejects stale writes and foreign owners" in {
      val id = DesignId.unsafe("d-1")
      for
        repo     <- inMemoryRepo
        svc       = DesignServiceLive.make(repo)
        created  <- svc.create(CreateDesignCmd(id, device, "My card", doc))
        saved    <- svc.save(SaveDesignCmd(id, device, 1, "My card v2", doc))
        stale    <- svc.save(SaveDesignCmd(id, device, 1, "Stale", doc)).either
        foreign  <- svc.get(id, customer).either
        dup      <- svc.create(CreateDesignCmd(id, device, "x", doc)).either
        badName  <- svc.save(SaveDesignCmd(id, device, 2, "  ", doc)).either
      yield
        assert(created.revision == 1 && saved.revision == 2 && saved.name == "My card v2")
        assert(stale == Left(DesignError.RevisionConflict(id, 1, 2)))
        assert(foreign == Left(DesignError.AccessDenied(id)))
        assert(dup == Left(DesignError.AlreadyExists(id)))
        assert(badName.isLeft)
    }

    "invalid documents are rejected with all errors" in {
      val broken = doc.copy(pages = doc.pages.map(p => p.copy(elements = p.elements.map(_.copy(opacity = 3)))))
      for
        repo <- inMemoryRepo
        res  <- DesignServiceLive.make(repo).create(CreateDesignCmd(DesignId.unsafe("d-x"), device, "x", broken)).either
      yield res match
        case Left(DesignError.InvalidDocument(errs)) => assert(errs.size >= 2)
        case other                                   => assert(false, other.toString)
    }

    "device designs are claimed by the customer, submitted designs are frozen and archived on delete" in {
      val id = DesignId.unsafe("d-2")
      for
        repo      <- inMemoryRepo
        svc        = DesignServiceLive.make(repo)
        _         <- svc.create(CreateDesignCmd(id, device, "Calendar", doc))
        moved     <- svc.claimDeviceDesigns(NonEmptyString.unsafe("device-1"), CustomerId.unsafe("cust-1"))
        mine      <- svc.listByOwner(customer)
        left      <- svc.listByOwner(device)
        submitted <- svc.markSubmitted(id, customer, ArtworkId.unsafe("art-9"))
        locked    <- svc.save(SaveDesignCmd(id, customer, submitted.revision, "edit", doc)).either
        _         <- svc.delete(id, customer)
        afterDel  <- svc.listByOwner(customer)
        prod      <- svc.getForProduction(id)
      yield
        assert(moved == 1 && mine.map(_.id) == List(id) && left.isEmpty)
        assert(locked == Left(DesignError.NotEditable(id)))
        assert(afterDel.isEmpty)
        assert(prod.status == DesignStatus.Archived)
    }
  }
