package mpbuilder.ui.visualeditor.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.visualeditor.*
import mpbuilder.ui.visualeditor.overlays.ProductOverlay
import org.scalajs.dom
import scala.scalajs.js

object EditorPageCanvas {
  def apply(): Element = {
    val currentPage = VisualEditorViewModel.currentPage
    val format = VisualEditorViewModel.productFormat

    div(
      cls := "calendar-canvas-container",
      styleAttr <-- format.map { fmt =>
        val maxW = if ProductFormat.isLandscape(fmt) then 800 else 600
        s"max-width: ${maxW}px;"
      },

      div(
        cls := "calendar-canvas",

        // Click on empty area to deselect
        onClick --> { ev =>
          if ev.target.asInstanceOf[dom.Element].classList.contains("calendar-page") ||
             ev.target.asInstanceOf[dom.Element].classList.contains("calendar-background") then
            VisualEditorViewModel.deselectElement()
        },

        // Render the page
        child <-- currentPage.combineWith(format, VisualEditorViewModel.productContext).map { (page, fmt, ctxOpt) =>
          renderPage(page, fmt, ctxOpt)
        }
      ),

      // Floating add buttons (top-right of canvas)
      div(
        cls := "canvas-float-buttons",

        // Hidden file input for photo upload
        input(
          typ := "file",
          accept := "image/*",
          idAttr := "canvas-photo-upload",
          display := "none",
          onChange --> { ev =>
            val inp = ev.target.asInstanceOf[dom.html.Input]
            val files = inp.files
            if files.length > 0 then
              val file = files(0)
              val reader = new dom.FileReader()
              reader.onload = { _ =>
                val imageData = reader.result.asInstanceOf[String]
                VisualEditorViewModel.uploadPhoto(imageData)
              }
              reader.readAsDataURL(file)
          }
        ),

        button(
          cls := "canvas-float-btn",
          title := "Add photo",
          "📷",
          onClick --> { _ =>
            dom.document.getElementById("canvas-photo-upload").asInstanceOf[dom.html.Input].click()
          }
        ),
        button(
          cls := "canvas-float-btn",
          title := "Add text",
          "T",
          onClick --> { _ => VisualEditorViewModel.addTextField() }
        ),
      ),
    )
  }

  private def renderPage(page: EditorPage, format: ProductFormat, productContext: Option[ProductContext]): Element = {
    // Scale to fit canvas: use aspect ratio from physical dimensions
    val aspectRatio = format.widthMm.toDouble / format.heightMm
    val canvasWidth = if ProductFormat.isLandscape(format) then 760 else 560
    val pageHeight = (canvasWidth / aspectRatio).toInt

    div(
      cls := "calendar-page",
      styleAttr := s"height: ${pageHeight}px;",

      // Background
      renderBackground(page.template.background),

      // Product overlay (wire binding, frame, stand, etc.)
      ProductOverlay.render(productContext, format),

      // Month title (locked template field) — only render if text is non-empty
      if page.template.monthField.text.nonEmpty then renderTemplateTextField(page.template.monthField) else emptyMod,

      // Days grid (locked template fields)
      page.template.daysGrid.map(dayField => renderTemplateTextField(dayField)),

      // All canvas elements (sorted by z-index)
      page.elements.sortBy(_.zIndex).map(renderCanvasElement)
    )
  }

  private def renderBackground(bg: PageBackground): Element = bg match {
    case PageBackground.SolidColor(color) =>
      div(
        cls := "calendar-background",
        styleAttr := s"background-color: $color;"
      )
    case PageBackground.BackgroundImage(imageData) =>
      div(
        cls := "calendar-background",
        styleAttr := s"background-image: url($imageData); background-size: cover; background-position: center;"
      )
  }

  private def renderTemplateTextField(field: TemplateTextField): Element = {
    div(
      cls := "calendar-text-field locked",
      styleAttr := s"position: absolute; left: ${field.position.x}px; top: ${field.position.y}px; font-size: ${field.fontSize}px; font-family: ${field.fontFamily}; color: ${field.color}; cursor: default; user-select: none;",
      field.text
    )
  }

