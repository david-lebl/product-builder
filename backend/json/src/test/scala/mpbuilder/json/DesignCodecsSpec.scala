package mpbuilder.json

import kyo.*
import kyo.test.*
import mpbuilder.design.*
import mpbuilder.json.DesignCodecs.given
import mpbuilder.json.JsonSupport.*
import mpbuilder.shared.*

class DesignCodecsSpec extends Test[Any]:

  "DesignCodecs" - {
    "every template document round-trips losslessly" in {
      DesignTemplates.all.foreach { t =>
        val doc  = DesignTemplates.instantiate(t, TemplateContext(Language.Cs, 2027))
        val json = DesignCodecs.encodeDocument(doc)
        assert(DesignCodecs.decodeDocument(json) == Right(doc), t.key)
      }
    }

    "projects with owners, statuses and assets round-trip" in {
      val asset = ImageAsset(AssetId.unsafe("sha-abc"), "photo.jpg", "image/jpeg", 4000, 3000, 2_000_000L, Some("abc"), AssetStorage.Uploaded("designs/sha-abc"))
      val base  = DesignTemplates.instantiate(DesignTemplates.find("poster-photo").get, TemplateContext(Language.En, 2027))
      val slot  = DesignOps.firstEmptySlot(base.pages.head).get
      val doc   = DesignOps.placeImage(DesignOps.addAsset(base, asset), base.pages.head.id, slot, asset.id)
      val now   = java.time.Instant.parse("2026-11-24T10:00:00Z")
      val project = DesignProject(
        DesignId.unsafe("design-1"),
        "Poster",
        DesignOwner.Customer(CustomerId.unsafe("c-1")),
        doc,
        7L,
        DesignStatus.Submitted(ArtworkId.unsafe("art-1"), now),
        now,
        now,
      )
      assert(DesignCodecs.decodeProject(DesignCodecs.encodeProject(project)) == Right(project))
    }

    "missing defaulted fields decode (forward-compatible documents)" in {
      val doc  = DesignTemplates.instantiate(DesignTemplates.find("bc-classic").get, TemplateContext(Language.En, 2027))
      val json = DesignCodecs
        .encodeDocument(doc)
        .replaceAll(",?\"locked\":false", "")
        .replaceAll(",?\"opacity\":1(\\.0)?(?=[,}])", "")
        .replace("{,", "{")
      assert(!json.contains("\"locked\""))
      assert(DesignCodecs.decodeDocument(json) == Right(doc))
    }

    "invalid colours and broken documents are rejected" in {
      val doc  = DesignTemplates.instantiate(DesignTemplates.find("bc-bold").get, TemplateContext(Language.En, 2027))
      val json = DesignCodecs.encodeDocument(doc)
      assert(DesignCodecs.decodeDocument(json.replace("#4f46e5", "purple")).isLeft)
      assert(DesignCodecs.decodeDocument("""{"formatVersion":1}""").isLeft)
    }
  }
