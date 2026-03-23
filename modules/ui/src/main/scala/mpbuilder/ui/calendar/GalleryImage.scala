package mpbuilder.ui.calendar

/** Metadata for an image stored in the gallery */
case class GalleryImage(
  id: String,
  name: String,
  thumbnailDataUrl: String,
  width: Int,
  height: Int,
  addedAt: Double,
  sizeBytes: Long,
)
