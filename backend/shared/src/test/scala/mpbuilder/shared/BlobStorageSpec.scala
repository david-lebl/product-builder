package mpbuilder.shared

import zio.*
import zio.test.*

object BlobStorageSpec extends ZIOSpecDefault:

  private val bytes = Chunk.fromArray("%PDF-1.7 tiny".getBytes("UTF-8"))

  def spec = suite("Blob storage & artwork ingest (Phase 5)")(
    test("pure SHA-256 matches FIPS 180-4 test vectors") {
      assertTrue(
        Sha256.hex(Array.emptyByteArray) == "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
        Sha256.hex("abc".getBytes("UTF-8")) == "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
        Sha256.hex("abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq".getBytes("UTF-8")) ==
          "248d6a61d20638b8e5c026930c3e6039a33ce45964ff2167f6ecedd419db06c1",
      )
    },
    test("data URLs decode (base64 and percent-encoded) and round-trip") {
      val encoded = DataUrl.encode("application/pdf", bytes)
      assertTrue(
        DataUrl.decode(encoded) == Right(DecodedDataUrl("application/pdf", bytes)),
        DataUrl.decode("data:,Hello%2C%20World") == Right(DecodedDataUrl("text/plain", Chunk.fromArray("Hello, World".getBytes("UTF-8")))),
        DataUrl.decode("https://example.com/x.pdf").isLeft,
        DataUrl.decode("data:image/png;base64,@@@").isLeft,
      )
    },
    test("storage keys reject traversal and absolute paths") {
      assertTrue(
        BlobStorage.validateKey("uploads/ord-1/art-2-flyer.pdf").isRight,
        BlobStorage.validateKey("../secret").isLeft,
        BlobStorage.validateKey("/etc/passwd").isLeft,
        BlobStorage.validateKey("a//b").isLeft,
        BlobStorage.validateKey("a/./b").isLeft,
      )
    },
    test("in-memory adapter stores, enforces max size, and deletes") {
      for
        store <- BlobStorage.inMemory(publicBaseUrl = Some("/api/v1/blobs"), maxBytes = 64)
        meta  <- store.put("drafts/s1/a.pdf", bytes, "application/pdf")
        back  <- store.get("drafts/s1/a.pdf")
        big   <- store.put("drafts/s1/big.bin", Chunk.fill(65)(1.toByte), "application/octet-stream").either
        _     <- store.delete("drafts/s1/a.pdf")
        gone  <- store.get("drafts/s1/a.pdf")
      yield assertTrue(
        meta.publicUrl.contains("/api/v1/blobs/drafts/s1/a.pdf"),
        meta.sizeBytes == bytes.size.toLong,
        back.map(_.bytes).contains(bytes),
        big == Left(BlobError.TooLarge(65, 64)),
        gone.isEmpty,
      )
    },
    test("ingest moves data URLs into storage; public URLs replace inline payloads, others pass through") {
      val dataUrl = DataUrl.encode("application/pdf", bytes)
      for
        served   <- BlobStorage.inMemory(publicBaseUrl = Some("/api/v1/blobs"))
        browser  <- BlobStorage.inMemory()
        a        <- BlobIngest.ingest(served, "uploads/o/a.pdf", Some(dataUrl), Some(dataUrl))
        b        <- BlobIngest.ingest(browser, "uploads/o/b.pdf", Some(dataUrl), None)
        c        <- BlobIngest.ingest(served, "uploads/o/c.pdf", Some("https://cdn.example/c.pdf"), None)
        stored   <- served.get("uploads/o/a.pdf")
      yield assertTrue(
        a.downloadUrl.contains("/api/v1/blobs/uploads/o/a.pdf"),
        a.previewUrl == a.downloadUrl,
        a.checksumSha256.contains(Sha256.hex(bytes.toArray)),
        a.sizeBytes.contains(bytes.size.toLong),
        stored.isDefined,
        b.downloadUrl.contains(dataUrl), // browser in-memory mode keeps the original data URL
        b.checksumSha256.isDefined,
        c == IngestedAsset(Some("https://cdn.example/c.pdf"), None, None, None),
      )
    },
    test("in-memory id sequence is monotonic") {
      for
        seq <- IdSequence.inMemory(41)
        a   <- seq.next
        b   <- seq.next
      yield assertTrue(a == 42L, b == 43L)
    },
  )