  private def renderCanvasElement(elem: CanvasElement): Element = elem match {
    case photo: PhotoElement   => renderPhoto(photo)
    case text: TextElement     => renderTextElement(text)
    case shape: ShapeElement   => renderShapeElement(shape)
    case clip: ClipartElement  => renderClipartElement(clip)
  }

  // ─── Text Element ────────────────────────────────────────────────

  private def renderTextElement(field: TextElement): Element = {
    val selected = VisualEditorViewModel.selectedElement

    div(
      cls := "calendar-element calendar-text-element",
      cls <-- selected.map(sel => if sel.contains(field.id) then "selected" else ""),
      styleAttr := s"position: absolute; left: ${field.position.x}px; top: ${field.position.y}px; width: ${field.size.width}px; height: ${field.size.height}px; transform: rotate(${field.rotation}deg); transform-origin: center; z-index: ${field.zIndex};",

      // Inner text with styling (editable when selected)
      div(
        cls := "text-element-content",
        cls <-- selected.map(sel => if sel.contains(field.id) then "editable" else ""),
        styleAttr := s"font-size: ${field.fontSize}px; font-family: ${field.fontFamily}; color: ${field.color}; font-weight: ${if field.bold then "bold" else "normal"}; font-style: ${if field.italic then "italic" else "normal"}; text-align: ${alignToCss(field.textAlign)}; width: 100%; height: 100%; overflow: hidden; word-wrap: break-word; overflow-wrap: break-word;",
        contentEditable := true,
        field.text,
        onBlur --> { ev =>
          val newText = ev.target.asInstanceOf[dom.Element].textContent
          if newText != field.text then
            VisualEditorViewModel.updateTextFieldText(field.id, newText)
        },
      ),

      // Resize handles (when selected)
      child.maybe <-- selected.map { sel =>
        if sel.contains(field.id) then Some(renderResizeHandles(field.id, field)) else None
      },

      // Rotation handle (when selected)
      child.maybe <-- selected.map { sel =>
        if sel.contains(field.id) then Some(renderRotationHandle(field.id, field)) else None
      },

      // Drag handle (when selected)
      child.maybe <-- selected.map { sel =>
        if sel.contains(field.id) then Some(renderDragHandle(field.id, field.position)) else None
      },

      // Element toolbar (when selected)
      child.maybe <-- selected.map { sel =>
        if sel.contains(field.id) then Some(renderElementToolbar(field)) else None
      },

      // Click to select (drag is initiated from drag handle for text elements)
      onMouseDown --> { ev =>
        val target = ev.target.asInstanceOf[dom.Element]
        if !target.classList.contains("resize-handle") &&
           !target.classList.contains("rotate-handle") &&
           !target.classList.contains("drag-handle") &&
           !target.classList.contains("toolbar-btn") &&
           !target.classList.contains("toolbar-color") then
          ev.stopPropagation()
          VisualEditorViewModel.selectElement(field.id)
          // Only start drag if not clicking on editable text content
          val isTextContent = target.classList.contains("text-element-content") || target.closest(".text-element-content") != null
          if !isTextContent then
            ev.preventDefault()
            startDrag(field.id, field.position, ev)
      }
    )
  }

  // ─── Shape Element ───────────────────────────────────────────────

