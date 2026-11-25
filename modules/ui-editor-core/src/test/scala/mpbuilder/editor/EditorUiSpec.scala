package mpbuilder.editor

import kyo.*
import kyo.test.*
import mpbuilder.design.*
import mpbuilder.editor.i18n.EditorTranslations
import mpbuilder.editor.theme.EditorTheme
import mpbuilder.shared.*
import mpbuilder.ui.i18n.AppI18n

class EditorUiSpec extends Test[Any]:

  private def boot(launch: EditorLaunch = EditorLaunch(), lang: Language = Language.En): (EditorController, AppI18n, DesignStore) < Async =
    for
      i18n  <- EditorTranslations.init(lang)
      store <- InMemoryDesignStore.make
      ctrl  <- I18n.let(i18n.handle)(EditorController.init(i18n, EditorHost("device-test", store), launch))
    yield (ctrl, i18n, store)

  private def html(ui: UI, i18n: AppI18n): String < Async =
    I18n.let(i18n.handle)(UI.runRender(ui).take(1).run.map(_.headMaybe.getOrElse("")))

  private def noMissingKeys(s: String): Boolean = !s.contains("‹") && !s.contains("›")

  "Visual Editor" - {
    "home screen lists templates and renders cleanly in EN and CS" in {
      for
        (ctrl, i18n, _) <- boot()
        en <- html(EditorApp.render(ctrl), i18n)
        _  <- i18n.setLanguage(Language.Cs)
        cs <- html(EditorApp.render(ctrl), i18n)
      yield
        assert(en.contains("Design Studio") && en.contains("Start a new design"))
        assert(cs.contains("Návrhové studio") && cs.contains("Nový návrh"))
        assert(en.contains("<svg")) // live template previews
        assert(noMissingKeys(en) && noMissingKeys(cs))
    }

    "launching for a configured product creates a design in the product's format and autosaves it" in {
      val fmt    = DesignFormat(90, 50, bleedMm = 2)
      val launch = EditorLaunch(Some(ProductBinding(DesignProductKind.BusinessCard, Some(CategoryId.unsafe("cat-business-cards")))), Some(fmt), Some(2), title = Some("Business Cards"))
      for
        (ctrl, i18n, store) <- boot(launch)
        _      <- I18n.let(i18n.handle)(ctrl.createFromTemplate(Some("bc-classic"), DesignProductKind.BusinessCard))
        doc    <- ctrl.doc.get
        screen <- ctrl.screen.get
        saved  <- store.list
        ui     <- html(EditorApp.render(ctrl), i18n)
      yield
        assert(screen == EditorScreen.Editing)
        assert(doc.format == fmt && doc.pages.size == 2)
        assert(saved.size == 1 && saved.head.project.name == "Business Cards")
        assert(ui.contains("data-ve-canvas") && ui.contains("data-ve-el=") && ui.contains("ve-overlay"))
        assert(noMissingKeys(ui))
    }

    "adding, editing, undo and redo work as single history steps" in {
      for
        (ctrl, i18n, _) <- boot()
        _       <- I18n.let(i18n.handle)(ctrl.createFromTemplate(None, DesignProductKind.Poster))
        _       <- ctrl.addText(TextPreset.Heading)
        sel     <- ctrl.selection.get
        _       <- ctrl.updateSelectedContent({ case t: ElementContent.Text => t.copy(text = "Hello"); case o => o }, Some("text"))
        _       <- ctrl.updateSelectedContent({ case t: ElementContent.Text => t.copy(text = "Hello world"); case o => o }, Some("text"))
        after   <- ctrl.doc.get
        _       <- ctrl.undo
        undone  <- ctrl.doc.get
        _       <- ctrl.undo
        empty   <- ctrl.doc.get
        _       <- ctrl.redo
        redone  <- ctrl.doc.get
      yield
        assert(sel.size == 1)
        val texts = (d: DesignDocument) => d.allElements.collect { case DesignElement(_, _, ElementContent.Text(t, _), _, _, _) => t }
        assert(texts(after) == Vector("Hello world"))
        assert(texts(undone) == Vector("Heading")) // both keystrokes coalesced into one step
        assert(texts(empty).isEmpty)
        assert(texts(redone) == Vector("Heading"))
    }

    "pointer gestures move with snapping, resize via handles and commit once" in {
      for
        (ctrl, i18n, _) <- boot()
        _      <- I18n.let(i18n.handle)(ctrl.createFromTemplate(None, DesignProductKind.Flyer))
        _      <- ctrl.addShape(ShapeKind.Rectangle)
        id     <- ctrl.selection.get.map(_.head)
        start  <- ctrl.selectedElements.map(_.head.frame).current
        _      <- ctrl.pointerDown(CanvasTarget.Element(id), Point(start.centerX, start.centerY), shift = false)
        prev   <- ctrl.pointerMove(Point(start.centerX + 10.3, start.centerY), shift = false, alt = true)
        hist0  <- ctrl.history.get
        _      <- ctrl.pointerUp(Point(start.centerX + 10.3, start.centerY), shift = false, alt = true)
        moved  <- ctrl.selectedElements.map(_.head.frame).current
        hist1  <- ctrl.history.get
        _      <- ctrl.pointerDown(CanvasTarget.ResizeHandle(Handle.SE), Point(moved.x + moved.width, moved.y + moved.height), shift = false)
        _      <- ctrl.pointerUp(Point(moved.x + moved.width + 5, moved.y + moved.height + 7), shift = false, alt = false)
        sized  <- ctrl.selectedElements.map(_.head.frame).current
        _      <- ctrl.pointerDown(CanvasTarget.Background, Point(1, 1), shift = false)
        sel    <- ctrl.selection.get
      yield
        assert(prev.size == 1 && math.abs(prev.head.current.x - (start.x + 10.3)) < 0.011)
        assert(hist0.past.size == hist1.past.size - 1) // nothing committed until pointer up
        assert(math.abs(moved.x - (start.x + 10.3)) < 0.011)
        assert(math.abs(sized.width - (moved.width + 5)) < 0.01 && math.abs(sized.height - (moved.height + 7)) < 0.01)
        assert(sel.isEmpty)
    }

    "photos land in empty slots, drop points pick the slot under the pointer" in {
      val asset = ImageAsset(AssetId.unsafe("img-a"), "a.jpg", "image/jpeg", 3000, 2000, 1000)
      for
        (ctrl, i18n, _) <- boot()
        _   <- I18n.let(i18n.handle)(ctrl.createFromTemplate(Some("photobook-classic"), DesignProductKind.PhotoBook))
        d0  <- ctrl.doc.get
        _   <- ctrl.selectPage(d0.pages(3).id) // Grid4 page
        slots = d0.pages(3).elements.filter(_.isImage)
        target = slots(3)
        _   <- ctrl.addPhoto(asset, "blob:a", Some(target.frame.center))
        d1  <- ctrl.doc.get
        urls <- ctrl.assetUrls.get
        canvas <- html(EditorApp.render(ctrl), i18n)
      yield
        assert(d1.page(d0.pages(3).id).get.element(target.id).exists {
          case DesignElement(_, _, ElementContent.Image(Some(a), _, _, _, _), _, _, _) => a == asset.id
          case _                                                                         => false
        })
        assert(urls.get(asset.id).contains("blob:a"))
        assert(canvas.contains("blob:a"))
    }

    "keyboard shortcuts map to commands" in {
      assert(EditorShortcuts.resolve("z", ctrlOrMeta = true, shift = false, alt = false).contains(EditorCommand.Undo))
      assert(EditorShortcuts.resolve("Z", ctrlOrMeta = true, shift = true, alt = false).contains(EditorCommand.Redo))
      assert(EditorShortcuts.resolve("ArrowLeft", ctrlOrMeta = false, shift = true, alt = false).contains(EditorCommand.Nudge(-10, 0)))
      assert(EditorShortcuts.resolve("Delete", ctrlOrMeta = false, shift = false, alt = false).contains(EditorCommand.Delete))
      assert(EditorShortcuts.resolve("q", ctrlOrMeta = false, shift = false, alt = false).isEmpty)
    }

    "completing hands the host a proof and preflight summary" in {
      for
        i18n   <- EditorTranslations.init(Language.Cs)
        store  <- InMemoryDesignStore.make
        result <- AtomicRef.init[Option[EditorResult]](None)
        host    = EditorHost("dev", store, onComplete = Some(r => result.set(Some(r))))
        ctrl   <- I18n.let(i18n.handle)(EditorController.init(i18n, host, EditorLaunch(product = Some(ProductBinding(DesignProductKind.WallCalendar)))))
        _      <- I18n.let(i18n.handle)(ctrl.createFromTemplate(Some("wall-calendar-photo"), DesignProductKind.WallCalendar))
        _      <- ctrl.openReview
        review <- html(EditorApp.render(ctrl), i18n)
        _      <- ctrl.complete
        res    <- result.get
      yield
        assert(review.contains("Zkontrolujte svůj návrh") && noMissingKeys(review))
        assert(res.exists(r => r.proofDataUrl.startsWith("data:image/svg+xml;base64,") && r.project.document.pages.size == 13))
        assert(res.exists(_.issues.exists(_.kind == IssueKind.EmptyPhotoSlot)))
    }

    "reopening a stored design restores it, inspector renders for every element kind" in {
      for
        (ctrl, i18n, store) <- boot()
        _    <- I18n.let(i18n.handle)(ctrl.createFromTemplate(Some("desk-calendar"), DesignProductKind.DeskCalendar))
        _    <- ctrl.addClipart(ClipartKind.Heart)
        _    <- ctrl.addShape(ShapeKind.Ellipse)
        _    <- ctrl.saveNow
        id   <- ctrl.session.get.map(_.get.id)
        d    <- ctrl.doc.get
        stored <- store.load(id)
        renders <- Kyo.foreach(d.pages(1).elements.toList ++ d.pages.head.elements.toList) { el =>
                     ctrl.selectPage(d.pages.find(_.element(el.id).isDefined).get.id).andThen(ctrl.select(List(el.id))).andThen(html(EditorApp.render(ctrl), i18n))
                   }
      yield
        assert(stored.exists(_.project.document == d))
        assert(renders.forall(noMissingKeys))
        assert(renders.exists(_.contains("ve-textarea")) || renders.exists(_.contains("Month grid")))
    }

    "editor stylesheet covers the workspace and responsive breakpoints" in {
      val css = EditorTheme.css
      assert(css.contains(".ve-canvas") && css.contains(".ve-handle-rotate") && css.contains("@media (max-width:640px)"))
    }
  }
