package mpbuilder.ui.calendar

/** Save status for the current editor session */
enum SaveStatus:
  case Unsaved
  case Saving
  case Saved(timestamp: Double)

/** Metadata about the current session (kept separate from CalendarState) */
case class SessionMeta(
  id: String,
  name: String,
  createdAt: Double,
)

/** Full persisted editor session */
case class EditorSession(
  id: String,
  name: String,
  productType: VisualProductType,
  productFormat: ProductFormat,
  pages: List[CalendarPage],
  createdAt: Double,
  updatedAt: Double,
)

/** Lightweight summary for session listing (no page data) */
case class SessionSummary(
  id: String,
  name: String,
  productType: VisualProductType,
  productFormat: ProductFormat,
  pageCount: Int,
  updatedAt: Double,
)

object EditorSession:
  def fromState(state: CalendarState, meta: SessionMeta): EditorSession =
    EditorSession(
      id = meta.id,
      name = meta.name,
      productType = state.productType,
      productFormat = state.productFormat,
      pages = state.pages,
      createdAt = meta.createdAt,
      updatedAt = System.currentTimeMillis().toDouble,
    )

  def toCalendarState(session: EditorSession): CalendarState =
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
      pageCount = session.pages.length,
      updatedAt = session.updatedAt,
    )