  private def renderShapeElement(shape: ShapeElement): Element = {
    val selected = VisualEditorViewModel.selectedElement

    val shapeContent = shape.shapeType match {
      case ShapeType.Line =>
        div(
          cls := "shape-line-inner",
          styleAttr := s"width: 100%; height: 0; border-top: ${shape.strokeWidth}px solid ${shape.strokeColor}; position: absolute; top: 50%; transform: translateY(-50%);"
        )
      case ShapeType.Rectangle =>
        div(
          cls := "shape-rect-inner",
          styleAttr := s"width: 100%; height: 100%; border: ${shape.strokeWidth}px solid ${shape.strokeColor}; background-color: ${shape.fillColor}; box-sizing: border-box;"
        )
    }

    div(
      cls := "calendar-element calendar-shape-element",
      cls <-- selected.map(sel => if sel.contains(shape.id) then "selected" else ""),
      styleAttr := s"position: absolute; left: ${shape.position.x}px; top: ${shape.position.y}px; width: ${shape.size.width}px; height: ${shape.size.height}px; transform: rotate(${shape.rotation}deg); transform-origin: center; z-index: ${shape.zIndex};",

      shapeContent,

      child.maybe <-- selected.map { sel =>
        if sel.contains(shape.id) then Some(renderResizeHandles(shape.id, shape)) else None
      },

      // Rotation handle (when selected)
      child.maybe <-- selected.map { sel =>
        if sel.contains(shape.id) then Some(renderRotationHandle(shape.id, shape)) else None
      },

      // Drag handle (when selected)
      child.maybe <-- selected.map { sel =>
        if sel.contains(shape.id) then Some(renderDragHandle(shape.id, shape.position)) else None
      },

      // Element toolbar (when selected)
      child.maybe <-- selected.map { sel =>
        if sel.contains(shape.id) then Some(renderElementToolbar(shape)) else None
      },

      onMouseDown --> { ev =>
        if !ev.target.asInstanceOf[dom.Element].classList.contains("resize-handle") &&
           !ev.target.asInstanceOf[dom.Element].classList.contains("rotate-handle") &&
           !ev.target.asInstanceOf[dom.Element].classList.contains("drag-handle") then
          ev.preventDefault()
          ev.stopPropagation()
          VisualEditorViewModel.selectElement(shape.id)
          startDrag(shape.id, shape.position, ev)
      }
    )
  }

  // ─── Clipart Element ─────────────────────────────────────────────

  private def renderClipartElement(clip: ClipartElement): Element = {
    val selected = VisualEditorViewModel.selectedElement

    div(
      cls := "calendar-element calendar-clipart-element",
      cls <-- selected.map(sel => if sel.contains(clip.id) then "selected" else ""),
      styleAttr := s"position: absolute; left: ${clip.position.x}px; top: ${clip.position.y}px; width: ${clip.size.width}px; height: ${clip.size.height}px; transform: rotate(${clip.rotation}deg); transform-origin: center; z-index: ${clip.zIndex};",

      img(
        src := clip.imageData,
        styleAttr := "width: 100%; height: 100%; object-fit: contain; pointer-events: none;",
        draggable := false,
      ),

      child.maybe <-- selected.map { sel =>
        if sel.contains(clip.id) then Some(renderResizeHandles(clip.id, clip)) else None
      },

      // Rotation handle (when selected)
      child.maybe <-- selected.map { sel =>
        if sel.contains(clip.id) then Some(renderRotationHandle(clip.id, clip)) else None
      },

      // Drag handle (when selected)
      child.maybe <-- selected.map { sel =>
        if sel.contains(clip.id) then Some(renderDragHandle(clip.id, clip.position)) else None
      },

      // Element toolbar (when selected)
      child.maybe <-- selected.map { sel =>
        if sel.contains(clip.id) then Some(renderElementToolbar(clip)) else None
      },

      onMouseDown --> { ev =>
        val target = ev.target.asInstanceOf[dom.Element]
        if !target.classList.contains("resize-handle") &&
           !target.classList.contains("rotate-handle") &&
           !target.classList.contains("drag-handle") then
          ev.preventDefault()
          ev.stopPropagation()
          VisualEditorViewModel.selectElement(clip.id)
          startDrag(clip.id, clip.position, ev)
      }
    )
  }

  // ─── Photo Element ───────────────────────────────────────────────

