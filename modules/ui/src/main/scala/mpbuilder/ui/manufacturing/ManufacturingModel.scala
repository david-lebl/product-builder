package mpbuilder.ui.manufacturing

import mpbuilder.domain.model.Language

// ── Station types ──────────────────────────────────────────────────

enum StationType:
  case DigitalPrinting
  case OffsetPrinting
  case LargeFormat
  case Cutting
  case Lamination
  case Folding
  case Binding
  case QualityControl
  case Packaging

  def label(lang: Language): String = (this, lang) match
    case (DigitalPrinting, Language.En) => "Digital Printing"
    case (DigitalPrinting, Language.Cs) => "Digitální tisk"
    case (OffsetPrinting, Language.En)  => "Offset Printing"
    case (OffsetPrinting, Language.Cs)  => "Ofsetový tisk"
    case (LargeFormat, Language.En)     => "Large Format"
    case (LargeFormat, Language.Cs)     => "Velkoplošný tisk"
    case (Cutting, Language.En)         => "Cutting"
    case (Cutting, Language.Cs)         => "Řezání"
    case (Lamination, Language.En)      => "Lamination"
    case (Lamination, Language.Cs)      => "Laminace"
    case (Folding, Language.En)         => "Folding"
    case (Folding, Language.Cs)         => "Skládání"
    case (Binding, Language.En)         => "Binding"
    case (Binding, Language.Cs)         => "Vazba"
    case (QualityControl, Language.En)  => "Quality Control"
    case (QualityControl, Language.Cs)  => "Kontrola kvality"
    case (Packaging, Language.En)       => "Packaging"
    case (Packaging, Language.Cs)       => "Balení"

  def icon: String = this match
    case DigitalPrinting => "🖨️"
    case OffsetPrinting  => "🏭"
    case LargeFormat     => "📐"
    case Cutting         => "✂️"
    case Lamination      => "🛡️"
    case Folding         => "📄"
    case Binding         => "📚"
    case QualityControl  => "✅"
    case Packaging       => "📦"

// ── Order status ───────────────────────────────────────────────────

enum OrderStatus:
  case Pending
  case InProgress
  case AtStation
  case Completed
  case Cancelled

  def label(lang: Language): String = (this, lang) match
    case (Pending, Language.En)    => "Pending"
    case (Pending, Language.Cs)    => "Čeká"
    case (InProgress, Language.En) => "In Progress"
    case (InProgress, Language.Cs) => "Probíhá"
    case (AtStation, Language.En)  => "At Station"
    case (AtStation, Language.Cs)  => "Na stanici"
    case (Completed, Language.En)  => "Completed"
    case (Completed, Language.Cs)  => "Dokončeno"
    case (Cancelled, Language.En)  => "Cancelled"
    case (Cancelled, Language.Cs)  => "Zrušeno"

  def cssClass: String = this match
    case Pending    => "status-pending"
    case InProgress => "status-in-progress"
    case AtStation  => "status-at-station"
    case Completed  => "status-completed"
    case Cancelled  => "status-cancelled"

// ── Order priority ─────────────────────────────────────────────────

enum OrderPriority:
  case Low
  case Normal
  case High
  case Urgent

  def label(lang: Language): String = (this, lang) match
    case (Low, Language.En)    => "Low"
    case (Low, Language.Cs)    => "Nízká"
    case (Normal, Language.En) => "Normal"
    case (Normal, Language.Cs) => "Normální"
    case (High, Language.En)   => "High"
    case (High, Language.Cs)   => "Vysoká"
    case (Urgent, Language.En) => "Urgent"
    case (Urgent, Language.Cs) => "Urgentní"

  def cssClass: String = this match
    case Low    => "priority-low"
    case Normal => "priority-normal"
    case High   => "priority-high"
    case Urgent => "priority-urgent"

// ── Manufacturing station ──────────────────────────────────────────

