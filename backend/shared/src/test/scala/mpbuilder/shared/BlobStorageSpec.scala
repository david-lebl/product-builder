package mpbuilder.shared

import kyo.*
import kyo.test.*

class BlobStorageSpec extends Test[Any]:

  private val bytes = Chunk.from("%PDF-1.7 tiny".getBytes("UTF-8"))

  "Blob storage & artwork ingest (Phase 5)" - {
    "pure SHA-256 matches FIPS 180-4 test vectors" in {
      assert(Sha256.hex(Array.emptyByteArray) == "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")
      assert(Sha256.hex("abc".getBytes("UTF-8")) == "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad")
      assert(Sha256.hex("abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq".getBytes("UTF-8")) ==
          "248d6a61d20638b8e5c026930c3e6039a33ce45964ff2167f6ecedd419db06c1")
    }

    "data URLs decode (base64 and percent-encoded) and round-trip" in {
      val encoded = DataUrl.encode("application/pdf", bytes)
      assert(DataUrl.decode(encoded) == Right(DecodedDataUrl("application/pdf", bytes)))
      assert(DataUrl.decode("data:,Hello%2C%20World") == Right(DecodedDataUrl("text/plain", Chunk.from("Hello, World".getBytes("UTF-8")))))
      assert(DataUrl.decode("https://example.com/x.pdf").isLeft)
      assert(DataUrl.decode("data:image/png;base64,@@@").isLeft)
    }

    "storage keys reject traversal and absolute paths" in {
      assert(BlobStorage.validateKey("uploads/ord-1/art-2-flyer.pdf").isRight)
      assert(BlobStorage.validateKey("../secret").isLeft)
      assert(BlobStorage.validateKey("/etc/passwd").isLeft)
      assert(BlobStorage.validateKey("a//b").isLeft)
      assert(BlobStorage.validateKey("a/./b").isLeft)
    }

    "in-memory adapter stores, enforces max size, and deletes" in {
      for
        store <- BlobStorage.inMemory(publicBaseUrl = Some("/api/v1/blobs"), maxBytes = 64)
        meta  <- store.put("drafts/s1/a.pdf", bytes, "application/pdf")
        back  <- store.get("drafts/s1/a.pdf")
        big   <- store.put("drafts/s1/big.bin", Chunk.fill(65)(1.toByte), "application/octet-stream").either
        _     <- store.delete("drafts/s1/a.pdf")
        gone  <- store.get("drafts/s1/a.pdf")
      yield {
        assert(meta.publicUrl.contains("/api/v1/blobs/drafts/s1/a.pdf"))
        assert(meta.sizeBytes == bytes.size.toLong)
        assert(back.map(_.bytes).contains(bytes))
        assert(big == Left(BlobError.TooLarge(65, 64)))
        assert(gone.isEmpty)
      }
    }

    "ingest moves data URLs into storage; public URLs replace inline payloads, others pass through" in {
      val dataUrl = DataUrl.encode("application/pdf", bytes)
      for
        served   <- BlobStorage.inMemory(publicBaseUrl = Some("/api/v1/blobs"))
        browser  <- BlobStorage.inMemory()
        a        <- BlobIngest.ingest(served, "uploads/o/a.pdf", Some(dataUrl), Some(dataUrl))
        b        <- BlobIngest.ingest(browser, "uploads/o/b.pdf", Some(dataUrl), None)
        c        <- BlobIngest.ingest(served, "uploads/o/c.pdf", Some("https://cdn.example/c.pdf"), None)
        stored   <- served.get("uploads/o/a.pdf")
      yield {
        assert(a.downloadUrl.contains("/api/v1/blobs/uploads/o/a.pdf"))
        assert(a.previewUrl == a.downloadUrl)
        assert(a.checksumSha256.contains(Sha256.hex(bytes.toArray)))
        assert(a.sizeBytes.contains(bytes.size.toLong))
        assert(stored.isDefined)
        assert(b.downloadUrl.contains(dataUrl))
        // browser in-memory mode keeps the original data URL
        assert(b.checksumSha256.isDefined)
        assert(c == IngestedAsset(Some("https://cdn.example/c.pdf"), None, None, None))
      }
    }

    "in-memory id sequence is monotonic" in {
      for
        seq <- IdSequence.inMemory(41)
        a   <- seq.next
        b   <- seq.next
      yield {
        assert(a == 42L)
        assert(b == 43L)
      }
    }
  }