  private def renderPhoto(photo: PhotoElement): Element = {
    val selected = VisualEditorViewModel.selectedElement

    div(
      cls := "calendar-element calendar-photo",
      cls <-- selected.map(sel => if sel.contains(photo.id) then "selected" else ""),
      styleAttr := s"position: absolute; left: ${photo.position.x}px; top: ${photo.position.y}px; width: ${photo.size.width}px; height: ${photo.size.height}px; transform: rotate(${photo.rotation}deg); transform-origin: center; z-index: ${photo.zIndex};",

      div(
        cls := "photo-image-container",
        styleAttr := "width: 100%; height: 100%; overflow: hidden; position: relative;",

        if photo.imageData.nonEmpty then
          img(
            src := photo.imageData,
            styleAttr := s"width: 100%; height: 100%; object-fit: cover; pointer-events: none; transform: scale(${photo.imageScale}) translate(${photo.imageOffsetX}px, ${photo.imageOffsetY}px); transform-origin: center;",
            draggable := false
          )
        else
          div(
            cls := "photo-placeholder",
            styleAttr := "width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; background: #f0f0f0; border: 2px dashed #ccc; box-sizing: border-box; color: #999; font-size: 14px; text-align: center; pointer-events: none;",
            "Click to add photo"
          )
      ),

      child.maybe <-- selected.map { sel =>
        if sel.contains(photo.id) then Some(renderResizeHandles(photo.id, photo)) else None
      },

      // Rotation handle (when selected)
      child.maybe <-- selected.map { sel =>
        if sel.contains(photo.id) then Some(renderRotationHandle(photo.id, photo)) else None
      },

      // Drag handle (when selected)
      child.maybe <-- selected.map { sel =>
        if sel.contains(photo.id) then Some(renderDragHandle(photo.id, photo.position)) else None
      },

      // Element toolbar (when selected)
      child.maybe <-- selected.map { sel =>
        if sel.contains(photo.id) then Some(renderElementToolbar(photo)) else None
      },

      onMouseDown --> { ev =>
        val target = ev.target.asInstanceOf[dom.Element]
        if !target.classList.contains("resize-handle") &&
           !target.classList.contains("rotate-handle") &&
           !target.classList.contains("drag-handle") then
          ev.preventDefault()
          ev.stopPropagation()
          VisualEditorViewModel.selectElement(photo.id)
          startDrag(photo.id, photo.position, ev)
      }
    )
  }

  // ─── Generic drag helper ─────────────────────────────────────────

  private def startDrag(elementId: String, startPos: Position, ev: dom.MouseEvent): Unit = {
    val startX = ev.clientX
    val startY = ev.clientY

    var mouseUpHandlerOpt: Option[js.Function1[dom.MouseEvent, Unit]] = None

    val mouseMoveHandler: js.Function1[dom.MouseEvent, Unit] = { moveEv =>
      val deltaX = moveEv.clientX - startX
      val deltaY = moveEv.clientY - startY
      VisualEditorViewModel.updateElementPosition(
        elementId,
        Position(startPos.x + deltaX, startPos.y + deltaY)
      )
    }

    val mouseUpHandler: js.Function1[dom.MouseEvent, Unit] = { _ =>
      dom.window.removeEventListener("mousemove", mouseMoveHandler)
      mouseUpHandlerOpt.foreach(h => dom.window.removeEventListener("mouseup", h))
      VisualEditorViewModel.commitElementChange()
    }

    mouseUpHandlerOpt = Some(mouseUpHandler)

    dom.window.addEventListener("mousemove", mouseMoveHandler)
    dom.window.addEventListener("mouseup", mouseUpHandler)
  }

  // ─── Generic resize handles ──────────────────────────────────────

