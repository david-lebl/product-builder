package mpbuilder.design

/** Resize handle positions on a selected element. */
enum Handle derives CanEqual:
  case N, NE, E, SE, S, SW, W, NW

  def movesLeft: Boolean   = this == W || this == NW || this == SW
  def movesRight: Boolean  = this == E || this == NE || this == SE
  def movesTop: Boolean    = this == N || this == NE || this == NW
  def movesBottom: Boolean = this == S || this == SE || this == SW
  def isCorner: Boolean    = (movesLeft || movesRight) && (movesTop || movesBottom)

object Handle:
  def fromCode(code: String): Option[Handle] = Handle.values.find(_.toString.equalsIgnoreCase(code))

enum GuideAxis derives CanEqual:
  /** A vertical line at `x = position`. */
  case Vertical
  /** A horizontal line at `y = position`. */
  case Horizontal

final case class SnapGuide(axis: GuideAxis, position: Double) derives CanEqual

final case class SnapResult(dx: Double, dy: Double, guides: List[SnapGuide])

/** Pure interaction geometry shared by the editor gestures and unit tests (all values in mm / degrees). */
object DesignGeometry:

  def rotateAround(p: Point, center: Point, degrees: Double): Point =
    if degrees == 0.0 then p
    else
      val rad = math.toRadians(degrees)
      val cos = math.cos(rad)
      val sin = math.sin(rad)
      val dx  = p.x - center.x
      val dy  = p.y - center.y
      Point(center.x + dx * cos - dy * sin, center.y + dx * sin + dy * cos)

  def normalizeAngle(deg: Double): Double =
    val m = ((deg % 360.0) + 360.0) % 360.0
    if m > 180.0 then m - 360.0 else m

  def round2(v: Double): Double = math.round(v * 100.0) / 100.0

  /** Resizes `start` by dragging `handle` to `pointer` (page coordinates), honouring rotation.
    *
    * @param keepAspect keep the start aspect ratio (corner handles; images & clipart by default)
    * @param fromCenter resize symmetrically around the centre (Alt key)
    */
  def resize(
      start: Placement,
      handle: Handle,
      pointer: Point,
      keepAspect: Boolean,
      fromCenter: Boolean,
      minSize: Double = 2.0,
  ): Placement =
    val c     = start.center
    val local = rotateAround(pointer, c, -start.rotation) - c
    val hw    = start.width / 2
    val hh    = start.height / 2

    var left   = -hw
    var right  = hw
    var top    = -hh
    var bottom = hh

    if handle.movesLeft then left = math.min(local.x, right - minSize)
    if handle.movesRight then right = math.max(local.x, left + minSize)
    if handle.movesTop then top = math.min(local.y, bottom - minSize)
    if handle.movesBottom then bottom = math.max(local.y, top + minSize)

    if fromCenter then
      if handle.movesLeft then right = -left
      if handle.movesRight then left = -right
      if handle.movesTop then bottom = -top
      if handle.movesBottom then top = -bottom

    if keepAspect && start.width > 0 && start.height > 0 then
      val aspect = start.width / start.height
      val w      = right - left
      val h      = bottom - top
      if handle.isCorner then
        val scale = math.max(w / start.width, h / start.height)
        val nw    = math.max(minSize, start.width * scale)
        val nh    = math.max(minSize, nw / aspect)
        if fromCenter then
          left = -nw / 2; right = nw / 2; top = -nh / 2; bottom = nh / 2
        else
          if handle.movesLeft then left = right - nw else right = left + nw
          if handle.movesTop then top = bottom - nh else bottom = top + nh
      else if handle.movesLeft || handle.movesRight then
        val nh = math.max(minSize, w / aspect)
        top = -nh / 2; bottom = nh / 2
      else
        val nw = math.max(minSize, h * aspect)
        left = -nw / 2; right = nw / 2

    val nw          = right - left
    val nh          = bottom - top
    val localCenter = Point((left + right) / 2, (top + bottom) / 2)
    val newCenter   = rotateAround(c + localCenter, c, start.rotation)
    Placement(
      round2(newCenter.x - nw / 2),
      round2(newCenter.y - nh / 2),
      round2(nw),
      round2(nh),
      start.rotation,
    )

  /** Rotation from the rotate handle (above the top edge) towards `pointer`. Snaps to 15° steps when `snap`,
    * and always to the right angles within ±3°.
    */
  def rotate(start: Placement, pointer: Point, snap: Boolean): Placement =
    val c   = start.center
    val raw = math.toDegrees(math.atan2(pointer.y - c.y, pointer.x - c.x)) + 90.0
    val deg = normalizeAngle(raw)
    val snapped =
      if snap then normalizeAngle(math.round(deg / 15.0) * 15.0)
      else
        val nearest = math.round(deg / 90.0) * 90.0
        if math.abs(deg - nearest) <= 3.0 then normalizeAngle(nearest) else math.round(deg * 10.0) / 10.0
    start.copy(rotation = if snapped == -180.0 then 180.0 else snapped)

  /** Snap lines offered to a moving selection: trim edges & centre lines, safe margins, and other elements. */
  def snapLines(format: DesignFormat, others: Seq[Rect]): (List[Double], List[Double]) =
    val trim = format.trimBox
    val safe = format.safeBox
    val xs   = List(trim.x, trim.centerX, trim.right, safe.x, safe.right) ++ others.flatMap(r => List(r.x, r.centerX, r.right))
    val ys   = List(trim.y, trim.centerY, trim.bottom, safe.y, safe.bottom) ++ others.flatMap(r => List(r.y, r.centerY, r.bottom))
    (xs.distinct, ys.distinct)

  /** Adjusts a move delta so the moved bounds' edges/centre align with the nearest snap line within `tolerance`. */
  def snapMove(bounds: Rect, dx: Double, dy: Double, lines: (List[Double], List[Double]), tolerance: Double): SnapResult =
    val (xs, ys) = lines
    def best(candidates: List[Double], targets: List[Double]): Option[(Double, Double)] =
      val options = for
        c <- candidates
        t <- targets
        d  = t - c
        if math.abs(d) <= tolerance
      yield (d, t)
      options.sortBy(o => math.abs(o._1)).headOption

    val movedXs = List(bounds.x + dx, bounds.centerX + dx, bounds.right + dx)
    val movedYs = List(bounds.y + dy, bounds.centerY + dy, bounds.bottom + dy)
    val sx      = best(movedXs, xs)
    val sy      = best(movedYs, ys)
    SnapResult(
      dx = dx + sx.map(_._1).getOrElse(0.0),
      dy = dy + sy.map(_._1).getOrElse(0.0),
      guides = sx.map(s => SnapGuide(GuideAxis.Vertical, s._2)).toList ++ sy.map(s => SnapGuide(GuideAxis.Horizontal, s._2)).toList,
    )

  /** Image placement inside its frame (frame-local mm): drawn size and offset of the full bitmap. */
  final case class ImagePlacement(x: Double, y: Double, width: Double, height: Double)

  def imagePlacement(frame: Placement, crop: ImageCrop, pixelWidth: Int, pixelHeight: Int): ImagePlacement =
    val pw    = math.max(1, pixelWidth).toDouble
    val ph    = math.max(1, pixelHeight).toDouble
    val base  = crop.fit match
      case ImageFit.Cover   => math.max(frame.width / pw, frame.height / ph)
      case ImageFit.Contain => math.min(frame.width / pw, frame.height / ph)
    val scale = base * math.max(0.1, crop.zoom)
    val w     = pw * scale
    val h     = ph * scale
    val fx    = math.max(0.0, math.min(1.0, crop.focusX))
    val fy    = math.max(0.0, math.min(1.0, crop.focusY))
    ImagePlacement((frame.width - w) * fx, (frame.height - h) * fy, w, h)

  /** Effective print resolution of an image as placed (pixels per inch of printed size). */
  def effectiveDpi(frame: Placement, crop: ImageCrop, pixelWidth: Int, pixelHeight: Int): Int =
    val placement = imagePlacement(frame, crop, pixelWidth, pixelHeight)
    if placement.width <= 0 then 0
    else math.round(pixelWidth / (placement.width / Units.MmPerInch)).toInt
