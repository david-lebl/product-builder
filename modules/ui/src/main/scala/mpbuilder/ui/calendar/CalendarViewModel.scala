package mpbuilder.ui.calendar

import com.raquo.laminar.api.L.*
import org.scalajs.dom

/** View model for managing calendar state */
object CalendarViewModel {
  
  // Main state
  private val stateVar: Var[CalendarState] = Var(CalendarState.empty)
  val state: Signal[CalendarState] = stateVar.signal
  
  // Currently selected element (for editing)
  private val selectedElementVar: Var[Option[String]] = Var(None)
  val selectedElement: Signal[Option[String]] = selectedElementVar.signal
  
  // Currently selected photo ID (for multi-photo support)
  private val selectedPhotoVar: Var[Option[String]] = Var(None)
  val selectedPhoto: Signal[Option[String]] = selectedPhotoVar.signal
  
  // Photo editor state (when cropping/resizing)
  private val photoEditorOpenVar: Var[Boolean] = Var(false)
  val photoEditorOpen: Signal[Boolean] = photoEditorOpenVar.signal
  
  // ID generation counter to avoid collisions
  private var idCounter: Int = 0
  
  private def generateId(prefix: String): String = {
    idCounter += 1
    s"$prefix-${System.currentTimeMillis()}-$idCounter"
  }
  
  // Current page as signal
  def currentPage: Signal[CalendarPage] = state.map(_.currentPage)
  
  def currentPageIndex: Signal[Int] = state.map(_.currentPageIndex)
  
  // Navigation
  def goToNextPage(): Unit = {
    stateVar.update(_.goToNext)
  }
  
  def goToPreviousPage(): Unit = {
    stateVar.update(_.goToPrevious)
  }
  
  def goToPage(index: Int): Unit = {
    stateVar.update(_.goToPage(index))
  }
  
  // Photo operations
  def uploadPhoto(imageData: String): Unit = {
    val photoId = generateId("photo")
    val photo = PhotoElement(
      id = photoId,
      imageData = imageData,
      position = Position(100, 100),
      size = Size(300, 200),
    )
    
    stateVar.update(s =>
      s.updateCurrentPage(page => page.copy(photos = page.photos :+ photo))
    )
    
    // Select the newly added photo
    selectedPhotoVar.set(Some(photoId))
  }
  
  def removePhoto(photoId: String): Unit = {
    stateVar.update(s =>
      s.updateCurrentPage(page => page.copy(photos = page.photos.filterNot(_.id == photoId)))
    )
    
    // Deselect if this was the selected photo
    if selectedPhotoVar.now().contains(photoId) then
      selectedPhotoVar.set(None)
  }
  
  def updatePhotoPosition(photoId: String, newPosition: Position): Unit = {
    stateVar.update(s =>
      s.updateCurrentPage(page =>
        page.copy(photos = page.photos.map { p =>
          if p.id == photoId then p.copy(position = newPosition) else p
        })
      )
    )
  }
  
  def updatePhotoSize(photoId: String, newSize: Size): Unit = {
    stateVar.update(s =>
      s.updateCurrentPage(page =>
        page.copy(photos = page.photos.map { p =>
          if p.id == photoId then p.copy(size = newSize) else p
        })
      )
    )
  }
  
  def updatePhotoRotation(photoId: String, rotation: Double): Unit = {
    stateVar.update(s =>
      s.updateCurrentPage(page =>
        page.copy(photos = page.photos.map { p =>
          if p.id == photoId then p.copy(rotation = rotation) else p
        })
      )
    )
  }
  
  def selectPhoto(photoId: String): Unit = {
    selectedPhotoVar.set(Some(photoId))
  }
  
  def deselectPhoto(): Unit = {
    selectedPhotoVar.set(None)
  }
  
  // Text field operations
  def addTextField(): Unit = {
    val textId = generateId("text")
    val textField = TextField(
      id = textId,
      text = "New Text",
      position = Position(200, 200),
      fontSize = 16,
      fontFamily = "Arial",
      color = "#000000",
      locked = false
    )
    
    stateVar.update(s =>
      s.updateCurrentPage(page =>
        page.copy(customTextFields = page.customTextFields :+ textField)
      )
    )
    
    // Select the newly added text field
    selectedElementVar.set(Some(textId))
  }
  
  def removeTextField(textId: String): Unit = {
    stateVar.update(s =>
      s.updateCurrentPage(page =>
        page.copy(customTextFields = page.customTextFields.filterNot(_.id == textId))
      )
    )
    
    // Deselect if this was the selected element
    if selectedElementVar.now().contains(textId) then
      selectedElementVar.set(None)
  }
  
  def updateTextField(textId: String, updater: TextField => TextField): Unit = {
    stateVar.update(s =>
      s.updateCurrentPage(page =>
        page.copy(customTextFields = page.customTextFields.map { field =>
          if field.id == textId then updater(field) else field
        })
      )
    )
  }
  
  def updateTextFieldText(textId: String, newText: String): Unit = {
    updateTextField(textId, _.copy(text = newText))
  }
  
  def updateTextFieldPosition(textId: String, newPosition: Position): Unit = {
    updateTextField(textId, _.copy(position = newPosition))
  }
  
  def updateTextFieldFontSize(textId: String, newSize: Int): Unit = {
    updateTextField(textId, _.copy(fontSize = newSize))
  }
  
  def updateTextFieldColor(textId: String, newColor: String): Unit = {
    updateTextField(textId, _.copy(color = newColor))
  }
  
  // Selection
  def selectElement(elementId: String): Unit = {
    selectedElementVar.set(Some(elementId))
  }
  
  def deselectElement(): Unit = {
    selectedElementVar.set(None)
  }
  
  // Photo editor
  def openPhotoEditor(): Unit = {
    photoEditorOpenVar.set(true)
  }
  
  def closePhotoEditor(): Unit = {
    photoEditorOpenVar.set(false)
  }
  
  // Reset to initial state
  def reset(): Unit = {
    stateVar.set(CalendarState.empty)
    selectedElementVar.set(None)
    selectedPhotoVar.set(None)
    photoEditorOpenVar.set(false)
  }
  
  // Update language for month names
  def updateLanguage(lang: String): Unit = {
    stateVar.update(state => CalendarState.updateLanguage(state, lang))
  }
}