  private def renderResizeHandles(elementId: String, elem: CanvasElement): Element = {
    div(
      cls := "resize-handles",

      List("nw", "ne", "sw", "se").map { corner =>
        div(
          cls := s"resize-handle resize-handle-$corner",
          onMouseDown --> { ev =>
            ev.preventDefault()
            ev.stopPropagation()

            val startX = ev.clientX
            val startY = ev.clientY
            val startWidth = elem.size.width
            val startHeight = elem.size.height
            val startPosX = elem.position.x
            val startPosY = elem.position.y
            val aspectRatio = if startHeight != 0 then startWidth / startHeight else 1.0

            var mouseUpHandlerOpt: Option[js.Function1[dom.MouseEvent, Unit]] = None

            val mouseMoveHandler: js.Function1[dom.MouseEvent, Unit] = { moveEv =>
              val deltaX = moveEv.clientX - startX
              val deltaY = moveEv.clientY - startY

              corner match {
                case "se" =>
                  val newWidth = math.max(30, startWidth + deltaX)
                  val newHeight = newWidth / aspectRatio
                  VisualEditorViewModel.updateElementSize(elementId, Size(newWidth, newHeight))

                case "sw" =>
                  val newWidth = math.max(30, startWidth - deltaX)
                  val newHeight = newWidth / aspectRatio
                  VisualEditorViewModel.updateElementSize(elementId, Size(newWidth, newHeight))
                  VisualEditorViewModel.updateElementPosition(elementId, Position(startPosX + (startWidth - newWidth), startPosY))

                case "ne" =>
                  val newWidth = math.max(30, startWidth + deltaX)
                  val newHeight = newWidth / aspectRatio
                  VisualEditorViewModel.updateElementSize(elementId, Size(newWidth, newHeight))
                  VisualEditorViewModel.updateElementPosition(elementId, Position(startPosX, startPosY + (startHeight - newHeight)))

                case "nw" =>
                  val newWidth = math.max(30, startWidth - deltaX)
                  val newHeight = newWidth / aspectRatio
                  VisualEditorViewModel.updateElementSize(elementId, Size(newWidth, newHeight))
                  VisualEditorViewModel.updateElementPosition(elementId, Position(startPosX + (startWidth - newWidth), startPosY + (startHeight - newHeight)))

                case _ => ()
              }
            }

            val mouseUpHandler: js.Function1[dom.MouseEvent, Unit] = { _ =>
              dom.window.removeEventListener("mousemove", mouseMoveHandler)
              mouseUpHandlerOpt.foreach(h => dom.window.removeEventListener("mouseup", h))
              VisualEditorViewModel.commitElementChange()
            }

            mouseUpHandlerOpt = Some(mouseUpHandler)
            dom.window.addEventListener("mousemove", mouseMoveHandler)
            dom.window.addEventListener("mouseup", mouseUpHandler)
          }
        )
      },

      List("n", "s", "e", "w").map { side =>
        div(
          cls := s"resize-handle resize-handle-$side",
          onMouseDown --> { ev =>
            ev.preventDefault()
            ev.stopPropagation()

            val startX = ev.clientX
            val startY = ev.clientY
            val startWidth = elem.size.width
            val startHeight = elem.size.height
            val startPosX = elem.position.x
            val startPosY = elem.position.y

            var mouseUpHandlerOpt: Option[js.Function1[dom.MouseEvent, Unit]] = None

            val mouseMoveHandler: js.Function1[dom.MouseEvent, Unit] = { moveEv =>
              val deltaX = moveEv.clientX - startX
              val deltaY = moveEv.clientY - startY

              side match {
                case "e" =>
                  VisualEditorViewModel.updateElementSize(elementId, Size(math.max(30, startWidth + deltaX), startHeight))
                case "w" =>
                  val newWidth = math.max(30, startWidth - deltaX)
                  VisualEditorViewModel.updateElementSize(elementId, Size(newWidth, startHeight))
                  VisualEditorViewModel.updateElementPosition(elementId, Position(startPosX + (startWidth - newWidth), startPosY))
                case "s" =>
                  VisualEditorViewModel.updateElementSize(elementId, Size(startWidth, math.max(30, startHeight + deltaY)))
                case "n" =>
                  val newHeight = math.max(30, startHeight - deltaY)
                  VisualEditorViewModel.updateElementSize(elementId, Size(startWidth, newHeight))
                  VisualEditorViewModel.updateElementPosition(elementId, Position(startPosX, startPosY + (startHeight - newHeight)))
                case _ => ()
              }
            }

            val mouseUpHandler: js.Function1[dom.MouseEvent, Unit] = { _ =>
              dom.window.removeEventListener("mousemove", mouseMoveHandler)
              mouseUpHandlerOpt.foreach(h => dom.window.removeEventListener("mouseup", h))
              VisualEditorViewModel.commitElementChange()
            }

            mouseUpHandlerOpt = Some(mouseUpHandler)
            dom.window.addEventListener("mousemove", mouseMoveHandler)
            dom.window.addEventListener("mouseup", mouseUpHandler)
          }
        )
      }
    )
  }

  // ─── Rotation Handle (drag-to-rotate) ─────────────────────────

