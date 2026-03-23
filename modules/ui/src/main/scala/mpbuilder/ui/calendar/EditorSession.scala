package mpbuilder.ui.calendar

/** A named editor session persisted to browser localStorage */
case class EditorSession(
  id: String,
  name: String,
  productType: VisualProductType,
  productFormat: ProductFormat,
  pages: List[CalendarPage],
  imageReferences: Set[String] = Set.empty,
  linkedConfigurationId: Option[String] = None,
  createdAt: Double,
  updatedAt: Double,
)

/** Lightweight projection of a session (no heavy page data) */
case class SessionSummary(
  id: String,
  name: String,
  productType: VisualProductType,
  productFormat: ProductFormat,
  pageCount: Int,
  elementCount: Int,
  linkedConfigurationId: Option[String],
  createdAt: Double,
  updatedAt: Double,
)

object EditorSession:
  def fromState(
    id: String,
    name: String,
    state: CalendarState,
    linkedConfigurationId: Option[String] = None,
    createdAt: Double = System.currentTimeMillis().toDouble,
  ): EditorSession =
    EditorSession(
      id = id,
      name = name,
      productType = state.productType,
      productFormat = state.productFormat,
      pages = state.pages,
      imageReferences = collectImageReferences(state.pages),
      linkedConfigurationId = linkedConfigurationId,
      createdAt = createdAt,
      updatedAt = System.currentTimeMillis().toDouble,
    )

  def toState(session: EditorSession): CalendarState =
    CalendarState(
      pages = session.pages,
      currentPageIndex = 0,
      productType = session.productType,
      productFormat = session.productFormat,
    )

  def toSummary(session: EditorSession): SessionSummary =
    SessionSummary(
      id = session.id,
      name = session.name,
      productType = session.productType,
      productFormat = session.productFormat,
      pageCount = session.pages.size,
      elementCount = session.pages.map(_.elements.size).sum,
      linkedConfigurationId = session.linkedConfigurationId,
      createdAt = session.createdAt,
      updatedAt = session.updatedAt,
    )

  /** Collect all image data references from session pages */
  private def collectImageReferences(pages: List[CalendarPage]): Set[String] =
    pages.flatMap(_.elements).collect {
      case p: PhotoElement if p.imageData.nonEmpty   => p.imageData.hashCode.toString
      case c: ClipartElement if c.imageData.nonEmpty  => c.imageData.hashCode.toString
    }.toSet
