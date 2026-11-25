package mpbuilder.artwork.sample

import mpbuilder.artwork.*
import mpbuilder.artwork.core.{ArtworkRepository, ArtworkServiceLive, ArtworkSubmission}
import mpbuilder.json.DomainCodecs.given
import mpbuilder.sample.SampleStorage
import mpbuilder.shared.*
import kyo.*

final class InMemoryArtworkRepository(
    ref: StateRef[Map[ArtworkId, ArtworkSubmission]],
) extends ArtworkRepository:

  override def findById(id: ArtworkId): Option[ArtworkSubmission] < Async =
    ref.get.map(_.get(id))

  override def findByOwner(owner: ArtworkOwner): List[ArtworkSubmission] < Async =
    ref.get.map(_.values.filter(_.owner == owner).toList)

  override def save(submission: ArtworkSubmission): Unit < Async =
    ref.update(_ + (submission.id -> submission))

  override def delete(id: ArtworkId): Unit < Async =
    ref.update(_ - id)

  def listAll: List[ArtworkSubmissionView] < Async =
    ref.get.map(_.values.map(_.toView).toList)

object ArtworkSampleWiring:

  private given Schema[ArtworkSubmission] = Schema.derived

  private val baseInstant = java.time.Instant.ofEpochMilli(1740000000000L)

  /** Generates a self-contained in-memory SVG print-proof `data:` URL (with crop marks, bleed frame,
    * trim box, and spec metadata) so Preview & Download work out-of-the-box without external storage.
    */
  def generateProofSvgDataUrl(
      title: String,
      subtitle: String,
      accentHex: String,
      widthMm: Int,
      heightMm: Int,
      dpi: Int,
      colorSpace: String,
      bleedMm: Int,
  ): NonEmptyString =
    val escTitle = title.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    val escSub   = subtitle.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    val svg =
      s"""<svg xmlns="http://www.w3.org/2000/svg" width="640" height="420" viewBox="0 0 640 420">
         |  <rect width="640" height="420" fill="#F1F5F9"/>
         |  <rect x="44" y="34" width="552" height="352" fill="#FFFFFF" stroke="#CBD5E1" stroke-width="1.5" stroke-dasharray="6 4"/>
         |  <rect x="64" y="54" width="512" height="312" fill="#FFFFFF" stroke="#0F172A" stroke-width="1.5"/>
         |  <line x1="64" y1="24" x2="64" y2="46" stroke="#0F172A" stroke-width="1"/>
         |  <line x1="34" y1="54" x2="56" y2="54" stroke="#0F172A" stroke-width="1"/>
         |  <line x1="576" y1="24" x2="576" y2="46" stroke="#0F172A" stroke-width="1"/>
         |  <line x1="584" y1="54" x2="606" y2="54" stroke="#0F172A" stroke-width="1"/>
         |  <line x1="64" y1="374" x2="64" y2="396" stroke="#0F172A" stroke-width="1"/>
         |  <line x1="34" y1="366" x2="56" y2="366" stroke="#0F172A" stroke-width="1"/>
         |  <line x1="576" y1="374" x2="576" y2="396" stroke="#0F172A" stroke-width="1"/>
         |  <line x1="584" y1="366" x2="606" y2="366" stroke="#0F172A" stroke-width="1"/>
         |  <rect x="64" y="54" width="512" height="72" fill="$accentHex"/>
         |  <text x="92" y="96" font-family="system-ui,-apple-system,sans-serif" font-size="22" font-weight="700" fill="#FFFFFF">$escTitle</text>
         |  <text x="92" y="158" font-family="system-ui,-apple-system,sans-serif" font-size="15" font-weight="600" fill="#1E293B">$escSub</text>
         |  <rect x="92" y="184" width="456" height="84" rx="8" fill="#F8FAFC" stroke="#E2E8F0"/>
         |  <text x="112" y="214" font-family="monospace" font-size="13" fill="#334155">TRIM: ${widthMm} × ${heightMm} mm   |   BLEED: ${bleedMm} mm</text>
         |  <text x="112" y="242" font-family="monospace" font-size="13" fill="#334155">RESOLUTION: ${dpi} DPI   |   COLOR: $colorSpace</text>
         |  <rect x="92" y="294" width="44" height="16" rx="3" fill="#00AEEF"/>
         |  <rect x="142" y="294" width="44" height="16" rx="3" fill="#EC008C"/>
         |  <rect x="192" y="294" width="44" height="16" rx="3" fill="#FFF200" stroke="#CBD5E1"/>
         |  <rect x="242" y="294" width="44" height="16" rx="3" fill="#111827"/>
         |  <text x="92" y="338" font-family="system-ui,-apple-system,sans-serif" font-size="11" fill="#64748B">PRINTCRAFT DEMO PROOF · TRIM BOX (SOLID) / BLEED (DASHED)</text>
         |</svg>""".stripMargin
    val b64 = base64EncodeUtf8(svg)
    NonEmptyString.unsafe(s"data:image/svg+xml;base64,$b64")

  private val Base64Chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"

  private def base64EncodeUtf8(str: String): String =
    val bytes = str.getBytes(java.nio.charset.StandardCharsets.UTF_8)
    val sb    = new StringBuilder(((bytes.length + 2) / 3) * 4)
    var i     = 0
    while i < bytes.length do
      val b0 = bytes(i) & 0xff
      val b1 = if i + 1 < bytes.length then bytes(i + 1) & 0xff else 0
      val b2 = if i + 2 < bytes.length then bytes(i + 2) & 0xff else 0
      sb.append(Base64Chars.charAt(b0 >>> 2))
      sb.append(Base64Chars.charAt(((b0 & 0x03) << 4) | (b1 >>> 4)))
      sb.append(if i + 1 < bytes.length then Base64Chars.charAt(((b1 & 0x0f) << 2) | (b2 >>> 6)) else '=')
      sb.append(if i + 2 < bytes.length then Base64Chars.charAt(b2 & 0x3f) else '=')
      i += 3
    sb.toString

  private[artwork] def seedArtworks: Map[ArtworkId, ArtworkSubmission] =
    val proof1 = generateProofSvgDataUrl("Dvořák Business Cards", "dvorak-business-cards-v1.pdf · 2 pages", "#4F46E5", 90, 50, 240, "RGB", 3)
    val art1 = ArtworkSubmission
      .create(
        UploadArtworkCmd(
          artworkId = ArtworkId.unsafe("art-seed-0901"),
          owner = ArtworkOwner.OrderLineRef(OrderId.unsafe("ord-seed-0901"), 0),
          componentKey = ComponentKey.Main,
          file = AssetFileMetadata(
            fileName = NonEmptyString.unsafe("dvorak-business-cards-v1.pdf"),
            mimeType = NonEmptyString.unsafe("application/pdf"),
            sizeBytes = 4_820_000L,
            storageKey = NonEmptyString.unsafe("demo/ord-seed-0901/dvorak-bc-v1.pdf"),
            downloadUrl = Some(proof1),
            previewUrl = Some(proof1),
          ),
          extractedSpecs = ExtractedFileSpecs(
            resolutionDpi = 240,
            trimSize = Dimension2D(PositiveMm.unsafe(90), PositiveMm.unsafe(50)),
            bleed = NonNegativeMm.unsafe(3),
            colorSpace = ColorSpace.RGB,
            pageCount = 2,
            fontsEmbedded = true,
          ),
          requirement = Some(
            PreflightRequirement(
              expectedTrimSize = Dimension2D(PositiveMm.unsafe(90), PositiveMm.unsafe(50)),
              requiredBleed = NonNegativeMm.unsafe(3),
              minResolutionDpi = 300,
              expectedPageCount = Some(2),
            )
          ),
        ),
        baseInstant,
      )
      .toOption
      .get

    val proof2 = generateProofSvgDataUrl("Spring Promo Flyers A5", "printshoppro-a5-flyers-spring.pdf · Low DPI & Bleed", "#D97706", 148, 210, 180, "RGB", 1)
    val art2 = ArtworkSubmission
      .create(
        UploadArtworkCmd(
          artworkId = ArtworkId.unsafe("art-seed-0902"),
          owner = ArtworkOwner.OrderLineRef(OrderId.unsafe("ord-seed-0902"), 0),
          componentKey = ComponentKey.Main,
          file = AssetFileMetadata(
            fileName = NonEmptyString.unsafe("printshoppro-a5-flyers-spring.pdf"),
            mimeType = NonEmptyString.unsafe("application/pdf"),
            sizeBytes = 12_400_000L,
            storageKey = NonEmptyString.unsafe("demo/ord-seed-0902/flyers-spring.pdf"),
            downloadUrl = Some(proof2),
            previewUrl = Some(proof2),
          ),
          extractedSpecs = ExtractedFileSpecs(
            resolutionDpi = 180,
            trimSize = Dimension2D(PositiveMm.unsafe(148), PositiveMm.unsafe(210)),
            bleed = NonNegativeMm.unsafe(1),
            colorSpace = ColorSpace.RGB,
            pageCount = 2,
            fontsEmbedded = true,
            hasUnflattenedTransparency = true,
          ),
          requirement = Some(
            PreflightRequirement(
              expectedTrimSize = Dimension2D(PositiveMm.unsafe(148), PositiveMm.unsafe(210)),
              requiredBleed = NonNegativeMm.unsafe(3),
              minResolutionDpi = 300,
              expectedPageCount = Some(2),
            )
          ),
        ),
        baseInstant.plusSeconds(3600),
      )
      .toOption
      .get
      .requestChanges(NonEmptyString.unsafe("Please re-export at 300 DPI with 3 mm bleed and CMYK color profile."))
      .toOption
      .get

    val proof3 = generateProofSvgDataUrl("MegaCorp Product Brochure", "megacorp-brochure-final-cmyk.pdf · Approved Press-Ready", "#059669", 210, 297, 300, "CMYK", 3)
    val art3 = ArtworkSubmission
      .create(
        UploadArtworkCmd(
          artworkId = ArtworkId.unsafe("art-seed-0903"),
          owner = ArtworkOwner.OrderLineRef(OrderId.unsafe("ord-seed-0903"), 0),
          componentKey = ComponentKey.Main,
          file = AssetFileMetadata(
            fileName = NonEmptyString.unsafe("megacorp-brochure-final-cmyk.pdf"),
            mimeType = NonEmptyString.unsafe("application/pdf"),
            sizeBytes = 28_900_000L,
            storageKey = NonEmptyString.unsafe("demo/ord-seed-0903/megacorp-brochure.pdf"),
            downloadUrl = Some(proof3),
            previewUrl = Some(proof3),
          ),
          extractedSpecs = ExtractedFileSpecs(
            resolutionDpi = 300,
            trimSize = Dimension2D(PositiveMm.unsafe(210), PositiveMm.unsafe(297)),
            bleed = NonNegativeMm.unsafe(3),
            colorSpace = ColorSpace.CMYK,
            pageCount = 4,
            fontsEmbedded = true,
          ),
          requirement = Some(
            PreflightRequirement(
              expectedTrimSize = Dimension2D(PositiveMm.unsafe(210), PositiveMm.unsafe(297)),
              requiredBleed = NonNegativeMm.unsafe(3),
              minResolutionDpi = 300,
              expectedPageCount = Some(4),
            )
          ),
        ),
        baseInstant.plusSeconds(7200),
      )
      .toOption
      .get
      .approve(EmployeeId.unsafe("emp-prepress"), overrideFailures = false, baseInstant.plusSeconds(9000))
      .toOption
      .get

    val proof4 = generateProofSvgDataUrl("GraphicDesign Roll-up Banner", "graphicdesign-rollups-cmyk.pdf · Approved Large Format", "#0284C7", 850, 2000, 300, "CMYK", 5)
    val art4 = ArtworkSubmission
      .create(
        UploadArtworkCmd(
          artworkId = ArtworkId.unsafe("art-seed-0904"),
          owner = ArtworkOwner.OrderLineRef(OrderId.unsafe("ord-seed-0904"), 0),
          componentKey = ComponentKey.Main,
          file = AssetFileMetadata(
            fileName = NonEmptyString.unsafe("graphicdesign-rollups-cmyk.pdf"),
            mimeType = NonEmptyString.unsafe("application/pdf"),
            sizeBytes = 19_500_000L,
            storageKey = NonEmptyString.unsafe("demo/ord-seed-0904/rollups.pdf"),
            downloadUrl = Some(proof4),
            previewUrl = Some(proof4),
          ),
          extractedSpecs = ExtractedFileSpecs(
            resolutionDpi = 300,
            trimSize = Dimension2D(PositiveMm.unsafe(850), PositiveMm.unsafe(2000)),
            bleed = NonNegativeMm.unsafe(5),
            colorSpace = ColorSpace.CMYK,
            pageCount = 1,
            fontsEmbedded = true,
          ),
          requirement = Some(
            PreflightRequirement(
              expectedTrimSize = Dimension2D(PositiveMm.unsafe(850), PositiveMm.unsafe(2000)),
              requiredBleed = NonNegativeMm.unsafe(3),
              minResolutionDpi = 150,
              expectedPageCount = Some(1),
            )
          ),
        ),
        baseInstant.plusSeconds(10800),
      )
      .toOption
      .get
      .approve(EmployeeId.unsafe("emp-prepress"), overrideFailures = false, baseInstant.plusSeconds(12000))
      .toOption
      .get

    List(art1, art2, art3, art4).map(a => a.id -> a).toMap

  def repository(
      seedSampleArtworks: Boolean = true,
      storage: SampleStorage = SampleStorage.memory,
  ): InMemoryArtworkRepository < Sync =
    val init = if seedSampleArtworks then seedArtworks else Map.empty[ArtworkId, ArtworkSubmission]
    storage.map("artwork.submissions", init)(_.value, ArtworkId.unsafe).map(new InMemoryArtworkRepository(_))

  def service(
      eventBus: EventBus,
      seedSampleArtworks: Boolean = true,
      storage: SampleStorage = SampleStorage.memory,
  ): ArtworkService < Sync =
    repository(seedSampleArtworks, storage).map(ArtworkServiceLive.make(_, eventBus))