  private def renderRotationHandle(elementId: String, elem: CanvasElement): Element = {
    div(
      cls := "rotate-handle-container",
      // Connecting line from element to handle
      div(cls := "rotate-handle-line"),
      // Draggable rotation handle
      div(
        cls := "rotate-handle",
        "⟳",
        title := "Drag to rotate",
        onMouseDown --> { ev =>
          ev.preventDefault()
          ev.stopPropagation()

          // Get the element's center position for angle calculation
          val target = ev.target.asInstanceOf[dom.Element]
          val parent = target.closest(".calendar-element")
          if parent != null then
            val rect = parent.getBoundingClientRect()
            val centerX = rect.left + rect.width / 2
            val centerY = rect.top + rect.height / 2
            val startAngle = math.atan2(ev.clientY - centerY, ev.clientX - centerX) * 180 / math.Pi
            val startRotation = elem.rotation

            var mouseUpHandlerOpt: Option[js.Function1[dom.MouseEvent, Unit]] = None

            val mouseMoveHandler: js.Function1[dom.MouseEvent, Unit] = { moveEv =>
              val currentAngle = math.atan2(moveEv.clientY - centerY, moveEv.clientX - centerX) * 180 / math.Pi
              val delta = currentAngle - startAngle
              val newRotation = ((startRotation + delta) % 360 + 360) % 360
              VisualEditorViewModel.updateElementRotation(elementId, newRotation)
            }

            val mouseUpHandler: js.Function1[dom.MouseEvent, Unit] = { _ =>
              dom.window.removeEventListener("mousemove", mouseMoveHandler)
              mouseUpHandlerOpt.foreach(h => dom.window.removeEventListener("mouseup", h))
              VisualEditorViewModel.commitElementChange()
            }

            mouseUpHandlerOpt = Some(mouseUpHandler)
            dom.window.addEventListener("mousemove", mouseMoveHandler)
            dom.window.addEventListener("mouseup", mouseUpHandler)
        }
      )
    )
  }

  // ─── Drag Handle ────────────────────────────────────────────────

  private def renderDragHandle(elementId: String, startPos: Position): Element = {
    div(
      cls := "drag-handle",
      "✥",
      title := "Drag to move",
      onMouseDown --> { ev =>
        ev.preventDefault()
        ev.stopPropagation()
        startDrag(elementId, startPos, ev)
      }
    )
  }

  // ─── Element Toolbar ────────────────────────────────────────────

  private def renderElementToolbar(elem: CanvasElement): Element = {
    val toolbarContent = elem match {
      case text: TextElement     => renderTextToolbar(text)
      case photo: PhotoElement   => renderPhotoToolbar(photo)
      case shape: ShapeElement   => renderShapeToolbar(shape)
      case clip: ClipartElement  => renderClipartToolbar(clip)
    }

    div(
      cls := "element-toolbar",
      toolbarContent,
      renderLayerActions(elem.id),
    )
  }

  private def renderTextToolbar(text: TextElement): Element = {
    div(
      cls := "toolbar-group",
      button(cls := "toolbar-btn" + (if text.bold then " active" else ""), "B", title := "Bold",
        onClick --> { ev => ev.stopPropagation(); VisualEditorViewModel.updateTextFieldBold(text.id, !text.bold) }),
      button(cls := "toolbar-btn toolbar-italic" + (if text.italic then " active" else ""), "I", title := "Italic",
        onClick --> { ev => ev.stopPropagation(); VisualEditorViewModel.updateTextFieldItalic(text.id, !text.italic) }),
      span(cls := "toolbar-separator"),
      button(cls := "toolbar-btn" + (if text.textAlign == TextAlignment.Left then " active" else ""), "≡", title := "Align left",
        onClick --> { ev => ev.stopPropagation(); VisualEditorViewModel.updateTextFieldAlign(text.id, TextAlignment.Left) }),
      button(cls := "toolbar-btn" + (if text.textAlign == TextAlignment.Center then " active" else ""), "≡", title := "Align center",
        onClick --> { ev => ev.stopPropagation(); VisualEditorViewModel.updateTextFieldAlign(text.id, TextAlignment.Center) }),
      button(cls := "toolbar-btn" + (if text.textAlign == TextAlignment.Right then " active" else ""), "≡", title := "Align right",
        onClick --> { ev => ev.stopPropagation(); VisualEditorViewModel.updateTextFieldAlign(text.id, TextAlignment.Right) }),
      span(cls := "toolbar-separator"),
      input(typ := "color", cls := "toolbar-color", value := text.color,
        onInput.mapToValue --> { v => VisualEditorViewModel.updateTextFieldColor(text.id, v) }),
      button(cls := "toolbar-btn", "A-", title := "Decrease font size",
        onClick --> { ev => ev.stopPropagation(); VisualEditorViewModel.updateTextFieldFontSize(text.id, math.max(8, text.fontSize - 2)) }),
      button(cls := "toolbar-btn", "A+", title := "Increase font size",
        onClick --> { ev => ev.stopPropagation(); VisualEditorViewModel.updateTextFieldFontSize(text.id, math.min(72, text.fontSize + 2)) }),
    )
  }

