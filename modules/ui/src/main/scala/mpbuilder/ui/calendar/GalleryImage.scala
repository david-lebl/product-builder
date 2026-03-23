package mpbuilder.ui.calendar

/** An image uploaded to the cross-session gallery */
case class GalleryImage(
  id: String,
  name: String,
  thumbnailDataUrl: String,
  width: Int,
  height: Int,
  addedAt: Double,
  sizeBytes: Long,
)
