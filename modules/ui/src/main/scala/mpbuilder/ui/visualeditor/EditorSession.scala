package mpbuilder.ui.visualeditor

/** Contextual product information passed from the Product Builder to the Visual Editor.
  * Uses plain types (not domain opaques) to keep the editor decoupled from the domain model.
  */
case class ProductContext(
  widthMm: Double,
  heightMm: Double,
  pageCount: Option[Int],
  categoryId: Option[String],
  categoryName: Option[String],
  bindingMethod: Option[String],
  visualProductType: Option[VisualProductType],
)

/** A saved editor session — persisted to IndexedDB */
case class EditorSession(
  id: String,
  title: String,
  configurationId: Option[String],
  productContext: Option[ProductContext],
  editorState: EditorState,
  createdAt: Double,
  updatedAt: Double,
  thumbnailDataUrl: Option[String],
)

/** A reference to an image used across editor sessions — persisted to IndexedDB */
case class ImageReference(
  id: String,
  dataUrl: String,
  fileName: Option[String],
  addedAt: Double,
  sizeBytes: Long,
  usedInSessions: Set[String],
)