  private def renderPhotoToolbar(photo: PhotoElement): Element = {
    div(
      cls := "toolbar-group",

      // Hidden file input for replace
      input(
        typ := "file",
        accept := "image/*",
        idAttr := s"toolbar-replace-${photo.id}",
        display := "none",
        onChange --> { ev =>
          val inp = ev.target.asInstanceOf[dom.html.Input]
          val files = inp.files
          if files.length > 0 then
            val file = files(0)
            val reader = new dom.FileReader()
            reader.onload = { _ =>
              val imageData = reader.result.asInstanceOf[String]
              VisualEditorViewModel.replacePhotoImage(photo.id, imageData)
            }
            reader.readAsDataURL(file)
        }
      ),

      button(cls := "toolbar-btn", "📷", title := "Replace image",
        onClick --> { ev => ev.stopPropagation(); dom.document.getElementById(s"toolbar-replace-${photo.id}").asInstanceOf[dom.html.Input].click() }),
      button(cls := "toolbar-btn", "✕", title := "Clear image",
        onClick --> { ev => ev.stopPropagation(); VisualEditorViewModel.clearPhotoImage(photo.id) }),
      span(cls := "toolbar-separator"),
      button(cls := "toolbar-btn", "🔍-", title := "Zoom out",
        onClick --> { ev => ev.stopPropagation(); VisualEditorViewModel.updatePhotoImageScale(photo.id, photo.imageScale - 0.1) }),
      button(cls := "toolbar-btn", "🔍+", title := "Zoom in",
        onClick --> { ev => ev.stopPropagation(); VisualEditorViewModel.updatePhotoImageScale(photo.id, photo.imageScale + 0.1) }),
      span(cls := "toolbar-separator"),
      button(cls := "toolbar-btn", "🔄", title := "Flip horizontal",
        onClick --> { ev => ev.stopPropagation(); VisualEditorViewModel.updatePhotoImageOffset(photo.id, -photo.imageOffsetX, photo.imageOffsetY) }),
    )
  }

  private def renderShapeToolbar(shape: ShapeElement): Element = {
    div(
      cls := "toolbar-group",
      input(typ := "color", cls := "toolbar-color", value := shape.strokeColor,
        onInput.mapToValue --> { v => VisualEditorViewModel.updateShape(shape.id, _.copy(strokeColor = v)) }),
    )
  }

  private def renderClipartToolbar(clip: ClipartElement): Element = {
    div(cls := "toolbar-group") // Layer actions only
  }

  private def renderLayerActions(elementId: String): Element = {
    div(
      cls := "toolbar-group toolbar-layer-actions",
      button(cls := "toolbar-btn", "↑", title := "Bring forward",
        onClick --> { ev => ev.stopPropagation(); VisualEditorViewModel.bringToFront(elementId) }),
      button(cls := "toolbar-btn", "↓", title := "Send back",
        onClick --> { ev => ev.stopPropagation(); VisualEditorViewModel.sendToBack(elementId) }),
      button(cls := "toolbar-btn", "⎘", title := "Duplicate",
        onClick --> { ev => ev.stopPropagation(); VisualEditorViewModel.duplicateElement(elementId) }),
      button(cls := "toolbar-btn toolbar-delete-btn", "🗑", title := "Delete",
        onClick --> { ev => ev.stopPropagation(); VisualEditorViewModel.removeElement(elementId) }),
    )
  }

  private def alignToCss(align: TextAlignment): String = align match {
    case TextAlignment.Left   => "left"
    case TextAlignment.Center => "center"
    case TextAlignment.Right  => "right"
  }
}