case class ManufacturingStation(
  id: String,
  name: String,
  stationType: StationType,
  currentOrderId: Option[String] = None,
  isActive: Boolean = true,
)

// ── Attached file (placeholder for future real file uploads) ───────

case class AttachedFile(
  name: String,
  fileType: String, // e.g. "pdf", "jpg", "ai"
  sizeKb: Int,
  uploadedAt: Long, // epoch millis
)

// ── Manufacturing order ────────────────────────────────────────────

case class ManufacturingOrder(
  id: String,
  customerName: String,
  productDescription: String,
  quantity: Int,
  status: OrderStatus,
  priority: OrderPriority,
  currentStationId: Option[String] = None,
  requiredStationTypes: List[StationType],
  completedStationTypes: List[StationType] = List.empty,
  createdAt: Long,    // epoch millis
  updatedAt: Long,    // epoch millis
  notes: String = "",
  attachedFiles: List[AttachedFile] = List.empty,
)

// ── Manufacturing sidebar route ────────────────────────────────────

enum ManufacturingRoute:
  case Dashboard
  case WorkQueue
  case Stations
  case Employees
  case MaterialStorage
  case PreStorage
  case Delivery

  def label(lang: Language): String = (this, lang) match
    case (Dashboard, Language.En)       => "Dashboard"
    case (Dashboard, Language.Cs)       => "Přehled"
    case (WorkQueue, Language.En)       => "Work Queue"
    case (WorkQueue, Language.Cs)       => "Pracovní fronta"
    case (Stations, Language.En)        => "Stations"
    case (Stations, Language.Cs)        => "Stanice"
    case (Employees, Language.En)       => "Employees"
    case (Employees, Language.Cs)       => "Zaměstnanci"
    case (MaterialStorage, Language.En) => "Material Storage"
    case (MaterialStorage, Language.Cs) => "Sklad materiálu"
    case (PreStorage, Language.En)      => "Pre-storage"
    case (PreStorage, Language.Cs)      => "Předsklad"
    case (Delivery, Language.En)        => "Delivery"
    case (Delivery, Language.Cs)        => "Doručení"

  def icon: String = this match
    case Dashboard       => "📊"
    case WorkQueue       => "📋"
    case Stations        => "🏭"
    case Employees       => "👥"
    case MaterialStorage => "📦"
    case PreStorage      => "🏪"
    case Delivery        => "🚚"

  /** Whether this route is implemented (vs. future placeholder) */
  def isAvailable: Boolean = this match
    case Dashboard => true
    case WorkQueue => true
    case Stations  => true
    case _         => false

// ── Manufacturing state ────────────────────────────────────────────

case class ManufacturingState(
  orders: List[ManufacturingOrder],
  stations: List[ManufacturingStation],
  currentRoute: ManufacturingRoute,
  selectedOrderId: Option[String] = None,
  stationFilter: Set[StationType] = Set.empty,
)

object ManufacturingState:

  /** Create initial state with sample stations */
  def initial: ManufacturingState = ManufacturingState(
    orders = List.empty,
    stations = defaultStations,
    currentRoute = ManufacturingRoute.Dashboard,
  )

  private val defaultStations: List[ManufacturingStation] = List(
    ManufacturingStation("st-1", "Digital Press A", StationType.DigitalPrinting),
    ManufacturingStation("st-2", "Digital Press B", StationType.DigitalPrinting),
    ManufacturingStation("st-3", "Offset Press 1", StationType.OffsetPrinting),
    ManufacturingStation("st-4", "Large Format Printer", StationType.LargeFormat),
    ManufacturingStation("st-5", "Cutter 1", StationType.Cutting),
    ManufacturingStation("st-6", "Laminator", StationType.Lamination),
    ManufacturingStation("st-7", "Folder", StationType.Folding),
    ManufacturingStation("st-8", "Binder", StationType.Binding),
    ManufacturingStation("st-9", "QC Station", StationType.QualityControl),
    ManufacturingStation("st-10", "Packing Station", StationType.Packaging),
  )
